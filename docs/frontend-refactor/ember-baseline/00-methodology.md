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

# Baseline Organization Method

## Objectives

This baseline answers four questions:

1. What can users do in the legacy Ember frontend?
2. Under which permissions, feature flags, service states, and wizard states can they do it?
3. Which backend interfaces do the operations call, and how are success, failure, and asynchronous execution handled?
4. Which source code and tests provide evidence for this behavior?

Completion is measured by behavioral equivalence, not by whether the pages look similar.

## Functional Record Fields

Each function in the module documentation uses a stable ID and should include the following fields where possible:

| Field | Meaning |
| --- | --- |
| ID | Stable identifier within the module; do not renumber it for later React comparison |
| Entry point | route, menu, button, popup, or automatically triggered point |
| Preconditions/permissions | permission, feature flag, installed service, component state, and upgrade/wizard mutual-exclusion conditions |
| User action | Atomic action that the user can perform, rather than a page name |
| Success result | Page changes, model refresh, background request, navigation, or download |
| Exception/boundary | Disabled state, confirmation, server error, polling termination, retry, cancellation, and recovery |
| Request | `App.ajax` request name; use `DIRECT:<位置>` for direct requests |
| Evidence | Source location in route/controller/template/mixin/test |

## Evidence Levels

| Marker | Definition |
| --- | --- |
| `CONFIRMED` | At least two types of evidence in route/controller/template/API/test corroborate each other |
| `STATIC_ONLY` | Static code exists, but requires validation with a real Ambari Server, specific stack, or external service |
| `CONDITIONAL` | Exists only when permission, feature flag, service/component, or stack conditions are met |
| `PLACEHOLDER` | Only a route or outlet shell exists; no complete page implementation was found in the current legacy tree |
| `OUT_OF_SCOPE` | Explicitly excluded content such as Metrics or the separate AngularJS Admin Console |

Module tables default to `CONFIRMED` or `CONDITIONAL`. Other states are stated explicitly in the entry.

## Interface Identification

The legacy Ember frontend has four types of network path:

1. `App.ajax.send({name: ...})`: The request name is registered in `app/utils/ajax/ajax.js`. The default method is `GET`, the default prefix is `/api/v1`, and `format()` can dynamically override the method, URL, body, and header.
2. Direct HTTP that bypasses the named registry: This includes `App.HttpClient`, native `XMLHttpRequest`, and jQuery AJAX; URLs are often constructed dynamically in a controller, view, or util.
3. Browser navigation and downloads: Quick Links, View iframes, client config downloads, log windows, and locally generated files may use `window.open`, `href`, or an iframe without going through either of the preceding request wrappers.
4. STOMP realtime channels: A native WebSocket connects to `{ws|wss}://{host}{:port}/api/stomp/v1/websocket`, then falls back to SockJS at `{http|https}://{host}{:port}/api/stomp/v1` after the first failure. See `generated/realtime-channels.json` for destinations, payloads, subscription/unsubscription, reconnection, and REST reconciliation.

When a GET URL exceeds `2048` characters, `App.ajax` changes it to `POST` and sends `X-Http-Method-Override: GET`, placing the query expression in `RequestInfo.query`. The React migration must not copy only the apparent GET method.

In the interface inventory:

- URLs do not include the default `/api/v1`; an override of `apiPrefix` is listed separately.
- `DYNAMIC` in the Method column means only that the HTTP method depends on runtime data. When the URL depends on a `format()`/caller expression, it is recorded separately as `hasDynamicUrl=true` and marked `DYNAMIC_URL` in Markdown. The two markers must not be conflated.
- `formatExpression` in `ajax-endpoints.json` preserves the complete `format()` function from the registry; it is the authoritative definition-side evidence for mutation bodies, headers, dataType, and dynamic URLs.
- `ajax-calls.json` records the request-name expression, top-level inline `data` keys, callback type, and source location for each call site; it is the authoritative call-site evidence for how the same request receives parameters in different business contexts.
- "0 callers" means that no same-name string reference was found in the legacy `app/`. The definition may be legacy, dynamically constructed, or test-only and must not be treated directly as a user feature.
- Dynamic request objects and dynamic request names cannot be safely consolidated by syntax alone. Call sites remain marked `DYNAMIC`, while `tools/contracts/dynamic-ajax-resolutions.mjs` audits their candidate requests, dispatch conditions, and open boundaries individually. In `ajax-calls.json`, `RESOLVED_CLOSED` means the candidate set is closed, while `RESOLVED_OPEN_BOUNDARY` means the current legacy callers have been enumerated but the wrapper/model/mixin can still receive other runtime values.
- `UNREGISTERED` means that a static request name is not actually in the AJAX registry; `App.ajax.send` issues a warning and returns `null` without sending HTTP. User-reachable legacy defects must be distinguished from unreachable legacy controllers; React must not invent an endpoint.
- The same REST endpoint may be used by multiple request names with different payloads, contexts, and operation levels. The migration must not deduplicate by URL alone.
- `generated/api-by-module/` is not a complete module interface inventory. The generator concatenates request names and caller paths and classifies them with broad regular-expression heuristics. Shared requests may be duplicated or misclassified across modules, and module-specific requests may be assigned to another page or to "cross-cutting and pending manual classification"; therefore, absence from a module page does not prove that the legacy UI lacks the request.

Authoritative interface verification cannot rely on any single inventory or request definition table. It must jointly inspect `ajax-endpoints` (named request definitions), `ajax-calls` (actual call sites and dynamic dispatch), `direct-http-calls` (HTTP that bypasses the registry), `browser-network-entrypoints` (navigation, downloads, and iframes), and `realtime-channels` (WebSocket/SockJS). `api-by-module` is used only to find candidate entry points and cannot replace these five layers of evidence; all layers remain subject to the Metrics exclusion rules below.

## Metrics Exclusion Rules

The following are excluded from the functional and interface baseline:

- Request names or source modules explicitly belonging to metrics, heatmap, timeline, or chart data.
- Shared requests whose callers are all located in Metrics/Heatmap/metric Widget code.
- Dashboard/Service metric Widget management.

The following are retained:

- metrics fields read by operations such as HA and decommission to determine safety conditions.
- Non-Metrics request progress, host health, component state, alert state, and upgrade progress.
- Configuration history, log search, background operations, and service checks.
- When the same direct HTTP response contains both topology/state and metric fields, retain only operational fields such as component topology, state, maintenance, stale config, HA state, and Active/Standby; metric values do not enter the baseline merely because they share a response.
- Although `hosts.ips` and `hiveServerInteractive.getStatus` are called by the mapper named `service_metrics_mapper`, they are actually used for host/IP mapping and the Hive Interactive Active/Standby quick-link indicator, respectively, and are retained as non-metric operational capabilities.

## Static Extraction Limitations

- Early Ember routes are nested objects, so the generator lists only route fragments and does not calculate final URLs.
- When `name`, URL, or method is passed through a variable, static extraction may record only a dynamic expression.
- Dynamic Handlebars actions, internal view click handlers, observers, and timers may not appear in the action inventory.
- Stack service descriptors, theme JSON, and server-side feature metadata can change the visible services, components, configuration items, and commands.
- Capabilities such as Knox, LDAP/Kerberos, Log Search, and HAWQ require a real external environment to validate their complete results.

## React Comparison Steps

1. Build the matrix with one row per feature ID, rather than organizing it by React file.
2. For each row, verify the route/entry point, visibility conditions, permissions, operations, payload, asynchronous state, error path, and recovery behavior.
3. Trace back from the React API layer to feature IDs to identify two types of gap: an interface with no entry point and a page with no request.
4. Arrange real-cluster scenario tests for `STATIC_ONLY` and `CONDITIONAL` items.
5. Record confirmed React test paths in the matrix without changing the legacy factual description.
