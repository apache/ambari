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

# Federation and HAWQ Standby Long-Running Workflow Baseline

This document covers five wizards in the legacy Ember frontend: NameNode Federation, Router-based Federation (RBF), and HAWQ Add/Remove/Activate Standby. All start from Service Actions and perform multi-step configuration and strictly serial server-side changes in a modal. This document excludes all Metrics displays, metric queries, charts, and HAWQ segment live status; state flags used only to wait for the HDFS namespace model are workflow-control dependencies, not product capabilities in this module.

> HAWQ evidence level: `CONDITIONAL / STATIC_ONLY`. The current `frontend-refactor` branch removed the server-side HAWQ/HDP stack definitions in commit `d680af8057` but retained the Ember route/controller/template. This document uses current UI source to describe frontend behavior and the stack, Kerberos descriptor, and agent command from `d680af8057^` to describe the historical backend contract. HAWQ cannot be shown to run successfully from static analysis of the current branch; all real-deployment conclusions require `NEEDS_RUNTIME_VALIDATION`.

## Entry, Permissions, and Visibility

| ID | Entry and behavior | Menu visibility conditions | Secondary preconditions on click | Route boundary |
| --- | --- | --- | --- | --- |
| NNF-ENTRY-001 | Enable NameNode Federation in HDFS Service Actions, entering `/main/services/NameNode/federation/step1` | The HDFS model's client-side hard-coded `serviceTypes` includes `FEDERATION`; requires `SERVICE.ENABLE_HA`, existing master/slave components, enabled NameNode HA, and at least 4 hosts | Runs `everyProperty(..., 'STARTED')` separately for filtered `ZOOKEEPER_SERVER` and `JOURNALNODE`; an existing non-STARTED component blocks entry. An empty component collection returns true, so the click still navigates | `namenode_federation_routes.js` has no permission or precondition guard; a direct URL bypasses the other menu conditions |
| RBF-ENTRY-001 | Enable Router-based Federation in HDFS Service Actions, entering `/main/services/NameNode/federation/routerBasedFederation/step1` | The HDFS model's client-side hard-coded `serviceTypes` includes `DFSRouter`; requires `SERVICE.ENABLE_HA` and multiple existing nameservices | Runs `everyProperty(..., 'STARTED')` separately for filtered `ZOOKEEPER_SERVER` and `JOURNALNODE`; an empty component collection returns true, so the click still navigates | `dfsrouter_federation_routes.js` has no route guard; a direct URL bypasses the other menu conditions |
| HAWQ-ENTRY-001 | Add HAWQ Standby in HAWQ Service Actions, entering `/main/services/highAvailability/Hawq/add/step1` | `CONDITIONAL / STATIC_ONLY`; the HAWQ model's client-side hard-coded `serviceTypes` includes `HA_MODE`; requires `SERVICE.ENABLE_HA`, master/slave components, a non-single-node deployment, and no existing `HAWQSTANDBY` | No independent click-time precondition check | The route has no permission/resource guard; a direct URL bypasses them |
| HAWQ-ENTRY-002 | Remove HAWQ Standby from the HAWQ Master custom command, entering `/main/services/highAvailability/Hawq/remove/step1` | `CONDITIONAL / STATIC_ONLY`; the stack's `HAWQMASTER.customCommands` includes `REMOVE_HAWQ_STANDBY`; Standby exists and Master is `STARTED` | No independent secondary authorization | The custom-command section requires any one of `SERVICE.RUN_CUSTOM_COMMAND, SERVICE.RUN_SERVICE_CHECK, SERVICE.TOGGLE_MAINTENANCE, SERVICE.ENABLE_HA`; the route also has no guard |
| HAWQ-ENTRY-003 | Activate HAWQ Standby from the HAWQ Standby custom command, entering `/main/services/highAvailability/Hawq/activate/step1` | `CONDITIONAL / STATIC_ONLY`; the stack's `HAWQSTANDBY.customCommands` includes `ACTIVATE_HAWQ_STANDBY`; Standby exists; Master/Standby service state is not checked | No independent secondary authorization | Uses the same broad OR permission section, without explicitly requiring `SERVICE.ENABLE_HA` again; the route has no guard |
| FHF-ENTRY-001 | All five wizards call `dataLoading()`, set the corresponding HDFS/HAWQ service as current, pause the normal update controller, and connect the current-step outlet inside a large modal | `dataLoading()` actually connects only the loading outlet and polls `clusterController.isLoaded`; it does not independently wait for the local DB or cluster status | NNF/RBF/HAWQ Add/Activate wait for this promise before creating the modal; HAWQ Remove does not, as described in `FHF-RISK-017` | Exit and ordinary navigation are controlled by each route's `unroutePath` and modal handler |
| FHF-ENTRY-002 | Each wizard view uses `WizardHostsLoading` to perform a GET for all hosts once when inserting the modal and writes the result to `content.hosts` | A modal-request failure still sets the view `isLoaded=true` but does not populate the host map; a second assignment request failure in NNF/RBF/Add Step 2 has no fail handler, so placement initialization does not complete | NNF/RBF/HAWQ Add each performs another GET for Step 2 assignment; initial one-way call counts are `2/2/2/1/1`, and Back, outlet reconstruction, or re-entering the modal can increase them | Remove/Activate send this request despite having no host assignment; React may remove the redundancy but must preserve actual workflow inputs that depend on the host map |
| FHF-ENTRY-003 | Each wizard controller calls `loadMasterComponentHosts()` when first constructing the installed master mapping | The helper waits for a legacy global loading flag whose name contains Metrics, then reads the local `App.HostComponent` model; this is an excluded data-loading dependency, not an interface or product capability in this module | When the local DB already has `masterComponentHosts`, restores directly without waiting for the flag | React should replace this naming coupling with an explicit topology-ready dependency and must not include the excluded loading chain in this module's interface inventory |

NameNode Federation's "existing HA, at least four hosts" and RBF's "multiple nameservices" come from action-disabled conditions; ZooKeeper/JournalNode state comes from the `mainAdminHighAvailabilityController` click handler. React must revalidate authorization and resource state at the route/action execution layer rather than copying menu visibility, or it will retain the legacy direct-URL authorization boundary.

## NameNode Federation Four-Step State Machine

### Step 1 Get Started

| ID | User behavior | Validation and branches | State/persistence and requests |
| --- | --- | --- | --- |
| NNF-1-001 | Views the existing nameservice list and irreversible/risk warnings, then enters a new nameservice ID | Displays an error and disables Next for an empty, duplicate, or invalid value | The namespace model comes from shared cluster bootstrap; an HA cluster conditionally calls `config.on_site` to read current `hdfs-site` on a config-cache miss; this step sends no dedicated write request |
| NNF-1-002 | Nameservice ID must be 1 to 63 characters and contain only ASCII letters, digits, and hyphens, with no leading or trailing hyphen | Regex `^([a-zA-Z0-9]\|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])$` | Next saves `nameServiceId` to the wizard local DB and enters Step 2 |
| NNF-1-003 | Duplicate validation against existing nameservice IDs depends on the asynchronous namespace model | The route's `dataLoading()` waits only for `clusterController.isLoaded`; the existing-ID list returns an empty array while `isHostComponentMetricsLoaded=false`, creating a window before two rounds of service/host-component loading complete in which an existing ID can be submitted as new; later flag changes recalculate it | No dedicated request in this step; the React loader must wait for topology/namespaces to be ready before enabling input and Next; `KNOWN_BUG` |

### Step 2 Select Hosts

