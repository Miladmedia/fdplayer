-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep class ir.miladmedia.fdplayer.data.local.entity.** { *; }
-keep class ir.miladmedia.fdplayer.domain.model.** { *; }
-dontwarn org.jsoup.**
-dontwarn okhttp3.**
