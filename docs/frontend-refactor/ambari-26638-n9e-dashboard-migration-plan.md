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
--->

# AMBARI-26638: n9e-Inspired Dashboard Migration Plan

## Status

This document defines the dashboard-specific implementation plan for
`AMBARI-26638`. It refines the general Prometheus migration plan by allowing
source-level migration of reusable n9e dashboard modules, followed by an
explicit Ambari adaptation. It does not authorize copying the n9e application
shell or its unrelated product features.

Reference repositories:

- Ambari worktree: `AMBARI-26638`, commit `02524c76b2` at the time of this
  analysis.
- n9e frontend: `git@github.com:n9e/fe.git`, `main`, commit `b69c99abb` at the
  time of this analysis.

The implementation target remains the native React application under
`ambari-web/latest`. No second React root, microfrontend, or dashboard iframe
is part of this plan.

## Executive Summary

The current dashboard is visually weak because its layout, panel sizing,
renderer selection, and query lifecycle are incomplete. Changing colors alone
will not solve the problem. The n9e dashboard contains reusable production
logic for all four areas:

1. A 24-column persisted layout with rows, resizing, dragging, and responsive
   behavior.
2. A renderer registry with time series, stat, gauge, bar gauge, table, pie,
   heatmap, bar chart, text, iframe, and hexbin renderers.
3. A normalized series/data-frame pipeline with legends, calculations, value
   mappings, thresholds, overrides, and units.
4. Debounced, batched, viewport-aware, cancellable queries with stale-response
   protection.

The recommended approach is to migrate those dashboard modules and utilities
where they provide real behavior, then replace their application dependencies
with Ambari equivalents. The result should look and behave like a dense
operations dashboard while remaining an Ambari feature with Ambari APIs,
authorization, routing, localization, and cluster isolation.

## Current Ambari Baseline

The following issues are confirmed in the current `AMBARI-26638` worktree:

- The former dashboard screen sorted panels by `y/x` but rendered only
  `layout.w`; `x`, `y`, and `h` did not determine the rendered geometry. This
  is replaced by `screens/Monitoring/Dashboard/DashboardLayout.tsx`.
- `monitoring.scss` uses a content-driven CSS grid without fixed row tracks,
  collision handling, row collapse, or an edit layout model.
- Dashboard panels have a fixed minimum height and time-series charts use a
  fixed 420px height. Small stat panels therefore contain excessive empty
  space.
- `DashboardPanel` has special cases for stat/table panels, but most other
  data panels fall through to the same line chart. Renderer-specific options
  are not honored consistently.
- The table only exposes series name and value. It does not expose Prometheus
  labels, calculated columns, filtering, sorting, or export behavior.
- Chart.js receives localized timestamp strings as category labels, which
  produces crowded x-axis labels for normal monitoring ranges.
- Each target is queried independently and all visible dashboard panels begin
  querying together. There is no viewport prefetch window, abortable request,
  or request sequence guard in this implementation.

The built-in Linux dashboards use `row`, `stat`, `gauge`, `table`, and
`timeseries` panels. The repository currently contains 11 Linux dashboard
payloads, including Linux Fleet, HDFS, HBase, Hive, and YARN dashboards. Their
existing `layout`, `options`, `custom`, `targets`, and variable fields must
remain importable.

The backend already exposes Prometheus range and instant batch endpoints in
`ambari-server/src/main/java/org/apache/ambari/server/api/services/metrics/PrometheusApiService.java`.
The current batch limit is 64 queries per request. Label, label-value,
metadata, series, and targets proxy endpoints are also available.

## Goals

- Migrate the reusable n9e dashboard layout, renderer, data transformation,
  and query lifecycle behavior into Ambari.
- Support all n9e dashboard renderer types that have a meaningful Prometheus
  representation in Ambari.
- Make existing Ambari dashboard payloads render correctly without requiring a
  PromQL rewrite as part of the first visual migration.
