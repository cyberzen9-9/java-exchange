# 智能设备管理项目：ChatGPT / 异地 Codex 接续说明

这是一份项目与学习对话的交接摘要，不是逐字聊天导出。上传此文件不会自动搬迁原 Codex 聊天记录；它让另一段 ChatGPT/Codex 对话获得继续所需的上下文。原始 Codex 对话仍保留在原位置。

## 可以直接发给 ChatGPT 的开场语

> 请把本附件作为我的 JavaSE 学习项目上下文。我正在完成“智能设备与任务管理工具（JavaSE 控制台版）”，第一版 F1–F9 已实现并通过 492 项断言及 3 个真实控制台进程检查。我想理解业务拆分、数据流、集合、对象引用、接口、多态、枚举、异常、Stream 和日期时间，为以后 MySQL、JavaWeb、Spring Boot 打基础。
>
> 仓库是 https://github.com/cyberzen9-9/java-exchange ，分支 main。请先读取本附件，再根据我提供的实际源码、README、docs/LEARNING.md 和 docs/ACCEPTANCE.md 继续。不要假定你能访问私人仓库，也不要把附件中的历史测试结果说成你刚运行的结果。如果看不到源码，请告诉我需要上传哪些文件。
>
> 现在先帮我梳理“菜单输入 → Service → Store / Model → 操作记录 → 菜单输出”的完整业务流程，结合正常巡检与运行中充电失败两条路径讲清楚。请按完整业务阶段教学，不让我逐个方法填空，不逐行翻译所有代码，不因为代码运行成功就判断我已经熟练掌握。暂时不要升级数据库、Web 或框架，也不要未经要求修改第一版业务规则。

## 项目定位与现状

- 个人 JavaSE 综合复习项目，只用本地内存模拟。
- 普通 `src` 构建，无 Maven、Gradle、Spring、第三方依赖或测试框架。
- 根包：`com.cyberzen.smartdevice`。
- 三种设备：EdgeVisionDevice（视觉）、InspectionRobot（巡检）、VoiceInteractionDevice（语音）。后两种支持充电。
- 本轮按 A 名称/状态、B 异常/删除/记录、C 筛选统计/菜单、D 整体验收/说明顺序完成。
- 验证环境：Windows、PowerShell 7.6.5、Oracle JDK 25，按原 Java 24 语言级别 `--release 24` 编译。
- 实际跑过：严格编译 `-Xlint:all -Werror`，492 项断言，原多态演示，3 个真实控制台 Java 子进程；全部成功。
- JDK/IDEA 配置和原包结构保留。没有连接机器人、数据库、网络或 AI 模型。
- 退出/重启后设备和操作记录全部丢失；GitHub 同步的是代码与文档，不是运行中的内存数据。

## 第一版不可擅自改变的业务规则

1. 注册：对象、编号、名称不能为 null 或空白；编号不可重复且从构造起不可修改。构造器与改名 setter 都校验名称。
2. 查询：按编号和全部查询；空编号、不存在要明确报错，空仓库返回空集合。
3. 改名：设备须存在，失败保留原名称；本版允许在所有状态改名。
4. 任务：新设备为 IDLE；仅 IDLE 可开始并变 RUNNING，仅 RUNNING 可结束并回 IDLE；不同设备保留不同任务描述。
5. 在线状态：仅 IDLE 可主动离线为 OFFLINE，仅 OFFLINE 可上线为 IDLE；离线不能任务或充电。
6. 充电：仅实现 Rechargeable 的设备支持；IDLE → CHARGING → IDLE；公开 charge/startCharging 也必须校验；被拒绝不变状态。
7. 删除：仅 RUNNING 禁止删除；IDLE、CHARGING、OFFLINE 均允许删除，不添加其他限制。只从管理仓库移除，不改变原设备状态或删除历史。
8. 筛选统计：按类型/状态筛选；统计总数及每个类型、状态数量；无匹配为空列表/0。
9. 操作记录：覆盖 Service 的全部业务调用，含参数错误、能力/状态拒绝；字段为适用的设备编号、动作、LocalDateTime、成败、失败原因、当时参数文字。不保存可变设备对象。

编号大小写敏感，使用 isBlank 拒绝空白，不自动裁剪非空编号两侧空格。删除后可重新注册同编号。查看记录自身也记一条 VIEW_RECORDS，并包含在本次返回值中；成功原因为空字符串，失败原因非空。菜单解析错误或字段未输入完整尚未调用 Service，不产生业务日志。

## 职责与入口

