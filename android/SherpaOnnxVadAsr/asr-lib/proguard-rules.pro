# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep sherpa-onnx JNI classes
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Keep Vosk classes
-keep class com.alphacep.vosk.** { *; }

# Keep library public API
-keep public class com.k2fsa.sherpa.onnx.asr.AsrLibrary { *; }
-keep public class com.k2fsa.sherpa.onnx.asr.AsrEngine { *; }
-keep public class com.k2fsa.sherpa.onnx.asr.StreamingAsrEngine { *; }
-keep public class com.k2fsa.sherpa.onnx.asr.AudioRecorder { *; }
-keep public class com.k2fsa.sherpa.onnx.asr.VadProcessor { *; }
