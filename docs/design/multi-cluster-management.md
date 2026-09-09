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

# Unified multi-cluster management

Status: Foundation and directory source checkpoints accepted; managed dependency
dispatch, recovery and security integration in progress. Executed validation is
pending the final integrated gate.
Detailed interaction contract: [multi-cluster-interactions.md](multi-cluster-interactions.md).
Baseline: PR 4208, commit `a62fe4959dc948210d844295a097a3311321dea6`.

## Product and identity

One Ambari server and database manage independent Cluster records. The runtime
identity remains Cluster -> Service -> Component -> Host. A service deployment
is identified by `(cluster_id, service_name)`. Cluster names remain compatible
with existing REST paths; persisted relationships use numeric cluster identity.
Renaming a cluster must not change dependency identity. A host belongs to zero
or one runtime cluster. A server/database outage affects all managed clusters;
this feature does not provide process-level high availability.

The console has two primary entry points: all authorized clusters and all
authorized service deployments, filterable by service type. Each service row
shows its owning cluster and provider relationships, and links directly to that
deployment's existing configuration and operations. Selecting a deployment
establishes context in its URL. Users can compare deployments without changing
a global selected-cluster preference.

## Frozen foundation contracts

1. Preserve `/api/v1/clusters/{clusterName}/...` APIs. Unified lists aggregate
   only authorized resources. Direct API calls remain subject to server RBAC.
2. Canonical UI routes use `/clusters/:clusterName/main/*` with the existing
   main route structure and an encoded cluster-name segment. `/clusters` and
   `/services` are the unified directories. A keyed runtime provider contains
   cluster-local state; scoped navigation helpers preserve the URL identity.
   Unscoped legacy links resolve automatically only for one authorized cluster;
   multiple clusters require explicit selection while preserving the target.
3. Cluster state belongs to a route lifetime and authenticated user. Requests,
   polling, tasks, metrics, permissions, configurations and websocket messages
   must never update a different context. Persistent wizard keys additionally
   distinguish wizard type and installation draft or cluster identity.
4. Enforce host uniqueness in the database and ordinary write paths. Upgrade
   detection of existing duplicate memberships fails with a useful remediation
   message; migration does not choose an owner or discard data implicitly.
5. Creating a cluster never deletes another cluster or shared repository record.
   Resume verifies the same draft and target, retains completed operation
   checkpoints, and reconciles uncertain create responses before retrying.
6. A dependency is an explicit binding between consumer and provider service
   identities. The provider owns its lifecycle, configuration and data. Consumer
   operations never cascade to provider stop/delete or data removal.
7. The existing user authorization API supplies `resource_type` and
   `cluster_name`. Preserve these records. AMBARI scope grants global authority;
   CLUSTER scope only applies to the requested cluster. A cluster administrator
   is not an Ambari administrator. Browser storage is not authority.
8. Server websocket delivery must enforce resource authorization. Shared
   broadcast subscription plus browser-side filtering is insufficient. Scoped
   delivery must not leak aggregate map entries, task output or unknown-identity
   messages. Existing API destinations are retained with per-recipient server
   authorization/projection if proven by broker-level evidence; a changed wire
   contract requires a separate review before frontend consumers change.
9. Discovery and mutation require permission at both relevant resource boundaries.
   Configuration snapshots expose an allowlist of client settings, never complete
   provider configuration or administrative credentials. Unsupported security or
   version combinations are explicit validation errors before provisioning.

## Interaction flow

The landing page lists authorized clusters with installation state and recovery
entry points. A service-type view lists deployments with cluster and dependency
columns. Search/filter state may be encoded in query parameters. Each row has
explicit deployment links; operation dialogs repeat the cluster and service.
Cluster-scoped hosts, alerts and tasks remain available inside each cluster.
Unified views must either provide explicit filtering or link to these real
scoped views; they cannot imply that unimplemented bulk operations work.

Create Cluster starts a distinct draft. Review shows the target name, selected
repositories, exclusive host allocation and dependencies. Failed preparation or
installation shows the failed action and a retry/resume entry for that target.
Refresh restores only the matching draft/cluster. Completing or cancelling one
flow cannot clear another cluster's operation state.

