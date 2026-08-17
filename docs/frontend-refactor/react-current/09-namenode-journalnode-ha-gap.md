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

The React routes and pages exist, but the module is not functionally complete. The highest-risk confirmed defects are:

1. NNHA Step 6 enables Next before validation and does not aggregate all selected JournalNode responses.
2. The shared install helper swallows prerequisite failures and can report a failed install chain as successful.
3. JN Federation Review and checkpoint calls do not use the required namespace-aware contracts.
4. JN delete-only mode does not skip or renumber Steps 3 and 5, and no-op assignment is accepted.
5. JN progress steps do not restore saved operations, and Step 6 starts a cached topology without reloading the server.
6. Persistence writes are not serialized with navigation or cancellation; hydration failures can overwrite recoverable state.
7. NNHA Step 9 omits PXF, has incomplete Ranger groups, and omits `hdfs-client` from HAWQ reconfiguration.
8. Direct-route and menu permission policies do not jointly enforce the workflow's mutation and persisted-data requirements.

## State Machines

### Enable NameNode HA

| State | Forward condition and side effects | Back/exit and recovery | React result |
| --- | --- | --- | --- |
| Entry | `SERVICE.ENABLE_HA`, persisted-data capability, HDFS HA support, HA disabled, at least 3 hosts, at least 3 ZooKeeper Servers, started current NN, and no HDFS master maintenance | Invalid entry remains on HDFS Summary; a valid persisted owner resumes its saved step | React checks the route permission, started NN, ZooKeeper count, and maintenance. It misses host count, explicit persisted-data gating, support checks, and robust persisted-owner validation. |
| Step 1 Get Started | Valid 1-63 character Nameservice ID; save ID and clear later topology/config snapshots | No Back; Cancel clears only pre-change state | ID validation matches. HDFS user loading, HAWQ warning, restored input, and awaited persistence are missing. |
| Step 2 Select Hosts | Exactly one additional NN and at least 3 JNs; unique valid hosts; all selected JNs are retained; save rollback hosts | Back to Step 1 and invalidate Review/later state | Assignment UI and Advisor calls exist. Next is always enabled, and assignment validity is not surfaced to the parent. |
| Step 3 Review | Load exact desired tags/config versions; generate HA and dependency changes; only JN edits directory is editable; save immutable snapshot | Back to Step 2; load/generation error stays on Review | Main generation exists, but a self-triggering effect mutates shared descriptors, missing desired configs are hidden as an endless spinner, and repeated loads are unsafe. |
| Step 4 Checkpoint | Poll current NN; require Safemode and transaction delta <= 1; then pass KDC session gate | No Back in Classic; Cancel is pre-change; poll/parse errors are recoverable | Gate exists, but JSON parsing is unsafe, no visible poll error exists, and KDC transport failure is silent. |
| Step 5 Configure | Strictly stop all, install NN, install all JNs, save HDFS/Ranger, install HDFS clients, start all JNs, disable SNN | No Back after mutations; command Retry only; critical exit warning/owner retained | Task list exists, but shared install and config helpers swallow failures, secure reload differs, and React exposes Back/ordinary Cancel. |
| Step 6 Initialize JNs | Poll every selected JN as a complete set; all must report this nameservice formatted | No Back; critical exit behavior | React starts enabled, races duplicate polls, resets counters per response, and retains the Classic three-response defect. |
| Step 7 Start Components | Strictly start ZK, conditional Infra/MySQL/Ranger, then current NN | No Back; command Retry and critical exit | ZK/current NN exist. Infra detection is wrong, MySQL is always removed, and Back/Cancel remain available. |
| Step 8 Initialize Metadata | Manual `formatZK` and `bootstrapStandby`; KDC gate; explicit completion confirmation | No Back; critical exit/rollback boundary | Commands and KDC call exist. The required manual-completion confirmation is missing and Next is immediately enabled. |
| Step 9 Finalize | Strictly start new NN, install/start ZKFC, conditional PXF/dependency configs, delete SNN, stop HDFS, start all | Complete clears owner after persistence; Retry resumes first failure; no Back | Core list exists. PXF and HAWQ client config are missing, Ranger groups are incomplete, a Knox branch calls non-JavaScript `findProperty`, and Back/Cancel remain available. |

### Manage JournalNodes

