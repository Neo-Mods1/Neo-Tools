// ---------------------------------------------------------------------------
// Image Editor -> Filters, Histogram, Eyedropper, Curves, Clone, Blend.
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "imageeditor/filters.hpp"

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

// ── Filters ────────────────────────────────────────────────────────────────

jobject FilterGrayscale(JNIEnv* env, jobject /* thiz */, jobject bitmap) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;
    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const uint8_t r = (p >> 0) & 0xFF;
        const uint8_t g = (p >> 8) & 0xFF;
        const uint8_t b = (p >> 16) & 0xFF;
        const uint8_t gray = clamp8(static_cast<int>(0.299f * r + 0.587f * g + 0.114f * b));
        px[i] = (p & 0xFF000000) | (gray << 16) | (gray << 8) | gray;
    }
    unlock(env, bitmap);
    return bitmap;
}

jobject FilterSepia(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat intensity) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;
    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float t = std::max(0.0f, std::min(1.0f, intensity));
    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const float r = (p >> 0) & 0xFF;
        const float g = (p >> 8) & 0xFF;
        const float b = (p >> 16) & 0xFF;
        const float sr = r * 0.393f + g * 0.769f + b * 0.189f;
        const float sg = r * 0.349f + g * 0.686f + b * 0.168f;
        const float sb = r * 0.272f + g * 0.534f + b * 0.131f;
        const int nr = clamp8(static_cast<int>(r + (sr - r) * t));
        const int ng = clamp8(static_cast<int>(g + (sg - g) * t));
        const int nb = clamp8(static_cast<int>(b + (sb - b) * t));
        px[i] = (p & 0xFF000000) | (nb << 16) | (ng << 8) | nr;
    }
    unlock(env, bitmap);
    return bitmap;
}

jobject FilterInvert(JNIEnv* env, jobject /* thiz */, jobject bitmap) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;
    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        px[i] = (p & 0xFF000000) | (((p >> 16) & 0xFF) << 16) |
                (((p >> 8) & 0xFF) << 8) | ((p >> 0) & 0xFF);
        // Invert: 255 - channel
        const uint32_t inv = 0xFF000000 | ((255 - ((p >> 16) & 0xFF)) << 16) |
                             ((255 - ((p >> 8) & 0xFF)) << 8) | (255 - ((p >> 0) & 0xFF));
        px[i] = inv;
    }
    unlock(env, bitmap);
    return bitmap;
}

jobject FilterThreshold(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat threshold) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;
    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    const float thresh = std::max(0.0f, std::min(1.0f, threshold)) * 255.0f;
    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const uint8_t r = (p >> 0) & 0xFF;
        const uint8_t g = (p >> 8) & 0xFF;
        const uint8_t b = (p >> 16) & 0xFF;
        const float lum = 0.299f * r + 0.587f * g + 0.114f * b;
        const uint8_t v = (lum >= thresh) ? 255 : 0;
        px[i] = (p & 0xFF000000) | (v << 16) | (v << 8) | v;
    }
    unlock(env, bitmap);
    return bitmap;
}

jobject FilterBlur(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat radius) {
    if (radius < 0.5f) return bitmap;
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;

    const int w = info.width;
    const int h = info.height;
    auto* src = static_cast<uint32_t*>(info.pixels);
    auto* tmp = new uint32_t[w * h];
    const int r = static_cast<int>(radius);

    // Box blur (3 passes ≈ gaussian)
    for (int pass = 0; pass < 3; pass++) {
        auto* in = (pass == 0) ? src : tmp;
        auto* out = (pass < 2) ? tmp : src;

        // Horizontal
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float rr = 0, gg = 0, bb = 0, aa = 0;
                int count = 0;
                for (int k = -r; k <= r; k++) {
                    const int sx = std::min(std::max(x + k, 0), w - 1);
                    const uint32_t px2 = in[y * w + sx];
                    rr += (px2 >> 0) & 0xFF;
                    gg += (px2 >> 8) & 0xFF;
                    bb += (px2 >> 16) & 0xFF;
                    aa += (px2 >> 24) & 0xFF;
                    count++;
                }
                out[y * w + x] = (clamp8(static_cast<int>(aa / count)) << 24) |
                                  (clamp8(static_cast<int>(bb / count)) << 16) |
                                  (clamp8(static_cast<int>(gg / count)) << 8) |
                                  clamp8(static_cast<int>(rr / count));
            }
        }
        // Vertical
        auto* in2 = out;
        auto* out2 = (pass < 2) ? tmp : src;
        if (pass == 2) out2 = src;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float rr = 0, gg = 0, bb = 0, aa = 0;
                int count = 0;
                for (int k = -r; k <= r; k++) {
                    const int sy = std::min(std::max(y + k, 0), h - 1);
                    const uint32_t px2 = in2[sy * w + x];
                    rr += (px2 >> 0) & 0xFF;
                    gg += (px2 >> 8) & 0xFF;
                    bb += (px2 >> 16) & 0xFF;
                    aa += (px2 >> 24) & 0xFF;
                    count++;
                }
                out2[y * w + x] = (clamp8(static_cast<int>(aa / count)) << 24) |
                                   (clamp8(static_cast<int>(bb / count)) << 16) |
                                   (clamp8(static_cast<int>(gg / count)) << 8) |
                                   clamp8(static_cast<int>(rr / count));
            }
        }
    }

    delete[] tmp;
    unlock(env, bitmap);
    return bitmap;
}

