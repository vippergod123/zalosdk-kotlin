-dontoptimize

#-dontwarn androidx.**
#-keep class androidx.** { *; }
#-keep interface androidx.** { *;}
#-keep class com.zing.zalo.zalosdk.oauth.ZaloSDK { com.zing.zalo.zalosdk.oauth.ZaloSDK$Companion Companion; }
#-keep class com.zing.zalo.zalosdk.oauth.ZaloSDK$Companion { *; }
#
#-keep class com.zing.zalo.zalosdk.oauth.helper.AuthStorage { *; }
#-keepnames class com.zing.zalo.zalosdk.oauth.ZaloSDK {
#    public *;
# }

-keepclassmembers class com.zing.zalo.zalosdk.oauth.ZaloSDK { *;}
-keep enum  com.zing.zalo.zalosdk.oauth.LoginVia { *;}
-keep class com.zing.zalo.zalosdk.oauth.callback.** { *;}

#print mapping
-printmapping proguard.map