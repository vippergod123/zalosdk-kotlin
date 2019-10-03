package com.zing.zalo.devicetrackingsdk

import android.content.Context
import android.os.AsyncTask
import android.text.TextUtils
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

class SdkTracking(var context: Context) : ISdkTracking {
    internal var sdkStorage = Storage(context)

    internal lateinit var getSdkIdAsyncTask: GetSdkIdAsyncTask

    fun setSDKId(value: String) {
        sdkStorage.setString(Constant.sharedPreference.PREF_SDK_ID, value)
    }

    override fun getSDKId(): String? {
        return sdkStorage.getString(Constant.sharedPreference.PREF_SDK_ID)
    }

    fun getSDKId(listener: SdkTrackingListener?) {
        val sdkId = getSDKId()
        if (!TextUtils.isEmpty(sdkId)) {
            listener?.onComplete(sdkId)
            return
        }

        runGetSdkIDAsyncTask(listener)
    }

    override fun getPrivateKey(): String? {
        return sdkStorage.getString(Constant.sharedPreference.PREF_PRIVATE_KEY)
    }

    fun setPrivateKey(value: String) {
        sdkStorage.setString(Constant.sharedPreference.PREF_PRIVATE_KEY, value)
    }

    fun runGetSdkIDAsyncTask(listener: SdkTrackingListener?) {
        if (!::getSdkIdAsyncTask.isInitialized)
            getSdkIdAsyncTask = GetSdkIdAsyncTask(
                WeakReference(context), listener
            )
        getSdkIdAsyncTask.execute()
    }

    class GetSdkIdAsyncTask(
        private val weakContext: WeakReference<Context>,
        private var listener: SdkTrackingListener?
    ) : AsyncTask<Void, Void, JSONObject>() {

        var httpClient = HttpClient(
            ServiceMapManager.urlFor(
                ServiceMapManager.KEY_URL_CENTRALIZED
            )
        )
        var request = HttpUrlEncodedRequest(Constant.api.API_SDK_ID)

        override fun doInBackground(vararg params: Void?): JSONObject? {
            val context = weakContext.get()
            try {
                if (context == null) throw Exception("Context is null")

                val deviceIdData = DeviceInfo.trackingData(context).toString()
                request.addParameter("appId", AppInfo.getAppId(context))
                request.addParameter("sdkv", DeviceInfo.getSDKVersion())
                request.addParameter("pl", "android")
                request.addParameter("osv", DeviceInfo.getOSVersion())
                request.addParameter("model", DeviceInfo.getModel())
                request.addParameter("screenSize", DeviceInfo.getScreenSize(context))
                request.addParameter("device", deviceIdData)
                request.addParameter("ref", AppInfo.getReferrer(context))

                val jsonObject = httpClient.send(request).getJSON()
                val errorCode = jsonObject?.getInt("error")
                if (errorCode == 0) {
                    val data = jsonObject.getJSONObject("data")
                    val sdkId = data.optString("sdkId")
                    val privateKey = data.optString("privateKey")

                    val dataJson = JSONObject()
                    dataJson.put("sdkId", sdkId)
                    dataJson.put("privateKey", privateKey)

                    val sdkTracking = SdkTracking(context)
                    sdkTracking.setSDKId(sdkId)
                    sdkTracking.setPrivateKey(privateKey)

                    return dataJson
                }
            } catch (ex: JSONException) {
                Log.e("GetSdkId", ex)
            } catch (ex: Exception) {
                Log.e("GetSdkId", ex)
            }
            return null
        }

        /*
    * Listener must be called in onPostExecute to avoid when doInBackground() return null
    */
        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)

            val data = result?.toString()
            listener?.onComplete(data)
            listener = null
        }
    }

}