| State | Forward condition and side effects | Back/exit and recovery | React result |
| --- | --- | --- | --- |
| Entry | Unified mutation/persistence policy; HA topology; Active and Standby NN labels; action cardinality permits add or delete | Invalid direct route is blocked; saved owner resumes | React route requires only `SERVICE.ENABLE_HA`; no persisted-data check, support/cardinality route gate, or Host Details permission reconciliation exists. |
| Step 1 Assign | Start with current JNs; final set >= 3; maximum `min(stack/hosts, existing*2-1)`; no-op disabled | No Back; Cancel clears pre-change state | React automatically adds a JN, uses only stack/host maximum, and always enables Next, including no-op. |
| Step 2 Review | Compute exact add/delete sets; non-Federation or every Federation shared-edits key; save deduplicated snapshot | Back to Step 1; delete-only goes to Step 4 | React computes sets, but only non-Federation descriptors are generated, `addedJournalNodes` stores the final set, and delete-only does not skip. |
| Step 3 Save Namespace | One command set per namespace; exact namespace-to-check-host response set must pass | Back to Review; omitted in delete-only | React renders only non-Federation commands, calls the single-host URL with comma-separated hosts, does not validate identity/cardinality, and does not skip delete-only. |
| Step 4 Add/Remove | Strictly stop standby NN, stop all, install adds, await all deletes, save HDFS, install clients | No Back; command Retry; critical exit warning/owner retained | Deletion now uses `Promise.all`, but install/config errors are swallowed, operations are not restored, and ordinary Cancel discards recovery. |
| Step 5 Copy Directories | Show deduplicated directories for one/all namespaces; manual completion | No Back; omitted in delete-only | Directory rendering exists; it is not skipped for delete-only and uses model fields that need runtime validation. |
| Step 6 Start JNs | Reload final server topology, then start exactly the final JN set | No Back; command Retry | React starts the Step 1 snapshot and never refreshes topology; saved operation status is ignored. |
| Step 7 Start All | Start all services and poll; clear owner only after durable completion | Finish returns to HDFS Summary | Start exists, but saved operation status is ignored and clear/navigation are fire-and-forget. |

## Feature ID Status

### Scope and Entry

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| NNHA-SCOPE-001 | `MATCH` | A modal nine-step route exists and stores step state. |
| NNHA-SCOPE-002 | `PARTIAL` | Seven JN pages exist, but delete-only remains seven visible steps. |
| NNHA-SCOPE-003 | `MATCH` | Product Metrics is absent while checkpoint/JN formatted fields are read. |
| NNHA-SCOPE-004 | `MATCH` | AMS is limited to `ams-hbase-site` migration/submission. |
| NNHA-SCOPE-005 | `MATCH` | This audit uses executable routes, operations, API helpers, and tests rather than inventory hits. |
| NNHA-ENTRY-001 | `PARTIAL` | The action uses `SERVICE.ENABLE_HA` and HA-disabled state, but stack support and all disable reasons are not represented. |
| NNHA-ENTRY-002 | `MATCH` | React exposes no Disable NNHA entry. |
| NNHA-ENTRY-003 | `MISSING` | The upgrade custom-check entry does not route into the React wizard. |
| NNHA-ENTRY-004 | `PARTIAL` | Started NN is checked, and missing NN fails gracefully; result depends on asynchronously loaded component data. |
| NNHA-ENTRY-005 | `MATCH` | React requires three ZooKeeper Server components and does not require STARTED state. |
| NNHA-ENTRY-006 | `MISSING` | React does not require at least three registered hosts. |
| NNHA-ENTRY-007 | `MATCH` | Explicit and implied master maintenance states block entry. |
| NNHA-ENTRY-008 | `PARTIAL` | Direct routes run some business validation, but omit host count, support, cardinality, and persistence capability. |

