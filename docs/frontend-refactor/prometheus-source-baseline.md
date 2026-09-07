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

# Prometheus Monitoring Source Baseline

## Evidence Boundary

This baseline records behavior from `am-pro` `origin/3.0_metrics` at
`e4c7db384c58f9fe6baca08a52906557ace5f573`. Evidence was read from the
detached worktree `/Users/jialiang/PRJS/am-pro-3.0-metrics-source`; the user's
`eee` checkout was not changed.

The target is PR 4180 at
`f24fd0c4bf557ff04ca0eddd4a9756d8b20eec2a`. The target implementation lives
in `/Users/jialiang/PRJS/ambari-prometheus-react` and integrates directly into
`ambari-web/latest`.

Existing `3.0_metrics` datasource and dashboard rows are production migration
inputs. Their identifiers, supported datasource types, and payloads must be
preserved. Categraf is not a target artifact. The separate Ambari Agent branch
is integrated only after the core migration is complete.

## Route Matrix

| Source route | Target route | Active source owner | Target permission |
| --- | --- | --- | --- |
| `/monitoring/help/source` | `/main/monitoring/data-sources` | `pages/datasource` | `CLUSTER.VIEW_METRICS` |
| `/monitoring/help/source/:action/:type/:id?` | `/main/monitoring/data-sources/:action/:type/:id?` | `pages/datasource/Form.tsx` | Read plus `AMBARI.MANAGE_SETTINGS` for mutation |
| `/monitoring/metric/explorer` | `/main/monitoring/explorer` | `pages/explorer/Metric` | `CLUSTER.VIEW_METRICS` |
| `/monitoring/dashboards` | `/main/monitoring/dashboards` | `pages/dashboard/List` | `CLUSTER.VIEW_METRICS` |
| `/monitoring/dashboards/:id` | `/main/monitoring/dashboards/:dashboardId` | `pages/dashboard/Detail` | `CLUSTER.VIEW_METRICS` |
| `/monitoring/chart/:ids` | `/main/monitoring/shared-charts/:shareIds` | `pages/chart` | Authenticated `CLUSTER.VIEW_METRICS` |
| `/monitoring/machines` | `/main/monitoring/targets` | `pages/targets` | `HOST.VIEW_METRICS` |
| `/dashboard/metrics` | Existing `/main/dashboard/metrics` | `pages/metrics/dashboard` | `CLUSTER.VIEW_METRICS` |
| Service metrics bridge | Existing `/main/services/:serviceName/metrics` | `pages/metrics/service` | `SERVICE.VIEW_METRICS` |

## Datasource Contract

### Persisted fields

The `datasource` table is preserved with these source-compatible columns:

| Column | Meaning | Compatibility rule |
| --- | --- | --- |
| `id` | Datasource identity referenced by dashboard JSON | Never renumber during upgrade |
| `name` | User-visible unique name | Preserve existing uniqueness behavior |
| `description` | User description | Preserve text |
| `category` | Query/rendering category | Preserve unknown values |
| `plugin_id` | Plugin metadata identifier | Preserve even when target has no bundled editor |
| `plugin_type` | Concrete datasource type | Preserve unknown values and open-source types |
| `plugin_type_name` | User-visible type label | Preserve text |
| `cluster_name` | Source cluster association | Preserve; validate against target cluster access |
| `settings` | Plugin-specific JSON object | Round-trip unknown keys |
| `http` | URL, timeout, TLS, headers, and proxy JSON | Round-trip unknown keys; redact secrets on reads |
| `auth` | Authentication JSON | Preserve encrypted or credential-backed value; never echo secrets |
| `status` | `enabled` or `disabled` | Disabled sources cannot be queried |
| `is_default` | Default selector flag | Preserve and enforce at most one default per scope |
| Audit columns | Creation/update identity and epoch seconds | Preserve existing values on upgrade |

The source UI contains editors for Prometheus, Elasticsearch, Loki, Jaeger,
and TDengine. The schema also permits unknown `plugin_type` values. The target
therefore uses a generic datasource DTO with type-specific editors layered on
top; it must not use a closed Java enum for persisted plugin types.

### Source endpoints and target treatment

| Source API | Behavior | Target treatment |
| --- | --- | --- |
| `GET /metrics/datasource/brief` | Selector list | Preserve envelope and add cluster scoping |
| `POST /metrics/datasource/query` | Filter by type/category/name | Preserve filters; return redacted DTOs |
| `POST /metrics/datasource/list` | Full list | Preserve compatibility method and add canonical `GET` |
| `POST /metrics/datasource/desc` | Read by numeric ID | Preserve compatibility method and add canonical `GET /{id}` |
| `POST /metrics/datasource/upsert` | Create/update generic JSON | Preserve accepted plugin fields; source update is broken because its request omits `id` |
| `POST /metrics/datasource/status/update` | Enable/disable | Preserve behavior with mutation authorization |
| `DELETE /metrics/datasource/{id}` | Delete | Reject when a dashboard still references the datasource |
| `POST /metrics/datasource/plugin/list` | Available editor metadata | Return the five open-source plugins and retained unknown installed types |
| `GET /metrics/datasource/{id}/metadata` | Source returns an empty stub | Replace with type-aware connection metadata |
| `POST /metrics/datasource/{id}/test` | Missing in source | Add a bounded, redacted connection check |
| `/api/n9e/proxy/{id}/...` | Expected by non-Prometheus renderers but absent from the native source backend | Replace with protected `/metrics/datasource/{id}/proxy/...` |

## Query Contract

The source native endpoints are retained for dashboard payload compatibility:

