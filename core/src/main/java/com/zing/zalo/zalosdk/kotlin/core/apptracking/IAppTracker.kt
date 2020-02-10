package com.zing.zalo.zalosdk.kotlin.core.apptracking

interface IAppTracker {
    var listener: AppTrackerListener?
}

interface AppTrackerListener {
    fun onAppTrackerCompleted(
        didRun: Boolean,
        scanId: String,
        packageNames: List<String>,
        installedApps: List<String>
    )
}