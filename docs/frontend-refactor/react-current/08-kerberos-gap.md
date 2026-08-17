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

# React Kerberos Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/08-kerberos.md` |
| React implementation | `ambari-web/latest`, Module 08 work based on `4fb6dadf19` |
| Feature IDs | All 81 IDs from `KRB-ENTRY-001` through `KRB-RISK-005` |
| Review date | 2026-08-17 |
| Metrics boundary | Metrics pages, widgets, charts, metric polling, and Metrics APIs are excluded |

The comparison used the written baseline, the actual `ambari-web/classic`
controllers/templates/AJAX registrations, the React source, focused tests, and
the generated endpoint evidence. Matching route or component names were not
accepted as parity evidence. API payloads, permissions, feature flags, KDC
modes, asynchronous sequencing, failure propagation, polling recovery, exit
cleanup, and cross-wizard behavior were reviewed separately.

## Initial Static Conclusion

This is the state of `origin/frontend-refactor` before Module 08 changes.

| Status | Count |
| --- | ---: |
| `STATICALLY_ALIGNED` | 18 |
| `PARTIAL` | 42 |
| `INCORRECT` | 14 |
| `MISSING` | 3 |
| `RUNTIME_OR_CROSS_MODULE` | 4 |
| Total | 81 |

The initial implementation exposed much of the wizard, but it did not safely
implement the complete contract. Important defects included incomplete mode
forcing, missing Step 3 host-component installation, missing Stack Advisor,
lost progress recovery, incorrect exit cleanup, swallowed credential failures,
and incomplete Disable and Regenerate sequencing.

## Final Audit Gap Checkpoint

The five-pass audit found the following additional defects after the first
implementation pass. Commit `434801dd80` records this checkpoint before the
corresponding React fixes.

| Affected IDs | Classic executable behavior | React behavior at checkpoint | Confirmed gap | Executable acceptance and final evidence |
| --- | --- | --- | --- | --- |
| `KRB-ENTRY-002`, `KRB-DIS-001` | `/main/admin/kerberos/disableSecurity` is a guarded route that opens the Disable workflow. | Only the management route and Enable step route were registered. | A valid Classic deep link fell through the React catch-all route. | `RoutesList.test.tsx` asserts all three paths; `EnableKerberos.test.tsx` enters the deep link and observes Disable confirmation. Permission, flag, upgrade, and owner guards wrap every path. `RESOLVED`. |
| `KRB-ENTRY-002`, `KRB-ENTRY-005`, `KRB-REC-001`, `KRB-REC-004` | Entering Enable writes `wizard-data` with the login name and controller; `App.isAuthorized` denies other users; completion and discard reset ownership. | Step recovery was persisted, but no owner was written or cleared and Kerberos routes/sidebar did not consume `isNonWizardUser`. | Another user could enter and mutate an active wizard. | Payload tests assert owner create/reset, route guards reject conflicting wizards, and `SideItemList.test.tsx` hides Kerberos for a non-owner. `RESOLVED`. |
| `KRB-DIS-006`, `KRB-DIS-008` | Delete success advances the sequence, and Disable close returns to the Kerberos page and reloads current cluster state. | Delete helpers discarded HTTP status, so a non-empty successful body could be marked failed; Complete only closed the modal. | Successful Disable could stall or continue to display Kerberos as enabled. | API tests assert retained 2xx status; the completion test reloads `Clusters/security_type`, returns to the canonical URL, and renders disabled state. Server-restart recovery remains a runtime boundary. `RESOLVED_STATICALLY`. |
| `KRB-MGMT-007` | Each accepted Regenerate action submits a new request; optional restart begins only after that request completes. | The parent boolean was never reset, so submission failure and closing Background Operations made later actions no-ops. | Regenerate was one-shot until page reload. | Component tests release the trigger on submission failure/close, start it again, and retain at-most-once restart per returned request. `RESOLVED`. |
| `KRB-REC-002`, `KRB-RISK-005` | Operation errors are reported through controller state and persisted wizard state supports recovery. | `OperationsProgress` invoked `errorCallback` while rendering, and persistence rejection had no Retry. | Parent state could update during child render and a failed checkpoint could silently lose recovery. | Tests report each terminal error from an effect once, expose recovery-load/checkpoint Retry, and serialize checkpoints before reset so old state cannot overwrite completion. `RESOLVED`. |

