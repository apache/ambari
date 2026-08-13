# Ambari Web Ember 功能基线

本文档集描述 `frontend-refactor` 分支中 `ambari-web/classic` 的非 Metrics 用户功能和后端接口，作为后续 React 查缺补漏与行为等价验收的权威旧版基线。静态记录与源码或运行态冲突时，仍必须回查源码并验证运行态，不能以文档覆盖事实。

## 基线信息

| 项目 | 值 |
| --- | --- |
| Git 分支 | `frontend-refactor` |
| Git 提交 | `8ac5c5a1346687bc46b4651c42b07237fe0e9ca9` |
| 经典前端 | `ambari-web/classic`，早期 Ember `Em.Router/Em.Route` 架构 |
| React 对照工程 | `ambari-web/latest` |
| REST 默认前缀 | `/api/v1` |
| 静态基线规模 | 288 个非 Metrics 命名 AJAX 定义、394 个纳入调用点（27 个动态、3 个未注册）、19 个直接 HTTP 调用点、56 个浏览器网络候选、5 种 client config 下载、160 个 route fragment、299 个模板 action、1002 个稳定功能 ID |

提交号是分析起点，不表示文档只能用于该提交。经典前端发生修改后，应重新运行提取器并审阅生成差异。

## 范围

纳入范围：

- `ambari-web/classic/app` 中登录、会话、全局导航、后台操作、集群安装、Hosts、Services、Configs、Alerts、Stack/Versions/Upgrade、Kerberos、HA/Federation、Views 等 Ember 功能。
- 页面可见性、按钮权限、feature flag、服务/组件/stack 前置条件、向导恢复、失败重试和确认弹窗。
- `App.ajax` 命名请求及动态 dispatch、绕过命名注册表的 `App.HttpClient`/`XMLHttpRequest`/jQuery AJAX、浏览器导航/下载和 STOMP/WebSocket/SockJS 实时通道。
- 能证明旧版行为的 controller、route、template、mixin、view、model、mapper 和 test 位置。

明确排除：

- 所有 Metrics 能力，包括 Dashboard Metrics、Cluster Metrics、Host/Service Metrics、Heatmap、Horizon Chart、AMS timeline 查询、指标图表和指标数据导出。
- 依赖指标定义的 Dashboard/Service Widget 布局、创建、编辑、共享和删除。
- `ambari-admin/src/main/resources/ui/admin-web` 的 AngularJS Admin Console。用户、组、角色、集群权限、View 实例管理属于另一个旧前端，不是 Ember 功能；本文只记录 Ember 跳转到 Admin View 的行为。
- 后端 API 本身存在但经典 Ember 从未引用的服务端能力。

注意：HA 前置检查可能读取 `metrics/...` 字段，例如 NameNode checkpoint 时间。这是 HA 运维流程而不是 Metrics 展示功能，因此仍纳入。

## 文档导航

