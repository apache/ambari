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

# Cluster Installation Wizard

The entry points are `/installer/step0` through `/installer/step10`. Only `AMBARI.ADD_DELETE_CLUSTERS` can install a cluster; users without permission are redirected to Views. The wizard uses both the local DB and server-side cluster status for recovery; it is not a one-time form.

## Installation Modes and Reuse Boundaries

"Installation mode" must be understood at two levels. React must not merge them into a single form that covers only the default path:

1. Deployment targets are three wizard types: a new cluster, Add Host to an existing cluster, and Add Service to an existing cluster.
2. Host onboarding for a new cluster and Add Host has three paths: Linux SSH automatic bootstrap, HDPWIN PowerShell Remoting automatic bootstrap, and a user manually installing and starting the Agent in advance. HDPWIN only hides the SSH fields; it does not become manual registration.
3. Repository sources for a new cluster are Public Repository and Local Repository. Local Repository is further divided into uploading a VDF/XML file and entering a VDF URL.
4. Kerberos-enabled Add Host/Add Service is not a fourth installation wizard. It is a conditional branch that adds KDC session, Kerberos descriptor, and principal/keytab handling before Review/submission.

| ID | Deployment mode | Actual steps | Reused core controllers | Mode-specific behavior |
| --- | --- | --- | --- | --- |
| INST-MODE-001 | New cluster | 0 Cluster Name; 1 Version; 2 Install Options; 3 Confirm Hosts; 4 Services; 5 Masters; 6 Slaves/Clients; 7 Configs; 8 Review; 9 Deploy; 10 Summary | `wizardStep0` through `wizardStep10` | Creates the cluster, selects the stack/version/repository, creates all service/component/config/host relationships for the first time, and finally sets provisioning state to `INSTALLED` |
| INST-MODE-002 | Add Host | 1 Install Options; 2 Confirm Hosts; 3 Slaves/Clients; 4 Config Groups; 5 Review; 6 Deploy; 7 Summary | Reuses Step 2/3/6/8/9/10 of new installation and also uses `addHostStep4Controller` | Does not change the stack/service/master; assigns only slave/client components to new hosts. When components exist, adds the new host to an existing config group; otherwise skips Config Groups. Checks the KDC session before submission |
| INST-MODE-003 | Add Service | 1 Services; 2 Masters; 3 Slaves/Clients; 4 Configs; 5 Review; 6 Deploy; 7 Summary | Reuses Step 4/5/6/7/8/9/10 of new installation | Filters installed services; may skip Master, Slave, or Config steps according to service cardinality. A Kerberos cluster updates the descriptor; any non-empty `kdc_type` prefetches/displays CSV, while only Manual mode assigns principal/keytab creation and distribution to the user |
| INST-MODE-004 | Public Repository | Default branch of Step 1 for new installation | `wizardStep1Controller` | Uses the default OS repository from the version definition; still requires URL/JDK/version validation |
| INST-MODE-005 | Local Repository + VDF/XML file | Local/upload branch of Step 1 for new installation | `wizardStep1Controller` | Reads local file content and submits an XML dry-run; after success, saves VDF data and creates the version definition again without dry-run during Review submission |
| INST-MODE-006 | Local Repository + VDF URL | Local/URL branch of Step 1 for new installation | `wizardStep1Controller` | The server reads the VDF from the URL and performs a dry-run; Review submits the same source without dry-run |
| INST-MODE-007 | Linux SSH automatic bootstrap | Step 2-3 of new installation, Step 1-2 of Add Host | `wizardStep2Controller`, `wizardStep3Controller` | The UI/payload collects the SSH private key and SSH user/port. sudo/passwordless sudo are external prerequisites, not UI/payload fields. When `customizeAgentUserAccount=false`, Agent user is hidden and the payload forces `root`; when the flag is enabled for automatic installation, it is shown and required. Polls bootstrap first, then Agent registration; automatic registration timeout is 120 seconds |
| INST-MODE-008 | Manual Agent registration | Step 2-3 of new installation, Step 1-2 of Add Host | Same as above | Displays manual installation instructions, sends no bootstrap, sets the initial boot status directly to `DONE`, and polls only registration; manual registration timeout is 15 seconds |
| INST-MODE-009 | Kerberized Add Host | Add Host Review/Deploy conditional branch | `wizardStep8Controller`, `mainAdminKerberosController` | Obtains KDC session state before submission; the backend installation flow creates/distributes identity material for the new host; failure cannot bypass the deployment state machine |
| INST-MODE-010 | Kerberized Add Service | Add Service Config/Review conditional branch | `wizardStep7Controller`, `wizardStep8Controller` | Reads security status and validates/updates the cluster Kerberos descriptor. Review prefetches and provides CSV for any non-empty KDC type; Manual mode additionally requires the user to create/distribute principals/keytabs manually before deployment |
| INST-MODE-011 | HDPWIN PowerShell automatic bootstrap | Step 2-3 of new installation, Step 1-2 of Add Host | `wizardStep2Controller`, `wizardStep3Controller`; server `BootstrapWindows`/`PSR` | `useSSH=false` hides the SSH key/user/port and Agent user UI as a block, but `manualInstall=false` by default, so it still sends a POST to `/bootstrap`. The default payload is `sshKey=""`, SSH `user=""`, and `sshPort="22"`; when `customizeAgentUserAccount=false`, `userRunAs="root"`. When the flag is enabled, the hidden Agent user is empty, becomes `userRunAs`, triggers required validation, and disables Next; this is a legacy defect. The server runs bootstrap through PowerShell Remoting according to the Windows OS family |

