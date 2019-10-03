//package com.zing.zalo.devicetrackingsdk
//
//import android.content.Context
//import android.os.AsyncTask
//import android.os.Build
//import android.text.TextUtils
//import com.zing.zalo.zalosdk.core.Constant
//import com.zing.zalo.zalosdk.core.helper.AppInfo
//import com.zing.zalo.zalosdk.core.helper.DeviceInfo
//import com.zing.zalo.zalosdk.core.helper.Storage
//import com.zing.zalo.zalosdk.core.helper.Utils
//import com.zing.zalo.zalosdk.core.http.HttpClient
//import com.zing.zalo.zalosdk.core.http.HttpUrlEncodedRequest
//import com.zing.zalo.zalosdk.core.log.Log
//import com.zing.zalo.zalosdk.core.servicemap.ServiceMapManager
//import org.json.JSONException
//import org.json.JSONObject
//import java.lang.ref.WeakReference
//
//
//object DeviceTrackingAsyncTask {
//    /**
//     * Call when data not save in default storage
//     * @param weakContext can be collected by GB
//     * @param listener callback privateKey & SdkID
//     */
//    class GetSdkId(
//        private val weakContext: WeakReference<Context>,
//        private var listener: SdkTrackingListener?
//    ) : AsyncTask<Void, Void, JSONObject>() {
//
//        var httpClient = HttpClient(ServiceMapManager.urlFor(
//            ServiceMapManager.KEY_URL_CENTRALIZED
//        ))
//        var request = HttpUrlEncodedRequest(Constant.api.API_SDK_ID)
//
//        override fun doInBackground(vararg params: Void?): JSONObject? {
//            val context = weakContext.get()
//            try {
//                if (context == null) throw Exception("Context is null")
//
//                val deviceIdData = prepareDeviceIdData(context).toString()
//                request.addParameter("appId", AppInfo.getAppId(context))
//                request.addParameter("sdkv", DeviceInfo.getSDKVersion())
//                request.addParameter("pl", "android")
//                request.addParameter("osv", DeviceInfo.getOSVersion())
//                request.addParameter("model", DeviceInfo.getModel())
//                request.addParameter("screenSize", DeviceInfo.getScreenSize(context))
//                request.addParameter("device", deviceIdData)
//                request.addParameter("ref", AppInfo.getReferrer(context))
//
//
//                val jsonObject  = httpClient.send(request).getJSON()
//                val errorCode = jsonObject?.getInt("error")
//                if (errorCode == 0) {
//                    val data = jsonObject.getJSONObject("data")
//                    val sdkId = data.optString("sdkId")
//                    val privateKey = data.optString("privateKey")
//
//                    val dataJson = JSONObject()
//                    dataJson.put("sdkId", sdkId)
//                    dataJson.put("privateKey", privateKey)
//
//                    val sdkTracking = SdkTracking(context)
//                    sdkTracking.setSDKId(sdkId)
//                    sdkTracking.setPrivateKey(privateKey)
//
////                    DeviceTracking.setSDKId(sdkId)
////                    DeviceTracking.setPrivateKey(privateKey)
//
//                    return dataJson
//                }
//            } catch (ex: JSONException) {
//                Log.e("GetSdkId", ex)
//            } catch (ex: Exception) {
//                Log.e("GetSdkId", ex)
//            }
//            return null
//        }
//
//        /*
//        * Listener must be called in onPostExecute to avoid when doInBackground() return null
//        */
//        override fun onPostExecute(result: JSONObject?) {
//            super.onPostExecute(result)
//
//            val data = result?.toString()
//            listener?.onComplete(data)
//            listener = null
//        }
//
//
//    }
//
//    class GetDeviceIdAsyncTask(
//        private val weakContext: WeakReference<Context>,
//        private val currentDeviceId: String,
//        private val timestamp: Long,
//        private var listener: DeviceTrackingListener?
//    ) : AsyncTask<Void, Void, JSONObject?>() {
//
//        var httpClient = HttpClient(ServiceMapManager.urlFor(
//            ServiceMapManager.KEY_URL_CENTRALIZED
//        ))
//
//        var request = HttpUrlEncodedRequest(Constant.api.API_HARDWARE_ID_URL)
////        private var api = weakContext.get()?.let { PrepareData(it) }
//
//        override fun doInBackground(vararg params: Void?): JSONObject? {
//            val context = weakContext.get()
//            try {
//                if (context == null) throw Exception("Context is null")
//                val sdkTracking = SdkTracking(context)
//
////                if (api == null) throw Exception("PrepareData cannot be initialized")
////                val deviceIdData = api?.deviceId()
////                val trackingData = api?.tracking(currentDeviceId, timestamp)
//
//                val deviceIdData = prepareDeviceIdData(context)
//                val trackingData = prepareTrackingData(context,currentDeviceId, timestamp)
//
//                val sdkId = sdkTracking.getSDKId() ?: ""
//                val appId = AppInfo.getAppId(context)
//                val authCode = Storage(context).getOAuthCode() ?: ""
//
//                val param = arrayOf("pl", "appId", "oauthCode", "device", "data", "ts", "sdkId")
//                val values = arrayOf(
//                    "android",
//                    appId,
//                    authCode,
//                    deviceIdData.toString(),
//                    trackingData.toString(),
//                    timestamp.toString(),
//                    sdkId
//                )
//
//                val sig = Utils.getSignature(
//                    param,
//                    values,
//                    Constant.key.TRK_SECRET_KEY
//                )
//
//                request.addQueryStringParameter("pl", "android")
//                request.addQueryStringParameter("appId", appId)
//                request.addQueryStringParameter("oauthCode", authCode)
//                request.addQueryStringParameter("device", deviceIdData.toString())
//                request.addQueryStringParameter("data", trackingData.toString())
//                request.addQueryStringParameter("ts", "" + timestamp)
//                request.addQueryStringParameter("sig", sig)
//                request.addQueryStringParameter("sdkId", sdkId)
//
//                val jsonObject = httpClient.send(request).getJSON()
//
//                val errorCode = jsonObject?.getInt("error")
//                if (errorCode == 0) {
//                    val data = jsonObject.getJSONObject("data")
//                    val deviceId = data.optString("deviceId")
//
//                    // expiredTime = duration + currentTime
//                    val duration = data.optLong("expiredTime")
//                    val expiredTime = duration + System.currentTimeMillis()
//
//                    val dataJson = JSONObject()
//                    dataJson.put("deviceId", deviceId)
//                    dataJson.put("expireTime", expiredTime)
//
//                    DeviceTracking.setDeviceId(deviceId, expiredTime.toString())
//
//                    return dataJson
//                }
//
//
//            } catch (ex: JSONException) {
//                Log.e("GetDeviceIdAsyncTask", ex)
//            } catch (ex: Exception) {
//                Log.e("GetDeviceIdAsyncTask", ex)
//            }
//            return null
//        }
//
//
//        /*
//         * Listener must be called in onPostExecute to avoid when doInBackground() return null
//         */
//        override fun onPostExecute(result: JSONObject?) {
//            super.onPostExecute(result)
//
//            val deviceId = result?.optString("deviceId")
//            listener?.onDeviceIdSuccess(deviceId)
//
//            val data = result?.toString()
//            listener?.onComplete(data)
//            listener = null
//        }
//
//    }
//
//
//    //#region private supportive method
////    private fun prepareDeviceIdData(context:Context): JSONObject {
////        val data = JSONObject()
////
////        try {
////            data.put("dId", DeviceInfo.getAdvertiseID(context))
////            data.put("aId", DeviceInfo.getAndroidId(context))
////            data.put("mod", DeviceInfo.getModel())
////            data.put("ser", DeviceInfo.getSerial())
////        } catch (e: Exception) {
////            Log.e("prepareDeviceIdData", e)
////        }
////
////        return data
////    }
////
////    private fun prepareTrackingData(context:Context, currentDeviceId: String, ts: Long): JSONObject {
////        val data = JSONObject()
////        try {
////            data.put("pkg", AppInfo.getPackageName(context))
////            data.put("pl", "android")
////            data.put("osv", DeviceInfo.getOSVersion())
////
////            data.put("sdkv", Constant.VERSION)
////            data.put("sdkv", DeviceInfo.getSDKVersion())
////            data.put("an", AppInfo.getAppName(context)) //imp
////            data.put("av", AppInfo.getVersionName(context))
//////            data.put("dId", DeviceInfo.getAdvertiseID(context)) //imp
////
////
////            data.put("mod", DeviceInfo.getModel())
////            data.put("ss", DeviceInfo.getScreenSize(context))
////
////
////            data.put("mno", DeviceInfo.getMobileNetworkCode(context))
////            if (!TextUtils.isEmpty(currentDeviceId)) {
////                data.put("sId", currentDeviceId)
////            }
////
////            data.put("dId", DeviceInfo.getAdvertiseID(context))
////            data.put("adId", DeviceInfo.getAdvertiseID(context))
////
////            data.put("ins_pkg", AppInfo.getInstallerPackageName(context))
////            if (!TextUtils.isEmpty(AppInfo.getReferrer(context))) {
////                data.put("ref", AppInfo.getReferrer(context))
////            }
////            data.put("ins_dte", AppInfo.getInstallDate(context))
////            data.put("fst_ins_dte", AppInfo.getFirstInstallDate(context))
////            data.put("lst_ins_dte", AppInfo.getLastUpdate(context))
////            data.put("fst_run_dte", AppInfo.getFirstRunDate(context))
////            data.put("ts", ts.toString())
////            data.put("brd", DeviceInfo.getBrand())
////            data.put("dev", Build.DEVICE)
////            data.put("prd", DeviceInfo.getProduct())
////            data.put("adk_ver", Build.VERSION.SDK_INT)
////            data.put("mnft", DeviceInfo.getManufacturer())
////            data.put("dev_type", Build.TYPE)
////            data.put("avc", AppInfo.getVersionCode(context))
////            data.put("was_ins", AppInfo.isPreInstalled(context).toString())
////            data.put("dpi", context.resources.displayMetrics.density.toDouble())
////
////            val preloadInfo = DeviceInfo.getPreloadInfo(context)
////            data.put("preload", preloadInfo.preload)
////
////            data.put("preloadDefault", AppInfo.getPreloadChannel(context))
////            if (!preloadInfo.isPreloaded()) {
////                data.put("preloadFailed", preloadInfo.error)
////            }
////
//////            data.put("conn", DeviceInfo.getConnectionType(context))
//////            val loc = DeviceInfo.getLocation(context) // remove
//////            data.put("mac", DeviceInfo.getWLANMACAddress(context)) //remove
//////            data.put("ser", DeviceInfo.getSerial()) // remove
//////            data.put("lang", Locale.getDefault().toString()) // remove
//////            data.put("aId", DeviceInfo.getAndroidId(context)) // remove
////        } catch (e: Exception) {
////            Log.e("tracking", e)
////        }
////
////        return data
////    }
//
//    //#endregion
//}
