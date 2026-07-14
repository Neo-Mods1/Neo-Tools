package com.neomods.tools.apk

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.tools.native.NeoNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ApkInfoViewModel(app: Application) : AndroidViewModel(app) {

    var state by mutableStateOf(ApkInfoState())
        private set

    private var parseJob: Job? = null
    private val cache = mutableMapOf<String, ApkInfo>()
    private val pm: PackageManager = app.packageManager

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                .filter { it.applicationInfo != null }
                .sortedBy { pm.getApplicationLabel(it.applicationInfo!!).toString().lowercase() }

            val appList = apps.map { pkg ->
                val ai = pkg.applicationInfo!!
                AppListItem(
                    packageName = pkg.packageName,
                    label = pm.getApplicationLabel(ai).toString(),
                    versionName = pkg.versionName ?: "",
                    versionCode = pkg.longVersionCode,
                    icon = pm.getApplicationIcon(ai),
                    apkPath = ai.sourceDir ?: "",
                    processName = ai.processName,
                    targetSdk = ai.targetSdkVersion,
                    minSdk = ai.minSdkVersion,
                    uid = ai.uid,
                    dataDir = ai.dataDir ?: "",
                    nativeLibDir = ai.nativeLibraryDir ?: "",
                    sourceDir = ai.sourceDir ?: "",
                    sharedUserId = pkg.sharedUserId ?: "",
                    installerPackage = try { pm.getInstallSourceInfo(pkg.packageName).installingPackageName ?: "" } catch (_: Exception) { "" },
                    firstInstallTime = pkg.firstInstallTime,
                    lastUpdateTime = pkg.lastUpdateTime,
                    debuggable = (ai.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                    allowBackup = (ai.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0,
                    extractNativeLibs = (ai.flags and android.content.pm.ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) != 0,
                    testOnly = (ai.flags and android.content.pm.ApplicationInfo.FLAG_TEST_ONLY) != 0,
                )
            }

            withContext(Dispatchers.Main) {
                state = state.copy(
                    allApps = appList,
                    filteredApps = appList,
                    isLoading = false
                )
            }
        }
    }

    fun filterApps(query: String) {
        val q = query.lowercase().trim()
        val filtered = if (q.isEmpty()) {
            state.allApps
        } else {
            state.allApps.filter {
                it.label.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q)
            }
        }
        state = state.copy(filteredApps = filtered, searchQuery = query)
    }

    fun selectApp(app: AppListItem) {
        if (state.selectedPackage == app.packageName) return

        parseJob?.cancel()
        state = state.copy(
            selectedPackage = app.packageName,
            selectedAppLabel = app.label,
            isInspectorOpen = true,
            isParsing = true,
            parsedApk = null
        )

        // Check cache first
        val cached = cache[app.packageName]
        if (cached != null) {
            state = state.copy(isParsing = false, parsedApk = cached)
            return
        }

        parseJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = parseApk(app)
                withContext(Dispatchers.Main) {
                    cache[app.packageName] = info
                    if (state.selectedPackage == app.packageName) {
                        state = state.copy(isParsing = false, parsedApk = info)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    state = state.copy(isParsing = false, parseError = e.message)
                }
            }
        }
    }

    fun closeInspector() {
        parseJob?.cancel()
        state = state.copy(
            isInspectorOpen = false,
            selectedPackage = null,
            selectedAppLabel = null,
            parsedApk = null,
            isParsing = false,
            activeManifestTab = ManifestTab.OVERVIEW
        )
    }

    fun setActiveManifestTab(tab: ManifestTab) {
        state = state.copy(activeManifestTab = tab)
    }

    fun setSearchQuery(query: String) {
        filterApps(query)
    }

    @Suppress("DEPRECATION")
    private fun parseApk(app: AppListItem): ApkInfo {
        val pkgInfo = try {
            pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES or
                PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_CONFIGURATIONS)
        } catch (e: Exception) {
            pm.getPackageInfo(app.packageName, 0)
        }

        // Parse via native
        val nativeJson = try {
            if (app.apkPath.isNotEmpty()) {
                JSONObject(NeoNative.nativeParseApkInfo(app.apkPath))
            } else JSONObject()
        } catch (e: Exception) { JSONObject() }

        val manifestJson = try {
            if (app.apkPath.isNotEmpty()) {
                val xml = NeoNative.nativeParseManifest(app.apkPath)
                parseManifestComponents(xml)
            } else ManifestParseResult()
        } catch (e: Exception) { ManifestParseResult() }

        val certJson = try {
            if (app.apkPath.isNotEmpty()) {
                JSONArray(NeoNative.nativeParseCertificate(app.apkPath))
            } else JSONArray()
        } catch (e: Exception) { JSONArray() }

        val certs = mutableListOf<CertInfo>()
        for (i in 0 until certJson.length()) {
            val obj = certJson.optJSONObject(i) ?: continue
            certs.add(CertInfo(
                file = obj.optString("file", ""),
                subject = obj.optString("subject", ""),
                issuer = obj.optString("issuer", ""),
                serial = obj.optString("serial", ""),
                sigAlgorithm = obj.optString("sigAlgorithm", ""),
                notBefore = obj.optString("notBefore", ""),
                notAfter = obj.optString("notAfter", ""),
                sha256 = obj.optString("sha256", ""),
                sha1 = obj.optString("sha1", ""),
            ))
        }

        // Extract features
        val features = mutableListOf<String>()
        try {
            val featArray = pkgInfo.reqFeatures
            if (featArray != null) {
                for (f in featArray) {
                    features.add(f.name)
                }
            }
        } catch (_: Exception) {}

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        return ApkInfo(
            packageName = app.packageName,
            label = app.label,
            versionName = app.versionName,
            versionCode = app.versionCode,
            icon = app.icon,
            apkPath = app.apkPath,
            appName = app.label,
            uid = app.uid,
            targetSdk = app.targetSdk,
            minSdk = app.minSdk,
            installDate = app.firstInstallTime,
            lastUpdateDate = app.lastUpdateTime,
            installerPackage = app.installerPackage,
            installLocation = try { pkgInfo.installLocation?.toString() ?: "?" } catch (_: Exception) { "?" },
            sharedUserId = app.sharedUserId,
            processName = app.processName,
            dataDir = app.dataDir,
            nativeLibraryDir = app.nativeLibDir,
            sourceDir = app.sourceDir,
            apkSize = nativeJson.optLong("apkSize", 0),
            apkSizeFormatted = nativeJson.optString("apkSizeFormatted", "?"),
            totalEntries = nativeJson.optInt("totalEntries", 0),
            activityCount = nativeJson.optInt("activityCount", manifestJson.activities.size),
            serviceCount = nativeJson.optInt("serviceCount", manifestJson.services.size),
            receiverCount = nativeJson.optInt("receiverCount", manifestJson.receivers.size),
            providerCount = nativeJson.optInt("providerCount", manifestJson.providers.size),
            permissionCount = nativeJson.optInt("permissionCount", manifestJson.permissions.size),
            abis = nativeJson.optJSONArray("abis")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList(),
            nativeLibsCount = nativeJson.optJSONArray("nativeLibs")?.length() ?: 0,
            so32Count = nativeJson.optInt("so32Count", 0),
            so64Count = nativeJson.optInt("so64Count", 0),
            hasManifest = nativeJson.optBoolean("hasManifest", false),
            hasResources = nativeJson.optBoolean("hasResources", false),
            dexCount = nativeJson.optInt("dexCount", 0),
            hasDebugSymbols = nativeJson.optBoolean("hasDebugSymbols", false),
            hasV1Signing = nativeJson.optBoolean("hasV1Signing", false),
            hasV2Signing = nativeJson.optBoolean("hasV2Signing", false),
            hasV3Signing = nativeJson.optBoolean("hasV3Signing", false),
            debuggable = app.debuggable,
            allowBackup = app.allowBackup,
            usesCleartext = false,
            extractNativeLibs = app.extractNativeLibs,
            testOnly = app.testOnly,
            certificates = certs,
            manifestXml = try {
                if (app.apkPath.isNotEmpty()) NeoNative.nativeGetManifestXml(app.apkPath) else ""
            } catch (_: Exception) { "" },
            requestedPermissions = manifestJson.permissions,
            activities = manifestJson.activities,
            services = manifestJson.services,
            receivers = manifestJson.receivers,
            providers = manifestJson.providers,
            features = features,
            intentFilters = manifestJson.intentFilters,
        )
    }

    private fun parseManifestComponents(xml: String): ManifestParseResult {
        val result = ManifestParseResult()
        if (xml.isEmpty()) return result

        // Parse permissions
        val permPattern = Regex("""uses-permission[^>]*name="([^"]+)"""")
        result.permissions.clear()
        result.permissions.addAll(permPattern.findAll(xml).map { it.groupValues[1] }.toList())

        // Parse activities
        val actPattern = Regex("""<activity\s[^>]*name="([^"]+)"[^>]*>""")
        for (match in actPattern.findAll(xml)) {
            val name = match.groupValues[1]
            val exported = xml.substring(match.range).contains("exported=\"true\"")
            val permission = Regex("""permission="([^"]+)"""").find(xml.substring(match.range))?.groupValues?.get(1) ?: ""
            result.activities.add(ComponentInfo(name = name, exported = exported, permission = permission))
        }

        // Parse services
        val svcPattern = Regex("""<service\s[^>]*name="([^"]+)"[^>]*>""")
        for (match in svcPattern.findAll(xml)) {
            val name = match.groupValues[1]
            val exported = xml.substring(match.range).contains("exported=\"true\"")
            result.services.add(ComponentInfo(name = name, exported = exported))
        }

        // Parse receivers
        val rcvPattern = Regex("""<receiver\s[^>]*name="([^"]+)"[^>]*>""")
        for (match in rcvPattern.findAll(xml)) {
            val name = match.groupValues[1]
            val exported = xml.substring(match.range).contains("exported=\"true\"")
            result.receivers.add(ComponentInfo(name = name, exported = exported))
        }

        // Parse providers
        val prvPattern = Regex("""<provider\s[^>]*name="([^"]+)"[^>]*>""")
        for (match in prvPattern.findAll(xml)) {
            val name = match.groupValues[1]
            result.providers.add(ComponentInfo(name = name))
        }

        return result
    }

    data class ManifestParseResult(
        val permissions: MutableList<String> = mutableListOf(),
        val activities: MutableList<ComponentInfo> = mutableListOf(),
        val services: MutableList<ComponentInfo> = mutableListOf(),
        val receivers: MutableList<ComponentInfo> = mutableListOf(),
        val providers: MutableList<ComponentInfo> = mutableListOf(),
        val intentFilters: MutableList<IntentFilterInfo> = mutableListOf(),
    )
}

data class ApkInfoState(
    val allApps: List<AppListItem> = emptyList(),
    val filteredApps: List<AppListItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isInspectorOpen: Boolean = false,
    val selectedPackage: String? = null,
    val selectedAppLabel: String? = null,
    val isParsing: Boolean = false,
    val parsedApk: ApkInfo? = null,
    val parseError: String? = null,
    val activeManifestTab: ManifestTab = ManifestTab.OVERVIEW,
)

data class AppListItem(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val apkPath: String,
    val processName: String,
    val targetSdk: Int,
    val minSdk: Int,
    val uid: Int,
    val dataDir: String,
    val nativeLibDir: String,
    val sourceDir: String,
    val sharedUserId: String,
    val installerPackage: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val debuggable: Boolean,
    val allowBackup: Boolean,
    val extractNativeLibs: Boolean,
    val testOnly: Boolean,
)
