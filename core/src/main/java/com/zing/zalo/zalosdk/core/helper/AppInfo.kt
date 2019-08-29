package com.zing.zalo.zalosdk.core.helper

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import java.net.URLEncoder
import java.security.MessageDigest

object AppInfo
{
    private val lock = Any()
    var extracted: Boolean = false
    private var appId: String? = null
    var versionName: String? = null
    var versionCode: Long = 0
    var packageName: String? = null
    var appName: String? = null
    private var applicationHashKey: String? = null
    var firstInstallDate: String? = null
    var installDate: String? = null
    var lastUpdateDate: String? = null
    var installerPackageName: String? = null
    var preloadChannel: String? = null
    private var isAutoTrackingOpenApp: Boolean = false


    fun isPackageExists(mContext: Context, targetPackage: String): Boolean {
        val pm: PackageManager = mContext.packageManager
        try {
            pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA)
        } catch (ex: Exception) {
            return false
        }
        return true
    }


    fun getAppId(ctx: Context): String {
        synchronized(lock) {
            if (extracted)
                return appId.toString()
        }

        extractBasicAppInfo(ctx)
        return appId!!
    }

    fun getAppIdLong(ctx: Context): Long {
        val str = getAppId(ctx)
        return try {
            java.lang.Long.parseLong(str)
        } catch (ex: Exception) {
            Log.w("getAppIdLong", ex.toString())
            return 0L
        }

    }

    fun getPackageName(ctx: Context): String {
        synchronized(lock) {
            if (extracted) return packageName!!
        }

        extractBasicAppInfo(ctx)
        return packageName!!
    }

    fun getApplicationHashKey(ctx: Context): String? {
        if (applicationHashKey != null) return applicationHashKey

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signInfo =
                    ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                        .signingInfo

                if (signInfo.hasMultipleSigners()) {
                    signInfo.apkContentsSigners.map { signature ->
                        encodeSignature(signature)
                    }
                } else {
                    signInfo.signingCertificateHistory.map { signature ->
                        encodeSignature(signature)
                    }
                }
            } else {
                val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
                info.signatures.map { signature ->
                    encodeSignature(signature)
                }
            }

        } catch (e: Exception) {
            com.zing.zalo.zalosdk.core.log.Log.e(e)
        }

        return applicationHashKey
    }

    fun getReferrer(context: Context): String {
        return try {
            context.getSharedPreferences("zacCookie", 0).getString("referrer", "") ?: ""
        } catch (ex: Exception) {
            ""
        }

    }

    //#region private method
    private fun encodeSignature(signature: Signature) {
        val md = MessageDigest.getInstance("SHA")
        md.update(signature.toByteArray())
        applicationHashKey = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim { it <= ' ' }
    }


    private fun getBoolean(bundle: Bundle, key: String, def: Boolean): Boolean {
        return if (bundle.containsKey(key)) {
            bundle.getBoolean(key)
        } else {
            return def
        }
    }

    private fun getString(bundle: Bundle, key: String, def: String?): String? {
        return if (bundle.containsKey(key)) {
            bundle.getString(key)
        } else {
            return def
        }
    }

    private fun extractBasicAppInfo(ctx: Context) {
        synchronized(lock) {
            if (extracted) return

            try {
                val pm = ctx.packageManager
                packageName = ctx.packageName
                val pInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                versionName = pInfo.versionName

                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    pInfo.versionCode.toLong()
                }

                appName = URLEncoder.encode(pInfo.applicationInfo.loadLabel(pm).toString(), "UTF-8")
                installerPackageName = pm.getInstallerPackageName(packageName)


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    installDate = pInfo.firstInstallTime.toString()
                    firstInstallDate = pInfo.firstInstallTime.toString()
                    lastUpdateDate = pInfo.lastUpdateTime.toString()
                } else {
                    installDate = ""
                    firstInstallDate = ""
                    lastUpdateDate = ""
                }


                val bundle = appInfo.metaData

                appId = getString(bundle, "com.zing.zalo.zalosdk.appID", null)
                if (appId == null) {
                    appId = getString(bundle, "appID", null)
                }

                isAutoTrackingOpenApp = getBoolean(bundle, "com.zing.zalosdk.configAutoTrackingActivity", true)
                preloadChannel = getString(bundle, "com.zalo.sdk.preloadChannel", "")

            } catch (ex: Exception) {
                Log.e("extractBasicAppInfo", ex.toString())
            }

            extracted = true
        }
    }
    //#endregion
}
	
