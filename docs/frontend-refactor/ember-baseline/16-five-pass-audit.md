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

# Five-Pass Independent Reverse Audit of the Ember Baseline

This report records five independent reverse-audit passes over non-Metrics features and APIs in `ambari-web/classic` at baseline commit `8ac5c5a1346687bc46b4651c42b07237fe0e9ca9`. Each pass re-enumerated source facts from a different entry point and recorded the input size, findings, corrections incorporated into the module documents, and risks that static analysis cannot eliminate.

Here, "passing" means only that generated artifacts, source locations, and the authored baseline are statically consistent. This pass did not run the legacy Karma suite or perform end-to-end validation against a real Ambari Server, Agent, KDC, HA topology, or legacy HAWQ stack; it therefore cannot claim runtime behavior passes.

## Audit Results

| Pass | Inputs and scope | Major findings | Corrections made | Remaining risks |
| --- | --- | --- | --- | --- |
| 1. Route and entry-point reverse audit | 160 non-Metrics route records from 21 router/route files; because nested wizards reuse `/stepN`, only 64 distinct literal fragments exist. Compared menus, route guards, post-click checks, and direct deep links item by item | The Installer route hard-checks `AMBARI.ADD_DELETE_CLUSTERS`; Add Service checks permission + flag in both the UI and route; Add Host checks `HOST.ADD_DELETE_HOSTS` only in the UI, and `/host/add` has no authorization or feature gate. Also confirmed that menu conditions in several Views, HA, Federation, and HAWQ paths do not equal route guards, and that NNF/RBF empty-set checks incorrectly pass with zero ZooKeeper Servers or JournalNodes | Documented UI gates, route gates, click checks, and direct-URL boundaries in the installation, Views, permissions, NNHA, RM/RA, and Federation/HAWQ modules; corrected the Admin View browser URL to `#/adminView` while distinguishing the route pattern `/adminView`; recorded the Add Host deep-link gap and Federation empty-set defect | The extractor records nested fragments rather than every concatenated final URL; dynamic redirects, browser history/hash behavior, and server-side deep-link authorization still require real-browser and Server validation |
| 2. Template-action and JavaScript-behavior reverse audit | 299 distinct Handlebars action names and 587 template occurrences; then manually traced view clicks, controller methods, observers, timers, dynamic actions, and route lifecycles | Host Re-upgrade has a static UI/controller path, but its request name is unregistered, its state was removed from the Server enumeration, and its payload still hard-codes the old stack; `App.ajax.send` only warns and returns `null` for an unregistered name. Also found the fully hidden HDPWIN automatic bootstrap UI, the Add Host `Skip host checks` dialog without a Cancel notice, Step 3 confirming only generic warnings, and control flows not expressible as template actions, such as the Kerberos restart observer | Classified Re-upgrade as `STATIC_ONLY / LEGACY_BROKEN / UNREGISTERED` and explicitly excluded it as a React API; recorded the conditional broken Rollback button; documented HDPWIN PowerShell, hidden Agent-user validation defects, host-check categorization, Kerberos pending/delayed restart, Views singleton, and wizard close/recovery as JavaScript-only semantics | Action extraction covers only static Handlebars actions, not runtime-injected action names, third-party View content, DOM plugin callbacks, or future stack extensions; these boundaries still require browser instrumentation |
| 3. Network and API reverse audit | 288 included non-Metrics named AJAX definitions and 95 excluded Metrics definitions; 394 `App.ajax.send` call sites consisted of 364 registered, 27 dynamic, and 3 unregistered calls. Of the 27 dynamic call sites, 23 were closed-set and 4 were open-boundary cases; all 45 unique candidate names were registered. All registry HTTP methods were actually static, so 0 dynamic methods should be extracted; 3 included and 1 excluded Metrics definition also provide dynamic URLs through `format()`. Audited 19 direct HTTP calls, 56 browser network candidates, 5 client-config download scopes, and 2 realtime transports, 11 destinations, and 4 lifecycle contracts | The old extractor incorrectly marked 65 fixed methods as `DYNAMIC` because of negative-lookahead backtracking and missed quoted `'type'`, causing `service.item.smoke` to be reported incorrectly as GET; it also omitted three runtime URLs. The browser catalog omitted JWT/preferred-path navigation, both full-page reload forms `window.location.reload()` and global `location.reload()`, the home-page link, the new UI link, and the View icon. The realtime audit also found an incorrect server field in the Alert Group delete push, asymmetric task terminal-state/cleanup handling, and heartbeat/authentication boundaries | Changed parsing to read top-level `type`/`url` from the object returned by `format()`; separated methods from dynamic URLs and froze each AJAX source-object hash, the ordered hash of the original 1000 IDs, and module-candidate content hashes; completed the browser entry-point catalog and explicitly excluded built static assets; retained manual resolution contracts for 27 dynamic call sites; froze 10 static + 1 dynamic destination, 11 subscribe sites, 1 addHandler, 1 removeHandler, 1 business unsubscribe, and 142 frontend/backend source and test locations for realtime channels; required authoritative API review to combine five layers of network evidence | 4 open dynamic wrapper/model boundaries may expand with future data; the module candidate index is not guaranteed to be complete; caller-supplied URLs, proxy/auth, actual payload encoding, STOMP wire serialization, events lost during disconnection, and final server authorization can be confirmed only through runtime traffic and fault injection |
| 4. Permission, flag, state, and recovery reverse audit | 38 permission names and 147 static uses, including 130 `isAuthorized` and 17 `havePermissions` uses; 23 `App.supports` flags and 58 uses; also manually indexed the three runtime gates `App.stackVersionsAvailable`, `App.upgradeHistoryAvailable`, and `App.enableDigitalClock`. Rechecked state, owner, persistence, Retry, Skip, and rollback across installation, Kerberos, NNHA/JN, RM/RA, and Federation/HAWQ | Comma-separated permissions are ORed; upgrade exceptions contaminate the complete OR expression; the broad Service Actions OR exposes multiple actions without per-action RBAC rechecks. Installation Review deletes all clusters and existing repository versions, with different lock-page, continue, and no-rollback branches for GET/DELETE/VDF/queue; Step 9 Retry is visible only for `INSTALL FAILED`. The `.always()` and blocking semantics in the Kerberos resource/credential chain are inconsistent, and RBF resets maintenance only for Routers whose state is `OFF` | Split permission responsibilities, broad OR behavior, upgrade contamination, runtime gates, and authoritative Server authorization in the unified permission index; added the installation Step 8 submission state machine and failure matrix; documented exact owner/persistence, reachable Retry/Skip/Complete states, Kerberos descriptor/credential propagation, Router maintenance boundaries, and known deadlock points in the wizard modules | The permission generator recognizes only static strings; non-transactional deletion and partial creation require fault injection; dynamic helper arguments, stack metadata, server privileges, cross-user ownership, refresh/Server restart, KDC sessions, and concurrent state changes require role matrices and breakpoint-recovery E2E validation |
| 5. Test and evidence-consistency reverse audit | `classic/test` contains 546 JS files and 500 `_test.js` files. The manifest has 499 entries: 498 `_test` references, 497 unique references, and 1 initialization module; `test/utils/config_test` is duplicated. 52 files contain 81 skip markers: 59 `describe.skip` plus 22 `it.skip`. Modules `01` through `14` contain 1154 unique stable IDs; authored module tables contain 303 full source-file references, normalized to 136 unique paths, and every case-sensitive path check succeeds | 3 `_test.js` files on disk are not loaded by the manifest: active non-Metrics `test/data/configs/wizards/secure_mapping_test.js`; `test/mappers/configs/stack_config_properties_mapper_test.js`, which contains its own `describe.skip`; and excluded Metrics `test/views/main/charts/heatmap/heatmap_rack_test.js`. The new HDPWIN automatic bootstrap and global Version Definition cleanup chain lack runtime tests sufficient to prove real PowerShell, partial deletion, and recovery results; Service Theme server/provider and React runtime gaps are recorded rather than assumed covered | Added bidirectional source/test evidence for each module and recorded skipped, unloaded, and untested content as gaps rather than success evidence; the generator ensures feature-ID index consistency, and the validator checks the generated catalog, source line numbers, exact case of complete source paths, test manifest, Markdown table column counts, and links | This pass did not install dependencies or run Karma, so it cannot claim that the legacy suite passes; stubbed unit tests, unloaded/skipped content, and real Server/Agent/Windows/stack/KDC/browser behavior remain part of runtime acceptance |

