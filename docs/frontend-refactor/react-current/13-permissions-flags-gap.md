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

# React Permissions, Feature Flags, and Runtime Gates Gap Analysis

## Scope and Method

This document compares all 36 stable IDs in
`ember-baseline/13-permissions-flags.md` with the executable Classic Ember
source and the current React implementation. It also audits every generated
`App.supports` flag, route and menu authorization, wizard-owner exclusion,
upgrade exclusion, stack/service/component metadata gate, state gate, and
client-side persistence mutation. Metrics pages, widgets, charts, and their
permissions remain excluded.

Matching names were not accepted as evidence. The audit separately checked
permission expression semantics, mutation versus read visibility, persisted
support overrides, direct routes, server-derived topology, failure behavior,
and tests. Static evidence cannot establish server-side RBAC, stack-specific
metadata, or multi-user behavior; those cases remain explicitly unverified.

| Status | Meaning |
| --- | --- |
| `STATICALLY_ALIGNED` | React implements the source-backed rule and focused static or test evidence exists. |
| `PARTIAL` | A usable path exists, but one or more documented conditions or branches are absent. |
| `BEHAVIOR_DIFF` | React intentionally or accidentally differs from executable Classic behavior. |
| `MISSING` | No React consumer implements the required behavior. |
| `NEEDS_RUNTIME_VALIDATION` | Static code exists, but the result depends on a real server, role, stack, topology, or workflow. |
| `OUT_OF_SCOPE` | The flag or permission belongs only to the excluded Metrics surface. |
| `STATIC_ONLY` | Classic defines the item but has no executable product consumer. |

## Audit Result

| Final status | Gate IDs |
| --- | ---: |
| `STATICALLY_ALIGNED` | 20 |
| `PARTIAL` | 1 |
| `BEHAVIOR_DIFF` | 6 |
| `MISSING` | 0 |
| `NEEDS_RUNTIME_VALIDATION` | 9 |
| Total | 36 |

The shared React authorization policy now preserves Classic comma-separated OR
expressions, the complete-expression upgrade exceptions, the
`opsDuringRollingUpgrade` override, and the distinction between read
visibility and mutations. Admin routes, User Settings, Experimental Save and
Reset, restart-required service operations, and RouteTracker persistence use
that policy, including Navbar. The digital clock and operational-log count are
now implemented, HAWQ fails closed on `HDPWIN`, and `autoRollbackHA` suppresses
unsafe cancellation during Classic's critical phases. The remaining static
product gaps are the missing automatic rollback state machine and the partial
Windows/stack family boundary. React intentionally applies stricter server-metadata proxies
to Federation and HAWQ workflow entry than Classic's hard-coded service tags,
and makes the experimental service restart path safe and recoverable instead
of reproducing its known scope, ordering, permission, and duplicate-request
defects.

## Permission Determination Semantics

| ID | Final status | React evidence and remaining acceptance |
| --- | --- | --- |
| `GATE-AUTH-001` | `STATICALLY_ALIGNED` | `store/UserContext.tsx:202-214` trims comma-separated IDs and returns true when any ID is present. `Utils/authorizationPolicy.test.ts:27-36` covers normalization and an OR expression containing an upgrade exception. |
| `GATE-AUTH-002` | `STATICALLY_ALIGNED` | `Utils/authorizationPolicy.ts:55-60` rejects a mutation for `isNonWizardUser`; `hooks/useAuthorizationPolicy.ts:51-64` exposes this as `isAuthorized`. The policy, Admin route, User Settings, and Experimental tests cover the non-owner result. |
| `GATE-AUTH-003` | `STATICALLY_ALIGNED` | `hooks/useAuthorizationPolicy.ts:38-49` exposes `havePermissions` without the wizard-owner check. `Utils/authorizationPolicy.test.ts:94-107` proves the same RBAC permission remains readable while mutation is denied. |
| `GATE-AUTH-004` | `STATICALLY_ALIGNED` | `Utils/authorizationPolicy.ts` and `hooks/useAuthorizationPolicy.ts` preserve both complete-expression exceptions, the early ordinary-permission deny, and `supports.opsDuringRollingUpgrade`; focused tests cover all branches. Navbar, route guards, Stack Services, Stack Versions, and Service Actions consume the shared policy instead of adding an unconditional upgrade deny. |
| `GATE-AUTH-005` | `STATICALLY_ALIGNED` | `store/UserContext.tsx:202-207` searches the initially empty authorization collection, so unknown/unloaded permission data denies every requested ID. Protected routes display loading before testing permissions (`components/AuthGuard.tsx:58-85`). |
| `GATE-AUTH-006` | `BEHAVIOR_DIFF` | `Utils/authPolicy.ts:21-25` and `store/context.tsx:225-227` classify an empty or sole `VIEW.USE` collection directly. Classic calls `App.isAuthorized('VIEW.USE')`, so an upgrade or another wizard owner can make a sole-View user cease to be view-only. React keeps the user in the Views shell; maintainers must accept this safer navigation difference or request exact compatibility. |
| `GATE-AUTH-007` | `STATICALLY_ALIGNED` | `store/UserContext.tsx:223-228` makes a Cluster Administrator both admin and operator, while Ambari Administrator contributes only to `isAdmin`. Fine-grained action checks still consume returned authorization IDs. |
| `GATE-AUTH-008` | `NEEDS_RUNTIME_VALIDATION` | React guards known routes and controls, but only a real Ambari Server can prove unauthorized deep links and direct REST mutations return 403. The runtime matrix requires this for every representative role. |

