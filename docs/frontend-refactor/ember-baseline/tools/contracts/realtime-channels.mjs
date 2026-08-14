// Static, source-audited contracts for the classic UI's non-Metrics STOMP channels.

const location = (source, line) => ({ source, line });

const stompClientSource = "ambari-web/classic/app/utils/stomp_client.js";
const stompClientTest = "ambari-web/classic/test/utils/stomp_client_test.js";

const transports = [
  {
    id: "RT-TRANSPORT-001",
    kind: "NATIVE_WEBSOCKET",
    urlTemplate: "{ws|wss}://{window.location.hostname}{:window.location.port}/api/stomp/v1/websocket",
    protocols: {
      socketSchemes: ["ws", "wss"],
      stompVersions: ["1.1", "1.0"]
    },
    connectHeaders: {},
    heartbeat: {
      clientOutgoingMs: 10000,
      clientIncomingMs: 10000,
      serverDefaultMs: 10000,
      serverProperty: "api.heartbeat.interval",
      staleConnectionRule: "The bundled STOMP client closes the socket after two negotiated incoming heartbeat periods without server activity."
    },
    fallback: {
      trigger: "Initial native WebSocket connection error before any successful connection.",
      transportId: "RT-TRANSPORT-002",
      automaticAfterEstablishedConnection: false
    },
    reconnect: {
      delayMs: 6000,
      policy: "After any previously successful connection is lost, reconnect with the same transport and restore the captured in-memory subscriptions.",
      eventReplay: false
    },
    sourceLocations: [
      location(stompClientSource, 42),
      location(stompClientSource, 75),
      location(stompClientSource, 86),
      location(stompClientSource, 93),
      location(stompClientSource, 115),
      location(stompClientSource, 131),
      location(stompClientSource, 155),
      location(stompClientSource, 164),
      location("ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java", 473),
      location("ambari-server/src/main/java/org/apache/ambari/server/controller/AmbariServer.java", 448),
      location("ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java", 61),
      location("ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java", 63),
      location("ambari-server/src/main/java/org/apache/ambari/server/configuration/Configuration.java", 1925)
    ],
    testLocations: [
      location(stompClientTest, 77),
      location(stompClientTest, 97),
      location(stompClientTest, 124),
      location(stompClientTest, 155)
    ],
    failureBoundaries: [
      "There is no REST polling substitute when both native WebSocket and SockJS fail.",
      "CONNECT sends no explicit login, passcode, token, or CSRF header. The endpoint allows every origin pattern, while the enclosing /api/* path still passes through the Spring Security filter; the authenticated principal and browser credential behavior must be verified at the HTTP upgrade/fallback handshake.",
      "isConnected is set after the first success and is never reset by the wrapper, so later failures keep scheduling reconnect attempts.",
      "The reconnect snapshot is shallow, has no event replay, and can restore a destination removed during the six-second delay or discard one added during reconnection.",
      "Authentication, reverse-proxy upgrade handling, negotiated heartbeat behavior, and wire serialization have unit-test coverage gaps and require runtime verification."
    ]
  },
  {
    id: "RT-TRANSPORT-002",
    kind: "SOCKJS_FALLBACK",
    urlTemplate: "{http|https}://{window.location.hostname}{:window.location.port}/api/stomp/v1",
    protocols: {
      socketSchemes: ["http", "https"],
      stompVersions: ["1.1", "1.0"],
      sockJsTransports: ["eventsource", "xhr-polling", "iframe-xhr-polling", "jsonp-polling"]
    },
    connectHeaders: {},
    heartbeat: {
      clientOutgoingMs: 10000,
      clientIncomingMs: 10000,
      serverDefaultMs: 10000,
      serverProperty: "api.heartbeat.interval",
      sockJsHeartbeatSource: "Spring SockJS endpoint heartbeat uses the same server property only until STOMP heartbeats are negotiated; Spring disables SockJS heartbeats for that connection after STOMP heartbeat negotiation."
    },
    fallback: {
      trigger: "Native WebSocket is unavailable or its initial connection fails.",
      transportId: null,
      automaticAfterEstablishedConnection: false,
      excludesSockJsWebSocketTransport: true
    },
    reconnect: {
      delayMs: 6000,
      policy: "A previously successful SockJS connection reconnects through SockJS and restores the captured subscriptions.",
      initialSockJsFailureRetries: false,
      eventReplay: false
    },
    sourceLocations: [
      location(stompClientSource, 47),
      location(stompClientSource, 50),
      location(stompClientSource, 54),
      location(stompClientSource, 115),
      location(stompClientSource, 155),
      location(stompClientSource, 164),
      location("ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java", 61),
      location("ambari-server/src/main/java/org/apache/ambari/server/configuration/spring/ApiStompConfig.java", 63)
    ],
    testLocations: [
      location(stompClientTest, 81),
      location(stompClientTest, 101),
      location(stompClientTest, 130),
      location(stompClientTest, 136),
      location(stompClientTest, 155)
    ],
    failureBoundaries: [
      "The fallback deliberately excludes WebSocket, XHR streaming, XDR, and htmlfile transports even if SockJS supports them.",
      "An initial SockJS failure stops transport-level retry, but MainController still attempts to register global subscriptions; each registration returns null because the client is disconnected.",
      "Opening config history after both transports fail can recurse indefinitely in addHandler because subscribe returns null and addHandler immediately calls itself again.",
      "The HTTP fallback endpoints must remain accessible through the same authentication and reverse-proxy path as /api/stomp/v1."
    ]
  }
];