### Core Step Reuse Matrix

| Legacy core capability | New cluster | Add Host | Add Service | Conditional navigation |
| --- | --- | --- | --- | --- |
| Repository/version | Step 1 | Not entered; reuses the cluster version | Not entered; reads the installed version definition to display the service version | Public/Local, file/URL |
| Install Options/Confirm Hosts | Step 2/3 | Step 1/2 | Not entered | Linux SSH/HDPWIN PowerShell/manual |
| Choose Services | Step 4 | Not entered | Step 1 | Displays only uninstalled and installable services |
| Assign Masters | Step 5 | Not entered | Step 2 | With `skipMasterStep`, Add Service moves from Step 1 to Step 3 or later |
| Assign Slaves/Clients | Step 6 | Step 3 | Step 3 | Add Service skips when `skipSlavesStep`; Add Host still enters it to allow client-only or no-component installation |
| Customize Configs | Step 7 | Config Groups dedicated Step 4 | Step 4 | Add Host goes directly to Review without components; Add Service skips when `skipConfigStep` |
| Review/Create resources | Step 8 | Step 5 | Step 5 | Submit first writes the `*_DEPLOY_PREP_2` checkpoint, then runs the cleanup/creation chain; Step 8 does not write provisioning state |
| Install/Start/Test | Step 9 | Step 6 | Step 6 | Retry is shown only for `INSTALL FAILED` and calls install again; `START FAILED` has no Retry |
| Summary | Step 10 | Step 7 | Step 7 | Back navigation is disabled for all lower steps; Complete cleans up the corresponding wizard |

### Back, Retry, and Irreversible Boundaries

| Stage | Back/Cancel | Retry | Data and side-effect boundary |
| --- | --- | --- | --- |
| Selection and assignment | Can go back; changing the stack/service/host clears downstream recommendations, assignments, and configs | Reruns validation/recommendation | Primarily local DB/persist data; cluster resources have not been created |
| Confirm Hosts | Can go back; leaving stops bootstrap polling; the UI does not automatically uninstall started or registered Agents | Can retry only failed hosts or remove failed hosts | Bootstrap/Agent registration has already created host-side side effects, but the host has not joined the cluster |
| Before Review submission | Can return to the last applicable configuration/assignment step; confirms discarding configuration changes | Repository/preinstall/descriptor validation can be rerun | Blueprint and configs can still be safely recalculated |
| After Review submission | Lower steps are disabled; a resource-queue failure reopens Submit/Back but does not roll back | Resubmission rebuilds and replays the entire creation queue from current client data | Successful cluster/services/components/configs/host-components are not rolled back; replay may encounter existing resources |
| Deploy | The new Installer allows only the code-defined Admin View/Views exceptions; Add Host/Add Service `unroutePath()` always returns false | Retry appears only for `INSTALL FAILED`; `START FAILED` has no Retry. Add Host/Add Service also allow `INSTALL FAILED` to enter Summary | Server request/task state is authoritative; only `INSTALL FAILED` still on Deploy has a legacy UI retry entry |
| Summary | Normal UI has only Complete, with no Back/Retry; although a `back` handler remains in the route, the template has no entry and lower steps are disabled | Retry must be completed on Deploy; after entering Summary, the legacy UI cannot return to view tasks | Only new-cluster Complete sets provisioning state to `INSTALLED`; Add Host/Add Service Complete refreshes existing cluster models |