- Provide compact, readable, responsive dashboards with no avoidable blank
  space or overlapping labels.
- Preserve Ambari authentication, cluster isolation, metric-view permissions,
  routing, localization, and API conventions.
- Keep the implementation reviewable through topic-specific commits and PRs.
- Validate the final result in the project build container and with focused
  frontend tests and screenshots.

## Non-Goals

- Copying the n9e application shell, login, user management, alerting,
  notification, trace, AI, business-group, or site-administration features.
- Adding a second React runtime or mounting the n9e application beside
  Ambari's React application.
- Preserving n9e-only endpoints, authentication bypasses, hard-coded
  datasource URLs, or credentials.
- Migrating unrelated n9e data sources such as Elasticsearch or log query
  renderers unless Ambari later defines an explicit contract for them.
- Replacing Ambari's existing navigation, authorization model, or service
  dashboard integration points.

## Migration Principles

### Migrate behavior, adapt ownership

The source implementation is Apache-licensed and can be used as a source for
selected dashboard modules after license review. The migrated code must be
owned by Ambari's `Monitoring` feature domain and use Ambari contexts, API
clients, permissions, and translations.

### Define one strict dashboard contract

The dashboard payload is a user-visible persistence contract owned by Ambari.
The new implementation accepts only schema `3.0.0`, validates every field and
panel type, and serializes the canonical model on save. Unknown fields,
unversioned payloads, and renderer-specific extensions are rejected with an
actionable validation error. A future change must introduce an explicit schema
version and migration tool; the runtime must not accumulate compatibility
branches or silently discard data.

### Use a normalized data model

Renderer components must not each parse raw Prometheus responses. A shared
adapter converts instant vectors and range matrices into a typed data-frame
model. Renderers then consume common series, labels, timestamps, values,
calculated values, units, mappings, and thresholds.

### Isolate runtime failures

Schema errors stop the dashboard at the load boundary and identify the exact
field that needs correction. After validation, a failed query affects only the
relevant panel or series. Successful panels continue to display data and
provide a retry action.

## Source Module Mapping

| n9e source module | Ambari destination | Migration treatment |
| --- | --- | --- |
| `src/pages/dashboard/Panels/index.tsx` | `screens/Monitoring/Dashboard/` | Migrate layout lifecycle and edit callbacks; replace state and API calls |
| `src/pages/dashboard/Panels/utils.ts` | `screens/Monitoring/Dashboard/layout/` | Migrate pure layout, row, insert, sort, and serialization utilities |
| `src/pages/dashboard/Panels/Row.tsx` | `screens/Monitoring/Dashboard/` | Migrate row behavior into `DashboardRow.tsx`; replace Ant Design controls |
| `src/pages/dashboard/Renderer/Renderer/Main.tsx` | `screens/Monitoring/Dashboard/PanelRenderer.tsx` | Migrate header, menu, state, and registry behavior |
| `src/pages/dashboard/Renderer/Renderer/index.tsx` | `screens/Monitoring/Dashboard/DashboardPanel.tsx` | Migrate viewport and query wiring to Ambari contexts |
| `Renderer/TimeSeriesNG` | `screens/Monitoring/Dashboard/renderers/TimeSeriesRenderer.tsx` | Migrate uPlot/data-frame/legend/threshold behavior |
| `Renderer/Stat` | `screens/Monitoring/Dashboard/renderers/StatRenderer.tsx` | Migrate calculations and responsive value layout |
| `Renderer/Gauge` | `screens/Monitoring/Dashboard/renderers/GaugeRenderer.tsx` | Migrate SVG/D3 gauge calculations and threshold colors |
| `Renderer/BarGauge` | `screens/Monitoring/Dashboard/renderers/BarGaugeRenderer.tsx` | Migrate basic and LCD bar modes |
| `Renderer/Table` and `TableNG` | `screens/Monitoring/Dashboard/renderers/TableRenderer.tsx` | Migrate label columns, sorting, filtering, links, and export |
| `Renderer/Pie` | `screens/Monitoring/Dashboard/renderers/PieRenderer.tsx` | Migrate vector-to-sector transformation and tooltip behavior |
| `Renderer/BarChart` | `screens/Monitoring/Dashboard/renderers/BarChartRenderer.tsx` | Migrate categorical and stacked bar transformations |
| `Renderer/Heatmap` | `screens/Monitoring/Dashboard/renderers/HeatmapRenderer.tsx` | Migrate bucket/matrix transformation and rendering |
| `Renderer/Hexbin` | `screens/Monitoring/Dashboard/renderers/HexbinRenderer.tsx` | Migrate only after a concrete Ambari data contract exists |
| `Renderer/Text` and `Iframe` | `screens/Monitoring/Dashboard/renderers/TextRenderer.tsx` and `IframeRenderer.tsx` | Migrate with Markdown sanitization and iframe security controls |
| `Renderer/utils/*` | `screens/Monitoring/Dashboard/data/` and `utils/` | Migrate pure calculation, format, mapping, legend, and override helpers |
| `Renderer/datasource/useQuery.tsx` | `datasource/useDashboardQuery.ts` | Migrate request state and cancellation; replace service layer |
| `Renderer/datasource/requestState.ts` | `datasource/requestState.ts` | Migrate request sequence and stale-response guards |
| `Renderer/datasource/queryStep.ts` | `datasource/queryStep.ts` | Migrate bounded step/max-data-point calculation |

