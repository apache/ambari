<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Multi-cluster source review and remediation

Review date: 2026-09-10. Baseline: `a62fe4959dc948210d844295a097a3311321dea6`.
Reviewed HEAD: `b7c19ab3efc567f11d961fee0613c6f45dfe4ba9`, branch
`multicluster-pr4208`, also the local tracking reference
`origin/AMBARI-26654-multicluster-final` at review time. No remote fetch is implied.
Sections 1–4 preserve the original findings and source locations at that immutable
revision. Sections 5–6 and 8 describe the current remediation and remaining gates.
The three-pass completion audit below records subsequent corrections against the
original checklist; historical defects are not claims that those fixes are absent.

The initial remediation batch passed 205 Java, 91 frontend and 31 Python tests.
Subsequent packaging corrections made full TypeScript checking and the native RPM
build pass. The runtime follow-up at the end of this document is the current
acceptance record: two real three-host Hadoop clusters, distinct-user REST/STOMP
isolation and actual browser route rejection have passed. Cross-cluster HBase
installation is still under repair; provider lifecycle and KDC acceptance remain
open. Runtime results use the recorded RPM plus explicit incremental overlays.

## 1. Overall conclusion

There are substantial isolation and managed-dependency foundations, but this is
not yet an accepted implementation of one console safely managing independent
clusters. Secure credential completion and installation recovery lack durable
production lineage. Retry, dispatch recovery and lifecycle UI are incomplete.
Agent task reports can reach side effects before ownership validation. That
last defect predates the feature baseline but blocks the claimed boundary.
No ordinary REST-user cross-cluster P0 exploit was established.

- **Source established:** host exclusivity, actual-route authorization, several
  task/stream authorization paths, durable binding identities, lifecycle guards,
  consumer client overlays and Python verification implementations.
- **Focused tests only:** protocol parsing and planner/dispatcher/result transitions
  against mocked collaborators, and selected React scope boundaries.
- **Historical evidence or mock assertions only:** earlier aggregate pass/build
  statements, frontend recovery using invented independent request IDs, and
  credential readiness without a production completion caller.
- **Not verified:** two live clusters with distinct users, KDC/keytab execution,
  real HDFS/ZooKeeper, cross-process crash recovery, browser/broker confidentiality,
  and upgrades on all database dialects.

This review followed critical production chains and test implementations; it does
not certify every line of the 536-file feature diff. Contracts describe required
behavior and cannot substitute for executable callers or persisted state.

Initial commands and results:

```text
git status --short
  (empty)
git branch --show-current
  multicluster-pr4208
git log --oneline --decorate -n 10
  b7c19ab3ef (HEAD -> multicluster-pr4208, origin/AMBARI-26654-multicluster-final) AMBARI-26654: Align dependency command fixtures
  e09314fc2d AMBARI-26654: Harden managed dependency protocol validation
  ff9700d6cc AMBARI-26654: Add unified multi-cluster management foundation
  a62fe4959d (apache/pr-4208) AMBARI-26653: Fix BIGTOP runtime deployment workflows
  9823ae02ed AMBARI-26653: Declare STOMP session headers explicitly
  b2e84da613 AMBARI-26653: Correct Jetty 12 JNDI coordinates
  76ad9f0aab AMBARI-26653: Integrate JAXB runtime alignment from PR 4207
  d8970d92e3 AMBARI-26653: Integrate agent assembly metadata from PR 4207
  4e2617a06b AMBARI-26653: Integrate Views assembly fix from PR 4207
  ad88859fdb AMBARI-26653: Remove unused Jetty runtime from agent
git diff --stat a62fe4959dc948210d844295a097a3311321dea6..HEAD
  536 files changed, 75179 insertions(+), 3656 deletions(-)
git diff --check a62fe4959dc948210d844295a097a3311321dea6..HEAD
  (empty; exit 0)
```

## 2. Established capabilities and limits

Unless otherwise specified, Java paths are relative to
`ambari-server/src/main/java/org/apache/ambari/server/`; React paths are relative
to `ambari-web/latest/src/`. Line numbers identify the reviewed HEAD.

| Capability | Source | Proof chain | Remaining limit |
| --- | --- | --- | --- |
| Exclusive host ownership | `state/cluster/ClustersImpl.java:724,792,810`; `upgrade/HostMembershipSchemaUpgrade.java:56,69`; PostgreSQL DDL unique host constraint | Sorted host locks and prevalidation precede persistence; startup/migration rejects ambiguous owners | Concurrent database writes/reload and every dialect not executed here |
| Task read ownership | `controller/internal/TaskResourceProvider.java:194,331` | Task/stage/request resolve actual owner before authorization and response | Full two-user REST matrix not executed |
| Server stream authorization | `api/stomp/ApiStompAuthorizationService.java:86,107`; outbound interceptor and session registry | Request/task DAO ownership, per-recipient projection and current session authority | Real subscriptions, revocation and reconnect remain open |
| Durable binding scope | `orm/entities/ServiceDependencyBindingEntity.java:50,75,79,85,109,112`; coordinator `:570,1394,1422` | Numeric identities, immutable snapshots, epochs and operations; distinct consumer/provider authorization before publication | Does not prove installation or credential task association |
| Lifecycle enforcement | `ManagedDependencyLifecyclePolicy.java:113,389`; `ServiceResourceProvider.java:1033`; coordinator `:1840` | Impact revision confirmation and guarded deletion; Start requires all current bindings ready | UI confirmation, retry and detach incomplete |
| HBase client overlay/checks | runtime planner `:169`; `ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/HBASE/package/scripts/managed_hbase_dependency.py:729,1180` | Consumer command decoration, host/package/config checks, nonempty HDFS write/read verification | Real HDFS/ZooKeeper execution not run |
| Secure descriptor isolation | `controller/KerberosHelperImpl.java:533`; `controller/dependencies/security/ManagedHBaseKerberosDescriptorOverlay.java:94,105` | Copy consumer descriptor and exclude provider administrative identities | No exact credential-completion chain |
| React cluster scope | `hooks/useAuth.ts:28`; `AppLoader.tsx:126,146`; `screens/Directories/ServiceDirectory.tsx:56,76` | Actual route drives authorization and keyed provider lifetime; directory joins cluster/service and rejects stale generations | Broader fixture failures and browser/network gate remain |

## 3. Severe findings

Findings R1–R10 below describe the reviewed revision. The implementation ledger and review addendum record their remediation and additional verified defects. P1 denotes a blocker for the
requested feature, not exploitability by every ordinary cluster viewer. No P0
was confirmed. Dependency classes below are in `controller/dependencies/` unless
another path is given.

### R1 — P1: agent ownership validation follows side effects

**Trigger and impact:** an authenticated agent reports another host's task or
mislabels its own task's cluster/role. Task persistence, action events and keytab
observations can be affected. A real ActionManager call with mocked persistence
forwarded a foreign-host completion to the DAO.

**Root and source:** `agent/stomp/AgentReportsController.java:117` uses the session
host but flattens supplied reports. `agent/HeartbeatProcessor.java:338,361,389,552`
applies reports before the managed processor's later check.
`actionmanager/ActionManager.java:135,169` ignores its hostname argument.
`agent/HeartBeatHandler.java:237` also sends raw reports to host-state processing.
This is a missing trust boundary, not a frontend filtering problem.

**Complete repair:** ActionManager owns validation against persisted task,
stage/request cluster and execution-command identity. Filter before any event,
credential, component, host-state or task write, including direct task-response
callers. Reject invalid siblings independently; preserve legitimate global tasks.

**Tests and compatibility:** wrong host/cluster/request/role/custom command,
unknown/malformed reports, mixed batches, normal/global tasks, no rejected-report
side effects, restart-loaded commands. Existing abbreviated test reports must be
made wire-realistic; preserve actual agent report formats.

### R2 — P1: credential completion lacks a production operation lineage

**Trigger and impact:** the production coordinator constructs
`ManagedDependencySnapshotValidator(false)` at line 103, rejecting all new
same-realm secure approvals. Even if that gate is enabled, secure PREP can reach
`CONSUMER_CREDENTIALS_REQUIRED` without a durable completion path. Reusing
historical keytab tasks could falsely mark ready. The hardcoded gate was missed
in the first review pass and confirmed during the producer-to-coordinator test;
changing that boolean alone would not repair this feature.

**Root and source:** `ManagedDependencyTaskResultProcessor.java:97` defines
`dispatchVerificationAfterCredentials` without a production caller; `:404` persists
the waiting phase. Checks cover task status/role/host/cluster, but no persisted
current-operation principal/keytab plan. Heartbeat updates by host/path.
`Step9.tsx:335,839` launches separate global keytab regeneration. Responsibilities
are split across unrelated workflows.

**Complete repair:** producer transaction persists exact credential tasks,
principals, keytab fingerprint, binding/operation/epoch. A terminal observer uses
those records to publish VERIFY atomically and recover after restart. Represent
manual distribution as a resumable step. Remove this flow's automatic global
regeneration while retaining the explicit general Kerberos action.

**Tests and compatibility:** actual producer-to-observer callback, wrong epoch,
principal/keytab, historical task, partial failure, manual step and restart.
Preserve ordinary regeneration and descriptor isolation.

### R3 — P1: lost INSTALL recovery uses contradictory browser heuristics

**Trigger and impact:** lose the INSTALL response or refresh after submission;
the wizard cannot reliably recover its request and can misassociate other work.

**Root and source:** `ManagedDependencyRuntimePlanner.java:332,374` and
`orm/dao/ServiceDependencyDAO.java:959` associate PREP with the actual INSTALL
request, once per canonical host component. `Step9.tsx:455-597` requires coverage
per component, excludes those PREP request IDs, then scans by context, browser
time and targets; `:700` sends no launch ID. `api/requestApi.ts:30` limits scans
to 100 requests. `Step9.test.tsx:232` invents PREP 70 and independent INSTALL 91,
concealing the real producer contract. Browser inference replaces backend ownership.

**Complete repair:** atomically publish durable deployment launch ID, exact target
set, binding epochs and actual request/tasks. Recover by that ID. Preserve
per-host PREP versus per-component INSTALL as distinct observations.

**Tests and compatibility:** dropped response before/after commit, duplicate launch,
cohosted Master/RegionServer, pagination, clock skew and missing lineage. Extend
installer/Add Service without removing legacy request APIs.

### R4 — P1: consumer retry uses the provider host

**Trigger and impact:** retry consumer PREP in independent clusters; its plan uses
a provider host without a consumer component and fails.

**Root and source:** coordinator `:1049,1062` passes `binding.actionHostId` to
`buildRetryPreparationPlan`; dispatcher `:99,380` selected this host for provider
execution. Runtime planner `:795,833` needs a consumer host. Coordinator `:2045`
never makes `retryAllowed` true. Negative tests do not prove an executable retry.
One field carries two incompatible ownership roles.

**Complete repair:** retain provider pin separately from failed consumer host
observations. Derive exact retry targets and epoch rules from those observations;
advertise the capability from the same executable plan.

**Tests and compatibility:** positive independent-cluster retry, partial failures,
changed membership and stale snapshots. Keep provider journals/fencing; do not
repeat succeeded provider effects.