| 层 | 关键文件 | 职责 |
| --- | --- | --- |
| Application | app/SmartDeviceApplication.java | 输入输出、0–15 持续菜单、错误恢复、EOF 正常退出 |
| Service | service/DeviceService.java | 注册/查询/改名/任务/充电/上下线/删除/筛选统计；私有 logged 统一记录并重新抛出异常 |
| Model | model/SmartDevice.java、三种设备类 | 自身数据和状态规则；final 编号/受控状态入口，无任意状态 setter |
| Store | store/DeviceStore.java、InMemoryDeviceStore.java | LinkedHashMap 存取、防覆盖、公开删除入口保护、只读独立列表 |
| 值对象 | model/OperationRecord.java、DeviceStatistics.java | 不可变历史值与独立统计；统计 Map 也 copyOf |
| 枚举 | enums/DeviceStatus.java、DeviceType.java、OperationAction.java | 有限状态、类型、动作 |
| 异常 | exception/ 下三个异常类 | DuplicateDeviceException、DeviceNotFoundException、IllegalDeviceOperationException；其他参数错误可为 IllegalArgumentException |
| 验收 | test/SmartDeviceAcceptanceTest.java、SmartDeviceTest.java | 独立验收与保留的原多态演示 |

Java 相对路径都在 `src/com/cyberzen/smartdevice/` 下。详细解释在 `docs/LEARNING.md`。

正常流程：菜单开始任务 → Service 私有 requireDevice 查设备 → Model.execute 检查 IDLE → 子类 taskDescription 描述行为 → 状态变 RUNNING → Service 记录成功 → 菜单显示。

失败流程：运行中充电 → Service 确认能力 → charge 委托 startCharging → Model 要求 IDLE 而当前 RUNNING，赋值前抛异常 → Service 记录原因并抛出 → 菜单提示继续，状态/名称/仓库未改变。

## 必须理解的兼容性和局限

- execute、charge、showDeviceInfo 改为返回 String，由入口打印；execute 现在真正开始任务。旧二进制需重编译，旧覆盖方法需适配。具体设备类为 final，没有新增 protected。
- 查询/筛选列表不可增删，但元素仍是原设备引用。注册方也可能保留原引用。直接调用 Model 的合法方法会改变设备而不经过 Service 日志，但仍不能绕过 Model 校验。这不等同于深拷贝或所有修改强制经过 Service。
- Store 的公开 save 防重复覆盖；公开 delete 也禁止 RUNNING。
- 日志只存不可变值，设备改名/删除不会改变已有记录的含义；日志列表和统计结果是独立快照。
- 单线程内存方案，无持久化、并发事务、权限控制、自动任务进度或电量。日志一直增长，无分页/归档。
- LocalDateTime 使用机器本地时间，无时区；列表顺序是追加顺序，不保证机器时钟单调。
- 未手工验证 IDEA UI、其他操作系统/JDK、真实硬件、多线程和其他 Store 实现。
- 编号查找/新增/删除平均 O(1)，遍历/筛选/统计 O(n)，读取全部记录 O(m)。没有路径规划或任务调度算法。

## 明天在另一台个人电脑取代码

首次下载，在准备保存项目的目录打开终端：

```powershell
git clone https://github.com/cyberzen9-9/java-exchange.git
Set-Location java-exchange
git log -1 --oneline
```

已有克隆时先检查本地修改，再更新；不要覆盖本地未提交内容：

```powershell
git status
git pull --ff-only origin main
```

IDEA 打开克隆目录，配置 JDK 25。命令行编译和运行：

```powershell
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 24 -Xlint:all -Werror -d out @sources
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out com.cyberzen.smartdevice.app.SmartDeviceApplication
```

PowerShell 7 一键验收：

```powershell
pwsh -NoProfile -File .\scripts\verify.ps1
```

源码推送后 GitHub 上可查看本说明。若 ChatGPT 无法访问仓库，下载本文件连同 README、docs/LEARNING.md、docs/ACCEPTANCE.md 上传；需要讲具体实现时，再上传对应 Java 文件或仓库源码压缩包。

## 给另一台电脑 Codex 的接续提示

> 请先读 README.md、docs/CHATGPT_HANDOFF.md、docs/LEARNING.md、docs/ACCEPTANCE.md，核对 git status 和实际源码，再运行 scripts/verify.ps1。第一版已完成，先报告当前版本是否与交接一致，不要重复实现已有功能，不覆盖未提交修改，不自行加框架。接下来以我的新需求或学习问题为准。

## 三道练习，暂不提供答案

1. 查询得到旧列表后，改名、开始充电并删除设备：预测旧列表大小、元素名称/状态、当前仓库数量和最早 REGISTER 记录内容。
2. 增加按名称关键字查询，空白关键字拒绝并记录失败：应修改哪些层、补哪些验收？
3. 删除后同编号注册另一种设备：历史如何区分？如果禁止同次运行内重用编号，信息应放在哪一层，需检查哪些边界？

## 后续协作偏好

按完整业务阶段推进，先盘点再实现，完成后说明实际运行的检查。保留已有代码及未提交修改，不用破坏性 Git 恢复命令；不为了展示技术增加无关功能。需要新业务范围或改变关键规则时先明确。提交、推送以当次用户授权为准，本次用户已授权提交并要求异地获取。
