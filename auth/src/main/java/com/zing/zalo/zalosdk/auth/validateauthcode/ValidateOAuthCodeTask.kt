package com.zing.zalo.zalosdk.auth.validateauthcode

import android.os.AsyncTask
import com.zing.zalo.zalosdk.auth.Constant
import com.zing.zalo.zalosdk.auth.ZaloOAuthResultCode
import com.zing.zalo.zalosdk.auth.ZaloSDK.unAuthenticate
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpClientRequest
import com.zing.zalo.zalosdk.core.http.HttpMethod
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONObject

internal class ValidateOAuthCodeTask(
    private val authCode: String,
    private val appID: String,
    private val appVersion: String,
    private val isOnline: Boolean,
    private val callback: ValidateOAuthCodeCallback?
) :
    AsyncTask<Void, Void, JSONObject>() {

    private var httpClient: HttpClient = HttpClient()
    fun setHttpClient(mHttpClient: HttpClient) {
        this.httpClient = mHttpClient
    }

    init {
        if (callback == null) throw IllegalArgumentException("callback can't be null")
    }

    override fun doInBackground(vararg arg0: Void): JSONObject? {
        val mainURL = ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_OAUTH,
            "/v2/mobile/validate_oauth_code"
        )

        val request = HttpClientRequest(HttpMethod.POST, mainURL)
        request.addQueryStringParameter(Constant.PARAM_APP_ID, appID)
        request.addQueryStringParameter(Constant.PARAM_OAUTH_CODE, authCode)
        request.addQueryStringParameter("version", appVersion)
        request.addQueryStringParameter("frm", "sdk")

        val text = httpClient.send(request).getText() ?: return null

        return try {
            JSONObject(text)
        } catch (ex: java.lang.Exception) {
            Log.d(ex.toString())
            null
        }
    }

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
        try {
            if (result != null) {
                val errorCode = result.getInt("error")

                if (errorCode == 0) {
                    val data = result.getJSONObject("data")
                    val id = data.getLong("uid")

                    callback!!.onValidateComplete(true, 0, id, authCode)
                } else {
                    callback!!.onValidateComplete(false, errorCode, -1, null)
                }
            } else {
                if (!isOnline) callback!!.onValidateComplete(
                    false,
                    ZaloOAuthResultCode.RESULTCODE_ZALO_SDK_NO_INTERNET_ACCESS,
                    -1,
                    null
                )
                else callback!!.onValidateComplete(
                    false,
                    ZaloOAuthResultCode.RESULTCODE_ZALO_UNKNOWN_ERROR,
                    -1,
                    null
                )
            }


        } catch (ex: Exception) {
            unAuthenticate()
            callback!!.onValidateComplete(
                false,
                ZaloOAuthResultCode.RESULTCODE_UNEXPECTED_ERROR,
                -1,
                null
            )
        }

    }
}
