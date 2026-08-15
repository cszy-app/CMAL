package com.cszyapp.cmal.ui.imports

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 导入处理器：处理 .mcpack/.mcaddon/.mcworld/.png 皮肤 以及 APK
 */
class ImportHandler(private val context: Context, private val container: AppContainer) {

    /** 导入外部文件 */
    suspend fun handle(uri: Uri) {
        val name = queryName(uri) ?: "import_${System.currentTimeMillis()}"
        val lower = name.lowercase()
        when {
            lower.endsWith(".mcpack") || lower.endsWith(".mcaddon") || lower.endsWith(".mcworld") ->
                importPack(uri)
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ->
                importSkin(uri, name)
            else ->
                Toast.makeText(context, R.string.unsupported_file, Toast.LENGTH_SHORT).show()
        }
    }

    /** 导入资源包/行为包/世界 */
    private suspend fun importPack(uri: Uri) {
        try {
            val resource = container.resourcesRepository.importPack(uri)
            Toast.makeText(context, context.getString(R.string.imported_ok, resource.name), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.import_fail, Toast.LENGTH_SHORT).show()
        }
    }

    /** 导入皮肤 */
    private suspend fun importSkin(uri: Uri, name: String) {
        try {
            val dir = File(context.getExternalFilesDir(null), "skins").apply { mkdirs() }
            val file = File(dir, File(name).name)
            copyUri(uri, file)
            container.skinsRepository.add(
                com.cszyapp.cmal.data.db.McSkin(name = name, localPath = file.absolutePath)
            )
            Toast.makeText(context, R.string.skin_imported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.import_fail, Toast.LENGTH_SHORT).show()
        }
    }

    /** 复制 Uri 内容到文件，返回文件路径或 null */
    suspend fun copyUriToFile(uri: Uri, name: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.getExternalFilesDir(null), "tmp").apply { mkdirs() }
            val file = File(dir, name)
            copyUri(uri, file)
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun copyUri(uri: Uri, target: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("cannot open uri")
    }

    private fun queryName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
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
