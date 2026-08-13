# Federation 与 HAWQ Standby 长流程基线

本文覆盖经典 Ember 中的 NameNode Federation、Router-based Federation（RBF）以及 HAWQ Add/Remove/Activate Standby 五条向导。它们都从 Service Actions 启动，在 modal 中完成多步配置和严格串行的服务端变更。本文不包含任何 Metrics 展示、指标查询、图表或 HAWQ segment live status；源码中仅用于等待 HDFS namespace model 就绪的状态标志属于流程控制依赖，不是本模块的产品能力。

> HAWQ 证据等级：`CONDITIONAL / STATIC_ONLY`。当前 `frontend-refactor` 分支已在提交 `d680af8057` 删除 Server 侧 HAWQ/HDP stack 定义，但保留 Ember route/controller/template。本文用当前 UI 源码说明前端行为，用 `d680af8057^` 的 stack、Kerberos descriptor 与 agent command 说明历史后端契约。HAWQ 不能仅凭当前分支静态运行成功，所有真实部署结论均需 `NEEDS_RUNTIME_VALIDATION`。

## 入口、权限与可见性

| ID | 入口与行为 | 菜单可见条件 | 点击时二次前置检查 | route 边界 |
| --- | --- | --- | --- | --- |
| NNF-ENTRY-001 | HDFS Service Actions 的 Enable NameNode Federation，进入 `/main/services/NameNode/federation/step1` | HDFS model 的客户端硬编码 `serviceTypes` 含 `FEDERATION`；`SERVICE.ENABLE_HA`；已有 master/slave；NameNode HA 已启用；至少 4 台 host | 对过滤出的 `ZOOKEEPER_SERVER` 和 `JOURNALNODE` 分别执行 `everyProperty(..., 'STARTED')`；存在且非 STARTED 才阻止。组件数量为零时空集合返回 true，点击同样会跳转 | `namenode_federation_routes.js` 没有权限或前置 guard，直接 URL 还能绕过其他菜单条件 |
| RBF-ENTRY-001 | HDFS Service Actions 的 Enable Router-based Federation，进入 `/main/services/NameNode/federation/routerBasedFederation/step1` | HDFS model 的客户端硬编码 `serviceTypes` 含 `DFSRouter`；`SERVICE.ENABLE_HA`；已有多个 nameservice | 对过滤出的 `ZOOKEEPER_SERVER` 和 `JOURNALNODE` 分别执行 `everyProperty(..., 'STARTED')`；组件数量为零时空集合返回 true，点击同样会跳转 | `dfsrouter_federation_routes.js` 没有 route guard，直接 URL 还能绕过其他菜单条件 |
| HAWQ-ENTRY-001 | HAWQ Service Actions 的 Add HAWQ Standby，进入 `/main/services/highAvailability/Hawq/add/step1` | `CONDITIONAL / STATIC_ONLY`；HAWQ model 的客户端硬编码 `serviceTypes` 含 `HA_MODE`；`SERVICE.ENABLE_HA`；有 master/slave；当前不是单节点部署且尚无 `HAWQSTANDBY` | 无独立点击前置检查 | route 无权限/资源 guard，直接 URL 可绕过 |
| HAWQ-ENTRY-002 | HAWQ Master custom command 的 Remove HAWQ Standby，进入 `/main/services/highAvailability/Hawq/remove/step1` | `CONDITIONAL / STATIC_ONLY`；stack 的 `HAWQMASTER.customCommands` 含 `REMOVE_HAWQ_STANDBY`；Standby 存在；Master 为 `STARTED` | 无独立二次授权 | custom-command 区块只要求 `SERVICE.RUN_CUSTOM_COMMAND, SERVICE.RUN_SERVICE_CHECK, SERVICE.TOGGLE_MAINTENANCE, SERVICE.ENABLE_HA` 中任一权限；route 也无 guard |
| HAWQ-ENTRY-003 | HAWQ Standby custom command 的 Activate HAWQ Standby，进入 `/main/services/highAvailability/Hawq/activate/step1` | `CONDITIONAL / STATIC_ONLY`；stack 的 `HAWQSTANDBY.customCommands` 含 `ACTIVATE_HAWQ_STANDBY`；Standby 存在；没有检查 Master/Standby service state | 无独立二次授权 | 同一宽泛 OR 权限区块，未明确再次要求 `SERVICE.ENABLE_HA`；route 无 guard |
| FHF-ENTRY-001 | 五条向导都会调用 `dataLoading()`、把对应 HDFS/HAWQ 设为当前 service、暂停常规 update controller，再在大 modal 内连接当前 step outlet | `dataLoading()` 实际只连接 loading outlet 并轮询 `clusterController.isLoaded`，不独立等待 local DB 或 cluster status | NNF/RBF/HAWQ Add/Activate 等待该 promise 后创建 modal；HAWQ Remove 不等待，见 `FHF-RISK-017` | 关闭和普通导航由各 route 的 `unroutePath`、modal handler 控制 |
| FHF-ENTRY-002 | 五个 wizard view 插入 modal 时都通过 `WizardHostsLoading` 固定 GET 一次全部 host，并把结果写入 `content.hosts` | modal 请求失败仍把 view `isLoaded=true`，但不会填充 host map；NNF/RBF/Add Step 2 的第二次 assignment 请求失败则没有 fail handler，placement 初始化不会完成 | NNF/RBF/HAWQ Add 的 Step 2 assignment 又各 GET 一次；首次单向流程调用数为 `2/2/2/1/1`，Back、重建 outlet 或重进 modal 会继续增加 | Remove/Activate 虽无 host assignment 仍发这次请求；React 可消除此冗余，但须保留真正依赖 host map 的流程输入 |
| FHF-ENTRY-003 | 五个 wizard controller 在首次构造已安装 master mapping 时调用 `loadMasterComponentHosts()` | helper 等待一个名称带 Metrics 的遗留全局加载标志后读取本地 `App.HostComponent` model；这里只是被排除的数据加载依赖，不构成本模块的接口或产品能力 | local DB 已有 `masterComponentHosts` 时直接恢复，不再等待该标志 | React 应以明确的 topology-ready 依赖替代该命名耦合，不得把被排除的加载链纳入本模块接口清单 |

NameNode Federation 的“已有 HA、至少四台主机”和 RBF 的“多个 nameservice”来自 action disabled 条件；ZooKeeper/JournalNode 状态来自 `mainAdminHighAvailabilityController` 的点击 handler。React 必须在 route/action 执行层重新校验授权与资源状态，不能只复制菜单隐藏，否则会保留旧版直接 URL 越权边界。

## NameNode Federation 四步状态机

### Step 1 Get Started

| ID | 用户行为 | 校验与分支 | 状态写入/请求 |
| --- | --- | --- | --- |
| NNF-1-001 | 查看已有 nameservice 列表和不可逆/风险提示，输入新的 nameservice ID | 空值、与已有 ID 重名或格式不合法时显示错误并禁用 Next | namespace model 由共享 cluster bootstrap 提供；HA 集群在 config cache 未命中时条件调用 `config.on_site` 读取 current `hdfs-site`，本步不发专用写请求 |
| NNF-1-002 | nameservice ID 格式严格为 1 至 63 个字符，只允许 ASCII 字母、数字、连字符，且首尾不能是连字符 | 正则 `^([a-zA-Z0-9]\|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])$` | Next 保存 `nameServiceId` 到 wizard local DB 后进入 Step 2 |
| NNF-1-003 | 与已有 nameservice ID 的重复校验依赖异步 namespace model | route 的 `dataLoading()` 只等 `clusterController.isLoaded`；已有 ID 列表在 `isHostComponentMetricsLoaded=false` 时返回空数组，因此两轮 service/host-component 异步加载完成前存在窗口，可把已有 ID 当成新 ID提交；后续 flag 变化才重算 | 本步无专用请求；React loader 必须先等待 topology/namespaces ready，再开放输入与 Next；`KNOWN_BUG` |

### Step 2 Select Hosts

