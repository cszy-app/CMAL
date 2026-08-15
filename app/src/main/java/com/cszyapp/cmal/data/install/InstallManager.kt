package com.cszyapp.cmal.data.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 安装管理器
 * 使用标准 Intent（FileProvider + ACTION_VIEW）发起 APK 安装，
 * 符合 Android 8.0+ 的"允许安装未知应用"权限流程，最安全。
 */
class InstallManager(private val context: Context) {

    companion object {
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }

    /**
     * 生成用于安装的 content Uri
     */
    fun contentUriFor(file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}$AUTHORITY_SUFFIX",
            file
        )

    /**
     * 发起安装意图（标准流程，由系统 PackageInstaller 处理）
     * @return Intent 以便 startActivity
     */
    fun createInstallIntent(apkFile: File): Intent {
        val uri = contentUriFor(apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * 通过内容 Uri 打开资源包（交给 MC 或系统处理）
     */
    fun createOpenIntent(uri: Uri, mimeType: String = "application/octet-stream"): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
