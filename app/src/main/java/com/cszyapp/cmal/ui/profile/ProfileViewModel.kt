package com.cszyapp.cmal.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cszyapp.cmal.data.AppContainer

/** 我的页 ViewModel */
class ProfileViewModel(private val container: AppContainer) : ViewModel() {

    var mcInstalled by mutableStateOf(container.systemHelper.isMcInstalled())
        private set

    var installedVersion by mutableStateOf(container.systemHelper.installedMcVersion())
        private set

    val appVersion: String = container.updateChecker.currentVersion()

    val developer: String = "cszy-app"

    fun refresh() {
        mcInstalled = container.systemHelper.isMcInstalled()
        installedVersion = container.systemHelper.installedMcVersion()
    }
}
