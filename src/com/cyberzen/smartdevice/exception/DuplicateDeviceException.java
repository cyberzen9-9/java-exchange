package com.cyberzen.smartdevice.exception;

public class DuplicateDeviceException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public DuplicateDeviceException(String deviceId) {
        super("设备 " + deviceId + " 已存在");
    }
}
