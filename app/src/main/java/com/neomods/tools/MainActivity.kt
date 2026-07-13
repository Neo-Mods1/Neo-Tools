package com.neomods.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.neomods.tools.navigation.NeoNavHost
import com.neomods.tools.storage.SettingsManager
import com.neomods.tools.ui.theme.NeoToolsTheme
import com.neomods.tools.theme.ThemeMode
import androidx.compose.runtime.collectAsState

/**
 * Single, Compose-only activity. All screens are hosted by [NeoNavHost].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Let the app draw behind the system bars so the themed surface shows
        // through the status/navigation bars instead of a system-default colour.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            AppContent()
        }
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val themeMode by settings.themeMode.collectAsState()
    val dynamicColors by settings.dynamicColors.collectAsState()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }

    NeoToolsTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insets = WindowCompat.getInsetsController(window, view)
                // Light status-bar icons when the app is in dark theme, and vice-versa.
                insets.isAppearanceLightStatusBars = !darkTheme
                insets.isAppearanceLightNavigationBars = !darkTheme
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val navController = rememberNavController()
            NeoNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }
}