| ID | User behavior | Validation and branches | State/persistence and requests |
| --- | --- | --- | --- |
| NNF-2-001 | Views all existing NameNodes (Current) and selects exactly two Additional NameNode hosts for the new namespace | `mastersToShow=['NAMENODE']`, `mastersToAdd=['NAMENODE','NAMENODE']`; existing NameNodes cannot be changed | Step 2 uses `hosts.high_availability.wizard` to load CPU, memory, disk, and maintenance state; initial loading calls `wizard.loadrecommendations` |
| NNF-2-002 | Can change the locations of the two new NameNodes in host dropdowns | `useServerValidation=false`; each value must be non-empty, the host must exist, installed hosts must have maintenance `OFF`, and component instances cannot share a host; does not call server validation after changes | Enables Next only after client-side checks pass |
| NNF-2-003 | Next saves all existing and new master-component-host mappings | Recovery reloads server masters and confirmed hosts; recommendations are used only as the initial layout | Writes the wizard local DB/cluster status and enters Step 3 |

### Step 3 Review and Configurations

| ID | User behavior | Validation and branches | State/persistence and requests |
| --- | --- | --- | --- |
| NNF-3-001 | Reviews current and new NameNode hosts and waits for configuration loading | First performs a GET for desired config tags, then reads `hdfs-site`; when Ranger is installed, also reads `core-site`, `ranger-tagsync-site`, and `ranger-hdfs-security`; when Accumulo is installed, reads `accumulo-site`; no dedicated failure handler exists for any GET, so the page usually remains loading with Next disabled | `config.tags` -> `admin.get.all_configurations` |
| NNF-3-002 | Reviews wizard-generated HDFS/Ranger/Accumulo changes | Only the new namespace's `dfs.journalnode.edits.dir.<newNs>` is an editable directory; all other generated properties are read-only and have `isOverridable=false` | This step saves only the form result to the local DB and does not yet write desired configs |
| NNF-3-003 | Edits the JournalNode directory for the new namespace | An initially empty value is required; accepts only Unix absolute directories, Windows drive paths, or `file:///` Windows URLs; rejects `/home*`, `/homes*`, leading spaces after commas, and trailing whitespace; disables Next while configs are loading or any config has `isValid=false` | Next saves `serviceConfigProperties` and enters the non-returnable Step 4 |

### Step 4 Configure Components

After Step 4 initialization, lower steps are disabled. The following 18 commands run strictly serially. Dynamically removed commands do not occupy a sequence position; any preceding command failure stops the queue.

| ID | Sequence/command | Exact behavior and target | Primary requests |
| --- | --- | --- | --- |
| NNF-4-001 | 1 `stopRequiredServices` | Stops all installed services except ZooKeeper; HDFS is also stopped | `PUT /clusters/{cluster}/services?ServiceInfo/service_name.in(...)`, state `INSTALLED` |
| NNF-4-002 | 2 `reconfigureServices` | Saves `hdfs-site` in one PUT and conditionally saves `ranger-tagsync-site` and `accumulo-site`; the success callback then creates/installs `HDFS_CLIENT` on all NameNode and JournalNode hosts, still within the same task | `common.service.multiConfigurations`, followed by the component existence/create/register/install chain |
| NNF-4-003 | 3 `installNameNode` | Creates and installs `NAMENODE` on the two new hosts | Component install chain |
| NNF-4-004 | 4 `installZKFC` | Creates and installs `ZKFC` on the two new hosts | Component install chain |
| NNF-4-005 | 5 `startJournalNodes` | Starts all existing JournalNodes; query forces `maintenance_state=OFF` | `common.host_components.update`, state `STARTED` |
| NNF-4-006 | 6 `startInfraSolr` | Starts only `AMBARI_INFRA_SOLR`; dynamically removed when Ranger or Infra is not installed | `common.services.update` |
| NNF-4-007 | 7 `startRangerAdmin` | Starts all `RANGER_ADMIN`; removed when Ranger is not installed | `common.host_components.update` |
| NNF-4-008 | 8 `startRangerUsersync` | Starts all `RANGER_USERSYNC`; removed when Ranger is not installed | `common.host_components.update` |
| NNF-4-009 | 9 `startNameNodes` | Starts existing NameNodes with `isInstalled=true` | `common.host_components.update` |
| NNF-4-010 | 10 `startZKFCs` | Starts ZKFC on existing NameNode hosts | `common.host_components.update` |
| NNF-4-011 | 11 `formatNameNode` | Runs `FORMAT` on the first new NameNode | `POST /clusters/{cluster}/requests`, HDFS/NAMENODE/host[0] |
| NNF-4-012 | 12 `formatZKFC` | Runs `FORMAT` on ZKFC on the first new host | `POST /clusters/{cluster}/requests`, HDFS/ZKFC/host[0] |
| NNF-4-013 | 13 `startZKFC` | Starts the first new ZKFC | `common.host_components.update` |
| NNF-4-014 | 14 `startNameNode` | Starts the first new NameNode | `common.host_components.update` |
| NNF-4-015 | 15 `bootstrapNameNode` | Runs `BOOTSTRAP_STANDBY` on the second new NameNode | `POST /clusters/{cluster}/requests`, HDFS/NAMENODE/host[1] |
| NNF-4-016 | 16 `startZKFC2` | Starts the second new ZKFC | `common.host_components.update` |
| NNF-4-017 | 17 `startNameNode2` | Starts the second new NameNode | `common.host_components.update` |
| NNF-4-018 | 18 `restartAllServices` | Requests `RESTART` for all non-excluded host-components in the cluster; excludes `NAMENODE`, `JOURNALNODE`, `ZKFC`, `RANGER_ADMIN`, and `RANGER_USERSYNC`, but has no stale-config filter | `restart.custom.filter`, command `RESTART` |
| NNF-4-019 | Complete | Enables Done after the final task reaches a terminal state; clears wizard DB/status, returns to Services, and refreshes the model | Cluster status/persist |

## NameNode Federation Configuration Transformations

