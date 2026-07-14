package com.neomods.tools.imageeditor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import kotlin.math.roundToInt

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
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = state.canRedo) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
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
        bottomBar = {
            EditorBottomBar(
                activeTool = state.activeTool,
                onToolSelected = { viewModel.selectTool(it) }
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
                // Empty state - prompt to pick image
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
                // Editor canvas
                EditorCanvas(
                    layers = state.layers,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Tool panel (slides up from bottom)
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
                    EditorTool.BG_ERASER -> BgEraserTools(
                        onRemoveBackground = { tolerance -> viewModel.removeBackground(tolerance) },
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

            // Loading indicator
            if (state.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    activeTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit
) {
    val tools = listOf(
        EditorTool.SELECT to "Select",
        EditorTool.ADJUST to "Adjust",
        EditorTool.CROP to "Crop",
        EditorTool.DRAW to "Draw",
        EditorTool.TEXT to "Text",
        EditorTool.STICKER to "Sticker",
        EditorTool.BG_ERASER to "BG Eraser",
        EditorTool.LAYERS to "Layers",
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        tools.forEach { (tool, label) ->
            NavigationBarItem(
                selected = activeTool == tool,
                onClick = { onToolSelected(tool) },
                icon = {
                    Icon(
                        when (tool) {
                            EditorTool.SELECT -> Icons.Default.TouchApp
                            EditorTool.ADJUST -> Icons.Default.Tune
                            EditorTool.CROP -> Icons.Default.Crop
                            EditorTool.DRAW -> Icons.Default.Brush
                            EditorTool.TEXT -> Icons.Default.TextFields
                            EditorTool.STICKER -> Icons.Default.EmojiEmotions
                            EditorTool.BG_ERASER -> Icons.Default.ContentCut
                            EditorTool.LAYERS -> Icons.Default.Layers
                        },
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = { Text(label, fontSize = 10.sp) }
            )
        }
    }
}
