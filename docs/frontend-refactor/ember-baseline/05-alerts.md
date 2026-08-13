# Alerts 模块

入口为 `/main/alerts`，详情为 `/main/alerts/:alert_definition_id`，创建向导为 `/main/alerts/add/step{1..3}`。Metric 类型 Alert Definition 的创建参数和指标表达式为 `OUT_OF_SCOPE`；通用告警运维仍纳入。

## Definitions 列表与快速入口

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| ALERT-LIST-001 | 列出 Alert Definitions，显示 service/component、name、state、enabled、latest status、last checked/changed、notification 和 check count | 列表由 alert summary/instances mapper 持续刷新 | alert definitions/summary 直接 HttpClient | `app/controllers/main/alert_definitions_controller.js`，alerts templates/views |
| ALERT-LIST-002 | 按 definition name、service、component、state、enabled 等筛选和排序 | 筛选条件在页面更新时保留 | definitions/summary load | alert definitions view/controller |
| ALERT-LIST-003 | 点击 definition 进入详情；从 service/host/全局 critical-warning 弹窗进入对应 definition 或全部 Alerts | 路由切换保存列表筛选加载状态 | `alerts.instances.unhealthy`、`alerts.instances.by_definition` | main route、alert notifications popup |
| ALERT-LIST-004 | 在列表直接启用/禁用 definition | 列表入口需要 `CLUSTER.TOGGLE_ALERTS` 并二次确认；本地 enabled 在 PUT 前乐观切换且没有 error rollback，失败只能依赖全局错误处理 | `alerts.update_alert_definition` | `app/templates/main/alerts/alert_definition/alert_definition_state.hbs`、`app/controllers/main/alert_definitions_controller.js#toggleDefinitionState` |
| ALERT-LIST-005 | Actions 菜单提供 Create Alert、Manage Groups、Manage Notifications、Manage Settings | Create 菜单只检查 `supports.createAlerts`，但向导 route 另需 `SERVICE.TOGGLE_ALERTS`；Notifications 需 `CLUSTER.MANAGE_ALERT_NOTIFICATIONS`；Groups/Settings 菜单没有独立 permission gate | 无单一请求 | `app/controllers/main/alerts/alert_definitions_actions_controller.js#content`、`app/routes/add_alert_definition_routes.js` |

