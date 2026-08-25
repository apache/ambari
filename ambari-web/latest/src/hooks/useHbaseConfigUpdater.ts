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

export const useHbaseConfigUpdater = () => {
  const {
    polledHostComponentsData,
    masterSlaveClientsData,
    serviceStatesData,
  } = useContext(ServiceContext);
  
  // @ts-ignore
  const { services, clusterName, parsedSocketMessages, clockDistance } = useContext(AppContext);

  // Early return if HBASE service is not installed
  const isHbaseInstalled = services && Array.isArray(services) &&
    services.some((service: any) => service.ServiceInfo.service_name === "HBASE");

  if (!isHbaseInstalled) {
    return;
  }

  //@ts-ignore
  const { allServiceModels, updateRegistry } = useContext(ServiceContext);

  const fetchHbaseMasterSlaveClientsData = async () => {
    let hbaseComponentsData = cachedServiceApi.getServiceComponentData("HBASE");
    
    if (!hbaseComponentsData) {
      hbaseComponentsData = Object.values(masterSlaveClientsData).filter(
        (item) => get(item, "ServiceComponentInfo.service_name") === "HBASE"
      );
    }
    
    return hbaseComponentsData;
  };

  const updateComponentObjectForSelectMaster = (componentData: any) => {
    let masterComponent = find(
      //@ts-ignore
      polledHostComponentsData?.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HBASE" &&
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
                if (componentData.componentName === "HBASE_MASTER") {
                  host.isActiveMaster = get(
                    hostComponent,
                    "metrics.hbase.master.IsActiveMaster"
                  );
                } else {
                  host.isActiveMaster = false;
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
    const items = await fetchHbaseMasterSlaveClientsData();

    if (!allServiceModels["hbase"]) {
      return;
    }

    if (!items || items.length === 0) {
      return;
    }

    const currentConfig = cloneDeep(allServiceModels["hbase"]);
    //const hdfsServiceObj = currentConfig.getServiceObject();
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
      } else {
        clientComponents.push(componentData);
      }
    });
    currentConfig[ServiceComponentMetricsEnums.HDFS.masterComponents] =
      masterComponents;
    currentConfig[ServiceComponentMetricsEnums.HDFS.slaveComponents] =
      slaveComponents;
    currentConfig[ServiceComponentMetricsEnums.HDFS.clientComponents] =
      clientComponents;

    if (!isEqual(allServiceModels["hbase"], currentConfig)) {
      allServiceModels["hbase"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };

  const updateHbaseHostComponentsData = async () => {
    let updatedConfig = cloneDeep(allServiceModels["hbase"]);

    const serviceName = "HBASE"; // Replace with the desired service name
    const fields = `ServiceComponentInfo/service_name,ServiceComponentInfo/category,ServiceComponentInfo/installed_count,ServiceComponentInfo/started_count,ServiceComponentInfo/init_count,ServiceComponentInfo/install_failed_count,ServiceComponentInfo/unknown_count,ServiceComponentInfo/total_count,ServiceComponentInfo/display_name,host_components/HostRoles/host_name&minimal_response=true`;
    const response =
      await ServiceApi.getAllServiceComponentsListAndInitialMetrics(
        clusterName,
        `${fields}&ServiceComponentInfo/service_name=${serviceName}`
      );

    const components = [
      { name: "HBASE_REGIONSERVER", metric: "hbaseRegionServers" },
      { name: "PHOENIX_QUERY_SERVER", metric: "phoenixQueryServers" },
    ];

    components.forEach((component) => {
      const hostComponents = findHostComponentItems(
        "HBASE",
        component.name,
        response
      );
      
      if (!hostComponents || !hostComponents.ServiceComponentInfo) {
        return; // Skip if hostComponents is undefined or doesn't have ServiceComponentInfo
      }
      
      const installedCount =
        hostComponents.ServiceComponentInfo.installed_count;
      const startedCount = hostComponents.ServiceComponentInfo.started_count;
      const totalCount = hostComponents.ServiceComponentInfo.total_count;
      updatedConfig[
        ServiceComponentMetricsEnums.HBASE[
          `${component.metric}Started ` as keyof typeof ServiceComponentMetricsEnums.HBASE
        ] as any
      ] = startedCount;
      updatedConfig[
        ServiceComponentMetricsEnums.HBASE[
          `${component.metric}Installed` as keyof typeof ServiceComponentMetricsEnums.HBASE
        ] as any
      ] = installedCount;
      updatedConfig[
        ServiceComponentMetricsEnums.HBASE[
          `${component.metric}Total` as keyof typeof ServiceComponentMetricsEnums.HBASE
        ] as any
      ] = totalCount;
    });

    if (!isEqual(allServiceModels["hbase"], updatedConfig)) {
      allServiceModels["hbase"].updateConfig(updatedConfig);
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
          await updateHbaseHostComponentsData();
          await findMasterSlaveClientComponents();
        }
      }
    }
  };

  const calculateHbaseMasterUptime = (startOrActiveTime: number) => {
    const currentConfig = cloneDeep(allServiceModels["hbase"]);
    const hbaseServiceObj = currentConfig?.getServiceObject();
    const uptime = startOrActiveTime;
    if (uptime && uptime > 0) {
      const appDateTime = Date.now() + clockDistance;
      let diff = appDateTime - uptime;
      if (diff < 0) {
        diff = 0;
      }
      const formatted = hbaseServiceObj.timingFormat(diff);
      return formatted.toString();
    }
  };
  const calcDiskUsagePartandPercent = () => {
    const currentConfig = cloneDeep(allServiceModels["hbase"]);
    const hbaseServiceObj = currentConfig?.getServiceObject();

    if (!hbaseServiceObj) {
      return;
    }

    const updates = [
      {
        key: "diskPartHbaseMasterHeap",
        value: hbaseServiceObj.diskPart(
          hbaseServiceObj.heapMemoryUsed,
          hbaseServiceObj.heapMemoryMax
        ),
      },
      {
        key: "percentHbaseMasterHeap",
        value: hbaseServiceObj.findCapacityPercentage(
          hbaseServiceObj.heapMemoryUsed,
          hbaseServiceObj.heapMemoryMax
        ),
      },
    ];

    updates.forEach(({ key, value }) => {
      if (currentConfig[key] && currentConfig[key] !== value) {
        currentConfig[key] = value.toString();
      }
    });

    if (!isEqual(allServiceModels["hbase"], currentConfig)) {
      allServiceModels["hbase"].updateConfig(currentConfig);
      updateRegistry(allServiceModels);
    }
  };


  const updateHbaseMasterComponents = async () => {
    //@ts-ignore
    if (!polledHostComponentsData?.items) return;

    const updates = cloneDeep(allServiceModels["hbase"]);
    let activeHbaseMasters = [] as any;
    let standbyHbaseMasters = [] as any;
    let nonActiveStandbyHbaseMasters = [] as any;

    let hbaseMaster = find(
      //@ts-ignore
      polledHostComponentsData.items,
      (item) =>
        get(item, "ServiceComponentInfo.service_name") === "HBASE" &&
        get(item, "ServiceComponentInfo.component_name") === "HBASE_MASTER"
    );

    if (hbaseMaster) {
      hbaseMaster.host_components.forEach((hostComponent: any) => {
        if (get(hostComponent, "HostRoles.component_name") === "HBASE_MASTER") {
          const hostComponentData = {
            componentName: "HBASE_MASTER",
            hostName: get(hostComponent, "HostRoles.host_name"),
            isActiveMaster: get(
              hostComponent,
              "metrics.hbase.master.IsActiveMaster"
            ),
            state: get(hostComponent, "HostRoles.state"),
          };

          if (
            hostComponentData.state === componentFinishStates[1] && //denotes started
            hostComponentData.isActiveMaster === "true"
          ) {
            //updates[ServiceComponentMetricsEnums.HDFS["nameNode"]] = hostComponentData;
            activeHbaseMasters.push(hostComponentData);
            return;
          } else if (
            hostComponentData.state === componentFinishStates[1] &&
            hostComponentData.isActiveMaster === "false"
          ) {
            standbyHbaseMasters.push(hostComponentData);
            return;
          }
          nonActiveStandbyHbaseMasters.push(hostComponentData);
        }
      });
    }
    updates[ServiceComponentMetricsEnums.HBASE["activeHbaseMasters"]] =
      activeHbaseMasters;
    updates[ServiceComponentMetricsEnums.HBASE["standbyHbaseMasters"]] =
      standbyHbaseMasters;
    updates[
      ServiceComponentMetricsEnums.HBASE["nonActiveStandbyHbaseMasters"]
    ] = nonActiveStandbyHbaseMasters;

    // Only update if we have changes
    if (!isEqual(allServiceModels["hbase"], updates)) {
      allServiceModels["hbase"].updateConfig(updates);
      updateRegistry(allServiceModels);
    }
  };

  const findHostComponentItems = (
    serviceName: string,
    componentName: any,
    response: any
  ) => {
    const item = find(response.data.items, (item) => {
      return (
        get(item, "ServiceComponentInfo.service_name") === serviceName &&
        get(item, "ServiceComponentInfo.component_name") === componentName
      );
    });
    return item;
  };

  const updateHbaseData = () => {
    const findMetrics = (data: any, metricParams: any) => {
      let hbaseMetrics = new Map();
      const item = find(
        data.items,
        (item) =>
          get(item, "ServiceComponentInfo.service_name") === "HBASE" &&
          get(item, "ServiceComponentInfo.component_name") === "HBASE_MASTER"
      );
      if (item) {
        const hostComponent = find(item.host_components, (hostComponent) => {
          return (
            get(hostComponent, "HostRoles.component_name") === "HBASE_MASTER" &&
            get(hostComponent, "HostRoles.state") === "STARTED" &&
            get(hostComponent, "metrics.hbase.master.IsActiveMaster") === "true"
          );
        });
        if (hostComponent) {
          metricParams.forEach((metricParam: any) => {
            //check for hbase master main metrics
            let metricParamValue = get(
              hostComponent,
              `metrics.hbase.master.${metricParam}`,
              null
            );
            if (metricParam === "AverageLoad" && metricParamValue !== null) {
              metricParamValue = Number.isInteger(metricParamValue)
                ? metricParamValue + " regions per RegionServer"
                : metricParamValue.toFixed(2) + " regions per RegionServer";
            } else if (
              metricParam === "MasterStartTime" ||
              metricParam === "MasterActiveTime"
            ) {
              metricParamValue = calculateHbaseMasterUptime(metricParamValue);
            }

            //check for jvm metrics
            if (metricParamValue == null) {
              metricParamValue = get(
                hostComponent,
                `metrics.jvm.${metricParam}`,
                null
              );
            }
            //check for regions in transition
            if (metricParamValue == null) {
              metricParamValue = get(
                hostComponent,
                `metrics.master.AssignmentManager.${metricParam}`,
                null
              );
            }
            hbaseMetrics.set(metricParam, metricParamValue);
          });
          return hbaseMetrics;
        }
      }
      return null;
    };

    const fetchComponentsData = async () => {
      let updatedConfig = cloneDeep(allServiceModels["hbase"]);

      // Simulating the fetching of host components data
      if (isEmpty(polledHostComponentsData)) {
        return;
      }

      // Simulating the fetching of HDFS metric keys and finding metrics
      const hbaseMetricKeys = Object.keys(
        ServiceComponentMetricsEnums.HBASE.metrics
      );
      const metricsMap = findMetrics(polledHostComponentsData, hbaseMetricKeys);
      const currentMetrics = {};
      const newMetrics = {};

      if (metricsMap) {
        hbaseMetricKeys.forEach((key) => {
          const metricKey =
            ServiceComponentMetricsEnums.HBASE.metrics[
              key as keyof typeof ServiceComponentMetricsEnums.HBASE.metrics
            ];
          const metricValue = metricsMap.get(key);
          if (metricValue || metricValue >= 0) {
            //@ts-ignore
            currentMetrics[metricKey as string] =
              allServiceModels["hbase"][metricKey as string];
            //@ts-ignore
            newMetrics[metricKey as string] = metricValue;
            updatedConfig[metricKey] = metricValue;
          }

          if (!isEqual(updatedConfig, allServiceModels["hbase"])) {
            allServiceModels["hbase"].updateConfig(updatedConfig);
            updateRegistry(allServiceModels);
          }
        });
      }
    };
    fetchComponentsData();
  };

  //usePolling(pollServiceComponentInfoApi, 3000);

  const updateServiceMaintenanceState = (maintenanceState: string) => {
    let configToBeUpdated = cloneDeep(allServiceModels["hbase"]);
    configToBeUpdated.isInPassiveForService =
      maintenanceState === maintenanceStates[0]; //signifies ON, assigns ON/PFF

    if (!isEqual(allServiceModels["hbase"], configToBeUpdated)) {
      allServiceModels["hbase"].updateConfig(configToBeUpdated);
      updateRegistry(allServiceModels);
    }
  };

  const updateAlertsAndServiceStateData = async () => {
    // Use centralized service state API instead of individual call
    updateServiceAlertsAndStateFromCentralizedApi("HBASE", "hbase", allServiceModels, updateRegistry);
  };
  const parseAlertsWebSocketMessages = async () => {
    let latestHostOperationMessage = {} as any;
    if (parsedSocketMessages.length > 0) {
      latestHostOperationMessage = parsedSocketMessages.find(
        (message) => message.service_name === "HBASE"
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
  
  useEffect(() => {
    //@ts-ignore
    if (polledHostComponentsData?.items) {
      updateHbaseData();
      updateHbaseMasterComponents();
      findMasterSlaveClientComponents();
      calcDiskUsagePartandPercent();
    }
  }, [polledHostComponentsData]);

  useEffect(() => {
    updateHbaseHostComponentsData();
    findMasterSlaveClientComponents();
    parseAlertsWebSocketMessages();
  }, []);

  useEffect(() => {
    updateAlertsAndServiceStateData();
  }, [allServiceModels, serviceStatesData]);

  useEffect(() => {
    parseWebSocketMessages();
    parseAlertsWebSocketMessages();
  }, [parsedSocketMessages]);
};
