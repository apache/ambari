<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to you under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Ambari Prometheus 与 VictoriaMetrics 监控架构

[English](ambari-prometheus-victoriametrics-architecture.md) | 简体中文

## 概述

Ambari Agent 内置了一个小型 Prometheus exporter，用于采集 Linux 主机指标，并通过有界、
基于路由的代理访问同一主机上受管组件公开的指标。Ambari Server 仍然是拓扑、Stack
元数据、生效配置、协议、端口和 Kerberos 设置的事实来源。

该实现将配置分发与指标值分离：

```text
控制平面

  Stack telemetry.json + JMX profiles
                  |
                  v
  Ambari Server assignment compiler（拓扑 + 生效配置）
                  |
       STOMP telemetry-v1 assignment
                  |
                  v
  Agent last-known-good configuration cache

数据平面

  Ambari HTTP SD --> vmagent --> Agent /metrics ----------------> Linux collectors
                           |
                           +--> /metrics/components/{routeId}
                                      |
                                      +--> native /prom pass-through
                                      +--> /jmx typed conversion
                           |
                           +-- remote write --> VictoriaMetrics

  React monitoring UI --> Ambari query proxy --> VictoriaMetrics datasource
```

Agent 只持久化当前 assignment 和按内容寻址的 JMX profiles。每次 vmagent 抓取时，
Agent 都会获取并校验对应的组件响应。Agent 不缓存之前抓取的指标值，不跨抓取聚合样本，
也不在 heartbeat payload 中发送样本。

## Agent Exporter

exporter 在 `ambari-agent.ini` 中配置：

```ini
[prometheus]
enabled=true
bind_address=0.0.0.0
port=9101
```

它在 Linux 上默认启用，也可以显式禁用。端口格式错误或超出范围时回退到 `9101`；
绑定失败只影响 exporter 线程，不会终止 Ambari Agent。

HTTP 接口如下：

| 路径 | 用途 |
| --- | --- |
| `/metrics` | Linux 主机指标和 exporter 自监控指标 |
| `/metrics/components/{routeId}` | 一个已分配的组件目标 |
| `/-/healthy` | exporter 进程健康检查 |

主机采集器读取 Linux `/proc` 和 `statvfs` 数据。有界的主机指标契约覆盖：

* CPU 时间和逻辑 CPU 数量；
* 内存、swap、load、uptime、boot time、进程状态和进程线程总数；
* 文件系统容量和 inode、磁盘操作和吞吐量，以及网络包、吞吐量、错误和丢包；
* context switch、interrupt、文件描述符分配、可用 entropy、OOM kill、conntrack
  使用量，以及按固定状态集合统计的 TCP 连接。

运行中的内核没有提供某个可选文件时，只省略对应指标。聚合指标
`ambari_agent_process_threads` 会汇总可读取的 `/proc/<pid>/stat` 记录中的第 20 个字段，
但不会公开进程级身份。采集器不会引入无界的 process、command-line、socket、user 或
container 标签。各采集器的失败相互隔离，一个数据源不可用不会抑制其他主机指标。
Linux `/proc` 没有提供可靠的跨设备数据源，因此通用设备错误推断不在本契约范围内。

每条组件路由都有独立的上游请求和失败结果。路由 ID 由 Ambari Server 生成，不能用来
选择任意 URL。未知路由或嵌套路由路径返回 404。

## 组件路由

首个 BIGTOP 3.2.0 Stack 集成覆盖 7 个组件：

