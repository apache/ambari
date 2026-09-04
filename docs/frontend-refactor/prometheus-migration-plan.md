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

# Native React Prometheus Monitoring Migration Plan

## Status and Decisions

- Target baseline: GitHub PR 4180 head
  `f24fd0c4bf557ff04ca0eddd4a9756d8b20eec2a`.
- Source snapshot: `am-pro` `origin/3.0_metrics` at
  `e4c7db384c58f9fe6baca08a52906557ace5f573`.
- Worktree: `/Users/jialiang/PRJS/ambari-prometheus-react`.
- Work branch: `prometheus-react-migration`.
- Frontend architecture: direct integration into the existing React 19
  application under `ambari-web/latest`; no microfrontend, iframe, secondary
  React root, or Ember bridge.
- The source Nightingale-derived frontend is behavior evidence, not a package
  to copy wholesale.
- Existing `3.0_metrics` datasource rows and dashboard payloads must remain
  usable after upgrade. The migration must preserve the source multi-datasource
  model rather than reducing it to a VictoriaMetrics-only configuration.
- Ambari Agent metric collection is implemented on the separate
  `prometheus-agent-integration` branch. That branch remains untouched until
  the server and React migration is complete, then is integrated as the final
  collection step.
- The legacy AMS and Ganglia implementations are removed as part of this
  migration. `metrics.json` retains only the minimal JMX control-plane values
  still required by management workflows; it is not a dashboard or time series
  contract.

The companion [source inventory](prometheus-source-inventory.md) is normative
for source revisions, paths, and exclusions.

## Goals

1. Replace the old AMS-oriented monitoring experience with a native
   Prometheus-compatible experience while retaining the existing Ambari shell,
   authentication, routing, authorization, service context, and visual system.
2. Provide datasource management, PromQL exploration, dashboard management,
   target management, cluster metrics, service metrics, and shared charts.
3. Preserve flexible Prometheus-compatible datasource configuration, including
   existing `3.0_metrics` datasource records and VictoriaMetrics deployments,
   without hard-coded infrastructure or credentials.
4. Preserve Ambari multi-cluster isolation and existing metric-view
   authorizations.
5. Add install and upgrade schema support, failure handling, tests, and
   recovery behavior that are missing from the source implementation.
6. Keep the migration reviewable through behavior-oriented commits that build
   and test independently.

## Non-Goals

- Porting the Nightingale application shell or running React 17 beside React 19
- Porting Nightingale user, permission, login, alert, task, notification,
  trace, model, AI, chat, or site administration features
- Preserving `/api/n9e` compatibility endpoints that have no selected workflow
- Reproducing the source Ant Design/Tailwind visual system
- Copying source hard-coded endpoints, credentials, authentication bypasses,
  or global metric property-validation bypasses
- Cherry-picking the `3.0_metrics` branch or its full-repository formatting
- Preserving AMS or Ganglia as a monitoring fallback
- Porting Categraf; Ambari Agent replaces it in a separately developed branch
- Merging the Ambari Agent collection branch before the core monitoring
  migration and its compatibility tests are complete

## Target Architecture

### Frontend ownership

Create a native feature domain with this intended structure:

```text
ambari-web/latest/src/
  api/
    monitoringApi.ts
    monitoringApi.test.ts
  screens/Monitoring/
    MonitoringLayout.tsx
    MonitoringRoutes.tsx
    context.tsx
    types.ts
    utils/
    components/
    DataSources/
    Query/
    Dashboards/
    Targets/
    Charts/
```

The existing `screens/Metrics` directory remains the service-tab integration
point for the Prometheus service-dashboard contract. The existing
`screens/Dashboard/Metrics.tsx` remains the cluster-dashboard integration
point. Neither path falls back to AMS or Ganglia.

Frontend rules:

- Use the existing `ambariApi` Axios instance so authentication cookies,
  `X-Requested-By`, error handling, and base paths remain consistent.
- Use React Router 7 route objects in `RoutesList.tsx`.
- Use `AppContext`, `UserContext`, and existing authorization hooks instead of
  the source `CommonStateContext` profile and business-group bootstrap.
- Use React Bootstrap, existing table/modal/tooltip components, Day.js,
  lodash, react-select, Chart.js, and existing loading/error patterns.
- Do not add Ant Design, Moment, Umi Request, Tailwind, React 17, or Router 5.
- Consider only React 19 compatible PromQL editor and dashboard-grid
  dependencies. Each new dependency requires a license and bundle-size check.
- Keep API response types explicit. Do not use source `any`-heavy DTOs as the
  target contract.
