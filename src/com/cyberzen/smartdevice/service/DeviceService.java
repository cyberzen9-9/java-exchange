package com.cyberzen.smartdevice.service;

import com.cyberzen.smartdevice.model.SmartDevice;
import com.cyberzen.smartdevice.store.DeviceStore;

import java.util.List;

public class DeviceService {

    private final DeviceStore deviceStore;

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
        String deviceId = device.getDeviceId();
        if (deviceId == null ||
                deviceId.isBlank()){
            throw new IllegalArgumentException("设备编号不能为空!");
        }

        if (deviceStore.findById(deviceId) != null){
            throw new IllegalArgumentException("设备已存在!");
        }

        deviceStore.save(device);


    }
    //F2 查询设备 findDeviceById()、findAllDevices()
    public SmartDevice findDeviceById(String deviceId){
        //设备编号不能为空
        if (deviceId == null || deviceId.isBlank()){
            throw new IllegalArgumentException("查询设备编号不能为空!");
        }

        SmartDevice device = deviceStore.findById(deviceId);

        //查询到的设备要存在
        if (device == null){
            throw  new IllegalArgumentException("设备" + deviceId + "不存在!");
        }


        return device;
    }
    public List<SmartDevice> findAllDevices(){
        return deviceStore.findAll();
    }

}
