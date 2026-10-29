package com.cyberzen.smartdevice.service;

import com.cyberzen.smartdevice.capability.Rechargeable;
import com.cyberzen.smartdevice.enums.DeviceStatus;
import com.cyberzen.smartdevice.enums.DeviceType;
import com.cyberzen.smartdevice.enums.OperationAction;
import com.cyberzen.smartdevice.exception.DeviceNotFoundException;
import com.cyberzen.smartdevice.exception.DuplicateDeviceException;
import com.cyberzen.smartdevice.exception.IllegalDeviceOperationException;
import com.cyberzen.smartdevice.model.*;
import com.cyberzen.smartdevice.store.DeviceStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

public class DeviceService {
    private final DeviceStore deviceStore;
    private final List<OperationRecord> records = new ArrayList<>();

    public DeviceService(DeviceStore deviceStore) {
        if (deviceStore == null) {
            throw new IllegalArgumentException("设备仓库不能为空");
        }
        this.deviceStore = deviceStore;
    }

    public void registerDevice(SmartDevice device) {
        String id = device == null ? null : device.getDeviceId();
        String details = device == null ? "设备=null" : "名称=" + device.getDeviceName();
        logged(OperationAction.REGISTER, id, details, () -> {
            register(device);
            return null;
        });
    }

    /** 菜单使用此入口，确保非法编号/名称/类型在构造阶段失败也有记录。 */
    public void registerDevice(DeviceType type, String deviceId, String name) {
        logged(OperationAction.REGISTER, deviceId, "类型=" + type + "，名称=" + name, () -> {
            if (type == null) {
                throw new IllegalArgumentException("设备类型不能为空");
            }
            SmartDevice device = switch (type) {
                case EDGE_VISION -> new EdgeVisionDevice(deviceId, name);
                case INSPECTION_ROBOT -> new InspectionRobot(deviceId, name);
                case VOICE_INTERACTION -> new VoiceInteractionDevice(deviceId, name);
            };
            register(device);
            return null;
        });
    }

    public SmartDevice findDeviceById(String deviceId) {
        return logged(OperationAction.FIND_BY_ID, deviceId, "", () -> requireDevice(deviceId));
    }

    public List<SmartDevice> findAllDevices() {
        return logged(OperationAction.FIND_ALL, null, "", () -> List.copyOf(deviceStore.findAll()));
    }

    public void renameDevice(String deviceId, String newName) {
        logged(OperationAction.RENAME, deviceId, "新名称=" + newName, () -> {
            requireDevice(deviceId).setDeviceName(newName);
            return null;
        });
    }

    public String startTask(String deviceId) {
        return logged(OperationAction.START_TASK, deviceId, "", () -> requireDevice(deviceId).execute());
    }

    public void finishTask(String deviceId) {
        logged(OperationAction.FINISH_TASK, deviceId, "", () -> {
            requireDevice(deviceId).finishTask();
            return null;
        });
    }

    public String startCharging(String deviceId) {
        return logged(OperationAction.START_CHARGING, deviceId, "", () -> {
            SmartDevice device = requireDevice(deviceId);
            if (!(device instanceof Rechargeable rechargeable)) {
                throw new IllegalDeviceOperationException("设备 " + deviceId + " 不支持充电");
            }
            return rechargeable.charge();
        });
    }

    public void finishCharging(String deviceId) {
        logged(OperationAction.FINISH_CHARGING, deviceId, "", () -> {
            requireDevice(deviceId).finishCharging();
            return null;
        });
    }

    public void goOffline(String deviceId) {
        logged(OperationAction.GO_OFFLINE, deviceId, "", () -> {
            requireDevice(deviceId).goOffline();
            return null;
        });
    }

    public void goOnline(String deviceId) {
        logged(OperationAction.GO_ONLINE, deviceId, "", () -> {
            requireDevice(deviceId).goOnline();
            return null;
        });
    }

    public void deleteDevice(String deviceId) {
        logged(OperationAction.DELETE, deviceId, "仅从管理系统移除", () -> {
            SmartDevice device = requireDevice(deviceId);
            if (device.getStatus() == DeviceStatus.RUNNING) {
                throw new IllegalDeviceOperationException("运行中的设备不能删除");
            }
            deviceStore.deleteById(deviceId);
            return null;
        });
    }

    public List<SmartDevice> filterByType(DeviceType type) {
        return logged(OperationAction.FILTER_BY_TYPE, null, "类型=" + type, () -> {
            if (type == null) {
                throw new IllegalArgumentException("筛选类型不能为空");
            }
            return deviceStore.findAll().stream().filter(device -> device.getDeviceType() == type).toList();
        });
    }

    public List<SmartDevice> filterByStatus(DeviceStatus status) {
        return logged(OperationAction.FILTER_BY_STATUS, null, "状态=" + status, () -> {
            if (status == null) {
                throw new IllegalArgumentException("筛选状态不能为空");
            }
            return deviceStore.findAll().stream().filter(device -> device.getStatus() == status).toList();
        });
    }

    public DeviceStatistics getStatistics() {
        return logged(OperationAction.STATISTICS, null, "", () -> {
            List<SmartDevice> devices = deviceStore.findAll();
            EnumMap<DeviceType, Long> byType = new EnumMap<>(DeviceType.class);
            EnumMap<DeviceStatus, Long> byStatus = new EnumMap<>(DeviceStatus.class);
            for (DeviceType type : DeviceType.values()) {
                byType.put(type, 0L);
            }
            for (DeviceStatus status : DeviceStatus.values()) {
                byStatus.put(status, 0L);
            }
            for (SmartDevice device : devices) {
                byType.merge(device.getDeviceType(), 1L, Long::sum);
                byStatus.merge(device.getStatus(), 1L, Long::sum);
            }
            return new DeviceStatistics(devices.size(), byType, byStatus);
        });
    }

    /** 本次查看也记录一次；返回值包含本次查看，旧列表不会随之后续操作增长。 */
    public List<OperationRecord> findAllRecords() {
        records.add(new OperationRecord(null, OperationAction.VIEW_RECORDS, LocalDateTime.now(), true, "", ""));
        return List.copyOf(records);
    }

    private void register(SmartDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("设备不能为空");
        }
        requireId(device.getDeviceId());
        if (device.getDeviceName() == null || device.getDeviceName().isBlank()) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
        if (deviceStore.findById(device.getDeviceId()) != null) {
            throw new DuplicateDeviceException(device.getDeviceId());
        }
        deviceStore.save(device);
    }

    private SmartDevice requireDevice(String deviceId) {
        requireId(deviceId);
        SmartDevice device = deviceStore.findById(deviceId);
        if (device == null) {
            throw new DeviceNotFoundException(deviceId);
        }
        return device;
    }

    private static void requireId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
    }

    // 每个业务调用只记录一次；内部使用私有查询，避免额外产生 FIND_BY_ID 记录。
    private <T> T logged(OperationAction action, String deviceId, String details, Supplier<T> operation) {
        try {
            T result = operation.get();
            records.add(new OperationRecord(deviceId, action, LocalDateTime.now(), true, "", details));
            return result;
        } catch (RuntimeException exception) {
            String reason = exception.getMessage();
            if (reason == null || reason.isBlank()) {
                reason = exception.getClass().getSimpleName();
            }
            records.add(new OperationRecord(deviceId, action, LocalDateTime.now(), false, reason, details));
            throw exception;
        }
    }
}