### Server-Side Recovery State

| Wizard | Server `clusterState` | Recovery step/behavior |
| --- | --- | --- |
| New installation | `CLUSTER_DEPLOY_PREP_2` | Recovers the Review/submitted preparation phase and prevents editing lower-level steps |
| New installation | `CLUSTER_INSTALLING_3`, `SERVICE_STARTING_3` | Recovers Deploy Step 9 and continues polling the current request |
| New installation | `CLUSTER_INSTALLED_4` | Recovers Summary Step 10; Complete still has to set provisioning state |
| Add Host | `ADD_HOSTS_DEPLOY_PREP_2` | The legacy route actually maps to Step 4 Config Groups, one step earlier than conceptual Review; this is the precise recovery behavior |
| Add Host | `ADD_HOSTS_INSTALLING_3`, `SERVICE_STARTING_3` | Actually maps to Step 5 Review rather than Deploy Step 6, where the request is running |
| Add Host | `ADD_HOSTS_INSTALLED_4` | Actually maps to Step 6 Deploy rather than Summary Step 7 |
| Add Service | `ADD_SERVICES_DEPLOY_PREP_2` | Recovers Review Step 5 |
| Add Service | `ADD_SERVICES_INSTALLING_3`, `SERVICE_STARTING_3`, `ADD_SERVICES_INSTALLED_4` | All three go directly to Step 7 Summary. The Summary controller generates its summary only from persisted hosts/tasks and does not restart Step 9 request polling, so active installation/start state can be downgraded prematurely to a static Summary |

Recovery is determined jointly by three sources: server-side cluster status stores `clusterState` and `wizardControllerName`; the local DB/server persist stores `currentStep` and input data; and `wizardWatcherController` determines whether the current user owns the wizard. Another window or a non-owner user cannot start a second conflicting wizard and is instead routed to the current wizard or restricted as a non-wizard user. `app/data/controller_route.js` is the registry of recoverable long-running flows in an installed cluster; the installation, Add Host, and Add Service routes also map server state back to their actual steps independently.

### Entry Gates for the Three Wizard Types

| ID | Entry behavior | UI gate | Route gate and deep-link boundary | Primary evidence |
| --- | --- | --- | --- | --- |
| INST-ENTRY-001 | New Installer `/installer/step0` through `step10` | The installation entry is exposed only to `AMBARI.ADD_DELETE_CLUSTERS` | The `installer` route independently enforces the permission; failure redirects to Admin View/Views, so a deep link cannot bypass it | `app/routes/installer.js`, `app/router.js` |
| INST-ENTRY-002 | Add Service `/main/service/add/step1` through `step7` | Service Actions requires both `SERVICE.ADD_DELETE_SERVICES` and `supports.enableAddDeleteServices` | The `addService` route checks the same permission and flag again; a deep link cannot bypass them | `app/templates/main/service/all_services_actions.hbs`, `app/routes/add_service_routes.js` |
| INST-ENTRY-003 | Add Host `/main/host/add/step1` through `step7` | The Host bulk-operation menu requires `HOST.ADD_DELETE_HOSTS` | The `addHost` route has no permission or feature gate; a direct URL can create the modal and enter the flow. This is a legacy authorization boundary that React route/mutation layers must fix | `app/templates/main/host/bulk_operation_menu.hbs`, `app/routes/add_host_routes.js` |

## Entry, Recovery, and Cancellation

