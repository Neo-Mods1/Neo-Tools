package com.neomods.tools.crash

import android.content.Context
import android.os.Build
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
 * Sends a JSON payload to the crash backend. Reports are de-duplicated by a
 * hash of the exception signature so the same crash is not reported repeatedly.
 *
 * The original CODE-IDE implementation used OkHttp for the network call. That
 * dependency was replaced with [java.net.HttpURLConnection] here to keep the
 * APK small (the project explicitly avoids large third-party libraries) while
 * preserving the exact same payload and reporting behaviour.
 *
 * Ported from the CODE-IDE crash system (com.neo.ide.crash.CrashReporter). also owned by me neo mods
 */
object CrashReporter {

    private const val API_URL = "https://neo-crash-system.vercel.app/api/report"
    private const val PREFS = "crash_reporter"
    private const val KEY_OPTED_IN = "opted_in"
    private const val KEY_SENT_HASHES = "sent_hashes"

    private val connectTimeoutMs = TimeUnit.SECONDS.toMillis(10).toInt()
    private val readTimeoutMs = TimeUnit.SECONDS.toMillis(10).toInt()

    fun isOptedIn(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_OPTED_IN, false)
    }

    fun setOptedIn(context: Context, optedIn: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_OPTED_IN, optedIn).apply()
    }

    fun reportCrash(
        context: Context,
        exceptionType: String,
        message: String,
        stacktrace: String,
        thread: String,
        crashLog: String
    ) {
        if (!isOptedIn(context)) return

        val hash = computeHash(exceptionType, message, stacktrace)
        if (hasSentBefore(context, hash)) return

        Thread {
            try {
                val body = buildPayload(exceptionType, message, stacktrace, thread, crashLog)
                val url = URL(API_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                connection.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }
                val successful = runCatching { connection.responseCode in 200..299 }.getOrDefault(false)
                if (successful) {
                    markSent(context, hash)
                }
                connection.disconnect()
            } catch (_: Exception) {
                // Best-effort reporting: swallow network failures.
            }
        }.start()
    }

    private fun buildPayload(
        exceptionType: String,
        message: String,
        stacktrace: String,
        thread: String,
        crashLog: String
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
