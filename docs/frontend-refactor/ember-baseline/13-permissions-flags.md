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

# Permissions, Feature Flags, and Runtime Conditions Index

This is not a permission-name list. It is the behavior baseline for how the legacy Ember frontend determines feature entry points and operation capability. React must preserve RBAC, global long-running workflow exclusion, feature flags, stack/service metadata, component state, and maintenance conditions; relying only on a backend 403 after page load is not parity.

## Permission Determination Semantics

| ID | Rule | Exact legacy behavior | Primary evidence |
| --- | --- | --- | --- |
| GATE-AUTH-001 | Comma-separated permissions are OR | `App.havePermissions("A, B")` trims and checks each item; true when `App.auth` contains any one of them. React must not interpret this as requiring all items | `app/app.js#havePermissions`, `test/app_test.js` |
| GATE-AUTH-002 | `isAuthorized` adds wizard ownership restriction | `App.isAuthorized(x) = App.havePermissions(x) && !wizardWatcherController.isNonWizardUser`; when another user owns a long-running wizard, the current user cannot mutate even with RBAC permission | `app/app.js#isAuthorized`, `app/utils/helper.js#isAuthorized` |
| GATE-AUTH-003 | `havePermissions` is not restricted by wizard ownership | Template `havePermissions` is suitable for read-only visibility; the same permission can therefore produce different results under `havePermissions` and `isAuthorized` | `app/utils/helper.js` |
| GATE-AUTH-004 | Global upgrade exclusion and OR-expression contamination | When upgrade is not `NOT_REQUIRED/COMPLETED`, not suspended, and `supports.opsDuringRollingUpgrade=false`, a permission expression is rejected early only when the complete string contains neither `CLUSTER.UPGRADE_DOWNGRADE_STACK` nor `CLUSTER.MANAGE_USER_PERSISTED_DATA`; if the combined string contains either exception, the entire OR bypasses the upgrade deny and any other matching permission in the string returns true. Exceptions are not evaluated per permission | `app/app.js#havePermissions`, `app/views/main/menu.js` |
| GATE-AUTH-005 | No permission model means deny all | `havePermissions` returns false before `App.auth` is loaded; UI remains hidden/disabled instead of briefly showing an operation | `app/app.js`, login initialization |
| GATE-AUTH-006 | Users with only View permission use separate navigation | `isOnlyViewUser=true` when `App.auth` is empty or contains exactly `VIEW.USE`; after login, enters Views without loading the complete cluster operations shell | `AUTH-006`, `app/app.js#isOnlyViewUser`, `app/router.js` |
| GATE-AUTH-007 | Cluster Administrator elevates frontend role flags | When the current cluster privilege includes `CLUSTER.ADMINISTRATOR`, sets both `App.isAdmin=true` and `isOperator=true`; Ambari Administrator is confirmed only by `AMBARI.ADMINISTRATOR` | `app/router.js#loginGetClustersSuccessCallback` |
| GATE-AUTH-008 | UI permission is not a substitute for server authorization | Ember hides/disables entry points only for known actions; every mutation still requires server validation. React comparison must test unauthorized deep links and direct API calls | All routes/templates/controllers, Ambari REST RBAC |

## Ambari-Level Permissions

