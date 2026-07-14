package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset

sealed class EditorLayer {
    abstract val id: String
    abstract val name: String
    abstract val position: IntOffset
    abstract val scale: Float
    abstract val rotation: Float
    abstract val opacity: Float
    abstract val isVisible: Boolean
    abstract val isLocked: Boolean

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
    ) : EditorLayer() {
        override val name: String get() = "Image"
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
    ) : EditorLayer() {
        override val name: String get() = "Text"
    }

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
    ) : EditorLayer() {
        override val name: String get() = "Sticker"
    }

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
    ) : EditorLayer() {
        override val name: String get() = "Drawing"
    }

    data class Shape(
        override val id: String,
        val shapeType: ShapeType = ShapeType.RECTANGLE,
        val width: Int = 200,
        val height: Int = 200,
        val fillColor: Color = Color.White,
        val strokeColor: Color = Color.Transparent,
        val strokeWidth: Float = 0f,
        val cornerRadius: Float = 0f,
        val hasShadow: Boolean = false,
        val shadowColor: Color = Color.Black.copy(alpha = 0.5f),
        val shadowRadius: Float = 8f,
        val shadowOffsetX: Float = 4f,
        val shadowOffsetY: Float = 4f,
        val triangleDirection: TriangleDirection = TriangleDirection.UP,
        val starPoints: Int = 5,
        val starInnerRadius: Float = 0.4f,
        val renderedBitmap: Bitmap? = null,
        override val position: IntOffset = IntOffset(100, 100),
        override val scale: Float = 1f,
        override val rotation: Float = 0f,
        override val opacity: Float = 1f,
        override val isVisible: Boolean = true,
        override val isLocked: Boolean = false,
    ) : EditorLayer() {
        override val name: String get() = "Shape"
    }
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
    SHAPE,
    BG_ERASER,
    LAYERS,
}

enum class ShapeType {
    RECTANGLE,
    ROUNDED_RECTANGLE,
    CIRCLE,
    OVAL,
    TRIANGLE,
    STAR,
    DIAMOND,
    ARROW_RIGHT,
    ARROW_LEFT,
    ARROW_UP,
    ARROW_DOWN,
    HEXAGON,
    OCTAGON,
    HEART,
    CROSS,
    OCTAGON_STAR,
}

enum class TriangleDirection {
    UP, DOWN, LEFT, RIGHT
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
