# React 功能差异矩阵规范

本文定义后续 `ambari-web/latest` 对照 `ambari-web/classic` 的验收格式。当前阶段只建立规则和模板，不提前判断 React 覆盖状态。旧版事实保存在模块文档，React 状态是可反复更新的派生数据。

## 对照单位

矩阵一行对应一个稳定功能 ID，而不是一个 Ember route、React component 或 REST URL。同一个页面中的查看、修改、失败重试和恢复可能是不同功能；同一个接口也可能服务多个功能。

对每个功能必须核对以下维度：

| 维度 | `COVERED` 的最低要求 |
| --- | --- |
| 入口 | route、菜单、按钮、弹窗或自动触发点存在，深链和返回路径等价 |
| 可见条件 | permission、feature flag、stack/service/component/status/maintenance/upgrade/wizard 条件等价 |
| 正常行为 | 用户输入、校验、确认、请求顺序、成功结果和模型刷新等价 |
| API | method、URL、predicate、fields、payload、header、operation level、context 和 response 分支等价 |
| 异步行为 | request ID、polling/STOMP、task detail、日志、终止状态和进度展示等价 |
| 异常行为 | 禁用、错误信息、retry、skip、rollback、cancel、partial failure 和重复提交保护等价 |
| 恢复 | 刷新、崩溃、另一窗口、向导所有权和服务端状态恢复等价 |
| 测试 | 至少有自动化测试；`STATIC_ONLY/CONDITIONAL` 还需记录真实集群场景证据 |

任何一个必需维度不等价，都不能标 `COVERED`。

## 状态枚举

| 状态 | 定义 |
| --- | --- |
| `COVERED` | UI、API、条件、异常、恢复和测试均与旧版基线等价 |
| `PARTIAL` | 主路径存在，但一个或多个必需维度尚未实现或缺少证据 |
| `MISSING` | React 没有可达入口/行为，或只有无功能的页面壳 |
| `BEHAVIOR_DIFF` | React 有意或无意改变旧版行为；必须记录差异、影响和维护者决策 |
| `NOT_APPLICABLE` | 经维护者明确决定不迁移；必须给出理由/issue，不得用于隐藏缺失 |
| `NEEDS_RUNTIME_VALIDATION` | 静态代码看似覆盖，但需真实 stack、KDC、HA、外部 DB/LDAP/Log Search 等环境确认 |
| `BLOCKED` | 被明确的后端、依赖或基础设施问题阻塞；必须关联 issue，不能代替 `MISSING` |

`PLACEHOLDER` 和 `OUT_OF_SCOPE` 是旧版证据等级/范围，不是 React 实现状态。旧版为 `PLACEHOLDER` 的条目通常不进入必做矩阵；Metrics `OUT_OF_SCOPE` 永远不作为缺口。

## 矩阵字段

| 字段 | 必填 | 内容 |
| --- | --- | --- |
| `feature_id` | 是 | 模块文档中的稳定 ID |
| `legacy_doc` | 是 | 旧版模块文档和锚点/标题 |
| `legacy_evidence` | 是 | `CONFIRMED/STATIC_ONLY/CONDITIONAL/PLACEHOLDER/OUT_OF_SCOPE` |
| `react_status` | 是 | 上述状态枚举 |
| `react_route` | 条件 | 可达 route/入口；自动行为写触发位置 |
| `react_ui` | 条件 | component/hook/store 文件 |
| `react_api` | 条件 | API client/query/mutation 文件和请求名/endpoint |
| `condition_evidence` | 是 | 权限、flags、状态等价性的代码/测试证据 |
| `happy_path_test` | 是 | unit/component/e2e/真实集群测试引用 |
| `failure_recovery_test` | 是 | error/retry/cancel/refresh/ownership 等测试引用；不适用需说明 |
| `differences` | 条件 | 缺失维度或有意行为变化的精确描述 |
| `runtime_scenario` | 条件 | stack、service、KDC、HA topology、权限角色等复现条件 |
| `issue` | 条件 | 修复/决策 issue 或 PR |
| `reviewed_commit` | 是 | React 对照时使用的完整 Git commit |
| `reviewed_by` | 是 | 审核人及日期 |

