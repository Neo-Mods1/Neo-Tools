package com.neomods.tools.apk

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInfoScreen(
    onBack: () -> Unit,
    viewModel: ApkInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps()
    }

    val listWidth = animateDpAsState(
        targetValue = if (state.isInspectorOpen) 80.dp else 0.dp,
        animationSpec = tween(300),
        label = "listWidth"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.isInspectorOpen && state.selectedAppLabel != null) {
                        Text(state.selectedAppLabel!!, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("APK Info")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isInspectorOpen) viewModel.closeInspector()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isInspectorOpen) {
                        var searchActive by remember { mutableStateOf(false) }
                        var searchText by remember { mutableStateOf("") }

                        IconButton(onClick = { searchActive = !searchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }

                        AnimatedVisibility(visible = searchActive) {
                            TextField(
                                value = searchText,
                                onValueChange = {
                                    searchText = it
                                    viewModel.setSearchQuery(it)
                                },
                                placeholder = { Text("Search apps…") },
                                singleLine = true,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .padding(end = 8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) ── App List (compresses to icon dock when inspector is open) ──────
        if (listWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .width(listWidth.value)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                AppIconDock(
                    apps = state.filteredApps,
                    selectedPackage = state.selectedPackage,
                    onSelect = { viewModel.selectApp(it) }
                )
            }
        }

        AnimatedVisibility(
            visible = !state.isInspectorOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    AppList(
                        apps = state.filteredApps,
                        onSelect = { viewModel.selectApp(it) }
                    )
                }
            }
        }

        // ── Inspector Panel ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.isInspectorOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isParsing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Parsing ${state.selectedAppLabel}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (state.parsedApk != null) {
                InspectorPanel(
                    apkInfo = state.parsedApk!!,
                    activeTab = state.activeManifestTab,
                    onTabSelected = { viewModel.setActiveManifestTab(it) },
                    onClose = { viewModel.closeInspector() }
                )
            } else if (state.parseError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("Parse error: ${state.parseError}",
                            color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ── Full app list ────────────────────────────────────────────────────────

@Composable
private fun AppList(
    apps: List<AppListItem>,
    onSelect: (AppListItem) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppListItemRow(app = app, onClick = { onSelect(app) })
        }
    }
}

@Composable
private fun AppListItemRow(
    app: AppListItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let { drawable ->
            Image(
                bitmap = drawable.toBitmap(48, 48).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } ?: Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "v${app.versionName} (${app.versionCode})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Icon dock (compressed view) ──────────────────────────────────────────

@Composable
private fun AppIconDock(
    apps: List<AppListItem>,
    selectedPackage: String?,
    onSelect: (AppListItem) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(apps, key = { it.packageName }) { app ->
            val isSelected = app.packageName == selectedPackage
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(app) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                app.icon?.let { drawable ->
                    Image(
                        bitmap = drawable.toBitmap(40, 40).asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}
