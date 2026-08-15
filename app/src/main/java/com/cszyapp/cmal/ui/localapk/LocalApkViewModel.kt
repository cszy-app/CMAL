package com.cszyapp.cmal.ui.localapk

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.ui.imports.ImportHandler
import kotlinx.coroutines.launch

/** 本地 APK 页 ViewModel */
class LocalApkViewModel(private val container: AppContainer) : ViewModel() {

    var mcInstalled by mutableStateOf(container.systemHelper.isMcInstalled())
        private set

    var installedVersion by mutableStateOf(container.systemHelper.installedMcVersion())
        private set

    var installing by mutableStateOf(false)
        private set

    fun installFrom(uri: Uri, context: Context) {
        if (installing) return
        installing = true
        viewModelScope.launch {
            val importHandler = ImportHandler(context, container)
            val file = importHandler.copyUriToFile(uri, "apk_${System.currentTimeMillis()}.apk")
            if (file != null) {
                val intent = container.installManager.createInstallIntent(file)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, R.string.apk_read_fail, Toast.LENGTH_SHORT).show()
            }
            installing = false
        }
    }
}