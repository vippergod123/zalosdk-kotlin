package com.zing.zalo.zalosdk.kotlin.openapi

import android.content.Context
import android.text.TextUtils
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.json.JSONException
import org.json.JSONObject

@Keep
class OpenApiStorage(ctx: Context) : Storage(ctx) {
    private val prefAccessTokenNewApi = Constant.sharedPreference.PREF_ACCESS_TOKEN_NEW_API

    fun getAccessTokenNewAPI(): JSONObject? {
        return try {
            val accessToken = getString(prefAccessTokenNewApi) ?: ""
            if (TextUtils.isEmpty(accessToken)) throw Exception("Access token is empty in auth storage")
            JSONObject(accessToken)
        } catch (ex: JSONException) {
            Log.e("getAccessTokenNewAPI", ex)
            null
        } catch (ex: Exception) {
            Log.w("getAccessTokenNewAPI", ex)
            null
        }
    }

    fun setAccessTokenNewAPI(token: String) {
        setString(prefAccessTokenNewApi, token)
    }

}