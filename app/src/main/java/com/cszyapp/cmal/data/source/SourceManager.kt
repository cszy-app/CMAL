package com.cszyapp.cmal.data.source

import com.cszyapp.cmal.util.Preferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 下载源管理
 * 维护可配置的 Minecraft 安装包下载源列表
 * 支持官方镜像源与自定义源
 */
data class DownloadSource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("baseUrl", baseUrl)
            .put("enabled", enabled)

    companion object {
        fun fromJson(obj: JSONObject): DownloadSource =
            DownloadSource(
                id = obj.optString("id"),
                name = obj.optString("name"),
                baseUrl = obj.optString("baseUrl"),
                enabled = obj.optBoolean("enabled", true)
            )
    }
}

class SourceManager(private val preferences: Preferences) {

    /** 内置默认源：版本索引与镜像 */
    companion object {
        const val DEFAULT_SOURCE_ID = "official"

        // 版本索引接口：返回 JSON 数组 [{"name":"1.21.4","code":12300000,"url":"...","size":...}]
        const val DEFAULT_INDEX_URL = "https://raw.githubusercontent.com/cszy-app/CMAL/main/mc_versions.json"

        // 官方版本镜像（仅作示例/占位，实际可配置）
        const val DEFAULT_BASE_URL = "https://raw.githubusercontent.com/cszy-app/CMAL/main/mirror"
    }

    /** 获取当前生效的源列表（默认源 + 自定义源） */
    fun getSources(): List<DownloadSource> {
        val list = mutableListOf(
            DownloadSource(DEFAULT_SOURCE_ID, "Official Mirror", DEFAULT_BASE_URL)
        )
        list += parseCustom()
        return list.filter { it.enabled }
    }

    /** 获取版本索引 URL 列表 */
    fun getIndexUrls(): List<String> {
        val urls = mutableListOf(DEFAULT_INDEX_URL)
        urls += getSources()
            .filter { it.id != DEFAULT_SOURCE_ID }
            .map { it.baseUrl.trimEnd('/') + "/index.json" }
        return urls
    }

    /** 解析自定义源 */
    private fun parseCustom(): List<DownloadSource> {
        val raw = preferences.customSources
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                DownloadSource.fromJson(arr.getJSONObject(i))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 添加自定义源 */
    fun addSource(name: String, url: String): Boolean {
        if (name.isBlank() || url.isBlank()) return false
        val sources = parseCustom().toMutableList()
        sources += DownloadSource(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            baseUrl = url.trimEnd('/')
        )
        val arr = JSONArray()
        sources.forEach { arr.put(it.toJson()) }
        preferences.customSources = arr.toString()
        return true
    }

    /** 删除自定义源 */
    fun removeSource(id: String) {
        val sources = parseCustom().filterNot { it.id == id }
        val arr = JSONArray()
        sources.forEach { arr.put(it.toJson()) }
        preferences.customSources = arr.toString()
    }
}
