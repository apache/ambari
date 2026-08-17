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

# React Services and Configs Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/04-services-configs.md` |
| React implementation | `ambari-web/latest`, Module 04 work based on `c01b54be258f04282e00403e7b77ee8054aab947` |
| Feature IDs | 60 non-Metrics IDs from `SVC-NAV-001` through `SVC-MOVE-006` |
| Review date | 2026-08-14 |
| Metrics boundary | Service Metrics, Heatmaps, metric display data, Metrics APIs, and metric widgets are excluded |

The audit compared the baseline with the actual `ambari-web/classic` implementation and the current React source. It also reverse-scanned the generated AJAX definitions and call sites, direct HTTP calls, browser network entry points, realtime channels, permissions, feature flags, and routes. The heuristic `generated/api-by-module/services-configs.md` inventory was treated as a candidate list, not an authoritative contract.

The legacy `/main/services/:service_id/audit` outlet has no functional page and remains a placeholder. React is not required to add an Audit tab.

## Initial Static Conclusion

| Status | Count |
| --- | ---: |
| `STATICALLY_ALIGNED` | 1 |
| `PARTIAL` | 50 |
| `INCORRECT` | 6 |
| `MISSING` | 3 |
| Total | 60 |

This table records the state before Module 04 implementation. A feature is not considered covered merely because a route or similarly named component exists. `STATICALLY_ALIGNED` still requires runtime validation against Ambari Server.

## Post-Implementation Status

`STATICALLY_ALIGNED` means the reviewed React behavior and focused executable evidence now match the classic contract, but the corresponding live scenario remains in the runtime acceptance matrix. `IMPROVED_PARTIAL` means this patch fixes identified defects while other parity work remains. `CROSS_MODULE_BOUNDARY` identifies behavior that cannot be completed without reconciling Module 03-owned Hosts code.

| Final status | Count |
| --- | ---: |
| `STATICALLY_ALIGNED` | 26 |
| `IMPROVED_PARTIAL` | 21 |
| `UNCHANGED_PARTIAL` | 11 |
| `CROSS_MODULE_BOUNDARY` | 2 |
| `MISSING` | 0 |
| Total | 60 |

The written Ember baseline overstates `SVC-ALL-004`: the global action does not offer immediate versus rolling/scheduled execution. Classic `main/service.js#restartAllRequired` confirms and invokes `restart.staleConfigs` once; its AJAX definition sends one `POST /clusters/{cluster}/requests` with the stale-host-component predicate. Rolling selection belongs to the per-service Summary/Configs flows. The status and runtime matrix below follow the executable Classic source and generated AJAX evidence. The source baseline should be corrected when it is next integrated into this worktree.

