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

# React Multi-Cluster Foundation

## Scope

This note records the cross-module React behavior added on top of pinned commit
`a62fe4959dc948210d844295a097a3311321dea6`. The applicable Classic behavior and
the Ember baseline documents for authentication/shell, background operations,
cluster installation, Add Host, Add Service, Kerberos, upgrade, and HA were read
before the changes. The multi-cluster behavior below intentionally differs where
Classic used one global cluster or one global wizard snapshot.

The 2026-09-10 source review and executed focused tests are recorded in the
[review and remediation plan](../../design/multi-cluster-review-and-remediation.md).
This table describes foundation code, not full workflow acceptance. The remediation
moves managed Step9 installation, readiness, Start and service-check state to a
durable backend deployment UUID. React renders that state and retains launch/retry
IDs plus acknowledgement of an observed server deployment; an acknowledged 404
cannot launch replacement work. Complete step checkpoints retain the handoff and
intent through the actual reducer and sanitizer, including shared object references.
It no longer scans requests to infer lineage. Provider impact confirmation and
binding update/retry/detach now have production UI callers. Legacy client-only no-op
completion requires actual owned host state; an empty daemon Start response cannot
complete the wizard. These intentional differences tighten Classic's empty-response
success and browser-owned deployment behavior.

## Current Contract

| Area | Current React behavior | Intentional difference and remaining gate |
| --- | --- | --- |
| Routes | Operational screens live below `/clusters/:clusterName/main/*`; `/clusters` and `/services` are global directories. An interrupted login restores its authorized cluster route. Direct login and legacy `/main/*` links use the current user's last numeric cluster ID, revalidated against the API list and authorization; otherwise a sole cluster is selected or a modal preserves the validated continuation. Login does not open the management directory. | Classic and the prior React shell selected the first returned cluster. Direct, Back/Forward, forbidden, and two-tab browser behavior still requires runtime capture. |
| Runtime identity | The application scope key distinguishes global scope from a named cluster and includes the authenticated user. Cluster providers remount at the route boundary; caches, async loaders, navigation callbacks, and browser event projection carry generation and cluster identity checks. | Late REST, polling, navigation, and websocket updates are discarded before reducer/cache mutation. Server delivery remains the confidentiality boundary. |
| Authorization | Runtime authorization records retain resource type/name. AMBARI grants are global; CLUSTER grants match the explicit route target. A cluster administrator is not promoted to Ambari administrator. Task-view policy is evaluated for the row/route cluster. | Classic-compatible global behavior is retained only for actual AMBARI-scoped authority. Selected role combinations pass focused tests; the live multi-user matrix remains pending. |
| Directories | `/clusters` shows authorized deployments and owner-only creation drafts. `/services` fetches services and HBase dependency summaries through one four-request limiter, cancels queued old generations, keeps usable rows on partial enrichment failure, and stores filters/sort/page in the URL. Desktop and narrow layouts expose owning cluster, provider owners, readable phases, and direct overview/dependencies/config/task links. | Service deployments remain independent rows; opening one establishes URL scope instead of changing a global selection. Provider HDFS/ZooKeeper pages list only server-authorized named HBase consumers and preserve the remaining consumers as an anonymous count. Lifecycle mutations remain separately gated. |
| Workflow recovery | New cluster state uses a UUID draft scope. Add Host, Add Service, reassign, Kerberos, HA, and upgrade state use numeric cluster scope with expected revisions. Secret values and authenticated repository URLs are removed, affected input steps are reachable, and mutation remains blocked until re-entry and a successful checkpoint. | A sole-cluster legacy snapshot migrates only with matching principal/controller proof. Multiple or ambiguous clusters preserve the old data and show recovery guidance. |
| Classic safety adapter | Classic workflow persistence uses verified principal plus draft/cluster/controller/tab identity. Browser storage is scoped, writes a sanitized snapshot, and retains credentials only in scope-local memory until refresh. Queue failure pauses all pending work; Retry reloads; release keeps ownership when the server call fails. | Widget drafts stay browser-only and scoped. Unsupported or ambiguous global recovery state is never assigned to a cluster. |
| Creation and repositories | Review reconciles the owner-only draft-to-cluster association before collision handling. Cluster creation sends `creation_draft_id`. Existing clusters and repository versions are never deleted. Identical repository settings are reused; new definitions send initial OS settings atomically; conflicts do not mutate shared data. | This deliberately removes Classic's destructive single-cluster cleanup. Lost responses may resume only through the exact server association, never a same-name lookup. |
| Metrics scope | Query endpoints remain usable for an authorized managed datasource. Unsupported scoped label/target metadata shows a terminal local explanation without retry loops or a false empty result. Datasource changes discard late metadata/query results, and PromQL history requires principal plus numeric cluster identity. | The stable server error is `METRICS_SCOPE_UNSUPPORTED`. The UI does not guess metadata from another datasource or cluster. |

