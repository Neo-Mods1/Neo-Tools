#pragma once

#include <jni.h>

/**
 * Registers the native methods implemented in this translation unit against
 * their Kotlin-side counterpart (`com.neomods.tools.native.NeoNative`).
 *
 * Called from `JNI_OnLoad` in Main.cpp. Returns JNI_OK on success.
 */
int RegisterEncoding(JNIEnv* env);