### NameNode HA Steps and Configuration

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| NNHA-STEP1-001 | `PARTIAL` | ID regex matches; HDFS user loading, restored ID, and topology clearing are incomplete. |
| NNHA-STEP1-002 | `MATCH` | HBase warning is informational. |
| NNHA-STEP1-003 | `MISSING` | No conditional HAWQ filespace warning is shown. |
| NNHA-STEP2-001 | `PARTIAL` | One NN and three JNs are seeded and Advisor is called; Next is not tied to readiness/validity. |
| NNHA-STEP2-002 | `PARTIAL` | Select options prevent common duplicates, but maintenance/invalid assignment is not a blocking parent state. |
| NNHA-STEP2-003 | `MATCH` | JN Add uses stack cardinality and host count rather than a hard maximum of three. |
| NNHA-STEP2-004 | `PARTIAL` | Back and downstream key removal exist; rollback host keys and durable ordering do not. |
| NNHA-STEP3-001 | `PARTIAL` | Review hosts and config spinner exist; failed load has no visible terminal error. |
| NNHA-STEP3-002 | `MATCH` | Only `dfs.journalnode.edits.dir` is editable. |
| NNHA-STEP3-003 | `PARTIAL` | Dependency sites are requested, but missing-site handling becomes a silent spinner and descriptors are mutated globally. |
| NNHA-STEP3-004 | `PARTIAL` | Snapshot is stored, but save/navigation is not serialized and tag rollback metadata is incomplete. |
| NNHA-STEP4-001 | `MATCH` | Commands and repeated checkpoint GET are present. |
| NNHA-STEP4-002 | `MATCH` | Safemode and transaction delta form the Next gate. |
| NNHA-STEP4-003 | `PARTIAL` | Desired state is neither displayed nor included in Next. |
| NNHA-STEP4-004 | `PARTIAL` | KDC gate exists; ordinary KDC transport failures have no visible recoverable state. |
| NNHA-STEP5-001 | `MATCH` | Stop-all request is first and request polling is used. |
| NNHA-STEP5-002 | `DIFFERENT` | Install helper can stop before registration or swallow failures and report success. |
| NNHA-STEP5-003 | `DIFFERENT` | All selected hosts are passed, but the same defective install chain applies. |
| NNHA-STEP5-004 | `PARTIAL` | HDFS/Ranger save and HDFS client install exist; save errors are converted to success. |
| NNHA-STEP5-005 | `PARTIAL` | React overlays its existing snapshot but does not reload current tags/configs before the secure save. |
| NNHA-STEP5-006 | `MATCH` | All selected JNs are sent to a STARTED host-component update. |
| NNHA-STEP5-007 | `MATCH` | SNN maintenance is set to ON without deleting it. |
| NNHA-STEP6-001 | `PARTIAL` | Manual command and concurrent GETs exist; polling starts twice. |
| NNHA-STEP6-002 | `DIFFERENT` | JSON parse is unsafe and Next starts enabled, bypassing formatted status. |
| NNHA-STEP6-003 | `DIFFERENT` | React copied the three-response behavior and also resets the formatted counter for each response. |
| NNHA-STEP7-001 | `MATCH` | ZooKeeper Server hosts are started first. |
| NNHA-STEP7-002 | `DIFFERENT` | It checks service name `AMBARI_INFRA_SOLR` rather than the loaded component model. |
| NNHA-STEP7-003 | `MISSING` | MySQL Server is unconditionally removed from the task list. |
| NNHA-STEP7-004 | `PARTIAL` | Ranger is conditional, but host extraction/model assumptions need correction. |
| NNHA-STEP7-005 | `MATCH` | Only the installed/original NN is started last. |
| NNHA-STEP8-001 | `MATCH` | `hdfs zkfc -formatZK` is shown without server validation. |
| NNHA-STEP8-002 | `MATCH` | `hdfs namenode -bootstrapStandby` is shown for the additional NN. |
| NNHA-STEP8-003 | `PARTIAL` | KDC is checked, but no manual-completion confirmation exists. |
| NNHA-STEP9-001 | `MATCH` | The additional NN is started and polled. |
| NNHA-STEP9-002 | `PARTIAL` | ZKFC install/start tasks exist but inherit install-chain error swallowing. |
| NNHA-STEP9-003 | `MISSING` | Historical PXF installation is absent. |
| NNHA-STEP9-004 | `DIFFERENT` | Only Ranger env/YARN/Hive/KMS are submitted by the callback; other prepared groups are discarded, and Knox uses `findProperty`. |
| NNHA-STEP9-005 | `MATCH` | HBase and present Ranger HBase sites are saved together. |
| NNHA-STEP9-006 | `MATCH` | Complete `ams-hbase-site` is submitted whenever AMS is installed. |
| NNHA-STEP9-007 | `MATCH` | `accumulo-site` is conditionally submitted. |
| NNHA-STEP9-008 | `PARTIAL` | `hawq-site` is saved, but `hdfs-client` is omitted. |
| NNHA-STEP9-009 | `MATCH` | SNN host-component is deleted directly. |
| NNHA-STEP9-010 | `MATCH` | HDFS is stopped, then all services are started without smoke tests. |
| NNHA-STEP9-011 | `PARTIAL` | Completion clears state and returns; HAWQ warning and awaited durable clear are missing. |
| NNHA-CONFIG-001 | `MATCH` | Both nameservice properties use the entered ID. |
| NNHA-CONFIG-002 | `MATCH` | `nn1,nn2` and configured failover provider are generated. |
| NNHA-CONFIG-003 | `MATCH` | Current RPC port and additional default 8020 are generated. |
| NNHA-CONFIG-004 | `MATCH` | Current HTTP/HTTPS ports and additional defaults are generated. |
| NNHA-CONFIG-005 | `MATCH` | All selected JNs are joined with `;` at port 8485. |
| NNHA-CONFIG-006 | `MATCH` | `fs.defaultFS` and comma-separated ZooKeeper quorum are generated. |
| NNHA-CONFIG-007 | `MATCH` | Fencing, automatic failover, and safemode values come from the descriptor. |
| NNHA-CONFIG-008 | `PARTIAL` | Old keys are removed during Review, but secure reload is not actually performed. |
| NNHA-CONFIG-009 | `PARTIAL` | Default/editability match; the Windows current-value branch is absent. |
| NNHA-CONFIG-010 | `MATCH` | HBase authority replacement matches Classic. |
| NNHA-CONFIG-011 | `MATCH` | Accumulo volume/replacements are generated. |
| NNHA-CONFIG-012 | `MATCH` | AMS changes only on current-NN authority match and is always submitted when installed. |
| NNHA-CONFIG-013 | `PARTIAL` | HAWQ URL migration exists; completion warning is absent. |
| NNHA-CONFIG-014 | `PARTIAL` | `hdfs-client` values are generated but never submitted in Step 9. |
| NNHA-CONFIG-015 | `PARTIAL` | HDFS/HBase and some Ranger sites are saved; Step 9 omits several installed service groups. |
| NNHA-CONFIG-016 | `PARTIAL` | Single-group shape matches; the intended Ranger multi-group body is incomplete. |

