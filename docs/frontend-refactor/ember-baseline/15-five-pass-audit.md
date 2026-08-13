# Ember 基线五轮独立反向审计

本报告记录对基线提交 `8ac5c5a1346687bc46b4651c42b07237fe0e9ca9` 中 `ambari-web/classic` 非 Metrics 功能与接口的五轮独立反向审计。每轮从不同入口重新枚举源码事实，并记录输入规模、发现、已落入模块文档的修正和无法由静态分析消除的风险。

这里的“通过”仅表示生成物、源码位置和手写基线静态一致。本轮没有运行旧 Karma suite，也没有在真实 Ambari Server、Agent、KDC、HA topology 或历史 HAWQ stack 上做端到端验证，不能据此声称运行态通过。

## 审计结果

| 轮次 | 输入与口径 | 主要发现 | 实际修正 | 剩余风险 |
| --- | --- | --- | --- | --- |
| 1. Route 与入口反查 | 160 个非 Metrics route 记录，来自 21 个 router/route 文件；因嵌套向导重复使用 `/stepN`，只有 64 个不同 literal fragment。逐项对照菜单、route guard、点击后二次检查和直接深链 | Installer route 硬校验 `AMBARI.ADD_DELETE_CLUSTERS`；Add Service 的 UI 与 route 都校验 permission + flag；Add Host 只有 UI 校验 `HOST.ADD_DELETE_HOSTS`，`/host/add` route 本身无授权或 feature gate。另确认 Views、HA、Federation、HAWQ 多处菜单条件不等于 route guard，NNF/RBF 对零个 ZooKeeper Server 或 JournalNode 的空集合检查会错误通过 | 在安装、Views、权限、NNHA、RM/RA、Federation/HAWQ 模块分别写清 UI gate、route gate、点击检查和直接 URL 边界；修正 Admin View 浏览器 URL 为 `#/adminView`，区分 route pattern `/adminView`；登记 Add Host 深链缺口和 Federation 空集合缺陷 | 提取器记录的是嵌套 fragment，不是拼接后的全部最终 URL；动态 redirect、浏览器 history/hash 行为及服务端深链授权仍需真实浏览器和 Server 验证 |
| 2. Template action 与 JavaScript 行为反查 | 299 个不同 Handlebars action 名、587 个模板出现位置；再人工反查 view click、controller 方法、observer、timer、动态 action 和 route lifecycle | Host Re-upgrade 虽有 UI/controller 静态路径，但请求名未注册、状态已从 Server 枚举移除且 payload 仍硬编码旧 stack；`App.ajax.send` 对未注册名只 warning 并返回 `null`。另发现 HDPWIN 自动 bootstrap 的整块隐藏 UI、Add Host `Skip host checks` 的无 Cancel 告知框、Step 3 只确认通用 warnings，以及 Kerberos restart observer 等模板 action 无法表达的控制流 | 将 Re-upgrade 定为 `STATIC_ONLY / LEGACY_BROKEN / UNREGISTERED`，明确不能作为 React API；记录条件坏 Rollback 按钮；把 HDPWIN PowerShell、Agent user 隐藏校验缺陷、host-check 分类、Kerberos pending/延迟 restart、Views singleton 和向导关闭/恢复写成 JavaScript-only 语义 | action 提取只覆盖静态 Handlebars action，不覆盖运行时注入的 action 名、第三方 View 内容、DOM plugin callback 和未来 stack extension；这些边界仍需浏览器 instrumentation |
| 3. 网络与 API 反查 | 288 个纳入的非 Metrics 命名 AJAX 定义、95 个 Metrics 排除定义；394 个 `App.ajax.send` 调用点为 364 registered、27 dynamic、3 unregistered。27 个动态调用点中 23 个闭集、4 个开放边界，共 45 个唯一候选名且全部已注册。注册表的 HTTP method 实际全部静态，提取为 0 个 dynamic method；另有 3 个纳入、1 个 Metrics 排除定义由 `format()` 提供动态 URL。另审计 19 个 direct HTTP、56 个浏览器网络候选、5 种 client-config 下载 scope，以及 2 个实时 transport、11 个 destination、4 个 lifecycle contract | 旧提取器因负向前瞻回溯把 65 个固定 method 误标为 `DYNAMIC`，并漏掉 quoted `'type'`，致 `service.item.smoke` 错报 GET；同时没有呈现三条运行时 URL。浏览器目录漏掉 JWT/preferred-path 导航、`window.location.reload()` 和全局 `location.reload()` 两种整页重载、首页链接、新 UI 与 View icon。实时审计还发现 Alert Group 删除推送的服务端字段删错、task 终态/清理不对称及心跳/认证边界 | 改为解析 `format()` 返回对象的顶层 `type`/`url`；method 与动态 URL 分栏，冻结每个 AJAX 源对象 hash、旧 1000 ID 顺序 hash、模块候选内容 hash；补齐浏览器入口并明确排除构建静态资源；为 27 个动态调用点保留人工解析契约；为实时通道冻结 10 static + 1 dynamic destination、11 subscribe、1 addHandler、1 removeHandler、1 业务 unsubscribe及 142 个前后端源码/测试位置；规定权威接口核对必须联合五层网络证据 | 4 个开放动态 wrapper/model 边界可被未来数据扩展；模块候选索引不保证完备；caller-supplied URL、proxy/auth、真实 payload 编码、STOMP wire serialization、断线期间事件丢失和服务端最终授权只能靠运行态流量与故障注入确认 |
| 4. Permission、flag、状态与恢复反查 | 38 个权限名、147 个静态使用项，其中 130 个 `isAuthorized`、17 个 `havePermissions`；23 个 `App.supports` flag、58 个使用项；另人工索引 `App.stackVersionsAvailable`、`App.upgradeHistoryAvailable`、`App.enableDigitalClock` 三个 runtime gate。逐步复核安装、Kerberos、NNHA/JN、RM/RA、Federation/HAWQ 的 state、owner、persist、Retry、Skip 和 rollback | 逗号权限是 OR；upgrade 例外会污染整条 OR 表达式；Service Actions 的宽 OR 会暴露多个未逐项复查 RBAC 的动作。安装 Review 会删除全部 cluster 与已有 repository versions，GET/DELETE/VDF/queue 各有不同锁页、继续和无回滚分支；Step 9 Retry 仅对 `INSTALL FAILED` 可见。Kerberos resource/credential 链的 `.always()` 与阻断语义不一致，RBF 只重置 maintenance 为 `OFF` 的 Router | 在统一权限索引中拆开权限职责、宽 OR、upgrade 污染、runtime gate 和 Server 权威授权；为安装 Step 8 增加提交状态机和失败矩阵；在各向导模块记录精确 owner/persist、可达 Retry/Skip/Complete、Kerberos descriptor/credential 传播、Router maintenance 边界及已知卡死点 | 权限生成器只识别静态字符串；非事务删除和部分创建必须做故障注入；动态 helper 参数、stack metadata、服务端 privilege、跨用户 owner、刷新/Server restart、KDC session 和并发状态变化必须用角色矩阵及断点恢复 E2E 验证 |
| 5. Tests 与证据一致性反查 | `classic/test` 有 546 个 JS、500 个 `_test.js`。manifest 有 499 项：498 个 `_test` 引用、497 个唯一引用和 1 个初始化模块；`test/utils/config_test` 重复。52 个文件含 81 个 skip 标记，为 59 个 `describe.skip` + 22 个 `it.skip`。最终 `01` 至 `13` 模块含 1002 个唯一稳定 ID；手写模块表格行中有 282 次完整源码文件引用，规范化后为 125 个唯一路径，逐级大小写检查全部存在 | 磁盘有 3 个 `_test.js` 未被 manifest 载入：active 的非 Metrics `test/data/configs/wizards/secure_mapping_test.js`、自身 `describe.skip` 的 `test/mappers/configs/stack_config_properties_mapper_test.js`、Metrics 排除的 `test/views/main/charts/heatmap/heatmap_rack_test.js`。新增的 HDPWIN 自动 bootstrap 与全局 Version Definition 清理链缺少足以证明真实 PowerShell、部分删除和恢复结果的运行态测试；全局 harness 又 stub `$.ajax`、`App.ajax.send`、updater 和 modal | 用源码与测试双向补证各模块，并把 skip、未载入文件和测试空白写成缺口而不是成功证据；生成器保证功能 ID 索引一致，validator 校验 generated catalog、源码行号、完整源码路径精确大小写、测试 manifest、Markdown 表列数和链接 | 本轮未安装依赖或运行 Karma，不能声称旧 suite 通过；stubbed unit tests、未载入/skip 内容及真实 Server/Agent/Windows/stack/KDC/browser 行为仍是运行态验收范围 |

