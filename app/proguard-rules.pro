-keep class com.proxyfarm.node.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.lifecycle.** { *; }
-keep class java.net.ServerSocket { *; }
-keep class java.net.Socket { *; }
-keep class java.io.** { *; }
-keep class org.json.** { *; }
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}