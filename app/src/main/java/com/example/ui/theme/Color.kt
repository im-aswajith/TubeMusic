package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Static true constants for places requiring fixed absolute colors
val TrueBlack = Color(0xFF000000)
val TrueWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)
val DarkCharcoal = Color(0xFF1C1C1E)

// Dynamic theme-aware colors that adapt automatically when Dark Theme is selected:
val CanvasBg: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.canvasBg

val PureWhite: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surface

val ObsidianBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textPrimary

val SoftSurfaceGray: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surfaceVariant

val ActivePillBg: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.pillBg

val MediumGray: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textSecondary

val LightBorderGray: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.border

val CoolGray: Color
    @Composable
    @ReadOnlyComposable
    get() = if (AppTheme.colors.isDark) Color(0xFFA1A1AA) else Color(0xFF6B7280)

// Accent Colors
val AccentRed = Color(0xFFFF2D55)
val MusicPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = ObsidianBlack
val MusicPrimaryDark = PureBlack
val MusicSecondary = Color(0xFF27272A)
val MusicAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = ObsidianBlack
val MusicEmerald = Color(0xFF10B981)

val DarkBackground = Color(0xFF121214)
val DarkSurface = Color(0xFF18181B)
val DarkSurfaceVariant = Color(0xFF27272A)
val DarkSurfaceElevated = Color(0xFF3F3F46)

val LightTextPrimary = Color(0xFF121214)
val LightTextSecondary = Color(0xFF71717A)
val LightTextTertiary = Color(0xFFA1A1AA)

val LiveRed = Color(0xFFFF2D55)
val LiveBadgeBg = Color(0xFFE11D48)
val TubeRed = Color(0xFFFF2D55)

object MusicGradients {
    val HeroButton = Brush.linearGradient(
        colors = listOf(Color(0xFF121214), Color(0xFF27272A))
    )
    val PlayerCardGlow = Brush.radialGradient(
        colors = listOf(Color(0x1A000000), Color(0x00000000))
    )
    val MiniPlayerBg = Brush.horizontalGradient(
        colors = listOf(Color(0xFF18181B), Color(0xFF121214))
    )
    val GenrePop = Brush.linearGradient(listOf(Color(0xFF1E1E24), Color(0xFF2B2B36)))
    val GenreHipHop = Brush.linearGradient(listOf(Color(0xFF27272A), Color(0xFF3F3F46)))
    val GenreLofi = Brush.linearGradient(listOf(Color(0xFF3F3F46), Color(0xFF52525B)))
    val GenreRock = Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF27272A)))
    val GenreElectronic = Brush.linearGradient(listOf(Color(0xFF27272A), Color(0xFF1E1E24)))
    val GenreChill = Brush.linearGradient(listOf(Color(0xFF3F3F46), Color(0xFF27272A)))
}


