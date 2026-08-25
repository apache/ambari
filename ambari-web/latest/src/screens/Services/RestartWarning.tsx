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

import { faRefresh } from "@fortawesome/free-solid-svg-icons";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useContext, useEffect, useState } from "react";
import { Alert, Dropdown } from "react-bootstrap";
import { groupPropertyValues } from "../../Utils/dataUtils";
import { filter, flatten, forEach, map, set, uniq } from "lodash";
import modalManager from "../../store/ModalManager";
import { AppContext } from "../../store/context";
import { HostsApi } from "../../api/hostsApi";
import { useCopyToClipboard } from "../../hooks/useCopyToClipboard";
import BackgroundOperations from "../BackgroundOperations";
import { RequestApi } from "../../api/requestApi";
import useAuthorizationPolicy from "../../hooks/useAuthorizationPolicy";
import { showRollingRestartPopup } from "../Hosts/batchUtils";
import { ServiceContext } from "../../store/ServiceContext";


type RestartWarningProps = {
  serviceName: string;
};
const serviceSpecificComponents: any = {
  HDFS: ["DataNode"],
  YARN: ["NodeManager"],
  HBASE: ["RegionServer"],
};

//Map component names to their service names
const componentToServiceMapping: { [key: string]: string } = {
  DataNode: "HDFS",
  DATANODE: "HDFS",
  NodeManager: "YARN",
  NODEMANAGER: "YARN",
  RegionServer: "HBASE",
  HBASE_REGIONSERVER: "HBASE",
};

