package com.zing.zalo.zalosdk.kotlin.core.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.zing.zalo.zalosdk.kotlin.core.SharedPreferenceConstant.PREF_OAUTH_CODE

open class Storage(val context: Context) {
    private var localPref: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun getString(key: String): String? {
        return localPref.getString(key, "")
    }

    fun setString(key: String, value: String) {
        val edit = localPref.edit()
        edit.putString(key, value)
        edit.apply()
    }

    fun getInt(key: String): Int {
        return localPref.getInt(key, 0)
    }

    fun setInt(key: String, value: Int) {
        val edit = localPref.edit()
        edit.putInt(key, value)
        edit.apply()
    }

    fun getLong(key: String): Long {
        return localPref.getLong(key, 0L)
    }

    fun setLong(key: String, value: Long) {
        val edit = localPref.edit()
        edit.putLong(key, value)
        edit.apply()
    }

    fun setBoolean(key: String, value: Boolean) {
        val editor = localPref.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(key: String): Boolean {
        return localPref.getBoolean(key, false)
    }

    fun getOAuthCode(): String? {
        return getString(PREF_OAUTH_CODE)
    }

    fun setAuthCode(code: String) {
        setString(PREF_OAUTH_CODE, code)
    }

    fun privateSharedPreferences(prefName: String): PrivateSharedPreferenceInterface {
        val prefEditor = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit()
        val sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)


        return object : PrivateSharedPreferenceInterface {
            override fun getString(key: String): String? {
                return sharedPref.getString(key, null)
            }

            override fun setString(key: String, value: String) {
                prefEditor.putString(key, value)
                prefEditor.apply()
            }

            override fun getInt(key: String): Int {
                return sharedPref.getInt(key, 0)
            }

            override fun setInt(key: String, value: Int) {
                prefEditor.putInt(key, value)
                prefEditor.apply()
            }

            override fun getLong(key: String): Long {
                return sharedPref.getLong(key, 0L)
            }

            override fun setLong(key: String, value: Long) {
                prefEditor.putLong(key, value)
                prefEditor.apply()
            }

            override fun getBoolean(key: String): Boolean {
                return sharedPref.getBoolean(key, false)
            }

            override fun setBoolean(key: String, value: Boolean) {
                prefEditor.putBoolean(key, value)
                prefEditor.apply()
            }
        }
    }
}

interface PrivateSharedPreferenceInterface {
    fun getString(key: String): String?

    fun setString(key: String, value: String)

    fun getInt(key: String): Int

    fun setInt(key: String, value: Int)

    fun getLong(key: String): Long

    fun setLong(key: String, value: Long)

    fun setBoolean(key: String, value: Boolean)

    fun getBoolean(key: String): Boolean
}