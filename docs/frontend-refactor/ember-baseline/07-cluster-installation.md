# 集群安装向导

入口 `/installer/step0` 到 `/installer/step10`。只有 `AMBARI.ADD_DELETE_CLUSTERS` 可以安装集群；无权限用户转 Views。向导使用 local DB 加服务端 cluster status 双重恢复，不是一次性表单。

## 安装模式与复用边界

“安装模式”必须按两个层次理解，React 不得把它们合并成一个只覆盖默认路径的表单：

1. 部署对象分为全新集群、向现有集群 Add Host、向现有集群 Add Service 三类向导。
2. 全新集群和 Add Host 的主机接入分为 Linux SSH 自动 bootstrap、HDPWIN PowerShell Remoting 自动 bootstrap，以及用户预先手工安装并启动 Agent 三条路径。HDPWIN 只是隐藏 SSH 字段，不会退化成手工注册。
3. 全新集群的仓库来源分为 Public Repository 与 Local Repository；Local Repository 又分为上传 VDF/XML 文件和输入 VDF URL。
4. 已启用 Kerberos 的 Add Host/Add Service 不是第四种安装向导，而是在 Review/提交前附加 KDC session、Kerberos descriptor、principal/keytab 处理的条件分支。

| ID | 部署模式 | 实际步骤 | 复用的核心 controller | 模式特有行为 |
| --- | --- | --- | --- | --- |
| INST-MODE-001 | 全新集群 | 0 Cluster Name；1 Version；2 Install Options；3 Confirm Hosts；4 Services；5 Masters；6 Slaves/Clients；7 Configs；8 Review；9 Deploy；10 Summary | `wizardStep0` 到 `wizardStep10` | 创建 cluster、选择 stack/version/repository、首次创建所有 service/component/config/host 关系，最后把 provisioning state 改为 `INSTALLED` |
| INST-MODE-002 | Add Host | 1 Install Options；2 Confirm Hosts；3 Slaves/Clients；4 Config Groups；5 Review；6 Deploy；7 Summary | 复用全新安装的 Step 2/3/6/8/9/10；另用 `addHostStep4Controller` | 不改 stack/service/master；只给新 hosts 分配 slave/client；有组件时将新 host 加入现有 config group，无组件时跳过 Config Groups；提交前检查 KDC session |
| INST-MODE-003 | Add Service | 1 Services；2 Masters；3 Slaves/Clients；4 Configs；5 Review；6 Deploy；7 Summary | 复用全新安装的 Step 4/5/6/7/8/9/10 | 过滤已安装 service；按服务 cardinality 可跳过 Master、Slave 或 Config 步；Kerberos 集群更新 descriptor，任何非空 `kdc_type` 都预取/显示 CSV，Manual 模式才把 principal/keytab 创建与分发交给用户 |
| INST-MODE-004 | Public Repository | 全新安装 Step 1 的默认分支 | `wizardStep1Controller` | 使用 version definition 中的默认 OS repository；仍需 URL/JDK/version validation |
| INST-MODE-005 | Local Repository + VDF/XML file | 全新安装 Step 1 的 local/upload 分支 | `wizardStep1Controller` | 读取本地文件内容并以 XML 提交 dry-run；通过后保存 VDF data，Review 提交时再次以非 dry-run 创建 version definition |
| INST-MODE-006 | Local Repository + VDF URL | 全新安装 Step 1 的 local/URL 分支 | `wizardStep1Controller` | 服务端从 URL 读取 VDF 并 dry-run；Review 时以非 dry-run 提交同一来源 |
| INST-MODE-007 | Linux SSH 自动 bootstrap | 全新安装 Step 2-3、Add Host Step 1-2 | `wizardStep2Controller`、`wizardStep3Controller` | UI/payload 收集 SSH private key、SSH user/port；sudo/passwordless sudo 是外部前置条件，不是 UI/payload 字段。`customizeAgentUserAccount=false` 时 Agent user 隐藏且 payload 强制 `root`；flag 开启且为自动安装时显示并必填。先轮询 bootstrap，再轮询 Agent registration；自动模式 registration timeout 为 120 秒 |
| INST-MODE-008 | 手工 Agent 注册 | 全新安装 Step 2-3、Add Host Step 1-2 | 同上 | 弹出人工安装说明；不发 bootstrap；初始 boot status 直接为 `DONE`，只轮询注册，manual registration timeout 为 15 秒 |
| INST-MODE-009 | Kerberized Add Host | Add Host Review/Deploy 条件分支 | `wizardStep8Controller`、`mainAdminKerberosController` | 提交前获取 KDC session state；后端安装流程为新 host 创建/分发身份材料；失败不能绕过部署状态机 |
| INST-MODE-010 | Kerberized Add Service | Add Service Config/Review 条件分支 | `wizardStep7Controller`、`wizardStep8Controller` | 读取 security status，校验并更新 cluster Kerberos descriptor；Review 对任何非空 KDC type 预取并提供 CSV，Manual 模式额外要求用户在部署前人工创建/分发 principals/keytabs |
| INST-MODE-011 | HDPWIN PowerShell 自动 bootstrap | 全新安装 Step 2-3、Add Host Step 1-2 | `wizardStep2Controller`、`wizardStep3Controller`；服务端 `BootstrapWindows`/`PSR` | `useSSH=false` 使 SSH key/user/port 和 Agent user 整块 UI 隐藏，但默认 `manualInstall=false`，因此仍向 `/bootstrap` POST；默认 payload 为 `sshKey=""`、SSH `user=""`、`sshPort="22"`，`customizeAgentUserAccount=false` 时 `userRunAs="root"`。flag 开启时隐藏的 Agent user 为空，既成为 `userRunAs` 又触发必填校验并禁用 Next，这是旧版缺陷。服务端按 Windows OS family 以 PowerShell Remoting 执行 bootstrap |

