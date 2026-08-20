# Keep ARCore
-keep class com.google.ar.core.** { *; }
-keepclassmembers class com.google.ar.core.** { *; }

# Keep Filament
-keep class com.google.android.filament.** { *; }
-keep class com.google.android.filament.gltfio.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
