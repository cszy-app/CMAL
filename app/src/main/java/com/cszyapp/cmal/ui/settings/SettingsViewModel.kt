package com.cszyapp.cmal.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 设置页 ViewModel */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    var themeMode by mutableStateOf(container.settingsRepository.themeMode)
        private set

    var accentColor by mutableStateOf(container.settingsRepository.accentColor)
        private set

    var language by mutableStateOf(container.settingsRepository.language)
        private set

    var updateInfo by mutableStateOf<String?>(null)
        private set

    var checkingUpdate by mutableStateOf(false)
        private set

    var downloadDirSize by mutableStateOf(0L)
        private set

    init {
        downloadDirSize = container.downloadManager.downloadDirSize()
    }

    fun clearDownloads() {
        container.downloadManager.clearFinishedDownloads()
        downloadDirSize = container.downloadManager.downloadDirSize()
    }

    fun updateThemeMode(mode: String) {
        themeMode = mode
        container.settingsRepository.themeMode = mode
    }

    fun updateAccentColor(color: Long) {
        accentColor = color
        container.settingsRepository.accentColor = color
    }

    fun updateLanguage(lang: String) {
        language = lang
        container.settingsRepository.language = lang
        // 简单重启方式：通过重启 Activity 由 MainActivity 处理
    }

    fun checkUpdate() {
        if (checkingUpdate) return
        checkingUpdate = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                container.updateChecker.check()
            }
            checkingUpdate = false
            updateInfo = result.getOrNull()?.let { "update:${it.versionName}" } ?: "up_to_date"
        }
    }
}
