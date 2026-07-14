// ---------------------------------------------------------------------------
// Image Editor -> Background Removal.
//
// Two approaches:
// 1. Auto-detect: samples edge pixels to find dominant background color,
//    then flood-fills from the edges removing similar colors.
// 2. Color key: removes all pixels matching a specific color.
//
// Both produce a new bitmap with transparent background (ARGB_8888).
// ---------------------------------------------------------------------------

#include "obfuscate.h"
#include "imageeditor/bgremove.hpp"

#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>
#include <cstring>
#include <algorithm>
#include <queue>
#include <vector>

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

// ── Color distance (weighted Euclidean in RGB) ─────────────────────────────

static inline float colorDistance(uint8_t r1, uint8_t g1, uint8_t b1,
                                   uint8_t r2, uint8_t g2, uint8_t b2) {
    const float dr = r1 - r2;
    const float dg = g1 - g2;
    const float db = b1 - b2;
    // Weights approximate human perception
    return sqrtf(2.0f * dr * dr + 4.0f * dg * dg + 3.0f * db * db);
}

// ── Sample dominant color from image edges ─────────────────────────────────

static void sampleEdgeColor(const uint32_t* pixels, int w, int h,
                             int sampleCount,
                             uint8_t& outR, uint8_t& outG, uint8_t& outB) {
    // Collect pixels from all 4 edges
    struct Sample { uint8_t r, g, b; };
    std::vector<Sample> samples;
    samples.reserve(sampleCount * 4);

    const int step = std::max(1, (w + h) * 2 / sampleCount);

    // Top edge
    for (int x = 0; x < w; x += step) {
        const uint32_t p = pixels[x];
        samples.push_back({(uint8_t)((p >> 0) & 0xFF),
                           (uint8_t)((p >> 8) & 0xFF),
                           (uint8_t)((p >> 16) & 0xFF)});
    }
    // Bottom edge
    for (int x = 0; x < w; x += step) {
        const uint32_t p = pixels[(h - 1) * w + x];
        samples.push_back({(uint8_t)((p >> 0) & 0xFF),
                           (uint8_t)((p >> 8) & 0xFF),
                           (uint8_t)((p >> 16) & 0xFF)});
    }
    // Left edge
    for (int y = 0; y < h; y += step) {
        const uint32_t p = pixels[y * w];
        samples.push_back({(uint8_t)((p >> 0) & 0xFF),
                           (uint8_t)((p >> 8) & 0xFF),
                           (uint8_t)((p >> 16) & 0xFF)});
    }
    // Right edge
    for (int y = 0; y < h; y += step) {
        const uint32_t p = pixels[y * w + (w - 1)];
        samples.push_back({(uint8_t)((p >> 0) & 0xFF),
                           (uint8_t)((p >> 8) & 0xFF),
                           (uint8_t)((p >> 16) & 0xFF)});
    }

    // Simple k-means with k=3 to find dominant color cluster
    // Initialize with 3 random samples
    if (samples.size() < 3) {
        outR = outG = outB = 0;
        return;
    }

    uint8_t centroids[3][3];
    centroids[0][0] = samples[0].r; centroids[0][1] = samples[0].g; centroids[0][2] = samples[0].b;
    centroids[1][0] = samples[samples.size() / 3].r;
    centroids[1][1] = samples[samples.size() / 3].g;
    centroids[1][2] = samples[samples.size() / 3].b;
    centroids[2][0] = samples[samples.size() * 2 / 3].r;
    centroids[2][1] = samples[samples.size() * 2 / 3].g;
    centroids[2][2] = samples[samples.size() * 2 / 3].b;

    int counts[3] = {0, 0, 0};
    float sumR[3] = {0, 0, 0};
    float sumG[3] = {0, 0, 0};
    float sumB[3] = {0, 0, 0};

    for (int iter = 0; iter < 5; iter++) {
        std::memset(counts, 0, sizeof(counts));
        sumR[0] = sumR[1] = sumR[2] = 0;
        sumG[0] = sumG[1] = sumG[2] = 0;
        sumB[0] = sumB[1] = sumB[2] = 0;

        for (const auto& s : samples) {
            float bestDist = 1e9f;
            int bestCluster = 0;
            for (int c = 0; c < 3; c++) {
                const float d = colorDistance(s.r, s.g, s.b,
                    centroids[c][0], centroids[c][1], centroids[c][2]);
                if (d < bestDist) {
                    bestDist = d;
                    bestCluster = c;
                }
            }
            counts[bestCluster]++;
            sumR[bestCluster] += s.r;
            sumG[bestCluster] += s.g;
            sumB[bestCluster] += s.b;
        }

        for (int c = 0; c < 3; c++) {
            if (counts[c] > 0) {
                centroids[c][0] = static_cast<uint8_t>(sumR[c] / counts[c]);
                centroids[c][1] = static_cast<uint8_t>(sumG[c] / counts[c]);
                centroids[c][2] = static_cast<uint8_t>(sumB[c] / counts[c]);
            }
        }
    }

    // Pick the cluster with the most samples (most common edge color)
    int bestCluster = 0;
    for (int c = 1; c < 3; c++) {
        if (counts[c] > counts[bestCluster]) bestCluster = c;
    }

    outR = centroids[bestCluster][0];
    outG = centroids[bestCluster][1];
    outB = centroids[bestCluster][2];
}

