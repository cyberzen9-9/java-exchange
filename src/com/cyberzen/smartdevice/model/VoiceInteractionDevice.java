package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.capability.Rechargeable;
import com.cyberzen.smartdevice.enums.DeviceType;

public final class VoiceInteractionDevice extends SmartDevice implements Rechargeable {

    public VoiceInteractionDevice(String deviceID, String deviceName) {
        super(deviceID, deviceName);
    }

    @Override
    String taskDescription() {
        return "正在执行语音交互任务";
    }
    //重写充电方法，实现充电功能
    @Override
    public String charge() {
        startCharging();
        return "语音交互设备正在充电";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.VOICE_INTERACTION;
    }
}