| ID | Final status | Post-implementation evidence or remaining gap |
| --- | --- | --- |
| `SVC-NAV-001` | `STATICALLY_ALIGNED` | URL changes and Back/Forward update the selected tab; invalid services replace to the first installed service Summary, and invalid or unauthorized tabs replace to Summary without rewriting embedded wizard routes. |
| `SVC-ALL-001` | `IMPROVED_PARTIAL` | Authorization, `enableAddDeleteServices`, conflicting-wizard/upgrade route gates, Add Service-owned cleanup, and provisioning-state behavior are fixed; full seven-step recovery remains runtime-required. |
| `SVC-ALL-002` | `STATICALLY_ALIGNED` | Preserves the broad classic service PUT and now adds health eligibility, confirmation, background progress, rejection details, and Retry. |
| `SVC-ALL-003` | `STATICALLY_ALIGNED` | Preserves the broad classic service PUT and now adds health eligibility, HDFS checkpoint/impact confirmation, background progress, rejection details, and Retry. |
| `SVC-ALL-004` | `STATICALLY_ALIGNED` | Preserves Classic's single confirmed `restart.staleConfigs` POST for every stale host component and exposes request progress and Retry; no global rolling selector is added. |
| `SVC-ALL-005` | `UNCHANGED_PARTIAL` | Cluster client download remains available, but popup-blocked browser behavior and the legacy outer-menu quirk require acceptance. |
| `SVC-ACT-001` | `IMPROVED_PARTIAL` | Start/Stop permission gates are corrected for restart entries; complete action eligibility, schedule choices, and convergence remain partial. |
| `SVC-ACT-002` | `IMPROVED_PARTIAL` | Service Check now requires `SERVICE.RUN_SERVICE_CHECK`; stack availability, result presentation, and retry still need broader coverage. |
| `SVC-ACT-003` | `IMPROVED_PARTIAL` | The exact service maintenance PUT is asserted, success converges local state, and server failure exposes Retry; exhaustive component/alert convergence remains live-required. |
| `SVC-ACT-004` | `IMPROVED_PARTIAL` | Restart entries now require `SERVICE.START_STOP`; stale filtering, rolling parameters, and progress remain partial. |
| `SVC-ACT-005` | `IMPROVED_PARTIAL` | Delete Service now respects authorization, the feature flag, and conflicting-wizard state; dependency and recovery matrices remain incomplete. |
| `SVC-ACT-006` | `IMPROVED_PARTIAL` | YARN queue refresh now requires `SERVICE.RUN_CUSTOM_COMMAND`; command availability and progress remain runtime-required. |
| `SVC-ACT-007` | `IMPROVED_PARTIAL` | HDFS rebalance now requires `SERVICE.RUN_CUSTOM_COMMAND`; topology eligibility and recovery remain partial. |
| `SVC-ACT-008` | `IMPROVED_PARTIAL` | HDFS/YARN service commands now use the semantic custom-command permission; stack/state coverage remains partial. |
| `SVC-ACT-009` | `IMPROVED_PARTIAL` | Dynamic commands no longer inherit unrelated permissions; metadata scopes, parameters, and recovery remain partial. |
| `SVC-ACT-010` | `IMPROVED_PARTIAL` | HA/Federation/Journal and Move entries/routes use semantic permissions and operation guards; all stack-specific conditions remain partial. |
| `SVC-ACT-011` | `STATICALLY_ALIGNED` | Current-service all-client download now uses `SERVICE`; `SERVICE_COMPONENT` remains reserved for one component and cluster download uses `CLUSTER`. |
| `SVC-SUM-001` | `UNCHANGED_PARTIAL` | Non-Metrics summary layouts still lack complete service-specific state, maintenance, restart, and distribution parity. |
| `SVC-SUM-002` | `CROSS_MODULE_BOUNDARY` | Exact Hosts filtering and return-path behavior must be reconciled with Module 03 without editing its files here. |
| `SVC-SUM-003` | `CROSS_MODULE_BOUNDARY` | Complete host-component actions depend on Module 03-owned eligibility and payload helpers; this branch records rather than duplicates them. |
| `SVC-SUM-004` | `STATICALLY_ALIGNED` | Flume Summary discovers per-host handlers, gates state-valid Start/Stop by permission and wizard state, sends the exact handler PUT, tracks progress, refreshes, and exposes Retry. |
| `SVC-SUM-005` | `IMPROVED_PARTIAL` | External links gain corrected descriptor/config/host resolution; separate Ambari View route/iframe parity remains incomplete. |
| `SVC-QL-001` | `STATICALLY_ALIGNED` | Descriptor URL, visible/removed filtering, related-component discovery, and focused API evidence are aligned. |
| `SVC-QL-002` | `STATICALLY_ALIGNED` | Deterministic tests cover `HTTP_ONLY`, `HTTPS_ONLY`, `EXIST`, `NOT_EXIST`, exact values, reversal, missing properties, and the Classic `hdfs-site/dfs.http.policy` fallback. |
| `SVC-QL-003` | `STATICALLY_ALIGNED` | Component hosts are generalized and internal hostnames are resolved through the public-host API before URL generation. |
| `SVC-QL-004` | `STATICALLY_ALIGNED` | Logged-in username and `${config-type/property-name}` placeholders are substituted with focused tests. |
| `SVC-QL-005` | `STATICALLY_ALIGNED` | MapReduce configured-host reverse mapping and Oozie `STARTED` host filtering are implemented. |
| `SVC-QL-006` | `UNCHANGED_PARTIAL` | HDFS/YARN/HBase Active/Standby selection exists but still requires exhaustive operational-state acceptance. |
| `SVC-QL-007` | `STATICALLY_ALIGNED` | External anchors retain `_blank` plus `noreferrer`; all generated branches remain runtime-required. |
| `SVC-CONFIG-001` | `UNCHANGED_PARTIAL` | Theme/category controls still require complete control-type and no-theme fallback acceptance. |
| `SVC-CONFIG-002` | `UNCHANGED_PARTIAL` | Recommended/default/final/read-only metadata semantics remain incomplete across all controls. |
| `SVC-CONFIG-003` | `UNCHANGED_PARTIAL` | Editing/recommendation/dependency/undo parity remains uneven between rows and widgets. |
| `SVC-CONFIG-004` | `STATICALLY_ALIGNED` | Default saves await the request and send every property plus all non-empty attribute categories for each changed type. |
| `SVC-CONFIG-005` | `STATICALLY_ALIGNED` | Navigation proceeds only after an awaited successful save; failure retains the blocker and edits, and refresh follows completion rather than a timer. |
| `SVC-CONFIG-006` | `IMPROVED_PARTIAL` | Compare now requires `SERVICE.COMPARE_CONFIGS`; non-default history selection and read-only behavior remain runtime-required. |
| `SVC-CONFIG-007` | `STATICALLY_ALIGNED` | Make Current requires `SERVICE.MODIFY_CONFIGS`, preserves the history-creating cluster PUT, and refreshes exact installed services with a corrected URL. |
| `SVC-CONFIG-008` | `IMPROVED_PARTIAL` | Empty-string overrides are preserved; complete widget/row restore and recommendation parity remains partial. |
| `SVC-CONFIG-009` | `IMPROVED_PARTIAL` | Restart entries use `SERVICE.START_STOP`; maintenance/state filters and rolling progress remain incomplete. |
| `SVC-CONFIG-010` | `STATICALLY_ALIGNED` | Create, missing-ID, task-list, poll, terminal task, and non-zero DB check failures end Connecting and permit Retry. |
| `SVC-CONFIG-011` | `STATICALLY_ALIGNED` | Single/bulk parsing validates trimmed keys, duplicates, existing entries, true line numbers, and extra `=` values; removed keys can be re-added and persisted deletion is omitted from the replacement payload. |
| `CFG-GROUP-001` | `STATICALLY_ALIGNED` | Management now requires `SERVICE.MANAGE_CONFIG_GROUPS` in both selector and modal actions. |
| `CFG-GROUP-002` | `IMPROVED_PARTIAL` | Membership uniqueness is enforced through clear-before-set ordering and save failures remain visible/retryable; create conflict acceptance remains. |
| `CFG-GROUP-003` | `STATICALLY_ALIGNED` | Duplicate copies desired configs and description while intentionally leaving hosts empty; existing-name validation remains in the form. |
| `CFG-GROUP-004` | `STATICALLY_ALIGNED` | Hosts can move directly between non-default groups, with atomic local state and two-phase clear/set server ordering, including swaps. |
| `CFG-GROUP-005` | `STATICALLY_ALIGNED` | Delete returns hosts to Default locally, clears server membership before DELETE, and preserves the modal with an error for Retry. |
| `CFG-GROUP-006` | `STATICALLY_ALIGNED` | Group properties and edit entry preserve the selected config group and version through the shared history selection path; focused history evidence covers group/version resolution. |
| `SVC-ADD-001` | `IMPROVED_PARTIAL` | Entry and direct route now enforce permission, feature flag, upgrade, and conflicting-wizard policy; dependency/conflict validation remains broad. |
| `SVC-ADD-002` | `UNCHANGED_PARTIAL` | Master cardinality, resource, installed-component, and ineligible-host matrices remain incomplete. |
| `SVC-ADD-003` | `UNCHANGED_PARTIAL` | Slave/client retention and host eligibility remain incomplete. |
| `SVC-ADD-004` | `UNCHANGED_PARTIAL` | Config recommendations, credentials, database/account tabs, and recovery remain incomplete. |
| `SVC-ADD-005` | `UNCHANGED_PARTIAL` | Review completeness and no-mutation acceptance remain outstanding. |
| `SVC-ADD-006` | `IMPROVED_PARTIAL` | Direct route ownership is guarded and persisted cleanup is corrected; exact deploy ordering and partial-failure recovery remain runtime-required. |
| `SVC-ADD-007` | `STATICALLY_ALIGNED` | Add Service completion no longer writes Installer-only `Clusters.provisioning_state=INSTALLED`; Cluster Creation behavior is preserved. |
| `SVC-ADD-008` | `IMPROVED_PARTIAL` | Add Service owns cancel cleanup, invalidates pending persistence, and restores owner state; other-user read-only presentation remains incomplete. |
| `SVC-MOVE-001` | `STATICALLY_ALIGNED` | Direct routes now wait for topology and enforce host count, stack `reassign_allowed`, installed component, and available target without inventing an API. |
| `SVC-MOVE-002` | `UNCHANGED_PARTIAL` | Review content and downtime/recommendation completeness remain runtime-required. |
| `SVC-MOVE-003` | `IMPROVED_PARTIAL` | DB task IDs are passed in the correct order and pending polling is awaited; full mutation idempotency and partial-failure recovery remain incomplete. |
| `SVC-MOVE-004` | `STATICALLY_ALIGNED` | Manual-step eligibility is shared across wizard construction and Step 4 task filtering; Oozie current config distinguishes Derby from external databases, load failure is retryable, and Oozie/MySQL commands are component-specific. |
| `SVC-MOVE-005` | `IMPROVED_PARTIAL` | DB task polling now settles correctly; affected-service checks and complete task retry/summaries remain partial. |
| `SVC-MOVE-006` | `STATICALLY_ALIGNED` | A failed DB test exposes the classic narrow rollback sequence; target deletion is idempotent for 404/`NoSuchResourceException` retries. |

