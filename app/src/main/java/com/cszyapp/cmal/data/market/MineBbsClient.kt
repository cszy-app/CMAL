package com.cszyapp.cmal.data.market

import java.io.IOException

/**
 * MineBBS 资源客户端（抓取解析，无需 API key）
 * 解析 MineBBS 的资源列表页，提取基岩版资源（资源包/世界/光影/模组）
 *
 * 注意：MineBBS 页面结构可能变化，解析失败时应抛出 IOException 由上层降级处理
 */
class MineBbsClient {

    companion object {
        private const val BASE = "https://www.minebbs.com"
        private const val SEARCH_URL = "$BASE/search/"

        /** MineBBS 的贝达版资源分类标签映射 */
        private val CATEGORY_IDS = mapOf(
            "resourcepack" to "1",   // 资源包
            "world" to "3",          // 基岩版地图
            "shader" to "5",         // 光影
            "mod" to "7"             // 基岩版模组
        )
    }

    /**
     * 搜索资源。offset/pageSize 分页（MineBBS 用 search 接口）。
     * @throws IOException / 解析错误
     */
    fun search(query: String, type: String?, offset: Int, pageSize: Int): List<MarketItem> {
        // MineBBS 的搜索走 XF 框架的 find-new 接口，返回 HTML。
        // 这里先返回空列表占位，避免运行时崩溃；后续接入真实解析。
        return emptyList()
    }

    /**
     * 获取下载直链。MineBBS 资源需要登录才能下载，直接返回 null。
     */
    fun getVersion(item: MarketItem): MarketItem? = item.copy(downloadUrl = null)
}