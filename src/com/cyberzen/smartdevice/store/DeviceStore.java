package com.cyberzen.smartdevice.store;

import com.cyberzen.smartdevice.model.SmartDevice;

import java.util.List;

public interface DeviceStore {
    //负责规定：
    //设备数据应该支持哪些存取操作。
    /*可以保存；
    可以查询单个设备；
    可以查询全部设备；
    可以删除*/
    void save(SmartDevice device);
    SmartDevice findById(String deviceId);
    List<SmartDevice> findAll();
    void deleteById(String deviceId);
}
