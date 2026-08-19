package com.cszyapp.cmal.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cszyapp.cmal.R
import com.cszyapp.cmal.data.AppContainer
import com.cszyapp.cmal.data.download.DownloadManager

/** 可选择的主题色（ARGB Long，与 Preferences.accentColor 一致） */
val ACCENT_COLORS: List<Long> = listOf(
    0xFFF5A623L, // 琥珀（默认）
    0xFFE64A19L, // 橙
    0xFFE53935L, // 红
    0xFF43A047L, // 绿
    0xFF1E88E5L, // 蓝
    0xFF8E24AAL, // 紫
    0xFFD81B60L, // 粉
    0xFF00ACC1L  // 青
)

/** 主题色选择对话框 */
@Composable
fun AccentColorDialog(
    current: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_theme_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ACCENT_COLORS.take(4).forEach { color ->
                        ColorSwatch(color = color, selected = current == color, onClick = { onSelect(color) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ACCENT_COLORS.drop(4).forEach { color ->
                        ColorSwatch(color = color, selected = current == color, onClick = { onSelect(color) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}

@Composable
private fun ColorSwatch(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(color), CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

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
            val base = java.io.File(dir, "cmal_backup_$stamp")
            base.mkdirs()
            copyFile(dbFile, java.io.File(base, "cmal.db"))
            // Room 默认 WAL 模式：活动数据在 -wal/-shm，必须一并备份，否则恢复后数据缺失/损坏
            listOf("-wal", "-shm").forEach { suffix ->
                val f = java.io.File(dbFile.absolutePath + suffix)
                if (f.exists()) copyFile(f, java.io.File(base, "cmal.db$suffix"))
            }
            backupPath = base.absolutePath
            Toast.makeText(context, R.string.backup_done, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.backup_fail, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFile(src: java.io.File, dst: java.io.File) {
        dst.parentFile.mkdirs()
        src.copyTo(dst, overwrite = true)
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
