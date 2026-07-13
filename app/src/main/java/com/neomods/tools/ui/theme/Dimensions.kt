package com.neomods.tools.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared spacing and sizing constants so every screen uses a consistent,
 * minimalist layout. Centralised to avoid magic numbers spread across the UI.
 */
object NeoDimens {
    val ScreenPadding = 16.dp
    val CardPadding = 18.dp
    val CardCorner = 22.dp
    val SectionSpacing = 12.dp
    val GroupSpacing = 8.dp

    val IconSize = 24.dp

    /** Large monochrome lead icons (no background) used on the premium cards. */
    val LeadIconSize = 46.dp
    val LeadIconSizeLarge = 56.dp

    /** Default card elevation for the surfaced, "toolbox" look. */
    val CardElevation = 2.dp

    val SearchBarCorner = 28.dp
}