## Service Theme Extension Audit

The standalone Service Theme module was checked through five independent
evidence paths after the original audit:

| Pass | Evidence path | Result incorporated into the baseline |
| --- | --- | --- |
| 1. REST/provider | Theme routes, provider projection, query expansion, predicate filtering, and normal REST ordering | Distinguished single 404 from per-service batch isolation, removed default-first ordering as a wire contract, and recorded named-file and primary-key provider defects |
| 2. Server model/inheritance | Descriptor loading, Jackson binding, every theme model merge method, resolved map publication, and server tests | Replaced semantic-validation assumptions with parse/bind limits and recorded the exact merge/removal matrix, null-parent hazards, and missing tests |
| 3. Ember compiler/renderer | Theme AJAX/mapping order, graph IDs, grids, nested tabs, all 14 configuration Widgets, raw fallback, and conditions | Separated legacy parity from metadata-contract corrections and converted identity, observer, value-attribute, and Widget defects into executable React cases |
| 4. Consumer flows | Installed/Host Configs, comparison, Installer Step 7, Add Service, DB action polling, permissions, fallback, and retry | Recorded five distinct consumer shapes, canonical save state, exact category behavior, and single-versus-batch failure differences |
| 5. Fixture/test reverse audit | Descriptor declarations, custom `themes-dir`, raw JSON, current conditions/tabs/Widget counts, Ember/server tests, and React test obligations | Counted API-reachable artifacts separately from loose files, restored MAPREDUCE2 `themes-mapred`, excluded Metrics, and froze 40 detailed test groups |

