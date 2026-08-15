package com.cszyapp.cmal.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration

/**
 * 轻量偏好设置封装
 * 基于 SharedPreferences，用于存储主题、语言、引导状态、源地址等
 */
class Preferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cmal_settings", Context.MODE_PRIVATE)

    /** 是否已看过引导页 */
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** 主题模式: system / light / dark */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    /** 自定义主题色（十六进制，无 #） */
    var accentColor: Long
        get() = prefs.getLong(KEY_ACCENT_COLOR, DEFAULT_ACCENT)
        set(value) = prefs.edit().putLong(KEY_ACCENT_COLOR, value).apply()

    /** 语言: auto / zh / en */
    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    /** 自定义下载源列表，JSON 数组 */
    var customSources: String
        get() = prefs.getString(KEY_CUSTOM_SOURCES, "[]") ?: "[]"
        set(value) = prefs.edit().putString(KEY_CUSTOM_SOURCES, value).apply()

    /** 下载并发线程数 */
    var downloadThreads: Int
        get() = prefs.getInt(KEY_DOWNLOAD_THREADS, 8)
        set(value) = prefs.edit().putInt(KEY_DOWNLOAD_THREADS, value).apply()

    /** 免责声明是否已同意 */
    var disclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, value).apply()

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String = ""): String =
        prefs.getString(key, default) ?: default

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int =
        prefs.getInt(key, default)

    companion object {
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_LANGUAGE = "language"
        const val KEY_CUSTOM_SOURCES = "custom_sources"
        const val KEY_DOWNLOAD_THREADS = "download_threads"
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        const val KEY_SOURCE_VERSION_INDEX = "source_version_index"

        /** 默认琥珀色（Citrine） */
        const val DEFAULT_ACCENT = 0xFFF5A623L

        /** 是否深色模式（用于 ViewModel 判断） */
        fun isDarkMode(context: Context): Boolean {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return mode == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
