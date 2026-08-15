package com.cszyapp.cmal.ui.resources

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McResource
import com.cszyapp.cmal.data.db.McSkin
import com.cszyapp.cmal.data.db.McWorld
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 资源页 ViewModel：资源包/皮肤/世界 */
class ResourcesViewModel(private val container: AppContainer) : ViewModel() {

    var resources by mutableStateOf<List<McResource>>(emptyList())
        private set

    var skins by mutableStateOf<List<McSkin>>(emptyList())
        private set

    var worlds by mutableStateOf<List<McWorld>>(emptyList())
        private set

    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            container.resourcesRepository.observeAll().collectLatest { resources = it }
        }
        viewModelScope.launch {
            container.skinsRepository.observeAll().collectLatest { skins = it }
        }
        viewModelScope.launch {
            container.worldsRepository.observeAll().collectLatest { worlds = it }
        }
    }

    fun deleteResource(r: McResource) {
        viewModelScope.launch {
            container.resourcesRepository.delete(r)
        }
    }

    fun deleteSkin(s: McSkin) {
        viewModelScope.launch {
            container.skinsRepository.delete(s)
        }
    }

    fun deleteWorld(w: McWorld) {
        viewModelScope.launch {
            container.worldsRepository.delete(w)
        }
    }

    fun clearMessage() {
        message = null
    }
}
