# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-dontobfuscate
#-renamesourcefileattribute SourceFile
#-keepattributes SourceFile,LineNumberTable

# Temp fix for androidx.window:window:1.0.0-alpha09 imported by termux-shared
# https://issuetracker.google.com/issues/189001730
# https://android-review.googlesource.com/c/platform/frameworks/support/+/1757630
-keep class androidx.window.** { *; }

# GitHub OAuth - ScribeJava Library
-keep class com.github.scribejava.** { *; }
-keepclassmembers class com.github.scribejava.** { *; }
-dontwarn com.github.scribejava.**

# GitHub OAuth Manager and User Info
-keep class com.termux.app.oauth.GitHubOAuthManager { *; }
-keep class com.termux.app.oauth.GitHubOAuthManager$GitHubUserInfo { *; }
-keepclassmembers class com.termux.app.oauth.GitHubOAuthManager$GitHubUserInfo {
    public <methods>;
    public <fields>;
}

# Keep Properties loading for supabase.properties
-keep class java.util.Properties { *; }
-keepclassmembers class java.util.Properties { *; }

# JSON parsing
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

# Keep CompletableFuture for async operations
-keep class java.util.concurrent.CompletableFuture { *; }
-keepclassmembers class java.util.concurrent.CompletableFuture { *; }

# Keep all serialization attributes
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# OkHttp (used by ScribeJava)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }

# Retrofit/Gson (if used by ScribeJava)
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation @interface com.google.gson.annotations.SerializedName
