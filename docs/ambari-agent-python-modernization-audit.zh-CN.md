<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Ambari Agent Python 现代化审计

## 1. 文档状态

- 状态：第 4 至第 12 节的核心 Agent、Server、打包项和 BIGTOP service Python 源码门禁已关闭。源码核对已覆盖产品代码、调用方、旧源码/打包引用、依赖与许可证闭包、失败路径和 focused test；统一并行验证、最终完整构建部署和三遍复查仍按第 13 节执行记录推进。
- 审计日期：2026-09-03。
- 基线：`apache/trunk`，提交 `821de739a11b`；该提交已是当前分支祖先并包含 trunk 的 JDK 17/Java Home 修复。
- 审计 worktree：`/jialiangc/bigdata/prjs/ambari-agent-python-audit`。
- 审计分支：`AMBARI-26643`。
- 范围：Linux 下的 Ambari Agent、共享 Python 运行库、resource management、直接相关的 Server Python utilities、BIGTOP 3.2.0 下全部 16 个 service Python、对应测试基础设施、直接相关的 Agent Simulator，以及 Windows 支持的完整删除。
- 非范围：Ember、Server Java 的一般性重构。BIGTOP 组件 JDK 选择属于本次运行兼容性验收范围：Hadoop/YARN 使用 Java 17，Hive 3.1 在 Java 17 主机上通过显式 `cluster-env/java_home_overrides` 使用预装 Java 8，并由服务模板条件化 GC 参数。
- 核心 Agent、Server、打包实现和 BIGTOP service 调用层已按本文边界补齐，Windows 支持也已作为独立提交删除。第一次完整编译和增量集群验证是此前阶段的真实执行记录，不代表本轮源码变更已通过统一全量和最终验证；实际执行结果统一记录在第 13 节末尾。

当前已实施且正在按顺序验证的内容：

- Linux 最低运行基线统一为 Python >= 3.9.2；Rocky 8 显式安装并选择 AppStream `python39`/`/usr/bin/python3.9`，Rocky 9 使用系统 Python 3.9；Agent 和 Server 启动入口都拒绝更低版本。
- 建立 Common、Agent、Server 三层依赖输入与 hash lock。Agent 继承 Common 并增加调度/控制通道依赖，Server 继承 Common 并增加 Server CLI 格式解析依赖；构建前清理旧依赖目录，主 lock 只接受带 hash 的 CPython 3.9 `manylinux2014_x86_64` binary wheel，stomp.py 的纯 Python 硬依赖 `docopt` 由独立 sdist lock 安装到同一 Ambari 私有 lib。
- `simplejson`、`mock`、`pbkdf2` 分别迁移到 `json`、`unittest.mock`、`hashlib.pbkdf2_hmac`；pyaes 迁移到官方 cryptography，保持 AES-CBC v1 wire compatibility，并通过 Agent capability 协商增加 AES-GCM v2。
- APScheduler、Jinja2、stomp.py/WebSocket 迁移到锁定的官方包，Ambari 只保留 scheduler、template 和 connection facade；STOMP 由官方 `connect()` 启动 transport，并以 10 秒上限等待 `CONNECTED`；删除对应 vendored fork、ws4py、CoilMQ fixture 和错误的 runtime dependency。
- Server `Properties` facade 已改用官方 javaproperties；takeover 工具选择保留并安全迁移到 PyYAML `safe_load()` 和现代 XML API，并主动加入 Server 私有 lib 以便直接执行脚本时找到打包依赖。
- 删除孤儿 Python 2 `_posixsubprocess.so`、无实际 API 消费的 `python-kerberos` 声明，以及无入口的 `ambari-server-state`、旧 Agent Simulator 和 `export_ams_metrics.py`。
- Windows Agent/Server、公共运行库、打包入口、服务脚本和对应测试已从产品中删除，并以独立提交交付；Ember 仍明确排除。
- `pluggable_stack_definition` 和 `replaceBaseUrl` 因 Agent/Server/Admin/Web 仍有真实 Maven 消费者而保留，已迁移 Python 3.9 删除的 XML API并增加 focused tests。

## 2. 结论摘要

本轮没有确认无需任何前置条件即可直接利用的 P0 问题，但确认了四组应优先处理的 P1 问题：

1. Agent 默认不验证 Server 身份，网络中间人可把控制通道风险放大为 root 级脚本执行。
2. Command status 分片 ACK 回调存在 late-binding 闭包错误，可能反复发送已经成功上报的状态。
3. ActionQueue 的并行执行没有并发上限，取消和退出存在共享状态竞态。
4. 七个核心测试类没有继承 `TestCase`，共 50 个测试方法不会被 `unittest` 收集。

Python 依赖不能以一次性批量升级的方式处理。建议分为三类：

- 可以删除并改用标准库：`simplejson`、`mock`、`pbkdf2`。
- 直接采用官方库，但需要调整 Ambari 调用层：APScheduler、Jinja2。迁移后不再维护第三方源码 fork。
- 直接使用官方替代实现并迁移 Ambari 协议：用官方 stomp.py WebSocket connection 删除 ws4py/自建 adapter，用官方 `cryptography` 删除 pyaes，并分阶段迁移 AES-CBC 格式。
- 直接删除无消费者或错误进入运行包的遗留物：Python 2 `_posixsubprocess.so`、测试专用 CoilMQ 及其生产依赖声明、仅用于存在性检查的 `python-kerberos`，以及已经无法运行的旧 Agent Simulator。
- 逐步替换通用格式解析实现：评估用官方 `javaproperties` 取代 ActiveState recipe；PyYAML 必须显式锁定并使用 `safe_load`，不能继续依赖节点偶然安装的版本。

本 worktree 采用三层实施边界；核心控制链、运行基础和官方依赖迁移已补齐，BIGTOP service 调用层仍在逐项关闭。最终交付仍按第 8、9 节拆成可独立审阅的 topic commits/JIRA/PR；拆分是交付边界，不是延期实现。控制链并发、TLS 信任策略、依赖迁移、加密协议 v2 和各 service 修复不压成单个提交。

## 3. 当前关键执行链

Agent 的核心信任链如下：

```text
Ambari Server
  -> HTTPS 注册探测
  -> WebSocket + STOMP 控制通道
  -> CommandsEventListener
  -> ActionQueue
  -> FileCache 下载 Server 脚本和资源
  -> CustomServiceOrchestrator
  -> PythonExecutor
  -> root 身份执行组件脚本
```

因此，TLS 身份验证、消息确认、队列并发、缓存原子性和子进程生命周期不是相互独立的小问题。控制通道一旦接受伪造 Server，后续下载并以 root 执行脚本会使影响直接升级为节点接管。

## 4. 第一层：控制链、安全与正确性

本节“证据/风险/建议”保留首次审计时的基线，不能据此判断当前实现状态。当前产品代码、调用方、删除项、打包引用和 focused tests 的逐项结论以第 13 节为准；本轮已关闭 4.1 至 4.8 的源码缺口。

### 4.1 P1：默认不验证 Server 身份

证据：

- `ambari-agent/conf/unix/ambari-agent.ini:56` 默认设置 `ssl_verify_cert=0`。
- `ambari-agent/src/main/python/ambari_agent/NetUtil.py:70-81` 在注册探测时显式创建未验证的 SSL context。
- `ambari-agent/src/main/python/ambari_agent/security.py:70-77` 的单向 WSS 连接没有传入证书验证参数。
- `ambari-common/src/main/python/ambari_ws4py/client/__init__.py:89-99` 默认使用 `ssl.CERT_NONE`。
- `ambari-common/src/main/python/ambari_commons/inet_utils.py:252-276` 即使加载 CA，也显式设置 `check_hostname=False`。

风险：

- TLS 只提供加密，没有确认连接对象确实是配置的 Ambari Server。
- 同一网络、错误 DNS、透明代理或路由劫持场景下，攻击者可能伪造 Server。
- Agent 通常以 root 运行并执行 Server 下发的脚本，因此最终影响不是信息泄漏，而是 root 代码执行。

建议：

1. 将 Server 身份验证作为所有 HTTPS/WSS 模式的统一要求，不应只依赖双向 TLS 开关。
2. 使用显式 `SSLContext`，要求 `CERT_REQUIRED` 和 hostname verification。
3. 规定 bootstrap trust 来源，例如安装包内 CA、管理员预置 CA、受控 fingerprint 或首次注册审批。
4. 兼容期允许显式关闭验证，但必须打印高可见性告警；最终默认值必须改为验证证书。
5. TLS 策略修复和传输库替换应分开，避免同时改变信任策略、协议实现和重连行为。

Focused tests：

- 可信 CA 和匹配 hostname 成功。
- 不可信 CA、过期证书、hostname 不匹配失败。
- 单向 TLS 和双向 TLS 都验证 Server。
- 注册探测与 WSS 建连使用相同信任策略。
- 兼容开关仅在显式配置时生效并产生告警。

### 4.2 P1：Command status ACK 清理错误

证据：

- `ambari-agent/src/main/python/ambari_agent/CommandStatusDict.py:104-117` 在循环中使用 lambda 捕获 `splitted_report`。
- Python 闭包按引用捕获循环变量，所有成功回调最终会引用最后一个分片。
- `ambari-agent/src/main/python/ambari_agent/CommandStatusDict.py:118-136` 对单条超大报告还会先产生空分片，且无法真正限制该报告大小。

影响：

- 前几个分片成功 ACK 后不会清除对应状态。
- 同一批状态可能在后续心跳中重复发送。
- Server 侧可能出现重复处理、状态覆盖、日志膨胀和不必要的网络开销。

建议：

- 使用默认参数、`functools.partial` 或显式 callback factory 固定每个分片。
- 给每个 pending batch 建立明确的 correlation ID 到 report keys 映射。
- 对单条超过上限的报告定义明确策略，例如允许单条超限并记录告警，或将大字段外置；不要发送空分片。

Focused tests：

- 两个及以上分片乱序 ACK，每个 ACK 只清除自己的报告。
- 某一分片发送失败，其他成功分片仍可清理。
- ACK 重复到达具有幂等性。
- 单条报告超过 `MAX_REPORT_SIZE` 时不产生空消息。

### 4.3 P1：ActionQueue 无界并发与取消竞态

证据：

- `ambari-agent/src/main/python/ambari_agent/ActionQueue.py:63-64` 定义 `MAX_CONCURRENT_ACTIONS=5`，但没有用于限制 worker。
- `ambari-agent/src/main/python/ambari_agent/ActionQueue.py:153-176` 为每条 retryable command 创建 daemon thread。
- `ambari-agent/src/main/python/ambari_agent/ActionQueue.py:111-138` 通过替换整个 Queue 来取消排队命令。
- `taskIdsToCancel` 和全局 `cancelEvent` 在多个线程间无锁共享。
- `ambari-agent/src/main/python/ambari_agent/main.py:349-368` 只 join ActionQueue 主线程，没有跟踪或 join 其 daemon workers。

风险：

- 大量 retryable commands 可耗尽线程、文件描述符、内存和子进程资源。
- producer、consumer 和 cancel 同时操作时可能把命令留在旧 Queue，造成丢失或重复执行。
- 一个任务设置或清除全局 cancel event 会影响其他任务的 retry wait。
- Agent 重启时 daemon worker 可能被 `os._exit` 截断，组件脚本子进程可能成为孤儿进程。

建议：

1. 使用固定大小 worker pool 或 semaphore，默认并发上限从配置读取并设置安全上限。
2. 不再替换 Queue；维护加锁的 task registry 和明确的 `QUEUED/RUNNING/CANCELLING/FINISHED` 状态。
3. 每个 task 使用独立 cancellation token/event。
4. 统一跟踪 worker、PythonExecutor 和 process group。
5. 退出顺序应是停止接收命令、取消或等待运行任务、终止进程组、join workers、停止 scheduler 和 reporters。

Focused tests：

- 同时提交超过并发上限的任务，实际并发数不会突破上限。
- 排队阶段、启动临界点、运行阶段和 retry wait 阶段分别取消。
- 取消一个任务不会唤醒或取消其他任务。
- stop/restart 后没有存活 worker 或子进程。
- 用 Event/Barrier 替代 `time.sleep`，所有等待必须有超时。

### 4.4 P1：核心测试实际未被收集

以下测试类把继承关系注释掉了：

| 文件 | 未收集的 `test_*` 数量 |
| --- | ---: |
| `TestHostInfo.py` | 15 |
| `TestCommandStatusDict.py` | 5 |
| `TestClusterConfigurationCache.py` | 2 |
| `TestCustomServiceOrchestrator.py` | 9 |
| `TestMain.py` | 11 |
| `TestAgentStompResponses.py` | 3 |
| `TestNetUtil.py` | 5 |
| 合计 | 50 |

证据示例：

- `ambari-agent/src/test/python/ambari_agent/TestCommandStatusDict.py:30`
- `ambari-agent/src/test/python/ambari_agent/TestAgentStompResponses.py:47`
- `ambari-agent/src/test/python/ambari_agent/TestNetUtil.py:29`
- Maven 仍通过 `ambari-agent/pom.xml:328-343` 调用自定义 `unitTests.py`。

建议：

- 先恢复测试继承和收集，不要在同一个提交里同时重写业务逻辑。
- 为避免一次暴露过多历史失败，可按 NetUtil/TLS、CommandStatus、STOMP、orchestrator/main/cache 分批恢复。
- CI 必须输出 collected test count，并对核心测试数设置下限，防止以后静默归零。
- 恢复测试时保留失败证据，区分产品缺陷、Python 3 兼容问题和旧 mock 行为差异。

### 4.5 P2：缓存更新不是原子操作

证据：

- `ambari-agent/src/main/python/ambari_agent/FileCache.py:192-238` 在解压新 archive 前删除当前缓存。
- 解压或写 hash 失败时，日志声称继续使用缓存，但旧缓存已经不存在。
- `ambari-agent/src/main/python/ambari_agent/ClusterCache.py:138-152` 分别覆盖 JSON 和 hash，没有原子 rename。
- 首次持久化时只有旧 `self.hash` 非空才写新 hash，可能导致首次 hash 丢失。
- Agent 下载的 Server resource archive 没有绑定到由已认证 WSS 控制通道传递的内容摘要；只使用不可信 HTTP 或本地 `.hash` 不能证明下载内容来自当前 Server generation。

