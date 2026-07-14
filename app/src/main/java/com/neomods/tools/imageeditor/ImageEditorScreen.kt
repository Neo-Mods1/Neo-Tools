package com.neomods.tools.imageeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    onBack: () -> Unit,
    viewModel: ImageEditorViewModel = viewModel()
) {
    val state = viewModel.state
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            viewModel.loadImage(it)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportImage(saveUri = it) }
    }

    LaunchedEffect(Unit) {
        pickerLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = state.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = state.canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = { viewModel.showExportDialog() }) {
                        Icon(Icons.Default.SaveAlt, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.layers.isEmpty() && imageUri == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Select an image to start editing",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = { pickerLauncher.launch("image/*") }) {
                        Text("Pick Image")
                    }
                }
            } else {
                EditorCanvas(
                    layers = state.layers,
                    activeTool = state.activeTool,
                    isPickingColor = state.isPickingColor,
                    pickedColor = state.pickedColor,
                    onTap = { x, y ->
                        when (state.activeTool) {
                            EditorTool.EYEDROPPER -> viewModel.pickColor(x, y)
                            EditorTool.CLONE_STAMP -> {
                                if (state.isCloning) {
                                    viewModel.applyCloneStamp(x, y, 64, 64)
                                } else {
                                    viewModel.startCloneStamp(x, y)
                                }
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Tool panel (slides up from bottom, above toolbar)
            AnimatedVisibility(
                visible = state.activeTool != EditorTool.SELECT && state.activeTool != EditorTool.LAYERS,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                when (state.activeTool) {
                    EditorTool.ADJUST -> AdjustTools(
                        activeType = state.activeAdjustType,
                        onTypeSelected = { viewModel.selectAdjustType(it) },
                        onValueChanged = { type, value -> viewModel.applyAdjustment(type, value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.CROP -> CropTools(
                        onRotate = { viewModel.rotateBitmap(it) },
                        onFlip = { viewModel.flipBitmap(it) },
                        onCrop = { x, y, w, h -> viewModel.cropBitmap(x, y, w, h) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.DRAW -> DrawTools(
                        brushType = viewModel.currentBrushType,
                        color = viewModel.currentDrawColor,
                        strokeWidth = viewModel.currentStrokeWidth,
                        opacity = viewModel.currentOpacity,
                        onBrushTypeChanged = { viewModel.setBrushType(it) },
                        onColorChanged = { viewModel.setDrawColor(it) },
                        onStrokeWidthChanged = { viewModel.setStrokeWidth(it) },
                        onOpacityChanged = { viewModel.setOpacity(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.TEXT -> TextTools(
                        onAddText = { text, color -> viewModel.addTextLayer(text, color) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.STICKER -> StickerTools(
                        onStickerSelected = { viewModel.addSticker(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.SHAPE -> ShapeTools(
                        onAddShape = { type, color, stroke, corner, shadow ->
                            viewModel.addShape(type, color, stroke, corner, shadow)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.BG_ERASER -> BgEraserTools(
                        onRemoveBackground = { tolerance -> viewModel.removeBackground(tolerance) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.FILTERS -> FilterTools(
                        activeFilter = state.activeFilterType,
                        onFilterSelected = {
                            viewModel.selectFilterType(it)
                            viewModel.applyFilter(it)
                        },
                        filterIntensity = 0.5f,
                        onIntensityChanged = { viewModel.applyFilter(state.activeFilterType, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    EditorTool.CURVES -> CurvesTools(
                        lut = state.curvesLut,
                        onLutChanged = { viewModel.applyCurvesLut(it) },
                        onReset = { viewModel.resetCurves() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    else -> {}
                }
            }

            // Layer panel overlay
            AnimatedVisibility(
                visible = state.showLayerPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LayerPanel(
                    layers = state.layers,
                    selectedLayerId = state.selectedLayerId,
                    onLayerSelected = { viewModel.selectLayer(it) },
                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                    onToggleLock = { viewModel.toggleLayerLock(it) },
                    onDelete = { viewModel.deleteLayer(it) },
                    onDuplicate = { viewModel.duplicateLayer(it) },
                    onDismiss = { viewModel.toggleLayerPanel() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }

            // Export dialog
            if (state.showExportDialog) {
                ExportDialog(
                    onExport = { format, quality ->
                        saveLauncher.launch("image.${format.extension}")
                    },
                    onDismiss = { viewModel.hideExportDialog() }
                )
            }

            if (state.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Horizontal scrollable toolbar at the very bottom
            EditorToolbar(
                activeTool = state.activeTool,
                onToolSelected = { viewModel.selectTool(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    activeTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    data class ToolItem(val tool: EditorTool, val icon: ImageVector, val label: String)

    val tools = listOf(
        ToolItem(EditorTool.SELECT, Icons.Default.TouchApp, "Select"),
        ToolItem(EditorTool.ADJUST, Icons.Default.Tune, "Adjust"),
        ToolItem(EditorTool.FILTERS, Icons.Default.AutoFixHigh, "Filters"),
        ToolItem(EditorTool.CURVES, Icons.AutoMirrored.Filled.ShowChart, "Curves"),
        ToolItem(EditorTool.CROP, Icons.Default.Crop, "Crop"),
        ToolItem(EditorTool.DRAW, Icons.Default.Brush, "Draw"),
        ToolItem(EditorTool.TEXT, Icons.Default.TextFields, "Text"),
        ToolItem(EditorTool.STICKER, Icons.Default.EmojiEmotions, "Sticker"),
        ToolItem(EditorTool.SHAPE, Icons.Default.Category, "Shape"),
        ToolItem(EditorTool.EYEDROPPER, Icons.Default.Colorize, "Picker"),
        ToolItem(EditorTool.CLONE_STAMP, Icons.Default.ContentCopy, "Clone"),
        ToolItem(EditorTool.BG_ERASER, Icons.Default.ContentCut, "BG Eraser"),
        ToolItem(EditorTool.LAYERS, Icons.Default.Layers, "Layers"),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tools.forEach { item ->
                val isActive = activeTool == item.tool
                val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onToolSelected(item.tool) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        fontSize = 10.sp,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