| ID | 用户行为 | 校验与分支 | 状态写入/请求 |
| --- | --- | --- | --- |
| NNF-2-001 | 查看全部现有 NameNode（Current）并为新 namespace 固定选择两个 Additional NameNode host | `mastersToShow=['NAMENODE']`、`mastersToAdd=['NAMENODE','NAMENODE']`；不能改变已有 NameNode | Step 2 再用 `hosts.high_availability.wizard` 加载 CPU、内存、磁盘和 maintenance state；初次加载调用 `wizard.loadrecommendations` |
| NNF-2-002 | 可在 host 下拉框中修改两个新增 NameNode 位置 | `useServerValidation=false`；每项必须非空、host 存在、已安装 host 的 maintenance 为 `OFF`，同一组件实例不能落在同一 host；不在修改后调用 Server validation | 客户端检查通过才启用 Next |
| NNF-2-003 | Next 保存所有现有和新增 master-component-host 映射 | 恢复时重新加载 server masters 与 confirmed hosts；推荐值只作初始布局 | 写 wizard local DB/cluster status，进入 Step 3 |

### Step 3 Review And Configurations

| ID | 用户行为 | 校验与分支 | 状态写入/请求 |
| --- | --- | --- | --- |
| NNF-3-001 | Review 当前与新增 NameNode host，并等待配置加载 | 先 GET desired config tags，再读取 `hdfs-site`；安装 Ranger 时同时读 `core-site`、`ranger-tagsync-site`、`ranger-hdfs-security`；安装 Accumulo 时读 `accumulo-site`；任一 GET 失败都没有专用 fail handler，页面通常停在 loading/Next disabled | `config.tags` -> `admin.get.all_configurations` |
| NNF-3-002 | 查看向导生成的 HDFS/Ranger/Accumulo 变更 | 只有新 namespace 的 `dfs.journalnode.edits.dir.<newNs>` 为可编辑 directory；其他生成属性只读且全部 `isOverridable=false` | 本步只把表单结果保存到 local DB，尚未写 desired configs |
| NNF-3-003 | 修改新 namespace 的 JournalNode directory | 初始空值为 required；只接受 Unix 绝对目录、Windows drive 路径或 `file:///` Windows URL，拒绝 `/home*`、`/homes*`、逗号后前导空格和尾随空白；配置未加载或任一 config `isValid=false` 时 Next 禁用 | Next 保存 `serviceConfigProperties` 后进入不可返回的 Step 4 |

### Step 4 Configure Components

Step 4 初始化后禁用更低步骤，以下 18 个 command 严格串行。动态删除的 command 不占运行序列位置；任一前序 command 失败都会停止队列。

| ID | 序号/command | 精确行为与目标 | 主要请求 |
| --- | --- | --- | --- |
| NNF-4-001 | 1 `stopRequiredServices` | 停止除 ZooKeeper 外的所有已安装 services；HDFS 也会停止 | `PUT /clusters/{cluster}/services?ServiceInfo/service_name.in(...)`，state `INSTALLED` |
| NNF-4-002 | 2 `reconfigureServices` | 一次 PUT 保存 `hdfs-site`，条件保存 `ranger-tagsync-site`、`accumulo-site`；成功 callback 继续在全部 NameNode 与 JournalNode hosts 创建/安装 `HDFS_CLIENT`，这仍属于同一 task | `common.service.multiConfigurations`，随后 component existence/create/register/install 链 |
| NNF-4-003 | 3 `installNameNode` | 在两个新增 hosts 创建并安装 `NAMENODE` | component install 链 |
| NNF-4-004 | 4 `installZKFC` | 在两个新增 hosts 创建并安装 `ZKFC` | component install 链 |
| NNF-4-005 | 5 `startJournalNodes` | 启动全部现有 JournalNode，query 强制 `maintenance_state=OFF` | `common.host_components.update`，state `STARTED` |
| NNF-4-006 | 6 `startInfraSolr` | 只启动 `AMBARI_INFRA_SOLR`；未安装 Ranger 或 Infra 时动态删除 | `common.services.update` |
| NNF-4-007 | 7 `startRangerAdmin` | 启动所有 `RANGER_ADMIN`；未安装 Ranger 时删除 | `common.host_components.update` |
| NNF-4-008 | 8 `startRangerUsersync` | 启动所有 `RANGER_USERSYNC`；未安装 Ranger 时删除 | `common.host_components.update` |
| NNF-4-009 | 9 `startNameNodes` | 启动原有、`isInstalled=true` 的 NameNode | `common.host_components.update` |
| NNF-4-010 | 10 `startZKFCs` | 在原有 NameNode hosts 启动 ZKFC | `common.host_components.update` |
| NNF-4-011 | 11 `formatNameNode` | 对第一个新增 NameNode 执行 `FORMAT` | `POST /clusters/{cluster}/requests`，HDFS/NAMENODE/host[0] |
| NNF-4-012 | 12 `formatZKFC` | 对第一个新增 host 的 ZKFC 执行 `FORMAT` | `POST /clusters/{cluster}/requests`，HDFS/ZKFC/host[0] |
| NNF-4-013 | 13 `startZKFC` | 启动第一个新增 ZKFC | `common.host_components.update` |
| NNF-4-014 | 14 `startNameNode` | 启动第一个新增 NameNode | `common.host_components.update` |
| NNF-4-015 | 15 `bootstrapNameNode` | 对第二个新增 NameNode 执行 `BOOTSTRAP_STANDBY` | `POST /clusters/{cluster}/requests`，HDFS/NAMENODE/host[1] |
| NNF-4-016 | 16 `startZKFC2` | 启动第二个新增 ZKFC | `common.host_components.update` |
| NNF-4-017 | 17 `startNameNode2` | 启动第二个新增 NameNode | `common.host_components.update` |
| NNF-4-018 | 18 `restartAllServices` | 对本 cluster 所有未排除 host-components 请求 `RESTART`；排除 `NAMENODE`、`JOURNALNODE`、`ZKFC`、`RANGER_ADMIN`、`RANGER_USERSYNC`，但没有 stale-config 过滤 | `restart.custom.filter`，command `RESTART` |
| NNF-4-019 | Complete | 最后一个 task 达终态后 Done 可用；清 wizard DB/status，回 Services 并刷新 model | cluster status/persist |

## NameNode Federation 配置变换

