package com.cszyapp.cmal.ui.download

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

/** 下载页 ViewModel */
class DownloadViewModel(private val container: AppContainer) : ViewModel() {

    var versions by mutableStateOf<List<McVersion>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var activeDownloads by mutableStateOf<List<DownloadProgress>>(emptyList())
        private set

    var refreshing by mutableStateOf(false)
        private set

    private val _pendingInstalls = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val pendingInstalls: kotlinx.coroutines.flow.StateFlow<String?> = _pendingInstalls

    val filteredVersions: List<McVersion>
        get() {
            val q = searchQuery.trim()
            return if (q.isEmpty()) versions
            else versions.filter { it.versionName.contains(q, ignoreCase = true) }
        }

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

    fun setQuery(q: String) {
        searchQuery = q
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            container.versionsRepository.refreshFromSources()
            refreshing = false
        }
    }

    /** 下载某个版本（若已有记录则复用） */
    fun downloadVersion(version: McVersion) {
        val existing = versions.firstOrNull { it.versionCode == version.versionCode }
        if (existing?.downloaded == true) return
        container.downloadManager.enqueue(
            url = version.downloadUrl,
            fileName = "${version.versionName}.apk"
        ) { path ->
            // 下载完成
            viewModelScope.launch {
                container.versionsRepository.markDownloaded(version)
            }
        }
    }

    fun cancelDownload(taskId: String) {
        container.downloadManager.cancel(taskId)
    }

    /** 下载 APK 后立即安装 */
    fun downloadAndInstall(version: McVersion) {
        container.downloadManager.enqueue(
            url = version.downloadUrl,
            fileName = "${version.versionName}.apk"
        ) { path ->
            viewModelScope.launch {
                container.versionsRepository.markDownloaded(version)
                _pendingInstalls.value = path
            }
        }
    }

    /** 消费"待安装"事件（UI 触发安装后重置） */
    fun consumePendingInstall() {
        _pendingInstalls.value = null
    }
}