- Use abortable requests and existing polling helpers. Route changes,
  datasource changes, and time-range changes must cancel stale queries.

### Routes and navigation

Add a top-level Monitoring sidebar group with these native routes:

| Route | Screen | Minimum read authorization |
| --- | --- | --- |
| `/main/monitoring/data-sources` | Datasource management | `CLUSTER.VIEW_METRICS` |
| `/main/monitoring/explorer` | PromQL explorer | `CLUSTER.VIEW_METRICS` |
| `/main/monitoring/dashboards` | Dashboard list | `CLUSTER.VIEW_METRICS` |
| `/main/monitoring/dashboards/:dashboardId` | Dashboard view/edit | `CLUSTER.VIEW_METRICS` |
| `/main/monitoring/targets` | Target machines | `HOST.VIEW_METRICS` |
| `/main/monitoring/shared-charts/:shareIds` | Shared charts | `CLUSTER.VIEW_METRICS` |

Retain and update these existing consumers:

- `/main/dashboard/metrics`
- `/main/services/:serviceName/metrics`

The sidebar group is visible when the user has any applicable cluster, host,
or service metric-view authorization. Mutation controls are independently
guarded and hidden when the user is read-only.

### Backend ownership

Keep the external namespace under `/api/v1/metrics`, but implement it as
native Ambari JAX-RS resources registered by the current `ApiModule`. Divide
the source monolith into focused services:

- `DatasourceService`: datasource CRUD, validation, test connection, and
  secret-safe DTO mapping
- `PrometheusQueryService`: instant/range/batch query and metadata proxying
- `DashboardService`: dashboards, payloads, cloning, visibility, import, and
  built-in dashboard provisioning
- `TargetService`: heartbeat, target list, metadata, tags, notes, and business
  group assignment
- `ChartShareService`: share creation and readback
- `BuiltinIntegrationService`: classpath resource discovery and idempotent
  import

Use existing Ambari Guice, JPA, transaction, Jackson, and HTTP facilities.
Do not add source Spring WebFlux, Reactor Netty, Lombok, or an additional JPA
provider unless a focused implementation proves the current facilities
insufficient.

### Persistence

Port the selected datasource, dashboard, built-in integration, target,
business-group, and chart-share entities. Correct the model while preserving
source behavior:

- Add cluster ownership where data is cluster-specific.
- Add owner identity for user-created dashboards and chart shares.
- Define foreign keys, uniqueness constraints, and query indexes explicitly.
- Avoid serializing datasource credentials in list/read responses.
- Store sensitive values through an established Ambari credential mechanism or
  encrypted representation; never return them after creation.
- Make built-in import idempotent by stable integration/dashboard identifiers.
- Preserve source datasource type, URL, authentication, TLS, HTTP, and custom
  configuration fields with a versioned compatibility mapper.
- Preserve existing board and board-payload identifiers and JSON. Payload
  changes must be lazy and reversible or performed by a tested in-place
  migration; existing rows must never be replaced by built-in defaults.
- Define deletion behavior for dashboards, payloads, business-group mappings,
  and target mappings.

Update every supported CREATE DDL dialect. Add a new non-final upgrade catalog
for the current 3.1 line, register it before `FinalUpgradeCatalog`, and cover
table, sequence, index, foreign-key, and idempotency behavior with upgrade
tests. The exact source/target version pair must match the accepted release
upgrade policy when implementation begins.

### Collection and storage

Port the final `VICTORIAMETRICS` BIGTOP 3.2.0 service state and its package
mappings. Do not port Categraf or the Nightingale server. Ambari Agent replaces
Categraf, but its independently developed branch is merged only after the core
server and React migration is complete.

Required runtime behavior:

- VictoriaMetrics location, port, TLS, and authentication come from Ambari
  configuration, never source literals.
- Ambari Server and Agent cache only validated discovery assignments, telemetry
  profiles, and other collection configuration. Each host or component scrape
  reads current values; neither layer caches metric values or transports them
  in Agent heartbeat payloads.
- HTTP service discovery identifies Agent host targets with authoritative
  `cluster`, `host`, and `ambari_target="host"` labels. The managed vmagent
  allowlist accepts `host;;ambari_agent_.*`; built-in host PromQL must include
  `cluster="${cluster}",ambari_target="host"`.
- Service checks do not place credentials on a command line or in logs.
- Package selection covers supported RPM and DEB families from the target
  stack matrix.
- The server assembly includes built-in collection templates and dashboards at
  a deterministic runtime path.
- Built-in datasource and dashboards are provisioned idempotently after the
  required service configuration becomes available.
