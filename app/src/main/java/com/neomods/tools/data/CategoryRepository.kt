package com.neomods.tools.data

import com.neomods.tools.R
import com.neomods.tools.model.Category

/**
 * Provides the static catalogue of tool categories.
 *
 * Implemented as an interface so a future backend (remote config, feature
 * flags, etc.) can swap the source without touching the UI or ViewModels.
 */
interface CategoryRepository {

    /** All categories in display order. */
    fun getCategories(): List<Category>

    /** Lookup a single category by [id], or null when unknown. */
    fun getCategory(id: String): Category?

    /**
     * Real-time search across category titles and descriptions.
     *
     * This is the entry point the Home screen uses for its live filter. To
     * also surface tools later, combine the result of
     * [ToolRepository.searchTools] into the same stream (see [com.neomods.tools.home.HomeViewModel]).
     */
    fun searchCategories(query: String): List<Category>
}

internal class DefaultCategoryRepository : CategoryRepository {

    private val categories = listOf(
        Category(
            id = "apk",
            title = "APK Tools",
            description = "Inspect, sign and rebuild Android packages",
            iconRes = R.drawable.ic_cat_apk
        ),
        Category(
            id = "binary",
            title = "Binary Tools",
            description = "Hex, diff and checksum utilities",
            iconRes = R.drawable.ic_cat_binary
        ),
        Category(
            id = "image",
            title = "Image Tools",
            description = "Optimize and convert images",
            iconRes = R.drawable.ic_cat_image
        ),
        Category(
            id = "xml",
            title = "XML Tools",
            description = "Format, validate and query XML",
            iconRes = R.drawable.ic_cat_xml
        ),
        Category(
            id = "encoding",
            title = "Encoding",
            description = "Base64, URL and hex encoders",
            iconRes = R.drawable.ic_cat_encoding
        ),
        Category(
            id = "text",
            title = "Text Tools",
            description = "Case, sort and regex helpers",
            iconRes = R.drawable.ic_cat_text
        ),
        Category(
            id = "crypto",
            title = "Crypto",
            description = "Hashes and ciphers",
            iconRes = R.drawable.ic_cat_crypto
        ),
        Category(
            id = "developer",
            title = "Developer",
            description = "HTTP, JSON and database tools",
            iconRes = R.drawable.ic_cat_developer
        )
    )

    private val byId = categories.associateBy { it.id }

    override fun getCategories(): List<Category> = categories

    override fun getCategory(id: String): Category? = byId[id]

    override fun searchCategories(query: String): List<Category> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return categories
        return categories.filter {
            it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }
}
