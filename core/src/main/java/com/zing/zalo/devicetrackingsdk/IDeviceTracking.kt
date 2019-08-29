package com.zing.zalo.devicetrackingsdk

interface IDeviceIdListener {
    fun onDeviceIdAvailable(deviceId: String)
}

interface IDeviceTracking {
    fun getSDKId(): String?
    fun getPrivateKey(): String?
    fun getDeviceId(): String?
    fun getDeviceId(listener: IDeviceIdListener)
}