// ── Flood fill from edges ─────────────────────────────────────────────────

static void floodFillEdges(uint32_t* pixels, bool* visited,
                            int w, int h,
                            uint8_t tR, uint8_t tG, uint8_t tB,
                            int tolerance) {
    std::queue<std::pair<int,int>> queue;

    // Seed all edge pixels that match the target color
    for (int x = 0; x < w; x++) {
        // Top edge
        {
            const uint32_t p = pixels[x];
            const float d = colorDistance((p>>0)&0xFF, (p>>8)&0xFF, (p>>16)&0xFF, tR, tG, tB);
            if (d <= tolerance && !visited[x]) {
                visited[x] = true;
                pixels[x] = 0; // make transparent
                queue.push({x, 0});
            }
        }
        // Bottom edge
        {
            const int idx = (h-1)*w + x;
            const uint32_t p = pixels[idx];
            const float d = colorDistance((p>>0)&0xFF, (p>>8)&0xFF, (p>>16)&0xFF, tR, tG, tB);
            if (d <= tolerance && !visited[idx]) {
                visited[idx] = true;
                pixels[idx] = 0;
                queue.push({x, h-1});
            }
        }
    }
    for (int y = 0; y < h; y++) {
        // Left edge
        {
            const uint32_t p = pixels[y*w];
            const float d = colorDistance((p>>0)&0xFF, (p>>8)&0xFF, (p>>16)&0xFF, tR, tG, tB);
            if (d <= tolerance && !visited[y*w]) {
                visited[y*w] = true;
                pixels[y*w] = 0;
                queue.push({0, y});
            }
        }
        // Right edge
        {
            const int idx = y*w + (w-1);
            const uint32_t p = pixels[idx];
            const float d = colorDistance((p>>0)&0xFF, (p>>8)&0xFF, (p>>16)&0xFF, tR, tG, tB);
            if (d <= tolerance && !visited[idx]) {
                visited[idx] = true;
                pixels[idx] = 0;
                queue.push({w-1, y});
            }
        }
    }

    // BFS flood fill
    const int dx[] = {0, 0, 1, -1};
    const int dy[] = {1, -1, 0, 0};

    while (!queue.empty()) {
        const auto [cx, cy] = queue.front();
        queue.pop();

        for (int d = 0; d < 4; d++) {
            const int nx = cx + dx[d];
            const int ny = cy + dy[d];
            if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;

            const int idx = ny * w + nx;
            if (visited[idx]) continue;

            const uint32_t p = pixels[idx];
            const float dist = colorDistance((p>>0)&0xFF, (p>>8)&0xFF, (p>>16)&0xFF, tR, tG, tB);

            if (dist <= tolerance) {
                visited[idx] = true;
                pixels[idx] = 0; // make transparent
                queue.push({nx, ny});
            }
        }
    }
}

// ── Public API ─────────────────────────────────────────────────────────────

jobject RemoveBackground(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                          jint tolerance, jint edgeSample) {
    BmpInfo srcInfo;
    if (!lock(env, bitmap, srcInfo)) return bitmap;

    const int w = srcInfo.width;
    const int h = srcInfo.height;
    auto* src = static_cast<uint32_t*>(srcInfo.pixels);

    // Sample dominant edge color
    uint8_t tR, tG, tB;
    sampleEdgeColor(src, w, h, static_cast<int>(edgeSample), tR, tG, tB);

    // Create output bitmap (copy of source)
    jobject newBmp = createBitmap(env, w, h);
    BmpInfo dstInfo;
    if (!lock(env, newBmp, dstInfo)) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);
    memcpy(dst, src, w * h * sizeof(uint32_t));

    // Flood fill from edges
    auto* visited = new bool[w * h]();
    floodFillEdges(dst, visited, w, h, tR, tG, tB, static_cast<int>(tolerance));
    delete[] visited;

    unlock(env, newBmp);
    unlock(env, bitmap);
    return newBmp;
}

jobject RemoveBackgroundByColor(JNIEnv* env, jobject /* thiz */, jobject bitmap,
                                 jint targetColor, jint tolerance) {
    BmpInfo srcInfo;
    if (!lock(env, bitmap, srcInfo)) return bitmap;

    const int w = srcInfo.width;
    const int h = srcInfo.height;
    auto* src = static_cast<uint32_t*>(srcInfo.pixels);

    const uint8_t tR = (targetColor >> 16) & 0xFF;
    const uint8_t tG = (targetColor >> 8) & 0xFF;
    const uint8_t tB = targetColor & 0xFF;

    jobject newBmp = createBitmap(env, w, h);
    BmpInfo dstInfo;
    if (!lock(env, newBmp, dstInfo)) {
        unlock(env, bitmap);
        return bitmap;
    }

    auto* dst = static_cast<uint32_t*>(dstInfo.pixels);

    for (int i = 0; i < w * h; i++) {
        const uint32_t p = src[i];
        const float d = colorDistance((p>>0)&0xFF, (p>>8)&0xFF, (p>>16)&0xFF, tR, tG, tB);
        if (d <= tolerance) {
            dst[i] = 0; // transparent
        } else {
            dst[i] = p;
        }
    }

    unlock(env, newBmp);
    unlock(env, bitmap);
    return newBmp;
}

} // namespace imageeditor
} // namespace neotools
