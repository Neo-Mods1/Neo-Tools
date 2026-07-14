#pragma once

#include <jni.h>

namespace neotools {
namespace imageeditor {

jobject FilterGrayscale(JNIEnv* env, jobject thiz, jobject bitmap);
jobject FilterSepia(JNIEnv* env, jobject thiz, jobject bitmap, jfloat intensity);
jobject FilterInvert(JNIEnv* env, jobject thiz, jobject bitmap);
jobject FilterThreshold(JNIEnv* env, jobject thiz, jobject bitmap, jfloat threshold);
jobject FilterBlur(JNIEnv* env, jobject thiz, jobject bitmap, jfloat radius);
jobject FilterPixelate(JNIEnv* env, jobject thiz, jobject bitmap, jint blockSize);
jobject FilterEmboss(JNIEnv* env, jobject thiz, jobject bitmap, jfloat intensity);

/**
 * Get ARGB color of a single pixel.
 * @return ARGB int (0xAARRGGBB).
 */
jint GetPixelColor(JNIEnv* env, jobject thiz, jobject bitmap, jint x, jint y);

/**
 * Compute histogram bins (256 per channel).
 * @return int array of 1024 ints: R[256] + G[256] + B[256] + Luma[256].
 */
jintArray ComputeHistogram(JNIEnv* env, jobject thiz, jobject bitmap);

/**
 * Apply a curves LUT to the bitmap.
 * @param lut 256-element lookup table for all channels (same LUT for R,G,B).
 */
jobject ApplyCurvesLut(JNIEnv* env, jobject thiz, jobject bitmap, jintArray lut);

/**
 * Clone-stamp: copy a rectangular region from source to destination.
 * @param srcBitmap source bitmap.
 * @param dstBitmap destination bitmap (modified in-place concept, returns new).
 * @param srcX,srcY source region top-left.
 * @param dstX,dstY destination region top-left.
 * @param width,height region dimensions.
 * @param blendAlpha alpha for blending (0.0-1.0).
 */
jobject CloneStamp(JNIEnv* env, jobject thiz,
    jobject srcBitmap, jobject dstBitmap,
    jint srcX, jint srcY, jint dstX, jint dstY,
    jint width, jint height, jfloat blendAlpha);

/**
 * Blend two bitmaps with a blend mode.
 * @param base bottom layer bitmap.
 * @param overlay top layer bitmap.
 * @param mode 0=normal,1=multiply,2=screen,3=overlay,4=darken,5=lighten,6=color_dodge,7=color_burn,8=soft_light,9=hard_light,10=difference,11=exclusion.
 * @param opacity overlay opacity (0.0-1.0).
 */
jobject BlendBitmaps(JNIEnv* env, jobject thiz,
    jobject base, jobject overlay, jint mode, jfloat opacity);

} // namespace imageeditor
} // namespace neotools
