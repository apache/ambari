# React 认证与应用外壳实现对照

## 对照范围

| 项目 | 值 |
| --- | --- |
| Ember 基准 | `ember-baseline/01-auth-shell.md` |
| React 实现 | `ambari-web/latest`，分支 `auth-shell-module` |
| React 基线提交 | `8ac5c5a1346687bc46b4651c42b07237fe0e9ca9` |
| 模块功能 ID | `AUTH-001` 至 `AUTH-008`，`SHELL-001` 至 `SHELL-009` |
| 复核日期 | 2026-08-13 |
| Metrics 范围 | 排除；`/main/dashboard/metrics` 仅作为已有默认路由，不评价 Metrics 页面能力 |

状态遵循 `ember-baseline/14-react-gap-matrix.md`。当前代码、单元测试和生产构建已通过静态验证，但尚未连接真实 Ambari Server、Knox 和 Admin View，因此 17 项均标记为 `NEEDS_RUNTIME_VALIDATION`，不能提前标记为 `COVERED`。

## 当前结论

| 状态 | 数量 |
| --- | ---: |
| `NEEDS_RUNTIME_VALIDATION` | 17 |
| 合计 | 17 |

认证与应用外壳的静态缺口已经补齐：Router 始终挂载，会话探测先于应用初始化，user/privileges/authorizations/MOTD 原子加载，四类 landing、统一 logout、keep-alive、inactivity、About、Experimental、User Settings 和 Installer 版本 gate 均已接线。剩余门禁是使用真实服务端完成本文列出的角色、SSO、错误和恢复场景。

## 功能状态

