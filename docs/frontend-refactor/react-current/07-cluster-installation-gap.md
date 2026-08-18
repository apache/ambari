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

# React Cluster Installation Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/07-cluster-installation.md` |
| React implementation | `ambari-web/latest`, Module 07 implemented on `AMBARI-26629` from `4fb6dadf190007b831fdd2d08a9a9c6431b252b9` |
| Feature IDs | 79 IDs from `INST-MODE-001` through `INST-10-002` |
| Review date | 2026-08-17 |
| Deployment modes | New cluster, Add Host, and Add Service |
| Metrics boundary | Ambari Metrics, Ganglia, metric widgets, charts, and metric display data are excluded; service dependency metadata and operational host/request state remain included |

The comparison used the written baseline, executable Classic routes, controllers,
mixins, templates, AJAX registry, and tests, and the current React routes,
stores, screens, APIs, and tests. The generated API-by-module page was used only
to locate candidates. The authoritative network review combined AJAX
definitions, AJAX call sites, direct HTTP, browser entry points, and realtime
channels.

## Post-Implementation Static Conclusion

| Status | Meaning | Count |
| --- | --- | ---: |
| `STATICALLY_ALIGNED` | The reviewed React path matches the static Classic contract but still requires live acceptance | 51 |
| `PARTIAL` | A user-reachable path exists, but a condition, payload, branch, recovery path, or test is incomplete | 28 |
| `INCORRECT` | React can issue a wrong request, transition at the wrong time, or expose a materially different result | 0 |
| `MISSING` | No user-reachable implementation exists for the Feature ID | 0 |
| Total |  | 79 |

The confirmed destructive gaps were implemented. Review now checkpoints and
runs an abort-on-error deployment plan, persists completed operation IDs for
retry, retains the validated VDF source, synchronizes configuration groups, and
stores the install request before navigation. Deploy owns a single phase and
request, serializes polling, restores persisted work, blocks navigation while
active, and gates Retry and Summary by terminal status. Remaining partial items
are primarily stack-dependent validation/configuration permutations, detailed
host reconciliation and task-log ergonomics, and behavior that requires the
runtime matrix below.

## Complete Wizard State Machines

### New Cluster

| State | Forward condition and side effects | Back/cancel condition | Recovery contract | Current React result |
| --- | --- | --- | --- | --- |
| 0 Name | Valid name and at least one installable stack; store name, then enter Version | Cancel confirms and navigates to Admin View | `CLUSTER_CURRENT` plus `CLUSTER_STATE.stepName=NAME` | Name validation and persistence-before-navigation align; stack availability is resolved on Version |
| 1 Version | Visible version definition selected; repository rows syntactically valid; URL checks pass or are explicitly skipped; JDK warning accepted; persist VDF source and repository edits | Back clears Version and all downstream state | Restore stack, definition, repository source, OS rows, validation result, and flags | Dynamic stack APIs, Public defaults, VDF retention, Satellite, and JDK acceptance are implemented and focused-tested |
| 2 Install Options | At least one new normalized host; automatic mode has required credentials; suspicious or installed hosts are explicitly accepted | Back clears hosts and downstream state | Restore normalized hosts and registration mode without replaying a mutation | Linux SSH, manual Agent, HDPWIN, and support-derived Agent user branches are implemented |
| 3 Confirm Hosts | At least one `REGISTERED` host; bootstrap/registration has settled; generic warnings accepted | Back stops timers; remove/retry affects only selected local wizard hosts | Restore request ID, host states/logs, registration deadline, check request, and results | Bootstrap request/deadline and host-check recovery are persisted; generic and independent JDK checks remain separate |
| 4 Services | At least one installable non-Metrics service; CRITICAL dependency/filesystem conflicts fixed; WARNING accepted | Back returns to hosts; change clears assignments/configs | Restore selection and accepted warnings | Service selection exists; complete stack-specific validation evidence is absent |
| 5 Masters | Advisor result loaded; assignment is cardinality-valid; current matching WARN/ERROR explicitly accepted | Back clears master and later data | Restore recommendation and manual moves; recalculate after service changes | Matching non-installed component issues and Continue Anyway align; React still adds non-metadata placement rules |
| 6 Slaves/Clients | Required matrix selection valid; server mapping WARN/ERROR explicitly accepted | Back preserves master assignments and clears later data | Restore matrix, hidden components, and accepted validation | Core matrix and validation exist; restoration and dependency permutations are incomplete |
| 7 Configs | Required values valid; required recommendations applied; dependent changes accepted; external tests pass or are consciously retried | Dirty Back confirms discard | Restore tabs, values, overrides, recommendations, validation, and dynamic assignments | Main config surface and support-gated Pre Install Checks shell exist; complete overrides and dirty-state behavior remain partial |
| 8 Review | Checkpoint `CLUSTER_DEPLOY_PREP_2`; complete destructive cleanup; non-dry-run VDF; abort-on-error serial resource queue; install request accepted | Back enabled only before successful submission; failures reopen Back/Deploy without rollback | Persist checkpoint and completed resource/request identity | Serial abort-on-error preparation, retry checkpoints, config groups, Print, Blueprint ZIP, and VDF retention are implemented |
| 9 Deploy | Install terminal; optionally start/check terminal; only defined terminal states enable Next; write `CLUSTER_INSTALLED_4` | No ordinary Back; only Classic Admin View/Views route exceptions; Retry only `INSTALL FAILED` | Resume current request and phase from server state and persisted request IDs | Phase/request recovery, serialized polling, exact Retry, route blocking, terminal gating, and checkpoints are implemented |
| 10 Summary | Complete attempts provisioning `INSTALLED`, clears wizard and cluster state, then enters Dashboard | No reachable Back; Cancel is not a second completion path | Restore static summary at `CLUSTER_INSTALLED_4` | New-cluster-only provisioning and retryable completion cleanup are implemented |