| ID | Function and behavior | Preconditions/boundaries | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| INST-FLOW-001 | After login, enters the Installer when no cluster is installed; an existing incomplete state restores the corresponding step | Checks server/web client version, supports, permissions, and cluster provisioning state | Router cluster/version/status requests | `app/routes/installer.js`, `app/router.js` |
| INST-FLOW-002 | Saves currentStep and selected stack/services/hosts/components/configs/recommendations to the local DB/persist at each step | Forward navigation saves current data; back navigation loads prior steps; cannot jump directly to an incomplete higher-level step | Persist/cluster status requests | installer/wizard controllers, DB/persist mixins |
| INST-FLOW-003 | Recovers after another window or a crash from `wizardControllerName`, clusterState, and currentStep | A non-wizard initiating user may be restricted to read-only/non-wizard user | Cluster status/persist | `app/data/controller_route.js`, wizard watcher |
| INST-FLOW-004 | Cancel Install displays confirmation and, after confirmation, only routes to `/adminView` | `cancelInstall()` does not clear wizard/local/persist/cluster status, delete the cluster, or wait for cleanup; this is separate from the existing-cluster deletion chain during Review Submit | No backend cleanup request; full-page Admin View navigation | `app/controllers/installer.js#cancelInstall`, `app/controllers/application.js#goToAdminView`, controller test |
| INST-FLOW-005 | Prevents Back/Next double-clicks and disables navigation while a request runs | Router-level `btnClickInProgress`, reset after asynchronous completion | None | `app/router.js`, installer routes |
| INST-FLOW-006 | Wizard ownership and multi-window recovery | `wizardWatcherController` reads/sets the current user; a non-wizard user cannot make concurrent changes; a crash or new window returns to the long-running flow through `wizardControllerName` | Wizard user/status requests | `app/data/controller_route.js`, `app/router.js` |

## Step 0 Get Started

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-0-001 | Enters the cluster name | Required; validates length, whitespace, and special characters; cannot continue without an available stack | `wizard.stacks` |
| INST-0-002 | Loads installable stacks and initializes wizard data | Displays an error on load failure; clears the previous stack/repository selection | `wizard.stacks`, version definitions |

## Step 1 Select Version

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-1-001 | Selects a stack and version definition | Displays only `show_available=true`; supports the default version; returns to the previous step when no definition exists | `wizard.stacks_versions_definitions` |
| INST-1-002 | Selects Public Repository | Warns when the network is unavailable and allows switching to Local; displays repositories for each OS | Version definition/repository load |
| INST-1-003 | Selects Local Repository and adds a version by uploading VDF/XML or entering a VDF URL | Dry-run validates stack/version/OS/repositories; warns that reset is required when the stack changes | `wizard.step1.post_version_definition_file.xml`, `.url` |
| INST-1-004 | Edits Base URL by OS, adds/removes OS entries, restores defaults, or clears them | OS/repository IDs must be unique; validates URL format and reachability; can explicitly skip validation | `wizard.advanced_repositories.valid_url` |
| INST-1-005 | Retry repository validation | Reruns all failed repositories after network recovery | Repository validation requests |
| INST-1-006 | Validates the Ambari Server JDK against the version definition's `min_jdk`/`max_jdk` range | Compares versions only for non-Custom JDK when a range is defined; incompatibility does not hard-block, but opens a danger-style confirmation and continues with `Proceed Anyway`. Custom JDK skips range validation because it has no comparable `java.version` | Ambari server properties, `wizard.stacks_versions_definitions` |
| INST-1-007 | Local Repository can switch to RedHat Satellite/Spacewalk managed-repository mode | Controls are disabled for Public Repository; enabling requires confirmation and disabling switches directly. Once enabled, Base URL/OS add/remove/skip-validation controls are disabled, empty URLs are allowed, and URL validation is skipped with `verify_base_url=false` | Writes `ambari_managed_repositories=false` when Review submits the version definition; disabling preserves default managed-repository semantics |

## Step 2 Install Options

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-2-001 | Enters host names and supports `[01-10]` pattern expansion | On read, applies `toLowerCase()`, then `trim().split(/\s+/g)`, pattern expansion, deduplication, and format validation. Splits only on whitespace; commas are not separators. When installed and new hosts are mixed, filters old hosts and continues with a prompt; blocks with an already-installed error only when no new hosts remain after filtering | Persisted hosts/local DB; no host-query mutation in this step |
| INST-2-002 | Selects Linux SSH automatic Ambari Agent installation | SSH private key and SSH user/port are required. Agent user is not optional: when the feature flag is disabled it is hidden and the payload is forced to `root`; when enabled it is required for automatic installation. sudo/passwordless sudo are external host prerequisites, not UI/payload fields | Subsequent `wizard.launch_bootstrap` |
| INST-2-003 | Selects manual Agent registration | Displays manual installation instructions and Ambari Java Home; waits for hosts to register themselves | Ambari properties/host registration load |
| INST-2-004 | Warns for suspicious input such as a hostname/IP without a dot; the user can return to edit or confirm continuation | Validation operates on lowercased values; installed hosts use another prompt, and mixed input filters old hosts without blocking new hosts | No additional request |
| INST-2-005 | Add Host-only `Skip host checks` checkbox | Checking the box immediately changes state, then opens a prompt with only OK and no Cancel; it is not a reversible confirmation. It skips hostname resolution and generic preinstalled checks. After entering Confirm Hosts, `startHostcheck()` still runs the JDK check independently. The new Installer has no such control | The checkbox itself makes no request; without skipping, the first check uses `preinstalled.checks`/`.tasks`, while JDK still uses `wizard.step3.jdk_check` |

