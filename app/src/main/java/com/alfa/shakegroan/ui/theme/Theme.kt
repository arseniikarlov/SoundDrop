package com.alfa.shakegroan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColors = darkColorScheme(
    primary = GlassCyan,
    onPrimary = DeepNight,
    primaryContainer = NightBlue,
    onPrimaryContainer = FrostWhite,
    secondary = GlassPink,
    onSecondary = DeepNight,
    secondaryContainer = NightBlue,
    onSecondaryContainer = FrostWhite,
    background = DeepNight,
    onBackground = FrostWhite,
    surface = NightBlue,
    onSurface = FrostWhite,
    surfaceVariant = NightBlue,
    onSurfaceVariant = SoftText,
)

@Composable
fun ShakeGroanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = ShakeGroanTypography,
        content = content
    )
}
