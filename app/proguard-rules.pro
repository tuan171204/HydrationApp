# MPAndroidChart - giữ lại để chart hoạt động đúng
-keep class com.github.mikephil.charting.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Giữ model classes (không bị obfuscate)
-keep class com.hydration.app.models.** { *; }
-keep class com.hydration.app.database.** { *; }