### R5 — P1: task and lineage publication are separate transactions

**Trigger and impact:** crash after `createAction` and before association;
restart can repeat provider/VERIFY tasks. Scheduler timeouts without reports may
leave dependency state active until server restart.

**Root and source:** dispatcher `:262,292-296` claims SCHEDULING, creates an action,
then marks DISPATCHED. DAO `:998,1026` permits reclaim/reassociation.
`recoverOutstanding:125` is called at startup (`controller/AmbariServer.java:565`)
without ongoing terminal-task reconciliation. Agent journals cannot reconstruct
Ambari task identity. Ownership publication is not atomic.

**Complete repair:** associate reserved commands with actual tasks in their
publication transaction. Terminal observers and a bounded reconciler feed the
existing coordinator. Uncertain remote effects stay fenced until reconciled.

**Tests and compatibility:** crash injection between writes, scheduler timeout,
queued/running/completed restart and duplicate events. Restrict observation to
owned managed operations; preserve ordinary ActionManager execution.

### R6 — P1: wizard completion ignores dependency readiness

**Trigger and impact:** INSTALL completes before VERIFY, or START returns no
request ID; wizard starts too early or reports success without evidence.

**Root and source:** `Step9.tsx:829,839` treats missing Start ID as success and
advances INSTALL/KEYTABS directly to Start. Coordinator `:1840` correctly rejects
unready consumers. Two components own incompatible deployment transitions.

**Complete repair:** backend deployment state owns phase and allowed actions.
UI observes install/credentials/verification/start. Missing response identity is
unknown until exact reconciliation establishes a no-op or original request.

**Tests and compatibility:** slow/failed verification, refresh every phase,
lost Start response and install-only mode. Preserve ordinary service progress.

### R7 — P1: Add Service retry selects resources outside its operation

**Trigger and impact:** retry while another component/workflow is incomplete;
unrelated components in the same cluster may be mutated.

**Root and source:** `Step9.tsx:939` retries cluster-wide
`HostRoles/desired_state=INSTALLED&HostRoles/state!=INSTALLED`. A broad predicate
replaces the operation's target ownership.

**Complete repair:** retry the backend operation's exact failed targets, sharing
R3's persistent set. No second retry state machine. Test two simultaneous service
operations and unchanged unrelated resources. Narrowing old retry scope is intentional.

### R8 — P1: backend lifecycle rules have no production UI consumers

**Trigger and impact:** provider stop/restart fails without a way to submit impact
confirmation; dependency updates/retry/detach lack an executable UI flow.

**Root and source:** lifecycle policy `:113,389` requires exact action/provider/
revision. `screens/Services/Actions.tsx:759,882` omits it.
`api/serviceDependenciesApi.ts:459` hardcodes STOP; only a test calls getImpact.
`ServiceDependencies.tsx:45,118,155` is read-only. Backend
`api/services/ManagedServiceDependencyService.java:120,127,135,142` exposes mutations.
An enforced domain contract is not connected to its user-facing caller.

**Complete repair:** integrate current impact into existing lifecycle dialogs and
dependency cards; submit immutable server operations and allowed actions. Preserve
anonymous dependent counts and reject stale confirmations.

**Tests and compatibility:** stop/restart/delete/detach, changed impact, denied
providers and data retention. Extend existing dialogs without stacked confirmation
or a new frontend orchestrator.

### R9 — P1: Blueprint bypasses managed dependency planning

**Trigger and impact:** remote-provider HBase Blueprint still requires or creates
local dependencies through static validation/autodeploy.

**Root and source:** `topology/BlueprintImpl.java:76,331` delegates to
`BlueprintValidatorImpl.java:65,288,349,379`, which knows legacy external settings,
not the managed plan. BIGTOP 3.2 HBASE metainfo `:37,44,227` requires HDFS clients
and auto-deploys ZooKeeper server. `TopologyManager.java:368` lacks the resolver
used by `StackAdvisorResourceProvider.java:165,254`. Automatic local HDFS server
creation was not established and must not be asserted.

**Complete repair:** pass validated Blueprint provider choices to the existing
resolver before dependency auto-completion. Retain clients and independently
requested local services. One resolver owns the decision across entry points.

**Tests and compatibility:** Blueprint and Add Service with identical plans,
permissions, host ownership and secure inputs; preserve local-only defaults.

## 4. Moderate issues and design debt

| ID / issue | Source and consequence | Convergence and compatibility |
| --- | --- | --- |
| R10 / P2: inconsistent path encoding | `Utils/clusterRoute.ts:51` and dependency APIs encode; `api/requestApi.ts:23` and older APIs interpolate names. Special characters break paths; RBAC bypass not established. | Shared API segment builder at actual callers; separate query serialization. Test Unicode, spaces, slash, percent and double encoding; normal names unchanged. |
| Split workflow ownership | Coordinator, dispatcher, processor and Step9 infer overlapping progress; CAS drafts do not prove task ownership. | Existing coordinator owns domain transitions, ActionManager tasks, DAO atomic lineage. React retains draft/UI/IDs. No new workflow framework. |
| Excessive module complexity | Coordinator exceeds 2,000 lines; Step9 combines polling, lineage, credentials and mutations. | Remove browser lineage code as backend ownership lands; extract only cohesive helpers with real callers. No unused DTO or orchestration layer. |
| Misleading evidence/fixtures | Historical PASS can mean source review only; lost-install mock contradicts actual producer; broader React fixtures fail. | One ledger below, immutable historical revision, real producer/consumer tests. Preserve raw evidence as history. |
| Recovery observability | Unknown dispatch, credentials and missing lineage look like stalled install. | Expose existing operation/phase/last transition and allowed action with safe reason codes; no independent status store or secrets. |

## 5. Acceptance scenarios and remaining runtime gates

The current behavior column below describes the remediated worktree, not the
reviewed HEAD. Passing focused tests do not close the runtime gates.

| Scenario | Current code behavior | Required behavior | Current blocker | Minimum remaining change/action | Test |
| --- | --- | --- | --- | --- | --- |
| Two independent clusters and different users | Agent reports are filtered before effects; REST request/deployment ownership and route/RBAC guards are enforced | No cross-cluster reads, writes or subscriptions | No two-user HTTP/broker deployment executed | Provision two isolated clusters/users; run current endpoints and real subscriptions | Denied reads/writes/subscriptions, mixed agent reports, revocation |
| A/HBase uses B/HDFS/ZooKeeper | Durable deployment waits for both bindings; same-realm proof validation and credential production are connected | Private namespaces and actual client operations without unintended local daemons | No live HDFS/ZK/KDC available in this validation run | Run supported BIGTOP 3.3.0 service setup; apply preview and approve exact bindings | Actual read/write, SASL, keytab and service checks |
| Provider stop/restart/delete; consumer detach/delete | Existing dialogs submit exact impact revisions; binding actions use backend capabilities; H2 detach/deletion guards pass | Preserve provider/data and fence invalid consumer use | Live rolling restart and storage retention not executed | Run lifecycle matrix with current endpoints and browser | Stop/restart confirmation, stale revision, delete denial, retained data after detach |
| Installation failure/retry/refresh/server restart | Backend owns UUID, exact targets, request history and retry epochs; bounded recovery reloads rows | Resume failed targets once across process restart | Tests recreate coordinator and use H2 transactions, but do not kill a live server/agent | Inject crash at each publication boundary; restart same database | INSTALL/credential/VERIFY/START/check failures, cohosted daemons and multiple bindings |
| Request response lost | GET/replay exact UUID; request/association commit together; notifications wait for commit | Recover same committed work without duplicate scheduling | Real proxy drop and process kill not run | Drop HTTP response after commit and retry same UUID | Frontend transport tests, H2 atomicity plus live fault injection |
| Credential completion | Actual SET_KEYTAB publication/result processor and periodic recovery use exact plan/task/epoch/principal/path; manual verification is explicit | Advance only with current producer evidence and real credentials | No KDC/distribution execution; agent helper is mocked in Python tests | Execute real producer/agent callback with same-principal multiple paths | Wrong task/epoch/path, partial failure, manual mode and restart |
| Missing installation lineage | UNRESOLVED fences missing or foreign request/task/binding associations; an acknowledged deployment returning 404 cannot be relaunched; browser does not scan | Never guess success or silently publish replacement | No automatic repair of genuinely missing historical identity | Restore authoritative persisted records through an audited recovery procedure; do not infer by time/name | Current negative tests; live retention/corruption experiment |
| React route and realtime isolation | Actual route guard, API segment encoding and cluster scope remain; managed page checks deployment ID and cluster ID | No stale A data/actions on B during route changes or reconnect | Browser/network/broker matrix remains unexecuted | Two sessions/tabs, delayed REST, route changes, reconnect and permission revocation | Current route/RBAC/transport tests plus browser capture |
| Blueprint and Add Service | PREPARE_ONLY freezes required types and prepares resources; live approval/deployment uses the same resolver as Add Service | Same approved dependency constraints and no unintended provider daemon creation | Explicit two-step Blueprint flow; no unattended one-POST deployment or secure live Blueprint test | Exercise documented handoff; automation must persist the returned deployment UUID | Real validator/HostRequest/frozen-requirement tests, then live Blueprint provisioning |
| Actual integration versus mocks | Fresh Guice/H2 tests exercise transaction rollback, locks, task association, detach and migration | Real distributed effects and recovery match persisted state | No real agents/services/browser; TypeScript baseline errors remain | Execute runtime matrix and separately repair baseline frontend typing | 205 Java, 91 frontend, 31 Python focused tests; full lifecycle and live acceptance still open |

## 6. Small target architecture and execution order

`ManagedDependencyDeploymentCoordinator` owns the consumer INSTALL, readiness,
START and service-check workflow in one deployment row. The existing
`ManagedServiceDependencyCoordinator` owns binding approval, retry and detach;
it does not independently install or start services. `ServiceDependencyDAO`
persists binding/snapshot/epoch, immutable command intents, host observations,
credential identity and request/stage/task links. The small deployment DAO owns
launch UUID, user/cluster ownership, exact targets and request history. Publication and association commit
together. Provider execution pin and consumer target hosts remain distinct.

REST authorizes actual consumer/provider before resolving or submitting a plan.
ActionManager persists/runs tasks; session-authenticated reports become facts only
after ownership validation. Terminal observers plus bounded restart/periodic
reconciliation feed the same coordinator transitions. STOMP projects authorized
persisted state, never owns workflow. Duplicate facts use operation/epoch/task keys
and compare-and-set transitions. Unknown remote effects remain fenced.

React stores route, draft revision, unsaved inputs, selections, operation IDs and
an acknowledgement that the exact server deployment has existed. Checkpoints
retain the complete step identity; acknowledgement never declares task success.
It renders backend phase/capabilities and discards old-scope responses. It does
not discover lineage or declare credential readiness. Blueprint resource preparation freezes required external dependency types before
auto-completion. Its subsequent live approval uses the same resolver/preview as
StackAdvisor and Add Service; the template is not itself an authorization token.
This deliberate two-step boundary is described below.

