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

    // ── Encoding ────────────────────────────────────────────────────────

    external fun encodeBase64(input: ByteArray): String
    external fun decodeBase64(input: String): ByteArray
    external fun fileToHeader(data: ByteArray, filename: String): String
    external fun headerToFile(header: String): ByteArray
    external fun headerFileName(header: String): String

    // ── Native Crash Detection ──────────────────────────────────────────

    external fun nativeCheckCrashFile(): String?

    // ── APK Tools ──────────────────────────────────────────────────────

    external fun nativeParseApkInfo(apkPath: String): String
    external fun nativeParseManifest(apkPath: String): String
    external fun nativeGetManifestXml(apkPath: String): String
    external fun nativeParseCertificate(apkPath: String): String
    external fun nativeGetNativeLibs(apkPath: String): String
    external fun nativeGetZipEntries(apkPath: String): String
}
