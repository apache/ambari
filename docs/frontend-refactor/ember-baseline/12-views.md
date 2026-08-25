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

# Ambari Views Module

The legacy Ember Ambari Views feature has three distinct layers: Ember lists View instances visible to the current user, parses two hash URL forms, and embeds an instance's server context path in a same-origin iframe; Ambari Server provides the individual View application under `/views/...`; and a separate AngularJS Admin Console manages users, groups, permissions, and View instances. React comparison must not combine these into one page feature.

`View` in this document means an Ambari View extension, not the Ember `Em.View` UI class. Entries are `CONFIRMED` by default; entries explicitly marked `STATIC_ONLY`, `CONDITIONAL`, `PLACEHOLDER`, or `OUT_OF_SCOPE` follow the evidence levels in [00-methodology.md](00-methodology.md).

## Scope and Object Model

| ID | Baseline fact | React comparison boundary | Primary evidence | Level |
| --- | --- | --- | --- | --- |
| VIEW-SCOPE-001 | The backend object hierarchy is View definition -> View version -> View instance; Ember ultimately creates a flat `App.ViewInstance[]` | Different versions/instances of the same View must remain distinct objects and must not be deduplicated only by `view_name` | `app/controllers/main/views_controller.js`, `app/models/view_instance.js` | `CONFIRMED` |
| VIEW-SCOPE-002 | Ember consumes only the instance icon, label, visible, version, description, view name, short URL, instance name, and context path | View parameters, properties, cluster binding, instance lifecycle, and authorization CRUD are outside this Ember page | `app/controllers/main/views_controller.js#loadViewInstancesSuccess` | `CONFIRMED` |
| VIEW-SCOPE-003 | `/views/{view}/{version}/{instance}/...` is the View Web context provided by Ambari Server, not an `/api/v1` REST route or an Ember hash route | The React shell may host this context, but the View application itself must not be described as part of the React refactor scope | `app/views/main/views/details.js#src`, `ViewInstanceInfo.context_path` | `CONFIRMED` |
| VIEW-SCOPE-004 | Built-in `ADMIN_VIEW` is a separate AngularJS Admin Console; Ember provides only entry checks, version discovery, and full-page navigation | Users, groups, roles, cluster permissions, repositories, and View instance/short URL/permission management inside the Admin Console are `OUT_OF_SCOPE` | `app/router.js#transitionToAdminView`, `ambari-admin/src/main/resources/ui/admin-web` | `OUT_OF_SCOPE` |

## Routes and Page State

| ID | URL / route state | Behavior | Primary evidence | Level |
| --- | --- | --- | --- | --- |
| VIEW-ROUTE-001 | `#/main/views/`, `main.views.index` | Waits for View instance data to finish, then connects the `mainViews` list to the main outlet | `app/routes/views.js#index`, `app/views/main/views_view.js` | `CONFIRMED` |
| VIEW-ROUTE-002 | `#/main/views/:viewName/:version/:instanceName`, `main.views.viewDetails` | Matches a preloaded instance by complete identity and connects the `mainViewsDetails` iframe to the outlet | `app/routes/views.js#viewDetails` | `CONFIRMED` |
| VIEW-ROUTE-003 | `#/main/view/`, `main.view.index` | The singular-route index also displays the complete View list; entering the parent route switches to the contrib-view wide layout | `app/routes/view.js#index` | `CONFIRMED` |
| VIEW-ROUTE-004 | `#/main/view/:viewName/:shortName`, `main.view.shortViewDetails` | Matches a preloaded instance by `viewName + shortUrl` and displays the same iframe | `app/routes/view.js#shortViewDetails`, `test/models/view_instance_test.js` | `CONFIRMED` |
| VIEW-ROUTE-005 | `#/adminView`, top-level `adminView` state; route pattern is `/adminView` | This is an outlet-less transition route in the hash router; after finding the Admin View URL from the server version, it performs full-page `location.replace`. Do not describe the route pattern as the browser root path `/adminView` | `app/router.js#adminView`, `app/router.js#adminViewInfoSuccessCallback` | `CONFIRMED` |
| VIEW-ROUTE-006 | `/views/:view/:version/:instance/...`, without `#` | The browser directly requests the View application's server context and does not pass through `main.views.viewDetails` | `app/views/main/views/details.js#src`, `app/router.js#adminViewInfoSuccessCallback` | `CONFIRMED` |

The `main.views` parent route has no breadcrumb; regular-detail breadcrumb text is bound to the instance label. The `main.view` parent route defines a label breadcrumb, but short detail explicitly sets it to `null`. This is a visible difference between the two URL forms in the legacy UI and must not be treated as a string alias.

## Instance Discovery and Listing

