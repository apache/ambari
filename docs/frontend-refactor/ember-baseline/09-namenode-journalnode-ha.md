# NameNode HA and JournalNode Management Baseline

This document is the legacy Ember behavior baseline for Enable NameNode HA and Manage JournalNodes in the React refactor. It covers entry points, permissions, the nine-step/seven-step state machines, manual commands, Kerberos branches, configuration migration, background requests, errors/Retry, exit/recovery, and rollback. See adjacent module documents for NameNode Federation/RBF and ResourceManager/Ranger/HAWQ HA.

## Scope, Evidence, and Metrics Boundary

| ID | Baseline fact | React acceptance requirement | Primary evidence |
| --- | --- | --- | --- |
| NNHA-SCOPE-001 | The main route is `/highAvailability/NameNode/enable`, with fixed internal Steps 1 through 9; the modal route is named `main.services.enableHighAvailability` | URL, modal, step navigation, and recovery must not be reduced to one ordinary configuration submission | `ambari-web/classic/app/routes/high_availability_routes.js`, `nameNode/wizard_controller.js` |
| NNHA-SCOPE-002 | The JN management route is `/highAvailability/JournalNode/manage`, with fixed internal Steps 1 through 7; pure deletion hides Steps 3/5, so users see five renumbered steps | React must cover add, delete, mixed add/delete, and pure delete; it must not implement only Add JournalNode | `ambari-web/classic/app/routes/manage_journalnode_routes.js`, `journalNode/wizard.hbs` |
| NNHA-SCOPE-003 | This module excludes Metrics product pages, charts, Widgets, and the global metrics refresh from installation helpers; it retains `metrics/dfs/...` in checkpoint/JN-formatted safety gates because those fields directly determine Next | Do not remove the three checkpoint GET requests because their field names contain `metrics`, and do not treat unrelated metric refresh as a product contract | `nameNode/step4_controller.js`, `nameNode/step6_controller.js`, `journalNode/step3_controller.js` |
| NNHA-SCOPE-004 | Only HA configuration migration is retained for AMS: when `ams-hbase-site/hbase.rootdir` contains the current NN host, it changes to the nameservice; when AMS is installed, Step 9 resubmits the complete `ams-hbase-site` snapshot regardless of whether the value matches. All other AMS/Metrics functionality is `OUT_OF_SCOPE` | Implement/verify only this configuration side effect and config-version submission; do not migrate AMS pages or metric interfaces | `app/utils/configs/nn_ha_config_initializer.js#_initHbaseRootDirForAMS`, `nameNode/step9_controller.js#reconfigureAMS` |
| NNHA-SCOPE-005 | Authoritative evidence is route/controller/view/template, the five-layer network inventory, shared progress/install mixins, and legacy Karma tests; conflicts between static code and actual reachability are marked `STATIC_ONLY`, `PLACEHOLDER`, or `NEEDS_RUNTIME_VALIDATION` | React gap review must compare each ID and must not infer availability from a same-name controller or a hit/miss in the heuristic module inventory | `generated/ajax-endpoints.md`, `generated/ajax-calls.md`, `generated/direct-http-calls.md`, `generated/browser-network-entrypoints.md`, `generated/realtime-channels.md`, this document's test section |

## Enable NameNode HA Entry and Preconditions

| ID | Entry/behavior | Permissions, conditions, and boundaries | Request/result | Primary evidence |
| --- | --- | --- | --- | --- |
| NNHA-ENTRY-001 | HDFS Service Actions displays Enable NameNode HA when HA is disabled, then enters the nine-step modal through the shared HA controller | HDFS stack service types include `HA_MODE` and have master/slave components; the item is visible under any outer service-action permission but specifically requires `SERVICE.ENABLE_HA`. It is disabled for a single-node cluster, no NN, or an uninstalled NN | After preconditions pass, transitions to `main.services.enableHighAvailability` | `app/views/main/service/item.js#observeMaintenanceOnce`, `app/models/host_component.js#TOGGLE_NN_HA`, `app/templates/main/service/item.hbs` |
| NNHA-ENTRY-002 | After HA is enabled, `TOGGLE_NN_HA` computes the action/label as Disable, but `isHidden=App.isHaEnabled` | The legacy UI has no usable Disable NameNode HA menu; dead code must not be treated as a supported entry point | None | `app/models/host_component.js#TOGGLE_NN_HA` |
| NNHA-ENTRY-003 | The Secondary NameNode custom check in Stack upgrade can display an Enable button and reuse the same precondition checks | This is a second entry point from upgrade checks; it ultimately enters the same wizard | Same as the main entry | `app/views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_checks_snn.js` |
| NNHA-ENTRY-004 | Before entry, checks `workStatus === STARTED` on the first NameNode found | When an NN exists but is not started, adds the reason to the same error popup and blocks routing. With no NN, evaluates `undefined.get(...)` and throws instead of producing an aggregate error. The normal menu is disabled when NN is missing, but direct controller invocation does not fail gracefully | None | `app/controllers/main/admin/highAvailability_controller.js#enableHighAvailability` |
| NNHA-ENTRY-005 | Requires at least three ZooKeeper Server components before entry | Checks only model count and not ZooKeeper `workStatus`; this is the precise legacy behavior and must not be described as a runtime-state check | None | Same as above |
| NNHA-ENTRY-006 | Requires at least three registered hosts before entry | Blocks when `App.allHostNames.length < 3` | None | Same as above |
| NNHA-ENTRY-007 | Blocks when any master component has explicit maintenance `passiveState=ON` or implied maintenance | Applies to all HDFS masters; combines all precondition errors into a `<br/>` popup | None | Same as above |
| NNHA-ENTRY-008 | Direct route access does not rerun permissions or the business preconditions above | A menu gate cannot replace server authorization; React route guards should validate explicitly, as the legacy behavior is a security gap | Route directly loads wizard data | `app/routes/high_availability_routes.js#enter` |

## Enable NameNode HA Nine-Step State Machine

### Step 1 Get Started

| ID | User behavior and state | Validation/exceptions | Requests/persistence |
| --- | --- | --- | --- |
| NNHA-STEP1-001 | Reads the maintenance-window and automatic/manual-step instructions and enters the Nameservice ID | 1 to 63 characters; letters, digits, and hyphens only, with no leading/trailing hyphen; invalid values disable Next | `config.on_site` reads `hadoop-env/hdfs_user`; Next saves `nameServiceId` and clears the old master assignment |
| NNHA-STEP1-002 | Strongly warns that the wizard should be exited and HBase stopped if HBase is running | Informational only; does not read HBase runtime state or force-block Next | None |
| NNHA-STEP1-003 | When HAWQ is installed, additionally warns that HAWQ filespace must be updated manually after HA | Shown only when the HAWQ service exists; does not validate the manual result before or after the wizard | None |

