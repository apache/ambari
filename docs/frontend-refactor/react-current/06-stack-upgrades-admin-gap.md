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

# React Stack Upgrades and Admin Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/06-stack-upgrades-admin.md` |
| React implementation | `ambari-web/latest` at `4fb6dadf190007b831fdd2d08a9a9c6431b252b9` |
| Feature IDs | 43 non-Metrics IDs from `STACK-SVC-001` through `ADMIN-AUTO-004` |
| Review date | 2026-08-17 |
| Metrics boundary | Metrics, Heatmaps, metric display data, and Metrics widget management are excluded |
| Delivery | ASF JIRA `AMBARI-26630`; branch `AMBARI-26630`; PR base `frontend-refactor` |

This audit compared every Module 06 feature with the executable Classic source,
the React routes/components/hooks/API layer, and the Ambari Server resource
providers. It also cross-scanned all five authoritative network inventories:
named AJAX definitions, AJAX call sites, direct HTTP calls, browser network
entry points, and realtime channels. The generated Stack and Upgrades API page
was used only for candidate discovery.

`/main/admin/authentication`, `/main/admin/advanced`, and `/main/admin/audit`
remain Classic placeholders. They are not Module 06 React requirements. The
separate AngularJS Admin View reached by Manage Versions is also not reimplemented
inside React.

## Initial Static Conclusion

| Status | Count |
| --- | ---: |
| `STATICALLY_ALIGNED` | 8 |
| `PARTIAL` | 24 |
| `INCORRECT` | 7 |
| `MISSING` | 4 |
| Total | 43 |

`STATICALLY_ALIGNED` means that the reviewed source and request contract agree;
it does not mean that the feature passed against a real cluster. `INCORRECT`
means that a reachable React path contradicts Classic or the backend contract,
not merely that presentation differs.

The audit found one authored-baseline error. The only executable consumer of
`supports.disableCredentialsAutocompleteForRepoUrls` is Classic Installer Step
1 at `app/views/wizard/step1_view.js:327`. Neither Module 06 repository editor
reads the flag. `ember-baseline/13-permissions-flags.md` is corrected with this
source evidence; generated evidence already listed only the actual caller and
did not require regeneration.

## Feature Status

### Stack Services and Repositories

| ID | Status | Classic behavior, contract, and boundary | Current React behavior and gap | Executable acceptance |
| --- | --- | --- | --- | --- |
| `STACK-SVC-001` | `STATICALLY_ALIGNED` | `/main/admin/stack/services` combines stack services with current repository-summary installation state and displays service, version, status, and description. Initial data comes from stack/repository model loads. | `ListStack` GETs cluster stack versions with repository versions, selects `CURRENT`, and renders the same four columns. Empty-current and request failures are surfaced, but live stack shapes remain unverified. | Given a current repository version with installed and uninstalled services, assert the rendered versions, status, descriptions, empty-current error, and failed-load recovery. |
| `STACK-SVC-002` | `PARTIAL` | The Add Service link requires `SERVICE.ADD_DELETE_SERVICES` and `supports.enableAddDeleteServices`; the click routes Kerberos through its own wizard and other services through Add Service while retaining the Stack return path. | React checks the permission and preselects the service, while the Add Service route checks the flag. The Stack link itself ignores the flag, Kerberos specialization, active upgrade/wizard ownership, and return path. | Test permission/flag truth table, Kerberos and ordinary services, conflicting wizard/upgrade, direct route, preselection, cancel, and return to Stack. |
| `STACK-SVC-003` | `PARTIAL` | When `App.stackVersionsAvailable` is false, Classic loads `/operating_systems?fields=repositories/*,OperatingSystems/*` and displays repository ID, base URL, and OS grouping. Version details also expose repository metadata. | React has no `stackVersionsAvailable` fallback on the Stack tab. `RepoModal` shows OS, name, and base URL from a selected version but omits mirror-list/fallback behavior. | Exercise both runtime-gate values and assert OS/repository/base URL/mirror metadata, no-version loading, and retry after load failure. |
| `STACK-SVC-004` | `INCORRECT` | The Versions edit entry requires `AMBARI.MANAGE_STACK_VERSIONS`; popup Save is limited to Ambari admins and excludes cluster operators. The old Stack fallback editor has no template permission gate and supports edit, clear, cancel/revert, validate, and save. | React opens `RepoModal` under `CLUSTER.UPGRADE_DOWNGRADE_STACK`, not `AMBARI.MANAGE_STACK_VERSIONS`; it does not enforce the admin/operator Save rule or implement restore/clear. The Stack fallback is absent. PUT shape is otherwise compatible. | Role-test Ambari admin, cluster operator, stack-version manager, upgrade operator, and viewer; assert entry/Save separately, restore/clear/cancel, exact PUT, and rejected-save retry. |
| `STACK-SVC-005` | `PARTIAL` | Versions validation POSTs each selected repository with `Repositories.base_url` and `repo_name`; invalid URLs remain marked and the user may explicitly skip validation before saving. The old Stack editor offers Save Anyway/Revert/Cancel after a validation failure. | React validates all repositories concurrently and supports an explicit preselected Skip option. A failure becomes a toast without per-repository state or the post-failure Save Anyway/Revert/Cancel recovery. | Assert exact validation URLs/payloads, mixed pass/fail results, per-row errors, explicit skip only, save retry, and no PUT before validation or skip succeeds. |

### Versions and Package Installation

