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

Status: Target design, not an implementation acceptance statement. Current code
findings, test results and implementation order are maintained in
[multi-cluster-review-and-remediation.md](multi-cluster-review-and-remediation.md).
The interaction contract is consolidated below; historical worker checkpoints
are not product requirements or current execution instructions.
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

## Interaction requirements

### Finding and comparing deployments

Offer two discoverable global destinations: Clusters and Services. Both retain
search and filter choices in the URL so Back returns to the same comparison.
Service type is a filter, not a global cluster switch. Opening a deployment uses
an explicit cluster route and leaves other tabs independent.

Cluster rows show the name, stack/version, install or operation state, and
available scoped entry points. Create Cluster appears only for global cluster
creation authority. An incomplete cluster offers Resume with its actual state.
Do not show a misleading healthy state when status has not loaded.

Service rows identify the service deployment and owning cluster together.
For HBase, show storage and ZooKeeper providers, including the owner cluster and
binding state. Configuration and tasks are direct real links. Lifecycle actions
use the existing authorized workflow, with the target cluster/service repeated
in the dialog and progress view. No decorative controls or implicit multi-cluster
bulk restart. A failure loading one cluster's deployments leaves other authorized
results usable and identifies the failed scope with a targeted Retry action.

Desktop uses the existing dense table, sorting and pagination. At narrow widths,
keep deployment identity, status and primary action readable; secondary details
may stack. Do not require horizontal scrolling to identify the target or recover
from an error. Controls need visible labels, keyboard focus and accessible names.

### Staying oriented inside a cluster

Display an explicit cluster identity in the operational shell, with a clear
return to the global cluster/service directory. Preserve existing cluster-level
service, host, alert, configuration and background-task screens. Navigation,
config saves, restart links and task drawers must retain the route's cluster.
A chooser for an old unscoped link explains which destination will open after
selection and retains that destination. A forbidden or removed target shows a
useful access/not-found state; it never silently opens a different cluster.

Keep global Ambari administration and Views distinct from deployment context.
A cluster administrator must not see controls implying global authority.

### Creating and recovering a cluster

Start a distinct draft with its own URL and persistence identity. The wizard
shows the target cluster name and current step throughout. Preserve entered data
when validation fails. Report a field-specific name collision before mutation,
with clear Change name and authorized Open existing cluster choices.

The cluster directory lists the signed-in user's unfinished creation drafts and
offers explicit Resume links, including after a browser restart. Creating another
cluster starts a fresh draft. Restore blocks editing until its scope and revision
are loaded; a conflict discards queued obsolete writes before reloading. Missing
credentials identify the affected step and field and prevent the relevant operation
until re-entered, while non-secret edits remain available.

Review presents hosts, repositories and dependencies for the new target. Do not
ask users to approve implicit deletion or global cleanup: creation does not do
those operations. Shared repository settings must not be overwritten incidentally.
Selection and review preserve existing validation semantics while making errors
and corrective actions understandable.

During preparation/installation, show the actual current operation and completed
progress. Failure identifies the failed operation, what has already succeeded,
and whether Retry or Resume is available. Retry reconciles uncertain server
responses and does not repeat another cluster's work. Refresh returns to the
same target and step. Cancel/leave behavior describes actual retained draft or
running operation state without unnecessary repeated confirmation dialogs.

Save the exact host-assignment intent before submitting it. After an interrupted
response, read the current component assignments and retry only missing targets.
An existing machine may receive a new service component. If the user changes a
placement that has already been submitted, offer Restore and review saved
assignments using the actual original values; do not require the user to guess
the previous hosts or leave a replacement beside an old assignment.

Keep manual master placement when retrying a failed validation or returning to
the assignment page with unchanged inputs. A validation retry checks the current
placement; it does not request a new initial layout. Saved placements are tied to
the draft or cluster, stack, selected services and hosts, and reviewed provider
plan. A change to those inputs invalidates old advice and delayed responses,
including changes within the same draft. Saving an unchanged checkpoint must not
itself cause repeated recommendation requests.