| ID | 状态 | React 实现与自动化证据 | 待运行态验证 |
| --- | --- | --- | --- |
| `AUTH-001` | `NEEDS_RUNTIME_VALIDATION` | `App.tsx` 始终挂载 hash router；`AuthenticatedApplication` 分隔公开登录与受保护应用；未认证深链保存安全目标后跳 `/login`；根 `/` 不作为 preferred path | 未登录直接访问根、Dashboard、Installer、View 深链；服务端 session 过期后的跳转与恢复 |
| `AUTH-002` | `NEEDS_RUNTIME_VALIDATION` | `loginApi.authenticate()` 发送 UTF-8 Basic；用户名仅在 API URL 边界编码；`loadSession()` 要求 user 和 authorizations 全部成功；测试覆盖 UTF-8、单次编码和 authorization failure | 本地用户成功登录；403、500、网络错误的服务端实际 response body 与 UI 文案 |
| `AUTH-003` | `NEEDS_RUNTIME_VALIDATION` | `/login/local`、`jwtProviderUrl`、返回 URL、三次循环限制和 local recovery 已实现；plain business 403 不再误判 session 失效 | Knox JWT 成功返回；Knox 401/403 payload；代理下完整 return URL；循环超限后的 local 登录 |
| `AUTH-004` | `NEEDS_RUNTIME_VALIDATION` | `/clusters` session probe 读取 `User` header；本地状态缺失时恢复；authorization failure 不建 session；401 会同步清 React 内存 session | 容器/SSO 已认证且 localStorage 为空；header 大小写与代理透传；session 过期后的所有 lifecycle teardown |
| `AUTH-005` | `NEEDS_RUNTIME_VALIDATION` | user、authorization 与 MOTD 并行加载；启用且有文本时在 AppProvider 前阻断；缺失、禁用、非法和请求失败时继续 | `/settings/motd` 的合法、换行、非法 JSON、disabled、404、500 实际响应 |
| `AUTH-006` | `NEEDS_RUNTIME_VALIDATION` | `selectLandingPath()` 覆盖 installed、incomplete、View-only、no-cluster；Admin View 生成版本化浏览器 URL；测试覆盖四类 landing | 四种角色/集群组合；Admin View 部署版本选择；未完成 Installer 与进行中 wizard 的实际恢复 |
| `AUTH-007` | `NEEDS_RUNTIME_VALIDATION` | Navbar 和 inactivity 共用 context logout；先清内存、本地 DB 和 preferred path，再 best-effort `/logout`；服务端失败不阻塞导航；测试覆盖 rejected logout | `/logout` 成功、500、网络断开和长时间 pending；cookie 删除；STOMP、polling 和 iframe teardown |
| `AUTH-008` | `NEEDS_RUNTIME_VALIDATION` | 认证应用挂载后每 60 秒串行调用 `/clusters`，卸载或 logout 后停止 | 长会话保持；请求跨 60 秒时无并发；session 过期 response；登出后 Network 面板无后续请求 |
| `SHELL-001` | `NEEDS_RUNTIME_VALIDATION` | AppProvider 加载 supports、wizard owner、Ambari properties/version、cluster；运维用户再加载 services/hosts/upgrade/background/STOMP；View-only 跳过运维初始化并使用最小壳层；初始化失败可 Retry | installed/incomplete/no-cluster/View-only 的实际请求清单；每个初始化 API 故障和 Retry；STOMP reconnect/deactivate |
| `SHELL-002` | `NEEDS_RUNTIME_VALIDATION` | `/main`、`/installer`、`/experimental`、`/adminView` 位于统一认证边界；Admin 子菜单和 route 分开授权；Auto Start 菜单保留两组 AND、route 保留 Manage OR；Kerberos/Auto Start 使用 feature guard | read-only、service operator、cluster admin、Ambari admin、View-only；upgrade 与 non-wizard owner；直接输入各受限 route |
| `SHELL-003` | `NEEDS_RUNTIME_VALIDATION` | `normalizeInternalPath()` 只接受单斜杠内部路径，排除根、login、absolute 和 `//host`；值恢复后消费；测试覆盖安全/拒绝样例 | 浏览器刷新、SSO 往返、session expiry 和主动 logout；各 wizard 写入/清除自己的服务端恢复键 |
| `SHELL-004` | `NEEDS_RUNTIME_VALIDATION` | `DocumentTitleUpdater` 只更新 `document.title`；认证和 shell 初始化分别显示 loading/error/retry；不再渲染标题占位 DOM | cluster 名、Installer、Views、Experimental 和深链 title；慢请求进度；初始化失败后 Retry 成功 |
| `SHELL-005` | `NEEDS_RUNTIME_VALIDATION` | admin/readonly property 分流；大于 0 才启用；60 秒 warning；Continue/Sign Out；window 和动态 iframe activity；wizard/upgrade 排除；测试覆盖 timeout 选择、倒计时和排除路径 | admin/readonly 实际 property 单位；同源和跨域 View iframe；动态 iframe；warning 两个按钮；路由切换和 timeout=0 |
| `SHELL-006` | `NEEDS_RUNTIME_VALIDATION` | Ambari version 初始化时缓存；About 只读 context，缺失显示 `N/A`，无 click-time 请求 | Main、Installer、View-only 和 no-cluster；properties 请求失败；重复打开 About 的 Network 记录 |
| `SHELL-007` | `NEEDS_RUNTIME_VALIDATION` | `/experimental` 由 `AMBARI.MANAGE_SETTINGS` 保护；经典 supports key；Save 同步共享 context；Reset 另需 persisted-data 权限且 non-owner 禁用；服务端 reset 成功后才清本地并整页 reload | supports 存在/缺失/非法；Save/Cancel；Reset 成功/失败；另一用户持有 Installer、Kerberos、HA wizard |
| `SHELL-008` | `NEEDS_RUNTIME_VALIDATION` | Maven 注入 `VITE_AMBARI_VERSION`；client/server 都非空且不一致才阻断 Installer；测试覆盖 empty/match/mismatch | Maven packaged artifact；相同版本、不一致版本和开发构建空版本；阻断页面显示两端版本 |
| `SHELL-009` | `NEEDS_RUNTIME_VALIDATION` | 菜单 `AMBARI.MANAGE_SETTINGS`、handler `CLUSTER.UPGRADE_DOWNGRADE_STACK`、保存 `CLUSTER.MANAGE_USER_PERSISTED_DATA` 三层 gate 保留；background/timezone/default persist、cluster/View privileges 和错误反馈已实现 | 三层不同权限组合；首次登录默认写回；字符串/JSON persist；timezone reload；无 privileges、Ambari admin 和 API failure |

