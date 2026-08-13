# Ember 后端接口目录

## 权威联合目录

不存在一份可以单独充当“接口全集”的生成文件。权威核对必须联合命名请求定义、实际调用点、直接 HTTP、浏览器网络入口和实时通道五层证据，并继续应用非 Metrics 范围规则。

- [../generated/ajax-endpoints.md](../generated/ajax-endpoints.md)：非 Metrics `App.ajax` 请求定义、method、URL、`format()` 输入键和调用位置。
- [../generated/ajax-endpoints.json](../generated/ajax-endpoints.json)：定义侧结构化契约，包含完整 `formatExpression`，用于还原动态 URL、body、header 和 dataType。
- [../generated/ajax-calls.md](../generated/ajax-calls.md)：每个纳入范围的 `App.ajax.send(...)` 调用点、调用参数键、回调和动态请求表达式。
- [../generated/ajax-calls.json](../generated/ajax-calls.json)：调用侧结构化契约，后续可与 React query/mutation 调用自动比较。
- [../generated/realtime-channels.md](../generated/realtime-channels.md)：2 种 STOMP transport、11 个 destination、payload消费、订阅生命周期、重连及 REST reconciliation。
- [../generated/realtime-channels.json](../generated/realtime-channels.json)：与人工审计 contract精确一致的实时通道结构化契约，包含前后端源码及测试位置。
- [../generated/direct-http-calls.md](../generated/direct-http-calls.md) / [JSON](../generated/direct-http-calls.json)：绕过命名注册表的 `HttpClient`、jQuery AJAX 和原生 XHR 调用点；`MIXED` 只纳入非指标字段。
- [../generated/browser-network-entrypoints.md](../generated/browser-network-entrypoints.md) / [JSON](../generated/browser-network-entrypoints.json)：`window.open`、下载链接、redirect 等浏览器网络候选，需人工区分远程请求和本地文档窗口。
- [../generated/permissions.md](../generated/permissions.md)：静态 permission 名与所有调用点。
- [../generated/feature-flags.md](../generated/feature-flags.md)：实际消费的 feature flag 与所有调用点。
- [../generated/api-by-module](../generated/api-by-module)：按请求名和调用者路径宽正则生成的启发式候选视图。它会混入、重复或漏掉模块请求，不是权威接口清单。

目录中的数量由生成器写入文件并由校验器与 README 核对，不在本页重复硬编码，避免经典前端变化后产生过期数字。

## URL 与 method 规则

1. `real` URL 默认以 `/api/v1` 为前缀。
2. 未写 `type` 时为 `GET`。
3. `format(data, opt)` 可以改写 method、URL、body、header 和 dataType。当前注册表中没有真正的动态 method；固定 method 由顶层 `type` 与 `format()` 返回对象的顶层 `type` 联合提取。caller-supplied/表达式 URL 以 `hasDynamicUrl=true` 和 `DYNAMIC_URL` 单独标记，不能再用 Method 列的 `DYNAMIC` 兼任 URL 标记。
4. GET URL 超过 2048 字符时实际发送 `POST`，header 为 `X-Http-Method-Override: GET`，body 为 `{"RequestInfo":{"query":"..."}}`。
5. JSON mutation 多数仍使用 `Content-Type: text/plain`，这是兼容 Knox 的既有行为。
6. Ambari 异步 mutation 常返回 request ID；UI 随后轮询 `/clusters/{cluster}/requests/{id}`、stage 和 task。
7. `DYNAMIC` 调用保留原始运行时表达式，同时由 `candidateRequestNames`、`dispatchCondition` 和 `resolutionStatus` 给出人工解析；闭集与开放 wrapper边界不得混为未注册。
8. `UNREGISTERED` 静态请求名不会发 HTTP；当前 3 处分别是两个未接线 NN HA rollback 遗留调用，以及一个依赖 Server 已删除的 `UPGRADE_FAILED` 状态、正常生产响应不可达的 Host Component Re-upgrade 死分支。三者都不能作为 React endpoint。
9. STOMP通道不经过 `App.ajax`；两种 transport均失败时没有全局 REST polling替代，必须逐 destination核对 snapshot、丢事件后的收敛和退订。

实现某个调用时至少应先从 `ajax-endpoints.json` 读取请求定义和 `formatExpression`，再从 `ajax-calls.json` 找对应业务调用的实际参数键、回调与源码；同时排查 `direct-http-calls.json`、`browser-network-entrypoints.json`，涉及推送时还要查 `realtime-channels.json`。只复制 endpoint 会丢失绕过注册表的请求、下载/导航、`RequestInfo.context`、operation level、predicate、动态 method、推送后的 mapper更新或错误恢复行为。

## 常见 payload 结构

| 场景 | 关键结构 |
| --- | --- |
| Service/Component 状态变更 | `RequestInfo.context`、`operation_level`、`Body.ServiceInfo` 或 `Body.HostRoles` |
| 批量主机/组件操作 | `RequestInfo.query` 或 URL predicate，body 中提供目标 `HostRoles` |
| 自定义命令 | `RequestInfo.command`、`context`、`operation_level`、`resource_filters` |
| 保存配置 | cluster PUT，`Clusters.desired_config` 或 config group/desired config 集合 |
| 创建 request schedule | `RequestSchedule`、`RequestScheduleBatch` 和批量请求列表 |
| Upgrade | repository version、direction、upgrade type、request options；暂停/重试/终止通过 upgrade PUT |
| Alert | `AlertDefinition`、`AlertTarget`、`AlertGroup` 资源 |
| Wizard | cluster/service/component/host 分阶段创建，部署后通过 request/task polling |

## 接口迁移核对

React 不能只核对 URL 和 method，还必须核对：

- query predicate、`fields`、`minimal_response` 与分页参数；这些会影响 mapper 和页面完整性。
- `RequestInfo.context` 和 `operation_level`；它们直接影响 Background Operations 展示与服务端锁粒度。
- 请求返回 200、201、202 或空 body 的不同分支。
- request ID 解析、轮询终止状态、abort、retry 和失败 task 日志。
- KDC、Knox、超长 GET 和浏览器下载等特殊 header/dataType。
- 权限不足、升级/向导互斥、maintenance mode 等操作前判断。

模块文档中的“后端请求”列使用请求名引用本目录。同一功能列出多个请求名时，顺序通常对应加载、mutation、polling/刷新。
