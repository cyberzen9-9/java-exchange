package com.cyberzen.smartdevice.test;

import com.cyberzen.smartdevice.capability.Rechargeable;
import com.cyberzen.smartdevice.app.SmartDeviceApplication;
import com.cyberzen.smartdevice.enums.DeviceStatus;
import com.cyberzen.smartdevice.enums.DeviceType;
import com.cyberzen.smartdevice.enums.OperationAction;
import com.cyberzen.smartdevice.exception.DeviceNotFoundException;
import com.cyberzen.smartdevice.exception.DuplicateDeviceException;
import com.cyberzen.smartdevice.exception.IllegalDeviceOperationException;
import com.cyberzen.smartdevice.model.*;
import com.cyberzen.smartdevice.service.DeviceService;
import com.cyberzen.smartdevice.store.InMemoryDeviceStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** 独立、无第三方依赖的验收入口；失败会抛 AssertionError 并以非零码退出。 */
public final class SmartDeviceAcceptanceTest {
    private static int checks;

    public static void main(String[] args) {
        modelRules();
        serviceAndStoreRules();
        chargingServiceRules();
        auditRules();
        filterAndStatisticsRules();
        consoleRules();
        System.out.println("PASS: " + checks + " checks");
    }

    private static void chargingServiceRules() {
        for (int type = 0; type < 3; type++) {
            DeviceService service = new DeviceService(new InMemoryDeviceStore());
            SmartDevice device = newDevice(type);
            String id = device.getDeviceId();
            service.registerDevice(device);
            if (!(device instanceof Rechargeable)) {
                expect(IllegalDeviceOperationException.class, () -> service.startCharging(id));
                expect(IllegalDeviceOperationException.class, () -> service.finishCharging(id));
                check(device.getStatus() == DeviceStatus.IDLE, "Service 视觉设备充电失败保持空闲");
            } else {
                check(service.startCharging(id).contains("充电"), "两种可充电设备均返回具体充电行为");
                expect(IllegalDeviceOperationException.class, () -> service.startCharging(id));
                expect(IllegalDeviceOperationException.class, () -> service.startTask(id));
                check(device.getStatus() == DeviceStatus.CHARGING, "拒绝操作后继续充电");
                service.finishCharging(id);
                check(device.getStatus() == DeviceStatus.IDLE, "两种设备充电完成回到空闲");
                expect(IllegalDeviceOperationException.class, () -> service.finishCharging(id));
            }
            service.goOffline(id);
            expect(IllegalDeviceOperationException.class, () -> service.startCharging(id));
            check(device.getStatus() == DeviceStatus.OFFLINE, "离线充电被拒绝后仍离线");
        }
    }

