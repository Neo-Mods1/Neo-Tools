package com.neomods.tools.util

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Reusable preview annotation that renders a Composable in both light and dark
 * themes, so every screen is verified against the app's theme up front.
 */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
annotation class CombinedPreviews
