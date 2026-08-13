# Ember 非 Metrics 实时通道契约

> 由 `tools/extract-ember-baseline.mjs` 从已审计的静态 contract 生成。本文冻结 classic UI 的 STOMP/WebSocket/SockJS 行为，不包含 Metrics 时序数据通道。

## 可校验摘要

- transports：2
- destinations：11（10 static + 1 dynamic）
- subscribe sites：11
- addHandler sites：1
- removeHandler sites：1
- business unsubscribe sites：1
- lifecycle contracts：4

## Transport

| ID / 类型 | URL | 协议与心跳 | Fallback / 重连 | 风险边界 | Source / Test |
| --- | --- | --- | --- | --- | --- |
| `RT-TRANSPORT-001`<br>`NATIVE_WEBSOCKET` | `{ws\|wss}://{window.location.hostname}{:window.location.port}/api/stomp/v1/websocket` | STOMP 1.1/1.0<br>heartbeat 10000/10000 ms<br>schemes ws/wss | fallback: Initial native WebSocket connection error before any successful connection.<br>reconnect: After any previously successful connection is lost, reconnect with the same transport and restore the captured in-memory subscriptions. | There is no REST polling substitute when both native WebSocket and SockJS fail.<br>CONNECT sends no explicit login, passcode, token, or CSRF header. The endpoint allows every origin pattern, while the enclosing /api/* path still passes through the Spring Security filter; the authenticated principal and browser credential behavior must be verified at the HTTP upgrade/fallback handshake.<br>isConnected is set after the first success and is never reset by the wrapper, so later failures keep scheduling reconnect attempts.<br>The reconnect snapshot is shallow, has no event replay, and can restore a destination removed during the six-second delay or discard one added during reconnection.<br>Authentication, reverse-proxy upgrade handling, negotiated heartbeat behavior, and wire serialization have unit-test coverage gaps and require runtime verification. | 实现：[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L42)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L75)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L86)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L93)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L115)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L131)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L155)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L164)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java#L473)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java#L448)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java#L61)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java#L63)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java#L1925)<br>测试：[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L77)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L97)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L124)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L155) |
| `RT-TRANSPORT-002`<br>`SOCKJS_FALLBACK` | `{http\|https}://{window.location.hostname}{:window.location.port}/api/stomp/v1` | STOMP 1.1/1.0<br>heartbeat 10000/10000 ms<br>SockJS eventsource, xhr-polling, iframe-xhr-polling, jsonp-polling | fallback: Native WebSocket is unavailable or its initial connection fails.<br>reconnect: A previously successful SockJS connection reconnects through SockJS and restores the captured subscriptions. | The fallback deliberately excludes WebSocket, XHR streaming, XDR, and htmlfile transports even if SockJS supports them.<br>An initial SockJS failure stops transport-level retry, but MainController still attempts to register global subscriptions; each registration returns null because the client is disconnected.<br>Opening config history after both transports fail can recurse indefinitely in addHandler because subscribe returns null and addHandler immediately calls itself again.<br>The HTTP fallback endpoints must remain accessible through the same authentication and reverse-proxy path as /api/stomp/v1. | 实现：[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L47)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L50)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L54)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L115)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L155)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L164)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java#L61)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java#L63)<br>测试：[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L81)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L101)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L130)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L136)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L155) |

## Destination 契约

| ID / Destination | Event | Ember 消费字段 | Handler chain | Lifecycle | REST reconcile | 风险边界 | Source |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `RT-SUB-001`<br>`/events/hostcomponents` | `HostComponentsUpdateEvent` | `hostComponents[].componentName`<br>`hostComponents[].hostName`<br>`hostComponents[].currentState`<br>`hostComponents[].previousState`<br>`hostComponents[].maintenanceState`<br>`hostComponents[].staleConfigs` | `App.hostComponentStatusMapper.map`<br>`App.ServerDataMapper.updatePropertiesByConfig`<br>`App.hostComponentStatusMapper.updateComponentsWithStaleConfigs`<br>`App.componentsStateMapper.updateComponentCountOnStateChange` | Global subscription created after the initial STOMP connection attempt; retained for the application lifetime and restored after reconnect. | Periodic service/component REST refresh can converge model state, but there is no replay of updates lost while disconnected. | A missing hostComponents array throws before any update is applied.<br>Updates for host components not loaded in the Ember store are ignored by updatePropertiesByConfig, while count maintenance may still run.<br>Malformed JSON or an exception in this mapper escapes the shared STOMP message callback. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L210)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/HostComponentsUpdateEvent.java#L29)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/HostComponentUpdate.java#L28)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L45)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L87)<br>[source](../../../../ambari-web/classic/test/mappers/socket/host_component_status_mapper_test.js#L54)<br>[source](../../../../ambari-web/classic/test/mappers/socket/host_component_status_mapper_test.js#L104) |
| `RT-SUB-002`<br>`/events/alerts` | `AlertUpdateEvent` | `summaries[App.clusterId][*].definition_id`<br>`summaries[App.clusterId][*].definition_name`<br>`summaries[App.clusterId][*].summary` | `App.alertSummaryMapper.map`<br>`App.alertDefinitionSummaryMapper.map` | Global application-lifetime subscription restored after reconnect. | Alert pages also load alert data through REST, but the socket client does not request a missed-event snapshot after reconnect. | A missing current-cluster entry causes iteration over undefined and throws.<br>The event is a grouped alert-state summary, not a time-series Metrics payload.<br>Malformed JSON or a downstream summary-mapper exception escapes the shared callback. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L211)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/AlertUpdateEvent.java#L29)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L37)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L88)<br>[source](../../../../ambari-web/classic/test/mappers/socket/alert_summary_mapper_test.js#L33) |
| `RT-SUB-003`<br>`/events/ui_topologies` | `TopologyUpdateEvent` | `eventType`<br>`clusters[App.clusterId].components[].componentName`<br>`clusters[App.clusterId].components[].serviceName`<br>`clusters[App.clusterId].components[].displayName`<br>`clusters[App.clusterId].components[].hostNames`<br>`clusters[App.clusterId].components[].publicHostNames`<br>`clusters[App.clusterId].components[].commandParams.version`<br>`clusters[App.clusterId].components[].lastComponentState`<br>`clusters[App.clusterId].hosts[].hostName` | `App.topologyMapper.map`<br>`App.topologyMapper.applyComponentTopologyChanges`<br>`App.topologyMapper.createHostComponent or deleteHostComponent`<br>`App.componentsStateMapper count update`<br>`App.UpdateController.updateHost for host changes` | Global application-lifetime subscription; UPDATE adds topology, DELETE removes it, and host changes trigger a REST refresh. | Host events explicitly invoke updateHost to fetch complete host models. Component events mutate the local store and rely on later REST refreshes for convergence. | A missing current-cluster entry throws before the mapper can recover.<br>The server serializes hostNames and publicHostNames from separate sets, while the client pairs them by array index; ordering must be verified at runtime.<br>UPDATE components with commandParams.version are skipped by the creation branch.<br>Host payloads are intentionally incomplete; failure of the follow-up REST update leaves partial local topology. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L212)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/TopologyUpdateEvent.java#L36)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/TopologyCluster.java#L33)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/TopologyComponent.java#L34)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/TopologyHost.java#L25)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L41)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L89)<br>[source](../../../../ambari-web/classic/test/mappers/socket/topology_mapper_test.js#L40)<br>[source](../../../../ambari-web/classic/test/mappers/socket/topology_mapper_test.js#L44)<br>[source](../../../../ambari-web/classic/test/mappers/socket/topology_mapper_test.js#L75)<br>[source](../../../../ambari-web/classic/test/mappers/socket/topology_mapper_test.js#L90) |
| `RT-SUB-004`<br>`/events/configs` | `ConfigsUpdateEvent` | `configs[].type` | `default: App.UpdateController.configsChangedHandler`<br>`default: ConfigurationController.updateConfigTags`<br>`default when cluster-env changed: App.UpdateController.updateClusterEnv`<br>`history: MainConfigHistoryController.load(true)` | The global default handler is permanent. The named history handler is added when config history renders and removed when its view is destroyed. | Every event triggers a REST config-tag refresh; cluster-env changes trigger a second REST read. Config history reloads its REST list without consuming event fields. | The default handler runs before the named history handler; an exception or failed JSON parse prevents later handlers for that message.<br>REST refresh failures do not retry at the event layer.<br>addHandler recurses indefinitely if the global subscription does not exist because the STOMP client is disconnected.<br>removeHandler dereferences an absent subscription and throws if lifecycle ordering is broken. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L213)<br>addHandler：[source](../../../../ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js#L166)<br>removeHandler：[source](../../../../ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js#L170)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/ConfigsUpdateEvent.java#L37)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L44)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L90)<br>[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L542)<br>[source](../../../../ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js#L134)<br>[source](../../../../ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js#L148) |
| `RT-SUB-005`<br>`/events/services` | `ServiceUpdateEvent` | `service_name`<br>`maintenance_state`<br>`state` | `App.serviceStateMapper.map`<br>`App.ServerDataMapper.updatePropertiesByConfig`<br>`App.cache.services update` | Global application-lifetime subscription restored after reconnect. | Periodic service/component REST refreshes can converge service state, but missed events are not replayed. | Updates for a Service record that is not loaded are ignored by updatePropertiesByConfig.<br>Only non-null maintenance_state and state values are applied.<br>Malformed JSON or mapper exceptions escape the shared callback. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L214)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/ServiceUpdateEvent.java#L32)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L48)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L91)<br>[source](../../../../ambari-web/classic/test/mappers/socket/service_state_mapper_test.js#L35) |
| `RT-SUB-006`<br>`/events/hosts` | `HostUpdateEvent` | `host_name`<br>`host_status`<br>`host_state`<br>`last_heartbeat_time`<br>`maintenance_state`<br>`alerts_summary` | `App.hostStateMapper.map`<br>`App.ServerDataMapper.updatePropertiesByConfig` | Global application-lifetime subscription restored after reconnect. | Host list/detail REST refreshes can replace missed status, heartbeat, maintenance, and alert-summary values; the channel itself has no snapshot request. | Updates for an unloaded Host record are ignored.<br>Partial events only update non-null fields, retaining all other local values.<br>Malformed JSON or mapper exceptions escape the shared callback. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L215)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/HostUpdateEvent.java#L32)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L49)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L92)<br>[source](../../../../ambari-web/classic/test/mappers/socket/host_state_mapper_test.js#L35) |
| `RT-SUB-007`<br>`/events/alert_definitions` | `AlertDefinitionsUIUpdateEvent` | `eventType`<br>`clusters[App.clusterId].alertDefinitions[]`<br>`clusters[App.clusterId].alertDefinitions[].definitionId`<br>`clusters[App.clusterId].alertDefinitions[].componentName`<br>`clusters[App.clusterId].alertDefinitions[].serviceName` | `App.alertDefinitionsMapperAdapter.map`<br>`UPDATE: normalize id/component_name/service_name and call App.alertDefinitionsMapper.map`<br>`DELETE: remove App.AlertDefinition record` | Global application-lifetime subscription restored after reconnect; emitted server flows use UPDATE and DELETE. | Alert-definition pages initially load REST models. Socket UPDATE/DELETE mutates those models, but reconnect does not force a full definition reload. | A missing current-cluster entry or alertDefinitions array throws.<br>The server enum includes CREATE, but the adapter only handles UPDATE and DELETE; CREATE payloads are ignored after iteration.<br>Full nested Alert Source wire serialization has no classic end-to-end STOMP test and requires runtime verification. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L216)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/AlertDefinitionsUIUpdateEvent.java#L30)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/AlertCluster.java#L34)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/state/alert/AlertDefinition.java#L54)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L52)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L93)<br>[source](../../../../ambari-web/classic/test/mappers/socket/alert_definitions_mapper_adapter_test.js#L38)<br>[source](../../../../ambari-web/classic/test/mappers/socket/alert_definitions_mapper_adapter_test.js#L68) |
| `RT-SUB-008`<br>`/events/alert_group` | `AlertGroupsUpdateEvent` | `updateType`<br>`groups[].cluster_id`<br>`groups[].default`<br>`groups[].definitions`<br>`groups[].id`<br>`groups[].name`<br>`groups[].service_name`<br>`groups[].targets` | `App.alertGroupsMapperAdapter.map`<br>`CREATE/UPDATE: convert definition and target IDs to {id} and call App.alertGroupsMapper.map`<br>`DELETE: remove App.AlertGroup record`<br>`toggle ManageAlertGroupsController.changeTrigger` | Global application-lifetime subscription restored after reconnect. | Alert-group management initially loads groups through REST; socket updates do not force a full reload after reconnect. | A missing groups array throws.<br>Unknown updateType values still toggle the management change trigger without changing records.<br>Definitions and targets are ID-only references; related records must already exist or be reconciled separately.<br>KNOWN_SERVER_BUG: on AlertDefinitionDeleteEvent the server detects membership in definitions but removes definitionId from targets, so the UPDATE can retain a deleted definition and accidentally remove a numerically equal target. React must not treat this push as an authoritative corrected definitions model; reconcile with REST or intentionally fix the server. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L217)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/AlertGroupsUpdateEvent.java#L30)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/AlertGroupUpdate.java#L30)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertGroupsUpdateListener.java#L55)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L38)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L94)<br>[source](../../../../ambari-web/classic/test/mappers/socket/alert_groups_mapper_adapter_test.js#L38)<br>[source](../../../../ambari-web/classic/test/mappers/socket/alert_groups_mapper_adapter_test.js#L63) |
| `RT-SUB-009`<br>`/events/upgrade` | `UpgradeUpdateEvent` | `type`<br>`request_id`<br>`request_status`<br>`suspended`<br>`all CREATE fields through restoreLastUpgrade` | `App.upgradeStateMapper.map`<br>`CREATE: MainAdminStackAndUpgradeController.restoreLastUpgrade({Upgrade:event})`<br>`matching UPDATE: persist upgradeState and isSuspended in controller/DB` | Global application-lifetime subscription; CREATE restores a new/current upgrade and UPDATE mutates only the matching request. | Upgrade controllers load/restore upgrade data through REST and local DB, but the socket mapper ignores UPDATE progress and timestamps and requests no snapshot after reconnect. | An UPDATE for a different request is silently ignored.<br>UPDATE progress_percent, start_time, and end_time are intentionally ignored by this mapper.<br>A CREATE event delegates the full object to restoreLastUpgrade, so its downstream assumptions are part of the contract. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L218)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/UpgradeUpdateEvent.java#L42)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L53)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L95)<br>[source](../../../../ambari-web/classic/test/mappers/socket/upgrade_state_mapper_test.js#L38)<br>[source](../../../../ambari-web/classic/test/mappers/socket/upgrade_state_mapper_test.js#L63) |
| `RT-SUB-010`<br>`/events/requests` | `RequestUpdateEvent` | `requestId`<br>`requestContext`<br>`progressPercent`<br>`requestStatus`<br>`userName`<br>`startTime`<br>`endTime`<br>`Tasks[].id`<br>`Tasks[].requestId`<br>`Tasks[].status`<br>`Tasks[].hostName` | `BackgroundOperationsController.subscribeToUpdates`<br>`BackgroundOperationsController.updateRequests`<br>`parseRequestContext and generateTasksMapOfRequest`<br>`update or insert the visible background-operation model` | Subscribed only after the initial most-recent-requests REST call completes, whether that REST call succeeds or fails; then retained for application lifetime. | The initial REST most-recent list provides the snapshot. Manual/background refreshes can converge later state, but reconnect itself does not reload it. | A missing Tasks array throws in generateTasksMapOfRequest.<br>The visible list is capped by operationsCount; insertion at the cap sets Show More and drops the final entry.<br>The server publisher buffers and merges request events for roughly one second, so messages are state updates rather than a lossless task transition log.<br>A failed initial REST request still enables the subscription, potentially starting from an empty local snapshot. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/background_operations_controller.js#L54)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/RequestUpdateEvent.java#L40)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/publishers/RequestUpdateEventPublisher.java#L39)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L47)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/background_operations_test.js#L738)<br>[source](../../../../ambari-web/classic/test/controllers/global/background_operations_test.js#L754) |
| `RT-SUB-011`<br>`/events/tasks/{taskId}` | `NamedTaskUpdateEvent` | `id`<br>`requestId`<br>`hostName`<br>`status`<br>`errorLog`<br>`outLog`<br>`stderr`<br>`stdout`<br>`structured_out` | `BackgroundOperationsController.handleTaskUpdates`<br>`BackgroundOperationsController.updateTask`<br>`unsubscribe when the client classifies status as finished` | Dynamic subscription is created on entry to TASK_DETAILS for a task outside the client's FAILED/ABORTED/COMPLETED terminal set. A received member of that same client set invokes STOMP unsubscribe; server-side strict COMPLETED may already have removed the task registry entry. | The task detail REST read supplies the initial snapshot. The server does not send a snapshot to a late subscriber, and reconnect does not re-read task detail. | Changing taskId while levelInfo.name remains TASK_DETAILS does not trigger the observer, because it observes only levelInfo.name.<br>Leaving task details does not unsubscribe an unfinished task.<br>HostRoleStatus.isCompleted includes FAILED, TIMEDOUT, ABORTED, COMPLETED, and SKIPPED_FAILED, but the client terminal set omits TIMEDOUT and SKIPPED_FAILED; those events do not trigger client unsubscribe.<br>The server proactively removes the task registry entry only for strict COMPLETED. FAILED and ABORTED normally reach client unsubscribe and server removeId; TIMEDOUT and SKIPPED_FAILED can retain both the browser subscription and server registry entry until disconnect.<br>If the request, host, or task model is absent, updateTask dereferences undefined and throws. | subscribe：[source](../../../../ambari-web/classic/app/controllers/global/background_operations_controller.js#L150)<br>business unsubscribe：[source](../../../../ambari-web/classic/app/controllers/global/background_operations_controller.js#L153)<br>event：[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/NamedTaskUpdateEvent.java#L33)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscriptions.java#L39)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscribeListener.java#L38)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscribeListener.java#L52)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscribeListener.java#L65)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/listeners/tasks/TaskStatusListener.java#L152)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/listeners/tasks/TaskStatusListener.java#L158)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleStatus.java#L84)<br>[source](../../../../ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java#L46)<br>test：[source](../../../../ambari-web/classic/test/controllers/global/background_operations_test.js#L932)<br>[source](../../../../ambari-server/src/test/java/org/apache/ambari/server/api/stomp/NamedTasksSubscriptionsTest.java#L75)<br>[source](../../../../ambari-server/src/test/java/org/apache/ambari/server/api/stomp/NamedTasksSubscriptionsTest.java#L127)<br>[source](../../../../ambari-server/src/test/java/org/apache/ambari/server/events/listeners/tasks/TaskStatusListenerTest.java#L178) |

## Payload Schema

<details>
<summary><code>RT-SUB-001</code> <code>/events/hostcomponents</code></summary>

```json
{
  "type": "object",
  "fields": {
    "hostComponents": {
      "type": "array",
      "required": true,
      "items": {
        "clusterId": "integer",
        "serviceName": "string",
        "hostName": "string",
        "componentName": "string",
        "currentState": "State; omitted when null",
        "previousState": "State; omitted when null",
        "maintenanceState": "MaintenanceState; omitted when null",
        "staleConfigs": "boolean; omitted when null"
      }
    }
  }
}
```

</details>

<details>
<summary><code>RT-SUB-002</code> <code>/events/alerts</code></summary>

```json
{
  "type": "object",
  "fields": {
    "summaries": {
      "type": "map<clusterId,map<definitionName,AlertDefinitionSummary>>",
      "required": true,
      "AlertDefinitionSummary": {
        "definition_id": "integer",
        "definition_name": "string",
        "summary": {
          "OK": "AlertStateValues",
          "WARNING": "AlertStateValues",
          "CRITICAL": "AlertStateValues",
          "UNKNOWN": "AlertStateValues"
        },
        "AlertStateValues": {
          "count": "integer",
          "maintenance_count": "integer",
          "original_timestamp": "integer",
          "latest_text": "string; omitted when null"
        }
      }
    }
  }
}
```

</details>

<details>
<summary><code>RT-SUB-003</code> <code>/events/ui_topologies</code></summary>

```json
{
  "type": "object",
  "fields": {
    "clusters": {
      "type": "map<clusterId,TopologyCluster>; omitted when null",
      "TopologyCluster": {
        "components": "array<TopologyComponent>; omitted when null",
        "hosts": "array<TopologyHost>; omitted when null"
      },
      "TopologyComponent": {
        "componentName": "string",
        "serviceName": "string",
        "displayName": "string",
        "version": "string",
        "hostIds": "array<integer>",
        "hostNames": "array<string>",
        "publicHostNames": "array<string>",
        "componentLevelParams": "map<string,string>",
        "commandParams": "map<string,string>",
        "lastComponentState": "State"
      },
      "TopologyHost": {
        "hostId": "integer",
        "hostName": "string",
        "rackName": "string",
        "ipv4": "string"
      }
    },
    "eventType": "CREATE|UPDATE|DELETE; omitted when null",
    "hash": "string; omitted when null"
  }
}
```

</details>

<details>
<summary><code>RT-SUB-004</code> <code>/events/configs</code></summary>

```json
{
  "type": "object",
  "omittedWhenEmpty": true,
  "fields": {
    "serviceConfigId": "integer",
    "clusterId": "integer",
    "serviceName": "string",
    "groupId": "integer",
    "version": "integer",
    "user": "string",
    "note": "string",
    "hostNames": "array<string>",
    "createTime": "integer",
    "groupName": "string",
    "configs": "array<{clusterId: integer, type: string, tag: string, version: integer}>",
    "changedConfigTypes": "array<string>"
  }
}
```

</details>

<details>
<summary><code>RT-SUB-005</code> <code>/events/services</code></summary>

```json
{
  "type": "object",
  "fields": {
    "cluster_name": "string",
    "maintenance_state": "MaintenanceState; omitted when null",
    "service_name": "string",
    "state": "State; omitted when null"
  }
}
```

</details>

<details>
<summary><code>RT-SUB-006</code> <code>/events/hosts</code></summary>

```json
{
  "type": "object",
  "fields": {
    "cluster_name": "string",
    "host_name": "string",
    "host_status": "string; omitted when null",
    "host_state": "HostState; omitted when null",
    "last_heartbeat_time": "integer; omitted when null",
    "maintenance_state": "MaintenanceState; omitted when null",
    "alerts_summary": "AlertSummaryDTO; omitted when null"
  }
}
```

</details>

<details>
<summary><code>RT-SUB-007</code> <code>/events/alert_definitions</code></summary>

```json
{
  "type": "object",
  "fields": {
    "eventType": "CREATE|UPDATE|DELETE",
    "clusters": {
      "type": "map<clusterId,AlertCluster>",
      "AlertCluster": {
        "alertDefinitions": "array<AlertDefinition>",
        "hostName": "string; omitted when null",
        "staleIntervalMultiplier": "integer; omitted when null"
      },
      "AlertDefinition": {
        "clusterId": "integer",
        "definitionId": "integer",
        "serviceName": "string",
        "componentName": "string",
        "name": "string",
        "scope": "Scope",
        "interval": "integer",
        "enabled": "boolean",
        "source": "Alert Source object",
        "label": "string",
        "description": "string",
        "uuid": "string",
        "ignore_host": "boolean",
        "help_url": "string",
        "repeat_tolerance": "integer",
        "repeat_tolerance_enabled": "boolean"
      }
    }
  }
}
```

</details>

<details>
<summary><code>RT-SUB-008</code> <code>/events/alert_group</code></summary>

```json
{
  "type": "object",
  "omittedWhenEmpty": true,
  "fields": {
    "updateType": "CREATE|UPDATE|DELETE",
    "groups": {
      "type": "array",
      "items": {
        "cluster_id": "integer",
        "default": "boolean",
        "definitions": "array<integer>",
        "id": "integer",
        "name": "string",
        "service_name": "string",
        "targets": "array<integer>"
      }
    }
  }
}
```

</details>

<details>
<summary><code>RT-SUB-009</code> <code>/events/upgrade</code></summary>

```json
{
  "type": "object",
  "omittedWhenEmpty": true,
  "fields": {
    "type": "CREATE|UPDATE",
    "associated_version": "string; full CREATE event",
    "cluster_id": "integer",
    "direction": "UPGRADE|DOWNGRADE; full CREATE event",
    "downgrade_allowed": "boolean; full CREATE event",
    "request_id": "integer",
    "request_status": "HostRoleStatus",
    "skip_failures": "boolean; full CREATE event",
    "skip_service_check_failures": "boolean; full CREATE event",
    "upgrade_type": "UpgradeType; full CREATE event",
    "start_time": "integer",
    "end_time": "integer",
    "upgrade_id": "integer; full CREATE event",
    "suspended": "boolean",
    "progress_percent": "number",
    "revert_allowed": "boolean; full CREATE event"
  }
}
```

</details>

<details>
<summary><code>RT-SUB-010</code> <code>/events/requests</code></summary>

```json
{
  "type": "object",
  "fields": {
    "clusterName": "string; omitted when null",
    "endTime": "integer; omitted when null",
    "requestId": "integer",
    "progressPercent": "number; omitted when null",
    "requestContext": "string; omitted when null",
    "requestStatus": "HostRoleStatus; omitted when null",
    "startTime": "integer; omitted when null",
    "userName": "string; omitted when null",
    "Tasks": {
      "type": "array",
      "items": {
        "id": "integer",
        "requestId": "integer",
        "status": "HostRoleStatus",
        "hostName": "string"
      }
    }
  }
}
```

</details>

<details>
<summary><code>RT-SUB-011</code> <code>/events/tasks/{taskId}</code></summary>

```json
{
  "type": "object",
  "fields": {
    "id": "integer",
    "requestId": "integer",
    "hostName": "string",
    "endTime": "integer; omitted when null",
    "status": "HostRoleStatus",
    "errorLog": "string; omitted when null",
    "outLog": "string; omitted when null",
    "stderr": "string; omitted when null",
    "stdout": "string; omitted when null",
    "structured_out": "string; omitted when null"
  }
}
```

</details>

## Lifecycle

| ID / 名称 | 行为 | 风险边界 | Source / Test |
| --- | --- | --- | --- |
| `RT-LIFE-001`<br>`APPLICATION_BOOTSTRAP` | MainController connects once, starts all global subscriptions on native success, or waits for the nested SockJS attempt to settle after native failure. Cluster REST loading starts independently. | Both transports failing still invokes startSubscriptions, but disconnected subscribe calls return null and are not queued.<br>There is no global unsubscribe or disconnect call on logout in classic application code. | 实现：[source](../../../../ambari-web/classic/app/controllers/main.js#L53)<br>[source](../../../../ambari-web/classic/app/controllers/global/update_controller.js#L209)<br>测试：[source](../../../../ambari-web/classic/test/controllers/main_test.js#L28)<br>[source](../../../../ambari-web/classic/test/controllers/global/update_controller_test.js#L72) |
| `RT-LIFE-002`<br>`MESSAGE_DISPATCH` | Each broker message body is parsed once per registered handler iteration and delivered synchronously in handler insertion order. A destination has one default handler plus optional named handlers. | JSON.parse has no try/catch.<br>One throwing handler prevents later handlers for the same message and provides no isolation or replay.<br>A second default subscribe for an existing destination logs an error and retains the original handler. | 实现：[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L211)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L240)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L258)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L271)<br>测试：[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L206)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L239)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L265)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L285) |
| `RT-LIFE-003`<br>`RECONNECT_AND_RESTORE` | After a previously successful connection fails, the wrapper waits 6000 ms, disconnects an open client, reconnects with the current transport, clears the subscription map, and recreates every captured default and named handler. | There is no replay cursor, sequence number, or forced REST snapshot after reconnect.<br>Subscription changes during the delay or reconnect can be resurrected or lost because restoration uses a shallow pre-delay snapshot.<br>disconnect does not clear wrapper subscriptions, isConnected, timerId, or client. | 实现：[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L151)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L155)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L164)<br>[source](../../../../ambari-web/classic/app/utils/stomp_client.js#L184)<br>测试：[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L107)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L114)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L143)<br>[source](../../../../ambari-web/classic/test/utils/stomp_client_test.js#L169) |
| `RT-LIFE-004`<br>`DYNAMIC_HANDLER_OWNERSHIP` | Config history owns a named handler on the permanent configs destination. Task detail owns an entire dynamic destination until selected client terminal states arrive. | Named handler removal assumes the destination exists.<br>Task ownership is not released merely by leaving TASK_DETAILS.<br>Client and server terminal-state definitions are inconsistent. | 实现：[source](../../../../ambari-web/classic/app/views/main/dashboard/config_history_view.js#L58)<br>[source](../../../../ambari-web/classic/app/views/main/dashboard/config_history_view.js#L69)<br>[source](../../../../ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js#L165)<br>[source](../../../../ambari-web/classic/app/controllers/global/background_operations_controller.js#L142)<br>测试：[source](../../../../ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js#L134)<br>[source](../../../../ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js#L148)<br>[source](../../../../ambari-web/classic/test/controllers/global/background_operations_test.js#L932) |
