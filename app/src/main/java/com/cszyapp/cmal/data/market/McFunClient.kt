package com.cszyapp.cmal.data.market

import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * McFun（原 mcshuo）资源客户端
 * 解析 JSON-LD（schema.org ItemList），无需 API key、无 WAF
 *
 * 说明：
 * - 支持按类型浏览最新资源（mod / resource_pack / map / modpack）
 * - 下载走网盘（夸克等），无法直连，故提供网盘跳转；详情页可再取 downloadUrl
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
     * 获取单个资源详情（含网盘下载地址）。
     * @return downloadUrl 为网盘链接，非直连；找不到返回 item
     */
    fun getDetail(item: MarketItem): MarketItem? {
        val id = item.id.removePrefix("mcfun_")
        val html = try {
            fetch("$BASE/resource/$id")
        } catch (e: IOException) {
            return item
        }
        // 详情页 JSON-LD 含 downloadUrl
        val m = Regex(""""downloadUrl"\s*:\s*"([^"]+)"""").find(html)
        val dl = m?.groupValues?.get(1)
        return if (!dl.isNullOrBlank()) item.copy(downloadUrl = dl) else item
    }

    private fun parseItemList(html: String, type: String): List<MarketItem> {
        val result = mutableListOf<MarketItem>()
        // 抓取第一个 ItemList 的 JSON-LD
        val blockMatch = Regex(""""@type"\s*:\s*"ItemList".*?\}""").find(html, ignoreCase = false)
        val block = blockMatch?.value ?: return result
        // 提取 itemListElement 数组（尽量宽松）
        val arrStart = block.indexOf("""itemListElement""")
        if (arrStart < 0) return result
        val arrJson = extractBalanced(block, arrStart + block.substring(arrStart).indexOf('['))
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
}