| ID | Config type/property | Generation rule | First/subsequent branch |
| --- | --- | --- | --- |
| NNF-CFG-001 | `dfs.nameservices`, `dfs.internal.nameservices` | Appends `<newNs>` to the original nameservice list, comma-separated | Every generation |
| NNF-CFG-002 | `dfs.ha.namenodes.<newNs>` | The two new NN IDs are `nn(nameNodes.length-1),nn(nameNodes.length)`; the length includes all existing and new NameNodes | Every generation; IDs increase across the cluster's NameNode count and do not reset to `nn1/nn2` |
| NNF-CFG-003 | `dfs.namenode.rpc-address.<newNs>.<newNnId>` | `<newHost>:<rpcPort>`; port comes from generic `dfs.namenode.rpc-address`, defaulting to `8020` | One entry for each of the two new NNs |
| NNF-CFG-004 | `dfs.namenode.http-address.*`, `dfs.namenode.https-address.*` | Uses generic HTTP/HTTPS address ports, defaulting to `50070`/`50470` | Two entries for each of the two new NNs |
| NNF-CFG-005 | `dfs.client.failover.proxy.provider.<newNs>` | Fixed at `org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider` | Every generation |
| NNF-CFG-006 | `dfs.namenode.shared.edits.dir.<newNs>` | `qjournal://<allJnHost:8485;...>/<newNs>`; all JournalNode hosts participate, separated by semicolons | Generated every time and read-only |
| NNF-CFG-007 | `dfs.journalnode.edits.dir.<newNs>` | Initially empty and required; must pass the shared directory validation for `displayType=directory`; the only generated item with `isReconfigurable=true` | Generated every time; editable but cannot be overridden by a config group |
| NNF-CFG-008 | Scoped JN/shared edits for the original namespace | Copies generic `dfs.journalnode.edits.dir` to `dfs.journalnode.edits.dir.<oldNs>` and generates `dfs.namenode.shared.edits.dir.<oldNs>` | Only on the first expansion from NameNode HA to Federation, when the property has `firstRun`; removes it for existing Federation |
| NNF-CFG-009 | `dfs.namenode.servicerpc-address.*` | If any scoped service RPC exists for the original namespace, generates `:8021` for both NNs in the new namespace; on the first run, also adds `nn1/nn2` for the original namespace | Removes all four old/new generated entries when neither original-namespace entry exists |
| NNF-CFG-010 | Generic JN property cleanup | Deletes `dfs.namenode.shared.edits.dir` and `dfs.journalnode.edits.dir` from the server `hdfs-site` copy to be saved, replacing them with scoped properties | Performed on every Review calculation |
| NNF-CFG-011 | Ranger TagSync mapping | Writes `ranger.tagsync.atlas.hdfs.instance.<cluster>.nameservice.<ns>.ranger.service=<repoPrefix><ns>` for each nameservice and writes a cluster-level mapping for the `fs.defaultFS` namespace | Ranger installed only; repo prefix comes from the Ranger HDFS service name or `<cluster>_hadoop_` |
| NNF-CFG-012 | Accumulo volumes | Changes `instance.volumes` to all `hdfs://<ns>/apps/accumulo/data`; generates only one source URI per nameservice for replacement: uses that group's `hosts[0]` for an existing namespace and the first new NameNode for the new namespace, with source port hard-coded to `8020`, mapped to the corresponding namespace URI | Accumulo installed only; both entries are read-only; does not generate a replacement for each NameNode |
| NNF-CFG-013 | Write atomicity boundary | Combines HDFS/Ranger/Accumulo desired configs into one array-body PUT to `/clusters/{cluster}`, then installs HDFS clients | Whether the server makes multiple desired_config writes truly transactional requires `NEEDS_RUNTIME_VALIDATION` |

## Router-based Federation Four-Step State Machine

### Steps 1-3

| ID | Step | User behavior, validation, and actual side effects | Requests/state |
| --- | --- | --- | --- |
| RBF-1-001 | 1 Get Started | Reads the Router Federation instructions; no input, and Next enters host assignment directly | Local DB/cluster status |
| RBF-2-001 | 2 Select Hosts | Displays existing NameNodes/Routers and initially adds one Router; `mastersAddableInHA=['ROUTER']`, and users can add/remove new Routers with shared +/- controls | Step 2 calls `hosts.high_availability.wizard` again, followed by initial `wizard.loadrecommendations` |
| RBF-2-002 | 2 Host validation | `useServerValidation=false`; after edits, performs only client-side non-empty, host-existence, maintenance OFF, and same-component uniqueness checks; maximum count is limited by host count and stack cardinality | Current BIGTOP `ROUTER` cardinality is `0+`; recovery preserves existing Routers |
| RBF-2-003 | 2 Next | Saves all master-component-host mappings | Local DB/cluster status |
| RBF-3-001 | 3 Review | Displays Routers to add; reads `hdfs-rbf-site`, `hdfs-site`, and `core-site`, generating four read-only Router configs; also waits for the shared namespace-model gate; either config GET failure has no dedicated failure handler and usually remains loading | `config.tags` -> `admin.get.all_configurations`; cluster-bootstrap `config.on_site` occurs conditionally only on a cache miss |
| RBF-3-002 | 3 Config write | **The actual legacy behavior does not save on Next click**: after configs load and the namespace model is ready, the observer immediately sends a PUT, then renders and sets `isLoaded=true` without waiting for success | `common.service.configurations`; in an ordinary browser script, `sender:self` usually resolves to global `window/self` rather than the controller, so the request usually still sends, but success has no callback and the failure callback attempts to call global `onTaskError` |
| RBF-3-003 | 3 Next | The page may enable Next while the PUT is incomplete or even after final failure; Next only saves the form property snapshot to the local DB and enters Step 4 | It no longer reliably performs the initial save and does not gate on PUT success |

### Step 4 Configure Router

| ID | Sequence/command | Exact behavior and target | Primary requests |
| --- | --- | --- | --- |
| RBF-4-001 | 1 `installRouter` | The controller passes all existing and new Router hosts to the install helper; the helper creates `ROUTER` only on missing hosts, and the final PUT `INSTALLED` still includes all Router hosts while forcing `HostRoles/maintenance_state=OFF`. Therefore only non-maintenance Routers, including started ones, are set to `INSTALLED` and temporarily stopped; maintenance Routers are unaffected by the final PUT | Component existence/create/register/install chain; final `common.host_components.update` |
| RBF-4-002 | 2 `startRouters` | Starts Router hosts in the wizard mapping, excluding components in maintenance in the query | `common.host_components.update`, state `STARTED` |
| RBF-4-003 | Complete | Clears wizard state and returns to Services after both tasks complete | Cluster status/persist |
| RBF-4-004 | Unreachable code | The controller defines `reconfigureServices()` and the subsequent `installHDFSClients()`, but neither is in the `commands` array and the normal state machine never calls them | React must not treat unreachable methods as legacy functionality |

## Router-based Federation Configuration Transformations

| ID | Property | Value and source | Write boundary |
| --- | --- | --- | --- |
| RBF-CFG-001 | `dfs.federation.router.monitor.namenode` | Combines all namespace/NameNode pairs into a comma-separated list; NN suffixes use a continuous cross-namespace count, such as `<ns1>.nn1,<ns1>.nn2,<ns2>.nn3,<ns2>.nn4` | Step 3 observer sends the PUT immediately |
| RBF-CFG-002 | `dfs.federation.router.default.nameserviceId` | First item in the current nameservice list | Step 3 observer sends the PUT immediately |
| RBF-CFG-003 | `zk-dt-secret-manager.zkAuthType` | Fixed at `none` | Step 3 observer sends the PUT immediately |
| RBF-CFG-004 | `zk-dt-secret-manager.zkConnectionString` | `core-site/ha.zookeeper.quorum` | Step 3 observer sends the PUT immediately |
| RBF-CFG-005 | Editability and override | All four have `isReconfigurable=false` and `isOverridable=false` and are read-only in Review | Code assumes existing `hdfs-rbf-site.properties` always exists; when absent, it writes properties to `undefined` |

## HAWQ Add Standby Four-Step State Machine

All items below are `CONDITIONAL / STATIC_ONLY`.