### 核心步骤复用矩阵

| 旧版核心能力 | 全新集群 | Add Host | Add Service | 条件跳转 |
| --- | --- | --- | --- | --- |
| Repository/version | Step 1 | 不进入，沿用 cluster version | 不进入，读取已安装 version definition 以显示 service version | Public/Local、file/URL |
| Install Options/Confirm Hosts | Step 2/3 | Step 1/2 | 不进入 | Linux SSH/HDPWIN PowerShell/manual |
| Choose Services | Step 4 | 不进入 | Step 1 | 只显示未安装且可安装 service |
| Assign Masters | Step 5 | 不进入 | Step 2 | `skipMasterStep` 时 Add Service 从 Step 1 到 Step 3 或更后 |
| Assign Slaves/Clients | Step 6 | Step 3 | Step 3 | `skipSlavesStep` 时 Add Service 跳过；Add Host 仍进入以允许只装 client/不装组件 |
| Customize Configs | Step 7 | Config Groups 专页 Step 4 | Step 4 | Add Host 无组件直接到 Review；Add Service `skipConfigStep` 时跳过 |
| Review/Create resources | Step 8 | Step 5 | Step 5 | Submit 先写 `*_DEPLOY_PREP_2` checkpoint，再运行清理/创建链；Step 8 不写 provisioning state |
| Install/Start/Test | Step 9 | Step 6 | Step 6 | Retry 只在 `INSTALL FAILED` 显示并重新调用 install；`START FAILED` 没有 Retry |
| Summary | Step 10 | Step 7 | Step 7 | 降低步骤导航全部禁用；Complete 清理对应向导 |

### 可返回、可重试与不可逆边界

| 阶段 | Back/Cancel | Retry | 数据与副作用边界 |
| --- | --- | --- | --- |
| 选择与分配阶段 | 可返回；改 stack/service/host 会清除下游 recommendations、assignments、configs | 重新 validation/recommendation | 主要是 local DB/persist 数据，尚未创建 cluster 资源 |
| Confirm Hosts | 可返回；离开时停止 bootstrap polling；已启动或注册的 Agent 不由 UI 自动卸载 | 可只重试失败 hosts，或移除失败 hosts | bootstrap/Agent 注册已经产生 host 侧副作用，但 host 尚未加入 cluster |
| Review 提交前 | 可返回到最后一个适用配置/分配步骤；有配置变更时确认丢弃 | repo/preinstall/descriptor 校验可重做 | 尚可安全重算 Blueprint 和 configs |
| Review 提交后 | lower steps 禁用；资源 queue 失败会重新开放 Submit/Back，但不是回滚 | 重新 Submit 会从当前客户端数据重建并重放整条创建 queue | 已成功的 cluster/services/components/configs/host-components 不回滚，重放可能命中既有资源 |
| Deploy | 全新 Installer 只允许代码定义的 Admin View/Views 例外；Add Host/Add Service 的 `unroutePath()` 一律返回 false | Retry 按钮只在 `INSTALL FAILED` 显示；`START FAILED` 无 Retry。Add Host/Add Service 还允许以 `INSTALL FAILED` 进入 Summary | server request/task 是事实来源；只有仍停留 Deploy 的 `INSTALL FAILED` 有经典 UI 重试入口 |
| Summary | 正常 UI 只有 Complete，没有 Back/Retry；route 中虽残留 `back` handler，模板无入口且 lower steps 被全部禁用 | 必须在 Deploy 页完成重试；进入 Summary 后不能通过经典 UI 返回查看 task | 全新集群 Complete 才把 provisioning state 设为 `INSTALLED`；Add Host/Add Service Complete 刷新已有集群模型 |

