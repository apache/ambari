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

# React Background Operations and Non-Metrics Dashboard Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/02-background-dashboard.md` |
| React implementation | `ambari-web/latest`, module 02 work based on `2c1f3e6ce621b92d89dd7e24a6ac7ca1cee4f144` |
| Feature IDs | `BG-001` through `BG-009`, `DASH-002` through `DASH-004` |
| Review date | 2026-08-13 |
| Metrics boundary | Metrics, Heatmaps, Horizon Charts, dashboard widgets, Metrics APIs, and `DASH-005` are excluded |

`DASH-001` records the existing default navigation to the Metrics route. This module does not change or evaluate the excluded Metrics page.

The comparison used the Ember feature baseline, the actual classic source, and all authoritative network inventories: AJAX definitions, AJAX call sites, direct HTTP calls, browser network entry points, and realtime channels. The broad `generated/api-by-module/background-common.md` candidate list was not treated as a complete contract.

## Current Conclusion

| Status | Count |
| --- | ---: |
| `NEEDS_RUNTIME_VALIDATION` | 10 |
| `PARTIAL` | 2 |
| Total | 12 |

The static React gaps in request snapshots, request/host/task details, logs, abort handling, preferences, realtime transport, Config History, and history-to-Service-Configs navigation are addressed. The module cannot be marked covered until it is exercised against a real Ambari Server. `BG-005` and `BG-006` remain partial because their complete acceptance boundary is distributed across Installer, Services, Upgrade, HSI, and other modules that have not yet been reconciled.

## Feature Status

| ID | Status | React implementation and automated evidence | Remaining boundary |
| --- | --- | --- | --- |
| `BG-001` | `NEEDS_RUNTIME_VALIDATION` | The application owns a de-duplicated, descending REST snapshot; request events upsert without deleting unrelated rows; the list shows context, status, progress, user, start time, duration, filters, running count, and Show More; polling is serialized | Validate long histories, overlapping REST/event timing, all server statuses, progress values, and request timestamps |
| `BG-002` | `NEEDS_RUNTIME_VALIDATION` | Opening an ordinary request ID loads the classic minimal request/task shape, groups tasks by host, adds hosts/tasks introduced by socket events, filters and paginates hosts, and drills into tasks | Validate large multi-stage requests, missing host names, task ordering, minimal-response fields, and transitions while the popup is open |
| `BG-003` | `NEEDS_RUNTIME_VALIDATION` | Task detail reads the REST snapshot, subscribes to `/events/tasks/{taskId}` while non-terminal, falls back to serialized 5-second polling while disconnected, maps output/error paths, stops on every server terminal status, isolates malformed events, and safely opens loaded text with `textContent` | Validate live stdout/stderr growth, log paths, copy/open behavior, disconnect/reconnect during a task, and HDFS Rebalance presentation; specialized Rebalance progress remains outside the generic log view |
| `BG-004` | `NEEDS_RUNTIME_VALIDATION` | Existing Services, Hosts, bulk action, Kerberos, service-check, and operation-progress entry points reuse `BackgroundOperations` for ordinary `Requests.id` values; scheduled responses no longer pass `RequestSchedule.id` to `/requests/{id}` | Validate each 202 response shape and confirm synchronous responses do not open request progress; Installer, Upgrade, HA, and remaining module-specific entry points are audited with their owning modules |
| `BG-005` | `PARTIAL` | Generic failed request, failed host, task status, task log, error, and Retry behavior is available; `FAILED` and `SKIPPED_FAILED` are grouped consistently | Business-specific retry commands and their eligibility remain owned by Installer, Services, Upgrade, Kerberos, and HA flows and are not invented in this generic popup |
| `BG-006` | `PARTIAL` | Existing Hosts and Services actions create scheduled batches; React can read a source schedule, identify scheduled/running/disabled state, cancel future batches, query pending schedules, avoid treating a schedule ID as a request ID, and suppress the one-host Recommission association | HSI pending-schedule conflict protection and complete wizard/upgrade mutual exclusion must be integrated and validated in their owning modules |
| `BG-007` | `NEEDS_RUNTIME_VALIDATION` | Background snapshots, host details, and task logs schedule their next poll only after the current request settles; timers and dynamic subscriptions are cleaned up on dependency change/unmount; failures expose Retry without starting parallel loops | Validate slow and hung HTTP requests, route/logout teardown, modal reopen, abort failure, and disconnected operation over an extended session |
| `BG-008` | `NEEDS_RUNTIME_VALIDATION` | One application STOMP client subscribes to all ten static non-Metrics destinations plus dynamic task detail; it uses native WebSocket first, classic SockJS fallback transports on initial native socket/STOMP failure, 6-second reconnect after an established connection, 10-second heartbeats, a 200-message cap, and a request snapshot after connect/reconnect | Validate native and SockJS handshakes through proxies, broker `ERROR`, initial dual failure, established connection loss, subscription restoration, and the documented absence of event replay or a universal REST fallback |
| `BG-009` | `NEEDS_RUNTIME_VALIDATION` | Abort requires `SERVICE.START_STOP`, follows the classic status allowlist, confirms, prevents duplicate submissions, sends `ABORTED` with an abort reason, restores action state on failure, refreshes on success, and exposes an accessible button | Validate Cluster User, service operator, cluster administrator, and Ambari administrator roles; validate server rejection, already-terminal races, and long-running PUT requests |
| `DASH-002` | `NEEDS_RUNTIME_VALIDATION` | Config History uses server paging, filtered and overall totals, classic default sorting and secondary version sorting, exact/match/relative-time predicates, dynamic field suggestions, recoverable errors, and `/events/configs` refresh | Validate large histories, service display-name mapping, predicate parsing, saved browser state expectations, sort stability, event bursts, empty histories, and actual server totals |
| `DASH-003` | `NEEDS_RUNTIME_VALIDATION` | A history row carries service, config version, group ID/name into Service Configs; uninstalled services and deleted groups are not links; navigation state is consumed from browser history; the existing unsaved-change blocker remains active | Validate current and historical default versions, non-default groups, Back/Forward behavior, a service removed between pages, config fetch failures, and unsaved edits |
| `DASH-004` | `NEEDS_RUNTIME_VALIDATION` | Rows expose group/current, hosts, notes with More/Less, compatibility, author, stack data, and creation time in the persisted user timezone | Validate default-group host association, deleted groups, null notes, long notes, incompatible stack versions, invalid timezone fallback, and large host lists |

