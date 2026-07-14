package com.neomods.tools.imageeditor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun EditorCanvas(
    layers: List<EditorLayer>,
    activeTool: EditorTool = EditorTool.SELECT,
    isPickingColor: Boolean = false,
    pickedColor: Int? = null,
    onTap: ((x: Int, y: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .pointerInput(activeTool) {
                if (activeTool == EditorTool.EYEDROPPER || activeTool == EditorTool.CLONE_STAMP) {
                    detectTapGestures { tapOffset ->
                        val x = ((tapOffset.x - offset.x) / scale).roundToInt()
                        val y = ((tapOffset.y - offset.y) / scale).roundToInt()
                        onTap?.invoke(x, y)
                    }
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.1f, 5f)
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        layers
            .filterIsInstance<EditorLayer.Image>()
            .filter { it.isVisible }
            .forEach { layer ->
                Image(
                    bitmap = layer.imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale * layer.scale,
                            scaleY = scale * layer.scale,
                            rotationZ = layer.rotation,
                            translationX = offset.x + layer.position.x,
                            translationY = offset.y + layer.position.y,
                            alpha = layer.opacity
                        )
                )
            }

        // Drawing layers
        layers
            .filterIsInstance<EditorLayer.Drawing>()
            .filter { it.isVisible && it.bitmap != null }
            .forEach { layer ->
                Image(
                    bitmap = layer.bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x + layer.position.x,
                            translationY = offset.y + layer.position.y,
                            alpha = layer.opacity
                        )
                )
            }

        // Text layers
        layers
            .filterIsInstance<EditorLayer.Text>()
            .filter { it.isVisible }
            .forEach { layer ->
                androidx.compose.material3.Text(
                    text = layer.text,
                    color = layer.color,
                    fontSize = (layer.fontSize / scale).sp,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale * layer.scale,
                            scaleY = scale * layer.scale,
                            rotationZ = layer.rotation,
                            translationX = offset.x + layer.position.x,
                            translationY = offset.y + layer.position.y,
                            alpha = layer.opacity
                        )
                )
            }

        // Sticker layers
        layers
            .filterIsInstance<EditorLayer.Sticker>()
            .filter { it.isVisible }
            .forEach { layer ->
                androidx.compose.material3.Text(
                    text = layer.emoji,
                    fontSize = (layer.fontSize / scale).sp,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale * layer.scale,
                            scaleY = scale * layer.scale,
                            rotationZ = layer.rotation,
                            translationX = offset.x + layer.position.x,
                            translationY = offset.y + layer.position.y,
                            alpha = layer.opacity
                        )
                )
            }

        // Shape layers
        layers
            .filterIsInstance<EditorLayer.Shape>()
            .filter { it.isVisible && it.renderedBitmap != null }
            .forEach { layer ->
                Image(
                    bitmap = layer.renderedBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale * layer.scale,
                            scaleY = scale * layer.scale,
                            rotationZ = layer.rotation,
                            translationX = offset.x + layer.position.x,
                            translationY = offset.y + layer.position.y,
                            alpha = layer.opacity
                        )
                )
            }

        // Eyedropper indicator
        if (isPickingColor && activeTool == EditorTool.EYEDROPPER) {
            Icon(
                Icons.Default.Colorize,
                contentDescription = "Eyedropper active",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Picked color indicator
        if (pickedColor != null && activeTool == EditorTool.EYEDROPPER) {
            val color = Color(pickedColor)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(color, MaterialTheme.shapes.medium)
            )
        }
    }
}
