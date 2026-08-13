# Services 与 Configs 模块

入口为 `/main/services/:service_id/{summary|configs|audit}`。Service Metrics 和 Heatmaps 为 `OUT_OF_SCOPE`。

## Service 导航与全局动作

`/main/services/:service_id/audit` 只有空的 `main/service/info/audit` controller 和 route outlet；经典 Ember 树中没有对应 template、view 或专用请求，标记为 `PLACEHOLDER`，不能据此要求 React 实现 Audit 页面。

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| SVC-NAV-001 | 左侧列出已安装 services、health/state、restart required 和 alerts，选择后进入 Summary | service 不存在或未加载时回首个 service | global service load | `app/routes/main.js`，service menu/item templates |
| SVC-ALL-001 | Add Service 打开 7 步向导 | `SERVICE.ADD_DELETE_SERVICES` 且 `supports.enableAddDeleteServices`；upgrade/wizard 期间禁用 | Add Service 请求组 | service controller、add service route |
| SVC-ALL-002 | Start All Services | `SERVICE.START_STOP`；过滤已启动/maintenance/不可操作 services，确认后批量 PUT | `common.services.update`、request progress | `app/controllers/main/service.js` |
| SVC-ALL-003 | Stop All Services | 同上；停止前显示影响确认 | `common.services.update` | service controller |
| SVC-ALL-004 | Restart All Required | 仅 stale/restart-required components，支持立即或 rolling/schedule | `restart.allServices`、`common.batch.request_schedules` | service controller、restart views |
| SVC-ALL-005 | 下载所有 services 的 client configs | 使用 `CLUSTER` resource scope；该入口意外位于 all-services menu 的外层 `SERVICE.START_STOP` 或 `SERVICE.ADD_DELETE_SERVICES` gate 内。浏览器 `window.open` 直接下载 archive，没有 in-app HTTP failure/retry，popup 被阻止时旧代码会对 null 调用 `focus()` | client configs download URL | `app/templates/main/service/all_services_actions.hbs`、`app/controllers/main/service.js#downloadAllClientConfigs`、support client configs download mixin |

## 单 Service 动作

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| SVC-ACT-001 | Start/Stop/Restart Service | `SERVICE.START_STOP`；根据 desired/current state、maintenance、upgrade/wizard 禁用 | `common.service.update`、`common.batch.request_schedules` | `app/controllers/main/service/item.js` |
| SVC-ACT-002 | Run Service Check | `SERVICE.RUN_SERVICE_CHECK`；异步 request 显示 task 结果 | `service.item.smoke` 或 custom action | service item controller |
| SVC-ACT-003 | Service 进入/退出 Maintenance Mode | `SERVICE.TOGGLE_MAINTENANCE`；影响其 components/alerts 展示 | `common.service.passive` | service item controller |
| SVC-ACT-004 | Restart Required Components | 过滤 stale configs，选择 rolling restart 参数并跟踪进度 | `restart.hostComponents`、`restart.staleConfigs`、schedule | service item/config action mixins |
| SVC-ACT-005 | Delete Service | `SERVICE.ADD_DELETE_SERVICES` 和 feature flag；停止/依赖/最后 service 等条件校验，二次确认 | `common.delete.service` | service item controller、delete templates |
| SVC-ACT-006 | Refresh YARN Queues | YARN 已安装且 command 可用 | `service.item.refreshQueueYarnRequest` | service item controller |
| SVC-ACT-007 | HDFS Rebalance 启动/停止 | HDFS 已安装、NameNode/数据节点状态合适；输入阈值，进度由 request 展示 | `service.item.rebalanceHdfsNodes` | service item controller |
| SVC-ACT-008 | Knox LDAP start/stop、HBase replication start/stop 等 service-specific command | 特定 service/stack command 可用，`CONDITIONAL` | `service.item.startStopLdapKnox`、`service.item.updateHBaseReplication`、`service.item.stopHBaseReplication` | service item controller |
| SVC-ACT-009 | 执行任意 stack custom command | `SERVICE.RUN_CUSTOM_COMMAND`；metadata 决定 command、scope 和参数 | `service.item.executeCustomCommand` | service item controller |
| SVC-ACT-010 | 启用 HA/Federation、Manage JournalNodes、HAWQ standby 或 Move Master | `SERVICE.ENABLE_HA`/`SERVICE.MOVE`、feature/service/component 条件 | 相应 wizard 请求组 | service item controller、main routes |
| SVC-ACT-011 | 下载当前 service 的全部 client configs 或指定 client component config | 全部使用 `SERVICE` scope，指定 client 使用 `SERVICE_COMPONENT` scope；入口需 `CLUSTER.VIEW_CONFIGS` 且还受 service actions 外层 permission 集合影响；下载与失败语义同 SVC-ALL-005 | client configs download URL | `app/views/main/service/item.js`、`app/controllers/main/service/item.js#downloadClientConfigs`、`downloadAllClientConfigs` |

