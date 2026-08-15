package com.cszyapp.cmal.data.repo

import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.McVersion
import com.cszyapp.cmal.data.source.SourceManager
import com.cszyapp.cmal.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 版本仓库
 * 负责版本索引的拉取（多源）、版本记录本地化
 */
class VersionsRepository(
    private val database: AppDatabase,
    private val sourceManager: SourceManager,
    private val preferences: Preferences
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun observeVersions(): Flow<List<McVersion>> = database.mcVersionDao().observeAll()

    suspend fun get(code: Int): McVersion? = database.mcVersionDao().get(code)

    /**
     * 从配置的所有源拉取版本列表（合并去重）
     */
    suspend fun refreshFromSources(): Result<List<McVersion>> = withContext(Dispatchers.IO) {
        val results = sourceManager.getIndexUrls().mapNotNull { url ->
            try {
                fetchIndex(url)
            } catch (_: Exception) {
                null
            }
        }
        val merged = results.flatten().distinctBy { it.versionCode }
        if (merged.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("all_sources_failed"))
        }
        // 本地化保存已下载/已安装状态
        val saved = database.mcVersionDao().latestDownloaded()
        val finalList = merged.map { v ->
            val existed = database.mcVersionDao().get(v.versionCode)
            v.copy(
                downloaded = existed?.downloaded == true,
                installed = existed?.installed == true,
                addedAt = existed?.addedAt ?: v.addedAt
            )
        }
        finalList.forEach { database.mcVersionDao().upsert(it) }
        Result.success(finalList)
    }

    private fun fetchIndex(url: String): List<McVersion> {
        val request = Request.Builder().url(url).header("User-Agent", "CMAL/0.1").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("http_${resp.code}")
            val body = resp.body?.string() ?: throw IllegalStateException("empty_body")
            return parseIndex(body)
        }
    }

    private fun parseIndex(body: String): List<McVersion> {
        val arr = JSONArray(body)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            McVersion(
                versionCode = o.getInt("code"),
                versionName = o.getString("name"),
                downloadUrl = o.getString("url"),
                size = o.optLong("size", 0L)
            )
        }
    }

    suspend fun upsert(version: McVersion) {
        database.mcVersionDao().upsert(version)
    }

    suspend fun markDownloaded(version: McVersion) {
        database.mcVersionDao().upsert(version.copy(downloaded = true))
    }

    suspend fun markInstalled(version: McVersion) {
        database.mcVersionDao().upsert(version.copy(installed = true))
    }

    suspend fun delete(version: McVersion) {
        database.mcVersionDao().delete(version)
    }
}
