# Stack、Versions、Upgrade 与集群 Admin

入口为 `/main/admin/stack/{services|versions|history}`，升级流程 route 为 `/main/admin/stack/upgrade`。核心权限是 `CLUSTER.VIEW_STACK_DETAILS` 和 `CLUSTER.UPGRADE_DOWNGRADE_STACK`。

## Stack Services 与仓库

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| STACK-SVC-001 | 列当前 stack 的 services、版本、安装状态和 repository version 信息 | Service 已安装与否影响可操作项 | stack/repo 直接 HttpClient | stack and upgrade controller、services template |
| STACK-SVC-002 | 从未安装 service 跳 Add Service | `SERVICE.ADD_DELETE_SERVICES` 与 feature flag；不满足时链接不可用 | 无额外请求 | services view/template |
| STACK-SVC-003 | 查看各 OS repository ID、base URL、mirrors list | Repository metadata 来自 version definition | `cluster.load_repositories` | stack and upgrade controller |
| STACK-SVC-004 | 编辑 repository base URL，恢复原值、清 local repository、保存 | Versions UI 以 `AMBARI.MANAGE_STACK_VERSIONS` 控制进入编辑，popup Save 还要求 `App.isAdmin && !App.isOperator`；旧 stackVersions 不可用时的 Stack Services repository rows 没有 template permission gate。不能用 `AMBARI.EDIT_STACK_REPOS` 统一描述两条路径 | `admin.stack_versions.edit.repo`、`wizard.advanced_repositories.valid_url` | `app/views/main/admin/stack_upgrade/upgrade_version_box_view.js`、`app/templates/main/admin/stack_upgrade/services.hbs` |
| STACK-SVC-005 | 验证 repository URL，可选择 skip validation | 验证失败警告但用户明确跳过时可继续 | `admin.stack_versions.validate.repo` | edit repositories template/controller |

## Versions 列表与包安装

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| VER-LIST-001 | 列出 repo versions，显示 display name/version、stack、type、host counts、service support 和状态 | 状态含 NOT_INSTALLED、INSTALLING、INSTALLED、CURRENT、UPGRADING、UPGRADED、INSTALL_FAILED、OUT_OF_SYNC | `admin.stack_versions.all`、stack/repo direct HttpClient | stack and upgrade controller、versions template |
| VER-LIST-002 | 按 Not Installed、All、Upgrade Ready、Installed、Current、In Process、Ready to Finalize 筛选 | count 随模型刷新 | 同 VER-LIST-001 | versions view |
| VER-LIST-003 | 展开 version 详情，查看 services、repositories、hosts 状态和不可升级原因 | Patch/Maint/Standard repo 语义不同 | repo/version load | versions/upgrade version templates |
| VER-LIST-004 | 按 version status 查看 Current/Installed/Not Installed hosts，并跳 Hosts 过滤结果 | 无 hosts 时禁用 | stack version/hosts load | version hosts popup/view |
| VER-LIST-005 | Install/Reinstall Packages 到全体适用 hosts | `CLUSTER.UPGRADE_DOWNGRADE_STACK`；maintenance、不需要或不支持的 hosts 跳过；二次确认 | `admin.stack_version.install.repo_version`、request polling | stack and upgrade controller |
| VER-LIST-006 | 对单 host 安装 version | 从 Host Stack Versions 进入 | `host.stack_versions.install` | host stack version controller |
| VER-LIST-007 | 处理 OUT_OF_SYNC component：reinstall 或 remove | 校验 component/host 状态与最低实例数 | `common.host_components.update`、`host.host_component.delete_components` | stack and upgrade controller |
| VER-LIST-008 | Hide 未使用/安装失败的 repository version | 这是确认后的 PUT，把 `RepositoryVersions.hidden` 设置为 `true`，不删除 resource；不允许处理 CURRENT/正在使用的 version | `admin.stack_versions.discard` | `app/controllers/main/admin/stack_and_upgrade_controller.js#confirmDiscardRepoVersion`、`app/utils/ajax/ajax.js` |
| VER-LIST-009 | Manage Versions 跳独立 Admin View | 离开 Cluster Management 前提示 | `ambari.service.load_server_version` | versions view、router |