## Feature Status

### Service Navigation, Global Actions, and Single-Service Actions

| ID | Initial status | React evidence and gap |
| --- | --- | --- |
| `SVC-NAV-001` | `PARTIAL` | The Services sidebar and Summary navigation exist, but service-event convergence, invalid-service fallback, state/restart/alert fidelity, and recovery require completion and runtime validation. |
| `SVC-ALL-001` | `PARTIAL` | A seven-step Add Service wizard exists. The menu and route omit `supports.enableAddDeleteServices`, active-wizard ownership, upgrade, and route-authorization parity. |
| `SVC-ALL-002` | `PARTIAL` | Start All correctly uses the broad cluster-wide service PUT used by classic, but omits the classic health-based disabled state, confirmation, request progress, and recoverable failure lifecycle. |
| `SVC-ALL-003` | `PARTIAL` | Stop All correctly uses the broad cluster-wide service PUT used by classic, but omits the health-based disabled state, impact/HDFS checkpoint confirmation, request progress, and recoverable failure lifecycle. |
| `SVC-ALL-004` | `PARTIAL` | Restart All and the stale-component request shape exist, but the confirmation/progress/recovery lifecycle and exact payload lacked focused evidence. Classic global behavior is one POST, not the rolling choice stated in the written baseline. |
| `SVC-ALL-005` | `PARTIAL` | Cluster client-config download exists, but the legacy outer-menu permission quirk, popup-blocked behavior, and browser acceptance remain unverified. |
| `SVC-ACT-001` | `PARTIAL` | Start, Stop, and Restart are present, but action eligibility, upgrade/wizard exclusion, rolling scheduling, and state recovery do not fully match classic. |
| `SVC-ACT-002` | `PARTIAL` | Service Check is present, but permission gating, stack availability, result presentation, and failure/retry behavior are incomplete. |
| `SVC-ACT-003` | `PARTIAL` | Service maintenance is present, but component/alert effects and state convergence need validation. |
| `SVC-ACT-004` | `PARTIAL` | Restart-required component flows exist, but stale filtering, parameter selection, scheduling, and progress coverage are incomplete. |
| `SVC-ACT-005` | `PARTIAL` | Delete Service exists, but the feature flag, stopped/dependency/last-service checks, wizard exclusion, and complete confirmation contract are not aligned. |
| `SVC-ACT-006` | `PARTIAL` | YARN queue refresh exists but is exposed through overly broad authorization and needs command-availability and progress validation. |
| `SVC-ACT-007` | `PARTIAL` | HDFS rebalance exists but is exposed through overly broad authorization; NameNode/DataNode eligibility, threshold validation, progress, and recovery are incomplete. |
| `SVC-ACT-008` | `PARTIAL` | Some service-specific commands exist, but stack command coverage, state conditions, and semantic permission gates are incomplete. |
| `SVC-ACT-009` | `PARTIAL` | Dynamic custom commands exist, but command metadata scopes, parameters, authorization, progress, and recovery require alignment. |
| `SVC-ACT-010` | `PARTIAL` | HA, federation, JournalNode, standby, and move-master entries exist selectively; permissions are too broad and service/component/feature conditions remain incomplete. |
| `SVC-ACT-011` | `PARTIAL` | Downloads exist, but current-service all-client download incorrectly uses `SERVICE_COMPONENT`; it must use `SERVICE`, reserving `SERVICE_COMPONENT` for a specified client component. |

### Non-Metrics Summary and Quick Links

| ID | Initial status | React evidence and gap |
| --- | --- | --- |
| `SVC-SUM-001` | `PARTIAL` | Summary renders service/component/state/alert information, but service-specific layouts, maintenance, restart-required, and host distribution parity are incomplete. |
| `SVC-SUM-002` | `PARTIAL` | Host/component navigation exists in places, but return-path and Hosts-filter semantics cross the Module 03 ownership boundary and require integration after that module lands. |
| `SVC-SUM-003` | `MISSING` | React has no complete individual host-component Start, Stop, Restart, Maintenance, or custom-command action set in Service Summary. |
| `SVC-SUM-004` | `MISSING` | Flume agent Start/Stop by host and handler is absent. |
| `SVC-SUM-005` | `PARTIAL` | External Quick Links are rendered, but descriptor/config/host resolution is incomplete and the separate Ambari View route/iframe behavior is not reconciled. |
| `SVC-QL-001` | `PARTIAL` | Visible-link and empty/error handling exists, but descriptor loading has a malformed URL and removal/component-existence semantics need focused coverage. |
| `SVC-QL-002` | `PARTIAL` | Protocol checks are partially implemented; exact `EXIST`, `NOT_EXIST`, fixed policy, reverse-protocol, and missing-property behavior need deterministic tests. |
| `SVC-QL-003` | `PARTIAL` | Component hosts are loaded, but internal-to-public-host mapping and all single/multi-host/nameservice grouping cases are incomplete. |
| `SVC-QL-004` | `PARTIAL` | Port/default resolution exists, but login substitution is always empty and `${config-type/property-name}` substitution is missing. |
| `SVC-QL-005` | `PARTIAL` | Ranger handling is partial; MapReduce configured-host reverse/public-host resolution is incomplete and Oozie hosts are not restricted to `STARTED`. |
| `SVC-QL-006` | `PARTIAL` | Active/Standby grouping is present in part, but HDFS/YARN/HBase operational selection and required non-display active-master field need validation. |
| `SVC-QL-007` | `STATICALLY_ALIGNED` | External anchors use `_blank` with `noreferrer`; runtime validation must confirm every rendering branch and generated URL. |

### Service Configs and Config Groups

