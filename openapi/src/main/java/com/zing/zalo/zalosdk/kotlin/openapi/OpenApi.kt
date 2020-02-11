package com.zing.zalo.zalosdk.kotlin.openapi

import android.content.*
import android.text.TextUtils
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.kotlin.core.http.IHttpRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.openapi.exception.OpenApiException
import com.zing.zalo.zalosdk.kotlin.openapi.model.FeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.Nullable
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class OpenApi(
    var context: Context,
    var oauthCode: String?,
    var isBroadcastRegistered: Boolean,
    private var httpClient: HttpClient,
    private var accessTokenHttpClient: HttpClient,
    var scope: CoroutineScope
) : IZaloOpenApi {


    private val ZALO_PARAM_BACK_TO_SOURCE = "backToSource"
    private val ZALO_PARAM_POST_FEED = "postFeed"
    private var feedCallbackReceiver: BroadcastReceiver? = null
    private var callbackZaloPluginClient: WeakReference<ZaloPluginCallback>? = null


    private var openApiStorage = OpenApiStorage(context.applicationContext)
    private var accessTokenRequest =
        HttpUrlEncodedRequest(Constant.api.AUTH_MOBILE_ACCESS_TOKEN_PATH)

    override var accessToken = ""
    override var accessTokenExpiredTime = 0L

    init {
        val accessTokenJSON = openApiStorage.getAccessTokenNewAPI()
        accessToken = accessTokenJSON?.optString("access_token") ?: ""
        accessTokenExpiredTime =
            accessTokenJSON?.optLong("expires_in") ?: 0L
    }

    /**
     * Get Zalo user's profile
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/thong-tin-nguoi-dung-post-28
     * @param fields   : id, birthday, gender, picture, name ex: {"id", "birthday", "gender", "picture", "name"}
     * @param callback
     */
    override fun getProfile(fields: Array<String>, @Nullable callback: ZaloOpenApiCallback) {

        val request = HttpGetRequest(Constant.api.GRAPH_V2_ME_PATH)
        request.addQueryStringParameter("fields", buildFieldsParam(fields))
        callApiRequest(request, callback)
    }

    /**
     * Lấy danh sách tất cả bạn bè của người dùng đã sử dụng ứng dụng
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/danh-sach-ban-be-post-34
     * @param fields   : Hỗ trợ các field: id, name, picture, gender
     * @param position position
     * @param count    count
     * @param callback
     */
    override fun getFriendListUsedApp(
        fields: Array<String>,
        position: Int,
        count: Int,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        val request = HttpGetRequest(Constant.api.GRAPH_ME_FRIENDS_PATH)
        request.addQueryStringParameter("fields", buildFieldsParam(fields))
        request.addQueryStringParameter("offset", position.toString())
        request.addQueryStringParameter("limit", count.toString())
        callApiRequest(request, callback)
    }


    /**
     * Lấy danh sách bạn bè chưa sử dụng ứng dụng và có thể nhắn tin mời sử dụng ứng dụng
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/danh-sach-ban-be-post-34
     *
     * @param position position
     * @param count    count
     * @param callback
     * @param fields   : Hỗ trợ các field: id, name, picture, gender
     */
    override fun getFriendListInvitable(
        fields: Array<String>,
        position: Int,
        count: Int,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        val request = HttpGetRequest(Constant.api.GRAPH_ME_INVITABLE_FRIENDS_PATH)
        request.addQueryStringParameter("fields", buildFieldsParam(fields))
        request.addQueryStringParameter("offset", position.toString() + "")
        request.addQueryStringParameter("limit", count.toString() + "")
        callApiRequest(request, callback)
    }


    /**
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/moi-su-dung-ung-dung-post-41
     * @param friendId ex: {"friend-id1", "friend-id2", "friend-id3"}
     * @param message  String
     * @param callback ZaloOpenApiCallback
     */
    override fun inviteFriendUseApp(
        friendId: Array<String>,
        message: String,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        val request = HttpUrlEncodedRequest(Constant.api.GRAPH_APP_REQUESTS_PATH)
        request.addParameter("to", buildFieldsParam(friendId))
        request.addParameter("message", message)
        callApiRequest(request, callback)
    }


    /**
     * Post a feed to wall
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/dang-bai-viet-post-39
     * @param link     String url link
     * @param msg      String msg
     * @param callback ZaloOpenApiCallback
     */
    override fun postToWall(link: String, msg: String, @Nullable callback: ZaloOpenApiCallback) {
        val request = HttpUrlEncodedRequest(Constant.api.GRAPH_ME_FEED_PATH)
        request.addParameter("link", link)
        request.addParameter("message", msg)
        callApiRequest(request, callback)
    }

    /**
     * Send message to friend
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/goi-tin-nhan-toi-ban-be-post-1183
     * @param friendId Friend ID
     * @param msg      String content message
     * @param link     Link
     * @param callback ZaloOpenApiCallback
     */

    override fun sendMsgToFriend(
        friendId: String,
        msg: String,
        link: String,
        @Nullable callback: ZaloOpenApiCallback
    ) {
        val request = HttpUrlEncodedRequest(Constant.api.GRAPH_ME_MESSAGE_PATH)
        request.addParameter("to", friendId)
        request.addParameter("message", msg)
        request.addParameter("link", link)
        callApiRequest(request, callback)
    }

    /**
     * gửi tin nhắn đến bạn bè thông qua app Zalo.
     * https://developers.zalo.me/docs/sdk/android-sdk/tuong-tac-voi-app-zalo/gui-tin-nhan-toi-ban-be-post-452
     * @param feedData: object feed cần share
     * @param callback: SDK sẽ gọi về callback này khi thông tin đã được gửicho app Zalo.
     */
    override fun shareMessage(
        feedData: FeedData,
        callback: ZaloPluginCallback?
    ) {
        shareZalo(context, feedData, "message", callback)
    }

    /**
     * đăng bài viết lên trang nhật ký của user thông qua app Zalo.
     * https://developers.zalo.me/docs/sdk/android-sdk/tuong-tac-voi-app-zalo/dang-bai-viet-post-447
     * @param feedData: object feed cần share
     * @param callback: callback: SDK sẽ gọi về callback này khi thông tin đã được gửicho app Zalo.
     */
    override fun shareFeed(
        feedData: FeedData,
        callback: ZaloPluginCallback?
    ) {
        shareZalo(context, feedData, "feed", callback)
    }


    //#region private supportive method
    internal fun shareZalo(
        context: Context,
        feedData: FeedData,
        shareTo: String,
        callback: ZaloPluginCallback?
    ) {
        val intent = getShareIntentZaloApp(feedData, shareTo)
        val ableCalled = intent.resolveActivityInfo(context.packageManager, 0) != null
        if (ableCalled) {
            registerBroadCast(context)

            callbackZaloPluginClient = if (callback == null) null
            else WeakReference(callback)

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Log.w("Zalo app is not installed!")

        }
    }

    private fun callApiRequest(request: IHttpRequest, @Nullable callback: ZaloOpenApiCallback) {
        scope.launch {
            if (!isAccessTokenValid()) {
                getAccessToken { errorCode ->
                    if (errorCode == ZaloOAuthResultCode.RESULTCODE_NO_ERROR)
                        callApiWithRequest(request, accessToken, callback)
                    else {
                        val result = JSONObject()
                        result.put("error", errorCode)
                        result.put("message", ZaloOAuthResultCode.findErrorMessageByID(errorCode))
                        GlobalScope.launch(Dispatchers.Main) { callback.onResult(result) }
//                        callback.onResult(result)
                    }
                }
            } else
                callApiWithRequest(request, accessToken, callback)
        }
    }

    private fun buildFieldsParam(fields: Array<String>): String {
        if (fields.isNotEmpty()) {
            val param = StringBuffer()
            for (each in fields) {
                param.append(each).append(",")
            }
            return param.substring(0, param.length - 1)
        }
        return ""
    }

    private fun isAccessTokenValid(): Boolean {
        if (!TextUtils.isEmpty(accessToken) && accessTokenExpiredTime > System.currentTimeMillis()) return true

        Log.w("isAccessTokenValid", "Token is not valid")
        return false
    }

    private fun getShareIntentZaloApp(
        feedData: FeedData,
        shareTo: String
    ): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.component = ComponentName(
            Constant.ZALO_PACKAGE_NAME,
            "com.zing.zalo.ui.TempShareViaActivity"
        )
        intent.putExtra(Intent.EXTRA_SUBJECT, feedData.msg)
        intent.putExtra(Intent.EXTRA_TEXT, feedData.link)
        val tokenShareZalo = System.currentTimeMillis().toString()
        intent.putExtra("token", tokenShareZalo)
        if (!TextUtils.isEmpty(shareTo)) {
            if (shareTo == "feed") {
                intent.putExtra(ZALO_PARAM_POST_FEED, true)
            } else if (shareTo == "message") {
                intent.putExtra("hidePostFeed", true)
            }
        }
        intent.putExtra("autoBack2S", true)
        intent.putExtra(ZALO_PARAM_BACK_TO_SOURCE, true)
        return intent
    }

    private fun registerBroadCast(context: Context) {
        if (Utils.isZaloSupportCallBack(context)) {
            if (!isBroadcastRegistered) {
                val intentFilter = IntentFilter()
                intentFilter.addAction("com.zing.zalo.shareFeedResultInfo")
                feedCallbackReceiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        Log.d(
                            "ZaloOpenApi - broadCastCallBack",
                            "broadCastCallBack ----- broadcast receiver====="
                        )
                        try {
                            val dataString =
                                intent?.extras?.getString("result") ?: return
                            val data = JSONObject(dataString)
                            if (data.has("token")) {
                                val token = data.optString("token")
                                var isSuccess = false
                                if (!TextUtils.isEmpty(token)) {
                                    val errorCode = data.getInt("error_code")
                                    unRegisterReceiver(context)//unregister after received callback

                                    if (errorCode == 0) {
                                        isSuccess = true
                                    }
                                    val callback =
                                        callbackZaloPluginClient!!.get()
                                            ?: throw OpenApiException("Can't get callback zalo plugin client!")
                                    callback.onResult(
                                        isSuccess,
                                        data.getInt("error_code"),
                                        null,
                                        null
                                    )
                                }
                            }
                        } catch (ex: OpenApiException) {
                            Log.w("ZaloOpenApi - registerBroadCast", ex)
                        } catch (ex: Exception) {
                            Log.e("ZaloOpenApi - registerBroadCast", ex)
                            return
                        }
                    }
                }
                context.registerReceiver(feedCallbackReceiver, intentFilter)
                isBroadcastRegistered = true
                Log.d("ZaloOpenApi - registerBroadCast", "register ----- broadcast receiver=====")
            }
        } else {
            val callback = callbackZaloPluginClient?.get() ?: return
            callback.onResult(true, 0, null, null)
        }
    }

    private fun unRegisterReceiver(context: Context) {
        if (feedCallbackReceiver != null) {
            context.unregisterReceiver(feedCallbackReceiver)
            isBroadcastRegistered = false
            Log.d("ZaloOpenApi - unRegisterReceiver", "unregister ----- broadcast receiver=====")
        }
    }

    private fun getAccessToken(callback: (Int) -> Unit) {
        try {
            val errorCode = makeGetAccessTokenRequest()
            callback(errorCode)
            return
        } catch (ex: OpenApiException) {
            Log.w("getAccessToken", ex)
            callback(ZaloOAuthResultCode.RESULTCODE_ZALO_OAUTH_INVALID)
            return
        } catch (ex: JSONException) {
            Log.e("getAccessToken", ex)
        } catch (ex: Exception) {
            Log.e("getAccessToken", ex)
        }
        callback(ZaloOAuthResultCode.RESULTCODE_ZALO_UNKNOWN_ERROR)
    }

    private fun callApiWithRequest(
        request: IHttpRequest?,
        accessToken: String,
        callback: ZaloOpenApiCallback?
    ) {

        if (request == null) return
        request.addQueryStringParameter("access_token", accessToken)
        val data = httpClient.send(request).getJSON()

        GlobalScope.launch(Dispatchers.Main) { callback?.onResult(data) }

//        callback(data)
    }

    private fun makeGetAccessTokenRequest(): Int {

        val authCode = oauthCode?: ""
        if (TextUtils.isEmpty(authCode)) throw OpenApiException("Auth code is invalid - Login again!")
        accessTokenRequest.addQueryStringParameter("code", authCode)
        accessTokenRequest.addQueryStringParameter("pkg_name", AppInfo.getPackageName(context))
        accessTokenRequest.addQueryStringParameter(
            "sign_key",
            AppInfo.getApplicationHashKey(context) ?: ""
        )
        accessTokenRequest.addQueryStringParameter("app_id", AppInfo.getAppId(context))
        accessTokenRequest.addQueryStringParameter("version", Constant.VERSION)
        accessTokenRequest.addQueryStringParameter(
            "zdevice",
            DeviceInfo.prepareDeviceIdData(context).toString()
        )
        accessTokenRequest.addQueryStringParameter(
            "ztracking",
            DeviceInfo.prepareTrackingData(
                context,
                DeviceTracking.getInstance().getDeviceId() ?: "",
                System.currentTimeMillis()
            ).toString()

        )
        accessTokenRequest.addHeader("gid", DeviceTracking.getInstance().getDeviceId() ?: "")

        val jsonObject = accessTokenHttpClient.send(accessTokenRequest).getJSON()
            ?: return ZaloOAuthResultCode.RESULTCODE_ZALO_UNKNOWN_ERROR
        val errorCode =
            jsonObject.optInt("error", ZaloOAuthResultCode.RESULTCODE_ZALO_UNKNOWN_ERROR)
        if (errorCode == 0 && jsonObject.has("data")) {

            val data = jsonObject.getJSONObject("data")

            accessToken = data.optString("access_token") ?: ""
            accessTokenExpiredTime =
                System.currentTimeMillis() + Utils.convertTimeToMilliSeconds(1, TimeUnit.HOURS)

            data.put("expires_in", accessTokenExpiredTime)
            openApiStorage.setAccessTokenNewAPI(data.toString())
        }
        return errorCode
    }
}