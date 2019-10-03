package com.zing.zalo.zalosdk.core.apptracking

import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.devicetrackingsdk.DeviceTrackingListener
import com.zing.zalo.devicetrackingsdk.SdkTracking
import com.zing.zalo.devicetrackingsdk.SdkTrackingListener
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.*
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.core.http.HttpMultipartRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONObject

class AppTracker(private val context: Context) : IAppTracker {
    companion object {
        var packageNames = arrayListOf<String>()
        var installedPackagedNames = arrayListOf<String>()
    }

    var httpClient = HttpClient(ServiceMapManager.urlFor(ServiceMapManager.KEY_URL_CENTRALIZED))

    var expiredTime = 0L
    var scanId = ""

    internal var appTrackerStorage = AppTrackerStorage(context)
    internal var storage = Storage(context)
    internal var sdkTracking = SdkTracking(context)

    private var submitRetry = 0
    private var listener: AppTrackerListener? = null

    override fun run() {
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

    override fun setListener(listener: AppTrackerListener) {
        this.listener = listener
    }

    internal fun needToScanInstalledApp(): Boolean {
        val expiredTime = appTrackerStorage.getInstallExpireTime()
        return System.currentTimeMillis() >= expiredTime
    }

    fun downloadPackages(): Boolean {
        try {
            val request = HttpGetRequest(Constant.api.API_TRACKING_URL)
            request.addQueryStringParameter("pl", "android")
            request.addQueryStringParameter("appId", AppInfo.getAppId(context))
            request.addQueryStringParameter(
                "zdId",
                DeviceTracking.getDeviceId() ?: ""
            )
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
                if (Utils.isPackageExisted(context, each)) installedPackagedNames.add(each)
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

            val deviceId = DeviceTracking.getDeviceId() ?: ""
            val sdkId = sdkTracking.getSDKId() ?: ""
            val privateKey = sdkTracking.getPrivateKey() ?: ""

            if (TextUtils.isEmpty(sdkId) || TextUtils.isEmpty(privateKey)) {
                submitRetry += 1
                Log.d(
                    "submitInstalledApps",
                    "sdkId & privateKey empty -> waiting sdkId to submit"
                )
                sdkTracking.getSDKId(object : SdkTrackingListener {
                    override fun onComplete(result: String?) {
                        submitInstalledApps()
                    }
                })
            } else if (TextUtils.isEmpty(deviceId)) {
                submitRetry += 1
                Log.d("submitInstalledApps", "deviceID empty -> waiting deviceId to submit")
                DeviceTracking.getDeviceId(object : DeviceTrackingListener {
                    override fun onComplete(result: String?) {
                        Log.d(
                            "submitInstalledApps",
                            "deviceID not empty -> submit install app: $result"
                        )
                        submitInstalledApps()
                    }
                })
            } else {
                Log.d(
                    "submitInstalledApps",
                    "ready to submit file to server"
                )
                ZTaskExecutor.queueRunnable(Runnable {
                    postRequestSubmitInstalledApps(sdkId, deviceId, privateKey)
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
        deviceId: String,
        privateKey: String
    ) {
        val appData = UtilsJSON.listToJSONArray(installedPackagedNames)

        val multipartRequest = HttpMultipartRequest(Constant.api.API_APP_TRACKING_ID_URL)
        multipartRequest.addQueryStringParameter("et", "1")
        multipartRequest.addQueryStringParameter("sdkId", sdkId)
        multipartRequest.addQueryStringParameter("gzip", "0")

        val jsonData = JSONObject()
        jsonData.put("pl", "android")
        jsonData.put("appId", AppInfo.getAppId(context))
        jsonData.put("an", AppInfo.getAppName(context))
        jsonData.put("av", AppInfo.getVersionName(context))
        jsonData.put("oauthCode", storage.getOAuthCode())
        jsonData.put("osv", Build.VERSION.RELEASE)
        jsonData.put("sdkv", Constant.VERSION)
        jsonData.put("zdId", deviceId)
        jsonData.put("scanId", scanId)
        jsonData.put("apps", appData)

        val data = jsonData.toString()
        val encodeData = Utils.encrypt(privateKey, data)

        val responseData =
            Utils.postFile(httpClient, multipartRequest, "data.dat", "zce", encodeData, null)

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
