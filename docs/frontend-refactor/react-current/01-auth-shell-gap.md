# React Authentication and Application Shell Comparison

## Comparison Scope

| Item | Value |
| --- | --- |
| Ember baseline | `ember-baseline/01-auth-shell.md` |
| React implementation | `ambari-web/latest`, branch `auth-shell-module` |
| React baseline commit | `8ac5c5a1346687bc46b4651c42b07237fe0e9ca9` |
| Module feature IDs | `AUTH-001` through `AUTH-008`, `SHELL-001` through `SHELL-009` |
| Review date | 2026-08-13 |
| Metrics scope | Excluded; `/main/dashboard/metrics` is only an existing default route, and Metrics page capability is not evaluated |

Statuses follow `ember-baseline/14-react-gap-matrix.md`. The current code, unit tests, and production build have passed static validation, but no real Ambari Server, Knox, or Admin View has been connected. All 17 items are therefore marked `NEEDS_RUNTIME_VALIDATION` and must not be marked `COVERED` in advance.

## Current Conclusion

| Status | Count |
| --- | ---: |
| `NEEDS_RUNTIME_VALIDATION` | 17 |
| Total | 17 |

The static gaps in authentication and the application shell have been filled: the Router is always mounted, session probing precedes application initialization, user/privileges/authorizations/MOTD load atomically, and all four landing paths, unified logout, keep-alive, inactivity, About, Experimental, User Settings, and the Installer version gate are wired. The remaining gate is to exercise the roles, SSO, error, and recovery scenarios listed here against a real server.

## Feature Status

| ID | Status | React implementation and automated evidence | Runtime validation pending |
| --- | --- | --- | --- |
| `AUTH-001` | `NEEDS_RUNTIME_VALIDATION` | `App.tsx` always mounts the hash router; `AuthenticatedApplication` separates public login from the protected application; unauthenticated deep links save a safe target and redirect to `/login`; root `/` is not used as a preferred path | Direct unauthenticated access to root, Dashboard, Installer, and View deep links; redirect and recovery after server session expiry |
| `AUTH-002` | `NEEDS_RUNTIME_VALIDATION` | `loginApi.authenticate()` sends UTF-8 Basic; the username is encoded only at the API URL boundary; `loadSession()` requires both user and authorizations to succeed; tests cover UTF-8, single encoding, and authorization failure | Successful local-user login; actual server response bodies and UI messages for 403, 500, and network errors |
| `AUTH-003` | `NEEDS_RUNTIME_VALIDATION` | `/login/local`, `jwtProviderUrl`, return URL, three-loop limit, and local recovery are implemented; a plain business 403 is no longer misclassified as session expiry | Successful Knox JWT return; Knox 401/403 payloads; complete return URL behind a proxy; local login after the loop limit is exceeded |
| `AUTH-004` | `NEEDS_RUNTIME_VALIDATION` | `/clusters` session probe reads the `User` header; restores when local state is missing; does not create a session on authorization failure; 401 synchronously clears the React in-memory session | Container/SSO authentication with empty localStorage; header case and proxy forwarding; all lifecycle teardown after session expiry |
| `AUTH-005` | `NEEDS_RUNTIME_VALIDATION` | User, authorization, and MOTD load in parallel; enabled MOTD with text blocks before AppProvider; missing, disabled, invalid, and failed requests continue | Actual `/settings/motd` responses for valid content, line breaks, invalid JSON, disabled, 404, and 500 |
| `AUTH-006` | `NEEDS_RUNTIME_VALIDATION` | `selectLandingPath()` covers installed, incomplete, View-only, and no-cluster; Admin View generates a versioned browser URL; tests cover all four landing paths | Four role/cluster combinations; Admin View deployment-version selection; actual recovery of incomplete Installer and in-progress wizard |
| `AUTH-007` | `NEEDS_RUNTIME_VALIDATION` | Navbar and inactivity share context logout; memory, local DB, and preferred path are cleared before best-effort `/logout`; server failure does not block navigation; tests cover rejected logout | `/logout` success, 500, network disconnection, and long-pending cases; cookie deletion; STOMP, polling, and iframe teardown |
| `AUTH-008` | `NEEDS_RUNTIME_VALIDATION` | After the authenticated application mounts, calls `/clusters` serially every 60 seconds and stops after unmount or logout | Long-session keep-alive; no concurrency when a request crosses 60 seconds; session-expiry response; no subsequent requests in the Network panel after logout |
| `SHELL-001` | `NEEDS_RUNTIME_VALIDATION` | AppProvider loads supports, wizard owner, Ambari properties/version, and cluster; operations users then load services/hosts/upgrade/background/STOMP; View-only skips operations initialization and uses a minimal shell; initialization failures can Retry | Actual request inventory for installed/incomplete/no-cluster/View-only; failure and Retry for each initialization API; STOMP reconnect/deactivate |
| `SHELL-002` | `NEEDS_RUNTIME_VALIDATION` | `/main`, `/installer`, `/experimental`, and `/adminView` are within the unified authentication boundary; Admin submenus and routes authorize separately; Auto Start preserves two menu AND groups and a Manage OR route; Kerberos/Auto Start use feature guards | Read-only, service operator, cluster admin, Ambari admin, and View-only roles; upgrade and non-wizard owner; direct entry to each restricted route |
| `SHELL-003` | `NEEDS_RUNTIME_VALIDATION` | `normalizeInternalPath()` accepts only single-slash internal paths and rejects root, login, absolute URLs, and `//host`; the value is consumed after recovery; tests cover safe and rejected examples | Browser refresh, SSO round trip, session expiry, and active logout; each wizard writing/clearing its server-side recovery key |
| `SHELL-004` | `NEEDS_RUNTIME_VALIDATION` | `DocumentTitleUpdater` updates only `document.title`; authentication and shell initialization show separate loading/error/retry states; no title-placeholder DOM is rendered | Cluster name, Installer, Views, Experimental, and deep-link titles; slow-request progress; successful Retry after initialization failure |
| `SHELL-005` | `NEEDS_RUNTIME_VALIDATION` | Admin/readonly property routing; enabled only when greater than 0; 60-second warning; Continue/Sign Out; window and dynamic iframe activity; wizard/upgrade exclusion; tests cover timeout selection, countdown, and exclusion paths | Actual admin/readonly property units; same-origin and cross-origin View iframes; dynamic iframes; both warning buttons; route changes and timeout=0 |
| `SHELL-006` | `NEEDS_RUNTIME_VALIDATION` | Ambari version is cached during initialization; About uses read-only context, displays `N/A` when missing, and makes no click-time request | Main, Installer, View-only, and no-cluster; properties request failure; Network records from opening About repeatedly |
| `SHELL-007` | `NEEDS_RUNTIME_VALIDATION` | `/experimental` is protected by `AMBARI.MANAGE_SETTINGS`; uses classic supports keys; Save synchronizes shared context; Reset additionally requires persisted-data permission and is disabled for non-owners; clears local state and reloads the page only after server reset succeeds | Supports present/missing/invalid; Save/Cancel; Reset success/failure; another user owning an Installer, Kerberos, or HA wizard |
| `SHELL-008` | `NEEDS_RUNTIME_VALIDATION` | Maven injects `VITE_AMBARI_VERSION`; Installer is blocked only when both client/server values are non-empty and differ; tests cover empty/match/mismatch | Maven packaged artifact; matching and mismatching versions and an empty development-build version; blocked page displays both versions |
| `SHELL-009` | `NEEDS_RUNTIME_VALIDATION` | Preserves three gates: menu `AMBARI.MANAGE_SETTINGS`, handler `CLUSTER.UPGRADE_DOWNGRADE_STACK`, and save `CLUSTER.MANAGE_USER_PERSISTED_DATA`; background/timezone/default persistence, cluster/View privileges, and error feedback are implemented | Different combinations of the three permissions; first-login default write-back; string/JSON persistence; timezone reload; no privileges, Ambari admin, and API failure |

