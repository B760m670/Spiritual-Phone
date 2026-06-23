# Keep MapLibre native bindings
-keep class org.maplibre.android.** { *; }
-keep class com.mapbox.** { *; }
-dontwarn org.maplibre.android.**

# ZXing (pure-Java QR) — kept defensively; we only use it directly.
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
