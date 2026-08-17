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

# React Hosts Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/03-hosts.md` |
| React implementation | `ambari-web/latest`, module 03 work based on `c01b54be25` |
| Feature IDs | `HOST-LIST-001` through `HOST-ADD-008` |
| Review date | 2026-08-14 |
| Metrics boundary | Host Metrics, metric charts, metric filters/data, and `HOST-TAB-005` are excluded |

The comparison used the corrected Ember baseline, classic source, React source, AJAX definitions and call sites, direct HTTP calls, browser entry points, and realtime inventories. The heuristic `generated/api-by-module/hosts.md` inventory was used only as a search aid.

## Current Conclusion

| Status | Count |
| --- | ---: |
| `NEEDS_RUNTIME_VALIDATION` | 46 |
| `PARTIAL` | 3 |
| `NOT_REQUIRED` | 4 |
| `OUT_OF_SCOPE` | 1 |
| Total | 54 |

No known static implementation gap remains in the 46 runtime-gated IDs. `HOST-LIST-005`, `HOST-BULK-010`, and `HOST-COMP-009` retain explicit boundaries described below. The four `NOT_REQUIRED` IDs are classic placeholders or broken unreachable behavior and must not be recreated in React.

## Feature Status

### List, Search, and Selection

| ID | Status | React implementation and remaining boundary |
| --- | --- | --- |
| `HOST-LIST-001` | `NEEDS_RUNTIME_VALIDATION` | Server paging/sorting, host/component/version mapping, totals, health, maintenance, selection, realtime convergence, serialized polling, and recoverable failures are implemented |
| `HOST-LIST-002` | `NEEDS_RUNTIME_VALIDATION` | Host, component, state, stale-config, maintenance, and version predicates are built through the classic field/type map; stack-version visibility now applies classic older/compatible rules |
| `HOST-LIST-003` | `NEEDS_RUNTIME_VALIDATION` | Combo facets, multi-values, lazy host suggestions, regex escaping, field allowlisting, and Axios override-body request shape are implemented |
| `HOST-LIST-004` | `NEEDS_RUNTIME_VALIDATION` | Row and current-page select-all operations preserve selected host names across paging/filter changes and drive Selected/Filtered/All counts and targets |
| `HOST-LIST-005` | `PARTIAL` | Host, alert, component, and version navigation entry points work; arbitrary Combo Search state is not yet restored after leaving and returning to Hosts |
| `HOST-LIST-006` | `NOT_REQUIRED` | Classic contains no Hosts export entry point or backend contract |

### Bulk Operations

| ID | Status | React implementation and remaining boundary |
| --- | --- | --- |
| `HOST-BULK-001` | `NEEDS_RUNTIME_VALIDATION` | Start/stop/restart eligibility, skip sets, immediate/scheduled requests, progress, and multi-NameNode checkpoint protection are implemented |
| `HOST-BULK-002` | `NEEDS_RUNTIME_VALIDATION` | Host maintenance authorization, no-op filtering, stack mismatch warning, update, and feedback are implemented |
| `HOST-BULK-003` | `NOT_REQUIRED` | Classic has no bulk component-maintenance operation; React correctly does not invent one |
| `HOST-BULK-004` | `NEEDS_RUNTIME_VALIDATION` | DataNode, NodeManager, and RegionServer decommission/recommission paths preserve their distinct checks and request flows |
| `HOST-BULK-005` | `NEEDS_RUNTIME_VALIDATION` | Install/reinstall component/client filtering, KDC session gating, request submission, and progress are implemented |
| `HOST-BULK-006` | `NEEDS_RUNTIME_VALIDATION` | Stale client config refresh/configure selection and request execution are implemented |
| `HOST-BULK-007` | `NEEDS_RUNTIME_VALIDATION` | Set Rack is exposed through the classic outer Host Actions gate and submits only validated changes |
| `HOST-BULK-008` | `NEEDS_RUNTIME_VALIDATION` | Bulk host-deletion dry run, last/non-addable/running component checks, skipped hosts, confirmation, and result reporting are implemented |
| `HOST-BULK-009` | `NEEDS_RUNTIME_VALIDATION` | Component deletion is available for Selected and Filtered, deliberately absent for All, and enforces state/minimum-count checks and dry-run results |
| `HOST-BULK-010` | `PARTIAL` | Immediate/scheduled request creation and progress are present; complete pending-schedule/wizard/upgrade conflict enforcement remains shared with module 02 and owning flows |