## Backend API Comparison

| Ember contract | React implementation | Static conclusion | Runtime gate |
| --- | --- | --- | --- |
| `POST /api/v1/auth` | `LoginApi.authenticate()` | UTF-8 Basic, `Content-Type: text/plain`, and `skipAuthRedirect` implemented | Knox/proxy headers and 403/500 bodies |
| `GET /api/v1/users/{userName}?fields=*,privileges/...` | `LoginApi.handleSuccessfulLogin()` | Single username encoding; atomically creates a session from user and privileges | Usernames with special characters and actual privilege shape |
| `GET /api/v1/users/{userName}/authorizations?fields=*` | `LoginApi.loadAuthorizationsCallback()` | Fails the entire session on error and does not reproduce the Ember `.complete()` defect | Authorization collections for different roles and 401/403/500 |
| `GET /api/v1/users/{userName}/privileges?fields=*` | `LoginApi.loadPrivileges()` | User Settings independently reads cluster/View fields | View instance/version/name and no-privilege response |
| `GET /api/v1/clusters?...` | `probeSession()`, `getClusterData()`, `noopPolling()` | Separates session probing, cluster initialization, and keep-alive; probe can read the `User` header | SSO header, no cluster, incomplete cluster, session expiry |
| `GET /api/v1/settings/motd` | `LoginApi.loadLoginMessage()` | Tolerant parsing and blocking confirmation implemented | Valid/invalid/404/500 payloads |
| `GET /api/v1/logout` | `LoginApi.logout()` | Unified call; client cleanup does not wait for server success | Cookies, proxy, pending/failed requests |
| `GET /api/v1/services/AMBARI/components/AMBARI_SERVER` | `ClusterApi.loadAmbariProperties()` | Properties and component version initialized and cached together | Response shape for each deployment version |
| `GET/POST /api/v1/persist[/key]` | `ClusterApi.getPersistData()`, `postPersistData()` | User Settings, supports, and wizard owner use the classic JSON-string value contract | 404/default, permission denial, concurrent wizard owner |
| `GET /api/v1/services/AMBARI?...component_version...` | `ServiceApi.getAmbariServerVersion()` | Selects the latest version for Admin View and constructs the browser URL | Multi-version ordering, empty response, Admin View not installed |