### Add Host

| Step | Forward condition and branch | Back/cancel and recovery | Current React result |
| --- | --- | --- | --- |
| 1 Install Options | Same Linux SSH, HDPWIN PowerShell, and manual paths as new cluster; optional Classic `Skip host checks` | No Back; cancel clears Add Host state; resume from `ADD_HOST` | Modes, Skip Host Checks, and serialized persistence are implemented |
| 2 Confirm Hosts | Registration/check rules from core Step 3 | Back stops polling; resume host/check request state | Registration, bootstrap/check recovery, and independent JDK checks are shared |
| 3 Slaves/Clients | Assign only slave/client components to new hosts; client-only/no-component is valid | Back preserves registered hosts | Core matrix is reused and covered by focused Add Host utility tests |
| 4 Config Groups | Skip automatically when no selected component; otherwise choose existing/default groups by affected service | Back to assignments; persist exact group choice | Dedicated React page and persisted selection exist |
| 5 Review | Write `ADD_HOSTS_DEPLOY_PREP_2`; register hosts/components; apply full config-group memberships; obtain KDC session before install | Resource/config-group failure remains retryable on Review | Dedicated React deployment is serial and tested; KDC/real-cluster behavior remains conditional |
| 6 Deploy | Poll install, optional keytab regeneration, start selected non-client components; Retry failed phase | No Back; persist phase/request; Add Host may proceed after terminal install failure | Dedicated React flow persists phase/request and is focused-tested |
| 7 Summary | Show host/task outcome; clear Add Host state; refresh Hosts; never write cluster provisioning state | No Back | Dedicated React summary follows this boundary |

Classic server-state mappings are implemented and retained for runtime tests:
`ADD_HOSTS_DEPLOY_PREP_2 -> Step 4`, `ADD_HOSTS_INSTALLING_3` and
`SERVICE_STARTING_3 -> Step 5`, and `ADD_HOSTS_INSTALLED_4 -> Step 6`. React
restores both its serialized active step and these Classic server states.

### Add Service

| Step | Forward condition and conditional skip | Back/cancel and recovery | Current React result |
| --- | --- | --- | --- |
| 1 Services | Show only uninstalled/installable services and enforce dependencies | No Back; cancel owns `ADD_SERVICE` cleanup | Selection filtering exists |
| 2 Masters | Skip when selected services have no assignable masters | Back to Services | Metadata-derived conditional navigation is implemented and tested |
| 3 Slaves/Clients | Skip when selected services have no slave/client components | Back to last applicable step | Metadata-derived conditional navigation is implemented and tested |
| 4 Configs | Skip when no selected service config is required; on Kerberos, validate/update descriptor | Back to last applicable assignment step | Config skipping and secure descriptor value propagation are implemented |
| 5 Review | Write `ADD_SERVICES_DEPLOY_PREP_2`; show Manual KDC responsibility; offer CSV for every non-empty `kdc_type`; create service resources serially | Failure reopens Review; persist completed boundary | Real KDC type, descriptor POST/PUT, CSV, Manual responsibility, and serial retry are implemented and tested |
| 6 Deploy | Install/start/check only added services; Retry `INSTALL FAILED`; persist request IDs | No Back; terminal states only | Shared phase/request state machine owns polling, retry, and recovery |
| 7 Summary | Show results; clear Add Service state; refresh cluster; never write provisioning state | No Back | Add Service clears only its state and never writes provisioning state |

Classic maps `ADD_SERVICES_DEPLOY_PREP_2 -> Step 5`, and all of
`ADD_SERVICES_INSTALLING_3`, `SERVICE_STARTING_3`, and
`ADD_SERVICES_INSTALLED_4 -> Step 7`. The latter mapping can prematurely show a
static Classic Summary; React must not create a second request on refresh and
must reconcile the active server request before presenting completion.

## Feature Status

### Modes, Entries, and Recovery

