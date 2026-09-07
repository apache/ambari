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

# React NameNode HA and JournalNode Management Gap Analysis

## Scope and Audit Result

This document compares the executable Classic Ember implementation, the Ambari REST contracts, and the React implementation for Module 09. The audit covers all 185 stable IDs in `09-namenode-journalnode-ha.md`. Metrics product pages and global metric refresh are excluded. The checkpoint and JournalNode formatted fields below `metrics/dfs/...` remain in scope because they are installation safety gates.

Status meanings:

| Status | Meaning |
| --- | --- |
| `MATCH` | React implements the reachable Classic behavior and contract. |
| `PARTIAL` | The main path exists, but a condition, branch, request, recovery path, or user-visible behavior is incomplete. |
| `DIFFERENT` | React implements behavior that conflicts with the Classic contract or would produce an incorrect server-side result. |
| `MISSING` | No usable React implementation exists. |
| `NOT_APPLICABLE` | Classic code is unreachable, placeholder-only, or explicitly excluded from parity. |

The confirmed migration gaps in the normal NNHA and Manage JournalNodes paths have been implemented. The shared progress runner now persists a task checkpoint before executing or advancing, resumes saved request IDs after refresh, treats every terminal failure state consistently, and fails closed on install or KDC prerequisites. The remaining product gap is the Stack Upgrade custom-check shortcut into NNHA. Stack-specific combinations, real Kerberos credential stores, Windows JournalNode defaults, server restart, and multi-user recovery still require the runtime matrix below and are not claimed from unit evidence.

Product telemetry is provided by the independent Prometheus monitoring path and does not participate in NNHA configuration submission. These workflows retain only the small JMX-derived control-plane fields required to validate NameNode checkpoints, NameNode journal state, and JournalNode formatted state. Legacy AMS service detection and `ams-hbase-site` migration are not part of the React contract.

Classic automatic rollback and Disable NNHA remain explicitly out of scope because the executable Classic tree contains only placeholder, unreachable, or incorrectly routed implementations. Metrics product pages remain excluded.

## State Machines

### Enable NameNode HA

| State | Forward condition and side effects | Back/exit and recovery | React result |
| --- | --- | --- | --- |
| Entry | `SERVICE.ENABLE_HA`, persisted-data capability, HDFS HA support, HA disabled, at least 3 hosts, at least 3 ZooKeeper Servers, started current NN, and no HDFS master maintenance | Invalid entry remains outside the wizard; a valid persisted owner resumes its saved step | Menu and direct route enforce service and persistence authorization; validation enforces host/ZooKeeper/NN/maintenance/HA state. `wizard-data.userName` protects another user's active workflow. Exact stack combinations remain runtime validation. |
| Step 1 Get Started | Valid 1-63 character Nameservice ID; save ID and clear later topology/config snapshots | No Back; early Cancel clears pre-change state and owner | React restores the ID, loads `hadoop-env/hdfs_user`, shows HBase/conditional HAWQ warnings, invalidates downstream keys, and awaits persistence before advancing. |
| Step 2 Select Hosts | Exactly one additional NN and at least 3 JNs; unique valid hosts; all selected JNs are retained | Back to Step 1 and invalidate Review/later state | Assignment and Advisor calls are retained; Next is tied to the complete unique NN/JN selection and the saved snapshot is persisted before navigation. |
| Step 3 Review | Load exact desired tags/config versions; generate HA and dependency changes; only JN edits directory is editable; save immutable snapshot | Back to Step 2; load/generation error stays on Review with Retry | React clones descriptors/state for every build, exposes tag/topology/generation errors, writes the editable directory into the submitted `hdfs-site` snapshot, and preserves exact desired versions. |
| Step 4 Checkpoint | Poll current NN; require Safemode and transaction delta <= 1; then pass KDC session gate | No Back; early Cancel clears; parse/GET/KDC failures remain recoverable | Malformed metrics and transport failures are visible without enabling Next. Started state remains the Classic informational warning, and KDC errors stay on the step. |
| Step 5 Configure | Strictly stop all, install NN, install all JNs, save HDFS/Ranger, install HDFS clients, start all JNs, disable SNN | No Back; command Retry; critical exit preserves owner/checkpoint | The install chain stops on create/register/install/KDC failure, secure clusters reload current tags before overlay, every request/status is checkpointed, and Retry targets the failed operation. |
| Step 6 Initialize JNs | Poll every selected JN as a complete set; all must report this nameservice formatted | No Back; critical exit preserves owner/checkpoint | Next starts disabled and requires a host-keyed response from every selected JN, including 4/5-node sets; malformed/missing results remain visible and polling retries. |
| Step 7 Start Components | Strictly start ZK, conditional Infra/MySQL/Ranger, then current NN | No Back; command Retry and critical exit | React reloads authoritative component topology, builds only applicable dependency tasks, restores saved operations, and offers Retry when topology loading fails. |
| Step 8 Initialize Metadata | Manual `formatZK` and `bootstrapStandby`; KDC gate; explicit completion confirmation | No Back; critical exit/rollback boundary | Both commands use the configured HDFS user. KDC validation precedes an explicit manual-completion confirmation and persistence failure remains on the step. |
| Step 9 Finalize | Strictly start new NN, install/start ZKFC, conditional PXF/dependency configs, delete SNN, stop HDFS, start all | Complete clears owner only after persistence; Retry resumes first failure; no Back | PXF, complete Ranger groups, HBase/Accumulo, HAWQ plus `hdfs-client`, SNN deletion, HDFS stop, and start-all are ordered. Prometheus configuration remains independent. HAWQ acknowledgement and completion-persistence errors are visible. |

### Manage JournalNodes

