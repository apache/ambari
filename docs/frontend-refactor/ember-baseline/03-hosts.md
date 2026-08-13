# Hosts 模块

入口为 `/main/hosts`，详情为 `/main/hosts/:host_id/{summary|configs|alerts|stackVersions|logs}`。Host Metrics route 存在但为 `OUT_OF_SCOPE`。

## 列表、搜索与选择

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| HOST-LIST-001 | 分页列出 host name、IP、rack、health/heartbeat、maintenance、components、stack versions 和选中状态 | 服务端分页/排序；刷新时保留筛选和选择 | Hosts 直接 HttpClient、`hosts.bulk.operations` | `app/controllers/main/host.js`，`app/templates/main/host.hbs` |
| HOST-LIST-002 | 按 host name、IP、rack、health、maintenance、component、component state、stale config、version 等字段筛选和排序 | filter 使用 Ambari predicate；部分列按 feature/service 数据动态出现 | Hosts 直接 HttpClient | `app/controllers/main/host.js`，`app/views/main/host/hosts_table_menu_view.js` |
| HOST-LIST-003 | Combo Search 组合多个 facet、operator 和值，支持添加、移除、恢复 token | 某些 facet 值通过服务端 distinct 查询懒加载 | `hosts.with_searchTerm` | `app/controllers/main/host/combo_search_box.js`，`app/templates/main/host/combo_search_box.hbs` |
| HOST-LIST-004 | 单选、多选、全选当前结果、清空选择，并在跨分页/筛选后保留目标 host 集合 | 批量菜单显示选中数量；无目标时 action 禁用 | 无单一请求 | host controller/view/template |
| HOST-LIST-005 | 从 host 行跳详情、从 host health/alert 数跳该 host Alerts、从 service component 链接反向筛 Hosts | route 切换前保存列表筛选条件 | `alerts.instances.by_host` | `app/routes/main.js`，host templates/controllers |
| HOST-LIST-006 | Hosts CSV/列表导出 | 经典 Hosts controller/view/template 中未找到入口、处理函数或下载调用，标记为 `PLACEHOLDER`；除非补充运行态证据，否则不生成 React 功能要求 | 无 | `app/controllers/main/host.js`，`app/views/main/host`，`app/templates/main/host.hbs` |

## 批量操作

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| HOST-BULK-001 | 批量启动、停止、重启选定 host 上的某类 component | 只操作状态允许且不在 maintenance 的 component；展示将执行与跳过列表 | `common.host_components.update`、`common.batch.request_schedules` | `app/controllers/main/host/bulk_operations_controller.js` |
| HOST-BULK-002 | 批量进入/退出 Host Maintenance Mode | `HOST.TOGGLE_MAINTENANCE`；只更新状态不同的 hosts | `bulk_request.hosts.passive_state` | bulk operations controller |
| HOST-BULK-003 | 批量进入/退出 Component Maintenance Mode | 对 eligible component 过滤；与 host maintenance 独立 | `common.host_components.update` | bulk operations controller |
| HOST-BULK-004 | 批量 decommission/recommission DataNode、NodeManager、RegionServer 等 slave | `SERVICE.DECOMMISSION_RECOMMISSION`；HBase 与 HDFS/YARN 有不同 request/poll 校验 | `bulk_request.decommission`、decommission status 请求 | bulk operations controller |
| HOST-BULK-005 | 批量重新安装或安装 component/client | `HOST.ADD_DELETE_COMPONENTS`；过滤已安装/状态不适用项，异步 request 展示进度 | `common.host_components.update`、component install 请求 | bulk operations controller、install_component mixin |
| HOST-BULK-006 | 批量 refresh configs / configure components | 只处理 stale config 或支持 refresh 的 component | `host.host_component.refresh_configs`、`common.host_components.update` | bulk operations controller |
| HOST-BULK-007 | 批量设置 Rack ID | 校验 rack 格式；仅提交发生变化的 hosts | host rack update 请求 | bulk operations controller、`app/utils/hosts.js` |
| HOST-BULK-008 | 批量删除 hosts 前做可删除性检查，区分可删与跳过项 | `HOST.ADD_DELETE_HOSTS`；运行中组件、最后一个 master、不可重加 master 等禁止删除；二次确认 | `common.hosts.delete` | bulk operations controller，delete popup templates |
| HOST-BULK-009 | 批量删除同类 host components，先校验最小实例数与组件状态 | `HOST.ADD_DELETE_COMPONENTS`；未停止、未安装、低于 stack 最小数的项跳过 | `host.host_component.delete_components` | bulk operations controller |
| HOST-BULK-010 | 批量动作支持立即执行或 schedule，并显示 request context/progress | 有 pending schedule、wizard 或 upgrade 时可能禁用 | `common.batch.request_schedules`、background request APIs | bulk operations controller、batch scheduled requests util |

