package com.flash.usdt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppDarkColorScheme =
  darkColorScheme(
    primary = UsdtTeal,
    secondary = OnlineGreen,
    onPrimary = TextPrimary,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle,
    outlineVariant = TextFooter,
    error = ErrorCoral,
  )

@Composable
fun FlashUsdtTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = AppDarkColorScheme, typography = Typography, content = content)
}
