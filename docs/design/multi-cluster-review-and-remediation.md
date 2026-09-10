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

Current worktree validation: **205 Java tests, 91 frontend tests and 31 Python tests
passed**. Java compilation passed. Full TypeScript checking still reports 49
baseline errors (52 at reviewed HEAD, no new normalized diagnostic). Real
independent clusters, KDC, browser/broker and process-kill acceptance remain open.

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
