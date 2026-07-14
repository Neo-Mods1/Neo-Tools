package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntOffset

sealed class EditorAction {
    data class AddLayer(val layer: EditorLayer) : EditorAction()
    data class RemoveLayer(val layerId: String) : EditorAction()
    data class MoveLayer(val layerId: String, val newPosition: IntOffset) : EditorAction()
    data class ScaleLayer(val layerId: String, val newScale: Float) : EditorAction()
    data class RotateLayer(val layerId: String, val newRotation: Float) : EditorAction()
    data class OpacityLayer(val layerId: String, val newOpacity: Float) : EditorAction()
    data class ToggleVisibility(val layerId: String, val isVisible: Boolean) : EditorAction()
    data class ToggleLock(val layerId: String, val isLocked: Boolean) : EditorAction()
    data class RenameLayer(val layerId: String, val newName: String) : EditorAction()
    data class ReorderLayers(val layerIds: List<String>) : EditorAction()
    data class UpdateAdjustments(val layerId: String, val adjustments: Adjustments) : EditorAction()
    data class UpdateText(val layerId: String, val text: String) : EditorAction()
    data class AddDrawingPath(val layerId: String, val path: DrawingPath) : EditorAction()
    data class CropBitmap(val newBitmap: Bitmap) : EditorAction()
}
