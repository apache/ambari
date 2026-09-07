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

import { useContext, useEffect, useRef } from "react";
import { cloneDeep, find, get, isEqual } from "lodash";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { centralizedServiceStateApi } from "../api/centralizedServiceStateApi";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";

export const useYarnConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, parsedSocketMessages } = useContext(AppContext);

  // Early return if YARN service is not installed
  const isYarnInstalled = services && Array.isArray(services) &&
    services.some((service: any) => service.ServiceInfo.service_name === "YARN");

  if (!isYarnInstalled) {
    return;
  }

  // @ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  const hasResourceManagerHAEnabledUseEffectRunOnce = useRef(false);

  const fetchYARNMasterSlaveClientsData = async () => {
    let yarnComponentsData = cachedServiceApi.getServiceComponentData("YARN");
    
    if (!yarnComponentsData) {
      yarnComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "YARN"
      );
    }
    
    return yarnComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "YARN" &&
        get(item, "ServiceComponentInfo.component_name") ===
          componentData.componentName
    );

    if (masterComponent) {
      const hostComponents = get(masterComponent, "host_components");
      if (hostComponents && hostComponents.length > 0) {
        hostComponents.forEach((hostComponent: any) => {
          if (
            get(hostComponent, "HostRoles.component_name") ===
            componentData.componentName
          ) {
            componentData.hostComponents.forEach((host: any) => {
              const polledHostComponentHostName = get(
                hostComponent,
                "HostRoles.host_name"
              );
              const componentDataHostName = get(host, "HostRoles.host_name");
              if (componentDataHostName === polledHostComponentHostName) {
                host.state = get(hostComponent, "HostRoles.state");
                host.passiveState = get(
                  hostComponent,
                  "HostRoles.maintenance_state"
                );
                if (componentData.componentName === "RESOURCEMANAGER") {
                  //check if ha is enabled
                  if (allServiceModels["yarn"].isRMHAEnabled) {
                    const haState = get(hostComponent, "HostRoles.ha_state");
                    // Format the HA status to match Ember display (e.g., "ACTIVE" -> "ACTIVE")
                    host.haStatus = haState ? haState.toUpperCase() : "";
                  } else {
                    // For non-HA mode, don't show any HA status
                    host.haStatus = "";
                  }
                }
              }
            });
          }
        });
      }
    }
    return componentData;
  };
  const findMasterSlaveClientComponents = async () => {
    const items = await fetchYARNMasterSlaveClientsData();

    if (!allServiceModels["yarn"]) {
      return;
    }

    if (!items || items.length === 0) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["yarn"]);
    let masterComponents: any[] = [];
    let slaveComponents: any[] = [];
    let clientComponents: any[] = [];

    items?.forEach((item: any) => {
      let componentData = {
        componentName: get(item, "ServiceComponentInfo.component_name"),
        displayName: get(item, "ServiceComponentInfo.display_name"),
        category: get(item, "ServiceComponentInfo.category"),
        installedCount: get(item, "ServiceComponentInfo.installed_count"),
        startedCount: get(item, "ServiceComponentInfo.started_count"),
        totalCount: get(item, "ServiceComponentInfo.total_count"),
        hostComponents: get(item, "host_components"),
      };

      if (componentData.category === Categories.MASTER) {
        const masterComponentDataWithState =
          updateComponentObjectForSelectMaster(componentData);
        masterComponents.push(masterComponentDataWithState);
      } else if (componentData.category === Categories.SLAVE) {
        slaveComponents.push(componentData);
      } else if (componentData.category === Categories.CLIENT) {
        const yarnClientsInstalled = componentData.installedCount;
        currentConfig[ServiceComponentFields.YARN.yarnClients] =
          yarnClientsInstalled;
        clientComponents.push(componentData);
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentFields.YARN.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentFields.YARN.slaveComponents] =
      slaveComponents;
    currentConfig[ServiceComponentFields.YARN.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["yarn"], currentConfig)) {
      allServiceModels["yarn"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const parseWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
        (message) => message.destination === "/events/hostcomponents"
      );
    }

    if (
      latestHostOperationMessage &&
      latestHostOperationMessage.hostComponents
    ) {
      for (const hostComponent of latestHostOperationMessage.hostComponents) {
        if (hostComponent.currentState in componentFinishStates) {
          await findMasterSlaveClientComponents();
        }
      }
    }
  };

  const updateYARNMasterComponents = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const updates = cloneDeep(allServiceModels["yarn"]);
    let activeResourceManagers = [] as any;
    let standbyResourceManagers = [] as any;
    let nonActiveStandbyResourceManagers = [] as any;
    let isResourceManagerHaEnabled = allServiceModels["yarn"].isRMHAEnabled;
    let resourceManager = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "YARN" &&
        get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
    );
    if (resourceManager && isResourceManagerHaEnabled) {
      resourceManager.host_components.forEach((hostComponent: any) => {
        if (
          get(hostComponent, "HostRoles.component_name") === "RESOURCEMANAGER"
        ) {
          const hostComponentData = {
            componentName: "RESOURCEMANAGER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            haStatus: get(hostComponent, "HostRoles.ha_state"),
            state: get(hostComponent, "HostRoles.state"),
          };
          if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.haStatus &&
            hostComponentData.haStatus.toUpperCase() === "ACTIVE"
          ) {
            activeResourceManagers.push(hostComponentData);
            return;
          } else if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.haStatus &&
            hostComponentData.haStatus.toUpperCase() === "STANDBY"
          ) {
            standbyResourceManagers.push(hostComponentData);
            return;
          }
          nonActiveStandbyResourceManagers.push(hostComponentData);
        }
      });
    } else if (resourceManager && !isResourceManagerHaEnabled) {
      resourceManager.host_components.forEach((hostComponent: any) => {
        if (
          get(hostComponent, "HostRoles.component_name") === "RESOURCEMANAGER"
        ) {
          const hostComponentData = {
            componentName: "RESOURCEMANAGER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          updates[ServiceComponentFields.YARN["resourceManager"]] =
            hostComponentData;
          nonActiveStandbyResourceManagers.push(hostComponentData);
        }
      });
    }
    updates[ServiceComponentFields.YARN["activeResourceManagers"]] =
      activeResourceManagers;
    updates[ServiceComponentFields.YARN["standbyResourceManagers"]] =
      standbyResourceManagers;
    updates[
      ServiceComponentFields.YARN["nonActiveStandbyResourceManagers"]
    ] = nonActiveStandbyResourceManagers;

    // Only update if we have changes
    if (!isEqual(allServiceModels["yarn"], updates)) {
      allServiceModels["yarn"].updateConfig(updates);
      updateRegistry(allServiceModels);
    }
  };

  //@ts-ignore
  const isRMAEnabled = async () => {
    const yarnComponentsData = cachedServiceApi.getServiceComponentData("YARN");
    
    if (!yarnComponentsData) {
      return;
    }

    const resourceManager = yarnComponentsData.find((item: any) =>
      item.ServiceComponentInfo?.component_name === "RESOURCEMANAGER"
    );

    if (resourceManager && resourceManager.host_components && resourceManager.host_components.length > 1) {
      const updates = {
        [ServiceComponentFields.YARN["isRMHAEnabled"]]: true,
      };

      if (
        !isEqual(updates, {
          isRMHAEnabled:
            allServiceModels["yarn"][
              ServiceComponentFields.YARN["isRMHAEnabled"]
            ],
        })
      ) {
        allServiceModels["yarn"].updateConfig(updates);
        updateRegistry(allServiceModels);
      }
    }
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["yarn"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["yarn"], configToBeUpdated)) {
      allServiceModels["yarn"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    const currentConfig = cloneDeep(allServiceModels["yarn"]);
    const serviceName = "YARN";
    
    // Use centralized service state API instead of individual call
    const serviceStateData = centralizedServiceStateApi.getServiceStateData(serviceName);
    
    if (!serviceStateData) return;

    const { alertsCount, hasCriticalAlerts, state } = serviceStateData;

    if (!alertsCount && alertsCount !== 0) return;

    currentConfig[ServiceComponentFields.YARN.hasCriticalAlerts] = hasCriticalAlerts;
    currentConfig[ServiceComponentFields.YARN.alertsCount] = alertsCount;
    currentConfig[ServiceComponentFields.YARN.state] = state;

    if (!isEqual(allServiceModels["yarn"], currentConfig)) {
      allServiceModels["yarn"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "YARN"
      );
    }
    if (
        latestHostOperationMessage &&
        (componentFinishStates.includes(latestHostOperationMessage.state)
        || (latestHostOperationMessage.maintenance_state && maintenanceStates.includes(latestHostOperationMessage.maintenance_state)))
    ) {
      await updateServiceMaintenanceState(latestHostOperationMessage.maintenance_state);
      await updateAlertsAndServiceStateData();
    }
  };

  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      updateYARNMasterComponents();
      findMasterSlaveClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    if (
      hasResourceManagerHAEnabledUseEffectRunOnce.current ||
      !allServiceModels["yarn"]
    ) {
      return;
    }
    isRMAEnabled();
    hasResourceManagerHAEnabledUseEffectRunOnce.current = true;
  }, [allServiceModels]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
