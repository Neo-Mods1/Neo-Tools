package com.neomods.tools.imageeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val brushColors = listOf(
    Color.White, Color.Black, Color.Red, Color.Green, Color.Blue,
    Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFFFF6B35),
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6),
    Color(0xFFF97316), Color(0xFF6366F1), Color(0xFF84CC16),
)

@Composable
fun DrawTools(
    brushType: Int,
    color: Int,
    strokeWidth: Float,
    opacity: Float,
    onBrushTypeChanged: (Int) -> Unit,
    onColorChanged: (Int) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Draw", style = MaterialTheme.typography.titleSmall)

        // Brush type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Pencil", "Marker", "Airbrush", "Eraser").forEachIndexed { index, name ->
                FilterChip(
                    selected = brushType == index,
                    onClick = { onBrushTypeChanged(index) },
                    label = { Text(name) }
                )
            }
        }

        // Color palette
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(brushColors) { c ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (color == c.hashCode()) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorChanged(c.hashCode()) }
                )
            }
        }

        // Stroke width
        Text("Width: ${"%.0f".format(strokeWidth)}px")
        Slider(
            value = strokeWidth,
            onValueChange = { onStrokeWidthChanged(it) },
            valueRange = 1f..50f,
            modifier = Modifier.fillMaxWidth()
        )

        // Opacity
        Text("Opacity: ${"%.0f".format(opacity * 100)}%")
        Slider(
            value = opacity,
            onValueChange = { onOpacityChanged(it) },
            valueRange = 0.1f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
