# Ambari Views 模块

经典 Ember 的 Ambari Views 功能由三个不同层次组成：Ember 负责列出当前用户可见的 View instance、解析两套 hash URL，并把 instance 的服务端 context path 嵌入同源 iframe；具体 View 应用由 Ambari Server 在 `/views/...` 下提供；用户、组、权限和 View instance 的管理则由独立 AngularJS Admin Console 提供。三者不能在 React 对照时合并成一个页面功能。

本文中的 `View` 专指 Ambari View extension，不是 Ember `Em.View` UI 类。默认均为 `CONFIRMED`；明确标为 `STATIC_ONLY`、`CONDITIONAL`、`PLACEHOLDER` 或 `OUT_OF_SCOPE` 的条目按 [00-methodology.md](00-methodology.md) 的证据等级处理。

## 范围与对象模型

| ID | 基线事实 | React 对照边界 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- |
| VIEW-SCOPE-001 | 后端对象层级是 View definition -> View version -> View instance；Ember 最终只建立扁平的 `App.ViewInstance[]` | 同一 View 的不同 version/instance 必须保留为不同对象，不能只按 `view_name` 去重 | `app/controllers/main/views_controller.js`、`app/models/view_instance.js` | `CONFIRMED` |
| VIEW-SCOPE-002 | Ember 只消费 instance 的 icon、label、visible、version、description、view name、short URL、instance name 和 context path | View parameters、properties、cluster binding、instance lifecycle 和授权 CRUD 不属于这个 Ember 页面 | `app/controllers/main/views_controller.js#loadViewInstancesSuccess` | `CONFIRMED` |
| VIEW-SCOPE-003 | `/views/{view}/{version}/{instance}/...` 是 Ambari Server 提供的 View Web context，不是 `/api/v1` REST route，也不是 Ember hash route | React shell可以承载该 context，但不能把 View 自身应用误写成 React 重构范围 | `app/views/main/views/details.js#src`、`ViewInstanceInfo.context_path` | `CONFIRMED` |
| VIEW-SCOPE-004 | 内置 `ADMIN_VIEW` 是独立 AngularJS Admin Console；Ember 只有入口判断、版本发现和整页跳转 | Admin Console 内的用户、组、角色、集群权限、repository、View instance/short URL/权限管理均为 `OUT_OF_SCOPE` | `app/router.js#transitionToAdminView`、`ambari-admin/src/main/resources/ui/admin-web` | `OUT_OF_SCOPE` |

## Route 与页面状态

| ID | URL / route state | 行为 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- |
| VIEW-ROUTE-001 | `#/main/views/`，`main.views.index` | 等待 View instance 数据完成后，把 `mainViews` 列表接到 main outlet | `app/routes/views.js#index`、`app/views/main/views_view.js` | `CONFIRMED` |
| VIEW-ROUTE-002 | `#/main/views/:viewName/:version/:instanceName`，`main.views.viewDetails` | 用完整 identity 匹配预加载的 instance，把 `mainViewsDetails` iframe 接到 outlet | `app/routes/views.js#viewDetails` | `CONFIRMED` |
| VIEW-ROUTE-003 | `#/main/view/`，`main.view.index` | singular route 的 index 同样显示完整 View 列表；进入父 route 时已经切换到 contrib-view 宽屏布局 | `app/routes/view.js#index` | `CONFIRMED` |
| VIEW-ROUTE-004 | `#/main/view/:viewName/:shortName`，`main.view.shortViewDetails` | 以 `viewName + shortUrl` 匹配预加载 instance，再显示相同 iframe | `app/routes/view.js#shortViewDetails`、`test/models/view_instance_test.js` | `CONFIRMED` |
| VIEW-ROUTE-005 | `#/adminView`，顶层 `adminView` state；route pattern 为 `/adminView` | 这是 hash router 中无 outlet 的过渡 route；通过 server version 找到 Admin View URL 后执行整页 `location.replace`。不得把 route pattern 误写成浏览器根路径 `/adminView` | `app/router.js#adminView`、`app/router.js#adminViewInfoSuccessCallback` | `CONFIRMED` |
| VIEW-ROUTE-006 | `/views/:view/:version/:instance/...`，无 `#` | 浏览器直接请求 View application 的 server context；不经过 `main.views.viewDetails` | `app/views/main/views/details.js#src`、`app/router.js#adminViewInfoSuccessCallback` | `CONFIRMED` |

`main.views` 父 route 没有 breadcrumb；regular detail 的 breadcrumb label 绑定 instance label。`main.view` 父 route 定义了 label breadcrumb，但 short detail 显式设为 `null`。这是两套 URL 在经典 UI 中可见的差异，不应假定它们只是字符串别名。

## Instance 发现与列表