| ID | Initial status | React evidence and gap |
| --- | --- | --- |
| `SVC-CONFIG-001` | `PARTIAL` | Theme/category controls exist, but control types, missing-theme fallback, tabs/sections, and stack metadata coverage require executable validation. |
| `SVC-CONFIG-002` | `PARTIAL` | Values, metadata, descriptions, units, and validation are rendered in part; recommended/default/final/read-only semantics are not fully covered. |
| `SVC-CONFIG-003` | `PARTIAL` | Editing, validation, recommendations, dependencies, undo, and final flags exist unevenly across traditional rows and widgets; permission and error blocking require alignment. |
| `SVC-CONFIG-004` | `INCORRECT` | Default-group saves include only changed properties even though desired-config replacement requires all properties for each changed type. Save promises are not awaited, so `saveInProgress`, success, refresh, and dependent-service failures race. |
| `SVC-CONFIG-005` | `INCORRECT` | The unsaved-navigation prompt exists, but Save can fail to call `blocker.proceed()` when validation has no warnings, and refresh relies on a fixed one-second delay instead of save completion. |
| `SVC-CONFIG-006` | `PARTIAL` | History, version selection, and comparison exist, but Compare lacks `SERVICE.COMPARE_CONFIGS`; old-version read-only and group/version selection need validation. |
| `SVC-CONFIG-007` | `INCORRECT` | Make Current sends the required cluster PUT, but lacks `SERVICE.MODIFY_CONFIGS`; its follow-up current-version GET is malformed and uses a hard-coded service set instead of installed services. |
| `SVC-CONFIG-008` | `PARTIAL` | Override controls exist, but empty override values are lost through truthy fallback and widget/row recommended/final/restore behavior needs coverage. |
| `SVC-CONFIG-009` | `PARTIAL` | Restart-required actions exist but permission, maintenance/state filters, host/component grouping, rolling parameters, and progress are incomplete. |
| `SVC-CONFIG-010` | `INCORRECT` | DB custom-action create, task-list, and polling failures can leave the widget in Connecting. KDC uses `finally`, but the DB path does not expose complete failure recovery. |
| `SVC-CONFIG-011` | `PARTIAL` | Custom property controls exist, but category eligibility, bulk parsing, duplicate/key validation, persisted deletion, permission, and save integration need focused tests. |
| `CFG-GROUP-001` | `PARTIAL` | Group listing exists, but management is gated by `SERVICE.MODIFY_CONFIGS` instead of `SERVICE.MANAGE_CONFIG_GROUPS`. |
| `CFG-GROUP-002` | `PARTIAL` | Create/name/description/host selection exists; one-non-default-group-per-service enforcement and server failure recovery require alignment. |
| `CFG-GROUP-003` | `PARTIAL` | Rename/description/copy UI exists, but duplicate does not copy desired configs and name uniqueness needs validation. Classic intentionally leaves the copied group's host list empty. |
| `CFG-GROUP-004` | `PARTIAL` | Host add/remove exists, but the selection UI cannot move a host directly from another non-default group and route/current-group refresh is incomplete. |
| `CFG-GROUP-005` | `PARTIAL` | Delete exists; default-group protection, host return-to-default, selection refresh, and recoverable failure need validation. |
| `CFG-GROUP-006` | `PARTIAL` | Group properties and edit entry exist, but preselected group/version routing and historical selection need runtime validation. |

### Add Service and Reassign Master

| ID | Initial status | React evidence and gap |
| --- | --- | --- |
| `SVC-ADD-001` | `PARTIAL` | Service choice exists; installability, dependencies, conflicts, validation, feature flag, and mutual exclusion require alignment. |
| `SVC-ADD-002` | `PARTIAL` | Master assignment exists; cardinality, resources, existing components, ineligible hosts, and recovery need validation. |
| `SVC-ADD-003` | `PARTIAL` | Slave/client assignment exists; retained installed components and host eligibility need validation. |
| `SVC-ADD-004` | `PARTIAL` | Config customization exists; recommendation, credential/database/account tabs, dependency handling, and failure recovery are incomplete. |
| `SVC-ADD-005` | `PARTIAL` | Review exists; all component/config changes and no-mutation behavior need executable acceptance evidence. |
| `SVC-ADD-006` | `PARTIAL` | Deploy executes installation stages, but exact request ordering/payloads, route lock, progress, retry, and partial-failure recovery require validation. |
| `SVC-ADD-007` | `INCORRECT` | Completion always writes `Clusters.provisioning_state=INSTALLED`; Add Service must only refresh the existing cluster and close. |
| `SVC-ADD-008` | `PARTIAL` | Step persistence exists, but another user's read-only mode, mutual exclusion, close cleanup, and correct Add Service context ownership are incomplete. |
| `SVC-MOVE-001` | `INCORRECT` | `ValidateMove` hard-codes `canStartMove=true` and direct routes bypass classic's host-count, stack reassignability, installed-component, and available-target checks. Classic performs these checks from loaded topology; it does not call a separate validation API. |
| `SVC-MOVE-002` | `PARTIAL` | Review exists, but source/target, affected config/service, recommendation, and downtime-warning completeness need validation. |
| `SVC-MOVE-003` | `PARTIAL` | Configure Component contains extensive mutation logic; exact ordering, payloads, idempotency, and partial-failure recovery need executable validation. |
| `SVC-MOVE-004` | `PARTIAL` | Conditional manual-command steps exist, but component-specific commands, confirmations, route persistence, and resume behavior need validation. |
| `SVC-MOVE-005` | `PARTIAL` | Start/Test flows exist, but affected-service selection, service checks, task summaries, retry, and failure recovery are incomplete. |
| `SVC-MOVE-006` | `MISSING` | React lacks classic's narrow rollback path for a failed database connection task. Classic step 7 removes the failed target component, reconfigures the database service, and restarts services; it is not a general restore of every component move. Module 04 implements this narrow compatibility path rather than inventing a general rollback contract. |

## React File Inventory

The primary Module 04 implementation surface is:

- Service shell and actions: `screens/Services/ServiceLoader.tsx`, `ServiceDashboard.tsx`, `ServiceSummary.tsx`, `ServiceComponents.tsx`, `Actions.tsx`, `ServiceActionsUrlMapping.tsx`, and `ComponentActionsMapping.tsx`.
- Quick Links: `screens/Services/ServiceQuicklinks.tsx`, `OptimizedServiceQuicklinks.tsx`, `hooks/useLazyQuicklinks.ts`, and `api/quicklinksApi.ts`.
- Service Configs: `screens/ServiceConfigs/index.tsx`, `screens/CommonConfigs/*`, `screens/ConfigVersions/*`, `hooks/useConfigSaver.tsx`, `hooks/useConfigs.ts`, `hooks/useConfigsTags.ts`, `hooks/useEnhancedConfigs.ts`, `api/configsApi.ts`, and `api/serviceConfigApi.ts`.
- Config Groups: `screens/ConfigGroups/*`, `Utils/configGroupUtils.ts`, and `api/configGroupApi.ts`.
- Add Service: `screens/Services/AddServiceWizard/*`, `screens/Services/AddWizardUrlMapping.tsx`, and shared Cluster Wizard step components.
- Reassign Master: `screens/Services/reassign/*`, move initializers, and `api/serviceApi.ts`.
- Shared state, routes, permissions, and requests: `store/ServiceContext.tsx`, application routing, `api/servicesApi.ts`, `api/actionsApi.ts`, and permission utilities.

Files under `screens/Hosts`, including `HostConfigs.tsx`, `actions.tsx`, `supportClientConfigsDownload.ts`, and Add Host wizard state, are owned by Module 03 and are not changed by Module 04.

## Backend Contract Comparison

