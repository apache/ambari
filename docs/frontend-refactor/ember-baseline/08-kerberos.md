# Kerberos 安全基线

经典 Ember 的入口是 `/main/admin/kerberos`，Enable 向导是 `/main/admin/kerberos/enable/step1` 到 `step8`，Disable 是 `/main/admin/kerberos/disableSecurity`。Enable 是登记到全局向导恢复表、会停止整个集群并改变 cluster security type 的长流程；Disable 虽复用 progress controller，却只是 route 内 modal，不具备同等级的自动恢复保证。两者都不是普通配置表单。

## 入口、权限与前置检查

| ID | 功能与行为 | 前置/分支 | 后端请求 | 主要证据 |
| --- | --- | --- | --- | --- |
| KRB-ENTRY-001 | 加载 cluster security type；`KERBEROS` 显示已启用页面，否则显示 Enable | 状态请求失败显示 error popup；加载中显示 spinner | `admin.security_status`、`admin.security.cluster_configs.kerberos` | `app/controllers/main/admin/kerberos.js`、`app/templates/main/admin/kerberos.hbs` |
| KRB-ENTRY-002 | 访问页面、显示 Enable/Disable/Edit 受权限和 feature flag 双重控制 | 必须 `CLUSTER.TOGGLE_KERBEROS` 且 `supports.enableToggleKerberos`；否则 route 转 Dashboard | 同上 | `app/routes/main.js`、`app/templates/main/admin/kerberos.hbs` |
| KRB-ENTRY-003 | Enable 前逐条展示已安装服务的特殊 warning | 当前代码对 YARN 有专用提示；取消任一 warning 即不启动向导 | 无 | `app/controllers/main/admin/kerberos.js#checkServiceWarnings` |
| KRB-ENTRY-004 | 可选 Pre-Kerberize Checks | `supports.preKerberizeCheck`；存在 `UpgradeChecks.status=FAIL` 时展示 cluster check popup 并阻止进入 | `admin.kerberos_security.checks` | `app/controllers/main/admin/kerberos.js#checkAndStartKerberosWizard` |
| KRB-ENTRY-005 | 启动向导并登记所有者/恢复状态 | 保存 `onClosePath`，cluster state 为 `KERBEROS_DEPLOY`，`wizardControllerName=kerberosWizardController` | cluster status/persist | `app/routes/add_kerberos_routes.js`、`app/controllers/main/admin/kerberos/wizard_controller.js` |

## 四种启用模式

| ID | 模式 | Step 2 可见/特殊配置 | 后续流程差异 |
| --- | --- | --- | --- |
| KRB-MODE-001 | Existing MIT KDC，后端值 `mit-kdc` | KDC hosts、realm、admin principal/password、executable search paths 等 MIT 配置；AD 密码策略字段隐藏 | 创建 KERBEROS service/client；安装和测试 client；Ambari 管理 principals/keytabs |
| KRB-MODE-002 | Existing Active Directory，后端值 `active-directory` | KDC hosts、realm、LDAP URL、container DN、AD 密码规则等；AD 专属字段可见 | 同自动模式；KDC connection/session 与 credential store 可用 |
| KRB-MODE-003 | Existing IPA，后端值 `ipa` | IPA 专属配置；保存时强制 `install_packages=false`、`manage_krb5_conf=false` | 仍由 Ambari 管理 identities，但不让 Ambari 安装 package/管理 krb5.conf |
| KRB-MODE-004 | Manage principals and keytabs manually，后端值 `none` | 只保留 realm、KDC type、executable search paths 等例外；隐藏 KDC credential 字段 | 强制 `manage_identities=false`、`install_packages=false`、`manage_krb5_conf=false`；不创建/安装 KERBEROS_CLIENT，Step 2 直接跳 Step 4；用户下载 CSV 后人工生成并分发 principals/keytabs |

Step 1 为每种模式展示独立前置条件清单。切换模式会清空该模式所有勾选；只有当前模式的所有可见条件都确认，Next 才可用。MIT 在安装 ONEFS 时还会出现额外条件。

## Enable Kerberos 八步流程

### Step 1 Get Started

| ID | 功能与行为 | 校验/异常 | 后端请求 |
| --- | --- | --- | --- |
| KRB-1-001 | 在 MIT、AD、IPA、Manual 四种模式间单选 | 默认 MIT；改变选项清空 precondition checkboxes | 无 |
| KRB-1-002 | 逐项确认所选模式的部署前置条件 | 任一可见条件未勾选时不能继续 | 无 |

