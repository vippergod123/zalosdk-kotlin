package com.zing.zalo.zalosdk.kotlin.core.helper

object AppInfoHelper {
    const val appId = "appId_123456"
    const val scanId = "scanId_3"
    const val appName = "appName"
    const val versionName = "2"
    const val applicationHashKey = "applicationHashKey"
    const val advertiserId = "advertiserId"

    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
        AppInfo.appName = appName
        AppInfo.versionName = versionName
        DeviceInfo.advertiserId = advertiserId
        AppInfo.applicationHashKey = applicationHashKey
    }
}