package com.zing.zalo.devicetrackingsdk

import android.content.Context
import android.os.AsyncTask
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference


object DeviceTrackingAsyncTask {
    /**
     * Call when data not save in default storage
     * @param weakContext can be collected by GB
     * @param listener callback privateKey & SdkID
     */
    class GetSdkId(
        private val weakContext: WeakReference<Context>,
        private var listener: DeviceTrackingListener?
    ) : AsyncTask<Void, Void, JSONObject>() {

        var httpClient = HttpClient(ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        ))
        var request = HttpUrlEncodedRequest(Constant.api.API_SDK_ID)

        private var api = weakContext.get()?.let { Api(it) }

        override fun doInBackground(vararg params: Void?): JSONObject? {
            val context = weakContext.get()
            try {
                if (context == null) throw Exception("Context is null")
                if (api == null) throw Exception("Api cannot be initialized")



                val deviceIdData = api?.prepareDeviceIdData().toString()

                request.addParameter("appId", AppInfo.getAppId(context))
                request.addParameter("sdkv", DeviceInfo.getSDKVersion())
                request.addParameter("pl", "android")
                request.addParameter("osv", DeviceInfo.getOSVersion())
                request.addParameter("model", DeviceInfo.getModel())
                request.addParameter("screenSize", DeviceInfo.getScreenSize(context))
                request.addParameter("device", deviceIdData)
                request.addParameter("ref", AppInfo.getReferrer(context))


                val jsonObject  = httpClient.send(request).getJSON()
                val errorCode = jsonObject?.getInt("error")
                if (errorCode == 0) {
                    val data = jsonObject.getJSONObject("data")
                    val sdkId = data.optString("sdkId")
                    val privateKey = data.optString("privateKey")

                    val dataJson = JSONObject()
                    dataJson.put("sdkId", sdkId)
                    dataJson.put("privateKey", privateKey)

                    DeviceTracking.setSDKId(sdkId)
                    DeviceTracking.setPrivateKey(privateKey)

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

    class GetDeviceId(
        private val weakContext: WeakReference<Context>,
        private val currentDeviceId: String,
        private val timestamp: Long,
        private var listener: DeviceTrackingListener?
    ) : AsyncTask<Void, Void, JSONObject?>() {

        var httpClient = HttpClient(ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        ))

        var request = HttpUrlEncodedRequest(Constant.api.API_HARDWARE_ID_URL)
        private var api = weakContext.get()?.let { Api(it) }

        override fun doInBackground(vararg params: Void?): JSONObject? {
            val context = weakContext.get()
            try {
                if (context == null) throw Exception("Context is null")
                if (api == null) throw Exception("Api cannot be initialized")

                val deviceIdData = api?.prepareDeviceIdData()
                val trackingData = api?.prepareTrackingData(currentDeviceId, timestamp)

                val sdkId = DeviceTracking.getSDKId() ?: ""
                val appId = AppInfo.getAppId(context)
                val authCode = Storage(context).getOAuthCode() ?: ""

                val param = arrayOf("pl", "appId", "oauthCode", "device", "data", "ts", "sdkId")
                val values = arrayOf(
                    "android",
                    appId,
                    authCode,
                    deviceIdData.toString(),
                    trackingData.toString(),
                    timestamp.toString(),
                    sdkId
                )

                val sig = Utils.getSignature(
                    param,
                    values,
                    Constant.key.TRK_SECRET_KEY
                )

                request.addQueryStringParameter("pl", "android")
                request.addQueryStringParameter("appId", appId)
                request.addQueryStringParameter("oauthCode", authCode)
                request.addQueryStringParameter("device", deviceIdData.toString())
                request.addQueryStringParameter("data", trackingData.toString())
                request.addQueryStringParameter("ts", "" + timestamp)
                request.addQueryStringParameter("sig", sig)
                request.addQueryStringParameter("sdkId", sdkId)

                val jsonObject = httpClient.send(request).getJSON()

                val errorCode = jsonObject?.getInt("error")
                if (errorCode == 0) {
                    val data = jsonObject.getJSONObject("data")
                    val deviceId = data.optString("deviceId")

                    // expiredTime = duration + currentTime
                    val duration = data.optLong("expiredTime")
                    val expiredTime = duration + System.currentTimeMillis()

                    val dataJson = JSONObject()
                    dataJson.put("deviceId", deviceId)
                    dataJson.put("expireTime", expiredTime)

                    DeviceTracking.setDeviceId(deviceId, expiredTime.toString())

                    return dataJson
                }


            } catch (ex: JSONException) {
                Log.e("GetDeviceId", ex)
            } catch (ex: Exception) {
                Log.e("GetDeviceId", ex)
            }
            return null
        }


        /*
         * Listener must be called in onPostExecute to avoid when doInBackground() return null
         */
        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)

            val deviceId = result?.optString("deviceId")
            listener?.onDeviceIdSuccess(deviceId)

            val data = result?.toString()
            listener?.onComplete(data)
            listener = null
        }

    }
}