## 后端接口对照

| Ember 契约 | React 实现 | 静态结论 | 运行态门禁 |
| --- | --- | --- | --- |
| `POST /api/v1/auth` | `LoginApi.authenticate()` | UTF-8 Basic、`Content-Type: text/plain`、`skipAuthRedirect` 已实现 | Knox/proxy header 和 403/500 body |
| `GET /api/v1/users/{userName}?fields=*,privileges/...` | `LoginApi.handleSuccessfulLogin()` | username 单次编码；user 与 privilege 原子建 session | 特殊字符用户名和真实 privilege shape |
| `GET /api/v1/users/{userName}/authorizations?fields=*` | `LoginApi.loadAuthorizationsCallback()` | 失败时整个 session 失败，不复刻 Ember `.complete()` 缺陷 | 不同角色 authorization 集合和 401/403/500 |
| `GET /api/v1/users/{userName}/privileges?fields=*` | `LoginApi.loadPrivileges()` | User Settings 独立读取 cluster/View 字段 | View instance/version/name 和无 privilege response |
| `GET /api/v1/clusters?...` | `probeSession()`、`getClusterData()`、`noopPolling()` | session probe、cluster 初始化和 keep-alive 职责分开；probe 可读 `User` header | SSO header、无集群、未完成集群、session expiry |
| `GET /api/v1/settings/motd` | `LoginApi.loadLoginMessage()` | 容错解析和阻断确认已实现 | 合法/非法/404/500 payload |
| `GET /api/v1/logout` | `LoginApi.logout()` | 统一调用；客户端清理不等待服务端成功 | cookie、proxy、pending/失败请求 |
| `GET /api/v1/services/AMBARI/components/AMBARI_SERVER` | `ClusterApi.loadAmbariProperties()` | properties 和 component version 一次初始化并缓存 | 各部署版本的 response shape |
| `GET/POST /api/v1/persist[/key]` | `ClusterApi.getPersistData()`、`postPersistData()` | User Settings、supports、wizard owner 使用经典 JSON-string value 契约 | 404/default、权限拒绝、并发 wizard owner |
| `GET /api/v1/services/AMBARI?...component_version...` | `ServiceApi.getAmbariServerVersion()` | Admin View 选择最新版本并构造 browser URL | 多版本排序、空 response、Admin View 未安装 |

## 五轮实现审计

| 轮次 | 独立入口 | 本轮发现 | 修正结果 |
| --- | --- | --- | --- |
| 1. Route 与 landing | Router、公开/受保护边界、四类首屏、深链 | 根 `/` 被保存为 preferred path 会自重定向；`/adminView` 缺经典 route guard | 根路径不再保存；Admin View 对已有集群保留 Upgrade guard、无集群允许 Ambari admin |
| 2. 认证与 session | Basic、JWT、User header、MOTD、logout、keep-alive | 全局 401 只改 hash但不清 React session；plain 403 会被误当 session expiry；logout 会等待服务端 | 增加 session-expired 事件；401/JWT challenge 与业务 403 分流；logout 先完成客户端清理并后台 logoff |
| 3. API 与持久化 | 全部十类 Auth/Shell endpoint、URL 编码、persist value | session 探测失败会全量清 DB，破坏 Installer/HA/Kerberos 恢复；Reset 先清本地再请求服务端 | session-only cleanup 保留 wizard namespace；主动 logout/reset 才全量清理；Reset 改为 server-first |
| 4. 权限与状态 | menu/route/handler/save gate、upgrade、wizard owner、feature flags | Settings 首次缺省值未写回；Navbar 在 upgrade 时显示无响应项；`wizardIsNotFinished` 漏 non-owner | 权限允许时写回默认值；受限菜单按经典语义隐藏；全局 wizard 状态包含 non-owner |
| 5. 异步与失败恢复 | STOMP、polling、iframe、version、View-only、错误/Retry | STOMP client 每 render 新建；动态 iframe 未监听；Modal 泄漏内部 props；View-only 仍暴露集群控件 | 固定单一 STOMP client并 teardown；MutationObserver 管理 iframe；过滤 Modal props；View-only 使用最小导航 |

