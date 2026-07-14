package com.neomods.tools.native

import android.graphics.Bitmap

/**
 * Thin JNI caller for the native `neotools` library.
 *
 * The actual tool implementations live in C++ (`src/main/cpp`). Kotlin never
 * re-implements the algorithm — it only marshals inputs/outputs across the JNI
 * boundary so the heavy lifting stays in native code.
 */
object NeoNative {

    init {
        System.loadLibrary("neotools")
    }

    // ── Encoding ────────────────────────────────────────────────────────

    external fun encodeBase64(input: ByteArray): String
    external fun decodeBase64(input: String): ByteArray
    external fun fileToHeader(data: ByteArray, filename: String): String
    external fun headerToFile(header: String): ByteArray
    external fun headerFileName(header: String): String

    // ── Image Editor: Adjustments ───────────────────────────────────────

    external fun nativeAdjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap
    external fun nativeAdjustContrast(bitmap: Bitmap, contrast: Float): Bitmap
    external fun nativeAdjustSaturation(bitmap: Bitmap, saturation: Float): Bitmap
    external fun nativeAdjustExposure(bitmap: Bitmap, exposure: Float): Bitmap
    external fun nativeAdjustWarmth(bitmap: Bitmap, warmth: Float): Bitmap
    external fun nativeAdjustHighlights(bitmap: Bitmap, highlights: Float): Bitmap
    external fun nativeAdjustShadows(bitmap: Bitmap, shadows: Float): Bitmap
    external fun nativeAdjustSharpness(bitmap: Bitmap, sharpness: Float): Bitmap
    external fun nativeAdjustVignette(bitmap: Bitmap, vignette: Float): Bitmap
    external fun nativeAdjustHue(bitmap: Bitmap, hue: Float): Bitmap

    external fun nativeApplyAllAdjustments(
        bitmap: Bitmap,
        brightness: Float, contrast: Float, saturation: Float, exposure: Float,
        warmth: Float, highlights: Float, shadows: Float,
        sharpness: Float, vignette: Float, hue: Float
    ): Bitmap

    // ── Image Editor: Crop / Transform ──────────────────────────────────

    external fun nativeCropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap
    external fun nativeRotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap
    external fun nativeFlipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap
    external fun nativeResizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap

    // ── Image Editor: Drawing ───────────────────────────────────────────

    external fun nativeRenderStroke(
        bitmap: Bitmap, points: FloatArray, numPoints: Int,
        color: Int, strokeWidth: Float, brushType: Int, opacity: Float
    ): Bitmap

    external fun nativeRenderStrokesBatch(
        bitmap: Bitmap, allPoints: FloatArray, strokeSizes: IntArray,
        numStrokes: Int, colors: IntArray, widths: FloatArray,
        brushTypes: IntArray, opacities: FloatArray
    ): Bitmap

    // ── Image Editor: Background Removal ────────────────────────────────

    external fun nativeRemoveBackground(bitmap: Bitmap, tolerance: Int, edgeSample: Int): Bitmap
    external fun nativeRemoveBackgroundByColor(bitmap: Bitmap, targetColor: Int, tolerance: Int): Bitmap

    // ── Image Editor: Shapes ───────────────────────────────────────────

    external fun nativeRenderShape(
        bitmap: Bitmap, shapeType: Int, width: Int, height: Int,
        fillColor: Int, strokeColor: Int, strokeWidth: Float,
        cornerRadius: Float, hasShadow: Boolean, shadowColor: Int,
        shadowRadius: Float, shadowOffsetX: Float, shadowOffsetY: Float,
        triangleDirection: Int, starPoints: Int, starInnerRadius: Float
    ): Bitmap

    // ── Image Editor: Filters ──────────────────────────────────────────

    external fun nativeFilterGrayscale(bitmap: Bitmap): Bitmap
    external fun nativeFilterSepia(bitmap: Bitmap, intensity: Float): Bitmap
    external fun nativeFilterInvert(bitmap: Bitmap): Bitmap
    external fun nativeFilterThreshold(bitmap: Bitmap, threshold: Float): Bitmap
    external fun nativeFilterBlur(bitmap: Bitmap, radius: Float): Bitmap
    external fun nativeFilterPixelate(bitmap: Bitmap, blockSize: Int): Bitmap
    external fun nativeFilterEmboss(bitmap: Bitmap, intensity: Float): Bitmap

    // ── Image Editor: Eyedropper ───────────────────────────────────────

    external fun nativeGetPixelColor(bitmap: Bitmap, x: Int, y: Int): Int

    // ── Image Editor: Histogram ────────────────────────────────────────

    external fun nativeComputeHistogram(bitmap: Bitmap): IntArray

    // ── Image Editor: Curves ───────────────────────────────────────────

    external fun nativeApplyCurvesLut(bitmap: Bitmap, lut: IntArray): Bitmap

    // ── Image Editor: Clone Stamp ──────────────────────────────────────

    external fun nativeCloneStamp(
        srcBitmap: Bitmap, dstBitmap: Bitmap,
        srcX: Int, srcY: Int, dstX: Int, dstY: Int,
        width: Int, height: Int, blendAlpha: Float
    ): Bitmap

    // ── Image Editor: Blend Modes ──────────────────────────────────────

    external fun nativeBlendBitmaps(base: Bitmap, overlay: Bitmap, mode: Int, opacity: Float): Bitmap

    // ── APK Tools ──────────────────────────────────────────────────────

    external fun nativeParseApkInfo(apkPath: String): String
    external fun nativeParseManifest(apkPath: String): String
    external fun nativeGetManifestXml(apkPath: String): String
    external fun nativeParseCertificate(apkPath: String): String
    external fun nativeGetNativeLibs(apkPath: String): String
    external fun nativeGetZipEntries(apkPath: String): String
}
