package com.neomods.tools.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.EditText
import android.widget.ScrollView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import com.neomods.tools.encoding.Base64DecoderViewModel
import com.neomods.tools.encoding.DecodedContentType
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.theme.NeoDimens
import java.io.File
import android.text.TextWatcher
import android.text.Editable

@Composable
fun Base64DecoderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: Base64DecoderViewModel = viewModel()

    val inputText by vm.inputText.collectAsState()
    val result by vm.result.collectAsState()
    val isDecoding by vm.isDecoding.collectAsState()
    val toast by vm.toast.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showLoadDialog by remember { mutableStateOf(false) }

    val loadFileLauncher = rememberLauncherForActivityResult(
        OpenTextFile()
    ) { uri ->
        uri?.let { vm.loadFromFile(it) }
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = { NeoTopBar(title = stringResource(R.string.decoder_title), onBack = onBack) },
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
                text = stringResource(R.string.decoder_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Input card — fixed height, scrolls internally
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
                        text = stringResource(R.string.decoder_input_label),
                        style = MaterialTheme.typography.titleSmall
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                EditText(ctx).apply {
                                    hint = "Paste your Base64 string here…"
                                    setText(inputText)
                                    isSingleLine = false
                                    maxLines = Int.MAX_VALUE
                                    textSize = 14f
                                    setPadding(16, 12, 16, 12)
                                    setBackgroundColor(0x00000000)
                                    setTextColor(ctx.getColor(android.R.color.black))
                                    addTextChangedListener(object : TextWatcher {
                                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                                        override fun afterTextChanged(s: Editable?) {
                                            vm.updateInput(s?.toString() ?: "")
                                        }
                                    })
                                    // Prevent parent Column from stealing scroll
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                }
                            },
                            update = { editText ->
                                if (editText.text.toString() != inputText) {
                                    editText.setText(inputText)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.decoder_load_file),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showLoadDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Hero "Decode" button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .clickable { vm.decode() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(NeoDimens.IconSize)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.decoder_decode),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (isDecoding) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Output section
            result?.let { decoded ->
                Text(
                    text = stringResource(R.string.decoder_result),
                    style = MaterialTheme.typography.titleMedium
                )

                when (decoded.contentType) {
                    DecodedContentType.IMAGE -> {
                        DecodedImagePreview(
                            bytes = decoded.bytes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    RoundedCornerShape(16.dp)
                                )
                        )
                    }
                    DecodedContentType.TEXT -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(NeoDimens.CardPadding)
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        EditText(ctx).apply {
                                            setText(decoded.text ?: "")
                                            isSingleLine = false
                                            maxLines = Int.MAX_VALUE
                                            isFocusable = false
                                            isFocusableInTouchMode = false
                                            textSize = 14f
                                            setPadding(16, 12, 16, 12)
                                            setBackgroundColor(0x00000000)
                                            setTextColor(ctx.getColor(android.R.color.black))
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    DecodedContentType.BINARY -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_file),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.decoder_binary_data),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.decoder_size,
                                            formatSize(decoded.bytes.size.toLong())
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Action row — copy + save for all types
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NeoDimens.SectionSpacing)
                ) {
                    FilledTonalIconButton(onClick = { vm.copyDecoded() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = stringResource(R.string.decoder_copy)
                        )
                    }

                    val saveLauncher = rememberLauncherForActivityResult(
                        SaveBinaryFile()
                    ) { uri ->
                        if (uri != null) {
                            runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { os ->
                                    os.write(decoded.bytes)
                                }
                            }.onSuccess {
                                vm.notify(context.getString(R.string.decoder_saved))
                            }.onFailure {
                                vm.notify(context.getString(R.string.decoder_save_failed))
                            }
                        }
                    }

                    val saveFileName = when (decoded.contentType) {
                        DecodedContentType.IMAGE -> "decoded_image.png"
                        DecodedContentType.TEXT -> "decoded_text.txt"
                        DecodedContentType.BINARY -> "decoded_output"
                    }
                    val saveMimeType = when (decoded.contentType) {
                        DecodedContentType.IMAGE -> "image/png"
                        DecodedContentType.TEXT -> "text/plain"
                        DecodedContentType.BINARY -> "application/octet-stream"
                    }

                    FilledTonalIconButton(onClick = {
                        saveLauncher.launch(saveFileName)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = stringResource(R.string.decoder_save)
                        )
                    }
                }
            }
        }
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text(stringResource(R.string.decoder_load_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogRow(
                        icon = R.drawable.ic_file,
                        label = stringResource(R.string.decoder_load_from_file),
                        onClick = {
                            showLoadDialog = false
                            loadFileLauncher.launch(Unit)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun DecodedImagePreview(bytes: ByteArray, modifier: Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bytes) {
        bitmap = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.decoder_image_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DialogRow(icon: Int, label: String, onClick: () -> Unit) {
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

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "--"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.1f KB".format(kb) else "%.2f MB".format(kb / 1024)
}

private class OpenTextFile : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: android.content.Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}

private class SaveBinaryFile : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: android.content.Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}
