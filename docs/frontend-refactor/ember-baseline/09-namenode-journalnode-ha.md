# NameNode HA 与 JournalNode 管理基线

本文是 React 重构中 Enable NameNode HA 和 Manage JournalNodes 的经典 Ember 行为基准。范围包括入口、权限、九步/七步状态机、人工命令、Kerberos 分支、配置迁移、后台 request、错误/Retry、关闭/恢复和 rollback。NameNode Federation/RBF、ResourceManager/Ranger/HAWQ HA 分别见相邻模块文档。

## 范围、证据与 Metrics 边界

| ID | 基线事实 | React 验收要求 | 主要证据 |
| --- | --- | --- | --- |
| NNHA-SCOPE-001 | 主流程 route 为 `/highAvailability/NameNode/enable`，内部固定 Step 1 到 Step 9；modal route 名为 `main.services.enableHighAvailability` | URL、modal、step 导航和恢复不能被压缩为一次普通配置提交 | `ambari-web/classic/app/routes/high_availability_routes.js`、`nameNode/wizard_controller.js` |
| NNHA-SCOPE-002 | JN 管理 route 为 `/highAvailability/JournalNode/manage`，内部代码固定 Step 1 到 Step 7；纯删除隐藏 Step 3/5，用户看到重新编号后的五步 | React 必须同时覆盖增、删、增删混合和纯删除，不得只实现 Add JournalNode | `ambari-web/classic/app/routes/manage_journalnode_routes.js`、`journalNode/wizard.hbs` |
| NNHA-SCOPE-003 | 本模块排除 Metrics 产品页面、图表、widget 和安装 helper 的全局 metrics refresh；保留 checkpoint/JN formatted 安全门槛中的 `metrics/dfs/...`，因为字段直接决定 Next | 不应因字段名含 `metrics` 删除三条 checkpoint GET，也不应把无关指标 refresh 当产品契约 | `nameNode/step4_controller.js`、`nameNode/step6_controller.js`、`journalNode/step3_controller.js` |
| NNHA-SCOPE-004 | AMS 只保留 HA 配置迁移：若 `ams-hbase-site/hbase.rootdir` 包含当前 NN host，则改成 nameservice；AMS 已安装时 Step 9 无论该值是否匹配都会重提交完整 `ams-hbase-site` snapshot。除此之外 AMS/Metrics 功能均 `OUT_OF_SCOPE` | 仅实现/验证此配置副作用与配置版本提交，不迁移 AMS 页面或指标接口 | `app/utils/configs/nn_ha_config_initializer.js#_initHbaseRootDirForAMS`、`nameNode/step9_controller.js#reconfigureAMS` |
| NNHA-SCOPE-005 | 权威证据为 route/controller/view/template、五层网络清单、通用 progress/install mixin 和旧 Karma tests；静态代码与实际可达性冲突时本文显式标为 `STATIC_ONLY`、`PLACEHOLDER` 或 `NEEDS_RUNTIME_VALIDATION` | React gap 评审必须逐 ID 对照，不得以“存在同名 controller”或启发式模块索引中的命中/缺席推断功能可用 | `generated/ajax-endpoints.md`、`generated/ajax-calls.md`、`generated/direct-http-calls.md`、`generated/browser-network-entrypoints.md`、`generated/realtime-channels.md`、本文件测试章节 |

## Enable NameNode HA 入口与前置条件

| ID | 入口/行为 | 权限、条件与边界 | 请求/结果 | 主要证据 |
| --- | --- | --- | --- | --- |
| NNHA-ENTRY-001 | HDFS Service Actions 在未启用 HA 时显示 Enable NameNode HA，调用统一 HA controller 后进入九步 modal | HDFS stack service types 含 `HA_MODE`、有 master/slave、外层任一 service-action 权限可见且具体项要求 `SERVICE.ENABLE_HA`；单节点、无 NN 或 NN 未安装时 disabled | 通过前置检查后转 `main.services.enableHighAvailability` | `app/views/main/service/item.js#observeMaintenanceOnce`、`app/models/host_component.js#TOGGLE_NN_HA`、`app/templates/main/service/item.hbs` |
| NNHA-ENTRY-002 | HA 已启用后 `TOGGLE_NN_HA` 的 action/label 虽计算为 Disable，但 `isHidden=App.isHaEnabled` | 经典 UI 实际没有可用 Disable NameNode HA 菜单；不得把死代码当成已支持入口 | 无 | `app/models/host_component.js#TOGGLE_NN_HA` |
| NNHA-ENTRY-003 | Stack upgrade 的 Secondary NameNode custom check 可显示 Enable 按钮并复用同一前置检查 | 这是升级检查中的第二入口；最终仍进入同一向导 | 同主入口 | `app/views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_snn.js` |
| NNHA-ENTRY-004 | 进入前对找到的第一个 NameNode 检查 `workStatus === STARTED` | 有 NN 但未启动时把原因加入同一 error popup并阻止路由；完全没有 NN 时直接对 `undefined.get(...)` 求值并抛异常，不会形成聚合错误。正常菜单会因 NN 缺失而 disabled，但直接调用 controller 不会优雅失败 | 无 | `app/controllers/main/admin/highAvailability_controller.js#enableHighAvailability` |
| NNHA-ENTRY-005 | 进入前要求至少三个 ZooKeeper Server component | 只按模型数量检查，不校验 ZooKeeper `workStatus`；这是经典精确语义，不应误写成运行状态检查 | 无 | 同上 |
| NNHA-ENTRY-006 | 进入前要求至少三台注册主机 | `App.allHostNames.length < 3` 阻止 | 无 | 同上 |
| NNHA-ENTRY-007 | 任一 master component 显式 maintenance `passiveState=ON` 或 implied maintenance 时阻止 | 不限 HDFS master；所有前置错误合并为 `<br/>` popup | 无 | 同上 |
| NNHA-ENTRY-008 | 直接访问 route 时 route 本身没有重新执行权限与上述业务前置检查 | 菜单 gate 不能替代服务端授权；React route guard 应显式验证，经典行为属于安全缺口 | route 直接加载向导数据 | `app/routes/high_availability_routes.js#enter` |

## Enable NameNode HA 九步状态机

### Step 1 Get Started

| ID | 用户行为与状态 | 校验/异常 | 请求/持久化 |
| --- | --- | --- | --- |
| NNHA-STEP1-001 | 阅读停机维护窗口、自动与人工步骤说明，并输入 Nameservice ID | 1 至 63 位，只允许字母、数字、连字符，首尾不能为连字符；无效时 Next disabled | `config.on_site` 读取 `hadoop-env/hdfs_user`；Next 保存 `nameServiceId` 并清旧 master assignment |
| NNHA-STEP1-002 | UI 强提示：若 HBase 正在运行应退出向导并先停止 HBase | 只是说明，不读取 HBase 运行状态、不强制阻止 Next | 无 |
| NNHA-STEP1-003 | 安装 HAWQ 时额外提示完成 HA 后必须人工更新 HAWQ filespace | 只按 HAWQ service 存在显示；向导结束前后均不验证人工结果 | 无 |

