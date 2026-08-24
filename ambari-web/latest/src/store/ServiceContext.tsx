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

import React, { useContext, useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import HDFSService from "../models/hdfs";
import { cloneDeep, isEmpty, isEqual } from "lodash";
import OptimizedUpdater from "./OptimizedUpdater.tsx";
import ZkService from "../models/zookeeper.ts";
import HBaseService from "../models/hbase.ts";
import RangerService from "../models/ranger.ts";
import MapReduce2Service from "../models/mapreduce2.ts";
import Spark3Service from "../models/spark3.ts";
import KerberosService from "../models/kerberos.ts";
import RangerKMSService from "../models/ranger_kms.ts";
import AmbariMetricsService from "../models/ambari_metrics.ts";
import TezService from "../models/tez.ts";
import TrinoService from "../models/trino.ts";
import SSMService from "../models/ssm.ts";
import YARNService from "../models/yarn.ts";
import HiveService from "../models/hive.ts";
import usePolling from "../hooks/usePolling.ts";
import { AppContext } from "./context.tsx";
import { useAlerts } from "./AlertsContext.tsx";
import usePrevious from "../hooks/usePrevious.ts";
import { ServiceApi } from "../api/serviceApi.ts";
import { cachedServiceApi } from "../api/cachedServiceApi.ts";
import { centralizedServiceStateApi } from "../api/centralizedServiceStateApi.ts";
import { serviceNameModelMapping } from "../constants.ts";
import SqoopService from "../models/sqoop.ts";
import KyuubiService from "../models/kyuubi.ts";
import TrinoGatewayService from "../models/trinogateway.ts";
import PinotService from "../models/pinot.ts";

interface ServiceContextType {
  allServiceModels: { [key: string]: any };
  allModelsLoaded: boolean;
  serviceModels: { [key: string]: any };
  updateRegistry: Function;
  polledHostComponentsData: {};
  quickLinksMapWithAPIResponse: Map<string, any>;
  masterSlaveClientsData: {};
  serviceStatesData: Map<string, any>;
}

export const ServiceContext = React.createContext<ServiceContextType>({
  allServiceModels: {},
  allModelsLoaded: false,
  serviceModels: {},
  updateRegistry: () => {},
  polledHostComponentsData: () => {},
  masterSlaveClientsData: () => {},
  //@ts-ignore
  quickLinksMapWithAPIResponse: Map<string, any>,
  //@ts-ignore
  serviceStatesData: new Map(),
});

interface ServiceProviderProps {
  children: any;
}

const ServiceProvider: React.FC<ServiceProviderProps> = ({ children }) => {
  // const { services } = useContext(AppContext);
  // const vdpStackVersion = get(cluster, "version", "").split("-")[1];
  const location = useLocation();
  const [allServiceModels, setAllServiceModels] = useState<any>({});
  const [allModelsLoaded, setAllModelsLoaded] = useState<boolean>(false);
  const [polledHostComponentsData, setPolledHostComponentsData] = useState<any>(
    {}
  );
  const previousHostComponentsData = usePrevious(polledHostComponentsData);
  const [quickLinksMapWithAPIResponse, setQuickLinksMapWithAPIResponse] =
    useState<any>(null);

  const [masterSlaveClientsData, setMasterSlaveClientsData] = useState<any>({});
  const previousMasterSlaveClientsData = usePrevious(masterSlaveClientsData);
  const [serviceStatesData, setServiceStatesData] = useState<Map<string, any>>(new Map());

  const { clusterName } = useContext(AppContext);

  // Alert data from AlertsContext, used to calculate service alert counts without a separate /alerts call
  const { alertSummary, alertDefinitions } = useAlerts();
  
  const isOnClusterAdminPage = location.pathname.includes('/main/admin/');

  // const [quicklinks, setQuicklinks] = useState<Map<string, any>>(new Map());

  // const { fetchAndUpdateConfig, updateConfig } = useHDFSConfigUpdater(
  //   allServiceModels,
  //   setAllServiceModels
  // );

  const initializeServiceRegistry = async () => {
    const modelsCopy: any = cloneDeep(allServiceModels);
    modelsCopy.ranger = new RangerService({ serviceName: "ranger" } as any);
    modelsCopy.hdfs = new HDFSService({ serviceName: "hdfs" } as any);
    modelsCopy.zk = new ZkService({ serviceName: "zookeeper" } as any);
    modelsCopy.hbase = new HBaseService({ serviceName: "hbase" } as any);
    modelsCopy.mapreduce2 = new MapReduce2Service({
      serviceName: "mapreduce2",
    } as any);
    modelsCopy.tez = new TezService({ serviceName: "tez" } as any);
    modelsCopy.spark3 = new Spark3Service({ serviceName: "spark3" } as any);
    modelsCopy.kerberos = new KerberosService({
      serviceName: "kerberos",
    } as any);
    modelsCopy.ranger_kms = new RangerKMSService({
      serviceName: "ranger_kms",
    } as any);
    modelsCopy.ambari_metrics = new AmbariMetricsService({
      serviceName: "ambari_metrics",
    } as any);
    modelsCopy.trino = new TrinoService({ serviceName: "trino" } as any);
    modelsCopy.ssm = new SSMService({ serviceName: "ssm" } as any);
    modelsCopy.hive = new HiveService({ serviceName: "hive" } as any);
    modelsCopy.yarn = new YARNService({ serviceName: "yarn" } as any);
    modelsCopy.sqoop = new SqoopService({ serviceName: "sqoop" } as any);
    modelsCopy.kyuubi = new KyuubiService({ serviceName: "kyuubi" } as any);
    modelsCopy.trino_gateway = new TrinoGatewayService({ serviceName: "trino_gateway" } as any);
    modelsCopy.pinot = new PinotService({ serviceName: "pinot" } as any);
    setAllServiceModels(modelsCopy);
  };

  const updateQuickLinksApiResponseForAllServices = async () => {
    const quickLinksMap = new Map<string, any>();
    // let serviceNames  = Object.keys(services);

    // await Promise.all(
    //   clusterServices.map(async (item: any) => {
    //     const serviceName = item.ServiceInfo.service_name;
    //     if (servicesWithQuickLinks.has(serviceName.toUpperCase())) {
    //       const quickLinksResponseForService =
    //         await QuicklinksApi.getQuicklinks(vdpStackVersion, serviceName);
    //       quickLinksMap.set(
    //         serviceName.toUpperCase(),
    //         quickLinksResponseForService
    //       );
    //     }
    //   })
    // );

    setQuickLinksMapWithAPIResponse(quickLinksMap);
  };

  const updateRegistry = (updatedModels: any) => {
    if (updatedModels) {
      const modelsCopy: any = cloneDeep(updatedModels);
      if (JSON.stringify(updatedModels) !== JSON.stringify(allServiceModels))
        setAllServiceModels(modelsCopy);
    }
  };
  useEffect(() => {
    updateQuickLinksApiResponseForAllServices();
    setAllModelsLoaded(false);
    initializeServiceRegistry();
    setAllModelsLoaded(true);
    // fetchMaintenanceModeForService();
  }, []);
  useEffect(() => {
    if (allModelsLoaded) {
    }
  }, [allModelsLoaded]);

  /**
   * OPTIMIZED: Use cached data from CachedServiceApi instead of making duplicate API call
   * This replaces the duplicate API call with cached data usage
   */
  const fetchOptimizedMaintenanceAndStaleData = async () => {
    try {
      // USE CACHED DATA instead of making new API call
      // CachedServiceApi is already polling this same endpoint
      const cachedData = cachedServiceApi.getAllComponentData();

      if (!cachedData) {
        // If no cached data yet, fetch it once (will be cached for subsequent calls)
        await cachedServiceApi.fetchAllServiceComponents(clusterName);
        return;
      }

      const responseData = cachedData;

      if (!isEmpty(responseData)) {
        
        // Process stale configs for all services following Ember logic
        // In Ember: isRestartRequired = serviceComponents.filterProperty('staleConfigHosts.length').length > 0
        const serviceStaleStatus: { [key: string]: boolean } = {};
        const serviceComponentsWithStaleHosts: { [key: string]: string[] } = {};
        
        // Single iteration through all components to extract stale config hosts (like Ember)
        responseData.items?.forEach((item: any) => {
          const serviceName = item.ServiceComponentInfo?.service_name;
          const componentName = item.ServiceComponentInfo?.component_name;
          if (!serviceName || !componentName) return;

          // Collect hosts with stale configs for this component (following Ember pattern)
          const staleConfigHosts = item.host_components?.filter(
            (hc: any) => hc.HostRoles?.stale_configs === true
          ).map((hc: any) => hc.HostRoles?.host_name).filter(Boolean) || [];

          // Track components with stale config hosts per service
          if (!serviceComponentsWithStaleHosts[serviceName]) {
            serviceComponentsWithStaleHosts[serviceName] = [];
          }
          
          // Add to service's stale hosts list if this component has stale hosts
          if (staleConfigHosts.length > 0) {
            serviceComponentsWithStaleHosts[serviceName] = serviceComponentsWithStaleHosts[serviceName].concat(staleConfigHosts);
          }
        });

        // Determine service restart requirement (following Ember logic)
        Object.entries(serviceComponentsWithStaleHosts).forEach(([serviceName, staleHosts]) => {
          // Service needs restart if ANY component has hosts with stale configs
          serviceStaleStatus[serviceName] = staleHosts.length > 0;
        });

        // Update service models with stale configs only (maintenance mode handled separately)
        let hasUpdates = false;
        const updatedModels = cloneDeep(allServiceModels);

        Object.entries(serviceStaleStatus).forEach(([serviceName, hasStaleConfigs]) => {
          const serviceModelKey = serviceNameModelMapping[serviceName];
          if (updatedModels[serviceModelKey]) {
            const currentStaleValue = updatedModels[serviceModelKey].isRestartRequiredForService;
            
            // Update stale configs status only (following Ember isRestartRequired logic)
            if (currentStaleValue !== hasStaleConfigs) {
              updatedModels[serviceModelKey].isRestartRequiredForService = hasStaleConfigs;
              hasUpdates = true;
            }
          }
        });

        // REACTIVE: Detect component-level maintenance mode changes
        const componentMaintenanceChanges: string[] = [];
        
        if (previousHostComponentsData?.items) {
          responseData.items?.forEach((currentItem: any) => {
            const serviceName = currentItem.ServiceComponentInfo?.service_name;
            const componentName = currentItem.ServiceComponentInfo?.component_name;
            
            if (!serviceName || !componentName) return;
            
            // Find the previous state of this component
            const previousItem = previousHostComponentsData.items.find((prevItem: any) =>
              prevItem.ServiceComponentInfo?.service_name === serviceName &&
              prevItem.ServiceComponentInfo?.component_name === componentName
            );
            
            if (previousItem) {
              // Check if any host component maintenance state changed
              const currentMaintenanceStates = currentItem.host_components?.map((hc: any) => 
                `${hc.HostRoles?.host_name}:${hc.HostRoles?.maintenance_state}`
              ).sort() || [];
              
              const previousMaintenanceStates = previousItem.host_components?.map((hc: any) => 
                `${hc.HostRoles?.host_name}:${hc.HostRoles?.maintenance_state}`
              ).sort() || [];
              
              // If maintenance states changed, track this service for alert refresh
              if (!isEqual(currentMaintenanceStates, previousMaintenanceStates)) {
                if (!componentMaintenanceChanges.includes(serviceName)) {
                  componentMaintenanceChanges.push(serviceName);
                }
              }
            }
          });
        }

        // Update both polledHostComponentsData and masterSlaveClientsData from single response
        if (
          responseData?.items &&
          (!isEqual(previousHostComponentsData?.items, responseData.items) ||
           !isEqual(previousMasterSlaveClientsData?.items, responseData.items))
        ) {
          setPolledHostComponentsData(responseData);
          setMasterSlaveClientsData(responseData.items);
        }

        // Update registry only if there were changes
        if (hasUpdates) {
          updateRegistry(updatedModels);
        }

        // REACTIVE: Immediately refresh alerts for services with component maintenance changes
        if (componentMaintenanceChanges.length > 0) {
          console.log('Component maintenance mode changed for services:', componentMaintenanceChanges);
          // Force refresh of centralized service state data to get updated alerts
          centralizedServiceStateApi.clearCache();
          await centralizedServiceStateApi.fetchAllServiceStatesAndAlerts(clusterName, alertSummary, alertDefinitions);
        }
      }
    } catch (error) {
      console.error('Error fetching optimized maintenance and stale data:', error);
    }
  };

  // Service-level maintenance mode handling with reactive alert updates
  useEffect(() => {
    const fetchMaintenanceModeForService = async () => {
      try {
        const responseData = await ServiceApi.getAllServices(clusterName);
        
        // Collect maintenance states like Ember's passiveStateMap
        const passiveStateMap: { [key: string]: string } = {};
        const changedServices: string[] = [];
        
        responseData.items.forEach((service: any) => {
          passiveStateMap[service.ServiceInfo.service_name] = service.ServiceInfo.maintenance_state;
        });

        // Track which services had maintenance mode changes
        let hasMaintenanceUpdates = false;
        const updatedModels = cloneDeep(allServiceModels);

        Object.entries(passiveStateMap).forEach(([serviceName, maintenanceState]) => {
          const serviceModelKey = serviceNameModelMapping[serviceName];
          if (updatedModels[serviceModelKey]) {
            const currentMaintenanceValue = updatedModels[serviceModelKey].isInPassiveForService;
            const newMaintenanceValue = maintenanceState === "ON";
            
            // Only update if the maintenance state has actually changed
            if (currentMaintenanceValue !== newMaintenanceValue) {
              updatedModels[serviceModelKey].isInPassiveForService = newMaintenanceValue;
              hasMaintenanceUpdates = true;
              changedServices.push(serviceName);
            }
          }
        });

        // Update registry only if there were actual maintenance state changes
        if (hasMaintenanceUpdates) {
          updateRegistry(updatedModels);
          
          // REACTIVE: Immediately refresh alerts for services that changed maintenance mode
          if (changedServices.length > 0) {
            console.log('Maintenance mode changed for services:', changedServices);
            // Force refresh of centralized service state data to get updated alerts
            centralizedServiceStateApi.clearCache();
            await centralizedServiceStateApi.fetchAllServiceStatesAndAlerts(clusterName, alertSummary, alertDefinitions);
          }
        }
      } catch (error) {
        console.error('Error fetching service maintenance mode:', error);
      }
    };

    // Only fetch maintenance mode when models are loaded and stable
    if (allModelsLoaded && Object.keys(allServiceModels).length > 0) {
      fetchMaintenanceModeForService();
    }
  }, [allModelsLoaded, clusterName]);

  // REMOVED: updateStaleConfigsForAllServices - now handled by fetchOptimizedMaintenanceAndStaleData
  // All stale config and maintenance state processing is consolidated in the optimized function

  // Initialize centralized component API polling (Ember.js style)
  useEffect(() => {
    if (clusterName && allModelsLoaded) {
      
      // Start centralized polling for all service components
      cachedServiceApi.startPolling(clusterName, 5000);
      
      // Subscribe to centralized data updates
      const unsubscribe = cachedServiceApi.subscribe((data) => {
        
        // Update BOTH masterSlaveClientsData AND polledHostComponentsData with centralized data
        // This ensures both component names AND status are available immediately
        if (data?.items && !isEqual(previousMasterSlaveClientsData?.items, data.items)) {
          setMasterSlaveClientsData(data.items);
        }
        
        // Also update polledHostComponentsData to provide component status immediately
        if (data?.items && !isEqual(previousHostComponentsData?.items, data.items)) {
          setPolledHostComponentsData(data);
        }
      });

      // Subscribe to centralized service state updates
      const unsubscribeServiceStates = centralizedServiceStateApi.subscribe((data) => {
        setServiceStatesData(data);
      });

      // Start centralized service state and alerts polling with timeout-based approach
      let serviceStateTimeout: NodeJS.Timeout | null = null;
      let isPollingActive = true;

      const pollServiceStates = async () => {
        if (!isPollingActive) return;

        try {
          const statesData = await centralizedServiceStateApi.fetchAllServiceStatesAndAlerts(clusterName, alertSummary, alertDefinitions);
          setServiceStatesData(statesData);
        } catch (error) {
          console.error('Error polling service states:', error);
        } finally {
          // Schedule next poll ONLY after current request completes
          if (isPollingActive) {
            serviceStateTimeout = setTimeout(pollServiceStates, 5000);
          }
        }
      };

      // Start initial poll
      pollServiceStates();

      return () => {
        unsubscribe();
        unsubscribeServiceStates();
        cachedServiceApi.stopPolling();
        isPollingActive = false;
        if (serviceStateTimeout) {
          clearTimeout(serviceStateTimeout);
        }
      };
    }
  }, [clusterName, allModelsLoaded]);

  // Control polling based on current route - pause on cluster admin pages
  useEffect(() => {
    if (isOnClusterAdminPage) {
      cachedServiceApi.pausePolling();
    } else {
      cachedServiceApi.resumePolling(clusterName);
    }
  }, [isOnClusterAdminPage, clusterName]);

  // Use the new optimized polling function that replaces multiple API calls
  // This polling will be controlled by the pausePolling/resumePolling mechanism
  const { pausePolling: pauseMaintenancePolling, resumePolling: resumeMaintenancePolling } = 
    usePolling(fetchOptimizedMaintenanceAndStaleData, 5000);

  // Control maintenance/stale config polling based on route
  useEffect(() => {
    if (isOnClusterAdminPage) {
      pauseMaintenancePolling();
    } else {
      resumeMaintenancePolling();
    }
  }, [isOnClusterAdminPage, pauseMaintenancePolling, resumeMaintenancePolling]);

  return (
    <ServiceContext.Provider
      value={{
        allServiceModels,
        allModelsLoaded,
        serviceModels: allServiceModels,
        updateRegistry,
        polledHostComponentsData,
        quickLinksMapWithAPIResponse,
        masterSlaveClientsData,
        serviceStatesData,
      }}
    >
      <OptimizedUpdater />
      {/* {installedService.includes("hdfs")?<HdfsUpdaterUpdater/>:null} */}
      {children}
    </ServiceContext.Provider>
  );
};

export default ServiceProvider;