| Permission | Legacy Ember role | Related feature IDs | Gate type/boundary |
| --- | --- | --- | --- |
| `AMBARI.ADD_DELETE_CLUSTERS` | Enters Installer with no cluster; controls new-cluster-specific configuration behavior in installation Step 7 | `AUTH-006`, `INST-FLOW-001`, `INST-MODE-001`, `INST-0-*` through `INST-10-*` | Hard redirect in Installer route; without permission, goes to Views |
| `AMBARI.MANAGE_SETTINGS` | Enters `/experimental`, edits supports, and controls Background Operations footer management actions | `SHELL-007`, `BG-001` through `BG-005` | Route guard + template action; unauthorized users go to Views/Dashboard |
| `AMBARI.MANAGE_STACK_VERSIONS` | Manage Versions external link, out-of-sync reinstall/remove entries, and repository URL editing on Versions | `VER-LIST-001` through `VER-LIST-009`, `VIEW-ADMIN-003` | Must not be conflated with version install/reinstall/upgrade/hide-discard; those are controlled by `CLUSTER.UPGRADE_DOWNGRADE_STACK` disabling logic |
| `AMBARI.EDIT_STACK_REPOS` | Permission-model member for repository URL editing | `STACK-SVC-003` through `STACK-SVC-005`, `INST-1-*` | Application Admin Console link includes this permission; legacy stack pages also depend on cluster upgrade/version permissions, requiring runtime validation of role combinations |
| `AMBARI.ASSIGN_ROLES` | Displays the separate Ambari Admin Console entry | `SHELL-002` | Ember only navigates; user/role pages are AngularJS, `OUT_OF_SCOPE` |
| `AMBARI.MANAGE_USERS` | Displays the separate Ambari Admin Console entry | `SHELL-002` | AngularJS Admin Console, `OUT_OF_SCOPE` |
| `AMBARI.MANAGE_GROUPS` | Displays the separate Ambari Admin Console entry | `SHELL-002` | AngularJS Admin Console, `OUT_OF_SCOPE` |
| `AMBARI.MANAGE_VIEWS` | Displays the separate Ambari Admin Console entry | `SHELL-002`, Admin View navigation in the Views document | View-instance management is AngularJS; Ember only discovers the version and navigates |
| `AMBARI.RENAME_CLUSTER` | Displays the separate Ambari Admin Console entry | `SHELL-002` | No rename form exists in the current Ember tree, `OUT_OF_SCOPE` |
| `AMBARI.ADMINISTRATOR` | Sets global `App.isAdmin` and usually grants other capabilities through the server privilege collection | `AUTH-002`, `AUTH-006` | Not a universal bypass checked by every button; frontend still evaluates the specific returned permission names |

## Cluster-Level Permissions

| Permission | Legacy Ember role | Related feature IDs | Gate type/boundary |
| --- | --- | --- | --- |
| `CLUSTER.ADMINISTRATOR` | Sets `isAdmin/isOperator` role flags | `AUTH-002`, `SHELL-001` | Frontend role derivation; actions still use fine-grained permissions |
| `CLUSTER.USER` | Identifies users with only read-only cluster-user permission | `AUTH-006`, `SHELL-002` | Mapper classification; no direct mutation button |
| `CLUSTER.VIEW_CONFIGS` | Makes the Service Config tab visible | `SVC-CONFIG-001`, `SVC-CONFIG-002`, `HOST-TAB-001` | Uses `havePermissions`; remains available for read-only viewing while a long wizard is occupied |
| `CLUSTER.VIEW_STACK_DETAILS` | Makes the main Admin entry and read-only Stack/Versions pages visible | `STACK-SVC-*`, `VER-LIST-*` | ORed with upgrade permission; Admin routes have additional routePath restrictions, so deep links require per-route tests |
| `CLUSTER.TOGGLE_KERBEROS` | Kerberos page, Enable/Disable/Edit, and long-workflow recovery | `KRB-ENTRY-*` through `KRB-REC-*` | Route guard + button guard + recovery guard; also requires `supports.enableToggleKerberos` |
| `CLUSTER.UPGRADE_DOWNGRADE_STACK` | Admin/Stack/Upgrade routes, starting and controlling upgrade, Admin View navigation, and Kerberos CSV | `UPG-START-*`, `UPG-RUN-*`, `VER-LIST-*`, `KRB-MGMT-008` | Route guard + mutation; this permission is a global exclusion during upgrade |
| `CLUSTER.MANAGE_ALERT_NOTIFICATIONS` | Manage Notifications in Actions and target create/edit/delete | `ALERT-LIST-005`, `ALERT-NOTIFY-*`, `ALERT-GROUP-005` | Filters while constructing controller actions |
| `CLUSTER.TOGGLE_ALERTS` | Enables/disables cluster-scope alerts in definition details | `ALERT-LIST-004`, `ALERT-DEF-004`, `ALERT-SET-003` | Template guard; service scope uses `SERVICE.TOGGLE_ALERTS` separately |
| `CLUSTER.MODIFY_CONFIGS` | Combined visibility of the Admin menu/Auto Start and cluster-scope config capability | `SHELL-002`, `ADMIN-AUTO-*`, selected `SVC-CONFIG-*` | Often ORed with service permission; actual Service Config editing primarily checks `SERVICE.MODIFY_CONFIGS` |
| `CLUSTER.MANAGE_AUTO_START` | Enters Service Auto Start and enables the global switch, select-all, and per-component recovery checkboxes | `ADMIN-AUTO-001` through `ADMIN-AUTO-004` | ORed with `SERVICE.MANAGE_AUTO_START` for the route, but all page edit controls check only this cluster permission |
| `CLUSTER.MANAGE_USER_PERSISTED_DATA` | Allows writing UI persistence/user preferences and remains available during upgrade | `INST-FLOW-002`, all long-workflow recovery, user UI settings | Persistence mixin guard; without it, client state must not be written to the server |
| `CLUSTER.MANAGE_WIDGETS` | Dashboard/Service Metrics Widget editing | `OUT_OF_SCOPE` | Metrics are explicitly excluded and this is not a React acceptance item |

