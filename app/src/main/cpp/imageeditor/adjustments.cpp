// ---------------------------------------------------------------------------
// Image Editor -> Adjustments (brightness, contrast, saturation, exposure,
// warmth, highlights, shadows, sharpness, vignette, hue).
//
// All pixel manipulation happens here in C++. Kotlin only passes the bitmap
// and float parameters across JNI. Premium checks are handled at the
// JNI registration level before reaching this code.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "imageeditor/adjustments.hpp"

#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <algorithm>

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

namespace neotools {
namespace imageeditor {

// ── helpers ────────────────────────────────────────────────────────────────

static inline uint8_t clamp8(int v) {
    return static_cast<uint8_t>(v < 0 ? 0 : (v > 255 ? 255 : v));
}

static inline float clampf(float v, float lo, float hi) {
    return v < lo ? lo : (v > hi ? hi : v);
}

struct BitmapInfo {
    void* pixels = nullptr;
    int width = 0;
    int height = 0;
    AndroidBitmapFormat format = ANDROID_BITMAP_FORMAT_RGBA_8888;
    int stride = 0;
};

static bool lockBitmap(JNIEnv* env, jobject bitmap, BitmapInfo& info) {
    AndroidBitmapInfo bmpInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bmpInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return false;
    }
    info.width = static_cast<int>(bmpInfo.width);
    info.height = static_cast<int>(bmpInfo.height);
    info.format = static_cast<AndroidBitmapFormat>(bmpInfo.format);
    info.stride = static_cast<int>(bmpInfo.stride);