### Role, Upgrade, and Wizard Matrix

| Raw role/permission state | Idle, no other owner | Another user owns wizard | Blocking upgrade, support flag false | Suspended upgrade or support flag true |
| --- | --- | --- | --- | --- |
| Permission absent or model not loaded | Read and mutation deny | Read and mutation deny | Read and mutation deny | Read and mutation deny |
| Ordinary permission present | `havePermissions=true`; `isAuthorized=true` | Read remains true; mutation false | Read and mutation false | Raw read allowed; mutation allowed only for the wizard owner |
| `CLUSTER.MANAGE_USER_PERSISTED_DATA` present | Read and mutation true | Read true; mutation false | Upgrade exception keeps raw permission usable; other-owner mutation still false | Same as idle, subject to owner |
| `CLUSTER.UPGRADE_DOWNGRADE_STACK` present | Read and mutation true | Read true; mutation false | Upgrade exception keeps raw permission usable; other-owner mutation still false | Same as idle, subject to owner |
| Combined OR contains an exception and another granted ID | Any granted ID makes the expression true | Read true; mutation false | Whole expression bypasses the upgrade deny, matching Classic contamination | Any granted ID makes the expression true, subject to owner |
| Empty or sole `VIEW.USE` authorization collection | React enters the Views-only shell | React remains Views-only, unlike Classic's `isAuthorized` computation | React remains Views-only, unlike Classic | React remains Views-only |
| Cluster Administrator privilege | `isAdmin=true`, `isOperator=true`; fine-grained IDs still govern actions | Mutation policy still blocks | Ordinary operations still block | Fine-grained IDs and owner decide |
| Ambari Administrator privilege only | `isAdmin=true`; not automatically operator | Mutation policy still blocks | Ordinary operations still block | Returned fine-grained IDs and owner decide |

No row grants server capability. The server must independently authorize each
REST request.

## Other Runtime Gates

| ID | Final status | React evidence and remaining acceptance |
| --- | --- | --- |
| `GATE-RUNTIME-001` | `STATICALLY_ALIGNED` | Host Details derives availability from the loaded stack-version collection and hides/redirects its Versions route (`screens/Hosts/index.tsx`). Admin Stack and Versions now consumes the same `useStackVersion` model, keeps the tab visible until the initial load resolves, hides it for an empty collection, and redirects a stale Versions deep link to Stack (`StackAndVersions.tsx`, `StackAndVersions.test.tsx`). |
| `GATE-RUNTIME-002` | `STATICALLY_ALIGNED` | `StackAndVersions/upgradeUtils.ts:81-95` requires finished history and distinguishes a sole active/aborted upgrade; `upgradeUtils.test.ts:92-112` covers the three Classic branches. `StackAndVersions.tsx:47-69` applies the result and exposes load failure. |
| `GATE-RUNTIME-003` | `STATICALLY_ALIGNED` | The React store loads `server_clock`, `Navbar.tsx` honors runtime `enableDigitalClock`, and `DigitalClock.tsx` advances from the server/client offset rather than treating this as an `App.supports` flag. Formatter, component, store, and visibility tests cover seconds versus milliseconds, invalid values, update, and cleanup; server skew remains runtime validation. |

## Stack, Service, and Component Metadata