## Frozen Conclusions

After the original five passes and the Service Theme extension audit, the
authored modules are frozen at 1154 globally unique stable feature IDs. The
original set includes `INST-MODE-011` for HDPWIN PowerShell automatic bootstrap
and `INST-8-009` for the global Version Definition/Repository Version cleanup
chain; Module 14 contributes 152 Service Theme contract/test IDs. The network
baseline remains frozen at 288 non-Metrics AJAX definitions and 394 call sites;
authoritative review must combine [AJAX definitions](generated/ajax-endpoints.md),
[AJAX calls](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md),
[browser entrypoints](generated/browser-network-entrypoints.md), and
[realtime channels](generated/realtime-channels.md). `generated/api-by-module/`
is for heuristic candidate discovery only and cannot replace the combined
inventory above.

The stable-ID compatibility contract checks more than current uniqueness: the
validator filters the two allowed additions from Modules 01-13 and computes
SHA-256 `21699bfe0be07648e5124cfd640d8593a83d840ca19de455c40712b74f1f1a23`
over the ordered JSON array of the original 1000 IDs. It independently freezes
the 152 Module 14 IDs at SHA-256
`7dc625b3b77624012c4f541ff456f23c606f81f3c491e49a82dfcc467574a1c1`.
Renaming, deleting, or reordering either set fails validation. Each AJAX
definition also freezes the hash of its `ajax.js` source object, and each
heuristic module-candidate page freezes a content hash composed of
name/method/endpoint/inputKeys/callers, preventing stale generated artifacts
with the same counts from passing.

