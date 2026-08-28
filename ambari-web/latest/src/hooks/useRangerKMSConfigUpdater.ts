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
import { cloneDeep, find, get, isEqual } from "lodash";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";

export const useRangerKMSConfigUpdater = () => {
  const { parsedSocketMessages } = useContext(AppContext);
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  const { services} = useContext(AppContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const isRangerKMSInstalled = services && Array.isArray(services) &&
    services.some((service: any) => service.ServiceInfo.service_name === "RANGER_KMS");

  if (!isRangerKMSInstalled) {
    return;
  }

  const fetchRangerKMSMasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let rangerKMSComponentsData = cachedServiceApi.getServiceComponentData("RANGER_KMS");
    
    if (!rangerKMSComponentsData) {
      rangerKMSComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "RANGER_KMS"
      );
    } else {
    }
    
    return rangerKMSComponentsData;
  };
  

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "RANGER_KMS" &&
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

  const findRangerKMSMasterComponents = async () => {
    const items = await fetchRangerKMSMasterSlaveClientsData();

    if (!allServiceModels["ranger_kms"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["ranger_kms"]);
    let masterComponents: any[] = [];

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
        if (componentData.componentName === "RANGER_KMS_SERVER") {
          const masterComponentDataWithState =
            updateComponentObjectForSelectMaster(componentData);
          masterComponents.push(masterComponentDataWithState);
        }
      }
    });

    currentConfig[ServiceComponentFields.RANGER_KMS.masterComponents] =
      masterComponents;

    if (!isEqual(allServiceModels["ranger_kms"], currentConfig)) {
      allServiceModels["ranger_kms"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //usePolling(pollServiceComponentInfoApi, 3000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["ranger_kms"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["ranger_kms"], configToBeUpdated)) {
      allServiceModels["ranger_kms"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("RANGER_KMS", "ranger_kms", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "RANGER_KMS"
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
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      findRangerKMSMasterComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findRangerKMSMasterComponents();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);
};
