package com.cszyapp.cmal.data.repo

import com.cszyapp.cmal.util.Preferences
import java.util.Locale

/**
 * 设置仓库：主题、语言、免责声明等应用设置
 */
class SettingsRepository(private val preferences: Preferences) {

    var themeMode: String
        get() = preferences.themeMode
        set(value) {
            preferences.themeMode = value
        }

    var accentColor: Long
        get() = preferences.accentColor
        set(value) {
            preferences.accentColor = value
        }

    var language: String
        get() = preferences.language
        set(value) {
            preferences.language = value
        }

    var disclaimerAccepted: Boolean
        get() = preferences.disclaimerAccepted
        set(value) {
            preferences.disclaimerAccepted = value
        }

    var onboarded: Boolean
        get() = preferences.onboarded
        set(value) {
            preferences.onboarded = value
        }

    fun currentLocale(): Locale = when (language) {
        "zh" -> Locale.SIMPLIFIED_CHINESE
        "en" -> Locale.ENGLISH
        else -> Locale.getDefault()
    }
}
