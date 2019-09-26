package com.zing.zalo.devicetrackingsdk

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.log.Log
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object DeviceTracking : IDeviceTracking {


    private var lock = Any()

    private const val KEY_DEVICE_ID = "deviceId"
    private const val KEY_DEVICE_ID_EXPIRED_TIME = "expiredTime"
    private const val DID_FILE_NAME = "ddinfo2"

    private lateinit var context: Context

    private var deviceId: String = ""
    private var deviceIdExpiredTime: Long = 0L
    private var deviceTrackingStorage: Storage? = null


    lateinit var getDeviceIdAsyncTask: DeviceTrackingAsyncTask.GetDeviceId
    lateinit var getSdkIdAsyncTask: DeviceTrackingAsyncTask.GetSdkId

    fun init(context: Context, listener: DeviceTrackingListener?) {
        this.context = context.applicationContext

        deviceTrackingStorage = Storage(context)

        loadDeviceIdSetting()
        runGetInfoDeviceTask(listener)
    }


    override fun setSDKId(value: String) {
        deviceTrackingStorage?.setString(Constant.sharedPreference.PREF_SDK_ID, value)
    }

    override fun getSDKId(): String? {
        return deviceTrackingStorage?.getString(Constant.sharedPreference.PREF_SDK_ID)
    }

    override fun getSDKId(listener: DeviceTrackingListener?) {
        val sdkId = getSDKId()

        if (!TextUtils.isEmpty(sdkId)) {
            listener?.onComplete(sdkId)
            return
        }

        runGetSdkIDAsyncTask(listener)
    }

    override fun getPrivateKey(): String? {
        return deviceTrackingStorage?.getString(Constant.sharedPreference.PREF_PRIVATE_KEY)
    }


    override fun setPrivateKey(value: String) {
        deviceTrackingStorage?.setString(Constant.sharedPreference.PREF_PRIVATE_KEY, value)
    }


    override fun getDeviceId(): String? {
        if (!TextUtils.isEmpty(deviceId)) return deviceId

        synchronized(lock) { loadDeviceIdSetting() }

        return deviceId
    }

    override fun getDeviceId(listener: DeviceTrackingListener?) {

        if (!TextUtils.isEmpty(getDeviceId())) {
            listener?.onComplete(getDeviceId())
            return
        }

        runGetInfoDeviceTask(listener)
    }


    override fun setDeviceId(deviceId: String, expiredTime: String) {
        checkContextIsInitialized()

        val data = JSONObject()
        data.put(KEY_DEVICE_ID, deviceId)
        data.put(KEY_DEVICE_ID_EXPIRED_TIME, expiredTime)
        Utils.writeToFile(context, data.toString(), DID_FILE_NAME)

    }

    fun isDeviceIdExpired(): Boolean {
        if (deviceIdExpiredTime == 0L)
            loadDeviceIdSetting()
        return System.currentTimeMillis() > deviceIdExpiredTime
    }


    //#region private supportive method

    private fun loadDeviceIdSetting() {
        checkContextIsInitialized()

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
    private fun checkContextIsInitialized() {
        if (!::context.isInitialized) throw Exception("Device Tracking must be init first")
    }

    /** method run async task to get SDK ID & deviceID
     * if SDK empty -> run SdkId Task -> run DeviceId Task
     * if SDK not empty -> run DeviceId Task
     * @return deviceId save in File ddinfo
     * @return SDKId save in SharePref
     * */
    private fun runGetInfoDeviceTask(listener: DeviceTrackingListener?) {

        if (TextUtils.isEmpty(getSDKId())) {
            val sdkIdListener = object : DeviceTrackingListener {
                override fun onComplete(result: String?) {
                    super.onComplete(result)
                    if (isDeviceIdExpired())
                        listener?.let { runGetDeviceIdAsyncTask(it) }
                }
            }
            runGetSdkIDAsyncTask(sdkIdListener)
            return
        }
        listener?.let { runGetDeviceIdAsyncTask(it) }
    }

    private fun runGetDeviceIdAsyncTask(listener: DeviceTrackingListener) {
        val currentMillis = System.currentTimeMillis()
        getDeviceIdAsyncTask = DeviceTrackingAsyncTask.GetDeviceId(
            WeakReference(context), deviceId, currentMillis, listener
        )
        getDeviceIdAsyncTask.execute()
    }

    private fun runGetSdkIDAsyncTask(listener: DeviceTrackingListener?) {
        getSdkIdAsyncTask = DeviceTrackingAsyncTask.GetSdkId(
            WeakReference(context), listener
        )
        getSdkIdAsyncTask.execute()
    }

    //#endregion

}
