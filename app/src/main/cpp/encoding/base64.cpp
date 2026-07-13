// ---------------------------------------------------------------------------
// Encryption & Decryption -> Base64 encoder.
//
// Implements standard RFC 4648 Base64 encoding of an arbitrary byte buffer.
// The output is a single continuous line (no newlines / spaces / tabs). The
// heavy lifting lives here in C++; the Kotlin layer only marshals bytes across
// the JNI boundary (see com.neomods.tools.native.NeoNative).
//
// The native method is resolved ONLY through RegisterNatives (see Main.cpp),
// so there is deliberately no exported Java_com_... symbol: keeping a single
// resolution path avoids a duplicate/ambiguous native method that previously
// crashed on every call. The implementation below is intentionally the only
// thing defined in this file; registration lives in Main.cpp.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "encoding/base64.hpp"

#include <cstdint>
#include <cstdlib>
#include <jni.h>

namespace neotools {
namespace encoding {

static const char kBase64Alphabet[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

jstring EncodeBase64(JNIEnv* env, jobject /* thiz */, jbyteArray data) {
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

    // Every 3 input bytes become 4 output chars; round up.
    const size_t outCapacity = ((static_cast<size_t>(len) + 2) / 3) * 4;
    char* out = static_cast<char*>(malloc(outCapacity + 1));
    if (out == nullptr) {
        env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
        return env->NewStringUTF("");
    }

    size_t oi = 0;
    for (jsize i = 0; i < len; i += 3) {
        const uint32_t a = static_cast<uint8_t>(bytes[i]);
        const uint32_t b = (i + 1 < len) ? static_cast<uint8_t>(bytes[i + 1]) : 0u;
        const uint32_t c = (i + 2 < len) ? static_cast<uint8_t>(bytes[i + 2]) : 0u;
        const uint32_t triple = (a << 16) | (b << 8) | c;

        out[oi++] = kBase64Alphabet[(triple >> 18) & 0x3Fu];
        out[oi++] = kBase64Alphabet[(triple >> 12) & 0x3Fu];
        out[oi++] = (i + 1 < len) ? kBase64Alphabet[(triple >> 6) & 0x3Fu] : '=';
        out[oi++] = (i + 2 < len) ? kBase64Alphabet[triple & 0x3Fu] : '=';
    }
    out[oi] = '\0';

    jstring result = env->NewStringUTF(out);

    free(out);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return result;
}

jbyteArray DecodeBase64(JNIEnv* env, jobject /* thiz */, jstring input) {
    if (input == nullptr) {
        return env->NewByteArray(0);
    }

    const char* inChars = env->GetStringUTFChars(input, nullptr);
    if (inChars == nullptr) {
        return env->NewByteArray(0);
    }

    const size_t inLen = strlen(inChars);
    if (inLen == 0) {
        env->ReleaseStringUTFChars(input, inChars);
        return env->NewByteArray(0);
    }

    // Build a reverse lookup table for the Base64 alphabet.
    static int8_t lookup[256];
    static bool lookupInit = false;
    if (!lookupInit) {
        memset(lookup, -1, sizeof(lookup));
        for (int i = 0; i < 64; i++) {
            lookup[static_cast<uint8_t>(kBase64Alphabet[i])] = i;
        }
        lookupInit = true;
    }

    // Strip whitespace / newlines and compute decode table.
    // Worst case: every 4 Base64 chars produce 3 bytes.
    const size_t maxOut = (inLen / 4) * 3 + 3;
    uint8_t* out = static_cast<uint8_t*>(malloc(maxOut));
    if (out == nullptr) {
        env->ReleaseStringUTFChars(input, inChars);
        return env->NewByteArray(0);
    }

    size_t oi = 0;
    uint32_t accum = 0;
    int bitsFilled = 0;

    for (size_t i = 0; i < inLen; i++) {
        const char c = inChars[i];
        if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue;

        const int8_t val = lookup[static_cast<uint8_t>(c)];
        if (val < 0) continue; // skip padding '=' and invalid chars

        accum = (accum << 6) | static_cast<uint32_t>(val);
        bitsFilled += 6;

        if (bitsFilled >= 8) {
            bitsFilled -= 8;
            out[oi++] = static_cast<uint8_t>((accum >> bitsFilled) & 0xFF);
        }
    }

    env->ReleaseStringUTFChars(input, inChars);

    jbyteArray result = env->NewByteArray(static_cast<jsize>(oi));
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, static_cast<jsize>(oi),
                                reinterpret_cast<jbyte*>(out));
    }

    free(out);
    return result;
}

} // namespace encoding
} // namespace neotools