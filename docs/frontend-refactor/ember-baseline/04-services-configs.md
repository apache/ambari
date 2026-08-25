<!---
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->

# Services and Configs Module

The entry point is `/main/services/:service_id/{summary|configs|audit}`. Service Metrics and Heatmaps are `OUT_OF_SCOPE`.

## Service Navigation and Global Actions

`/main/services/:service_id/audit` contains only an empty `main/service/info/audit` controller and route outlet; no corresponding template, view, or dedicated request was found in the legacy Ember tree. It is marked `PLACEHOLDER` and must not be used to require an Audit page in React.

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| SVC-NAV-001 | Lists installed services, health/state, restart required, and alerts on the left; selection enters Summary | Returns to the first service when the service does not exist or is not loaded | Global service load | `app/routes/main.js`, service menu/item templates |
| SVC-ALL-001 | Add Service opens a 7-step wizard | `SERVICE.ADD_DELETE_SERVICES` and `supports.enableAddDeleteServices`; disabled during an upgrade/wizard | Add Service request group | service controller, Add Service route |
| SVC-ALL-002 | Start All Services | `SERVICE.START_STOP`; filters already started, maintenance, and inoperable services, then performs a confirmed batch PUT | `common.services.update`, request progress | `app/controllers/main/service.js` |
| SVC-ALL-003 | Stop All Services | Same as above; displays an impact confirmation before stopping | `common.services.update` | service controller |
| SVC-ALL-004 | Restart All Required | Processes only stale/restart-required components and supports immediate or rolling/scheduled execution | `restart.allServices`, `common.batch.request_schedules` | service controller, restart views |
| SVC-ALL-005 | Downloads client configs for all services | Uses the `CLUSTER` resource scope; this entry is unexpectedly inside the outer `SERVICE.START_STOP` or `SERVICE.ADD_DELETE_SERVICES` gate of the all-services menu. The browser downloads the archive directly with `window.open`, with no in-app HTTP failure/retry; the legacy code calls `focus()` on null when the popup is blocked | Client config download URL | `app/templates/main/service/all_services_actions.hbs`, `app/controllers/main/service.js#downloadAllClientConfigs`, support client configs download mixin |

## Single-Service Actions

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| SVC-ACT-001 | Start/Stop/Restart Service | `SERVICE.START_STOP`; disabled according to desired/current state, maintenance, and upgrade/wizard state | `common.service.update`, `common.batch.request_schedules` | `app/controllers/main/service/item.js` |
| SVC-ACT-002 | Run Service Check | `SERVICE.RUN_SERVICE_CHECK`; displays task results for the asynchronous request | `service.item.smoke` or custom action | service item controller |
| SVC-ACT-003 | Enters or exits Service Maintenance Mode | `SERVICE.TOGGLE_MAINTENANCE`; affects the display of its components/alerts | `common.service.passive` | service item controller |
| SVC-ACT-004 | Restart Required Components | Filters stale configs, selects rolling-restart parameters, and tracks progress | `restart.hostComponents`, `restart.staleConfigs`, schedule | service item/config action mixins |
| SVC-ACT-005 | Delete Service | `SERVICE.ADD_DELETE_SERVICES` and feature flag; validates stopped, dependency, last-service, and other conditions, with confirmation | `common.delete.service` | service item controller, delete templates |
| SVC-ACT-006 | Refresh YARN Queues | YARN is installed and the command is available | `service.item.refreshQueueYarnRequest` | service item controller |
| SVC-ACT-007 | Starts or stops HDFS Rebalance | HDFS is installed and NameNode/data-node states are suitable; accepts a threshold and displays progress from the request | `service.item.rebalanceHdfsNodes` | service item controller |
| SVC-ACT-008 | Executes service-specific commands such as Knox LDAP start/stop and HBase replication start/stop | The specific service/stack command is available, `CONDITIONAL` | `service.item.startStopLdapKnox`, `service.item.updateHBaseReplication`, `service.item.stopHBaseReplication` | service item controller |
| SVC-ACT-009 | Executes any stack custom command | `SERVICE.RUN_CUSTOM_COMMAND`; metadata determines the command, scope, and parameters | `service.item.executeCustomCommand` | service item controller |
| SVC-ACT-010 | Enables HA/Federation, manages JournalNodes, configures HAWQ standby, or moves a master | `SERVICE.ENABLE_HA`/`SERVICE.MOVE` and feature/service/component conditions | Corresponding wizard request group | service item controller, main routes |
| SVC-ACT-011 | Downloads all client configs for the current service or a specified client component config | All downloads use `SERVICE` scope and a specified client uses `SERVICE_COMPONENT` scope; the entry requires `CLUSTER.VIEW_CONFIGS` and is also affected by the outer service-actions permission set; download and failure semantics are the same as SVC-ALL-005 | Client config download URL | `app/views/main/service/item.js`, `app/controllers/main/service/item.js#downloadClientConfigs`, `downloadAllClientConfigs` |

