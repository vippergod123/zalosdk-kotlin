package com.zing.zalo.zalosdk.kotlin.analytics.helper

import com.zing.zalo.zalosdk.kotlin.analytics.model.Event

object DataHelper {
    const val EVENT_STORED_IN_DEVICE =
        "{\"events\":[{\"params\":{\"name\":\"Luke\",\"age\":\"0\"},\"action\":\"0\"},{\"params\":{\"name\":\"Luke\",\"age\":\"1\"},\"action\":\"1\"},{\"params\":{\"name\":\"Luke\",\"age\":\"2\"},\"action\":\"2\"},{\"params\":{\"name\":\"Luke\",\"age\":\"3\"},\"action\":\"3\"},{\"params\":{\"name\":\"Luke\",\"age\":\"4\"},\"action\":\"4\"},{\"params\":{\"name\":\"Luke\",\"age\":\"5\"},\"action\":\"5\"},{\"params\":{\"name\":\"Luke\",\"age\":\"6\"},\"action\":\"6\"},{\"params\":{\"name\":\"Luke\",\"age\":\"7\"},\"action\":\"7\"},{\"params\":{\"name\":\"Luke\",\"age\":\"8\"},\"action\":\"8\"},{\"params\":{\"name\":\"Luke\",\"age\":\"9\"},\"action\":\"9\"},{\"params\":{\"name\":\"Luke\",\"age\":\"10\"},\"action\":\"10\"}]}"

    const val preloadInfo = "preload_info"
    fun mockEvent(): Event {
        val timeStamp = System.currentTimeMillis()
        val action = "action-$timeStamp"
        val params = mutableMapOf<String, String>()


        params["name"] = "datahelper-$timeStamp"
        params["age"] = timeStamp.toString()
        return Event(action, params, timeStamp)
    }
}

object AppInfoHelper {
    const val appName = "app_name"
    const val versionName = "version_name"
    const val appId = "app_id"

}

object DeviceHelper {
    const val deviceId = "device_id"
    const val adsId = "ads_id"
}
