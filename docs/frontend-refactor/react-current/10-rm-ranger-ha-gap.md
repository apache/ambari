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

# React ResourceManager and Ranger Admin HA Gap Analysis

## Scope and Audit Result

This document compares the executable Classic Ember implementation, its REST
contracts, and the current React implementation for Module 10. The audit covers
all 107 stable IDs in `10-rm-ranger-ha.md`. Metrics pages, charts, widgets, and
metric queries are excluded. CPU, memory, and disk fields used to choose an HA
host remain in scope because they are placement inputs rather than Metrics UI.

Status meanings:

| Status | Meaning |
| --- | --- |
| `MATCH` | React implements the reachable Classic behavior and contract. |
| `COMPATIBILITY_FIX` | React preserves the intended workflow while deliberately avoiding a verified unsafe or defective Classic behavior. |
| `PARTIAL` | The main path exists, but a condition, request, recovery path, or visible behavior remains incomplete. |
| `MISSING` | No usable React implementation exists. |
| `NOT_APPLICABLE` | Classic code is broken, unreachable, or not a valid migration target. |
| `NEEDS_RUNTIME_VALIDATION` | Static implementation and tests exist where practical, but a real Ambari cluster is required for acceptance. |

The normal RM HA and Ranger Admin HA paths have guarded entries, complete
four-step flows, exact configuration generation, fail-closed component
installation, ordered progress, durable request checkpoints, refresh recovery,
and explicit completion behavior. One cross-cutting recovery gap remains:
`OperationsProgress` cannot atomically couple an Ambari mutation with the later
request-ID checkpoint. If the server accepts a mutation but its response is
lost, or the browser terminates before the returned ID is persisted, the saved
`QUEUED` state cannot distinguish accepted from not submitted and may replay on
recovery. Closing that unknown-outcome window requires a server idempotency key
or reconciliation protocol. The real-cluster matrix also remains mandatory for
stack variants, Kerberos providers, external load balancers, interrupted
requests, and cross-user recovery.

## State Machines

### ResourceManager HA

| State | Forward condition and side effects | Back, exit, and recovery | React result |
| --- | --- | --- | --- |
| Entry | YARN, both workflow permissions, HA disabled, at least three hosts and ZooKeeper Servers, and a started current RM | Fresh invalid entry remains outside the wizard; a persisted deployment may resume | The menu preserves Classic's disabled versus hidden states. The direct route repeats authorization and business validation and aggregates all prerequisite failures. |
| Step 1 Get Started | Acknowledge Active/Standby semantics and downtime; clear stale downstream state | No Back; Cancel clears pre-change state only after `/persist` succeeds | The page, warning, invalidation, and serialized checkpoint precede navigation. |
| Step 2 Select Hosts | Load hosts/topology, obtain Advisor placement, retain one current RM, and choose one distinct non-maintenance additional RM | Back invalidates Review/deployment; load failure exposes Retry | Host order is memory descending, CPU descending, hostname ascending; large clusters use a ten-result typeahead. The complete assignment is persisted before Review. |
| Step 3 Review | Load exact desired tags and configs, call Advisor, generate read-only YARN/conditional HAWQ/proxyuser changes, then pass the KDC gate | Back invalidates deployment; every load/build/KDC failure remains visible | Missing or malformed tags/configs/recommendations fail closed with Retry instead of copying Classic's unreliable degradation path. |
| Step 4 Configure | Stop non-HDFS services, install RM, save YARN, optional HAWQ, HDFS, then start all services | No Back; Retry starts at the failed operation; critical exit retains owner/checkpoint | Request IDs and task state are persisted before polling or advancement. Completion is allowed after only the final Start failure, matching Classic RM behavior; cleanup failure blocks navigation. |

### Ranger Admin HA

| State | Forward condition and side effects | Back, exit, and recovery | React result |
| --- | --- | --- | --- |
| Entry | RANGER, both workflow permissions, one eligible current RA, HA not already enabled | A persisted Step 4 may resume even after the second RA exists | Fresh entry is disabled/hidden with Classic menu semantics, while direct-route recovery is owner-aware and does not incorrectly restart validation from a one-RA topology. |
| Step 1 Get Started | Enter a Classic-compatible `http`, `https`, or `ftp` load-balancer URL | No Back; Cancel clears pre-change state | The exact validator is retained. External reachability, TLS, health checks, and host separation remain operator responsibilities. |
| Step 2 Select Hosts | Shared placement must be fully loaded and valid; exactly one current RA and at least one additional RA on unique hosts | Back to Step 1; host/Advisor failures expose Retry | Add/remove cardinality, installed-master immutability, host sorting, maintenance validation, and the load-state barrier are enforced. |
| Step 3 Review | Preview the ten candidate properties for installed services from stack metadata | Back to Step 2; load failure exposes Retry | Categories are deduplicated and values are read-only. The Classic comma-operator category defect is not copied. |
| Step 4 Install, Start and Test | Stop all, install every additional RA, submit one array-body multi-config PUT, then start all with smoke-test policy | No Back; Retry is required after any failure; critical exit retains owner/checkpoint | Assignment and URL preflight are fail closed. Configs preserve full properties and attributes. Completion requires all four tasks and durable cleanup. |