## Ambari Naming, Routing, and Ownership Policy

Source-level migration does not mean source-level naming. The migrated
dashboard must be recognizable as an Ambari feature in its routes, files,
components, CSS classes, state ownership, and API calls.

### Route policy

All canonical routes remain under Ambari's `/main/monitoring` area. n9e-only
paths such as `/dashboard`, `/dashboards-v2`, `/api/n9e`, and source-specific
share URLs must not be introduced.

The canonical Ambari routes are:

```text
/main/monitoring/dashboards
/main/monitoring/dashboards/:dashboardId
/main/monitoring/explorer
/main/monitoring/targets
/main/monitoring/data-sources
/main/monitoring/shared-charts/:shareIds
```

These names are intentionally different from n9e source routes and follow the
Ambari Monitoring domain. The migration does not add redirects or aliases for
the removed source-style paths. All callers, tests, sidebar entries, and
embedded screens must use the canonical routes in one change.

### File and component policy

Do not create a copied `pages/dashboard` tree or retain generic n9e names such
as `Renderer`, `Panels`, `CommonStateContext`, `N9E_PATHNAME`, or
`dashboardV2` in the target implementation. Use the Monitoring domain and
Ambari-specific names:

```text
screens/Monitoring/Dashboard/
  DashboardPage.tsx
  DashboardLayout.tsx
  DashboardRow.tsx
  DashboardPanel.tsx
  PanelHeader.tsx
  PanelRenderer.tsx
  renderers/*Renderer.tsx
  layout/*
  datasource/*
  data/*
```

`DashboardPage.tsx` is the only dashboard screen entry point. The removed
`DashboardDetail.tsx` name must not be reintroduced as a wrapper or alias.

### Internal types versus persisted JSON

Persisted fields are the Ambari `3.0.0` contract: `version`, `var`, `panels`,
the explicit panel type set, and complete `layout` geometry. Internal
TypeScript names use Ambari semantics and map directly to this contract; no
source-format aliases or unknown-field passthrough are allowed.

### API, state, and CSS ownership

- Use `MetricsApi`, `ambariApi`, `AppContext`, `UserContext`, and `useAuth`.
- Use Ambari's `/metrics` endpoints, not n9e request paths or interceptors.
- Use `monitoring-*` or `ambari-dashboard-*` CSS prefixes.
- Replace Ant Design controls with Ambari React Bootstrap components and
  existing icon, modal, table, toast, and permission helpers.
