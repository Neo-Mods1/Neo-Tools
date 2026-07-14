package com.neomods.tools.imageeditor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BgEraserTools(
    onRemoveBackground: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tolerance by remember { mutableFloatStateOf(30f) }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Background Removal", style = MaterialTheme.typography.titleSmall)
        Text(
            "Detects the dominant edge color and removes it. Higher tolerance removes more.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Tolerance: ${"%.0f".format(tolerance)}")
        Slider(
            value = tolerance,
            onValueChange = { tolerance = it },
            valueRange = 5f..100f,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onRemoveBackground(tolerance.toInt()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Remove Background")
        }
    }
}