### Step 2 Configure Kerberos

| ID | 功能与行为 | 校验/分支 | 后端请求 |
| --- | --- | --- | --- |
| KRB-2-001 | 从 stack 加载 KERBEROS config types，按 KDC 模式过滤字段并填 `kdc_type` | AD/IPA/MIT/Manual 可见性不同；必填、realm/host/password 等沿用 config 校验 | stack config APIs |
| KRB-2-002 | 测试 KDC connection | 测试进行中关闭向导先二次确认；失败显示 KDC 错误并可重输 | `admin.kerberos_security.test_connection` |
| KRB-2-003 | 自动模式创建 KERBEROS service、KERBEROS_CLIENT service component 和所有 host-components | 先删除残留 KERBEROS service，避免旧向导资源冲突；刷新 component state 后，缺少 service component 时还会显式创建 `KERBEROS_CLIENT` | `common.delete.service`、`wizard.step8.create_selected_services`、`common.create_component`、`wizard.step8.register_host_to_component` |
| KRB-2-004 | 保存 `kerberos-env` 等 desired configs | 多 config type 一次提交，并写配置版本 note | `common.across.services.configurations` |
| KRB-2-005 | 创建 live KDC admin session/credentials | 自动模式先 GET alias，再 POST/PUT temporary 或 persisted credential；create/update 无论成功失败都会 resolve，因此 Step 2 的 `.done()` 仍前进。Manual 以 cluster `session_attributes.kerberos_admin` 传当前会话且后续不管理 identities | `credentials.get/create/update` 或 `common.cluster.update` |
| KRB-2-006 | Manual/IPA 强制安全选项 | Manual 关闭 identity/package/krb5.conf 管理；IPA 关闭 package/krb5.conf 管理 | 与配置保存同请求 |
| KRB-2-007 | Step 2 的精确失败传播并不一致 | 残留 KERBEROS DELETE 失败被 `.always()` 忽略；KERBEROS service POST、host-component registration、config save 失败会阻断。`common.create_component` 自身以 `.always()` 强制 resolve，因此创建失败仍继续 registration；自动 credential POST/PUT 也总是 resolve 并继续。Manual 的 `common.cluster.update` 失败则阻断。所有阻断分支都不复位 `nextBtnClickInProgress`，没有本页 Retry，Next 持续锁住 | `common.delete.service`、resource create、config save、credential CRUD、`common.cluster.update`；`KNOWN_BUG` |

### Step 3 Install And Test Kerberos Client

| ID | 功能与行为 | 失败/恢复 | 后端请求 |
| --- | --- | --- | --- |
| KRB-3-001 | 安装 KERBEROS_CLIENT 到所有 hosts；若 service component 仍为 INIT 先把 KERBEROS service 置 INSTALLED | 展示 progress tasks、host/task output | `common.service_component.info`、`common.services.update`、host-component install 请求 |
| KRB-3-002 | 执行 KERBEROS service check | KDC session 失效会触发 credential popup；取消则任务 FAILED | `service.item.smoke` |
| KRB-3-003 | 完成后检查 HEARTBEAT_LOST hosts | 任一 lost heartbeat 会把第一任务标 FAILED，并显示 affected hosts 详情 | `hosts.heartbeat_lost` |
| KRB-3-004 | 失败任务支持 Retry，也可勾选整步 `Ignore errors and continue` 后继续 | 有 lost heartbeat 时 Retry 从 install 第一任务开始；本 controller 从未设置 `canSkip`，没有 task-level Skip。`supports.autoRollbackHA=true` 时失败 task 会显示 Rollback，但 Kerberos controller 没有 `rollback()` handler，点击是条件出现的坏按钮 | Retry 重发失败的 install/test 请求；Ignore 和坏 Rollback 按钮不发 mutation；`KNOWN_BUG` |
| KRB-3-005 | Manual 模式完全跳过本步 | Step 2 Next 直接进入 Step 4，Back 也从 Step 4 返回 Step 2 | 无 |

### Step 4 Configure Identities