### 服务端恢复状态

| 向导 | 服务端 `clusterState` | 恢复步骤/行为 |
| --- | --- | --- |
| 全新安装 | `CLUSTER_DEPLOY_PREP_2` | 恢复 Review/已提交准备阶段，禁止重新编辑低阶步骤 |
| 全新安装 | `CLUSTER_INSTALLING_3`、`SERVICE_STARTING_3` | 恢复 Deploy Step 9，继续轮询当前 request |
| 全新安装 | `CLUSTER_INSTALLED_4` | 恢复 Summary Step 10；尚待 Complete 设置 provisioning state |
| Add Host | `ADD_HOSTS_DEPLOY_PREP_2` | 经典 route 实际映射 Step 4 Config Groups，比概念上的 Review 早一步；这是精确恢复行为 |
| Add Host | `ADD_HOSTS_INSTALLING_3`、`SERVICE_STARTING_3` | 实际映射 Step 5 Review，而不是正在运行请求的 Deploy Step 6 |
| Add Host | `ADD_HOSTS_INSTALLED_4` | 实际映射 Step 6 Deploy，而不是 Summary Step 7 |
| Add Service | `ADD_SERVICES_DEPLOY_PREP_2` | 恢复 Review Step 5 |
| Add Service | `ADD_SERVICES_INSTALLING_3`、`SERVICE_STARTING_3`、`ADD_SERVICES_INSTALLED_4` | 三者都直接转 Step 7 Summary；Summary controller 只从已持久的 hosts/tasks 生成摘要，不重启 Step 9 request polling，因此活动安装/启动状态可被过早降级成静态 Summary |

恢复由三个来源共同决定：服务端 cluster status 保存 `clusterState` 与 `wizardControllerName`；local DB/服务端 persist 保存 `currentStep` 和输入数据；`wizardWatcherController` 判断当前用户是否为向导所有者。另一个窗口或非所有者用户不能启动第二个冲突向导，而是被路由到当前向导或按 non-wizard user 限制。`app/data/controller_route.js` 是已安装集群中所有可恢复长流程的注册表；安装、Add Host 和 Add Service 分别还在自己的 route 中将服务端状态映射回实际 step。

### 三类向导入口门禁

| ID | 入口行为 | UI gate | Route gate 与深链边界 | 主要证据 |
| --- | --- | --- | --- | --- |
| INST-ENTRY-001 | 全新 Installer `/installer/step0` 到 `step10` | 安装入口只向 `AMBARI.ADD_DELETE_CLUSTERS` 暴露 | `installer` route 自身再次硬校验该权限；失败转 Admin View/Views，因此深链不能绕过 | `app/routes/installer.js`、`app/router.js` |
| INST-ENTRY-002 | Add Service `/main/service/add/step1` 到 `step7` | Service Actions 同时要求 `SERVICE.ADD_DELETE_SERVICES` 和 `supports.enableAddDeleteServices` | `addService` route 再次校验相同 permission 与 flag；深链不能绕过 | `app/templates/main/service/all_services_actions.hbs`、`app/routes/add_service_routes.js` |
| INST-ENTRY-003 | Add Host `/main/host/add/step1` 到 `step7` | Host bulk-operation 菜单要求 `HOST.ADD_DELETE_HOSTS` | `addHost` route 没有 permission 或 feature gate；直接 URL 可创建 modal 并进入流程，是必须由 React route/mutation 层修复的旧越权边界 | `app/templates/main/host/bulk_operation_menu.hbs`、`app/routes/add_host_routes.js` |

## 进入、恢复与取消

