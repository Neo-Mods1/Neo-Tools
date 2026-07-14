// ---------------------------------------------------------------------------
// JNI root: loads the native library and registers every tool's native methods.
//
// Each category owns its own folder (e.g. `encoding/`) exposing its native
// implementation in a `neotools::<category>` namespace. The actual
// registration of those methods against their Kotlin counterparts happens HERE,
// in JNI_OnLoad, so each category's translation unit only defines the
// implementation and never touches registration. Future categories
// (Hashing, Ciphers, ...) just add a folder, expose its function in the
// header, and get a Register call below.
// ---------------------------------------------------------------------------

#include <jni.h>
#include <android/log.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <ucontext.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>

#include "obfuscate.h"
#include "encoding/base64.hpp"
#include "encoding/cpp_converter.hpp"
#include "imageeditor/adjustments.hpp"
#include "imageeditor/crop.hpp"
#include "imageeditor/drawing.hpp"
#include "imageeditor/bgremove.hpp"
#include "imageeditor/shapes.hpp"
#include "imageeditor/filters.hpp"
#include "apk/apk_parser.hpp"

#ifndef LOG_TAG
#define LOG_TAG "NeoTools"
#endif

static struct sigaction old_sa[NSIG];

static const char* signalName(int sig) {
    switch (sig) {
        case SIGSEGV: return "SIGSEGV";
        case SIGABRT: return "SIGABRT";
        case SIGBUS:  return "SIGBUS";
        case SIGFPE:  return "SIGFPE";
        case SIGILL:  return "SIGILL";
        default:      return "UNKNOWN";
    }
}

static std::string getCrashFilePath() {
    char pkg[256] = {0};

    int fd = open("/proc/self/cmdline", O_RDONLY);
    if (fd >= 0) {
        ssize_t len = read(fd, pkg, sizeof(pkg) - 1);
        close(fd);

        if (len > 0) {
            pkg[len] = '\0';
            return std::string("/data/data/") + pkg + "/files/native_crash.txt";
        }
    }

    // Fallback
    return "/data/local/tmp/native_crash.txt";
}

static void nativeCrashHandler(int sig, siginfo_t* info, void* ucontext) {
    // Restore default handler so a second crash isn't trapped
    sigaction(sig, &old_sa[sig], nullptr);

    int fd = -1;

    // Write crash info to a temp file the Kotlin side will read
    std::string crashPath = getCrashFilePath();

    fd = open(crashPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd >= 0) {
        const char* header =
            "========================================\n"
            "  Neo Tools - Native Crash Report\n"
            "========================================\n\n";
        write(fd, header, strlen(header));

        const char* sigLine = "Signal: ";
        write(fd, sigLine, strlen(sigLine));
        const char* name = signalName(sig);
        write(fd, name, strlen(name));
        write(fd, "\n", 1);

        // Fault address
        if (info && info->si_addr) {
            const char* addrLine = "Fault addr: ";
            write(fd, addrLine, strlen(addrLine));
            char hex[20];
            snprintf(hex, sizeof(hex), "%p", info->si_addr);
            write(fd, hex, strlen(hex));
            write(fd, "\n", 1);
        }

        // Signal code
        if (info) {
            const char* codeLine = "Signal code: ";
            write(fd, codeLine, strlen(codeLine));
            char codeBuf[16];
            snprintf(codeBuf, sizeof(codeBuf), "%d", info->si_code);
            write(fd, codeBuf, strlen(codeBuf));
            write(fd, "\n", 1);
        }

        // PC register from ucontext
        if (ucontext) {
            ucontext_t* uc = (ucontext_t*)ucontext;
            const char* pcLine = "PC: 0x";
            write(fd, pcLine, strlen(pcLine));
            char pcHex[20];
#if defined(__aarch64__)
            snprintf(pcHex, sizeof(pcHex), "%llx",
                     (unsigned long long)uc->uc_mcontext.pc);
#elif defined(__arm__)
            snprintf(pcHex, sizeof(pcHex), "%llx",
                     (unsigned long long)uc->uc_mcontext.arm_pc);
#else
            snprintf(pcHex, sizeof(pcHex), "unknown");
#endif
            write(fd, pcHex, strlen(pcHex));
            write(fd, "\n", 1);
        }

        write(fd, "\n", 1);

        // Grab last 64 logcat error lines
        const char* logcatCmd =
            "logcat -d -t 64 *:E 2>/dev/null";
        const char* logcatHeader =
            "========================================\n"
            "  Logcat (last 64 lines)\n"
            "========================================\n\n";
        write(fd, logcatHeader, strlen(logcatHeader));

        FILE* pipe = popen(logcatCmd, "r");
        if (pipe) {
            char line[512];
            while (fgets(line, sizeof(line), pipe)) {
                write(fd, line, strlen(line));
            }
            pclose(pipe);
        }

        close(fd);
    }

    // Raise the signal again so the process dies normally (and Android's
    // tombstone handler can write the full core dump if needed)
    tgkill(getpid(), gettid(), sig);
}