### Step 2 Select Hosts

| ID | 用户行为与状态 | 校验/分支 | 请求/持久化 |
| --- | --- | --- | --- |
| NNHA-STEP2-001 | 分配一台 Additional NameNode 和至少三台 JournalNode；现有 NameNode 标为 Current | 初始 `mastersToAdd` 为一个 NN 加三个 JN；JN 超过三个时出现 remove，未达到 stack/host 上限时最后一项出现 add | host inventory + Stack Advisor `recommend: host_groups`；Next 保存 master topology |
| NNHA-STEP2-002 | 同一种多实例 master 的 host 必须唯一，目标 host 必须存在 | 空 host、同 component 重复 host 无效；已安装组件所在 host 只有 maintenance `OFF` 才可保留 | 客户端 assignment 校验 |
| NNHA-STEP2-003 | JN 最大数由 stack component cardinality 与可用 host 数共同限制，可大于 3 | 不能把向导硬编码为“恰好三台”；最低值才是硬编码 3 | Stack metadata、assignment mixin |
| NNHA-STEP2-004 | Back 回 Step 1；Next 同时记录 Additional NN 和 SNN host 供 rollback 数据使用 | 更改拓扑后清空先前 Review configs，防止旧配置快照复用 | local DB `masterComponentHosts`、rollback host keys |

### Step 3 Review

| ID | 用户行为与状态 | 配置/异常 | 请求/持久化 |
| --- | --- | --- | --- |
| NNHA-STEP3-001 | Review 展示 Current NN、待删除 SNN、待安装 Additional NN 和全部待安装 JN | 配置未加载时 spinner；Back 可重选 hosts | `config.tags` 后 `admin.get.all_configurations` |
| NNHA-STEP3-002 | 只允许编辑 `hdfs-site/dfs.journalnode.edits.dir`；其余 HA 与依赖服务配置只读 | 所有生成 config 统一 `isOverridable=false`，仅该项 `isReconfigurable=true` | Next 把修改合并进完整 server config snapshot |
| NNHA-STEP3-003 | 按已安装服务加载 HBase、Accumulo、AMS、HAWQ、Ranger 相关当前 tags/configs | HBase、Accumulo、AMS、HAWQ 及 `ranger-env` 的基础 tag 被直接解引用：服务存在但 desired config 缺失时会抛异常；只有部分 Ranger plugin/audit site 有存在性 guard。React 增加缺项校验属于明确的健壮性改进 | 动态 `urlParams=(type=...&tag=...)\|...` |
| NNHA-STEP3-004 | Next 保存 `hdfs-site`、`core-site`，条件保存 `hbase-site`、`ranger-env` 原 tags，供后续提交/静态 rollback 使用 | 请求或配置初始化失败留在 Review并显示通用错误 | local DB config snapshot/tags |

### Step 4 Create Checkpoint

| ID | 用户行为与状态 | 安全门槛/异常 | 请求 |
| --- | --- | --- | --- |
| NNHA-STEP4-001 | 用户登录 Current NN，依次以 HDFS user 执行 `hdfs dfsadmin -safemode enter` 和 `hdfs dfsadmin -saveNamespace` | UI 每 1 秒轮询，不能代替人工命令 | `admin.high_availability.getNnCheckPointStatus` |
| NNHA-STEP4-002 | Next 只在 `Safemode` 非空且 `LastAppliedOrWrittenTxId - MostRecentCheckpointTxId <= 1` 时启用 | 字段来自 `metrics.dfs.namenode`，是必须保留的 HA 安全数据；已有足够新的 checkpoint 可提前满足 | GET 当前 NN host-component |
| NNHA-STEP4-003 | desired state 非 `STARTED` 时显示错误说明 | `isNameNodeStarted` 不参与 Next computed；若 checkpoint 条件满足仍可继续，这是经典不一致 | 同上 |
| NNHA-STEP4-004 | 点击已启用 Next 后先检查 KDC session，再进入自动变更阶段 | Manual Kerberos (`kdc_type=none`) 直接通过；自动 KDC 无效或服务端返回可识别的 KDC 400 时弹 credential popup，取消则停留本步。KDC 类型/session 请求的普通 HTTP 失败只有默认错误框，既不继续也不调用 cancel handler | Kerberos config/session/credential APIs |

### Step 5 Configure Components

| ID | 严格串行任务 | 精确副作用与异常 | 主要请求 |
| --- | --- | --- | --- |
| NNHA-STEP5-001 | 1 Stop All Services | 对 cluster services PUT `INSTALLED`，轮询后台 request；这是全停而非只停 HDFS | `common.services.update`、request polling |
| NNHA-STEP5-002 | 2 Install Additional NameNode | 先检查组件存在性，必要时创建 service-component、注册 host-component，再 PUT `INSTALLED`；每个 install 前检查 KDC session | component install chain |
| NNHA-STEP5-003 | 3 Install JournalNodes | 对 Step 2 所有 JN hosts 执行同一安装链，包括已有 host-component 的幂等检查 | component install chain |
| NNHA-STEP5-004 | 4 Reconfigure HDFS | 保存 `hdfs-site`、`core-site`，条件保存 Ranger HDFS audit/plugin；成功后在全部 NN/JN hosts 安装 `HDFS_CLIENT` 并保存 host 列表 | `common.service.configurations` + component install chain |
| NNHA-STEP5-005 | Kerberos 集群的重配置分支 | Agent 安装 JN 后可能注入安全属性，因此重新加载最新 tags/configs，再以 Review HA properties 覆盖合并，删除旧 NN keys后提交 | config GET chain + `common.service.configurations` |
| NNHA-STEP5-006 | 5 Start JournalNodes | 对全部选定 JN、maintenance `OFF` 的 host-components PUT `STARTED` 并轮询 | `common.host_components.update` |
| NNHA-STEP5-007 | 6 Disable Secondary NameNode | 实际不是 stop/delete，而是把 SNN host-component maintenance/passive state PUT 为 `ON` | `common.host.host_component.passive` |

### Step 6 Initialize JournalNodes

| ID | 用户行为与状态 | 安全门槛/异常 | 请求 |
| --- | --- | --- | --- |
| NNHA-STEP6-001 | 用户登录 Current NN，以 HDFS user 执行 `hdfs namenode -initializeSharedEdits` | UI 每 1 秒并发轮询所选 JN | `admin.high_availability.getJnCheckPointStatus` 每 host 一次 |
| NNHA-STEP6-002 | 对响应解析 `metrics.dfs.journalnode.journalsStatus`，要求当前 nameservice 的 `Formatted === "true"` | 无 metrics 时显示“所有 JN 应已启动”；不验证人工命令 exit code | 同上 |
| NNHA-STEP6-003 | 经典实现只在收到前三个响应时判定，并要求当次计数为 3 | 选择超过三台 JN 时可能在其余响应前提前成功，且结果受响应顺序影响；React 必须等待完整目标集合，这是旧版缺陷 | `nameNode/step6_controller.js#MINIMAL_JOURNALNODE_COUNT` |

