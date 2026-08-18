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

# React Alerts Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/05-alerts.md` |
| React implementation | `ambari-web/latest`, Module 05 audit based on `4fb6dadf19` plus the Alerts remediation branch |
| Tracking issue | `AMBARI-26631` |
| Feature IDs | 40 non-Metrics IDs from `ALERT-LIST-001` through `ALERT-SET-003` |
| Review date | 2026-08-17 |
| Metrics boundary | Metric Alert Definition parameters, metric expressions, metric data, and Metrics APIs are excluded |

The audit compared the written baseline with executable Classic source and the current React implementation. It also reverse-scanned the generated AJAX definitions and call sites, direct HTTP calls, browser entry points, realtime channels, routes, permissions, and feature flags. The heuristic generated module inventory was used only as a candidate list.

Static inspection does not establish complete parity. Every feature that becomes `STATICALLY_ALIGNED` remains represented in the live runtime acceptance matrix until it has been exercised against Ambari Server.

## Initial Static Conclusion

| Status | Count |
| --- | ---: |
| `PARTIAL` | 21 |
| `INCORRECT` | 12 |
| `MISSING` | 6 |
| `BROKEN_LEGACY` | 1 |
| Total | 40 |

`BROKEN_LEGACY` records unreachable or broken Classic behavior that React must not reproduce merely to make a checklist look complete. Script and Aggregate creation may intentionally repair broken Classic paths and then be marked `IMPROVED_BEYOND_CLASSIC`.

## Post-Implementation Static Conclusion

| Status | Count |
| --- | ---: |
| `STATICALLY_ALIGNED` | 37 |
| `IMPROVED_BEYOND_CLASSIC` | 1 |
| `BROKEN_LEGACY` | 1 |
| `LIVE_REQUIRED` | 1 |
| Total | 40 |

These statuses describe source and focused-test evidence only. They do not change any row in the live runtime acceptance matrix.

## Baseline Corrections

The five-pass audit found three places where the written baseline overstated executable Classic behavior:

1. `ALERT-DEF-006`: `deleteAlertDefinition` and `alerts.delete_alert_definition` exist, but no inspected Classic template, view, route, action, or menu invokes the controller method. It is unreachable dead code, not a deletion workflow.
2. `ALERT-CREATE-001/002`: Port and Web have functional-looking configuration paths. RAW has no renderer case. Script dereferences `content.parameters`, which the wizard does not populate. Aggregate omits the common definition fields and continuation dereferences a missing definition name.
3. `ALERT-GROUP-001/003`: the inspected Classic Alert Group UI and request contract contain name, definitions, and notification targets, but no description field.

The Ember baseline was corrected to preserve these boundaries. React must not invent definition deletion or group descriptions for parity. A working Script or Aggregate creator is a deliberate repair beyond Classic.

## Post-Implementation Feature Status

### Definition List and Entry Points

| ID | Static status | React implementation evidence | Remaining live evidence |
| --- | --- | --- | --- |
| `ALERT-LIST-001` | `STATICALLY_ALIGNED` | Group definitions and grouped summaries merge by definition ID; list and definition loads have separate recoverable errors; 30-second polling retains prior rows on failure. | Confirm Server field completeness and polling convergence. |
| `ALERT-LIST-002` | `STATICALLY_ALIGNED` | Filtering and full-result sorting run after every data refresh; group membership handles multiple groups; numeric second/millisecond time filters work; filter/sort state persists per cluster in `sessionStorage`. | Confirm page, filter, and sort retention in a browser through polling and navigation. |
| `ALERT-LIST-003` | `STATICALLY_ALIGNED` | Definition, host, and service links use backend identifiers; persisted list state restores after returning from details. | Exercise every Classic popup and return path. |
| `ALERT-LIST-004` | `STATICALLY_ALIGNED` | List toggles use `CLUSTER.TOGGLE_ALERTS`, await the PUT, show failure in the confirmation modal, and refresh only after success. | Run the cluster/service permission-role matrix. |
| `ALERT-LIST-005` | `STATICALLY_ALIGNED` | Groups and Settings remain visible; Notifications uses `CLUSTER.MANAGE_ALERT_NOTIFICATIONS`; Create uses `supports.createAlerts`; direct creation is separately route-protected. | Confirm feature-flag, permission, and upgrade-state combinations. |

### Definition Details and Instances

