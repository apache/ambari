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

## Problem

The React frontend has partial implementations of Background Operations and Dashboard Config History, but their behavior does not yet provide a reliable equivalent to the classic Ember UI. Request snapshots and realtime updates use incompatible data shapes, task and host updates can be lost, operation polling can overlap, task subscriptions and timers are not consistently cleaned up, and native WebSocket failure does not reproduce the classic SockJS fallback boundary.

The existing popup also lacks complete permission, preference, abort, terminal-status, Request Schedule, and error-recovery behavior. Some scheduled action responses incorrectly treat `RequestSchedule.id` as a normal request ID. Config History loads an incomplete client-side view rather than the classic server-paged contract, loses the selected version when navigating to Service Configs, and does not expose all association, compatibility, timezone, sorting, filtering, refresh, or failure behavior.

This issue covers the complete non-Metrics module defined by the classic UI baseline. Metrics pages, charts, Heatmaps, Horizon Charts, widgets, and Metrics APIs are not part of the scope.

## Scope

* Reconcile the recent request list as a REST snapshot and merge request events as bounded upserts without losing unrelated operations.
* Preserve the classic request fields, upgrade exclusion, running count, status groups, progress, context, user, timestamps, duration, and Show More behavior.
* Load request hosts and tasks through the classic minimal-response contract and add tasks or hosts introduced by request events.
* Load task stdout, stderr, output/error log paths, and terminal status; use dynamic task events while connected and serialized REST polling while disconnected.
* Clean up all module polling, reconnect, and dynamic subscription work on status, dependency, route, modal, session, or application lifecycle changes.
* Use one application STOMP client with the ten non-Metrics static destinations and dynamic task destinations.
* Use native WebSocket first, fall back to the classic SockJS transport list after an initial native socket or STOMP failure, reconnect established transports after six seconds, use ten-second heartbeats, and bound retained messages.
* Apply exact Cluster User restrictions, automatic/manual popup preference semantics, `SERVICE.START_STOP` authorization, accessible Abort UI, duplicate submission locking, failure recovery, success feedback, and request refresh.
* Preserve existing request progress entry points for ordinary `Requests.id` response values and never use a `RequestSchedule.id` as a request ID.
* Read, display, and cancel source Request Schedules, expose the pending-schedule query, and retain one-host Recommission suppression.
* Replace Config History with the classic server paging, sorting, filtered/overall totals, exact/match/relative-time predicates, field-specific suggestions, and config-event refresh contract.
* Show service, version, group, current state, hosts, author, notes, stack compatibility, and creation time in the persisted user timezone.
* Navigate eligible history records to Service Configs with the selected service, group, and version; load the current default together with a selected non-default version; preserve the unsaved-change guard and consume transient history state.
* Add focused tests for data reconciliation, status policy, transport selection, schedule APIs, permission boundaries, task lifecycle, Config History queries, error recovery, and selected-version navigation.

## Classic UI Baseline

The acceptance baseline is `docs/frontend-refactor/ember-baseline/02-background-dashboard.md`, feature IDs `BG-001` through `BG-009` and `DASH-002` through `DASH-004`. `DASH-001` is an existing navigation fact, not a request to implement the excluded Metrics page. The React gap analysis is recorded in `docs/frontend-refactor/react-current/02-background-dashboard-gap.md`.

The authoritative network comparison includes the classic AJAX definitions and call sites, direct HTTP calls, browser network entry points, and realtime inventory. The required contracts include:

* Recent background request GET with its restricted `Requests` field list and `minimal_response=true`.
* Request details GET with request fields and minimal task fields.
* Individual task GET for task state, stdout, stderr, and log paths.
* Request PUT for `ABORTED` plus an abort reason.
* Request Schedule POST, individual GET, pending GET, and DELETE.
* Native STOMP WebSocket and SockJS endpoints.
* Ten static non-Metrics STOMP destinations and dynamic `/events/tasks/{taskId}`.
* Server-paged service config versions GET, unfiltered total GET, and field-specific suggestion GET.
* Current and selected service config version requests used after history navigation.

## Acceptance Criteria

