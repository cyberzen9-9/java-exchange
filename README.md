# 智能设备与任务管理工具（JavaSE 控制台版）

JavaSE 综合复习项目：管理边缘视觉设备、巡检机器人、语音交互设备。沿用普通 `src` 包结构和 JDK 构建，无第三方依赖。

**本版本完全是本地内存模拟。退出或重启后，设备和操作记录都会丢失。** 任务与充电只改变内存状态并返回行为说明，没有接入真实硬件、数据库、网络、Web 或 AI 模型。

## 编译、启动与验收

本次实测：Windows、PowerShell 7、Oracle JDK 25（25+37），按原 IDEA 的 Java 24 语言级别编译。保持原 JDK/IDEA 配置；不使用 Maven 或 Spring。

在项目根目录执行以下 PowerShell 命令：

```powershell
Set-Location C:\Users\Adminstrator\IdeaProjects\java-exchange
java -version
javac -version
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 24 -Xlint:all -Werror -d out @sources
```

编译成功没有输出；失败时请先解决编译错误再启动。`out/` 为现有 Git 忽略目录。

```powershell
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out com.cyberzen.smartdevice.app.SmartDeviceApplication
```

独立验收和原演示入口：

```powershell
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out com.cyberzen.smartdevice.test.SmartDeviceAcceptanceTest
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out com.cyberzen.smartdevice.test.SmartDeviceTest
```

一键重新编译、验收、运行原演示和真实控制台进程检查（使用 PowerShell 7）：

```powershell
pwsh -NoProfile -File .\scripts\verify.ps1
```

在已打开的 PowerShell 7 中也可直接运行 `./scripts/verify.ps1`。本次实际结果见 [验收记录](docs/ACCEPTANCE.md)。IDEA 中把 `src` 标记为 Sources Root，运行同名 `main` 即可；本次没有手工操作 IDEA 验证。

## 使用菜单

启动后仓库为空；按照提示逐行输入，设备编号和名称均按字符串处理。

| 选项 | 操作 | 选项 | 操作 |
| --- | --- | --- | --- |
| 1 | 注册 | 2 | 按编号查询 |
| 3 | 查询全部 | 4 | 修改名称 |
| 5 | 开始任务 | 6 | 结束任务 |
| 7 | 开始充电 | 8 | 结束充电 |
| 9 | 离线 | 10 | 上线 |
| 11 | 删除 | 12 | 按类型筛选 |
| 13 | 按状态筛选 | 14 | 统计 |
| 15 | 查看操作记录 | 0 | 退出 |

注册和类型筛选：`1` 视觉、`2` 巡检、`3` 语音。状态筛选：`1` IDLE、`2` RUNNING、`3` CHARGING、`4` OFFLINE。

例如依次输入 `1 → 2 → R-001 → 园区巡检 → 5 → R-001 → 6 → R-001`，完成注册、开始巡检、结束巡检。每个箭头代表一次回车。

空白菜单、非数字、越界选项、错误编号或空白名称会显示提示并回到主菜单。输入结束（EOF）正常退出；若字段尚未输入完整，不执行该业务操作。菜单的数字解析负责交互，编号、名称、能力及状态校验仍由 Service/Model 完成。

## 业务规则与完成清单

| 功能 | 完成行为 |
| --- | --- |
| F1 注册 | 拒绝空对象、null/空白编号与名称、重复编号；编号从构造起不可修改 |
| F2 查询 | 编号必填，不存在抛明确异常；全部查询为空时返回空列表 |
| F3 改名 | 设备须存在；构造和 setter 均校验名称，失败保留原值 |
| F4/F5 任务与在线状态 | 新设备 IDLE；只允许下表所列转换；三种设备保留各自任务描述 |
| F6 充电 | 巡检、语音实现 Rechargeable；视觉不支持；直接调用 charge 也校验状态 |
| F7 删除 | 仅 RUNNING 禁止删除；IDLE、CHARGING、OFFLINE 可移除；不改变原对象状态、不清理历史 |
| F8 筛选统计 | 按类型/状态筛选；统计总数和每个枚举项，缺项数量为 0 |
| F9 操作记录 | 每次 Service 业务调用记录编号（适用时）、动作、LocalDateTime、成败、原因和参数文字；支持查看全部 |

| 操作 | 所需状态 | 成功后状态 | 附加条件 |
| --- | --- | --- | --- |
| execute / Service.startTask | IDLE | RUNNING | 任意设备类型 |
| finishTask | RUNNING | IDLE | 任意设备类型 |
| startCharging / charge | IDLE | CHARGING | 实现 Rechargeable |
| finishCharging | CHARGING | IDLE | 实现 Rechargeable |
| goOffline | IDLE | OFFLINE | 任意设备类型 |
| goOnline | OFFLINE | IDLE | 任意设备类型 |

