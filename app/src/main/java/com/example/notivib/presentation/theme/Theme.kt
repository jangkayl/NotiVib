package com.example.notivib.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LogoNeonGreen,
    secondary = LogoNeonGreen,
    background = LogoDarkBackground,
    surface = LogoSurfaceDark,
    surfaceVariant = LogoSurfaceVariant,
    onPrimary = LogoDarkBackground,
    onSecondary = LogoDarkBackground,
    onBackground = LogoTextPrimary,
    onSurface = LogoTextPrimary,
    onSurfaceVariant = LogoTextSecondary,
    error = LogoDanger,
    errorContainer = LogoDanger.copy(alpha = 0.2f),
    onErrorContainer = LogoDanger
)

@Composable
fun NotiVibTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
