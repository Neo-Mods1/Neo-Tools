// ---------------------------------------------------------------------------
// Image Editor -> Crop, Rotate, Flip, Resize.
//
// Pure C++ bitmap transformations using AndroidBitmap API.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "imageeditor/crop.hpp"

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

// ── Crop ───────────────────────────────────────────────────────────────────

jobject CropBitmap(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                   jint x, jint y, jint width, jint height) {
    BmpInfo srcInfo;
    if (!lock(env, bitmap, srcInfo)) return bitmap;

    const int sx = std::max(0, std::min(static_cast<int>(x), srcInfo.width));
    const int sy = std::max(0, std::min(static_cast<int>(y), srcInfo.height));
    const int sw = std::min(static_cast<int>(width), srcInfo.width - sx);
    const int sh = std::min(static_cast<int>(height), srcInfo.height - sy);

    if (sw <= 0 || sh <= 0) {
        unlock(env, bitmap);
        return bitmap;
    }

    jobject newBmp = createBitmap(env, sw, sh);
    BmpInfo dstInfo;
    if (!lock(env, newBmp, dstInfo)) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* src = static_cast<uint32_t*>(srcInfo.pixels);
    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);

    for (int row = 0; row < sh; row++) {
        memcpy(dst + row * sw, src + (sy + row) * srcInfo.width + sx, sw * sizeof(uint32_t));
    }

    unlock(env, newBmp);
    unlock(env, bitmap);
    return newBmp;
}

// ── Rotate ─────────────────────────────────────────────────────────────────

jobject RotateBitmap(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat degrees) {
    BmpInfo srcInfo;
    if (!lock(env, bitmap, srcInfo)) return bitmap;

    const float rad = degrees * 3.14159265f / 180.0f;
    const float cosA = cosf(rad);
    const float sinA = sinf(rad);

    const int w = srcInfo.width;
    const int h = srcInfo.height;
    const float cx = w / 2.0f;
    const float cy = h / 2.0f;

    // Compute bounding box of rotated image
    const float corners[4][2] = {
        {-cx, -cy}, {cx, -cy}, {cx, cy}, {-cx, cy}
    };
    float minX = 1e9f, maxX = -1e9f, minY = 1e9f, maxY = -1e9f;
    for (auto& c : corners) {
        const float rx = c[0] * cosA - c[1] * sinA;
        const float ry = c[0] * sinA + c[1] * cosA;
        minX = fminf(minX, rx); maxX = fmaxf(maxX, rx);
        minY = fminf(minY, ry); maxY = fmaxf(maxY, ry);
    }

    const int nw = static_cast<int>(ceilf(maxX - minX));
    const int nh = static_cast<int>(ceilf(maxY - minY));
    const float ncx = nw / 2.0f;
    const float ncy = nh / 2.0f;

    jobject newBmp = createBitmap(env, nw, nh);
    BmpInfo dstInfo;
    if (!lock(env, newBmp, dstInfo)) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* src = static_cast<uint32_t*>(srcInfo.pixels);
    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);

    // Fill with transparent black
    memset(dst, 0, nw * nh * sizeof(uint32_t));

    // Reverse rotation: for each output pixel, find source pixel
    const float cosNeg = cosf(-rad);
    const float sinNeg = sinf(-rad);

    for (int oy = 0; oy < nh; oy++) {
        for (int ox = 0; ox < nw; ox++) {
            const float dx = ox - ncx;
            const float dy = oy - ncy;
            const float srcX = dx * cosNeg - dy * sinNeg + cx;
            const float srcY = dx * sinNeg + dy * cosNeg + cy;

            const int sx = static_cast<int>(srcX);
            const int sy = static_cast<int>(srcY);

            if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
                dst[oy * nw + ox] = src[sy * w + sx];
            }
        }
    }

    unlock(env, newBmp);
    unlock(env, bitmap);
    return newBmp;
}

// ── Flip ───────────────────────────────────────────────────────────────────

jobject FlipBitmap(JNIEnv* env, jobject /* thiz */, jobject bitmap, jboolean horizontal) {
    BmpInfo srcInfo;
    if (!lock(env, bitmap, srcInfo)) return bitmap;

    const int w = srcInfo.width;
    const int h = srcInfo.height;

    jobject newBmp = createBitmap(env, w, h);
    BmpInfo dstInfo;
    if (!lock(env, newBmp, dstInfo)) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* src = static_cast<uint32_t*>(srcInfo.pixels);
    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            const int dx = horizontal ? (w - 1 - x) : x;
            const int dy = horizontal ? y : (h - 1 - y);
            dst[y * w + x] = src[dy * w + dx];
        }
    }

    unlock(env, newBmp);
    unlock(env, bitmap);
    return newBmp;
}

// ── Resize (bilinear interpolation) ────────────────────────────────────────

jobject ResizeBitmap(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                     jint newWidth, jint newHeight) {
    BmpInfo srcInfo;
    if (!lock(env, bitmap, srcInfo)) return bitmap;

    const int w = srcInfo.width;
    const int h = srcInfo.height;
    const int nw = static_cast<int>(newWidth);
    const int nh = static_cast<int>(newHeight);

    if (nw <= 0 || nh <= 0) {
        unlock(env, bitmap);
        return bitmap;
    }

    jobject newBmp = createBitmap(env, nw, nh);
    BmpInfo dstInfo;
    if (!lock(env, newBmp, dstInfo)) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* src = static_cast<uint32_t*>(srcInfo.pixels);
    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);

    const float xRatio = static_cast<float>(w) / nw;
    const float yRatio = static_cast<float>(h) / nh;

    for (int y = 0; y < nh; y++) {
        for (int x = 0; x < nw; x++) {
            const float srcX = x * xRatio;
            const float srcY = y * yRatio;

            const int x0 = static_cast<int>(srcX);
            const int y0 = static_cast<int>(srcY);
            const int x1 = std::min(x0 + 1, w - 1);
            const int y1 = std::min(y0 + 1, h - 1);

            const float fx = srcX - x0;
            const float fy = srcY - y0;

            const uint32_t p00 = src[y0 * w + x0];
            const uint32_t p10 = src[y0 * w + x1];
            const uint32_t p01 = src[y1 * w + x0];
            const uint32_t p11 = src[y1 * w + x1];

            auto lerp = [](uint8_t a, uint8_t b, float t) -> uint8_t {
                return clamp8(static_cast<int>(a + t * (b - a)));
            };

            const uint8_t r = lerp(
                lerp((p00 >> 0) & 0xFF, (p10 >> 0) & 0xFF, fx),
                lerp((p01 >> 0) & 0xFF, (p11 >> 0) & 0xFF, fx), fy);
            const uint8_t g = lerp(
                lerp((p00 >> 8) & 0xFF, (p10 >> 8) & 0xFF, fx),
                lerp((p01 >> 8) & 0xFF, (p11 >> 8) & 0xFF, fx), fy);
            const uint8_t b = lerp(
                lerp((p00 >> 16) & 0xFF, (p10 >> 16) & 0xFF, fx),
                lerp((p01 >> 16) & 0xFF, (p11 >> 16) & 0xFF, fx), fy);
            const uint8_t a = lerp(
                lerp((p00 >> 24) & 0xFF, (p10 >> 24) & 0xFF, fx),
                lerp((p01 >> 24) & 0xFF, (p11 >> 24) & 0xFF, fx), fy);

            dst[y * nw + x] = (a << 24) | (b << 16) | (g << 8) | r;
        }
    }

    unlock(env, newBmp);
    unlock(env, bitmap);
    return newBmp;
}

} // namespace imageeditor
} // namespace neotools
