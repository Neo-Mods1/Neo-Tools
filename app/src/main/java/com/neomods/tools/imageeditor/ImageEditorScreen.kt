package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    onBack: () -> Unit,
    imageUri: Uri? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Current bitmap state — can be replaced by image picker
    var currentBitmap by remember {
        mutableStateOf(
            imageUri?.let {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        )
    }

    // Track original bitmap for reset
    var originalBitmap by remember { mutableStateOf(currentBitmap) }

    var photoEditor by remember { mutableStateOf<PhotoEditor?>(null) }
    var showBrushPanel by remember { mutableStateOf(false) }
    var showTextPanel by remember { mutableStateOf(false) }
    var brushColor by remember { mutableIntStateOf(android.graphics.Color.BLACK) }
    var brushSize by remember { mutableFloatStateOf(8f) }
    var brushOpacity by remember { mutableFloatStateOf(100f) }
    var isEraserMode by remember { mutableStateOf(false) }

    // Image picker — system photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { picked ->
                if (picked.width > 0 && picked.height > 0) {
                    // Clear existing editor state and replace source
                    photoEditor?.clearAllViews()
                    currentBitmap = picked
                    originalBitmap = picked
                }
            }
        }
    }

    // Sticker picker — pick any image to add as overlay
    val stickerPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { sticker ->
                photoEditor?.addImage(sticker)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ────────────────────────────────────────────────────
        TopAppBar(
            title = { Text("Image Editor", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Pick image
                FilledTonalIconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Pick Image")
                }
                Spacer(Modifier.width(4.dp))

                // Undo
                FilledTonalIconButton(
                    onClick = { photoEditor?.undo() },
                    enabled = photoEditor?.isUndoAvailable == true,
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                Spacer(Modifier.width(4.dp))

                // Redo
                FilledTonalIconButton(
                    onClick = { photoEditor?.redo() },
                    enabled = photoEditor?.isRedoAvailable == true,
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
                Spacer(Modifier.width(4.dp))

                // Reset
                FilledTonalIconButton(
                    onClick = {
                        photoEditor?.clearAllViews()
                        currentBitmap = originalBitmap
                    },
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "Reset")
                }
                Spacer(Modifier.width(4.dp))

                // Save
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            photoEditor?.let { editor ->
                                val saveSettings = ja.burhanrashid52.photoeditor.SaveSettings.Builder()
                                    .setTransparencyEnabled(false)
                                    .setClearViewsEnabled(false)
                                    .build()
                                val result = editor.saveAsBitmap(saveSettings)
                                withContext(Dispatchers.IO) {
                                    val file = File(context.cacheDir, "edited_${System.currentTimeMillis()}.png")
                                    java.io.FileOutputStream(file).use { out ->
                                        result.compress(
                                            android.graphics.Bitmap.CompressFormat.PNG,
                                            100, out
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Saved to cache", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
                Spacer(Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        // ── Editor Canvas ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            PhotoEditorHost(
                bitmap = currentBitmap,
                modifier = Modifier.fillMaxSize(),
                onReady = { photoEditor = it },
            )
        }

        // ── Bottom Tool Bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Draw tool
            ToolChip(
                icon = Icons.Default.Brush,
                label = if (isEraserMode) "Eraser" else "Draw",
                selected = showBrushPanel,
                onClick = {
                    showBrushPanel = !showBrushPanel
                    showTextPanel = false
                    isEraserMode = false
                    if (showBrushPanel) {
                        photoEditor?.setBrushDrawingMode(true)
                        photoEditor?.setShape(
                            ShapeBuilder()
                                .withShapeType(ShapeType.Brush)
                                .withShapeSize(brushSize)
                                .withShapeColor(brushColor)
                                .withShapeOpacity(brushOpacity.toInt())
                        )
                    } else {
                        photoEditor?.setBrushDrawingMode(false)
                    }
                },
            )

            // Eraser tool
            ToolChip(
                icon = Icons.Default.Close,
                label = "Eraser",
                selected = showBrushPanel && isEraserMode,
                onClick = {
                    showBrushPanel = true
                    showTextPanel = false
                    isEraserMode = true
                    photoEditor?.setBrushDrawingMode(true)
                    photoEditor?.brushEraser()
                },
            )

            // Text tool
            ToolChip(
                icon = Icons.Default.TextFields,
                label = "Text",
                selected = showTextPanel,
                onClick = {
                    showTextPanel = !showTextPanel
                    showBrushPanel = false
                    isEraserMode = false
                    photoEditor?.setBrushDrawingMode(false)
                },
            )

            // Sticker tool
            ToolChip(
                icon = Icons.Default.Image,
                label = "Sticker",
                selected = false,
                onClick = {
                    photoEditor?.setBrushDrawingMode(false)
                    isEraserMode = false
                    showBrushPanel = false
                    showTextPanel = false
                    stickerPickerLauncher.launch("image/*")
                },
            )
        }

        // ── Brush Panel ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showBrushPanel,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            BrushPanel(
                isEraser = isEraserMode,
                brushSize = brushSize,
                onBrushSizeChange = { brushSize = it },
                brushOpacity = brushOpacity,
                onBrushOpacityChange = { brushOpacity = it },
                brushColor = brushColor,
                onBrushColorChange = { brushColor = it },
                onApply = {
                    if (isEraserMode) {
                        photoEditor?.setBrushEraserSize(brushSize)
                    } else {
                        photoEditor?.setShape(
                            ShapeBuilder()
                                .withShapeType(ShapeType.Brush)
                                .withShapeSize(brushSize)
                                .withShapeColor(brushColor)
                                .withShapeOpacity(brushOpacity.toInt())
                        )
                    }
                },
                onToggleEraser = {
                    isEraserMode = !isEraserMode
                    if (isEraserMode) {
                        photoEditor?.brushEraser()
                    } else {
                        photoEditor?.setShape(
                            ShapeBuilder()
                                .withShapeType(ShapeType.Brush)
                                .withShapeSize(brushSize)
                                .withShapeColor(brushColor)
                                .withShapeOpacity(brushOpacity.toInt())
                        )
                    }
                },
                onClose = {
                    showBrushPanel = false
                    isEraserMode = false
                    photoEditor?.setBrushDrawingMode(false)
                },
            )
        }

        // ── Text Panel ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showTextPanel,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            TextPanel(
                onAddText = { text, color ->
                    photoEditor?.addText(text, color)
                    showTextPanel = false
                },
                onClose = { showTextPanel = false },
            )
        }
    }
}

