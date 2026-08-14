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

# Stack, Versions, Upgrade, and Cluster Admin

The entry point is `/main/admin/stack/{services|versions|history}`, and the upgrade-flow route is `/main/admin/stack/upgrade`. The core permissions are `CLUSTER.VIEW_STACK_DETAILS` and `CLUSTER.UPGRADE_DOWNGRADE_STACK`.

## Stack Services and Repositories

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| STACK-SVC-001 | Lists services, versions, installation state, and repository-version information for the current stack | Whether a service is installed affects the available actions | Direct stack/repository HttpClient | stack and upgrade controller, services template |
| STACK-SVC-002 | Navigates from an uninstalled service to Add Service | `SERVICE.ADD_DELETE_SERVICES` and feature flag; the link is unavailable when the conditions are not met | No additional request | services view/template |
| STACK-SVC-003 | Displays repository IDs, base URLs, and mirror lists for each OS | Repository metadata comes from the version definition | `cluster.load_repositories` | stack and upgrade controller |
| STACK-SVC-004 | Edits a repository base URL, restores the original value, clears the local repository, and saves | The Versions UI requires `AMBARI.MANAGE_STACK_VERSIONS` to enter editing, while popup Save also requires `App.isAdmin && !App.isOperator`. Stack Services repository rows when legacy stackVersions are unavailable have no template permission gate. `AMBARI.EDIT_STACK_REPOS` must not be used to describe both paths uniformly | `admin.stack_versions.edit.repo`, `wizard.advanced_repositories.valid_url` | `app/views/main/admin/stack_upgrade/upgrade_version_box_view.js`, `app/templates/main/admin/stack_upgrade/services.hbs` |
| STACK-SVC-005 | Validates a repository URL, with an option to skip validation | Can continue after a validation-failure warning when the user explicitly skips validation | `admin.stack_versions.validate.repo` | edit repositories template/controller |

## Versions List and Package Installation

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| VER-LIST-001 | Lists repo versions, displaying display name/version, stack, type, host counts, service support, and status | Statuses include NOT_INSTALLED, INSTALLING, INSTALLED, CURRENT, UPGRADING, UPGRADED, INSTALL_FAILED, and OUT_OF_SYNC | `admin.stack_versions.all`, stack/repo direct HttpClient | stack and upgrade controller, versions template |
| VER-LIST-002 | Filters by Not Installed, All, Upgrade Ready, Installed, Current, In Process, and Ready to Finalize | Counts refresh with the model | Same as VER-LIST-001 | versions view |
| VER-LIST-003 | Expands version details to view service, repository, and host state and reasons an upgrade is unavailable | Patch/Maint/Standard repository semantics differ | Repository/version load | versions/upgrade version templates |
| VER-LIST-004 | Views Current/Installed/Not Installed hosts by version status and navigates to filtered Hosts results | Disabled when there are no hosts | Stack version/hosts load | version hosts popup/view |
| VER-LIST-005 | Installs/reinstalls packages on all applicable hosts | `CLUSTER.UPGRADE_DOWNGRADE_STACK`; skips hosts in maintenance or that do not need or support the operation; confirmation is required | `admin.stack_version.install.repo_version`, request polling | stack and upgrade controller |
| VER-LIST-006 | Installs a version on a single host | Entered from Host Stack Versions | `host.stack_versions.install` | host stack version controller |
| VER-LIST-007 | Handles an OUT_OF_SYNC component by reinstalling or removing it | Validates component/host state and minimum instance counts | `common.host_components.update`, `host.host_component.delete_components` | stack and upgrade controller |
| VER-LIST-008 | Hides an unused or failed repository version | This confirmed PUT sets `RepositoryVersions.hidden` to `true` without deleting the resource; CURRENT/in-use versions cannot be processed | `admin.stack_versions.discard` | `app/controllers/main/admin/stack_and_upgrade_controller.js#confirmDiscardRepoVersion`, `app/utils/ajax/ajax.js` |
| VER-LIST-009 | Manage Versions navigates to the separate Admin View | Displays a prompt before leaving Cluster Management | `ambari.service.load_server_version` | versions view, router |

