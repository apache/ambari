## Problem

The React frontend does not yet provide a reliable authentication and application-shell boundary equivalent to the classic Ember UI. Authentication, server-session recovery, protected routing, landing-page selection, session lifecycle handling, persisted user settings, and global shell behavior are either incomplete or split across unrelated components.

The current implementation selects the login page from a one-time `window.location.hash` snapshot and mounts the router only after cluster initialization. An unauthenticated request to a protected URL can therefore remain on the loading screen. The `/main` and `/installer` trees do not share an authentication guard, authorization failures can still establish a client-side session, and usernames are URL-encoded by both the context and API layers.

This issue covers the complete non-Metrics Auth/Shell module defined by the classic UI baseline. Metrics pages are not part of the scope; an existing Metrics route may only be referenced as a landing route.

## Scope

* Establish a single router-driven boundary between public login routes and the authenticated application.
* Support local Basic authentication, Knox/external JWT redirection, `/login/local`, redirect-loop protection, and server-authenticated session recovery through the `User` response header.
* Load the user, privileges, and authorizations before declaring the session authenticated. Treat any required authorization-loading failure as a failed session.
* Load and display the login message of the day when enabled, while allowing login to continue when the setting is absent, invalid, or unavailable.
* Select the initial destination for installed clusters, incomplete installation, users with only View privileges, and Ambari administrators with no cluster.
* Preserve and restore only normalized same-origin application paths. Reject protocol-relative, absolute, and login targets.
* Consolidate logout, local-state cleanup, polling teardown, and server logoff behavior.
* Add the authenticated-session keep-alive lifecycle.
* Load global shell state in the correct order and avoid initializing full cluster operations for View-only users.
* Apply authentication and authorization checks independently to navigation visibility and direct route access.
* Implement document-title updates, cached About information, and actionable global loading/error states.
* Implement administrator and read-only inactivity timeouts, the 60-second warning, continue/sign-out actions, activity monitoring, and wizard/upgrade exclusions.
* Implement the Experimental page, persisted support flags, and Reset UI States with its separate persisted-data permission.
* Complete User Settings for background-operation preference, timezone, and current cluster/View privileges while preserving the classic UI's distinct visibility, handler, and persistence permission checks.
* Block the Installer on a packaged web-client/server version mismatch when a client version is available.

## Classic UI Baseline

The acceptance baseline is `docs/frontend-refactor/ember-baseline/01-auth-shell.md`, feature IDs `AUTH-001` through `AUTH-008` and `SHELL-001` through `SHELL-009`. The React gap analysis is recorded in `docs/frontend-refactor/react-current/01-auth-shell-gap.md`.

The authoritative network comparison must include all of the following classic contracts rather than only matching API file names:

* `POST /api/v1/auth`
* `GET /api/v1/users/{userName}` with privileges
* `GET /api/v1/users/{userName}/authorizations`
* `GET /api/v1/users/{userName}/privileges`
* `GET /api/v1/clusters` for authentication, cluster state, and keep-alive
* `GET /api/v1/settings/motd`
* `GET /api/v1/logout`
* `GET /api/v1/services/AMBARI/components/AMBARI_SERVER` for properties and version
* `GET` and `POST /api/v1/persist[/key]`
* Ambari Admin View browser navigation

## Acceptance Criteria

* Visiting `/`, `/main/...`, or `/installer/...` without an authenticated server session routes to `/login` and safely preserves an eligible target.
* A valid local login sends UTF-8 Basic credentials and URL-encodes the username exactly once at the URL boundary.
* User data, privileges, and authorizations must all load before protected content is rendered.
* A server-authenticated user with empty local storage is restored from the `User` response header.
* Authentication responses with `jwtProviderUrl` redirect to the provider with the current URL as the return target, except on `/login/local`; repeated redirects stop with a visible recovery path.
* An enabled message of the day requires acknowledgment before navigation. Missing, malformed, disabled, or failed MOTD retrieval does not block login.
* Landing behavior is correct for installed clusters, incomplete installation, no-cluster Ambari administrators, and View-only users.
* Restored targets are internal normalized hash paths and never protocol-relative or absolute URLs.
* Logout stops session-owned work, clears local UI state, attempts server logoff, and reaches login even when logoff fails.
* Keep-alive starts only for an authenticated application and stops on unmount or logout.
* Navigation visibility and direct route entry each enforce their required permissions.
* Inactivity handling selects the administrator or read-only timeout, warns 60 seconds before expiration, supports continue/sign out, monitors the window and accessible iframes, and skips wizard and stack-upgrade routes.
* About uses the server version cached during initialization and displays `N/A` when unavailable without a click-time request.
* User Settings loads and saves background-operation preference and timezone and displays cluster/View privileges under the documented permission gates.
* Experimental support flags can be loaded, edited, saved, canceled, and reset under the documented permission gates.
* A packaged client/server version mismatch blocks the Installer and displays both versions.
* Unit tests cover authentication branches, authorization failure, URL normalization, landing selection, permission gates, logout failure, inactivity behavior, and persisted settings.
* Runtime validation covers local Basic auth, Knox JWT, server-session recovery, each landing mode, administrator/read-only inactivity, MOTD variants, logout failure, Admin View navigation, and a packaged version mismatch.

## Out of Scope

* Metrics dashboards, charts, widgets, and Metrics data APIs.
* AngularJS Ambari Admin Console implementation. This issue only covers navigation into the existing Admin View.
* Feature work belonging to Hosts, Services, Alerts, Upgrades, Installer steps, Kerberos, or HA beyond the shell entry and lifecycle behavior described above.

## Compatibility Decisions

The React implementation must not reproduce known unsafe or failure-prone classic UI behavior:

* Protocol-relative preferred paths such as `//host/path` are rejected even though the classic prefix check accepts them.
* User and privilege data are not sufficient to establish a session when the authorization request fails.
* A plain business-operation `403 Forbidden` is not treated as proof that the authenticated session expired.
* Client logout and lifecycle teardown do not wait for successful server logoff.
* Reset UI States does not erase recoverable local wizard data until the server-side wizard owner reset succeeds.

These are intentional compatibility corrections, not missing parity.

## Verification Boundary

The module is not complete based on static code or unit tests alone. All feature IDs remain `NEEDS_RUNTIME_VALIDATION` until the runtime matrix in `docs/frontend-refactor/react-current/01-auth-shell-gap.md` is executed against real Ambari Server, Knox, Admin View, packaged web-client versions, and representative permission roles.
