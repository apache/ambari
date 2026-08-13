# 权限、Feature Flag 与运行时条件索引

本文不是权限名称清单，而是经典 Ember 对功能入口和操作能力的实际判定基线。React 必须同时复刻 RBAC、全局长流程互斥、feature flag、stack/service metadata、组件状态和 maintenance 条件；仅在页面加载后依赖后端返回 403 不等价。

## 权限判定语义

| ID | 规则 | 旧版精确行为 | 主要证据 |
| --- | --- | --- | --- |
| GATE-AUTH-001 | 逗号分隔权限是 OR | `App.havePermissions("A, B")` trim 后逐项检查，只要 `App.auth` 包含任意一个即为 true；React 不得解释为全部满足 | `app/app.js#havePermissions`、`test/app_test.js` |
| GATE-AUTH-002 | `isAuthorized` 增加向导所有权限制 | `App.isAuthorized(x) = App.havePermissions(x) && !wizardWatcherController.isNonWizardUser`；另一用户占有长向导时，即使当前用户有 RBAC 权限也不能修改 | `app/app.js#isAuthorized`、`app/utils/helper.js#isAuthorized` |
| GATE-AUTH-003 | `havePermissions` 不受向导所有权限制 | 模板 `havePermissions` 适合只读可见性；因此相同 permission 在 `havePermissions` 与 `isAuthorized` 下可能有不同结果 | `app/utils/helper.js` |
| GATE-AUTH-004 | Upgrade 全局互斥及 OR 表达式污染 | upgrade 非 `NOT_REQUIRED/COMPLETED`、非 suspended，且 `supports.opsDuringRollingUpgrade=false` 时，一条权限表达式只有在整个字符串既不包含 `CLUSTER.UPGRADE_DOWNGRADE_STACK` 也不包含 `CLUSTER.MANAGE_USER_PERSISTED_DATA` 时才被提前拒绝；只要组合串包含任一例外，整条 OR 绕过 upgrade deny，随后命中串中任意其他权限也返回 true。例外不是逐 permission 判断 | `app/app.js#havePermissions`、`app/views/main/menu.js` |
| GATE-AUTH-005 | 无权限模型即全部拒绝 | `App.auth` 尚未加载时 `havePermissions` 返回 false，UI 保持隐藏/禁用而不是先闪现操作 | `app/app.js`、登录初始化 |
| GATE-AUTH-006 | 只有 View 权限的用户走独立导航 | `App.auth` 空数组，或权限恰好只有 `VIEW.USE` 时 `isOnlyViewUser=true`，登录后进入 Views，不加载完整集群运维外壳 | `AUTH-006`、`app/app.js#isOnlyViewUser`、`app/router.js` |
| GATE-AUTH-007 | Cluster Administrator 提升前端角色标志 | 当前 cluster privilege 含 `CLUSTER.ADMINISTRATOR` 时同时设置 `App.isAdmin=true`、`isOperator=true`；Ambari Administrator 只由 `AMBARI.ADMINISTRATOR` 确认 | `app/router.js#loginGetClustersSuccessCallback` |
| GATE-AUTH-008 | UI 权限不是服务端授权替代品 | Ember 只为已知动作隐藏/禁用入口；所有 mutation 仍需服务端验证。React 对照必须同时测试无权限深链和直接 API 调用 | 所有 route/template/controller、Ambari REST RBAC |

## Ambari 级权限