建议：

- 下载到临时文件，先验证 archive、路径和 hash。
- 解压到同文件系统临时目录，完成后通过 rename/swap 原子替换。
- 替换失败保留上一版缓存。
- Cluster cache 使用临时文件、`flush/fsync` 和 `os.replace`，JSON 与 hash 增加 generation 标识。
- Server 应原子发布每个 archive 的 SHA-256 manifest，并通过已认证控制通道下发；Agent 在解压前验证可信摘要，HTTP 缺少摘要时 fail closed，同时明确 Server-first 升级顺序。

### 4.6 P2：凭据文件和密钥作用域过宽

证据：

- `ambari-agent/src/main/python/ambari_agent/CustomServiceOrchestrator.py:315-351` 生成 JCEKS 后设置为 `0644`。
- Agent/Common/Ranger/Ranger KMS 的旧 JCEKS 创建命令通过 `-value <password>` 把明文放进 argv，本机其他用户可通过进程列表观察。
- `ambari-agent/src/main/python/ambari_agent/CustomServiceOrchestrator.py:475-476` 将 `AGENT_ENCRYPTION_KEY` 写入 Agent 全局环境。
- `ambari-agent/src/main/python/ambari_agent/PythonExecutor.py:218-231` 复制整个 `os.environ` 给所有后续子进程。

建议：

- JCEKS 应根据目标 service user 设置 owner/group，优先 `0640`，避免 world-readable。
- 凭据必须通过有边界的 stdin 协议传入 helper，argv、日志和异常中不得包含明文；helper 应拒绝旧 `-value` 入口、截断和尾随输入，并在失败时保留旧 store。
- 解密密钥只注入确实需要它的单次 PythonExecutor 环境，不修改进程全局环境。
- 在日志、异常、命令 dump 和 debug 输出中对密钥做显式屏蔽。

### 4.7 P2：远程调试接口不适合生产 Agent

证据：

- `ambari-agent/src/main/python/ambari_agent/RemoteDebugUtils.py:52-73` 使用 `/tmp/debug-<pid>` 和默认 `0666` FIFO。
- `RemoteDebugUtils.py:90-106` 使用 pickle 传输对象。
- `RemoteDebugUtils.py:124-175` 在 root Agent 进程内执行收到的 Python 代码。
- `ambari-agent/src/main/python/ambari_agent/HeartbeatHandlers.py:136-145` 默认绑定 SIGUSR2 handler。

风险需要准确描述：普通用户不能直接向 root Agent 发送 SIGUSR2，但一旦管理员触发调试，本地用户可能抢占或接入可预测 FIFO，进而获得 root 代码执行。

建议优先删除该功能。若必须保留，应使用 root 私有运行目录、`0600` socket、peer credential 校验、非 pickle 的受限协议，并通过显式配置启用。

### 4.8 P2：默认 enrollment passphrase

- `ambari-agent/conf/unix/ambari-env.sh:16-20` 和 `ambari-agent/conf/unix/ambari-agent:162-170` 使用默认值 `DEV`。
- 默认共享秘密不应被视为认证边界。
- 应由安装/注册流程生成随机值或使用一次性 enrollment token，并在成功注册后失效。

## 5. 第二层：Python 运行时和打包契约

本节前半部分保留首次审计发现的冲突。当前契约已统一为 Python >= 3.9.2，并要求启动解释器 minor 与包内 CPython 原生扩展 ABI 完全一致；当前证据见第 13 节。

### 5.1 当前契约互相矛盾

| 位置 | 当前行为 |
| --- | --- |
| `pyproject.toml:21` | `requires-python = "~=3.6"` |
| `pyproject.toml:74-75` | 注释写 Python 3.6，Ruff 实际 target 为 `py37` |
| `install-helper.sh:121-131` | 优先 `/usr/bin/python`，接受任意 Python >= 3.0 |
| `ambari-agent:173-192` | 调用 `python` 而非 `${PYTHON}`，最低版本仍是 2.7 |
| `dependencies.properties:32` | Debian 依赖仍为 `python (>= 2.6)` |
| `dependencies.properties:31` | RPM 只依赖默认解释器的 `python3-rpm`/`python3-distro`，不能保证可被 Python 3.9 import |
| `ambari-python-wrap:20-29` | PYTHONPATH 固定到 Python 3.9，但解释器优先通用 `python3` |

结果是安装成功不等于运行时可用。不同发行版可能选择不同解释器，同时加载错误版本的 site-packages。

### 5.2 推荐基线

本轮确定最低运行版本为 Python 3.9.2，仍属于 Python 3.9 发行版兼容基线。`cryptography 50.0.1` 明确排除了 Python 3.9.0/3.9.1，因此不能把元数据宽泛写成实际不可满足的 `>=3.9`。Rocky 8 AppStream 和 Rocky 9 当前提供的 Python 3.9 补丁版本均高于该下限。Python 3.9 已结束上游维护，因此这里只把它作为发行版兼容下限，CI 和开发目标仍应包含 Python 3.12/3.13。

Rocky Linux 8 的默认 `/usr/bin/python3` 和系统管理用 `/usr/libexec/platform-python` 通常是 Python 3.6.8，不是 Python 3.9。Rocky 8 上必须从 AppStream 显式安装 `python39`，并使用 `/usr/bin/python3.9`；安装器不能假设 `python3` symlink 已经切换版本。Rocky 9 可以使用系统 Python 3.9。

对应的运行契约是：

- `pyproject.toml`、安装器、init script、RPM/DEB 依赖、wrapper、开发容器和 CI 统一声明 Python >= 3.9.2。
- 解释器解析先检查明确配置，再检查 `/usr/bin/python3.9`，最后才接受满足版本检查的 `python3`；不再接受裸 `python` 或任意 Python >= 3.0。
- Rocky 8 RPM 显式依赖 `python39`，官方第三方依赖在构建期按 Python 3.9 lock/hash 打入 Ambari 私有 lib，节点运行时不执行 `pip install`。
- Server `sbin/ambari-server` 与 Agent init script 都在进入业务代码前检查 `sys.version_info >= (3, 9, 2)`；安装 helper 和 wrapper 使用相同下限，避免包管理器声明与实际入口不一致。
- Linux host bootstrap 先创建 `/usr/bin/ambari-python-wrap`，再通过它运行远端 `setupAgent.py`；不能直接调用 Rocky 8 上指向 Python 3.6 的通用 `python3`。
- 当前 `python3-rpm` 是默认 Python 3.6 的 ABI package，不能被 `/usr/bin/python3.9` 可靠 import。`yum_manager.py` 和 `zypper_manager.py` 的两处 `rpm.TransactionSet()` 查询应改用系统 `rpm` CLI，并删除 Agent 对 Python RPM binding 的依赖。
- `distro` 也不从默认 Python 3.6 的 `python3-distro` site-packages 加载，而是作为锁定的纯 Python runtime dependency 随 Agent 打包。

构建契约不能跟随构建机解释器和操作系统自动选择 wheel。根 Maven 属性固定 `--platform manylinux2014_x86_64 --python-version 3.9.2 --implementation cp --abi cp39`，Agent 和 Server 使用同一组参数。这里必须写完整的 3.9.2：pip 会把 `3.9` 当作 3.9.0，而 cryptography 50.0.1 明确排除 3.9.0/3.9.1。macOS/Apple Silicon 或 Python 3.12/3.13 开发机执行打包时，仍下载 Rocky 8/9 x86_64 可加载的 CPython 3.9 wheel，而不会把 macOS、arm64 或其他 CPython ABI 的原生扩展写入 Linux 安装包。该契约也意味着当前 RPM/DEB 产物明确面向 Linux x86_64；未来支持 aarch64 时必须生成独立 lock/产物并采用对应 platform tag，不能把两种原生扩展混装到同一私有 lib。

RPM 的包架构和 Python wheel 架构是两个独立参数。仓库默认 `build.os_arch=x86_64`，因此在 aarch64 构建容器中验证时必须同时传入 `-Dbuild.os_arch=aarch64` 和 `-Dpython.wheel.platform=manylinux2014_aarch64`；只设置 wheel platform 会生成包含 aarch64 扩展、但 RPM header 仍声明 x86_64 的错误产物。默认值继续固定为 x86_64，避免发布产物因开发机架构而静默变化。

Rocky 8 的 RPM `%install` 默认运行 `brp-mangle-shebangs`，并通过 `PYTHON3=/usr/libexec/platform-python` 把 `#!/usr/bin/env python3` 改成系统 Python 3.6；官方 `distro.py` 的兼容 CLI shebang 还是 `#!/usr/bin/env python`，会被该脚本作为 ambiguous shebang 直接拒绝并导致构建失败。Agent 和 Server RPM 均显式设置 `__brp_mangle_shebangs %{nil}`，禁止打包工具修改已审查的入口。Ambari 的实际服务入口由 `/usr/bin/ambari-python-wrap` 选择并校验 Python >= 3.9.2，普通模块 shebang 不作为运行时解释器选择机制。

`docopt==0.6.2` 是唯一的 sdist 例外：stomp.py 8.2.0 把它声明为硬依赖，但 PyPI 只发布源码归档。pip 在指定 `--platform/--python-version/--abi` 时不允许同时混用 `--only-binary=:all:` 和 `--no-binary docopt`，因此不能在一次跨平台安装中处理。Agent POM 采用两个顺序执行：主 lock 由 `uv --no-emit-package docopt` 生成，用固定 Linux/CPython 3.9 ABI、`--only-binary=:all: --no-deps --require-hashes` 安装完整 wheel 闭包；独立 `requirements-sdist.lock` 只锁定 docopt 的 sdist/hash，不带跨平台参数，以 `--no-binary=:all: --no-deps --require-hashes` 安装到同一私有 lib。`--no-deps` 是 pip 跨平台安装的必要约束，不代表忽略依赖完整性；产物验证必须基于已安装 `.dist-info` 执行等价于 `pip check` 的闭包检查，确认 stomp.py 的 `docopt` 和 `websocket-client` metadata 均已满足。PEP 517 构建工具由发布环境或离线镜像提供，生成的 Agent 运行包只包含安装结果，不包含 docopt 源码树，也不能用 `docopt-ng` 制造 distribution metadata 不一致。

根 `ambari-python` 和 Agent 源码包各自提供完整 `pyproject.toml`，固定 setuptools build backend、Python floor 和 runtime metadata。`dev-support/test_python_sdist.py` 在隔离副本中以 `python -m build --sdist --no-isolation` 同时构建两类 sdist，核对根包所需运行资源、不包含旧 vendored 目录，并核对 Agent `PKG-INFO` 的 `Requires-Python` 和完整 `Requires-Dist`。该测试依赖先按 `requirements-build.lock` 安装 `build`/setuptools，不能在缺少 build module 的宿主解释器上把环境错误记为源码失败或通过。

不能只修改 `pyproject.toml` 或 wrapper，否则会出现包装器选择 Python 3.9、依赖却仍安装在 Python 3.6 site-packages 的半升级状态。

### 5.3 Python 3.12/3.13 阻塞点

- 基线中的 `ssl.wrap_socket` 已在 Python 3.12 移除，原调用位于 `ambari_commons.network`、ws4py/stomp fallback 和 BIGTOP HDFS service check；本 worktree 已改为 `SSLContext` 并删除旧 transport。
- 基线中的 APScheduler 2.1.2 使用 `setDaemon`、旧式线程池和旧调度 API；本 worktree 已通过 Ambari facade 迁移到官方 3.11.3。
- 基线中的 vendored Jinja2 包含大量 Python 2/早期 Python 3 兼容层；本 worktree 已删除 fork 并通过 `Template/InlineTemplate` facade 使用官方 3.1.6。
- 基线测试使用 2012 年的 mock 1.0.1；本 worktree 已迁移到 `unittest.mock` 并删除 vendored mock。
- `ambari_commons/os_check.py` 原来使用 `eval` 解析名为 JSON 的内部资源文件；本 worktree 已改为标准库 `json.load()`，显式使用 UTF-8，并校验顶层对象、`mapping`、`aliases` 和每个 family 的 `distro` 结构。损坏或结构错误的资源在初始化时仍转换成原有统一加载异常，同时通过 exception chaining 保留具体原因。
- `ambari_commons.network` 顶层导入 `resource_management.core.exceptions` 会先执行 `resource_management.__init__` 的通配初始化，后者通过 script 模块再次回引尚未初始化完成的 network；结果取决于模块导入顺序，单独加载网络 helper 会失败。本 worktree 已把仅在 SSL 校验失败时使用的 `Fail` 延迟到异常路径导入，消除该循环。
- tarball 安装器原来使用 Python 3.12 已删除的 `configparser.SafeConfigParser()`，并把 `range` 与 list 相加，实际无法在 Python 3 执行依赖回退选择；本 worktree 已改用 `ConfigParser()`，恢复 `family+major -> family -> generic` 的候选顺序，无匹配且显式跳过依赖时直接返回。
- `ambari_server.utils.update_latest_in_repoinfos_for_stacks()` 原来调用 iterator 的 `.next()`，并以默认 bytes 编码向文本 XML 流写入；本 worktree 已改为 `next()` 和 `encoding="unicode"`，覆盖 stack repo 元数据更新路径。
- `HookSequenceBuilder` 原来使用字符串 identity 表达式 `"role" is hook_definition`，在 Python 3.9 会产生 SyntaxWarning 且条件实际恒为 false，空 role 命令仍会生成 role hook；本 worktree 已改为 membership 判断并增加空 role 序列测试。
- `FacterLinux` 的 interface IP/netmask ioctl 原来每次探测都遗留 datagram socket；本 worktree 已在成功和异常路径通过 `finally` 关闭，避免 Agent 周期性 fact/alert 收集持续消耗文件描述符。
- `TestBootstrap` 在 2024 年 Ruff 集成时把 `TestCase` 基类注释掉，31 个使用 `self.assert*` 的方法既不能被 unittest 收集，也不能由 pytest 正常执行；本 worktree 已恢复原有基类，并修复 bootstrap grep command 的无效转义 warning，使 Linux bootstrap 修改重新进入测试覆盖。
- `ambari_commons/libs/x86_64/_posixsubprocess.so` 针对 Python 2.7 和 Ubuntu 16.04 构建，原消费者 `subprocess32.py` 已删除，但该二进制仍进入 Agent 包。
- 基线中的 `takeover_config_merge.py` 使用无 Loader 的 `yaml.load()` 和已移除的 `getiterator()`；本 worktree 已迁移为 `safe_load()`、mapping 校验和 `iter()`。
- `pluggable_stack_definition/GenerateStackDefinition.py` 的七处 `getiterator()` 已改为 `iter()`，profile 本身暂时保留。
- BIGTOP HDFS `checkWebUI.py` 的 `ssl.wrap_socket()` 已改为显式 `SSLContext`，并修复未创建连接时异常路径再次调用 `close()` 的问题。
- 基线中的 `export_ams_metrics.py` 使用失效的 `flask.ext.cors` 且没有依赖声明；本 worktree 已删除该无入口脚本。
- Agent、Server CLI 和资源脚本仍大量使用已停止功能演进的 `optparse`。它尚未从 Python 3 删除，不是 3.12/3.13 启动阻塞，但新参数和新子命令应统一使用 `argparse`，旧入口按命令逐个迁移并保持 help、默认值和退出码兼容。
- `contrib/agent-simulator` 虽然在 2024 年被机械改成 `python3` shebang，主体仍使用 Python 2 `print` 语句和 `ConfigParser`，在 Python 3 下不能启动；内部远程命令还显式调用不确定版本的 `python`。

