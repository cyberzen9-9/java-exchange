# 实际盘点与验收记录

验收日期：2026-10-29（Asia/Shanghai）。环境：Windows、PowerShell 7.6.5、Oracle JDK/javac 25，`--release 24`。未添加依赖、测试框架或构建框架。

## 修改前基线

初始 Git 状态为 `## main...origin/main`，无未提交修改；当前提交 `b5f327a Complete M1 device registration and query`。项目无 README、AGENTS.md、Maven/Gradle 配置；有现有 IDEA 配置、普通 src 和两个 main 入口。

| 范围 | 盘点结果 |
| --- | --- |
| 已完成 | 三种设备、Rechargeable、四种状态枚举；编号字段 final；构造时默认 IDLE；按编号/全部查询，全部查询用独立 ArrayList，空仓库得到空列表 |
| 部分完成 | 注册拒绝空对象/空编号/重复，但不检查名称；构造器不校验编号/名称，setter 不校验名称；execute/charge 只打印，各设备行为存在但未维护状态；Store 有删除方法但没有业务保护 |
| 未完成 | Service 改名、任务状态转换、充电状态转换、上下线、删除保护、自定义异常、筛选统计、操作记录、持续菜单和正式验收 |

基线实际执行：

```powershell
$sources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 24 -d out @sources
java -cp out com.cyberzen.smartdevice.app.SmartDeviceApplication
java -cp out com.cyberzen.smartdevice.test.SmartDeviceTest
```

编译成功。原 Application 查询 `Vision=2333` 不存在，抛 IllegalArgumentException，退出码 1，后续显示/遍历未执行。原 SmartDeviceTest 输出三种设备、任务说明、总数 3 和两种充电说明，退出码 0，但没有状态断言。基线输出有终端中文编码不匹配；最终入口用 UTF-8 输出选项验证，输出正常。

## 分阶段实际结果

| 阶段 | 已运行检查 | 实际结果 |
| --- | --- | --- |
| A | 编译；名称边界、各设备可达状态×7 个动作矩阵、Service 正常流；原演示 | 156 项断言通过；两个验收/演示入口退出 0 |
| B | A + Service/Store 参数与异常、删除规则、集合保护、历史记录内容 | 315 项断言通过 |
| C | B + 类型/状态筛选、混合统计、全菜单动作、非法输入恢复、字段截断 EOF | 435 项断言通过 |
| D | 增强 Service 参数错误记录校验、三种构造器边界、两类设备充电及视觉拒绝；严格编译和真实子进程 | 最终 492 项断言通过，原演示和 3 个控制台子进程退出 0 |

“492 项”是实际执行的断言/预期异常检查数，包含矩阵组合，不表示 492 个互不相关的业务场景。

## 最终命令、预期与实测

准确命令和单独编译、启动命令见 README。根目录一键命令：

```powershell
pwsh -NoProfile -File .\scripts\verify.ps1
```

脚本执行 `javac -encoding UTF-8 --release 24 -Xlint:all -Werror -d out @sources`，预期编译无警告和错误；实际成功。然后运行独立验收与原演示，以及 3 个真正调用 Application.main 的 Java 子进程。

| 检查 | 预期 | 实测 |
| --- | --- | --- |
| SmartDeviceAcceptanceTest | 所有断言通过，失败时 AssertionError 非零退出 | `PASS: 492 checks`，退出 0 |
| SmartDeviceTest | 保留三种任务与两种充电演示，任务结束后才充电 | 完整输出，退出 0 |
| 真实进程 0 | 非数字/越界菜单后可注册、开始/结束任务、删除、统计、查看历史和退出 | 所有预期输出存在，退出 0 |
| 真实进程 1 | 空标准输入立即正常退出 | 出现“输入已结束，正常退出”，退出 0 |
| 真实进程 2 | 注册只输入类型和编号，EOF 后退出且不陷入循环 | 出现正常 EOF 提示，退出 0；未完成注册不修改仓库另有内存断言 |
| Git 与入口复查 | diff 无空白错误，无 protected、setStatus、setDeviceId；Model/Service/Store 不打印 | `git diff --check` 无输出；相关 rg 搜索无匹配 |

进程检查有 10 秒超时；标准输入使用 UTF-8；独立验收不要求 `-ea`，因为使用显式 check/expect。脚本最后实际输出：

```text
PASS: 492 checks
（原演示输出）
PASS: console process smoke 0 (exit 0)
PASS: console process smoke 1 (exit 0)
PASS: console process smoke 2 (exit 0)
PASS: compilation, acceptance, original demo and 3 console processes
```

## 最终验收覆盖对应

| 业务边界 | 实际执行位置 |
| --- | --- |
| 三种设备注册/查询/改名；空仓库 | serviceAndStoreRules |
| 空对象、null/空串/全空白编号与名称、重复编号、查无设备 | modelRules、serviceAndStoreRules；包括全部构造器和两种注册入口 |
| 任务正常路径、重复开始、空闲结束 | modelRules 状态矩阵、serviceAndStoreRules |
| 两种设备充电、视觉拒绝、运行中充电/充电中任务拒绝、重复结束 | chargingServiceRules、modelRules、consoleRules |
| 上下线正常和非法转换、离线任务/充电拒绝 | modelRules 全部可达起点矩阵、chargingServiceRules、consoleRules |
| RUNNING 删除拒绝，其余三状态删除生效；不改原对象状态 | serviceAndStoreRules，直接 Store 删除入口也测 |
| 空筛选、非空仓库无匹配、类型/状态正确数量 | filterAndStatisticsRules，5 台混合状态设备 |
| 每次 Service 调用对应动作，失败原因非空，非法编号原样保存 | serviceAndStoreRules、auditRules、consoleRules；所有 OperationAction 均有成功记录断言 |
| 失败后名称/状态/仓库不变，重复保存不覆盖 | modelRules、serviceAndStoreRules、chargingServiceRules |
| 删除/改名后历史内容不变，记录只读、旧记录列表不增长 | auditRules |
| 返回列表/统计 Map 不可直接增删；设备引用仍是原对象 | serviceAndStoreRules、filterAndStatisticsRules |
| 非数字、整数溢出、空白菜单、无效选项、空白名称、错误编号后继续 | consoleRules；真实进程 0 再验证 main 与标准输入 |
| 顶层/注册每个字段、其他单字段和改名中途 EOF | consoleRules；真实进程 1、2 |
| 原有正常演示功能 | 继续运行 SmartDeviceTest，实际任务/充电现在遵守状态规则 |

每个状态矩阵组合新建设备；每类业务测试有独立 Store/Service；EOF 场景也分别新建实例。一个分组内的连续调用用于验证明确的业务流程，不依赖其他分组的状态。

## Git 范围与未验证内容

首次验收时修改 10 个原有 Java 文件；新增 8 个 Java 文件、1 个验收脚本和 3 个文档，共 22 个变更文件，当时尚未提交或推送。后续按用户跨电脑接续要求新增 docs/CHATGPT_HANDOFF.md，并更新 README 与本记录，合计 23 个文件。提交状态以 `git log` 和远程分支为准。未改变 DeviceStatus、构建配置或无关文件；没有删除用户文件。`out/` 只有被 Git 忽略的编译产物。

未手动点击验证 IDEA；未验证其他 JDK、其他操作系统、恶意反射、其他 DeviceStore 实现、并发访问、进程强制结束时的数据恢复或真实硬件。持久化、网络和电量变化不在本版范围。内存与对象引用等已知局限见 README 和学习说明。