1. R1: agent report boundary, including no side effects for rejected reports.
2. R3/R5: atomic deployment/task lineage and terminal/restart recovery.
3. R2/R4: exact credential completion and executable consumer retry.
4. R6/R7/R8: simplify frontend progression and connect lifecycle APIs.
5. R9/R10: shared dependency planning and API path encoding.
6. Execute real-service, browser, RBAC and database migration acceptance.

## 7. Executed review validation

These results precede remediation. Java tests used existing compiled classes and
are not fresh compilation of HEAD. No full build or runtime acceptance is claimed.
Local Maven 3.8.7 does not meet the project's Maven 3.9.x lifecycle requirement;
Java is 17.0.20 and Node is 22.23.1. No fresh Docker/KDC deployment was run here.

Frontend command (working directory `ambari-web/latest`):

```sh
node --input-type=module -e 'import { startVitest } from "vitest/node"; import react from "@vitejs/plugin-react"; const ctx = await startVitest("test", ["src/screens/ClusterWizard/Step9.test.tsx", "src/components/ClusterBoundary.test.tsx", "src/store/UserContext.test.tsx", "src/api/serviceDependenciesApi.test.ts", "src/router/RoutesList.test.tsx"], { config: false, watch: false, cache: false, environment: "jsdom", setupFiles: ["./src/test/setup.ts"], testTimeout: 10000, maxWorkers: 2 }, { plugins: [react()] }); await ctx?.close();'
```

Four existing files: 38 passed. `ClusterBoundary.test.tsx` did not exist and was
not tested. The same entry point/options with `AppLoader.test.tsx`,
`store/context.test.tsx`, `Utils/clusterEvents.test.ts`,
`screens/ClusterWizard/Step8.test.tsx`,
`screens/ClusterWizard/clusterStore/context.test.tsx` and
`screens/Directories/ServiceDirectory.test.tsx` ran six files: 45 passed, 19 failed,
one unhandled error (exit 1). Failures include duplicate DOM fixtures, split-text
assertions, old Step8 copy/assignment expectations, and revision/queue expectations.
They require fixture-versus-product triage, not blanket dismissal.

Python command (repository root):

```sh
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-agent/src/test/python/resource_management -p TestManagedDependency.py -q
```

14 passed. Tests temporary journals/process locks, not real services.

Java review execution loaded `java.class.path` from
`ambari-server/target/surefire-reports/TEST-org.apache.ambari.server.controller.dependencies.ManagedDependencyTaskResultProcessorTest.xml`,
added the cached JUnit Platform launcher 1.14.4 jar, and selected tests using
`jshell --feedback concise --class-path <that classpath> -`. Package
`org.apache.ambari.server.controller.dependencies`: `ManagedDependencyCommandTest`,
`ManagedDependencyRuntimePlannerTest`, `ManagedDependencyTaskResultProcessorTest`,
`ManagedDependencyOperationDispatcherTest`, `ManagedDependencyLifecyclePolicyTest`,
`ManagedDependencyReadinessPolicyTest`. 50 passed from cached bytecode. The same
harness reproduced R1 with real ActionManager and mocked persistence. Fresh
remediation commands and results follow below.

## 8. Documentation ownership and implementation progress

- This file owns defects, acceptance gaps, implementation order and executed
  validation. Reviewed-revision findings remain intact after fixes.
- [Main design](multi-cluster-management.md) owns target invariants and interaction
  requirements. The old interaction document was merged there and deleted.
  Obsolete worker/model/deferred-test/publication orchestration was removed.
- [React foundation](../frontend-refactor/react-current/multi-cluster-foundation.md)
  retains migration mapping and links here for current gaps and tests. Ember
  baseline and generated evidence are preserved.
- External `.codex-runs/ambari-multicluster` mission/status/checkpoint and evidence
  files are labelled historical. Specialized security/integration contracts remain
  available, but are not acceptance evidence. No unrelated artifacts were deleted.

The source plan for R1–R10 has been implemented. Compilation and tests ran together
after the source changes, followed by repairs and focused reruns for observed
failures. The original review was rechecked against actual production callers;
additional omissions are recorded below. This is source remediation with passing
focused validation, not complete distributed-system acceptance.

| Work | Current implementation | Acceptance limitation |
| --- | --- | --- |
| R1 | ActionManager filters persisted task/stage/host/cluster identity; both heartbeat paths consume only accepted reports before side effects | 20 ownership regressions pass; real agent integration remains open |
| R2 | Action publication freezes credential producer/task/principal/keytab lineage; actual keytab publication marks transmission; terminal task processor and periodic recovery invoke verification; explicit manual verification | Actual KDC/keytab distribution remains a runtime gate |
| R3/R5 | A durable deployment UUID owns exact targets, attempts and actual request IDs. DAO mutation and task publication share the Ambari transaction; terminal and bounded periodic scans reconcile | Real H2 rollback/publication and notification tests pass; process-kill faults remain open |
| R4/R7 | Consumer daemon hosts own retry preparation. Reinstallation creates a new epoch and assigns AWAITING_INSTALL intents to the replacement INSTALL; other hosts use reserved preparation. Legacy retry reads failed tasks of the exact request | Exact consumer-host retry and failed-request replay tests pass; live cohosted/two-binding failure timing remains open |
| R6 | Backend waits for verified bindings before START and service checks; React renders persisted state and request history, preserves launch/retry IDs, rejects legacy missing lineage | Real browser, delayed responses and full service checks remain runtime gates |
| R8 | Existing provider dialogs fetch action-specific impact; dependency cards expose backend update/retry/detach capabilities | Live rolling restart and provider data-retention acceptance pending |
| R9 | Typed Blueprint requirements suppress remote daemon auto-completion while retaining clients/explicit local daemons. PREPARE_ONLY persists placement/configuration. Live approval and deployment reuse the same binding/resolver/API path as React Add Service. Frozen requirements guard ordinary HBase lifecycle submission | This is an explicit two-step Blueprint flow, not an unattended single POST deployment. Live secure Blueprint acceptance pending |
| R10 | Shared raw-identity path segment encoder applied to existing API callers and rolling request URIs | Special-character transport tests pass; browser/server encoded-slash routing remains open |

### Recheck against the original review

The original R1–R10 findings remain the traceable checklist. R2 now explicitly
records the overlooked hardcoded secure-preview gate. R5 now includes notification
publication: atomic database writes alone did not make events atomic. The following
additional defects were verified while exercising the actual production graph.
Locations in this addendum identify the current worktree; the descriptions state
what failed before the repair.

**R11 / P1 — eager initialization crosses the persistence boundary.** Creating the
real Guice graph failed before requests could be served: static REST injection
constructed the binding resolver before session interceptors were ready; resolving
ActionManager also formed `ActionDBAccessor -> runtime planner -> resolver ->
KerberosHelper -> ActionManager`. The defect affects server startup and was hidden
by mocked resolvers. REST adapters and ActionDBAccessor now hold Guice Providers,
resolved only by actual request/publication callers. See
`api/services/ServiceDependenciesApiService.java:46` and
`actionmanager/ActionDBAccessorImpl.java:137`. This moves initialization to the
owning lifecycle without an extra orchestrator. The unmocked Guice schema tests
and real ActionDBAccessor/H2 tests pass. Existing endpoints retain behavior;
startup smoke testing in the packaged server remains required.

**R5 supplement / P1 — rollback published tasks that did not exist.** Injecting a
checked exception after ActionDBAccessor publication rolled back the deployment,
request and PREP association but delivered one TaskCreateEvent. Task/STOMP listeners
could observe or act on uncommitted state. The transaction interceptor now owns
commit notifications; TaskEventPublisher and STOMPUpdatePublisher enqueue there,
clear on rollback and deliver after the outer commit. See
`orm/AmbariJpaLocalTxnInterceptor.java:137,209`. H2 tests now prove zero task events
on rollback and one on commit; task-listener and transaction-interceptor tests
also pass. This intentionally changes in-transaction notification timing for
existing callers; periodic reconciliation handles lost notifications after a
process crash. It is not a durable general event bus.

**R12 / P1 — schema helpers did not implement the declared storage contract.**
Creating the additive schema failed because GenericDbmsHelper discarded explicit
LOB definitions and passed a null Java type to EclipseLink. H2 constraint detection
queried Derby system tables, and a unique binding lookup generated `FOR UPDATE
LIMIT`, rejected by H2. The generic column converter now preserves explicit types;
H2 uses its own schema metadata; the unique-key binding lookup has no redundant
pagination. See `orm/helpers/dbms/GenericDbmsHelper.java:230`,
`orm/helpers/dbms/H2Helper.java:66`, and `orm/dao/ServiceDependencyDAO.java:617`.
Four real migration tests and the 23-test H2 dependency suite pass. This repairs
shared schema responsibilities without database-specific workflow exceptions.
Other database dialect migrations remain unexecuted; the generic converter change
also applies to existing callers with explicit database types.

**R8 supplement / P1 — lifecycle projection and command ownership disagreed.**
The generic command-owner check rejected the active DETACH invalidation command;
bulk service deletion refreshed agent metadata while the cluster service map still
contained removed service rows. Real H2 lifecycle tests exposed both. The command
check now recognizes only the active DETACH/INVALIDATE pair at the exact epoch,
and bulk deletion removes service cache entries before building deletion
projections. See `orm/dao/ServiceDependencyDAO.java:1529` and
`state/cluster/ClusterImpl.java:1380`. Detach replay/data-reference retention and
concurrent deletion/publication tests pass. Ordinary PREP/VERIFY still cannot run
in DETACHING. Full cache reconstruction after arbitrary legacy deletion rollback
is not established by these tests and remains a broader Ambari limitation.

The fixture audit also corrected invalid hash encodings, absent permission and
repository parents, unsupported stack versions, missing provider principals,
nested Mockito stubbing, and tests that used a second binding for an already-owned
consumer/type. Assertions still verify forbidden status/code and immutable replay;
no production security checks were weakened to match mocks.

Current entry points for the original repairs (dependency class paths below are
relative to `controller/dependencies/` unless otherwise specified):

| Review item | Implemented ownership/caller boundary | Focused proof |
| --- | --- | --- |
| R1 | `actionmanager/ActionManager.java:178`, then both heartbeat paths | CommandReportOwnershipTest: 20 |
| R2 | `ManagedDependencyCredentialManager.java:111,165,186`; production TaskResultProcessor; coordinator validator at line 103 | CredentialManager: 4; real descriptor/coordinator and snapshot validation; Python Kerberos: 17 |
| R3/R5 | `ManagedDependencyDeploymentCoordinator.java:97,218`; deployment DAO transaction; ActionDBAccessor; committed task events | DeploymentCoordinator: 13; H2 dependency integration: 23 |
| R4/R7 | coordinator `retryInstallation:1047`, `retryPreparationCommands:1115`; runtime planner uses exact host histories | RuntimePlanner: 9; deployment failed-request replay; real H2 reinstallation claim/association/replay and epoch history |
| R6 | deployment `advance:218`, `publishChecks:358`; `ManagedDeploymentProgress.tsx` renders server state | Step9: 11; backend phase tests; installationProgress: 6; scopedWorkflow: 20 |
| R8 | existing Actions/restart dialogs and DependencyActions use current backend capabilities and impact | Impact hook, service dependency page, REST adapters and H2 lifecycle tests |
| R9 | `ManagedDependencyBlueprintPlan.java:45,81`; topology PREPARE_ONLY and frozen types; same live binding APIs as Add Service | Blueprint plan: 3; existing validator: 10; HostRequest: 2 |
| R10 | `api/apiPath.ts` at existing request callers | Encoded API transport: 7 |

