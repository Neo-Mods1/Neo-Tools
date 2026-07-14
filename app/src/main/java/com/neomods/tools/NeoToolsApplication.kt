package com.neomods.tools

import android.app.Application
import android.content.Intent
import com.neomods.tools.crash.CrashActivity
import com.neomods.tools.crash.CrashHandler
import com.neomods.tools.data.CategoryRepository
import com.neomods.tools.data.DefaultCategoryRepository
import com.neomods.tools.data.DefaultPermissionRepository
import com.neomods.tools.data.DefaultToolRepository
import com.neomods.tools.data.PermissionRepository
import com.neomods.tools.data.ToolRepository
import com.neomods.tools.native.NeoNative
import com.neomods.tools.onboarding.DefaultOnboardingRepository
import com.neomods.tools.onboarding.OnboardingRepository

/**
 * Application entry point.
 *
 * Load order matters:
 * 1. libCrashHandler.so — installs POSIX signal handlers FIRST so native
 *    crashes are caught even if the main neotools library fails to load.
 * 2. CrashHandler.install() — installs the Java/Kotlin uncaught-exception handler.
 * 3. Check for a native crash file from the previous run.
 * 4. neotools.so is loaded lazily by NeoNative when first accessed.
 */
class NeoToolsApplication : Application() {

    val categoryRepository: CategoryRepository by lazy { DefaultCategoryRepository() }
    val toolRepository: ToolRepository by lazy { DefaultToolRepository() }
    val permissionRepository: PermissionRepository by lazy { DefaultPermissionRepository() }
    val onboardingRepository: OnboardingRepository by lazy {
        DefaultOnboardingRepository(getSharedPreferences("neo_onboarding", MODE_PRIVATE))
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Load native crash handler library (installs sigaction handlers)
        try {
            System.loadLibrary("CrashHandler")
        } catch (_: Throwable) { }

        // 2. Install Java/Kotlin uncaught-exception handler
        CrashHandler.install(this)

        // 3. If the previous run had a native crash, open CrashActivity
        checkNativeCrash()
    }

    private fun checkNativeCrash() {
        try {
            val crashLog = NeoNative.nativeCheckCrashFile() ?: return

            val exceptionType = Regex("""Signal:\s*(\S+)""").find(crashLog)
                ?.groupValues?.get(1) ?: "NativeCrash"

            val intent = Intent(this, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_LOG, crashLog)
                putExtra(CrashActivity.EXTRA_CRASH_MESSAGE, "Native crash (signal)")
                putExtra(CrashActivity.EXTRA_EXCEPTION_TYPE, exceptionType)
                putExtra(CrashActivity.EXTRA_STACKTRACE, crashLog)
                putExtra(CrashActivity.EXTRA_THREAD, "native")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        } catch (_: Throwable) {
            // If JNI is broken, don't crash the app trying to report a crash
        }
    }
}