### Step 2 Select Hosts

| ID | User behavior and state | Validation/branches | Requests/persistence |
| --- | --- | --- | --- |
| NNHA-STEP2-001 | Assigns one Additional NameNode and at least three JournalNodes; marks the existing NameNode as Current | Initial `mastersToAdd` contains one NN and three JNs; Remove appears above three JNs, and Add appears on the last item below stack/host limits | Host inventory + Stack Advisor `recommend: host_groups`; Next saves master topology |
| NNHA-STEP2-002 | Hosts for multiple masters of the same type must be unique and target hosts must exist | Empty hosts and duplicate hosts for the same component are invalid; a host with an installed component can remain only with maintenance `OFF` | Client-side assignment validation |
| NNHA-STEP2-003 | Maximum JNs are jointly limited by stack component cardinality and available hosts and may exceed 3 | Do not hard-code the wizard to exactly three; only the minimum of 3 is hard-coded | Stack metadata, assignment mixin |
| NNHA-STEP2-004 | Back returns to Step 1; Next records Additional NN and SNN hosts for rollback data | Changing topology clears prior Review configs to prevent reuse of an old config snapshot | Local DB `masterComponentHosts`, rollback host keys |

### Step 3 Review

| ID | User behavior and state | Configuration/exceptions | Requests/persistence |
| --- | --- | --- | --- |
| NNHA-STEP3-001 | Review displays the Current NN, SNN to delete, Additional NN to install, and all JNs to install | Shows a spinner until configs load; Back can reselect hosts | `config.tags` followed by `admin.get.all_configurations` |
| NNHA-STEP3-002 | Allows editing only `hdfs-site/dfs.journalnode.edits.dir`; other HA and dependent-service configs are read-only | All generated configs use `isOverridable=false`; only this property uses `isReconfigurable=true` | Next merges changes into the complete server config snapshot |
| NNHA-STEP3-003 | Loads current tags/configs for HBase, Accumulo, AMS, HAWQ, and Ranger based on installed services | Base tags for HBase, Accumulo, AMS, HAWQ, and `ranger-env` are dereferenced directly: a present service with missing desired config throws. Only some Ranger plugin/audit sites have existence guards. Adding missing-item validation in React is an explicit robustness improvement | Dynamic `urlParams=(type=...&tag=...)\|...` |
| NNHA-STEP3-004 | Next saves `hdfs-site` and `core-site`, and conditionally saves original `hbase-site`/`ranger-env` tags for later submission/static rollback | Request or config-initialization failure remains on Review with a generic error | Local DB config snapshot/tags |

### Step 4 Create Checkpoint

| ID | User behavior and state | Safety gate/exceptions | Requests |
| --- | --- | --- | --- |
| NNHA-STEP4-001 | Logs in to the Current NN and, as the HDFS user, runs `hdfs dfsadmin -safemode enter` followed by `hdfs dfsadmin -saveNamespace` | UI polls every 1 second and cannot replace the manual commands | `admin.high_availability.getNnCheckPointStatus` |
| NNHA-STEP4-002 | Enables Next only when `Safemode` is non-empty and `LastAppliedOrWrittenTxId - MostRecentCheckpointTxId <= 1` | Fields come from `metrics.dfs.namenode` and are required HA safety data; an already recent checkpoint can satisfy the condition early | GET current NN host-component |
| NNHA-STEP4-003 | Displays an error when desired state is not `STARTED` | `isNameNodeStarted` is not included in the Next computation; continuation is still possible when checkpoint conditions pass, an inconsistency in the legacy behavior | Same as above |
| NNHA-STEP4-004 | After enabled Next is clicked, checks the KDC session before entering the automatic-change phase | Manual Kerberos (`kdc_type=none`) passes directly. An invalid automatic KDC or recognizable KDC 400 opens a credential popup; cancellation remains on this step. Ordinary HTTP failure from KDC type/session requests shows only the default error and neither continues nor calls the cancel handler | Kerberos config/session/credential APIs |

### Step 5 Configure Components

| ID | Strict serial task | Exact side effects and exceptions | Primary requests |
| --- | --- | --- | --- |
| NNHA-STEP5-001 | 1 Stop All Services | Sends a PUT to set cluster services to `INSTALLED` and polls the background request; stops all services, not only HDFS | `common.services.update`, request polling |
| NNHA-STEP5-002 | 2 Install Additional NameNode | Checks component existence, creates the service-component and registers the host-component when needed, then sends a PUT for `INSTALLED`; checks the KDC session before each install | Component install chain |
| NNHA-STEP5-003 | 3 Install JournalNodes | Runs the same installation chain for all JN hosts from Step 2, including idempotent checks for existing host-components | Component install chain |
| NNHA-STEP5-004 | 4 Reconfigure HDFS | Saves `hdfs-site` and `core-site`, conditionally saves Ranger HDFS audit/plugin settings, then installs `HDFS_CLIENT` on all NN/JN hosts and saves the host list | `common.service.configurations` + component install chain |
| NNHA-STEP5-005 | Kerberos-cluster reconfiguration branch | Agent installation on JNs may inject security properties, so reloads the latest tags/configs, overlays Review HA properties, removes old NN keys, and submits | Config GET chain + `common.service.configurations` |
| NNHA-STEP5-006 | 5 Start JournalNodes | Sends a PUT for `STARTED` to all selected JN host-components with maintenance `OFF` and polls | `common.host_components.update` |
| NNHA-STEP5-007 | 6 Disable Secondary NameNode | Does not stop/delete it; sends a PUT for the SNN host-component maintenance/passive state to `ON` | `common.host.host_component.passive` |

### Step 6 Initialize JournalNodes

| ID | User behavior and state | Safety gate/exceptions | Requests |
| --- | --- | --- | --- |
| NNHA-STEP6-001 | Logs in to the Current NN and, as the HDFS user, runs `hdfs namenode -initializeSharedEdits` | UI concurrently polls selected JNs every 1 second | One `admin.high_availability.getJnCheckPointStatus` request per host |
| NNHA-STEP6-002 | Parses `metrics.dfs.journalnode.journalsStatus` and requires the current nameservice's `Formatted === "true"` | Displays "all JNs should be started" when metrics are absent; does not validate the manual command exit code | Same as above |
| NNHA-STEP6-003 | The legacy implementation decides after receiving the first three responses and requires a count of 3 | With more than three selected JNs, it may succeed before other responses arrive, and the result depends on response order; React must wait for the complete target set, as this is a legacy defect | `nameNode/step6_controller.js#MINIMAL_JOURNALNODE_COUNT` |