const subscriptions = [
  {
    id: "RT-SUB-001",
    destinationTemplate: "/events/hostcomponents",
    eventClass: "HostComponentsUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/HostComponentsUpdateEvent.java", 29),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/HostComponentUpdate.java", 28),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 45)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        hostComponents: {
          type: "array",
          required: true,
          items: {
            clusterId: "integer",
            serviceName: "string",
            hostName: "string",
            componentName: "string",
            currentState: "State; omitted when null",
            previousState: "State; omitted when null",
            maintenanceState: "MaintenanceState; omitted when null",
            staleConfigs: "boolean; omitted when null"
          }
        }
      }
    },
    consumedFields: [
      "hostComponents[].componentName",
      "hostComponents[].hostName",
      "hostComponents[].currentState",
      "hostComponents[].previousState",
      "hostComponents[].maintenanceState",
      "hostComponents[].staleConfigs"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 210)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: [
      "App.hostComponentStatusMapper.map",
      "App.ServerDataMapper.updatePropertiesByConfig",
      "App.hostComponentStatusMapper.updateComponentsWithStaleConfigs",
      "App.componentsStateMapper.updateComponentCountOnStateChange"
    ],
    lifecycle: "Global subscription created after the initial STOMP connection attempt; retained for the application lifetime and restored after reconnect.",
    restReconciliation: "Periodic service/component REST refresh can converge model state, but there is no replay of updates lost while disconnected.",
    clusterFiltering: "No clusterId check is performed before looking up componentName_hostName in the current store.",
    failureBoundaries: [
      "A missing hostComponents array throws before any update is applied.",
      "Updates for host components not loaded in the Ember store are ignored by updatePropertiesByConfig, while count maintenance may still run.",
      "Malformed JSON or an exception in this mapper escapes the shared STOMP message callback."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 87),
      location("ambari-web/classic/test/mappers/socket/host_component_status_mapper_test.js", 54),
      location("ambari-web/classic/test/mappers/socket/host_component_status_mapper_test.js", 104)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_OPERATIONAL_STATE"
  },
  {
    id: "RT-SUB-002",
    destinationTemplate: "/events/alerts",
    eventClass: "AlertUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/AlertUpdateEvent.java", 29),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 37)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        summaries: {
          type: "map<clusterId,map<definitionName,AlertDefinitionSummary>>",
          required: true,
          AlertDefinitionSummary: {
            definition_id: "integer",
            definition_name: "string",
            summary: {
              OK: "AlertStateValues",
              WARNING: "AlertStateValues",
              CRITICAL: "AlertStateValues",
              UNKNOWN: "AlertStateValues"
            },
            AlertStateValues: {
              count: "integer",
              maintenance_count: "integer",
              original_timestamp: "integer",
              latest_text: "string; omitted when null"
            }
          }
        }
      }
    },
    consumedFields: [
      "summaries[App.clusterId][*].definition_id",
      "summaries[App.clusterId][*].definition_name",
      "summaries[App.clusterId][*].summary"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 211)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: ["App.alertSummaryMapper.map", "App.alertDefinitionSummaryMapper.map"],
    lifecycle: "Global application-lifetime subscription restored after reconnect.",
    restReconciliation: "Alert pages also load alert data through REST, but the socket client does not request a missed-event snapshot after reconnect.",
    clusterFiltering: "Directly selects summaries[App.clusterId]; it does not process other cluster entries.",
    failureBoundaries: [
      "A missing current-cluster entry causes iteration over undefined and throws.",
      "The event is a grouped alert-state summary, not a time-series Metrics payload.",
      "Malformed JSON or a downstream summary-mapper exception escapes the shared callback."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 88),
      location("ambari-web/classic/test/mappers/socket/alert_summary_mapper_test.js", 33)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_ALERTING"
  },
  {
    id: "RT-SUB-003",
    destinationTemplate: "/events/ui_topologies",
    eventClass: "TopologyUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/TopologyUpdateEvent.java", 36),
      location("ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/TopologyCluster.java", 33),
      location("ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/TopologyComponent.java", 34),
      location("ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/TopologyHost.java", 25),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 41)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        clusters: {
          type: "map<clusterId,TopologyCluster>; omitted when null",
          TopologyCluster: {
            components: "array<TopologyComponent>; omitted when null",
            hosts: "array<TopologyHost>; omitted when null"
          },
          TopologyComponent: {
            componentName: "string",
            serviceName: "string",
            displayName: "string",
            version: "string",
            hostIds: "array<integer>",
            hostNames: "array<string>",
            publicHostNames: "array<string>",
            componentLevelParams: "map<string,string>",
            commandParams: "map<string,string>",
            lastComponentState: "State"
          },
          TopologyHost: {
            hostId: "integer",
            hostName: "string",
            rackName: "string",
            ipv4: "string"
          }
        },
        eventType: "CREATE|UPDATE|DELETE; omitted when null",
        hash: "string; omitted when null"
      }
    },
    consumedFields: [
      "eventType",
      "clusters[App.clusterId].components[].componentName",
      "clusters[App.clusterId].components[].serviceName",
      "clusters[App.clusterId].components[].displayName",
      "clusters[App.clusterId].components[].hostNames",
      "clusters[App.clusterId].components[].publicHostNames",
      "clusters[App.clusterId].components[].commandParams.version",
      "clusters[App.clusterId].components[].lastComponentState",
      "clusters[App.clusterId].hosts[].hostName"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 212)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: [
      "App.topologyMapper.map",
      "App.topologyMapper.applyComponentTopologyChanges",
      "App.topologyMapper.createHostComponent or deleteHostComponent",
      "App.componentsStateMapper count update",
      "App.UpdateController.updateHost for host changes"
    ],
    lifecycle: "Global application-lifetime subscription; UPDATE adds topology, DELETE removes it, and host changes trigger a REST refresh.",
    restReconciliation: "Host events explicitly invoke updateHost to fetch complete host models. Component events mutate the local store and rely on later REST refreshes for convergence.",
    clusterFiltering: "Directly selects clusters[App.clusterId] and ignores other map entries.",
    failureBoundaries: [
      "A missing current-cluster entry throws before the mapper can recover.",
      "The server serializes hostNames and publicHostNames from separate sets, while the client pairs them by array index; ordering must be verified at runtime.",
      "UPDATE components with commandParams.version are skipped by the creation branch.",
      "Host payloads are intentionally incomplete; failure of the follow-up REST update leaves partial local topology."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 89),
      location("ambari-web/classic/test/mappers/socket/topology_mapper_test.js", 40),
      location("ambari-web/classic/test/mappers/socket/topology_mapper_test.js", 44),
      location("ambari-web/classic/test/mappers/socket/topology_mapper_test.js", 75),
      location("ambari-web/classic/test/mappers/socket/topology_mapper_test.js", 90)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_TOPOLOGY"
  },
  {
    id: "RT-SUB-004",
    destinationTemplate: "/events/configs",
    eventClass: "ConfigsUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/ConfigsUpdateEvent.java", 37),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 44)
    ],
    payloadSchema: {
      type: "object",
      omittedWhenEmpty: true,
      fields: {
        serviceConfigId: "integer",
        clusterId: "integer",
        serviceName: "string",
        groupId: "integer",
        version: "integer",
        user: "string",
        note: "string",
        hostNames: "array<string>",
        createTime: "integer",
        groupName: "string",
        configs: "array<{clusterId: integer, type: string, tag: string, version: integer}>",
        changedConfigTypes: "array<string>"
      }
    },
    consumedFields: ["configs[].type"],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 213)],
    addHandlerSites: [location("ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js", 166)],
    removeHandlerSites: [location("ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js", 170)],
    unsubscribeSites: [],
    handlerChain: [
      "default: App.UpdateController.configsChangedHandler",
      "default: ConfigurationController.updateConfigTags",
      "default when cluster-env changed: App.UpdateController.updateClusterEnv",
      "history: MainConfigHistoryController.load(true)"
    ],
    lifecycle: "The global default handler is permanent. The named history handler is added when config history renders and removed when its view is destroyed.",
    restReconciliation: "Every event triggers a REST config-tag refresh; cluster-env changes trigger a second REST read. Config history reloads its REST list without consuming event fields.",
    clusterFiltering: "No clusterId check is performed before refreshing the current cluster's configuration state.",
    failureBoundaries: [
      "The default handler runs before the named history handler; an exception or failed JSON parse prevents later handlers for that message.",
      "REST refresh failures do not retry at the event layer.",
      "addHandler recurses indefinitely if the global subscription does not exist because the STOMP client is disconnected.",
      "removeHandler dereferences an absent subscription and throws if lifecycle ordering is broken."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 90),
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 542),
      location("ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js", 134),
      location("ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js", 148)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_CONFIGURATION"
  },
  {
    id: "RT-SUB-005",
    destinationTemplate: "/events/services",
    eventClass: "ServiceUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/ServiceUpdateEvent.java", 32),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 48)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        cluster_name: "string",
        maintenance_state: "MaintenanceState; omitted when null",
        service_name: "string",
        state: "State; omitted when null"
      }
    },
    consumedFields: ["service_name", "maintenance_state", "state"],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 214)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: ["App.serviceStateMapper.map", "App.ServerDataMapper.updatePropertiesByConfig", "App.cache.services update"],
    lifecycle: "Global application-lifetime subscription restored after reconnect.",
    restReconciliation: "Periodic service/component REST refreshes can converge service state, but missed events are not replayed.",
    clusterFiltering: "cluster_name is present but not checked; service_name is resolved in the current Ember store.",
    failureBoundaries: [
      "Updates for a Service record that is not loaded are ignored by updatePropertiesByConfig.",
      "Only non-null maintenance_state and state values are applied.",
      "Malformed JSON or mapper exceptions escape the shared callback."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 91),
      location("ambari-web/classic/test/mappers/socket/service_state_mapper_test.js", 35)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_OPERATIONAL_STATE"
  },
  {
    id: "RT-SUB-006",
    destinationTemplate: "/events/hosts",
    eventClass: "HostUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/HostUpdateEvent.java", 32),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 49)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        cluster_name: "string",
        host_name: "string",
        host_status: "string; omitted when null",
        host_state: "HostState; omitted when null",
        last_heartbeat_time: "integer; omitted when null",
        maintenance_state: "MaintenanceState; omitted when null",
        alerts_summary: "AlertSummaryDTO; omitted when null"
      }
    },
    consumedFields: [
      "host_name",
      "host_status",
      "host_state",
      "last_heartbeat_time",
      "maintenance_state",
      "alerts_summary"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 215)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: ["App.hostStateMapper.map", "App.ServerDataMapper.updatePropertiesByConfig"],
    lifecycle: "Global application-lifetime subscription restored after reconnect.",
    restReconciliation: "Host list/detail REST refreshes can replace missed status, heartbeat, maintenance, and alert-summary values; the channel itself has no snapshot request.",
    clusterFiltering: "cluster_name is not checked; host_name is resolved in the current Ember store.",
    failureBoundaries: [
      "Updates for an unloaded Host record are ignored.",
      "Partial events only update non-null fields, retaining all other local values.",
      "Malformed JSON or mapper exceptions escape the shared callback."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 92),
      location("ambari-web/classic/test/mappers/socket/host_state_mapper_test.js", 35)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_OPERATIONAL_STATE"
  },
  {
    id: "RT-SUB-007",
    destinationTemplate: "/events/alert_definitions",
    eventClass: "AlertDefinitionsUIUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/AlertDefinitionsUIUpdateEvent.java", 30),
      location("ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/AlertCluster.java", 34),
      location("ambari-server/src/main/java/org/apache/ambari/server/state/alert/AlertDefinition.java", 54),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 52)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        eventType: "CREATE|UPDATE|DELETE",
        clusters: {
          type: "map<clusterId,AlertCluster>",
          AlertCluster: {
            alertDefinitions: "array<AlertDefinition>",
            hostName: "string; omitted when null",
            staleIntervalMultiplier: "integer; omitted when null"
          },
          AlertDefinition: {
            clusterId: "integer",
            definitionId: "integer",
            serviceName: "string",
            componentName: "string",
            name: "string",
            scope: "Scope",
            interval: "integer",
            enabled: "boolean",
            source: "Alert Source object",
            label: "string",
            description: "string",
            uuid: "string",
            ignore_host: "boolean",
            help_url: "string",
            repeat_tolerance: "integer",
            repeat_tolerance_enabled: "boolean"
          }
        }
      }
    },
    consumedFields: [
      "eventType",
      "clusters[App.clusterId].alertDefinitions[]",
      "clusters[App.clusterId].alertDefinitions[].definitionId",
      "clusters[App.clusterId].alertDefinitions[].componentName",
      "clusters[App.clusterId].alertDefinitions[].serviceName"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 216)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: [
      "App.alertDefinitionsMapperAdapter.map",
      "UPDATE: normalize id/component_name/service_name and call App.alertDefinitionsMapper.map",
      "DELETE: remove App.AlertDefinition record"
    ],
    lifecycle: "Global application-lifetime subscription restored after reconnect; emitted server flows use UPDATE and DELETE.",
    restReconciliation: "Alert-definition pages initially load REST models. Socket UPDATE/DELETE mutates those models, but reconnect does not force a full definition reload.",
    clusterFiltering: "Directly selects clusters[App.clusterId] and ignores other map entries.",
    failureBoundaries: [
      "A missing current-cluster entry or alertDefinitions array throws.",
      "The server enum includes CREATE, but the adapter only handles UPDATE and DELETE; CREATE payloads are ignored after iteration.",
      "Full nested Alert Source wire serialization has no classic end-to-end STOMP test and requires runtime verification."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 93),
      location("ambari-web/classic/test/mappers/socket/alert_definitions_mapper_adapter_test.js", 38),
      location("ambari-web/classic/test/mappers/socket/alert_definitions_mapper_adapter_test.js", 68)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_ALERT_CONFIGURATION"
  },
  {
    id: "RT-SUB-008",
    destinationTemplate: "/events/alert_group",
    eventClass: "AlertGroupsUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/AlertGroupsUpdateEvent.java", 30),
      location("ambari-server/src/main/java/org/apache/ambari/server/agent/stomp/dto/AlertGroupUpdate.java", 30),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/listeners/alerts/AlertGroupsUpdateListener.java", 55),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 38)
    ],
    payloadSchema: {
      type: "object",
      omittedWhenEmpty: true,
      fields: {
        updateType: "CREATE|UPDATE|DELETE",
        groups: {
          type: "array",
          items: {
            cluster_id: "integer",
            default: "boolean",
            definitions: "array<integer>",
            id: "integer",
            name: "string",
            service_name: "string",
            targets: "array<integer>"
          }
        }
      }
    },
    consumedFields: [
      "updateType",
      "groups[].cluster_id",
      "groups[].default",
      "groups[].definitions",
      "groups[].id",
      "groups[].name",
      "groups[].service_name",
      "groups[].targets"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 217)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: [
      "App.alertGroupsMapperAdapter.map",
      "CREATE/UPDATE: convert definition and target IDs to {id} and call App.alertGroupsMapper.map",
      "DELETE: remove App.AlertGroup record",
      "toggle ManageAlertGroupsController.changeTrigger"
    ],
    lifecycle: "Global application-lifetime subscription restored after reconnect.",
    restReconciliation: "Alert-group management initially loads groups through REST; socket updates do not force a full reload after reconnect.",
    clusterFiltering: "cluster_id is passed to the downstream REST-shaped mapper but is not checked against the current cluster before mutation.",
    failureBoundaries: [
      "A missing groups array throws.",
      "Unknown updateType values still toggle the management change trigger without changing records.",
      "Definitions and targets are ID-only references; related records must already exist or be reconciled separately.",
      "KNOWN_SERVER_BUG: on AlertDefinitionDeleteEvent the server detects membership in definitions but removes definitionId from targets, so the UPDATE can retain a deleted definition and accidentally remove a numerically equal target. React must not treat this push as an authoritative corrected definitions model; reconcile with REST or intentionally fix the server."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 94),
      location("ambari-web/classic/test/mappers/socket/alert_groups_mapper_adapter_test.js", 38),
      location("ambari-web/classic/test/mappers/socket/alert_groups_mapper_adapter_test.js", 63)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_ALERT_CONFIGURATION"
  },
  {
    id: "RT-SUB-009",
    destinationTemplate: "/events/upgrade",
    eventClass: "UpgradeUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/UpgradeUpdateEvent.java", 42),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 53)
    ],
    payloadSchema: {
      type: "object",
      omittedWhenEmpty: true,
      fields: {
        type: "CREATE|UPDATE",
        associated_version: "string; full CREATE event",
        cluster_id: "integer",
        direction: "UPGRADE|DOWNGRADE; full CREATE event",
        downgrade_allowed: "boolean; full CREATE event",
        request_id: "integer",
        request_status: "HostRoleStatus",
        skip_failures: "boolean; full CREATE event",
        skip_service_check_failures: "boolean; full CREATE event",
        upgrade_type: "UpgradeType; full CREATE event",
        start_time: "integer",
        end_time: "integer",
        upgrade_id: "integer; full CREATE event",
        suspended: "boolean",
        progress_percent: "number",
        revert_allowed: "boolean; full CREATE event"
      }
    },
    consumedFields: [
      "type",
      "request_id",
      "request_status",
      "suspended",
      "all CREATE fields through restoreLastUpgrade"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/update_controller.js", 218)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: [
      "App.upgradeStateMapper.map",
      "CREATE: MainAdminStackAndUpgradeController.restoreLastUpgrade({Upgrade:event})",
      "matching UPDATE: persist upgradeState and isSuspended in controller/DB"
    ],
    lifecycle: "Global application-lifetime subscription; CREATE restores a new/current upgrade and UPDATE mutates only the matching request.",
    restReconciliation: "Upgrade controllers load/restore upgrade data through REST and local DB, but the socket mapper ignores UPDATE progress and timestamps and requests no snapshot after reconnect.",
    clusterFiltering: "cluster_id is not checked. UPDATE is filtered by controller.upgradeId === event.request_id.",
    failureBoundaries: [
      "An UPDATE for a different request is silently ignored.",
      "UPDATE progress_percent, start_time, and end_time are intentionally ignored by this mapper.",
      "A CREATE event delegates the full object to restoreLastUpgrade, so its downstream assumptions are part of the contract."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 95),
      location("ambari-web/classic/test/mappers/socket/upgrade_state_mapper_test.js", 38),
      location("ambari-web/classic/test/mappers/socket/upgrade_state_mapper_test.js", 63)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_UPGRADE_STATE"
  },
  {
    id: "RT-SUB-010",
    destinationTemplate: "/events/requests",
    eventClass: "RequestUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/RequestUpdateEvent.java", 40),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/publishers/RequestUpdateEventPublisher.java", 39),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 47)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        clusterName: "string; omitted when null",
        endTime: "integer; omitted when null",
        requestId: "integer",
        progressPercent: "number; omitted when null",
        requestContext: "string; omitted when null",
        requestStatus: "HostRoleStatus; omitted when null",
        startTime: "integer; omitted when null",
        userName: "string; omitted when null",
        Tasks: {
          type: "array",
          items: {
            id: "integer",
            requestId: "integer",
            status: "HostRoleStatus",
            hostName: "string"
          }
        }
      }
    },
    consumedFields: [
      "requestId",
      "requestContext",
      "progressPercent",
      "requestStatus",
      "userName",
      "startTime",
      "endTime",
      "Tasks[].id",
      "Tasks[].requestId",
      "Tasks[].status",
      "Tasks[].hostName"
    ],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/background_operations_controller.js", 54)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [],
    handlerChain: [
      "BackgroundOperationsController.subscribeToUpdates",
      "BackgroundOperationsController.updateRequests",
      "parseRequestContext and generateTasksMapOfRequest",
      "update or insert the visible background-operation model"
    ],
    lifecycle: "Subscribed only after the initial most-recent-requests REST call completes, whether that REST call succeeds or fails; then retained for application lifetime.",
    restReconciliation: "The initial REST most-recent list provides the snapshot. Manual/background refreshes can converge later state, but reconnect itself does not reload it.",
    clusterFiltering: "clusterName is not checked. Requests whose context contains upgrading or downgrading are excluded from background operations.",
    failureBoundaries: [
      "A missing Tasks array throws in generateTasksMapOfRequest.",
      "The visible list is capped by operationsCount; insertion at the cap sets Show More and drops the final entry.",
      "The server publisher buffers and merges request events for roughly one second, so messages are state updates rather than a lossless task transition log.",
      "A failed initial REST request still enables the subscription, potentially starting from an empty local snapshot."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/background_operations_test.js", 738),
      location("ambari-web/classic/test/controllers/global/background_operations_test.js", 754)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_REQUEST_PROGRESS"
  },
  {
    id: "RT-SUB-011",
    destinationTemplate: "/events/tasks/{taskId}",
    eventClass: "NamedTaskUpdateEvent",
    eventSourceSites: [
      location("ambari-server/src/main/java/org/apache/ambari/server/events/NamedTaskUpdateEvent.java", 33),
      location("ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscriptions.java", 39),
      location("ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscribeListener.java", 38),
      location("ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscribeListener.java", 52),
      location("ambari-server/src/main/java/org/apache/ambari/server/api/stomp/NamedTasksSubscribeListener.java", 65),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/listeners/tasks/TaskStatusListener.java", 152),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/listeners/tasks/TaskStatusListener.java", 158),
      location("ambari-server/src/main/java/org/apache/ambari/server/actionmanager/HostRoleStatus.java", 84),
      location("ambari-server/src/main/java/org/apache/ambari/server/events/DefaultMessageEmitter.java", 46)
    ],
    payloadSchema: {
      type: "object",
      fields: {
        id: "integer",
        requestId: "integer",
        hostName: "string",
        endTime: "integer; omitted when null",
        status: "HostRoleStatus",
        errorLog: "string; omitted when null",
        outLog: "string; omitted when null",
        stderr: "string; omitted when null",
        stdout: "string; omitted when null",
        structured_out: "string; omitted when null"
      }
    },
    consumedFields: ["id", "requestId", "hostName", "status", "errorLog", "outLog", "stderr", "stdout", "structured_out"],
    subscribeSites: [location("ambari-web/classic/app/controllers/global/background_operations_controller.js", 150)],
    addHandlerSites: [],
    removeHandlerSites: [],
    unsubscribeSites: [location("ambari-web/classic/app/controllers/global/background_operations_controller.js", 153)],
    handlerChain: [
      "BackgroundOperationsController.handleTaskUpdates",
      "BackgroundOperationsController.updateTask",
      "unsubscribe when the client classifies status as finished"
    ],
    lifecycle: "Dynamic subscription is created on entry to TASK_DETAILS for a task outside the client's FAILED/ABORTED/COMPLETED terminal set. A received member of that same client set invokes STOMP unsubscribe; server-side strict COMPLETED may already have removed the task registry entry.",
    restReconciliation: "The task detail REST read supplies the initial snapshot. The server does not send a snapshot to a late subscriber, and reconnect does not re-read task detail.",
    clusterFiltering: "No cluster field exists. The client resolves requestId, hostName, and task id against its current background-operation model.",
    failureBoundaries: [
      "Changing taskId while levelInfo.name remains TASK_DETAILS does not trigger the observer, because it observes only levelInfo.name.",
      "Leaving task details does not unsubscribe an unfinished task.",
      "HostRoleStatus.isCompleted includes FAILED, TIMEDOUT, ABORTED, COMPLETED, and SKIPPED_FAILED, but the client terminal set omits TIMEDOUT and SKIPPED_FAILED; those events do not trigger client unsubscribe.",
      "The server proactively removes the task registry entry only for strict COMPLETED. FAILED and ABORTED normally reach client unsubscribe and server removeId; TIMEDOUT and SKIPPED_FAILED can retain both the browser subscription and server registry entry until disconnect.",
      "If the request, host, or task model is absent, updateTask dereferences undefined and throws."
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/global/background_operations_test.js", 932),
      location("ambari-server/src/test/java/org/apache/ambari/server/api/stomp/NamedTasksSubscriptionsTest.java", 75),
      location("ambari-server/src/test/java/org/apache/ambari/server/api/stomp/NamedTasksSubscriptionsTest.java", 127),
      location("ambari-server/src/test/java/org/apache/ambari/server/events/listeners/tasks/TaskStatusListenerTest.java", 178)
    ],
    metricsDisposition: "INCLUDED_NON_METRICS_TASK_STATUS_AND_LOGS"
  }
];