| ID | 功能与行为 | 前置/边界 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| INST-FLOW-001 | 登录后无已安装集群进入 Installer；已有未完成状态恢复对应 step | 检查 server/web client version、supports、权限和 cluster provisioning state | router cluster/version/status 请求 | `app/routes/installer.js`，`app/router.js` |
| INST-FLOW-002 | 每步保存 currentStep、已选 stack/services/hosts/components/configs/recommendations 到 local DB/persist | 前进保存当前数据；后退加载 prior steps；不能直接跳到未完成高阶 step | persist/cluster status 请求 | installer/wizard controllers、DB/persist mixins |
| INST-FLOW-003 | 另一窗口或崩溃后根据 `wizardControllerName`、clusterState 和 currentStep 恢复 | 非向导发起用户可被限制为只读/non-wizard user | cluster status/persist | `app/data/controller_route.js`、wizard watcher |
| INST-FLOW-004 | Cancel Install 显示确认，确认后只路由到 `/adminView` | `cancelInstall()` 不清 wizard/local/persist/cluster status，不删集群，也不等待任何清理；这与 Review Submit 的既有集群删除链是两件事 | 无后端清理请求；Admin View 整页导航 | `app/controllers/installer.js#cancelInstall`、`app/controllers/application.js#goToAdminView`、controller test |
| INST-FLOW-005 | 防止 Back/Next 双击；request 运行中禁用导航 | router 级 `btnClickInProgress`，异步完成后复位 | 无 | `app/router.js`、installer routes |
| INST-FLOW-006 | 向导所有权与多窗口恢复 | `wizardWatcherController` 读取/设置当前 user；非向导用户不能并行更改；崩溃或新窗口按 `wizardControllerName` 回到长流程 | wizard user/status 请求 | `app/data/controller_route.js`、`app/router.js` |

## Step 0 Get Started

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-0-001 | 输入 cluster name | 必填、长度、空白、特殊字符校验；无可用 stack 时不能继续 | `wizard.stacks` |
| INST-0-002 | 加载可安装 stack 列表并初始化向导数据 | load 失败显示错误；清前一次 stack/repo 选择 | `wizard.stacks`、version definitions |

## Step 1 Select Version

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-1-001 | 选择 stack 与 version definition | 只显示 `show_available=true`；支持 default version；没有定义时退回前一步 | `wizard.stacks_versions_definitions` |
| INST-1-002 | 选择 Public Repository | 网络不可用时提示并可切 Local；显示各 OS repo | version definition/repository load |
| INST-1-003 | 选择 Local Repository，通过上传 VDF/XML 或输入 VDF URL 添加 version | dry-run 校验 stack/version/OS/repositories；stack 改变时提示重置 | `wizard.step1.post_version_definition_file.xml`、`.url` |
| INST-1-004 | 按 OS 编辑 Base URL、添加/移除 OS、恢复默认或清空 | OS/repo ID 唯一；URL 格式和可达性校验；可显式 skip validation | `wizard.advanced_repositories.valid_url` |
| INST-1-005 | Retry repository validation | 网络恢复后重跑全部失败 repo | repository validate 请求 |
| INST-1-006 | 校验 Ambari Server JDK 与 version definition 的 `min_jdk`/`max_jdk` 范围 | 仅非 Custom JDK 且定义了范围时比较版本；不兼容不硬阻断，只弹危险样式确认并以 `Proceed Anyway` 继续。Custom JDK 因没有可比较的 `java.version` 而跳过范围校验 | Ambari server properties、`wizard.stacks_versions_definitions` |
| INST-1-007 | Local Repository 可切换 RedHat Satellite/Spacewalk 管理仓库模式 | Public Repository 时控件禁用；启用必须确认，关闭则直接切换。启用后禁止 Base URL/OS add/remove/skip-validation 控件，允许空 URL并以 `verify_base_url=false` 跳过 URL 验证 | Review 提交 version definition 时写 `ambari_managed_repositories=false`；关闭时保持默认 managed repository 语义 |

## Step 2 Install Options

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-2-001 | 输入 host names，支持 `[01-10]` pattern 展开 | 读取值时先统一 `toLowerCase()`，再执行 `trim().split(/\s+/g)`、pattern 展开、去重和格式校验；只按空白切分，逗号不是分隔符。混合输入已安装与新 hosts 时过滤旧 hosts、弹提示后仍可继续新 hosts；只有过滤后一个新 host 都不剩才以 already-installed 错误阻断 | 已持久 hosts/local DB；本步无 host 查询 mutation |
| INST-2-002 | 选择 Linux SSH 自动安装 Ambari Agent | SSH private key、SSH user/port 必填。Agent user 不是可选字段：feature flag 关闭时隐藏并强制 payload=`root`；开启时自动安装必须填写。sudo/passwordless sudo 仅是外部主机前置条件，不是 UI/payload 字段 | 后续 `wizard.launch_bootstrap` |
| INST-2-003 | 选择手工注册 Agent | 显示 manual install instructions 与 Ambari Java Home；等待 hosts 自行注册 | Ambari properties/host registration load |
| INST-2-004 | 对无点 hostname/IP 等可疑输入弹 warning，用户可返回修改或确认继续 | 校验对象已经小写化；已安装 hosts 使用另一提示框，混输时只过滤旧 hosts 而不阻断新 hosts | 无新增请求 |
| INST-2-005 | Add Host 独有 `Skip host checks` checkbox | checkbox 一经勾选就已改变状态，随后弹窗只有 OK、没有 Cancel，不是可撤销的二次确认。它跳过 hostname-resolution 和通用 preinstalled checks；进入 Confirm Hosts 后 `startHostcheck()` 仍独立执行 JDK check。全新 Installer 无此控件 | checkbox 本身无请求；未跳过时首次检查使用 `preinstalled.checks`/`.tasks`，JDK 仍用 `wizard.step3.jdk_check` |

