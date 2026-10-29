package com.cyberzen.smartdevice.store;

import com.cyberzen.smartdevice.model.SmartDevice;
import com.cyberzen.smartdevice.enums.DeviceStatus;
import com.cyberzen.smartdevice.exception.DeviceNotFoundException;
import com.cyberzen.smartdevice.exception.DuplicateDeviceException;
import com.cyberzen.smartdevice.exception.IllegalDeviceOperationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryDeviceStore implements DeviceStore{
    // 设备存储

    private final Map<String, SmartDevice> devices = new LinkedHashMap<>();


    @Override
    // 只新增设备；LinkedHashMap 让菜单展示保持注册顺序。
    public void save(SmartDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("设备不能为空");
        }
        requireId(device.getDeviceId());
        if (device.getDeviceName() == null || device.getDeviceName().isBlank()) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
        if (devices.putIfAbsent(device.getDeviceId(), device) != null) {
            throw new DuplicateDeviceException(device.getDeviceId());
        }
    }

    @Override
    //根据设备ID查找设备
    public SmartDevice findById(String deviceId) {
        requireId(deviceId);
        return devices.get(deviceId);
    }

    @Override
    //查找所有设备
    public List<SmartDevice> findAll() {
        return List.copyOf(devices.values());

    }

    @Override
    public void deleteById(String deviceId) {
        requireId(deviceId);
        SmartDevice device = devices.get(deviceId);
        if (device == null) {
            throw new DeviceNotFoundException(deviceId);
        }
        // 仓库公开删除入口也防守，避免跳过 Service 删除运行中的设备。
        if (device.getStatus() == DeviceStatus.RUNNING) {
            throw new IllegalDeviceOperationException("运行中的设备不能删除");
        }
        devices.remove(deviceId);

    }

    private static void requireId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
    }
}