## Service and Host-Level Permissions

| Permission | Legacy Ember role | Related feature IDs | Gate type/boundary |
| --- | --- | --- | --- |
| `SERVICE.START_STOP` | Service, component, and host bulk start/stop/restart; abort-eligible requests; restart-required operations | `SVC-ALL-002` through `SVC-ALL-004`, `SVC-ACT-001`, `SVC-CONFIG-009`, `HOST-BULK-001`, `HOST-COMP-001` | Menu construction + template; still filters by current/desired state, maintenance, and upgrade |
| `SERVICE.RUN_SERVICE_CHECK` | Semantic permission for Run Service Check | `SVC-ACT-002`, test results in installation/security/HA flows | Legacy Service Actions does not use this as an independent gate; broad OR branches may add smoke tests through another service permission, ultimately relying on server authorization |
| `SERVICE.RUN_CUSTOM_COMMAND` | Semantic permission for stack custom commands, Refresh Queues, Rebalance, and specific service commands | `SVC-ACT-006` through `SVC-ACT-009`, `HOST-COMP-007`, `HOST-COMP-008` | Metadata/status filtering exists, but most commands after the broad Service Actions OR have no second frontend check for their semantic permission; ultimately relies on server authorization |
| `SERVICE.ADD_DELETE_SERVICES` | Add Service, Delete Service, and adding in Stack Services | `SVC-ALL-001`, `SVC-ACT-005`, `SVC-ADD-*`, `STACK-SVC-002` | Also requires `supports.enableAddDeleteServices`; Add Service route is hard-protected |
| `SERVICE.MODIFY_CONFIGS` | Service Config edit/save/revert, override, DB-test-related editing, and some restart UI | `SVC-CONFIG-003` through `SVC-CONFIG-010`, `CFG-GROUP-006`, `HOST-TAB-001` | Edit-control checks + save controller; old-version/compare modes are always read-only |
| `SERVICE.COMPARE_CONFIGS` | Config-version Compare | `SVC-CONFIG-006` | `havePermissions`, read-only capability |
| `SERVICE.MANAGE_CONFIG_GROUPS` | Manage Config Groups and group/host/override management | `CFG-GROUP-001` through `CFG-GROUP-006`, `HOST-ADD-004` | Template action; actual config save may also require modify configs |
| `SERVICE.TOGGLE_MAINTENANCE` | Semantic permission for service/component maintenance actions | `SVC-ACT-003`, `SVC-SUM-003`, `HOST-COMP-006` | Service Actions toggle is added directly through the broad OR branch without an independent frontend permission check; component paths may also be shown by Host maintenance permission, with server scope authorization still required |
| `SERVICE.TOGGLE_ALERTS` | Alerts entry actions, create-wizard route, and service-scope definition enable/disable/edit/delete | `ALERT-LIST-004`, `ALERT-DEF-002` through `ALERT-DEF-006`, `ALERT-CREATE-*` | Create also requires `supports.createAlerts`; Metric types are excluded |
| `SERVICE.DECOMMISSION_RECOMMISSION` | Bulk and single-component decommission/recommission for DataNode/NodeManager/RegionServer and others | `HOST-BULK-004`, `HOST-COMP-005` | Also requires `components.decommissionAllowed` and a safe state |
| `SERVICE.ENABLE_HA` | NN/RM/Ranger HA, JournalNode Management, Federation, and HAWQ Standby entry/recovery | `SVC-ACT-010`, `NNHA-*`, `JN-*`, `RMHA-*`, `RAHA-*`, `FED-*`, `RBF-*`, `HAWQ-*` | Service action + `serviceTypes`/feature/component preconditions; wizard ownership uniformly revokes operation capability |
| `SERVICE.MOVE` | Reassign Master entry | `SVC-MOVE-*`, `HOST-COMP-009` | Only `components.reassignable` masters; disabled when all hosts already have the master |
| `SERVICE.SET_SERVICE_USERS_GROUPS` | Admin Service Accounts page | `ADMIN-ACCT-001`, `ADMIN-ACCT-002` | Hard-protected route; separate capability from Config edit permission |
| `SERVICE.MANAGE_AUTO_START` | Allows entry to the Auto Start route | `ADMIN-AUTO-*` | ORed with cluster auto-start permission; page does not filter by authorized service, and all switches/checkboxes are disabled without `CLUSTER.MANAGE_AUTO_START`, so service-only users can only view |
| `SERVICE.VIEW_OPERATIONAL_LOGS` | Host Logs tab and task Log Search link | `HOST-TAB-004`, `BG-003` | Also requires LOGSEARCH service, `supports.logSearch`, and target host/log metadata |
| `HOST.ADD_DELETE_HOSTS` | Add Host and single/bulk host deletion | `HOST-ADD-*`, `HOST-DETAIL-004`, `HOST-BULK-008` | Add Host entry is also subject to wizard/upgrade exclusion; performs component safety checks before deletion |
| `HOST.ADD_DELETE_COMPONENTS` | Add, install, delete, and reinstall host components | `HOST-BULK-005`, `HOST-BULK-009`, `HOST-COMP-002` through `HOST-COMP-004` | Stack cardinality, last-master, and component state are additional hard conditions |
| `HOST.TOGGLE_MAINTENANCE` | Host maintenance and some host/component maintenance menus | `HOST-BULK-002`, `HOST-BULK-003`, `HOST-DETAIL-003`, `HOST-COMP-006` | Different scopes use different endpoints |
| `SERVICE.VIEW_METRICS` | Metrics columns/views | `OUT_OF_SCOPE` | Explicitly excluded |