| ID | Step | User behavior, validation, and branches | Requests/state |
| --- | --- | --- | --- |
| HAWQ-ADD-1-001 | 1 Get Started | Reads the maintenance-window warning that adding a Standby stops/starts HAWQ; Next clears old `hawqHosts` and the master assignment | Local DB/cluster status |
| HAWQ-ADD-2-001 | 2 Select Host | Displays the installed HAWQ Master and selects a host for one new `HAWQSTANDBY`; the Master cannot be moved | Host list + Stack Advisor recommendation |
| HAWQ-ADD-2-002 | 2 Client validation | The host must exist, be non-empty, and have maintenance OFF; generic validation only prevents multiple instances with the same `component_name` from sharing a host | `HAWQMASTER` and `HAWQSTANDBY` are different components, so Standby and Master on the same host pass this client validation; only basic errors directly block Next |
| HAWQ-ADD-2-003 | 2 Advisor validation | `useServerValidation=true`; initial layout requests a recommendation, and assignment changes and Submit each request a new recommendation followed by `config.validations` | The historical Advisor reports Master/Standby on the same host as `HAWQSTANDBY` host-component `ERROR` and warns about an Ambari Server/PostgreSQL 5432 conflict; the legacy UI still sets `submitDisabled=false`, allowing Continue Anyway in the issue popup |
| HAWQ-ADD-3-001 | 3 Review | Generates read-only `hawq_standby_address_host=<selectedHost>`; loads current `hawq-site` and reads `hawq_master_directory` | `config.tags` -> `reassign.load_configs` |
| HAWQ-ADD-3-002 | 3 Manual data-directory gate | Submit opens confirmation requiring the user to rename `<hawq_master_directory>` on the new Standby host or ensure it is empty to prevent starting with old data; the UI does not validate the directory remotely | After confirmation, checks the KDC session and enters Step 4 only on success |
| HAWQ-ADD-4-001 | 1 `stopRequiredServices` | Stops only the HAWQ service | `common.services.update`, state `INSTALLED` |
| HAWQ-ADD-4-002 | 2 `installHawqStandbyMaster` | Creates and installs `HAWQSTANDBY` on the new host | Component install chain, including KDC session check |
| HAWQ-ADD-4-003 | 3 `reconfigureHAWQ` | Reloads the latest `hawq-site`, merges `hawq_standby_address_host`, and saves a new desired config | `config.tags` -> `reassign.load_configs` -> `common.service.configurations` |
| HAWQ-ADD-4-004 | 4 `startRequiredServices` | Starts only the HAWQ service without requesting a smoke test | `common.services.update`, state `STARTED` |
| HAWQ-ADD-4-005 | Complete | Clears the Add wizard local DB, sets cluster status to `DEFAULT`, returns to Services, and reloads | Cluster status/persist |

## HAWQ Remove Standby Three-Step State Machine

All items below are `CONDITIONAL / STATIC_ONLY`.

| ID | Step/sequence | User behavior and exact side effects | Requests/state |
| --- | --- | --- | --- |
| HAWQ-REMOVE-1-001 | 1 Get Started | Reads and saves current `HAWQMASTER` and `HAWQSTANDBY` hosts; displays removal instructions | Local DB/cluster status |
| HAWQ-REMOVE-2-001 | 2 Review | Displays the Standby host to delete and the `hawq_standby_address_host` property to remove | Does not read configs; Submit checks the KDC session first |
| HAWQ-REMOVE-2-002 | 2 Irreversible confirmation | After KDC session success, the route shows an "Ambari cannot roll back" confirmation; confirmation disables lower steps in Step 3 | Local DB/cluster status |
| HAWQ-REMOVE-3-001 | 1 `removeStandby` | Executes the `REMOVE_HAWQ_STANDBY` custom command on the current Master host | `POST /clusters/{cluster}/requests`, HAWQ/HAWQMASTER/master host |
| HAWQ-REMOVE-3-002 | 2 `stopRequiredServices` | Stops only HAWQ | `common.services.update`, state `INSTALLED` |
| HAWQ-REMOVE-3-003 | 3 `reconfigureHAWQ` | Loads the latest `hawq-site`, removes `hawq_standby_address_host`, and saves it | Config GET chain -> `common.service.configurations` |
| HAWQ-REMOVE-3-004 | 4 `deleteHawqStandbyComponent` | Issues a DELETE for the `HAWQSTANDBY` host-component on the Standby host; treats `NoSuchResourceException` as success | `common.delete.host_component` |
| HAWQ-REMOVE-3-005 | 5 `startRequiredServices` | Starts only HAWQ | `common.services.update`, state `STARTED` |
| HAWQ-REMOVE-3-006 | Complete | Clears the wizard, sets status=`DEFAULT`, returns to Services, and reloads | Cluster status/persist |

The historical agent implementation of `REMOVE_HAWQ_STANDBY` executes `hawq init standby -a -v -r --ignore-bad-hosts`. Command semantics and failure output require `NEEDS_RUNTIME_VALIDATION` on a matching historical HAWQ stack.

## HAWQ Activate Standby Three-Step State Machine

All items below are `CONDITIONAL / STATIC_ONLY`.

| ID | Step/sequence | User behavior and exact side effects | Requests/state |
| --- | --- | --- | --- |
| HAWQ-ACT-1-001 | 1 Get Started | Reads and saves the original Master/Standby hosts and displays failover instructions | Local DB/cluster status |
| HAWQ-ACT-2-001 | 2 Review | Displays the original Master for deletion and the Standby to promote; generates read-only `hawq_master_address_host=<oldStandbyHost>` | Does not read server configs in this step; Submit checks the KDC session |
| HAWQ-ACT-2-002 | 2 Irreversible confirmation | After KDC success, the route shows a cannot-rollback confirmation; confirmation is required to enter progress | Local DB/cluster status |
| HAWQ-ACT-3-001 | 1 `activateStandby` | Executes `ACTIVATE_HAWQ_STANDBY` on the original Standby host | `POST /clusters/{cluster}/requests`, HAWQ/HAWQSTANDBY/standby host |
| HAWQ-ACT-3-002 | 2 `stopRequiredServices` | Stops only HAWQ; after historical agent activation, it may stop the still-running new Master process using the old port | `common.services.update`, state `INSTALLED` |
| HAWQ-ACT-3-003 | 3 `reconfigureHAWQ` | Loads the latest `hawq-site`, removes `hawq_standby_address_host`, writes `hawq_master_address_host=<oldStandby>`, and saves it | Config GET chain -> `common.service.configurations` |
| HAWQ-ACT-3-004 | 4 `installHawqMaster` | Creates and installs `HAWQMASTER` on the original Standby host | Component install chain, including KDC session check |
| HAWQ-ACT-3-005 | 5 `deleteOldHawqMaster` | Issues a DELETE for `HAWQMASTER` on the original Master host | `common.delete.host_component` |
| HAWQ-ACT-3-006 | 6 `deleteHawqStandby` | Issues a DELETE for `HAWQSTANDBY` on the original Standby host | `common.delete.host_component` |
| HAWQ-ACT-3-007 | 7 `startRequiredServices` | Starts only HAWQ | `common.services.update`, state `STARTED` |
| HAWQ-ACT-3-008 | Complete | Clears the wizard, sets status=`DEFAULT`, returns to Services, and reloads | Cluster status/persist |

The historical agent implementation of `ACTIVATE_HAWQ_STANDBY` executes `hawq activate standby -a -M fast -v --ignore-bad-hosts`. The topology after this command succeeds but a later Ambari stop/install/delete operation fails requires dedicated fault-injection validation.

## HAWQ Configuration and Historical Stack Contract