| 服务组件 | 上游 | Agent 处理方式 | 契约证据 |
| --- | --- | --- | --- |
| HDFS NameNode | `/prom` | 校验后透传 | 真实 `/prom` 响应样本 |
| HDFS DataNode | `/prom` | 校验后透传 | 真实 `/prom` 响应样本 |
| YARN ResourceManager | `/prom` | 校验后透传 | 真实 active/standby `/prom` 响应样本及 Hadoop 3.3.6 源码契约 |
| YARN NodeManager | `/jmx` | 类型化 JMX 转换 | Hadoop 3.3 profile 和转换 fixture |
| HBase Master | `/jmx` | 类型化 JMX 转换 | HBase 2.4 profile 和转换 fixture |
| HBase RegionServer | `/jmx` | 类型化 JMX 转换 | HBase 2.4 profile 和转换 fixture |
| HiveServer2 | `/jmx` | 类型化 JMX 转换 | Hive 3.1 profile 和转换 fixture |

`hadoop.prometheus.endpoint.enabled=true` 用于启用 Hadoop 原生端点。Hadoop 3.3 Stack
契约有意不让 NodeManager 使用该端点。进程内 Timeline Collector 可能先于固定的
NodeManager Web UI 注册全局 Prometheus sink，导致稳定的 Web UI `/prom` 端点为空。
因此 assignment 使用稳定的 NodeManager Web UI `/jmx` 端点。

原生输出保留组件自己的指标名和标签。JMX profiles 会选择明确的 ObjectName 和数值属性，
应用类型化名称及单位换算，强制 counter 使用 `_total` 后缀，并限制输出 series 数量。
除非 profile 显式声明了有界标签来源，否则动态 ObjectName 不会作为标签导出。

验证证据有明确边界。NameNode、DataNode 以及 active 和 standby ResourceManager 的原生
指标名称和类型已于 2026-08-28 使用真实 `/prom` 响应进行核验。ResourceManager 类型还
对照了 Hadoop 3.3.6 `PrometheusMetricsSink` 命名算法和指标源定义。其余 4 条 JMX 路由
通过已捕获的 JSON fixtures 和 profile 转换测试进行检查。原始响应样本只作为本地验证
产物，不随仓库发布。

## Stack 契约

服务通过 `telemetry.json` 选择接入。该描述文件把组件映射到上游格式、路径、生效配置引用、
可选 HA 属性前缀、超时和响应上限，以及可选 JMX profile。它通过现有 Stack service
继承链加载。

`metrics.json` 不再承担时序监控契约，只保留管理流程所需的少量 direct-JMX 控制平面属性：

| 组件 | 保留属性 |
| --- | --- |
| NameNode | `HAState`、`ClusterId`、`Safemode`、`LastCheckpointTime`、`JournalTransactionInfo` |
| JournalNode | `JournalsStatus` |
| HBase Master | `IsActiveMaster`、`liveRegionServers`、`deadRegionServers` |

这些时点值用于 HA role、safe mode、cluster identity、checkpoint 和 active-master 决策。
它们不会写入 VictoriaMetrics，也不会被监控 Dashboard 查询。

`telemetry.json` 及其类型化 JMX profiles 是监控数据平面契约。它们描述抓取传输和
Prometheus 转换，不把采集过程与某个存储实现耦合。

Server 在分配前校验描述文件和 profiles。它解析主机的生效配置，使用组件主机名作为目标，
只提取已配置端口，依据 Stack 策略选择 HTTP 或 HTTPS，解析带 HA 后缀的属性，并在
Kerberos principal 中替换 `_HOST`。

## Assignment 生命周期

Ambari Server 为每台 Agent 主机编译一份按稳定 route ID 排序的完整 assignment。JMX
profiles 规范化后使用 SHA-256 寻址。完整事件使用现有 Agent data-holder hash，并通过
`telemetry-v1` STOMP capability 传递。

cluster 配置变化、组件安装或卸载、主机注册或移除，以及 Stack upgrade 完成时，Server
都会刷新 assignments。注册时立即比较 hash。Agent 还会每 5 分钟校准当前 hash，使遗漏
事件可以在不重启 Server 或 Agent 的情况下恢复。

Agent 会在激活前校验完整的候选 bundle。Profile 文件和 assignment envelope 使用临时
文件、`fsync` 和原子 rename 写入；同一把 update lock 会串行处理 STOMP 更新和定期校准。
无效或不完整的更新不会替换上一份内存和磁盘中的有效 assignment。启动时只加载被引用且
digest 匹配的 profiles。

