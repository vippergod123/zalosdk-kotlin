package com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk

import android.content.Context
import android.text.TextUtils
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
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
import java.util.concurrent.atomic.AtomicBoolean

class SdkTracking private constructor() : BaseModule(), ISdkTracking {
    internal var storage: Storage? = null
    private val loading = AtomicBoolean(false)
    private var sdkId: String? = null
    private var privateKey: String? = null

    var httpClient = HttpClient(
        ServiceMapManager.getInstance().urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        )
    )

    companion object {
        private val instance = SdkTracking()
        fun getInstance(): SdkTracking {
            return instance
        }
    }

    private var job = Job()
    var scope = CoroutineScope(Dispatchers.IO + job)

    override fun onStart(context: Context) {
        super.onStart(context)

        if (storage == null) {
            storage = Storage(context)
        }

        sdkId = storage?.getString(Constant.sharedPreference.PREF_SDK_ID)
        privateKey = storage?.getString(Constant.sharedPreference.PREF_PRIVATE_KEY)
        if (TextUtils.isEmpty(sdkId) ||
            TextUtils.isEmpty(privateKey)
        ) {
            runGetSdkIDTask()
        }
    }

    override fun onStop() {
        super.onStop()
        storage = null
        sdkId = null
        privateKey = null
    }


    override fun getSDKId(): String? {
        if (TextUtils.isEmpty(sdkId)) {
            runGetSdkIDTask()
        }
        return sdkId
    }

    override fun getPrivateKey(): String? {
        if (TextUtils.isEmpty(privateKey)) {
            runGetSdkIDTask()
        }
        return privateKey
    }

    private fun runGetSdkIDTask() {
        if (loading.get() || !hasContext) {
            return
        }

        loading.set(true)
        callSdkIdRequest(context!!, httpClient) { sdkId, privateKey ->

            if (sdkId != null && privateKey != null) {
                this.sdkId = sdkId
                this.privateKey = privateKey
                storage?.setString(Constant.sharedPreference.PREF_PRIVATE_KEY, privateKey)
                storage?.setString(Constant.sharedPreference.PREF_SDK_ID, sdkId)
            }

            loading.set(false)
        }
    }

    private fun callSdkIdRequest(
        context: Context,
        httpClient: HttpClient,
        callback: (String?, String?) -> Unit
    ) {
        scope.launch {
            try {
                val deviceIdData = DeviceInfo.prepareDeviceIdData(context).toString()
                val request = HttpUrlEncodedRequest(Constant.api.API_SDK_ID)
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
                    val sId = data?.optString("sdkId")
                    val pKey = data?.optString("privateKey")
                    callback(sId, pKey)
                    return@launch
                }
            } catch (ex: JSONException) {
                Log.e("GetSdkId", ex)
            } catch (ex: Exception) {
                Log.e("GetSdkId", ex)
            }
            callback(null, null)
            return@launch
        }
    }
}
