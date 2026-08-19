package com.cszyapp.cmal.ui.market

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.download.DownloadManager
import com.cszyapp.cmal.data.market.MarketItem
import com.cszyapp.cmal.ui.navigation.SimpleFactory
import kotlinx.coroutines.launch

/** 资源市场页：搜索并下载第三方资源 */
@Composable
fun MarketScreen() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: MarketViewModel = viewModel(factory = SimpleFactory { MarketViewModel(container) })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun importDownloaded(item: MarketItem) {
        val file = vm.completedFiles().firstOrNull { it.first.id == item.id }?.second ?: return
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        scope.launch {
            com.cszyapp.cmal.ui.imports.ImportHandler(context, container).handle(uri)
            file.delete()
            vm.removeTask(item.id)
        }
    }

    vm.toastMessage?.let { msg ->
        LaunchedEffect(msg) {
            val text = when (msg) {
                "no_download_url" -> context.getString(R.string.market_no_download_url)
                else -> msg
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            vm.clearToastMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 搜索框
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.updateQuery(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.market_search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(0) })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { vm.search(0) }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.market_search_hint))
            }
        }

        // 类型筛选
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf("all", "mod", "resourcepack", "shader", "datapack", "world")
            types.forEach { type ->
                val label = if (type == "all") stringResource(R.string.market_type_all) else TypeName(type)
                FilterChip(
                    selected = vm.selectedType == type,
                    onClick = { vm.setType(type) },
                    label = { Text(label) },
                    shape = FilterChipDefaults.shape
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            vm.loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            vm.error != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.market_load_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.search(0) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            vm.items.isEmpty() -> {
                Text(
                    stringResource(R.string.empty),
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(vm.items, key = { it.id }) { item ->
                        MarketItemCard(
                            item = item,
                            task = vm.tasks[item.id],
                            onDownload = { vm.downloadAndInstall(item) },
                            onCancel = { vm.cancelDownload(item.id) },
                            onImport = { importDownloaded(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketItemCard(
    item: MarketItem,
    task: com.cszyapp.cmal.data.download.DownloadState?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onImport: () -> Unit
) {
    val running = task?.running == true
    val done = task?.done == true
    val failed = task?.error != null && !running && !done

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = item.iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${item.author} · ${TypeName(item.type)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        SourceBadge(source = item.source)
                    }
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.version?.let { v ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.market_required_version, v),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (running && task != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { task.progress },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(task.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.cancel))
                    }
                }
            } else if (done) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.market_downloaded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onImport, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text(stringResource(R.string.import_pack))
                    }
                }
            } else if (failed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.market_download_failed, task?.error ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onDownload) { Text(stringResource(R.string.retry)) }
            } else {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DownloadManager.sizeString(item.fileSize).ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Button(onClick = onDownload, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.download))
                    }
                }
            }
        }
    }
}

/** 来源徽标：mcfun → McFun；mcpedl → mcpedl */
@Composable
private fun SourceBadge(source: String) {
    val label = when (source) {
        "mcfun" -> "McFun"
        "mcpedl" -> "mcpedl"
        else -> source
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun TypeName(type: String): String = when (type) {
    "mod" -> stringResource(R.string.market_type_mod)
    "resourcepack" -> stringResource(R.string.market_type_resourcepack)
    "shader" -> stringResource(R.string.market_type_shader)
    "datapack" -> stringResource(R.string.market_type_datapack)
    "world" -> stringResource(R.string.market_type_world)
    "modpack" -> stringResource(R.string.market_type_modpack)
    else -> type
}