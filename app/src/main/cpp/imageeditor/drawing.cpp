// ---------------------------------------------------------------------------
// Image Editor -> Drawing (stroke rendering).
//
// Renders drawing strokes directly onto bitmaps in C++. Supports pencil,
// marker, airbrush, and eraser brush types with variable width and opacity.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "imageeditor/drawing.hpp"

#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <cstring>
#include <algorithm>

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

namespace neotools {
namespace imageeditor {

static inline uint8_t clamp8(int v) {
    return static_cast<uint8_t>(v < 0 ? 0 : (v > 255 ? 255 : v));
}

struct BmpInfo {
    void* pixels = nullptr;
    int width = 0;
    int height = 0;
    AndroidBitmapFormat format = ANDROID_BITMAP_FORMAT_RGBA_8888;
    int stride = 0;
};

static bool lock(JNIEnv* env, jobject bitmap, BmpInfo& info) {
    AndroidBitmapInfo bmpInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bmpInfo) != ANDROID_BITMAP_RESULT_SUCCESS) return false;
    info.width = static_cast<int>(bmpInfo.width);
    info.height = static_cast<int>(bmpInfo.height);
    info.format = static_cast<AndroidBitmapFormat>(bmpInfo.format);
    info.stride = static_cast<int>(bmpInfo.stride);
    return AndroidBitmap_lockPixels(env, bitmap, &info.pixels) == ANDROID_BITMAP_RESULT_SUCCESS
           && info.pixels != nullptr;
}

static void unlock(JNIEnv* env, jobject bitmap) {
    AndroidBitmap_unlockPixels(env, bitmap);
}

// ── Blend a single pixel with alpha compositing ───────────────────────────

static inline void blendPixel(uint32_t* dst, int dr, int dg, int db, uint8_t alpha) {
    const uint32_t d = *dst;
    const uint8_t da = (d >> 24) & 0xFF;
    const uint8_t ddR = (d >> 0) & 0xFF;
    const uint8_t ddG = (d >> 8) & 0xFF;
    const uint8_t ddB = (d >> 16) & 0xFF;

    const float a = alpha / 255.0f;
    const float da2 = da / 255.0f * (1.0f - a);

    const uint8_t outA = clamp8(static_cast<int>((a + da2) * 255.0f));
    const uint8_t outR = clamp8(static_cast<int>((dr * a + ddR * da2) / (a + da2 + 0.001f)));
    const uint8_t outG = clamp8(static_cast<int>((dg * a + ddG * da2) / (a + da2 + 0.001f)));
    const uint8_t outB = clamp8(static_cast<int>((db * a + ddB * da2) / (a + da2 + 0.001f)));

    *dst = (outA << 24) | (outB << 16) | (outG << 8) | outR;
}

// ── Draw a circle of pixels (for pencil / airbrush) ───────────────────────

static void drawCircle(uint32_t* pixels, int w, int h,
                        int cx, int cy, float radius,
                        uint8_t r, uint8_t g, uint8_t b, uint8_t a,
                        int brushType) {
    const int left = std::max(0, static_cast<int>(cx - radius));
    const int right = std::min(w - 1, static_cast<int>(cx + radius));
    const int top = std::max(0, static_cast<int>(cy - radius));
    const int bottom = std::min(h - 1, static_cast<int>(cy + radius));

    for (int y = top; y <= bottom; y++) {
        for (int x = left; x <= right; x++) {
            const float dx = x - cx;
            const float dy = y - cy;
            const float dist = sqrtf(dx * dx + dy * dy);

            if (dist > radius) continue;

            uint8_t pixelAlpha = a;

            if (brushType == 0) {
                // Pencil: hard edge
                if (dist > radius - 0.5f) continue;
            } else if (brushType == 1) {
                // Marker: slightly soft edge
                const float edge = radius - dist;
                if (edge < 1.0f) {
                    pixelAlpha = static_cast<uint8_t>(a * edge);
                }
            } else if (brushType == 2) {
                // Airbrush: gaussian falloff from center
                const float norm = dist / radius;
                const float gaussian = expf(-norm * norm * 3.0f);
                pixelAlpha = static_cast<uint8_t>(a * gaussian);
            } else if (brushType == 3) {
                // Eraser: remove alpha
                pixels[y * w + x] = 0;
                continue;
            }

            if (pixelAlpha > 0) {
                blendPixel(&pixels[y * w + x], r, g, b, pixelAlpha);
            }
        }
    }
}

// ── Draw a line between two points with interpolation ──────────────────────

