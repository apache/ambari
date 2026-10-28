<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Management Pack Catalog, Appearance, and Workspace Navigation

This follow-up implements the service/version grouping proposed in the previous
monitoring review and adds persistent appearance preferences and an explicit
return path from global directories. It continues the existing React frontend;
no Server API, mpack transaction, or cluster deployment contract is replaced.

## Baseline and intentional differences

Reviewed `ember-baseline/01-auth-shell.md`, the service/configuration baseline,
Classic `app/views/main/menu.js`, and the current React directory, configuration
group, and mpack implementations. Classic's shell has no equivalent runtime
management-pack catalog or light/dark preference. These are deliberate new
presentation/navigation capabilities rather than assertions of Classic parity.
Existing protected routes and operation recovery remain authoritative.

## Implemented behavior

- Catalog cards group exact service/stack/version identities. A group exposes
  one selected definition version, preferring an already-enabled definition.
  Otherwise numeric package versions sort descending without integer precision
  loss. Non-numeric versions have deterministic display ordering, not inferred
  compatibility. Exact provider IDs are retained in planning requests.
- Choosing another version replaces only the provider for that group; it does
  not select two versions of the same service. Stack/environment differences
  remain separate groups. Selected destination and selected services constrain
  compatible choices; the Server still validates the deployment plan.
- A sticky selection panel retains the selected service list, package versions,
  destination, and continuation action. Empty searches can clear their filters.
- Bundle import uses a dialog. Catalog, installed package management, and
  operation history are separate keyboard-operable tabs. History initially
  displays ten operations and can reveal more. Uncertain submissions and
  verified deployment handoffs preserve their existing identity/recovery flow.
- The global cluster/service/management-pack navigation uses a shared segmented
  workspace control. Cluster and Management Packs entries sit directly below
  the brand, above cluster features in both expanded and collapsed sidebars.
- The exact last cluster pathname and query are recorded in session storage
  under the authenticated principal. Global directories expose a Return to
  workspace link and preserve it across directory changes and reloads. Unsafe,
  external, traversal, malformed, and foreign-principal continuations are
  rejected. Router state provides an in-session fallback if storage is blocked.
  The link restores location, not unsaved form contents, and does not bypass
  current permissions or resource-existence checks.
- Appearance supports Light, Dark, and System through a top-bar menu, including
  login. Explicit preferences survive reload and synchronize across tabs;
  System responds to OS changes, and blocked storage does not prevent a theme
  change in the current tab. A bounded bootstrap script applies the preference
  before the application renders.
- Dark surfaces use graphite layers (`#0d1117`, `#161b22`, `#21262d`), primary
  text `#e6edf3`, secondary text `#9da7b3`, and an accessible green accent.
  IBM Plex Sans is bundled locally under OFL 1.1; native CJK sans-serif and
  monospace configuration/code fallbacks remain explicit.
- Theme rules cover legacy Summary/Configs tabs, table text, config version
  menus, React Select portal menus, dialogs, status labels, pagination,
  notifications, and query/code fields. Chart axes, legends, and series palettes
  react to theme changes. Status meaning remains unchanged.

## Regression and acceptance

Focused tests exercise exact provider replacement, numeric ordering, active
version preference, incompatible destinations, empty-search recovery, existing
mpack idempotency/recovery, theme persistence/system/storage handling, and safe
principal-scoped return navigation. Sidebar tests check global entries precede
cluster features; directory tests retain the exact previous configuration URL.

The local desktop acceptance covers grouping eleven Doris package versions into
one service card, selection, search, compatibility, appearance persistence,
system/explicit preference transitions, dark dialogs/config inputs, and return
navigation through global directories and reload. No import, uninstall, or
cluster modification is performed by the browser presentation checks.

Dark-mode contrast inspection samples visible active text across thirteen major
pages plus configuration-version and configuration-group menus. It resolves
computed foreground/background colors and checks 4.5:1 normal / 3:1 large-text
thresholds. Canvas, hidden, and disabled content requires separate visual
inspection and is not certified by this DOM check. The work remains desktop
focused as explicitly requested by the user.

Deployment reuses the running Server: new hashed assets and the bundled font
license are copied first, then `index.html` is replaced atomically. Old assets
and the preceding Web directory remain available for rollback.

## Verified results

- Full Vitest run: 258 files, 1,445 tests passed.
- Final focused run after palette refinement: 21 files, 89 tests passed.
- TypeScript/Vite production build and `git diff --check` passed.
- Chrome verified thirteen catalog/appearance workflows without uncaught
  exceptions. Ten service groups were rendered; eleven Doris package versions
  remained selectable inside one card.
- Chrome verified the promoted sidebar entries and the exact return URL through
  Clusters, Management Packs, and a full reload, including query parameters.
- The dark contrast pass checked 1,082 visible text observations across thirteen
  pages and two configuration menus, with no remaining findings below its
  normal/large-text thresholds. Uncaught browser exceptions: zero.
- DevTools confirmed Summary text uses the locally loaded IBM Plex Sans Medium
  font. Body, secondary, and accent text on the primary dark surface have
  contrast ratios of approximately 14.64:1, 7.09:1, and 11.09:1 respectively.

These observations describe the inspected desktop states. They are not a claim
of full accessibility certification for every role, plugin, or hidden state.