| ID | Static status | React implementation evidence | Remaining live evidence |
| --- | --- | --- | --- |
| `ALERT-DEF-001` | `STATICALLY_ALIGNED` | Details load the exact definition resource by ID and independently merge groups and summaries. | Open a definition with only default or empty non-default membership. |
| `ALERT-DEF-002` | `STATICALLY_ALIGNED` | Label validation precedes one awaited PUT; failed values remain in edit mode for Retry. | Inject a Server rejection and retry in-browser. |
| `ALERT-DEF-003` | `STATICALLY_ALIGNED` | Update construction preserves raw source data, description, interval, thresholds, and parameters; invalid numeric/threshold values block Save. | Validate every installed non-Metric source shape against Server. |
| `ALERT-DEF-004` | `STATICALLY_ALIGNED` | Detail toggles use `SERVICE.TOGGLE_ALERTS`, update UI only after success, and retain an error in the modal on failure. | Confirm Server-converged state and upgrade boundaries. |
| `ALERT-DEF-005` | `STATICALLY_ALIGNED` | Per-definition repeat tolerance accepts 1-99 and `DEBUG`, uses one definition PUT, and retains the dialog on failure. | Round-trip all boundary values against Server. |
| `ALERT-DEF-006` | `BROKEN_LEGACY` | React has no deletion UI, which matches the reachable Classic surface. Classic's controller method and AJAX definition are dead code. | Do not add deletion UI solely for parity; revisit only with a separately specified product requirement. |
| `ALERT-DEF-007` | `STATICALLY_ALIGNED` | Definition-specific polling stops on unmount, keeps stale data on refresh failure, recovers from an initial failure, and separates core-instance and history errors. | Verify entry/exit timers and transient failures in a real browser. |
| `ALERT-DEF-008` | `STATICALLY_ALIGNED` | Copy and new-window rendering use literal text nodes and never `document.write`. | Probe markup and script-like Server text under the deployed CSP. |
| `ALERT-DEF-009` | `STATICALLY_ALIGNED` | The exact 24-hour history resource is queried and records are counted per host before sorting and pagination. | Verify timestamp boundaries and multi-host counts. |
| `ALERT-DEF-010` | `STATICALLY_ALIGNED` | `useBlocker` and `beforeunload` protect dirty label/config editors; Save awaits both, Discard restores both, and a partial failure leaves the failed editor dirty. | Exercise browser Back, refresh, close, and all combined-editor outcomes. |

### Create Alert Definition

| ID | Static status | React implementation evidence | Remaining live evidence |
| --- | --- | --- | --- |
| `ALERT-CREATE-001` | `STATICALLY_ALIGNED` | The menu and protected three-step route offer Port, Web, Script, and Aggregate only; Metric and RAW are absent. | Verify menu flag and direct-route permission independently. |
| `ALERT-CREATE-002` | `IMPROVED_BEYOND_CLASSIC` | Common and type-specific validation produce Server-compatible source payloads; Port/Web match Classic while Script/Aggregate deliberately repair broken Classic paths. | Create and execute every type against Agent/Server. |
| `ALERT-CREATE-003` | `STATICALLY_ALIGNED` | Step 3 renders the exact immutable request payload and Back returns to the populated editor. | Confirm browser layout and payload review for every type. |
| `ALERT-CREATE-004` | `STATICALLY_ALIGNED` | Submission uses a synchronous in-flight guard, POSTs the exact collection once, retains the wizard on failure, and leaves only after success. | Fault-inject the POST and double-click in a real browser. |
| `ALERT-CREATE-005` | `STATICALLY_ALIGNED` | `/main/alerts/add/:stepNumber` supports Back and validation-gated forward entry; direct Step 2/3 entry is redirected to the earliest required step. | Exercise reload and manual URL entry at every step. |

### Alert Groups

| ID | Static status | React implementation evidence | Remaining live evidence |
| --- | --- | --- | --- |
| `ALERT-GROUP-001` | `STATICALLY_ALIGNED` | Groups and notifications load independently with visible Retry states. | Verify large-group rendering and Server failures. |
| `ALERT-GROUP-002` | `STATICALLY_ALIGNED` | Classic-compatible trimmed name validation and case-insensitive uniqueness checks run before local creation; failed creates remain pending. | Confirm concurrent-name rejection handling. |
| `ALERT-GROUP-003` | `STATICALLY_ALIGNED` | Rename and copy use the complete local group model and preserve definition/target IDs. | Round-trip renamed and copied groups. |
| `ALERT-GROUP-004` | `STATICALLY_ALIGNED` | Definition changes remain local, default-group restrictions remain enforced, and Save sends numeric replacement arrays. | Exercise service/component filters and default groups. |
| `ALERT-GROUP-005` | `STATICALLY_ALIGNED` | Target assignments remain local and are sent in `AlertGroup.targets`; unsupported pseudo membership resources were removed. | Reload assignments from Server. |
| `ALERT-GROUP-006` | `STATICALLY_ALIGNED` | Default deletion is blocked; failed deletes remain pending in the open editor. | Inject referenced/concurrent delete failures. |
| `ALERT-GROUP-007` | `STATICALLY_ALIGNED` | All deletes settle before concurrent updates/creates; every result is aggregated, and refreshed Server state reconciles ambiguous timeout outcomes by ID and complete membership payload. | Fault-inject partial success, timeout-after-commit, and refresh failure. |

