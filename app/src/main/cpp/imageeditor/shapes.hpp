#pragma once

#include <jni.h>

namespace neotools {
namespace imageeditor {

/**
 * Render a shape onto a bitmap.
 *
 * @param bitmap       target bitmap (ARGB_8888, mutable).
 * @param shapeType    0=rect, 1=rounded_rect, 2=circle, 3=oval, 4=triangle,
 *                     5=star, 6=diamond, 7=arrow_right, 8=arrow_left,
 *                     9=arrow_up, 10=arrow_down, 11=hexagon, 12=octagon,
 *                     13=heart, 14=cross, 15=octagon_star.
 * @param width        shape width in pixels.
 * @param height       shape height in pixels.
 * @param fillColor    fill color as ARGB int.
 * @param strokeColor  stroke color as ARGB int (0 = no stroke).
 * @param strokeWidth  stroke width in pixels.
 * @param cornerRadius corner radius for rounded rect (0 = sharp).
 * @param hasShadow    whether to draw a shadow.
 * @param shadowColor  shadow color as ARGB int.
 * @param shadowRadius shadow blur radius.
 * @param shadowOffsetX shadow X offset.
 * @param shadowOffsetY shadow Y offset.
 * @param triangleDirection 0=up,1=down,2=left,3=right (only for triangle).
 * @param starPoints   number of star points (only for star).
 * @param starInnerRadius inner radius ratio for star (0.0-1.0).
 * @return new bitmap with the shape rendered.
 */
jobject RenderShape(JNIEnv* env, jobject thiz, jobject bitmap,
    jint shapeType, jint width, jint height,
    jint fillColor, jint strokeColor, jfloat strokeWidth,
    jfloat cornerRadius,
    jboolean hasShadow, jint shadowColor, jfloat shadowRadius,
    jfloat shadowOffsetX, jfloat shadowOffsetY,
    jint triangleDirection, jint starPoints, jfloat starInnerRadius);

} // namespace imageeditor
} // namespace neotools