function RestartWarning({ serviceName }: RestartWarningProps) {
  const [restartHosts, setRestartHosts] = useState<string[]>([]);
  const [restartComponents, setRestartComponents] = useState<any[]>([]);
  const [componentsInRestartState, setComponentsInRestartState] = useState<any>(
    []
  );
  const [, copy] = useCopyToClipboard();
  const { clusterName } = useContext(AppContext);
  const { polledHostComponentsData } = useContext(ServiceContext);

  const { isAuthorized } = useAuthorizationPolicy();
  const canStartStopServices = isAuthorized("SERVICE.START_STOP");

  // Assume that the components in serviceSpecificComponents all support rolling restart
  const [_rollingRestartSupportedComponents] = useState<{
    [key: string]: boolean;
  }>({
    DataNode: true,
    NodeManager: true,
    RegionServer: true,
  });

  // Reset state when serviceName changes to prevent stale data
  useEffect(() => {
    setRestartHosts([]);
    setRestartComponents([]);
    setComponentsInRestartState([]);
  }, [serviceName]);

  // Reuse centralized polled data from CachedServiceApi instead of making a separate API call
  // CachedServiceApi already polls /components/ with all required fields (including stale_configs)
  useEffect(() => {
    if (!polledHostComponentsData?.items || !serviceName) {
      return;
    }

    const response = polledHostComponentsData;

    for (const component of response.items) {
      forEach(component.host_components, (comp: any) => {
        set(
          comp.HostRoles,
          "serviceName",
          component.ServiceComponentInfo.service_name
        );
      });
    }
    const allHostComponentsWithHostRoles = flatten(
      response?.items?.map((host: any) => host.host_components)
    );
    const allHostComponents = flatten(
      map(allHostComponentsWithHostRoles, "HostRoles")
    );
    const serviceComponents = filter(allHostComponents, [
      "serviceName",
      serviceName,
    ]);
    const componentsWithStaleConfigs = filter(serviceComponents, [
      "stale_configs",
      true,
    ]);
    setComponentsInRestartState(
      groupPropertyValues(componentsWithStaleConfigs, "host_name")
    );
  }, [polledHostComponentsData, serviceName]);

  useEffect(() => {
    const affectedHosts = [];
    const affectedComponents = [];
    for (const hostName in componentsInRestartState) {
      if (componentsInRestartState[hostName].length) {
        affectedHosts.push(hostName);
        affectedComponents.push(...componentsInRestartState[hostName]);
      }
    }
    setRestartHosts(affectedHosts);
    setRestartComponents(affectedComponents);
  }, [componentsInRestartState]);

  function showRestartHostDetailsModal() {
    modalManager.show({
      modalTitle: "Hosts Requiring Restart",
      onClose: () => modalManager.hide(),
      successCallback: () => {
        modalManager.hide();
      },
      modalBody: (
        <>
          <div
            className="custom-link"
            onClick={async () => {
              await copy(restartHosts.join(", "));
            }}
          >
            Copy
          </div>
          <div className="mt-2">{restartHosts.join(", ")}</div>
        </>
      ),
      options: {
        cancelableViaBtn: false,
        cancelableViaIcon: true,
      },
    });
  }

  function showRestartComponentsDetailsModal() {
    const componentCount = groupPropertyValues(
      restartComponents,
      "display_name"
    );
    const restartDetailsString = Object.keys(componentCount)
      .map((component) => {
        return `${componentCount[component].length} ${component}`;
      })
      .join(", ");
    modalManager.show({
      modalTitle: "Components Requiring Restart",
      onClose: () => modalManager.hide(),
      successCallback: () => {
        modalManager.hide();
      },
      modalBody: (
        <>
          <div
            className="custom-link"
            onClick={async () => {
              await copy(restartDetailsString);
            }}
          >
            Copy
          </div>
          <div className="mt-2">{restartDetailsString}</div>
        </>
      ),
      options: {
        cancelableViaBtn: false,
        cancelableViaIcon: true,
      },
    });
  }

  const restartComponent = async (componentName: string) => {
    // Capture current values to prevent stale closure issues
    const currentServiceName = serviceName;
    const currentClusterName = clusterName;
    
    if (!currentServiceName || !currentClusterName) {
      return;
    }
    
    // Check if the component supports rolling restart
    if (_rollingRestartSupportedComponents[componentName]) {
      // Map of display names to component names
      const componentNameMapping: { [key: string]: string } = {
        DataNode: "DATANODE",
        NodeManager: "NODEMANAGER",
        RegionServer: "HBASE_REGIONSERVER",
      };

      // The actual component name in the API might be different (e.g., "DATANODE" instead of "DataNode")
      const apiComponentName =
        componentNameMapping[componentName] || componentName;

      try {
        // Get host components details from the API
        const fields =
          "fields=Hosts/host_name,host_components/HostRoles/component_name,host_components/HostRoles/stale_configs,host_components/HostRoles/maintenance_state";
        const hostDetailsResponse = await HostsApi.getHostComponentsDetails(
          currentClusterName,
          fields
        );

        // Filter components that match our component name
        let componentDetails: any[] = [];
        hostDetailsResponse.items.forEach((host: any) => {
          // Get the host name from the host object
          const hostName = host.Hosts?.host_name;

          // Filter components on this host that match our target component
          const componentsOnHosts = host.host_components.filter(
            (component: any) =>
              component.HostRoles.component_name === apiComponentName
          );

          // If we found matching components, ensure they have the host name
          if (componentsOnHosts.length > 0) {
            componentsOnHosts.forEach((component: any) => {
              // Ensure the host_name is set correctly
              component.HostRoles.host_name = component.HostRoles.host_name || hostName;
            });
            componentDetails.push(...componentsOnHosts);
          }
        });

        // Get the correct service name for this component
        const componentServiceName =
          componentToServiceMapping[componentName] ||
          componentToServiceMapping[apiComponentName] ||
          currentServiceName;

        // Format the components for the RollingRestartModal
        const hostComponents = componentDetails.map((component: any) => ({
          componentName: apiComponentName,
          hostName: component.HostRoles.host_name, // This is the critical field
          serviceName: componentServiceName,
          staleConfigs: component.HostRoles.stale_configs === true,
          passiveState: component.HostRoles.maintenance_state || "OFF",
        }));

        // Add a check to ensure we have host components with valid host names
        if (hostComponents.length === 0 || !hostComponents.some(comp => comp.hostName)) {
          console.error("No valid host components found for", apiComponentName, "on service", currentServiceName);
          confirmRestartAll();
          return;
        }

        // Check if any components have stale configs
        const hasAnyStaleConfigs = hostComponents.some(
          (comp) => comp.staleConfigs
        );

        // Use the showRollingRestartPopup function with the correct service name
        showRollingRestartPopup(
          apiComponentName,
          componentServiceName,
          false, // maintenance mode - we don't have this info in RestartWarning
          hasAnyStaleConfigs,
          hostComponents,
          currentClusterName
        );
      } catch (error) {
        console.error("Error fetching host components for", componentName, "on service", currentServiceName, ":", error);
        // If there's an error, fall back to the default restart all behavior
        confirmRestartAll();
      }
    } else {
      // For components that don't support rolling restart, use the default restart all behavior
      confirmRestartAll();
    }
  };

  function confirmRestartAll() {
    // Capture current serviceName to prevent stale closure issues
    const currentServiceName = serviceName;
    const currentClusterName = clusterName;
    const currentComponentsInRestartState = componentsInRestartState;
    
    if (!currentServiceName || !currentClusterName) {
      console.error("Missing serviceName or clusterName for restart operation");
      return;
    }
    
    modalManager.show({
      modalTitle: "Confirmation",
      onClose: () => modalManager.hide(),
      successCallback: async () => {
        try {
          
          await HostsApi.getHostsList(
            currentClusterName,
            `host_components/HostRoles/component_name&minimal_response=true`,
            {
              RequestInfo: {
                query: `host_components/HostRoles/service_name.in(${currentServiceName})&host_components/HostRoles/stale_configs=true&Hosts/maintenance_state=OFF`,
              },
            }
          );
          
          const componentsToRestart = flatten(
            Object.values(currentComponentsInRestartState)
          );
          
          if (componentsToRestart.length === 0) {
            modalManager.hide();
            return;
          }
          
          const uniqueComponents = uniq(
            map(componentsToRestart, "component_name")
          );
          const componentsWithAllHosts = [];
          
          for (const component of uniqueComponents) {
            const hosts = filter(componentsToRestart, [
              "component_name",
              component,
            ]);
            
            if (hosts.length > 0) {
              componentsWithAllHosts.push({
                component_name: component,
                hosts: hosts.map((host: any) => host.host_name),
              });
            }
          }
          
          if (componentsWithAllHosts.length === 0) {
            console.error("No valid components with hosts found for service:", currentServiceName);
            modalManager.hide();
            return;
          }
          
          
          await RequestApi.postRequest(currentClusterName, {
            RequestInfo: {
              command: "RESTART",
              context: `Restart all components with Stale Configs for ${currentServiceName}`,
              operation_level: {
                level: "SERVICE",
                cluster_name: currentClusterName,
                service_name: currentServiceName,
              },
            },
            "Requests/resource_filters": componentsWithAllHosts.map(
              (component: any) => {
                return {
                  service_name: currentServiceName,
                  hosts: component.hosts.join(","),
                  component_name: component.component_name,
                };
              }
            ),
          });
          
          modalManager.hide();
          modalManager.show(
            <BackgroundOperations
              isOpen={true}
              onClose={() => {
                modalManager.hide();
              }}
              clusterName={currentClusterName}
            />
          );
        } catch (error) {
          console.error("Error during restart operation for service:", currentServiceName, error);
          modalManager.hide();
          // Optionally show error modal
        }
      },
      modalBody: (
        <>
          <div className="fs-14">You are about to restart {currentServiceName}</div>
          <div className="mt-2">
            <Alert variant="warning">
              <div className="fs-12">
                This will trigger alerts as the service is restarted. To
                suppress alerts, turn on Maintenance Mode for {currentServiceName} prior to
                running restart all
              </div>
            </Alert>
          </div>
        </>
      ),
      options: {
        cancelableViaBtn: true,
        okButtonText: "CONFIRM RESTART ALL",
        cancelableViaIcon: true,
      },
    });
  }

  if (!restartHosts.length) {
    return null;
  }

  return (
    <>
      <Alert
        variant="warning"
        className="d-flex justify-content-between align-items-center p-3"
      >
        <div className="d-flex align-items-center justify-content-between">
          <div className="d-flex">
            <FontAwesomeIcon icon={faRefresh} className="me-2" />
            <div className="fw-bold">Restart Required: </div>
            <div
              className="custom-link d-flex mx-1"
              onClick={showRestartComponentsDetailsModal}
            >
              {restartComponents.length} components
            </div>{" "}
            on{" "}
            <div
              className="custom-link d-flex mx-1"
              onClick={showRestartHostDetailsModal}
            >
              {restartHosts.length} hosts
            </div>
          </div>
          {/* Restart Dropdown - Requires SERVICE.START_STOP authorization */}
          {canStartStopServices && (
            <Dropdown>
              <Dropdown.Toggle variant="warning" size="sm" className="text-white">
                RESTART
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <Dropdown.Item onClick={confirmRestartAll}>
                  Restart all affected
                </Dropdown.Item>
                {serviceSpecificComponents[serviceName]?.map((component: any) => (
                  <Dropdown.Item
                    key={component}
                    onClick={() => restartComponent(component)}
                  >
                    Restart {component}
                  </Dropdown.Item>
                ))}
              </Dropdown.Menu>
            </Dropdown>
          )}
        </div>
      </Alert>
    </>
  );
}

export default RestartWarning;
