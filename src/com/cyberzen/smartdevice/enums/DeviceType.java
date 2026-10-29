package com.cyberzen.smartdevice.enums;

public enum DeviceType {
    EDGE_VISION("边缘视觉设备"),
    INSPECTION_ROBOT("巡检机器人"),
    VOICE_INTERACTION("语音交互设备");

    private final String displayName;

    DeviceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