## Host 详情与主机动作

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| HOST-DETAIL-001 | Summary 显示 host health、IP/rack、OS、uptime、disk/memory/CPU 基本信息、component 列表及状态 | 不包含 Metrics tab 和指标图表 | host/global model load | `app/templates/main/host/summary.hbs`，`app/controllers/main/host/details.js` |
| HOST-DETAIL-002 | 设置单 host Rack ID | 校验且确认后更新模型 | host rack update 请求 | host details controller/template |
| HOST-DETAIL-003 | Host 进入/退出 Maintenance Mode | `HOST.TOGGLE_MAINTENANCE`；显示影响确认 | `bulk_request.hosts.passive_state` | host details controller |
| HOST-DETAIL-004 | 删除单 host | `HOST.ADD_DELETE_HOSTS`；复用可删除校验，必要时先重配特殊 master 关联 | `common.delete.host` 及特殊配置更新 | host details controller、delete host popups |
| HOST-DETAIL-005 | Recover Host 在确认所有 host components 均处于 STOPPED、INSTALL_FAILED 或 INIT 后，批量把全部 components 依次置为 `INIT`、`INSTALLED`；Kerberos 集群追加该 host keytab regeneration | 这是 component 恢复批处理，不执行 Check Host 环境检查，也不负责重新注册 agent；请求成功后打开 Background Operations | `common.batch.request_schedules` | `app/controllers/main/host/details.js#recoverHost`、recover popup templates |
| HOST-DETAIL-006 | 下载 host 上全部 client configs 或单个 client config | Host Details template 没有显式 permission gate；单个使用 `HOST_COMPONENT` scope，全部使用 `HOST` scope；浏览器 `window.open` 直接下载 archive，无 in-app HTTP failure/retry；popup 被阻止时旧代码对 null 调用 `focus()` | client config download URL | `app/mixins/main/host/details/support_client_configs_download.js`、`app/controllers/main/host/details.js#downloadClientConfigs`、details template |
| HOST-DETAIL-007 | 重新生成该 host 的 Kerberos keytabs | Kerberos 已启用、`regenerateKeytabsOnSingleHost`、相应管理权限 | `admin.kerberos_security.regenerate_keytabs.host` | host details controller |
| HOST-DETAIL-008 | 从 Host Actions 启动、停止或重启该 host 上全部可操作的非 client components | `SERVICE.START_STOP`；heartbeat failure 时三项禁用；Stop 包含 NameNode last-checkpoint safeguard，均在确认后提交并显示 request progress | host component state update/request APIs | `app/views/main/host/details.js#maintenance`、`app/controllers/main/host/details.js#doStartAllComponents`、`doStopAllComponents`、`doRestartAllComponents` |
| HOST-DETAIL-009 | Check Host 对当前 host 发起 pre-installed/environment checks，轮询 task 并按 JDK、repository、disk、THP 等类别显示 warnings，可 rerun | 入口仅对 `App.isAdmin` 或 `App.isOperator` 显示；这是独立于 Recover Host 的动作 | `preinstalled.checks`、`preinstalled.checks.tasks` | `app/views/main/host/details.js#maintenance`、`app/controllers/main/host/details.js#doAction`、`app/mixins/main/host/details/actions/check_host.js` |
| HOST-DETAIL-010 | Host Summary 中按 service/log level 显示的 log-count donut | counter 由 `Math.random()` 生成，不是后端 operational log 数据，标记为 `PLACEHOLDER`；不能用它证明 `SERVICE.VIEW_OPERATIONAL_LOGS` 或要求 React 复刻 | 无 | `app/views/main/host/log_metrics.js#logsData` |

