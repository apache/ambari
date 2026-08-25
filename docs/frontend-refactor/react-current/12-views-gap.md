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

# React Views Parity Review

This document compares the React Views shell with
`ember-baseline/12-views.md` and the executable Ember sources. It covers View
discovery, list and detail routes, iframe hosting, View-only navigation, and
the transition into the separate AngularJS Admin View. It does not claim to
replace the applications hosted inside View iframes or the Admin View CRUD
surface. Metrics remain outside this review.

Static alignment below means that routes, request contracts, state handling,
and focused tests agree with the source baseline. It does not mean that a real
View artifact, Ambari Server authorization response, proxy deployment, or
browser iframe lifecycle has been accepted at runtime.

## Final Feature ID Status

| ID | Final status | React evidence and remaining acceptance |
| --- | --- | --- |
| `VIEW-SCOPE-001` | `NEEDS_RUNTIME_VALIDATION` | `src/Utils/viewUtils.ts` flattens definition -> version -> instance data without deduplicating by View name; `src/Utils/viewUtils.test.ts` covers two versions and visible instances. Real multi-version payload order remains pending. |
| `VIEW-SCOPE-002` | `NEEDS_RUNTIME_VALIDATION` | `src/Utils/viewUtils.ts` maps only icon, label, visibility, version, description, View name, short URL, instance name, and server context path. Admin-side instance lifecycle and properties remain outside this shell. |
| `VIEW-SCOPE-003` | `NEEDS_RUNTIME_VALIDATION` | `src/components/ViewIframe/ViewIframe.tsx` uses the server-returned context path as a same-origin browser URL and does not treat `/views/...` as REST. A real hosted View must verify the Web context and session. |
| `VIEW-SCOPE-004` | `NOT_APPLICABLE` | AngularJS Admin View user, group, permission, repository, and View-instance CRUD is a separate frontend and is intentionally not reimplemented by this module. |
| `VIEW-ROUTE-001` | `NEEDS_RUNTIME_VALIDATION` | `/main/views` renders `src/screens/Views/ViewsListPage.tsx` after the shared provider resolves, with loading, empty, and failure states. Browser refresh remains part of the runtime matrix. |
| `VIEW-ROUTE-002` | `NEEDS_RUNTIME_VALIDATION` | `/main/views/:viewName/:viewVersion/:instanceName/*` resolves the complete identity in `src/screens/Views/ViewDetails.tsx` and hosts the matched instance. |
| `VIEW-ROUTE-003` | `NEEDS_RUNTIME_VALIDATION` | `/main/view` renders the same directory in the full-width `src/layout/Views.tsx` shell. Visual equivalence of the wide layout remains a browser check. |
| `VIEW-ROUTE-004` | `NEEDS_RUNTIME_VALIDATION` | `/main/view/:viewName/:shortName/*` resolves by View name plus short URL and uses the same detail component and iframe. |
| `VIEW-ROUTE-005` | `NEEDS_RUNTIME_VALIDATION` | `/adminView` is an authenticated transition route guarded by `src/components/AdminViewRouteGuard.tsx`; `src/screens/Authentication/AdminViewRedirect.tsx` performs full-page `location.replace`. |
| `VIEW-ROUTE-006` | `NEEDS_RUNTIME_VALIDATION` | The iframe and Admin redirect issue browser navigation to `/views/...` outside the React route tree. Proxy-root and server Web-context behavior require runtime evidence. |
| `VIEW-LIST-001` | `NEEDS_RUNTIME_VALIDATION` | `src/api/viewApi.tsx` first calls `GET /api/v1/views`, returns an empty collection when no definitions exist, and sends the instance request only when definitions exist; `src/api/viewApi.test.ts` covers both branches. |
| `VIEW-LIST-002` | `NEEDS_RUNTIME_VALIDATION` | `src/Utils/viewUtils.ts` flattens all returned versions and instances in traversal order; the directory has no search, pagination, or initial client sort. Real response scale and order remain pending. |
| `VIEW-LIST-003` | `NEEDS_RUNTIME_VALIDATION` | The instance query retains `system=false`, and `flattenVisibleViewInstances()` includes only truthy `visible` instances with usable identity/context fields. Server deployment and authorization filtering remain authoritative. |
| `VIEW-LIST-004` | `NEEDS_RUNTIME_VALIDATION` | `src/Utils/viewUtils.ts` implements label and description fallbacks, while `src/screens/Views/ViewsListPage.tsx` supplies the bundled default icon and preserves `context_path`. |
| `VIEW-LIST-005` | `NEEDS_RUNTIME_VALIDATION` | The `Your Views` rows show icon, label, version, and description and call `window.open` through `openViewInstance()`; `src/screens/Views/ViewsListPage.test.tsx` and `src/Utils/viewUtils.test.ts` cover the action. |
| `VIEW-LIST-006` | `BEHAVIOR_DIFF` | An empty success still displays `No views`, but `src/screens/Views/ViewsListPage.tsx` now distinguishes request failure with the server message and Retry instead of silently presenting the empty state. |
| `VIEW-LIST-007` | `BEHAVIOR_DIFF` | `src/screens/Views/ViewInstancesContext.tsx` uses explicit promise loading/finally state and a spinner rather than a 50 ms Ember poll; every success, empty result, and rejection releases loading. |
| `VIEW-LIST-008` | `BEHAVIOR_DIFF` | One provider owns discovery for the mounted main shell and explicit Retry replaces the array. This avoids route-specific duplicate loaders, although simultaneous manual retries are not deduplicated or aborted. |
| `VIEW-NAV-001` | `NEEDS_RUNTIME_VALIDATION` | `src/AppLoader.tsx`, `src/layout/Main.tsx`, and `src/Utils/authPolicy.ts` route View-only and installer-denied users to `/main/view` while preserving regular/short View routes. Role and provisioning combinations require live acceptance. |
| `VIEW-NAV-002` | `NEEDS_RUNTIME_VALIDATION` | Installed-cluster `src/components/Navbar.tsx` lists the provider instances, shows `No Views` when empty, and opens the selected View in a new browsing context. |
| `VIEW-NAV-003` | `NEEDS_RUNTIME_VALIDATION` | Both `/main/views` and `/main/view` are authenticated direct routes; React does not add a separate Views sidebar item. |
| `VIEW-URL-001` | `BEHAVIOR_DIFF` | `generateViewUrl()` still prefers the short route and otherwise uses the regular route, but safely encodes every dynamic segment and opens with `noopener,noreferrer`; Classic emitted raw segments. |
| `VIEW-URL-002` | `NEEDS_RUNTIME_VALIDATION` | `findRegularViewInstance()` matches complete returned identity first and retains the proxy-compatible `/views/...` `endsWith` fallback. Knox and non-root contexts remain pending. |
| `VIEW-URL-003` | `NEEDS_RUNTIME_VALIDATION` | `findShortViewInstance()` selects the first instance with the requested View name and short URL; server uniqueness/authorization remains the boundary. |
| `VIEW-URL-004` | `BEHAVIOR_DIFF` | React derives `viewPath` for each route render instead of mutating the shared instance object, preventing residual internal paths from leaking between navigations. |
| `VIEW-URL-005` | `BEHAVIOR_DIFF` | Unknown, hidden, unauthorized, or deleted instances render `View not available` with Return to Views; `src/screens/Views/ViewDetails.test.tsx` proves warm navigation does not reuse the old iframe. |
| `VIEW-URL-006` | `BEHAVIOR_DIFF` | A refreshed deep link rediscovers instances through the provider; discovery failure now has Retry and an unmatched result has a recovery action instead of falling into the legacy stale/undefined outlet path. |
| `VIEW-PATH-001` | `NEEDS_RUNTIME_VALIDATION` | `parseViewPath()` returns the wildcard path or an empty string when no query exists, and `buildViewIframeSrc()` retains the context trailing slash. |
| `VIEW-PATH-002` | `BEHAVIOR_DIFF` | Ordinary query parameters are deterministically forwarded as the iframe query rather than depending on legacy dynamic-parameter parsing. |
| `VIEW-PATH-003` | `NEEDS_RUNTIME_VALIDATION` | Encoded leading-slash paths are parsed with `URLSearchParams`, normalized once, and covered in `src/Utils/viewUtils.test.ts`. |
| `VIEW-PATH-004` | `NEEDS_RUNTIME_VALIDATION` | A `viewPath` plus remaining query parameters becomes the internal path plus query; focused tests cover multiple parameters and order. |
| `VIEW-PATH-005` | `NEEDS_RUNTIME_VALIDATION` | Encoded View hash routes become `#/...` after one leading slash is removed; a real Tez history deep link remains pending. |
| `VIEW-PATH-006` | `BEHAVIOR_DIFF` | Invalid percent encoding no longer throws out of routing; `URLSearchParams` produces a recoverable value and the parser test verifies the call does not throw. |
| `VIEW-PATH-007` | `BEHAVIOR_DIFF` | React recognizes the exact `viewPath` key, so unrelated names containing that substring remain ordinary query parameters. |
| `VIEW-IFRAME-001` | `NEEDS_RUNTIME_VALIDATION` | `src/components/ViewIframe/ViewIframe.tsx` renders a borderless, full-width iframe with `seamless` and `allowFullScreen`; content sizing needs real browser evidence. |
| `VIEW-IFRAME-002` | `NEEDS_RUNTIME_VALIDATION` | `buildViewIframeSrc()` forces `window.location.origin` and appends the server context plus parsed path. Same-origin and proxy behavior remain pending. |
| `VIEW-IFRAME-003` | `NEEDS_RUNTIME_VALIDATION` | View detail adds both React's `contrib-view` class and Classic's `contribview` class and removes both on unmount; View-only routes also use the full-width layout. |
| `VIEW-IFRAME-004` | `BEHAVIOR_DIFF` | React resets the specific iframe to `height:auto`, calculates `max(content scrollHeight, body height - #top-nav - footer)`, and resizes immediately, on load/window resize, and every five seconds. It restores both window and React Views-container scroll positions and deliberately fixes Classic's global iframe selector; dynamic browser layout still needs acceptance. |
| `VIEW-IFRAME-005` | `NEEDS_RUNTIME_VALIDATION` | The component clears its five-second interval and resize listener on unmount; `src/components/ViewIframe/ViewIframe.test.tsx` asserts interval cleanup. |
| `VIEW-IFRAME-006` | `NEEDS_RUNTIME_VALIDATION` | `src/InactivityTimeout.tsx` observes iframe insertion/load and binds mousemove, keypress, and click to same-origin frame windows, with cross-origin access guarded. Real inactivity timing remains pending. |
| `VIEW-IFRAME-007` | `NEEDS_RUNTIME_VALIDATION` | The iframe has no sandbox and retains fullscreen; the focused iframe test pins both attributes. Downloads, popups, clipboard, and retained View applications require browser acceptance. |
| `VIEW-IFRAME-008` | `BEHAVIOR_DIFF` | React adds source/attempt-aware loading, navigation error capture, a 30-second timeout, explicit failure UI, and Retry through a fresh iframe with the same URL. Browsers commonly emit `load` for iframe HTTP 404/500 and cannot expose content-script failures, so those cases remain runtime/server-handshake boundaries. |
| `VIEW-IFRAME-009` | `NEEDS_RUNTIME_VALIDATION` | The shell writes only the initial path and does not mirror iframe history or listen for location/postMessage changes, matching the legacy ownership boundary. |
| `VIEW-ONLY-001` | `NEEDS_RUNTIME_VALIDATION` | Empty/sole-`VIEW.USE` users now await supports, Ambari properties, and cluster identity before ready, retain keep-alive/inactivity and Views, and skip wizard ownership, user settings, services, hosts, upgrades, background requests, service metadata, and STOMP. Provider tests cover ordering, failure/Retry, and the operational-call exclusions; live session and no-cluster behavior remain pending. |
| `VIEW-ONLY-002` | `NEEDS_RUNTIME_VALIDATION` | An ordinary installed-cluster user receives the Dashboard shell and the shared View provider populates the top dropdown after normal app initialization. |
| `VIEW-ONLY-003` | `NEEDS_RUNTIME_VALIDATION` | A View-only user with no cluster lands on `/main/view`; empty authorization and sole `VIEW.USE` are covered by policy tests, while the real login response matrix is pending. |
| `VIEW-ONLY-004` | `NEEDS_RUNTIME_VALIDATION` | A no-cluster non-View-only user lands on `/adminView`; Admin version lookup failure redirects to `/main/view`. |
| `VIEW-ONLY-005` | `BEHAVIOR_DIFF` | Incomplete-cluster routing preserves either View deep-link form, sends authorized users to `/installer/step0`, and sends unauthorized users to `/main/view`. It does not use Classic's `CLUSTER_CURRENT_STATUS` value to select a specific restored installer state. |
| `VIEW-ONLY-006` | `NEEDS_RUNTIME_VALIDATION` | Every direct `/installer` route without `AMBARI.ADD_DELETE_CLUSTERS` deterministically redirects to `/main/view`, including installed and incomplete clusters; `src/Utils/authPolicy.test.ts` covers both. |
| `VIEW-ONLY-007` | `NEEDS_RUNTIME_VALIDATION` | `clusterProvisioningRedirect()` exempts both `/main/view/...` and `/main/views/...`, and `src/layout/Main.tsx` accepts both prefixes, preserving authenticated deep links. |
| `VIEW-ONLY-008` | `NEEDS_RUNTIME_VALIDATION` | `src/AppLoader.tsx` stores a pre-login relative path. Ordinary users can consume it; View-only landing deliberately selects the directory, retaining Classic's lack of explicit detail restoration. |
| `VIEW-ONLY-009` | `NEEDS_RUNTIME_VALIDATION` | `src/layout/Views.tsx` has no operations sidebar or service provider and exposes only the Views outlet shell. |
| `VIEW-ONLY-010` | `NEEDS_RUNTIME_VALIDATION` | `clusterControls=false` hides notifications, background operations, and the top Views grid in `src/components/Navbar.tsx`. |
| `VIEW-ONLY-011` | `NEEDS_RUNTIME_VALIDATION` | The home icon is marked disabled and has no navigation handler when cluster controls are unavailable. |
| `VIEW-ONLY-012` | `NEEDS_RUNTIME_VALIDATION` | About, Switch Experience, and Sign out remain in the user menu; Manage Ambari is evaluated independently through the Ambari permission expression and operation policy. |
| `VIEW-INIT-001` | `BEHAVIOR_DIFF` | `src/store/UserContext.tsx` probes the cluster endpoint before mounting the authenticated shell, and `src/store/context.tsx` then maps cluster identity for every user. React deliberately makes a separate detail read instead of reusing one saved jqXHR through later initialization. |
| `VIEW-INIT-002` | `NEEDS_RUNTIME_VALIDATION` | `src/store/context.tsx` reads `user-pref-{login}-supports`, catches missing/error responses, and overlays results on defaults before marking the app ready. |
| `VIEW-INIT-003` | `BEHAVIOR_DIFF` | `src/AppLoader.tsx` starts keep-alive after 60 seconds, schedules the next call after completion, and clears the timer whenever authentication unmounts, including failed server logout. Classic explicitly stopped only after successful logout. |
| `VIEW-INIT-004` | `BEHAVIOR_DIFF` | Ambari properties are awaited before ready state and drive inactivity. Failure now displays `Unable to initialize Ambari` with Retry instead of silently breaking the remaining promise chain. |
| `VIEW-INIT-005` | `BEHAVIOR_DIFF` | View discovery is owned by `src/screens/Views/ViewInstancesContext.tsx` after the main route mounts rather than being started in parallel inside the Ambari-properties/cluster-identity chain. The resulting list and retry behavior are explicit. |
| `VIEW-INIT-006` | `NEEDS_RUNTIME_VALIDATION` | View-only now loads cluster identity before ready and then skips wizard ownership, user settings, service/host/alert/upgrade/background models, service metadata, and STOMP. `src/store/context.test.tsx` proves the request order, installed-cluster mapping, failure/Retry, and exclusions; real labeling and inactivity remain pending. |
| `VIEW-INIT-007` | `BEHAVIOR_DIFF` | Provisioning-state routing selects Installer versus Views without preempting View routes, while `src/screens/ClusterWizard/clusterStore/context.tsx` restores `CLUSTER_CURRENT` plus `CLUSTER_STATE`, maps Classic installer state through `resolveRecoveryStep()`, jumps to the recovered step, and exposes Retry on failure. This replaces the fragile `CLUSTER_CURRENT_STATUS` success-only chain rather than reproducing it. |
| `VIEW-X-001` | `NEEDS_RUNTIME_VALIDATION` | React service Quick Links remain a separate URL mechanism; the Views shell handles one only when its final target is a View route. No generic Quick Link is reclassified as an Ambari View. |
| `VIEW-X-002` | `NOT_APPLICABLE` | The unpopulated Classic Hive `viewsToShow` extension point is a legacy placeholder and is not invented in React. |
| `VIEW-X-003` | `NOT_APPLICABLE` | The commented-out generic Service Summary Views panel has no reachable legacy behavior to migrate. |
| `VIEW-X-004` | `NEEDS_RUNTIME_VALIDATION` | React parses `viewPath` on both regular and short routes, and `src/components/ViewLink/ViewLink.tsx` can generate a regular link. The legacy producer is conditional stack/server configuration rather than a guaranteed Views-shell action; a real Tez/config-generated URL remains pending runtime evidence. |
| `VIEW-ADMIN-001` | `NEEDS_RUNTIME_VALIDATION` | No-cluster landing carries explicit router state into `src/components/AdminViewRouteGuard.tsx`, looks up the server version, and falls back to `/main/view` on failure. |
| `VIEW-ADMIN-002` | `BEHAVIOR_DIFF` | Navbar visibility uses the complete Ambari-level OR expression and `/adminView` retains the independent `CLUSTER.UPGRADE_DOWNGRADE_STACK` guard. React improves explicit-route lookup failure by returning to Views instead of leaving an outlet-less state/default modal path. |
| `VIEW-ADMIN-003` | `BEHAVIOR_DIFF` | Manage Versions uses `havePermissions('AMBARI.MANAGE_STACK_VERSIONS')`, remains visible-but-disabled for another wizard owner, confirms exit, and the `page=stackVersions` guard uses that distinct operation policy. Lookup failure returns to Views rather than Classic's global-error path. |
| `VIEW-ADMIN-004` | `BEHAVIOR_DIFF` | `latestServerVersion()` filters unusable values and compares numeric parts, so `2.10` correctly wins over `2.9`; empty data becomes a recoverable failure. Classic used lexical sort and assumed a usable final component. |
| `VIEW-ADMIN-005` | `NEEDS_RUNTIME_VALIDATION` | `adminViewUrl()` removes the `/latest/` segment, preserves a proxy application root, includes the optional Admin page hash, and `location.replace()` avoids retaining the transition route in history. |
| `VIEW-ADMIN-006` | `NEEDS_RUNTIME_VALIDATION` | Ordinary discovery still excludes system versions and Admin navigation independently queries the server version, so it does not depend on a visible `ADMIN_VIEW` list instance. |
| `VIEW-PERM-001` | `BEHAVIOR_DIFF` | `isViewOnlyUser()` classifies empty or sole `VIEW.USE` directly. Classic let upgrade/wizard operation gates contaminate the sole-View test; React intentionally keeps that user in the safer Views shell. |
| `VIEW-PERM-002` | `NEEDS_RUNTIME_VALIDATION` | React does not use backend permission name `VIEW.USER` as a route/list switch and keeps it distinct from authorization ID `VIEW.USE`. Real privilege payloads remain pending. |
| `VIEW-PERM-003` | `NEEDS_RUNTIME_VALIDATION` | Server filtering protects both `/api/v1/views` and the Web context; React's `visible` filter is only a display predicate. Server 403 and mixed instance privilege tests remain pending. |
| `VIEW-PERM-004` | `NEEDS_RUNTIME_VALIDATION` | `AMBARI.MANAGE_VIEWS` is one member of Navbar's Manage Ambari OR expression and does not add Admin CRUD to React. |
| `VIEW-PERM-005` | `NEEDS_RUNTIME_VALIDATION` | The ordinary `/adminView` transition keeps the independent `CLUSTER.UPGRADE_DOWNGRADE_STACK` operation guard. |
| `VIEW-PERM-006` | `NEEDS_RUNTIME_VALIDATION` | `AMBARI.MANAGE_STACK_VERSIONS` alone controls Manage Versions visibility/entry and is not used for ordinary View access. |
| `VIEW-PERM-007` | `NEEDS_RUNTIME_VALIDATION` | `AMBARI.ADD_DELETE_CLUSTERS` selects Installer versus Views for incomplete/direct-installer cases; focused policy tests cover both permission outcomes. |
| `VIEW-PERM-008` | `NEEDS_RUNTIME_VALIDATION` | Another wizard owner sees Manage Versions disabled and policy-aware Admin actions are blocked, while ordinary list/detail routes remain usable. Multi-user runtime acceptance is pending. |
| `VIEW-RISK-001` | `BEHAVIOR_DIFF` | React deliberately retains the two-phase server contract but centralizes it in one provider, resets loading on Retry, and replaces the result atomically. Concurrent Retry requests are still not generation-guarded. |
| `VIEW-RISK-002` | `BEHAVIOR_DIFF` | Empty and failed discovery are now separate, recoverable states with server-message display and Retry. |
| `VIEW-RISK-003` | `BEHAVIOR_DIFF` | Unknown/deleted/hidden instances cannot reuse the prior iframe and receive an explicit return path; cold, warm, and directory-error cases have focused tests. |
| `VIEW-RISK-004` | `BEHAVIOR_DIFF` | Structured query parsing handles parameter order, exact keys, invalid encoding, hashes, and extra queries without relying on Ember's final dynamic parameter. Proxy/browser coverage remains pending. |
| `VIEW-RISK-005` | `NEEDS_RUNTIME_VALIDATION` | React keeps the unsandboxed same-origin model and catches cross-origin resize/activity access. Every retained View still needs login, navigation, popup, download, clipboard, and session validation. |
| `VIEW-RISK-006` | `BEHAVIOR_DIFF` | The iframe now has source/attempt-aware loading, timeout/error/Retry recovery, exact page-chrome height calculation, ref-local resizing, and window plus Views-container scroll preservation. HTTP error pages, dynamic browser layout, downloads/popups/fullscreen, and inactivity remain mandatory runtime checks. |
| `VIEW-RISK-007` | `NEEDS_RUNTIME_VALIDATION` | React does not invent client privilege metadata as a security guard; it matches only instances returned by the authorized directory request and relies on server Web-context protection. |
| `VIEW-RISK-008` | `BEHAVIOR_DIFF` | Empty/sole-View classification is tested from authorization objects, but deliberately omits Classic's global operation-state contamination. |
| `VIEW-RISK-009` | `NEEDS_RUNTIME_VALIDATION` | Manage Ambari visibility, the ordinary Admin transition guard, and Manage Versions permission remain three separately tested policies. Live role combinations remain pending. |
| `VIEW-RISK-010` | `BEHAVIOR_DIFF` | Numeric version selection, suffix cleanup, invalid-value filtering, and empty-response fallback intentionally remove the lexical/undefined legacy failure modes. |
| `VIEW-RISK-011` | `NOT_APPLICABLE` | Disabled Hive/generic Service-to-View placeholders remain disabled; React does not infer functionality from unreachable legacy hooks. |
| `VIEW-RISK-012` | `BEHAVIOR_DIFF` | React retains supports, Ambari properties, cluster identity, keep-alive, inactivity, and session context, adds visible initialization Retry, and stops timers after client logout even when server logout fails. View-only readiness now follows the complete shared identity chain before operational initialization is skipped. |