    private static void filterAndStatisticsRules() {
        DeviceService service = new DeviceService(new InMemoryDeviceStore());
        DeviceStatistics empty = service.getStatistics();
        check(empty.total() == 0, "空统计总数为零");
        for (DeviceType type : DeviceType.values()) {
            check(service.filterByType(type).isEmpty() && empty.byType().get(type) == 0, "空类型筛选及零计数");
        }
        for (DeviceStatus status : DeviceStatus.values()) {
            check(service.filterByStatus(status).isEmpty() && empty.byStatus().get(status) == 0, "空状态筛选及零计数");
        }
        expect(IllegalArgumentException.class, () -> service.filterByType(null));
        expect(IllegalArgumentException.class, () -> service.filterByStatus(null));
        service.registerDevice(DeviceType.EDGE_VISION, "E1", "视觉一");
        service.registerDevice(DeviceType.EDGE_VISION, "E2", "视觉二");
        service.registerDevice(DeviceType.INSPECTION_ROBOT, "R1", "巡检一");
        service.registerDevice(DeviceType.INSPECTION_ROBOT, "R2", "巡检二");
        service.registerDevice(DeviceType.VOICE_INTERACTION, "V1", "语音一");
        check(service.filterByStatus(DeviceStatus.OFFLINE).isEmpty(), "有设备但无匹配状态");
        service.startTask("E2");
        service.startCharging("R1");
        service.goOffline("R2");
        DeviceStatistics statistics = service.getStatistics();
        check(statistics.total() == 5, "混合数据总数正确");
        check(statistics.byType().get(DeviceType.EDGE_VISION) == 2
                && statistics.byType().get(DeviceType.INSPECTION_ROBOT) == 2
                && statistics.byType().get(DeviceType.VOICE_INTERACTION) == 1, "所有类型计数正确");
        for (DeviceStatus status : DeviceStatus.values()) {
            check(statistics.byStatus().get(status) == (status == DeviceStatus.IDLE ? 2 : 1), "各状态计数正确");
            check(service.filterByStatus(status).size() == statistics.byStatus().get(status), "状态筛选与统计一致");
        }
        for (DeviceType type : DeviceType.values()) {
            List<SmartDevice> filtered = service.filterByType(type);
            check(filtered.size() == statistics.byType().get(type), "类型筛选与统计一致");
            check(filtered.stream().allMatch(device -> device.getDeviceType() == type), "筛选元素正确");
            expect(UnsupportedOperationException.class, () -> filtered.clear(), false);
        }
        expect(UnsupportedOperationException.class, () -> statistics.byType().clear(), false);
        expect(UnsupportedOperationException.class, () -> statistics.byStatus().clear(), false);
        service.deleteDevice("V1");
        check(service.filterByType(DeviceType.VOICE_INTERACTION).isEmpty(), "删除后无匹配类型");
        check(statistics.total() == 5 && empty.total() == 0, "统计结果不随设备变化");
        List<OperationRecord> records = service.findAllRecords();
        check(records.stream().anyMatch(r -> r.action() == OperationAction.FILTER_BY_TYPE && !r.success()
                && !r.failureReason().isBlank()), "非法类型筛选记录失败原因");
        check(records.stream().anyMatch(r -> r.action() == OperationAction.FILTER_BY_STATUS && !r.success()
                && !r.failureReason().isBlank()), "非法状态筛选记录失败原因");
        check(records.stream().anyMatch(r -> r.action() == OperationAction.STATISTICS && r.success()), "统计有记录");
    }

    private static void consoleRules() {
        String script = String.join("\n", "abc", "99", "2147483648", "", "2", "missing", "2", " ",
                "1", "2", "R", " ", "1", "2", "R", "巡检", "4", "R", " ",
                "4", "R", "新巡检", "2", "R", "3", "5", "R", "5", "R", "7", "R", "11", "R",
                "6", "R", "7", "R", "5", "R", "8", "R", "9", "R", "7", "R", "10", "R",
                "12", "2", "12", "1", "13", "1", "12", "9", "13", "9", "13", "abc",
                "14", "15", "11", "R", "14", "15", "0") + "\n";
        DeviceService service = new DeviceService(new InMemoryDeviceStore());
        String output = runConsole(service, script);
        for (String message : List.of("请输入数字菜单选项", "无效菜单选项", "不存在", "不能为空",
                "注册成功", "名称修改成功", "新巡检", "正在执行区域巡检任务", "要求状态为 IDLE",
                "运行中的设备不能删除", "任务已结束", "巡检机器人正在充电", "充电已结束",
                "设备已离线", "设备已上线", "没有匹配的设备", "设备总数：1", "设备总数：0",
                "已从管理系统移除", "VIEW_RECORDS", "已退出")) {
            check(output.contains(message), "控制台输出包含：" + message);
        }
        check(service.findAllDevices().isEmpty(), "控制台完成真实业务删除");
        List<OperationRecord> records = service.findAllRecords();
        for (OperationAction action : OperationAction.values()) {
            check(records.stream().anyMatch(r -> r.action() == action && r.success()), "菜单覆盖成功动作 " + action);
        }
        for (OperationRecord record : records) {
            check(record.success() || !record.failureReason().isBlank(), "菜单失败记录有原因");
        }
        // 逐个截断注册输入：顶层和每个字段的 EOF 都必须退出，不能执行半次注册。
        String[] fields = {"1", "2", "R", "巡检"};
        for (int length = 0; length <= fields.length; length++) {
            DeviceService fresh = new DeviceService(new InMemoryDeviceStore());
            String prefix = String.join("\n", java.util.Arrays.copyOf(fields, length));
            String result = runConsole(fresh, prefix.isEmpty() ? "" : prefix + "\n");
            check(result.contains("输入已结束，正常退出"), "注册输入截断后退出 " + length);
            check(fresh.findAllDevices().size() == (length == fields.length ? 1 : 0), "未完成输入不产生注册");
        }
        for (String scriptEnd : List.of("2\n", "4\nR\n", "5\n", "12\n", "13\n")) {
            check(runConsole(new DeviceService(new InMemoryDeviceStore()), scriptEnd)
                    .contains("输入已结束，正常退出"), "其他菜单字段 EOF 安全退出");
        }
    }