Ordinary cluster creation must persist a creation request/draft UUID and
server-verified creator identity atomically with the Cluster row. This lets a
client recover from a lost create response without assuming that an existing
same-name cluster belongs to its draft. Subsequent configuration writes cannot
serve as ownership proof because a crash may occur before those writes. Existing
clusters with no creation identity remain valid; they are not implicitly adopted
by a new draft. The same owned draft/name resolves the same numeric Cluster ID,
while a conflicting identity produces a useful conflict. Exact optional API
field names and persistence methods are reviewed before the wizard consumes them.

Provider selection shows authorized compatible services and owner clusters.
Binding preview describes endpoint/config versions, independent data/znode
paths, authorization requirements and affected consumers. Updates use an
explicit preview/apply sequence; a provider change marks prior snapshots stale.
Unavailable, forbidden, incompatible and provisioning-failed states must be
recognizable and have appropriate retry/recovery behavior.

## Dependency security and lifecycle decisions

HBase bindings must not reproduce the baseline script's use of the HDFS
administrator identity on consumer hosts. Provision independent root and WAL
paths on provider NameNodes with provider-local authority. The consumer uses
its HBase identity and allowlisted client configuration; it receives no provider
administrator keytab. Existing unbound local HBase deployments retain their
local workflow. Managed bindings have an explicit version and recoverable
provisioning state, and provider mutations expose stale snapshots and impact.
Cross-realm and mixed-security combinations are rejected until their identity
and trust contract is implemented and tested. Same-realm support requires
verified identity issuance, local-name mapping, authorization and runtime
connectivity; endpoint syntax alone is insufficient.

## Binding identity and compatibility review

Use an immutable generated UUID binding ID and composite foreign keys to existing
`clusterservices(cluster_id, service_name)` for consumer and provider. The
service name is a fixed service type in this deliverable; a cluster rename
preserves its numeric ID. A new numeric service-ID migration is not required.
Provider deletion is restricted while references exist. Consumer detach/delete
must invalidate or cancel pending binding work and preserve provider data.
Already-dispatched work checks binding/version identity so stale operations
cannot target a later service or binding with the same display names.

Namespaces must be independent per binding incarnation. Recreating a service
must not silently adopt data preserved by a deleted binding. Provider-side
ownership markers and path validation reject namespace ownership conflicts;
retries for the same binding and snapshot are idempotent. Provisioning commands
are reserved server-validated operations and cannot accept arbitrary caller
paths or credentials through generic custom-command APIs.

Provider preparation is serialized on one durably pinned action host for the
binding's lifetime, including retries, updates and invalidation. A protected
local journal and an exclusive lock cover the entire remote operation. Neither
an operation epoch nor a terminal Ambari task status alone fences an old process.
An uncertain host/process or missing journal blocks reassignment and new mutation
with an explicit recoverable state. Automatic preparation failover is not claimed;
HDFS client HA endpoint resolution remains a separate capability.

Journal bootstrap uses the existing authenticated agent task/result channel.
PREPARE writes a protected random pending challenge without provider mutation.
The server persists that exact result and grants one initialization authorization
for the binding. INITIALIZE matches the pending challenge and atomically promotes
it into the final journal under the binding lock. The server accepts its exact
result before dispatching any provider mutation. A changed challenge after an
authorization was issued, or missing/corrupt state after initialization, cannot
obtain a replacement authorization. Exact retries retain their operation identity;
this challenge exchange is not a public capability or a substitute for RBAC.