## API Comparison

| Legacy contract | React call site | Comparison result |
| --- | --- | --- |
| `GET /api/v1/views` | `src/api/viewApi.tsx#getDefinitions` | Exact first phase, using the suppressed-global-error Axios instance so the page owns its error UI. |
| `GET /api/v1/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label&versions/ViewVersionInfo/system=false` | `src/api/viewApi.tsx#getInstances` | Exact second phase and only sent after a non-empty first phase. |
| `GET /api/v1/services/AMBARI?fields=components/RootServiceComponents/component_version&components/RootServiceComponents/component_name=AMBARI_SERVER&minimal_response=true` | `src/api/serviceApi.ts#getAmbariServerVersion` | Same method, query, and response field for no-cluster, Manage Ambari, and Manage Versions transitions. |
| `POST /api/v1/auth` | `src/api/loginApi.ts#authenticate` | Same UTF-8 Basic credential flow and status-specific login messages. |
| `GET /api/v1/users/{user}?fields=...` and `GET /api/v1/users/{user}/authorizations?fields=*` | `src/store/UserContext.tsx#loadSession` | Loaded in parallel with MOTD; authorization IDs, not privilege labels, drive View-only classification. |
| `GET /api/v1/settings/motd` | `src/api/loginApi.ts#loadLoginMessage` | Missing, failed, invalid, or disabled content continues without a message. |
| `GET /api/v1/clusters?...` | `src/api/loginApi.ts#probeSession`, `src/api/clusterApi.ts#getClusterData`, and keep-alive | Authentication probe and shared cluster mapping retain provisioning/security/version/id fields for ordinary and View-only users. Keep-alive uses a minimal `/clusters` request. |
| `GET /api/v1/persist/user-pref-{user}-supports` | `src/store/context.tsx` | Failure preserves defaults and initialization continues. |
| `GET /api/v1/services/AMBARI/components/AMBARI_SERVER` | `src/api/clusterApi.ts#loadAmbariProperties` | Provides properties/version for all users, including View-only; failure now enters a visible Retry state. |
| `GET /api/v1/persist/CLUSTER_CURRENT_STATUS` | `src/Utils/authPolicy.ts`, `src/screens/ClusterWizard/clusterStore/context.tsx`, and `src/screens/ClusterWizard/wizardRecovery.ts` | React routes from `Clusters.provisioning_state`, restores `CLUSTER_CURRENT` plus `CLUSTER_STATE`, maps Classic state names to wizard steps, and exposes Retry on failure. This is a deliberate recovery-contract change rather than the legacy success-only chain. |
| `window.open` View route, iframe browser GET, and `window.location.replace` Admin View | `src/Utils/viewUtils.ts`, `src/components/ViewIframe/ViewIframe.tsx`, `src/screens/Authentication/AdminViewRedirect.tsx` | All three browser/history channels are retained; route encoding, error recovery, and semantic version selection are deliberate improvements. |

