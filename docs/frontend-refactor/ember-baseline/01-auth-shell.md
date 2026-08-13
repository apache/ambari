# 认证、应用外壳与全局导航

## 功能清单

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| AUTH-001 | 访问 `/` 自动跳转登录页；登录页清空主界面标题并连接 login outlet | 未认证 | `router.login.clusters` | `app/router.js`，`app/templates/login.hbs` |
| AUTH-002 | 用户名密码使用 UTF-8 Base64 组成 Basic Authorization，先验证身份，再加载当前用户、privileges 和集群 | 失败时 403 显示认证错误，500 显示服务错误，其他错误显示通用失败 | `router.login`、`router.afterLogin` | `app/router.js`，`app/controllers/login_controller.js` |
| AUTH-003 | 支持 Knox/外部 JWT 登录跳转；401/403 响应中的 `jwtProviderUrl` 附加当前 URL 作为返回地址 | `/login/local` 永不跳外部认证；重定向次数超限显示错误弹窗 | `router.login.clusters` | `app/router.js` |
| AUTH-004 | 已被服务端认证但 localStorage 未标记时，从响应 `User` header 恢复用户会话 | 用户数据加载后调用 authorization 请求，但旧代码使用 `.complete()` 串联后续流程，权限加载失败也会继续加载登录消息并尝试进入主界面；这是迁移时不能复刻的失败路径缺陷 | `router.afterLogin`、authorization load | `app/router.js#afterLoginSuccessCallback` |
| AUTH-005 | 登录成功加载登录消息设置；启用的消息以 modal 展示，用户确认后继续 | 设置不存在、JSON 非法或请求失败时直接继续 | `router.login.message` | `app/router.js` |
| AUTH-006 | 根据集群和权限决定首屏：已安装集群进入 Dashboard/恢复路径，无集群进入 Admin View，仅 View 用户进入 Views，未完成集群进入 Installer | preferred target 只按首字符接受 `/` 或 `#` 并排除 login；`//host/path` 因此前缀检查仍会被接受，构成经典 UI 的 protocol-relative 跳转缺陷 | `router.login.clusters`、`ambari.service.load_server_version` | `app/router.js#restorePreferedPath` |
| AUTH-007 | 退出时停止主界面 polling、清 localStorage/向导状态/权限、发送 logoff，并回登录页 | 用户主动退出才向服务端发请求；已有集群数据时 complete 后刷新页面；keep-alive 标志只在 logoff 成功回调中清除，失败回调为空 | `router.logoff` | `app/router.js#logOff`、`app/router.js#logOffSuccessCallback` |
| AUTH-008 | 维持服务端会话 keep-alive，主路由进入后启动 | updater 由 `isPollerRunning` 控制；正常 logoff 成功会停止，但 logoff 失败时标志可能保持为 true，不能把“退出必定停止”作为等价要求 | 见“认证与应用外壳”模块接口目录 | `app/controllers/application.js#startKeepAlivePoller`，`app/routes/main.js`，`app/router.js#logOffErrorCallback` |
| SHELL-001 | 主应用进入时升级 localStorage schema，加载 supports、Ambari properties、cluster name、Views，再初始化全局模型和 polling | 未认证保存当前位置并跳登录；仅 View 用户不加载完整集群运维数据 | 多个 router/cluster 初始化请求 | `app/routes/main.js`，`app/controllers/main.js`，`app/controllers/global/cluster_controller.js` |
| SHELL-002 | 全局导航包含 Dashboard、Services、Hosts、Alerts、Admin、Views，并由权限、安装状态、已安装服务和 feature flag 控制 | 菜单可见不等于 route 可进入；route 仍执行权限重定向 | 无单一请求 | `app/templates/main.hbs`，`app/views/main/menu.js`，`app/routes/main.js` |
| SHELL-003 | 保存用户 preferred path，登录后恢复到原 route | 接受以 `/` 或 `#` 开头且不是 login 的值；经典实现错误接受 `//host`，React 必须使用同源/规范化 URL 校验而不是复刻该缺陷 | 无 | `app/router.js#savePreferedPath`、`app/router.js#restorePreferedPath` |
| SHELL-004 | 页面标题、breadcrumb、侧边服务菜单、全局 spinner/loading overlay 随 route 和模型加载状态更新 | 数据未加载时 route 等待 `dataLoading()` 或相应 deferred | 全局加载请求 | `app/routes/main.js`，`app/views/common/breadcrumbs_view.js` |
| SHELL-005 | Inactivity timeout 按 admin 与 readonly Ambari property 选择时长，监听 window/iframe 的 mousemove、keypress、click，每秒检查活跃时间，并在超时前 60 秒显示继续会话/退出倒计时 | wizard step 与 `/stack/upgrade` 期间跳过超时检查；超时或用户选择退出都会调用 logoff；设置来源是 Ambari properties，不是 User Settings popup | Ambari properties load；超时本身无额外读取请求 | `app/controllers/main.js#monitorInactivity`、`app/controllers/main.js#checkActiveness` |
| SHELL-006 | About 弹窗显示 controller 中已缓存的 Ambari Server 版本；没有 click-time 版本请求 | 缓存来自 Installer 或 Main controller；均没有值时显示 N/A，因此不存在 About 点击请求失败流程 | 无 click-time 请求 | `app/controllers/application.js#ambariVersion`、`app/controllers/application.js#showAboutPopup` |
| SHELL-007 | Experimental 页面读取支持开关，允许逐项修改并 Save/Cancel；Reset UI States 会清本地 DB/cache/向导/cluster status，并向服务端持久化 `wizard-data={}` | route 仅 `AMBARI.MANAGE_SETTINGS` 可进入；Reset 按钮另需 `CLUSTER.MANAGE_USER_PERSISTED_DATA` 且 non-wizard user 时禁用 | user supports preference、`persist.post` | `app/router.js#experimental`，`app/controllers/experimental.js`，`app/templates/experimental.hbs` |
| SHELL-008 | 版本不一致检查在 Installer 进入前阻止继续并提示 server/web client mismatch | 真实行为依赖打包版本，`CONDITIONAL` | Server 版本请求 | `app/controllers/installer.js`，`app/routes/installer.js` |
| SHELL-009 | User Settings popup 可保存是否自动展示 Background Operations 和 timezone，并列出当前用户的 cluster/View privileges | 菜单由 `AMBARI.MANAGE_SETTINGS` 控制，handler 又要求 `CLUSTER.UPGRADE_DOWNGRADE_STACK`，持久化最终要求 `CLUSTER.MANAGE_USER_PERSISTED_DATA`；这些不一致 gate 必须分别保留为基线事实 | `persist.get`、`persist.post`、`router.user.privileges` | `app/templates/application.hbs`，`app/controllers/global/user_settings_controller.js`，`app/templates/common/settings.hbs`，`app/mixins/common/persist.js` |

