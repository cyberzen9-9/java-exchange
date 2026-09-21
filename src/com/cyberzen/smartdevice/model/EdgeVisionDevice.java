package com.cyberzen.smartdevice.model;

public class EdgeVisionDevice extends SmartDevice{

    public EdgeVisionDevice(String deviceID, String deviceName) {
        super(deviceID, deviceName);
    }

    @Override
    public void execute() {
        System.out.println("正在执行图像识别任务");
    }

}