## Compatibility Decisions

The React implementation deliberately does not reproduce several unsafe or
ambiguous legacy behaviors:

- Unknown View routes never reuse a previously selected instance.
- Query parsing uses exact keys, handles parameter order, and does not throw on
  malformed percent encoding.
- Dynamic route segments are encoded and new View windows use
  `noopener,noreferrer`.
- Directory and initialization failures have visible Retry paths.
- Admin versions use numeric ordering and unusable/empty data is recoverable.
- Empty or sole `VIEW.USE` authorization always stays in the Views shell;
  upgrade/wizard state cannot grant cluster navigation through classification.
- Client logout tears down keep-alive even if the server logout request fails.

No known actionable static gap remains in the Views shell after the View-only
identity sequence, installer recovery, and iframe lifecycle work. Real View
artifacts, proxy paths, browser iframe behavior, authorization payloads, and
the conditional stack/server service-to-View producer remain explicitly in
the runtime acceptance matrix rather than being claimed as parity.

## Five Independent Audit Passes

| Pass | Question | Evidence checked | Result |
| --- | --- | --- | --- |
| 1 | Are all legacy routes, object fields, entries, and View/Admin boundaries represented? | All 92 canonical IDs, Ember Views routes/controllers/models/templates, React route tree, list/detail/layout utilities | 92/92 IDs classified; Admin View and hosted View application scope remain separate. |
| 2 | Do REST, browser navigation, and proxy-sensitive contracts match? | Named AJAX definitions/callers, React APIs, `window.open`, iframe GET, `location.replace`, server context paths | Both View REST phases and Admin version request match; proxy and real browser requests remain in the runtime matrix. |
| 3 | Do login, permissions, View-only routing, and wizard/upgrade gates compose correctly? | Ember router/main/installer/application helpers, React UserContext, AppLoader, auth policies, guards, Navbar | Installer denial is deterministic; regular/short links are preserved; three Admin entry policies remain distinct; one safer View-only classification difference is explicit. |
| 4 | Are asynchronous success, empty, error, retry, cleanup, and stale-navigation paths covered? | Provider state, iframe lifecycle, initialization chain, keep-alive, Admin lookup, focused tests | Loading is released on directory completion, Retry is available, stale iframes are prevented, and timers/listeners clean up. Browser treatment of iframe HTTP and script failures remains an explicit runtime boundary. |
| 5 | Do tests and reverse inventories prove static completeness without claiming runtime? | React focused tests, baseline/test cross-check, source ID reverse comparison, nine acceptance scenarios | Static implementation and failure paths are covered where mockable; every server/browser-only condition stays `NOT_RUN`. |