`ambari_commons.import_utils` 已经用 `importlib.util` 和 `types.ModuleType` 替代被 Python 3.12 删除的 `imp`。这个 Ambari facade 可以保留，用于 stack advisor 和 alert script 的动态加载；应逐步把调用点的局部别名从 `imp` 改成表达真实语义的名称，但没有必要为此引入第三方 plugin framework。

## 6. 第三层：旧依赖升级审计

本节保留迁移前的依赖风险和选择依据。官方库迁移、vendored 源码删除、调用方迁移、lock/Maven/许可证闭包和失败测试的当前结论见第 13 节。

版本信息通过 2026-08-31 的 `pip index versions` 进行核对。

| 依赖 | 当前版本 | 当前最新稳定版 | 建议 | 风险/工作量 |
| --- | ---: | ---: | --- | --- |
| mock | 1.0.1 | 标准库已提供 | 删除，改用 `unittest.mock` | 低风险，64 个测试文件 |
| simplejson | 3.16.1 | 4.1.2 | 删除，改用 `json` | 低到中，约 56 个调用点 |
| pbkdf2 | 1.3 | 1.3 | 删除，改用 `hashlib.pbkdf2_hmac` | 中，需 Java/Python golden vectors |
| pyaes | 1.3.0 | 1.6.1 | 删除，改用官方 `cryptography` | 高，需先兼容旧格式，再迁移 Server/Agent 加密协议 |
| cryptography | 当前未使用 | 50.0.1 | 根据 Python floor 锁定官方兼容版本 | 中，包含原生 wheel 和离线打包要求 |
| APScheduler | 2.1.2 | 3.11.3 | 删除 vendored fork，直接使用官方 3.11.x | 中高，需把线程注入移回 Ambari 调用层 |
| ws4py | 0.5.1 | 0.6.0 | 删除，由官方 stomp.py WebSocket adapter 替代 | 高，核心控制通道 |
| stomp.py | 4.1.17 | 8.2.0 | 删除 vendored fork，使用官方 `WSStompConnection` | 高，需验证 ACK、重连和 listener 行为 |
| websocket-client | 当前未使用 | 1.9.0 | 使用 stomp.py 官方依赖范围内的锁定版本 | 中，需验证 TLS 和双向证书配置 |
| Jinja2 | 2.5.5 | 3.1.6 | 删除 vendored fork，直接使用官方 3.1.x 和 MarkupSafe | 很高，需保持 Ambari 模板 facade 的输出兼容 |
| `_posixsubprocess.so` | Python 2.7 ABI，2018 构建 | Python 3 标准库已提供 | 直接删除孤儿二进制和空 `libs` package | 低，确认 tar/RPM/DEB 内容即可 |
| CoilMQ | 1.0.1 | 1.0.1 | 删除 vendored test broker 和错误的 runtime dependency | 中，重写两个 STOMP 测试入口 |
| javaproperties | 当前为复制的 ActiveState recipe | 0.8.2 | 通过 Ambari facade 使用官方包，删除通用 parser 源码 | 中，需保持注释、顺序、转义和写回语义 |
| distro | OS 包/CI 未固定 | 1.9.0 | Python 3.10+ 优先改用标准库，否则锁定 1.9.0 并使用现代 API | 低到中，RPM/DEB 契约不一致 |
| PyYAML | 节点可选安装，版本未声明 | 6.0.3 | 显式锁定，使用 `safe_load`；若 takeover 工具废弃则删除依赖 | 中，需确认 takeover 支持状态 |
| python-kerberos | OS 包，版本未声明 | 1.3.1 | 删除无效 import gate 和组件包依赖，不需要换成 gssapi | 低，仅做模块存在性检查 |
| setuptools | build 版本未锁定 | 82.0.1 | 作为 build-only 依赖按 Python floor 锁定，迁移到 PEP 517 元数据 | 中，不进入 Agent runtime |
| docopt | 当前未使用，stomp.py 8.2 硬依赖 | 0.6.2 | 短期锁定 sdist/hash 并推动 stomp.py 上游改为 CLI extra/argparse，不在 Ambari 内 fork | 中，上游包多年未发布且没有 wheel |
| tzlocal | 当前未使用，APScheduler 3.11 硬依赖 | 5.4.4 | 按 Python floor 锁定；Python 3.9 使用兼容版本，3.10+ 才能用最新版本 | 低，基础 scheduler 唯一新增依赖 |
| Flask 工具链 | 未声明，代码使用旧 namespace | Flask 3.1.2 / Flask-Cors 6.0.2 | 优先删除无入口的 metrics export 工具；若保留则删除 Flask-RESTful | 中，不应进入 Agent runtime |
| Ruff | CI 安装 latest | 持续变化 | 固定版本并逐步启用规则 | 低，但应避免一次性格式 churn |

### 6.1 mock：应删除而不是升级

`ambari-common/src/test/python/mock/mock.py:7` 表明当前为 mock 1.0.1。Python 3 已经提供 `unittest.mock`，继续维护 vendored test library 没有价值。

迁移建议：

- 先恢复被禁用测试的收集，避免同时改变 mock 行为而难以定位失败。
- 再按模块机械替换 import。
- 由于涉及 64 个文件，应拆成多个 reviewable commits，不要形成一个无法审阅的大提交。

### 6.2 simplejson：优先使用标准库

当前保留 simplejson 的注释主要强调相对 Python 2.6 标准库的性能优势。这个前提已经不存在。

静态扫描没有发现生产调用使用 `use_decimal`、`for_json`、`ignore_nan` 等 simplejson 专属参数，因此原则上可以迁移到标准库。但仍需 golden tests 覆盖：

- heartbeat 和 command report 的序列化结果。
- Unicode、bytes、NaN/Infinity 和异常行为。
- 字典顺序、compact separators 和大整数。
- cache JSON 的读取兼容性。

### 6.3 PBKDF2 和 pyaes：分为兼容替换与协议升级

当前 Python 解密路径位于 `resource_management/core/encryption.py:38-44`：

```text
PBKDF2-HMAC-SHA1, 65536 iterations, 128-bit key
AES-CBC + PKCS padding
salt::iv::ciphertext 的 hex 编码
```

Java 端位于 `ambari-server/.../AESEncryptor.java:55-111`，使用 `PBKDF2WithHmacSHA1` 和 `AES/CBC/PKCS5Padding`。

当前属性前缀虽然叫 `aes256_hex`，但 `AESEncryptor.java:40-41` 的实际 key length 是 128 bit。协议 v2 不应继续复用这个有歧义的名称。

推荐两阶段迁移：

1. 兼容实现替换：使用标准库 `hashlib.pbkdf2_hmac` 和官方 `cryptography` 解密旧格式，保持 wire format 完全不变。
2. 协议 v2：Server 使用 AES-GCM 等 AEAD 格式发送新值，Agent 同时支持 v1/v2 解密，升级窗口结束后再移除 CBC。

兼容阶段直接使用官方密码实现，不复制或修改第三方源码：

```python
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

key = hashlib.pbkdf2_hmac("sha1", password, salt, 65536, dklen=16)
decryptor = Cipher(algorithms.AES(key), modes.CBC(iv)).decryptor()
```

这一步只替换 Python 实现，Java/Python wire format、PBKDF2-SHA1 参数和 PKCS padding 均保持不变，并通过双向 golden vectors 验证。

协议 v2 应使用明确版本的 envelope，而不是继续解析位置相关的 `salt::iv::ciphertext`。建议至少包含：

```json
{
  "version": 2,
  "algorithm": "AES-256-GCM",
  "kdf": "PBKDF2-HMAC-SHA256",
  "iterations": 200000,
  "salt": "...",
  "nonce": "...",
  "ciphertext": "...",
  "tag": "..."
}
```

具体 KDF 和 iteration 参数应在安全评审和性能测试后固定，写入 envelope 并设置允许范围，不能由不可信输入任意放大。

发布顺序：

1. Agent 先发布 v1/v2 双解密能力，并在 registration 的 `encryptionTypes` 中声明 `aes256_gcm` capability。
2. Server 仅向声明 capability 的 Agent 发送 v2，旧 Agent 继续接收 v1。
3. 完成 Agent 升级窗口后停止发送 v1，再在后续版本删除 CBC 解密代码。

Agent command/config 加密必须与 Server master-key 持久化加密隔离。当前 `AESEncryptionService` 还被 master key 等流程使用，不能为了 Agent 协议一次性全局替换。

不能直接把 pyaes 1.3.0 升到 1.6.1后宣称安全问题已经解决。AES-CBC 缺少认证的问题与库版本无关。

### 6.4 APScheduler：使用官方库，适配 Ambari 调用层

当前只有 AlertSchedulerHandler 是主要业务消费者，但 vendored APScheduler 已经被 Ambari 修改：

- `AlertSchedulerHandler.py:72-84` 注入 `context_injector` 和 Agent config。
- `apscheduler/threadpool.py:38-63` 扩展了上游 thread pool 构造参数。
- 当前调用 `add_interval_job`，APScheduler 3.x 推荐使用 `add_job(..., trigger="interval")`。

目标不是继续修改或重新 fork APScheduler，而是删除 `ambari-agent/src/main/python/ambari_agent/apscheduler`，直接使用官方 APScheduler 3.11.x：

```python
from apscheduler.executors.pool import ThreadPoolExecutor
from apscheduler.schedulers.background import BackgroundScheduler
```

Ambari 当前加入 APScheduler 内部线程池的 `context_injector` 属于 Agent 业务逻辑。它应移动到 Ambari 自己的 job wrapper，例如：

```python
def run_alert_with_agent_context(definition):
  initialize_agent_context(config)
  definition.collect()
```

官方 scheduler 只负责调度这个 wrapper，不感知 AmbariConfig、resource management monkey patch 或 Agent 生命周期。

具体适配：

- 使用 `BackgroundScheduler`。
- 显式配置 `ThreadPoolExecutor(max_workers=3)`。
- 用 Ambari job wrapper 设置 resource management 上下文，不修改 APScheduler 内部实现。
- 将 `add_interval_job()` 映射为 `add_job(..., trigger="interval")`。
- 创建 job 时设置稳定的 UUID `id/name`，不再创建后修改 `job.name`。
- 将 `unschedule_job(job)` 映射为 `remove_job(job.id)`。
- 保留 `coalesce`、`misfire_grace_time`、动态 definition reload 和 on-demand alert 行为。
- shutdown 必须纳入 Agent 的统一退出顺序。

迁移完成后，代码库中不应保留 APScheduler 的复制源码或补丁。依赖版本由构建清单锁定，后续升级直接使用官方发布包。

### 6.5 ws4py 和 stomp.py：按控制通道迁移

ws4py 0.6.0 相对当前 0.5.1 的提升不足以解决维护状态和现代 TLS 问题，因此不建议做一次 0.5.1 到 0.6.0 的过渡升级。

核对官方 stomp.py 8.2.0 发布包后确认，它已经提供 `stomp.adapter.ws.WSStompConnection`，内部使用官方 `websocket-client`。因此 Ambari 不需要继续维护自己的 WebSocket transport 或 adapter：

```python
from stomp.adapter.ws import WSStompConnection
```

目标结构：

```text
AmbariStompConnection
  -> official stomp.adapter.ws.WSStompConnection
  -> official websocket-client
```

`AmbariStompConnection` 只保留 JSON 序列化、correlation ID、日志脱敏、response callback 和 Ambari listener/destination 等业务逻辑，不包含 WebSocket framing、STOMP parser、socket reader 或重连算法。

迁移完成后删除：

```text
ambari-common/src/main/python/ambari_ws4py
ambari-common/src/main/python/ambari_stomp
```

当前 `ambari_stomp/adapter/websocket.py` 是 Ambari 自定义桥接层，历史上还修复过连接泄漏、重连 hang、race condition 和消息锁问题。迁移必须先把行为固化成 contract tests：

- STOMP 1.2 CONNECT/SUBSCRIBE/SEND/DISCONNECT。
- destinations、headers 和 correlation ID 保持不变。
- Server receipt/response 与 command report ACK 的映射。
- 单连接消息顺序和并发 send 锁。
- Server 重启、网络中断、半开连接、超时和重新注册。
- TLS CA、hostname、客户端证书和 SNI。
- clean close 与 Agent stop/restart。

配置适配只负责把当前 WSS URL 拆分为 host、port 和 `ws_path`，并通过官方 `set_ssl()` 映射 CA、hostname verification、client certificate 和 key。必须分别验证单向 TLS 和双向 TLS，不能假设官方默认值与 Ambari 当前配置语义一致。

官方 `WSStompConnection.connect()` 自身会启动 transport 和 receiver thread，因此调用层必须只调用一次 `connect()`，不能保留旧 adapter 的显式 `start()`，否则会创建重复 reader。Ambari facade 以 event 等待协议级 `CONNECTED`，最长 10 秒；收到断开或错误、transport 未连接或超时都应让注册失败并进入既有重试路径，不能无限阻塞 Agent 启动。

