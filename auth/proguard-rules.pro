-dontoptimize

#-dontwarn androidx.**
#-keep class androidx.** { *; }
#-keep interface androidx.** { *;}
#-keep class com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK { com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK$Companion Companion; }
#-keep class com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK$Companion { *; }
#
#-keep class com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthStorage { *; }
#-keepnames class com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK {
#    public *;
# }

#-keepclassmembers class com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK { *;}
-keep enum  com.zing.zalo.zalosdk.kotlin.oauth.LoginVia { *;}
-keep class com.zing.zalo.zalosdk.kotlin.oauth.callback.** { *;}

#print mapping
-printmapping proguard.map