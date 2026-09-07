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

import { useContext, useEffect, useState } from "react";
import { cloneDeep, find, get, isEmpty, isEqual } from "lodash";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { updateServiceAlertsAndStateFromCentralizedApi } from "../Utils/centralizedServiceStateUtils";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import { Categories } from "../enums/Categories";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";
import ConfigsApi from "../api/configsApi";

export const useHiveConfigUpdater = () => {
  // @ts-ignore
  const { services, parsedSocketMessages, clusterName } = useContext(AppContext);
  
  // Early return if HIVE service is not installed
  const isHiveInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "HIVE");
  
  if (!isHiveInstalled) {
    return;
  }
  
  const { polledHostComponentsData, masterSlaveClientsData, serviceStatesData } =
    useContext(ServiceContext);
  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  const [hiveJDBCURL, setHiveJDBCURL] = useState("");


  const fetchHiveMasterSlaveClientsData = async () => {
    if (allServiceModels["hive"].hiveServer2JDBCURL === "") {
      allServiceModels["hive"].hiveServer2JDBCURL = hiveJDBCURL;
    }
    
    let hiveComponentsData = cachedServiceApi.getServiceComponentData("HIVE");
    
    if (!hiveComponentsData) {
      hiveComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "HIVE"
      );
    }
    
    return hiveComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item: any) =>
        get(item, "ServiceComponentInfo.service_name") === "HIVE" &&
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

  const findHiveMasterClientComponents = async () => {
    const items = await fetchHiveMasterSlaveClientsData();

    if (!allServiceModels["hive"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["hive"]);
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
        const hiveClientsInstalled = componentData.installedCount;
        currentConfig[ServiceComponentFields.HIVE.hiveClients] =
          hiveClientsInstalled;
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentFields.HIVE.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentFields.HIVE.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["hive"], currentConfig)) {
      allServiceModels["hive"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["hive"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["hive"], configToBeUpdated)) {
      allServiceModels["hive"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("HIVE", "hive", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "HIVE"
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

  const updateHiveServer2JDBCURL = async () => {
    if (
      allServiceModels["hive"] &&
      !isEmpty(allServiceModels["hive"].hiveServer2JDBCURL)
    ) {
      return;
    }

    try {
      // Following Ember.js setHiveEndPointsValue logic - fetch configurations by sites
      const sites = ['hive-site', 'hive-interactive-site'];
      
      // First get the current config tags (following Ember.js pattern)
      const configTagsResponse = await ConfigsApi.loadConfigTags(clusterName);
      
      if (!configTagsResponse?.Clusters?.desired_configs) {
        return;
      }

      const desiredConfigs = configTagsResponse.Clusters.desired_configs;
      
      // Build URL params for the sites we need (following Ember.js pattern)
      const urlParams = sites
        .filter(site => desiredConfigs[site]) // Only include sites that exist
        .map(site => `(type=${site}&tag=${desiredConfigs[site].tag})`)
        .join('|');

      if (!urlParams) {
        return;
      }

      // Fetch configurations by tags (following Ember.js reassign.load_configs pattern)
      const configsResponse = await ConfigsApi.reassignLoadConfigs(clusterName, urlParams);
      
      if (!configsResponse?.items) {
        return;
      }

      // Convert to the format expected by our logic
      const configs = configsResponse.items.filter((config: any) => config && config.properties);

      if (configs.length === 0) {
        return;
      }

      // Following Ember.js logic - ensure hive-site is first
      const hiveSiteIndex = configs.findIndex((config: any) => config.type === 'hive-site');
      if (hiveSiteIndex > 0) {
        // Move hive-site to first position
        const hiveSiteConfig = configs.splice(hiveSiteIndex, 1)[0];
        configs.unshift(hiveSiteConfig);
      }

      // Initialize with hive-site values (following Ember.js pattern)
      let hiveSiteDynamicDiscovery = configs[0].properties['hive.server2.support.dynamic.service.discovery'];
      let hiveSiteZkQuorom = configs[0].properties['hive.zookeeper.quorum'];
      let hiveSiteServiceDiscoveryMode = 'zooKeeper';
      let hiveSiteZkNameSpace = configs[0].properties['hive.server2.zookeeper.namespace'];

      // Process each config (following Ember.js forEach logic)
      configs.forEach((_config: any) => {
        if (_config.type === 'hive-interactive-site') {
          // Override with hive-interactive-site values if available (Ember.js fallback pattern)
          hiveSiteDynamicDiscovery = _config.properties['hive.server2.support.dynamic.service.discovery'] || hiveSiteDynamicDiscovery;
          hiveSiteZkQuorom = _config.properties['hive.zookeeper.quorum'] || hiveSiteZkQuorom;
          
          // For namespace, only override if it's not the interactive-specific namespace
          // This ensures we use the standard hiveserver2 namespace, not hiveserver2-interactive
          const interactiveNamespace = _config.properties['hive.server2.zookeeper.namespace'];
          if (interactiveNamespace && interactiveNamespace !== 'hiveserver2-interactive') {
            hiveSiteZkNameSpace = interactiveNamespace;
          }
          
          // Check for HIVE_SERVER_INTERACTIVE HA mode (following Ember.js HA detection)
          // Only use HA namespace if it exists and is not a placeholder value
          const haNamespace = _config.properties['hive.server2.active.passive.ha.registry.namespace'];
          if (haNamespace && haNamespace !== 'hs2ActivePassiveHA') {
            hiveSiteServiceDiscoveryMode = 'zooKeeperHA';
            hiveSiteZkNameSpace = haNamespace;
          }
        }
      });

      // Check if required properties exist after processing all configs
      if (!hiveSiteZkQuorom || !hiveSiteZkNameSpace) {
        return;
      }

      // Construct JDBC URL following Ember.js format pattern
      const hiveServer2JDBCURL = `jdbc:hive2://${hiveSiteZkQuorom}/${hiveSiteZkNameSpace};serviceDiscoveryMode=${hiveSiteServiceDiscoveryMode};zooKeeperNamespace=${hiveSiteZkNameSpace}`;

      if (!allServiceModels["hive"]) {
        return;
      }

      const currentConfig = cloneDeep(allServiceModels["hive"]);
      currentConfig[ServiceComponentFields.HIVE.hiveServer2JDBCURL] = hiveServer2JDBCURL;
      setHiveJDBCURL(hiveServer2JDBCURL);
      
      if (!isEqual(allServiceModels["hive"], currentConfig)) {
        allServiceModels["hive"].updateConfig(currentConfig);
        updateRegistry(allServiceModels);
      }
    } catch (error) {
      // Graceful error handling - JDBC URL will remain empty if config is not available
    }
  };

  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      findHiveMasterClientComponents();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    if (!isEmpty(parsedSocketMessages)) {
      parseAlertsWebSocketMessages();
    }
  }, [parsedSocketMessages]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    if (
      allServiceModels &&
      allServiceModels["hive"] &&
      allServiceModels["hive"].hiveServer2JDBCURL === ""
    ) {
      updateHiveServer2JDBCURL();
    }
  }, [allServiceModels]);
};