The three `UNREGISTERED` calls cannot become React endpoints: Host Re-upgrade is a broken legacy branch for obsolete state, and the two NNHA rollback requests are in unwired controllers. Neither realtime transport has a unified REST polling fallback after failure. These negative facts are also part of the compatibility baseline; React comparisons must mark them as known legacy defects, intentional fixes, or runtime gaps rather than assuming existing capability.

The realtime contract references 142 frontend/backend source and test locations, normalized to 131 unique `source:line` entries. New evidence covers the `/api/*` security filter, the allow-any-origin pattern, the Alert Group delete listener, task client/server terminal-state sets, and subscribe/unsubscribe/disconnect registry cleanup. These static locations prove that the consumer chain exists; they do not prove successful proxying, authentication, heartbeat negotiation, message serialization, or disconnection recovery in a real deployment.

## Machine-Frozen Counts

The validator compares the following JSON field by field with the current generated directory, authored feature tables, legacy test manifest, and realtime contract. Missing or extra fields, or any numeric drift, causes validation to fail.

```json
{
  "featureIds": 1154,
  "routeRecords": 160,
  "routeSourceFiles": 21,
  "distinctRouteFragments": 64,
  "templateActionNames": 299,
  "templateActionOccurrences": 587,
  "ajaxDefinitions": 288,
  "excludedMetricsDefinitions": 95,
  "ajaxCalls": 394,
  "registeredAjaxCalls": 364,
  "dynamicAjaxCalls": 27,
  "unregisteredAjaxCalls": 3,
  "resolvedClosedDynamicCalls": 23,
  "resolvedOpenDynamicCalls": 4,
  "uniqueDynamicCandidates": 45,
  "directHttpCalls": 19,
  "browserNetworkEntrypoints": 56,
  "clientConfigDownloadScopes": 5,
  "permissions": 38,
  "permissionUses": 147,
  "isAuthorizedUses": 130,
  "havePermissionsUses": 17,
  "featureFlags": 23,
  "featureFlagUses": 58,
  "runtimeGates": 3,
  "realtimeTransports": 2,
  "realtimeDestinations": 11,
  "realtimeStaticDestinations": 10,
  "realtimeDynamicDestinations": 1,
  "realtimeLifecycleContracts": 4,
  "realtimeSubscribeSites": 11,
  "realtimeAddHandlerSites": 1,
  "realtimeRemoveHandlerSites": 1,
  "realtimeUnsubscribeSites": 1,
  "realtimeLocationOccurrences": 142,
  "realtimeUniqueLocations": 131,
  "sourceReferenceOccurrences": 303,
  "sourceReferenceUniquePaths": 136,
  "testJsFiles": 546,
  "diskTestModules": 500,
  "manifestEntries": 499,
  "manifestTestReferences": 498,
  "uniqueManifestTestReferences": 497,
  "manifestInitializationModules": 1,
  "duplicateManifestReferences": 1,
  "diskTestsNotLoaded": 3,
  "manifestTestsMissingOnDisk": 0,
  "skipFiles": 52,
  "describeSkipMarkers": 59,
  "itSkipMarkers": 22,
  "skipMarkers": 81
}
```

## Verification Commands

Run the following commands from the repository root. The `generated/` hashes must be identical after two consecutive extractor runs; the validator's `warnings` and `errors` must both be empty.

```bash
node docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
find docs/frontend-refactor/ember-baseline/generated -type f | sort | xargs shasum
node docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
find docs/frontend-refactor/ember-baseline/generated -type f | sort | xargs shasum
node docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs
node --check docs/frontend-refactor/ember-baseline/tools/extract-ember-baseline.mjs
node --check docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs
rg -n '[ \t]+$' docs/frontend-refactor/ember-baseline
git diff --check
```

Legacy Karma, real-cluster, and external-system validation are outside the static commands above. Before declaring a React module `COVERED`, run the installation modes, four Kerberos modes, HA/Federation, upgrade, permission-role, refresh-recovery, and fault-injection tests required by the complex scenario matrix in [React gap matrix](15-react-gap-matrix.md) and the Service Theme executable contract.
