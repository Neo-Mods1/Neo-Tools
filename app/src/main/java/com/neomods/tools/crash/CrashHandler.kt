package com.neomods.tools.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.neomods.tools.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global uncaught-exception handler for Java/Kotlin crashes.
 *
 * Works together with the native [neotools::crash] signal handler:
 * - Java/Kotlin crash → this handler → launches [CrashActivity] immediately
 * - Native signal crash → libCrashHandler.so writes file → next launch reads it → [CrashActivity]
 *
 * Ported from the CODE-IDE crash system (com.neo.ide.crash.CrashHandler). also owned by me neo mods
 */
class CrashHandler private constructor(context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val PREFS = "crash_handler"
        private const val KEY_IS_CRASHING = "is_crashing"

        @Volatile
        private var installed = false

        fun install(context: Context) {
            if (installed) return
            installed = true
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }
    }

    private val appContext = context.applicationContext
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Prevent crash loop: if we're already crashing, delegate to the
        // default handler (which kills the process) instead of launching
        // CrashActivity again.
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IS_CRASHING, false)) {
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        try {
            prefs.edit().putBoolean(KEY_IS_CRASHING, true).apply()

            val crashLog = buildCrashLog(thread, throwable)
            val stacktrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()

            val intent = Intent(appContext, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_CRASH_LOG, crashLog)
                putExtra(CrashActivity.EXTRA_CRASH_MESSAGE, throwable.message ?: "Unknown error")
                putExtra(CrashActivity.EXTRA_EXCEPTION_TYPE, throwable.javaClass.name)
                putExtra(CrashActivity.EXTRA_STACKTRACE, stacktrace)
                putExtra(CrashActivity.EXTRA_THREAD, thread.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            appContext.startActivity(intent)

            // Kill the process after CrashActivity has had time to start.
            // Using Handler.postDelayed instead of Thread.sleep so we don't
            // block the crashing thread (which may be the main thread).
            Handler(Looper.getMainLooper()).postDelayed({
                Process.killProcess(Process.myPid())
            }, 1500)
        } catch (_: Throwable) {
            // Last resort — kill immediately
            Process.killProcess(Process.myPid())
        }
    }

    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()

        sb.appendLine("========================================")
        sb.appendLine("  Neo Tools - Crash Report")
        sb.appendLine("========================================")
        sb.appendLine()
        sb.appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine()
        sb.appendLine("========================================")
        sb.appendLine("  Exception")
        sb.appendLine("========================================")
        sb.appendLine()
        sb.appendLine("Type: ${throwable.javaClass.name}")
        sb.appendLine("Message: ${throwable.message ?: "No message"}")
        sb.appendLine()
        sb.appendLine("========================================")
        sb.appendLine("  Stack Trace")
        sb.appendLine("========================================")
        sb.appendLine()

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        sb.appendLine(sw.toString())

        return sb.toString()
    }
}
