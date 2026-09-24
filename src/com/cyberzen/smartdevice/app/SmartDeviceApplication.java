package com.cyberzen.smartdevice.app;

import com.cyberzen.smartdevice.model.EdgeVisionDevice;
import com.cyberzen.smartdevice.model.InspectionRobot;
import com.cyberzen.smartdevice.model.SmartDevice;
import com.cyberzen.smartdevice.service.DeviceService;
import com.cyberzen.smartdevice.store.DeviceStore;
import com.cyberzen.smartdevice.store.InMemoryDeviceStore;



public class SmartDeviceApplication {
    public static void main(String[] args) {
        DeviceStore deviceStore = new InMemoryDeviceStore();

        DeviceService deviceService = new DeviceService(deviceStore);

        //F1 注册设备
        deviceService.registerDevice(new EdgeVisionDevice("Vision-1001", "边缘视觉设备"));
        deviceService.registerDevice(new InspectionRobot("Robot-1001", "巡检机器人"));
//        deviceService.registerDevice(new EdgeVisionDevice("Vision-1001", "边缘视觉设备"));
//        deviceService.registerDevice(new InspectionRobot("", "巡检机器人"));
        //F2 查询设备
        SmartDevice device = deviceService.findDeviceById("Vision-1001");
        //测试边界条件
//        SmartDevice device1 = deviceService.findDeviceById(" ");

//        SmartDevice device2 = deviceService.findDeviceById(null);

//        SmartDevice device3 = deviceService.findDeviceById("");
        SmartDevice device1 = deviceService.findDeviceById("Vision=2333");


        //F2 查询到设备后显示设备信息
        device.showDeviceInfo();
        System.out.println("-------------------------------------------------");
        //F2 查询所有设备
        for (SmartDevice item : deviceService.findAllDevices()){
            //遍历查询到的所有设备
            item.showDeviceInfo();

        }


    }
    
}
