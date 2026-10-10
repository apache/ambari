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

# Monitoring Workspace and Console Presentation

Follow-up: [Management Pack Catalog, Appearance, and Workspace Navigation](mpack-catalog-and-appearance-20260929.md)
records the subsequent catalog grouping, dark theme, and global-return changes.

## Scope and baseline

This incremental change applies the approved console design to the cluster
shell, global directories, and the existing monitoring workflows. It preserves
service operations, datasource management permissions, configuration APIs,
dashboard persistence, and the management pack transaction contract.

Reviewed baseline material: `ember-baseline/01-auth-shell.md`,
`02-background-dashboard.md`, `04-services-configs.md`, and
`14-service-theme-layout.md`. Reviewed Classic entry points include
`app/views/main/menu.js`, `app/templates/main/dashboard.hbs`, and
`app/views/common/controls_view.js`. Classic metrics are explicitly outside the
non-metrics parity baseline. The monitoring changes below are intentional
extensions, not claims of Classic monitoring parity.

## Implemented behavior

- A light console shell shares control, panel, and dialog radii of 8, 12, and
  16 pixels. Global cluster navigation is visible from the cluster sidebar;
  Management Packs is visible only to Ambari administrators. The existing
  protected route remains authoritative. Collapsed entries have accessible
  names, and collapse controls are buttons.
- The footer participates in document flow rather than covering charts.
  Breadcrumbs expose the actual cluster identity and a global cluster link.
- Dashboard catalogs use named cards with secondary export, clone, and delete
  actions. Internal dashboard identifiers and schema versions are not primary
  browsing content. Existing import and editing capabilities remain available.
- Dashboard refresh defaults to 30 seconds. Preferences are isolated by user
  and cluster, tolerate unavailable browser storage, and preserve explicit
  pause. Absolute historical ranges and layout editing do not auto-advance.
  Query timestamps retain second precision.
- Time series receive explicit query boundaries, including single-sample
  series. Narrow reading layouts retain panel height rather than collapsing
  content into a 32-pixel grid row. Background stat colors use a light tint and
  the configured threshold color as an accent, retaining threshold semantics.
- Panel menus hand the resolved query, datasource, cluster route, and time
  range to Explorer. Explorer distinguishes initial, loading, empty, failed,
  and successful results, supports request cancellation, and identifies results
  belonging to previously edited query inputs. Query history remains isolated
  by principal and persistent cluster ID.
- Time-series legends use aligned, scrollable DOM controls instead of wrapping
  canvas labels. Clicking a name toggles only that series; Only isolates it,
  and Show all restores every series. Visibility and colors follow canonical
  metric labels and query-target identity across refresh/reordering. Isolated
  views keep newly arriving series hidden; an all-hidden state is explicit.
  Reading a legend does not change collection. Tests cover reordering,
  refreshed samples, newly arriving/missing series, identical display names,
  and the same controls in table legends and bar presentation.
- Target lists support search, health filtering, and a detail drawer. Managed
  VictoriaMetrics targets join Ambari discovery with stored `up`,
  `timestamp(up)`, and scrape duration observations. They do not ask the
  storage node to report a separate VMAGENT's scrape targets.
- Managed observation joins use exact cluster name, persistent cluster ID,
  host, target kind, service, and component identities. Missing, conflicting,
  malformed, foreign, or older-than-five-minute observations do not become
  healthy. Batch failures remain explicit errors. Discovery and query requests
  retain the Server's authorization checks; other datasources retain their
  native targets API and unsupported-scope behavior.
- Datasource details distinguish configuration enabled/disabled state from
  successful connectivity testing. Existing edit and management authorization
  are unchanged. New primary monitoring controls have English and Chinese
  translations.
- The legacy Trino updater keeps a stable React hook order and waits for a
  native model. Imported Trino services using the generic model no longer
  trigger undefined legacy-model writes or missing quicklink-response reads.

## Regression coverage

- Sidebar tests cover the global entry, administrator-only Management Packs,
  collapsed access labels, and existing declared service identities.
- Managed-target tests cover successful structured observations, explicit down
  samples, missing/stale/future samples, conflicting collectors, foreign
  persistent identities, invalid numbers, and failed envelopes with misleading
  diagnostic text.
- Workspace tests cover second precision, preference isolation, explicit pause,
  corrupt stored values, and blocked storage.
- Explorer tests cover empty-result feedback, actual AbortSignal cancellation,
  late responses, unsupported metadata, and existing cluster/datasource
  isolation. Panel tests preserve chart sharing and verify query handoff scope.
- Trino hook tests cover missing services, generic imported services without a
  legacy model, late native-model initialization, and service removal without
  changing hook order.

## Deployment and remaining boundaries

The local deployment uses the existing Ambari RPM and cluster. Vite assets are
copied into the running Server's Web directory before atomically replacing
`index.html`. Previous hashed assets are retained for already-open tabs and the
previous Web directory is backed up. No Server restart or RPM build is needed.

This change does not install exporters for every imported service, replace the
configuration textarea with a full code editor, or implement the separate
management-pack service/version grouping proposal. Those are independent
workflow changes. Dashboard names and query legends from stored documents stay
authored content; translating the new console controls does not rewrite them.
The build still reports the existing large JavaScript chunk warning. Complete
multi-role, SSO, and production-scale performance acceptance remains separate.

## Verification result

- `npm run build`: passed (TypeScript and Vite).
- `npm test -- --maxWorkers=6`: 255 files and 1,432 tests passed.
- `git diff --check`: passed.
- Chrome desktop acceptance passed 12 workflows: refresh persistence,
  management-pack navigation, chart-to-query context, query results, empty
  results, query failure, ten live targets, target filtering, target details,
  target-to-query context, datasource details, and dashboard searching. No
  uncaught browser exceptions were observed in this final run.
- Built and deployed `index.html` SHA-256 values match:
  `97473bcc240caead716c7ce169e096b19391f166e442ad0033cd6c0bfb7d25cd`.
- Desktop is the accepted runtime scope. The user explicitly excluded mobile
  layout work; mobile acceptance is not claimed. An intermediate 768px check
  found overflow in the existing top navigation controls, which remains outside
  that scope.
- Follow-up disk-throughput legend acceptance observed 22 series. Labels were
  aligned in a scrollable DOM legend; toggle, isolate, Show all, and isolation
  across refresh passed in Chrome without uncaught exceptions. The chart canvas
  retained 88px and the legend 74px in the existing 229px panel. Four additional
  legend regression tests cover stable identity and refresh behavior.

## Final combined commit verification

The combined source tree passed 260 Vitest files and 1,451 tests with
`npm test -- --maxWorkers=6`. Each of the five code-topic snapshots was exported
from the Git index to an independent directory and passed forced TypeScript
compilation, a Vite production build, and its focused tests before commit.
The focused counts were 25 (shell/theme/navigation), 68 (monitoring),
14 (management-pack catalog), 1 (Trino initialization), and 2 (host alert badge).
Documentation is recorded in a separate final commit.