## Step 3 Confirm Hosts

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-3-001 | Starts bootstrap in Linux SSH or HDPWIN PowerShell automatic mode and polls RUNNING/DONE/FAILED and logs for each host | Both automatic modes POST to the same `/bootstrap`; a POST failure can be retried, and leaving the step stops bootstrap polling | `wizard.launch_bootstrap`, `wizard.step3.bootstrap` |
| INST-3-002 | Polls Agent registration, transitions DONE to REGISTERING/REGISTERED, and marks timeouts as failed | Calculates the registration timeout after the last bootstrap completes | `wizard.step3.is_hosts_registered` |
| INST-3-003 | Displays host status categories and each host's bootstrap `bootLog` | Supports status filtering; the popup provides only highlighted, read-only log text and has no Step 9 task-output or open-new-window capability | Bootstrap/host data |
| INST-3-004 | Retries bootstrap/registration for one or more failed hosts | Running items cannot be retried | Bootstrap requests |
| INST-3-005 | Removes one or more hosts | After confirmation, deletes the host only from the controller/local DB/memory; does not unregister the Agent or DELETE the server-side registered host. At least one host must be `REGISTERED` to continue | No backend request |
| INST-3-006 | Runs host checks for hostname resolution, last-agent-env, installed packages, existing repositories, and THP, with OS/disk categories from host information | Initial checks create a request through `preinstalled.checks` and poll it with `preinstalled.checks.tasks`; `wizard.step3.rerun_checks` is used only after the user triggers Rerun to refresh `last_agent_env`. The display merges generic `warnings` with separate hostname-resolution, JDK, repository, disk, and THP collections, but Submit shows confirmation only when generic `warnings.length>0`; the other collections affect display only and do not hard-block without confirmation. The only hard condition for Next is at least one host being `REGISTERED` | `preinstalled.checks`, `preinstalled.checks.tasks`; rerun also uses `wizard.step3.rerun_checks` |
| INST-3-007 | Checks JDK and displays host-specific warnings | Two request/task phases; generates warnings only by parsing `structured_out.java_home_check.exit_code`, with no stderr/error-log entry point | `wizard.step3.jdk_check`, `.get_results` |
| INST-3-008 | Displays other registered hosts not included in the current input | The user can confirm/check them to avoid omitting a registered Agent | Hosts registration load |

## Step 4 Choose Services

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-4-001 | Lists stack-installable services and supports single-select, select-all, and cancel, grouped by stack-available file system | Metrics services such as Ambari Metrics are not functional requirements here, but service dependency validation still follows stack metadata | `wizard.service_components` |
| INST-4-002 | Automatically prompts for and selects required dependencies | Missing dependencies can block or prompt for confirmation; existing/built-in dependencies are not duplicated | Stack dependency metadata |
| INST-4-003 | Validates file-system selection, multiple DFS, and Ozone/Spark/Ranger conflicts/recommended combinations | CRITICAL issues must be fixed; WARNING issues can be explicitly accepted | Client-side + stack metadata |
| INST-4-004 | Performs Choose Services combination validation entirely on the client | `validate()` runs dependency, filesystem, Spark, and several service-specific checks, blocking or requiring acceptance through the error stack/popup; this step does not call a Stack Advisor validation API | None; `config.validations` first appears in later host/component layout validation |

## Step 5 Assign Masters

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-5-001 | Stack Advisor recommends the initial layout of master components on hosts | Considers CPU/memory/disk, cardinality, co-host rules, and existing components | `config.recommendations`/stack advisor |
| INST-5-002 | Allows the user to move each master to another host and displays validation issues matching the current assignment | Client constraints can disable ineligible options. The server result retains only `type=host-component` items whose component and selected host both match the current master assignment. General issues, other types, and unmatched host-component issues are discarded; matching ERROR/WARN items are attached to the master, and both levels allow `Continue Anyway` | `config.validations` |
| INST-5-003 | Clears old recommendations and recalculates after dynamic service/component changes | Must refresh when re-entered after returning to Step 4 | Recommendation request |