| ID | Final status | React evidence and remaining acceptance |
| --- | --- | --- |
| `GATE-META-001` | `STATICALLY_ALIGNED` | The shared Installer/Add Service Choose Services step filters explicit `StackServices.is_installable=false` entries and preserves Classic's mapper-level Kerberos exclusion before selection, preselection, dependency validation, and persistence (`Utils/stackMetadata.ts`, `ClusterWizard/Step4.tsx`, `Step4.test.tsx`). The Add Service provider intentionally retains already-installed non-installable names for downstream workflow state, matching Classic's `saveServices`; dependency-aware deletion remains in `hooks/useServiceDeletion.tsx`. |
| `GATE-META-002` | `BEHAVIOR_DIFF` | Classic's `serviceTypes` is a hard-coded service-name map, not server metadata. `models/service.ts` now mirrors its `HA_MODE`/`FEDERATION`/`DFSRouter` labels and the HDFS/YARN/RANGER entries use the same service-name boundary. HAWQ entry and direct routes additionally require stack components, custom commands, and valid topology through `useHawqStandbyCapabilities`; maintainers accepted this fail-closed difference pending real HDP-version validation. |
| `GATE-META-003` | `BEHAVIOR_DIFF` | Classic always tags HDFS with `FEDERATION`. React deliberately requires an HDFS stack service, `core-site`/`hdfs-site`, and NAMENODE/JOURNALNODE/ZKFC metadata before showing or entering Federation (`useHdfsWorkflowCapabilities.ts`, `Federation/index.tsx`). Metadata load failure closes the workflow and exposes Retry. This stricter proxy is tested but is not an authoritative server capability flag. |
| `GATE-META-004` | `BEHAVIOR_DIFF` | Classic always tags HDFS with `DFSRouter` and separately disables the action until NameNode Federation exists. React additionally requires `hdfs-rbf-site` and a stack ROUTER component, while retaining the namespace/topology condition in the menu and direct route (`useHdfsWorkflowCapabilities.ts`, `WorkflowActions.tsx`, `RouterFederation/index.tsx`). The stricter proxy and Retry path require maintainer acceptance against supported stacks. |
| `GATE-META-005` | `NEEDS_RUNTIME_VALIDATION` | Host utilities consume master/client/slave/HA-only classifications and cardinality (`screens/Hosts/utils.tsx:212-328`); installer assignment and install stages also consume cardinality. Real stack minimum/maximum and optional-component combinations remain unexecuted. |
| `GATE-META-006` | `STATICALLY_ALIGNED` | Reassign menu construction reads `reassign_allowed` and host availability (`Services/reassign/index.tsx:53-101`); direct-route validation repeats the stack check (`Utils/reassignValidation.ts:69-119`). Tests reject a non-reassignable deep link. |
| `GATE-META-007` | `NEEDS_RUNTIME_VALIDATION` | Host component models retain `decommissionAllowed`, and Host Details/list action construction filters on it (`screens/Hosts/details.tsx:625`, `HostsList.tsx:1018`, `Hosts/utils.tsx:1086`). Server metadata variants and safe-state combinations remain unexecuted. |
| `GATE-META-008` | `NEEDS_RUNTIME_VALIDATION` | Host component models retain stack custom commands and Host utilities build command menus (`models/hostComponent.ts`, `screens/Hosts/utils.tsx:509-545`). Service Actions also has command-specific authorization, but real stack command collections require runtime comparison. |
| `GATE-META-009` | `NEEDS_RUNTIME_VALIDATION` | Config and installer paths load config types, dependencies, themes, value attributes, read-only state, and Advisor data; examples include `Hosts/HostConfigs.tsx:147`, `ClusterWizard/Step7/index.tsx:1078-1084,2113`, and Common Config components. Real service themes and cross-property dependencies remain unexecuted. |
| `GATE-META-010` | `PARTIAL` | React derives the stack family from cluster metadata, hides the Kerberos sidebar entry for Classic's `HDPWIN` stack, and makes HAWQ capability loading fail closed on `HDPWIN` with visible Retry (`Utils/stackMetadata.ts`, `SideItemList.tsx`, `useHawqStandbyCapabilities.ts`, focused tests). Other HA actions still rely on service/component presence rather than a complete explicit Windows/version support matrix, so Windows and non-HDP runtime evidence remains required. |

## State, Maintenance, and Long Workflows