## Feature ID Status

### Entry and Route Scope

| Feature ID | Status | React implementation and evidence |
| --- | --- | --- |
| RMHA-ENTRY-001 | `MATCH` | `Services/Actions.tsx` and `resourceManager/index.tsx` expose the YARN action only with `SERVICE.ENABLE_HA` and persisted-data authorization; `RoutesList.tsx` repeats both guards. |
| RMHA-ENTRY-002 | `MATCH` | `resourceManager/index.tsx` disables while topology loads or fails, on one host, missing RM, `INIT`, or `INSTALL_FAILED`, while a stopped installed RM remains clickable for the combined validation dialog. Covered by `resourceManager/index.test.tsx`. |
| RMHA-ENTRY-003 | `MATCH` | `getRmHaEnablementErrors` aggregates STARTED RM, three ZooKeeper Server, and three-host failures. `ValidateEnablement.tsx` displays all results and `rmHaUtils.test.ts` covers aggregation. |
| RMHA-ENTRY-004 | `MATCH` | Live component topology hides fresh entry when more than one RM exists; React exposes no misleading Disable action. Entry and route tests cover hidden and mapped-route states. |
| RMHA-ENTRY-005 | `MATCH` | `Hosts/utils.tsx#isMasterAddableOnlyOnHA` excludes `RESOURCEMANAGER` from ordinary Add Component; only this wizard installs it. |
| RAHA-ENTRY-001 | `MATCH` | `Services/Actions.tsx`, `rangerAdmin/index.tsx`, and the protected route require both workflow permissions before fresh entry. |
| RAHA-ENTRY-002 | `MATCH` | `evaluateRangerAdminEnablement` uses authoritative RA host names and states plus the loaded cluster host count. `rangerAdmin/index.tsx` preserves Classic single-host/missing/`INIT`/`INSTALL_FAILED` disabling and hides fresh entry after HA; `ValidateEnablement.tsx` repeats the same checks for a direct URL while bypassing fresh-topology validation only for recovery. API, menu, and direct-entry tests cover every state. |
| RAHA-ENTRY-003 | `MATCH` | Ranger validation intentionally does not add RM's STARTED, ZooKeeper-count, or three-host preconditions; it checks permissions and fresh-versus-recovery RA topology only. |
| RAHA-ENTRY-004 | `MATCH` | `Hosts/utils.tsx` excludes `RANGER_ADMIN` from ordinary Add Component, and the Ranger action has no Disable mode. |
| HA-STATIC-001 | `COMPATIBILITY_FIX` | `RoutesList.tsx` requires both permissions and `ServiceOperationRouteGuard` enforces active-wizard ownership; both `ValidateEnablement` components repeat workflow-specific validation. This closes Classic's login-only direct-route path. |

### ResourceManager Steps and Configuration

