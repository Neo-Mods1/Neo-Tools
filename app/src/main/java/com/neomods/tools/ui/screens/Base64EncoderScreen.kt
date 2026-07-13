package com.neomods.tools.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.neomods.tools.R
import com.neomods.tools.encoding.Base64EncoderViewModel
import com.neomods.tools.encoding.Base64Entry
import com.neomods.tools.encoding.SelectedFile
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.theme.NeoDimens
import com.neomods.tools.ui.theme.NeoGradients

/**
 * Fully functional Base64 Encoder tool.
 *
 * Layout (top to bottom):
 *  1. Selected files preview (images in a responsive gallery, others as cards)
 *  2. Gradient "Select Files" hero -> Material 3 dialog (Image / File)
 *  3. Converted list: each row has a thumbnail/icon, truncated name, encoded
 *     size, copy and save actions.
 *
 * Encoding is delegated to the native layer ([Base64EncoderViewModel] ->
 * `neotools` C++ library) and runs on a background dispatcher.
 */
@Composable
fun Base64EncoderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: Base64EncoderViewModel = viewModel()

    val selected by vm.selected.collectAsState()
    val entries by vm.entries.collectAsState()
    val isEncoding by vm.isEncoding.collectAsState()
    val toast by vm.toast.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showPickerDialog by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Base64Entry?>(null) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> vm.addFiles(uris.filterNotNull()) }

    val pickFiles = rememberLauncherForActivityResult(
        OpenMultipleDocuments(arrayOf("*/*"))
    ) { uris -> vm.addFiles(uris) }

    val saveLauncher = rememberLauncherForActivityResult(
        CreateDocumentContract("text/plain")
    ) { uri ->
        pendingSave?.let { entry ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(entry.base64.toByteArray())
                    }
                }.onSuccess { vm.notify(context.getString(R.string.base64_saved)) }
                    .onFailure { vm.notify(context.getString(R.string.base64_save_failed)) }
            }
        }
        pendingSave = null
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = { NeoTopBar(title = stringResource(R.string.base64_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(NeoDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing)
        ) {
            Text(
                text = stringResource(R.string.base64_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SelectedPreview(selected = selected, onRemove = vm::removeFile)

            // Hero "Select Files" button with the brand gradient.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeoGradients.Primary)
                    .clickable { showPickerDialog = true }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(NeoDimens.IconSize)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.base64_select_files),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (isEncoding) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (entries.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.base64_converted),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(NeoDimens.GroupSpacing)
                ) {
                    entries.forEach { entry ->
                        ConvertedRow(
                            entry = entry,
                            onCopy = vm::copyBase64,
                            onSave = {
                                pendingSave = it
                                saveLauncher.launch(defaultSaveName(it.name))
                            }
                        )
                    }
                }
            } else if (selected.isNotEmpty() && !isEncoding) {
                Text(
                    text = stringResource(R.string.base64_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPickerDialog) {
        PickerDialog(
            onDismiss = { showPickerDialog = false },
                onImage = {
                    showPickerDialog = false
                    pickImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            onFile = {
                showPickerDialog = false
                pickFiles.launch(Unit)
            }
        )
    }
}

@Composable
private fun SelectedPreview(
    selected: List<SelectedFile>,
    onRemove: (String) -> Unit
) {
    val images = selected.filter { it.isImage }
    val files = selected.filter { !it.isImage }

    Card(
        shape = RoundedCornerShape(NeoDimens.CardCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(NeoDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(NeoDimens.GroupSpacing)
        ) {
            Text(
                text = stringResource(R.string.base64_selected),
                style = MaterialTheme.typography.titleSmall
            )

            if (selected.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.base64_preview_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (images.isNotEmpty()) {
                if (images.size == 1) {
                    UriImage(
                        uri = images[0].uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(16.dp)
                            )
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(images, key = { it.id }) { sf ->
                            UriImage(
                                uri = sf.uri,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(14.dp)
                                    )
                            )
                        }
                    }
                }
            }

            if (files.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    files.forEach { sf -> FileChip(sf, onRemove) }
                }
            }
            }
        }
    }
}

@Composable
private fun FileChip(file: SelectedFile, onRemove: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_file),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatSize(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onRemove(file.id) }) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.remove)
            )
        }
    }
}

@Composable
private fun ConvertedRow(
    entry: Base64Entry,
    onCopy: (Base64Entry) -> Unit,
    onSave: (Base64Entry) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = NeoDimens.CardElevation),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (entry.isImage) {
                    UriImage(
                        uri = entry.uri,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_file),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.base64_size,
                        formatSize(entry.encodedSize.toLong())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalIconButton(onClick = { onCopy(entry) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = stringResource(R.string.base64_copy)
                )
            }
            FilledTonalIconButton(onClick = { onSave(entry) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_save),
                    contentDescription = stringResource(R.string.base64_save)
                )
            }
        }
    }
}

@Composable
private fun PickerDialog(
    onDismiss: () -> Unit,
    onImage: () -> Unit,
    onFile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.base64_select_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogOption(
                    icon = R.drawable.ic_image,
                    label = stringResource(R.string.base64_select_image),
                    onClick = onImage
                )
                DialogOption(
                    icon = R.drawable.ic_file,
                    label = stringResource(R.string.base64_select_file),
                    onClick = onFile
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DialogOption(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Loads a content-URI image into a downsampled [Bitmap] so previews stay
 * memory-efficient even for multi-megapixel photos.
 */
@Composable
private fun UriImage(uri: Uri, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            bitmap = loadSampledBitmap(context.contentResolver, uri, 512)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private fun loadSampledBitmap(
    cr: android.content.ContentResolver,
    uri: Uri,
    maxEdge: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
    val (w, h) = bounds.outWidth to bounds.outHeight
    if (w <= 0 || h <= 0) return null

    val rawSample = maxOf(w, h) / maxEdge
    val sample = if (rawSample <= 1) 1 else {
        var s = 1
        while (s * 2 <= rawSample) s *= 2
        s
    }

    val decode = BitmapFactory.Options().apply {
        inJustDecodeBounds = false
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return runCatching { cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) } }
        .getOrNull()
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.1f KB".format(kb) else "%.2f MB".format(kb / 1024)
}

private fun defaultSaveName(name: String): String {
    val base = name.substringBeforeLast('.')
    return if (base.isEmpty()) "base64" else "${base}_base64.txt"
}

/**
 * Version-independent contract for picking one or many documents (images or
 * any file). Uses [Intent.ACTION_OPEN_DOCUMENT] with [Intent.EXTRA_ALLOW_MULTIPLE]
 * so it works across all supported Android versions and surfaces the system
 * photo picker for the "image" MIME type wildcard on Android 13+.
 */
private class OpenMultipleDocuments(
    private val mimeTypes: Array<String>
) : ActivityResultContract<Unit, List<Uri>>() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            if (mimeTypes.size == 1) {
                type = mimeTypes[0]
            } else {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            val out = mutableListOf<Uri>()
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).uri?.let { out.add(it) }
            }
            return out
        }
        val single = intent.data
        return if (single != null) listOf(single) else emptyList()
    }
}

/**
 * Version-independent contract for creating a new document (used to let the
 * user choose where to save the encoded Base64 as a `.txt` file).
 */
private class CreateDocumentContract(
    private val mimeType: String
) : ActivityResultContract<String, Uri?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}