### Step 7 Start Components

| ID | Strict serial task | Conditions and side effects | Primary requests |
| --- | --- | --- | --- |
| NNHA-STEP7-001 | 1 Start ZooKeeper Servers | Sends a PUT for `STARTED` to all ZK Server host-components in the topology | `common.host_components.update` |
| NNHA-STEP7-002 | 2 Conditional Start Ambari Infra | Retains the task only when the `AMBARI_INFRA_SOLR` model is loaded and starts the service | `common.services.update` |
| NNHA-STEP7-003 | 3 Conditional Start MySQL Server | Requires an existing component model with installedCount > 0; updates MYSQL_SERVER hosts using service name `HIVE` | `common.host_components.update` |
| NNHA-STEP7-004 | 4 Conditional Start Ranger | Requires an existing RANGER_ADMIN model with installedCount > 0; uses Ranger hosts from the assignment | `common.host_components.update` |
| NNHA-STEP7-005 | 5 Start Current NameNode | Starts only the originally installed NN to provide an Active NN for the next manual step | `common.host_components.update` |

### Step 8 Initialize Metadata

| ID | User behavior and state | Validation/exceptions | Requests |
| --- | --- | --- | --- |
| NNHA-STEP8-001 | Runs `hdfs zkfc -formatZK` on the Current NN | The page does not poll or validate the command result | No business request |
| NNHA-STEP8-002 | Runs `hdfs namenode -bootstrapStandby` on the Additional NN | The page does not validate host output or metadata completeness | No business request |
| NNHA-STEP8-003 | Next first checks the KDC session and then opens an "manual steps completed" confirmation; confirmation is required to enter Step 9 | Manual Kerberos passes directly. Closing the invalid-KDC popup or cancelling the manual confirmation remains on this step; ordinary HTTP failure from KDC type/session requests displays only the default error and does not enter confirmation | Kerberos session/credential APIs |

### Step 9 Finalize HA Setup

| ID | Strict serial task | Conditions and side effects | Primary requests |
| --- | --- | --- | --- |
| NNHA-STEP9-001 | 1 Start Additional NameNode | Sends a PUT for `STARTED` to the new NN and polls | `common.host_components.update` |
| NNHA-STEP9-002 | 2 Install ZKFC; 3 Start ZKFC | Creates/registers/installs ZKFC on both NN hosts, then sends a PUT for `STARTED`; checks KDC before installation | Component install chain, `common.host_components.update` |
| NNHA-STEP9-003 | 4 Conditional Install PXF | Historical HAWQ/PXF branch: installs only when the PXF service exists and the new NN host has no PXF | Component install chain |
| NNHA-STEP9-004 | 5 Conditional Reconfigure Ranger | Saves `ranger-env` and submits YARN, Storm, Kafka, Knox, Atlas, Hive, and Ranger KMS audit/plugin sites separately based on installed services and actual property existence | `common.service.multiConfigurations` |
| NNHA-STEP9-005 | 6 Conditional Reconfigure HBase | Saves `hbase-site` and conditionally includes HBase audit/plugin sites when Ranger exists | `common.service.configurations` |
| NNHA-STEP9-006 | 7 Conditional Reconfigure AMS | When AMS is installed, submits the complete `ams-hbase-site` snapshot. Changes to nameservice only when `hbase.rootdir` contains the current NN host; otherwise the original value still creates a new config version with the snapshot. Other AMS/Metrics capabilities are excluded | `common.service.configurations` |
| NNHA-STEP9-007 | 8 Conditional Reconfigure Accumulo | Saves the `accumulo-site` volume migration | `common.service.configurations` |
| NNHA-STEP9-008 | 9 Conditional Reconfigure HAWQ | Saves nameservice/NN address migration for `hawq-site` and `hdfs-client` | `common.service.configurations` |
| NNHA-STEP9-009 | 10 Delete Secondary NameNode | Issues a DELETE for the SNN host-component; ordinary errors mark the task FAILED, and the generic NoSuchResource deletion tolerance is not used by this direct call | `common.delete.host_component` |
| NNHA-STEP9-010 | 11 Stop HDFS; 12 Start All Services | First sends a PUT for only HDFS to `INSTALLED`, then sends a PUT for all services to `STARTED`; does not run smoke tests | `common.services.update`, request polling |
| NNHA-STEP9-011 | Complete | Displays a manual filespace-update alert when HAWQ exists; then clears task/storage, sets status=`DEFAULT`, and returns to Services | Persist/status APIs |

## NameNode HA Configuration Contract

