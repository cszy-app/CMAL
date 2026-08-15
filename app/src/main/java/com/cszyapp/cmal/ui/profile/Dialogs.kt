package com.cszyapp.cmal.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.download.DownloadManager

/** 主题模式选择对话框 */
@Composable
fun ThemePickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("system", stringResource(R.string.theme_system), 0xFFF5A623),
        Triple("light", stringResource(R.string.theme_light), 0xFFF5A623),
        Triple("dark", stringResource(R.string.theme_dark), 0xFFFFB74D)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column {
                options.forEach { (value, label, _) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == value,
                            onClick = { onSelect(value) }
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}

/** 语言选择对话框 */
@Composable
fun LanguageDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Triple("auto", stringResource(R.string.language_auto), false),
        Triple("zh", stringResource(R.string.language_zh), true),
        Triple("en", stringResource(R.string.language_en), false)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column {
                options.forEach { (value, label, _) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == value,
                            onClick = { onSelect(value) }
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}

/** 备份/恢复对话框 */
@Composable
fun BackupDialog(container: AppContainer, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var backupPath by remember { mutableStateOf("") }

    fun doBackup() {
        try {
            val dbFile = java.io.File(context.getDatabasePath("cmal.db").absolutePath)
            val dir = java.io.File(context.getExternalFilesDir(null), "backups")
            dir.mkdirs()
            val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(java.util.Date())
            val target = java.io.File(dir, "cmal_backup_$stamp.db")
            dbFile.copyTo(target, overwrite = true)
            backupPath = target.absolutePath
            Toast.makeText(context, R.string.backup_done, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.backup_fail, Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore)) },
        text = {
            Column {
                Text(stringResource(R.string.backup_path))
                Spacer(Modifier.height(8.dp))
                if (backupPath.isNotBlank()) {
                    Text(
                        backupPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { doBackup() }) {
                Text(stringResource(R.string.backup_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun DownloadDialog(
    sizeBytes: Long,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_manager)) },
        text = {
            Column {
                Text(stringResource(R.string.download_dir_size))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (sizeBytes > 0) "${DownloadManager.sizeString(sizeBytes)}" else stringResource(R.string.empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.download_clear_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClear()
                onDismiss()
            }) {
                Text(stringResource(R.string.download_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