官方 stomp.py 8.2.0 要求 Python `>=3.7,<4.0`，并依赖 `websocket-client>=1.2.3,<2.0.0` 和 `docopt`。构建清单需要锁定这些传递依赖并打入离线 Agent 包；其中 docopt 没有官方 wheel，必须通过独立 hash lock 作为明确的纯 Python sdist 例外处理。

如果官方 adapter 存在缺陷，优先向 stomp.py/websocket-client 上游提交修复。Ambari 侧最多保留组合式 wrapper，不重新复制第三方源码形成新 fork。

### 6.6 Jinja2：使用官方库，保留 Ambari 模板 facade

虽然直接 import 集中在 `resource_management/core/source.py` 和少量 helper，但所有 `Template`、`InlineTemplate`、XML/properties provider 以及 stack service templates 都间接依赖它。

目标同样不是修改 Jinja2 3.1.x，也不是把新版本源码重新复制成 `ambari_jinja2`。应删除 `ambari-common/src/main/python/ambari_jinja2`，直接导入官方 Jinja2 和 MarkupSafe：

```python
from jinja2 import BaseLoader, Environment, FunctionLoader, StrictUndefined
```

`resource_management.core.source.Template/InlineTemplate` 应继续保留。它们是 Ambari 面向 stack scripts 的稳定业务 API，不是 Jinja2 fork；内部实现改为组合官方 `Environment`。

当前 `TemplateLoader` 可以继续继承官方 `BaseLoader`，也可以用官方 `FunctionLoader` 表达动态模板查找。这些都是 Jinja2 支持的公开扩展点，不需要修改第三方源码。

必须保持的当前语义：

- `autoescape=False`。
- `StrictUndefined`。
- `trim_blocks=True`。
- 自定义 TemplateLoader 基于 stack/service 的 `templates` 目录解析。
- `extra_imports`、`env`、`unicode=str` 等模板上下文。

迁移前应建立代表性模板 corpus，覆盖 HDFS、YARN、HBase、Hive、Kafka、Ranger、Kerberos 和 HA 配置。比较旧/新引擎生成内容，只有明确评审过的差异才允许进入 golden files。

Jinja2 迁移不要与 resource management 重构、模板格式化或 BIGTOP stack 行为修改放在同一 PR。

迁移完成后，stack scripts 仍然调用 Ambari 的 `Template/InlineTemplate`，而 Ambari 模板层只依赖官方 Jinja2 API。后续安全升级只需更新锁定版本并处理明确的公开 API 变化。

### 6.7 `_posixsubprocess.so`：孤儿 Python 2 二进制应直接删除

证据链完整，不需要再为它寻找 Python 3 替代包：

- `ambari-common/src/main/python/ambari_commons/libs/x86_64/_posixsubprocess.so` 是带 debug info 的 x86-64 ELF；二进制字符串显示它基于 Python 2.7 headers、Ubuntu 16.04 和当时的 GCC 构建。
- 该文件在 2018 年为 vendored `subprocess32.py` 引入，用于规避 Python 2 多线程 fork/exec 的可靠性问题。
- `subprocess32.py` 已在 2024 年 Python 3 迁移中删除，当前生产和测试代码都没有 import 这个二进制。
- `ambari-agent/src/packages/tarball/all.xml:37-42` 会复制整个 `ambari_commons` 目录，因此它仍进入 Agent tar/RPM/DEB。
- 根 `pom.xml` 仍只排除旧路径 `ambari_commons/_posixsubprocess.so`，不能排除当前的 `ambari_commons/libs/x86_64/_posixsubprocess.so`。

Python 3 的 `subprocess` 已由解释器自身提供相匹配的 `_posixsubprocess` 实现。Ambari 不应 import、重编译或分发 CPython 私有扩展。建议删除 `.so` 以及删除后为空的 `ambari_commons/libs` package，并清理无效的旧路径 exclusion。

这项清理应通过检查构建产物内容验证，不需要行为迁移测试。`ambari_simplejson/_speedups` 下的 Python 2 ABI 二进制则随 simplejson 整包删除，不另做移植。

### 6.8 CoilMQ：删除完整测试 broker 和错误的生产依赖

`ambari-common/src/test/python/coilmq` 是 CoilMQ 1.0.1 的完整源码副本，共 26 个文件，包含 broker engine、queue/topic、store、scheduler、STOMP protocol 和自带 `six.py`。上游最新版本仍是 1.0.1，因此这里不存在一个有意义的“升级到新版”方案。

当前只有两个测试入口使用它：

- `BaseStompServerTestCase.py` 在固定 `21613` 端口启动 STOMP 1.0 raw TCP broker，并把生产 `AmbariStompConnection` monkey-patch 为 `TestCaseTcpConnection`。
- `TestAgentStompResponses.py` 使用 CoilMQ 的 `Frame` 和 parser，但 `class TestAgentStompResponses:  # (BaseStompServerTestCase)` 已把继承注释掉，因此当前不会被 runner 收集；若直接恢复收集，现有 `super().setUp()` 也不能正常工作。

这套 fixture 历史上用于在没有 Ambari Server 时模拟 registration、topology、metadata、configuration、alert definition、commands 和 status report 的消息往返。但它绕过了生产 WSS transport、TLS、WebSocket path 和 Ambari Server endpoint，当前既没有执行，也不能覆盖计划迁移的 stomp.py WebSocket adapter。继续升级 CoilMQ 会产生测试存在的假象，而不能降低控制通道迁移风险。

同时，根 `setup.py` 把 `coilmq==1.0.1` 写入 `install_requires`。这会把测试 broker 错误地变成 `ambari-python` 的生产安装依赖，即使运行时没有任何调用。

替代方案应按测试层次拆开：

1. 把大文件拆为 registration、cache event、command/status report 等 focused tests；通过构造官方 stomp.py `Frame` 调用 listener，并用 Ambari `FakeConnection` 记录 destination、headers、body、correlation ID 和 callback，不启动 broker。
2. transport 层只保留少量 contract tests。若需要独立验证官方 stomp.py 的 STOMP-over-WebSocket/TLS 行为，可在 integration profile 中使用预创建的 ActiveMQ Artemis 容器；它不进入 Python runtime，也不替代 Ambari 业务测试。
3. WSS path、TLS、注册、重连、Server response 和 ACK 的最终 contract tests 应连接真实 Ambari Server endpoint，因为通用 broker 无法代表 Ambari Java 控制端行为。
4. 用 `threading.Event` 和有界 queue wait 替代固定端口、`time.sleep` 和依赖全局 frame 顺序的断言。

迁移后删除 vendored `coilmq` 目录和 `setup.py` 中的 `install_requires=["coilmq==1.0.1"]`。不要自行实现一个新的 STOMP server，也不要把 stomp.py client 当作 server 使用。

### 6.9 Java properties parser：用官方库替换复制的 recipe

`ambari-server/src/main/python/ambari_server/properties.py:47-50` 明确说明实现来自 ActiveState 的 Java properties recipe。它不是 Ambari 领域逻辑，却长期由 Ambari 自己承担转义、续行、分隔符、排序和写回兼容性。

本 worktree 已选择保留 Ambari 当前 `Properties` facade，内部改用官方 `javaproperties` 0.8.2 的 parse/dump API，而不是让所有调用点直接依赖第三方对象。新增 focused tests 覆盖：

- `=`、`:`、空格分隔和 escaped separators。
- continuation line、Unicode escape、空值和重复 key。
- 注释、原始 key、输入顺序和当前的 sorted write 行为。
- `store()` 当前附加 ASF header、用户 header 和 timestamp 的行为。
- 写失败时异常继续上抛并关闭 stream；当前 facade 不承诺原子替换目标文件。

Ambari facade 保留排序写回、header 和调用接口等少量组合代码，但不再维护一套完整的通用语法 parser。最终应按 Server CLI 依赖边界拆为独立 topic commit。

### 6.10 distro、PyYAML 和 python-kerberos：清理依赖契约

#### distro

当前 RPM 声明 `python3-distro`，DEB 仍声明 `python (>= 2.6)` 且没有 distro；代码调用已废弃的 `distro.linux_distribution()`。此外，`resource_management/core/system.py` import 了 distro，却读取不存在的 `sys.distro`。

由于最低版本确定为 Python 3.9，不能使用 Python 3.10 才提供的 `platform.freedesktop_os_release()`。应锁定官方 distro 1.9.0，改用 `distro.id()`、`distro.version()` 和 `distro.codename()`，并作为纯 Python 依赖打入 Agent/Server 私有 lib。RPM 不再依赖默认 Python 3.6 的 `python3-distro`，也不能继续让解释器碰巧加载系统 site-packages。

#### PyYAML

`takeover_config_merge.py` 在运行时临时 import PyYAML，缺失时提示管理员手工 `pip install`，但 Agent 离线部署不能接受这种依赖模式。当前 `yaml.load(file)` 在 PyYAML 6.x 缺少 Loader 参数时会失败，在旧版本下还可能构造任意 Python 对象。

本 worktree 采用保守兼容决策：保留 takeover merge 工具，将 PyYAML 6.0.3 锁入 Server 第三层依赖，构建期打包并改用 `yaml.safe_load()`，同时校验顶层必须是 mapping。这样不会在依赖现代化过程中未经支持状态确认就删除管理员可能仍直接调用的 Server 工具。

`ambari_commons/yaml_utils.py` 不是复制的 YAML 引擎，但当前仓库只有测试引用。应先确认外部 management pack 是否把它当作兼容 API；没有外部契约时可删除，有契约时则保留薄 facade 并用安全 parser 实现，不继续扩展正则 YAML 解析。

#### python-kerberos

`AMBARI_METRICS/3.0.0/metainfo.xml` 安装 `python-kerberos`，但仓库里唯一的生产 import 只是 `ams.py` 对模块是否存在的检查，之后没有调用任何 kerberos API。这里不需要升级到 kerberos 1.3.1，也不需要无意义地替换为 gssapi。

应删除该 import gate 和组件 package dependency，再用 AMS SPNEGO 安装、启动和 service check 证明行为不依赖这个 Python 模块。如果运行验证发现真实的 GSSAPI 调用存在于仓库外脚本，再单独设计基于官方 gssapi 的适配层。

### 6.11 Python build 元数据：不要继续把测试库发布成 runtime

根 `setup.py` 当前把 vendored Jinja2、mock 测试包和 `urlinfo_processor` 混入 `ambari-python`，并把 CoilMQ 声明为 runtime dependency；`ambari-agent/src/main/python/setup.py` 仍使用 `1.0.3-SNAPSHOT`、Incubator URL 和独立于 Maven 的元数据。根 `pyproject.toml` 也没有 `[build-system]` 和完整依赖声明。

建议建立单一的声明式依赖来源，并让 Maven 的 Agent 离线包从该锁定清单构建：

- setuptools 仅作为 build dependency，按最低 Python 版本锁定，不进入 Agent 私有 runtime 目录。
- 采用 PEP 517 build backend 和 wheel 构建，不再使用 `setup.py install/upload`。
- runtime、test 和 tooling 依赖分组，禁止 mock、CoilMQ 等测试库进入 runtime metadata。
- Maven 版本、Python package version 和产物 SBOM 使用同一项目版本来源。
- `urlinfo_processor` 是 Ambari optional build-time 工具，不是应由第三方库替换的协议或 parser fork；确认下游仍使用 `replaceBaseUrl` profile 后才保留为独立 entry point。

### 6.12 源码所有权边界

本次按 package 目录、版本标识、非 ASF copyright、README/LICENSE 和历史提交扫描后，没有发现上述对象之外的新整包第三方 Python 副本。处理边界如下：

| 分类 | 对象 | 处理原则 |
| --- | --- | --- |
| 删除第三方源码 | `ambari_simplejson`、`ambari_pbkdf2`、`ambari_pyaes`、`ambari_jinja2`、`ambari_stomp`、`ambari_ws4py`、vendored APScheduler、mock、CoilMQ | 改用标准库或锁定的官方 package，仓库不保留重命名副本或补丁 fork；官方发行制品自带的 tests/docs/examples 可以随离线依赖进入产品包，但仍受 hash、RECORD、LICENSE 和 SBOM 审计 |
| 删除遗留二进制 | `_posixsubprocess.so`、simplejson `_speedups.so/.pyd` | 不维护 CPython 私有 ABI；随对应 package 清理并扫描发布产物 |
| 替换通用实现 | `ambari_server/properties.py` | 保留 Ambari facade，parser 委托官方库 |
| 保留 Ambari 业务层 | `ambari_agent`、`resource_management`、`ambari_commons`、listeners、Agent connection wrapper、`Template/InlineTemplate`、encryption facade | 这些代码表达 Ambari 生命周期、协议字段、组件配置和兼容契约；只把通用机制下沉到官方库 |
| 条件保留构建工具 | `urlinfo_processor` | 属于可选的 repo URL 重写 profile，不是第三方库 fork；先确认下游构建是否仍使用 |
| 已删除的历史工具 | `export_ams_metrics.py`、`ambari-server-state`、`contrib/agent-simulator` | 无入口或实现已失效，删除源码并同步清理 Maven、assembly、PYTHONPATH 和依赖引用 |
| 保留并安全迁移 | `takeover_config_merge.py` | 保留管理员可能直接使用的脚本，锁定 PyYAML，使用 safe parser、输入类型校验和现代 XML API |
| 保留兼容 | `pluggable_stack_definition` | Agent/Server/Admin/Web 仍有显式 Maven profile 消费者；保留入口并修复已删除 XML API |
| 删除前需确认外部 API | `ambari_commons/yaml_utils.py` | 仓库内只有测试引用，但外部 management pack 可能 import；先做兼容性调查 |

`ambari_commons.network`、`inet_utils` 和 `urllib_handlers` 也不是第三方源码副本。它们应该收敛到一个使用标准 `SSLContext`、`http.client`/`urllib` 公共 API 的薄网络层，而不是新增 requests fork 或继续维护 socket/TLS 底层实现。`ssl.wrap_socket`、全局 opener monkey patch 和关闭 hostname verification 的问题已归入 TLS 修复，不应与第三方 package 清理混为一项。

### 6.13 官方库的传递依赖边界

删除 vendored fork 后，依赖清单不能只写三个顶层包。通过 PyPI metadata 核对后的最小 runtime closure 是：

