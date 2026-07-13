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
// The native method is resolved ONLY through RegisterNatives (see Main.cpp),
// so there is deliberately no exported Java_com_... symbol.
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

// --- Generator: bytes -> .hpp header ---

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

    // Sanitize filename for C++ identifier: replace non-alnum with '_'
    std::string safeName;
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

    // Build the header string piece by piece to avoid huge allocations.
    std::string hpp;
    hpp.reserve(static_cast<size_t>(len) * 5 + 512);

    hpp += "#pragma once\n";
    hpp += "#include <cstdint>\n";
    hpp += "#include <cstddef>\n\n";

    // constexpr unsigned char <name>_data[] = {
    hpp += "inline constexpr unsigned char ";
    hpp += safeName;
    hpp += "_data[] = {\n";

    const size_t total = static_cast<size_t>(len);
    for (size_t i = 0; i < total; i += 16) {
        hpp += "    ";
        const size_t lineEnd = std::min(i + 16, total);
        for (size_t j = i; j < lineEnd; ++j) {
            char buf[8];
            snprintf(buf, sizeof(buf), "0x%02x",
                     static_cast<uint8_t>(bytes[j]));
            hpp += buf;
            if (j + 1 < lineEnd) {
                hpp += ", ";
            }
        }
        hpp += ",\n";
    }

    hpp += "};\n\n";

    // <name>_size
    hpp += "inline constexpr std::size_t ";
    hpp += safeName;
    hpp += "_size = ";
    hpp += std::to_string(total);
    hpp += ";\n\n";

    // <name>_name
    hpp += "inline constexpr const char ";
    hpp += safeName;
    hpp += "_name[] = \"";
    hpp += fname;
    hpp += "\";\n";

    if (filename != nullptr && fname != nullptr) {
        env->ReleaseStringUTFChars(filename, fname);
    }

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    jstring result = env->NewStringUTF(hpp.c_str());
    return result;
}

// --- Helpers for Decoder ---

static bool iequals(const std::string& a, const std::string& b) {
    if (a.size() != b.size()) return false;
    for (size_t i = 0; i < a.size(); ++i) {
        if (std::tolower(static_cast<unsigned char>(a[i])) !=
            std::tolower(static_cast<unsigned char>(b[i]))) return false;
    }
    return true;
}

static std::string trim(const std::string& s) {
    size_t start = s.find_first_not_of(" \t\r\n");
    if (start == std::string::npos) return "";
    size_t end = s.find_last_not_of(" \t\r\n");
    return s.substr(start, end - start + 1);
}

// Parse "0xHH" or "0xHH," to a byte value. Returns -1 on failure.
static int parseHexByte(const std::string& token) {
    std::string t = trim(token);
    // Remove trailing comma
    while (!t.empty() && t.back() == ',') t.pop_back();
    t = trim(t);

    if (t.size() < 3 || t[0] != '0' || (t[1] != 'x' && t[1] != 'X')) {
        return -1;
    }

    const char* hex = t.c_str() + 2;
    char* end = nullptr;
    unsigned long val = strtoul(hex, &end, 16);
    if (end == hex || *end != '\0' || val > 0xFF) {
        return -1;
    }
    return static_cast<int>(val);
}

// --- Decoder: .hpp header -> bytes ---

jbyteArray HeaderToFile(JNIEnv* env, jobject /* thiz */, jstring header) {
    if (header == nullptr) {
        return env->NewByteArray(0);
    }

    const char* hdrChars = env->GetStringUTFChars(header, nullptr);
    if (hdrChars == nullptr) {
        return env->NewByteArray(0);
    }

    std::string hdr(hdrChars);
    env->ReleaseStringUTFChars(header, hdrChars);

    // Find the byte array: look for "_data[] = {"
    size_t arrayStart = hdr.find("_data[]");
    if (arrayStart == std::string::npos) {
        return env->NewByteArray(0);
    }
    arrayStart = hdr.find('{', arrayStart);
    if (arrayStart == std::string::npos) {
        return env->NewByteArray(0);
    }
    arrayStart++; // skip past '{'

    // Find closing '}'
    size_t arrayEnd = hdr.find('}', arrayStart);
    if (arrayEnd == std::string::npos) {
        return env->NewByteArray(0);
    }

    std::string arrayContent = hdr.substr(arrayStart, arrayEnd - arrayStart);

    // Parse hex bytes
    std::vector<uint8_t> bytes;
    bytes.reserve(arrayContent.size() / 4);

    size_t pos = 0;
    while (pos < arrayContent.size()) {
        // Skip non-hex characters (spaces, commas, newlines, 0x prefix)
        while (pos < arrayContent.size()) {
            char c = arrayContent[pos];
            if (c == '0' && pos + 1 < arrayContent.size() &&
                (arrayContent[pos + 1] == 'x' || arrayContent[pos + 1] == 'X')) {
                break;
            }
            pos++;
        }
        if (pos >= arrayContent.size()) break;

        // Extract token starting with 0x
        size_t tokenStart = pos;
        while (pos < arrayContent.size() && arrayContent[pos] != ',' &&
               arrayContent[pos] != ' ' && arrayContent[pos] != '\n' &&
               arrayContent[pos] != '\r' && arrayContent[pos] != '\t') {
            pos++;
        }
        std::string token = arrayContent.substr(tokenStart, pos - tokenStart);
        int val = parseHexByte(token);
        if (val >= 0) {
            bytes.push_back(static_cast<uint8_t>(val));
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
