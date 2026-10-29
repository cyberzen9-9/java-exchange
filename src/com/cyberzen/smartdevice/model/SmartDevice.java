package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.capability.Rechargeable;
import com.cyberzen.smartdevice.enums.DeviceStatus;
import com.cyberzen.smartdevice.enums.DeviceType;
import com.cyberzen.smartdevice.exception.IllegalDeviceOperationException;

public abstract class SmartDevice {
//    唯一的设备编号；
//    设备名称。
    private final String deviceId;
    private String deviceName;
    private DeviceStatus status;



    public SmartDevice(String deviceID, String deviceName) {
        this.deviceId = requireText(deviceID, "设备编号");
        this.deviceName = requireText(deviceName, "设备名称");
        this.status = DeviceStatus.IDLE;
    }

    public final String getDeviceId() {
        return deviceId;
    }

    public final String getDeviceName() {
        return deviceName;
    }

    public final void setDeviceName(String deviceName) {
        this.deviceName = requireText(deviceName, "设备名称");
    }

    public final DeviceStatus getStatus() {
        return status;
    }



    //展示每台设备的编号和名称
    public final String showDeviceInfo() {
        return "设备编号：" + deviceId + "，设备名称：" + deviceName + "，设备类型："
                + getDeviceType().getDisplayName() + "，设备状态：" + status;
    }

    //让每台设备执行自己的主要任务
    public final String execute() {
        requireStatus(DeviceStatus.IDLE, "开始任务");
        String description = taskDescription();
        status = DeviceStatus.RUNNING;
        return description;
    }

    public final void finishTask() {
        requireStatus(DeviceStatus.RUNNING, "结束任务");
        status = DeviceStatus.IDLE;
    }

    public final void goOffline() {
        requireStatus(DeviceStatus.IDLE, "离线");
        status = DeviceStatus.OFFLINE;
    }

    public final void goOnline() {
        requireStatus(DeviceStatus.OFFLINE, "上线");
        status = DeviceStatus.IDLE;
    }

    public final void startCharging() {
        requireRechargeable();
        requireStatus(DeviceStatus.IDLE, "开始充电");
        status = DeviceStatus.CHARGING;
    }

    public final void finishCharging() {
        requireRechargeable();
        requireStatus(DeviceStatus.CHARGING, "结束充电");
        status = DeviceStatus.IDLE;
    }

    public abstract DeviceType getDeviceType();

    // 包内的多态行为只描述任务；公开 execute 统一负责状态转换。
    abstract String taskDescription();

    private void requireRechargeable() {
        if (!(this instanceof Rechargeable)) {
            throw new IllegalDeviceOperationException("设备 " + deviceId + " 不支持充电");
        }
    }

    private void requireStatus(DeviceStatus expected, String action) {
        if (status != expected) {
            throw new IllegalDeviceOperationException(action + "要求状态为 " + expected + "，当前为 " + status);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value;
    }
}