static void drawLine(uint32_t* pixels, int w, int h,
                      float x0, float y0, float x1, float y1, float radius,
                      uint8_t r, uint8_t g, uint8_t b, uint8_t a,
                      int brushType) {
    const float dx = x1 - x0;
    const float dy = y1 - y0;
    const float len = sqrtf(dx * dx + dy * dy);

    if (len < 0.5f) {
        drawCircle(pixels, w, h, static_cast<int>(x0), static_cast<int>(y0),
                   radius, r, g, b, a, brushType);
        return;
    }

    const int steps = std::max(1, static_cast<int>(ceilf(len)));
    for (int i = 0; i <= steps; i++) {
        const float t = static_cast<float>(i) / steps;
        const float x = x0 + dx * t;
        const float y = y0 + dy * t;
        drawCircle(pixels, w, h, static_cast<int>(x), static_cast<int>(y),
                   radius, r, g, b, a, brushType);
    }
}

// ── Public API ─────────────────────────────────────────────────────────────

jobject RenderStroke(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                     jfloatArray points, jint numPoints, jint color,
                     jfloat strokeWidth, jint brushType, jfloat opacity) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;

    jfloat* pts = env->GetFloatArrayElements(points, nullptr);
    if (pts == nullptr) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* px = static_cast<uint32_t*>(info.pixels);
    const uint8_t r = (color >> 16) & 0xFF;
    const uint8_t g = (color >> 8) & 0xFF;
    const uint8_t b = color & 0xFF;
    const uint8_t a = static_cast<uint8_t>(opacity * 255.0f);
    const float radius = strokeWidth / 2.0f;

    for (int i = 0; i < numPoints - 1; i++) {
        const float x0 = pts[i * 2];
        const float y0 = pts[i * 2 + 1];
        const float x1 = pts[(i + 1) * 2];
        const float y1 = pts[(i + 1) * 2 + 1];
        drawLine(px, info.width, info.height, x0, y0, x1, y1, radius,
                 r, g, b, a, brushType);
    }

    // Draw endpoint if only one point (dot)
    if (numPoints == 1) {
        drawCircle(px, info.width, info.height,
                   static_cast<int>(pts[0]), static_cast<int>(pts[1]),
                   radius, r, g, b, a, brushType);
    }

    env->ReleaseFloatArrayElements(points, pts, JNI_ABORT);
    unlock(env, bitmap);
    return bitmap;
}

jobject RenderStrokesBatch(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                           jfloatArray allPoints, jintArray strokeSizes,
                           jint numStrokes, jintArray colors,
                           jfloatArray widths, jintArray brushTypes,
                           jfloatArray opacities) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;

    jfloat* pts = env->GetFloatArrayElements(allPoints, nullptr);
    jint* sizes = env->GetIntArrayElements(strokeSizes, nullptr);
    jint* cols = env->GetIntArrayElements(colors, nullptr);
    jfloat* wds = env->GetFloatArrayElements(widths, nullptr);
    jint* bts = env->GetIntArrayElements(brushTypes, nullptr);
    jfloat* ops = env->GetFloatArrayElements(opacities, nullptr);

    auto* px = static_cast<uint32_t*>(info.pixels);
    int offset = 0;

    for (int s = 0; s < numStrokes; s++) {
        const int nPts = sizes[s];
        const uint32_t col = static_cast<uint32_t>(cols[s]);
        const float w = wds[s];
        const float op = ops[s];

        const uint8_t r = (col >> 16) & 0xFF;
        const uint8_t g = (col >> 8) & 0xFF;
        const uint8_t b = col & 0xFF;
        const uint8_t a = static_cast<uint8_t>(op * 255.0f);
        const float radius = w / 2.0f;

        for (int i = 0; i < nPts - 1; i++) {
            drawLine(px, info.width, info.height,
                     pts[offset + i * 2], pts[offset + i * 2 + 1],
                     pts[offset + (i + 1) * 2], pts[offset + (i + 1) * 2 + 1],
                     radius, r, g, b, a, bts[s]);
        }
        if (nPts == 1) {
            drawCircle(px, info.width, info.height,
                       static_cast<int>(pts[offset]), static_cast<int>(pts[offset + 1]),
                       radius, r, g, b, a, bts[s]);
        }

        offset += nPts * 2;
    }

    env->ReleaseFloatArrayElements(allPoints, pts, JNI_ABORT);
    env->ReleaseIntArrayElements(strokeSizes, sizes, JNI_ABORT);
    env->ReleaseIntArrayElements(colors, cols, JNI_ABORT);
    env->ReleaseFloatArrayElements(widths, wds, JNI_ABORT);
    env->ReleaseIntArrayElements(brushTypes, bts, JNI_ABORT);
    env->ReleaseFloatArrayElements(opacities, ops, JNI_ABORT);

    unlock(env, bitmap);
    return bitmap;
}

} // namespace imageeditor
} // namespace neotools
