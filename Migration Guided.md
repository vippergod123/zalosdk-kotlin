# **ZALO&#39;S KOTLIN SDK**

1. Cách sử dụng

- Import module từ jfrog/jCenter

```
        def versionSDK = sdk_version
        implementation "com.zing.zalo.zalosdk:core:$versionSDK"
        implementation "com.zing.zalo.zalosdk:auth:$versionSDK"
        implementation "com.zing.zalo.zalosdk:analytics:$versionSDK"
        implementation "com.zing.zalo.zalosdk:openapi:$versionSDK"
```

- Lấy application hash key

```
        AppInfo.INSTANCE.getApplicationHashKey(this);
```

- Đăng ký app trên web [https://developers.zalo.me](https://developers.zalo.me/) để lấy appID
- Tạo ứng dụng android với package name và appID 

- Thêm thẻ provider và meta-data vào Android Manifest

```
        <provider
            tools:replace="android:authorities"
            android:name="com.zing.zalo.zalosdk.core.module.ZaloSDKInitProvider"
            android:authorities= package_ name
            android:exported="false" />

        <meta-data
            android:name="com.zing.zalo.zalosdk.appID"
            android:value="@string/appID" />
```

- Thêm thẻ string vào string.xml

```
        <string name="zalosdk_login_protocol_schema"
                translatable="false">zalo-$appID</string>
        <string name="zalosdk_app_id" translatable="false">$appID</string>
```

2. Gọi phương thức ZaloSdk trong Java - Kotlin

```
        Java:   ZaloSDK zaloSDK = new ZaloSDK(Context context, String authCode);
        Kotlin: val zaaloSDK = ZaloSDK(context:Context, authCode:String) 
```

3. So sánh ZaloSDK Kotlin (bản mới) - Java (bản cũ)

- Cài đặt

| JAVA | KOTLIN |
| --- | --- |
| Tự tạo hàm getApplicationHashKey để lấy hash key | Dùng trực tiếp hàm AppInfo.INSTANCE.getApplicationHashKey(this); trong core lib để lấy hash key  |
| Thêm dependenciestừ maven, jCenter | Giữ nguyên |
| Lấy appId từ trang developers và thêm vào file strings.xml, và thêm thẻ metadata cho appID trong AndroidManifest.xml: | Giữ nguyên. Thêm vào build.gradle (project)  `maven {url    "https://zalo.bintray.com/ZaloSDK"}`|
| Thêm attribute name của Application | Xóa attribute name của Appplication trong manifest  |
| Thêm activity để login Zalo bằng Web | Bỏ bước này (hoặc xóa đi nếu upgrade từ java sdk lên kotlin sdk) |

4. Chuyển đổi ZaloSDK từ **Java** qua **Kotlin**

- Xóa attribute name của Application

```
        <application android:name="com.zing.zalo.zalosdk.oauth.ZaloSDKApplication" />
```

- Bỏ wrap(this); ( nếu có)

- Xóa thẻ activity trong AndroidManifest.xml (nếu có)

```
        <activity
            android:name="com.zing.zalo.zalosdk.oauth.BrowserLoginActivity">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="zalo-[appid]" />
            <!-- Lưu ý: thay [appid] bằng id của ứng dụng lấy trên trang developers --!>
            </intent-filter>
        </activity>
```

- Một số method của ZaloOpenApi bản cũ được chuyển từ singleton pattern sang class trong bản mới

```
        VD: Kotlin: val zaloOpenApi = ZaloOpenApi(context:Context, authCode:String)
            Java:   ZaloOpenApi  zaloOpenApi = new ZaloOpenApi(Context context, String authCode)
```

| OLD | NEW |
| --- | --- |
| ZaloSDK.Instance.getProfile(Context ctx, ZaloOpenAPICallback callback, String[] fields) | zaloOpenApi.getProfile(String[] fields, ZaloOpenAPICallback callback) |
| ZaloSDK.Instance.inviteFriendUseApp(Context ctx, String[] friendIds, String message, ZaloOpenAPICallback callback)  | zaloOpenApi.inviteFriendUseApp(String[] friendIds, String message, ZaloOpenAPICallback callback)  |
| ZaloSDK.Instance.getFriendListUsedApp(Context ctx, int position, int count, ZaloOpenAPICallback callback, String[] fields)  | zaloOpenApi.getFriendListUsedApp(String[] fields, int position, int count, ZaloOpenAPICallback callback)  |
| ZaloSDK.Instance.getFriendListInvitable | zaloOpenApi.getFriendListInvitable |
| ZaloSDK.Instance.inviteFriendUseApp(Context ctx, String[] friendIds, String message, ZaloOpenAPICallback callback)  | zaloOpenApi.inviteFriendUseApp(String[] friendIds, String message, ZaloOpenAPICallback callback)  |
| OpenAPIService.getInstance().postToWall(Context context, String link, String msg, ZaloOpenAPICallback callback) | zaloOpenApi.postToWall( String link, String msg, ZaloOpenAPICallback callback) |
| OpenAPIService.getInstance().sendMsgToFriend(Context context, String friendId, String msg, String link, ZaloOpenAPICallback callback);  | zaloOpenApi.sendMsgToFriend(String friendId, String msg, String link, ZaloOpenAPICallback callback);  |
| OpenAPIService.getInstance().shareFeed(final Context context, final FeedData feedOb, final ZaloPluginCallback callback)  | zaloOpenApi.shareFeed(final FeedData feedOb, final ZaloPluginCallback callback)  |
| OpenAPIService.getInstance().shareMessage(final Context context, final FeedData feedOb, final ZaloPluginCallback callback)  | zaloOpenApi.shareMessage(final FeedData feedOb, final ZaloPluginCallback callback)  |

- Một số class name được đổi tên hoặc sửa đổi

| OLD | NEW |
| --- | --- |
| Singleton OpenApiService | Class ZaloOpenApi(context:Context, authCode:String) |
| Singleton ZaloSDK | Class ZaloSDK(context:Context, authCode:String) |

- Một số thay đổi khác về interface và callback

| OLD | NEW |
| --- | --- |
| class OAuthCompleteListener | interface IAuthenticateCompleteListener |
| interface ZaloOpenAPICallback | interface ZaloOpenApiCallback |