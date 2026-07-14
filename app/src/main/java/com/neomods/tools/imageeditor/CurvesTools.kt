package com.neomods.tools.imageeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun CurvesTools(
    lut: IntArray,
    onLutChanged: (IntArray) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragPoint by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Curves",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = (change.position.x / size.width * 255).toInt().coerceIn(0, 255)
                        val y = (1f - change.position.y / size.height) * 255
                        val newLut = lut.copyOf()
                        newLut[x] = y.toInt().coerceIn(0, 255)
                        // Smooth the curve: interpolate between control points
                        onLutChanged(smoothLut(newLut))
                    }
                }
        ) {
            // Draw grid
            val gridColor = Color.Gray.copy(alpha = 0.3f)
            for (i in 0..4) {
                val pos = size.width * i / 4
                drawLine(gridColor, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, pos), Offset(size.width, pos), strokeWidth = 1f)
            }

            // Draw diagonal reference line
            drawLine(
                Color.Gray.copy(alpha = 0.5f),
                Offset(0f, size.height),
                Offset(size.width, 0f),
                strokeWidth = 1f
            )

            // Draw curve from LUT
            val path = Path()
            for (i in 0..255) {
                val x = i * size.width / 255f
                val y = (1f - lut[i] / 255f) * size.height
                if (i == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }
            drawPath(path, Color.White, style = Stroke(width = 2.5f))

            // Draw control points
            val accentColor = Color(0xFF6750A4)
            for (i in lut.indices step 32) {
                val x = i * size.width / 255f
                val y = (1f - lut[i] / 255f) * size.height
                drawCircle(accentColor, radius = 5f, center = Offset(x, y))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Shadows",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Highlights",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Curves")
        }
    }
}

private fun smoothLut(lut: IntArray): IntArray {
    val result = lut.copyOf()
    for (i in 1 until result.size - 1) {
        result[i] = (lut[i - 1] + lut[i] + lut[i + 1]) / 3
    }
    return result
}