| Permission | 旧版 Ember 作用 | 关联功能 ID | 闸门类型/边界 |
| --- | --- | --- | --- |
| `AMBARI.ADD_DELETE_CLUSTERS` | 无集群时进入 Installer；安装 Step 7 中决定全新集群特有配置行为 | `AUTH-006`、`INST-FLOW-001`、`INST-MODE-001`、`INST-0-*` 到 `INST-10-*` | Installer route 硬重定向；没有权限转 Views |
| `AMBARI.MANAGE_SETTINGS` | 进入 `/experimental`、编辑 supports；Background Operations footer 的管理动作 | `SHELL-007`、`BG-001` 到 `BG-005` | route guard + template action；非权限用户转 Views/Dashboard |
| `AMBARI.MANAGE_STACK_VERSIONS` | Versions 页的 Manage Versions 外链、out-of-sync reinstall/remove 入口及 repository URL 编辑 | `VER-LIST-001` 到 `VER-LIST-009`、`VIEW-ADMIN-003` | 不能与 version install/reinstall/upgrade/hide-discard 混为一项；后者由 `CLUSTER.UPGRADE_DOWNGRADE_STACK` 禁用逻辑控制 |
| `AMBARI.EDIT_STACK_REPOS` | repository URL 编辑的权限模型成员 | `STACK-SVC-003` 到 `STACK-SVC-005`、`INST-1-*` | application Admin Console 链接包含此权限；经典 stack 页面还依赖 cluster upgrade/version 权限，真实角色组合需运行态验证 |
| `AMBARI.ASSIGN_ROLES` | 显示独立 Ambari Admin Console 入口 | `SHELL-002` | Ember 只负责跳转；用户/角色页面属于 AngularJS，`OUT_OF_SCOPE` |
| `AMBARI.MANAGE_USERS` | 显示独立 Ambari Admin Console 入口 | `SHELL-002` | AngularJS Admin Console，`OUT_OF_SCOPE` |
| `AMBARI.MANAGE_GROUPS` | 显示独立 Ambari Admin Console 入口 | `SHELL-002` | AngularJS Admin Console，`OUT_OF_SCOPE` |
| `AMBARI.MANAGE_VIEWS` | 显示独立 Ambari Admin Console 入口 | `SHELL-002`、Views 文档的 Admin View 跳转 | View instance 管理由 AngularJS 完成，Ember 仅发现版本并跳转 |
| `AMBARI.RENAME_CLUSTER` | 显示独立 Ambari Admin Console 入口 | `SHELL-002` | 当前 Ember 树没有 rename form，`OUT_OF_SCOPE` |
| `AMBARI.ADMINISTRATOR` | 设置全局 `App.isAdmin` 并通常由服务端 privilege 集合赋予其他能力 | `AUTH-002`、`AUTH-006` | 不是所有按钮直接检查的万能 bypass；前端仍按返回的具体 permission names 判断 |

## Cluster 级权限

| Permission | 旧版 Ember 作用 | 关联功能 ID | 闸门类型/边界 |
| --- | --- | --- | --- |
| `CLUSTER.ADMINISTRATOR` | 设置 `isAdmin/isOperator` 角色标志 | `AUTH-002`、`SHELL-001` | 前端角色派生；动作仍按细粒度权限 |
| `CLUSTER.USER` | 标识只有只读 cluster user 权限的用户 | `AUTH-006`、`SHELL-002` | mapper 分类；无直接 mutation 按钮 |
| `CLUSTER.VIEW_CONFIGS` | Service Config tab 可见 | `SVC-CONFIG-001`、`SVC-CONFIG-002`、`HOST-TAB-001` | 使用 `havePermissions`，长向导占用时仍可只读查看 |
| `CLUSTER.VIEW_STACK_DETAILS` | Admin 主入口和 Stack/Versions 只读页面可见 | `STACK-SVC-*`、`VER-LIST-*` | 与 upgrade permission OR；Admin route 本身另有 routePath 限制，深链需逐路由测试 |
| `CLUSTER.TOGGLE_KERBEROS` | Kerberos 页面、Enable/Disable/Edit、长流程恢复 | `KRB-ENTRY-*` 到 `KRB-REC-*` | route guard + button guard +恢复 guard；还需 `supports.enableToggleKerberos` |
| `CLUSTER.UPGRADE_DOWNGRADE_STACK` | Admin/Stack/Upgrade route、发起和控制 upgrade、Admin View 跳转、Kerberos CSV | `UPG-START-*`、`UPG-RUN-*`、`VER-LIST-*`、`KRB-MGMT-008` | route guard + mutation；upgrade 中该权限是全局互斥例外 |
| `CLUSTER.MANAGE_ALERT_NOTIFICATIONS` | Actions 中 Manage Notifications，创建/编辑/删除 targets | `ALERT-LIST-005`、`ALERT-NOTIFY-*`、`ALERT-GROUP-005` | controller action 构造时过滤 |
| `CLUSTER.TOGGLE_ALERTS` | Definition detail 中 enable/disable cluster-scope alert | `ALERT-LIST-004`、`ALERT-DEF-004`、`ALERT-SET-003` | template guard；service-scope 另用 `SERVICE.TOGGLE_ALERTS` |
| `CLUSTER.MODIFY_CONFIGS` | Admin menu/Auto Start 的组合可见性以及 cluster-scope config 能力 | `SHELL-002`、`ADMIN-AUTO-*`、部分 `SVC-CONFIG-*` | 常与 service permission OR；具体 Service Config 编辑主要检查 `SERVICE.MODIFY_CONFIGS` |
| `CLUSTER.MANAGE_AUTO_START` | 进入 Service Auto Start，并实际启用全局开关、全选和每个 component recovery checkbox | `ADMIN-AUTO-001` 到 `ADMIN-AUTO-004` | route 与 `SERVICE.MANAGE_AUTO_START` OR，但整个页面的编辑控件统一只看此 cluster permission |
| `CLUSTER.MANAGE_USER_PERSISTED_DATA` | 允许写 UI persist/user preferences；upgrade 时仍可用 | `INST-FLOW-002`、所有长流程恢复、用户 UI 设置 | persist mixin guard；无权限时不能把客户端状态写到服务端 |
| `CLUSTER.MANAGE_WIDGETS` | Dashboard/Service Metrics Widget 编辑 | `OUT_OF_SCOPE` | 因用户明确排除 Metrics，不作为 React 验收项 |