| ID | Site/property | Legacy generation or migration rule | Boundary |
| --- | --- | --- | --- |
| NNHA-CONFIG-001 | `hdfs-site/dfs.nameservices`, `dfs.internal.nameservices` | Both write the user-provided Nameservice ID | Initial non-Federation HA with one nameservice |
| NNHA-CONFIG-002 | `dfs.ha.namenodes.<ns>` | Writes `nn1,nn2`; the failover provider is `org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider` | Placeholders in the property name are also replaced with `<ns>` |
| NNHA-CONFIG-003 | `dfs.namenode.rpc-address.<ns>.nn1/nn2` | nn1=Current NN plus the existing non-namespaced RPC port, default 8020; nn2=Additional NN plus 8020 | Host and port sources are asymmetric |
| NNHA-CONFIG-004 | `dfs.namenode.http-address.<ns>.nn1/nn2`, corresponding HTTPS keys | nn1 reuses existing HTTP/HTTPS ports, defaulting to 50070/50470; nn2 uses fixed defaults 50070/50470 | Preserve stack/version port-difference validation |
| NNHA-CONFIG-005 | `dfs.namenode.shared.edits.dir` | `qjournal://<jn1>:8485;<jn2>:8485;.../<ns>` | Uses all selected JNs, not limited to three |
| NNHA-CONFIG-006 | `core-site/fs.defaultFS`, `ha.zookeeper.quorum` | Writes `hdfs://<ns>` and all ZK hosts plus existing `zoo.cfg/clientPort`, defaulting to 2181 | Quorum separator is a comma |
| NNHA-CONFIG-007 | `dfs.ha.fencing.methods`, `dfs.ha.automatic-failover.enabled`, `dfs.namenode.safemode.threshold-pct` | Set to `shell(/bin/true)`, `true`, and `0.99f`, respectively | Read-only in Review |
| NNHA-CONFIG-008 | Removes old HDFS properties | Removes `dfs.namenode.secondary.http-address` and non-namespaced NN RPC/HTTP/HTTPS addresses from the submitted snapshot | Removal occurs during Review initialization and again after secure reload |
| NNHA-CONFIG-009 | `dfs.journalnode.edits.dir` | Defaults to `/hadoop/hdfs/journal` and is the only editable property; Windows stacks inherit the current value from `hdfs-site` | `isOverridable=false` but `isReconfigurable=true` |
| NNHA-CONFIG-010 | `hbase-site/hbase.rootdir` | Applies `/\/\/[^\/]*/` replacement to the original value, changing any existing URI authority to `<ns>` | HBase installed only; the legacy code does not verify that the original authority is the current NN host |
| NNHA-CONFIG-011 | `accumulo-site/instance.volumes` and `.replacements` | Replaces the volume authority with `<ns>`; writes replacements as `<oldValue> <newValue>` | Accumulo installed only |
| NNHA-CONFIG-012 | `ams-hbase-site/hbase.rootdir` | Changes to `<ns>` and becomes visible only when the original contains `hdfs://<currentNN>`; when AMS is installed, Step 9 always submits the complete `ams-hbase-site` snapshot regardless of this match | The only AMS contract in this module; other Metrics content is excluded |
| NNHA-CONFIG-013 | `hawq-site/hawq_dfs_url` | Replaces the URI host:port segment with the nameservice; still requires manual filespace update after completion | Historical HAWQ service only |
| NNHA-CONFIG-014 | `hdfs-client` | Writes `dfs.nameservices`, `dfs.ha.namenodes.<ns>`, and RPC/HTTP addresses for both NNs | HAWQ conditional branch |
| NNHA-CONFIG-015 | Ranger `xasecure.audit.destination.hdfs.dir` | Uses the original `ranger-env` value and replaces the URI authority with `<ns>`; each audit/plugin site is saved only when the service is installed and the original property exists | Step 5 handles HDFS sites; Step 9 handles other service sites |
| NNHA-CONFIG-016 | Config save shape | A single group is `PUT /clusters/{cluster}` with body `{Clusters:{desired_config:[...]}}`; Ranger multi-group save uses the same URL with an array of multiple `{Clusters:{desired_config:[...]}}` bodies | Each item includes `type`, `properties`, optional `properties_attributes`, and a version note |

## Manage JournalNodes Entry and Modes

| ID | Entry/mode | Permissions, conditions, and exact behavior | Primary evidence |
| --- | --- | --- | --- |
| JN-ENTRY-001 | HDFS Service Actions displays Manage JournalNodes | `supports.manageJournalNode`, HDFS `HA_MODE`, `App.isHaEnabled`, and either host count greater than JN count or JN count greater than 3 | `app/views/main/service/item.js`, `app/models/host_component.js#MANAGE_JN` |
| JN-ENTRY-002 | The outer Service Actions template shows the button for OR of `RUN_CUSTOM_COMMAND/RUN_SERVICE_CHECK/START_STOP/TOGGLE_MAINTENANCE/ENABLE_HA`; the inner branch that creates Manage JN accepts only the other four values, excluding `START_STOP` | Manage JN has no independent `SERVICE.ENABLE_HA` requirement. With only `SERVICE.START_STOP`, Actions and start/stop are visible but Manage JN is not generated. Any of `RUN_CUSTOM_COMMAND`, `RUN_SERVICE_CHECK`, `TOGGLE_MAINTENANCE`, or `ENABLE_HA` can generate it | `app/templates/main/service/item.hbs`, `app/views/main/service/item.js` |
| JN-ENTRY-003 | After selecting the service entry, requires both display labels `Active NameNode` and `Standby NameNode` in the model | Comments/text say started is required, but code does not check `workStatus`; failure displays a warning and does not enter the route | `highAvailability_controller.js#manageJournalNode` |
| JN-ENTRY-004 | Host Details Add JournalNode reads Kerberos type and checks the KDC session before confirmation; Manual Kerberos skips the session and adds a warning to the confirmation. Delete JournalNode performs no KDC check and enters the same wizard after confirmation | Both are controlled by host-component UI `HOST.ADD_DELETE_COMPONENTS`; both bypass `manageJournalNode()` Active/Standby checks and do not preselect the clicked host | `main/host/details.js#addComponentWithCheck/#addComponent/#deleteComponent`, host component template/view |
| JN-ENTRY-005 | Delete is disabled when global JN count <= 3 | Minimum 3 is hard-coded; the component usually must also be in a deletable state | `app/views/main/host/details/host_component_view.js#isDeleteComponentDisabled` |
| JN-MODE-001 | Add-only | Adds at least one and deletes none; runs all seven steps | `wizard_controller.js#getJournalNodesToAdd` |
| JN-MODE-002 | Delete-only | Adds none and deletes at least one; skips Step 3 checkpoint and Step 5 manual copy, so users see five steps | `isDeleteOnly`, route Step 2/4, wizard template |
| JN-MODE-003 | Mixed add/delete | Both sets are non-empty; runs all seven steps, including checkpoint and copy | Same as above |
| JN-MODE-004 | No-op | Step 1 Next is disabled when the host set equals the original set | Compares sorted host lists; empty changes cannot enter Review |

## Manage JournalNodes Seven-Step State Machine

### Step 1 Assign JournalNodes

| ID | User behavior and state | Validation/exceptions | Requests/persistence |
| --- | --- | --- | --- |
| JN-STEP1-001 | Initializes the assignment from current JN hosts and allows adding, deleting, or replacing hosts | Retains at least 3; same-component host uniqueness and host/maintenance validation reuse master assignment | Host inventory + Stack Advisor host-group recommendations |
| JN-STEP1-002 | Maximum JN count is `min(stack/host cardinality, existingCount * 2 - 1)` | Adds at most `existingCount - 1` in one operation; for example, 3 can become at most 5 | Client-side cardinality |
| JN-STEP1-003 | Next saves the final master topology, then computes add/delete hosts from the difference with the live model | The Host Details entry does not automatically select the current host | Local DB `masterComponentHosts` |

### Step 2 Review

| ID | User behavior and state | Configuration/branches | Requests |
| --- | --- | --- | --- |
| JN-STEP2-001 | Review lists JN hosts to install/delete and displays read-only HDFS shared-edits changes | Next is disabled until config loading completes; Back can reselect | `config.tags`, `admin.get.all_configurations` |
| JN-STEP2-002 | Non-Federation updates `dfs.namenode.shared.edits.dir` | `qjournal://<最终JN hosts>:8485/<dfs.nameservices>` | Config snapshot |
| JN-STEP2-003 | NameNode Federation updates each `dfs.namenode.shared.edits.dir.<ns>` | Splits `dfs.nameservices` by comma; every namespace uses the same final JN host set | Config snapshot |
| JN-STEP2-004 | Next saves the config snapshot/tag/nameservice; pure delete goes directly to Step 4 and other modes enter Step 3 | `moveJNConfig.configs` is a long-lived controller array; reload does not clear it and may append duplicate same-name configs, a legacy defect | Local DB |