### Alert Notifications

| ID | Static status | React implementation evidence | Remaining live evidence |
| --- | --- | --- | --- |
| `ALERT-NOTIFY-001` | `STATICALLY_ALIGNED` | The permission-gated modal renders target scope, severity, enabled state, API type, and description with load Retry. | Validate real target variants and roles. |
| `ALERT-NOTIFY-002` | `STATICALLY_ALIGNED` | Email validation and payload construction cover recipients, SMTP host/port/from, optional authentication, TLS, and credentials. | Validate dispatcher configuration against Server. |
| `ALERT-NOTIFY-003` | `STATICALLY_ALIGNED` | Built-in SNMP maps to `AMBARI_SNMP` and uses `ambari.dispatch.recipients`. | Round-trip SNMP v1/v2c. |
| `ALERT-NOTIFY-004` | `STATICALLY_ALIGNED` | Custom SNMP maps to `SNMP` and writes trap/subject/body OID properties. | Round-trip a Custom SNMP target. |
| `ALERT-NOTIFY-005` | `STATICALLY_ALIGNED` | Alert Script maps to `ALERT_SCRIPT` and the two Classic dispatch property names. | Execute a deployed notification script. |
| `ALERT-NOTIFY-006` | `STATICALLY_ALIGNED` | Global disables group assignment; custom scope and severity arrays are submitted exactly. | Verify group association reload. |
| `ALERT-NOTIFY-007` | `STATICALLY_ALIGNED` | Edit preserves an unchanged returned sensitive password or replaces it explicitly; copies require a unique name and do not silently reuse a hidden credential. | Exercise masked-property semantics against Server. |
| `ALERT-NOTIFY-008` | `STATICALLY_ALIGNED` | Enable/disable sends only `AlertTarget/is_enabled`, mutates selection only after HTTP success, and reloads targets. | Inject toggle and reload failures. |
| `ALERT-NOTIFY-009` | `STATICALLY_ALIGNED` | Delete failure remains visible in the confirmation modal with the target selected; success closes and refreshes. | Exercise a target referenced by groups. |
| `ALERT-NOTIFY-010` | `STATICALLY_ALIGNED` | Custom keys validate syntax, uniqueness, and conflicts against exact type-specific built-ins. | Validate uncommon dispatcher properties. |

### Global Alert Settings

| ID | Static status | React implementation evidence | Remaining live evidence |
| --- | --- | --- | --- |
| `ALERT-SET-001` | `STATICALLY_ALIGNED` | Detail repeat tolerance shares validated 1-99/`DEBUG` semantics and retains errors for Retry. | Round-trip values against Server. |
| `ALERT-SET-002` | `STATICALLY_ALIGNED` | Global settings preserve `DEBUG`, await the `cluster-env` save/reload, and keep the dialog open on failure. | Verify a new desired-config version and reload ordering. |
| `ALERT-SET-003` | `LIVE_REQUIRED` | React displays maintenance counts and states; mutations remain owned by Host, Service, and Admin modules. Static Alerts code cannot establish notification suppression convergence. | Run the cross-module maintenance matrix. |

## Reverse API Contract

