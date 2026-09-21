package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.enums.DeviceStatus;

public abstract class SmartDevice {
//    唯一的设备编号；
//    设备名称。
    private final String deviceId;
    private String deviceName;
    private DeviceStatus status;



    public SmartDevice(String deviceID, String deviceName) {
        this.deviceId = deviceID;
        this.deviceName = deviceName;
        this.status = DeviceStatus.IDLE;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public DeviceStatus getStatus() {
        return status;
    }



    //展示每台设备的编号和名称
    public void showDeviceInfo() {
        System.out.println("设备编号：" + deviceId + "，设备名称：" + deviceName + "，设备状态：" + status);
    }

    //让每台设备执行自己的主要任务
    public abstract void execute();
}
