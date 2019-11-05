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
import com.zing.zalo.zalosdk.core.module.BaseModule
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
class DeviceTracking private constructor(): BaseModule(), IDeviceTracking {
    companion object {
        private val instance = DeviceTracking()
        fun getInstance() : DeviceTracking { return instance }

        const val DID_FILE_NAME = "ddinfo2"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_DEVICE_ID_EXPIRED_TIME = "expiredTime"
    }

    private var deviceId: String? = null
    var deviceIdExpiredTime: Long = 0L
    lateinit var sdkTracking: SdkTracking
    private var listeners = mutableListOf<DeviceTrackingListener>()
    private val loading = AtomicBoolean(false)

    var httpClient = HttpClient(
        ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        )
    )

    override fun onStart(context: Context) {
        loadDeviceIdSetting()

        if (isDeviceIdValid()) {
            return
        }

        runGetDeviceIdAsyncTask()
    }

    override fun onStop() {
        deviceIdExpiredTime = 0
        deviceId = null
    }

    override fun getDeviceId(): String? {
        if (TextUtils.isEmpty(deviceId)|| !isDeviceIdValid()) {
            runGetDeviceIdAsyncTask()
        }

        return deviceId
    }

    override fun getDeviceId(listener: DeviceTrackingListener?) {
        if (deviceId != null) {
            listener?.onComplete(deviceId!!)
            if(isDeviceIdValid()) {
                return
            }
        } else {
            listener?.let { listeners.add(it) }
        }

        runGetDeviceIdAsyncTask()
    }

    private fun setDeviceId(deviceId: String, expiredTime: Long) {
        if (!isContextInitialized()) return

        val data = JSONObject()
        data.put(KEY_DEVICE_ID, deviceId)
        data.put(KEY_DEVICE_ID_EXPIRED_TIME, expiredTime)
        Utils.writeToFile(context!!, data.toString(), DID_FILE_NAME)
        Log.d("setDeviceId", "Write file complete $deviceId $expiredTime")
    }

    private fun isDeviceIdValid(): Boolean {
        return !TextUtils.isEmpty(deviceId) && System.currentTimeMillis() < deviceIdExpiredTime
    }

    //#region private supportive method
    private fun loadDeviceIdSetting() {
        if (!isContextInitialized()) return

        val obj = Utils.readFromFile(context!!, DID_FILE_NAME)
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

    private fun isContextInitialized(): Boolean {
        if (!hasContext)
            Log.e("isContextInitialized", "Device Tracking must be init context first")

        return hasContext
    }

    private fun runGetDeviceIdAsyncTask() {
        if(loading.get() || !hasContext) return

        loading.set(true)
        val currentMillis = System.currentTimeMillis()
        val task  = GetDeviceIdAsyncTask(
            context!!, sdkTracking, httpClient, deviceId, currentMillis
        ) { dId, expireTime ->
            if(dId.isNotEmpty()) {
                deviceId = dId
                deviceIdExpiredTime = expireTime + System.currentTimeMillis()
                setDeviceId(dId, deviceIdExpiredTime)

                val it = listeners.iterator()
                while (it.hasNext()) {
                    it.next().onComplete(dId)
                }
                listeners.clear()
            }

            loading.set(false)
        }
        task.execute()

    }
    //#endregion
}

private typealias GetDeviceIdAsyncTaskCallback = (String, Long) -> Unit
@SuppressLint("StaticFieldLeak")
private class GetDeviceIdAsyncTask(
    private val context: Context,
    private val sdkTracking: SdkTracking,
    private val httpClient: HttpClient,
    private val currentDeviceId: String?,
    private val timestamp: Long,
    private var callback: GetDeviceIdAsyncTaskCallback
) : AsyncTask<Void, Void, JSONObject?>() {

    override fun doInBackground(vararg params: Void?): JSONObject? {
        try {
            val storage = Storage(context)

            val deviceIdData = DeviceInfo.prepareDeviceIdData(context)
            val trackingData = DeviceInfo.prepareTrackingData(context, currentDeviceId, timestamp)

            val sdkId = sdkTracking.getSDKId() ?: ""
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

            val request = HttpUrlEncodedRequest(Constant.api.API_HARDWARE_ID_URL)
            request.addParameter("pl", "android")
            request.addParameter("appId", appId)
            request.addParameter("oauthCode", authCode)
            request.addParameter("device", deviceIdData.toString())
            request.addParameter("data", trackingData.toString())
            request.addParameter("ts", "" + timestamp)
            request.addParameter("sig", sig)
            request.addParameter("sdkId", sdkId)

            val jsonObject = httpClient.send(request).getJSON()

            val errorCode = jsonObject?.getInt("error")
            if (errorCode == 0) {
                return jsonObject.getJSONObject("data")
            }

        } catch (ex: JSONException) {
            Log.e("GetDeviceIdAsyncTask", ex)
        } catch (ex: Exception) {
            Log.e("GetDeviceIdAsyncTask", ex)
        }
        return null
    }

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)
        callback(result?.optString("deviceId") ?: "",
                result?.optLong("expiredTime") ?: 0)
    }
}