### Step 3 Save Namespace

| ID | User behavior and state | Safety gate/exceptions | Requests |
| --- | --- | --- | --- |
| JN-STEP3-001 | Runs safemode enter and saveNamespace on the Active NN for a single namespace | Uses the same commands as NNHA Step 4; pure deletion skips this step | `admin.high_availability.getNnCheckPointsStatuses` |
| JN-STEP3-002 | For multiple namespaces, displays safemode/saveNamespace commands with `-fs hdfs://<ns>` for each namespace | The first UI line still displays only the saved Active NN host, but commands target each logical URI | Same as above |
| JN-STEP3-003 | For multiple namespaces, selects a check host for each group: uses the Active NN model first; when no Active NN is available, prefers a `STARTED` NN in that group, otherwise the first NN | This is a fault-tolerant selection and does not prove that the selected NN is actually Active | HDFS master component groups/model |
| JN-STEP3-004 | Enables Next only when every returned item has non-empty `Safemode` and a txid checkpoint difference <= 1 | A desired state other than STARTED displays only an error; requests are polled every 1 second | Checkpoint metrics GET |
| JN-STEP3-005 | Does not validate that the response item count equals the namespace count | If the server returns only a qualifying subset, `every()` can pass early; React must validate the complete target namespace/host set | `journalNode/step3_controller.js#checkNnCheckPointStatus` |

### Step 4 Add/Remove JournalNodes

| ID | Strict serial task | Exact side effects and exceptions | Primary requests |
| --- | --- | --- | --- |
| JN-STEP4-001 | 1 Stop Standby NameNode | Sends a PUT for `INSTALLED` to the saved Standby NN | `common.host_components.update` |
| JN-STEP4-002 | 2 Stop Services | Stops all services by sending a PUT for `INSTALLED` to cluster services | `common.services.update` |
| JN-STEP4-003 | 3 Add JournalNodes | Completes immediately when the add set is empty; otherwise runs the component install chain on new hosts and checks KDC | Component install chain |
| JN-STEP4-004 | 4 Delete JournalNodes | Completes immediately when the delete set is empty; otherwise issues a DELETE for each host; treats NoSuchResource as completed | `common.delete.host_component` |
| JN-STEP4-005 | Multi-JN deletion aggregation defect | Each successful DELETE directly calls `onTaskCompleted`, so the first success can advance to Reconfigure while other deletions are still in flight; React must wait for the complete terminal state set | `journalNode/step4_controller.js#deleteJournalNodes`, shared `deleteComponent` |
| JN-STEP4-006 | 5 Reconfigure HDFS | Saves the final `hdfs-site` shared-edits configuration, then installs `HDFS_CLIENT` on all remaining NN/JN hosts | `common.service.configurations` + component install chain |

### Step 5 Copy JournalNode Directories

| ID | User behavior and state | Validation/exceptions | Requests |
| --- | --- | --- | --- |
| JN-STEP5-001 | Archives the Journal directories from any existing JN host, copies them to all new JNs, and extracts them at the same locations | For a single namespace, displays `dfs.journalnode.edits.dir`; for Federation, reads and deduplicates `dfs.journalnode.edits.dir.<ns>` for each namespace | No business request |
| JN-STEP5-002 | The user clicks Next to indicate manual completion | The UI does not validate the tarball, permissions, owner, checksum, or target directory; pure deletion skips this step | None |

### Step 6 Start JournalNodes and Step 7 Start All Services

| ID | Step/behavior | Exact side effects and exceptions | Requests |
| --- | --- | --- | --- |
| JN-STEP6-001 | Step 6 reads JN hosts from the current `App.HostComponent` model and sends a PUT for `STARTED` | The model may not yet reflect additions/deletions, so a new JN may be omitted or a deleted JN may be started; `NEEDS_RUNTIME_VALIDATION` | `common.host_components.update` |
| JN-STEP7-001 | Step 7 sends a PUT for `STARTED` to all services without running smoke tests | After completion, clears tasks/storage, sets status=`DEFAULT`, returns to Services, and refreshes the model | `common.services.update`, persist |

## Progress, Errors, Logs, and Kerberos

| ID | Behavior | Exact semantics |
| --- | --- | --- |
| NNHA-PROGRESS-001 | Strict serial execution | Each progress page's `commands` creates PENDING tasks; it applies QUEUE and runs the next task only after the current task is COMPLETED and persistence succeeds; execution does not continue after a prior failure |
| NNHA-PROGRESS-002 | Request polling | Saves `Requests.id` returned by a mutation to task/current request IDs; performs a GET for the request every 4 seconds until no server task is `PENDING/QUEUED/IN_PROGRESS` |
| NNHA-PROGRESS-003 | Terminal-state aggregation | If any server task is `FAILED`, `TIMEDOUT`, or `ABORTED`, marks the wizard task FAILED; otherwise marks it COMPLETED |
| NNHA-PROGRESS-004 | Retry | Normal NNHA/JN progress supports command-level Retry: resets only the first FAILED command to PENDING and reruns it; previously completed side effects are not undone |
| NNHA-PROGRESS-005 | Skip | The shared mixin provides `onTaskErrorWithSkip`, but no normal NNHA/JN task calls it, so Skip/Ignore and Proceed is unavailable; support must not be inferred from generic button code |
| NNHA-PROGRESS-006 | Host/task details | Clicking an IN_PROGRESS/FAILED/COMPLETED task with an existing request ID opens a popup; aggregates hosts by request/stage, then polls `stdout`, `stderr`, `output_log`, and `error_log` after a single task is expanded |
| NNHA-PROGRESS-007 | Install KDC gate | Each component install calls `getKDCSessionState` before create/register/install; cancelling the invalid-KDC popup for an automatic KDC marks the current task FAILED, while Manual Kerberos calls the callback directly. Ordinary non-KDC HTTP failures from `getSecurityType` or `kerberos.session.state` have no business error callback: they show only the default error dialog, neither continue nor cancel, and do not call `onTaskError`, so an install task can remain `QUEUED` |
| NNHA-PROGRESS-008 | Install-chain failure-handling defect | The service-component create helper's `.always()` continues after creation fails; both success and error for host-component registration point to `onCreateComponent`, which still sends Install. React should stop and present the original error; the legacy behavior is not an idempotency guarantee |
| NNHA-PROGRESS-009 | Mixed topology refresh | The install helper first directly performs a GET for component state/topology, then invokes shared `updateServiceMetric`; only topology/state/maintenance/HA operational fields are baseline data, while metric values and global metrics refresh are excluded |
| NNHA-PROGRESS-010 | Component install wire chain | The normal chain is strictly `host_component.installed.on_hosts` -> when needed `common.create_component` -> when needed `wizard.step8.register_host_to_component` -> `common.host_components.update` targeting `INSTALLED`; `host.host_component.add_new_component` is not in the NNHA/JN chain, and migration must not replace this batch chain with the single-component Host Details install API |
| NNHA-PROGRESS-011 | Invalid-KDC credential persistence | `credentials.get` probes the alias first, then sends a POST to create or a PUT to update the same `/clusters/{cluster}/credentials/{alias}`, with `{Credential: resource}` as the body in both cases; the helper resolves even when create/update fails, after which the caller replays the original AJAX request. The popup's persisted checkbox depends on `Clusters.credential_store_properties['storage.persistent']` already loaded in the cluster model, not on a new `credentials.store.info` request for every popup |