## `App.supports` Feature Flags

This section is the authoritative enumeration of `App.supports.*` flags recognized by the generator. Their initial defaults are defined in `app/config.js` and are overridden by the server through `experimentalController.loadSupports()` when entering main/installer. React must not compile the current defaults into permanent behavior; non-`App.supports` runtime UI gates are listed in the next section.

| Flag (classic default) | Functional impact | Related feature IDs | Boundary |
| --- | --- | --- | --- |
| `enableAddDeleteServices=true` | Add/Delete Service and the Stack Services add link | `SVC-ALL-001`, `SVC-ACT-005`, `SVC-ADD-*`, `STACK-SVC-002` | AND with `SERVICE.ADD_DELETE_SERVICES` |
| `enableToggleKerberos=true` | Kerberos Admin menu/route/Enable/Disable/Edit | `KRB-ENTRY-002`, `KRB-MGMT-*` | AND with `CLUSTER.TOGGLE_KERBEROS`; Windows stack menu has a separate exclusion |
| `preKerberizeCheck=false` | Server checks before Enable Kerberos | `KRB-ENTRY-004` | When false, enters the wizard directly |
| `kerberosStackAdvisor=true` | Configure Identities recommendations | `KRB-4-002` | Called only when stored values are absent |
| `regenerateKeytabsOnSingleHost=false` | Host action Regenerate Keytabs | `HOST-DETAIL-007`, `KRB-MGMT-006` | AND with Kerberos enabled |
| `autoRollbackHA=false` | NN HA critical-stage Close/rollback mode | `NNHA-REC-*` | When true, hides the critical-step close button and enters automatic rollback; when false, provides manual rollback instructions |
| `manageJournalNode=true` | Manage JournalNodes in HDFS Service Actions | `JN-ENTRY-*` | Also requires HA_MODE and an adjustable host/JN count |
| `preInstallChecks=false` | Pre-Install Checks in Installer Customize Services | `INST-7-006` | Used only by a new-cluster installer; must not be applied to every reused Step 7 scenario |
| `customizeAgentUserAccount=false` | Ambari Agent OS user in SSH install options | `INST-MODE-007`, `INST-2-002`, `HOST-ADD-001` | When false, the bootstrap payload forces `userRunAs=root` |
| `skipComponentStartAfterInstall=false` | Install/Add Host/Add Service Step 9 state machine can skip start/check after installation | `INST-9-001`, `INST-9-004`, `HOST-ADD-006`, `SVC-ADD-007` | Changes progress weighting, completion state, and retry paths; dedicated controller tests cover it |
| `disableCredentialsAutocompleteForRepoUrls=true` | Whether browser credential/autocomplete-related input is retained when repository URLs change in Installer Step 1 | `INST-1-004` | The only executable consumer is `app/views/wizard/step1_view.js`; the Stack Services and Versions repository editors do not read this flag |
| `alwaysEnableManagedMySQLForHive=false` | Visibility of the Hive managed MySQL option during installation/Add Service | `INST-7-002`, `SVC-ADD-004` | Behavior differs between the regular Service Configs route and the installation wizard |
| `createAlerts=false` | Create Alert action | `ALERT-LIST-005`, `ALERT-CREATE-*` | Also requires alert toggle permission; Metric types are excluded |
| `preUpgradeCheck=true` | Pre-Upgrade checks and custom cluster check flow | `UPG-START-004`, `UPG-START-005` | The server still determines the specific checks and whether they can be bypassed |
| `enabledWizardForHostOrderedUpgrade=true` | Host Ordered upgrade wizard availability/restrictions | `UPG-START-002`, `UPG-RUN-*` | When false, controller `isWizardRestricted` prohibits related mutations |
| `displayOlderVersions=false` | Display of older stack versions in Hosts/Versions | `HOST-TAB-003`, `VER-LIST-001` through `VER-LIST-004` | When false, filters records for versions older than current |
| `opsDuringRollingUpgrade=false` | Whether ordinary permission-based operations are allowed during upgrade | `GATE-AUTH-004`, `UPG-RUN-010` | Implemented in global `havePermissions`; affects all modules |
| `serviceAutoStart=true` | Admin Auto Start menu/page | `ADMIN-AUTO-*` | Still requires cluster/service auto-start permission |
| `enableNewServiceRestartOptions=false` | New Restart Service option in Service Actions | `SVC-ACT-001`, `SVC-ACT-004` | Existing Restart All/rolling actions may still be available |
| `logSearch=true` | Host Logs tab, task/host Log Search links, and additional logging resource loads | `HOST-TAB-004`, `BG-003` | Also requires LOGSEARCH to be installed and operational logs permission |
| `logCountVizualization=false` | Log count visualization in Host Summary | `HOST-DETAIL-001` | Operational logs, not Metrics; requires runtime validation with LOGSEARCH |
| `installGanglia=false` | Whether the legacy GANGLIA service model is retained | `OUT_OF_SCOPE` | Ganglia is the legacy Metrics service and is not an installation requirement here |
| `customizedWidgetLayout=false` | Metrics widgets | `OUT_OF_SCOPE` | Explicitly excluded |
| `showPageLoadTime=false` | Development/experimental page-load timer | `OUT_OF_SCOPE` | Not an end-user business function |
| `redhatSatellite=false` | Defined by default in config, with no consumer call in the current classic `app/` | None | Legacy `STATIC_ONLY` capability; not a confirmed feature |
| `addingNewRepository=false` | Defined by default in config, with no consumer call in the current classic `app/` | None | Legacy `STATIC_ONLY` capability |
| `kerberosAutomated` | Only a TODO comment exists in the Add Service route; current runtime code does not read it | None | Do not create a React feature flag based on the comment |