### Step 7 Start Components

| ID | 严格串行任务 | 条件与副作用 | 主要请求 |
| --- | --- | --- | --- |
| NNHA-STEP7-001 | 1 Start ZooKeeper Servers | 对拓扑中的所有 ZK Server host-components PUT `STARTED` | `common.host_components.update` |
| NNHA-STEP7-002 | 2 条件 Start Ambari Infra | 仅 `AMBARI_INFRA_SOLR` model loaded 时保留任务，启动该 service | `common.services.update` |
| NNHA-STEP7-003 | 3 条件 Start MySQL Server | 仅 component model 存在且 installedCount > 0；以 service name `HIVE` 更新 MYSQL_SERVER hosts | `common.host_components.update` |
| NNHA-STEP7-004 | 4 条件 Start Ranger | 仅 RANGER_ADMIN model 存在且 installedCount > 0；使用 assignment 中 Ranger hosts | `common.host_components.update` |
| NNHA-STEP7-005 | 5 Start Current NameNode | 只启动原已安装 NN，为下一人工步骤提供 Active NN | `common.host_components.update` |

### Step 8 Initialize Metadata

| ID | 用户行为与状态 | 校验/异常 | 请求 |
| --- | --- | --- | --- |
| NNHA-STEP8-001 | 在 Current NN 执行 `hdfs zkfc -formatZK` | 页面不轮询或验证命令结果 | 无业务请求 |
| NNHA-STEP8-002 | 在 Additional NN 执行 `hdfs namenode -bootstrapStandby` | 页面不验证 host 输出或元数据完整性 | 无业务请求 |
| NNHA-STEP8-003 | 点击 Next 先检查 KDC session，再弹“已执行人工步骤”确认；确认后才进入 Step 9 | Manual Kerberos直接通过；关闭 invalid-KDC popup或取消人工确认均留在本步；KDC 类型/session 普通 HTTP 失败只显示默认错误框且不会进入确认 | Kerberos session/credential APIs |

### Step 9 Finalize HA Setup

| ID | 严格串行任务 | 条件与副作用 | 主要请求 |
| --- | --- | --- | --- |
| NNHA-STEP9-001 | 1 Start Additional NameNode | 对新 NN PUT `STARTED` 并轮询 | `common.host_components.update` |
| NNHA-STEP9-002 | 2 Install ZKFC；3 Start ZKFC | 在两台 NN hosts 创建/注册/安装 ZKFC，再 PUT `STARTED`；安装前检查 KDC | component install chain、`common.host_components.update` |
| NNHA-STEP9-003 | 4 条件 Install PXF | HAWQ/PXF 历史分支：PXF service 存在且新 NN host 尚无 PXF 才安装 | component install chain |
| NNHA-STEP9-004 | 5 条件 Reconfigure Ranger | 保存 `ranger-env`，并按已安装服务及属性实际存在性拆分提交 YARN、Storm、Kafka、Knox、Atlas、Hive、Ranger KMS 的 audit/plugin sites | `common.service.multiConfigurations` |
| NNHA-STEP9-005 | 6 条件 Reconfigure HBase | 保存 `hbase-site`，Ranger 存在时条件并入 HBase audit/plugin sites | `common.service.configurations` |
| NNHA-STEP9-006 | 7 条件 Reconfigure AMS | AMS 已安装就提交完整 `ams-hbase-site` snapshot；只有 `hbase.rootdir` 包含当前 NN host 时才改为 nameservice，否则原值也会随 snapshot 创建新 config version。其余 AMS/Metrics 产品能力排除 | `common.service.configurations` |
| NNHA-STEP9-007 | 8 条件 Reconfigure Accumulo | 保存 `accumulo-site` 的 volumes 迁移 | `common.service.configurations` |
| NNHA-STEP9-008 | 9 条件 Reconfigure HAWQ | 保存 `hawq-site` 与 `hdfs-client` 的 nameservice/NN 地址迁移 | `common.service.configurations` |
| NNHA-STEP9-009 | 10 Delete Secondary NameNode | DELETE SNN host-component；普通错误使 task FAILED，NoSuchResource 的通用删除容错未用于此直接调用 | `common.delete.host_component` |
| NNHA-STEP9-010 | 11 Stop HDFS；12 Start All Services | 先只把 HDFS PUT `INSTALLED`，再将所有 services PUT `STARTED`；不运行 smoke tests | `common.services.update`、request polling |
| NNHA-STEP9-011 | 完成 | HAWQ 存在时先展示人工 filespace 更新 alert；随后清 task/storage、status=`DEFAULT`，返回 Services | persist/status APIs |

## NameNode HA 配置契约

