package com.cszyapp.cmal.data.market

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资源市场仓库：聚合多个来源
 * - McFun（原 mcshuo）：中文基岩版资源站，列表 JSON-LD 解析，下载走官方接口直连
 * - mcpedl.org：英文 Bedrock 资源站，wp-json 搜索 + 详情页直链
 *
 * 下载统一走直连（绕过网盘）；列表按来源聚合，卡片显示来源徽标。
 */
class MarketRepository(
    private val mcFun: McFunClient = McFunClient(),
    private val mcpedl: McpedlClient = McpedlClient()
) {

    /**
     * 统一入口搜索。
     * - 有搜索词：mcpedl 支持搜索；McFun 无资源搜索 API，跳过
     * - 无搜索词：两源按类型浏览最新，聚合返回
     */
    suspend fun search(
        query: String,
        type: String?,
        offset: Int,
        pageSize: Int = 20
    ): List<MarketItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MarketItem>()
        if (query.isNotBlank()) {
            try { results += mcpedl.search(query, type, offset, pageSize) } catch (e: Exception) {}
        } else {
            try { results += mcFun.browse(type ?: "mod", offset, pageSize) } catch (e: Exception) {}
            try { results += mcpedl.browse(type, offset, pageSize) } catch (e: Exception) {}
        }
        results
    }

    /** 获取具体下载信息（直连 URL、版本） */
    suspend fun resolveDownload(item: MarketItem): MarketItem = withContext(Dispatchers.IO) {
        when (item.source) {
            "mcfun" -> mcFun.getDetail(item) ?: item.copy(downloadUrl = null)
            "mcpedl" -> mcpedl.getDetail(item) ?: item.copy(downloadUrl = null)
            else -> item.copy(downloadUrl = null)
        }
    }
}