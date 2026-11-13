package com.sreekumar.countryexplorer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * ## Theming: Material 3
 *
 * This is your design-token layer — the CSS-variables equivalent. Anything
 * below in the tree reads colours via `MaterialTheme.colorScheme.primary`
 * instead of hard-coding hex values, so dark mode and dynamic colour work for
 * free.
 *
 * `MaterialTheme` is a *CompositionLocal* provider. CompositionLocal is
 * implicit, ambient data passed down the tree without threading it through
 * every function's parameters — i.e. exactly React Context.
 */

private val BrandBlue = Color(0xFF1B3A5C)
private val BrandTeal = Color(0xFF2E7D74)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    secondary = BrandTeal,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FC6EC),
    secondary = Color(0xFF7FD3C8),
)

@Composable
fun CountryExplorerTheme(
    // Default argument reads the OS setting. Android has had real system-level
    // dark mode since 10, so you rarely build your own toggle.
    darkTheme: Boolean = isSystemInDarkTheme(),
    // "Material You": on Android 12+, derive the palette from the user's
    // wallpaper. A platform affordance with no web equivalent.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