| ID | Site/属性 | 经典生成或迁移规则 | 边界 |
| --- | --- | --- | --- |
| NNHA-CONFIG-001 | `hdfs-site/dfs.nameservices`、`dfs.internal.nameservices` | 均写用户 Nameservice ID | 初始非 Federation HA 单 nameservice |
| NNHA-CONFIG-002 | `dfs.ha.namenodes.<ns>` | 写 `nn1,nn2`；failover provider 写 `org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider` | property 名中的占位符也替换为 `<ns>` |
| NNHA-CONFIG-003 | `dfs.namenode.rpc-address.<ns>.nn1/nn2` | nn1=Current NN + 现有无 namespace RPC port，缺省 8020；nn2=Additional NN + 8020 | host 与 port 的来源不对称 |
| NNHA-CONFIG-004 | `dfs.namenode.http-address.<ns>.nn1/nn2`、HTTPS 对应键 | nn1 复用现有 HTTP/HTTPS port，缺省 50070/50470；nn2 固定缺省 50070/50470 | 保留 stack/version 的端口差异验证 |
| NNHA-CONFIG-005 | `dfs.namenode.shared.edits.dir` | `qjournal://<jn1>:8485;<jn2>:8485;.../<ns>` | 使用全部选定 JN，不限三台 |
| NNHA-CONFIG-006 | `core-site/fs.defaultFS`、`ha.zookeeper.quorum` | 分别写 `hdfs://<ns>` 和所有 ZK hosts + 现有 `zoo.cfg/clientPort`，缺省 2181 | quorum 分隔符为逗号 |
| NNHA-CONFIG-007 | `dfs.ha.fencing.methods`、`dfs.ha.automatic-failover.enabled`、`dfs.namenode.safemode.threshold-pct` | 分别为 `shell(/bin/true)`、`true`、`0.99f` | Review 只读 |
| NNHA-CONFIG-008 | 删除旧 HDFS properties | 从提交 snapshot 删除 `dfs.namenode.secondary.http-address` 和无 namespace 的 NN RPC/HTTP/HTTPS 地址 | 删除发生于 Review初始化；secure reload 后再次删除 |
| NNHA-CONFIG-009 | `dfs.journalnode.edits.dir` | 默认 `/hadoop/hdfs/journal` 且唯一可编辑；Windows stack 从当前 `hdfs-site` 原值继承 | `isOverridable=false` 但 `isReconfigurable=true` |
| NNHA-CONFIG-010 | `hbase-site/hbase.rootdir` | 对原值执行 `/\/\/[^\/]*/` 替换，将任意现有 URI authority 改为 `<ns>` | 仅 HBase installed；经典代码不校验原 authority 是否为当前 NN host |
| NNHA-CONFIG-011 | `accumulo-site/instance.volumes` 与 `.replacements` | volumes authority 换 `<ns>`；replacements 写 `<oldValue> <newValue>` | 仅 Accumulo installed |
| NNHA-CONFIG-012 | `ams-hbase-site/hbase.rootdir` | 只有原值包含 `hdfs://<currentNN>` 时才换成 `<ns>` 并变为可见；AMS 已安装时 Step 9 始终提交完整 `ams-hbase-site` snapshot，不以该匹配结果决定是否提交 | 本模块唯一 AMS 契约；其余 Metrics 排除 |
| NNHA-CONFIG-013 | `hawq-site/hawq_dfs_url` | 将 URI 中 host:port 段替换为 nameservice；完成后仍要求人工更新 filespace | 仅历史 HAWQ service |
| NNHA-CONFIG-014 | `hdfs-client` | 写 `dfs.nameservices`、`dfs.ha.namenodes.<ns>`、两台 NN 的 RPC/HTTP 地址 | HAWQ 条件分支 |
| NNHA-CONFIG-015 | Ranger `xasecure.audit.destination.hdfs.dir` | 以 `ranger-env` 原值为基准，把 URI authority 换 `<ns>`；各 audit/plugin site 仅在服务已安装且原 property 存在时保存 | Step 5 处理 HDFS sites；Step 9 处理其余服务 sites |
| NNHA-CONFIG-016 | config 保存形状 | 单组为 `PUT /clusters/{cluster}` body `{Clusters:{desired_config:[...]}}`；Ranger 多组为同 URL、body 是多个 `{Clusters:{desired_config:[...]}}` 数组 | 每项含 `type`、`properties`、可选 `properties_attributes` 和 version note |

## Manage JournalNodes 入口与模式

| ID | 入口/模式 | 权限、条件与精确行为 | 主要证据 |
| --- | --- | --- | --- |
| JN-ENTRY-001 | HDFS Service Actions 显示 Manage JournalNodes | `supports.manageJournalNode`、HDFS `HA_MODE`、`App.isHaEnabled`，且 host 数大于 JN 数或 JN 数大于 3 | `app/views/main/service/item.js`、`app/models/host_component.js#MANAGE_JN` |
| JN-ENTRY-002 | Service Actions 外层 template 以 `RUN_CUSTOM_COMMAND/RUN_SERVICE_CHECK/START_STOP/TOGGLE_MAINTENANCE/ENABLE_HA` 的 OR 显示按钮；生成 Manage JN 选项的内层分支只接受除 `START_STOP` 外的另外四项 OR | Manage JN 自身没有单独要求 `SERVICE.ENABLE_HA`；只有 `SERVICE.START_STOP` 时可看到 Actions 和 start/stop，但不会生成 Manage JN。任一 `RUN_CUSTOM_COMMAND`、`RUN_SERVICE_CHECK`、`TOGGLE_MAINTENANCE` 或 `ENABLE_HA` 即可能生成该项 | `app/templates/main/service/item.hbs`、`app/views/main/service/item.js` |
| JN-ENTRY-003 | 服务入口点击后要求模型中同时存在 display label `Active NameNode` 和 `Standby NameNode` | 注释/文案称要求 started，但代码不检查 `workStatus`；失败显示 warning，不进入 route | `highAvailability_controller.js#manageJournalNode` |
| JN-ENTRY-004 | Host Details 的 Add JournalNode 先读取 Kerberos 类型并检查 KDC session，再确认进入向导；Manual Kerberos 跳过 session并在确认文案追加 warning。Delete JournalNode 不做 KDC 检查，直接确认进入同一向导 | 两者受 host component UI 的 `HOST.ADD_DELETE_COMPONENTS` 控制；均绕过 `manageJournalNode()` 的 Active/Standby 检查，也不把所点 host 预选 | `main/host/details.js#addComponentWithCheck/#addComponent/#deleteComponent`、host component template/view |
| JN-ENTRY-005 | 删除按钮在全局 JN count <= 3 时 disabled | 硬编码最低 3；组件通常还需处于允许删除的状态 | `app/views/main/host/details/host_component_view.js#isDeleteComponentDisabled` |
| JN-MODE-001 | Add-only | 至少增加一台、删除集合为空；执行完整七步 | `wizard_controller.js#getJournalNodesToAdd` |
| JN-MODE-002 | Delete-only | 增加集合为空、删除至少一台；跳过 Step 3 checkpoint 和 Step 5 人工复制，用户看到五步 | `isDeleteOnly`、route Step 2/4、wizard template |
| JN-MODE-003 | Mixed add/delete | 两集合均非空；执行完整七步，包括 checkpoint 和复制 | 同上 |
| JN-MODE-004 | No-op | host 集合与原集合相同则 Step 1 Next disabled | 排序后 host list 比较；不允许空变更进入 Review |

## Manage JournalNodes 七步状态机

### Step 1 Assign JournalNodes

| ID | 用户行为与状态 | 校验/异常 | 请求/持久化 |
| --- | --- | --- | --- |
| JN-STEP1-001 | 以当前 JN hosts 初始化 assignment，可增、删或换 host | 最终至少保留 3；同 component host 唯一、host/maintenance 校验沿用 master assignment | host inventory + Stack Advisor host-group recommendations |
| JN-STEP1-002 | 最大 JN 数为 `min(stack/host cardinality, existingCount * 2 - 1)` | 单次最多新增 `existingCount - 1`；例如 3 台最多变 5 台 | 客户端 cardinality |
| JN-STEP1-003 | Next 保存最终 master topology，后续通过与实时模型差集计算 add/delete hosts | Host Details 入口不会自动选中当前 host | local DB `masterComponentHosts` |

### Step 2 Review