| Ember contract | Initial React difference | Required Module 04 behavior |
| --- | --- | --- |
| `PUT /api/v1/clusters/{cluster}/services?{urlParams}` | React uses the same broad state mutation as classic, but lacks the surrounding confirmation and recovery lifecycle | Preserve the broad classic request with `ServiceInfo.state`, request context, optional operator query, progress tracking, and retry after rejection |
| `PUT /api/v1/clusters/{cluster}/services/{service}` | State, maintenance, and request-context handling varies by action | Preserve exact desired-state or maintenance payload and response progress semantics |
| `POST /api/v1/clusters/{cluster}/requests` | Custom commands and service checks share incomplete authorization/error paths | Preserve command name, resource filters, parameters, request context, and async task ownership |
| `POST /api/v1/clusters/{cluster}/request_schedules` | Per-service immediate versus rolling/scheduled restart selection is incomplete | Preserve ordered batch requests, interval, task tolerance, and request-schedule response type for per-service Summary/Configs flows |
| `PUT /api/v1/clusters/{cluster}` with `Clusters.desired_config` | Save emits only changed properties for default groups and does not await requests | Send all properties and attributes for every changed config type; await every current/dependent service save |
| `GET /api/v1/clusters/{cluster}/configurations/service_config_versions` | History reads exist, but permission/selection/revert semantics are incomplete | Preserve service/group/version filters and treat old versions as immutable history |
| Create a new desired config from a historical version | `setIsCurrent()` performs a malformed GET | Build and PUT a new desired config version with an audit note; never mutate the historical row |
| Config group CRUD at `/api/v1/clusters/{cluster}/config_groups` | CRUD URLs are broadly correct; duplicate desired configs and cross-group host movement are incomplete | Copy desired configs but leave duplicate hosts empty, clear hosts from their previous non-default group before assigning the target group, and refresh selection |
| `GET /api/v1/stacks/{stack}/versions/{version}/services/{service}/quicklinks` | `QuicklinksApi.getQuicklinks()` appends a stray apostrophe | Use the exact descriptor URL and preserve descriptor-empty error handling |
| `GET /api/v1/clusters/{cluster}/hosts?Hosts/host_name.in({hosts})&fields=Hosts/public_host_name{urlParams}&minimal_response=true` | Public-host mapping request is absent | Map internal component hosts to public host names before generating external URLs |
| Grouped multi-version configuration predicate containing `%26` | React URL construction must be verified | Encode the grouped `&` as `%26`, matching Ambari predicate parsing |
| `GET .../services/{service}/components?...` | `getAllServiceComponentsListAndInitialMetrics()` has a trailing backtick | Remove malformed URL text; Module 04 uses only non-Metrics fields |
| `POST /api/v1/clusters/{cluster}/services?ServiceInfo/service_name={service}` | `createComponent()` has the correct collection URL and body but omits `method`, so it can default to GET | Send POST with the component body used by classic `common.create_component` |
| `GET /api/v1/clusters/{cluster}?fields=Clusters/desired_configs...` | `ConfigsApi.getDesiredConfigsInfo()` has a trailing backtick | Use the exact desired-config query URL |
| `GET ...?format=client_config_tar` with `HostRoles/component_name={scope}` | Current-service all-client download uses component scope | Use `CLUSTER` for all services, `SERVICE` for all clients of one service, and `SERVICE_COMPONENT` for one client component |

The default-group save contract is replacement-oriented: once a config type is changed, its submitted `properties` object must contain every property in that type, including unchanged values. `properties_attributes` must preserve final and related attributes. Non-default config-group saves retain the classic parallel, non-atomic server behavior, but the React lifecycle must await all started promises and report every failure deterministically.

### Implemented Request Corrections

| Flow | Method and exact resource | Request/query/payload correction |
| --- | --- | --- |
| Compare two versions | `GET /api/v1/clusters/{cluster}/configurations/service_config_versions?(service_name={service}%26service_config_version.in({v1},{v2}))` | Encodes the grouped predicate separator as `%26` instead of sending a raw `&`. |
| Quick Link descriptor | `GET /api/v1/stacks/{stack}/versions/{version}/services/{service}/quicklinks?QuickLinkInfo/default=true&fields=*&_={timestamp}` | Removes the trailing apostrophe that changed the descriptor resource. |
| Quick Link public hosts | `GET /api/v1/clusters/{cluster}/hosts?Hosts/host_name.in({encodedHosts})&fields=Hosts/public_host_name&minimal_response=true` | URL-encodes each hostname and maps `Hosts.host_name` to `Hosts.public_host_name`. |
| Component topology | `GET /api/v1/clusters/{cluster}/components?fields={nonMetricsFields}&_={timestamp}` | Removes the trailing backtick and uses only service/component, hostname, and state fields for this module. |
| Create service component | `POST /api/v1/clusters/{cluster}/services?ServiceInfo/service_name={service}` | Adds the missing POST method and sends `components[].ServiceComponentInfo.component_name`. |
| Desired config refresh | `GET /api/v1/clusters/{cluster}?fields=Clusters/desired_configs&_={timestamp}` | Removes the trailing backtick. |
| Default config save | `PUT /api/v1/clusters/{cluster}` | Sends `Clusters.desired_config[]` with the complete `properties` replacement and every non-empty `properties_attributes` category for each changed type. |
| Make Current | `PUT /api/v1/clusters/{cluster}` followed by current-version GET | Sends `Clusters.desired_service_config_versions` with service, selected version, and note; refreshes the actual installed-service list through a corrected current-version URL. |
| Config Group membership | `PUT /api/v1/clusters/{cluster}/config_groups/{id}` | Phase 1 sends `ConfigGroup.hosts: []` for every membership-changed/deleted group; phase 2 writes final memberships before creates, preventing cross-group swap conflicts. |
| Config Group delete | `PUT .../config_groups/{id}` then `DELETE .../config_groups/{id}` | Clears hosts before DELETE so they return to Default and the operation remains retryable after a failed stage. |
| Start/Stop All | `PUT /api/v1/clusters/{cluster}/services?` | Preserves classic's broad resource with `RequestInfo.context`, cluster operation level, and `Body.ServiceInfo.state` (`STARTED` or `INSTALLED`). |
| Restart All Required | `POST /api/v1/clusters/{cluster}/requests` | Sends one Classic-compatible request with `RequestInfo.command=RESTART`, context `Restart all required services`, operation level `host_component`, and `Requests/resource_filters[0].hosts_predicate=HostRoles/stale_configs=true&HostRoles/cluster_name={cluster}`. |
| Service maintenance | `PUT /api/v1/clusters/{cluster}/services/{service}` | Sends `RequestInfo.context` and `Body.ServiceInfo.maintenance_state` (`ON` or `OFF`), then converges local state only after success. |
| Flume agent state | `PUT /api/v1/clusters/{cluster}/hosts/{host}/host_components/FLUME_HANDLER` | Sends `RequestInfo.flume_handler={agent}`, a `HOST_COMPONENT` operation level for FLUME and the target host, and `Body.HostRoles.state` (`STARTED` or `INSTALLED`). |
| Test Connection | `POST /api/v1/clusters/{cluster}/requests` or the uninstalled-service custom-action resource | Preserves `RequestInfo.parameters` and `Requests/resource_filters[].hosts`; task-list and task-detail IDs are awaited and non-zero `structured_out.db_connection_check.exit_code` is failure. |
| Oozie manual-step eligibility | `GET /api/v1/clusters/{cluster}/configurations/service_config_versions?service_name.in(OOZIE)&is_current=true&fields=*` | Reads the current `oozie-site/oozie.service.JPAService.jdbc.driver`; only Derby uses the embedded-database manual copy steps, while load failure blocks entry and exposes Retry. |
| Reassign DB rollback | Host-component maintenance PUT, target component DELETE, MySQL clean/configure commands, then service start | Uses the persisted source/target and affected component list; no separate validation or universal rollback API is introduced. |
| Client download scope | Browser GET ending in `?format=client_config_tar` | Uses `CLUSTER` for all cluster clients, `SERVICE` for one service's clients, and `SERVICE_COMPONENT` only for one named client component. |

