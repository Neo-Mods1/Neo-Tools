package com.neomods.tools.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.neomods.tools.NeoToolsApplication
import com.neomods.tools.model.Category
import com.neomods.tools.model.Tool

/**
 * Loads the tools that belong to a category identified by the `categoryId`
 * navigation argument.
 */
class CategoryViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val app = application as NeoToolsApplication

    val categoryId: String = savedStateHandle.get<String>("categoryId").orEmpty()

    val category: Category? = app.categoryRepository.getCategory(categoryId)

    val tools: List<Tool> = app.toolRepository.getTools(categoryId)
}