| ID | 用户行为与状态 | 配置/分支 | 请求 |
| --- | --- | --- | --- |
| JN-STEP2-001 | Review 明列待安装与待删除 JN hosts，并展示只读 HDFS shared-edits 变化 | 配置加载完成前 Next disabled；可 Back 重选 | `config.tags`、`admin.get.all_configurations` |
| JN-STEP2-002 | 非 Federation 更新 `dfs.namenode.shared.edits.dir` | `qjournal://<最终JN hosts>:8485/<dfs.nameservices>` | config snapshot |
| JN-STEP2-003 | NameNode Federation 更新每个 `dfs.namenode.shared.edits.dir.<ns>` | `dfs.nameservices` 按逗号拆分，每个 namespace 使用同一最终 JN host 集合 | config snapshot |
| JN-STEP2-004 | Next 保存 config snapshot/tag/nameservice；纯删除直达 Step 4，其余进入 Step 3 | `moveJNConfig.configs` 是 controller 长生命周期数组，reload 未清空，可能重复追加同名配置，属于旧版缺陷 | local DB |

### Step 3 Save Namespace

| ID | 用户行为与状态 | 安全门槛/异常 | 请求 |
| --- | --- | --- | --- |
| JN-STEP3-001 | 单 namespace 在 Active NN 执行 safemode enter 和 saveNamespace | 命令与 NNHA Step 4 相同；纯删除不执行此步 | `admin.high_availability.getNnCheckPointsStatuses` |
| JN-STEP3-002 | 多 namespace 为每个 namespace显示 `-fs hdfs://<ns>` 的 safemode/saveNamespace 命令 | UI 首行仍只显示保存的 Active NN host，但命令针对各 logical URI | 同上 |
| JN-STEP3-003 | 多 namespace 选择每组检查 host：先使用 Active NN 模型；缺 Active 时优先该组 `STARTED` NN，否则第一台 | 这是容错选择，不代表所选 NN 真为 Active | HDFS master component groups/model |
| JN-STEP3-004 | 所有返回项均需 `Safemode` 非空且 txid checkpoint 差 <= 1 才启用 Next | desired state 非 STARTED 只显示错误；请求每 1 秒轮询 | checkpoint metrics GET |
| JN-STEP3-005 | 响应 item 数没有与 namespace 数做等量校验 | 服务端若只返回满足条件的子集，`every()` 可提前通过；React 必须验证目标 namespace/host 完整集合 | `journalNode/step3_controller.js#checkNnCheckPointStatus` |

### Step 4 Add/Remove JournalNodes

| ID | 严格串行任务 | 精确副作用与异常 | 主要请求 |
| --- | --- | --- | --- |
| JN-STEP4-001 | 1 Stop Standby NameNode | 对保存的 Standby NN PUT `INSTALLED` | `common.host_components.update` |
| JN-STEP4-002 | 2 Stop Services | Stop All Services，cluster services PUT `INSTALLED` | `common.services.update` |
| JN-STEP4-003 | 3 Add JournalNodes | add 集合为空则立即 completed；否则对新 hosts 执行 component install chain并检查 KDC | component install chain |
| JN-STEP4-004 | 4 Delete JournalNodes | delete 集合为空则立即 completed；否则逐 host DELETE；NoSuchResource 按完成 | `common.delete.host_component` |
| JN-STEP4-005 | 删除多台 JN 的聚合缺陷 | 每个 DELETE success 都直接 `onTaskCompleted`，第一台成功即可推进 Reconfigure，其他删除仍在飞行；React 必须等待全集终态 | `journalNode/step4_controller.js#deleteJournalNodes`、通用 `deleteComponent` |
| JN-STEP4-006 | 5 Reconfigure HDFS | 保存最终 `hdfs-site` shared-edits 配置，然后在剩余所有 NN/JN hosts 安装 `HDFS_CLIENT` | `common.service.configurations` + component install chain |

### Step 5 Copy JournalNode Directories

| ID | 用户行为与状态 | 校验/异常 | 请求 |
| --- | --- | --- | --- |
| JN-STEP5-001 | 从任一现有 JN host 打包 Journal directories，复制到所有新 JN并在相同位置解压 | 单 namespace显示 `dfs.journalnode.edits.dir`；Federation 对每个 namespace 取 `dfs.journalnode.edits.dir.<ns>` 后去重 | 无业务请求 |
| JN-STEP5-002 | 用户点击 Next 表示人工完成 | UI 不验证 tarball、权限、owner、checksum 或目标目录；纯删除跳过 | 无 |

### Step 6 Start JournalNodes 与 Step 7 Start All Services

| ID | 步骤/行为 | 精确副作用与异常 | 请求 |
| --- | --- | --- | --- |
| JN-STEP6-001 | Step 6 从当前 `App.HostComponent` model 读取 JN hosts并 PUT `STARTED` | 模型可能尚未反映新增/删除，可能漏新 JN或尝试启动已删除 JN；`NEEDS_RUNTIME_VALIDATION` | `common.host_components.update` |
| JN-STEP7-001 | Step 7 将全部 services PUT `STARTED`，不运行 smoke tests | 完成后清 tasks/storage、status=`DEFAULT`，回 Services并刷新 model | `common.services.update`、persist |

## 通用进度、错误、日志与 Kerberos

| ID | 行为 | 精确语义 |
| --- | --- | --- |
| NNHA-PROGRESS-001 | 严格串行 | 每个 progress page 的 `commands` 生成 PENDING tasks；只有当前任务 COMPLETED 且持久化成功后才 QUEUE/运行下一个，前序失败不继续 |
| NNHA-PROGRESS-002 | request 轮询 | mutation 返回 `Requests.id` 后保存到 task/current request IDs；每 4 秒 GET request，直到无 `PENDING/QUEUED/IN_PROGRESS` server task |
| NNHA-PROGRESS-003 | 终态聚合 | 任一 server task 为 `FAILED`、`TIMEDOUT`、`ABORTED`，wizard task 标 FAILED；否则 COMPLETED |
| NNHA-PROGRESS-004 | Retry | NNHA/JN 正常 progress 为 command-level Retry，只重置首个 FAILED command为 PENDING并重跑；不撤销此前已完成副作用 |
| NNHA-PROGRESS-005 | Skip | 通用 mixin 有 `onTaskErrorWithSkip`，但 NNHA/JN 正常任务均未调用，因此没有可用 Skip/Ignore and Proceed；不得从通用按钮代码推断支持 |
| NNHA-PROGRESS-006 | host/task 详情 | 点击已有 request ID 的 IN_PROGRESS/FAILED/COMPLETED task 打开 popup；按 request/stage 聚合 hosts，展开单 task 后轮询 `stdout`、`stderr`、`output_log`、`error_log` |
| NNHA-PROGRESS-007 | install KDC gate | 每个 component install 在 create/register/install 之前调用 `getKDCSessionState`；自动 KDC 的 invalid-KDC popup取消时当前 task FAILED，Manual Kerberos直接 callback。`getSecurityType` 或 `kerberos.session.state` 遇到非 KDC 普通 HTTP 失败没有业务 error callback，只弹默认错误框，既不 continue/cancel，也不调用 `onTaskError`，安装 task可停在 `QUEUED` |
| NNHA-PROGRESS-008 | install chain 容错缺陷 | service-component create helper 的 `.always()` 会在创建失败后继续；host-component register 的 success/error 都指向 `onCreateComponent`，随后仍发 Install；React 应停止并呈现原始错误，旧行为不是幂等保证 |
| NNHA-PROGRESS-009 | mixed topology refresh | 安装 helper 先 direct GET component state/topology，再触发共享 `updateServiceMetric`；只把 topology/state/maintenance/HA 运维字段视为基线，指标数值和全局 metrics refresh 排除 |
| NNHA-PROGRESS-010 | component install wire chain | 正常链严格为 `host_component.installed.on_hosts` -> 必要时 `common.create_component` -> 必要时 `wizard.step8.register_host_to_component` -> `common.host_components.update` 将目标设为 `INSTALLED`；`host.host_component.add_new_component` 不在 NNHA/JN 链中；迁移不得以 Host Details 的单组件安装接口替换该批量链 |
| NNHA-PROGRESS-011 | invalid-KDC credential 保存 | `credentials.get` 先探测 alias，再对同一 `/clusters/{cluster}/credentials/{alias}` POST create 或 PUT update，body 均是 `{Credential: resource}`；helper 即使 create/update 失败也 resolve，调用者随后仍重放原请求；popup 的 persisted checkbox 依赖 cluster model 已加载的 `Clusters.credential_store_properties['storage.persistent']`，不是每次弹窗新调 `credentials.store.info` |

