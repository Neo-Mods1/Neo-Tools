package com.neomods.tools.encoding

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neomods.tools.encoding.CppConverterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CppConverterViewModel(application: Application) : AndroidViewModel(application) {

    // --- Generator state ---

    private val _inputBytes = MutableStateFlow<ByteArray?>(null)
    val inputBytes: StateFlow<ByteArray?> = _inputBytes.asStateFlow()

    private val _inputFileName = MutableStateFlow("")
    val inputFileName: StateFlow<String> = _inputFileName.asStateFlow()

    private val _generatedHeader = MutableStateFlow<String?>(null)
    val generatedHeader: StateFlow<String?> = _generatedHeader.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // --- Decoder state ---

    private val _decodedBytes = MutableStateFlow<ByteArray?>(null)
    val decodedBytes: StateFlow<ByteArray?> = _decodedBytes.asStateFlow()

    private val _decodedFileName = MutableStateFlow("")
    val decodedFileName: StateFlow<String> = _decodedFileName.asStateFlow()

    private val _headerText = MutableStateFlow("")
    val headerText: StateFlow<String> = _headerText.asStateFlow()

    private val _isDecoding = MutableStateFlow(false)
    val isDecoding: StateFlow<Boolean> = _isDecoding.asStateFlow()

    // --- Shared ---

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun notify(message: String) {
        _toast.value = message
    }

    fun clearToast() {
        _toast.value = null
    }

    // --- Generator methods ---

    fun loadInputFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val cr = getApplication<Application>().contentResolver
            var name = "unknown_file"
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
                _inputBytes.value = bytes
                _inputFileName.value = name
                _generatedHeader.value = null
                _toast.value = "Loaded $name (${formatSize(bytes.size.toLong())})"
            } else {
                _toast.value = "Failed to load file"
            }
        }
    }

    fun generateHeader() {
        val bytes = _inputBytes.value
        if (bytes == null || bytes.isEmpty()) {
            _toast.value = "Select a file first"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            val header = withContext(Dispatchers.IO) {
                runCatching { CppConverterUtils.fileToHeader(bytes, _inputFileName.value) }.getOrNull()
            }
            _isGenerating.value = false

            if (header.isNullOrBlank()) {
                _toast.value = "Failed to generate header"
                _generatedHeader.value = null
            } else {
                _generatedHeader.value = header
            }
        }
    }

    fun getGeneratedHeader(): String? = _generatedHeader.value

    // --- Decoder methods ---

    fun loadHeaderFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val cr = getApplication<Application>().contentResolver
            val bytes = runCatching {
                cr.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) {
                val text = bytes.toString(Charsets.UTF_8)
                _headerText.value = text
                _decodedBytes.value = null
                _decodedFileName.value = ""
                _toast.value = "Header loaded"
            } else {
                _toast.value = "Failed to load header file"
            }
        }
    }

    fun decodeHeader() {
        val header = _headerText.value.trim()
        if (header.isEmpty()) {
            _toast.value = "Load a .hpp header first"
            return
        }

        viewModelScope.launch {
            _isDecoding.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching { CppConverterUtils.headerToFile(header) }.getOrNull()
            }
            val fname = withContext(Dispatchers.IO) {
                runCatching { CppConverterUtils.headerFileName(header) }.getOrNull()
            }
            _isDecoding.value = false

            if (result == null || result.isEmpty()) {
                _toast.value = "Invalid or empty header"
                _decodedBytes.value = null
            } else {
                _decodedBytes.value = result
                _decodedFileName.value = fname ?: "decoded_output"
            }
        }
    }

    fun getDecodedBytes(): ByteArray? = _decodedBytes.value
    fun getDecodedFileName(): String = _decodedFileName.value

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "--"
        val kb = bytes / 1024.0
        return if (kb < 1024) "%.1f KB".format(kb) else "%.2f MB".format(kb / 1024)
    }
}