### JournalNode Entry, Modes, and Steps

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| JN-ENTRY-001 | `PARTIAL` | Action requires HA, but support and exact `(hosts > JNs || JNs > 3)` cardinality are not implemented. |
| JN-ENTRY-002 | `DIFFERENT` | React requires `SERVICE.ENABLE_HA`; Classic can generate the action under four other service permissions. |
| JN-ENTRY-003 | `MATCH` | Active and Standby HA labels are required; work state is not checked. |
| JN-ENTRY-004 | `MISSING` | Host Details add/delete entry and its `HOST.ADD_DELETE_COMPONENTS`/KDC branches are not wired to this React wizard. |
| JN-ENTRY-005 | `PARTIAL` | Assignment retains minimum three, but no equivalent Host Details delete disablement exists. |
| JN-MODE-001 | `PARTIAL` | Add-only side effects exist; assignment starts with an unsolicited added JN. |
| JN-MODE-002 | `DIFFERENT` | Delete-only does not skip or renumber Steps 3 and 5. |
| JN-MODE-003 | `MATCH` | Mixed mode can reach all seven steps. |
| JN-MODE-004 | `DIFFERENT` | No-op Next is always enabled. |
| JN-STEP1-001 | `PARTIAL` | Current assignments and add/remove controls exist; readiness and maintenance validation do not gate Next. |
| JN-STEP1-002 | `DIFFERENT` | Maximum is stack/host cardinality, not `existingCount * 2 - 1`. |
| JN-STEP1-003 | `PARTIAL` | Final topology is stored and diffed later; Host Details is absent and persistence is not ordered. |
| JN-STEP2-001 | `MATCH` | Add/delete Review and read-only shared-edits config are shown after load. |
| JN-STEP2-002 | `MATCH` | Non-Federation shared-edits is generated from the final JN set. |
| JN-STEP2-003 | `MISSING` | Federation descriptors are explicitly filtered out. |
| JN-STEP2-004 | `DIFFERENT` | Descriptor arrays are mutated; delete-only does not jump; added host data is not the delta. |
| JN-STEP3-001 | `MATCH` | Single-namespace commands and checkpoint gate exist. |
| JN-STEP3-002 | `MISSING` | Namespace-specific `-fs hdfs://<ns>` commands are not rendered. |
| JN-STEP3-003 | `MATCH` | React derives active hosts and falls back to started/first hosts by namespace. |
| JN-STEP3-004 | `PARTIAL` | Every returned item is checked and polling repeats; desired-state errors are not visible. |
| JN-STEP3-005 | `DIFFERENT` | Response identity/count is not checked, and the multi-host call uses the wrong endpoint. |
| JN-STEP4-001 | `MATCH` | Saved Standby NN is stopped first. |
| JN-STEP4-002 | `MATCH` | All services are stopped second. |
| JN-STEP4-003 | `PARTIAL` | Add set is installed conditionally but inherits install-chain defects. |
| JN-STEP4-004 | `PARTIAL` | Deletes use `Promise.all`; explicit NoSuchResource idempotency is absent. |
| JN-STEP4-005 | `MATCH` | React waits for every DELETE promise before reconfiguration. |
| JN-STEP4-006 | `PARTIAL` | HDFS is saved and clients are installed, but save/install failures can be treated as success. |
| JN-STEP5-001 | `PARTIAL` | Directories are read/deduplicated; namespace grouping field assumptions need runtime verification. |
| JN-STEP5-002 | `MATCH` | Next is a manual acknowledgment and performs no validation. |
| JN-STEP6-001 | `DIFFERENT` | React uses the pre-change wizard snapshot, not a final server topology reload. |
| JN-STEP7-001 | `PARTIAL` | Start-all and return exist; refresh recovery and durable clear are incomplete. |