| ID | 配置 type/属性 | 生成规则 | 首次/后续分支 |
| --- | --- | --- | --- |
| NNF-CFG-001 | `dfs.nameservices`、`dfs.internal.nameservices` | 原 nameservice 列表追加 `<newNs>`，逗号分隔 | 每次生成 |
| NNF-CFG-002 | `dfs.ha.namenodes.<newNs>` | 两个新 NN ID 为 `nn(nameNodes.length-1),nn(nameNodes.length)`；长度包含全部已有和新增 NameNode | 每次生成；ID 是跨集群 NameNode 数量递增，不重置为 `nn1/nn2` |
| NNF-CFG-003 | `dfs.namenode.rpc-address.<newNs>.<newNnId>` | `<newHost>:<rpcPort>`；端口取 generic `dfs.namenode.rpc-address` 的端口，缺省 `8020` | 两个新增 NN 各一项 |
| NNF-CFG-004 | `dfs.namenode.http-address.*`、`dfs.namenode.https-address.*` | 分别使用 generic HTTP/HTTPS address 端口，缺省 `50070`/`50470` | 两个新增 NN 各两项 |
| NNF-CFG-005 | `dfs.client.failover.proxy.provider.<newNs>` | 固定 `org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider` | 每次生成 |
| NNF-CFG-006 | `dfs.namenode.shared.edits.dir.<newNs>` | `qjournal://<allJnHost:8485;...>/<newNs>`；JournalNode host 全量参与，分号分隔 | 每次生成且只读 |
| NNF-CFG-007 | `dfs.journalnode.edits.dir.<newNs>` | 初始为空且 required，必须通过 `displayType=directory` 的通用目录校验；这是唯一 `isReconfigurable=true` 的生成项 | 每次生成，可编辑但不可 config-group override |
| NNF-CFG-008 | 原 namespace 的 scoped JN/shared edits | 把 generic `dfs.journalnode.edits.dir` 复制为 `dfs.journalnode.edits.dir.<oldNs>`，并生成 `dfs.namenode.shared.edits.dir.<oldNs>` | 只有首次从 NameNode HA 扩为 Federation，属性带 `firstRun`；已有 Federation 时移除 |
| NNF-CFG-009 | `dfs.namenode.servicerpc-address.*` | 原 namespace 若已存在任一 scoped service RPC，则为新 namespace 两个 NN 生成 `:8021`；首次还补原 namespace `nn1/nn2` | 原 namespace 两项都不存在时，旧/新四个生成项全部去除 |
| NNF-CFG-010 | generic JN 属性清理 | 从待保存的 server `hdfs-site` 副本删除 `dfs.namenode.shared.edits.dir` 与 `dfs.journalnode.edits.dir`，由 scoped 属性替代 | 每次 Review 计算时执行 |
| NNF-CFG-011 | Ranger TagSync mapping | 为每个 nameservice 写 `ranger.tagsync.atlas.hdfs.instance.<cluster>.nameservice.<ns>.ranger.service=<repoPrefix><ns>`，并为 `fs.defaultFS` namespace 写 cluster-level mapping | 仅安装 Ranger；repo prefix 取 Ranger HDFS service name或 `<cluster>_hadoop_` |
| NNF-CFG-012 | Accumulo volumes | `instance.volumes` 变为所有 `hdfs://<ns>/apps/accumulo/data`；replacement 每个 nameservice 只生成一组源 URI：已有 namespace 取该 group 的 `hosts[0]`，新 namespace 取第一个新增 NameNode，源端口硬编码 `8020`，映射到对应 namespace URI | 仅安装 Accumulo；两项只读；不是为每个 NameNode 分别生成 replacement |
| NNF-CFG-013 | 写入原子边界 | HDFS/Ranger/Accumulo desired configs 组合为一个 array body PUT `/clusters/{cluster}`；随后才安装 HDFS clients | Server 是否跨多个 desired_config 真正事务化需 `NEEDS_RUNTIME_VALIDATION` |

## Router-based Federation 四步状态机

### Steps 1-3

| ID | 步骤 | 用户行为、校验和实际副作用 | 请求/状态 |
| --- | --- | --- | --- |
| RBF-1-001 | 1 Get Started | 阅读 Router Federation 说明；无输入，Next 直接进入 host assignment | local DB/cluster status |
| RBF-2-001 | 2 Select Hosts | 展示现有 NameNode/Router，初始增加一个 Router；`mastersAddableInHA=['ROUTER']`，用户可用通用加减控件继续添加或删除新增 Router | Step 2 再调用 `hosts.high_availability.wizard`，随后做初始 `wizard.loadrecommendations` |
| RBF-2-002 | 2 host 校验 | `useServerValidation=false`；修改后只做客户端非空、host 存在、maintenance OFF、同组件不重复检查；最大数量受 host 数和 stack cardinality 限制 | 当前 BIGTOP `ROUTER` cardinality 为 `0+`；恢复时保留已有 Router |
| RBF-2-003 | 2 Next | 保存全部 master-component-host 映射 | local DB/cluster status |
| RBF-3-001 | 3 Review | 展示待新增 Router；读取 `hdfs-rbf-site`、`hdfs-site`、`core-site`，生成四个只读 Router config；同时等待共享 namespace model gate；两个配置 GET 任一失败都没有专用 fail handler，通常停在 loading | `config.tags` -> `admin.get.all_configurations`；cluster bootstrap 的 `config.on_site` 仅在 cache miss 时条件发生 |
| RBF-3-002 | 3 配置写入 | **实际旧版行为不是点击 Next 时保存**：配置加载且 namespace model 就绪后，observer 立即发 PUT，然后不等待成功就 render 并设 `isLoaded=true` | `common.service.configurations`；`sender:self` 在普通浏览器脚本中通常解析为全局 `window/self` 而不是 controller，请求一般仍会发出，但成功无 callback，失败 callback 会尝试调用全局 `onTaskError` |
| RBF-3-003 | 3 Next | 页面在 PUT 未完成甚至最终失败时也可能已启用 Next；Next 只把表单 property snapshot 保存到 local DB，再进入 Step 4 | 不再可靠地承担首次保存动作，也不构成 PUT 成功门禁 |

### Step 4 Configure Router

| ID | 序号/command | 精确行为与目标 | 主要请求 |
| --- | --- | --- | --- |
| RBF-4-001 | 1 `installRouter` | controller 把现有和新增的全部 Router hosts 交给 install helper；helper 只为缺失 host 创建 `ROUTER`，最终 PUT `INSTALLED` 虽仍携带全部 Router host 列表，但 query 同时强制 `HostRoles/maintenance_state=OFF`。因此只有非 maintenance Router（包括已启动者）会被置为 `INSTALLED` 并暂时停止，maintenance Router 不受该最终 PUT 影响 | component existence/create/register/install 链；最终 `common.host_components.update` |
| RBF-4-002 | 2 `startRouters` | 启动向导映射中的 Router hosts，query 排除 maintenance 中组件 | `common.host_components.update`，state `STARTED` |
| RBF-4-003 | Complete | 两个 task 完成后清 wizard 状态并返回 Services | cluster status/persist |
| RBF-4-004 | 不可达代码 | controller 定义了 `reconfigureServices()` 和成功后的 `installHDFSClients()`，但二者不在 `commands` 数组，正常状态机永远不会调用 | React 不得把不可达方法误当成旧版功能 |

## Router-based Federation 配置变换

| ID | 属性 | 值与来源 | 写入边界 |
| --- | --- | --- | --- |
| RBF-CFG-001 | `dfs.federation.router.monitor.namenode` | 所有 namespace/NameNode 组合为逗号列表；NN suffix 用跨 namespace 连续计数，例如 `<ns1>.nn1,<ns1>.nn2,<ns2>.nn3,<ns2>.nn4` | Step 3 observer 立即 PUT |
| RBF-CFG-002 | `dfs.federation.router.default.nameserviceId` | 当前 nameservice 列表的第一项 | Step 3 observer 立即 PUT |
| RBF-CFG-003 | `zk-dt-secret-manager.zkAuthType` | 固定 `none` | Step 3 observer 立即 PUT |
| RBF-CFG-004 | `zk-dt-secret-manager.zkConnectionString` | `core-site/ha.zookeeper.quorum` | Step 3 observer 立即 PUT |
| RBF-CFG-005 | 编辑与 override | 四项 `isReconfigurable=false`、`isOverridable=false`，Review 只读 | 代码假定现有 `hdfs-rbf-site.properties` 必定存在；缺失时会对 `undefined` 写属性 |

## HAWQ Add Standby 四步状态机

以下全部为 `CONDITIONAL / STATIC_ONLY`。