| ID | 功能与行为 | 校验/分支 | 后端请求 |
| --- | --- | --- | --- |
| KRB-4-001 | 读取 Kerberos descriptor，生成 Global、Ambari Principals 和已安装 service 的 identity 配置 | Enable Step 4 无条件读取 cluster `COMPOSITE?evaluate_when=true`，不会先读 STACK。Add Service 才先读 STACK，再读 COMPOSITE 并以 COMPOSITE 覆盖同名值、保留两侧独有属性；Add Service 还另行探测 cluster artifact 是否存在，以决定后续 create 或 update | Enable：`admin.kerberize.cluster_descriptor`；Add Service：`admin.kerberize.cluster_descriptor.stack`、`admin.kerberize.cluster_descriptor`、`admin.kerberize.cluster_descriptor_artifact` |
| KRB-4-002 | Stack Advisor 推荐 identity/config 值 | `supports.kerberosStackAdvisor` 且尚无 stored values 时请求；required recommendations 不能静默丢弃 | `config.recommendations` |
| KRB-4-003 | 编辑 principal/keytab/name/rule 等 descriptor 属性 | Manual 隐藏 KDC credentials 类属性；按 installed services 过滤身份 | descriptor 只在 Next 提交 |
| KRB-4-004 | 创建或更新 cluster `kerberos_descriptor` artifact | POST 409 改 PUT；提交前缓存表单值供失败返回 | `admin.kerberos.cluster.artifact.create`、`.update` |
| KRB-4-005 | 提交 descriptor 后先调用 unkerberize 清理半完成 security state，再进入 Confirm | 成功或失败都推进，目的是让后续正式 kerberize 从一致状态开始 | `admin.unkerberize.cluster` |
| KRB-4-006 | Enable Step load 等待 COMPOSITE descriptor GET | `getDescriptor()` 自建 Deferred 只 resolve、不 reject；GET 失败时 `loadStep()` 的 failure callback 不可达，页面永久 pending且没有 Retry | `admin.kerberize.cluster_descriptor`；`KNOWN_BUG` |

### Step 5 Confirm Configuration

| ID | 功能与行为 | 校验/分支 | 后端请求 |
| --- | --- | --- | --- |
| KRB-5-001 | 按所选模式展示最终 KDC properties | MIT/IPA/Manual/AD 展示字段集合不同；空值不展示 | prior configs |
| KRB-5-002 | 下载 `kerberos.csv` | error callback 错误复用 success handler；失败时对 jqXHR 调用 `split('\n')` 会抛异常，download progress flag 不复位 | `admin.kerberos.cluster.csv`；`KNOWN_BUG` |
| KRB-5-003 | Manual 模式提示必须依据 CSV 人工创建 principals、keytabs 并分发到目标路径 | 这是继续 Step 6 前的人工责任边界，UI 不验证文件是否实际存在 | CSV 请求 |
| KRB-5-004 | Exit Wizard | 仍显示退出 warning，并执行 discard：设置 security NONE、删除 KERBEROS service 后清状态 | `admin.unkerberize.cluster`、`common.delete.service` |

### Step 6 Stop Services

| ID | 功能与行为 | 动态任务/失败 | 后端请求 |
| --- | --- | --- | --- |
| KRB-6-001 | 停止所有 services | 关键阶段关闭显示 critical warning；低阶步骤禁用 | `common.services.update` |
| KRB-6-002 | 在 YARN 已安装、ATS 不支持 Kerberos 且 APP_TIMELINE_SERVER 存在时删除该 component | NoSuchResource 视为已完成；其他错误既不 complete 也不 `onTaskError`，task 永久卡住。这是兼容性清理，不属于 Metrics 功能 | `common.delete.host_component`；非 NoSuchResource 失败为 `KNOWN_BUG` |

### Step 7 Kerberize Cluster

| ID | 功能与行为 | 失败/恢复 | 后端请求 |
| --- | --- | --- | --- |
| KRB-7-001 | 将 `Clusters.security_type` 改为 `KERBEROS` 并启动服务端 KERBERIZE_CLUSTER request | 单 request progress，轮询 request/tasks；进行中不能回到低阶步骤 | `admin.kerberize.cluster`、request polling |
| KRB-7-002 | 失败后允许回 Step 4 修 descriptor，或 Retry | Retry 直接清旧 stage/tasks 并改发 `force_toggle_kerberos=true`；不先 unkerberize | `admin.kerberize.cluster.force` |
| KRB-7-003 | controller 中保留 `unkerberizeCluster()` 清理方法 | 没有 template action、route handler 或生产调用点；只有单元测试直接调用。成功/失败 callback 都转 Step 7，但它不是用户可达 Retry 路径 | `admin.unkerberize.cluster`；`STATIC_ONLY` |

