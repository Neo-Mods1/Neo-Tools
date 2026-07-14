#pragma once

#include <jni.h>

namespace neotools {
namespace imageeditor {

/**
 * Crop bitmap to the given rectangle.
 */
jobject CropBitmap(JNIEnv* env, jobject thiz, jobject bitmap,
    jint x, jint y, jint width, jint height);

/**
 * Rotate bitmap by the given angle (degrees). Fills background with black.
 */
jobject RotateBitmap(JNIEnv* env, jobject thiz, jobject bitmap, jfloat degrees);

/**
 * Flip bitmap horizontally or vertically.
 * horizontal: true = left-right, false = top-bottom
 */
jobject FlipBitmap(JNIEnv* env, jobject thiz, jobject bitmap, jboolean horizontal);

/**
 * Resize bitmap to the given dimensions using bilinear interpolation.
 */
jobject ResizeBitmap(JNIEnv* env, jobject thiz, jobject bitmap,
    jint newWidth, jint newHeight);

} // namespace imageeditor
} // namespace neotools
