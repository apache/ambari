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

import { useContext, useEffect, useRef } from "react";
import { cloneDeep, find, get, isEmpty, isEqual } from "lodash";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";
import ConfigsApi from "../api/configsApi";

export const useRangerConfigUpdater = () => {
  let isRangerConfigUpdating = useRef(false);
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if RANGER service is not installed
  const isRangerInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "RANGER");
  
  if (!isRangerInstalled) {
    return;
  }
  
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];
  const isFetchingRangerPluginInfo = useRef(false); // Flag to check if the function is running

  const fetchRangerMasterSlaveClientsData = async () => {
    return Object.values(masterSlaveClientsData).filter(
      (item) => get(item, "ServiceComponentInfo.service_name") === "RANGER"
    );
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "RANGER" &&
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

  const findMasterSlaveClientComponents = async () => {
    const items = await fetchRangerMasterSlaveClientsData();

    if (!allServiceModels || !allServiceModels["ranger"]) {
      return;
    }

    if (!items || items.length === 0) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["ranger"]);

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
          componentData.componentName === "RANGER_ADMIN" ||
          componentData.componentName === "RANGER_USERSYNC"
        ) {
          const masterComponentDataWithState =
            updateComponentObjectForSelectMaster(componentData);
          masterComponents.push(masterComponentDataWithState);
        }
      } else if (componentData.category === Categories.SLAVE) {
        slaveComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentFields.RANGER.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentFields.RANGER.slaveComponents] =
      slaveComponents;

    if (!isEqual(allServiceModels["ranger"], currentConfig)) {
      isRangerConfigUpdating.current = true;
      allServiceModels["ranger"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
      isRangerConfigUpdating.current = false;
    }
    // if (!isEqual(allServiceModels["ranger"], currentConfig)) {
    //   updateQueue.enqueue(async () => {
    //     allServiceModels["ranger"].updateConfig(currentConfig);
    //     updateRegistry(allServiceModels);
    //   });
    // }
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


  const fetchRangerPluginInfo = async () => {
    if (isFetchingRangerPluginInfo.current) {
      return;
    }


    let configsData: any = {};

    if (!allServiceModels || !allServiceModels["ranger"]) return;

    const desiredConfigsResponse =
      await ConfigsApi.getDesiredConfigsInfo(clusterName);


    const configTags = [
      "ranger-hdfs-plugin-properties",
      "ranger-hbase-plugin-properties",
      "hive-env",
      "ranger-yarn-plugin-properties",
    ];

    const configTagValues = configTags.map((tag) =>
      get(desiredConfigsResponse, `data.Clusters.desired_configs.${tag}.tag`)
    );


    const rangerPluginInfo = await ConfigsApi.getEnabledConfigsForRangerPlugins(
      //@ts-ignore
      ...configTagValues,
      clusterName
    );


    const rangerPluginPropertiesKeys = Object.keys(
      ServiceComponentFields.RANGER.pluginProperties
    );


    rangerPluginPropertiesKeys.forEach((key) => {
      configsData[key] = rangerPluginInfo.data.items.find(
        (item: any) => item.type === key
      );
    });

    const pluginStatusMapping = {
      "ranger-hdfs-plugin-properties":
        get(
          configsData,
          "ranger-hdfs-plugin-properties.properties.ranger-hdfs-plugin-enabled"
        ) === "Yes"
          ? "Enabled"
          : "Disabled",
      "ranger-hbase-plugin-properties":
        get(
          configsData,
          "ranger-hbase-plugin-properties.properties.ranger-hbase-plugin-enabled"
        ) === "Yes"
          ? "Enabled"
          : "Disabled",
      "hive-env":
        get(configsData, "hive-env.properties.hive_security_authorization") ===
        "Ranger"
          ? "Enabled"
          : "Disabled",
      "ranger-yarn-plugin-properties":
        get(
          configsData,
          "ranger-yarn-plugin-properties.properties.ranger-yarn-plugin-enabled"
        ) === "Yes"
          ? "Enabled"
          : "Disabled",
    };

    let currentConfig = cloneDeep(allServiceModels["ranger"]);
    Object.keys(pluginStatusMapping).forEach((key) => {
      const propertyKey =
        key as keyof typeof ServiceComponentFields.RANGER.pluginProperties;
      currentConfig[
        ServiceComponentFields.RANGER.pluginProperties[propertyKey]
      ] =
        //@ts-ignore
        pluginStatusMapping[key];
    });

    // Object.keys(pluginStatusMapping).forEach((key) => {
    //   currentConfig[
    //     ServiceComponentFields.RANGER.pluginProperties[
    //       key as keyof typeof ServiceComponentFields.RANGER.pluginProperties
    //     ]
    //   ] =
    //     pluginStatusMapping[
    //       key as keyof typeof ServiceComponentFields.RANGER.pluginProperties
    //     ];
    // });

    if (!isEqual(allServiceModels["ranger"], currentConfig)) {
      isRangerConfigUpdating.current = true;
      allServiceModels["ranger"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
      isRangerConfigUpdating.current = false;
    }
  };

  const updateRangerAdminComponent = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items || !allServiceModels["ranger"]) return;

    const currentConfig = cloneDeep(allServiceModels["ranger"]);
    let rangerAdmins = [] as any;

    let rangerAdmin = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "RANGER" &&
        get(item, "ServiceComponentInfo.component_name") === "RANGER_ADMIN"
    );

    if (!isEmpty(rangerAdmin)) {
      rangerAdmin.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "RANGER_ADMIN") {
          const hostComponentData = {
            componentName: "RANGER_ADMIN",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          rangerAdmins.push(hostComponentData);
        }
      });
      currentConfig[ServiceComponentFields.RANGER.rangerAdmins] =
        rangerAdmins;
    }

    // Only update if we have changes
    if (
      !isEqual(allServiceModels["ranger"], currentConfig) &&
      !isRangerConfigUpdating.current
    ) {
      isRangerConfigUpdating.current = true;
      allServiceModels["ranger"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
      isRangerConfigUpdating.current = false;
    }
  };

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("RANGER", "ranger", allServiceModels, updateRegistry);
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["ranger"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["ranger"], configToBeUpdated)) {
      allServiceModels["ranger"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }
  
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "RANGER"
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
      findMasterSlaveClientComponents();
      updateRangerAdminComponent();
      fetchRangerPluginInfo();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);
};