- Keep translations in Ambari locale files and avoid source-specific global
  providers.

### Source map and review record

Every migrated source module must have a source map entry recording:

- n9e source path and frozen source commit.
- Ambari destination path and renamed public symbols.
- Logic retained from the source.
- Ambari-specific code rewritten for routing, API, authorization, types, and
  visual components.
- Source logic intentionally removed because it was n9e-only or unsafe.
- License and NOTICE impact.

The source map makes the migration auditable and avoids the appearance of an
unreviewed application fork. It also ensures that future maintainers know
which code can be updated from n9e and which code is now Ambari-owned.

## Renderer Migration Matrix

### Time series

Migrate the n9e `TimeSeriesNG` behavior rather than the current fixed-size
Chart.js wrapper. The target must support:

- Numeric timestamp x-values with controlled tick formatting.
- Multiple aligned series, null values, span-null behavior, and stacking.
- Line, area, and bar display modes where the payload requests them.
- Threshold bands, value mappings, units, decimals, and overrides.
- Shared tooltip, crosshair, zoom, reset zoom, and optional annotations.
- Bottom or right-side legends with visibility toggling, sorting, and latest,
  min, max, average, and sum columns.

`uPlot` is the preferred first implementation candidate because n9e already
has a mature wrapper and it is well suited to dense time-series data. The
wrapper must be adapted to React 19, Day.js, Ambari routing, and Ambari
styling. A Chart.js implementation remains an alternative if the dependency
and bundle review rejects uPlot.

### Stat

Migrate n9e's reducer and display behavior:

- `last`, `min`, `max`, `avg`, `sum`, and configured calculation modes.
- Unit conversion, decimal precision, value mappings, and special values.
- Threshold-driven text or background color.
- Optional sparkline or mini-graph when the panel requests it.
- Responsive multi-series grid without forcing the panel to 420px height.

### Gauge and bar gauge

Migrate SVG/D3 geometry, min/max ranges, thresholds, labels, orientation,
sorting, and responsive sizing. Gauge values must use the same normalized
calculated series and formatter as stat panels.

### Table

Migrate both the standard and enhanced table behavior:

- Build columns from Prometheus metric labels and configured fields.
- Support numeric and lexical sorting, filtering, search, pagination, and
  column sizing.
- Render units, mappings, thresholds, links, and special values.
- Export the visible or complete table to CSV.

Ambari already has `@tanstack/react-table`, so the table behavior can be
ported without importing the n9e Ant Design table.

### Pie and bar chart

Instant vector results map naturally to pie sectors and bar categories. The
adapter must define how a series name is derived from labels and how multiple
labels are displayed. Sorting, stacking, percentages, unit formatting, and
tooltips should follow n9e's payload options.

### Heatmap and hexbin

Heatmaps require an explicit matrix contract. The first supported form should
be Prometheus histogram buckets identified by `le`, with time on the x-axis
and bucket ranges on the y-axis. Hexbin should remain a later feature unless a
real Ambari dashboard needs coordinate or high-dimensional distribution data.

### Text and iframe

Text panels can migrate with sanitized Markdown, variable substitution, and
configured alignment/colors. Iframes require a strict URL allowlist,
`sandbox`, CSP compatibility, and a clear error state. An arbitrary URL from a
dashboard payload must not become an unrestricted script or credential
boundary.

## Common Data Contract

Create an explicit internal model similar to n9e's data-frame pipeline:

```text
PanelDataFrame
  fields: Field[]
  series: PanelSeries[]
  labels: Record<string, string>
  timestamps: number[]
  values: Array<number | null>
  latestValue: number | null
  calculatedValues: CalculatedValue[]
  unit: string
  thresholds: ThresholdConfig
```

The Prometheus adapter must support:

- Instant vectors for `stat`, `gauge`, `barGauge`, `pie`, and table panels.
- Range matrices for `timeseries` and time-based heatmaps.
- Label-aware series names using the target legend template.
- Multiple result series per target and hidden targets.
- Numeric timestamps in seconds and null values without coercing them to zero.
- Reducers, mappings, overrides, and threshold state in one shared path.

The `DashboardPanel` type is closed and renderer props use narrow typed
interfaces. Source `any`-heavy DTOs should not be copied into Ambari's public
API types.

## Layout and Dashboard Shell

### Read-only layout

Implement a typed layout adapter that clamps invalid values, assigns stable
panel IDs, and honors `x`, `y`, `w`, and `h`. Use fixed row tracks derived
from a configurable row height so panel content cannot resize neighboring
panels. Rows occupy all 24 columns and define a collapse boundary.

For the first migration increment, native CSS Grid is sufficient for correct
read-only rendering. It avoids making the dashboard view depend on an old
layout package before React 19 compatibility is proven.

### Edit layout

Evaluate a React 19-compatible version of `react-grid-layout` for drag and
resize editing. If the dependency passes the compatibility and bundle checks,
use it only in edit mode and persist the resulting layout into the existing
panel JSON. If it does not pass, retain the typed CSS layout adapter and add a
small Ambari-owned edit interaction rather than coupling the view to an
unmaintained package.

### Dashboard shell

Use a compact toolbar containing dashboard selection, variables, time range,
timezone, refresh interval, manual refresh, share, and authorized edit
actions. Move panel type and metadata out of permanent visual chrome. Show
panel controls on hover or keyboard focus through an overflow menu.

The visual language should be dense and operational:

- Neutral page background, white panels, restrained borders, and small radius.
- Consistent spacing and a fixed header height.
- Status colors driven by threshold state rather than arbitrary per-panel
  colors.
- Compact stat cards and charts that use the available layout height.
- Responsive breakpoints that collapse 24 columns to 12 and then one column.
- No nested decorative cards or content that is hidden behind overlays.

## Query Lifecycle

### Request grouping

Use the typed instant and range batch methods in
`ambari-web/latest/src/api/metricsApi.ts`. Group queries by datasource and
query mode, split groups at the backend limit of 64 queries, and return one
`refId/status/result/error` item per query. A failed target is reported without
discarding successful results.

### Cancellation and freshness

Every dashboard query owns an `AbortController`. Cancel requests when the
dashboard, datasource, variables, time range, or panel is replaced. Associate
each request with a monotonically increasing sequence ID and ignore responses
that are not the latest sequence for the panel.

### Viewport loading

Use an `IntersectionObserver`-based hook equivalent to n9e's
`useInViewport`. Query panels entering a configurable prefetch window and
retain successful data when a panel leaves the viewport. Static text and row
panels never issue Prometheus requests.

### Resolution and limits

Calculate step and maximum data points from the selected range and measured
panel width. Clamp the result to Prometheus and backend limits. A panel resize
may update the display resolution without forcing an unnecessary full
dashboard refresh.

### Loading and failure states

Panel headers expose loading and error indicators without replacing a
previously successful data view. Retry is scoped to the failed panel or
query. A dashboard-level summary may report partial failure, but it must not
hide healthy panels.

## Variables and Panel Operations

Migrate n9e's variable behavior into an Ambari-owned variable bar:

- Datasource variables resolve only enabled datasources visible to the current
  cluster and user.
- Cluster, host, service, and custom variables use the Ambari `${name}`
  expansion contract.
- Host and service options use Prometheus label APIs where appropriate and
  provide a loading, empty, and error state.
- Variable changes cancel stale panel requests and invalidate only affected
  data.

Migrate the panel menu actions while retaining Ambari authorization:

- View or inspect query and response data.
- Refresh one panel.
- Share a panel using the existing Ambari chart-share API.
- Edit, clone, copy, export, and delete when the user has the required
  persisted-data permission.