jobject FilterPixelate(JNIEnv* env, jobject /* thiz */, jobject bitmap, jint blockSize) {
    if (blockSize <= 1) return bitmap;
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int w = info.width;
    const int h = info.height;
    const int bs = static_cast<int>(blockSize);

    for (int by = 0; by < h; by += bs) {
        for (int bx = 0; bx < w; bx += bs) {
            float rr = 0, gg = 0, bb = 0;
            int count = 0;
            for (int y = by; y < std::min(by + bs, h); y++) {
                for (int x = bx; x < std::min(bx + bs, w); x++) {
                    const uint32_t p = px[y * w + x];
                    rr += (p >> 0) & 0xFF;
                    gg += (p >> 8) & 0xFF;
                    bb += (p >> 16) & 0xFF;
                    count++;
                }
            }
            const uint8_t r = clamp8(static_cast<int>(rr / count));
            const uint8_t g = clamp8(static_cast<int>(gg / count));
            const uint8_t b = clamp8(static_cast<int>(bb / count));
            for (int y = by; y < std::min(by + bs, h); y++) {
                for (int x = bx; x < std::min(bx + bs, w); x++) {
                    const uint32_t p = px[y * w + x];
                    px[y * w + x] = (p & 0xFF000000) | (b << 16) | (g << 8) | r;
                }
            }
        }
    }

    unlock(env, bitmap);
    return bitmap;
}

jobject FilterEmboss(JNIEnv* env, jobject /* thiz */, jobject bitmap, jfloat intensity) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return bitmap;

    const int w = info.width;
    const int h = info.height;
    auto* src = static_cast<uint32_t*>(info.pixels);
    auto* dst = new uint32_t[w * h];

    // Emboss kernel
    const int kernel[3][3] = {
        {-2, -1, 0},
        {-1,  1, 1},
        { 0,  1, 2}
    };

    const float t = std::max(0.0f, std::min(2.0f, intensity));

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            float rr = 0, gg = 0, bb = 0;
            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    const int sx = std::min(std::max(x + kx, 0), w - 1);
                    const int sy = std::min(std::max(y + ky, 0), h - 1);
                    const uint32_t p = src[sy * w + sx];
                    const int k = kernel[ky + 1][kx + 1];
                    rr += ((p >> 0) & 0xFF) * k;
                    gg += ((p >> 8) & 0xFF) * k;
                    bb += ((p >> 16) & 0xFF) * k;
                }
            }
            const uint32_t orig = src[y * w + x];
            const int orr = (orig >> 0) & 0xFF;
            const int ogg = (orig >> 8) & 0xFF;
            const int obb = (orig >> 16) & 0xFF;
            const int nr = clamp8(static_cast<int>(orr + rr * t));
            const int ng = clamp8(static_cast<int>(ogg + gg * t));
            const int nb = clamp8(static_cast<int>(obb + bb * t));
            dst[y * w + x] = (orig & 0xFF000000) | (nb << 16) | (ng << 8) | nr;
        }
    }

    memcpy(src, dst, w * h * sizeof(uint32_t));
    delete[] dst;
    unlock(env, bitmap);
    return bitmap;
}

// ── Eyedropper ─────────────────────────────────────────────────────────────

