#pragma once

#include <jni.h>

namespace neotools {
namespace imageeditor {

/**
 * Remove background using flood-fill from edges.
 *
 * Samples the dominant color from the image edges and removes all similar
 * pixels within the given tolerance, making them transparent.
 *
 * @param bitmap    the input bitmap (ARGB_8888).
 * @param tolerance color similarity threshold (0-255). Higher = more aggressive.
 * @param edgeSample number of pixels to sample from each edge for dominant color.
 * @return new bitmap with background removed (transparent).
 */
jobject RemoveBackground(JNIEnv* env, jobject thiz, jobject bitmap,
    jint tolerance, jint edgeSample);

/**
 * Remove background by color key.
 *
 * Makes all pixels matching the given color transparent.
 *
 * @param bitmap    the input bitmap.
 * @param targetColor the color to remove (ARGB int).
 * @param tolerance color similarity threshold (0-255).
 * @return new bitmap with matching pixels transparent.
 */
jobject RemoveBackgroundByColor(JNIEnv* env, jobject thiz, jobject bitmap,
    jint targetColor, jint tolerance);

} // namespace imageeditor
} // namespace neotools