## Dashboard Workspace and Visualization Mapping

The Ambari implementation uses one `DashboardPage` workspace with explicit
view and edit modes. Edits are held as a local dashboard draft. Drag, resize,
panel changes, variables, metadata, and advanced JSON changes are written only
when the user invokes the workspace Save action; Discard restores the last
saved dashboard. Built-in dashboards are immutable and expose Clone and edit.
New dashboards and clones open directly in edit mode.

The full-screen panel editor follows the useful n9e workflow of live preview,
queries, and visualization options, but its modules, controls, and persisted
contract are Ambari-owned. It does not copy n9e routes, component names, Ant
Design forms, request clients, global state, or CSS.

| Visualization | Ambari renderer and effective settings |
| --- | --- |
| Time series | Lines or bars, smooth or linear interpolation, width, fill, stacking, null connection, points, linear or logarithmic scale, legend position, tooltip mode/order, unit, decimals, min/max |
| Stat | Reducer, value/name mode, value or threshold-background color, orientation, text sizes, sparkline, unit, decimals, thresholds |
| Gauge | Reducer, value/name mode, automatic or explicit bounds, units, decimals, threshold colors |
| Bar gauge | Reducer, continuous or LCD display, sorting, value mode, automatic or explicit bounds, units, decimals, thresholds |
| Pie | Pie or donut shape, reducer, legend position, units and decimals |
| Bar chart | Vertical or horizontal layout, reducer, sorting, units, decimals and explicit bounds |
| Table | Label columns, reducer, filter, sorting, CSV export, header and wrap controls, plain or threshold-colored values |
| Advanced table | Table behavior plus threshold background and in-cell gauge modes |
| Heatmap | Time/value matrix, selectable color schemes, units and decimals |
| Hex tiles | One tile per Prometheus result series, reducer, text mode, continuous palettes and reverse order |
| Text | Content, text/background colors, size, horizontal and vertical alignment |
| Embedded page | Same-origin URL enforcement and sandboxed iframe rendering |

The query editor is intentionally Prometheus-native: datasource, PromQL,
legend templates, instant/range mode, hidden targets, and maximum data points.
n9e query builders for Elasticsearch, Loki, ClickHouse, and log records are
not copied because Ambari has no corresponding datasource contract. Likewise,
n9e application-level user groups, alerting, annotations, and business groups
remain outside the dashboard workspace.

## Dependency and Runtime Adaptation

| n9e dependency or API | Ambari replacement or decision |
| --- | --- |
| React 18 | React 19 already used by `ambari-web/latest` |
| React Router 5 | React Router 7 route objects and hooks |
| Ant Design | React Bootstrap, existing Ambari Table/Modal, FontAwesome icons |
| Moment | Day.js |
| Umi Request | Existing authenticated `ambariApi` and `MetricsApi` |
| Tailwind and source Less | Ambari monitoring Sass/SCSS variables and components |
| `CommonStateContext` | `AppContext`, `UserContext`, `useAuth`, and feature context |
| `react-grid-layout` | Candidate for edit mode after React 19 verification |
| `uplot` | Candidate for TimeSeriesNG after bundle/license verification |
| D3 | Candidate for gauge/stat geometry where it removes complexity |
| G2 and related chart packages | Add only for renderer types that need them |

The migration must not import n9e's global providers, request interceptors,
router assumptions, or application-level CSS. New dependencies require a
license inventory, bundle-size review, and focused React 19 test.

## Implementation Phases

### Phase 0: Source inventory and contracts

- Freeze the n9e source revision used for migration.
- Copy only selected dashboard modules into a temporary migration area or
  migrate them directly with an auditable source map.
- Define the Ambari panel, layout, data-frame, renderer, and query-state
  interfaces.
- Add fixtures for every existing built-in panel type and representative n9e
  renderer options.

