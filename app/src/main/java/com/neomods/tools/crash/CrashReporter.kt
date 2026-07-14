package com.neomods.tools.crash

import android.content.Context
import android.os.Build
import android.util.Log
import com.neomods.tools.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Opt-in, anonymous crash reporter.
 *
 * Sends a JSON payload to the Neo Crash System backend at
 * https://neo-crash-system.vercel.app/api/report.
 *
 * The API only requires `exceptionType` and `stacktrace` — all other fields
 * are optional metadata. On success the backend sends the report to Telegram.
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val API_URL = "https://neo-crash-system.vercel.app/api/report"
    private const val PREFS = "crash_reporter"
    private const val KEY_OPTED_IN = "opted_in"
    private const val KEY_SENT_HASHES = "sent_hashes"

    private val connectTimeoutMs = TimeUnit.SECONDS.toMillis(15).toInt()
    private val readTimeoutMs = TimeUnit.SECONDS.toMillis(15).toInt()

    /** Result of a single report attempt. */
    data class ReportResult(val success: Boolean, val message: String)

    fun isOptedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_OPTED_IN, false)
    }

    fun setOptedIn(context: Context, optedIn: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OPTED_IN, optedIn).apply()
    }

    /**
     * Synchronous crash report — blocks the calling thread until the network
     * call completes (or times out). Returns [ReportResult] indicating
     * success/failure. Must NOT be called on the main thread.
     */
    fun reportCrashSync(
        context: Context,
        exceptionType: String,
        message: String,
        stacktrace: String,
        thread: String
    ): ReportResult {
        if (!isOptedIn(context)) {
            return ReportResult(false, "User has not opted in")
        }

        val hash = computeHash(exceptionType, message, stacktrace)
        if (hasSentBefore(context, hash)) {
            return ReportResult(true, "Already sent")
        }

        return try {
            val body = buildPayload(exceptionType, message, stacktrace, thread)
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
            }

            connection.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val responseBody = try {
                connection.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                connection.errorStream?.bufferedReader()?.readText() ?: ""
            }
            connection.disconnect()

            if (code in 200..299) {
                markSent(context, hash)
                Log.d(TAG, "Report sent successfully (HTTP $code)")
                ReportResult(true, "Report sent")
            } else {
                Log.w(TAG, "Report failed with HTTP $code: $responseBody")
                ReportResult(false, "Server error: HTTP $code")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Report failed: ${e.message}")
            ReportResult(false, "Network error: ${e.message}")
        }
    }

    /**
     * Fire-and-forget async report — runs [reportCrashSync] on a background
     * thread. Use when you don't need the result (e.g. from CrashHandler).
     */
    fun reportCrash(
        context: Context,
        exceptionType: String,
        message: String,
        stacktrace: String,
        thread: String
    ) {
        Thread {
            reportCrashSync(context, exceptionType, message, stacktrace, thread)
        }.start()
    }

    private fun buildPayload(
        exceptionType: String,
        message: String,
        stacktrace: String,
        thread: String
    ): String {
        val json = JSONObject()
        json.put("appName", "Neo Tools")
        json.put("packageName", BuildConfig.APPLICATION_ID)
        json.put("versionName", BuildConfig.VERSION_NAME)
        json.put("versionCode", BuildConfig.VERSION_CODE.toString())
        json.put("androidVersion", Build.VERSION.RELEASE)
        json.put("sdk", Build.VERSION.SDK_INT.toString())
        json.put("manufacturer", Build.MANUFACTURER)
        json.put("brand", Build.BRAND)
        json.put("model", Build.MODEL)
        json.put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
        json.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()))
        json.put("crashType", "UNCAUGHT")
        json.put("exceptionType", exceptionType)
        json.put("message", message)
        json.put("stacktrace", stacktrace)
        json.put("thread", thread)
        json.put("occurrenceHash", computeHash(exceptionType, message, stacktrace))
        return json.toString()
    }

    private fun computeHash(exceptionType: String, message: String, stacktrace: String): String {
        val frames = stacktrace.lines()
            .filter { it.trim().startsWith("at ") }
            .take(10)
            .joinToString("\n")
        val input = "$exceptionType|$message|$frames"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hasSentBefore(context: Context, hash: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hashes = prefs.getStringSet(KEY_SENT_HASHES, emptySet()) ?: emptySet()
        return hash in hashes
    }

    private fun markSent(context: Context, hash: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hashes = prefs.getStringSet(KEY_SENT_HASHES, emptySet())?.toMutableSet() ?: mutableSetOf()
        hashes.add(hash)
        prefs.edit().putStringSet(KEY_SENT_HASHES, hashes).apply()
    }
}
