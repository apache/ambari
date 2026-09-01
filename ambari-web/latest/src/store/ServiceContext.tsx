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

import React, { useContext, useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import HDFSService from "../models/hdfs";
import { cloneDeep, isEmpty } from "lodash";
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
  polledHostComponentsData: any;
  quickLinksMapWithAPIResponse: Map<string, any>;
  masterSlaveClientsData: any;
  serviceStatesData: Map<string, any>;
  applyServiceMaintenanceChange: (serviceName: string, newState: string) => void;
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
  applyServiceMaintenanceChange: () => {},
});

interface ServiceProviderProps {
  children: any;
}

/**
 * Compute alert counts per service from alert summary and definitions.
 * Same logic as CentralizedServiceStateApi.calculateServiceAlertCounts but without the API call.
 */
function computeServiceAlertCounts(
  alertSummary?: { alerts_summary_grouped: any[] },
  alertDefinitions?: any[]
): Map<string, { alertsCount: number; hasCriticalAlerts: boolean }> {
  const serviceAlerts = new Map<string, { alertsCount: number; hasCriticalAlerts: boolean }>();

  if (!alertSummary?.alerts_summary_grouped || !alertDefinitions) {
    return serviceAlerts;
  }

  const definitionIdToService = new Map<number, string>();
  alertDefinitions.forEach((def: any) => {
    if (def.id && def.service_name) {
      definitionIdToService.set(def.id, def.service_name);
    }
  });

  alertSummary.alerts_summary_grouped.forEach((alert: any) => {
    const definitionId = alert.definition_id;
    if (!definitionId) return;

    const serviceName = definitionIdToService.get(definitionId);
    if (!serviceName) return;

    const criticalCount = alert.summary?.CRITICAL?.count || 0;
    const warningCount = alert.summary?.WARNING?.count || 0;
    const totalCount = criticalCount + warningCount;
    const hasCritical = criticalCount > 0;

    if (!serviceAlerts.has(serviceName)) {
      serviceAlerts.set(serviceName, { alertsCount: 0, hasCriticalAlerts: false });
    }

    const current = serviceAlerts.get(serviceName)!;
    current.alertsCount += totalCount;
    current.hasCriticalAlerts = current.hasCriticalAlerts || hasCritical;
  });

  return serviceAlerts;
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
  // Keep a ref in sync so fetchOptimizedMaintenanceAndStaleData always reads the
  // latest polled/WebSocket-updated host component data (avoids stale closure).
  const polledHostComponentsDataRef = useRef(polledHostComponentsData);
  useEffect(() => {
    polledHostComponentsDataRef.current = polledHostComponentsData;
  }, [polledHostComponentsData]);
  const [quickLinksMapWithAPIResponse, setQuickLinksMapWithAPIResponse] =
    useState<any>(null);

  const [masterSlaveClientsData, setMasterSlaveClientsData] = useState<any>({});
  const [serviceStatesData, setServiceStatesData] = useState<Map<string, any>>(new Map());

  const { clusterName, parsedSocketMessages, alertSummary: socketAlertSummary } = useContext(AppContext);

  // Boot-fetched summary from AlertsContext; prefer the synchronous socket summary when available.
  // socketAlertSummary is set in the same render cycle as the socket arrival (context.tsx handler),
  // avoiding the extra render cycle that parsedSocketMessages → AlertsContext useEffect requires.
  const { alertSummary: bootAlertSummary, alertDefinitions } = useAlerts();
  const alertSummary = socketAlertSummary ?? bootAlertSummary;

  // Refs to avoid stale closures in subscriber callback and prevent useEffect re-runs
  const alertSummaryRef = useRef(alertSummary);
  const alertDefinitionsRef = useRef(alertDefinitions);
  const allServiceModelsRef = useRef(allServiceModels);

  useEffect(() => { alertSummaryRef.current = alertSummary; }, [alertSummary]);
  useEffect(() => { alertDefinitionsRef.current = alertDefinitions; }, [alertDefinitions]);
  useEffect(() => { allServiceModelsRef.current = allServiceModels; }, [allServiceModels]);

  // REACTIVE ALERT UPDATE: When alertSummary changes (from synchronous socket path or boot load),
  // immediately recompute service alert counts and push to serviceStatesData.
  // Mirrors Ember's alertDefinitionSummaryMapper which runs synchronously on socket events,
  // instantly updating service.alertsCount + service.hasCriticalAlerts.
  useEffect(() => {
    if (!alertSummary || !alertDefinitions?.length) return;

    const serviceAlertCounts = computeServiceAlertCounts(alertSummary, alertDefinitions);

    setServiceStatesData((prev) => {
      const updated = new Map(prev);
      serviceAlertCounts.forEach(({ alertsCount, hasCriticalAlerts }, serviceName) => {
        const existing = updated.get(serviceName);
        if (existing) {
          updated.set(serviceName, { ...existing, alertsCount, hasCriticalAlerts });
        }
      });
      updated.forEach((data, serviceName) => {
        if (!serviceAlertCounts.has(serviceName)) {
          updated.set(serviceName, { ...data, alertsCount: 0, hasCriticalAlerts: false });
        }
      });
      centralizedServiceStateApi.setDerivedServiceStates(updated);
      return updated;
    });
  }, [alertSummary, alertDefinitions]);

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

  const modelHasComponentData = (model: any): boolean =>
    !!model &&
    ((Array.isArray(model.masterComponents) && model.masterComponents.length > 0) ||
      (Array.isArray(model.slaveComponents) && model.slaveComponents.length > 0) ||
      (Array.isArray(model.clientComponents) && model.clientComponents.length > 0));

  const updateRegistry = (updatedModels: any) => {
    if (!updatedModels) return;
    setAllServiceModels((prev: any) => {
      const merged: any = { ...prev };
      for (const key of Object.keys(updatedModels)) {
        const incoming = updatedModels[key];
        const existing = prev[key];
        if (modelHasComponentData(existing) && !modelHasComponentData(incoming)) {
          continue;
        }
        merged[key] = incoming;
      }
      // Keep the ref in sync synchronously so multiple updateRegistry calls within
      // the same tick each clone the freshest models instead of a stale snapshot,
      // which previously clobbered isRestartRequiredForService.
      allServiceModelsRef.current = merged;
      return merged;
    });
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
      // Prefer the reactive polledHostComponentsData state: the WebSocket
      // /events/hostcomponents handler updates it immediately with fresh
      // stale_configs, whereas cachedServiceApi.getAllComponentData() is only
      // refreshed by the 5s HTTP poll. Reading the cache here meant the Sidebar
      // restart icon lagged until the next poll after a config save.
      const polledData = polledHostComponentsDataRef.current;
      const responseData =
        polledData?.items?.length
          ? polledData
          : cachedServiceApi.getAllComponentData();

      if (!responseData?.items?.length) {
        // If no data yet, fetch it once (will be cached for subsequent calls)
        await cachedServiceApi.fetchAllServiceComponents(clusterName);
        return;
      }

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

        // Update service models with stale configs only (maintenance mode handled separately).
        // Use the ref (not the closed-over allServiceModels) so we always read the latest
        // models — this closure captures a stale value otherwise, preventing the
        // Sidebar restart icon from updating after configs are saved.
        let hasUpdates = false;
        const updatedModels = cloneDeep(allServiceModelsRef.current);

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

        // polledHostComponentsData and masterSlaveClientsData are now set by the CachedServiceApi subscriber
        // No need to set them here - this function only processes stale config logic

        // Update registry only if there were changes
        if (hasUpdates) {
          updateRegistry(updatedModels);
        }

        // Service states (including alerts) are now recomputed in the CachedServiceApi subscriber
        // whenever components data refreshes - no separate API call needed
      }
    } catch (error) {
      console.error('Error fetching optimized maintenance and stale data:', error);
    }
  };

  // Service-level state and maintenance mode initial load (mirrors Ember's serviceMapper)
  // Reads ServiceInfo.state and ServiceInfo.maintenance_state from backend - these are
  // the AUTHORITATIVE source of truth (Ember does NOT derive workStatus from component counts).
  useEffect(() => {
    const fetchMaintenanceModeForService = async () => {
      try {
        const responseData = await ServiceApi.getAllServices(clusterName);

        // Collect maintenance states like Ember's passiveStateMap
        const passiveStateMap: { [key: string]: string } = {};

        responseData.items.forEach((service: any) => {
          passiveStateMap[service.ServiceInfo.service_name] = service.ServiceInfo.maintenance_state;
        });

        // Use the ref (not the closed-over allServiceModels) so this clone is not stale and
        // does not clobber isRestartRequiredForService written by fetchOptimizedMaintenanceAndStaleData.
        let hasMaintenanceUpdates = false;
        const updatedModels = cloneDeep(allServiceModelsRef.current);

        Object.entries(passiveStateMap).forEach(([serviceName, maintenanceState]) => {
          const serviceModelKey = serviceNameModelMapping[serviceName];
          if (updatedModels[serviceModelKey]) {
            const currentMaintenanceValue = updatedModels[serviceModelKey].isInPassiveForService;
            const newMaintenanceValue = maintenanceState === "ON";

            if (currentMaintenanceValue !== newMaintenanceValue) {
              updatedModels[serviceModelKey].isInPassiveForService = newMaintenanceValue;
              hasMaintenanceUpdates = true;
            }
          }
        });

        if (hasMaintenanceUpdates) {
          updateRegistry(updatedModels);
        }

        // Populate serviceStatesData with authoritative state from backend ServiceInfo.state
        // (Ember's service_mapper.js: work_status: 'ServiceInfo.state')
        setServiceStatesData((prev) => {
          const updated = new Map(prev);
          const serviceAlertCounts = computeServiceAlertCounts(alertSummaryRef.current, alertDefinitionsRef.current);
          responseData.items.forEach((service: any) => {
            const serviceName = service.ServiceInfo.service_name;
            const alertData = serviceAlertCounts.get(serviceName) || { alertsCount: 0, hasCriticalAlerts: false };
            const existing = updated.get(serviceName) || {};
            updated.set(serviceName, {
              ...existing,
              serviceName,
              state: service.ServiceInfo.state,
              maintenance_state: service.ServiceInfo.maintenance_state,
              alertsCount: alertData.alertsCount,
              hasCriticalAlerts: alertData.hasCriticalAlerts,
            });
          });
          centralizedServiceStateApi.setDerivedServiceStates(updated);
          return updated;
        });
      } catch (error) {
        console.error('Error fetching service maintenance mode:', error);
      }
    };

    if (allModelsLoaded && Object.keys(allServiceModels).length > 0) {
      fetchMaintenanceModeForService();
    }
  }, [allModelsLoaded, clusterName]);

  // REMOVED: updateStaleConfigsForAllServices - now handled by fetchOptimizedMaintenanceAndStaleData
  // All stale config and maintenance state processing is consolidated in the optimized function

  // Centralized component API polling using usePolling hook (mirrors Ember's updateServiceMetric)
  // ONE poll drives all data updates - no separate /services poll needed
  const clusterNameRef = useRef(clusterName);
  useEffect(() => { clusterNameRef.current = clusterName; }, [clusterName]);

  // Process components data and update derived state.
  // Called both from the subscriber (when ANY caller fetches data) and from pollServiceComponents.
  // IMPORTANT: This does NOT derive service `state` from component counts. Ember treats
  // ServiceInfo.state as the authoritative source (set on initial /services load and via
  // /events/services WebSocket). We preserve existing state and only refresh alerts +
  // maintenance_state (which tracks the model's isInPassiveForService flag).
  const processComponentsData = (data: any) => {
    if (!data?.items) return;

    setMasterSlaveClientsData(data.items);
    setPolledHostComponentsData(data);

    const presentServiceNames = new Set<string>();
    data.items.forEach((item: any) => {
      const serviceName = item.ServiceComponentInfo?.service_name;
      if (serviceName) presentServiceNames.add(serviceName);
    });

    const serviceAlertCounts = computeServiceAlertCounts(alertSummaryRef.current, alertDefinitionsRef.current);

    setServiceStatesData((prev) => {
      const updated = new Map(prev);
      presentServiceNames.forEach((serviceName) => {
        const serviceModelKey = serviceNameModelMapping[serviceName];
        const serviceModel = allServiceModelsRef.current[serviceModelKey];
        const maintenance_state = serviceModel?.isInPassiveForService ? 'ON' : 'OFF';
        const alertData = serviceAlertCounts.get(serviceName) || { alertsCount: 0, hasCriticalAlerts: false };
        const existing = updated.get(serviceName) || { serviceName, state: 'INSTALLED' };

        updated.set(serviceName, {
          ...existing,
          serviceName,
          maintenance_state,
          alertsCount: alertData.alertsCount,
          hasCriticalAlerts: alertData.hasCriticalAlerts,
        });
      });
      centralizedServiceStateApi.setDerivedServiceStates(updated);
      return updated;
    });
  };

  // Subscribe to cachedServiceApi - notified whenever ANY caller (e.g. useHDFSConfigUpdater
  // calling fetchAllServiceComponents directly) returns fresh data.
  // This ensures state updates flow regardless of which code path initiated the fetch.
  useEffect(() => {
    const unsubscribe = cachedServiceApi.subscribe(processComponentsData);
    return () => unsubscribe();
  }, []);

  const pollServiceComponents = async () => {
    const currentClusterName = clusterNameRef.current;
    if (!currentClusterName || !allModelsLoaded) return;

    // fetchAllServiceComponents notifies subscribers internally - state will be updated via processComponentsData
    await cachedServiceApi.fetchAllServiceComponents(currentClusterName);
  };

  // Eager first fetch - fires immediately when clusterName becomes available
  useEffect(() => {
    if (clusterName && allModelsLoaded) {
      pollServiceComponents();
    }
  }, [clusterName, allModelsLoaded]);

  // Subsequent polling every 5s via usePolling hook
  const pollingInterval = (clusterName && allModelsLoaded) ? 5000 : null;
  //@ts-ignore
  const { pausePolling, resumePolling } = usePolling(pollServiceComponents, pollingInterval);

  // Pause on cluster admin pages, resume otherwise
  useEffect(() => {
    if (isOnClusterAdminPage) {
      pausePolling();
    } else {
      resumePolling();
    }
  }, [isOnClusterAdminPage]);

  // Process maintenance and stale config data reactively when polled data changes
  // This replaces the independent 5s usePolling timer - no separate poll needed
  useEffect(() => {
    if (polledHostComponentsData?.items && allModelsLoaded) {
      fetchOptimizedMaintenanceAndStaleData();
    }
  }, [polledHostComponentsData]);

  // Apply a service-level maintenance state change to all derived state.
  // Mirrors Ember's pattern of doing optimistic UI updates after the API call returns
  // (see ui/app/controllers/main/service/item.js:1198 - self.set('content.passiveState', params.passive_state))
  // Called both from the WebSocket /events/services handler and from optimistic UI updates
  // in Actions.tsx after the toggleMaintenanceMode API call returns.
  const applyServiceMaintenanceChange = (service_name: string, maintenance_state: string) => {
    const serviceModelKey = serviceNameModelMapping[service_name];
    if (serviceModelKey && allServiceModelsRef.current[serviceModelKey]) {
      const updatedModels = cloneDeep(allServiceModelsRef.current);
      updatedModels[serviceModelKey].isInPassiveForService = maintenance_state === 'ON';
      updateRegistry(updatedModels);
    }
    setServiceStatesData((prev) => {
      const updated = new Map(prev);
      const existing = updated.get(service_name) || { serviceName: service_name, state: 'INSTALLED', maintenance_state: 'OFF', alertsCount: 0, hasCriticalAlerts: false };
      updated.set(service_name, { ...existing, maintenance_state });
      centralizedServiceStateApi.setDerivedServiceStates(updated);
      return updated;
    });

    // Cascade to all components of this service
    const cascadeValue = maintenance_state === 'ON' ? 'IMPLIED_FROM_SERVICE' : 'OFF';
    setPolledHostComponentsData((prev: any) => {
      if (!prev?.items) return prev;
      const updated = cloneDeep(prev);
      updated.items.forEach((item: any) => {
        if (item.ServiceComponentInfo?.service_name !== service_name) return;
        item.host_components?.forEach((hc: any) => {
          if (hc.HostRoles) hc.HostRoles.maintenance_state = cascadeValue;
        });
      });
      return updated;
    });
    setMasterSlaveClientsData((prev: any[]) => {
      if (!Array.isArray(prev) || prev.length === 0) return prev;
      const updated = cloneDeep(prev);
      updated.forEach((item: any) => {
        if (item.ServiceComponentInfo?.service_name !== service_name) return;
        item.host_components?.forEach((hc: any) => {
          if (hc.HostRoles) hc.HostRoles.maintenance_state = cascadeValue;
        });
      });
      return updated;
    });
  };

  // WebSocket /events/hostcomponents and /events/services handler
  // Mirrors Ember's hostComponentStatusMapper + serviceStateMapper: merges WebSocket events
  // directly into polledHostComponentsData and allServiceModels so UI updates instantly
  // without waiting for the next 5s poll.
  const lastProcessedSocketMessageRef = useRef<any>(null);
  useEffect(() => {
    if (!parsedSocketMessages?.length) return;

    const latestMessage = parsedSocketMessages[0];
    if (latestMessage === lastProcessedSocketMessageRef.current) return;
    lastProcessedSocketMessageRef.current = latestMessage;

    const destination = latestMessage?.destination;

    // /events/hostcomponents - update host component state in polledHostComponentsData
    // Ember mapping: workStatus<-currentState, staleConfigs<-staleConfigs, passiveState<-maintenanceState
    if (destination === '/events/hostcomponents' && Array.isArray(latestMessage.hostComponents)) {
      setPolledHostComponentsData((prev: any) => {
        if (!prev?.items) return prev;
        const updated = cloneDeep(prev);
        latestMessage.hostComponents.forEach((evt: any) => {
          const componentName = evt.componentName;
          const hostName = evt.hostName;
          const componentItem = updated.items.find(
            (item: any) => item.ServiceComponentInfo?.component_name === componentName
          );
          if (!componentItem) return;
          const hostComp = componentItem.host_components?.find(
            (hc: any) => hc.HostRoles?.host_name === hostName
          );
          if (!hostComp) return;
          if (evt.currentState !== undefined) hostComp.HostRoles.state = evt.currentState;
          if (evt.staleConfigs !== undefined) hostComp.HostRoles.stale_configs = evt.staleConfigs;
          if (evt.maintenanceState !== undefined) hostComp.HostRoles.maintenance_state = evt.maintenanceState;
        });
        return updated;
      });
      setMasterSlaveClientsData((prev: any[]) => {
        if (!Array.isArray(prev) || prev.length === 0) return prev;
        const updated = cloneDeep(prev);
        latestMessage.hostComponents.forEach((evt: any) => {
          const componentItem = updated.find(
            (item: any) => item.ServiceComponentInfo?.component_name === evt.componentName
          );
          if (!componentItem) return;
          const hostComp = componentItem.host_components?.find(
            (hc: any) => hc.HostRoles?.host_name === evt.hostName
          );
          if (!hostComp) return;
          if (evt.currentState !== undefined) hostComp.HostRoles.state = evt.currentState;
          if (evt.staleConfigs !== undefined) hostComp.HostRoles.stale_configs = evt.staleConfigs;
          if (evt.maintenanceState !== undefined) hostComp.HostRoles.maintenance_state = evt.maintenanceState;
        });
        return updated;
      });
    }

    // /events/services - update service state and maintenance_state
    // Ember mapping: workStatus<-state, passiveState<-maintenance_state
    if (destination === '/events/services' && latestMessage.service_name) {
      const { service_name, state, maintenance_state } = latestMessage;

      // Apply maintenance state change (updates models, serviceStatesData, and cascades to components)
      if (maintenance_state !== undefined) {
        applyServiceMaintenanceChange(service_name, maintenance_state);
      }

      // Update state field independently
      if (state !== undefined) {
        setServiceStatesData((prev) => {
          const updated = new Map(prev);
          const existing = updated.get(service_name) || { serviceName: service_name, state: 'INSTALLED', maintenance_state: 'OFF', alertsCount: 0, hasCriticalAlerts: false };
          updated.set(service_name, { ...existing, state });
          centralizedServiceStateApi.setDerivedServiceStates(updated);
          return updated;
        });
      }
    }
  }, [parsedSocketMessages]);

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
        applyServiceMaintenanceChange,
      }}
    >
      <OptimizedUpdater />
      {/* {installedService.includes("hdfs")?<HdfsUpdaterUpdater/>:null} */}
      {children}
    </ServiceContext.Provider>
  );
};

export default ServiceProvider;