| Flow | Method and resource | Required query or payload |
| --- | --- | --- |
| List definitions | `GET /api/v1/clusters/{cluster}/alert_definitions` | Definition identity, display, enabled, source, interval, tolerance, groups, and non-Metric configuration fields needed by list/details. |
| Definition by ID | `GET /api/v1/clusters/{cluster}/alert_definitions/{id}` | `fields=*`; details must not depend on membership in a loaded group. |
| Grouped summary | `GET /api/v1/clusters/{cluster}/alerts?format=groupedSummary` | Merge by definition ID and retain status counts and timestamps. |
| Definition instances | `GET /api/v1/clusters/{cluster}/alerts?fields=*&Alert/definition_id={id}` | Poll only while details are mounted. |
| 24-hour history | `GET /api/v1/clusters/{cluster}/alert_history?(AlertHistory/definition_name={name})&(AlertHistory/timestamp>={now-86400000})` | Count returned `AlertHistory.host_name` records per host. |
| Update definition | `PUT /api/v1/clusters/{cluster}/alert_definitions/{id}` | Slash-keyed properties such as `AlertDefinition/label`, `AlertDefinition/enabled`, and validated config/source fields. |
| Create definition | `POST /api/v1/clusters/{cluster}/alert_definitions/` | Exact reviewed payload containing common definition fields and the selected non-Metric source. |
| Load groups | `GET /api/v1/clusters/{cluster}/alert_groups?fields=*` | Preserve definition and target references. |
| Create group | `POST /api/v1/clusters/{cluster}/alert_groups` | `{ "AlertGroup": { "name": string, "definitions": number[], "targets": number[] } }`. Empty arrays remain explicit replacement values. |
| Update group | `PUT /api/v1/clusters/{cluster}/alert_groups/{id}` | Same complete `AlertGroup` replacement payload as create. |
| Delete group | `DELETE /api/v1/clusters/{cluster}/alert_groups/{id}` | Only non-default groups; delete phase completes before update/create phase begins. |
| Load targets | `GET /api/v1/alert_targets?fields=*` | Preserve server type, scope, groups, states, enabled, and property values including masked sensitive values. |
| Create target | `POST /api/v1/alert_targets` | `{ "AlertTarget": ... }` with API type and exact type-specific properties. |
| Update target | `PUT /api/v1/alert_targets/{id}` | Complete edited target or the intentional enabled-only partial update. |
| Delete target | `DELETE /api/v1/alert_targets/{id}` | Retain UI state when the server rejects a referenced target. |
| Global repeat tolerance | `PUT /api/v1/clusters/{cluster}` through the existing config saver | New `cluster-env` version preserving `alerts_repeat_tolerance` as `DEBUG` or a decimal string. |

The React pseudo-resources `/alert_groups/{group}/alert_definitions/{definition}` and `/alert_groups/{group}/alert_targets/{target}` do not appear in the inspected Classic contract. Group membership and target association are replacement arrays on Alert Group create/update.

## Notification Property Contract

| UI type | API `notification_type` | Required built-in properties |
| --- | --- | --- |
| Email | `EMAIL` | `ambari.dispatch.recipients`, `mail.smtp.host`, `mail.smtp.port`, `mail.smtp.from`, `mail.smtp.auth`; when authentication is enabled, credential username/password and `mail.smtp.starttls.enable` |
| SNMP | `AMBARI_SNMP` | `ambari.dispatch.snmp.version`, `ambari.dispatch.snmp.community`, `ambari.dispatch.recipients`, `ambari.dispatch.snmp.port` |
| Custom SNMP | `SNMP` | SNMP version/community/recipients/port plus `ambari.dispatch.snmp.oids.trap`, `.oids.subject`, and `.oids.body` |
| Alert Script | `ALERT_SCRIPT` | Optional `ambari.dispatch-property.script` and `ambari.dispatch-property.script.filename` |

Custom properties cannot duplicate any built-in key for the selected type. An unchanged masked Email password must retain the server's sensitive-property semantics instead of being replaced with an empty string.

## Permissions, Feature Flags, and Routes

| Entry or operation | Required Classic gate | Current React state |
| --- | --- | --- |
| Alerts list | Normal authenticated cluster route | Present. |
| List enable/disable | `CLUSTER.TOGGLE_ALERTS` | Aligned. |
| Detail edit and enable/disable | `SERVICE.TOGGLE_ALERTS` | Aligned. |
| Create menu entry | `supports.createAlerts` | Aligned. |
| Direct create wizard route | `SERVICE.TOGGLE_ALERTS` | Aligned independently of menu visibility. |
| Manage Groups | No independent permission gate | Aligned; the action remains visible. |
| Manage Notifications | `CLUSTER.MANAGE_ALERT_NOTIFICATIONS` | Aligned. |
| Manage Settings | No independent permission gate | Aligned; the action remains visible. |

The target create route is `/main/alerts/add/:stepNumber`. It must use `ProtectedRoute requireAuthorization="SERVICE.TOGGLE_ALERTS"`; menu visibility additionally checks `supports.createAlerts`, matching the distinction in Classic.

## Pre-Implementation Five-Pass Audit Record

