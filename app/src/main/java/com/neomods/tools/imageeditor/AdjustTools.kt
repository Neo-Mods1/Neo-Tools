package com.neomods.tools.imageeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdjustTools(
    activeType: AdjustType,
    onTypeSelected: (AdjustType) -> Unit,
    onValueChanged: (AdjustType, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentValue by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Adjustment type selector (horizontal chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdjustType.entries.take(5).forEach { type ->
                FilterChip(
                    selected = activeType == type,
                    onClick = {
                        onTypeSelected(type)
                        currentValue = 0f
                    },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdjustType.entries.drop(5).forEach { type ->
                FilterChip(
                    selected = activeType == type,
                    onClick = {
                        onTypeSelected(type)
                        currentValue = 0f
                    },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                )
            }
        }

        // Slider for current adjustment
        val range = when (activeType) {
            AdjustType.BRIGHTNESS -> -1f..1f
            AdjustType.CONTRAST -> 0f..2f
            AdjustType.SATURATION -> 0f..2f
            AdjustType.EXPOSURE -> -1f..1f
            AdjustType.WARMTH -> -1f..1f
            AdjustType.HIGHLIGHTS -> -1f..1f
            AdjustType.SHADOWS -> -1f..1f
            AdjustType.SHARPNESS -> 0f..1f
            AdjustType.VIGNETTE -> 0f..1f
            AdjustType.HUE -> 0f..1f
        }

        val defaultValue = when (activeType) {
            AdjustType.CONTRAST, AdjustType.SATURATION -> 1f
            else -> 0f
        }

        Text(
            text = "${activeType.name.lowercase().replaceFirstChar { it.uppercase() }}: ${"%.2f".format(currentValue)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = currentValue,
            onValueChange = {
                currentValue = it
                onValueChanged(activeType, it)
            },
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(
            onClick = {
                currentValue = defaultValue
                onValueChanged(activeType, defaultValue)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Reset")
        }
    }
}