其他转换一律拒绝，拒绝前不赋值。改名允许在所有状态进行，本版没有额外限制。编号大小写敏感，编号/名称用 `isBlank()` 拒绝空白，但不自动去除非空值两侧空格；`R` 与 ` R ` 是不同编号。

`VIEW_RECORDS` 本身也生成一条成功记录，并包含在本次返回值中。筛选、统计等不对应单设备的操作编号为 null；非法编号按原参数保留。失败原因不为空；成功原因使用空字符串。菜单解析错误和未完成的输入没有调用 Service，因此不会生成业务记录。

## 文件职责与变更

| 修改或新增文件 | 原因 |
| --- | --- |
| `model/SmartDevice.java` | 构造/改名校验；final 编号、受控状态转换与任务入口 |
| `model/EdgeVisionDevice.java`、`InspectionRobot.java`、`VoiceInteractionDevice.java` | 保留任务差异；具体类型声明；充电接入受控入口；具体设备类设为 final |
| `capability/Rechargeable.java` | 充电返回展示文字，不在 Model 内输出 |
| `enums/DeviceType.java`、`OperationAction.java` | 明确类型与记录动作，避免随意字符串 |
| `exception/DeviceNotFoundException.java`、`DuplicateDeviceException.java`、`IllegalDeviceOperationException.java` | 区分不存在、重复及能力/状态拒绝 |
| `model/OperationRecord.java`、`DeviceStatistics.java` | 不可变历史值与统计结果；不暴露可变内部容器 |
| `store/DeviceStore.java`、`InMemoryDeviceStore.java` | 明确存取约定；防覆盖、公开删除保护、独立只读列表；按注册顺序展示 |
| `service/DeviceService.java` | 业务编排、查询/改名/任务/充电/上下线/删除/筛选统计及统一记录 |
| `app/SmartDeviceApplication.java` | 持续菜单、输入输出、错误恢复与 EOF 退出 |
| `test/SmartDeviceTest.java` | 保留原多态演示；由入口打印返回文字，任务结束后再充电 |
| `test/SmartDeviceAcceptanceTest.java` | 独立数据、状态矩阵、Service/Store/记录/控制台验收 |
| `scripts/verify.ps1` | 可重复编译与独立进程 smoke 检查，无测试框架依赖 |
| `README.md`、`docs/LEARNING.md`、`docs/ACCEPTANCE.md` | 使用、设计、练习、实际验收结果与原基线记录 |

上述 Java 相对路径均在 `src/com/cyberzen/smartdevice/` 下；原包结构保留，`DeviceStatus`、JDK/IDEA 配置及 `.gitignore` 未改变。

## 兼容性与边界

`execute()`、`charge()`、`showDeviceInfo()` 从 void/内部打印改为返回 String，调用方负责显示；只写 `device.execute();` 的源码仍可编译，但不会再自动打印，且现在真正开始任务。旧二进制需重新编译，旧实现/子类需调整返回类型及覆盖方式。`execute` 和关键读写入口设为 final；任务描述由 model 包内的 `taskDescription()` 多态实现。没有增加 protected 或任意状态 setter。

原查询方法仍返回设备对象；`findAll`/筛选返回不可修改的独立列表，但元素是原对象引用。调用 setter 或状态方法会真实影响设备，不过仍经过 Model 校验；直接操作 Model/Store 不产生 Service 记录。应用层应统一经 Service 操作。如将来要求所有调用者都只能通过 Service 改动，需要进一步引入只读设备视图或调整对象所有权，属于后续 API 设计。

本版为单线程、小规模、可信本地代码场景：没有持久化、并发事务、权限控制、真实任务进度、电量或自动完成逻辑；记录持续增长，没有分页/归档。LocalDateTime 使用运行机器本地时间，不保存时区。重复编号在仓库当前内容中检查，删除后可重新注册相同编号，旧记录保留，按时间/动作区分。新增类型需同步类型枚举、具体 Model 与注册分支。

按本次业务验收未发现剩余阻塞问题；IDEA 手工点击流程、其他系统/JDK、真实硬件与多线程未验证。更完整的数据流、公开入口、JavaSE 知识用途及三道不附答案的练习见 [学习说明](docs/LEARNING.md)。

## 跨电脑与 ChatGPT 接续

另一台电脑可从 `https://github.com/cyberzen9-9/java-exchange.git` 克隆项目；已有克隆先检查 `git status`，再用 `git pull --ff-only origin main` 更新。Git 同步代码和文档，不同步运行内存或聊天历史。

[ChatGPT / 异地 Codex 接续说明](docs/CHATGPT_HANDOFF.md) 包含项目背景、业务规则、代码入口、验收结果、局限和可直接复制的接续提示。可以将该 Markdown 文件上传到自己的 ChatGPT 对话；另一台 Codex 先读该文件及 README 即可恢复项目背景。此文件是摘要，不是原聊天逐字导出，也不会自动创建 ChatGPT 对话。