| State | Forward condition and side effects | Back/exit and recovery | React result |
| --- | --- | --- | --- |
| Entry | Unified mutation/persistence policy; HA topology; Active and Standby NN labels; action cardinality permits add or delete | Invalid direct route is blocked; saved owner resumes | Service action and route enforce feature flag, persisted-data permission, Classic's service-permission alternatives, HA/cardinality, and Active/Standby validation. Host Details add/delete routes to the same wizard under its host permission; add uses the KDC gate. |
| Step 1 Assign | Start with current JNs; final set >= 3; maximum `min(stack/hosts, existing*2-1)`; no-op disabled | No Back; early Cancel clears pre-change state | Current JNs are retained without an unsolicited add. Next requires a real change, minimum three, and the Classic host/existing-count maximum. |
| Step 2 Review | Compute exact add/delete sets; non-Federation or every Federation shared-edits key; save deduplicated snapshot | Back to Step 1; delete-only goes to Step 4 | React builds exact delta sets and fresh one-per-namespace descriptors. Delete-only hides Steps 3/5, renumbers the navigation, persists the mode, and jumps to Step 4. |
| Step 3 Save Namespace | One command set per namespace; exact namespace-to-check-host response set must pass | Back to Review; omitted in delete-only | Federation commands include `-fs hdfs://<ns>` and the aggregate endpoint must return exactly one response for every selected host; incomplete/duplicate/malformed sets cannot pass. |
| Step 4 Add/Remove | Strictly stop standby NN, stop all, install adds, await all deletes, save HDFS, install clients | No Back; command Retry; critical exit preserves owner/checkpoint | Installs and config saves fail closed, all deletes are awaited with missing resources idempotent, operations restore after refresh, and persistence precedes each command. |
| Step 5 Copy Directories | Show deduplicated directories for one/all namespaces; manual completion | No Back; omitted in delete-only | React requires every namespace directory property, deduplicates displayed paths, exposes load/missing-property Retry, and uses manual acknowledgement. |
| Step 6 Start JNs | Reload final server topology, then start exactly the final JN set | No Back; command Retry | An authoritative component GET must return at least three final JNs before the restored start operation can execute; loading failure exposes Retry. |
| Step 7 Start All | Start all services and poll; clear owner only after durable completion | Finish returns to HDFS Summary | Saved operation status resumes. Finish awaits owner/workflow clearing, reports persistence failure, and navigates only after success. |

## Feature ID Status

### Scope and Entry

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| NNHA-SCOPE-001 | `MATCH` | A modal nine-step route exists and stores step state. |
| NNHA-SCOPE-002 | `MATCH` | Manage JN has seven normal steps and five visible, renumbered delete-only steps. |
| NNHA-SCOPE-003 | `MATCH` | Product Metrics is absent while checkpoint/JN formatted fields are read. |
| NNHA-SCOPE-004 | `NOT_APPLICABLE` | Legacy AMS and `ams-hbase-site` migration were removed. Prometheus owns product telemetry; the workflow reads only its minimal JMX-derived HA safety fields. |
| NNHA-SCOPE-005 | `MATCH` | This audit uses executable routes, operations, API helpers, and tests rather than inventory hits. |
| NNHA-ENTRY-001 | `MATCH` | The HDFS action uses its `HA_MODE` service model, `SERVICE.ENABLE_HA`, persisted-data permission, HA-disabled state, and validation disable reasons. |
| NNHA-ENTRY-002 | `MATCH` | React exposes no Disable NNHA entry. |
| NNHA-ENTRY-003 | `MISSING` | The upgrade custom-check entry does not route into the React wizard. |
| NNHA-ENTRY-004 | `MATCH` | Validation waits for component data, requires a started NN, and fails gracefully when the NN is absent. |
| NNHA-ENTRY-005 | `MATCH` | React requires three ZooKeeper Server components and does not require STARTED state. |
| NNHA-ENTRY-006 | `MATCH` | Both entry validation and assignment require the three-host minimum. |
| NNHA-ENTRY-007 | `MATCH` | Explicit and implied master maintenance states block entry. |
| NNHA-ENTRY-008 | `MATCH` | Direct routes enforce service/persistence authorization and wizard ownership, then run the same host, HA, NN, ZooKeeper, and maintenance validation. |

