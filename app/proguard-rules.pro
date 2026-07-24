# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 5
-allowaccessmodification
-repackageclasses
-ignorewarnings

-keep class go.** { *; }
-keep class libv2ray.** { *; }
-keep class com.trilead.ssh2.** { *; }
-keep class com.cbtunnel.plus.service.** { *; }
-keep class com.cbtunnel.plus.thread.** { *; }
-keep class com.cbtunnel.plus.core.** { *; }
-keep class com.** { *; }
-keep class net.** { *; }
-keep class org.** { *; }
-keep class android.** { *; }
-dontwarn java.nio.file.Files
-dontwarn java.nio.file.Path
-dontwarn java.nio.file.OpenOption
-dontwarn okio.**
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *; }
-keep interface com.squareup.okhttp3.** { *; }

-keepattributes Signature
-keepattributes *Annotation*

-dontwarn org.apache.commons.**
-keep class org.apache.commons.** { *;}

# LVL
-keep class com.google.**
-keep class autovalue.shaded.com.google.**

-dontwarn org.apache.**
-dontwarn com.google.**
-dontwarn autovalue.shaded.com.google.**
-dontwarn com.android.vending.billing.**

-keep class com.google.**
-keep class autovalue.shaded.com.google.**
-keep class com.android.vending.billing.**
-keep public class com.android.vending.licensing.**

# Disable debug info output
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String,int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
}
