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

# Alerts Module

The entry point is `/main/alerts`, details are at `/main/alerts/:alert_definition_id`, and the creation wizard is `/main/alerts/add/step{1..3}`. Creation parameters and metric expressions for Metric-type Alert Definitions are `OUT_OF_SCOPE`; general alert operations remain in scope.

## Definition List and Quick Entry Points

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| ALERT-LIST-001 | Lists Alert Definitions, displaying service/component, name, state, enabled, latest status, last checked/changed, notification, and check count | The list is continuously refreshed by the alert summary/instances mapper | Direct HttpClient for alert definitions/summary | `app/controllers/main/alert_definitions_controller.js`, alerts templates/views |
| ALERT-LIST-002 | Filters and sorts by definition name, service, component, state, enabled, and other fields | Filter conditions are retained when the page updates | Definitions/summary load | alert definitions view/controller |
| ALERT-LIST-003 | Enters details by selecting a definition; service/host/global critical-warning popups enter the corresponding definition or all Alerts | Preserves list filter/loading state across route changes | `alerts.instances.unhealthy`, `alerts.instances.by_definition` | main route, alert notifications popup |
| ALERT-LIST-004 | Enables/disables a definition directly in the list | The list entry requires `CLUSTER.TOGGLE_ALERTS` and confirmation; local enabled state changes optimistically before the PUT with no error rollback, so failure relies on global error handling | `alerts.update_alert_definition` | `app/templates/main/alerts/alert_definition/alert_definition_state.hbs`, `app/controllers/main/alert_definitions_controller.js#toggleDefinitionState` |
| ALERT-LIST-005 | The Actions menu provides Create Alert, Manage Groups, Manage Notifications, and Manage Settings | The Create menu checks only `supports.createAlerts`, but the wizard route additionally requires `SERVICE.TOGGLE_ALERTS`; Notifications requires `CLUSTER.MANAGE_ALERT_NOTIFICATIONS`; Groups/Settings have no independent permission gate | No single request | `app/controllers/main/alerts/alert_definitions_actions_controller.js#content`, `app/routes/add_alert_definition_routes.js` |

## Definition Details and Instances

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| ALERT-DEF-001 | Displays definition label, description/type, service/component, scope/source, interval, threshold/config, groups, notification, enabled, and repeat tolerance | Does not expand metric configuration details for Metric types | Definition/instances load | definition details controller/template |
| ALERT-DEF-002 | Edits the label | The details editing entry requires `SERVICE.TOGGLE_ALERTS`; Save updates the model and exits editing before sending a PUT without a custom error callback, so failure does not restore editing state or the old value | `alerts.update_alert_definition` | definition details template, `app/controllers/main/alerts/definition_details_controller.js#saveEdit` |
| ALERT-DEF-003 | Edits general alert configs/thresholds | Validates warning/critical order, values, units, and other fields. Save first disables the fields and exits edit, then sends a PUT without a custom error callback; the label Save-on-leave and this operation are two independent concurrent requests with no result aggregation | `alerts.update_alert_definition` | `app/controllers/main/alerts/definition_configs_controller.js#saveConfigs`, `definition_details_controller.js#saveLabelAndConfigs` |
| ALERT-DEF-004 | Enables/disables a definition | The details entry requires `SERVICE.TOGGLE_ALERTS` and confirmation; as in the list, enabled changes optimistically and failure has no rollback | `alerts.update_alert_definition` | definition details template, `app/controllers/main/alerts/definition_details_controller.js#toggleDefinitionState` |
| ALERT-DEF-005 | Edits repeat tolerance/check count; accepts 1 through 99 or the hidden sentinel `DEBUG`, and can disable repeat tolerance | Enabled state and tolerance are submitted through two independent PUT requests. The popup closes immediately after submission with no aggregation/rollback, so partial success is possible | `alerts.update_alert_definition` | `app/controllers/main/alerts/definition_details_controller.js#editRepeatTolerance` |
| ALERT-DEF-006 | Deletes a custom definition | Model properties control whether default/stack definitions can be deleted; confirmation is required and the current page remains on failure | `alerts.delete_alert_definition` | definition details controller |
| ALERT-DEF-007 | Lists current instances, displaying service/host, state, last check, and response; can navigate to service or host alerts | Instance polling stops when the page exits | `alerts.instances.by_definition` | alert instances controller, details template |
| ALERT-DEF-008 | Opens instance response/log text | The modal displays the `text` carried by the instance, supports Copy and opening in a new window, makes no new backend log request, and provides no download | No new request; uses data from the instances response | details/instance views, `app/views/common/modal_popups/logs_popup.js` |
| ALERT-DEF-009 | Queries instance history from the last 24 hours and displays returned record counts by host | This is not a state-change timeline; the UI displays a history count for each host | `alerts.get_instances_history` | `app/controllers/main/alerts/definition_details_controller.js`, definition details template/model |
| ALERT-DEF-010 | Displays Save/Discard/Cancel when leaving a route during editing | Triggered only when `isEditing`; Save can start label and config PUT requests separately and allows the route flow to continue without waiting for aggregate completion | Update or no request | `app/routes/main.js`, `app/controllers/main/alerts/definition_details_controller.js#saveLabelAndConfigs` |

## Create Alert Definition

