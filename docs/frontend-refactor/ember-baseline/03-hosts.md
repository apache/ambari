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

# Hosts Module

The entry point is `/main/hosts`, with details at `/main/hosts/:host_id/{summary|configs|alerts|stackVersions|logs}`. The Host Metrics route exists but is `OUT_OF_SCOPE`.

## List, Search, and Selection

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| HOST-LIST-001 | Paginates host name, IP, rack, health/heartbeat, maintenance, components, stack versions, and selection state | Server-side pagination/sorting; filters and selections are retained on refresh | Direct Hosts HttpClient, `hosts.bulk.operations` | `app/controllers/main/host.js`, `app/templates/main/host.hbs` |
| HOST-LIST-002 | Filters and sorts by host name, IP, rack, health, maintenance, component, component state, stale config, version, and other fields | The filter uses an Ambari predicate; some columns appear dynamically based on feature/service data | Direct Hosts HttpClient | `app/controllers/main/host.js`, `app/views/main/host/hosts_table_menu_view.js` |
| HOST-LIST-003 | Combines multiple facets, operators, and values in Combo Search, with support for adding, removing, and restoring tokens | Some facet values are lazy-loaded through server-side distinct queries | `hosts.with_searchTerm` | `app/controllers/main/host/combo_search_box.js`, `app/templates/main/host/combo_search_box.hbs` |
| HOST-LIST-004 | Selects one or multiple hosts, selects all current results, clears the selection, and retains the target host set across pagination/filter changes | The bulk menu displays the selection count; actions are disabled when there is no target | No single request | host controller/view/template |
| HOST-LIST-005 | Navigates from a host row to details, from a host health/alert count to that host's Alerts, and from a service component link back to filtered Hosts | Saves list filters before route changes | `alerts.instances.by_host` | `app/routes/main.js`, host templates/controllers |
| HOST-LIST-006 | Hosts CSV/list export | No entry point, handler, or download call was found in the legacy Hosts controller/view/template; marked `PLACEHOLDER`. Do not create a React feature requirement without additional runtime evidence | None | `app/controllers/main/host.js`, `app/views/main/host`, `app/templates/main/host.hbs` |

## Bulk Operations

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| HOST-BULK-001 | Starts, stops, or restarts a type of component in bulk on selected hosts | Operates only on components whose state permits the action and that are not in maintenance; displays the execution and skip lists | `common.host_components.update`, `common.batch.request_schedules` | `app/controllers/main/host/bulk_operations_controller.js` |
| HOST-BULK-002 | Enters or exits Host Maintenance Mode in bulk | `HOST.TOGGLE_MAINTENANCE`; updates only hosts whose state changes | `bulk_request.hosts.passive_state` | bulk operations controller |
| HOST-BULK-003 | Enters or exits Component Maintenance Mode in bulk | Filters eligible components; independent of host maintenance | `common.host_components.update` | bulk operations controller |
| HOST-BULK-004 | Decommissions or recommissions slaves such as DataNode, NodeManager, and RegionServer in bulk | `SERVICE.DECOMMISSION_RECOMMISSION`; HBase and HDFS/YARN use different request/polling checks | `bulk_request.decommission`, decommission status requests | bulk operations controller |
| HOST-BULK-005 | Reinstalls or installs components/clients in bulk | `HOST.ADD_DELETE_COMPONENTS`; filters installed or inapplicable items and displays progress for the asynchronous request | `common.host_components.update`, component install requests | bulk operations controller, install_component mixin |
| HOST-BULK-006 | Refreshes configs or configures components in bulk | Processes only components with stale config or refresh support | `host.host_component.refresh_configs`, `common.host_components.update` | bulk operations controller |
| HOST-BULK-007 | Sets Rack ID in bulk | Validates the rack format; submits only hosts whose values changed | Host rack update requests | bulk operations controller, `app/utils/hosts.js` |
| HOST-BULK-008 | Checks whether hosts can be deleted before bulk deletion and separates deletable and skipped items | `HOST.ADD_DELETE_HOSTS`; running components, the last master, and masters that cannot be re-added, among others, prevent deletion; confirmation is required | `common.hosts.delete` | bulk operations controller, delete popup templates |
| HOST-BULK-009 | Deletes host components of the same type in bulk after checking minimum instance counts and component states | `HOST.ADD_DELETE_COMPONENTS`; items that are not stopped, not installed, or below the stack minimum are skipped | `host.host_component.delete_components` | bulk operations controller |
| HOST-BULK-010 | Supports immediate or scheduled bulk actions and displays request context/progress | May be disabled when a pending schedule, wizard, or upgrade exists | `common.batch.request_schedules`, background request APIs | bulk operations controller, batch scheduled requests util |

