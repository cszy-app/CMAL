package com.cszyapp.cmal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 服务器收藏
 */
@Entity(tableName = "servers")
data class McServer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val port: Int = 19132,
    val featured: Boolean = false,
    val description: String = ""
)

/**
 * 本地皮肤
 */
@Entity(tableName = "skins")
data class McSkin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val localPath: String,
    val width: Int = 64,
    val height: Int = 64,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * 世界（存档）记录
 */
@Entity(tableName = "worlds")
data class McWorld(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val folderPath: String,
    val worldSize: Long = 0,
    val lastPlayed: Long = 0,
    val importedAt: Long = System.currentTimeMillis()
)

/**
 * 导入的资源包 / 行为包
 */
@Entity(tableName = "resources")
data class McResource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val filePath: String,
    val installed: Boolean = false,
    val importedAt: Long = System.currentTimeMillis()
)

/**
 * Minecraft 版本实例
 * 一个实例绑定一个已安装的 MC 包（不同版本/渠道包名不同）
 */
@Entity(tableName = "instances")
data class McInstance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String,
    val versionName: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
