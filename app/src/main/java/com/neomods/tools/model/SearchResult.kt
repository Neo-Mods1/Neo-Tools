package com.neomods.tools.model

/**
 * Unified search result used by the Home screen.
 *
 * Today only [CategoryResult] is produced during a live search. [ToolResult]
 * is already modelled so tool search can be enabled later without changing the
 * UI layer — the Home grid simply renders whatever [SearchResult]s are emitted.
 */
sealed interface SearchResult {

    val title: String
    val description: String
    val iconRes: Int

    data class CategoryResult(val category: Category) : SearchResult {
        override val title: String get() = category.title
        override val description: String get() = category.description
        override val iconRes: Int get() = category.iconRes
    }

    data class ToolResult(val tool: Tool) : SearchResult {
        override val title: String get() = tool.title
        override val description: String get() = tool.description
        override val iconRes: Int get() = tool.iconRes
    }
}