### NameNode HA Steps and Configuration

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| NNHA-STEP1-001 | `MATCH` | ID validation, restored input, configured HDFS user, downstream invalidation, and awaited save are implemented. |
| NNHA-STEP1-002 | `MATCH` | HBase warning is informational. |
| NNHA-STEP1-003 | `MATCH` | HAWQ installations display the Classic filespace warning at entry and completion. |
| NNHA-STEP2-001 | `MATCH` | One additional NN and at least three JNs are seeded through Advisor; readiness and complete assignment validity gate Next. |
| NNHA-STEP2-002 | `MATCH` | Duplicate/empty/invalid NN or JN assignment keeps Next disabled. |
| NNHA-STEP2-003 | `MATCH` | JN Add uses stack cardinality and host count rather than a hard maximum of three. |
| NNHA-STEP2-004 | `MATCH` | Back removes downstream normal-flow snapshots and persistence completes before navigation; placeholder rollback data is not retained. |
| NNHA-STEP3-001 | `MATCH` | Review displays the exact hosts/configs and exposes topology, tag, fetch, and build failures with Retry. |
| NNHA-STEP3-002 | `MATCH` | Only `dfs.journalnode.edits.dir` is editable. |
| NNHA-STEP3-003 | `MATCH` | Required dependency sites are loaded by exact desired tags; fresh cloned descriptors prevent cross-load mutation. |
| NNHA-STEP3-004 | `MATCH` | The immutable submitted snapshot, including edited JN directory value, is serialized before navigation. |
| NNHA-STEP4-001 | `MATCH` | Commands and repeated checkpoint GET are present. |
| NNHA-STEP4-002 | `MATCH` | Safemode and transaction delta form the Next gate. |
| NNHA-STEP4-003 | `MATCH` | A non-started NN displays the Classic warning but, as in Classic, is not added to the checkpoint Next calculation. |
| NNHA-STEP4-004 | `MATCH` | Manual/automatic KDC gates remain on the step after cancellation, transport, or credential-save failure and expose an error. |
| NNHA-STEP5-001 | `MATCH` | Stop-all request is first and request polling is used. |
| NNHA-STEP5-002 | `MATCH` | The additional NN chain performs existence, optional component creation, registration, then install, and stops on any non-idempotent failure. |
| NNHA-STEP5-003 | `MATCH` | The same fail-closed, idempotent chain covers the complete selected JN set. |
| NNHA-STEP5-004 | `MATCH` | HDFS/Ranger save and HDFS client install are ordered and any save/install failure stops the task. |
| NNHA-STEP5-005 | `MATCH` | Secure mode reloads exact current tags/configs, overlays Review changes, removes obsolete keys, and submits the merged snapshot. |
| NNHA-STEP5-006 | `MATCH` | All selected JNs are sent to a STARTED host-component update. |
| NNHA-STEP5-007 | `MATCH` | SNN maintenance is set to ON without deleting it. |
| NNHA-STEP6-001 | `MATCH` | The configured HDFS user command is shown and one poll cycle concurrently fetches each selected JN. |
| NNHA-STEP6-002 | `MATCH` | Malformed/missing formatted JSON is handled without throwing and Next starts disabled. |
| NNHA-STEP6-003 | `MATCH` | A host-keyed complete-set barrier requires every selected JN, including fourth/fifth responses. |
| NNHA-STEP7-001 | `MATCH` | ZooKeeper Server hosts are started first. |
| NNHA-STEP7-002 | `MATCH` | Authoritative component topology conditionally adds the installed Ambari Infra service task. |
| NNHA-STEP7-003 | `MATCH` | Installed MySQL Server hosts are conditionally started before Ranger/current NN. |
| NNHA-STEP7-004 | `MATCH` | Installed Ranger Admin hosts are resolved from authoritative topology and started conditionally. |
| NNHA-STEP7-005 | `MATCH` | Only the installed/original NN is started last. |
| NNHA-STEP8-001 | `MATCH` | `hdfs zkfc -formatZK` is shown without server validation. |
| NNHA-STEP8-002 | `MATCH` | `hdfs namenode -bootstrapStandby` is shown for the additional NN. |
| NNHA-STEP8-003 | `MATCH` | KDC validation is followed by explicit manual-completion confirmation before Step 9. |
| NNHA-STEP9-001 | `MATCH` | The additional NN is started and polled. |
| NNHA-STEP9-002 | `MATCH` | ZKFC existence/create/register/install/start is fail closed on both NN hosts. |
| NNHA-STEP9-003 | `MATCH` | Historical PXF is installed on the additional NN only when the service is present and the component is absent there. |
| NNHA-STEP9-004 | `MATCH` | Ranger env and every installed YARN/Storm/Kafka/Knox/Atlas/Hive/KMS group are loaded and submitted using JavaScript-safe lookups. |
| NNHA-STEP9-005 | `MATCH` | HBase and present Ranger HBase sites are saved together. |
| NNHA-STEP9-006 | `NOT_APPLICABLE` | Step 9 has no AMS service branch and never submits `ams-hbase-site`; Prometheus collection configuration is managed outside NNHA. |
| NNHA-STEP9-007 | `MATCH` | `accumulo-site` is conditionally submitted. |
| NNHA-STEP9-008 | `MATCH` | `hawq-site` and `hdfs-client` are saved together. |
| NNHA-STEP9-009 | `MATCH` | SNN host-component is deleted directly. |
| NNHA-STEP9-010 | `MATCH` | HDFS is stopped, then all services are started without smoke tests. |
| NNHA-STEP9-011 | `MATCH` | HAWQ acknowledgement precedes an awaited owner/checkpoint clear; clear failure remains visible and blocks navigation. |
| NNHA-CONFIG-001 | `MATCH` | Both nameservice properties use the entered ID. |
| NNHA-CONFIG-002 | `MATCH` | `nn1,nn2` and configured failover provider are generated. |
| NNHA-CONFIG-003 | `MATCH` | Current RPC port and additional default 8020 are generated. |
| NNHA-CONFIG-004 | `MATCH` | Current HTTP/HTTPS ports and additional defaults are generated. |
| NNHA-CONFIG-005 | `MATCH` | All selected JNs are joined with `;` at port 8485. |
| NNHA-CONFIG-006 | `MATCH` | `fs.defaultFS` and comma-separated ZooKeeper quorum are generated. |
| NNHA-CONFIG-007 | `MATCH` | Fencing, automatic failover, and safemode values come from the descriptor. |
| NNHA-CONFIG-008 | `MATCH` | Old keys are removed both from Review and the authoritative secure reload/overlay submission. |
| NNHA-CONFIG-009 | `PARTIAL` | Default/editability match; the Windows current-value branch is absent. |
| NNHA-CONFIG-010 | `MATCH` | HBase authority replacement matches Classic. |
| NNHA-CONFIG-011 | `MATCH` | Accumulo volume/replacements are generated. |
| NNHA-CONFIG-012 | `NOT_APPLICABLE` | The removed AMS service has no NNHA configuration branch. Minimal JMX control-plane reads do not mutate monitoring configuration. |
| NNHA-CONFIG-013 | `MATCH` | HAWQ URL migration and the completion filespace acknowledgement are both present. |
| NNHA-CONFIG-014 | `MATCH` | Generated `hdfs-client` values are submitted with `hawq-site`. |
| NNHA-CONFIG-015 | `MATCH` | HDFS/HBase/Accumulo and all applicable Ranger/HAWQ sites are submitted from the reviewed snapshot; no legacy AMS site is loaded or submitted. |
| NNHA-CONFIG-016 | `MATCH` | Ranger's multi-configuration PUT contains one desired-config body per installed service group. |

