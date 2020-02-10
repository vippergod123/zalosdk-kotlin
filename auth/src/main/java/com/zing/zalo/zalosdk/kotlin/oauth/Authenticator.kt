package com.zing.zalo.zalosdk.kotlin.oauth

import android.app.Activity
import android.content.*
import android.net.Uri
import android.text.TextUtils
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.helper.ZTaskExecutor
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager
import com.zing.zalo.zalosdk.kotlin.oauth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.kotlin.oauth.callback.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthStorage
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthUtils
import com.zing.zalo.zalosdk.kotlin.oauth.service.ZaloService
import com.zing.zalo.zalosdk.kotlin.oauth.task.ValidateOAuthCodeTask
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.net.URLEncoder

class Authenticator(val context: Context, private val mStorage: AuthStorage) :
    IAuthenticator {
    private var wListener: WeakReference<IAuthenticateCompleteListener> =
        WeakReference<IAuthenticateCompleteListener>(null)
    private var bIsZaloLoginSuccessful = false
    private var bIsZaloOutOfDate: Boolean = false
    var httpClient: HttpClient =
        HttpClient(ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_OAUTH))

    private fun setOAuthCompleteListener(listener: IAuthenticateCompleteListener) {
        wListener = WeakReference(listener)
    }

    var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constant.AUTHORIZATION_LOGIN_SUCCESSFUL_ACTION == intent.action) {
                bIsZaloLoginSuccessful =
                    intent.getBooleanExtra(Constant.EXTRA_AUTHORIZATION_LOGIN_SUCCESSFUL, false)
            }
        }
    }

    override fun authenticate(
        activity: Activity,
        via: LoginVia,
        listener: IAuthenticateCompleteListener?
    ) {
        if (listener == null) throw IllegalArgumentException("AuthCompleteListenerI must be set.")
        setOAuthCompleteListener(listener)
        sendOAuthRequest(activity, via)
    }

    override fun registerZalo(activity: Activity, listener: IAuthenticateCompleteListener?) {
        if (listener == null) throw IllegalArgumentException("AuthCompleteListenerI must be set.")
        setOAuthCompleteListener(listener)
        val intent = WebLoginActivity.newIntent(activity, true)
        activity.startActivityForResult(intent, Constant.ZALO_AUTHENTICATE_REQUEST_CODE)
    }

    override fun isAuthenticate(code: String, callback: ValidateOAuthCodeCallback?): Boolean {
        if (code.isEmpty()) {
            callback?.onValidateComplete(
                false,
                ZaloOAuthResultCode.RESULTCODE_ZALO_OAUTH_INVALID,
                -1,
                null
            )
            return false
        }

        val appID = AppInfo.getAppId(context)
        val appVersion = AppInfo.getSDKVersion()
        val isOnline = Utils.isOnline(context)

        val task = ValidateOAuthCodeTask(httpClient, code, appID, appVersion, isOnline, callback)
        task.execute()
        return true
    }

    private fun sendOAuthRequest(activity: Activity, loginVia: LoginVia) {
        val isZaloInstalled = AppInfo.isPackageExists(context, Constant.core.ZALO_PACKAGE_NAME)
        val settingsManager = SettingsManager.getInstance()

        when (loginVia) {
            LoginVia.APP -> {
                if (isZaloInstalled) {
                    loginViaApp(activity)
                } else {
                    wListener.get()?.onAuthenticateError(
                        ZaloOAuthResultCode.RESULTCODE_ZALO_APPLICATION_NOT_INSTALLED,
                        context.getString(R.string.zalo_app_not_installed)
                    )
                }
            }
            LoginVia.WEB -> loginViaWeb(activity)
            LoginVia.APP_OR_WEB -> {
                if (isZaloInstalled) {
                    if (settingsManager.isUseWebViewLoginZalo()) {
                        // webview_login == true -> app not log in -> login via web view
                        loginViaAppOrWebIfNotLogin(activity)
                    } else {
                        loginViaApp(activity)
                    }
                } else {
                    loginViaWeb(activity)
                }
            }
        }
    }


    fun loginViaApp(activity: Activity) {
        try {
            try {
                activity.unregisterReceiver(receiver)
            } catch (ex: Exception) {
            }
            val intentFilter = IntentFilter(Constant.AUTHORIZATION_LOGIN_SUCCESSFUL_ACTION)
            activity.registerReceiver(receiver, intentFilter)

            val i = Intent("com.zing.zalo.intent.action.THIRD_PARTY_APP_AUTHORIZATION")
            i.putExtra(Intent.EXTRA_UID, AppInfo.getAppIdLong(activity))
            activity.startActivityForResult(i, Constant.ZALO_AUTHENTICATE_REQUEST_CODE)
        } catch (ex: SecurityException) {
            bIsZaloOutOfDate = true

            val errorMsg =
                ZaloOAuthResultCode.findErrorMessageByID(ZaloOAuthResultCode.RESULTCODE_ZALO_OUT_OF_DATE)
            wListener.get()
                ?.onAuthenticateError(ZaloOAuthResultCode.RESULTCODE_ZALO_OUT_OF_DATE, errorMsg)
            //tra ve error code
        } catch (ex: ActivityNotFoundException) {
            bIsZaloOutOfDate = true

            val errorMsg =
                ZaloOAuthResultCode.findErrorMessageByID(ZaloOAuthResultCode.RESULTCODE_ZALO_OUT_OF_DATE)
            wListener.get()
                ?.onAuthenticateError(ZaloOAuthResultCode.RESULTCODE_ZALO_OUT_OF_DATE, errorMsg)
            //tra ve error code
        }

    }

    fun loginViaWeb(activity: Activity) {
        if (!Utils.isOnline(context)) {
            wListener.get()?.onAuthenticateError(
                ZaloOAuthResultCode.RESULTCODE_ZALO_WEBVIEW_NO_NETWORK,
                context.getString(R.string.no_network)
            )
        }

        if (AuthUtils.canUseBrowserLogin(context)) {
            loginViaBrowser(activity)
        } else {
            loginViaWebView(activity)
        }
    }

    private fun loginViaWebView(activity: Activity) {
        val intent = WebLoginActivity.newIntent(activity, false)
        activity.startActivityForResult(intent, Constant.ZALO_AUTHENTICATE_REQUEST_CODE)
    }

    private fun loginViaBrowser(activity: Activity) {

        val url = StringBuilder()
        url.append(
            ServiceMapManager.getInstance().urlFor(
                ServiceMapManager.KEY_URL_OAUTH,
                "/v3/auth?app_id="
            )
        )
        try {
            url.append(AppInfo.getAppIdLong(activity))
                .append("&sign_key=")
                .append(URLEncoder.encode(AppInfo.getApplicationHashKey(activity), "UTF-8"))
                .append("&pkg_name=")
                .append(URLEncoder.encode(activity.getPackageName(), "UTF-8"))
                .append("&orientation=")
                .append(activity.resources.configuration.orientation)
                .append("&ts=").append(System.currentTimeMillis())
                .append("&lang=")
                .append(Utils.getLanguage())
        } catch (e: UnsupportedEncodingException) {
            wListener.get()?.onAuthenticateError(
                ZaloOAuthResultCode.RESULTCODE_UNEXPECTED_ERROR,
                e.message.toString()
            )
            return
        }


        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url.toString())
        activity.startActivity(intent)
    }

    private fun loginViaAppOrWebIfNotLogin(activity: Activity) {
        getZaloLoginStatus(object : GetZaloLoginStatus {
            override fun onGetZaloLoginStatusCompleted(status: Int) {
                if (status == 1) {
                    loginViaApp(activity)
                } else {
                    loginViaWeb(activity)
                }
            }
        })
    }

    override fun getZaloLoginStatus(callback: GetZaloLoginStatus?) {
        if (callback == null) return
        ZTaskExecutor.queueRunnable(Runnable {
            try {
                val status = ZaloService().getUserLoggedStatus(context)
                callback.onGetZaloLoginStatusCompleted(status)
            } catch (e: InterruptedException) {
                Log.w("getZaloLoginStatus", e)
            }
        })
    }

    override fun unAuthenticate() {
        try {
            mStorage.setAccessTokenNewAPI("")
            mStorage.setAuthCode("")
            mStorage.setZaloId(0)
            mStorage.setZaloDisplayName("")
        } catch (ex: Exception) {
            Log.w("unAuthenticate", ex)
        }

    }

    override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (requestCode == Constant.ZALO_AUTHENTICATE_REQUEST_CODE) {
            receiveAuthData(activity, data)
            return true
        }
        return false
    }

    private fun receiveAuthData(activity: Activity, data: Intent?) {
        try {
            activity.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w("receiveAuthData", e)
        }


        if (bIsZaloOutOfDate) {
            return
        }

        mStorage.setAccessTokenNewAPI("")
        if (data == null) {
            wListener.get()?.onAuthenticateError(ZaloOAuthResultCode.RESULTCODE_USER_BACK, "")
            return
        }

        val error = data.getIntExtra("error", Constant.RESULT_CODE_SUCCESSFUL)
        when (error) {
            203 -> wListener.get()?.onAuthenticateError(
                ZaloOAuthResultCode.RESULTCODE_ZALO_WEB_VIEW_LOGIN_NOT_ALLOWED,
                "Không thể đăng nhập Zalo."
            )

            Constant.RESULT_CODE_SUCCESSFUL -> {
                val id = data.getLongExtra(Constant.user.UID, 0)
                val authCode = data.getStringExtra(Constant.user.AUTH_CODE)

                mStorage.setAuthCode(authCode!!)
                mStorage.setZaloId(id)
                try {
                    val jsData = data.getStringExtra("data")
                    val exData = JSONObject(jsData!!).getJSONObject("data")
                    val displayName = exData.getString(Constant.user.DISPLAY_NAME)

                    mStorage.setZaloDisplayName(displayName)

                    val mData = HashMap<String, String>()
                    mData[Constant.user.DISPLAY_NAME] = displayName
                    wListener.get()?.onAuthenticateSuccess(id, authCode, mData)
                } catch (ex: Exception) {
                    Log.e("receiveAuthData", ex)
                }
            }

            Constant.RESULT_CODE_ZALO_NOT_LOGIN -> {
                if (bIsZaloLoginSuccessful) {
                    authenticate(activity, LoginVia.APP, wListener.get())
                } else {
                    wListener.get()
                        ?.onAuthenticateError(ZaloOAuthResultCode.RESULTCODE_USER_BACK, "")
                }
            }

            else -> {
                val e = ZaloOAuthResultCode.findById(error)

                var errorMsg = ZaloOAuthResultCode.findErrorMessageByID(e)
                try {
                    val jsData = data.getStringExtra("data")
                    if (!TextUtils.isEmpty(jsData)) {
                        val exData = JSONObject(jsData!!)
                        val msg = exData.getString("errorMsg")
                        if (msg.isNotEmpty()) errorMsg = msg
                    }
                } catch (ex: Exception) {
                    Log.w("receiveAuthData", "zalo return empty message")
                }

                wListener.get()?.onAuthenticateError(e, errorMsg)
            }
        }
    }
}
