package com.zing.zalo.zalosdk.oauth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.module.BaseModule
import com.zing.zalo.zalosdk.core.module.ModuleManager
import com.zing.zalo.zalosdk.oauth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.oauth.callback.ValidateOAuthCodeCallback
import com.zing.zalo.zalosdk.oauth.helper.AuthStorage

@SuppressLint("StaticFieldLeak")
class ZaloSDK : BaseModule() {
    @Keep
    companion object {
        private val instance = ZaloSDK()

        fun getInstance(): ZaloSDK {
            return instance
        }

        init {
            ModuleManager.addModule(instance)
        }
    }


    private var mAuthenticator: IAuthenticator? = null
    private var mStorage: AuthStorage? = null

    override fun onStart(context: Context) {
        super.onStart(context)

        mStorage = AuthStorage(context)
        mAuthenticator = Authenticator(context, mStorage!!)
        verifyConfig(context)
        Log.d("ZaloSDK", "ZaloSDK isInitialized")
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
        listener: IAuthenticateCompleteListener
    ) {
        if (checkInitialize())
            mAuthenticator?.authenticate(activity, loginVia, listener)
    }


    /**
     * Logout current Zalo's account
     */
    fun unAuthenticate() {
        if (checkInitialize())
            mAuthenticator?.unAuthenticate()
    }

    fun registerZalo(activity: Activity, listener: IAuthenticateCompleteListener) {
        if (checkInitialize())
            mAuthenticator?.registerZalo(activity, listener)
    }

    fun getZaloLoginStatus(callback: GetZaloLoginStatus) {
        if (checkInitialize())
            mAuthenticator?.getZaloLoginStatus(callback)
    }

    /**
     * Check if users have already authenticated.
     * @param callback Callback will be called after verify with server. If passed null, no server verification will be made.
     * @return True if oauth code cached, otherwise false
     */
    fun isAuthenticate(callback: ValidateOAuthCodeCallback): Boolean {
        if (checkInitialize()) {
            return mAuthenticator?.isAuthenticate(mStorage?.getOAuthCode().toString(), callback)!!
        }
        return false
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
        if (checkInitialize())
            return mAuthenticator?.onActivityResult(activity, requestCode, resultCode, data)!!
        return false
    }

    private fun checkInitialize(): Boolean {
        if (getInstance().hasContext && mAuthenticator != null)
            return true

        return false
    }

    private fun verifyConfig(context: Context) {
        val res = context.resources
        try {
            if (res.getString(R.string.zalosdk_app_id).equals("missing-app-id")) {
                Log.e("Missing zalosdk_app_id in strings.xml!!");
            }

            if (res.getString(R.string.zalosdk_login_protocol_schema).equals("missing-protocol-schema")) {
                Log.e("Missing zalosdk_login_protocol_schema in strings.xml, please define it as \"zalo-[app_id]\" !!");
            }
        } catch (ignored: Exception) {
        }
    }
}