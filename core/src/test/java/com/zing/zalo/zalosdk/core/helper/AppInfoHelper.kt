package com.zing.zalo.zalosdk.core.helper

object AppInfoHelper {
    val appId = "123456"
    val scanId = "3"


    val appName = "ABC"
    val versionName = "2"
    val advertiserId = "abcdef"

    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
        AppInfo.appName = appName
        AppInfo.versionName = versionName
        DeviceInfo.advertiserId = advertiserId
    }
}