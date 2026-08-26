package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ChampagnePrimary,
    onPrimary = WarmBlack,
    primaryContainer = GraphiteSurfaceVariant,
    onPrimaryContainer = ChampagnePrimary,
    secondary = BronzeSecondary,
    onSecondary = SoftWhite,
    background = DeepCharcoal,
    onBackground = SoftWhite,
    surface = GraphiteSurface,
    onSurface = SoftWhite,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = MutedTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = CharcoalPrimary,
    onPrimary = IvoryBackground,
    primaryContainer = SoftGraySurfaceVariant,
    onPrimaryContainer = CharcoalPrimary,
    secondary = BronzeSecondaryLight,
    onSecondary = IvoryBackground,
    background = IvoryBackground,
    onBackground = CharcoalText,
    surface = SoftGraySurface,
    onSurface = CharcoalText,
    surfaceVariant = SoftGraySurfaceVariant,
    onSurfaceVariant = MutedTextLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