```text
stomp.py 8.2.0
  -> websocket-client >=1.2.3,<2.0.0
  -> docopt >=0.6.2,<0.7.0

APScheduler 3.11.3
  -> tzlocal >=3.0

Jinja2 3.1.6
  -> MarkupSafe >=2.0

cryptography 50.0.1 (CPython)
  -> cffi >=2.0.0
     -> pycparser
  -> typing-extensions on Python <3.11
```

版本 floor 也必须一起考虑：websocket-client 1.9.0、MarkupSafe 3.0.3、cryptography 50.0.1 和 cffi 2.0.0 要求 Python 3.9+；tzlocal 5.4.4 要求 Python 3.10+，因此当前 Python 3.9 基线必须锁定 tzlocal 5.3.1。构建发布应维护一套经过验证的 Python 3.9 lock；长期提高 floor 后可以使用更新版本，不能让 resolver 在构建时自由选择。

PyPI 发布物核对确认，cryptography 50.0.1、cffi 2.0.0、MarkupSafe 3.0.3 和 PyYAML 6.0.3 都提供 CPython 3.9 x86_64 的 manylinux2014 wheel；docopt 只提供已锁定 hash 的纯 Python sdist。主 wheel lock 与独立 docopt sdist lock 合并安装后的依赖闭包可以在 Rocky 8 的 glibc 基线上离线运行，但发布检查必须再次按 distribution metadata 验证闭包。LICENSE 已按实际 metadata 补齐：Jinja2、MarkupSafe、pycparser 使用 BSD-3-Clause；APScheduler、docopt、tzlocal、javaproperties、PyYAML 使用 MIT；cffi 使用 MIT-0；cryptography、distro、stomp.py、websocket-client 选择或使用 Apache-2.0；typing-extensions 使用 PSF-2.0。发布产物仍需核对每个 `.dist-info/licenses` 与根 LICENSE 一致。

stomp.py 的 `docopt` 是需要明确接受的残余风险。stomp.py 只在 CLI 入口使用它，但 8.2.0 metadata 把它声明为硬依赖，而 docopt 最新仍是 2014 年的 0.6.2，并且没有 wheel。建议向 stomp.py 上游提交把 CLI 改为 argparse 或 optional extra 的修改；上游发布前在独立 sdist hash lock 中保留 docopt，不能把 stomp.py 或 docopt 复制回 Ambari 仓库打补丁。`docopt-ng` 虽然维护活跃并提供同名 import，却不能满足 stomp.py 对 `docopt` distribution metadata 的要求，不应通过忽略产物依赖检查强行替换。

只安装 Ambari 实际使用的 base dependencies：

- APScheduler 不启用 SQLAlchemy、MongoDB、Redis、etcd、Tornado、Twisted、gevent 或 ZooKeeper jobstore extras；Alert scheduler 只需要内存 jobstore、interval trigger 和 thread pool。
- Jinja2 不安装 Babel i18n extra；官方 wheel 自带的非运行文件允许保留，但不得因此安装 Django、Mako、Genshi、Sphinx、Docutils 或 Pygments。
- 删除 ws4py 后不安装其历史 CherryPy、Tornado 和 gevent server/client adapters。
- cryptography 不安装 SSH extra；Agent 只使用 symmetric cipher primitives。

### 6.14 不要升级明显失去产品入口的历史工具

#### `export_ams_metrics.py`

该脚本自 2016 年后没有功能修改，仓库中没有调用方或使用文档，但 `server.xml` 会把整个 `resources/scripts` 目录打入 Server 包。它导入 Flask、`flask.ext.cors` 和 Flask-RESTful，RPM/DEB/pyproject 均未声明这些依赖；`Response` 和 `reqparse` 甚至没有被使用。

本 worktree 已删除该脚本。它没有仓库入口、文档或完整依赖契约，不应为它把 Flask 工具链加入 Server runtime。若后续证明确有受支持的离线导出流程，应基于明确需求和认证边界单独恢复，而不是继续兼容当前实现。原保留方案的最低要求是：

- 作为独立 optional tool 打包，不进入 Agent 或 Server 核心 runtime dependency。
- 使用官方 Flask 3.x 和 `from flask_cors import CORS`。
- 三个简单 GET endpoint 直接使用 Flask route，删除 Flask-RESTful，避免额外引入 `six`、`pytz` 和 `aniso8601`。
- 禁止默认启用 debug server，对 bind address、输入目录和响应大小设置明确限制。

#### `pluggable_stack_definition`

该目录的配置只覆盖 ODP、PHD 和 SAPHD，最后一次功能修改在 2016 年；默认 BIGTOP build 不激活 `pluggable-stack-definition` profile。当前实现有 7 处 Python 3.9 已移除的 `getiterator()`。

`pluggable-stack-definition` 和 `replaceBaseUrl` Maven profile 在 Agent、Server、Admin/Web 构建中有真实消费者，因此本轮明确保留。七处 `getiterator()` 已改为 `iter()` 以解除 Python 3.9 阻塞，并用 focused tests 固定替换和生成行为；未来退休必须另开兼容性 JIRA，不能在本轮推测删除。

#### `takeover_config_merge.py`

该脚本最后一次功能修改在 2016 年，仓库内没有调用入口，同时存在未声明 PyYAML、unsafe `yaml.load()` 和已移除 `getiterator()`。本 worktree 选择保留兼容：使用 Server 专属 lock 提供 PyYAML，改用 `safe_load()`、校验顶层 mapping、使用 `iter()`，并新增 unsafe/non-mapping/XML focused tests。后续确认功能正式废弃后可再删除。

#### `ambari-server-state`

`ambari-server/src/main/python/ambari-server-state` 是 2012 年遗留的交互式 XML 生成原型。它不进入 Server assembly，没有命令入口、文档、调用方或测试；当前仅被无意义地加入 Maven test `PYTHONPATH`。代码还保留 class-level mutable collections、wildcard imports 和机械 Python 3 转换后的 `input(...).numerator`。

本 worktree 已删除该目录并清理 Maven test path，无需为其寻找 XML library 或补兼容层。

#### `contrib/agent-simulator`

该工具的最后一次功能修改在 2015 年，目标是用 GCE VM、CentOS 7、Docker 1.7 和旧 Weave 网络模拟数千 Agent。2024 年的 Python 3 变更只替换了 shebang，当前源码仍包含大量 Python 2 `print` 语句、`ConfigParser` import，以及远程执行中的裸 `python` 命令，所以入口在 Python 3 下会直接语法错误或 import 失败。

即使完成语法迁移，其基础设施也已经失效：安装脚本下载 Docker 1.7，镜像引用 HDP 2.3 和 Ambari 2.1.1 的旧 Hortonworks 仓库，网络依赖旧版 Weave CLI，并要求管理员在 Dockerfile 中直接填写 root 密码和 SSH key。它不进入产品 assembly，仓库内也没有调用方、自动化测试或现行 deploy 文档引用。

本 worktree 已删除整个 `contrib/agent-simulator`，没有为它增加 Google Cloud SDK、Docker Python SDK、Paramiko 或新的网络库。当前本地 deploy 和预创建镜像覆盖本轮集成验证目标；若将来确实需要大规模 Agent 压测，应基于现行 deploy/container 编排重新建立独立工具，而不是迁移这套 2015 年实现。

同一原则适用于 `custom_actions/scripts/remove_bits.py`：它只为 HDP 2.1 Express Upgrade 删除旧包，当前 BIGTOP stack 没有调用方、metainfo 入口或部署契约；对应的 `TestRemoveBits.py` 也只验证该历史 HDP 包列表。本 worktree 已删除二者，避免把不可执行的 HDP 迁移逻辑继续装入 Server 包。

### 6.15 其余 Python 的升级边界

扫描 Agent、共享库、Server Python utilities 和直接相关测试工具后，没有发现其他应作为官方 package 引入的整包第三方源码。剩余工作主要是升级 Ambari 自有代码使用的标准库 API：

- 将 `ssl.wrap_socket`、`PROTOCOL_SSLv23` 和分散的 TLS 参数收敛到显式 `SSLContext`；这是正确性和安全修复，不需要引入 requests 或维护新的 HTTP/TLS fork。
- 将 `optparse` 逐入口迁移到 `argparse`。不要一次性改写 `ambari-server` 的全部 CLI；参数解析行为是用户接口，应按命令建立 focused tests 后迁移。
- 将仍然执行裸 `python` 的 Linux 脚本统一到受控 wrapper 或已解析的 Python 解释器；不能依赖发行版上 `python` symlink 的偶然指向。
- 根 `setup.py`、Agent `setup.py` 和不完整的 `pyproject.toml` 应收敛为一套 PEP 517 build metadata。setuptools 属于构建依赖，不应进入 Agent runtime。
- `ambari_commons.import_utils`、`resource_management`、`ASTChecker`、repository managers 和 Agent listeners 都承载 Ambari 协议或运维语义，应继续由 Ambari 维护；可以缩小和现代化实现，但不应为了减少源码行数换成通用框架。
- `yaml_utils.py` 实际是 Ambari 配置值转义 helper，不是 vendored YAML parser。除非确认外部 management pack 无消费者，否则不能因引入 PyYAML 而顺手删除它。

`contrib` 下其他 management pack、旧功能测试和开发脚本在基线中也有 Python 2 import 或陈旧 CLI；它们虽然不属于 Agent runtime，但仍在本轮文档核对范围内。基线确认的 backlog 包括：

- `contrib/nagios-alerts`、`contrib/nagios-snmp` 以及 `contrib/addons` 下的 Nagios
  插件和 RPM/DEB 打包脚本只服务于已移除的 HDP Nagios 集成，没有 BIGTOP stack
  入口、调用方或当前部署契约。本 worktree 已删除这些目录和打包引用；主升级兼容
  列表、主机检查过滤和 React 操作文案中的 Nagios 旧分支也已移除。HDP/FAKE
  测试夹具和 Ember classic 历史代码不属于当前 BIGTOP 运行/打包路径，保留以避免
  改变通用兼容测试和已排除的前端范围。
- HDF management pack 的 NiFi service check 使用 `urllib2`/`httplib`，部分 hooks 使用 Python 2 `print`。
- ONEFS 的 `hdfs_to_onefs_convert.py` 是 Python 2 CLI，并使用 `urllib2`/`optparse`。
- `contrib/utils/perf/deploy-gce-perf-cluster.py` 是面向 CentOS 7 的 Python 2 GCE 工具。
- `contrib/version-builder`、`contrib/utils/preinstall-check` 和 `dev-support/docker` 使用 `optparse`。

Nagios 旧集成已经删除，不再迁移或测试其无调用方的 CLI；HDF NiFi service check、ONEFS、version-builder、preinstall 和 Docker Linux 路径仍使用 `urllib.request`/`urllib.error`/`http.client` 或 `argparse`，失效的 GCE perf 工具已删除。窄扫描未再发现产品运行路径中的 Python 2 import、旧语法、裸 Python shebang 或 `optparse`；HDF 6/6、ONEFS 14/14、preinstall 7/7、version-builder 2/2 focused tests 通过。本项源码已关闭，贡献目录删除按独立所有权边界提交/JIRA。

## 7. 测试和静态检查基础设施

本节 7.1、7.2 的问题描述是初始基线。当前四套 runner、收集下限、零收集失败、timeout 和 Ruff 固定基线的代码证据见第 13 节。

### 7.1 自定义 runner 的问题

`ambari-agent/src/test/python/unitTests.py:77-144`：

- 递归扫描文件并把目录追加到 `sys.path`。
- 按 basename 导入测试模块，容易发生同名冲突。
- 没有测试隔离、随机顺序、per-test timeout 或 collected count guard。
- 多个并发测试使用 `time.sleep` 和无上限轮询，容易产生慢测试和偶发失败。

建议渐进迁移到标准 `unittest discover` 或 pytest。第一步不是替换所有测试框架，而是保证现有测试被准确收集、可按模块运行并有超时。

### 7.2 Ruff 配置

`pyproject.toml:77-154` 虽然选择了 `F/E/B/TRY`，随后又忽略了 `F821`、`F4`、`F8`、`E4`、`E7` 和大部分异常处理规则，当前静态检查无法有效阻止 undefined name、错误 import 和异常吞噬。

`Jenkinsfile:58-72` 每次安装未固定版本的最新 Ruff，可能在没有代码变化时造成 CI 行为漂移。

建议：

- 固定 Ruff 版本。
- 先启用会发现真实错误的规则，例如 `F821/F822/F823/F524/F601/F632`。
- 对历史问题建立临时 per-file ignore，不要永久全局关闭整个规则族。
- 格式化和 import sorting 的大规模机械修改单独提交。

## 8. 三层实施边界

### 第一层：稳定控制链

建议独立 JIRA/PR：

1. 恢复七个核心测试类的收集，并增加 collected-count guard。
2. 修复 CommandStatus ACK、超大报告和 retry/persistence 语义。
3. 重构 ActionQueue 为有界 worker、per-task cancellation 和确定性 shutdown。
4. 建立统一 TLS trust policy 和 enrollment 兼容迁移。
5. 原子化 FileCache/ClusterCache，并收紧 JCEKS、环境密钥和 remote debug。

### 第二层：统一运行基础

本 worktree 已实施的基础项，最终应拆为可独立审阅的 topic commits：

1. 统一 Python >= 3.9.2 元数据、安装器、包依赖、wrapper 和 CI；Rocky 8 显式依赖 `python39`，Rocky 9 使用系统 Python 3.9。
2. 删除孤儿 `_posixsubprocess.so`，并对 tar/RPM/DEB 增加禁止 Python 2 ABI 二进制的产物检查。
3. 修正 Python build metadata，分离 runtime/test/tooling 依赖并建立包含传递依赖、Python marker 和 hash 的离线 lock。
4. 将 mock 迁移到 `unittest.mock`，删除 CoilMQ test broker 和错误的 runtime dependency。
5. 将 simplejson 迁移到标准 `json`。
6. 使用标准库 PBKDF2 和官方 `cryptography` 替换 vendored crypto 代码，保持 v1 wire format。
7. 锁定并离线打包 distro 1.9.0，删除系统 `python3-distro` 偶然依赖；确认 takeover 状态后升级或删除 PyYAML 依赖。
8. 删除无实际 API 消费的 `python-kerberos` dependency，并验证 AMS SPNEGO。
9. 改进 test discovery、timeout 和 Ruff 规则。
10. 清理 `ambari-server-state` 和旧 Agent Simulator；确认支持状态后分别退休 pluggable stack、takeover 和 metrics export 历史工具。