### Progress, Recovery, Rollback, and Risks

| Feature ID | Status | Classic versus current React |
| --- | --- | --- |
| NNHA-PROGRESS-001 | `PARTIAL` | Operations are serial in-memory, but dispatch persistence is not awaited before the next task. |
| NNHA-PROGRESS-002 | `PARTIAL` | Requests poll every 2 seconds; poll failures are uncaught and stop progress. |
| NNHA-PROGRESS-003 | `DIFFERENT` | Request status is trusted directly; `TIMEDOUT` is not terminal and `ABORTED` is not treated as failed. |
| NNHA-PROGRESS-004 | `PARTIAL` | Retry exists, but its started-task filtering is defective and JN tasks do not restore saved state. |
| NNHA-PROGRESS-005 | `MATCH` | No normal task exposes Skip. |
| NNHA-PROGRESS-006 | `MATCH` | Request-bearing operations open Background Operations and task logs. |
| NNHA-PROGRESS-007 | `PARTIAL` | KDC cancellation rejects install; ordinary KDC transport errors can leave the promise unresolved. |
| NNHA-PROGRESS-008 | `DIFFERENT` | Registration and outer install errors are swallowed; Install may run after registration failure. |
| NNHA-PROGRESS-009 | `PARTIAL` | Existence/topology GET exists; no guaranteed final topology refresh exists. |
| NNHA-PROGRESS-010 | `PARTIAL` | Correct endpoints exist, but the service-component existence decision can return before register/install. |
| NNHA-PROGRESS-011 | `PARTIAL` | Shared KDC popup/replay exists; credential save failure semantics require runtime verification. |
| NNHA-RECOVERY-001 | `PARTIAL` | Step/task data and active step are posted, but wire values are doubly stringified and writes race. |
| NNHA-RECOVERY-002 | `PARTIAL` | Active step and NNHA operation states can restore; hydration/poll errors can corrupt or stall recovery. |
| NNHA-RECOVERY-003 | `MATCH` | Early Cancel clears persisted state and returns to HDFS Summary. |
| NNHA-RECOVERY-004 | `MISSING` | No critical-phase/manual rollback warning is shown. |
| NNHA-RECOVERY-005 | `NOT_APPLICABLE` | The Classic automatic rollback target is placeholder-only and is not a parity target. |
| JN-RECOVERY-001 | `PARTIAL` | Step/task data is posted, but progress steps do not consume saved operations and writes race. |
| JN-RECOVERY-002 | `DIFFERENT` | React also discards critical state on Cancel, without warning or completed-side-effect evidence. |
| JN-RECOVERY-003 | `PARTIAL` | React defines a JN cluster status and persisted owner, but cross-refresh/user behavior is unverified. |
| NNHA-RECOVERY-006 | `MISSING` | Entry does not require `CLUSTER.MANAGE_USER_PERSISTED_DATA`; post failures are suppressed. |
| NNHA-ROLLBACK-001 | `NOT_APPLICABLE` | React correctly does not claim parity with the three-step placeholder. |
| NNHA-ROLLBACK-002 | `NOT_APPLICABLE` | Placeholder checkpoint wiring is not a migration target. |
| NNHA-ROLLBACK-003 | `NOT_APPLICABLE` | Empty completion page is not a migration target. |
| NNHA-ROLLBACK-004 | `NOT_APPLICABLE` | Hidden, incorrect Disable navigation is not implemented. |
| NNHA-ROLLBACK-005 | `NOT_APPLICABLE` | Unwired static reverse controller is not reused. |
| NNHA-ROLLBACK-006 | `NOT_APPLICABLE` | Invalid rollback route/state is not reused. |
| NNHA-ROLLBACK-007 | `NOT_APPLICABLE` | Defective rollback interfaces/types are not reused. |
| NNHA-ROLLBACK-008 | `MATCH` | React exposes no Disable or automatic rollback claim. |
| JN-RISK-001 | `DIFFERENT` | Checkpoint/formatted JSON parse and polling errors are not visible or recoverable. |
| JN-RISK-002 | `DIFFERENT` | Complete selected-host barrier is absent. |
| JN-RISK-003 | `MATCH` | All JN deletion promises are awaited. |
| JN-RISK-004 | `DIFFERENT` | Final topology reload is absent. |
| JN-RISK-005 | `DIFFERENT` | Federation identity/cardinality validation is absent. |
| JN-RISK-006 | `DIFFERENT` | Shared descriptor arrays are mutated across loads. |
| JN-RISK-007 | `DIFFERENT` | Component prerequisite failures are swallowed. |
| JN-RISK-008 | `PARTIAL` | Service route validates HA labels, but entry permissions/cardinality and Host Details are not unified. |
| JN-RISK-009 | `MISSING` | Critical-exit side-effect evidence and warning are absent. |
| JN-RISK-010 | `MATCH` | React does not reuse or advertise the defective rollback controller. |