* The recent request list converges correctly when REST snapshots and request events arrive in either order, remains de-duplicated and sorted, excludes upgrade requests, and does not grow beyond its visible bound.
* Request, host, task, and log levels display their classic fields and support failure, Retry, filters, status transitions, newly introduced tasks/hosts, and lifecycle cleanup.
* `FAILED`, `SKIPPED_FAILED`, `TIMEDOUT`, `ABORTED`, and `COMPLETED` are terminal where appropriate; Pending includes queued work and Success maps completed work.
* A disconnected running task is polled without overlapping requests. A connected running task uses its dynamic destination and unsubscribes on every terminal status or when the detail view closes.
* Malformed static or dynamic STOMP messages do not break the application.
* Initial native socket or STOMP failure switches to SockJS eventsource/xhr-polling transports. Established connection loss reconnects after six seconds and restores subscriptions without claiming event replay.
* Only the exact classic Cluster User role is restricted from Background Operations. Manual Navbar entry remains available regardless of the automatic-popup preference for eligible users.
* Abort is shown only with `SERVICE.START_STOP` and an abortable status, confirms before submission, locks duplicate attempts, sends the classic payload, recovers on failure, and refreshes on success.
* Ordinary 202 responses open the returned request. Scheduled responses open the operation list and never request `/requests/{RequestSchedule.id}`.
* A source Request Schedule can be read and cancelled while future batches may run; disabled state is shown; one-host Recommission is not misrepresented as a cancellable schedule.
* Config History performs server paging and sorting, distinguishes filtered and overall totals, applies classic predicate types, safely escapes and encodes match values, and refreshes after config events.
* Config History displays all documented non-Metrics association fields, uses the persisted user timezone, and provides recoverable errors.
* Deleted groups and uninstalled services are not navigable. Eligible rows open Service Configs at the requested group/version, including historical default and non-default versions, without bypassing unsaved-change protection.
* Unit tests cover snapshot/event semantics, terminal and filter mappings, schedule identity, transport URLs/fallback, permission and preference policy, Abort failure recovery, task subscription cleanup, Config History query construction, timezone, association display, config-event refresh, and selected-version navigation.
* Runtime validation covers real WebSocket/SockJS, reconnect loss, schedules, permission roles, task logs, large request/config histories, pagination, timezone, and selected config group/version loading.

## Partial Cross-Module Boundaries

`BG-005` remains partial until business-specific Retry eligibility and commands are reconciled in Installer, Services, Upgrade, Kerberos, and HA modules. The generic Background Operations popup must not guess or issue those commands.

`BG-006` remains partial until HSI pending-schedule conflict protection and complete wizard/upgrade mutual exclusion are reconciled in their owning modules. This issue provides the Request Schedule contracts and generic cancellation behavior but does not claim those flows are complete.

## Out of Scope

* Metrics dashboards, Metrics tabs, Metrics widgets, charts, Heatmaps, Horizon Charts, and Metrics data APIs.
* Dashboard widget health summaries recorded as `DASH-005`.
* Business-specific retry implementations owned by Installer, Services, Upgrade, Kerberos, or HA.
* Complete HSI, wizard, or upgrade scheduling mutual exclusion owned by later modules.
* A new universal REST replacement for every classic realtime mapper. Each owning module must validate its documented snapshot and convergence boundary.

## Compatibility Decisions

The React implementation must not reproduce known unsafe or failure-prone classic behavior:

* Loaded task output is opened by assigning `textContent` to a `pre` element rather than interpolating it through `document.write()`.
* `TIMEDOUT` and `SKIPPED_FAILED` terminate polling and task subscriptions rather than leaking them.
* Missing tasks, unseen hosts/tasks, and malformed socket messages are tolerated rather than dereferencing an absent Ember model hierarchy.
* A Request Schedule ID is never treated as a request ID.
* Match filter values are regex-escaped and URL-encoded before they enter an Ambari predicate.
* Native connection resources are deactivated before an initial-failure fallback activates SockJS.

These are intentional compatibility corrections, not missing parity.

## Verification Boundary

Static code and unit tests are not sufficient to complete this issue. Ten feature IDs remain `NEEDS_RUNTIME_VALIDATION`, while `BG-005` and `BG-006` remain `PARTIAL`, until the runtime matrix in `docs/frontend-refactor/react-current/02-background-dashboard-gap.md` is executed against a real Ambari Server, representative permission roles, scheduled operations, proxy transports, large histories, and the later owning modules.
