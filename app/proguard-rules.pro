# Keep MapLibre native bindings
-keep class org.maplibre.android.** { *; }
-keep class com.mapbox.** { *; }
-dontwarn org.maplibre.android.**

# ZXing (pure-Java QR) — kept defensively; we only use it directly.
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# TensorFlow Lite (sky segmentation) — keep the runtime + GPU delegate; parts are
# reached via JNI/reflection and must survive R8.
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.**
