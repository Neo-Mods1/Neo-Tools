// ---------------------------------------------------------------------------
// JNI root: loads the native library and registers every tool's native methods.
//
// Each category owns its own folder (e.g. `encoding/`) exposing its native
// implementation in a `neotools::<category>` namespace. The actual
// registration of those methods against their Kotlin counterparts happens HERE,
// in JNI_OnLoad, so each category's translation unit only defines the
// implementation and never touches registration. Future categories
// (Hashing, Ciphers, ...) just add a folder, expose its function in the
// header, and get a Register call below.
// ---------------------------------------------------------------------------

#include <jni.h>
#include <android/log.h>

#include "obfuscate.h"
#include "encoding/base64.hpp"

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

namespace {

// Registers the Encryption & Decryption -> Base64 method against
// com.neomods.tools.native.NeoNative. The implementation
// (neotools::encoding::EncodeBase64) is defined in encoding/base64.cpp.
int RegisterEncoding(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("encodeBase64"),  OBFUSCATE("([B)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::EncodeBase64) },
        { OBFUSCATE("decodeBase64"),  OBFUSCATE("(Ljava/lang/String;)[B"),
          reinterpret_cast<void*>(neotools::encoding::DecodeBase64) }
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

} // namespace

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Registration is the ONLY resolution path (no exported Java_com_...
    // symbols). If this fails the method simply won't link instead of crashing
    // with a duplicate/ambiguous native method. If it succeeds, everything
    // resolves through RegisterNatives.
    if (RegisterEncoding(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "Encoding native registration failed");
    }

    return JNI_VERSION_1_6;
}