# ResourceManager 与 Ranger Admin HA 基线

本文档覆盖经典 Ember 中 ResourceManager HA（RM HA）和 Ranger Admin HA（RA HA）的入口、四步向导、配置变换、后台任务、Kerberos 联动、失败恢复和后端接口。它是 React 等价实现的行为基准，不把“存在同名页面”视为完成。

范围明确排除 Metrics 展示、图表、Widget 和指标查询。本文出现的主机 CPU、内存、磁盘字段仅用于 HA 主机选择，不属于 Metrics 展示功能。

证据标记遵循 [00-methodology.md](00-methodology.md)：`CONFIRMED` 表示至少两类静态证据互相印证；`CONDITIONAL` 表示依赖权限、stack、服务或安全模式；`STATIC_ONLY` 表示静态路径存在但不能宣称运行态可用；`NEEDS_RUNTIME_VALIDATION` 表示 React 对照前必须在真实集群复核。

## 路由与入口

| 对象 | 经典路由 |
| --- | --- |
| RM HA | `/main/services/highAvailability/ResourceManager/enable/step1` 到 `step4` |
| RA HA | `/main/services/highAvailability/RangerAdmin/enable/step1` 到 `step4` |

| ID | 等级 | 功能与行为 | 前置、隐藏与禁用条件 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RMHA-ENTRY-001 | `CONDITIONAL` | 在 YARN Service 页 Actions 菜单生成 Enable ResourceManager HA；点击后调用入口 controller 并进入 RM 四步向导 | YARN 固定带 `HA_MODE`，但菜单仍要求 `SERVICE.ENABLE_HA`、service 含 master/slave；`App.isAuthorized` 还会因升级状态、`supports.opsDuringRollingUpgrade=false` 或其他用户占用向导而返回 false。该权限只是入口 gate，后续 stop/start、配置保存和 component mutation 前端不再逐项复查对应权限 | 无新增请求；使用已加载 service/host-component 模型 | `app/views/main/service/item.js#observeMaintenanceOnce`、`app/controllers/main/service/item.js#enableRMHighAvailability`、`app/app.js#isAuthorized` |
| RMHA-ENTRY-002 | `CONDITIONAL` | RM action 在单节点、找不到 RM、或 RM 为 `INIT/INSTALL_FAILED` 时显示为 disabled | `isNotInstalled` 只覆盖 `INIT/INSTALL_FAILED`；其他非启动状态并不在菜单层禁用，而由点击后的前置检查处理 | 无 | `app/models/host_component.js#TOGGLE_RM_HA`、`app/models/host_component.js#isNotInstalled` |
| RMHA-ENTRY-003 | `CONFIRMED` | 点击后一次收集并以 `<br/>` 合并显示所有失败项；只有全部通过才 transition | 当前 RM 必须 `STARTED`；至少 3 个 `ZOOKEEPER_SERVER` 实例；至少 3 台 host。这里只数 ZooKeeper 实例，不检查其运行态，也不检查 maintenance mode | 无 | `app/controllers/main/admin/highAvailability_controller.js#enableRMHighAvailability`、对应 controller test |
| RMHA-ENTRY-004 | `CONDITIONAL` | HA 已启用后 action 被隐藏；启用判定为 stack 允许多实例且已加载 RM host-component 数量大于 1 | label/css 虽可生成 Disable 样式，但 handler 始终是 `enableRMHighAvailability`，且 action 已隐藏，因此没有可用 RM Disable HA UI | 无 | `app/app.js#isRMHaEnabled`、`app/models/host_component.js#TOGGLE_RM_HA` |
| RMHA-ENTRY-005 | `CONFIRMED` | RM 不能通过普通 Add Component 添加，只能走专用 HA 向导 | `RESOURCEMANAGER` 属于 `isMasterAddableOnlyOnHA`，从 host 普通 addable master 中排除 | 无 | `app/models/stack_service_component.js#isMasterAddableOnlyOnHA`、`#isAddableToHost` |
| RAHA-ENTRY-001 | `CONDITIONAL` | 在 RANGER Service 页 Actions 菜单生成 Enable Ranger Admin HA；点击即进入 RA 四步向导 | 与 RM 相同，菜单要求 `SERVICE.ENABLE_HA`、`HA_MODE`、master/slave 和 `App.isAuthorized` 全局门禁；后续 stop/start、配置保存和 component mutation 前端不再逐项复查对应权限 | 无新增请求 | `app/views/main/service/item.js#observeMaintenanceOnce`、`app/controllers/main/service/item.js#enableRAHighAvailability` |
| RAHA-ENTRY-002 | `CONDITIONAL` | RA action 在单节点、找不到 RA、或 RA 为 `INIT/INSTALL_FAILED` 时 disabled；已有多个 RA 时 hidden | `isRAHaEnabled` 要求 stack 允许多实例且 RA host-component 数量大于 1 | 无 | `app/app.js#isRAHaEnabled`、`app/models/host_component.js#TOGGLE_RA_HA` |
| RAHA-ENTRY-003 | `CONFIRMED` | Ranger 入口 controller 不做 RM 那组运行态、ZooKeeper 数量、主机数或 maintenance 前置检查，直接 transition | 菜单层只有单节点/组件缺失与全局权限门禁；这不是“通过检查”，而是没有实现对应检查 | 无 | `app/controllers/main/admin/highAvailability_controller.js#enableRAHighAvailability` |
| RAHA-ENTRY-004 | `CONFIRMED` | RA 不能通过普通 Add Component 添加；HA 已启用后也没有 Disable UI | `RANGER_ADMIN` 属于 `isMasterAddableOnlyOnHA`；action 只有 Enable handler，已有多个 RA 时隐藏 | 无 | `app/models/stack_service_component.js`、`app/models/host_component.js#TOGGLE_RA_HA` |
| HA-STATIC-001 | `STATIC_ONLY` | 两个 `App.WizardRoute` 的 `isRoutable` 只要求登录；route 本身不复查 `SERVICE.ENABLE_HA`、service/stack 条件或入口前置检查，执行阶段也不在前端复查 `SERVICE.START_STOP`、配置或 host-component mutation 权限 | 直接 URL 是前端越权/脆弱路径；服务端仍可能逐请求拒绝。React route loader 与后端必须分别授权，不能把菜单 gate 当端到端授权契约 | 进入后会触发向导自己的数据请求 | `app/router.js#WizardRoute`、`app/routes/rm_high_availability_routes.js`、`ra_high_availability_routes.js`、两向导 Step 4 controller |

## ResourceManager HA 四步流程

### Step 1 Get Started

