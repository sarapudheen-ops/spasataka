# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Bluetooth classes
-keep class android.bluetooth.** { *; }

# Keep OBD-II related classes
-keep class com.spacetec.obd.** { *; }
-keep class com.spacetec.vehicle.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep JNI bridge classes to prevent obfuscation
-keep class com.Autel.maxi.diagnose.Entry { *; }
-keep class com.spacetec.diagnostic.vci.NativeLoader { *; }
