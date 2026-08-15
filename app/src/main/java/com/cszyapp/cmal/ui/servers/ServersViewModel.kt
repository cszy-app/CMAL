package com.cszyapp.cmal.ui.servers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McServer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 服务器页 ViewModel */
class ServersViewModel(private val container: AppContainer) : ViewModel() {

    var servers by mutableStateOf<List<McServer>>(emptyList())
        private set

    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            container.serversRepository.observeAll().collectLatest { servers = it }
        }
    }

    fun add(name: String, address: String, port: Int): Boolean {
        if (name.isBlank() || address.isBlank()) return false
        viewModelScope.launch {
            container.serversRepository.add(McServer(name = name, address = address, port = port))
        }
        return true
    }

    fun update(server: McServer) {
        viewModelScope.launch {
            container.serversRepository.update(server)
        }
    }

    fun delete(server: McServer) {
        viewModelScope.launch {
            container.serversRepository.delete(server)
        }
    }

    fun clearMessage() {
        message = null
    }
}
