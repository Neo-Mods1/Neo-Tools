// ---------------------------------------------------------------------------
// Native crash handler — separate library loaded BEFORE neotools.
//
// Installs POSIX signal handlers (sigaction) for fatal signals. On crash
// writes a crash log to a file. On next app launch the Kotlin side reads
// that file via nativeCheckCrashFile() and shows CrashActivity.
// ---------------------------------------------------------------------------

#include <jni.h>
#include <android/log.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <ucontext.h>
#include <sys/stat.h>

#ifndef LOG_TAG
#define LOG_TAG "CrashHandler"
#endif

namespace neotools::crash {

static struct sigaction old_sa[NSIG];

static const char* signalName(int sig) {
    switch (sig) {
        case SIGSEGV: return "SIGSEGV";
        case SIGABRT: return "SIGABRT";
        case SIGBUS:  return "SIGBUS";
        case SIGFPE:  return "SIGFPE";
        case SIGILL:  return "SIGILL";
        case SIGTRAP: return "SIGTRAP";
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
    return "/storage/emulated/0/native_crash.txt";
}

static void nativeCrashHandler(int sig, siginfo_t* info, void* ucontext) {
    // Restore default handler so a second crash isn't trapped
    sigaction(sig, &old_sa[sig], nullptr);

    // Write crash info to a temp file the Kotlin side will read
    std::string crashPath = getCrashFilePath();
    int fd = open(crashPath.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);

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

    // Re-raise so Android's tombstone handler can write the core dump
    tgkill(getpid(), gettid(), sig);
}

void installSignalHandlers() {
    static const int signals[] = { SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGTRAP };
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = nativeCrashHandler;
    sa.sa_flags = SA_SIGINFO;
    sigemptyset(&sa.sa_mask);

    for (int sig : signals) {
        sigaction(sig, &sa, &old_sa[sig]);
    }
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG,
        "Signal handlers installed for SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL, SIGTRAP");
}

jstring checkNativeCrashFile(JNIEnv* env, jobject /* this */) {
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

} // namespace neotools::crash

// ---------------------------------------------------------------------------
// JNI_OnLoad for libCrashHandler.so — registers nativeCheckCrashFile
// ---------------------------------------------------------------------------

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /* reserved */) {
    // Install signal handlers immediately — this is the whole point of
    // loading this library before neotools.
    neotools::crash::installSignalHandlers();

    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Register nativeCheckCrashFile on NeoNative
    jclass clazz = env->FindClass("com/neomods/tools/native/NeoNative");
    if (clazz == nullptr) return JNI_ERR;

    JNINativeMethod methods[] = {
        { "nativeCheckCrashFile",
          "()Ljava/lang/String;",
          reinterpret_cast<void*>(neotools::crash::checkNativeCrashFile) },
    };

    if (env->RegisterNatives(clazz, methods, 1) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
