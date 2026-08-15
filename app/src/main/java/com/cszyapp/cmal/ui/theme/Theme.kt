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
import androidx.compose.ui.platform.LocalContext

/**
 * CMAL 主题
 * 支持 Material 3 动态取色（Android 12+）+ 自定义主题色（琥珀默认）
 */
val CitrineLight = Color(0xFFF5A623)
val CitrineDark = Color(0xFFFFB74D)

private val LightColors = lightColorScheme(
    primary = CitrineLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF4A2A00),
    secondary = Color(0xFFB26A00),
    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFBF5)
)

private val DarkColors = darkColorScheme(
    primary = CitrineDark,
    onPrimary = Color(0xFF3E2500),
    primaryContainer = Color(0xFF5C3A00),
    onPrimaryContainer = Color(0xFFFFDEAC),
    secondary = Color(0xFFFFB74D),
    background = Color(0xFF14110C),
    surface = Color(0xFF14110C)
)

@Composable
fun CMALTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Long = CitrineLight.value.toLong(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
