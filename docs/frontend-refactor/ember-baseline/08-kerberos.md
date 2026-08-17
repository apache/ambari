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

# Kerberos Security Baseline

The legacy Ember entry point is `/main/admin/kerberos`, the Enable wizard is `/main/admin/kerberos/enable/step1` through `step8`, and Disable is `/main/admin/kerberos/disableSecurity`. Enable is a long-running flow registered in the global wizard recovery table that stops the entire cluster and changes the cluster security type. Disable reuses the progress controller but is only a route-local modal and does not provide the same automatic recovery guarantee. Neither is an ordinary configuration form.

## Entry, Permissions, and Preconditions

| ID | Function and behavior | Preconditions/branches | Backend requests | Primary evidence |
| --- | --- | --- | --- | --- |
| KRB-ENTRY-001 | Loads the cluster security type; displays the enabled page for `KERBEROS`, otherwise displays Enable | Displays an error popup when the status request fails and a spinner while loading | `admin.security_status`, `admin.security.cluster_configs.kerberos` | `app/controllers/main/admin/kerberos.js`, `app/templates/main/admin/kerberos.hbs` |
| KRB-ENTRY-002 | Page access and Enable/Disable/Edit visibility are controlled by both permission and feature flag | Requires `CLUSTER.TOGGLE_KERBEROS` and `supports.enableToggleKerberos`; otherwise the route redirects to Dashboard | Same as above | `app/routes/main.js`, `app/templates/main/admin/kerberos.hbs` |
| KRB-ENTRY-003 | Displays an individual warning for each installed service before Enable | The current code has a dedicated YARN message; cancelling any warning prevents the wizard from starting | None | `app/controllers/main/admin/kerberos.js#checkServiceWarnings` |
| KRB-ENTRY-004 | Optional Pre-Kerberize Checks | `supports.preKerberizeCheck`; when `UpgradeChecks.status=FAIL` exists, displays the cluster check popup and blocks entry | `admin.kerberos_security.checks` | `app/controllers/main/admin/kerberos.js#checkAndStartKerberosWizard` |
| KRB-ENTRY-005 | Starts the wizard and records ownership/recovery state | Saves `onClosePath`, sets cluster state to `KERBEROS_DEPLOY`, and sets `wizardControllerName=kerberosWizardController` | Cluster status/persist | `app/routes/add_kerberos_routes.js`, `app/controllers/main/admin/kerberos/wizard_controller.js` |

## Four Enable Modes

| ID | Mode | Step 2 visible/special configuration | Subsequent flow differences |
| --- | --- | --- | --- |
| KRB-MODE-001 | Existing MIT KDC, backend value `mit-kdc` | MIT settings such as KDC hosts, realm, admin principal/password, and executable search paths; AD password-policy fields are hidden | Creates the KERBEROS service/client, installs and tests the client, and lets Ambari manage principals/keytabs |
| KRB-MODE-002 | Existing Active Directory, backend value `active-directory` | KDC hosts, realm, LDAP URL, container DN, AD password rules, and other settings; AD-specific fields are visible | Same as automatic mode; KDC connection/session and credential store are available |
| KRB-MODE-003 | Existing IPA, backend value `ipa` | IPA-specific settings; saving forces `install_packages=false` and `manage_krb5_conf=false` | Ambari still manages identities but does not have Ambari install packages or manage krb5.conf |
| KRB-MODE-004 | Manage principals and keytabs manually, backend value `none` | Retains only exceptions such as realm, KDC type, and executable search paths; hides KDC credential fields | Forces `manage_identities=false`, `install_packages=false`, and `manage_krb5_conf=false`; does not create/install KERBEROS_CLIENT, skips directly from Step 2 to Step 4, and requires the user to download CSV and manually generate/distribute principals/keytabs |

Step 1 displays an independent precondition checklist for each mode. Switching modes clears all checks for that mode; Next is available only when every visible condition for the current mode is confirmed. MIT also has additional conditions when installing ONEFS.

## Eight-Step Enable Kerberos Flow

### Step 1 Get Started

