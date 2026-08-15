package com.cszyapp.cmal

import android.app.Application
import android.content.Context
import com.cszyapp.cmal.data.AppContainer

/**
 * CMAL 应用入口
 * 负责初始化依赖容器（手动 DI，避免引入 Hilt/Koin，保持轻量）
 */
class CMalApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    companion object {
        /** 获取全局依赖容器 */
        fun of(context: Context): AppContainer =
            (context.applicationContext as CMalApp).container
    }
}