## Start Upgrade/Downgrade

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| UPG-START-001 | Determines whether the target version is compatible and loads supported upgrade types | The target must be INSTALLED/upgrade ready; unsupported direct jumps are prohibited | `admin.upgrade.get_compatible_versions`, `admin.upgrade.get_supported_upgradeTypes` | stack and upgrade controller |
| UPG-START-002 | Upgrade Options selects Rolling, Express, or Host Ordered | Host Ordered is controlled by `enabledWizardForHostOrderedUpgrade`; available types come from the server | Supported upgrade types request | upgrade options view/template |
| UPG-START-003 | Sets slave component failure and service check failure tolerance | Supported only by the corresponding upgrade type; warns about the risk of skipping | Included in the `admin.upgrade.start` payload | upgrade options view |
| UPG-START-004 | Runs/reruns Pre-Upgrade Checks and displays Required/Warning/Bypassed | A Required failure blocks progress; explicitly warns when server configuration allows bypass | `admin.upgrade.pre_upgrade_check` | stack and upgrade controller, check popups |
| UPG-START-005 | Runs additional custom cluster checks: maintenance, host heartbeat, previous upgrade, component installation, and service checks | `supports.preUpgradeCheck`/server check types; displays host/service details per item. In the previous-upgrade check, the Finalize button in the legacy template is incorrectly bound to `abortUpgrade`, while the actual `finalizeUpgrade` handler is never called. Mark it `BROKEN/PLACEHOLDER`; React must not reproduce it | Rolling checks and direct Hosts HTTP; the incorrect button calls `admin.upgrade.abort` | custom cluster check views, `custom_cluster_checks_prev_upgrade.hbs`, `custom_cluster_checks_prev_upgrade_view.js` |
| UPG-START-006 | Creates an Upgrade after confirmation | `CLUSTER.UPGRADE_DOWNGRADE_STACK`; displays a notification-suppression warning; Express explicitly warns about downtime | `admin.upgrade.start` | stack and upgrade controller |
| UPG-START-007 | Starts a Downgrade or retries an Upgrade from an aborted/failed upgrade | Buttons are determined by `downgrade_allowed`, current state, and target version | `admin.downgrade.start`, `admin.upgrade.retry` | stack upgrade controller/routes |
| UPG-START-008 | Performs a Patch/Maint upgrade or reverts it | Repository type/service selection determines the payload; lists affected services before revert | `admin.upgrade.start`, `admin.upgrade.revert` | stack and upgrade controller |

## Upgrade Execution State Machine

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| UPG-RUN-001 | Loads upgrade groups/items/tasks and displays total progress, current group, item, and status | Requests detailed data only for non-PENDING groups and continues polling | `admin.upgrade.data`, `admin.upgrade.state` | stack upgrade controllers/views |
| UPG-RUN-002 | Expands group/item/task details to view host, role, command, stdout/stderr, and copy or open loaded logs in a new window | Task details are lazy-loaded; the generic task UI does not display raw `structured_out` or provide downloads, and structured data is consumed only by the dedicated failure summary | `admin.upgrade.upgrade_item`, `admin.upgrade.upgrade_task` | upgrade group/task views, `app/templates/main/admin/stack_upgrade/upgrade_task.hbs` |
| UPG-RUN-003 | A HOLDING/manual step displays instructions; after the user confirms completion, Proceed sets the current UpgradeItem to `COMPLETED` | Item state and skippability determine the action. The view immediately clears the local manual-done checkbox after sending the request; mutation failure relies on global error handling and has no rollback | `admin.upgrade.upgradeItem.setState` | stack upgrade controller, `app/views/main/admin/stack_upgrade/upgrade_wizard_view.js#complete` |
| UPG-RUN-004 | A failed item can Retry, Skip/Ignore and Proceed, or view failed hosts | Retry sets the item to `PENDING`; Ignore/Proceed removes `HOLDING_` from `HOLDING_FAILED`/`HOLDING_TIMED_OUT` and sets `FAILED`/`TIMED_OUT`. Details close before the request completes and there is no dedicated rollback | `admin.upgrade.upgradeItem.setState`, task APIs | upgrade controller/popups, `app/views/main/admin/stack_upgrade/upgrade_wizard_view.js#retry`, `continue` |
| UPG-RUN-005 | Pauses/Suspends an Upgrade or Downgrade | A strong warning says not to modify the cluster while paused; sends a PUT for the current Upgrade with `request_status=ABORTED` and `suspended=true`, displaying a dedicated error on failure | `admin.upgrade.suspend` | stack upgrade controller, `app/utils/ajax/ajax.js` |
| UPG-RUN-006 | Resumes a paused Upgrade/Downgrade | Always uses `admin.upgrade.retry` to PUT the current Upgrade with `request_status=PENDING`; it does not branch by item state. The failure path has no callback to reset `requestInProgress`/`isRetryPending`, so the UI may remain stuck in pending state permanently | `admin.upgrade.retry` | `app/controllers/main/admin/stack_and_upgrade_controller.js#retryUpgrade`, `resumeUpgrade`, `app/utils/ajax/ajax.js` |
| UPG-RUN-007 | Abort of the current Upgrade is a prerequisite for starting a Downgrade or a legacy repair action for the previous-upgrade custom check | The normal progress wizard has no generic Abort/Stop button, only Pause and conditional Downgrade. Previous-upgrade Abort has no confirmation; "generic Abort + confirmation" must not be treated as a React feature parity requirement | `admin.upgrade.abort` | stack upgrade controller, stack upgrade wizard template, previous-upgrade custom check template |
| UPG-RUN-008 | The upgrade continues in the background when the progress modal closes and can be reopened by returning to Versions | The current upgrade ID/state is persisted and restored by the server | `cluster.load_last_upgrade`, upgrade data/state | routes/controller_route/stack upgrade route |
| UPG-RUN-009 | When another user starts an upgrade, the current user sees the initiator and a read-only/non-wizard state | Permissions still control whether the user can view/operate | Upgrade state/user persistence | wizard watcher, stack controller |
| UPG-RUN-010 | Suppresses notifications and restricts some host/service operations during an Upgrade | Feature flags such as `opsDuringRollingUpgrade` can relax some actions | No single request | Global app flags, service/host controllers |
| UPG-RUN-011 | Loads skipped service checks and failed hosts and confirms the final risk | Can pause to repair, downgrade, or ignore and continue | `admin.upgrade.service_checks` | finalize/failed hosts views |
| UPG-RUN-012 | Finalize Upgrade/Downgrade sets the last manual/finalize UpgradeItem to `COMPLETED`; Finalize Later reuses the Pause/Suspend flow | A revertible upgrade explicitly states that it cannot be reverted after finalization. The server orchestration advances the final submission of the cluster desired stack/version. `admin.stack_upgrade.run_upgrade` has only a registered definition and no production caller, so it cannot be listed as a Finalize endpoint | `admin.upgrade.upgradeItem.setState`; Finalize Later uses `admin.upgrade.suspend` | stack and upgrade controller, upgrade wizard view/template |

