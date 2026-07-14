package com.neomods.tools.encoding

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.tools.encoding.Base64Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DecodedContentType { TEXT, IMAGE, BINARY }

data class DecodedResult(
    val text: String?,
    val bytes: ByteArray,
    val contentType: DecodedContentType,
    val fileName: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedResult) return false
        return text == other.text && contentType == other.contentType
    }
    override fun hashCode(): Int = text.hashCode() * 31 + contentType.hashCode()
}

class Base64DecoderViewModel(application: Application) : AndroidViewModel(application) {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _result = MutableStateFlow<DecodedResult?>(null)
    val result: StateFlow<DecodedResult?> = _result.asStateFlow()

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding: StateFlow<Boolean> = _isDecoding.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun updateInput(text: String) {
        _inputText.value = text
        if (text.isBlank()) _result.value = null
    }

    fun decode() {
        val input = _inputText.value.trim()
        if (input.isBlank()) {
            _toast.value = "Paste a Base64 string first"
            return
        }

        viewModelScope.launch {
            _isDecoding.value = true
            val decoded = withContext(Dispatchers.IO) {
                runCatching { Base64Utils.decode(input) }.getOrNull()
            }
            _isDecoding.value = false

            if (decoded == null || decoded.isEmpty()) {
                _toast.value = "Invalid Base64 input"
                _result.value = null
                return@launch
            }

            val contentType = detectContentType(decoded)
            val resultText = when (contentType) {
                DecodedContentType.TEXT -> decoded.toString(Charsets.UTF_8)
                else -> null
            }

            _result.value = DecodedResult(
                text = resultText,
                bytes = decoded,
                contentType = contentType,
                fileName = null
            )
        }
    }

    fun loadFromFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val cr = getApplication<Application>().contentResolver
            var name = "base64.txt"
            runCatching {
                cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) name = cursor.getString(0) ?: name
                    }
            }
            val bytes = runCatching {
                cr.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) {
                val text = bytes.toString(Charsets.UTF_8)
                _inputText.value = text
                _toast.value = "Loaded $name"
            }
        }
    }

    fun copyDecoded() {
        val r = _result.value ?: return
        val text = r.text ?: r.bytes.toString(Charsets.ISO_8859_1)
        val maxClipboardBytes = 512 * 1024 // ~512KB safe limit for Binder transactions
        if (text.toByteArray(Charsets.UTF_8).size > maxClipboardBytes) {
            notify("Content too large to copy to clipboard")
            return
        }
        val cm = getApplication<Application>()
            .getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("decoded", text))
        notify("Decoded content copied")
    }

    fun getDecodedBytes(): ByteArray? = _result.value?.bytes

    fun notify(message: String) {
        _toast.value = message
    }

    fun clearToast() {
        _toast.value = null
    }

    private fun detectContentType(bytes: ByteArray): DecodedContentType {
        if (bytes.size >= 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            val b2 = bytes[2].toInt() and 0xFF
            val b3 = bytes[3].toInt() and 0xFF
            // PNG: 89 50 4E 47
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return DecodedContentType.IMAGE
            // JPEG: FF D8 FF
            if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return DecodedContentType.IMAGE
            // GIF: 47 49 46 38
            if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return DecodedContentType.IMAGE
            // WebP: 52 49 46 46 ... 57 45 42 50
            if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 && bytes.size >= 12) {
                val b8 = bytes[8].toInt() and 0xFF
                val b9 = bytes[9].toInt() and 0xFF
                val b10 = bytes[10].toInt() and 0xFF
                val b11 = bytes[11].toInt() and 0xFF
                if (b8 == 0x57 && b9 == 0x45 && b10 == 0x42 && b11 == 0x50) return DecodedContentType.IMAGE
            }
            // BMP: 42 4D
            if (b0 == 0x42 && b1 == 0x4D) return DecodedContentType.IMAGE
        }
        // Try UTF-8
        return try {
            bytes.toString(Charsets.UTF_8)
            DecodedContentType.TEXT
        } catch (_: Exception) {
            DecodedContentType.BINARY
        }
    }
}
