package com.cszyapp.cmal.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cszyapp.cmal.data.AppContainer

/** 首页 ViewModel */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    var mcInstalled by mutableStateOf(container.systemHelper.isMcInstalled())
        private set

    var installedVersion by mutableStateOf(container.systemHelper.installedMcVersion())
        private set

    fun launchMc() {
        container.systemHelper.launchMc()
    }

    fun refresh() {
        mcInstalled = container.systemHelper.isMcInstalled()
        installedVersion = container.systemHelper.installedMcVersion()
    }
}