## Permissions, Feature Flags, and Routes

| Entry or operation | Required semantic gate | Initial React difference |
| --- | --- | --- |
| Service Configs tab | `CLUSTER.VIEW_CONFIGS` and a configurable service | React additionally hides Configs during upgrades; classic does not |
| Add/Delete Service | `SERVICE.ADD_DELETE_SERVICES` and `supports.enableAddDeleteServices` | Feature flag, route guard, and complete wizard/upgrade ownership checks are missing |
| Start/Stop/Restart | `SERVICE.START_STOP` plus state, maintenance, upgrade, and wizard conditions | The broad action menu gate does not enforce every per-action condition |
| Service Check | `SERVICE.RUN_SERVICE_CHECK` | Current authorization is too broad |
| Custom command | `SERVICE.RUN_CUSTOM_COMMAND` | Current authorization is too broad for metadata-driven commands |
| Maintenance | `SERVICE.TOGGLE_MAINTENANCE` | State and affected-component/alert convergence are incomplete |
| HA/Federation | `SERVICE.ENABLE_HA` plus stack/service/component conditions | Entries use incomplete semantic permissions and availability conditions |
| Move master | `SERVICE.MOVE` plus component conditions | Route and validator must reject unauthorized or invalid direct navigation |
| Manage JournalNodes | Its classic semantic permission and feature/service conditions | Current entry inherits a broader action permission |
| Compare Configs | `SERVICE.COMPARE_CONFIGS` | Gate is missing |
| Modify/Make Current | `SERVICE.MODIFY_CONFIGS` | Make Current gate is missing; management currently conflates this with config groups |
| Manage Config Groups | `SERVICE.MANAGE_CONFIG_GROUPS` | React uses `SERVICE.MODIFY_CONFIGS` |
| Client config download | `CLUSTER.VIEW_CONFIGS`, scope-specific availability, and the legacy outer-menu gate | Scope and some menu conditions differ |

Direct route entry must enforce the same permissions, feature flags, service/component availability, wizard ownership, and upgrade conditions as its menu item. Hiding a menu entry alone is not authorization parity. The classic placeholder Audit route is not a React acceptance requirement.

### Implemented Permission And Route Results

| Area | Final React result | Remaining acceptance |
| --- | --- | --- |
| Configs tab | Requires `CLUSTER.VIEW_CONFIGS` and remains visible during upgrades. | Verify each configurable/non-configurable service live. |
| Add/Delete Service | Requires `SERVICE.ADD_DELETE_SERVICES`, `enableAddDeleteServices`, and no conflicting wizard/upgrade; Add Service direct routes apply all three gates. | Other-user read-only presentation and full wizard recovery remain partial. |
| Service Check | Requires `SERVICE.RUN_SERVICE_CHECK`. | Stack support and result/retry UI remain partial. |
| HDFS/YARN/custom commands | Require `SERVICE.RUN_CUSTOM_COMMAND`. | Exhaustive stack metadata and state predicates remain partial. |
| Restart entries | Require `SERVICE.START_STOP`. | Rolling/scheduled parameter acceptance remains partial. |
| Service maintenance | Requires `SERVICE.TOGGLE_MAINTENANCE`; success updates the selected service state and failure exposes Retry. | Component and alert convergence requires live acceptance. |
| Flume handlers | Require `SERVICE.START_STOP`, valid handler state, and no active wizard owned by the current flow. | Verify server-side role rejection and realtime/poll convergence live. |
| HA/Federation/Journal | Menu and direct routes require `SERVICE.ENABLE_HA` and the shared operation guard. | Every stack/service/component feature condition remains partial. |
| Move master | Menu and direct routes require `SERVICE.MOVE`; topology validation rejects invalid direct entry. | Component-specific review/manual-step coverage remains partial. |
| Compare/Make Current | Require `SERVICE.COMPARE_CONFIGS` and `SERVICE.MODIFY_CONFIGS`, respectively. | Non-default historical selection requires live acceptance. |
| Config Groups | Requires `SERVICE.MANAGE_CONFIG_GROUPS`. | 403/409/500 server responses require live acceptance. |

## Async Lifecycle, Recovery, and Persistence

| Flow | Initial defect | Required convergence and recovery |
| --- | --- | --- |
| Service mutations | Batch filters and confirmation/retry are incomplete | Lock duplicate submission, show progress for real request IDs, refresh service/component state after success, and expose retry after failure |
| Service/config realtime | App-level `/events/configs` subscription exists but Service Configs and Quick Links do not consume it | Refresh or invalidate relevant current configs and generated links without parallel polling or stale overwrite |
| Default config save | Save promises are launched but not returned/awaited | Keep saving state until every request settles; refresh cluster/configs/Quick Links only after success; retain edits and expose errors after failure |
| Dependent/non-default save | Parallel server operations can partially succeed | Report per-service failures deterministically, refresh confirmed successes, and never announce full success before all promises settle |
| Unsaved route blocker | Save can leave navigation blocked; refresh waits a fixed one second | Proceed only after a successful awaited save; Discard proceeds immediately; Cancel keeps the user and edits; failed save remains blocked and recoverable |
| Test Connection | Create/task/poll rejection can remain Connecting | End Connecting for create, task-list, and poll errors; display recoverable failure details; cancel timers and stale updates on unmount/retry |
| Quick Links | Manual refresh and stale configs can leave old URLs | Handle load errors explicitly, allow Retry, refresh on current-config change/event, and never construct a URL without a valid descriptor/host |
| Add Service | Step persistence exists, but ownership/cleanup differs | Restore persisted step for the owner; other users remain read-only; close invokes Add Service cleanup; completion refreshes services without changing provisioning state |
| Reassign Master | Persistence exists, validation/rollback do not | Persist source/target and step, validate before mutations, resume deterministically, stop on failures, and offer component-specific rollback |

### Implemented Lifecycle Results