| ID | Status | Classic behavior, contract, and boundary | Current React behavior and gap | Executable acceptance |
| --- | --- | --- | --- | --- |
| `VER-LIST-001` | `PARTIAL` | Classic merges cluster stack versions and repository versions, hides `hidden=true`, optionally filters older incompatible versions, and renders NOT_INSTALLED, INSTALLING, INSTALLED, CURRENT, UPGRADING, UPGRADED, INSTALL_FAILED, and OUT_OF_SYNC semantics. | React performs a local version comparison and renders most server states, but never loads the compatibility set, ignores `supports.displayOlderVersions`, assumes nonempty nested arrays, and does not derive UPGRADING/UPGRADED repository status correctly. | Assert every state, PATCH/MAINT/STANDARD and cross-stack compatibility, `hidden`, both flag values, missing nested data, polling convergence, and load retry. |
| `VER-LIST-002` | `INCORRECT` | The seven filters map to concrete derived states: Not Installed, All, Upgrade Ready, Installed, Current, UPGRADING, and UPGRADED, with live counts. | React's In Process and Ready to Finalize filters compare stack state to display strings (`Upgrade/Downgrade in Progress` and `Ready to Finalize`), so they cannot match server records. Upgrade Ready also expects a non-server `UPGRADE_READY` state instead of deriving it from INSTALLED/current version. | Feed all state combinations and assert exact membership/counts for all seven categories after polling updates. |
| `VER-LIST-003` | `PARTIAL` | Expanding a version shows status, service versions, repositories/OS data, host states, and the compatibility/state reasons that disable actions; PATCH, MAINT, SERVICE, and STANDARD differ. | React shows service versions in the grid and a small host-count/repository edit modal, but omits complete repository/service details and unavailable-action reasons. | Assert expanded content and disabled reasons for all repository types, incompatible jumps, no current version, concurrent install, and upgrade-in-progress. |
| `VER-LIST-004` | `STATICALLY_ALIGNED` | Current/Installed/Not Installed host counts open a host list and can navigate to the matching Hosts filter; zero counts are disabled. | React calculates the three lists, disables zero counts, shows hostnames, and routes to `/main/hosts/version/:versionName/:versionStatus`. Host filtering is owned by Module 03 and has focused tests there. | Integration-test encoded version names, each status, empty lists, Back navigation, and the Module 03 filtered results. |
| `VER-LIST-005` | `PARTIAL` | After confirmation, authorized users POST `ClusterStackVersions.{stack,version,repository_version}`. Server orchestration skips maintenance/inapplicable hosts; request ID is persisted and progress survives refresh. Duplicate installs are blocked. | React sends the correct POST and wraps it in persisted operation progress. Several install/reinstall entries omit a local `CLUSTER.UPGRADE_DOWNGRADE_STACK` check, and restored operation ownership is a global array not tied to repository/request reconciliation. | Assert permission on every entry, confirmation, exact POST, maintenance/inapplicable hosts, duplicate clicks, timeout/error/Retry, refresh restore, and terminal reconciliation. |
| `VER-LIST-006` | `STATICALLY_ALIGNED` | Host Stack Versions POSTs one `HostStackVersions` record for the selected host and repository version. | Module 03 uses encoded cluster/host paths and the correct payload in `versionsApi.installHostStackVersion`; focused API and Host Stack Versions tests exist. | Run the Module 03 single-host install scenarios on a live cluster, including rejected install and refresh recovery. |
| `VER-LIST-007` | `INCORRECT` | OUT_OF_SYNC recovery confirms affected hosts, obtains the KDC session when needed, and bulk-updates or deletes only components matching `HostRoles/state=INSTALL_FAILED`. Contrary to the earlier written baseline, executable Classic code performs no client-side component-cardinality or minimum-instance check here (`stack_and_upgrade_controller.js:1646-1693`; controller tests at `:3800-3860`). | React sends the same state-constrained bulk mutations and now obtains the KDC session, but operation progress, no-op feedback, rejection recovery, and refresh reconciliation still require completion and runtime proof. | Kerberized and non-Kerberized tests must cover mixed component states, exact query/body, no-op response, request progress, rejection, Retry, and refresh reconciliation. |
| `VER-LIST-008` | `MISSING` | Hide confirms and PUTs `RepositoryVersions.hidden=true`; it is available only for unused/failed repository versions and never deletes the resource. | React exposes no Hide/Discard action and its API layer only has an unrelated DELETE helper. | Assert eligible PATCH/MAINT/unused/failed states, in-use/CURRENT denial, confirmation, exact PUT, failure retention/Retry, and disappearance after refresh. |
| `VER-LIST-009` | `STATICALLY_ALIGNED` | Manage Versions confirms leaving Cluster Management, loads the Ambari Server version, and navigates to the ADMIN_VIEW instance. | React confirms and uses the authenticated `/adminView?page=stackVersions` redirect path; the shared guard resolves the installed Admin View version and authorization. | Browser-test missing/one/multiple Admin View versions, insufficient privilege, popup cancellation, redirect failure, and return to Dashboard. |

### Upgrade Start and Pre-Checks