## Host Component 动作

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| HOST-COMP-001 | 启动、停止、重启 component | `SERVICE.START_STOP`；依据 current/desired state、maintenance、upgrade/wizard 状态禁用 | `common.host.host_component.update`、schedule/request polling | `app/templates/main/host/details/host_component.hbs`，details controller |
| HOST-COMP-002 | 安装/重新安装 component 或 client | `HOST.ADD_DELETE_COMPONENTS`；先创建 component/host-component，再 INSTALL | `common.create_component`、`host.host_component.add_new_component(s)`、状态更新 | install_component mixin |
| HOST-COMP-003 | 添加可选 component 到 host | stack cardinality、依赖、host 状态和 service 安装状态校验；显示推荐/冲突提示 | `host.host_component.add_new_component(s)`、recommendation 请求 | details controller、addDeleteComponentPopup |
| HOST-COMP-004 | 删除 component | `HOST.ADD_DELETE_COMPONENTS`；校验最后实例、停止状态和特殊组件；JournalNode 等额外确认 | `common.delete.host_component`、配置更新请求 | details controller、delete popup |
| HOST-COMP-005 | Decommission/Recommission slave component | `SERVICE.DECOMMISSION_RECOMMISSION`；轮询 NameNode/HBase/YARN 状态确认完成 | `host.host_component.decommission_slave` 及 status 请求 | details controller |
| HOST-COMP-006 | 进入/退出 Component Maintenance Mode | `SERVICE.TOGGLE_MAINTENANCE` 或 Host 权限路径；状态切换后刷新 | `common.host.host_component.passive` | details controller |
| HOST-COMP-007 | Refresh configs / refresh component configs | stale 或 stack 支持时显示；执行自定义命令并跟踪 request | `host.host_component.refresh_configs` | details controller/template |
| HOST-COMP-008 | 执行 stack 定义的 custom command | `SERVICE.RUN_CUSTOM_COMMAND`；命令列表来自 service/component metadata | `service.item.executeCustomCommand` | details controller/template |
| HOST-COMP-009 | Move Master 进入 Reassign Master wizard | `SERVICE.MOVE`；仅可迁移 master，目标 hosts 由向导校验 | Reassign 请求组 | details controller、reassign routes |
| HOST-COMP-010 | Upgrade component / 安装 host stack version | stack version 可用且 host 未升级；展示 install progress | `host.stack_versions.install`、request APIs | stack versions controller/template |
| HOST-COMP-011 | Host Component 为 `UPGRADE_FAILED` 时，状态图标和 action 菜单静态代码会显示 Re-upgrade，确认后尝试重新提交 component upgrade | 当前 Server `State` 已不含 `UPGRADE_FAILED`，正常生产响应不会出现入口；图标入口要求 `HOST.ADD_DELETE_COMPONENTS`，dropdown整体却继承 `SERVICE.DECOMMISSION_RECOMMISSION`，内部 Re-upgrade无独立 gate。若旧/注入数据触发，代码调用未注册的 `host.host_component.upgrade`，`App.ajax.send` 仅 warning并返回 `null`，不会发 HTTP或打开 progress，payload还硬编码 `HDP-1.2.2`。旧测试因全局 stub只证明调用对象；标记 `STATIC_ONLY/LEGACY_BROKEN/UNREGISTERED`，React 不得当有效 API | `UNREGISTERED:host.host_component.upgrade` | `app/templates/main/host/details/host_component.hbs`、`app/views/main/host/details/host_component_view.js#isUpgradeFailed`、`app/controllers/main/host/details.js#upgradeComponent`、`app/utils/ajax/ajax.js#send`、`ambari-server/src/main/java/org/apache/ambari/server/state/State.java`、`test/controllers/main/host/details_test.js` |

