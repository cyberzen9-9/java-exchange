# 对照代码学习：业务、引用与边界

## 职责如何配合

Application 读取整行输入、选择菜单、调用 Service、打印结果与错误；Service 不读键盘或打印，它校验业务参数、组织仓库存取和 Model 操作，并记录结果；Model 维护设备自己的合法数据与状态；Store 持有编号到设备的映射，负责保存、查找、移除。

正常数据流是 `Application → DeviceService → DeviceStore / SmartDevice → 返回结果 → Application`。操作记录保存在 Service 的独立列表，不依附设备生命周期。Store 的防覆盖和删除检查是对公开底层入口的必要防守，Service 仍明确组织同样的业务；没有为了消除少量重复引入复杂结构。

## 一条正常流程：巡检任务

1. 菜单 1 读取类型 2、编号 `R-001`、名称 `园区巡检`，调用 `registerDevice(DeviceType, String, String)`。
2. `logged` 执行业务；Service 构造 `InspectionRobot`，父类构造器校验编号和名称，初始化 IDLE。
3. 私有 `register` 确认对象、编号、名称合法且编号不重复；`InMemoryDeviceStore.save` 用 `putIfAbsent` 新增映射，Service 追加 REGISTER 成功记录。
4. 菜单 5 调用 `startTask("R-001")`。私有 `requireDevice` 查到该对象，`SmartDevice.execute` 确认 IDLE，通过 `taskDescription` 得到“正在执行区域巡检任务”，将状态改为 RUNNING，并返回文字。
5. Service 追加 START_TASK 成功记录，Application 输出文字。菜单 6 经 `finishTask` 检查 RUNNING，再改回 IDLE 并记录完成。

Service 内部不调用公开 `findDeviceById`，否则一次开始任务会额外生成查询记录。现在每次公开业务调用产生一条对应记录。

## 一条失败流程：运行中开始充电

已有巡检设备 R-001，状态 RUNNING。菜单 7 调用 `startCharging("R-001")`，Service 查到设备并确认它实现 Rechargeable，然后调用其 `charge()`。

`InspectionRobot.charge` 首先调用父类 `startCharging`。能力校验通过，但 `requireStatus(IDLE, "开始充电")` 抛出 `IllegalDeviceOperationException`。赋值语句尚未运行，所以设备仍是 RUNNING，名称、编号、仓库成员都不变。

异常回到 Service 的 `logged`，生成失败记录：编号 R-001、动作 START_CHARGING、当前 LocalDateTime、success=false、非空原因。随后重新抛出原异常；Application 捕获并显示“开始充电要求状态为 IDLE，当前为 RUNNING”，继续菜单。这里捕获异常是为了记录和恢复交互，没有把失败伪装成成功。

失败改名也遵循“先检查、后赋值”：`setDeviceName` 先执行 `requireText`；失败时不会覆盖原名称。被拒绝删除时尚未执行 `Map.remove`。

## JavaSE 知识在项目中的实际位置

| 知识 | 对应代码及作用 |
| --- | --- |
| 集合选择 | `LinkedHashMap<String, SmartDevice>` 用唯一编号查设备，并保持注册顺序；顺序可使菜单输出和验收稳定。`ArrayList<OperationRecord>` 按调用完成顺序追加历史。返回时 `List.copyOf` 保护集合结构。`EnumMap` 用于枚举键计数，先放入所有零值项。 |
| 对象引用 | 注册保存的是同一个设备引用；查询返回同一对象。列表复制只复制引用，不深拷贝设备。旧查询列表不随仓库新增/删除改变大小，但其中设备的名称和状态会随对象变化。历史记录必须与这种“活对象列表”区分。 |
| 接口 | `DeviceStore` 让 Service 依赖存取约定；未来换持久化实现有替换位置，但仍需重新设计事务。`Rechargeable` 表示可充电能力，不是每种 SmartDevice 都实现它。 |
| 多态 | Service 通过 SmartDevice 执行统一任务入口；父类 final execute 守住状态，具体子类的包内 taskDescription 提供视觉识别、巡检、语音交互差异。通过 Rechargeable 调用两种设备各自的 charge。 |
| 枚举 | DeviceStatus 限定四种状态，DeviceType 限定三种设备，OperationAction 限定日志动作。枚举约束取值范围，合法转换仍需方法校验。 |
| 异常传播 | 参数错误 IllegalArgumentException；重复、找不到设备、非法操作有独立自定义类型。Model/Store 抛出，Service 记录并重新抛出，Application 展示。DuplicateDeviceException 与 DeviceNotFoundException 继承 IllegalArgumentException，仍可按具体类分别处理。 |
| Stream | filterByType/filterByStatus 把列表过滤为新列表；toList 返回不可修改结果。统计用一次简单循环同时累计两组数量，避免多次扫描，也更直观。 |
| Lambda 与方法引用 | logged 接收 Supplier，将本次业务作为无参操作执行，再统一处理日志；它只是一个私有辅助方法。统计里的 Long::sum 表示累计，不是额外的框架。 |
| 日期时间 | OperationRecord 用 LocalDateTime 保存操作发生时的值；菜单用 DateTimeFormatter 显示。显示到秒，底层记录可能含更细精度。它不保存时区，也不用于比较跨机器时间。 |
| record 与不可变值 | OperationRecord 的组件都是 String、枚举、时间和布尔值，无可变设备引用。DeviceStatistics 的两个 Map 还必须 copyOf；只有 record 关键字并不会自动使内部 Map 不可变。 |