## Other Runtime UI Gates

| ID | Gate (classic default) | Runtime source and functional impact | Boundary |
| --- | --- | --- | --- |
| GATE-RUNTIME-001 | `App.stackVersionsAvailable=true` | Recomputed after all upgrades/repository versions initially load, based on whether `App.StackVersion` is non-empty; controls the Admin Versions tab and Host Stack Versions route | Not a per-user support flag or RBAC; without versions, a Host deep link returns to Summary |
| GATE-RUNTIME-002 | `App.upgradeHistoryAvailable=false` | `restoreUpgradeState()` recomputes based on whether a completed/non-running upgrade exists; controls the Upgrade History tab | A sole running/paused upgrade does not make the history tab visible |
| GATE-RUNTIME-003 | `App.enableDigitalClock=false` | Local config directly controls the top-bar Clock view | Not currently overridden from supports; a shell UI gate, not business authorization |

## Stack, Service, and Component Metadata Conditions

| ID | Condition source | Legacy impact | Related features |
| --- | --- | --- | --- |
| GATE-META-001 | `StackService.isInstallable`, installed service names, and service dependencies | Installer/Add Service options and Delete Service dependency restrictions | `INST-4-*`, `SVC-ADD-001`, `SVC-ACT-005` |
| GATE-META-002 | Service `serviceTypes` contains `HA_MODE` | Shows corresponding HA actions for HDFS/YARN/RANGER/HAWQ | `NNHA-*`, `JN-*`, `RMHA-*`, `RAHA-*`, `HAWQ-*` |
| GATE-META-003 | Service `serviceTypes` contains `FEDERATION` | Shows NameNode Federation for HDFS | `FED-*` |
| GATE-META-004 | Service `serviceTypes` contains `DFSRouter` | Shows Router-based Federation for HDFS | `RBF-*` |
| GATE-META-005 | Component cardinality/min/max, `isMaster`, `isClient`, `isSlave`, and `isHAComponentOnly` | Host assignment, add/delete component, and skipped master/slave steps | `INST-5-*`, `INST-6-*`, `HOST-COMP-002` through `HOST-COMP-004`, `SVC-ADD-*` |
| GATE-META-006 | `components.reassignable` | Move Master action and target host | `SVC-MOVE-*`, `HOST-COMP-009` |
| GATE-META-007 | `components.decommissionAllowed` | Decommission menu | `HOST-BULK-004`, `HOST-COMP-005` |
| GATE-META-008 | Stack component custom commands | Visibility of Refresh/Rebalance/Knox/HBase and arbitrary commands | `SVC-ACT-006` through `SVC-ACT-009`, `HOST-COMP-007`, `HOST-COMP-008` |
| GATE-META-009 | Config types/themes/value attributes/dependencies | Config tab availability, controls, required/read-only/override behavior, and recommendations | `SVC-CONFIG-*`, `INST-7-*` |
| GATE-META-010 | Windows stack/stack family/version | Kerberos menu, selected services/commands, and legacy HAWQ capability | `KRB-ENTRY-002`, `HAWQ-*` |

