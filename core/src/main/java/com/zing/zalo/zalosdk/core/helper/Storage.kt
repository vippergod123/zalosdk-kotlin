package com.zing.zalo.zalosdk.core.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.zing.zalo.zalosdk.core.SharedPreference.PREF_OAUTH_CODE

open class Storage(val context: Context)
{
	private var localPref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

	fun getString(key: String): String?
	{
		return localPref.getString(key, "")
	}
	
	fun setString(key: String, value: String)
	{
		val edit = localPref.edit()
		edit.putString(key, value)
		edit.apply()
	}
	
	internal fun getInt(key: String): Int?
	{
		return localPref.getInt(key, 0)
	}
	
	internal fun setInt(key: String, value: Int)
	{
		val edit = localPref.edit()
		edit.putInt(key, value)
		edit.apply()
	}
	
	fun getLong(key: String): Long?
	{
		return localPref.getLong(key, 0)
	}
	
	fun setLong(key: String, value: Long)
	{
		val edit = localPref.edit()
		edit.putLong(key, value)
		edit.apply()
	}
	
	fun setBoolean(key: String, value: Boolean)
	{
		val editor = localPref.edit()
		editor.putBoolean(key, value)
		editor.apply()
	}
	
	fun getBoolean(key: String): Boolean
	{
		return localPref.getBoolean(key, false)
	}

    fun getOAuthCode(): String? {
        return getString(PREF_OAUTH_CODE)
    }

    fun setAuthCode(code: String) {
        setString(PREF_OAUTH_CODE, code)
    }
}