// ── Reusable Components ─────────────────────────────────────────────────

@Composable
private fun ToolChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Brush Panel ─────────────────────────────────────────────────────────

private val brushColors = listOf(
    0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFF44336.toInt(),
    0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFF673AB7.toInt(),
    0xFF3F51B5.toInt(), 0xFF2196F3.toInt(), 0xFF03A9F4.toInt(),
    0xFF00BCD4.toInt(), 0xFF009688.toInt(), 0xFF4CAF50.toInt(),
    0xFF8BC34A.toInt(), 0xFFCDDC39.toInt(), 0xFFFFC107.toInt(),
    0xFFFF9800.toInt(), 0xFFFF5722.toInt(), 0xFF795548.toInt(),
    0xFF9E9E9E.toInt(), 0xFF607D8B.toInt(),
)

@Composable
private fun BrushPanel(
    isEraser: Boolean,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    brushOpacity: Float,
    onBrushOpacityChange: (Float) -> Unit,
    brushColor: Int,
    onBrushColorChange: (Int) -> Unit,
    onApply: () -> Unit,
    onToggleEraser: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (isEraser) "Eraser Settings" else "Brush Settings",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Row {
                FilledTonalIconButton(
                    onClick = onToggleEraser,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Toggle eraser",
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Size slider
        Text("Size: ${brushSize.toInt()}dp", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = brushSize,
            onValueChange = { onBrushSizeChange(it); onApply() },
            valueRange = 1f..60f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if (!isEraser) {
            Spacer(Modifier.height(8.dp))

            // Opacity slider
            Text("Opacity: ${brushOpacity.toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = brushOpacity,
                onValueChange = { onBrushOpacityChange(it); onApply() },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Color grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier.height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(brushColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .clickable {
                                onBrushColorChange(color)
                                onApply()
                            },
                    )
                }
            }
        }
    }
}

// ── Text Panel ──────────────────────────────────────────────────────────

@Composable
private fun TextPanel(
    onAddText: (String, Int) -> Unit,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var textColor by remember { mutableIntStateOf(android.graphics.Color.BLACK) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Add Text", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Enter text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        // Color picker row
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            brushColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .clickable { textColor = color },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = { if (text.isNotBlank()) onAddText(text, textColor) },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank(),
        ) {
            Text("Add to Image")
        }
    }
}
