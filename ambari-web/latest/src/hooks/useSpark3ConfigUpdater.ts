import { useContext, useEffect } from "react";
import { cloneDeep, find, get, isEmpty, isEqual } from "lodash";
import { ServiceApi } from "../api/ServiceApi.ts";
import { cachedServiceApi } from "../api/CachedServiceApi.ts";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils.ts";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums.ts";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories.ts";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants.ts";

export const useSpark3ConfigUpdater =  () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if SPARK3 service is not installed
  const isSpark3Installed = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "SPARK3");
  
  if (!isSpark3Installed) {
    return;
  }
  
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];

  const fetchSpark3MasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let spark3ComponentsData = cachedServiceApi.getServiceComponentData("SPARK3");
    
    if (!spark3ComponentsData) {
      spark3ComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "SPARK3"
      );
    } else {
    }
    
    return spark3ComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "SPARK3" &&
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


  const updateSpark3HostComponentsData = async () => {
    let updatedConfig = cloneDeep(allServiceModels["spark3"]);
    const serviceName = "SPARK3"; // Replace with the desired service name
    const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
    await ServiceApi.getAllServiceComponentsListAndInitialMetrics(
      clusterName,
      `${fields}&ServiceComponentInfo/service_name=${serviceName}`
    );

    const components = [
      { name: "LIVY3_SERVER", metric: "livyForSpark3Server" },
      { name: "SPARK3_THRIFTSERVER", metric: "spark3ThriftServer" },
    ];

    components.forEach((component) => {
      // const hostComponents = findHostComponentItems("RANGER", component.name, response);
      const hostComponents = { ServiceComponentInfo: {} as any };
      
      if (!hostComponents || !hostComponents.ServiceComponentInfo) {
        return; // Skip if hostComponents is undefined or doesn't have ServiceComponentInfo
      }
      
      const installedCount =
        hostComponents.ServiceComponentInfo.installed_count;
      const startedCount = hostComponents.ServiceComponentInfo.started_count;
      const totalCount = hostComponents.ServiceComponentInfo.total_count;
      updatedConfig[
        ServiceComponentMetricsEnums.SPARK3[
          `${component.metric}Started` as keyof typeof ServiceComponentMetricsEnums.SPARK3
        ] as any
      ] = startedCount;
      updatedConfig[
        ServiceComponentMetricsEnums.SPARK3[
          `${component.metric}Installed` as keyof typeof ServiceComponentMetricsEnums.SPARK3
        ] as any
      ] = installedCount;
      updatedConfig[
        ServiceComponentMetricsEnums.SPARK3[
          `${component.metric}Total` as keyof typeof ServiceComponentMetricsEnums.SPARK3
        ] as any
      ] = totalCount;
    });

    if (!isEqual(allServiceModels["spark3"], updatedConfig)) {
      allServiceModels["spark3"].updateConfig(updatedConfig);
      updateRegistry(allServiceModels);
    }
  };

  const updateSpark3HistoryServerComponent = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const currentConfig = cloneDeep(allServiceModels["spark3"]);
    let spark3HistoryServers = [] as any;

    let spark3HistoryServer = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "SPARK3" &&
        get(item, "ServiceComponentInfo.component_name") ===
          "SPARK3_JOBHISTORYSERVER"
    );

    if (!isEmpty(spark3HistoryServer)) {
      spark3HistoryServer.host_components.forEach((hostComponent: any) => {
        if (
          get(hostComponent, "HostRoles.component_name") ===
          "SPARK3_JOBHISTORYSERVER"
        ) {
          const hostComponentData = {
            componentName: "SPARK3_JOBHISTORYSERVER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          spark3HistoryServers.push(hostComponentData);
        }
      });
      currentConfig[
        ServiceComponentMetricsEnums.SPARK3.spark3JobHistoryServers
      ] = spark3HistoryServers;
    }
    if (!isEqual(allServiceModels["spark3"], currentConfig)) {
      allServiceModels["spark3"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const findMasterSlaveClientComponents = async () => {
    const items = await fetchSpark3MasterSlaveClientsData();

    if (!allServiceModels["spark3"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["spark3"]);
    let masterComponents: any[] = [];
    let slaveConponents: any[] = [];
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
        slaveConponents.push(componentData);
      } else {
        const spark3ClientsInstalled = componentData.installedCount;
        currentConfig[ServiceComponentMetricsEnums.SPARK3.spark3Clients] =
          spark3ClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.SPARK3.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.SPARK3.slaveComponents] =
      slaveConponents;
    currentConfig[ServiceComponentMetricsEnums.SPARK3.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["spark3"], currentConfig)) {
      allServiceModels["spark3"].updateConfig(currentConfig);
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
    let configToBeUpdated = cloneDeep(allServiceModels["spark3"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["spark3"], configToBeUpdated)) {
      allServiceModels["spark3"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("SPARK3", "spark3", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "SPARK3"
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

  //usePolling(pollServiceComponentInfoApi, 3000);
  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      findMasterSlaveClientComponents();
      updateSpark3HistoryServerComponent();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findMasterSlaveClientComponents();
    updateSpark3HostComponentsData();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
