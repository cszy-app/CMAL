package com.cszyapp.cmal.ui.resources

import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McResource
import com.cszyapp.cmal.data.db.McSkin
import com.cszyapp.cmal.data.db.McWorld
import com.cszyapp.cmal.ui.navigation.SimpleFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** 资源页：资源包 / 皮肤 / 世界 */
@Composable
fun ResourcesScreen() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: ResourcesViewModel = viewModel(factory = SimpleFactory { ResourcesViewModel(container) })

    // 打开文件选择器（导入资源）
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            CoroutineScope(Dispatchers.Main).launch {
                com.cszyapp.cmal.ui.imports.ImportHandler(context, container).handle(it)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.import_pack),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    importLauncher.launch(arrayOf(
                        "application/mcpack",
                        "application/mcaddon",
                        "application/mcworld",
                        "application/octet-stream",
                        "*/*"
                    ))
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.import_pack))
                }
            }
        }

        // 资源包
        item {
            SectionTitle(stringResource(R.string.resource_packs), vm.resources.size)
        }
        if (vm.resources.isEmpty()) {
            item { EmptyHint(stringResource(R.string.empty)) }
        }
        items(vm.resources, key = { it.id }) { r ->
            ResourceItem(
                resource = r,
                onDelete = { vm.deleteResource(r) },
                onOpen = {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(r.filePath)
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/octet-stream")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 皮肤
        item {
            SectionTitle(stringResource(R.string.skins), vm.skins.size)
        }
        if (vm.skins.isEmpty()) {
            item { EmptyHint(stringResource(R.string.empty)) }
        }
        items(vm.skins, key = { it.id }) { s ->
            SkinItem(skin = s, onDelete = { vm.deleteSkin(s) })
        }

        // 世界
        item {
            SectionTitle(stringResource(R.string.worlds), vm.worlds.size)
        }
        if (vm.worlds.isEmpty()) {
            item { EmptyHint(stringResource(R.string.empty)) }
        }
        items(vm.worlds, key = { it.id }) { w ->
            WorldItem(world = w, onDelete = { vm.deleteWorld(w) })
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ResourceItem(resource: McResource, onDelete: () -> Unit, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (resource.type == "behavior") Icons.Filled.Public else Icons.Filled.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(resource.name, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(if (resource.type == "behavior") R.string.pack_type_behavior else R.string.pack_type_resource),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.FileOpen, contentDescription = stringResource(R.string.open_minecraft))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun SkinItem(skin: McSkin, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = File(skin.localPath),
                contentDescription = skin.name,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(skin.name, fontWeight = FontWeight.Medium)
                Text("${skin.width}x${skin.height}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun WorldItem(world: McWorld, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(world.name, fontWeight = FontWeight.Medium)
                Text(
                    ctx.getString(R.string.world_size_kb, world.worldSize / 1024),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}