| ID | 等级 | 功能与行为 | 边界与结果 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RMHA-1-001 | `CONFIRMED` | 展示启用后的 Active/Standby ResourceManager、自动 failover 语义 | 明确提示安排 maintenance window 并准备 cluster downtime；页面只有 Next，无输入 | 无 | `app/templates/main/admin/highAvailability/resourceManager/step1.hbs`、`app/messages.js` |
| RMHA-1-002 | `CONFIRMED` | Next 清除旧 `rmHosts` 和 master-component host 选择，再进入 Step 2 | Step 1 到 Step 3 的 route 均显式 `unroutePath=false`，阻止普通路由离开 | `persist.post` 间接保存 current step/localdb | `app/routes/rm_high_availability_routes.js#step1`、`app/controllers/wizard.js` |

### Step 2 Select Host

| ID | 等级 | 功能与行为 | 校验、分支与结果 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RMHA-2-001 | `CONFIRMED` | 显示当前已安装 RM 并固定新增一个 RM；当前 RM host selector disabled，新 RM 可选 host | `mastersToShow/mastersToAdd=['RESOURCEMANAGER']`；当前项带 Current、新增项带 Additional | `hosts.high_availability.wizard`、`wizard.loadrecommendations` | RM Step 2 controller/view、`templates/common/assign_master_components.hbs` |
| RMHA-2-002 | `CONFIRMED` | Stack Advisor 为新增 RM 推荐 placement，优先使用未占用的推荐 host，否则按可用 host 顺序选择 | payload 为 `recommend=host_groups`、全部当前 hosts/services 和现有 blueprint；推荐失败走全局 error popup，不会进入可操作完成态 | `POST {stackVersionUrl}/recommendations` | `assign_master_components.js#loadStep`、host-component recommendation mixin |
| RMHA-2-003 | `CONFIRMED` | 客户端校验 host 非空、存在、已安装 host 的 maintenance state 为 `OFF`，且同一 host 不得放两个同名 RM | `useServerValidation=false`，所以 Next 不调用 `/validations`，也没有 Continue Anyway 的 server warning/error 流程 | 明确不调用 `config.validations` | RM Step 2 controller、`assign_master_components.js#isHostNameValid/#submit` |
| RMHA-2-004 | `CONFIRMED` | host 选项展示 hostname、内存、CPU，并携带 disk/maintenance 数据；排序为内存降序、CPU 降序、hostname 升序 | host 数大于 25 使用最多展示 10 项的 typeahead；25 及以下使用 dropdown | `hosts.high_availability.wizard` | `assign_master_components.js#loadWizardHostsSuccessCallback/#sortHosts`、assign master view |
| RMHA-2-005 | `CONFIRMED` | Next 保存 `{currentRM, additionalRM}` 和完整 master-component host mapping 后进入 Step 3；Back 回 Step 1 | 相同组件 host 冲突或非法 host 时 Next disabled | `persist.post` 间接保存选择 | `app/routes/rm_high_availability_routes.js#step2` |
| RMHA-2-006 | `NEEDS_RUNTIME_VALIDATION` | RM wizard controller 自身的 Step 2 load map 只从 namespace 读取 `hosts` 快照，再读取 service/master 数据；外层 RM wizard view 才会另行调用 HA hosts API 填充快照 | `dataLoading()` 与 modal view 插入的时序、全新直达 Step 2/刷新时 host 快照是否始终可靠，需要真实浏览器验证 | `hosts.high_availability.wizard`，但不走 `hosts.confirmed` | RM wizard controller load map、`WizardHostsLoading`、RM wizard view |

### Step 3 Review

| ID | 等级 | 功能与行为 | 配置与边界 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RMHA-3-001 | `CONFIRMED` | 显示当前 RM、新增 RM 和将被修改的 YARN/条件性 HAWQ/HDFS 配置 | 所有列出的 HA 配置只读、不可 override；加载完成前 Next disabled | `config.tags`、`reassign.load_configs` | RM Step 3 controller/template、`rm_ha_properties.js` |
| RMHA-3-002 | `CONFIRMED` | 读取当前 `zoo.cfg`、`yarn-site`、`yarn-env`，从现有地址保留 resource tracker、HTTP、HTTPS、ZooKeeper client port | 缺值时默认 `8025`、`8088`、`8090`、`2181` | `GET /clusters/{cluster}/configurations?...` | RM Step 3 `_prepareDependencies` |
| RMHA-3-003 | `CONFIRMED` | 生成 YARN HA 核心配置 | `ha.enabled=true`、`ha.rm-ids=rm1,rm2`、两 RM hostname/resource-tracker/webapp/http(s)、recovery enabled、`ZKRMStateStore`、ZooKeeper quorum、cluster ID `yarn-cluster`、leader election path `/yarn-leader-election` | 同上，Step 4 才保存 | `rm_ha_properties.js`、`rm_ha_config_initializer.js`、controller tests |
| RMHA-3-004 | `CONDITIONAL` | 若 HAWQ 已安装，生成 `yarn-client` 的 RM HA 地址 | `yarn.resourcemanager.ha=<rm1>:8032,<rm2>:8032`；scheduler HA 使用 `8030`；未安装 HAWQ 时 category/task 均去除 | 同上 | RM properties/initializer、Step 4 `initializeTasks` |
| RMHA-3-005 | `CONFIRMED` | 调用 Stack Advisor configuration recommendation，并把新 RM 加入对应 blueprint host group | payload 为 `recommend=configurations`、所有 host、已安装 service、含当前 configs 的 blueprint；返回可能包含多项推荐，但 controller 实际只读取并应用 `core-site` 的 `hadoop.proxyuser.<yarn_user>.hosts`，其他推荐不写入 Review 或保存集合 | `config.recommendations` | RM Step 3 `loadRecommendations/#applyRecommendedConfigurations` |
| RMHA-3-006 | `CONFIRMED` | proxyuser hosts 存在时更新原属性，否则动态追加只读 `core-site` 属性，值覆盖当前与新增 RM hosts | 只有同时取得 `yarn_user` 和 advisor 推荐值才追加/更新 | `config.recommendations` | RM Step 3 controller、initializer tests |
| RMHA-3-007 | `CONDITIONAL` | 点击 Next 先验证 KDC session；非 Kerberos或 Manual Kerberos 直接继续，自动 Kerberos只有 session `OK` 才保存 configs 并进入 Step 4 | KDC 无效弹 credential popup；取消留在 Step 3；Step 3 route 的 `next` 只有收到 callback 才执行 | `admin.security.cluster_configs.kerberos`（按需）、`kerberos.session.state`、credential CRUD（按需） | RM Step 3 `submit`、main admin Kerberos controller |
| RMHA-3-008 | `NEEDS_RUNTIME_VALIDATION` | `reassign.load_configs` 的 error callback 指向 success handler，意图对配置读取错误降级；recommendation 使用 `.always()`，失败也继续显示页面 | initializer 仍直接读取 `data.items` 与 `yarn-env`，而 `config.tags` error 指向未在本 controller 定义的 `loadConfigsErrorCallback`；完整失败是否能安全降级不可由静态代码确认 | 同 RMHA-3-001 | RM Step 3 `renderConfigs/#loadConfigsSuccessCallback` |

