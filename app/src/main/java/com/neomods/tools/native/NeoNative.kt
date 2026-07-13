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

    /**
     * Decode a standard Base64 string back to raw bytes.
     *
     * @param input a single continuous line of Base64 text.
     * @return the decoded raw bytes.
     */
    external fun decodeBase64(input: String): ByteArray

    /**
     * Convert raw file bytes to a C++ header (.hpp) string.
     * Output matches xxd -i format with 16 bytes per line.
     *
     * @param data     raw file bytes.
     * @param filename original file name (e.g. "image.png").
     * @return the complete .hpp header text.
     */
    external fun fileToHeader(data: ByteArray, filename: String): String

    /**
     * Parse a .hpp header and extract the raw byte data.
     *
     * @param header the .hpp header text.
     * @return the decoded raw bytes.
     */
    external fun headerToFile(header: String): ByteArray

    /**
     * Extract the file name from a .hpp header.
     *
     * @param header the .hpp header text.
     * @return the file_name value from the header.
     */
    external fun headerFileName(header: String): String
}
