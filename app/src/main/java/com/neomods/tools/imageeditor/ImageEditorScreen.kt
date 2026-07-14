package com.neomods.tools.imageeditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.SaveSettings
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

// ── Aspect ratio presets ────────────────────────────────────────────────

private enum class AspectPreset(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT("9:16", 9f / 16f),
    LANDSCAPE("16:9", 16f / 9f),
    CLASSIC("4:3", 4f / 3f),
    PHOTO("3:2", 3f / 2f),
    CIRCLE("Circle", 1f),
}

private val bgColors = listOf(
    android.graphics.Color.TRANSPARENT,
    android.graphics.Color.WHITE,
    android.graphics.Color.BLACK,
    0xFF4CAF50.toInt(),
    0xFF2196F3.toInt(),
    0xFFF44336.toInt(),
    0xFFFFC107.toInt(),
    0xFF9C27B0.toInt(),
    0xFFFF9800.toInt(),
    0xFF00BCD4.toInt(),
    0xFF795548.toInt(),
    0xFF607D8B.toInt(),
    0xFFE91E63.toInt(),
    0xFF3F51B5.toInt(),
    0xFF009688.toInt(),
    0xFF8BC34A.toInt(),
)

private val brushColors = listOf(
    android.graphics.Color.BLACK,
    android.graphics.Color.WHITE,
    0xFFF44336.toInt(),
    0xFFE91E63.toInt(),
    0xFF9C27B0.toInt(),
    0xFF673AB7.toInt(),
    0xFF3F51B5.toInt(),
    0xFF2196F3.toInt(),
    0xFF03A9F4.toInt(),
    0xFF00BCD4.toInt(),
    0xFF009688.toInt(),
    0xFF4CAF50.toInt(),
    0xFF8BC34A.toInt(),
    0xFFFFC107.toInt(),
    0xFFFF9800.toInt(),
    0xFFFF5722.toInt(),
    0xFF795548.toInt(),
    0xFF9E9E9E.toInt(),
    0xFF607D8B.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    onBack: () -> Unit,
    imageUri: Uri? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Bitmap state with undo history ──────────────────────────────────
    var bitmapHistory by remember { mutableStateOf(listOf<Bitmap>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Decode initial bitmap off main thread to prevent screen-switch lag
    LaunchedEffect(imageUri) {
        isLoading = true
        val decoded = withContext(Dispatchers.IO) {
            imageUri?.let {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.WHITE)
            }
        }
        currentBitmap = decoded
        originalBitmap = decoded
        bitmapHistory = listOf(decoded)
        historyIndex = 0
        isLoading = false
    }

    fun pushHistory(bmp: Bitmap) {
        val trimmed = bitmapHistory.subList(0, historyIndex + 1)
        bitmapHistory = trimmed + bmp
        historyIndex = bitmapHistory.lastIndex
    }

    fun undoBitmap() {
        if (historyIndex > 0) {
            historyIndex--
            currentBitmap = bitmapHistory[historyIndex]
        }
    }

    fun redoBitmap() {
        if (historyIndex < bitmapHistory.lastIndex) {
            historyIndex++
            currentBitmap = bitmapHistory[historyIndex]
        }
    }

    val canUndo by remember { derivedStateOf { historyIndex > 0 } }
    val canRedo by remember { derivedStateOf { historyIndex < bitmapHistory.lastIndex } }

    // ── PhotoEditor reference ───────────────────────────────────────────
    var photoEditor by remember { mutableStateOf<PhotoEditor?>(null) }

    // ── Panel visibility ────────────────────────────────────────────────
    var showBrushPanel by remember { mutableStateOf(false) }
    var showTextPanel by remember { mutableStateOf(false) }
    var showCropPanel by remember { mutableStateOf(false) }
    var showAspectPanel by remember { mutableStateOf(false) }
    var showBgPanel by remember { mutableStateOf(false) }

    // ── Brush state ─────────────────────────────────────────────────────
    var brushColor by remember { mutableIntStateOf(android.graphics.Color.BLACK) }
    var brushSize by remember { mutableFloatStateOf(8f) }
    var brushOpacity by remember { mutableFloatStateOf(100f) }
    var isEraserMode by remember { mutableStateOf(false) }

    // ── Crop state ──────────────────────────────────────────────────────
    var cropLeft by remember { mutableFloatStateOf(0.1f) }
    var cropTop by remember { mutableFloatStateOf(0.1f) }
    var cropRight by remember { mutableFloatStateOf(0.9f) }
    var cropBottom by remember { mutableFloatStateOf(0.9f) }

    // ── Aspect ratio ────────────────────────────────────────────────────
    var selectedAspect by remember { mutableStateOf(AspectPreset.FREE) }

    // ── Background color ────────────────────────────────────────────────
    var bgColor by remember { mutableIntStateOf(android.graphics.Color.TRANSPARENT) }

    // ── Confirmation dialog state ───────────────────────────────────────
    var pendingConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // ── Color picker dialog state ───────────────────────────────────────
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf("") }
    var colorPickerCurrent by remember { mutableIntStateOf(android.graphics.Color.BLACK) }

    fun closeAllPanels() {
        showBrushPanel = false
        showTextPanel = false
        showCropPanel = false
        showAspectPanel = false
        showBgPanel = false
        isEraserMode = false
        photoEditor?.setBrushDrawingMode(false)
    }

    // ── Image picker (off-thread decode) ────────────────────────────────
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val picked = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                if (picked != null && picked.width > 0 && picked.height > 0) {
                    photoEditor?.clearAllViews()
                    currentBitmap = picked
                    originalBitmap = picked
                    bitmapHistory = listOf(picked)
                    historyIndex = 0
                }
            }
        }
    }

    // ── Sticker picker ──────────────────────────────────────────────────
    val stickerPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }?.let { sticker ->
                    photoEditor?.addImage(sticker)
                }
            }
        }
    }

    // ── Bg image picker ─────────────────────────────────────────────────
    val bgImagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val bgImg = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                if (bgImg != null && currentBitmap != null) {
                    val bmp = currentBitmap!!
                    withContext(Dispatchers.IO) {
                        val scaled = Bitmap.createScaledBitmap(bgImg, bmp.width, bmp.height, true)
                        val merged = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(merged)
                        canvas.drawBitmap(scaled, 0f, 0f, null)
                        canvas.drawBitmap(bmp, 0f, 0f, null)
                        merged
                    }.also { merged ->
                        photoEditor?.clearAllViews()
                        currentBitmap = merged
                        pushHistory(merged)
                    }
                }
            }
        }
    }

    // ── UI ──────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ────────────────────────────────────────────────────
        TopAppBar(
            title = {
                val bmp = currentBitmap
                if (bmp != null) {
                    Text(
                        "${bmp.width} × ${bmp.height}",
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                FilledTonalIconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = "Pick Image")
                }
                Spacer(Modifier.width(2.dp))
                FilledTonalIconButton(
                    onClick = { undoBitmap() },
                    enabled = canUndo,
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                Spacer(Modifier.width(2.dp))
                FilledTonalIconButton(
                    onClick = { redoBitmap() },
                    enabled = canRedo,
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
                Spacer(Modifier.width(2.dp))
                FilledTonalIconButton(
                    onClick = {
                        if (!canUndo) return@FilledTonalIconButton
                        pendingConfirmAction = {
                            photoEditor?.clearAllViews()
                            currentBitmap = originalBitmap
                            bitmapHistory = listOf(originalBitmap!!)
                            historyIndex = 0
                        }
                        showConfirmDialog = true
                    },
                    enabled = canUndo,
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "Reset")
                }
                Spacer(Modifier.width(2.dp))
                FilledTonalButton(
                    onClick = {
                        val bmp = currentBitmap ?: return@FilledTonalButton
                        scope.launch {
                            val editor = photoEditor ?: return@launch
                            val settings = SaveSettings.Builder()
                                .setTransparencyEnabled(bgColor == android.graphics.Color.TRANSPARENT)
                                .setClearViewsEnabled(false)
                                .build()
                            val result = editor.saveAsBitmap(settings)
                            withContext(Dispatchers.IO) {
                                val file = File(context.cacheDir, "edited_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    result.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Saved to cache", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        // ── Editor Canvas ──────────────────────────────────────────────
        val bmp = currentBitmap
        if (isLoading || bmp == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                PhotoEditorHost(
                    bitmap = bmp,
                    modifier = Modifier.fillMaxSize(),
                    onReady = { photoEditor = it },
                )

                if (showCropPanel) {
                    CropOverlay(
                        cropLeft = cropLeft,
                        cropTop = cropTop,
                        cropRight = cropRight,
                        cropBottom = cropBottom,
                        onCropChange = { l, t, r, b ->
                            cropLeft = l; cropTop = t; cropRight = r; cropBottom = b
                        },
                    )
                }
            }
        }

        // ── Bottom Tool Bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToolChip(Icons.Default.Brush, "Draw", showBrushPanel && !isEraserMode) {
                closeAllPanels(); showBrushPanel = true; isEraserMode = false
                photoEditor?.setBrushDrawingMode(true)
                photoEditor?.setShape(
                    ShapeBuilder().withShapeType(ShapeType.Brush)
                        .withShapeSize(brushSize).withShapeColor(brushColor)
                        .withShapeOpacity(brushOpacity.toInt())
                )
            }
            ToolChip(Icons.Default.FormatPaint, "Eraser", showBrushPanel && isEraserMode) {
                closeAllPanels(); showBrushPanel = true; isEraserMode = true
                photoEditor?.setBrushDrawingMode(true)
                photoEditor?.brushEraser()
            }
            ToolChip(Icons.Default.TextFields, "Text", showTextPanel) {
                closeAllPanels(); showTextPanel = true
            }
            ToolChip(Icons.Default.Image, "Sticker", false) {
                closeAllPanels()
                stickerPickerLauncher.launch("image/*")
            }
            ToolChip(Icons.Default.Crop, "Crop", showCropPanel) {
                closeAllPanels(); showCropPanel = true
            }
            ToolChip(Icons.Default.PhotoSizeSelectLarge, "Aspect", showAspectPanel) {
                closeAllPanels(); showAspectPanel = true
            }
            ToolChip(Icons.Default.FormatColorFill, "Background", showBgPanel) {
                closeAllPanels(); showBgPanel = true
            }
        }

        // ── Brush Panel ────────────────────────────────────────────────
        AnimatedVisibility(visible = showBrushPanel, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            BrushPanel(
                isEraser = isEraserMode, brushSize = brushSize,
                onBrushSizeChange = { brushSize = it },
                brushOpacity = brushOpacity, onBrushOpacityChange = { brushOpacity = it },
                brushColor = brushColor, onBrushColorChange = { brushColor = it },
                onApply = {
                    if (isEraserMode) photoEditor?.setBrushEraserSize(brushSize)
                    else photoEditor?.setShape(
                        ShapeBuilder().withShapeType(ShapeType.Brush)
                            .withShapeSize(brushSize).withShapeColor(brushColor)
                            .withShapeOpacity(brushOpacity.toInt())
                    )
                },
                onToggleEraser = {
                    isEraserMode = !isEraserMode
                    if (isEraserMode) photoEditor?.brushEraser()
                    else photoEditor?.setShape(
                        ShapeBuilder().withShapeType(ShapeType.Brush)
                            .withShapeSize(brushSize).withShapeColor(brushColor)
                            .withShapeOpacity(brushOpacity.toInt())
                    )
                },
                onPickColor = {
                    colorPickerTarget = "brush"
                    colorPickerCurrent = brushColor
                    showColorPicker = true
                },
                onClose = { closeAllPanels() },
            )
        }

        // ── Text Panel ─────────────────────────────────────────────────
        AnimatedVisibility(visible = showTextPanel, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            TextPanel(
                onAddText = { text, color -> photoEditor?.addText(text, color); showTextPanel = false },
                onPickColor = {
                    colorPickerTarget = "text"
                    colorPickerCurrent = android.graphics.Color.BLACK
                    showColorPicker = true
                },
                onClose = { showTextPanel = false },
            )
        }

        // ── Crop Panel ─────────────────────────────────────────────────
        AnimatedVisibility(visible = showCropPanel, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            val bmpSnap = currentBitmap
            if (bmpSnap != null) {
                CropPanel(
                    cropLeft = cropLeft, cropTop = cropTop,
                    cropRight = cropRight, cropBottom = cropBottom,
                    imgWidth = bmpSnap.width, imgHeight = bmpSnap.height,
                    onApplyCrop = {
                        val w = bmpSnap.width
                        val h = bmpSnap.height
                        val x = (cropLeft * w).roundToInt().coerceIn(0, w - 1)
                        val y = (cropTop * h).roundToInt().coerceIn(0, h - 1)
                        val cw = ((cropRight - cropLeft) * w).roundToInt().coerceIn(1, w - x)
                        val ch = ((cropBottom - cropTop) * h).roundToInt().coerceIn(1, h - y)
                        if (cw > 1 && ch > 1) {
                            val cropped = Bitmap.createBitmap(bmpSnap, x, y, cw, ch)
                            photoEditor?.clearAllViews()
                            currentBitmap = cropped
                            pushHistory(cropped)
                            // Reset crop overlay
                            cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f
                        }
                        showCropPanel = false
                    },
                    onReset = {
                        cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f
                    },
                    onClose = { showCropPanel = false },
                )
            }
        }

        // ── Aspect Panel ───────────────────────────────────────────────
        AnimatedVisibility(visible = showAspectPanel, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            AspectPanel(
                selected = selectedAspect,
                onSelect = { preset ->
                    selectedAspect = preset
                    if (preset == AspectPreset.FREE) {
                        showAspectPanel = false
                        return@AspectPanel
                    }
                    val bmpSnap = currentBitmap ?: return@AspectPanel
                    val w = bmpSnap.width
                    val h = bmpSnap.height

                    val newBmp = if (preset == AspectPreset.CIRCLE) {
                        val side = minOf(w, h)
                        val x = ((w - side) / 2f).roundToInt()
                        val y = ((h - side) / 2f).roundToInt()
                        val cropped = Bitmap.createBitmap(bmpSnap, x, y, side, side)
                        val circle = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                        val c = Canvas(circle)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        c.drawCircle(side / 2f, side / 2f, side / 2f, paint)
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        c.drawBitmap(cropped, 0f, 0f, paint)
                        circle
                    } else {
                        preset.ratio?.let { ratio ->
                            val newW: Int
                            val newH: Int
                            if (w.toFloat() / h > ratio) {
                                newH = h
                                newW = (h * ratio).roundToInt()
                            } else {
                                newW = w
                                newH = (w / ratio).roundToInt()
                            }
                            val x = ((w - newW) / 2f).roundToInt()
                            val y = ((h - newH) / 2f).roundToInt()
                            Bitmap.createBitmap(bmpSnap, x, y, newW, newH)
                        }
                    }
                    if (newBmp != null) {
                        photoEditor?.clearAllViews()
                        currentBitmap = newBmp
                        pushHistory(newBmp)
                    }
                    showAspectPanel = false
                },
                onClose = { showAspectPanel = false },
            )
        }

        // ── Background Panel ───────────────────────────────────────────
        AnimatedVisibility(visible = showBgPanel, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            BgPanel(
                selectedColor = bgColor,
                onColorSelect = { color ->
                    bgColor = color
                    val bmpSnap = currentBitmap ?: return@BgPanel
                    val merged = Bitmap.createBitmap(
                        bmpSnap.width, bmpSnap.height, Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(merged)
                    canvas.drawColor(color)
                    canvas.drawBitmap(bmpSnap, 0f, 0f, null)
                    photoEditor?.clearAllViews()
                    currentBitmap = merged
                    pushHistory(merged)
                },
                onPickImage = { bgImagePickerLauncher.launch("image/*") },
                onClose = { showBgPanel = false },
            )
        }
    }

    // ── Confirmation Dialog ─────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Reset image?") },
            text = { Text("This will discard all edits and restore the original image.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirmAction?.invoke()
                    pendingConfirmAction = null
                    showConfirmDialog = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ── Color Picker Dialog ─────────────────────────────────────────────
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = colorPickerCurrent,
            onSelect = { color ->
                when (colorPickerTarget) {
                    "brush" -> {
                        brushColor = color
                        photoEditor?.setShape(
                            ShapeBuilder().withShapeType(ShapeType.Brush)
                                .withShapeSize(brushSize).withShapeColor(color)
                                .withShapeOpacity(brushOpacity.toInt())
                        )
                    }
                    "text" -> { /* textColor handled in TextPanel */ }
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }
}

// ── Color Picker Dialog ────────────────────────────────────────────────

@Composable
private fun ColorPickerDialog(
    currentColor: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedColor by remember { mutableIntStateOf(currentColor) }

    val swatches = remember {
        listOf(
            android.graphics.Color.BLACK,
            android.graphics.Color.WHITE,
            android.graphics.Color.RED,
            android.graphics.Color.GREEN,
            android.graphics.Color.BLUE,
            android.graphics.Color.YELLOW,
            android.graphics.Color.CYAN,
            android.graphics.Color.MAGENTA,
            0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(),
            0xFF673AB7.toInt(), 0xFF3F51B5.toInt(), 0xFF2196F3.toInt(),
            0xFF03A9F4.toInt(), 0xFF00BCD4.toInt(), 0xFF009688.toInt(),
            0xFF4CAF50.toInt(), 0xFF8BC34A.toInt(), 0xFFFFC107.toInt(),
            0xFFFF9800.toInt(), 0xFFFF5722.toInt(), 0xFF795548.toInt(),
            0xFF9E9E9E.toInt(), 0xFF607D8B.toInt(),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Color", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                // Current color preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ComposeColor(selectedColor))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(String.format("#%06X", 0xFFFFFF and selectedColor), fontSize = 14.sp)
                }
                // Color grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(swatches) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ComposeColor(color))
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selectedColor) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── Crop Overlay ────────────────────────────────────────────────────────

@Composable
private fun CropOverlay(
    cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float,
    onCropChange: (Float, Float, Float, Float) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val l = cropLeft * w
            val t = cropTop * h
            val r = cropRight * w
            val b = cropBottom * h

            drawRect(ComposeColor.Black.copy(alpha = 0.4f))
            drawRect(
                ComposeColor.Transparent,
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
            )
            drawRect(
                ComposeColor.White,
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
                style = Stroke(width = 2.dp.toPx()),
            )
            for (i in 1..2) {
                val xFrac = l + (r - l) * i / 3f
                val yFrac = t + (b - t) * i / 3f
                drawLine(ComposeColor.White.copy(alpha = 0.5f), Offset(xFrac, t), Offset(xFrac, b), strokeWidth = 1.dp.toPx())
                drawLine(ComposeColor.White.copy(alpha = 0.5f), Offset(l, yFrac), Offset(r, yFrac), strokeWidth = 1.dp.toPx())
            }
        }

        var activeHandle by remember { mutableStateOf("") }

        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / size.width
                    val dy = dragAmount.y / size.height
                    when (activeHandle) {
                        "tl" -> onCropChange(
                            (cropLeft + dx).coerceIn(0f, cropRight - 0.05f),
                            (cropTop + dy).coerceIn(0f, cropBottom - 0.05f),
                            cropRight, cropBottom
                        )
                        "tr" -> onCropChange(
                            cropLeft,
                            (cropTop + dy).coerceIn(0f, cropBottom - 0.05f),
                            (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f),
                            cropBottom
                        )
                        "bl" -> onCropChange(
                            (cropLeft + dx).coerceIn(0f, cropRight - 0.05f),
                            cropTop,
                            cropRight,
                            (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                        )
                        "br" -> onCropChange(
                            cropLeft, cropTop,
                            (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f),
                            (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                        )
                        "top" -> onCropChange(cropLeft, (cropTop + dy).coerceIn(0f, cropBottom - 0.05f), cropRight, cropBottom)
                        "bottom" -> onCropChange(cropLeft, cropTop, cropRight, (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f))
                        "left" -> onCropChange((cropLeft + dx).coerceIn(0f, cropRight - 0.05f), cropTop, cropRight, cropBottom)
                        "right" -> onCropChange(cropLeft, cropTop, (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f), cropBottom)
                    }
                }
            }
        ) {
            listOf(
                "tl" to Offset(cropLeft, cropTop),
                "tr" to Offset(cropRight, cropTop),
                "bl" to Offset(cropLeft, cropBottom),
                "br" to Offset(cropRight, cropBottom),
            ).forEach { (handle, pos) ->
                Box(modifier = Modifier
                    .graphicsLayer {
                        translationX = pos.x * size.width - 12.dp.toPx()
                        translationY = pos.y * size.height - 12.dp.toPx()
                    }
                    .size(24.dp)
                    .background(ComposeColor.White, CircleShape)
                    .border(2.dp, ComposeColor.Black, CircleShape)
                    .pointerInput(handle) {
                        detectDragGestures(
                            onDragStart = { activeHandle = handle },
                            onDragEnd = { activeHandle = "" },
                            onDragCancel = { activeHandle = "" },
                        ) { _, _ -> }
                    }
                )
            }
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

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(20.dp))
    }
}

// ── Brush Panel ─────────────────────────────────────────────────────────

@Composable
private fun BrushPanel(
    isEraser: Boolean, brushSize: Float, onBrushSizeChange: (Float) -> Unit,
    brushOpacity: Float, onBrushOpacityChange: (Float) -> Unit,
    brushColor: Int, onBrushColorChange: (Int) -> Unit,
    onApply: () -> Unit, onToggleEraser: () -> Unit,
    onPickColor: () -> Unit, onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (isEraser) "Eraser" else "Brush", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row {
                FilledTonalIconButton(onClick = onToggleEraser, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.FormatPaint, "Toggle eraser", modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Brush preview + size slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(brushSize.coerceIn(4f, 60f).dp)
                    .clip(CircleShape)
                    .background(if (isEraser) ComposeColor.White else ComposeColor(brushColor))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Size: ${brushSize.toInt()}px", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = brushSize,
                    onValueChange = { onBrushSizeChange(it); onApply() },
                    valueRange = 1f..60f,
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
                )
            }
        }

        if (!isEraser) {
            Spacer(Modifier.height(4.dp))
            Text("Opacity: ${brushOpacity.toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = brushOpacity,
                onValueChange = { onBrushOpacityChange(it); onApply() },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                brushColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ComposeColor(color))
                            .clickable { onBrushColorChange(color); onApply() },
                    )
                }
                // Custom color picker trigger
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable(onClick = onPickColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Text Panel ──────────────────────────────────────────────────────────

@Composable
private fun TextPanel(
    onAddText: (String, Int) -> Unit,
    onPickColor: () -> Unit,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var textColor by remember { mutableIntStateOf(android.graphics.Color.BLACK) }

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Add Text", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 4,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            brushColors.forEach { color ->
                val isSelected = color == textColor
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(color))
                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .clickable { textColor = color },
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onPickColor),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = { if (text.isNotBlank()) onAddText(text, textColor) },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank(),
        ) {
            Text("Add to Image")
        }
    }
}

// ── Crop Panel ──────────────────────────────────────────────────────────

@Composable
private fun CropPanel(
    cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float,
    imgWidth: Int, imgHeight: Int,
    onApplyCrop: () -> Unit, onReset: () -> Unit, onClose: () -> Unit,
) {
    val pxLeft = (cropLeft * imgWidth).roundToInt()
    val pxTop = (cropTop * imgHeight).roundToInt()
    val pxRight = (cropRight * imgWidth).roundToInt()
    val pxBottom = (cropBottom * imgHeight).roundToInt()
    val cropW = (pxRight - pxLeft).coerceAtLeast(0)
    val cropH = (pxBottom - pxTop).coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Crop", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Crop: ${cropW} × ${cropH} px  (${imgWidth} × ${imgHeight} full)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset") }
            FilledTonalButton(onClick = onApplyCrop, modifier = Modifier.weight(1f)) { Text("Apply Crop") }
        }
    }
}

