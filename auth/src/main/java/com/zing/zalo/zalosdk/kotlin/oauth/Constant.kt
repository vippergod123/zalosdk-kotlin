package com.zing.zalo.zalosdk.kotlin.oauth

import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.core.Constant

@Keep
object Constant {
    val core = Constant
    val user = User

    const val PARAM_APP_ID = "app_id"
    const val PARAM_OAUTH_CODE = "code"
    const val AUTHORIZATION_LOGIN_SUCCESSFUL_ACTION =
        "com.zing.zalo.action.ZALO_LOGIN_SUCCESSFUL_FOR_AUTHORIZATION_APP"
    const val EXTRA_AUTHORIZATION_LOGIN_SUCCESSFUL = "loginSuccessful"

    const val ZALO_AUTHENTICATE_REQUEST_CODE = 0xfcd5
    const val RESULT_CODE_ZALO_NOT_LOGIN = 4
    const val RESULT_CODE_SUCCESSFUL = 0
}

@Keep
object User {
    const val AUTH_CODE = "code"
    const val UID = "uid"
    const val DISPLAY_NAME = "display_name"
    const val IS_REGISTER = "isRegister"
}