## Step 3 Confirm Hosts

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-3-001 | Linux SSH 或 HDPWIN PowerShell 自动模式启动 bootstrap，轮询每 host 的 RUNNING/DONE/FAILED 与日志 | 两种自动模式都 POST 同一 `/bootstrap`；POST 失败可 retry，离开 step 停止 bootstrap polling | `wizard.launch_bootstrap`、`wizard.step3.bootstrap` |
| INST-3-002 | 轮询 Agent registration，将 DONE 转 REGISTERING/REGISTERED，超时标失败 | 最后一个 bootstrap 完成后按 registration timeout 计算 | `wizard.step3.is_hosts_registered` |
| INST-3-003 | 展示 host 状态分类和单 host bootstrap `bootLog` | 可按状态筛选；弹窗只提供高亮、只读日志文本，不具备 Step 9 task output 或 open-new-window 能力 | bootstrap/host data |
| INST-3-004 | 单个/多选失败 host Retry，重新 bootstrap/注册 | 正在运行项不能 retry | bootstrap requests |
| INST-3-005 | 单个/多选 Remove host | 确认后只删除 controller/local DB/内存中的 host；不注销 Agent，也不 DELETE 服务端 registered host。至少一个 host 为 `REGISTERED` 才能继续 | 无后端请求 |
| INST-3-006 | 运行 host checks：hostname resolution、last-agent-env、installed packages、existing repos、THP，并由 host info 补 OS/disk 分类 | 首次检查通过 `preinstalled.checks` 创建 request、以 `preinstalled.checks.tasks` 轮询；`wizard.step3.rerun_checks` 只在用户触发 Rerun 后用于最后刷新 `last_agent_env`。展示层合并通用 `warnings` 与 hostname-resolution、JDK、repo、disk、THP 等独立集合，但 Submit 只在通用 `warnings.length>0` 时弹确认；其他集合只影响展示，不确认也不硬阻断。Next 的硬条件只是至少一个 host 为 `REGISTERED` | `preinstalled.checks`、`preinstalled.checks.tasks`；重跑另有 `wizard.step3.rerun_checks` |
| INST-3-007 | 检查 JDK 并展示 host-specific warning | request/task 两阶段；只解析 `structured_out.java_home_check.exit_code` 生成 warning，没有 stderr/error log 入口 | `wizard.step3.jdk_check`、`.get_results` |
| INST-3-008 | 显示不在当前输入中的其他 registered hosts | 用户可确认/检查，避免遗漏已注册 agent | hosts registration load |

## Step 4 Choose Services

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-4-001 | 列出 stack installable services，单选/全选/取消，按 stack 可用 file system 分组 | Ambari Metrics 等 Metrics service 不作为本文功能要求，但服务依赖校验仍遵循 stack metadata | `wizard.service_components` |
| INST-4-002 | 自动提示并选择 required dependencies | 缺少依赖可阻止或弹确认；已有/内置 dependencies 不重复 | stack dependency metadata |
| INST-4-003 | 校验 file system 选择、多个 DFS、Ozone/Spark/Ranger 等互斥/建议组合 | CRITICAL 必须修复；WARNING 可明确接受 | client-side + stack metadata |
| INST-4-004 | Choose Services 组合校验完全在客户端 | `validate()` 只执行 dependency、filesystem、Spark 和若干特定服务检查，通过 error stack/popup 阻止或要求接受；本步不发 Stack Advisor validation API | 无；`config.validations` 首次属于后续 host/component layout 校验 |