### Host Details and Actions

| ID | Status | React implementation and remaining boundary |
| --- | --- | --- |
| `HOST-DETAIL-001` | `NEEDS_RUNTIME_VALIDATION` | Non-Metrics host identity, health, rack, OS, uptime, capacity summaries, and component state/actions are mapped |
| `HOST-DETAIL-002` | `NEEDS_RUNTIME_VALIDATION` | Rack validation, confirmation, update, and model refresh are implemented |
| `HOST-DETAIL-003` | `NEEDS_RUNTIME_VALIDATION` | Host maintenance permission, impact/version warning, request, and feedback are implemented |
| `HOST-DETAIL-004` | `NEEDS_RUNTIME_VALIDATION` | Deletability checks, special-component configuration loading, sequential component deletion, configuration write, and final host deletion stop on the first failure |
| `HOST-DETAIL-005` | `NEEDS_RUNTIME_VALIDATION` | Recover Host validates component states, submits ordered INIT/INSTALLED batches, obtains KDC state, regenerates keytabs for Kerberos, and opens progress |
| `HOST-DETAIL-006` | `NEEDS_RUNTIME_VALIDATION` | Single-client and all-client archive URLs use the correct resource scopes; blocked popups no longer cause a null `focus()` call |
| `HOST-DETAIL-007` | `NEEDS_RUNTIME_VALIDATION` | Entry requires enabled non-manual Kerberos plus `regenerateKeytabsOnSingleHost`; like classic it has no invented UI authorization gate and relies on backend authorization |
| `HOST-DETAIL-008` | `NEEDS_RUNTIME_VALIDATION` | Start/stop/restart-all excludes clients, honors heartbeat and permission state, checks every selected NameNode checkpoint, loads the configured HDFS user for recovery instructions, and opens the returned request once |
| `HOST-DETAIL-009` | `NEEDS_RUNTIME_VALIDATION` | Admin/Operator Check Host confirmation, task creation/polling, categorized warnings, rerun, failure, and cleanup are present and remain separate from Recover Host |
| `HOST-DETAIL-010` | `NOT_REQUIRED` | The classic log donut uses random placeholder data and is not reproduced |

### Host Component Actions

| ID | Status | React implementation and remaining boundary |
| --- | --- | --- |
| `HOST-COMP-001` | `NEEDS_RUNTIME_VALIDATION` | Per-component start/stop/restart state, maintenance, heartbeat, permission, confirmation, and progress behavior are implemented |
| `HOST-COMP-002` | `NEEDS_RUNTIME_VALIDATION` | Install/reinstall creates or associates the host component before installation and propagates request failures |
| `HOST-COMP-003` | `NEEDS_RUNTIME_VALIDATION` | Optional-component cardinality/dependency/host checks, recommendation UI, host selection, configuration rework, component-authoritative service selection, Oozie/Hive config gates, and the HDFS/Ozone exclusion are implemented |
| `HOST-COMP-004` | `NEEDS_RUNTIME_VALIDATION` | Last-instance/state validation, JournalNode routing, recommendation/config updates, deletion, and error propagation are implemented |
| `HOST-COMP-005` | `NEEDS_RUNTIME_VALIDATION` | Slave decommission/recommission and HDFS/HBase/YARN-specific safety checks are implemented |
| `HOST-COMP-006` | `NEEDS_RUNTIME_VALIDATION` | Component maintenance visibility accepts the classic Service or Host permission path and refreshes after update |
| `HOST-COMP-007` | `NEEDS_RUNTIME_VALIDATION` | Refresh-config actions use stack metadata/stale state and track their request |
| `HOST-COMP-008` | `NEEDS_RUNTIME_VALIDATION` | Stack-defined custom commands preserve service/component/host filters and request progress |
| `HOST-COMP-009` | `PARTIAL` | Eligible Move Master entries navigate to the Reassign Master flow; full Reassign behavior belongs to the later HA module |
| `HOST-COMP-010` | `NEEDS_RUNTIME_VALIDATION` | Visible compatible host versions can be installed with exact payload, authorization, confirmation, duplicate lock, error Retry, optimistic state, and request progress |
| `HOST-COMP-011` | `NOT_REQUIRED` | React does not reproduce classic's unreachable `UPGRADE_FAILED` action backed by an unregistered endpoint and hard-coded obsolete version |

