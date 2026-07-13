// ---------------------------------------------------------------------------
// Encryption & Decryption -> C++ Header Generator/Decoder.
//
// Converts arbitrary binary files to C++ header (.hpp) files containing the
// data as a constexpr unsigned char array. Output matches xxd -i format:
//   - 16 bytes per line
//   - 0x hex notation, comma-separated
//   - #pragma once, constexpr, file_size, file_name metadata
//
// The decoder parses a previously generated .hpp header and reconstructs
// the original binary file byte-for-byte.
//
// Optimized for speed: uses preallocated buffers, avoids per-byte allocations.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "encoding/cpp_converter.hpp"

#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <cctype>

namespace neotools {
namespace encoding {

// Fast hex lookup table
static const char kHexDigits[] = "0123456789abcdef";

jstring FileToHeader(JNIEnv* env, jobject /* thiz */, jbyteArray data, jstring filename) {
    if (data == nullptr) {
        return env->NewStringUTF("");
    }

    const jsize len = env->GetArrayLength(data);
    if (len == 0) {
        return env->NewStringUTF("");
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return env->NewStringUTF("");
    }

    const char* fname = "";
    if (filename != nullptr) {
        fname = env->GetStringUTFChars(filename, nullptr);
    }

    // Sanitize filename for C++ identifier
    std::string safeName;
    safeName.reserve(64);
    if (fname != nullptr && fname[0] != '\0') {
        for (const char* p = fname; *p; ++p) {
            char c = *p;
            if (std::isalnum(static_cast<unsigned char>(c))) {
                safeName += c;
            } else {
                safeName += '_';
            }
        }
    } else {
        safeName = "file";
    }

    const size_t total = static_cast<size_t>(len);

    // Pre-calculate output size to avoid reallocations
    // Header overhead ~200 bytes, each byte ~6 chars ("0xXX, "), 16 per line + newline
    size_t estimatedSize = 256 + (total * 6) + (total / 16 * 2) + 128;
    std::string hpp;
    hpp.reserve(estimatedSize);

    hpp += "#pragma once\n#include <cstdint>\n#include <cstddef>\n\n";
    hpp += "inline constexpr unsigned char ";
    hpp += safeName;
    hpp += "_data[] = {\n";

    // Hex lookup for fast conversion
    char lineBuf[128]; // 16 bytes * 7 chars + null = enough

    for (size_t i = 0; i < total; i += 16) {
        size_t lineLen = 0;
        const size_t lineEnd = std::min(i + 16, total);

        for (size_t j = i; j < lineEnd; ++j) {
            uint8_t b = static_cast<uint8_t>(bytes[j]);
            lineBuf[lineLen++] = '0';
            lineBuf[lineLen++] = 'x';
            lineBuf[lineLen++] = kHexDigits[b >> 4];
            lineBuf[lineLen++] = kHexDigits[b & 0x0F];
            lineBuf[lineLen++] = ',';
            lineBuf[lineLen++] = ' ';
        }
        // Remove trailing ", " from last byte on line
        if (lineLen >= 2) lineLen -= 2;
        lineBuf[lineLen++] = ',';
        lineBuf[lineLen++] = '\n';

        hpp.append(lineBuf, lineLen);
    }

    hpp += "};\n\n";
    hpp += "inline constexpr std::size_t ";
    hpp += safeName;
    hpp += "_size = ";
    hpp += std::to_string(total);
    hpp += ";\n\n";
    hpp += "inline constexpr const char ";
    hpp += safeName;
    hpp += "_name[] = \"";
    hpp += fname;
    hpp += "\";\n";

    if (filename != nullptr && fname != nullptr) {
        env->ReleaseStringUTFChars(filename, fname);
    }

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    return env->NewStringUTF(hpp.c_str());
}

// --- Decoder ---

jbyteArray HeaderToFile(JNIEnv* env, jobject /* thiz */, jstring header) {
    if (header == nullptr) {
        return env->NewByteArray(0);
    }

    const char* hdrChars = env->GetStringUTFChars(header, nullptr);
    if (hdrChars == nullptr) {
        return env->NewByteArray(0);
    }

    const size_t hdrLen = strlen(hdrChars);
    std::string hdr(hdrChars, hdrLen);
    env->ReleaseStringUTFChars(header, hdrChars);

    // Find byte array start: look for "_data[] = {"
    size_t arrayStart = hdr.find("_data[]");
    if (arrayStart == std::string::npos) {
        return env->NewByteArray(0);
    }
    arrayStart = hdr.find('{', arrayStart);
    if (arrayStart == std::string::npos) {
        return env->NewByteArray(0);
    }
    arrayStart++;

    // Find closing '}'
    size_t arrayEnd = hdr.find('}', arrayStart);
    if (arrayEnd == std::string::npos) {
        return env->NewByteArray(0);
    }

    // Parse hex bytes directly from string without creating substrings
    std::vector<uint8_t> bytes;
    bytes.reserve(arrayEnd - arrayStart);

    size_t pos = arrayStart;
    while (pos < arrayEnd) {
        // Skip to '0'
        while (pos < arrayEnd && hdr[pos] != '0') pos++;
        if (pos >= arrayEnd) break;

        // Check for 0x prefix
        if (pos + 1 >= arrayEnd || (hdr[pos + 1] != 'x' && hdr[pos + 1] != 'X')) {
            pos++;
            continue;
        }

        // Parse hex digits after 0x
        pos += 2;
        uint8_t val = 0;
        int hexCount = 0;
        while (pos < arrayEnd && hexCount < 2) {
            char c = hdr[pos];
            uint8_t nibble;
            if (c >= '0' && c <= '9') nibble = c - '0';
            else if (c >= 'a' && c <= 'f') nibble = 10 + (c - 'a');
            else if (c >= 'A' && c <= 'F') nibble = 10 + (c - 'A');
            else break;
            val = (val << 4) | nibble;
            hexCount++;
            pos++;
        }
        if (hexCount == 2) {
            bytes.push_back(val);
        }
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(bytes.size()));
    if (result != nullptr && !bytes.empty()) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(bytes.size()),
                                reinterpret_cast<jbyte*>(bytes.data()));
    }
    return result;
}

jstring HeaderFileName(JNIEnv* env, jobject /* thiz */, jstring header) {
    if (header == nullptr) {
        return env->NewStringUTF("");
    }

    const char* hdrChars = env->GetStringUTFChars(header, nullptr);
    if (hdrChars == nullptr) {
        return env->NewStringUTF("");
    }

    std::string hdr(hdrChars);
    env->ReleaseStringUTFChars(header, hdrChars);

    // Find _name[] = "..."  (any identifier before _name[])
    size_t namePos = hdr.find("_name[]");
    if (namePos == std::string::npos) {
        return env->NewStringUTF("");
    }

    size_t eqPos = hdr.find('=', namePos);
    if (eqPos == std::string::npos) {
        return env->NewStringUTF("");
    }

    size_t quoteStart = hdr.find('"', eqPos);
    if (quoteStart == std::string::npos) {
        return env->NewStringUTF("");
    }
    quoteStart++;

    size_t quoteEnd = hdr.find('"', quoteStart);
    if (quoteEnd == std::string::npos) {
        return env->NewStringUTF("");
    }

    std::string fname = hdr.substr(quoteStart, quoteEnd - quoteStart);
    return env->NewStringUTF(fname.c_str());
}

} // namespace encoding
} // namespace neotools