| ID | Function and behavior | Validation/exceptions | Backend requests |
| --- | --- | --- | --- |
| KRB-1-001 | Selects one of MIT, AD, IPA, and Manual modes | MIT is the default; changing the option clears the precondition checkboxes | None |
| KRB-1-002 | Confirms deployment preconditions for the selected mode individually | Cannot continue while any visible condition is unchecked | None |

### Step 2 Configure Kerberos

| ID | Function and behavior | Validation/branches | Backend requests |
| --- | --- | --- | --- |
| KRB-2-001 | Loads KERBEROS config types from the stack, filters fields by KDC mode, and sets `kdc_type` | Visibility differs for AD/IPA/MIT/Manual; required fields and realm/host/password validation reuse config validation | Stack config APIs |
| KRB-2-002 | Tests the KDC connection | Closing the wizard during the test requires confirmation; failure displays a KDC error and allows re-entry | `admin.kerberos_security.test_connection` |
| KRB-2-003 | In automatic modes, creates the KERBEROS service, KERBEROS_CLIENT service component, and all host-components | First deletes a leftover KERBEROS service to avoid old-wizard resource conflicts. After refreshing component state, explicitly creates `KERBEROS_CLIENT` when the service component is missing | `common.delete.service`, `wizard.step8.create_selected_services`, `common.create_component`, `wizard.step8.register_host_to_component` |
| KRB-2-004 | Saves desired configs such as `kerberos-env` | Submits multiple config types together and writes a config-version note | `common.across.services.configurations` |
| KRB-2-005 | Creates a live KDC admin session/credentials | Automatic mode first performs a GET for the alias, then sends a POST or PUT for a temporary or persisted credential; create/update always resolves, so Step 2 `.done()` continues on failure. Manual passes the current session through cluster `session_attributes.kerberos_admin` and does not manage identities afterward | `credentials.get/create/update` or `common.cluster.update` |
| KRB-2-006 | Forces security options for Manual/IPA | Manual disables identity/package/krb5.conf management; IPA disables package/krb5.conf management | Same request as config save |
| KRB-2-007 | Step 2 failure propagation is inconsistent | Failure of a leftover KERBEROS service DELETE is ignored by `.always()`; failure of the KERBEROS service POST, host-component registration, or config save blocks. `common.create_component` itself forces resolve through `.always()`, so creation failure still continues registration; automatic credential POST/PUT also always resolves and continues. Manual `common.cluster.update` failure blocks. No blocking branch resets `nextBtnClickInProgress`; there is no page-level Retry and Next remains locked | `common.delete.service`, resource creation, config save, credential CRUD, `common.cluster.update`; `KNOWN_BUG` |

### Step 3 Install And Test Kerberos Client

| ID | Function and behavior | Failure/recovery | Backend requests |
| --- | --- | --- | --- |
| KRB-3-001 | Installs KERBEROS_CLIENT on all hosts; when the service component is still INIT, first sets the KERBEROS service to INSTALLED | Displays progress tasks and host/task output | `common.service_component.info`, `common.services.update`, host-component install requests |
| KRB-3-002 | Runs the KERBEROS service check | An invalid KDC session triggers a credential popup; cancellation marks the task FAILED | `service.item.smoke` |
| KRB-3-003 | Checks for HEARTBEAT_LOST hosts after completion | Any lost heartbeat marks the first task FAILED and displays affected-host details | `hosts.heartbeat_lost` |
| KRB-3-004 | Failed tasks support Retry, and the entire step can continue after selecting `Ignore errors and continue` | When there is a lost heartbeat, Retry starts from the first install task. This controller never sets `canSkip`, so there is no task-level Skip. When `supports.autoRollbackHA=true`, a failed task displays Rollback, but the Kerberos controller has no `rollback()` handler, making the conditionally displayed button broken | Retry resends the failed install/test request; Ignore and the broken Rollback button make no mutation; `KNOWN_BUG` |
| KRB-3-005 | Manual mode skips this step entirely | Step 2 Next goes directly to Step 4, and Back from Step 4 returns to Step 2 | None |

### Step 4 Configure Identities

