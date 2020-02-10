package com.zing.zalo.zalosdk.kotlin.oauth

import android.app.Activity
import android.content.Intent
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.oauth.callback.GetZaloLoginStatus
import com.zing.zalo.zalosdk.kotlin.oauth.callback.ValidateOAuthCodeCallback

@Keep
interface IAuthenticateCompleteListener {
    fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>)
    fun onAuthenticateError(errorCode: Int, message: String)
}

@Keep
interface IAuthenticator {
    fun authenticate(activity: Activity, via: LoginVia, listener: IAuthenticateCompleteListener?)
    fun registerZalo(activity: Activity, listener: IAuthenticateCompleteListener?)
    fun isAuthenticate(code: String, callback: ValidateOAuthCodeCallback?): Boolean
    fun unAuthenticate()
    fun getZaloLoginStatus(callback: GetZaloLoginStatus?)
    fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean
}