| Feature ID | Status | React implementation and evidence |
| --- | --- | --- |
| RMHA-1-001 | `MATCH` | `resourceManager/Step1.tsx` renders Active/Standby, automatic failover, maintenance-window, and downtime guidance with no user input. |
| RMHA-1-002 | `MATCH` | Step 1 invalidates Select Hosts, Review, and Configure snapshots, awaits the checkpoint, and then advances. Cancel persistence failure is visible and keeps the modal open; covered by `Step1.test.tsx`. |
| RMHA-2-001 | `MATCH` | `Step2.tsx` fixes the installed RM and provides exactly one additional-RM selector with Current/Additional labels. |
| RMHA-2-002 | `MATCH` | `rmHaApi.getHostRecommendations`, `buildHostRecommendationPayload`, and `recommendedHostsForComponent` implement the complete host-group Advisor request and recommendation-first fallback. |
| RMHA-2-003 | `MATCH` | `createRmHaAssignment` requires present, distinct, available, maintenance-OFF hosts without adding a `/validations` request. |
| RMHA-2-004 | `MATCH` | Hosts are loaded with CPU, memory, disk, and maintenance fields and sorted memory/CPU/name; the selector shows host resources and limits large-cluster search results. Covered by `rmHaUtils.test.ts`. |
| RMHA-2-005 | `MATCH` | The full `{currentRM, additionalRM, hosts, masterComponentHosts, topologyHosts}` snapshot is saved before Next; Back persists downstream invalidation before Step 1. |
| RMHA-2-006 | `COMPATIBILITY_FIX` | React directly awaits host, topology, and Advisor loads on fresh entry or refresh and presents an error with Retry, removing Classic's modal-insertion/snapshot race. |
| RMHA-3-001 | `MATCH` | `Step3.tsx` displays current/additional hosts and read-only grouped YARN, conditional HAWQ, and HDFS changes; Next remains disabled until all data is built. |
| RMHA-3-002 | `MATCH` | `loadRmHaReview` loads exact current `zoo.cfg`, `yarn-site`, and `yarn-env`; `buildRmHaReviewConfig` preserves ports with `8025`, `8088`, `8090`, and `2181` fallbacks. |
| RMHA-3-003 | `MATCH` | `rm_ha_properties.ts`, `rm_ha_config_initializer.ts`, and `buildRmHaReviewConfig` generate the full core YARN HA property set. `rmHaUtils.test.ts` asserts concrete values. |
| RMHA-3-004 | `MATCH` | HAWQ presence conditionally includes `yarn-client` addresses in Review and adds the HAWQ task in `createRmHaOperations`; absence removes both. |
| RMHA-3-005 | `MATCH` | `buildConfigRecommendationPayload` includes current configs/topology and `loadRmHaReview` awaits the configuration Advisor response before building Review. |
| RMHA-3-006 | `MATCH` | `buildRmHaReviewConfig` updates or creates only `hadoop.proxyuser.<yarn_user>.hosts` from Advisor and includes it in the immutable `core-site` save set. |
| RMHA-3-007 | `MATCH` | `Step3.tsx` uses `runWithKdcSession`; non-secure/Manual continues, while automatic KDC cancellation, lookup, or credential failure remains on Review with a visible error. |
| RMHA-3-008 | `COMPATIBILITY_FIX` | Missing tags, configs, `yarn_user`, or malformed Advisor data rejects `loadRmHaReview` and exposes Retry. React does not copy Classic's undefined error callback or partially initialized Review. |
| RMHA-CFG-001 | `MATCH` | `yarn.resourcemanager.ha.enabled=true` is generated and saved. |
| RMHA-CFG-002 | `MATCH` | Fixed `yarn.resourcemanager.ha.rm-ids=rm1,rm2` is generated and saved. |
| RMHA-CFG-003 | `MATCH` | `hostname.rm1` and `hostname.rm2` use the current and additional hostnames. |
| RMHA-CFG-004 | `MATCH` | Both resource-tracker addresses preserve the current port or use `8025`. |
| RMHA-CFG-005 | `MATCH` | Both HTTP webapp addresses preserve the current port or use `8088`. |
| RMHA-CFG-006 | `MATCH` | Both HTTPS webapp addresses preserve the current port or use `8090`. |
| RMHA-CFG-007 | `MATCH` | `yarn.resourcemanager.recovery.enabled=true` is generated and saved. |
| RMHA-CFG-008 | `MATCH` | The fixed `ZKRMStateStore` class is generated and saved. |
| RMHA-CFG-009 | `MATCH` | ZooKeeper Server topology and `zoo.cfg/clientPort`, default `2181`, produce the full quorum. |
| RMHA-CFG-010 | `MATCH` | Fixed cluster ID `yarn-cluster` is generated and saved. |
| RMHA-CFG-011 | `MATCH` | Fixed leader-election path `/yarn-leader-election` is generated and saved. |
| RMHA-CFG-012 | `MATCH` | HAWQ-only RM client addresses use current/additional hosts at port `8032`. |
| RMHA-CFG-013 | `MATCH` | HAWQ-only scheduler HA addresses use current/additional hosts at port `8030`. |
| RMHA-CFG-014 | `MATCH` | The exact Advisor proxyuser-host property is added only when `yarn_user` and a recommendation exist. |
| RMHA-CFG-015 | `MATCH` | `buildDesiredConfigPayload` overlays each reviewed site on the latest complete properties, retains `properties_attributes`, and performs ordered independent desired-config PUTs. |
| RMHA-4-001 | `MATCH` | `createRmHaOperations` stops every selected installed service except HDFS and requires its Ambari request to terminate before install. |
| RMHA-4-002 | `COMPATIBILITY_FIX` | `rmHaApi.installAdditionalResourceManager` runs KDC, duplicate check, optional service-component create, host registration, Install PUT, and polling in order; every unexpected failure stops the task. |
| RMHA-4-003 | `MATCH` | Reconfigure YARN reloads the exact latest tag and full `yarn-site`, overlays Review, preserves attributes, and completes only after PUT success. |
| RMHA-4-004 | `MATCH` | The conditional HAWQ operation applies the same contract to `yarn-client`. |
| RMHA-4-005 | `MATCH` | Reconfigure HDFS reloads complete `core-site`, applies the optional proxyuser change, preserves attributes, and saves even when the overlay is empty. |
| RMHA-4-006 | `MATCH` | Start All uses `params/run_smoke_test` and honors `skip.service.checks`; request completion is polled. |
| RMHA-4-007 | `MATCH` | `allowCompleteOnFinalFailure` plus `canCompleteRmHa` allows completion only when all prior tasks completed and the final Start is terminal, including failed/timed-out/aborted. |
| RMHA-4-008 | `COMPATIBILITY_FIX` | Completion awaits clearing workflow/owner state and stays visible on persistence failure instead of navigating from Classic's `always` callback. |
| RMHA-4-009 | `MATCH` | Stable operation IDs retain serial YARN, optional HAWQ, HDFS order. Retry does not replay completed sites and no cross-site transaction is claimed. |

### Ranger Admin Steps

