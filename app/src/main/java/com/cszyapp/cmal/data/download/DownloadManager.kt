package com.cszyapp.cmal.data.download

import android.content.Context
import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.DownloadTask
import com.cszyapp.cmal.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 下载进度状态
 */
data class DownloadProgress(
    val taskId: String,
    val fileName: String,
    val totalBytes: Long,
    val doneBytes: Long,
    val speedBytesPerSec: Long,
    val status: String
) {
    val fraction: Float
        get() = if (totalBytes > 0) (doneBytes.toFloat() / totalBytes) else 0f
}

/**
 * 多线程分片下载管理器
 *
 * 特性：
 * - 单文件分片多线程下载（Range 断点续传）
 * - 多任务并行（极限并发）
 * - 进度实时上报（速度 + 百分比）
 * - 断点续传（临时分片文件 .part 复用）
 */
class DownloadManager(
    private val context: Context,
    private val preferences: Preferences
) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.get(context)

    private val _activeTasks = MutableStateFlow<List<DownloadProgress>>(emptyList())
    val activeTasks: StateFlow<List<DownloadProgress>> = _activeTasks.asStateFlow()

    private val tasks = ConcurrentHashMap<String, TaskRuntime>()
    private val jobs = ConcurrentHashMap<String, Job>()

    /** 单文件分片数（极限并发，默认 8 片） */
    private val maxChunks: Int = 8

    /** 下载目录 */
    fun downloadDir(): File =
        File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }

    private class TaskRuntime(
        val progress: MutableStateFlow<DownloadProgress>
    )

    /**
     * 发起下载
     * @param url 下载地址
     * @param fileName 保存文件名
     * @param onFinish 完成后回调（文件路径）
     */
    fun enqueue(url: String, fileName: String, onFinish: (String) -> Unit = {}) {
        val taskId = UUID.randomUUID().toString()
        val safeName = File(fileName).name
        val task = DownloadTask(
            taskId = taskId,
            url = url,
            fileName = safeName,
            savePath = File(downloadDir(), safeName).absolutePath,
            status = "queued",
            createdAt = System.currentTimeMillis()
        )
        scope.launch { database.downloadTaskDao().upsert(task) }
        jobs[taskId] = scope.launch {
            runDownload(taskId, url, safeName, onFinish)
        }
    }

    /** 取消任务 */
    fun cancel(taskId: String) {
        jobs[taskId]?.cancel()
        jobs.remove(taskId)
        tasks.remove(taskId)
        publish()
    }

    private suspend fun runDownload(
        taskId: String,
        url: String,
        fileName: String,
        onFinish: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val progress = MutableStateFlow(
            DownloadProgress(taskId, fileName, 0, 0, 0, "starting")
        )
        tasks[taskId] = TaskRuntime(progress)
        publish()
        try {
            val file = File(downloadDir(), fileName)
            val total = probeTotalSize(url)
            if (total == null || total <= 0) {
                progress.value = progress.value.copy(status = "downloading")
                downloadWhole(url, file, progress)
                finishProgress(taskId, progress, file.length())
                onFinish(file.absolutePath)
                return@withContext
            }

            progress.value = progress.value.copy(totalBytes = total, status = "downloading")

            val chunkCount = if (total < 1024 * 1024) 1 else maxChunks
            val chunkSize = total / chunkCount
            val partDir = File(downloadDir(), "$fileName.parts").apply { mkdirs() }
            val partFiles = (0 until chunkCount).map { i -> File(partDir, "part_$i") }

            // 累计已下载字节数（用于断点续传后的进度基准）
            val baseDone = partFiles.sumOf { it.length().coerceAtMost(chunkSize) }
            val done = java.util.concurrent.atomic.AtomicLong(baseDone)
            progress.value = progress.value.copy(doneBytes = baseDone)

            val pool = Executors.newFixedThreadPool(chunkCount)
            val futures = ArrayList<java.util.concurrent.Future<*>>()
            for (i in 0 until chunkCount) {
                val start = i * chunkSize
                val end = if (i == chunkCount - 1) total - 1 else (i + 1) * chunkSize - 1
                futures.add(pool.submit {
                    downloadChunk(url, start, end, partFiles[i], done, progress)
                })
            }
            futures.forEach { it.get() }
            pool.shutdown()

            mergeParts(partFiles, file, total)
            partDir.deleteRecursively()

            finishProgress(taskId, progress, total)
            database.downloadTaskDao().upsert(
                DownloadTask(taskId, url, fileName, file.absolutePath, total, total, "done")
            )
            onFinish(file.absolutePath)
        } catch (e: Exception) {
            progress.value = progress.value.copy(status = "error")
            database.downloadTaskDao().upsert(
                DownloadTask(taskId, url, fileName, File(downloadDir(), fileName).absolutePath, 0, 0, "error")
            )
        } finally {
            tasks.remove(taskId)
            jobs.remove(taskId)
            publish()
        }
    }

    private fun finishProgress(taskId: String, progress: MutableStateFlow<DownloadProgress>, done: Long) {
        val base = progress.value.doneBytes
        progress.value = progress.value.copy(status = "done", doneBytes = done, totalBytes = progress.value.totalBytes)
        publish()
    }

    /** 探测文件总大小（HEAD Range 请求） */
    private fun probeTotalSize(url: String): Long? {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .header("User-Agent", "CMAL/0.1")
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val range = resp.header("Content-Range")
                if (range != null) {
                    val idx = range.lastIndexOf('/')
                    if (idx >= 0) range.substring(idx + 1).toLongOrNull() else null
                } else {
                    resp.header("Content-Length")?.toLongOrNull()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 下载单个分片（断点续传：从 part 文件现有长度继续） */
    private fun downloadChunk(
        url: String,
        start: Long,
        end: Long,
        partFile: File,
        sharedDone: java.util.concurrent.atomic.AtomicLong,
        progress: MutableStateFlow<DownloadProgress>
    ) {
        val existing = partFile.length().coerceAtMost(end - start + 1)
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=${start + existing}-$end")
            .header("User-Agent", "CMAL/0.1")
            .build()
        client.newCall(request).execute().use { resp ->
            // 只接受 206 Partial Content；若服务器忽略 Range 返回 200（完整文件），
            // 直接放弃该分片，避免把整文件写进每个分片导致合并损坏
            if (resp.code != 206) return@use
            resp.body ?: return@use
            val body = resp.body!!
            RandomAccessFile(partFile, "rw").use { raf ->
                raf.seek(existing)
                val buffer = ByteArray(256 * 1024)
                val input = body.byteStream()
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    raf.write(buffer, 0, n)
                    // 实时累加进度
                    sharedDone.addAndGet(n.toLong())
                    progress.value = progress.value.copy(doneBytes = sharedDone.get())
                }
            }
        }
    }

    /** 不支持 Range 时整文件下载（单线程） */
    private fun downloadWhole(url: String, file: File, progress: MutableStateFlow<DownloadProgress>) {
        val request = Request.Builder().url(url).header("User-Agent", "CMAL/0.1").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("http_${resp.code}")
            val total = resp.header("Content-Length")?.toLongOrNull() ?: -1
            resp.body?.let { body ->
                file.outputStream().use { out ->
                    val buffer = ByteArray(256 * 1024)
                    val input = body.byteStream()
                    var done = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        if (total > 0) {
                            progress.value = progress.value.copy(totalBytes = total, doneBytes = done)
                        }
                    }
                }
            }
        }
    }

    /** 合并分片并校验大小 */
    private fun mergeParts(parts: List<File>, target: File, expectedSize: Long) {
        RandomAccessFile(target, "rw").use { raf ->
            raf.setLength(0)
            for (p in parts) {
                RandomAccessFile(p, "r").use { input ->
                    raf.seek(raf.length())
                    val buf = ByteArray(256 * 1024)
                    var n: Int
                    while (true) {
                        n = input.read(buf)
                        if (n < 0) break
                        raf.write(buf, 0, n)
                    }
                }
            }
        }
        if (target.length() < expectedSize) {
            throw IllegalStateException("merge_incomplete ${target.length()}/$expectedSize")
        }
    }

    private fun publish() {
        _activeTasks.value = tasks.values.map { it.progress.value }
    }

    /** 释放资源 */
    fun shutdown() {
        scope.cancel()
    }
}
