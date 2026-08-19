package com.cszyapp.cmal.ui.servers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.db.McServer
import com.cszyapp.cmal.ui.navigation.SimpleFactory

/** 服务器页：自定义服务器列表 + 精选 */
@Composable
fun ServersScreen() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: ServersViewModel = viewModel(factory = SimpleFactory { ServersViewModel(container) })

    var showAddDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<McServer?>(null) }

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
                    stringResource(R.string.server_list),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_server))
                }
            }
        }

        // 精选服务器
        item {
            Text(
                stringResource(R.string.featured_servers),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        featuredServers().forEach { s ->
            item {
                ServerCard(
                    server = s,
                    onJoin = {
                        joinServer(context, s)
                    },
                    onEdit = null,
                    onDelete = null
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.server_list),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (vm.servers.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(vm.servers, key = { it.id }) { s ->
            ServerCard(
                server = s,
                onJoin = { joinServer(context, s) },
                onEdit = { editingServer = s },
                onDelete = { vm.delete(s) }
            )
        }
    }

    if (showAddDialog) {
        ServerEditDialog(
            title = stringResource(R.string.add_server),
            onConfirm = { name, addr, port ->
                val ok = vm.add(name, addr, port)
                showAddDialog = false
                if (!ok) Toast.makeText(context, context.getString(R.string.info_incomplete), Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingServer?.let { server ->
        ServerEditDialog(
            title = stringResource(R.string.edit),
            initial = server,
            onConfirm = { name, addr, port ->
                vm.update(server.copy(name = name, address = addr, port = port))
                editingServer = null
            },
            onDismiss = { editingServer = null }
        )
    }
}

/** 内置精选服务器 */
private fun featuredServers(): List<McServer> = listOf(
    McServer(
        name = "Hive",
        address = "play.hivebedrock.network",
        port = 19132,
        featured = true,
        description = "Popular minigame network"
    ),
    McServer(
        name = "Lifeboat",
        address = "play.lbsg.net",
        port = 19132,
        featured = true,
        description = "Minigames & classic games"
    )
)

private fun joinServer(context: android.content.Context, server: McServer) {
    // 通过 Minecraft 的服务器 URL scheme 加入（官方参数名为 serverUrl / serverPort）
    val scheme = "minecraft://connect/?serverUrl=${Uri.encode(server.address)}&serverPort=${server.port}"
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.open_mc_fail), Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun ServerCard(
    server: McServer,
    onJoin: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (server.featured) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(server.name, fontWeight = FontWeight.Bold)
                Text(
                    "${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (server.description.isNotBlank()) {
                    Text(server.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = onJoin) { Text(stringResource(R.string.play)) }
            if (onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun ServerEditDialog(
    title: String,
    initial: McServer = McServer(name = "", address = ""),
    onConfirm: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var addr by remember { mutableStateOf(initial.address) }
    var port by remember { mutableStateOf(initial.port.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.server_name)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = addr,
                    onValueChange = { addr = it },
                    label = { Text(stringResource(R.string.server_address)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val p = port.toIntOrNull() ?: 19132
                if (p !in 1..65535) {
                    Toast.makeText(context, context.getString(R.string.info_incomplete), Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                onConfirm(name.trim(), addr.trim(), p)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