## 有意不复刻的 Ember 缺陷

| Ember 行为 | React 决定 |
| --- | --- |
| preferred path 只看首字符，错误接受 `//host` | 拒绝 protocol-relative 和 absolute URL |
| authorization 请求通过 `.complete()` 串联，失败仍可能继续 | user、privileges、authorizations 任一失败均不建立 session |
| logoff 失败可能保留 keep-alive 标志 | 客户端退出和 lifecycle teardown 不依赖服务端 logoff 结果 |
| Experimental Reset 不等待服务端 persist 完成 | 先成功清服务端 wizard owner，再清本地并整页恢复 session |

## 自动化证据

`npm test` 当前运行 7 个 Vitest 文件、32 个测试，覆盖：

- UTF-8 Basic Authorization 和 username 单次编码。
- authorization failure 不建立 session，`User` header 恢复 session，logout failure cleanup。
- JWT/local 分支、redirect limit、session-expired 事件和 plain 403 分流。
- `//host`、absolute、login 和根路径拒绝，以及安全内部路径接受。
- installed/default、preferred、incomplete、no-cluster 和 View-only landing。
- client/server version empty、match、mismatch。
- persist JSON values、fallback、admin/readonly inactivity、wizard/upgrade exclusion和倒计时。
- Admin View existing-cluster guard 与 no-cluster Ambari admin landing。

`npm run build` 已通过。现存 Sass deprecation、重复 switch、`eval` 和大 bundle 警告来自基线，不作为本模块完成证据，也不在本模块范围内修复。全仓 `npm run lint` 仍被 `frontend-refactor` 基线的大量 `any`、`@ts-ignore` 和 hook dependency 技术债阻断；本模块新增的策略、API、layout、guard、页面和测试文件无 ESLint error。

## 运行态验收矩阵

至少执行以下场景后才允许将对应 ID 改为 `COVERED`：

1. 本地 Basic 登录：成功、错误密码、500、网络失败、特殊字符用户名。
2. Knox JWT：成功往返、`/login/local` bypass、循环超限、代理 return URL。
3. 服务端已有会话且 localStorage 为空；运行中 session 过期。
4. installed cluster、incomplete cluster、no-cluster Ambari admin、View-only 四类 landing。
5. admin timeout、readonly timeout、timeout=0、Continue、Sign Out、wizard/upgrade exclusion、同源/跨域动态 View iframe。
6. MOTD enabled/disabled/malformed/404/500。
7. logout 成功、失败、pending，确认无后续 keep-alive、polling 或 STOMP 工作。
8. User Settings 三层权限、首次默认写回、timezone reload、cluster/View privilege 列表。
9. Experimental Save/Cancel/Reset、persist failure、non-wizard owner。
10. Admin View navigation、版本请求失败 fallback、Maven packaged version match/mismatch。

## Issue 与 PR 粒度

本模块只使用一个主 JIRA 和一个主 PR，不按单个功能 ID 拆分。JIRA 英文草稿位于 `react-current/issues/01-auth-shell.md`；PR 必须以 `apache/ambari:frontend-refactor` 为 base，并推送到 `JiaLiangC/ambari`。
