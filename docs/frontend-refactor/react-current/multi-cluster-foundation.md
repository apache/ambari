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

Source and focused regression tests exist, but no new test, build, or browser
result is claimed here until the final integrated validation gate runs.

## Current Contract

| Area | Current React behavior | Intentional difference and remaining gate |
| --- | --- | --- |
| Routes | Operational screens live below `/clusters/:clusterName/main/*`; `/clusters` and `/services` are global directories. One authorized cluster resolves a legacy `/main/*` link; multiple clusters show a chooser that preserves a validated continuation. | Classic and the prior React shell selected the first returned cluster. Direct, Back/Forward, forbidden, and two-tab browser behavior still requires runtime capture. |
| Runtime identity | The application scope key distinguishes global scope from a named cluster and includes the authenticated user. Cluster providers remount at the route boundary; caches, async loaders, navigation callbacks, and browser event projection carry generation and cluster identity checks. | Late REST, polling, navigation, and websocket updates are discarded before reducer/cache mutation. Server delivery remains the confidentiality boundary. |
| Authorization | Runtime authorization records retain resource type/name. AMBARI grants are global; CLUSTER grants match the explicit route target. A cluster administrator is not promoted to Ambari administrator. Task-view policy is evaluated for the row/route cluster. | Classic-compatible global behavior is retained only for actual AMBARI-scoped authority. Executed role combinations remain pending. |
| Directories | `/clusters` shows authorized deployments and owner-only creation drafts. `/services` fetches services and HBase dependency summaries through one four-request limiter, cancels queued old generations, keeps usable rows on partial enrichment failure, and stores filters/sort/page in the URL. Desktop and narrow layouts expose owning cluster, provider owners, readable phases, and direct overview/dependencies/config/task links. | Service deployments remain independent rows; opening one establishes URL scope instead of changing a global selection. Provider HDFS/ZooKeeper pages list only server-authorized named HBase consumers and preserve the remaining consumers as an anonymous count. Lifecycle mutations remain separately gated. |
| Workflow recovery | New cluster state uses a UUID draft scope. Add Host, Add Service, reassign, Kerberos, HA, and upgrade state use numeric cluster scope with expected revisions. Secret values and authenticated repository URLs are removed, affected input steps are reachable, and mutation remains blocked until re-entry and a successful checkpoint. | A sole-cluster legacy snapshot migrates only with matching principal/controller proof. Multiple or ambiguous clusters preserve the old data and show recovery guidance. |
| Classic safety adapter | Classic workflow persistence uses verified principal plus draft/cluster/controller/tab identity. Browser storage is scoped, writes a sanitized snapshot, and retains credentials only in scope-local memory until refresh. Queue failure pauses all pending work; Retry reloads; release keeps ownership when the server call fails. | Widget drafts stay browser-only and scoped. Unsupported or ambiguous global recovery state is never assigned to a cluster. |
| Creation and repositories | Review reconciles the owner-only draft-to-cluster association before collision handling. Cluster creation sends `creation_draft_id`. Existing clusters and repository versions are never deleted. Identical repository settings are reused; new definitions send initial OS settings atomically; conflicts do not mutate shared data. | This deliberately removes Classic's destructive single-cluster cleanup. Lost responses may resume only through the exact server association, never a same-name lookup. |
| Metrics scope | Query endpoints remain usable for an authorized managed datasource. Unsupported scoped label/target metadata shows a terminal local explanation without retry loops or a false empty result. Datasource changes discard late metadata/query results, and PromQL history requires principal plus numeric cluster identity. | The stable server error is `METRICS_SCOPE_UNSUPPORTED`. The UI does not guess metadata from another datasource or cluster. |

## Focused Evidence Pending Execution

The regression source covers scoped authorization, deterministic legacy links,
global-versus-cluster route identity, stale cache and navigation completion,
event projection, bounded directory and provider-summary loading/retry/revocation,
authorized provider-dependent projection, desktop/mobile directory structure,
draft conflict and deferred responses, secret re-entry,
exact create reconciliation, repository reuse/conflict, Classic tab and volatile
credential isolation, and unsupported metrics metadata with rapid datasource
changes. Execute only the focused files selected in the run artifact, followed
by TypeScript/Vite build and the required browser/network scenarios.
