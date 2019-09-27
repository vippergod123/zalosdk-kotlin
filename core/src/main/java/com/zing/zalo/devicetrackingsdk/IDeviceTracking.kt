package com.zing.zalo.devicetrackingsdk

//TODO: Callback này chỉ nên có 1 method onComplete
interface DeviceTrackingListener {
    fun onComplete(result: String?){}
    fun onDeviceIdSuccess(deviceId:String?){}
}

interface IDeviceTracking {
    fun setSDKId(value: String)
    fun getSDKId(): String?
    fun getSDKId(listener: DeviceTrackingListener?)

    fun setPrivateKey(value: String)
    fun getPrivateKey(): String?

    fun setDeviceId(deviceId:String, expiredTime:String)
    fun getDeviceId(): String?
    fun getDeviceId(listener: DeviceTrackingListener?)
}