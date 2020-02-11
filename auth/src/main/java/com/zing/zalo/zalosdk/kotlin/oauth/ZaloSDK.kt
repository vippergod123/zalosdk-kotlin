package com.zing.zalo.zalosdk.kotlin.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.oauth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.kotlin.oauth.callback.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.kotlin.oauth.helper.AuthStorage

class ZaloSDK(context: Context) {

    private var mStorage = AuthStorage(context)
    private var mAuthenticator = Authenticator(context, mStorage)

    init {
        verifyConfig(context)
    }


    /**
     * Authenticate by using Zalo account
     * @param activity Activity the login activity
     * @param loginVia not support, SDK will login with Zalo app only.
     * @param listener AuthCompleteListenerI listener to receive authenticate event
     */
    fun authenticate(
        activity: Activity,
        loginVia: LoginVia,
        listener: IAuthenticateCompleteListener?
    ) {
        mAuthenticator.authenticate(activity, loginVia, listener)
    }

    /**
     * Get authentication code
     */
    fun getOauthCode() :String?  {
        return mStorage.getOAuthCode()
    }

    /**
     * Logout current Zalo's account
     */
    fun unAuthenticate() {
            mAuthenticator.unAuthenticate()
    }

    fun registerZalo(activity: Activity, listener: IAuthenticateCompleteListener?) {
            mAuthenticator.registerZalo(activity, listener)
    }

    fun getZaloLoginStatus(callback: GetZaloLoginStatus?) {
            mAuthenticator.getZaloLoginStatus(callback)
    }

    /**
     * Check if users have already authenticated.
     * @param callback Callback will be called after verify with server. If passed null, no server verification will be made.
     * @return True if oauth code cached, otherwise false
     */
    fun isAuthenticate(callback: ValidateOAuthCodeCallback?): Boolean {
            return mAuthenticator.isAuthenticate(mStorage.getOAuthCode().toString(), callback)!!
    }

    fun getVersion(): String {
        return Constant.core.VERSION
    }

    fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
            return mAuthenticator.onActivityResult(activity, requestCode, resultCode, data)!!
    }


    //#region private supportive method
    private fun verifyConfig(context: Context) {
        val res = context.resources
        try {
            if (res.getString(R.string.zalosdk_app_id).equals("missing-app-id")) {
                Log.e("Missing zalosdk_app_id in strings.xml!!")
            }

            if (res.getString(R.string.zalosdk_login_protocol_schema).equals("missing-protocol-schema")) {
                Log.e("Missing zalosdk_login_protocol_schema in strings.xml, please define it as \"zalo-[app_id]\" !!")
            }
        } catch (ignored: Exception) {
            Log.e("ZaloSDK", ignored)
        }
    }
    //#endregion
}