| ID | Function and behavior | Validation/branches | Backend requests |
| --- | --- | --- | --- |
| KRB-4-001 | Reads the Kerberos descriptor and generates identity configuration for Global, Ambari Principals, and installed services | Enable Step 4 unconditionally reads cluster `COMPOSITE?evaluate_when=true` without reading STACK first. Add Service reads STACK first, then COMPOSITE, with COMPOSITE overriding same-name values while retaining properties unique to each side. Add Service also probes whether the cluster artifact exists to choose create or update | Enable: `admin.kerberize.cluster_descriptor`; Add Service: `admin.kerberize.cluster_descriptor.stack`, `admin.kerberize.cluster_descriptor`, `admin.kerberize.cluster_descriptor_artifact` |
| KRB-4-002 | Stack Advisor recommends identity/config values | Requests when `supports.kerberosStackAdvisor` and no stored values exist; required recommendations cannot be silently discarded | `config.recommendations` |
| KRB-4-003 | Edits descriptor properties such as principal/keytab/name/rule | Manual hides KDC credential properties; filters identities by installed services | Descriptor is submitted only on Next |
| KRB-4-004 | Creates or updates the cluster `kerberos_descriptor` artifact | A POST 409 changes to PUT; caches form values before submission for failure return | `admin.kerberos.cluster.artifact.create`, `.update` |
| KRB-4-005 | After submitting the descriptor, calls unkerberize to clean up partial security state before entering Confirm | Advances on both success and failure so that the formal kerberize operation starts from a consistent state | `admin.unkerberize.cluster` |
| KRB-4-006 | Enable Step load waits for the COMPOSITE descriptor GET | `getDescriptor()` creates a Deferred that resolves but never rejects; when GET fails, `loadStep()`'s failure callback is unreachable, leaving the page permanently pending with no Retry | `admin.kerberize.cluster_descriptor`; `KNOWN_BUG` |

### Step 5 Confirm Configuration

| ID | Function and behavior | Validation/branches | Backend requests |
| --- | --- | --- | --- |
| KRB-5-001 | Displays final KDC properties according to the selected mode | MIT/IPA/Manual/AD display different field sets; empty values are hidden | Prior configs |
| KRB-5-002 | Downloads `kerberos.csv` | The error callback incorrectly reuses the success handler; on failure, calling `split('\n')` on jqXHR throws and the download progress flag is not reset | `admin.kerberos.cluster.csv`; `KNOWN_BUG` |
| KRB-5-003 | In Manual mode, instructs the user to create principals/keytabs manually from the CSV and distribute them to the target paths | This is a manual-responsibility boundary before continuing to Step 6; the UI does not verify that the files exist | CSV request |
| KRB-5-004 | Exit Wizard | Still displays an exit warning and runs discard: sets security to NONE, deletes the KERBEROS service, and clears state | `admin.unkerberize.cluster`, `common.delete.service` |

### Step 6 Stop Services

| ID | Function and behavior | Dynamic tasks/failure | Backend requests |
| --- | --- | --- | --- |
| KRB-6-001 | Stops all services | Displays a critical warning when closing during a critical phase; disables lower-level steps | `common.services.update` |
| KRB-6-002 | When YARN is installed, ATS does not support Kerberos, and APP_TIMELINE_SERVER exists, deletes the component | NoSuchResource is treated as complete; other errors neither complete nor call `onTaskError`, leaving the task permanently stuck. This is compatibility cleanup, not a Metrics feature | `common.delete.host_component`; non-NoSuchResource failure is `KNOWN_BUG` |

### Step 7 Kerberize Cluster

| ID | Function and behavior | Failure/recovery | Backend requests |
| --- | --- | --- | --- |
| KRB-7-001 | Sets `Clusters.security_type` to `KERBEROS` and starts the server-side KERBERIZE_CLUSTER request | Single-request progress with request/task polling; cannot return to lower-level steps while running | `admin.kerberize.cluster`, request polling |
| KRB-7-002 | After failure, allows returning to Step 4 to edit the descriptor or Retry | Retry clears old stages/tasks and sends `force_toggle_kerberos=true`; it does not unkerberize first | `admin.kerberize.cluster.force` |
| KRB-7-003 | The controller retains an `unkerberizeCluster()` cleanup method | No template action, route handler, or production call site exists; only unit tests call it directly. Both success/failure callbacks go to Step 7, but it is not a user-reachable Retry path | `admin.unkerberize.cluster`; `STATIC_ONLY` |