### API Contract

| Feature ID | Status | React URL, method, query/payload, and order |
| --- | --- | --- |
| HA-API-001 | `MATCH` | `GET /clusters/{cluster}/hosts?...host_components...`; host-component hooks load topology. |
| HA-API-002 | `MATCH` | `GET /clusters/{cluster}/hosts?fields=Hosts/cpu_count,...&minimal_response=true`; assignment host details. |
| HA-API-003 | `MATCH` | Two `POST {stackVersionUrl}/recommendations` requests with `recommend:host_groups`, hosts, services, blueprint/binding. |
| HA-API-004 | `MATCH` | `GET /clusters/{cluster}?fields=Clusters/desired_configs`; config hook first. |
| HA-API-005 | `MATCH` | `GET /clusters/{cluster}/configurations?...`; exact desired tag combinations follow. |
| HA-API-006 | `MATCH` | `GET /clusters/{cluster}/hosts/{host}/host_components/NAMENODE`; NNHA and single-namespace JN checkpoint. |
| HA-API-007 | `DIFFERENT` | Helper exists, but Federation Step 3 calls HA-API-006 with a comma-separated host path. |
| HA-API-008 | `MATCH` | One `GET .../hosts/{host}/host_components/JOURNALNODE?fields=metrics` per selected JN. |
| HA-API-009 | `MATCH` | `PUT /clusters/{cluster}/services?...` with RequestInfo/Body for stop/start operations. |
| HA-API-010 | `MATCH` | `PUT /clusters/{cluster}/host_components` with query in RequestInfo and `Body.HostRoles.state`. |
| HA-API-011 | `MATCH` | Single-component state helper is available; normal flows mainly use the batch endpoint. |
| HA-API-012 | `MATCH` | `PUT .../hosts/{host}/host_components/SECONDARY_NAMENODE` with maintenance state ON. |
| HA-API-013 | `MATCH` | Direct DELETE is used for SNN and each removed JN. |
| HA-API-014 | `MATCH` | `PUT /clusters/{cluster}` with `desired_config` groups is used. |
| HA-API-015 | `PARTIAL` | Multi-body Ranger PUT exists, but React assembles only a subset during the callback. |
| HA-API-016 | `MATCH` | Installed-host GET precedes component registration. |
| HA-API-017 | `PARTIAL` | Service-component POST exists, but model detection can return before later chain steps. |
| HA-API-018 | `PARTIAL` | Host registration POST exists; its error is swallowed and Install still runs. |
| HA-API-019 | `PARTIAL` | Component topology GET is used on Review, not reliably before every create decision. |
| HA-API-020 | `PARTIAL` | Service model loading supplies operational topology, but no explicit final JN refresh is ordered. |
| HA-API-021 | `PARTIAL` | `GET /requests/{id}?fields=*,tasks/...` polls at 2 seconds; transport failures stop polling. |
| HA-API-022 | `MATCH` | Background Operations supports request/stage detail polling. |
| HA-API-023 | `MATCH` | Background Operations retrieves `/requests/{id}/tasks/{taskId}` logs. |
| HA-API-024 | `MATCH` | Current Kerberos config versions are read when security is enabled. |
| HA-API-025 | `PARTIAL` | KDC session GET is used; transport failure without an error callback is silent. |
| HA-API-026 | `PARTIAL` | Shared credential popup uses the credential APIs; failure/replay ordering remains runtime-only evidence. |
| HA-API-027 | `PARTIAL` | React GETs workflow-specific `/persist/{key}` rather than `CLUSTER_CURRENT_STATUS`; parse/error handling is weak. |
| HA-API-028 | `PARTIAL` | `POST /persist` is used, but NNHA and JN encode values differently and navigation does not await it. |
| HA-API-029 | `NOT_APPLICABLE` | Registered rollback host loading is outside the normal React workflows. |
| HA-API-030 | `NOT_APPLICABLE` | Static unwired rollback interfaces are not migration targets. |
| HA-API-031 | `PARTIAL` | Config hooks load current sites, but Step 1 does not explicitly load/use `hadoop-env/hdfs_user`. |

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

