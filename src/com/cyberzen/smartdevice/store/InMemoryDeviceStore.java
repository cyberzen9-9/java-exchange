package com.cyberzen.smartdevice.store;

import com.cyberzen.smartdevice.model.SmartDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryDeviceStore implements DeviceStore{
    // 设备存储

    private final Map<String, SmartDevice> devices = new HashMap<>();


    @Override
    //可以保存设备,实际上保存到内存中（HashMap）
    public void save(SmartDevice device) {
        devices.put(device.getDeviceId(),device);
    }

    @Override
    //根据设备ID查找设备
    public SmartDevice findById(String deviceId) {
        return devices.get(deviceId);
    }

    @Override
    //查找所有设备
    public List<SmartDevice> findAll() {
        return new ArrayList<>(devices.values());

    }

    @Override
    public void deleteById(String deviceId) {
        devices.remove(deviceId);

    }
}
