package com.neomods.tools.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.neomods.tools.NeoToolsApplication
import com.neomods.tools.model.Category
import com.neomods.tools.model.SearchResult
import com.neomods.tools.model.Tool

/**
 * Drives the Home screen: exposes the catalogue and a pure search function.
 *
 * The live query state lives in the UI layer (via [androidx.compose.runtime.mutableStateOf])
 * so this ViewModel stays free of Compose types and the filtering logic can be
 * reused or moved behind a repository/use-case later without UI changes.
 *
 * [searchTools] is already wired so enabling tool search later only requires
 * the UI to render the returned [Tool]s (see [com.neomods.tools.ui.screens.HomeScreen]).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val categoryRepository = (application as NeoToolsApplication).categoryRepository
    private val toolRepository = (application as NeoToolsApplication).toolRepository

    fun getCategories(): List<Category> = categoryRepository.getCategories()

    fun searchCategories(query: String): List<Category> =
        categoryRepository.searchCategories(query)

    fun searchTools(query: String): List<Tool> =
        if (query.isBlank()) emptyList() else toolRepository.searchTools(query)

    /** Combined results (categories first, then tools) for a query. */
    fun search(query: String): List<SearchResult> = buildList {
        categoryRepository.searchCategories(query).mapTo(this) { SearchResult.CategoryResult(it) }
        if (query.isNotBlank()) {
            toolRepository.searchTools(query).mapTo(this) { SearchResult.ToolResult(it) }
        }
    }
}