## Backend Contract Comparison

| Ember contract | React implementation | Static conclusion | Runtime gate |
| --- | --- | --- | --- |
| `GET /api/v1/clusters/{cluster}/requests?to=end&page_size=...&fields=Requests/...&minimal_response=true` | `ClusterApi.getRequests()` | Field list, newest-page shape, upgrade exclusion, snapshot replacement, and Show More are implemented | Real `itemTotal`, page boundary, slow response, and event race |
| `GET /api/v1/clusters/{cluster}/requests/{requestId}?fields=*,tasks/Tasks/...&minimal_response=true` | `ClusterApi.getRequestById()` | Classic minimal task field contract is retained for host/task drill-down | Large request and every task/host/status shape |
| `GET /api/v1/clusters/{cluster}/requests/{requestId}/tasks/{taskId}` | `ClusterApi.getClusterRequestTaskLogs()` | Initial log snapshot and disconnected polling are implemented | Live and terminal task payloads, logs, and failure bodies |
| `PUT /api/v1/clusters/{cluster}/requests/{requestId}` | `ClusterApi.updateRequest()` | Sends `Requests.request_status=ABORTED` and `Requests.abort_reason` under authorization and duplicate-submit guards | Server permission response, terminal race, and actual abort propagation |
| `POST /api/v1/clusters/{cluster}/request_schedules` | Existing `HostsApi.batchRequest()` and `ActionsApi.actionRequest()` | Existing batch payload creation remains in the owning action flows | Batch ordering, intervals, tolerance, immediate/scheduled modes, and mutual exclusion |
| `GET /api/v1/clusters/{cluster}/request_schedules/{scheduleId}` | `RequestScheduleApi.fetch()` | Source schedule status is loaded independently of request IDs | Scheduled, disabled, completed, missing, and forbidden responses |
| `GET /api/v1/clusters/{cluster}/request_schedules?fields=*&(RequestSchedule/status.in(SCHEDULED,IN_PROGRESS))` | `RequestScheduleApi.fetchPending()` | Pending-schedule contract is available for owning wizard/module guards | HSI and wizard integration remains partial |
| `DELETE /api/v1/clusters/{cluster}/request_schedules/{scheduleId}` | `RequestScheduleApi.cancel()` | Future-batch cancellation and refresh are implemented | Permission, already-completed, network failure, and resulting status |
| Native `/api/stomp/v1/websocket`; SockJS `/api/stomp/v1` | `createStompTransport()` and the application STOMP lifecycle | Native-first selection, classic fallback transport list, heartbeat, reconnect, resubscription, and cleanup are implemented | Proxy/browser combinations, broker `ERROR`, connection loss, and no-event-replay behavior |
| Static STOMP destinations `/events/hostcomponents`, `/events/alerts`, `/events/ui_topologies`, `/events/configs`, `/events/services`, `/events/hosts`, `/events/alert_definitions`, `/events/alert_group`, `/events/upgrade`, `/events/requests` | `AppProvider` subscriptions | All ten non-Metrics static destinations are present and malformed messages are isolated | Mapper-owned convergence for Hosts, Services, Alerts, Upgrade, and topology is validated in those modules |
| Dynamic STOMP `/events/tasks/{taskId}` | `TaskLogs` subscription | Opens only for non-terminal tasks, merges log fields, and unsubscribes on terminal status or unmount | Late subscription, reconnect, server registry cleanup, and high-volume log output |
| `GET /api/v1/clusters/{cluster}/configurations/service_config_versions?...fields=...&minimal_response=true` | `ConfigHistoryApi.fetchConfigHistory()` | Classic fields, paging, sorting, exact/match/time predicates, filtered total, and stale-response suppression are implemented | Server predicate parsing, total correctness, large data, and response mapping |
| `GET .../service_config_versions?page_size=1&minimal_response=true` | `ConfigHistoryApi.fetchTotal()` | Overall counter is independent of filtered total | Concurrent history mutation and failure fallback |
| `GET .../service_config_versions?fields={field}&minimal_response=true` | `ConfigHistoryApi.fetchSuggestions()` | Service, group, author, and notes suggestions are field-allowlisted and de-duplicated | Large suggestion sets, null fields, service display labels, and failures |
| Current and selected service-config-version loads after Config History navigation | `ConfigsApi.getConfigValues()`, `ConfigsApi.getVersionConfigValues()`, `ServiceConfigApi.getServiceConfig()` | Current default plus the selected default/non-default group version is loaded and selected; version list prioritizes the requested history version | Real current/non-current default and config-group combinations |