## 详情子页

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| HOST-TAB-001 | Configs 按 service 展示该 host 的 config group/override 与属性 | 需 cluster configs 加载完成；权限决定是否可编辑 | `config.tags`、`config.host_overrides`、`config_groups.all_fields` | `app/controllers/main/host/configs_service.js`，host configs templates |
| HOST-TAB-002 | Host Alerts 列出该 host 的 alert instances，可跳 service 和 definition | 页面退出停止 alert instance polling | `alerts.instances.by_host` | host alerts controller/template/route |
| HOST-TAB-003 | Stack Versions 列出每个 repo version 的 host 状态并可发起安装 | `stackVersionsAvailable`；无支持时 route 回 Summary | `host.stack_versions.install` | main route、stack versions view/template |
| HOST-TAB-004 | Logs 列 service/component log files，打开/尾随日志并跳 Log Search UI | 菜单需要 `supports.logSearch`、已安装 LOGSEARCH 和 `SERVICE.VIEW_OPERATIONAL_LOGS`；直接 route 只检查 `supports.logSearch`，后两项不会阻止手工 URL 进入 | Log Search/host log endpoints | logs view/template、`app/routes/main.js`、`app/views/main/host/menu.js` |

## Host Logs 与 Log Search 外链

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| HOST-LOG-001 | `/main/hosts/:host_id/logs:query` 展示该 host 的 component log file 元数据，支持按 service/component/file 选择 | route guard 仅要求 `supports.logSearch=true`，否则回 Summary；LOGSEARCH 安装与 `SERVICE.VIEW_OPERATIONAL_LOGS` 只控制菜单可见性，经典实现存在 direct-URL gate 缺口 | `host.logging` 与全局 logging resource load | `app/routes/main.js`、`app/views/main/host/menu.js`、host logs template/view |
| HOST-LOG-002 | 打开日志 tail popup，选择 tail 数量并加载文本；可复制或把当前内容写入新窗口 | 新窗口只写已加载文本，不再次请求后端；popup 关闭/切换时清理 clipboard | `logtail.get` | `app/views/common/modal_popups/log_tail_popup.js`、`app/templates/common/modal_popups/log_tail_popup.hbs` |
| HOST-LOG-003 | 从 host log 行或 tail popup 打开 Log Search UI，并带 host、component、path/query 参数 | URL 来自 LOGSEARCH quicklink、`LOGSEARCH_SERVER` host 与端口；目标不可达由浏览器处理 | quicklink/config/host load；点击为浏览器外部导航 | `app/views/common/log_search_ui_link_view.js`、`log_tail_popup.js#logSearchUrl`、host logs template |
| HOST-LOG-004 | Background Operation、wizard 和普通 logs popup 可复制 stdout/stderr，或把当前文本/HTML写入新窗口 | 这些 `window.open()` 是本地文档输出，不是新的日志 API；popup blocker 时旧代码缺少统一恢复 | task/log load 已在相应功能请求中完成 | host progress、logs popup、wizard Step 9 log views、`generated/browser-network-entrypoints.md` |
| HOST-TAB-005 | Metrics route/tab | `OUT_OF_SCOPE` | 排除 | `app/routes/main.js` |

## Add Host Wizard

入口 `/main/host/add`，共 7 步。Hosts 菜单入口要求 `HOST.ADD_DELETE_HOSTS`，但 `addHost` route 本身没有 permission 或 feature gate，手工 direct URL 可以进入向导，这是经典实现的越权边界：

