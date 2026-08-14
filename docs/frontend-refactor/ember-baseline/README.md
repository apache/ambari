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

# Ambari Web Ember Feature Baseline

This document set describes the non-Metrics user features and backend APIs of `ambari-web/classic` on the `frontend-refactor` branch. It is the authoritative legacy baseline for subsequent React gap analysis and feature parity acceptance. When static records conflict with source code or runtime behavior, recheck the source and verify runtime behavior; the documents must not override facts.

## Baseline Information

| Item | Value |
| --- | --- |
| Git branch | `frontend-refactor` |
| Git commit | `8ac5c5a1346687bc46b4651c42b07237fe0e9ca9` |
| Legacy frontend | `ambari-web/classic`, early Ember `Em.Router/Em.Route` architecture |
| React comparison project | `ambari-web/latest` |
| REST default prefix | `/api/v1` |
| Static baseline size | 288 non-Metrics named AJAX definitions, 394 included call sites (27 dynamic, 3 unregistered), 19 direct HTTP call sites, 56 browser network candidates, 5 client-config downloads, 160 route fragments, 299 template actions, and 1002 stable feature IDs |

The commit is the analysis starting point and does not restrict the documents to that commit. Rerun the extractor and review generated differences after changes to the legacy frontend.

## Scope

Included:

- Ember features in `ambari-web/classic/app`, including login, sessions, global navigation, Background Operations, cluster installation, Hosts, Services, Configs, Alerts, Stack/Versions/Upgrade, Kerberos, HA/Federation, and Views.
- Page visibility, button permissions, feature flags, service/component/stack prerequisites, wizard recovery, failure retry, and confirmation modals.
- Named `App.ajax` requests and dynamic dispatch; `App.HttpClient`/`XMLHttpRequest`/jQuery AJAX calls that bypass the named registry; browser navigation/downloads; and STOMP/WebSocket/SockJS realtime channels.
- Controller, route, template, mixin, view, model, mapper, and test locations that provide evidence of legacy behavior.

Explicitly excluded:

- All Metrics capabilities, including Dashboard Metrics, Cluster Metrics, Host/Service Metrics, Heatmap, Horizon Chart, AMS timeline queries, metric charts, and metric-data export.
- Dashboard/Service Widget layout, creation, editing, sharing, and deletion that depend on metric definitions.
- The AngularJS Admin Console under `ambari-admin/src/main/resources/ui/admin-web`. User, group, role, cluster-permission, and View-instance management belong to another legacy frontend and are not Ember features; this document records only Ember navigation to Admin View.
- Backend capabilities that exist in the API but are never referenced by the legacy Ember frontend.

Note: HA prerequisite checks may read `metrics/...` fields, such as the NameNode checkpoint time. This supports the HA operational workflow rather than Metrics presentation and is therefore included.

## Document Navigation