### Step 8 Start And Test Services

| ID | Function and behavior | Failure/completion | Backend requests |
| --- | --- | --- | --- |
| KRB-8-001 | Starts all services and uses an Ambari property to determine whether to run smoke tests at the same time | `skip.service.checks=true` sets `run_smoke_test=false` | `common.services.update`, request polling |
| KRB-8-002 | Allows Complete even when the request fails | Submit is available in `COMPLETED` or `FAILED`; after completion, the user must repair failed services manually on the normal pages | Prior request |
| KRB-8-003 | Complete clears the Kerberos wizard local DB/status and returns to the Kerberos management page | Closing Step 8 does not discard; it shows only a normal warning and retains enabled Kerberos | Cluster status/persist |

## Disable Kerberos

| ID | Sequence | Behavior and conditions | Backend requests |
| --- | --- | --- | --- |
| KRB-DIS-001 | Preconditions | Disable is available only when enabled, authorized, and no identity edits are unsaved; first displays service warnings and confirmation | `admin.security_status` |
| KRB-DIS-002 | 1 Start ZooKeeper | Starts only ZooKeeper so the services required for unkerberize are available | Common service update |
| KRB-DIS-003 | 2 Stop Required Services | Stops services other than ZooKeeper | Common service update |
| KRB-DIS-004 | 3 Unkerberize Cluster | Changes the security type back to NONE and lets the backend revoke identities/config changes | `admin.unkerberize.cluster` |
| KRB-DIS-005 | 3 failure skip | An unkerberize error can enter a skip branch that does not manage Kerberos identities | `admin.unkerberize.cluster.skip` |
| KRB-DIS-006 | 4 Remove Kerberos | Deletes the KERBEROS service; deletion failure is also treated as task completion to avoid a permanent stall | `common.delete.service` |
| KRB-DIS-007 | 5 Start Services | Starts all services with `runSmokeTest=true`; the generic progress controller then uses the Ambari property `skip.service.checks` to determine actual `params/run_smoke_test`, after which it can close and refresh the cluster | Common service update |
| KRB-DIS-008 | Exit and recovery boundaries | `unroutePath=false`; closing is hard-blocked while unkerberize is running and requires confirmation at other incomplete stages. On close, clears task/local namespaces, writes cluster state=`DEFAULT`, and reloads. The Disable controller's own `clusterDeployState` is `DEFAULT` from the start and is not in `controller_route.js`, so refresh/crash has no Enable-style automatic reroute recovery; recovery completeness is `NEEDS_RUNTIME_VALIDATION` | Cluster status/persist |
| KRB-DIS-009 | Closing the Disable modal incorrectly calls `addServiceController.finish()` in the cleanup chain | First clears Disable progress in-memory tasks/current request IDs, the Disable DB namespace, and security deploy commands; then resets Add Service in-memory install options/hosts/cluster shell, clears its persisted wizard fields and DB namespace, and calls `updateAll()`. It does not directly reset Add Service in-memory `currentStep`; `clearServiceConfigProperties()` also calls `get`, so it clears only DB values and not in-memory config. Finally it persists `clusterState=DEFAULT` and the current local DB, then reloads. This is a cross-wizard legacy side effect, not a Disable requirement | Cluster status/persist; `updateAll()` triggers normal global refresh; `KNOWN_BUG` |

## Post-Enable Management Capabilities

