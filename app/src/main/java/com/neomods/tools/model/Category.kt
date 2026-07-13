package com.neomods.tools.model

import androidx.compose.runtime.Immutable

/**
 * Immutable UI model describing a top-level tool category shown on the Home
 * screen. All fields are value types so the model is safe to snapshot and
 * compare inside Compose without triggering unnecessary recompositions.
 *
 * @param id        Stable unique identifier, also used to build the navigation
 *                  route for the category screen.
 * @param title     Display name.
 * @param description Short summary shown under the title.
 * @param iconRes   Drawable resource for the category icon.
 * @param tags      A few representative tool names surfaced on the Home card
 *                  (e.g. "Dex • Smali • Manifest") so users see what's inside.
 */
@Immutable
data class Category(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val tags: List<String> = emptyList()
)