| Pass | Evidence inspected | Main conclusions |
| --- | --- | --- |
| 1. User-visible surface | Classic routes, templates, views, menus; React routes and Alerts components | Create is absent; deletion is dead code; group description is not a Classic feature; Actions visibility is too restrictive. |
| 2. State and controller logic | Classic definition, group, notification, config, and wizard controllers; React hooks and modal state | Dirty navigation is absent; group associations mutate too early; failures commonly discard or hide state. |
| 3. API contract | Generated endpoints/call sites and concrete request builders in both implementations | History is missing; group targets are stripped; notification type/property mappings are wrong; pseudo membership resources are unsupported by Classic evidence. |
| 4. Permissions and flags | Generated permissions/flags, Classic authorization helpers/routes, React guards/hooks/context | List toggle uses the wrong permission; Create needs separate flag and route authorization; Groups/Settings must remain visible. |
| 5. Failure, concurrency, and recovery | Classic callbacks/save sequencing and React async paths | Group phase ordering/all-settled behavior differs; list/details/modals lack consistent Retry; sensitive Email edit and response text opening are unsafe. |

## Live Runtime Acceptance Matrix

| Scenario | Required live evidence |
| --- | --- |
| Definition list refresh | Status counts, timestamps, current filters, sorting, and selected page remain coherent through polling and STOMP or server-side changes. |
| List permission boundary | A user with only cluster toggle can toggle from the list; a service-toggle-only user cannot; denied direct calls do not leave stale optimistic state. |
| Action visibility | Users without alert mutation permissions still see Groups and Settings; Notifications and Create follow their independent gates. |
| Direct definition load | A valid definition with no non-default group membership opens directly by ID and renders complete non-Metric information. |
| Dirty route leave | Label-only, config-only, and combined edits exercise Save, Discard, Cancel, browser Back, in-app Back, and refresh/close protection. |
| Definition save failure | Validation prevents bad input; transport/server failures keep values and allow a successful Retry. |
| Instance polling | Polling starts on entry, stops on exit, survives one failed request, and correctly links host/service rows. |
| Response text safety | Response strings containing markup and script-like text are copied/opened literally and never interpreted. |
| 24-hour history | Multiple records on multiple hosts produce exact per-host counts at the timestamp boundary. |
| Create each type | Port and Web create exact definitions; repaired Script and Aggregate paths are identified as beyond-Classic behavior; Metric and RAW are absent. |
| Create authorization and flag | Menu flag and direct-route permission combinations are tested independently, including URL entry. |
| Group successful save | Concurrent deletes finish before concurrent updates/creates; definitions and targets persist after reload. |
| Group partial failure | Every started request settles, aggregate failures are shown, and unsaved state remains available for reconciliation and Retry. |
| Notification types | Email, built-in SNMP, Custom SNMP, and Alert Script round-trip with exact API types and properties. |
| Sensitive Email edit | Saving an authenticated Email without entering a new password does not erase or replace the stored credential. |
| Notification failure | Create, edit, enable/disable, and delete failures retain the selected target and permit Retry. |
| Repeat tolerance | Values 1, 99, and `DEBUG` round-trip; invalid values are rejected; save failure leaves the dialog open. |
| Maintenance interaction | Host/service maintenance and alert toggles update presentation and notification suppression after owner-module operations converge. |

## Focused Executable Evidence

Module 05 now includes deterministic tests for:

- exact Alert API URLs, methods, query values, and payloads;
- create-form validation and request generation for every supported non-Metric type;
- menu feature/permission policy and direct route protection;
- history count mapping by host;
- definition config validation, description preservation, and dirty-state decisions;
- safe response opening without HTML interpretation;
- complete group payloads and delete-before-update/create all-settled orchestration;
- notification UI/API type conversion, property construction, built-in conflicts, and sensitive Email edits;
- `DEBUG` repeat-tolerance preservation; and
- rejected load/toggle/save operations retaining state for Retry.

The focused evidence is in `src/api/alertsApi.test.ts`, `src/Utils/alertCreation.test.ts`, `src/Utils/alertDefinitions.test.ts`, `src/Utils/alertGroups.test.ts`, `src/Utils/alertNotifications.test.ts`, `src/Utils/alertPolicy.test.ts`, `src/screens/Alerts/alertUtils.test.ts`, and `src/screens/Alerts/AlertInstancesTable.test.tsx`.

Runtime-required rows must remain in this document even after focused tests pass. They are removed only after the corresponding behavior has been accepted against a live Ambari cluster.
