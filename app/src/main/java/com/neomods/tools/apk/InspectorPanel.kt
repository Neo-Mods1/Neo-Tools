package com.neomods.tools.apk

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InspectorPanel(
    apkInfo: ApkInfo,
    activeTab: ManifestTab,
    onTabSelected: (ManifestTab) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showManifestDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Tab bar ──────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = ManifestTab.entries.indexOf(activeTab),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {}
        ) {
            ManifestTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(tab.label, maxLines = 1) }
                )
            }
        }

        // ── Content ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            when (activeTab) {
                ManifestTab.OVERVIEW -> OverviewSection(apkInfo, showManifestDialog = { showManifestDialog = true })
                ManifestTab.PERMISSIONS -> PermissionsSection(apkInfo)
                ManifestTab.ACTIVITIES -> ComponentSection("Activities", apkInfo.activities, apkInfo.activityCount)
                ManifestTab.SERVICES -> ComponentSection("Services", apkInfo.services, apkInfo.serviceCount)
                ManifestTab.RECEIVERS -> ComponentSection("Receivers", apkInfo.receivers, apkInfo.receiverCount)
                ManifestTab.PROVIDERS -> ComponentSection("Providers", apkInfo.providers, apkInfo.providerCount)
                ManifestTab.FEATURES -> FeaturesSection(apkInfo)
                ManifestTab.INTENT_FILTERS -> IntentFiltersSection(apkInfo)
                ManifestTab.META_DATA -> MetaDataSection(apkInfo)
                ManifestTab.QUERIES -> QueriesSection(apkInfo)
                ManifestTab.DEFS -> DefinedPermissionsSection(apkInfo)
            }
        }
    }

    if (showManifestDialog) {
        ManifestViewerDialog(
            manifestXml = apkInfo.manifestXml,
            packageName = apkInfo.packageName,
            onDismiss = { showManifestDialog = false }
        )
    }
}

// ── Overview Section ─────────────────────────────────────────────────────

