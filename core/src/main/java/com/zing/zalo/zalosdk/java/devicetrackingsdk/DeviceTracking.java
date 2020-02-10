package com.zing.zalo.zalosdk.java.devicetrackingsdk;

import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.DeviceTrackingListener;
import com.zing.zalo.zalosdk.kotlin.core.devicetrackingsdk.IDeviceTracking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeviceTracking implements IDeviceTracking {

    private static DeviceTracking instance = new DeviceTracking();

    private DeviceTracking() {
    }

    public static DeviceTracking getInstance() {
        return instance;
    }

    @Deprecated()
    @Override
    public void initDeviceTracking() {
    }

    @Override
    public String getDeviceId() {

        return DeviceTracking.instance.getDeviceId();
    }

    @Override
    public void getDeviceId(@Nullable DeviceTrackingListener listener) {
        DeviceTracking.instance.getDeviceId(listener);
    }

    @NotNull
    @Override
    public String getVersion() {
        return DeviceTracking.getInstance().getVersion();
    }
}

