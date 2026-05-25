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

import { useContext, useEffect } from "react";
import { cloneDeep, find, get, isEmpty, isEqual } from "lodash";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";

export const useZkConfigUpdater = () => {
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if ZOOKEEPER service is not installed
  const isZookeeperInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "ZOOKEEPER");
  
  if (!isZookeeperInstalled) {
    return;
  }
  
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const fetchZkMasterSlaveClientsData = async () => {
    let zkComponentsData = cachedServiceApi.getServiceComponentData("ZOOKEEPER");
    
    if (!zkComponentsData) {
      zkComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "ZOOKEEPER"
      );
    }
    
    return zkComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "ZOOKEEPER" &&
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
  const findZkMasterClientComponents = async () => {
    const items = await fetchZkMasterSlaveClientsData();

    if (!allServiceModels["zk"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["zk"]);
    let masterComponents: any[] = [];
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
      } else {
        const zkClientsInstalled = componentData.installedCount;
        currentConfig[
          ServiceComponentMetricsEnums.ZOOKEEPER.zkClientsInstalled
        ] = zkClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.ZOOKEEPER.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.ZOOKEEPER.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["zk"], currentConfig)) {
      allServiceModels["zk"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["zk"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["zk"], configToBeUpdated)) {
      allServiceModels["zk"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("ZOOKEEPER", "zk", allServiceModels, updateRegistry);
  };

  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "ZOOKEEPER"
      );
    }
    if (
        latestHostOperationMessage &&
        componentFinishStates.includes(latestHostOperationMessage.state)
        || (latestHostOperationMessage.maintenance_state && maintenanceStates.includes(latestHostOperationMessage.maintenance_state))
    ) {
      await updateServiceMaintenanceState(latestHostOperationMessage.maintenance_state);
      await updateAlertsAndServiceStateData();
    }
  };

  useEffect(() => {
    findZkMasterClientComponents();
  }, []);

  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      findZkMasterClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    if (!isEmpty(parsedSocketMessages)) {
      parseAlertsWebSocketMessages();
    }
  }, [parsedSocketMessages]);
};
