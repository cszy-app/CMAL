package com.cszyapp.cmal.data.market

import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * mcpedl.org 资源客户端（Bedrock 版资源站）
 * - 列表/搜索：wp-json REST API，无需 key、无 WAF
 * - 下载：详情页取 fileId → /getfile/{id} 页面内嵌直链 → 直连下载
 *
 * 说明：
 * - 文件托管在 mcpedl 自家服务器，直连可下
 * - 分类由 slug 关键词推断（mods / texture / shader / map）
 */
class McpedlClient {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        private const val BASE = "https://mcpedl.org"
        private const val API = "$BASE/wp-json/wp/v2"
    }

    /**
     * 搜索资源（mcpedl 全站搜索）。
     * @throws IOException / 解析错误
     */
    fun search(query: String, type: String?, offset: Int, pageSize: Int): List<MarketItem> {
        val url = StringBuilder("$API/posts?per_page=$pageSize&offset=$offset")
        if (query.isNotBlank()) url.append("&search=").append(encode(query))
        return parsePosts(fetch(url.toString()), type)
    }

    /**
     * 按类型浏览最新资源。
     * 按分类 slug 关键词无法直接过滤 wp-json，故拉最新帖 + 客户端按类型关键词过滤。
     */
    fun browse(type: String, offset: Int, pageSize: Int): List<MarketItem> {
        val url = "$API/posts?per_page=$pageSize&offset=$offset&orderby=date&order=desc"
        return parsePosts(fetch(url), type)
    }

    /**
     * 获取下载信息：详情页表格取 fileId + 版本 → /getfile 页面取直链。
     * @return downloadUrl 为 mcpedl 自家服务器直链；失败返回 null
     */
    fun getDetail(item: MarketItem): MarketItem? {
        val web = item.webUrl ?: return item.copy(downloadUrl = null)
        val detailHtml = try {
            fetch(web)
        } catch (e: IOException) {
            return item.copy(downloadUrl = null)
        }
        // 详情页 #download-link 表格：form action="/getfile/{fileId}"，Version 列 = "最低 – 最高"
        val form = Regex("""<form[^>]*action="/getfile/(\d+)"""")
            .find(detailHtml)?.groupValues?.get(1) ?: return item.copy(downloadUrl = null)
        // 表格行 tbody tr：第一行 Name/Version/File，Version 是第 2 个 td
        val versionRange = Regex(
            """<section id="download-link"(?:(?!</section>).)*?<tbody>(?:(?!</tr>).)*?<td>([^<]*)</td>(?:(?!</tr>).)*?<td>([^<]*)</td>""",
            RegexOption.DOT_MATCHES_ALL
        ).find(detailHtml)?.groupValues?.get(2)
        val versions = parseVersionRange(versionRange)
        val direct = fetchGetFile(form) ?: return item.copy(downloadUrl = null, gameVersions = versions)
        return item.copy(downloadUrl = direct, gameVersions = versions, version = versions.firstOrNull(), fileSize = probeSize(direct))
    }

    /** POST /getfile/{id} → 解析内嵌直链 window.location.href='...' */
    private fun fetchGetFile(fileId: String): String? {
        val html = try {
            fetch("$BASE/getfile/$fileId")
        } catch (e: Exception) {
            return null
        }
        return Regex("""window\.location\.href='([^']+)'""")
            .find(html)?.groupValues?.get(1)
            ?: Regex("""window\.location\.href="([^"]+)"""")
                .find(html)?.groupValues?.get(1)
    }

    /** "1.19.0 – 1.26.44" / "1.20+" / "1.21" → 拆分版本列表，取首个为最低 */
    private fun parseVersionRange(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val parts = raw.split(Regex("""[\s–—~-]+""")).filter { it.matches(Regex("""\d+(\.\d+){0,3}""")) }
        return parts
    }

    /** 探测直连文件大小（HEAD，被拒时退化为 Range=0-0 读 Content-Range） */
    private fun probeSize(url: String): Long {
        try {
            val head = okhttp3.Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) CMAL/0.1")
                .head()
                .build()
            client.newCall(head).execute().use { resp ->
                val cl = resp.body?.contentLength() ?: 0L
                if (cl > 0) return cl
            }
            val range = okhttp3.Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) CMAL/0.1")
                .header("Range", "bytes=0-0")
                .build()
            client.newCall(range).execute().use { resp ->
                val total = resp.header("Content-Range")?.substringAfter("/")?.toLongOrNull() ?: 0L
                if (total > 0) return total
            }
        } catch (_: Exception) {
        }
        return 0L
    }

    private fun parsePosts(json: String, type: String?): List<MarketItem> {
        val array = JSONArray(json)
        val result = mutableListOf<MarketItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val link = obj.optString("link", "")
            val id = obj.optLong("id", -1)
            if (id < 0 || link.isBlank()) continue
            val title = stripHtml(obj.optJSONObject("title")?.optString("rendered", "") ?: "untitled")
            val content = obj.optJSONObject("content")?.optString("rendered", "") ?: ""
            // 排除游戏本体 APK 页（非资源）
            if (title.contains("APK", ignoreCase = true) &&
                (title.contains("Download Minecraft", ignoreCase = true) || title.contains("Minecraft PE", ignoreCase = true))
            ) continue
            val itemType = inferType(title, content)
            if (type != null && type != "all" && itemType != type) continue
            val icon = Regex("""src="([^"]+\.(?:png|jpg|jpeg|webp))"""")
                .find(content)?.groupValues?.get(1)
            result.add(
                MarketItem(
                    id = "mcpedl_$id",
                    title = title,
                    description = stripHtml(content).replace(Regex("\\s+"), " ").take(160),
                    author = "",
                    type = itemType,
                    iconUrl = icon,
                    source = "mcpedl",
                    webUrl = link,
                    fileSize = 0
                )
            )
        }
        return result
    }

    /** 通过标题/内容关键词推断类型 */
    private fun inferType(title: String, content: String): String {
        val text = (title + " " + content).lowercase()
        return when {
            text.contains("shader") -> "shader"
            text.contains("texture") || text.contains("resource pack") || text.contains("resourcepack") -> "resourcepack"
            text.contains("world") || text.contains(" map") || text.contains("map ") -> "world"
            text.contains("datapack") -> "datapack"
            else -> "mod"
        }
    }

    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun fetch(url: String): String {
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) CMAL/0.1")
            .header("Accept", "application/json, text/html")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("mcpedl_http_${resp.code}")
            return resp.body?.string() ?: throw IOException("mcpedl_empty")
        }
    }
}