| ID | Current status | Classic behavior versus current React |
| --- | --- | --- |
| `INST-MODE-001` | `PARTIAL` | Eleven-step new-cluster UI exists, but version selection, Review ordering, Deploy recovery, and final state are not equivalent |
| `INST-MODE-002` | `STATICALLY_ALIGNED` | Add Host implements conditional host checks, checkpoints, serial preparation, phased deployment, retry, and owned completion |
| `INST-MODE-003` | `PARTIAL` | Add Service conditional navigation, Kerberos, deployment, and recovery are implemented; shared stack-specific config permutations remain |
| `INST-MODE-004` | `STATICALLY_ALIGNED` | Public Repository is the default, dynamic stack APIs preserve server URLs, and incompatible JDKs require explicit acceptance |
| `INST-MODE-005` | `STATICALLY_ALIGNED` | XML dry-run retains the exact body and content type for the non-dry-run Review POST |
| `INST-MODE-006` | `STATICALLY_ALIGNED` | URL dry-run retains the exact `VersionDefinition.version_url` payload for Review |
| `INST-MODE-007` | `STATICALLY_ALIGNED` | Linux bootstrap uses runtime Agent-user support and persists request/deadline state for refresh |
| `INST-MODE-008` | `STATICALLY_ALIGNED` | Manual mode skips bootstrap, uses the shorter deadline, and restores registration/check state |
| `INST-MODE-009` | `STATICALLY_ALIGNED` | Add Host waits for the KDC session and runs keytab regeneration as a persisted deployment phase |
| `INST-MODE-010` | `STATICALLY_ALIGNED` | Add Service loads real `kdc_type`, validates and POSTs/PUTs the descriptor, prefetches CSV, and exposes Manual ownership |
| `INST-MODE-011` | `STATICALLY_ALIGNED` | Both new-cluster and Add Host derive HDPWIN from stack state and retain automatic PowerShell bootstrap semantics |
| `INST-ENTRY-001` | `STATICALLY_ALIGNED` | `/installer/:stepNumber` requires `AMBARI.ADD_DELETE_CLUSTERS` and passes the active-operation guard |
| `INST-ENTRY-002` | `STATICALLY_ALIGNED` | Add Service route has feature, permission, and operation guards |
| `INST-ENTRY-003` | `STATICALLY_ALIGNED` | Add Host direct entry requires `HOST.ADD_DELETE_HOSTS` and passes the active-operation guard |
| `INST-FLOW-001` | `PARTIAL` | Landing can select Installer and local persisted step can restore; complete provisioning-state routing is not reconciled here |
| `INST-FLOW-002` | `STATICALLY_ALIGNED` | Providers synchronously update state snapshots, serialize writes, and handlers await destination persistence before navigation |
| `INST-FLOW-003` | `STATICALLY_ALIGNED` | All three providers map Classic deployment checkpoints to their actual React Deploy or Summary steps |
| `INST-FLOW-004` | `PARTIAL` | Cancel serializes owned cleanup before navigation; this accepted React ownership policy intentionally differs from Classic retention |
| `INST-FLOW-005` | `PARTIAL` | Persistence and deployment handlers lock or await transitions, but a single generic footer-level double-click lock is not yet universal |
| `INST-FLOW-006` | `STATICALLY_ALIGNED` | Installation providers claim and release their Classic-compatible wizard owner names and reject conflicting operation entry |

### Steps 0 Through 3

| ID | Current status | Classic behavior versus current React |
| --- | --- | --- |
| `INST-0-001` | `STATICALLY_ALIGNED` | Name required, length, whitespace, and special-character validation exist |
| `INST-0-002` | `PARTIAL` | Version definitions load in Step 1 rather than stacks in Step 0; load failure has no focused recovery UI |
| `INST-1-001` | `STATICALLY_ALIGNED` | React queries definitions and operating systems with the selected stack and returns to Name on an empty or failed inventory |
| `INST-1-002` | `STATICALLY_ALIGNED` | Public/Local controls, Public default, server repository URLs, and network warning align statically |
| `INST-1-003` | `STATICALLY_ALIGNED` | File and URL dry-run source type, payload, and headers persist through Review submission |
| `INST-1-004` | `PARTIAL` | Edit/add/remove/restore and syntax validation exist; uniqueness, autocomplete, and server contract are incomplete |
| `INST-1-005` | `PARTIAL` | Next can rerun validation, but there is no explicit failed-repository Retry lifecycle |
| `INST-1-006` | `STATICALLY_ALIGNED` | Ambari Server JDK is compared with inclusive stack ranges and an explicit Proceed Anyway branch |
| `INST-1-007` | `STATICALLY_ALIGNED` | Satellite disables URL verification and produces unmanaged operating-system repository payloads |
| `INST-2-001` | `STATICALLY_ALIGNED` | Input is lowercased, whitespace-split, pattern-expanded, deduplicated, and installed hosts are filtered |
| `INST-2-002` | `STATICALLY_ALIGNED` | SSH fields validate and Agent user visibility/defaults consume `customizeAgentUserAccount` |
| `INST-2-003` | `STATICALLY_ALIGNED` | Manual instructions, no bootstrap, and registration-only wait exist |
| `INST-2-004` | `STATICALLY_ALIGNED` | Suspicious FQDN and mixed installed-host confirmations exist |
| `INST-2-005` | `STATICALLY_ALIGNED` | Add Host persists Skip Host Checks, confirms the risk, and still runs the independent JDK boundary |
| `INST-3-001` | `STATICALLY_ALIGNED` | Bootstrap request ID, host states, registration deadline, serialized polling, and cleanup persist across refresh |
| `INST-3-002` | `STATICALLY_ALIGNED` | Registration poll is serialized and uses 120-second automatic/15-second manual timeouts |
| `INST-3-003` | `STATICALLY_ALIGNED` | Status filters and read-only bootstrap logs exist |
| `INST-3-004` | `PARTIAL` | Retry resets all failed hosts; selected-subset retry and refresh continuity are absent |
| `INST-3-005` | `STATICALLY_ALIGNED` | Removal is local only and at least one registered host gates Next |
| `INST-3-006` | `PARTIAL` | Preinstalled request/task polling and warning parsing exist; complete category and warning-acceptance parity needs tests |
| `INST-3-007` | `STATICALLY_ALIGNED` | A separate custom-JDK `/requests` action is polled and `java_home_check.exit_code` is parsed per host |
| `INST-3-008` | `STATICALLY_ALIGNED` | Other registered Agents are discovered and can be included |