| ID | 步骤 | 用户行为、校验与分支 | 请求/状态 |
| --- | --- | --- | --- |
| HAWQ-ADD-1-001 | 1 Get Started | 阅读新增 Standby 会停止/启动 HAWQ 的维护窗口提示；Next 清旧 `hawqHosts` 和 master assignment | local DB/cluster status |
| HAWQ-ADD-2-001 | 2 Select Host | 展示已安装 HAWQ Master 并为一个新 `HAWQSTANDBY` 选择 host；Master 不可移动 | host list + Stack Advisor recommendation |
| HAWQ-ADD-2-002 | 2 client validation | host 必须存在、非空、maintenance OFF；generic 校验只禁止相同 `component_name` 的多个实例重复落在同一 host | `HAWQMASTER` 与 `HAWQSTANDBY` 是不同 component，因此 Standby 与 Master 同 host 会通过该客户端校验；基础错误才直接阻断 Next |
| HAWQ-ADD-2-003 | 2 advisor validation | `useServerValidation=true`；初始布局请求 recommendation，assignment 变化及 Submit 均先重新 recommendation、再请求 `config.validations` | 历史 Advisor 将 Master/Standby 同 host 报为 `HAWQSTANDBY` host-component `ERROR`，并对 Ambari Server/PostgreSQL 5432 冲突告警；旧 UI 仍把 `submitDisabled=false`，用户可在 issue popup 选择 Continue Anyway |
| HAWQ-ADD-3-001 | 3 Review | 生成只读 `hawq_standby_address_host=<selectedHost>`；加载当前 `hawq-site` 并读取 `hawq_master_directory` | `config.tags` -> `reassign.load_configs` |
| HAWQ-ADD-3-002 | 3 人工数据目录门禁 | Submit 弹确认，要求用户已在新 Standby host 将 `<hawq_master_directory>` 重命名或保证为空，防止旧数据启动；UI 不远程验证目录 | 用户确认后检查 KDC session，成功才进入 Step 4 |
| HAWQ-ADD-4-001 | 1 `stopRequiredServices` | 只停止 HAWQ service | `common.services.update`, state `INSTALLED` |
| HAWQ-ADD-4-002 | 2 `installHawqStandbyMaster` | 在新 host 创建并安装 `HAWQSTANDBY` | component install 链，内含 KDC session 检查 |
| HAWQ-ADD-4-003 | 3 `reconfigureHAWQ` | 重新加载最新 `hawq-site`，合入 `hawq_standby_address_host`，保存新 desired config | `config.tags` -> `reassign.load_configs` -> `common.service.configurations` |
| HAWQ-ADD-4-004 | 4 `startRequiredServices` | 只启动 HAWQ service，不请求 smoke test | `common.services.update`, state `STARTED` |
| HAWQ-ADD-4-005 | Complete | 清 Add wizard local DB，cluster status 设 `DEFAULT`，返回 Services 后 reload | cluster status/persist |

## HAWQ Remove Standby 三步状态机

以下全部为 `CONDITIONAL / STATIC_ONLY`。

| ID | 步骤/序号 | 用户行为与精确副作用 | 请求/状态 |
| --- | --- | --- | --- |
| HAWQ-REMOVE-1-001 | 1 Get Started | 读取并保存当前 `HAWQMASTER` 和 `HAWQSTANDBY` host；展示移除说明 | local DB/cluster status |
| HAWQ-REMOVE-2-001 | 2 Review | 展示将删除的 Standby host，以及会删除 `hawq_standby_address_host` | 无配置读取；Submit 先检查 KDC session |
| HAWQ-REMOVE-2-002 | 2 irreversible confirm | KDC session 成功后 route 再弹“Ambari 无法 rollback”确认；确认后 Step 3 禁用低阶步骤 | local DB/cluster status |
| HAWQ-REMOVE-3-001 | 1 `removeStandby` | 在当前 Master host 执行 `REMOVE_HAWQ_STANDBY` custom command | `POST /clusters/{cluster}/requests`, HAWQ/HAWQMASTER/master host |
| HAWQ-REMOVE-3-002 | 2 `stopRequiredServices` | 只停止 HAWQ | `common.services.update`, state `INSTALLED` |
| HAWQ-REMOVE-3-003 | 3 `reconfigureHAWQ` | 加载最新 `hawq-site`，删除 `hawq_standby_address_host` 并保存 | config GET chain -> `common.service.configurations` |
| HAWQ-REMOVE-3-004 | 4 `deleteHawqStandbyComponent` | DELETE Standby host 的 `HAWQSTANDBY` host-component；`NoSuchResourceException` 按成功 | `common.delete.host_component` |
| HAWQ-REMOVE-3-005 | 5 `startRequiredServices` | 只启动 HAWQ | `common.services.update`, state `STARTED` |
| HAWQ-REMOVE-3-006 | Complete | 清 wizard、status=`DEFAULT`、返回 Services 并 reload | cluster status/persist |

历史 agent 实现的 `REMOVE_HAWQ_STANDBY` 实际执行 `hawq init standby -a -v -r --ignore-bad-hosts`。命令语义和失败输出需在匹配的历史 HAWQ stack 上 `NEEDS_RUNTIME_VALIDATION`。

## HAWQ Activate Standby 三步状态机

以下全部为 `CONDITIONAL / STATIC_ONLY`。

| ID | 步骤/序号 | 用户行为与精确副作用 | 请求/状态 |
| --- | --- | --- | --- |
| HAWQ-ACT-1-001 | 1 Get Started | 读取并保存原 Master/Standby hosts，展示故障切换说明 | local DB/cluster status |
| HAWQ-ACT-2-001 | 2 Review | 展示原 Master 将删除、Standby 将提升；生成只读 `hawq_master_address_host=<oldStandbyHost>` | 本步不读取 server configs；Submit 检查 KDC session |
| HAWQ-ACT-2-002 | 2 irreversible confirm | KDC 成功后 route 弹无法 rollback 确认，确认才进入 progress | local DB/cluster status |
| HAWQ-ACT-3-001 | 1 `activateStandby` | 在原 Standby host 执行 `ACTIVATE_HAWQ_STANDBY` | `POST /clusters/{cluster}/requests`, HAWQ/HAWQSTANDBY/standby host |
| HAWQ-ACT-3-002 | 2 `stopRequiredServices` | 只停止 HAWQ；历史 agent activate 后可能按旧端口停止仍运行的新 Master process | `common.services.update`, state `INSTALLED` |
| HAWQ-ACT-3-003 | 3 `reconfigureHAWQ` | 加载最新 `hawq-site`，删除 `hawq_standby_address_host`，写 `hawq_master_address_host=<oldStandby>`，保存 | config GET chain -> `common.service.configurations` |
| HAWQ-ACT-3-004 | 4 `installHawqMaster` | 在原 Standby host 创建并安装 `HAWQMASTER` | component install 链，内含 KDC session 检查 |
| HAWQ-ACT-3-005 | 5 `deleteOldHawqMaster` | DELETE 原 Master host 的 `HAWQMASTER` | `common.delete.host_component` |
| HAWQ-ACT-3-006 | 6 `deleteHawqStandby` | DELETE 原 Standby host 的 `HAWQSTANDBY` | `common.delete.host_component` |
| HAWQ-ACT-3-007 | 7 `startRequiredServices` | 只启动 HAWQ | `common.services.update`, state `STARTED` |
| HAWQ-ACT-3-008 | Complete | 清 wizard、status=`DEFAULT`、返回 Services 并 reload | cluster status/persist |

历史 agent 实现的 `ACTIVATE_HAWQ_STANDBY` 执行 `hawq activate standby -a -M fast -v --ignore-bad-hosts`。该命令成功但后续 Ambari stop/install/delete 中途失败的拓扑，需要专门故障注入验证。

## HAWQ 配置与历史 Stack 契约

