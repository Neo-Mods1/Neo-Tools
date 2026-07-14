package com.neomods.tools.imageeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterTools(
    activeFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    filterIntensity: Float,
    onIntensityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            "Filters",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterType.entries.forEach { filter ->
                val isActive = activeFilter == filter
                val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activeFilter == FilterType.THRESHOLD ||
            activeFilter == FilterType.BLUR ||
            activeFilter == FilterType.SEPIA ||
            activeFilter == FilterType.EMBOSS
        ) {
            Text(
                "Intensity: %.2f".format(filterIntensity),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = filterIntensity,
                onValueChange = onIntensityChanged,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
