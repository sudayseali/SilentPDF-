package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PremiumPrimary,
    onPrimary = PremiumOnPrimary,
    primaryContainer = PremiumPrimaryContainer,
    onPrimaryContainer = PremiumOnPrimaryContainer,
    background = PremiumBackground,
    onBackground = PremiumTextPrimary,
    surface = PremiumSurface,
    onSurface = PremiumTextPrimary,
    surfaceVariant = PremiumSurfaceVariant,
    onSurfaceVariant = PremiumTextSecondary,
    outline = PremiumBorder
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumPrimary,
    onPrimary = PremiumOnPrimary,
    primaryContainer = PremiumPrimaryContainer,
    onPrimaryContainer = PremiumOnPrimaryContainer,
    background = PremiumBackground,
    onBackground = PremiumTextPrimary,
    surface = PremiumSurface,
    onSurface = PremiumTextPrimary,
    surfaceVariant = PremiumSurfaceVariant,
    onSurfaceVariant = PremiumTextSecondary,
    outline = PremiumBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Enforce the premium dark signature style consistently
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We always use the dark scheme to preserve the requested premium aesthetic,
    // protecting user eyes with the deep-black layout.
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