## Static Verification

| Check | Result |
| --- | --- |
| Focused Views, initialization, route, and Installer-recovery Vitest suite | Passed: 13 files, 81 tests. |
| Targeted ESLint for the View iframe, View details, Views layout, and new provider test | Passed with no warnings or errors. Existing repository lint debt outside these focused files is not included. |
| `npx tsc -b --pretty false` | Passed. |
| `npm run build` | Passed; existing Sass deprecation, `eval`, and chunk-size warnings remain. |
| Module 12 canonical-ID reverse comparison | Passed: 92 baseline IDs, 92 final status rows, no missing, extra, duplicate, parse, or selection errors. |
| `node docs/frontend-refactor/ember-baseline/tools/validate-ember-baseline.mjs` | Passed: 1,154 feature IDs, no warnings or errors. |
| `node --test docs/frontend-refactor/react-current/tools/react-parity-matrix.test.mjs` | Passed: 9 tests. |
| `git diff --check` | Passed. |

## Runtime Acceptance Matrix

Every row remains `NOT_RUN` until captured against a real Ambari Server and
browser. A mock response, hidden control, or TypeScript build is not runtime
authorization or iframe evidence.

| # | Environment/scenario | Required observation | Status |
| --- | --- | --- | --- |
| 1 | Zero authorization, sole `VIEW.USE`, cluster plus View, and multiple instance privileges | First screen, shell controls, and server-filtered View collection match each authorization payload | `NOT_RUN` |
| 2 | One View with multiple versions/instances, visible/hidden, system/non-system, short/no-short URL | Directory order, fallbacks, filtering, URL choice, and top menu match the returned resources | `NOT_RUN` |
| 3 | Regular and short deep links before/after login, refresh, slash/hash/query `viewPath` variants | Both identities and every internal-path form load the intended instance without stale content | `NOT_RUN` |
| 4 | View REST 401/403/500, deletion, deployment transition, context 404/500, and iframe script failure | Directory Retry and not-available recovery remain stable; iframe/browser failure behavior is recorded | `NOT_RUN` |
| 5 | Dynamic View height, host scroll, inactivity warning/logout, popup, download, clipboard, fullscreen, and exit | Sizing and session activity remain usable and all frame/timer listeners clean up | `NOT_RUN` |
| 6 | Root, non-root reverse proxy, and Knox-style deployment | `endsWith` matching, hashes, server context, Classic switch, and Admin View root all preserve the deployment prefix | `NOT_RUN` |
| 7 | View-only no-cluster user, Ambari administrator, ordinary unauthorized user, and another wizard owner | Views/Admin/Installer fallbacks and all three Admin entry permissions agree with server authorization | `NOT_RUN` |
| 8 | Multiple Ambari Server components with custom suffixes, missing values, `2.9`, and `2.10` | Numeric selection, Admin page target, empty response, and lookup failure recovery are confirmed | `NOT_RUN` |
| 9 | Supports, Ambari properties, cluster identity, persistence, and logout fault injection with a virtual clock | Initialization Retry, first 60-second keep-alive, no duplicate timer, inactivity setup, and logout cleanup converge | `NOT_RUN` |
