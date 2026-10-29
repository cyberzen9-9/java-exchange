package com.cyberzen.smartdevice.app;

import com.cyberzen.smartdevice.enums.DeviceStatus;
import com.cyberzen.smartdevice.enums.DeviceType;
import com.cyberzen.smartdevice.exception.IllegalDeviceOperationException;
import com.cyberzen.smartdevice.model.DeviceStatistics;
import com.cyberzen.smartdevice.model.OperationRecord;
import com.cyberzen.smartdevice.model.SmartDevice;
import com.cyberzen.smartdevice.service.DeviceService;
import com.cyberzen.smartdevice.store.InMemoryDeviceStore;

import java.io.EOFException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class SmartDeviceApplication {
    private final DeviceService service;
    private final Scanner input;
    private final PrintStream output;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SmartDeviceApplication(DeviceService service, Scanner input, PrintStream output) {
        if (service == null || input == null || output == null) {
            throw new IllegalArgumentException("Service、输入和输出不能为空");
        }
        this.service = service;
        this.input = input;
        this.output = output;
    }

    public static void main(String[] args) {
        DeviceService service = new DeviceService(new InMemoryDeviceStore());
        new SmartDeviceApplication(service, new Scanner(System.in, StandardCharsets.UTF_8), System.out).run();
    }

    public void run() {
        output.println("智能设备与任务管理工具（内存模拟，重启后数据丢失）");
        try {
            while (true) {
                showMenu();
                try {
                    int option = Integer.parseInt(readLine("请选择：").trim());
                    if (option == 0) {
                        output.println("已退出。");
                        return;
                    }
                    handle(option);
                } catch (NumberFormatException exception) {
                    output.println("输入错误：请输入数字菜单选项。");
                } catch (IllegalArgumentException | IllegalDeviceOperationException exception) {
                    output.println("操作失败：" + exception.getMessage());
                }
            }
        } catch (EOFException exception) {
            output.println("输入已结束，正常退出。");
        }
    }

    private void showMenu() {
        output.println();
        output.println("1 注册  2 按编号查询  3 查询全部  4 修改名称");
        output.println("5 开始任务  6 结束任务  7 开始充电  8 结束充电");
        output.println("9 离线  10 上线  11 删除  12 按类型筛选");
        output.println("13 按状态筛选  14 统计  15 查看记录  0 退出");
    }

    private void handle(int option) throws EOFException {
        switch (option) {
            case 1 -> {
                DeviceType type = readType();
                String id = readLine("设备编号：");
                String name = readLine("设备名称：");
                service.registerDevice(type, id, name);
                output.println("注册成功。");
            }
            case 2 -> output.println(service.findDeviceById(readId()).showDeviceInfo());
            case 3 -> showDevices(service.findAllDevices());
            case 4 -> {
                String id = readId();
                String name = readLine("新名称：");
                service.renameDevice(id, name);
                output.println("名称修改成功。");
            }
            case 5 -> output.println(service.startTask(readId()));
            case 6 -> {
                service.finishTask(readId());
                output.println("任务已结束，设备空闲。");
            }
            case 7 -> output.println(service.startCharging(readId()));
            case 8 -> {
                service.finishCharging(readId());
                output.println("充电已结束，设备空闲。");
            }
            case 9 -> {
                service.goOffline(readId());
                output.println("设备已离线。");
            }
            case 10 -> {
                service.goOnline(readId());
                output.println("设备已上线，状态为 IDLE。");
            }
            case 11 -> {
                service.deleteDevice(readId());
                output.println("已从管理系统移除；此操作不关闭真实硬件。");
            }
            case 12 -> showDevices(service.filterByType(readType()));
            case 13 -> showDevices(service.filterByStatus(readStatus()));
            case 14 -> showStatistics();
            case 15 -> showRecords();
            default -> output.println("无效菜单选项，请输入 0～15。");
        }
    }

    private String readId() throws EOFException {
        return readLine("设备编号：");
    }

    private DeviceType readType() throws EOFException {
        DeviceType[] values = DeviceType.values();
        for (int i = 0; i < values.length; i++) {
            output.println((i + 1) + " " + values[i].getDisplayName());
        }
        int choice = Integer.parseInt(readLine("类型序号：").trim());
        // 越界值交给 Service 作为非法参数处理并记录。
        return choice >= 1 && choice <= values.length ? values[choice - 1] : null;
    }

    private DeviceStatus readStatus() throws EOFException {
        DeviceStatus[] values = DeviceStatus.values();
        for (int i = 0; i < values.length; i++) {
            output.println((i + 1) + " " + values[i]);
        }
        int choice = Integer.parseInt(readLine("状态序号：").trim());
        return choice >= 1 && choice <= values.length ? values[choice - 1] : null;
    }

    private String readLine(String prompt) throws EOFException {
        output.print(prompt);
        output.flush();
        if (!input.hasNextLine()) {
            throw new EOFException("输入已结束");
        }
        return input.nextLine();
    }

    private void showDevices(List<SmartDevice> devices) {
        if (devices.isEmpty()) {
            output.println("没有匹配的设备。");
            return;
        }
        for (SmartDevice device : devices) {
            output.println(device.showDeviceInfo());
        }
    }

    private void showStatistics() {
        DeviceStatistics statistics = service.getStatistics();
        output.println("设备总数：" + statistics.total());
        for (DeviceType type : DeviceType.values()) {
            output.println(type.getDisplayName() + "：" + statistics.byType().get(type));
        }
        for (DeviceStatus status : DeviceStatus.values()) {
            output.println(status + "：" + statistics.byStatus().get(status));
        }
    }

    private void showRecords() {
        for (OperationRecord record : service.findAllRecords()) {
            output.println(record.time().format(TIME_FORMAT) + " | 编号=" + record.deviceId()
                    + " | " + record.action() + " | " + (record.success() ? "成功" : "失败")
                    + " | 原因=" + record.failureReason() + " | " + record.details());
        }
    }
}
