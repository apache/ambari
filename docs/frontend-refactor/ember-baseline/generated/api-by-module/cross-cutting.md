# 跨模块与待人工归类：Ember 非 Metrics 命名 AJAX 候选索引

> 由 `tools/extract-ember-baseline.mjs` 生成。该页只按请求名和调用者路径的宽正则启发式归类：共享请求可能跨模块混入或重复，模块请求也可能漏列或归到其他页。它不是模块接口全集；权威核对必须联合 `../ajax-endpoints.json`、`../ajax-calls.json`、`../direct-http-calls.json`、`../browser-network-entrypoints.json` 和 `../realtime-channels.json`。

共 36 个命名请求候选。
候选内容 SHA-256：`68d4c1da2d1104c6b92a6fcd224fb3fe41964aebcd94f3931000296aa6cbab2b`。

| 请求名 | Method | URL（不含默认 prefix） | format 输入键 | 调用位置 |
| --- | --- | --- | --- | --- |
| `common.delete.user` | `DELETE` | `/users/{user}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `configs.quicklinksconfig.services` | `GET` | `{stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&quicklinks/QuickLinkInfo/default=true&fields=quicklinks/*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `configs.stack_configs.load.service` | `GET` | `{stackVersionUrl}/services/{serviceName}?fields=configurations/*,StackServices/config_types/*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `configs.config_versions.load` | `GET` | `/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&service_config_version={configVersion}&fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `configs.config_versions.load.group` | `GET` | `/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&group_id={id}&fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `configs.config_versions.load.current_versions` | `GET` | `/clusters/{clusterName}/configurations/service_config_versions?service_name.in({serviceNames})&is_current=true&fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `reassign.save_configs` | `PUT` | `/clusters/{clusterName}` | `properties`, `service_config_version_note`, `siteName` | 未发现经典前端字符串调用证据 |
| `config.cluster` | `GET` | `{stackVersionUrl}/configurations?fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `config.advanced` | `GET` | `{stackVersionUrl}/services/{serviceName}/configurations?fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `config.advanced.multiple.services` | `GET` | `{stackVersionUrl}/services?StackServices/service_name.in({serviceNames})&fields=configurations/*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `config.advanced.partial` | `GET` | `{stackVersionUrl}/services/?StackServices/service_name.in({serviceList})&fields=configurations/*{queryFilter}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `config.config_types` | `GET` | `{stackVersionUrl}/services/{serviceName}?fields=StackServices/config_types` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `config.tags_and_groups` | `GET` | `/clusters/{clusterName}?fields=Clusters/desired_configs,config_groups/*{urlParams}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `config.tags.selected` | `GET` | `/clusters/{clusterName}/configurations?type.in({tags})` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `cluster.logging.searchEngine` | `GET` | `/clusters/{clusterName}/logging/searchEngine?{query}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `admin.high_availability.polling` | `GET` | `/clusters/{clusterName}/requests/{requestId}?fields=tasks/*,Requests/*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `get.cluster.artifact` | `GET` | `/clusters/{clusterName}/artifacts/{artifactName}?fields=artifact_data` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `admin.kerberize.stack_descriptor` | `GET` | `/clusters/{clusterName}/kerberos_descriptors/STACK` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `admin.user.create` | `POST` | `/users/{user}` | `data` | 未发现经典前端字符串调用证据 |
| `admin.user.edit` | `PUT` | `/users/{user}` | `data` | 未发现经典前端字符串调用证据 |
| `admin.stack_versions.all` | `GET` | `/clusters/{clusterName}/stack_versions?fields=ClusterStackVersions/*,repository_versions/RepositoryVersions/*&minimal_response=true` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `persist.get.text` | `GET` | `/persist/{key}` | 无静态 `data.*` 引用 | `ambari-web/classic/app/mixins/common/persist.js:68` |
| `persist.get` | `GET` | `/persist/{key}` | 无 `format()` | `ambari-web/classic/app/mixins/common/persist.js:49` |
| `persist.post` | `POST` | `/persist` | `keyValuePair` | `ambari-web/classic/app/mixins/common/persist.js:129` |
| `wizard.stacks_versions` | `GET` | `/stacks/{stackName}/versions?fields=Versions,operating_systems/repositories/Repositories` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `users.all` | `GET` | `/users/?fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `users.privileges` | `GET` | `/privileges?fields=*` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `components.get_category` | `GET` | `/clusters/{clusterName}/components?fields=ServiceComponentInfo/component_name,ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/recovery_enabled,ServiceComponentInfo/total_count&minimal_response=true` | 无 `format()` | `ambari-web/classic/app/controllers/main/admin/service_auto_start.js:134` |
| `components.update` | `PUT` | `/clusters/{clusterName}/components?{urlParams}` | `ServiceComponentInfo`, `query` | `ambari-web/classic/app/controllers/main/admin/service_auto_start.js:164` |
| `components.get_installed` | `GET` | `/clusters/{clusterName}/components` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `service.serviceConfigVersions.get.total` | `GET` | `/clusters/{clusterName}/configurations/service_config_versions?page_size=1&minimal_response=true` | 无 `format()` | `ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js:141` |
| `service.serviceConfigVersion.get` | `GET` | `/clusters/{clusterName}/configurations/service_config_versions?service_name={serviceName}&service_config_version={serviceConfigVersion}` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `service.serviceConfigVersions.get.suggestions` | `GET` | `/clusters/{clusterName}/configurations/service_config_versions?fields={key}&minimal_response=true` | 无 `format()` | `ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js:210` |
| `service.mysql.testHiveConnection` | `POST` | `/requests` | `db_connection_url`, `db_name`, `db_pass`, `db_user`, `hosts`, `java_home`, `jdk_location`, `jdk_name` | 未发现经典前端字符串调用证据 |
| `service.components.load` | `GET` | `/clusters/{clusterName}/services?fields=components&minimal_response=true` | 无 `format()` | 未发现经典前端字符串调用证据 |
| `hiveServerInteractive.getStatus` | `GET` | `http://:/leader [dynamic: 'http://' + data.hsiHost + ':' + data.port + '/leader'] [DYNAMIC_URL]` | `hsiHost`, `port` | `ambari-web/classic/app/mappers/service_metrics_mapper.js:353` |
