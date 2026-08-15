package com.cszyapp.cmal.data.market

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 资源市场仓库：聚合多个来源
 * - Modrinth：官方 API，无需 key
 * - MineBBS：预留（后续抓取解析）
 */
class MarketRepository(private val modrinth: ModrinthClient) {

    /** 搜索资源（Modrinth），阻塞式，需在 IO 线程 */
    suspend fun search(
        query: String,
        type: String?,
        offset: Int,
        pageSize: Int = 20
    ): List<MarketItem> = withContext(Dispatchers.IO) {
        modrinth.search(query, type, offset, pageSize)
    }

    /** 获取具体下载版本信息（Modrinth 项目版本） */
    suspend fun resolveDownload(item: MarketItem): MarketItem = withContext(Dispatchers.IO) {
        modrinth.getVersion(item) ?: item.copy(downloadUrl = null)
    }
}