| ID | Final status | React evidence and remaining acceptance |
| --- | --- | --- |
| `GATE-STATE-001` | `STATICALLY_ALIGNED` | Service/Host menus consume desired/current states. Service Actions now blocks `STARTING`/`STOPPING`, a local submission, an accepted request ID, and an active service request; it records transitional state after HTTP 202 instead of fabricating a terminal state. `serviceActionPolicy.test.ts` and `Actions.test.tsx` cover transition, accepted-request, duplicate-click, failure, and retry paths. |
| `GATE-STATE-002` | `NEEDS_RUNTIME_VALIDATION` | Host bulk/detail operations, service actions, Reassign, and HA validation consume host/component/service maintenance fields. Scope inheritance and implied maintenance combinations require real topology evidence. |
| `GATE-STATE-003` | `NEEDS_RUNTIME_VALIDATION` | Installer registration, host recovery/delete, and HA host selection consume `Hosts/host_status`, registration state, and component health. Heartbeat loss and recovery cannot be established by static fixtures alone. |
| `GATE-STATE-004` | `STATICALLY_ALIGNED` | `Services/RestartWarning.tsx:89-128,434-482` limits the banner/action to stale components and affected hosts; restart mutation now uses `isAuthorized('SERVICE.START_STOP')`. Host bulk restart utilities also carry stale-config and maintenance predicates. |
| `GATE-STATE-005` | `STATICALLY_ALIGNED` | Background Operations remains the shared request snapshot. Service Actions disables Start, Stop, Restart All, and the experimental service restart for active `_PARSE_` requests targeting the service, `ALL_SERVICES`, or one of the service's rolling-restart components. Immediate requests retain the accepted request ID until terminal state reload; rolling service restarts retain and poll the accepted schedule ID. Submission failures leave confirmation/configuration available for retry. This deliberately repairs the experimental Classic path's missing lock; a real server race remains in the runtime matrix rather than being claimed by unit mocks. |
| `GATE-STATE-006` | `STATICALLY_ALIGNED` | `store/context.tsx:506-508,698-709` derives the persisted owner and `authorizationPolicy.ts:55-60` revokes policy-aware mutations. Route/action tests cover the main modules, and global `AppLoader.tsx:136-157` now requires persisted-data mutation authorization before saving `USER_REDIRECTION_URL`; its component tests cover denied and owner paths. |
| `GATE-STATE-007` | `STATICALLY_ALIGNED` | `store/context.tsx:698-709` derives running/holding/suspended state; the shared policy and `ServiceOperationRouteGuard.tsx:28-44` apply the default deny and support override. Upgrade controls retain their owner/read-only rules. |
| `GATE-STATE-008` | `NEEDS_RUNTIME_VALIDATION` | Installer, Add Host, Kerberos, Reassign, and HA code contains KDC session and `Clusters/security_type` gates. Automatic/manual KDC, cancellation, credential save, and replay require real KDC acceptance. |
| `GATE-STATE-009` | `NEEDS_RUNTIME_VALIDATION` | Cluster/Add Host/Add Service/Kerberos/HA stores read and write `CLUSTER_STATE`, active step, request, and owner checkpoints. Browser crash, server restart, stale write, and cross-user recovery remain runtime-only. |

## Known Risks and Compatibility Decisions

| ID | Final status | React evidence and decision |
| --- | --- | --- |
| `GATE-RISK-001` | `BEHAVIOR_DIFF` | React's Kerberos sidebar now combines the explicit permission, `enableToggleKerberos`, wizard/upgrade mutation policy, and the source-backed `HDPWIN` exclusion (`Sidebar/SideItemList.tsx`). It intentionally does not reproduce Classic's operator-precedence leak, which can add a disabled Kerberos item during an upgrade regardless of Windows stack or permission. A maintainer decision is still required for that upgrade-only difference. |
| `GATE-RISK-002` | `STATICALLY_ALIGNED` | The Admin parent uses one policy-aware OR expression (`components/AdminRouteGuard.tsx:39-69`); each mutation child route adds its own permission and workflow guard (`router/RoutesList.tsx:306-397`). Route tests cover Stack, Service Accounts, Auto Start, and Kerberos deep-link contracts. React retains Auto Start permissions as an explicit extension. |
| `GATE-RISK-003` | `BEHAVIOR_DIFF` | `Services/Actions.tsx:115-166` first accepts any relevant service permission, then applies per-action semantic permissions. This is deliberately stricter than Classic's broad outer guard and must not be regressed to reproduce missing client checks. Server RBAC remains authoritative. |
| `GATE-RISK-004` | `STATICALLY_ALIGNED` | Persisted supports merge over Classic defaults (`store/context.tsx:551-561`); Experimental edits them only with persisted-data mutation authorization (`screens/Experimental/index.tsx:31-77,114-136`). Flags remain non-security inputs. |
| `GATE-RISK-005` | `STATICALLY_ALIGNED` | `components/Navbar.tsx:208-234` uses the Ambari-level permission OR only to expose Manage Ambari navigation. React does not infer or implement AngularJS user/group/role/View CRUD pages. |
| `GATE-RISK-006` | `STATICALLY_ALIGNED` | `authorizationPolicy.ts:28-52` intentionally checks exceptions against the complete expression before applying raw OR permission semantics. Tests pin this Classic contamination behavior. |