- Final Agent integration documents and tests the scrape endpoint, labels,
  metric names, target identity, and VictoriaMetrics scrape configuration
  without modifying historical datasource or dashboard records.

## Native API Contract

Before porting screens, define and test typed DTOs for these selected contracts:

### Datasources

- List with pagination/filtering
- Read redacted details
- Create and update
- Delete with dependency conflict reporting
- Test connection
- Brief selector list
- Prometheus metadata discovery

### Prometheus queries

- Instant query
- Range query
- Batch instant query
- Batch range query
- Label names
- Label values
- Series lookup
- Metric metadata

The proxy must validate schemes and resolved destinations, reject local or
otherwise disallowed destinations according to policy, apply connection/read
timeouts and response-size limits, forward only required headers, redact
credentials from errors, and map upstream failures into stable Ambari error
responses.

### Dashboards and chart shares

- Dashboard list, read, create, update, delete, clone, import, and export
- Dashboard payload read/update
- Built-in dashboard list and import
- Public/shared visibility scoped by Ambari authorization
- Chart share create/read with expiry and ownership rules

### Targets and business groups

- Authenticated agent heartbeat
- Paginated target list with query, status, tag, and business-group filters
- Metadata and extra metadata
- Tag, note, and business-group updates
- Target deletion with deterministic empty-list behavior
- Business-group list and selected mutation operations

No selected frontend code may call `/api/n9e` after the native API layer is
introduced.

## Authorization Matrix

Use existing Ambari role authorizations instead of source fake profiles or
filter bypasses.

| Operation | Recommended authorization |
| --- | --- |
| Cluster query and dashboard read | `CLUSTER.VIEW_METRICS` |
| Host target read | `HOST.VIEW_METRICS` |
| Service metrics read | `SERVICE.VIEW_METRICS` |
| Dashboard create/update/delete | `CLUSTER.MANAGE_USER_PERSISTED_DATA` |
| Datasource create/update/delete/test | `AMBARI.MANAGE_SETTINGS` |
| Target tags, notes, and group assignment | `CLUSTER.MODIFY_CONFIGS` |
| Agent heartbeat | Authenticated agent identity, not a browser permission |

The final matrix must be enforced in both JAX-RS resources and frontend route
or action guards. Tests must prove read-only access, denied mutations, and no
data leakage across clusters.

## Implementation Phases

### Phase 0: Baseline and contract evidence

1. Keep this plan and source inventory aligned with both repositories.
2. Capture source API request/response examples for every selected workflow.
3. Record source success, empty, validation, upstream failure, and retry
   behavior in a new Prometheus baseline document.
4. Build a route-to-endpoint matrix and an entity-to-DDL matrix.
5. Record which source behaviors are intentionally corrected rather than
   preserved.

Exit criteria:

- Every selected route has an owner, target route, endpoint set, permission,
  source evidence, and proposed test.
- No unclassified source file is scheduled for copying.

### Phase 1: Secure server foundation and schema

1. Add typed entities, DTOs, DAOs, and focused service interfaces.
2. Add persistence registration and supported-dialect CREATE DDL.
3. Add and register the current-line upgrade catalog.
4. Add database constraints, indexes, redaction, and cluster ownership.
5. Add DAO, converter, schema-upgrade, and authorization tests.

Exit criteria:

- Fresh-schema and upgrade tests pass.
- Entity registration starts without JPA validation errors.
- Credentials are not exposed through DTO serialization or logs.

### Phase 2: Datasource and query APIs

1. Implement datasource CRUD and connection testing.
2. Implement Prometheus instant/range/batch query and metadata APIs.
3. Use current Ambari HTTP facilities with strict proxy controls.
4. Add upstream timeout, authentication failure, malformed JSON, partial batch,
   and recovery tests.
5. Do not alter `MetricsPropertyProviderProxy` until the query layer is stable.

Exit criteria:

- API contract tests cover success and all defined failures.
- No authentication filter bypass is present.
- No endpoint or credential literal from the source remains.

### Phase 3: VictoriaMetrics and built-in resources

1. Port and normalize VictoriaMetrics service definitions and scripts.
2. Merge package metadata for supported operating systems.
3. Package built-in metric definitions, icons, and dashboards.
4. Implement idempotent datasource and dashboard provisioning without
   replacing user-created or previously deployed `3.0_metrics` records.
5. Add Python service-script tests and resource import tests.

Exit criteria:

- VictoriaMetrics install, start, stop, status, service check, restart, and
  reinstall paths are covered.