## 冻结结论

五轮审计后，手写模块冻结为 1002 个全局唯一稳定功能 ID，其中新增 `INST-MODE-011` 固化 HDPWIN PowerShell 自动 bootstrap，新增 `INST-8-009` 固化全局 Version Definition/Repository Version 清理链。网络基线冻结为 288 个非 Metrics AJAX 定义和 394 个调用点；权威核对必须联合 [AJAX definitions](generated/ajax-endpoints.md)、[AJAX calls](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。`generated/api-by-module/` 只用于启发式找候选，不能代替上述联合清单。

稳定 ID 的兼容契约不仅检查当前唯一性：validator 会过滤上述两个允许新增 ID，再对原 1000 个 ID 的有序 JSON 数组计算 SHA-256 `21699bfe0be07648e5124cfd640d8593a83d840ca19de455c40712b74f1f1a23`。旧 ID 被改名、删除或重排都会失败。每个 AJAX definition 同时冻结其 `ajax.js` 源对象 hash，每个启发式模块候选页冻结由 name/method/endpoint/inputKeys/callers 构成的内容 hash，防止同数量的陈旧生成物通过。

三个 `UNREGISTERED` 调用不能成为 React endpoint：Host Re-upgrade 是旧状态上的遗留坏分支；两个 NNHA rollback 请求位于未接线 controller。两种实时 transport 都失败后没有统一 REST polling fallback。上述负面事实同样属于兼容基线，React 对比时必须标为已知旧缺陷、有意修复或运行态缺口，不能默认为已有能力。

实时 contract 共引用 142 个前后端源码/测试位置，规范化后为 131 个唯一 `source:line`。新增证据覆盖 `/api/*` security filter、允许任意 origin pattern、Alert Group delete listener、task client/server 终态集合以及 subscribe/unsubscribe/disconnect registry 清理。这些静态位置证明消费链存在，不证明代理、认证、心跳协商、消息序列化或断线恢复在真实部署中成功。

## 机器冻结计数

下列 JSON 由 validator 与当前生成目录、手写功能表、经典测试 manifest 和实时 contract 逐字段比对。字段缺失、多余或数值漂移都会使校验失败。

```json
{
  "featureIds": 1002,
  "routeRecords": 160,
  "routeSourceFiles": 21,
  "distinctRouteFragments": 64,
  "templateActionNames": 299,
  "templateActionOccurrences": 587,
  "ajaxDefinitions": 288,
  "excludedMetricsDefinitions": 95,
  "ajaxCalls": 394,
  "registeredAjaxCalls": 364,
  "dynamicAjaxCalls": 27,
  "unregisteredAjaxCalls": 3,
  "resolvedClosedDynamicCalls": 23,
  "resolvedOpenDynamicCalls": 4,
  "uniqueDynamicCandidates": 45,
  "directHttpCalls": 19,
  "browserNetworkEntrypoints": 56,
  "clientConfigDownloadScopes": 5,
  "permissions": 38,
  "permissionUses": 147,
  "isAuthorizedUses": 130,
  "havePermissionsUses": 17,
  "featureFlags": 23,
  "featureFlagUses": 58,
  "runtimeGates": 3,
  "realtimeTransports": 2,
  "realtimeDestinations": 11,
  "realtimeStaticDestinations": 10,
  "realtimeDynamicDestinations": 1,
  "realtimeLifecycleContracts": 4,
  "realtimeSubscribeSites": 11,
  "realtimeAddHandlerSites": 1,
  "realtimeRemoveHandlerSites": 1,
  "realtimeUnsubscribeSites": 1,
  "realtimeLocationOccurrences": 142,
  "realtimeUniqueLocations": 131,
  "sourceReferenceOccurrences": 282,
  "sourceReferenceUniquePaths": 125,
  "testJsFiles": 546,
  "diskTestModules": 500,
  "manifestEntries": 499,
  "manifestTestReferences": 498,
  "uniqueManifestTestReferences": 497,
  "manifestInitializationModules": 1,
  "duplicateManifestReferences": 1,
  "diskTestsNotLoaded": 3,
  "manifestTestsMissingOnDisk": 0,
  "skipFiles": 52,
  "describeSkipMarkers": 59,
  "itSkipMarkers": 22,
  "skipMarkers": 81
}
```

## 复核命令

以下命令应在仓库根目录执行。生成器连续运行两次后的 `generated/` 哈希必须相同；validator 的 `warnings` 和 `errors` 必须都为空。

```bash
node docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
find docs/frontend-refactor/ember-baseline/generated -type f | sort | xargs shasum
node docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
find docs/frontend-refactor/ember-baseline/generated -type f | sort | xargs shasum
node docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs
node --check docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
node --check docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs
rg -n '[ \t]+$' docs/frontend-refactor/ember-baseline
git diff --check
```

旧版 Karma、真实集群和外部系统验证不属于上述静态命令。React 宣布模块 `COVERED` 前，仍必须按 [React gap matrix](14-react-gap-matrix.md) 的复杂场景矩阵执行安装模式、Kerberos 四模式、HA/Federation、upgrade、权限角色、刷新恢复和故障注入测试。
