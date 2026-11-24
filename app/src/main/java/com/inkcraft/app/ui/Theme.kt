package com.inkcraft.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * macOS / Apple Studio-inspired light color system:
 * Crisp neutral surfaces, 1dp hairline borders, and System Blue accents.
 */
private val MacStudioColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF004999),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEEF8),
    onSecondaryContainer = Color(0xFF2D2B8F),
    tertiary = Color(0xFFFF9500),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1D1D1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1F),
    surfaceVariant = Color(0xFFEEEEF0),
    onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFD1D1D6),
    error = Color(0xFFFF3B30)
)

@Composable
fun InkCraftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MacStudioColors,
        content = content
    )
}