| ID | Function and behavior | Success result | Exceptions/boundaries | Backend request | Primary evidence | Level |
| --- | --- | --- | --- | --- | --- | --- |
| VIEW-LIST-001 | First checks whether any View definition exists for a logged-in user | Continues to load instances from all non-system versions only when `items` exists; no item completes with an empty array | Sends no request when unauthenticated; the main parent route handles route authentication | `views.info` | `app/controllers/main/views_controller.js#loadAmbariViews`, controller tests | `CONFIRMED` |
| VIEW-LIST-002 | Loads instances from all versions and flattens them | Each visible instance becomes an `App.ViewInstance` in server traversal order | No client-side pagination, sorting, search, or manual refresh | `views.instances` | `app/controllers/main/views_controller.js#loadViewInstancesSuccess` | `CONFIRMED` |
| VIEW-LIST-003 | Includes only instances returned by the server from non-system, deployed versions with truthy `ViewInstanceInfo.visible` | Hidden instances, built-in system Views, and instances from undeployed versions are absent from the directory and top View menu | `system=false` is an explicit query; the server View resource provider constructs returned instances only for `DEPLOYED` versions, while the frontend has no explicit status predicate; `visible` is filtered after response and the template applies a defensive check | `views.instances` | AJAX definition, controller, server View resource provider, `main/views.hbs` | `CONFIRMED` |
| VIEW-LIST-004 | Computes instance display fields and fallbacks | Missing icon uses `/img/ambari-view-default.png`; label prefers `instance.label`, then `version.label`, then `view_name`; missing description displays `No description` | `href` is `context_path + '/'` directly; the client does not reconstruct the server context | `views.instances` | `main/views_controller.js:83-94`, messages | `CONFIRMED` |
| VIEW-LIST-005 | Displays the `Your Views` table | Each row displays icon, label, version, and description; clicking the row calls `window.open(internalAmbariUrl)` to open a new browsing context | The browser may choose a tab/window according to settings; the action does nothing without context | No new request | `app/templates/main/views.hbs`, `#setView`, controller tests | `CONFIRMED` |
| VIEW-LIST-006 | Displays `No views` when no visible instance exists | Empty response and load error ultimately use the same empty state | The legacy UI does not distinguish genuinely empty, unauthorized instances, and request failure | `views.info`, `views.instances` | Controller error callbacks, template | `CONFIRMED` |
| VIEW-LIST-007 | `dataLoading()` checks every 50ms and connects the outlet only after `isDataLoaded=true` | Initial request success, empty result, or error callback all release the wait | No Views-specific spinner; if a request reaches no callback, the promise waits forever | No new request | `main/views_controller.js#dataLoading`, routes | `CONFIRMED` |
| VIEW-LIST-008 | main, installer, explicit Views routes, and login branching can all trigger `loadAmbariViews()` | A later success replaces the entire instance array | Does not deduplicate concurrent requests or reset `isDataLoaded` to false before refresh; a route after the first completion can consume the old array before async results update it | `views.info`, `views.instances` | `app/routes/main.js`, `app/routes/installer.js`, both Views routes | `STATIC_ONLY` |

### Reachable Entry Points

| ID | Entry | Legacy behavior | Preconditions/boundaries | Level |
| --- | --- | --- | --- | --- |
| VIEW-NAV-001 | Automatic branching for View-only users, users without cluster permission, or installer routes without permission | Enters `main.views.index`, the primary explicit entry to the View list | See "Login and View-only Users" | `CONFIRMED` |
| VIEW-NAV-002 | Top-grid Views dropdown for an installed cluster | Lists all visible instances from `ApplicationView.views`; clicking calls the same `setView` and opens a new browsing context; empty state displays disabled `No Views` | Appears only when `applicationController.enableLinks=true`, meaning the cluster is installed/loaded and the user is not View-only | `CONFIRMED` |
| VIEW-NAV-003 | Direct access to `#/main/views` or `#/main/view` | Displays the list after authentication | Current `MainSideMenuView.content` does not create a Views menu item; although `isViewsItem/goToSection('views')` code and tests remain, they do not prove a current sidebar entry | `STATIC_ONLY` |

## Regular, Short URLs, and Internal View Paths

### URL Selection and Instance Matching

| ID | Function and behavior | Exact rule | Exceptions/boundaries | Primary evidence | Level |
| --- | --- | --- | --- | --- | --- |
| VIEW-URL-001 | Generates the legacy UI internal URL for a list item | Generates `#/main/view/{viewName}/{shortUrl}` when `shortUrl` exists; otherwise generates `#/main/views/{viewName}/{version}/{instanceName}` | Does not URL-encode; name validity depends on the Admin Console/server | `app/models/view_instance.js#internalAmbariUrl`, model tests | `CONFIRMED` |
| VIEW-URL-002 | Parses a regular URL | Constructs `/views/{viewName}/{version}/{instanceName}/` and selects the first object where `instance.href.endsWith(constructedPath)` | `endsWith` permits `context_path` to include a proxy/root prefix; the route does not request an individual instance by parameters | `app/routes/views.js#connectOutlets` | `CONFIRMED` |
| VIEW-URL-003 | Parses a short URL | First filters by `viewName`, then takes the first object with `shortUrl == shortName`; version and instance name do not appear in the URL | Short-name uniqueness and authorization are managed by the server; the client has no conflict UI for duplicate results | `app/routes/view.js#connectOutlets` | `CONFIRMED` |
| VIEW-URL-004 | Saves `viewPath` after matching an instance | Writes the parsed result to the selected `App.ViewInstance` before connecting the outlet; the iframe src uses it | The object is reused in the global array; the next navigation without an internal path resets it to an empty string | Both routes, details view | `CONFIRMED` |
| VIEW-URL-005 | Route parameters match no loaded instance | Still calls `connectOutlet('mainViewsDetails', undefined)`; legacy Ember updates singleton controller.content only when context is truthy, so navigating in one session from a valid instance to an unmatched URL definitely reuses the old instance and any residual `viewPath`. With empty controller content on cold start, `src` computed evaluation of `content.href/viewPath` produces an exception or malformed URL | No not-found, unauthorized, return-to-list, or retry state; warm navigation leaks a stale instance, while cold-start behavior still requires browser validation | Both routes, `vendor/scripts/ember-latest.js#connectOutlet`, details controller/view | `STATIC_ONLY` |
| VIEW-URL-006 | Refreshes a regular/short deep link in an authenticated state | In-memory models start empty; main/Views routes rediscover instances, and details routes wait for `isDataLoaded` before matching and creating the iframe | The instance must still be returned by the API and visible; any directory-request failure falls into VIEW-URL-005, with no persisted snapshot or single-instance fallback request | Main route, both Views routes, controller | `CONFIRMED` |

### `viewPath` Conversion Algorithm

`viewPath` deep-links from an Ambari hash route to an internal View application page, such as Tez application history. The regular and short routes each duplicate the exact same parsing logic:

1. Takes the query from the first `?` in the browser's current URL; with no `?`, the parsed value is empty.
2. When the query contains `viewPath`, takes everything after the last `?viewPath=` and runs `decodeURIComponent`; the code correctly recognizes only `?viewPath=`, so it must be the first query parameter and `&viewPath=` produces an incorrect slice.
3. Replaces the first `&` in the decoded result with `?`, turning the remaining parameters into the internal View query; later `&` characters remain.
4. Because the legacy Ember router may attach the query to the final dynamic route parameter, truncates `instanceName` or `shortName` at its last `?` and uses the truncated value to match the instance.
5. If the final dynamic parameter did not actually carry a query, clears the parsed `viewPath`; this is coupled to the old router's query parsing.
6. Removes one leading `/` before forwarding because `instance.href` already ends with `/`.

