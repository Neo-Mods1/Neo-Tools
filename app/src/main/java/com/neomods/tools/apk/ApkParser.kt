package com.neomods.tools.apk

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
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
                val info = JSONObject()
                info.put("file", certEntry.name)
                info.put("subject", cert.subjectX500Principal?.name ?: "")
                info.put("issuer", cert.issuerX500Principal?.name ?: "")
                info.put("serial", cert.serialNumber?.toString(16) ?: "")
                info.put("sigAlgorithm", cert.sigAlgName ?: "")
                info.put("notBefore", cert.notBefore?.toString() ?: "")
                info.put("notAfter", cert.notAfter?.toString() ?: "")
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

        val parser = BinaryXmlParser(data)
        return try {
            parser.parse()
        } catch (_: Exception) {
            ""
        }
    }

    private class BinaryXmlParser(private val data: ByteArray) {
        private var pos = 0
        private val strings = mutableListOf<String>()
        private val resourceIds = mutableListOf<Int>()
        private val output = StringBuilder()
        private var indent = 0

        private fun u16(): Int {
            if (pos + 2 > data.size) return 0
            val v = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
            pos += 2
            return v
        }

        private fun u8(): Int {
            if (pos >= data.size) return 0
            return data[pos++].toInt() and 0xFF
        }

        private fun u32(): Long {
            if (pos + 4 > data.size) return 0
            val v = (data[pos].toLong() and 0xFF) or
                ((data[pos + 1].toLong() and 0xFF) shl 8) or
                ((data[pos + 2].toLong() and 0xFF) shl 16) or
                ((data[pos + 3].toLong() and 0xFF) shl 24)
            pos += 4
            return v
        }

        fun parse(): String {
            if (data.size < 8) return ""

            // File header
            u16() // type (0x0003)
            u16() // headerSize (8)
            u32() // chunkSize

            // String pool
            parseStringPool()

            // Resource ID map (optional)
            val saved = pos
            val t = u16()
            val hs = u16()
            val cs = u32()
            if (t == 0x0180) {
                val count = (cs.toInt() / 4) - 2
                for (i in 0 until count) {
                    resourceIds.add(u32().toInt())
                }
            } else {
                pos = saved
            }

            output.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            parseXmlNode()
            return output.toString()
        }

        private fun parseStringPool() {
            if (pos + 28 > data.size) return
            u16() // chunk type
            u16() // headerSize
            val chunkSize = u32().toInt()
            val stringCount = u32().toInt()
            u32() // styleCount
            val flags = u32().toInt()
            val stringsStart = u32().toInt()
            u32() // stylesStart

            val isUtf8 = (flags and (1 shl 8)) != 0
            val savedPos = pos
            pos = savedPos + stringsStart

            strings.clear()
            for (i in 0 until stringCount) {
                if (pos >= data.size) break
                if (isUtf8) {
                    var charLen = data[pos++].toInt() and 0xFF
                    if (charLen and 0x80 != 0) {
                        pos++
                        charLen = ((charLen and 0x7F) shl 8) or (data[pos++].toInt() and 0xFF)
                    }
                    var byteLen = data[pos++].toInt() and 0xFF
                    if (byteLen and 0x80 != 0) {
                        byteLen = ((byteLen and 0x7F) shl 8) or (data[pos++].toInt() and 0xFF)
                    }
                    if (pos + byteLen <= data.size) {
                        strings.add(String(data, pos, byteLen, Charsets.UTF_8))
                        pos += byteLen + 1
                    } else {
                        strings.add("")
                    }
                } else {
                    var charLen = u16()
                    if (charLen and 0x8000 != 0) {
                        charLen = ((charLen and 0x7FFF) shl 16) or u16()
                    }
                    val byteLen = charLen * 2
                    if (pos + byteLen <= data.size) {
                        val sb = StringBuilder(charLen)
                        for (c in 0 until charLen) {
                            val ch = (data[pos + c * 2].toInt() and 0xFF) or
                                ((data[pos + c * 2 + 1].toInt() and 0xFF) shl 8)
                            sb.append(if (ch < 128) ch.toChar() else '?')
                        }
                        strings.add(sb.toString())
                        pos += byteLen + 2
                    } else {
                        strings.add("")
                    }
                }
            }
            pos = savedPos + chunkSize
        }

        private fun parseXmlNode() {
            while (pos < data.size) {
                val type = u16()
                val headerSize = u16()
                val chunkSize = u32().toInt()

                when (type) {
                    0x0100 -> { // START_NAMESPACE
                        u32(); u32(); u32(); u32()
                    }
                    0x0101 -> { // END_NAMESPACE
                        u32(); u32(); u32(); u32()
                    }
                    0x0102 -> { // START_ELEMENT
                        u32(); u32() // lineNum, comment
                        val nsIdx = u32().toInt()
                        val nameIdx = u32().toInt()
                        val attrCount = u16()
                        u16(); u16(); u16() // idIdx, classIdx, styleIdx

                        val name = if (nameIdx < strings.size) strings[nameIdx] else "???"
                        output.append(indentStr()).append("<").append(name)

                        for (i in 0 until attrCount) {
                            u32() // attrNs
                            val attrName = u32().toInt()
                            val attrRawValue = u32().toInt()
                            u16() // size
                            u8() // res0
                            val attrType = u16()
                            val attrData = u32().toInt()

                            val aName = if (attrName < strings.size) strings[attrName] else "???"
                            val aRaw = if (attrRawValue < strings.size) strings[attrRawValue] else ""
                            val aVal = formatAttrValue(attrType, attrData, aRaw)
                            output.append(" ").append(aName).append("=").append(aVal)
                        }
                        output.append(">\n")
                        indent++
                        return
                    }
                    0x0103 -> { // END_ELEMENT
                        u32(); u32()
                        u32() // nsIdx
                        val nameIdx = u32().toInt()
                        val name = if (nameIdx < strings.size) strings[nameIdx] else "???"
                        indent--
                        output.append(indentStr()).append("</").append(name).append(">\n")
                        return
                    }
                    0x0104 -> { // TEXT
                        u32(); u32()
                        val dataIdx = u32().toInt()
                        if (dataIdx < strings.size) {
                            output.append(indentStr()).append(xmlEscape(strings[dataIdx])).append("\n")
                        }
                    }
                    0x0000 -> return // END
                    else -> {
                        if (chunkSize > 8) pos += chunkSize - 8
                    }
                }
            }
        }

        private fun formatAttrValue(type: Int, data: Int, rawValue: String): String {
            if (rawValue.isNotEmpty()) return "\"${xmlEscape(rawValue)}\""
            return when (type) {
                0x03 -> "\"${xmlEscape(if (data < strings.size) strings[data] else "")}\""
                0x14, 0x15 -> data.toString()
                0x00 -> "null"
                else -> if (data < strings.size) "\"${xmlEscape(strings[data])}\"" else "0x%08X".format(data)
            }
        }

        private fun indentStr(): String = "  ".repeat(indent)

        private fun xmlEscape(s: String): String = buildString {
            for (c in s) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(c)
                }
            }
        }
    }
}
