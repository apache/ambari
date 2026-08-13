# 基线整理方法

## 目标

这套基线回答四个问题：

1. 用户在经典 Ember 中能做什么？
2. 在什么权限、feature flag、服务状态和向导状态下可以做？
3. 操作会调用哪些后端接口，成功、失败和异步执行分别如何处理？
4. 哪些源码和测试能证明上述行为？

它不以“页面看起来相似”为完成标准，而以行为等价为标准。

## 功能记录字段

模块文档中的每个功能使用稳定 ID，并尽量包含以下字段：

| 字段 | 含义 |
| --- | --- |
| ID | 模块内稳定标识，后续 React 对照不得重新编号 |
| 入口 | route、菜单、按钮、弹窗或自动触发点 |
| 前置/权限 | permission、feature flag、已安装服务、组件状态、升级/向导互斥条件 |
| 用户行为 | 用户可执行的原子动作，而不是页面名称 |
| 成功结果 | 页面变化、模型刷新、后台 request、导航或下载 |
| 异常/边界 | 禁用、确认、服务端错误、轮询终止、重试、取消和恢复 |
| 请求 | `App.ajax` 请求名；直接请求用 `DIRECT:<位置>` |
| 证据 | route/controller/template/mixin/test 的源码位置 |

## 证据等级

| 标记 | 定义 |
| --- | --- |
| `CONFIRMED` | route/controller/template/API/test 中至少两类证据互相印证 |
| `STATIC_ONLY` | 静态代码存在，但需真实 Ambari Server、特定 stack 或外部服务验证 |
| `CONDITIONAL` | 仅在权限、feature flag、service/component/stack 条件成立时存在 |
| `PLACEHOLDER` | 只有路由或 outlet 壳，当前经典树中未找到完整页面实现 |
| `OUT_OF_SCOPE` | Metrics 或独立 AngularJS Admin Console 等明确排除内容 |

模块表默认均为 `CONFIRMED` 或 `CONDITIONAL`。其他状态会在条目中明确写出。

## 接口识别

经典 Ember 有四类网络路径：

1. `App.ajax.send({name: ...})`：请求名注册在 `app/utils/ajax/ajax.js`。默认 method 是 `GET`，默认 prefix 是 `/api/v1`，`format()` 可动态覆盖 method、URL、body 和 header。
2. 绕过命名注册表的直接 HTTP：包括 `App.HttpClient`、原生 `XMLHttpRequest` 和 jQuery AJAX；URL 常在 controller、view 或 util 中动态构造。
3. 浏览器导航和下载：Quick Link、View iframe、client config download、日志新窗口和本地生成文件可能通过 `window.open`、`href` 或 iframe 发起，不经过前两类请求封装。
4. STOMP 实时通道：原生 WebSocket 连接 `{ws|wss}://{host}{:port}/api/stomp/v1/websocket`，首次失败后以 SockJS `{http|https}://{host}{:port}/api/stomp/v1` 回退；destination、payload、订阅/退订、重连和 REST reconciliation 见 `generated/realtime-channels.json`。

长 GET URL 超过 `2048` 字符时，`App.ajax` 会改为 `POST` 并发送 `X-Http-Method-Override: GET`，查询表达式放入 `RequestInfo.query`。React 迁移不能只复制表面的 GET method。

接口目录中：

