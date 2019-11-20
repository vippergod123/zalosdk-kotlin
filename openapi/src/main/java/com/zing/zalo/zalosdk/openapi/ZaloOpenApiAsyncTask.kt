package com.zing.zalo.zalosdk.openapi

import android.content.Context
import android.os.AsyncTask
import android.text.TextUtils
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.core.http.IHttpRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import com.zing.zalo.zalosdk.oauth.ZaloOAuthResultCode
import com.zing.zalo.zalosdk.oauth.ZaloSDK
import com.zing.zalo.zalosdk.oauth.helper.AuthStorage
import com.zing.zalo.zalosdk.openapi.exception.OpenApiException
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class GetAccessTokenAsyncTask(
    private var weakContext: WeakReference<Context>,
    internal var callback: ZaloOpenApiCallback?
) :
    AsyncTask<Void, Void, JSONObject?>() {
    var request = HttpUrlEncodedRequest(Constant.api.AUTH_MOBILE_ACCESS_TOKEN_PATH)
    var httpClient = HttpClient(ServiceMapManager.urlFor(ServiceMapManager.KEY_URL_OAUTH))
    private lateinit var authStorage: AuthStorage

    override fun doInBackground(vararg p0: Void?): JSONObject? {
        val context = weakContext.get()
        try {
            if (context == null) throw Exception("Context is null")

            return getAccessToken(context)
                ?: return JSONObject("{\"error\":" + ZaloOAuthResultCode.RESULTCODE_CREATE_ACCESS_TOKEN_FAILED + "}")


        }
        catch (ex: OpenApiException) {
            Log.w("GetAccessTokenAsyncTask", ex)
            return JSONObject("{\"error\":" + ZaloOAuthResultCode.RESULTCODE_ZALO_OAUTH_INVALID + "}")
        }
        catch (ex: JSONException) {
            Log.e("GetAccessTokenAsyncTask", ex)
        }
        catch (ex: Exception) {
            Log.e("GetAccessTokenAsyncTask", ex)
        }

        return null
    }

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
        try {
            if (result != null) {
                callback?.onResult(result)
            } else {
                callback?.onResult(JSONObject("{\"error\":" + ZaloOAuthResultCode.RESULTCODE_ZALO_UNKNOWN_ERROR + "}"))
            }
        } catch (ex: Exception) {
            Log.e("GetAccessTokenAsyncTask - onPostExecute", ex)
            callback?.onResult(null)
        }

    }

    private fun getAccessToken(context: Context): JSONObject? {
        authStorage = AuthStorage(context)
        val oauthCode = authStorage.getOAuthCode() ?: ""
        if (TextUtils.isEmpty(oauthCode)) throw OpenApiException("Auth code is invalid - Login again!")
        request.addQueryStringParameter("code", oauthCode)
        request.addQueryStringParameter("pkg_name", AppInfo.getPackageName(context))
        request.addQueryStringParameter("sign_key", AppInfo.getApplicationHashKey(context) ?: "")
        request.addQueryStringParameter("app_id", AppInfo.getAppId(context) + "")
        request.addQueryStringParameter("version", ZaloSDK.getInstance().getVersion())
        request.addQueryStringParameter(
            "zdevice",
            DeviceInfo.prepareDeviceIdData(context).toString()
        )
        request.addQueryStringParameter(
            "ztracking",
            DeviceInfo.prepareTrackingData(
                context,
                DeviceTracking.getInstance().getDeviceId() ?: "",
                System.currentTimeMillis()
            ).toString()

        )
        request.addHeader("gid", DeviceTracking.getInstance().getDeviceId() ?: "")

        val jsonObject = httpClient.send(request).getJSON() ?: return null
        val errorCode = jsonObject.getInt("error")
        if (errorCode == 0) {
            val accessToken = jsonObject.optJSONObject("data") ?: JSONObject()
            accessToken.put("error", 0)

            if (!TextUtils.isEmpty(accessToken.toString())) {
                val time =
                    System.currentTimeMillis() + Utils.convertTimeToMilliSeconds(1, TimeUnit.HOURS)

                //accessToken.optInt("expires_in", 0)*1000 + System.currentTimeMillis() - 60000;
                accessToken.put("expires_in", time)
                authStorage.setAccessTokenNewAPI(accessToken.toString())

                return accessToken
            }
        }

        return null
    }
}

class CallApiAsyncTask(private val callback: ZaloOpenApiCallback?): AsyncTask<IHttpRequest, Void, JSONObject?>() {
    var httpClient = HttpClient(ServiceMapManager.urlFor(ServiceMapManager.KEY_URL_GRAPH))
    override fun doInBackground(vararg req: IHttpRequest?): JSONObject? {
        if (req[0] == null) return null
        return httpClient.send(req[0] as IHttpRequest).getJSON()
    }

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
        callback?.onResult(result)
    }
}