The old coordinator remains large; this work removes installation ownership and
browser lineage inference from it rather than claiming all historical complexity
is eliminated. Safe error codes, phases, request histories and binding capabilities
are exposed, but operational dashboards and real fault-injection acceptance remain
open. Legacy client-only Start responses can complete only after checking actual
owned, installed client targets; daemon deployments still require a request ID.
That intentionally tightens Classic's empty-response success behavior.

### Three-pass completion audit against the original checklist

All three passes inspected current source and production callers. Compilation and
focused execution followed the combined fixes; failures found by those tests were
repaired and the affected selection rerun. No pass certifies absence of all bugs.

| Pass | Original review coverage and actual chain inspected | Result |
| --- | --- | --- |
| 1: transactions and recovery | R2–R7; deployment launch/retry/advance, deployment DAO transaction, ActionDBAccessor association, credential producer/result/recovery, dispatcher and after-commit publishers | Fixed pre-install binding failure remaining in WAIT_PROVIDER and retry bypassing binding preparation. Existing H2 atomicity and epoch tests rerun. |
| 2: ownership and permissions | R1/R2/R8/R10; HeartBeatHandler -> HeartbeatProcessor -> ActionManager, host mapping/DAO boundary, deployment REST and owner checks, STOMP session reauthentication -> projection, actual-route authorization | Retry capability now checks the same current consumer mutation permissions as the endpoint. Added distinct-cluster, distinct-owner, permission-revocation and inactive-owner recovery tests. No new cross-cluster authorization bypass was established in these inspected chains. |
| 3: frontend and document consistency | R3/R6/R7/R10; Step9 -> managed progress -> actual wizard reducer -> workflow projection, scoped service rendering, Ember installation/service baselines and Classic Step9; original R1–R12 ledger | Fixed complete-step checkpoint loss, mistaken shared-reference removal, acknowledged-404 resubmission and stale completion/cluster rendering. Updated original finding-to-code/test mapping and acceptance limitations. |

**R4/R6 supplement / P1 — installation waits forever on failed preparation.**
Trigger: a selected binding fails before INSTALL while its provider-preparation
hash remains present. The old NEW/WAIT_PROVIDER branch checked failure only when
the hash was absent, then waited on a denied installation capability indefinitely.
A generic retry also reset the deployment without retrying that failed binding.
`controller/dependencies/ManagedDependencyDeploymentCoordinator.java:140,278,442`
now owns the transition to FAILED and consults current binding capabilities for
both the retry response and mutation. A permitted retry creates binding preparation
operations once and returns to the original installation/start/check phase.
Failed INSTALL requests still use the separate exact-task reinstallation path.
Tests cover pre-install failure, same-attempt replay, stale bindings and missing
provider proof. Existing installations can now report a failure earlier. Provider
preparation with unknown effects remains fenced: it is not safely replaceable by
a consumer retry, and still requires authoritative fencing review.

**R8 supplement / P2 — retry capability outlives its owner's mutation grant.**
Trigger: the deployment owner retains VIEW but loses a required consumer mutation
grant. The old GET response still advertised retry although POST rejected it.
The coordinator's `summary` and `authorize` now share `canModify` at line 484;
current owner identity remains mandatory, and provider mutation authorization is
still enforced by the binding operation. Tests prove denied cross-cluster reads,
denied non-owner writes, loss of retry capability and no publication for an
inactive owner. Read access for another authorized user is unchanged. This is a
capability/authorization consistency repair, not evidence of a prior write bypass.

**R3/R5/R6 supplement / P1 — partial checkpoints destroy installation identity.**
Trigger: retry or acknowledgement writes a partial INSTALL_START_TEST payload.
Both actual ClusterCreation and AddService reducers replace the entire step;
the progress page assumed they merged fields. Handoff and intent could disappear
on refresh, which dispatch-only mocks did not detect. The progress page now owns
a complete step checkpoint, initialized from the resolved handoff and merged with
each local update before dispatch (`ManagedDeploymentProgress.tsx:71`). The test
runs the real reducer and `projectClusterCreationValues`, then retries and remounts
from the saved result. No shared reducer semantics change for existing wizards.

**R3/R6 supplement / P1 — missing acknowledged deployment is treated as a fresh launch.**
Trigger: a page has read its exact server deployment, then a later GET returns
404, including after refresh. The old version checkpoint allowed another POST.
The page now persists acknowledgement only after verifying cluster/deployment IDs
and refuses replacement when that acknowledged identity disappears
(`ManagedDeploymentProgress.tsx:67,99,120`). An unacknowledged lost launch response
can still replay its immutable UUID. Revalidation errors disable cached completion
at line 178. Tests cover post-completion disappearance, browser refresh, foreign
responses after cached success and ordinary lost-response replay. This intentionally
blocks unsafe recovery of missing server records; no timestamp/name inference or
second workflow state machine is introduced.

**R13 / P1 — workflow sanitization mistakes sharing for cycles.**
The real checkpoint regression first failed because the same intent object was
present under both handoff and current intent. `Utils/scopedWorkflow.ts:69` tracked
all visited objects instead of just ancestors, silently dropped the second path
and marked it for user re-entry. Arrays bypassed cycle detection altogether. The
sanitizer now removes an object from its traversal set after each branch and checks
array ancestors too. Tests prove shared intent survives, credentials are stripped
on every occurrence, genuine cycles terminate, and the actual reducer/projection
round trip preserves the full handoff. Existing wizard payloads with shared objects
now retain their non-secret fields; secret-removal rules are unchanged.

**R10 supplement / P2 — render identity lags the route identity.**
The service dependency page cleared rows in an effect, allowing the first commit
under a new cluster context to contain the old provider rows. It now renders only
a result with the current cluster/runtime scope (`ServiceDependencies.tsx:146,191`).
A layout-effect probe verifies the committed DOM before passive effects run.
Step9 also keys its managed child by the resolved intent and cluster name, matching
the intent it actually passes through context (`Step9.tsx:663`). Existing pages may
show a loader while a new scope is loading; old-scope actions are not rendered.

The first nine-file completion-audit run passed 70 of 71 tests; its real checkpoint
test exposed the sanitizer defect. After repair, the expanded run passed 89 of 91
and exposed two pre-existing fixture assumptions. The final ten-file rerun passed
91 of 91 with zero failures or unhandled errors.

The expanded workflow test file required actual i18n initialization. Its previous
credential-reentry test also skipped the documented queue reset: a failed queued
save pauses subsequent writes until explicit reload. The corrected fixture proves
both rejection before reload and safe re-entry after reload; production queue
failure semantics were not relaxed. The final frontend selection passes all 91
tests, including 20 workflow tests and 11 Step9 tests. HTTP remains mocked; real
reducer/projection coverage does not substitute for a browser or server workflow
persistence integration test.

### Blueprint handoff contract

A managed Blueprint declares requirements, not authorization, through its existing
structured settings representation:

```json
{"settings":[{"managed_dependencies":[
  {"consumer_service":"HBASE","dependency_type":"HDFS"},
  {"consumer_service":"HBASE","dependency_type":"ZOOKEEPER"}
]}]}
```

Provision that template with `provision_action: PREPARE_ONLY`. Wait for actual host
placement and topology configuration to finish. Then use the existing HBASE
complete-selection preview API, apply its reviewed configuration, approve exact
binding UUIDs/fingerprints, and POST an immutable deployment UUID and exact targets
to `clusters/{cluster}/services/HBASE/dependencies/deployments/{id}`. The last API
is also the React managed Add Service caller. Provider IDs and credentials do not
belong in a reusable Blueprint template. Declared required types are frozen on the
provisioning request; deleting/replacing the template cannot make an unbound HBase
INSTALL legal. Ordinary local Blueprints retain their default actions.

### Remediation validation

Results from the final remediated worktree: **205 Java tests passed, 91 frontend
tests passed, 31 Python tests passed; zero skipped in those selections**. Full
TypeScript checking exits 2 with 49 errors, compared with 52 at the reviewed HEAD;
normalizing file/diagnostic text shows no new diagnostics. This is not a full
Maven lifecycle, browser, KDC, or real-service acceptance claim. These validation
results were recorded before commit/publication; no runtime deployment was performed.

The preexisting `target/classes` lacked `PropertyExists`/`PropertyValueEquals`,
which made JAXB fail with `InternalError`. Both classes/test-classes were moved
to `/tmp/ambari-review-build-backup-jq2axli3` before rebuilding. Explicitly copying
`stacks` and `common-services` into `target/classes` supplies resources excluded
by the default resources goal. Rebuilding compiled **2,176 production and 889
test source files**; subsequent focused runs compiled all later modified sources.
This uses installed dependencies and direct Maven goals because Maven 3.8.7 does
not satisfy the repository's 3.9.x lifecycle enforcer.

Preparation actually executed (repository root):

```python
from pathlib import Path
import shutil, tempfile
backup = Path(tempfile.mkdtemp(prefix='ambari-review-build-backup-'))
for name in ('classes', 'test-classes'):
    source = Path('ambari-server/target') / name
    if source.exists():
        shutil.move(str(source), str(backup / name))
for name in ('stacks', 'common-services'):
    shutil.copytree(Path('ambari-server/src/main/resources') / name,
                    Path('ambari-server/target/classes') / name, dirs_exist_ok=True)
```

Fresh compilation command (exit 0):

```sh
mvn -pl ambari-server -Denforcer.skip=true -DskipTests -Dcheckstyle.skip=true org.apache.maven.plugins:maven-resources-plugin:resources org.apache.maven.plugins:maven-resources-plugin:testResources org.apache.maven.plugins:maven-compiler-plugin:compile org.apache.maven.plugins:maven-compiler-plugin:testCompile
```

Final Java command after the three-pass audit (exit 0, 205 tests, zero failures/errors/skips):

```sh
mvn -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=CommandReportOwnershipTest,ManagedDependencyCredentialManagerTest,ManagedDependencyBlueprintPlanTest,ManagedDependencyDeploymentCoordinatorTest,ManagedDependencyCommandTest,ManagedDependencyRuntimePlannerTest,ManagedDependencyTaskResultProcessorTest,ManagedDependencyOperationDispatcherTest,ManagedDependencyLifecyclePolicyTest,ManagedDependencyReadinessPolicyTest,ManagedServiceDependencyCoordinatorTest,ManagedDependencyDescriptorResolverTest,ManagedDependencySnapshotValidatorTest,ServiceDependencyDAOTest,ServiceDependencyDAOIntegrationTest,ServiceDependencySchemaUpgradeTest,BlueprintValidatorImplTest,HostRequestTest,ManagedServiceDependencyServiceTest,ServiceDependenciesApiServiceTest,AmbariJpaLocalTxnInterceptorTest,TaskStatusListenerTest org.apache.maven.plugins:maven-compiler-plugin:compile org.apache.maven.plugins:maven-compiler-plugin:testCompile org.apache.maven.plugins:maven-surefire-plugin:test
```

