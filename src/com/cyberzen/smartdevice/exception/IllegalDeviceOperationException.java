package com.cyberzen.smartdevice.exception;

/** 设备能力或当前状态不允许执行该操作。 */
public class IllegalDeviceOperationException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public IllegalDeviceOperationException(String message) {
        super(message);
    }
}
