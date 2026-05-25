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
import { cloneDeep, find, get, has, isEmpty, isEqual } from "lodash";
import { ServiceApi } from "../api/serviceApi";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";

export const useTrinoConfigUpdater = () => {
  const {
    quickLinksMapWithAPIResponse,
    polledHostComponentsData,
    masterSlaveClientsData,
  } = useContext(ServiceContext);
  const { services,clusterName, parsedSocketMessages } = useContext(AppContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];

  const isTrinoInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "TRINO");

  if (!isTrinoInstalled) {
    return;
  }

  const fetchTrinoMasterSlaveClientsData = async () => {
    // 🚀 OPTIMIZATION: Try centralized cache first, fallback to masterSlaveClientsData
    
    let trinoComponentsData = cachedServiceApi.getServiceComponentData("TRINO");
    
    if (!trinoComponentsData) {
      trinoComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "TRINO"
      );
    } else {
    }
    
    return trinoComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "TRINO" &&
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

  const constructUrl = (link: any, node: any) => {
    const protocol = link.port.http_property ? "http" : "https";
    const port = link.port.http_property
      ? link.port.http_default_port
      : link.port.https_default_port;

    return link.url
      .replace("%@", protocol)
      .replace("%@", node)
      .replace("%@", port);
  };

  const fetchLinks = (items: any) => {
    const links = get(
      find(items, (item) =>
        has(
          item,
          "QuickLinkInfo.quicklink_data.QuickLinksConfiguration.configuration.links"
        )
      ),
      "QuickLinkInfo.quicklink_data.QuickLinksConfiguration.configuration.links",
      []
    );
    return links;
  };

  const updateQuicklinksData = async () => {
    const currentConfig = cloneDeep(allServiceModels["trino"]);
    // const response = await QuicklinksApi.getQuicklinks(
    //   vdpStackVersion,
    //   "TRINO"
    // );
    const response = quickLinksMapWithAPIResponse.get("TRINO");
    const linksObj = fetchLinks(response.data.items);
    let quickLinks: any[] = [];
    let trinoCoordinators = [];

    if (allServiceModels["trino"]?.trinoCoordinators?.length > 0) {
      trinoCoordinators = allServiceModels["trino"].trinoCoordinators;
    }

    trinoCoordinators?.forEach((trinoCoordinator: any) => {
      const hostName = trinoCoordinator.hostName;
      const hostLinks = linksObj.map((link: any) => {
        return {
          label: link.label,
          url: constructUrl(link, hostName),
          fileName: link.port.site,
          http_property: link.port.http_property,
          https_property: link.port.https_property
        };
      });
      quickLinks.push({ hostName, links: hostLinks });
    });

    currentConfig.quickLinks = quickLinks;
    if (
      !isEqual(allServiceModels["trino"], currentConfig) &&
      !isEmpty(currentConfig)
    ) {
      allServiceModels["trino"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const updateTrinoCoordinatorComponent = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const currentConfig = cloneDeep(allServiceModels["trino"]);
    let trinoCoordinators = [] as any;

    let trinoCoordinator = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "TRINO" &&
        get(item, "ServiceComponentInfo.component_name") === "TRINO_COORDINATOR"
    );

    if (!isEmpty(trinoCoordinator)) {
      trinoCoordinator.host_components.forEach((hostComponent: any) => {
        if (
          get(hostComponent, "HostRoles.component_name") === "TRINO_COORDINATOR"
        ) {
          const hostComponentData = {
            componentName: "TRINO_COORDINATOR",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          trinoCoordinators.push(hostComponentData);
        }
      });
      currentConfig[ServiceComponentMetricsEnums.TRINO.trinoCoordinators] =
        trinoCoordinators;
    }
    if (!isEqual(allServiceModels["trino"], currentConfig)) {
      allServiceModels["trino"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const findMasterSlaveClientComponents = async () => {
    const items = await fetchTrinoMasterSlaveClientsData();

    if (!allServiceModels["trino"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["trino"]);
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

    currentConfig[ServiceComponentMetricsEnums.TRINO.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.TRINO.slaveComponents] =
      slaveConponents;
    currentConfig[ServiceComponentMetricsEnums.TRINO.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["trino"], currentConfig)) {
      allServiceModels["trino"].updateConfig(currentConfig);
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
    let configToBeUpdated = cloneDeep(allServiceModels["trino"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["trino"], configToBeUpdated)) {
      allServiceModels["trino"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    const currentConfig = cloneDeep(allServiceModels["trino"]);
    const serviceName = "TRINO";
    const serviceStateResponse = await ServiceApi.getServiceState(
      clusterName,
      serviceName
    );
    const alertsCount =
      serviceStateResponse?.data?.alerts_summary?.CRITICAL +
      serviceStateResponse?.data?.alerts_summary?.WARNING;
    const serviceState = serviceStateResponse?.data?.ServiceInfo?.state;

    if (!alertsCount && alertsCount !== 0) return;

    if (serviceStateResponse?.data?.alerts_summary?.CRITICAL) {
      currentConfig[ServiceComponentMetricsEnums.AMBARI_METRICS.hasCriticalAlerts] =
          serviceStateResponse?.data?.alerts_summary?.CRITICAL > 0;
    }

    currentConfig[ServiceComponentMetricsEnums.TRINO.alertsCount] = alertsCount;
    currentConfig[ServiceComponentMetricsEnums.TRINO.state] = serviceState;
    if (!isEqual(allServiceModels["trino"], currentConfig)) {
      allServiceModels["trino"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "TRINO"
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
      updateTrinoCoordinatorComponent();
      updateQuicklinksData();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    //pollServiceComponentInfoApi();
    findMasterSlaveClientComponents();
    //updateSpark3HostComponentsData();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