| ID | 步骤 | 行为与边界 | 主要请求 |
| --- | --- | --- | --- |
| HOST-ADD-001 | Step 1 Install Options | host names 先统一为小写，再校验格式、重复和已安装项；混输已安装与新 hosts 时提示后过滤旧项继续，全部已安装才阻断。Linux 自动模式采集 SSH private key/user/port；`customizeAgentUserAccount=false` 时 Agent user 隐藏且 bootstrap payload 强制 `root`，为 `true` 时自动模式下显示并必填。sudo/passwordless sudo 只是外部主机前置条件，不是 UI/payload 字段。HDPWIN 自动模式令 `useSSH=false`，隐藏全部 SSH 与 Agent user 字段，但仍向 `/bootstrap` 发送这些空字段，由服务端使用 PowerShell Remoting；若同时开启 `customizeAgentUserAccount`，隐藏的空 Agent user 仍会禁用 Next，是旧缺陷。手工模式不发 bootstrap，只等待预装 Agent 注册 | `wizard.launch_bootstrap` 或 host registration polling |
| HOST-ADD-002 | Step 2 Confirm Hosts | 自动模式启动/轮询 bootstrap，随后等待 Agent 注册；可 retry/remove。首次环境检查由 `preinstalled.checks` 创建任务并用 `preinstalled.checks.tasks` 轮询，warnings popup 的 Rerun 才使用 `wizard.step3.rerun_checks`。Add Host 的 `Skip host checks` 只跳 hostname resolution 与通用 preinstalled checks，提示框只有 OK、不是能在弹窗中取消勾选的二次确认，JDK check 仍独立执行 | `wizard.launch_bootstrap`、`wizard.step3.bootstrap`、`preinstalled.checks`、`preinstalled.checks.tasks`；重跑为 `wizard.step3.rerun_checks` |
| HOST-ADD-003 | Step 3 Assign Slaves and Clients | 为新增 hosts 勾选 slave/client components，遵守 cardinality 和依赖 | stack component metadata、recommendations |
| HOST-ADD-004 | Step 4 Config Groups | 按所选 slave/client 涉及的 service 加载现有 config groups，为新增 hosts 选择 Default 或既有非 default group；没有选择任何组件时跳过本步。这里只保存选择，不加载或编辑 config recommendations | `config_groups.all_fields` 等 config-group load |
| HOST-ADD-005 | Step 5 Review | 汇总 hosts、components 和 config-group 选择；提交时 `applyConfigGroup()` 对所选非 default groups 发更新，但调用方不等待 promise、没有成功/失败回调，失败对 UI 不可见且不阻断后续组件安装，也不会回滚已经更新的 group | `config_groups.update_config_group`、host-component install 请求 |
| HOST-ADD-006 | Step 6 Install, Start and Test | 安装组件、启动并运行必要 service checks，按 host/task 显示日志和 retry；Deploy route 的 `unroutePath()` 无条件返回 `false`，Add Host 不继承全新 Installer 对 Admin View/Views 的离开例外 | common service/component update、request/task polling |
| HOST-ADD-007 | Step 7 Summary | 汇总成功/警告/失败，Complete 关闭向导、回 Hosts 并刷新已有集群模型；不会写 cluster provisioning state `INSTALLED`，该写入只属于全新 Installer Complete | host/status refresh；无 provisioning mutation |
| HOST-ADD-008 | Wizard 恢复与取消 | 当前 step 和 local DB 持久化；其他窗口可恢复；取消清理 wizard state | persist/cluster status 请求 |

[generated/api-by-module/hosts.md](generated/api-by-module/hosts.md) 只是按请求名和 caller path 宽匹配生成的启发式候选索引，可能混入跨模块请求，也可能漏掉 Add Host 等独占调用，不具备模块级完备性。权威核对必须联合全局 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。