| ID | Status | Classic behavior, contract, and boundary | Current React behavior and gap | Executable acceptance |
| --- | --- | --- | --- | --- |
| `UPG-START-001` | `PARTIAL` | Classic loads the compatible repository set for the current stack, then obtains server-supported upgrade types for a selected installed target. Incompatible direct jumps and uninstalled targets cannot start. | React calls the supported-types endpoint after selection but never uses its compatible-versions API. Its local predicate permits every different stack and can expose an incompatible target. | Assert compatible/incompatible same-stack and cross-stack targets, installed requirement, empty/malformed compatibility responses, and server rejection without opening Start. |
| `UPG-START-002` | `PARTIAL` | Server upgrade types drive Rolling, Express, and Host Ordered choices; Host Ordered additionally requires `supports.enabledWizardForHostOrderedUpgrade`. | React filters by server types, but Host Ordered is hard-coded `allowed=false` and the flag is never read. Method selection state also survives between unrelated targets. | Test every server type set and both flag values, target switches, no-supported-method state, and exact selected `upgrade_type`. |
| `UPG-START-003` | `PARTIAL` | The shared Classic Upgrade Options template displays both slave-component and service-check tolerance controls for the upgrade flow generally (`upgrade_options.hbs:65-81`); it does not gate them per method. Both values are serialized as `skip_failures` and `skip_service_check_failures`. | React correctly exposes both controls and sends string booleans in the matching fields, but must reliably reset them between unrelated targets. | Assert both controls for every supported method, target-switch state reset, confirmation text, and exact POST/active-upgrade PUT payloads. |
| `UPG-START-004` | `INCORRECT` | When `supports.preUpgradeCheck` is false Classic starts directly; otherwise checks classify FAIL, WARNING, and BYPASS, block required failures, allow warning/bypass confirmation, support rerun, and expose config merge details. | React always runs checks, uses inconsistent `ERROR` versus server/Classic `FAIL` detection, caches results without invalidation, and can enable Proceed while a request is still loading. | Flag-test direct start, classify all statuses, block FAIL, confirm WARNING/BYPASS, rerun after server change, reject/stale response handling, target switch, and unmount cancellation. |
| `UPG-START-005` | `MISSING` | Classic adds client checks for maintenance mode, heartbeat/removal, previous upgrade, component installation, and service checks, with item-specific repair actions. The previous-upgrade Finalize button is a documented broken binding and must not be copied. | React only renders generic server check results/config merge data; none of the custom check views, repair requests, or host/service detail flows exist. | Exercise every check type, item details, authorized repair and rerun, repair rejection, no-host/no-service cases, and explicitly assert no invented previous-upgrade Finalize action. |
| `UPG-START-006` | `PARTIAL` | An authorized user confirms, sees notification suppression and Express downtime warnings, and POSTs repository ID, type, tolerance fields, and UPGRADE direction with a long timeout. Failure leaves the target recoverable. | React sends the correct core payload and warnings, but mutation permission is inconsistently enforced across entries, duplicate submission is not blocked, and no explicit long timeout/cancellation ownership exists. | Assert all entry permissions, Rolling/Express warnings, double-click, timeout, server rejection, Retry, and one created upgrade ID. |
| `UPG-START-007` | `PARTIAL` | Downgrade requires `downgrade_allowed`, aborts the current upgrade first, waits for ABORTED, then POSTs DOWNGRADE; an aborted/failed upgrade can be retried with request status PENDING. | React implements abort/poll/start downgrade and suspended resume, but the polling interval has no timeout/unmount/error cleanup, abort failure is swallowed, and non-suspended aborted/failed upgrade recovery is incomplete. | Test allowed/denied downgrade, abort failure, poll failure/timeout/unmount, duplicate clicks, retry failure, and exactly one downgrade creation. |
| `UPG-START-008` | `PARTIAL` | PATCH/MAINT start uses repository/service semantics; Revert confirms affected services and POSTs `Upgrade.revert_upgrade_id`. Finalized/non-revertible versions cannot revert. | React starts PATCH with the generic payload and implements the correct revert field plus affected-service table. Eligibility, target identity persistence, permission, and failure reconciliation remain incomplete. | Test PATCH/MAINT service subsets, supported/not-supported revert, finalized state, exact payload, rejection/Retry, refresh, and correct resulting upgrade identity. |

### Upgrade Execution State Machine

