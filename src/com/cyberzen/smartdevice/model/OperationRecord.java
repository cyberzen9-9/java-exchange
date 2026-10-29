package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.enums.OperationAction;
import java.time.LocalDateTime;

/** 只保存不可变的值，不保留设备引用。details 保存当时的操作参数。 */
public record OperationRecord(String deviceId, OperationAction action, LocalDateTime time,
                              boolean success, String failureReason, String details) {
}