### Steps 4 Through 7

| ID | Current status | Classic behavior versus current React |
| --- | --- | --- |
| `INST-4-001` | `PARTIAL` | Installable service list and selection exist; complete filesystem grouping and cancel behavior remain |
| `INST-4-002` | `PARTIAL` | Required-service prompts exist; transitive and already-installed dependency cases lack focused evidence |
| `INST-4-003` | `PARTIAL` | Several filesystem/service conflicts are checked; the complete stack-conditional matrix remains |
| `INST-4-004` | `STATICALLY_ALIGNED` | Choose Services validation remains client-side and does not invent Advisor validation |
| `INST-5-001` | `PARTIAL` | Advisor recommendations load, but React adds a hard-coded ZooKeeper placement and assumes response topology |
| `INST-5-002` | `STATICALLY_ALIGNED` | React attaches only matching non-installed host-component ERROR/WARN issues and requires explicit Continue Anyway acceptance without hard-blocking |
| `INST-5-003` | `PARTIAL` | Validation discards stale responses, but complete re-entry and dynamic service-change recommendation behavior remains untested |
| `INST-6-001` | `STATICALLY_ALIGNED` | Host/component matrix, All/None, required, and disabled selections exist |
| `INST-6-002` | `STATICALLY_ALIGNED` | Master plus slave/client Blueprint and hidden required components are produced |
| `INST-6-003` | `PARTIAL` | Server validation and Continue Anyway modal exist; exact general/host/component issue mapping needs coverage |
| `INST-6-004` | `PARTIAL` | Persisted selections are consumed, but refresh/back recommendation preservation is not focused-tested |
| `INST-7-001` | `PARTIAL` | Service/theme/category configuration exists; all stack control types and fallback themes remain |
| `INST-7-002` | `PARTIAL` | Accounts and Credentials are dedicated; complete Database/Directory tab and validation semantics remain |
| `INST-7-003` | `PARTIAL` | Recommendations, dependencies, and required values exist; rejection/required matrices remain |
| `INST-7-004` | `PARTIAL` | Shared connection test paths exist; all conditional services and recovery remain |
| `INST-7-005` | `PARTIAL` | Existing values/overrides are loaded for Add Service; host override/config-group parity is incomplete |
| `INST-7-006` | `STATICALLY_ALIGNED` | The new-cluster-only `preInstallChecks` flag exposes Classic's placeholder and warns before an unchecked Next |
| `INST-7-007` | `PARTIAL` | Config-derived assignments are calculated, but Review Blueprint propagation lacks focused evidence |
| `INST-7-008` | `PARTIAL` | Generic sidebar warning always claims data loss; it does not detect actual dirty configuration state |

### Review, Deploy, and Summary

| ID | Current status | Classic behavior versus current React |
| --- | --- | --- |
| `INST-8-001` | `PARTIAL` | Review shows cluster, hosts, repositories, services, and assignments; complete config and expandable host detail are absent |
| `INST-8-002` | `STATICALLY_ALIGNED` | Print Review invokes the browser print workflow before deployment locks navigation |
| `INST-8-003` | `STATICALLY_ALIGNED` | Review prefetches identities for every non-empty KDC type and downloads `kerberos.csv` with visible retry |
| `INST-8-004` | `STATICALLY_ALIGNED` | Review generates a local ZIP containing `blueprint.json` and `clustertemplate.json` from current assignments and configs |
| `INST-8-005` | `STATICALLY_ALIGNED` | Cluster inventory and parallel deletion failures stop preparation, remain visible, and retry from reconciled server inventory |
| `INST-8-008` | `STATICALLY_ALIGNED` | Cluster deletes run as an awaited parallel stage; only a wholly successful stage is checkpointed |
| `INST-8-009` | `STATICALLY_ALIGNED` | Repository-version inventory/deletion is an awaited stage with visible failure and retry |
| `INST-8-006` | `STATICALLY_ALIGNED` | Dependency stages run serially, abort on first failed prerequisite, checkpoint completion IDs, and synchronize config groups |
| `INST-8-007` | `STATICALLY_ALIGNED` | Review reuses the validated XML or URL source and persists repository artifacts before later stages |
| `INST-9-001` | `STATICALLY_ALIGNED` | Deploy owns explicit install/keytab/start phases, valid queries, request IDs, and serialized polling |
| `INST-9-002` | `PARTIAL` | Task modal exists, but lazy log loading returns when a task ID exists and there is no Classic copy/new-window parity |
| `INST-9-003` | `PARTIAL` | Host status filters and failures exist; heartbeat and failed-master branches contain unsafe model-style calls on plain objects |
| `INST-9-004` | `STATICALLY_ALIGNED` | Retry appears only for exact `INSTALL FAILED`; Summary uses wizard-specific terminal gates |
| `INST-9-005` | `STATICALLY_ALIGNED` | React Router and `beforeunload` blockers protect active deployment, with an explicit Leave decision |
| `INST-9-006` | `STATICALLY_ALIGNED` | Next waits for the wizard-specific `*_INSTALLED_4` checkpoint and is disabled outside defined terminal states |
| `INST-10-001` | `PARTIAL` | Summary derives host/master/start results, but service-check and recovered-request fidelity remain incomplete |
| `INST-10-003` | `STATICALLY_ALIGNED` | No Back entry is rendered on Summary |
| `INST-10-002` | `STATICALLY_ALIGNED` | Only new-cluster completion writes provisioning `INSTALLED`; completion flushes owned state and failures stay visibly retryable |