No browser download or external URL contract belongs to this module. Opening a loaded task log uses an in-memory `about:blank` document and does not perform a second backend request.

## Five Independent Audits

| Pass | Independent entry | Findings | Result |
| --- | --- | --- | --- |
| 1. Data and state semantics | REST snapshots, request events, host/task grouping, Config History transformation | The old React code merged a REST page as events, replaced event data as a snapshot, discarded unseen socket tasks/hosts, and confused request and schedule IDs | REST replacement and event upsert are separate; socket tasks/hosts are inserted; IDs retain their resource type |
| 2. Status and API reverse scan | Every classic AJAX definition/call site, direct HTTP entry, status map, and response field | Missing `TIMEDOUT`/`SKIPPED_FAILED` terminal handling, incomplete Pending/Failed/Success filters, unencoded match predicates, and absent schedule read/cancel contracts | Status sets, filter aliases, exact URLs, payloads, escaping, encoding, and schedule operations are covered by tests |
| 3. Async and network lifecycle | Poll overlap, teardown, WebSocket/SockJS selection, reconnect, heartbeats, task subscriptions, config events | Pollers could overlap, socket handlers captured stale state, native failure did not fully reproduce SockJS fallback, task subscriptions leaked/re-subscribed, and message history was unbounded | Polling is serialized; handlers use state upserts; initial socket/STOMP failure switches transport; subscriptions clean up; history is capped |
| 4. Entry, permission, and persistence | Navbar, automatic/manual popup entry, action response shapes, role derivation, Abort, `show_bg`, timezone, navigation state | Cluster User was inferred as every non-operator, `show_bg` was inverted, manual entry was blocked by preference, scheduled response IDs opened request endpoints, and history selection was lost | Exact `CLUSTER.USER` semantics, preference defaults, explicit entry override, Abort gate, correct response routing, timezone, and consumed navigation state are implemented |
| 5. Executable acceptance | Focused utilities, APIs, components, failures, retries, malformed messages, and navigation | Static reading alone could not prove failure recovery, selected-version state, or terminal task cleanup | Targeted TypeScript and Vitest coverage exercises these boundaries; full build and runtime gates remain separately recorded |

