package com.zing.zalo.devicetrackingsdk

import android.annotation.SuppressLint
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
import com.zing.zalo.zalosdk.core.module.BaseModule
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
class SdkTracking private constructor(): BaseModule(), ISdkTracking {
    internal var storage: Storage? = null
    private val loading = AtomicBoolean(false)
    private var sdkId: String? = null
    private var privateKey: String? = null

    var httpClient = HttpClient(
        ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED
        )
    )

    companion object {
        private val instance = SdkTracking()
        fun getInstance() : SdkTracking { return instance }
    }

    override fun onStart(context: Context) {
        super.onStart(context)

        if(storage == null) {
            storage = Storage(context)
        }

        sdkId = storage?.getString(Constant.sharedPreference.PREF_SDK_ID)
        privateKey = storage?.getString(Constant.sharedPreference.PREF_PRIVATE_KEY)
        if(TextUtils.isEmpty(sdkId) ||
            TextUtils.isEmpty(privateKey)) {
            runGetSdkIDAsyncTask()
        }
    }

    override fun onStop() {
        super.onStop()
        storage = null
        sdkId = null
        privateKey = null
    }


    override fun getSDKId(): String? {
        if(TextUtils.isEmpty(sdkId)) {
            runGetSdkIDAsyncTask()
        }
        return sdkId
    }

    override fun getPrivateKey(): String? {
        if(TextUtils.isEmpty(privateKey)) {
            runGetSdkIDAsyncTask()
        }
        return privateKey
    }

    private fun runGetSdkIDAsyncTask() {
        if(loading.get() || !hasContext) {
            return
        }

        loading.set(true)
        val task = GetSdkIdAsyncTask(context!!, httpClient) {
            sdkId, privateKey ->

            if(sdkId != null && privateKey != null) {
                this.sdkId = sdkId
                this.privateKey = privateKey
                storage?.setString(Constant.sharedPreference.PREF_PRIVATE_KEY, privateKey)
                storage?.setString(Constant.sharedPreference.PREF_SDK_ID, sdkId)
            }

            loading.set(false)
        }
        task.execute()
    }
}

private typealias GetSdkIdAsyncTaskCallback = (String?, String?) -> Unit
@SuppressLint("StaticFieldLeak")
class GetSdkIdAsyncTask(
    private val context: Context,
    private val httpClient: HttpClient,
    private var callback: GetSdkIdAsyncTaskCallback
) : AsyncTask<Void, Void, JSONObject>() {

    override fun doInBackground(vararg params: Void?): JSONObject? {
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
                return jsonObject.getJSONObject("data")
            }
        } catch (ex: JSONException) {
            Log.e("GetSdkId", ex)
        } catch (ex: Exception) {
            Log.e("GetSdkId", ex)
        }
        return null
    }

    override fun onPostExecute(result: JSONObject?) {
        super.onPostExecute(result)

        callback(result?.optString("sdkId"),
            result?.optString("privateKey"))
    }
}