| ID | Evidence/configuration | Historical static contract | React baseline boundary |
| --- | --- | --- | --- |
| HAWQ-CFG-001 | `hawq_standby_address_host` | Add writes the new Standby host; Remove/Activate deletes it | Reloads the latest `hawq-site` before every write to avoid overwriting concurrent config with the old Review snapshot |
| HAWQ-CFG-002 | `hawq_master_address_host` | Activate writes the original Standby host | UI-generated property is read-only; save occurs after the custom command and HAWQ stop |
| HAWQ-CFG-003 | `hawq_master_directory` | Add only reads it for manual cleanup confirmation and does not modify it | The UI does not validate the remote directory; the manual responsibility boundary must remain explicit |
| HAWQ-CFG-004 | Component cardinality | `HAWQMASTER=1`, `HAWQSTANDBY=0-1` | From `d680af8057^`; `STATIC_ONLY` on the current branch |
| HAWQ-CFG-005 | Service/component dependency | HAWQ required service is HDFS; Master also has a cluster-scope `HDFS/NAMENODE` dependency with `auto-deploy=false`; Master/Standby both have host-scope `HDFS/HDFS_CLIENT` dependencies with `auto-deploy=true` | From pre-removal metainfo; actual dependency completion and stop/start orchestration require `NEEDS_RUNTIME_VALIDATION` |
| HAWQ-CFG-006 | Kerberos identity | Master/Standby share `hawq_identity`: principal `postgres@${realm}`, keytab `${keytab_dir}/hawq.service.keytab`, owner `gpadmin`, and group `${cluster-env/user_group}` | From the pre-removal Kerberos descriptor; static evidence cannot prove that the current Server can still create the identity; `NEEDS_RUNTIME_VALIDATION` |
| HAWQ-CFG-007 | Custom-command timeout | Historical commandScript timeout for both `REMOVE_HAWQ_STANDBY` and `ACTIVATE_HAWQ_STANDBY` is 1200 seconds | The UI does not set the timeout; a compatible stack must expose it and slow-command/timeout scenarios require `NEEDS_RUNTIME_VALIDATION` |

## Kerberos Conditional Branches

| ID | Scenario | Legacy behavior | React parity requirement |
| --- | --- | --- | --- |
| FHF-KRB-001 | All component install commands | `createInstallComponentTask()` calls `getKDCSessionState` before create/register/install; cancelling the KDC popup marks the current progress task `FAILED` | Do not create the component before checking credentials; failure must remain on the current command and support Retry |
| FHF-KRB-002 | HAWQ Add Step 3 | Explicitly checks the KDC session after the user confirms the data directory and before entering Step 4 | Automatic Kerberos requires a valid admin credential; Manual Kerberos passes the shared session check directly |
| FHF-KRB-003 | HAWQ Remove/Activate Step 2 | Submit checks the KDC session first, then displays irreversible confirmation before entering progress | Preserve the order of both gates |
| FHF-KRB-004 | Federation/RBF | No dedicated descriptor editor, identity review, or Manual Kerberos CSV page; new NameNode/ZKFC/Router identities on a secure cluster depend on standard Ambari Server component installation behavior | `NEEDS_RUNTIME_VALIDATION`: verify principal/keytab creation, missing credentials, Retry, and cleanup on automatic and Manual Kerberos clusters |
| FHF-KRB-005 | RBF stack descriptor | The current BIGTOP descriptor statically defines Router principal `router/_HOST@${realm}` and keytab `${keytab_dir}/dr.service.keytab` | This is current static evidence and does not prove that installation requests fully materialize the identity |
| FHF-KRB-006 | HAWQ descriptor | Can use only the `hawq_identity` evidence from `d680af8057^` | `CONDITIONAL / STATIC_ONLY`; must be verified on a runnable historical stack |
| FHF-KRB-007 | Shared KDC-type branch | For a secure cluster where the controller has no `kdc_type`, first reads `kerberos-env/kdc_type` from the current Kerberos service config; `none` is treated as Manual Kerberos and calls back directly, while other types read session state | React must not incorrectly require a KDC admin credential for Manual Kerberos; only automatic-KDC session-state failure enters the invalid KDC popup |
| FHF-KRB-008 | Invalid KDC session | The popup accepts principal/password and `temporary` or `persisted`; first performs a GET for alias `kdc.admin.credential`, sends a PUT when present or a POST when absent, then uses bare `$.ajax` to replay the saved original request options | The replay may be the session-state GET or the mutation that triggered the KDC error; cancelling the popup during component install marks the current task FAILED, while cancelling a HAWQ progress precondition remains on Review |
| FHF-KRB-009 | Credential-save failure | The create/update branches of `createOrUpdateCredentials()` resolve regardless of actual success/failure and then replay the original request | React must stop and display an error when credential persistence fails; a later replay must not mask the save failure |

## Shared Progress, Failure, Retry, and Logs

| ID | Behavior | Exact semantics |
| --- | --- | --- |
| FHF-PROG-001 | Strict serial execution | `commands` initializes a task array; only a `COMPLETED` current task changes the next `PENDING` task to `QUEUED` and calls its method; execution does not continue after a prior failure |
| FHF-PROG-002 | Request polling | Saves `Requests.id` returned by a mutation to the current task; performs a GET through `background_operations.get_by_request` every 4 seconds until no task is `PENDING/QUEUED/IN_PROGRESS` |
| FHF-PROG-003 | Terminal-state aggregation | If any server task is `FAILED`, `TIMEDOUT`, or `ABORTED`, the current wizard task is `FAILED`; otherwise it is `COMPLETED` |
| FHF-PROG-004 | Retry | This scope uses command-level Retry: resets and reruns only the first failed command as `PENDING`; it does not automatically undo earlier completed side effects |
| FHF-PROG-005 | Skip | The shared controller has `onTaskErrorWithSkip`, but none of the 36 commands in this scope calls it, so Skip/Ignore and Proceed is unavailable |
| FHF-PROG-006 | Final-task failure | `WizardEnableDone` enables Complete when the final command is `FAILED`; the user can finish with a final-stage failure. Any earlier command failure still blocks completion because later tasks remain `PENDING` |
| FHF-PROG-007 | Host/task logs | Clicking an `IN_PROGRESS/FAILED/COMPLETED` command with a request ID opens the host progress popup; commands in this scope have `stageId=null`, so the popup continues to aggregate hosts with `background_operations.get_by_request`; only a non-null `stageId` switches the shared popup to `common.request.polling`; after a task is expanded, `background_operations.get_by_task` polls `stdout`, `stderr`, `output_log`, and `error_log` |
| FHF-PROG-008 | Install chain and partial idempotency | First checks whether target hosts already have the component; when missing, directly performs a GET for component state and refreshes the local service model, sends a POST for the service-component when needed, sends a POST for the host-component, and finally sends a PUT for `INSTALLED`; the existence GET has no failure handler, so failure leaves the task without a terminal state. The legacy helper also swallows service-component creation failure and treats the host-component registration error callback as success; this is not reliable idempotency |
| FHF-PROG-009 | Delete idempotency protection | Treats a DELETE host-component `NoSuchResourceException` as `COMPLETED` and continues; other DELETE errors are FAILED |
| FHF-PROG-010 | Service/component mutation | Stop/start/service restart and component start usually return background requests; advances only after all request tasks reach terminal states; `common.host_components.update` excludes host-components in maintenance from its query |

## Exit, Persistence, and Failure Recovery

