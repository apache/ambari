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
import { ServiceApi } from "../api/serviceApi";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import {
  componentFinishStates,
  maintenanceStates,
} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";

export const useAmbariMetricsConfigUpdater = () => {
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if AMBARI_METRICS service is not installed
  const isAmbariMetricsInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "AMBARI_METRICS");
  
  if (!isAmbariMetricsInstalled) {
    return;
  }
  
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];

  const fetchAmbariMetricsMasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let ambariMetricsComponentsData = cachedServiceApi.getServiceComponentData("AMBARI_METRICS");
    
    if (!ambariMetricsComponentsData) {
      ambariMetricsComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "AMBARI_METRICS"
      );
    } else {
    }
    
    return ambariMetricsComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "AMBARI_METRICS" &&
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

  const findMasterSlaveComponents = async () => {
    const items = await fetchAmbariMetricsMasterSlaveClientsData();

    if (!allServiceModels["ambari_metrics"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["ambari_metrics"]);
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
        if (
          componentData.componentName === "METRICS_COLLECTOR" ||
          componentData.componentName === "METRICS_GRAFANA"
        ) {
          const masterComponentDataWithState =
            updateComponentObjectForSelectMaster(componentData);
          masterComponents.push(masterComponentDataWithState);
        }
      } else if (componentData.category === Categories.SLAVE) {
        slaveComponents.push(componentData);
      }
    });

    currentConfig[
      ServiceComponentMetricsEnums.AMBARI_METRICS.masterComponents
    ] = masterComponents;
    currentConfig[ServiceComponentMetricsEnums.AMBARI_METRICS.slaveComponents] =
      slaveComponents;

    if (!isEqual(allServiceModels["ambari_metrics"], currentConfig)) {
      allServiceModels["ambari_metrics"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  //check for updating metrics monitor
  //@ts-ignore
  const updateAmbariMetricsHostComponentsData = async () => {
    let updatedConfig = cloneDeep(allServiceModels["ambari_metrics"]);
    const serviceName = "AMBARI_METRICS"; // Replace with the desired service name
    const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
    await ServiceApi.getAllServiceComponentsListAndInitialMetrics(
      clusterName,
      `${fields}&ServiceComponentInfo/service_name=${serviceName}`
    );

    const components = [{ name: "METRICS_MONITOR", metric: "metricsMonitors" }];

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
        ServiceComponentMetricsEnums.AMBARI_METRICS[
          `${component.metric}Started` as keyof typeof ServiceComponentMetricsEnums.AMBARI_METRICS
        ] as any
      ] = startedCount;
      updatedConfig[
        ServiceComponentMetricsEnums.AMBARI_METRICS[
          `${component.metric}Installed` as keyof typeof ServiceComponentMetricsEnums.AMBARI_METRICS
        ] as any
      ] = installedCount;
      updatedConfig[
        ServiceComponentMetricsEnums.AMBARI_METRICS[
          `${component.metric}Total` as keyof typeof ServiceComponentMetricsEnums.AMBARI_METRICS
        ] as any
      ] = totalCount;
    });

    if (!isEqual(allServiceModels["ambari_metrics"], updatedConfig)) {
      allServiceModels["ambari_metrics"].updateConfig(updatedConfig);
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
          //await updateRangerHostComponentsData();
          await findMasterSlaveComponents();
        }
      }
    }
  };


  const updateGrafanaComponent = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const currentConfig = cloneDeep(allServiceModels["ambari_metrics"]);

    let grafana = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "AMBARI_METRICS" &&
        get(item, "ServiceComponentInfo.component_name") === "METRICS_GRAFANA"
    );
    grafana.state = get(grafana, "HostRoles.state");
    currentConfig[ServiceComponentMetricsEnums.AMBARI_METRICS.grafana] =
      grafana;

    // Only update if we have changes
    if (
      !isEqual(allServiceModels["ambari_metrics"], currentConfig) &&
      !isEmpty(currentConfig)
    ) {
      allServiceModels["ambari_metrics"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["ambari_metrics"]);
    configToBeUpdated.isInPassiveForService =
      maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["ambari_metrics"], configToBeUpdated)) {
      allServiceModels["ambari_metrics"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  };

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("AMBARI_METRICS", "ambari_metrics", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
        (message) => message.service_name === "AMBARI_METRICS"
      );
    }
    if (
      latestHostOperationMessage &&
      (componentFinishStates.includes(latestHostOperationMessage.state) ||
      (latestHostOperationMessage.maintenance_state && maintenanceStates.includes(latestHostOperationMessage.maintenance_state)))
    ) {
      await updateServiceMaintenanceState(
        latestHostOperationMessage.maintenance_state
      );
      await updateAlertsAndServiceStateData();
    }
  };

  //usePolling(pollServiceComponentInfoApi, 1000);

  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      findMasterSlaveComponents();
      updateGrafanaComponent();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findMasterSlaveComponents();
    updateAmbariMetricsHostComponentsData();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
