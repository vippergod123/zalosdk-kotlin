package com.zing.zalo.zalosdk.core.helper

object AppInfoHelper {
    val appId = "appId_123456"
    val scanId = "scanId_3"


    val appName = "appName"
    val versionName = "2"
    val advertiserId = "advertiserId"

    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
        AppInfo.appName = appName
        AppInfo.versionName = versionName
        DeviceInfo.advertiserId = advertiserId
    }
}