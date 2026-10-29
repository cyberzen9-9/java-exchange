package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.enums.DeviceType;

public final class EdgeVisionDevice extends SmartDevice{

    public EdgeVisionDevice(String deviceID, String deviceName) {
        super(deviceID, deviceName);
    }

    @Override
    String taskDescription() {
        return "正在执行图像识别任务";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.EDGE_VISION;
    }

}
