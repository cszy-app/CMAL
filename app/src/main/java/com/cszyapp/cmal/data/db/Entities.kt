package com.cszyapp.cmal.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 已下载/可用的 Minecraft 版本记录
 */
@Entity(tableName = "mc_versions")
data class McVersion(
    @PrimaryKey val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val size: Long = 0,
    val downloaded: Boolean = false,
    val installed: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

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
 * 下载任务记录
 */
@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey val taskId: String,
    val url: String,
    val fileName: String,
    val savePath: String,
    val totalBytes: Long = 0,
    val doneBytes: Long = 0,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)
