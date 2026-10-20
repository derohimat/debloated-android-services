# ProGuard & R8 configuration rules for DebloatMaster

# Keep application data models and Room entities
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Moshi JSON serialization rules
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**

# Kotlin Coroutines and Dispatchers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
