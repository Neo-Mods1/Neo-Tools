#pragma once
#include <jni.h>
#include <string>

namespace neotools::apk {

// Parse APK metadata and return JSON string with all info.
// Reads the ZIP, extracts manifest hash, signature info, native libs, etc.
jstring ParseApkInfo(JNIEnv* env, jobject /* this */, jstring apkPath);

// Parse AndroidManifest.xml from APK and return structured JSON.
jstring ParseManifest(JNIEnv* env, jobject /* this */, jstring apkPath);

// Return raw (pretty-printed) manifest XML text.
jstring GetManifestXml(JNIEnv* env, jobject /* this */, jstring apkPath);

// Extract and return certificate info as JSON from the APK's signing block.
jstring ParseCertificate(JNIEnv* env, jobject /* this */, jstring apkPath);

// Return list of native libraries found in lib/ as JSON array.
jstring GetNativeLibs(JNIEnv* env, jobject /* this */, jstring apkPath);

// Return list of all ZIP entries as JSON array.
jstring GetZipEntries(JNIEnv* env, jobject /* this */, jstring apkPath);

} // namespace neotools::apk
