# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# kotlinx coroutines
-dontwarn kotlinx.coroutines.**

# 保持实体类字段（Room 序列化）
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

# Compose / 反射相关内容
-keepclassmembers class * {
    @androidx.compose.ui.tooling.* <fields>;
}
