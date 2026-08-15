package com.cszyapp.cmal.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.download.DownloadProgress
import com.cszyapp.cmal.ui.navigation.SimpleFactory

/** 首页：启动卡片 + 活跃下载 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: HomeViewModel = viewModel(factory = SimpleFactory { HomeViewModel(container) })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 启动卡片
        item {
            PlayCard(
                mcInstalled = vm.mcInstalled,
                installedVersion = vm.installedVersion,
                refreshing = vm.refreshing,
                hasDownloaded = vm.latestDownloaded() != null,
                onPlay = {
                    if (vm.mcInstalled) {
                        vm.launchMc()
                    } else {
                        Toast.makeText(context, R.string.please_install_first, Toast.LENGTH_SHORT).show()
                    }
                },
                onRefresh = { vm.refresh() },
                onInstall = {
                    if (!vm.mcInstalled) {
                        Toast.makeText(context, context.getString(R.string.install_from_download), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (vm.activeDownloads.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.download),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(vm.activeDownloads, key = { it.taskId }) { p ->
                DownloadProgressCard(p)
            }
        }
    }
}

@Composable
private fun PlayCard(
    mcInstalled: Boolean,
    installedVersion: String?,
    refreshing: Boolean,
    hasDownloaded: Boolean,
    onPlay: () -> Unit,
    onRefresh: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.app_full_name),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = if (mcInstalled) {
                    installedVersion?.let { "Minecraft $it" } ?: stringResource(R.string.installed)
                } else {
                    stringResource(R.string.not_installed)
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(20.dp))

            androidx.compose.material3.Button(
                onClick = onPlay,
                enabled = mcInstalled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.play))
            }

            if (!mcInstalled) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mc_install))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.refresh_version))
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(p: DownloadProgress) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(p.fileName, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${(p.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { p.fraction },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatSize(p.doneBytes) + " / " + formatSize(p.totalBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 字节数格式化 */
fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