## Service 与 Host 级权限

| Permission | 旧版 Ember 作用 | 关联功能 ID | 闸门类型/边界 |
| --- | --- | --- | --- |
| `SERVICE.START_STOP` | service、component、host bulk start/stop/restart；abort eligible request；restart-required 操作 | `SVC-ALL-002` 到 `SVC-ALL-004`、`SVC-ACT-001`、`SVC-CONFIG-009`、`HOST-BULK-001`、`HOST-COMP-001` | 菜单构造 +模板；仍按 current/desired state、maintenance、upgrade过滤 |
| `SERVICE.RUN_SERVICE_CHECK` | Run Service Check 的语义权限 | `SVC-ACT-002`、安装/安全/HA流程中的测试结果 | Service Actions 旧实现不是该项的独立 gate；宽 OR 分支可能因其他 service permission而加入 smoke test，最终依赖后端授权 |
| `SERVICE.RUN_CUSTOM_COMMAND` | stack custom commands、Refresh Queues、Rebalance、特定 service 命令的语义权限 | `SVC-ACT-006` 到 `SVC-ACT-009`、`HOST-COMP-007`、`HOST-COMP-008` | metadata/status过滤存在，但 Service Actions 宽 OR 后多数命令没有对应 permission的第二次前端检查；最终依赖后端授权 |
| `SERVICE.ADD_DELETE_SERVICES` | Add Service、Delete Service、Stack Services 中添加 | `SVC-ALL-001`、`SVC-ACT-005`、`SVC-ADD-*`、`STACK-SVC-002` | 同时要求 `supports.enableAddDeleteServices`；Add Service route 硬保护 |
| `SERVICE.MODIFY_CONFIGS` | Service Config 编辑/保存/revert、override、DB test关联编辑、部分 restart UI | `SVC-CONFIG-003` 到 `SVC-CONFIG-010`、`CFG-GROUP-006`、`HOST-TAB-001` | 编辑控件检查 + save controller；旧版本/compare 模式始终只读 |
| `SERVICE.COMPARE_CONFIGS` | 配置版本 Compare | `SVC-CONFIG-006` | `havePermissions`，只读能力 |
| `SERVICE.MANAGE_CONFIG_GROUPS` | Manage Config Groups、group/host/override 管理 | `CFG-GROUP-001` 到 `CFG-GROUP-006`、`HOST-ADD-004` | template action；实际 config 保存还可能需 modify configs |
| `SERVICE.TOGGLE_MAINTENANCE` | service/component maintenance 动作的语义权限 | `SVC-ACT-003`、`SVC-SUM-003`、`HOST-COMP-006` | Service Actions 的 toggle 在宽 OR 分支中直接加入，无独立前端 permission复查；组件路径也可能由 Host maintenance 权限显示，最终仍需后端按 scope授权 |
| `SERVICE.TOGGLE_ALERTS` | Alerts 页面入口动作、创建向导 route、service-scope definition enable/disable/edit/delete | `ALERT-LIST-004`、`ALERT-DEF-002` 到 `ALERT-DEF-006`、`ALERT-CREATE-*` | Create 还要求 `supports.createAlerts`；Metric 类型创建排除 |
| `SERVICE.DECOMMISSION_RECOMMISSION` | DataNode/NodeManager/RegionServer 等批量与单组件退役/重新服役 | `HOST-BULK-004`、`HOST-COMP-005` | 还要求 component 在 `components.decommissionAllowed` 且状态安全 |
| `SERVICE.ENABLE_HA` | NN/RM/Ranger HA、JournalNode Management、Federation、HAWQ standby 入口及恢复 | `SVC-ACT-010`、`NNHA-*`、`JN-*`、`RMHA-*`、`RAHA-*`、`FED-*`、`RBF-*`、`HAWQ-*` | service action + `serviceTypes`/feature/组件前置；向导所有权会统一撤销操作能力 |
| `SERVICE.MOVE` | Reassign Master 入口 | `SVC-MOVE-*`、`HOST-COMP-009` | 仅 `components.reassignable` master；所有 hosts 已有该 master 时禁用 |
| `SERVICE.SET_SERVICE_USERS_GROUPS` | Admin Service Accounts 页面 | `ADMIN-ACCT-001`、`ADMIN-ACCT-002` | route 硬保护；与 Config edit 权限是两套能力 |
| `SERVICE.MANAGE_AUTO_START` | 允许进入 Auto Start route | `ADMIN-AUTO-*` | 与 cluster auto-start permission OR；但页面没有按授权 service过滤，且所有开关/checkbox统一因缺 `CLUSTER.MANAGE_AUTO_START` 而 disabled，所以 service-only 用户只能查看 |
| `SERVICE.VIEW_OPERATIONAL_LOGS` | Host Logs tab、task Log Search 链接 | `HOST-TAB-004`、`BG-003` | 还需 LOGSEARCH service、`supports.logSearch` 和目标 host/log metadata |
| `HOST.ADD_DELETE_HOSTS` | Add Host、单/批删除 host | `HOST-ADD-*`、`HOST-DETAIL-004`、`HOST-BULK-008` | Add Host 入口还受 wizard/upgrade互斥；删除前做组件安全检查 |
| `HOST.ADD_DELETE_COMPONENTS` | 添加、安装、删除、重新安装 host components | `HOST-BULK-005`、`HOST-BULK-009`、`HOST-COMP-002` 到 `HOST-COMP-004` | stack cardinality、最后 master、component state 是额外硬条件 |
| `HOST.TOGGLE_MAINTENANCE` | host maintenance、部分 host/component maintenance 菜单 | `HOST-BULK-002`、`HOST-BULK-003`、`HOST-DETAIL-003`、`HOST-COMP-006` | scope 不同使用不同 endpoint |
| `SERVICE.VIEW_METRICS` | Metrics 列/视图 | `OUT_OF_SCOPE` | 明确排除 |