## State, Maintenance, and Long-Workflow Exclusion

| ID | Condition | Required behavior | Related features |
| --- | --- | --- | --- |
| GATE-STATE-001 | Service/component desired/current state | Do not show Start when already STARTED; do not show Stop when already INSTALLED; prevent duplicate submission during a transition/request | `SVC-ACT-001`, `HOST-COMP-001`, all progress wizards |
| GATE-STATE-002 | Host/component/service maintenance | Disable or skip start/stop, delete, HA preconditions, and other actions by scope; authorized users can still toggle maintenance itself | `HOST-BULK-*`, `HOST-COMP-*`, `SVC-ACT-*`, HA documentation |
| GATE-STATE-003 | Heartbeat/host health | Add Host registration, Recover, deletion, HA host selection, and decommission safety checks | `INST-3-*`, `HOST-DETAIL-004`, `HOST-DETAIL-005`, HA documentation |
| GATE-STATE-004 | Stale configs/restart required | Restart Required/Refresh Configs applies only to affected components | `SVC-ALL-004`, `SVC-ACT-004`, `SVC-CONFIG-009`, `HOST-COMP-007` |
| GATE-STATE-005 | Background request/schedule already active | Avoid starting the same action again; show the existing operation or disable the action | `BG-*`, `HOST-BULK-010`, `SVC-ACT-001` |
| GATE-STATE-006 | `wizardWatcherController.isNonWizardUser` | Temporarily revoke global mutation capability; the current wizard owner continues, while other windows are read-only or routed to the workflow | `INST-FLOW-003`, `INST-FLOW-006`, `KRB-REC-*`, all HA recovery |
| GATE-STATE-007 | Upgrade state | Ordinary operations are disabled by default; the upgrade owner/authorized user can control pause/retry/finalize | `UPG-RUN-*`, `GATE-AUTH-004` |
| GATE-STATE-008 | Kerberos security type/KDC session | Validate or update identities before creating components through Add Host/Add Service/Reassign/HA and similar flows | `INST-MODE-009`, `INST-MODE-010`, `KRB-X-*`, HA documentation |
| GATE-STATE-009 | Cluster provisioning/wizard `clusterState` | Restore the current step/request after refresh or a crash; prevent concurrent creation from the entry point | `INST-FLOW-*`, `KRB-REC-*`, all HA/Federation recovery |