## 抓取处理

原生 Prometheus 响应必须是非空 UTF-8 文本，至少包含一个样本，且不能有格式错误的样本行。
Agent 不会重命名或合并原生 metric families。

JMX 响应必须包含 JSON `beans` 数组。ObjectName 被解析为 domain 和带引号的 key
properties，profiles 对这些结构化字段进行匹配。只渲染有限数值或 Prometheus 支持的
特殊值。缺失属性只会省略单个样本。没有 profile 匹配、输出 series 重复、JSON 无效或
series 数量过多都会导致该路由失败。

每条路由都有 nonblocking concurrency semaphore、请求超时、响应大小限制，并拒绝重定向。
超过并发限制时返回 503；上游、认证、校验或转换失败返回非 2xx，使 Prometheus 将目标记录
为 down。其他组件路由和主机 exporter 仍可继续工作。

## 安全

exporter 没有应用层认证，并且在 Linux 上默认启用。部署时必须绑定到监控网络接口，或使用
主机防火墙策略限制访问。Ambari Server service discovery 需要
`CLUSTER.VIEW_METRICS` 权限。

Assignments 只接受不含 credential、query string 或 fragment 的 HTTP/HTTPS URL。
在 Kerberos cluster 中，它们包含本地 keytab 路径和解析后的 principal，但绝不包含
keytab 内容或密码。SPNEGO 请求复用现有 Agent Kerberos cache helper，禁用重定向，
启用 HTTP error，限制响应大小，并验证 TLS。路由没有指定独立 CA 路径时，会继承 Agent
配置的 CA 文件。

## 服务发现

vmagent 从以下 Ambari Prometheus HTTP service-discovery 端点获取目标：

```text
GET /api/v1/clusters/{cluster}/prometheus_targets
```

该端点为每个主机 exporter 和组件路由返回一个 target group。组件 group 带有权威的
`cluster`、`host`、`service`、`component` 和 `ambari_target="component"` 标签。
主机 group 带有 `cluster`、`host` 和 `ambari_target="host"`，有意不包含 service 或
component identity。`__metrics_path__` 选择 `/metrics` 或独立的组件路由。

受管 vmagent metric allowlist 只在目标 identity 和 metric family 同时匹配
`host;;ambari_agent_.*` 时保留主机样本。组件 families 使用各组件独立的 allowlist 条目。
这可以防止组件原生输出或上游同名标签被误认为主机 telemetry。内置 Linux Dashboard
查询因此会同时使用 `cluster="${cluster}"` 和 `ambari_target="host"` 限定每个主机选择器。

渲染后的 discovery 配置按 source assignment hashes 缓存，包含 ETag，并使用较短的
private revalidation window。这是配置缓存，不是指标值缓存。Ambari Server 中配置的
`prometheus.agent.metrics.port` 必须与每台主机上的 Agent exporter 端口一致。

受管 vmagent 抓取模板如下：

```yaml
scrape_configs:
  - job_name: ambari-prometheus-targets
    honor_labels: false
    http_sd_configs:
      - url: https://ambari.example.com:8443/api/v1/clusters/cluster1/prometheus_targets
        basic_auth:
          username: prometheus
          password_file: /etc/prometheus/ambari-password
```

`honor_labels: false` 保证上游 exporter 输出冲突标签时，HTTP SD 标签优先。vmagent 的
`external_labels` 值 `ambari_cluster` 是独立元数据，不是 Dashboard 隔离键；查询使用
权威的 `cluster` 标签。HTTP SD identity 只需要 `CLUSTER.VIEW_METRICS`。TLS 和 Ambari
认证遵循 vmagent 与 Prometheus 兼容的 `http_sd_config` 行为。