| Surface | Required React policy | Current gap |
| --- | --- | --- |
| NNHA menu and direct route | `SERVICE.ENABLE_HA` and `CLUSTER.MANAGE_USER_PERSISTED_DATA`; HDFS `HA_MODE`; HA disabled | Persisted-data and stack-support checks are absent. |
| NNHA business gate | At least 3 registered hosts, 3 ZooKeeper Servers, a started NN, no HDFS master explicit/implied maintenance | Host count is absent. |
| Manage JN service action | Preserve Classic visibility for `RUN_CUSTOM_COMMAND`, `RUN_SERVICE_CHECK`, `TOGGLE_MAINTENANCE`, or `ENABLE_HA`, then apply a documented route/mutation policy | React requires only `SERVICE.ENABLE_HA` in both menu and route. |
| Manage JN Host Details | `HOST.ADD_DELETE_COMPONENTS`, HA/topology/cardinality gate, KDC check for add | Entry is missing. |
| Manage JN route | Persisted-data capability plus a mutation permission accepted by the unified policy; HA enabled; Active/Standby; supported stack; add/delete cardinality | Only `SERVICE.ENABLE_HA` and HA labels are checked. |
| Service conditions | Ranger, HBase, AMS, Accumulo, HAWQ/PXF, Infra, MySQL branches only when installed/component-count conditions match | Several Step 7/9 conditions are absent or use the wrong service/component identity. |
| Kerberos | Non-secure, automatic KDC, and Manual Kerberos; cancellation/error must terminate the current transition without losing owner state | Main hooks exist; transport and popup cancellation paths are incomplete. |
| Auto rollback | Do not advertise automatic rollback or Disable NNHA parity | React correctly exposes neither. |

## Test Feature IDs

| Feature ID | Status | React evidence or required coverage |
| --- | --- | --- |
| HA-TEST-001 | `MISSING` | No focused React tests cover NNHA Steps 1-9. |
| HA-TEST-002 | `MISSING` | No focused React tests cover JN Steps 1-7. |
| HA-TEST-003 | `PARTIAL` | Shared background-operation tests exist elsewhere; HA task recovery/Retry/poll failure is uncovered. |
| HA-TEST-004 | `MISSING` | React HA config initializer has no focused parity tests. |
| HA-TEST-005 | `MISSING` | No direct-route permission/business-gate matrix exists. |
| HA-TEST-006 | `MISSING` | No owner/exit/cross-refresh tests exist. |
| HA-TEST-007 | `NOT_APPLICABLE` | Placeholder/unwired Classic rollback is not implemented; absence must remain explicit. |
| HA-TEST-008 | `MISSING` | No complete-set/out-of-order 4/5 JN test exists. |
| HA-TEST-009 | `MISSING` | No concurrent deletion failure or final-topology reload test exists. |
| HA-TEST-010 | `MISSING` | No Federation missing/duplicate namespace response test exists. |
| HA-TEST-011 | `MISSING` | No automatic/Manual Kerberos failure-replay matrix exists. |
| HA-TEST-012 | `MISSING` | Full real-cluster success/recovery matrix remains required. |

## Executable Acceptance Criteria

1. Direct NNHA entry is rejected before opening Step 1 for missing authorization, missing persisted-data capability, unsupported stack/service, HA already enabled, fewer than three hosts/ZooKeeper Servers, missing or stopped NN, or any HDFS master maintenance state.
2. NNHA assignment cannot advance until exactly one additional NN and at least three unique valid JNs are selected; 4/5 JNs remain in the generated shared-edits URI and formatted-status barrier.
3. Review loads immutable fresh descriptors on every visit, submits exact current config snapshots, exposes load/generation errors with Retry, and never duplicates or permanently renames descriptor entries.
4. Checkpoint JSON parse and GET failures display a recoverable error and continue controlled polling after Retry; Next never enables from a missing, malformed, partial, or wrong-host response.
5. Component installation stops on service-component create, host registration, KDC, or Install failure. An explicit ResourceAlreadyExists response may continue idempotently; other errors retain the failed task and original server message.
6. Progress executes one operation at a time, treats `FAILED`, `TIMEDOUT`, and `ABORTED` as failure, persists request IDs/status before advancing, restores polling after refresh, and retries only the failed operation.
7. NNHA Step 6 waits for every selected JN, associates each response with its host, and enables Next only when every host reports the current nameservice formatted.
8. NNHA Step 8 requires explicit confirmation after a successful KDC gate. Steps 5-9 do not offer unsafe Back navigation, and critical Cancel preserves completed-side-effect evidence with a manual recovery warning.
9. NNHA Step 9 executes every applicable PXF/Ranger/HBase/AMS/Accumulo/HAWQ branch, includes `hdfs-client` for HAWQ, then deletes SNN, stops HDFS, and starts all services in order.
10. Manage JN begins with the live JN set, enforces minimum/maximum/no-op rules, supports add-only/delete-only/mixed, and displays five renumbered steps for delete-only.
11. Federation Review writes exactly one shared-edits property per nameservice. Checkpoint renders one command set per nameservice and requires a one-to-one expected namespace/host response set.
12. JN deletion awaits all hosts and reports failures by host. Retry targets only failed deletions. Reconfigure cannot start while any deletion is unresolved.
13. Before JN Step 6, React reloads server topology and starts exactly the final JN set, excluding deleted hosts and including every newly registered host.
14. Workflow state hydration, step transitions, task checkpoints, completion, and Cancel are serialized. A failed `/persist` request blocks further mutation/navigation and displays a retryable error.
15. Focused tests cover validation helpers, complete-set aggregation, Federation cardinality, install prerequisite failure, deletion aggregation, topology refresh, task recovery, and persistence ordering.