## Validation limits

The regression source covers scoped authorization, deterministic legacy links,
global-versus-cluster route identity, stale cache and navigation completion,
event projection, bounded directory and provider-summary loading/retry/revocation,
authorized provider-dependent projection, desktop/mobile directory structure,
draft conflict and deferred responses, secret re-entry,
exact create reconciliation, repository reuse/conflict, Classic tab and volatile
credential isolation, and unsupported metrics metadata with rapid datasource
changes. The final remediation selection ran ten frontend files and 91 tests,
all passing. Backend focused validation ran 205 Java tests, including real H2/Guice
publication/rollback and migration; Python selections passed 31 tests. The subsequent deploy build exposed those TypeScript diagnostics as a packaging blocker.
The runtime follow-up repairs them without excluding tests from compilation;
`npx --no-install tsc -b` now passes. The affected build/recovery selection adds
51 plus 7 passing tests, including generation-scoped Add Service reloads. The earlier broad selection's 19 failures and one
unhandled error were not relabelled as passing. See the linked plan for exact
commands and mock boundaries. Browser/network, real services, real KDC and full
packaged-build acceptance remain open.

## Approved navigation follow-up

The user-approved interaction restores the installed cluster Dashboard by default,
matching Classic AUTH-006 and SHELL-003 without its global cluster selection. A
user-scoped local preference stores only a numeric cluster ID; runtime ownership
continues to come from the explicit route. Interrupted paths are tab-local session
storage, consumed after login. Removed or revoked preferences prompt selection.
The visible cluster dropdown lists authorized API results and exposes Admin Cluster
Management only to the existing Admin rename authority. Admin receives the selected
cluster in its document query, validates it against the API list, and returns its
Dashboard using an encoded cluster route. Direct Admin access with several clusters
requires selection instead of silently choosing the first API item. The user subsequently approved the Admin management structure described below.

Regression sources were updated for continuation, rename, principal separation,
revocation, and Admin URL propagation. The user requested manual acceptance after
file replacement; these new tests and browser acceptance have not been run. Only
the two frontend production builds are required for this replacement. Commit and
RPM rebuilding must wait for the user's acceptance. Subsequent deployment acceptance
will use two independent clusters with HBase using local Hadoop and ZooKeeper.

## Admin cluster management expansion

The approved Admin menu now has Cluster Overview, Create Cluster, Host Resources,
Cluster Permissions, Versions & Repositories, and Remote Clusters. Admin root opens
the global overview. Cluster Details uses explicit document-query cluster context
and URL-selected Basic Information, Services & Hosts, Access Permissions, Operation
History, and Configuration & Export tabs. Global inventories do not filter by the
selected operational cluster. Registration, installation, Add Host and Add Service
continue through existing wizard callers and durable draft IDs. Host Resources
opens the target's Add Host wizard; it does not implement a second host-assignment
workflow. Existing registered-host ownership checks remain the backend boundary.

New reads use a cancelable resource hook that drops previous-scope responses and
rejects malformed collections. Authorization comes from the authenticated HTTP User
header and the user's actual authorization resources, matching AMBARI or explicit
CLUSTER scope. Cluster grants use POST and exact privilege-ID DELETE, followed by
GET reconciliation; they never replace the entire cluster grant collection. Rename
reconciliation uses numeric cluster ID. Deletion shows impact, checks current and
desired component states, re-reads target identity, calls the existing guarded API,
and confirms 404 before completion. No native lifecycle constraints are bypassed.

Operation history shows exact request IDs and links into the existing cluster task
viewer; the task route now accepts a validated positive integer request ID. Remote registration retains its existing implementation. The version list now
binds status/navigation to the explicitly selected cluster and also loads the
global catalog without a default cluster. Side navigation
expansion is keyboard-operable, and collapsed entries navigate to their routes.
The actual Angular Cluster controller/privilege APIs, Classic installation and Add
Host controllers, and Ember installation/host/permission baselines were inspected.

Regression sources cover resource-scope cancellation, malformed collections,
cluster/global authorization boundaries, lost grant response reconciliation, and
exact task links. The user subsequently accepted the UI and authorized functional testing. The
Chrome DevTools MCP acceptance section in the remediation review records the
executed tests, real browser/API checks, three runtime fixes and remaining gaps.


Chrome DevTools MCP found and verified fixes for selected-cluster version ownership,
production JSON-filter handling of nested workflow endpoints, and preservation of
the draft query through wizard navigation. These are not mock-only claims: browser
Next, refresh and Admin resume recovered one exact persisted draft. Read-back also
verified temporary-cluster grant creation/removal, stable-ID rename and deletion.
The former manual-only acceptance gate is historical; packaged independent-cluster
HBase and the broader integration matrix remain unverified at this checkpoint.