| ID | Input example | `parseViewPath` result | Final appended form | Evidence/level |
| --- | --- | --- | --- | --- |
| VIEW-PATH-001 | No query | Empty string | `{context_path}/` | Route code; `CONFIRMED` |
| VIEW-PATH-002 | `?foo=bar&count=1` | `?foo=bar&count=1` | Forwarding depends on whether the query enters the final route parameter | Route test validates only the parser; `STATIC_ONLY` |
| VIEW-PATH-003 | `?viewPath=%2Fuser%2Fadmin%2Faddress` | `/user/admin/address` | `{context_path}/user/admin/address` | `test/routes/views_test.js`; `CONFIRMED` |
| VIEW-PATH-004 | `?viewPath=%2Fuser%2Fadmin%2Faddress&foo=bar&count=1` | `/user/admin/address?foo=bar&count=1` | `{context_path}/user/admin/address?foo=bar&count=1` | `test/routes/views_test.js`; `CONFIRMED` |
| VIEW-PATH-005 | `?viewPath=%2F%23%2Ftez-app%2Fapplication_...` | `/#/tez-app/application_...` | `{context_path}/#/tez-app/application_...` | Route code, Tez history URL template; `CONDITIONAL` |
| VIEW-PATH-006 | Invalid percent encoding, such as `?viewPath=%E0%A4%A` | `decodeURIComponent` synchronously throws `URIError` | No try/catch, route error state, or fallback; the details outlet does not connect through the normal chain | Both route implementations; `STATIC_ONLY` |
| VIEW-PATH-007 | An unrelated query name/value contains only the substring `viewPath` | `path.contains('viewPath')` enters special parsing incorrectly, then calculates a bad slice with `lastIndexOf('?viewPath=')=-1` | The parser does not parse query keys and may lose or alter the original query; existing tests do not cover it | Both route implementations; `STATIC_ONLY` |

The short route has no independent parser test, and the existing route test calls only the regular route's `parseViewPath()`; React must separately validate regular/short URLs, ordinary queries, encoded slashes, hashes, and multiple query parameters.

## iframe Hosting and Rendering Lifecycle

| ID | Function and behavior | Exact behavior | Exceptions/boundaries | Primary evidence | Level |
| --- | --- | --- | --- | --- | --- |
| VIEW-IFRAME-001 | The details outlet renders as an iframe | `tagName=iframe`, bound to `src`, `seamless`, and `allowfullscreen`; CSS is 100% width, minimum 100% height, and borderless | No separate details template | `app/views/main/views/details.js`, `app/styles/application.less` | `CONFIRMED` |
| VIEW-IFRAME-002 | Generates the iframe src | Forces the current `protocol + '//' + host`, then appends server-returned `context_path + '/'` and parsed `viewPath` | Does not accept an external origin returned by the instance; View context is designed for same-origin access | Details view `src` | `CONFIRMED` |
| VIEW-IFRAME-003 | Details use the wide contrib-view layout | Regular detail adds/removes a body class on entry/exit; the singular `/main/view` parent route applies the same treatment to its index and short detail | Navbar retains a fixed container width while main content expands to auto | Routes, `bootstrap_overrides.less` | `CONFIRMED` |
| VIEW-IFRAME-004 | Resizes immediately after iframe insertion and every 5 seconds thereafter | Height is the larger of the View body `scrollHeight` and `document.body.outerHeight()` after removing the `#top-nav` and footer outer heights; preserves host window scrollTop before/after resize | Selector reads the first iframe but applies the resulting height to every iframe in the document; pages with multiple iframes may read and resize the wrong objects | Details view `didInsertElement/resizeFunction` | `STATIC_ONLY` |
| VIEW-IFRAME-005 | Clears the resize interval when destroying the details view | Clears only a saved interval to prevent DOM changes after leaving the page | No dedicated test | Details view `willDestroyElement` | `STATIC_ONLY` |
| VIEW-IFRAME-006 | Activity inside the iframe counts toward the Ambari inactivity timeout | Restarts/binds the inactivity monitor after details insertion; binds mousemove, keypress, and click on the iframe `contentWindow` to `keepActive` | Depends on same-origin access; cross-origin redirect or browser restrictions may make `contentWindow.document`/events fail, and the code has no catch | Details view, `app/controllers/main.js#bindActivityEventMonitors` | `STATIC_ONLY` |
| VIEW-IFRAME-007 | The legacy iframe has no sandbox restriction | Declares only seamless and fullscreen; the View and Ambari shell share origin/session | If React adds sandbox/CSP, validate View login, navigation, downloads, popups, clipboard, and related compatibility | Details view attributes | `CONFIRMED` |
| VIEW-IFRAME-008 | Iframe navigation has no Ember loading/error/retry UI | Only the instance-directory request is awaited; after the iframe browser GET, there is no `load/error` handler, spinner, timeout, or error placeholder | Server 404/500, View deployment failure, and content-script errors are rendered by the iframe/browser | Details view, routes | `CONFIRMED` |
| VIEW-IFRAME-009 | Internal View navigation does not update the host Ember URL | The shell does not listen to iframe location, history, or `postMessage`; the host writes only the initial `viewPath` to `src` | Browser refresh restores only the original `viewPath` in the host URL and cannot guarantee the page later reached inside the iframe; View-side persistence is separate | Details view, both routes | `STATIC_ONLY` |

## Login and View-only Users

### Determination Semantics

`App.auth` is the set of unique `AuthorizationInfo.authorization_id` values from `GET /users/{user}/authorizations?fields=*`. The legacy code defines `isOnlyViewUser=true` in either of these cases:

- The authorization collection exists but is an empty array.
- The collection length is exactly 1, its only value is `VIEW.USE`, and `App.isAuthorized('VIEW.USE')` is still true at that time.

The second condition goes through `isAuthorized`, so it also inherits global upgrade restrictions and the `wizardWatcherController.isNonWizardUser` restriction. This is legacy static semantics and must not be simplified to "has any View privilege."

| ID | Scenario | Exact result after login/entering main | Primary requests | Primary evidence | Level |
| --- | --- | --- | --- | --- | --- |
| VIEW-ONLY-001 | Existing cluster, `isOnlyViewUser=true` | Actively loads Views after login and transitions to `main.views.index`; entering main still loads supports, keep-alive, Ambari properties, cluster identity, and Views, but skips repo detail checks, `mainController.initialize()`, STOMP, and full cluster operations models, setting only the cluster controller loaded to display the outlet | `persist.get`, `ambari.service`, conditional `cluster.load_cluster_name`, `views.info`, `views.instances`, keep-alive `router.login.clusters` | `app/router.js#loginGetClustersSuccessCallback`, `app/routes/main.js` | `CONFIRMED` |
| VIEW-ONLY-002 | Existing cluster, ordinary cluster/Ambari user | Uses normal preferred-path/Dashboard initialization; main also loads View instances in the background for the top Views dropdown | Auth, cluster, and Views requests | Router, main route | `CONFIRMED` |
| VIEW-ONLY-003 | No cluster, `isOnlyViewUser=true` or empty authorization | Enters `main.views.index` directly | Auth, cluster, and Views requests | Router; related login route suite is a skipped test | `STATIC_ONLY` |
| VIEW-ONLY-004 | No cluster, non-View-only user | Does not enter Installer; probes the Ambari Server version and navigates to the separate Admin View; probe failure returns to the Views list | `ambari.service.load_server_version` | Router, router tests | `CONFIRMED` |
| VIEW-ONLY-005 | Cluster provisioning is incomplete and the current route is not a View route | Restores Installer when cluster state is an installer state and `AMBARI.ADD_DELETE_CLUSTERS` exists; otherwise transitions to Views | Cluster status/persistence | `app/routes/main.js`, router redirections mixin | `CONFIRMED` |
| VIEW-ONLY-006 | Directly enters `/installer` without `AMBARI.ADD_DELETE_CLUSTERS` | Loads supports, version, and Views, then transitions to `main.views.index` | Server version and Views requests | `app/routes/installer.js` | `CONFIRMED` |
| VIEW-ONLY-007 | Authenticated user directly opens a regular/short deep link | When main finds current state already `viewDetails/shortViewDetails`, it does not replace it with index and preserves the target View | Views requests | `app/routes/main.js:53-56` | `CONFIRMED` |
| VIEW-ONLY-008 | Unauthenticated user opens a deep link and completes login | An ordinary cluster user can have the security-checked relative preferred path restored by `transitionToApp()`; the View-only branch directly calls `transitionToViews()`, and static code has no explicit restoration of the original details path | Auth, cluster, and Views requests | `app/router.js#transitionToApp/#transitionToViews` | `STATIC_ONLY` |

### Main Initialization Request Chain

View-only skips only cluster operations-data initialization; it does not "request only Views." After entering the `/main` parent route, ordinary and View-only users first use the same shell chain. The following order and waiting relationships are the React comparison baseline:

| ID | Sequence/condition | Request and exact behavior | Failure/concurrency boundary | Primary evidence | Level |
| --- | --- | --- | --- | --- | --- |
| VIEW-INIT-001 | 1. `main.enter` verifies authentication first | `getAuthenticated()` obtains or reuses the `router.login.clusters` jqXHR saved during login; a new page requests cluster provisioning/security/version/id, while just after login it usually reuses the completed or in-progress request | Authentication failure saves the current hash as the preferred path and returns to login; request reuse means network count cannot be inferred from route count | `app/router.js#getClusterDataRequest/#getAuthenticated`, `app/routes/main.js:35` | `CONFIRMED` |
| VIEW-INIT-002 | 2. Loads supports after authentication succeeds | `persist.get` reads `user-pref-{loginName}-supports`; when a response exists, overwrites matching values in `App.supports` | Route uses `.complete()`, so 404, no data, or request failure continues; failure preserves compile-time default supports | `app/controllers/experimental.js#loadSupports`, `app/mixins/common/persist.js#getUserPref`, `app/routes/main.js:38` | `CONFIRMED` |
| VIEW-INIT-003 | 3. Starts keep-alive after supports completes | `startKeepAlivePoller()` registers a timer only when `isPollerRunning=false`; the first request is not immediate and calls `router.login.clusters` after 60,000ms, with later calls scheduled by the AJAX complete callback | View-only users also keep this poller; it maintains/verifies the session and does not map the response to a complete cluster model. Only successful logoff explicitly sets `isPollerRunning=false`; logoff error callback is empty | `app/controllers/application.js#startKeepAlivePoller/#getStack`, `app/utils/updater.js`, `app/router.js#logOffSuccessCallback`, `app/config.js#sessionKeepAliveInterval` | `CONFIRMED` |
| VIEW-INIT-004 | 4. Waits synchronously for Ambari Server properties | `ambari.service` without `fields` requests the AMBARI_SERVER root component; success saves server properties/clock/version, determines custom JDK/MySQL OS family, and starts the inactivity monitor | Main route chains later steps through `.then(success)`, while the request error callback is empty; failure silently blocks this route's cluster-name/loaded branch even though login/Views routes may have triggered View discovery separately | `app/controllers/global/cluster_controller.js#loadAmbariPropertiesSuccess`, `app/routes/main.js:40` | `CONFIRMED` |
| VIEW-INIT-005 | 5. Discovers Views and confirms cluster identity in parallel after properties succeed | Asynchronously calls `loadAmbariViews()` without waiting; then calls `loadClusterName(false)`. If global `clusterName + clusterId` already exists, the latter synchronizes local state only; otherwise sends `cluster.load_cluster_name` and waits | Views routes also call discovery, so requests may overlap with main; cluster-name failure uses the global reload/error flow and the main branch does not continue | Main route, cluster controller, Views controller | `CONFIRMED` |
| VIEW-INIT-006 | 6a. Installed cluster and View-only | Preserves an open regular/short detail or transitions to `main.views.index`, then directly sets `clusterController.isLoaded=true` | Explicitly does not call `checkDetailedRepoVersion()`, `mainController.initialize()`, `App.StompClient.connect()`, or `loadClusterData()`; therefore does not load operational models such as hosts/services/alerts/upgrades/user settings | `app/routes/main.js:47-58`, `app/controllers/main.js#initialize` | `CONFIRMED` |
| VIEW-INIT-007 | 6b. Uninstalled cluster | On the next tick, uses `persist.get` to read `CLUSTER_CURRENT_STATUS`; after success, when not on a View route and state is an installer state, restores Installer if `AMBARI.ADD_DELETE_CLUSTERS` exists, otherwise enters Views; an existing regular/short View route is not preempted | Main uses jqXHR `.then(success)`: 404 silently keeps default state but the promise remains rejected, while other errors also show an update-error modal; neither executes the success branch, potentially leaving partial initialization that works only through an already-connected View route | `app/models/cluster_states.js#updateFromServer`, `app/routes/main.js:60-78` | `CONFIRMED` |

