package com.zing.zalo.zalosdk.core.helper

object AppInfoHelper {
    val appId = "123456"
    val zdId = "z_device_id"
    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
    }
}