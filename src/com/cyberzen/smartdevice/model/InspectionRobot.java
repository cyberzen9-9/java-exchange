package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.capability.Rechargeable;

public class InspectionRobot extends SmartDevice implements Rechargeable {

    public InspectionRobot(String deviceID, String deviceName) {
        super(deviceID, deviceName);
    }

    @Override
    public void execute() {
        System.out.println("正在执行区域巡检任务");
    }

    //重写充电方法
    @Override
    public void charge() {
        System.out.println("巡检机器人正在充电");
    }
}