| Feature ID | Status | React implementation and evidence |
| --- | --- | --- |
| RAHA-1-001 | `MATCH` | `rangerAdmin/Step1.tsx` explains Active/Standby, failover, downtime, and the external load-balancer prerequisite without claiming to configure it. |
| RAHA-1-002 | `MATCH` | React uses the same URL regular expression as Classic; empty or invalid input disables Next and non-empty invalid input shows the error. `Step1.test.tsx` covers accepted and rejected forms. |
| RAHA-1-003 | `MATCH` | Steps 1 and 2 retain the separate-host warning without pretending to validate DNS, TLS, reachability, health checks, or co-location. |
| RAHA-1-004 | `MATCH` | The URL is serialized before Step 2 and restored from the provider namespace; downstream snapshots are invalidated on a new Step 1 transition. |
| RAHA-2-001 | `MATCH` | `AssignMastersAddable` fixes the current RA, seeds at least one additional RA, supports add/remove to stack/host cardinality, and prevents removal of installed instances. |
| RAHA-2-002 | `MATCH` | Shared placement performs the complete Advisor exchange, resource sorting, current-first presentation, and Additional labels. `AssignMastersAddable.test.tsx` covers payload and ordering. |
| RAHA-2-003 | `MATCH` | Shared and Ranger-specific validation require assigned, maintenance-OFF, unique hosts without invoking server `/validations`; the LB warning remains non-blocking. |
| RAHA-2-004 | `COMPATIBILITY_FIX` | Host/topology/Advisor loading reports `loading`, `ready`, or actionable error/Retry to Step 2, replacing Classic's unresolved load-map failure. |
| RAHA-2-005 | `MATCH` | The complete master mapping is serialized; Step 3/4 derive one current and every additional RA from it. Back persists invalidation before returning to Step 1. |
| RAHA-3-001 | `MATCH` | `Step3.tsx` renders current/additional hosts, blocks on the stack-config load, and shows every LB change read-only. |
| RAHA-3-002 | `MATCH` | `wizardConstants.ts` defines all ten candidates; `buildRangerAdminPreview` includes candidates mapped to installed services and preserves Classic's absent-property fallback display. |
| RAHA-3-003 | `MATCH` | Review proceeds directly after a durable checkpoint; KDC validation remains inside the install operation. |
| RAHA-3-004 | `COMPATIBILITY_FIX` | `buildRangerAdminPreview` performs an actual category-name membership check, so multiple candidate sites cannot create the duplicate categories caused by Classic's comma operator. |
| RAHA-4-001 | `MATCH` | `createRangerAdminHaOperations` begins with Stop All Services and the shared request runner polls its request before install. |
| RAHA-4-002 | `COMPATIBILITY_FIX` | Step 4 validates assignments, checks KDC, reconciles all additional hosts with `{reconcileHosts:true}`, installs them in one request, and fails closed unless a lost response is verified against topology. |
| RAHA-4-003 | `COMPATIBILITY_FIX` | `reconfigureRangerAdminServices` validates every desired tag and returned config, updates applicable candidate sites, retains complete properties/attributes, and sends the required array body. It cannot falsely complete after a thrown JS-array/config error. |
| RAHA-4-004 | `MATCH` | Start All requests smoke tests unless `skip.service.checks=true`, then polls the request. |
| RAHA-4-005 | `MATCH` | Ranger does not opt into final-failure completion; any failed task exposes Retry and keeps Complete disabled. |
| RAHA-4-006 | `COMPATIBILITY_FIX` | Complete awaits persisted workflow/owner cleanup and navigates only after success; critical Cancel warns and retains the checkpoint. |

### Shared Failure, Recovery, Kerberos, and Installation