### ResourceManager 配置契约

以下是 Review 展示并由 Step 4 覆盖写入的完整属性集合。Review 中这些属性全部只读且不可 override；`rm1` 固定取当前已安装 RM，`rm2` 固定取新增 RM。

| ID | Site | 精确属性和值 | 来源、条件与兼容边界 |
| --- | --- | --- | --- |
| RMHA-CFG-001 | `yarn-site` | `yarn.resourcemanager.ha.enabled=true` | 源码中的值为 boolean `true` |
| RMHA-CFG-002 | `yarn-site` | `yarn.resourcemanager.ha.rm-ids=rm1,rm2` | ID 固定，不由已有配置或 Advisor 改名 |
| RMHA-CFG-003 | `yarn-site` | `yarn.resourcemanager.hostname.rm1=<currentRM>`；`yarn.resourcemanager.hostname.rm2=<additionalRM>` | 仅 hostname，不附端口 |
| RMHA-CFG-004 | `yarn-site` | `yarn.resourcemanager.resource-tracker.address.rm1=<currentRM>:<port>`；`.rm2=<additionalRM>:<port>` | 从旧 `yarn.resourcemanager.resource-tracker.address` 的冒号后字段取 port；缺失时 `8025` |
| RMHA-CFG-005 | `yarn-site` | `yarn.resourcemanager.webapp.address.rm1=<currentRM>:<port>`；`.rm2=<additionalRM>:<port>` | 从旧 `yarn.resourcemanager.webapp.address` 取 port；缺失时 `8088` |
| RMHA-CFG-006 | `yarn-site` | `yarn.resourcemanager.webapp.https.address.rm1=<currentRM>:<port>`；`.rm2=<additionalRM>:<port>` | 从旧 `yarn.resourcemanager.webapp.https.address` 取 port；缺失时 `8090` |
| RMHA-CFG-007 | `yarn-site` | `yarn.resourcemanager.recovery.enabled=true` | 源码中的值为 boolean `true` |
| RMHA-CFG-008 | `yarn-site` | `yarn.resourcemanager.store.class=org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore` | 固定实现类 |
| RMHA-CFG-009 | `yarn-site` | `yarn.resourcemanager.zk-address=<zk1>:<port>,<zk2>:<port>,...` | 使用全部 `ZOOKEEPER_SERVER` master mapping；port 来自 `zoo.cfg/clientPort`，缺失时 `2181`；不筛运行态或 maintenance |
| RMHA-CFG-010 | `yarn-site` | `yarn.resourcemanager.cluster-id=yarn-cluster` | 固定值 |
| RMHA-CFG-011 | `yarn-site` | `yarn.resourcemanager.ha.automatic-failover.zk-base-path=/yarn-leader-election` | 固定值 |
| RMHA-CFG-012 | `yarn-client` | `yarn.resourcemanager.ha=<currentRM>:8032,<additionalRM>:8032` | 仅 HAWQ 已安装时生成、展示和保存 |
| RMHA-CFG-013 | `yarn-client` | `yarn.resourcemanager.scheduler.ha=<currentRM>:8030,<additionalRM>:8030` | 仅 HAWQ 已安装时生成、展示和保存 |
| RMHA-CFG-014 | `core-site` | `hadoop.proxyuser.<yarn_user>.hosts=<Advisor value>` | 仅 `yarn-env/yarn_user` 与 Advisor 推荐同时存在时加入保存集合；向导 initializer 自身计算的 host 值没有直接加入表单 |
| RMHA-CFG-015 | 三个 site | 分别合并进当前完整 `yarn-site`、条件性 `yarn-client`、完整 `core-site` 后创建新 desired config version | 三个 site 是串行独立 PUT，不是事务；旧的无 `.rm1/.rm2` generic address 属性不会被显式删除，向导也不额外生成 RM client/admin/scheduler 的 suffixed address 属性 |

### Step 4 Configure Components

| ID | 等级 | 顺序与行为 | 请求、成功与失败语义 | 主要证据 |
| --- | --- | --- | --- | --- |
| RMHA-4-001 | `CONFIRMED` | 1 Stop Required Services | 调用 `stopServices(['HDFS'])` 的真实语义是停止除 HDFS 外的所有已安装 service；request 成功后轮询，API/后端 task 任一失败即停链 | RM Step 4、progress mixin |
| RMHA-4-002 | `CONDITIONAL` | 2 Install Additional ResourceManager | 先做 KDC session check，再查重、必要时创建 service-component、注册 host-component、PUT 到 `INSTALLED` 并轮询；KDC popup 取消使当前任务 FAILED | RM Step 4、progress/install mixins |
| RMHA-4-003 | `CONFIRMED` | 3 Reconfigure YARN | 重新读取当前 `yarn-site`，保留原 `properties` 和 `properties_attributes`，覆盖向导生成属性，以 HA note 提交一个 desired config version；任一 GET/PUT 失败置 task FAILED | `config.tags`、`reassign.load_configs`、`common.service.configurations` |
| RMHA-4-004 | `CONDITIONAL` | 4 Reconfigure HAWQ | 仅 HAWQ 已安装时存在；同样保存 `yarn-client` 两个 HA 地址并保留原属性/attributes | 同上 |
| RMHA-4-005 | `CONFIRMED` | 5 Reconfigure HDFS | 总会重新读取并保存完整当前 `core-site` desired config version；若 Review 得到 proxyuser 推荐则覆盖/追加该属性，否则仍提交内容未被向导改变的完整 `core-site`；保留原 properties/attributes | 同上 |
| RMHA-4-006 | `CONFIRMED` | 6 Start All Services | `PUT` 全部 service 到 `STARTED`，附 `params/run_smoke_test=true`；若 Ambari property `skip.service.checks=true` 则发送 false；响应 request 后轮询 | `common.services.update`、`background_operations.get_by_request` |
| RMHA-4-007 | `CONFIRMED` | 最后 Start All Services 即便 FAILED 仍启用 Complete | RM Step 4 混入 `WizardEnableDone`，仅“最后一个 task 且位于最后一步”失败时解除 Complete disabled；前面任务失败仍必须 Retry | `wizardEnableDone.js`、RM Step 4 controller |
| RMHA-4-008 | `CONFIRMED` | Complete 清 RM wizard namespace、刷新全部模型、置 `isFinished`，将 cluster status 设 `DEFAULT`，隐藏 modal、回 Services 并 reload | `setClusterStatus` 的 persist 请求无论成功失败都在 `alwaysCallback` 导航/reload | RM wizard `finish`、RM route Step 4、`resetOnClose` |
| RMHA-4-009 | `CONFIRMED` | 三次配置保存按 YARN、条件性 HAWQ、HDFS 的 task 顺序串行推进 | 每个 PUT 成功后才开始下一 task，但服务端没有跨 task transaction；例如 YARN 已创建新 version 后 HAWQ/HDFS 失败会留下部分配置状态，Retry 只重放失败 task，不回滚 YARN | RM Step 4 `commands/#onSaveConfigs`、progress task observer |