const lifecycle = [
  {
    id: "RT-LIFE-001",
    name: "APPLICATION_BOOTSTRAP",
    behavior: "MainController connects once, starts all global subscriptions on native success, or waits for the nested SockJS attempt to settle after native failure. Cluster REST loading starts independently.",
    sourceLocations: [
      location("ambari-web/classic/app/controllers/main.js", 53),
      location("ambari-web/classic/app/controllers/global/update_controller.js", 209)
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/main_test.js", 28),
      location("ambari-web/classic/test/controllers/global/update_controller_test.js", 72)
    ],
    failureBoundaries: [
      "Both transports failing still invokes startSubscriptions, but disconnected subscribe calls return null and are not queued.",
      "There is no global unsubscribe or disconnect call on logout in classic application code."
    ]
  },
  {
    id: "RT-LIFE-002",
    name: "MESSAGE_DISPATCH",
    behavior: "Each broker message body is parsed once per registered handler iteration and delivered synchronously in handler insertion order. A destination has one default handler plus optional named handlers.",
    sourceLocations: [
      location(stompClientSource, 211),
      location(stompClientSource, 240),
      location(stompClientSource, 258),
      location(stompClientSource, 271)
    ],
    testLocations: [
      location(stompClientTest, 206),
      location(stompClientTest, 239),
      location(stompClientTest, 265),
      location(stompClientTest, 285)
    ],
    failureBoundaries: [
      "JSON.parse has no try/catch.",
      "One throwing handler prevents later handlers for the same message and provides no isolation or replay.",
      "A second default subscribe for an existing destination logs an error and retains the original handler."
    ]
  },
  {
    id: "RT-LIFE-003",
    name: "RECONNECT_AND_RESTORE",
    behavior: "After a previously successful connection fails, the wrapper waits 6000 ms, disconnects an open client, reconnects with the current transport, clears the subscription map, and recreates every captured default and named handler.",
    sourceLocations: [
      location(stompClientSource, 151),
      location(stompClientSource, 155),
      location(stompClientSource, 164),
      location(stompClientSource, 184)
    ],
    testLocations: [
      location(stompClientTest, 107),
      location(stompClientTest, 114),
      location(stompClientTest, 143),
      location(stompClientTest, 169)
    ],
    failureBoundaries: [
      "There is no replay cursor, sequence number, or forced REST snapshot after reconnect.",
      "Subscription changes during the delay or reconnect can be resurrected or lost because restoration uses a shallow pre-delay snapshot.",
      "disconnect does not clear wrapper subscriptions, isConnected, timerId, or client."
    ]
  },
  {
    id: "RT-LIFE-004",
    name: "DYNAMIC_HANDLER_OWNERSHIP",
    behavior: "Config history owns a named handler on the permanent configs destination. Task detail owns an entire dynamic destination until selected client terminal states arrive.",
    sourceLocations: [
      location("ambari-web/classic/app/views/main/dashboard/config_history_view.js", 58),
      location("ambari-web/classic/app/views/main/dashboard/config_history_view.js", 69),
      location("ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js", 165),
      location("ambari-web/classic/app/controllers/global/background_operations_controller.js", 142)
    ],
    testLocations: [
      location("ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js", 134),
      location("ambari-web/classic/test/controllers/main/dashboard/config_history_controller_test.js", 148),
      location("ambari-web/classic/test/controllers/global/background_operations_test.js", 932)
    ],
    failureBoundaries: [
      "Named handler removal assumes the destination exists.",
      "Task ownership is not released merely by leaving TASK_DETAILS.",
      "Client and server terminal-state definitions are inconsistent."
    ]
  }
];

export default { transports, subscriptions, lifecycle };
