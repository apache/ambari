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

The React Hosts module does not yet provide a reliable equivalent to the classic Ember UI. Host list data and realtime events can diverge, failures can leave pages permanently loading, bulk target queries and action visibility do not consistently match Selected/Filtered/All semantics, and destructive component/host operations may continue after a failed prerequisite.

Host Details also lacks complete routing, permission, checkpoint, Kerberos, Check/Recover Host, Alerts, Stack Versions, Host Configs, and Logs behavior. Host Configs incorrectly exposes editing concepts even though classic Host mode is read-only. Host Stack Version rows do not apply classic older-version and cross-stack compatibility visibility. Host Logs lacks complete metadata/tail lifecycle and Log Search navigation.

The Add Host flow reuses generic Installer steps and misses critical Add Host-specific behavior across Linux SSH, manual Agent registration, Windows PowerShell Remoting, bootstrap, environment checks, slave/client assignment, config groups, deployment checkpoints, installation/start polling, Kerberos keytabs, retries, summary, persistence, cancellation, and completion.

This issue covers the complete non-Metrics Hosts module defined by the corrected classic baseline. Host Metrics, metric charts, and Metrics APIs are not part of the scope.

## Scope

* Reconcile paged Host REST data with host-component, host, and completed decommission/recommission events without losing false, zero, OFF, or multi-host updates.
* Preserve server paging/sorting, Combo Search predicates, lazy suggestions, regex safety, Selected/Filtered/All target construction, counts, and action visibility.
* Add recoverable loading and request failures and ensure pollers schedule only after the current request settles and clean up on dependency change or unmount.
* Match bulk start/stop/restart, host maintenance, decommission/recommission, install/reinstall, configure, set rack, host deletion, component deletion, and immediate/scheduled request behavior.
* Check every affected NameNode checkpoint before stop/restart and submit the protected operation only once.
* Match Host Details routes and Host/component action permission, heartbeat, maintenance, state, cardinality, dependency, recommendation, reconfiguration, and progress boundaries.
* Stop component/host deletion after the first failed component, configuration load, configuration save, or backend delete request.
* Preserve separate Check Host and Recover Host flows, including warning categories, rerun, ordered recovery, KDC session checks, and keytab regeneration.
* Make Host Configs service-specific and read-only, map assigned config groups/overrides, and gate only config-group reassignment with `SERVICE.MANAGE_CONFIG_GROUPS`.
* Implement Host Alerts polling/filtering/navigation and Host Stack Version visibility, filtering, authorization, installation, Retry, and request progress.
* Implement Host Logs metadata, filters, sorting, paging, tail/older rows, merge/deduplication, copy/open, serialized polling, cleanup, error Retry, and encoded Log Search quick-link navigation.
* Replace generic Installer Review/Install/Summary reuse with Add Host-specific steps and exact stage persistence.
* Match Add Host Linux SSH/manual modes, Windows PowerShell behavior, support flags, hostname normalization/validation, installed-host handling, bootstrap polling, registration, checks, retry, and removal.
* Expand generic clients to concrete components, preserve service ownership, assign config groups with full array payloads, and block installation on failed prerequisites.
* Install then start only selected non-client components, regenerate Kerberos keytabs between phases, poll tasks, expose logs, retry failed phases, and prevent duplicate Strict Mode requests.
* Complete with a success/warning/failure summary, clear persisted state, return to Hosts, and never apply the new Installer provisioning-state mutation.
* Add focused tests for APIs, predicates, event reconciliation, permissions, version visibility, config groups, destructive sequencing, bulk targeting, Add Host stages, polling, persistence, logs, and failure recovery.

## Classic UI Baseline

The acceptance baseline is `docs/frontend-refactor/ember-baseline/03-hosts.md`, feature IDs `HOST-LIST-001` through `HOST-ADD-008`. The detailed React comparison, five-pass audit, backend contracts, compatibility decisions, and runtime matrix are recorded in `docs/frontend-refactor/react-current/03-hosts-gap.md`.

The baseline was corrected during the audit:

* Classic has no bulk Component Maintenance operation; bulk `PASSIVE_STATE` is host-level.
* Host keytab regeneration has indirect manual-Kerberos visibility but no explicit UI authorization gate.
* Host Configs properties are always read-only; only config-group reassignment is permission-gated.
* Add Host starts selected host components but does not explicitly request service checks.

The authoritative network comparison includes global AJAX definitions/call sites, direct HTTP calls, browser entry points, realtime destinations, bootstrap/check requests, config groups, logging/quick links, and host/component/version operations.

## Acceptance Criteria