The earlier recheck added a real H2 reinstallation test for AWAITING_INSTALL:
background dispatch cannot take that intent, replacement INSTALL association is
idempotent, a different task is rejected, and prior-epoch history/provider proof
survive. Its targeted rerun passed all 23 integration tests (exit 0). The final
205-test Java command above includes these 23 tests and seven additional deployment
recovery/authorization tests. Repeated executions are not counted twice.

```sh
mvn -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ServiceDependencyDAOIntegrationTest org.apache.maven.plugins:maven-compiler-plugin:testCompile org.apache.maven.plugins:maven-surefire-plugin:test
```

Final frontend command (`ambari-web/latest`, exit 0, ten files/91 tests):

```sh
node --input-type=module -e 'import { startVitest } from "vitest/node"; import react from "@vitejs/plugin-react"; const ctx = await startVitest("test", ["src/screens/ClusterWizard/Step9.test.tsx", "src/screens/ClusterWizard/installationProgress.test.ts", "src/screens/Services/useDependencyImpact.test.tsx", "src/screens/Services/ServiceDependencies.test.tsx", "src/screens/Services/serviceRestartUtils.test.ts", "src/api/serviceDependenciesApi.test.ts", "src/api/apiPath.test.ts", "src/router/RoutesList.test.tsx", "src/store/UserContext.test.tsx", "src/Utils/scopedWorkflow.test.ts"], { config: false, watch: false, cache: false, environment: "jsdom", setupFiles: ["./src/test/setup.ts"], testTimeout: 10000, maxWorkers: 2 }, { plugins: [react()] }); await ctx?.close();'
```

Python commands (repository root, both exit 0; 14 and 17 tests respectively):

```sh
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-agent/src/test/python/resource_management -p TestManagedDependency.py -q
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-server/src/test/python -p TestKerberosBigtop.py -q
```

Type checking (`ambari-web/latest`, exit 2):

```sh
./node_modules/.bin/tsc -b --pretty false
```

The same command was run against an isolated `git archive HEAD ambari-web/latest`
with a symlink to the existing node_modules. Both runs fail in baseline code; the
three removed diagnostics belonged to the replaced Step9 fixture. No unrelated
typing errors were suppressed. `git diff --check` exits 0.

Current validation logs are `/tmp/ambari-multicluster-three-pass-java.log`,
`/tmp/ambari-multicluster-three-pass-frontend-final.log`,
`/tmp/ambari-multicluster-three-pass-typescript-final.log`. Earlier validation logs
are `/tmp/ambari-multicluster-remediation-final-java.log`,
`/tmp/ambari-multicluster-remediation-reinstall-integration.log`,
`/tmp/ambari-multicluster-remediation-final-frontend.log`,
`/tmp/ambari-multicluster-remediation-python.log`,
`/tmp/ambari-multicluster-remediation-kerberos-python-final.log`,
`/tmp/ambari-multicluster-remediation-final-typescript.log` and
`/tmp/ambari-multicluster-head-typescript.log`. They are local run artifacts,
not a substitute for the commands and source contracts above.

The Java selection includes 23 real H2/Guice dependency integration tests and four
schema migration tests. Deployment state tests use mocked persistence/actuators;
the separate H2 tests prove real ActionDBAccessor publication and rollback.
Credential tests execute the real manager/result processor but mock transport and
KDC. Python keytab tests invoke the real output hook but mock file distribution.
Frontend tests mock HTTP and cannot prove server authorization or browser routing.
Earlier broad React failures remain historical, untriaged outside this change's
focused selection; they are not counted as passes.

Runtime preflight also ran `command -v docker`,
`docker ps --format '{{.Names}}\t{{.Status}}'`, and
`docker images --format '{{.Repository}}:{{.Tag}}'`. Docker is available. The only
running container was unrelated to Ambari; cached Ambari images reference older
baseline builds, not this worktree. No two-cluster/KDC setup containing these
changes was provisioned. Thus live acceptance is **not executed**, not claimed to
be blocked merely because Docker is unavailable. The historical registry timeout
is historical evidence, not a verified current outage.


### Publication structure

The user subsequently requested publication to the existing personal-fork branch
`origin/AMBARI-26654-multicluster-final`. The remediation is organized as six topic
commits under AMBARI-26654: agent report ownership; durable backend deployment and
credential lineage; API path encoding; workflow sanitization; React deployment and
lifecycle integration; and the consolidated review/design documentation.

The backend commit keeps schema/entities, DAO transactions, production credential
callbacks, dispatch recovery, deployment REST and frozen Blueprint requirements
together: their shared runtime planner and publication boundary must use one
contract. Separating those callers from the schema and coordinator would leave
an intermediate tree uncompilable or permit task publication without its lineage.
Frontend API contract changes stay with the required UI consumers. Focused tests
are included with the corresponding implementation. Validation totals above cover
the combined source tree; individual intermediate checkouts were not separately
built. Git history records the final commit IDs. Publication does not close the
remaining runtime acceptance gates.

### Packaged runtime follow-up: topology and build preflight (2026-09-10)

The user requested a native deploy-tool build and live installation, then refined
acceptance to six registered agents: install cluster A on three hosts, leave three
unassigned, and create independent Hadoop cluster B on those remaining hosts via
API. A single cluster with an added service is insufficient for this gate.

The exact-commit build of `675d4b1dcc657e89448f5d96c7d8a50efcc89810` failed in
`ambari-web`'s real `npm run build` TypeScript phase; no RPM from that run was
accepted. Its deploy build run was
`ambari-675d4b1dcc65-a635f42f-101c3f6e-4d1869d1`. The previously documented type
errors are therefore a packaging blocker, not merely optional diagnostics.
The follow-up fixes restore typed workflow records and step names, remove unused
bindings, type the existing test fixtures/spies, and declare the user-event test
dependency. Tests remain in TypeScript compilation; type checking is not skipped.

Executing the affected recovery tests also exposed an Add Service retry defect:
resetting a boolean hydration flag and immediately restoring it can be batched
into one React render. Service loading had already been cleared but its effect
never restarted, leaving an endless spinner. Hydration now records persistence
identity plus recovery generation; each accepted snapshot restarts its dependent
reads. This preserves the Classic Add Service controller's explicit service/host
reloads while retaining React's cluster-scoped stale-response rejection.

Initial test failures also included missing translation initialization, leaked DOM
between master-assignment tests, a mock returning unstable hook arrays (an infinite
render loop), and exact save-count assertions that ignored queued user edits.
The repaired tests verify current placement, saved CAS revisions and suppression of
stale mutations. They do not substitute mock success for a live server result.

Executed from `ambari-web/latest`:

```bash
npx --no-install tsc -b
npx --no-install vitest run --maxWorkers=2 src/components/AssignMasters.test.tsx src/screens/ClusterWizard/ManagedDependencySelector.test.tsx src/screens/ClusterWizard/ManagedDependencySettings.test.tsx src/screens/ClusterWizard/Step6.test.tsx src/screens/ClusterWizard/Step7/RestAllTabs.test.tsx src/screens/ClusterWizard/deploymentInputRecovery.test.ts src/screens/ClusterWizard/managedDependencyAdvisor.test.ts src/screens/Services/AddServiceWizard/wizardDataStore/context.test.tsx src/screens/KerberosWizard/KerberosStore/context.test.tsx
npx --no-install vitest run --maxWorkers=1 src/screens/Services/highAvailibility/resourceManager/store/context.test.tsx src/screens/Services/highAvailibility/rangerAdmin/store/context.test.tsx
```

Results: TypeScript passes; 51 plus 7 focused tests pass. Step 6 still emits existing
React list-key warnings. The initial broader invocation did not finish cleanly
because of the unstable Step 6 mock and is not counted as a pass. Live six-host
installation, server authorization, runtime component checks and KDC acceptance
remain open until corresponding runtime evidence is recorded below.

#### Live startup correction (2026-09-10)

Native Maven and all three RPM identity checks passed for source `5b18f54533dd`.
Deploy's first publication failure was a false negative: the real Server RPM file
inventory has 2,235 paths / 166,182 bytes, while its command runner retained only
the last 64 KiB. Reading machine data through the complete binary result fixes
that check. Metrics additionally required the upstream dotted `package.release`
property. These are deploy-tool corrections; neither bypasses artifact validation.

The six-node installation exposed a **P1 production startup defect** missed by the
prior mocked-controller integration setup. Constructing the dependency dispatcher
created runtime planner → descriptor resolver → management controller → command
helper → runtime planner. Guice could not proxy the unfinished concrete planner;
Server never opened port 8080. The resolver now lazily obtains the controller and
Kerberos adapter only when performing an authorized security calculation. Both
edges matter because the adapter's Kerberos helper also references the controller.
This preserves the existing workflow owners and removes premature construction;
there is no new orchestration abstraction or security bypass.

Only `ManagedDependencyDescriptorResolver.java` was compiled with `javac --release
17`, using the existing verified local classpath `/tmp/ambari-review-java-cp`.
Its three resulting class entries replaced the corresponding entries in the
container's Server JAR; the RPM was not rebuilt. Original and patched JAR digests
are recorded in the private runtime overlay manifest. The real packaged Server
then started successfully. Acceptance after this point concerns **the RPM plus
this explicit overlay**, not an unchanged RPM. The disposable cluster uses
non-Kerberos security; successful startup does not establish KDC acceptance.

The changed test source was compiled with the same classpath. Executed regression:

```sh
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyDescriptorResolverTest,ManagedServiceDependencyCoordinatorTest org.apache.maven.plugins:maven-surefire-plugin:test
```

Result: 34 tests, zero failures/errors/skips. The added test uses real Guice to
verify construction defers both heavyweight dependencies; DAO/metadata are test
providers. Its first run failed because `toInstance` injected members of Mockito
objects and required unrelated database bindings; the corrected setup uses provider
bindings. Actual full-container startup is the production-graph evidence.


#### Six-node runtime checkpoint and production corrections (2026-09-10)

The isolated Docker project is `ambari-mc-api-675d4b1dcc-a63c939d`. A `mc_api_test`
(cluster ID 2) owns worker1..3; B `mc_api_b` (ID 3) owns worker4..6. All six Agents
registered before B creation; the second three were initially unassigned. B was
created and extended through private API acceptance code, not a deploy add-service
command. The two clusters have separate Cluster/Host memberships and Hadoop
service records. Native installation does not by itself prove service health.

