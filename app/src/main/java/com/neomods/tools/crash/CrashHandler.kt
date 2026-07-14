package com.neomods.tools.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.neomods.tools.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global uncaught-exception handler.
 *
 * Captures a detailed crash log (device, app version, exception, stack trace
 * and a tail of logcat), then launches [CrashActivity] instead of letting the
 * process die silently.
 *
 * Ported from the CODE-IDE crash system (com.neo.ide.crash.CrashHandler). also owned by me neo mods
 */
class CrashHandler private constructor(context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val PREFS = "crash_handler"
        private const val KEY_IS_CRASHING = "is_crashing"

        @Volatile
        private var installed = false

        /**
         * Install the global crash handler. Safe to call multiple times —
         * only the first call takes effect.
         */
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

            // Give CrashActivity time to start, then kill this process
            Thread.sleep(500)
        } catch (_: Throwable) {
            // Last resort — if anything above fails, just delegate to default
        } finally {
            Process.killProcess(Process.myPid())
            System.exit(1)
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

        sb.appendLine()
        sb.appendLine("========================================")
        sb.appendLine("  Logcat (last 200 lines)")
        sb.appendLine("========================================")
        sb.appendLine()

        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "200", "*:E"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.appendLine(line)
            }
            reader.close()
        } catch (_: Exception) {
            sb.appendLine("Failed to read logcat")
        }

        return sb.toString()
    }
}