| ID | Scenario | Legacy behavior and limitations |
| --- | --- | --- |
| FHF-REC-001 | Step snapshot | Saves current step, host mapping, and configs on every step change; progress additionally saves each task status/request IDs and current request IDs to the local DB and writes cluster status through `/persist`; without `CLUSTER.MANAGE_USER_PERSISTED_DATA`, the POST promise rejects directly |
| FHF-REC-002 | Progress refresh | Recovery continues polling for `IN_PROGRESS` and invokes the current command again for `QUEUED`; all five controllers are registered in `controller_route.js`, so the wizard user can be returned to the wizard by `wizardControllerName` |
| FHF-REC-003 | NameNode/RBF exit | Every-step exit requires confirmation; Step 4 uses a stronger "changes have started" warning. Confirmation clears local state, sets status=`DEFAULT`, returns to Services/reloads, but does not undo server-side effects |
| FHF-REC-004 | HAWQ exit | Add Steps 1-3 and Remove/Activate Steps 1-2 clear state and reload without extra confirmation; only progress Step 4/3 confirms. Cleanup likewise does not roll back completed requests |
| FHF-REC-005 | Completion | The route permits ordinary exit only after `isFinished=true`; Complete clears the namespace, updates models/status, and returns to the Service page |
| FHF-REC-006 | Wizard ownership | NameNode/RBF/Add/Activate call `wizardWatcherController.setUser` on enter; Remove does not. HAWQ finish/close has no explicit `resetUser`, so Add/Activate may retain the owner and Remove may lack one; `NEEDS_RUNTIME_VALIDATION`: cross-user, refresh, post-exit new wizard, and stale `/persist` scenarios |
| FHF-REC-007 | Federation cluster state | NameNode/RBF progress inherits `HIGH_AVAILABILITY_DEPLOY`, but routes recognize only `NN_FEDERATION_DEPLOY`/`RBF_FEDERATION_DEPLOY`; no dedicated state write point was found in source. Exact refresh recovery is unreliable and is recorded as a legacy defect, not expected behavior |
| FHF-REC-008 | HAWQ cluster state | Progress writes `ADD_HAWQ_STANDBY`, `REMOVE_HAWQ_STANDBY`, and `ACTIVATE_HAWQ_STANDBY`, matching route recovery; the loader accepts server `response.clusterState` directly and does not consume the declared-but-unused `cluster_states.validStates`, so source supports state-based recovery. It remains affected by inconsistent ownership and the Remove initialization race |
| FHF-REC-009 | Persistence permission | Cluster status and wizard owner both use `App.Persist.postUserPref()`, with static gate `CLUSTER.MANAGE_USER_PERSISTED_DATA`; none of the five routes checks it explicitly. Without permission, the Deferred rejects immediately, while progress queues the first command only in the status POST success callback, so all five flows definitely fail to start; Retry also does not advance and no network error popup is triggered |

## API Contract Table

The default REST prefix is `/api/v1`. The table lists interfaces actually used directly or through shared mixins by the five flows; one named request can serve multiple commands depending on query/payload. Dedicated controllers do not use raw HTTP, but the shared component-install helper uses one non-Metrics `App.HttpClient` direct GET, included as `FHF-API-027`. The helper then also waits for a global Metrics refresh; because it does not determine wizard input or results, it is explicitly excluded from scope and is not a product interface React must reproduce.

| ID | App.ajax name / Method | URL | Key query or payload | Usage |
| --- | --- | --- | --- | --- |
| FHF-API-001 | `hosts.high_availability.wizard` GET | `/clusters/{clusterName}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true` | No body; one fixed request per modal, plus one in NNF/RBF/Add Step 2 | Initial one-way call counts: NNF 2, RBF 2, Add 2, Remove 1, Activate 1; modal-request failure still marks the view loaded, while the second Step 2 request failure prevents assignment initialization; re-entry/Back can increase the count |
| FHF-API-002 | `wizard.loadrecommendations` POST | `{stackVersionUrl}/recommendations` | `hosts=<全部 registered fqdn>`, `services=<全部 selected/installed service>`, `recommend:"host_groups"`; this scope actually requires the complete installed topology `recommendations:{blueprint,blueprint_cluster_binding}` | Initial NNF/RBF recommendations; HAWQ Add initial recommendation, and called again before validation on assignment changes and Submit |
| FHF-API-003 | `config.validations` POST | `{stackVersionUrl}/validations` | Same hosts/services; `validate:"host_groups"`; `recommendations` is the complete object from the immediately preceding recommendation response | HAWQ Add only; called after successful recommendation on assignment changes and Submit |
| FHF-API-004 | `config.tags` GET | `/clusters/{clusterName}?fields=Clusters/desired_configs` | No body | Reads current tags before Step 3/reconfiguration; NNF/RBF Review failure usually remains loading, HAWQ Add failure keeps `isLoaded=false`, and HAWQ progress failure marks the current task FAILED |
| FHF-API-005 | `admin.get.all_configurations` GET | `/clusters/{clusterName}/configurations?{urlParams}` | Batch reads Federation/RBF with `(type=X&tag=Y)\|...` | Federation/RBF Review; no dedicated error callback, so failure usually remains loading with Next disabled |
| FHF-API-006 | `reassign.load_configs` GET | `/clusters/{clusterName}/configurations?{urlParams}` | HAWQ `(type=hawq-site&tag=<tag>)` | HAWQ Add Review error is routed to the success callback; the three progress reconfigure flows use `onTaskError` and correctly fail the current task |
| FHF-API-007 | `common.service.configurations` PUT | `/clusters/{clusterName}` | Formatter wraps `{Clusters:{desired_config:<value>}}` unchanged; RBF value is one `{type:"hdfs-rbf-site",properties,properties_attributes?}`, while HAWQ value is the single-element array generated by `reconfigureSites()` | RBF observer sends early and the UI does not wait for the result; HAWQ saves one type; preserve tests for server compatibility with object/array forms |
| FHF-API-008 | `common.service.multiConfigurations` PUT | `/clusters/{clusterName}` | `[{Clusters:{desired_config:...}}, ...]` | Saves HDFS/conditional Ranger/Accumulo for NameNode Federation; only referenced by unreachable RBF methods |
| FHF-API-009 | `common.services.update` PUT | `/clusters/{clusterName}/services?{urlParams}` | `RequestInfo.context/operation_level=CLUSTER`; `Body.ServiceInfo.state=INSTALLED\|STARTED`; query `ServiceInfo/service_name.in(...)` | Stops/starts required services |
| FHF-API-010 | `host_component.installed.on_hosts` GET | `/clusters/{clusterName}/host_components` | Component name and host `.in(...)`; returns existing host-components | Idempotency check before every component install; caller registers only `.done()`, so GET failure shows only the default error popup and the install task may never reach a terminal state |
| FHF-API-011 | `common.create_component` POST | `/clusters/{clusterName}/services?ServiceInfo/service_name={serviceName}` | `{components:[{ServiceComponentInfo:{component_name}}]}` | Creates a missing service-component; caller resolves through `.always()` and continues host-component registration after actual creation failure |
| FHF-API-012 | `wizard.step8.register_host_to_component` POST | `/clusters/{cluster}/hosts` | `RequestInfo.query=Hosts/host_name=...\|...`; `Body.host_components[].HostRoles.component_name` | Registers the component on target hosts; both success/error bind to `onCreateComponent`, and both paths continue to PUT `INSTALLED` |
| FHF-API-013 | `common.host_components.update` PUT | `/clusters/{clusterName}/host_components?{urlParams}` | This scope usually does not pass `urlParams`; component/host/`maintenance_state=OFF` filters are in JSON `RequestInfo.query`, with `Body.HostRoles.state=INSTALLED\|STARTED` | Installs or starts a component |
| FHF-API-014 | `common.delete.host_component` DELETE | `/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}` | Path parameters | Deletes topology components in HAWQ Remove/Activate |
| FHF-API-015 | `service.item.executeCustomCommand` POST | `/clusters/{clusterName}/requests` | `RequestInfo.command/context`; filter `{service_name,component_name,hosts}` | HAWQ Remove/Activate custom command |
| FHF-API-016 | `nameNode.federation.formatNameNode` POST | `/clusters/{clusterName}/requests` | Command `FORMAT`; filter HDFS/NAMENODE/first new host | NNF command 11 |
| FHF-API-017 | `nameNode.federation.formatZKFC` POST | `/clusters/{clusterName}/requests` | Command `FORMAT`; filter HDFS/ZKFC/first new host | NNF command 12 |
| FHF-API-018 | `nameNode.federation.bootstrapNameNode` POST | `/clusters/{clusterName}/requests` | Command `BOOTSTRAP_STANDBY`; filter HDFS/NAMENODE/second new host | NNF command 15 |
| FHF-API-019 | `restart.custom.filter` POST | `/clusters/{clusterName}/requests` | Command `RESTART`; `hosts_predicate` excludes NN/JN/ZKFC/Ranger components and constrains `HostRoles/cluster_name`; **no** `stale_configs=true` condition | NNF command 18; actually requests restart for all non-excluded host-components, not only stale components |
| FHF-API-020 | `background_operations.get_by_request` GET | `/clusters/{clusterName}/requests/{requestId}` | Fields include request and task status/command/host; `minimal_response=true` | Progress polling every 4 seconds |
| FHF-API-021 | `common.request.polling` GET | `/clusters/{clusterName}/requests/{requestId}?fields=...&tasks/Tasks/stage_id={stageId}` | Request/task IDs, command/detail, timing, status, host, and structured output | `CONDITIONAL` shared popup branch; used only with a non-null `stageId`, while all normal 36-command tasks in this scope have null stageId |
| FHF-API-022 | `background_operations.get_by_task` GET | `/clusters/{clusterName}/requests/{requestId}/tasks/{taskId}` | Path parameters; returns the complete task, including stdout/stderr/output/error logs | Single-task details and log polling |
| FHF-API-023 | `admin.security.cluster_configs.kerberos` GET | `/clusters/{clusterName}/configurations/service_config_versions?service_name=KERBEROS&is_current=true` | Current Kerberos config version | Shared controller for automatic/Manual KDC session branches |
| FHF-API-024 | `kerberos.session.state` GET | `/clusters/{clusterName}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details` | No body | Precheck for component install and HAWQ progress |
| FHF-API-025 | `persist.get` GET | `/persist/{key}` | No query/body on the wire; key is `CLUSTER_CURRENT_STATUS` or `wizard-data`. Cluster-loader user/login/auth/override values remain client callback parameters and are not sent to the Server | Conditional state and wizard-owner loading by app bootstrap/updater, not an explicit GET per step |
| FHF-API-026 | `persist.post` POST | `/persist` | Cluster status is double-stringified as `{"CLUSTER_CURRENT_STATUS":"\"<LZString-base64>\""}`; owner is `{"wizard-data":"{\"userName\":...,\"controllerName\":...}"}`, and reset owner value is the string `"null"` | Step/task state, owner, completion/exit cleanup; gated by persisted-data permission |
| FHF-API-027 | Direct `App.HttpClient.get` GET | `/clusters/{clusterName}/components/?fields=ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true` | No body; updates `componentsStateMapper` | Refreshes component state before checking/creating a service-component when component-install targets have missing hosts |
| FHF-API-028 | `credentials.get` GET | `/clusters/{clusterName}/credentials/kdc.admin.credential` | Alias path parameter | Probes the alias before saving new credentials from the invalid KDC popup |
| FHF-API-029 | `credentials.create` POST | `/clusters/{clusterName}/credentials/kdc.admin.credential` | `{Credential:{principal,key,type:<"temporary" 或 "persisted">}}` | Creates the KDC admin credential when the alias is absent, then replays the original AJAX options after settlement |
| FHF-API-030 | `credentials.update` PUT | `/clusters/{clusterName}/credentials/kdc.admin.credential` | Same Credential body, with either type | Updates an existing alias; after settlement, likewise replays the original request, which may be the session-state GET or the original mutation |
| FHF-API-031 | `config.on_site` GET | `/clusters/{clusterName}/configurations?{params}` | In this scenario `params=(type=hdfs-site&tag=<currentTag>)`; when the local tag table is empty, the chain first uses `config.tags` | `CONDITIONAL / shared cluster bootstrap`; loads the namespace model when HDFS is loaded, HA is enabled, and current hdfs-site cache/tag is missing; failure is collapsed to empty config by `.always()`, so the outer layer may resolve without setting the namespace-ready flag and NNF/RBF Review may wait forever |