## Step 5 Assign Masters

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-5-001 | Stack Advisor 推荐 master component 到 hosts 的初始布局 | 考虑 CPU/memory/disk、cardinality、co-host rules 和已有组件 | `config.recommendations`/stack advisor |
| INST-5-002 | 用户为每个 master 更换 host，并显示当前 assignment 可匹配的校验问题 | 客户端约束可禁用不合格选项；服务端结果只保留 `type=host-component` 且 component 与 selected host 同时匹配当前 master assignment 的项。general issue、其他 type 和无法匹配的 host-component issue 被丢弃；匹配项的 ERROR/WARN 都会标到 master，且两种级别都允许 `Continue Anyway` | `config.validations` |
| INST-5-003 | 动态 services/components 变化后清旧 recommendations 并重算 | 返回 Step 4 后重新进入必须刷新 | recommendation request |

## Step 6 Assign Slaves and Clients

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-6-001 | 以 host x component 矩阵分配 slave/client，支持列 All/None | required/依赖 component 自动勾选或禁用 | host/component metadata |
| INST-6-002 | 合并 Master 与 Slave/Client selection 形成 Blueprint | 不可见但必须部署的 component 仍保留 | 无 mutation |
| INST-6-003 | 服务端校验 Blueprint，按 general/host/component 显示 ERROR 与 WARN，并在 host x component 矩阵标色 | 旧 UI 对任意 ERROR/WARN 都弹危险样式的 Continue Anyway/Cancel；不是 ERROR 硬阻断。React 若改为禁止绕过 ERROR，必须记为有意安全差异 | `config.validations` |
| INST-6-004 | 恢复 recommendations 和已选 hosts | 返回/刷新时不丢失人工修改 | persist/local DB |

## Step 7 Customize Services

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-7-001 | 按 service/theme/category 配置所有 selected services | 与 Service Configs 控件、校验和 theme 语义一致 | `configs.theme.services`、stack configs |
| INST-7-002 | Accounts、Credentials、Databases、Directories 专用 tabs | Service/stack 决定 tab；password/confirm、DB type/host/port 等校验 | configs/recommendations |
| INST-7-003 | Stack Advisor recommendations、dependent config changes、required changes | required recommendation 不可拒绝；warning/validation issue 展示计数和筛选 | `config.recommendations` |
| INST-7-004 | 测试数据库连接和外部依赖 | Hive/Oozie/Ranger 等条件功能 | DB/custom action requests |
| INST-7-005 | 加载已存在 host overrides/config groups（Add Service/Host 复用路径） | 新集群通常无 override；复用 controller 时必须保持行为 | config groups/overrides |
| INST-7-006 | Pre-Install Checks 仅为空壳 | `supportsPreInstallChecks` 条件下，Run 只把 `preInstallChecksWhereRun=true` 并打开空 body modal；没有结果模型、errors/warnings 或严重级别阻断，未运行时 primary 仍可直接 skip | 无 AJAX；`PLACEHOLDER` |
| INST-7-007 | Config 改变可增加 components/改变 host assignment，并回写后续 Review blueprint | 动态 component 需要重新 recommendations/validation | recommendations/validation |
| INST-7-008 | 后退且已有修改时确认是否丢弃 | 避免 silent data loss | 无 |

