package com.zing.zalo.devicetrackingsdk

interface DeviceTrackingListener {
    fun onComplete(result: String?)
}

interface IDeviceTracking {
    fun getSDKId(): String?
    fun getPrivateKey(): String?
    fun setSDKId(value: String)
    fun setPrivateKey(value: String)
    fun getDeviceId(): String
    fun getDeviceId(listener: DeviceTrackingListener?)
}