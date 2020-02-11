package com.zing.zalo.zalosdk.kotlin.core.helper

import android.os.Build
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.apptracking.AppTracker
import org.json.JSONObject

object AppTrackerHelper {
    const val deviceId = "device_id_test"
    const val sdkId = "sdk_id_test"
    const val privateKey = "private_key_test"

    fun prepareDataForSubmitInstalledApp(appTracker: AppTracker, authCode: String):JSONObject {
        val appData = UtilsJSON.listToJSONArray(AppTracker.installedPackagedNames)
        val jsonData = JSONObject()
        jsonData.put("pl", "android")
        jsonData.put("appId", AppInfoHelper.appId)
        jsonData.put("an", AppInfoHelper.appName)
        jsonData.put("av", AppInfoHelper.versionName)
        jsonData.put("oauthCode", authCode)
        jsonData.put("osv", Build.VERSION.RELEASE)
        jsonData.put("sdkv", Constant.VERSION)
        jsonData.put("zdId", deviceId)
        jsonData.put("scanId", appTracker.scanId)
        jsonData.put("apps", appData)

        return jsonData
    }


}