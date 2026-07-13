package com.neomods.tools.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Neo Tools brand colors.
 *
 * Centralised so the whole UI shares one palette. The XML crash theme
 * (res/values/colors.xml) mirrors these values for the non-Compose screen.
 */
internal val NeoPrimary = Color(0xFF4F5BD5)
internal val NeoOnPrimary = Color(0xFFFFFFFF)
internal val NeoPrimaryContainer = Color(0xFFDEE0FF)
internal val NeoOnPrimaryContainer = Color(0xFF00115A)

internal val NeoSurfaceLight = Color(0xFFFFFBFE)
internal val NeoOnSurfaceLight = Color(0xFF1A1B21)
internal val NeoOnSurfaceVariantLight = Color(0xFF46464F)

internal val NeoSurfaceDark = Color(0xFF121319)
internal val NeoOnSurfaceDark = Color(0xFFE4E1E6)
internal val NeoOnSurfaceVariantDark = Color(0xFFC7C5D0)

internal val NeoError = Color(0xFFBA1A1A)
internal val NeoErrorDark = Color(0xFFFFB4AB)

/** Cool brand gradient (violet -> blue) used for hero accents and buttons. */
internal val NeoGradientStart = Color(0xFF7C4DFF)
internal val NeoGradientEnd = Color(0xFF4FC3F7)

/** Material 3 secondary role — fuller, more expressive palette. */
internal val NeoSecondary = Color(0xFF5B5DB0)
internal val NeoOnSecondary = Color(0xFFFFFFFF)
internal val NeoSecondaryContainer = Color(0xFFE0E0FF)
internal val NeoOnSecondaryContainer = Color(0xFF16164B)

/** Material 3 tertiary role. */
internal val NeoTertiary = Color(0xFF7C4DFF)
internal val NeoOnTertiary = Color(0xFFFFFFFF)
internal val NeoTertiaryContainer = Color(0xFFEADDFF)
internal val NeoOnTertiaryContainer = Color(0xFF250056)

/** Surface variants + outline for tonal elevation (M3 "cool" surfaces). */
internal val NeoSurfaceVariantLight = Color(0xFFE6E0EC)
internal val NeoSurfaceVariantDark = Color(0xFF2A2833)
internal val NeoOutlineLight = Color(0xFF777680)
internal val NeoOutlineDark = Color(0xFF928F9A)