## 发起 Upgrade/Downgrade

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| UPG-START-001 | 判断目标 version 是否 compatible 并加载支持的 upgrade types | 目标必须 INSTALLED/upgrade ready；禁止不支持的直接跨度 | `admin.upgrade.get_compatible_versions`、`admin.upgrade.get_supported_upgradeTypes` | stack and upgrade controller |
| UPG-START-002 | Upgrade Options 选择 Rolling、Express 或 Host Ordered | Host Ordered 受 `enabledWizardForHostOrderedUpgrade`；可选类型由服务端返回 | supported upgrade types 请求 | upgrade options view/template |
| UPG-START-003 | 设置 slave component failure 与 service check failure tolerance | 仅相应 upgrade type 支持；提示跳过风险 | 随 `admin.upgrade.start` payload | upgrade options view |
| UPG-START-004 | 运行/rerun Pre-Upgrade Checks，按 Required/Warning/Bypassed 展示 | Required 失败阻止；server 配置允许 bypass 时明确警告 | `admin.upgrade.pre_upgrade_check` | stack and upgrade controller、check popups |
| UPG-START-005 | 执行额外 custom cluster checks：maintenance、host heartbeat、previous upgrade、component installation 和 service checks | `supports.preUpgradeCheck`/服务端 check 类型；逐项展示 host/service details；previous-upgrade check 的 Finalize 按钮在经典模板中错误绑定 `abortUpgrade`，而真正的 `finalizeUpgrade` handler 未被调用，标记为 `BROKEN/PLACEHOLDER`，React 不得复刻 | rolling check 与直接 hosts HTTP；错误按钮会调用 `admin.upgrade.abort` | custom cluster check views、`custom_cluster_checks_prev_upgrade.hbs`、`custom_cluster_checks_prev_upgrade_view.js` |
| UPG-START-006 | 确认后创建 Upgrade | `CLUSTER.UPGRADE_DOWNGRADE_STACK`；显示通知 suppression 警告；Express 明确提示 downtime | `admin.upgrade.start` | stack and upgrade controller |
| UPG-START-007 | 从 aborted/failed upgrade 发起 Downgrade 或 retry Upgrade | `downgrade_allowed`、当前状态和目标 version 决定按钮 | `admin.downgrade.start`、`admin.upgrade.retry` | stack upgrade controller/routes |
| UPG-START-008 | Patch/Maint upgrade 和 revert | repo type/service selection决定 payload；revert 前列受影响 services | `admin.upgrade.start`、`admin.upgrade.revert` | stack and upgrade controller |

