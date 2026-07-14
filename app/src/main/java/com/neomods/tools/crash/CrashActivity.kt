package com.neomods.tools.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.materialswitch.MaterialSwitch
import com.neomods.tools.R
import com.neomods.tools.app.BaseActivity

/**
 * Screen shown when an uncaught exception is captured by [CrashHandler].
 *
 * Flow:
 * 1. Setup UI immediately (message, log)
 * 2. Check prefs — if user opted in, auto-send report
 * 3. If not opted in, show switch so user can enable sending
 * 4. When switch toggled ON, send report and update status
 */
class CrashActivity : BaseActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var reportSwitch: MaterialSwitch
    private lateinit var reportStatus: TextView
    private val mainHandler = Handler(Looper.getMainLooper())

    private var fullLog: String = ""
    private var crashMessage: String = ""
    private var exceptionType: String = ""
    private var stacktrace: String = ""
    private var crashThread: String = ""
    private var reportSent = false

    override fun bindLayout(): View {
        return layoutInflater.inflate(R.layout.activity_crash, null)
    }

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"
        const val EXTRA_CRASH_MESSAGE = "crash_message"
        const val EXTRA_EXCEPTION_TYPE = "exception_type"
        const val EXTRA_STACKTRACE = "stacktrace"
        const val EXTRA_THREAD = "crash_thread"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Clear crash-loop flag so CrashHandler can show CrashActivity again
        // if a NEW crash occurs later.
        getSharedPreferences("crash_handler", Context.MODE_PRIVATE)
            .edit().putBoolean("is_crashing", false).apply()

        // ── Step 1: Setup UI ──────────────────────────────────────────
        val messageText = findViewById<TextView>(R.id.crash_message)
        val loadingText = findViewById<TextView>(R.id.crash_loading)
        scrollView = findViewById(R.id.crash_scroll_view)
        logTextView = findViewById(R.id.crash_log_text)
        reportSwitch = findViewById(R.id.crash_send_report)
        reportStatus = findViewById(R.id.crash_report_status)
        val copyButton = findViewById<Button>(R.id.crash_copy_btn)
        val restartButton = findViewById<Button>(R.id.crash_restart_btn)

        crashMessage = intent.getStringExtra(EXTRA_CRASH_MESSAGE) ?: "Unknown error occurred"
        messageText.text = crashMessage

        fullLog = intent.getStringExtra(EXTRA_CRASH_LOG) ?: "No crash log available"
        exceptionType = intent.getStringExtra(EXTRA_EXCEPTION_TYPE) ?: extractExceptionType(fullLog)
        stacktrace = intent.getStringExtra(EXTRA_STACKTRACE) ?: extractStacktrace(fullLog)
        crashThread = intent.getStringExtra(EXTRA_THREAD) ?: "main"

        logTextView.text = fullLog
        loadingText.visibility = View.GONE

        copyButton.setOnClickListener { copyLogToClipboard() }
        restartButton.setOnClickListener { restartApp() }

        // ── Step 2: Check prefs and auto-report ──────────────────────
        reportSwitch.visibility = View.VISIBLE

        val optedIn = CrashReporter.isOptedIn(this)
        if (optedIn) {
            // User already opted in — auto-send immediately
            reportSwitch.isChecked = true
            sendReport()
        } else {
            // Not opted in — show switch, send when user toggles ON
            reportSwitch.isChecked = false
            reportSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !reportSent) {
                    sendReport()
                }
            }
        }
    }

    private fun sendReport() {
        reportStatus.visibility = View.VISIBLE
        reportStatus.text = getString(R.string.crash_sending)

        Thread {
            val result = CrashReporter.reportCrashSync(
                this@CrashActivity,
                exceptionType,
                crashMessage,
                stacktrace,
                crashThread
            )
            reportSent = true

            mainHandler.post {
                if (result.success) {
                    reportStatus.text = getString(R.string.crash_report_sent)
                } else {
                    reportStatus.text = getString(R.string.crash_report_failed, result.message)
                }
            }
        }.start()
    }

    private fun extractExceptionType(log: String): String {
        val match = Regex("""(?m)^Type:\s*(.+)""").find(log)
        if (match != null) return match.groupValues[1].trim()
        val lines = log.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("Exception") || trimmed.contains("Error")) {
                return trimmed.substringBefore(":").substringBefore(" ").trim()
            }
        }
        return "UnknownException"
    }

    private fun extractStacktrace(log: String): String {
        val stackStart = log.indexOf("at ")
        if (stackStart == -1) return log
        return log.substring(stackStart)
    }

    private fun copyLogToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Crash Log", fullLog)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        finish()
        Process.killProcess(Process.myPid())
    }
}
