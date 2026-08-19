package com.cszyapp.cmal.data.repo

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.McResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 资源仓库：管理 .mcpack / .mcaddon 资源包与行为包
 */
class ResourcesRepository(private val context: Context) {

    private val database = AppDatabase.get(context)

    fun observeAll(): Flow<List<McResource>> = database.resourceDao().observeAll()

    /** 资源包存储目录 */
    val packsDir: File
        get() = File(context.getExternalFilesDir(null), "packs").apply { mkdirs() }

    /**
     * 从 content Uri 导入资源包
     * 返回导入的记录，失败抛异常
     */
    suspend fun importPack(uri: Uri): McResource = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = queryName(resolver, uri) ?: "pack_${System.currentTimeMillis()}"
        val safeName = File(name).name
        val type = when {
            safeName.endsWith(".mcpack", true) -> "resource"
            else -> "behavior"
        }
        val target = File(packsDir, safeName)
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("cannot_open_uri")

        val resource = McResource(
            name = safeName.substringBeforeLast('.'),
            type = type,
            filePath = target.absolutePath
        )
        database.resourceDao().upsert(resource)
        resource
    }

    suspend fun delete(resource: McResource) {
        try {
            File(resource.filePath).delete()
        } catch (_: Exception) {
        }
        database.resourceDao().delete(resource)
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