| ID | Function and behavior | Permissions/branches | Backend requests |
| --- | --- | --- | --- |
| KRB-MGMT-001 | Views composite identities/configs grouped as Global, Ambari Principals, and each service | Page-level permissions are the same as Kerberos; loads identities only for installed services | Descriptor/config APIs |
| KRB-MGMT-002 | Edits, cancels, and saves identities | `CLUSTER.TOGGLE_KERBEROS` + flag; realm is always read-only; Cancel restores the saved/default value; Disable/Regenerate is disabled while changes are unsaved | `admin.kerberos.cluster.artifact.update`, attempts `.create` on 404 |
| KRB-MGMT-003 | Regenerates keytabs after saving identity changes | On normal PUT success, Manual regenerates all without automatic restart; automatic mode first asks whether to restart affected components. The 404 fallback POST is not chained to the original PUT promise and cannot trigger the same success flow | Artifact + `admin.kerberos_security.regenerate_keytabs` |
| KRB-MGMT-004 | Cluster-level Regenerate Keytabs | Shown only for automatic Kerberos; selects `all` or `missing`, then automatic or later manual restart | `admin.kerberos_security.regenerate_keytabs` |
| KRB-MGMT-005 | Service-level Regenerate Keytabs | Generates for all components of the selected service from service actions, with config update policy none | `admin.kerberos_security.regenerate_keytabs.service` |
| KRB-MGMT-006 | Host-level Regenerate Keytabs | Requires `supports.regenerateKeytabsOnSingleHost` and enabled Kerberos; targets one host with config update policy none | `admin.kerberos_security.regenerate_keytabs.host` |
| KRB-MGMT-007 | Associates background operations after successful Regenerate | Success first uses the `show_bg` setting to decide whether to show Background Operations, and sets the restart flag only in `.done()` after that GET settles. The underlying preference GET resolves through `.always()`; on failure it usually does not show the popup but still sets the flag. The restart observer is triggered only by a **subsequent** global `runningOperationsCount` change, not immediately when the flag is set; if the count reaches zero before GET settles and then does not change, restart does not occur. An unrelated operation can delay restart all until the global count reaches zero again and can even become the change that triggers a previously missed restart | Regenerate request + user-setting/background-operation APIs; `KNOWN_BUG` |
| KRB-MGMT-008 | Downloads the current identities CSV | The button is controlled by `CLUSTER.UPGRADE_DOWNGRADE_STACK`; not limited to Manual mode | `admin.kerberos.cluster.csv` |
| KRB-MGMT-009 | The artifact-update 404 create fallback has an implementation defect | The error callback uses `self`, which is undefined in scope; even when the runtime does not throw there, the independent POST does not return/resolve the original PUT. Manual's original PUT `.done()` does not regenerate, and automatic mode also does not wait for fallback; mark `KNOWN_BUG` and define an explicit create-then-regenerate atomic chain in React | `admin.kerberos.cluster.artifact.update`, `.create` |

## KDC Credential Store

| ID | Function and behavior | Validation/branches | Backend requests |
| --- | --- | --- | --- |
| KRB-CRED-001 | Detects persistent credential-store capability | The Manage button is shown only for non-Manual mode when `App.isCredentialStorePersistent`; the value comes from cluster model `Clusters.credential_store_properties/storage.persistent`. `credentials.store.info` has a registration and utility wrapper, but no legacy production call site uses it to determine the button | Cluster load; `credentials.store.info` is a `STATIC_ONLY` utility |
| KRB-CRED-002 | Checks whether `kdc.admin.credential` exists | The API list does not echo the secret to the UI; it determines only stored/removable state | `credentials.list`, `credentials.get` |
| KRB-CRED-003 | Saves the KDC admin principal/password | Principal/password are required and principal cannot be blank; sends a PUT when present and a POST when absent; the management form fixes the resource type to persisted | `credentials.get` followed by `credentials.update` or `credentials.create` |
| KRB-CRED-004 | Deletes a persisted KDC credential | Confirmation is required; refreshes removable state after the request settles | `credentials.delete` |
| KRB-CRED-005 | Validates the session when another wizard needs KDC | Runs the original callback only when KDC validation succeeds; failure opens an invalid KDC popup, allowing new credentials and optional persistence | `kerberos.session.state`, credential CRUD |
| KRB-CRED-006 | Credential CRUD failure propagation is swallowed | `createOrUpdateCredentials()` ultimately resolves POST/PUT through `.always()` and passes only a success boolean; the management form uses `.always()` again and discards that boolean, so a failed save still displays success. DELETE also displays success in `.always()`; because the invalid-KDC popup resolves, it replays the original AJAX after a failed save | Credential CRUD; `KNOWN_BUG` |

## Integration with Installation and Daily Operations

