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
#include "encoding/cpp_converter.hpp"
#include "imageeditor/adjustments.hpp"
#include "imageeditor/crop.hpp"
#include "imageeditor/drawing.hpp"
#include "imageeditor/bgremove.hpp"
#include "imageeditor/shapes.hpp"

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
    if (clazz == nullptr) {
        return JNI_ERR;
    }
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
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

} // namespace

int RegisterImageEditor(JNIEnv* env) {
    JNINativeMethod methods[] = {
        // Adjustments
        { OBFUSCATE("nativeAdjustBrightness"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustBrightness) },
        { OBFUSCATE("nativeAdjustContrast"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustContrast) },
        { OBFUSCATE("nativeAdjustSaturation"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustSaturation) },
        { OBFUSCATE("nativeAdjustExposure"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustExposure) },
        { OBFUSCATE("nativeAdjustWarmth"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustWarmth) },
        { OBFUSCATE("nativeAdjustHighlights"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustHighlights) },
        { OBFUSCATE("nativeAdjustShadows"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustShadows) },
        { OBFUSCATE("nativeAdjustSharpness"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustSharpness) },
        { OBFUSCATE("nativeAdjustVignette"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustVignette) },
        { OBFUSCATE("nativeAdjustHue"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustHue) },
        { OBFUSCATE("nativeApplyAllAdjustments"),
          OBFUSCATE("(Landroid/graphics/Bitmap;FFFFFFFFFF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::ApplyAllAdjustments) },
        // Crop / Transform
        { OBFUSCATE("nativeCropBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::CropBitmap) },
        { OBFUSCATE("nativeRotateBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RotateBitmap) },
        { OBFUSCATE("nativeFlipBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;Z)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FlipBitmap) },
        { OBFUSCATE("nativeResizeBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::ResizeBitmap) },
        // Drawing
        { OBFUSCATE("nativeRenderStroke"),
          OBFUSCATE("(Landroid/graphics/Bitmap;[FIFIF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RenderStroke) },
        { OBFUSCATE("nativeRenderStrokesBatch"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F[II[FI[F[F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RenderStrokesBatch) },
        // Background removal
        { OBFUSCATE("nativeRemoveBackground"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RemoveBackground) },
        { OBFUSCATE("nativeRemoveBackgroundByColor"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RemoveBackgroundByColor) },
        // Shapes
        { OBFUSCATE("nativeRenderShape"),
          OBFUSCATE("(Landroid/graphics/Bitmap;IIIIFFFZIFIIIIIF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RenderShape) },
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

    if (RegisterImageEditor(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "ImageEditor native registration failed");
    }

    return JNI_VERSION_1_6;
}