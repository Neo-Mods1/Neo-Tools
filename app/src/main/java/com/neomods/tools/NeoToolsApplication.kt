package com.neomods.tools

import android.app.Application
import android.content.Intent
import com.neomods.tools.crash.CrashActivity
import com.neomods.tools.crash.CrashHandler
import com.neomods.tools.crash.CrashReporter
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
 * Owns the manually-wired dependencies (repositories) so the rest of the app
 * can stay decoupled from concrete implementations. This is intentionally
 * lightweight — no third-party DI framework is used to keep the APK small.
 *
 * Also installs the global [CrashHandler] so uncaught exceptions are routed
 * to the crash report screen, and checks for native crash files left by the
 * C++ signal handler.
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
        CrashHandler(this).init()
        checkNativeCrash()
    }

    /**
     * If the C++ signal handler wrote a crash file on the previous run, read
     * it and launch [CrashActivity] so the user sees the native crash details
     * and can opt into reporting.
     */
    private fun checkNativeCrash() {
        try {
            val crashLog = NeoNative.nativeCheckCrashFile() ?: return

            val exceptionType = extractNativeExceptionType(crashLog)
            val stacktrace = extractNativeStacktrace(crashLog)

            val intent = Intent(this, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_LOG, crashLog)
                putExtra(CrashActivity.EXTRA_CRASH_MESSAGE, "Native crash (signal)")
                putExtra(CrashActivity.EXTRA_EXCEPTION_TYPE, exceptionType)
                putExtra(CrashActivity.EXTRA_STACKTRACE, stacktrace)
                putExtra(CrashActivity.EXTRA_THREAD, "native")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // If JNI is broken, don't crash the app trying to report a crash
        }
    }

    private fun extractNativeExceptionType(log: String): String {
        val match = Regex("""Signal:\s*(\S+)""").find(log)
        return match?.groupValues?.get(1) ?: "NativeCrash"
    }

    private fun extractNativeStacktrace(log: String): String {
        return log
    }
}