## Compatibility Decisions

| Classic behavior or defect | React decision |
| --- | --- |
| Task log Open writes loaded output through `document.write()` | Create a `pre` element and assign `textContent`, preventing loaded log text from becoming executable markup |
| Classic task subscription terminal logic omits `TIMEDOUT` and `SKIPPED_FAILED` and can retain subscriptions | Treat all server terminal states as terminal and unsubscribe on event, REST snapshot, dependency change, or unmount |
| Classic request event mapping assumes `Tasks` and an existing model hierarchy | Accept a missing task list, insert new requests/tasks/hosts, and isolate malformed messages |
| A `RequestSchedule.id` resembles a request ID numerically | Never call `/requests/{scheduleId}`; scheduled responses open the recent operation list until a real request is emitted |
| User-entered match values are interpolated as regex text | Escape regex metacharacters and URL-encode the value at the query boundary |
| Classic initial native WebSocket failure falls back to SockJS but has known cleanup gaps | Preserve native-first/SockJS fallback behavior while ensuring the failed native client is deactivated before rebuilding the transport |

## Automated Evidence

The targeted verification currently covers 8 Vitest files and 34 tests:

- Request snapshot replacement, event upsert, status aliases, terminal states, schedule association, popup preference, and Abort policy.
- Native/SockJS URL and transport selection plus initial-failure policy.
- Schedule read, pending query, and cancellation URLs.
- Cluster User role classification.
- Background Operations authorization, direct request entry, abort duplicate lock/failure recovery, and Cluster User restriction.
- Task log REST snapshot, dynamic subscription, malformed-message isolation, terminal unsubscribe, safe rendering, and cleanup.
- Config History sorting, predicates, encoding, transformation, timezone, deleted/uninstalled navigation restrictions, selected group/version state, error Retry, and config-event refresh.

Expected test-only output includes the intentional Config History failure path logged with `console.error` and a React Bootstrap/JSDOM `TimeoutNaNWarning`; neither is an application timer failure.

## Runtime Acceptance Matrix

At minimum, execute the following scenarios before changing any `NEEDS_RUNTIME_VALIDATION` item to `COVERED`:

1. Open recent operations with 0, 1, 20, 21, and hundreds of requests; use every status filter and Show More while new request events arrive.
2. Open ordinary requests from Navbar, Services, Hosts, bulk actions, service checks, Kerberos, Installer, Upgrade, and HA flows; verify only real `Requests.id` values load request progress.
3. Exercise requests with multiple stages, hosts, newly added socket tasks/hosts, no-host tasks, failures, timeouts, skipped failures, aborts, and completion.
4. Open running and completed task logs; copy and open text containing HTML/script characters; disconnect and reconnect while output changes.
5. Validate Abort as Cluster User, service operator, cluster administrator, and Ambari administrator; cover success, 403, 500, network loss, duplicate click, and already-terminal race.
6. Create immediate and scheduled start/stop/restart batches; inspect interval/tolerance; read and cancel scheduled/in-progress schedules; cover one-host Recommission and already-completed schedules.
7. Run with native WebSocket success, initial socket rejection, STOMP `ERROR`, SockJS success, both transports failing, established connection loss, 6-second reconnect, and proxy TLS termination.
8. Leave/re-enter the popup and application, switch task IDs, log out during polls, and verify Network/WS panels contain no orphan poll or dynamic subscription.
9. Load Config History with large data, all page sizes, every sort, every filter type, special regex/URL characters, empty data, server errors, Retry, and rapid `/events/configs` messages.
10. Display null/long notes, current/deleted/default/non-default groups, default and explicit host associations, compatible/incompatible stack versions, and valid/invalid persisted timezones.
11. Navigate from history to a current default, historical default, and non-default group version; use Back/Forward and unsaved edits; remove the service or group between page loads.
12. Validate the remaining `BG-005` business retry and `BG-006` wizard/upgrade/HSI exclusion boundaries when their owning modules are reconciled.

## Issue and PR Granularity

This module uses one primary JIRA and one primary pull request rather than splitting request operations, realtime transport, Config History, and navigation into small submissions. The English JIRA body is in `react-current/issues/02-background-dashboard.md`. The pull request targets `apache/ambari:frontend-refactor` from `JiaLiangC/ambari`.