## `App.supports` Feature Flags

`constants.ts:150-177` defines the complete Classic defaults. On application
initialization, `store/context.tsx:551-561` overlays the persisted
`user-pref-<login>-supports` object, so the defaults are not permanently
compiled behavior.

| Flag | React status | Consumer or gap |
| --- | --- | --- |
| `enableAddDeleteServices=true` | `STATICALLY_ALIGNED` | Sidebar, Add Service route, Service Actions deletion, and Stack Services all combine the flag with action permission. |
| `enableToggleKerberos=true` | `STATICALLY_ALIGNED` | Sidebar and all Kerberos routes combine the flag with `CLUSTER.TOGGLE_KERBEROS` and workflow guards; the sidebar additionally applies Classic's normal `HDPWIN` exclusion. The upgrade precedence difference is recorded under `GATE-RISK-001`. |
| `preKerberizeCheck=false` | `STATICALLY_ALIGNED` | `KerberosWizard/EnableKerberos.tsx:247` chooses precheck versus direct wizard entry. |
| `kerberosStackAdvisor=true` | `STATICALLY_ALIGNED` | `Kerberos/ConfigureIdentities.tsx:160-223` controls first-load Advisor recommendations. |
| `regenerateKeytabsOnSingleHost=false` | `STATICALLY_ALIGNED` | `Hosts/index.tsx:344` gates the single-host keytab action with Kerberos state. |
| `autoRollbackHA=false` | `PARTIAL` | NNHA consumes the flag and hides Cancel during Classic's critical automatic-rollback phases, preventing an unsafe exit. React still has no reverse-operation rollback state machine, so the flag is not functionally complete. |
| `manageJournalNode=true` | `STATICALLY_ALIGNED` | Manage JournalNodes route and action consume the flag plus topology and permission gates. |
| `preInstallChecks=false` | `STATICALLY_ALIGNED` | Cluster Creation Step 7 alone consumes it, preserving the new-cluster boundary. |
| `customizeAgentUserAccount=false` | `STATICALLY_ALIGNED` | Cluster/Add Host Step 2 and bootstrap payload generation consume it; disabled forces root. |
| `skipComponentStartAfterInstall=false` | `STATICALLY_ALIGNED` | Cluster/Add Host/Add Service install state machines consume install-only behavior and completion weighting. |
| `disableCredentialsAutocompleteForRepoUrls=true` | `STATICALLY_ALIGNED` | Installer Step 1 propagates URL username/password to every valid repository URL only when the persisted flag is explicitly `false`; invalid URLs remain unchanged (`Utils/repositoryCredentials.ts`, `ClusterWizard/Step1.tsx`, focused tests). |
| `alwaysEnableManagedMySQLForHive=false` | `STATICALLY_ALIGNED` | Installer/Add Service Step 7 passes the support flag and `server.os_family` into the Hive initializer. Managed MySQL remains available for the override or supported OS families and is hidden with a safe default on `redhat5`/`suse11`, matching Classic tests (`WizardConfigInitializer.ts`, focused tests). |
| `createAlerts=false` | `STATICALLY_ALIGNED` | Alert action and direct create route combine the flag with alert mutation authorization. Metrics alert types remain excluded. |
| `preUpgradeCheck=true` | `STATICALLY_ALIGNED` | Stack Versions start/check state and menu paths consume the flag. Server-provided checks still require runtime acceptance. |
| `enabledWizardForHostOrderedUpgrade=true` | `STATICALLY_ALIGNED` | Stack Versions restricts Host Ordered wizard availability in its upgrade-method state. |
| `displayOlderVersions=false` | `STATICALLY_ALIGNED` | Host version and Stack Version filters consume the persisted flag. |
| `opsDuringRollingUpgrade=false` | `STATICALLY_ALIGNED` | Shared authorization policy, Navbar, operation route guards, Stack Services, Stack Versions, and Service Actions consume it while preserving Classic's two complete-expression exceptions. |
| `serviceAutoStart=true` | `STATICALLY_ALIGNED` | Sidebar and direct route combine it with service/cluster Auto Start permission; mutation controls retain cluster-level requirements. |
| `enableNewServiceRestartOptions=false` | `BEHAVIOR_DIFF` | False hides the experimental entry; true adds Restart All/Masters/Slaves alongside existing Restart All under semantic `SERVICE.START_STOP`. React implements selectable rolling/Express requests, validates positive schedule IDs, and retains active work and failures for polling or Retry. Exact compatibility decisions and focused evidence are recorded below. |
| `logSearch=true` | `STATICALLY_ALIGNED` | Host Logs requires this flag, installed LOGSEARCH, and `SERVICE.VIEW_OPERATIONAL_LOGS`. |
| `logCountVizualization=false` | `STATICALLY_ALIGNED` | Host Summary renders `HostLogMetrics` only when this flag is true, `logSearch` is enabled, LOGSEARCH is installed, and the user has `SERVICE.VIEW_OPERATIONAL_LOGS`; the component loads per-host operational error/fatal counts with loading, empty, error, and Retry states. This remains in scope and is distinct from excluded Metrics UI. |
| `installGanglia=false` | `OUT_OF_SCOPE` | Legacy Metrics service behavior is excluded. |
| `customizedWidgetLayout=false` | `OUT_OF_SCOPE` | Metrics widgets are excluded. |
| `showPageLoadTime=false` | `OUT_OF_SCOPE` | Development timing display is not a migration acceptance item. |
| `redhatSatellite=false` | `STATIC_ONLY` | Neither executable Classic nor React has a confirmed product consumer. |
| `addingNewRepository=false` | `STATIC_ONLY` | Neither executable Classic nor React has a confirmed product consumer. |
| `kerberosAutomated` | `STATIC_ONLY` | Classic contains only a TODO comment and React correctly does not invent a flag. |

