package com.cszyapp.cmal.data

import android.content.Context
import com.cszyapp.cmal.CMalApp
import com.cszyapp.cmal.data.db.AppDatabase
import com.cszyapp.cmal.data.download.DownloadManager
import com.cszyapp.cmal.data.install.InstallManager
import com.cszyapp.cmal.data.market.MarketRepository
import com.cszyapp.cmal.data.market.ModrinthClient
import com.cszyapp.cmal.data.repo.InstancesRepository
import com.cszyapp.cmal.data.repo.ResourcesRepository
import com.cszyapp.cmal.data.repo.ServersRepository
import com.cszyapp.cmal.data.repo.SettingsRepository
import com.cszyapp.cmal.data.repo.SkinsRepository
import com.cszyapp.cmal.data.repo.WorldsRepository
import com.cszyapp.cmal.data.update.UpdateChecker
import com.cszyapp.cmal.data.xbox.XboxAuthManager
import com.cszyapp.cmal.util.Preferences
import com.cszyapp.cmal.util.SystemHelper

/**
 * 手动依赖注入容器
 * 统一创建并持有各模块单例，供 ViewModel 使用
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val preferences: Preferences = Preferences(appContext)

    val database: AppDatabase = AppDatabase.get(appContext)

    val systemHelper: SystemHelper = SystemHelper(appContext)

    val resourcesRepository: ResourcesRepository = ResourcesRepository(appContext)

    val serversRepository: ServersRepository = ServersRepository(database)

    val skinsRepository: SkinsRepository = SkinsRepository(database)

    val worldsRepository: WorldsRepository = WorldsRepository(database)

    val settingsRepository: SettingsRepository = SettingsRepository(preferences)

    val installManager: InstallManager = InstallManager(appContext)

    val updateChecker: UpdateChecker = UpdateChecker(appContext)

    val xboxAuthManager: XboxAuthManager = XboxAuthManager(preferences)

    val modrinthClient: ModrinthClient = ModrinthClient()

    val marketRepository: MarketRepository = MarketRepository(modrinthClient)

    val downloadManager: DownloadManager = DownloadManager(appContext)

    val instancesRepository: InstancesRepository = InstancesRepository(appContext)

    companion object {
        /** 便捷获取全局容器 */
        fun of(context: Context): AppContainer =
            (context.applicationContext as CMalApp).container
    }
}