| ID | 功能与行为 | 成功结果 | 异常/边界 | 后端请求 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- | --- | --- |
| VIEW-LIST-001 | 已登录时先查询是否存在任何 View definition | 有 `items` 才继续加载所有非 system version 的 instances；无 item 直接完成为空数组 | 未登录时不发请求；route 的认证由 main 父 route 处理 | `views.info` | `app/controllers/main/views_controller.js#loadAmbariViews`、controller tests | `CONFIRMED` |
| VIEW-LIST-002 | 加载所有 version 下的 instance 并扁平化 | 每个可见 instance 变成一个 `App.ViewInstance`，保持服务端遍历顺序 | 没有客户端分页、排序、搜索或手工 refresh | `views.instances` | `app/controllers/main/views_controller.js#loadViewInstancesSuccess` | `CONFIRMED` |
| VIEW-LIST-003 | 只纳入服务端返回的非 system、已部署 version 中，且 `ViewInstanceInfo.visible` 为 truthy 的 instance | 隐藏 instance、内置 system View 和未部署 version 的 instance 不出现在目录、顶部 View 菜单 | `system=false` 是显式 query；服务端 View 资源 provider 只为 `DEPLOYED` version 构造可返回 instances，前端没有显式 status predicate；`visible` 在响应后过滤，模板再做防御性判断 | `views.instances` | AJAX definition、controller、server View resource provider、`main/views.hbs` | `CONFIRMED` |
| VIEW-LIST-004 | 计算 instance 展示字段和 fallback | icon 缺失用 `/img/ambari-view-default.png`；label 优先 `instance.label`、再 `version.label`、再 `view_name`；description 缺失显示 `No description` | `href` 直接取 `context_path + '/'`，不在客户端重建 server context | `views.instances` | `main/views_controller.js:83-94`、messages | `CONFIRMED` |
| VIEW-LIST-005 | 显示 `Your Views` 表格 | 每行显示 icon、label、version、description；点击整行用 `window.open(internalAmbariUrl)` 打开新的浏览上下文 | 浏览器可按设置选择 tab/window；action 无 context 时不做任何事 | 无新请求 | `app/templates/main/views.hbs`、`#setView`、controller tests | `CONFIRMED` |
| VIEW-LIST-006 | 无可见 instance 时显示 `No views` | 空响应和加载错误最终使用同一个空状态 | 经典 UI 不区分“确实为空”“无 instance 权限”和“请求失败” | `views.info`、`views.instances` | controller error callbacks、template | `CONFIRMED` |
| VIEW-LIST-007 | `dataLoading()` 每 50ms 等待 `isDataLoaded=true` 后才接 outlet | 首次请求成功、空结果或 error callback 都会解除等待 | 没有 Views 专属 spinner；若请求根本没有进入任何 callback，promise 会无限等待 | 无新请求 | `main/views_controller.js#dataLoading`、routes | `CONFIRMED` |
| VIEW-LIST-008 | main、installer、显式 Views route 和登录分流都可触发 `loadAmbariViews()` | 后续成功会替换整个 instance 数组 | 不去重并发请求，也不在刷新前把 `isDataLoaded` 重置为 false；首次完成后的 route 可以先消费旧数组，再被异步结果更新 | `views.info`、`views.instances` | `app/routes/main.js`、`app/routes/installer.js`、两条 Views routes | `STATIC_ONLY` |

### 可到达入口

| ID | 入口 | 经典行为 | 前置/边界 | 等级 |
| --- | --- | --- | --- | --- |
| VIEW-NAV-001 | View-only、无 cluster 权限或安装路由无权限时的自动分流 | 进入 `main.views.index`，这是 View 列表最主要的显式入口 | 详见“登录与 View-only 用户” | `CONFIRMED` |
| VIEW-NAV-002 | 已安装集群的顶部九宫格 Views 下拉 | 从 `ApplicationView.views` 列所有 visible instances；点击调用同一 `setView` 并打开新浏览上下文；空时显示 disabled `No Views` | 只在 `applicationController.enableLinks=true` 时出现，即 cluster 已安装且加载完成，并且不是 View-only 用户 | `CONFIRMED` |
| VIEW-NAV-003 | 直接访问 `#/main/views` 或 `#/main/view` | 认证后显示列表 | 当前 `MainSideMenuView.content` 没有创建 Views 菜单项；虽然保留 `isViewsItem/goToSection('views')` 代码和单测，不能据此认定当前侧栏有 Views 入口 | `STATIC_ONLY` |

## Regular、Short URL 与 View 内部路径

### URL 选择与 instance 匹配

| ID | 功能与行为 | 精确规则 | 异常/边界 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- | --- |
| VIEW-URL-001 | 为列表项生成经典 UI 内部 URL | 有 `shortUrl` 时生成 `#/main/view/{viewName}/{shortUrl}`；否则生成 `#/main/views/{viewName}/{version}/{instanceName}` | 不做 URL encoding；名称合法性依赖 Admin Console/服务端 | `app/models/view_instance.js#internalAmbariUrl`、model tests | `CONFIRMED` |
| VIEW-URL-002 | regular URL 解析 | 构造 `/views/{viewName}/{version}/{instanceName}/`，选择 `instance.href.endsWith(constructedPath)` 的第一个对象 | `endsWith` 允许 `context_path` 带 proxy/root 前缀；route 不按参数请求单个 instance | `app/routes/views.js#connectOutlets` | `CONFIRMED` |
| VIEW-URL-003 | short URL 解析 | 先按 `viewName` 过滤，再取 `shortUrl == shortName` 的第一个对象；version 和 instance name 不出现在 URL | short name 的唯一性与授权由服务端管理；客户端对重复结果没有冲突 UI | `app/routes/view.js#connectOutlets` | `CONFIRMED` |
| VIEW-URL-004 | instance 匹配后保存 `viewPath` | 在 connect outlet 前把解析结果写入所选 `App.ViewInstance`，iframe src 使用它 | 对象在全局数组中复用；下一次无内部路径的导航会把它重置为空字符串 | 两条 route、details view | `CONFIRMED` |
| VIEW-URL-005 | route 参数没有匹配任何已加载 instance | 仍调用 `connectOutlet('mainViewsDetails', undefined)`；旧 Ember 只有 context truthy 才更新 singleton controller.content，因此同一会话从有效 instance再导航到无匹配 URL 会确定复用旧 instance及其可能残留的 `viewPath`。冷启动 controller content为空时，`src` computed对 `content.href/viewPath` 求值会生成异常或畸形 URL | 没有 not-found、无权限、返回列表或 retry 状态；warm navigation存在 stale-instance泄漏，冷启动表现仍需浏览器验证 | 两条 route、`vendor/scripts/ember-latest.js#connectOutlet`、details controller/view | `STATIC_ONLY` |
| VIEW-URL-006 | 已认证状态刷新 regular/short deep link | 内存模型从空开始，main/Views route 重新发现 instances，details route 等待 `isDataLoaded` 后重新匹配并创建 iframe | instance 必须仍被 API 返回且 visible；任一目录请求失败即落入 VIEW-URL-005，没有持久化 snapshot 或单 instance fallback 请求 | main route、两条 Views routes、controller | `CONFIRMED` |

### `viewPath` 转换算法

`viewPath` 用于从 Ambari hash route 深链到 View application 内部页面，例如 Tez application history。regular 和 short route 各自复制了一份完全相同的解析逻辑：

1. 从浏览器当前 URL 的第一个 `?` 开始取 query；没有 `?` 时解析为空。
2. query 包含 `viewPath` 时，从最后一个 `?viewPath=` 之后取全部内容并执行 `decodeURIComponent`；代码只正确识别 `?viewPath=`，因此它必须是第一个 query 参数，`&viewPath=` 会产生错误切片。
3. 把解码结果中的第一个 `&` 替换为 `?`，从而把其余参数变成 View 内部 query；后续 `&` 保留。
4. 旧 Ember router 可能把 query 附在最后一个动态 route 参数上，因此再从 `instanceName` 或 `shortName` 的最后一个 `?` 处截断，用截断值匹配 instance。
5. 如果最后一个动态参数实际上没有携带 query，代码把已解析的 `viewPath` 清空；这是旧 router query 解析方式的耦合点。
6. 转发前移除一个开头 `/`，因为 `instance.href` 已以 `/` 结尾。