| ID | Scenario | Required conditional behavior |
| --- | --- | --- |
| KRB-X-001 | Add Service | The entry first loads security status/KDC type; Customize Configs validates and merges the Kerberos descriptor for the new service; Manual mode updates the descriptor and generates CSV before Review |
| KRB-X-002 | Add Host | Checks the KDC session before Review submission; automatic mode requires valid admin credentials, while Manual mode continues directly |
| KRB-X-003 | Add/Delete Host Component | Adding a component in a Kerberos cluster may require a KDC session and keytabs; after deleting/recovering a host, Regenerate is available from host actions |
| KRB-X-004 | Reassign Master/HA/Federation | The new master/component identity, principal, and keytab must be synchronized with component/config changes; do not copy only the non-security task list |
| KRB-X-005 | Service/Host restart | Regenerate can ask the backend to restart affected components automatically or only generate keytabs and leave restart responsibility to the user |

## Recovery, Exit, and Static Boundaries

| ID | Behavior | Details |
| --- | --- | --- |
| KRB-REC-001 | Enable saves a complete local DB snapshot to cluster status on every step change | `clusterState=KERBEROS_DEPLOY`; refresh or another window recovers from `localdb.KerberosWizard.currentStep` |
| KRB-REC-002 | Enable saves task statuses, task request IDs, and old request IDs | Steps 3/6/7/8 continue polling after recovery rather than sending duplicate requests |
| KRB-REC-003 | Every Enable step's `unroutePath()` returns false | Can exit only through the modal close handler; Step 2 connection test has additional confirmation, and Steps 6/7 have critical warnings |
| KRB-REC-004 | Exit during incomplete Enable performs discard | Unkerberizes and deletes the KERBEROS service; closing Step 8 is an exception and does not revoke a completed security transition |
| KRB-REC-005 | Manual CSV completion cannot be proven by static UI | React comparison must use real MIT/AD/IPA/Manual environments to verify that the backend returns equivalent errors when principals/keytabs are missing; marked `NEEDS_RUNTIME_VALIDATION` |
| KRB-REC-006 | Disable is not in the global wizard controller-route recovery table | The modal controller loads task/request IDs from local DB within the same page instance, but `clusterState=DEFAULT`, so refresh does not automatically return to Disable based on server status; validate refresh/server restart before and after each mutation; `NEEDS_RUNTIME_VALIDATION` |

## Known Defects and Validation Gates

| ID | Static conclusion | React baseline requirement |
| --- | --- | --- |
| KRB-RISK-001 | Step 3 displays Rollback when `supports.autoRollbackHA=true` and a task fails, but the controller has no `rollback()` handler; Skip also has no reachable state | Do not register the broken button as executable legacy capability; React should hide Rollback or implement a stateful inverse operation and mark it as an intentional fix |
| KRB-RISK-002 | Step 7's isolated unkerberize method is separate from the actual Retry | Compare React Retry with the direct force kerberize legacy behavior; if compensating cleanup is added first, record the new security semantics |
| KRB-RISK-003 | Credential save/update/delete failures are reported as success by the UI, and a failed save can replay the original KDC request | React must reject correctly, retain input, and stop replaying the original request; this fixes a legacy bug and does not require reproducing the error |
| KRB-RISK-004 | The descriptor 404 fallback may throw directly because `self` is undefined and does not continue to Regenerate | React must chain PUT 404 -> POST -> regenerate/restart as an observable promise and cover both Manual/automatic branches |
| KRB-RISK-005 | Create/update failure semantics and store-type helper tests in `credentials_test.js` are disabled by `describe.skip` | Legacy tests are not evidence that failure paths are verified; add tests for 404, 401/403, 500, network interruption, and duplicate submission |

See the heuristic module inventory at [generated/api-by-module/security-ha-federation.md](generated/api-by-module/security-ha-federation.md): it uses broad matching of request names and caller paths, may include cross-module requests or omit requests indirectly called by shared Kerberos controllers/mixins, and cannot be treated as a complete interface inventory. Credential Store is currently classified under background/common but remains part of this baseline; authoritative network verification must jointly inspect [generated/ajax-endpoints.md](generated/ajax-endpoints.md), [generated/ajax-calls.md](generated/ajax-calls.md), [generated/direct-http-calls.md](generated/direct-http-calls.md), [generated/browser-network-entrypoints.md](generated/browser-network-entrypoints.md), and [generated/realtime-channels.md](generated/realtime-channels.md).
