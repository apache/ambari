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

# Prometheus Migration Source Inventory

## Purpose

This inventory defines the source boundary for migrating the Prometheus-based
monitoring implementation from `am-pro` into the native React frontend. It is
not a request to merge or cherry-pick the source branch. The source and target
histories diverged substantially, and the source history contains broad
formatting, unrelated product work, an Ember/React bridge, and prototype
security changes.

## Verified Revisions

| Role | Repository | Revision | Meaning |
| --- | --- | --- | --- |
| Target | `apache/ambari` | `f24fd0c4bf557ff04ca0eddd4a9756d8b20eec2a` | GitHub PR 4180 head and worktree baseline |
| Source | `am-pro` | `e4c7db384c58f9fe6baca08a52906557ace5f573` | `origin/3.0_metrics` final snapshot |
| Shared metrics line end | `am-pro` | `d483cefa6df8005f6d1d4d394d68a51e69c8cd12` | Last commit shared by `eee_prom` and `3.0_metrics` |
| Source branch fork from `eee` | `am-pro` | `849edb1a9d6cdb4c2693cb9a348602e75f3204d2` | `eee` does not contain the Prometheus line |

Remote refs were checked before creating the worktree. The target worktree is
`/Users/jialiang/PRJS/ambari-prometheus-react` on branch
`prometheus-react-migration`.

## Source Timeline

### Query and management backend

| Commit | Purpose | Migration treatment |
| --- | --- | --- |
| `52097340a0` | Initial Prometheus property provider | Use as behavior evidence; rewrite configuration, metric selection, validation, and credentials |
| `a85f755fd2` | Metrics controller, API, DAO, entity, and service foundation | Selectively port domain models and contracts |
| `7d3bc6c6ff` | Datasource corrections | Use the final datasource state |
| `2de06d9b6f` | Boards, built-in integrations, converters, and resource payloads | Port the required dashboard and built-in integration model |
| `bbf5dcca14` | Prometheus query support | Port query behavior through Ambari HTTP facilities |
| `55b08bb600` | Metadata endpoints | Port labels, label values, series, and metadata behavior |
| `6bd0c0e043` | Static integration API and Categraf correction | Port only native metrics functionality; exclude fake compatibility APIs |
| `b7edcbaba8` | Metrics resource packaging | Reapply against current assembly and API registration |
| `84afff672c` | Chart share support | Port chart-share persistence and REST contract |
| `31f7282cb2` | Target machine support | Port target lifecycle and indexes with cluster scoping |
| `e887409247` | Business group support | Port only the business-group behavior used by dashboards and targets |
| `c36395e79d` | Target DAO/service performance fixes | Use final DAO semantics |
| `377bff3f10` | Target corrections | Use final target state |
| `fb627d5c33` | Empty target handling | Preserve empty-list behavior and add tests |
| `e2d34b75ab` | Built-in dashboards and automatic import | Port final JSON resources and idempotent import behavior |
| `4155861fe7` | Automatic datasource creation | Rework into an idempotent native provisioning flow |
| `7711ac1a3a` | Session annotation changes and dashboard filename correction | Review transaction boundaries instead of copying annotation removal |
| `1d3964b146` | Datasource correction | Use final datasource DAO state |
| `d483cefa6d` | Base URL correction | Replace with same-origin native API paths |
| `2311ab2f05` | Metrics proxy correction | Use as evidence for HA-state handling; do not copy the prototype dispatch logic |

### Runtime services and resources

| Commit | Purpose | Migration treatment |
| --- | --- | --- |
| `0bb6a52850` | Initial Nightingale stack service | Reference only; not required by the selected native React workflows |
| `b7a1f40669` | VictoriaMetrics service and Nightingale corrections | Port the VictoriaMetrics service dependency closure |
| `ad2701e273` | Categraf split into its own service | Reference only; Categraf is replaced by the separate Ambari Agent collection branch |
| `db5d028ae5` | Categraf service corrections | Reference only; do not port Categraf scripts or service checks |
| `d333362f6d` | Ubuntu 24 package support | Include applicable VictoriaMetrics package metadata only |
| `af993f950d` | VictoriaMetrics DEB correction | Include the final package mapping |