### 第三层：迁移核心依赖

本 worktree 已实施的依赖迁移，最终应按依赖边界拆为可独立审阅的 topic commits：

1. 删除 ws4py 和 Ambari stomp fork，迁移到官方 stomp.py 8.x `WSStompConnection`。
2. 删除 APScheduler vendored fork，迁移到官方 APScheduler 3.11.x。
3. 删除 `ambari_jinja2` vendored fork，通过 Ambari 模板 facade 使用官方 Jinja2 3.1.x + MarkupSafe。
4. 通过 Ambari `Properties` facade 使用官方 javaproperties，删除复制的 ActiveState parser。
5. 在兼容解密落地后迁移 Server/Agent 加密协议 v2。

## 9. 明确禁止捆绑的变更

- TLS 默认策略与 stomp/ws 实现替换不要放在同一个 PR。
- ActionQueue 并发重构与 CommandStatus ACK 修复不要放在同一个 PR。
- Python 版本基线与 Jinja2 迁移不要放在同一个 PR。
- APScheduler 和 Jinja2 不要放在同一个依赖升级 PR。
- AES-GCM 协议升级与 JCEKS 文件权限修复不要放在同一个 PR。
- 测试恢复与 64 个文件的 mock 机械迁移不要压成一个不可审阅提交。
- CoilMQ 测试重写与 stomp.py 生产控制通道迁移不要放在同一个提交；先建立不依赖旧 broker 的 contract tests。
- Server properties parser 迁移不要与 Agent runtime dependency 清理放在同一个 PR。
- distro/Python floor 决策不要与 PyYAML takeover 行为修改混在一个兼容性变更中。

## 10. 打包建议

Agent 需要支持无外网集群安装，因此不建议在节点运行时执行 `pip install`。

推荐方案：

- 在构建阶段维护明确的 agent dependency manifest、版本和 hash。
- Agent 和 Server 每次 bundling 前删除 `target/python-dependencies`，避免增量构建残留已经从 lock 删除或换版本的 package。
- 对纯 Python 依赖，在 Maven/package 阶段安装到私有 lib 目录或打包经过校验的 wheel。
- 对 `cryptography` 一类原生依赖，明确支持的 Linux 发行版、CPU 架构和 wheel/系统包来源。
- 发布构建的主 lock 显式使用 `manylinux2014_x86_64`、Python 3.9.2 兼容性解析和 `cp39` ABI，不允许 pip 根据构建机选择 platform/Python ABI，并拒绝所有源码分发包；已锁定的纯 Python docopt sdist 在不带跨平台参数的第二阶段独立安装；Rocky 8 RPM 显式依赖 `python39`，Rocky 9 使用系统 Python 3.9。
- 产物检查必须确认实际 wrapper 与依赖目录属于同一 Python ABI，并确认 cryptography/cffi/MarkupSafe/PyYAML 原生文件均为 Linux x86_64，不包含 macOS、arm64 或构建机 Python ABI。
- Server 的 takeover 工具通过已安装的 `/usr/lib/ambari-server/lib` 加载 PyYAML；节点运行时不提示也不执行全局 `pip install`。
- 升级清理清单保留已删除的 `ambari_jinja2`、`ambari_simplejson`、`ambari_stomp`、`ambari_ws4py`、`ambari_pbkdf2`、`ambari_pyaes` 名称，只用于删除旧版本遗留目录，不代表它们仍是运行依赖。RPM/DEB 由包管理器按旧文件清单移除，install helper 另以显式安全清单兜底；tar 升级根据归档实际拥有的 Agent/Server `lib` 根先删除旧目录再解压，避免覆盖安装保留已删除源码，同时完整保留新 wheel 自带的 tests/docs/examples。
- 删除 `python3-rpm` runtime dependency，两处已安装包匹配改用系统 `rpm` CLI，避免把默认 Python 3.6 的扩展模块加载到 Python 3.9。
- 不依赖任意 `/usr/bin/python` 的全局 site-packages。
- 生成依赖清单并同步更新 LICENSE/NOTICE，避免继续复制无版本来源的第三方源码。
- 对构建产物扫描 ELF/PYD 扩展及其 Python ABI，禁止再次把解释器私有扩展或未知来源二进制打进包。
- 生成 runtime/test/tooling 三类依赖图，发布产物中不得出现 CoilMQ、mock 或未声明的测试发行版；锁定官方 wheel 自带的 tests/docs/examples 允许保留，并继续接受 RECORD 所有权、hash、LICENSE 和 SBOM 审计。
- 对每个 Python floor 独立生成 lock 并执行 metadata consistency 检查；禁止 resolver 在发布构建中临时选择 tzlocal、MarkupSafe、cffi 等传递依赖版本。
- 只解析和安装 APScheduler/Jinja2/cryptography 的 base runtime closure，不启用 optional jobstore、test 或 protocol extras；官方 wheel 已包含的 docs/examples/tests 不再二次裁剪。

## 11. 验证策略

遵循 focused tests，不运行全部 Maven 测试，也不运行 Ember。

每个变更的最低验证顺序：

1. 静态检查：目标 Python 文件 `compileall`、固定版本 Ruff 和必要的 AST/import 检查。
2. focused unit tests：只执行受影响的 Agent 测试文件。
3. Python matrix：最低支持版本和最新支持版本各执行一次 focused tests。
4. 本地编译容器：执行修改模块的 Maven test mask，不跑整个工程。
5. 产物审计：列出 tar/RPM/DEB 中的 Python packages、原生扩展和依赖 SBOM，确认没有未声明测试发行版或 Python 2 ABI 文件；官方 wheel 自带的 tests/docs/examples 允许存在。
6. deploy 集群：使用预创建镜像验证 Agent 注册、命令执行、取消、重连、告警和模板渲染。

示例 focused test 入口，实际参数应按对应 PR 调整：

```bash
mvn -pl ambari-agent -Dpython.test.mask=TestCommandStatusDict test
mvn -pl ambari-agent -Dpython.test.mask=TestActionQueue test
mvn -pl ambari-agent -Dpython.test.mask=TestNetUtil test
mvn -pl ambari-agent -Dpython.test.mask=TestAlertSchedulerHandler test
```

控制通道集成测试至少覆盖：

- 首次注册和已有 Agent 重连。
- Server 重启和短时网络中断。
- 多条命令并发、取消和 Agent restart。
- 命令状态报告分片与乱序 ACK。
- 错误 CA、错误 hostname 和正确双向 TLS。

Jinja2 集成测试至少比较代表性 stack templates 的旧/新生成结果，并在 deploy 后确认核心组件能启动和通过 service check。

## 12. Review 决策结论

以下结论是本轮实现和验收边界，不再作为无结论问题保留。部署结果若推翻某项前提，必须先修正实现和本节结论再提交 PR。

1. **Python floor 和发行版映射**：最低版本固定为 CPython 3.9.2。RPM 主产物为 cp39；Rocky/Red Hat 8 显式依赖 `python39`，Rocky/Red Hat 9 使用提供 `python(abi) = 3.9` 的系统 `python3`。其他 RPM 发行版使用 `python(abi) = 3.9` capability 约束而不是猜测私有包名。Ubuntu 22 Bigtop 产物单独使用 cp310 和 `python3 (>= 3.10), python3 (<< 3.11)`；aarch64 按 `build.os_arch` 生成独立 manylinux2014 产物。启动入口从包内 `.cpython-XY-*.so` 推导唯一 ABI 并拒绝解释器 minor 不匹配。
2. **Server CA 来源**：不采用 TOFU 或运行时下载 fingerprint。安装/自动 bootstrap 流程从 Server 已生成的 `security.server.cert_name` CA 预置 Agent `/var/lib/ambari-agent/keys/ca.crt`；手工安装由管理员预置同一信任锚。CA 缺失时 Agent fail closed。Server identity 使用部署 hostname 的 SAN；PKCS#12 导出为 `-passin` 和 `-passout` 使用两个独立的 0600 口令文件来源，临时输出口令副本在成功和失败路径都删除，兼容 OpenSSL 1.1.1 对同一 `file:` 顺序读取两行的语义。
3. **不安全 TLS 开关**：不保留兼容发布周期。`ssl_verify_cert` 已从产品配置和分支删除，HTTPS/WSS 始终要求 CA 和 hostname 验证。管理员独立工具 `configs.py --unsafe` 是显式单次 CLI 行为，不会降低 Agent 控制通道。
4. **离线依赖获取**：Agent/Common/Server、Agent sdist、build 和 tooling 分别使用精确 pin 与 SHA-256 lock。主运行依赖只接受指定 platform/Python/ABI 的 binary wheel；docopt 0.6.2 是唯一 sdist 例外，使用锁定 build backend、`--no-build-isolation`、`--use-pep517`、`--no-deps` 生成带 `WHEEL/RECORD` 的标准 wheel 元数据。`python.wheelhouse` 激活 `PIP_NO_INDEX`；aarch64/cp310 分开构建和审计，不在节点运行 pip。
5. **Agent stop/restart 语义**：选择取消而不是等待或配置化。停止时拒绝新任务，设置每任务 cancel event，取消排队和运行任务，终止完整进程组，等待 worker/background callback 完成后退出；旧 generation 的迟到结果不得覆盖新任务。
6. **AES v2 兼容窗口**：Agent registration 通过 `encryptionTypes` 声明 `aes256_gcm` capability；Server 仅向包含该值的 Agent 发送 AES-GCM v2，旧 Agent、空列表和未知 capability 继续接收 v1；新 Agent 同时读取 v1/v2。`HeartbeatControllerTest` 固定注册时的 capability 更新、配置缓存失效、empty/unknown/unchanged fallback 和注册失败路径。v1 读取和发送保留到受支持旧 Agent 窗口结束，删除 v1 必须另开 JIRA 并提升最低 Agent capability，不能在本 PR 静默移除。
7. **Jinja2 兼容目标**：代表性 HDFS、YARN、HBase、Hive、Kafka、Ranger、Kerberos、HA 模板以逐字输出一致为验收目标，不接受未评审的规范化差异。文件模板使用 `StrictUndefined`；`InlineTemplate` 保留历史 Undefined 行为，并显式提供 `unicode=str` 兼容变量。
8. **takeover config merge**：本发布继续作为受支持管理员工具保留，迁移到私有 PyYAML、`safe_load()` 和现代 XML API。其退休必须单独 JIRA、迁移说明和消费者公告，本轮不删除。
9. **`yaml_utils` 边界**：按外部 management pack 可使用的兼容 facade 保留；只允许 safe parser，不删除现有函数名。仓内 focused tests 固定其数组解析和转义行为。
10. **`ambari.properties` 兼容边界**：接受 javaproperties facade 的既有 sorted write/header 语义；保证转义、续行、Unicode、重复 key 和写失败行为，不承诺逐字保存注释或原始顺序。
11. **pluggable stack 与 `replaceBaseUrl`**：保留。Agent、Server、Admin/Web 的 Maven profile 仍有真实消费者，`replaceBaseUrl` 也有显式 Maven execution；只迁移已删除 Python/XML API并加 focused tests。
12. **`export_ams_metrics.py`**：删除。仓库内无入口、调用方、打包引用或测试；仓库外若要恢复必须新建 JIRA 和受支持入口，不能把死代码留作推测兼容。
13. **docopt**：接受短期锁定 0.6.2，作为 stomp.py 8.2.0 的硬依赖，不阻塞本轮。其 sdist hash、锁定 build backend、metadata/RECORD/license 闭包均进入产物门禁；迁出 docopt 或等待上游修改单独处理。
14. **旧 Agent Simulator**：删除。2015 年 simulator 无受支持入口且不再代表当前 STOMP/TLS/加密协议；现行 deploy 场景和预创建镜像承担集成验证。需要模拟器时应基于当前协议另开 JIRA 重建。
15. **官方 wheel 的非运行内容**：允许锁定官方发行制品自带的 tests/docs/examples 随离线依赖进入 RPM/DEB，不为裁剪这些目录修改安装树或 `RECORD`；收益不足以抵消维护和完整性成本。唯一允许的规范化例外是删除 Ambari 不调用的 `console_scripts`/`gui_scripts` 入口及依赖根目录的 `bin`，并由 `normalize_python_dependencies.py` 原子改写对应 `RECORD`；脚本必须由 `entry_points.txt` 和 `RECORD` 唯一声明，遇到未声明、缺失或多 owner 内容时 fail closed。其余文件仍必须由已验 SHA-256 的发行制品提供、由规范化后的 `RECORD` 唯一拥有，并纳入 LICENSE/SBOM。该结论不允许把第三方源码重新 vendoring 到 Ambari 仓库，也不允许增加 test extra 或测试发行版。
16. **资源归档摘要和升级顺序**：选择 Server-first。新 Server 先发布并经 WSS 下发每个 archive 的可信 SHA-256，旧 Agent 忽略额外 metadata；随后升级 Agent。新 Agent 连接旧 Server 时，仅在资源 URL 使用已验证 CA/hostname 的 HTTPS 时允许无 manifest 兼容，并把实际下载 archive 的 SHA-256 写入本地 marker 约束容错回退；HTTP 无可信摘要必须 fail closed。反向的 Agent-first HTTP 升级不受支持，不能用不可信 `.hash`、TOFU 或关闭 TLS 验证绕过。

## 13. 实施核对清单

本节是第 4 至第 12 节的交付检查表。状态只使用以下四种取值：

- `已完成且有代码证据`：产品代码、调用方、删除项、打包引用和 focused tests 均已核对。
- `实现不完整`：已有部分实现，但调用方、失败路径、打包或测试仍有缺口。
- `尚未实现`：风险或行为仍保留在产品代码中。
- `需要独立提交/JIRA/PR`：本轮仍需完成，但必须按第 8、9 节边界独立交付。

当前状态以代码核对为准，不继承提交信息或本文前文的“已实施”声明。构建、部署和
产物状态只能在源码项全部关闭后更新。