### Experimental Service Restart Compatibility Decision

The support flag is implemented as a deliberate repair of an unfinished
Classic path, not as a claim that every executable defect or ignored control
was reproduced.

| Boundary | Executable Classic behavior | React behavior and evidence |
| --- | --- | --- |
| Visibility and permission | The flag adds the experimental entry alongside existing Restart All, but the entry inherits a broad custom-command/service-check/maintenance/HA permission branch | `Actions.tsx` preserves existing Restart All and shows the new submenu only for semantic `SERVICE.START_STOP`; tests cover flag false/true and all three scopes |
| Parent interaction and label | Clicking the `RESTART_SERVICE` parent immediately selects All, while its `Restart {displayName}s` label blindly appends `s` | React treats the parent only as a submenu toggle, requires an explicit All/Masters/Slaves choice, and uses the service display name without client-side plural concatenation. This avoids accidental submission and malformed service names rather than reproducing the Classic defects |
| Empty groups and component scope | All three fixed submenu entries remain enabled; empty rolling groups close without a request | React disables empty groups. Masters and slaves come from the current service model, clients are excluded, and All deduplicates HDFS ZKFCs that participate in both source groupings |
| HDFS HA order | The intended order is JournalNodes, Standby NameNode/ZKFC, then Active NameNode/ZKFC, but the wrong-cased `isHAEnabled` lookup normally makes that branch unreachable | React reads the actual HA model, promotes ZKFCs from the real `slaveComponents` collection into the master phase, and tests the exact final request order through the DataNode worker batch |
| Rolling request encoding | Master requests use `RESTART {displayName}` while worker batches use `_PARSE_.ROLLING-RESTART...`; the schedule hard-codes interval `1` and tolerance `0` | React uses parseable rolling contexts for both phases so active requests can be recognized, preserves one-host master requests and component-specific worker batches, and applies the validated host batch size, interval, and tolerance |
| Advanced controls | Host mode uses the entered batch size. Rack mode derives `ceil(percentRackStarted * 100 / rackCount)` as a batch size but does not actually group by rack. Interval inputs, retry/count, failure fields, alert suppression, and pause-after-first do not reach the submitted schedule | React exposes a host batch size rather than Classic's misleading rack mode and applies interval and failure tolerance. Empty numeric inputs are rejected while explicit zero remains valid for interval/tolerance. It does not claim to reproduce the omitted rack selector or the visible-but-ignored retry, suppression, and pause controls |
| Express scope | Every scope restarts components returned with maintenance state `OFF` for the service, including clients | React likewise requires explicit `OFF`, excluding `ON` and implied service/host maintenance, but intentionally limits resource filters to the selected master/slave scope and excludes clients; mixed and all-maintenance selections, the exact payload, and the accepted-request lock are tested |
| Submission and recovery | The dialog closes immediately; no local duplicate lock, accepted-schedule tracking, retained error, or in-dialog Retry exists | React requires positive safe-integer IDs from both rolling schedule and accepted Express responses, keeps invalid/rejected submissions open for Retry, and locks accepted work immediately. Poll errors, empty/unknown states, and resumable `PAUSED` schedules retain the lock; only server terminal states `COMPLETED`, `DISABLED`, and `ABORTED` release it |
| Existing request detection | The experimental path has no duplicate guard | React combines service action evidence with rolling component contexts. `PENDING`, `QUEUED`, `IN_PROGRESS`, `HOLDING`, `HOLDING_FAILED`, `HOLDING_TIMEDOUT`, and `PAUSED` all retain the lock. When request evidence includes resource filters, same-named components from another service do not lock the current service; missing filter evidence retains the conservative component match |

