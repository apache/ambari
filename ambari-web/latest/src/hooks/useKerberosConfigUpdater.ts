import { useContext, useEffect } from "react";
import { cloneDeep, get, isEqual } from "lodash";
import { cachedServiceApi } from "../api/CachedServiceApi.ts";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils.ts";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums.ts";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories.ts";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants.ts";

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
          ServiceComponentMetricsEnums.KERBEROS.kerberosClientsInstalled
        ] = kerberosClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.KERBEROS.clientComponents] =
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