## Exit, Persistence, and Failure Recovery

| ID | Scenario | Legacy behavior and React acceptance |
| --- | --- | --- |
| NNHA-RECOVERY-001 | NNHA snapshot | Each step saves the current step, assignment, and configs; progress separately saves task statuses, per-task request IDs, and current request IDs to the local DB, then writes `CLUSTER_CURRENT_STATUS` through `/persist`; the owner is `wizard-data` |
| NNHA-RECOVERY-002 | NNHA refresh recovery | `HIGH_AVAILABILITY_DEPLOY` is in the valid states; enter restores the current step. IN_PROGRESS continues request polling, QUEUED invokes the command again, and `controller_route.js` can return to the route by owner |
| NNHA-RECOVERY-003 | NNHA Step 1-4 exit | Directly clears tasks/storage, sets cluster state=`DEFAULT`, and returns to Services; no server-side change phase has started, so there is no rollback request |
| NNHA-RECOVERY-004 | Exit with `autoRollbackHA=false` and Step > 4 | Shows a manual rollback warning; confirmation only clears local/persistent state and returns without sending reverse operations. The user must restore any services that may have been stopped |
| NNHA-RECOVERY-005 | `autoRollbackHA=true` | Hides close on Steps 5/7/9; closing Steps 6/8 resets the main wizard to Step 1 and enters the registered three-step rollback route; this does not execute a complete automatic reverse operation |
| JN-RECOVERY-001 | JN snapshot | The controller likewise saves the current step, assignment/configs, and progress task/request IDs; the cluster state is written as `JOURNALNODE_MANAGEMENT` |
| JN-RECOVERY-002 | JN exit | At any step, including after Stop All, deletion, or reconfiguration, exit shows no critical-phase warning and performs no rollback; it directly clears state, sets `DEFAULT`, returns, and refreshes |
| JN-RECOVERY-003 | Unreliable JN recovery | The route enter handler and `controller_route.js` have JN branches, but `JOURNALNODE_MANAGEMENT` is not in `cluster_states.validStates`; server local DB special recovery recognizes only NNHA/Kerberos. Cross-refresh/cross-user recovery is `NEEDS_RUNTIME_VALIDATION` |
| NNHA-RECOVERY-006 | Persistence permission | Cluster status/owner depends on `CLUSTER.MANAGE_USER_PERSISTED_DATA`, while entry explicitly gates only service permission. Without persistence permission, `postUserPref` rejects immediately, and tasks are queued/run only in the persistence success callback, so the first or next PENDING task cannot advance. React must validate workflow and recovery permissions together before entry |

## Actual Rollback and Disable Reachability

| ID | Implementation | Actual behavior/defect | Level |
| --- | --- | --- | --- |
| NNHA-ROLLBACK-001 | Registers route `/highAvailability/NameNode/rollbackHA` named `main.services.rollbackHighAvailability` | Step 1 selects/displays Additional NN and SNN hosts; Step 2 instructs the user to create a checkpoint on the Active NN and polls; Step 3 completes | `PLACEHOLDER` |
| NNHA-ROLLBACK-002 | Registers rollback Step 2 | Inherits the checkpoint controller, but the route's Next is not bound to `isNextEnabled`, so the user can continue without waiting for the checkpoint | `PLACEHOLDER` |
| NNHA-ROLLBACK-003 | Registers rollback Step 3 | Empty page; Next only clears storage, sets `DEFAULT`, and reloads. It does not stop, delete, reconfigure, or start any component or service | `PLACEHOLDER` |
| NNHA-ROLLBACK-004 | `disableHighAvailability()` | Navigates to `main.admin.rollbackHighAvailability`, while the registered route is `main.services.rollbackHighAvailability`; the menu itself is also hidden after HA is enabled | `STATIC_ONLY` incorrect navigation |
| NNHA-ROLLBACK-005 | Monolithic `HighAvailabilityRollbackController` | Statically defines 15 reverse tasks, trimmed by failed command: stop services, restore dependency/HDFS configs, stop/delete ZKFC/PXF/NN/JN, restore SNN, start services, and support Retry/Skip | `STATIC_ONLY`; no registered route/outlet found |
| NNHA-ROLLBACK-006 | Monolithic controller navigation/state | Calls nonexistent `main.admin.highAvailabilityRollback`, writes `HIGH_AVAILABILITY_ROLLBACK`, which is not a valid state; the rollback action in normal main-wizard progress also navigates to the same nonexistent route | `STATIC_ONLY` |
| NNHA-ROLLBACK-007 | Monolithic controller interface/type errors | Calls unregistered `admin.high_availability.load_accumulo_configs` and `.load_hawq_configs`; incorrectly uses `mapProperty` on PXF/Additional NN objects, with inconsistent count/host types in multiple places | `STATIC_ONLY`; cannot serve as a React payload baseline |
| NNHA-ROLLBACK-008 | Product baseline conclusion | The current legacy tree has no trustworthy successful Disable NN HA or automatic rollback path. Any React addition requires separate product design, server-side compensation strategy, and fault-injection acceptance; it must not be claimed as an Ember-equivalent migration | `NEEDS_RUNTIME_VALIDATION` |

## Backend API Contract

The default prefix is `/api/v1`. The 31 IDs in the table cover named requests from both normal wizards, the component-install direct GET, the Kerberos credential chain, and persistence. Additional static interfaces for registered but unwired rollback are excluded from the normal success path and listed separately in the final row.

