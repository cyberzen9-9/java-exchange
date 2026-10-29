package com.cyberzen.smartdevice.exception;

public class DeviceNotFoundException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public DeviceNotFoundException(String deviceId) {
        super("设备 " + deviceId + " 不存在");
    }
}