static void installSignalHandlers() {
    static const int signals[] = { SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL };
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = nativeCrashHandler;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);

    for (int sig : signals) {
        sigaction(sig, &sa, &old_sa[sig]);
    }
}

namespace {

int RegisterEncoding(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("encodeBase64"),  OBFUSCATE("([B)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::EncodeBase64) },
        { OBFUSCATE("decodeBase64"),  OBFUSCATE("(Ljava/lang/String;)[B"),
          reinterpret_cast<void*>(neotools::encoding::DecodeBase64) }
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

int RegisterCppConverter(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("fileToHeader"),  OBFUSCATE("([BLjava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::FileToHeader) },
        { OBFUSCATE("headerToFile"),  OBFUSCATE("(Ljava/lang/String;)[B"),
          reinterpret_cast<void*>(neotools::encoding::HeaderToFile) },
        { OBFUSCATE("headerFileName"), OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::encoding::HeaderFileName) }
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

} // namespace

// Check for a native crash file written by the signal handler.
// Returns the crash log content as a Java String, or null if no crash occurred.
static jstring checkNativeCrashFile(JNIEnv* env, jobject /* this */) {
    const char* crashPath = "/data/data/com.neomods.tools/files/native_crash.txt";
    int fd = open(crashPath, O_RDONLY);
    if (fd < 0) return nullptr;

    struct stat st;
    if (fstat(fd, &st) != 0 || st.st_size == 0) {
        close(fd);
        unlink(crashPath);
        return nullptr;
    }

    // Cap at 64KB
    size_t len = (st.st_size > 65536) ? 65536 : (size_t)st.st_size;
    char* buf = new char[len + 1];
    ssize_t bytesRead = read(fd, buf, len);
    close(fd);
    unlink(crashPath);

    if (bytesRead <= 0) {
        delete[] buf;
        return nullptr;
    }
    buf[bytesRead] = '\0';

    jstring result = env->NewStringUTF(buf);
    delete[] buf;
    return result;
}

int RegisterImageEditor(JNIEnv* env) {
    JNINativeMethod methods[] = {
        // Adjustments
        { OBFUSCATE("nativeAdjustBrightness"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustBrightness) },
        { OBFUSCATE("nativeAdjustContrast"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustContrast) },
        { OBFUSCATE("nativeAdjustSaturation"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustSaturation) },
        { OBFUSCATE("nativeAdjustExposure"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustExposure) },
        { OBFUSCATE("nativeAdjustWarmth"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustWarmth) },
        { OBFUSCATE("nativeAdjustHighlights"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustHighlights) },
        { OBFUSCATE("nativeAdjustShadows"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustShadows) },
        { OBFUSCATE("nativeAdjustSharpness"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustSharpness) },
        { OBFUSCATE("nativeAdjustVignette"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustVignette) },
        { OBFUSCATE("nativeAdjustHue"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::AdjustHue) },
        { OBFUSCATE("nativeApplyAllAdjustments"),
          OBFUSCATE("(Landroid/graphics/Bitmap;FFFFFFFFFF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::ApplyAllAdjustments) },
        // Crop / Transform
        { OBFUSCATE("nativeCropBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;IIII)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::CropBitmap) },
        { OBFUSCATE("nativeRotateBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RotateBitmap) },
        { OBFUSCATE("nativeFlipBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;Z)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FlipBitmap) },
        { OBFUSCATE("nativeResizeBitmap"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::ResizeBitmap) },
        // Drawing
        { OBFUSCATE("nativeRenderStroke"),
          OBFUSCATE("(Landroid/graphics/Bitmap;[FIFIF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RenderStroke) },
        { OBFUSCATE("nativeRenderStrokesBatch"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F[II[FI[F[F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RenderStrokesBatch) },
        // Background removal
        { OBFUSCATE("nativeRemoveBackground"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RemoveBackground) },
        { OBFUSCATE("nativeRemoveBackgroundByColor"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RemoveBackgroundByColor) },
        // Shapes
        { OBFUSCATE("nativeRenderShape"),
          OBFUSCATE("(Landroid/graphics/Bitmap;IIIIFFFZIFIIIIIF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::RenderShape) },
        // Filters
        { OBFUSCATE("nativeFilterGrayscale"),
          OBFUSCATE("(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterGrayscale) },
        { OBFUSCATE("nativeFilterSepia"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterSepia) },
        { OBFUSCATE("nativeFilterInvert"),
          OBFUSCATE("(Landroid/graphics/Bitmap;)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterInvert) },
        { OBFUSCATE("nativeFilterThreshold"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterThreshold) },
        { OBFUSCATE("nativeFilterBlur"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterBlur) },
        { OBFUSCATE("nativeFilterPixelate"),
          OBFUSCATE("(Landroid/graphics/Bitmap;I)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterPixelate) },
        { OBFUSCATE("nativeFilterEmboss"),
          OBFUSCATE("(Landroid/graphics/Bitmap;F)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::FilterEmboss) },
        // Eyedropper
        { OBFUSCATE("nativeGetPixelColor"),
          OBFUSCATE("(Landroid/graphics/Bitmap;II)I"),
          reinterpret_cast<void*>(neotools::imageeditor::GetPixelColor) },
        // Histogram
        { OBFUSCATE("nativeComputeHistogram"),
          OBFUSCATE("(Landroid/graphics/Bitmap;)[I"),
          reinterpret_cast<void*>(neotools::imageeditor::ComputeHistogram) },
        // Curves
        { OBFUSCATE("nativeApplyCurvesLut"),
          OBFUSCATE("(Landroid/graphics/Bitmap;[I)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::ApplyCurvesLut) },
        // Clone stamp
        { OBFUSCATE("nativeCloneStamp"),
          OBFUSCATE("(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IIIIIF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::CloneStamp) },
        // Blend
        { OBFUSCATE("nativeBlendBitmaps"),
          OBFUSCATE("(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;IF)Landroid/graphics/Bitmap;"),
          reinterpret_cast<void*>(neotools::imageeditor::BlendBitmaps) },
        // Native crash file check
        { OBFUSCATE("nativeCheckCrashFile"),
          OBFUSCATE("()Ljava/lang/String;"),
          reinterpret_cast<void*>(checkNativeCrashFile) },
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

int RegisterApkTools(JNIEnv* env) {
    JNINativeMethod methods[] = {
        { OBFUSCATE("nativeParseApkInfo"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::ParseApkInfo) },
        { OBFUSCATE("nativeParseManifest"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::ParseManifest) },
        { OBFUSCATE("nativeGetManifestXml"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::GetManifestXml) },
        { OBFUSCATE("nativeParseCertificate"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::ParseCertificate) },
        { OBFUSCATE("nativeGetNativeLibs"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::GetNativeLibs) },
        { OBFUSCATE("nativeGetZipEntries"),
          OBFUSCATE("(Ljava/lang/String;)Ljava/lang/String;"),
          reinterpret_cast<void*>(neotools::apk::GetZipEntries) },
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/neomods/tools/native/NeoNative"));
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    // Install native crash signal handlers immediately so we catch crashes
    // even during registration or library init.
    installSignalHandlers();

    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (RegisterEncoding(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "Encoding native registration failed");
    }

    if (RegisterCppConverter(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "CppConverter native registration failed");
    }

    if (RegisterImageEditor(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "ImageEditor native registration failed");
    }

    if (RegisterApkTools(env) != JNI_OK) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
            "ApkTools native registration failed");
    }

    return JNI_VERSION_1_6;
}