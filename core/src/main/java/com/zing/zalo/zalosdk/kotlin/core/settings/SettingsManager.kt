package com.zing.zalo.zalosdk.kotlin.core.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.Keep
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.Api.API_GET_SETTING
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.SharedPreferenceConstant.PREFS_NAME_WAKEUP
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.helper.PrivateSharedPreferenceInterface
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.helper.Utils
import com.zing.zalo.zalosdk.kotlin.core.http.HttpClient
import com.zing.zalo.zalosdk.kotlin.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.module.BaseModule
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

@SuppressLint("StaticFieldLeak")
class SettingsManager : BaseModule() {

    @Keep
    companion object {
        private val instance = SettingsManager()
        fun getInstance(): SettingsManager {
            return instance
        }

        const val KEY_SETTINGS_WEB_VIEW =
            "com.zing.zalo.sdk.settings.useWebViewForUnloginZalo"
        const val KEY_SETTINGS_OUT_APP_LOGIN = "com.zing.zalo.sdk.settings.outapplogin"
        const val KEY_LAST_TIME_WAKEUP = "com.zing.zalo.sdk.wakeup.lastimewakeup"
        const val KEY_EXPIRE_TIME = "com.zing.zalo.sdk.wakeup.expiresetting"
        const val KEY_WAKEUP_INTERVAL = "com.zing.zalo.sdk.wakeup.wakeupsetting"
        const val KEY_WAKEUP_ENABLE = "com.zing.zalo.sdk.wakeup.wakeupenable"

        private const val WAKEUP_INTERVAL = "wakeup_interval"
        private const val EXPIRED_TIME = "expiredTime"
        private const val WAKEUP_INTERVAL_ENABLE = "wakeup_interval_enable"
        private const val WEB_VIEW_LOGIN = "webview_login"
        private const val IS_OUT_APP_LOGIN = "isOutAppLogin"
    }

    private var job = Job()
    var scope = CoroutineScope(job + Dispatchers.IO)

    var wakeUpStorage: PrivateSharedPreferenceInterface? = null
    var httpClient =
        HttpClient(ServiceMapManager.getInstance().urlFor(ServiceMapManager.KEY_URL_CENTRALIZED))
    var deviceTracking = DeviceTracking.getInstance()

    override fun onStart(context: Context) {
        val deviceId = deviceTracking.getDeviceId() ?: ""

        if (wakeUpStorage == null) {
            wakeUpStorage = Storage(context).privateSharedPreferences(PREFS_NAME_WAKEUP)
        }

        if (isExpiredSetting()) {
            makeGetSDKSettingRequest(deviceId)
        }
    }

    override fun onStop() {
        super.onStop()
        wakeUpStorage = null
    }

    fun getExpiredTime(): Long {
        return wakeUpStorage?.getLong(KEY_EXPIRE_TIME) ?: 0
    }

    fun getWakeUpInterval(): Long {
        return wakeUpStorage?.getLong(KEY_WAKEUP_INTERVAL) ?: 0
    }

    fun getWakeUpSetting(): Boolean {
        return wakeUpStorage?.getBoolean(KEY_WAKEUP_ENABLE) ?: false
    }

    fun isUseWebViewLoginZalo(): Boolean {
        return wakeUpStorage?.getBoolean(KEY_SETTINGS_WEB_VIEW) ?: false
    }

    fun isLoginViaBrowser(): Boolean {
        return wakeUpStorage?.getBoolean(KEY_SETTINGS_OUT_APP_LOGIN) ?: false
    }

    private fun isExpiredSetting(): Boolean {
        val expiredTime = wakeUpStorage?.getLong(KEY_EXPIRE_TIME) ?: 0
        return System.currentTimeMillis() > expiredTime
    }

    private fun makeGetSDKSettingRequest(
        zdId: String
    ) {
        scope.launch {
            try {
                val ctx = context ?: throw Exception("Context is empty")

                val request = HttpGetRequest(API_GET_SETTING)
                request.addQueryStringParameter("pl", "android")
                request.addQueryStringParameter("appId", AppInfo.getAppId(ctx))
                request.addQueryStringParameter("sdkv", Constant.VERSION)
                request.addQueryStringParameter("pkg", ctx.packageName)
                request.addQueryStringParameter("zdId", zdId)

                val jsonObject = httpClient.send(request).getJSON() ?: return@launch
                val errorCode = jsonObject.getInt("error")

                if (errorCode != 0) throw Exception("ErrorCode != 0")

                val jsonData = jsonObject.optJSONObject("data")
                saveSettingDataToStorage(jsonData)

                return@launch
            } catch (ex: Exception) {
                Log.w("makeGetSDKSettingRequest", ex)
            }
            return@launch
        }
    }

    private fun saveSettingDataToStorage(data: JSONObject) {
        wakeUpStorage?.setBoolean(
            KEY_SETTINGS_OUT_APP_LOGIN,
            Utils.getBoolean(data, IS_OUT_APP_LOGIN) ?: false
        )
        wakeUpStorage?.setBoolean(
            KEY_SETTINGS_WEB_VIEW,
            Utils.getBoolean(data, WEB_VIEW_LOGIN) ?: false
        )

        val settingData = data.getJSONObject("setting")
        wakeUpStorage?.setBoolean(
            KEY_WAKEUP_ENABLE,
            Utils.getBoolean(settingData, WAKEUP_INTERVAL_ENABLE)
                ?: false
        )
        wakeUpStorage?.setLong(
            KEY_WAKEUP_INTERVAL,
            settingData.optLong(WAKEUP_INTERVAL)
        )
        wakeUpStorage?.setLong(
            KEY_EXPIRE_TIME,
            System.currentTimeMillis() + settingData.optLong(EXPIRED_TIME)
        )
    }
}
