# Keep model classes used by Moshi reflection
-keep class com.phantasmaa.panoplia.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