## Ranger Admin HA 四步流程

### Step 1 Get Started

| ID | 等级 | 功能与行为 | 校验、边界与结果 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RAHA-1-001 | `CONFIRMED` | 说明新增 Standby Ranger Admin、自动 failover、停机窗口，并要求用户先自行部署 load balancer | UI 不创建、不探测、不配置外部 load balancer | 无 | RA Step 1 template/messages |
| RAHA-1-002 | `CONFIRMED` | 输入 load balancer URL；Next 仅在 URL validator 通过时可用 | 必须带 `http://`、`https://` 或 `ftp://` scheme；空值禁用且不显示错误，非空非法值显示错误 | 无 | RA Step 1 controller、`utils/validator.js#isValidURL` |
| RAHA-1-003 | `CONFIRMED` | 页面提示 load balancer 与 Ranger Admin 分离 | 这只是说明/warning，不验证 LB host、DNS、连通性、TLS、健康检查或与 RA 的共置关系 | 无 | RA Step 1 message、RA Step 2 general warning |
| RAHA-1-004 | `CONFIRMED` | Next 保存 `loadBalancerURL`、清旧 master host mapping，进入 Step 2 | 向导恢复时会从 RA namespace 重新加载该 URL | `persist.post` 间接保存 localdb/current step | RA route Step 1、RA wizard load map |

### Step 2 Select Hosts

| ID | 等级 | 功能与行为 | 校验、分支与结果 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RAHA-2-001 | `CONFIRMED` | 显示不可编辑的当前 RA，默认新增一个 RA；用户可继续用 `+/-` 增删新增 RA | `mastersAddableInHA=['RANGER_ADMIN']`；最大实例数是 stack cardinality 上限与 host 数量的较小值；已安装 RA 不可删除 | `hosts.confirmed`、`hosts.high_availability.wizard`、`wizard.loadrecommendations` | RA Step 2 controller、assign master mixin/template |
| RAHA-2-002 | `CONFIRMED` | 使用与 RM 相同的 Stack Advisor placement、host 排序、25-host typeahead 分支 | 当前 RA 显示在前；新增项带 Additional；推荐失败走默认错误处理 | 同上 | RA Step 2 controller、共享 assign master 源码 |
| RAHA-2-003 | `CONFIRMED` | 客户端校验 host 非空、存在、maintenance `OFF`、同一 host 不能有两个 RA | `useServerValidation=false`，不调用 `/validations`；LB 分离 warning 不阻止 Next | 明确不调用 `config.validations` | RA Step 2 controller、assign master mixin |
| RAHA-2-004 | `CONFIRMED` | 与 RM 不同，RA wizard 明确先调用 `loadHosts()`；首次从 `hosts.confirmed` 初始化含 host-component 状态的 namespace 快照，再获取选择页资源数据 | 若 namespace 已有快照则复用；请求失败走默认 error popup，异步 load map 不 resolve | `hosts.confirmed` | RA wizard load map、`WizardController#loadHosts` |
| RAHA-2-005 | `CONFIRMED` | Next 保存 `{currentRA, additionalRA: [...]}` 和 master mapping 进入 Step 3；Back 回 Step 1 | 安装阶段会处理全部 additionalRA hosts | `persist.post` 间接保存 | RA route Step 2 |

### Step 3 Review

| ID | 等级 | 功能与行为 | 配置与边界 | 请求 | 主要证据 |
| --- | --- | --- | --- | --- | --- |
| RAHA-3-001 | `CONFIRMED` | 显示当前 RA、所有新增 RA 和配置变更；配置 collection 加载完成前显示 spinner | 所有值设为 Step 1 的 LB URL 且 `isEditable=false` | 使用全局已加载 configs collection；本步无专属 REST | RA Step 3 controller/template/test |
| RAHA-3-002 | `CONDITIONAL` | 只为“config type 可映射到 service 且该 service 已安装”的项生成预览 | 静态候选共 10 项：`admin-properties/policymgr_external_url`，以及 HDFS、YARN、HBase、Hive、Knox、Kafka、KMS、Storm、Atlas 的 `ranger.plugin.*.policy.rest.url`；controller 用 `getConfigByName(...) \|\| {}`，所以目标 property 在 stack collection 不存在时仍会用空对象构造只读 LB URL 行，并不验证属性真实存在 | 无 | RA wizard `configs`、RA Step 3 controller |
| RAHA-3-003 | `CONFIRMED` | Next 直接进入安装，不在 Review 阶段检查 KDC session | 与 RM Step 3 不对称；KDC 检查延迟到 Install Additional Ranger Admin task | 无 | RA route Step 3、RA Step 3 controller |
| RAHA-3-004 | `STATIC_ONLY` | category 去重条件写成 `if (!configCategories.someProperty('name'), serviceName)` | JavaScript 逗号运算符使条件实际取 `serviceName`；非空 service name 通常恒为 true，因此同一 service 对应多个候选 site 时可能重复创建 category。React 不应复制该缺陷 | 无 | RA Step 3 controller line 51 |

### Step 4 Install, Start and Test