| ID | App.ajax / Method | URL | Key query/body and usage |
| --- | --- | --- | --- |
| HA-API-001 | `hosts.confirmed` GET | `/clusters/{cluster}/hosts?fields=host_components/HostRoles/state&minimal_response=true` | Base wizard host/topology loading |
| HA-API-002 | `hosts.high_availability.wizard` GET | `/clusters/{cluster}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true` | Assignment host details |
| HA-API-003 | `wizard.loadrecommendations` POST | `{stackVersionUrl}/recommendations` | `hosts`, `services`, `recommend:"host_groups"`, and complete blueprint/binding recommendations |
| HA-API-004 | `config.tags` GET | `/clusters/{cluster}?fields=Clusters/desired_configs` | Reads current tags for Review/secure reload |
| HA-API-005 | `admin.get.all_configurations` GET | `/clusters/{cluster}/configurations?{urlParams}` | OR query `(type=X&tag=Y)\|...` reads exact versions |
| HA-API-031 | `config.on_site` GET | `/clusters/{cluster}/configurations?{params}` | Step 1 indirectly loads `hadoop-env/hdfs_user` through `configurationController.loadFromServer([{siteName:'hadoop-env'}])`; JN uses the same chain when no local cache exists |
| HA-API-006 | `admin.high_availability.getNnCheckPointStatus` GET | `/clusters/{cluster}/hosts/{host}/host_components/NAMENODE` | Single-NN desired state plus the `metrics/dfs/namenode` checkpoint gate |
| HA-API-007 | `admin.high_availability.getNnCheckPointsStatuses` GET | `/clusters/{cluster}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in({hosts})&fields=HostRoles/desired_state,metrics/dfs/namenode&minimal_response=true` | JN single/multiple-namespace checkpoint set |
| HA-API-008 | `admin.high_availability.getJnCheckPointStatus` GET | `/clusters/{cluster}/hosts/{host}/host_components/JOURNALNODE?fields=metrics` | `journalsStatus[ns].Formatted` gate |
| HA-API-009 | `common.services.update` PUT | `/clusters/{cluster}/services?{urlParams}` | `{RequestInfo:{context,operation_level:CLUSTER},Body:{ServiceInfo:{state}}}`; stops/starts all or selected services |
| HA-API-010 | `common.host_components.update` PUT | `/clusters/{cluster}/host_components?{urlParams}` | RequestInfo query/context/operation level plus `Body.HostRoles.state`; query excludes hosts with maintenance other than OFF |
| HA-API-011 | `common.host.host_component.update` PUT | `/clusters/{cluster}/hosts/{host}/host_components/{component}?{urlParams}` | Static rollback and generic single-component state mutation |
| HA-API-012 | `common.host.host_component.passive` PUT | `/clusters/{cluster}/hosts/{host}/host_components/{component}` | `{RequestInfo:{context},Body:{HostRoles:{maintenance_state}}}`; SNN maintenance ON |
| HA-API-013 | `common.delete.host_component` DELETE | `/clusters/{cluster}/hosts/{host}/host_components/{component}` | Deletes SNN/JN and static rollback resources |
| HA-API-014 | `common.service.configurations` PUT | `/clusters/{cluster}` | `{Clusters:{desired_config:[{type,properties,properties_attributes?,service_config_version_note}]}}` |
| HA-API-015 | `common.service.multiConfigurations` PUT | `/clusters/{cluster}` | Body contains multiple `{Clusters:{desired_config:[...]}}`; saves Ranger groups |
| HA-API-016 | `host_component.installed.on_hosts` GET | `/clusters/{cluster}/host_components?HostRoles/component_name={component}&HostRoles/host_name.in({hosts})&fields=HostRoles/host_name&minimal_response=true` | Pre-install existence check |
| HA-API-017 | `common.create_component` POST | `/clusters/{cluster}/services?ServiceInfo/service_name={service}` | Body `components[].ServiceComponentInfo.component_name`; ensures the service-component exists |
| HA-API-018 | `wizard.step8.register_host_to_component` POST | `/clusters/{cluster}/hosts` | `RequestInfo.query=Hosts/host_name=...\|...` plus `Body.host_components[].HostRoles.component_name` |
| HA-API-019 | Direct `App.HttpClient` GET | `/clusters/{cluster}/components/?fields=ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true` | `updateComponentsState` refreshes service/category/state counts and host topology before component creation; not Metrics |
| HA-API-020 | Direct `App.HttpClient` GET | `/clusters/{cluster}/components/?{FLUME/ATS/HA component predicates}ServiceComponentInfo/category.in(MASTER,CLIENT)&fields=ServiceComponentInfo/service_name,host_components/HostRoles/{display_name,host_name,public_host_name,state,maintenance_state,stale_configs,ha_state,desired_admin_state},{conditionalFields}&minimal_response=true` | `updateServiceMetric` refreshes master/client topology/state/maintenance/stale/HA/desired-admin; conditional fields retain only operational selection fields such as HDFS `ClusterId` and HBase `IsActiveMaster`, excluding metric values |
| HA-API-021 | `background_operations.get_by_request` GET | `/clusters/{cluster}/requests/{requestId}?fields=*,tasks/...&minimal_response=true` | Progress aggregation polling every 4 seconds |
| HA-API-022 | `common.request.polling` GET | `/clusters/{cluster}/requests/{requestId}?fields=tasks/...&tasks/Tasks/stage_id={stageId}` | Stage polling in a popup when stageId is present |
| HA-API-023 | `background_operations.get_by_task` GET | `/clusters/{cluster}/requests/{requestId}/tasks/{taskId}` | stdout/stderr/output/error log details |
| HA-API-024 | `admin.security.cluster_configs.kerberos` GET | `/clusters/{cluster}/configurations/service_config_versions?service_name=KERBEROS&is_current=true` | Reads `kerberos-env/kdc_type` when the controller does not know the KDC type; there is no business error callback, and ordinary HTTP failure does not invoke the original callback |
| HA-API-025 | `kerberos.session.state` GET | `/clusters/{cluster}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details` | Automatic-KDC session gate; skipped for Manual; ordinary HTTP failure only shows the default error dialog and does not mark the install task FAILED |
| HA-API-026 | `credentials.get/create/update` GET/POST/PUT | `/clusters/{cluster}/credentials/{alias}`, with alias=`kdc.admin.credential` for HA | GET checks the alias; POST/PUT bodies are `{Credential:{principal,key,type}}`, with type `temporary` or `persisted`; the helper still resolves after create/update failure and replays the original AJAX; persisted availability comes from cluster-model credential-store properties |
| HA-API-027 | `persist.get` GET | `/persist/CLUSTER_CURRENT_STATUS` | Loads the compressed cluster/wizard local DB; 404 uses default state |
| HA-API-028 | `persist.post` POST | `/persist` | Body key/value includes `CLUSTER_CURRENT_STATUS` or the wizard owner; controlled by persisted-data permission |
| HA-API-029 | `hosts.all` GET | `/clusters/{cluster}/hosts?fields=Hosts/*,host_components/...` | Host selection/mapping for registered rollback Step 1; normal NNHA/JN flows do not depend on this dedicated call |
| HA-API-030 | Static rollback interface set | Config load/save, component GET/delete/passive, and service mutation | Used only by the unwired `HighAvailabilityRollbackController`; two request names are unregistered; `STATIC_ONLY`, excluded from the normal implementation |