## 关闭、持久化与恢复

| ID | 场景 | 经典行为与 React 验收 |
| --- | --- | --- |
| NNHA-RECOVERY-001 | NNHA snapshot | 每次 step 保存 current step、assignment、configs；progress 另存 task statuses、每 task request IDs、current request IDs到 local DB，并经 `/persist` 写 `CLUSTER_CURRENT_STATUS`；owner 使用 `wizard-data` |
| NNHA-RECOVERY-002 | NNHA 刷新恢复 | `HIGH_AVAILABILITY_DEPLOY` 在 valid states，enter 恢复 current step；IN_PROGRESS 继续 request polling，QUEUED 重新调用 command；`controller_route.js` 可按 owner 导回 route |
| NNHA-RECOVERY-003 | NNHA Step 1-4 关闭 | 直接清 tasks/storage、cluster state=`DEFAULT`，返回 Services；尚未进入服务端变更阶段，无 rollback request |
| NNHA-RECOVERY-004 | `autoRollbackHA=false` 且 Step > 4 关闭 | 弹人工回退警告；确认只清本地/持久状态并返回，不发送反向操作。用户必须自行恢复可能已停止的集群 |
| NNHA-RECOVERY-005 | `autoRollbackHA=true` | Step 5/7/9 隐藏 close；Step 6/8 关闭时重置主向导 Step 1并进入注册的三步 rollback route；这不是自动执行完整反向任务 |
| JN-RECOVERY-001 | JN snapshot | controller同样保存 current step、assignment/configs和 progress task/request IDs，cluster state 写 `JOURNALNODE_MANAGEMENT` |
| JN-RECOVERY-002 | JN 关闭 | 任意步骤，包括 Stop All/删除/重配置之后，关闭都无关键阶段警告、无 rollback，直接清状态、`DEFAULT`、返回并刷新 |
| JN-RECOVERY-003 | JN 恢复不可靠 | route enter 和 `controller_route.js` 有 JN 分支，但 `JOURNALNODE_MANAGEMENT` 不在 `cluster_states.validStates`，server localdb 特殊恢复只识别 NNHA/Kerberos；跨刷新/跨用户恢复为 `NEEDS_RUNTIME_VALIDATION` |
| NNHA-RECOVERY-006 | persist 权限 | cluster status/owner 依赖 `CLUSTER.MANAGE_USER_PERSISTED_DATA`，入口只显式 gate service 权限；缺 persist 权限时 `postUserPref` 立即 reject，而任务只在 persist success callback中 queue/run，因此首个或下一个 PENDING task确定不会推进。React 必须在进入前统一校验流程与恢复权限 |

## Rollback 与 Disable 的真实可达性

| ID | 实现 | 真实行为/缺陷 | 等级 |
| --- | --- | --- | --- |
| NNHA-ROLLBACK-001 | 注册 route `/highAvailability/NameNode/rollbackHA`，route 名 `main.services.rollbackHighAvailability` | Step 1 选择/显示 Additional NN 与 SNN hosts；Step 2 提示在 Active NN checkpoint并轮询；Step 3 完成 | `PLACEHOLDER` |
| NNHA-ROLLBACK-002 | 注册 rollback Step 2 | 继承 checkpoint controller但 route 的 Next 不绑定 `isNextEnabled`，用户可不等待 checkpoint直接继续 | `PLACEHOLDER` |
| NNHA-ROLLBACK-003 | 注册 rollback Step 3 | 空页面；Next 只清 storage、设 `DEFAULT`、reload，未 stop/delete/reconfigure/start任何组件或服务 | `PLACEHOLDER` |
| NNHA-ROLLBACK-004 | `disableHighAvailability()` | 跳转 `main.admin.rollbackHighAvailability`，而已注册 route 是 `main.services.rollbackHighAvailability`；且 HA 启用后菜单本身隐藏 | `STATIC_ONLY` 错误跳转 |
| NNHA-ROLLBACK-005 | 单体 `HighAvailabilityRollbackController` | 静态定义按失败 command裁剪的 15 个反向任务：停服务、恢复依赖/HDFS configs、停删 ZKFC/PXF/NN/JN、恢复 SNN、启动服务，并支持 Retry/Skip | `STATIC_ONLY`，未找到注册 route/outlet |
| NNHA-ROLLBACK-006 | 单体 controller 自身导航/状态 | 调用不存在的 `main.admin.highAvailabilityRollback`，写不在 valid states 的 `HIGH_AVAILABILITY_ROLLBACK`；正常主向导 progress 的 rollback action也跳同一不存在 route | `STATIC_ONLY` |
| NNHA-ROLLBACK-007 | 单体 controller 接口/类型错误 | 调用未注册 `admin.high_availability.load_accumulo_configs`、`.load_hawq_configs`；PXF/Additional NN 对象上误用 `mapProperty`，多处计数/host 类型不一致 | `STATIC_ONLY`，不能作为 React payload 基准 |
| NNHA-ROLLBACK-008 | 产品基线结论 | 当前经典树没有可信的“Disable NN HA”或自动 rollback 成功路径；React 若新增必须另立产品设计、服务端补偿策略和故障注入验收，不得声称是 Ember 等价迁移 | `NEEDS_RUNTIME_VALIDATION` |

## 后端接口契约

