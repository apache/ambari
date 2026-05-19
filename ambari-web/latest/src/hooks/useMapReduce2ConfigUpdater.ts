import { useContext, useEffect } from "react";
import { cloneDeep, find, get, isEmpty, isEqual } from "lodash";
import { cachedServiceApi } from "../api/CachedServiceApi.ts";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils.ts";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums.ts";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories.ts";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants.ts";

export const useMapReduce2ConfigUpdater = () => {
  // @ts-ignore
  const { services, parsedSocketMessages, clusterName } = useContext(AppContext);
  
  // Early return if MAPREDUCE2 service is not installed
  const isMapReduce2Installed = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "MAPREDUCE2");
  
  if (!isMapReduce2Installed) {
    return;
  }
  
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];

  const fetchMR2MasterSlaveClientsData = async () => {
    let mr2ComponentsData = cachedServiceApi.getServiceComponentData("MAPREDUCE2");
    
    if (!mr2ComponentsData) {
      mr2ComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "MAPREDUCE2"
      );
    }
    
    return mr2ComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "MAPREDUCE2" &&
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


  const updateJobHistoryServerComponent = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const currentConfig = cloneDeep(allServiceModels["mapreduce2"]);

    let jobHistoryServer = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "MAPREDUCE2" &&
        get(item, "ServiceComponentInfo.component_name") === "HISTORYSERVER"
    );
    jobHistoryServer.state = get(jobHistoryServer, "HostRoles.state");
    currentConfig[ServiceComponentMetricsEnums.MAPREDUCE2.jobHistoryServer] =
      jobHistoryServer;

    // Only update if we have changes
    if (
      !isEqual(allServiceModels["mapreduce2"], currentConfig) &&
      !isEmpty(currentConfig)
    ) {
      allServiceModels["mapreduce2"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const findMR2MasterClientComponents = async () => {
    const items = await fetchMR2MasterSlaveClientsData();

    if (!allServiceModels["mapreduce2"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["mapreduce2"]);
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
        const mr2ClientsInstalled = componentData.installedCount;
        currentConfig[
          ServiceComponentMetricsEnums.MAPREDUCE2.mapReduce2Clients
        ] = mr2ClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.MAPREDUCE2.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.MAPREDUCE2.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["mapreduce2"], currentConfig)) {
      allServiceModels["mapreduce2"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //usePolling(pollServiceComponentInfoApi, 3000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["mapreduce2"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["mapreduce2"], configToBeUpdated)) {
      allServiceModels["mapreduce2"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("MAPREDUCE2", "mapreduce2", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "MAPREDUCE2"
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
      findMR2MasterClientComponents();
      updateJobHistoryServerComponent();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findMR2MasterClientComponents();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);
};