// ── Aspect Panel ────────────────────────────────────────────────────────

@Composable
private fun AspectPanel(selected: AspectPreset, onSelect: (AspectPreset) -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Aspect Ratio", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectPreset.entries.forEach { preset ->
                val isSelected = preset == selected
                val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg)
                        .clickable { onSelect(preset) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val previewMod = when (preset) {
                        AspectPreset.CIRCLE -> Modifier.size(32.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                        else -> {
                            val r = preset.ratio ?: 1.5f
                            Modifier.width(40.dp).height((40 / r).dp.coerceIn(20.dp, 50.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                        }
                    }
                    Box(previewMod)
                    Spacer(Modifier.height(4.dp))
                    Text(preset.label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ── Background Panel ────────────────────────────────────────────────────

@Composable
private fun BgPanel(selectedColor: Int, onColorSelect: (Int) -> Unit, onPickImage: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Background", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        FilledTonalButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Use Image as Background")
        }

        Spacer(Modifier.height(12.dp))
        Text("Or pick a color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(bgColors) { color ->
                val isSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(
                            if (color == android.graphics.Color.TRANSPARENT) {
                                Modifier
                                    .background(ComposeColor(0xFFCCCCCC))
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(ComposeColor.White)
                            } else {
                                Modifier.background(ComposeColor(color))
                            }
                        )
                        .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .clickable { onColorSelect(color) },
                ) {
                    if (color == android.graphics.Color.TRANSPARENT) {
                        Text(
                            "None",
                            fontSize = 7.sp,
                            modifier = Modifier.align(Alignment.Center),
                            color = ComposeColor.Black,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
