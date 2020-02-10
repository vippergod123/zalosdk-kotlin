package com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk

import android.content.Context
import android.text.TextUtils
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpUrlEncodedRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.module.BaseModule
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class DeviceTracking private constructor() : BaseModule(), IDeviceTracking {
    @Keep
    companion object {
        private val instance = DeviceTracking()

        fun getInstance(): DeviceTracking {
            return instance
        }

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
        ServiceMapManager.getInstance().urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        )
    )

    private val job: Job = Job()
    var scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStart(context: Context) {
        loadDeviceIdSetting()

        if (isDeviceIdValid()) {
            return
        }

        runGetDeviceIdCoroutines()
    }

    override fun onStop() {
        deviceIdExpiredTime = 0
        deviceId = null
    }

    @Deprecated("")
    override fun initDeviceTracking() {
    }


    override fun getDeviceId(): String? {
        if (TextUtils.isEmpty(deviceId) || !isDeviceIdValid()) {
            runGetDeviceIdCoroutines()
        }

        return deviceId
    }

    override fun getDeviceId(listener: DeviceTrackingListener?) {
        if (deviceId != null) {
            listener?.onComplete(deviceId!!)
            if (isDeviceIdValid()) {
                return
            }
        } else {
            listener?.let { listeners.add(it) }
        }
        runGetDeviceIdCoroutines()
    }

    override fun getVersion(): String {
        return Constant.VERSION
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

    private fun runGetDeviceIdCoroutines() {
        if (loading.get() || !hasContext) return

        loading.set(true)
        val currentMillis = System.currentTimeMillis()
        callDeviceIdRequest(
            context!!,
            sdkTracking,
            httpClient,
            deviceId,
            currentMillis
        ) { dId, expireTime ->
            if (dId != null) {
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

    }

    private fun callDeviceIdRequest(
        context: Context,
        sdkTracking: SdkTracking,
        httpClient: HttpClient,
        currentDeviceId: String?,
        timestamp: Long,
        callback: (String?, Long) -> Unit
    ) {
        scope.launch {
            try {
                val storage = Storage(context)

                val deviceIdData = DeviceInfo.prepareDeviceIdData(context)
                val trackingData =
                    DeviceInfo.prepareTrackingData(context, currentDeviceId, timestamp)

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
                    val data = jsonObject.getJSONObject("data")
                    val dId = data?.optString("deviceId", "")
                    val expTime = data?.optLong("expiredTime")
                    callback(dId, expTime ?: 0)
                    return@launch
                }
            } catch (ex: JSONException) {
                Log.e("callDeviceIdRequest", ex)
            } catch (ex: Exception) {
                Log.e("callDeviceIdRequest", ex)
            }
            callback(null, 0)
        }
    }
    //#endregion
}