jint GetPixelColor(JNIEnv* env, jobject /* thiz */, jobject bitmap, jint x, jint y) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return 0;

    const int px = std::min(std::max(static_cast<int>(x), 0), info.width - 1);
    const int py = std::min(std::max(static_cast<int>(y), 0), info.height - 1);
    auto* pixels = static_cast<uint32_t*>(info.pixels);
    const uint32_t color = pixels[py * info.width + px];

    unlock(env, bitmap);
    return static_cast<jint>(color);
}

// ── Histogram ──────────────────────────────────────────────────────────────

jintArray ComputeHistogram(JNIEnv* env, jobject /* thiz */, jobject bitmap) {
    BmpInfo info;
    if (!lock(env, bitmap, info)) return env->NewIntArray(0);

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;
    int histR[256] = {0};
    int histG[256] = {0};
    int histB[256] = {0};
    int histL[256] = {0};

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        histR[(p >> 0) & 0xFF]++;
        histG[(p >> 8) & 0xFF]++;
        histB[(p >> 16) & 0xFF]++;
        const uint8_t lum = clamp8(static_cast<int>(
            0.299f * ((p >> 0) & 0xFF) + 0.587f * ((p >> 8) & 0xFF) + 0.114f * ((p >> 16) & 0xFF)));
        histL[lum]++;
    }

    unlock(env, bitmap);

    jintArray result = env->NewIntArray(1024);
    jint buf[1024];
    memcpy(buf, histR, 256 * sizeof(int));
    memcpy(buf + 256, histG, 256 * sizeof(int));
    memcpy(buf + 512, histB, 256 * sizeof(int));
    memcpy(buf + 768, histL, 256 * sizeof(int));
    env->SetIntArrayRegion(result, 0, 1024, buf);
    return result;
}

// ── Curves LUT ─────────────────────────────────────────────────────────────

jobject ApplyCurvesLut(JNIEnv* env, jobject /* thiz */, jobject bitmap, jintArray lut) {
    if (lut == nullptr) return bitmap;

    jint* lutData = env->GetIntArrayElements(lut, nullptr);
    if (lutData == nullptr) return bitmap;

    BmpInfo info;
    if (!lock(env, bitmap, info)) {
        env->ReleaseIntArrayElements(lut, lutData, JNI_ABORT);
        return bitmap;
    }

    auto* px = static_cast<uint32_t*>(info.pixels);
    const int total = info.width * info.height;

    for (int i = 0; i < total; i++) {
        const uint32_t p = px[i];
        const int r = lutData[(p >> 0) & 0xFF];
        const int g = lutData[(p >> 8) & 0xFF];
        const int b = lutData[(p >> 16) & 0xFF];
        px[i] = (p & 0xFF000000) | (clamp8(b) << 16) | (clamp8(g) << 8) | clamp8(r);
    }

    env->ReleaseIntArrayElements(lut, lutData, JNI_ABORT);
    unlock(env, bitmap);
    return bitmap;
}

// ── Clone Stamp ────────────────────────────────────────────────────────────

jobject CloneStamp(JNIEnv* env, jobject /* thiz */,
                   jobject srcBitmap, jobject dstBitmap,
                   jint srcX, jint srcY, jint dstX, jint dstY,
                   jint width, jint height, jfloat blendAlpha) {

    BmpInfo srcInfo, dstInfo;
    if (!lock(env, srcBitmap, srcInfo)) return dstBitmap;
    if (!lock(env, dstBitmap, dstInfo)) {
        unlock(env, srcBitmap);
        return dstBitmap;
    }

    auto* src = static_cast<uint32_t*>(srcInfo.pixels);
    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);
    const float alpha = std::max(0.0f, std::min(1.0f, blendAlpha));

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            const int sx = srcX + x;
            const int sy = srcY + y;
            const int dx = dstX + x;
            const int dy = dstY + y;

            if (sx < 0 || sx >= srcInfo.width || sy < 0 || sy >= srcInfo.height) continue;
            if (dx < 0 || dx >= dstInfo.width || dy < 0 || dy >= dstInfo.height) continue;

            const uint32_t sp = src[sy * srcInfo.width + sx];
            const uint32_t dp = dst[dy * dstInfo.width + dx];

            const float sa = alpha;
            const float da = 1.0f - sa;
            const uint8_t r = clamp8(static_cast<int>(((sp >> 0) & 0xFF) * sa + ((dp >> 0) & 0xFF) * da));
            const uint8_t g = clamp8(static_cast<int>(((sp >> 8) & 0xFF) * sa + ((dp >> 8) & 0xFF) * da));
            const uint8_t b = clamp8(static_cast<int>(((sp >> 16) & 0xFF) * sa + ((dp >> 16) & 0xFF) * da));
            const uint8_t a = clamp8(static_cast<int>(((sp >> 24) & 0xFF) * sa + ((dp >> 24) & 0xFF) * da));

            dst[dy * dstInfo.width + dx] = (a << 24) | (b << 16) | (g << 8) | r;
        }
    }

    unlock(env, dstBitmap);
    unlock(env, srcBitmap);
    return dstBitmap;
}