| ID | 证据/配置 | 历史静态契约 | React 基线边界 |
| --- | --- | --- | --- |
| HAWQ-CFG-001 | `hawq_standby_address_host` | Add 写新 Standby host；Remove/Activate 删除 | 所有写入先重新加载最新 `hawq-site`，避免用 Review 时旧 snapshot 覆盖并发配置 |
| HAWQ-CFG-002 | `hawq_master_address_host` | Activate 写原 Standby host | UI 生成属性只读，保存发生在 custom command 和 stop HAWQ 之后 |
| HAWQ-CFG-003 | `hawq_master_directory` | Add 只读取并用于人工清理确认，不修改 | UI 不验证远端目录，必须保留清晰的人工责任边界 |
| HAWQ-CFG-004 | component cardinality | `HAWQMASTER=1`，`HAWQSTANDBY=0-1` | 来自 `d680af8057^`，当前分支 `STATIC_ONLY` |
| HAWQ-CFG-005 | service/component dependency | HAWQ required service 为 HDFS；Master 另有 cluster-scope `HDFS/NAMENODE` 依赖且 `auto-deploy=false`；Master/Standby 都有 host-scope `HDFS/HDFS_CLIENT` 依赖且 `auto-deploy=true` | 来自删除前 metainfo，真实依赖补全和 stop/start 编排 `NEEDS_RUNTIME_VALIDATION` |
| HAWQ-CFG-006 | Kerberos identity | Master/Standby 共用 `hawq_identity`：principal `postgres@${realm}`，keytab `${keytab_dir}/hawq.service.keytab`，owner `gpadmin`、group `${cluster-env/user_group}` | 来自删除前 Kerberos descriptor，当前 Server 是否仍可创建身份不可静态证明，`NEEDS_RUNTIME_VALIDATION` |
| HAWQ-CFG-007 | custom command timeout | `REMOVE_HAWQ_STANDBY` 与 `ACTIVATE_HAWQ_STANDBY` 的历史 commandScript timeout 都是 1200 秒 | UI 不设置 timeout；必须由兼容 stack 暴露并在慢命令/超时场景 `NEEDS_RUNTIME_VALIDATION` |

## Kerberos 条件分支

| ID | 场景 | 旧版行为 | React 等价要求 |
| --- | --- | --- | --- |
| FHF-KRB-001 | 所有 component install command | `createInstallComponentTask()` 在 create/register/install 之前调用 `getKDCSessionState`；KDC popup 被取消会把当前 progress task 标为 `FAILED` | 不得先创建 component 再检查 credential；失败必须落在可 Retry 的当前 command |
| FHF-KRB-002 | HAWQ Add Step 3 | 用户确认数据目录后、进入 Step 4 前显式检查 KDC session | 自动 Kerberos 要求有效 admin credential；Manual Kerberos 直接通过通用 session 检查 |
| FHF-KRB-003 | HAWQ Remove/Activate Step 2 | Submit 先检查 KDC session，再显示不可逆确认并进入 progress | 两层门禁顺序必须保持 |
| FHF-KRB-004 | Federation/RBF | 没有专用 descriptor editor、identity review 或 Manual Kerberos CSV 页面；安全集群中新 NameNode/ZKFC/Router identity 依赖 Ambari Server 安装 component 的标准行为 | `NEEDS_RUNTIME_VALIDATION`：用自动和 Manual Kerberos 集群验证 principal/keytab 创建、缺失凭据、Retry 和清理 |
| FHF-KRB-005 | RBF stack descriptor | 当前 BIGTOP descriptor 静态定义 Router principal `router/_HOST@${realm}` 和 keytab `${keytab_dir}/dr.service.keytab` | 这是当前静态证据，不证明安装请求会完整物化 identity |
| FHF-KRB-006 | HAWQ descriptor | 只能使用 `d680af8057^` 的 `hawq_identity` 证据 | `CONDITIONAL / STATIC_ONLY`，必须在可运行历史 stack 验证 |
| FHF-KRB-007 | 通用 KDC 类型分支 | secure cluster 且 controller 尚无 `kdc_type` 时，先读 current Kerberos service config 的 `kerberos-env/kdc_type`；值为 `none` 时视为 Manual Kerberos并直接 callback，其他类型才读取 session state | React 不能对 Manual Kerberos错误要求 KDC admin credential；自动 KDC 的 session-state 失败才进入 invalid KDC popup |
| FHF-KRB-008 | KDC session 无效 | popup 允许输入 principal/password 并选 `temporary` 或 `persisted`；先 GET alias `kdc.admin.credential`，存在则 PUT、不存在则 POST，随后裸 `$.ajax` 重发保存的原始请求 options | 重发可能是 session-state GET，也可能是触发 KDC 错误的 mutation；component install 中取消 popup 会把当前 task 标为 FAILED，HAWQ progress 前置检查取消则留在 Review |
| FHF-KRB-009 | credential 保存异常 | `createOrUpdateCredentials()` 的 create/update 分支无论实际成功或失败都会 resolve，随后仍重发原始请求 | React 必须在 credential 写入失败时停止并展示错误，不能用后续重放掩盖保存失败 |

## 通用进度、失败、重试与日志

| ID | 行为 | 精确语义 |
| --- | --- | --- |
| FHF-PROG-001 | 严格串行 | `commands` 初始化为 task 数组；只有当前 task `COMPLETED` 才把下一个 `PENDING` 改为 `QUEUED` 并调用对应 method；前序失败不继续 |
| FHF-PROG-002 | request 轮询 | mutation 返回 `Requests.id` 后保存到当前 task；每 4 秒 GET `background_operations.get_by_request`，直到不存在 `PENDING/QUEUED/IN_PROGRESS` task |
| FHF-PROG-003 | 终态聚合 | 任一 server task 为 `FAILED`、`TIMEDOUT` 或 `ABORTED`，当前 wizard task 为 `FAILED`；否则为 `COMPLETED` |
| FHF-PROG-004 | Retry | 本范围使用 command-level Retry：只把第一个失败 command 重置为 `PENDING` 并重新执行，不自动撤销前面已完成的副作用 |
| FHF-PROG-005 | Skip | 通用 controller 有 `onTaskErrorWithSkip`，但本范围 36 个 command 均未调用它，因此没有可用 Skip/Ignore and Proceed |
| FHF-PROG-006 | 最后任务失败 | `WizardEnableDone` 允许最后一个 command 为 `FAILED` 时启用 Complete；用户可带着最后阶段失败退出。任何更早 command 失败仍因后续 `PENDING` 而阻断完成 |
| FHF-PROG-007 | host/task 日志 | 点击 `IN_PROGRESS/FAILED/COMPLETED` command 且已有 request ID 时可打开 host progress popup；本范围 command 的 `stageId=null`，所以 popup 仍用 `background_operations.get_by_request` 聚合 hosts；只有共用 popup 收到非 null `stageId` 才改用 `common.request.polling`；用户展开单 task 后用 `background_operations.get_by_task` 轮询 `stdout`、`stderr`、`output_log`、`error_log` |
| FHF-PROG-008 | 安装链与部分幂等 | 先查询目标 host 是否已有 component；有缺失时先 direct GET component state 并刷新本地 service model，必要时 POST service-component，再 POST host-component，最后 PUT `INSTALLED`；existence GET 没有 fail handler，失败后 task 不进入终态；旧 helper 还会吞掉 service-component create 失败，并把 host-component register 的 error callback 当 success，不能视为可靠幂等 |
| FHF-PROG-009 | 删除幂等保护 | DELETE host-component 返回 `NoSuchResourceException` 时按 `COMPLETED` 继续；其他 DELETE 错误为 FAILED |
| FHF-PROG-010 | service/component mutation | stop/start/service restart 和 component start 通常返回后台 request；只有 request 内所有 tasks 终态后才推进；maintenance 中 host-component 被 `common.host_components.update` query 排除 |

## 退出、持久化与恢复

