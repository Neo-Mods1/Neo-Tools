#pragma once

#include <jni.h>

namespace neotools {
namespace imageeditor {

/**
 * Apply brightness adjustment to bitmap pixels.
 * brightness: -1.0 to 1.0 (0 = no change)
 */
jobject AdjustBrightness(JNIEnv* env, jobject thiz, jobject bitmap, jfloat brightness);

/**
 * Apply contrast adjustment.
 * contrast: 0.0 to 2.0 (1.0 = no change)
 */
jobject AdjustContrast(JNIEnv* env, jobject thiz, jobject bitmap, jfloat contrast);

/**
 * Apply saturation adjustment.
 * saturation: 0.0 to 2.0 (1.0 = no change)
 */
jobject AdjustSaturation(JNIEnv* env, jobject thiz, jobject bitmap, jfloat saturation);

/**
 * Apply exposure adjustment.
 * exposure: -1.0 to 1.0 (0 = no change)
 */
jobject AdjustExposure(JNIEnv* env, jobject thiz, jobject bitmap, jfloat exposure);

/**
 * Apply warmth (color temperature) adjustment.
 * warmth: -1.0 to 1.0 (0 = no change, positive = warm, negative = cool)
 */
jobject AdjustWarmth(JNIEnv* env, jobject thiz, jobject bitmap, jfloat warmth);

/**
 * Apply highlights adjustment.
 * highlights: -1.0 to 1.0 (0 = no change)
 */
jobject AdjustHighlights(JNIEnv* env, jobject thiz, jobject bitmap, jfloat highlights);

/**
 * Apply shadows adjustment.
 * shadows: -1.0 to 1.0 (0 = no change)
 */
jobject AdjustShadows(JNIEnv* env, jobject thiz, jobject bitmap, jfloat shadows);

/**
 * Apply sharpness (unsharp mask) adjustment.
 * sharpness: 0.0 to 1.0 (0 = no change)
 */
jobject AdjustSharpness(JNIEnv* env, jobject thiz, jobject bitmap, jfloat sharpness);

/**
 * Apply vignette effect.
 * vignette: 0.0 to 1.0 (0 = no effect)
 */
jobject AdjustVignette(JNIEnv* env, jobject thiz, jobject bitmap, jfloat vignette);

/**
 * Apply hue rotation.
 * hue: 0.0 to 1.0 (0 = no change, full rotation)
 */
jobject AdjustHue(JNIEnv* env, jobject thiz, jobject bitmap, jfloat hue);

/**
 * Apply all adjustments at once for performance.
 * brightness, contrast, saturation, exposure, warmth, highlights, shadows,
 * sharpness, vignette, hue — all in one pass where possible.
 */
jobject ApplyAllAdjustments(JNIEnv* env, jobject thiz, jobject bitmap,
    jfloat brightness, jfloat contrast, jfloat saturation, jfloat exposure,
    jfloat warmth, jfloat highlights, jfloat shadows,
    jfloat sharpness, jfloat vignette, jfloat hue);

} // namespace imageeditor
} // namespace neotools