### JournalNode Entry, Modes, and Steps

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| JN-ENTRY-001 | `MATCH` | Service action requires the feature flag, HDFS HA model, and exact `(hosts > JNs || JNs > 3)` cardinality. |
| JN-ENTRY-002 | `MATCH` | Menu and route accept Classic's custom-command, service-check, maintenance, or enable-HA service permissions, plus the host permission for Host Details. |
| JN-ENTRY-003 | `MATCH` | Active and Standby HA labels are required; work state is not checked. |
| JN-ENTRY-004 | `MATCH` | Host Details add/delete uses `HOST.ADD_DELETE_COMPONENTS`, routes to this wizard, applies KDC before add, and does not require KDC for delete. |
| JN-ENTRY-005 | `MATCH` | Host Details disables deletion at three JNs and the wizard independently enforces the same minimum. |
| JN-MODE-001 | `MATCH` | Add-only begins from the live set, records only added hosts, and retains all seven steps. |
| JN-MODE-002 | `MATCH` | Delete-only hides Save Namespace and Copy Directories, jumps to Step 4, and renders five numbered steps. |
| JN-MODE-003 | `MATCH` | Mixed mode can reach all seven steps. |
| JN-MODE-004 | `MATCH` | No-op, under-minimum, over-maximum, and duplicate final sets keep Next disabled. |
| JN-STEP1-001 | `MATCH` | Current assignment, add/remove controls, Advisor readiness, uniqueness, minimum/maximum, and real-change validation gate Next. |
| JN-STEP1-002 | `MATCH` | Maximum is `min(hostCount, max(3, existingCount * 2 - 1))`. |
| JN-STEP1-003 | `MATCH` | Final topology is stored from service or Host Details entry and serialized before navigation. |
| JN-STEP2-001 | `MATCH` | Add/delete Review and read-only shared-edits config are shown after load. |
| JN-STEP2-002 | `MATCH` | Non-Federation shared-edits is generated from the final JN set. |
| JN-STEP2-003 | `MATCH` | Federation creates exactly one `dfs.namenode.shared.edits.dir.<ns>` descriptor per nameservice. |
| JN-STEP2-004 | `MATCH` | Fresh descriptors and exact add/delete deltas are persisted; delete-only hides two steps and jumps to Add/Remove. |
| JN-STEP3-001 | `MATCH` | Single-namespace commands and checkpoint gate exist. |
| JN-STEP3-002 | `MATCH` | Federation renders safemode/checkpoint commands with `-fs hdfs://<ns>` for every nameservice. |
| JN-STEP3-003 | `MATCH` | React derives active hosts and falls back to started/first hosts by namespace. |
| JN-STEP3-004 | `MATCH` | Every response must pass checkpoint metrics; desired-state and parse/transport failures are visible while polling retries. |
| JN-STEP3-005 | `MATCH` | The aggregate host-component endpoint is used and exact host identity/count rejects missing, duplicate, or extra subsets. |
| JN-STEP4-001 | `MATCH` | Saved Standby NN is stopped first. |
| JN-STEP4-002 | `MATCH` | All services are stopped second. |
| JN-STEP4-003 | `MATCH` | Empty add sets complete idempotently; non-empty sets use the fail-closed component install chain. |
| JN-STEP4-004 | `MATCH` | All deletes are awaited and explicit 404/NoSuchResource responses are idempotent. |
| JN-STEP4-005 | `MATCH` | React waits for every DELETE promise before reconfiguration. |
| JN-STEP4-006 | `MATCH` | Final `hdfs-site` save precedes HDFS client install and any failure stops the sequence. |
| JN-STEP5-001 | `MATCH` | Single/Federation directory properties are required and deduplicated; missing config exposes Retry. |
| JN-STEP5-002 | `MATCH` | Next is a manual acknowledgment and performs no validation. |
| JN-STEP6-001 | `MATCH` | An authoritative post-change component GET supplies the exact final JN hosts before STARTED. |
| JN-STEP7-001 | `MATCH` | Start-all resumes from persisted status; owner/state clearing is awaited and failures block navigation. |