| ID | Status | Classic behavior, contract, and boundary | Current React behavior and gap | Executable acceptance |
| --- | --- | --- | --- | --- |
| `UPG-RUN-001` | `PARTIAL` | The wizard loads Upgrade, non-PENDING groups, items, progress, direction, and state; lightweight/full REST polling continues without overlapping requests and stops on route teardown/terminal completion. | `useUpgrade` polls the matching full URL sequentially and merges groups. It also performs a duplicate initial fetch, does not pause polling at terminal state before reload, and turns repeated poll failures into transient toasts without a stable Retry state. | Assert initial/periodic call counts, no overlap, PENDING group exclusion, state transitions, repeated failures/Retry, terminal stop, unmount, and route reopen. |
| `UPG-RUN-002` | `PARTIAL` | Groups/items/tasks expand lazily; each task exposes host, role, command, status, stdout/stderr, Copy and Open. Structured output is reserved for failure summaries. | React lazy-loads tasks/logs and provides Copy/Open, but the current-item shortcut fetches only the first task log and local task/log state can leak when the active item changes. | Multi-task groups must verify each task/log, item switch, missing logs, rejected lazy loads/Retry, copy fallback, popup blocking, and no raw structured output. |
| `UPG-RUN-003` | `STATICALLY_ALIGNED` | A HOLDING manual item displays server instructions; checked Proceed PUTs `UpgradeItem.status=COMPLETED`. Classic clears the checkbox before completion and relies on error handling without rollback. | React parses instructions, requires the checkbox, sends the exact PUT, clears it immediately, and retains server state on failure with an error toast. | Assert plain/JSON instructions, checkbox gate, exact PUT, rejected PUT with a second attempt, and polling convergence. |
| `UPG-RUN-004` | `PARTIAL` | Failed items can Retry to PENDING; skippable HOLDING_FAILED/HOLDING_TIMEDOUT items can continue as FAILED/TIMEDOUT; task/failed-host details remain available. | React implements the status transforms, retry, and details. Controls are not consistently disabled while all asynchronous detail/status work is active, and failed detail fetches have no visible Retry. | Cover every failed/timed-out/skippable state, non-skippable denial, duplicate clicks, details errors, exact PUT, rejection, and refreshed state. |
| `UPG-RUN-005` | `INCORRECT` | Pause confirms a strong cluster warning and PUTs ABORTED with `suspended=true`. On failure Classic shows a dedicated error and keeps the running wizard state. | React's inner suspend helper catches and swallows rejection; the outer function then sets local ABORTED and closes the wizard as if pause succeeded. | Inject PUT rejection and assert the wizard remains open/running with Retry; success must set suspended only after response and prevent duplicate pause. |
| `UPG-RUN-006` | `PARTIAL` | Resume always PUTs current Upgrade to PENDING. Classic can remain stuck pending after failure because it does not reset its request flags; React should intentionally fix that defect. | React sends the exact PUT and changes state only after success, avoiding the Classic stuck flag, but has no in-flight disablement, visible failure, or explicit Retry. | Assert one PUT per click, disabled in flight, rejected response leaves suspended state and exposes Retry, successful convergence, and refresh. |
| `UPG-RUN-007` | `STATICALLY_ALIGNED` | Abort is used only as the prerequisite for downgrade and by the broken previous-upgrade repair path; the normal wizard has no generic Stop/Abort button. | React uses abort only for downgrade and does not invent a generic Abort action. | Assert the normal wizard never exposes Abort and downgrade invokes the exact ABORTED/`suspended=false` PUT. |
| `UPG-RUN-008` | `PARTIAL` | Closing the modal leaves server orchestration running; Versions can reopen it from the persisted current upgrade. `/main/admin/stack/upgrade` restores the modal and closing a terminal wizard reloads/reset ownership. | React closing unmounts polling and Versions can reopen from global REST state. There is no functional `stack/upgrade` route restoration; `stack/:tabName` treats `upgrade` as an ordinary unknown tab. | Close/reopen during every active state, refresh/deep-link to upgrade, browser Back/Forward, terminal close, ownership reset, and no orphan polling. |
| `UPG-RUN-009` | `MISSING` | When another user starts an upgrade, Classic shows the initiator and makes non-owner windows read-only while still permitting authorized viewing. | React loads generic `wizard-data` ownership but upgrade creation does not claim it, the Module 06 pages do not display the initiator, and mutation controls do not check `isNonWizardUser`. | Two-user browser test must show initiator, deny all non-owner mutations/direct routes, retain read-only progress/logs, transfer/reset ownership, and recover after refresh. |
| `UPG-RUN-010` | `PARTIAL` | Upgrade state suppresses notifications and ordinary operations; `supports.opsDuringRollingUpgrade` relaxes selected operations while backend authorization remains final. | React shows the suppression warning and several other modules check upgrade state, but Module 06 never reads `opsDuringRollingUpgrade`; gating is distributed and inconsistent for install, repository, Auto Start, and Accounts paths. | Role/flag matrix must cover every Module 06 mutation plus representative Host/Service operations during running, holding, suspended, completed, and no-upgrade states. |
| `UPG-RUN-011` | `INCORRECT` | Final risk loads completed failed/aborted/timed-out SERVICE_CHECK tasks from `admin.upgrade.service_checks`, plus structured failed hosts, then offers repair/pause, downgrade, or ignore/continue as state permits. | React never calls the service-check endpoint. Its structured-output parser writes into uninitialized local variables, so service names can remain undefined and the failure summary can throw or spin forever; Continue is absent in the service-check branch. | Assert service-only, host-component-only, mixed and empty failures, exact GET, all permitted decisions, rejected detail load/Retry, pause/downgrade, and safe rendering. |
| `UPG-RUN-012` | `PARTIAL` | Finalize checks risk acknowledgement and PUTs the final UpgradeItem to COMPLETED; Finalize Later reuses suspend. Revertible upgrade text states finalization is irreversible. | React provides acknowledgement, exact item PUT, revertible text, and Finalize Later. It inherits the incorrect suspend failure handling and does not block duplicate Finalize submissions. | Assert upgrade/downgrade/PATCH messages, exact PUT, duplicate click, rejection/Retry, Finalize Later failure, resulting desired repository version, and refresh. |

### Upgrade History and Cluster Admin

