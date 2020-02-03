-verbose
-keepattributes MethodParameters,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

#Android's module
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *;    }


#ZDK"s module
-keep class com.zing.zalo.zalosdk.core.log.** { *;}

#Device Tracking
-keepclasseswithmembers class com.zing.zalo.devicetrackingsdk.DeviceTracking { *; }
-keepclasseswithmembers class com.zing.zalo.devicetrackingsdk.model.PreloadInfo {*;}

#Helper
-keepclassmembers class com.zing.zalo.zalosdk.core.helper.DeviceInfo { *;}
-keep class com.zing.zalo.zalosdk.core.helper.** {  *;}

-keep class com.zing.zalo.zalosdk.core.module.** {
 *;
}

-keepclasseswithmembers class com.zing.zalo.zalosdk.core.http.** { *;}
-keepclasseswithmembers class com.zing.zalo.zalosdk.core.apptracking.** {  *;}

#Service Map
-keep class com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager {
  public *;
}

-keep class com.zing.zalo.zalosdk.core.settings.SettingsManager {
  public *;
}

# -keep public class * extends android.content.BroadcastReceiver
#Android"s module
#-keep public class * extends android.app.Activity
# -keep public class * extends android.app.Application
# -keep public class * extends android.app.Service
# -keep public class * extends android.content.BroadcastReceiver
#-keep class android.content.ContentProvider { *;}


#print mapping
-printmapping proguard.map