## `App.supports` Feature Flags

本节权威枚举生成器可识别的 `App.supports.*` flags。它们的初始默认值在 `app/config.js`，进入 main/installer 时由 `experimentalController.loadSupports()` 从服务端覆盖。React 不得将当前默认值编译成永久行为；非 `App.supports` 的 runtime UI gates 另见下一节。

| Flag（classic 默认） | 功能影响 | 关联功能 ID | 边界 |
| --- | --- | --- | --- |
| `enableAddDeleteServices=true` | Add/Delete Service 与 Stack Services add link | `SVC-ALL-001`、`SVC-ACT-005`、`SVC-ADD-*`、`STACK-SVC-002` | 与 `SERVICE.ADD_DELETE_SERVICES` AND |
| `enableToggleKerberos=true` | Kerberos Admin menu/route/Enable/Disable/Edit | `KRB-ENTRY-002`、`KRB-MGMT-*` | 与 `CLUSTER.TOGGLE_KERBEROS` AND；Windows stack menu 另有排除 |
| `preKerberizeCheck=false` | Enable Kerberos 前 server checks | `KRB-ENTRY-004` | false 时直接进入向导 |
| `kerberosStackAdvisor=true` | Configure Identities recommendations | `KRB-4-002` | 仅没有 stored values 时调用 |
| `regenerateKeytabsOnSingleHost=false` | Host action Regenerate Keytabs | `HOST-DETAIL-007`、`KRB-MGMT-006` | Kerberos enabled AND |
| `autoRollbackHA=false` | NN HA 关键阶段 Close/rollback 模式 | `NNHA-REC-*` | true 隐藏关键 step 关闭按钮并进入自动 rollback；false 给 manual rollback instructions |
| `manageJournalNode=true` | HDFS Service Action 的 Manage JournalNodes | `JN-ENTRY-*` | 还需 HA_MODE 且 host/JN count 可调整 |
| `preInstallChecks=false` | Installer Customize Services 的 Pre-Install Checks | `INST-7-006` | 只用于全新 installer，不应误加到所有复用 Step 7 场景 |
| `customizeAgentUserAccount=false` | SSH install options 的 Ambari Agent OS user | `INST-MODE-007`、`INST-2-002`、`HOST-ADD-001` | false 时 bootstrap payload 强制 `userRunAs=root` |
| `skipComponentStartAfterInstall=false` | Install/Add Host/Add Service Step 9 状态机可在 install 后跳过 start/check | `INST-9-001`、`INST-9-004`、`HOST-ADD-006`、`SVC-ADD-007` | 改变进度权重、可完成状态和 retry路径；有专门 controller tests |
| `disableCredentialsAutocompleteForRepoUrls=true` | repository URL 变化时是否保留浏览器 credential/autocomplete 相关输入 | `INST-1-004`、`STACK-SVC-004` | 仅交互细节，仍需保留避免凭据意外复用 |
| `alwaysEnableManagedMySQLForHive=false` | 安装/Add Service 的 Hive managed MySQL option 可见性 | `INST-7-002`、`SVC-ADD-004` | 在普通 Service Configs route 与安装向导行为不同 |
| `createAlerts=false` | Create Alert action | `ALERT-LIST-005`、`ALERT-CREATE-*` | 还需 toggle alert 权限；Metric type 排除 |
| `preUpgradeCheck=true` | Pre-Upgrade checks 与 custom cluster check 流程 | `UPG-START-004`、`UPG-START-005` | 服务端仍决定具体 checks/bypassability |
| `enabledWizardForHostOrderedUpgrade=true` | Host Ordered upgrade wizard 可选/受限 | `UPG-START-002`、`UPG-RUN-*` | false 时 controller `isWizardRestricted` 禁止相关 mutations |
| `displayOlderVersions=false` | Hosts/Versions 对旧 stack version 的显示 | `HOST-TAB-003`、`VER-LIST-001` 到 `VER-LIST-004` | false 时过滤低于 current 的旧版本记录 |
| `opsDuringRollingUpgrade=false` | Upgrade 期间是否允许普通 permission 操作 | `GATE-AUTH-004`、`UPG-RUN-010` | 位于全局 `havePermissions`，影响所有模块 |
| `serviceAutoStart=true` | Admin Auto Start menu/page | `ADMIN-AUTO-*` | 仍需 cluster/service auto-start permission |
| `enableNewServiceRestartOptions=false` | Service actions 中新的 Restart Service 选项 | `SVC-ACT-001`、`SVC-ACT-004` | 旧 Restart All/rolling actions仍可能存在 |
| `logSearch=true` | Host Logs tab、task/host Log Search link和额外 logging resource load | `HOST-TAB-004`、`BG-003` | 还需 LOGSEARCH installed 和 operational logs permission |
| `logCountVizualization=false` | Host Summary 的 log count visualization | `HOST-DETAIL-001` | 属于 operational logs，不是 Metrics；需 LOGSEARCH运行态验证 |
| `installGanglia=false` | 是否保留旧 GANGLIA service model | `OUT_OF_SCOPE` | Ganglia 是旧 Metrics service，本文不作为安装功能要求 |
| `customizedWidgetLayout=false` | Metrics widgets | `OUT_OF_SCOPE` | 明确排除 |
| `showPageLoadTime=false` | 开发/实验 page load timer | `OUT_OF_SCOPE` | 不构成最终用户业务功能 |
| `redhatSatellite=false` | config 中有默认定义，当前 classic `app/` 无消费调用 | 无 | `STATIC_ONLY` 遗留 capability，不作为已确认功能 |
| `addingNewRepository=false` | config 中有默认定义，当前 classic `app/` 无消费调用 | 无 | `STATIC_ONLY` 遗留 capability |
| `kerberosAutomated` | Add Service route 只有 TODO 注释，当前运行代码不读取 | 无 | 不得据注释创建 React 功能开关 |

