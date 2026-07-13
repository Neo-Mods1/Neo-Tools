package com.neomods.tools.native

/**
 * Thin JNI caller for the native `neotools` library.
 *
 * The actual tool implementations live in C++ (`src/main/cpp`). Kotlin never
 * re-implements the algorithm — it only marshals inputs/outputs across the JNI
 * boundary so the heavy lifting stays in native code.
 */
object NeoNative {

    init {
        System.loadLibrary("neotools")
    }

    /**
     * Encode raw bytes to standard Base64.
     *
     * @return a single continuous line of Base64 (no line breaks, spaces or
     *         tabs), suitable for APIs, JSON and decoding.
     */
    external fun encodeBase64(input: ByteArray): String
}
