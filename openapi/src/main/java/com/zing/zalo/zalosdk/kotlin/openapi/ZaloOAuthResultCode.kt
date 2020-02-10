package com.zing.zalo.zalosdk.kotlin.openapi

import android.content.res.Resources
import androidx.annotation.Keep

@Keep
object ZaloOAuthResultCode {
    const val RESULTCODE_NO_ERROR = 0
    const val RESULTCODE_PERMISSION_DENIED = -201
    const val RESULTCODE_USER_BACK = -1111
    const val RESULTCODE_USER_REJECT = -1114
    const val RESULTCODE_ZALO_UNKNOWN_ERROR = -1112
    const val RESULTCODE_ZALO_WEB_VIEW_LOGIN_NOT_ALLOWED = -1118
    const val RESULTCODE_UNEXPECTED_ERROR = -1000
    const val RESULTCODE_INVALID_APP_ID = -1001
    const val RESULTCODE_INVALID_PARAM = -1002
    const val RESULTCODE_INVALID_SECRET_KEY = -1003
    const val RESULTCODE_INVALID_OAUTH_CODE = -1004
    const val RESULTCODE_ACCESS_DENIED = -1005
    const val RESULTCODE_INVALID_SESSION = -1006
    const val RESULTCODE_CREATE_OAUTH_FAILED = -1007
    const val RESULTCODE_CREATE_ACCESS_TOKEN_FAILED = -1008
    const val RESULTCODE_USER_CONSENT_FAILED = -1009
    const val RESULTCODE_APPLICATION_IS_NOT_APPROVED = -1014
    const val RESULTCODE_ZALO_OAUTH_INVALID = -1019
    const val RESULTCODE_ZALO_WEBVIEW_NO_NETWORK = -1021
    const val RESULTCODE_ZALO_SDK_NO_INTERNET_ACCESS = -1022
    const val RESULTCODE_ZALO_APPLICATION_NOT_INSTALLED = -1024
    const val RESULTCODE_ZALO_OUT_OF_DATE = -1025


    fun findById(rawCode: Int): Int {
        when (rawCode) {
            0 -> return RESULTCODE_NO_ERROR
            1 -> return RESULTCODE_ZALO_UNKNOWN_ERROR
            2 -> return RESULTCODE_USER_BACK
            3 -> return RESULTCODE_USER_REJECT
//            4 -> return
//            5 -> return
//            6 -> return
            -201 -> return RESULTCODE_PERMISSION_DENIED
            -1000 -> return RESULTCODE_UNEXPECTED_ERROR
            -1001 -> return RESULTCODE_INVALID_APP_ID
            -1002 -> return RESULTCODE_INVALID_PARAM
            -1003 -> return RESULTCODE_INVALID_SECRET_KEY
            -1004 -> return RESULTCODE_INVALID_OAUTH_CODE
            -1005 -> return RESULTCODE_ACCESS_DENIED
            -1006 -> return RESULTCODE_INVALID_SESSION
            -1007 -> return RESULTCODE_CREATE_OAUTH_FAILED
            -1008 -> return RESULTCODE_CREATE_ACCESS_TOKEN_FAILED
            -1009 -> return RESULTCODE_USER_CONSENT_FAILED
            -1014 -> return RESULTCODE_APPLICATION_IS_NOT_APPROVED
            -1019 -> return RESULTCODE_ZALO_OAUTH_INVALID
            -1021 -> return RESULTCODE_ZALO_WEBVIEW_NO_NETWORK
            -1022 -> return RESULTCODE_ZALO_SDK_NO_INTERNET_ACCESS
            -1024 -> return RESULTCODE_ZALO_APPLICATION_NOT_INSTALLED
            -1025 -> return RESULTCODE_ZALO_OUT_OF_DATE
            else -> return rawCode
        }
    }

    fun findErrorMessageByID(rawCode: Int): String {
        return when (rawCode) {
            RESULTCODE_ZALO_UNKNOWN_ERROR -> "Lỗi không xác định"
            RESULTCODE_ZALO_SDK_NO_INTERNET_ACCESS -> Resources.getSystem().getString(R.string.no_network)
            RESULTCODE_ZALO_APPLICATION_NOT_INSTALLED -> Resources.getSystem().getString(R.string.zalo_app_not_installed)
            RESULTCODE_ZALO_OUT_OF_DATE -> Resources.getSystem().getString(R.string.zalo_app_out_of_date)
            RESULTCODE_ZALO_OAUTH_INVALID -> "OAuth Code is invalid"
            else -> "Không thể đăng nhập Zalo."
        }
    }
}