| Flow | Final behavior | Evidence or residual risk |
| --- | --- | --- |
| Default config save | Owns and awaits the replacement PUT, holds busy state through settlement, refreshes only after success, and returns failure without navigating. | Focused hook tests cover pending and rejected requests; dependent multi-service partial success still needs live acceptance. |
| Unsaved navigation | Save proceeds only after successful settlement; failed Save remains blocked, while existing Discard/Cancel semantics remain available. | TypeScript and focused save tests pass; browser Back/Forward remains live-only. |
| Test Connection | Create, task-list, poll, missing status/IDs, terminal failure, and non-zero DB check exit all stop polling and enable Retry. | Focused component tests cover create rejection, poll rejection, and completed-task DB failure. |
| Quick Links | Correctly resolves user/config placeholders, component/public hosts, MapReduce host overrides, and Oozie running hosts; topology-read failure falls back to legacy single-link reconstruction. | Helper/API tests pass; live descriptor and config-event refresh remain. |
| Config Groups | Local moves are atomic; server saves clear all changed memberships before set/create; errors remain visible and Save is retryable. | Pure planning/state tests cover swaps, empty membership, moves, and delete-to-Default. |
| Add Service | Cancel invalidates sequence/debounce in the Add Service provider; completion clears persisted state without changing cluster provisioning state. | Static/build evidence passes; reload, second-user, and partial deploy remain live-only. |
| Reassign | Entry waits for App and service topology, validates classic predicates, awaits DB polling, and exposes the narrow rollback with idempotent target deletion. | Validation/rollback helpers are tested; live multi-stage failure and resume remain. |
| Service navigation | Route params are the source of truth; Back/Forward, invalid services, unsupported tabs, and unauthorized Configs tabs converge with replacement navigation, while embedded wizard dashboards retain their routes. | Pure navigation tests pass; service create/delete event convergence remains live-only. |
| Flume handlers | State-valid commands lock per agent, use the returned request ID for progress, refresh topology on completion, and expose Retry after submission or terminal request failure. | Helper, component, and exact payload tests pass; live handler state convergence remains pending. |
| Service maintenance | Success updates the local service state only after the PUT settles; rejection keeps the prior state and offers Retry with server details. | Exact payload evidence passes; service/component/alert realtime convergence remains live-only. |
| Custom properties | Bulk parsing preserves value text after the first `=`, reports source line numbers, validates trimmed keys, permits re-adding removed keys, and excludes persisted deletions from full replacement payloads. | Parser and config-saver tests pass; exhaustive widget/category rendering remains live-only. |
| Reassign manual steps | Oozie config loading owns its failure state and Retry; one eligibility decision controls both wizard steps and configure-task filtering. | Derby, external Oozie DB, MySQL, load failure, and task-filter helpers are covered; reload/resume remains live-only. |

## Five Independent Audits

| Pass | Independent entry | Initial findings | Implementation acceptance |
| --- | --- | --- | --- |
| 1. Features and state semantics | All 60 baseline IDs, classic controllers/templates/routes, and React components/hooks | Similar page names conceal missing host-component actions, Flume actions, rollback, Start/Stop All confirmation and recovery, full config replacement, public-host links, and wizard ownership | Every ID has an explicit post-implementation status and focused evidence or a documented boundary |
| 2. Backend APIs and payloads | AJAX definitions/calls, direct HTTP, browser entries, API classes, and mutation call sites | Stray URL characters, missing POST, wrong download scope, malformed Make Current, incomplete save properties, and missing public-host mapping | URL/method/query/payload tests assert exact requests and failure propagation |
| 3. Permissions, feature flags, and routes | Classic permission/flag docs, route guards, menus, and direct React routes | Configs is wrongly upgrade-hidden; Add/Delete flags and direct-route guards are incomplete; Compare, Make Current, Config Groups, checks, commands, and wizards use incorrect gates | Role/flag/upgrade/wizard matrices cover both visible entry and direct navigation |
| 4. Async lifecycle, polling, recovery, and persistence | Promise ownership, timers, realtime, blockers, wizard stores, and partial failures | Saves are not awaited, Test Connection can remain Connecting, config events do not converge consumers, Add Service cleanup is misowned, and Reassign lacks rollback | Focused tests cover rejection, retry, unmount, partial success, navigation continuation, and persisted resume |
| 5. Tests and executable acceptance evidence | Existing test inventory, Vitest configuration, build, lint, and runtime scenarios | The base has no focused Module 04 Vitest suite; static reading cannot validate live Server behavior | Focused Vitest, full Vitest, TypeScript/Vite build, ESLint, `git diff --check`, and the runtime matrix are all recorded honestly |

## Compatibility Decisions

| Classic behavior or defect | React decision |
| --- | --- |
| Configs remains visible during an upgrade when the user can view configs | Preserve classic visibility; only mutations are disabled by their own upgrade/wizard conditions |
| A default desired-config PUT replaces a config type | Always send the complete property set and attributes for each changed type, even when only one value changed |
| Classic non-default/dependent saves are parallel and non-atomic | Do not claim atomicity; await every request and surface partial failures without clearing unsaved state prematurely |
| Empty string is a valid override | Use nullish/explicit override selection, never truthy fallback |
| Making a historical version current creates history | Create a new desired config version; never mutate or relabel the selected historical record |
| Browser client-config and Quick Link navigation bypasses application HTTP handling | Keep direct browser navigation and secure external anchors; avoid inventing in-app retry for a browser download whose response cannot be observed |
| Classic Quick Links substitutes public hosts and descriptor placeholders | Preserve the stack-driven algorithm rather than hard-coding service URLs |
| Classic Quick Links can consume the HBase active-master field | Retain only the operational field needed for safe link selection; do not add Metrics display or analysis |
| A missing Quick Link descriptor protocol policy falls back to `hdfs-site/dfs.http.policy`, including services such as HBase | Preserve the shared Classic fallback; do not force HTTP from a service-name capability table |
| The written baseline assigns global rolling/scheduled behavior to Restart All Required | Follow executable Classic behavior: send one confirmed `restart.staleConfigs` POST globally; retain rolling/scheduled choices only on per-service Summary/Configs actions |
| The all-services menu opens only for `SERVICE.START_STOP` or `SERVICE.ADD_DELETE_SERVICES`, even though Download and Run All Service Check have their own inner permissions | Preserve the outer-menu quirk for compatibility; the inner entries retain `CLUSTER.VIEW_CONFIGS` and `SERVICE.RUN_SERVICE_CHECK` respectively |
| Add Service completion does not set cluster provisioning state | Remove the Installer-only mutation from Add Service completion |
| Classic Add Service deploy route cannot be left through normal wizard navigation | Preserve route ownership and persisted recovery while still allowing deliberate cleanup on completion/cancel |
| Classic Reassign validates component-specific placement and has a narrow DB-test rollback | Do not enable mutation until topology validation passes; after DB-test failure only, remove target components, reconfigure the database service, and restart affected services. |
| Oozie Reassign shows embedded database copy commands only for Derby | Read current Oozie config before building the wizard; external databases omit those steps and a failed config read blocks entry with Retry |
| A malformed classic response can leave some polling views busy | React must terminate busy state and provide retry for every create/read/poll failure |

## Cross-Module Boundaries

Module 04 currently imports Module 03-owned helpers from `screens/Hosts` for host-component actions, client-config download, and Reassign Step 5. This branch does not modify those files. The following integration work must wait for or be reconciled with Module 03:

- `SVC-SUM-002`: exact Hosts filter, target route, and return-path behavior.
- `SVC-SUM-003`: shared host-component action eligibility and payload helpers if Module 03 changes their contract.
- `SVC-ACT-011`: shared browser download helper changes, if any, must be integrated after Module 03 rather than edited concurrently.
- Reassign Step 5: any required changes to imported Hosts action utilities remain a boundary; Module 04 can adapt its own caller only.
- `AddWizardUrlMapping.tsx` currently mixes Add Service and Add Host cleanup contexts. Module 04 may isolate Add Service ownership without changing Add Host state or behavior; any shared restructuring requires later integration.

