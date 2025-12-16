# ProGuard configuration for StreamPad

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Remove all logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Keep our model classes for serialization
-keep class com.streampad.bt.model.** { *; }
-keep class com.streampad.bt.utils.SerializableShortcut { *; }
-keep class com.streampad.bt.utils.SerializableProfile { *; }

# Keep security-crypto classes
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Suppress warnings for optional Tink dependencies
-dontwarn com.google.api.client.http.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.joda.time.**
