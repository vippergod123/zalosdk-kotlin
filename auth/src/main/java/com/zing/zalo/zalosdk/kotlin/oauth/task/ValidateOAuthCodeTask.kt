package com.zing.zalo.zalosdk.kotlin.oauth.task

import android.os.AsyncTask
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.oauth.Constant
import com.zing.zalo.zalosdk.kotlin.oauth.ZaloOAuthResultCode
import com.zing.zalo.zalosdk.kotlin.oauth.callback.ValidateOAuthCodeCallback

internal class ValidateOAuthCodeTask(
    private val httpClient: HttpClient,
    private val authCode: String,
    private val appID: String,
    private val appVersion: String,
    private val isOnline: Boolean,
    private val callback: ValidateOAuthCodeCallback?
) :
    AsyncTask<Void, Void, Boolean>() {

    private var errorCode = 0
    private var uid = 0L

    init {
        if (callback == null) throw IllegalArgumentException("callback can't be null")
    }

    override fun doInBackground(vararg arg0: Void): Boolean {

        try {
            val request = HttpUrlEncodedRequest("/v2/mobile/validate_oauth_code")
            request.addParameter(Constant.PARAM_APP_ID, appID)
            request.addParameter(Constant.PARAM_OAUTH_CODE, authCode)
            request.addParameter("version", appVersion)
            request.addParameter("frm", "sdk")

            val json = httpClient.send(request).getJSON()

            if (json != null) {
                errorCode = json.getInt("error")

                if (errorCode == 0) {
                    val data = json.getJSONObject("data")
                    uid = data.getLong("uid")

                    return true
                }
            }

            if (!isOnline) {
                errorCode = ZaloOAuthResultCode.RESULTCODE_ZALO_SDK_NO_INTERNET_ACCESS
            } else {
                errorCode = ZaloOAuthResultCode.RESULTCODE_ZALO_UNKNOWN_ERROR
            }

        } catch (ex: Exception) {
            Log.w("ValidateOAuthCodeTask", ex)
            errorCode = ZaloOAuthResultCode.RESULTCODE_UNEXPECTED_ERROR
        }

        return false
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        callback?.onValidateComplete(
            result,
            errorCode,
            uid,
            authCode
        )
    }
}