## Step 8 Review

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-8-001 | 汇总 cluster、repositories、services、masters、slave/client、hosts 和 configs | 提供展开 host 列表 | load prior steps |
| INST-8-002 | Print Review | 浏览器打印当前 review | 无 |
| INST-8-003 | Download CSV | Add Service Review 对任何非空 `kdc_type`（MIT/AD/IPA/Manual）预取并显示；Manual 特有的是人工 principal/keytab 责任与 warning，不是 CSV 按钮本身 | `admin.kerberos.cluster.csv`；内存文本浏览器下载 |
| INST-8-004 | Generate Blueprint ZIP | 从当前 assignment/config 生成 `blueprint.json` 与 `clustertemplate.json`，确认后一起打包下载；cluster template 含 host 实例、`NEVER_APPLY` 与 `INSTALL_AND_START`。经典 Installer 只有这个导出能力，没有 Blueprint/cluster-template 导入入口 | 本地 ZIP/Blob，无新后端请求 |
| INST-8-005 | Submit 先查询所有既有 cluster | Installer 模式不是只拒绝同名；`GET /clusters` 返回的每个 cluster name 都会分别 DELETE，全部成功才进入 repository-version/resource 创建。GET 失败时 custom error callback 虽把 `clusterNames=[]`，但 jqXHR 保持 rejected，而调用方只有 success-only `.then()`：不继续、不弹此链专用错误、也不解锁 Step 8 | `wizard.step8.existing_cluster_names`、每 cluster 一次 `common.delete.cluster` |
| INST-8-008 | 既有集群删除是非事务批量副作用 | DELETE 并行发送，部分成功/部分失败时无回滚；全部请求 settled、`clusterDeleteRequestsCompleted` 达到 cluster 总数后聚合失败视图并弹 popup，停留 Review。这是旧版为单集群模型保留的破坏性逻辑，React 不得将其弱化成“同名检查” | `GET /api/v1/clusters`，`DELETE /api/v1/clusters/{clusterName}` |
| INST-8-009 | 全局 Version Definition/Repository Version 清理链 | 所有 cluster 删除成功后 `GET /version_definitions`；全新 Installer 枚举返回的每项，以其 stack name/version/id 并行 DELETE 对应 repository version，计数全部归零才部署。DELETE 没有 error callback；任一失败都不会递减计数，页面永久保持锁定，已删除项无回滚 | `wizard.get_version_definitions`；`DELETE /stacks/{stackName}/versions/{stackVersion}/repository_versions/{id}` (`wizard.delete_repository_versions`) |
| INST-8-006 | 将 cluster、services、components、configs、hosts、host-components、config groups 等创建请求按依赖加入串行 queue | queue 默认 `abortOnError=true`；任一请求失败即清空剩余 queue、关闭进度框、停留 Review，并重新开放 Submit/Back/步骤。已成功创建的资源不回滚，重新 Submit 会重建并重放请求 | `wizard.step8.create_cluster`、`.create_selected_services`、`.create_components`、`.register_host_to_cluster`、`.register_host_to_component`、`.apply_configuration_groups`、config PUT |
| INST-8-007 | Local Repository 提交 selected VDF（非 dry-run），随后更新 repository OS 信息 | VDF URL/XML POST 失败会弹错、清除本地 VDF data，promise rejected 且 Review 仍锁定；成功后 OS repository PUT 即使失败也强制 resolve，继续资源 queue。Step 8 只保存 `*_DEPLOY_PREP_2` cluster status，不写 provisioning state | `wizard.step8.post_version_definition_file(.xml)`、`admin.stack_versions.edit.repo`、cluster status persist |

### Step 8 提交与失败矩阵

| 阶段 | 成功转移 | 失败行为 | 回滚/重提边界 |
| --- | --- | --- | --- |
| Submit/KDC checkpoint | 禁用 Submit、Back 与低阶步骤，发起持久化 `CLUSTER_DEPLOY_PREP_2`、`ADD_HOSTS_DEPLOY_PREP_2` 或 `ADD_SERVICES_DEPLOY_PREP_2`，但不等待该请求完成就进入后续链 | Add Host/Add Service 的 KDC session 获取失败会重新开放按钮/步骤；Installer 无 KDC session gate。checkpoint persist 失败走默认 persist 错误提示，但 GET/DELETE/创建链已经启动且不被阻断 | checkpoint 是恢复标记，不是 provisioning `INSTALLED` |
| Existing clusters GET | Installer 获取全量 cluster names；Add Host/Add Service 仍执行 GET，但不会删除现有 cluster | jqXHR rejection 使 success-only `.then()` 不运行；无专用 popup/解锁，静默卡在 Review | 无创建副作用，但用户不能在当前页面重提 |
| Existing clusters DELETE | 每个 cluster 并行 DELETE；全部请求 settled 且完成计数达到总数、无错误时继续全局版本清理 | 聚合 cluster DELETE errors 后弹 popup、停留 Review；已成功 DELETE 的 cluster 不恢复 | 这是唯一有聚合 error views 的前置清理阶段 |
| Version definitions GET/DELETE | 三类向导都 GET definitions；只有全新 Installer 才按 stack/version/id 删除所有对应 repository versions，成功计数减到 0 后继续；Add Host/Add Service 直接进入资源 queue | GET 失败只走默认 AJAX 错误处理，没有 continuation 或解锁，页面仍锁住；DELETE 缺少 error callback，任一失败使计数永不归零并锁页 | 已成功 DELETE 的版本资源不恢复 |
| 非 dry-run VDF POST | 保存新 definition，取得 id/stack 后 PUT repository OS data | POST 失败弹错并清 VDF data、锁页；2xx 缺少 `resources[0].VersionDefinition` 时 deferred 永不 settle，存在该对象但缺 id/stack name/version 时虽 resolve、后续 guard 却不执行也无 fallback，两者都静默锁页；repository OS PUT 失败被强制当完成，部署继续 | 无事务，也不会恢复刚删除的版本资源 |
| Resource AJAX queue | 全部成功后路由调用 install request，写 `*_INSTALLING_3` 并进入 Deploy | 任一失败中止剩余 queue、登记错误 popup、停留 Review、重新开放 Submit/Back | 已成功资源不回滚；重提会重放整条 queue |
| Add Host config-group assignment | Resource queue 完成并路由 `next` 后，`applyConfigGroup()` 在 queue 外直接发送既有 config-group PUT，紧接着触发 install；这是串行 resource queue 之外的并发例外 | 更新是 fire-and-forget：失败不可见，也不阻断并发的 install、状态保存或进入 Deploy | UI 不提供局部 retry；服务端 config group 可能未包含新 host |
| Install request handoff | Resource queue 完成后调用全新 Installer/Add Host/Add Service 各自的 install mutation，回调保存 `*_INSTALLING_3` 并进入 Deploy | install mutation 的 rejection 也被接到同一推进 callback；错误处理只记 `isInstallError`/弹错，仍进入 Deploy。此时可能没有新 request id，Step 9 仍按 `PENDING` 尝试轮询，是旧版失败恢复缺口 | 不会回到 Review，也不是 resource queue 的可重提失败 |