    private static String runConsole(DeviceService service, String script) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Scanner input = new Scanner(script);
             PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new SmartDeviceApplication(service, input, output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void serviceAndStoreRules() {
        expect(IllegalArgumentException.class, () -> new DeviceService(null));
        InMemoryDeviceStore store = new InMemoryDeviceStore();
        DeviceService service = new DeviceService(store);
        check(service.findAllDevices().isEmpty(), "空仓库返回空集合");
        expect(IllegalArgumentException.class, () -> service.registerDevice((SmartDevice) null));
        expect(IllegalArgumentException.class, () -> store.save(null));
        for (int type = 0; type < 3; type++) {
            SmartDevice device = newDevice(type);
            service.registerDevice(device);
            String id = device.getDeviceId();
            check(service.findDeviceById(id) == device, "原查询 API 保留设备引用");
            service.renameDevice(id, "新" + id);
            check(device.getDeviceName().equals("新" + id), "三种设备均能改名");
            service.startTask(id);
            expect(IllegalDeviceOperationException.class, () -> service.startTask(id));
            expect(IllegalDeviceOperationException.class, () -> service.deleteDevice(id));
            expect(IllegalDeviceOperationException.class, () -> store.deleteById(id));
            check(store.findById(id) == device && device.getStatus() == DeviceStatus.RUNNING,
                    "删除失败保留仓库和状态");
            service.finishTask(id);
            expect(IllegalDeviceOperationException.class, () -> service.finishTask(id));
            service.deleteDevice(id);
            check(store.findById(id) == null, "IDLE 删除生效");
        }
        for (DeviceStatus status : new DeviceStatus[]{DeviceStatus.OFFLINE, DeviceStatus.CHARGING}) {
            SmartDevice device = new InspectionRobot("R", "待删除");
            moveTo(device, status);
            service.registerDevice(device);
            service.deleteDevice("R");
            check(store.findById("R") == null && device.getStatus() == status,
                    "其余状态可删除且不关闭或改变硬件状态");
        }
        SmartDevice original = new InspectionRobot("R", "原名称");
        service.registerDevice(original);
        expect(DuplicateDeviceException.class, () -> service.registerDevice(new EdgeVisionDevice("R", "新设备")));
        expect(DuplicateDeviceException.class, () -> store.save(new EdgeVisionDevice("R", "覆盖")));
        check(store.findById("R") == original, "重复保存不覆盖原设备");
        List<SmartDevice> snapshot = service.findAllDevices();
        expect(UnsupportedOperationException.class, () -> snapshot.clear(), false);
        expect(UnsupportedOperationException.class, () -> store.findAll().clear(), false);
        expect(DeviceNotFoundException.class, () -> store.deleteById("missing"));

        List<Consumer<String>> operations = List.of(service::findDeviceById,
                id -> service.renameDevice(id, "新名称"), service::startTask, service::finishTask,
                service::startCharging, service::finishCharging, service::goOffline, service::goOnline,
                service::deleteDevice);
        List<OperationAction> actions = List.of(OperationAction.FIND_BY_ID, OperationAction.RENAME,
                OperationAction.START_TASK, OperationAction.FINISH_TASK, OperationAction.START_CHARGING,
                OperationAction.FINISH_CHARGING, OperationAction.GO_OFFLINE, OperationAction.GO_ONLINE,
                OperationAction.DELETE);
        for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
            Consumer<String> operation = operations.get(operationIndex);
            for (String invalid : new String[]{null, "", " \t"}) {
                expect(IllegalArgumentException.class, () -> operation.accept(invalid));
                List<OperationRecord> logs = service.findAllRecords();
                OperationRecord failure = logs.get(logs.size() - 2);
                check(failure.action() == actions.get(operationIndex) && !failure.success()
                        && java.util.Objects.equals(failure.deviceId(), invalid)
                        && !failure.failureReason().isBlank(), "每个 Service 动作的参数错误均记录原始编号和原因");
            }
            expect(DeviceNotFoundException.class, () -> operation.accept("missing"));
        }
        for (String invalid : new String[]{null, "", " \t"}) {
            expect(IllegalArgumentException.class, () -> service.renameDevice("R", invalid));
            check(original.getDeviceName().equals("原名称"), "Service 失败改名保留原名称");
            expect(IllegalArgumentException.class, () -> service.registerDevice(DeviceType.EDGE_VISION, invalid, "视觉"));
            expect(IllegalArgumentException.class, () -> service.registerDevice(DeviceType.EDGE_VISION, "E", invalid));
            expect(IllegalArgumentException.class, () -> store.findById(invalid));
            expect(IllegalArgumentException.class, () -> store.deleteById(invalid));
        }
        expect(IllegalArgumentException.class, () -> service.registerDevice(null, "E", "视觉"));
        check(store.findAll().size() == 1, "所有失败操作不改变仓库内容");
        service.deleteDevice("R");
        check(snapshot.size() == 1, "查询列表是独立的结构快照");
    }