## Non-Metrics Summary

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| SVC-SUM-001 | Displays service state, master/slave/client components, host distribution, alerts, maintenance, and restart required | A service-specific template can replace the base layout | Global service/component/alert load | summary controller/templates |
| SVC-SUM-002 | Component/host links navigate to Host details or filter Hosts by component | Retains the return path and filters | Hosts load | main route, service templates |
| SVC-SUM-003 | Performs start/stop/restart, maintenance, and custom commands for an individual component | Permission and state conditions are the same as for Host Components | Common host component/custom command requests | summary controller/templates |
| SVC-SUM-004 | Starts/stops a Flume agent | FLUME service; located by host and handler | `service.flume.agent.command` | service summary controller |
| SVC-SUM-005 | Service-specific Quick Links navigate to external Web UIs; an Ambari View uses a separate route/iframe mechanism | Quick Link visibility and protocol/host/port come from stack metadata/configs; the two entry types are not interchangeable; legacy `_blank` links set `rel="noopener noreferrer"` | `configs.quicklinksconfig`, `hosts.for_quick_links`, current config load | service summary template, `app/views/common/quick_view_link_view.js` |

## Quick Links and Browser External Links

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| SVC-QL-001 | Loads link definitions from the merged quicklinks descriptor for the current stack service; the client ultimately retains only items with `visible=true` | When a child descriptor contains only a name, parent-link removal occurs during server-side stack merging. Although the client skips residual `remove=true` items while scanning config dependencies, the final visible set is not filtered directly by `remove`. When the service has no descriptor, all items are invisible, or the associated component does not exist, it displays a no-links/error state and does not construct an empty URL | `configs.quicklinksconfig` | `app/views/common/quick_view_link_view.js#loadQuickLinksConfigurations`, `getQuickLinksConfiguration`; server `state/quicklinks/Link.java#mergeWithParent` |
| SVC-QL-002 | Selects HTTP/HTTPS from descriptor protocol checks, current site properties, and `hdfs-site/dfs.http.policy` | `HTTPS_ONLY`/`HTTP_ONLY` can fix the protocol; normal mode matches `EXIST`, `NOT_EXIST`, or an exact value for each check and reverses the protocol when a check is not satisfied | Current configs by required sites | `quick_view_link_view.js#setProtocol`, `meetDesired`, `reverseType` |
| SVC-QL-003 | Loads internal-host to public-host mappings for descriptor-associated components and generates link groups for a single host, multiple hosts, or multiple nameservices/master groups | Displays no-host when there is no associated host and the descriptor has no override host; uses only actually installed components | `hosts.for_quick_links` | `quick_view_link_view.js#getQuickLinksHosts`, `getHosts`, `findHosts` |
| SVC-QL-004 | Resolves ports from config properties, regular expressions, and defaults, then substitutes `${config-type/property-name}` placeholders and an optional login username into the URL template | Falls back to the default port when a property/regex is missing; preserves the descriptor's existing fallback semantics when a placeholder cannot be found | Current config load | `quick_view_link_view.js#getHostLink`, `resolvePlaceholders`, `setPort` |
| SVC-QL-005 | Applies service-specific cases: Ranger prefers `admin-properties/policymgr_external_url`; MapReduce2 can reverse-resolve a public host from a configured host:port; Oozie lists only STARTED servers | The external URL is determined by the stack/config, and the legacy frontend does not probe whether the target Web UI is reachable | Same as above | `quick_view_link_view.js#getHostLink`, `processOozieHosts` |
| SVC-QL-006 | Marks HDFS NameNode, YARN ResourceManager, and HBase Master as Active/Standby and displays them by group | HDFS/YARN use operational models; HBase reads `metrics/hbase/master/IsActiveMaster` for safe Quick Link selection, but this is not included as metric display | `hosts.for_quick_links`; HBase request includes the active-master field | `quick_view_link_view.js#processHdfsHosts`, `processYarnHosts`, `processHbaseHosts` |
| SVC-QL-007 | Opens the final external URL in a `target="_blank"` window | The click bypasses `App.ajax` and navigates directly in the browser; both rendering branches in the legacy templates set `rel="noopener noreferrer"` | Browser navigation to a dynamic URL | `app/templates/main/service/info/summary.hbs`, `generated/browser-network-entrypoints.md` |

