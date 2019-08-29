@file:JvmName("Constants")

package com.zing.zalo.zalosdk.core

object Constant {
    const val VERSION = "4.0"
    const val DEV_MODE = false
    const val ZALO_PACKAGE_NAME = "com.zing.zalo"

    val sharedPreference = MySharedPreference
    val nameSharedPreference = NameSharedPreference
    val api = Api
}

object MySharedPreference {
    const val PREF_OAUTH_CODE = "PREFERECE_ZALO_SDK_OAUTH_CODE"
    const val PREF_ZALO_ID = "PREFERECE_ZALO_SDK_ZALO_ID"
    const val PREF_ZALO_DISPLAY_NAME = "PREFERECE_ZALO_SDK_ZALO_DISPLAY_NAME"
    const val PREF_ACCESS_TOKEN_NEW_API = "PREF_NEW_API_ACCESSTOKEN"

    const val PREF_KEY_URL_AUTH = "PREFERECE_KEY_URL_OAUTH"
    const val PREF_KEY_URL_CENTRALIZED = "PREFERECE_KEY_URL_CENTRALIZED"
    const val PREF_KEY_URL_GRAPH = "PREFERECE_KEY_URL_GRAPH"
    const val PREF_EXPIRE_TIME = "PREFERCE_EXPIRE_TIME"

    const val PREFS_NAME_WAKEUP = "com.zing.zalo.sdk.preload.wakeup"
    const val PREFS_NAME_PRELOAD = "com.zing.zalo.sdk.preload"
}


object NameSharedPreference {
    const val ADVERTISE_ID = "adsid"
}
object Api {
    const val API_SDK_ID_URL = "/sdk/mobile/android"
    const val API_GET_SETTING_URL = "/sdk/mobile/setting"
}