| Method and path | Required input | Success data |
| --- | --- | --- |
| `GET /metrics/{id}/api/v1/query` | `query`, optional epoch `time` | Prometheus query response |
| `GET /metrics/{id}/api/v1/query_range` | `query`, epoch `start`, `end`, positive `step` | Prometheus matrix response |
| `POST /metrics/query-instant-batch` | Datasource ID and query items | Ordered per-query results |
| `POST /metrics/query-range-batch` | Datasource ID, range, query items | Ordered per-query results |
| `GET /metrics/{id}/api/v1/labels` | Datasource ID | Label names |
| `GET /metrics/{id}/api/v1/label/{name}/values` | Label and optional matcher/range | Label values |
| `GET /metrics/{id}/api/v1/series` | One or more `match[]`, optional range | Series label maps |
| `GET /metrics/{id}/api/v1/metadata` | Optional metric and limit | Metric metadata |

Range queries reject missing fields, non-positive steps, reversed ranges, and
more than 11,000 points. The target keeps these validation rules. It replaces
source stack-trace response bodies with stable error codes and messages.

All proxy paths validate datasource state, scheme, resolved destination,
redirects, request method, path, timeouts, response size, and forwarded
headers. Custom datasources remain usable when VictoriaMetrics or Agent
collection is unavailable.

## Dashboard Contract

### Persisted fields

The `board` and `board_payload` source tables remain authoritative. The target
does not translate rows into Ambari widgets during schema upgrade.

| Object | Compatibility rule |
| --- | --- |
| `board.id` | Preserve; route and share references depend on it |
| `board.group_id` | Preserve business-group association |
| `board.name` and `ident` | Preserve names and stable lookup identifiers |
| `tags` | Preserve source space-delimited representation |
| `public`, `public_cate`, `built_in`, `hide` | Preserve numeric flag semantics |
| Audit fields | Preserve epoch seconds and identities |
| `display_locations` | Preserve source comma-delimited locations |
| `board_payload.id` | Must continue to equal the owning board ID |
| `board_payload.payload` | Round-trip the complete JSON string unchanged unless a versioned mapper is explicitly invoked |

Dashboard payloads may reference datasource IDs and include Prometheus or
Elasticsearch panel definitions. Unknown panel, variable, transform, override,
and datasource keys must survive read/update/import/export cycles. A renderer
may show an unsupported-panel state, but it must not discard that panel.

### Source endpoints

| API | Behavior to preserve or correct |
| --- | --- |
| `GET /metrics/boards` | List and query dashboards; target adds cluster/owner filtering |
| `GET /metrics/public-boards` | Source incorrectly returns all boards; target filters visibility and still requires authentication |
| `POST /metrics/boards` | Create metadata and payload atomically |
| `GET /metrics/board/{id}` | Return metadata plus `configs` payload |
| `GET /metrics/board/{id}/pure` | Return metadata without payload |
| `PUT /metrics/board/{id}` | Update metadata without destroying omitted fields |
| `PUT /metrics/board/{id}/configs` | Replace payload string without normalizing unknown JSON |
| `POST /metrics/board/{id}/clone` | Clone metadata and payload with a new ID |
| `POST /metrics/boards/clone` | Batch clone with per-board failures |
| `DELETE /metrics/board/{id}` | Delete board, payload, mappings, and owned shares transactionally |
| `GET/POST /metrics/share-charts` | Authenticated share read/create; use actual user, cluster, ownership, and expiry policy |

## Targets And Built-Ins

The target workflow retains target list, metadata, tags, note, business-group,
delete, and empty-list behavior. The source heartbeat endpoint is Categraf
specific and is not migrated during the core phase. Its identity contract is
reconciled with the Ambari Agent branch only at the final merge.

Built-in components, metrics, and dashboard payloads are packaged as immutable
classpath resources. Import is idempotent and cannot overwrite an existing
user or upgraded `3.0_metrics` dashboard with the same numeric ID.

## Failure And Recovery Matrix

| Condition | Required target behavior |
| --- | --- |
| No datasource | Show an actionable empty selector; do not issue a query |
| Disabled datasource | Reject query with a stable conflict response |
| Missing datasource | Return not found without leaking another cluster's row |
| Bad credentials | Return connection/query failure without returning stored secret |
| Timeout or malformed upstream response | Keep current screen state and allow retry |
| Route or datasource changes during query | Abort stale request; stale results cannot replace current results |
| One failed batch panel | Mark that panel failed and render successful panels |
| Empty dashboard payload | Render an editable empty dashboard |
| Unknown dashboard panel or datasource type | Preserve JSON and show unsupported state |
| Existing data during upgrade | No row replacement, ID change, or payload normalization |
| Repeated built-in provisioning | No duplicate datasource, dashboard, or payload |
| VictoriaMetrics unavailable | Custom datasources and dashboard management remain available |

## Intentional Corrections

The following source behavior is evidence of intent but is not copied:

1. No hard-coded monitoring endpoint, username, password, or administrative
   request credential.
2. No authentication filter bypass, fake profile, fake permissions, or
   `/api/n9e` compatibility shell.
3. No API logging of full datasource request bodies or authentication JSON.
4. No ordinary DTO response containing datasource passwords, tokens, private
   headers, or client keys.
5. No WebFlux, Reactor Netty, Lombok, Spring transaction annotations, or
   secondary persistence provider.
6. No global metric-property validation bypass or CPU-only Prometheus routing.
7. No stack traces or upstream authorization headers in HTTP responses.
8. No silent deletion or rewriting of unknown datasource settings or
   dashboard payload fields.

## Completion Evidence

Each implementation phase updates this document or the companion migration
plan with target paths and tests. Completion requires automated comparison of
source routes, frontend service calls, REST methods, persisted columns,
built-in resources, and active dashboard datasource renderers.
