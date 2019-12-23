package com.zing.zalo.zalosdk.core.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.annotation.Keep
import com.zing.zalo.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.core.Api.API_GET_SETTING
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.SharedPreferenceConstant
import com.zing.zalo.zalosdk.core.SharedPreferenceConstant.PREFS_NAME_WAKEUP
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.PrivateSharedPreferenceInterface
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.helper.Utils
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpGetRequest
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.module.BaseModule
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import com.zing.zalo.zalosdk.core.settings.SettingsManager.Companion.KEY_EXPIRE_TIME
import com.zing.zalo.zalosdk.core.settings.SettingsManager.Companion.KEY_SETTINGS_OUT_APP_LOGIN
import com.zing.zalo.zalosdk.core.settings.SettingsManager.Companion.KEY_SETTINGS_WEB_VIEW
import com.zing.zalo.zalosdk.core.settings.SettingsManager.Companion.KEY_WAKEUP_ENABLE
import com.zing.zalo.zalosdk.core.settings.SettingsManager.Companion.KEY_WAKEUP_INTERVAL
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
class SettingsManager : BaseModule() {

    @Keep
    companion object {
        private val instance = SettingsManager()
        fun getInstance() : SettingsManager { return instance }

        const val KEY_SETTINGS_WEB_VIEW =
            "com.zing.zalo.sdk.settings.useWebViewForUnloginZalo"
        const val KEY_SETTINGS_OUT_APP_LOGIN = "com.zing.zalo.sdk.settings.outapplogin"
        const val KEY_LAST_TIME_WAKEUP = "com.zing.zalo.sdk.wakeup.lastimewakeup"
        const val KEY_EXPIRE_TIME = "com.zing.zalo.sdk.wakeup.expiresetting"
        const val KEY_WAKEUP_INTERVAL = "com.zing.zalo.sdk.wakeup.wakeupsetting"
        const val KEY_WAKEUP_ENABLE = "com.zing.zalo.sdk.wakeup.wakeupenable"
    }


    var wakeUpStorage: PrivateSharedPreferenceInterface? = null
    var httpClient = HttpClient(ServiceMapManager.urlFor(ServiceMapManager.KEY_URL_CENTRALIZED))
    var deviceTracking = DeviceTracking.getInstance()

    override fun onStart(context: Context) {
        val deviceId = deviceTracking.getDeviceId() ?: ""

        if(wakeUpStorage == null) {
            wakeUpStorage = Storage(context).privateSharedPreferences(PREFS_NAME_WAKEUP)
        }

        if (isExpiredSetting()) {
            GetSDKSettingAsyncTask(context, deviceId, httpClient, wakeUpStorage!!).execute()
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

    fun isExpiredSetting(): Boolean {
        val expiredTime = wakeUpStorage?.getLong(KEY_EXPIRE_TIME) ?: 0
        return System.currentTimeMillis() > expiredTime
    }
}

class GetSDKSettingAsyncTask(
    context: Context, private val zdId: String,
    private val httpClient: HttpClient,
    private val storage: PrivateSharedPreferenceInterface
) :
    AsyncTask<Void, Void, String>() {

    companion object {
        const val WAKEUP_INTERVAL = "wakeup_interval"
        const val EXPIRED_TIME = "expiredTime"
        const val WAKEUP_INTERVAL_ENABLE = "wakeup_interval_enable"
        const val WEB_VIEW_LOGIN = "webview_login"
        const val IS_OUT_APP_LOGIN = "isOutAppLogin"
    }

    private val weakRefContext: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg p0: Void?): String? {
        val context = weakRefContext.get() ?: return null

        val request = HttpGetRequest(API_GET_SETTING)
        request.addQueryStringParameter("pl", "android")
        request.addQueryStringParameter("appId", AppInfo.getAppId(context))
        request.addQueryStringParameter("sdkv", Constant.VERSION)
        request.addQueryStringParameter("pkg", context.packageName)
        request.addQueryStringParameter("zdId", zdId)

        val response = httpClient.send(request)

        try {
            val jsonObject = response.getJSON() ?: return null
            val errorCode = jsonObject.getInt("error")

            if (errorCode != 0) throw Exception("ErrorCode != 0")

            val data = jsonObject.getJSONObject("data")
            storage.setBoolean(
                KEY_SETTINGS_OUT_APP_LOGIN,
                Utils.getBoolean(data, IS_OUT_APP_LOGIN) ?: false
            )
            storage.setBoolean(
                KEY_SETTINGS_WEB_VIEW,
                Utils.getBoolean(data, WEB_VIEW_LOGIN) ?: false
            )

            val settingData = data.getJSONObject("setting")
            storage.setBoolean(
                KEY_WAKEUP_ENABLE,
                Utils.getBoolean(settingData, WAKEUP_INTERVAL_ENABLE) ?: false
            )
            storage.setLong(KEY_WAKEUP_INTERVAL, settingData.optLong(WAKEUP_INTERVAL))
            storage.setLong(
                KEY_EXPIRE_TIME,
                System.currentTimeMillis() + settingData.optLong(EXPIRED_TIME)
            )

            return "success"
        } catch (ex: Exception) {
            Log.w("GetSDKSettingAsyncTask", ex)
        }
        return null
    }
}
