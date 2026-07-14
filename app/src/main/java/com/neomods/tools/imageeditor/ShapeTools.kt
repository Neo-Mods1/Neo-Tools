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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ShapeItem(val type: ShapeType, val symbol: String, val label: String)

private val shapeItems = listOf(
    ShapeItem(ShapeType.RECTANGLE, "\u25A1", "Rect"),
    ShapeItem(ShapeType.ROUNDED_RECTANGLE, "\u25AD", "Round"),
    ShapeItem(ShapeType.CIRCLE, "\u25CB", "Circle"),
    ShapeItem(ShapeType.OVAL, "\u25E5", "Oval"),
    ShapeItem(ShapeType.TRIANGLE, "\u25B3", "Triangle"),
    ShapeItem(ShapeType.DIAMOND, "\u25C7", "Diamond"),
    ShapeItem(ShapeType.STAR, "\u2606", "Star"),
    ShapeItem(ShapeType.HEART, "\u2661", "Heart"),
    ShapeItem(ShapeType.CROSS, "\u271A", "Cross"),
    ShapeItem(ShapeType.ARROW_RIGHT, "\u25B6", "Arrow R"),
    ShapeItem(ShapeType.ARROW_LEFT, "\u25C0", "Arrow L"),
    ShapeItem(ShapeType.ARROW_UP, "\u25B2", "Arrow U"),
    ShapeItem(ShapeType.ARROW_DOWN, "\u25BC", "Arrow D"),
    ShapeItem(ShapeType.HEXAGON, "\u2B21", "Hexagon"),
    ShapeItem(ShapeType.OCTAGON, "\u2B22", "Octagon"),
    ShapeItem(ShapeType.OCTAGON_STAR, "\u2736", "8-Star"),
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
                Surface(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable {
                            onAddShape(item.type, selectedColor, strokeWidth, cornerRadius, hasShadow)
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.symbol,
                            fontSize = 28.sp,
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
