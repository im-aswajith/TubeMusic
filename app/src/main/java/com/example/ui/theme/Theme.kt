package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val canvasBg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val pillBg: Color,
    val accent: Color,
    val isDark: Boolean
)

val LightAppColors = AppColors(
    canvasBg = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F3F5),
    surfaceElevated = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF121214),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFFA1A1AA),
    border = Color(0xFFE5E7EB),
    pillBg = Color(0xFFF3F4F6),
    accent = Color(0xFF121214),
    isDark = false
)

val DarkAppColors = AppColors(
    canvasBg = Color(0xFF0F0F12),
    surface = Color(0xFF18181B),
    surfaceVariant = Color(0xFF242428),
    surfaceElevated = Color(0xFF2A2A30),
    textPrimary = Color(0xFFF4F4F6),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    border = Color(0xFF2E2E34),
    pillBg = Color(0xFF27272A),
    accent = Color(0xFFFFFFFF),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

private val MusicLightColorScheme = lightColorScheme(
    primary = Color(0xFF121214),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF2F3F5),
    onPrimaryContainer = Color(0xFF121214),
    secondary = Color(0xFF6B7280),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2F3F5),
    onSecondaryContainer = Color(0xFF121214),
    tertiary = Color(0xFF121214),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF121214),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121214),
    surfaceVariant = Color(0xFFF2F3F5),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFF3F4F6)
)

private val MusicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF121214),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF121214),
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFF4F4F6),
    surface = Color(0xFF18181B),
    onSurface = Color(0xFFF4F4F6),
    surfaceVariant = Color(0xFF242428),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF2E2E34),
    outlineVariant = Color(0xFF1E1E22)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MusicDarkColorScheme else MusicLightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}