## Classic Executable Workflows

The source-backed Classic flows used for every Feature ID are summarized here;
endpoint details are retained in the contract table below.

* Entry (`KRB-ENTRY-*`) loads `Clusters/security_type`, enforces permission,
  feature, upgrade, and wizard-owner restrictions, presents installed-service
  warnings and optional prechecks, persists the owner and Step 1, then routes
  to the resumable wizard.
* Steps 1-2 (`KRB-MODE-*`, `KRB-1-*`, `KRB-2-*`) choose MIT, AD, IPA, or Manual,
  gate the exact prerequisite checklist, validate visible stack properties,
  test KDC access, delete stale Kerberos resources, create service/component/
  host resources, save desired configs, and create or update the credential
  alias in dependency order. Any failed prerequisite stops the transition.
* Steps 3-5 (`KRB-3-*` through `KRB-5-*`) install the Kerberos service or named
  clients according to component state, validate the session and heartbeat,
  load COMPOSITE identities, call Stack Advisor on first load, save descriptor
  edits, show the mode-specific confirmation, and provide Manual CSV before
  service mutation begins.
* Steps 6-8 (`KRB-6-*` through `KRB-8-*`) stop services, conditionally delete
  unsupported ATS, submit Kerberize, poll the returned request, allow force
  Retry after failure, start services with the configured smoke-test policy,
  and clear recovery/ownership only when completion is accepted.
* Disable (`KRB-DIS-*`) follows confirmation with start ZooKeeper, stop all
  other services, session-gated unkerberize, explicit no-identity Skip after
  failure, idempotent KERBEROS deletion, service restart, canonical navigation,
  and security-state reload. In-progress unkerberize cannot be abandoned.
* Management and credentials (`KRB-MGMT-*`, `KRB-CRED-*`) edit COMPOSITE
  identities, await descriptor fallback, regenerate all or missing keytabs,
  optionally restart only after that request completes, authorize CSV
  separately, and implement alias list/create/update/delete plus invalid-session
  replay without exposing the secret.
* Integration and recovery (`KRB-X-*`, `KRB-REC-*`, `KRB-RISK-*`) preserve the
  Add Service/Host, Hosts, Services, Reassign, HA, and Federation ownership
  boundaries; serialize owner/step/operation checkpoints, resume request IDs,
  guard route exit, and apply the documented compatibility fixes instead of
  reproducing unreachable or false-success Classic paths.

## Post-Implementation Status

The final statuses below are mutually exclusive and total all 81 baseline IDs.
`STATICALLY_ALIGNED` still requires live acceptance. `COMPATIBILITY_FIX` means
React intentionally corrects a documented Classic defect. `STATIC_ONLY` is a
legacy method with no user-reachable behavior. `CROSS_MODULE_BOUNDARY` is owned
jointly with another refactor module. `NEEDS_RUNTIME_VALIDATION` cannot be
proven from source or unit tests.

| Final status | Count |
| --- | ---: |
| `STATICALLY_ALIGNED` | 54 |
| `COMPATIBILITY_FIX` | 17 |
| `STATIC_ONLY` | 1 |
| `CROSS_MODULE_BOUNDARY` | 5 |
| `NEEDS_RUNTIME_VALIDATION` | 4 |
| Total | 81 |

## Feature Status

### Entry, Modes, and Steps 1-2

Every row below is also an executable acceptance assertion: the described
React behavior must be observable with the named mode, request, failure, or
permission condition, and its final status must remain true after refresh.