    private static void auditRules() {
        DeviceService service = new DeviceService(new InMemoryDeviceStore());
        LocalDateTime before = LocalDateTime.now();
        service.registerDevice(DeviceType.INSPECTION_ROBOT, "R", "原名称");
        service.renameDevice("R", "新名称");
        expect(IllegalArgumentException.class, () -> service.renameDevice("R", " "));
        service.startTask("R");
        expect(IllegalDeviceOperationException.class, () -> service.deleteDevice("R"));
        service.finishTask("R");
        service.deleteDevice("R");
        expect(DeviceNotFoundException.class, () -> service.findDeviceById("R"));
        expect(IllegalArgumentException.class, () -> service.registerDevice((SmartDevice) null));
        expect(IllegalArgumentException.class, () -> service.registerDevice(DeviceType.EDGE_VISION, " ", "视觉"));
        List<OperationRecord> records = service.findAllRecords();
        check(records.size() == 11, "每次 Service 调用恰好一条记录，含查看本身");
        OperationAction[] actions = {OperationAction.REGISTER, OperationAction.RENAME, OperationAction.RENAME,
                OperationAction.START_TASK, OperationAction.DELETE, OperationAction.FINISH_TASK,
                OperationAction.DELETE, OperationAction.FIND_BY_ID, OperationAction.REGISTER,
                OperationAction.REGISTER, OperationAction.VIEW_RECORDS};
        for (int i = 0; i < records.size(); i++) {
            OperationRecord record = records.get(i);
            check(record.action() == actions[i], "动作与调用次序一致");
            check(record.success() == !(i == 2 || i == 4 || i == 7 || i == 8 || i == 9), "成败内容正确");
            check(!record.time().isBefore(before) && !record.time().isAfter(LocalDateTime.now()), "LocalDateTime 时间有效");
            check(record.success() ? record.failureReason().isEmpty() : !record.failureReason().isBlank(), "失败原因非空");
            check(i >= 8 || "R".equals(record.deviceId()), "历史设备编号正确");
        }
        check(records.get(0).details().contains("原名称"), "改名和删除未改变历史名称参数");
        check(records.get(8).deviceId() == null, "空对象记录允许无设备编号");
        check(" ".equals(records.get(9).deviceId()), "非法编号按原参数记录");
        expect(UnsupportedOperationException.class, () -> records.clear(), false);
        service.findAllRecords();
        check(records.size() == 11, "旧记录列表不会随新记录增长");
    }