### Progress, Recovery, Rollback, and Risks

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| NNHA-PROGRESS-001 | `MATCH` | Operations are serial and each QUEUED/request/status checkpoint must persist before callback execution or advancement. |
| NNHA-PROGRESS-002 | `MATCH` | Requests poll at two seconds; transient transport/incomplete responses remain visible and schedule controlled retry. |
| NNHA-PROGRESS-003 | `MATCH` | `FAILED`, `TIMEDOUT`, and `ABORTED` are terminal failures; only `COMPLETED` advances. |
| NNHA-PROGRESS-004 | `MATCH` | Refresh restores completed/in-progress/failed tasks and Retry reruns only the active failed operation. |
| NNHA-PROGRESS-005 | `MATCH` | No normal task exposes Skip. |
| NNHA-PROGRESS-006 | `MATCH` | Request-bearing operations open Background Operations and task logs. |
| NNHA-PROGRESS-007 | `MATCH` | Manual Kerberos passes directly; automatic KDC cancellation, transport, lookup, or credential-save failure rejects the task with a visible error. |
| NNHA-PROGRESS-008 | `MATCH` | Create/register/install is fail closed; only explicit already-exists service-component responses continue idempotently. |
| NNHA-PROGRESS-009 | `MATCH` | Existence/topology GETs precede installs and authoritative reloads precede dependency and final-JN starts. |
| NNHA-PROGRESS-010 | `MATCH` | The exact batch chain runs in order and no asynchronous existence branch can return before registration/install. |
| NNHA-PROGRESS-011 | `MATCH` | Credential GET is awaited, then existing credentials use PUT and explicit 404 uses POST; save failure prevents replay. |
| NNHA-RECOVERY-001 | `MATCH` | Step/task/request state, active step, cluster status, and owner share one normalized serialized `/persist` checkpoint. |
| NNHA-RECOVERY-002 | `MATCH` | Hydration blocks rendering, restores the named step and operations, resumes request polling, and exposes Retry on load failure. |
| NNHA-RECOVERY-003 | `MATCH` | Early Cancel clears persisted state and returns to HDFS Summary. |
| NNHA-RECOVERY-004 | `MATCH` | Steps 5-9 show a manual-recovery warning and retain owner/checkpoint instead of claiming rollback. |
| NNHA-RECOVERY-005 | `NOT_APPLICABLE` | The Classic automatic rollback target is placeholder-only and is not a parity target. |
| JN-RECOVERY-001 | `MATCH` | Assignment/config/task/request/step data is serialized and every progress step consumes restored operations. |
| JN-RECOVERY-002 | `MATCH` | Critical Cancel warns and retains completed-side-effect evidence and owner; early Cancel clears pre-change state. |
| JN-RECOVERY-003 | `MATCH` | JN status, route step, workflow key, and `wizard-data.userName` restore the owner while other users are blocked. |
| NNHA-RECOVERY-006 | `MATCH` | Menu and direct route require persisted-data authorization and any failed checkpoint blocks mutation/navigation. |
| NNHA-ROLLBACK-001 | `NOT_APPLICABLE` | React correctly does not claim parity with the three-step placeholder. |
| NNHA-ROLLBACK-002 | `NOT_APPLICABLE` | Placeholder checkpoint wiring is not a migration target. |
| NNHA-ROLLBACK-003 | `NOT_APPLICABLE` | Empty completion page is not a migration target. |
| NNHA-ROLLBACK-004 | `NOT_APPLICABLE` | Hidden, incorrect Disable navigation is not implemented. |
| NNHA-ROLLBACK-005 | `NOT_APPLICABLE` | Unwired static reverse controller is not reused. |
| NNHA-ROLLBACK-006 | `NOT_APPLICABLE` | Invalid rollback route/state is not reused. |
| NNHA-ROLLBACK-007 | `NOT_APPLICABLE` | Defective rollback interfaces/types are not reused. |
| NNHA-ROLLBACK-008 | `MATCH` | React exposes no Disable or automatic rollback claim. |
| JN-RISK-001 | `MATCH` | Checkpoint/formatted JSON and polling errors are visible, non-throwing, and retried under a disabled Next gate. |
| JN-RISK-002 | `MATCH` | Complete selected-host barriers cover NNHA formatted status and Federation checkpoint response sets. |
| JN-RISK-003 | `MATCH` | All JN deletion promises are awaited. |
| JN-RISK-004 | `MATCH` | Final JN topology is reloaded from the server before STARTED. |
| JN-RISK-005 | `MATCH` | Federation validates exact expected host identity/cardinality. |
| JN-RISK-006 | `MATCH` | Review clones descriptors and assignment data on every load. |
| JN-RISK-007 | `MATCH` | Component create, register, KDC, and install failures stop the operation. |
| JN-RISK-008 | `MATCH` | Service and Host Details entries converge on feature, permissions, HA, cardinality, and persisted owner route guards. |
| JN-RISK-009 | `MATCH` | Critical exit warns and retains recoverable state instead of silently clearing it. |
| JN-RISK-010 | `MATCH` | React does not reuse or advertise the defective rollback controller. |

### API Contract

All paths below use the `/api/v1` prefix. Query values containing cluster, host, stack, service, component, tag, request, or task identifiers are URL variables.

