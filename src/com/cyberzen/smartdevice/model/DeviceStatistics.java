package com.cyberzen.smartdevice.model;

import com.cyberzen.smartdevice.enums.DeviceStatus;
import com.cyberzen.smartdevice.enums.DeviceType;
import java.util.Map;

/** 独立、不可变的统计结果，保留数量为 0 的枚举项。 */
public record DeviceStatistics(int total, Map<DeviceType, Long> byType, Map<DeviceStatus, Long> byStatus) {
    public DeviceStatistics {
        byType = Map.copyOf(byType);
        byStatus = Map.copyOf(byStatus);
    }
}