| ID | Final status | React final behavior and executable acceptance |
| --- | --- | --- |
| `KRB-ENTRY-001` | `STATICALLY_ALIGNED` | Security type loading has spinner, visible failure, and Retry. |
| `KRB-ENTRY-002` | `STATICALLY_ALIGNED` | All three Kerberos routes require `CLUSTER.TOGGLE_KERBEROS`, `enableToggleKerberos`, and no conflicting upgrade or non-owner wizard. |
| `KRB-ENTRY-003` | `STATICALLY_ALIGNED` | The installed-service warning map preserves the Classic YARN warning and blocks entry until confirmed. |
| `KRB-ENTRY-004` | `STATICALLY_ALIGNED` | Optional pre-Kerberize checks preserve the API-root request, block `FAIL`, show reasons, and allow Retry. |
| `KRB-ENTRY-005` | `STATICALLY_ALIGNED` | Login owner/controller and Step 1 are persisted before navigation; an active `CLUSTER_STATE` reopens the saved step only for the owner. |
| `KRB-MODE-001` | `STATICALLY_ALIGNED` | MIT settings, automatic resources, KDC session, client install, and managed identities are retained. |
| `KRB-MODE-002` | `STATICALLY_ALIGNED` | AD fields and password-policy inputs are mode-visible and use the automatic flow. |
| `KRB-MODE-003` | `STATICALLY_ALIGNED` | IPA forces package and krb5.conf management off while retaining identity management. |
| `KRB-MODE-004` | `STATICALLY_ALIGNED` | Manual forces all management flags off, skips Step 3, requires CSV responsibility, and hides automatic controls. |
| `KRB-1-001` | `STATICALLY_ALIGNED` | Four modes are selectable; changing mode clears its checklist, while reload restores the persisted mode and checked prerequisites. |
| `KRB-1-002` | `STATICALLY_ALIGNED` | Classic's 3/5/3/5 prerequisite counts are restored, including the conditional ONEFS MIT item. |
| `KRB-2-001` | `STATICALLY_ALIGNED` | Stack Kerberos configs are loaded, mode visibility is applied, and visible validation gates Next. |
| `KRB-2-002` | `STATICALLY_ALIGNED` | KDC test failure remains visible and retryable; duplicate Step 2 submissions are locked. |
| `KRB-2-003` | `STATICALLY_ALIGNED` | Automatic mode deletes leftovers, creates service/component/host resources in dependency order, and propagates failure. |
| `KRB-2-004` | `STATICALLY_ALIGNED` | Desired configs are grouped by type and include the initial-version note. |
| `KRB-2-005` | `COMPATIBILITY_FIX` | Credential create/update or Manual session update is awaited; failure stops Step 2 instead of Classic's false success. |
| `KRB-2-006` | `STATICALLY_ALIGNED` | Manual and IPA forced values are asserted by focused payload tests. |
| `KRB-2-007` | `COMPATIBILITY_FIX` | Every prerequisite rejects the sequence, unlocks Next, retains the form, and exposes a retryable error. |

### Steps 3-8