## Upgrade History

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| UPG-HIST-001 | Lists Upgrade/Downgrade history, displaying direction, type, repository/name/type, service from/to versions, status, and start/duration/end time | The UI does not display the request ID; the list loads through direct HttpClient and clears the ready spinner only on complete | `DIRECT:stack_upgrade_history_controller.js#upgradeHistoryUrl` | history controller/template |
| UPG-HIST-002 | Filters by nine categories: All, Upgrade, Downgrade, and Successful, Aborted, and Failed separately for Upgrade/Downgrade | There is no single Successful/Aborted/Failed filter spanning both directions | Same as UPG-HIST-001 | history controller/view |
| UPG-HIST-003 | Loads summary, group/item, and task state by request ID after selecting a history entry | Actually calls `admin.upgrade.data`; although the controller constructs a direct URL for one record, it does not use it. When details fail, the deferred resolves only on success and may leave a permanent spinner | `admin.upgrade.data` | `app/controllers/main/admin/stack_upgrade_history_controller.js`, history view |

## Service Accounts

| ID | Function and behavior | Preconditions/boundaries | Backend requests |
| --- | --- | --- | --- |
| ADMIN-ACCT-001 | `/main/admin/serviceAccounts` summarizes service users/groups across configs with displayType `user` and category `Users and Groups` | `SERVICE.SET_SERVICE_USERS_GROUPS`; returns to Dashboard without permission | Config tags/current configs |
| ADMIN-ACCT-002 | Displays service account names and values read-only in definition order | The page only loads, filters, sorts, and displays a table; it has no editing controls, Save action, or config mutation. See `app/templates/main/admin/serviceAccounts.hbs` and `app/controllers/main/admin/serviceAccounts_controller.js` for evidence | Config load only |

## Service Auto Start

| ID | Function and behavior | Preconditions/boundaries | Backend requests |
| --- | --- | --- | --- |
| ADMIN-AUTO-001 | `/main/admin/serviceAutoStart` loads global auto-start/recovery switches | Gates are inconsistent: the Admin menu requires START_STOP or MODIFY_CONFIGS, any auto-start permission, and `supports.serviceAutoStart`; the child route accepts any auto-start permission without checking the flag; the parent Admin `routePath` allows only upgrade permission/an upgrade in progress and may therefore block an auto-start-only direct URL first. See `app/views/main/menu.js`, `app/routes/main.js#admin`, and `adminServiceAutoStart` for evidence | `config.tags`, cluster-env config load |
| ADMIN-AUTO-002 | Lists restartable installed components by service and toggles `recovery_enabled` individually | Clients and uninstalled components are hidden; the view disables all controls without `CLUSTER.MANAGE_AUTO_START`, even though the child route accepts `SERVICE.MANAGE_AUTO_START`. See the service auto-start view/controller for evidence | `components.get_category` |
| ADMIN-AUTO-003 | Saves global `cluster-env.recovery_enabled` and the changed component set | Up to three requests run in parallel and the popup closes immediately; the transition callback and cached-status synchronization run only when all requests succeed, while any failure may leave partial server-side updates. See `app/controllers/main/admin/service_auto_start.js#showSavePopup` for evidence | `admin.save_configs`, `components.update` |
| ADMIN-AUTO-004 | Selects Save/Discard/Cancel when leaving with unsaved changes | Discard restores cached values | Same as ADMIN-AUTO-003 or no request |

`/main/admin/authentication`, `advanced`, and `audit` are `PLACEHOLDER`.

[generated/api-by-module/stack-upgrades.md](generated/api-by-module/stack-upgrades.md) is only a heuristic candidate inventory generated by broad matching of request names and caller paths. It may include cross-module requests or omit module-specific calls and is not complete at the module level. Authoritative verification must jointly inspect the global [AJAX definitions](generated/ajax-endpoints.md), [AJAX call sites](generated/ajax-calls.md), [direct HTTP](generated/direct-http-calls.md), [browser entry points](generated/browser-network-entrypoints.md), and [realtime channels](generated/realtime-channels.md).