| Feature ID | Status | React implementation and evidence |
| --- | --- | --- |
| HA-COMMON-FAIL-001 | `PARTIAL` | `OperationsProgress` serializes PENDING/QUEUED/IN_PROGRESS/terminal transitions and persists QUEUED before invoking a callback. It cannot atomically bind that checkpoint to the server mutation, so a lost mutation response before request-ID persistence remains an unknown-outcome replay window. Direct successful config PUTs complete without fabricating a request. |
| HA-COMMON-FAIL-002 | `MATCH` | Ambari requests poll every four seconds; FAILED, TIMEDOUT, and ABORTED are terminal failures and only COMPLETED advances. Covered by `OperationsProgress.test.tsx`. |
| HA-COMMON-FAIL-003 | `MATCH` | Request-bearing operation labels open Background Operations, whose request, host, task, stdout, stderr, output, and error flows are shared with Module 02. |
| HA-COMMON-FAIL-004 | `COMPATIBILITY_FIX` | Retry targets a locally failed or server-confirmed terminal operation, invalidates stale polling generations, and clears that operation and all downstream request IDs before replay. This prevents old/new request-ID mixing after a confirmed failure but does not reconcile the pre-ID unknown-outcome window. |
| HA-COMMON-FAIL-005 | `MATCH` | Every RM/RA operation is `skippable:false`; neither wizard exposes Skip. |
| HA-COMMON-FAIL-006 | `NOT_APPLICABLE` | React does not expose Classic's NameNode-hard-coded rollback button for RM or RA and makes no automatic rollback claim. |
| HA-COMMON-REC-001 | `COMPATIBILITY_FIX` | Both providers serialize step, task status, request ID, cluster progress, and owner through a queued `/persist` write; request execution/advancement waits for the checkpoint. |
| HA-COMMON-REC-002 | `PARTIAL` | Hydration restores completed tasks and resumes IN_PROGRESS requests by saved ID; malformed or missing IDs fail visibly instead of hanging. A durably QUEUED callback runs once in the recovered client, but the client cannot prove that an earlier browser did not submit it before losing the response. |
| HA-COMMON-REC-003 | `COMPATIBILITY_FIX` | React uses explicit `ENABLING_RM_HA`/`ENABLING_RANGER_ADMIN_HA`, namespace `activeStep`, and route-owner data rather than Classic's inconsistent valid-state/local-DB trust list. Real restart recovery remains a runtime case. |
| HA-COMMON-REC-004 | `COMPATIBILITY_FIX` | Early Cancel clears pre-change state; Step 4 Cancel/close warns that no rollback occurs and retains the recoverable deployment checkpoint. |
| HA-COMMON-REC-005 | `COMPATIBILITY_FIX` | Both wizards use the same guarded Back/jump/modal-close rules. Step 4 has no Back, and explicit critical exit retains state rather than reproducing Classic's inconsistent RM/RA route hooks. |
| HA-COMMON-REC-006 | `COMPATIBILITY_FIX` | Every early transition writes the HA progress state and active step, so recovery is not deferred until the first Step 4 task observer. |
| HA-COMMON-REC-007 | `COMPATIBILITY_FIX` | `wizard-data.userName` is loaded by `AppContext`; `ServiceOperationRouteGuard` prevents another user from entering and providers preserve the owner on each checkpoint. |
| HA-COMMON-REC-008 | `COMPATIBILITY_FIX` | Menu and direct route require both `SERVICE.ENABLE_HA` and `CLUSTER.MANAGE_USER_PERSISTED_DATA`, eliminating entry into a workflow that cannot checkpoint. |
| HA-COMMON-KRB-001 | `COMPATIBILITY_FIX` | `useKDCSessionState` explicitly awaits security type/session APIs and sends transport/parse errors to the workflow callback; non-secure and Manual mode continue directly. |
| HA-COMMON-KRB-002 | `MATCH` | Invalid KDC opens the credential flow with temporary/persisted storage capability and alias `kdc.admin.credential`; successful credentials retry the gate. |
| HA-COMMON-KRB-003 | `COMPATIBILITY_FIX` | RM checks at Review and Install; RA checks at Install. Cancellation, lookup failure, or credential error rejects the current action instead of leaving it QUEUED. |
| HA-COMMON-KRB-004 | `NEEDS_RUNTIME_VALIDATION` | React uses the server host-component install chain and does not invent descriptor/keytab calls. MIT, AD, IPA, and Manual identity/keytab convergence requires a real cluster. |
| HA-COMMON-KRB-005 | `COMPATIBILITY_FIX` | Credential GET and 404-create/existing-update are awaited; credential-save failure is terminal and cannot silently replay the KDC request. Covered by shared Kerberos tests. |
| HA-COMMON-KRB-006 | `COMPATIBILITY_FIX` | Missing or null validation data becomes a controlled invalid/error path; React never calls `.toUpperCase()` on an unchecked value. |
| HA-COMMON-INSTALL-001 | `MATCH` | The install helper queries matching component/hosts first, subtracts existing registrations, and still reconciles the full RA target set when requested. |
| HA-COMMON-INSTALL-002 | `COMPATIBILITY_FIX` | React uses authoritative component/host queries at each decision and after ambiguous responses instead of relying on asynchronously refreshed frontend metric models. |
| HA-COMMON-INSTALL-003 | `MATCH` | A missing service-component is created before host registration. |
| HA-COMMON-INSTALL-004 | `COMPATIBILITY_FIX` | Create failure is fatal unless it is explicit already-exists or reconciliation proves the component/hosts now exist. |
| HA-COMMON-INSTALL-005 | `COMPATIBILITY_FIX` | Host registration failure is fatal unless a follow-up topology query proves every target was registered; only then may Install continue. |
| HA-COMMON-INSTALL-006 | `MATCH` | The component/host/maintenance-OFF Install PUT returns a required request ID and is polled to terminal state. |
| HA-COMMON-INSTALL-007 | `COMPATIBILITY_FIX` | Duplicate-check, partial-host, create, register, malformed-success, missing-request-ID, and Install errors all produce a retryable failed operation with no later task advancement. |

### Required Runtime Scenarios

