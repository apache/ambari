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
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";

export const useSqoopConfigUpdater = () => {
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  const { services, parsedSocketMessages } = useContext(AppContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const isSqoopInstalled = services && Array.isArray(services) &&
    services.some((service: any) => service.ServiceInfo.service_name === "SQOOP");

  if (!isSqoopInstalled) {
    return;
  }

  const fetchSqoopMasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let sqoopComponentsData = cachedServiceApi.getServiceComponentData("SQOOP");
    
    if (!sqoopComponentsData) {
      sqoopComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "SQOOP"
      );
    } else {
    }
    
    return sqoopComponentsData;
  };

  const findSqoopClientComponents = async () => {
    const items = await fetchSqoopMasterSlaveClientsData();

    if (!allServiceModels["sqoop"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["sqoop"]);
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
        const sqoopClientsInstalled = componentData.installedCount;
        const sqoopClientsStarted = componentData.startedCount;
        const sqoopClientsTotal = componentData.totalCount;
        
        currentConfig[ServiceComponentMetricsEnums.SQOOP.sqoopClientsInstalled] =
          sqoopClientsInstalled;
        currentConfig[ServiceComponentMetricsEnums.SQOOP.sqoopClientsStarted] =
          sqoopClientsStarted;
        currentConfig[ServiceComponentMetricsEnums.SQOOP.sqoopClientsTotal] =
          sqoopClientsTotal;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.SQOOP.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["sqoop"], currentConfig)) {
      allServiceModels["sqoop"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //usePolling(pollServiceComponentInfoApi, 3000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["sqoop"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/OFF

    if (!isEqual(allServiceModels["sqoop"], configToBeUpdated)) {
      allServiceModels["sqoop"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("SQOOP", "sqoop", allServiceModels, updateRegistry);
  };

  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "SQOOP"
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
      findSqoopClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);

  useEffect(() => {
    // pollServiceComponentInfoApi();
    findSqoopClientComponents();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);
};
