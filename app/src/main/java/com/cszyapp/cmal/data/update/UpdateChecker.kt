package com.cszyapp.cmal.data.update

import android.content.Context
import android.content.pm.PackageManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 应用自更新检查
 * 从 GitHub Releases API 拉取最新版本信息
 */
data class AppUpdateInfo(
    val versionName: String,
    val tagName: String,
    val apkUrl: String?,
    val changelog: String
)

class UpdateChecker(private val context: Context?) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 仓库地址（可在源码中修改为你自己的 GitHub 仓库） */
    companion object {
        const val REPO = "cszy-app/CMAL"
        const val RELEASES_URL = "https://api.github.com/repos/$REPO/releases/latest"
    }

    /** 当前应用版本号 */
    fun currentVersion(): String =
        try {
            if (context == null) return "0.1"
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.1"
        }

    /**
     * 检查更新。返回 null 表示已是最新
     */
    fun check(): Result<AppUpdateInfo?> {
        return try {
            val req = Request.Builder()
                .url(RELEASES_URL)
                .header("User-Agent", "CMAL")
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Result.success(null)
                resp.body?.string() ?: return Result.success(null)
            }
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            val name = json.optString("name", tag)
            val notes = json.optString("body", "")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val aName = asset.optString("name")
                    if (aName.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            if (apkUrl == null) apkUrl = json.optString("html_url")

            val current = currentVersion()
            val info = AppUpdateInfo(name, tag, apkUrl, notes)
            Result.success(if (isNewer(tag, current)) info else null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 简单版本比较：tag 形如 v1.2.3 或 1.2.3 */
    private fun isNewer(remote: String, current: String): Boolean {
        val parse = { v: String ->
            Regex("(\\d+)").findAll(v).map { it.groupValues[1].toIntOrNull() ?: 0 }.toList()
        }
        val a = parse(remote)
        val b = parse(current)
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av > bv
        }
        return false
    }
}