### View-only Shell Differences

| ID | Behavior | Legacy result | Level |
| --- | --- | --- | --- |
| VIEW-ONLY-009 | Left operations navigation | Does not create Dashboard, Services, Hosts, or Alerts items; usually no Admin item either | `CONFIRMED` |
| VIEW-ONLY-010 | Top Views dropdown and cluster notifications | `enableLinks=false`, so both are hidden; View-only users work through the current list or iframe rather than switching through the top grid | `CONFIRMED` |
| VIEW-ONLY-011 | Ambari logo/Dashboard navigation | `goToDashboard` does not navigate because `enableLinks=false` | `CONFIRMED` |
| VIEW-ONLY-012 | User menu | About, Switch Experience, and Sign out remain; Manage Ambari visibility is determined separately by Ambari-level permissions | `CONFIRMED` |

Views routes have no explicit `VIEW.USE` guard. Ember relies on main-route authentication and server authorization filtering of the `/api/v1/views` response set; the client checks only `system=false` and `visible` afterward. React must not use `visible` as a substitute for server authorization or assume that cluster permission grants access to every instance.

## Service and View Cross-Entry Points

| ID | Legacy source state | Behavior/boundary | Primary evidence | Level |
| --- | --- | --- | --- | --- |
| VIEW-X-001 | Ordinary Service Quick Links are a separate external Web UI link mechanism | `App.QuickLinksView` generates URLs from stack metadata/config/host; it reuses this document's short route only when the final URL explicitly targets `#/main/view/...`, so not every Quick Link is an Ambari View | `app/views/common/quick_view_link_view.js`, service summary template | `CONFIRMED` |
| VIEW-X-002 | Hive summary retains a View-link extension point | `viewsToShow` uses an instance-name allowlist and can override labels; the template passes results to `goToView()`. The current class defaults to `{}`, and no runtime code populates it across the repository, so the current baseline displays no such links | `app/views/main/service/services/hive.js`, Hive template, summary controller | `PLACEHOLDER` |
| VIEW-X-003 | The generic Service Summary Views panel is commented out | The computed `views` and corresponding Handlebars section do not execute and are not an entry React must reproduce | `app/views/main/service/info/summary.js:72-78`, summary template `131-145` | `PLACEHOLDER` |
| VIEW-X-004 | Config- or server-generated View deep links can carry `viewPath` | For example, the Tez history URL template encodes the target application path into `viewPath`; regular/short routes and the iframe still process it | Views routes, Tez configuration/advisor | `CONDITIONAL` |

Hive's `<a target="_blank">` template also binds an Ember action, while the controller's actual action is `App.router.route(internalAmbariUrl)`. If this extension point is re-enabled, whether it opens a new tab or the current tab depends on legacy Ember event handling and requires runtime validation; HTML `target` alone is insufficient.

## Admin View Discovery and Navigation

| ID | Entry/behavior | Permission and result | Failure/boundary | Backend request | Primary evidence | Level |
| --- | --- | --- | --- | --- | --- | --- |
| VIEW-ADMIN-001 | Post-login default entry with no cluster | A non-View-only user calls `transitionToAdminView()` and enters the Admin Console as a full page on success | Request error callback does not show the default error modal and instead returns to `main.views.index` | `ambari.service.load_server_version` | Router, router tests | `CONFIRMED` |
| VIEW-ADMIN-002 | User-menu `Manage Ambari` | Shown when `showManageAmbari` is true and the user has any Ambari-level management permission listed by the template; clicking enters `#/adminView` state | The route also requires login and `CLUSTER.UPGRADE_DOWNGRADE_STACK`, otherwise it goes to login; entry permission and route guard differ. The route's server-version request has no error callback: 500/401/407/413 use the default modal, while 403/404 and others are silently defaulted; no failure uses the Views fallback from post-login `transitionToAdminView()`, leaving an outlet-less state | `ambari.service.load_server_version` | Application template/controller, router, AJAX default error handler | `CONFIRMED` |
| VIEW-ADMIN-003 | Stack Versions `Manage Versions` | Shown only when `havePermissions('AMBARI.MANAGE_STACK_VERSIONS')` is true and disabled for a non-current wizard owner; after confirming "leave Cluster Management", probes the version and navigates as a full page | `havePermissions` also gates on global upgrade state, `supports.opsDuringRollingUpgrade`, and `App.auth`, not only the permission string; request failure uses global AJAX error, and cancelled confirmation sends no request | `ambari.service.load_server_version` | Versions template/view, `app/app.js#havePermissions`, version view tests | `CONFIRMED` |
| VIEW-ADMIN-004 | Selects the Admin View version | Maps each component's `RootServiceComponents.component_version`, uses default string sorting, takes the last item, and removes the build suffix with `/[^\d.-]/g` | Does not filter `undefined`; this is lexicographic rather than semantic version sorting. Empty arrays, missing versions, or malformed versions may fail before/after `.replace()` without dedicated recovery | Same as above | Router callback, tests | `CONFIRMED` |
| VIEW-ADMIN-005 | Constructs the Admin Console URL | Uses `App.appURLRoot + 'views/ADMIN_VIEW/' + latestVersion + '/INSTANCE/#/'`, then calls `window.location.replace()` | replace does not retain the current Ember page as a browser back-history entry; `appURLRoot` provides the proxy root | Browser navigation | Router, helper, config | `CONFIRMED` |
| VIEW-ADMIN-006 | `ADMIN_VIEW` does not enter the ordinary View directory | Instance query excludes `ViewVersionInfo/system=true`; the Admin instance in fixtures is also `visible=false` | The management entry must not depend on ordinary instance-list load success | `views.instances` | AJAX query, Views fixtures | `CONFIRMED` |