| ID | 等级 | 顺序与行为 | 请求、成功与失败语义 | 主要证据 |
| --- | --- | --- | --- | --- |
| RAHA-4-001 | `CONFIRMED` | 1 Stop All Services | 调用 `stopServices([], true, true)`，对 `/services` PUT `INSTALLED`；成功后轮询；失败停链 | RA Step 4、progress mixin |
| RAHA-4-002 | `CONDITIONAL` | 2 Install Additional Ranger Admin | 对所有 additionalRA host 一次查重/注册并 PUT `INSTALLED`；进入 task 时检查 KDC session，取消 credential popup 则 task FAILED | RA Step 4、progress/install mixins |
| RAHA-4-003 | `CONDITIONAL` | 3 Reconfigure Services | 从当前 `Clusters.desired_configs` 中只选择存在的候选 site，加载后保留原 properties/attributes并无条件写对应 LB URL，以一个 `common.service.multiConfigurations` PUT 提交全部 selected sites；这里不按已安装 service 或目标 property 是否已存在过滤，stale desired config 也可能被更新 | RA Step 4 `onLoadConfigsTags/#onLoadConfigs` |
| RAHA-4-004 | `CONFIRMED` | 4 Start All Services | 与 RM 相同，附 `params/run_smoke_test=!skip.service.checks` 并轮询 | `common.services.update`、request polling |
| RAHA-4-005 | `CONFIRMED` | 最后任务失败不能 Complete，必须 Retry 到成功 | RA controller未混入 `WizardEnableDone`；失败只显示 task Retry，完成按钮保持 disabled | RA Step 4 controller、progress mixin |
| RAHA-4-006 | `CONFIRMED` | Complete 重置 wizard watcher owner，清 RA namespace、刷新模型、置 cluster `DEFAULT`，隐藏 modal、回 Services 并 reload | cluster-status persist 无论成功失败都执行最终导航 | RA route Step 4、RA wizard `finish` |

## 共用失败、恢复与 Kerberos 语义

| ID | 等级 | 功能与行为 | 关键边界 | 主要证据 |
| --- | --- | --- | --- | --- |
| HA-COMMON-FAIL-001 | `CONFIRMED` | Step 4 task 状态机为 `PENDING -> QUEUED -> IN_PROGRESS -> COMPLETED/FAILED`；同一时刻只推进一个未完成 task | 有 `FAILED/QUEUED/IN_PROGRESS` 时不启动后续任务；API error 直接把当前 task 置 FAILED | `wizardProgressPageController.js#statusChangeCallback/#onTaskError` |
| HA-COMMON-FAIL-002 | `CONFIRMED` | request 每 4 秒轮询；后端所有 task 都离开 `PENDING/QUEUED/IN_PROGRESS` 后聚合结果 | 任一 server task 为 `FAILED/TIMEDOUT/ABORTED`，整个 UI task 记 FAILED；否则 COMPLETED | progress mixin `POLL_INTERVAL/#parseLogs` |
| HA-COMMON-FAIL-003 | `CONFIRMED` | 点击 task title 可打开 host/task progress popup，查看按 host 聚合的任务与 stdout/stderr/output/error log | 只有 task 为 IN_PROGRESS/FAILED/COMPLETED 且有 request ID 时可打开；task detail 继续轮询至终态 | HA progress popup controller、common progress template |
| HA-COMMON-FAIL-004 | `CONFIRMED` | Retry 只选择首个 FAILED task，清 Retry/Rollback UI并把它重置为 PENDING | 已 COMPLETED task 不重放；该 task 成功后按原顺序继续后续 PENDING task | progress mixin `retryTask` |
| HA-COMMON-FAIL-005 | `CONFIRMED` | RM/RA 两 controller 都未调用 `onTaskErrorWithSkip`，因此没有可用 Skip | 通用 template 虽有 Skip 控件，但只有 task `canSkip=true` 才显示；不能把模板能力当成本向导能力 | progress mixin、RM/RA Step 4 controllers |
| HA-COMMON-FAIL-006 | `STATIC_ONLY` | `supports.autoRollbackHA` 可令失败 task 显示 Rollback 按钮，但继承实现硬编码 NameNode controller/rollback route | RM/RA wizard content 没有 `failedTask`/`saveFailedTask` 专用实现；不得把此静态按钮宣称为 RM/RA 自动回滚能力 | HA progress controller `rollback`、RM/RA controllers |
| HA-COMMON-REC-001 | `CONFIRMED` | 每次 task 状态变化保存 `tasksStatuses`、每 task request IDs、当前 request IDs，并以 `CLUSTER_CURRENT_STATUS` 持久化 localdb | persist 需要 `CLUSTER.MANAGE_USER_PERSISTED_DATA`；写失败会显示持久化错误，但业务 request 可能已发出 | progress mixin、WizardController、cluster states/Persist |
| HA-COMMON-REC-002 | `CONFIRMED` | 在 Step 4 重新连接时恢复已完成状态；若保存状态为 IN_PROGRESS，恢复 current task/request IDs 并继续轮询；QUEUED 则重新调用 task command | Retry/恢复基于前端快照，不撤销服务端已执行副作用 | progress mixin `loadTasks` |
| HA-COMMON-REC-003 | `NEEDS_RUNTIME_VALIDATION` | route 映射表包含 RM/RA controller 重定向，route enter 也能识别 `RM_HIGH_AVAILABILITY_DEPLOY`/`RA_HIGH_AVAILABILITY_DEPLOY` | `clusterStatus.validStates` 未列这两个 state；从服务端恢复 localdb 时只特殊信任通用 `HighAvailabilityWizard` 和 Kerberos namespace。刷新、崩溃、跨浏览器及跨用户恢复必须实测 | `data/controller_route.js`、`models/cluster_states.js`、RM/RA routes |
| HA-COMMON-REC-004 | `CONFIRMED` | Step 4 关闭 modal 显示“需人工完成或回滚”的确认；确认后只清前端 namespace/owner、设 `DEFAULT` 并 reload | 不调用 abort request，也不执行 RM/RA rollback；已经发出的服务端 request 会继续。Step 1-3 关闭则直接清状态，不显示 Step 4 critical warning | RM/RA routes、`resetOnClose` |
| HA-COMMON-REC-005 | `CONFIRMED` | RM Step 1-3 显式 `unroutePath=false`，Step 4 只有 `isFinished=true` 才允许离开；RA 四个 step 都未覆盖离开 hook | 全局 `Ember.Route.exitRoute` 默认直接执行 transition callback，RA 因此允许 route leave，不依赖 modal close；离开 route 不等于清 namespace、owner 或 abort request | RM/RA routes、`app/utils/ember_reopen.js#exitRoute` |
| HA-COMMON-REC-006 | `CONFIRMED` | Step 1-3 的 `setCurrentStep()` 会保存 localdb/current step，但 RM/RA override 未传 `clusterState` | deploy state 主要在 Step 4 task observer 写成 `RM_HIGH_AVAILABILITY_DEPLOY`/`RA_HIGH_AVAILABILITY_DEPLOY`；因此早期步骤的服务端状态不具备与 Step 4 相同的重定向/恢复语义 | RM/RA wizard controllers、progress mixin `onTaskStatusChange` |
| HA-COMMON-REC-007 | `STATIC_ONLY` | 菜单借 `App.isAuthorized` 阻止非 owner 启动，但两个 WizardRoute 的 direct URL guard 只检查登录；route enter 随后调用 `setUser` | 尤其 RA `enter` 无任何入口检查且无条件写 owner；具备 persisted-data 写权限的其他登录用户可通过 direct URL 尝试覆盖 `wizard-data` 的原 owner。React 必须在服务端或 route loader 原子校验 ownership | `app/router.js#WizardRoute`、RM/RA routes、wizard watcher |
| HA-COMMON-REC-008 | `CONFIRMED` | 菜单入口只明确要求 `SERVICE.ENABLE_HA`，而 owner/status/current-step persist 另要求 `CLUSTER.MANAGE_USER_PERSISTED_DATA` | 仅有前一权限时 `postUserPref` 立即 reject；Step 4 的 task queue/run 只从 persist success callback触发，所以首个或后续 PENDING task确定停止推进。React 应在进入前统一校验流程与恢复权限 | service action、Persist、progress mixin |
| HA-COMMON-KRB-001 | `CONDITIONAL` | 非 Kerberos或 Manual Kerberos 直接执行 callback；自动 Kerberos读取/缓存 `kdc_type` 后查询 KDC validation result | `admin.security.cluster_configs.kerberos` 最多等待 10 秒；session `OK` 才继续。两请求均无业务 error callback，非 KDC 普通 HTTP 失败只弹默认错误框且不触发 continue/cancel | main admin Kerberos controller |
| HA-COMMON-KRB-002 | `CONDITIONAL` | KDC invalid 或相关请求返回可识别的 KDC 400 错误时显示 credential popup；用户可输入 principal/password并选择 temporary/persisted store | persistent checkbox 依赖 credential store capability；保存使用 alias `kdc.admin.credential`，成功后重发原请求 | invalid KDC popup、credentials util、Ajax default KDC handler |
| HA-COMMON-KRB-003 | `CONFIRMED` | RM 在 Step 3 Next 和 Step 4 安装前各检查一次；RA 只在 Step 4 安装前检查 | 安装阶段关闭 invalid-KDC popup会调用 cancel handler，把当前 install task 置 FAILED；RM Step 3 取消仅留在本步。若 KDC 类型/session 遇到普通 HTTP 失败，cancel handler不执行，安装 task可停在 `QUEUED` 而非 `FAILED` | RM Step 3、progress mixin `createInstallComponentTask` |
| HA-COMMON-KRB-004 | `NEEDS_RUNTIME_VALIDATION` | 静态 UI 代码只注册并安装新增 RM/RA，未直接更新 cluster Kerberos descriptor，也未显式调用 regenerate keytabs | 新组件 principal/identity/keytab 是否由 Ambari Server 的 host-component 安装链自动同步，需分别对 MIT/AD/IPA 和 Manual 模式验证 | RM/RA controllers、[08-kerberos.md](08-kerberos.md) |
| HA-COMMON-KRB-005 | `STATIC_ONLY` | credential create/update 无论成功失败都由 `.always()` 转成 resolved promise，布尔成功值只是第一个 resolve 参数 | `clusterController.createKerberosAdminSession` 的 `.then()` 忽略该布尔值并重放原 KDC Ajax；credential 保存失败仍可能继续请求，根因可能被后续错误掩盖 | credentials util、cluster controller、invalid KDC popup |
| HA-COMMON-KRB-006 | `STATIC_ONLY` | `kerberos.session.state` success handler 直接调用 `res.toUpperCase()` | 响应缺少或返回 null `kdc_validation_result` 时会抛异常，而不是进入 popup、cancel handler或 task FAILED；React 应显式处理缺字段 | main admin Kerberos controller `checkState` |