| ID | Final status | React final behavior and executable acceptance |
| --- | --- | --- |
| `KRB-3-001` | `STATICALLY_ALIGNED` | Reads `KERBEROS_CLIENT` state; INIT installs the service, otherwise all named host-components are installed. |
| `KRB-3-002` | `STATICALLY_ALIGNED` | Service check forces the KDC session gate before security is enabled, and credential cancellation rejects the task. |
| `KRB-3-003` | `STATICALLY_ALIGNED` | Lost-heartbeat hosts fail the explicit heartbeat operation with host names. |
| `KRB-3-004` | `COMPATIBILITY_FIX` | Retry and Ignore Errors are implemented; a heartbeat failure retries from client installation, while the broken Classic Rollback and unreachable Skip are not exposed. |
| `KRB-3-005` | `STATICALLY_ALIGNED` | Manual jumps Step 2 to Step 4 and Step 4 Back returns to Step 2. |
| `KRB-4-001` | `STATICALLY_ALIGNED` | COMPOSITE descriptor values are filtered to installed services and grouped as Global, Ambari, and service identities. |
| `KRB-4-002` | `STATICALLY_ALIGNED` | First load builds desired-config and host-group input, calls Stack Advisor, applies recommendations, and blocks on failure. |
| `KRB-4-003` | `STATICALLY_ALIGNED` | Descriptor fields retain visibility/editability and current form values are submitted. |
| `KRB-4-004` | `COMPATIBILITY_FIX` | POST 409 falls back to PUT and PUT 404 falls back to POST as one awaited operation. |
| `KRB-4-005` | `STATICALLY_ALIGNED` | Descriptor save is followed by best-effort unkerberize cleanup before Confirm. |
| `KRB-4-006` | `COMPATIBILITY_FIX` | Descriptor or recommendation failure leaves the step visible with Retry instead of hanging permanently. |
| `KRB-5-001` | `STATICALLY_ALIGNED` | Confirmation uses mode-specific property lists and hides empty values. |
| `KRB-5-002` | `COMPATIBILITY_FIX` | CSV request failure is reported and can be retried rather than entering the Classic success handler. |
| `KRB-5-003` | `NEEDS_RUNTIME_VALIDATION` | UI responsibility text and CSV download exist; principal/keytab creation and distribution require a real Manual environment. |
| `KRB-5-004` | `STATICALLY_ALIGNED` | Exit attempts unkerberize and KERBEROS deletion before clearing recovery state. |
| `KRB-6-001` | `STATICALLY_ALIGNED` | Stop-all is a recoverable tracked request and external navigation is guarded. |
| `KRB-6-002` | `COMPATIBILITY_FIX` | ATS support is derived from stack cardinality; 404 is idempotent and other failures expose Retry. |
| `KRB-7-001` | `STATICALLY_ALIGNED` | Kerberize sets the security type, records the request, and resumes polling after reload. |
| `KRB-7-002` | `STATICALLY_ALIGNED` | Failed operations enable Step 4 Back and Retry sends `force_toggle_kerberos=true`. |
| `KRB-7-003` | `STATIC_ONLY` | No UI action is invented for the Classic test-only unkerberize method. |
| `KRB-8-001` | `STATICALLY_ALIGNED` | Start-all derives `run_smoke_test` from `skip.service.checks`. |
| `KRB-8-002` | `STATICALLY_ALIGNED` | Complete is enabled after success or failure, with failed-service repair guidance. |
| `KRB-8-003` | `STATICALLY_ALIGNED` | Completion atomically clears both recovery namespaces; Step 8 exit never unkerberizes the enabled cluster. |

### Disable and Management

| ID | Final status | React final behavior and executable acceptance |
| --- | --- | --- |
| `KRB-DIS-001` | `STATICALLY_ALIGNED` | Management and `disableSecurity` deep-link routes enforce permission, feature, upgrade, and owner guards before warning, confirmation, and unsaved-edit lock. |
| `KRB-DIS-002` | `STATICALLY_ALIGNED` | ZooKeeper is started independently. |
| `KRB-DIS-003` | `STATICALLY_ALIGNED` | All non-ZooKeeper services are stopped. |
| `KRB-DIS-004` | `STATICALLY_ALIGNED` | Normal unkerberize uses a validated KDC session. |
| `KRB-DIS-005` | `STATICALLY_ALIGNED` | Failed unkerberize exposes the explicit `manage_kerberos_identities=false` Skip request. |
| `KRB-DIS-006` | `COMPATIBILITY_FIX` | KERBEROS deletion retains its 2xx status and remains idempotent on failure, so either response advances instead of stranding the modal. |
| `KRB-DIS-007` | `STATICALLY_ALIGNED` | Services restart with the same service-check property decision. |
| `KRB-DIS-008` | `NEEDS_RUNTIME_VALIDATION` | Complete returns to the canonical route and reloads security state; mid-flow refresh/server-restart behavior must still be tested before and after every mutation. |
| `KRB-DIS-009` | `COMPATIBILITY_FIX` | React does not reset unrelated Add Service state when Disable closes. |
| `KRB-MGMT-001` | `STATICALLY_ALIGNED` | Composite identities render by Global, Ambari, and installed service. |
| `KRB-MGMT-002` | `STATICALLY_ALIGNED` | Edit/Discard/Save, realm read-only behavior, validation, and management-button locking are present. |
| `KRB-MGMT-003` | `COMPATIBILITY_FIX` | Descriptor create fallback is awaited before Manual or automatic Regenerate begins. |
| `KRB-MGMT-004` | `STATICALLY_ALIGNED` | Automatic clusters expose all/missing plus automatic/manual restart selection. |
| `KRB-MGMT-005` | `STATICALLY_ALIGNED` | Service actions retain service-scoped Regenerate with `config_update_policy=none`. |
| `KRB-MGMT-006` | `STATICALLY_ALIGNED` | Host action requires the feature flag, enabled automatic Kerberos, and host-scoped Regenerate. |
| `KRB-MGMT-007` | `COMPATIBILITY_FIX` | React polls the returned Regenerate request, starts at most one restart after success, releases the trigger after close/failure, and permits a later independent Regenerate. |
| `KRB-MGMT-008` | `STATICALLY_ALIGNED` | CSV requires `CLUSTER.UPGRADE_DOWNGRADE_STACK` and is available in either mode. |
| `KRB-MGMT-009` | `COMPATIBILITY_FIX` | PUT 404 -> POST -> Regenerate is one observable promise; the undefined Classic `self` path is not reproduced. |