| ID | 输入示例 | `parseViewPath` 结果 | 最终追加形式 | 证据/等级 |
| --- | --- | --- | --- | --- |
| VIEW-PATH-001 | 无 query | 空字符串 | `{context_path}/` | route code；`CONFIRMED` |
| VIEW-PATH-002 | `?foo=bar&count=1` | `?foo=bar&count=1` | 是否转发取决于 query 是否进入最后一个 route 参数 | route test 只验证 parser；`STATIC_ONLY` |
| VIEW-PATH-003 | `?viewPath=%2Fuser%2Fadmin%2Faddress` | `/user/admin/address` | `{context_path}/user/admin/address` | `test/routes/views_test.js`；`CONFIRMED` |
| VIEW-PATH-004 | `?viewPath=%2Fuser%2Fadmin%2Faddress&foo=bar&count=1` | `/user/admin/address?foo=bar&count=1` | `{context_path}/user/admin/address?foo=bar&count=1` | `test/routes/views_test.js`；`CONFIRMED` |
| VIEW-PATH-005 | `?viewPath=%2F%23%2Ftez-app%2Fapplication_...` | `/#/tez-app/application_...` | `{context_path}/#/tez-app/application_...` | route code、Tez history URL template；`CONDITIONAL` |
| VIEW-PATH-006 | 非法 percent encoding，例如 `?viewPath=%E0%A4%A` | `decodeURIComponent` 同步抛 `URIError` | 没有 try/catch、route error state或 fallback，details outlet不会按正常链连接 | 两条 route code；`STATIC_ONLY` |
| VIEW-PATH-007 | 普通 query 的其他参数名/值中仅包含子串 `viewPath` | `path.contains('viewPath')` 会误入特殊解析，再以 `lastIndexOf('?viewPath=')=-1` 计算错误 slice | parser没有按 query key解析，可能丢失或篡改原 query；现有 tests未覆盖 | 两条 route code；`STATIC_ONLY` |

short route 没有独立 parser test，且现有 route test 只调用 regular route 的 `parseViewPath()`；React 必须分别验证 regular/short URL、普通 query、encoded slash、hash 和多个 query 参数。

## iframe 承载与渲染生命周期

| ID | 功能与行为 | 精确行为 | 异常/边界 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- | --- |
| VIEW-IFRAME-001 | details outlet 本身渲染为 iframe | `tagName=iframe`，绑定 `src`、`seamless`、`allowfullscreen`，CSS 为 100% 宽、最小 100% 高、无边框 | 没有单独的 details template | `app/views/main/views/details.js`、`app/styles/application.less` | `CONFIRMED` |
| VIEW-IFRAME-002 | 生成 iframe src | 强制使用当前 `protocol + '//' + host`，再追加服务端返回的 `context_path + '/'` 和解析后的 `viewPath` | 不接受 instance 返回的外部 origin；View context 按同源访问设计 | details view `src` | `CONFIRMED` |
| VIEW-IFRAME-003 | details 使用宽屏 contrib-view 布局 | regular detail 进入/退出时添加/移除 body class；singular `/main/view` 父 route 对其 index 和 short detail统一处理 | navbar 仍保持固定 container 宽度，主内容扩展为 auto | routes、`bootstrap_overrides.less` | `CONFIRMED` |
| VIEW-IFRAME-004 | 插入 iframe 时立即 resize，以后每 5 秒 resize | 高度取 View body `scrollHeight` 与 viewport 去除 header/footer 后高度的较大值；resize 前后保持宿主 window scrollTop | selector 查找 document 中所有 iframe但只读取第一个；多 iframe 页面可能选错对象 | details view `didInsertElement/resizeFunction` | `STATIC_ONLY` |
| VIEW-IFRAME-005 | 销毁 details view 时清理 resize interval | 已保存 interval 才清除，避免离页后继续改 DOM | 无独立测试 | details view `willDestroyElement` | `STATIC_ONLY` |
| VIEW-IFRAME-006 | iframe 内活动计入 Ambari inactivity timeout | details 插入后重新启动/绑定 inactivity monitor；宿主对 iframe `contentWindow` 的 mousemove、keypress、click 绑定 `keepActive` | 依赖同源；跨源 redirect 或浏览器限制下访问 `contentWindow.document`/事件可能失败，代码无 catch | details view、`app/controllers/main.js#bindActivityEventMonitors` | `STATIC_ONLY` |
| VIEW-IFRAME-007 | 经典 iframe 没有 sandbox 限制 | 只声明 seamless 和 fullscreen；View 与 Ambari shell共享 origin/session | React 若新增 sandbox/CSP，必须验证 View 登录、导航、下载、弹窗和剪贴板等兼容性 | details view attributes | `CONFIRMED` |
| VIEW-IFRAME-008 | iframe navigation 没有 Ember loading/error/retry UI | 等待的只是 instance 目录请求；iframe 发出浏览器 GET 后没有 `load/error` handler、spinner、timeout 或错误占位 | server 404/500、View deploy failure、内容脚本异常由 iframe/浏览器自行呈现 | details view、routes | `CONFIRMED` |
| VIEW-IFRAME-009 | View 内部导航不回写宿主 Ember URL | shell 没有监听 iframe location、history 或 `postMessage`；宿主只把初始 `viewPath` 写入 `src` | 浏览器刷新只恢复宿主 URL 中原有的 `viewPath`，不能保证恢复用户随后在 iframe 内到达的页面；View 自身持久化另论 | details view、两条 routes | `STATIC_ONLY` |

## 登录与 View-only 用户

### 判定语义

`App.auth` 来自 `GET /users/{user}/authorizations?fields=*` 的唯一 `AuthorizationInfo.authorization_id` 集合。经典代码将以下两种情况定义为 `isOnlyViewUser=true`：

- authorization 集合存在但为空数组；
- 集合长度恰好为 1，唯一值为 `VIEW.USE`，并且当时 `App.isAuthorized('VIEW.USE')` 仍为 true。

