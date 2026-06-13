package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AureliaDarkColorScheme = darkColorScheme(
    primary = BrightBrass,
    onPrimary = DeepNavyBg,
    secondary = StatusGreen,
    onSecondary = DeepNavyBg,
    tertiary = DiagnosticCyan,
    background = DeepNavyBg,
    onBackground = TextWhite,
    surface = DarkGraphite,
    onSurface = TextWhite,
    surfaceVariant = SlateGrayCard,
    onSurfaceVariant = TextDim,
    outline = BorderMuted,
    error = StatusRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force default elite dark mission-control visual
    dynamicColor: Boolean = false, // Use our handcrafted tactical style
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AureliaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
