# Ember Client Config 浏览器下载契约

> 由 `tools/extract-ember-baseline.mjs` 生成。五种 resource scope 共用一个 mixin，并由 `window.open()` 直接请求 archive；不会进入 `App.ajax` 或直接 HTTP 调用计数。

共 5 种 resource scope。

| Resource type | Method | URL（含 `/api/v1` 后的路径） | 分支位置 |
| --- | --- | --- | --- |
| `CLUSTER` | GET (browser download) | `/clusters/{clusterName}/components?format=client_config_tar` | [source](../../../../ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js#L69) |
| `HOST` | GET (browser download) | `/clusters/{clusterName}/hosts/{hostName}/host_components?format=client_config_tar` | [source](../../../../ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js#L63) |
| `SERVICE` | GET (browser download) | `/clusters/{clusterName}/services/{serviceName}/components?format=client_config_tar` | [source](../../../../ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js#L66) |
| `SERVICE_COMPONENT` | GET (browser download) | `/clusters/{clusterName}/services/{serviceName}/components/{componentName}?format=client_config_tar` | [source](../../../../ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js#L57) |
| `HOST_COMPONENT` | GET (browser download) | `/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}?format=client_config_tar` | [source](../../../../ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js#L60) |