The `Manage Ambari` template permission set is `AMBARI.ADD_DELETE_CLUSTERS`, `AMBARI.ASSIGN_ROLES`, `AMBARI.EDIT_STACK_REPOS`, `AMBARI.MANAGE_GROUPS`, `AMBARI.MANAGE_STACK_VERSIONS`, `AMBARI.MANAGE_USERS`, `AMBARI.MANAGE_VIEWS`, and `AMBARI.RENAME_CLUSTER`. `AMBARI.MANAGE_USERS` is repeated in the template string, which does not change OR semantics.

### AngularJS Admin Console Boundary

Although the following capabilities are served by the same Ambari Server, the current Ember frontend does not implement them; the React-versus-Ember comparison matrix must not record them under this document's feature IDs:

- View definition/version deployment status and inventory management.
- Creating, cloning, editing, and deleting View instances.
- Setting display label, description, visibility, properties, and local/remote/custom cluster binding.
- Creating and maintaining short URLs.
- Granting View permission to users/groups and managing cluster permissions.
- Users, groups, roles, remote clusters, and other Admin Console management pages.

If the React refactor also replaces the AngularJS Admin Console, create a separate baseline; do not infer its CRUD details from the navigation behavior in this document.

An ordinary View application also has its own frontend, resource endpoints, and business flow. The legacy shell only places its Web context in an iframe and cannot enumerate every deployed View's internal API from `ambari-web/classic`; when migrating a specific View, create a separate baseline from that View's artifact/source. This document requires parity only for shell discovery, routes, hosting, authorization boundaries, and browser navigation.

## Permission and Visibility Model

| ID | Permission/condition | Actual role in legacy Ember | Key boundary | Level |
| --- | --- | --- | --- | --- |
| VIEW-PERM-001 | `VIEW.USE` authorization | Combines with authorization-collection length to compute `isOnlyViewUser` | Not a per-instance route guard; also subject to global `isAuthorized` state | `CONFIRMED` |
| VIEW-PERM-002 | `VIEW.USER` permission | Backend permission name for View-instance privileges and may appear in user privilege data | Ember Views controller does not check `VIEW.USER` directly; do not confuse it with the `VIEW.USE` authorization ID | `STATIC_ONLY` |
| VIEW-PERM-003 | Server-side instance access | Determines which resources `/api/v1/views` returns for the current session and protects `/views/{context}` | Client `visible=true` is only a display switch, not authorization | `STATIC_ONLY` |
| VIEW-PERM-004 | `AMBARI.MANAGE_VIEWS` | One of the OR permissions for the `Manage Ambari` entry | View-instance CRUD is in AngularJS; this permission alone does not make the Ember route perform CRUD | `CONFIRMED` |
| VIEW-PERM-005 | `CLUSTER.UPGRADE_DOWNGRADE_STACK` | Hard guard for the `/adminView` transition route | Differs from the Ambari-level permission set used by the `Manage Ambari` link | `CONFIRMED` |
| VIEW-PERM-006 | `AMBARI.MANAGE_STACK_VERSIONS` | Displays the `Manage Versions` Admin View entry on Stack Versions | Controls only this entry, not ordinary View use | `CONFIRMED` |
| VIEW-PERM-007 | `AMBARI.ADD_DELETE_CLUSTERS` | Determines whether an incomplete installation/Installer route restores the wizard or falls back to Views | Not a View-use permission | `CONFIRMED` |
| VIEW-PERM-008 | Wizard owner | Disables external management buttons on Stack Versions for a non-wizard user; installation Step 9 explicitly permits exit to Admin View/Views | Ordinary View list and iframe have no unified wizard prohibition | `CONFIRMED` |

The user-settings controller groups instance name, view name, version, and permission labels where `PrivilegeInfo.type='VIEW'` for privilege display; this logic does not participate in View-list filtering or route authorization.

## Backend API Contract

### Named Requests Used Directly by Views

| Request name | Method | Full URL | Request timing | Key response fields | Failure behavior |
| --- | --- | --- | --- | --- | --- |
| `views.info` | `GET` | `/api/v1/views` | First phase of `loadAmbariViews()` | `items[]`; Ember checks only length | Custom error callback clears the list and sets `isDataLoaded=true`, without the default error modal |
| `views.instances` | `GET` | `/api/v1/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label&versions/ViewVersionInfo/system=false` | Only when `views.info.items.length > 0` | `items[].versions[].ViewVersionInfo.label`, `versions[].instances[].ViewInstanceInfo` | Custom error callback clears the list and sets loaded, without distinguishing 401/403/404/500 |
| `ambari.service.load_server_version` | `GET` | `/api/v1/services/AMBARI?fields=components/RootServiceComponents/component_version&components/RootServiceComponents/component_name=AMBARI_SERVER&minimal_response=true` | No-cluster fallback, `/adminView` route, and Manage Versions | `components[].RootServiceComponents.component_version` | Login fallback returns to Views; explicit `/adminView`/Manage Versions has no equivalent fallback and uses default handling or remains |