## Summary 非 Metrics

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| SVC-SUM-001 | 显示 service 状态、master/slave/client components、host 分布、alerts、maintenance 和 restart required | service-specific template 可替换基础布局 | global service/component/alert load | summary controller/templates |
| SVC-SUM-002 | component/host 链接跳 Host detail 或按 component 筛 Hosts | 保留返回路径和筛选 | Hosts load | main route、service templates |
| SVC-SUM-003 | 对单 component 执行 start/stop/restart、maintenance、custom command | 权限和状态条件同 Host Component | common host component/custom command 请求 | summary controller/templates |
| SVC-SUM-004 | Flume agent start/stop | FLUME service；host + handler 定位 | `service.flume.agent.command` | service summary controller |
| SVC-SUM-005 | Service-specific Quick Links 跳外部 Web UI；Ambari View 是另一套 route/iframe 机制 | Quick Link visibility、protocol/host/port 来自 stack metadata/configs；两类入口不能互相替代；经典 `_blank` 链接已设置 `rel="noopener noreferrer"` | `configs.quicklinksconfig`、`hosts.for_quick_links`、current config load | service summary template、`app/views/common/quick_view_link_view.js` |

## Quick Links 与浏览器外链

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| SVC-QL-001 | 从当前 stack service 的已合并 quicklinks descriptor 加载链接定义，客户端最终只保留 `visible=true` 的项 | 子 descriptor 只有 name 时，父链接删除发生在服务端 stack merge 阶段；客户端虽在配置依赖扫描时跳过残留 `remove=true` 项，但最终可见集合并不直接按 `remove` 过滤。service 没有 descriptor、全部不可见或关联 component 不存在时显示无链接/错误状态，不构造空 URL | `configs.quicklinksconfig` | `app/views/common/quick_view_link_view.js#loadQuickLinksConfigurations`、`getQuickLinksConfiguration`；server `state/quicklinks/Link.java#mergeWithParent` |
| SVC-QL-002 | 根据 descriptor 的 protocol checks、当前 site properties 和 `hdfs-site/dfs.http.policy` 选择 HTTP/HTTPS | `HTTPS_ONLY`/`HTTP_ONLY` 可固定协议；普通模式逐条匹配 `EXIST`、`NOT_EXIST` 或精确值，检查不满足时反转协议 | current configs by required sites | `quick_view_link_view.js#setProtocol`、`meetDesired`、`reverseType` |
| SVC-QL-003 | 为 descriptor 关联 component 加载内部 host 到 public host 映射，并按单 host、多 host、多 nameservice/master group生成链接组 | 没有关联 host 且 descriptor 没有 override host 时显示 no-host；只使用实际已安装 component | `hosts.for_quick_links` | `quick_view_link_view.js#getQuickLinksHosts`、`getHosts`、`findHosts` |
| SVC-QL-004 | 从 config property、正则和默认值解析端口，把 `${config-type/property-name}` placeholder 与可选登录用户名代入 URL template | property/regex 缺失时回 default port；placeholder 找不到时保留 descriptor 既有回退语义 | current config load | `quick_view_link_view.js#getHostLink`、`resolvePlaceholders`、`setPort` |
| SVC-QL-005 | 应用 service 特例：Ranger 优先使用 `admin-properties/policymgr_external_url`；MapReduce2 可从配置中的 host:port 反查 public host；Oozie 只列 STARTED server | 外部 URL 由 stack/config 决定，经典前端不探测目标 Web UI 是否可达 | 同上 | `quick_view_link_view.js#getHostLink`、`processOozieHosts` |
| SVC-QL-006 | 对 HDFS NameNode、YARN ResourceManager、HBase Master 标注 Active/Standby 并按 group 展示 | HDFS/YARN 使用运维模型；HBase 为 Quick Link 安全选择读取 `metrics/hbase/master/IsActiveMaster`，不纳入指标展示 | `hosts.for_quick_links`；HBase 请求附加 active-master 字段 | `quick_view_link_view.js#processHdfsHosts`、`processYarnHosts`、`processHbaseHosts` |
| SVC-QL-007 | 点击链接以 `target="_blank"` 打开最终外部 URL | 点击本身绕过 `App.ajax`，浏览器直接导航；经典模板两条渲染分支都设置 `rel="noopener noreferrer"` | 浏览器导航到动态 URL | `app/templates/main/service/info/summary.hbs`、`generated/browser-network-entrypoints.md` |