| Feature ID | Status | Required React evidence |
| --- | --- | --- |
| HA-STATIC-002 | `NEEDS_RUNTIME_VALIDATION` | On one RM entry, combine stopped RM, two ZooKeeper Servers, and two hosts and verify all failures render; verify stopped ZooKeeper instances still count, matching Classic. |
| HA-STATIC-003 | `NEEDS_RUNTIME_VALIDATION` | Enter Ranger with its Classic-permitted runtime states and record the first real backend rejection/recovery point without adding RM-only prerequisites. |
| HA-STATIC-004 | `NEEDS_RUNTIME_VALIDATION` | Refresh and directly enter RM Step 2 on small and more-than-25-host clusters; verify host resources, ten-result typeahead, Advisor payload, load error, and Retry. |
| HA-STATIC-005 | `NEEDS_RUNTIME_VALIDATION` | Exercise RA stack cardinalities `1-2`, `1+`, and limits above host count; verify add/remove bounds, immutable current RA, and one RA per host. |
| HA-STATIC-006 | `NEEDS_RUNTIME_VALIDATION` | Test `http`, `https`, and `ftp` URLs with ports/path/query plus empty, no-scheme, unreachable, TLS-failing, and co-located load balancers; distinguish syntax from external validation. |
| HA-STATIC-007 | `NEEDS_RUNTIME_VALIDATION` | Fault RM desired-tag GET, config GET, and Advisor separately; Next must stay disabled, Retry must recover, and no incomplete proxyuser/config snapshot may reach Step 4. |
| HA-STATIC-008 | `NEEDS_RUNTIME_VALIDATION` | Fault duplicate GET, create, register, Install PUT, and polling with partial/maintenance-ON targets; verify fail-closed Retry and final topology. |
| HA-STATIC-009 | `PARTIAL` | During every Stop/Install/Configure/Start request, close, refresh, crash, restart the server, use a second browser, and change users. Saved request IDs can resume without replay; the pre-ID unknown-outcome case cannot guarantee exactly-once mutation until the server supplies reconciliation or idempotency evidence. |
| HA-STATIC-010 | `NEEDS_RUNTIME_VALIDATION` | Fail final Start: RM may Complete after durable failure, RA may not. Fail earlier tasks and verify only the intended failed operation replays and Skip remains absent. |
| HA-STATIC-011 | `NEEDS_RUNTIME_VALIDATION` | Enable `supports.autoRollbackHA` and confirm no RM/RA rollback control or NameNode route appears in React. |
| HA-STATIC-012 | `NEEDS_RUNTIME_VALIDATION` | Cover Kerberos NONE, Manual, MIT, AD, and IPA with temporary/persisted credentials, cancellation, bad save, retry, and final RM/RA principal/keytab ownership checks. |

## React API Contract

All paths below use `/api/v1`. Cluster, stack, version, host, component, config
tag, and request values are URL variables and must be encoded where applicable.

| Operation | Method, URL, and payload/order | React evidence |
| --- | --- | --- |
| RM hosts | `GET /clusters/{cluster}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true` | `rmHaApi.getHosts`; exact URL asserted in `rmHaApi.test.ts`. |
| RM topology | `GET /clusters/{cluster}/components?fields=ServiceComponentInfo/...,...host_components/HostRoles/...&minimal_response=true` before entry, assignment, and Advisor | `rmHaApi.getClusterComponents`, `flattenClusterTopology`. |
| RA topology | `GET /clusters/{cluster}/services/RANGER/components/RANGER_ADMIN?fields=host_components/HostRoles/host_name,host_components/HostRoles/state` before fresh entry | `rangerAdminEnablementApi`, `evaluateRangerAdminEnablement`, and `rangerAdminHaApi.test.ts`. |
| Shared RA assignment | Host/component topology, host-resource GET, and Stack Advisor recommendation sequence used by `AssignMastersAddable` | `AssignMastersAddable.tsx` and its focused test. |
| Host recommendation | `POST /stacks/{stack}/versions/{version}/recommendations` with `recommend:"host_groups"`, all hosts/services, blueprint, and binding | `rmHaApi.getHostRecommendations`, `buildHostRecommendationPayload`. |
| Desired tags | `GET /clusters/{cluster}?fields=Clusters/desired_configs` before every Review/save config load | `rmHaApi.getDesiredConfigs`; `rangerAdminConfigApi.loadConfigTags`. |
| Current configs | `GET /clusters/{cluster}/configurations?(type={type}&tag={tag})|...` using every exact desired tag | `rmHaApi.getConfigs`; `rangerAdminConfigApi.reassignLoadConfigs`. |
| Config recommendation | `POST /stacks/{stack}/versions/{version}/recommendations` with `recommend:"configurations"`, topology, blueprint, and current configurations | `loadRmHaReview`, `buildConfigRecommendationPayload`. |
| RM desired config | Ordered `PUT /clusters/{cluster}` with `{Clusters:{desired_config:[{type,properties,properties_attributes?,service_config_version_note}]}}` | `rmHaApi.saveDesiredConfig`, `buildDesiredConfigPayload`, workflow/API tests. |
| RA desired configs | One `PUT /clusters/{cluster}` whose body is an array of `{Clusters:{desired_config:[...]}}` objects, one for every applicable current candidate site | `reconfigureRangerAdminServices`, API and utility tests. |
| Component existence | `GET /clusters/{cluster}/host_components?HostRoles/component_name={component}&HostRoles/host_name.in({hosts})&fields=HostRoles/host_name&minimal_response=true` | RM API helper and shared `taskUtils`. |
| Service-component existence/create | RM checks `GET /services/YARN/components/RESOURCEMANAGER`; missing components use `POST /clusters/{cluster}/services?ServiceInfo/service_name={service}` with `components[].ServiceComponentInfo.component_name` | `rmHaApi.installAdditionalResourceManager`; shared `ServiceApi.createComponent`. |
| Host registration | `POST /clusters/{cluster}/hosts` with OR host query and `Body.host_components[].HostRoles.component_name` | RM API helper; `HostsApi.registerHostToComponent`; fail-closed tests. |
| Component install | `PUT /clusters/{cluster}/host_components` with component, hosts, `maintenance_state=OFF`, cluster operation level, and `Body.HostRoles.state="INSTALLED"` | RM API helper; `taskUtils.updateComponent`; request-ID tests. |
| Stop/start services | `PUT /clusters/{cluster}/services?{predicate or params}` with cluster operation level and `Body.ServiceInfo.state`; Start includes `params/run_smoke_test={true|false}` | RM API helper, `taskUtils.stopAllServices/startAllServices`, focused tests. |
| Request polling | `GET /clusters/{cluster}/requests/{id}?fields=*,tasks/Tasks/...&minimal_response=true` every four seconds until terminal | `RequestApi.getRequestStatus`, `OperationsProgress`, timer/recovery tests. |
| Kerberos gate | Security status/config type, then `GET /clusters/{cluster}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,...`; credential GET followed by PUT or explicit-404 POST | `useKDCSessionState`, `InvalidKdcPopup`, credentials API and tests. |
| Persistence | `GET /persist/{workflowKey}` on hydration and serialized `POST /persist` for workflow state, `CLUSTER_STATE`, and `wizard-data` | Both provider contexts and their tests. |

