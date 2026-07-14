# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp / OkIO (pulled transitively by Lottie)
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# JetBrains annotations (optional dep of Compose)
-dontwarn org.jetbrains.annotations.**

# Compose: the compiler handles @Composable functions; let R8 strip unused internals.
# Only keep what reflection or serialization requires.
-keep class com.neomods.tools.model.** { *; }
-keep class com.neomods.tools.BuildConfig { *; }

# Crash reporter (Java-only, no native library)
-keep public class com.neomods.tools.crash.CrashActivity { *; }
-keep public class com.neomods.tools.crash.CrashHandler { *; }

# PhotoEditor library (View-based, internal classes accessed via layout XML / reflection)
-keep class ja.burhanrashid52.photoeditor.** { *; }
-dontwarn ja.burhanrashid52.photoeditor.**

# Background Remover library (ML Kit)
-keep class com.slowmac.autobackgroundremover.** { *; }
-dontwarn com.slowmac.autobackgroundremover.**

# AXMLPrinter library
-keep class mt.modder.hub.axml.** { *; }
-keep class mt.modder.hub.axmlTools.** { *; }
-dontwarn mt.modder.hub.**

# Guava transitive dependencies (not included, safe to ignore)
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