## Service Configs

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| SVC-CONFIG-001 | 按 stack theme、tab、section、category 展示 service 配置，支持文本、密码、checkbox、select、radio、slider、目录、数据库等控件 | theme 缺失时回退传统 category 布局 | `configs.theme`、`configs.theme.services`、stack config load | configs controller、themes mapper、common config views |
| SVC-CONFIG-002 | 展示当前值、推荐值、默认值、是否 required、只读、错误/警告、单位和描述 | 属性 metadata、dependencies、value attributes 决定控件与校验 | stack configs/recommendation 请求 | config models/views |
| SVC-CONFIG-003 | 编辑配置并执行前端校验、依赖配置联动和 stack advisor recommendations；普通属性与 theme widget 支持 value/list 选择、widget/text 编辑切换、设置推荐值、undo saved value 和切换 final flag | `SERVICE.MODIFY_CONFIGS`；有错误时禁止保存；`common/configs/widgets` 是配置控件而非被排除的 Metrics Widget；推荐值还可同步 advisor 推荐的 final 状态 | `config.recommendations` | configs controller、enhanced configs mixins、`app/views/common/configs/widgets/config_widget_view.js`、config controls templates |
| SVC-CONFIG-004 | 保存新 config version，填写 note，显示 changed properties 和 dependent services | default-group 成功或失败后都会刷新 cluster/configs/quicklinks 并清 recommendations；非 default group 按 service 独立并行保存，仅当前 service 绑定 success callback，因此不是原子操作，dependent service 失败可能发生在成功提示之后，当前 service 失败还可能留下 `saveInProgress` | config save/cluster PUT 请求 | `app/mixins/common/configs/configs_saver.js#saveConfigsForNonDefaultGroup`、`doPUTClusterConfigurationSiteErrorCallback`、`onDoPUTClusterConfigurations` |
| SVC-CONFIG-005 | 未保存修改时离开 route，弹出 Save/Discard/Cancel | 其他用户运行 wizard 时不能保存 | 保存请求或无请求 | `app/routes/main.js`，configs controller |
| SVC-CONFIG-006 | 浏览 config version 历史，选择旧版、比较版本、显示新增/删除/修改项 | `SERVICE.COMPARE_CONFIGS` 控制 compare；旧版默认只读 | service config versions 请求 | config versions views/models |
| SVC-CONFIG-007 | 将历史 version 设为 current / revert | `SERVICE.MODIFY_CONFIGS`；生成新的 desired config 版本而非修改历史 | config save 请求 | config version controls |
| SVC-CONFIG-008 | Host override：为 config group 创建 override、删除 override、恢复 saved/default value，并可对 override 独立设置推荐值与 final flag | 非 default group；属性必须可 override；widget 与传统 property row 都提供 create/remove override 控件 | config group/config save 请求 | config_overridable mixin、overridden property view、`app/templates/common/configs/widgets/controls/create_override.hbs`、`remove_override.hbs` |
| SVC-CONFIG-009 | 显示并操作 restart required：按 service/host/component restart，支持 rolling restart | 权限、maintenance 和状态过滤 | `restart.hostComponents`、`restart.staleConfigs`、schedule | component_actions_by_configs mixin |
| SVC-CONFIG-010 | 测试数据库连接：创建 custom action，查询 request 中的 task ID，再轮询 task 结果 | Hive/Oozie/Ranger/Kerberos 等特定 configs；create 失败会结束 Connecting，task-list 或 polling GET 没有专用 error callback，可能永久停留在 Connecting；失败展示 stderr/stdout 和 structured check message，成功只显示 Connection OK | `cluster.custom_action.create`/`custom_action.create`、`custom_action.request` | `app/views/common/configs/widgets/test_db_connection_widget_view.js`、database util |
| SVC-CONFIG-011 | 在允许的 Advanced category 中新增自定义 property，支持单条 key/value 与多行 `key=value` bulk mode；可删除 user property | `SERVICE.MODIFY_CONFIGS`；校验 key、重复项和 bulk 行格式；已保存 property 删除时把对应 config type 标入待更新集合 | 随 config version save 提交 | `app/templates/common/configs/service_config_category.hbs`、`app/views/common/configs/service_configs_by_category_view.js#showAddPropertyWindow`、`removeProperty` |

