package com.neomods.tools.encoding

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.tools.native.NeoNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Immutable description of a file the user selected.
 *
 * @param id      Local stable id (independent of the content [uri], which can
 *                be re-used across sessions).
 * @param uri     Content URI of the selected file.
 * @param name    Display name (from [OpenableColumns.DISPLAY_NAME]).
 * @param mimeType Resolved MIME type (from [android.content.ContentResolver.getType]).
 * @param size    File size in bytes (0 when unknown).
 */
data class SelectedFile(
    val id: String,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

/**
 * Result of encoding a [SelectedFile] to Base64.
 *
 * @param base64       Single-line Base64 text (no newlines/spaces).
 * @param encodedSize  Length of [base64] in characters (for display).
 */
data class Base64Entry(
    val id: String,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val base64: String,
    val encodedSize: Int
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

/**
 * Drives the Base64 Encoder tool.
 *
 * Architecture:
 *  - Inputs come from the UI through [addFiles] (AndroidX Activity Result URIs).
 *  - Encoding is delegated to the native layer ([NeoNative.encodeBase64]),
 *    executed on [Dispatchers.IO] so the UI stays responsive.
 *  - Results are cached by URI for the lifetime of the screen to avoid
 *    re-encoding the same file.
 *
 * The ViewModel keeps no Compose types, so it can be reused or moved behind a
 * use-case later without UI changes.
 */
class Base64EncoderViewModel(application: Application) : AndroidViewModel(application) {

    private val _selected = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selected: StateFlow<List<SelectedFile>> = _selected.asStateFlow()

    private val _entries = MutableStateFlow<List<Base64Entry>>(emptyList())
    val entries: StateFlow<List<Base64Entry>> = _entries.asStateFlow()

    private val _isEncoding = MutableStateFlow(false)
    val isEncoding: StateFlow<Boolean> = _isEncoding.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** URI -> cached entry. Guarded by [cacheLock] for concurrent [addFiles]. */
    private val cache = LinkedHashMap<String, Base64Entry>()
    private val cacheLock = Mutex()

    /** Register newly picked files and start background encoding for new ones. */
    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val resolved = uris.mapNotNull { resolveMeta(it) }
        if (resolved.isEmpty()) return

        _selected.update { it + resolved }
        encodeNew(resolved)
    }

    private fun resolveMeta(uri: Uri): SelectedFile? {
        val cr = getApplication<Application>().contentResolver
        var name = "file"
        var size = 0L
        runCatching {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0) ?: name
                        size = cursor.getLong(1)
                    }
                }
        }
        val mime = cr.getType(uri) ?: "application/octet-stream"
        return SelectedFile(UUID.randomUUID().toString(), uri, name, mime, size)
    }

    private fun encodeNew(files: List<SelectedFile>) {
        viewModelScope.launch(Dispatchers.IO) {
            val todo = cacheLock.withLock {
                files.filter { cache[it.uri.toString()] == null }
            }
            if (todo.isEmpty()) return@launch

            val produced = todo.map { sf ->
                cacheLock.withLock {
                    cache.getOrPut(sf.uri.toString()) {
                        val bytes = readBytes(sf.uri)
                        val b64 = NeoNative.encodeBase64(bytes)
                        Base64Entry(sf.id, sf.uri, sf.name, sf.mimeType, b64, b64.length)
                    }
                }
            }

            _entries.update { it + produced }
        }
    }

    private fun readBytes(uri: Uri): ByteArray {
        val cr = getApplication<Application>().contentResolver
        return runCatching {
            cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        }.getOrDefault(ByteArray(0))
    }

    /** Copy a Base64 entry's text to the system clipboard. */
    fun copyBase64(entry: Base64Entry) {
        val cm = getApplication<Application>()
            .getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText(entry.name, entry.base64))
        notify("Base64 copied to clipboard")
    }

    fun clearToast() {
        _toast.value = null
    }

    fun notify(message: String) {
        _toast.value = message
    }

    /** Drop one selected file (and its cached result) from the screen. */
    fun removeFile(id: String) {
        _selected.update { list -> list.filter { it.id != id } }
        viewModelScope.launch {
            cacheLock.withLock {
                val uri = _entries.value.firstOrNull { it.id == id }?.uri?.toString()
                if (uri != null) cache.remove(uri)
            }
            _entries.update { list -> list.filter { it.id != id } }
        }
    }
}