| Feature ID | Status | React URL, method, query/payload, and order |
| --- | --- | --- |
| HA-API-001 | `MATCH` | `GET /clusters/{cluster}/hosts?fields=host_components/HostRoles/state&minimal_response=true`; base host/component topology loads before assignment. |
| HA-API-002 | `MATCH` | `GET /clusters/{cluster}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true`; assignment host details. |
| HA-API-003 | `MATCH` | Two `POST {stackVersionUrl}/recommendations` requests with `recommend:host_groups`, hosts, services, blueprint/binding. |
| HA-API-004 | `MATCH` | `GET /clusters/{cluster}?fields=Clusters/desired_configs`; config hook first. |
| HA-API-005 | `MATCH` | `GET /clusters/{cluster}/configurations?(type=X&tag=Y)\|...`; exact desired tag combinations follow HA-API-004. |
| HA-API-006 | `MATCH` | `GET /clusters/{cluster}/hosts/{host}/host_components/NAMENODE`; NNHA and single-namespace JN checkpoint. |
| HA-API-007 | `MATCH` | Federation uses `GET /clusters/{cluster}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in(...)&fields=HostRoles/desired_state,metrics/dfs/namenode&minimal_response=true`. |
| HA-API-008 | `MATCH` | One `GET .../hosts/{host}/host_components/JOURNALNODE?fields=metrics` per selected JN. |
| HA-API-009 | `MATCH` | `PUT /clusters/{cluster}/services?{service predicate}` with `{RequestInfo:{context,operation_level:{level:"CLUSTER",cluster_name}},Body:{ServiceInfo:{state}}}`; every request is polled before the next mutation. |
| HA-API-010 | `MATCH` | `PUT /clusters/{cluster}/host_components` with `RequestInfo.query=HostRoles/component_name=...&HostRoles/host_name.in(...)`, context/operation level, and `Body.HostRoles.state`. |
| HA-API-011 | `MATCH` | `PUT /clusters/{cluster}/hosts/{host}/host_components/{component}?{params}` carries the single-component state body; normal flows use HA-API-010. |
| HA-API-012 | `MATCH` | `PUT /clusters/{cluster}/hosts/{host}/host_components/SECONDARY_NAMENODE` with `{RequestInfo:{context},Body:{HostRoles:{maintenance_state:"ON"}}}`. |
| HA-API-013 | `MATCH` | `DELETE /clusters/{cluster}/hosts/{host}/host_components/{component}` deletes SNN or JN; Manage JN treats only explicit 404/NoSuchResource as idempotent. |
| HA-API-014 | `MATCH` | `PUT /clusters/{cluster}` with `{Clusters:{desired_config:[{type,properties,properties_attributes?,service_config_version_note}]}}`. |
| HA-API-015 | `MATCH` | Ranger sends one multi-body `PUT /clusters/{cluster}` with an exact desired-config body for every applicable service group. |
| HA-API-016 | `MATCH` | `GET /clusters/{cluster}/host_components?HostRoles/component_name={component}&HostRoles/host_name.in({hosts})&fields=HostRoles/host_name&minimal_response=true` precedes component registration. |
| HA-API-017 | `MATCH` | Missing service-component uses `POST /clusters/{cluster}/services?ServiceInfo/service_name={service}` with `components[].ServiceComponentInfo.component_name`; only explicit already-exists is idempotent. |
| HA-API-018 | `MATCH` | `POST /clusters/{cluster}/hosts` carries `RequestInfo.query=Hosts/host_name=...\|...` and `Body.host_components[].HostRoles.component_name`; failure prevents Install. |
| HA-API-019 | `MATCH` | `GET /clusters/{cluster}/components/?fields=ServiceComponentInfo/{service_name,category,installed_count,started_count,init_count,install_failed_count,unknown_count,total_count,display_name},host_components/HostRoles/host_name&minimal_response=true` refreshes topology. |
| HA-API-020 | `MATCH` | `GET /clusters/{cluster}/components/?fields=ServiceComponentInfo/{service_name,component_name,installed_count},host_components/HostRoles/host_name` precedes Step 7 tasks and final JN start. |
| HA-API-021 | `MATCH` | `GET /requests/{id}?fields=*,tasks/...` polls at two seconds; transient transport/incomplete responses retry without advancing. |
| HA-API-022 | `MATCH` | `GET /clusters/{cluster}/requests/{requestId}?fields=tasks/...&tasks/Tasks/stage_id={stageId}` supports request/stage detail polling. |
| HA-API-023 | `MATCH` | `GET /clusters/{cluster}/requests/{requestId}/tasks/{taskId}` retrieves stdout/stderr/output/error details. |
| HA-API-024 | `MATCH` | `GET /clusters/{cluster}/configurations/service_config_versions?service_name=KERBEROS&is_current=true` reads `kerberos-env/kdc_type`. |
| HA-API-025 | `MATCH` | `GET /clusters/{cluster}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details` is awaited; Manual Kerberos skips it. |
| HA-API-026 | `MATCH` | `GET`, then `PUT` or 404-only `POST /clusters/{cluster}/credentials/kdc.admin.credential`; payload is `{Credential:{principal,key,type}}`, and failed save prevents replay. |
| HA-API-027 | `MATCH` | `GET /persist/HIGH_AVAILIBILITY_NAMENODE` or `/persist/MANAGE_JOURNALNODES`; 404 starts fresh, other load failures block rendering with Retry. |
| HA-API-028 | `MATCH` | `POST /persist` sends JSON-string values for the workflow key, `CLUSTER_STATE`, and `wizard-data:{userName}`; mutation/navigation waits for success. |
| HA-API-029 | `NOT_APPLICABLE` | Registered rollback host loading is outside the normal React workflows. |
| HA-API-030 | `NOT_APPLICABLE` | Static unwired rollback interfaces are not migration targets. |
| HA-API-031 | `MATCH` | `GET /clusters/{cluster}/configurations/service_config_versions?service_name.in(HDFS)&is_current=true&fields=*` supplies `hadoop-env/hdfs_user` to both workflows. |

Normal NNHA request ordering must be:

1. Host/stack topology, Advisor recommendations, desired tags, and exact config versions.
2. Current NN checkpoint polling, then KDC session validation.
3. Stop-all request and terminal polling.
4. Additional NN install chain, then all selected JN install chains.
5. HDFS/Ranger config submission and HDFS client install, then start all selected JNs and disable SNN.
6. Complete-set JN formatted polling.
7. ZooKeeper/conditional dependencies/current NN starts in strict order.
8. Manual metadata confirmation and KDC validation.
9. Additional NN, ZKFC install/start, conditional dependency config saves, SNN delete, HDFS stop, and start-all in strict order.

Normal Manage JN request ordering must be:

1. Host/topology, Advisor, desired tags, and exact HDFS configuration versions.
2. For non-delete-only, exact namespace checkpoint polling.
3. Stop Standby NN, stop all services, install all added JNs, delete all removed JNs, save HDFS, and install HDFS clients.
4. For non-delete-only, manual directory-copy acknowledgment.
5. Reload final JournalNode topology, start the exact final set, then start all services.

No mutation may begin until the previous mutation is terminal and its workflow checkpoint is durably persisted. Retry repeats only the failed command or failed per-host item. Cancel stops client polling; it does not cancel an Ambari request unless an explicit request-abort API is added.

## Permission, Flag, and Stack Matrix

| Surface | Required React policy | React result / remaining validation |
| --- | --- | --- |
| NNHA menu and direct route | `SERVICE.ENABLE_HA` and `CLUSTER.MANAGE_USER_PERSISTED_DATA`; HDFS `HA_MODE`; HA disabled | Enforced in the menu/route/model; exact supported stack versions remain `NEEDS_RUNTIME_VALIDATION`. |
| NNHA business gate | At least 3 registered hosts, 3 ZooKeeper Servers, a started NN, no HDFS master explicit/implied maintenance | Enforced before rendering Step 1 and on direct routes. |
| Manage JN service action | Classic visibility under `RUN_CUSTOM_COMMAND`, `RUN_SERVICE_CHECK`, `TOGGLE_MAINTENANCE`, or `ENABLE_HA`, plus persisted-data capability | Menu and direct route use the combined service alternatives, persistence permission, feature flag, HA, and cardinality. |
| Manage JN Host Details | `HOST.ADD_DELETE_COMPONENTS`, HA/topology/cardinality gate, KDC check for add | Existing add/delete navigation is retained; add passes through KDC and delete is disabled at three JNs. |
| Manage JN route | Persisted-data plus accepted service/host mutation permission; HA enabled; Active/Standby; supported stack; add/delete cardinality | Enforced by feature/authorization/owner guards followed by workflow validation. |
| Service conditions | Ranger, HBase, Accumulo, HAWQ/PXF, Infra, and MySQL branches only when installed/component-count conditions match | Authoritative topology/config tags build only applicable tasks. Prometheus telemetry is independent, while the small JMX HA safety field set remains read-only; historical combinations remain runtime matrix cases. |
| Kerberos | Non-secure, automatic KDC, and Manual Kerberos; cancellation/error must terminate the current transition without losing owner state | Static branches are fail closed and tested; MIT/AD/IPA credential-store behavior remains runtime validation. |
| Auto rollback | Do not advertise automatic rollback or Disable NNHA parity | React correctly exposes neither. |

