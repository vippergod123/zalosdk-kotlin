package com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.model

import android.text.TextUtils

data class PreloadInfo(var preload: String = "", var error: String = "") {
    fun isPreloaded(): Boolean {
        return !TextUtils.isEmpty(preload) && !TextUtils.isEmpty(error)
    }
}