package com.neomods.tools.encoding

object CppConverterUtils {

    private val hexDigits = "0123456789abcdef".toCharArray()

    fun fileToHeader(data: ByteArray, filename: String): String {
        if (data.isEmpty()) return ""

        val safeName = buildString {
            for (c in filename) {
                append(if (c.isLetterOrDigit()) c else '_')
            }
            if (isEmpty()) append("file")
        }

        return buildString {
            append("#pragma once\n#include <cstdint>\n#include <cstddef>\n\n")
            append("inline constexpr unsigned char ${safeName}_data[] = {\n")

            var i = 0
            while (i < data.size) {
                val lineEnd = minOf(i + 16, data.size)
                for (j in i until lineEnd) {
                    val b = data[j].toInt() and 0xFF
                    append("0x${hexDigits[b shr 4]}${hexDigits[b and 0x0F]}, ")
                }
                // Remove trailing ", " from last byte, add comma + newline
                if (lineEnd > i) {
                    delete(length - 2, length)
                    append(",\n")
                }
                i = lineEnd
            }

            append("};\n\n")
            append("inline constexpr std::size_t ${safeName}_size = ${data.size};\n\n")
            append("inline constexpr const char ${safeName}_name[] = \"$filename\";\n")
        }
    }

    fun headerToFile(header: String): ByteArray {
        val arrayStart = header.indexOf("_data[]")
            .let { if (it < 0) return ByteArray(0) else header.indexOf('{', it) }
            .let { if (it < 0) return ByteArray(0) else it + 1 }
        val arrayEnd = header.indexOf('}', arrayStart)
        if (arrayEnd < 0) return ByteArray(0)

        val bytes = mutableListOf<Byte>()
        var pos = arrayStart
        while (pos < arrayEnd) {
            // Find '0'
            while (pos < arrayEnd && header[pos] != '0') pos++
            if (pos >= arrayEnd) break

            // Check for 0x prefix
            if (pos + 1 >= arrayEnd || (header[pos + 1] != 'x' && header[pos + 1] != 'X')) {
                pos++
                continue
            }

            // Parse hex digits after 0x
            pos += 2
            var value = 0
            var hexCount = 0
            while (pos < arrayEnd && hexCount < 2) {
                val c = header[pos]
                val nibble = when {
                    c in '0'..'9' -> c - '0'
                    c in 'a'..'f' -> 10 + (c - 'a')
                    c in 'A'..'F' -> 10 + (c - 'A')
                    else -> break
                }
                value = (value shl 4) or nibble
                hexCount++
                pos++
            }
            if (hexCount == 2) {
                bytes.add(value.toByte())
            }
        }

        return bytes.toByteArray()
    }

    fun headerFileName(header: String): String {
        val namePos = header.indexOf("_name[]")
        if (namePos < 0) return ""

        val eqPos = header.indexOf('=', namePos)
        if (eqPos < 0) return ""

        val quoteStart = header.indexOf('"', eqPos)
        if (quoteStart < 0) return ""

        val quoteEnd = header.indexOf('"', quoteStart + 1)
        if (quoteEnd < 0) return ""

        return header.substring(quoteStart + 1, quoteEnd)
    }
}