## Service Configs

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| SVC-CONFIG-001 | Displays service configuration by stack theme, tab, section, and category, supporting text, password, checkbox, select, radio, slider, directory, database, and other controls | Falls back to the traditional category layout when the theme is missing | `configs.theme`, `configs.theme.services`, stack config load | configs controller, themes mapper, common config views |
| SVC-CONFIG-002 | Displays current, recommended, and default values, required/read-only state, errors/warnings, units, and descriptions | Property metadata, dependencies, and value attributes determine the controls and validation | Stack config/recommendation requests | config models/views |
| SVC-CONFIG-003 | Edits configurations with frontend validation, dependent-configuration linkage, and stack advisor recommendations; ordinary properties and theme Widgets support value/list selection, Widget/text editing, setting recommended values, undoing saved values, and changing the final flag | `SERVICE.MODIFY_CONFIGS`; saving is blocked when errors exist. `common/configs/widgets` contains configuration controls, not excluded Metrics Widgets; recommended values can also synchronize the final state recommended by the advisor | `config.recommendations` | configs controller, enhanced configs mixins, `app/views/common/configs/widgets/config_widget_view.js`, config controls templates |
| SVC-CONFIG-004 | Saves a new config version, accepts a note, and displays changed properties and dependent services | Both success and failure for the default group refresh the cluster/configs/quicklinks and clear recommendations. Non-default groups save independently and in parallel per service, with a success callback bound only to the current service, so the operation is not atomic: a dependent-service failure can occur after the success notification, and a current-service failure can leave `saveInProgress` set | Config save/cluster PUT requests | `app/mixins/common/configs/configs_saver.js#saveConfigsForNonDefaultGroup`, `doPUTClusterConfigurationSiteErrorCallback`, `onDoPUTClusterConfigurations` |
| SVC-CONFIG-005 | When leaving a route with unsaved changes, displays Save/Discard/Cancel | Saving is unavailable while another user is running a wizard | Save request or no request | `app/routes/main.js`, configs controller |
| SVC-CONFIG-006 | Browses config-version history, selects an older version, compares versions, and displays added/removed/modified items | `SERVICE.COMPARE_CONFIGS` controls comparison; older versions are read-only by default | Service config-version requests | config versions views/models |
| SVC-CONFIG-007 | Sets a historical version as current or reverts to it | `SERVICE.MODIFY_CONFIGS`; creates a new desired config version rather than modifying history | Config save request | config version controls |
| SVC-CONFIG-008 | Host override: creates or removes an override for a config group, restores the saved/default value, and independently sets a recommended value and final flag for the override | Non-default group; the property must support overrides; both Widgets and traditional property rows provide create/remove override controls | Config group/config save requests | config_overridable mixin, overridden property view, `app/templates/common/configs/widgets/controls/create_override.hbs`, `remove_override.hbs` |
| SVC-CONFIG-009 | Displays and operates restart required: restarts by service/host/component and supports rolling restart | Filters by permission, maintenance, and state | `restart.hostComponents`, `restart.staleConfigs`, schedule | component_actions_by_configs mixin |
| SVC-CONFIG-010 | Tests a database connection by creating a custom action, reading the task ID from the request, and polling the task result | Specific configs such as Hive/Oozie/Ranger/Kerberos; a create failure ends Connecting, while task-list or polling GET has no dedicated error callback and may remain in Connecting indefinitely. Failure displays stderr/stdout and the structured check message; success displays only Connection OK | `cluster.custom_action.create`/`custom_action.create`, `custom_action.request` | `app/views/common/configs/widgets/test_db_connection_widget_view.js`, database util |

The metadata compiler behind `SVC-CONFIG-001` through `SVC-CONFIG-003`, Host
Configs, comparison, and DB-test Widgets is specified independently in
[Service Theme and Configuration Layout](14-service-theme-layout.md). That
module is authoritative for custom service layouts, all 14 operational Widget
types, conditions, fallback, and React test obligations.
| SVC-CONFIG-011 | Adds a custom property in an allowed Advanced category, supporting a single key/value and multi-line `key=value` bulk mode; user properties can be deleted | `SERVICE.MODIFY_CONFIGS`; validates keys, duplicates, and bulk-line format; deleting a saved property adds its config type to the pending update set | Submitted with config-version save | `app/templates/common/configs/service_config_category.hbs`, `app/views/common/configs/service_configs_by_category_view.js#showAddPropertyWindow`, `removeProperty` |

