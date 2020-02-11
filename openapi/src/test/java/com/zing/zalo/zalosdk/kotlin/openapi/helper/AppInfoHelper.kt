package com.zing.zalo.zalosdk.kotlin.openapi.helper

import com.zing.zalo.zalosdk.kotlin.core.helper.AppInfo

object AppInfoHelper {
    const val appId = "123456"
    const val packageName = "packageName"
    const val scanId = "3"
    const val appName = "ABC"
    const val versionName = "2"
    const val applicationHashKey = "applicationHashKey"

    fun setup() {
        AppInfo.extracted = true
        AppInfo.appId = appId
        AppInfo.packageName = packageName
        AppInfo.versionName = versionName
        AppInfo.applicationHashKey = applicationHashKey
    }
}