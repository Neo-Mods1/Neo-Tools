package com.neomods.tools.imageeditor

import android.graphics.Bitmap

data class EditorState(
    val layers: List<EditorLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val activeTool: EditorTool = EditorTool.SELECT,
    val activeAdjustType: AdjustType = AdjustType.BRIGHTNESS,
    val drawBrushType: DrawBrushType = DrawBrushType.PENCIL,
    val drawColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val drawStrokeWidth: Float = 8f,
    val cropAspectRatio: CropAspectRatio = CropAspectRatio.FREE,
    val isCropping: Boolean = false,
    val history: List<EditorAction> = emptyList(),
    val historyIndex: Int = -1,
    val isExporting: Boolean = false,
    val showExportDialog: Boolean = false,
    val showLayerPanel: Boolean = false,
    val canvasZoom: Float = 1f,
    val canvasOffset: androidx.compose.ui.unit.IntOffset = androidx.compose.ui.unit.IntOffset.Zero,
    val originalBitmap: Bitmap? = null,
) {
    val canUndo: Boolean get() = historyIndex >= 0
    val canRedo: Boolean get() = historyIndex < history.lastIndex
    val selectedLayer: EditorLayer? get() = layers.find { it.id == selectedLayerId }
}
