package com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk

import androidx.annotation.Keep

@Keep
interface DeviceTrackingListener {
    fun onComplete(result: String)
}

@Keep
interface IDeviceTracking {
    fun initDeviceTracking()
    fun getDeviceId(): String?
    fun getDeviceId(listener: DeviceTrackingListener?)
    fun getVersion():String
}

@Keep
interface ISdkTracking {
    fun getSDKId(): String?
    fun getPrivateKey(): String?
}
