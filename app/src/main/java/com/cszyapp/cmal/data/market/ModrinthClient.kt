package com.cszyapp.cmal.data.market

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Modrinth 官方 API 客户端（免费、无需 API key）
 * 文档：https://docs.modrinth.com/api-spec/
 */
class ModrinthClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE = "https://api.modrinth.com/v2"

        /** 支持的资源类型（Bedrock 相关） */
        val SUPPORTED_TYPES = listOf("mod", "resourcepack", "shader", "datapack")

        /** 中文友好名称 */
        fun typeName(type: String): String = when (type) {
            "mod" -> "模组"
            "resourcepack" -> "资源包"
            "shader" -> "光影"
            "datapack" -> "数据包"
            "world" -> "世界"
            else -> type
        }
    }

    /**
     * 搜索资源。offset/pageSize 分页。
     * @throws IOException / 解析错误
     */
    fun search(query: String, type: String?, offset: Int, pageSize: Int): List<MarketItem> {
        val urlBuilder = StringBuilder("$BASE/search?limit=$pageSize&offset=$offset")
        if (query.isNotBlank()) urlBuilder.append("&query=${urlEncode(query)}")
        if (type != null && type != "all") {
            // facets 需要转义引号
            urlBuilder.append("&facets=%5B%5B%22project_type%3A$type%22%5D%5D")
        }
        val body = requestBody(urlBuilder.toString())
        val hits = body.optJSONArray("hits") ?: JSONArray()
        val result = mutableListOf<MarketItem>()
        for (i in 0 until hits.length()) {
            val h = hits.optJSONObject(i) ?: continue
            val projectType = h.optString("project_type", "mod")
            val iconUrl = h.optString("icon_url", "").takeIf { it.isNotBlank() }
            result.add(
                MarketItem(
                    id = h.optString("project_id", "m_$i"),
                    title = h.optString("title", "untitled"),
                    description = h.optString("description", ""),
                    author = h.optString("author", ""),
                    type = projectType,
                    iconUrl = iconUrl,
                    gameVersions = h.optJSONArray("versions")?.let { ja ->
                        (0 until ja.length()).map { ja.optString(it) }
                    } ?: emptyList(),
                    downloadCount = h.optLong("downloads", 0),
                    source = "modrinth",
                    webUrl = "https://modrinth.com/${h.optString("project_type", "mod")}/${h.optString("slug", "")}"
                )
            )
        }
        return result
    }

    /**
     * 获取项目的某个版本信息（含下载地址/大小）。
     * @return 若找不到可用版本返回 null
     */
    fun getVersion(item: MarketItem): MarketItem? {
        val body = requestBody("$BASE/project/${item.id}/version")
        val versions = body.optJSONArray("version") ?: body // /project/{id}/version 返回数组
        if (versions is JSONArray && versions.length() > 0) {
            val v = versions.optJSONObject(0)
            val files = v?.optJSONArray("files") ?: return item
            if (files.length() > 0) {
                val file = files.optJSONObject(0)
                return item.copy(
                    version = v.optString("version_number", ""),
                    downloadUrl = file.optString("url", "").takeIf { it.isNotBlank() },
                    fileSize = file.optLong("size", 0),
                    gameVersions = v.optJSONArray("game_versions")?.let { ja ->
                        (0 until ja.length()).map { ja.optString(it) }
                    } ?: item.gameVersions
                )
            }
        }
        return item.copy(downloadUrl = null)
    }

    private fun requestBody(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CMAL/0.1 (contact: cmal@example.com)")
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("modrinth_http_${resp.code}")
            }
            return JSONObject(resp.body?.string() ?: "{}")
        }
    }

    private fun urlEncode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8")
}