vmagent 将样本 remote-write 到单机 VictoriaMetrics 端点；分布式部署则写入 `vminsert`。
可选的 `vmauth` 端点可以作为写入和查询的前置入口。vmagent 本地 remote-write queue 是
抓取后的传输缓冲，不会改变 Agent 不缓存指标值的契约。

## 存储拓扑

`VICTORIAMETRICS` Stack 服务支持两种存储拓扑，Agent、HTTP SD 和 vmagent 采集链路在
两种模式下保持不变：

| 部署模式 | 组件 | 数据链路 |
| --- | --- | --- |
| `single` | `VICTORIAMETRICS_SERVER` | 单个进程同时提供写入、存储和 Prometheus-compatible 查询 |
| `cluster` | `VMINSERT`、`VMSTORAGE` 和 `VMSELECT` | `VMINSERT` 分发写入，`VMSTORAGE` 持久化样本，`VMSELECT` 提供 tenant-scoped 查询 |

`VMAGENT` 在两种模式下都负责发现、抓取、relabel 和 remote write。`VMAUTH` 是可选组件，
在写入和查询链路前提供稳定的认证端点。如果没有显式 URL override，Stack scripts 会根据
已分配的组件拓扑和配置的 `tenant_id` 推导写入与查询 URL。

VictoriaMetrics `replication_factor` 控制在 VMSTORAGE 节点间写入的数据副本数量。独立的
vmagent scrape replication factor 控制每个目标由多少个 vmagent 成员抓取；受 Agent
路由并发契约限制，其最大值为 2。scrape replication 为 2 时，受管存储使用 scrape
interval 作为 deduplication interval。

## 查询与 Dashboard 链路

Ambari Server 保存 cluster-scoped datasource 定义，并提供有界的 Prometheus-compatible
query proxy。React 客户端调用 Ambari 的 `/metrics/{datasourceId}/api/v1/query` 和
`query_range` 端点；浏览器客户端不需要直接访问 VictoriaMetrics 网络或 credential。
受管 VictoriaMetrics 服务在未启用 VMAUTH 认证时，Ambari 会提供默认的 VictoriaMetrics
datasource。其他认证拓扑可以使用显式配置的 datasource。

React 监控 UI 渲染内置和用户创建的 Dashboards。11 个内置 Dashboard 覆盖 HDFS、
NameNode、DataNode、HBase Master、HBase RegionServer、HiveServer2、NodeManager、
ResourceManager、ResourceManager 主机指标，以及两个 Linux 主机工作流：

| Dashboard | 范围 | 主机选择方式 |
| --- | --- | --- |
| Linux Fleet Overview | 全 cluster 的 inventory、capacity、utilization 和 Top-N 主机信号 | 当前 cluster 中所有 HTTP SD 主机目标 |
| Linux Host Detail | CPU、memory、load、filesystem、disk、network、process 和 kernel 明细 | 文本变量 `host`，默认 `.*`，以 `host=~"${host}"` 应用 |

两个 Linux Dashboard 都位于 React Dashboard metrics 页面。查询直接使用
`ambari_agent_*` families，并始终带有权威的
`cluster="${cluster}",ambari_target="host"` 目标范围。不提供旧 Categraf、Telegraf 或
AMS aliases。

4 个源模板 `categraf-processes.json`、`host_generic_categraf.json`、
`linux_by_categraf.json` 和 `linux_by_telegraf_overview.json` 只是语义迁移输入，不作为
兼容 Dashboard 打包。它们有价值的主机监控信号被整合到 fleet 和 host-detail 工作流中；
重复布局、旧指标名以及 Categraf/Telegraf 假设均不保留。