## Authoritative API Contract and Order

All URLs are relative to `/api/v1`. Path values must be encoded at the segment
boundary. Query expressions and payloads shown below are contract data, not
display labels.

### Selection, Bootstrap, and Validation

| Order | Method and URL | Query/payload | Current React result |
| ---: | --- | --- | --- |
| 1 | `GET /stacks` | None | Loaded at Version entry; failure returns to Name instead of presenting an empty selector |
| 2 | `GET /version_definitions` | `fields=VersionDefinition/stack_default,...` and `VersionDefinition/show_available=true&VersionDefinition/stack_name={stackName}` | Uses the selected encoded stack and exact fields without a cache-buster |
| 3 | `GET /stacks/{stack}/versions/{version}` | `fields=operating_systems/repositories/Repositories` | Uses encoded selected stack/version and preserves returned URLs |
| 4a | `POST /version_definitions?dry_run=true` | XML body with `Content-Type: text/xml` | Exact XML and header are retained for final submission |
| 4b | `POST /version_definitions?dry_run=true` | `{VersionDefinition:{version_url}}` JSON | Exact URL payload is retained for final submission |
| 5 | `PUT /stacks/{stack}/versions/{version}/operating_systems/{os}/repositories/{repo}` | `{Repositories:{base_url,repo_name,verify_base_url}}` | React performs a validation-only POST before the PUT; skip/Satellite correctly control verification and management flags |
| 6 | `GET /services/AMBARI/components/AMBARI_SERVER{fields}` | Ambari Java home, JDK location/version, and skip-check properties | Runtime values drive Agent user visibility and inclusive JDK acceptance |
| 7 | `POST /bootstrap` | `{verbose,sshKey,hosts,user,userRunAs,sshPort}`; HDPWIN retains automatic mode with empty SSH fields | Shared payload covers new cluster/Add Host, support-derived Agent user, manual bypass, and HDPWIN |
| 8 | `GET /bootstrap/{requestId}` | Poll only after prior call settles; stop on unmount/step exit | Request ID, host status, and registration deadline persist for recovery |
| 9 | `GET /hosts?fields=Hosts/host_status` | Registration poll; 120 seconds automatic, 15 seconds manual | Static behavior aligns |
| 10 | `POST /requests` | `RequestInfo.action=check_host`, check list and host resource filters | Implemented by `useHostChecks` |
| 11 | `GET /requests/{requestId}` | Request status and host-check structured output fields | Request ID and parsed results persist; complete stack categories remain in runtime acceptance |
| 12 | `POST /requests` then `GET /requests/{id}` | Separate JDK check with `java_home`, `jdk_location`, and `java_home_check.exit_code` | Implemented as an independent request even when generic Add Host checks are skipped |
| 13 | `POST {stackVersionUrl}/recommendations` | Hosts, services, Blueprint, bindings, and configuration properties according to step | Present across assignment/config screens; payload permutations need tests |
| 14 | `POST {stackVersionUrl}/validations` | Current hosts/services/Blueprint/bindings | Present; Step 5 filters to matching non-installed assignments and exposes Continue Anyway |

### Review Submission

Classic writes the `*_DEPLOY_PREP_2` recovery checkpoint before this chain. A
new-cluster submission then executes stages A through D; Add Host and Add
Service skip destructive stage A and reuse only their applicable serial
resources. React must expose a recoverable Review error and must never start the
install mutation after a resource-stage rejection.

| Stage | Method and URL | Payload/order | Concurrency and failure contract |
| --- | --- | --- | --- |
| A1 | `GET /clusters` | None | New cluster only; failure stops and unlocks React Review with Retry |
| A2 | `DELETE /clusters/{clusterName}` for every item | None | Parallel batch; wait for all; aggregate failures; no rollback |
| A3 | `GET /version_definitions` | None | Continue only after every cluster DELETE succeeds |
| A4 | `DELETE /stacks/{stack}/versions/{version}/repository_versions/{id}` | One per definition | Parallel cleanup; wait for all; no rollback; React intentionally replaces Classic's silent lock with a visible retryable failure |
| B1 | `POST /version_definitions` | Original XML body or `{VersionDefinition:{version_url}}`; no dry-run | Local repository only; source must be identical to the validated source |
| B2 | `PUT /stacks/{stack}/versions/{version}/repository_versions/{id}` | `{operating_systems:[...]}` and `ambari_managed_repositories` | After B1; record but do not silently lose a failure |
| C1 | `POST /clusters/{cluster}` | `{Clusters:{version:{stack-version-id}}}` | First abort-on-error serial resource |
| C2 | `POST /clusters/{cluster}/services` | `[{ServiceInfo:{service_name,desired_repository_version_id}}]` | After cluster creation |
| C3 | `POST /clusters/{cluster}/services?ServiceInfo/service_name={service}` | `{components:[{ServiceComponentInfo:{component_name}}]}` | One service at a time after C2 |
| C4 | `POST /clusters/{cluster}/hosts` | Host array | After service/component resources |
| C5 | `POST /clusters/{cluster}/hosts` | `{RequestInfo:{query},Body:{host_components:[...]}}` | One component association at a time |
| C6 | `PUT /clusters/{cluster}` | Array of `{Clusters:{desired_config:[...]}}` | Configs before installation |
| C7 | `POST /clusters/{cluster}/config_groups` or `PUT /clusters/{cluster}/config_groups/{id}` | Full group plus host membership payload | Required for created groups and Add Host selected existing groups |
| C8 | `POST` or `PUT /clusters/{cluster}/artifacts/kerberos_descriptor` | `{artifact_data:{...}}` | Conditional Add Service Kerberos resource; Manual saves before Review use and managed KDC saves in the deployment plan |
| D | `PUT /clusters/{cluster}/services?...` or `/host_components?...` | Install desired state plus `RequestInfo.context/query` | Only after every applicable C resource succeeds; persist returned request ID before entering Deploy |

