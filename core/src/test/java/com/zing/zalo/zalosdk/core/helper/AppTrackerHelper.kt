package com.zing.zalo.zalosdk.core.helper

import android.os.Build
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.apptracking.AppTracker
import org.json.JSONArray
import org.json.JSONObject

object AppTrackerHelper {
    const val deviceId = "2002.1ecf16c74adba385faca.1568970832304.b92c8f7c"
    const val sdkId = "nRHRPtwUxNE8smukCyQjIBdU0rvbeza6wArCKcUZwaAxrJTBMv_KSudR0d9qaj8wzROn0Ypvot2p_M9nNfFj8TQ1IquUng07iF4kAocXobMZi6LOPlkDBUVvJsvoxAT5gBLzIoF0anMnu6fvGihxBlcgAZaD-lPIqqpZM4XgTKyzHFj2isfg5IaRl5NsBaDlNLICOS0XC3yK7i87ot04SpLqm46TMa8FARmzBq-GoZ0"
    const val privateKey = "TTie3CTwKY0UxInP"

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