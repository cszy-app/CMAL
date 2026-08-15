package com.cszyapp.cmal.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cszyapp.cmal.R

private data class OnboardItem(
    val title: String,
    val desc: String,
    val icon: ImageVector
)

/** 首次使用引导页 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    val items = listOf(
        OnboardItem(
            stringResource(R.string.onboard_welcome_title),
            stringResource(R.string.onboard_welcome_desc),
            Icons.Filled.CloudDownload
        ),
        OnboardItem(
            stringResource(R.string.onboard_feature1_title),
            stringResource(R.string.onboard_feature1_desc),
            Icons.Filled.CloudDownload
        ),
        OnboardItem(
            stringResource(R.string.onboard_feature2_title),
            stringResource(R.string.onboard_feature2_desc),
            Icons.Filled.Palette
        ),
        OnboardItem(
            stringResource(R.string.onboard_feature3_title),
            stringResource(R.string.onboard_feature3_desc),
            Icons.Filled.Public
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val item = items[page]
        Icon(
            item.icon,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            item.desc,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        // 圆点指示器
        RowDotIndicator(page, items.size)

        Spacer(Modifier.height(24.dp))

        if (page < items.lastIndex) {
            Button(
                onClick = { page++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboard_next))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboard_skip))
            }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboard_start))
            }
        }
    }
}

@Composable
private fun RowDotIndicator(current: Int, total: Int) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(total) { i ->
            val active = i == current
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
                    .size(if (active) 24.dp else 8.dp, 8.dp)
            )
        }
    }
}
