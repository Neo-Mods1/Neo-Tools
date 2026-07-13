package com.neomods.tools.ui.theme

import androidx.compose.ui.graphics.Brush

/**
 * Reusable brand gradients that give the UI its "cool" Material 3 accent.
 *
 * These are fixed brand colors (not pulled from the dynamic color scheme) so
 * the hero accents stay on-brand even when Material You recolors the rest of
 * the surface.
 */
object NeoGradients {

    /** Horizontal violet -> blue sweep used for the primary hero button. */
    val Primary: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(NeoGradientStart, NeoGradientEnd)
        )

    /** Vertical variant for large hero surfaces. */
    val PrimaryVertical: Brush
        get() = Brush.verticalGradient(
            colors = listOf(NeoGradientStart, NeoGradientEnd)
        )
}