### Credentials, Integration, Recovery, and Risks

| ID | Final status | React final behavior and executable acceptance |
| --- | --- | --- |
| `KRB-CRED-001` | `STATICALLY_ALIGNED` | Persistent store capability and non-Manual mode control Manage visibility. |
| `KRB-CRED-002` | `STATICALLY_ALIGNED` | Credential listing determines presence without exposing a secret. |
| `KRB-CRED-003` | `STATICALLY_ALIGNED` | Required principal/password use POST or PUT according to alias presence. |
| `KRB-CRED-004` | `STATICALLY_ALIGNED` | Delete requires confirmation and refreshes presence only after success. |
| `KRB-CRED-005` | `STATICALLY_ALIGNED` | Session failure opens the credential popup and replays only after a successful save. |
| `KRB-CRED-006` | `COMPATIBILITY_FIX` | CRUD rejection retains input, reports the error, and never reports false success or replays the protected request. |
| `KRB-X-001` | `CROSS_MODULE_BOUNDARY` | Add Service owns descriptor merge, Manual CSV, and Review integration; validate after its module lands. |
| `KRB-X-002` | `CROSS_MODULE_BOUNDARY` | Add Host owns Review/session and keytab install checkpoints. |
| `KRB-X-003` | `CROSS_MODULE_BOUNDARY` | Hosts owns component add/delete and post-recovery host Regenerate. |
| `KRB-X-004` | `CROSS_MODULE_BOUNDARY` | Reassign/HA/Federation own identity/config synchronization and task ordering. |
| `KRB-X-005` | `CROSS_MODULE_BOUNDARY` | Services/Hosts own affected-component restart behavior. |
| `KRB-REC-001` | `STATICALLY_ALIGNED` | Start persists login owner/controller plus active step and cluster state; each step persists form/progress, and all completion/discard paths clear ownership. |
| `KRB-REC-002` | `STATICALLY_ALIGNED` | Serialized checkpoints persist status/request ID, expose load/save Retry, resume polling without duplicate mutation, and cannot overwrite a later reset. |
| `KRB-REC-003` | `STATICALLY_ALIGNED` | Step 1-7 client navigation and page unload are guarded; Step 2/6/7 retain specialized warnings. |
| `KRB-REC-004` | `STATICALLY_ALIGNED` | Incomplete exit waits for cleanup and serialized state/owner reset, then releases the route guard for one navigation; Step 8 is the explicit no-discard exception. |
| `KRB-REC-005` | `NEEDS_RUNTIME_VALIDATION` | Missing Manual principals/keytabs and backend error equivalence cannot be established statically. |
| `KRB-REC-006` | `NEEDS_RUNTIME_VALIDATION` | Disable refresh/server-restart recovery remains a live boundary inherited from its modal ownership. |
| `KRB-RISK-001` | `COMPATIBILITY_FIX` | Broken Rollback and unreachable Skip are not rendered. |
| `KRB-RISK-002` | `STATICALLY_ALIGNED` | Retry directly force-kerberizes without inventing compensating cleanup. |
| `KRB-RISK-003` | `COMPATIBILITY_FIX` | Credential failures reject and stop the protected request. |
| `KRB-RISK-004` | `COMPATIBILITY_FIX` | Descriptor fallback is chained through Regenerate. |
| `KRB-RISK-005` | `COMPATIBILITY_FIX` | Focused tests cover fallback/rejection, failed deletion/save, duplicate locks, DELETE status, owner guards, persistence Retry/order, Disable refresh, and repeated Regenerate. |