| ID | 场景 | 旧版行为与限制 |
| --- | --- | --- |
| FHF-REC-001 | step snapshot | 每次切 step 保存 current step、host mapping、configs；progress 额外保存每个 task status/request IDs 和当前 request IDs 到 local DB，并通过 `/persist` 写 cluster status；没有 `CLUSTER.MANAGE_USER_PERSISTED_DATA` 时 POST promise 直接 reject |
| FHF-REC-002 | progress 刷新 | 恢复时 `IN_PROGRESS` 继续轮询，`QUEUED` 重新调用当前 command；五个 controller 均在 `controller_route.js` 注册，wizard user 可依据 `wizardControllerName` 被导回向导 |
| FHF-REC-003 | NameNode/RBF 关闭 | 任一步关闭都二次确认；Step 4 使用更强的“已开始变更”警告。确认后清 local state、status=`DEFAULT`、返回 Services/reload，但不撤销 server 副作用 |
| FHF-REC-004 | HAWQ 关闭 | Add 的 Step 1-3、Remove/Activate 的 Step 1-2 直接清理并 reload，不额外确认；progress Step 4/3 才确认。清理同样不回滚已完成请求 |
| FHF-REC-005 | 完成 | `isFinished=true` 后 route 才允许普通离开；Complete 清 namespace、更新 models/status 并回 Service 页面 |
| FHF-REC-006 | wizard ownership | NameNode/RBF/Add/Activate 在 enter 调 `wizardWatcherController.setUser`；Remove 没有。HAWQ finish/close 未见显式 `resetUser`，Add/Activate 可能残留 owner，Remove 可能缺 owner；`NEEDS_RUNTIME_VALIDATION`：跨用户、刷新、关闭后新向导和 stale `/persist` 场景 |
| FHF-REC-007 | Federation cluster state | NameNode/RBF progress 继承 `HIGH_AVAILABILITY_DEPLOY`，route 却只识别 `NN_FEDERATION_DEPLOY`/`RBF_FEDERATION_DEPLOY`；源码未找到专用状态写入点；精确刷新恢复不可信，列为旧版缺陷而非期望行为 |
| FHF-REC-008 | HAWQ cluster state | progress 写 `ADD_HAWQ_STANDBY`、`REMOVE_HAWQ_STANDBY`、`ACTIVATE_HAWQ_STANDBY`，与各 route 恢复匹配；loader 直接接受 server `response.clusterState`，未消费声明但未使用的 `cluster_states.validStates`，所以源码支持按状态恢复。仍受 owner 不一致和 Remove 初始化竞态影响 |
| FHF-REC-009 | persist 权限 | cluster status 与 wizard owner 都通过 `App.Persist.postUserPref()`，静态 gate 为 `CLUSTER.MANAGE_USER_PERSISTED_DATA`；五条 route 没有显式校验。缺权限时 Deferred 立即 reject，而 progress 只在 status POST success callback 中入队首个 command，因此五条流程都确定不会启动，Retry 同样不推进，也不会触发网络错误 popup |

## 接口契约表

默认 REST 前缀为 `/api/v1`。下表列出五条流程直接或经通用 mixin 实际使用的接口；同一命名请求会因 query/payload 承担多个 command。专用 controller 没有裸 HTTP，但 component install 的共享 helper 使用一条非 Metrics `App.HttpClient` direct GET，已作为 `FHF-API-027` 纳入。该 helper 随后还等待一次全局 Metrics refresh；后者没有决定向导输入或结果，按范围明确排除，不是 React 必须复刻的产品接口。

| ID | App.ajax name / Method | URL | 关键 query 或 payload | 使用场景 |
| --- | --- | --- | --- | --- |
| FHF-API-001 | `hosts.high_availability.wizard` GET | `/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true` | 无 body；五个 modal 固定一次，NNF/RBF/Add Step 2 各再一次 | 首次单向调用数：NNF 2、RBF 2、Add 2、Remove 1、Activate 1；modal 请求失败仍标记 view loaded，Step 2 的第二次请求失败则使 assignment 初始化不完成；重进/Back 可增加 |
| FHF-API-002 | `wizard.loadrecommendations` POST | `{stackVersionUrl}/recommendations` | `hosts=<全部 registered fqdn>`、`services=<全部 selected/installed service>`、`recommend:"host_groups"`；本范围实际必传完整已装拓扑 `recommendations:{blueprint,blueprint_cluster_binding}` | NNF/RBF 初始推荐；HAWQ Add 初始推荐，assignment 变化和 Submit 时又在 validation 前调用 |
| FHF-API-003 | `config.validations` POST | `{stackVersionUrl}/validations` | 同一 hosts/services；`validate:"host_groups"`；`recommendations` 为紧邻上一 recommendation 响应的完整对象 | 仅 HAWQ Add；assignment 变化及 Submit 时在 recommendation 成功后调用 |
| FHF-API-004 | `config.tags` GET | `/clusters/{clusterName}?fields=Clusters/desired_configs` | 无 body | Step 3/重配置前找 current tags；NNF/RBF Review 失败通常停在 loading，HAWQ Add 失败保持 `isLoaded=false`，HAWQ progress 的失败则标记当前 task FAILED |
| FHF-API-005 | `admin.get.all_configurations` GET | `/clusters/{clusterName}/configurations?{urlParams}` | Federation/RBF 以 `(type=X&tag=Y)\|...` 批量读 | Federation/RBF Review；无专用 error callback，失败通常停在 loading/Next disabled |
| FHF-API-006 | `reassign.load_configs` GET | `/clusters/{clusterName}/configurations?{urlParams}` | HAWQ `(type=hawq-site&tag=<tag>)` | HAWQ Add Review 错误误入 success callback；三条 progress reconfigure 则用 `onTaskError` 正常失败当前 task |
| FHF-API-007 | `common.service.configurations` PUT | `/clusters/{clusterName}` | formatter 原样包装 `{Clusters:{desired_config:<value>}}`；RBF 的 value 是单个 `{type:"hdfs-rbf-site",properties,properties_attributes?}`，HAWQ 的 value 是 `reconfigureSites()` 生成的单元素 array | RBF observer 提前发送且 UI 不等待结果；HAWQ 单 type 保存；Server 对 object/array 两种形态的兼容性需保留测试 |
| FHF-API-008 | `common.service.multiConfigurations` PUT | `/clusters/{clusterName}` | `[{Clusters:{desired_config:...}}, ...]` | NameNode Federation 保存 HDFS/条件 Ranger/Accumulo；RBF 中仅不可达方法引用 |
| FHF-API-009 | `common.services.update` PUT | `/clusters/{clusterName}/services?{urlParams}` | `RequestInfo.context/operation_level=CLUSTER`; `Body.ServiceInfo.state=INSTALLED\|STARTED`; query `ServiceInfo/service_name.in(...)` | stop/start required services |
| FHF-API-010 | `host_component.installed.on_hosts` GET | `/clusters/{clusterName}/host_components` | component name、host `.in(...)`，返回已有 host-components | 每个 component install 前幂等检查；调用方只注册 `.done()`，GET 失败仅走默认错误弹窗，当前 install task 可能永久不进终态 |
| FHF-API-011 | `common.create_component` POST | `/clusters/{clusterName}/services?ServiceInfo/service_name={serviceName}` | `{components:[{ServiceComponentInfo:{component_name}}]}` | service-component 尚未建立时创建；调用方以 `.always()` resolve，真实创建失败也继续注册 host-component |
| FHF-API-012 | `wizard.step8.register_host_to_component` POST | `/clusters/{cluster}/hosts` | `RequestInfo.query=Hosts/host_name=...\|...`; `Body.host_components[].HostRoles.component_name` | 给目标 hosts 注册 component；success/error 都绑定 `onCreateComponent`，两条路径都会继续 PUT `INSTALLED` |
| FHF-API-013 | `common.host_components.update` PUT | `/clusters/{clusterName}/host_components?{urlParams}` | 本范围通常不传 `urlParams`；component/host/`maintenance_state=OFF` 过滤位于 JSON `RequestInfo.query`，`Body.HostRoles.state=INSTALLED\|STARTED` | 安装或启动 component |
| FHF-API-014 | `common.delete.host_component` DELETE | `/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}` | path 参数 | HAWQ Remove/Activate 删除拓扑组件 |
| FHF-API-015 | `service.item.executeCustomCommand` POST | `/clusters/{clusterName}/requests` | `RequestInfo.command/context`; filter `{service_name,component_name,hosts}` | HAWQ Remove/Activate custom command |
| FHF-API-016 | `nameNode.federation.formatNameNode` POST | `/clusters/{clusterName}/requests` | command `FORMAT`; filter HDFS/NAMENODE/第一个新 host | NNF command 11 |
| FHF-API-017 | `nameNode.federation.formatZKFC` POST | `/clusters/{clusterName}/requests` | command `FORMAT`; filter HDFS/ZKFC/第一个新 host | NNF command 12 |
| FHF-API-018 | `nameNode.federation.bootstrapNameNode` POST | `/clusters/{clusterName}/requests` | command `BOOTSTRAP_STANDBY`; filter HDFS/NAMENODE/第二个新 host | NNF command 15 |
| FHF-API-019 | `restart.custom.filter` POST | `/clusters/{clusterName}/requests` | command `RESTART`；`hosts_predicate` 排除 NN/JN/ZKFC/Ranger components并约束 `HostRoles/cluster_name`，**没有** `stale_configs=true` 条件 | NNF command 18，实际会请求重启所有未排除的 host-components，而不只 stale components |
| FHF-API-020 | `background_operations.get_by_request` GET | `/clusters/{clusterName}/requests/{requestId}` | fields 含 request 与 task status/command/host；`minimal_response=true` | 每 4 秒 progress polling |
| FHF-API-021 | `common.request.polling` GET | `/clusters/{clusterName}/requests/{requestId}?fields=...&tasks/Tasks/stage_id={stageId}` | request 与 task ID、command/detail、timing、status、host、structured output | `CONDITIONAL` 共用 popup 分支；仅传入非 null `stageId` 时使用，本范围正常 36-command task 的 stageId 均为 null |
| FHF-API-022 | `background_operations.get_by_task` GET | `/clusters/{clusterName}/requests/{requestId}/tasks/{taskId}` | path 参数；返回完整 task，包括 stdout/stderr/output/error log | 单 task detail 与日志轮询 |
| FHF-API-023 | `admin.security.cluster_configs.kerberos` GET | `/clusters/{clusterName}/configurations/service_config_versions?service_name=KERBEROS&is_current=true` | current Kerberos config version | 自动/Manual KDC session 分支的通用 controller |
| FHF-API-024 | `kerberos.session.state` GET | `/clusters/{clusterName}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details` | 无 body | component install 与 HAWQ progress 前检查 |
| FHF-API-025 | `persist.get` GET | `/persist/{key}` | wire 上无 query/body；key 为 `CLUSTER_CURRENT_STATUS` 或 `wizard-data`。cluster loader 的 user/login/auth/override 仅留在客户端 callback params，不发送到 Server | app bootstrap/updater 条件加载状态与 wizard owner，不是每步显式 GET |
| FHF-API-026 | `persist.post` POST | `/persist` | cluster status 是双层字符串化 `{"CLUSTER_CURRENT_STATUS":"\"<LZString-base64>\""}`；owner 是 `{"wizard-data":"{\"userName\":...,\"controllerName\":...}"}`，reset 的 owner value 为字符串 `"null"` | step/task 状态、owner、完成/关闭清理；受 persisted-data 权限 gate |
| FHF-API-027 | direct `App.HttpClient.get` GET | `/clusters/{clusterName}/components/?fields=ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true` | 无 body；更新 `componentsStateMapper` | component install 有缺失 host 时，在判断/创建 service-component 前刷新 component state |
| FHF-API-028 | `credentials.get` GET | `/clusters/{clusterName}/credentials/kdc.admin.credential` | alias path 参数 | KDC invalid popup 保存新凭据前探测 alias |
| FHF-API-029 | `credentials.create` POST | `/clusters/{clusterName}/credentials/kdc.admin.credential` | `{Credential:{principal,key,type:<"temporary" 或 "persisted">}}` | alias 不存在时创建 KDC admin credential，settle 后重放原始 AJAX options |
| FHF-API-030 | `credentials.update` PUT | `/clusters/{clusterName}/credentials/kdc.admin.credential` | 同一 Credential body，type 二选一 | alias 已存在时更新；settle 后同样重放原请求，可能是 session-state GET 或原 mutation |
| FHF-API-031 | `config.on_site` GET | `/clusters/{clusterName}/configurations?{params}` | 本场景 `params=(type=hdfs-site&tag=<currentTag>)`；若本地 tag 表为空，调用链还会先用 `config.tags` | `CONDITIONAL / shared cluster bootstrap`；HDFS loaded 且 HA enabled、current hdfs-site cache/tag 未命中时加载 namespace model；失败被 `.always()` 收敛为空配置，外层可能 resolve，但 namespace-ready 标志不会置位，NNF/RBF Review 可永久等待 |