| ID | Status | Classic behavior, contract, and boundary | Current React behavior and gap | Executable acceptance |
| --- | --- | --- | --- | --- |
| `UPG-HIST-001` | `PARTIAL` | When completed history exists, direct GET `/clusters/{cluster}/upgrades?fields=Upgrade` renders direction, type, repository/type, per-service versions, status, and start/duration/end. Classic can leave its spinner on GET failure. | React renders the same information and derives repository type from loaded stacks. Both GETs are unguarded; either rejection leaves loading forever, and unknown/removed repositories are not handled safely. | Assert empty/mixed history, removed repository versions, running end time, request failure with visible Retry, retry success, and permission denial. |
| `UPG-HIST-002` | `STATICALLY_ALIGNED` | Exactly nine categories exist: All, Upgrade, Downgrade, and Successful/Aborted/Failed separately for each direction. | React defines and applies the same nine conjunctions. | Feed every direction/status combination and assert membership/count after a refreshed list. |
| `UPG-HIST-003` | `PARTIAL` | Selecting history calls `admin.upgrade.data` by request ID, then lazily loads group/item/task state and logs. Classic's unused direct one-record URL is not a contract. Detail failure may leave a spinner. | React correctly calls the full upgrade endpoint and renders read-only groups/tasks, but pauses polling through an asynchronous state change that can still schedule another request; load/detail failures render no recoverable state. | Assert one full detail load, no background polling in read-only mode, lazy task/log requests, rejection/Retry, close/unmount, and selection of a second record. |
| `ADMIN-ACCT-001` | `STATICALLY_ALIGNED` | `/main/admin/serviceAccounts` requires `SERVICE.SET_SERVICE_USERS_GROUPS`, loads current config tags/values, and redirects without permission. | React route guard uses the exact permission and the page loads current configs through shared stack/config hooks. | Assert menu and direct route for allowed/denied users, initial load failure/Retry, and empty configs. |
| `ADMIN-ACCT-002` | `INCORRECT` | Classic filters `displayType=user` and category `Users and Groups`, preserves the order produced by stack/config definitions, and displays Name/Value read-only with no Save. | React filters the right rows and is read-only, but alphabetically re-sorts by display name and can remain on a spinner forever when either dependency fails. | Assert definition order, invisible-row exclusion, duplicate labels, empty list, load failure/Retry, and absence of edit/save controls. |
| `ADMIN-AUTO-001` | `PARTIAL` | Menu visibility requires START_STOP or MODIFY_CONFIGS, an auto-start permission, and `supports.serviceAutoStart`; the child route accepts either auto-start permission without checking the flag. The Classic parent-route inconsistency can still block an auto-start-only deep link. | React matches the menu and intentionally applies the flag to direct routing, but `AdminRouteGuard` omits both auto-start permissions and can block the child before its correct guard. | Test menu/direct route for every permission pair, flag values, upgrade states, and document the intentional stricter direct flag behavior. |
| `ADMIN-AUTO-002` | `PARTIAL` | Classic displays installed restartable non-client components, grouped by service, plus global and per-component toggles. Service-only permission permits viewing, but all controls require `CLUSTER.MANAGE_AUTO_START`. | React applies the cluster mutation gate and renders grouping/toggles, but includes all returned components without stack `isRestartable`, client, or total-count filtering. Load errors only log to the console. | Assert client/uninstalled/non-restartable exclusion, grouping/order, service-only read-only view, select-all, load error/Retry, and live component metadata. |
| `ADMIN-AUTO-003` | `PARTIAL` | Save may run cluster-env, enable-set, and disable-set requests in parallel. The popup closes immediately; cache/transition updates occur only if all succeed, so partial server mutation is possible and must be reconciled. | React sends compatible component/config payloads sequentially and refreshes only after all succeed, but has no catch/recovery UI, no save-in-flight disablement, and the Modal callback does not await the promise. Partial success can leave stale client state. | Fault-inject each of the three requests and combinations; assert exact bodies, disabled duplicate Save, visible partial result, server reconciliation, Retry, and cache update only after convergence. |
| `ADMIN-AUTO-004` | `MISSING` | Leaving with unsaved changes blocks transition and offers Save, Discard, or Cancel. Save continues only after all requests succeed; Discard restores cached values; Cancel remains on the page. | React offers Save/Discard only after pressing its Save button. Browser/sidebar/route navigation discards local edits without warning, and Back/refresh have no protection. | Modify each setting, then exercise sidebar, direct route, Back, refresh, Save success/failure, Discard, Cancel, and repeated navigation. |

## Backend Contract Comparison

| Workflow | Classic and Server contract | React result |
| --- | --- | --- |
| Stack/repository load | Cluster stack versions, global stack repository versions, operating systems, and compatible repository versions are separate resources | Core cluster load exists; compatibility and Stack fallback repository load are missing |
| Repository validation/save/hide | POST repository `?validate_only=true`; PUT repository version with operating systems; PUT `RepositoryVersions.hidden=true` | Validation/save exist; permission/recovery are wrong and Hide is missing |
| Install packages | POST cluster stack versions with `ClusterStackVersions` and follow returned Request | Payload matches; permission and restored request reconciliation are incomplete |
| Upgrade checks/start | GET compatible versions, GET supported types, optional GET checks, then POST `Upgrade` | Supported types/check/start exist; compatibility, flag behavior, custom checks, and request ownership are incomplete |
| Upgrade control | PUT Upgrade for abort/suspend/retry/options; PUT UpgradeItem for Retry/Continue/Proceed/Finalize | URLs and bodies match; suspend error handling, concurrency, service-check summary, and ownership are defective |
| History/details | GET Upgrade history and GET full Upgrade/groups/items/tasks by request ID | Core reads match; failure/retry and read-only polling teardown are incomplete |
| Auto Start | current cluster-env plus component category; one config PUT and up to two component PUTs | Core payloads match; filtering, partial-failure reconciliation, and route-leave blocking are incomplete |

The backend resource providers accept the current core field names:
`Upgrade/request_status`, `Upgrade/suspended`, `Upgrade/skip_failures`,
`Upgrade/skip_service_check_failures`, `Upgrade/revert_upgrade_id`, and
`UpgradeItem/status`. React must keep server authorization as the final boundary;
client permissions and feature flags are visibility and workflow controls only.

## Permissions, Flags, and State Boundaries

| Boundary | Classic contract | React gap and required decision |
| --- | --- | --- |
| Stack route | OR of `CLUSTER.VIEW_STACK_DETAILS` and `CLUSTER.UPGRADE_DOWNGRADE_STACK`; running upgrade may keep Admin visible | Route uses the same OR, but mutation controls need separate semantic gates |
| Repository editing | `AMBARI.MANAGE_STACK_VERSIONS` entry; admin/non-operator Save; unguarded legacy fallback | React incorrectly uses upgrade permission; preserve the two distinct Classic paths or deliberately retire the fallback with documented acceptance |
| Package/upgrade mutation | `CLUSTER.UPGRADE_DOWNGRADE_STACK`, current-state eligibility, and wizard ownership | Several React entry points omit one or more checks |
| Add Service link | `SERVICE.ADD_DELETE_SERVICES` AND `enableAddDeleteServices` | Link ignores the flag even though its target route checks it |
| Pre-checks | `supports.preUpgradeCheck`; Host Ordered uses `enabledWizardForHostOrderedUpgrade` | Neither flag is honored by Module 06 React |
| Version visibility | `displayOlderVersions`, compatibility, `hidden`, current version, repository type | React hard-codes a partial local comparison |
| Auto Start | Menu predicate, child-route OR permission, cluster-only mutation controls | Parent guard omits auto-start capabilities; component filter is incomplete |
| Upgrade/wizard state | owner can mutate; other users are read-only; operations usually blocked unless a documented exception/flag applies | Module 06 does not claim/display/enforce upgrade ownership and applies upgrade blocking inconsistently |

## Async Lifecycle and Recovery