## Config Groups

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| CFG-GROUP-001 | 列出 service 的 default 与非 default config groups、hosts 数和属性 overrides | `SERVICE.MANAGE_CONFIG_GROUPS` 控制管理 | `service.load_config_groups`、`config_groups.all_fields` | manage config groups controller/template |
| CFG-GROUP-002 | 创建 group，填写名称/描述并选择 hosts | host 同一 service 只能归属一个非 default group | `config_groups.create` | manage config groups controller |
| CFG-GROUP-003 | 重命名、修改描述、复制 group | 名称唯一校验 | `config_groups.update`/`config_groups.update_config_group` | manage config groups controller |
| CFG-GROUP-004 | 添加/移除 hosts；移动 host 时从原 group 调整 | 保存后 host config route 与 current group 更新 | config group update | manage config groups controller |
| CFG-GROUP-005 | 删除非 default group | default group 不可删；hosts 回到 default | `common.delete.config_group` | manage config groups controller |
| CFG-GROUP-006 | 查看 group properties 并进入对应 Configs 编辑 | 路由预选 group/version | config load | manage config groups controller |

## Add Service Wizard

| ID | 步骤 | 行为与边界 | 主要请求 |
| --- | --- | --- | --- |
| SVC-ADD-001 | Choose Services | 只列未安装且可安装 services；自动选择 dependencies，处理互斥与 service validation | stack services/components metadata |
| SVC-ADD-002 | Assign Masters | 为新增 masters 选择 hosts，校验 cardinality、资源和已有组件 | hosts/stack metadata |
| SVC-ADD-003 | Assign Slaves and Clients | 选择目标 hosts，保留已安装 component | hosts/component metadata |
| SVC-ADD-004 | Customize Services | 加载 configs、recommendations、credentials/database/account tabs | configs/recommendations |
| SVC-ADD-005 | Review | 汇总服务、组件、配置变更并确认 | 无 mutation |
| SVC-ADD-006 | Install, Start and Test | 创建 services/components/host-components，保存 configs，安装、启动、service checks；Deploy route 的 `unroutePath()` 无条件返回 `false`，Add Service 不继承全新 Installer 对 Admin View/Views 的离开例外 | common service/component/config/request APIs |
| SVC-ADD-007 | Summary | 展示完成/警告/失败并刷新已有集群的 service 菜单；Complete 关闭向导，不写 cluster provisioning state `INSTALLED`，该写入只属于全新 Installer Complete | service/cluster refresh；无 provisioning mutation |
| SVC-ADD-008 | 恢复与互斥 | 持久化 current step；其他用户被标为 non-wizard user，只读等待 | cluster status/persist |

## Reassign Master Wizard

| ID | 步骤/行为 | 说明 | 主要请求 |
| --- | --- | --- | --- |
| SVC-MOVE-001 | Get Started / Assign Master | 选择目标 master 和新 host，排除当前 host、不合格 host 与依赖冲突 | `hosts.high_availability.wizard`、config load |
| SVC-MOVE-002 | Review | 展示源/目标、受影响 configs/services 和停机提示 | config/recommendation |
| SVC-MOVE-003 | Configure Component | 停服务、创建/安装新 component、更新 configs、删除或停旧 component | common service/component/config APIs |
| SVC-MOVE-004 | Manual Commands | 对 NameNode、MySQL 等组件显示必须人工执行的命令并等待确认，`CONDITIONAL` | service-specific requests |
| SVC-MOVE-005 | Start and Test Services | 启动受影响 services、运行 checks、汇总 task | request/task APIs |
| SVC-MOVE-006 | Rollback | 失败时按组件类型恢复 configs、旧 component 与 service state | delete/update/config requests |

[generated/api-by-module/services-configs.md](generated/api-by-module/services-configs.md) 只是按请求名和 caller path 宽匹配生成的启发式候选索引，可能混入跨模块请求，也可能漏掉模块独占调用，不具备模块级完备性。权威核对必须联合全局 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。
