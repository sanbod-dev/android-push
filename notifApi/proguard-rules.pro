# نگه داشتن کلاس‌هایی که توسط reflection صدا زده می‌شن (مثل Gson, Retrofit, Room و ...)
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.sanbod.push.** { *; }
# Google Tink and errorprone annotations
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**

# Optional: To avoid warnings about missing annotations
-keep class com.google.errorprone.annotations.** { *; }
# نگه داشتن کلاس‌های Activity/Service/Receiver
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver

# اگر از Gson استفاده می‌کنی
-keep class com.google.gson.** { *; }
-keep class your.model.package.** { *; }