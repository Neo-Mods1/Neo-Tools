// ---------------------------------------------------------------------------
// JNI root: loads the native library and registers every tool's native methods.
//
// Each category owns its own folder (e.g. `encoding/`) exposing a
// `RegisterXxx(JNIEnv*)` function declared in its header. Main.cpp pulls those
// headers in and wires them up from JNI_OnLoad, exactly like a modular tool
// menu. Future categories (Hashing, Ciphers, ...) just add a folder + a
// Register call here.
// ---------------------------------------------------------------------------

#include <jni.h>
#include <android/log.h>

#include "obfuscate.h"
#include "encoding/base64.hpp"

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Registration is the ONLY resolution path (no exported Java_com_...
    // symbols). If this fails the method simply won't link instead of crashing
    // with a duplicate/ambiguous native method.
    if (RegisterEncoding(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "Encoding native registration skipped; using dynamic linkage");
    }

    return JNI_VERSION_1_6;
}