| Concern | Required behavior | Current risk |
| --- | --- | --- |
| Polling | One request at a time; stop on unmount/terminal; keep a visible recoverable error | Core polling is sequential, but duplicate initial calls, terminal reload, and toast-only repeated failures remain |
| Upgrade mutations | Disable duplicate actions; update local state only after success; retain a Retry path | Suspend falsely succeeds on rejection; several other controls allow duplicates or silently log failure |
| Downgrade abort wait | Bounded polling with cleanup and explicit abort/start errors | Raw `setInterval` has no timeout, unmount cleanup, or poll-error recovery |
| Refresh/reopen | Reconcile persisted ID with server state and current user ownership | Upgrade ID is restored, but upgrade owner and package request identity are incomplete |
| History/read-only | One detail load, lazy task/log requests, no progress polling | Pause is state-driven and failures have no visible recovery |
| Auto Start partial save | Report which writes succeeded, reload authoritative values, permit Retry | Sequential writes can partially succeed and leave an unhandled promise/stale UI |
| Route leave | Active upgrade continues; unsaved Auto Start blocks and offers Save/Discard/Cancel | Upgrade modal mostly unmounts correctly; Auto Start leave protection is absent |

## Five Independent Audits

| Pass | Independent entry | Inputs | Findings incorporated |
| --- | --- | --- | --- |
| 1. Pages, routes, and actions | Classic router/menu/templates versus React router/sidebar/components | Admin children, Stack tabs, upgrade modal/deep link, every visible action | Found missing upgrade deep-link restoration, custom checks, Hide, other-user display, and Auto Start leave blocker |
| 2. Controller/service/model state | Classic controllers/views/models/tests versus React hooks/context/component state | Repository status derivation, upgrade groups/items, package persistence, history filters, config caches | Found invalid filter states, missing compatibility derivation, stale method/check state, and unsafe structured-output locals |
| 3. URL/method/query/payload | Five generated network layers, AJAX registry/callers, React API layer, Server resource providers | Every Module 06 read/mutation and browser redirect; Metrics excluded | Confirmed core mutation bodies, and found missing compatibility/service-check/hide/custom-check interfaces plus permission-misaligned reachable calls |
| 4. Permissions, flags, and boundaries | Generated permissions/flags/routes, Classic guards, React guards and all mutation entries | Role OR semantics, runtime gates, repository types, upgrade states, owner state | Found repository permission mismatch, ignored Module 06 flags, Auto Start parent-guard gap, and missing upgrade ownership enforcement |
| 5. Errors, retries, concurrency, refresh, and leave | Promise callbacks, pollers/timers, persistence, teardown, Classic tests, React test inventory | Rejection at every stage, duplicate actions, partial saves, Back/refresh/unmount | Found false-success suspend, unbounded downgrade polling, spinner-only failures, absent read-only detail recovery, and no Module 06 focused component/hook tests |

## Runtime Acceptance Matrix

No row below passed during this static audit. These scenarios require a real
Ambari Server, Agents, stack repository metadata, and in several cases two
authenticated users or fault injection.

| Scenario | Roles/flags/state | Live operations and evidence | Expected result |
| --- | --- | --- | --- |
| Stack and repositories | Viewer, upgrade operator, stack-version manager, cluster operator, Ambari admin; stack versions present/absent | Load Stack, edit/validate/save/revert/clear URLs, fail validation and PUT | Exact visibility and Save rules; no unauthorized mutation; fallback behavior and recovery agree with the documented decision |
| Version inventory | Standard/PATCH/MAINT, compatible/incompatible stacks, every version state; `displayOlderVersions` both values | Poll stack/repository/compatibility APIs and apply all filters | Exact records, counts, reasons, hidden behavior, and stable refresh |
| Package install | Maintenance and healthy hosts, supported/unsupported services, Kerberized cluster | Install/reinstall, refresh mid-request, inject timeout/failure, retry | One request, skipped hosts honored, progress restored, KDC/safety rules enforced |
| Upgrade method selection | Rolling/Express/Host Ordered; both flags; server type permutations | Compatibility, supported types, tolerance controls, checks and start | Only eligible target/type starts with exact payload; no duplicate upgrade |
| Custom pre-checks | Maintenance, heartbeat, previous upgrade, component install, service-check failures | Repair, rerun, rejection, cancel | Item-specific details/actions recover correctly; broken Classic Finalize is not reproduced |
| Active upgrade | Running, holding, failed, timed out, suspended, aborted, finalize; upgrade and downgrade | Logs, Retry, Continue, Proceed, pause/resume, downgrade, finalize | Exact state PUTs, no overlap, failure remains retryable, final desired version converges |
| Revert | Revertible/non-revertible/finalized PATCH and MAINT | Start revert, reject, retry, refresh | Exact `revert_upgrade_id`, affected services, and eligibility |
| Two-user ownership | Owner and authorized non-owner in two browsers | Start, refresh, deep link, operate, close, complete | Initiator shown; non-owner read-only; ownership restored and cleared correctly |
| Realtime interruption | Connected, disconnected, reconnect during upgrade | Drop STOMP, change state server-side, reconnect and refresh | REST polling remains authoritative; no stale mutation or duplicate modal |
| History | Mixed direction/status/type, removed repository metadata | Filter, open details, load logs, inject failures, select second entry | Nine filters and read-only details work with visible Retry and no leaked polling |
| Service Accounts | Allowed/denied roles, empty/malformed config metadata | Load, fail/retry, inspect order | Definition order and read-only values are preserved; no mutation is possible |
| Auto Start | Service-only and cluster permission; flag both values; client/uninstalled components | Toggle all/individual/global, partial-fail each request, navigate away | Correct filtering/permissions, authoritative partial-save recovery, and Save/Discard/Cancel leave behavior |

## Implementation Plan

