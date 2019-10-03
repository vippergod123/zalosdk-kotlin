package com.zing.zalo.zalosdk.core.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.Base64
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.Utils.isExternalStorageReadable
import com.zing.zalo.zalosdk.core.log.Log
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest

object AppInfo {
    private val lock = Any()
    var extracted: Boolean = false
    var appId: String? = null
    internal var versionName: String? = null
    internal var versionCode: Long = 0
    internal var appName: String? = null
    var applicationHashKey: String? = null
    internal var firstInstallDate: String? = null
    internal var installDate: String? = null
    internal var lastUpdateDate: String? = null
    internal var installerPackageName: String? = null
    internal var preloadChannel: String? = null
    internal var packageName: String? = null
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

    fun getAppIdLong(ctx: Context): Long {
        val str = getAppId(ctx)
        return try {
            java.lang.Long.parseLong(str)
        } catch (ex: Exception) {
            Log.w("getAppIdLong", ex)
            return 0L
        }

    }

    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    fun getApplicationHashKey(ctx: Context): String? {
        if (applicationHashKey != null) return applicationHashKey

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signInfo =
                    ctx.packageManager.getPackageInfo(
                        ctx.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
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
                val info = ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.GET_SIGNATURES
                )
                info.signatures.map { signature ->
                    encodeSignature(signature)
                }
            }

        } catch (e: Exception) {
            Log.e("AppInfo: getApplicationHashKey()", e)
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

    fun getPackageName(context: Context): String {
        return getPropertyAsT(context, "packageName") ?: ""

    }

    fun getAppId(context: Context): String {
        return getPropertyAsT(context, "appId") ?: ""
    }

    fun getAppName(context: Context): String {
        return getPropertyAsT(context, "appName") ?: ""
    }

    fun getVersionName(context: Context): String {
        return getPropertyAsT(context, "versionName") ?: ""
    }

    fun getVersionCode(context: Context): Long {
        return getPropertyAsT(context, "versionCode") ?: 0L
    }

    fun getInstallerPackageName(context: Context): String {
        return getPropertyAsT(context, "installerPackageName") ?: ""
    }

    fun getInstallDate(context: Context): String {
        return getPropertyAsT(context, "installDate") ?: ""
    }

    fun getFirstInstallDate(context: Context): String {

        return getPropertyAsT(context, "firstInstallDate") ?: ""
    }

    fun getLastUpdate(context: Context): String {

        return getPropertyAsT(context, "installDate") ?: ""
    }

    fun getFirstRunDate(context: Context): String {
        return getPropertyAsT(context, "firstInstallDate") ?: ""
    }

    fun getPreloadChannel(context: Context): String {
        return getPropertyAsT(context, "preloadChannel") ?: ""
    }

    fun isPreInstalled(context:Context): Boolean {
        try {
            if (isExternalStorageReadable(context)) {
                val file = prepareFileInExternalStore(context.packageName, false)
                return if (file.exists()) {
                    true
                } else {
                    file.createNewFile()
                    false
                }
            }
        } catch (e: Exception) {
            return false
        }

        return false
    }

    //#region private supportive method
    private fun prepareFileInExternalStore(fileName: String, clearIfExists: Boolean): File {
        val path =
            Environment.getExternalStorageDirectory().absolutePath + "/Android/data/com.google.android.zdt.data/" + fileName
        val f = File(path)
        f.parentFile.mkdirs()

        if (clearIfExists && f.exists()) {
            f.delete()
        }

        return f
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getPropertyAsT(context: Context, key: String): T? {
        var result: T?
        synchronized(lock) {
            if (extracted)
                result = findPropertyValue(key) as T
        }
        extractBasicAppInfo(context)
        result = findPropertyValue(key) as T

        return result
    }


    private fun findPropertyValue(key: String): Any? {
        return when (key) {
            "appId" -> appId
            "versionName" -> versionName
            "versionCode" -> versionCode
            "appName" -> appName
            "applicationHashKey" -> applicationHashKey
            "firstInstallDate" -> firstInstallDate
            "installDate" -> installDate
            "lastUpdateDate" -> lastUpdateDate
            "installerPackageName" -> installerPackageName
            "preloadChannel" -> preloadChannel
            "isAutoTrackingOpenApp" -> isAutoTrackingOpenApp
            "packageName" -> packageName
            else -> null
        }
    }

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

    @Suppress("DEPRECATION")
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

                if (TextUtils.isEmpty(appId)) {
                    appId = getString(bundle, "appID", "")
                }

                isAutoTrackingOpenApp =
                    getBoolean(bundle, "com.zing.zalosdk.configAutoTrackingActivity", true)

                preloadChannel = getString(bundle, "com.zalo.sdk.preloadChannel", "")

            } catch (ex: Exception) {
                Log.e("extractBasicAppInfo", ex.toString())
            }

            extracted = true
        }
    }

    fun trackingData(context: Context, currentDeviceId: String, ts: Long): JSONObject {
        val data = JSONObject()
        try {
            data.put("pkg", getPackageName(context))
            data.put("pl", "android")
            data.put("osv", DeviceInfo.getOSVersion())

            data.put("sdkv", Constant.VERSION)
            data.put("sdkv", DeviceInfo.getSDKVersion())
            data.put("an", getAppName(context)) //imp
            data.put("av", getVersionName(context))

            data.put("mod", DeviceInfo.getModel())
            data.put("ss", DeviceInfo.getScreenSize(context))

            data.put("mno", DeviceInfo.getMobileNetworkCode(context))
            if (!TextUtils.isEmpty(currentDeviceId)) {
                data.put("sId", currentDeviceId)
            }

            data.put("dId", DeviceInfo.getAdvertiseID(context))
            data.put("adId", DeviceInfo.getAdvertiseID(context))

            data.put("ins_pkg", getInstallerPackageName(context))
            if (!TextUtils.isEmpty(getReferrer(context))) {
                data.put("ref", getReferrer(context))
            }
            data.put("ins_dte", getInstallDate(context))
            data.put("fst_ins_dte", getFirstInstallDate(context))
            data.put("lst_ins_dte", getLastUpdate(context))
            data.put("fst_run_dte", getFirstRunDate(context))
            data.put("ts", ts.toString())
            data.put("brd", DeviceInfo.getBrand())
            data.put("dev", Build.DEVICE)
            data.put("prd", DeviceInfo.getProduct())
            data.put("adk_ver", Build.VERSION.SDK_INT)
            data.put("mnft", DeviceInfo.getManufacturer())
            data.put("dev_type", Build.TYPE)
            data.put("avc", getVersionCode(context))
            data.put("was_ins", isPreInstalled(context).toString())
            data.put("dpi", context.resources.displayMetrics.density.toDouble())

            val preloadInfo = DeviceInfo.getPreloadInfo(context)
            data.put("preload", preloadInfo.preload)

            data.put("preloadDefault", getPreloadChannel(context))
            if (!preloadInfo.isPreloaded()) {
                data.put("preloadFailed", preloadInfo.error)
            }
        } catch (e: Exception) {
            Log.e("tracking", e)
        }

        return data
    }

    //#endregion
}
	
