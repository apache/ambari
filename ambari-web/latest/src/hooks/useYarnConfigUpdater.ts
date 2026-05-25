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
import { cloneDeep, find, get, isEmpty, isEqual } from "lodash";
import { ServiceApi } from "../api/serviceApi";
import { cachedServiceApi } from "../api/cachedServiceApi";
import { centralizedServiceStateApi } from "../api/centralizedServiceStateApi";
import { ServiceComponentMetricsEnums } from "../enums/ServiceComponentMetricsEnums";
import { AppContext } from "../store/context.tsx";
import { ServiceContext } from "../store/ServiceContext.tsx";
import {componentFinishStates, maintenanceStates} from "../screens/Hosts/constants";
import { Categories } from "../enums/Categories";
import bytesToSize from "../Utils/numberUtils";
import objectUtils from "../Utils/objectUtils";

export const useYarnConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages } = useContext(AppContext);
  
  // Early return if YARN service is not installed
  const isYarnInstalled = services && Array.isArray(services) && 
    services.some((service: any) => service.ServiceInfo.service_name === "YARN");
  
  if (!isYarnInstalled) {
    return;
  }

  // @ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);
  const [serverClockTime, setServerClockTime] = useState<any>();
  const hasResourceManagerHAEnabledUseEffectRunOnce = useRef(false);

  const fetchServerCLockTime = async () => {
    const fields = "?fields=RootServiceComponents/server_clock";
    const responseData = await ServiceApi.ambariService(fields);
    const serverClock = get(
      responseData,
      "RootServiceComponents.server_clock",
      null
    );
    setServerClockTime(serverClock);
  };

  const fetchYARNMasterSlaveClientsData = async () => {
    let yarnComponentsData = cachedServiceApi.getServiceComponentData("YARN");
    
    if (!yarnComponentsData) {
      yarnComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "YARN"
      );
    }
    
    return yarnComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "YARN" &&
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
                // const yarnServiceObject =
                //   allServiceModels["yarn"].getServiceObject();
                // host.healthStatusMappedValue =
                //     yarnServiceObject.findHealthStatusMapValueForSingleHost(
                //         host.state
                //     );
                if (componentData.componentName === "RESOURCEMANAGER") {
                  //check if ha is enabled
                  if (allServiceModels["yarn"].isRMHAEnabled) {
                    const haState = get(hostComponent, "HostRoles.ha_state");
                    // Format the HA status to match Ember display (e.g., "ACTIVE" -> "ACTIVE")
                    host.haStatus = haState ? haState.toUpperCase() : "";
                  } else {
                    // For non-HA mode, don't show any HA status
                    host.haStatus = "";
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
    const items = await fetchYARNMasterSlaveClientsData();

    if (!allServiceModels["yarn"]) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["yarn"]);
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
        const masterComponentDataWithState =
          updateComponentObjectForSelectMaster(componentData);
        masterComponents.push(masterComponentDataWithState);
      } else if (componentData.category === Categories.SLAVE) {
        slaveComponents.push(componentData);
      } else if (componentData.category === Categories.CLIENT) {
        const yarnClientsInstalled = componentData.installedCount;
        currentConfig[ServiceComponentMetricsEnums.YARN.yarnClients] =
          yarnClientsInstalled;
        clientComponents.push(componentData);
        clientComponents.push(componentData);
      }
    });

    currentConfig[ServiceComponentMetricsEnums.YARN.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.YARN.slaveComponents] =
      slaveComponents;
    currentConfig[ServiceComponentMetricsEnums.YARN.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["yarn"], currentConfig)) {
      allServiceModels["yarn"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  // const updateHDFSHostComponentsData = async () => {
  //     let updatedConfig = cloneDeep(allServiceModels["hdfs"]);
  //
  //     const serviceName = "HDFS"; // Replace with the desired service name
  //     const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
  //     const response =
  //         await ServiceApi.getAllServiceComponentsListAndInitialMetrics(
  //             clusterName,
  //             `${fields}&ServiceComponentInfo/service_name=${serviceName}`
  //         );
  //
  //     let datanodes = [] as any;
  //
  //     const components = [
  //         { name: "DATANODE", metric: "dataNodes" },
  //         { name: "ROUTER", metric: "routers" },
  //         { name: "NFS_GATEWAY", metric: "nfsGateways" },
  //         { name: "JOURNALNODE", metric: "journalNodes" },
  //     ];
  //
  //     components.forEach((component) => {
  //         const hostComponents = findHostComponentItems(
  //             "HDFS",
  //             component.name,
  //             response
  //         );
  //         const installedCount =
  //             hostComponents.ServiceComponentInfo.installed_count;
  //         const startedCount = hostComponents.ServiceComponentInfo.started_count;
  //         const totalCount = hostComponents.ServiceComponentInfo.total_count;
  //         updatedConfig[
  //             ServiceComponentMetricsEnums.HDFS[
  //                 `${component.metric}Started` as keyof typeof ServiceComponentMetricsEnums.HDFS
  //                 ] as any
  //             ] = startedCount;
  //         updatedConfig[
  //             ServiceComponentMetricsEnums.HDFS[
  //                 `${component.metric}Installed` as keyof typeof ServiceComponentMetricsEnums.HDFS
  //                 ] as any
  //             ] = installedCount;
  //         updatedConfig[
  //             ServiceComponentMetricsEnums.HDFS[
  //                 `${component.metric}Total` as keyof typeof ServiceComponentMetricsEnums.HDFS
  //                 ] as any
  //             ] = totalCount;
  //     });
  //
  //     const datanode = find(
  //         response.data.items,
  //         (item) =>
  //             get(item, "ServiceComponentInfo.service_name") === "HDFS" &&
  //             get(item, "ServiceComponentInfo.component_name") === components[0].name
  //     );
  //
  //     if (datanode) {
  //         datanode.host_components.forEach((hostComponent: any) => {
  //             if (get(hostComponent, "HostRoles.component_name") === "DATANODE") {
  //                 const hostComponentData = {
  //                     componentName: components[0].name,
  //                     hostName: get(hostComponent, "HostRoles.host_name"),
  //                     state: get(hostComponent, "HostRoles.state"),
  //                 };
  //                 datanodes.push(hostComponentData);
  //             }
  //         });
  //
  //         if (datanodes.length > 0) {
  //             updatedConfig[ServiceComponentMetricsEnums.HDFS.datanodes] = datanodes;
  //         }
  //     }
  //     if (!isEqual(allServiceModels["hdfs"], updatedConfig)) {
  //         allServiceModels["hdfs"].updateConfig(updatedConfig);
  //         updateRegistry(allServiceModels);
  //     }
  // };

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

  const calclateResourceManagereUptime = (startTime: number) => {
    const currentConfig = cloneDeep(allServiceModels["yarn"]);
    const yarnServiceObj = currentConfig.getServiceObject();
    let clientClock = Date.now();

    let serverClock = serverClockTime;
    if (!serverClock) {
      return null; // Return null if serverClockTime is not available
    }
    
    serverClock = serverClock.toString();
    serverClock = serverClock.length < 13 ? serverClock + "000" : serverClock;
    const clockDistance = serverClock - clientClock;
    const uptime = startTime;
    if (uptime && uptime > 0) {
      const appDateTime = Date.now() + clockDistance;
      let diff = appDateTime - uptime;
      if (diff < 0) {
        diff = 0;
      }
      const formatted = yarnServiceObj.timingFormat(diff);
      return formatted.toString();
    }
  };

  const calcDiskUsagePartandPercent = () => {
    const currentConfig = cloneDeep(allServiceModels["yarn"]);
    const yarnServiceObj = currentConfig?.getServiceObject();
    const heapMemoryUsed = yarnServiceObj?.jvmMemoryHeapUsed;
    const heapMemoryMax = yarnServiceObj?.jvmMemoryHeapMax;

    const updates = [
      {
        key: "diskPartResourceManagerHeapMemory",
        value: yarnServiceObj?.diskPart(heapMemoryUsed, heapMemoryMax),
      },
    ];

    updates.forEach(({ key, value }) => {
      if (!value && value !== 0) {
        return;
      }
      currentConfig[key] = value.toString();
    });

    if (!isEqual(allServiceModels["yarn"], currentConfig)) {
      allServiceModels["yarn"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };


  const updateYARNMasterComponents = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const updates = cloneDeep(allServiceModels["yarn"]);
    let activeResourceManagers = [] as any;
    let standbyResourceManagers = [] as any;
    let nonActiveStandbyResourceManagers = [] as any;
    let isResourceManagerHaEnabled = allServiceModels["yarn"].isRMHAEnabled;
    let resourceManager = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "YARN" &&
        get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
    );
    if (resourceManager && isResourceManagerHaEnabled) {
      resourceManager.host_components.forEach((hostComponent: any) => {
        if (
          get(hostComponent, "HostRoles.component_name") === "RESOURCEMANAGER"
        ) {
          const hostComponentData = {
            componentName: "RESOURCEMANAGER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            haStatus: get(hostComponent, "HostRoles.ha_state"),
            state: get(hostComponent, "HostRoles.state"),
          };
          if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.haStatus &&
            hostComponentData.haStatus.toUpperCase() === "ACTIVE"
          ) {
            //updates[ServiceComponentMetricsEnums.HDFS["nameNode"]] = hostComponentData;
            activeResourceManagers.push(hostComponentData);
            return;
          } else if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.haStatus &&
            hostComponentData.haStatus.toUpperCase() === "STANDBY"
          ) {
            standbyResourceManagers.push(hostComponentData);
            return;
          }
          nonActiveStandbyResourceManagers.push(hostComponentData);
        }
      });
    } else if (resourceManager && !isResourceManagerHaEnabled) {
      resourceManager.host_components.forEach((hostComponent: any) => {
        if (
          get(hostComponent, "HostRoles.component_name") === "RESOURCEMANAGER"
        ) {
          const hostComponentData = {
            componentName: "RESOURCEMANAGER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            state: get(hostComponent, "HostRoles.state"),
          };
          updates[ServiceComponentMetricsEnums.YARN["resourceManager"]] =
            hostComponentData;
          nonActiveStandbyResourceManagers.push(hostComponentData);
        }
      });
    }
    updates[ServiceComponentMetricsEnums.YARN["activeResourceManagers"]] =
      activeResourceManagers;
    updates[ServiceComponentMetricsEnums.YARN["standbyResourceManagers"]] =
      standbyResourceManagers;
    updates[
      ServiceComponentMetricsEnums.YARN["nonActiveStandbyResourceManagers"]
    ] = nonActiveStandbyResourceManagers;

    // Only update if we have changes
    if (!isEqual(allServiceModels["yarn"], updates)) {
      allServiceModels["yarn"].updateConfig(updates);
      updateRegistry(allServiceModels);
    }
  };

  //@ts-ignore
  const isRMAEnabled = async () => {
    const yarnComponentsData = cachedServiceApi.getServiceComponentData("YARN");
    
    if (!yarnComponentsData) {
      return;
    }

    const resourceManager = yarnComponentsData.find((item: any) =>
      item.ServiceComponentInfo?.component_name === "RESOURCEMANAGER"
    );

    if (resourceManager && resourceManager.host_components && resourceManager.host_components.length > 1) {
      const updates = {
        [ServiceComponentMetricsEnums.YARN["isRMHAEnabled"]]: true,
      };

      if (
        !isEqual(updates, {
          isRMHAEnabled:
            allServiceModels["yarn"][
              ServiceComponentMetricsEnums.YARN["isRMHAEnabled"]
            ],
        })
      ) {
        allServiceModels["yarn"].updateConfig(updates);
        updateRegistry(allServiceModels);
      }
    }
  };

  // const findHostComponentItems = (
  //   serviceName: string,
  //   componentName: any,
  //   response: any
  // ) => {
  //   const item = find(response.data.items, (item) => {
  //     return (
  //       get(item, "ServiceComponentInfo.service_name") === serviceName &&
  //       get(item, "ServiceComponentInfo.component_name") === componentName
  //     );
  //   });
  //   return item;
  // };

  const updateYARNData = () => {
    const findMetrics = (data: any, metricParams: any) => {
      let yarnMetrics = new Map();
      const item = find(
        data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "YARN" &&
          get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
      );

      if (item) {
        const isRMHAEnabled = allServiceModels["yarn"]?.isRMHAEnabled;
        let hostComponent = {};
        if (isRMHAEnabled) {
          hostComponent = find(item.host_components, (hostComponent) => {
            return (
              get(hostComponent, "HostRoles.component_name") ===
                "RESOURCEMANAGER" &&
              get(hostComponent, "HostRoles.state") === "STARTED" &&
              get(hostComponent, "HostRoles.ha_state") === "ACTIVE"
            );
          });
        } else {
          hostComponent = find(item.host_components, (hostComponent) => {
            return (
              get(hostComponent, "HostRoles.component_name") ===
                "RESOURCEMANAGER" &&
              get(hostComponent, "HostRoles.state") === "STARTED"
            );
          });
        }
        if (hostComponent) {
          metricParams.forEach((metricParam: any) => {
            //check for metrics.Queue.root
            let metricParamValue = get(
              hostComponent,
              `metrics.yarn.Queue.root.${metricParam}`,
              null
            );

            // console.log(
            //   "metricParam === ",
            //   metricParam,
            //   "metricParamValue===",
            //   metricParamValue
            // );

            //metrics.Queue.root
            if (metricParamValue !== null && metricParamValue !== undefined) {
              if (
                metricParam === "AvailableMB" ||
                metricParam == "ReservedMB" ||
                (metricParam == "UsedAMResourceMB" &&
                  typeof metricParamValue === "number")
              ) {
                //value multiplied as the data we get from API is in MB's
                //@ts-ignore
                metricParamValue = bytesToSize(metricParamValue * 1024 * 1024);
              }
              yarnMetrics.set(metricParam, metricParamValue);
              return;
            } else {
              //not found in yarn queue root so check in yarn cluster
              metricParamValue = get(
                hostComponent,
                `metrics.yarn.ClusterMetrics.${metricParam}`,
                null
              );
              if (metricParamValue !== null && metricParamValue !== undefined) {
                yarnMetrics.set(metricParam, metricParamValue);
                return;
              } else {
                metricParamValue = get(
                  hostComponent,
                  `metrics.jvm.${metricParam}`,
                  null
                );
                if (
                  metricParamValue !== null &&
                  metricParamValue !== undefined
                ) {
                  yarnMetrics.set(metricParam, metricParamValue);
                  return;
                }
                //not found in FSNamesystem or jvm metrics so check in runtime metrics
                metricParamValue = get(
                  hostComponent,
                  `metrics.runtime.${metricParam}`,
                  null
                );
                if (metricParam === "StartTime") {
                  //pass the startTime to calculate namenode uptime
                  if (metricParamValue !== null) {
                    metricParamValue =
                      calclateResourceManagereUptime(metricParamValue);
                  }
                  yarnMetrics.set(metricParam, metricParamValue);
                  return;
                }
                //else {
                //     //not found in FSNamesystem or jvm metrics or runtime metrics so check in namenode metrics
                //     metricParamValue = get(
                //         hostComponent,
                //         `metrics.dfs.namenode.${metricParam}`,
                //         null
                //     );
                //     if (metricParam === "Safemode") {
                //         metricParamValue =
                //             allServiceModels["hdfs"]?.findSafeModeStatus(
                //                 metricParamValue
                //             );
                //     } else if (metricParam === "UpgradeFinalized") {
                //         const currentHostName = get(
                //             hostComponent,
                //             "HostRoles.host_name"
                //         );
                //         const healthStatus =
                //             allServiceModels["hdfs"]?.workStatusValues[
                //                 currentHostName as any
                //                 ]?.healthStatus;
                //         metricParamValue = allServiceModels[
                //             "hdfs"
                //             ].findUpgradeStatus(metricParamValue, healthStatus);
                //     } else if (
                //         metricParam === "LiveNodes" ||
                //         metricParam === "DeadNodes" ||
                //         metricParam === "DecomNodes"
                //     ) {
                //         const dataNodesStatusObj = get(
                //             hostComponent,
                //             `metrics.dfs.namenode.${metricParam}`,
                //             null
                //         );
                //         metricParamValue =
                //             allServiceModels["hdfs"]?.countKeysMatchingPattern(
                //                 dataNodesStatusObj
                //             );
                //     }
                //     dfsMetrics.set(metricParam, metricParamValue);
                //     return;
                // }
              }
            }
          });
        }
      }
      return yarnMetrics;
    };

    const fetchComponentsData = async () => {
      let updatedConfig = cloneDeep(allServiceModels["yarn"]);

      if (isEmpty(polledHostComponentsData)) {
        return;
      }

      // Simulating the fetching of YARB metric keys and finding metrics
      const yarnMetricsQueueRoot = Object.keys(
        ServiceComponentMetricsEnums.YARN.metrics.yarn.Queue.root
      );
      const yarnMetricsYarnCluster = Object.keys(
        ServiceComponentMetricsEnums.YARN.metrics.yarn.clusterMetrics
      );
      const yarnMetricsJvm = Object.keys(
        ServiceComponentMetricsEnums.YARN.metrics.jvm
      );
      const yarnMetricsRuntime = Object.keys(
        ServiceComponentMetricsEnums.YARN.metrics.runTime
      );

      const yarnPolledComponentData = find(
        //@ts-ignore
        polledHostComponentsData.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "YARN" &&
          get(item, "ServiceComponentInfo.component_name") === "RESOURCEMANAGER"
      );

      // component.queue = JSON.stringify({
      //   'root': self.parseObject(root)
      // });

      const getYarnQueueRootKeysPolledData = get(
        yarnPolledComponentData,
        "metrics.yarn.Queue.root",
        {}
      );

      const rootStringifiedData = JSON.stringify({
        root: objectUtils.parseObject(getYarnQueueRootKeysPolledData),
      });

      const queueRecursiveCountData = () => {
        const queueKeysParsed = JSON.parse(rootStringifiedData);
        return objectUtils.recursiveKeysCount(queueKeysParsed);
      };
      const queuesCount = queueRecursiveCountData();

      updatedConfig.queueKeysPolledFormattedData = queuesCount + " Queues";

      const metricsMapForMetricsQueueRoot = findMetrics(
        polledHostComponentsData,
        yarnMetricsQueueRoot
      );
      const metricsMapForYarnCluster = findMetrics(
        polledHostComponentsData,
        yarnMetricsYarnCluster
      );
      const metricsMapForJvm = findMetrics(
        polledHostComponentsData,
        yarnMetricsJvm
      );
      const metricsMapForRunTime = findMetrics(
        polledHostComponentsData,
        yarnMetricsRuntime
      );
      const currentMetrics = {};
      const newMetrics = {};

      if (!isEmpty(yarnMetricsRuntime)) {
        yarnMetricsRuntime.forEach((key) => {
          const metricKey =
            ServiceComponentMetricsEnums.YARN.metrics.runTime[
              key as keyof typeof ServiceComponentMetricsEnums.YARN.metrics.runTime
            ];
          const metricValue = metricsMapForRunTime.get(key);
          if (metricValue || metricValue >= 0) {
            //@ts-ignore
            currentMetrics[metricKey as string] =
              allServiceModels["yarn"][metricKey as string];
            //@ts-ignore
            newMetrics[metricKey as string] = metricValue;
            updatedConfig[metricKey] = metricValue;
          }
        });
      }

      if (!isEmpty(yarnMetricsQueueRoot)) {
        yarnMetricsQueueRoot.forEach((key) => {
          const metricKey =
            ServiceComponentMetricsEnums.YARN.metrics.yarn.Queue.root[
              key as keyof typeof ServiceComponentMetricsEnums.YARN.metrics.yarn.Queue.root
            ];
          const metricValue = metricsMapForMetricsQueueRoot.get(key);
          if (metricValue || metricValue >= 0) {
            //@ts-ignore
            currentMetrics[metricKey as string] =
              allServiceModels["yarn"][metricKey as string];
            //@ts-ignore
            newMetrics[metricKey as string] = metricValue;
            updatedConfig[metricKey] = metricValue;
          }
        });
      }
      if (!isEmpty(yarnMetricsYarnCluster)) {
        yarnMetricsYarnCluster.forEach((key) => {
          const metricKey =
            ServiceComponentMetricsEnums.YARN.metrics.yarn.clusterMetrics[
              key as keyof typeof ServiceComponentMetricsEnums.YARN.metrics.yarn.clusterMetrics
            ];
          const metricValue = metricsMapForYarnCluster.get(key);
          if (metricValue || metricValue >= 0) {
            //@ts-ignore
            currentMetrics[metricKey as string] =
              allServiceModels["yarn"][metricKey as string];
            //@ts-ignore
            newMetrics[metricKey as string] = metricValue;
            updatedConfig[metricKey] = metricValue;
          }
        });
      }
      if (!isEmpty(yarnMetricsJvm)) {
        yarnMetricsJvm.forEach((key) => {
          const metricKey =
            ServiceComponentMetricsEnums.YARN.metrics.jvm[
              key as keyof typeof ServiceComponentMetricsEnums.YARN.metrics.jvm
            ];
          const metricValue = metricsMapForJvm.get(key);
          if (metricValue || metricValue >= 0) {
            //@ts-ignore
            currentMetrics[metricKey as string] =
              allServiceModels["yarn"][metricKey as string];
            //@ts-ignore
            newMetrics[metricKey as string] = metricValue;
            updatedConfig[metricKey] = metricValue;
          }
        });
      }
      if (!isEqual(updatedConfig, allServiceModels["yarn"])) {
        allServiceModels["yarn"].updateConfig(updatedConfig);
        updateRegistry(allServiceModels);
      }
    };
    fetchComponentsData();
  };

  // usePolling(updateHDFSData, 5000);

  //usePolling(pollServiceComponentInfoApi, 3000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["yarn"]);
    configToBeUpdated.isInPassiveForService = maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["yarn"], configToBeUpdated)) {
      allServiceModels["yarn"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  }

  const updateAlertsAndServiceStateData = async () => {
    const currentConfig = cloneDeep(allServiceModels["yarn"]);
    const serviceName = "YARN";
    
    // Use centralized service state API instead of individual call
    const serviceStateData = centralizedServiceStateApi.getServiceStateData(serviceName);
    
    if (!serviceStateData) return;

    const { alertsCount, hasCriticalAlerts, state } = serviceStateData;

    if (!alertsCount && alertsCount !== 0) return;

    currentConfig[ServiceComponentMetricsEnums.AMBARI_METRICS.hasCriticalAlerts] = hasCriticalAlerts;
    currentConfig[ServiceComponentMetricsEnums.YARN.alertsCount] = alertsCount;
    currentConfig[ServiceComponentMetricsEnums.YARN.state] = state;

    if (!isEqual(allServiceModels["yarn"], currentConfig)) {
      allServiceModels["yarn"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
          (message) => message.service_name === "YARN"
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
      updateYARNData();
      updateYARNMasterComponents();
      findMasterSlaveClientComponents();
      fetchServerCLockTime();
      calcDiskUsagePartandPercent();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    if (
      hasResourceManagerHAEnabledUseEffectRunOnce.current ||
      !allServiceModels["yarn"]
    ) {
      return;
    }
    isRMAEnabled();
    hasResourceManagerHAEnabledUseEffectRunOnce.current = true;
  }, [allServiceModels]);

  useEffect(() => {
    fetchServerCLockTime();
    //isRMAEnabled();
    //findMasterSlaveClientComponents();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