第二个条件通过 `isAuthorized`，因此也会继承 upgrade 全局权限限制和 `wizardWatcherController.isNonWizardUser` 限制。这是旧版静态语义，不应简化成“存在任意 View privilege”。

| ID | 场景 | 登录/进入 main 后的精确结果 | 主要请求 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- | --- |
| VIEW-ONLY-001 | 已有 cluster，`isOnlyViewUser=true` | 登录后主动加载 Views 并转 `main.views.index`；进入 main 时仍加载 supports、keep-alive、Ambari properties、cluster identity 和 Views，但不执行 repo detail check、`mainController.initialize()`、STOMP 或完整 cluster 运维模型加载，只把 cluster controller 标为 loaded 以显示 outlet | `persist.get`、`ambari.service`、`cluster.load_cluster_name`（条件）、`views.info`、`views.instances`、keep-alive `router.login.clusters` | `app/router.js#loginGetClustersSuccessCallback`、`app/routes/main.js` | `CONFIRMED` |
| VIEW-ONLY-002 | 已有 cluster，普通 cluster/Ambari 用户 | 走正常 preferred path / Dashboard 初始化；main 同时后台加载 View instances，供顶部 Views 下拉使用 | auth、cluster、Views 请求 | router、main route | `CONFIRMED` |
| VIEW-ONLY-003 | 没有 cluster，`isOnlyViewUser=true` 或 authorization 为空 | 直接进入 `main.views.index` | auth、cluster、Views 请求 | router；相关 login route suite 为 skipped test | `STATIC_ONLY` |
| VIEW-ONLY-004 | 没有 cluster，非 View-only 用户 | 不进 Installer，而是探测 Ambari Server version 并跳独立 Admin View；探测失败回 Views 列表 | `ambari.service.load_server_version` | router、router tests | `CONFIRMED` |
| VIEW-ONLY-005 | cluster provisioning 未完成，当前不在 View route | cluster state 属于 installer states 且有 `AMBARI.ADD_DELETE_CLUSTERS` 时恢复 Installer；否则转 Views | cluster status/persist | `app/routes/main.js`、router redirections mixin | `CONFIRMED` |
| VIEW-ONLY-006 | 直接进入 `/installer` 但无 `AMBARI.ADD_DELETE_CLUSTERS` | 加载 supports、版本和 Views 后转 `main.views.index` | server version、Views 请求 | `app/routes/installer.js` | `CONFIRMED` |
| VIEW-ONLY-007 | 已认证用户直接打开 regular/short deep link | main route 发现 current state 已是 `viewDetails/shortViewDetails` 时不覆盖为 index，保留目标 View | Views 请求 | `app/routes/main.js:53-56` | `CONFIRMED` |
| VIEW-ONLY-008 | 未认证用户直接打开 deep link后完成登录 | 普通 cluster 用户可由 `transitionToApp()` 恢复安全校验后的 relative preferred path；View-only 分支直接调用 `transitionToViews()`，静态代码没有显式恢复原 details path | auth、cluster、Views 请求 | `app/router.js#transitionToApp/#transitionToViews` | `STATIC_ONLY` |

### 进入 main 的初始化请求链

View-only 只跳过集群运维数据初始化，不是“只请求 Views”。进入 `/main` 父 route 后，普通用户和 View-only 用户先走同一条外壳链；下表顺序和等待关系是 React 对照基准：

| ID | 顺序/条件 | 请求与精确行为 | 失败/并发边界 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- | --- |
| VIEW-INIT-001 | 1. `main.enter` 先确认认证 | `getAuthenticated()` 获取或复用登录阶段保存的 `router.login.clusters` jqXHR；新页面会请求 cluster provisioning/security/version/id，刚登录时通常复用已经完成或进行中的同名请求 | 认证检查失败保存当前 hash 为 preferred path 并回 login；“复用请求”意味着不能按 route 次数推断网络次数 | `app/router.js#getClusterDataRequest/#getAuthenticated`、`app/routes/main.js:35` | `CONFIRMED` |
| VIEW-INIT-002 | 2. 认证成功后先加载 supports | `persist.get` 读取 key `user-pref-{loginName}-supports`，有响应时覆盖 `App.supports` 中的同名值 | route 使用 `.complete()`，所以 404、无数据或请求失败都继续；失败保留编译时默认 supports | `app/controllers/experimental.js#loadSupports`、`app/mixins/common/persist.js#getUserPref`、`app/routes/main.js:38` | `CONFIRMED` |
| VIEW-INIT-003 | 3. supports 请求完成后启动 keep-alive | `startKeepAlivePoller()` 只在 `isPollerRunning=false` 时注册定时器；首次不是立即请求，而是在 60,000ms 后调用 `router.login.clusters`，以后由 AJAX complete callback安排下一次 | View-only 用户也保持该 poller；它只是维持/验证会话，不把响应映射为完整 cluster model。只有 logoff请求成功才显式置 `isPollerRunning=false`；logoff error callback为空 | `app/controllers/application.js#startKeepAlivePoller/#getStack`、`app/utils/updater.js`、`app/router.js#logOffSuccessCallback`、`app/config.js#sessionKeepAliveInterval` | `CONFIRMED` |
| VIEW-INIT-004 | 4. 同步等待 Ambari Server properties | `ambari.service` 无 `fields` 参数时请求 AMBARI_SERVER root component；成功保存 server properties/clock/version、判断 custom JDK/MySQL OS family并启动 inactivity monitor | main route用 `.then(success)` 串接后续步骤，而请求的 error callback 是空函数；失败会无专用提示地阻断这次 route 中的 cluster name/loaded 分支，尽管登录/Views route可能已另行触发过 View discovery | `app/controllers/global/cluster_controller.js#loadAmbariPropertiesSuccess`、`app/routes/main.js:40` | `CONFIRMED` |
| VIEW-INIT-005 | 5. properties 成功后并行发现 Views并确认 cluster identity | 先异步调用 `loadAmbariViews()`，不等待其完成；随后 `loadClusterName(false)`。若全局已有 `clusterName + clusterId`，后者只同步本地状态；否则发 `cluster.load_cluster_name` 并等待 | Views route自身也会调用 discovery，故可能与 main 请求重叠；cluster name失败使用全局 reload/error流程，main分支不会继续 | main route、cluster controller、Views controller | `CONFIRMED` |
| VIEW-INIT-006 | 6a. cluster 已安装且为 View-only | 保留已打开的 regular/short detail，否则转 `main.views.index`，再直接设 `clusterController.isLoaded=true` | 明确不调用 `checkDetailedRepoVersion()`、`mainController.initialize()`、`App.StompClient.connect()`、`loadClusterData()`；因此不加载 hosts/services/alerts/upgrades/user settings等运维模型 | `app/routes/main.js:47-58`、`app/controllers/main.js#initialize` | `CONFIRMED` |
| VIEW-INIT-007 | 6b. cluster 未安装 | 下一 tick 用 `persist.get` 读取 `CLUSTER_CURRENT_STATUS`；请求成功后，当前不在 View route且状态属于 installer states时，有 `AMBARI.ADD_DELETE_CLUSTERS` 恢复 Installer，否则进入 Views；已在 regular/short View route则不抢占 | main使用 jqXHR `.then(success)`：404虽静默沿用默认状态，promise仍为 rejected，其他错误还显示 update error modal；两者都不执行该 success分支，可能留下只靠已连接 View route工作的 partial-init状态 | `app/models/cluster_states.js#updateFromServer`、`app/routes/main.js:60-78` | `CONFIRMED` |

