package com.zing.zalo.zalosdk.kotlin.core.module

import android.annotation.SuppressLint
import android.content.Context
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTracking
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTrackingListener
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.SdkTracking
import com.zing.zalo.zalosdk.kotlin.core.apptracking.AppTracker
import com.zing.zalo.zalosdk.kotlin.core.apptracking.AppTrackerListener
import com.zing.zalo.zalosdk.kotlin.core.apptracking.AppTrackerStorage
import com.zing.zalo.zalosdk.kotlin.core.helper.Storage
import com.zing.zalo.zalosdk.kotlin.core.log.Log
import com.zing.zalo.zalosdk.kotlin.core.servicemap.ServiceMapManager
import com.zing.zalo.zalosdk.kotlin.core.settings.SettingsManager
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object ModuleManager {

    private var isInitialized = AtomicBoolean(false)
    private val modules = mutableSetOf<IModule>()
    private var context: Context? = null

    init {
        val dt = DeviceTracking.getInstance()
        val st = SdkTracking.getInstance()
        val sm = SettingsManager.getInstance()
        val smm = ServiceMapManager.getInstance()

        modules.add(smm)
        modules.add(sm)
        modules.add(st)
        dt.sdkTracking = st
        modules.add(dt)



    }

    fun addModule(module: IModule) {
        if (isInitialized.get() && context != null) {
            module.start(context!!)
        }
        modules.add(module)
    }

    fun removeModule(module: IModule) {
        modules.remove(module)
    }

    fun initializeApp(context: Context) {
        if (isInitialized.get()) return

        this.context = context
        val iter = modules.iterator()
        while (iter.hasNext()) {
            iter.next().start(context)
        }

        isInitialized.set(true)
        DeviceTracking.getInstance().getDeviceId(object : DeviceTrackingListener {
            override fun onComplete(result: String) {
                onHasDeviceId(context, result)
            }
        })

        importModule()
    }

    private fun onHasDeviceId(context: Context, deviceId: String) {
        val at = AppTracker()
        at.sdkTracking = SdkTracking.getInstance()
        at.deviceId = deviceId
        at.storage = Storage(context)
        at.appTrackerStorage = AppTrackerStorage(context)
        at.listener = object : AppTrackerListener {
            override fun onAppTrackerCompleted(
                didRun: Boolean,
                scanId: String,
                packageNames: List<String>,
                installedApps: List<String>
            ) {
                at.listener = null
                at.stop()
                removeModule(at)
            }

        }
        addModule(at)
    }

    private fun importModule() {
        try {
            Class.forName("com.zing.zalo.zalosdk.kotlin.oauth.ZaloSDK")
            Class.forName("com.zing.zalo.zalosdk.kotlin.openapi.ZaloOpenApi")
            Class.forName("com.zing.zalo.zalosdk.kotlin.analytics.EventTracker")
        } catch (ex: Exception) {
            Log.w(ex)
        }

    }
}