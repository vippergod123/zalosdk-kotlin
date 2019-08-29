package com.zing.zalo.zalosdk.core

import android.content.Context
import com.zing.zalo.zalosdk.core.helper.Storage

object SettingsManager
{
    private const val KEY_SETTINGS_WEB_VIEW = "com.zing.zalo.sdk.settings.useWebViewForUnloginZalo"
	
	fun isUseWebViewUnLoginZalo(context: Context): Boolean
	{
        return Storage(context).getBoolean(KEY_SETTINGS_WEB_VIEW)
	}
	
}