## Host Details and Host Actions

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| HOST-DETAIL-001 | Summary displays host health, IP/rack, OS, uptime, basic disk/memory/CPU information, and the component list and state | Does not include the Metrics tab or metric charts | Host/global model load | `app/templates/main/host/summary.hbs`, `app/controllers/main/host/details.js` |
| HOST-DETAIL-002 | Sets the Rack ID for a single host | Validates and updates the model after confirmation | Host rack update requests | host details controller/template |
| HOST-DETAIL-003 | Enters or exits Host Maintenance Mode | `HOST.TOGGLE_MAINTENANCE`; displays an impact confirmation | `bulk_request.hosts.passive_state` | host details controller |
| HOST-DETAIL-004 | Deletes a single host | `HOST.ADD_DELETE_HOSTS`; reuses deletability checks and, when necessary, reconfigures special master associations first | `common.delete.host` and special configuration updates | host details controller, delete host popups |
| HOST-DETAIL-005 | Recover Host, after confirming that all host components are STOPPED, INSTALL_FAILED, or INIT, sequentially sets all components to `INIT` and `INSTALLED`; a Kerberos cluster additionally regenerates the host keytab | This is component recovery batching; it does not run Check Host environment checks or re-register the agent. Background Operations opens after the request succeeds | `common.batch.request_schedules` | `app/controllers/main/host/details.js#recoverHost`, recover popup templates |
| HOST-DETAIL-006 | Downloads all client configs on a host or a single client config | The Host Details template has no explicit permission gate; the single download uses `HOST_COMPONENT` scope and the full download uses `HOST` scope. The browser downloads the archive directly through `window.open`, with no in-app HTTP failure/retry; the legacy code calls `focus()` on null when the popup is blocked | Client config download URL | `app/mixins/main/host/details/support_client_configs_download.js`, `app/controllers/main/host/details.js#downloadClientConfigs`, details template |
| HOST-DETAIL-007 | Regenerates Kerberos keytabs for the host | Kerberos is enabled, `regenerateKeytabsOnSingleHost` is enabled, and the required administrative permission is present | `admin.kerberos_security.regenerate_keytabs.host` | host details controller |
| HOST-DETAIL-008 | Starts, stops, or restarts all operable non-client components on the host from Host Actions | `SERVICE.START_STOP`; all three actions are disabled on heartbeat failure. Stop includes the NameNode last-checkpoint safeguard; each action submits after confirmation and displays request progress | Host component state update/request APIs | `app/views/main/host/details.js#maintenance`, `app/controllers/main/host/details.js#doStartAllComponents`, `doStopAllComponents`, `doRestartAllComponents` |
| HOST-DETAIL-009 | Check Host starts pre-installed/environment checks for the host, polls tasks, displays warnings by category such as JDK, repository, disk, and THP, and supports rerun | The entry point is shown only for `App.isAdmin` or `App.isOperator`; this action is independent of Recover Host | `preinstalled.checks`, `preinstalled.checks.tasks` | `app/views/main/host/details.js#maintenance`, `app/controllers/main/host/details.js#doAction`, `app/mixins/main/host/details/actions/check_host.js` |
| HOST-DETAIL-010 | The log-count donut in Host Summary is displayed by service/log level | The counter is generated by `Math.random()` rather than backend operational-log data and is marked `PLACEHOLDER`; it cannot prove `SERVICE.VIEW_OPERATIONAL_LOGS` or require React to reproduce it | None | `app/views/main/host/log_metrics.js#logsData` |

