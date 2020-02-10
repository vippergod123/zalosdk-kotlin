package com.zing.zalo.zalosdk.kotlin.core.helper

import android.Manifest
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Looper
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.WindowManager
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.model.PreloadInfo
import com.zing.zalo.zalosdk.kotlin.core.Constant
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

object DeviceInfo {

    private var lock = Any()
    internal var advertiserId: String? = null
    private var screenSize: String = "unknown"
    private var MNO: String = "unknown"
    var preloadInfo: PreloadInfo = PreloadInfo()

    private const val KEY_PRELOAD = "com.zing.zalo.sdk.preloadkey"
    private const val KEY_EXCEPTION_FILE_PRELOAD = "com.zing.zalo.sdk.preloadkey.exception"
    private val PRELOAD_PATH = arrayOf(
        "/data/etc/appchannel/zalo_appchannel.in", //oppo new: fileCode: 0
        "/data/etc/appchannel", //oppo old: fileCode: 1
        "/system/etc/zalo_appchannel.in"//vivo: fileCode: 2
    )


    fun getAdvertiseID(context: Context): String {
        if (!TextUtils.isEmpty(advertiserId)) return advertiserId.toString()

        val advertiseStorage =
            Storage(context).privateSharedPreferences(Constant.sharedPreference.PREFS_ADVERTISE_ID)
        try {
            advertiserId = advertiseStorage.getString("adsidstr")
            if (!TextUtils.isEmpty(advertiserId)) {
                return advertiserId.toString()
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                throw Exception("DeviceHelper.getAdvertiseID call on main thread!!")
            }

            val getAdvertisingIdInfo = Utils.getMethodQuietly(
                "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo",
                Context::class.java
            )
            if (getAdvertisingIdInfo != null) {
                val advertisingInfo =
                    Utils.invokeMethodQuietly(null, getAdvertisingIdInfo, context)
                if (advertisingInfo != null) {
                    val getId = Utils.getMethodQuietly(advertisingInfo::class.java, "getId")
                    val isLimitAdTrackingEnabled =
                        Utils.getMethodQuietly(
                            advertisingInfo.javaClass,
                            "isLimitAdTrackingEnabled"
                        )
                    if (getId != null && isLimitAdTrackingEnabled != null) {
                        val result = Utils.invokeMethodQuietly(advertisingInfo, getId) as String?
                        if (!TextUtils.isEmpty(result)) {
                            advertiserId = result
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("getAdvertiseID", ex)
        }

        //save to share pref
        if (!TextUtils.isEmpty(advertiserId)) {
            advertiseStorage.setString("adsidstr", advertiserId.toString())
        }

        return advertiserId ?: ""
    }

    fun getOSVersion(): String {
        return Build.VERSION.RELEASE
    }

    fun getSDKVersion(): String {
        return Constant.VERSION
    }

    fun getModel(): String {
        return Build.MODEL
    }

    fun getBrand(): String {
        return Build.BRAND
    }

    fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    fun getProduct(): String {
        return Build.PRODUCT
    }

    fun getScreenSize(context: Context): String {
        screenSize = try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val point = Point()
            display.getSize(point)
            point.x.toString() + "x" + point.y
        } catch (ex: Exception) {
            "unknown"
        }
        return screenSize
    }

    fun getMobileNetworkCode(context: Context): String {
        MNO = try {
            if (Utils.isPermissionGranted(
                    context, Manifest.permission.READ_PHONE_STATE
                )
            ) {
                val tel = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                tel.simOperator
            } else {
                "unknown"
            }
        } catch (ex: Exception) {
            "unknown"
        }

        return MNO
    }


    fun getPreloadInfo(context: Context): PreloadInfo {
        synchronized(lock) {
            if (preloadInfo.isPreloaded()) {
                return preloadInfo
            }

            val preloadStorage =
                Storage(context).privateSharedPreferences(Constant.sharedPreference.PREFS_NAME_PRELOAD)

            //load from cacheDeviceInfo
            val preload = preloadStorage.getString(KEY_PRELOAD) ?: ""
            val error = preloadStorage.getString(KEY_EXCEPTION_FILE_PRELOAD) ?: ""
            if (!TextUtils.isEmpty(preload) || !TextUtils.isEmpty(error)/*error != null*/) {
                preloadInfo = PreloadInfo(preload, error)
                return preloadInfo
            }
            var count = 0
            //load from file
            try {
                preloadInfo = PreloadInfo()
                val preloadJSonFailed = JSONArray()
                for (filePath in PRELOAD_PATH) {
                    val jsonObject = JSONObject()
                    jsonObject.put(
                        "fileCode",
                        listOf(*PRELOAD_PATH).indexOf(filePath)
                    )

                    try {
                        val file = File(filePath)
                        val data = Utils.readFileData(file)
                        if (!TextUtils.isEmpty(data)) {
                            if (data.contains(":")) {
                                val preloadData =
                                    data.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                                        .toTypedArray()
                                if (preloadData.size == 2) {
                                    preloadInfo.preload = preloadData[1].trim { it <= ' ' }
                                }
                            } else {
                                jsonObject.put("err", "-3")
                            }
                        } else {
                            jsonObject.put("err", "-4")
                        }

                    } catch (ex: FileNotFoundException) {
                        Log.w("getPreloadInfo", ex)
                        jsonObject.put("err", "-2")
                        count++
                    } catch (ex: Exception) {
                        Log.w("getPreloadInfo", ex)
                        jsonObject.put("err", "-1: " + ex.message)
                    }

                    preloadJSonFailed.put(jsonObject)
                }

                preloadInfo.error = preloadJSonFailed.toString()
            } catch (ex: JSONException) {
                Log.e("getPreloadInfo", ex)
            }

            if (count == PRELOAD_PATH.size) {
                preloadInfo.preload = ""
                preloadInfo.error = ""
                //all files not found, if another exception will submit to server
            }
            //cache it
            preloadStorage.setString(KEY_PRELOAD, preloadInfo.preload)
            preloadStorage.setString(KEY_EXCEPTION_FILE_PRELOAD, preloadInfo.error)
        }
        return preloadInfo
    }

    fun getAndroidId(): String {
        return "unknown"
    }

    fun getSerial(): String {
        return "unknown"
    }


    fun prepareDeviceIdData(context: Context): JSONObject {
        val data = JSONObject()

        try {
            data.put("dId", getAdvertiseID(context))
            data.put("aId", getAndroidId())
            data.put("mod", getModel())
            data.put("ser", getSerial())
        } catch (e: Exception) {
            Log.e("prepareDeviceIdData", e)
        }

        return data
    }

    fun prepareTrackingData(context: Context, currentDeviceId: String?, ts: Long): JSONObject {
        val data = JSONObject()
        try {
            data.put("pkg", AppInfo.getPackageName(context))
            data.put("pl", "android")
            data.put("osv", getOSVersion())

            data.put("sdkv", Constant.VERSION)
            data.put("sdkv", getSDKVersion())
            data.put("an", AppInfo.getAppName(context)) //imp
            data.put("av", AppInfo.getVersionName(context))

            data.put("mod", getModel())
            data.put("ss", getScreenSize(context))

            data.put("mno", getMobileNetworkCode(context))
            if (!TextUtils.isEmpty(currentDeviceId)) {
                data.put("sId", currentDeviceId)
            }

            data.put("dId", getAdvertiseID(context))
            data.put("adId", getAdvertiseID(context))

            data.put("ins_pkg", AppInfo.getInstallerPackageName(context))
            if (!TextUtils.isEmpty(AppInfo.getReferrer(context))) {
                data.put("ref", AppInfo.getReferrer(context))
            }
            data.put("ins_dte", AppInfo.getInstallDate(context))
            data.put("fst_ins_dte", AppInfo.getFirstInstallDate(context))
            data.put("lst_ins_dte", AppInfo.getLastUpdate(context))
            data.put("fst_run_dte", AppInfo.getFirstRunDate(context))
            data.put("ts", ts.toString())
            data.put("brd", getBrand())
            data.put("dev", Build.DEVICE)
            data.put("prd", getProduct())
            data.put("adk_ver", Build.VERSION.SDK_INT)
            data.put("mnft", with(DeviceInfo) { getManufacturer() })
            data.put("dev_type", Build.TYPE)
            data.put("avc", AppInfo.getVersionCode(context))
            data.put("was_ins", AppInfo.isPreInstalled(context).toString())
            data.put("dpi", context.resources.displayMetrics.density.toDouble())

            val preloadInfo = getPreloadInfo(context)
            data.put("preload", preloadInfo.preload)

            data.put("preloadDefault", AppInfo.getPreloadChannel(context))
            if (!preloadInfo.isPreloaded()) {
                data.put("preloadFailed", preloadInfo.error)
            }
        } catch (e: Exception) {
            Log.e("tracking", e)
        }

        return data
    }

}