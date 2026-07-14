package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntOffset

data class EditorState(
    val layers: List<EditorLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val activeTool: EditorTool = EditorTool.SELECT,
    val activeAdjustType: AdjustType = AdjustType.BRIGHTNESS,
    val activeFilterType: FilterType = FilterType.GRAYSCALE,
    val activeBlendMode: BlendMode = BlendMode.NORMAL,
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
    val canvasOffset: IntOffset = IntOffset.Zero,
    val originalBitmap: Bitmap? = null,
    // Eyedropper
    val pickedColor: Int? = null,
    val isPickingColor: Boolean = false,
    // Histogram
    val histogram: IntArray? = null,
    // Clone stamp
    val isCloning: Boolean = false,
    val cloneSrcX: Int = 0,
    val cloneSrcY: Int = 0,
    // Curves
    val curvesLut: IntArray = (0..255).toList().toIntArray(),
    // Multi-select
    val selectedLayerIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
) {
    val canUndo: Boolean get() = historyIndex >= 0
    val canRedo: Boolean get() = historyIndex < history.lastIndex
    val selectedLayer: EditorLayer? get() = layers.find { it.id == selectedLayerId }
}
