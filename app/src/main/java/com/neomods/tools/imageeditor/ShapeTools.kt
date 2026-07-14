package com.neomods.tools.imageeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ShapeItem(
    val type: ShapeType,
    val label: String,
    val pathData: PathData,
)

private val shapeItems = listOf(
    ShapeItem(ShapeType.RECTANGLE, "Rect",
        PathData { addRect(0f, 0f, 24f, 24f) }),
    ShapeItem(ShapeType.ROUNDED_RECTANGLE, "Round Rect",
        PathData { addRoundRect(0f, 0f, 24f, 24f, 4f, 4f) }),
    ShapeItem(ShapeType.CIRCLE, "Circle",
        PathData { addOval(0f, 0f, 24f, 24f) }),
    ShapeItem(ShapeType.OVAL, "Oval",
        PathData { addOval(0f, 2f, 24f, 22f) }),
    ShapeItem(ShapeType.TRIANGLE, "Triangle",
        PathData { moveTo(12f, 0f); lineTo(24f, 24f); lineTo(0f, 24f); close() }),
    ShapeItem(ShapeType.DIAMOND, "Diamond",
        PathData { moveTo(12f, 0f); lineTo(24f, 12f); lineTo(12f, 24f); lineTo(0f, 12f); close() }),
    ShapeItem(ShapeType.STAR, "Star",
        PathData {
            moveTo(12f, 0f); lineTo(15f, 9f); lineTo(24f, 9f); lineTo(17f, 15f)
            lineTo(19f, 24f); lineTo(12f, 18f); lineTo(5f, 24f); lineTo(7f, 15f)
            lineTo(0f, 9f); lineTo(9f, 9f); close()
        }),
    ShapeItem(ShapeType.HEART, "Heart",
        PathData {
            moveTo(12f, 22f); cubicTo(12f, 22f, 0f, 16f, 0f, 8f)
            cubicTo(0f, 3f, 4f, 0f, 8f, 0f); cubicTo(10f, 0f, 12f, 2f, 12f, 4f)
            cubicTo(12f, 2f, 14f, 0f, 16f, 0f); cubicTo(20f, 0f, 24f, 3f, 24f, 8f)
            cubicTo(24f, 16f, 12f, 22f, 12f, 22f); close()
        }),
    ShapeItem(ShapeType.CROSS, "Cross",
        PathData {
            moveTo(8f, 0f); lineTo(16f, 0f); lineTo(16f, 8f); lineTo(24f, 8f)
            lineTo(24f, 16f); lineTo(16f, 16f); lineTo(16f, 24f); lineTo(8f, 24f)
            lineTo(8f, 16f); lineTo(0f, 16f); lineTo(0f, 8f); lineTo(8f, 8f); close()
        }),
    ShapeItem(ShapeType.ARROW_RIGHT, "Arrow R",
        PathData {
            moveTo(0f, 8f); lineTo(14f, 8f); lineTo(14f, 2f); lineTo(24f, 12f)
            lineTo(14f, 22f); lineTo(14f, 16f); lineTo(0f, 16f); close()
        }),
    ShapeItem(ShapeType.ARROW_LEFT, "Arrow L",
        PathData {
            moveTo(24f, 8f); lineTo(10f, 8f); lineTo(10f, 2f); lineTo(0f, 12f)
            lineTo(10f, 22f); lineTo(10f, 16f); lineTo(24f, 16f); close()
        }),
    ShapeItem(ShapeType.HEXAGON, "Hexagon",
        PathData {
            moveTo(12f, 0f); lineTo(22f, 6f); lineTo(22f, 18f); lineTo(12f, 24f)
            lineTo(2f, 18f); lineTo(2f, 6f); close()
        }),
    ShapeItem(ShapeType.OCTAGON, "Octagon",
        PathData {
            moveTo(8f, 0f); lineTo(16f, 0f); lineTo(24f, 8f); lineTo(24f, 16f)
            lineTo(16f, 24f); lineTo(8f, 24f); lineTo(0f, 16f); lineTo(0f, 8f); close()
        }),
)

private val shapeColors = listOf(
    Color.White, Color.Black, Color.Red, Color.Green, Color.Blue,
    Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFFFF6B35),
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6),
)

@Composable
fun ShapeTools(
    onAddShape: (ShapeType, Color, Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedColor by remember { mutableStateOf(Color.White) }
    var strokeWidth by remember { mutableFloatStateOf(0f) }
    var cornerRadius by remember { mutableFloatStateOf(0f) }
    var hasShadow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Shapes", style = MaterialTheme.typography.titleSmall)

        // Shape grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 180.dp)
        ) {
            items(shapeItems) { item ->
                val painter = rememberVectorPainter(24f, 24f) { _, _ ->
                    Path(item.pathData)
                }
                Surface(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable {
                            onAddShape(item.type, selectedColor, strokeWidth, cornerRadius, hasShadow)
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                // Simple scaled version
                                val scale = size.minDimension / 24f
                                val path = androidx.compose.ui.graphics.Path()
                                // Use vector path approach
                            },
                            color = selectedColor
                        )
                    }
                    // Fallback: use icon-style text
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when (item.type) {
                                ShapeType.RECTANGLE -> "\u25A1"
                                ShapeType.ROUNDED_RECTANGLE -> "\u25AD"
                                ShapeType.CIRCLE -> "\u25CB"
                                ShapeType.OVAL -> "\u25E5"
                                ShapeType.TRIANGLE -> "\u25B3"
                                ShapeType.DIAMOND -> "\u25C7"
                                ShapeType.STAR -> "\u2606"
                                ShapeType.HEART -> "\u2661"
                                ShapeType.CROSS -> "\u271A"
                                ShapeType.ARROW_RIGHT -> "\u25B6"
                                ShapeType.ARROW_LEFT -> "\u25C0"
                                ShapeType.HEXAGON -> "\u2B21"
                                ShapeType.OCTAGON -> "\u2B22"
                                ShapeType.ARROW_UP -> "\u25B2"
                                ShapeType.ARROW_DOWN -> "\u25BC"
                                ShapeType.OCTAGON_STAR -> "\u2736"
                            },
                            fontSize = 24.sp,
                            color = selectedColor
                        )
                    }
                }
            }
        }

        // Color palette
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shapeColors) { c ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (selectedColor == c) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { selectedColor = c }
                )
            }
        }

        // Stroke width
        Text("Stroke: ${"%.0f".format(strokeWidth)}px", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = strokeWidth,
            onValueChange = { strokeWidth = it },
            valueRange = 0f..20f,
            modifier = Modifier.fillMaxWidth()
        )

        // Corner radius
        Text("Corner: ${"%.0f".format(cornerRadius)}px", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = cornerRadius,
            onValueChange = { cornerRadius = it },
            valueRange = 0f..50f,
            modifier = Modifier.fillMaxWidth()
        )

        // Shadow toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = hasShadow, onCheckedChange = { hasShadow = it })
            Text("Shadow", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
