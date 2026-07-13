// ---------------------------------------------------------------------------
// Encryption & Decryption -> Base64 encoder.
//
// Implements standard RFC 4648 Base64 encoding of an arbitrary byte buffer.
// The output is a single continuous line (no newlines / spaces / tabs). The
// heavy lifting lives here in C++; the Kotlin layer only marshals bytes across
// the JNI boundary (see com.neomods.tools.native.NeoNative).
//
// The native method is resolved ONLY through RegisterNatives (see
// RegisterEncoding). There is deliberately no exported Java_com_... symbol:
// keeping a single resolution path avoids a duplicate/ambiguous native method
// that previously crashed on every call.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "encoding/base64.hpp"

#include <cstdint>
#include <cstdlib>
#include <jni.h>

static const char kBase64Alphabet[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

// Core encoder. Registered via RegisterNatives under the Kotlin-side method
// name. NOTE: a non-static Kotlin `external fun` maps to a 3-argument native
// function (JNIEnv*, jobject thiz, jbyteArray). Getting the arity wrong here
// corrupts the call frame and crashes natively.
extern "C" {

static jstring NeoBase64Encode(JNIEnv* env, jobject /* thiz */, jbyteArray data) {
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

} // extern "C"

int RegisterEncoding(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("encodeBase64"),  OBFUSCATE("([B)Ljava/lang/String;"), reinterpret_cast<void*>(NeoBase64Encode) }
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}