默认前缀为 `/api/v1`。表中 31 个 ID 覆盖两条正常向导的命名请求、component install direct GET、Kerberos credential链和持久化；注册/未接线 rollback 的额外静态接口不混入正常成功路径，另列末项。

| ID | App.ajax / Method | URL | 关键 query/body 与使用场景 |
| --- | --- | --- | --- |
| HA-API-001 | `hosts.confirmed` GET | `/clusters/{cluster}/hosts?fields=host_components/HostRoles/state&minimal_response=true` | wizard基础 hosts/topology加载 |
| HA-API-002 | `hosts.high_availability.wizard` GET | `/clusters/{cluster}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true` | assignment host详情 |
| HA-API-003 | `wizard.loadrecommendations` POST | `{stackVersionUrl}/recommendations` | `hosts`、`services`、`recommend:"host_groups"`、完整 blueprint/binding recommendations |
| HA-API-004 | `config.tags` GET | `/clusters/{cluster}?fields=Clusters/desired_configs` | Review/secure reload取 current tags |
| HA-API-005 | `admin.get.all_configurations` GET | `/clusters/{cluster}/configurations?{urlParams}` | OR query `(type=X&tag=Y)\|...` 读取精确版本 |
| HA-API-031 | `config.on_site` GET | `/clusters/{cluster}/configurations?{params}` | Step 1 通过 `configurationController.loadFromServer([{siteName:'hadoop-env'}])` 间接加载 `hadoop-env/hdfs_user`；JN 本地无缓存时走同一链 |
| HA-API-006 | `admin.high_availability.getNnCheckPointStatus` GET | `/clusters/{cluster}/hosts/{host}/host_components/NAMENODE` | 单 NN desired state + `metrics/dfs/namenode` checkpoint门槛 |
| HA-API-007 | `admin.high_availability.getNnCheckPointsStatuses` GET | `/clusters/{cluster}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in({hosts})&fields=HostRoles/desired_state,metrics/dfs/namenode&minimal_response=true` | JN管理单/多 namespace checkpoint集合 |
| HA-API-008 | `admin.high_availability.getJnCheckPointStatus` GET | `/clusters/{cluster}/hosts/{host}/host_components/JOURNALNODE?fields=metrics` | `journalsStatus[ns].Formatted`门槛 |
| HA-API-009 | `common.services.update` PUT | `/clusters/{cluster}/services?{urlParams}` | `{RequestInfo:{context,operation_level:CLUSTER},Body:{ServiceInfo:{state}}}`；stop/start all或选定 services |
| HA-API-010 | `common.host_components.update` PUT | `/clusters/{cluster}/host_components?{urlParams}` | RequestInfo query/context/operation level + `Body.HostRoles.state`；query排除 maintenance非 OFF |
| HA-API-011 | `common.host.host_component.update` PUT | `/clusters/{cluster}/hosts/{host}/host_components/{component}?{urlParams}` | 静态 rollback及通用单 component state mutation |
| HA-API-012 | `common.host.host_component.passive` PUT | `/clusters/{cluster}/hosts/{host}/host_components/{component}` | `{RequestInfo:{context},Body:{HostRoles:{maintenance_state}}}`；SNN maintenance ON |
| HA-API-013 | `common.delete.host_component` DELETE | `/clusters/{cluster}/hosts/{host}/host_components/{component}` | 删除 SNN/JN及静态 rollback资源 |
| HA-API-014 | `common.service.configurations` PUT | `/clusters/{cluster}` | `{Clusters:{desired_config:[{type,properties,properties_attributes?,service_config_version_note}]}}` |
| HA-API-015 | `common.service.multiConfigurations` PUT | `/clusters/{cluster}` | body 为多个 `{Clusters:{desired_config:[...]}}`；Ranger分组保存 |
| HA-API-016 | `host_component.installed.on_hosts` GET | `/clusters/{cluster}/host_components?HostRoles/component_name={component}&HostRoles/host_name.in({hosts})&fields=HostRoles/host_name&minimal_response=true` | install前存在性检查 |
| HA-API-017 | `common.create_component` POST | `/clusters/{cluster}/services?ServiceInfo/service_name={service}` | body `components[].ServiceComponentInfo.component_name`；确保 service-component |
| HA-API-018 | `wizard.step8.register_host_to_component` POST | `/clusters/{cluster}/hosts` | `RequestInfo.query=Hosts/host_name=...\|...` + `Body.host_components[].HostRoles.component_name` |
| HA-API-019 | direct `App.HttpClient` GET | `/clusters/{cluster}/components/?fields=ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true` | component create前 `updateComponentsState` 刷新 service/category/state counts 与 host topology；非 Metrics |
| HA-API-020 | direct `App.HttpClient` GET | `/clusters/{cluster}/components/?{FLUME/ATS/HA component predicates}ServiceComponentInfo/category.in(MASTER,CLIENT)&fields=ServiceComponentInfo/service_name,host_components/HostRoles/{display_name,host_name,public_host_name,state,maintenance_state,stale_configs,ha_state,desired_admin_state},{conditionalFields}&minimal_response=true` | `updateServiceMetric` 刷新 master/client topology/state/maintenance/stale/HA/desired-admin；条件字段中只保留 HDFS `ClusterId`、HBase `IsActiveMaster` 等运维选择字段，指标数值排除 |
| HA-API-021 | `background_operations.get_by_request` GET | `/clusters/{cluster}/requests/{requestId}?fields=*,tasks/...&minimal_response=true` | 每 4 秒进度聚合轮询 |
| HA-API-022 | `common.request.polling` GET | `/clusters/{cluster}/requests/{requestId}?fields=tasks/...&tasks/Tasks/stage_id={stageId}` | popup有 stageId 时按 stage轮询 |
| HA-API-023 | `background_operations.get_by_task` GET | `/clusters/{cluster}/requests/{requestId}/tasks/{taskId}` | stdout/stderr/output/error log详情 |
| HA-API-024 | `admin.security.cluster_configs.kerberos` GET | `/clusters/{cluster}/configurations/service_config_versions?service_name=KERBEROS&is_current=true` | controller未知 KDC type时读 `kerberos-env/kdc_type`；无业务 error callback，普通 HTTP 失败不会执行原 callback |
| HA-API-025 | `kerberos.session.state` GET | `/clusters/{cluster}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details` | 自动 KDC session gate；Manual跳过；普通 HTTP 失败只显示默认错误框，安装任务不会自动标 FAILED |
| HA-API-026 | `credentials.get/create/update` GET/POST/PUT | `/clusters/{cluster}/credentials/{alias}`，HA 中 alias=`kdc.admin.credential` | GET 检查 alias；POST/PUT body 均为 `{Credential:{principal,key,type}}`，type 为 `temporary` 或 `persisted`；create/update 失败 helper 仍 resolve 并重放原 AJAX；persisted 可用性来自 cluster model credential-store properties |
| HA-API-027 | `persist.get` GET | `/persist/CLUSTER_CURRENT_STATUS` | 加载压缩 cluster/wizard local DB；404使用默认状态 |
| HA-API-028 | `persist.post` POST | `/persist` | body key/value含 `CLUSTER_CURRENT_STATUS` 或 wizard owner；受 persisted-data权限控制 |
| HA-API-029 | `hosts.all` GET | `/clusters/{cluster}/hosts?fields=Hosts/*,host_components/...` | 注册 rollback Step 1选择/映射 hosts；正常 NNHA/JN流程不依赖此专用调用 |
| HA-API-030 | 静态 rollback接口集合 | config load/save、component GET/delete/passive和service mutation | 仅未接线 `HighAvailabilityRollbackController` 使用，其中两个请求名未注册；`STATIC_ONLY`，不得纳入正常实现 |