内置 panels 只对物理维度一致的目标分组。Byte gauge 使用 `bytesIEC`，byte rate 使用
`bytesSecIEC`，duration 使用 `seconds`，0..1 ratio 使用 `percentUnit`，event/request
rate 使用 `cps` 或 `reqps`。以 MiB 为值的 Hadoop ResourceManager 指标会先换算成 bytes
再显示。静态 Dashboard 契约会校验查询数量、指标类型、标签范围、动态 rate window、
显示单位和已知源缩放。HBase cluster-level Master panels 通过类型化
`hbase_master_active` gauge 做 join，防止 standby Master 状态进入 cluster 聚合；process
和 JVM panels 则有意按主机显示。

每次 panel 查询前，React 客户端都会用当前 `AppContext.clusterName` 覆盖 Dashboard
payload 中的任何 `cluster` 变量，并将其转义为 Prometheus label value。客户端还计算
`$__rate_interval = max(4 * query_step, 120s)`。120 秒下限是受管 vmagent 默认 30 秒
scrape interval 的 4 倍。内置 counter 查询使用带 `[$__rate_interval]` 的 `rate` 或
`increase`，gauge 则直接查询。`cluster` 和 `__rate_interval` 是保留变量，不会作为可编辑
Dashboard 变量显示。共享 panel 时也会应用相同的 cluster binding。

权限由 Ambari Server 强制执行，而不只依赖 React route guards：

| 操作 | 所需权限 |
| --- | --- |
| HTTP service discovery、读取 datasource 和 Dashboard，以及查询指标 | `CLUSTER.VIEW_METRICS` |
| 创建、更新或删除 Dashboard | `CLUSTER.MANAGE_USER_PERSISTED_DATA` |
| 创建、更新、删除 datasource 或选择默认 datasource | `AMBARI.MANAGE_SETTINGS` |

查询边界将 query string 限制为 65,536 个字符，range query 限制为 11,000 个数据点，
batch 限制为 64 条查询。Datasource response 上限为 16 MiB，代理 request body 上限为
8 MiB，配置的 request timeout 最大为 60 秒。请求不会跟随重定向。

## 运行指标

主机 `/metrics` 端点报告 exporter 状态，包括已配置路由、按路由和结果统计的请求、最后
一次抓取耗时、最后一次成功抓取时间、JMX 转换失败、配置重载状态，以及主机采集器健康状态
和耗时。

这些指标用于区分 Agent exporter 故障和上游组件故障。Prometheus `up` 仍然是每个独立
发现目标的权威端到端抓取结果。

## 删除与保留边界

运行时监控链路删除以下旧子系统：

* Ambari Metrics System collector/monitor 服务、temporal property providers、sinks、
  service commands、Stack dependencies 及其测试和 fixtures；
* Ganglia 服务、集成、配置和测试；
* AMS-backed 旧 `Widget`、`WidgetLayout`、active-widget REST、provider、persistence、
  descriptor、authorization 和 test model。

React `Board`、`Dashboard` 和 `Datasource` 模型替代已删除的 Dashboard widget model。
新的 `ambari-metrics` distribution module 用于打包 VictoriaMetrics，不会恢复旧 AMS runtime。

以下名称相似的功能不在删除边界内，仍然受支持：

* direct JMX/REST providers、`MetricsRetrievalService`、JMX alerts，以及前文说明的最小
  `metrics.json` 控制平面值；
* Hadoop YARN Timeline Service，它是 Hadoop 组件，不是 AMS Timeline Metrics collector；
* service Theme 配置 widgets，它们是 form controls，不是监控 Dashboards。

`ambari-web/classic` 不在本次迁移范围内，也不属于受支持的 React 监控链路。

## 兼容性与限制

该实现不为旧 Categraf 或 Telegraf 指标名提供 aliases。Ambari 提供自己的 React
Dashboards，不提供 Grafana Dashboards 或 recording rules。主机采集器有意优先采集稳定、
低基数的操作系统信号，而不是与每个第三方 collector plugin 完全等价。只有完成对应版本的
输出和认证行为验证后，HBase 2.6+ 原生 `/prometheus` 才能替代 JMX。初始主机采集器依赖
Linux `/proc`，因此 Windows 默认禁用 exporter。