## Step 6 Assign Slaves and Clients

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-6-001 | Assigns slaves/clients in a host x component matrix, with column All/None controls | Required/dependent components are automatically selected or disabled | Host/component metadata |
| INST-6-002 | Merges Master and Slave/Client selections into a Blueprint | Components that are invisible but required for deployment remain included | No mutation |
| INST-6-003 | Validates the Blueprint on the server, displays ERROR and WARN by general/host/component, and marks the host x component matrix | The legacy UI opens a danger-style Continue Anyway/Cancel prompt for any ERROR/WARN; ERROR is not a hard block. If React prohibits bypassing ERROR, record it as an intentional security difference | `config.validations` |
| INST-6-004 | Restores recommendations and selected hosts | Does not lose manual changes on return/refresh | Persist/local DB |

## Step 7 Customize Services

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-7-001 | Configures all selected services by service/theme/category | Consistent with Service Configs controls, validation, and theme semantics | `configs.theme.services`, stack configs |
| INST-7-002 | Provides dedicated Accounts, Credentials, Databases, and Directories tabs | Service/stack determines the tabs; validates password/confirmation, DB type/host/port, and other fields | Configs/recommendations |

The exact stack-config-before-Theme request order, five-category state machine,
canonical save collection, named Theme availability, Add Service differences,
and failure/retry matrix are specified in
[Service Theme and Configuration Layout](14-service-theme-layout.md#new-installer-category-state).
| INST-7-003 | Applies Stack Advisor recommendations, dependent config changes, and required changes | Required recommendations cannot be rejected; displays counts and filters for warnings/validation issues | `config.recommendations` |
| INST-7-004 | Tests database connections and external dependencies | Conditional functionality such as Hive/Oozie/Ranger | DB/custom action requests |
| INST-7-005 | Loads existing host overrides/config groups (reused Add Service/Host path) | A new cluster usually has no overrides; behavior must be preserved when reusing the controller | Config groups/overrides |
| INST-7-006 | Pre-Install Checks is only a shell | When `supportsPreInstallChecks`, Run only sets `preInstallChecksWhereRun=true` and opens an empty-body modal; there is no result model, errors/warnings, or severity blocking, and primary can be skipped directly when not run | No AJAX; `PLACEHOLDER` |
| INST-7-007 | Configuration changes can add components/change host assignments and update the subsequent Review Blueprint | Dynamic components require new recommendations/validation | Recommendations/validation |
| INST-7-008 | Confirms whether to discard changes when navigating back with modifications | Prevents silent data loss | None |

## Step 8 Review

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-8-001 | Summarizes cluster, repositories, services, masters, slave/client components, hosts, and configs | Provides an expandable host list | Load of prior steps |
| INST-8-002 | Print Review | Prints the current review in the browser | None |
| INST-8-003 | Download CSV | Add Service Review prefetches and displays CSV for any non-empty `kdc_type` (MIT/AD/IPA/Manual); Manual-specific behavior is the user's principal/keytab responsibility and warning, not the CSV button itself | `admin.kerberos.cluster.csv`; in-memory text browser download |
| INST-8-004 | Generate Blueprint ZIP | Generates `blueprint.json` and `clustertemplate.json` from the current assignment/config, then packages and downloads them together after confirmation. The cluster template contains host instances, `NEVER_APPLY`, and `INSTALL_AND_START`. The legacy Installer has only this export capability and no Blueprint/cluster-template import entry point | Local ZIP/Blob; no new backend request |
| INST-8-005 | Submit first queries all existing clusters | Installer mode does not only reject a duplicate name; after `GET /clusters`, it issues a DELETE for each returned cluster name, and repository-version/resource creation starts only after all succeed. On GET failure, the custom error callback sets `clusterNames=[]`, but jqXHR remains rejected and the caller has only a success-only `.then()`: it does not continue, show a chain-specific error, or unlock Step 8 | `wizard.step8.existing_cluster_names`, one `common.delete.cluster` per cluster |
| INST-8-008 | Existing-cluster deletion is a non-transactional batch side effect | DELETE requests are sent in parallel with no rollback for partial success/failure. After all requests settle and `clusterDeleteRequestsCompleted` reaches the total cluster count, the aggregate failure view opens a popup and remains on Review. This destructive logic is retained for the single-cluster model in the legacy implementation; React must not weaken it to a "duplicate-name check" | `GET /api/v1/clusters`, `DELETE /api/v1/clusters/{clusterName}` |
| INST-8-009 | Global Version Definition/Repository Version cleanup chain | After all cluster deletions succeed, runs `GET /version_definitions`. The new Installer enumerates each returned item and concurrently issues a DELETE for the corresponding repository version using its stack name/version/id; deployment proceeds only when the count reaches zero. The DELETE has no error callback, so a failure does not decrement the count, permanently locking the page; deleted items are not rolled back | `wizard.get_version_definitions`; `DELETE /stacks/{stackName}/versions/{stackVersion}/repository_versions/{id}` (`wizard.delete_repository_versions`) |
| INST-8-006 | Adds creation requests for the cluster, services, components, configs, hosts, host-components, config groups, and other resources to a dependency-ordered serial queue | The queue defaults to `abortOnError=true`; any failure clears the remaining queue, closes the progress dialog, remains on Review, and reopens Submit/Back/steps. Successfully created resources are not rolled back; resubmission rebuilds and replays the requests | `wizard.step8.create_cluster`, `.create_selected_services`, `.create_components`, `.register_host_to_cluster`, `.register_host_to_component`, `.apply_configuration_groups`, config PUT |
| INST-8-007 | Submits the selected VDF for Local Repository (without dry-run), then updates repository OS information | A VDF URL/XML POST failure displays an error, clears local VDF data, rejects the promise, and keeps Review locked. After success, the OS repository PUT is forced to resolve even on failure, so the resource queue continues. Step 8 saves only `*_DEPLOY_PREP_2` cluster status and does not write provisioning state | `wizard.step8.post_version_definition_file(.xml)`, `admin.stack_versions.edit.repo`, cluster status persist |

### Step 8 Submission and Failure Matrix

| Stage | Successful transition | Failure behavior | Rollback/resubmission boundary |
| --- | --- | --- | --- |
| Submit/KDC checkpoint | Disables Submit, Back, and lower steps; starts persistence of `CLUSTER_DEPLOY_PREP_2`, `ADD_HOSTS_DEPLOY_PREP_2`, or `ADD_SERVICES_DEPLOY_PREP_2` without waiting for completion before entering the next chain | Add Host/Add Service reopens buttons/steps when KDC session acquisition fails; Installer has no KDC session gate. Checkpoint persistence failure uses the default persist error prompt, but the GET/DELETE/creation chain has already started and is not blocked | The checkpoint is a recovery marker, not provisioning `INSTALLED` |
| Existing clusters GET | Installer obtains all cluster names; Add Host/Add Service also performs GET but does not delete existing clusters | jqXHR rejection prevents the success-only `.then()` from running; there is no dedicated popup/unlock, so Review remains silently locked | No creation side effect, but the user cannot resubmit on the current page |
| Existing clusters DELETE | Issues a DELETE for each cluster in parallel; continues global version cleanup when all requests settle, the completion count reaches the total, and there are no errors | Aggregated cluster DELETE errors open a popup and remain on Review; successfully completed DELETE operations are not restored | The only pre-cleanup stage with aggregate error views |
| Version definitions GET/DELETE | All three wizard types perform a GET for definitions; only the new Installer removes all corresponding repository versions by stack/version/id, continuing when the success count reaches 0; Add Host/Add Service enter the resource queue directly | GET failure uses only default AJAX error handling with no continuation or unlock, so the page remains locked; DELETE has no error callback, so any failure prevents the count from reaching zero and locks the page | Successfully completed DELETE operations are not restored |
| Non-dry-run VDF POST | Saves the new definition, obtains its ID/stack, and sends a PUT for repository OS data | POST failure displays an error, clears VDF data, and locks the page. A 2xx response without `resources[0].VersionDefinition` leaves the deferred unsettled; when the object exists but lacks id/stack name/version, it resolves but later guards do not run and there is no fallback. Both cases silently lock the page. Repository OS PUT failure is forced to count as complete, so deployment continues | No transaction and no restoration of just-deleted version resources |
| Resource AJAX queue | After all succeed, the route calls the install request, writes `*_INSTALLING_3`, and enters Deploy | Any failure aborts the remaining queue, records an error popup, remains on Review, and reopens Submit/Back | Successful resources are not rolled back; resubmission replays the entire queue |
| Add Host config-group assignment | After the resource queue completes and the route calls `next`, `applyConfigGroup()` sends an existing config-group PUT directly outside the queue and immediately triggers install; this is a concurrency exception outside the serial resource queue | The update is fire-and-forget: failure is invisible and does not block concurrent install, state persistence, or entry to Deploy | The UI provides no local retry; the server-side config group may not include the new host |
| Install request handoff | After the resource queue completes, calls the new Installer/Add Host/Add Service install mutation; the callback saves `*_INSTALLING_3` and enters Deploy | Rejection of the install mutation is also connected to the same progress callback; error handling records `isInstallError`/shows an error but still enters Deploy. There may be no new request ID, while Step 9 still attempts to poll as `PENDING`; this is a legacy failure-recovery gap | Does not return to Review and is not a resubmittable resource-queue failure |

## Step 9 Install, Start and Test

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-9-001 | Installs, starts, and runs service checks in phases, displaying progress by service/host/task | Service checks can be disabled by configuration; the state machine drives the UI | Common service/component updates, request polling |
| INST-9-002 | Displays each host's tasks, command details, stdout/stderr/error logs, with copy/new-window support | Large logs are lazy-loaded by task | `wizard.step9.load_log`, background task APIs |
| INST-9-003 | Categorizes hosts by status and displays failed-host details | Failure does not automatically enter Summary | `wizard.step9.installer.get_host_status` |
| INST-9-004 | Retry failed installation | The Retry button is controlled strictly by `status === 'INSTALL FAILED'` and calls `installServices` again. `START FAILED` has no Retry and can only enter Summary with Next. Add Host/Add Service include `INSTALL FAILED` in the Next state set and can therefore abandon retry directly to Summary; the new Installer cannot Next from `INSTALL FAILED`, but an observer re-enables its preceding-step links | Common update/request APIs |
| INST-9-005 | Prevents ordinary route navigation during deployment | Only the new Installer route guard allows `/adminView` and Views routes; Add Host/Add Service Deploy always `return false` and have no Admin View/Views exception | Route guard |
| INST-9-006 | Deploy Next saves the completion-stage cluster state and enters Summary | The new Installer writes `CLUSTER_INSTALLED_4`; Add Host writes `ADD_HOSTS_INSTALLED_4` and enters Summary through `alwaysCallback` even if state persistence fails; Add Service starts `ADD_SERVICES_INSTALLED_4` persistence and enters Summary without waiting. None sets provisioning `INSTALLED` | Cluster status persistence |

## Step 10 Summary

| ID | Function and behavior | Validation/boundaries | Backend requests |
| --- | --- | --- | --- |
| INST-10-001 | Summarizes installation, start, and check success/warnings/failures for each service | Allows completion with warnings; the template has only Complete, lower steps are disabled, and there is no reachable Step 9 entry to view/retry | Prior persisted request results; Summary starts no new polling |
| INST-10-003 | Summary retains an unreachable `back` route handler | Installer, Add Host, and Add Service routes define a handler returning to Deploy, but the shared Step 10 template has no Back button and `setLowerStepsDisable()` blocks sidebar `gotoStep()` | `STATIC_ONLY`; not a user feature |
| INST-10-002 | New Installer Complete clears the wizard, requests cluster provisioning state `INSTALLED`, resets clusterState to `DEFAULT`, and enters Dashboard | Only the new Installer performs the provisioning PUT; it uses `.complete()`, so both PUT success and failure continue to reset state and enter Dashboard. Add Host/Add Service Complete only closes the modal/refreshes the existing-cluster flow and does not write provisioning | `cluster.save_provisioning_state`, cluster status persist; Add Host/Add Service have no such PUT |

See the heuristic module interface inventory at [generated/api-by-module/installation-wizards.md](generated/api-by-module/installation-wizards.md). It is generated by broad matching of request names and caller paths, may include cross-module requests or omit Add Host/Add Service-specific calls, and must not be treated as a complete interface inventory. Authoritative verification must jointly inspect `generated/ajax-endpoints.md`, `generated/ajax-calls.md`, `generated/direct-http-calls.md`, `generated/browser-network-entrypoints.md`, and `generated/realtime-channels.md`.
