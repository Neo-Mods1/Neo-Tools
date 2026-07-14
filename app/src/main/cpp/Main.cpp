// ---------------------------------------------------------------------------
// JNI root: loads the native library and registers every tool's native methods.
//
// Each category owns its own folder (e.g. `encoding/`) exposing its native
// implementation in a `neotools::<category>` namespace. The actual
// registration of those methods against their Kotlin counterparts happens HERE,
// in JNI_OnLoad, so each category's translation unit only defines the
// implementation and never touches registration.
//
// NOTE: The native crash handler (sigaction) lives in crash/CrashHandler.cpp
// and is built as a SEPARATE library (libCrashHandler.so) that gets loaded
// by NeoToolsApplication BEFORE neotools.so. This ensures signal handlers
// are installed before any other native code runs.
// ---------------------------------------------------------------------------

#include <jni.h>
#include <android/log.h>

#include "obfuscate.h"
#include "encoding/base64.hpp"
#include "encoding/cpp_converter.hpp"
#include "apk/apk_parser.hpp"

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

namespace {

int RegisterEncoding(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("encodeBase64"),  OBFUSCATE("([B)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::EncodeBase64) },
        { OBFUSCATE("decodeBase64"),  OBFUSCATE("(Ljava/lang/String;)[B"),
          reinterpret_cast<void*>(neotools::encoding::DecodeBase64) }
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) return JNI_ERR;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

int RegisterCppConverter(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("fileToHeader"),  OBFUSCATE("([BLjava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::FileToHeader) },
        { OBFUSCATE("headerToFile"),  OBFUSCATE("(Ljava/lang/String;)[B"),
          reinterpret_cast<void*>(neotools::encoding::HeaderToFile) },
        { OBFUSCATE("headerFileName"), OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::HeaderFileName) }
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) return JNI_ERR;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

int RegisterApkTools(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("nativeParseApkInfo"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::ParseApkInfo) },
        { OBFUSCATE("nativeParseManifest"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::ParseManifest) },
        { OBFUSCATE("nativeGetManifestXml"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::GetManifestXml) },
        { OBFUSCATE("nativeParseCertificate"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::ParseCertificate) },
        { OBFUSCATE("nativeGetNativeLibs"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::GetNativeLibs) },
        { OBFUSCATE("nativeGetZipEntries"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::GetZipEntries) },
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) return JNI_ERR;
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

    if (RegisterEncoding(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "Encoding native registration failed");
    }

    if (RegisterCppConverter(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "CppConverter native registration failed");
    }

    if (RegisterApkTools(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "ApkTools native registration failed");
    }

    return JNI_VERSION_1_6;
}
