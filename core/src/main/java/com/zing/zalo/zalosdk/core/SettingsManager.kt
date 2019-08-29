package com.zing.zalo.zalosdk.core

import android.content.Context
import android.os.AsyncTask
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.Storage
import com.zing.zalo.zalosdk.core.http.HttpClient
import com.zing.zalo.zalosdk.core.http.HttpClientRequest
import com.zing.zalo.zalosdk.core.http.HttpMethod
import com.zing.zalo.zalosdk.core.log.Log
import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
import org.json.JSONObject
import java.lang.ref.WeakReference

object SettingsManager {
    private const val KEY_SETTINGS_WEB_VIEW = "com.zing.zalo.sdk.settings.useWebViewForUnloginZalo"
    private const val KEY_LAST_TIME_WAKEUP = "com.zing.zalo.sdk.wakeup.lastimewakeup"
    private const val KEY_EXPIRE_SETTING = "com.zing.zalo.sdk.wakeup.expiresetting"
    private const val KEY_WAKEUP_SETTING = "com.zing.zalo.sdk.wakeup.wakeupsetting"
    private const val KEY_WAKEUP_ENABLE = "com.zing.zalo.sdk.wakeup.wakeupenable"


    fun isUseWebViewUnLoginZalo(context: Context): Boolean {
        return Storage(context).getBoolean(KEY_SETTINGS_WEB_VIEW)
    }

    fun setting(context: Context): SettingsManagerStorageInterface {
        val storage = Storage(context)
        return object : SettingsManagerStorageInterface {
            override fun setLastTimeWakeUp(value: Long) {
                storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).setLong(
                    KEY_LAST_TIME_WAKEUP, value
                )
            }

            override fun getLastTimeWakeup(): Long {
                return storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).getLong(
                    KEY_LAST_TIME_WAKEUP
                )
            }

            override fun setExpiredSetting(value: Long) {
                storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).setLong(
                    KEY_EXPIRE_SETTING, value
                )
            }

            override fun getExpiredSetting(): Long {
                return storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).getLong(
                    KEY_EXPIRE_SETTING
                )
            }

            override fun setWakeUpInterval(value: Long) {
                storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).setLong(
                    KEY_WAKEUP_SETTING, value
                )
            }

            override fun getWakeUpInterval(): Long {
                return storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).getLong(
                    KEY_WAKEUP_SETTING
                )
            }

            override fun setWakeUpSetting(isEnable: Boolean) {
                storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).setBoolean(
                    KEY_WAKEUP_ENABLE, isEnable
                )
            }

            override fun getWakeupSetting(): Boolean {
                return storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).getBoolean(
                    KEY_WAKEUP_ENABLE
                )
            }

            override fun isExpiredSetting(): Boolean {
                val expiredTime =
                    storage.privateSharedPreferences(MySharedPreference.PREFS_NAME_WAKEUP).getLong(
                        KEY_EXPIRE_SETTING
                    )
                return System.currentTimeMillis() > expiredTime
            }
        }
    }
}

class GetSDKSettingAsyncTask(context: Context, private val zdId: String) : AsyncTask<Void, Void, String>() {

    private val weakRefContext: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(vararg p0: Void?): String? {
        val context = weakRefContext.get() ?: return null

        val apiUrl = ServiceMapManager.urlFor(
            ServiceMapManager.KEY_URL_CENTRALIZED,
            Api.API_GET_SETTING_URL
        )
        val request = HttpClientRequest(HttpMethod.GET, apiUrl)
        request.addQueryStringParameter("pl", "android")
        request.addQueryStringParameter("appId", AppInfo.getAppId(context))

        //Todo: change Constant Version to  method
//        request.addQueryStringParameter("sdkv", DeviceTracking.getInstance().getVersion())
        request.addQueryStringParameter("sdkv", Constant.VERSION)
        request.addQueryStringParameter("pkg", context.packageName)
        request.addQueryStringParameter("zdId", zdId)

        val client = HttpClient()
        val response = client.send(request)
        if (response.getStatusCode() != 200) return null

        try {
            val result = response.getText() ?: return null
            val jsonObject = JSONObject(result)
            val errorCode = jsonObject.getInt("error")
            if (errorCode == 0) {
                val data = jsonObject.getJSONObject("data") ?: return null

                val settingData = data.getJSONObject("setting") ?: return null
                if (settingData.has("wakeup_interval")) {

                    val wakeupInterval = settingData.getLong("wakeup_interval")
                    SettingsManager.setting(context).setWakeUpInterval(wakeupInterval)

                    if (settingData.has("expiredTime")) {
                        val expiredTime = settingData.getLong("expiredTime")
                        SettingsManager.setting(context).setExpiredSetting(expiredTime + System.currentTimeMillis())
                    }

                    if (settingData.has("wakeup_interval_enable")) {
                        val wakeupIntervalEnable = settingData.getBoolean("wakeup_interval_enable")
                        SettingsManager.setting(context).setWakeUpSetting(wakeupIntervalEnable)//default will be False
                    }

                    return "success"
                } else {
                    Log.d("Loi muon doi` xac dinh duoc")
                }
            } else {
                Log.d("Server fail lo`i ")
            }
        } catch (ex: Exception) {
            Log.d(ex.toString())
        }
        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if (result == null)
            Log.d("Null r a oi")
        else
            Log.d("Hen qua khong null")
    }
}

interface SettingsManagerStorageInterface {
    fun setLastTimeWakeUp(value: Long)
    fun getLastTimeWakeup(): Long

    fun setExpiredSetting(value: Long)
    fun getExpiredSetting(): Long

    fun setWakeUpInterval(value: Long)
    fun getWakeUpInterval(): Long

    fun setWakeUpSetting(isEnable: Boolean)
    fun getWakeupSetting(): Boolean

    fun isExpiredSetting(): Boolean
}