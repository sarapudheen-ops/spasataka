package com.spacetec.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// SpaceTec Dark Color Scheme - Enhanced for better contrast and space theme
private val DarkColorScheme = darkColorScheme(
    primary = SpaceColors.Primary,
    secondary = SpaceColors.Secondary,
    tertiary = SpaceColors.GlowPurple,
    background = SpaceColors.Background,
    surface = SpaceColors.Surface,
    onPrimary = SpaceColors.OnPrimary,
    onSecondary = SpaceColors.OnSecondary,
    onBackground = SpaceColors.OnBackground,
    onSurface = SpaceColors.OnSurface,
    onError = SpaceColors.OnError,
    error = SpaceColors.Error,
    primaryContainer = SpaceColors.PrimaryVariant,
    secondaryContainer = SpaceColors.SecondaryVariant,
    surfaceVariant = SpaceColors.SurfaceVariant,
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFF3E4B7A),
    scrim = Color(0xCC000000),
    surfaceTint = SpaceColors.Primary.copy(alpha = 0.5f)
)

@Composable
fun SpaceTecTheme(
    darkTheme: Boolean = true, // Default to dark theme for space feel
    dynamicColor: Boolean = false, // Disable dynamic color to maintain space theme
    content: @Composable () -> Unit
) {
    // Always use our custom dark theme for now
    val colorScheme = DarkColorScheme
    val SpaceDarkColorScheme = DarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpaceTypographySet,
        content = content
    )
}

/**
 * Extension function to get the current color scheme
 */
val MaterialTheme.colorPalette: SpaceColors
    @Composable
    @ReadOnlyComposable
    get() = SpaceColors
