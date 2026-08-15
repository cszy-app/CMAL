package com.cszyapp.cmal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.ui.imports.ImportHandler
import com.cszyapp.cmal.ui.navigation.CMALRoot
import com.cszyapp.cmal.ui.theme.CMALTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var importHandler: ImportHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = CMalApp.of(this)
        importHandler = ImportHandler(this, container)

        setContent {
            val settingsRepo = container.settingsRepository
            val darkTheme = when (settingsRepo.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            CMALTheme(
                darkTheme = darkTheme,
                accentColor = settingsRepo.accentColor
            ) {
                CMALRoot()
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** 处理外部打开（安装包 / 资源包分享导入） */
    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                val mime = intent.type
                when {
                    mime == "application/vnd.android.package-archive" -> handleApkSelect(uri)
                    else -> {
                        lifecycleScope.launch {
                            importHandler.handle(uri)
                        }
                    }
                }
            }
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
                lifecycleScope.launch {
                    importHandler.handle(uri)
                }
            }
        }
    }

    private fun handleApkSelect(uri: Uri) {
        lifecycleScope.launch {
            container.installManager.apply {
                val file = importHandler.copyUriToFile(uri, "apk_${System.currentTimeMillis()}.apk")
                if (file != null) {
                    val installIntent = createInstallIntent(file)
                    startActivity(installIntent)
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.apk_read_fail), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