These differences require maintainer acceptance and runtime execution against
supported stacks before the row can move beyond `BEHAVIOR_DIFF`.

## Authorization Fix Checkpoint

The Module 13 audit found six mutation/route paths that used raw RBAC instead
of Classic's operation policy. They are resolved in the current worktree:

| Path | Previous risk | Current evidence |
| --- | --- | --- |
| Admin View route | Another wizard owner could enter with upgrade permission. | `AdminViewRouteGuard.tsx:37-47` uses `isAuthorized`; route policy tests retain existing/no-cluster behavior. |
| Admin parent route | Six independent raw checks ignored wizard ownership. | `AdminRouteGuard.tsx:39-69` evaluates one OR expression through the shared policy; tests cover both Auto Start permissions, upgrade visibility, denial, and another owner. |
| User Settings | Default writes and Save used raw persisted-data permission. | `UserSettingsModal.tsx:40-128` gates defaults and Save through `isAuthorized`; component test proves denied users cannot write or save. |
| Experimental | Save had no persisted-data check; Reset duplicated only part of `isAuthorized`. | `Experimental/index.tsx:31-77,114-136` gates both methods and controls; component test proves both are unavailable without mutation capability. |
| Restart Required | Service restart actions used raw `SERVICE.START_STOP`. | `Services/RestartWarning.tsx:61-65,462-482` uses the shared mutation policy. |
| Route Tracker | Workflow routes wrote `USER_REDIRECTION_URL` without persisted-data authorization or owner exclusion. | `AppLoader.tsx:136-157` gates the server write through the shared policy; `AppLoader.test.tsx` covers denied and authorized owners. |

## Five Independent Audit Passes

| Pass | Question | Evidence checked | Result |
| --- | --- | --- | --- |
| 1 | Are all permission, flag, metadata, state, and route gates inventoried? | All 36 Gate IDs, every generated support flag, Classic helpers/templates/routes, React routes/components | 36/36 Gate IDs and 27 support entries are classified; Metrics boundaries are explicit. |
| 2 | Do mutation and read-only authorization semantics match? | `App.havePermissions`, `App.isAuthorized`, React UserContext/policy, route/menu/action call sites | Added one shared policy and corrected six raw mutation call sites; sole-View classification remains an explicit behavior difference. |
| 3 | Do upgrade, wizard ownership, persistence, and recovery compose correctly? | Upgrade exceptions, support override, route guards, user preferences, wizard stores, tests | Whole-expression contamination is pinned and policy-aware mutations, including Navbar and RouteTracker, honor support/ownership rules; real multi-user/crash recovery remains runtime validation. |
| 4 | Are runtime flags and server metadata actually consumed rather than merely defined? | `DEFAULT_SUPPORTS`, persisted merge, all flag references, stack/service/component consumers | Corrected stack-version availability, installability, repository credentials, managed Hive MySQL, the normal Windows Kerberos entry, digital clock, operational-log counts, HAWQ Windows exclusion, and selectable service restart. Automatic rollback remains partial; Federation/HA metadata proxies and service-restart safety repairs are explicit compatibility decisions. |
| 5 | Are server authorization and every role/topology combination proven? | Direct-route guards, request call sites, unit tests, static stack/state scans | Static client coverage is recorded without claiming server RBAC or real-stack parity; the following runtime matrix is mandatory. |

## Static Verification

| Check | Result |
| --- | --- |
| Focused Vitest: authorization policy, Admin guards, RouteTracker, User Settings, Experimental | Passed: 6 files, 19 tests. |
| Focused Vitest: Classic service tags, stack metadata, Choose Services, Admin Versions, Windows Kerberos menu | Passed: 5 files, 16 tests. |
| Focused Vitest: service action lock, repository credentials, managed Hive MySQL | Passed: 4 files, 12 tests. |
| Focused Vitest: experimental service restart flag, scope planning, exact HDFS order, maintenance filtering, input/ID validation, polling recovery, failure/retry, and locking | Passed: 2 files, 23 tests. |
| Focused Vitest: HDFS workflow metadata proxy and API/menu/direct-route boundaries | Passed: 5 files, 19 tests. |
| `npx tsc -b --pretty false` | Passed in the combined worktree after the Module 13 changes. |
| Focused ESLint for the new Module 13 helpers and tests | Passed with no findings. |
| Complete Gate-ID reverse comparison | Passed: 36 baseline IDs, 36 status rows, no missing or extra IDs. |
| `node docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs` | Passed: 1,154 Feature IDs, no warnings or errors. |
| `node --test docs/frontend-refactor/react-current/tools/react-parity-matrix.test.mjs` | Passed: 9 tests. |
| `node docs/frontend-refactor/react-current/tools/validate-react-parity-matrix.mjs` | Not yet applicable: the combined worktree has not generated `react-feature-parity-matrix.json`; generate and validate it only after Modules 10-12 comparison documents are final. |
| Repository-wide ESLint | Not claimed. The repository contains pre-existing lint debt outside this module. |