### Deploy Polling and Completion

| Phase | Method and URL | Termination and transition |
| --- | --- | --- |
| Install | `GET /clusters/{cluster}/requests/{requestId}?fields=tasks/...&minimal_response=true` | Serialized 3-second polling; `PENDING/QUEUED/IN_PROGRESS` continue; terminal failure becomes `INSTALL FAILED`; success launches the applicable start phase |
| Host reconciliation | `GET /clusters/{cluster}/hosts?fields=Hosts/host_state,host_components/HostRoles/state` | Detect heartbeat loss and component completion before start |
| Kerberos | Conditional keytab/principal mutation and returned request poll | Required between component creation/install and start for Kerberized Add Host/Add Service |
| Start/check | `PUT /clusters/{cluster}/services?...params/run_smoke_test={boolean}` or host-component PUT | Persist the new request ID; terminal failure is `START FAILED`, not install retry |
| Task detail | `GET /clusters/{cluster}/requests/{requestId}/tasks/{taskId}` | Lazy load stdout, stderr, structured output, and error log; failure leaves the popup retryable |
| Completion checkpoint | Persist `CLUSTER_INSTALLED_4`, `ADD_HOSTS_INSTALLED_4`, or `ADD_SERVICES_INSTALLED_4` | Only terminal states enable Summary according to each wizard's Classic rules |
| Final provisioning | `PUT /clusters/{cluster}` with `{Clusters:{provisioning_state:"INSTALLED"}}` | New cluster only; Add Host/Add Service must never send it |

There are no Module 07 STOMP destinations in Classic. Installation progress is
REST-polled. React must not depend on the global realtime connection to make a
wizard reach a terminal state.

## Permissions, Flags, and Runtime Conditions

| Gate | New cluster | Add Host | Add Service | Required React boundary |
| --- | --- | --- | --- | --- |
| `AMBARI.ADD_DELETE_CLUSTERS` | Required | Not applicable | Not applicable | Entry and direct route guard; backend still authorizes mutations |
| `HOST.ADD_DELETE_HOSTS` | Not applicable | Required | Not applicable | Menu, direct route, and every mutation boundary |
| `SERVICE.ADD_DELETE_SERVICES` | Not applicable | Not applicable | Required | Menu, direct route, and every mutation boundary |
| `supports.enableAddDeleteServices` | Not applicable | Not applicable | Required | UI and route, never a security substitute |
| `supports.customizeAgentUserAccount` | Agent user shown/required only when enabled | Same | Not applicable | Use runtime supports; false forces `userRunAs=root` |
| `supports.preInstallChecks` | Shows Classic placeholder only | Not reused | Not reused | New-cluster Step 7 only |
| `supports.skipComponentStartAfterInstall` | Alters progress weights and omits start/check | Same core behavior | Same core behavior | Runtime branch in deploy state machine |
| `CLUSTER.MANAGE_USER_PERSISTED_DATA` | Controls server persistence | Same | Same | Denied users must not silently write `/persist` |
| Wizard owner | Claim during mutation workflow | Claim | Claim | Other users are non-wizard/read-only and cannot enter a competing flow |
| Upgrade state | Blocks ordinary mutation when required | Blocks | Blocks | Route plus mutation guard, with server authorization |
| Stack `HDPWIN` | PowerShell automatic bootstrap | PowerShell automatic bootstrap | Not applicable | Derived from selected/current stack, not a manual toggle |
| Component metadata | Assignment and step presence | Slave/client eligibility | Filter and conditional step skips | Cardinality, master/slave/client, HA-only, dependencies, installable/installed flags |
| Security/KDC | Initial configs may enable Kerberos later | KDC session before deploy | Descriptor, KDC type, CSV, and Manual responsibility | KDC/descriptor failures block deploy; CSV failure is visible/retryable but nonblocking like Classic |
| HA prerequisites | Selected services/configs can establish HA topology | Existing HA topology limits eligible component changes | Added service may expose HA prerequisites | Advisor/validation and live topology decide eligibility; no extra HA wizard is embedded |

React's authorization helpers correctly implement comma-separated OR behavior
and global non-owner restrictions at the application level. Module 07 must use
the semantic single permission at each entry; it must not reproduce Classic's
unguarded Add Host deep link.

## Asynchronous, Cancellation, and Recovery Matrix