The final source uses Categraf, but the target collection implementation is
being developed independently with Ambari Agent on branch
`prometheus-agent-integration`, based on the same PR 4180 revision. Do not merge
or copy that worktree until the core monitoring migration is complete. The
selected UI routes do not require the Nightingale server.

### Frontend behavior source

| Commit | Purpose | Migration treatment |
| --- | --- | --- |
| `614b96ea26` | Imports the Nightingale-derived React application | Use only as behavior and domain-code source |
| `bc978448ef`, `dbd5ea2d29` | Adds initially missing frontend files | Include when required by an active-route import closure |
| `e887409247` through `fb627d5c33` | Business group and target behavior | Port selected user-visible workflows |
| `43405195d2` | Chart selector correction | Preserve in dashboard tests |
| `6e72a8efd3` | Time-series parameter parsing correction | Preserve in query serialization tests |
| `d9effd1a17` | Target behavior correction | Preserve final list/filter/edit semantics |
| `a9f792680c` | Empty dashboard binding state | Preserve the empty state |
| `9efb490bf4` | Chart tooltip correction | Preserve in visual checks |
| `4f44bef8ec` | Dashboard/target menu correction | Use final active route behavior |

The source frontend is Nightingale 7.7.0 based and uses React 17, React Router
5, Vite 2, Ant Design 4, Moment, Umi Request, Tailwind, and optional `plus:`
modules. None of its application shell or build configuration is a target
artifact.

## Selected User-Visible Workflows

| Source route | Source area | Target route or consumer |
| --- | --- | --- |
| `/monitoring/help/source` | Datasource list, create, edit, delete, and test | `/main/monitoring/data-sources` |
| `/monitoring/metric/explorer` | PromQL query, labels, series, time range, and chart/table result | `/main/monitoring/explorer` |
| `/monitoring/dashboards` | Dashboard list, import, clone, delete, and visibility | `/main/monitoring/dashboards` |
| `/monitoring/dashboards/:id` | Dashboard view and edit | `/main/monitoring/dashboards/:dashboardId` |
| `/monitoring/machines` | Target list, metadata, tags, note, and business group | `/main/monitoring/targets` |
| `/monitoring/chart/:ids` | Shared chart rendering | `/main/monitoring/shared-charts/:shareIds` |
| `/services/:serviceName/metrics` | Service dashboard metrics | Existing `/main/services/:serviceName/metrics` tab |
| `/dashboard/metrics` | Cluster dashboard metrics | Existing `/main/dashboard/metrics` tab |

## Selected Source Paths

### Backend code to port or adapt

- `ambari-server/src/main/java/org/apache/ambari/server/api/services/metrics/`
- `ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/client/`
- `ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/model/`
- `ambari-server/src/main/java/org/apache/ambari/server/controller/metrics/prometheus/`
- `ambari-server/src/main/java/org/apache/ambari/server/service/metrics/`
- Metrics-owned classes under `ambari-server/src/main/java/org/apache/ambari/server/orm/dao/`
- Metrics-owned classes under `ambari-server/src/main/java/org/apache/ambari/server/orm/entities/`
- JSON converters under `ambari-server/src/main/java/org/apache/ambari/server/orm/converters/`

Integration points must be manually merged into the current versions of
`ApiModule`, `ControllerModule`, `AbstractProviderModule`,
`MetricsPropertyProviderProxy`, `Configuration`, `persistence.xml`, assembly
descriptors, DDL files, and upgrade catalog registration.

### Persistence objects in the final source

- Datasources and nested HTTP, TLS, and authentication settings
- Boards and board payloads
- Board-to-business-group mappings
- Chart shares
- Built-in components, metrics, and payloads
- Business groups
- Targets and target-to-business-group mappings