## Test Feature IDs

| Feature ID | Status | React evidence or required coverage |
| --- | --- | --- |
| HA-TEST-001 | `PARTIAL` | Focused NNHA Step 7 topology/Retry and Step 9 completion/HAWQ tests exist; full rendered Steps 1-9 matrix remains runtime coverage. |
| HA-TEST-002 | `PARTIAL` | Manage JN provider hydration/delete-only and Step 7 completion failure are covered; full rendered Steps 1-7 matrix remains runtime coverage. |
| HA-TEST-003 | `MATCH` | Progress tests prove pre-execution persistence, request-ID persistence Retry, terminal error retention, and Utility terminal statuses. |
| HA-TEST-004 | `PARTIAL` | Pure config helpers cover Federation generation, secure overlay, edited Review value, desired-tag errors, and Ranger groups; historical full initializer combinations remain runtime. |
| HA-TEST-005 | `PARTIAL` | Shared authorization/route guards have tests and Module 09 entry code was statically audited; the complete browser permission matrix remains runtime. |
| HA-TEST-006 | `PARTIAL` | Both providers test hydration, delete-only recovery, and persisted owner; browser/server restart and second-user behavior remain runtime. |
| HA-TEST-007 | `NOT_APPLICABLE` | Placeholder/unwired Classic rollback is not implemented; absence must remain explicit. |
| HA-TEST-008 | `MATCH` | Utility tests cover missing fourth response and a complete four-JN set independently of response order. |
| HA-TEST-009 | `PARTIAL` | Idempotent delete and topology-load Retry are covered; multi-DELETE server timing and final JN start require runtime capture. |
| HA-TEST-010 | `MATCH` | Federation utilities reject missing/duplicate hosts and accept a complete out-of-order set. |
| HA-TEST-011 | `PARTIAL` | Automatic KDC transport/cancel/save ordering and Manual bypass are tested; actual MIT/AD/IPA stores remain runtime. |
| HA-TEST-012 | `MISSING` | Full real-cluster success/recovery matrix remains required. |

## Executable Acceptance Criteria

Criteria 1-15 are implemented in the reviewed React code and focused tests. Items that depend on real Ambari agents, stack metadata, credential stores, or server/browser restart must additionally pass the runtime matrix.

1. Direct NNHA entry is rejected before opening Step 1 for missing authorization, missing persisted-data capability, unsupported stack/service, HA already enabled, fewer than three hosts/ZooKeeper Servers, missing or stopped NN, or any HDFS master maintenance state.
2. NNHA assignment cannot advance until exactly one additional NN and at least three unique valid JNs are selected; 4/5 JNs remain in the generated shared-edits URI and formatted-status barrier.
3. Review loads immutable fresh descriptors on every visit, submits exact current config snapshots, exposes load/generation errors with Retry, and never duplicates or permanently renames descriptor entries.
4. Checkpoint JSON parse and GET failures display a recoverable error and continue controlled polling after Retry; Next never enables from a missing, malformed, partial, or wrong-host response.
5. Component installation stops on service-component create, host registration, KDC, or Install failure. An explicit ResourceAlreadyExists response may continue idempotently; other errors retain the failed task and original server message.
6. Progress executes one operation at a time, treats `FAILED`, `TIMEDOUT`, and `ABORTED` as failure, persists request IDs/status before advancing, restores polling after refresh, and retries only the failed operation.
7. NNHA Step 6 waits for every selected JN, associates each response with its host, and enables Next only when every host reports the current nameservice formatted.
8. NNHA Step 8 requires explicit confirmation after a successful KDC gate. Steps 5-9 do not offer unsafe Back navigation, and critical Cancel preserves completed-side-effect evidence with a manual recovery warning.
9. NNHA Step 9 executes every applicable PXF/Ranger/HBase/Accumulo/HAWQ branch, includes `hdfs-client` for HAWQ, then deletes SNN, stops HDFS, and starts all services in order; it does not mutate Prometheus or legacy AMS configuration.
10. Manage JN begins with the live JN set, enforces minimum/maximum/no-op rules, supports add-only/delete-only/mixed, and displays five renumbered steps for delete-only.
11. Federation Review writes exactly one shared-edits property per nameservice. Checkpoint renders one command set per nameservice and requires a one-to-one expected namespace/host response set.
12. JN deletion awaits all hosts and reports failures by host. Retry targets only failed deletions. Reconfigure cannot start while any deletion is unresolved.
13. Before JN Step 6, React reloads server topology and starts exactly the final JN set, excluding deleted hosts and including every newly registered host.
14. Workflow state hydration, step transitions, task checkpoints, completion, and Cancel are serialized. A failed `/persist` request blocks further mutation/navigation and displays a retryable error.
15. Focused tests cover validation helpers, complete-set aggregation, Federation cardinality, install prerequisite failure, deletion aggregation, topology refresh, task recovery, and persistence ordering.

## Runtime Acceptance Matrix