## 旧版已确认缺陷与 React 处理原则

| ID | 旧版缺陷 | 静态证据与影响 | React 处理原则 |
| --- | --- | --- | --- |
| FHF-RISK-001 | 所有五条 route 缺权限和资源 guard | 只在菜单构建/点击 handler 检查；直接 URL 可打开 wizard | 不兼容复刻；route loader 和 mutation 前必须重验权限、stack capability、组件/服务状态 |
| FHF-RISK-002 | RBF Review 自动提前保存 | `onLoad` observer 在用户点击 Next 前 PUT `hdfs-rbf-site` | 不兼容复刻；React 应明确提交点、失败反馈和退出语义，并为行为差异写 migration decision |
| FHF-RISK-003 | RBF 请求 sender 是错误全局对象 | `sender:self` 在普通浏览器脚本中通常取全局 `window/self`，不是 controller；请求可正常发出，但失败时 AJAX wrapper 调用全局 `onTaskError` 会再抛异常，成功又没有 callback，且 UI 已先允许继续 | 修复为明确 handler/context；React 必须等待 PUT 成功后才允许 Next，并覆盖慢请求、HTTP 失败和退出竞态 |
| FHF-RISK-004 | RBF 假定 config type 已存在 | `hdfsrbfConfigs&&hdfsrbfConfigs.properties` 后仍执行 `configToSave.properties[name]=...` | 缺 type 时阻断并给可操作错误，或显式初始化；不能抛 JS exception |
| FHF-RISK-005 | RBF progress 有不可达重配置代码 | `commands` 只有 install/start，`reconfigureServices/installHDFSClients` 从不调用 | 以可达状态机为旧版功能基线；若 React 加入，作为有意修复而非“等价覆盖” |
| FHF-RISK-006 | Ranger defaultFS mapping 重复追加 | Federation 在每个 nameservice 循环内追加相同 cluster-level property | React 应去重，并用最终 desired config fixture 验证 |
| FHF-RISK-007 | Federation persist 状态名不匹配 | progress 写 `HIGH_AVAILABILITY_DEPLOY`，route 读 `NN_FEDERATION_DEPLOY`/`RBF_FEDERATION_DEPLOY` | 定义单一显式 state enum，测试刷新恢复每个 command |
| FHF-RISK-009 | 通用 Rollback 错接向导 | `supports.autoRollbackHA` 会给失败 task 显示 Rollback，但 `HighAvailabilityProgressPageController.rollback()` 硬编码 `highAvailabilityWizardController` 和 NameNode HA rollback route | 本范围不得宣称支持 Rollback；React 仅在存在对应逆操作状态机时显示 |
| FHF-RISK-010 | 关闭只是忘记状态，不撤销副作用 | progress 关闭会清 DB/status/reload，已完成 stop/config/install/delete/custom command 保留 | UI 必须明确“退出不回滚”；可提供 Resume，不得把清状态描述成 Cancel transaction |
| FHF-RISK-011 | HAWQ wizard owner 不一致 | Add/Activate set user 但未显式 reset；Remove 未 set user | 重构时统一 acquire/release ownership；旧版具体跨用户行为 `NEEDS_RUNTIME_VALIDATION` |
| FHF-RISK-012 | 最后一项失败仍能完成 | `WizardEnableDone` 允许最后 command FAILED 时 Done | 保留“带失败完成”必须明确告警并链接后台任务；否则设计为阻断并记录有意差异 |
| FHF-RISK-013 | HAWQ 已无当前 Server stack | UI route 仍存在但 component/config/custom command 服务端定义被删除 | 默认隐藏/禁用；只有检测到兼容 stack capability 才启用，不以遗留 Ember 文件判断 |
| FHF-RISK-014 | component install 吞掉创建错误 | service-component POST 用 `.always()` 继续；host-component POST 的 `error` 也绑定 `onCreateComponent`，因此任意注册失败后仍尝试 PUT `INSTALLED`；专门的 `onCreateComponentError` 没接到该调用 | React 必须区分 already-exists 与真实失败；真实失败停止 command，Retry 前重新读 server state |
| FHF-RISK-015 | HAWQ Add 配置加载错误不一致 | Step 3 `config.tags` 失败保持未加载且没有 Retry；后续 `reassign.load_configs` 又把 success/error 都指向 `loadConfigsSuccessCallback`，可能以 jqXHR 作为 `hawqProps` 并显示已加载，Submit 再访问缺失的 `items[0]` | 不兼容复刻；两个读取阶段都必须进入显式 error state、阻断 Submit 并提供 Retry |
| FHF-RISK-016 | HAWQ Activate 泄漏全局变量 | Step 2 `newHawqMaster = ...` 未声明变量 | React 使用局部不可变状态；为 Review host 生成写单元测试 |
| FHF-RISK-017 | HAWQ Remove 不等待初始化 | route 调用 `dataLoading().done(set current HAWQ service)` 后，立即另行 `Em.run.next()` 创建 modal、读 cluster status 并 transition；不像其余四条流程等待 promise | React route loader 必须先完成 cluster/service/status 加载再渲染；旧版竞态需用冷启动和慢请求 `NEEDS_RUNTIME_VALIDATION` |
| FHF-RISK-018 | HAWQ Remove/Activate Step 1 markup 无效 | 两个模板把 footer 起始标签写成 `</div class="wizard-footer col-md-12">`，形成多余关闭标签且没有真实 footer wrapper | React 不复刻损坏 DOM；增加结构、键盘导航和按钮可见性测试 |
| FHF-RISK-019 | component existence GET 失败会卡住 task | `createComponent()` 对 `checkInstalledComponents()` 只接 `.done()`；请求失败后除默认错误弹窗外，不调用 `onTaskError`，task 保持运行前状态且 Retry 不出现 | React 把 preflight GET 纳入 command 状态机；失败必须终止当前 attempt、可 Retry，并在重试前重读 server topology |
| FHF-RISK-020 | Federation 配置读取失败会留下永久 gate | NNF/RBF Review 自己的 `config.tags`、configuration GET 无 fail handler；共享 loader 中 tag GET 用 `.always()` 继续、`config.on_site` 用 `.always()` 把失败收敛为空数组，`updateHDFSNameSpaces()` 收到空配置后不设置 `isHDFSNameSpacesLoaded=true` | React 的配置 loader 必须有 success/error 终态、超时与 Retry；namespace 解析失败不得表现为无限 spinner |