Configuration PUTs are synchronous workflow tasks unless the server actually
returns `Requests.id`; React never invents a request ID. Stop, install, and
start operations require an ID and poll it. A malformed successful response is
therefore a visible failure for request-producing operations, while a valid 2xx
desired-config response completes directly.

## Compatibility Fixes

The following differences are intentional and must not be regressed in the
name of byte-for-byte Classic parity:

1. Direct routes require mutation and persisted-data authorization and reject another workflow owner.
2. Host, tag, config, Advisor, and preflight failures remain visible with Retry instead of continuing with partial state.
3. Service-component create and host registration are fail closed except when an authoritative follow-up proves an ambiguous/lost response succeeded.
4. Native array/object APIs build Ranger multi-config payloads; full properties and `properties_attributes` are preserved.
5. KDC transport, malformed state, cancellation, and credential-save failures reject the guarded operation.
6. QUEUED state, returned request IDs, navigation, and completion transitions wait for durable persistence at their respective boundaries; the server mutation and later request-ID write are not atomic.
7. Retry clears stale request IDs and polling generations before replay, preventing a new request from being mixed with an earlier failed request.
8. Broken RM/RA automatic rollback controls are omitted; critical exit explicitly retains a manual-recovery checkpoint.

## Executable Acceptance Criteria

1. Fresh menu and direct-route entry enforce both permissions, workflow ownership, service/topology gates, and the correct hidden-versus-disabled state for each HA mode.
2. RM entry reports STARTED RM, ZooKeeper count, and host count failures together; Ranger does not inherit those RM-only runtime checks.
3. RM and RA Step 2 do not enable Next before host and Advisor state is ready and the entire assignment satisfies current/additional, uniqueness, availability, maintenance, and cardinality rules.
4. RM Review loads exact active tags, generates all 15 configuration IDs, preserves current ports/defaults, and includes HAWQ/proxyuser only under their documented conditions.
5. Ranger accepts exactly the Classic URL grammar, previews all installed-service candidates read-only, and emits no duplicate categories.
6. Every config save starts from the latest complete site, retains attributes, and uses the documented single-body RM or array-body Ranger contract.
7. Stop, install, config, and start tasks execute in stable order; no later task starts after a failed prerequisite.
8. Component installation checks existence, creates the service-component if needed, registers missing hosts, installs the complete target, and treats only verified idempotent cases as success.
9. Request-producing operations require and persist `Requests.id`, poll every four seconds, and treat FAILED, TIMEDOUT, and ABORTED as failure.
10. Retry after a local or server-confirmed failure replays the intended operation without reusing an old request ID; completed earlier work remains completed and neither wizard exposes Skip. Unknown-outcome recovery remains partial until server reconciliation exists.
11. RM permits completion only after a final Start failure; Ranger requires final Start success. Earlier failures never enable Complete.
12. Automatic Kerberos gates fail closed on cancellation, lookup, malformed state, or credential persistence; non-secure and Manual paths do not require KDC credentials.
13. Refresh restores the exact step, assignment/config snapshot, task states, active request, and owner before rendering executable controls.
14. Early Cancel clears only after persistence; critical Cancel retains recovery evidence and does not claim rollback; completion navigates only after cleanup succeeds.
15. Focused tests cover entry, route, assignment, config generation/payloads, install idempotency/failure, polling/Retry, smoke-test policy, persistence ordering, hydration, owner state, and completion differences.