| Scenario | Topology/mode | Fault or transition points | Required evidence | Status |
| --- | --- | --- | --- | --- |
| NNHA non-secure | 3 hosts, 3 JNs, HDFS/ZK only | Refresh before/after every Step 5/7/9 mutation | Browser route/owner snapshot, ordered HTTP capture, Ambari request/task terminal states, final HDFS config version, Active/Standby/ZKFC topology. | `NEEDS_RUNTIME_VALIDATION` |
| NNHA 5 JNs | 5+ hosts, out-of-order formatted responses | Delay the fourth/fifth JN and return malformed JSON once | Next remains disabled until all five valid responses; visible recovery; shared-edits includes all five. | `NEEDS_RUNTIME_VALIDATION` |
| NNHA automatic Kerberos | MIT, AD, and IPA where supported | Expired KDC session, popup cancel, bad credential save, retry | No mutation after cancel/failure; persisted owner survives; successful replay executes once. | `NEEDS_RUNTIME_VALIDATION` |
| NNHA Manual Kerberos | `kdc_type=none` | Refresh at install and metadata gates | No KDC admin credential required; component identities/keytabs install; progress resumes. | `NEEDS_RUNTIME_VALIDATION` |
| NNHA dependencies | Ranger, HBase, Accumulo, historical HAWQ/PXF combinations; independent Prometheus telemetry | Missing optional site/property and config-save failure | Only applicable HA dependency sites are submitted, complete snapshots are retained, failure stops the sequence, and the HAWQ warning is shown. No monitoring configuration is submitted. | `NEEDS_RUNTIME_VALIDATION` |
| NNHA Windows branch | A stack exposing the Windows JournalNode directory default | Review and secure reload | `dfs.journalnode.edits.dir` uses the stack/runtime-supported default and survives submission. | `NEEDS_RUNTIME_VALIDATION` |
| NNHA critical exit | Steps 5, 6, 7, 8, and 9 | Close modal after each completed command | Completed-side-effect list retained; no false rollback claim; manual recovery is actionable. | `NEEDS_RUNTIME_VALIDATION` |
| JN add-only | 3 to 4/5 JNs | Refresh after registration and before start | Seven steps; exact add set; final reload starts all final JNs. | `NEEDS_RUNTIME_VALIDATION` |
| JN delete-only | 5 to 3/4 JNs | One DELETE delayed and one NoSuchResource | Five renumbered steps; no checkpoint/copy; all deletions terminal before config save. | `NEEDS_RUNTIME_VALIDATION` |
| JN mixed | Replace one or more JNs | One delete fails, retry after other deletes succeed | Retry only failed host; no duplicate install/delete; final config/topology exact. | `NEEDS_RUNTIME_VALIDATION` |
| JN no-op/cardinality | 3, 4, and 5 existing JNs with varying host count | Return to Step 1 after Review | No-op disabled; minimum three and `existing*2-1` maximum enforced; snapshots invalidated correctly. | `NEEDS_RUNTIME_VALIDATION` |
| JN Federation | 2+ nameservices | Missing, duplicate, wrong-host, and non-STARTED checkpoint items | Exact namespace properties and commands; no partial-response pass. | `NEEDS_RUNTIME_VALIDATION` |
| Persistence ownership | Both workflows | Server restart, browser refresh, second user login, permission revoked | Authorized owner resumes; unauthorized user cannot mutate/overwrite; persist failure blocks progression. | `NEEDS_RUNTIME_VALIDATION` |
| Request failures | Both workflows | `FAILED`, `TIMEDOUT`, `ABORTED`, poll 500, and transient network loss | Visible terminal status, controlled polling recovery, correct Retry target, no next-command execution. | `NEEDS_RUNTIME_VALIDATION` |

## Five-Pass Audit Record

| Pass | Surface | Result |
| --- | --- | --- |
| 1 | Routes, pages, buttons, and wizard navigation | Confirmed service and Host Details entries, corrected the original React audit's false Host Details gap, unified route permissions/business gates, removed critical Back navigation, and implemented delete-only hide/renumber/jump. The upgrade custom-check shortcut remains the only missing entry. |
| 2 | Controller/service/model state and recovery | Compared Classic local DB/task/request checkpoints with both React providers and `OperationsProgress`; serialized writes, normalized wire encoding, blocked rendering during hydration, restored every progress page, and persisted `wizard-data.userName`. |
| 3 | API contracts and request order | Traced every operation through `adminApi`, `HostsApi`, `ServiceApi`, `ConfigsApi`, `RequestApi`, credentials, and `/persist`; corrected Federation aggregation, fail-closed install/config behavior, final topology reload, Ranger groups, and credential GET/PUT/POST order. |
| 4 | Modes, permissions, feature flags, stack/service branches | Checked add/delete/mixed/no-op, HA state, service/host/persistence permissions, `manageJournalNode`, Kerberos, Federation, and every dependency service; implemented static branches and retained stack/history combinations in the runtime matrix. |
| 5 | Error, Retry, refresh, Back, Cancel, and interrupted recovery | Fault-read polling, task checkpoints, install/deletion/config/KDC/persist failures, completion, and critical exit; failures now remain visible and recoverable without advancing or discarding owner state. |

No executable Classic evidence contradicted the current Ember baseline, so no baseline or generated evidence file was changed. The initial React gap document incorrectly reported the existing Host Details JournalNode routes and three-JN deletion gate as absent; this document now records their source-backed behavior.

## Verification Evidence

Focused Vitest coverage passes 47 tests in 13 files for request checkpoint ordering, terminal failure/Retry, fail-closed component registration, idempotent deletion, NNHA/JN assignment and Federation helpers, complete 4/5-JN barriers, secure config overlay, Review edit submission, Ranger groups, topology Retry, completion persistence, workflow owner/hydration, KDC cancellation, credential persistence ordering, and Manual Kerberos bypass. The full React suite passes 229 tests in 65 files; TypeScript, production build, baseline validation, and diff checks pass. Repository-wide lint remains a pre-existing migration gate with 5,811 errors and 452 warnings, while the new HA utility, credential utility, and new focused test files pass targeted ESLint.

Shared-file changes are limited to Module 09 contracts: `Utility` terminal statuses; `taskUtils` install/delete ordering; `OperationsProgress` checkpoints/poll recovery; `AssignMastersAddable` assignment bounds; Step Wizard hidden-step/navigation/cancel support; `useStepWizard`; `useConfigsTags`; route/service-action guards; KDC popup/session handling; and credential persistence ordering. No Module 05, Module 06, or other module gap document or implementation was changed.