## Host Component Actions

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| HOST-COMP-001 | Starts, stops, or restarts a component | `SERVICE.START_STOP`; disabled based on current/desired state, maintenance, and upgrade/wizard state | `common.host.host_component.update`, schedule/request polling | `app/templates/main/host/details/host_component.hbs`, details controller |
| HOST-COMP-002 | Installs or reinstalls a component or client | `HOST.ADD_DELETE_COMPONENTS`; creates the component/host-component first, then performs INSTALL | `common.create_component`, `host.host_component.add_new_component(s)`, state updates | install_component mixin |
| HOST-COMP-003 | Adds an optional component to a host | Validates stack cardinality, dependencies, host state, and service installation state; displays recommendation/conflict warnings | `host.host_component.add_new_component(s)`, recommendation requests | details controller, addDeleteComponentPopup |
| HOST-COMP-004 | Deletes a component | `HOST.ADD_DELETE_COMPONENTS`; validates the last instance, stopped state, and special components; requires additional confirmation for components such as JournalNode | `common.delete.host_component`, configuration update requests | details controller, delete popup |
| HOST-COMP-005 | Decommissions or recommissions a slave component | `SERVICE.DECOMMISSION_RECOMMISSION`; polls NameNode/HBase/YARN state to confirm completion | `host.host_component.decommission_slave` and status requests | details controller |
| HOST-COMP-006 | Enters or exits Component Maintenance Mode | `SERVICE.TOGGLE_MAINTENANCE` or the Host permission path; refreshes after the state change | `common.host.host_component.passive` | details controller |
| HOST-COMP-007 | Refreshes configs or component configs | Shown when stale or supported by the stack; runs a custom command and tracks the request | `host.host_component.refresh_configs` | details controller/template |
| HOST-COMP-008 | Executes a stack-defined custom command | `SERVICE.RUN_CUSTOM_COMMAND`; the command list comes from service/component metadata | `service.item.executeCustomCommand` | details controller/template |
| HOST-COMP-009 | Enters the Reassign Master wizard through Move Master | `SERVICE.MOVE`; only movable masters are eligible and the wizard validates target hosts | Reassign request group | details controller, reassign routes |
| HOST-COMP-010 | Upgrades a component or installs a host stack version | A stack version is available and the host is not upgraded; displays install progress | `host.stack_versions.install`, request APIs | stack versions controller/template |
| HOST-COMP-011 | When a Host Component is `UPGRADE_FAILED`, static state-icon and action-menu code displays Re-upgrade and, after confirmation, attempts to resubmit the component upgrade | The current server `State` no longer contains `UPGRADE_FAILED`, so normal production responses cannot expose the entry point. The icon entry requires `HOST.ADD_DELETE_COMPONENTS`, while the dropdown inherits `SERVICE.DECOMMISSION_RECOMMISSION` and the internal Re-upgrade has no independent gate. If legacy/injected data triggers it, the code calls the unregistered `host.host_component.upgrade`; `App.ajax.send` only warns and returns `null`, sends no HTTP, and opens no progress, while the payload hard-codes `HDP-1.2.2`. The legacy test proves only the call object because of a global stub; mark this `STATIC_ONLY/LEGACY_BROKEN/UNREGISTERED`, and React must not treat it as a valid API | `UNREGISTERED:host.host_component.upgrade` | `app/templates/main/host/details/host_component.hbs`, `app/views/main/host/details/host_component_view.js#isUpgradeFailed`, `app/controllers/main/host/details.js#upgradeComponent`, `app/utils/ajax/ajax.js#send`, `ambari-server/src/main/java/org/apache/ambari/server/state/State.java`, `test/controllers/main/host/details_test.js` |

## Detail Subpages

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| HOST-TAB-001 | Configs displays the host's config groups/overrides and properties by service | Cluster configs must be loaded; permissions determine whether editing is allowed | `config.tags`, `config.host_overrides`, `config_groups.all_fields` | `app/controllers/main/host/configs_service.js`, host configs templates |
| HOST-TAB-002 | Host Alerts lists alert instances for the host and can navigate to the service and definition | Alert instance polling stops when the page exits | `alerts.instances.by_host` | host alerts controller/template/route |
| HOST-TAB-003 | Stack Versions lists the host state for each repository version and can start an installation | `stackVersionsAvailable`; returns to Summary when unsupported | `host.stack_versions.install` | main route, stack versions view/template |
| HOST-TAB-004 | Logs lists service/component log files, opens or tails logs, and navigates to the Log Search UI | The menu requires `supports.logSearch`, installed LOGSEARCH, and `SERVICE.VIEW_OPERATIONAL_LOGS`; the direct route checks only `supports.logSearch`, so the other two do not block manual URL access | Log Search/host log endpoints | logs view/template, `app/routes/main.js`, `app/views/main/host/menu.js` |

## Host Logs and Log Search External Links

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| HOST-LOG-001 | `/main/hosts/:host_id/logs:query` displays component log-file metadata for the host and supports selection by service/component/file | The route guard requires only `supports.logSearch=true` and otherwise returns to Summary; LOGSEARCH installation and `SERVICE.VIEW_OPERATIONAL_LOGS` control only menu visibility, leaving a direct-URL gate gap in the legacy implementation | `host.logging` and global logging resource load | `app/routes/main.js`, `app/views/main/host/menu.js`, host logs template/view |
| HOST-LOG-002 | Opens a log-tail popup, selects the tail count, and loads text; supports copying or writing the current content to a new window | The new window writes only the loaded text and does not request the backend again; closing or switching the popup clears the clipboard | `logtail.get` | `app/views/common/modal_popups/log_tail_popup.js`, `app/templates/common/modal_popups/log_tail_popup.hbs` |
| HOST-LOG-003 | Opens the Log Search UI from a host log row or tail popup with host, component, and path/query parameters | The URL comes from the LOGSEARCH quick link, `LOGSEARCH_SERVER` host, and port; the browser handles an unreachable target | Quick-link/config/host load; the click is browser external navigation | `app/views/common/log_search_ui_link_view.js`, `log_tail_popup.js#logSearchUrl`, host logs template |
| HOST-LOG-004 | Background Operation, wizard, and ordinary log popups can copy stdout/stderr or write the current text/HTML to a new window | These `window.open()` calls output local documents rather than calling a new log API; the legacy code has no unified recovery when a popup is blocked | Task/log loading is completed by the corresponding feature request | host progress, logs popup, wizard Step 9 log views, `generated/browser-network-entrypoints.md` |
| HOST-TAB-005 | Metrics route/tab | `OUT_OF_SCOPE` | Excluded | `app/routes/main.js` |

