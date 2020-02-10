package com.zing.zalo.zalosdk.kotlin.oauth

import org.robolectric.Robolectric

object TestUtils {
    fun waitTaskRunInBackgroundAndForeground() {
        Robolectric.flushBackgroundThreadScheduler()
        Robolectric.flushForegroundThreadScheduler()
    }
}