package com.cszyapp.cmal.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.xbox.DeviceCodeInfo
import com.cszyapp.cmal.data.xbox.XboxAccount
import kotlinx.coroutines.launch

/** Xbox 登录 ViewModel */
class XboxViewModel(private val container: AppContainer) : ViewModel() {

    var account: XboxAccount? by mutableStateOf(container.xboxAuthManager.load())
        private set

    var deviceCode: DeviceCodeInfo? by mutableStateOf(null)
        private set

    var loggingIn by mutableStateOf(false)
        private set

    var loggedIn by mutableStateOf(container.xboxAuthManager.isLoggedIn())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun startLogin() {
        if (loggingIn) return
        loggingIn = true
        error = null
        viewModelScope.launch {
            try {
                val info = container.xboxAuthManager.requestDeviceCode()
                deviceCode = info
                val (accessToken, _) = container.xboxAuthManager.pollForToken(info)
                val acc = container.xboxAuthManager.completeLogin(accessToken)
                container.xboxAuthManager.save(acc)
                account = acc
                loggedIn = true
            } catch (e: Exception) {
                error = e.message
            } finally {
                loggingIn = false
            }
        }
    }

    fun cancel() {
        loggingIn = false
        deviceCode = null
    }

    fun logout() {
        container.xboxAuthManager.clear()
        account = null
        loggedIn = false
    }

    fun clearError() {
        error = null
    }
}