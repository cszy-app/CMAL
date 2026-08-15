package com.cszyapp.cmal.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 首页 ViewModel：启动 + 多版本实例管理 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    var mcInstalled by mutableStateOf(container.systemHelper.isMcInstalled())
        private set

    var installedVersion by mutableStateOf(container.systemHelper.installedMcVersion())
        private set

    var instances by mutableStateOf<List<McInstance>>(emptyList())
        private set

    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            container.instancesRepository.observeAll().collectLatest {
                instances = it
                refresh()
            }
        }
    }

    fun launchDefault() {
        val def = instances.firstOrNull { it.isDefault }
        val target = def ?: instances.firstOrNull()
        if (target != null && container.systemHelper.isMcInstalled()) {
            if (!container.systemHelper.launchPackage(target.packageName)) {
                message = "launch_failed"
            }
        } else {
            message = if (target == null) "no_instance" else "not_installed"
        }
    }

    /** 启动指定实例 */
    fun launchInstance(instance: McInstance) {
        if (!container.systemHelper.launchPackage(instance.packageName)) {
            message = "launch_failed"
        }
    }

    fun setDefault(id: Long) {
        viewModelScope.launch { container.instancesRepository.setDefault(id) }
    }

    fun deleteInstance(instance: McInstance) {
        viewModelScope.launch { container.instancesRepository.delete(instance) }
    }

    /** 添加一个已安装的 MC 版本为实例 */
    fun addInstance(packageName: String, versionName: String) {
        viewModelScope.launch {
            val existing = instances.any { it.packageName == packageName }
            if (!existing) {
                container.instancesRepository.add(
                    name = simpleName(packageName),
                    packageName = packageName,
                    versionName = versionName,
                    isDefault = instances.isEmpty()
                )
            }
        }
    }

    /** 扫描系统已安装的 MC 包 */
    fun scanInstalled(): List<Pair<String, String>> =
        container.instancesRepository.scanInstalledPackages()

    fun clearMessage() {
        message = null
    }

    fun refresh() {
        mcInstalled = container.systemHelper.isMcInstalled()
        installedVersion = container.systemHelper.installedMcVersion()
    }

    private fun simpleName(packageName: String): String = when (packageName) {
        "com.mojang.minecraftpe" -> "Minecraft Bedrock"
        "com.mojang.minecraftpe.gpad" -> "Minecraft (Play)"
        "io.mrarm.mcpelauncher" -> "Pojav (mcpe-launcher)"
        "net.kdt.pojavlaunch" -> "PojavLauncher"
        "com.pojavlauncher.pojavlauncher" -> "PojavLauncher (Stable)"
        else -> packageName.substringAfterLast('.')
    }
}