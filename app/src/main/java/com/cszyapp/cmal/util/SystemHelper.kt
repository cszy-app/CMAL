package com.cszyapp.cmal.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * 系统辅助工具
 * 负责检测已安装的 Minecraft、调用启动、申请未知来源安装权限等
 */
class SystemHelper(private val context: Context) {

    /** Minecraft Bedrock 官方包名 */
    companion object {
        const val MC_PACKAGE = "com.mojang.minecraftpe"
    }

    /** 是否已安装 Minecraft Bedrock */
    fun isMcInstalled(): Boolean = isPackageInstalled(MC_PACKAGE)

    /** 指定包是否已安装 */
    fun isPackageInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /** 已安装的 MC 版本号，未安装返回 null */
    fun installedMcVersion(): String? =
        try {
            context.packageManager.getPackageInfo(MC_PACKAGE, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    /** 启动已安装的 Minecraft */
    fun launchMc(): Boolean = launchPackage(MC_PACKAGE)

    /** 启动任意已安装的 Minecraft 系列包 */
    fun launchPackage(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            return false
        }
    }

    /** 打开安装未知应用设置页（Android 8.0+） */
    fun openUnknownAppSettings(packageName: String = context.packageName) {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openAppDetails()
        }
    }

    /** 打开应用详情页 */
    fun openAppDetails() {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    /** 文件选择器：选择 APK */
    fun pickApkIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
        }

    /** 文件选择器：选择资源包/皮肤/世界 */
    fun pickFileIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/mcpack",
                "application/mcaddon",
                "application/mcworld",
                "application/octet-stream",
                "*/*"
            ))
        }

    /** 通过包名调起某个 activity（用于皮肤预览等，当前未使用可忽略） */
    fun hasActivityFor(intent: Intent): Boolean =
        intent.resolveActivity(context.packageManager) != null

    fun resolveComponent(intent: Intent): ComponentName? =
        intent.resolveActivity(context.packageManager)
}
