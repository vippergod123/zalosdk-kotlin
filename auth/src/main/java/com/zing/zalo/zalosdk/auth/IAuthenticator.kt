package com.zing.zalo.zalosdk.auth

import android.app.Activity
import android.content.Context
import com.zing.zalo.zalosdk.auth.validateauthcode.ValidateOAuthCodeCallback

interface IAuthenticateCompleteListener
{
	fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>)
	fun onAuthenticateError(errorCode: Int, message: String)
}

interface IAuthenticator
{
	fun authenticate(activity: Activity, via: LoginVia, listener: IAuthenticateCompleteListener?)
	fun registerZalo(activity: Activity, listener: IAuthenticateCompleteListener?)
	fun isAuthenticate(code: String, callback: ValidateOAuthCodeCallback?): Boolean
}