@Composable
private fun OverviewSection(apkInfo: ApkInfo, showManifestDialog: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    // App header
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            apkInfo.icon?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = apkInfo.label,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(16.dp))
            }
            Column {
                Text(apkInfo.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(apkInfo.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("v${apkInfo.versionName} (${apkInfo.versionCode})",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    // Quick actions
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { showManifestDialog() },
            label = { Text("View Manifest") },
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f)
        )
        AssistChip(
            onClick = { copyToClipboard(context, apkInfo.manifestXml, "Manifest XML") },
            label = { Text("Copy XML") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(8.dp))

    // Basic Info
    SectionHeader("Basic Information")
    InfoCard {
        InfoRow("Package", apkInfo.packageName)
        InfoRow("Version", "${apkInfo.versionName} (${apkInfo.versionCode})")
        InfoRow("UID", apkInfo.uid.toString())
        InfoRow("Process", apkInfo.processName)
        if (apkInfo.sharedUserId.isNotEmpty()) InfoRow("Shared UID", apkInfo.sharedUserId)
    }

    // SDK
    SectionHeader("SDK Information")
    InfoCard {
        InfoRow("Min SDK", apkInfo.minSdk.toString())
        InfoRow("Target SDK", apkInfo.targetSdk.toString())
    }

    // Installation
    SectionHeader("Installation")
    InfoCard {
        if (apkInfo.installDate > 0) InfoRow("Install Date", dateFormat.format(Date(apkInfo.installDate)))
        if (apkInfo.lastUpdateDate > 0) InfoRow("Last Update", dateFormat.format(Date(apkInfo.lastUpdateDate)))
        if (apkInfo.installerPackage.isNotEmpty()) InfoRow("Installer", apkInfo.installerPackage)
    }

    // Storage
    SectionHeader("Storage")
    InfoCard {
        InfoRow("APK Size", apkInfo.apkSizeFormatted)
        InfoRow("APK Path", apkInfo.apkPath)
        if (apkInfo.dataDir.isNotEmpty()) InfoRow("Data Dir", apkInfo.dataDir)
        if (apkInfo.nativeLibraryDir.isNotEmpty()) InfoRow("Native Lib Dir", apkInfo.nativeLibraryDir)
        InfoRow("Source Dir", apkInfo.sourceDir)
    }

    // Security
    SectionHeader("Security")
    InfoCard {
        SecurityFlag("Debuggable", apkInfo.debuggable)
        SecurityFlag("Allow Backup", apkInfo.allowBackup)
        SecurityFlag("Cleartext Traffic", apkInfo.usesCleartext)
        SecurityFlag("Extract Native Libs", apkInfo.extractNativeLibs)
        SecurityFlag("Test Only", apkInfo.testOnly)
    }

    // Signing
    SectionHeader("Signing")
    InfoCard {
        SecurityFlag("V1 (JAR)", apkInfo.hasV1Signing)
        SecurityFlag("V2 (APK Sig)", apkInfo.hasV2Signing)
        SecurityFlag("V3", apkInfo.hasV3Signing)
    }

    // Certificates
    if (apkInfo.certificates.isNotEmpty()) {
        SectionHeader("Certificates")
        apkInfo.certificates.forEach { cert ->
            InfoCard {
                InfoRow("File", cert.file)
                InfoRow("Subject", cert.subject)
                InfoRow("Issuer", cert.issuer)
                InfoRow("Serial", cert.serial)
                InfoRow("Algorithm", cert.sigAlgorithm)
                InfoRow("Valid From", cert.notBefore)
                InfoRow("Valid Until", cert.notAfter)
                if (cert.sha256.isNotEmpty()) InfoRow("SHA-256", cert.sha256)
            }
        }
    }

    // Native
    SectionHeader("Native Architecture")
    InfoCard {
        InfoRow("ABIs", apkInfo.abis.joinToString(", "))
        InfoRow("Native Libs", apkInfo.nativeLibsCount.toString())
        InfoRow("32-bit .so", apkInfo.so32Count.toString())
        InfoRow("64-bit .so", apkInfo.so64Count.toString())
    }

    // General
    SectionHeader("General")
    InfoCard {
        InfoRow("Total ZIP Entries", apkInfo.totalEntries.toString())
        InfoRow("DEX Files", apkInfo.dexCount.toString())
        InfoRow("Has Resources", apkInfo.hasResources.toString())
        InfoRow("Debug Symbols", apkInfo.hasDebugSymbols.toString())
        InfoRow("Permissions", apkInfo.permissionCount.toString())
        InfoRow("Activities", apkInfo.activityCount.toString())
        InfoRow("Services", apkInfo.serviceCount.toString())
        InfoRow("Receivers", apkInfo.receiverCount.toString())
        InfoRow("Providers", apkInfo.providerCount.toString())
    }
}

// ── Permissions Section ──────────────────────────────────────────────────

@Composable
private fun PermissionsSection(apkInfo: ApkInfo) {
    SectionHeader("Requested Permissions (${apkInfo.requestedPermissions.size})")
    if (apkInfo.requestedPermissions.isEmpty()) {
        InfoCard { Text("No permissions requested", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else {
        InfoCard {
            apkInfo.requestedPermissions.forEachIndexed { index, perm ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(perm, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ── Component Section (Activities, Services, Receivers, Providers) ───────

@Composable
private fun ComponentSection(
    title: String,
    components: List<ComponentInfo>,
    count: Int
) {
    SectionHeader("$title ($count)")
    if (components.isEmpty()) {
        InfoCard { Text("No $title found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else {
        components.forEach { comp ->
            InfoCard {
                Text(comp.name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (comp.exported) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Exported", fontSize = 10.sp) }
                        )
                    }
                    if (comp.permission.isNotEmpty()) {
                        Text("Permission: ${comp.permission}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── Features Section ─────────────────────────────────────────────────────

@Composable
private fun FeaturesSection(apkInfo: ApkInfo) {
    SectionHeader("Requested Features (${apkInfo.features.size})")
    if (apkInfo.features.isEmpty()) {
        InfoCard { Text("No features declared", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else {
        InfoCard {
            apkInfo.features.forEach { feature ->
                Text(feature, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ── Intent Filters Section ───────────────────────────────────────────────

@Composable
private fun IntentFiltersSection(apkInfo: ApkInfo) {
    SectionHeader("Intent Filters (${apkInfo.intentFilters.size})")
    if (apkInfo.intentFilters.isEmpty()) {
        InfoCard { Text("No intent filters parsed", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else {
        apkInfo.intentFilters.forEach { filter ->
            InfoCard {
                if (filter.actions.isNotEmpty()) {
                    Text("Actions:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    filter.actions.forEach { Text("  $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                }
                if (filter.categories.isNotEmpty()) {
                    Text("Categories:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    filter.categories.forEach { Text("  $it", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                }
            }
        }
    }
}

// ── Meta-data, Queries, Defined Permissions (placeholder sections) ──────

@Composable
private fun MetaDataSection(apkInfo: ApkInfo) {
    SectionHeader("Meta-data")
    InfoCard { Text("Parsed from manifest XML", color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun QueriesSection(apkInfo: ApkInfo) {
    SectionHeader("Queries")
    InfoCard { Text("Parsed from manifest XML", color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun DefinedPermissionsSection(apkInfo: ApkInfo) {
    SectionHeader("Defined Permissions")
    InfoCard { Text("Parsed from manifest XML", color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

// ── Shared UI Components ─────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SecurityFlag(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (enabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            if (enabled) "Yes" else "No",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