## Config Groups

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| CFG-GROUP-001 | Lists the service's default and non-default config groups, host counts, and property overrides | Management is controlled by `SERVICE.MANAGE_CONFIG_GROUPS` | `service.load_config_groups`, `config_groups.all_fields` | manage config groups controller/template |
| CFG-GROUP-002 | Creates a group, enters its name/description, and selects hosts | A host can belong to only one non-default group for a service | `config_groups.create` | manage config groups controller |
| CFG-GROUP-003 | Renames, edits the description of, or copies a group | Validates name uniqueness | `config_groups.update`/`config_groups.update_config_group` | manage config groups controller |
| CFG-GROUP-004 | Adds or removes hosts; moving a host adjusts it in the original group | The host config route and current group are updated after saving | Config group update | manage config groups controller |
| CFG-GROUP-005 | Deletes a non-default group | The default group cannot be deleted; hosts return to default | `common.delete.config_group` | manage config groups controller |
| CFG-GROUP-006 | Views group properties and enters Configs editing for the group | The route preselects the group/version | Config load | manage config groups controller |

## Add Service Wizard

| ID | Step | Behavior and boundaries | Primary requests |
| --- | --- | --- | --- |
| SVC-ADD-001 | Choose Services | Lists only uninstalled and installable services; automatically selects dependencies and handles conflicts and service validation | Stack services/components metadata |
| SVC-ADD-002 | Assign Masters | Selects hosts for new masters and validates cardinality, resources, and existing components | Hosts/stack metadata |
| SVC-ADD-003 | Assign Slaves and Clients | Selects target hosts and retains installed components | Hosts/component metadata |
| SVC-ADD-004 | Customize Services | Loads configs, recommendations, and credentials/database/account tabs | Configs/recommendations |
| SVC-ADD-005 | Review | Summarizes and confirms service, component, and configuration changes | No mutation |
| SVC-ADD-006 | Install, Start and Test | Creates services/components/host-components, saves configs, installs, starts, and runs service checks. The Deploy route's `unroutePath()` unconditionally returns `false`; Add Service does not inherit the new Installer's leave exceptions for Admin View/Views | Common service/component/config/request APIs |
| SVC-ADD-007 | Summary | Displays completion/warnings/failures and refreshes the existing cluster's service menu. Complete closes the wizard and does not write cluster provisioning state `INSTALLED`; that write belongs only to the new Installer Complete flow | Service/cluster refresh; no provisioning mutation |
| SVC-ADD-008 | Recovery and mutual exclusion | Persists the current step; other users are marked non-wizard users and wait in read-only mode | Cluster status/persist |

## Reassign Master Wizard

| ID | Step/behavior | Description | Primary requests |
| --- | --- | --- | --- |
| SVC-MOVE-001 | Get Started / Assign Master | Selects the target master and new host, excluding the current host, ineligible hosts, and dependency conflicts | `hosts.high_availability.wizard`, config load |
| SVC-MOVE-002 | Review | Displays source/target, affected configs/services, and the downtime warning | Config/recommendation |
| SVC-MOVE-003 | Configure Component | Stops services, creates/installs the new component, updates configs, and deletes or stops the old component | Common service/component/config APIs |
| SVC-MOVE-004 | Manual Commands | Displays commands that must be run manually for components such as NameNode and MySQL and waits for confirmation, `CONDITIONAL` | Service-specific requests |
| SVC-MOVE-005 | Start and Test Services | Starts affected services, runs checks, and summarizes tasks | Request/task APIs |
| SVC-MOVE-006 | Rollback | On failure, restores configs, the old component, and service state by component type | Delete/update/config requests |

[generated/api-by-module/services-configs.md](generated/api-by-module/services-configs.md) is only a heuristic candidate inventory generated by broad matching of request names and caller paths. It may include cross-module requests or omit module-specific calls and is not complete at the module level. Authoritative verification must jointly inspect the global [AJAX definitions](generated/ajax-endpoints.md), [AJAX call sites](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md), [browser entry points](generated/browser-network-entrypoints.md), and [realtime channels](generated/realtime-channels.md).
