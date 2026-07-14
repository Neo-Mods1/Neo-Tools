package com.neomods.tools.apk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ManifestViewerDialog(
    manifestXml: String,
    packageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Raw XML", "Permissions", "Activities", "Services", "Receivers", "Providers", "Features")

    val parsedPermissions = remember(manifestXml) {
        Regex("""uses-permission[^>]*name="([^"]+)"""").findAll(manifestXml)
            .map { it.groupValues[1] }.toList()
    }
    val parsedActivities = remember(manifestXml) {
        Regex("""<activity\s[^>]*name="([^"]+)"[^>]*>""").findAll(manifestXml)
            .map { it.groupValues[1] }.toList()
    }
    val parsedServices = remember(manifestXml) {
        Regex("""<service\s[^>]*name="([^"]+)"[^>]*>""").findAll(manifestXml)
            .map { it.groupValues[1] }.toList()
    }
    val parsedReceivers = remember(manifestXml) {
        Regex("""<receiver\s[^>]*name="([^"]+)"[^>]*>""").findAll(manifestXml)
            .map { it.groupValues[1] }.toList()
    }
    val parsedProviders = remember(manifestXml) {
        Regex("""<provider\s[^>]*name="([^"]+)"[^>]*>""").findAll(manifestXml)
            .map { it.groupValues[1] }.toList()
    }
    val parsedFeatures = remember(manifestXml) {
        Regex("""uses-feature[^>]*name="([^"]+)"""").findAll(manifestXml)
            .map { it.groupValues[1] }.toList()
    }

    val filteredContent = remember(activeTab, searchText, parsedPermissions, parsedActivities,
        parsedServices, parsedReceivers, parsedProviders, parsedFeatures, manifestXml) {
        val items = when (activeTab) {
            1 -> parsedPermissions
            2 -> parsedActivities
            3 -> parsedServices
            4 -> parsedReceivers
            5 -> parsedProviders
            6 -> parsedFeatures
            else -> emptyList()
        }
        if (searchText.isEmpty()) items
        else items.filter { it.contains(searchText, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AndroidManifest.xml", style = MaterialTheme.typography.titleMedium)
                        Text(packageName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        copyToClipboard(context, manifestXml, "Manifest XML")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 8.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            1 -> parsedPermissions.size
                            2 -> parsedActivities.size
                            3 -> parsedServices.size
                            4 -> parsedReceivers.size
                            5 -> parsedProviders.size
                            6 -> parsedFeatures.size
                            else -> 0
                        }
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                Text(
                                    if (count > 0) "$title ($count)" else title,
                                    maxLines = 1,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Content
                if (activeTab == 0) {
                    // Raw XML
                    HorizontalScrollbar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = manifestXml.ifEmpty { "No manifest data available" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                } else {
                    // Structured list
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        if (filteredContent.isEmpty()) {
                            Text("No items found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp))
                        } else {
                            filteredContent.forEach { item ->
                                ListItem(
                                    headlineContent = {
                                        Text(item, style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace)
                                    },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            copyToClipboard(context, item, "Component")
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                                                modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalScrollbar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
