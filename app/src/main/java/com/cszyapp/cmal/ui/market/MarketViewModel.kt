package com.cszyapp.cmal.ui.market

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.download.DownloadState
import com.cszyapp.cmal.data.market.MarketItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 资源市场 ViewModel
 * 搜索/浏览第三方资源，下载并导入
 */
class MarketViewModel(private val container: AppContainer) : ViewModel() {

    var items by mutableStateOf<List<MarketItem>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var query by mutableStateOf("")
        private set

    var selectedType by mutableStateOf("all")
        private set

    var tasks by mutableStateOf<Map<String, DownloadState>>(emptyMap())
        private set

    init {
        viewModelScope.launch {
            container.downloadManager.tasks.collectLatest { tasks = it }
        }
    }

    fun updateQuery(q: String) {
        query = q
    }

    fun setType(type: String) {
        selectedType = type
        search(offset = 0)
    }

    fun search(offset: Int = 0) {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                items = container.marketRepository.search(query, selectedType.takeIf { it != "all" }, offset)
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    /** 下载并导入到 Minecraft */
    fun downloadAndInstall(item: MarketItem) {
        viewModelScope.launch {
            error = null
            try {
                val resolved = container.marketRepository.resolveDownload(item)
                if (resolved.downloadUrl.isNullOrBlank()) {
                    error = "no_download_url"
                    return@launch
                }
                container.downloadManager.startDownload(resolved)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun cancelDownload(taskId: String) {
        container.downloadManager.cancelDownload(taskId)
    }

    fun removeTask(taskId: String) {
        container.downloadManager.removeTask(taskId)
    }

    /** 全部已下载文件（用于导入） */
    fun completedFiles() = container.downloadManager.completedFiles()

    fun clearError() {
        error = null
    }
}