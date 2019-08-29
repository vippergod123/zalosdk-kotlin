package com.zing.zalo.zalosdk.core

import org.robolectric.Robolectric

object TestUtils {
    fun waitTaskRunInBackgroundAndForeground() {
        Robolectric.flushBackgroundThreadScheduler()
        Robolectric.flushForegroundThreadScheduler()
    }
}