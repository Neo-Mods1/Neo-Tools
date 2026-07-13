package com.neomods.tools.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
 * Lets the user copy the crash log or restart the app, and (when the user has
 * previously opted in) sends an anonymous report via [CrashReporter].
 *
 * Ported from the CODE-IDE crash system (com.neo.ide.crash.CrashActivity). also owned by me neo mods
 */
class CrashActivity : BaseActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var reportSwitch: MaterialSwitch
    private lateinit var reportStatus: TextView
    private var fullLog: String = ""
    private var crashMessage: String = ""
    private var exceptionType: String = ""
    private var stacktrace: String = ""
    private var crashThread: String = ""

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

        copyButton.setOnClickListener { copyLogToClipboard() }
        restartButton.setOnClickListener { restartApp() }

        loadingText.visibility = View.GONE
        logTextView.text = fullLog

        val optedIn = CrashReporter.isOptedIn(this)
        if (optedIn) {
            reportSwitch.visibility = View.VISIBLE
            reportSwitch.isChecked = true
            sendReport()
        } else {
            reportSwitch.visibility = View.VISIBLE
            reportSwitch.isChecked = false
            reportSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) sendReport()
            }
        }
    }

    private fun sendReport() {
        reportStatus.visibility = View.VISIBLE
        reportStatus.text = "Sending crash report..."

        CrashReporter.reportCrash(
            this,
            exceptionType,
            crashMessage,
            stacktrace,
            crashThread,
            fullLog
        )

        reportStatus.text = "Report sent"
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
