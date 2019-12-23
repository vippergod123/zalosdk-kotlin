package com.zing.zalo.devicetrackingsdk

import androidx.annotation.Keep

@Keep
interface DeviceTrackingListener {
    fun onComplete(result: String)
}

@Keep
interface IDeviceTracking {
    fun getDeviceId(): String?
    fun getDeviceId(listener: DeviceTrackingListener?)
}

@Keep
interface ISdkTracking {
    fun getSDKId(): String?
    fun getPrivateKey(): String?
}
