package com.cszyapp.cmal.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.xbox.DeviceCodeInfo
import com.cszyapp.cmal.data.xbox.XboxAccount
import com.cszyapp.cmal.data.xbox.XboxAuthException
import kotlinx.coroutines.launch

/** Xbox 登录 ViewModel（支持多账号切换） */
class XboxViewModel(private val container: AppContainer) : ViewModel() {

    var account: XboxAccount? by mutableStateOf(container.xboxAuthManager.load())
        private set

    var accounts by mutableStateOf(container.xboxAuthManager.allAccounts())
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
                accounts = container.xboxAuthManager.allAccounts()
                loggedIn = true
            } catch (e: XboxAuthException) {
                error = "Xbox ${e.stage} ${e.errorCode} · ${XboxAuthException.describe(e.errorCode, e.message ?: "登录失败")}"
            } catch (e: Exception) {
                error = e.message
            } finally {
                loggingIn = false
            }
        }
    }

    fun switchAccount(xuid: String) {
        val acc = container.xboxAuthManager.switchTo(xuid)
        if (acc != null) {
            account = acc
            accounts = container.xboxAuthManager.allAccounts()
            loggedIn = acc.expiresAt > System.currentTimeMillis()
        }
    }

    fun removeAccount(xuid: String) {
        container.xboxAuthManager.removeAccount(xuid)
        accounts = container.xboxAuthManager.allAccounts()
        account = container.xboxAuthManager.load()
        loggedIn = container.xboxAuthManager.isLoggedIn()
    }

    fun cancel() {
        loggingIn = false
        deviceCode = null
    }

    fun logout() {
        container.xboxAuthManager.clear()
        account = null
        accounts = emptyList()
        loggedIn = false
    }

    fun clearError() {
        error = null
    }
}