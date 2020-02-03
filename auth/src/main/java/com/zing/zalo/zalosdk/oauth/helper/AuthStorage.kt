package com.zing.zalo.zalosdk.oauth.helper

import android.content.Context
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.oauth.Constant

@Keep
class AuthStorage(ctx: Context) : Storage(ctx) {
    private val prefZaloID = Constant.core.sharedPreference.PREF_ZALO_ID
    private val prefZaloDisplayName = Constant.core.sharedPreference.PREF_ZALO_DISPLAY_NAME
    private val prefAccessTokenNewApi = Constant.core.sharedPreference.PREF_ACCESS_TOKEN_NEW_API


    fun getZaloId(): Long? {
        return getLong(prefZaloID)
    }

    fun setZaloId(id: Long) {
        setLong(prefZaloID, id)
    }

    fun getZaloDisplayName(): String? {
        return getString(prefZaloDisplayName)
    }

    fun setZaloDisplayName(displayname: String) {
        setString(prefZaloDisplayName, displayname)
    }

    fun setAccessTokenNewAPI(token: String) {
        setString(prefAccessTokenNewApi, token)
    }

}