No Module 04 implementation may depend on uncommitted Module 03 behavior. Boundaries remain `CROSS_MODULE_BOUNDARY` until integrated and accepted.

## Runtime Acceptance Matrix

Static and unit evidence is insufficient to mark these features covered. At minimum, execute these scenarios against representative Ambari Server stacks:

| Area | Automated status | Live Ambari Server status |
| --- | --- | --- |
| Exact API URL/method/payload corrections | Covered by focused Vitest for corrected API helpers and config replacement payloads | `PENDING` |
| Permissions, feature flag, and operation route guard | Covered by policy and route-guard Vitest plus static route review | `PENDING` |
| Config save and Test Connection recovery | Covered by focused hook/component Vitest | `PENDING` |
| Quick Link substitution and host mapping | Covered by focused helper/API Vitest | `PENDING` |
| Navigation, protocol policy, custom properties, and config-history selection | Covered by focused pure-function and config-saver Vitest | `PENDING` |
| Global stale restart, service maintenance, and Flume handlers | Covered by exact payload plus Flume helper/component Vitest | `PENDING` |
| Config Group ordering and local membership persistence | Covered by focused pure-function Vitest | `PENDING` |
| Add Service and Reassign multi-stage workflows | Static TypeScript/build coverage plus focused persistence/validation tests | `PENDING` |
| Hosts-owned integration scenarios | Not implemented in this branch by ownership rule | `BLOCKED_ON_MODULE_03` |
| Metrics | Excluded from Module 04 | `NOT_APPLICABLE` |

1. Navigate installed services with healthy, maintenance, restart-required, alerting, missing, removed, and newly event-created service states; exercise Back/Forward and invalid service IDs.
2. Run Start/Stop All with mixed health states; confirm classic-compatible button eligibility, broad PUT resources, impact/checkpoint confirmation, 202 progress, rejection, Retry, and event/REST convergence.
3. Run global Restart All Required and verify its single stale-component POST; separately run per-service/component immediate and rolling/scheduled restart with mixed stale/state/maintenance hosts and validate interval, tolerance, ordering, and schedule IDs.
4. Exercise every service action as Cluster User, service operator, cluster administrator, and Ambari administrator during normal, upgrade, and active-wizard states; try direct routes as well as menu entries.
5. Delete a stopped eligible service and reject running, dependency-blocked, last-service, feature-disabled, unauthorized, and server-failed cases.
6. Run YARN refresh, HDFS rebalance with valid/invalid thresholds, service checks, custom commands, maintenance, HA/Federation, JournalNode management, and move-master entries on supported and unsupported stacks.
7. Download all cluster clients, all clients for one service, and one client component; inspect `CLUSTER`, `SERVICE`, and `SERVICE_COMPONENT` query scopes and popup-blocked behavior.
8. Render non-Metrics summaries for every installed service, navigate component/host links, exercise Flume handler actions, and defer individual host-component action integration to Module 03.
9. Load Quick Links with HTTP-only, HTTPS-only, existence, non-existence, exact-value, missing-property, default-port, regex-port, public-host, override-host, nameservice, Active/Standby, and empty/error descriptors.
10. Validate Ranger external URL, MapReduce configured-host reverse mapping, Oozie STARTED filtering, login username, config placeholders, Retry, config-event refresh, external target, and `rel` attributes.
11. Load themed and non-themed configs across all non-Metrics control types; edit ordinary, widget, recommended, dependent, final, password, directory, database, custom, and bulk custom properties with errors and warnings.
12. Save one and multiple config types and dependent services; inspect complete properties/attributes, audit notes, versions, partial failures, retries, duplicate-submit locking, recommendation clearing, and config/Quick Link refresh timing.
13. Trigger unsaved Save/Discard/Cancel through service, group, history, browser Back/Forward, and wizard-related navigation; cover successful save, validation warning/error, HTTP failure, retry, and refresh.
14. Browse, compare, and make current default/non-default historical versions under every relevant permission; verify immutable history and the newly created current version.
15. Create and remove empty-string and non-empty overrides; restore saved/default values; set recommended/final independently in traditional rows and widgets.
16. Test Hive, Oozie, Ranger, and Kerberos connections through create failure, missing task ID, task-list failure, poll failure, server failure with stdout/stderr/check message, success, Retry, and unmount.
17. Create, rename, describe, copy, move hosts between, delete, and edit default/non-default config groups; verify unique names, copied configs with an empty copied host list, host return to default, current route, 403/409/500, and Retry.
18. Run all seven Add Service steps with dependencies/conflicts, partial deployment failures, Retry, refresh, close, resume after reload, a second user, upgrade, direct route, and feature-disabled state; verify provisioning state is never changed.
19. Run Reassign for every supported component with valid/invalid target hosts, dependency conflicts, manual commands, reload/resume, request failures, checks, and component-specific rollback.
20. Leave and re-enter Service Configs and Quick Links during slow requests, config events, service events, reconnect, Retry, logout, and unmount; verify no stale overwrite, duplicate mutation, timer, or subscription remains.

## Verification Commands

Final local evidence from `ambari-web/latest`:

| Check | Result |
| --- | --- |
| Focused Vitest: `npx vitest run src/Utils/configGroupSavePlan.test.ts src/Utils/customConfigProperties.test.ts src/Utils/flumeAgents.test.ts src/Utils/quicklinks.test.ts src/Utils/reassignManualCommands.test.ts src/Utils/reassignValidation.test.ts src/Utils/serviceGlobalActions.test.ts src/Utils/serviceNavigation.test.ts src/Utils/servicesConfigsPolicy.test.ts src/Utils/sslProtocolUtils.test.ts src/api/servicesConfigsApi.test.ts src/components/ServiceOperationRouteGuard.test.tsx src/hooks/useConfigSaver.test.tsx src/screens/CommonConfigs/TestConnection.test.tsx src/screens/Services/FlumeSummary.test.tsx` | `PASS`: 15 files, 50 tests |
| Full Vitest: `npm test` | `PASS`: 29 files, 113 tests |
| TypeScript/Vite: `npm run build` | `PASS`; existing Sass deprecations, duplicate `ServiceLoader` cases, existing `eval` notices, and chunk-size warnings remain |
| Full ESLint: `npx eslint . --format json` | `FAIL`: 5,776 errors and 455 warnings from repository-wide existing debt |
| New Module 04 files ESLint | `PASS`: 26 source and test files, 0 findings |
| ESLint findings mapped to lines added by this patch | `PASS`: 0 findings |
| `git diff --check` | `PASS` |

The build warnings and full-lint baseline are not caused by Module 04 and are not refactored in this branch. Live Ambari Server scenarios remain explicitly `PENDING`; automated evidence does not replace the runtime acceptance matrix. No JIRA issue, commit, push, or pull request has been created.