## 其他 Runtime UI Gates

| ID | Gate（classic 默认） | 运行时来源与功能影响 | 边界 |
| --- | --- | --- | --- |
| GATE-RUNTIME-001 | `App.stackVersionsAvailable=true` | 初始加载所有 upgrades/repository versions 后重算为 `App.StackVersion` 是否非空；控制 Admin Versions tab 与 Host Stack Versions route | 不是 per-user supports，也不是 RBAC；无 version 时 Host深链回 Summary |
| GATE-RUNTIME-002 | `App.upgradeHistoryAvailable=false` | `restoreUpgradeState()` 根据是否存在已完成/非运行 upgrade重算；控制 Upgrade History tab | 运行中/暂停的唯一一条 upgrade本身不使 history tab可见 |
| GATE-RUNTIME-003 | `App.enableDigitalClock=false` | 本地 config直接控制顶栏 Clock view | 当前未从 supports覆盖；属于 shell UI gate，不是业务授权 |

## Stack、Service 与 Component Metadata 条件

| ID | 条件来源 | 旧版影响 | 关联功能 |
| --- | --- | --- | --- |
| GATE-META-001 | `StackService.isInstallable`、已安装 service names、service dependencies | Installer/Add Service 可选项，Delete Service 依赖限制 | `INST-4-*`、`SVC-ADD-001`、`SVC-ACT-005` |
| GATE-META-002 | service `serviceTypes` 包含 `HA_MODE` | HDFS/YARN/RANGER/HAWQ 显示对应 HA action | `NNHA-*`、`JN-*`、`RMHA-*`、`RAHA-*`、`HAWQ-*` |
| GATE-META-003 | service `serviceTypes` 包含 `FEDERATION` | HDFS 显示 NameNode Federation | `FED-*` |
| GATE-META-004 | service `serviceTypes` 包含 `DFSRouter` | HDFS 显示 Router-based Federation | `RBF-*` |
| GATE-META-005 | component cardinality/min/max、`isMaster`、`isClient`、`isSlave`、`isHAComponentOnly` | host assignment、add/delete component、skip master/slave steps | `INST-5-*`、`INST-6-*`、`HOST-COMP-002` 到 `HOST-COMP-004`、`SVC-ADD-*` |
| GATE-META-006 | `components.reassignable` | Move Master action与目标 host | `SVC-MOVE-*`、`HOST-COMP-009` |
| GATE-META-007 | `components.decommissionAllowed` | Decommission菜单 | `HOST-BULK-004`、`HOST-COMP-005` |
| GATE-META-008 | stack component custom commands | Refresh/Rebalance/Knox/HBase及任意 command 可见性 | `SVC-ACT-006` 到 `SVC-ACT-009`、`HOST-COMP-007`、`HOST-COMP-008` |
| GATE-META-009 | config types/themes/value attributes/dependencies | Config tab是否存在、控件、必填/只读/override/recommendations | `SVC-CONFIG-*`、`INST-7-*` |
| GATE-META-010 | Windows stack/stack family/version | Kerberos menu、部分 services/commands、旧 HAWQ 能力 | `KRB-ENTRY-002`、`HAWQ-*` |