## Confirmed Legacy Defects and React Handling Principles

| ID | Legacy defect | Static evidence and impact | React handling principle |
| --- | --- | --- | --- |
| FHF-RISK-001 | All five routes lack permission and resource guards | Checks occur only in menu construction/click handlers; direct URLs can open the wizard | Do not reproduce; revalidate permission, stack capability, and component/service state in the route loader and before mutations |
| FHF-RISK-002 | RBF Review saves automatically and prematurely | The `onLoad` observer sends a PUT for `hdfs-rbf-site` before the user clicks Next | Do not reproduce; React should define the submission point, failure feedback, and exit semantics and record the behavior difference as a migration decision |
| FHF-RISK-003 | RBF request sender is the wrong global object | `sender:self` in an ordinary browser script usually resolves to global `window/self`, not the controller; the request may send, but failure makes the AJAX wrapper call global `onTaskError` and throw again, while success has no callback and the UI has already allowed continuation | Replace with an explicit handler/context; React must enable Next only after PUT success and cover slow requests, HTTP failure, and exit races |
| FHF-RISK-004 | RBF assumes the config type exists | It executes `configToSave.properties[name]=...` after `hdfsrbfConfigs&&hdfsrbfConfigs.properties` | Block with an actionable error or explicitly initialize the missing type; do not throw a JS exception |
| FHF-RISK-005 | RBF progress has unreachable reconfiguration code | `commands` contains only install/start; `reconfigureServices/installHDFSClients` are never called | Use the reachable state machine as the legacy baseline; if React adds them, record it as an intentional fix rather than parity coverage |
| FHF-RISK-006 | Ranger defaultFS mapping is appended repeatedly | Federation appends the same cluster-level property inside each nameservice loop | React should deduplicate and verify with a final desired-config fixture |
| FHF-RISK-007 | Federation persistence state names do not match | Progress writes `HIGH_AVAILABILITY_DEPLOY`, while routes read `NN_FEDERATION_DEPLOY`/`RBF_FEDERATION_DEPLOY` | Define one explicit state enum and test refresh recovery for every command |
| FHF-RISK-009 | Shared Rollback is wired to the wrong wizard | `supports.autoRollbackHA` displays Rollback for a failed task, but `HighAvailabilityProgressPageController.rollback()` hard-codes `highAvailabilityWizardController` and the NameNode HA rollback route | Do not claim Rollback support in this scope; React should display it only when a corresponding reverse-operation state machine exists |
| FHF-RISK-010 | Exit only forgets state and does not undo side effects | Progress exit clears DB/status/reloads, while completed stop/config/install/delete/custom-command effects remain | UI must state clearly that exit does not roll back; Resume may be offered, but clearing state must not be described as a Cancel transaction |
| FHF-RISK-011 | HAWQ wizard ownership is inconsistent | Add/Activate set the user without an explicit reset; Remove does not set the user | Standardize ownership acquire/release during refactor; specific legacy cross-user behavior is `NEEDS_RUNTIME_VALIDATION` |
| FHF-RISK-012 | Completion is allowed after final-item failure | `WizardEnableDone` allows Done when the final command is FAILED | If preserving "complete with failure", provide an explicit warning and link to background tasks; otherwise block it and record the intentional difference |
| FHF-RISK-013 | HAWQ no longer has a current Server stack | UI routes remain, but server definitions for components/config/custom commands were deleted | Hide/disable by default; enable only when a compatible stack capability is detected, not based on legacy Ember files |
| FHF-RISK-014 | Component install swallows creation errors | Service-component POST continues through `.always()`; host-component POST `error` also binds to `onCreateComponent`, so any registration failure still attempts PUT `INSTALLED`; dedicated `onCreateComponentError` is not connected to the call | React must distinguish already-exists from real failure; real failure stops the command, and Retry rereads server state first |
| FHF-RISK-015 | HAWQ Add config-load error handling is inconsistent | Step 3 `config.tags` failure remains unloaded with no Retry; later `reassign.load_configs` points both success/error to `loadConfigsSuccessCallback`, which may treat jqXHR as `hawqProps` and show loaded, then Submit accesses missing `items[0]` | Do not reproduce; both read phases must enter an explicit error state, block Submit, and provide Retry |
| FHF-RISK-016 | HAWQ Activate leaks a global variable | Step 2 assigns undeclared `newHawqMaster = ...` | React should use local immutable state and add a unit test for Review-host generation |
| FHF-RISK-017 | HAWQ Remove does not wait for initialization | After calling `dataLoading().done(set current HAWQ service)`, the route immediately uses `Em.run.next()` to create the modal, read cluster status, and transition; unlike the other four flows, it does not wait for the promise | React route loader must finish cluster/service/status loading before rendering; validate the legacy race with cold starts and slow requests as `NEEDS_RUNTIME_VALIDATION` |
| FHF-RISK-018 | HAWQ Remove/Activate Step 1 markup is invalid | Both templates write the footer opening tag as `</div class="wizard-footer col-md-12">`, creating an extra closing tag with no actual footer wrapper | React must not reproduce broken DOM; add structure, keyboard-navigation, and button-visibility tests |
| FHF-RISK-019 | Component-existence GET failure stalls the task | `createComponent()` attaches only `.done()` to `checkInstalledComponents()`; after failure, it shows the default error popup but does not call `onTaskError`, so the task remains in its pre-run state and Retry does not appear | React must include the preflight GET in the command state machine; failure must terminate the current attempt, support Retry, and reread server topology before retrying |
| FHF-RISK-020 | Federation config-read failure leaves a permanent gate | NNF/RBF Review `config.tags` and configuration GET requests have no failure handler; shared loader continues the tag GET through `.always()`, and `config.on_site` collapses failure to an empty array through `.always()`, after which `updateHDFSNameSpaces()` does not set `isHDFSNameSpacesLoaded=true` | React config loaders must have success/error terminal states, timeout, and Retry; namespace-resolution failure must not appear as an infinite spinner |