## Runtime Acceptance Matrix

| Scenario | Topology/mode | Fault or transition points | Required evidence |
| --- | --- | --- | --- |
| NNHA non-secure | 3 hosts, 3 JNs, HDFS/ZK only | Refresh before/after every Step 5/7/9 mutation | Browser route/owner snapshot, ordered HTTP capture, Ambari request/task terminal states, final HDFS config version, Active/Standby/ZKFC topology. |
| NNHA 5 JNs | 5+ hosts, out-of-order formatted responses | Delay the fourth/fifth JN and return malformed JSON once | Next remains disabled until all five valid responses; visible recovery; shared-edits includes all five. |
| NNHA automatic Kerberos | MIT, AD, and IPA where supported | Expired KDC session, popup cancel, bad credential save, retry | No mutation after cancel/failure; persisted owner survives; successful replay executes once. |
| NNHA Manual Kerberos | `kdc_type=none` | Refresh at install and metadata gates | No KDC admin credential required; component identities/keytabs install; progress resumes. |
| NNHA dependencies | Ranger, HBase, AMS, Accumulo, historical HAWQ/PXF combinations | Missing optional site/property and config-save failure | Only applicable sites submitted, complete snapshots retained, failure stops sequence, HAWQ warning shown. |
| NNHA critical exit | Steps 5, 6, 7, 8, and 9 | Close modal after each completed command | Completed-side-effect list retained; no false rollback claim; manual recovery is actionable. |
| JN add-only | 3 to 4/5 JNs | Refresh after registration and before start | Seven steps; exact add set; final reload starts all final JNs. |
| JN delete-only | 5 to 3/4 JNs | One DELETE delayed and one NoSuchResource | Five renumbered steps; no checkpoint/copy; all deletions terminal before config save. |
| JN mixed | Replace one or more JNs | One delete fails, retry after other deletes succeed | Retry only failed host; no duplicate install/delete; final config/topology exact. |
| JN no-op/cardinality | 3, 4, and 5 existing JNs with varying host count | Return to Step 1 after Review | No-op disabled; minimum three and `existing*2-1` maximum enforced; snapshots invalidated correctly. |
| JN Federation | 2+ nameservices | Missing, duplicate, wrong-host, and non-STARTED checkpoint items | Exact namespace properties and commands; no partial-response pass. |
| Persistence ownership | Both workflows | Server restart, browser refresh, second user login, permission revoked | Authorized owner resumes; unauthorized user cannot mutate/overwrite; persist failure blocks progression. |
| Request failures | Both workflows | `FAILED`, `TIMEDOUT`, `ABORTED`, poll 500, and transient network loss | Visible terminal status, controlled polling recovery, correct Retry target, no next-command execution. |

## Five-Pass Audit Record

| Pass | Surface | Result |
| --- | --- | --- |
| 1 | Routes, pages, buttons, and wizard navigation | Confirmed both route surfaces and all pages; found incorrect Manage JN authorization, absent Host Details/upgrade entries, unsafe Back states, and missing delete-only skip/renumber. |
| 2 | Controller/service/model state and recovery | Compared Classic local DB/task/request checkpoints with both React providers and `OperationsProgress`; found unawaited writes, inconsistent wire encoding, weak hydration, and missing JN task restore. |
| 3 | API contracts and request order | Traced every operation into `adminApi`, `HostsApi`, `ServiceApi`, `ConfigsApi`, `RequestApi`, and `/persist`; found wrong Federation checkpoint call, swallowed install/config errors, and incomplete Step 9 groups. |
| 4 | Modes, permissions, feature flags, stack/service branches | Checked add/delete/mixed/no-op, HA state, authorization, persistence, Kerberos, Federation, and dependency-service branches; found route/menu policy and conditional-task gaps. |
| 5 | Error, Retry, refresh, Back, Cancel, and interrupted recovery | Fault-read polling, progress, install, deletion, config save, KDC, and persistence paths; found incomplete terminal aggregation, unsafe critical exit, and several refresh replay risks. |

No executable Classic evidence contradicted the current Ember baseline during this audit, so no baseline or generated evidence file was changed. Common-file changes, if required by implementation, must be limited to the generic task/progress behavior that Module 09 directly exercises and must be called out in the pull request.
