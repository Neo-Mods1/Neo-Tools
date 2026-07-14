package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset

sealed class EditorLayer(
    val id: String,
    val name: String,
    val position: IntOffset = IntOffset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
) {
    data class Image(
        override val id: String,
        val bitmap: Bitmap,
        val cropRect: android.graphics.RectF? = null,
        val adjustments: Adjustments = Adjustments(),
        override val position: IntOffset = IntOffset.Zero,
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
    ) : EditorLayer(id, "Image", position, scale, rotation, opacity, isVisible, isLocked) {
        val imageBitmap get() = bitmap.asImageBitmap()
    }

    data class Text(
        override val id: String,
        val text: String,
        val color: Color = Color.White,
        val fontSize: Float = 48f,
        val fontFamily: String = "default",
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val hasShadow: Boolean = false,
        val shadowColor: Color = Color.Black,
        val shadowRadius: Float = 4f,
        val backgroundColor: Color = Color.Transparent,
        override val position: IntOffset = IntOffset(100, 100),
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
    ) : EditorLayer(id, "Text", position, scale, rotation, opacity, isVisible, isLocked)

    data class Sticker(
        override val id: String,
        val emoji: String,
        val fontSize: Float = 72f,
        override val position: IntOffset = IntOffset(100, 100),
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
    ) : EditorLayer(id, "Sticker", position, scale, rotation, opacity, isVisible, isLocked)

    data class Drawing(
        override val id: String,
        val bitmap: Bitmap? = null,
        val paths: MutableList<DrawingPath> = mutableListOf(),
        override val position: IntOffset = IntOffset.Zero,
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
    ) : EditorLayer(id, "Drawing", position, scale, rotation, opacity, isVisible, isLocked)
}

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float = 1f,
    val isEraser: Boolean = false,
)

data class Adjustments(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val warmth: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Float = 0f,
    val hue: Float = 0f,
)

enum class EditorTool {
    SELECT,
    ADJUST,
    CROP,
    DRAW,
    TEXT,
    STICKER,
    LAYERS,
    BG_ERASER,
}

enum class AdjustType {
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    EXPOSURE,
    WARMTH,
    HIGHLIGHTS,
    SHADOWS,
    SHARPNESS,
    VIGNETTE,
    HUE,
}

enum class DrawBrushType {
    PENCIL,
    MARKER,
    AIRBRUSH,
    ERASER,
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT("4:5", 4f / 5f),
    LANDSCAPE("5:4", 5f / 4f),
    WIDESCREEN("16:9", 16f / 9f),
    PHONE("9:16", 9f / 16f),
    CLASSIC("4:3", 4f / 3f),
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),
}