## 测试覆盖与运行态场景

| ID | 静态测试发现 | 已覆盖 | 明确缺口 |
| --- | --- | --- | --- |
| FHF-TEST-001 | NameNode Federation tests | Step 1 ID 校验、Step 3 配置生成、Step 4 controller 方法/AJAX 参数、wizard local DB load/save | route/权限/入口状态、Step 2 assignment 集成、真实 18-command 执行、Ranger/Accumulo 完整 desired config、Kerberos、退出恢复 E2E |
| FHF-TEST-002 | Router Federation tests | 未发现专用 test | 全部流程缺口，特别是提前保存、错误的全局 `self` sender、缺 `hdfs-rbf-site`、多 Router cardinality、Kerberos identity、刷新恢复 |
| FHF-TEST-003 | HAWQ Add tests | Step 3 dynamic config/确认逻辑、Step 4 controller/AJAX、wizard load/save | route/权限、真实 advisor ERROR/WARN、目录清理、KDC、agent command、服务失败与恢复 E2E |
| FHF-TEST-004 | HAWQ Remove tests | Step 2 submit、Step 3 custom command/config/delete、wizard load/save | route/权限、不可逆确认顺序、真实 agent、partial failure 和 Retry |
| FHF-TEST-005 | HAWQ Activate tests | Step 2 property、Step 3 custom command/config/install/delete、wizard controller | route/权限、真实 process/端口行为、安装新 Master identity、删除顺序失败、跨用户恢复 |
| FHF-TEST-006 | 排除项检查 | `hawqsegment_live_test.js` 及任何 dashboard/service/host Metrics test 不属于本模块 | React 对比不得把指标展示缺口计入本文覆盖率 |

React 验收至少要覆盖下列运行态矩阵，静态源码无法替代：

| ID | 场景 | 断言 |
| --- | --- | --- |
| FHF-RUNTIME-001 | NameNode HA 首次扩为 Federation；再次添加第三个 namespace | firstRun scoped 属性、NN ID、JN URI、service RPC 条件和旧 generic 属性清理均正确 |
| FHF-RUNTIME-002 | Federation 分别安装/不安装 Ranger、Accumulo、Infra | 18-task 动态删除、desired config、停止列表与无 stale gate 的重启谓词正确，Ranger mapping 无重复 |
| FHF-RUNTIME-003 | RBF 单/多 Router、缺失 `hdfs-rbf-site`、Step 3 关闭/请求失败 | 提交边界明确，无 JS exception；Router install/start 和 config 保持一致 |
| FHF-RUNTIME-004 | 自动 Kerberos、Manual Kerberos、KDC session 过期/取消 | 新 NN/ZKFC/Router/HAWQ component 的 principal/keytab、失败 task 与 Retry 等价 |
| FHF-RUNTIME-005 | 每个 command 注入 FAILED/TIMEDOUT/ABORTED，刷新页面后 Retry | 已完成副作用不重复破坏；当前 request 恢复轮询；无虚假 Rollback/Skip |
| FHF-RUNTIME-006 | Step 1/Review/progress 分别关闭；原用户/另一用户重进 | warning、owner、persist、恢复点、server 副作用陈述准确 |
| FHF-RUNTIME-007 | 兼容历史 HAWQ stack 的 Add/Remove/Activate 全流程与每阶段中断 | cardinality、custom command、配置、组件关系、共享 identity 和 service state 最终一致 |

## 主要源码证据

- Federation routes：`ambari-web/classic/app/routes/namenode_federation_routes.js`、`ambari-web/classic/app/routes/dfsrouter_federation_routes.js`
- Federation controllers：`ambari-web/classic/app/controllers/main/admin/federation/`
- HAWQ routes：`ambari-web/classic/app/routes/add_hawq_standby_routes.js`、`remove_hawq_standby_routes.js`、`activate_hawq_standby_routes.js`
- HAWQ controllers：`ambari-web/classic/app/controllers/main/admin/highAvailability/hawq/`
- Service Actions：`ambari-web/classic/app/views/main/service/item.js`、`ambari-web/classic/app/models/host_component.js`
- 通用 assignment/progress：`ambari-web/classic/app/mixins/wizard/assign_master_components.js`、`ambari-web/classic/app/mixins/wizard/wizardProgressPageController.js`、`ambari-web/classic/app/controllers/main/admin/highAvailability/progress_controller.js`
- 请求注册表：`ambari-web/classic/app/utils/ajax/ajax.js`
- 测试：`ambari-web/classic/test/controllers/main/admin/federation/`、`ambari-web/classic/test/controllers/main/admin/highAvailability/hawq/`
- 历史 HAWQ stack/agent 证据：`git show d680af8057^:ambari-server/src/main/resources/common-services/HAWQ/2.0.0/...`

启发式模块索引见 [generated/api-by-module/security-ha-federation.md](generated/api-by-module/security-ha-federation.md)：它按 request name 和 caller path 宽匹配，可能混入跨模块请求，也可能漏掉由共享 controller/mixin 间接调用的请求，不能视为接口全集。跨模块的 component install、KDC session、progress log 与 persist 请求仍属于本基线；权威网络核对必须联合 [generated/ajax-endpoints.md](generated/ajax-endpoints.md)、[generated/ajax-calls.md](generated/ajax-calls.md)、[generated/direct-http-calls.md](generated/direct-http-calls.md)、[generated/browser-network-entrypoints.md](generated/browser-network-entrypoints.md) 与 [generated/realtime-channels.md](generated/realtime-channels.md)。
