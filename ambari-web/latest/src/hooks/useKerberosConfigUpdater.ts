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

export const useKerberosConfigUpdater = () => {
  // @ts-ignore
  const { services, parsedSocketMessages, clusterName } = useContext(AppContext);
  
  // Early return if KERBEROS service is not installed
  const isKerberosInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "KERBEROS");
  
  if (!isKerberosInstalled) {
    return;
  }
  
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const fetchKerberosMasterSlaveClientsData = async () => {
    let kerberosComponentsData = cachedServiceApi.getServiceComponentData("KERBEROS");
    
    if (!kerberosComponentsData) {
      kerberosComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "KERBEROS"
      );
    }
    
    return kerberosComponentsData;
  };

  const findKerberosClientComponents = async () => {
    const items = await fetchKerberosMasterSlaveClientsData();

    if (!allServiceModels["kerberos"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["kerberos"]);
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
        const kerberosClientsInstalled = componentData.installedCount;
        currentConfig[
          ServiceComponentFields.KERBEROS.kerberosClientsInstalled
        ] = kerberosClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentFields.KERBEROS.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["kerberos"], currentConfig)) {
      allServiceModels["kerberos"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //usePolling(pollServiceComponentInfoApi, 3000);

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
    updateServiceAlertsAndStateFromCentralizedApi("KERBEROS", "kerberos", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "KERBEROS"
      );
    }
    if (
        latestHostOperationMessage &&
        componentFinishStates.includes(latestHostOperationMessage.state)
        || maintenanceStates.includes(latestHostOperationMessage.maintenance_state)
    ) {
      await updateServiceMaintenanceState(latestHostOperationMessage.maintenance_state);
      await updateAlertsAndServiceStateData();
    }
  };

  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      findKerberosClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findKerberosClientComponents();
  }, []);
};
