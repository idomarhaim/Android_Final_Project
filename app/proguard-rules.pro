# GoalPilot ProGuard / R8 rules

# Keep Kotlinx Serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase Firestore uses reflection to (de)serialize model classes.
# Keep the fields of our Firestore DTOs.
-keepclassmembers class com.idomarhaim.goalpilot.data.firestore.dto.** {
    *;
}
-keepclassmembers class com.idomarhaim.goalpilot.data.remote.dto.** {
    *;
}

# Firebase / Google Play Services (generally safe defaults)
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