| Scenario | Actual result at this checkpoint | Remaining gate |
| --- | --- | --- |
| Initial A / API-created B | A request 1 completed (52 logical tasks). B original request 10 aborted; explicit reinstall 21 and start 22 completed. | All-service workload checks remain separate. |
| Server restart during B start | Request 22 completed across a deliberate Server restart after the heartbeat correction. | Hard kill at each managed transaction boundary is not covered by this single restart. |
| Distinct users and host isolation | Each CLUSTER.USER sees only its own cluster; foreign reads/writes return 403, foreign scoped request returns 404, duplicate host attachment returns 409. | Revocation during an active workload and all database dialects remain open. |
| Real STOMP delivery | Concurrent A/B ZooKeeper checks 95/96 completed; each authenticated subscription received only its own request events. A subscription to B task 302 received ERROR. | Broker revocation/reconnect matrix remains open. |
| Real React route guard | Headless Chrome logged in as mc_a_viewer, rendered A's three hosts and rejected B's explicit route with Cluster access unavailable; no B hosts appeared. | Back/Forward, two-tab switching and complete Add Service browser execution remain open. |
| B provider preparation for A HBase | Approved HDFS/ZooKeeper snapshot 3 preparations, requests 102/104, completed on B. | Provider preparation is not consumer readiness or HBase health. |
| Lost launch response | Deliberately discarded the launch response; GET and identical POST recovered deployment fbf55091-113d-49fa-81f9-337054efa4ef without request scanning. | Lost acknowledged installation lineage still requires its separate acceptance case. |
| HBase installation and recovery | Original deployment and failed request history retained. Request 107 failed in a common hook; request 110 advanced into HBase configuration and failed package-parent ownership validation. Explicit API retries use saved attempt UUIDs. | Cross-cluster HBase START/read-write and lifecycle acceptance remain open while these runtime corrections are exercised. |
| Kerberos completion | Production callbacks and exact task association have source/focused coverage. | No KDC execution in this unsecured six-node topology; not accepted as runtime verified. |

The following P1 findings extend R3/R5/R6/R9. They were discovered through actual
production callers and must not be erased by later successful retries:

- **Restart heartbeat baseline:** restored hosts have no current-process heartbeat.
  The monitor previously declared them lost immediately and aborted active work.
  It now starts an observation window for the process and retains actual heartbeat
  timing and already-lost status. `HeartbeatMonitorRecoveryTest` verifies the clock
  boundary; real request 22 establishes one restart path. Existing deployments also
  benefit from the bounded startup window; it does not revive an already-lost host.
- **PostgreSQL persistence contract:** the Boolean field used a SMALLINT production
  column; queued JPA parent/child inserts also violated real immediate foreign keys.
  The entity uses numeric storage with a Boolean API, and DAO flushes binding,
  snapshot/operation and host-result publication in dependency order inside one
  transaction. Detach deletes children first. Real PostgreSQL binding creation and
  H2 tests using production FK declarations cover the correction, including rollback
  of flushed parents. Other database dialects still require their migration gate.
- **Action publication envelope:** internal dependency identity was present only in
  stage parameters, merged after ActionDB tried to associate the task. The command
  helper now copies the reserved envelope before atomic task/lineage publication;
  a reserved task without identity is rejected in that transaction. Old orphan
  requests were selected by their exact structured binding UUIDs and canceled through
  API; their rows were not repaired or associated heuristically. Helper stage tests,
  ActionDB rollback tests and actual provider tasks cover the production boundary.
- **Journal operation ownership:** CREATE intentionally retains one operation UUID
  across prepare-journal, initialize-journal and provision. The Agent treated each
  changed step hash as UUID misuse. It now validates explicit legal step transitions
  under the same epoch/snapshot/operation and retains exact-step idempotency. UPDATE
  provisions through the existing journal on its pinned host, rather than creating
  a new journal challenge. Filesystem tests cover forward transitions, mismatched
  identities, replay and old-epoch fencing; provider requests 102/104 exercised UPDATE.
- **Approved pre-install recovery:** a changed approved provider snapshot stranded a
  deployment before it published any INSTALL. Retry can adopt current approvals only
  when all original targets remain INIT and no current/historical request exists.
  The original plan stays immutable; progress stores current approved binding versions
  and previous attempts retain their approval lineage. Missing acknowledged request
  history never qualifies. A mock initially hid `plan_json` being non-updatable;
  the final regression uses a real deployment DAO, clears the persistence context,
  reloads, and replays the same retry UUID. This adds no second workflow owner.
- **Agent/package integration:** ZooKeeper/HBase shell calls used unsupported
  `environment=` instead of native `env=`; the mock accepted arbitrary keywords.
  A regression now calls the real shell wrapper while mocking only process execution.
  Fresh HDFS lacked `/apps`; provider provisioning now creates that controlled parent
  as the HDFS administrator after root validation and rejects unsafe existing parents.
  BIGTOP 3.3 HBase overrides its parent package list, so it must explicitly include
  the matching Hadoop client package (also inherited by 3.4). No local HDFS daemon or
  service is added by this client package declaration.
- **Shared hook versus HBase profile ownership:** replacing execution core/hdfs-site
  with the reduced provider client map removed hook-required local properties and
  could make shared hooks render provider settings into local Hadoop configuration.
  Provider maps now remain solely in the immutable bundle and HBase's dedicated
  profile; common hooks retain consumer-local maps. The HBase entrypoint also takes
  the package-created configuration parent into its existing root ownership before
  invoking the protected profile transaction. Symbolic-link parents are rejected;
  profile ownership checks remain strict. Real filesystem ownership-transition and
  planner regressions cover these boundaries, in addition to ongoing installation.

Focused commands executed against incrementally compiled classes (not stale
Surefire output):

```sh
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ServiceDependencyDAOIntegrationTest,AmbariCustomCommandExecutionHelperTest,ManagedDependencyRuntimePlannerTest,ManagedDependencyOperationDispatcherTest org.apache.maven.plugins:maven-surefire-plugin:test
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ServiceDependencyDAOIntegrationTest,ManagedDependencyOperationDispatcherTest,ManagedServiceDependencyCoordinatorTest,ManagedDependencyTaskResultProcessorTest org.apache.maven.plugins:maven-surefire-plugin:test
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyDeploymentCoordinatorTest,ServiceDependencyDAOIntegrationTest org.apache.maven.plugins:maven-surefire-plugin:test
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyRuntimePlannerTest,AmbariCustomCommandExecutionHelperTest org.apache.maven.plugins:maven-surefire-plugin:test
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-agent/src/test/python/resource_management -p TestManagedDependency.py -q
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestManaged*DependencyBigtop.py' -q
```

Results in command order: 57, 58, 44 and 26 Java tests passed; 17 Agent protocol
and 52 stack Python tests passed. These overlapping runs are not additive totals.
The 52-test Python run performs the package-parent ownership transition as root;
that individual test explicitly skips on an unprivileged runner. Expected child
process rejection traces in the concurrency test do not indicate a suite failure.
Earlier failed runs and private API receipts remain retained under the external
runtime-api directory and `/tmp/ambari-runtime-*.log`. Credentials, command payloads
and browser profiles are excluded from repository evidence. Subsequent acceptance
updates below supersede only the stated gates, not these historical failures.


The next installation attempts exposed further bootstrap dependencies. BIGTOP
service metadata includes package release (`3.3.6-1`), whereas the installed CLI
reports upstream software (`3.3.6`). Initial and retry planners now derive that
expectation from the same approved metadata; the original snapshot and failed
command remain unchanged. Preparation software/package expectations belong to
an epoch, while provider configuration/security fingerprints remain snapshot-owned.
A new epoch requires fresh observation and verification. Same-epoch command
changes and old-epoch rollback remain rejected by the Agent profile store.

HBase INSTALL now selects its installed stack links before invoking preparation,
using the existing selector with a process lock and the exact installation or
repository version. The version probe also exposed BIGTOP's layout defaulting to
unversioned `/usr/lib` when the isolated profile has no hadoop-env.sh. Binary
locations now come from stack_select into the HDFS subprocess environment; approved
provider XML remains isolated, and no local or provider shell configuration is
copied into the profile. An actual container invocation with those layout variables
and the managed profile returned Hadoop 3.3.6 successfully.

After a retry's publication failed, the deployment previously cleared its current
request ID and failed to reuse the prior INSTALL receipt for a subsequent retry.
It now retrieves that exact ID from its own durable history; the coordinator still
validates owner and terminal tasks. The missing-lineage UNRESOLVED state does not
become retryable. The original plan stays immutable, and retry UUID replay does
not create another epoch. This supplements R3/R5 without a second orchestrator.

Additional executed validation:

```sh
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyRuntimePlannerTest,ManagedDependencyCommandTest org.apache.maven.plugins:maven-surefire-plugin:test
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyRuntimePlannerTest,ManagedDependencyDeploymentCoordinatorTest,ManagedServiceDependencyCoordinatorTest,ServiceDependencyDAOIntegrationTest,ManagedDependencyOperationDispatcherTest,ManagedDependencyTaskResultProcessorTest org.apache.maven.plugins:maven-surefire-plugin:test
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyRuntimePlannerTest org.apache.maven.plugins:maven-surefire-plugin:test
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python:ambari-server/src/main/resources/stacks python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestHbaseBigtop*.py' -q
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestManaged*DependencyBigtop.py' -q
```

Results: 29, 86 and 10 Java tests passed, respectively; 61 HBase and 53 managed
stack Python tests passed. The initial HBase suite failed because its PYTHONPATH
omitted the real stack_advisor module and package expectations lacked the managed
Hadoop client. The final command supplies the module path and checks the complete
actual package lists. These overlapping results are not an aggregate count.


#### Structured client observations and completed cross-cluster HBase (2026-09-10)

This checkpoint supersedes the unfinished HBase rows above without discarding
failed receipts. Deployment `fbf55091-113d-49fa-81f9-337054efa4ef` is COMPLETE:
INSTALL 117, consumer preparation and verification at epoch 11, START 138,
service check 145/task 602. Both bindings are READY against provider cluster 3;
consumer cluster 2 retained its original local core-site/hdfs-site. The service
check ran on worker3 and used actual HBase SDK Put/Get with byte equality.

The original R3/R5/R6/R9 checklist exposed two additional integration defects:

- **P1: human-readable CLI output was treated as authoritative state.** HDFS
  stat/ACL/count/version parsing could confuse stderr diagnostics with permissions
  or treat an RPC failure as path absence. `ManagedDependencyClient` now obtains
  observations and performs bounded mutations through the installed Hadoop SDK;
  `managed_dependency_client.py` validates a versioned JSON schema, exact command
  envelope, operation and typed fields. Provider journal steps remain in Python;
  persistent workflow remains in the Server. Hadoop/HBase versions come from
  their VersionInfo APIs. ZooKeeper continues using its SDK; its helper now works
  with the actual HBase Commons CLI 1.2 classpath. The source rule is in AGENTS.md.
  A foreign or corrupt existing probe is never silently deleted or accepted.
- **P1: the deployment published an unsupported generic service-check command.**
  Real `AmbariCustomCommandExecutionHelper.validateAction` rejected SERVICE_CHECK,
  while mock publication accepted it. The coordinator now uses ActionMetadata's
  registered command, stores one exact service/request receipt at a time, and
  resumes remaining checks from the same attempt after restart. Missing check
  history is UNRESOLVED. HBase smoke checks use unique operation-owned tables and
  an SDK-written receipt; no substring of shell output establishes success.

Focused validation actually executed after partial javac:

```sh
mvn -B -pl ambari-agent -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dcommons-cli.version=1.2 -Dtest=ManagedDependencyClientTest,ManagedDependencyZkTest,ZkConnectionTest org.apache.maven.plugins:maven-surefire-plugin:test
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyDeploymentCoordinatorTest org.apache.maven.plugins:maven-surefire-plugin:test
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestManaged*DependencyBigtop.py' -q
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python:ambari-server/src/main/resources/stacks python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestHbaseBigtop*.py' -q
```

