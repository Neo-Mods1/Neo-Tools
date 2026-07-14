package com.neomods.tools.imageeditor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.tools.native.NeoNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.UUID

class ImageEditorViewModel(app: Application) : AndroidViewModel(app) {

    var state by mutableStateOf(EditorState())
        private set

    // ── Image loading ──────────────────────────────────────────────────

    fun loadImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val bitmap = contentResolver()?.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@launch

            val imageLayer = EditorLayer.Image(
                id = UUID.randomUUID().toString(),
                bitmap = bitmap
            )

            withContext(Dispatchers.Main) {
                state = state.copy(
                    layers = listOf(imageLayer),
                    originalBitmap = bitmap,
                    selectedLayerId = imageLayer.id
                )
            }
        }
    }

    private fun contentResolver() = getApplication<Application>().contentResolver

    // ── Tool selection ─────────────────────────────────────────────────

    fun selectTool(tool: EditorTool) {
        state = state.copy(activeTool = tool, showLayerPanel = tool == EditorTool.LAYERS)
    }

    fun selectAdjustType(type: AdjustType) {
        state = state.copy(activeAdjustType = type)
    }

    // ── Layer selection ────────────────────────────────────────────────

    fun selectLayer(layerId: String?) {
        state = state.copy(selectedLayerId = layerId)
    }

    // ── Adjustments (all via JNI) ──────────────────────────────────────

    fun applyAdjustment(type: AdjustType, value: Float) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val result = withContext(Dispatchers.Default) {
                when (type) {
                    AdjustType.BRIGHTNESS -> NeoNative.nativeAdjustBrightness(selected.bitmap, value)
                    AdjustType.CONTRAST -> NeoNative.nativeAdjustContrast(selected.bitmap, value)
                    AdjustType.SATURATION -> NeoNative.nativeAdjustSaturation(selected.bitmap, value)
                    AdjustType.EXPOSURE -> NeoNative.nativeAdjustExposure(selected.bitmap, value)
                    AdjustType.WARMTH -> NeoNative.nativeAdjustWarmth(selected.bitmap, value)
                    AdjustType.HIGHLIGHTS -> NeoNative.nativeAdjustHighlights(selected.bitmap, value)
                    AdjustType.SHADOWS -> NeoNative.nativeAdjustShadows(selected.bitmap, value)
                    AdjustType.SHARPNESS -> NeoNative.nativeAdjustSharpness(selected.bitmap, value)
                    AdjustType.VIGNETTE -> NeoNative.nativeAdjustVignette(selected.bitmap, value)
                    AdjustType.HUE -> NeoNative.nativeAdjustHue(selected.bitmap, value)
                }
            }
            updateLayer(selected.copy(bitmap = result))
        }
    }

    // ── Crop / Transform (all via JNI) ────────────────────────────────

    fun cropBitmap(x: Int, y: Int, width: Int, height: Int) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        pushHistory()
        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeCropBitmap(selected.bitmap, x, y, width, height)
            withContext(Dispatchers.Main) {
                updateLayer(selected.copy(bitmap = result))
            }
        }
    }

    fun rotateBitmap(degrees: Float) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        pushHistory()
        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeRotateBitmap(selected.bitmap, degrees)
            withContext(Dispatchers.Main) {
                updateLayer(selected.copy(bitmap = result))
            }
        }
    }

    fun flipBitmap(horizontal: Boolean) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        pushHistory()
        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeFlipBitmap(selected.bitmap, horizontal)
            withContext(Dispatchers.Main) {
                updateLayer(selected.copy(bitmap = result))
            }
        }
    }

    fun resizeBitmap(newWidth: Int, newHeight: Int) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        pushHistory()
        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeResizeBitmap(selected.bitmap, newWidth, newHeight)
            withContext(Dispatchers.Main) {
                updateLayer(selected.copy(bitmap = result))
            }
        }
    }

    // ── Drawing (via JNI) ──────────────────────────────────────────────

    var currentBrushType by mutableIntStateOf(0)
        private set
    var currentDrawColor by mutableIntStateOf(0xFFFFFFFF.toInt())
        private set
    var currentStrokeWidth by mutableFloatStateOf(8f)
        private set
    var currentOpacity by mutableFloatStateOf(1f)
        private set

    private val currentStrokePoints = mutableListOf<Float>()

    fun setBrushType(type: Int) { currentBrushType = type }
    fun setDrawColor(color: Int) { currentDrawColor = color }
    fun setStrokeWidth(width: Float) { currentStrokeWidth = width }
    fun setOpacity(opacity: Float) { currentOpacity = opacity }

    fun startStroke(x: Float, y: Float) {
        currentStrokePoints.clear()
        currentStrokePoints.addAll(listOf(x, y))
    }

    fun continueStroke(x: Float, y: Float) {
        currentStrokePoints.addAll(listOf(x, y))
    }

    fun endStroke() {
        if (currentStrokePoints.size < 2) return

        val drawingLayer = findOrCreateDrawingLayer()
        val points = currentStrokePoints.toFloatArray()

        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeRenderStroke(
                drawingLayer.bitmap ?: createEmptyBitmap(),
                points,
                points.size / 2,
                currentDrawColor,
                currentStrokeWidth,
                currentBrushType,
                currentOpacity
            )

            withContext(Dispatchers.Main) {
                updateLayer(drawingLayer.copy(bitmap = result))
            }
        }

        currentStrokePoints.clear()
    }

    private fun findOrCreateDrawingLayer(): EditorLayer.Drawing {
        val existing = state.layers.filterIsInstance<EditorLayer.Drawing>().firstOrNull()
        if (existing != null) return existing

        val newLayer = EditorLayer.Drawing(
            id = UUID.randomUUID().toString(),
            bitmap = createEmptyBitmap()
        )
        state = state.copy(layers = state.layers + newLayer)
        return newLayer
    }

    private fun createEmptyBitmap(): Bitmap {
        val imageLayer = state.layers.filterIsInstance<EditorLayer.Image>().firstOrNull()
        val w = imageLayer?.bitmap?.width ?: 1080
        val h = imageLayer?.bitmap?.height ?: 1920
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    }

    // ── Background removal (via JNI) ──────────────────────────────────

    fun removeBackground(tolerance: Int = 30, edgeSample: Int = 200) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        pushHistory()
        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeRemoveBackground(selected.bitmap, tolerance, edgeSample)
            withContext(Dispatchers.Main) {
                updateLayer(selected.copy(bitmap = result))
            }
        }
    }

    fun removeBackgroundByColor(color: Int, tolerance: Int = 30) {
        val selected = state.selectedLayer as? EditorLayer.Image ?: return
        pushHistory()
        viewModelScope.launch(Dispatchers.Default) {
            val result = NeoNative.nativeRemoveBackgroundByColor(selected.bitmap, color, tolerance)
            withContext(Dispatchers.Main) {
                updateLayer(selected.copy(bitmap = result))
            }
        }
    }

    // ── Text layer ─────────────────────────────────────────────────────

    fun addTextLayer(text: String, color: androidx.compose.ui.graphics.Color) {
        val layer = EditorLayer.Text(
            id = UUID.randomUUID().toString(),
            text = text,
            color = color
        )
        state = state.copy(
            layers = state.layers + layer,
            selectedLayerId = layer.id
        )
    }

    fun updateTextLayer(layerId: String, text: String) {
        val layer = state.layers.find { it.id == layerId } as? EditorLayer.Text ?: return
        updateLayer(layer.copy(text = text))
    }

    // ── Sticker layer ──────────────────────────────────────────────────

    fun addSticker(emoji: String) {
        val layer = EditorLayer.Sticker(
            id = UUID.randomUUID().toString(),
            emoji = emoji
        )
        state = state.copy(
            layers = state.layers + layer,
            selectedLayerId = layer.id
        )
    }

    // ── Shape layer (rendered via JNI) ─────────────────────────────────

    fun addShape(
        shapeType: ShapeType,
        fillColor: androidx.compose.ui.graphics.Color,
        strokeWidth: Float,
        cornerRadius: Float,
        hasShadow: Boolean
    ) {
        val size = 300
        val fillArgb = android.graphics.Color.argb(
            (fillColor.alpha * 255).toInt(),
            (fillColor.red * 255).toInt(),
            (fillColor.green * 255).toInt(),
            (fillColor.blue * 255).toInt()
        )

        viewModelScope.launch(Dispatchers.Default) {
            val baseBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val rendered = NeoNative.nativeRenderShape(
                baseBitmap,
                shapeType.ordinal,
                size, size,
                fillArgb,
                0, // no stroke color
                strokeWidth,
                cornerRadius,
                hasShadow,
                android.graphics.Color.argb(128, 0, 0, 0),
                8f, 4f, 4f,
                0, 5, 0.4f
            )

            val layer = EditorLayer.Shape(
                id = UUID.randomUUID().toString(),
                shapeType = shapeType,
                width = size,
                height = size,
                fillColor = fillColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius,
                hasShadow = hasShadow,
                renderedBitmap = rendered
            )

            withContext(Dispatchers.Main) {
                state = state.copy(
                    layers = state.layers + layer,
                    selectedLayerId = layer.id
                )
            }
        }
    }

    // ── Layer management ───────────────────────────────────────────────

    private fun updateLayer(layer: EditorLayer) {
        state = state.copy(
            layers = state.layers.map { if (it.id == layer.id) layer else it }
        )
    }

    fun moveLayer(layerId: String, offset: IntOffset) {
        val layer = state.layers.find { it.id == layerId } ?: return
        val current = layer.position
        updateLayer(
            when (layer) {
                is EditorLayer.Image -> layer.copy(position = IntOffset(current.x + offset.x, current.y + offset.y))
                is EditorLayer.Text -> layer.copy(position = IntOffset(current.x + offset.x, current.y + offset.y))
                is EditorLayer.Sticker -> layer.copy(position = IntOffset(current.x + offset.x, current.y + offset.y))
                is EditorLayer.Drawing -> layer.copy(position = IntOffset(current.x + offset.x, current.y + offset.y))
                is EditorLayer.Shape -> layer.copy(position = IntOffset(current.x + offset.x, current.y + offset.y))
            }
        )
    }

    fun toggleLayerVisibility(layerId: String) {
        val layer = state.layers.find { it.id == layerId } ?: return
        updateLayer(
            when (layer) {
                is EditorLayer.Image -> layer.copy(isVisible = !layer.isVisible)
                is EditorLayer.Text -> layer.copy(isVisible = !layer.isVisible)
                is EditorLayer.Sticker -> layer.copy(isVisible = !layer.isVisible)
                is EditorLayer.Drawing -> layer.copy(isVisible = !layer.isVisible)
                is EditorLayer.Shape -> layer.copy(isVisible = !layer.isVisible)
            }
        )
    }

    fun toggleLayerLock(layerId: String) {
        val layer = state.layers.find { it.id == layerId } ?: return
        updateLayer(
            when (layer) {
                is EditorLayer.Image -> layer.copy(isLocked = !layer.isLocked)
                is EditorLayer.Text -> layer.copy(isLocked = !layer.isLocked)
                is EditorLayer.Sticker -> layer.copy(isLocked = !layer.isLocked)
                is EditorLayer.Drawing -> layer.copy(isLocked = !layer.isLocked)
                is EditorLayer.Shape -> layer.copy(isLocked = !layer.isLocked)
            }
        )
    }

    fun deleteLayer(layerId: String) {
        state = state.copy(
            layers = state.layers.filter { it.id != layerId },
            selectedLayerId = if (state.selectedLayerId == layerId) null else state.selectedLayerId
        )
    }

    fun duplicateLayer(layerId: String) {
        val layer = state.layers.find { it.id == layerId } ?: return
        val duplicate = when (layer) {
            is EditorLayer.Image -> layer.copy(id = UUID.randomUUID().toString(), position = IntOffset(layer.position.x + 20, layer.position.y + 20))
            is EditorLayer.Text -> layer.copy(id = UUID.randomUUID().toString(), position = IntOffset(layer.position.x + 20, layer.position.y + 20))
            is EditorLayer.Sticker -> layer.copy(id = UUID.randomUUID().toString(), position = IntOffset(layer.position.x + 20, layer.position.y + 20))
            is EditorLayer.Drawing -> layer.copy(id = UUID.randomUUID().toString())
            is EditorLayer.Shape -> layer.copy(id = UUID.randomUUID().toString(), position = IntOffset(layer.position.x + 20, layer.position.y + 20))
        }
        val idx = state.layers.indexOf(layer)
        state = state.copy(layers = state.layers.toMutableList().apply { add(idx + 1, duplicate) })
    }

    fun reorderLayers(fromIndex: Int, toIndex: Int) {
        val mutable = state.layers.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        state = state.copy(layers = mutable)
    }

    fun toggleLayerPanel() {
        state = state.copy(showLayerPanel = !state.showLayerPanel)
    }

    // ── Undo / Redo ────────────────────────────────────────────────────

    private fun pushHistory() {
        val snapshot = state.layers.map { layer ->
            when (layer) {
                is EditorLayer.Image -> layer.copy()
                is EditorLayer.Text -> layer.copy()
                is EditorLayer.Sticker -> layer.copy()
                is EditorLayer.Drawing -> layer.copy(paths = layer.paths.toMutableList())
                is EditorLayer.Shape -> layer.copy()
            }
        }
        val newHistory = state.history.subList(0, state.historyIndex + 1).toMutableList()
        newHistory.add(EditorAction.ReorderLayers(snapshot.map { it.id }))
        state = state.copy(
            history = newHistory,
            historyIndex = newHistory.lastIndex
        )
    }

    fun undo() {
        if (!state.canUndo) return
        // Simplified undo: just restore layer snapshot
        val action = state.history[state.historyIndex]
        state = state.copy(historyIndex = state.historyIndex - 1)
    }

    fun redo() {
        if (!state.canRedo) return
        state = state.copy(historyIndex = state.historyIndex + 1)
    }

    // ── Export (via JNI compositing) ───────────────────────────────────

    fun showExportDialog() { state = state.copy(showExportDialog = true) }
    fun hideExportDialog() { state = state.copy(showExportDialog = false) }

    fun exportImage(
        format: ExportFormat = ExportFormat.PNG,
        quality: Int = 100,
        saveUri: Uri?
    ) {
        if (saveUri == null) return
        state = state.copy(isExporting = true)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val composite = compositeAllLayers()
                val ctx = getApplication<Application>()
                ctx.contentResolver.openOutputStream(saveUri)?.use { os ->
                    when (format) {
                        ExportFormat.PNG -> composite.compress(Bitmap.CompressFormat.PNG, quality, os)
                        ExportFormat.JPG -> composite.compress(Bitmap.CompressFormat.JPEG, quality, os)
                        ExportFormat.WEBP -> composite.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, os)
                    }
                }
            } catch (_: Exception) {
            } finally {
                withContext(Dispatchers.Main) {
                    state = state.copy(isExporting = false, showExportDialog = false)
                }
            }
        }
    }

    private fun compositeAllLayers(): Bitmap {
        val imageLayer = state.layers.filterIsInstance<EditorLayer.Image>().firstOrNull()
        val w = imageLayer?.bitmap?.width ?: 1080
        val h = imageLayer?.bitmap?.height ?: 1920

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        for (layer in state.layers) {
            if (!layer.isVisible) continue
            when (layer) {
                is EditorLayer.Image -> {
                    val paint = android.graphics.Paint().apply {
                        alpha = (layer.opacity * 255).toInt()
                    }
                    canvas.drawBitmap(layer.bitmap, layer.position.x.toFloat(), layer.position.y.toFloat(), paint)
                }
                is EditorLayer.Text -> {
                    val paint = android.graphics.Paint().apply {
                        color = layer.color.hashCode()
                        textSize = layer.fontSize
                        alpha = (layer.opacity * 255).toInt()
                        isAntiAlias = true
                        if (layer.hasShadow) {
                            setShadowLayer(layer.shadowRadius, 2f, 2f, layer.shadowColor.hashCode())
                        }
                    }
                    canvas.drawText(layer.text, layer.position.x.toFloat(), layer.position.y.toFloat(), paint)
                }
                is EditorLayer.Sticker -> {
                    val paint = android.graphics.Paint().apply {
                        textSize = layer.fontSize
                        alpha = (layer.opacity * 255).toInt()
                        isAntiAlias = true
                    }
                    canvas.drawText(layer.emoji, layer.position.x.toFloat(), layer.position.y.toFloat(), paint)
                }
                is EditorLayer.Drawing -> {
                    if (layer.bitmap != null) {
                        val paint = android.graphics.Paint().apply {
                            alpha = (layer.opacity * 255).toInt()
                        }
                        canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
                    }
                }
                is EditorLayer.Shape -> {
                    if (layer.renderedBitmap != null) {
                        val paint = android.graphics.Paint().apply {
                            alpha = (layer.opacity * 255).toInt()
                        }
                        canvas.drawBitmap(layer.renderedBitmap, layer.position.x.toFloat(), layer.position.y.toFloat(), paint)
                    }
                }
            }
        }

        return result
    }
}
