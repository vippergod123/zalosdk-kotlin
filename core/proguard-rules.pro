-verbose
-keepattributes MethodParameters,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

#Android's module
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *;    }


#ZDK"s module
-keep class com.zing.zalo.zalosdk.kotlin.core.log.** { *;}

#Device Tracking
-keepclasseswithmembers class com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking { *; }
-keepclasseswithmembers class com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.model.PreloadInfo {*;}

#Helper
-keepclassmembers class com.zing.zalo.zalosdk.kotlin.core.helper.DeviceInfo { *;}
-keep class com.zing.zalo.zalosdk.kotlin.core.helper.** {  *;}

-keep class com.zing.zalo.zalosdk.kotlin.core.module.** {
 *;
}

-keepclasseswithmembers class com.zing.zalo.zalosdk.kotlin.core.http.** { *;}
-keepclasseswithmembers class com.zing.zalo.zalosdk.kotlin.core.apptracking.** {  *;}

#Service Map
-keep class com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager {
  public *;
}

-keep class com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager {
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