Results: 10 Agent Java, 20 coordinator Java, 54 managed Python and 61 HBase Python
passed. Java tests include actual local filesystem IO and a real ZooKeeper server;
Python tests exercise structured failures, stale/foreign identity, invalid types,
noise and recovery. Local filesystem tests do not establish DataNode or ACL
behavior; live epoch-11 verification provides the actual provider-HDFS proof.
Initial focused runs failed for a generated import typo and missing test imports;
those were corrected before the passing runs. An earlier javac attempt selected
embedded dependency sources; `-sourcepath ''` avoids compiling dependency .java
entries and compiles only the explicitly selected changed sources.

All nine API-submitted service checks completed: A HBase/HDFS/MAPREDUCE2/YARN/ZK
requests 146/148/149/151/153, and B HDFS/MAPREDUCE2/YARN/ZK 154/156/157/158.
These are real native commands. They do not imply that every preexisting Hadoop
service-check implementation has been converted away from textual checks.
Private acceptance receipts and SHA-256 manifests are under runtime-api, including
`overlays/structured-client` and `overlays/service-check-publication`. This is the
previously built RPM plus documented file/class overlays, not a fresh RPM build.

Provider lifecycle acceptance subsequently exposed a separate REST error-boundary
failure: an unconfirmed STOP was rejected by the domain but surfaced as an empty
HTTP 500. Centralized structured conflict mapping is being corrected. Confirmed
stop/restart, detach/data retention and real Kerberos completion remain open.

## User acceptance gate: cluster navigation replacement

The next frontend overlay addresses the reviewed route/context boundary and the
user-approved default landing behavior. Login restores an interrupted authorized
cluster route, otherwise the last user-scoped numeric cluster identity resolves
to its Dashboard. Without a valid preference, one cluster resolves directly and
multiple clusters require a modal selection; login does not open the cluster
management directory. The visible top dropdown switches to a target Dashboard and
links to Admin Cluster Management with an explicit cluster query. Admin validates
that query, never chooses the first of several API results, and returns Dashboard
links to the selected cluster. Existing global Admin screens remain accessible
without cluster selection. Admin logout preserves only valid user-scoped numeric
navigation preferences. Cluster detail information architecture remains pending
the user's separate interaction decision.

The user requested file replacement followed by their own validation. New
regression sources are present but intentionally not executed at this gate; only
frontend production build commands are run to generate deployment assets. No
commit, push, or RPM rebuild is authorized until the user accepts this replacement.
After acceptance, rebuild the RPM and use two completely independent clusters for
the next deployment scenario: each HBase uses its own local Hadoop and ZooKeeper.
The existing six-node environment still contains the earlier cross-cluster HBase
bindings; this frontend overlay does not convert that service topology. Historical
cross-cluster results above remain historical evidence, not proof of the new local
HBase acceptance scenario.

Replacement completed on 2026-09-10. Both `npm run build` commands completed with
exit code 0 in `ambari-web/latest` and
`ambari-admin/src/main/resources/ui/ambari-admin`. Admin dependencies were installed
with `npm ci --no-audit --no-fund` from the existing lockfile. Private overlay
`runtime-api/overlays/cluster-navigation/manifest.json` records source hashes,
15 deployed assets, and the original/patched Admin View JAR hashes. The main web
resources, extracted Admin View resources and Admin View JAR were replaced without
a Server restart. User browser acceptance is pending; tests were not run.

### Admin View 503 follow-up

User acceptance found HTTP 503 on the explicit Admin cluster URL. Replacing the
View JAR triggered ViewDirectoryWatcher registration. AmbariHandlerList attached
the already started server SessionCache before the replacement WebAppContext
started; Jetty AbstractSessionCache.initialize rejected changing its context.
WebAppContext's default startup handling recorded unavailability without throwing,
so the new handler was published despite its failed initialization.

AmbariHandlerList now starts the View with its own cache, then attaches the server
cache, matching the existing initial-startup sharing phase. Handler publication
follows successful startup; successful replacement removes the old handler, and
startup exceptions retain the previous mapping. View startup exceptions are no
longer swallowed. Concurrent registrations use a concurrent handler map. A real
Jetty lifecycle regression source was added for registration and replacement after
the server cache has started, but was not executed at this user-owned test gate.

Only AmbariHandlerList and its nested class were compiled with javac --release 17
using the existing server dependency classpath and deployed in a Server JAR overlay.
Ambari Server was restarted once; no RPM was built. The reported Admin URL now
returns HTTP 200 and its response bytes exactly match the built Admin index.html.
This narrow 503 recovery observation is not browser interaction acceptance. Private
receipts: runtime-api/overlays/admin-view-session-startup/manifest.json and
http-recovery.json. Commit, push and RPM remain pending user acceptance.

### Approved Admin management scope implemented

The user approved expanding Admin Cluster Management to six entries and requested
replacement followed by personal acceptance. Cluster Overview is the Admin landing
page, with all authorized clusters, installation/host health, host/service counts,
Stack version, Dashboard, Manage, and low-frequency rename/export/delete actions.
Create Cluster resumes owner-visible backend draft UUIDs or opens a new wizard.
Host Resources shows registered ownership and launches the selected cluster's Add
Host workflow. Cluster Permissions lists user/group grants, grants a selected role,
and revokes exact grant IDs with server read-back. The details page has five tabs
for basic information, services/hosts, permissions, request history and config/export.
Versions/repositories and remote registrations keep existing capabilities.

This follow-up addresses the review's frontend route/ownership and recoverability
requirements without introducing another installation orchestrator. API path
segments are encoded, authenticated authorization resources scope actions, stale
reads are dropped, and writes are reconciled against exact grants or cluster IDs.
Existing Add Host/Add Service/install workflows own mutations and recovery. The
operation viewer accepts the exact request ID from Admin history. User acceptance
of these interactions and the later two-independent-cluster local-HBase deployment
are pending; the current runtime topology has not been converted.

Admin management replacement builds completed with exit code 0: `npm run build`
in `ambari-admin/src/main/resources/ui/ambari-admin` and in `ambari-web/latest`.
No test runner or browser acceptance was executed. Private overlay
`runtime-api/overlays/admin-cluster-management/manifest.json` records source and
artifact hashes, 15 replaced frontend assets, and original/patched Admin View JARs.
The Server was stopped before replacing the View JAR and started afterward, avoiding
live ViewDirectoryWatcher replacement. No commit, push, or RPM rebuild occurred.
The user can inspect Admin root at
`/views/ADMIN_VIEW/3.1.0.0/INSTANCE/latest/#/clusters`.

### Chrome DevTools MCP functional acceptance (2026-09-10)

The user accepted the expanded UI and authorized agent-driven functional tests,
then requested a local Chrome DevTools MCP installation. This supersedes the
historical browser-test deferral above. Chrome DevTools MCP 1.9.0 is installed
outside the repository and configured in the local Codex user configuration.
Its stdio initialize, tools/list and browser tools were exercised against Chrome
on 127.0.0.1:19222. This running session uses a local stdio client; configuration
alone is not evidence that tools were dynamically loaded into the session.

Live browser checks uncovered three defects that unit-only validation had missed:

1. **Version ownership (original cluster-identity checklist):** the active Admin
   version list loaded status for the selected cluster but labeled it with
   `getClusterInfo().items[0]`, and links used unscoped routes. The list now owns a
   selected-cluster instance, cancels abandoned reads, binds status and links to
   that exact name, and can display the global repository catalog without a
   selected cluster. Failed status reads expose an error rather than an install
   action. The actual Angular version controller and Classic stack/upgrade route,
   plus baseline 06, were inspected. The deliberate multi-cluster difference is
   explicit selection and encoded cluster navigation.
2. **Draft REST transport (recovery / real integration):** the production
   ContentTypeOverrideFilter discovered class-level paths only. Nested JSON-only
   draft endpoints received text/plain and returned HTTP 415. Discovery now
   combines class and method JAX-RS templates using Jersey's own template parser.
   A real Jetty + Jersey test exercises JSON draft/cluster subpaths and the legacy
   text/plain root through the production filter. A Jersey resource test without
   the servlet filter could not detect this integration failure.
3. **Draft lineage (original review R3 / recovery checklist):** shared step navigation discarded
   the route query on mount and step changes, causing another draft UUID to be
   generated. It now retains the query. A real router/hook test covers mount,
   next, back and existing-cluster wizard context. Classic installer save/load
   behavior and baseline 07 were inspected. React deliberately uses a backend
   draft UUID instead of the legacy singleton installer state.

Live MCP evidence (real browser actions plus authoritative API read-back):

| Scenario | Observed result | Limit |
| --- | --- | --- |
| Login and navigation | Fresh admin session selected a cluster and entered its Dashboard; top menu opened Admin with the same cluster query | Dashboard metrics are unavailable without a queryable Prometheus source |
| Cluster/host inventory | Two clusters; six registered hosts; six host links matched API ownership; B filter showed three hosts; unassigned filter showed none | Existing runtime service topology still contains the earlier cross-cluster HBase dependency |
| Scoped user | Separate browser context for mc_a_viewer listed only mc_api_test, entered its Dashboard and received HTTP 403 for mc_api_b | Full role/group matrix and active-session revocation were not repeated |
| Permissions | Browser POST created privilege 52 on temporary cluster 52; confirmed removal used DELETE and GET returned an empty grant list | Group inheritance and response-loss fault injection remain separate gates |
| Rename/delete | Browser rename preserved temporary cluster ID 52; browser delete confirmed HTTP 404; original clusters remained IDs 2 and 3 with three hosts each | Empty-cluster deletion does not prove disk retention or dependent-provider deletion |
| Running-cluster delete | 22 observed STARTED components; entering the correct name still left Delete disabled | No destructive request was submitted for the running cluster |
| Request history | Twenty rendered request IDs and states matched the scoped API; links contained exact request IDs | Task detail content and all paging edges were not exhaustively repeated |
| Versions | A and B showed their own CURRENT status and cluster-specific links; global mode loaded the repository without a guessed cluster | Repository registration/install/upgrade mutations were not run |
| Create/resume/refresh | Draft beedab5e-a6c9-4e0d-b0dc-424e72b58685 retained its ID through Next, refresh and Admin resume; backend recovered the name mc_draft_browser_check and VERSION phase | Stopped before host provisioning; no spare hosts exist in this topology |
| Remote registrations | List route loaded and returned its empty state; no browser console error on the inspected Admin routes | No external cluster registration was created |

The saved-installation directory exposes a cluster name only after a cluster has
been created. Its current "Not named yet" fallback is imprecise for a named but
uncreated draft; displaying a separate server-provided proposed name remains a UX
gap. It must not infer cluster identity or reconstruct lineage from that label.

Executed focused validation (working directory is the named frontend, or repository
root for Maven):