- Re-running provisioning creates no duplicate datasource or dashboard.
- Existing multi-datasource and dashboard fixtures survive an upgrade and
  remain readable by the native API.

### Phase 4: Native React datasource and query workflows

1. Add the Monitoring route group and authorization-aware sidebar entries.
2. Add `monitoringApi.ts` with typed datasource and query contracts.
3. Port datasource list/create/edit/delete/test behavior using target controls.
4. Port PromQL query, datasource selection, time range, refresh, chart/table
   modes, empty state, and error recovery.
5. Add route, API, component, cancellation, and permission tests.

Exit criteria:

- No source shell, Ant Design, Umi Request, Moment, or Router 5 code is present.
- Query changes cancel stale requests and cannot overwrite current results.
- Desktop and mobile screenshots have no clipping or overlapping controls.

### Phase 5: Native dashboards and shared charts

1. Port dashboard list, filtering, create, clone, delete, import, and export.
2. Port dashboard layout, variable resolution, panel query execution, time
   range, refresh, tooltip, and empty states.
3. Port chart sharing through a protected native route.
4. Port built-in HDFS, HBase, and YARN dashboards. Semantically migrate the
   source Categraf and Telegraf process, host, and Linux templates into `Linux
   Fleet Overview` and `Linux Host Detail`; do not retain their metric names,
   duplicate layouts, or collector-specific assumptions.
5. Add panel serialization, dashboard recovery, partial query failure, and
   visual regression tests.

Exit criteria:

- Source dashboard payloads either render directly or pass through a tested,
  deterministic migration function.
- A single failed panel does not blank the dashboard.
- Imported built-in dashboards resolve their datasource and variables.
- `Linux Fleet Overview` covers cluster-wide capacity, utilization, and Top-N
  host signals. `Linux Host Detail` defaults its `host` regex variable to `.*`
  and applies `host=~"${host}"` together with the required cluster and host-target
  labels.
- The Agent may aggregate bounded `/proc` values required by these dashboards,
  including total process threads, but does not reproduce per-process Categraf
  labels or other unbounded process identity.

### Phase 6: Targets, cluster metrics, and service metrics

1. Port target list, filters, status, tags, notes, metadata, and business-group
   assignment without `plus:` features.
2. Integrate cluster dashboard panels at `/main/dashboard/metrics`.
3. Integrate service dashboards at the existing service Metrics tab.
4. Build an explicit Ambari metric-property-to-PromQL mapping for supported
   cluster, host, and service metrics.
5. Replace the prototype special-case provider with complete dispatch,
   validation, configuration discovery, and fallback behavior.
6. Cover NameNode HA state and other control-flow metrics explicitly; do not
   silently skip them.

Exit criteria:

- HDFS, HBase, YARN, host, and cluster dashboard workflows have mapped metric
  coverage and no hidden AMS dependency.
- Read permissions are enforced at cluster, host, and service levels.
- Stopped, missing, stale, and recovering monitoring targets render
  predictably.

### Phase 7: Ambari Agent collection branch integration

1. Complete Phases 0 through 6 on `prometheus-react-migration` before touching
   the separate `prometheus-agent-integration` worktree.
2. Merge the two branches only after both have focused passing tests.
3. Align Agent metric names and identity labels with the migrated built-in
   dashboards and target APIs.
4. Configure VictoriaMetrics to scrape the protected Agent metrics endpoint.
5. Add upgrade, mixed-version, unavailable-Agent, duplicate-target, and
   restart recovery tests.

Exit criteria:

- No Categraf runtime dependency remains.
- A rolling upgrade can temporarily contain old and new Agents without losing
  existing datasource or dashboard access.
- Agent collection failure does not prevent custom datasources from querying.

### Phase 8: Cutover, cleanup, and parity evidence

1. Run the source-to-target workflow, endpoint, entity, resource, and route
   matrices.
2. Verify that all AMS and Ganglia server, Agent, Stack, and React paths have
   been removed.
3. Update `docs/frontend-refactor/react-current` and parity evidence.
4. Run license, secret, dependency, build, test, and visual checks.

Exit criteria:

- Every inventory item is marked migrated, intentionally rewritten, or
  excluded with evidence.
- No `/api/n9e`, microfrontend mount, Ember monitoring route, source credential,
  or hard-coded source endpoint remains.
- Fresh install, server upgrade, service restart, page refresh, failed query,
  retry, and datasource recovery paths pass.

## Test Strategy

### Server

- JAX-RS resource tests for payload validation, status codes, authorization,
  redaction, and error mapping
- DAO tests for cluster scoping, pagination, mappings, uniqueness, deletion,
  and empty results