## Known Implementation Risks and React Decisions

| ID | Risk | React handling requirement |
| --- | --- | --- |
| JN-RISK-001 | NNHA Steps 4 and 6 directly `JSON.parse` server JSON strings without try/catch; poll GET has no error callback/reschedule | Malformed/missing payloads must present a recoverable error and continue controlled polling; the page must not fail silently |
| JN-RISK-002 | NNHA Step 6 waits for only the first three JN responses | Build a complete-set barrier using stable IDs for selected hosts and wait for all success or an explicit failure/timeout |
| JN-RISK-003 | In JN Step 4, the first successful DELETE can advance the workflow | Mutation aggregation must be all-settled and show results by host; Retry after failure must retry only failed items |
| JN-RISK-004 | JN Step 6 depends on an Ember model that may not have been refreshed | Re-read final topology from the server after Reconfigure/Delete, then start the exact target set |
| JN-RISK-005 | Multiple-namespace checkpoint validation does not verify response cardinality/identity | Validate the expected mapping from each namespace to its Active NN; pass only with no missing or duplicate items |
| JN-RISK-006 | The JN Review config array may accumulate duplicates on reload | Rebuild from an immutable template on every load and deduplicate by `(site,name)` before submission |
| JN-RISK-007 | Component registration errors still proceed to Install, and service-component creation failures are swallowed | Stop the current task on any prerequisite mutation failure; handle ResourceAlreadyExists as an idempotent branch using an explicit error code |
| JN-RISK-008 | The JN route can bypass Active/Standby checks through Host Details, and direct URLs have no unified business gate | The route loader must perform unified permission, HA topology, state, and cardinality validation; all entry points should only navigate |
| JN-RISK-009 | JN critical progress exits without confirmation/rollback, while NNHA without auto rollback only clears state | React must retain a list of completed side effects and must not discard failure recovery context without notice; the compensation flow requires separate design |
| JN-RISK-010 | Registered rollback is only a shell, while the complete static rollback is unwired and contains interface/type errors | Do not reuse the old controller; redefine rollback with server-side transactions/idempotent compensation and fault injection |

## Test Evidence and Runtime Acceptance Matrix

| ID | Evidence/scenario | Current coverage and gap |
| --- | --- | --- |
| HA-TEST-001 | NNHA Step 1-9 controllers/views | Legacy Karma tests cover ID validation, assignment, Review initialization, checkpoint, each progress task, and manual pages; Steps 5/9 each have AJAX assertions, but there are 4 `it.skip` cases in total, so this is not continuous passing evidence |
| HA-TEST-002 | JN Step 1-7 | Controller tests cover Steps 1/2/3/4/6/7 and progress/wizard; there is no Step 5 controller test, only a Step 5 view test |
| HA-TEST-003 | Shared progress/popup | Tests cover task recovery, Retry, request/task popups, and logs; they cannot prove real server polling, KDC replay, or multi-request races |
| HA-TEST-004 | Configuration migration | `move_namenode_config_initializer_test.js` covers host/config initialization; real-stack validation is still required for property existence, Windows, and historical HAWQ/Accumulo/Ranger combinations |
| HA-TEST-005 | Entry/permission | Service-item and Host Details tests cover some menu and JN navigation; there are no direct-URL authorization, Active/Standby bypass, or persistence-permission combination tests |
| HA-TEST-006 | Recovery/exit/owner | No complete tests were found for NNHA/JN route exit, wizard owner, or cross-refresh/cross-user behavior; a failure-point matrix must be run in the browser and Server |
| HA-TEST-007 | Rollback | The registered three-step placeholder and unwired 15-task controller have no direct tests; the incorrect Disable route is also untested |
| HA-TEST-008 | More than three JNs | Out-of-order/partial NNHA formatted responses are not covered; a complete-set barrier must be tested with 4/5 JNs |
| HA-TEST-009 | Concurrent JN deletion/model refresh | No coverage for first DELETE success with other failures/delays or the Step 6 stale model; latency and failure injection are required |
| HA-TEST-010 | Federation checkpoint | Missing/duplicate namespace responses, no-Active-label fallback, and a non-STARTED NN combination are not covered |
| HA-TEST-011 | Kerberos modes | Automatic MIT/AD/IPA must validate session expiry, credential cancellation/save failure/Retry; Manual must verify that no KDC admin credential is required while component identity is installed correctly |
| HA-TEST-012 | Complete success and checkpoint recovery | At minimum, accept non-secure, secure automatic-KDC, Manual Kerberos, dependency-service combinations, JN add-only/delete-only/mixed, refresh before/after every mutation, Server restart, and login by another user |

## Five-Round Independent Audit Record

| Round | Review surface | Module conclusion |
| --- | --- | --- |
| 1 | Routes, menus, and template actions | Found the two formal wizards, the registered rollback route, the upgrade-check entry, and the Host Details JN bypass; confirmed that the legacy Disable entry is actually hidden |
| 2 | Nine/seven-step controllers and shared mixins | Established strict task order, manual commands, checkpoint gates, Kerberos gate, Retry/no Skip, and exit semantics |
| 3 | Configuration definitions, initializer, and stack conditions | Established all HDFS keys, dependency-service migration, default ports, Windows branches, and the sole retained AMS boundary |
| 4 | AJAX registration, call inventory, direct HTTP, and persistence | Established the 31-item API contract, added indirect `config.on_site`, distinguished the normal install chain and operational metrics fields, and excluded metrics refresh and STATIC_ONLY rollback interfaces |
| 5 | Tests and reverse-gap inspection | Confirmed existing test coverage and skips/blanks; identified gaps for more than 3 JNs, concurrent deletion, stale model, namespace-subset responses, route exit/recovery, and rollback |

The minimum bar for completed React comparison is that every stable ID in this document has a `MATCH/MISSING/DIFFERENT/NOT_APPLICABLE` conclusion; every `NEEDS_RUNTIME_VALIDATION` scenario retains request, task, and configuration-version evidence from a real Ambari Server/Agent/stack; and every legacy risk is replaced by an explicit remediation decision rather than silently copied.