### Detail Tabs and Logs

| ID | Status | React implementation and remaining boundary |
| --- | --- | --- |
| `HOST-TAB-001` | `NEEDS_RUNTIME_VALIDATION` | Host Configs loads only services assigned to the host, applies overrides/groups, hides all editing controls, and gates only config-group reassignment with `SERVICE.MANAGE_CONFIG_GROUPS` |
| `HOST-TAB-002` | `NEEDS_RUNTIME_VALIDATION` | Host alert instances, filters, sorting, paging, service/definition links, serialized polling, Retry, and teardown are implemented |
| `HOST-TAB-003` | `NEEDS_RUNTIME_VALIDATION` | Versions availability routing, classic visibility rules, filters, install authorization/payload/progress, and failure Retry are implemented |
| `HOST-TAB-004` | `NEEDS_RUNTIME_VALIDATION` | Logs menu/route, metadata, filters, tail, copy/open, Log Search links, quick-link resolution, failure recovery, and polling cleanup are implemented |
| `HOST-LOG-001` | `NEEDS_RUNTIME_VALIDATION` | Host logging resources are mapped by service/component/file with filters, sort, paging, and recoverable metadata failures |
| `HOST-LOG-002` | `NEEDS_RUNTIME_VALIDATION` | Tail size changes, serialized two-second refresh, older-page loading, merge/deduplication, copy/open, Retry, dependency reset, and unmount cleanup are implemented |
| `HOST-LOG-003` | `NEEDS_RUNTIME_VALIDATION` | LOGSEARCH quick-link resolution builds encoded host/component/path URLs for both log rows and the tail popup |
| `HOST-LOG-004` | `NEEDS_RUNTIME_VALIDATION` | Loaded text is copied or opened locally with `textContent`; Hosts task logs reuse the module 02 Background Operations implementation |
| `HOST-TAB-005` | `OUT_OF_SCOPE` | Host Metrics and all Metrics data are excluded |

### Add Host Wizard

