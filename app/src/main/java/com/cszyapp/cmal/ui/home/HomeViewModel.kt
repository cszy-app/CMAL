package com.cszyapp.cmal.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McVersion
import com.cszyapp.cmal.data.download.DownloadProgress
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 首页 ViewModel */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    var versions by mutableStateOf<List<McVersion>>(emptyList())
        private set

    var activeDownloads by mutableStateOf<List<DownloadProgress>>(emptyList())
        private set

    var mcInstalled by mutableStateOf(container.systemHelper.isMcInstalled())
        private set

    var installedVersion by mutableStateOf(container.systemHelper.installedMcVersion())
        private set

    var refreshing by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            container.versionsRepository.observeVersions().collectLatest { list ->
                versions = list
            }
        }
        viewModelScope.launch {
            container.downloadManager.activeTasks.collectLatest { list ->
                activeDownloads = list
            }
        }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        error = null
        viewModelScope.launch {
            val result = container.versionsRepository.refreshFromSources()
            refreshing = false
            result.onFailure { e ->
                error = e.message
            }
        }
    }

    /** 已安装版本的最新记录 */
    fun latestDownloaded(): McVersion? = versions.firstOrNull { it.downloaded }

    fun installLatestDownloaded() {
        val v = latestDownloaded() ?: return
        if (!container.systemHelper.isMcInstalled()) return
        container.systemHelper.launchMc()
    }

    fun launchMc() {
        container.systemHelper.launchMc()
    }
}