## Known Priorities and Migration Risks

| ID | Legacy expression | Risk/baseline conclusion |
| --- | --- | --- |
| GATE-RISK-001 | `!isWindows && isAuthorized(KERBEROS) \|\| upgradeRunning` | In JavaScript, `&&` has precedence over `\|\|`, so when an upgrade is running, the Admin Kerberos category may be shown even on Windows or without Kerberos permission; this is legacy static behavior and must not be treated as security authorization. Mark `BEHAVIOR_DIFF` in the React matrix and let maintainers decide whether to fix it |
| GATE-RISK-002 | Admin parent `enter` accepts four permissions ORed together, while `routePath` primarily requires upgrade permission | Menu visibility, the parent route, and child-route conditions are not fully consistent; every deep link requires a unit test rather than a single unified Admin guard |
| GATE-RISK-003 | Service Actions outer guard uses multiple permissions ORed together | Legacy code directly adds Refresh Configs, Restart All, Run Smoke Test, Toggle Maintenance, and various custom commands after the broad OR; most actions/controllers do not re-check their semantic permission, while HA, Move, and Add Component perform local checks. A user with any permission in the OR can therefore see or trigger several actions with other semantics, subject to server authorization. React must validate each action, route, and server authorization without reproducing this missing gate |
| GATE-RISK-006 | Upgrade exceptions are evaluated against the complete permission string | If a combined string contains an upgrade/persistence exception, other permissions in the string also bypass the global upgrade deny; if React corrects this per permission, mark `BEHAVIOR_DIFF` in the matrix and do not claim that the classic implementation already enforced strict isolation |
| GATE-RISK-004 | Client feature flags can be modified on the Experimental page | Flags are server/user-environment facts, not a trusted security boundary; the backend must continue to authorize operations |
| GATE-RISK-005 | Some permissions appear only on a separate Admin Console entry | Do not infer that classic Ember has the corresponding CRUD page merely because the permission name exists |

## Metrics Permission Boundary

`SERVICE.VIEW_METRICS`, `CLUSTER.MANAGE_WIDGETS`, and feature flags related to Metrics Widgets are all `OUT_OF_SCOPE`. Reading `metrics/...` fields for HA checkpoints, decommission, or component health safety decisions is not excluded because those reads support operational state machines rather than metric presentation.