* Host list paging, sorting, filters, counts, and selections converge with realtime updates and expose Retry without overlapping requests or orphan timers.
* Suggestion requests use an allowlisted field, escaped predicate, Axios query params, and `X-Http-Method-Override` body with no malformed or double-encoded query.
* Selected, Filtered, and All operations target exactly their intended hosts; component deletion is offered for Selected/Filtered and not All.
* Bulk and detail actions match classic permission/state/maintenance/heartbeat rules and open only real returned request IDs.
* Every affected NameNode is checked; old or unavailable checkpoints produce one warning and one eventual callback.
* Host/component destructive flows never continue after a failed prerequisite and display the backend error.
* Check Host, Recover Host, client downloads, keytab regeneration, rack changes, host maintenance, and start/stop/restart-all match their documented independent boundaries.
* Optional components use their own service metadata, validate dependencies/cardinality and Oozie/Hive/Ozone configuration gates, carry rejected promises, and apply selected configuration recommendations before install.
* Host Configs contains no add/remove/final/undo/widget editing controls, shows only host services, maps groups/overrides, and performs complete Default/non-default membership transitions under the exact permission.
* Host Alerts stop polling on exit and Host Stack Versions apply older/cross-stack compatibility before display and submit exact install payloads with recoverable failure.
* Host Logs can filter and tail files, load older rows, copy/open safe text, stop polling on close, and open encoded Log Search URLs from both row and popup.
* Add Host handles Linux automatic/manual and Windows modes, validates and normalizes hosts, polls bootstrap/registration/checks, supports retry/remove, and cleans every timer.
* Add Host Review reports concrete components/groups, persists completed checkpoints, blocks on host/component/config-group/install failures, and retries only remaining work.
* Add Host Install polls install/start tasks, regenerates keytabs in Kerberos mode, prevents duplicate phase requests, exposes failed-task logs and Retry, and does not invent an explicit service-check request.
* Add Host Summary reports final outcomes, clears wizard persistence, returns to Hosts, and does not mutate cluster provisioning state.
* Focused tests cover high-risk request shapes, false/zero event values, multi-host decommission, compatibility filtering, permission controls, failure sequencing, bulk targeting, Strict Mode, unmount cleanup, Log Search encoding, and Add Host recovery.
* The applicable runtime matrix in `docs/frontend-refactor/react-current/03-hosts-gap.md` passes against a real Ambari Server.

## Partial Cross-Module Boundaries

`HOST-LIST-005` remains partial because arbitrary Combo Search state is not restored after navigating away and back. Direct component/version route filters and ordinary navigation are implemented.

`HOST-BULK-010` remains partial until all pending-schedule, wizard, and upgrade conflicts are enforced across their owning modules. This issue preserves existing immediate/scheduled request behavior and correct request/schedule identity.

`HOST-COMP-009` remains partial because this module owns the eligible Move Master entry, while the complete Reassign Master wizard belongs to the HA module.

## Out of Scope

* Host Metrics, Metrics routes, charts, metric filters, metric polling, and Metrics APIs.
* Classic's random Host Summary log-count donut placeholder.
* A Hosts export feature, because classic contains no such entry or contract.
* Bulk Component Maintenance, because classic exposes only bulk Host Maintenance.
* The broken unreachable classic component Re-upgrade action and its unregistered obsolete endpoint.
* Full Reassign Master, HA, upgrade, and global Request Schedule conflict workflows owned by later modules.

## Compatibility Decisions

The React implementation must not reproduce known unsafe or failure-prone classic behavior:

* A failed Add Host config-group update blocks installation and is retryable instead of failing silently while deployment continues.
* Windows mode does not validate hidden Linux Agent/SSH fields.
* Direct Logs navigation applies the complete menu authorization/installation gate rather than preserving classic's direct-route gap.
* Loaded logs are opened with `textContent` and external Log Search links use encoded values plus `noopener noreferrer`.
* Destructive operations await every prerequisite and stop on rejection.
* Incompatible or hidden host versions are computed deterministically before rendering.
* The broken classic `UPGRADE_FAILED` component action is not implemented.

These are intentional compatibility corrections, not missing parity.

## Verification Boundary

Static code and focused tests are not sufficient to mark this issue covered. Forty-six IDs remain `NEEDS_RUNTIME_VALIDATION`, and three IDs remain `PARTIAL`, until the real-cluster matrix covers large host sets, every permission role and component state, HA/Kerberos modes, schedules, destructive failures, config groups, alerts, stack compatibility, logs/quick links, Linux/manual/Windows Add Host modes, bootstrap/check failures, reload/resume/cancel, and every deployment phase.