## 状态、Maintenance 与长流程互斥

| ID | 条件 | 必须保持的行为 | 关联功能 |
| --- | --- | --- | --- |
| GATE-STATE-001 | service/component desired/current state | 已 STARTED 不再显示 Start；已 INSTALLED 不再 Stop；transition/request中禁重复提交 | `SVC-ACT-001`、`HOST-COMP-001`、所有 progress wizard |
| GATE-STATE-002 | host/component/service maintenance | start/stop、delete、HA前置等动作按 scope 禁用或跳过；maintenance 本身仍可由有权用户切换 | `HOST-BULK-*`、`HOST-COMP-*`、`SVC-ACT-*`、HA文档 |
| GATE-STATE-003 | heartbeat/host health | Add Host registration、Recover、delete、HA host selection和decommission安全判断 | `INST-3-*`、`HOST-DETAIL-004`、`HOST-DETAIL-005`、HA文档 |
| GATE-STATE-004 | stale configs/restart required | Restart Required/Refresh Configs 只对受影响 components | `SVC-ALL-004`、`SVC-ACT-004`、`SVC-CONFIG-009`、`HOST-COMP-007` |
| GATE-STATE-005 | background request/schedule already active | 避免同一 action 重复发起，显示现有 operation或禁用 | `BG-*`、`HOST-BULK-010`、`SVC-ACT-001` |
| GATE-STATE-006 | `wizardWatcherController.isNonWizardUser` | 全局 mutation权限临时失效；当前向导所有者继续，其他窗口只读或路由到流程 | `INST-FLOW-003`、`INST-FLOW-006`、`KRB-REC-*`、所有 HA recovery |
| GATE-STATE-007 | upgrade state | 普通操作默认失效；upgrade owner/authorized user可控制 pause/retry/finalize | `UPG-RUN-*`、`GATE-AUTH-004` |
| GATE-STATE-008 | Kerberos security type/KDC session | Add Host/Add Service/Reassign/HA等创建 component前验证或更新 identities | `INST-MODE-009`、`INST-MODE-010`、`KRB-X-*`、HA文档 |
| GATE-STATE-009 | cluster provisioning/wizard `clusterState` | 刷新/崩溃后恢复当前 step/request，禁止从入口重新并发创建 | `INST-FLOW-*`、`KRB-REC-*`、所有 HA/Federation recovery |