## Runtime Acceptance Matrix

Every row is `NOT_RUN` until captured against a real Ambari Server. A frontend
403 page, hidden control, or unit mock is not server-authorization evidence.

| Environment | Scenario | Required observation | Status |
| --- | --- | --- | --- |
| No authorization model during login | Slow privilege load | No mutation flashes or protected request occurs before permissions arrive | `NOT_RUN` |
| View-only user | Installed, incomplete, and absent cluster | Views shell is stable; no Installer/View redirect loop; no cluster mutation APIs execute | `NOT_RUN` |
| Cluster User | Every read-only page and deep link | Config/version/history visibility matches returned permissions; mutations remain absent/403 | `NOT_RUN` |
| Service Operator | Services, Hosts, Alerts, Auto Start | Each semantic action appears only for its permission and server scope; denied REST calls return 403 | `NOT_RUN` |
| Cluster Administrator | Admin, workflows, persistence | Role flags elevate correctly but fine-grained IDs and server checks still control operations | `NOT_RUN` |
| Ambari Administrator | No cluster, installed cluster, Admin View | Landing and Manage Ambari work without turning Ambari admin into every cluster permission in the client | `NOT_RUN` |
| Two users/two browsers | Each Installer, Kerberos, upgrade, and HA workflow | Owner continues; other user retains reads but cannot mutate; completion releases ownership | `NOT_RUN` |
| Rolling upgrade | Flag false/true, running/holding/suspended | Ordinary and exception permissions follow the matrix, including combined-expression contamination | `NOT_RUN` |
| Stack with no versions | Host and Admin Versions deep links | Both surfaces hide/redirect consistently after loading completes | `NOT_RUN` |
| Upgrade history | None, sole active, finished, older finished plus active | History tab and retry behavior match the server collection | `NOT_RUN` |
| Windows and non-HDP stacks | Kerberos, HA, Federation, HAWQ actions | Stack/service-type and family restrictions match supported capabilities | `NOT_RUN` |
| Service metadata variants | Classic hard-coded tags plus HDFS/HAWQ stack proxies present/absent | Menu and direct route agree; verify that stricter React proxies do not hide supported Classic workflows and unsupported workflows cannot start | `NOT_RUN` |
| Component metadata variants | Cardinality, reassignable, decommission, custom commands | Menus, target selection, and direct routes enforce the same metadata | `NOT_RUN` |
| Maintenance and heartbeat transitions | Host/service/component scopes | Actions update without stale permission/state leaks and unsafe operations remain blocked | `NOT_RUN` |
| Existing background operation | Repeat service/host action | Existing request is shown or the duplicate action is disabled; no duplicate request is accepted | `NOT_RUN` |
| Experimental service restart | Flag false/true; All/Masters/Slaves; Rolling/Express; HDFS HA; empty group; rejected request | Visibility, selected resource filters, master/worker ordering, schedule settings, accepted lock/release, progress, and Retry match the documented React compatibility decision | `NOT_RUN` |
| Automatic and Manual Kerberos | Install/Add Host/Add Service/Reassign/HA | KDC/session gates, cancellation, credential replay, and owner recovery converge | `NOT_RUN` |
| Browser refresh/server restart | Every long workflow | Step/request/owner checkpoint restores without duplicate mutation or stale overwrite | `NOT_RUN` |

## Unresolved Cross-Module Risks

Navbar's Manage Ambari/Settings gate now consumes the shared policy at the
Modules 01/12 boundary. The current `clusterProvisioningRedirect` change
exempts View-only routes and resolves the previously suspected Installer/View
redirect loop.

The static result is not a claim of complete authorization parity. Completion
requires resolving the `PARTIAL`/`MISSING` rows and executing the role,
upgrade, wizard, stack, state, and server-RBAC matrix above.