| ID | Status | React implementation and remaining boundary |
| --- | --- | --- |
| `HOST-ADD-001` | `NEEDS_RUNTIME_VALIDATION` | Seven-step Add Host uses lowercase normalization, duplicate/installed handling, pattern/FQDN warnings, Linux SSH/manual modes, Windows PowerShell mode, support flags, and classic-compatible bootstrap fields |
| `HOST-ADD-002` | `NEEDS_RUNTIME_VALIDATION` | Bootstrap launch/poll, registration wait, retry/remove, preinstalled checks, warning categories, rerun, skip boundaries, and timer cleanup are implemented |
| `HOST-ADD-003` | `NEEDS_RUNTIME_VALIDATION` | Slave/client assignment uses installed metadata, dependencies, cardinality, and concrete client expansion |
| `HOST-ADD-004` | `NEEDS_RUNTIME_VALIDATION` | Only selected component services load config groups; Default/non-default selection and the empty-component skip path are persisted |
| `HOST-ADD-005` | `NEEDS_RUNTIME_VALIDATION` | Review summarizes hosts/components/groups, registers hosts and component associations, applies full config-group payload arrays, checkpoints stages, and exposes retryable failures |
| `HOST-ADD-006` | `NEEDS_RUNTIME_VALIDATION` | Install and selected non-client start requests are polled without duplicate Strict Mode submissions; Kerberos keytabs regenerate between phases; failed phases and task logs are retryable. No explicit service-check request is invented |
| `HOST-ADD-007` | `NEEDS_RUNTIME_VALIDATION` | Summary distinguishes success/warnings/failures, shows failed tasks, clears wizard state, and returns to Hosts without mutating cluster provisioning state |
| `HOST-ADD-008` | `NEEDS_RUNTIME_VALIDATION` | Hydration, current step and phase/request persistence, serialized writes, resume, cancellation, completion cleanup, initialization errors, and Retry are implemented |

## Backend Contract Comparison

| Contract area | React behavior | Runtime gate |
| --- | --- | --- |
| Host list/details | Uses Ambari POST plus `X-Http-Method-Override: GET` predicates where required; maps host/component/version/log resources and realtime host/component/request events | Large clusters, paging totals, every predicate, slow requests, and REST/event races |
| Host suggestions | Keeps paging/fields in Axios `params`, allowlists the field, regex-escapes input, and places the predicate in `RequestInfo.query` | Real server predicate parser, empty input, Unicode, large distinct sets, and 403/500 |
| Host/component updates | Preserves `RequestInfo.context`, operation level, query, desired state/admin/maintenance state, and returned `Requests.id` | Every component state, no-op/synchronous response, permission denial, and backend validation |
| Bulk schedules | Preserves host/component filters, immediate and scheduled batches, intervals/tolerance, dry runs, skip sets, and progress identity | Pending schedules, collisions with wizard/upgrade, partial execution, cancellation, and long batches |
| Host/component deletion | Uses component DELETE, config reconfiguration, dry-run host/component bulk DELETE, and final host DELETE without continuing after a failed prerequisite | Last master, special masters, unknown components, partial dry-run result, config failure, and concurrent state changes |
| Check/Recover Host | Uses preinstalled check task create/poll separately from ordered recovery batches and Kerberos regeneration | Every warning family, rerun, heartbeat loss, mixed component states, KDC expiry, and task failure |
| Alerts | Loads `/clusters/{cluster}/alerts` with exact host filtering and periodic refresh | High alert volume, maintenance states, deleted definitions, and route teardown |
| Host stack versions | Loads compatible repository versions, applies classic visibility, and POSTs exact `HostStackVersions` install payload | Cross-stack compatibility, support flag, older versions, all states, permissions, and install failures |
| Host Configs/groups | Loads stack configurations/themes, current values, host services, and full service config groups; PUTs complete group-array payloads for membership transitions | Default/non-default moves, multiple services/groups, final/hidden/custom/widget properties, 403, and concurrent edits |
| Host logs | Loads host logging metadata and `/logging/searchEngine` tail pages; resolves LOGSEARCH quick links/config and opens encoded external URLs | Real log payload fields, rotation, pagination ordering, quick-link overrides/HTTPS, clipboard and popup blocking |
| Add Host bootstrap | POSTs `/bootstrap`, polls `/bootstrap/{requestId}`, waits for registration, then runs preinstalled checks | Linux SSH users/ports/keys, manual Agent, Windows PowerShell, mixed success, timeout, retry, and removal |
| Add Host deployment | Registers hosts, associates component groups, PUTs full config groups, installs and starts selected components, regenerates Kerberos keytabs, and polls request/tasks | Multi-service/client assignments, HA clusters, KDC renewal, partial phase failures, resume, cancellation, and browser reload |

## Five Independent Audits

