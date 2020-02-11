-verbose


-keep class com.zing.zalo.zalosdk.kotlin.openapi.model.** { *; }
-keep interface com.zing.zalo.zalosdk.kotlin.openapi.ZaloOpenApiCallback {*;}
-keep interface com.zing.zalo.zalosdk.kotlin.openapi.ZaloPluginCallback {*;}
-keep interface com.zing.zalo.zalosdk.kotlin.openapi.IZaloOpenApi {*;}

-keepclassmembers class com.zing.zalo.zalosdk.kotlin.openapi.exception.OpenApiException { *; }

-keepclasseswithmembers public interface androidx.annotation.Nullable { *;}
# Output a source map file
-printmapping proguard.map
