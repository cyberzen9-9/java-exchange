package com.cyberzen.smartdevice.capability;
//给所有“可充电设备”定义一个公共的充电接口
public interface Rechargeable {
    /** 开始充电；实现必须走设备自身的状态检查。返回值供调用方展示。 */
    String charge();
}