    if (AndroidBitmap_lockPixels(env, bitmap, &info.pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return false;
    }
    return info.pixels != nullptr;
}

static void unlockBitmap(JNIEnv* env, jobject bitmap) {
    AndroidBitmap_unlockPixels(env, bitmap);
}

// Create a new mutable bitmap with same dimensions
static jobject createSameBitmap(JNIEnv* env, jobject src, BitmapInfo& srcInfo) {
    jclass bitmapClass = env->FindClass(OBFUSCATE("android/graphics/Bitmap"));
    jmethodID createMethod = env->GetStaticMethodID(bitmapClass,
        OBFUSCATE("createBitmap"),
        OBFUSCATE("(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"));

    jclass configClass = env->FindClass(OBFUSCATE("android/graphics/Bitmap$Config"));
    jfieldID argbField = env->GetStaticFieldID(configClass,
        OBFUSCATE("ARGB_8888"), OBFUSCATE("Landroid/graphics/Bitmap$Config;"));
    jobject config = env->GetStaticObjectField(configClass, argbField);

    return env->CallStaticObjectMethod(bitmapClass, createMethod,
        srcInfo.width, srcInfo.height, config);
}

// ── RGB <-> HSV ────────────────────────────────────────────────────────────

static void rgbToHsv(uint8_t r, uint8_t g, uint8_t b, float& h, float& s, float& v) {
    const float rf = r / 255.0f;
    const float gf = g / 255.0f;
    const float bf = b / 255.0f;

    const float cmax = fmaxf(rf, fmaxf(gf, bf));
    const float cmin = fminf(rf, fminf(gf, bf));
    const float delta = cmax - cmin;

    v = cmax;
    s = (cmax == 0.0f) ? 0.0f : delta / cmax;

    if (delta < 0.001f) {
        h = 0.0f;
    } else if (cmax == rf) {
        h = 60.0f * fmodf((gf - bf) / delta, 6.0f);
    } else if (cmax == gf) {
        h = 60.0f * ((bf - rf) / delta + 2.0f);
    } else {
        h = 60.0f * ((rf - gf) / delta + 4.0f);
    }
    if (h < 0.0f) h += 360.0f;
}

static void hsvToRgb(float h, float s, float v, uint8_t& r, uint8_t& g, uint8_t& b) {
    const float c = v * s;
    const float x = c * (1.0f - fabsf(fmodf(h / 60.0f, 2.0f) - 1.0f));
    const float m = v - c;

    float rf, gf, bf;
    if (h < 60)       { rf = c; gf = x; bf = 0; }
    else if (h < 120) { rf = x; gf = c; bf = 0; }
    else if (h < 180) { rf = 0; gf = c; bf = x; }
    else if (h < 240) { rf = 0; gf = x; bf = c; }
    else if (h < 300) { rf = x; gf = 0; bf = c; }
    else               { rf = c; gf = 0; bf = x; }

    r = clamp8(static_cast<int>((rf + m) * 255.0f));
    g = clamp8(static_cast<int>((gf + m) * 255.0f));
    b = clamp8(static_cast<int>((bf + m) * 255.0f));
}

// ── Luminance for highlights/shadows ───────────────────────────────────────

static float luminance(uint8_t r, uint8_t g, uint8_t b) {
    return 0.299f * r + 0.587f * g + 0.114f * b;
}

// ── Gaussian blur (for unsharp mask / sharpness) ───────────────────────────

static void gaussianBlur(const uint32_t* src, uint32_t* dst,
                          int w, int h, int radius) {
    const int size = radius * 2 + 1;
    const float sigma = radius / 2.0f;
    const float sigma2 = 2.0f * sigma * sigma;
    float kernel[256];
    float sum = 0;
    for (int i = 0; i < size && i < 256; i++) {
        const int x = i - radius;
        kernel[i] = expf(-(x * x) / sigma2);
        sum += kernel[i];
    }
    for (int i = 0; i < size && i < 256; i++) kernel[i] /= sum;

    // Horizontal pass
    auto* tmp = new uint32_t[w * h];
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            float r = 0, g = 0, b = 0, a = 0;
            for (int k = -radius; k <= radius; k++) {
                const int sx = std::min(std::max(x + k, 0), w - 1);
                const uint32_t px = src[y * w + sx];
                const float wt = kernel[k + radius];
                r += ((px >> 0) & 0xFF) * wt;
                g += ((px >> 8) & 0xFF) * wt;
                b += ((px >> 16) & 0xFF) * wt;
                a += ((px >> 24) & 0xFF) * wt;
            }
            tmp[y * w + x] = (clamp8(static_cast<int>(a)) << 24) |
                              (clamp8(static_cast<int>(b)) << 16) |
                              (clamp8(static_cast<int>(g)) << 8) |
                              clamp8(static_cast<int>(r));
        }
    }

    // Vertical pass
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            float r = 0, g = 0, b = 0, a = 0;
            for (int k = -radius; k <= radius; k++) {
                const int sy = std::min(std::max(y + k, 0), h - 1);
                const uint32_t px = tmp[sy * w + x];
                const float wt = kernel[k + radius];
                r += ((px >> 0) & 0xFF) * wt;
                g += ((px >> 8) & 0xFF) * wt;
                b += ((px >> 16) & 0xFF) * wt;
                a += ((px >> 24) & 0xFF) * wt;
            }
            dst[y * w + x] = (clamp8(static_cast<int>(a)) << 24) |
                              (clamp8(static_cast<int>(b)) << 16) |
                              (clamp8(static_cast<int>(g)) << 8) |
                              clamp8(static_cast<int>(r));
        }
    }
    delete[] tmp;
}

// ── Individual adjustments ─────────────────────────────────────────────────