Exit criteria: every packaged dashboard parses as Ambari schema `3.0.0`,
invalid documents are rejected with field-level errors, and the source-to-
target module map is reviewed.

### Phase 1: Layout and shell

- Migrate layout utilities, rows, collapse state, and responsive breakpoints.
- Implement correct read-only `x/y/w/h` placement.
- Add the compact dashboard toolbar and variable bar frame.
- Add the panel header and hover/focus overflow menu shell.

Exit criteria: Linux Fleet and HDFS layouts match their payload geometry on
desktop and mobile, with no fixed 420px chart requirement.

### Phase 2: Data adapter and query lifecycle

- Migrate the normalized series/data-frame utilities.
- Add typed batch API methods and per-panel query state.
- Add debounce, grouping, cancellation, sequence guards, viewport loading,
  partial failure, retry, and resolution calculation.

Exit criteria: a dashboard with multiple targets uses grouped requests, stale
responses cannot overwrite current data, and one failed target does not blank
other series.

### Phase 3: Core renderers

- Migrate time series, stat, gauge, bar gauge, and table.
- Implement shared formatting, mappings, thresholds, legends, tooltips, and
  responsive sizing.
- Replace the current generic fallback line chart.

Exit criteria: all built-in Linux dashboards render their current panel types
with options and units applied.

### Phase 4: Advanced renderers

- Migrate pie, bar chart, heatmap, and text.
- Add hex tiles using the documented Prometheus vector contract: one tile per
  result series, colored by its configured reducer value.
- Add renderer-specific empty, loading, error, and export behavior.

Exit criteria: each supported type has a dedicated renderer test and invalid
types are rejected before rendering.

### Phase 5: Editing and panel operations

- Add edit-mode layout persistence after the grid dependency decision.
- Migrate inspect, share, clone, copy, export, delete, and retry actions.
- Enforce read and mutation permissions independently.
- Import and export only canonical Ambari schema `3.0.0` JSON.

Exit criteria: an authorized user can edit a validated dashboard and save the
canonical schema, while a read-only user sees no mutation controls.

### Phase 6: Built-in dashboard presentation

- Tune Linux Fleet Overview first.
- Tune HDFS, HBase, YARN, and host dashboards after renderer behavior is
  stable.
- Keep PromQL changes separate from visual layout changes where possible.
- Store desktop and mobile screenshots as visual regression evidence.

Exit criteria: the first viewport shows compact health stats and readable
charts, and no built-in dashboard has avoidable blank space or label overlap.

### Phase 7: Container verification and rollout

- Run focused frontend tests and type checks in the project build container.
- Build the frontend in the container, not by requiring a local full build.
- Exercise the dashboards against a Prometheus-compatible datasource.
- Run Playwright screenshots at desktop and mobile sizes.
- Review bundle size, dependency licenses, NOTICE changes, and security
  controls before merge.

## Recommended PR and Commit Split

The source-level migration will be too large and cross-cutting for one
unstructured PR. Use the JIRA key in every commit subject and keep each topic
independently reviewable:

1. `AMBARI-26638: Add dashboard layout adapter and compact shell`
2. `AMBARI-26638: Migrate dashboard data frame and query lifecycle`
3. `AMBARI-26638: Add native time series and value renderers`
4. `AMBARI-26638: Add advanced dashboard renderers and panel operations`
5. `AMBARI-26638: Refresh built-in dashboard layouts and visual evidence`

Keep focused tests with the behavior they verify. Put broad screenshot or
generated evidence changes in the final dashboard presentation commit when
practical.

## Testing Strategy

### Unit tests

- Layout normalization, clamping, row collapse, ordering, mobile mapping, and
  missing layout fields.
- Prometheus vector/matrix normalization and label-based series names.
- Reducers, value mappings, thresholds, units, decimals, null values, and
  special values.
