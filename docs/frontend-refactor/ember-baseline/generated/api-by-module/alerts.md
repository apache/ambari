# 告警：Ember 非 Metrics 命名 AJAX 候选索引

> 由 `tools/extract-ember-baseline.mjs` 生成。该页只按请求名和调用者路径的宽正则启发式归类：共享请求可能跨模块混入或重复，模块请求也可能漏列或归到其他页。它不是模块接口全集；权威核对必须联合 `../ajax-endpoints.json`、`../ajax-calls.json`、`../direct-http-calls.json`、`../browser-network-entrypoints.json` 和 `../realtime-channels.json`。

共 19 个命名请求候选。
候选内容 SHA-256：`2d1ae67fdd95e61ad4a15635993bc42eea50d79902a5db086f4f51a910e7bba7`。

| 请求名 | Method | URL（不含默认 prefix） | format 输入键 | 调用位置 |
| --- | --- | --- | --- | --- |
| `alerts.load_alert_groups` | `GET` | `/clusters/{clusterName}/alert_groups?fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `alerts.load_an_alert_group` | `GET` | `/clusters/{clusterName}/alert_groups/{group_id}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `alert_groups.create` | `POST` | `/clusters/{clusterName}/alert_groups` | `definitions`, `name`, `targets` | `ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:476` |
| `alert_groups.update` | `PUT` | `/clusters/{clusterName}/alert_groups/{group_id}` | `definitions`, `name`, `targets` | `ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:508` |
| `alert_groups.delete` | `DELETE` | `/clusters/{clusterName}/alert_groups/{group_id}` | 无静态 `data.*` 引用 | `ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:542` |
| `alerts.load_all_alert_definitions` | `GET` | `/clusters/{clusterName}/alert_definitions?fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `alerts.notifications` | `GET` | `/alert_targets?fields=*` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/manage_alert_groups_controller.js:210`<br>`ambari-web/classic/app/controllers/main/alerts/manage_alert_notifications_controller.js:363` |
| `alerts.instances` | `GET` | `/clusters/{clusterName}/alerts?fields=*` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/alert_instances_controller.js:113` |
| `alerts.instances.unhealthy` | `GET` | `/clusters/{clusterName}/alerts?fields=*&Alert/state.in(CRITICAL,WARNING)&{paginationInfo}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `alerts.instances.by_definition` | `GET` | `/clusters/{clusterName}/alerts?fields=*&Alert/definition_id={definitionId}` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/alert_instances_controller.js:104` |
| `alerts.instances.by_host` | `GET` | `/clusters/{clusterName}/alerts?fields=*&Alert/host_name={hostName}` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/alert_instances_controller.js:95` |
| `alerts.update_alert_definition` | `PUT` | `/clusters/{clusterName}/alert_definitions/{id}` | `data` | `ambari-web/classic/app/controllers/main/alert_definitions_controller.js:110`<br>`ambari-web/classic/app/controllers/main/alerts/definition_configs_controller.js:654`<br>`ambari-web/classic/app/controllers/main/alerts/definition_details_controller.js:155`<br>`ambari-web/classic/app/controllers/main/alerts/definition_details_controller.js:246`<br>`ambari-web/classic/app/controllers/main/alerts/definition_details_controller.js:265`<br>`ambari-web/classic/app/controllers/main/alerts/definition_details_controller.js:309` |
| `alerts.create_alert_definition` | `POST` | `/clusters/{clusterName}/alert_definitions/` | `data` | `ambari-web/classic/app/controllers/main/alerts/add_alert_definition/add_alert_definition_controller.js:38` |
| `alerts.delete_alert_definition` | `DELETE` | `/clusters/{clusterName}/alert_definitions/{id}` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/definition_details_controller.js:189` |
| `alerts.create_alert_notification` | `POST` | `/alert_targets?{urlParams}` | `data` | `ambari-web/classic/app/controllers/main/alerts/manage_alert_notifications_controller.js:860`<br>`ambari-web/classic/app/controllers/wizard/step8_controller.js:1706` |
| `alerts.update_alert_notification` | `PUT` | `/alert_targets/{id}` | `data` | `ambari-web/classic/app/controllers/main/alerts/manage_alert_notifications_controller.js:898`<br>`ambari-web/classic/app/controllers/main/alerts/manage_alert_notifications_controller.js:977` |
| `alerts.delete_alert_notification` | `DELETE` | `/alert_targets/{id}` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/manage_alert_notifications_controller.js:939` |
| `alerts.get_instances_history` | `GET` | `/clusters/{clusterName}/alert_history?(AlertHistory/definition_name={definitionName})&(AlertHistory/timestamp>={timestamp})` | 无 `format()` | `ambari-web/classic/app/controllers/main/alerts/definition_details_controller.js:91` |
| `admin.save_configs` | `PUT` | `/clusters/{clusterName}` | `properties`, `siteName` | `ambari-web/classic/app/controllers/main/admin/highAvailability/nameNode/rollback_controller.js:364`<br>`ambari-web/classic/app/controllers/main/admin/highAvailability/nameNode/rollback_controller.js:377`<br>`ambari-web/classic/app/controllers/main/admin/highAvailability/nameNode/rollback_controller.js:391`<br>`ambari-web/classic/app/controllers/main/admin/highAvailability/nameNode/rollback_controller.js:428`<br>`ambari-web/classic/app/controllers/main/admin/highAvailability/nameNode/rollback_controller.js:438`<br>`ambari-web/classic/app/controllers/main/admin/service_auto_start.js:152`<br>`ambari-web/classic/app/controllers/main/alerts/alert_definitions_actions_controller.js:240` |
