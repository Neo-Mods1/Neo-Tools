// ---------------------------------------------------------------------------
// Image Editor -> Shape rendering.
//
// Renders shapes onto bitmaps using Android Canvas API via JNI.
// Supports: rectangle, rounded rect, circle, oval, triangle, star, diamond,
// arrows, hexagon, octagon, heart, cross, octagon star.
// Includes stroke and shadow support.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "imageeditor/shapes.hpp"

#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <cstring>

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

namespace neotools {
namespace imageeditor {

static jobject createBitmap(JNIEnv* env, int w, int h) {
    jclass cls = env->FindClass(OBFUSCATE("android/graphics/Bitmap"));
    jmethodID mid = env->GetStaticMethodID(cls,
        OBFUSCATE("createBitmap"),
        OBFUSCATE("(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"));

    jclass cfgCls = env->FindClass(OBFUSCATE("android/graphics/Bitmap$Config"));
    jfieldID fid = env->GetStaticFieldID(cfgCls,
        OBFUSCATE("ARGB_8888"), OBFUSCATE("Landroid/graphics/Bitmap$Config;"));
    jobject cfg = env->GetStaticObjectField(cfgCls, fid);

    return env->CallStaticObjectMethod(cls, mid, w, h, cfg);
}

// Build an android.graphics.Path from shape type
static jobject buildPath(JNIEnv* env, jint shapeType, float w, float h,
                          jint triangleDirection, jint starPoints, jfloat starInnerRadius) {
    jclass pathClass = env->FindClass(OBFUSCATE("android/graphics/Path"));
    jmethodID ctor = env->GetMethodID(pathClass, OBFUSCATE("<init>"), OBFUSCATE("()V"));
    jobject path = env->NewObject(pathClass, ctor);

    jmethodID moveTo = env->GetMethodID(pathClass, OBFUSCATE("moveTo"), OBFUSCATE("(FF)V"));
    jmethodID lineTo = env->GetMethodID(pathClass, OBFUSCATE("lineTo"), OBFUSCATE("(FF)V"));
    jmethodID close = env->GetMethodID(pathClass, OBFUSCATE("close"), OBFUSCATE("()V"));

    const float cx = w / 2.0f;
    const float cy = h / 2.0f;

    switch (shapeType) {
        case 4: { // Triangle
            env->CallVoidMethod(path, moveTo, cx, 0);
            switch (triangleDirection) {
                case 0: // up
                    env->CallVoidMethod(path, moveTo, cx, 0);
                    env->CallVoidMethod(path, lineTo, w, h);
                    env->CallVoidMethod(path, lineTo, 0, h);
                    break;
                case 1: // down
                    env->CallVoidMethod(path, moveTo, 0, 0);
                    env->CallVoidMethod(path, lineTo, w, 0);
                    env->CallVoidMethod(path, lineTo, cx, h);
                    break;
                case 2: // left
                    env->CallVoidMethod(path, moveTo, w, 0);
                    env->CallVoidMethod(path, lineTo, w, h);
                    env->CallVoidMethod(path, lineTo, 0, cy);
                    break;
                case 3: // right
                    env->CallVoidMethod(path, moveTo, 0, 0);
                    env->CallVoidMethod(path, lineTo, 0, h);
                    env->CallVoidMethod(path, lineTo, w, cy);
                    break;
            }
            env->CallVoidMethod(path, close);
            break;
        }
        case 5: { // Star
            const int pts = starPoints > 2 ? starPoints : 5;
            const float innerR = starInnerRadius > 0 && starInnerRadius < 1 ? starInnerRadius : 0.4f;
            const float outerR = fminf(cx, cy) * 0.95f;
            const float innerRR = outerR * innerR;
            const float angleOffset = -M_PI_2;

            for (int i = 0; i < pts * 2; i++) {
                const float angle = angleOffset + (M_PI * i / pts);
                const float r = (i % 2 == 0) ? outerR : innerRR;
                const float px = cx + r * cosf(angle);
                const float py = cy + r * sinf(angle);
                if (i == 0) env->CallVoidMethod(path, moveTo, px, py);
                else env->CallVoidMethod(path, lineTo, px, py);
            }
            env->CallVoidMethod(path, close);
            break;
        }
        case 6: { // Diamond
            env->CallVoidMethod(path, moveTo, cx, 0);
            env->CallVoidMethod(path, lineTo, w, cy);
            env->CallVoidMethod(path, lineTo, cx, h);
            env->CallVoidMethod(path, lineTo, 0, cy);
            env->CallVoidMethod(path, close);
            break;
        }
        case 7: { // Arrow right
            env->CallVoidMethod(path, moveTo, 0, cy * 0.6f);
            env->CallVoidMethod(path, lineTo, cx, cy * 0.6f);
            env->CallVoidMethod(path, lineTo, cx, 0);
            env->CallVoidMethod(path, lineTo, w, cy);
            env->CallVoidMethod(path, lineTo, cx, h);
            env->CallVoidMethod(path, lineTo, cx, cy * 1.4f);
            env->CallVoidMethod(path, lineTo, 0, cy * 1.4f);
            env->CallVoidMethod(path, close);
            break;
        }
        case 8: { // Arrow left
            env->CallVoidMethod(path, moveTo, w, cy * 0.6f);
            env->CallVoidMethod(path, lineTo, cx, cy * 0.6f);
            env->CallVoidMethod(path, lineTo, cx, 0);
            env->CallVoidMethod(path, lineTo, 0, cy);
            env->CallVoidMethod(path, lineTo, cx, h);
            env->CallVoidMethod(path, lineTo, cx, cy * 1.4f);
            env->CallVoidMethod(path, lineTo, w, cy * 1.4f);
            env->CallVoidMethod(path, close);
            break;
        }
        case 9: { // Arrow up
            env->CallVoidMethod(path, moveTo, cx * 0.6f, h);
            env->CallVoidMethod(path, lineTo, cx * 0.6f, cy);
            env->CallVoidMethod(path, lineTo, 0, cy);
            env->CallVoidMethod(path, lineTo, cx, 0);
            env->CallVoidMethod(path, lineTo, w, cy);
            env->CallVoidMethod(path, lineTo, cx * 1.4f, cy);
            env->CallVoidMethod(path, lineTo, cx * 1.4f, h);
            env->CallVoidMethod(path, close);
            break;
        }
        case 10: { // Arrow down
            env->CallVoidMethod(path, moveTo, cx * 0.6f, 0);
            env->CallVoidMethod(path, lineTo, cx * 0.6f, cy);
            env->CallVoidMethod(path, lineTo, 0, cy);
            env->CallVoidMethod(path, lineTo, cx, h);
            env->CallVoidMethod(path, lineTo, w, cy);
            env->CallVoidMethod(path, lineTo, cx * 1.4f, cy);
            env->CallVoidMethod(path, lineTo, cx * 1.4f, 0);
            env->CallVoidMethod(path, close);
            break;
        }
        case 11: { // Hexagon
            for (int i = 0; i < 6; i++) {
                const float angle = -M_PI_2 + (M_PI / 3.0f) * i;
                const float px = cx + cx * 0.95f * cosf(angle);
                const float py = cy + cy * 0.95f * sinf(angle);
                if (i == 0) env->CallVoidMethod(path, moveTo, px, py);
                else env->CallVoidMethod(path, lineTo, px, py);
            }
            env->CallVoidMethod(path, close);
            break;
        }
        case 12: { // Octagon
            for (int i = 0; i < 8; i++) {
                const float angle = -M_PI_2 + (M_PI / 4.0f) * i;
                const float px = cx + cx * 0.95f * cosf(angle);
                const float py = cy + cy * 0.95f * sinf(angle);
                if (i == 0) env->CallVoidMethod(path, moveTo, px, py);
                else env->CallVoidMethod(path, lineTo, px, py);
            }
            env->CallVoidMethod(path, close);
            break;
        }
        case 13: { // Heart (approximated with cubic beziers via lines)
            const float s = fminf(w, h) * 0.95f;
            const float ox = (w - s) / 2;
            const float oy = (h - s) / 2;
            // Heart using bezier-like polygon
            env->CallVoidMethod(path, moveTo, cx, oy + s * 0.3f);
            env->CallVoidMethod(path, lineTo, ox + s * 0.1f, oy);
            env->CallVoidMethod(path, lineTo, ox, oy + s * 0.3f);
            env->CallVoidMethod(path, lineTo, cx, oy + s);
            env->CallVoidMethod(path, lineTo, ox + s, oy + s * 0.3f);
            env->CallVoidMethod(path, lineTo, ox + s * 0.9f, oy);
            env->CallVoidMethod(path, close);
            break;
        }
        case 14: { // Cross
            const float arm = fminf(w, h) * 0.3f;
            env->CallVoidMethod(path, moveTo, cx - arm, 0);
            env->CallVoidMethod(path, lineTo, cx + arm, 0);
            env->CallVoidMethod(path, lineTo, cx + arm, cy - arm);
            env->CallVoidMethod(path, lineTo, w, cy - arm);
            env->CallVoidMethod(path, lineTo, w, cy + arm);
            env->CallVoidMethod(path, lineTo, cx + arm, cy + arm);
            env->CallVoidMethod(path, lineTo, cx + arm, h);
            env->CallVoidMethod(path, lineTo, cx - arm, h);
            env->CallVoidMethod(path, lineTo, cx - arm, cy + arm);
            env->CallVoidMethod(path, lineTo, 0, cy + arm);
            env->CallVoidMethod(path, lineTo, 0, cy - arm);
            env->CallVoidMethod(path, lineTo, cx - arm, cy - arm);
            env->CallVoidMethod(path, close);
            break;
        }
        case 15: { // Octagon star (8-pointed star)
            for (int i = 0; i < 16; i++) {
                const float angle = -M_PI_2 + (M_PI / 8.0f) * i;
                const float r = (i % 2 == 0) ? fminf(cx, cy) * 0.95f : fminf(cx, cy) * 0.5f;
                const float px = cx + r * cosf(angle);
                const float py = cy + r * sinf(angle);
                if (i == 0) env->CallVoidMethod(path, moveTo, px, py);
                else env->CallVoidMethod(path, lineTo, px, py);
            }
            env->CallVoidMethod(path, close);
            break;
        }
        default: { // Rectangle (0) and fallback
            env->CallVoidMethod(path, moveTo, 0, 0);
            env->CallVoidMethod(path, lineTo, w, 0);
            env->CallVoidMethod(path, lineTo, w, h);
            env->CallVoidMethod(path, lineTo, 0, h);
            env->CallVoidMethod(path, close);
            break;
        }
    }

    return path;
}

jobject RenderShape(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                    jint shapeType, jint width, jint height,
                    jint fillColor, jint strokeColor, jfloat strokeWidth,
                    jfloat cornerRadius,
                    jboolean hasShadow, jint shadowColor, jfloat shadowRadius,
                    jfloat shadowOffsetX, jfloat shadowOffsetY,
                    jint triangleDirection, jint starPoints, jfloat starInnerRadius) {

    // Create output bitmap
    jobject newBmp = createBitmap(env, width, height);

    // Get Canvas from bitmap
    jclass bitmapClass = env->FindClass(OBFUSCATE("android/graphics/Bitmap"));
    jmethodID createCanvas = env->GetMethodID(bitmapClass,
        OBFUSCATE("createBitmap"),
        OBFUSCATE("(Landroid/graphics/Bitmap;)Landroid/graphics/Canvas;"));

    // Use Canvas(bitmap) constructor
    jclass canvasClass = env->FindClass(OBFUSCATE("android/graphics/Canvas"));
    jmethodID canvasCtor = env->GetMethodID(canvasClass,
        OBFUSCATE("<init>"), OBFUSCATE("(Landroid/graphics/Bitmap;)V"));
    jobject canvas = env->NewObject(canvasClass, canvasCtor, newBmp);

    // Build Paint for fill
    jclass paintClass = env->FindClass(OBFUSCATE("android/graphics/Paint"));
    jmethodID paintCtor = env->GetMethodID(paintClass, OBFUSCATE("<init>"), OBFUSCATE("()V"));
    jobject fillPaint = env->NewObject(paintClass, paintCtor);

    jmethodID setAntiAlias = env->GetMethodID(paintClass, OBFUSCATE("setAntiAlias"), OBFUSCATE("(Z)V"));
    jmethodID setColor = env->GetMethodID(paintClass, OBFUSCATE("setColor"), OBFUSCATE("(I)V"));
    jmethodID setStyle = env->GetMethodID(paintClass, OBFUSCATE("setStyle"), OBFUSCATE("(Landroid/graphics/Paint$Style;)V"));
    jmethodID setStrokeWidth = env->GetMethodID(paintClass, OBFUSCATE("setStrokeWidth"), OBFUSCATE("(F)V"));

    // Get Paint.Style.FILL
    jclass styleClass = env->FindClass(OBFUSCATE("android/graphics/Paint$Style"));
    jfieldID fillStyle = env->GetStaticFieldID(styleClass, OBFUSCATE("FILL"),
        OBFUSCATE("Landroid/graphics/Paint$Style;"));
    jobject fillStyleObj = env->GetStaticObjectField(styleClass, fillStyle);

    jmethodID setShadowLayer = env->GetMethodID(paintClass, OBFUSCATE("setShadowLayer"),
        OBFUSCATE("(FFFFI)V"));

    env->CallVoidMethod(fillPaint, setAntiAlias, JNI_TRUE);
    env->CallVoidMethod(fillPaint, setColor, fillColor);
    env->CallVoidMethod(fillPaint, setStyle, fillStyleObj);

    // Set shadow if needed
    if (hasShadow) {
        env->CallVoidMethod(fillPaint, setShadowLayer, shadowRadius, shadowOffsetX, shadowOffsetY, 0, shadowColor);
    }

    // Draw shape based on type
    if (shapeType == 1) {
        // Rounded rectangle
        jclass rectFClass = env->FindClass(OBFUSCATE("android/graphics/RectF"));
        jmethodID rectFCtor = env->GetMethodID(rectFClass, OBFUSCATE("<init>"), OBFUSCATE("(FFFF)V"));
        jobject rect = env->NewObject(rectFClass, rectFCtor, 0.0f, 0.0f,
            static_cast<float>(width), static_cast<float>(height));

        jmethodID drawRoundRect = env->GetMethodID(canvasClass, OBFUSCATE("drawRoundRect"),
            OBFUSCATE("(Landroid/graphics/RectF;FFLandroid/graphics/Paint;)V"));
        env->CallVoidMethod(canvas, drawRoundRect, rect, cornerRadius, cornerRadius, fillPaint);
    } else if (shapeType == 2) {
        // Circle
        jmethodID drawCircle = env->GetMethodID(canvasClass, OBFUSCATE("drawCircle"),
            OBFUSCATE("(FFFLandroid/graphics/Paint;)V"));
        const float r = fminf(width, height) / 2.0f;
        env->CallVoidMethod(canvas, drawCircle, width / 2.0f, height / 2.0f, r, fillPaint);
    } else if (shapeType == 3) {
        // Oval
        jclass rectFClass = env->FindClass(OBFUSCATE("android/graphics/RectF"));
        jmethodID rectFCtor = env->GetMethodID(rectFClass, OBFUSCATE("<init>"), OBFUSCATE("(FFFF)V"));
        jobject rect = env->NewObject(rectFClass, rectFCtor, 0.0f, 0.0f,
            static_cast<float>(width), static_cast<float>(height));

        jmethodID drawOval = env->GetMethodID(canvasClass, OBFUSCATE("drawOval"),
            OBFUSCATE("(Landroid/graphics/RectF;Landroid/graphics/Paint;)V"));
        env->CallVoidMethod(canvas, drawOval, rect, fillPaint);
    } else {
        // Path-based shapes: triangle, star, diamond, arrows, hexagon, octagon, heart, cross, octagon star
        jobject path = buildPath(env, shapeType, static_cast<float>(width), static_cast<float>(height),
                                  triangleDirection, starPoints, starInnerRadius);

        jmethodID drawPath = env->GetMethodID(canvasClass, OBFUSCATE("drawPath"),
            OBFUSCATE("(Landroid/graphics/Path;Landroid/graphics/Paint;)V"));
        env->CallVoidMethod(canvas, drawPath, path, fillPaint);
    }

    // Draw stroke if enabled
    if (strokeWidth > 0 && strokeColor != 0) {
        jobject strokePaint = env->NewObject(paintClass, paintCtor);

        jfieldID strokeStyle = env->GetStaticFieldID(styleClass, OBFUSCATE("STROKE"),
            OBFUSCATE("Landroid/graphics/Paint$Style;"));
        jobject strokeStyleObj = env->GetStaticObjectField(styleClass, strokeStyle);

        env->CallVoidMethod(strokePaint, setAntiAlias, JNI_TRUE);
        env->CallVoidMethod(strokePaint, setColor, strokeColor);
        env->CallVoidMethod(strokePaint, setStyle, strokeStyleObj);
        env->CallVoidMethod(strokePaint, setStrokeWidth, strokeWidth);

        if (shapeType == 1) {
            jclass rectFClass = env->FindClass(OBFUSCATE("android/graphics/RectF"));
            jmethodID rectFCtor = env->GetMethodID(rectFClass, OBFUSCATE("<init>"), OBFUSCATE("(FFFF)V"));
            jobject rect = env->NewObject(rectFClass, rectFCtor, 0.0f, 0.0f,
                static_cast<float>(width), static_cast<float>(height));
            jmethodID drawRoundRect = env->GetMethodID(canvasClass, OBFUSCATE("drawRoundRect"),
                OBFUSCATE("(Landroid/graphics/RectF;FFLandroid/graphics/Paint;)V"));
            env->CallVoidMethod(canvas, drawRoundRect, rect, cornerRadius, cornerRadius, strokePaint);
        } else if (shapeType == 2) {
            jmethodID drawCircle = env->GetMethodID(canvasClass, OBFUSCATE("drawCircle"),
                OBFUSCATE("(FFFLandroid/graphics/Paint;)V"));
            const float r = fminf(width, height) / 2.0f;
            env->CallVoidMethod(canvas, drawCircle, width / 2.0f, height / 2.0f, r, strokePaint);
        } else if (shapeType == 3) {
            jclass rectFClass = env->FindClass(OBFUSCATE("android/graphics/RectF"));
            jmethodID rectFCtor = env->GetMethodID(rectFClass, OBFUSCATE("<init>"), OBFUSCATE("(FFFF)V"));
            jobject rect = env->NewObject(rectFClass, rectFCtor, 0.0f, 0.0f,
                static_cast<float>(width), static_cast<float>(height));
            jmethodID drawOval = env->GetMethodID(canvasClass, OBFUSCATE("drawOval"),
                OBFUSCATE("(Landroid/graphics/RectF;Landroid/graphics/Paint;)V"));
            env->CallVoidMethod(canvas, drawOval, rect, strokePaint);
        } else {
            jobject path = buildPath(env, shapeType, static_cast<float>(width), static_cast<float>(height),
                                      triangleDirection, starPoints, starInnerRadius);
            jmethodID drawPath = env->GetMethodID(canvasClass, OBFUSCATE("drawPath"),
                OBFUSCATE("(Landroid/graphics/Path;Landroid/graphics/Paint;)V"));
            env->CallVoidMethod(canvas, drawPath, path, strokePaint);
        }
    }

    return newBmp;
}

} // namespace imageeditor
} // namespace neotools