## Upgrade 执行状态机

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| UPG-RUN-001 | 加载 upgrade groups/items/tasks，显示总进度、当前 group、item 和状态 | 只请求非 PENDING group 的详细数据并持续 polling | `admin.upgrade.data`、`admin.upgrade.state` | stack upgrade controllers/views |
| UPG-RUN-002 | 展开 group/item/task 详情，查看 host、role、command、stdout/stderr，并可 Copy 或在新窗口打开已加载日志 | task detail 懒加载；通用 task UI 不展示 raw `structured_out`，也不提供下载，structured data 只被专用失败 summary 消费 | `admin.upgrade.upgrade_item`、`admin.upgrade.upgrade_task` | upgrade group/task views、`app/templates/main/admin/stack_upgrade/upgrade_task.hbs` |
| UPG-RUN-003 | HOLDING/manual step 显示说明，用户确认已完成后 Proceed，将当前 UpgradeItem 设为 `COMPLETED` | item 状态和是否 skippable 决定 action；view 发请求后立即清除本地 manual-done checkbox，mutation 失败依赖全局错误处理且没有 rollback | `admin.upgrade.upgradeItem.setState` | stack upgrade controller、`app/views/main/admin/stack_upgrade/upgrade_wizard_view.js#complete` |
| UPG-RUN-004 | 失败 item 可 Retry、Skip/Ignore and Proceed 或查看失败 hosts | Retry 把 item 设 `PENDING`；Ignore/Proceed 把 `HOLDING_FAILED`/`HOLDING_TIMED_OUT` 去掉 `HOLDING_` 后设为 `FAILED`/`TIMED_OUT`；details 在请求完成前关闭，无专用 rollback | `admin.upgrade.upgradeItem.setState`、task APIs | upgrade controller/popups、`app/views/main/admin/stack_upgrade/upgrade_wizard_view.js#retry`、`continue` |
| UPG-RUN-005 | Pause/Suspend Upgrade 或 Downgrade | 强警告暂停期间不得修改集群；PUT 当前 Upgrade 为 `request_status=ABORTED`、`suspended=true`，失败显示专用错误 | `admin.upgrade.suspend` | stack upgrade controller、`app/utils/ajax/ajax.js` |
| UPG-RUN-006 | Resume paused Upgrade/Downgrade | 始终通过 `admin.upgrade.retry` PUT 当前 Upgrade 为 `request_status=PENDING`；不是按 item state 分支；失败无 callback 重置 `requestInProgress`/`isRetryPending`，可能永久卡在 pending UI | `admin.upgrade.retry` | `app/controllers/main/admin/stack_and_upgrade_controller.js#retryUpgrade`、`resumeUpgrade`、`app/utils/ajax/ajax.js` |
| UPG-RUN-007 | Abort 当前 Upgrade 是发起 Downgrade 的前置操作，或 previous-upgrade custom check 的 legacy 修复动作 | 正常 progress wizard 没有通用 Abort/Stop 按钮，只提供 Pause 与条件性 Downgrade；previous-upgrade Abort 没有二次确认，不能把“通用 Abort + confirmation”作为 React 等价要求 | `admin.upgrade.abort` | stack upgrade controller、stack upgrade wizard template、previous-upgrade custom check template |
| UPG-RUN-008 | 关闭 progress modal 时升级继续在后台；返回 Versions 可重新打开 | current upgrade ID/state 持久化并由 server 恢复 | `cluster.load_last_upgrade`、upgrade data/state | routes/controller_route/stack upgrade route |
| UPG-RUN-009 | 其他用户发起升级时当前用户显示 initiator 和只读/non-wizard 状态 | 权限仍控制能否查看/操作 | upgrade state/user persist | wizard watcher、stack controller |
| UPG-RUN-010 | Upgrade 期间 suppress notifications、限制部分 host/service operations | `opsDuringRollingUpgrade` 等 feature flag 可放宽部分动作 | 无单一请求 | global app flags、service/host controllers |
| UPG-RUN-011 | 加载 skipped service checks 和 failed hosts，最终确认风险 | 可暂停修复、downgrade 或忽略继续 | `admin.upgrade.service_checks` | finalize/failed hosts views |
| UPG-RUN-012 | Finalize Upgrade/Downgrade 将最后一个 manual/finalize UpgradeItem 设为 `COMPLETED`；Finalize Later 复用 Pause/Suspend 流程 | revertible upgrade 明确说明 finalize 后不可 revert；cluster desired stack/version 的最终提交由服务端 orchestration 推进；`admin.stack_upgrade.run_upgrade` 只有注册定义、无 production caller，不能列为 Finalize endpoint | `admin.upgrade.upgradeItem.setState`；Finalize Later 为 `admin.upgrade.suspend` | stack and upgrade controller、upgrade wizard view/template |

