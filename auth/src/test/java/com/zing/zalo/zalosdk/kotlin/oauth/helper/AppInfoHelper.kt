package com.zing.zalo.zalosdk.kotlin.oauth.helper

import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo

object AppInfoHelper {
    val appId = "123456"
    val zdId = "3000.4258336839922934833"
    val applicationHashKey = "abcd1234"
    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
        AppInfo.applicationHashKey = applicationHashKey
    }
}