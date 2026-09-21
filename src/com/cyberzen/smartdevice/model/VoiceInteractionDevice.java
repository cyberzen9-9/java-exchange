package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.capability.Rechargeable;

public class VoiceInteractionDevice extends SmartDevice implements Rechargeable {

    public VoiceInteractionDevice(String deviceID, String deviceName) {
        super(deviceID, deviceName);
    }

    @Override
    public void execute() {
        System.out.println("正在执行语音交互任务");
    }
    //重写充电方法，实现充电功能
    @Override
    public void charge() {
        System.out.println("语音交互设备正在充电");
    }
}
