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

import { useContext, useEffect} from "react";
import { cloneDeep, find, get, isEqual } from "lodash";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { centralizedServiceStateApi } from "../api/centralizedServiceStateApi";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { componentFinishStates, maintenanceStates } from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";

export const usePinotConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if PINOT service is not installed
  const isPinotInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "PINOT");
  
  if (!isPinotInstalled) {
    return;
  }

  // @ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const fetchPinotMasterSlaveClientsData = async () => {
    let pinotComponentsData = cachedServiceApi.getServiceComponentData("PINOT");
    
    if (!pinotComponentsData) {
      pinotComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "PINOT"
      );
    }
    
    return pinotComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "PINOT" &&
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
              }
            });
          }
        });
      }
    }
    return componentData;
  };

  const findMasterSlaveClientComponents = async () => {
    const items = await fetchPinotMasterSlaveClientsData();

    if (!allServiceModels["pinot"]) {
      return;
    }

    if (!items || items.length === 0) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["pinot"]);
    let masterComponents: any[] = [];
    let slaveComponents: any[] = [];

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
        
        // Update specific component counts
        if (componentData.componentName === "PINOT_BROKER") {
          currentConfig[ServiceComponentFields.PINOT.pinotBrokerStartedCount] = componentData.startedCount;
          currentConfig[ServiceComponentFields.PINOT.pinotBrokerInstalledCount] = componentData.installedCount;
          currentConfig[ServiceComponentFields.PINOT.pinotBrokerTotalCount] = componentData.totalCount;
        } else if (componentData.componentName === "PINOT_MINION") {
          currentConfig[ServiceComponentFields.PINOT.pinotMinionStartedCount] = componentData.startedCount;
          currentConfig[ServiceComponentFields.PINOT.pinotMinionInstalledCount] = componentData.installedCount;
          currentConfig[ServiceComponentFields.PINOT.pinotMinionTotalCount] = componentData.totalCount;
        } else if (componentData.componentName === "PINOT_SERVER") {
          currentConfig[ServiceComponentFields.PINOT.pinotServerStartedCount] = componentData.startedCount;
          currentConfig[ServiceComponentFields.PINOT.pinotServerInstalledCount] = componentData.installedCount;
          currentConfig[ServiceComponentFields.PINOT.pinotServerTotalCount] = componentData.totalCount;
        }
      }
    });

    currentConfig[ServiceComponentFields.PINOT.masterComponents] = masterComponents;
    currentConfig[ServiceComponentFields.PINOT.slaveComponents] = slaveComponents;

    if (!isEqual(allServiceModels["pinot"], currentConfig)) {
      allServiceModels["pinot"].updateConfig(currentConfig);
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

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["pinot"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/OFF

    if (!isEqual(allServiceModels["pinot"], configToBeUpdated)) {
      allServiceModels["pinot"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    const currentConfig = cloneDeep(allServiceModels["pinot"]);
    const serviceName = "PINOT";
    
    // Use centralized service state API instead of individual call
    const serviceStateData = centralizedServiceStateApi.getServiceStateData(serviceName);
    
    if (!serviceStateData) return;

    const { alertsCount, hasCriticalAlerts, state } = serviceStateData;

    if (!alertsCount && alertsCount !== 0) return;

    currentConfig[ServiceComponentFields.PINOT.hasCriticalAlerts] = hasCriticalAlerts;
    currentConfig[ServiceComponentFields.PINOT.alertsCount] = alertsCount;
    currentConfig[ServiceComponentFields.PINOT.state] = state;

    if (!isEqual(allServiceModels["pinot"], currentConfig)) {
      allServiceModels["pinot"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "PINOT"
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
      findMasterSlaveClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