## 组件安装请求链

RM 和 RA 的 Install Additional task 共用以下精确顺序。React 必须保留幂等与错误语义，不能只发送最后一个 PUT。

| ID | 等级 | 顺序与语义 | 请求 |
| --- | --- | --- | --- |
| HA-COMMON-INSTALL-001 | `CONFIRMED` | 查询目标 host 是否已有该 component；已有者跳过注册，全部已有仍继续做状态 PUT | `host_component.installed.on_hosts` |
| HA-COMMON-INSTALL-002 | `CONFIRMED` | 对确有缺失 component 的 host，先直接刷新全局 service-component count/host topology，再刷新 master/client host-component topology；第二个响应可能附带指标字段，但本基线只消费 service/component、host、state、maintenance、stale config、HA/admin state等运维字段 | `DIRECT:updateController.updateComponentsState`、`DIRECT:updateController.updateServiceMetric`；Metrics 数值排除 |
| HA-COMMON-INSTALL-003 | `CONFIRMED` | 刷新后若前端 service-component 列表仍无目标 component，则尝试创建 service-component | `common.create_component` |
| HA-COMMON-INSTALL-004 | `CONFIRMED` | `common.create_component` 无论成功失败都通过 `.always()` 继续；这是显式容错，不代表创建一定成功 | 同上 |
| HA-COMMON-INSTALL-005 | `CONFIRMED` | 对缺少 component 的 host 批量 POST host-component；success 和 error 都绑定 `onCreateComponent`，均继续最终 PUT | `wizard.step8.register_host_to_component` |
| HA-COMMON-INSTALL-006 | `CONFIRMED` | PUT 匹配 component/hosts 且 `maintenance_state=OFF` 的 host-components 到 `INSTALLED`，保存 request ID并轮询；此 PUT 或轮询失败最终决定 UI task FAILED | `common.host_components.update`、`background_operations.get_by_request` |
| HA-COMMON-INSTALL-007 | `NEEDS_RUNTIME_VALIDATION` | 查重 GET 没有 error callback；创建/注册失败又被继续处理，可能使 task 无 request、卡住或把真实根因延迟到 PUT；必须注入 404/409/500、部分 host 已存在、maintenance ON 等场景验证 | 共享 progress/install mixins |

## 后端接口契约

下表 URL 均以 `/api/v1` 为默认前缀；`{stackVersionUrl}` 通常为 `/stacks/{stack}/versions/{version}`。`DYNAMIC/PUT` 表示注册表无静态 type 或由 `format()` 改写，运行时实际为 PUT。