The frontend contract for `ViewInstanceInfo` is:

| Response field | Ember model field | Usage |
| --- | --- | --- |
| `icon_path` | `iconPath` | List icon; default image when empty |
| `label` | `label` | List, top dropdown, regular breadcrumb |
| `visible` | `visible` | Final client-side filter |
| `version` | `version` | List and regular hash URL |
| `description` | `description` | List description; fallback when empty |
| `view_name` | `viewName` | Both hash URLs and matching key |
| `short_url` | `shortUrl` | Prefer singular short URL when present |
| `instance_name` | `instanceName` | Regular hash URL and conditional service hook |
| `context_path` | `href` | Appends `/` and serves as the iframe server path; must be treated as server fact |

### Requests Used by Login, the Main Shell, and Permission Branching

| Request name | Method | URL/key | Request timing and relation to Views | Failure semantics |
| --- | --- | --- | --- | --- |
| `router.login` | `POST` | `/api/v1/auth` | Local login submission; Base64 of UTF-8 `username:password` is placed in the Basic Authorization header | 403 displays an authentication error, 500 displays a server error, and other statuses use generic login failure; see the authentication document for the external JWT branch |
| `router.afterLogin` | `GET` | `/api/v1/users/{loginName}?fields=*,privileges/PrivilegeInfo/cluster_name,privileges/PrivilegeInfo/permission_name` | Establishes the logged-in user and privilege context | Failure uses login error and does not enter Views branching |
| `router.user.authorizations` | `GET` | `/api/v1/users/{userName}/authorizations?fields=*` | Establishes `App.auth`; `VIEW.USE` and collection length jointly determine View-only | Login chain continues through `.complete()`, so `App.auth` may remain empty/old after failure; requires runtime validation |
| `router.login.message` | `GET` | `/api/v1/settings/motd` | Reads the login message after authorizations complete and continues cluster branching after confirmation or no valid message | Error, empty value, or invalid JSON all continue as "no message" |
| `router.login.clusters` | `GET` | `/api/v1/clusters?fields=Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id` | Shared by login branching, `main.getAuthenticated()`, and 60-second keep-alive; determines normal app, Installer, Views, or Admin View | Authentication probe failure returns to login; keep-alive has no business error UI and updater complete schedules the next call |
| `persist.get` (supports) | `GET` | `/api/v1/persist/user-pref-{loginName}-supports` | Merges per-user feature flags before each main/installer entry | Empty/404/error preserves defaults and continues |
| `ambari.service` (main) | `GET` | `/api/v1/services/AMBARI/components/AMBARI_SERVER` | View-only also reads server properties/clock/version and establishes the inactivity timeout | Empty error callback; later success chaining in the main flow is cut off |
| `cluster.load_cluster_name` | `GET` | `/api/v1/clusters?fields=Clusters/security_type,Clusters/version,Clusters/cluster_id` | Sent only when `App.clusterName` or `App.clusterId` is missing or a caller requests reload; sets cluster name/id/stack/security | Uses the reload error handler; failure does not enter the View-only loaded branch |
| `persist.get` (cluster status) | `GET` | `/api/v1/persist/CLUSTER_CURRENT_STATUS` | Restores installer/wizard state and decides whether to fall back to Views only when cluster provisioning is incomplete | Historical value can replace the local DB; 404 keeps defaults but jqXHR remains rejected, while other errors show a modal and reject; neither continues main's `.then(success)` |

### Browser Requests/Navigation Bypassing `App.ajax`

Views controllers, routes, and details view contain no `App.HttpClient.get`, native `XMLHttpRequest`, or direct `$.ajax` calls, so [generated/direct-http-calls.md](generated/direct-http-calls.md) has no Views-specific call site. Migration must still track these three browser behaviors that are outside the named-request table:

| Identifier | Behavior | Network/history semantics |
| --- | --- | --- |
| `NAV:ViewInstance.internalAmbariUrl` | `window.open('#/main/view...')` or `window.open('#/main/views...')` | Opens a new browsing context for the same Ambari shell, then the route loads the iframe |
| `BROWSER_GET:ViewInstanceInfo.context_path` | iframe `src={origin}{context_path}/{viewPath}` | Browser directly performs a GET for the View Web application and static resources, sharing the current Ambari session |
| `NAV:ADMIN_VIEW` | `window.location.replace('{appURLRoot}views/ADMIN_VIEW/{version}/INSTANCE/#/')` | Leaves the Ember shell as a full page and replaces the current history entry |

[generated/api-by-module/views.md](generated/api-by-module/views.md) is only a candidate index using broad request-name and caller-path matching; a cross-module match is not a direct Views-page call, and absence does not prove that no request exists. Authoritative network verification must combine [AJAX definitions](generated/ajax-endpoints.md), [AJAX call sites](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md), [browser entrypoints](generated/browser-network-entrypoints.md), and [realtime channels](generated/realtime-channels.md).

## Source and Test Cross-Check