- Schema validation, canonical serialization, and renderer registry coverage.
- Query grouping, batch splitting at 64, step calculation, cancellation,
  request sequence guards, and partial failures.

### Component tests

- Each renderer with empty, loading, success, partial failure, and retry
  states.
- Interactive legends and table sorting/filtering/export.
- Panel menu visibility under read-only and mutation permissions.
- Variable changes invalidating and cancelling affected queries.
- Dashboard payload save and reload using only schema `3.0.0` fields.

### Integration and visual tests

- Linux Fleet, HDFS, HBase, YARN, Hive, and host built-in dashboards.
- Cluster isolation and datasource authorization.
- Desktop, tablet, and mobile breakpoints.
- Long labels, many series, missing values, slow datasource, and datasource
  failure cases.
- Playwright screenshots compared against an approved visual baseline.

## Security And Licensing

- Preserve Apache license headers in migrated source files and review all
  transitive dependency licenses.
- Update Ambari NOTICE or license inventories when source code or dependencies
  require it.
- Never copy source credentials, hard-coded endpoints, authentication bypasses,
  or development-only proxy settings.
- Keep datasource credentials out of list/read DTOs and browser logs.
- Sanitize Markdown and enforce iframe URL policies.
- Preserve `CLUSTER.VIEW_METRICS`, `HOST.VIEW_METRICS`, and persisted-data
  mutation checks through all migrated operations.
- Preserve cluster ownership on dashboard, datasource, and chart-share
  operations.
- Do not introduce a second router, HTTP client, or global state provider.

## Risks and Mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Old `react-grid-layout` is incompatible with React 19 | Build or runtime failure | Verify a compatible version in isolation; keep CSS layout fallback |
| uPlot/G2 increases bundle size | Slower initial dashboard load | Lazy-load advanced renderers and measure bundle deltas |
| Source components depend on Ant Design globals | Visual or runtime regressions | Replace UI primitives at module boundaries and remove global CSS |
| Source data model is broader than Prometheus | Invalid assumptions in renderers | Normalize through an explicit Ambari `PanelDataFrame` contract |
| Heatmap/hexbin payloads lack a stable query shape | Incorrect charts | Document supported matrix contracts and preserve unsupported payloads |
| Dashboard refresh causes request storms | Prometheus overload | Batch by datasource, use viewport loading, debounce, and bounded steps |
| Schema evolves without an explicit migration | User configuration cannot be loaded | Reject the document with a field-level error and add a versioned migration before changing the contract |
| Direct source reuse creates license omissions | Release compliance issue | Keep a source map, retain headers, and complete NOTICE/dependency review |

## Acceptance Criteria

The migration is ready for merge when all of the following are true:

- Existing packaged dashboard JSON files are authored in schema `3.0.0` and
  render without runtime conversion.
- `x/y/w/h` determine panel placement and size in the view.
- Stat, gauge, bar gauge, table, time series, pie, bar, heatmap, and text
  panels have dedicated behavior where their contracts are supported.
- Time-series axes remain readable across short and long time ranges.
- Panel height is derived from layout and measured container size.
- Queries are grouped and bounded, out-of-viewport panels are deferred, and
  stale responses are ignored.
- A failed panel or target does not blank successful panels.
- Read-only users cannot invoke edit or mutation operations.
- Dashboard payloads are validated and round-trip as canonical schema `3.0.0`
  JSON; unsupported documents fail with an actionable error.
- Desktop and mobile screenshots contain no horizontal overflow, clipped
  labels, or excessive fixed-height whitespace.
- Container verification, focused tests, visual checks, license review, and
  security review are complete.

## Decision Record

The selected direction is **source-level migration of the n9e dashboard
subsystem with Ambari-native adaptation**. This is broader than using n9e as a
visual reference, but narrower than copying the n9e application. The first
implementation priority is layout and renderer correctness; advanced chart
types follow once the common data-frame and query contracts are stable.