| ID | Step/behavior | Preconditions/boundaries | Backend requests |
| --- | --- | --- | --- |
| ALERT-CREATE-001 | Step 1 Choose Alert Type | Port, Web, Script, and Aggregate enter their corresponding configurations; Metric parameters are `OUT_OF_SCOPE`. Although the fixture lists Raw, the renderer has no `RAW` case and accesses a missing source on continuation; mark it `BROKEN/PLACEHOLDER` | None |
| ALERT-CREATE-002 | Step 2 Define Alert and Thresholds | Enters and validates name/label, service/component/scope, interval/timeout, and type-specific source, warning/critical/retry, and other fields in the shared configs view; Next generates the request JSON in this step | Stack/definitions load |
| ALERT-CREATE-003 | Step 3 Review | Read-only display of the selected type and formatted Alert Definition JSON; thresholds are no longer edited in this step and submission occurs only on Done | No mutation |
| ALERT-CREATE-004 | Done creates the definition and returns to Alerts | The menu requires `supports.createAlerts`, while the wizard route additionally requires `SERVICE.TOGGLE_ALERTS`; when the server fails, the create promise does not complete the subsequent close/finish flow | `alerts.create_alert_definition` |
| ALERT-CREATE-005 | Wizard forward/back navigation | Completed steps can be revisited; navigation forward is blocked when validation is not satisfied | None |

## Alert Groups

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| ALERT-GROUP-001 | Lists groups, descriptions, definitions, and notification targets by service | The Manage Groups action is visible | `alerts.load_alert_groups`, `alerts.notifications` | manage alert groups controller/template |
| ALERT-GROUP-002 | Creates a group | Name is required and must be unique within the service | `alert_groups.create` | manage alert groups controller |
| ALERT-GROUP-003 | Renames, edits the description of, or copies a group | Definitions in the default group cannot be modified as for an ordinary group | `alert_groups.update` | manage alert groups controller |
| ALERT-GROUP-004 | Adds/removes definitions from a group, with service/component filtering and multi-select | Manual additions/removals are prohibited for the default group; at least one definition must be selected | `alert_groups.update` | manage alert groups controller, add definition popup |
| ALERT-GROUP-005 | Associates or disassociates notification targets with a group | The notification must already exist | `alert_groups.update` | manage alert groups controller |
| ALERT-GROUP-006 | Deletes a non-default group | The default group cannot be deleted; confirmation is required | `alert_groups.delete` | manage alert groups controller |
| ALERT-GROUP-007 | Concurrently deletes on save, then concurrently updates and creates after all delete callbacks complete; closes and refreshes notifications when there are no errors | Partial failures still wait for the remaining requests and leave the aggregate error in the original popup; created/updated/deleted counts in the success popup come from planned operations, not server-confirmed successes | Create/update/delete requests | `app/controllers/main/alerts/alert_definitions_actions_controller.js#manageAlertGroups` |

## Alert Notifications

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| ALERT-NOTIFY-001 | Lists notification name, type, enabled, global/group scope, severity, and description | `CLUSTER.MANAGE_ALERT_NOTIFICATIONS` | `alerts.notifications` | manage alert notifications controller/template |
| ALERT-NOTIFY-002 | Creates an Email notification | Validates unique name, recipients, SMTP host/port, and from; auth, username/password confirmation, and STARTTLS are optional | `alerts.create_alert_notification` | create notification template/controller |
| ALERT-NOTIFY-003 | Creates an SNMP v1/v2c notification | Configures hosts, port, community, OIDs, severity, and groups, and validates FQDN/port | `alerts.create_alert_notification` | notification controller |
| ALERT-NOTIFY-004 | Creates a Custom SNMP notification | Allows custom properties in addition to SNMP settings | `alerts.create_alert_notification` | notification controller |
| ALERT-NOTIFY-005 | Creates an Alert Script notification | Configures the dispatch property and script filename, and validates the filename | `alerts.create_alert_notification` | notification controller |
| ALERT-NOTIFY-006 | Sets Global or selects groups, then selects severities such as Critical/Warning/OK/Unknown | Group selection is disabled for Global; supports Select All/Clear All | Create/update notification | notification view/controller |
| ALERT-NOTIFY-007 | Edits or copies a notification | Editing preserves sensitive-property semantics; copying requires a new name | `alerts.update_alert_notification` or create | notification controller |
| ALERT-NOTIFY-008 | Enables/disables a notification | Updates only the enabled state and refreshes on completion | `alerts.update_alert_notification` | notification controller |
| ALERT-NOTIFY-009 | Deletes a notification | Confirmation is required; the server handles or reports notifications referenced by a group | `alerts.delete_alert_notification` | notification controller |
| ALERT-NOTIFY-010 | Adds/removes a custom property | Names must be valid config keys and cannot conflict with built-in or existing properties | Submitted with create/update | notification controller/template |

## Global Alert Settings

| ID | Function and behavior | Preconditions/boundaries | Backend requests |
| --- | --- | --- | --- |
| ALERT-SET-001 | Edits alert check count/repeat tolerance by definition | Permissions and input ranges are the same as in the details view | `alerts.update_alert_definition` |
| ALERT-SET-002 | Manage Alert Settings changes `cluster-env.alerts_repeat_tolerance`, accepts 1 through 99 or `DEBUG`, and saves the configuration | The Actions menu always adds this item with no feature/stack/permission gate; it closes and reloads without waiting for `admin.save_configs`, so reload may interrupt the error callback. See `app/controllers/main/alerts/alert_definitions_actions_controller.js#manageSettings` for evidence | `admin.save_configs` |
| ALERT-SET-003 | Service/cluster maintenance and toggle alerts change alert presentation and notifications | Triggered by the corresponding Service/Host/Admin operation | Service/host/definition update |

[generated/api-by-module/alerts.md](generated/api-by-module/alerts.md) is only a heuristic candidate inventory generated by broad matching of request names and caller paths. It may include cross-module requests or omit module-specific calls and is not complete at the module level. Authoritative verification must jointly inspect the global [AJAX definitions](generated/ajax-endpoints.md), [AJAX call sites](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md), [browser entry points](generated/browser-network-entrypoints.md), and [realtime channels](generated/realtime-channels.md).