1. Extract typed, testable Module 06 state and contract helpers for version
   visibility/filters, upgrade eligibility/check classification, upgrade state
   transitions, component failure summaries, and Auto Start diffs.
2. Correct route/menu/permission/flag/owner gates, add deep-link and unsaved-leave
   guards, and preserve documented intentional fixes rather than reproducing
   Classic defects.
3. Complete missing interfaces and recovery paths: compatibility, Hide,
   custom checks, service-check summary, repository fallback/edit recovery,
   upgrade mutation concurrency, and Auto Start reconciliation.
4. Add focused API, helper, hook, route, and component tests covering success,
   rejection, retry, duplicate action, refresh/unmount, and partial success.
5. Run focused and full Vitest, TypeScript/Vite build, lint where practical,
   baseline validation, and `git diff --check`; keep live-only scenarios in the
   runtime matrix until exercised against a real cluster.

## Post-Implementation Status

The audit tables above intentionally preserve the pre-implementation React
state reviewed at `4fb6dadf190007b831fdd2d08a9a9c6431b252b9`. The following
table records the result after the Module 06 implementation. `STATIC_COMPLETE`
means the Classic flow, client boundary, and request contract are represented
in React and reviewed in source. It does not promote any row in the Runtime
Acceptance Matrix to passed; every live-cluster row remains `RUNTIME_PENDING`.

| ID | Source status | Implemented evidence and remaining runtime acceptance |
| --- | --- | --- |
| `STACK-SVC-001` | `STATIC_COMPLETE` | Current-stack and fallback service loads now expose stable load errors and Retry; live stack metadata shapes remain pending. |
| `STACK-SVC-002` | `STATIC_COMPLETE` | Add Service applies permission, feature flag, upgrade, ownership, Kerberos, preselection, and return-path gates; both wizard exits require browser acceptance. |
| `STACK-SVC-003` | `STATIC_COMPLETE` | Missing cluster stack versions fall back to stack operating systems and repositories, including mirror metadata and recoverable loading. |
| `STACK-SVC-004` | `STATIC_COMPLETE` | Versions entry uses `AMBARI.MANAGE_STACK_VERSIONS`, popup Save uses admin/non-operator eligibility, and the legacy fallback remains separately editable. |
| `STACK-SVC-005` | `STATIC_COMPLETE` | Repository validation is keyed by OS plus repo ID, reports individual failures, and supports Revert, Cancel, Skip, and explicit Save Anyway without swallowing PUT failures. |
| `VER-LIST-001` | `STATIC_COMPLETE` | Hidden, older-version, repository type, current-stack, and compatibility rules now drive visible versions with retryable loads. |
| `VER-LIST-002` | `STATIC_COMPLETE` | All seven filters and counts use derived server-state semantics; helper tests cover not-installed, ready, current, installed, active, and finalize states. |
| `VER-LIST-003` | `STATIC_COMPLETE` | Version cards, details, service versions, repository OS data, host states, and state-gated actions use defensive nested-data access; full live metadata remains pending. |
| `VER-LIST-004` | `STATIC_COMPLETE` | Existing host lists and encoded Hosts navigation remain unchanged and aligned; Module 03 owns destination behavior. |
| `VER-LIST-005` | `STATIC_COMPLETE` | Every bulk install/reinstall entry now shares permission, ownership, active-upgrade, and duplicate-operation gates while retaining persisted operation recovery. |
| `VER-LIST-006` | `STATIC_COMPLETE` | Existing single-host payload and Module 03 ownership remain unchanged; real Agent orchestration remains pending. |
| `VER-LIST-007` | `STATIC_COMPLETE` | OUT_OF_SYNC actions retain the Classic `INSTALL_FAILED` query, obtain KDC state, serialize requests, surface rejection, and refresh authoritative state. |
| `VER-LIST-008` | `STATIC_COMPLETE` | Eligible unused, failed, PATCH, and MAINT versions confirm and PUT `RepositoryVersions.hidden=true`; in-use versions stay unavailable. |
| `VER-LIST-009` | `STATIC_COMPLETE` | Existing authenticated Admin View redirect remains unchanged and aligned. |
| `UPG-START-001` | `STATIC_COMPLETE` | Compatible repository versions constrain targets before supported upgrade types are requested. |
| `UPG-START-002` | `STATIC_COMPLETE` | Server types plus `enabledWizardForHostOrderedUpgrade` determine the three methods and target switches reset selection state. |
| `UPG-START-003` | `STATIC_COMPLETE` | Both Classic tolerance controls remain available and reset per target; start and active-update payloads use the two string boolean fields. |
| `UPG-START-004` | `STATIC_COMPLETE` | `preUpgradeCheck` bypass, FAIL/WARNING/BYPASS classification, loading gates, generation-based stale-result rejection, rerun, and config details are implemented. |
| `UPG-START-005` | `STATIC_COMPLETE` | Custom service, maintenance, heartbeat, component, service-check, and previous-upgrade actions render details and rerun only after successful repair; the broken Classic Finalize binding is absent. |
| `UPG-START-006` | `STATIC_COMPLETE` | Start is permission/ownership gated, single-flight, recoverable, and claims ownership before creating the upgrade request. |
| `UPG-START-007` | `STATIC_COMPLETE` | Downgrade abort waiting is bounded and cancellable, failures remain visible, and suspended or terminal Retry restarts polling. |
| `UPG-START-008` | `STATIC_COMPLETE` | Revert eligibility, affected services, `revert_upgrade_id`, target identity, persistence failure, and rejected-start recovery are represented. |
| `UPG-RUN-001` | `STATIC_COMPLETE` | Polling has one initial request, remains sequential, exposes stable errors, stops on teardown/terminal state, and can be explicitly restarted. |
| `UPG-RUN-002` | `STATIC_COMPLETE` | Every task and log is loaded lazily, item changes clear local detail, and Copy/Open operate per task. |
| `UPG-RUN-003` | `STATIC_COMPLETE` | Existing manual instructions, acknowledgement, exact status PUT, and rejection behavior remain aligned. |
| `UPG-RUN-004` | `STATIC_COMPLETE` | Failed and timed-out items remain current actionable items, status writes are single-flight, detail failures expose Retry, and successful Retry resumes polling. |
| `UPG-RUN-005` | `STATIC_COMPLETE` | Pause changes local state and closes only after the suspend PUT succeeds; rejection leaves the wizard open with a visible error. |
| `UPG-RUN-006` | `STATIC_COMPLETE` | Resume is single-flight, retains suspended state on failure, exposes Retry, and restarts the stopped poller after success. |
| `UPG-RUN-007` | `STATIC_COMPLETE` | Abort remains limited to downgrade and previous-upgrade repair; no generic Stop action was added. |
| `UPG-RUN-008` | `STATIC_COMPLETE` | `/main/admin/stack/upgrade` restores the active request, close/unmount stops local polling, and terminal completion clears browser ownership before reload. |
| `UPG-RUN-009` | `STATIC_COMPLETE` | Upgrade start persists the initiator before POST; refresh and upgrade events reload ownership, non-owners see the initiator and retain read-only progress/log access. |
| `UPG-RUN-010` | `STATIC_COMPLETE` | Module 06 mutation and route guards consistently apply upgrade, suspended, ownership, and `opsDuringRollingUpgrade` boundaries. |
| `UPG-RUN-011` | `STATIC_COMPLETE` | Final risk loads the Classic service-check endpoint, safely parses service/host failures, handles `TIMEDOUT`, and exposes detail Retry plus permitted decisions. |
| `UPG-RUN-012` | `STATIC_COMPLETE` | Finalize and Finalize Later are single-flight, retain visible rejection, render skipped checks, and allow terminal recovery even if browser persistence fails. |
| `UPG-HIST-001` | `STATIC_COMPLETE` | History and repository metadata load together with Retry, safe removed-repository fallback, and running end-time handling. |
| `UPG-HIST-002` | `STATIC_COMPLETE` | The existing nine conjunction filters remain unchanged and aligned. |
| `UPG-HIST-003` | `STATIC_COMPLETE` | Read-only detail performs one explicit load with no poll interval, supports load/detail Retry, lazy logs, unmount, and second-record selection. |
| `ADMIN-ACCT-001` | `STATIC_COMPLETE` | Existing exact route permission remains; current stack/config loads now terminate on error with Retry. |
| `ADMIN-ACCT-002` | `STATIC_COMPLETE` | User/group definitions preserve source order, visibility, duplicates, and read-only values without edit or Save controls. |
| `ADMIN-AUTO-001` | `STATIC_COMPLETE` | Parent and child guards recognize both auto-start permissions; direct routing intentionally applies `serviceAutoStart` consistently with menu visibility. |
| `ADMIN-AUTO-002` | `STATIC_COMPLETE` | Installed non-client components are filtered by category/count, grouped by service, read-only for service permission, and reloadable after failure. |
| `ADMIN-AUTO-003` | `STATIC_COMPLETE` | Config, enable, and disable writes run concurrently; partial failure reloads server truth, preserves only remaining changes, disables duplicates, and supports Retry. |
| `ADMIN-AUTO-004` | `STATIC_COMPLETE` | Router transitions and browser unload are blocked for edits; Save, Discard, and Cancel preserve the intended destination and failure behavior. |