| Document | Contents |
| --- | --- |
| [00-methodology.md](00-methodology.md) | Evidence levels, feature-record fields, API extraction rules, and React comparison method |
| [01-auth-shell.md](01-auth-shell.md) | Login, SSO, local login, sessions, global loading, permissions, and navigation |
| [02-background-dashboard.md](02-background-dashboard.md) | Background Operations, polling, non-Metrics Dashboard, and configuration history |
| [03-hosts.md](03-hosts.md) | Hosts list, bulk operations, details, components, configs, alerts, versions, logs, and Add Host wizard |
| [04-services-configs.md](04-services-configs.md) | Services navigation, service actions, summary, configs, config groups, Add Service, and Move Master |
| [05-alerts.md](05-alerts.md) | Alert Definitions, instances, notifications, groups, create/edit/delete, and permissions |
| [06-stack-upgrades-admin.md](06-stack-upgrades-admin.md) | Stack/Versions, repositories, version installation, upgrade/downgrade, upgrade history, and Admin cluster settings |
| [07-cluster-installation.md](07-cluster-installation.md) | Cluster installation wizard Steps 0 through 10, recovery, validation, deployment, and completion |
| [08-kerberos.md](08-kerberos.md) | MIT/AD/IPA/Manual Kerberos, eight-step enablement, disablement, identities, keytabs, and KDC credentials |
| [09-namenode-journalnode-ha.md](09-namenode-journalnode-ha.md) | NameNode HA, JournalNode Management, checkpoint, and rollback |
| [10-rm-ranger-ha.md](10-rm-ranger-ha.md) | ResourceManager HA and Ranger Admin HA |
| [11-federation-hawq.md](11-federation-hawq.md) | NameNode/Router Federation and the HAWQ Standby long workflow |
| [12-views.md](12-views.md) | Views list, long and short URLs, iframe, View-only users, and Admin View navigation |
| [13-permissions-flags.md](13-permissions-flags.md) | Unified index of permissions, feature flags, and service/stack/component/status conditions |
| [14-react-gap-matrix.md](14-react-gap-matrix.md) | Statuses, scenarios, and review gates for later React comparison |
| [15-five-pass-audit.md](15-five-pass-audit.md) | Inputs, findings, corrections, and remaining risks from the five-pass reverse audit |
| [api/README.md](api/README.md) | API catalog entry point, calling conventions, and common payload semantics |

## Generated Evidence Catalog

| File | Purpose |
| --- | --- |
| [generated/ajax-endpoints.md](generated/ajax-endpoints.md) | Non-Metrics named AJAX definitions, fixed/dynamic methods, separate dynamic-URL markers, call sites, and excluded Metrics requests |
| [generated/ajax-endpoints.json](generated/ajax-endpoints.json) | Structured version for scripts and subsequent React gap analysis |
| [generated/ajax-calls.md](generated/ajax-calls.md) | 394 call sites, dynamic-request candidate resolution, and 3 unregistered legacy calls |
| [generated/direct-http-calls.md](generated/direct-http-calls.md) | Direct HTTP calls that bypass `App.ajax` |
| [generated/browser-network-entrypoints.md](generated/browser-network-entrypoints.md) | Browser navigation, reload, downloads, dynamic images/iframes, and local-window entry points; ordinary built static assets are explicitly excluded |
| [generated/client-config-downloads.md](generated/client-config-downloads.md) | Contracts for five client-config resource scopes |
| [generated/realtime-channels.md](generated/realtime-channels.md) | STOMP transport, 11 destinations, payloads, lifecycle, and failure boundaries |
| [generated/permissions.md](generated/permissions.md) | Permission names actually consumed by the legacy code and their call sites |
| [generated/feature-flags.md](generated/feature-flags.md) | `App.supports` feature flag names and call sites |
| [generated/routes.md](generated/routes.md) | Non-Metrics route fragments and definition locations |
| [generated/template-actions.md](generated/template-actions.md) | Non-Metrics template actions and occurrence locations |
| [generated/feature-index.md](generated/feature-index.md) | Machine index of stable IDs from the authored modules |
| [generated/api-by-module](generated/api-by-module) | Heuristic candidate index grouped by request name and caller path using broad matching; it mixes, duplicates, and omits entries across modules and is not a complete API catalog |

Regenerate with:

```bash
node docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
```

The extractor uses only built-in Node.js modules and does not require `npm install`. Do not edit generated files manually.

## Usage Rules

1. Use the stable feature IDs in the module documents as the authority for whether a legacy feature exists.
2. Review network contracts jointly using `generated/ajax-endpoints.json`, `ajax-calls.json`, `direct-http-calls.json`, `browser-network-entrypoints.json`, and `realtime-channels.json`; do not replace the combined review with `api-by-module` or any single catalog.
3. Module documents describe user behavior; the generated catalog describes static facts. When they conflict, return to the source and verify runtime behavior rather than guessing.
4. React comparison must not compare only route or component filenames; compare entry points, permissions, success results, failure paths, asynchronous requests, and recovery behavior.
5. A feature may be marked `COVERED` only when its React UI, API, permissions, error handling, and tests are all equivalent.
