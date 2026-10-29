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
    /** 仅新增；重复编号抛异常，不能覆盖。 */
    void save(SmartDevice device);
    /** 非空编号未命中时返回 null，由 Service 转为业务异常。 */
    SmartDevice findById(String deviceId);
    /** 返回独立、不可修改的列表；设备元素仍为原对象引用。 */
    List<SmartDevice> findAll();
    /** 不存在或运行中的设备不能删除；其余状态只移除引用。 */
    void deleteById(String deviceId);
}
