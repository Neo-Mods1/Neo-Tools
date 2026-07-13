package com.neomods.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.neomods.tools.navigation.NeoNavHost
import com.neomods.tools.storage.SettingsManager
import com.neomods.tools.theme.NeoToolsTheme
import com.neomods.tools.theme.ThemeMode
import androidx.compose.runtime.collectAsState

/**
 * Single, Compose-only activity. All screens are hosted by [NeoNavHost].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val navController = rememberNavController()
            NeoNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }
}