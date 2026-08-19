package com.cszyapp.cmal.data.download

import android.content.Context
import com.cszyapp.cmal.data.market.MarketItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 单个下载任务的状态
 */
data class DownloadState(
    val item: MarketItem,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val running: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
    val targetFile: File? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

/**
 * 多线程下载引擎
 * - 并发下载多个任务（默认 3 个）
 * - 进度上报、取消、错误透传
 * - 下载到应用私有目录，避免存储权限
 */
class DownloadManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tasks = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val tasks: StateFlow<Map<String, DownloadState>> = _tasks.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()
    private val semaphore = Semaphore(MAX_CONCURRENT)
    private val startMutex = Mutex()

    private val downloadDir: File
        get() = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }

    /** 启动下载（多任务并发）。返回 taskId。已存在进行中/已完成任务则复用。 */
    suspend fun startDownload(item: MarketItem): String = withContext(Dispatchers.IO) {
        startMutex.withLock {
            val existing = _tasks.value[item.id]
            if (existing?.running == true || existing?.done == true) return@withContext item.id
            // 创建任务即置 running=true，避免快速连点并发写同一文件
            _tasks.update { it + (item.id to DownloadState(item = item, running = true, targetFile = File(downloadDir, buildFileName(item)))) }
            jobs[item.id] = scope.launch {
                semaphore.withPermit { downloadOne(item) }
            }
            item.id
        }
    }

    /** 取消某个任务 */
    fun cancelDownload(taskId: String) {
        jobs[taskId]?.cancel()
        _tasks.update { m ->
            val cur = m[taskId] ?: return@update m
            m + (taskId to cur.copy(running = false, error = "cancelled"))
        }
    }

    /** 取消所有进行中的任务 */
    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
    }

    /** 删除任务记录 */
    fun removeTask(taskId: String) {
        jobs.remove(taskId)?.cancel()
        _tasks.update { it - taskId }
    }

    /** 已下载完成的文件列表 */
    fun completedFiles(): List<Pair<MarketItem, File>> =
        _tasks.value.values
            .filter { it.done && it.targetFile?.exists() == true }
            .mapNotNull { it.targetFile?.let { f -> it.item to f } }

    fun shutdown() {
        jobs.values.forEach { it.cancel() }
        scope.cancel()
    }

    /** 下载目录总占用字节数 */
    fun downloadDirSize(): Long = downloadDir.listFiles()?.sumOf { it.length() } ?: 0L

    /** 清空已完成且未在下载中的文件 */
    fun clearFinishedDownloads() {
        val finishedFiles = _tasks.value.values
            .filter { it.done && it.targetFile != null }
            .mapNotNull { it.targetFile }
            .toSet()
        _tasks.update { m ->
            m.filterValues { !it.done }
        }
        // 只删已完成任务的文件，保留进行中的
        val activeFiles = _tasks.value.values.mapNotNull { it.targetFile }.toSet()
        finishedFiles.forEach { f ->
            if (f !in activeFiles) f.delete()
        }
    }

    private suspend fun downloadOne(item: MarketItem) {
        val url = item.downloadUrl
        val target = _tasks.value[item.id]?.targetFile ?: return
        if (url.isNullOrBlank()) {
            _tasks.update { m -> m + (item.id to (m[item.id]?.copy(running = false, error = "no_download_url") ?: return@update m)) }
            return
        }
        _tasks.update { m ->
            m + (item.id to (m[item.id]?.copy(running = true, error = null, totalBytes = 0, downloadedBytes = 0) ?: return@update m))
        }
        try {
            val request = Request.Builder().url(url).header("User-Agent", "CMAL/0.1").build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    fail(item, "http_${resp.code}", deleteFile = true)
                    return
                }
                // 拦截落地页/HTML：McFun 部分"直连"是 makily.com 的 MCloud 落地页
                val ct = resp.header("Content-Type").orEmpty().lowercase()
                if (ct.contains("text/html") || ct.contains("text/plain")) {
                    fail(item, "invalid_file", deleteFile = true)
                    return
                }
                val total = resp.body?.contentLength() ?: 0L
                val bytes = AtomicLong(0)
                _tasks.update { m ->
                    m + (item.id to (m[item.id]?.copy(totalBytes = total) ?: return@update m))
                }
                resp.body?.byteStream()?.use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            bytes.addAndGet(read.toLong())
                            // 每 ~32KB 上报一次进度，避免过度重组
                            if (bytes.get() % 32768 < 8192) {
                                val downloaded = bytes.get()
                                _tasks.update { m ->
                                    m + (item.id to (m[item.id]?.copy(downloadedBytes = downloaded) ?: return@update m))
                                }
                            }
                        }
                    }
                }
            }
            val finalTotal = _tasks.value[item.id]?.totalBytes ?: 0
            _tasks.update { m ->
                m + (item.id to (m[item.id]?.copy(running = false, done = true, downloadedBytes = finalTotal) ?: return@update m))
            }
        } catch (e: CancellationException) {
            deletePartial(item)
            throw e
        } catch (e: Exception) {
            fail(item, e.message ?: "download_error", deleteFile = true)
        }
    }

    /** 置失败状态；deleteFile=true 时清理已写入的部分文件 */
    private fun fail(item: MarketItem, error: String, deleteFile: Boolean) {
        if (deleteFile) deletePartial(item)
        _tasks.update { m ->
            m + (item.id to (m[item.id]?.copy(running = false, error = error) ?: return@update m))
        }
    }

    private fun deletePartial(item: MarketItem) {
        _tasks.value[item.id]?.targetFile?.delete()
    }

    private fun buildFileName(item: MarketItem): String =
        DownloadManager.buildFileName(item.title, item.type, item.id.hashCode())

    companion object {
        private const val MAX_CONCURRENT = 3

        /** 生成安全的下载文件名（纯函数，可单测）。salt 用于避免同名不同资源互相覆盖 */
        fun buildFileName(title: String, type: String, salt: Int = 0): String {
            val safe = title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .replace(Regex("\\s+"), "_")
                .take(48)
                .ifBlank { "download" }
            val saltPart = if (salt == 0) "" else "_${(salt % 100000).let { if (it < 0) -it else it }}"
            val ext = when (type) {
                "world" -> ".mcworld"
                else -> ".mcpack"
            }
            return "$safe$saltPart$ext"
        }

        fun sizeString(bytes: Long): String {
            if (bytes <= 0) return ""
            return when {
                bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
                bytes >= 1024 -> "${bytes / 1024} KB"
                else -> "$bytes B"
            }
        }
    }
}