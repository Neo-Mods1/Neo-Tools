package com.neomods.tools.apk

import android.content.pm.PackageInfo
import android.graphics.Bitmap

data class ApkInfo(
    val packageName: String = "",
    val label: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    val icon: Bitmap? = null,
    val apkPath: String = "",
    val appName: String = "",
    // From PackageManager
    val uid: Int = 0,
    val targetSdk: Int = 0,
    val minSdk: Int = 0,
    val compileSdk: Int = 0,
    val installDate: Long = 0,
    val lastUpdateDate: Long = 0,
    val installerPackage: String = "",
    val installLocation: String = "",
    val sharedUserId: String = "",
    val processName: String = "",
    val dataDir: String = "",
    val nativeLibraryDir: String = "",
    val sourceDir: String = "",
    // From native parser
    val apkSize: Long = 0,
    val apkSizeFormatted: String = "",
    val totalEntries: Int = 0,
    val activityCount: Int = 0,
    val serviceCount: Int = 0,
    val receiverCount: Int = 0,
    val providerCount: Int = 0,
    val permissionCount: Int = 0,
    val abis: List<String> = emptyList(),
    val nativeLibsCount: Int = 0,
    val nativeLibFiles: List<String> = emptyList(),
    val so32Count: Int = 0,
    val so64Count: Int = 0,
    val hasManifest: Boolean = false,
    val hasResources: Boolean = false,
    val dexCount: Int = 0,
    val hasDebugSymbols: Boolean = false,
    val hasV1Signing: Boolean = false,
    val hasV2Signing: Boolean = false,
    val hasV3Signing: Boolean = false,
    // Security
    val debuggable: Boolean = false,
    val allowBackup: Boolean = false,
    val usesCleartext: Boolean = false,
    val extractNativeLibs: Boolean = false,
    val testOnly: Boolean = false,
    val profileable: Boolean = false,
    // Certificates
    val certificates: List<CertInfo> = emptyList(),
    // Manifest parsed
    val manifestXml: String = "",
    val requestedPermissions: List<String> = emptyList(),
    val activities: List<ComponentInfo> = emptyList(),
    val services: List<ComponentInfo> = emptyList(),
    val receivers: List<ComponentInfo> = emptyList(),
    val providers: List<ComponentInfo> = emptyList(),
    val features: List<String> = emptyList(),
    val intentFilters: List<IntentFilterInfo> = emptyList(),
)

data class CertInfo(
    val file: String = "",
    val subject: String = "",
    val issuer: String = "",
    val serial: String = "",
    val sigAlgorithm: String = "",
    val notBefore: String = "",
    val notAfter: String = "",
    val sha256: String = "",
    val sha1: String = "",
)

data class ComponentInfo(
    val name: String = "",
    val exported: Boolean = false,
    val enabled: Boolean = true,
    val permission: String = "",
    val intentFilters: List<IntentFilterInfo> = emptyList(),
)

data class IntentFilterInfo(
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val data: List<DataInfo> = emptyList(),
)

data class DataInfo(
    val scheme: String = "",
    val host: String = "",
    val port: String = "",
    val path: String = "",
    val mimeType: String = "",
)

enum class ManifestTab(val label: String) {
    OVERVIEW("Overview"),
    PERMISSIONS("Permissions"),
    ACTIVITIES("Activities"),
    SERVICES("Services"),
    RECEIVERS("Receivers"),
    PROVIDERS("Providers"),
    FEATURES("Features"),
    INTENT_FILTERS("Intent Filters"),
    META_DATA("Meta-data"),
    QUERIES("Queries"),
    DEFS("Defined Permissions"),
}

data class ManifestSection(
    val title: String,
    val items: List<String>,
)