Use confirmation for real destructive or disruptive lifecycle actions when
needed. The text identifies the exact affected deployment and dependency impact.
Avoid vague errors, repetitive inputs and confirmations for reversible filtering
or navigation. Screen-reader announcements and focus placement should make
validation errors and progress changes discoverable without stealing focus.

### Managed provider selection and updates

When HBase is selected in the service-selection step, show separate Storage
and ZooKeeper choices. Preserve the existing local-service default; choosing
an existing managed provider opens an authorized searchable provider list and
releases local daemon services selected only to satisfy HBase. Preserve services
explicitly selected by the user or needed by another local consumer, and explain
why those remain. Client packages remain part of the HBase install plan. Do not force the user to select another
full Hadoop deployment to satisfy the old local dependency checklist.

Provider choices are saved with the draft. Changing a choice invalidates its old
preview and late responses without discarding unrelated service configuration.
The configuration step shows approved shared client settings as managed values,
with a link back to provider selection. Ordinary HBase settings remain editable.
The review step repeats the owning cluster, provider, private paths and planned
HBase identity before any binding is materialized.

Add Service previews a new HBase deployment against the current saved service
plan before a HBASE record exists. Adding another service beside an installed
HBase deployment preserves its existing dependency ownership and does not open
a new provider selection flow. A changed provider can make a previously listed
choice incompatible; show the returned reason beside the selection and provide
a way to review it again or choose another provider.

The existing cluster creation wizard creates an unsecured cluster. The initial
secure managed-dependency flow adds HBase to an already Kerberized cluster,
using its authoritative realm and security settings. Final approval follows
HBase and desired-configuration creation; provider preparation, client install,
credential handling and connection verification precede Start. A secure draft
without a usable security plan remains explicitly incomplete.

Discover only authorized providers. Show owner cluster, service, compatible
version/security status and current availability. Unsupported choices explain
the reason before submission. A provider is a managed service identity, never
just a free-text URI. Explain that HBase uses the provider directly and that the
provider retains its own lifecycle; omit internal implementation machinery.

Preview shows the selected provider, independent HBase root/WAL and ZooKeeper
namespace, configuration version and planned preparation. Preparing directories
runs with provider-local authority. Ready means the required preparation and
validation completed; unknown connectivity is not equivalent to ready.

The HBase service page exposes a Dependencies tab with one storage and one
ZooKeeper card. Each shows its actual local or managed provider, preparation
and host-validation progress, and applicable Review changes or Retry actions.
Global HBase rows link directly to this tab. Show unverified connectivity, failed
verification and readiness as distinct states. A pending ownership handoff offers
the server-authorized verification action, without calling it a completed setup
or repeatedly recreating provider namespaces. Provider HDFS/ZooKeeper pages show
their dependents with the same authorization rules as the server. Consumer
progress exposes safe binding summaries; opening provider task details still
requires permission for that provider cluster.

Translate server phases into a short progress sequence: Review settings,
Prepare provider, Install clients, Check connections, Ready to start. Provider
preparation alone never fills the final step. ZooKeeper ownership reconciliation
appears as Confirm ZooKeeper access within connection checks. An unknown
execution outcome explains that the existing operation must be checked before
retrying; it does not invite creation of a replacement binding. Show the failed
stage and the next server-supported action together. Unknown response states
have a neutral unavailable status, never a green success badge. Keep protocol
hashes, epochs and journal terminology out of the main progress view.

The installation page has two explicit dependency gates. After approval, it waits
for provider preparation before installing consumer clients. After installation
and any required credential distribution, it waits for all current HBase daemon
hosts to finish connection checks before starting the selected services. The
page preserves its planned binding identities and progress on refresh, and
resumes the same server operations. Approval alone does not advance installation.
Recovery identifies the original creation attempt separately from the current
operation. An update or retry does not make a successfully created binding appear
missing, and the progress page always describes the current desired snapshot.
A failed status read offers Reload status; an operation failure offers only the
server-supported recovery action for that stage. Show completed hosts, pending
hosts and the failed connection check without requiring provider-task privileges.
Manual credential distribution is an explicit resumable step. An install-only
choice remains visibly incomplete for readiness and never reports successful
connection checks that did not run.