// ── Blend Modes ────────────────────────────────────────────────────────────

static inline float blendChannel(float base, float overlay, int mode) {
    switch (mode) {
        case 0: return overlay;                                           // Normal
        case 1: return base * overlay;                                    // Multiply
        case 2: return 1.0f - (1.0f - base) * (1.0f - overlay);         // Screen
        case 3: return base < 0.5f ? 2*base*overlay :                     // Overlay
                     1.0f - 2*(1.0f-base)*(1.0f-overlay);
        case 4: return fminf(base, overlay);                              // Darken
        case 5: return fmaxf(base, overlay);                              // Lighten
        case 6: return base / (1.0f - overlay + 0.001f);                 // Color Dodge
        case 7: return 1.0f - (1.0f - base) / (overlay + 0.001f);       // Color Burn
        case 8: return overlay < 0.5f ?                                   // Soft Light
                     2*base*overlay + base*base*(1-2*overlay) :
                     2*base*(1-overlay) + sqrtf(base)*(2*overlay-1);
        case 9: return overlay < 0.5f ? 2*base*overlay :                  // Hard Light
                     1.0f - 2*(1.0f-base)*(1.0f-overlay);
        case 10: return fabsf(base - overlay);                            // Difference
        case 11: return base + overlay - 2*base*overlay;                  // Exclusion
        default: return overlay;
    }
}

jobject BlendBitmaps(JNIEnv* env, jobject /* thiz */,
                     jobject base, jobject overlay, jint mode, jfloat opacity) {
    BmpInfo baseInfo, overInfo;
    if (!lock(env, base, baseInfo)) return base;
    if (!lock(env, overlay, overInfo)) {
        unlock(env, base);
        return base;
    }

    auto* bpx = static_cast<uint32_t*>(baseInfo.pixels);
    auto* opx = static_cast<uint32_t*>(overInfo.pixels);
    const int count = std::min(baseInfo.width * baseInfo.height, overInfo.width * overInfo.height);
    const float alpha = std::max(0.0f, std::min(1.0f, opacity));

    for (int i = 0; i < count; i++) {
        const uint32_t bp = bpx[i];
        const uint32_t op = opx[i];

        const float ba = ((bp >> 24) & 0xFF) / 255.0f;
        const float br = ((bp >> 0) & 0xFF) / 255.0f;
        const float bg = ((bp >> 8) & 0xFF) / 255.0f;
        const float bb = ((bp >> 16) & 0xFF) / 255.0f;

        const float oa = ((op >> 24) & 0xFF) / 255.0f * alpha;
        const float orr = ((op >> 0) & 0xFF) / 255.0f;
        const float og = ((op >> 8) & 0xFF) / 255.0f;
        const float ob = ((op >> 16) & 0xFF) / 255.0f;

        const float rr = blendChannel(br, orr, mode);
        const float rg = blendChannel(bg, og, mode);
        const float rb = blendChannel(bb, ob, mode);

        const float outA = oa + ba * (1.0f - oa);
        const float outR = (rr * oa + br * ba * (1.0f - oa)) / (outA + 0.001f);
        const float outG = (rg * oa + bg * ba * (1.0f - oa)) / (outA + 0.001f);
        const float outB = (rb * oa + bb * ba * (1.0f - oa)) / (outA + 0.001f);

        bpx[i] = (clamp8(static_cast<int>(outA * 255)) << 24) |
                 (clamp8(static_cast<int>(outB * 255)) << 16) |
                 (clamp8(static_cast<int>(outG * 255)) << 8) |
                 clamp8(static_cast<int>(outR * 255));
    }

    unlock(env, overlay);
    unlock(env, base);
    return base;
}

} // namespace imageeditor
} // namespace neotools
