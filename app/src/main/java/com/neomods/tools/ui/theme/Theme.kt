package com.neomods.tools.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Neo Tools theme.
 *
 * - Honours the system dark/light setting.
 * - Uses Android 12+ dynamic colors (Material You) when available and the
 *   system is in light mode; on Android < 12 it falls back to the brand
 *   palette defined in [Color].
 * - Shares one consistent spacing/typography scale across the app.
 */
private val DarkColorScheme = darkColorScheme(
    primary = NeoPrimary,
    onPrimary = NeoOnPrimary,
    primaryContainer = NeoPrimaryContainer,
    onPrimaryContainer = NeoOnPrimaryContainer,
    surface = NeoSurfaceDark,
    onSurface = NeoOnSurfaceDark,
    onSurfaceVariant = NeoOnSurfaceVariantDark,
    error = NeoErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = NeoPrimary,
    onPrimary = NeoOnPrimary,
    primaryContainer = NeoPrimaryContainer,
    onPrimaryContainer = NeoOnPrimaryContainer,
    surface = NeoSurfaceLight,
    onSurface = NeoOnSurfaceLight,
    onSurfaceVariant = NeoOnSurfaceVariantLight,
    error = NeoError
)

@Composable
fun NeoToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeoTypography,
        content = content
    )
}
