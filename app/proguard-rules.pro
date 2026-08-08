# PetIngle ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Keep Coil
-keep class coil.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }

# Keep our app classes
-keep class br.com.petingle.** { *; }
