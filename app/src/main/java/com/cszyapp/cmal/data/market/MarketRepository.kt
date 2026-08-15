package com.cszyapp.cmal.data.market

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资源市场仓库：聚合多个来源
 * - Modrinth：官方 API，无需 key
 * - MineBBS：预留（后续抓取解析）
 */
class MarketRepository(
    private val modrinth: ModrinthClient,
    private val mineBbs: MineBbsClient = MineBbsClient()
) {

    /** 搜索资源（Modrinth + MineBBS），阻塞式，需在 IO 线程 */
    suspend fun search(
        query: String,
        type: String?,
        offset: Int,
        pageSize: Int = 20
    ): List<MarketItem> = withContext(Dispatchers.IO) {
        // 先取 Modrinth 结果；MineBBS 解析失败时静默降级
        val modrinthHits = try {
            modrinth.search(query, type, offset, pageSize)
        } catch (e: Exception) {
            emptyList()
        }
        if (offset == 0 && modrinthHits.isEmpty()) {
            // 仅在首页无结果时尝试 MineBBS，避免重复请求
            try {
                mineBbs.search(query, type, offset, pageSize)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            modrinthHits
        }
    }

    /** 获取具体下载版本信息（Modrinth 项目版本） */
    suspend fun resolveDownload(item: MarketItem): MarketItem = withContext(Dispatchers.IO) {
        if (item.source == "minebbs") {
            mineBbs.getVersion(item) ?: item.copy(downloadUrl = null)
        } else {
            modrinth.getVersion(item) ?: item.copy(downloadUrl = null)
        }
    }
}