## Five Independent Audit Passes

| Pass | Independent question | Evidence checked | Result |
| --- | --- | --- | --- |
| 1 | Does every baseline UI behavior have a React path? | All 81 IDs, Classic controllers/templates, React routes/components | Found Step 3 reinstall, Step 2 discard, and prerequisite defects; fixed. |
| 2 | Do HTTP method, URL, query, body, and ordering match? | AJAX registrations/callers, direct endpoints, API helpers/tests | Added exact Step 3, precheck, ATS, descriptor, credential, and recovery contracts. |
| 3 | Can failure, Retry, reload, exit, and polling converge? | OperationsProgress, Steps 2-8, Disable, persistence, route exit | Added request recovery, timer cleanup, bounded restart, force Retry, atomic completion, and single-confirmation awaited discard. |
| 4 | Do MIT, AD, IPA, Manual, management, and credentials branch correctly? | Mode visibility/payloads, CSV, CRUD, Regenerate, ONEFS/YARN | Corrected forced values, controls, error propagation, and mode-specific navigation. |
| 5 | Is any ID, endpoint, cross-flow, secret, or excluded Metrics work unaccounted for? | Reverse ID count, endpoint scans, cross-module call sites, credential scan, Metrics scan | 81/81 classified; Metrics excluded; runtime and cross-module items are explicit. |

## Key Endpoint Contracts

| Purpose | Contract |
| --- | --- |
| Security mode | `GET clusters/{cluster}?fields=Clusters/security_type` |
| Pre-Kerberize checks | Registered Classic-compatible API-root `GET ""` |
| KDC test | `GET kdc_check/{encoded-host-list}` |
| Descriptor | `GET .../kerberos_descriptors/COMPOSITE`, cluster artifact POST/PUT |
| Step 3 state | `GET .../services/KERBEROS/components/KERBEROS_CLIENT?fields=ServiceComponentInfo/state` |
| Step 3 INIT | `PUT .../services?ServiceInfo/state=INSTALLED&ServiceInfo/service_name=KERBEROS` |
| Step 3 initialized | `PUT .../host_components` with component/host/maintenance predicate |
| Kerberize/unkerberize | Cluster `PUT` with optional force or identity-management query |
| Credential alias | Cluster credential list plus alias POST/PUT/DELETE |
| Regenerate | Cluster `PUT` with `regenerate_keytabs=all|missing`; service/host variants retain policy `none` |
| CSV | `GET .../kerberos_identities?fields=*&format=csv` |
| Recovery | `/persist/ENABLING_KERBEROS`, `/persist/CLUSTER_STATE`, and atomic `/persist` POST |

## Compatibility Decisions

React intentionally does not reproduce known Classic defects. Failed
credentials do not display success, descriptor fallback is atomic, CSV failure
does not enter its success handler, ATS deletion cannot hang forever, Step 3
does not show a nonfunctional Rollback, and Disable does not reset Add Service.
These are recorded as `COMPATIBILITY_FIX`, not missing parity.

The optional pre-Kerberize check keeps the unusual registered API-root request
because that is the executable Classic contract. The Step 7 Retry keeps direct
`force_toggle_kerberos=true` semantics and does not add an unkerberize request.

## Static Verification Evidence

