package com.cszyapp.cmal.data.market

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * McFun（原 mcshuo）资源客户端
 * - 列表：解析 JSON-LD（schema.org ItemList），无需 API key、无 WAF
 * - 下载：调官方接口取直连 URL（绕过网盘）
 *
 * 说明：
 * - 支持按类型浏览最新资源（mod / resource_pack / map / modpack）
 * - 详情接口返回网盘 + "其他下载"（第三方 CDN 直连），应用内直接取直连 URL
 */
class McFunClient {

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE = "https://www.mcshuo.com"

        /** McFun 的类型路径 → MarketItem.type */
        private val TYPE_PATHS = mapOf(
            "mod" to "mod",
            "resource_pack" to "resourcepack",
            "map" to "world",
            "modpack" to "modpack"
        )

        /**
         * 落地页 / 网盘 / 短链 host 特征。
         * 实测 McFun "其他下载" 有时指向 makily.com 的 MCloud HTML 落地页（200 text/html），
         * 下载会得到 HTML 而非资源文件，必须按 host 拦截（note 字段不可靠）。
         */
        private val BLOCKED_HOST_HINTS = listOf(
            "makily", "pan.quark.cn", "pan.baidu.com", "weiyun", "lanzou", "123pan",
            "aliyundrive", "drive.uc.cn", "cloud.189.cn", "tianyi", "kdocs.cn",
            "t.cn", "dwz.cn", "url.cn", "jd.com", "s.mcshuo", "mcpan", "mcloud"
        )
    }

    /**
     * 按类型浏览 McFun 最新资源（不支持搜索）。
     * @throws IOException / 解析错误
     */
    fun browse(type: String, offset: Int, pageSize: Int): List<MarketItem> {
        val typePath = TYPE_PATHS.entries.firstOrNull { it.value == type }?.key ?: return emptyList()
        val html = fetch("$BASE/resources/type/$typePath")
        return parseItemList(html, type)
    }

    /**
     * 获取单个资源详情（直连下载 URL）。
     * 调官方下载接口，取非网盘（note != 夸克网盘）的直连链接。
     * @return 找不到直连返回 item（downloadUrl 为 null）
     */
    fun getDetail(item: MarketItem): MarketItem? {
        val id = item.id.removePrefix("mcfun_")
        var versions: List<String> = item.gameVersions
        try {
            val html = fetch("$BASE/resource/$id")
            val v = Regex(""""softwareVersion"\s*:\s*(\[[^\]]*\])""").find(html)
            if (v != null) {
                val arr = JSONArray(v.groupValues[1])
                versions = (0 until arr.length()).map { arr.optString(it) }
            }
        } catch (e: Exception) {
            // 版本解析失败不阻塞下载
        }
        try {
            val body = postJson("$BASE/api/v1/resource/$id/download", "{}")
            val success = body.optBoolean("success", false)
            if (!success) return item.copy(downloadUrl = null, gameVersions = versions)
            val links = body.optJSONObject("data")?.optJSONArray("links") ?: return item.copy(downloadUrl = null, gameVersions = versions)
            for (i in 0 until links.length()) {
                val l = links.optJSONObject(i) ?: continue
                val url = l.optString("url", "").trim()
                if (url.isBlank()) continue
                val note = l.optString("note", "")
                if (note.contains("网盘") || note.contains("盘")) continue
                if (!isDirectHost(url)) continue
                return item.copy(downloadUrl = url, gameVersions = versions, version = versions.firstOrNull(), fileSize = probeSize(url))
            }
            return item.copy(downloadUrl = null, gameVersions = versions)
        } catch (e: Exception) {
            return item.copy(downloadUrl = null, gameVersions = versions)
        }
    }

    /** 仅接受可直连下载的 http(s) 链接，拦截落地页/网盘/短链 host */
    private fun isDirectHost(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val host = try { java.net.URI(url).host?.lowercase() ?: "" } catch (e: Exception) { "" }
        if (host.isBlank()) return false
        return BLOCKED_HOST_HINTS.none { host.contains(it) }
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

    private fun parseItemList(html: String, type: String): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        // 直接定位 itemListElement 数组
        val keyIdx = html.indexOf("\"itemListElement\"")
        if (keyIdx < 0) return result
        val arrStart = html.indexOf('[', keyIdx)
        if (arrStart < 0) return result
        val arrJson = extractBalanced(html, arrStart)
        if (arrJson.isBlank()) return result
        try {
            val array = JSONArray(arrJson)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val item = obj.optJSONObject("item") ?: continue
                val url = item.optString("url", "")
                val id = Regex("""/resource/(\d+)""").find(url)?.groupValues?.get(1) ?: continue
                result.add(
                    MarketItem(
                        id = "mcfun_$id",
                        title = item.optString("name", "untitled"),
                        description = item.optString("description", "")
                            .replace(Regex("[\\r\\n]+"), " ")
                            .replace(Regex("#{1,6}\\s*"), "")
                            .take(160),
                        author = item.optString("author", ""),
                        type = type,
                        iconUrl = item.optString("image", "").takeIf { it.isNotBlank() },
                        source = "mcfun",
                        webUrl = url
                    )
                )
            }
        } catch (e: Exception) {
            // 解析失败静默降级
        }
        return result
    }

    /** 从 JSON 里平衡提取括号内的内容（自 JSON 下标起），正确处理字符串转义 */
    private fun extractBalanced(s: String, startIdx: Int): String {
        if (startIdx < 0 || startIdx >= s.length) return ""
        var depth = 0
        val sb = StringBuilder()
        var i = startIdx
        while (i < s.length) {
            val c = s[i]
            when (c) {
                '[' -> { depth++; sb.append(c); i++ }
                ']' -> {
                    depth--
                    sb.append(c)
                    if (depth == 0) return sb.toString()
                    i++
                }
                '"' -> {
                    // 原样拷贝整个字符串（含转义），跳过内部所有字符
                    sb.append(c)
                    var j = i + 1
                    while (j < s.length) {
                        val sc = s[j]
                        sb.append(sc)
                        if (sc == '\\') {
                            if (j + 1 < s.length) { sb.append(s[j + 1]); j += 2 } else { j++ }
                        } else if (sc == '"') {
                            j++
                            break
                        } else {
                            j++
                        }
                    }
                    i = j
                }
                else -> { sb.append(c); i++ }
            }
        }
        return ""
    }

    private fun fetch(url: String): String {
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) CMAL/0.1")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("mcfun_http_${resp.code}")
            return resp.body?.string() ?: throw IOException("mcfun_empty")
        }
    }

    /** POST JSON，返回 JSONObject */
    private fun postJson(url: String, json: String): JSONObject {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) CMAL/0.1")
            .header("Content-Type", "application/json")
            .post(json.toRequestBody(mediaType))
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("mcfun_http_${resp.code}")
            return JSONObject(resp.body?.string() ?: "{}")
        }
    }
}