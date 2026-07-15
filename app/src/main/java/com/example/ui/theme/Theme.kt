package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PremiumPrimary,
    onPrimary = PremiumOnPrimary,
    primaryContainer = PremiumPrimaryContainer,
    onPrimaryContainer = PremiumOnPrimaryContainer,
    background = Color.Black, // OLED True Dark background
    onBackground = PremiumTextPrimary,
    surface = Color(0xFF0A0C16), // Extremely dark charcoal/slate surface
    onSurface = PremiumTextPrimary,
    surfaceVariant = Color(0xFF141724), // Elevated dark surface
    onSurfaceVariant = PremiumTextSecondary,
    outline = PremiumBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumLightPrimary,
    onPrimary = PremiumLightOnPrimary,
    primaryContainer = PremiumLightPrimaryContainer,
    onPrimaryContainer = PremiumLightOnPrimaryContainer,
    background = PremiumLightBackground,
    onBackground = PremiumLightTextPrimary,
    surface = PremiumLightSurface,
    onSurface = PremiumLightTextPrimary,
    surfaceVariant = PremiumLightSurfaceVariant,
    onSurfaceVariant = PremiumLightTextSecondary,
    outline = PremiumLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set default to false (Light Theme as requested by the user's reference)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
