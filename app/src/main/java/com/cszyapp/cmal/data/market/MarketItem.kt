package com.cszyapp.cmal.data.market

/**
 * 资源中心条目（多源统一模型）
 * source: "mcfun" / "mcpedl" / "local"
 */
data class MarketItem(
    val id: String,
    val title: String,
    val description: String,
    val author: String,
    val type: String,          // addon / resourcepack / world / skin / shader / mod
    val iconUrl: String? = null,
    val downloadUrl: String? = null,
    val fileSize: Long = 0,
    val version: String? = null,
    val gameVersions: List<String> = emptyList(),
    val source: String,        // mcfun / mcpedl
    val webUrl: String? = null
)