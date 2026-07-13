package com.neomods.tools.model

import androidx.compose.runtime.Immutable

/**
 * Immutable UI model describing a single tool inside a [Category].
 *
 * Tools are placeholders for now: they all open the same
 * [com.neomods.tools.ui.screens.PlaceholderToolScreen]. The [id] is routed to
 * that screen so future implementations can map it to a real feature.
 *
 * @param id          Stable unique identifier (route argument for the tool).
 * @param categoryId  Parent [Category.id].
 * @param title       Display name.
 * @param description Short summary shown under the title.
 * @param iconRes     Drawable resource for the tool icon.
 */
@Immutable
data class Tool(
    val id: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val iconRes: Int
)
