package com.neomods.tools.apk

import mt.modder.hub.axml.AXMLPrinter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.cert.CertificateFactory
import java.util.zip.ZipFile

object ApkParser {

    fun parseApkInfo(apkPath: String): String {
        val file = File(apkPath)
        if (!file.exists()) return "{}"

        val zip = try { ZipFile(file) } catch (_: Exception) { return "{}" }
        val entries = zip.entries().toList()
        zip.close()

        val json = JSONObject()
        json.put("apkSize", file.length())
        json.put("apkSizeFormatted", formatSize(file.length()))
        json.put("totalEntries", entries.size)
        json.put("hasManifest", entries.any { it.name == "AndroidManifest.xml" })
        json.put("hasResources", entries.any { it.name == "resources.arsc" })

        // Count DEX files
        json.put("dexCount", entries.count { it.name.endsWith(".dex") })

        // Native libs
        val nativeLibs = entries.filter {
            it.name.startsWith("lib/") && it.name.endsWith(".so")
        }
        json.put("nativeLibs", JSONArray(nativeLibs.map { it.name }))
        json.put("nativeLibsCount", nativeLibs.size)

        // ABI detection
        val abis = nativeLibs.map {
            val parts = it.name.split("/")
            if (parts.size >= 2) parts[1] else "unknown"
        }.distinct()
        json.put("abis", JSONArray(abis))

        // 32-bit vs 64-bit .so count
        json.put("so32Count", nativeLibs.count {
            it.name.contains("/armeabi-v7a") || it.name.contains("/x86") && !it.name.contains("x86_64")
        })
        json.put("so64Count", nativeLibs.count {
            it.name.contains("/arm64-v8a") || it.name.contains("/x86_64")
        })

        // Debug symbols (.so with .debug_ext or unstripped)
        json.put("hasDebugSymbols", nativeLibs.any { it.name.contains("debug") })

        // Signing schemes
        json.put("hasV1Signing", entries.any {
            it.name.startsWith("META-INF/") &&
                (it.name.endsWith(".SF") || it.name.endsWith(".RSA") || it.name.endsWith(".DSA"))
        })
        json.put("hasV2Signing", entries.any { it.name == "META-INF/v2-certificate-chain.bin" })
        json.put("hasV3Signing", entries.any { it.name == "META-INF/v3-certificate-chain.bin" })

        // Components from binary manifest
        val manifestData = entries.find { it.name == "AndroidManifest.xml" }?.let { entry ->
            try {
                val zip2 = ZipFile(file)
                val stream = zip2.getInputStream(entry)
                val data = stream.readBytes()
                stream.close()
                zip2.close()
                data
            } catch (_: Exception) { null }
        }

        if (manifestData != null) {
            val decoded = decodeBinaryXml(manifestData)
            json.put("activityCount", countTag(decoded, "activity"))
            json.put("serviceCount", countTag(decoded, "service"))
            json.put("receiverCount", countTag(decoded, "receiver"))
            json.put("providerCount", countTag(decoded, "provider"))
            json.put("permissionCount", countTag(decoded, "uses-permission"))
        }

        // Certificate info
        val certInfo = parseCertificates(file, entries)
        json.put("certificates", certInfo)

        return json.toString()
    }

    fun parseManifest(apkPath: String): String {
        val file = File(apkPath)
        if (!file.exists()) return ""

        val zip = try { ZipFile(file) } catch (_: Exception) { return "" }
        val entry = zip.entries().asSequence().find { it.name == "AndroidManifest.xml" }
            ?: return run { zip.close(); "" }

        val data = try {
            val stream = zip.getInputStream(entry)
            val bytes = stream.readBytes()
            stream.close()
            zip.close()
            bytes
        } catch (_: Exception) { zip.close(); return "" }

        return decodeBinaryXml(data)
    }

    fun getManifestXml(apkPath: String): String = parseManifest(apkPath)

    fun parseCertificate(apkPath: String): String {
        val file = File(apkPath)
        if (!file.exists()) return "[]"

        val zip = try { ZipFile(file) } catch (_: Exception) { return "[]" }
        val entries = zip.entries().toList()
        val result = parseCertificates(file, entries)
        zip.close()
        return result.toString()
    }

    fun getNativeLibs(apkPath: String): String {
        val file = File(apkPath)
        if (!file.exists()) return "[]"

        val zip = try { ZipFile(file) } catch (_: Exception) { return "[]" }
        val libs = zip.entries().asSequence()
            .filter { it.name.startsWith("lib/") && it.name.endsWith(".so") }
            .map { it.name }
            .toList()
        zip.close()
        return JSONArray(libs).toString()
    }

    fun getZipEntries(apkPath: String): String {
        val file = File(apkPath)
        if (!file.exists()) return "[]"

        val zip = try { ZipFile(file) } catch (_: Exception) { return "[]" }
        val arr = JSONArray()
        zip.entries().asSequence().forEach { entry ->
            val obj = JSONObject()
            obj.put("name", entry.name)
            obj.put("size", entry.size)
            obj.put("compressedSize", entry.compressedSize)
            obj.put("isDirectory", entry.isDirectory)
            arr.put(obj)
        }
        zip.close()
        return arr.toString()
    }

    // ── Private helpers ────────────────────────────────────────────────

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "%.1f KB".format(bytes / 1024.0)
        if (bytes < 1024L * 1024 * 1024) return "%.1f MB".format(bytes / (1024.0 * 1024))
        return "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }

    private fun countTag(xml: String, tag: String): Int {
        return Regex("""<$tag[\s>]""").findAll(xml).count()
    }

    private fun parseCertificates(file: File, entries: List<java.util.zip.ZipEntry>): JSONArray {
        val arr = JSONArray()
        val cf = try { CertificateFactory.getInstance("X.509") } catch (_: Exception) { return arr }

        val certFiles = entries.filter {
            it.name.startsWith("META-INF/") &&
                (it.name.endsWith(".RSA") || it.name.endsWith(".DSA") || it.name.endsWith(".EC"))
        }

        for (certEntry in certFiles) {
            try {
                val zip = ZipFile(file)
                val stream = zip.getInputStream(certEntry)
                val certBytes = stream.readBytes()
                stream.close()
                zip.close()

                val cert = cf.generateCertificate(certBytes.inputStream())
                val x509 = cert as? java.security.cert.X509Certificate
                val info = JSONObject()
                info.put("file", certEntry.name)
                info.put("subject", x509?.subjectX500Principal?.name ?: "")
                info.put("issuer", x509?.issuerX500Principal?.name ?: "")
                info.put("serial", x509?.serialNumber?.toString(16) ?: "")
                info.put("sigAlgorithm", x509?.sigAlgName ?: "")
                info.put("notBefore", x509?.notBefore?.toString() ?: "")
                info.put("notAfter", x509?.notAfter?.toString() ?: "")
                info.put("sha256", cert.getEncoded()?.let { sha256Hex(it) } ?: "")
                info.put("sha1", cert.getEncoded()?.let { sha1Hex(it) } ?: "")
                arr.put(info)
            } catch (_: Exception) { }
        }
        return arr
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02X".format(it) }
    }

    private fun sha1Hex(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1").digest(data)
        return digest.joinToString("") { "%02X".format(it) }
    }

    // ── Binary XML decoder ─────────────────────────────────────────────

    private fun decodeBinaryXml(data: ByteArray): String {
        if (data.size < 8) return ""
        return try {
            val printer = AXMLPrinter()
            printer.convertXml(data) ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