| Pass | Independent entry | Findings | Result |
| --- | --- | --- | --- |
| 1. Feature and route inventory | Every baseline ID, Hosts menu, list row, detail action, tab, log popup, and all seven Add Host steps | Missing Logs UI, incomplete detail routes, generic Installer steps in Add Host, wrong action visibility, and unrecoverable load failures were found | All entry points are mapped; placeholders and Metrics are explicitly excluded; three cross-module/persistence boundaries remain named |
| 2. API and payload reverse scan | Classic AJAX definitions/call sites, direct HTTP/browser calls, React APIs, predicates, IDs, operation levels, config groups, bootstrap, tasks, and quick links | Malformed host suggestion Axios shape, missing comma in fields, unencoded identifiers, wrong request ID shape, incomplete config-group payload, and missing compatible-version/log-search contracts were found | Exact request construction and response identity are covered by API/utility tests and the runtime contract table |
| 3. Permission, state, and destructive sequencing | Every classic authorization wrapper and indirect action-map condition; selection modes; delete/install/reconfigure ordering | Baseline incorrectly claimed keytab authorization, bulk component maintenance, editable Host Configs, and Add Host service checks; React hid actions incorrectly and could continue after failed component/config work | Baseline corrected; action gates match classic; component/host deletion and Add Host stages stop and retry at failures |
| 4. Async, realtime, HA, Kerberos, and persistence | Poll overlap/cleanup, socket convergence, multi-NameNode checks, KDC sessions/keytabs, wizard hydration/write ordering, Strict Mode, cancellation, retry | Polls could overlap/leak, multi-NameNode checkpoint checks were skipped, stage labels could persist stale text, config-group failure was silent in classic, and stale wizard writes could follow cleanup | Polling and persistence are serialized; lifecycle cleanup and stage checkpoints are explicit; React deliberately blocks on config-group failure |
| 5. Executable acceptance and regression scan | Focused utility/API/component tests, TypeScript build, full test/build/lint commands, diff checks, and runtime scenario design | Static reading alone could not prove request shapes, failure stops, compatibility filtering, task polling, or Strict Mode behavior | Targeted tests cover high-risk boundaries; full repository verification and the real-cluster matrix remain mandatory |

## Compatibility Decisions

| Classic behavior or defect | React decision |
| --- | --- |
| Add Host starts installation even when an unawaited config-group PUT fails silently | Stop before installation, show the backend error, retain completed checkpoints, and retry only the failed/remaining stages |
| Add Host Windows plus `customizeAgentUserAccount=true` can validate a hidden empty Agent user | Do not mount or validate Linux-only fields in Windows PowerShell mode |
| Direct Logs URL bypasses installed-LOGSEARCH and `SERVICE.VIEW_OPERATIONAL_LOGS` menu conditions | Apply the complete menu condition to direct React navigation rather than preserving the classic gate gap |
| Log text is written with `document.write()` | Open a local `pre` and assign `textContent`; use `noopener noreferrer` for Log Search links |
| Host/component destructive callbacks can lose rejected promises | Await each prerequisite, surface its backend message, and never delete the host after a component/config failure |
| `UPGRADE_FAILED` component action calls an unregistered obsolete endpoint | Do not expose or implement the broken action |
| A host-version compatibility model may not be loaded when Hosts maps versions | Fetch the compatible repository set before mapping and apply the same older/cross-stack visibility rule deterministically |

## Automated Evidence

Focused coverage includes:

* Host event/component/request convergence, maintenance transitions, predicate escaping, stack-version compatibility, and destructive sequencing.
* Host suggestions, alerts, stack-version installation/compatibility, bootstrap, config groups, host logs, and KDC mode API shapes.
* Serialized polling failure/retry/unmount behavior.
* Host Alerts and Stack Versions filters, permissions, install failure recovery, and progress.
* Host Configs read-only controls, service-specific groups, Default/non-default transitions, authorization, and Retry.
* Add Host input helpers, assignments, config-group payloads, deployment failure checkpoints, Strict Mode polling, unmount cleanup, and completion semantics.
* Multi-NameNode checkpoints and configured HDFS-user instructions, Selected/Filtered/All bulk targeting, component-delete visibility, failure-stop sequencing, config-driven optional-component selection, Log Search encoding, and log-tail teardown.