## Test Coverage and Runtime Scenarios

| ID | Static test finding | Covered | Explicit gap |
| --- | --- | --- | --- |
| FHF-TEST-001 | NameNode Federation tests | Step 1 ID validation, Step 3 config generation, Step 4 controller methods/AJAX parameters, wizard local DB load/save | Route/permission/entry state, Step 2 assignment integration, real 18-command execution, complete Ranger/Accumulo desired config, Kerberos, and exit/recovery E2E |
| FHF-TEST-002 | Router Federation tests | No dedicated test found | Entire flow is uncovered, especially premature save, incorrect global `self` sender, missing `hdfs-rbf-site`, multiple-Router cardinality, Kerberos identity, and refresh recovery |
| FHF-TEST-003 | HAWQ Add tests | Step 3 dynamic config/confirmation logic, Step 4 controller/AJAX, wizard load/save | Route/permission, real Advisor ERROR/WARN, directory cleanup, KDC, agent command, and service failure/recovery E2E |
| FHF-TEST-004 | HAWQ Remove tests | Step 2 submit, Step 3 custom command/config/delete, wizard load/save | Route/permission, irreversible-confirmation order, real agent, partial failure, and Retry |
| FHF-TEST-005 | HAWQ Activate tests | Step 2 property, Step 3 custom command/config/install/delete, wizard controller | Route/permission, real process/port behavior, new Master identity installation, deletion-order failure, and cross-user recovery |
| FHF-TEST-006 | Exclusion check | `hawqsegment_live_test.js` and any dashboard/service/host Metrics test are outside this module | React comparison must not count metric-display gaps in this document's coverage |

React acceptance must cover at least the following runtime matrix; static source cannot replace it:

| ID | Scenario | Assertion |
| --- | --- | --- |
| FHF-RUNTIME-001 | First expand NameNode HA to Federation; then add a third namespace | firstRun scoped properties, NN IDs, JN URI, service RPC conditions, and old generic-property cleanup are correct |
| FHF-RUNTIME-002 | Federation with and without Ranger, Accumulo, and Infra | 18-task dynamic removal, desired config, stop list, and restart predicate without a stale gate are correct, and Ranger mappings are not duplicated |
| FHF-RUNTIME-003 | RBF with one/multiple Routers, missing `hdfs-rbf-site`, and Step 3 exit/request failure | Submission boundary is explicit with no JS exception; Router install/start and config remain consistent |
| FHF-RUNTIME-004 | Automatic Kerberos, Manual Kerberos, and KDC session expiry/cancellation | Principals/keytabs for new NN/ZKFC/Router/HAWQ components, failed tasks, and Retry are equivalent |
| FHF-RUNTIME-005 | Inject FAILED/TIMEDOUT/ABORTED into every command, then refresh and Retry | Completed side effects are not destructively repeated; current request polling recovers; no false Rollback/Skip |
| FHF-RUNTIME-006 | Exit separately from Step 1/Review/progress; re-enter as the original/another user | Warnings, owner, persistence, recovery point, and server-side-effect statements are accurate |
| FHF-RUNTIME-007 | Complete Add/Remove/Activate flows and interruption at every phase on a compatible historical HAWQ stack | Final cardinality, custom commands, config, component relationships, shared identity, and service state are consistent |

## Primary Source Evidence

- Federation routes: `ambari-web/classic/app/routes/namenode_federation_routes.js`, `ambari-web/classic/app/routes/dfsrouter_federation_routes.js`
- Federation controllers: `ambari-web/classic/app/controllers/main/admin/federation/`
- HAWQ routes: `ambari-web/classic/app/routes/add_hawq_standby_routes.js`, `remove_hawq_standby_routes.js`, `activate_hawq_standby_routes.js`
- HAWQ controllers: `ambari-web/classic/app/controllers/main/admin/highAvailability/hawq/`
- Service Actions: `ambari-web/classic/app/views/main/service/item.js`, `ambari-web/classic/app/models/host_component.js`
- Shared assignment/progress: `ambari-web/classic/app/mixins/wizard/assign_master_components.js`, `ambari-web/classic/app/mixins/wizard/wizardProgressPageController.js`, `ambari-web/classic/app/controllers/main/admin/highAvailability/progress_controller.js`
- Request registry: `ambari-web/classic/app/utils/ajax/ajax.js`
- Tests: `ambari-web/classic/test/controllers/main/admin/federation/`, `ambari-web/classic/test/controllers/main/admin/highAvailability/hawq/`
- Historical HAWQ stack/agent evidence: `git show d680af8057^:ambari-server/src/main/resources/common-services/HAWQ/2.0.0/...`

See the heuristic module index at [generated/api-by-module/security-ha-federation.md](generated/api-by-module/security-ha-federation.md): it uses broad request-name and caller-path matching, may include cross-module requests, and may omit requests called indirectly by shared controllers/mixins, so it is not a complete interface inventory. Cross-module component install, KDC session, progress log, and persistence requests remain in this baseline; authoritative network verification must combine [generated/ajax-endpoints.md](generated/ajax-endpoints.md), [generated/ajax-calls.md](generated/ajax-calls.md), [generated/direct-http-calls.md](generated/direct-http-calls.md), [generated/browser-network-entrypoints.md](generated/browser-network-entrypoints.md), and [generated/realtime-channels.md](generated/realtime-channels.md).