### View-only 外壳差异

| ID | 行为 | 旧版结果 | 等级 |
| --- | --- | --- | --- |
| VIEW-ONLY-009 | 左侧运维导航 | 不创建 Dashboard、Services、Hosts、Alerts 项；通常也没有 Admin 项 | `CONFIRMED` |
| VIEW-ONLY-010 | 顶部 Views 下拉及 cluster notifications | `enableLinks=false`，所以全部隐藏；View-only 用户通过当前列表或 iframe工作，而不是再用九宫格切换 | `CONFIRMED` |
| VIEW-ONLY-011 | Ambari logo / Dashboard 跳转 | `goToDashboard` 因 `enableLinks=false` 不导航 | `CONFIRMED` |
| VIEW-ONLY-012 | 用户菜单 | About、Switch Experience、Sign out 仍存在；Manage Ambari 是否出现另按 Ambari 级权限判断 | `CONFIRMED` |

Views route 本身没有显式 `VIEW.USE` guard。Ember 依赖 main route 的认证和 server 对 `/api/v1/views` 返回集的授权过滤；客户端只再检查 `system=false` 和 `visible`。React 不能用 `visible` 代替服务端授权，也不能因为用户拥有 cluster 权限就假定其能使用每个 instance。

## Service 页面与 View 的交叉入口

| ID | 经典源码状态 | 行为/边界 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- |
| VIEW-X-001 | 普通 Service Quick Links 是另一套外部 Web UI 链接机制 | `App.QuickLinksView` 从 stack metadata/config/host 生成 URL；只有最终 URL 明确指向 `#/main/view/...` 时才复用本文 short route，不能把所有 Quick Link 当成 Ambari View | `app/views/common/quick_view_link_view.js`、service summary template | `CONFIRMED` |
| VIEW-X-002 | Hive summary 留有 View link 扩展点 | `viewsToShow` 按 instance name 白名单并可覆盖 label，模板把结果交给 `goToView()`；但当前类默认 `{}`，全仓库没有运行代码填充，所以当前基线不会显示任何该类链接 | `app/views/main/service/services/hive.js`、Hive template、summary controller | `PLACEHOLDER` |
| VIEW-X-003 | 通用 Service Summary 的 Views panel 已被注释 | computed `views` 和对应 Handlebars section 都不执行，不能作为 React 必须复刻的入口 | `app/views/main/service/info/summary.js:72-78`、summary template `131-145` | `PLACEHOLDER` |
| VIEW-X-004 | 配置或服务端生成的 View deep link 可以携带 `viewPath` | 例如 Tez history URL template 把目标 application path编码进 `viewPath`；最终仍由 regular/short route 和 iframe处理 | Views routes、Tez configuration/advisor | `CONDITIONAL` |

Hive 模板中的 `<a target="_blank">` 同时绑定 Ember action，而 controller 的实际 action 是 `App.router.route(internalAmbariUrl)`；若未来重新启用此扩展点，究竟新 tab 还是当前 tab取决于旧 Ember action 的事件处理，必须运行态验证，不能只依据 HTML `target`。

## Admin View 发现与跳转

| ID | 入口/行为 | 权限与结果 | 失败/边界 | 后端请求 | 主要证据 | 等级 |
| --- | --- | --- | --- | --- | --- | --- |
| VIEW-ADMIN-001 | 无 cluster 的登录后默认入口 | 非 View-only 用户调用 `transitionToAdminView()`；成功后整页进入 Admin Console | 请求 error callback 不显示默认 error modal，而是回 `main.views.index` | `ambari.service.load_server_version` | router、router tests | `CONFIRMED` |
| VIEW-ADMIN-002 | 用户菜单 `Manage Ambari` | `showManageAmbari` 成立且用户拥有模板列出的任一 Ambari 级管理权限时显示，点击进入 `#/adminView` state | route又硬性要求已登录且 `CLUSTER.UPGRADE_DOWNGRADE_STACK`，否则转 login；入口权限和 route guard不一致。该 route 的 server-version请求没有 error callback：500/401/407/413 走默认 modal，403/404等默认静默；任何失败都不会执行登录后 `transitionToAdminView()` 所用的 Views fallback，并停在无 outlet state | `ambari.service.load_server_version` | application template/controller、router、AJAX default error handler | `CONFIRMED` |
| VIEW-ADMIN-003 | Stack Versions 的 `Manage Versions` | `havePermissions('AMBARI.MANAGE_STACK_VERSIONS')` 为真才显示，且非当前 wizard owner时 disabled；确认“离开 Cluster Management”后探测版本并整页跳转 | `havePermissions` 还受全局 upgrade state、`supports.opsDuringRollingUpgrade` 和 `App.auth` 门控，不是只看 permission string；请求失败走全局 AJAX error，取消确认不请求 | `ambari.service.load_server_version` | versions template/view、`app/app.js#havePermissions`、version view tests | `CONFIRMED` |
| VIEW-ADMIN-004 | 选择 Admin View version | 对每个 component 映射 `RootServiceComponents.component_version`，默认字符串排序，取最后一项，再用 `/[^\d.-]/g` 删除 build suffix | 映射不滤掉 `undefined`；这是 lexicographic 而非 semantic version sort；空数组、缺 version 或异常版本格式可能在 `.replace()` 前后失败且没有专用恢复 | 同上 | router callback、tests | `CONFIRMED` |
| VIEW-ADMIN-005 | 构造 Admin Console URL | `App.appURLRoot + 'views/ADMIN_VIEW/' + latestVersion + '/INSTANCE/#/'`，然后 `window.location.replace()` | replace 不保留当前 Ember 页面为浏览器 back history entry；proxy root 由 `appURLRoot` 提供 | 浏览器导航 | router、helper、config | `CONFIRMED` |
| VIEW-ADMIN-006 | `ADMIN_VIEW` 不进入普通 View 目录 | instance 查询排除 `ViewVersionInfo/system=true`；fixture 中 Admin instance 也为 `visible=false` | 管理入口不能依赖普通 instance 列表是否加载成功 | `views.instances` | AJAX query、Views fixtures | `CONFIRMED` |