## Definition 详情与实例

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| ALERT-DEF-001 | 展示 definition label、description/type、service/component、scope/source、interval、threshold/config、groups、notification、enabled 和 repeat tolerance | 不展开 Metric 类型的指标配置细节 | definition/instances load | definition details controller/template |
| ALERT-DEF-002 | 编辑 label | 详情编辑入口需要 `SERVICE.TOGGLE_ALERTS`；Save 先更新模型并退出 editing，再发无自定义 error callback 的 PUT，失败不会恢复编辑状态/旧值 | `alerts.update_alert_definition` | definition details template、`app/controllers/main/alerts/definition_details_controller.js#saveEdit` |
| ALERT-DEF-003 | 编辑通用 alert configs/thresholds | warning/critical 顺序、数值、单位等校验；Save 先禁用字段并退出 edit，再发无自定义 error callback 的 PUT；与 label 的 Save-on-leave 是两个独立并发请求，不聚合结果 | `alerts.update_alert_definition` | `app/controllers/main/alerts/definition_configs_controller.js#saveConfigs`、`definition_details_controller.js#saveLabelAndConfigs` |
| ALERT-DEF-004 | 启用/禁用 definition | 详情入口需要 `SERVICE.TOGGLE_ALERTS` 并确认；同列表一样先乐观更新 enabled，失败没有 rollback | `alerts.update_alert_definition` | definition details template、`app/controllers/main/alerts/definition_details_controller.js#toggleDefinitionState` |
| ALERT-DEF-005 | 编辑 repeat tolerance/check count；接受 1 到 99 或隐藏 sentinel `DEBUG`，也可关闭 repeat tolerance | enabled 状态和 tolerance 值通过两个独立 PUT 提交，popup 提交后立即关闭，无聚合/rollback，可能部分成功 | `alerts.update_alert_definition` | `app/controllers/main/alerts/definition_details_controller.js#editRepeatTolerance` |
| ALERT-DEF-006 | 删除自定义 definition | 默认/stack 定义是否可删由模型属性控制；二次确认，失败保留当前页 | `alerts.delete_alert_definition` | definition details controller |
| ALERT-DEF-007 | 列出当前 instances，显示 service/host、state、last check、response；可跳 service 或 host alerts | 页面退出停止 instance polling | `alerts.instances.by_definition` | alert instances controller、details template |
| ALERT-DEF-008 | 打开 instance response/log 文本 | modal 展示 instance 已携带的 `text`，支持 Copy 和在新窗口打开；不发新的 backend log 请求，也不提供下载 | 无新请求，使用 instances 响应内数据 | details/instance views、`app/views/common/modal_popups/logs_popup.js` |
| ALERT-DEF-009 | 查询最近 24 小时 instance history，并按 host 显示返回记录数量 | 不是状态变化 timeline；UI 展示每个 host 的 history count | `alerts.get_instances_history` | `app/controllers/main/alerts/definition_details_controller.js`、definition details template/model |
| ALERT-DEF-010 | 编辑中离开 route 时弹 Save/Discard/Cancel | 仅 `isEditing` 时触发；Save 可分别启动 label 与 configs PUT，不等待聚合完成即允许 route 流程继续 | update 或无请求 | `app/routes/main.js`、`app/controllers/main/alerts/definition_details_controller.js#saveLabelAndConfigs` |

## 创建 Alert Definition

| ID | 步骤/行为 | 前置/边界 | 后端请求 |
| --- | --- | --- | --- |
| ALERT-CREATE-001 | Step 1 Choose Alert Type | Port、Web、Script、Aggregate 可进入对应配置；Metric 参数为 `OUT_OF_SCOPE`；fixture 虽列出 Raw，但 renderer 没有 `RAW` case，继续时会访问不存在的 source，标记为 `BROKEN/PLACEHOLDER` | 无 |
| ALERT-CREATE-002 | Step 2 Define Alert and Thresholds | 在统一 configs view 中输入 name/label、service/component/scope、interval/timeout 及各类型 source、warning/critical/retry 等字段并完成校验；Next 在本步生成 request JSON | stack/definitions load |
| ALERT-CREATE-003 | Step 3 Review | 只读显示选中类型和格式化后的 Alert Definition JSON；本步不再编辑 threshold，Done 才提交 | 无 mutation |
| ALERT-CREATE-004 | Done 创建 definition 并返回 Alerts | 菜单需 `supports.createAlerts`，向导 route 实际另需 `SERVICE.TOGGLE_ALERTS`；服务端失败时 create promise 不完成后续 close/finish 流程 | `alerts.create_alert_definition` |
| ALERT-CREATE-005 | 向导前后导航 | 已完成 step 可返回；未满足校验不能前进 | 无 |

## Alert Groups

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| ALERT-GROUP-001 | 按 service 列出 groups、description、definitions 和 notification targets | Manage Groups action 可见 | `alerts.load_alert_groups`、`alerts.notifications` | manage alert groups controller/template |
| ALERT-GROUP-002 | 创建 group | 名称必填且在 service 内唯一 | `alert_groups.create` | manage alert groups controller |
| ALERT-GROUP-003 | 重命名、修改描述、复制 group | 默认 group 不能按普通 group 修改 definitions | `alert_groups.update` | manage alert groups controller |
| ALERT-GROUP-004 | 给 group 添加/移除 definitions，支持按 service/component 筛选与多选 | 默认 group 禁止手工增删；至少选择一个 definition | `alert_groups.update` | manage alert groups controller、add definition popup |
| ALERT-GROUP-005 | 给 group 关联/取消 notification targets | notification 必须已存在 | `alert_groups.update` | manage alert groups controller |
| ALERT-GROUP-006 | 删除非 default group | default group 不可删；二次确认 | `alert_groups.delete` | manage alert groups controller |
| ALERT-GROUP-007 | 保存时先并发删除，全部回调后再并发更新和创建；无错误时关闭并刷新 notifications | 部分失败仍等待其余请求并把聚合错误留在原 popup；success popup 的 created/updated/deleted 数量来自计划操作数，不是服务端确认成功数 | create/update/delete requests | `app/controllers/main/alerts/alert_definitions_actions_controller.js#manageAlertGroups` |