| 来源 | 检查项 | 当前状态 | 代码证据或待关闭缺口 |
| --- | --- | --- | --- |
| 4.1 | HTTPS/WSS 统一验证 Server CA 和 hostname | 已完成且有代码证据 | `AmbariConfig.py`/`security.py`/`inet_utils.py` 共用 `CERT_REQUIRED`、hostname 和最低 TLS 1.2；Server 生成 hostname SAN identity、独立 PKCS#12/truststore 并清理临时口令，bootstrap/deploy 预置 CA；`TestTlsVerification.py` 和 `CertGenerationTest` 覆盖可信、错误 CA、错误 hostname、过期证书、mTLS、keystore 内容和临时文件清理，旧 flag 不能降级 |
| 4.2 | Command status 分片 ACK、乱序/重复 ACK、超大单条报告 | 已完成且有代码证据 | `CommandStatusDict.py` 以 batch/revision/correlation 绑定 callback，失败时注销，ACK 幂等并压缩旧 revision；第二遍源码审查进一步发现 supersede 后注销 callback 的迟到 ACK 会被误当成同步响应永久放入 `BlockingDictionary`，现由 `HeartbeatThread.blocking_request()` 在 presend 时显式登记同步 correlation，只有这些响应可入队并在成功、发送失败或 timeout 时清理，迟到异步响应直接丢弃；`TestCommandStatusDict.py` 17/17、`TestAgentStompResponses.py` 10/10、`TestRegistration.py` 4/4 覆盖乱序、重复、stale、迟到响应、同步响应、timeout 清理、发送失败、UTF-8 byte size 和截断 |
| 4.3 | ActionQueue 有界并发、per-task cancel、确定性 shutdown | 已完成且有代码证据 | `ActionQueue.py` 使用有界 worker slot 和每 generation cancel event；第二遍源码审查发现并修复同步执行计数重复增加但只减少一次，以及普通 SIGTERM 的 `interrupt()` 未在 `join()` 前先广播取消、可能等待同步命令自然结束的问题；`interrupt()` 现从调用线程先设置任务 cancel event 并由 `CustomServiceOrchestrator.py`/`PythonExecutor.py` 终止进程组，再唤醒队列；测试覆盖计数成功/异常回收、同步命令阻塞时的停止、队列取消、并发上限、迟到结果和后台 timeout，`TestActionQueue.py` 30/30 通过 |
| 4.4 | 七个核心测试类恢复收集并设置 collected-count guard | 已完成且有代码证据 | 七个类均继承 `TestCase`；Agent runner 输出 collected count、对七模块设下限并在零收集时失败 |
| 4.5 | FileCache/ClusterCache 原子更新、可信摘要和失败回退 | 已完成且有代码证据 | `resourceFilesKeeper.py` 对相对 POSIX 路径、mode、size 和内容做 framed SHA-256，源在归档期间变化则保留旧 archive 并失败；同文件系统原子发布 archive、`.hash` 和 digest manifest。Java `ResourceManager` 复核 manifest key/digest/archive，`MetadataHolder` 运行时刷新并经可信 WSS command metadata 下发，`ActionScheduler` 总以当前 manifest 覆盖 stage/command 旧值，非法或陈旧 manifest 阻止派发；`FileCache.py` 在解包前验证该 SHA-256，HTTP 无可信摘要 fail closed，HTTPS 重定向到 HTTP 也 fail closed，按 generation 协调并发 waiter，并以 staging/backup/`os.replace` rollback；HTTPS old Server compatibility 和 Server-first 升级边界已明确。`ClusterCache.py` 原子写 JSON/hash commit marker。`TestFileCache.py` 28/28、`TestResourceFilesKeeper.py` 13/13、ActionScheduler 注入 3/3 通过，覆盖 rename/chmod、归档竞态、不同 generation waiter、缺失/错误摘要、disabled update、协议降级和严格 fallback |
| 4.6 | JCEKS stdin 协议、权限、密钥单次环境注入和日志脱敏 | 已完成且有代码证据 | Java `CredentialStoreCreate` 只接受 `create <alias> -provider <path> [-f]`，通过 4-byte 长度前缀 UTF-8 stdin 读取最多 1 MiB 凭据并在成功、截断、尾随和 malformed 路径清零 buffer，拒绝 `-value`；Common helper 统一调用，Agent、Ranger、Ranger KMS 四条产品链已迁移，产品残留扫描为零。helper jar 进入现有 Agent `cred/lib`，没有新增第三方 runtime/license；本地 JCEKS 更新使用同目录持久 lock、staging、`fsync`、删除旧 Hadoop CRC sidecar 后 `os.replace`，并发更新不丢 alias，失败保留可读旧 store。Agent 整库重建对最终路径持锁，文件位于 `0750` service group 目录并发布为 `0640`。所有产品调用使用 `ambari_java_home` 运行 JDK 17 helper，组件 Java Home 不参与；Java 17 生成格式仍可由 Java 8 Hadoop 读取。`AGENT_ENCRYPTION_KEY` 只进入需要解密的单个 command env；命令日志和 status transport 脱敏。真实 Hadoop 3.3.4 Java 4/4、Agent orchestrator 16/16、Common 7/7、argv safety 2/2 通过；标准 Maven assembly 归第 11 节最终构建验证 |
| 4.7 | 删除生产远程调试代码执行接口 | 已完成且有代码证据 | `RemoteDebugUtils.py`、`debug.py`、SIGUSR2/FIFO/pickle 入口和打包引用均已删除；全仓文件/引用扫描为零 |
| 4.8 | 删除默认 `DEV` enrollment passphrase | 已完成且有代码证据 | Agent/Server env 不再回退 `DEV`；Server 生成高熵 secret，Linux bootstrap 通过预置 `0600` 文件传递并一次消费，环境随即清理；`TestBootstrap.py`/`TestSetupAgent.py` 覆盖传递、消费和失败路径，`test_ambari_server_launcher.py` 直接覆盖随机生成、显式环境覆盖、`0600` 持久化及 openssl/mkdir/mktemp/sed/chmod/mv 失败 |
| 5.1-5.2 | Python >= 3.9.2 解释器、RPM、wrapper 和 wheel ABI 契约 | 已完成且有代码证据 | pyproject/setup/wrapper/RPM/Bigtop 一致；本次交付按用户范围只生成 Linux x86_64 RPM，不构建 DEB；cp39、cp310、x86_64、aarch64 的兼容 profile 和 staged artifact 门禁仍保留；安装和启动脚本从原生扩展推导唯一 ABI 并拒绝 minor 不匹配；tar/custom-root tests 覆盖缺失和混合 ABI |
| 5.3 | Python 3.12/3.13 已删除 API和启动阻塞 | 已完成且有代码证据 | Linux 产品路径已迁移 `urllib`、argparse、现代 XML/SSL/configparser/importlib API；`AttributeDictionary.__unicode__` 兼容入口改用 `str()`；窄扫描无 Python 2 import/语法、裸 Python shebang、`optparse` 或已删除 API；Windows 产品支持已由独立提交删除，不再作为扫描排除项 |
| 6.1 | vendored mock 删除并迁移 `unittest.mock` | 已完成且有代码证据 | vendored 目录和打包引用已删除，所有调用方改为 `unittest.mock`；旧 import 扫描为零 |
| 6.2 | simplejson 删除并迁移标准库 `json` | 已完成且有代码证据 | 产品 import、vendored 源码和打包引用已删除；cache restart、STOMP payload 和 command status tests 覆盖 Unicode、大整数、NaN、bytes 拒绝和排序无关语义 |
| 6.3a | PBKDF2/cryptography 保持 v1 wire format | 已完成且有代码证据 | 标准库 PBKDF2 和官方 cryptography AES-CBC 维持 Java/Python golden vector；错误 key、padding、malformed envelope 失败测试存在 |
| 6.3b | Server/Agent AES-GCM 协议 v2 和能力协商 | 已完成且有代码证据 | Java `AgentEncryptionCapabilities`/`AgentConfigUpdateEncryptor` 与 Python `encryption.py` 实现 v2 AES-GCM；registration 的 `aes256_gcm` capability 决定 v2/v1 发送，新 Agent 双读；加密 tests 覆盖 tamper、wrong key、unknown version 和 old Agent，`HeartbeatControllerTest` 覆盖 capability 更新、缓存失效、empty/unknown fallback 和注册失败 |
| 6.4 | 官方 APScheduler facade 和生命周期 | 已完成且有代码证据 | vendored fork和打包引用已删除；`AlertSchedulerHandler.py` 适配官方 3.11 API、显式 start/reschedule/remove/shutdown；测试覆盖 scheduler 失败和恢复 |
| 6.5 | 官方 stomp.py/WebSocket facade 和控制通道契约 | 已完成且有代码证据 | ws4py/Ambari stomp 源码已删；`AmbariStompConnection.py` 使用官方 `WSStompConnection`、TLS context、CONNECTED timeout、heartbeat 和 callback 清理；连接失败、重连、send/disconnect/listener tests 存在 |
| 6.6 | 官方 Jinja2 facade 和代表性模板兼容 corpus | 已完成且有代码证据 | vendored Jinja 已删；`source.py` facade 使用 Jinja2 3.1，文件/inline Undefined 边界明确；`TestTemplateCompatibility.py` 覆盖 HDFS/YARN/HBase/Hive/Kafka/Ranger/Kerberos/HA，provider tests 固定 XML/properties 输出和失败路径 |
| 6.7 | `_posixsubprocess.so`、Python 2 bytecode 和空 package 删除 | 已完成且有代码证据 | 源码、assembly、Bigtop spec 均无 `_posixsubprocess.so`；第三遍源码树扫描又发现并删除 AMBARI_INFRA_SOLR 中 10 个已有同名 `.py` 源码的 Python 2.7 `.pyo`。Agent/Server 所有产品 Python/resource fileSet 现显式排除 `__pycache__`、`.pyc`、`.pyo`，metadata gate 的 failure test 防止回归；artifact audit 同时拒绝 `.pyc`/`.pyo`、私有扩展、Python 2 ABI、非 Linux 和错误 architecture |
| 6.8 | CoilMQ 删除、runtime dependency 清理和测试替代 | 已完成且有代码证据 | broker 源码、lock/requirements/Maven 引用已删；listener 和 fake official connection contract tests 替代旧 broker |
| 6.9 | 官方 javaproperties facade | 已完成且有代码证据 | `ambari_server/properties.py` 委托 javaproperties；测试覆盖 escaping、continuation、Unicode、duplicate、sorted write、write failure 和 stream close；该 facade 不承诺原子写入 |
| 6.10 | distro、PyYAML 和 python-kerberos 契约 | 已完成且有代码证据 | distro/PyYAML 进入 Server 私有 lock 和产物闭包，takeover 使用 `safe_load`；deploy 删除系统 distro 偶然依赖；python-kerberos 无 requirements/import/包引用，SPNEGO 使用现有 curl/GSSAPI 路径和失败测试 |
| 6.11 | PEP 517 元数据及 runtime/test/tooling 分组 | 已完成且有代码证据 | 根/Agent pyproject 均有固定 build backend 和 `requires-python >=3.9.2`；Common/Agent/Server/build/tooling/sdist 分 manifest/lock；`check_python_dependency_metadata.py` 比对 setup/pyproject/lock/Maven execution，`test_python_sdist.py` 构建并检查根与 Agent sdist 的资源、旧 vendored 排除和 PKG-INFO |
| 6.12-6.13 | 源码所有权和官方传递依赖闭包 | 已完成且有代码证据 | vendored 第三方源码为零；artifact audit 校验精确 distribution set、manifest 可达闭包、marker、WHEEL/RECORD、ELF、license 和 SBOM，失败 tests 覆盖悬空/多 owner/错误 hash/ABI/架构；官方 wheel 自带 tests/docs/examples 允许保留，仅按第 12.15 项删除无调用方 entry-point scripts 并规范化 RECORD |
| 6.14 | 无入口历史工具删除或安全保留 | 已完成且有代码证据 | metrics export、server-state、GCE perf、Agent Simulator 以及只被旧 GCE perf 调用且缺少配置/打包入口的 `agent-multiplier.py` 无调用方后删除；takeover、yaml_utils、pluggable stack、replaceBaseUrl 因真实入口/外部兼容边界保留并测试；第 12 节给出明确结论 |
| 6.15 | 剩余标准库 API、裸 Python、optparse 和列明 contrib backlog | 已完成且有代码证据 | 过时 Nagios 集成及其 contrib/打包代码已删除；HDF/ONEFS/preinstall/version-builder/Docker Linux 已迁移，GCE perf 已删除，产品运行路径旧 API/import/shebang/optparse 窄扫描为零；HDF NiFi service check、ONEFS、preinstall、version-builder focused tests 实测通过 |
| 7.1 | 标准 discovery、collected count 和 per-test timeout | 已完成且有代码证据 | Agent/Server/HDF/ONEFS runner 使用 `unittest` discovery，显式输出收集/执行数、零收集失败、SIGALRM per-test timeout；Server 子进程另有 terminate/kill 总 timeout |
| 7.2 | 固定 Ruff 并启用真实错误规则 | 已完成且有代码证据 | `requirements-tooling.txt` 固定 `ruff==0.12.11` 和 hash；Ruff target py39，启用 E9/F524/F601/F632/F821/F822/F823 且无全局 ignore，`contrib` 不再被 exclude；Jenkins hash 安装并运行 metadata gate |
| 8 | 三层实现边界全部关闭 | 已完成且有代码证据 | 第一层控制链、第二层 Python 运行/打包基础和第三层官方依赖迁移均已完成；BIGTOP 16 个 service 的 PID/UID/argv、超时进程组、凭据失败闭合、权限和 stack 版本继承也已逐项核对并由 service contract tests 固定 |
| 9 | 禁止捆绑项按边界拆分 | 已完成且有代码证据 | Windows 删除已由独立提交 `1956740ced` 交付；其余测试恢复、ACK、ActionQueue、TLS/enrollment、cache/JCEKS、Python packaging、官方依赖、协议 v2、历史工具、BIGTOP service 和证据文档均已按边界完成并提交，行为与 focused tests 同提交。拆分提交满足审查边界，不再作为延期项 |
| 10 | 离线 lock、清洁 bundling、ABI/SBOM/LICENSE 产物检查 | 已完成且有代码证据 | Maven 先清理，再以 hash/no-deps 安装 locked build/runtime/sdist，normalize RECORD 后审计并组装；产品 source fileSet 也禁止生成 bytecode，源码树的 Python 2 `.pyo` 已删除；wheelhouse 支持 no-index；本次交付只验证和发布 RPM，DEB 不在用户要求范围内，不进行 DEB 构建；RPM/Bigtop 架构/ABI和文档一致，LICENSE/NOTICE/SBOM 纳入 RPM。tar 升级在解压前替换归档拥有的完整私有 `lib` 根，install helper 另以明确历史名称兜底，测试证明旧 vendored/删除入口消失且官方 dependency 的 docs/examples 保留。第二遍独立审计的 cp310 x86_64 及 cp39/cp310 aarch64 Agent/Server staged artifacts、metadata、normalize、sdist 和 Bigtop ABI/staged audit 全部通过且无 high/medium finding；最终 RPM 执行证据仍归第 11 节 |
| 11 | 静态、focused matrix、容器、RPM 产物和 deploy 验证入口 | 已完成且有代码证据 | 四套 runner、Ruff/metadata/artifact audit、源码 provenance、RPM/SBOM 门禁和 deploy enrollment/runtime import checks 均已通过；最终 `ambari-26643-final-v5` RPM 构建完成，Agent/Server Python runner 分别 629/629、1391/1391，deploy 全量 438/438，最终三节点部署 13 个 phase 全部 `completed` 且状态 `applied`。本次交付只构建和验证 Linux x86_64 RPM，不构建 DEB 或容器镜像 |
| 12 | 16 个 Review 决策形成明确结论 | 已完成且有代码证据 | 第 12 节已逐项选择 floor/CA/TLS/offline/shutdown/v2/Jinja/takeover/yaml/properties/profiles/历史工具/docopt/simulator/官方 wheel 非运行内容和资源归档升级边界，无未决问题 |