jobject AdjustBrightness(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat brightness) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const int shift = static_cast<int>(brightness * 255.0f);

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const int r = ((p >> 0) & 0xFF) + shift;
        const int g = ((p >> 8) & 0xFF) + shift;
        const int b = ((p >> 16) & 0xFF) + shift;
        px[i] = (p & 0xFF000000) | (clamp8(b) << 16) | (clamp8(g) << 8) | clamp8(r);
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustContrast(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat contrast) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float factor = (259.0f * (contrast * 255.0f + 255.0f)) / (255.0f * (259.0f - contrast * 255.0f));

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const int r = clamp8(static_cast<int>(factor * (((p >> 0) & 0xFF) - 128) + 128));
        const int g = clamp8(static_cast<int>(factor * (((p >> 8) & 0xFF) - 128) + 128));
        const int b = clamp8(static_cast<int>(factor * (((p >> 16) & 0xFF) - 128) + 128));
        px[i] = (p & 0xFF000000) | (b << 16) | (g << 8) | r;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustSaturation(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat saturation) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const uint8_t r = (p >> 0) & 0xFF;
        const uint8_t g = (p >> 8) & 0xFF;
        const uint8_t b = (p >> 16) & 0xFF;

        const float lum = 0.299f * r + 0.587f * g + 0.114f * b;
        const int nr = clamp8(static_cast<int>(lum + saturation * (r - lum)));
        const int ng = clamp8(static_cast<int>(lum + saturation * (g - lum)));
        const int nb = clamp8(static_cast<int>(lum + saturation * (b - lum)));

        px[i] = (p & 0xFF000000) | (nb << 16) | (ng << 8) | nr;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustExposure(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat exposure) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float factor = powf(2.0f, exposure * 3.0f); // +/-3 stops

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const int r = clamp8(static_cast<int>(((p >> 0) & 0xFF) * factor));
        const int g = clamp8(static_cast<int>(((p >> 8) & 0xFF) * factor));
        const int b = clamp8(static_cast<int>(((p >> 16) & 0xFF) * factor));
        px[i] = (p & 0xFF000000) | (b << 16) | (g << 8) | r;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustWarmth(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat warmth) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const int warmShift = static_cast<int>(warmth * 30.0f);

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const int r = clamp8(((p >> 0) & 0xFF) + warmShift);
        const int g = clamp8(((p >> 8) & 0xFF) + warmShift / 2);
        const int b = clamp8(((p >> 16) & 0xFF) - warmShift);
        px[i] = (p & 0xFF000000) | (b << 16) | (g << 8) | r;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustHighlights(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat highlights) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float shift = highlights * 60.0f;

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const uint8_t r = (p >> 0) & 0xFF;
        const uint8_t g = (p >> 8) & 0xFF;
        const uint8_t b = (p >> 16) & 0xFF;
        const float lum = luminance(r, g, b);

        // Only affect bright areas (luminance > 128)
        const float mask = clampf((lum - 128.0f) / 127.0f, 0.0f, 1.0f);
        const int nr = clamp8(static_cast<int>(r + shift * mask));
        const int ng = clamp8(static_cast<int>(g + shift * mask));
        const int nb = clamp8(static_cast<int>(b + shift * mask));
        px[i] = (p & 0xFF000000) | (nb << 16) | (ng << 8) | nr;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustShadows(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat shadows) {
    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float shift = shadows * 60.0f;

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const uint8_t r = (p >> 0) & 0xFF;
        const uint8_t g = (p >> 8) & 0xFF;
        const uint8_t b = (p >> 16) & 0xFF;
        const float lum = luminance(r, g, b);

        // Only affect dark areas (luminance < 128)
        const float mask = clampf((128.0f - lum) / 127.0f, 0.0f, 1.0f);
        const int nr = clamp8(static_cast<int>(r + shift * mask));
        const int ng = clamp8(static_cast<int>(g + shift * mask));
        const int nb = clamp8(static_cast<int>(b + shift * mask));
        px[i] = (p & 0xFF000000) | (nb << 16) | (ng << 8) | nr;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustSharpness(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat sharpness) {
    if (sharpness <= 0.01f) return bitmap;

    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    const int w = info.width;
    const int h = info.height;
    auto* src = static_cast<uint32_t*>(info.pixels);

    auto* blurred = new uint32_t[w * h];
    const int radius = std::min(static_cast<int>(sharpness * 5) + 1, 10);
    gaussianBlur(src, blurred, w, h, radius);

    // Unsharp mask: original + (original - blurred) * amount
    const float amount = sharpness * 2.0f;
    for (int i = 0; i < w * h; i++) {
        const uint32_t orig = src[i];
        const uint32_t blur = blurred[i];

        const int r = clamp8(static_cast<int>(((orig >> 0) & 0xFF) +
            amount * (((orig >> 0) & 0xFF) - ((blur >> 0) & 0xFF))));
        const int g = clamp8(static_cast<int>(((orig >> 8) & 0xFF) +
            amount * (((orig >> 8) & 0xFF) - ((blur >> 8) & 0xFF))));
        const int b = clamp8(static_cast<int>(((orig >> 16) & 0xFF) +
            amount * (((orig >> 16) & 0xFF) - ((blur >> 16) & 0xFF))));

        src[i] = (orig & 0xFF000000) | (b << 16) | (g << 8) | r;
    }

    delete[] blurred;
    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustVignette(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat vignette) {
    if (vignette <= 0.01f) return bitmap;

    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int w = info.width;
    const int h = info.height;
    const float cx = w / 2.0f;
    const float cy = h / 2.0f;
    const float maxDist = sqrtf(cx * cx + cy * cy);

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            const float dx = x - cx;
            const float dy = y - cy;
            const float dist = sqrtf(dx * dx + dy * dy) / maxDist;
            const float darkening = 1.0f - vignette * dist * dist;

            const int i = y * w + x;
            const uint32_t p = px[i];
            const int r = clamp8(static_cast<int>(((p >> 0) & 0xFF) * darkening));
            const int g = clamp8(static_cast<int>(((p >> 8) & 0xFF) * darkening));
            const int b = clamp8(static_cast<int>(((p >> 16) & 0xFF) * darkening));
            px[i] = (p & 0xFF000000) | (b << 16) | (g << 8) | r;
        }
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject AdjustHue(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat hue) {
    if (fabsf(hue) < 0.001f) return bitmap;

    BitmapInfo info;
    if (!lockBitmap(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float hueShift = hue * 360.0f;

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        float h, s, v;
        rgbToHsv((p >> 0) & 0xFF, (p >> 8) & 0xFF, (p >> 16) & 0xFF, h, s, v);

        h += hueShift;
        if (h >= 360.0f) h -= 360.0f;
        if (h < 0.0f) h += 360.0f;

        uint8_t r, g, b;
        hsvToRgb(h, s, v, r, g, b);
        px[i] = (p & 0xFF000000) | (b << 16) | (g << 8) | r;
    }

    unlockBitmap(env, bitmap);
    return bitmap;
}

jobject ApplyAllAdjustments(JNIEnv* env, jobject thiz, jobject bitmap,
    jfloat brightness, jfloat contrast, jfloat saturation, jfloat exposure,
    jfloat warmth, jfloat highlights, jfloat shadows,
    jfloat sharpness, jfloat vignette, jfloat hue) {

    // Apply in optimal order: exposure -> brightness -> contrast ->
    // highlights/shadows -> warmth -> saturation -> hue -> sharpness -> vignette
    bitmap = AdjustExposure(env, thiz, bitmap, exposure);
    bitmap = AdjustBrightness(env, thiz, bitmap, brightness);
    bitmap = AdjustContrast(env, thiz, bitmap, contrast);
    bitmap = AdjustHighlights(env, thiz, bitmap, highlights);
    bitmap = AdjustShadows(env, thiz, bitmap, shadows);
    bitmap = AdjustWarmth(env, thiz, bitmap, warmth);
    bitmap = AdjustSaturation(env, thiz, bitmap, saturation);
    bitmap = AdjustHue(env, thiz, bitmap, hue);
    bitmap = AdjustSharpness(env, thiz, bitmap, sharpness);
    bitmap = AdjustVignette(env, thiz, bitmap, vignette);
    return bitmap;
}

} // namespace imageeditor
} // namespace neotools