- URL 不包含默认 `/api/v1`；若定义覆盖 `apiPrefix`，会单独列出。
- Method 列的 `DYNAMIC` 只表示 HTTP method 依赖运行时数据；URL 依赖 `format()`/caller 表达式时单独记录 `hasDynamicUrl=true` 并在 Markdown 标 `DYNAMIC_URL`。两者不得混用。
- `ajax-endpoints.json` 的 `formatExpression` 保存注册表中的完整 `format()` 函数；这是 mutation body、header、dataType 和动态 URL 的定义侧权威证据。
- `ajax-calls.json` 逐调用点保存请求名表达式、内联 `data` 顶层键、回调类型和源码位置；这是同一请求在不同业务场景如何传参的调用侧权威证据。
- “调用者 0”表示在经典 `app/` 中未找到同名字符串引用，可能是遗留定义、动态构造或测试专用，不能直接当作用户功能。
- 动态请求对象和动态请求名不能仅靠语法安全归并；调用点仍以 `DYNAMIC` 保留，但 `tools/contracts/dynamic-ajax-resolutions.mjs` 已逐点审计其候选请求、dispatch条件和开放边界。`ajax-calls.json` 中 `RESOLVED_CLOSED` 表示候选闭集，`RESOLVED_OPEN_BOUNDARY` 表示当前经典调用者已枚举但 wrapper/model/mixin 仍允许运行时传入其他值。
- `UNREGISTERED` 表示静态请求名实际不在 AJAX 注册表；`App.ajax.send` 会 warning 并返回 `null`，不会发出 HTTP。必须区分用户可达旧缺陷和不可达遗留 controller，不能为 React虚构 endpoint。
- 相同 REST endpoint 可能由多个请求名以不同 payload、context、operation level 使用，迁移时不能仅按 URL 去重。
- `generated/api-by-module/` 不是模块接口全集。生成器只把请求名和调用者路径拼接后用宽正则启发式归类；共享请求可能跨模块重复或误归类，模块专属请求也可能被归到其他模块或“跨模块与待人工归类”，因此模块页的缺席不能证明旧 UI 没有该请求。

权威接口核对不能依赖任何单一目录或单一请求定义表，必须联合检查 `ajax-endpoints`（命名请求定义）、`ajax-calls`（实际调用点和动态 dispatch）、`direct-http-calls`（绕过注册表的 HTTP）、`browser-network-entrypoints`（导航、下载和 iframe）以及 `realtime-channels`（WebSocket/SockJS）。`api-by-module` 仅用于寻找候选入口，不能替代这五层证据；所有层仍应用下述 Metrics 排除规则。

## Metrics 排除规则

以下内容从功能和接口基线中排除：

- 请求名或源码模块明确属于 metrics、heatmap、timeline、chart 数据。
- 所有调用者均位于 Metrics/Heatmap/指标 Widget 代码中的通用请求。
- Dashboard/Service 的指标 Widget 管理。

以下内容保留：

- HA、decommission 等操作为判断安全条件而读取的 metrics 字段。
- 非 Metrics 的请求进度、主机健康、组件状态、告警状态和升级进度。
- 配置历史、日志搜索、后台 operation 和 service check。
- 同一直接 HTTP 响应同时含 topology/state 与指标字段时，只保留 component topology、state、maintenance、stale config、HA state、Active/Standby 等运维字段；指标数值不因共用响应而进入基线。
- `hosts.ips` 和 `hiveServerInteractive.getStatus` 虽由名为 `service_metrics_mapper` 的 mapper 调用，实际分别用于 host/IP 映射和 Hive Interactive Active/Standby quick-link 标识，按非指标运维能力保留。

## 静态提取局限

- 早期 Ember route 是嵌套对象，生成器只列 route fragment，不计算最终 URL。
- `name`、URL 或 method 经变量传递时，静态提取可能只能记录动态表达式。
- Handlebars 的动态 action、view 内部 click handler、observer 和定时器不一定出现在 action 清单。
- Stack service descriptors、theme JSON 和 server-side feature metadata 会改变可见服务、组件、配置项和命令。
- Knox、LDAP/Kerberos、Log Search、HAWQ 等能力需要真实外部环境才能验证完整结果。

## React 对比步骤

1. 以功能 ID 为行建立矩阵，不按 React 文件组织。
2. 对每行核对 route/入口、可见条件、权限、操作、payload、异步状态、错误路径和恢复行为。
3. 从 React API 层反向关联到功能 ID，找出“有接口无入口”和“有页面无请求”两类缺口。
4. 对 `STATIC_ONLY` 和 `CONDITIONAL` 项安排真实集群场景测试。
5. 将确认过的 React 测试路径写回矩阵，但不修改旧版事实描述。
