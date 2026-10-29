package com.cyberzen.smartdevice.test;

import com.cyberzen.smartdevice.capability.Rechargeable;
import com.cyberzen.smartdevice.model.EdgeVisionDevice;
import com.cyberzen.smartdevice.model.InspectionRobot;
import com.cyberzen.smartdevice.model.SmartDevice;
import com.cyberzen.smartdevice.model.VoiceInteractionDevice;

import java.util.ArrayList;

public class SmartDeviceTest {
    public static void main(String[] args) {
        //创建一台边缘视觉设备；
        EdgeVisionDevice edgeVisionDevice = new EdgeVisionDevice("Vision-001", "边缘视觉设备");

        //创建一台巡检机器人；

        InspectionRobot inspectionRobot = new InspectionRobot("Robot-001", "园区巡检机器人");


        //创建一台语音交互设备；
        VoiceInteractionDevice voiceInteractionDevice = new VoiceInteractionDevice("Voice-001", "智能语音设备");

        //创建一个集合保存所有智能设备
        ArrayList<SmartDevice> devices = new ArrayList<>();
        //添加一台边缘视觉设备；
        devices.add(edgeVisionDevice);
        //添加一台巡检机器人；
        devices.add(inspectionRobot);

        //添加一台语音交互设备；
        devices.add(voiceInteractionDevice);



        //使用增强for循环统一展示信息、执行任务；
        //遍历集合

        for(SmartDevice device : devices){
            System.out.println(device.showDeviceInfo());
            System.out.println(device.execute());
            device.finishTask();
            System.out.println("--------------------");

        }
        //输出设备总数。
        System.out.println("设备总数：" + devices.size());

        //用集合管理所有具备充电能力的对象
        ArrayList<Rechargeable> rechargeables = new ArrayList<>();

        //添加具备充电能力的对象
        rechargeables.add(voiceInteractionDevice);
        rechargeables.add(inspectionRobot);

        //使用增强for循环统一充电
        for(Rechargeable rechargeable : rechargeables){
            System.out.println(rechargeable.charge());
        }

    }
}