| Operation | Serialization/cancellation | Retry | Refresh and failure recovery |
| --- | --- | --- | --- |
| Persist state | One write at a time; a step transition waits for its destination checkpoint | Visible initialization Retry after load failure | Never overwrite a failed hydration with empty state; retain owner and actual active step |
| Repository validation | All selected repositories may run concurrently; Next remains locked until all settle | Retry failed/all validations after edits | Preserve source, OS rows, per-repository result, skip flag, and managed mode |
| Bootstrap | One POST; one non-overlapping GET loop; clear timer on Back/unmount | Selected failed hosts only; never retry RUNNING | Persist request ID/deadline/status/log; reconcile before starting a new POST |
| Registration | One non-overlapping GET loop; clear timer on Back/unmount | Failed manual/automatic registration returns to the correct initial state | Resume remaining deadline and merge other registered Agents |
| Host checks | Create one request and poll it serially; cancel local polling on exit | Rerun refreshes `last_agent_env`; Add Host skip bypasses generic checks only | Persist request ID/results; JDK remains independent |
| Advisor/validation | Ignore stale responses after stack/service/assignment change | Retry with current Blueprint | Preserve manual choices; never apply an older response over newer state |
| Review cleanup | Cluster DELETEs concurrent within batch; version DELETEs concurrent within batch; stages are serial | Explicit stage-level Retry with partial-success disclosure | Do not repeat successful destructive work blindly; reconcile current server resources |
| Resource creation | Strict serial, abort on first error | Rebuild queue from current server/client state; created resources are not rolled back | Show failed resource and retain Review controls; never auto-install after failure |
| Add Host config group | Must finish before install, correcting Classic fire-and-forget behavior | Retry update without duplicating component resources | Re-read group membership before continuing |
| Install/start/check | One request poll at a time; stop all timers on unmount | Install Retry only for `INSTALL FAILED`; no start retry is invented | Resume persisted request; reconcile server terminal state before any mutation |
| Logs | Lazy task request; cancel display update after unmount | Popup can retry failed task detail | Previously loaded logs remain available; current task identity persists |
| Cancel before Review | Confirm once and clear only the current wizard according to accepted React ownership policy | Retry persistence cleanup if it fails | Do not navigate until the cleanup result is known |
| Cancel during Deploy | Navigation is blocked except explicitly allowed Classic destinations | Not a cancellation of server work | Refresh returns to Deploy and current request rather than starting a new request |
| Complete | Idempotent state cleanup after final provisioning boundary | A visible failure remains actionable | Add modes refresh existing cluster; new cluster enters Dashboard only after the selected provisioning policy |

## Five-Pass Cross-Check

| Pass | Independent inputs | Findings that determine implementation |
| --- | --- | --- |
| 1. Routes, pages, controls, navigation | Classic router/three route files/templates; React `RoutesList`, menus, StepWizard and footers | Direct route permission/operation gates, conditional Add Service navigation, persistence-before-navigation, Review controls, and Deploy blockers are implemented; generic double-click locking remains partial |
| 2. Controller/service/model state | Classic installer/add controllers, persist and watcher; three React providers/reducers | All providers hydrate before writing, maintain synchronous snapshots, serialize persistence, claim ownership, and map Classic checkpoints; runtime cross-window behavior remains to be exercised |
| 3. API definitions, calls, order | All five generated network inventories plus Classic AJAX registry/callers and React APIs | Dynamic stack/VDF/JDK contracts, serial Review prerequisites, request persistence, and phase polling are implemented and focused-tested; server reconciliation after an unpersisted successful mutation remains a runtime risk |
| 4. Modes, permissions, flags | Generated permission/flag inventories, `13-permissions-flags.md`, stack metadata and React AppContext | Installer/Add Host/Add Service gates, HDPWIN, Agent user, Pre Install Checks, start-skip, conditional steps, and KDC branches are wired; stack/service permutations remain in the runtime matrix |
| 5. Error, retry, refresh, back, interruption, tests | Classic tests and route/controller error paths; React Vitest inventory and error handlers | Focused tests cover route contracts, persistence, VDF/JDK, bootstrap recovery, JDK checks, Review retry/Kerberos/export, deployment phase gates, Add Host, Add Service navigation, and completion; real request/task failures remain runtime acceptance |

The Ember baseline validator passed with 1,002 feature IDs, 288 non-Metrics AJAX
definitions, 394 AJAX call sites, 19 direct HTTP calls, 56 browser entry points,
38 permissions, 23 flags, 160 route fragments, and no warnings or errors. No
new conflict between the written Module 07 baseline and executable Classic
source was found, so this audit does not edit the frozen baseline or generated
evidence.

## Executable Acceptance Criteria

1. Route tests prove Installer requires `AMBARI.ADD_DELETE_CLUSTERS`, Add Host
   requires `HOST.ADD_DELETE_HOSTS`, Add Service requires its permission and
   flag, and all three reject a conflicting non-owner or upgrade workflow.
2. State-provider tests prove hydration failure cannot write empty state,
   same-event transitions persist the destination step and dispatched data,
   writes are serialized, Cancel/Complete clear only owned keys, and refresh
   maps each server checkpoint to the intended actual step.
3. Version API tests assert dynamic stack names, exact fields/query parameters,
   XML versus URL dry-run payloads, retained source for non-dry-run POST,
   managed-repository flags, URL validation, and JDK acceptance.