### Step 8 Start And Test Services

| ID | 功能与行为 | 失败/完成 | 后端请求 |
| --- | --- | --- | --- |
| KRB-8-001 | 启动所有 services，并按 Ambari property 决定是否同时跑 smoke tests | `skip.service.checks=true` 时 `run_smoke_test=false` | `common.services.update`、request polling |
| KRB-8-002 | request 失败也允许 Complete | Submit 在 `COMPLETED` 或 `FAILED` 可用；用户完成后需在常规页面人工修复失败服务 | prior request |
| KRB-8-003 | Complete 清 Kerberos wizard local DB/status 并返回 Kerberos 管理页 | 关闭 Step 8 不做 discard，只显示普通 warning，保留已经启用的 Kerberos | cluster status/persist |

## Disable Kerberos

| ID | 顺序 | 行为与条件 | 后端请求 |
| --- | --- | --- | --- |
| KRB-DIS-001 | 前置 | Disable 按钮仅已启用、授权且无未保存 identity 编辑时可用；先展示 service warnings 和确认 | `admin.security_status` |
| KRB-DIS-002 | 1 Start ZooKeeper | 只启动 ZooKeeper，确保 unkerberize 所需基础服务可用 | common service update |
| KRB-DIS-003 | 2 Stop Required Services | 停止除 ZooKeeper 之外的 services | common service update |
| KRB-DIS-004 | 3 Unkerberize Cluster | 将 security type 切回 NONE并由后端撤销 identities/config changes | `admin.unkerberize.cluster` |
| KRB-DIS-005 | 3 failure skip | unkerberize error 可选择不管理 Kerberos identities 的 skip 分支 | `admin.unkerberize.cluster.skip` |
| KRB-DIS-006 | 4 Remove Kerberos | 删除 KERBEROS service；删除失败也按 task completed 继续，避免永久卡死 | `common.delete.service` |
| KRB-DIS-007 | 5 Start Services | 以 `runSmokeTest=true` 调用启动全部 services；通用 progress controller 再由 Ambari property `skip.service.checks` 决定实际 `params/run_smoke_test`，完成后可关闭并刷新 cluster | common service update |
| KRB-DIS-008 | 退出与恢复边界 | `unroutePath=false`；正在 unkerberize 时关闭被硬阻止，其他未完成阶段二次确认；关闭后清 task/local namespace、写 cluster state=`DEFAULT` 并 reload。Disable controller 自身的 `clusterDeployState` 从开始就是 `DEFAULT`，且不在 `controller_route.js`，刷新/崩溃后没有 Enable 式自动重路由恢复；恢复完整性为 `NEEDS_RUNTIME_VALIDATION` | cluster status/persist |
| KRB-DIS-009 | 关闭 Disable modal 的清理链误调用 `addServiceController.finish()` | 先清 Disable progress 的内存 tasks/current request IDs、Disable DB namespace 和 security deploy commands；随后重置 Add Service 内存中的 install options/hosts/cluster shell，清其持久化 wizard 字段和 DB namespace，并调用 `updateAll()`。它不会直接重置 Add Service 内存 `currentStep`；`clearServiceConfigProperties()` 也因调用 `get` 而只清 DB 值、不清内存配置。最后仍把 `clusterState=DEFAULT` 和当前 local DB 持久化后 reload。这是跨向导旧副作用，不是 Disable 业务需要 | cluster status/persist；`updateAll()` 触发常规全局刷新；`KNOWN_BUG` |

## 已启用后的管理能力