`Manage Ambari` 模板中的权限集合为 `AMBARI.ADD_DELETE_CLUSTERS`、`AMBARI.ASSIGN_ROLES`、`AMBARI.EDIT_STACK_REPOS`、`AMBARI.MANAGE_GROUPS`、`AMBARI.MANAGE_STACK_VERSIONS`、`AMBARI.MANAGE_USERS`、`AMBARI.MANAGE_VIEWS`、`AMBARI.RENAME_CLUSTER`。`AMBARI.MANAGE_USERS` 在模板字符串中重复一次，不改变 OR 语义。

### AngularJS Admin Console 边界

以下能力虽由同一 Ambari Server 发布，但当前 Ember 没有实现，React Ember 对照矩阵不得把它们记到本文功能 ID 下：

- View definition/version 的部署状态和清单管理。
- 创建、clone、编辑、删除 View instance。
- 设置 display label、description、visibility、properties、local/remote/custom cluster binding。
- 创建和维护 short URL。
- 给用户/组授予 View permission，以及管理 cluster permissions。
- 用户、组、角色、remote cluster 和其他 Admin Console 管理页面。

若 React 重构决定同时替代 AngularJS Admin Console，应建立独立基线，不得根据本文的跳转行为推断其 CRUD 细节。

普通 View application 也有自己的前端、resource endpoints 和业务流程。经典 shell 只把其 Web context 放入 iframe，无法从 `ambari-web/classic` 穷举每个已部署 View 的内部 API；需要迁移具体 View 时，应按对应 View artifact/source 另建基线。本文只要求 shell 的发现、路由、承载、授权边界和浏览器导航等价。

## 权限与可见性模型

| ID | 权限/条件 | 经典 Ember 中的实际作用 | 关键边界 | 等级 |
| --- | --- | --- | --- | --- |
| VIEW-PERM-001 | `VIEW.USE` authorization | 与 authorization 集合长度共同计算 `isOnlyViewUser` | 不是 route 内逐 instance guard；还受全局 `isAuthorized` 状态限制 | `CONFIRMED` |
| VIEW-PERM-002 | `VIEW.USER` permission | 是 View instance privilege 的后端 permission name，可在用户 privilege 数据中出现 | Ember Views controller 不直接检查 `VIEW.USER`；不要与 `VIEW.USE` authorization id 混用 | `STATIC_ONLY` |
| VIEW-PERM-003 | 服务端 instance access | 决定 `/api/v1/views` 对当前 session 返回哪些 resources，并保护 `/views/{context}` | 客户端的 `visible=true` 只是展示开关，不是授权 | `STATIC_ONLY` |
| VIEW-PERM-004 | `AMBARI.MANAGE_VIEWS` | 只是 `Manage Ambari` 入口 OR 权限之一 | View instance CRUD 位于 AngularJS；该权限本身不让 Ember route执行 CRUD | `CONFIRMED` |
| VIEW-PERM-005 | `CLUSTER.UPGRADE_DOWNGRADE_STACK` | `/adminView` transition route 的硬 guard | 与 `Manage Ambari` 链接使用的 Ambari 级权限集合不一致 | `CONFIRMED` |
| VIEW-PERM-006 | `AMBARI.MANAGE_STACK_VERSIONS` | 显示 Stack Versions 页的 `Manage Versions` Admin View 入口 | 仅控制该入口，不控制普通 View 使用 | `CONFIRMED` |
| VIEW-PERM-007 | `AMBARI.ADD_DELETE_CLUSTERS` | 决定未完成安装/Installer route 应恢复向导还是 fallback 到 Views | 不是 View 使用权限 | `CONFIRMED` |
| VIEW-PERM-008 | wizard owner | Stack Versions 的外部管理按钮对 non-wizard user disabled；安装 Step 9 明确允许退出到 Admin View/Views | 普通 View 列表和 iframe没有统一的 wizard 禁止逻辑 | `CONFIRMED` |

用户设置 controller 会把 `PrivilegeInfo.type='VIEW'` 的 instance name、view name、version 和 permission labels 分组用于 privilege 展示；该逻辑不参与 View 列表过滤或 route 授权。

## 后端接口契约

### Views 直接使用的命名请求

| 请求名 | Method | 完整 URL | 请求时机 | 关键响应字段 | 失败行为 |
| --- | --- | --- | --- | --- | --- |
| `views.info` | `GET` | `/api/v1/views` | `loadAmbariViews()` 的第一阶段 | `items[]`，Ember只判断长度 | 自定义 error callback 清空列表并置 `isDataLoaded=true`，不走默认 error modal |
| `views.instances` | `GET` | `/api/v1/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label&versions/ViewVersionInfo/system=false` | 仅 `views.info.items.length > 0` | `items[].versions[].ViewVersionInfo.label`、`versions[].instances[].ViewInstanceInfo` | 自定义 error callback 清空列表并置 loaded，不区分 401/403/404/500 |
| `ambari.service.load_server_version` | `GET` | `/api/v1/services/AMBARI?fields=components/RootServiceComponents/component_version&components/RootServiceComponents/component_name=AMBARI_SERVER&minimal_response=true` | 无 cluster fallback、`/adminView` route、Manage Versions | `components[].RootServiceComponents.component_version` | 登录 fallback 回 Views；显式 `/adminView`/Manage Versions 没有同样 fallback，使用默认处理或停留 |

