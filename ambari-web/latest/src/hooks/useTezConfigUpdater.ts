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
import { cloneDeep, get, isEqual } from "lodash";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";

export const useTezConfigUpdater = () => {
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if TEZ service is not installed
  const isTezInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "TEZ");
  
  if (!isTezInstalled) {
    return;
  }
  
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const fetchTezMasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let tezComponentsData = cachedServiceApi.getServiceComponentData("TEZ");
    
    if (!tezComponentsData) {
      tezComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "TEZ"
      );
    } else {
    }
    
    return tezComponentsData;
  };

  const findTezClientComponents = async () => {
    const items = await fetchTezMasterSlaveClientsData();

    if (!allServiceModels["tez"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["tez"]);
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

      if (componentData.category === Categories.CLIENT) {
        const tezClientsInstalled = componentData.installedCount;
        currentConfig[ServiceComponentFields.TEZ.tezClientsInstalled] =
          tezClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentFields.ZOOKEEPER.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["tez"], currentConfig)) {
      allServiceModels["tez"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //usePolling(pollServiceComponentInfoApi, 3000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["tez"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["tez"], configToBeUpdated)) {
      allServiceModels["tez"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("TEZ", "tez", allServiceModels, updateRegistry);
  };

  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "TEZ"
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
      findTezClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);

  useEffect(() => {
    // pollServiceComponentInfoApi();
    findTezClientComponents();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);
};
