package com.zing.zalo.devicetrackingsdk

interface DeviceTrackingListener {
    fun onComplete(result: String)
}

interface IDeviceTracking {
    fun getDeviceId(): String?
    fun getDeviceId(listener: DeviceTrackingListener?)
}

interface ISdkTracking {
    fun getSDKId(): String?
    fun getPrivateKey(): String?
}
