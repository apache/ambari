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

# Ember Backend API Catalog

## Authoritative Combined Catalog

No single generated file can serve as the complete API catalog. Authoritative review must combine five layers of evidence: named request definitions, actual call sites, direct HTTP, browser network entry points, and realtime channels, while continuing to apply the non-Metrics scope rules.

- [../generated/ajax-endpoints.md](../generated/ajax-endpoints.md): Non-Metrics `App.ajax` request definitions, methods, URLs, `format()` input keys, and call sites.
- [../generated/ajax-endpoints.json](../generated/ajax-endpoints.json): Structured definition-side contract with complete `formatExpression`, used to recover dynamic URLs, body, header, and dataType.
- [../generated/ajax-calls.md](../generated/ajax-calls.md): Each in-scope `App.ajax.send(...)` call site, call-parameter keys, callbacks, and dynamic request expression.
- [../generated/ajax-calls.json](../generated/ajax-calls.json): Structured call-side contract for later automated comparison with React query/mutation calls.
- [../generated/realtime-channels.md](../generated/realtime-channels.md): 2 STOMP transports, 11 destinations, payload consumption, subscription lifecycle, reconnection, and REST reconciliation.
- [../generated/realtime-channels.json](../generated/realtime-channels.json): Structured realtime-channel contract matching the manual-audit contract, including frontend/backend source and test locations.
- [../generated/direct-http-calls.md](../generated/direct-http-calls.md) / [JSON](../generated/direct-http-calls.json): `HttpClient`, jQuery AJAX, and native XHR call sites that bypass the named registry; `MIXED` includes only non-metric fields.
- [../generated/browser-network-entrypoints.md](../generated/browser-network-entrypoints.md) / [JSON](../generated/browser-network-entrypoints.json): Browser network candidates such as `window.open`, download links, and redirects; manual review must distinguish remote requests from local document windows.
- [../generated/permissions.md](../generated/permissions.md): Static permission names and all call sites.
- [../generated/feature-flags.md](../generated/feature-flags.md): Feature flags actually consumed and all call sites.
- [../generated/api-by-module](../generated/api-by-module): Heuristic candidate view generated with broad matching by request name and caller path. It may mix, duplicate, or omit module requests and is not the authoritative API catalog.

The generator writes catalog counts into the files, and the validator compares them with the README. Counts are not hard-coded again on this page, avoiding stale numbers after legacy frontend changes.

## URL and Method Rules

1. A `real` URL is prefixed with `/api/v1` by default.
2. An omitted `type` means `GET`.
3. `format(data, opt)` can rewrite the method, URL, body, header, and dataType. The current registry has no genuinely dynamic methods; fixed methods are extracted jointly from the top-level `type` and the top-level `type` in the object returned by `format()`. Caller-supplied/expression URLs are marked separately with `hasDynamicUrl=true` and `DYNAMIC_URL`; the Method column's `DYNAMIC` marker must not also serve as a URL marker.
4. When a GET URL exceeds 2048 characters, the actual request is `POST`, with header `X-Http-Method-Override: GET` and body `{"RequestInfo":{"query":"..."}}`.
5. Most JSON mutations still use `Content-Type: text/plain`; this is existing behavior for Knox compatibility.
6. Ambari asynchronous mutations commonly return a request ID; the UI then polls `/clusters/{cluster}/requests/{id}`, stages, and tasks.
7. `DYNAMIC` calls retain the original runtime expression and provide manual resolution through `candidateRequestNames`, `dispatchCondition`, and `resolutionStatus`; closed-set and open-wrapper boundaries must not be treated as unregistered.
8. An `UNREGISTERED` static request name does not send HTTP. The current 3 cases are two unwired NN HA rollback legacy calls and a Host Component Re-upgrade dead branch that depends on the Server-deleted `UPGRADE_FAILED` state and is unreachable in normal production responses. None can serve as a React endpoint.
9. STOMP channels do not use `App.ajax`; when both transports fail, there is no global REST polling fallback, so snapshot, post-event convergence, and unsubscribe behavior must be checked for each destination.

When implementing a call, first read its request definition and `formatExpression` from `ajax-endpoints.json`, then use `ajax-calls.json` to find the actual parameter keys, callbacks, and source for the corresponding business call. Also inspect `direct-http-calls.json` and `browser-network-entrypoints.json`, and consult `realtime-channels.json` for push behavior. Copying only the endpoint loses requests that bypass the registry, downloads/navigation, `RequestInfo.context`, operation level, predicates, dynamic methods, post-push mapper updates, and failure recovery behavior.

## Common Payload Structures

| Scenario | Key structures |
| --- | --- |
| Service/Component state change | `RequestInfo.context`, `operation_level`, `Body.ServiceInfo`, or `Body.HostRoles` |
| Bulk host/component operation | `RequestInfo.query` or URL predicate, with target `HostRoles` in the body |
| Custom command | `RequestInfo.command`, `context`, `operation_level`, `resource_filters` |
| Save configuration | Cluster PUT, `Clusters.desired_config`, or a config-group/desired-config collection |
| Create request schedule | `RequestSchedule`, `RequestScheduleBatch`, and the batch request list |
| Upgrade | Repository version, direction, upgrade type, and request options; pause/retry/terminate through upgrade PUT |
| Alert | `AlertDefinition`, `AlertTarget`, and `AlertGroup` resources |
| Wizard | Staged cluster/service/component/host creation, followed by request/task polling after deployment |

## API Migration Review

React must review more than URL and method. It must also check:

- Query predicates, `fields`, `minimal_response`, and pagination parameters; these affect mapper behavior and page completeness.
- `RequestInfo.context` and `operation_level`; they directly affect Background Operations display and server lock granularity.
- Different branches for responses with status 200, 201, 202, or an empty body.
- Request ID parsing, polling terminal states, abort, retry, and failed-task logs.
- Special headers/dataType for KDC, Knox, oversized GET requests, and browser downloads.
- Pre-operation checks for insufficient permissions, upgrade/wizard exclusion, maintenance mode, and similar conditions.

The "backend requests" column in module documents references this catalog by request name. When a feature lists multiple request names, their order generally corresponds to loading, mutation, and polling/refresh.
