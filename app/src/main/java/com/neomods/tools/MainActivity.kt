package com.neomods.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.neomods.tools.navigation.NeoNavHost
import com.neomods.tools.ui.theme.NeoToolsTheme

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
    NeoToolsTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val navController = rememberNavController()
            NeoNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
    }
}
