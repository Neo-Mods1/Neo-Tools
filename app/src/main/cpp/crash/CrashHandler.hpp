#pragma once

#include <jni.h>

namespace neotools::crash {

// Install POSIX signal handlers for SIGSEGV, SIGABRT, SIGBUS, SIGFPE, SIGILL.
// Writes crash info to a file on native crash. Called once from JNI_OnLoad.
void installSignalHandlers();

// JNI method: reads + deletes the native crash file written by the signal
// handler. Returns the crash log as a Java String, or null if none exists.
jstring checkNativeCrashFile(JNIEnv* env, jobject /* this */);

} // namespace neotools::crash