The source CREATE DDL defines `datasource`, `board`, `board_payload`,
`chart_share`, `board_busigroup`, `builtin_components`, `builtin_payloads`,
`builtin_metrics`, `busi_group`, `target`, and `target_busi_group`. It updates
only MySQL and PostgreSQL. The target must add supported-dialect CREATE DDL and
an upgrade catalog with tests.

Existing `3.0_metrics` datasource and board rows are migration inputs, not
disposable prototype data. The target API and schema upgrade must preserve all
supported datasource variants and existing dashboard identifiers and payloads.

### Runtime resources

- `ambari-server/src/main/resources/metrics/integrations/Linux/`
- `ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services/VICTORIAMETRICS/`
- The corresponding `stack_packages.json` entries and assembly resource copy

### Frontend behavior references

- `ambari-metrics-web/src/pages/dashboard/`
- `ambari-metrics-web/src/pages/metric/explorer/`
- `ambari-metrics-web/src/pages/datasource/` (mounted at
  `/monitoring/help/source`)
- `ambari-metrics-web/src/pages/targets/`
- `ambari-metrics-web/src/pages/serviceMetrics/`
- `ambari-metrics-web/src/pages/chart/`
- Required renderer, PromQL input, time-range, datasource selector, and table
  utilities reachable from those entry points
- `ambari-metrics-web/src/services/dashboardV2.ts`
- `ambari-metrics-web/src/services/metric.ts`
- `ambari-metrics-web/src/services/metricViews.ts`
- `ambari-metrics-web/src/services/targets.ts`
- Only the datasource functions needed from `ambari-metrics-web/src/services/common.ts`
- Open-source datasource plugins required by active datasource and dashboard
  workflows: Prometheus, Elasticsearch, Loki, Jaeger, and TDengine

## Explicit Exclusions

- The `eee` branch, which does not contain the Prometheus line
- AI, model, chat, and MCP commits unique to `eee_prom`
- Full-repository formatting commits `d0df18f869`, `420c52a5d2`, and
  `02799dc27f`
- `ambari-web/app/` Ember route, template, view, menu, and CSS bridge changes
- `ambari-web/ambari-web-vite/` experiments
- `ambari-metrics-web/copy.sh`, standalone Vite configuration, lockfiles, and
  React 17 application entry points
- Nightingale login, user, user-group, permission, notification, alert,
  recording-rule, task, trace, and site-settings pages
- Optional `plus:` modules and proprietary placeholders
- `FakeApiService` and all `/api/n9e` compatibility endpoints
- Commit `8d287a3ad0` authentication bypass changes
- Hard-coded network endpoints, usernames, passwords, and request credentials
- The prototype global property-validation bypass and single-metric dispatch
- Categraf stack service definitions, packages, scripts, and templates; the
  separate Ambari Agent branch replaces this collection path after the core
  migration is complete
- Unrelated Pulsar, StarRocks, Ubuntu, Python formatting, AI, and service work

## Known Source Defects to Correct

1. The property provider routes only a special CPU property to Prometheus and
   falls back to AMS for other metrics. The target removes that fallback and
   the legacy provider entirely.
2. Metric property validation is globally bypassed in the prototype.
3. The source contains hard-coded endpoint and administrative request
   credentials.
4. Authentication filters were loosened to make the mixed frontend work.
5. Datasource proxying needs URL validation, strict timeouts, response limits,
   and authorization to avoid server-side request forgery.
6. Datasource secrets can be exposed through ordinary entity serialization.
7. The schema is not complete for all Ambari database dialects and has no
   upgrade path.
8. Metrics-owned Java files have incomplete ASF header coverage.
9. The selected source frontend has no focused tests and contains unresolved
   `plus:` imports in active target-related areas.
10. The source proxy correction still skips NameNode HA state rather than
    supplying an equivalent Prometheus mapping.

## Inventory Completion Rule

A source file is eligible for migration only when it is reachable from a
selected workflow, required by a selected backend contract, or required to
operate VictoriaMetrics. Each implementation commit must update the workflow
and API matrices in the migration plan. Files outside this inventory require
an explicit plan amendment before they are copied or reimplemented. Ambari
Agent collection files enter the target only during the final branch merge.
