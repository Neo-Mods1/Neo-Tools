#pragma once

#include <jni.h>

namespace neotools {
namespace imageeditor {

/**
 * Render a stroke onto the bitmap.
 *
 * @param bitmap    the target bitmap (ARGB_8888, mutable).
 * @param points    flat float array: [x0,y0, x1,y1, ...].
 * @param numPoints number of point pairs (array length / 2).
 * @param color     stroke color as ARGB int (0xAARRGGBB).
 * @param strokeWidth width in pixels.
 * @param brushType 0=pencil, 1=marker, 2=airbrush, 3=eraser.
 * @param opacity   0.0-1.0 alpha for the stroke.
 */
jobject RenderStroke(JNIEnv* env, jobject thiz, jobject bitmap,
    jfloatArray points, jint numPoints, jint color,
    jfloat strokeWidth, jint brushType, jfloat opacity);

/**
 * Render multiple strokes at once for batch processing.
 *
 * @param bitmap      the target bitmap.
 * @param allPoints   flat array of all stroke point arrays concatenated.
 * @param strokeSizes int array with the number of points per stroke.
 * @param numStrokes  number of strokes.
 * @param colors      int array of ARGB colors per stroke.
 * @param widths      float array of stroke widths per stroke.
 * @param brushTypes  int array of brush types per stroke.
 * @param opacities   float array of opacities per stroke.
 */
jobject RenderStrokesBatch(JNIEnv* env, jobject thiz, jobject bitmap,
    jfloatArray allPoints, jintArray strokeSizes, jint numStrokes,
    jintArray colors, jfloatArray widths, jintArray brushTypes,
    jfloatArray opacities);

} // namespace imageeditor
} // namespace neotools
