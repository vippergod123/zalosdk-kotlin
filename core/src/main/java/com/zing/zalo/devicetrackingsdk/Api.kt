package com.zing.zalo.devicetrackingsdk

import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.zing.zalo.zalosdk.core.Constant
import com.zing.zalo.zalosdk.core.helper.AppInfo
import com.zing.zalo.zalosdk.core.helper.DeviceInfo
import com.zing.zalo.zalosdk.core.log.Log
import org.json.JSONObject

class Api(private val context: Context) {

    private var data = JSONObject()
    fun prepareTrackingData(currentDeviceId: String, ts: Long): JSONObject {

        try {
            data.put("pkg", AppInfo.getPackageName(context))
            data.put("pl", "android")
            data.put("osv", DeviceInfo.getOSVersion())

            data.put("sdkv", Constant.VERSION)
            data.put("sdkv", DeviceInfo.getSDKVersion())
            data.put("an", AppInfo.getAppName(context)) //imp
            data.put("av", AppInfo.getVersionName(context))
//            data.put("dId", DeviceInfo.getAdvertiseID(context)) //imp


            data.put("mod", DeviceInfo.getModel())
            data.put("ss", DeviceInfo.getScreenSize(context))


            data.put("mno", DeviceInfo.getMobileNetworkCode(context))
            if (!TextUtils.isEmpty(currentDeviceId)) {
                data.put("sId", currentDeviceId)
            }

            data.put("dId", DeviceInfo.getAdvertiseID(context))
            data.put("adId", DeviceInfo.getAdvertiseID(context))

            data.put("ins_pkg", AppInfo.getInstallerPackageName(context))
            if (!TextUtils.isEmpty(AppInfo.getReferrer(context))) {
                data.put("ref", AppInfo.getReferrer(context))
            }
            data.put("ins_dte", AppInfo.getInstallDate(context))
            data.put("fst_ins_dte", AppInfo.getFirstInstallDate(context))
            data.put("lst_ins_dte", AppInfo.getLastUpdate(context))
            data.put("fst_run_dte", AppInfo.getFirstRunDate(context))
            data.put("ts", ts.toString())
            data.put("brd", DeviceInfo.getBrand())
            data.put("dev", Build.DEVICE)
            data.put("prd", DeviceInfo.getProduct())
            data.put("adk_ver", Build.VERSION.SDK_INT)
            data.put("mnft", DeviceInfo.getManufacturer())
            data.put("dev_type", Build.TYPE)
            data.put("avc", AppInfo.getVersionCode(context))
            data.put("was_ins", AppInfo.isPreInstalled(context).toString())
            data.put("dpi", context.resources.displayMetrics.density.toDouble())

            val preloadInfo = DeviceInfo.getPreloadInfo(context)
            data.put("preload", preloadInfo.preload)

            data.put("preloadDefault", AppInfo.getPreloadChannel(context))
            if (!preloadInfo.isPreloaded()) {
                data.put("preloadFailed", preloadInfo.error)
            }

//            data.put("conn", DeviceInfo.getConnectionType(context))
//            val loc = DeviceInfo.getLocation(context) // remove
//            data.put("mac", DeviceInfo.getWLANMACAddress(context)) //remove
//            data.put("ser", DeviceInfo.getSerial()) // remove
//            data.put("lang", Locale.getDefault().toString()) // remove
//            data.put("aId", DeviceInfo.getAndroidId(context)) // remove
        } catch (e: Exception) {
            Log.e("prepareTrackingData", e)
        }

        return data
    }

    fun prepareDeviceIdData(): JSONObject {
        data = JSONObject()

        try {
            data.put("dId", DeviceInfo.getAdvertiseID(context))
            data.put("aId", DeviceInfo.getAndroidId(context))
            data.put("mod", DeviceInfo.getModel())
            data.put("ser", DeviceInfo.getSerial())
        } catch (e: Exception) {
            Log.e("prepareDeviceIdData", e)
        }

        return data
    }
}
