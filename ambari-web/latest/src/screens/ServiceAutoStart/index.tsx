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

import { useContext, useEffect, useState } from "react";
import { HostsApi } from "../../api/hostsApi";
import { cloneDeep, filter, find, forEach, get, map, uniq } from "lodash";
import { updatedDiff } from "deep-object-diff";
import {
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
type ServiceComponentInfo = {
  category: string;
  component_name: string;
  recovery_enabled: string;
  service_name: string;
  total_count: number;
};
type Grouping = { serviceName: string; components: ServiceComponentInfo[] };

function ServiceAutoStart() {
  const [serviceComponentsGroups, setServiceComponentsGroups] = useState<
    Grouping[]
  >([]);
  const [serviceComponentsGroupsApi, setServiceComponentsGroupsApi] = useState<
    Grouping[]
  >([]);
  const {clusterName}=useContext(AppContext);
  const [clusterEnvProperties, setClusterEnvProperties] = useState({});
  const [clusterEnvPropertiesApi, setClusterEnvPropertiesApi] = useState({});
  const [showWarningModal, setShowWarningModal] = useState(false);

  // Authorization hooks - implementing Ember.js service auto-start authorization patterns
  const { hasAuthorization } = useAuth();
  
  // Check specific authorizations for service auto-start operations
  // Based on Ember.js: App.isAuthorized('CLUSTER.MANAGE_AUTO_START')
  const canManageAutoStart = hasAuthorization('CLUSTER.MANAGE_AUTO_START');
  async function getSetServicesAndComponents() {
    try {
      const servicesAndComponentsResponse = await HostsApi.getClusterComponents(
        clusterName,
        "ServiceComponentInfo/component_name,ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/recovery_enabled,ServiceComponentInfo/total_count&minimal_response=true"
      );
      const servicesAndComponentsItems = sortPropertyLight(
        servicesAndComponentsResponse.items,
        "ServiceComponentInfo.service_name"
      );
      const servicesAndComponents = {
        ...servicesAndComponentsResponse,
        items: servicesAndComponentsItems,
      };
      const uniqueServices = uniq(
        map(servicesAndComponents.items, "ServiceComponentInfo.service_name")
      );
      const serviceComponents: any[] = map(
        uniqueServices,
        function (service: string) {
          const components = filter(servicesAndComponents.items, [
            "ServiceComponentInfo.service_name",
            service,
          ]);
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
        }
      );
      console.log("Service Components is", serviceComponents);
      setServiceComponentsGroups(serviceComponents);
      setServiceComponentsGroupsApi(serviceComponents);
    } catch (err: any) {
      console.error("Couldn't get services and it's components", err?.message);
    }
  }

  const getConfigBySites = async (tags: any[]) => {
    let urlParams: string[] = [];
    tags.forEach(function (_tag: any) {
      urlParams.push("(type=" + _tag.siteName + "&tag=" + _tag.tagName + ")");
    });
    const allProperties = await ConfigsApi.getConfigsByTags(
      clusterName,
      urlParams.join("|") 
    );
    return get(allProperties, "items.0.properties", []);
  };

  const getClusterConfigs = async () => {
    const tags = await ConfigsApi.updateConfigTags(clusterName);
    const envProperties = await getConfigBySites([
      find(tags, ["siteName", "cluster-env"]),
    ]);
    setClusterEnvProperties(envProperties);
    setClusterEnvPropertiesApi(envProperties);
  };
  useEffect(() => {
    getSetServicesAndComponents();
    getClusterConfigs();
  }, []);

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
            disabled={!canManageAutoStart}
            onChange={() => {
              if (canManageAutoStart) {
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
              disabled={!canManageAutoStart}
              onChange={() => {
                if (canManageAutoStart) {
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
  const areControlsDisabled = () => {
    return (
      !canManageAutoStart ||
      (JSON.stringify(serviceComponentsGroupsApi) ===
        JSON.stringify(serviceComponentsGroups) &&
      JSON.stringify(clusterEnvProperties) ===
        JSON.stringify(clusterEnvPropertiesApi))
    );
  };
  const savePreferences = async () => {
    if (
      JSON.stringify(serviceComponentsGroupsApi) !==
      JSON.stringify(serviceComponentsGroups)
    ) {
      const preferencesDiff = updatedDiff(
        serviceComponentsGroups,
        serviceComponentsGroupsApi
      );
      const changedComponentsArr: any = [];
      forEach(preferencesDiff, (value: any, key: any) => {
        const changedComponents = value?.components;
        forEach(
          changedComponents,
          (_changedComponentValue: any, changedComponentKey: any) => {
            const component =
              serviceComponentsGroups[key]?.components[changedComponentKey];
            changedComponentsArr.push(component);
          }
        );
      });
      const trueRecoveryMode = filter(changedComponentsArr, [
        "ServiceComponentInfo.recovery_enabled",
        "true",
      ]);
      const falseRecoveryMode = filter(changedComponentsArr, [
        "ServiceComponentInfo.recovery_enabled",
        "false",
      ]);
      if (trueRecoveryMode.length) {
        const query = `ServiceComponentInfo/component_name.in(${map(
          trueRecoveryMode,
          "ServiceComponentInfo.component_name"
        ).join(",")})`;
        const requestBody = {
          RequestInfo: {
            query,
          },
          ServiceComponentInfo: {
            recovery_enabled: "true",
          },
        };
        await componentApi.editComponent(clusterName, requestBody);
      }
      if (falseRecoveryMode.length) {
        const query = `ServiceComponentInfo/component_name.in(${map(
          falseRecoveryMode,
          "ServiceComponentInfo.component_name"
        ).join(",")})`;
        const requestBody = {
          RequestInfo: {
            query,
          },
          ServiceComponentInfo: {
            recovery_enabled: "false",
          },
        };
        await componentApi.editComponent(clusterName, requestBody);
      }
    }
    if (
      JSON.stringify(clusterEnvProperties) !==
      JSON.stringify(clusterEnvPropertiesApi)
    ) {
      // Use safe update function to preserve all existing cluster-env properties
      await safeUpdateClusterEnvConfig(
        clusterName,
        clusterEnvProperties,
        "Updated auto-start configuration"
      );
    }
    await getSetServicesAndComponents();
    await getClusterConfigs();
    setShowWarningModal(false);
  };
  return (
    <UpgradeGuard>
      <Modal
        modalTitle="Save Auto-Start Configuration"
        modalBody="You are changing the auto-start configuration.Click Save to commit the change or Discard to revert your changes"
        isOpen={showWarningModal}
        onClose={() => {
          setShowWarningModal(false);
        }}
        options={{
          okButtonText: "SAVE",
          extraButtons: [
            {
              text: "DISCARD",
              variant: "secondary",
              onClick: () => {
                setServiceComponentsGroups(
                  cloneDeep(serviceComponentsGroupsApi)
                );
                setClusterEnvProperties(cloneDeep(clusterEnvPropertiesApi));
                setShowWarningModal(false);
              },
            },
          ],
        }}
        successCallback={() => {
          savePreferences();
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
              disabled={!canManageAutoStart}
              label={
                get(clusterEnvProperties, "recovery_enabled", "false") ===
                "false"
                  ? "Disabled"
                  : "Enabled"
              }
              onChange={() => {
                if (canManageAutoStart) {
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
          <Table columns={columns} data={serviceComponentsGroups}></Table>
        </CardBody>
        <CardFooter>
          {/* <Button> */}
          <Stack direction="horizontal" className="justify-content-end">
            <Button
              size="sm"
              variant="outline-secondary"
              disabled={areControlsDisabled()}
              onClick={() => {
                setServiceComponentsGroups(
                  cloneDeep(serviceComponentsGroupsApi)
                );
                setClusterEnvProperties(cloneDeep(clusterEnvPropertiesApi));
              }}
            >
              CANCEL
            </Button>
            <Button
              size="sm"
              variant="success"
              className="ms-2"
              disabled={areControlsDisabled()}
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
