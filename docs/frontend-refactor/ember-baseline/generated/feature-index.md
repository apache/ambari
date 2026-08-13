# Ember 稳定功能索引

> 由 `tools/extract-ember-baseline.mjs` 从 `01` 至 `13` 模块文档生成，请勿手工编辑。`00` 方法论、`14` React 矩阵和 `15` 审计报告不会被识别为功能来源。

共 1002 个稳定功能 ID。

| 功能 ID | 模块 | 小节 | 摘要 | 定义 |
| --- | --- | --- | --- | --- |
| `AUTH-001` | `01-auth-shell.md` | 功能清单 | 访问 \`/\` 自动跳转登录页；登录页清空主界面标题并连接 login outlet | [source](../01-auth-shell.md#L7) |
| `AUTH-002` | `01-auth-shell.md` | 功能清单 | 用户名密码使用 UTF-8 Base64 组成 Basic Authorization，先验证身份，再加载当前用户、privileges 和集群 | [source](../01-auth-shell.md#L8) |
| `AUTH-003` | `01-auth-shell.md` | 功能清单 | 支持 Knox/外部 JWT 登录跳转；401/403 响应中的 \`jwtProviderUrl\` 附加当前 URL 作为返回地址 | [source](../01-auth-shell.md#L9) |
| `AUTH-004` | `01-auth-shell.md` | 功能清单 | 已被服务端认证但 localStorage 未标记时，从响应 \`User\` header 恢复用户会话 | [source](../01-auth-shell.md#L10) |
| `AUTH-005` | `01-auth-shell.md` | 功能清单 | 登录成功加载登录消息设置；启用的消息以 modal 展示，用户确认后继续 | [source](../01-auth-shell.md#L11) |
| `AUTH-006` | `01-auth-shell.md` | 功能清单 | 根据集群和权限决定首屏：已安装集群进入 Dashboard/恢复路径，无集群进入 Admin View，仅 View 用户进入 Views，未完成集群进入 Installer | [source](../01-auth-shell.md#L12) |
| `AUTH-007` | `01-auth-shell.md` | 功能清单 | 退出时停止主界面 polling、清 localStorage/向导状态/权限、发送 logoff，并回登录页 | [source](../01-auth-shell.md#L13) |
| `AUTH-008` | `01-auth-shell.md` | 功能清单 | 维持服务端会话 keep-alive，主路由进入后启动 | [source](../01-auth-shell.md#L14) |
| `SHELL-001` | `01-auth-shell.md` | 功能清单 | 主应用进入时升级 localStorage schema，加载 supports、Ambari properties、cluster name、Views，再初始化全局模型和 polling | [source](../01-auth-shell.md#L15) |
| `SHELL-002` | `01-auth-shell.md` | 功能清单 | 全局导航包含 Dashboard、Services、Hosts、Alerts、Admin、Views，并由权限、安装状态、已安装服务和 feature flag 控制 | [source](../01-auth-shell.md#L16) |
| `SHELL-003` | `01-auth-shell.md` | 功能清单 | 保存用户 preferred path，登录后恢复到原 route | [source](../01-auth-shell.md#L17) |
| `SHELL-004` | `01-auth-shell.md` | 功能清单 | 页面标题、breadcrumb、侧边服务菜单、全局 spinner/loading overlay 随 route 和模型加载状态更新 | [source](../01-auth-shell.md#L18) |
| `SHELL-005` | `01-auth-shell.md` | 功能清单 | Inactivity timeout 按 admin 与 readonly Ambari property 选择时长，监听 window/iframe 的 mousemove、keypress、click，每秒检查活跃时间，并在超时前 60 秒显示继续会话/退出倒计时 | [source](../01-auth-shell.md#L19) |
| `SHELL-006` | `01-auth-shell.md` | 功能清单 | About 弹窗显示 controller 中已缓存的 Ambari Server 版本；没有 click-time 版本请求 | [source](../01-auth-shell.md#L20) |
| `SHELL-007` | `01-auth-shell.md` | 功能清单 | Experimental 页面读取支持开关，允许逐项修改并 Save/Cancel；Reset UI States 会清本地 DB/cache/向导/cluster status，并向服务端持久化 \`wizard-data={}\` | [source](../01-auth-shell.md#L21) |
| `SHELL-008` | `01-auth-shell.md` | 功能清单 | 版本不一致检查在 Installer 进入前阻止继续并提示 server/web client mismatch | [source](../01-auth-shell.md#L22) |
| `SHELL-009` | `01-auth-shell.md` | 功能清单 | User Settings popup 可保存是否自动展示 Background Operations 和 timezone，并列出当前用户的 cluster/View privileges | [source](../01-auth-shell.md#L23) |
| `BG-001` | `02-background-dashboard.md` | 后台操作 | 顶部 Background Operations 展示 request 列表、状态、进度、context、开始/结束时间 | [source](../02-background-dashboard.md#L7) |
| `BG-002` | `02-background-dashboard.md` | 后台操作 | 展开 request 查看 stages/tasks，按 host、role、command 和状态展示 | [source](../02-background-dashboard.md#L8) |
| `BG-003` | `02-background-dashboard.md` | 后台操作 | 打开单 task 查看 stdout、stderr 及对应 output/error log 路径，并可复制或在新窗口打开已加载文本 | [source](../02-background-dashboard.md#L9) |
| `BG-004` | `02-background-dashboard.md` | 后台操作 | 从服务、主机、安装和升级动作打开对应 request progress，而不是另建进度模型 | [source](../02-background-dashboard.md#L10) |
| `BG-005` | `02-background-dashboard.md` | 后台操作 | request/task 失败时显示失败主机与日志，允许相关业务流程触发 retry | [source](../02-background-dashboard.md#L11) |
| `BG-006` | `02-background-dashboard.md` | 后台操作 | 支持 request schedule：批量启动/停止/重启可立即执行或按时间调度 | [source](../02-background-dashboard.md#L12) |
| `BG-007` | `02-background-dashboard.md` | 后台操作 | polling 避免同类请求重叠，页面退出或 controller disable 时停止 | [source](../02-background-dashboard.md#L13) |
| `BG-008` | `02-background-dashboard.md` | 后台操作 | 原生 WebSocket/STOMP 推送 host-component、alert summary、topology、config、service、host、alert definition/group、upgrade、background request及动态 task detail，再由 mapper/controller更新 Ember Data；初始原生连接失败改用 SockJS eventsource/xhr polling系列 transport | [source](../02-background-dashboard.md#L14) |
| `BG-009` | `02-background-dashboard.md` | 后台操作 | 对可中止的运行中/未知状态 request 显示 Abort，确认后将 request status 更新为 \`ABORTED\` 并附加 abort reason | [source](../02-background-dashboard.md#L15) |
| `DASH-001` | `02-background-dashboard.md` | Dashboard 非 Metrics | Dashboard 默认 route 会转到 Metrics 页面 | [source](../02-background-dashboard.md#L23) |
| `DASH-002` | `02-background-dashboard.md` | Dashboard 非 Metrics | Config History 列出 service config versions，包括 service、version、author、创建时间、note、group、current 和 cluster-compatible 状态 | [source](../02-background-dashboard.md#L24) |
| `DASH-003` | `02-background-dashboard.md` | Dashboard 非 Metrics | 从配置历史记录跳到对应 Service Configs，并预选该 config version | [source](../02-background-dashboard.md#L25) |
| `DASH-004` | `02-background-dashboard.md` | Dashboard 非 Metrics | Config History 查看某版本的 hosts/config group 关联和版本说明 | [source](../02-background-dashboard.md#L26) |
| `DASH-005` | `02-background-dashboard.md` | Dashboard 非 Metrics | Dashboard service/host/alert health 汇总与导航来自 Dashboard Widget | [source](../02-background-dashboard.md#L27) |
| `HOST-LIST-001` | `03-hosts.md` | 列表、搜索与选择 | 分页列出 host name、IP、rack、health/heartbeat、maintenance、components、stack versions 和选中状态 | [source](../03-hosts.md#L9) |
| `HOST-LIST-002` | `03-hosts.md` | 列表、搜索与选择 | 按 host name、IP、rack、health、maintenance、component、component state、stale config、version 等字段筛选和排序 | [source](../03-hosts.md#L10) |
| `HOST-LIST-003` | `03-hosts.md` | 列表、搜索与选择 | Combo Search 组合多个 facet、operator 和值，支持添加、移除、恢复 token | [source](../03-hosts.md#L11) |
| `HOST-LIST-004` | `03-hosts.md` | 列表、搜索与选择 | 单选、多选、全选当前结果、清空选择，并在跨分页/筛选后保留目标 host 集合 | [source](../03-hosts.md#L12) |
| `HOST-LIST-005` | `03-hosts.md` | 列表、搜索与选择 | 从 host 行跳详情、从 host health/alert 数跳该 host Alerts、从 service component 链接反向筛 Hosts | [source](../03-hosts.md#L13) |
| `HOST-LIST-006` | `03-hosts.md` | 列表、搜索与选择 | Hosts CSV/列表导出 | [source](../03-hosts.md#L14) |
| `HOST-BULK-001` | `03-hosts.md` | 批量操作 | 批量启动、停止、重启选定 host 上的某类 component | [source](../03-hosts.md#L20) |
| `HOST-BULK-002` | `03-hosts.md` | 批量操作 | 批量进入/退出 Host Maintenance Mode | [source](../03-hosts.md#L21) |
| `HOST-BULK-003` | `03-hosts.md` | 批量操作 | 批量进入/退出 Component Maintenance Mode | [source](../03-hosts.md#L22) |
| `HOST-BULK-004` | `03-hosts.md` | 批量操作 | 批量 decommission/recommission DataNode、NodeManager、RegionServer 等 slave | [source](../03-hosts.md#L23) |
| `HOST-BULK-005` | `03-hosts.md` | 批量操作 | 批量重新安装或安装 component/client | [source](../03-hosts.md#L24) |
| `HOST-BULK-006` | `03-hosts.md` | 批量操作 | 批量 refresh configs / configure components | [source](../03-hosts.md#L25) |
| `HOST-BULK-007` | `03-hosts.md` | 批量操作 | 批量设置 Rack ID | [source](../03-hosts.md#L26) |
| `HOST-BULK-008` | `03-hosts.md` | 批量操作 | 批量删除 hosts 前做可删除性检查，区分可删与跳过项 | [source](../03-hosts.md#L27) |
| `HOST-BULK-009` | `03-hosts.md` | 批量操作 | 批量删除同类 host components，先校验最小实例数与组件状态 | [source](../03-hosts.md#L28) |
| `HOST-BULK-010` | `03-hosts.md` | 批量操作 | 批量动作支持立即执行或 schedule，并显示 request context/progress | [source](../03-hosts.md#L29) |
| `HOST-DETAIL-001` | `03-hosts.md` | Host 详情与主机动作 | Summary 显示 host health、IP/rack、OS、uptime、disk/memory/CPU 基本信息、component 列表及状态 | [source](../03-hosts.md#L35) |
| `HOST-DETAIL-002` | `03-hosts.md` | Host 详情与主机动作 | 设置单 host Rack ID | [source](../03-hosts.md#L36) |
| `HOST-DETAIL-003` | `03-hosts.md` | Host 详情与主机动作 | Host 进入/退出 Maintenance Mode | [source](../03-hosts.md#L37) |
| `HOST-DETAIL-004` | `03-hosts.md` | Host 详情与主机动作 | 删除单 host | [source](../03-hosts.md#L38) |
| `HOST-DETAIL-005` | `03-hosts.md` | Host 详情与主机动作 | Recover Host 在确认所有 host components 均处于 STOPPED、INSTALL_FAILED 或 INIT 后，批量把全部 components 依次置为 \`INIT\`、\`INSTALLED\`；Kerberos 集群追加该 host keytab regeneration | [source](../03-hosts.md#L39) |
| `HOST-DETAIL-006` | `03-hosts.md` | Host 详情与主机动作 | 下载 host 上全部 client configs 或单个 client config | [source](../03-hosts.md#L40) |
| `HOST-DETAIL-007` | `03-hosts.md` | Host 详情与主机动作 | 重新生成该 host 的 Kerberos keytabs | [source](../03-hosts.md#L41) |
| `HOST-DETAIL-008` | `03-hosts.md` | Host 详情与主机动作 | 从 Host Actions 启动、停止或重启该 host 上全部可操作的非 client components | [source](../03-hosts.md#L42) |
| `HOST-DETAIL-009` | `03-hosts.md` | Host 详情与主机动作 | Check Host 对当前 host 发起 pre-installed/environment checks，轮询 task 并按 JDK、repository、disk、THP 等类别显示 warnings，可 rerun | [source](../03-hosts.md#L43) |
| `HOST-DETAIL-010` | `03-hosts.md` | Host 详情与主机动作 | Host Summary 中按 service/log level 显示的 log-count donut | [source](../03-hosts.md#L44) |
| `HOST-COMP-001` | `03-hosts.md` | Host Component 动作 | 启动、停止、重启 component | [source](../03-hosts.md#L50) |
| `HOST-COMP-002` | `03-hosts.md` | Host Component 动作 | 安装/重新安装 component 或 client | [source](../03-hosts.md#L51) |
| `HOST-COMP-003` | `03-hosts.md` | Host Component 动作 | 添加可选 component 到 host | [source](../03-hosts.md#L52) |
| `HOST-COMP-004` | `03-hosts.md` | Host Component 动作 | 删除 component | [source](../03-hosts.md#L53) |
| `HOST-COMP-005` | `03-hosts.md` | Host Component 动作 | Decommission/Recommission slave component | [source](../03-hosts.md#L54) |
| `HOST-COMP-006` | `03-hosts.md` | Host Component 动作 | 进入/退出 Component Maintenance Mode | [source](../03-hosts.md#L55) |
| `HOST-COMP-007` | `03-hosts.md` | Host Component 动作 | Refresh configs / refresh component configs | [source](../03-hosts.md#L56) |
| `HOST-COMP-008` | `03-hosts.md` | Host Component 动作 | 执行 stack 定义的 custom command | [source](../03-hosts.md#L57) |
| `HOST-COMP-009` | `03-hosts.md` | Host Component 动作 | Move Master 进入 Reassign Master wizard | [source](../03-hosts.md#L58) |
| `HOST-COMP-010` | `03-hosts.md` | Host Component 动作 | Upgrade component / 安装 host stack version | [source](../03-hosts.md#L59) |
| `HOST-COMP-011` | `03-hosts.md` | Host Component 动作 | Host Component 为 \`UPGRADE_FAILED\` 时，状态图标和 action 菜单静态代码会显示 Re-upgrade，确认后尝试重新提交 component upgrade | [source](../03-hosts.md#L60) |
| `HOST-TAB-001` | `03-hosts.md` | 详情子页 | Configs 按 service 展示该 host 的 config group/override 与属性 | [source](../03-hosts.md#L66) |
| `HOST-TAB-002` | `03-hosts.md` | 详情子页 | Host Alerts 列出该 host 的 alert instances，可跳 service 和 definition | [source](../03-hosts.md#L67) |
| `HOST-TAB-003` | `03-hosts.md` | 详情子页 | Stack Versions 列出每个 repo version 的 host 状态并可发起安装 | [source](../03-hosts.md#L68) |
| `HOST-TAB-004` | `03-hosts.md` | 详情子页 | Logs 列 service/component log files，打开/尾随日志并跳 Log Search UI | [source](../03-hosts.md#L69) |
| `HOST-LOG-001` | `03-hosts.md` | Host Logs 与 Log Search 外链 | \`/main/hosts/:host_id/logs:query\` 展示该 host 的 component log file 元数据，支持按 service/component/file 选择 | [source](../03-hosts.md#L75) |
| `HOST-LOG-002` | `03-hosts.md` | Host Logs 与 Log Search 外链 | 打开日志 tail popup，选择 tail 数量并加载文本；可复制或把当前内容写入新窗口 | [source](../03-hosts.md#L76) |
| `HOST-LOG-003` | `03-hosts.md` | Host Logs 与 Log Search 外链 | 从 host log 行或 tail popup 打开 Log Search UI，并带 host、component、path/query 参数 | [source](../03-hosts.md#L77) |
| `HOST-LOG-004` | `03-hosts.md` | Host Logs 与 Log Search 外链 | Background Operation、wizard 和普通 logs popup 可复制 stdout/stderr，或把当前文本/HTML写入新窗口 | [source](../03-hosts.md#L78) |
| `HOST-TAB-005` | `03-hosts.md` | Host Logs 与 Log Search 外链 | Metrics route/tab | [source](../03-hosts.md#L79) |
| `HOST-ADD-001` | `03-hosts.md` | Add Host Wizard | Step 1 Install Options | [source](../03-hosts.md#L87) |
| `HOST-ADD-002` | `03-hosts.md` | Add Host Wizard | Step 2 Confirm Hosts | [source](../03-hosts.md#L88) |
| `HOST-ADD-003` | `03-hosts.md` | Add Host Wizard | Step 3 Assign Slaves and Clients | [source](../03-hosts.md#L89) |
| `HOST-ADD-004` | `03-hosts.md` | Add Host Wizard | Step 4 Config Groups | [source](../03-hosts.md#L90) |
| `HOST-ADD-005` | `03-hosts.md` | Add Host Wizard | Step 5 Review | [source](../03-hosts.md#L91) |
| `HOST-ADD-006` | `03-hosts.md` | Add Host Wizard | Step 6 Install, Start and Test | [source](../03-hosts.md#L92) |
| `HOST-ADD-007` | `03-hosts.md` | Add Host Wizard | Step 7 Summary | [source](../03-hosts.md#L93) |
| `HOST-ADD-008` | `03-hosts.md` | Add Host Wizard | Wizard 恢复与取消 | [source](../03-hosts.md#L94) |
| `SVC-NAV-001` | `04-services-configs.md` | Service 导航与全局动作 | 左侧列出已安装 services、health/state、restart required 和 alerts，选择后进入 Summary | [source](../04-services-configs.md#L11) |
| `SVC-ALL-001` | `04-services-configs.md` | Service 导航与全局动作 | Add Service 打开 7 步向导 | [source](../04-services-configs.md#L12) |
| `SVC-ALL-002` | `04-services-configs.md` | Service 导航与全局动作 | Start All Services | [source](../04-services-configs.md#L13) |
| `SVC-ALL-003` | `04-services-configs.md` | Service 导航与全局动作 | Stop All Services | [source](../04-services-configs.md#L14) |
| `SVC-ALL-004` | `04-services-configs.md` | Service 导航与全局动作 | Restart All Required | [source](../04-services-configs.md#L15) |
| `SVC-ALL-005` | `04-services-configs.md` | Service 导航与全局动作 | 下载所有 services 的 client configs | [source](../04-services-configs.md#L16) |
| `SVC-ACT-001` | `04-services-configs.md` | 单 Service 动作 | Start/Stop/Restart Service | [source](../04-services-configs.md#L22) |
| `SVC-ACT-002` | `04-services-configs.md` | 单 Service 动作 | Run Service Check | [source](../04-services-configs.md#L23) |
| `SVC-ACT-003` | `04-services-configs.md` | 单 Service 动作 | Service 进入/退出 Maintenance Mode | [source](../04-services-configs.md#L24) |
| `SVC-ACT-004` | `04-services-configs.md` | 单 Service 动作 | Restart Required Components | [source](../04-services-configs.md#L25) |
| `SVC-ACT-005` | `04-services-configs.md` | 单 Service 动作 | Delete Service | [source](../04-services-configs.md#L26) |
| `SVC-ACT-006` | `04-services-configs.md` | 单 Service 动作 | Refresh YARN Queues | [source](../04-services-configs.md#L27) |
| `SVC-ACT-007` | `04-services-configs.md` | 单 Service 动作 | HDFS Rebalance 启动/停止 | [source](../04-services-configs.md#L28) |
| `SVC-ACT-008` | `04-services-configs.md` | 单 Service 动作 | Knox LDAP start/stop、HBase replication start/stop 等 service-specific command | [source](../04-services-configs.md#L29) |
| `SVC-ACT-009` | `04-services-configs.md` | 单 Service 动作 | 执行任意 stack custom command | [source](../04-services-configs.md#L30) |
| `SVC-ACT-010` | `04-services-configs.md` | 单 Service 动作 | 启用 HA/Federation、Manage JournalNodes、HAWQ standby 或 Move Master | [source](../04-services-configs.md#L31) |
| `SVC-ACT-011` | `04-services-configs.md` | 单 Service 动作 | 下载当前 service 的全部 client configs 或指定 client component config | [source](../04-services-configs.md#L32) |
| `SVC-SUM-001` | `04-services-configs.md` | Summary 非 Metrics | 显示 service 状态、master/slave/client components、host 分布、alerts、maintenance 和 restart required | [source](../04-services-configs.md#L38) |
| `SVC-SUM-002` | `04-services-configs.md` | Summary 非 Metrics | component/host 链接跳 Host detail 或按 component 筛 Hosts | [source](../04-services-configs.md#L39) |
| `SVC-SUM-003` | `04-services-configs.md` | Summary 非 Metrics | 对单 component 执行 start/stop/restart、maintenance、custom command | [source](../04-services-configs.md#L40) |
| `SVC-SUM-004` | `04-services-configs.md` | Summary 非 Metrics | Flume agent start/stop | [source](../04-services-configs.md#L41) |
| `SVC-SUM-005` | `04-services-configs.md` | Summary 非 Metrics | Service-specific Quick Links 跳外部 Web UI；Ambari View 是另一套 route/iframe 机制 | [source](../04-services-configs.md#L42) |
| `SVC-QL-001` | `04-services-configs.md` | Quick Links 与浏览器外链 | 从当前 stack service 的已合并 quicklinks descriptor 加载链接定义，客户端最终只保留 \`visible=true\` 的项 | [source](../04-services-configs.md#L48) |
| `SVC-QL-002` | `04-services-configs.md` | Quick Links 与浏览器外链 | 根据 descriptor 的 protocol checks、当前 site properties 和 \`hdfs-site/dfs.http.policy\` 选择 HTTP/HTTPS | [source](../04-services-configs.md#L49) |
| `SVC-QL-003` | `04-services-configs.md` | Quick Links 与浏览器外链 | 为 descriptor 关联 component 加载内部 host 到 public host 映射，并按单 host、多 host、多 nameservice/master group生成链接组 | [source](../04-services-configs.md#L50) |
| `SVC-QL-004` | `04-services-configs.md` | Quick Links 与浏览器外链 | 从 config property、正则和默认值解析端口，把 \`${config-type/property-name}\` placeholder 与可选登录用户名代入 URL template | [source](../04-services-configs.md#L51) |
| `SVC-QL-005` | `04-services-configs.md` | Quick Links 与浏览器外链 | 应用 service 特例：Ranger 优先使用 \`admin-properties/policymgr_external_url\`；MapReduce2 可从配置中的 host:port 反查 public host；Oozie 只列 STARTED server | [source](../04-services-configs.md#L52) |
| `SVC-QL-006` | `04-services-configs.md` | Quick Links 与浏览器外链 | 对 HDFS NameNode、YARN ResourceManager、HBase Master 标注 Active/Standby 并按 group 展示 | [source](../04-services-configs.md#L53) |
| `SVC-QL-007` | `04-services-configs.md` | Quick Links 与浏览器外链 | 点击链接以 \`target="_blank"\` 打开最终外部 URL | [source](../04-services-configs.md#L54) |
| `SVC-CONFIG-001` | `04-services-configs.md` | Service Configs | 按 stack theme、tab、section、category 展示 service 配置，支持文本、密码、checkbox、select、radio、slider、目录、数据库等控件 | [source](../04-services-configs.md#L60) |
| `SVC-CONFIG-002` | `04-services-configs.md` | Service Configs | 展示当前值、推荐值、默认值、是否 required、只读、错误/警告、单位和描述 | [source](../04-services-configs.md#L61) |
| `SVC-CONFIG-003` | `04-services-configs.md` | Service Configs | 编辑配置并执行前端校验、依赖配置联动和 stack advisor recommendations；普通属性与 theme widget 支持 value/list 选择、widget/text 编辑切换、设置推荐值、undo saved value 和切换 final flag | [source](../04-services-configs.md#L62) |
| `SVC-CONFIG-004` | `04-services-configs.md` | Service Configs | 保存新 config version，填写 note，显示 changed properties 和 dependent services | [source](../04-services-configs.md#L63) |
| `SVC-CONFIG-005` | `04-services-configs.md` | Service Configs | 未保存修改时离开 route，弹出 Save/Discard/Cancel | [source](../04-services-configs.md#L64) |
| `SVC-CONFIG-006` | `04-services-configs.md` | Service Configs | 浏览 config version 历史，选择旧版、比较版本、显示新增/删除/修改项 | [source](../04-services-configs.md#L65) |
| `SVC-CONFIG-007` | `04-services-configs.md` | Service Configs | 将历史 version 设为 current / revert | [source](../04-services-configs.md#L66) |
| `SVC-CONFIG-008` | `04-services-configs.md` | Service Configs | Host override：为 config group 创建 override、删除 override、恢复 saved/default value，并可对 override 独立设置推荐值与 final flag | [source](../04-services-configs.md#L67) |
| `SVC-CONFIG-009` | `04-services-configs.md` | Service Configs | 显示并操作 restart required：按 service/host/component restart，支持 rolling restart | [source](../04-services-configs.md#L68) |
| `SVC-CONFIG-010` | `04-services-configs.md` | Service Configs | 测试数据库连接：创建 custom action，查询 request 中的 task ID，再轮询 task 结果 | [source](../04-services-configs.md#L69) |
| `SVC-CONFIG-011` | `04-services-configs.md` | Service Configs | 在允许的 Advanced category 中新增自定义 property，支持单条 key/value 与多行 \`key=value\` bulk mode；可删除 user property | [source](../04-services-configs.md#L70) |
| `CFG-GROUP-001` | `04-services-configs.md` | Config Groups | 列出 service 的 default 与非 default config groups、hosts 数和属性 overrides | [source](../04-services-configs.md#L76) |
| `CFG-GROUP-002` | `04-services-configs.md` | Config Groups | 创建 group，填写名称/描述并选择 hosts | [source](../04-services-configs.md#L77) |
| `CFG-GROUP-003` | `04-services-configs.md` | Config Groups | 重命名、修改描述、复制 group | [source](../04-services-configs.md#L78) |
| `CFG-GROUP-004` | `04-services-configs.md` | Config Groups | 添加/移除 hosts；移动 host 时从原 group 调整 | [source](../04-services-configs.md#L79) |
| `CFG-GROUP-005` | `04-services-configs.md` | Config Groups | 删除非 default group | [source](../04-services-configs.md#L80) |
| `CFG-GROUP-006` | `04-services-configs.md` | Config Groups | 查看 group properties 并进入对应 Configs 编辑 | [source](../04-services-configs.md#L81) |
| `SVC-ADD-001` | `04-services-configs.md` | Add Service Wizard | Choose Services | [source](../04-services-configs.md#L87) |
| `SVC-ADD-002` | `04-services-configs.md` | Add Service Wizard | Assign Masters | [source](../04-services-configs.md#L88) |
| `SVC-ADD-003` | `04-services-configs.md` | Add Service Wizard | Assign Slaves and Clients | [source](../04-services-configs.md#L89) |
| `SVC-ADD-004` | `04-services-configs.md` | Add Service Wizard | Customize Services | [source](../04-services-configs.md#L90) |
| `SVC-ADD-005` | `04-services-configs.md` | Add Service Wizard | Review | [source](../04-services-configs.md#L91) |
| `SVC-ADD-006` | `04-services-configs.md` | Add Service Wizard | Install, Start and Test | [source](../04-services-configs.md#L92) |
| `SVC-ADD-007` | `04-services-configs.md` | Add Service Wizard | Summary | [source](../04-services-configs.md#L93) |
| `SVC-ADD-008` | `04-services-configs.md` | Add Service Wizard | 恢复与互斥 | [source](../04-services-configs.md#L94) |
| `SVC-MOVE-001` | `04-services-configs.md` | Reassign Master Wizard | Get Started / Assign Master | [source](../04-services-configs.md#L100) |
| `SVC-MOVE-002` | `04-services-configs.md` | Reassign Master Wizard | Review | [source](../04-services-configs.md#L101) |
| `SVC-MOVE-003` | `04-services-configs.md` | Reassign Master Wizard | Configure Component | [source](../04-services-configs.md#L102) |
| `SVC-MOVE-004` | `04-services-configs.md` | Reassign Master Wizard | Manual Commands | [source](../04-services-configs.md#L103) |
| `SVC-MOVE-005` | `04-services-configs.md` | Reassign Master Wizard | Start and Test Services | [source](../04-services-configs.md#L104) |
| `SVC-MOVE-006` | `04-services-configs.md` | Reassign Master Wizard | Rollback | [source](../04-services-configs.md#L105) |
| `ALERT-LIST-001` | `05-alerts.md` | Definitions 列表与快速入口 | 列出 Alert Definitions，显示 service/component、name、state、enabled、latest status、last checked/changed、notification 和 check count | [source](../05-alerts.md#L9) |
| `ALERT-LIST-002` | `05-alerts.md` | Definitions 列表与快速入口 | 按 definition name、service、component、state、enabled 等筛选和排序 | [source](../05-alerts.md#L10) |
| `ALERT-LIST-003` | `05-alerts.md` | Definitions 列表与快速入口 | 点击 definition 进入详情；从 service/host/全局 critical-warning 弹窗进入对应 definition 或全部 Alerts | [source](../05-alerts.md#L11) |
| `ALERT-LIST-004` | `05-alerts.md` | Definitions 列表与快速入口 | 在列表直接启用/禁用 definition | [source](../05-alerts.md#L12) |
| `ALERT-LIST-005` | `05-alerts.md` | Definitions 列表与快速入口 | Actions 菜单提供 Create Alert、Manage Groups、Manage Notifications、Manage Settings | [source](../05-alerts.md#L13) |
| `ALERT-DEF-001` | `05-alerts.md` | Definition 详情与实例 | 展示 definition label、description/type、service/component、scope/source、interval、threshold/config、groups、notification、enabled 和 repeat tolerance | [source](../05-alerts.md#L19) |
| `ALERT-DEF-002` | `05-alerts.md` | Definition 详情与实例 | 编辑 label | [source](../05-alerts.md#L20) |
| `ALERT-DEF-003` | `05-alerts.md` | Definition 详情与实例 | 编辑通用 alert configs/thresholds | [source](../05-alerts.md#L21) |
| `ALERT-DEF-004` | `05-alerts.md` | Definition 详情与实例 | 启用/禁用 definition | [source](../05-alerts.md#L22) |
| `ALERT-DEF-005` | `05-alerts.md` | Definition 详情与实例 | 编辑 repeat tolerance/check count；接受 1 到 99 或隐藏 sentinel \`DEBUG\`，也可关闭 repeat tolerance | [source](../05-alerts.md#L23) |
| `ALERT-DEF-006` | `05-alerts.md` | Definition 详情与实例 | 删除自定义 definition | [source](../05-alerts.md#L24) |
| `ALERT-DEF-007` | `05-alerts.md` | Definition 详情与实例 | 列出当前 instances，显示 service/host、state、last check、response；可跳 service 或 host alerts | [source](../05-alerts.md#L25) |
| `ALERT-DEF-008` | `05-alerts.md` | Definition 详情与实例 | 打开 instance response/log 文本 | [source](../05-alerts.md#L26) |
| `ALERT-DEF-009` | `05-alerts.md` | Definition 详情与实例 | 查询最近 24 小时 instance history，并按 host 显示返回记录数量 | [source](../05-alerts.md#L27) |
| `ALERT-DEF-010` | `05-alerts.md` | Definition 详情与实例 | 编辑中离开 route 时弹 Save/Discard/Cancel | [source](../05-alerts.md#L28) |
| `ALERT-CREATE-001` | `05-alerts.md` | 创建 Alert Definition | Step 1 Choose Alert Type | [source](../05-alerts.md#L34) |
| `ALERT-CREATE-002` | `05-alerts.md` | 创建 Alert Definition | Step 2 Define Alert and Thresholds | [source](../05-alerts.md#L35) |
| `ALERT-CREATE-003` | `05-alerts.md` | 创建 Alert Definition | Step 3 Review | [source](../05-alerts.md#L36) |
| `ALERT-CREATE-004` | `05-alerts.md` | 创建 Alert Definition | Done 创建 definition 并返回 Alerts | [source](../05-alerts.md#L37) |
| `ALERT-CREATE-005` | `05-alerts.md` | 创建 Alert Definition | 向导前后导航 | [source](../05-alerts.md#L38) |
| `ALERT-GROUP-001` | `05-alerts.md` | Alert Groups | 按 service 列出 groups、description、definitions 和 notification targets | [source](../05-alerts.md#L44) |
| `ALERT-GROUP-002` | `05-alerts.md` | Alert Groups | 创建 group | [source](../05-alerts.md#L45) |
| `ALERT-GROUP-003` | `05-alerts.md` | Alert Groups | 重命名、修改描述、复制 group | [source](../05-alerts.md#L46) |
| `ALERT-GROUP-004` | `05-alerts.md` | Alert Groups | 给 group 添加/移除 definitions，支持按 service/component 筛选与多选 | [source](../05-alerts.md#L47) |
| `ALERT-GROUP-005` | `05-alerts.md` | Alert Groups | 给 group 关联/取消 notification targets | [source](../05-alerts.md#L48) |
| `ALERT-GROUP-006` | `05-alerts.md` | Alert Groups | 删除非 default group | [source](../05-alerts.md#L49) |
| `ALERT-GROUP-007` | `05-alerts.md` | Alert Groups | 保存时先并发删除，全部回调后再并发更新和创建；无错误时关闭并刷新 notifications | [source](../05-alerts.md#L50) |
| `ALERT-NOTIFY-001` | `05-alerts.md` | Alert Notifications | 列出 notification name、type、enabled、global/group scope、severity 和 description | [source](../05-alerts.md#L56) |
| `ALERT-NOTIFY-002` | `05-alerts.md` | Alert Notifications | 创建 Email notification | [source](../05-alerts.md#L57) |
| `ALERT-NOTIFY-003` | `05-alerts.md` | Alert Notifications | 创建 SNMP v1/v2c notification | [source](../05-alerts.md#L58) |
| `ALERT-NOTIFY-004` | `05-alerts.md` | Alert Notifications | 创建 Custom SNMP notification | [source](../05-alerts.md#L59) |
| `ALERT-NOTIFY-005` | `05-alerts.md` | Alert Notifications | 创建 Alert Script notification | [source](../05-alerts.md#L60) |
| `ALERT-NOTIFY-006` | `05-alerts.md` | Alert Notifications | 设置 Global 或选择 groups，选择 Critical/Warning/OK/Unknown 等 severity | [source](../05-alerts.md#L61) |
| `ALERT-NOTIFY-007` | `05-alerts.md` | Alert Notifications | 编辑或复制 notification | [source](../05-alerts.md#L62) |
| `ALERT-NOTIFY-008` | `05-alerts.md` | Alert Notifications | 启用/禁用 notification | [source](../05-alerts.md#L63) |
| `ALERT-NOTIFY-009` | `05-alerts.md` | Alert Notifications | 删除 notification | [source](../05-alerts.md#L64) |
| `ALERT-NOTIFY-010` | `05-alerts.md` | Alert Notifications | 添加/删除自定义 property | [source](../05-alerts.md#L65) |
| `ALERT-SET-001` | `05-alerts.md` | 全局告警设置 | Alert check count/repeat tolerance 可按 definition 编辑 | [source](../05-alerts.md#L71) |
| `ALERT-SET-002` | `05-alerts.md` | 全局告警设置 | Manage Alert Settings 修改 \`cluster-env.alerts_repeat_tolerance\`，接受 1 到 99 或 \`DEBUG\` 后调用 config save | [source](../05-alerts.md#L72) |
| `ALERT-SET-003` | `05-alerts.md` | 全局告警设置 | Service/cluster maintenance 与 toggle alerts 会改变告警呈现和通知 | [source](../05-alerts.md#L73) |
| `STACK-SVC-001` | `06-stack-upgrades-admin.md` | Stack Services 与仓库 | 列当前 stack 的 services、版本、安装状态和 repository version 信息 | [source](../06-stack-upgrades-admin.md#L9) |
| `STACK-SVC-002` | `06-stack-upgrades-admin.md` | Stack Services 与仓库 | 从未安装 service 跳 Add Service | [source](../06-stack-upgrades-admin.md#L10) |
| `STACK-SVC-003` | `06-stack-upgrades-admin.md` | Stack Services 与仓库 | 查看各 OS repository ID、base URL、mirrors list | [source](../06-stack-upgrades-admin.md#L11) |
| `STACK-SVC-004` | `06-stack-upgrades-admin.md` | Stack Services 与仓库 | 编辑 repository base URL，恢复原值、清 local repository、保存 | [source](../06-stack-upgrades-admin.md#L12) |
| `STACK-SVC-005` | `06-stack-upgrades-admin.md` | Stack Services 与仓库 | 验证 repository URL，可选择 skip validation | [source](../06-stack-upgrades-admin.md#L13) |
| `VER-LIST-001` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | 列出 repo versions，显示 display name/version、stack、type、host counts、service support 和状态 | [source](../06-stack-upgrades-admin.md#L19) |
| `VER-LIST-002` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | 按 Not Installed、All、Upgrade Ready、Installed、Current、In Process、Ready to Finalize 筛选 | [source](../06-stack-upgrades-admin.md#L20) |
| `VER-LIST-003` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | 展开 version 详情，查看 services、repositories、hosts 状态和不可升级原因 | [source](../06-stack-upgrades-admin.md#L21) |
| `VER-LIST-004` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | 按 version status 查看 Current/Installed/Not Installed hosts，并跳 Hosts 过滤结果 | [source](../06-stack-upgrades-admin.md#L22) |
| `VER-LIST-005` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | Install/Reinstall Packages 到全体适用 hosts | [source](../06-stack-upgrades-admin.md#L23) |
| `VER-LIST-006` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | 对单 host 安装 version | [source](../06-stack-upgrades-admin.md#L24) |
| `VER-LIST-007` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | 处理 OUT_OF_SYNC component：reinstall 或 remove | [source](../06-stack-upgrades-admin.md#L25) |
| `VER-LIST-008` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | Hide 未使用/安装失败的 repository version | [source](../06-stack-upgrades-admin.md#L26) |
| `VER-LIST-009` | `06-stack-upgrades-admin.md` | Versions 列表与包安装 | Manage Versions 跳独立 Admin View | [source](../06-stack-upgrades-admin.md#L27) |
| `UPG-START-001` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | 判断目标 version 是否 compatible 并加载支持的 upgrade types | [source](../06-stack-upgrades-admin.md#L33) |
| `UPG-START-002` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | Upgrade Options 选择 Rolling、Express 或 Host Ordered | [source](../06-stack-upgrades-admin.md#L34) |
| `UPG-START-003` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | 设置 slave component failure 与 service check failure tolerance | [source](../06-stack-upgrades-admin.md#L35) |
| `UPG-START-004` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | 运行/rerun Pre-Upgrade Checks，按 Required/Warning/Bypassed 展示 | [source](../06-stack-upgrades-admin.md#L36) |
| `UPG-START-005` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | 执行额外 custom cluster checks：maintenance、host heartbeat、previous upgrade、component installation 和 service checks | [source](../06-stack-upgrades-admin.md#L37) |
| `UPG-START-006` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | 确认后创建 Upgrade | [source](../06-stack-upgrades-admin.md#L38) |
| `UPG-START-007` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | 从 aborted/failed upgrade 发起 Downgrade 或 retry Upgrade | [source](../06-stack-upgrades-admin.md#L39) |
| `UPG-START-008` | `06-stack-upgrades-admin.md` | 发起 Upgrade/Downgrade | Patch/Maint upgrade 和 revert | [source](../06-stack-upgrades-admin.md#L40) |
| `UPG-RUN-001` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | 加载 upgrade groups/items/tasks，显示总进度、当前 group、item 和状态 | [source](../06-stack-upgrades-admin.md#L46) |
| `UPG-RUN-002` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | 展开 group/item/task 详情，查看 host、role、command、stdout/stderr，并可 Copy 或在新窗口打开已加载日志 | [source](../06-stack-upgrades-admin.md#L47) |
| `UPG-RUN-003` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | HOLDING/manual step 显示说明，用户确认已完成后 Proceed，将当前 UpgradeItem 设为 \`COMPLETED\` | [source](../06-stack-upgrades-admin.md#L48) |
| `UPG-RUN-004` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | 失败 item 可 Retry、Skip/Ignore and Proceed 或查看失败 hosts | [source](../06-stack-upgrades-admin.md#L49) |
| `UPG-RUN-005` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | Pause/Suspend Upgrade 或 Downgrade | [source](../06-stack-upgrades-admin.md#L50) |
| `UPG-RUN-006` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | Resume paused Upgrade/Downgrade | [source](../06-stack-upgrades-admin.md#L51) |
| `UPG-RUN-007` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | Abort 当前 Upgrade 是发起 Downgrade 的前置操作，或 previous-upgrade custom check 的 legacy 修复动作 | [source](../06-stack-upgrades-admin.md#L52) |
| `UPG-RUN-008` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | 关闭 progress modal 时升级继续在后台；返回 Versions 可重新打开 | [source](../06-stack-upgrades-admin.md#L53) |
| `UPG-RUN-009` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | 其他用户发起升级时当前用户显示 initiator 和只读/non-wizard 状态 | [source](../06-stack-upgrades-admin.md#L54) |
| `UPG-RUN-010` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | Upgrade 期间 suppress notifications、限制部分 host/service operations | [source](../06-stack-upgrades-admin.md#L55) |
| `UPG-RUN-011` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | 加载 skipped service checks 和 failed hosts，最终确认风险 | [source](../06-stack-upgrades-admin.md#L56) |
| `UPG-RUN-012` | `06-stack-upgrades-admin.md` | Upgrade 执行状态机 | Finalize Upgrade/Downgrade 将最后一个 manual/finalize UpgradeItem 设为 \`COMPLETED\`；Finalize Later 复用 Pause/Suspend 流程 | [source](../06-stack-upgrades-admin.md#L57) |
| `UPG-HIST-001` | `06-stack-upgrades-admin.md` | Upgrade History | 列出 Upgrade/Downgrade 历史，显示 direction、type、repository/name/type、service from/to versions、status、start/duration/end time | [source](../06-stack-upgrades-admin.md#L63) |
| `UPG-HIST-002` | `06-stack-upgrades-admin.md` | Upgrade History | 按九类筛选：All、Upgrade、Downgrade，以及分别针对 Upgrade/Downgrade 的 Successful、Aborted、Failed | [source](../06-stack-upgrades-admin.md#L64) |
| `UPG-HIST-003` | `06-stack-upgrades-admin.md` | Upgrade History | 选择历史记录后通过 request ID 加载 summary、groups/items/task 状态 | [source](../06-stack-upgrades-admin.md#L65) |
| `ADMIN-ACCT-001` | `06-stack-upgrades-admin.md` | Service Accounts | \`/main/admin/serviceAccounts\` 汇总所有 configs 中 displayType 为 \`user\`、category 为 \`Users and Groups\` 的 service users/groups | [source](../06-stack-upgrades-admin.md#L71) |
| `ADMIN-ACCT-002` | `06-stack-upgrades-admin.md` | Service Accounts | 按定义顺序只读显示 service account 名和值 | [source](../06-stack-upgrades-admin.md#L72) |
| `ADMIN-AUTO-001` | `06-stack-upgrades-admin.md` | Service Auto Start | \`/main/admin/serviceAutoStart\` 加载全局 auto-start/recovery 开关 | [source](../06-stack-upgrades-admin.md#L78) |
| `ADMIN-AUTO-002` | `06-stack-upgrades-admin.md` | Service Auto Start | 按 service 分组列 restartable 且已安装 components，逐项开关 \`recovery_enabled\` | [source](../06-stack-upgrades-admin.md#L79) |
| `ADMIN-AUTO-003` | `06-stack-upgrades-admin.md` | Service Auto Start | 保存全局 \`cluster-env.recovery_enabled\` 和变更 component 集合 | [source](../06-stack-upgrades-admin.md#L80) |
| `ADMIN-AUTO-004` | `06-stack-upgrades-admin.md` | Service Auto Start | 离页有未保存修改时选择 Save/Discard/Cancel | [source](../06-stack-upgrades-admin.md#L81) |
| `INST-MODE-001` | `07-cluster-installation.md` | 安装模式与复用边界 | 全新集群 | [source](../07-cluster-installation.md#L16) |
| `INST-MODE-002` | `07-cluster-installation.md` | 安装模式与复用边界 | Add Host | [source](../07-cluster-installation.md#L17) |
| `INST-MODE-003` | `07-cluster-installation.md` | 安装模式与复用边界 | Add Service | [source](../07-cluster-installation.md#L18) |
| `INST-MODE-004` | `07-cluster-installation.md` | 安装模式与复用边界 | Public Repository | [source](../07-cluster-installation.md#L19) |
| `INST-MODE-005` | `07-cluster-installation.md` | 安装模式与复用边界 | Local Repository + VDF/XML file | [source](../07-cluster-installation.md#L20) |
| `INST-MODE-006` | `07-cluster-installation.md` | 安装模式与复用边界 | Local Repository + VDF URL | [source](../07-cluster-installation.md#L21) |
| `INST-MODE-007` | `07-cluster-installation.md` | 安装模式与复用边界 | Linux SSH 自动 bootstrap | [source](../07-cluster-installation.md#L22) |
| `INST-MODE-008` | `07-cluster-installation.md` | 安装模式与复用边界 | 手工 Agent 注册 | [source](../07-cluster-installation.md#L23) |
| `INST-MODE-009` | `07-cluster-installation.md` | 安装模式与复用边界 | Kerberized Add Host | [source](../07-cluster-installation.md#L24) |
| `INST-MODE-010` | `07-cluster-installation.md` | 安装模式与复用边界 | Kerberized Add Service | [source](../07-cluster-installation.md#L25) |
| `INST-MODE-011` | `07-cluster-installation.md` | 安装模式与复用边界 | HDPWIN PowerShell 自动 bootstrap | [source](../07-cluster-installation.md#L26) |
| `INST-ENTRY-001` | `07-cluster-installation.md` | 三类向导入口门禁 | 全新 Installer \`/installer/step0\` 到 \`step10\` | [source](../07-cluster-installation.md#L72) |
| `INST-ENTRY-002` | `07-cluster-installation.md` | 三类向导入口门禁 | Add Service \`/main/service/add/step1\` 到 \`step7\` | [source](../07-cluster-installation.md#L73) |
| `INST-ENTRY-003` | `07-cluster-installation.md` | 三类向导入口门禁 | Add Host \`/main/host/add/step1\` 到 \`step7\` | [source](../07-cluster-installation.md#L74) |
| `INST-FLOW-001` | `07-cluster-installation.md` | 进入、恢复与取消 | 登录后无已安装集群进入 Installer；已有未完成状态恢复对应 step | [source](../07-cluster-installation.md#L80) |
| `INST-FLOW-002` | `07-cluster-installation.md` | 进入、恢复与取消 | 每步保存 currentStep、已选 stack/services/hosts/components/configs/recommendations 到 local DB/persist | [source](../07-cluster-installation.md#L81) |
| `INST-FLOW-003` | `07-cluster-installation.md` | 进入、恢复与取消 | 另一窗口或崩溃后根据 \`wizardControllerName\`、clusterState 和 currentStep 恢复 | [source](../07-cluster-installation.md#L82) |
| `INST-FLOW-004` | `07-cluster-installation.md` | 进入、恢复与取消 | Cancel Install 显示确认，确认后只路由到 \`/adminView\` | [source](../07-cluster-installation.md#L83) |
| `INST-FLOW-005` | `07-cluster-installation.md` | 进入、恢复与取消 | 防止 Back/Next 双击；request 运行中禁用导航 | [source](../07-cluster-installation.md#L84) |
| `INST-FLOW-006` | `07-cluster-installation.md` | 进入、恢复与取消 | 向导所有权与多窗口恢复 | [source](../07-cluster-installation.md#L85) |
| `INST-0-001` | `07-cluster-installation.md` | Step 0 Get Started | 输入 cluster name | [source](../07-cluster-installation.md#L91) |
| `INST-0-002` | `07-cluster-installation.md` | Step 0 Get Started | 加载可安装 stack 列表并初始化向导数据 | [source](../07-cluster-installation.md#L92) |
| `INST-1-001` | `07-cluster-installation.md` | Step 1 Select Version | 选择 stack 与 version definition | [source](../07-cluster-installation.md#L98) |
| `INST-1-002` | `07-cluster-installation.md` | Step 1 Select Version | 选择 Public Repository | [source](../07-cluster-installation.md#L99) |
| `INST-1-003` | `07-cluster-installation.md` | Step 1 Select Version | 选择 Local Repository，通过上传 VDF/XML 或输入 VDF URL 添加 version | [source](../07-cluster-installation.md#L100) |
| `INST-1-004` | `07-cluster-installation.md` | Step 1 Select Version | 按 OS 编辑 Base URL、添加/移除 OS、恢复默认或清空 | [source](../07-cluster-installation.md#L101) |
| `INST-1-005` | `07-cluster-installation.md` | Step 1 Select Version | Retry repository validation | [source](../07-cluster-installation.md#L102) |
| `INST-1-006` | `07-cluster-installation.md` | Step 1 Select Version | 校验 Ambari Server JDK 与 version definition 的 \`min_jdk\`/\`max_jdk\` 范围 | [source](../07-cluster-installation.md#L103) |
| `INST-1-007` | `07-cluster-installation.md` | Step 1 Select Version | Local Repository 可切换 RedHat Satellite/Spacewalk 管理仓库模式 | [source](../07-cluster-installation.md#L104) |
| `INST-2-001` | `07-cluster-installation.md` | Step 2 Install Options | 输入 host names，支持 \`[01-10]\` pattern 展开 | [source](../07-cluster-installation.md#L110) |
| `INST-2-002` | `07-cluster-installation.md` | Step 2 Install Options | 选择 Linux SSH 自动安装 Ambari Agent | [source](../07-cluster-installation.md#L111) |
| `INST-2-003` | `07-cluster-installation.md` | Step 2 Install Options | 选择手工注册 Agent | [source](../07-cluster-installation.md#L112) |
| `INST-2-004` | `07-cluster-installation.md` | Step 2 Install Options | 对无点 hostname/IP 等可疑输入弹 warning，用户可返回修改或确认继续 | [source](../07-cluster-installation.md#L113) |
| `INST-2-005` | `07-cluster-installation.md` | Step 2 Install Options | Add Host 独有 \`Skip host checks\` checkbox | [source](../07-cluster-installation.md#L114) |
| `INST-3-001` | `07-cluster-installation.md` | Step 3 Confirm Hosts | Linux SSH 或 HDPWIN PowerShell 自动模式启动 bootstrap，轮询每 host 的 RUNNING/DONE/FAILED 与日志 | [source](../07-cluster-installation.md#L120) |
| `INST-3-002` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 轮询 Agent registration，将 DONE 转 REGISTERING/REGISTERED，超时标失败 | [source](../07-cluster-installation.md#L121) |
| `INST-3-003` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 展示 host 状态分类和单 host bootstrap \`bootLog\` | [source](../07-cluster-installation.md#L122) |
| `INST-3-004` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 单个/多选失败 host Retry，重新 bootstrap/注册 | [source](../07-cluster-installation.md#L123) |
| `INST-3-005` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 单个/多选 Remove host | [source](../07-cluster-installation.md#L124) |
| `INST-3-006` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 运行 host checks：hostname resolution、last-agent-env、installed packages、existing repos、THP，并由 host info 补 OS/disk 分类 | [source](../07-cluster-installation.md#L125) |
| `INST-3-007` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 检查 JDK 并展示 host-specific warning | [source](../07-cluster-installation.md#L126) |
| `INST-3-008` | `07-cluster-installation.md` | Step 3 Confirm Hosts | 显示不在当前输入中的其他 registered hosts | [source](../07-cluster-installation.md#L127) |
| `INST-4-001` | `07-cluster-installation.md` | Step 4 Choose Services | 列出 stack installable services，单选/全选/取消，按 stack 可用 file system 分组 | [source](../07-cluster-installation.md#L133) |
| `INST-4-002` | `07-cluster-installation.md` | Step 4 Choose Services | 自动提示并选择 required dependencies | [source](../07-cluster-installation.md#L134) |
| `INST-4-003` | `07-cluster-installation.md` | Step 4 Choose Services | 校验 file system 选择、多个 DFS、Ozone/Spark/Ranger 等互斥/建议组合 | [source](../07-cluster-installation.md#L135) |
| `INST-4-004` | `07-cluster-installation.md` | Step 4 Choose Services | Choose Services 组合校验完全在客户端 | [source](../07-cluster-installation.md#L136) |
| `INST-5-001` | `07-cluster-installation.md` | Step 5 Assign Masters | Stack Advisor 推荐 master component 到 hosts 的初始布局 | [source](../07-cluster-installation.md#L142) |
| `INST-5-002` | `07-cluster-installation.md` | Step 5 Assign Masters | 用户为每个 master 更换 host，并显示当前 assignment 可匹配的校验问题 | [source](../07-cluster-installation.md#L143) |
| `INST-5-003` | `07-cluster-installation.md` | Step 5 Assign Masters | 动态 services/components 变化后清旧 recommendations 并重算 | [source](../07-cluster-installation.md#L144) |
| `INST-6-001` | `07-cluster-installation.md` | Step 6 Assign Slaves and Clients | 以 host x component 矩阵分配 slave/client，支持列 All/None | [source](../07-cluster-installation.md#L150) |
| `INST-6-002` | `07-cluster-installation.md` | Step 6 Assign Slaves and Clients | 合并 Master 与 Slave/Client selection 形成 Blueprint | [source](../07-cluster-installation.md#L151) |
| `INST-6-003` | `07-cluster-installation.md` | Step 6 Assign Slaves and Clients | 服务端校验 Blueprint，按 general/host/component 显示 ERROR 与 WARN，并在 host x component 矩阵标色 | [source](../07-cluster-installation.md#L152) |
| `INST-6-004` | `07-cluster-installation.md` | Step 6 Assign Slaves and Clients | 恢复 recommendations 和已选 hosts | [source](../07-cluster-installation.md#L153) |
| `INST-7-001` | `07-cluster-installation.md` | Step 7 Customize Services | 按 service/theme/category 配置所有 selected services | [source](../07-cluster-installation.md#L159) |
| `INST-7-002` | `07-cluster-installation.md` | Step 7 Customize Services | Accounts、Credentials、Databases、Directories 专用 tabs | [source](../07-cluster-installation.md#L160) |
| `INST-7-003` | `07-cluster-installation.md` | Step 7 Customize Services | Stack Advisor recommendations、dependent config changes、required changes | [source](../07-cluster-installation.md#L161) |
| `INST-7-004` | `07-cluster-installation.md` | Step 7 Customize Services | 测试数据库连接和外部依赖 | [source](../07-cluster-installation.md#L162) |
| `INST-7-005` | `07-cluster-installation.md` | Step 7 Customize Services | 加载已存在 host overrides/config groups（Add Service/Host 复用路径） | [source](../07-cluster-installation.md#L163) |
| `INST-7-006` | `07-cluster-installation.md` | Step 7 Customize Services | Pre-Install Checks 仅为空壳 | [source](../07-cluster-installation.md#L164) |
| `INST-7-007` | `07-cluster-installation.md` | Step 7 Customize Services | Config 改变可增加 components/改变 host assignment，并回写后续 Review blueprint | [source](../07-cluster-installation.md#L165) |
| `INST-7-008` | `07-cluster-installation.md` | Step 7 Customize Services | 后退且已有修改时确认是否丢弃 | [source](../07-cluster-installation.md#L166) |
| `INST-8-001` | `07-cluster-installation.md` | Step 8 Review | 汇总 cluster、repositories、services、masters、slave/client、hosts 和 configs | [source](../07-cluster-installation.md#L172) |
| `INST-8-002` | `07-cluster-installation.md` | Step 8 Review | Print Review | [source](../07-cluster-installation.md#L173) |
| `INST-8-003` | `07-cluster-installation.md` | Step 8 Review | Download CSV | [source](../07-cluster-installation.md#L174) |
| `INST-8-004` | `07-cluster-installation.md` | Step 8 Review | Generate Blueprint ZIP | [source](../07-cluster-installation.md#L175) |
| `INST-8-005` | `07-cluster-installation.md` | Step 8 Review | Submit 先查询所有既有 cluster | [source](../07-cluster-installation.md#L176) |
| `INST-8-008` | `07-cluster-installation.md` | Step 8 Review | 既有集群删除是非事务批量副作用 | [source](../07-cluster-installation.md#L177) |
| `INST-8-009` | `07-cluster-installation.md` | Step 8 Review | 全局 Version Definition/Repository Version 清理链 | [source](../07-cluster-installation.md#L178) |
| `INST-8-006` | `07-cluster-installation.md` | Step 8 Review | 将 cluster、services、components、configs、hosts、host-components、config groups 等创建请求按依赖加入串行 queue | [source](../07-cluster-installation.md#L179) |
| `INST-8-007` | `07-cluster-installation.md` | Step 8 Review | Local Repository 提交 selected VDF（非 dry-run），随后更新 repository OS 信息 | [source](../07-cluster-installation.md#L180) |
| `INST-9-001` | `07-cluster-installation.md` | Step 9 Install, Start and Test | 分阶段 Install、Start、Service Check，按 service/host/task 展示进度 | [source](../07-cluster-installation.md#L199) |
| `INST-9-002` | `07-cluster-installation.md` | Step 9 Install, Start and Test | 展示每 host 的 tasks、command detail、stdout/stderr/error log，可复制/新窗口 | [source](../07-cluster-installation.md#L200) |
| `INST-9-003` | `07-cluster-installation.md` | Step 9 Install, Start and Test | 按 status 分类 hosts，显示 failed hosts details | [source](../07-cluster-installation.md#L201) |
| `INST-9-004` | `07-cluster-installation.md` | Step 9 Install, Start and Test | Retry failed install | [source](../07-cluster-installation.md#L202) |
| `INST-9-005` | `07-cluster-installation.md` | Step 9 Install, Start and Test | 部署期间禁止普通 route 离开 | [source](../07-cluster-installation.md#L203) |
| `INST-9-006` | `07-cluster-installation.md` | Step 9 Install, Start and Test | Deploy Next 保存完成阶段 cluster state 并进入 Summary | [source](../07-cluster-installation.md#L204) |
| `INST-10-001` | `07-cluster-installation.md` | Step 10 Summary | 汇总各 services 的安装、启动、check 成功/警告/失败 | [source](../07-cluster-installation.md#L210) |
| `INST-10-003` | `07-cluster-installation.md` | Step 10 Summary | Summary 保留不可达 \`back\` route handler | [source](../07-cluster-installation.md#L211) |
| `INST-10-002` | `07-cluster-installation.md` | Step 10 Summary | 全新 Installer Complete 清向导、请求将 cluster provisioning state 设为 \`INSTALLED\`、clusterState 重置 \`DEFAULT\`，进入 Dashboard | [source](../07-cluster-installation.md#L212) |
| `KRB-ENTRY-001` | `08-kerberos.md` | 入口、权限与前置检查 | 加载 cluster security type；\`KERBEROS\` 显示已启用页面，否则显示 Enable | [source](../08-kerberos.md#L9) |
| `KRB-ENTRY-002` | `08-kerberos.md` | 入口、权限与前置检查 | 访问页面、显示 Enable/Disable/Edit 受权限和 feature flag 双重控制 | [source](../08-kerberos.md#L10) |
| `KRB-ENTRY-003` | `08-kerberos.md` | 入口、权限与前置检查 | Enable 前逐条展示已安装服务的特殊 warning | [source](../08-kerberos.md#L11) |
| `KRB-ENTRY-004` | `08-kerberos.md` | 入口、权限与前置检查 | 可选 Pre-Kerberize Checks | [source](../08-kerberos.md#L12) |
| `KRB-ENTRY-005` | `08-kerberos.md` | 入口、权限与前置检查 | 启动向导并登记所有者/恢复状态 | [source](../08-kerberos.md#L13) |
| `KRB-MODE-001` | `08-kerberos.md` | 四种启用模式 | Existing MIT KDC，后端值 \`mit-kdc\` | [source](../08-kerberos.md#L19) |
| `KRB-MODE-002` | `08-kerberos.md` | 四种启用模式 | Existing Active Directory，后端值 \`active-directory\` | [source](../08-kerberos.md#L20) |
| `KRB-MODE-003` | `08-kerberos.md` | 四种启用模式 | Existing IPA，后端值 \`ipa\` | [source](../08-kerberos.md#L21) |
| `KRB-MODE-004` | `08-kerberos.md` | 四种启用模式 | Manage principals and keytabs manually，后端值 \`none\` | [source](../08-kerberos.md#L22) |
| `KRB-1-001` | `08-kerberos.md` | Step 1 Get Started | 在 MIT、AD、IPA、Manual 四种模式间单选 | [source](../08-kerberos.md#L32) |
| `KRB-1-002` | `08-kerberos.md` | Step 1 Get Started | 逐项确认所选模式的部署前置条件 | [source](../08-kerberos.md#L33) |
| `KRB-2-001` | `08-kerberos.md` | Step 2 Configure Kerberos | 从 stack 加载 KERBEROS config types，按 KDC 模式过滤字段并填 \`kdc_type\` | [source](../08-kerberos.md#L39) |
| `KRB-2-002` | `08-kerberos.md` | Step 2 Configure Kerberos | 测试 KDC connection | [source](../08-kerberos.md#L40) |
| `KRB-2-003` | `08-kerberos.md` | Step 2 Configure Kerberos | 自动模式创建 KERBEROS service、KERBEROS_CLIENT service component 和所有 host-components | [source](../08-kerberos.md#L41) |
| `KRB-2-004` | `08-kerberos.md` | Step 2 Configure Kerberos | 保存 \`kerberos-env\` 等 desired configs | [source](../08-kerberos.md#L42) |
| `KRB-2-005` | `08-kerberos.md` | Step 2 Configure Kerberos | 创建 live KDC admin session/credentials | [source](../08-kerberos.md#L43) |
| `KRB-2-006` | `08-kerberos.md` | Step 2 Configure Kerberos | Manual/IPA 强制安全选项 | [source](../08-kerberos.md#L44) |
| `KRB-2-007` | `08-kerberos.md` | Step 2 Configure Kerberos | Step 2 的精确失败传播并不一致 | [source](../08-kerberos.md#L45) |
| `KRB-3-001` | `08-kerberos.md` | Step 3 Install And Test Kerberos Client | 安装 KERBEROS_CLIENT 到所有 hosts；若 service component 仍为 INIT 先把 KERBEROS service 置 INSTALLED | [source](../08-kerberos.md#L51) |
| `KRB-3-002` | `08-kerberos.md` | Step 3 Install And Test Kerberos Client | 执行 KERBEROS service check | [source](../08-kerberos.md#L52) |
| `KRB-3-003` | `08-kerberos.md` | Step 3 Install And Test Kerberos Client | 完成后检查 HEARTBEAT_LOST hosts | [source](../08-kerberos.md#L53) |
| `KRB-3-004` | `08-kerberos.md` | Step 3 Install And Test Kerberos Client | 失败任务支持 Retry，也可勾选整步 \`Ignore errors and continue\` 后继续 | [source](../08-kerberos.md#L54) |
| `KRB-3-005` | `08-kerberos.md` | Step 3 Install And Test Kerberos Client | Manual 模式完全跳过本步 | [source](../08-kerberos.md#L55) |
| `KRB-4-001` | `08-kerberos.md` | Step 4 Configure Identities | 读取 Kerberos descriptor，生成 Global、Ambari Principals 和已安装 service 的 identity 配置 | [source](../08-kerberos.md#L61) |
| `KRB-4-002` | `08-kerberos.md` | Step 4 Configure Identities | Stack Advisor 推荐 identity/config 值 | [source](../08-kerberos.md#L62) |
| `KRB-4-003` | `08-kerberos.md` | Step 4 Configure Identities | 编辑 principal/keytab/name/rule 等 descriptor 属性 | [source](../08-kerberos.md#L63) |
| `KRB-4-004` | `08-kerberos.md` | Step 4 Configure Identities | 创建或更新 cluster \`kerberos_descriptor\` artifact | [source](../08-kerberos.md#L64) |
| `KRB-4-005` | `08-kerberos.md` | Step 4 Configure Identities | 提交 descriptor 后先调用 unkerberize 清理半完成 security state，再进入 Confirm | [source](../08-kerberos.md#L65) |
| `KRB-4-006` | `08-kerberos.md` | Step 4 Configure Identities | Enable Step load 等待 COMPOSITE descriptor GET | [source](../08-kerberos.md#L66) |
| `KRB-5-001` | `08-kerberos.md` | Step 5 Confirm Configuration | 按所选模式展示最终 KDC properties | [source](../08-kerberos.md#L72) |
| `KRB-5-002` | `08-kerberos.md` | Step 5 Confirm Configuration | 下载 \`kerberos.csv\` | [source](../08-kerberos.md#L73) |
| `KRB-5-003` | `08-kerberos.md` | Step 5 Confirm Configuration | Manual 模式提示必须依据 CSV 人工创建 principals、keytabs 并分发到目标路径 | [source](../08-kerberos.md#L74) |
| `KRB-5-004` | `08-kerberos.md` | Step 5 Confirm Configuration | Exit Wizard | [source](../08-kerberos.md#L75) |
| `KRB-6-001` | `08-kerberos.md` | Step 6 Stop Services | 停止所有 services | [source](../08-kerberos.md#L81) |
| `KRB-6-002` | `08-kerberos.md` | Step 6 Stop Services | 在 YARN 已安装、ATS 不支持 Kerberos 且 APP_TIMELINE_SERVER 存在时删除该 component | [source](../08-kerberos.md#L82) |
| `KRB-7-001` | `08-kerberos.md` | Step 7 Kerberize Cluster | 将 \`Clusters.security_type\` 改为 \`KERBEROS\` 并启动服务端 KERBERIZE_CLUSTER request | [source](../08-kerberos.md#L88) |
| `KRB-7-002` | `08-kerberos.md` | Step 7 Kerberize Cluster | 失败后允许回 Step 4 修 descriptor，或 Retry | [source](../08-kerberos.md#L89) |
| `KRB-7-003` | `08-kerberos.md` | Step 7 Kerberize Cluster | controller 中保留 \`unkerberizeCluster()\` 清理方法 | [source](../08-kerberos.md#L90) |
| `KRB-8-001` | `08-kerberos.md` | Step 8 Start And Test Services | 启动所有 services，并按 Ambari property 决定是否同时跑 smoke tests | [source](../08-kerberos.md#L96) |
| `KRB-8-002` | `08-kerberos.md` | Step 8 Start And Test Services | request 失败也允许 Complete | [source](../08-kerberos.md#L97) |
| `KRB-8-003` | `08-kerberos.md` | Step 8 Start And Test Services | Complete 清 Kerberos wizard local DB/status 并返回 Kerberos 管理页 | [source](../08-kerberos.md#L98) |
| `KRB-DIS-001` | `08-kerberos.md` | Disable Kerberos | 前置 | [source](../08-kerberos.md#L104) |
| `KRB-DIS-002` | `08-kerberos.md` | Disable Kerberos | 1 Start ZooKeeper | [source](../08-kerberos.md#L105) |
| `KRB-DIS-003` | `08-kerberos.md` | Disable Kerberos | 2 Stop Required Services | [source](../08-kerberos.md#L106) |
| `KRB-DIS-004` | `08-kerberos.md` | Disable Kerberos | 3 Unkerberize Cluster | [source](../08-kerberos.md#L107) |
| `KRB-DIS-005` | `08-kerberos.md` | Disable Kerberos | 3 failure skip | [source](../08-kerberos.md#L108) |
| `KRB-DIS-006` | `08-kerberos.md` | Disable Kerberos | 4 Remove Kerberos | [source](../08-kerberos.md#L109) |
| `KRB-DIS-007` | `08-kerberos.md` | Disable Kerberos | 5 Start Services | [source](../08-kerberos.md#L110) |
| `KRB-DIS-008` | `08-kerberos.md` | Disable Kerberos | 退出与恢复边界 | [source](../08-kerberos.md#L111) |
| `KRB-DIS-009` | `08-kerberos.md` | Disable Kerberos | 关闭 Disable modal 的清理链误调用 \`addServiceController.finish()\` | [source](../08-kerberos.md#L112) |
| `KRB-MGMT-001` | `08-kerberos.md` | 已启用后的管理能力 | 查看按 Global、Ambari Principals、各 service 分类的 composite identities/configs | [source](../08-kerberos.md#L118) |
| `KRB-MGMT-002` | `08-kerberos.md` | 已启用后的管理能力 | Edit、Cancel、Save identities | [source](../08-kerberos.md#L119) |
| `KRB-MGMT-003` | `08-kerberos.md` | 已启用后的管理能力 | 保存 identity 变更后 regenerate keytabs | [source](../08-kerberos.md#L120) |
| `KRB-MGMT-004` | `08-kerberos.md` | 已启用后的管理能力 | 集群级 Regenerate Keytabs | [source](../08-kerberos.md#L121) |
| `KRB-MGMT-005` | `08-kerberos.md` | 已启用后的管理能力 | 服务级 Regenerate Keytabs | [source](../08-kerberos.md#L122) |
| `KRB-MGMT-006` | `08-kerberos.md` | 已启用后的管理能力 | 主机级 Regenerate Keytabs | [source](../08-kerberos.md#L123) |
| `KRB-MGMT-007` | `08-kerberos.md` | 已启用后的管理能力 | Regenerate 成功后关联后台操作 | [source](../08-kerberos.md#L124) |
| `KRB-MGMT-008` | `08-kerberos.md` | 已启用后的管理能力 | 下载当前 identities CSV | [source](../08-kerberos.md#L125) |
| `KRB-MGMT-009` | `08-kerberos.md` | 已启用后的管理能力 | artifact update 的 404 create fallback 存在实现缺陷 | [source](../08-kerberos.md#L126) |
| `KRB-CRED-001` | `08-kerberos.md` | KDC Credential Store | 检测 persistent credential store 能力 | [source](../08-kerberos.md#L132) |
| `KRB-CRED-002` | `08-kerberos.md` | KDC Credential Store | 查询 \`kdc.admin.credential\` 是否存在 | [source](../08-kerberos.md#L133) |
| `KRB-CRED-003` | `08-kerberos.md` | KDC Credential Store | 保存 KDC admin principal/password | [source](../08-kerberos.md#L134) |
| `KRB-CRED-004` | `08-kerberos.md` | KDC Credential Store | 删除已持久化 KDC credential | [source](../08-kerberos.md#L135) |
| `KRB-CRED-005` | `08-kerberos.md` | KDC Credential Store | 其他向导需要 KDC 时验证 session | [source](../08-kerberos.md#L136) |
| `KRB-CRED-006` | `08-kerberos.md` | KDC Credential Store | Credential CRUD 的失败传播被吞掉 | [source](../08-kerberos.md#L137) |
| `KRB-X-001` | `08-kerberos.md` | 与安装和日常运维的联动 | Add Service | [source](../08-kerberos.md#L143) |
| `KRB-X-002` | `08-kerberos.md` | 与安装和日常运维的联动 | Add Host | [source](../08-kerberos.md#L144) |
| `KRB-X-003` | `08-kerberos.md` | 与安装和日常运维的联动 | Add/Delete Host Component | [source](../08-kerberos.md#L145) |
| `KRB-X-004` | `08-kerberos.md` | 与安装和日常运维的联动 | Reassign Master/HA/Federation | [source](../08-kerberos.md#L146) |
| `KRB-X-005` | `08-kerberos.md` | 与安装和日常运维的联动 | Service/Host restart | [source](../08-kerberos.md#L147) |
| `KRB-REC-001` | `08-kerberos.md` | 恢复、退出与静态边界 | Enable 每次切 step 保存完整 local DB snapshot 到 cluster status | [source](../08-kerberos.md#L153) |
| `KRB-REC-002` | `08-kerberos.md` | 恢复、退出与静态边界 | Enable 保存 task statuses、task request IDs 和旧 request IDs | [source](../08-kerberos.md#L154) |
| `KRB-REC-003` | `08-kerberos.md` | 恢复、退出与静态边界 | Enable 所有 step 的 \`unroutePath()\` 返回 false | [source](../08-kerberos.md#L155) |
| `KRB-REC-004` | `08-kerberos.md` | 恢复、退出与静态边界 | Enable 未完成时 Exit 执行 discard | [source](../08-kerberos.md#L156) |
| `KRB-REC-005` | `08-kerberos.md` | 恢复、退出与静态边界 | Manual CSV 完成度无法由静态 UI证明 | [source](../08-kerberos.md#L157) |
| `KRB-REC-006` | `08-kerberos.md` | 恢复、退出与静态边界 | Disable 不在全局 wizard controller-route 恢复表 | [source](../08-kerberos.md#L158) |
| `KRB-RISK-001` | `08-kerberos.md` | 已知缺陷与验证门槛 | Step 3 在 \`supports.autoRollbackHA=true\` 且 task 失败时会显示 Rollback，但 controller 没有 \`rollback()\` handler；Skip 仍没有可达 state | [source](../08-kerberos.md#L164) |
| `KRB-RISK-002` | `08-kerberos.md` | 已知缺陷与验证门槛 | Step 7 的孤立 unkerberize 方法与真实 Retry 分离 | [source](../08-kerberos.md#L165) |
| `KRB-RISK-003` | `08-kerberos.md` | 已知缺陷与验证门槛 | Credential save/update/delete 失败会被 UI 报成成功，且保存失败可重放原 KDC 请求 | [source](../08-kerberos.md#L166) |
| `KRB-RISK-004` | `08-kerberos.md` | 已知缺陷与验证门槛 | descriptor 404 fallback 可能因未定义 \`self\` 直接抛错，也不会接续 regenerate | [source](../08-kerberos.md#L167) |
| `KRB-RISK-005` | `08-kerberos.md` | 已知缺陷与验证门槛 | \`credentials_test.js\` 中 create/update 失败语义和 store type helper 测试被 \`describe.skip\` | [source](../08-kerberos.md#L168) |
| `NNHA-SCOPE-001` | `09-namenode-journalnode-ha.md` | 范围、证据与 Metrics 边界 | 主流程 route 为 \`/highAvailability/NameNode/enable\`，内部固定 Step 1 到 Step 9；modal route 名为 \`main.services.enableHighAvailability\` | [source](../09-namenode-journalnode-ha.md#L9) |
| `NNHA-SCOPE-002` | `09-namenode-journalnode-ha.md` | 范围、证据与 Metrics 边界 | JN 管理 route 为 \`/highAvailability/JournalNode/manage\`，内部代码固定 Step 1 到 Step 7；纯删除隐藏 Step 3/5，用户看到重新编号后的五步 | [source](../09-namenode-journalnode-ha.md#L10) |
| `NNHA-SCOPE-003` | `09-namenode-journalnode-ha.md` | 范围、证据与 Metrics 边界 | 本模块排除 Metrics 产品页面、图表、widget 和安装 helper 的全局 metrics refresh；保留 checkpoint/JN formatted 安全门槛中的 \`metrics/dfs/...\`，因为字段直接决定 Next | [source](../09-namenode-journalnode-ha.md#L11) |
| `NNHA-SCOPE-004` | `09-namenode-journalnode-ha.md` | 范围、证据与 Metrics 边界 | AMS 只保留 HA 配置迁移：若 \`ams-hbase-site/hbase.rootdir\` 包含当前 NN host，则改成 nameservice；AMS 已安装时 Step 9 无论该值是否匹配都会重提交完整 \`ams-hbase-site\` snapshot。除此之外 AMS/Metrics 功能均 \`OUT_OF_SCOPE\` | [source](../09-namenode-journalnode-ha.md#L12) |
| `NNHA-SCOPE-005` | `09-namenode-journalnode-ha.md` | 范围、证据与 Metrics 边界 | 权威证据为 route/controller/view/template、五层网络清单、通用 progress/install mixin 和旧 Karma tests；静态代码与实际可达性冲突时本文显式标为 \`STATIC_ONLY\`、\`PLACEHOLDER\` 或 \`NEEDS_RUNTIME_VALIDATION\` | [source](../09-namenode-journalnode-ha.md#L13) |
| `NNHA-ENTRY-001` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | HDFS Service Actions 在未启用 HA 时显示 Enable NameNode HA，调用统一 HA controller 后进入九步 modal | [source](../09-namenode-journalnode-ha.md#L19) |
| `NNHA-ENTRY-002` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | HA 已启用后 \`TOGGLE_NN_HA\` 的 action/label 虽计算为 Disable，但 \`isHidden=App.isHaEnabled\` | [source](../09-namenode-journalnode-ha.md#L20) |
| `NNHA-ENTRY-003` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | Stack upgrade 的 Secondary NameNode custom check 可显示 Enable 按钮并复用同一前置检查 | [source](../09-namenode-journalnode-ha.md#L21) |
| `NNHA-ENTRY-004` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | 进入前对找到的第一个 NameNode 检查 \`workStatus === STARTED\` | [source](../09-namenode-journalnode-ha.md#L22) |
| `NNHA-ENTRY-005` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | 进入前要求至少三个 ZooKeeper Server component | [source](../09-namenode-journalnode-ha.md#L23) |
| `NNHA-ENTRY-006` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | 进入前要求至少三台注册主机 | [source](../09-namenode-journalnode-ha.md#L24) |
| `NNHA-ENTRY-007` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | 任一 master component 显式 maintenance \`passiveState=ON\` 或 implied maintenance 时阻止 | [source](../09-namenode-journalnode-ha.md#L25) |
| `NNHA-ENTRY-008` | `09-namenode-journalnode-ha.md` | Enable NameNode HA 入口与前置条件 | 直接访问 route 时 route 本身没有重新执行权限与上述业务前置检查 | [source](../09-namenode-journalnode-ha.md#L26) |
| `NNHA-STEP1-001` | `09-namenode-journalnode-ha.md` | Step 1 Get Started | 阅读停机维护窗口、自动与人工步骤说明，并输入 Nameservice ID | [source](../09-namenode-journalnode-ha.md#L34) |
| `NNHA-STEP1-002` | `09-namenode-journalnode-ha.md` | Step 1 Get Started | UI 强提示：若 HBase 正在运行应退出向导并先停止 HBase | [source](../09-namenode-journalnode-ha.md#L35) |
| `NNHA-STEP1-003` | `09-namenode-journalnode-ha.md` | Step 1 Get Started | 安装 HAWQ 时额外提示完成 HA 后必须人工更新 HAWQ filespace | [source](../09-namenode-journalnode-ha.md#L36) |
| `NNHA-STEP2-001` | `09-namenode-journalnode-ha.md` | Step 2 Select Hosts | 分配一台 Additional NameNode 和至少三台 JournalNode；现有 NameNode 标为 Current | [source](../09-namenode-journalnode-ha.md#L42) |
| `NNHA-STEP2-002` | `09-namenode-journalnode-ha.md` | Step 2 Select Hosts | 同一种多实例 master 的 host 必须唯一，目标 host 必须存在 | [source](../09-namenode-journalnode-ha.md#L43) |
| `NNHA-STEP2-003` | `09-namenode-journalnode-ha.md` | Step 2 Select Hosts | JN 最大数由 stack component cardinality 与可用 host 数共同限制，可大于 3 | [source](../09-namenode-journalnode-ha.md#L44) |
| `NNHA-STEP2-004` | `09-namenode-journalnode-ha.md` | Step 2 Select Hosts | Back 回 Step 1；Next 同时记录 Additional NN 和 SNN host 供 rollback 数据使用 | [source](../09-namenode-journalnode-ha.md#L45) |
| `NNHA-STEP3-001` | `09-namenode-journalnode-ha.md` | Step 3 Review | Review 展示 Current NN、待删除 SNN、待安装 Additional NN 和全部待安装 JN | [source](../09-namenode-journalnode-ha.md#L51) |
| `NNHA-STEP3-002` | `09-namenode-journalnode-ha.md` | Step 3 Review | 只允许编辑 \`hdfs-site/dfs.journalnode.edits.dir\`；其余 HA 与依赖服务配置只读 | [source](../09-namenode-journalnode-ha.md#L52) |
| `NNHA-STEP3-003` | `09-namenode-journalnode-ha.md` | Step 3 Review | 按已安装服务加载 HBase、Accumulo、AMS、HAWQ、Ranger 相关当前 tags/configs | [source](../09-namenode-journalnode-ha.md#L53) |
| `NNHA-STEP3-004` | `09-namenode-journalnode-ha.md` | Step 3 Review | Next 保存 \`hdfs-site\`、\`core-site\`，条件保存 \`hbase-site\`、\`ranger-env\` 原 tags，供后续提交/静态 rollback 使用 | [source](../09-namenode-journalnode-ha.md#L54) |
| `NNHA-STEP4-001` | `09-namenode-journalnode-ha.md` | Step 4 Create Checkpoint | 用户登录 Current NN，依次以 HDFS user 执行 \`hdfs dfsadmin -safemode enter\` 和 \`hdfs dfsadmin -saveNamespace\` | [source](../09-namenode-journalnode-ha.md#L60) |
| `NNHA-STEP4-002` | `09-namenode-journalnode-ha.md` | Step 4 Create Checkpoint | Next 只在 \`Safemode\` 非空且 \`LastAppliedOrWrittenTxId - MostRecentCheckpointTxId <= 1\` 时启用 | [source](../09-namenode-journalnode-ha.md#L61) |
| `NNHA-STEP4-003` | `09-namenode-journalnode-ha.md` | Step 4 Create Checkpoint | desired state 非 \`STARTED\` 时显示错误说明 | [source](../09-namenode-journalnode-ha.md#L62) |
| `NNHA-STEP4-004` | `09-namenode-journalnode-ha.md` | Step 4 Create Checkpoint | 点击已启用 Next 后先检查 KDC session，再进入自动变更阶段 | [source](../09-namenode-journalnode-ha.md#L63) |
| `NNHA-STEP5-001` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | 1 Stop All Services | [source](../09-namenode-journalnode-ha.md#L69) |
| `NNHA-STEP5-002` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | 2 Install Additional NameNode | [source](../09-namenode-journalnode-ha.md#L70) |
| `NNHA-STEP5-003` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | 3 Install JournalNodes | [source](../09-namenode-journalnode-ha.md#L71) |
| `NNHA-STEP5-004` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | 4 Reconfigure HDFS | [source](../09-namenode-journalnode-ha.md#L72) |
| `NNHA-STEP5-005` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | Kerberos 集群的重配置分支 | [source](../09-namenode-journalnode-ha.md#L73) |
| `NNHA-STEP5-006` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | 5 Start JournalNodes | [source](../09-namenode-journalnode-ha.md#L74) |
| `NNHA-STEP5-007` | `09-namenode-journalnode-ha.md` | Step 5 Configure Components | 6 Disable Secondary NameNode | [source](../09-namenode-journalnode-ha.md#L75) |
| `NNHA-STEP6-001` | `09-namenode-journalnode-ha.md` | Step 6 Initialize JournalNodes | 用户登录 Current NN，以 HDFS user 执行 \`hdfs namenode -initializeSharedEdits\` | [source](../09-namenode-journalnode-ha.md#L81) |
| `NNHA-STEP6-002` | `09-namenode-journalnode-ha.md` | Step 6 Initialize JournalNodes | 对响应解析 \`metrics.dfs.journalnode.journalsStatus\`，要求当前 nameservice 的 \`Formatted === "true"\` | [source](../09-namenode-journalnode-ha.md#L82) |
| `NNHA-STEP6-003` | `09-namenode-journalnode-ha.md` | Step 6 Initialize JournalNodes | 经典实现只在收到前三个响应时判定，并要求当次计数为 3 | [source](../09-namenode-journalnode-ha.md#L83) |
| `NNHA-STEP7-001` | `09-namenode-journalnode-ha.md` | Step 7 Start Components | 1 Start ZooKeeper Servers | [source](../09-namenode-journalnode-ha.md#L89) |
| `NNHA-STEP7-002` | `09-namenode-journalnode-ha.md` | Step 7 Start Components | 2 条件 Start Ambari Infra | [source](../09-namenode-journalnode-ha.md#L90) |
| `NNHA-STEP7-003` | `09-namenode-journalnode-ha.md` | Step 7 Start Components | 3 条件 Start MySQL Server | [source](../09-namenode-journalnode-ha.md#L91) |
| `NNHA-STEP7-004` | `09-namenode-journalnode-ha.md` | Step 7 Start Components | 4 条件 Start Ranger | [source](../09-namenode-journalnode-ha.md#L92) |
| `NNHA-STEP7-005` | `09-namenode-journalnode-ha.md` | Step 7 Start Components | 5 Start Current NameNode | [source](../09-namenode-journalnode-ha.md#L93) |
| `NNHA-STEP8-001` | `09-namenode-journalnode-ha.md` | Step 8 Initialize Metadata | 在 Current NN 执行 \`hdfs zkfc -formatZK\` | [source](../09-namenode-journalnode-ha.md#L99) |
| `NNHA-STEP8-002` | `09-namenode-journalnode-ha.md` | Step 8 Initialize Metadata | 在 Additional NN 执行 \`hdfs namenode -bootstrapStandby\` | [source](../09-namenode-journalnode-ha.md#L100) |
| `NNHA-STEP8-003` | `09-namenode-journalnode-ha.md` | Step 8 Initialize Metadata | 点击 Next 先检查 KDC session，再弹“已执行人工步骤”确认；确认后才进入 Step 9 | [source](../09-namenode-journalnode-ha.md#L101) |
| `NNHA-STEP9-001` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 1 Start Additional NameNode | [source](../09-namenode-journalnode-ha.md#L107) |
| `NNHA-STEP9-002` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 2 Install ZKFC；3 Start ZKFC | [source](../09-namenode-journalnode-ha.md#L108) |
| `NNHA-STEP9-003` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 4 条件 Install PXF | [source](../09-namenode-journalnode-ha.md#L109) |
| `NNHA-STEP9-004` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 5 条件 Reconfigure Ranger | [source](../09-namenode-journalnode-ha.md#L110) |
| `NNHA-STEP9-005` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 6 条件 Reconfigure HBase | [source](../09-namenode-journalnode-ha.md#L111) |
| `NNHA-STEP9-006` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 7 条件 Reconfigure AMS | [source](../09-namenode-journalnode-ha.md#L112) |
| `NNHA-STEP9-007` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 8 条件 Reconfigure Accumulo | [source](../09-namenode-journalnode-ha.md#L113) |
| `NNHA-STEP9-008` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 9 条件 Reconfigure HAWQ | [source](../09-namenode-journalnode-ha.md#L114) |
| `NNHA-STEP9-009` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 10 Delete Secondary NameNode | [source](../09-namenode-journalnode-ha.md#L115) |
| `NNHA-STEP9-010` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 11 Stop HDFS；12 Start All Services | [source](../09-namenode-journalnode-ha.md#L116) |
| `NNHA-STEP9-011` | `09-namenode-journalnode-ha.md` | Step 9 Finalize HA Setup | 完成 | [source](../09-namenode-journalnode-ha.md#L117) |
| `NNHA-CONFIG-001` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`hdfs-site/dfs.nameservices\`、\`dfs.internal.nameservices\` | [source](../09-namenode-journalnode-ha.md#L123) |
| `NNHA-CONFIG-002` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`dfs.ha.namenodes.<ns>\` | [source](../09-namenode-journalnode-ha.md#L124) |
| `NNHA-CONFIG-003` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`dfs.namenode.rpc-address.<ns>.nn1/nn2\` | [source](../09-namenode-journalnode-ha.md#L125) |
| `NNHA-CONFIG-004` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`dfs.namenode.http-address.<ns>.nn1/nn2\`、HTTPS 对应键 | [source](../09-namenode-journalnode-ha.md#L126) |
| `NNHA-CONFIG-005` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`dfs.namenode.shared.edits.dir\` | [source](../09-namenode-journalnode-ha.md#L127) |
| `NNHA-CONFIG-006` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`core-site/fs.defaultFS\`、\`ha.zookeeper.quorum\` | [source](../09-namenode-journalnode-ha.md#L128) |
| `NNHA-CONFIG-007` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`dfs.ha.fencing.methods\`、\`dfs.ha.automatic-failover.enabled\`、\`dfs.namenode.safemode.threshold-pct\` | [source](../09-namenode-journalnode-ha.md#L129) |
| `NNHA-CONFIG-008` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | 删除旧 HDFS properties | [source](../09-namenode-journalnode-ha.md#L130) |
| `NNHA-CONFIG-009` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`dfs.journalnode.edits.dir\` | [source](../09-namenode-journalnode-ha.md#L131) |
| `NNHA-CONFIG-010` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`hbase-site/hbase.rootdir\` | [source](../09-namenode-journalnode-ha.md#L132) |
| `NNHA-CONFIG-011` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`accumulo-site/instance.volumes\` 与 \`.replacements\` | [source](../09-namenode-journalnode-ha.md#L133) |
| `NNHA-CONFIG-012` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`ams-hbase-site/hbase.rootdir\` | [source](../09-namenode-journalnode-ha.md#L134) |
| `NNHA-CONFIG-013` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`hawq-site/hawq_dfs_url\` | [source](../09-namenode-journalnode-ha.md#L135) |
| `NNHA-CONFIG-014` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | \`hdfs-client\` | [source](../09-namenode-journalnode-ha.md#L136) |
| `NNHA-CONFIG-015` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | Ranger \`xasecure.audit.destination.hdfs.dir\` | [source](../09-namenode-journalnode-ha.md#L137) |
| `NNHA-CONFIG-016` | `09-namenode-journalnode-ha.md` | NameNode HA 配置契约 | config 保存形状 | [source](../09-namenode-journalnode-ha.md#L138) |
| `JN-ENTRY-001` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | HDFS Service Actions 显示 Manage JournalNodes | [source](../09-namenode-journalnode-ha.md#L144) |
| `JN-ENTRY-002` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | Service Actions 外层 template 以 \`RUN_CUSTOM_COMMAND/RUN_SERVICE_CHECK/START_STOP/TOGGLE_MAINTENANCE/ENABLE_HA\` 的 OR 显示按钮；生成 Manage JN 选项的内层分支只接受除 \`START_STOP\` 外的另外四项 OR | [source](../09-namenode-journalnode-ha.md#L145) |
| `JN-ENTRY-003` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | 服务入口点击后要求模型中同时存在 display label \`Active NameNode\` 和 \`Standby NameNode\` | [source](../09-namenode-journalnode-ha.md#L146) |
| `JN-ENTRY-004` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | Host Details 的 Add JournalNode 先读取 Kerberos 类型并检查 KDC session，再确认进入向导；Manual Kerberos 跳过 session并在确认文案追加 warning。Delete JournalNode 不做 KDC 检查，直接确认进入同一向导 | [source](../09-namenode-journalnode-ha.md#L147) |
| `JN-ENTRY-005` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | 删除按钮在全局 JN count <= 3 时 disabled | [source](../09-namenode-journalnode-ha.md#L148) |
| `JN-MODE-001` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | Add-only | [source](../09-namenode-journalnode-ha.md#L149) |
| `JN-MODE-002` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | Delete-only | [source](../09-namenode-journalnode-ha.md#L150) |
| `JN-MODE-003` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | Mixed add/delete | [source](../09-namenode-journalnode-ha.md#L151) |
| `JN-MODE-004` | `09-namenode-journalnode-ha.md` | Manage JournalNodes 入口与模式 | No-op | [source](../09-namenode-journalnode-ha.md#L152) |
| `JN-STEP1-001` | `09-namenode-journalnode-ha.md` | Step 1 Assign JournalNodes | 以当前 JN hosts 初始化 assignment，可增、删或换 host | [source](../09-namenode-journalnode-ha.md#L160) |
| `JN-STEP1-002` | `09-namenode-journalnode-ha.md` | Step 1 Assign JournalNodes | 最大 JN 数为 \`min(stack/host cardinality, existingCount * 2 - 1)\` | [source](../09-namenode-journalnode-ha.md#L161) |
| `JN-STEP1-003` | `09-namenode-journalnode-ha.md` | Step 1 Assign JournalNodes | Next 保存最终 master topology，后续通过与实时模型差集计算 add/delete hosts | [source](../09-namenode-journalnode-ha.md#L162) |
| `JN-STEP2-001` | `09-namenode-journalnode-ha.md` | Step 2 Review | Review 明列待安装与待删除 JN hosts，并展示只读 HDFS shared-edits 变化 | [source](../09-namenode-journalnode-ha.md#L168) |
| `JN-STEP2-002` | `09-namenode-journalnode-ha.md` | Step 2 Review | 非 Federation 更新 \`dfs.namenode.shared.edits.dir\` | [source](../09-namenode-journalnode-ha.md#L169) |
| `JN-STEP2-003` | `09-namenode-journalnode-ha.md` | Step 2 Review | NameNode Federation 更新每个 \`dfs.namenode.shared.edits.dir.<ns>\` | [source](../09-namenode-journalnode-ha.md#L170) |
| `JN-STEP2-004` | `09-namenode-journalnode-ha.md` | Step 2 Review | Next 保存 config snapshot/tag/nameservice；纯删除直达 Step 4，其余进入 Step 3 | [source](../09-namenode-journalnode-ha.md#L171) |
| `JN-STEP3-001` | `09-namenode-journalnode-ha.md` | Step 3 Save Namespace | 单 namespace 在 Active NN 执行 safemode enter 和 saveNamespace | [source](../09-namenode-journalnode-ha.md#L177) |
| `JN-STEP3-002` | `09-namenode-journalnode-ha.md` | Step 3 Save Namespace | 多 namespace 为每个 namespace显示 \`-fs hdfs://<ns>\` 的 safemode/saveNamespace 命令 | [source](../09-namenode-journalnode-ha.md#L178) |
| `JN-STEP3-003` | `09-namenode-journalnode-ha.md` | Step 3 Save Namespace | 多 namespace 选择每组检查 host：先使用 Active NN 模型；缺 Active 时优先该组 \`STARTED\` NN，否则第一台 | [source](../09-namenode-journalnode-ha.md#L179) |
| `JN-STEP3-004` | `09-namenode-journalnode-ha.md` | Step 3 Save Namespace | 所有返回项均需 \`Safemode\` 非空且 txid checkpoint 差 <= 1 才启用 Next | [source](../09-namenode-journalnode-ha.md#L180) |
| `JN-STEP3-005` | `09-namenode-journalnode-ha.md` | Step 3 Save Namespace | 响应 item 数没有与 namespace 数做等量校验 | [source](../09-namenode-journalnode-ha.md#L181) |
| `JN-STEP4-001` | `09-namenode-journalnode-ha.md` | Step 4 Add/Remove JournalNodes | 1 Stop Standby NameNode | [source](../09-namenode-journalnode-ha.md#L187) |
| `JN-STEP4-002` | `09-namenode-journalnode-ha.md` | Step 4 Add/Remove JournalNodes | 2 Stop Services | [source](../09-namenode-journalnode-ha.md#L188) |
| `JN-STEP4-003` | `09-namenode-journalnode-ha.md` | Step 4 Add/Remove JournalNodes | 3 Add JournalNodes | [source](../09-namenode-journalnode-ha.md#L189) |
| `JN-STEP4-004` | `09-namenode-journalnode-ha.md` | Step 4 Add/Remove JournalNodes | 4 Delete JournalNodes | [source](../09-namenode-journalnode-ha.md#L190) |
| `JN-STEP4-005` | `09-namenode-journalnode-ha.md` | Step 4 Add/Remove JournalNodes | 删除多台 JN 的聚合缺陷 | [source](../09-namenode-journalnode-ha.md#L191) |
| `JN-STEP4-006` | `09-namenode-journalnode-ha.md` | Step 4 Add/Remove JournalNodes | 5 Reconfigure HDFS | [source](../09-namenode-journalnode-ha.md#L192) |
| `JN-STEP5-001` | `09-namenode-journalnode-ha.md` | Step 5 Copy JournalNode Directories | 从任一现有 JN host 打包 Journal directories，复制到所有新 JN并在相同位置解压 | [source](../09-namenode-journalnode-ha.md#L198) |
| `JN-STEP5-002` | `09-namenode-journalnode-ha.md` | Step 5 Copy JournalNode Directories | 用户点击 Next 表示人工完成 | [source](../09-namenode-journalnode-ha.md#L199) |
| `JN-STEP6-001` | `09-namenode-journalnode-ha.md` | Step 6 Start JournalNodes 与 Step 7 Start All Services | Step 6 从当前 \`App.HostComponent\` model 读取 JN hosts并 PUT \`STARTED\` | [source](../09-namenode-journalnode-ha.md#L205) |
| `JN-STEP7-001` | `09-namenode-journalnode-ha.md` | Step 6 Start JournalNodes 与 Step 7 Start All Services | Step 7 将全部 services PUT \`STARTED\`，不运行 smoke tests | [source](../09-namenode-journalnode-ha.md#L206) |
| `NNHA-PROGRESS-001` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | 严格串行 | [source](../09-namenode-journalnode-ha.md#L212) |
| `NNHA-PROGRESS-002` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | request 轮询 | [source](../09-namenode-journalnode-ha.md#L213) |
| `NNHA-PROGRESS-003` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | 终态聚合 | [source](../09-namenode-journalnode-ha.md#L214) |
| `NNHA-PROGRESS-004` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | Retry | [source](../09-namenode-journalnode-ha.md#L215) |
| `NNHA-PROGRESS-005` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | Skip | [source](../09-namenode-journalnode-ha.md#L216) |
| `NNHA-PROGRESS-006` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | host/task 详情 | [source](../09-namenode-journalnode-ha.md#L217) |
| `NNHA-PROGRESS-007` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | install KDC gate | [source](../09-namenode-journalnode-ha.md#L218) |
| `NNHA-PROGRESS-008` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | install chain 容错缺陷 | [source](../09-namenode-journalnode-ha.md#L219) |
| `NNHA-PROGRESS-009` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | mixed topology refresh | [source](../09-namenode-journalnode-ha.md#L220) |
| `NNHA-PROGRESS-010` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | component install wire chain | [source](../09-namenode-journalnode-ha.md#L221) |
| `NNHA-PROGRESS-011` | `09-namenode-journalnode-ha.md` | 通用进度、错误、日志与 Kerberos | invalid-KDC credential 保存 | [source](../09-namenode-journalnode-ha.md#L222) |
| `NNHA-RECOVERY-001` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | NNHA snapshot | [source](../09-namenode-journalnode-ha.md#L228) |
| `NNHA-RECOVERY-002` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | NNHA 刷新恢复 | [source](../09-namenode-journalnode-ha.md#L229) |
| `NNHA-RECOVERY-003` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | NNHA Step 1-4 关闭 | [source](../09-namenode-journalnode-ha.md#L230) |
| `NNHA-RECOVERY-004` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | \`autoRollbackHA=false\` 且 Step > 4 关闭 | [source](../09-namenode-journalnode-ha.md#L231) |
| `NNHA-RECOVERY-005` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | \`autoRollbackHA=true\` | [source](../09-namenode-journalnode-ha.md#L232) |
| `JN-RECOVERY-001` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | JN snapshot | [source](../09-namenode-journalnode-ha.md#L233) |
| `JN-RECOVERY-002` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | JN 关闭 | [source](../09-namenode-journalnode-ha.md#L234) |
| `JN-RECOVERY-003` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | JN 恢复不可靠 | [source](../09-namenode-journalnode-ha.md#L235) |
| `NNHA-RECOVERY-006` | `09-namenode-journalnode-ha.md` | 关闭、持久化与恢复 | persist 权限 | [source](../09-namenode-journalnode-ha.md#L236) |
| `NNHA-ROLLBACK-001` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 注册 route \`/highAvailability/NameNode/rollbackHA\`，route 名 \`main.services.rollbackHighAvailability\` | [source](../09-namenode-journalnode-ha.md#L242) |
| `NNHA-ROLLBACK-002` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 注册 rollback Step 2 | [source](../09-namenode-journalnode-ha.md#L243) |
| `NNHA-ROLLBACK-003` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 注册 rollback Step 3 | [source](../09-namenode-journalnode-ha.md#L244) |
| `NNHA-ROLLBACK-004` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | \`disableHighAvailability()\` | [source](../09-namenode-journalnode-ha.md#L245) |
| `NNHA-ROLLBACK-005` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 单体 \`HighAvailabilityRollbackController\` | [source](../09-namenode-journalnode-ha.md#L246) |
| `NNHA-ROLLBACK-006` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 单体 controller 自身导航/状态 | [source](../09-namenode-journalnode-ha.md#L247) |
| `NNHA-ROLLBACK-007` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 单体 controller 接口/类型错误 | [source](../09-namenode-journalnode-ha.md#L248) |
| `NNHA-ROLLBACK-008` | `09-namenode-journalnode-ha.md` | Rollback 与 Disable 的真实可达性 | 产品基线结论 | [source](../09-namenode-journalnode-ha.md#L249) |
| `HA-API-001` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`hosts.confirmed\` GET | [source](../09-namenode-journalnode-ha.md#L257) |
| `HA-API-002` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`hosts.high_availability.wizard\` GET | [source](../09-namenode-journalnode-ha.md#L258) |
| `HA-API-003` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`wizard.loadrecommendations\` POST | [source](../09-namenode-journalnode-ha.md#L259) |
| `HA-API-004` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`config.tags\` GET | [source](../09-namenode-journalnode-ha.md#L260) |
| `HA-API-005` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`admin.get.all_configurations\` GET | [source](../09-namenode-journalnode-ha.md#L261) |
| `HA-API-031` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`config.on_site\` GET | [source](../09-namenode-journalnode-ha.md#L262) |
| `HA-API-006` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`admin.high_availability.getNnCheckPointStatus\` GET | [source](../09-namenode-journalnode-ha.md#L263) |
| `HA-API-007` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`admin.high_availability.getNnCheckPointsStatuses\` GET | [source](../09-namenode-journalnode-ha.md#L264) |
| `HA-API-008` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`admin.high_availability.getJnCheckPointStatus\` GET | [source](../09-namenode-journalnode-ha.md#L265) |
| `HA-API-009` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.services.update\` PUT | [source](../09-namenode-journalnode-ha.md#L266) |
| `HA-API-010` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.host_components.update\` PUT | [source](../09-namenode-journalnode-ha.md#L267) |
| `HA-API-011` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.host.host_component.update\` PUT | [source](../09-namenode-journalnode-ha.md#L268) |
| `HA-API-012` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.host.host_component.passive\` PUT | [source](../09-namenode-journalnode-ha.md#L269) |
| `HA-API-013` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.delete.host_component\` DELETE | [source](../09-namenode-journalnode-ha.md#L270) |
| `HA-API-014` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.service.configurations\` PUT | [source](../09-namenode-journalnode-ha.md#L271) |
| `HA-API-015` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.service.multiConfigurations\` PUT | [source](../09-namenode-journalnode-ha.md#L272) |
| `HA-API-016` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`host_component.installed.on_hosts\` GET | [source](../09-namenode-journalnode-ha.md#L273) |
| `HA-API-017` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.create_component\` POST | [source](../09-namenode-journalnode-ha.md#L274) |
| `HA-API-018` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`wizard.step8.register_host_to_component\` POST | [source](../09-namenode-journalnode-ha.md#L275) |
| `HA-API-019` | `09-namenode-journalnode-ha.md` | 后端接口契约 | direct \`App.HttpClient\` GET | [source](../09-namenode-journalnode-ha.md#L276) |
| `HA-API-020` | `09-namenode-journalnode-ha.md` | 后端接口契约 | direct \`App.HttpClient\` GET | [source](../09-namenode-journalnode-ha.md#L277) |
| `HA-API-021` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`background_operations.get_by_request\` GET | [source](../09-namenode-journalnode-ha.md#L278) |
| `HA-API-022` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`common.request.polling\` GET | [source](../09-namenode-journalnode-ha.md#L279) |
| `HA-API-023` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`background_operations.get_by_task\` GET | [source](../09-namenode-journalnode-ha.md#L280) |
| `HA-API-024` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`admin.security.cluster_configs.kerberos\` GET | [source](../09-namenode-journalnode-ha.md#L281) |
| `HA-API-025` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`kerberos.session.state\` GET | [source](../09-namenode-journalnode-ha.md#L282) |
| `HA-API-026` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`credentials.get/create/update\` GET/POST/PUT | [source](../09-namenode-journalnode-ha.md#L283) |
| `HA-API-027` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`persist.get\` GET | [source](../09-namenode-journalnode-ha.md#L284) |
| `HA-API-028` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`persist.post\` POST | [source](../09-namenode-journalnode-ha.md#L285) |
| `HA-API-029` | `09-namenode-journalnode-ha.md` | 后端接口契约 | \`hosts.all\` GET | [source](../09-namenode-journalnode-ha.md#L286) |
| `HA-API-030` | `09-namenode-journalnode-ha.md` | 后端接口契约 | 静态 rollback接口集合 | [source](../09-namenode-journalnode-ha.md#L287) |
| `JN-RISK-001` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | NNHA Step 4 与 Step 6 对服务端 JSON字符串直接 \`JSON.parse\`，无 try/catch；poll GET没有 error callback/reschedule | [source](../09-namenode-journalnode-ha.md#L293) |
| `JN-RISK-002` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | NNHA Step 6只等前三个 JN响应 | [source](../09-namenode-journalnode-ha.md#L294) |
| `JN-RISK-003` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | JN Step 4多 DELETE第一台 success可提前推进 | [source](../09-namenode-journalnode-ha.md#L295) |
| `JN-RISK-004` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | JN Step 6依赖可能未刷新的 Ember model | [source](../09-namenode-journalnode-ha.md#L296) |
| `JN-RISK-005` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | 多 namespace checkpoint不验证返回基数/身份 | [source](../09-namenode-journalnode-ha.md#L297) |
| `JN-RISK-006` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | JN Review配置数组 reload时可能累积重复项 | [source](../09-namenode-journalnode-ha.md#L298) |
| `JN-RISK-007` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | component register error仍继续 Install，service-component create失败被吞 | [source](../09-namenode-journalnode-ha.md#L299) |
| `JN-RISK-008` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | JN route可由Host Details绕过Active/Standby检查，且直接URL无统一业务 gate | [source](../09-namenode-journalnode-ha.md#L300) |
| `JN-RISK-009` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | JN critical progress关闭无确认/回退，NNHA无auto rollback时只清状态 | [source](../09-namenode-journalnode-ha.md#L301) |
| `JN-RISK-010` | `09-namenode-journalnode-ha.md` | 已知实现风险与 React 决策 | 注册 rollback只是空壳，静态完整rollback未接线且含错误接口/类型 | [source](../09-namenode-journalnode-ha.md#L302) |
| `HA-TEST-001` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | NNHA Step 1-9 controllers/views | [source](../09-namenode-journalnode-ha.md#L308) |
| `HA-TEST-002` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | JN Step 1-7 | [source](../09-namenode-journalnode-ha.md#L309) |
| `HA-TEST-003` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | 通用 progress/popup | [source](../09-namenode-journalnode-ha.md#L310) |
| `HA-TEST-004` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | 配置迁移 | [source](../09-namenode-journalnode-ha.md#L311) |
| `HA-TEST-005` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | 入口/权限 | [source](../09-namenode-journalnode-ha.md#L312) |
| `HA-TEST-006` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | 恢复/关闭/owner | [source](../09-namenode-journalnode-ha.md#L313) |
| `HA-TEST-007` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | rollback | [source](../09-namenode-journalnode-ha.md#L314) |
| `HA-TEST-008` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | 大于三台JN | [source](../09-namenode-journalnode-ha.md#L315) |
| `HA-TEST-009` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | JN并发删除/模型刷新 | [source](../09-namenode-journalnode-ha.md#L316) |
| `HA-TEST-010` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | Federation checkpoint | [source](../09-namenode-journalnode-ha.md#L317) |
| `HA-TEST-011` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | Kerberos模式 | [source](../09-namenode-journalnode-ha.md#L318) |
| `HA-TEST-012` | `09-namenode-journalnode-ha.md` | 测试证据与运行态验收矩阵 | 完整成功与断点恢复 | [source](../09-namenode-journalnode-ha.md#L319) |
| `RMHA-ENTRY-001` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L18) |
| `RMHA-ENTRY-002` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L19) |
| `RMHA-ENTRY-003` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L20) |
| `RMHA-ENTRY-004` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L21) |
| `RMHA-ENTRY-005` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L22) |
| `RAHA-ENTRY-001` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L23) |
| `RAHA-ENTRY-002` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L24) |
| `RAHA-ENTRY-003` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L25) |
| `RAHA-ENTRY-004` | `10-rm-ranger-ha.md` | 路由与入口 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L26) |
| `HA-STATIC-001` | `10-rm-ranger-ha.md` | 路由与入口 | \`STATIC_ONLY\` | [source](../10-rm-ranger-ha.md#L27) |
| `RMHA-1-001` | `10-rm-ranger-ha.md` | Step 1 Get Started | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L35) |
| `RMHA-1-002` | `10-rm-ranger-ha.md` | Step 1 Get Started | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L36) |
| `RMHA-2-001` | `10-rm-ranger-ha.md` | Step 2 Select Host | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L42) |
| `RMHA-2-002` | `10-rm-ranger-ha.md` | Step 2 Select Host | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L43) |
| `RMHA-2-003` | `10-rm-ranger-ha.md` | Step 2 Select Host | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L44) |
| `RMHA-2-004` | `10-rm-ranger-ha.md` | Step 2 Select Host | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L45) |
| `RMHA-2-005` | `10-rm-ranger-ha.md` | Step 2 Select Host | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L46) |
| `RMHA-2-006` | `10-rm-ranger-ha.md` | Step 2 Select Host | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L47) |
| `RMHA-3-001` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L53) |
| `RMHA-3-002` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L54) |
| `RMHA-3-003` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L55) |
| `RMHA-3-004` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L56) |
| `RMHA-3-005` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L57) |
| `RMHA-3-006` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L58) |
| `RMHA-3-007` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L59) |
| `RMHA-3-008` | `10-rm-ranger-ha.md` | Step 3 Review | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L60) |
| `RMHA-CFG-001` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L68) |
| `RMHA-CFG-002` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L69) |
| `RMHA-CFG-003` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L70) |
| `RMHA-CFG-004` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L71) |
| `RMHA-CFG-005` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L72) |
| `RMHA-CFG-006` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L73) |
| `RMHA-CFG-007` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L74) |
| `RMHA-CFG-008` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L75) |
| `RMHA-CFG-009` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L76) |
| `RMHA-CFG-010` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L77) |
| `RMHA-CFG-011` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-site\` | [source](../10-rm-ranger-ha.md#L78) |
| `RMHA-CFG-012` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-client\` | [source](../10-rm-ranger-ha.md#L79) |
| `RMHA-CFG-013` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`yarn-client\` | [source](../10-rm-ranger-ha.md#L80) |
| `RMHA-CFG-014` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | \`core-site\` | [source](../10-rm-ranger-ha.md#L81) |
| `RMHA-CFG-015` | `10-rm-ranger-ha.md` | ResourceManager 配置契约 | 三个 site | [source](../10-rm-ranger-ha.md#L82) |
| `RMHA-4-001` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L88) |
| `RMHA-4-002` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L89) |
| `RMHA-4-003` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L90) |
| `RMHA-4-004` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L91) |
| `RMHA-4-005` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L92) |
| `RMHA-4-006` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L93) |
| `RMHA-4-007` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L94) |
| `RMHA-4-008` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L95) |
| `RMHA-4-009` | `10-rm-ranger-ha.md` | Step 4 Configure Components | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L96) |
| `RAHA-1-001` | `10-rm-ranger-ha.md` | Step 1 Get Started | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L104) |
| `RAHA-1-002` | `10-rm-ranger-ha.md` | Step 1 Get Started | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L105) |
| `RAHA-1-003` | `10-rm-ranger-ha.md` | Step 1 Get Started | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L106) |
| `RAHA-1-004` | `10-rm-ranger-ha.md` | Step 1 Get Started | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L107) |
| `RAHA-2-001` | `10-rm-ranger-ha.md` | Step 2 Select Hosts | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L113) |
| `RAHA-2-002` | `10-rm-ranger-ha.md` | Step 2 Select Hosts | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L114) |
| `RAHA-2-003` | `10-rm-ranger-ha.md` | Step 2 Select Hosts | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L115) |
| `RAHA-2-004` | `10-rm-ranger-ha.md` | Step 2 Select Hosts | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L116) |
| `RAHA-2-005` | `10-rm-ranger-ha.md` | Step 2 Select Hosts | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L117) |
| `RAHA-3-001` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L123) |
| `RAHA-3-002` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L124) |
| `RAHA-3-003` | `10-rm-ranger-ha.md` | Step 3 Review | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L125) |
| `RAHA-3-004` | `10-rm-ranger-ha.md` | Step 3 Review | \`STATIC_ONLY\` | [source](../10-rm-ranger-ha.md#L126) |
| `RAHA-4-001` | `10-rm-ranger-ha.md` | Step 4 Install, Start and Test | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L132) |
| `RAHA-4-002` | `10-rm-ranger-ha.md` | Step 4 Install, Start and Test | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L133) |
| `RAHA-4-003` | `10-rm-ranger-ha.md` | Step 4 Install, Start and Test | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L134) |
| `RAHA-4-004` | `10-rm-ranger-ha.md` | Step 4 Install, Start and Test | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L135) |
| `RAHA-4-005` | `10-rm-ranger-ha.md` | Step 4 Install, Start and Test | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L136) |
| `RAHA-4-006` | `10-rm-ranger-ha.md` | Step 4 Install, Start and Test | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L137) |
| `HA-COMMON-FAIL-001` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L143) |
| `HA-COMMON-FAIL-002` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L144) |
| `HA-COMMON-FAIL-003` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L145) |
| `HA-COMMON-FAIL-004` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L146) |
| `HA-COMMON-FAIL-005` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L147) |
| `HA-COMMON-FAIL-006` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`STATIC_ONLY\` | [source](../10-rm-ranger-ha.md#L148) |
| `HA-COMMON-REC-001` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L149) |
| `HA-COMMON-REC-002` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L150) |
| `HA-COMMON-REC-003` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L151) |
| `HA-COMMON-REC-004` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L152) |
| `HA-COMMON-REC-005` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L153) |
| `HA-COMMON-REC-006` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L154) |
| `HA-COMMON-REC-007` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`STATIC_ONLY\` | [source](../10-rm-ranger-ha.md#L155) |
| `HA-COMMON-REC-008` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L156) |
| `HA-COMMON-KRB-001` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L157) |
| `HA-COMMON-KRB-002` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONDITIONAL\` | [source](../10-rm-ranger-ha.md#L158) |
| `HA-COMMON-KRB-003` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L159) |
| `HA-COMMON-KRB-004` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L160) |
| `HA-COMMON-KRB-005` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`STATIC_ONLY\` | [source](../10-rm-ranger-ha.md#L161) |
| `HA-COMMON-KRB-006` | `10-rm-ranger-ha.md` | 共用失败、恢复与 Kerberos 语义 | \`STATIC_ONLY\` | [source](../10-rm-ranger-ha.md#L162) |
| `HA-COMMON-INSTALL-001` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L170) |
| `HA-COMMON-INSTALL-002` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L171) |
| `HA-COMMON-INSTALL-003` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L172) |
| `HA-COMMON-INSTALL-004` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L173) |
| `HA-COMMON-INSTALL-005` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L174) |
| `HA-COMMON-INSTALL-006` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`CONFIRMED\` | [source](../10-rm-ranger-ha.md#L175) |
| `HA-COMMON-INSTALL-007` | `10-rm-ranger-ha.md` | 组件安装请求链 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L176) |
| `HA-STATIC-002` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L218) |
| `HA-STATIC-003` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L219) |
| `HA-STATIC-004` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L220) |
| `HA-STATIC-005` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L221) |
| `HA-STATIC-006` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L222) |
| `HA-STATIC-007` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L223) |
| `HA-STATIC-008` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L224) |
| `HA-STATIC-009` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L225) |
| `HA-STATIC-010` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L226) |
| `HA-STATIC-011` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L227) |
| `HA-STATIC-012` | `10-rm-ranger-ha.md` | React 对照必测场景 | \`NEEDS_RUNTIME_VALIDATION\` | [source](../10-rm-ranger-ha.md#L228) |
| `NNF-ENTRY-001` | `11-federation-hawq.md` | 入口、权限与可见性 | HDFS Service Actions 的 Enable NameNode Federation，进入 \`/main/services/NameNode/federation/step1\` | [source](../11-federation-hawq.md#L11) |
| `RBF-ENTRY-001` | `11-federation-hawq.md` | 入口、权限与可见性 | HDFS Service Actions 的 Enable Router-based Federation，进入 \`/main/services/NameNode/federation/routerBasedFederation/step1\` | [source](../11-federation-hawq.md#L12) |
| `HAWQ-ENTRY-001` | `11-federation-hawq.md` | 入口、权限与可见性 | HAWQ Service Actions 的 Add HAWQ Standby，进入 \`/main/services/highAvailability/Hawq/add/step1\` | [source](../11-federation-hawq.md#L13) |
| `HAWQ-ENTRY-002` | `11-federation-hawq.md` | 入口、权限与可见性 | HAWQ Master custom command 的 Remove HAWQ Standby，进入 \`/main/services/highAvailability/Hawq/remove/step1\` | [source](../11-federation-hawq.md#L14) |
| `HAWQ-ENTRY-003` | `11-federation-hawq.md` | 入口、权限与可见性 | HAWQ Standby custom command 的 Activate HAWQ Standby，进入 \`/main/services/highAvailability/Hawq/activate/step1\` | [source](../11-federation-hawq.md#L15) |
| `FHF-ENTRY-001` | `11-federation-hawq.md` | 入口、权限与可见性 | 五条向导都会调用 \`dataLoading()\`、把对应 HDFS/HAWQ 设为当前 service、暂停常规 update controller，再在大 modal 内连接当前 step outlet | [source](../11-federation-hawq.md#L16) |
| `FHF-ENTRY-002` | `11-federation-hawq.md` | 入口、权限与可见性 | 五个 wizard view 插入 modal 时都通过 \`WizardHostsLoading\` 固定 GET 一次全部 host，并把结果写入 \`content.hosts\` | [source](../11-federation-hawq.md#L17) |
| `FHF-ENTRY-003` | `11-federation-hawq.md` | 入口、权限与可见性 | 五个 wizard controller 在首次构造已安装 master mapping 时调用 \`loadMasterComponentHosts()\` | [source](../11-federation-hawq.md#L18) |
| `NNF-1-001` | `11-federation-hawq.md` | Step 1 Get Started | 查看已有 nameservice 列表和不可逆/风险提示，输入新的 nameservice ID | [source](../11-federation-hawq.md#L28) |
| `NNF-1-002` | `11-federation-hawq.md` | Step 1 Get Started | nameservice ID 格式严格为 1 至 63 个字符，只允许 ASCII 字母、数字、连字符，且首尾不能是连字符 | [source](../11-federation-hawq.md#L29) |
| `NNF-1-003` | `11-federation-hawq.md` | Step 1 Get Started | 与已有 nameservice ID 的重复校验依赖异步 namespace model | [source](../11-federation-hawq.md#L30) |
| `NNF-2-001` | `11-federation-hawq.md` | Step 2 Select Hosts | 查看全部现有 NameNode（Current）并为新 namespace 固定选择两个 Additional NameNode host | [source](../11-federation-hawq.md#L36) |
| `NNF-2-002` | `11-federation-hawq.md` | Step 2 Select Hosts | 可在 host 下拉框中修改两个新增 NameNode 位置 | [source](../11-federation-hawq.md#L37) |
| `NNF-2-003` | `11-federation-hawq.md` | Step 2 Select Hosts | Next 保存所有现有和新增 master-component-host 映射 | [source](../11-federation-hawq.md#L38) |
| `NNF-3-001` | `11-federation-hawq.md` | Step 3 Review And Configurations | Review 当前与新增 NameNode host，并等待配置加载 | [source](../11-federation-hawq.md#L44) |
| `NNF-3-002` | `11-federation-hawq.md` | Step 3 Review And Configurations | 查看向导生成的 HDFS/Ranger/Accumulo 变更 | [source](../11-federation-hawq.md#L45) |
| `NNF-3-003` | `11-federation-hawq.md` | Step 3 Review And Configurations | 修改新 namespace 的 JournalNode directory | [source](../11-federation-hawq.md#L46) |
| `NNF-4-001` | `11-federation-hawq.md` | Step 4 Configure Components | 1 \`stopRequiredServices\` | [source](../11-federation-hawq.md#L54) |
| `NNF-4-002` | `11-federation-hawq.md` | Step 4 Configure Components | 2 \`reconfigureServices\` | [source](../11-federation-hawq.md#L55) |
| `NNF-4-003` | `11-federation-hawq.md` | Step 4 Configure Components | 3 \`installNameNode\` | [source](../11-federation-hawq.md#L56) |
| `NNF-4-004` | `11-federation-hawq.md` | Step 4 Configure Components | 4 \`installZKFC\` | [source](../11-federation-hawq.md#L57) |
| `NNF-4-005` | `11-federation-hawq.md` | Step 4 Configure Components | 5 \`startJournalNodes\` | [source](../11-federation-hawq.md#L58) |
| `NNF-4-006` | `11-federation-hawq.md` | Step 4 Configure Components | 6 \`startInfraSolr\` | [source](../11-federation-hawq.md#L59) |
| `NNF-4-007` | `11-federation-hawq.md` | Step 4 Configure Components | 7 \`startRangerAdmin\` | [source](../11-federation-hawq.md#L60) |
| `NNF-4-008` | `11-federation-hawq.md` | Step 4 Configure Components | 8 \`startRangerUsersync\` | [source](../11-federation-hawq.md#L61) |
| `NNF-4-009` | `11-federation-hawq.md` | Step 4 Configure Components | 9 \`startNameNodes\` | [source](../11-federation-hawq.md#L62) |
| `NNF-4-010` | `11-federation-hawq.md` | Step 4 Configure Components | 10 \`startZKFCs\` | [source](../11-federation-hawq.md#L63) |
| `NNF-4-011` | `11-federation-hawq.md` | Step 4 Configure Components | 11 \`formatNameNode\` | [source](../11-federation-hawq.md#L64) |
| `NNF-4-012` | `11-federation-hawq.md` | Step 4 Configure Components | 12 \`formatZKFC\` | [source](../11-federation-hawq.md#L65) |
| `NNF-4-013` | `11-federation-hawq.md` | Step 4 Configure Components | 13 \`startZKFC\` | [source](../11-federation-hawq.md#L66) |
| `NNF-4-014` | `11-federation-hawq.md` | Step 4 Configure Components | 14 \`startNameNode\` | [source](../11-federation-hawq.md#L67) |
| `NNF-4-015` | `11-federation-hawq.md` | Step 4 Configure Components | 15 \`bootstrapNameNode\` | [source](../11-federation-hawq.md#L68) |
| `NNF-4-016` | `11-federation-hawq.md` | Step 4 Configure Components | 16 \`startZKFC2\` | [source](../11-federation-hawq.md#L69) |
| `NNF-4-017` | `11-federation-hawq.md` | Step 4 Configure Components | 17 \`startNameNode2\` | [source](../11-federation-hawq.md#L70) |
| `NNF-4-018` | `11-federation-hawq.md` | Step 4 Configure Components | 18 \`restartAllServices\` | [source](../11-federation-hawq.md#L71) |
| `NNF-4-019` | `11-federation-hawq.md` | Step 4 Configure Components | Complete | [source](../11-federation-hawq.md#L72) |
| `NNF-CFG-001` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.nameservices\`、\`dfs.internal.nameservices\` | [source](../11-federation-hawq.md#L78) |
| `NNF-CFG-002` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.ha.namenodes.<newNs>\` | [source](../11-federation-hawq.md#L79) |
| `NNF-CFG-003` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.namenode.rpc-address.<newNs>.<newNnId>\` | [source](../11-federation-hawq.md#L80) |
| `NNF-CFG-004` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.namenode.http-address.*\`、\`dfs.namenode.https-address.*\` | [source](../11-federation-hawq.md#L81) |
| `NNF-CFG-005` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.client.failover.proxy.provider.<newNs>\` | [source](../11-federation-hawq.md#L82) |
| `NNF-CFG-006` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.namenode.shared.edits.dir.<newNs>\` | [source](../11-federation-hawq.md#L83) |
| `NNF-CFG-007` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.journalnode.edits.dir.<newNs>\` | [source](../11-federation-hawq.md#L84) |
| `NNF-CFG-008` | `11-federation-hawq.md` | NameNode Federation 配置变换 | 原 namespace 的 scoped JN/shared edits | [source](../11-federation-hawq.md#L85) |
| `NNF-CFG-009` | `11-federation-hawq.md` | NameNode Federation 配置变换 | \`dfs.namenode.servicerpc-address.*\` | [source](../11-federation-hawq.md#L86) |
| `NNF-CFG-010` | `11-federation-hawq.md` | NameNode Federation 配置变换 | generic JN 属性清理 | [source](../11-federation-hawq.md#L87) |
| `NNF-CFG-011` | `11-federation-hawq.md` | NameNode Federation 配置变换 | Ranger TagSync mapping | [source](../11-federation-hawq.md#L88) |
| `NNF-CFG-012` | `11-federation-hawq.md` | NameNode Federation 配置变换 | Accumulo volumes | [source](../11-federation-hawq.md#L89) |
| `NNF-CFG-013` | `11-federation-hawq.md` | NameNode Federation 配置变换 | 写入原子边界 | [source](../11-federation-hawq.md#L90) |
| `RBF-1-001` | `11-federation-hawq.md` | Steps 1-3 | 1 Get Started | [source](../11-federation-hawq.md#L98) |
| `RBF-2-001` | `11-federation-hawq.md` | Steps 1-3 | 2 Select Hosts | [source](../11-federation-hawq.md#L99) |
| `RBF-2-002` | `11-federation-hawq.md` | Steps 1-3 | 2 host 校验 | [source](../11-federation-hawq.md#L100) |
| `RBF-2-003` | `11-federation-hawq.md` | Steps 1-3 | 2 Next | [source](../11-federation-hawq.md#L101) |
| `RBF-3-001` | `11-federation-hawq.md` | Steps 1-3 | 3 Review | [source](../11-federation-hawq.md#L102) |
| `RBF-3-002` | `11-federation-hawq.md` | Steps 1-3 | 3 配置写入 | [source](../11-federation-hawq.md#L103) |
| `RBF-3-003` | `11-federation-hawq.md` | Steps 1-3 | 3 Next | [source](../11-federation-hawq.md#L104) |
| `RBF-4-001` | `11-federation-hawq.md` | Step 4 Configure Router | 1 \`installRouter\` | [source](../11-federation-hawq.md#L110) |
| `RBF-4-002` | `11-federation-hawq.md` | Step 4 Configure Router | 2 \`startRouters\` | [source](../11-federation-hawq.md#L111) |
| `RBF-4-003` | `11-federation-hawq.md` | Step 4 Configure Router | Complete | [source](../11-federation-hawq.md#L112) |
| `RBF-4-004` | `11-federation-hawq.md` | Step 4 Configure Router | 不可达代码 | [source](../11-federation-hawq.md#L113) |
| `RBF-CFG-001` | `11-federation-hawq.md` | Router-based Federation 配置变换 | \`dfs.federation.router.monitor.namenode\` | [source](../11-federation-hawq.md#L119) |
| `RBF-CFG-002` | `11-federation-hawq.md` | Router-based Federation 配置变换 | \`dfs.federation.router.default.nameserviceId\` | [source](../11-federation-hawq.md#L120) |
| `RBF-CFG-003` | `11-federation-hawq.md` | Router-based Federation 配置变换 | \`zk-dt-secret-manager.zkAuthType\` | [source](../11-federation-hawq.md#L121) |
| `RBF-CFG-004` | `11-federation-hawq.md` | Router-based Federation 配置变换 | \`zk-dt-secret-manager.zkConnectionString\` | [source](../11-federation-hawq.md#L122) |
| `RBF-CFG-005` | `11-federation-hawq.md` | Router-based Federation 配置变换 | 编辑与 override | [source](../11-federation-hawq.md#L123) |
| `HAWQ-ADD-1-001` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 1 Get Started | [source](../11-federation-hawq.md#L131) |
| `HAWQ-ADD-2-001` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 2 Select Host | [source](../11-federation-hawq.md#L132) |
| `HAWQ-ADD-2-002` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 2 client validation | [source](../11-federation-hawq.md#L133) |
| `HAWQ-ADD-2-003` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 2 advisor validation | [source](../11-federation-hawq.md#L134) |
| `HAWQ-ADD-3-001` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 3 Review | [source](../11-federation-hawq.md#L135) |
| `HAWQ-ADD-3-002` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 3 人工数据目录门禁 | [source](../11-federation-hawq.md#L136) |
| `HAWQ-ADD-4-001` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 1 \`stopRequiredServices\` | [source](../11-federation-hawq.md#L137) |
| `HAWQ-ADD-4-002` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 2 \`installHawqStandbyMaster\` | [source](../11-federation-hawq.md#L138) |
| `HAWQ-ADD-4-003` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 3 \`reconfigureHAWQ\` | [source](../11-federation-hawq.md#L139) |
| `HAWQ-ADD-4-004` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | 4 \`startRequiredServices\` | [source](../11-federation-hawq.md#L140) |
| `HAWQ-ADD-4-005` | `11-federation-hawq.md` | HAWQ Add Standby 四步状态机 | Complete | [source](../11-federation-hawq.md#L141) |
| `HAWQ-REMOVE-1-001` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 1 Get Started | [source](../11-federation-hawq.md#L149) |
| `HAWQ-REMOVE-2-001` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 2 Review | [source](../11-federation-hawq.md#L150) |
| `HAWQ-REMOVE-2-002` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 2 irreversible confirm | [source](../11-federation-hawq.md#L151) |
| `HAWQ-REMOVE-3-001` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 1 \`removeStandby\` | [source](../11-federation-hawq.md#L152) |
| `HAWQ-REMOVE-3-002` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 2 \`stopRequiredServices\` | [source](../11-federation-hawq.md#L153) |
| `HAWQ-REMOVE-3-003` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 3 \`reconfigureHAWQ\` | [source](../11-federation-hawq.md#L154) |
| `HAWQ-REMOVE-3-004` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 4 \`deleteHawqStandbyComponent\` | [source](../11-federation-hawq.md#L155) |
| `HAWQ-REMOVE-3-005` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | 5 \`startRequiredServices\` | [source](../11-federation-hawq.md#L156) |
| `HAWQ-REMOVE-3-006` | `11-federation-hawq.md` | HAWQ Remove Standby 三步状态机 | Complete | [source](../11-federation-hawq.md#L157) |
| `HAWQ-ACT-1-001` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 1 Get Started | [source](../11-federation-hawq.md#L167) |
| `HAWQ-ACT-2-001` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 2 Review | [source](../11-federation-hawq.md#L168) |
| `HAWQ-ACT-2-002` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 2 irreversible confirm | [source](../11-federation-hawq.md#L169) |
| `HAWQ-ACT-3-001` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 1 \`activateStandby\` | [source](../11-federation-hawq.md#L170) |
| `HAWQ-ACT-3-002` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 2 \`stopRequiredServices\` | [source](../11-federation-hawq.md#L171) |
| `HAWQ-ACT-3-003` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 3 \`reconfigureHAWQ\` | [source](../11-federation-hawq.md#L172) |
| `HAWQ-ACT-3-004` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 4 \`installHawqMaster\` | [source](../11-federation-hawq.md#L173) |
| `HAWQ-ACT-3-005` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 5 \`deleteOldHawqMaster\` | [source](../11-federation-hawq.md#L174) |
| `HAWQ-ACT-3-006` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 6 \`deleteHawqStandby\` | [source](../11-federation-hawq.md#L175) |
| `HAWQ-ACT-3-007` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | 7 \`startRequiredServices\` | [source](../11-federation-hawq.md#L176) |
| `HAWQ-ACT-3-008` | `11-federation-hawq.md` | HAWQ Activate Standby 三步状态机 | Complete | [source](../11-federation-hawq.md#L177) |
| `HAWQ-CFG-001` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | \`hawq_standby_address_host\` | [source](../11-federation-hawq.md#L185) |
| `HAWQ-CFG-002` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | \`hawq_master_address_host\` | [source](../11-federation-hawq.md#L186) |
| `HAWQ-CFG-003` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | \`hawq_master_directory\` | [source](../11-federation-hawq.md#L187) |
| `HAWQ-CFG-004` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | component cardinality | [source](../11-federation-hawq.md#L188) |
| `HAWQ-CFG-005` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | service/component dependency | [source](../11-federation-hawq.md#L189) |
| `HAWQ-CFG-006` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | Kerberos identity | [source](../11-federation-hawq.md#L190) |
| `HAWQ-CFG-007` | `11-federation-hawq.md` | HAWQ 配置与历史 Stack 契约 | custom command timeout | [source](../11-federation-hawq.md#L191) |
| `FHF-KRB-001` | `11-federation-hawq.md` | Kerberos 条件分支 | 所有 component install command | [source](../11-federation-hawq.md#L197) |
| `FHF-KRB-002` | `11-federation-hawq.md` | Kerberos 条件分支 | HAWQ Add Step 3 | [source](../11-federation-hawq.md#L198) |
| `FHF-KRB-003` | `11-federation-hawq.md` | Kerberos 条件分支 | HAWQ Remove/Activate Step 2 | [source](../11-federation-hawq.md#L199) |
| `FHF-KRB-004` | `11-federation-hawq.md` | Kerberos 条件分支 | Federation/RBF | [source](../11-federation-hawq.md#L200) |
| `FHF-KRB-005` | `11-federation-hawq.md` | Kerberos 条件分支 | RBF stack descriptor | [source](../11-federation-hawq.md#L201) |
| `FHF-KRB-006` | `11-federation-hawq.md` | Kerberos 条件分支 | HAWQ descriptor | [source](../11-federation-hawq.md#L202) |
| `FHF-KRB-007` | `11-federation-hawq.md` | Kerberos 条件分支 | 通用 KDC 类型分支 | [source](../11-federation-hawq.md#L203) |
| `FHF-KRB-008` | `11-federation-hawq.md` | Kerberos 条件分支 | KDC session 无效 | [source](../11-federation-hawq.md#L204) |
| `FHF-KRB-009` | `11-federation-hawq.md` | Kerberos 条件分支 | credential 保存异常 | [source](../11-federation-hawq.md#L205) |
| `FHF-PROG-001` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | 严格串行 | [source](../11-federation-hawq.md#L211) |
| `FHF-PROG-002` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | request 轮询 | [source](../11-federation-hawq.md#L212) |
| `FHF-PROG-003` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | 终态聚合 | [source](../11-federation-hawq.md#L213) |
| `FHF-PROG-004` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | Retry | [source](../11-federation-hawq.md#L214) |
| `FHF-PROG-005` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | Skip | [source](../11-federation-hawq.md#L215) |
| `FHF-PROG-006` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | 最后任务失败 | [source](../11-federation-hawq.md#L216) |
| `FHF-PROG-007` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | host/task 日志 | [source](../11-federation-hawq.md#L217) |
| `FHF-PROG-008` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | 安装链与部分幂等 | [source](../11-federation-hawq.md#L218) |
| `FHF-PROG-009` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | 删除幂等保护 | [source](../11-federation-hawq.md#L219) |
| `FHF-PROG-010` | `11-federation-hawq.md` | 通用进度、失败、重试与日志 | service/component mutation | [source](../11-federation-hawq.md#L220) |
| `FHF-REC-001` | `11-federation-hawq.md` | 退出、持久化与恢复 | step snapshot | [source](../11-federation-hawq.md#L226) |
| `FHF-REC-002` | `11-federation-hawq.md` | 退出、持久化与恢复 | progress 刷新 | [source](../11-federation-hawq.md#L227) |
| `FHF-REC-003` | `11-federation-hawq.md` | 退出、持久化与恢复 | NameNode/RBF 关闭 | [source](../11-federation-hawq.md#L228) |
| `FHF-REC-004` | `11-federation-hawq.md` | 退出、持久化与恢复 | HAWQ 关闭 | [source](../11-federation-hawq.md#L229) |
| `FHF-REC-005` | `11-federation-hawq.md` | 退出、持久化与恢复 | 完成 | [source](../11-federation-hawq.md#L230) |
| `FHF-REC-006` | `11-federation-hawq.md` | 退出、持久化与恢复 | wizard ownership | [source](../11-federation-hawq.md#L231) |
| `FHF-REC-007` | `11-federation-hawq.md` | 退出、持久化与恢复 | Federation cluster state | [source](../11-federation-hawq.md#L232) |
| `FHF-REC-008` | `11-federation-hawq.md` | 退出、持久化与恢复 | HAWQ cluster state | [source](../11-federation-hawq.md#L233) |
| `FHF-REC-009` | `11-federation-hawq.md` | 退出、持久化与恢复 | persist 权限 | [source](../11-federation-hawq.md#L234) |
| `FHF-API-001` | `11-federation-hawq.md` | 接口契约表 | \`hosts.high_availability.wizard\` GET | [source](../11-federation-hawq.md#L242) |
| `FHF-API-002` | `11-federation-hawq.md` | 接口契约表 | \`wizard.loadrecommendations\` POST | [source](../11-federation-hawq.md#L243) |
| `FHF-API-003` | `11-federation-hawq.md` | 接口契约表 | \`config.validations\` POST | [source](../11-federation-hawq.md#L244) |
| `FHF-API-004` | `11-federation-hawq.md` | 接口契约表 | \`config.tags\` GET | [source](../11-federation-hawq.md#L245) |
| `FHF-API-005` | `11-federation-hawq.md` | 接口契约表 | \`admin.get.all_configurations\` GET | [source](../11-federation-hawq.md#L246) |
| `FHF-API-006` | `11-federation-hawq.md` | 接口契约表 | \`reassign.load_configs\` GET | [source](../11-federation-hawq.md#L247) |
| `FHF-API-007` | `11-federation-hawq.md` | 接口契约表 | \`common.service.configurations\` PUT | [source](../11-federation-hawq.md#L248) |
| `FHF-API-008` | `11-federation-hawq.md` | 接口契约表 | \`common.service.multiConfigurations\` PUT | [source](../11-federation-hawq.md#L249) |
| `FHF-API-009` | `11-federation-hawq.md` | 接口契约表 | \`common.services.update\` PUT | [source](../11-federation-hawq.md#L250) |
| `FHF-API-010` | `11-federation-hawq.md` | 接口契约表 | \`host_component.installed.on_hosts\` GET | [source](../11-federation-hawq.md#L251) |
| `FHF-API-011` | `11-federation-hawq.md` | 接口契约表 | \`common.create_component\` POST | [source](../11-federation-hawq.md#L252) |
| `FHF-API-012` | `11-federation-hawq.md` | 接口契约表 | \`wizard.step8.register_host_to_component\` POST | [source](../11-federation-hawq.md#L253) |
| `FHF-API-013` | `11-federation-hawq.md` | 接口契约表 | \`common.host_components.update\` PUT | [source](../11-federation-hawq.md#L254) |
| `FHF-API-014` | `11-federation-hawq.md` | 接口契约表 | \`common.delete.host_component\` DELETE | [source](../11-federation-hawq.md#L255) |
| `FHF-API-015` | `11-federation-hawq.md` | 接口契约表 | \`service.item.executeCustomCommand\` POST | [source](../11-federation-hawq.md#L256) |
| `FHF-API-016` | `11-federation-hawq.md` | 接口契约表 | \`nameNode.federation.formatNameNode\` POST | [source](../11-federation-hawq.md#L257) |
| `FHF-API-017` | `11-federation-hawq.md` | 接口契约表 | \`nameNode.federation.formatZKFC\` POST | [source](../11-federation-hawq.md#L258) |
| `FHF-API-018` | `11-federation-hawq.md` | 接口契约表 | \`nameNode.federation.bootstrapNameNode\` POST | [source](../11-federation-hawq.md#L259) |
| `FHF-API-019` | `11-federation-hawq.md` | 接口契约表 | \`restart.custom.filter\` POST | [source](../11-federation-hawq.md#L260) |
| `FHF-API-020` | `11-federation-hawq.md` | 接口契约表 | \`background_operations.get_by_request\` GET | [source](../11-federation-hawq.md#L261) |
| `FHF-API-021` | `11-federation-hawq.md` | 接口契约表 | \`common.request.polling\` GET | [source](../11-federation-hawq.md#L262) |
| `FHF-API-022` | `11-federation-hawq.md` | 接口契约表 | \`background_operations.get_by_task\` GET | [source](../11-federation-hawq.md#L263) |
| `FHF-API-023` | `11-federation-hawq.md` | 接口契约表 | \`admin.security.cluster_configs.kerberos\` GET | [source](../11-federation-hawq.md#L264) |
| `FHF-API-024` | `11-federation-hawq.md` | 接口契约表 | \`kerberos.session.state\` GET | [source](../11-federation-hawq.md#L265) |
| `FHF-API-025` | `11-federation-hawq.md` | 接口契约表 | \`persist.get\` GET | [source](../11-federation-hawq.md#L266) |
| `FHF-API-026` | `11-federation-hawq.md` | 接口契约表 | \`persist.post\` POST | [source](../11-federation-hawq.md#L267) |
| `FHF-API-027` | `11-federation-hawq.md` | 接口契约表 | direct \`App.HttpClient.get\` GET | [source](../11-federation-hawq.md#L268) |
| `FHF-API-028` | `11-federation-hawq.md` | 接口契约表 | \`credentials.get\` GET | [source](../11-federation-hawq.md#L269) |
| `FHF-API-029` | `11-federation-hawq.md` | 接口契约表 | \`credentials.create\` POST | [source](../11-federation-hawq.md#L270) |
| `FHF-API-030` | `11-federation-hawq.md` | 接口契约表 | \`credentials.update\` PUT | [source](../11-federation-hawq.md#L271) |
| `FHF-API-031` | `11-federation-hawq.md` | 接口契约表 | \`config.on_site\` GET | [source](../11-federation-hawq.md#L272) |
| `FHF-RISK-001` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | 所有五条 route 缺权限和资源 guard | [source](../11-federation-hawq.md#L278) |
| `FHF-RISK-002` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | RBF Review 自动提前保存 | [source](../11-federation-hawq.md#L279) |
| `FHF-RISK-003` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | RBF 请求 sender 是错误全局对象 | [source](../11-federation-hawq.md#L280) |
| `FHF-RISK-004` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | RBF 假定 config type 已存在 | [source](../11-federation-hawq.md#L281) |
| `FHF-RISK-005` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | RBF progress 有不可达重配置代码 | [source](../11-federation-hawq.md#L282) |
| `FHF-RISK-006` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | Ranger defaultFS mapping 重复追加 | [source](../11-federation-hawq.md#L283) |
| `FHF-RISK-007` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | Federation persist 状态名不匹配 | [source](../11-federation-hawq.md#L284) |
| `FHF-RISK-009` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | 通用 Rollback 错接向导 | [source](../11-federation-hawq.md#L285) |
| `FHF-RISK-010` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | 关闭只是忘记状态，不撤销副作用 | [source](../11-federation-hawq.md#L286) |
| `FHF-RISK-011` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | HAWQ wizard owner 不一致 | [source](../11-federation-hawq.md#L287) |
| `FHF-RISK-012` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | 最后一项失败仍能完成 | [source](../11-federation-hawq.md#L288) |
| `FHF-RISK-013` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | HAWQ 已无当前 Server stack | [source](../11-federation-hawq.md#L289) |
| `FHF-RISK-014` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | component install 吞掉创建错误 | [source](../11-federation-hawq.md#L290) |
| `FHF-RISK-015` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | HAWQ Add 配置加载错误不一致 | [source](../11-federation-hawq.md#L291) |
| `FHF-RISK-016` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | HAWQ Activate 泄漏全局变量 | [source](../11-federation-hawq.md#L292) |
| `FHF-RISK-017` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | HAWQ Remove 不等待初始化 | [source](../11-federation-hawq.md#L293) |
| `FHF-RISK-018` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | HAWQ Remove/Activate Step 1 markup 无效 | [source](../11-federation-hawq.md#L294) |
| `FHF-RISK-019` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | component existence GET 失败会卡住 task | [source](../11-federation-hawq.md#L295) |
| `FHF-RISK-020` | `11-federation-hawq.md` | 旧版已确认缺陷与 React 处理原则 | Federation 配置读取失败会留下永久 gate | [source](../11-federation-hawq.md#L296) |
| `FHF-TEST-001` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | NameNode Federation tests | [source](../11-federation-hawq.md#L302) |
| `FHF-TEST-002` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | Router Federation tests | [source](../11-federation-hawq.md#L303) |
| `FHF-TEST-003` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | HAWQ Add tests | [source](../11-federation-hawq.md#L304) |
| `FHF-TEST-004` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | HAWQ Remove tests | [source](../11-federation-hawq.md#L305) |
| `FHF-TEST-005` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | HAWQ Activate tests | [source](../11-federation-hawq.md#L306) |
| `FHF-TEST-006` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | 排除项检查 | [source](../11-federation-hawq.md#L307) |
| `FHF-RUNTIME-001` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | NameNode HA 首次扩为 Federation；再次添加第三个 namespace | [source](../11-federation-hawq.md#L313) |
| `FHF-RUNTIME-002` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | Federation 分别安装/不安装 Ranger、Accumulo、Infra | [source](../11-federation-hawq.md#L314) |
| `FHF-RUNTIME-003` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | RBF 单/多 Router、缺失 \`hdfs-rbf-site\`、Step 3 关闭/请求失败 | [source](../11-federation-hawq.md#L315) |
| `FHF-RUNTIME-004` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | 自动 Kerberos、Manual Kerberos、KDC session 过期/取消 | [source](../11-federation-hawq.md#L316) |
| `FHF-RUNTIME-005` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | 每个 command 注入 FAILED/TIMEDOUT/ABORTED，刷新页面后 Retry | [source](../11-federation-hawq.md#L317) |
| `FHF-RUNTIME-006` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | Step 1/Review/progress 分别关闭；原用户/另一用户重进 | [source](../11-federation-hawq.md#L318) |
| `FHF-RUNTIME-007` | `11-federation-hawq.md` | 测试覆盖与运行态场景 | 兼容历史 HAWQ stack 的 Add/Remove/Activate 全流程与每阶段中断 | [source](../11-federation-hawq.md#L319) |
| `VIEW-SCOPE-001` | `12-views.md` | 范围与对象模型 | 后端对象层级是 View definition -> View version -> View instance；Ember 最终只建立扁平的 \`App.ViewInstance[]\` | [source](../12-views.md#L11) |
| `VIEW-SCOPE-002` | `12-views.md` | 范围与对象模型 | Ember 只消费 instance 的 icon、label、visible、version、description、view name、short URL、instance name 和 context path | [source](../12-views.md#L12) |
| `VIEW-SCOPE-003` | `12-views.md` | 范围与对象模型 | \`/views/{view}/{version}/{instance}/...\` 是 Ambari Server 提供的 View Web context，不是 \`/api/v1\` REST route，也不是 Ember hash route | [source](../12-views.md#L13) |
| `VIEW-SCOPE-004` | `12-views.md` | 范围与对象模型 | 内置 \`ADMIN_VIEW\` 是独立 AngularJS Admin Console；Ember 只有入口判断、版本发现和整页跳转 | [source](../12-views.md#L14) |
| `VIEW-ROUTE-001` | `12-views.md` | Route 与页面状态 | \`#/main/views/\`，\`main.views.index\` | [source](../12-views.md#L20) |
| `VIEW-ROUTE-002` | `12-views.md` | Route 与页面状态 | \`#/main/views/:viewName/:version/:instanceName\`，\`main.views.viewDetails\` | [source](../12-views.md#L21) |
| `VIEW-ROUTE-003` | `12-views.md` | Route 与页面状态 | \`#/main/view/\`，\`main.view.index\` | [source](../12-views.md#L22) |
| `VIEW-ROUTE-004` | `12-views.md` | Route 与页面状态 | \`#/main/view/:viewName/:shortName\`，\`main.view.shortViewDetails\` | [source](../12-views.md#L23) |
| `VIEW-ROUTE-005` | `12-views.md` | Route 与页面状态 | \`#/adminView\`，顶层 \`adminView\` state；route pattern 为 \`/adminView\` | [source](../12-views.md#L24) |
| `VIEW-ROUTE-006` | `12-views.md` | Route 与页面状态 | \`/views/:view/:version/:instance/...\`，无 \`#\` | [source](../12-views.md#L25) |
| `VIEW-LIST-001` | `12-views.md` | Instance 发现与列表 | 已登录时先查询是否存在任何 View definition | [source](../12-views.md#L33) |
| `VIEW-LIST-002` | `12-views.md` | Instance 发现与列表 | 加载所有 version 下的 instance 并扁平化 | [source](../12-views.md#L34) |
| `VIEW-LIST-003` | `12-views.md` | Instance 发现与列表 | 只纳入服务端返回的非 system、已部署 version 中，且 \`ViewInstanceInfo.visible\` 为 truthy 的 instance | [source](../12-views.md#L35) |
| `VIEW-LIST-004` | `12-views.md` | Instance 发现与列表 | 计算 instance 展示字段和 fallback | [source](../12-views.md#L36) |
| `VIEW-LIST-005` | `12-views.md` | Instance 发现与列表 | 显示 \`Your Views\` 表格 | [source](../12-views.md#L37) |
| `VIEW-LIST-006` | `12-views.md` | Instance 发现与列表 | 无可见 instance 时显示 \`No views\` | [source](../12-views.md#L38) |
| `VIEW-LIST-007` | `12-views.md` | Instance 发现与列表 | \`dataLoading()\` 每 50ms 等待 \`isDataLoaded=true\` 后才接 outlet | [source](../12-views.md#L39) |
| `VIEW-LIST-008` | `12-views.md` | Instance 发现与列表 | main、installer、显式 Views route 和登录分流都可触发 \`loadAmbariViews()\` | [source](../12-views.md#L40) |
| `VIEW-NAV-001` | `12-views.md` | 可到达入口 | View-only、无 cluster 权限或安装路由无权限时的自动分流 | [source](../12-views.md#L46) |
| `VIEW-NAV-002` | `12-views.md` | 可到达入口 | 已安装集群的顶部九宫格 Views 下拉 | [source](../12-views.md#L47) |
| `VIEW-NAV-003` | `12-views.md` | 可到达入口 | 直接访问 \`#/main/views\` 或 \`#/main/view\` | [source](../12-views.md#L48) |
| `VIEW-URL-001` | `12-views.md` | URL 选择与 instance 匹配 | 为列表项生成经典 UI 内部 URL | [source](../12-views.md#L56) |
| `VIEW-URL-002` | `12-views.md` | URL 选择与 instance 匹配 | regular URL 解析 | [source](../12-views.md#L57) |
| `VIEW-URL-003` | `12-views.md` | URL 选择与 instance 匹配 | short URL 解析 | [source](../12-views.md#L58) |
| `VIEW-URL-004` | `12-views.md` | URL 选择与 instance 匹配 | instance 匹配后保存 \`viewPath\` | [source](../12-views.md#L59) |
| `VIEW-URL-005` | `12-views.md` | URL 选择与 instance 匹配 | route 参数没有匹配任何已加载 instance | [source](../12-views.md#L60) |
| `VIEW-URL-006` | `12-views.md` | URL 选择与 instance 匹配 | 已认证状态刷新 regular/short deep link | [source](../12-views.md#L61) |
| `VIEW-PATH-001` | `12-views.md` | \`viewPath\` 转换算法 | 无 query | [source](../12-views.md#L76) |
| `VIEW-PATH-002` | `12-views.md` | \`viewPath\` 转换算法 | \`?foo=bar&count=1\` | [source](../12-views.md#L77) |
| `VIEW-PATH-003` | `12-views.md` | \`viewPath\` 转换算法 | \`?viewPath=%2Fuser%2Fadmin%2Faddress\` | [source](../12-views.md#L78) |
| `VIEW-PATH-004` | `12-views.md` | \`viewPath\` 转换算法 | \`?viewPath=%2Fuser%2Fadmin%2Faddress&foo=bar&count=1\` | [source](../12-views.md#L79) |
| `VIEW-PATH-005` | `12-views.md` | \`viewPath\` 转换算法 | \`?viewPath=%2F%23%2Ftez-app%2Fapplication_...\` | [source](../12-views.md#L80) |
| `VIEW-PATH-006` | `12-views.md` | \`viewPath\` 转换算法 | 非法 percent encoding，例如 \`?viewPath=%E0%A4%A\` | [source](../12-views.md#L81) |
| `VIEW-PATH-007` | `12-views.md` | \`viewPath\` 转换算法 | 普通 query 的其他参数名/值中仅包含子串 \`viewPath\` | [source](../12-views.md#L82) |
| `VIEW-IFRAME-001` | `12-views.md` | iframe 承载与渲染生命周期 | details outlet 本身渲染为 iframe | [source](../12-views.md#L90) |
| `VIEW-IFRAME-002` | `12-views.md` | iframe 承载与渲染生命周期 | 生成 iframe src | [source](../12-views.md#L91) |
| `VIEW-IFRAME-003` | `12-views.md` | iframe 承载与渲染生命周期 | details 使用宽屏 contrib-view 布局 | [source](../12-views.md#L92) |
| `VIEW-IFRAME-004` | `12-views.md` | iframe 承载与渲染生命周期 | 插入 iframe 时立即 resize，以后每 5 秒 resize | [source](../12-views.md#L93) |
| `VIEW-IFRAME-005` | `12-views.md` | iframe 承载与渲染生命周期 | 销毁 details view 时清理 resize interval | [source](../12-views.md#L94) |
| `VIEW-IFRAME-006` | `12-views.md` | iframe 承载与渲染生命周期 | iframe 内活动计入 Ambari inactivity timeout | [source](../12-views.md#L95) |
| `VIEW-IFRAME-007` | `12-views.md` | iframe 承载与渲染生命周期 | 经典 iframe 没有 sandbox 限制 | [source](../12-views.md#L96) |
| `VIEW-IFRAME-008` | `12-views.md` | iframe 承载与渲染生命周期 | iframe navigation 没有 Ember loading/error/retry UI | [source](../12-views.md#L97) |
| `VIEW-IFRAME-009` | `12-views.md` | iframe 承载与渲染生命周期 | View 内部导航不回写宿主 Ember URL | [source](../12-views.md#L98) |
| `VIEW-ONLY-001` | `12-views.md` | 判定语义 | 已有 cluster，\`isOnlyViewUser=true\` | [source](../12-views.md#L113) |
| `VIEW-ONLY-002` | `12-views.md` | 判定语义 | 已有 cluster，普通 cluster/Ambari 用户 | [source](../12-views.md#L114) |
| `VIEW-ONLY-003` | `12-views.md` | 判定语义 | 没有 cluster，\`isOnlyViewUser=true\` 或 authorization 为空 | [source](../12-views.md#L115) |
| `VIEW-ONLY-004` | `12-views.md` | 判定语义 | 没有 cluster，非 View-only 用户 | [source](../12-views.md#L116) |
| `VIEW-ONLY-005` | `12-views.md` | 判定语义 | cluster provisioning 未完成，当前不在 View route | [source](../12-views.md#L117) |
| `VIEW-ONLY-006` | `12-views.md` | 判定语义 | 直接进入 \`/installer\` 但无 \`AMBARI.ADD_DELETE_CLUSTERS\` | [source](../12-views.md#L118) |
| `VIEW-ONLY-007` | `12-views.md` | 判定语义 | 已认证用户直接打开 regular/short deep link | [source](../12-views.md#L119) |
| `VIEW-ONLY-008` | `12-views.md` | 判定语义 | 未认证用户直接打开 deep link后完成登录 | [source](../12-views.md#L120) |
| `VIEW-INIT-001` | `12-views.md` | 进入 main 的初始化请求链 | 1. \`main.enter\` 先确认认证 | [source](../12-views.md#L128) |
| `VIEW-INIT-002` | `12-views.md` | 进入 main 的初始化请求链 | 2. 认证成功后先加载 supports | [source](../12-views.md#L129) |
| `VIEW-INIT-003` | `12-views.md` | 进入 main 的初始化请求链 | 3. supports 请求完成后启动 keep-alive | [source](../12-views.md#L130) |
| `VIEW-INIT-004` | `12-views.md` | 进入 main 的初始化请求链 | 4. 同步等待 Ambari Server properties | [source](../12-views.md#L131) |
| `VIEW-INIT-005` | `12-views.md` | 进入 main 的初始化请求链 | 5. properties 成功后并行发现 Views并确认 cluster identity | [source](../12-views.md#L132) |
| `VIEW-INIT-006` | `12-views.md` | 进入 main 的初始化请求链 | 6a. cluster 已安装且为 View-only | [source](../12-views.md#L133) |
| `VIEW-INIT-007` | `12-views.md` | 进入 main 的初始化请求链 | 6b. cluster 未安装 | [source](../12-views.md#L134) |
| `VIEW-ONLY-009` | `12-views.md` | View-only 外壳差异 | 左侧运维导航 | [source](../12-views.md#L140) |
| `VIEW-ONLY-010` | `12-views.md` | View-only 外壳差异 | 顶部 Views 下拉及 cluster notifications | [source](../12-views.md#L141) |
| `VIEW-ONLY-011` | `12-views.md` | View-only 外壳差异 | Ambari logo / Dashboard 跳转 | [source](../12-views.md#L142) |
| `VIEW-ONLY-012` | `12-views.md` | View-only 外壳差异 | 用户菜单 | [source](../12-views.md#L143) |
| `VIEW-X-001` | `12-views.md` | Service 页面与 View 的交叉入口 | 普通 Service Quick Links 是另一套外部 Web UI 链接机制 | [source](../12-views.md#L151) |
| `VIEW-X-002` | `12-views.md` | Service 页面与 View 的交叉入口 | Hive summary 留有 View link 扩展点 | [source](../12-views.md#L152) |
| `VIEW-X-003` | `12-views.md` | Service 页面与 View 的交叉入口 | 通用 Service Summary 的 Views panel 已被注释 | [source](../12-views.md#L153) |
| `VIEW-X-004` | `12-views.md` | Service 页面与 View 的交叉入口 | 配置或服务端生成的 View deep link 可以携带 \`viewPath\` | [source](../12-views.md#L154) |
| `VIEW-ADMIN-001` | `12-views.md` | Admin View 发现与跳转 | 无 cluster 的登录后默认入口 | [source](../12-views.md#L162) |
| `VIEW-ADMIN-002` | `12-views.md` | Admin View 发现与跳转 | 用户菜单 \`Manage Ambari\` | [source](../12-views.md#L163) |
| `VIEW-ADMIN-003` | `12-views.md` | Admin View 发现与跳转 | Stack Versions 的 \`Manage Versions\` | [source](../12-views.md#L164) |
| `VIEW-ADMIN-004` | `12-views.md` | Admin View 发现与跳转 | 选择 Admin View version | [source](../12-views.md#L165) |
| `VIEW-ADMIN-005` | `12-views.md` | Admin View 发现与跳转 | 构造 Admin Console URL | [source](../12-views.md#L166) |
| `VIEW-ADMIN-006` | `12-views.md` | Admin View 发现与跳转 | \`ADMIN_VIEW\` 不进入普通 View 目录 | [source](../12-views.md#L167) |
| `VIEW-PERM-001` | `12-views.md` | 权限与可见性模型 | \`VIEW.USE\` authorization | [source](../12-views.md#L190) |
| `VIEW-PERM-002` | `12-views.md` | 权限与可见性模型 | \`VIEW.USER\` permission | [source](../12-views.md#L191) |
| `VIEW-PERM-003` | `12-views.md` | 权限与可见性模型 | 服务端 instance access | [source](../12-views.md#L192) |
| `VIEW-PERM-004` | `12-views.md` | 权限与可见性模型 | \`AMBARI.MANAGE_VIEWS\` | [source](../12-views.md#L193) |
| `VIEW-PERM-005` | `12-views.md` | 权限与可见性模型 | \`CLUSTER.UPGRADE_DOWNGRADE_STACK\` | [source](../12-views.md#L194) |
| `VIEW-PERM-006` | `12-views.md` | 权限与可见性模型 | \`AMBARI.MANAGE_STACK_VERSIONS\` | [source](../12-views.md#L195) |
| `VIEW-PERM-007` | `12-views.md` | 权限与可见性模型 | \`AMBARI.ADD_DELETE_CLUSTERS\` | [source](../12-views.md#L196) |
| `VIEW-PERM-008` | `12-views.md` | 权限与可见性模型 | wizard owner | [source](../12-views.md#L197) |
| `VIEW-RISK-001` | `12-views.md` | 已知旧版风险与 React 验收要求 | 两阶段 View 查询增加一次往返；reload 不重置 loaded且不去重并发 | [source](../12-views.md#L271) |
| `VIEW-RISK-002` | `12-views.md` | 已知旧版风险与 React 验收要求 | 空列表、403/500 和 instance load failure 显示相同 \`No views\` | [source](../12-views.md#L272) |
| `VIEW-RISK-003` | `12-views.md` | 已知旧版风险与 React 验收要求 | invalid regular/short deep link没有 not-found恢复，并可能复用 singleton details controller 的旧 content | [source](../12-views.md#L273) |
| `VIEW-RISK-004` | `12-views.md` | 已知旧版风险与 React 验收要求 | \`viewPath\` 依赖旧 Ember把 query附到最后一个动态参数；parser用 substring而非 query key识别且不捕获 \`decodeURIComponent\` 的 \`URIError\` | [source](../12-views.md#L274) |
| `VIEW-RISK-005` | `12-views.md` | 已知旧版风险与 React 验收要求 | iframe读取 content document、绑定事件且无 sandbox，隐含同源假设 | [source](../12-views.md#L275) |
| `VIEW-RISK-006` | `12-views.md` | 已知旧版风险与 React 验收要求 | iframe没有 load/error lifecycle，并用全局 selector取第一个 iframe resize | [source](../12-views.md#L276) |
| `VIEW-RISK-007` | `12-views.md` | 已知旧版风险与 React 验收要求 | View route无客户端逐 instance permission guard | [source](../12-views.md#L277) |
| `VIEW-RISK-008` | `12-views.md` | 已知旧版风险与 React 验收要求 | \`isOnlyViewUser\` 把空 authorization 当 View-only，并受 upgrade/wizard全局 gate影响 | [source](../12-views.md#L278) |
| `VIEW-RISK-009` | `12-views.md` | 已知旧版风险与 React 验收要求 | Manage Ambari 链接权限、\`/adminView\` route guard和 Manage Versions权限是三套不同条件 | [source](../12-views.md#L279) |
| `VIEW-RISK-010` | `12-views.md` | 已知旧版风险与 React 验收要求 | Admin version用字符串排序并假设至少一个合法 component version | [source](../12-views.md#L280) |
| `VIEW-RISK-011` | `12-views.md` | 已知旧版风险与 React 验收要求 | generic service-to-View panel和 Hive hook当前未启用 | [source](../12-views.md#L281) |
| `VIEW-RISK-012` | `12-views.md` | 已知旧版风险与 React 验收要求 | View-only 仍依赖 supports -> \`ambari.service\` -> cluster identity这条 main外壳链；\`ambari.service\` 或未完成 cluster 的 persist失败会阻断 success串接；keep-alive只在 logoff成功时显式关闭 | [source](../12-views.md#L282) |
| `GATE-AUTH-001` | `13-permissions-flags.md` | 权限判定语义 | 逗号分隔权限是 OR | [source](../13-permissions-flags.md#L9) |
| `GATE-AUTH-002` | `13-permissions-flags.md` | 权限判定语义 | \`isAuthorized\` 增加向导所有权限制 | [source](../13-permissions-flags.md#L10) |
| `GATE-AUTH-003` | `13-permissions-flags.md` | 权限判定语义 | \`havePermissions\` 不受向导所有权限制 | [source](../13-permissions-flags.md#L11) |
| `GATE-AUTH-004` | `13-permissions-flags.md` | 权限判定语义 | Upgrade 全局互斥及 OR 表达式污染 | [source](../13-permissions-flags.md#L12) |
| `GATE-AUTH-005` | `13-permissions-flags.md` | 权限判定语义 | 无权限模型即全部拒绝 | [source](../13-permissions-flags.md#L13) |
| `GATE-AUTH-006` | `13-permissions-flags.md` | 权限判定语义 | 只有 View 权限的用户走独立导航 | [source](../13-permissions-flags.md#L14) |
| `GATE-AUTH-007` | `13-permissions-flags.md` | 权限判定语义 | Cluster Administrator 提升前端角色标志 | [source](../13-permissions-flags.md#L15) |
| `GATE-AUTH-008` | `13-permissions-flags.md` | 权限判定语义 | UI 权限不是服务端授权替代品 | [source](../13-permissions-flags.md#L16) |
| `GATE-RUNTIME-001` | `13-permissions-flags.md` | 其他 Runtime UI Gates | \`App.stackVersionsAvailable=true\` | [source](../13-permissions-flags.md#L112) |
| `GATE-RUNTIME-002` | `13-permissions-flags.md` | 其他 Runtime UI Gates | \`App.upgradeHistoryAvailable=false\` | [source](../13-permissions-flags.md#L113) |
| `GATE-RUNTIME-003` | `13-permissions-flags.md` | 其他 Runtime UI Gates | \`App.enableDigitalClock=false\` | [source](../13-permissions-flags.md#L114) |
| `GATE-META-001` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | \`StackService.isInstallable\`、已安装 service names、service dependencies | [source](../13-permissions-flags.md#L120) |
| `GATE-META-002` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | service \`serviceTypes\` 包含 \`HA_MODE\` | [source](../13-permissions-flags.md#L121) |
| `GATE-META-003` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | service \`serviceTypes\` 包含 \`FEDERATION\` | [source](../13-permissions-flags.md#L122) |
| `GATE-META-004` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | service \`serviceTypes\` 包含 \`DFSRouter\` | [source](../13-permissions-flags.md#L123) |
| `GATE-META-005` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | component cardinality/min/max、\`isMaster\`、\`isClient\`、\`isSlave\`、\`isHAComponentOnly\` | [source](../13-permissions-flags.md#L124) |
| `GATE-META-006` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | \`components.reassignable\` | [source](../13-permissions-flags.md#L125) |
| `GATE-META-007` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | \`components.decommissionAllowed\` | [source](../13-permissions-flags.md#L126) |
| `GATE-META-008` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | stack component custom commands | [source](../13-permissions-flags.md#L127) |
| `GATE-META-009` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | config types/themes/value attributes/dependencies | [source](../13-permissions-flags.md#L128) |
| `GATE-META-010` | `13-permissions-flags.md` | Stack、Service 与 Component Metadata 条件 | Windows stack/stack family/version | [source](../13-permissions-flags.md#L129) |
| `GATE-STATE-001` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | service/component desired/current state | [source](../13-permissions-flags.md#L135) |
| `GATE-STATE-002` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | host/component/service maintenance | [source](../13-permissions-flags.md#L136) |
| `GATE-STATE-003` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | heartbeat/host health | [source](../13-permissions-flags.md#L137) |
| `GATE-STATE-004` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | stale configs/restart required | [source](../13-permissions-flags.md#L138) |
| `GATE-STATE-005` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | background request/schedule already active | [source](../13-permissions-flags.md#L139) |
| `GATE-STATE-006` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | \`wizardWatcherController.isNonWizardUser\` | [source](../13-permissions-flags.md#L140) |
| `GATE-STATE-007` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | upgrade state | [source](../13-permissions-flags.md#L141) |
| `GATE-STATE-008` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | Kerberos security type/KDC session | [source](../13-permissions-flags.md#L142) |
| `GATE-STATE-009` | `13-permissions-flags.md` | 状态、Maintenance 与长流程互斥 | cluster provisioning/wizard \`clusterState\` | [source](../13-permissions-flags.md#L143) |
| `GATE-RISK-001` | `13-permissions-flags.md` | 已知优先级与迁移风险 | \`!isWindows && isAuthorized(KERBEROS) \\|\\| upgradeRunning\` | [source](../13-permissions-flags.md#L149) |
| `GATE-RISK-002` | `13-permissions-flags.md` | 已知优先级与迁移风险 | Admin parent \`enter\` 接受四种权限 OR，但 \`routePath\` 主要要求 upgrade permission | [source](../13-permissions-flags.md#L150) |
| `GATE-RISK-003` | `13-permissions-flags.md` | 已知优先级与迁移风险 | Service actions 外层用多个 permissions OR | [source](../13-permissions-flags.md#L151) |
| `GATE-RISK-006` | `13-permissions-flags.md` | 已知优先级与迁移风险 | upgrade例外按整条 permission字符串判断 | [source](../13-permissions-flags.md#L152) |
| `GATE-RISK-004` | `13-permissions-flags.md` | 已知优先级与迁移风险 | Client feature flags 可在 Experimental 页面修改 | [source](../13-permissions-flags.md#L153) |
| `GATE-RISK-005` | `13-permissions-flags.md` | 已知优先级与迁移风险 | 某些权限只出现在独立 Admin Console入口 | [source](../13-permissions-flags.md#L154) |
