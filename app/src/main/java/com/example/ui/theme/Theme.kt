package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkSageOnPrimary,
    primaryContainer = DarkSagePrimaryContainer,
    onPrimaryContainer = DarkSageOnPrimaryContainer,
    background = DarkAlabasterBackground,
    onBackground = DarkSlateOnBackground,
    surface = DarkSandSurface,
    onSurface = DarkSlateOnSurface,
    surfaceVariant = DarkSandSurfaceVariant,
    onSurfaceVariant = DarkSlateOnSurfaceVariant,
    outline = DarkSageBorder
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    background = AlabasterBackground,
    onBackground = SlateOnBackground,
    surface = SandSurface,
    onSurface = SlateOnSurface,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = SageBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor by default to guarantee our beautiful "Natural Tones" signature palette is shown
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
