package com.zing.zalo.zalosdk.kotlin.core.apptracking

import android.content.Context
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage

class AppTrackerStorage(context: Context): Storage(context) {

    companion object {
        private const val PREF_TRACKING_APP_INSTALL_EXP_TIME = "PREFERENCE_TRACKING_APP_INSTALL_EXP_TIME"
    }

    fun setInstallExpireTime(expiredTime: Long) {
        setLong(PREF_TRACKING_APP_INSTALL_EXP_TIME, expiredTime)
    }

    fun getInstallExpireTime():Long {
        return getLong(PREF_TRACKING_APP_INSTALL_EXP_TIME)
    }
}