Provider namespace markers bind stable service identity, paths and ownership.
Changing an approved client snapshot must not change that namespace identity or
make its marker conflicting merely because configuration fingerprints changed.
HDFS preparation checks extended/default ACLs as well as mode and owner before
accepting a private namespace; inherited ACLs must not expose newly created paths.
See the [Hadoop 3.3.6 permissions guide](https://hadoop.apache.org/docs/r3.3.6/hadoop-project-dist/hadoop-hdfs/HdfsPermissionsGuide.html).

Initial compatibility targets the active BIGTOP/3.3.0 stack and an explicitly
supported client feature set. Compare resolved repository version metadata,
not database row-ID equality: equivalent mirror definitions may have different
row IDs. Unsupported versions or client features produce explicit validation
outcomes. Minimal snapshots are allowlisted and versioned, never whole provider
configuration copies.

Same-realm secure consumers require distinct effective HBase identities and
private root/WAL/znode authorization. Different host principals that all map to
the same short user `hbase` do not establish separation between deployments.
Identity issuance, provider local-name mapping and actual data access must be
validated before enabling that combination; unsupported secure modes fail early.

Managed consumers use distinct effective HBase users even in simple-auth mode,
so ordinary filesystem ownership does not grant every deployment the same user.
Simple authentication does not provide Kerberos security against impersonation.
A server-derived draft user plan is persisted as ordinary HBase configuration
and survives materialization unchanged, preserving draft/live compatibility.
Both dependency types use the same consumer user. New managed bindings target
fresh INIT/uninstalled HBase; automatic migration of installed local HBase data
to a new namespace is not part of this deliverable.

## Provisioning order and bootstrap boundary

A binding must distinguish provider preparation from complete consumer readiness.
READY requires a real non-empty HDFS write/read from every HBase daemon host and
ZooKeeper session/namespace validation where bound. Those checks require client
packages, rendered configuration and, for secure consumers, issued identities.
Requiring READY before any consumer installation would therefore prevent a new
HBase-only deployment from ever becoming ready.

The durable workflow first approves a versioned snapshot and prepares the provider
namespace. Consumer installation may then install client packages, render the
approved snapshot and perform host-specific checks. START/RESTART requires complete
current readiness; INSTALL requires current provider preparation and snapshot
approval. A failure retains its phase, binding incarnation, operation epoch and
per-host progress so retry resumes only the relevant work. Adding a daemon host
requires that host's current validation before it may start. A new desired snapshot
cannot inherit successful validation from an earlier version.

Client installation uses an approved preparation plan before an exact operating
system package version is known. The agent then reports the installed package
version, the actual Hadoop or HBase client software version, and rendered
configuration from the selected profile. The server checks software compatibility
against trusted stack/VDF metadata and persists that task's immutable observation.
Subsequent verification must match the observation and its preparation lineage,
and recheck the package and configuration around the real connectivity probes.
Copying an observed package string into an expected field is not itself evidence
of compatibility. Preparation does not establish readiness. A normal START that
renders configuration reuses the current approved plan and existing readiness;
it cannot manufacture a new installation observation or operation generation.

New-cluster provider discovery and preview occur before the consumer Cluster row
exists. They use the authenticated user's explicit creation draft and its resolved
stack/repository/security descriptor, plus provider authorization. Planned stable
provider/binding identities are stored with that draft; materialization waits for
the exact draft-owned consumer Cluster and HBASE service rows. Existing INIT-service
and draft preview share validation rules. A live Cluster object cannot be the only
input to preview validation. The endpoint layout is finalized at the API checkpoint.

Service selection also precedes repository record creation in the installer.
Draft version resolution therefore uses trusted selected VDF metadata without
publishing a repository as a preview side effect. The later repository row must
produce the same semantic compatibility facts. Missing security-plan inputs have
an explicit editable recovery path before final approval; they cannot silently
pass validation or make it impossible to reach the configuration step.

Initial preview returns a fresh UUID binding identity and server-derived namespace
paths. Later draft, service-plan and live previews accept that same optional
`binding_id`, preserving its namespace and binding-specific provider fingerprint.
Revalidation must not allocate another identity for an already reviewed selection.
An active or fenced identity uses its existing binding/operation recovery path;
ordinary preview does not reserve or reclaim it.
Create accepts that identity and expected provider fingerprint, revalidates the
complete authorized specification and derives paths/configuration itself. The UUID
is stored as a portable 36-character key. Repeating it with the same immutable
specification returns the existing operation; a different specification conflicts.
No database reservation or secret preview token is needed: create never trusts
caller-derived paths or configuration. Preview provisions nothing. Provider identity
is immutable within a binding; replacement requires an explicit new binding and
namespace after safe detachment of the old one.

Secure HDFS and ZooKeeper selection produces one consumer mapping for the complete
selected binding set. The approved integration design extends existing preview
and creation routes with a bounded one/two-item plan while retaining legacy
single-item requests. All items keep their own stable binding and operation IDs.
The server authorizes every parent and validates every fingerprint before a single
DAO transaction persists all bindings, snapshots, operations and provider intents.
Only then may independent provider scheduling start. Exact all-item replay returns
existing operations; partial or mismatched prior creation conflicts without
silently creating a missing peer. This integration remains subject to source and
runtime acceptance; it does not introduce a new group lifecycle or tenant record.

Provider discovery must also use the authoritative security producer so supported
secure providers remain selectable. Discovery creates no reservation or approval;
final preview recalculates the whole selected plan. Missing plan inputs are distinct
from unsupported policy, version or realm combinations. The browser cannot waive
security-proof failures. A new binding type on a genuinely fresh INIT service is
classified separately from an already managed type; an installed local service
still requires an explicitly supported data-migration flow.

Adding HBase to an existing cluster uses an owned ADD_SERVICE workflow and exact
saved revision for SERVICE_PLAN discovery and preview before HBASE exists. Once
the same workflow creates fresh INIT HBASE, it switches to live preview and
retains that materialization intent across refresh. A pre-existing installed
HBase service is not adopted by a new plan. If a custom repository cannot be
resolved until materialization, the first live preview supplies concrete values
for review; it cannot require those still-unapplied values to exist already.
Final approval and dispatch enforce effective controlled configuration. Save the
exact create body and operation ID before mutation, and reconcile an existing
operation before previewing again after a lost response.

Secure HDFS policy inspection describes the provider's effective supported
auth-to-local rules and managed default realm independently of a consumer. A
separate symbolic proof maps the exact unique consumer principal pattern through
that policy without requiring hosts to be assigned. Both fingerprints belong to
the approved HDFS snapshot; actual credential and host coverage remain later
readiness evidence. Evaluation must not read or modify process-global Hadoop
rule/default-realm state. A ZooKeeper binding proves its own provider policy;
overall HBase storage validation is a separate live-plan requirement. Independent
provider selectors must not need access to an unselected provider.

Secure snapshot schema 2 separates the consumer's generated mapping profile,
the provider's policy fingerprint and the exact principal-to-provider pair proof.
Preview and approval compare the full snapshot fingerprint as well as provider
and consumer fingerprints. Prospective Add Service calculation and later live
calculation use the same semantic identity; workflow provenance and placement do
not change that identity. Raw provider rules and krb5 templates stay in transient
server inputs. Consumer-generated rules enter only the private stored snapshot
and authorized HBase task profile; public summaries expose fingerprints.
An old secure snapshot without typed proof requires a new preview and approval.

Prospective Kerberos calculation uses a detached raw composite descriptor and
the authoritative current-plus-planned service set. SERVICE_PLAN resolution carries
and rechecks the exact owned workflow revision, including other services selected
in the same workflow. Live SERVICE uses the actual materialized topology. Provider
stock-template provenance comes from stack metadata rather than the effective
custom template being checked. Ordinary cluster-scoped
descriptor and credential calculations use an approved persisted live plan.
The latter reconstructs one consumer identity from its complete HDFS/ZooKeeper
binding set and compares the final generated mapping profile with the approved
snapshot, including after Stack Advisor recommendations. Missing or inconsistent
active security evidence cannot fall back to the original shared HBase identity.
Consumer-service and binding locks protect a coherent snapshot read; provider
reachability is not part of this metadata calculation. Installation and identity
issuance can precede connection readiness. Legacy insecure snapshots retain their
read compatibility, while unsupported security transitions fail explicitly.

Stack Advisor accepts an explicit managed dependency plan tied to the owned
draft, saved Add Service revision, or active service binding. Server authorization
resolves the exact provider/binding identities and approved fingerprints into
request-local facts. Only the selected HBase dependencies are satisfied externally;
local HDFS/ZooKeeper required by another service remain part of that service's
plan. Caller-supplied trust flags never reach the advisor. Target and host scope
are checked before reads, including cache access, and auto-completion requires
the same configuration-read authority as the normal configuration API. Ordinary
unmanaged advice may still address another supported Stack release.

Provider core-site/hdfs-site values belong to HBase-specific execution
configuration and protected client profiles. They must not overwrite a consumer
cluster's local HDFS configuration when local HDFS is retained for Hive or another
service. Ordinary desired configuration contains the consumer-owned HBase
root/WAL/user/znode/principal values. Server-owned HBASE command decoration supplies
the approved provider client maps before agent parameter resolution. Consumer-local
Kerberos mappings remain separately approved and preserve local service mappings.

Managed ZooKeeper namespaces use `/ambari-managed-hbase/<binding_uuid>` as a
consumer-owned container, with HBase rooted at its `/hbase` child. The provider
owns CREATE/DELETE on the shared parent and a separate immutable binding ledger;
consumers can manage their own descendants but cannot create or delete siblings.
Provider credentials remain on provider hosts. Uncertain container creation
requires authenticated consumer verification before readiness; no automatic ACL
repair or provider administrator addition to `hbase.superuser` is allowed.

This extra container is required by actual upstream behavior: ZooKeeper checks
parent CREATE permission before NodeExists, while HBase attempts base creation
on master startup and normalizes its own subtree ACLs. The layout allows native
HBase behavior while keeping shared-parent authority with the provider. See
[ZooKeeper creation checks](https://github.com/apache/zookeeper/blob/release-3.5.9/zookeeper-server/src/main/java/org/apache/zookeeper/server/PrepRequestProcessor.java)
and [HBase namespace initialization](https://github.com/apache/hbase/blob/rel/2.4.13/hbase-zookeeper/src/main/java/org/apache/hadoop/hbase/zookeeper/ZKWatcher.java).

Client snapshots become the desired HBase dependency profile only through an
explicit authorized binding approval or update; provider core-site/hdfs-site maps
remain private to HBase execution. Internal snapshot/provenance fields and bound
root/WAL/znode identities cannot be overwritten through generic configuration APIs
to bypass validation. Ordinary editable HBase settings retain their existing
configuration workflow. Provider client metadata deliberately exported through an
approved binding is distinct from unrestricted provider configuration access;
provider details and dependency discovery remain subject to their own permissions.

## Creation identity and recovery

Creation submits `Clusters/creation_draft_id` and records that UUID together with
the authenticated creator's stable user ID in the same transaction as the Cluster
row. A server-only created-cluster ID on the retained workflow row records
consumption independently of user-editable phase/payload, even after cluster
deletion or draft release. This internal association does not alter the user
workflow revision or phase. The owned draft is locked during creation; retries reconcile that identity
without reinitializing an existing cluster. Tokenless clients retain ordinary
creation behavior. `GET /persist/scopes/drafts/{uuid}/cluster` returns only the
associated cluster ID/name, or not-found, after checking the draft's owner and
creation authority. A successful name lookup alone cannot establish ownership.

`GET /persist/scopes/drafts` lists only the current stable user's active creation
draft summaries, without saved values. Summaries include authoritative associated
cluster identity when creation has committed. This supports recovery after closing
the original browser tab. Resume explicitly selects the draft UUID in the URL;
Create starts a new draft. Normal cluster responses do not expose creation tokens.

New repository definitions include their complete validated initial OS/repository
settings in the first committed creation. Publishing defaults and then applying
wizard edits with a later PUT allows another cluster to adopt a record that is
about to change. Existing identical definitions are reused; conflicting shared
settings require explicit resolution. Existing global repository administration
retains its own authorized editing flow.

## Deletion concurrency boundary

Collection deletion acquires every requested cluster write lock in numeric order,
re-resolves current service objects by name and validates all removability and
active dependency references before removing the first target. Binding publication
uses the corresponding canonical parent read locks through its transaction commit.
Transactional cluster deletion retains its runtime cluster lock in the existing
local transaction interceptor until commit or rollback, including nested calls.
Manually begun transactions bypassing that interceptor are rejected for these
transactional cluster deletion methods. Normal direct service deletion retains
its lexical lock through the existing service deletion transaction.

This source boundary does not establish recovery from an arbitrary database commit
failure after existing in-memory component mutations. Final validation must verify
actual lock release, commit/rollback behavior and supported failure recovery;
written concurrency fixtures are not executed evidence.

## Metrics isolation and compatibility

Datasource authorization alone does not constrain the series returned by a
shared monitoring server. The accepted implementation contract restricts cluster
users to verified Ambari-managed VictoriaMetrics query/query_range and batch
operations with a server-injected immutable `ambari_cluster_id` filter. Discovery
and scraping preserve existing name labels while adding numeric identity; caller
filter parameters and exporter labels cannot override that identity.

Unproven custom backends and metadata endpoints return an explicit unsupported
scope result. Existing query charts remain usable without autocomplete or target
metadata. Global settings administrators retain their explicit generic datasource
management access. Historical series without numeric identity have no cluster-name
fallback, since a deleted cluster's name can be reused. This is a deliberate
compatibility limitation; it does not delete historical data.

VictoriaMetrics can ignore extra filters on label APIs through
`search.ignoreExtraFiltersAtLabelsAPI`; query filtering therefore cannot establish
metadata isolation by itself. See the [VictoriaMetrics documentation](https://docs.victoriametrics.com/victoriametrics/index.html).
Actual source, effective version and isolated query evidence remain review gates.

## Dependency-ordered execution and ownership

- Stage 0: separate Sol evidence for server foundations, frontend foundations,
  and managed service dependencies; Astra inspects critical sources and freezes
  contracts before source implementation.
- Stage 1: server isolation and membership; frontend routing/context and safe
  creation. Independent writers own disjoint files. Security/interface changes
  require an Astra checkpoint before releasing consumers.
- Stage 2: unified browsing and real deployment navigation; desktop/mobile and
  asynchronous isolation evidence. API dependencies must already be accepted.
- Stage 3: persistence/API, provider-side provisioning, stack client integration,
  and dependency UX in dependency order. No unreviewed contract consumers.
- Stage 4: integration, recovery, RBAC, lifecycle and browser validation followed
  by an Astra review of actual diff and evidence against the pinned baseline.

Each worker supplies midpoint source/test diffs and fixes ordinary implementation
issues. By the user's latest instruction, all compilation and test execution
are deferred until all source implementation is integrated. Interim Astra PASS
or REWORK concerns code/contracts and permits further implementation only;
execution remains pending. The final combined gate compiles and executes the
focused/integration/runtime checks, with targeted correction and rerun for
failures. Completion statements alone are not evidence. The user later selected Luna with xhigh reasoning for execution. At most three
workers execute concurrently and one writer owns each shared file. Full logs remain outside tracked source.

Final local validation uses the deploy project's documented prebuilt images and
build caches where available. Run necessary focused regressions and isolated
deployment/browser scenarios after integration; do not run full unit suites
locally. Broader suites remain for future submission CI, which has not run here.

## Acceptance matrix

| Scenario | Required evidence | Status |
| --- | --- | --- |
| Create B preserves A and repository records | API/wizard tests; retry after partial B creation | Pending |
| Several HBase deployments have real scoped actions | Route/action tests and browser evidence | Pending |
| Tabs, delayed responses, polling and events isolate state | Adversarial asynchronous tests | Pending |
| Cluster RBAC covers lists, direct calls, events and dependencies | Server permission denial tests | Pending |
| One host cannot join two clusters | Concurrent database/write-path test and reload | Pending |
| A/HBASE uses B/HDFS without local HDFS servers | Isolated provisioning/runtime evidence | Pending |
| Consumer removal preserves providers and data | Lifecycle and provisioning regression tests | Pending |
| Provider version/stale/error/retry behavior | Binding API/stack/UX regression evidence | Pending |
| Existing single-cluster installation migrates safely | Schema and legacy route regression tests | Pending |
| Desktop/mobile recovery and dependency navigation work | Browser captures and action/network assertions | Pending |

## Submission and validation plan

The feature is tracked by [AMBARI-26654](https://issues.apache.org/jira/browse/AMBARI-26654).
At the user's explicit request, four source-reference commits were published to
[the contributor reference branch](https://github.com/JiaLiangC/ambari/tree/AMBARI-26654-multicluster-reference)
at `8bf556b6ce94b350b3c3b12e15a7882d07bd19f7`. That fixed reference predates later
integration corrections and has not been compiled or tested.

The user subsequently requested prompt batch submission of completed work,
superseding the earlier topic-splitting plan for continuation changes. Reviewed
source batches accumulate on `AMBARI-26654-multicluster` in an isolated continuation
worktree. Unfinished source remains in the pinned writer worktree. Each batch uses
the same JIRA key, includes its focused regression source and records outstanding
validation. Compilation and focused execution remain deferred until all source is
integrated, as explicitly requested; publication does not establish test success.

Atomic one/two-binding DAO creation and immutable CREATE replay have passed source
review. Secure descriptor resolution now calculates the complete approved provider
selection against the owned workflow revision and selected plus installed services.
Their focused regression sources are included; execution remains deferred.

Complete dependency-plan API/advisor integration has also passed source review: all
provider parents are authorized before binding lookup, secure selections share one
authoritative calculation, and creation preserves exact operation replay. Binding
status includes current preparation request/task lineage for deployment recovery.

The remaining work covers executable lifecycle retry/update and credential gates,
completed deployment recovery and operation interactions, and integration evidence.
No pull request, merge or deployment to existing live environments is authorized.
Isolated local deployment and focused validation are authorized after source
integration, using the deploy project's documented prebuilt images and reusable
build outputs.