BIGTOP service 深度审计矩阵如下。每项均已核对产品实现、所有调用方、旧名称/旧入口/死代码、打包与配置引用以及失败路径测试源码；最终执行结果仍以第 11 项的真实验证记录为准。

| Service | 当前状态 | 已确认代码证据或待关闭缺口 |
| --- | --- | --- |
| ALLUXIO | 已完成且有代码证据 | 使用 BIGTOP native library 路径，PID/UID/argv 身份、启动 timeout、metastore fallback 和配置文件权限均已收紧；无 HDP service 路径残留 |
| FLINK | 已完成且有代码证据 | 1.15/1.19/1.20 的 historyserver/config contract 已统一，legacy YAML 清理、log/config 权限、启动 timeout 和失败回滚均已覆盖 |
| HBASE | 已完成且有代码证据 | shared tmp 保留 sticky bit；master/regionserver PID 身份、TERM/KILL 进程组、权限和 decommission/upgrade 失败路径已闭合 |
| HDFS | 已完成且有代码证据 | rolling restart、JournalNode、NameNode backup/finalize/marker、DataNode shutdown 异常失败闭合、共享 tmp sticky bit 和 PID 身份均已修复并测试 |
| HIVE | 已完成且有代码证据 | HiveServer2/Metastore/WebHCat/MySQL helper/schematool/service check 均使用安全 argv 或 0600 临时属性，密码不进 argv，schema/凭据失败路径显式失败 |
| KAFKA | 已完成且有代码证据 | BIGTOP service 名称、PID/UID/argv 身份、broker stop 的 TERM/KILL 进程组、topic service-check 清理和超时均已统一 |
| KERBEROS | 已完成且有代码证据 | keytab/principal/cache/权限验证及 kinit 失败路径逐文件核对，调用方使用私有 cache 和显式 timeout，无遗留旧入口 |
| LIVY | 已完成且有代码证据 | stop/status 改用安全 PID 身份和进程组终止，配置/端口校验、Kerberos cache、失败回滚和 service-check 覆盖完成 |
| RANGER | 已完成且有代码证据 | JCEKS/DB verifier 使用长度前缀 stdin，管理员/数据库密码不进 argv，bootstrap 轮换失败闭合、权限和 service 名称已统一 |
| RANGER_KMS | 已完成且有代码证据 | repository HTTP、audit HDFS 目录、XML/JCEKS 权限、PID 身份、service/advisor 名称和 shell 参数边界均已修复并有失败测试 |
| SOLR | 已完成且有代码证据 | PID shell 注入和不受控信号已移除，安全身份/TERM/KILL、expect/HTTP service check、权限及失败路径均已覆盖 |
| SPARK | 已完成且有代码证据 | advisor 精确匹配、history/thrift launcher、PID 身份、配置文件和启动失败回滚已统一，3.2/3.5 版本 contract 已固定 |
| TEZ | 已完成且有代码证据 | 配置严格校验、临时文件清理、命令 timeout/进程组、权限和 service-check 失败聚合已完成 |
| YARN | 已完成且有代码证据 | advisor 使用当前 spark-env，ResourceManager/NodeManager/Timeline/HBase archive 调用链的 PID、权限、超时、原子发布和失败回滚已闭合 |
| ZEPPELIN | 已完成且有代码证据 | metadata、Spark/Hive/Livy 配置、stack root 和 Bigtop service contract 已对齐；有兼容和空端口 focused tests；解释器 ID `spark2`/`livy2` 按外部持久兼容保留 |
| ZOOKEEPER | 已完成且有代码证据 | 3.5/3.7/3.8 start-foreground contract、PID/UID/argv 身份、status/start/stop 失败路径、配置权限和进程组终止均已统一 |

源码完成门禁结论：第 4 至第 12 节的产品实现、调用方迁移、删除项、依赖/打包闭包、focused tests、最终构建、部署和三遍复查均已关闭。第 9 节的提交拆分已实际交付，不再存在 `需要独立提交/JIRA/PR` 的未完成项。

### 验证执行记录

以下记录只在命令真实执行成功后更新，不由代码存在性推断：

- 第一次完整编译：`ambari-audit-first-full-20260901-005` 成功；Rocky 8 x86_64 builder profile `rocky-8-bigtop-v3` 使用 Python 3.9.25，Agent/Server Python artifact audit、Maven assembly 和 RPM 构建通过；Agent RPM SHA-256 `ce17ac5f3a688f902b459a36bd016a5238a9b2b40f31b426f478fd89ba4a05e0`，Server RPM SHA-256 `7ab5714c65cdf633c89e5ae6462df25e5b84189c19e5bbd3d85901fccf4f2ce0`。本次构建后的第 15 项决策变更由 focused test 和最终完整构建重新验证。
- 容器内增量替换和集群行为验证：第一次完整编译产物已完成一轮。预装镜像 plan `r8-min-preload-py39-20260901-001` 使用 runtime profile `universal-v3`，镜像 ID `sha256:4b387f2e97b11616c67be2fe800390811ee79d5a05c5d638f0044195bb625bc5`，独立 image verify 通过；镜像包含 Python 3.9、锁定的 Ansible Core/collections 和预装 Bigtop 组件，不预装 Ambari。部署 plan `52a57ab012af57c6ec0e68ba8b887ab372379f96274e3236ba7243bf7c9015a2` 精确挂载第一次完整编译的 005 Agent/Server RPM，三节点 `minimal8g-admin-v3` 的 13 个 phase 全部完成，最终状态 `applied`；Server 和三台 Agent 均为 active，`ambari-python-wrap` 实际为 Python 3.9.25，产品私有 native 依赖导入通过，HDFS/YARN/Hive/HBase blueprint 请求成功完成。第二遍源码审查后的 ActionQueue 等修复已由最终 v4 RPM 部署再次复验。
- 增量验证发现并修复两项不能留给最终构建的问题：deploy 的 native import 探针缺少产品私有 `PYTHONPATH`，现已分别固定为 `/usr/lib/ambari-agent/lib` 和 `/usr/lib/ambari-server/lib`；OpenSSL 1.1.1 对 PKCS#12 的同一输入/输出 `file:` 口令源会读取两行，`CertificateManager` 现使用一次性 0600 输出口令副本并在所有路径清理。后者通过 JDK 17 单 class/JAR 增量替换、最终 v4 RPM 构建和三节点重新部署验证 keystore/truststore、Server 启动和 Agent enrollment。另修复早期初始化失败时 `AmbariServer.stop()` 因 Jetty 尚未创建而抛异常、阻止 `System.exit` 的失败清理路径。
- Windows 支持删除：独立提交 `1956740ced`。对原 Linux/Python 删除范围做父/子 AST 对比，没有出现父版本可解析而新提交新增语法错误；变更 shell/launcher 的 `bash -n`、XML/POM 和 JSON 解析、`git diff --check` 均通过。Ember 按明确排除不处理。
- 当前静态与工具门禁：锁定环境执行全仓 `ruff check --no-cache .`、836 个 Python 文件隔离 `py_compile`、dependency metadata gate、`git diff --check`、改动 shell 语法检查以及 XML/JSON 解析检查均通过，且 `contrib` 已纳入 Ruff。按 `requirements-build.lock` 建立的环境执行 dev-support 40/40，通过 root/Agent sdist、launcher、credential argv safety、install cleanup、metadata、normalizer、artifact audit 和产品 source fileSet bytecode exclusion tests。
- 当前 focused/full tests：HDF 6/6、ONEFS 14/14、preinstall 7/7、version-builder 2/2、deploy 438/438 通过；过时 Nagios CLI 测试已随源码删除，不再纳入测试矩阵。资源归档补丁的 `TestFileCache.py` 28/28、`TestResourceFilesKeeper.py` 13/13 和 ActionScheduler 注入 3/3 通过；JCEKS stdin/原子提交补丁以真实 Hadoop 3.3.4 执行 Java 4/4、Agent orchestrator 16/16、Common 7/7 和 argv safety 2/2 通过。tar upgrade 12/12、install-helper cleanup 3/3、artifact audit 19/19 通过。最新正式 runner 从头执行 Agent 629/629、Server 1391/1391，均为 0 error/0 failure；日志分别为 `/tmp/ambari-agent-python-full-final.log` 和 `/tmp/ambari-server-python-full-final.log`。
- 当前集群增量验证：三节点 Agent 均从 `/usr/bin/ambari-python-wrap` 使用 Python 3.9 并保持 active；Server 当前 manifest 的 21 个 `archive.zip` 全部独立重算 SHA-256 一致，每台 Agent 的 8 个 `.archive.sha256` marker 均与该 manifest 对应。Agent 重启触发 9 个真实 Hive/HCat 组件 store 重建，全部为 `0640`、无 `.jceks.crc`，Java 8 Hadoop 可读；三台节点分别并发写入两个随机凭据 alias，均保留 2/2、Java 8 可读且无 checksum sidecar，证明 JDK 17 helper、锁和原子提交契约。最终 v4 RPM 已通过删除旧容器、挂载本地 RPM repository、启动新容器并完成 blueprint 的方式重新部署。
- 当前 Java focused matrix：独立本地 build 容器、JDK 17、离线 Maven cache 下重新编译当前 Agent/Server Java 源码；`CredentialStoreCreateTest` 3/3，Server 的 `TestResources`、Metadata holder/cluster、HeartbeatProcessor 和 ManagementController matrix 共 193 个实际执行、20 个既有条件 skip，均为 0 failure/0 error、`BUILD SUCCESS`。日志为 `/tmp/ambari-java-credential-focused-20260901.log` 和 `/tmp/ambari-java-server-focused-matrix-20260901.log`；测试未在集群容器运行。
- 第二遍 packaging 审计：cp310 x86_64 的 Agent 为 12 distributions/3 native extensions、Server 为 9/4；aarch64 cp39 和 cp310 的 Agent 均为 12/3、Server 均为 9/4，全部通过。metadata 9/9、artifact audit 与 normalize 22/22、sdist 2/2、Bigtop ABI 3/3 和 staged audit 3/3 通过，未发现 high/medium finding；这些 staged 证据已由下方 v4 RPM 完整构建和部署复验。
- 当前源码缺口：无已识别的实现不完整或尚未实现项。DataNode shutdown 的异常失败闭合和 Server Python runner 的临时目录嵌套问题均已修复；当前 worktree 的产品代码、调用方、vendored/打包引用、lock/Maven/RPM/Licence/Notice 和 focused failure paths 已核对。
- 最终完整构建和 RPM/SBOM 审计：已完成。`deploy/.ambari-build/rocky8-x86_64/build-state/runs/ambari-26643-final-v5/manifest.json` 显示 Agent RPM SHA-256 `cd2ff2a60406bdd0c18ad5dc89c01d41dc5c79c7a5dba6a480b192571dc5b916`、Server RPM SHA-256 `0489693dedf2686af3545ce4b872c072cd23346e5549628c4ff9ba94c5bb9a60`；构建输入由 `downloads/ambari-source.json` 绑定到提交 `5072cf186b4adc361edabb040e12dbd5e3afdb0b`，归档 SHA-256 为 `976225f6b7e4735dd67e56f5391445137b3a14926127cdc7afe2416a4a50607c`。两个 RPM 均含 `/usr/share/doc/*/LICENSE.txt`、`NOTICE.txt` 和 `python-dependencies.sbom.json`，并声明 Python 3.9 ABI 依赖。本次不构建 DEB 或容器镜像。
- 最终部署：已完成。`minimal8g-admin-v3` 的最新 deployment state 为 `applied`，13 个 phase 全部成功；HDFS、YARN、HBase、Hive、MapReduce2、Tez、ZooKeeper 服务状态与期望状态一致，三台节点使用 Python 3.9.25，Java 17 基础服务与 Hive Java 8 兼容运行均已现场核验。部署使用 v5 RPM 更新本地离线仓库后删除旧容器并启动新容器，未重建预装镜像。
- 完成后三遍潜在问题复查：已完成。第一遍源码/静态审计通过 `git diff --check`、XML/JSON/POM 解析、旧名称/import/bytecode 扫描及 focused failure paths；第二遍产物审计重新计算源码归档、RPM、SBOM、LICENSE/NOTICE、ABI 和 RPM repository metadata；第三遍运行审计重启并检查三节点容器、50 个 host components 无 desired-state mismatch、7 个核心服务、Python/Java runtime、关键端口及 server/agent 日志，未发现 high/medium 问题。期间发现并修复 XML `&` 未转义和 runner 临时目录嵌套，修复后重新构建、部署和复验。
