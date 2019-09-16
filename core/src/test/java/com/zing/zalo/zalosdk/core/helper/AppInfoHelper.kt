package com.zing.zalo.zalosdk.core.helper

object AppInfoHelper {
    val appId = "123456"
    val zdId = "3000.4258336839922934833"
    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
    }
}