| Review target | Confirmed behavior | Test status/gap |
| --- | --- | --- |
| `test/controllers/main/views_controller_test.js` | Requests only after login, two-phase load, empty/error reset, instance field mapping, and `setView` calling `window.open` | Covers only one visible instance; no hidden/system, multiple version, multiple instance, fallback label/icon/description, or concurrent reload coverage |
| `test/models/view_instance_test.js` | Uses the singular route with a short URL and the regular route without one | Does not cover names requiring encoding, empty fields, or proxy root |
| `test/routes/views_test.js` | Regular-route `parseViewPath` output for no query, ordinary query, encoded path, and path + query | Does not execute complete `connectOutlets`; short-route parser, parameter slicing, instance lookup, and invalid route are untested |
| `test/views/main/views_view_test.js` | `MainViewsView.views` binds the controller array | Does not render template actions, empty state, or visible filter |
| `test/router_test.js#adminViewInfoSuccessCallback` | Takes the last sorted component version, removes custom build suffix, and generates the Admin View URL | Version-selection test exists; the entire `loginGetClustersSuccessCallback` suite is `describe.skip`, so no cluster/View-only matrix is active regression coverage |
| `test/router_test.js#transitionToViews/#adminViewInfoErrorCallback` | Loads Views and transitions to index; Admin version request failure returns to Views | Does not cover inconsistent `/adminView` route guard and link permissions |
| `test/views/main/admin/stack_upgrade/version_view_test.js` | Manage Versions requests the server version and calls `location.replace` after confirmation | Covers ordinary/custom versions; does not cover cancellation, request failure, empty components, or lexical-version traps |
| `test/views/main/menu_test.js` | Dormant `goToSection('views')` branch can call the router | Does not prove that current `MainSideMenuView.content` creates a Views item |
| `test/controllers/application_test.js`, `test/controllers/global/cluster_controller_test.js` | `enableLinks` excludes View-only, keep-alive sends `router.login.clusters`, and Ambari properties/cluster-name requests and success mapping | No main-route integration test proves the full order, failure short-circuit, and timer cleanup for VIEW-INIT-001 through VIEW-INIT-007 |
| Reverse inventory of Views files in `generated/template-actions.md` | Currently reachable Views actions are the two list/top-dropdown `setView` actions and user-menu `goToAdminView`; Hive `goToView` is an empty extension point | Static action extraction does not cover View click handlers, dynamic actions, or iframe applications; finding no additional Views mutation does not prove runtime absence |
| `app/views/main/views/details.js` | iframe src, resize, interval, inactivity | No dedicated unit/integration test; all require browser runtime validation |

## Known Legacy Risks and React Acceptance Requirements

| ID | Legacy risk/ambiguity | React comparison handling |
| --- | --- | --- |
| VIEW-RISK-001 | Two-phase View queries add one round trip; reload does not reset loaded or deduplicate concurrent requests | If React combines/caches requests, still prove final authorization filtering, refresh, and error semantics; mark behavior changes `BEHAVIOR_DIFF` |
| VIEW-RISK-002 | Empty list, 403/500, and instance-load failure display the same `No views` | Adding diagnosable error/retry in React is a reasonable improvement, but must not be marked as existing legacy behavior in the matrix |
| VIEW-RISK-003 | Invalid regular/short deep links have no not-found recovery and may reuse old content from the singleton details controller | If React fixes this, record the new behavior and separately test cold/warm navigation, unknown, hidden, unauthorized, and deleted instances; never display the previous instance |
| VIEW-RISK-004 | `viewPath` depends on legacy Ember attaching the query to the final dynamic parameter; parser uses substring rather than query-key recognition and does not catch `decodeURIComponent` `URIError` | Use real-browser coverage for both URL forms, hash/query, query-parameter order, unrelated fields containing `viewPath`, invalid percent encoding, encoded slash, and proxy/Knox paths; do not only port parser unit tests |
| VIEW-RISK-005 | Iframe reads the content document, binds events, and has no sandbox, implying same-origin access | Validate every retained View before changing iframe policy in React; design cross-origin support separately if it is a new requirement |
| VIEW-RISK-006 | Iframe has no load/error lifecycle and uses a global selector to resize the first iframe | React may improve this, but must validate height, host scrolling, internal View routes, downloads, and inactivity timeout |
| VIEW-RISK-007 | View routes have no client-side per-instance permission guard | Preserve server REST/context authorization; React may add a route guard but must not trust client metadata as the security boundary |
| VIEW-RISK-008 | `isOnlyViewUser` treats empty authorization as View-only and is affected by global upgrade/wizard gates | Comparison tests must use authorization payloads rather than constructing roles only from privilege labels or usernames |
| VIEW-RISK-009 | Manage Ambari link permissions, `/adminView` route guard, and Manage Versions permissions are three different conditions | Do not silently change legacy entry points with one `canManageViews` boolean; if unified, mark `BEHAVIOR_DIFF` and obtain maintainer confirmation |
| VIEW-RISK-010 | Admin version uses string sorting and assumes at least one valid component version | Test multiple Ambari Server components, custom builds, `2.9`/`2.10`, and empty/bad responses; may use server fact or semantic sorting instead |
| VIEW-RISK-011 | Generic service-to-View panel and Hive hook are currently disabled | React must not infer missing functionality from comments or empty extension points; upgrade from `PLACEHOLDER` only after stack/runtime injection is confirmed |
| VIEW-RISK-012 | View-only still depends on the main-shell chain supports -> `ambari.service` -> cluster identity; `ambari.service` or incomplete-cluster persistence failure blocks success chaining; keep-alive is explicitly stopped only on successful logoff | React must not remove keep-alive/inactivity/session context because it "only needs Views"; if made degradable with displayed errors, mark `BEHAVIOR_DIFF` and cover partial-init, logout failure, and recovery |

### Minimum Runtime Scenarios

1. Use users with zero authorization, only `VIEW.USE`, cluster + View, and multiple View instance privileges, and verify the first screen, menus, and API response set.
2. Use one View with multiple versions/instances, including visible/hidden, system/non-system, and with/without short URL combinations.
3. Cover regular and short deep links with no internal path, `viewPath` slash, hash route, query parameters, refresh, and pre-login access.
4. Cover View REST 401/403/500, instance deletion, context 404/500, View deployment in progress, and iframe script errors, recording legacy and React error states.
5. Cover View content-height changes, host scrolling, inactivity warning/logout, in-View popups/downloads/fullscreen, and interval cleanup after exit.
6. Install Ambari under a non-root proxy path/Knox path and verify `endsWith` instance matching, hash URLs, iframe context, and Admin View `appURLRoot`.
7. Log in separately as a View-only user with no cluster, an Ambari administrator, and an ordinary unauthorized user, and verify Views/Admin View fallbacks.
8. Use multiple Ambari Server component versions, custom suffixes, and two-digit minor versions, and verify Admin View version selection and failure recovery.
9. Inject supports, Ambari properties, cluster identity, and persistence failures into View-only main initialization; use a virtual clock to verify initial keep-alive delay, no duplicate registration on re-entry, and different stop behavior after successful/failed logoff.
