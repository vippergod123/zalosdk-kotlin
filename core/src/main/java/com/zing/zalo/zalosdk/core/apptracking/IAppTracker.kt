package com.zing.zalo.zalosdk.core.apptracking

import android.content.Context

interface IAppTracker {
    fun run()
    fun setListener(listener: AppTrackerListener)
}

interface AppTrackerListener {
    fun onAppTrackerCompleted(didRun: Boolean, scanId: String, packageNames: List<String>, installedApps: List<String>)
}