package com.neomods.tools.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.neomods.tools.NeoToolsApplication
import com.neomods.tools.model.Tool

/**
 * Loads a single tool by its `toolId` navigation argument for the placeholder
 * tool screen.
 */
class ToolViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val app = application as NeoToolsApplication

    val toolId: String = savedStateHandle.get<String>("toolId").orEmpty()

    val tool: Tool? = app.toolRepository.getTool(toolId)
}