| 请求名 | Method 与 URL | 关键 payload / query | 触发点与响应处理 | 错误语义 |
| --- | --- | --- | --- | --- |
| `hosts.confirmed` | `GET /clusters/{clusterName}/hosts?fields=host_components/HostRoles/state&minimal_response=true` | 无 body | RA Step 2 首次初始化 namespace host 快照；映射 hostname、REGISTERED、installed、host-components | 默认 error popup；RA load map promise 不 resolve。RM 本流程不直接调用 |
| `hosts.high_availability.wizard` | `GET /clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true` | 无 body | 两向导 Step 2 获取选择资源；RM 外层 wizard view还用它建立 host 快照 | assign page 无专用 error；RM 外层 error 仅解除 spinner，可能留下空 host 数据 |
| `wizard.loadrecommendations` | `POST {stackVersionUrl}/recommendations` | `{hosts, services, recommend:'host_groups', recommendations:<blueprint>}` | Step 2 placement；响应 `resources[0].recommendations` 转为组件推荐 host 列表 | 调用默认错误 popup；Step 2 不进入 loaded/Next 状态 |
| `config.validations` | `POST {stackVersionUrl}/validations` | `{hosts, services, validate, recommendations}` | 共享 assign-master 能力存在 | RM/RA 都因 `useServerValidation=false` 明确不调用；React 不应新增而改变继续语义 |
| `config.tags` | `GET /clusters/{clusterName}?fields=Clusters/desired_configs` | 无 body | RM Step 3/4、RA Step 4取得 current tags | Step 4 error 把当前 task 置 FAILED；RM Step 3 error callback 可靠性需运行验证 |
| `reassign.load_configs` | `GET /clusters/{clusterName}/configurations?{urlParams}` | RM Review：`zoo.cfg/yarn-site/yarn-env`；RM save：单一 `yarn-site/yarn-client/core-site`；RA：存在 site 的 OR query | 返回 `items[].{type,properties,properties_attributes}`；作为变更基底 | Step 4 error 使 task FAILED；RM Review error 被送到 success handler，但完整降级不可靠 |
| `config.recommendations` | `POST {stackVersionUrl}/recommendations` | `{recommend:'configurations', hosts, services, recommendations:<blueprint with configurations>}` | RM Step 3把新增 RM 加入 blueprint，消费 proxyuser hosts recommendation | `.always()` 后仍显示 review；失败不会阻止 Next，但可能缺少 proxyuser 更新 |
| `common.service.configurations` | `PUT /clusters/{clusterName}` | `{Clusters:{desired_config:[{type,properties,properties_attributes?,service_config_version_note}]}}` | RM Step 4分别保存 YARN、条件 HAWQ、HDFS | success 完成当前 task；error 当前 task FAILED |
| `common.service.multiConfigurations` | `PUT /clusters/{clusterName}` | `[{Clusters:{desired_config:[...] }}, ...]` | RA Step 4一次保存全部真实存在 site | success 完成 task；error task FAILED；不是逐 site partial retry |
| `host_component.installed.on_hosts` | `GET /clusters/{clusterName}/host_components?HostRoles/component_name={componentName}&HostRoles/host_name.in({hostNames})&fields=HostRoles/host_name&minimal_response=true` | componentName、逗号分隔 hostNames | 安装前幂等查重，响应 host 列表与目标集合求差 | 无显式 error callback；失败/悬挂行为需运行验证 |
| `DIRECT:updateController.updateComponentsState` | `GET /api/v1/clusters/{clusterName}/components/?fields=ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true` | 无 body；由 `App.HttpClient.get` 直调 | 仅当目标 host 缺少 component 时，在 create service-component 前刷新全局 component 模型 | `complete` 无论 HTTP 成败都继续下一刷新；错误不直接置 HA task FAILED |
| `DIRECT:updateController.updateServiceMetric` | `GET /api/v1/clusters/{clusterName}/components/?<component filters>&ServiceComponentInfo/category.in(MASTER,CLIENT)&fields=<topology/state fields + conditional fields>&minimal_response=true` | 固定字段含 service、host、public host、state、maintenance、stale config、HA/admin state；filters/conditional fields 随已安装服务和 HA 状态变化 | 在上一直接 GET 完成后刷新 service/master component 模型，再决定是否 `common.create_component` | `complete` 无论 HTTP 成败都继续；响应中的任何 Metrics 字段均不进入本文功能/API基线 |
| `common.create_component` | `POST /clusters/{clusterName}/services?ServiceInfo/service_name={serviceName}` | `{"components":[{"ServiceComponentInfo":{"component_name":...}}]}` | 当前前端模型没有 service-component 时创建 | `.always()` 继续，不直接把 task 标失败 |
| `wizard.step8.register_host_to_component` | `POST /clusters/{cluster}/hosts` | `{RequestInfo:{query:'Hosts/host_name=h1\|...'},Body:{host_components:[{HostRoles:{component_name}}]}}` | 注册所有缺少 component 的 hosts | success/error 都继续 `onCreateComponent`，最后 PUT 决定 task |
| `common.host_components.update` | `PUT /clusters/{clusterName}/host_components?{urlParams}` | `RequestInfo.context/operation_level/query`；`Body.HostRoles.state='INSTALLED'`；query 含 component、hosts、`maintenance_state=OFF` | 安装新增 RM/RA并取得 `Requests.id` | HTTP error 当前 task FAILED；成功进入 request polling |
| `common.services.update` | `PUT /clusters/{clusterName}/services?{urlParams}` | `RequestInfo.context`、cluster operation level；`Body.ServiceInfo.state=INSTALLED/STARTED`；start 可带 `params/run_smoke_test` | RM 停非 HDFS、RA 停全部、两者启动全部 | HTTP error FAILED；成功保存 request ID并轮询；RM 最后 start failure可 Complete，RA 不可 |
| `background_operations.get_by_request` | `GET /clusters/{clusterName}/requests/{requestId}?fields=*,tasks/Tasks/...&minimal_response=true` | requestId | 4 秒轮询并计算进度；也用于 host progress popup | HTTP error当前 UI task FAILED；server task FAILED/TIMEDOUT/ABORTED 聚合 FAILED |
| `admin.security.cluster_configs.kerberos` | `GET /clusters/{clusterName}/configurations/service_config_versions?service_name=KERBEROS&is_current=true` | 10 秒 timeout | 自动安全集群首次判定 `kerberos-env/kdc_type`；缓存到 controller | 无调用侧 error callback；普通 HTTP 失败只显示默认错误框，原 callback不执行 |
| `kerberos.session.state` | `GET /clusters/{clusterName}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details` | callback 与 cancel handler仅为前端参数 | `OK` 执行原 callback；否则打开 invalid KDC popup | popup 取消：安装 task FAILED，RM Review 留在原步；可识别 KDC 400 也进入同 popup；其他 HTTP 失败不调用 cancel handler |
| `credentials.store.info` | `GET /clusters/{clusterName}?fields=Clusters/credential_store_properties` | 无 body | credential utility 的通用查询，RM/RA invalid-KDC popup实际不发此请求；popup alias读取 `App.isCredentialStorePersistent`，该值由 main 初始化时 direct `clusterController.loadClusterInfo` 映射 cluster model获得 | 不得把此注册请求计入 HA popup实际调用链；初始 cluster GET失败时全局值回落为 false |
| `credentials.list` | `GET /clusters/{clusterName}/credentials?fields=Credential/*` | 无 body | credential 管理通用能力，判断 alias 是否存在/类型 | 本向导通常由 popup的 create-or-update 路径直接用 `credentials.get` |
| `credentials.get` | `GET /clusters/{clusterName}/credentials/{alias}` | alias=`kdc.admin.credential` | 存在则走 update，不存在/GET fail 则走 create | GET fail 被解释为“不存在”，随后 POST；可能掩盖非 404 错误 |
| `credentials.create` | `POST /clusters/{clusterName}/credentials/{alias}` | `{Credential:{principal,key,type:'temporary'\|'persisted'}}` | invalid KDC popup Save 创建 live admin credential | create/update util以 `.always()` resolve布尔结果；cluster controller随后重发原 Ajax |
| `credentials.update` | `PUT /clusters/{clusterName}/credentials/{alias}` | 同 create | 更新现有 credential | 同上 |
| `credentials.delete` | `DELETE /clusters/{clusterName}/credentials/{alias}` | 无 body | credential 管理通用删除能力；HA向导本身不主动删除 | 错误留给通用 credential UI处理 |
| `persist.get` / `persist.get.text` | `GET /persist/{key}` | `CLUSTER_CURRENT_STATUS`、`wizard-data` 等 key | 恢复 cluster status/owner；text variant用于压缩数据通用能力 | 404 保持默认；其他错误弹 cluster-status error；RM/RA namespace恢复限制见风险项 |
| `persist.post` | `POST /persist` | `{key: JSON string}`；cluster status实际压缩成 Base64后写 `CLUSTER_CURRENT_STATUS` | current step、host/config选择、task/request IDs、wizard owner、完成/退出状态 | 要求 `CLUSTER.MANAGE_USER_PERSISTED_DATA`；失败不回滚已发业务 request，完成/退出仍在 always callback reload |

