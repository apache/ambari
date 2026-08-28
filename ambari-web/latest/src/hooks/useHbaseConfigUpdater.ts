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
import {
  componentFinishStates,
  maintenanceStates,
} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";

export const useHbaseConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, parsedSocketMessages } = useContext(AppContext);

  // Early return if HBASE service is not installed
  const isHbaseInstalled = services && Array.isArray(services) &&
    services.some((service: any) => service.ServiceInfo.service_name === "HBASE");

  if (!isHbaseInstalled) {
    return;
  }

  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const fetchHbaseMasterSlaveClientsData = async () => {
    let hbaseComponentsData = cachedServiceApi.getServiceComponentData("HBASE");
    
    if (!hbaseComponentsData) {
      hbaseComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "HBASE"
      );
    }
    
    return hbaseComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HBASE" &&
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
                if (componentData.componentName === "HBASE_MASTER") {
                  host.isActiveMaster = get(
                    hostComponent,
                    "metrics.hbase.master.IsActiveMaster"
                  );
                } else {
                  host.isActiveMaster = false;
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
    const items = await fetchHbaseMasterSlaveClientsData();

    if (!allServiceModels["hbase"]) {
      return;
    }

    if (!items || items.length === 0) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["hbase"]);
    //const hdfsServiceObj = currentConfig.getServiceObject();
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
      } else {
        clientComponents.push(componentData);
      }
    });
    currentConfig[ServiceComponentFields.HBASE.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentFields.HBASE.slaveComponents] =
      slaveComponents;
    currentConfig[ServiceComponentFields.HBASE.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["hbase"], currentConfig)) {
      allServiceModels["hbase"].updateConfig(currentConfig);
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

  const updateHbaseMasterComponents = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const updates = cloneDeep(allServiceModels["hbase"]);
    let activeHbaseMasters = [] as any;
    let standbyHbaseMasters = [] as any;
    let nonActiveStandbyHbaseMasters = [] as any;

    let hbaseMaster = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HBASE" &&
        get(item, "ServiceComponentInfo.component_name") === "HBASE_MASTER"
    );

    if (hbaseMaster) {
      hbaseMaster.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "HBASE_MASTER") {
          const hostComponentData = {
            componentName: "HBASE_MASTER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            isActiveMaster: get(
              hostComponent,
              "metrics.hbase.master.IsActiveMaster"
            ),
            state: get(hostComponent, "HostRoles.state"),
          };

          if (
            hostComponentData.state === componentFinishStates[1] && //denotes started
            hostComponentData.isActiveMaster === "true"
          ) {
            activeHbaseMasters.push(hostComponentData);
            return;
          } else if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.isActiveMaster === "false"
          ) {
            standbyHbaseMasters.push(hostComponentData);
            return;
          }
          nonActiveStandbyHbaseMasters.push(hostComponentData);
        }
      });
    }
    updates[ServiceComponentFields.HBASE["activeHbaseMasters"]] =
      activeHbaseMasters;
    updates[ServiceComponentFields.HBASE["standbyHbaseMasters"]] =
      standbyHbaseMasters;
    updates[
      ServiceComponentFields.HBASE["nonActiveStandbyHbaseMasters"]
    ] = nonActiveStandbyHbaseMasters;

    // Only update if we have changes
    if (!isEqual(allServiceModels["hbase"], updates)) {
      allServiceModels["hbase"].updateConfig(updates);
      updateRegistry(allServiceModels);
    }
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["hbase"]);
    configToBeUpdated.isInPassiveForService =
      maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["hbase"], configToBeUpdated)) {
      allServiceModels["hbase"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  };

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("HBASE", "hbase", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
        (message) => message.service_name === "HBASE"
      );
    }
    if (
      (latestHostOperationMessage &&
        componentFinishStates.includes(latestHostOperationMessage.state)) ||
      (latestHostOperationMessage.maintenance_state && maintenanceStates.includes(latestHostOperationMessage.maintenance_state))
    ) {
      await updateServiceMaintenanceState(
        latestHostOperationMessage.maintenance_state
      );
      await updateAlertsAndServiceStateData();
    }
  };
  
  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      updateHbaseMasterComponents();
      findMasterSlaveClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    findMasterSlaveClientComponents();
    parseAlertsWebSocketMessages();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
