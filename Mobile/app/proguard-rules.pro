# -keepattributes Exceptions,InnerClasses
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class com.unifurniture.mobile.data.model.** { *; }
-keep class com.google.gson.** { *; }
