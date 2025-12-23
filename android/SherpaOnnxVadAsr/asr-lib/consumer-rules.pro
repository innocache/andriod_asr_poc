# Consumer ProGuard rules for asr-lib

# Keep native methods for JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep sherpa-onnx classes
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Keep Vosk classes  
-keep class com.alphacep.vosk.** { *; }
