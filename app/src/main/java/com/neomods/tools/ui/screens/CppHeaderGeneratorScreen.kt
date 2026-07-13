package com.neomods.tools.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.EditText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neomods.tools.R
import com.neomods.tools.encoding.CppConverterViewModel
import com.neomods.tools.ui.components.NeoTopBar
import com.neomods.tools.ui.theme.NeoDimens
import android.content.ContentResolver
import android.os.ParcelFileDescriptor

@Composable
fun CppHeaderGeneratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: CppConverterViewModel = viewModel()

    val inputFileName by vm.inputFileName.collectAsState()
    val inputBytes by vm.inputBytes.collectAsState()
    val generatedHeader by vm.generatedHeader.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()
    val toast by vm.toast.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val pickFileLauncher = rememberLauncherForActivityResult(
        OpenAnyFile()
    ) { uri ->
        uri?.let { vm.loadInputFile(it) }
    }

    LaunchedEffect(toast) {
        toast?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = { NeoTopBar(title = stringResource(R.string.cpp_gen_title), onBack = onBack) },
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
                text = stringResource(R.string.cpp_gen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Input file card
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
                        text = stringResource(R.string.cpp_gen_input_label),
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (inputBytes != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inputFileName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatSize(inputBytes!!.size.toLong()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.cpp_gen_no_file),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.cpp_gen_pick_file),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { pickFileLauncher.launch(Unit) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Hero "Generate" button
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
                    .clickable { vm.generateHeader() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tool_hex),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(NeoDimens.IconSize)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.cpp_gen_generate),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (isGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Output section
            generatedHeader?.let { header ->
                Text(
                    text = stringResource(R.string.cpp_gen_output),
                    style = MaterialTheme.typography.titleMedium
                )

                // Show preview only to avoid OOM on large files
                val previewText = remember(header) {
                    val lines = header.lines()
                    if (lines.size > 100) {
                        lines.take(100).joinToString("\n") + "\n\n... (" + (lines.size - 100) + " more lines)"
                    } else {
                        header
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(NeoDimens.CardPadding)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                EditText(ctx).apply {
                                    setText(previewText)
                                    isSingleLine = false
                                    maxLines = Int.MAX_VALUE
                                    isFocusable = false
                                    isFocusableInTouchMode = false
                                    textSize = 12f
                                    typeface = android.graphics.Typeface.MONOSPACE
                                    setPadding(16, 12, 16, 12)
                                    setBackgroundColor(0x00000000)
                                    setTextColor(ctx.getColor(android.R.color.black))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Save button
                val saveLauncher = rememberLauncherForActivityResult(
                    Save_hpp()
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { os ->
                                os.write(header.toByteArray(Charsets.UTF_8))
                            }
                        }.onSuccess {
                            vm.notify(context.getString(R.string.cpp_gen_saved))
                        }.onFailure {
                            vm.notify(context.getString(R.string.cpp_gen_save_failed))
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        val baseName = inputFileName.ifEmpty { "output" }
                            .substringBeforeLast('.')
                        saveLauncher.launch("$baseName.hpp")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cpp_gen_save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "--"
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.1f KB".format(kb) else "%.2f MB".format(kb / 1024)
}

private class OpenAnyFile : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: android.content.Context, input: Unit): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}

private class Save_hpp : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: android.content.Context, input: String): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/x-hpp"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}
