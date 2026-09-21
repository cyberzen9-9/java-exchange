package com.cyberzen.smartdevice.service;

import com.cyberzen.smartdevice.model.SmartDevice;
import com.cyberzen.smartdevice.store.DeviceStore;

import java.util.List;

public class DeviceService {

    private DeviceStore deviceStore;

    public DeviceService(DeviceStore deviceStore) {

        this.deviceStore = deviceStore;
    }


    //F1 注册设备
    public void registerDevice(SmartDevice device) {
        //判断设备是否为空或者已存在
        //设备本身不能为空
        if (device == null){
            throw new IllegalArgumentException("设备不能为空!");
        }
        if (device.getDeviceId() == null ||
                device.getDeviceId().isBlank()){
            throw new IllegalArgumentException("设备编号不能为空!");
        }

        if (deviceStore.findById(device.getDeviceId()) != null){
            throw new IllegalArgumentException("设备已存在!");
        }

        deviceStore.save(device);


    }
    //F2 查询设备 findDeviceById()、findAllDevices()
    public SmartDevice findDeviceById(String deviceId){
        return deviceStore.findById(deviceId);
    }
    public List<SmartDevice> findAllDevices(){
        return deviceStore.findAll();
    }

}
