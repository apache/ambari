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
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";

export const useSSMConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  const { services } = useContext(AppContext);
  const { parsedSocketMessages } = useContext(AppContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];

  const isSSMInstalled = services && Array.isArray(services) &&
    services.some((service: any) => service.ServiceInfo.service_name === "SSM");

  if (!isSSMInstalled) {
    return;
  }

  const fetchSSMMasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let ssmComponentsData = cachedServiceApi.getServiceComponentData("SSM");
    
    if (!ssmComponentsData) {
      ssmComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "SSM"
      );
    } else {
    }
    
    return ssmComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "SSM" &&
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
                host.is_active_ssm_ip = get(
                  hostComponent,
                  "processes[0].HostComponentProcess.active_ssm_ip"
                );
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

  const findMasterSlaveComponents = async () => {
    const items = await fetchSSMMasterSlaveClientsData();

    if (!allServiceModels["ssm"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["ssm"]);
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
        if (componentData.componentName === "SSM_SERVER") {
          const masterComponentDataWithState =
            updateComponentObjectForSelectMaster(componentData);
          masterComponents.push(masterComponentDataWithState);
        }
      } else if (componentData.category === Categories.SLAVE) {
        slaveComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.SSM.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.SSM.slaveComponents] =
      slaveComponents;

    if (!isEqual(allServiceModels["ssm"], currentConfig)) {
      allServiceModels["ssm"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //check for updating metrics monitor
  //@ts-ignore
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
          //await updateRangerHostComponentsData();
          await findMasterSlaveComponents();
        }
      }
    }
  };


  const updateSmartServerComponent = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const currentConfig = cloneDeep(allServiceModels["ssm"]);
    let smartServers = [] as any;

    let smartServer = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "SSM" &&
        get(item, "ServiceComponentInfo.component_name") === "SSM_SERVER"
    );

    if (!isEmpty(smartServer)) {
      smartServer.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "SSM_SERVER") {
          const hostComponentData = {
            componentName: "SSM_SERVER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            haState: get(
              hostComponent,
              "processes[0].HostComponentProcess.active_ssm_ip"
            ),
          };
          smartServers.push(hostComponentData);
        }
      });
      currentConfig[
        ServiceComponentMetricsEnums.SSM
          .smartServers as keyof typeof ServiceComponentMetricsEnums.SSM
      ] = smartServers;
    }
    if (!isEqual(allServiceModels["ssm"], currentConfig)) {
      allServiceModels["ssm"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //usePolling(pollServiceComponentInfoApi, 1000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["ssm"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["ssm"], configToBeUpdated)) {
      allServiceModels["ssm"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("SSM", "ssm", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "SSM"
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
      findMasterSlaveComponents();
      updateSmartServerComponent();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findMasterSlaveComponents();
    //updateAmbariMetricsHostComponentsData();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