## 已知优先级与迁移风险

| ID | 旧版表达式 | 风险/基线结论 |
| --- | --- | --- |
| GATE-RISK-001 | `!isWindows && isAuthorized(KERBEROS) \|\| upgradeRunning` | JavaScript 中 `&&` 优先于 `\|\|`，所以 upgrade running 时即使 Windows/无 Kerberos 权限也可能显示 Admin Kerberos category；这是旧版静态行为，不应无意当作安全授权。React 矩阵标 `BEHAVIOR_DIFF` 后由维护者决定是否修复 |
| GATE-RISK-002 | Admin parent `enter` 接受四种权限 OR，但 `routePath` 主要要求 upgrade permission | 菜单可见、父 route 与子 route条件不完全一致；每个深链必须单测，不可只实现一个统一 Admin guard |
| GATE-RISK-003 | Service actions 外层用多个 permissions OR | 旧代码在宽 OR 后直接加入 Refresh Configs、Restart All、Run Smoke Test、Toggle Maintenance以及多种 custom command；多数 action/controller没有按语义 permission二次过滤，只有 HA、Move、Add Component等局部另查。用户仅有 OR 中任一权限即可看到/触发若干其他语义动作，最终由 server授权。React 必须逐 action + route + server授权，不复制漏闸 |
| GATE-RISK-006 | upgrade例外按整条 permission字符串判断 | 组合串只要包含 upgrade/persist例外，就会让串中其他权限一起绕过全局 upgrade deny；React 若按逐权限修正，应在矩阵标 `BEHAVIOR_DIFF`，不得误称经典已有严格隔离 |
| GATE-RISK-004 | Client feature flags 可在 Experimental 页面修改 | flags 是服务端/用户环境事实，不是可信安全边界；后端必须继续授权 |
| GATE-RISK-005 | 某些权限只出现在独立 Admin Console入口 | 不得因 permission 名存在就推断经典 Ember 有相应 CRUD 页面 |

## Metrics 权限边界

`SERVICE.VIEW_METRICS`、`CLUSTER.MANAGE_WIDGETS`、Metrics Widget 相关 feature flags 全部为 `OUT_OF_SCOPE`。HA checkpoint、decommission 或 component health 为安全判定读取 `metrics/...` 字段不受此排除影响，因为它们关联的是运维状态机而非指标展示。