`ViewInstanceInfo` 的前端契约如下：

| 响应字段 | Ember model 字段 | 使用方式 |
| --- | --- | --- |
| `icon_path` | `iconPath` | 列表 icon；空时默认图片 |
| `label` | `label` | 列表、顶部下拉、regular breadcrumb |
| `visible` | `visible` | 客户端最终过滤 |
| `version` | `version` | 列表和 regular hash URL |
| `description` | `description` | 列表描述；空时 fallback |
| `view_name` | `viewName` | 两套 hash URL 和匹配键 |
| `short_url` | `shortUrl` | 存在时优先生成 singular short URL |
| `instance_name` | `instanceName` | regular hash URL 和 conditional service hook |
| `context_path` | `href` | 追加 `/` 后作为 iframe server path；必须视为服务端事实 |

### 登录、main 外壳与权限分流依赖的请求

| 请求名 | Method | URL / key | 请求时机及与 Views 的关系 | 失败语义 |
| --- | --- | --- | --- | --- |
| `router.login` | `POST` | `/api/v1/auth` | 本地登录提交；UTF-8 `username:password` 的 Base64 放入 Basic Authorization header | 403显示认证错误，500显示 server error，其他状态走通用登录失败；外部 JWT分支见认证文档 |
| `router.afterLogin` | `GET` | `/api/v1/users/{loginName}?fields=*,privileges/PrivilegeInfo/cluster_name,privileges/PrivilegeInfo/permission_name` | 建立登录用户和 privilege上下文 | 失败走 login error，不进入 Views 分流 |
| `router.user.authorizations` | `GET` | `/api/v1/users/{userName}/authorizations?fields=*` | 建立 `App.auth`；`VIEW.USE` 与集合长度共同决定 View-only | login链使用 `.complete()` 后继续，因此失败时 `App.auth` 可能仍为空/旧值，需运行态验证 |
| `router.login.message` | `GET` | `/api/v1/settings/motd` | authorizations 完成后读取登录消息，确认或无有效消息后才继续 cluster分流 | error、空值或非法 JSON均按“无消息”继续 |
| `router.login.clusters` | `GET` | `/api/v1/clusters?fields=Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id` | 登录分流、`main.getAuthenticated()` 以及每 60 秒 keep-alive共用；决定 normal app、Installer、Views或 Admin View | 认证探测失败回 login；keep-alive没有业务 error UI，由 updater complete继续调度 |
| `persist.get`（supports） | `GET` | `/api/v1/persist/user-pref-{loginName}-supports` | 每次进入 main/installer 前合并 per-user feature flags | 空/404/error均保留 defaults并继续 |
| `ambari.service`（main） | `GET` | `/api/v1/services/AMBARI/components/AMBARI_SERVER` | View-only 也读取 server properties/clock/version并建立 inactivity timeout | 空 error callback；main链的后续 success串接被截断 |
| `cluster.load_cluster_name` | `GET` | `/api/v1/clusters?fields=Clusters/security_type,Clusters/version,Clusters/cluster_id` | 仅在 `App.clusterName` 或 `App.clusterId` 缺失，或调用方要求 reload时发出；设置 cluster name/id/stack/security | 使用 reload error handler；失败不进入 View-only loaded分支 |
| `persist.get`（cluster status） | `GET` | `/api/v1/persist/CLUSTER_CURRENT_STATUS` | 仅 cluster provisioning未完成时恢复 installer/wizard状态并决定是否 fallback Views | 历史值可替换 local DB；404沿用默认但 jqXHR仍拒绝，其他错误显示 modal并拒绝，main的 `.then(success)` 均不继续 |

### 绕过 `App.ajax` 的浏览器请求/导航

Views controller、route 和 details view 中没有 `App.HttpClient.get`、原生 `XMLHttpRequest` 或直接 `$.ajax` 调用，因此 [generated/direct-http-calls.md](generated/direct-http-calls.md) 没有 Views 专属调用点。但迁移时仍必须追踪三类不在命名请求表中的浏览器行为：

| 标识 | 行为 | 网络/历史语义 |
| --- | --- | --- |
| `NAV:ViewInstance.internalAmbariUrl` | `window.open('#/main/view...')` 或 `window.open('#/main/views...')` | 打开同一 Ambari shell 的新浏览上下文，再由 route加载 iframe |
| `BROWSER_GET:ViewInstanceInfo.context_path` | iframe `src={origin}{context_path}/{viewPath}` | 浏览器直接 GET View Web application及其静态资源，共享当前 Ambari session |
| `NAV:ADMIN_VIEW` | `window.location.replace('{appURLRoot}views/ADMIN_VIEW/{version}/INSTANCE/#/')` | 整页离开 Ember shell并替换当前 history entry |

[generated/api-by-module/views.md](generated/api-by-module/views.md) 只是按请求名和 caller path 宽匹配的候选索引；跨模块命中不等于 Views 页面直接调用，缺席也不能证明没有请求。权威网络核对必须联合 [AJAX 定义](generated/ajax-endpoints.md)、[AJAX 调用点](generated/ajax-calls.md)、[direct HTTP](generated/direct-http-calls.md)、[browser entrypoints](generated/browser-network-entrypoints.md) 和 [realtime channels](generated/realtime-channels.md)。

## 源码与测试反向核对

