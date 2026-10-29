package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.capability.Rechargeable;
import com.cyberzen.smartdevice.enums.DeviceType;

public final class InspectionRobot extends SmartDevice implements Rechargeable {

    public InspectionRobot(String deviceID, String deviceName) {
        super(deviceID, deviceName);
    }

    @Override
    String taskDescription() {
        return "正在执行区域巡检任务";
    }

    //重写充电方法
    @Override
    public String charge() {
        startCharging();
        return "巡检机器人正在充电";
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.INSPECTION_ROBOT;
    }
}