An installed local HBase deployment cannot silently adopt a new managed root.
Explain the unsupported data-migration case before submission. Removing a
managed HBase deployment preserves its provider data; removing a binding must
not silently reactivate old local defaults or make the consumer startable.

Changed provider configuration marks bindings stale and offers a deliberate
preview/update flow. Failed provisioning keeps its identity and provides a safe
retry path. Provider deletion is blocked while active dependencies exist. Provider
stop or restart shows affected consumers and requires deliberate confirmation of
the current impact; changed impact requires a fresh preview. Removing a consumer
preserves provider services and stored data.

### Lifecycle confirmation and recovery detail

Use the existing Stop or Restart dialog for dependency impact, with the exact
provider service and cluster in its title. Load impact inside that dialog; disable
the action until the current result is available. Show affected deployments the
user may read and a count for the remaining dependents. A failed read keeps the
dialog open with Retry. A stale confirmation refreshes the impact and asks for a
new explicit click; it never automatically submits the newly expanded operation.
Do not stack a second confirmation dialog for the same action. Use action-specific
button labels and preserve keyboard focus on retry. Capture the original route
and principal scope for the entire dialog and submission lifetime.

After acceptance, link directly to the exact cluster operation and show its current
progress. Closing a progress view leaves the server operation running and does not
repeat it when reopened. A recoverable submission failure preserves the dialog's
selection; an unknown outcome first reconciles the existing operation. Bulk actions
show one combined impact before any member is changed. Local consumer Stop stays
available even when its provider is stale or unavailable.

The dependency deployment view uses four understandable stages: Prepare provider,
Install HBase clients, Prepare credentials when required, and Verify connections.
Completed work stays visible during retries. Expand host details only when useful;
show the failed check and next available recovery action without exposing provider
administrator tasks, raw policy rules or internal hashes. An unavailable dependency
has a direct route to its review/recovery view; a disabled Start button explains the
current blocking stage. Desktop and mobile use the same operation identity and
recovery choices.

### UX evidence gate

Review actual implementation and browser evidence at 1440px and 375px.
Exercise comparison -> deployment -> config/tasks -> Back, legacy link selection,
forbidden target, incomplete-cluster resume, partial list failure, failed wizard
retry, two tabs, and stale/unavailable dependency handling. Capture network or
behavior assertions for the selected cluster, not screenshots alone. Record any
runtime blockers separately from component tests. Acceptance requires functional
navigation and recovery as well as readable layout.

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

## Implementation and acceptance

The implementation sequence, current blockers and acceptance matrix live in the
[review and remediation plan](multi-cluster-review-and-remediation.md). This
design specifies required behavior; a contract or historical source checkpoint
does not prove that its production callers, persistence or recovery exist.

The feature is tracked as AMBARI-26654. Complete coherent planned changes before unified compilation and focused validation.
Record the exact revision, commands, failures and environment limits. Publishing
a branch does not establish acceptance, and historical publication authorizations
are not instructions to publish subsequent work.

## Remediated implementation boundary

The current source implementation and executable validation are tracked in the
[review and remediation ledger](multi-cluster-review-and-remediation.md).
ManagedDependencyDeploymentCoordinator owns INSTALL, dependency readiness, START
and service checks. ManagedServiceDependencyCoordinator owns binding operations;
ActionManager owns actual tasks, and publication/lineage share the transaction.
Task and STOMP notifications are delivered only after commit. React retains draft
inputs, immutable operation IDs and acknowledgement of an observed deployment,
and renders backend state. An acknowledged deployment that disappears requires
record recovery. Wizard checkpoints preserve the whole step through reducer and
sanitizer; current binding capabilities and mutation grants govern deployment retry.

Managed Blueprints use PREPARE_ONLY to materialize resources and freeze external
dependency requirements. Live preview/approval and deployment then use the same
binding APIs as Add Service. This is a deliberate two-step contract; an unattended
single Blueprint POST is not implemented. Same-realm Kerberos is subject to the
full identity, mapping and credential proof chain; cross-realm remains unsupported.
The passing focused tests do not close the real-service/browser/KDC acceptance gate.
