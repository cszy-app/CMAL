package com.cszyapp.cmal.ui.download

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McVersion
import com.cszyapp.cmal.data.download.DownloadProgress
import com.cszyapp.cmal.ui.home.formatSize
import com.cszyapp.cmal.ui.navigation.SimpleFactory

/** 下载页：版本列表 + 搜索 + 进度 */
@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: DownloadViewModel = viewModel(factory = SimpleFactory { DownloadViewModel(container) })

    // 监听"下载完成待安装"事件，自动发起安装
    val pendingInstalls by vm.pendingInstalls.collectAsState()
    LaunchedEffect(pendingInstalls) {
        val path = pendingInstalls ?: return@LaunchedEffect
        val file = java.io.File(path)
        if (file.exists()) {
            val intent = container.installManager.createInstallIntent(file)
            context.startActivity(intent)
        }
        // 重置事件，确保同路径二次下载完成时仍能触发安装
        vm.consumePendingInstall()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Spacer(Modifier.width(8.dp))
            if (vm.refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = { vm.refresh() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_version))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(vm.filteredVersions, key = { it.versionCode }) { version ->
                VersionCard(
                    version = version,
                    downloading = vm.activeDownloads.firstOrNull { it.fileName == "${version.versionName}.apk" },
                    onDownload = {
                        Toast.makeText(context, "${version.versionName} ${context.getString(R.string.download)}", Toast.LENGTH_SHORT).show()
                        vm.downloadVersion(version)
                    },
                    onInstall = {
                        if (version.downloaded) {
                            val file = java.io.File(container.downloadManager.downloadDir(), "${version.versionName}.apk")
                            if (file.exists()) {
                                val intent = container.installManager.createInstallIntent(file)
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDownloadInstall = {
                        vm.downloadAndInstall(version)
                        Toast.makeText(context, "${version.versionName} ${context.getString(R.string.download)}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun VersionCard(
    version: McVersion,
    downloading: DownloadProgress?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDownloadInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (version.downloaded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Minecraft ${version.versionName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        version.downloaded.let {
                            if (it) stringResource(R.string.installed_ready)
                            else "${stringResource(R.string.size)}: ${formatSize(version.size)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (downloading != null) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { downloading.fraction },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(downloading.fraction * 100).toInt()}%  " +
                            formatSize(downloading.doneBytes) + " / " + formatSize(downloading.totalBytes),
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (version.downloaded) {
                        TextButton(onClick = onInstall, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.mc_install))
                        }
                    } else {
                        TextButton(onClick = onDownload, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.download))
                        }
                        TextButton(onClick = onDownloadInstall, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.download_install))
                        }
                    }
                }
            }
        }
    }
}