[generated/api-by-module/security-ha-federation.md](generated/api-by-module/security-ha-federation.md) 只是按请求名和 caller path 宽匹配的候选索引，可能混入或漏掉请求。权威网络核对必须联合 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。

## React 对照必测场景

| ID | 等级 | 场景与验收点 |
| --- | --- | --- |
| HA-STATIC-002 | `NEEDS_RUNTIME_VALIDATION` | RM 入口组合检查：RM stopped、ZooKeeper 只有 2 个、只有 2 台 host 同时失败时必须一次显示全部；3 个 stopped ZooKeeper 在旧 UI 仍通过该项，React 若加强必须显式记行为差异 |
| HA-STATIC-003 | `NEEDS_RUNTIME_VALIDATION` | RA 无前置检查场景：RANGER stopped、2 hosts、无 ZooKeeper仍可进入；随后失败点和恢复结果必须记录，不能假定与 RM 相同 |
| HA-STATIC-004 | `NEEDS_RUNTIME_VALIDATION` | RM 全新进入、刷新 Step 2、直接 URL进入 Step 2，验证 host snapshot 与 spinner，不允许出现 undefined `_host.bootStatus` 或空推荐 |
| HA-STATIC-005 | `NEEDS_RUNTIME_VALIDATION` | RA cardinality 为 `1-2`、`1+`、上限大于 host 数时验证 `+/-`、默认新增数量、当前 RA不可删、每 host单 RA |
| HA-STATIC-006 | `NEEDS_RUNTIME_VALIDATION` | RA LB URL 覆盖 http/https/ftp、端口/path/query、空值、无 scheme、无法解析/不可达、与 RA 共置；明确区分格式校验与外部设施验证 |
| HA-STATIC-007 | `NEEDS_RUNTIME_VALIDATION` | RM Review 的 `config.tags`、配置 GET、advisor recommendation分别注入失败，确认 Next、spinner、proxyuser和最终保存行为 |
| HA-STATIC-008 | `NEEDS_RUNTIME_VALIDATION` | 安装链注入 service-component create失败、host registration失败、PUT失败、查重GET失败、部分 host 已存在、maintenance ON，核对 Retry幂等性与最终 server拓扑 |
| HA-STATIC-009 | `NEEDS_RUNTIME_VALIDATION` | 在 Stop、Install、Reconfigure、Start request进行中分别关闭、刷新、崩溃、另开浏览器、换用户登录，核对 owner、重定向、轮询恢复和未 abort request |
| HA-STATIC-010 | `NEEDS_RUNTIME_VALIDATION` | RM 最后 Start failure可 Complete；RA同一失败必须不可 Complete。前置 task失败均只 Retry首个失败 task，且没有 Skip |
| HA-STATIC-011 | `NEEDS_RUNTIME_VALIDATION` | `supports.autoRollbackHA=true` 时验证 RM/RA Rollback 按钮的坏路径；React基线应默认不提供，除非实现并单独评审真正的 RM/RA rollback |
| HA-STATIC-012 | `NEEDS_RUNTIME_VALIDATION` | Kerberos 分别覆盖 NONE、Manual、MIT、AD、IPA、credential temporary/persisted、KDC失效取消/重输；安装后验证新增 RM/RA principals、keytabs、ownership和服务启动 |

## 源码证据索引

- 入口与权限：`ambari-web/classic/app/views/main/service/item.js`、`app/models/host_component.js`、`app/models/stack_service_component.js`、`app/controllers/main/admin/highAvailability_controller.js`。
- 路由与关闭：`app/routes/rm_high_availability_routes.js`、`app/routes/ra_high_availability_routes.js`、`app/router.js`。
- RM：`app/controllers/main/admin/highAvailability/resourceManager/*`、`app/data/configs/wizards/rm_ha_properties.js`、`app/utils/configs/rm_ha_config_initializer.js`。
- RA：`app/controllers/main/admin/highAvailability/rangerAdmin/*`。
- 共享 placement/校验：`app/mixins/wizard/assign_master_components.js`、`app/mixins/common/hosts/host_component_recommendation_mixin.js`、`app/views/common/assign_master_components_view.js`、`app/templates/common/assign_master_components.hbs`。
- 共享执行：`app/controllers/main/admin/highAvailability/progress_controller.js`、`app/mixins/wizard/wizardProgressPageController.js`、`app/mixins/main/host/details/host_components/install_component.js`、`app/mixins/wizard/wizardEnableDone.js`。
- Kerberos：`app/controllers/main/admin/kerberos.js`、`app/views/common/modal_popups/invalid_KDC_popup.js`、`app/utils/credentials.js`。
- 恢复：`app/controllers/wizard.js`、`app/models/cluster_states.js`、`app/data/controller_route.js`、`app/controllers/global/wizard_watcher_controller.js`。
- 接口注册：`ambari-web/classic/app/utils/ajax/ajax.js`。