推荐用 CSV/JSON 保存可编辑矩阵，Markdown 只作为自动渲染结果。旧版功能索引应由文档生成，禁止人工复制 ID 清单。

## 行模板

```csv
feature_id,legacy_doc,legacy_evidence,react_status,react_route,react_ui,react_api,condition_evidence,happy_path_test,failure_recovery_test,differences,runtime_scenario,issue,reviewed_commit,reviewed_by
INST-MODE-008,07-cluster-installation.md,CONFIRMED,MISSING,,,,,,,,,,,
KRB-MODE-003,08-kerberos.md,CONDITIONAL,NEEDS_RUNTIME_VALIDATION,/main/admin/kerberos,...,...,...,...,...,,Existing IPA with Kerberos enabled,...,...,...
```

## 对照工作流

1. 在固定 Ember baseline commit 上重跑提取器和基线检查，冻结功能 ID 与接口目录。
2. 在固定 React commit 上扫描 routes、UI actions、API clients、permissions 和 tests，先建立静态候选关联。
3. 逐功能核对全部维度；不能仅因 route 或 endpoint 存在就标覆盖。
4. 对安装、Kerberos、HA/Federation、upgrade、external DB/LDAP/Log Search 等场景执行真实集群测试。
5. 由第二位 reviewer 复查所有 `COVERED` 和 `NOT_APPLICABLE`；复杂长流程至少再做一次故障注入/刷新恢复。
6. 每次 React PR 只更新受影响行的状态和证据，不改旧版事实；发现旧版基线错误时单独提交基线修正及源码证据。

## 复杂流程场景矩阵

下列场景不能由单一 happy path 代表：

| 场景组 | 至少覆盖的变量 |
| --- | --- |
| Cluster Installation | Public/Local repository；VDF file/URL；SSH/manual Agent；warning acceptance；bootstrap/registration failure；install/start/check retry；刷新恢复 |
| Add Host/Add Service | Kerberos off/automatic/manual；有/无 master/slave/config step；config group；descriptor/CSV；partial deployment failure |
| Kerberos | MIT/AD/IPA/Manual；KDC connection failure；lost heartbeat；descriptor create/update；Step 7 force retry；Disable skip；credential persistent/temporary；keytab all/missing/restart |
| NameNode/JournalNode HA | secure/non-secure；依赖 service动态任务；checkpoint等待；JN add/remove/delete-only；auto/manual rollback；刷新/另窗口 |
| RM/Ranger HA | service/topology前置失败；host冲突；Stack Advisor changes；条件 HAWQ/HDFS配置；progress failure/retry |
| Federation/HAWQ | NameNode/Router federation；format/bootstrap failure；Kerberos；旧 HAWQ stack 的 add/remove/activate standby |
| Upgrade | Rolling/Express/Host Ordered；precheck required/warning/bypass；pause/resume/retry/skip/abort/downgrade/finalize；non-owner |
| Permissions | 至少 read-only、service operator、cluster admin、Ambari admin、View-only；upgrade/wizard互斥下再次验证 |

## 评审门禁

一个模块可以宣布迁移完成，必须满足：

1. 该模块所有纳入范围的 feature IDs 都有矩阵行且没有空白状态。
2. `MISSING/PARTIAL/BLOCKED/BEHAVIOR_DIFF` 均有 issue 或明确维护决策。
3. 所有 `COVERED` 都有正常路径和异常/恢复证据；无异常路径的简单只读项需明确写 `not applicable` 原因。
4. 所有 `CONDITIONAL/STATIC_ONLY/NEEDS_RUNTIME_VALIDATION` 都有运行场景和测试结果。
5. API 反向扫描没有未关联 mutation，route/action 反向扫描没有未关联可达用户行为。
6. Metrics 排除项没有被错误计入完成率。
