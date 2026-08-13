# 后台操作与 Dashboard 非 Metrics 功能

## 后台操作

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| BG-001 | 顶部 Background Operations 展示 request 列表、状态、进度、context、开始/结束时间 | 轮询只在主界面运行；退出停止 | `background_operations.get_most_recent` 及同组请求 | `app/controllers/global/background_operations_controller.js` |
| BG-002 | 展开 request 查看 stages/tasks，按 host、role、command 和状态展示 | 大响应使用字段过滤和 minimal response | `background_operations.get_by_request` | `app/controllers/global/background_operations_controller.js`，`app/templates/common/host_progress_popup.hbs` |
| BG-003 | 打开单 task 查看 stdout、stderr 及对应 output/error log 路径，并可复制或在新窗口打开已加载文本 | task 尚未完成时继续 polling；通用 UI 不展示 raw `structured_out` 或 exit code，只有 HDFS Rebalance 会把 structured fields 转成专用进度数据 | `background_operations.get_by_task` | `app/utils/host_progress_popup.js#createTask`、`app/utils/host_progress_popup.js#_handleRebalanceHDFS`、`app/templates/common/host_progress_popup.hbs` |
| BG-004 | 从服务、主机、安装和升级动作打开对应 request progress，而不是另建进度模型 | request ID 由 202 响应返回；同步响应不进入 progress | `common.request.polling`、`background_operations.get_by_request` | `app/utils/host_progress_popup.js`，`app/controllers/global/background_operations_controller.js` |
| BG-005 | request/task 失败时显示失败主机与日志，允许相关业务流程触发 retry | retry 是否可用由具体业务 controller 决定 | 业务专用 retry 请求 | 各 wizard/service/upgrade controller |
| BG-006 | 支持 request schedule：批量启动/停止/重启可立即执行或按时间调度 | 创建、查询 pending、删除 schedule；与正在运行的 wizard/upgrade 互斥 | `common.batch.request_schedules`、`request_schedule.get.pending`、`common.delete.request_schedule` | `app/utils/batch_scheduled_requests.js` |
| BG-007 | polling 避免同类请求重叠，页面退出或 controller disable 时停止 | 网络失败和 abort 不应产生重复 modal；具体 poller 有独立周期 | 多个 GET/status 请求 | `app/utils/polling.js`，`app/utils/updater.js`，`app/controllers/global/update_controller.js` |
| BG-008 | 原生 WebSocket/STOMP 推送 host-component、alert summary、topology、config、service、host、alert definition/group、upgrade、background request及动态 task detail，再由 mapper/controller更新 Ember Data；初始原生连接失败改用 SockJS eventsource/xhr polling系列 transport | 两种 transport 都失败时没有统一 REST polling替代：部分已有 updater或页面 REST snapshot后来可收敛，另一些状态直到手工刷新/重新进入才恢复。已建立连接断开后 6 秒重连并浅快照恢复订阅，但无 event replay；初始 SockJS失败不继续 transport retry。完整11个 destination及故障边界见实时契约，`CONDITIONAL` | `/api/stomp/v1/websocket`、SockJS `/api/stomp/v1`，非 REST资源 API | `app/utils/stomp_client.js`、`app/mappers/socket`、`app/controllers/global/update_controller.js`、`generated/realtime-channels.md` |
| BG-009 | 对可中止的运行中/未知状态 request 显示 Abort，确认后将 request status 更新为 `ABORTED` 并附加 abort reason | 需要 `SERVICE.START_STOP`；提交时先禁用 Abort，失败时按当前状态恢复按钮并调用全局错误处理，成功时显示确认结果 | `background_operations.abort_request` | `app/utils/host_progress_popup.js#isAbortableByStatus`、`app/utils/host_progress_popup.js#abortRequest`、`app/utils/ajax/ajax.js` |

## Dashboard 非 Metrics

Metrics tab、Heatmap、Horizon Chart 和指标 Widget 全部为 `OUT_OF_SCOPE`。保留以下非 Metrics 能力：

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| DASH-001 | Dashboard 默认 route 会转到 Metrics 页面 | 页面本身因范围要求排除；这里只记录默认导航事实 | Metrics 请求排除 | `app/routes/main.js` |
| DASH-002 | Config History 列出 service config versions，包括 service、version、author、创建时间、note、group、current 和 cluster-compatible 状态 | 支持分页、排序、关键词/服务/版本筛选；数据通过直接 HttpClient 加载 | `DIRECT:main/dashboard/config_history_controller.js#getUrl` | `app/controllers/main/dashboard/config_history_controller.js`，`app/templates/main/dashboard/config_history.hbs` |
| DASH-003 | 从配置历史记录跳到对应 Service Configs，并预选该 config version | 目标服务必须仍存在；退出 service configs 时仍执行未保存检查 | 无额外请求，复用配置版本加载 | `app/routes/main.js` |
| DASH-004 | Config History 查看某版本的 hosts/config group 关联和版本说明 | 数据包含 `hosts`、`group_id`、`group_name` | 同 DASH-002 | `app/models/configs/service_config_version.js`，config history view/controller |
| DASH-005 | Dashboard service/host/alert health 汇总与导航来自 Dashboard Widget | Widget 布局和内容属于本文明确排除的 Metrics/Widget 能力，标记为 `OUT_OF_SCOPE`；非 Metrics Dashboard shell 只有 tabs/outlet 与 Config History，不能据此要求 React 实现独立 health summary | 排除 | `app/templates/main/dashboard.hbs`、`app/routes/main.js`、dashboard widget templates |

## 全局数据刷新

`global/update_controller.js` 维护独立刷新通道：

- Hosts 和 host component state。
- Services、service component state 和 stale config。
- Alert definitions、instances、summary、groups 和 notifications。
- Upgrade state、background requests 和 cluster topology。
- Metrics 刷新通道明确排除。

这些 REST updater 与实时通道是部分重叠而非统一 fallback；逐 destination 的 snapshot/reconciliation 语义见 [generated/realtime-channels.md](generated/realtime-channels.md)。[generated/api-by-module/background-common.md](generated/api-by-module/background-common.md) 仅是宽正则候选索引，不能代表本模块接口全集；权威网络核对必须联合 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。