```text
# ambari-admin/src/main/resources/ui/ambari-admin: 12 passed
npx --no-install vitest run src/tests/stackVersions.test.tsx src/tests/clusterManagement.test.tsx src/tests/navigation.test.ts
# ambari-web/latest: 29 passed
npx --no-install vitest run src/hooks/useStepWizard.navigation.test.tsx src/AppLoader.test.tsx src/screens/Directories/ClusterTasksRoute.test.tsx src/screens/Authentication/AdminViewRedirect.test.ts src/Utils/authNavigation.test.ts
# ambari-web/latest: 3 passed, including the two navigation cases above
npx --no-install vitest run src/hooks/useStepWizard.test.ts src/hooks/useStepWizard.navigation.test.tsx
# repository root: 7 passed
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ContentTypeOverrideFilterTest,AmbariHandlerListTest org.apache.maven.plugins:maven-surefire-plugin:test
# both frontends: exit 0
npm run build
# repository root: exit 0
git diff --check
```

Java production/test sources were first compiled with `javac --release 17
-sourcepath ''` against existing target classes and the private runtime dependency
classpath. Exact expanded argument arrays are retained in the private acceptance
record. This was partial compilation, not a clean full Maven build. Admin's current
`npm run build` invokes `tsc` without `-b`, so its successful exit is not a full
project-reference type-check claim. Main React runs `tsc -b` before Vite.

The initial React batch failed five cases because test DOMs were not cleaned up;
explicit cleanup corrected the test isolation. The first real View lifecycle test
failed because its ViewConfig fixture was missing; supplying the normal configuration
allowed actual Jetty startup/replacement to run. Final counts above reflect reruns.
One accidental Vitest invocation at repository root had no local Vitest and failed;
the documented frontend-directory invocation was then run successfully.

Private evidence: runtime-api/admin-management-acceptance and
runtime-api/overlays/admin-mcp-functional-fixes/manifest.json. Three compiled filter
classes, Admin assets/View JAR and main frontend assets were replaced. HTTP 200 and
SHA-256 were checked for all 15 frontend files. A temporary nested asset copy caused
a brief main-script 404 during replacement; the paths were corrected and only the
verified duplicate files were removed. The temporary cluster was deleted and both
created test drafts were released to IDLE. No RPM was rebuilt or commit published.
This does not close the next packaged two-independent-cluster/local-HBase acceptance,
real KDC callback, or broader restart/response-loss/failure-injection gates.

## 2026-09-10 publication validation checkpoint

The user accepted the UI, authorized functional testing, and then explicitly
authorized committing, pushing to the existing review branch, rebuilding Ambari
RPMs and redeploying. Earlier manual-approval gates above are historical.
Publication does not establish packaged acceptance of two independent clusters.
That deployment must give each HBase only its own cluster's HDFS and ZooKeeper.

All changed and new Agent/Server Java sources were partially compiled against the
local runtime dependency classpaths with `javac --release 17 -sourcepath ''`.
The DAO integration fixture initially failed compilation because its deployment
coordinator constructor lacked the newly required ActionMetadata; it now obtains
the real metadata from the test injector. Production and test compilation then
exited zero. This remains incremental compilation, not a clean reactor build.

Additional pre-publication commands, executed from the repository root:

```text
# 62 passed
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python:ambari-server/src/main/resources/stacks python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestHbaseBigtop*.py' -q
# 54 passed
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestManaged*DependencyBigtop.py' -q
# 10 passed
mvn -B -pl ambari-agent -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dcommons-cli.version=1.2 -Dtest=ManagedDependencyClientTest,ManagedDependencyZkTest,ZkConnectionTest org.apache.maven.plugins:maven-surefire-plugin:test
# 109 passed; BaseServiceTest is abstract and contributes no standalone case
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ManagedDependencyRuntimePlannerTest,ManagedDependencyDeploymentCoordinatorTest,ManagedServiceDependencyCoordinatorTest,ManagedDependencyLifecyclePolicyTest,ServiceDependencyDAOIntegrationTest,ManagedDependencyOperationDispatcherTest,ManagedDependencyTaskResultProcessorTest,BaseServiceTest,DeleteHandlerTest,ContentTypeOverrideFilterTest,AmbariHandlerListTest org.apache.maven.plugins:maven-surefire-plugin:test
# 1 passed; concrete subclass executes the shared BaseServiceTest contract
mvn -B -pl ambari-server -Denforcer.skip=true -DskipSurefireTests=false -Dtestcase.groups= -Dsurefire.argLine= -Dtest=ClusterServiceTest org.apache.maven.plugins:maven-surefire-plugin:test
```

The previously recorded frontend cases and builds were already run after their
last source changes. The Java batch includes the earlier seven filter/View cases;
counts must not add those twice. Runtime KDC, broader failure injection and the
new clean RPM deployment remain unverified at this publication checkpoint.

## 2026-09-10 independent-cluster RPM acceptance and partial corrections

The nine-topic publication was verified on origin/AMBARI-26654-multicluster-final
at a245919fdb1aba4ebaa987fde747e319a4dbbe88. Deploy's exact-commit workflow built
new native Ambari RPMs, reused the pinned stable Hadoop image, and completed a
six-node deployment on port 18081. The earlier port 18080 deployment was retained.
The six Agent packages and the Server package were queried through RPM's explicit
NAME/VERSION/RELEASE fields and matched release 1789033924.gita245919fdb1a.

The initial API inventory proved that only worker1..3 belonged to mc_local_a;
worker4..6 were registered and unassigned. API blueprint request 10 created
mc_local_b on worker4..6. Ordinary ADD_SERVICE requests 19 and 23 installed HBase
in A and B; exact service checks 22 and 26 completed SDK Put/Get verification.
No add-service command was added to deploy. Different scoped users received 403
for foreign-cluster reads and writes; a duplicate host attachment received 409
and request 10 under the wrong cluster received 404.

The effective HBase client configuration was obtained through the installed Java
SDK, writing bounded JSON receipts with exact operation UUIDs. The roots resolve
to hdfs://mc-local-a/apps/hbase/data and hdfs://mc-local-b/apps/hbase/data; HBase
cluster UUIDs differ. Hadoop HA RPC hosts and ZooKeeper bootstrap hosts resolve
only to each cluster's own nodes. The current ZooKeeper bootstrap configuration
is localhost on each HBase host, where a local member of the appropriate ensemble
runs. This verifies ownership, not loss of that individual bootstrap endpoint.

The first private configuration probe incorrectly demanded a fully qualified
REST hbase.rootdir and an empty dependencies response. A path without a URI scheme
legitimately uses fs.defaultFS; the dependencies resource returns unbound per-type
summaries even without binding rows. The corrected probe checks effective SDK
configuration, exact service-host membership and absence of binding IDs. The
HDFS summary remains conservatively unknown for this unqualified configuration;
it is not used as proof of local ownership. An initial diagnostic probe also used
a filesystem utility method absent from this HBase version; it was replaced with
Hadoop Path/FileSystem APIs before obtaining the successful JSON observations.

Chrome DevTools MCP exercised the packaged UI: select a cluster at first login,
enter its Dashboard, open Admin through the cluster menu with the same cluster
query, open B's Dashboard from the overview and reload without losing B's scope.
The metrics panels still have no configured Prometheus target; no metrics
availability claim is made.

Two real lifecycle defects were found and corrected with partial file replacement,
as the user requested. No intermediate RPM build was performed for these fixes.

### P1: Registry DNS supervisor can recreate a stopped child

Source: ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/YARN/package/scripts/service.py:88; regression: ambari-server/src/test/python/TestYarnBigtopLifecycle.py:822.

B's stop request 27/task 126 remained IN_PROGRESS after its stop script returned.
A /proc observation found an orphan yarn-owned jsvc process. The YARN service
helper stopped the daemon before its root supervisor, allowing a replacement child
to outlive the supervisor. The supervisor now stops before its daemon; exact PID,
UID, command-token and process-start identity checks remain in safe_process. The
regression models a supervisor which replaces a stopped child and verifies no
process remains. All 192 TestYarnBigtop cases passed. Seven installed/cache script
copies were replaced and SHA-256 verified. Request 27 was explicitly aborted and
retained; retry 28 completed, and A's HBase check 29 completed while B was stopped.
After a successful B restart, a fresh stop cycle 33 completed with no surviving
jsvc process and A's HBase check 34 completed during that outage. This second
cycle covers the live supervisor case, beyond cleanup of the original orphan.

### P1: Common hook assumes absent HBase configuration on unrelated commands

Source: ambari-server/src/main/resources/stack-hooks/before-ANY/scripts/params.py:221 and shared_initialization.py:68; regression: ambari-server/src/test/python/stacks/stack-hooks/before-ANY/test_before_any.py:37.

B's restart request 30 failed client INSTALL tasks 159..162 on worker5. The
before-ANY hook saw HBase masters in clusterHostInfo and initialized an HBase
directory using an UnknownConfiguration user, although those commands did not
carry hbase-env. Service-specific user initialization now uses the current
command's configuration scope. Generic user/group initialization still uses the
server-provided identities, and HBase initialization still runs when its
configuration is supplied. This removes the cluster-wide inventory inference
without inventing a default HBase user or copying unrelated service credentials.

The hook regression executes the real params and hook with absent/present HBase
configuration while the cluster inventory contains a remote HBase master. The
existing resource test's OS-distribution fixture was extended to cover assertions
as well as script execution, avoiding unrelated filesystem-mock exhaustion. Two
hook test methods passed, including both scope subcases. Fourteen installed/cache
script copies were replaced and hashed. Recovery request 31 completed all tasks,
and B's HBase check 32 completed. Request 30 remains recorded as FAILED.

Executed commands (repository root):

```text
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python:ambari-server/src/main/resources/stacks python3 -B -m unittest discover -s ambari-server/src/test/python -p 'TestYarnBigtop*.py' -q
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH=ambari-common/src/main/python:ambari-server/src/test/python:ambari-server/src/main/resources/stacks python3 -B -m unittest discover -s ambari-server/src/test/python/stacks/stack-hooks/before-ANY -p 'test_before_any.py' -q
```

The deploy command was run from the deploy checkout:

```text
.venv/bin/ambari-test run --profile .ambari-test/multicluster-local-a245919fdb/profile.yml --repository /jialiangc/bigdata/prjs/ambari-multicluster --commit a245919fdb1aba4ebaa987fde747e319a4dbbe88 --output json
```

Private evidence is under runtime-api/independent-a245919fdb: initial inventory,
API operation receipts, SDK JSON, local-dependencies.json, isolation-result.json,
browser-navigation.json and the registry-dns-overlay / hook-scope-overlay
manifests. The second complete lifecycle cycle passed: stop request 33 (22 tasks),
A read/write check 34, restart request 35 (35 tasks), and B read/write check 36.
Ambari Server was then restarted; cluster IDs 2/3, six host memberships and exact
completed requests 19/22/23/26 were retained. The six authorization/ownership
assertions passed again. Actual browser reauthentication returned directly to B's
Dashboard with cluster ID 3. Private commands were run with deploy's .venv/bin/python:
acceptance.py inspect/create-b/isolation, the sequential ops.add_service calls in
add-hbase.log, probe_sdk.py, verify_local.py, lifecycle-after-hook-fix.log's
change_services/check_hbase/verify_outage sequence, and restart_server.py.

At this source publication checkpoint both partial corrections have passed their
focused tests and live recovery checks. A final native RPM containing them is the
remaining packaging gate. Real KDC callbacks and broader response-loss fault
injection remain outside this non-Kerberos independent-cluster run.