## Upgrade History

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| UPG-HIST-001 | 列出 Upgrade/Downgrade 历史，显示 direction、type、repository/name/type、service from/to versions、status、start/duration/end time | UI 不显示 request ID；列表通过直接 HttpClient 加载，complete 时才解除 ready spinner | `DIRECT:stack_upgrade_history_controller.js#upgradeHistoryUrl` | history controller/template |
| UPG-HIST-002 | 按九类筛选：All、Upgrade、Downgrade，以及分别针对 Upgrade/Downgrade 的 Successful、Aborted、Failed | 不存在跨 direction 的单一 Successful/Aborted/Failed filter | 同 UPG-HIST-001 | history controller/view |
| UPG-HIST-003 | 选择历史记录后通过 request ID 加载 summary、groups/items/task 状态 | 实际调用 `admin.upgrade.data`；controller 虽构造单 record direct URL 但未使用；详情失败时 deferred 只在 success resolve，可能留下永久 spinner | `admin.upgrade.data` | `app/controllers/main/admin/stack_upgrade_history_controller.js`、history view |

## Service Accounts

| ID | 功能与行为 | 前置/边界 | 后端请求 |
| --- | --- | --- | --- |
| ADMIN-ACCT-001 | `/main/admin/serviceAccounts` 汇总所有 configs 中 displayType 为 `user`、category 为 `Users and Groups` 的 service users/groups | `SERVICE.SET_SERVICE_USERS_GROUPS`；无权限回 Dashboard | config tags/current configs |
| ADMIN-ACCT-002 | 按定义顺序只读显示 service account 名和值 | 页面只做加载、筛选、排序与表格展示，没有编辑控件、Save action 或 config mutation；证据见 `app/templates/main/admin/serviceAccounts.hbs`、`app/controllers/main/admin/serviceAccounts_controller.js` | config load only |

## Service Auto Start

| ID | 功能与行为 | 前置/边界 | 后端请求 |
| --- | --- | --- | --- |
| ADMIN-AUTO-001 | `/main/admin/serviceAutoStart` 加载全局 auto-start/recovery 开关 | gate 不一致：Admin 菜单要求 START_STOP 或 MODIFY_CONFIGS、任一 auto-start permission 和 `supports.serviceAutoStart`；child route 接受任一 auto-start permission 且不检查 flag；父 Admin `routePath` 又只允许 upgrade permission/进行中 upgrade，可能先阻止 auto-start-only direct URL；证据见 `app/views/main/menu.js`、`app/routes/main.js#admin`、`adminServiceAutoStart` | `config.tags`、cluster-env config load |
| ADMIN-AUTO-002 | 按 service 分组列 restartable 且已安装 components，逐项开关 `recovery_enabled` | Client 和未安装 component 不显示；view 没有 `CLUSTER.MANAGE_AUTO_START` 时禁用全部控件，即使 child route 接受 `SERVICE.MANAGE_AUTO_START`；证据见 service auto-start view/controller | `components.get_category` |
| ADMIN-AUTO-003 | 保存全局 `cluster-env.recovery_enabled` 和变更 component 集合 | 最多三个请求并行，popup 立即关闭；只有全部请求成功才执行 transition callback 和 sync cached status，任一失败都可能留下服务端部分更新；证据见 `app/controllers/main/admin/service_auto_start.js#showSavePopup` | `admin.save_configs`、`components.update` |
| ADMIN-AUTO-004 | 离页有未保存修改时选择 Save/Discard/Cancel | Discard 恢复 cached 值 | 同 ADMIN-AUTO-003 或无请求 |

`/main/admin/authentication`、`advanced`、`audit` 为 `PLACEHOLDER`。

[generated/api-by-module/stack-upgrades.md](generated/api-by-module/stack-upgrades.md) 只是按请求名和 caller path 宽匹配生成的启发式候选索引，可能混入跨模块请求，也可能漏掉模块独占调用，不具备模块级完备性。权威核对必须联合全局 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。