## 权限模型

登录后的 `privileges[].PrivilegeInfo.permission_name` 决定全局角色：

- `AMBARI.ADMINISTRATOR` 设置 `App.isAdmin`。
- 当前集群的 `CLUSTER.ADMINISTRATOR` 同时设置 `isAdmin` 和 `isOperator`。
- Cluster Operator、Service Administrator、Service Operator、Cluster User 等细粒度动作最终使用 `App.isAuthorized(permission)` 判断。
- 只有 View 权限、没有集群运维权限的用户进入 Views，不初始化完整运维页面。

详细 permission 与功能关联见 [13-permissions-flags.md](13-permissions-flags.md)。

## 全局 HTTP 语义

- 所有命名请求默认 timeout 为 `App.timeout`，默认 `Content-Type: text/plain`，用于避免 Knox 修改 JSON body。
- 没有自定义 error callback 时，500、401、407、413 等状态使用全局错误 modal，展示 method、URL、status 和服务端 message。
- KDC 相关 400 错误会被识别并改用 Invalid KDC popup。
- 同一 request 可以设置 loading popup；超过 500ms 才显示，complete 时清除。
- 超长 GET 自动转换为带 `X-Http-Method-Override: GET` 的 POST。

[generated/api-by-module/auth-shell.md](generated/api-by-module/auth-shell.md) 只是按请求名和 caller path 宽匹配的候选索引，可能混入或漏掉请求。权威网络核对必须联合 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。

## 占位与边界

- `/main/admin/authentication`、`/main/admin/advanced`、`/main/admin/audit` 在 `routes/main.js` 有 outlet 路由，但经典 Ember 树中未找到对应完整 controller/template，标记为 `PLACEHOLDER`，不能作为 React 必须复刻的已确认页面。
- `/main/test` 只在 `routes/main.js` 中连接 `mainTest` outlet；经典 Ember 树中没有对应 controller、template 或 view，标记为 `PLACEHOLDER`。该 route fragment 仍保留在静态目录中，但不生成 React 功能需求。
- 用户、组、角色和 View 实例管理由独立 AngularJS Admin Console 提供，不属于本文 Ember 基线。
