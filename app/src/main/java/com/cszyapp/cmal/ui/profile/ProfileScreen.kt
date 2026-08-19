package com.cszyapp.cmal.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.download.DownloadManager
import com.cszyapp.cmal.ui.navigation.SimpleFactory
import com.cszyapp.cmal.ui.settings.SettingsViewModel

/** 我的页：个人信息 + 设置入口 + 免责声明 */
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: ProfileViewModel = viewModel(factory = SimpleFactory { ProfileViewModel(container) })

    var showAbout by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 个人信息头部
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            vm.developer,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${stringResource(R.string.app_full_name)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${stringResource(R.string.version)} ${vm.appVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Xbox 账户
        item {
            XboxSection()
        }

        // 设置项
        item {
            SettingsCard()
        }

        // 关于/免责声明
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                SettingRow(
                    icon = Icons.Filled.Android,
                    title = stringResource(R.string.about),
                    onClick = { showAbout = true }
                )
                HorizontalDivider()
                SettingRow(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.disclaimer_title),
                    onClick = { showDisclaimer = true }
                )
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.about)) },
            text = {
                Column {
                    Text(stringResource(R.string.app_full_name))
                    Spacer(Modifier.height(8.dp))
                    Text("${stringResource(R.string.developer)}: ${vm.developer}")
                    Spacer(Modifier.height(4.dp))
                    Text("${stringResource(R.string.version)}: ${vm.appVersion}")
                    Spacer(Modifier.height(4.dp))
                    Text("Package: com.cszyapp.cmal")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = { Text(stringResource(R.string.disclaimer_text)) },
            confirmButton = {
                TextButton(onClick = {
                    container.settingsRepository.disclaimerAccepted = true
                    showDisclaimer = false
                }) {
                    Text(stringResource(R.string.agree))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclaimer = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsCard() {
    val context = LocalContext.current
    val container = AppContainer.of(context)
    val vm: SettingsViewModel = viewModel(factory = SimpleFactory { SettingsViewModel(container) })

    var showThemePicker by remember { mutableStateOf(false) }
    var showAccent by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(false) }

    LaunchedEffect(vm.updateInfo) {
        vm.updateInfo?.let { info ->
            val text = when {
                info.startsWith("update:") -> context.getString(R.string.update_available, info.removePrefix("update:"))
                info == "up_to_date" -> context.getString(R.string.up_to_date)
                else -> context.getString(R.string.network_error)
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            vm.clearUpdateInfo()
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column {
            SettingRow(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.settings),
                trailing = {
                    Text(
                        stringResource(
                            when (vm.themeMode) {
                                "dark" -> R.string.theme_dark
                                "light" -> R.string.theme_light
                                else -> R.string.theme_system
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { showThemePicker = true }
            )
            HorizontalDivider()
            SettingRow(
                icon = Icons.Filled.ColorLens,
                title = stringResource(R.string.choose_theme_color),
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(vm.accentColor), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                },
                onClick = { showAccent = true }
            )
            HorizontalDivider()
            SettingRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.language),
                trailing = {
                    Text(
                        stringResource(
                            when (vm.language) {
                                "zh" -> R.string.language_zh
                                "en" -> R.string.language_en
                                else -> R.string.language_auto
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { showLanguage = true }
            )
            HorizontalDivider()
            SettingRow(
                icon = Icons.Filled.Build,
                title = stringResource(R.string.backup_restore),
                onClick = { showBackup = true }
            )
            HorizontalDivider()
            SettingRow(
                icon = Icons.Filled.Download,
                title = stringResource(R.string.download_manager),
                trailing = {
                    Text(
                        DownloadManager.sizeString(vm.downloadDirSize).ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = { showDownloads = true }
            )
            HorizontalDivider()
            SettingRow(
                icon = Icons.Filled.Update,
                title = stringResource(R.string.check_update),
                onClick = { vm.checkUpdate() }
            )
        }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            current = vm.themeMode,
            onSelect = { mode ->
                vm.updateThemeMode(mode)
                showThemePicker = false
                // 重新创建 Activity 使主题即时生效（MainActivity 从 SharedPreferences 读取）
                val activity = context as? android.app.Activity
                activity?.recreate()
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showAccent) {
        AccentColorDialog(
            current = vm.accentColor,
            onSelect = { color ->
                vm.updateAccentColor(color)
                showAccent = false
            },
            onDismiss = { showAccent = false }
        )
    }

    if (showLanguage) {
        LanguageDialog(
            current = vm.language,
            onSelect = { lang ->
                vm.updateLanguage(lang)
                showLanguage = false
                // 语言切换后重启 Activity 使资源生效
                val activity = context as? android.app.Activity
                activity?.recreate()
            },
            onDismiss = { showLanguage = false }
        )
    }

    if (showBackup) {
        BackupDialog(container, onDismiss = { showBackup = false })
    }

    if (showDownloads) {
        DownloadDialog(
            sizeBytes = vm.downloadDirSize,
            onClear = { vm.clearDownloads() },
            onDismiss = { showDownloads = false }
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailing?.invoke()
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