## Add Host Wizard

The entry point is `/main/host/add`, with 7 steps. The Hosts menu entry requires `HOST.ADD_DELETE_HOSTS`, but the `addHost` route itself has no permission or feature gate. A manual direct URL can enter the wizard, which is an authorization boundary in the legacy implementation:

| ID | Step | Behavior and boundaries | Primary requests |
| --- | --- | --- | --- |
| HOST-ADD-001 | Step 1 Install Options | Normalizes host names to lowercase, then validates format, duplicates, and installed entries. When installed and new hosts are mixed, it warns, filters the old entries, and continues; it blocks only when all hosts are already installed. Linux automatic mode collects the SSH private key/user/port. When `customizeAgentUserAccount=false`, Agent user is hidden and the bootstrap payload forces `root`; when `true`, it is shown and required in automatic mode. sudo/passwordless sudo are external-host prerequisites, not UI/payload fields. HDPWIN automatic mode sets `useSSH=false` and hides all SSH and Agent user fields, but still sends those empty fields to `/bootstrap`, with the server using PowerShell Remoting. If `customizeAgentUserAccount` is also enabled, the hidden empty Agent user still disables Next; this is a legacy defect. Manual mode does not send bootstrap and only waits for a preinstalled Agent to register | `wizard.launch_bootstrap` or host registration polling |
| HOST-ADD-002 | Step 2 Confirm Hosts | Automatic mode starts/polls bootstrap and then waits for Agent registration; retry/remove are available. Initial environment checks create a task with `preinstalled.checks` and poll it with `preinstalled.checks.tasks`; only Rerun in the warnings popup uses `wizard.step3.rerun_checks`. Add Host's `Skip host checks` skips only hostname resolution and generic preinstalled checks. The prompt has only OK and is not a second confirmation where checks can be unchecked; the JDK check still runs independently | `wizard.launch_bootstrap`, `wizard.step3.bootstrap`, `preinstalled.checks`, `preinstalled.checks.tasks`; rerun uses `wizard.step3.rerun_checks` |
| HOST-ADD-003 | Step 3 Assign Slaves and Clients | Selects slave/client components for new hosts while respecting cardinality and dependencies | Stack component metadata, recommendations |
| HOST-ADD-004 | Step 4 Config Groups | Loads existing config groups for services associated with the selected slave/client components, then assigns new hosts to Default or an existing non-default group; skips this step when no components are selected. It saves only the selection and does not load or edit config recommendations | `config_groups.all_fields` and other config-group loads |
| HOST-ADD-005 | Step 5 Review | Summarizes host, component, and config-group selections. On submission, `applyConfigGroup()` updates selected non-default groups, but the caller does not await the promise and has no success/failure callback; failure is invisible to the UI, does not block subsequent component installation, and does not roll back the updated group | `config_groups.update_config_group`, host-component install requests |
| HOST-ADD-006 | Step 6 Install, Start and Test | Installs components, starts them, and runs required service checks, displaying logs and retry by host/task. The Deploy route's `unroutePath()` unconditionally returns `false`; Add Host does not inherit the new Installer's leave exceptions for Admin View/Views | Common service/component updates, request/task polling |
| HOST-ADD-007 | Step 7 Summary | Summarizes success/warnings/failures; Complete closes the wizard, returns to Hosts, and refreshes existing cluster models. It does not write cluster provisioning state `INSTALLED`; that write belongs only to the new Installer Complete flow | Host/status refresh; no provisioning mutation |
| HOST-ADD-008 | Wizard recovery and cancellation | Persists the current step and local DB state; other windows can resume; cancellation clears wizard state | Persist/cluster status requests |

[generated/api-by-module/hosts.md](generated/api-by-module/hosts.md) is only a heuristic candidate inventory generated by broad matching of request names and caller paths. It may include cross-module requests or omit exclusive calls such as Add Host, and is not complete at the module level. Authoritative verification must jointly inspect the global [AJAX definitions](generated/ajax-endpoints.md), [AJAX call sites](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md), [browser entry points](generated/browser-network-entrypoints.md), and [realtime channels](generated/realtime-channels.md).
