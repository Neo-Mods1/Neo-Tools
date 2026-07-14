package com.neomods.tools.imageeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun LayerPanel(
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    onLayerSelected: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .heightIn(max = 350.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layers", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(layers.reversed()) { layer ->
                val isSelected = layer.id == selectedLayerId

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLayerSelected(layer.id) },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Layer type icon
                        Icon(
                            when (layer) {
                                is EditorLayer.Image -> Icons.Default.Image
                                is EditorLayer.Text -> Icons.Default.TextFields
                                is EditorLayer.Sticker -> Icons.Default.EmojiEmotions
                                is EditorLayer.Drawing -> Icons.Default.Brush
                                is EditorLayer.Shape -> Icons.Default.Category
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.width(8.dp))

                        // Layer name
                        Text(
                            text = when (layer) {
                                is EditorLayer.Image -> "Image"
                                is EditorLayer.Text -> layer.text.take(20)
                                is EditorLayer.Sticker -> "Sticker"
                                is EditorLayer.Drawing -> "Drawing"
                                is EditorLayer.Shape -> layer.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Visibility toggle
                        IconButton(
                            onClick = { onToggleVisibility(layer.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Lock toggle
                        IconButton(
                            onClick = { onToggleLock(layer.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Toggle lock",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // More options
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = { onDuplicate(layer.id); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = { onDelete(layer.id); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
