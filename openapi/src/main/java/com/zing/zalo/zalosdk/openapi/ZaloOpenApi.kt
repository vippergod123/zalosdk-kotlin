package com.zing.zalo.zalosdk.openapi

import android.annotation.SuppressLint
import android.content.*
import android.text.TextUtils
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.core.http.IHttpRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.module.BaseModule
import com.zing.zalo.zalosdk.core.module.ModuleManager
import com.zing.zalo.zalosdk.oauth.helper.AuthStorage
import com.zing.zalo.zalosdk.openapi.exception.OpenApiException
import com.zing.zalo.zalosdk.openapi.model.FeedData
import org.jetbrains.annotations.Nullable
import org.json.JSONObject
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
class ZaloOpenApi : BaseModule(), IZaloOpenApi {

    companion object {
        private val instance = ZaloOpenApi()
        private lateinit var authStorage: AuthStorage

        private const val ZALO_PARAM_BACK_TO_SOURCE = "backToSource"
        private const val ZALO_PARAM_POST_FEED = "postFeed"
        private var feedCallbackReceiver: BroadcastReceiver? = null
        private var callbackZaloPluginClient: WeakReference<ZaloPluginCallback>? = null

        internal var isBroadcastRegistered = false

        fun getInstance(): ZaloOpenApi {
            return instance
        }

        init {
            ModuleManager.addModule(instance)
        }
    }


    private var accessToken = ""
    private var expiredAccessToken = 0L

    internal lateinit var callApiAsyncTask: CallApiAsyncTask
    internal lateinit var getAccessTokenAsyncTask: GetAccessTokenAsyncTask

    override fun onStart(context: Context) {
        super.onStart(context)
        authStorage = AuthStorage(context.applicationContext)
        updateAccessToken()
    }

    /**
     * Get Zalo user's profile
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/thong-tin-nguoi-dung-post-28
     * @param fields   : id, birthday, gender, picture, name ex: {"id", "birthday", "gender", "picture", "name"}
     * @param callback
     */
    override fun getProfile(fields: Array<String>, @Nullable callback: ZaloOpenApiCallback) {
        if (!checkInitialize()) return

        val request = HttpGetRequest(Constant.api.GRAPH_V2_ME_PATH)
        request.addQueryStringParameter("fields", buildFieldsParam(fields))
        callApi(request, callback)
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
        if (!checkInitialize()) return
        val request = HttpGetRequest(Constant.api.GRAPH_ME_FRIENDS_PATH)
        request.addQueryStringParameter("fields", buildFieldsParam(fields))
        request.addQueryStringParameter("offset", position.toString())
        request.addQueryStringParameter("limit", count.toString())
        callApi(request, callback)
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
        if (!checkInitialize()) return
        val request = HttpGetRequest(Constant.api.GRAPH_ME_INVITABLE_FRIENDS_PATH)
        request.addQueryStringParameter("fields", buildFieldsParam(fields))
        request.addQueryStringParameter("offset", position.toString() + "")
        request.addQueryStringParameter("limit", count.toString() + "")
        callApi(request, callback)
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
        if (!checkInitialize()) return

        val request = HttpUrlEncodedRequest(Constant.api.GRAPH_APP_REQUESTS_PATH)
        request.addParameter("to", buildFieldsParam(friendId))
        request.addParameter("message", message)
        callApi(request, callback)
    }


    /**
     * Post a feed to wall
     * http://developers.zaloapp.com/docs/api/open-api/tai-lieu/dang-bai-viet-post-39
     * @param link     String url link
     * @param msg      String msg
     * @param callback ZaloOpenApiCallback
     */
    override fun postToWall(link: String, msg: String, @Nullable callback: ZaloOpenApiCallback) {
        if (!checkInitialize()) return

        val request = HttpUrlEncodedRequest(Constant.api.GRAPH_ME_FEED_PATH)
        request.addParameter("link", link)
        request.addParameter("message", msg)
        callApi(request, callback)
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
        if (!checkInitialize()) return

        val request = HttpUrlEncodedRequest(Constant.api.GRAPH_ME_MESSAGE_PATH)
        request.addParameter("to", friendId)
        request.addParameter("message", msg)
        request.addParameter("link", link)
        callApi(request, callback)
    }

    /**
     * gửi tin nhắn đến bạn bè thông qua app Zalo.
     * https://developers.zalo.me/docs/sdk/android-sdk/tuong-tac-voi-app-zalo/gui-tin-nhan-toi-ban-be-post-452
     * @param feedData: object feed cần share
     * @param callback: SDK sẽ gọi về callback này khi thông tin đã được gửicho app Zalo.
     */
    override fun shareMessage(
        feedData: FeedData,
        callback: ZaloPluginCallback
    ) {
        val ctx = getInstance().context ?: return
        shareZalo(ctx, feedData, "message", callback)
    }

    /**
     * đăng bài viết lên trang nhật ký của user thông qua app Zalo.
     * https://developers.zalo.me/docs/sdk/android-sdk/tuong-tac-voi-app-zalo/dang-bai-viet-post-447
     * @param feedData: object feed cần share
     * @param callback: callback: SDK sẽ gọi về callback này khi thông tin đã được gửicho app Zalo.
     */
    override fun shareFeed(
        feedData: FeedData,
        callback: ZaloPluginCallback
    ) {
        val ctx = getInstance().context ?: return
        shareZalo(ctx, feedData, "feed", callback)
    }


    //#region private supportive method
    internal fun shareZalo(
        context: Context,
        feedData: FeedData,
        shareTo: String,
        callback: ZaloPluginCallback?
    ) {
        if (!checkInitialize()) return

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

    private val enableUnitTest by lazy { false }

    private fun callApi(request: IHttpRequest, @Nullable callback: ZaloOpenApiCallback) {


        val tokenCallback = object : ZaloOpenApiCallback {
            override fun onResult(data: JSONObject?) {

                val error = data?.optInt("error", -1)
                if (error != 0) {
                    callback.onResult(data)
                    return
                }
                updateAccessToken()
                callApiAsyncTaskMethod(request)

            }
        }
        if (!enableUnitTest)
            callApiAsyncTask = CallApiAsyncTask(callback)

        if (isAccessTokenValid()) {
            callApiAsyncTaskMethod(request)
            return
        }

        if (!enableUnitTest)
            getAccessTokenAsyncTask = GetAccessTokenAsyncTask(
                WeakReference(getInstance().context as Context),
                tokenCallback
            )
        getAccessTokenAsyncTask.callback = tokenCallback
        getAccessTokenAsyncTask.execute()
    }


    private fun isAccessTokenValid(): Boolean {

        if (!TextUtils.isEmpty(accessToken) && expiredAccessToken > System.currentTimeMillis()) return true

        Log.w("isAccessTokenValid", "Token is not valid")
        return false
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

    private fun checkInitialize(): Boolean {
        if (getInstance().hasContext)
            return true

        Log.e("Zalo Open Api is not init yet!")
        return false
    }

    private fun updateAccessToken() {
        val accessTokenJSON = authStorage.getAccessTokenNewAPI()

        accessToken = accessTokenJSON?.optString("access_token") ?: ""
        expiredAccessToken = accessTokenJSON?.optLong("expires_in") ?: 0L

    }

    private fun callApiAsyncTaskMethod(request: IHttpRequest) {
        request.addQueryStringParameter("access_token", accessToken)
        callApiAsyncTask.execute(request)
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
//#endregion
}