设 n 为当前设备数、m 为记录数：按编号查找、新增、删除平均 O(1)（通常的散列表假设，不承诺最坏常数）；遍历、查询全部、筛选、统计为 O(n)。返回设备列表占 O(n) 额外引用空间；查看全部记录为 O(m)，复制占 O(m)。追加记录通常摊销 O(1)。这里没有任务调度或路径规划算法。

## 相关数据入口逐项检查

| 入口 | 规则与影响 |
| --- | --- |
| 三种设备构造器 → SmartDevice 构造器 | 编号、名称拒绝 null/空串/空白；状态只能初始为 IDLE。编号 private final，无 setter。getter final 防止子类伪造身份读取。 |
| setDeviceName | final；所有调用均校验后才赋值，无状态限制；直接调用不写 Service 日志。 |
| execute、finishTask、goOffline、goOnline | final；只有明确的前置状态可转换；无公共任意状态赋值入口。 |
| startCharging、finishCharging | final；同时检查 Rechargeable 能力和状态；视觉即便直接调用也被拒绝。 |
| 两个 charge | 具体类为 final，charge 委托 startCharging，不存在仅打印而绕过状态的旧实现。 |
| 包内 taskDescription | 只返回任务描述，不修改状态；不能由外部包当作公开任务入口使用。新增类型须在 model 包内实现它。 |
| registerDevice(SmartDevice) | 兼容现有对象入口；拒绝 null 和重复，已有对象与仓库共享引用。对象构造失败发生在调用前时，Service 无法记录从未进入它的调用。 |
| registerDevice(DeviceType, id, name) | 菜单主入口；构造放在记录范围内，类型/编号/名称错误都能记录。 |
| Service 其他公开业务方法 | 查找存在设备、委托 Model、写一次对应成败日志。筛选拒绝 null 枚举；集合操作不对应单设备时记录编号 null。构造 Service 时拒绝空仓库，此时还没有可用 Service 实例写日志。 |
| Store.save | 只新增，不做更新；空对象/非法字段拒绝，重复编号不覆盖原对象。 |
| Store.findById / findAll | 前者拒绝空编号，未找到返回 null；后者返回独立只读列表，不返回 Map 或 values 活视图。 |
| Store.deleteById | 拒绝空编号、不存在、RUNNING；仅移除映射。Service 的删除入口同样检查运行状态。 |
| 操作记录、统计返回值 | 列表和 Map 不可修改；日志与设备引用分离，统计是当时数量快照。旧日志列表不会随新操作增长。 |
| Application 输入 | 只解析菜单数字；字段完整后才调用 Service；业务错误由业务层决定。数字格式错误显示提示，EOF 退出。 |

## 状态边界与局限

完整合法转换见 README 的状态表。RUNNING 不能直接离线或充电；CHARGING 不能开始任务或离线；OFFLINE 不能开始任务或充电。只有先回到合法起点才能继续。删除例外：它是移除管理引用，CHARGING/OFFLINE 可以直接删除，不会自动结束充电或让设备上线。改名不改变状态。

状态由对象封装，业务日志由 Service 负责。当前公开查询兼容原 SmartDevice 引用，注册方也可保留该引用，所以直接调用 Model 的合法操作会改变管理对象而没有 Service 日志。这不允许非法名称或状态进入，但不提供“所有调用都经过 Service”的强隔离保证。测试明确验证了这种引用关系；对外暴露只读 DTO 是未来可讨论的修改，不应误认为 List.copyOf 已完成深拷贝。

设备类 final 和状态方法 final 面向本版三种具体设备，减少覆盖校验的入口；不防御恶意反射或修改项目源码。Store 接口可替换，但本版只验收 InMemoryDeviceStore，其他实现仍应遵守契约。没有多线程锁和事务，不能把当前“先查后保存”“修改后记日志”直接当成并发系统保证。记录无限增长；时钟调整可能影响时间先后，因此记录列表顺序才是本次运行的追加顺序。删除后同编号可重用，旧日志没有独立的“设备实例版本号”。

## 练习（先不附答案）

1. 结果预测：新建巡检 R 并注册，令 `List<SmartDevice> list = service.findAllDevices()`，随后经 Service 改名为“二号”、开始充电、删除 R。预测 `list.size()`、`list.get(0)` 的名称/状态、当前仓库数量，以及最早 REGISTER 记录的名称参数。分别说明是对象引用还是值快照造成的。
2. 小需求修改：增加“按名称关键字查询”，非空关键字做大小写敏感的包含匹配；关键字全空白时拒绝并记失败记录，无匹配返回空列表。说明要修改哪些层，完成菜单与独立验收；不要把查询逻辑只写进菜单。
3. 遗漏边界排查：删除设备后，用原编号重新注册另一种类型，再查询历史记录。当前哪些字段能够区分两次注册，哪些使用场景仍容易混淆？如果要禁止同一次运行内重用已删除编号，需要在哪一层保存什么信息，并补哪些失败及历史不变性用例？