| 核对对象 | 已证实行为 | 测试状态/缺口 |
| --- | --- | --- |
| `test/controllers/main/views_controller_test.js` | 登录后才请求、两阶段 load、空/error 归零、instance field mapping、`setView` 调用 `window.open` | 只覆盖一个 visible instance；未覆盖 hidden/system、多 version、多 instance、fallback label/icon/description、并发 reload |
| `test/models/view_instance_test.js` | 有 short URL 走 singular route，无 short URL 走 regular route | 未覆盖需要 encoding 的 names、空字段和 proxy root |
| `test/routes/views_test.js` | regular route 的 `parseViewPath` 对无 query、普通 query、encoded path、path + query 的输出 | 未执行完整 `connectOutlets`；short route parser、参数切片、instance lookup、invalid route 未测 |
| `test/views/main/views_view_test.js` | `MainViewsView.views` 绑定 controller array | 未渲染 template action、empty state、visible filter |
| `test/router_test.js#adminViewInfoSuccessCallback` | 多 component version 取排序末项，去除 custom build suffix，生成 Admin View URL | version 选择测试存在；整个 `loginGetClustersSuccessCallback` suite 为 `describe.skip`，无 cluster/View-only矩阵并非 active regression coverage |
| `test/router_test.js#transitionToViews/#adminViewInfoErrorCallback` | 加载 Views并转 index；Admin version请求失败回 Views | 不覆盖 `/adminView` route guard 与链接权限不一致 |
| `test/views/main/admin/stack_upgrade/version_view_test.js` | Manage Versions 确认后请求 server version并 `location.replace` | 覆盖普通/custom version；不覆盖取消、请求失败、空 components、lexical version陷阱 |
| `test/views/main/menu_test.js` | dormant `goToSection('views')` branch可调用 router | 不证明 `MainSideMenuView.content` 当前实际创建 Views item |
| `test/controllers/application_test.js`、`test/controllers/global/cluster_controller_test.js` | `enableLinks` 排除 View-only、keep-alive发送 `router.login.clusters`、Ambari properties/cluster name请求及成功映射 | 没有 main route集成测试证明 VIEW-INIT-001 到 VIEW-INIT-007 的完整顺序、失败短路和定时器清理 |
| `generated/template-actions.md` 对 Views 文件的反向清点 | 当前可达 Views动作只有列表/顶部下拉的两个 `setView` 和用户菜单的 `goToAdminView`；Hive `goToView` 对应空扩展点 | 静态 action提取不覆盖 view click handler、动态 action或 iframe内部应用；未发现额外 Views mutation不等于运行态绝对不存在 |
| `app/views/main/views/details.js` | iframe src、resize、interval、inactivity | 没有专用 unit/integration test，全部需要浏览器运行态验证 |

## 已知旧版风险与 React 验收要求

| ID | 旧版风险/歧义 | React 对照处理 |
| --- | --- | --- |
| VIEW-RISK-001 | 两阶段 View 查询增加一次往返；reload 不重置 loaded且不去重并发 | 若 React 合并/缓存请求，仍需证明最终授权过滤、刷新和错误语义；行为改变标 `BEHAVIOR_DIFF` |
| VIEW-RISK-002 | 空列表、403/500 和 instance load failure 显示相同 `No views` | React 新增可诊断 error/retry 是合理改进，但不能在矩阵中误标成旧版已存在 |
| VIEW-RISK-003 | invalid regular/short deep link没有 not-found恢复，并可能复用 singleton details controller 的旧 content | React 若修复，记录新行为并分别测试 cold/warm navigation、unknown、hidden、unauthorized、deleted instance，禁止显示上一 instance |
| VIEW-RISK-004 | `viewPath` 依赖旧 Ember把 query附到最后一个动态参数；parser用 substring而非 query key识别且不捕获 `decodeURIComponent` 的 `URIError` | 必须用真实浏览器覆盖两套 URL、hash/query、query 参数顺序、无关字段包含 `viewPath`、非法 percent encoding、encoded slash、proxy/Knox路径；不能只移植 parser unit test |
| VIEW-RISK-005 | iframe读取 content document、绑定事件且无 sandbox，隐含同源假设 | React 改 iframe policy前验证所有保留 View；跨源支持如为新需求应独立设计 |
| VIEW-RISK-006 | iframe没有 load/error lifecycle，并用全局 selector取第一个 iframe resize | React 可改进，但需验证高度、宿主滚动、View内部路由、下载和 inactivity timeout |
| VIEW-RISK-007 | View route无客户端逐 instance permission guard | 服务端 REST/context授权必须保留；React 可增加 route guard，但不能信任客户端 metadata作为安全边界 |
| VIEW-RISK-008 | `isOnlyViewUser` 把空 authorization 当 View-only，并受 upgrade/wizard全局 gate影响 | 对照测试必须使用 authorization payload，不要只按 privilege label或用户名构造角色 |
| VIEW-RISK-009 | Manage Ambari 链接权限、`/adminView` route guard和 Manage Versions权限是三套不同条件 | 不得用一个统一 `canManageViews` boolean静默改变旧入口；若统一应标 `BEHAVIOR_DIFF` 并由维护者确认 |
| VIEW-RISK-010 | Admin version用字符串排序并假设至少一个合法 component version | 多 Ambari Server、custom build、`2.9`/`2.10`、空/坏响应都需测试；可改为服务端事实或 semantic sort |
| VIEW-RISK-011 | generic service-to-View panel和 Hive hook当前未启用 | React 不应根据注释或空扩展点认定功能缺失；只有确认 stack/runtime注入后才从 `PLACEHOLDER` 升级 |
| VIEW-RISK-012 | View-only 仍依赖 supports -> `ambari.service` -> cluster identity这条 main外壳链；`ambari.service` 或未完成 cluster 的 persist失败会阻断 success串接；keep-alive只在 logoff成功时显式关闭 | React 不得因“只需 Views”删掉 keep-alive/inactivity/session上下文；若改成可降级并显示错误，应标 `BEHAVIOR_DIFF` 并覆盖 partial-init、logout失败和恢复 |

### 最低运行态场景

1. 一个用户分别拥有零 authorization、仅 `VIEW.USE`、cluster + View、多个 View instance privileges，核对首屏、菜单和 API 返回集。
2. 同一 View 存在多个 version/instance，包含 visible/hidden、system/non-system、带和不带 short URL的组合。
3. regular 与 short deep link分别覆盖无内部路径、`viewPath` slash、hash route、query 参数、刷新和登录前访问。
4. View REST 401/403/500、instance删除、context 404/500、View deploy中和 iframe script error，记录旧版与 React error状态。
5. View 内容高度变化、宿主滚动、inactivity warning/logout、View内弹窗/下载/fullscreen以及离页 interval清理。
6. Ambari 安装在非根 proxy path/Knox路径下，核对 `endsWith` instance匹配、hash URL、iframe context和 Admin View `appURLRoot`。
7. 无 cluster的 View-only、Ambari admin、普通无权限用户分别登录，核对 Views/Admin View fallback。
8. 多个 Ambari Server component version、custom suffix和两位数 minor version，核对 Admin View版本选择与失败恢复。
9. View-only main初始化分别注入 supports、Ambari properties、cluster identity和persist失败，并用虚拟时钟确认 keep-alive首次延迟、重复进入不重复注册，以及 logoff成功/失败时不同的停止行为。