## Alert Notifications

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| ALERT-NOTIFY-001 | 列出 notification name、type、enabled、global/group scope、severity 和 description | `CLUSTER.MANAGE_ALERT_NOTIFICATIONS` | `alerts.notifications` | manage alert notifications controller/template |
| ALERT-NOTIFY-002 | 创建 Email notification | 校验唯一 name、recipients、SMTP host/port、from；可选 auth、username/password confirmation、STARTTLS | `alerts.create_alert_notification` | create notification template/controller |
| ALERT-NOTIFY-003 | 创建 SNMP v1/v2c notification | 配置 hosts、port、community、OIDs、severity 与 groups，校验 FQDN/port | `alerts.create_alert_notification` | notification controller |
| ALERT-NOTIFY-004 | 创建 Custom SNMP notification | 在 SNMP 基础上允许 custom properties | `alerts.create_alert_notification` | notification controller |
| ALERT-NOTIFY-005 | 创建 Alert Script notification | 配置 dispatch property 和 script filename，校验文件名 | `alerts.create_alert_notification` | notification controller |
| ALERT-NOTIFY-006 | 设置 Global 或选择 groups，选择 Critical/Warning/OK/Unknown 等 severity | Global 时 group 选择禁用；支持 Select All/Clear All | create/update notification | notification view/controller |
| ALERT-NOTIFY-007 | 编辑或复制 notification | 编辑保留敏感属性语义；复制必须使用新名称 | `alerts.update_alert_notification` 或 create | notification controller |
| ALERT-NOTIFY-008 | 启用/禁用 notification | 只更新 enabled 状态，完成后刷新 | `alerts.update_alert_notification` | notification controller |
| ALERT-NOTIFY-009 | 删除 notification | 二次确认；被 group 引用时由服务端处理/报错 | `alerts.delete_alert_notification` | notification controller |
| ALERT-NOTIFY-010 | 添加/删除自定义 property | 名称只允许合法 config key，禁止与内建/已有属性冲突 | 随 create/update 提交 | notification controller/template |

## 全局告警设置

| ID | 功能与行为 | 前置/边界 | 后端请求 |
| --- | --- | --- | --- |
| ALERT-SET-001 | Alert check count/repeat tolerance 可按 definition 编辑 | 权限与输入范围同详情 | `alerts.update_alert_definition` |
| ALERT-SET-002 | Manage Alert Settings 修改 `cluster-env.alerts_repeat_tolerance`，接受 1 到 99 或 `DEBUG` 后调用 config save | Actions 菜单始终添加此项，没有 feature/stack/permission gate；提交后不等待 `admin.save_configs` 就关闭并 reload，error callback 可能被 reload 打断；证据见 `app/controllers/main/alerts/alert_definitions_actions_controller.js#manageSettings` | `admin.save_configs` |
| ALERT-SET-003 | Service/cluster maintenance 与 toggle alerts 会改变告警呈现和通知 | 由对应 Service/Host/Admin 操作触发 | service/host/definition update |

[generated/api-by-module/alerts.md](generated/api-by-module/alerts.md) 只是按请求名和 caller path 宽匹配生成的启发式候选索引，可能混入跨模块请求，也可能漏掉模块独占调用，不具备模块级完备性。权威核对必须联合全局 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。
