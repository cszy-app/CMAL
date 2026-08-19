package com.cszyapp.cmal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/**
 * CMAL 主题
 * 以用户自定义主题色（默认琥珀）派生明暗两套配色。
 * Android 12+ 的动态取色仅在 dynamicColor=true 时启用（默认关闭，保证主题色设置生效）。
 */
val CitrineLight = Color(0xFFF5A623)
val CitrineDark = Color(0xFFFFB74D)

private fun lightColors(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = lerp(accent, Color.White, 0.82f),
    onPrimaryContainer = lerp(accent, Color.Black, 0.72f),
    secondary = lerp(accent, Color.Black, 0.15f),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5)
)

private fun darkColors(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF3E2500),
    primaryContainer = lerp(accent, Color.Black, 0.72f),
    onPrimaryContainer = lerp(accent, Color.White, 0.9f),
    secondary = lerp(accent, Color.White, 0.2f),
    background = Color(0xFF14110C),
    surface = Color(0xFF14110C)
)

@Composable
fun CMALTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Long = CitrineLight.value.toLong(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = Color(accentColor)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColors(accent)
        else -> lightColors(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}