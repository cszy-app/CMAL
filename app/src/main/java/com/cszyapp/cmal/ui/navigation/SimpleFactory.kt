package com.cszyapp.cmal.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** 极简 ViewModel 工厂（避免引入 Hilt/Koin 依赖） */
class SimpleFactory<T : ViewModel>(
    private val create: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM =
        create() as VM
}