### Shared-File Scope

No parallel-module conflict was observed. Cross-boundary changes were kept to
the minimum needed for Module 06:

| File | Module 06 reason |
| --- | --- |
| `src/Utils/Utility.ts` | Recognize Ambari's `TIMEDOUT` status in upgrade failure/active derivation. |
| `src/components/AdminRouteGuard.tsx` | Permit the two Auto Start capabilities to reach their guarded child route. |
| `src/components/UpgradeGuard.tsx` | Honor `opsDuringRollingUpgrade` for Module 06 admin operations. |
| `src/components/Table.tsx` | Consume the existing `noBorder` presentation prop instead of leaking it to the DOM. |
| `src/hooks/usePolling.ts` | Provide explicit restart semantics after a terminal Module 06 request is retried. |
| `src/hooks/useStackServices.ts` | Load current stack service definitions with cancellation and recoverable errors for Service Accounts. |
| `src/store/context.tsx` | Restore typed upgrade persistence and refresh the initiating user on upgrade events. |
| `src/screens/KerberosWizard/KerberosStore/context.tsx` | Return Kerberos Add Service completion to the Module 06 Stack tab. |
| `src/screens/Services/AddServiceWizard/wizardDataStore/context.tsx` | Return ordinary Add Service completion to the Module 06 Stack tab. |

All live scenarios in the Runtime Acceptance Matrix remain pending because this
worktree has no Ambari Server, Agents, repository metadata, multi-user fixture,
or fault-injection cluster.

## Verification Record

| Command | Result |
| --- | --- |
| `npm test` in `ambari-web/latest` | Passed: 63 test files and 224 tests. Expected failure-path stderr and React Bootstrap jsdom transition warnings were emitted without test failures. |
| `npm run build` in `ambari-web/latest` | Passed: TypeScript project build and Vite production build, 3,287 modules transformed. Existing Sass, duplicate-case, `eval`, and chunk-size warnings remain outside Module 06. |
| `node docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs` | Passed with 1,002 feature IDs and no warnings or errors after regenerating the Module 06 feature-index rows. |
| Changed-file `npx eslint ...` | Failed on the existing frontend `any`, legacy `@ts-ignore`, hook, and shared-file lint debt. No lint success is claimed; TypeScript and production build passed. |
| `git diff --check` | Passed. |

The automated results above are static/local evidence only. They do not replace
any `RUNTIME_PENDING` row in the live-cluster acceptance matrix.
