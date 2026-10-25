# Keep Kotlinx Serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.sncreator.app.**$$serializer { *; }
-keepclassmembers class com.sncreator.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.sncreator.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