| ID | 功能与行为 | 权限/分支 | 后端请求 |
| --- | --- | --- | --- |
| KRB-MGMT-001 | 查看按 Global、Ambari Principals、各 service 分类的 composite identities/configs | 页面级权限同 Kerberos；只加载已安装 services 的身份 | descriptor/config APIs |
| KRB-MGMT-002 | Edit、Cancel、Save identities | `CLUSTER.TOGGLE_KERBEROS` + flag；realm 始终不可编辑；Cancel 恢复 saved/default value；有未保存改动时 Disable/Regenerate 禁用 | `admin.kerberos.cluster.artifact.update`，404 时尝试 `.create` |
| KRB-MGMT-003 | 保存 identity 变更后 regenerate keytabs | 正常 PUT 成功时，Manual regenerate all 且不自动重启；自动模式先询问是否自动重启 affected components。404 fallback POST 没有串回原 PUT promise，不能触发同一成功链 | artifact + `admin.kerberos_security.regenerate_keytabs` |
| KRB-MGMT-004 | 集群级 Regenerate Keytabs | 自动 Kerberos 才显示；选择 `all` 或 `missing`，再选择自动重启或之后人工重启 | `admin.kerberos_security.regenerate_keytabs` |
| KRB-MGMT-005 | 服务级 Regenerate Keytabs | 从 service action 对指定 service 所有 components 生成，config update policy 为 none | `admin.kerberos_security.regenerate_keytabs.service` |
| KRB-MGMT-006 | 主机级 Regenerate Keytabs | `supports.regenerateKeytabsOnSingleHost` 且 Kerberos 已启用；针对单 host，config update policy 为 none | `admin.kerberos_security.regenerate_keytabs.host` |
| KRB-MGMT-007 | Regenerate 成功后关联后台操作 | success 先以 `show_bg` setting 决定是否弹 Background Operations，并在该 GET settle 后的 `.done()` 中才设置 restart flag；底层 preference GET 用 `.always()` resolve，失败时通常不弹 popup但仍设置 flag。restart observer 只由**后续**全局 `runningOperationsCount` 变化触发，而不是设置 flag 时立即检查；若 GET settle 前计数已归零且之后不再变化，就不会 restart。无关 operation 会把 restart all 延迟到全局计数以后再次归零，甚至成为触发此前遗漏 restart 的变化 | regenerate request + user setting/background operation APIs；`KNOWN_BUG` |
| KRB-MGMT-008 | 下载当前 identities CSV | `CLUSTER.UPGRADE_DOWNGRADE_STACK` 权限决定按钮；不限 Manual 模式 | `admin.kerberos.cluster.csv` |
| KRB-MGMT-009 | artifact update 的 404 create fallback 存在实现缺陷 | error callback 使用作用域中未定义的 `self`；即使运行环境未在此处抛错，独立 POST 也未 return/resolve 原 PUT。Manual 的原 PUT `.done()` 不会 regenerate，自动模式同样不等待 fallback；必须标 `KNOWN_BUG` 并在 React 中定义明确的 create-then-regenerate 原子链 | `admin.kerberos.cluster.artifact.update`、`.create` |

## KDC Credential Store

| ID | 功能与行为 | 校验/分支 | 后端请求 |
| --- | --- | --- | --- |
| KRB-CRED-001 | 检测 persistent credential store 能力 | Manage 按钮只在非 Manual 且 `App.isCredentialStorePersistent` 时显示；该值来自 cluster model 的 `Clusters.credential_store_properties/storage.persistent`。`credentials.store.info` 有注册和 utility wrapper，但经典生产调用点未使用它来决定按钮 | cluster load；`credentials.store.info` 为 `STATIC_ONLY` utility |
| KRB-CRED-002 | 查询 `kdc.admin.credential` 是否存在 | API 列表不向 UI回显 secret；只决定 stored/removable 状态 | `credentials.list`、`credentials.get` |
| KRB-CRED-003 | 保存 KDC admin principal/password | principal/password 必填，principal 不允许空白；存在则 PUT，不存在则 POST；管理表单固定资源 type 为 persisted | `credentials.get` 后 `credentials.update` 或 `credentials.create` |
| KRB-CRED-004 | 删除已持久化 KDC credential | 二次确认；请求 settle 后刷新 removable 状态 | `credentials.delete` |
| KRB-CRED-005 | 其他向导需要 KDC 时验证 session | KDC validation OK 才执行原 callback；失败弹 invalid KDC popup，允许输入新 credential 并选择持久保存 | `kerberos.session.state`、credential CRUD |
| KRB-CRED-006 | Credential CRUD 的失败传播被吞掉 | `createOrUpdateCredentials()` 对 POST/PUT 的 `.always()` 最终都 resolve，并只把成功布尔值作为参数；管理表单再次用 `.always()` 丢弃该布尔值，保存失败仍显示 success。DELETE 也在 `.always()` 中显示 success；invalid-KDC popup 因 resolve 而在保存失败后仍重放原 AJAX | credential CRUD；`KNOWN_BUG` |

## 与安装和日常运维的联动

