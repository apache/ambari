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

import { useContext, useEffect, useRef, useState } from "react";
import { cloneDeep, find, get, isEmpty, isEqual, map, set } from "lodash";
import { ServiceApi } from "../api/serviceApi";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { centralizedServiceStateApi } from "../api/centralizedServiceStateApi";
import { ServiceComponentFields } from "../enums/ServiceComponentFields";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import {
  componentFinishStates,
  maintenanceStates,
} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";
import useHDFSConfigsTags from "./useConfigsTags";

export const useHDFSConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);

  // @ts-ignore
  const { services } = useContext(AppContext);
  
  // Early return if HDFS service is not installed
  const isHDFSInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "HDFS");
  
  if (!isHDFSInstalled) {
    return;
  }

  const { configsData } = useHDFSConfigsTags();

  // @ts-ignore
  const { clusterName } = useContext(AppContext);
  // @ts-ignore
  const { parsedSocketMessages } = useContext(AppContext);

  // @ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  //const vdpStackVersion = get(cluster, "version", "").split("-")[1];
  const hasNameNodeHAEnabledUseEffectRunOnce = useRef(false);
  const [isHAEnabledForNamenode, setIsHAEnabledForNamenode] = useState(false);

  // useEffect(() => {
  //   if (allServiceModels?.["hdfs"]) {
  //     console.log("namespace obj ", inferNamespace());
  //     inferNamespace();
  //   }
  // }, [JSON.stringify(allServiceModels?.["hdfs"]?.masterComponents)]);

  useEffect(() => {
    if (allServiceModels?.["hdfs"]) {
      inferNamespace();
    }
  }, [allServiceModels["hdfs"]?.isNameNodeHaEnabled]);

  // Update masterComponentGroups when configs change (e.g., after federation is enabled)
  useEffect(() => {
    if (allServiceModels?.["hdfs"] && configsData?.items) {
      inferNamespace();
    }
  }, [JSON.stringify(configsData?.items)]);

  function inferNamespace() {
    const hdfsModel = cloneDeep(allServiceModels["hdfs"]);
    const isHAEnabled = hdfsModel?.isNameNodeHaEnabled;
    if (isHAEnabled) {
      const hdfsSiteConfigs = find(configsData?.items, ["type", "hdfs-site"]);
      if (hdfsSiteConfigs) {
        const properties = get(hdfsSiteConfigs, "properties", {});
        if (properties) {
          const nameSpaceProperty = properties["dfs.nameservices"];
          if (nameSpaceProperty) {
            const nameSpaces = nameSpaceProperty
              .split(",")
              .map((nameSpace: any) => {
                const nameNodeIdsProperty =
                  properties[`dfs.ha.namenodes.${nameSpace}`];
                if (nameNodeIdsProperty) {
                  const nameNodeIds = nameNodeIdsProperty.split(","),
                    hostNames = nameNodeIds.map((id: any) => {
                      const propertyValue =
                          properties[
                            `dfs.namenode.http-address.${nameSpace}.${id}`
                          ],
                        matches =
                          propertyValue &&
                          propertyValue.match(/([\D\d]+)\:\d+$/),
                        hostName = matches && matches[1];
                      return hostName;
                    });
                  return {
                    nameSpace,
                    hostNames,
                  };
                }
              });
            const componentsCopy = cloneDeep(
              get(hdfsModel, "masterComponents")
            );
            const allNameNodes = map(
              componentsCopy.find(
                (component: any) => component.componentName === "NAMENODE"
              )?.hostComponents,
              "HostRoles"
            );

            allNameNodes.forEach((component) => {
              const nameSpaceObject = nameSpaces.find(
                (ns: any) =>
                  ns &&
                  ns.hostNames &&
                  ns.hostNames.includes(component.host_name)
              );
              if (nameSpaceObject) {
                set(component, "haNameSpace", nameSpaceObject.nameSpace);
              }
            });

            // Create masterComponentGroups for federation detection
            const masterComponentGroups = nameSpaces
              .filter((ns: any) => ns && ns.nameSpace && ns.hostNames)
              .map((ns: any) => ({
                name: ns.nameSpace,
                title: ns.nameSpace,
                hosts: ns.hostNames.filter((host: string) => host), // Filter out null/undefined hosts
                components: ["NAMENODE", "ZKFC"],
                clusterId: "default"
              }));

            hdfsModel.updateConfig({
              namespaces: nameSpaces,
              isNamespaceLoaded: true,
              masterComponents: componentsCopy,
              federationNamespaces: masterComponentGroups,
            });
          }
        }
      }
    } else {
      // For non-HA HDFS, create a single default namespace group
      const defaultMasterComponentGroups = [{
        name: "default",
        title: "default", 
        hosts: hdfsModel.masterComponents
          ?.find((comp: any) => comp.componentName === "NAMENODE")
          ?.hostComponents?.map((hc: any) => hc.HostRoles.host_name) || [],
        components: ["NAMENODE"],
        clusterId: "default"
      }];

      hdfsModel.updateConfig({
        isNamespaceLoaded: true,
        federationNamespaces: defaultMasterComponentGroups,
      });
    }
    updateRegistry({ ...allServiceModels, ...{ hdfs: hdfsModel as any } });
  }

  const fetchHDFSMasterSlaveClientsData = async () => {
    if (isHAEnabledForNamenode)
      allServiceModels["hdfs"].isNameNodeHaEnabled = true;

    return Object.values(masterSlaveClientsData).filter(
      (item) => get(item, "ServiceComponentInfo.service_name") === "HDFS"
    );
  };
  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
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
                const hdfsServiceObject =
                  allServiceModels["hdfs"].getServiceObject();
                host.healthStatusMappedValue =
                  hdfsServiceObject.findHealthStatusMapValueForSingleHost(
                    host.state
                  );
                if (componentData.componentName === "NAMENODE") {
                  //check if ha is enabled
                  if (allServiceModels["hdfs"].isNameNodeHaEnabled) {
                    host.haStatus = get(
                      hostComponent,
                      "metrics.dfs.FSNamesystem.HAState"
                    );
                  }
                }
              }
            });
          }
        });
      }
    }
    return componentData;
  };
  const findMasterSlaveClientComponents = async () => {
    const items = await fetchHDFSMasterSlaveClientsData();
    //const isHAEnabled = allServiceModels["hdfs"]?.isNameNodeHaEnabled;

    if (!allServiceModels["hdfs"]) {
      return;
    }

    if (!items || items.length === 0) {
      return;
    }
    const isHAEnabled = allServiceModels["hdfs"]?.isNameNodeHaEnabled;

    const currentConfig = cloneDeep(allServiceModels["hdfs"]);
    let masterComponents: any[] = [];
    let slaveComponents: any[] = [];
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
        if (
          isHAEnabled &&
          componentData.componentName === "SECONDARY_NAMENODE"
        ) {
          return;
        }
        const masterComponentDataWithState =
          updateComponentObjectForSelectMaster(componentData);
        masterComponents.push(masterComponentDataWithState);
      } else if (componentData.category === Categories.SLAVE) {
        //For ZKFC state is updated via polled Data
        if (componentData.componentName === "ZKFC") {
          const slaveComponentDataWithState =
            updateComponentObjectForSelectMaster(componentData);
          slaveComponents.push(slaveComponentDataWithState);
        }
        //for these slaves the state is not updated via polled data
        else {
          slaveComponents.push(componentData);
        }
      } else {
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentFields.HDFS.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentFields.HDFS.slaveComponents] =
      slaveComponents;
    currentConfig[ServiceComponentFields.HDFS.clientComponents] =
      clientComponents;
    const allComponents = [...masterComponents, ...slaveComponents];
    const componentHosts = (componentName: string) =>
      allComponents
        .find((component) => component.componentName === componentName)
        ?.hostComponents?.map((hostComponent: any) => ({
          componentName,
          hostName: get(hostComponent, "HostRoles.host_name"),
          state: get(hostComponent, "HostRoles.state"),
        })) || [];
    currentConfig[ServiceComponentFields.HDFS.datanodes] =
      componentHosts("DATANODE");
    currentConfig[ServiceComponentFields.HDFS.journalNodes] =
      componentHosts("JOURNALNODE");

    // Update masterComponentGroups when master components change
    if (isHAEnabled && configsData?.items) {
      const hdfsSiteConfigs = find(configsData?.items, ["type", "hdfs-site"]);
      if (hdfsSiteConfigs) {
        const properties = get(hdfsSiteConfigs, "properties", {});
        const nameSpaceProperty = properties["dfs.nameservices"];
        if (nameSpaceProperty) {
          const nameSpaces = nameSpaceProperty.split(",");
          const masterComponentGroups = nameSpaces.map((nameSpace: string) => {
            const nameNodeIdsProperty = properties[`dfs.ha.namenodes.${nameSpace}`];
            let hosts: string[] = [];
            if (nameNodeIdsProperty) {
              const nameNodeIds = nameNodeIdsProperty.split(",");
              hosts = nameNodeIds.map((id: any) => {
                const propertyValue = properties[`dfs.namenode.http-address.${nameSpace}.${id}`];
                const matches = propertyValue && propertyValue.match(/([\D\d]+)\:\d+$/);
                return matches && matches[1];
              }).filter((host: string) => host); // Filter out null/undefined hosts
            }
            return {
              name: nameSpace,
              title: nameSpace,
              hosts: hosts,
              components: ["NAMENODE", "ZKFC"],
              clusterId: "default"
            };
          });
          currentConfig.federationNamespaces = masterComponentGroups;
        }
      }
    } else {
      // For non-HA HDFS, create a single default namespace group
      const nameNodeComponent = masterComponents.find((comp: any) => comp.componentName === "NAMENODE");
      const defaultMasterComponentGroups = [{
        name: "default",
        title: "default", 
        hosts: nameNodeComponent?.hostComponents?.map((hc: any) => hc.HostRoles.host_name) || [],
        components: ["NAMENODE"],
        clusterId: "default"
      }];
      currentConfig.federationNamespaces = defaultMasterComponentGroups;
    }

    if (!isEqual(allServiceModels["hdfs"], currentConfig)) {
      allServiceModels["hdfs"].updateConfig(currentConfig);
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

  const updateHDFSMasterComponents = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items || !allServiceModels["hdfs"]) return;

    //console.log("updating HDFS master components");
    const updates = cloneDeep(allServiceModels["hdfs"]);
    //console.log("hdfs updated conf = ", updates);

    let activeNameNodes = [] as any;
    let standbyNameNodes = [] as any;
    let zookeeperFailOverControllers = [] as any;
    let nonActiveStandbyNamenodes = [] as any;

    let isNameNodeHaEnabled = allServiceModels["hdfs"].isNameNodeHaEnabled;
    //let isNameNodeHaEnabled = true;
    //let isNameNodeHaEnabled = isNnHAEnabled();
    //console.log("is namenode ha enabled = ", isNameNodeHaEnabled);
    let nameNode = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
        get(item, "ServiceComponentInfo.component_name") === "NAMENODE"
    );

    if (nameNode && isNameNodeHaEnabled) {
      nameNode.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "NAMENODE") {
          const hostComponentData = {
            componentName: "NAMENODE",
            hostName: get(hostComponent, "HostRoles.host_name"),
            haStatus: get(hostComponent, "metrics.dfs.FSNamesystem.HAState"),
            state: get(hostComponent, "HostRoles.state"),
          };

          if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.haStatus === "active"
          ) {
            activeNameNodes.push(hostComponentData);
            return;
          } else if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.haStatus === "standby"
          ) {
            standbyNameNodes.push(hostComponentData);
            return;
          }
          nonActiveStandbyNamenodes.push(hostComponentData);
        }
      });
    } else if (nameNode && !isNameNodeHaEnabled) {
      nameNode.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "NAMENODE") {
          const hostComponentData = {
            componentName: "NAMENODE",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          updates[ServiceComponentFields.HDFS["nameNode"]] =
            hostComponentData;
          nonActiveStandbyNamenodes.push(hostComponentData);
        }
      });
    }
    updates[ServiceComponentFields.HDFS["activeNameNodes"]] =
      activeNameNodes;
    updates[ServiceComponentFields.HDFS["standbyNameNodes"]] =
      standbyNameNodes;
    updates[ServiceComponentFields.HDFS["nonActiveStandbyNamenodes"]] =
      nonActiveStandbyNamenodes;

    let snameNode = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
        get(item, "ServiceComponentInfo.component_name") ===
          "SECONDARY_NAMENODE"
    );

    if (snameNode) {
      const sNameNodeData = snameNode.host_components.map(
        (hostComponent: any) => ({
          componentName: "SECONDARY_NAMENODE",
          hostName: get(hostComponent, "HostRoles.host_name"),
          state: get(hostComponent, "HostRoles.state"),
        })
      )[0];

      if (sNameNodeData) {
        updates[ServiceComponentFields.HDFS["snameNode"]] = sNameNodeData;
      }
    }

    let zookeeperFC = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
        get(item, "ServiceComponentInfo.component_name") === "ZKFC"
    );

    if (zookeeperFC) {
      zookeeperFC.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "ZKFC") {
          const hostComponentData = {
            componentName: "ZKFC",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          zookeeperFailOverControllers.push(hostComponentData);
        }
      });
    }
    if (zookeeperFailOverControllers.length > 0) {
      updates[
        ServiceComponentFields.HDFS["zookeeperFailoverControllers"]
      ] = zookeeperFailOverControllers;
    }

    // Only update if we have changes
    if (!isEqual(allServiceModels["hdfs"], updates)) {
      allServiceModels["hdfs"].updateConfig(updates);
      updateRegistry(allServiceModels);
    }
  };

  const isNnHAEnabled = async () => {
    if (!allServiceModels["hdfs"]) return;

    const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
    const response =
      await ServiceApi.getAllServiceComponents(
        clusterName,
        fields
      );

    const secondaryNn = find(response.data.items, (item) => {
      return (
        get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
        get(item, "ServiceComponentInfo.component_name") ===
          "SECONDARY_NAMENODE"
      );
    });
    const nameNode = find(response.data.items, (item) => {
      return (
        get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
        get(item, "ServiceComponentInfo.component_name") === "NAMENODE"
      );
    });

    const currentConfig = cloneDeep(allServiceModels["hdfs"]);
    if (
      (!allServiceModels["hdfs"].snameNode ||
        secondaryNn.host_components.length === 0) &&
      nameNode.host_components.length > 1
    ) {
      currentConfig[ServiceComponentFields.HDFS.isNameNodeHaEnabled] =
        true;
      currentConfig[ServiceComponentFields.HDFS.snameNode] = null;
      setIsHAEnabledForNamenode(true);
      if (!isEqual(currentConfig, allServiceModels["hdfs"])) {
        allServiceModels["hdfs"].updateConfig(currentConfig);
        updateRegistry(allServiceModels);
      }
    }
  };

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["hdfs"]);
    configToBeUpdated.isInPassiveForService =
      maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["hdfs"], configToBeUpdated)) {
      allServiceModels["hdfs"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  };

  const updateAlertsAndServiceStateData = async () => {
    const currentConfig = cloneDeep(allServiceModels["hdfs"]);
    const serviceName = "HDFS";
    
    // Use centralized service state API instead of individual call
    const serviceStateData = centralizedServiceStateApi.getServiceStateData(serviceName);
    
    if (!serviceStateData) return;

    const { alertsCount, hasCriticalAlerts, state } = serviceStateData;

    if (!alertsCount && alertsCount !== 0) return;

    currentConfig[ServiceComponentFields.HDFS.hasCriticalAlerts] = hasCriticalAlerts;
    currentConfig[ServiceComponentFields.HDFS.alertsCount] = alertsCount;
    currentConfig[ServiceComponentFields.HDFS.state] = state;

    if (!isEqual(allServiceModels["hdfs"], currentConfig)) {
      allServiceModels["hdfs"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
        (message) => message.service_name === "HDFS"
      );
    }
    if (
      (latestHostOperationMessage &&
        componentFinishStates.includes(latestHostOperationMessage.state)) ||
      (latestHostOperationMessage.maintenance_state && maintenanceStates.includes(latestHostOperationMessage.maintenance_state))
    ) {
      await updateServiceMaintenanceState(
        latestHostOperationMessage.maintenance_state
      );
      await updateAlertsAndServiceStateData();
    }
  };

  const resetIsNameNodeHaEnabledAttr = () => {
    if (isHAEnabledForNamenode) {
      allServiceModels["hdfs"].isNameNodeHaEnabled = true;
    }
  };

  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      updateHDFSMasterComponents();
      findMasterSlaveClientComponents();
    }
  }, [polledHostComponentsData]);

  // useEffect(() => {
  //   updateQuicklinksData();
  // }, [JSON.stringify(allServiceModels["hdfs"])]);

  useEffect(() => {
    if (isEmpty(masterSlaveClientsData) && clusterName) {
      cachedServiceApi.fetchAllServiceComponents(clusterName);
    }
    findMasterSlaveClientComponents();
  }, [masterSlaveClientsData, clusterName]);

  useEffect(() => {
    if (
      hasNameNodeHAEnabledUseEffectRunOnce.current ||
      !allServiceModels["hdfs"] ||
      isHAEnabledForNamenode
    ) {
      return;
    }
    isNnHAEnabled();
    hasNameNodeHAEnabledUseEffectRunOnce.current = true;
  }, [allServiceModels]);
  // useEffect(() => {
  //   if (
  //       allServiceModels &&
  //       allServiceModels["hdfs"] &&
  //       allServiceModels["hdfs"].isNameNodeHaEnabled === false
  //   ) {
  //     isNnHAEnabled();
  //   }
  // }, [allServiceModels]);

  useEffect(() => {
    resetIsNameNodeHaEnabledAttr();
  }, [isHAEnabledForNamenode]);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
