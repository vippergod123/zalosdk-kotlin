package com.zing.zalo.zalosdk.kotlin.core.apptracking

import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.SdkTracking
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.helper.*
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.http.HttpMultipartRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.module.BaseModule
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import org.json.JSONObject

class AppTracker : BaseModule(), IAppTracker {
    companion object {
        var packageNames = arrayListOf<String>()
        var installedPackagedNames = arrayListOf<String>()
    }

    var httpClient = HttpClient(ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_CENTRALIZED))
    var expiredTime = 0L
    var scanId = ""

    internal lateinit var appTrackerStorage: AppTrackerStorage
    internal lateinit var storage: Storage
    internal lateinit var sdkTracking: SdkTracking
    internal lateinit var deviceId: String
    private var submitRetry = 0
    override var listener: AppTrackerListener? = null

    override fun onStart(context: Context) {
        super.onStart(context)

        storage = Storage(context)
        appTrackerStorage = AppTrackerStorage(context)
        sdkTracking = SdkTracking.getInstance()
        deviceId = DeviceTracking.getInstance().getDeviceId() ?: ""

        if (!needToScanInstalledApp()) {
            Log.v("App Tracker run():", "not expired yet! ")
            cleanUp()
            return
        }

        ZTaskExecutor.queueRunnable(Runnable {
            Log.v("App Tracker run():", "start app tracker thread")
            try {
                if (downloadPackages() && scanInstalledApps())
                    submitInstalledApps()
            } catch (ex: Exception) {
                Log.e("App Tracker run():", ex)
                cleanUp()
            }
        })
    }

    internal fun needToScanInstalledApp(): Boolean {
        val expiredTime = appTrackerStorage.getInstallExpireTime()
        return System.currentTimeMillis() >= expiredTime
    }

    fun downloadPackages(): Boolean {
        try {
            val request = HttpGetRequest(Constant.api.API_TRACKING_URL)
            request.addQueryStringParameter("pl", "android")
            request.addQueryStringParameter("appId", AppInfo.getAppId(context!!))
            request.addQueryStringParameter("zdId", deviceId)
            request.addQueryStringParameter(
                "sdkId",
                sdkTracking.getSDKId() ?: ""
            )

            val response = httpClient.send(request)
            val jsonObject = response.getJSON()

            val error = jsonObject?.getInt("error")
            if (error != 0) throw Exception("Error when call api Download Packages")

            val data = jsonObject.getJSONObject("data")
            val apps = data.optJSONArray("apps")

            Log.d("downloadPackages", data.toString())
            packageNames = UtilsJSON.jsonArrayToArrayList(apps)

            expiredTime = data.optLong("expiredTime") + System.currentTimeMillis()
            scanId = data.optString("scanId", "")

            return true
        } catch (ex: Exception) {
            Log.w("downloadPackages", ex)
        }

        return false
    }

    fun scanInstalledApps(): Boolean {
        try {
            for (each in packageNames) {
                if (Utils.isPackageExisted(context!!, each)) installedPackagedNames.add(each)
            }

        } catch (ex: Exception) {
            Log.w("scanInstalledApps", ex)
        }

        Log.d("scanInstalledApps", "installed apps: ${installedPackagedNames.size}")
        return true
    }

    /**
     * if sdkId or privateKey empty -> runGetSdkIDAsyncTask -> submit app
     * if sdkId or privateKey empty -> runGetSdkIDAsyncTask -> submit app
    * */
    fun submitInstalledApps() {
        try {
            if (installedPackagedNames.size == 0 || TextUtils.isEmpty(scanId) || submitRetry >= 5) {
                Log.w("submitInstalledApps", "Submit fail more than maximum retries")
                cleanUp()
                return
            }

            val sdkId = sdkTracking.getSDKId() ?: ""
            val privateKey = sdkTracking.getPrivateKey() ?: ""

            if (TextUtils.isEmpty(sdkId) || TextUtils.isEmpty(privateKey)) {
                submitRetry += 1
                Log.d(
                    "submitInstalledApps",
                    "sdkId & privateKey empty -> waiting sdkId to submit"
                )
                submitInstalledApps()
            } else {
                Log.d(
                    "submitInstalledApps",
                    "ready to submit file to server"
                )
                ZTaskExecutor.queueRunnable(Runnable {
                    postRequestSubmitInstalledApps(sdkId, deviceId)
                    Log.v("App Tracker run():", "completed")
                    cleanUp()
                })
            }
        } catch (ex: Exception) {
            Log.w("submitInstalledApps", ex)
        }
    }

    //#region private supportive method
    private fun postRequestSubmitInstalledApps(
        sdkId: String,
        deviceId: String
    ) {
        val appData = UtilsJSON.listToJSONArray(installedPackagedNames)

        val multipartRequest = HttpMultipartRequest(Constant.api.API_APP_TRACKING_ID_URL)
        multipartRequest.addQueryStringParameter("et", "0")
        multipartRequest.addQueryStringParameter("sdkId", sdkId)
        multipartRequest.addQueryStringParameter("gzip", "0")

        val jsonData = JSONObject()
        jsonData.put("pl", "android")
        jsonData.put("appId", AppInfo.getAppId(context!!))
        jsonData.put("an", AppInfo.getAppName(context!!))
        jsonData.put("av", AppInfo.getVersionName(context!!))
        jsonData.put("oauthCode", storage.getOAuthCode())
        jsonData.put("osv", Build.VERSION.RELEASE)
        jsonData.put("sdkv", Constant.VERSION)
        jsonData.put("zdId", deviceId)
        jsonData.put("scanId", scanId)
        jsonData.put("apps", appData)

        val data = jsonData.toString()

        multipartRequest.setFileParameter("zce", "data.dat",data.toByteArray())
        val response = httpClient.send(multipartRequest)
        val responseData = response.getJSON()

        Log.v("submitInstalledApps", "submit app tracking to server with result $responseData")
        val error = responseData?.getInt("error")
        if (error == 0) {
            appTrackerStorage.setInstallExpireTime(expiredTime)
        }
    }

    private fun cleanUp() {
        if (listener != null) {
            listener?.onAppTrackerCompleted(
                installedPackagedNames.isNotEmpty(),
                scanId,
                packageNames,
                installedPackagedNames
            )
        }
    }
    //#endregion

}
