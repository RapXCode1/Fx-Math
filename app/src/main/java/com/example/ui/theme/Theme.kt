package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    tertiary = AccentPink,
    surfaceVariant = CardGlassDark
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFFF1F3F9),
    background = Color(0xFFFAFAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF131520),
    onSurface = Color(0xFF131520),
    tertiary = Color(0xFFE91E63),
    surfaceVariant = Color(0x0F000000)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark theme as requested (Dark Mode First)
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