## Step 9 Install, Start and Test

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-9-001 | 分阶段 Install、Start、Service Check，按 service/host/task 展示进度 | service check 可由配置开关关闭；状态机驱动 UI | common service/component update、request polling |
| INST-9-002 | 展示每 host 的 tasks、command detail、stdout/stderr/error log，可复制/新窗口 | 大日志按 task 懒加载 | `wizard.step9.load_log`、background task APIs |
| INST-9-003 | 按 status 分类 hosts，显示 failed hosts details | 失败不自动进入 Summary | `wizard.step9.installer.get_host_status` |
| INST-9-004 | Retry failed install | Retry 按钮严格由 `status === 'INSTALL FAILED'` 控制，并重新调用 `installServices`；`START FAILED` 没有 Retry，只能以 Next 进入 Summary。Add Host/Add Service 的 Next 状态集合额外包含 `INSTALL FAILED`，因此也能放弃重试直接进入 Summary；全新 Installer 的 `INSTALL FAILED` 不能 Next，但 observer 会重新启用其前序步骤链接 | common update/request APIs |
| INST-9-005 | 部署期间禁止普通 route 离开 | 只有全新 Installer 的 route guard 对 `/adminView`、Views routes 放行；Add Host/Add Service Deploy 一律 `return false`，没有 Admin View/Views 例外 | route guard |
| INST-9-006 | Deploy Next 保存完成阶段 cluster state 并进入 Summary | 全新 Installer 写 `CLUSTER_INSTALLED_4`；Add Host 写 `ADD_HOSTS_INSTALLED_4`，即使状态 persist 失败也通过 `alwaysCallback` 进入 Summary；Add Service 发起 `ADD_SERVICES_INSTALLED_4` persist 后不等待就进入 Summary。三者都仍未设置 provisioning `INSTALLED` | cluster status persist |

## Step 10 Summary

| ID | 功能与行为 | 校验/边界 | 后端请求 |
| --- | --- | --- | --- |
| INST-10-001 | 汇总各 services 的安装、启动、check 成功/警告/失败 | 允许带 warning 完成；模板只有 Complete，lower steps 已 disabled，没有可达的回 Step 9 查看/重试入口 | prior persisted request results，Summary 不启动新 polling |
| INST-10-003 | Summary 保留不可达 `back` route handler | Installer、Add Host、Add Service route 都定义返回 Deploy 的 handler，但共享 Step 10 template 无 Back 按钮，且 `setLowerStepsDisable()` 阻止侧栏 `gotoStep()` | `STATIC_ONLY`；不能当成用户功能 |
| INST-10-002 | 全新 Installer Complete 清向导、请求将 cluster provisioning state 设为 `INSTALLED`、clusterState 重置 `DEFAULT`，进入 Dashboard | 只有全新 Installer 执行 provisioning PUT；使用 `.complete()`，所以 PUT 成功或失败都会继续重置状态并进入 Dashboard。Add Host/Add Service Complete 只关闭 modal/刷新已有集群流程，不写 provisioning | `cluster.save_provisioning_state`、cluster status persist；Add Host/Add Service 无该 PUT |

启发式模块接口索引见 [generated/api-by-module/installation-wizards.md](generated/api-by-module/installation-wizards.md)。该页按请求名与 caller path 宽匹配生成，可能混入跨模块请求，也可能漏掉 Add Host/Add Service 独占调用，不能当作接口全集；权威核对必须联合 `generated/ajax-endpoints.md`、`generated/ajax-calls.md`、`generated/direct-http-calls.md`、`generated/browser-network-entrypoints.md` 与 `generated/realtime-channels.md`。
