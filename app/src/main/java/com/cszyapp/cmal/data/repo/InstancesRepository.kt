package com.cszyapp.cmal.data.repo

import android.content.Context
import android.content.pm.PackageManager
import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.db.McInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 版本实例仓库：管理多个 Minecraft 实例
 * 每个实例绑定一个已安装的 MC 包
 */
class InstancesRepository(private val context: Context) {

    private val database = AppDatabase.get(context)

    /** 常见 Minecraft 系列包名（用于扫描已安装版本） */
    companion object {
        val KNOWN_MC_PACKAGES = listOf(
            "com.mojang.minecraftpe",           // 官方基岩版
            "com.mojang.minecraftpe.gpad",      // Google Play
            "io.mrarm.mcpelauncher",            // PojavLauncher
            "net.kdt.pojavlaunch",              // Pojav
            "com.pojavlauncher.pojavlauncher",  // PojavLauncher stable
            "io.github.retroboard.mcpe",        // 测试版渠道
            "com.blocklauncher.beta",           // BlockLauncher
            "com.mojang.minecraftdemo"          // 试玩版
        )
    }

    fun observeAll(): Flow<List<McInstance>> = database.instanceDao().observeAll()

    suspend fun add(
        name: String,
        packageName: String,
        versionName: String = "",
        isDefault: Boolean = false
    ): McInstance = withContext(Dispatchers.IO) {
        if (isDefault) database.instanceDao().clearDefault()
        val inst = McInstance(
            name = name,
            packageName = packageName,
            versionName = versionName,
            isDefault = isDefault
        )
        val id = database.instanceDao().upsert(inst)
        inst.copy(id = id)
    }

    suspend fun setDefault(id: Long) = withContext(Dispatchers.IO) {
        database.instanceDao().clearDefault()
        database.instanceDao().getById(id)?.let {
            database.instanceDao().upsert(it.copy(isDefault = true))
        }
    }

    suspend fun delete(instance: McInstance) = withContext(Dispatchers.IO) {
        database.instanceDao().delete(instance)
    }

    /** 扫描系统里已安装的 MC 相关包 */
    fun scanInstalledPackages(): List<Pair<String, String>> {
        val pm = context.packageManager
        return KNOWN_MC_PACKAGES.mapNotNull { pkg ->
            try {
                val info = pm.getPackageInfo(pkg, 0)
                pkg to (info.versionName ?: "")
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}