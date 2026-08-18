/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useCallback, useContext, useEffect, useState } from "react";
import { HostsApi } from "../../api/hostsApi";
import { cloneDeep, filter, find, forEach, get, map, uniq } from "lodash";
import {
  Alert,
  Button,
  Card,
  CardBody,
  CardFooter,
  FormCheck,
  Stack,
} from "react-bootstrap";
import Table from "../../components/Table";

import { role, sortPropertyLight } from "../../Utils/Utility";
import ConfigsApi from "../../api/configsApi";
import componentApi from "../../api/componentApi";
import Modal from "../../components/Modal";
import { AppContext } from "../../store/context";
import { useAuth } from "../../hooks/useAuth";
import UpgradeGuard from "../../components/UpgradeGuard";
import { safeUpdateClusterEnvConfig } from "../../Utils/clusterConfigUtils";
import { useBlocker } from "react-router-dom";
import Spinner from "../../components/Spinner";
import {
  AutoStartComponent,
  changedRecoveryComponents,
  filterAutoStartComponents,
} from "./autoStartUtils";

type Grouping = {
  serviceName: string;
  displayServiceName: string;
  components: AutoStartComponent[];
};

function ServiceAutoStart() {
  const [serviceComponentsGroups, setServiceComponentsGroups] = useState<
    Grouping[]
  >([]);
  const [serviceComponentsGroupsApi, setServiceComponentsGroupsApi] = useState<
    Grouping[]
  >([]);
  const { clusterName, isNonWizardUser } = useContext(AppContext);
  const [clusterEnvProperties, setClusterEnvProperties] = useState<Record<string, any>>({});
  const [clusterEnvPropertiesApi, setClusterEnvPropertiesApi] = useState<Record<string, any>>({});
  const [showWarningModal, setShowWarningModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Authorization hooks - implementing Ember.js service auto-start authorization patterns
  const { hasAuthorization } = useAuth();
  
  // Check specific authorizations for service auto-start operations
  // Based on Ember.js: App.isAuthorized('CLUSTER.MANAGE_AUTO_START')
  const canManageAutoStart = hasAuthorization('CLUSTER.MANAGE_AUTO_START');
  const canEditAutoStart = canManageAutoStart && !isNonWizardUser;
  const fetchServicesAndComponents = useCallback(async (): Promise<Grouping[]> => {
    const response = await HostsApi.getClusterComponents(
      clusterName,
      "ServiceComponentInfo/component_name,ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/recovery_enabled,ServiceComponentInfo/total_count&minimal_response=true"
    );
    const restartableComponents = filterAutoStartComponents(response.items || []);
    const sortedComponents = sortPropertyLight(
      [...restartableComponents],
      "ServiceComponentInfo.service_name"
    );
    const uniqueServices = uniq(
      map(sortedComponents, "ServiceComponentInfo.service_name")
    ) as string[];

    return uniqueServices.map(function (service: string) {
      const components = cloneDeep(filter(sortedComponents, [
        "ServiceComponentInfo.service_name",
        service,
      ]));
      forEach(components, function (component) {
        component.ServiceComponentInfo.componentDisplayName = role(
          component.ServiceComponentInfo.component_name,
          false
        );
      });
      return {
        serviceName: service,
        displayServiceName: role(service, true),
        components,
      };
    });
  }, [clusterName]);

  const fetchClusterConfigs = useCallback(async () => {
    const tags = await ConfigsApi.updateConfigTags(clusterName);
    const clusterEnvTag = find(tags, ["siteName", "cluster-env"]);
    if (!clusterEnvTag) {
      throw new Error("The current cluster-env configuration could not be found");
    }
    const urlParam = `(type=${clusterEnvTag.siteName}&tag=${clusterEnvTag.tagName})`;
    const allProperties = await ConfigsApi.getConfigsByTags(clusterName, urlParam);
    return get(allProperties, "items.0.properties", []);
  }, [clusterName]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const [groups, envProperties] = await Promise.all([
        fetchServicesAndComponents(),
        fetchClusterConfigs(),
      ]);
      setServiceComponentsGroups(cloneDeep(groups));
      setServiceComponentsGroupsApi(cloneDeep(groups));
      setClusterEnvProperties(cloneDeep(envProperties));
      setClusterEnvPropertiesApi(cloneDeep(envProperties));
    } catch (error: any) {
      setLoadError(
        error?.response?.data?.message
          || error?.message
          || "Auto-start configuration could not be loaded"
      );
    } finally {
      setLoading(false);
    }
  }, [fetchClusterConfigs, fetchServicesAndComponents]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const toggleRecoveryFor = (
    service: string,
    component: string,
    value: string
  ) => {
    const servicesCopy = cloneDeep(serviceComponentsGroups);
    const matchingService = find(servicesCopy, ["serviceName", service]);
    if (matchingService) {
      const matchingComponent: any = find(matchingService.components, [
        "ServiceComponentInfo.component_name",
        component,
      ]);
      if (matchingComponent) {
        matchingComponent.ServiceComponentInfo.recovery_enabled = value;
      }
    }
    setServiceComponentsGroups(servicesCopy);
  };

  const autoStartColumns = [
    {
      header: "",
      id: "autostart",
      cell: ({ row: { original: data } }: any) => {
        const checkboxId = `autostart-${data.ServiceComponentInfo.service_name}-${data.ServiceComponentInfo.component_name}`;
        return (
          <FormCheck
            id={checkboxId}
            checked={
              get(data, "ServiceComponentInfo.recovery_enabled", "false") ===
              "true"
            }
            disabled={!canEditAutoStart}
            onChange={() => {
              if (canEditAutoStart) {
                toggleRecoveryFor(
                  data.ServiceComponentInfo.service_name,
                  data.ServiceComponentInfo.component_name,
                  get(data, "ServiceComponentInfo.recovery_enabled", "false") ===
                    "false"
                    ? "true"
                    : "false"
                );
              }
            }}
          />
        );
      },
    },
  ];
  const componentColumns = [
    {
      header: "",
      id: "name",
      accessorKey: "ServiceComponentInfo.componentDisplayName",
    },
  ];
  const allComponentsChecked = () => {
    let allComponentsChecked = true;
    forEach(serviceComponentsGroups, (service) => {
      forEach(service.components, (component: any) => {
        if (component.ServiceComponentInfo.recovery_enabled === "false") {
          allComponentsChecked = false;
        }
      });
    });
    return allComponentsChecked;
  };
  const toggleAllChecked = () => {
    const servicesCopy = cloneDeep(serviceComponentsGroups);
    forEach(servicesCopy, (service) => {
      forEach(service.components, (component: any) => {
        component.ServiceComponentInfo.recovery_enabled = allComponentsChecked()
          ? "false"
          : "true";
      });
    });
    setServiceComponentsGroups(servicesCopy);
  };
  const columns = [
    {
      header: "Service",
      accessorKey: "displayServiceName",
    },
    {
      header: "Components",
      id: "components",
      cell: ({ row: { original: data } }: any) => {
        return (
          <Table
            columns={componentColumns}
            showHeader={false}
            data={data.components}
          />
        );
      },
    },
    {
      header: () => {
        const selectAllId = "select-all-autostart";
        return (
          <Stack direction="horizontal">
            <FormCheck
              id={selectAllId}
              className="me-2 ms-2"
              disabled={!canEditAutoStart}
              onChange={() => {
                if (canEditAutoStart) {
                  toggleAllChecked();
                }
              }}
              checked={allComponentsChecked()}
              label="Auto Start"
            />
          </Stack>
        );
      },
      id: "auto-start",
      cell: ({ row: { original: data } }: any) => {
        return (
          <Table
            bordered={false}
            //@ts-ignore
            noBorder
            columns={autoStartColumns}
            showHeader={false}
            data={data.components}
          />
        );
      },
    },
  ];
  const isModified =
    JSON.stringify(serviceComponentsGroupsApi) !== JSON.stringify(serviceComponentsGroups)
    || JSON.stringify(clusterEnvProperties) !== JSON.stringify(clusterEnvPropertiesApi);
  const areControlsDisabled = !canEditAutoStart || !isModified || saving;
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    isModified && currentLocation.pathname !== nextLocation.pathname
  );

  useEffect(() => {
    if (blocker.state === "blocked") {
      setShowWarningModal(true);
    }
  }, [blocker]);

  useEffect(() => {
    const warnBeforeUnload = (event: BeforeUnloadEvent) => {
      if (isModified) {
        event.preventDefault();
      }
    };
    window.addEventListener("beforeunload", warnBeforeUnload);
    return () => window.removeEventListener("beforeunload", warnBeforeUnload);
  }, [isModified]);

  const flattenComponents = (groups: Grouping[]) =>
    groups.flatMap((group) => group.components);

  const savePreferences = async (): Promise<boolean> => {
    if (saving || !canEditAutoStart) {
      return false;
    }
    setSaving(true);
    setSaveError(null);
    const desiredGroups = cloneDeep(serviceComponentsGroups);
    const desiredClusterEnv = cloneDeep(clusterEnvProperties);
    const changes = changedRecoveryComponents(
      flattenComponents(desiredGroups),
      flattenComponents(serviceComponentsGroupsApi),
    );
    const requests: Promise<any>[] = [];

    if (changes.enabled.length) {
        const query = `ServiceComponentInfo/component_name.in(${changes.enabled.join(",")})`;
        const requestBody = {
          RequestInfo: {
            query,
          },
          ServiceComponentInfo: {
            recovery_enabled: "true",
          },
        };
        requests.push(componentApi.editComponent(clusterName, requestBody));
    }
    if (changes.disabled.length) {
        const query = `ServiceComponentInfo/component_name.in(${changes.disabled.join(",")})`;
        const requestBody = {
          RequestInfo: {
            query,
          },
          ServiceComponentInfo: {
            recovery_enabled: "false",
          },
        };
        requests.push(componentApi.editComponent(clusterName, requestBody));
    }
    if (JSON.stringify(desiredClusterEnv) !== JSON.stringify(clusterEnvPropertiesApi)) {
      requests.push(safeUpdateClusterEnvConfig(
        clusterName,
        desiredClusterEnv,
        "Updated auto-start configuration"
      ));
    }

    const results = await Promise.allSettled(requests);
    const failedRequests = results.filter((result) => result.status === "rejected");
    try {
      const [serverGroups, serverClusterEnv] = await Promise.all([
        fetchServicesAndComponents(),
        fetchClusterConfigs(),
      ]);
      setServiceComponentsGroupsApi(cloneDeep(serverGroups));
      setClusterEnvPropertiesApi(cloneDeep(serverClusterEnv));
      if (failedRequests.length) {
        setServiceComponentsGroups(desiredGroups);
        setClusterEnvProperties(desiredClusterEnv);
        setSaveError(
          `${failedRequests.length} auto-start update request(s) failed. Server state was refreshed; retry the remaining changes.`
        );
        return false;
      }
      setServiceComponentsGroups(cloneDeep(serverGroups));
      setClusterEnvProperties(cloneDeep(serverClusterEnv));
      setShowWarningModal(false);
      return true;
    } catch (error: any) {
      setSaveError(
        error?.response?.data?.message
          || error?.message
          || "The server state could not be refreshed after saving"
      );
      return false;
    } finally {
      setSaving(false);
    }
  };

  const discardChanges = () => {
    setServiceComponentsGroups(cloneDeep(serviceComponentsGroupsApi));
    setClusterEnvProperties(cloneDeep(clusterEnvPropertiesApi));
    setSaveError(null);
  };

  return (
    <UpgradeGuard>
      <Modal
        modalTitle={blocker.state === "blocked" ? "Warning" : "Save Auto-Start Configuration"}
        modalBody={
          <>
            <div>
              {blocker.state === "blocked"
                ? "You have unsaved changes."
                : "You are changing the auto-start configuration. Click Save to commit the change or Discard to revert your changes."}
            </div>
            {saveError && <Alert variant="danger" className="mt-3 mb-0">{saveError}</Alert>}
          </>
        }
        isOpen={showWarningModal}
        onClose={() => {
          if (blocker.state === "blocked") {
            blocker.reset();
          }
          setShowWarningModal(false);
        }}
        options={{
          okButtonText: "SAVE",
          okButtonDisabled: saving,
          cancelableViaIcon: !saving,
          extraButtons: [
            {
              text: "DISCARD",
              variant: "secondary",
              disabled: saving,
              onClick: () => {
                discardChanges();
                setShowWarningModal(false);
                if (blocker.state === "blocked") {
                  blocker.proceed();
                }
              },
            },
          ],
        }}
        successCallback={async () => {
          const saved = await savePreferences();
          if (saved && blocker.state === "blocked") {
            blocker.proceed();
          }
        }}
      />
      <Card className="h-100 m-4">
        <CardBody>
          <small className="text-muted">
            Ambari services can be configured to start automatically on system
            boot. Each service can be configured to start all components,
            masters and workers, or selectively.
          </small>
          <div className="d-flex align-items-center">
            <h3 style={{ fontSize: 20 }} className="mt-1">
              Auto Start Settings
            </h3>
            <FormCheck
              type="switch"
              className="labelled-switch ms-2"
              id="auto-start"
              disabled={!canEditAutoStart || saving || loading || Boolean(loadError)}
              label={
                get(clusterEnvProperties, "recovery_enabled", "false") ===
                "false"
                  ? "Disabled"
                  : "Enabled"
              }
              onChange={() => {
                if (canEditAutoStart) {
                  setClusterEnvProperties({
                    ...clusterEnvProperties,
                    recovery_enabled:
                      get(clusterEnvProperties, "recovery_enabled", "false") ===
                      "false"
                        ? "true"
                        : "false",
                  });
                }
              }}
              checked={
                //@ts-ignore
                get(clusterEnvProperties, "recovery_enabled", "false") ===
                "true"
              }
            ></FormCheck>
          </div>
          {loadError ? (
            <Alert variant="danger" className="mt-3 d-flex justify-content-between align-items-center">
              <span>{loadError}</span>
              <Button size="sm" variant="outline-danger" onClick={() => void loadData()}>
                Retry
              </Button>
            </Alert>
          ) : loading ? (
            <div className="d-flex justify-content-center p-4"><Spinner /></div>
          ) : (
            <Table columns={columns} data={serviceComponentsGroups}></Table>
          )}
          {saveError && !showWarningModal && <Alert variant="danger" className="mt-3">{saveError}</Alert>}
        </CardBody>
        <CardFooter>
          <Stack direction="horizontal" className="justify-content-end">
            <Button
              size="sm"
              variant="outline-secondary"
              disabled={areControlsDisabled}
              onClick={discardChanges}
            >
              CANCEL
            </Button>
            <Button
              size="sm"
              variant="success"
              className="ms-2"
              disabled={areControlsDisabled}
              onClick={() => setShowWarningModal(true)}
            >
              SAVE
            </Button>
          </Stack>
        </CardFooter>
      </Card>
    </UpgradeGuard>
  );
}

export default ServiceAutoStart;
