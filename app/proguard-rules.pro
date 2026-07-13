# Add project specific ProGuard rules here.
# For more details, see https://developer.android.com/build/shrink-code

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose
-dontwarn org.jetbrains.annotations.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Navigation Compose
-keep class androidx.navigation.** { *; }

# Material3
-keep class androidx.compose.material3.** { *; }

# Parcelable / Serializable models passed through navigation
-keep class com.neomods.tools.model.** { *; }

# Crash reporting: keep BuildConfig reference used by the handler
-keep class com.neomods.tools.BuildConfig { *; }

# Crash reporter uses java.net.HttpURLConnection (no reflection), nothing to keep.

# Retain the crash activity entry point.
-keep public class com.neomods.tools.crash.CrashActivity { *; }
-keep public class com.neomods.tools.crash.CrashHandler { *; }
-keep public class com.neomods.tools.crash.CrashReporter { *; }
