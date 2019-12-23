-verbose

-keep class com.zing.zalo.zalosdk.openapi.ZaloOpenApi$Companion { com.zing.zalo.zalosdk.openapi.ZaloOpenApi getInstance(); }

-keepclassmembers class com.zing.zalo.zalosdk.openapi.ZaloOpenApi {
    public *;
}
-keep class com.zing.zalo.zalosdk.openapi.ZaloOpenApi { com.zing.zalo.zalosdk.openapi.GetAccessTokenAsyncTask getAccessTokenAsyncTask; }
-keep class com.zing.zalo.zalosdk.openapi.model.** { *; }
-keep interface com.zing.zalo.zalosdk.openapi.ZaloOpenApiCallback {*;}
-keep interface com.zing.zalo.zalosdk.openapi.ZaloPluginCallback {*;}
-keep interface com.zing.zalo.zalosdk.openapi.IZaloOpenApi {*;}

-keepclassmembers class com.zing.zalo.zalosdk.openapi.exception.OpenApiException { *; }

-keepclasseswithmembers public interface androidx.annotation.Nullable { *;}
# Output a source map file
-printmapping proguard.map
