package com.cszyapp.cmal.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.cszyapp.cmal.ui.navigation.SimpleFactory

/**
 * Xbox 账户卡片：登录 / 显示玩家代号 / 多账号切换 / 退出
 */
@Composable
fun XboxSection() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: XboxViewModel = viewModel(factory = SimpleFactory { XboxViewModel(container) })

    var showDeviceCode by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }

    val account = vm.account
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (account != null) Icons.Filled.Person else Icons.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (account != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.xbox_account),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    account?.gamertag ?: stringResource(R.string.xbox_not_logged_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (vm.loggingIn) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (account != null) {
                if (vm.accounts.size > 1) {
                    TextButton(onClick = { showAccounts = true }) {
                        Text(stringResource(R.string.xbox_switch))
                    }
                }
                TextButton(onClick = { vm.logout() }) {
                    Text(stringResource(R.string.xbox_logout))
                }
            } else {
                TextButton(onClick = {
                    vm.startLogin()
                    showDeviceCode = true
                }) {
                    Text(stringResource(R.string.xbox_login))
                }
            }
        }
    }

    if (showDeviceCode) {
        DeviceCodeDialog(
            vm = vm,
            onDismiss = {
                showDeviceCode = false
                vm.cancel()
            }
        )
    }

    if (showAccounts) {
        AccountSwitchDialog(
            vm = vm,
            onDismiss = { showAccounts = false }
        )
    }

    vm.error?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.clearError()
    }
}

@Composable
private fun AccountSwitchDialog(
    vm: XboxViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.xbox_switch)) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                vm.accounts.forEach { acc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                acc.gamertag,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "XUID ${acc.xuid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.OutlinedButton(onClick = {
                            vm.switchAccount(acc.xuid)
                            onDismiss()
                        }) {
                            Text(stringResource(R.string.xbox_use))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DeviceCodeDialog(
    vm: XboxViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deviceCode = vm.deviceCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.xbox_login)) },
        text = {
            Column {
                Text(stringResource(R.string.xbox_device_hint))
                Spacer(Modifier.height(12.dp))
                if (deviceCode != null) {
                    Text(
                        deviceCode.userCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(deviceCode.verificationUri))
                                )
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.xbox_open_link))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.xbox_waiting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (vm.loggedIn) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}