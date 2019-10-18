package com.zing.zalo.devicetrackingsdk

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.text.TextUtils
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

@SuppressLint("StaticFieldLeak")
object DeviceTracking : IDeviceTracking {
    const val DID_FILE_NAME = "ddinfo2"

    private const val KEY_DEVICE_ID = "deviceId"
    private const val KEY_DEVICE_ID_EXPIRED_TIME = "expiredTime"

    private lateinit var context: Context
    private var deviceId: String = ""

    var sdkTracking: ISdkTracking? = null
    var deviceIdExpiredTime: Long = 0L

    internal lateinit var getDeviceIdAsyncTask: GetDeviceIdAsyncTask
    var httpClient = HttpClient(
        ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        )
    )
    var request = HttpUrlEncodedRequest(Constant.api.API_HARDWARE_ID_URL)

    fun init(ctx: Context, listener: DeviceTrackingListener?) {
        context = ctx.applicationContext

        loadDeviceIdSetting()

        if (isDeviceIdValid()) return

        runGetDeviceIdAsyncTask(listener)
    }

    override fun getDeviceId(): String? {
        if (!TextUtils.isEmpty(deviceId)) return deviceId

        loadDeviceIdSetting()

        return deviceId
    }

    override fun getDeviceId(listener: DeviceTrackingListener?) {
        if (!TextUtils.isEmpty(getDeviceId())) {
            listener?.onComplete(getDeviceId())
            if (isDeviceIdValid()) return
        }

        runGetDeviceIdAsyncTask(listener)
    }

    fun setDeviceId(deviceId: String, expiredTime: String) {
        if (!isContextInitialized()) return

        val data = JSONObject()
        data.put(KEY_DEVICE_ID, deviceId)
        data.put(KEY_DEVICE_ID_EXPIRED_TIME, expiredTime)
        Utils.writeToFile(context, data.toString(), DID_FILE_NAME)
        Log.d("setDeviceId", "Write file complete")
    }

    private fun isDeviceIdValid(): Boolean {
        return !TextUtils.isEmpty(deviceId) && System.currentTimeMillis() > deviceIdExpiredTime
    }

    //#region private supportive method
    private fun loadDeviceIdSetting() {
        if (!isContextInitialized()) return

        val obj = Utils.readFromFile(context, DID_FILE_NAME)
        if (!TextUtils.isEmpty(obj)) {
            try {
                val data = JSONObject(obj)
                deviceId = data.optString(KEY_DEVICE_ID)
                deviceIdExpiredTime = data.optLong(KEY_DEVICE_ID_EXPIRED_TIME, 0L)
            } catch (e: JSONException) {
                Log.e("loadDeviceIdSetting", e)
            }

        }
        Log.v("loadDeviceIdSetting " + obj.toString())
    }

    @Throws(Exception::class)
    private fun isContextInitialized(): Boolean {
        if (!::context.isInitialized)
            Log.e("isContextInitialized", "Device Tracking must be init context first")

        return ::context.isInitialized
    }

    private fun runGetDeviceIdAsyncTask(listener: DeviceTrackingListener?) {
        val currentMillis = System.currentTimeMillis()
        if (!isDeviceIdValid()) {
            getDeviceIdAsyncTask = GetDeviceIdAsyncTask(
                WeakReference(context), deviceId, currentMillis, listener
            )
            getDeviceIdAsyncTask.execute()
        }
    }
    //#endregion

    class GetDeviceIdAsyncTask(
        private val weakContext: WeakReference<Context>,
        private val currentDeviceId: String,
        private val timestamp: Long,
        private var listener: DeviceTrackingListener?
    ) : AsyncTask<Void, Void, JSONObject?>() {

        override fun doInBackground(vararg params: Void?): JSONObject? {
            val context = weakContext.get()
            try {
                if (context == null) throw Exception("Context is null")
                val storage = Storage(context)

                val deviceIdData = DeviceInfo.prepareDeviceIdData(context)
                val trackingData = DeviceInfo.prepareTrackingData(context, currentDeviceId, timestamp)

                val sdkId = sdkTracking?.getSDKId() ?: ""
                val appId = AppInfo.getAppId(context)
                val authCode = storage.getOAuthCode() ?: ""

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
                    val resultDeviceId = data.optString("deviceId")
                    val duration = data.optLong("expiredTime")
                    val expiredTime = duration + System.currentTimeMillis()

                    val dataJson = JSONObject()
                    dataJson.put("deviceId", resultDeviceId)
                    dataJson.put("expireTime", expiredTime)

                    deviceId = resultDeviceId
                    deviceIdExpiredTime = expiredTime
                    setDeviceId(deviceId, expiredTime.toString())

                    return dataJson
                }

            } catch (ex: JSONException) {
                Log.e("GetDeviceIdAsyncTask", ex)
            } catch (ex: Exception) {
                Log.e("GetDeviceIdAsyncTask", ex)
            }
            return null
        }


        /*
         * Listener must be called in onPostExecute to avoid when doInBackground() return null
         */
        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)
            val deviceId = result?.optString("deviceId")
            listener?.onComplete(deviceId)
            listener = null
        }
    }
}
