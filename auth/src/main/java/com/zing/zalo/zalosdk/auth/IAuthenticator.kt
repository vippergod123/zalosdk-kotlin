package com.zing.zalo.zalosdk.auth

import android.app.Activity

enum class AuthenticateVia {
    App,
    Web,
    AppOrWeb
}

data class AuthResponse(val errorCode: String, val errorMessage: String) {
    var uId: Long = 0
    var oauthCode: String? = null
}

interface AuthenticateCompleteListener {
    fun onAuthenticateSuccess(uid: Long, code: String, data: Map<String, Any>)
    fun onAuthenticateError(errorCode: Int, message: String)
}

interface IAuthenticator {
    fun authenticate(activity: Activity, via: AuthenticateVia, listener: AuthenticateCompleteListener)
}