4. Install Options and Confirm Hosts tests cover Linux SSH, HDPWIN PowerShell,
   manual Agent, customized/default Agent user, pattern/installed/suspicious
   hosts, automatic/manual timeout, bootstrap POST failure, serialized polling,
   selected retry/remove, Add Host skip, JDK results, and unmount cleanup.
5. Service, master, slave/client, and config tests cover dependencies,
   filesystem conflicts, cardinality, conditional Add Service step skips,
   matching validation issue filtering, Continue Anyway, manual choices after
   Back/refresh, required/dependent configs, external tests, and dirty Back.
6. Review tests assert Print, Blueprint ZIP, applicable Kerberos CSV/Manual
   confirmation, destructive cleanup batches, retained VDF source, exact serial
   resource order, abort on first failure, no install handoff on failure,
   partial-success Retry, config groups, and Kerberos descriptor ordering.
7. Deploy tests use fake timers and deferred promises to prove one poll at a
   time, request-ID persistence, no empty request, install/start/check phase
   transitions, support-flag branches, exact Retry visibility, task log detail,
   route blocking, unmount cancellation, and terminal-only Summary.
8. Summary tests prove no Back, correct service/host outcomes, new-cluster-only
   provisioning PUT, idempotent cleanup, Add Host/Add Service refresh, and
   recoverable completion failure.
9. `npm test`, focused Vitest commands, `npm run build`, baseline validation,
   `git diff --check`, and whitespace checks pass from the worktree.

## Runtime Acceptance Matrix

No Feature ID may be changed to `COVERED` from static evidence alone. Capture
browser Network requests, persisted values, server cluster status, request/task
IDs, and screenshots for each scenario.

| Scenario | Variants | Breakpoints and expected recovery | Feature IDs |
| --- | --- | --- | --- |
| New cluster repository | Public; Local XML; Local URL; Satellite; skip validation; incompatible JDK | Network loss before/after dry-run, one invalid repository, VDF POST failure, repository PUT failure, refresh on Step 1/8 | `INST-MODE-001`, `INST-MODE-004` through `006`, `INST-1-*`, `INST-8-007` |
| Host onboarding | Linux SSH; manual Agent; HDPWIN PowerShell; customized/default Agent user | Bootstrap POST failure, mid-poll refresh, one failed host, timeout, other Agent, remove/retry, Back/unmount | `INST-MODE-007`, `008`, `011`, `INST-2-*`, `INST-3-*` |
| Host checks | New cluster; Add Host regular; Add Host skip | Generic check create/poll failure, JDK failure, repository/disk/THP warning, rerun, refresh | `INST-2-005`, `INST-3-006`, `007` |
| Service selection | HDFS-compatible filesystem; multiple DFS; Ozone/Spark; Ranger dependencies; service with no masters/slaves/configs | Accept WARNING, reject CRITICAL, Back and change selection, refresh each assignment step | `INST-4-*`, `INST-5-*`, `INST-6-*` |
| Config customization | Accounts; credentials; database; directories; required/dependent values; override/config group | Advisor failure, DB test failure/retry, dirty Back, dynamic component change, refresh | `INST-7-*` |
| New-cluster cleanup | Zero/one/multiple existing clusters and version definitions | GET failure, one parallel DELETE failure, partial deletion, refresh and Retry | `INST-8-005`, `008`, `009` |
| Resource creation | Minimal stack and multi-service stack | Failure at every serial resource stage, partial prior success, refresh, resubmit without duplicate install | `INST-8-006` |
| Add Host | Component hosts; client-only; no component; default/non-default config groups | Group PUT failure, install failure/retry, start failure, Kerberos keytab failure, refresh in every phase | `INST-MODE-002`, `009`, all reused IDs |
| Add Service | Master-only; slave/client-only; config-free; all steps | Conditional skips, descriptor create/update failure, each KDC type, CSV, Manual acknowledgment, install/start failure, refresh | `INST-MODE-003`, `010`, all reused IDs |
| Deploy | Service checks on/off; `skipComponentStartAfterInstall` true/false | Install poll 403/500/network recovery, task failure, heartbeat loss, task log failure, route attempt, browser refresh | `INST-9-*` |
| Completion | Success; warnings; install failure; start failure; provisioning PUT failure | Refresh before Complete, double click, failed cleanup, dashboard/Hosts refresh | `INST-10-*` |
| RBAC/ownership | Ambari admin, cluster admin without Ambari permission, host/service operator, read-only, View-only, current owner, different user | Direct deep links and direct mutations; upgrade active/suspended; second browser window | `INST-ENTRY-*`, `INST-FLOW-*` |
| Scale and concurrency | 1, 50, and environment-maximum hosts; many services/repositories | Slow responses longer than poll interval, large GET override behavior, browser reload, server restart | `INST-FLOW-005`, `INST-3-*`, `INST-8-*`, `INST-9-*` |

## Issue and PR Scope

Module 07 uses one ASF JIRA, one branch named with that JIRA key, and one pull
request against `apache/ambari:frontend-refactor`. It will not edit Module 05,
Module 06, or any other module's gap document or implementation. Shared router,
wizard, API, and application-state files may receive only the minimum changes
needed to enforce Module 07 contracts, and those changes must be listed in the
pull request.
