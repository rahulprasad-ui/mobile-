# Keep BroadcastReceiver classes to prevent minification issues
-keep class com.rivavafi.data.receiver.SmsReceiver { *; }
-keep public class * extends android.content.BroadcastReceiver

# Keep Hilt / Dagger generated classes
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Compose specific classes
-keep class androidx.compose.** { *; }
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