## Runtime Acceptance Matrix

1. Load 0, 1, 10, 11, hundreds, and thousands of hosts; page, sort, filter, select, receive host/component/request events, and inject slow/failed responses.
2. Exercise every Combo Search facet/operator/multi-value, regex metacharacters, empty suggestions, service/component/version route filters, and filter return-navigation behavior.
3. Run Selected, Filtered, and All bulk actions; verify the exact target set and that component Delete exists only for Selected/Filtered.
4. Start, stop, restart, install, refresh, decommission, recommission, maintain, set rack, and delete with eligible, skipped, no-op, heartbeat-lost, maintenance, and permission-denied targets.
5. Stop/restart one and multiple NameNodes with recent, old, missing, failed, active, and standby checkpoint responses; ensure one warning/operation submission.
6. Delete a component and host with last-master, running, unknown, special reconfiguration, config-load, config-save, component-delete, and host-delete failures.
7. Recover hosts with every allowed/disallowed component-state mix in simple and Kerberized clusters, including expired KDC sessions and keytab failure.
8. Validate Host Actions and component actions as Cluster User, service operator, cluster operator, cluster administrator, and Ambari administrator during normal, upgrade, wizard, and heartbeat-loss states.
9. Validate single/all client-config downloads, encoded names, browser popup blocking, and archive contents.
10. Run Check Host with every warning category, no warnings, create/poll failure, rerun, close, route change, and slow task response.
11. Load Host Configs for zero/one/many services and groups; move Default to non-default, between non-default groups, and back; cover 403/500, concurrent changes, hidden/final/custom/widget properties, and role visibility.
12. Load and mutate host alerts while sorting/filtering/paging; navigate to service and definition; leave during a slow poll and verify teardown.
13. Validate same-stack older/current/newer versions, compatible/incompatible cross-stack versions, `displayOlderVersions`, all host states, install permission, 202/no-ID/error responses, and progress.
14. Load logs with missing metadata, many services/components/files, `.log`/`.out`, log rotation, duplicate pages, tail-size changes, older pages, slow/failing polls, Retry, copy/open, and unmount.
15. Resolve HTTP/HTTPS and overridden LOGSEARCH quick links; open row/tail links and verify exact host/component/path selection and `noopener` isolation.
16. Add Linux hosts using root/non-root SSH, custom port, valid/invalid keys, manual Agent registration, patterns, mixed installed/new hosts, lowercase/duplicate names, and all-installed rejection.
17. Add Windows hosts through PowerShell Remoting with `customizeAgentUserAccount` both false and true; confirm hidden Linux fields never block continuation.
18. Run bootstrap with mixed success/failure/timeout, retry/remove, late Agent registration, skipped checks, every environment warning, rerun, and route/modal cleanup.
19. Assign multiple slaves and concrete clients across services and config groups; cover no components, Default, existing non-default groups, dependency/cardinality limits, and missing metadata.
20. Fail and retry every Review/Install/Start/Kerberos/config-group phase; reload between phases; use Strict Mode; inspect task logs; cancel/complete while writes are queued; verify no stale state or provisioning-state mutation.
21. Exercise immediate and scheduled batches with pending schedule, wizard, upgrade, partial batch, cancellation, request/schedule identity, and Background Operations progress.
22. Enter Add Host and Logs through both menus and direct URLs to confirm the documented classic and intentional React authorization boundaries.

No `NEEDS_RUNTIME_VALIDATION` item may be changed to `COVERED` until its applicable matrix cases pass against a real Ambari Server.
