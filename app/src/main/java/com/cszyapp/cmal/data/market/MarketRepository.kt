package com.cszyapp.cmal.data.market

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资源市场仓库：聚合多个来源
 * - Modrinth：官方 API，无需 key，支持搜索 + 直连下载
 * - McFun（原 mcshuo）：中文资源站，JSON-LD 解析，按类型浏览，下载走网盘
 */
class MarketRepository(
    private val modrinth: ModrinthClient,
    private val mcFun: McFunClient = McFunClient()
) {

    /** 搜索资源（Modrinth）。McFun 不支持搜索，仅按类型浏览。 */
    suspend fun search(
        query: String,
        type: String?,
        offset: Int,
        pageSize: Int = 20
    ): List<MarketItem> = withContext(Dispatchers.IO) {
        try {
            modrinth.search(query, type, offset, pageSize)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 按类型浏览（McFun 中文资源，网盘下载）。用于"发现"页。 */
    suspend fun browse(type: String, offset: Int, pageSize: Int = 20): List<MarketItem> =
        withContext(Dispatchers.IO) {
            try {
                mcFun.browse(type, offset, pageSize)
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** 获取具体下载版本信息 */
    suspend fun resolveDownload(item: MarketItem): MarketItem = withContext(Dispatchers.IO) {
        if (item.source == "mcfun") {
            mcFun.getDetail(item) ?: item.copy(downloadUrl = null)
        } else {
            modrinth.getVersion(item) ?: item.copy(downloadUrl = null)
        }
    }
}