| Check | Result |
| --- | --- |
| `npm test` in `ambari-web/latest` | Passed: 70 files and 263 tests. Expected failure-path console output and existing timer warnings remain visible. |
| `npm run build` in `ambari-web/latest` | Passed TypeScript and the Vite production build. Existing Sass deprecations, duplicate service-loader cases, `eval`, and bundle-size warnings remain. |
| `./node_modules/.bin/eslint . --format json` | Repository-wide lint remains failing, but Module 08 reduces the baseline from 5,854 errors / 461 warnings (6,315 total) to 5,847 errors / 451 warnings (6,298 total). |
| `node docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs` | Passed with 1,002 feature IDs and no warnings or errors. |
| `python3 -m unittest -v` in `dev-support/ambari-ai` | Passed: 16 tests. |
| `git diff --check` | Passed. |
| Kerberos feature-ID comparison | Passed: 81 baseline IDs, 81 React rows, no missing or extra IDs. |
| Credential-pattern scan of changed source and Module 08 documents | No token, password assignment, or private-key pattern matched. |

The lint comparison uses the same ESLint version, dependency tree, and file
set in the Module 08 worktree and a clean `4fb6dadf19` worktree. A lower count
does not make repository-wide lint green; it establishes that this module does
not increase the existing debt.

## Runtime Acceptance Matrix

Static tests have not executed these scenarios. Every row is `NOT_RUN` until a
real Ambari Server and KDC environment supplies evidence.

| Environment | Scenario | Required observation | Status |
| --- | --- | --- | --- |
| MIT KDC | Complete Steps 1-8 | Resources, principals/keytabs, service check, stop/kerberize/start all converge | `NOT_RUN` |
| Active Directory | Complete Steps 1-8 | LDAPS/container/password rules and credential session work end to end | `NOT_RUN` |
| IPA | Complete Steps 1-8 | No package/krb5.conf management; identities remain managed | `NOT_RUN` |
| Manual | Complete Steps 1,2,4-8 | No client install; CSV responsibility and missing-file backend errors are correct | `NOT_RUN` |
| MIT + ONEFS | Step 1 | Conditional ONEFS prerequisite appears and gates Next | `NOT_RUN` |
| YARN, ATS unsupported | Step 6 | ATS is discovered and removed after services stop | `NOT_RUN` |
| YARN, ATS supported | Step 6 | ATS is retained and no deletion request is sent | `NOT_RUN` |
| All modes | Descriptor + Stack Advisor | Recommendations apply once; reload uses stored form values | `NOT_RUN` |
| Automatic modes | Step 3 with INIT and INSTALLED component | Service install and host-component install branches both complete | `NOT_RUN` |
| Automatic modes | Invalid KDC at Steps 3/7/Disable | Popup save/cancel controls replay and failure exactly once | `NOT_RUN` |
| Automatic modes | Regenerate all/missing with/without restart | Returned request is tracked and optional restart begins only after success | `NOT_RUN` |
| Persistent/non-persistent store | Manage credentials | Visibility and POST/PUT/DELETE behavior match capability | `NOT_RUN` |
| Read-only/operator/admin roles | Entry and management | Route, CSV, edit, Disable, Regenerate, and credential controls match permissions | `NOT_RUN` |
| Reload/browser back | Every Enable step | Saved step/form/request resumes without duplicate mutation | `NOT_RUN` |
| Exit | Steps 1,2,6,7,8 | Steps 1-7 discard; Step 8 retains enabled security | `NOT_RUN` |
| Server/network failures | Every mutation and poll | Error remains visible, Retry is bounded, and no false success occurs | `NOT_RUN` |
| Disable | Before/during/after each operation | Skip, idempotent delete, refresh behavior, and final service start are correct | `NOT_RUN` |
| Add Service | Automatic and Manual Kerberos | Descriptor, CSV, session, Review, and install behavior integrate | `NOT_RUN` |
| Add Host/component | Automatic and Manual Kerberos | Session gate and targeted keytabs integrate | `NOT_RUN` |
| Reassign/HA/Federation | Supported secure services | Principal/keytab/config updates follow component changes | `NOT_RUN` |

## Verification Boundary

Focused unit/component tests prove deterministic helpers, request shapes,
failure sequencing, recovery bookkeeping, and UI gating. They do not prove KDC
connectivity, Kerberos principal creation, keytab distribution, server-side
Stack Advisor output, service restart convergence, or crash recovery. The
runtime matrix must pass before the migration can claim operational Kerberos
parity.