| ID | 场景 | 必须保留的条件行为 |
| --- | --- | --- |
| KRB-X-001 | Add Service | 入口先加载 security status/KDC type；Customize Configs 校验并合并新增服务的 Kerberos descriptor；Manual 模式在 Review 前更新 descriptor 并生成 CSV |
| KRB-X-002 | Add Host | Review 提交前检查 KDC session；自动模式需要有效 admin credential，Manual 模式直接继续 |
| KRB-X-003 | Add/Delete Host Component | Kerberos 集群中添加组件可能需要 KDC session 和 keytabs；删除/恢复 host 后可从 host action regenerate |
| KRB-X-004 | Reassign Master/HA/Federation | 新 master/component 的 identity、principal、keytab 必须随 component/config 变更同步，不能只复制非安全模式任务列表 |
| KRB-X-005 | Service/Host restart | Regenerate 可选择后端自动重启 affected components，或只生成 keytabs并把 restart 责任留给用户 |

## 恢复、退出与静态边界

| ID | 行为 | 细节 |
| --- | --- | --- |
| KRB-REC-001 | Enable 每次切 step 保存完整 local DB snapshot 到 cluster status | `clusterState=KERBEROS_DEPLOY`，刷新或另一个窗口按 `localdb.KerberosWizard.currentStep` 恢复 |
| KRB-REC-002 | Enable 保存 task statuses、task request IDs 和旧 request IDs | Step 3/6/7/8 恢复后继续轮询而不是重复发请求 |
| KRB-REC-003 | Enable 所有 step 的 `unroutePath()` 返回 false | 只能经过 modal close handler；Step 2 connection test 有额外确认，Step 6/7 为 critical warning |
| KRB-REC-004 | Enable 未完成时 Exit 执行 discard | unkerberize + 删除 KERBEROS service；Step 8 关闭例外，不撤销已经完成的安全切换 |
| KRB-REC-005 | Manual CSV 完成度无法由静态 UI证明 | React 对比必须用真实 MIT/AD/IPA/Manual 环境验证后端是否在缺 principal/keytab 时给出等价错误；文档将此标为 `NEEDS_RUNTIME_VALIDATION` |
| KRB-REC-006 | Disable 不在全局 wizard controller-route 恢复表 | modal controller 会在同一页面实例中从 local DB 加载 task/request ID，但 `clusterState=DEFAULT`，刷新时不会因服务端 status 自动回到 Disable；需在每个 mutation 前后做刷新/Server 重启验收；`NEEDS_RUNTIME_VALIDATION` |

## 已知缺陷与验证门槛

| ID | 静态结论 | React 基线要求 |
| --- | --- | --- |
| KRB-RISK-001 | Step 3 在 `supports.autoRollbackHA=true` 且 task 失败时会显示 Rollback，但 controller 没有 `rollback()` handler；Skip 仍没有可达 state | 不得把坏按钮登记为旧版可执行能力；React 应隐藏 Rollback，或实现有状态逆操作并标记为有意修复 |
| KRB-RISK-002 | Step 7 的孤立 unkerberize 方法与真实 Retry 分离 | React Retry 对比以直接 force kerberize 为旧行为；若先补偿清理，需记录新的安全语义 |
| KRB-RISK-003 | Credential save/update/delete 失败会被 UI 报成成功，且保存失败可重放原 KDC 请求 | React 必须正确 reject、保留输入并停止原请求重放；这是修复旧 bug，不要求复制错误 |
| KRB-RISK-004 | descriptor 404 fallback 可能因未定义 `self` 直接抛错，也不会接续 regenerate | React 必须将 PUT 404 -> POST -> regenerate/restart 串成可观察 promise，并覆盖 Manual/自动两分支测试 |
| KRB-RISK-005 | `credentials_test.js` 中 create/update 失败语义和 store type helper 测试被 `describe.skip` | 旧测试不能作为失败路径已验证证据；必须补 404、401/403、500、网络中断和重复提交测试 |

启发式模块索引见 [generated/api-by-module/security-ha-federation.md](generated/api-by-module/security-ha-federation.md)：它按 request name 和 caller path 宽匹配，可能混入跨模块请求，也可能漏掉 Kerberos 被共享 controller/mixin 间接调用的请求，不能视为接口全集。Credential Store 当前被归类在 background/common，但仍属于本基线；权威网络核对必须联合 [generated/ajax-endpoints.md](generated/ajax-endpoints.md)、[generated/ajax-calls.md](generated/ajax-calls.md)、[generated/direct-http-calls.md](generated/direct-http-calls.md)、[generated/browser-network-entrypoints.md](generated/browser-network-entrypoints.md) 与 [generated/realtime-channels.md](generated/realtime-channels.md)。