## Runtime Acceptance Matrix

| Scenario | Topology or mode | Fault and transition points | Required evidence | Status |
| --- | --- | --- | --- | --- |
| RM base | Three hosts, three ZooKeeper Servers, YARN/HDFS | Refresh before/after each Step 4 task | Ordered browser HTTP capture, request/task terminal states, desired config versions, Active/Standby RM topology | `NEEDS_RUNTIME_VALIDATION` |
| RM HAWQ | Historical stack with HAWQ installed | Fail each of YARN, HAWQ, and HDFS saves | Conditional `yarn-client`, serial partial-state behavior, Retry only failed site | `NEEDS_RUNTIME_VALIDATION` |
| RM large cluster | More than 25 hosts with equal resource values | Advisor failure and response reordering | Deterministic host order, ten-result typeahead, complete Advisor body, visible Retry | `NEEDS_RUNTIME_VALIDATION` |
| Ranger base | One current plus one additional RA and real load balancer | Fault config tag/read/array PUT and Start | Exact ten-candidate filtering, full snapshots/attributes, external URL convergence, RA Complete disabled on failure | `NEEDS_RUNTIME_VALIDATION` |
| Ranger multi-add | Stack allowing three or more RAs | Partial registration, lost create/register response, maintenance ON | All additional hosts reconciled exactly once and final topology matches selection | `NEEDS_RUNTIME_VALIDATION` |
| Ranger LB variants | HTTP/HTTPS/FTP, ports/path/query, unreachable/TLS/co-located cases | Step 1 validation and post-start client requests | Syntax gate separated from operator-managed connectivity/health evidence | `NEEDS_RUNTIME_VALIDATION` |
| Automatic Kerberos | MIT, AD, and IPA where supported | Expired session, popup cancel, failed credential save, retry | No mutation after failure, correct credential method, generated principal/keytab ownership, one install request | `NEEDS_RUNTIME_VALIDATION` |
| Manual Kerberos | `kdc_type=none` | Refresh at Review and Install | No admin credential required; server install provides valid identities/keytabs; progress resumes | `NEEDS_RUNTIME_VALIDATION` |
| Recovery and ownership | Both workflows | Browser refresh/crash, Ambari Server restart, second browser/user, permission revoked | Correct owner rejection/resume, request-ID continuation, visible persist failure, and captured duplicate behavior in the pre-ID unknown-outcome window | `PARTIAL` |
| Terminal failures | Both workflows | FAILED, TIMEDOUT, ABORTED, poll 500, transient network loss | Correct terminal status, stale-ID cleanup, Retry target, RM/RA final-failure difference | `NEEDS_RUNTIME_VALIDATION` |

## Five-Pass Audit Record

| Pass | Surface | Result |
| --- | --- | --- |
| 1 | Routes, menus, pages, controls, and wizard navigation | Traced both service actions through protected routes and all eight pages; verified hidden/disabled states, URL/assignment gates, Back invalidation, critical exit, and RM-versus-RA completion. |
| 2 | Controller/provider/model state and recovery | Compared Classic local DB fields with React namespaces, active-step/cluster status, owner, task status, and request IDs; verified serialized hydration and checkpoint-before-navigation behavior. |
| 3 | REST contracts, payloads, and request order | Traced host/topology/Advisor/config/component/service/request/Kerberos/persist calls; verified all 15 RM properties, ten Ranger candidates, complete config snapshots, array-body Ranger PUT, and fail-closed install order. |
| 4 | Permissions, stack/service branches, cardinality, and modes | Checked both permissions, HA enabled state, single/large hosts, ZooKeeper counts, maintenance, HAWQ, Ranger dependency sites, `skip.service.checks`, and Kerberos NONE/Manual/automatic branches. |
| 5 | Failure, Retry, refresh, Back, Cancel, completion, and interrupted recovery | Fault-read every asynchronous boundary; returned request IDs are checkpointed, stale IDs are cleared on confirmed-failure Retry, persistence failure blocks advancement, and critical exit retains owner/recovery state. The pre-ID unknown-outcome window and runtime scenarios remain explicitly unclaimed. |

The Classic baseline was corrected only where executable source/tests disproved
handwritten evidence claims. Generated evidence was not edited.

## Verification Evidence

The Module 10 focused React suite covers entry and route guards, Step 1/2
behavior, assignment and Advisor payloads, all generated configs, desired-config
and component APIs, Ranger multi-config bodies, KDC/install failure, task
ordering, four-second polling, Retry request-ID cleanup, completion differences,
provider hydration, and persistence ordering. TypeScript, the production build,
targeted ESLint, baseline validation, ID coverage comparison, credential/debug
scan, and `git diff --check` are required to pass before this document is used
as implementation-complete evidence. Real-cluster results must be appended to
the runtime matrix rather than inferred from unit tests.
