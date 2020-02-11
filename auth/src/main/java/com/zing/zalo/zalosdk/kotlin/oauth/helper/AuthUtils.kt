package com.zing.zalo.zalosdk.kotlin.oauth.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager
import com.zing.zalo.zalosdk.kotlin.oauth.BrowserLoginActivity

object AuthUtils {
    @SuppressLint("StaticFieldLeak")
    lateinit var settingsManager: SettingsManager

    internal fun canUseBrowserLogin(context: Context): Boolean {

        if (!AuthUtils::settingsManager.isInitialized)
            settingsManager = SettingsManager.getInstance()

        if (!settingsManager.isLoginViaBrowser()) return false

        val appId = AppInfo.getAppId(context)
        val pkgName = context.packageName
        if (TextUtils.isEmpty(appId)) return false

        val pkgMgr = context.packageManager
        val intent = Intent()
        intent.setPackage(pkgName)
        intent.data = Uri.parse("zalo-$appId://")
        val componentName = intent.resolveActivity(pkgMgr)
        if (componentName != null &&
            BrowserLoginActivity::class.java.name.equals(componentName.className, ignoreCase = true)
        ) {
            return true
        }

        Log.e("ZaloSDK support login via browser from version 2.4.0901")
        Log.e("Please add this activity to your AndroidManifest.xml")
        Log.e("  <activity android:name=\" " + BrowserLoginActivity::class.java.name + " \">")
        Log.e("    <intent-filter>")
        Log.e("      <action android:name=\"android.intent.action.VIEW\" />")
        Log.e("      <category android:name=\"android.intent.category.DEFAULT\" />")
        Log.e("      <category android:name=\"android.intent.category.BROWSABLE\" />")
        Log.e("      <data android:scheme=\"zalo-$appId\" />")
        Log.e("    </intent-filter>")
        Log.e("  </activity>")

        return false
    }
}