| 文档 | 内容 |
| --- | --- |
| [00-methodology.md](00-methodology.md) | 证据等级、功能记录字段、接口提取规则和 React 对比方法 |
| [01-auth-shell.md](01-auth-shell.md) | 登录、SSO、本地登录、会话、全局加载、权限和导航 |
| [02-background-dashboard.md](02-background-dashboard.md) | 后台操作、轮询、Dashboard 非 Metrics、配置历史 |
| [03-hosts.md](03-hosts.md) | Hosts 列表、批量操作、详情、组件、配置、告警、版本、日志、加主机向导 |
| [04-services-configs.md](04-services-configs.md) | Services 导航、服务动作、摘要、配置、配置组、加服务和迁移 Master |
| [05-alerts.md](05-alerts.md) | Alert Definitions、实例、通知、组、创建/编辑/删除和权限 |
| [06-stack-upgrades-admin.md](06-stack-upgrades-admin.md) | Stack/Versions、仓库、版本安装、升级/降级、升级历史和 Admin 集群设置 |
| [07-cluster-installation.md](07-cluster-installation.md) | 0 到 10 步集群安装向导、恢复、校验、部署和完成 |
| [08-kerberos.md](08-kerberos.md) | MIT/AD/IPA/Manual Kerberos、八步启用、禁用、identities、keytabs 和 KDC credentials |
| [09-namenode-journalnode-ha.md](09-namenode-journalnode-ha.md) | NameNode HA、JournalNode Management、checkpoint 与 rollback |
| [10-rm-ranger-ha.md](10-rm-ranger-ha.md) | ResourceManager HA 与 Ranger Admin HA |
| [11-federation-hawq.md](11-federation-hawq.md) | NameNode/Router Federation 与 HAWQ standby 长流程 |
| [12-views.md](12-views.md) | Views 列表、长短 URL、iframe、只拥有 View 权限的用户和 Admin View 跳转 |
| [13-permissions-flags.md](13-permissions-flags.md) | 权限、feature flag、服务/stack/component/status 条件统一索引 |
| [14-react-gap-matrix.md](14-react-gap-matrix.md) | 后续 React 对照的状态、场景和评审门禁 |
| [15-five-pass-audit.md](15-five-pass-audit.md) | 五轮反向审计的输入、发现、修正和剩余风险 |
| [api/README.md](api/README.md) | 接口目录入口、调用约定和常用 payload 语义 |

## 自动生成的证据目录

| 文件 | 用途 |
| --- | --- |
| [generated/ajax-endpoints.md](generated/ajax-endpoints.md) | 非 Metrics 命名 AJAX 定义、固定/动态 method、独立的动态 URL 标记、调用位置和已排除 Metrics 请求 |
| [generated/ajax-endpoints.json](generated/ajax-endpoints.json) | 供脚本和后续 React 差异分析使用的结构化版本 |
| [generated/ajax-calls.md](generated/ajax-calls.md) | 394 个调用点、动态请求候选解析和3个未注册旧调用 |
| [generated/direct-http-calls.md](generated/direct-http-calls.md) | 绕过 `App.ajax` 的直接 HTTP 调用 |
| [generated/browser-network-entrypoints.md](generated/browser-network-entrypoints.md) | 浏览器导航、reload、下载、动态图片/iframe及本地窗口入口；普通构建静态资源明确排除 |
| [generated/client-config-downloads.md](generated/client-config-downloads.md) | 五种 client config resource scope下载契约 |
| [generated/realtime-channels.md](generated/realtime-channels.md) | STOMP transport、11个destination、payload、生命周期和故障边界 |
| [generated/permissions.md](generated/permissions.md) | 经典代码实际消费的 permission名称与调用点 |
| [generated/feature-flags.md](generated/feature-flags.md) | `App.supports` feature flag名称与调用点 |
| [generated/routes.md](generated/routes.md) | 非 Metrics route fragment 及定义位置 |
| [generated/template-actions.md](generated/template-actions.md) | 非 Metrics 模板 action 及出现位置 |
| [generated/feature-index.md](generated/feature-index.md) | 手写模块稳定 ID的机器索引 |
| [generated/api-by-module](generated/api-by-module) | 按请求名和调用路径宽正则自动归类的候选索引；会跨模块混入、重复和漏项，不是接口全集 |

重新生成：

```bash
node docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
```

提取器只依赖 Node.js 内置模块，不要求 `npm install`。生成文件不得手工修改。

## 使用规则

1. 旧版功能是否存在，以模块文档中的稳定功能 ID 为准。
2. 网络契约核对必须联合 `generated/ajax-endpoints.json`、`ajax-calls.json`、`direct-http-calls.json`、`browser-network-entrypoints.json` 和 `realtime-channels.json`；不得用 `api-by-module` 或其中任一单表代替联合核对。
3. 模块文档描述用户行为；生成目录描述静态事实。两者冲突时必须回到源码和运行态验证，不能直接猜测。
4. React 对比不得仅比较路由或组件文件名，必须比较入口、权限、成功结果、失败路径、异步请求和恢复行为。
5. 一个功能只有在 React 的 UI、API、权限、错误处理和测试全部等价时，才能标记为 `COVERED`。