    private static void modelRules() {
        for (String invalid : new String[]{null, "", " \t"}) {
            expect(IllegalArgumentException.class, () -> new EdgeVisionDevice(invalid, "视觉"));
            expect(IllegalArgumentException.class, () -> new InspectionRobot(invalid, "巡检"));
            expect(IllegalArgumentException.class, () -> new VoiceInteractionDevice(invalid, "语音"));
            expect(IllegalArgumentException.class, () -> new EdgeVisionDevice("E", invalid));
            expect(IllegalArgumentException.class, () -> new InspectionRobot("R", invalid));
            expect(IllegalArgumentException.class, () -> new VoiceInteractionDevice("V", invalid));
            SmartDevice device = new EdgeVisionDevice("E", "原名称");
            expect(IllegalArgumentException.class, () -> device.setDeviceName(invalid));
            check(device.getDeviceName().equals("原名称"), "改名失败保留原名称");
        }
        // 每个状态、每个动作都新建对象，避免前一个断言污染下一个。
        for (int type = 0; type < 3; type++) {
            for (DeviceStatus status : DeviceStatus.values()) {
                if (type == 0 && status == DeviceStatus.CHARGING) {
                    continue; // 视觉设备没有合法路径进入充电状态。
                }
                for (int action = 0; action < 7; action++) {
                    SmartDevice device = newDevice(type);
                    moveTo(device, status);
                    int operation = action;
                    Runnable command = () -> apply(device, operation);
                    boolean charging = action >= 4;
                    DeviceStatus required = switch (action) {
                        case 0, 2, 4, 6 -> DeviceStatus.IDLE;
                        case 1 -> DeviceStatus.RUNNING;
                        case 3 -> DeviceStatus.OFFLINE;
                        case 5 -> DeviceStatus.CHARGING;
                        default -> throw new AssertionError();
                    };
                    boolean allowed = status == required && (!charging || device instanceof Rechargeable);
                    if (allowed) {
                        command.run();
                        DeviceStatus expected = switch (action) {
                            case 0 -> DeviceStatus.RUNNING;
                            case 2 -> DeviceStatus.OFFLINE;
                            case 4, 6 -> DeviceStatus.CHARGING;
                            default -> DeviceStatus.IDLE;
                        };
                        check(device.getStatus() == expected, "合法转换 " + action);
                    } else {
                        expect(IllegalDeviceOperationException.class, command);
                        check(device.getStatus() == status, "拒绝后状态不变 " + action);
                    }
                }
            }
        }
        DeviceService service = new DeviceService(new InMemoryDeviceStore());
        service.registerDevice(new InspectionRobot("R", "巡检"));
        service.renameDevice("R", "新巡检");
        check(service.startTask("R").contains("巡检"), "保留任务多态行为");
        expect(IllegalDeviceOperationException.class, () -> service.startCharging("R"));
        service.finishTask("R");
        service.startCharging("R");
        expect(IllegalDeviceOperationException.class, () -> service.startTask("R"));
        service.finishCharging("R");
        service.goOffline("R");
        expect(IllegalDeviceOperationException.class, () -> service.startTask("R"));
        service.goOnline("R");
        check(service.findDeviceById("R").getStatus() == DeviceStatus.IDLE, "Service 完整正常路径");
    }

    private static SmartDevice newDevice(int type) {
        return switch (type) {
            case 0 -> new EdgeVisionDevice("E", "视觉");
            case 1 -> new InspectionRobot("R", "巡检");
            case 2 -> new VoiceInteractionDevice("V", "语音");
            default -> throw new AssertionError();
        };
    }

    private static void moveTo(SmartDevice device, DeviceStatus status) {
        switch (status) {
            case RUNNING -> device.execute();
            case OFFLINE -> device.goOffline();
            case CHARGING -> {
                device.startCharging();
            }
            default -> { }
        }
    }

    private static void apply(SmartDevice device, int action) {
        switch (action) {
            case 0 -> device.execute();
            case 1 -> device.finishTask();
            case 2 -> device.goOffline();
            case 3 -> device.goOnline();
            case 4 -> device.startCharging();
            case 5 -> device.finishCharging();
            case 6 -> {
                if (device instanceof Rechargeable rechargeable) {
                    rechargeable.charge();
                } else {
                    device.startCharging();
                }
            }
            default -> throw new AssertionError();
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        expect(type, action, true);
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action, boolean requireMessage) {
        checks++;
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception) && (!requireMessage || (exception.getMessage() != null
                    && !exception.getMessage().isBlank()))) {
                return;
            }
            throw new AssertionError("异常类型或提示错误", exception);
        }
        throw new AssertionError("预期异常：" + type.getSimpleName());
    }
}
