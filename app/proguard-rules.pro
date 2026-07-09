# Keep generic signatures & annotations (needed by Retrofit + kotlinx.serialization reflection).
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, RuntimeVisibleAnnotations, AnnotationDefault

# ---- kotlinx.serialization (official rules, scoped to our package) ----
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Our @Serializable DTOs: keep their generated serializers + Companion.
-keep,includedescriptorclasses class com.immichtv.**$$serializer { *; }
-keepclassmembers class com.immichtv.** {
    *** Companion;
}
-keepclasseswithmembers class com.immichtv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Retrofit (R8 fullMode-safe) ----
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
# Keep the API interface (Retrofit creates a runtime proxy from it).
-keep interface com.immichtv.data.remote.ImmichApi { *; }

# OkHttp / Okio / Coil / Media3 / Hilt ship their own consumer rules; nothing extra needed.