## 已知实现风险与 React 决策

| ID | 风险 | React 处理要求 |
| --- | --- | --- |
| JN-RISK-001 | NNHA Step 4 与 Step 6 对服务端 JSON字符串直接 `JSON.parse`，无 try/catch；poll GET没有 error callback/reschedule | malformed/missing payload必须呈现可恢复错误并继续可控轮询，不能让页面静默崩溃 |
| JN-RISK-002 | NNHA Step 6只等前三个 JN响应 | 以所选 host稳定 ID 建全集 barrier，等待全部成功或明确失败/超时 |
| JN-RISK-003 | JN Step 4多 DELETE第一台 success可提前推进 | mutation聚合必须 all-settled并按 host展示结果，失败后 Retry只补失败项 |
| JN-RISK-004 | JN Step 6依赖可能未刷新的 Ember model | Reconfigure/Delete完成后从 server重新读取最终 topology，再启动精确目标集合 |
| JN-RISK-005 | 多 namespace checkpoint不验证返回基数/身份 | 按 namespace到Active NN的期望映射校验，无缺项、无重复才通过 |
| JN-RISK-006 | JN Review配置数组 reload时可能累积重复项 | 每次 load从不可变模板重建，提交前按 `(site,name)` 去重 |
| JN-RISK-007 | component register error仍继续 Install，service-component create失败被吞 | 任何前置 mutation失败即停止当前 task；ResourceAlreadyExists需以明确错误码做幂等分支 |
| JN-RISK-008 | JN route可由Host Details绕过Active/Standby检查，且直接URL无统一业务 gate | route loader执行统一权限、HA topology、状态和cardinality校验，所有入口只负责导航 |
| JN-RISK-009 | JN critical progress关闭无确认/回退，NNHA无auto rollback时只清状态 | React必须保留已完成副作用清单，禁止无提示丢失恢复上下文；补偿流程需单独设计 |
| JN-RISK-010 | 注册 rollback只是空壳，静态完整rollback未接线且含错误接口/类型 | 不复用旧 controller；以服务端事务/幂等补偿和故障注入重新定义 rollback |

## 测试证据与运行态验收矩阵

| ID | 证据/场景 | 当前覆盖与缺口 |
| --- | --- | --- |
| HA-TEST-001 | NNHA Step 1-9 controllers/views | 旧 Karma tests覆盖 ID校验、assignment、Review初始化、checkpoint、各 progress task和人工页；Step 5/9各有 AJAX断言，但共 4 个 `it.skip`，不能当持续通过证据 |
| HA-TEST-002 | JN Step 1-7 | controller tests覆盖 Step 1/2/3/4/6/7与progress/wizard；没有 Step 5 controller test，只有 Step 5 view test |
| HA-TEST-003 | 通用 progress/popup | tests覆盖任务恢复、Retry、request/task popup与日志；不能证明真实Server轮询、KDC重放或多request竞态 |
| HA-TEST-004 | 配置迁移 | `move_namenode_config_initializer_test.js`覆盖主机/配置初始化；仍需真实stack验证properties存在性、Windows和历史HAWQ/Accumulo/Ranger组合 |
| HA-TEST-005 | 入口/权限 | service item与host details tests覆盖部分菜单和JN跳转；没有直接URL越权、Active/Standby绕过、persist权限组合测试 |
| HA-TEST-006 | 恢复/关闭/owner | 未找到 NNHA/JN route close、wizard owner、跨刷新/跨用户完整测试；必须在浏览器+Server做故障点矩阵 |
| HA-TEST-007 | rollback | 注册三步placeholder和未接线15任务controller均无直接测试；Disable错误路由也无覆盖 |
| HA-TEST-008 | 大于三台JN | 未覆盖NNHA formatted响应乱序/部分失败；必须用4/5台JN验证全集barrier |
| HA-TEST-009 | JN并发删除/模型刷新 | 未覆盖多DELETE第一台成功、其余失败/延迟，以及Step 6 stale model；必须做延迟与失败注入 |
| HA-TEST-010 | Federation checkpoint | 未覆盖namespace响应缺项、重复、无Active label fallback和某NN非STARTED组合 |
| HA-TEST-011 | Kerberos模式 | 自动MIT/AD/IPA需验证session失效、credential取消/保存失败/Retry；Manual模式需验证不要求KDC admin credential但仍正确安装组件identity |
| HA-TEST-012 | 完整成功与断点恢复 | 至少验收非安全、安全自动KDC、Manual Kerberos、依赖服务组合、JN add-only/delete-only/mixed、每个mutation前后刷新、Server重启和另一用户登录 |

## 五轮独立审计记录

| 轮次 | 反查面 | 本模块结论 |
| --- | --- | --- |
| 1 | route、菜单、template actions | 找到两个正式向导、注册rollback route、升级检查入口与Host Details JN旁路；确认经典Disable入口实际隐藏 |
| 2 | 九步/七步 controller与通用mixins | 固化严格任务顺序、人工命令、checkpoint门槛、Kerberos gate、Retry/无Skip和关闭语义 |
| 3 | 配置定义/initializer/stack条件 | 固化所有HDFS keys、依赖服务迁移、端口默认值、Windows分支及AMS唯一保留边界 |
| 4 | AJAX注册、调用目录、direct HTTP、persist | 建立31项接口契约，补齐间接 `config.on_site`，区分正常安装链、运维metrics字段、排除metrics refresh和STATIC_ONLY rollback接口 |
| 5 | tests与逆向缺口检查 | 确认既有测试覆盖及skip/空白；识别>3 JN、并发删除、stale model、namespace子集响应、route close/recovery/rollback缺口 |

React 对照完成的最低门槛是：本文件全部稳定 ID 均有 `MATCH/MISSING/DIFFERENT/NOT_APPLICABLE` 结论；所有 `NEEDS_RUNTIME_VALIDATION` 场景在真实 Ambari Server/Agent/stack 上留存请求、task和配置版本证据；所有旧版风险由明确修复决策替代，而不是静默复制。
