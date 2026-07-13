#pragma once

#include <jni.h>

/**
 * Encryption & Decryption -> Base64 encoder implementation.
 *
 * The native method is resolved exclusively through RegisterNatives (see
 * Main.cpp), so there is no exported Java_com_... symbol. The implementation
 * lives in this translation unit; the registration lives in Main.cpp.
 */
namespace neotools {
namespace encoding {

/**
 * Encode raw bytes to standard RFC 4648 Base64 (single line, no padding
 * newlines). Maps to the Kotlin-side `external fun encodeBase64`.
 *
 * @param env  JNI environment.
 * @param thiz the `NeoNative` instance (unused; encoder is stateless).
 * @param data input bytes to encode.
 * @return the Base64 text as a `jstring`.
 */
jstring EncodeBase64(JNIEnv* env, jobject thiz, jbyteArray data);

} // namespace encoding
} // namespace neotools