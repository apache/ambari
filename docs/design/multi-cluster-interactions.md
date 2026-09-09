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

# Multi-cluster interaction contract

Status: Astra interaction design for the implementation mission. The user
explicitly requested improved usability and improvements to existing
interactions on 2026-09-08. Follow the established Ambari style and localization;
this is an incremental product-flow improvement, not an unrelated visual rewrite.

## Finding and comparing deployments

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

## Staying oriented inside a cluster

Display an explicit cluster identity in the operational shell, with a clear
return to the global cluster/service directory. Preserve existing cluster-level
service, host, alert, configuration and background-task screens. Navigation,
config saves, restart links and task drawers must retain the route's cluster.
A chooser for an old unscoped link explains which destination will open after
selection and retains that destination. A forbidden or removed target shows a
useful access/not-found state; it never silently opens a different cluster.

Keep global Ambari administration and Views distinct from deployment context.
A cluster administrator must not see controls implying global authority.

## Creating and recovering a cluster

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

## Managed provider selection and updates

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

## Lifecycle confirmation and recovery detail

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

## UX evidence gate

Astra reviews actual implementation and browser evidence at 1440px and 375px.
Exercise comparison -> deployment -> config/tasks -> Back, legacy link selection,
forbidden target, incomplete-cluster resume, partial list failure, failed wizard
retry, two tabs, and stale/unavailable dependency handling. Capture network or
behavior assertions for the selected cluster, not screenshots alone. Record any
runtime blockers separately from component tests. Acceptance requires functional
navigation and recovery as well as readable layout.