## Five-Pass Implementation Audit

| Pass | Independent entry point | Findings | Correction result |
| --- | --- | --- | --- |
| 1. Route and landing | Router, public/protected boundary, four landing paths, deep links | Saving root `/` as the preferred path causes self-redirect; `/adminView` lacks the classic route guard | Root path is no longer saved; Admin View retains the Upgrade guard for existing clusters and allows Ambari admin with no cluster |
| 2. Authentication and session | Basic, JWT, User header, MOTD, logout, keep-alive | Global 401 changed only the hash without clearing the React session; plain 403 was misclassified as session expiry; logout waited for the server | Added a session-expired event; separated 401/JWT challenges from business 403; complete client cleanup before background logoff |
| 3. API and persistence | All ten Auth/Shell endpoint categories, URL encoding, persist values | Failed session probing cleared the entire DB and broke Installer/HA/Kerberos recovery; Reset cleared local state before requesting the server | Session-only cleanup preserves the wizard namespace; only active logout/reset performs full cleanup; Reset is now server-first |
| 4. Permissions and state | Menu/route/handler/save gates, upgrade, wizard owner, feature flags | Settings defaults were not written back on first use; Navbar displayed non-responsive items during upgrade; `wizardIsNotFinished` omitted non-owners | Write back defaults when permitted; hide restricted menus according to classic semantics; include non-owners in global wizard state |
| 5. Asynchronous behavior and failure recovery | STOMP, polling, iframe, version, View-only, errors/Retry | A new STOMP client was created on every render; dynamic iframes were not monitored; Modal leaked internal props; View-only still exposed cluster controls | Use one stable STOMP client and tear it down; manage iframes with MutationObserver; filter Modal props; use minimal navigation for View-only |

## Intentionally Unreproduced Ember Defects

| Ember behavior | React decision |
| --- | --- |
| Preferred path checks only the first character and incorrectly accepts `//host` | Reject protocol-relative and absolute URLs |
| Authorization requests are chained through `.complete()` and may continue after failure | Do not create a session if user, privileges, or authorizations fails |
| A failed logoff may retain the keep-alive flag | Client logout and lifecycle teardown do not depend on the server logoff result |
| Experimental Reset does not wait for server persistence to complete | Successfully clear the server wizard owner first, then clear local state and restore the session with a full-page reload |

## Automated Evidence

`npm test` currently runs 7 Vitest files and 32 tests, covering:

- UTF-8 Basic Authorization and single username encoding.
- No session on authorization failure, session recovery from the `User` header, and logout-failure cleanup.
- JWT/local branches, redirect limit, session-expired event, and plain 403 separation.
- Rejection of `//host`, absolute, login, and root paths, plus acceptance of safe internal paths.
- Installed/default, preferred, incomplete, no-cluster, and View-only landing paths.
- Empty, matching, and mismatching client/server versions.
- Persisted JSON values, fallback, admin/readonly inactivity, wizard/upgrade exclusion, and countdown.
- Admin View existing-cluster guard and no-cluster Ambari admin landing.

`npm run build` has passed. Existing Sass deprecation, duplicate switch, `eval`, and large-bundle warnings come from the baseline and are neither completion evidence nor in scope for this module. Repository-wide `npm run lint` remains blocked by substantial `any`, `@ts-ignore`, and hook-dependency technical debt in the `frontend-refactor` baseline; the policy, API, layout, guard, page, and test files added by this module have no ESLint errors.

## Runtime Acceptance Matrix

The corresponding IDs may be changed to `COVERED` only after at least the following scenarios are executed:

1. Local Basic login: success, incorrect password, 500, network failure, and a username with special characters.
2. Knox JWT: successful round trip, `/login/local` bypass, loop limit exceeded, and proxy return URL.
3. Existing server session with empty localStorage; session expiry while running.
4. Four landing paths: installed cluster, incomplete cluster, no-cluster Ambari admin, and View-only.
5. Admin timeout, readonly timeout, timeout=0, Continue, Sign Out, wizard/upgrade exclusion, and same-origin/cross-origin dynamic View iframes.
6. MOTD enabled/disabled/malformed/404/500.
7. Logout success, failure, and pending; confirm that no subsequent keep-alive, polling, or STOMP work occurs.
8. Three User Settings permission layers, first-login default write-back, timezone reload, and cluster/View privilege lists.
9. Experimental Save/Cancel/Reset, persistence failure, and non-wizard owner.
10. Admin View navigation, fallback after version-request failure, and Maven packaged version match/mismatch.

## Issue and PR Scope

This module uses one primary JIRA and one primary PR rather than splitting work by feature ID. The English JIRA draft is in `react-current/issues/01-auth-shell.md`; the PR must use `apache/ambari:frontend-refactor` as its base and be pushed to `JiaLiangC/ambari`.