- Upgrade catalog tests for all tables, columns, sequences, indexes, foreign
  keys, and repeated execution
- Prometheus client tests for instant/range/vector/matrix/scalar/string results,
  metadata, malformed responses, timeouts, authentication failures, and batch
  partial failures
- Built-in import tests for resource discovery and idempotency
- Python tests for VictoriaMetrics configure/start/stop/status, package
  conditions, credentials, and service checks
- Final-merge Agent tests for scrape serving, lifecycle, metric labels, mixed
  versions, and VictoriaMetrics target discovery

### Frontend

- API tests for URL, method, headers, query encoding, abort signals, and typed
  response mapping
- Route and sidebar tests for read and mutation permissions
- Datasource form and test-connection tests
- PromQL query cancellation, stale response, empty data, malformed data,
  timeout, retry, and datasource-change tests
- Dashboard payload migration, variable, panel query, layout, import/export,
  clone, share, and partial failure tests
- Target pagination, filtering, tag/note/group mutation, empty result, and
  heartbeat recovery tests
- Cluster and service metrics tests for supported mappings and missing data
- Playwright screenshots at representative desktop and mobile viewports

### Suggested verification commands

Run exact focused commands as each phase lands and record their output in the
PR description. The expected final command set is:

```bash
cd ambari-web/latest
npm install
npm test
npm run lint
npm run build

mvn -pl ambari-server -Dtest='*Metrics*,*Prometheus*,*Datasource*,*Dashboard*,*Target*,*UpgradeCatalog*' test
mvn -pl ambari-server -DskipTests package
mvn -pl ambari-server,ambari-web -DskipTests apache-rat:check
```

Python stack tests and Playwright commands must be added once their concrete
test files exist. Commands that are not run must not be reported as passing.

## Completeness Audits

### Source-to-target audit

- Compare selected source active route imports with target feature files.
- Compare source frontend endpoint literals with native API client methods.
- Compare source JAX-RS methods with selected target API contracts.
- Compare source entities with target persistence registration and DDL.
- Compare source built-in resources with packaged files and import results.
- Compare VictoriaMetrics configuration references with stack service
  dependencies and package metadata.
- After the final branch merge, compare Agent metric families and labels with
  every built-in dashboard query and target identity rule.

### Negative scans

The final tree must have no migration-owned occurrences of:

```text
/api/n9e
react-mfe
plus:
umi-request
antd
moment
react-router-dom@5
hard-coded monitoring hostnames or credentials
```

### Security audit

- Secret scan source-derived files before staging.
- Verify datasource list/read DTOs are redacted.
- Verify server errors and logs do not contain datasource credentials or full
  authorization headers.
- Test blocked proxy destinations and redirects.
- Test cross-cluster access denial and mutation denial.
- Verify heartbeat authenticates an agent and cannot be used anonymously.

### License audit

- Add ASF headers to new Ambari-owned source and documentation.
- Preserve compatible third-party copyright notices where code is adapted.
- Update `NOTICE.txt` when the retained Nightingale-derived code requires it.
- Do not solve RAT failures by broadly excluding source directories.

## Reviewable Commit Plan

Use a real JIRA key before committing implementation. Keep tests with the
behavior they verify.

1. `<JIRA>: Add Prometheus migration baseline and contracts`
2. `<JIRA>: Add metrics persistence and schema upgrade support`
3. `<JIRA>: Add secure datasource and Prometheus query APIs`
4. `<JIRA>: Add VictoriaMetrics runtime support`
5. `<JIRA>: Add native React datasource and PromQL workflows`
6. `<JIRA>: Add native React dashboards and shared charts`
7. `<JIRA>: Add target and service metrics integration`
8. `<JIRA>: Integrate Ambari Agent Prometheus collection`
9. `<JIRA>: Complete Prometheus cutover and parity evidence`

Generated or broad evidence updates belong in the final commit where
practical. Each intermediate commit must compile and pass its focused tests.

## Confirmed Implementation Defaults

1. Datasource mutations use the existing `AMBARI.MANAGE_SETTINGS`
   authorization in the first implementation.
2. The implementation derives the exact current-line upgrade catalog boundary
   from the PR 4180 baseline and adds tests for that registered transition.
3. Chart-share reads require an authenticated Ambari session in the first
   implementation; anonymous signed links are deferred.
4. The initial VictoriaMetrics service topology is single-node.
5. Existing `3.0_metrics` datasource rows, supported datasource types,
   dashboard rows, identifiers, and payloads must be preserved in place.
6. Ambari Agent collection is merged only after the core migration is
   complete; Categraf is not migrated.
