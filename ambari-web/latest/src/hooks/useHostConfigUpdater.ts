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

import { cloneDeep, forEach, get, isEmpty, set } from "lodash";
import { useContext, useEffect, useRef, useState } from "react";
import { AppContext } from "../store/context";
import { HostsApi } from "../api/hostsApi";
import Host, { IHost } from "../models/host";
import { hostMapper } from "../mappers/hostsMapper";
import HostComponent, { IHostComponent } from "../models/hostComponent";
import HostStackVersion, {
  IHostStackVersion,
} from "../models/hostStackVersion";
import { sortBasedOnMasterSlave } from "../screens/Hosts/utils";
import { PassiveStateOnFilters } from "../screens/Hosts/enums";
import usePolling from "./usePolling";

export const useHostConfigUpdater = (
  hostApiQueryParams: any,
  allHostModels: Host[],
  setAllHostModels: Function,
  setTotalItems?: Function,
  setPaginationLoading?: Function
) => {
  const [hostsData, setHostsData] = useState<any>({});
  const queryData = useRef({});
  const allHostModelsRef = useRef<Host[]>(allHostModels);

  const { clusterName, serviceComponentInfo, parsedSocketMessages } =
    useContext(AppContext);

  // Keep the ref updated with the latest allHostModels
  useEffect(() => {
    allHostModelsRef.current = allHostModels;
  }, [allHostModels]);

  useEffect(() => {
    if (parsedSocketMessages.length) {
      switch (get(parsedSocketMessages[0], "destination", "")) {
        case "/events/hostcomponents":
          updateHostComponent(parsedSocketMessages[0]);
          break;
        case "/events/hosts":
          updateHost(parsedSocketMessages[0]);
          break;
        case "/events/requests":
          updateHostOnRequests(parsedSocketMessages[0]);
          break;
        default:
      }
    }
  }, [parsedSocketMessages]);

  useEffect(() => {
    if (!isEmpty(serviceComponentInfo)) {
      getHostsData();
    }
  }, [serviceComponentInfo, hostApiQueryParams]);

  useEffect(() => {
    if (!isEmpty(hostsData)) {
      populateHostModels();
    }
  }, [hostsData]);

  const getHostMetrics = async () => {
    if (isEmpty(queryData.current)) return;
    const newQueryString = get(queryData.current, "RequestInfo.query", "");
    let url = "";
    //For Hosts List Page
    if (isGetHostsList()) {
      const paginationString = `&page_size=${get(
        hostApiQueryParams,
        "pageSize",
        10
      )}&from=${get(hostApiQueryParams, "startFrom", 0)}`;
      url = `metrics/disk/disk_free,metrics/disk/disk_total,metrics/load/load_one&minimal_response=true${
        newQueryString ? "" : paginationString
      }&sortBy=${get(hostApiQueryParams, "sortBy", "Hosts/host_name")}.${get(
        hostApiQueryParams,
        "sortOrder",
        "asc"
      )}`;
    }
    //For Hosts Summary Page
    else {
      url =
        "metrics/disk/disk_free,metrics/disk/disk_total,metrics/load/load_one,host_components/metrics/dfs/namenode/ClusterId,host_components/metrics/jvm/HeapMemoryMax,host_components/metrics/jvm/HeapMemoryUsed,host_components/metrics/dfs/FSNamesystem/CapacityUsed,host_components/metrics/dfs/FSNamesystem/CapacityTotal,host_components/metrics/dfs/FSNamesystem/CapacityRemaining,host_components/metrics/dfs/FSNamesystem/CapacityNonDFSUsed,host_components/metrics/rpc/client/RpcQueueTime_avg_time,host_components/metrics/runtime/StartTime&minimal_response=true";
    }

    const response = await HostsApi.getHostsList(
      clusterName,
      url,
      queryData.current
    );

    if (get(response, "items", []).length) {
      // Use the ref to get the latest allHostModels value, avoiding stale closure
      const allHostModelsCopy = cloneDeep(allHostModelsRef.current);
      get(response, "items", []).forEach((host: any) => {
        const hostName = get(host, "Hosts.host_name", "");
        const hostModel = allHostModelsCopy.find(
          (h: Host) => h.hostName === hostName
        );
        if (hostModel) {
          (
            Object.keys(hostMapper.hostConfig) as Array<
              keyof typeof hostMapper.hostConfig
            >
          ).forEach((key) => {
            set(
              hostModel,
              key,
              get(host, hostMapper.hostConfig[key], get(hostModel, key))
            );
          });
        }
      });
      setAllHostModels(allHostModelsCopy);
    }
  };

  usePolling(getHostMetrics, 15000);

  const updateHostComponent = (message: any) => {
    const config = {
      workStatus: "currentState",
      staleConfigs: "staleConfigs",
      passiveState: "maintenanceState",
    };
    // Use the ref to get the latest allHostModels value
    let allHostModelsCopy = cloneDeep(allHostModelsRef.current);
    get(message, "hostComponents", []).forEach((hostComponent: any) => {
      allHostModelsCopy.forEach((host: Host) => {
        host.hostComponents.forEach((hostComponentModel: HostComponent) => {
          if (
            hostComponentModel.componentName ===
            get(hostComponent, "componentName")
          ) {
            Object.keys(config).forEach((key) => {
              if (get(hostComponent, config[key as keyof typeof config])) {
                set(
                  hostComponentModel,
                  key,
                  get(
                    hostComponent,
                    config[key as keyof typeof config],
                    get(hostComponentModel, key)
                  )
                );
              }
            });
          }
        });
      });
    });
    setAllHostModels(allHostModelsCopy);
  };

  const updateHost = (message: any) => {
    const config = {
      alertsSummary: "alerts_summary",
      healthStatus: "host_status",
      state: "host_state",
      lastHeartBeatTime: "last_heartbeat_time",
      passiveState: "maintenance_state",
    };
    // Use the ref to get the latest allHostModels value
    let allHostModelsCopy = cloneDeep(allHostModelsRef.current);
    allHostModelsCopy.forEach((host: Host) => {
      if (host.hostName === get(message, "host_name", "")) {
        Object.keys(config).forEach((key) => {
          if (get(message, config[key as keyof typeof config])) {
            set(
              host,
              key,
              get(message, config[key as keyof typeof config], get(host, key))
            );

            if (key === "passiveState") {
              handlePassiveStateChange(config, key, message, host);
            }
          }
        });
      }
    });
    setAllHostModels(allHostModelsCopy);
  };

  const updateHostOnRequests = (message: any) => {

    const requestContext = get(message, "requestContext", "").toLowerCase();
    const requestStatus = get(message, "requestStatus", "");
    const hostName = get(message, "Tasks.[0].hostName", "");

    if (requestStatus !== "COMPLETED" || !hostName) {
      return;
    }

    let operation = "";
    let componentType = "";

    if (requestContext.includes("decommission")) {
      operation = "DECOMMISSION";
      if (requestContext.includes("datanode")) {
        componentType = "DATANODE";
      } else if (requestContext.includes("nodemanager")) {
        componentType = "NODEMANAGER";
      } else if (requestContext.includes("regionserver")) {
        componentType = "HBASE_REGIONSERVER";
      } else if (requestContext.includes("tasktracker")) {
        componentType = "TASKTRACKER";
      }
    } else if (requestContext.includes("recommission")) {
      operation = "RECOMMISSION";
      if (requestContext.includes("datanode")) {
        componentType = "DATANODE";
      } else if (requestContext.includes("nodemanager")) {
        componentType = "NODEMANAGER";
      } else if (requestContext.includes("regionserver")) {
        componentType = "HBASE_REGIONSERVER";
      } else if (requestContext.includes("tasktracker")) {
        componentType = "TASKTRACKER";
      }
    }

    if (operation && componentType) {
      updateComponentAdminState(hostName, componentType, operation);
    }
  };

  const updateComponentAdminState = (
    hostName: string,
    componentType: string,
    operation: string
  ) => {
    // Use the ref to get the latest allHostModels value
    const allHostModelsCopy = cloneDeep(allHostModelsRef.current);

    allHostModelsCopy.forEach((host: Host) => {
      if (host.hostName === hostName) {
        host.hostComponents.forEach((hostComponent: HostComponent) => {
          if (hostComponent.componentName === componentType) {
            if (operation === "DECOMMISSION") {
              set(hostComponent, "adminState", "DECOMMISSIONED");
            } else if (operation === "RECOMMISSION") {
              set(hostComponent, "adminState", "INSERVICE");
            }
          }
        });
      }
    });

    setAllHostModels(allHostModelsCopy);
  };

  const handlePassiveStateChange = (
    config: any,
    key: string,
    message: any,
    host: Host
  ) => {
    host.hostComponents.forEach((hostComponent: HostComponent) => {
      const currentPassiveState = get(hostComponent, "passiveState", "");
      const desiredPassiveState = get(
        message,
        config[key as keyof typeof config],
        get(host, key)
      );
      if (desiredPassiveState !== currentPassiveState) {
        if (desiredPassiveState === "OFF") {
          if (
            currentPassiveState ===
            PassiveStateOnFilters.IMPLIED_FROM_SERVICE_AND_HOST
          ) {
            set(
              hostComponent,
              "passiveState",
              PassiveStateOnFilters.IMPLIED_FROM_SERVICE
            );
          } else if (
            currentPassiveState === PassiveStateOnFilters.IMPLIED_FROM_HOST
          ) {
            set(hostComponent, "passiveState", desiredPassiveState);
          }
        } else {
          if (
            currentPassiveState === PassiveStateOnFilters.IMPLIED_FROM_SERVICE
          ) {
            set(
              hostComponent,
              "passiveState",
              PassiveStateOnFilters.IMPLIED_FROM_SERVICE_AND_HOST
            );
          } else if (currentPassiveState === "OFF") {
            set(
              hostComponent,
              "passiveState",
              PassiveStateOnFilters.IMPLIED_FROM_HOST
            );
          }
        }
      }
    });
  };

  const getHostNamesForCurrentFilters = async (url: string) => {
    const response = await HostsApi.getHostComponentsDetails(clusterName, url);
    const hostNames = get(response, "items", []).map((host: any) =>
      get(host, "Hosts.host_name", "")
    );
    if (setTotalItems) {
      setTotalItems(parseInt(get(response, "itemTotal", "0"), 10));
    }
    const newQueryString = hostNames.length
      ? `Hosts/host_name.in(${hostNames.join(",")})`
      : "";
    return newQueryString;
  };

  const isGetHostsList = () => {
    return (
      get(hostApiQueryParams, "pageSize", 0) > 0 &&
      get(hostApiQueryParams, "startFrom", -1) > -1 &&
      get(hostApiQueryParams, "sortBy", "") &&
      get(hostApiQueryParams, "sortOrder", "")
    );
  };

  const getHostsData = async () => {
    const queryString = get(hostApiQueryParams, "RequestInfo.query", "");

    const data = {
      RequestInfo: get(hostApiQueryParams, "RequestInfo", {}),
    };

    let newQueryString = "";
    if (queryString.includes("host_components")) {
      newQueryString = await getHostNamesForCurrentFilters(queryString);
      if (!newQueryString) {
        // If no host names are found, we can return early
        setHostsData({ items: [], itemTotal: 0 });
        return;
      }
      data.RequestInfo.query = newQueryString;
    }

    queryData.current = data;
    let url = "";

    //For Hosts List Page
    if (isGetHostsList()) {
      const paginationString = `&page_size=${get(
        hostApiQueryParams,
        "pageSize",
        10
      )}&from=${get(hostApiQueryParams, "startFrom", 0)}`;
      url = `Hosts/rack_info,Hosts/host_name,Hosts/maintenance_state,Hosts/public_host_name,Hosts/cpu_count,Hosts/ph_cpu_count,Hosts/last_agent_env,alerts_summary,Hosts/host_status,Hosts/host_state,Hosts/last_heartbeat_time,Hosts/ip,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/service_name,host_components/HostRoles/display_name,host_components/HostRoles/desired_admin_state,host_components/metrics/dfs/namenode/ClusterId,host_components/metrics/dfs/FSNamesystem/HAState,metrics/disk,metrics/load/load_one,Hosts/total_mem,Hosts/os_arch,Hosts/os_type,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free,stack_versions/HostStackVersions,stack_versions/repository_versions/RepositoryVersions/repository_version,stack_versions/repository_versions/RepositoryVersions/id,stack_versions/repository_versions/RepositoryVersions/display_name&minimal_response=true,host_components/logging${
        newQueryString ? "" : paginationString
      }&sortBy=${get(hostApiQueryParams, "sortBy", "Hosts/host_name")}.${get(
        hostApiQueryParams,
        "sortOrder",
        "asc"
      )}`;
    }
    //For Hosts Summary Page
    else {
      url =
        "Hosts/rack_info,Hosts/host_name,Hosts/maintenance_state,Hosts/public_host_name,Hosts/cpu_count,Hosts/ph_cpu_count,Hosts/last_agent_env,alerts_summary,Hosts/host_status,Hosts/host_state,Hosts/last_heartbeat_time,Hosts/ip,host_components/HostRoles/state,host_components/HostRoles/maintenance_state,host_components/HostRoles/stale_configs,host_components/HostRoles/service_name,host_components/HostRoles/display_name,host_components/HostRoles/desired_admin_state,host_components/metrics/dfs/namenode/ClusterId,host_components/metrics/dfs/FSNamesystem/HAState,metrics/disk,metrics/load/load_one,Hosts/total_mem,Hosts/os_arch,Hosts/os_type,metrics/cpu/cpu_system,metrics/cpu/cpu_user,metrics/memory/mem_total,metrics/memory/mem_free,stack_versions/HostStackVersions,stack_versions/repository_versions/RepositoryVersions/repository_version,stack_versions/repository_versions/RepositoryVersions/id,stack_versions/repository_versions/RepositoryVersions/display_name&minimal_response=true,host_components/logging&sortBy=Hosts/host_name.asc";
    }

    const response = await HostsApi.getHostsList(clusterName, url, data);

    let allComponents: any[] = [];
    get(serviceComponentInfo, "items", []).forEach((service: any) => {
      allComponents = allComponents.concat(
        get(service, "components", []).map((component: any) => {
          return {
            HostRoles: {
              ...get(component, "StackServiceComponents"),
              dependencies: get(component, "dependencies", []).map(
                (d: any) => d.Dependencies.component_name
              ),
            },
          };
        })
      );
    });

    // If the response contains itemTotal, set it to totalItems
    if (!newQueryString && setTotalItems) {
      setTotalItems(parseInt(get(response, "itemTotal", "0"), 10));
    }

    get(response, "items", []).forEach((host: any) => {
      get(host, "host_components", []).forEach((component: any) =>
        set(component, "HostRoles", {
          ...get(component, "HostRoles", {}),
          ...allComponents.filter(
            (c) =>
              get(c, "HostRoles.component_name") ===
              get(component, "HostRoles.component_name")
          )[0].HostRoles,
        })
      );
      if (!isGetHostsList()) {
        set(
          host,
          "host_components",
          sortBasedOnMasterSlave(
            get(host, "host_components", []),
            "HostRoles.component_category"
          )
        );
      }
    });
    setHostsData(response);
  };

  const populateHostComponentModels = (hostComponent: any) => {
    const hostComponentModel = new HostComponent({} as IHostComponent);
    (
      Object.keys(hostMapper.hostComponentConfig) as Array<
        keyof typeof hostMapper.hostComponentConfig
      >
    ).map((key) => {
      set(
        hostComponentModel,
        key,
        get(hostComponent, hostMapper.hostComponentConfig[key])
      );
    });
    return hostComponentModel;
  };

  const populateHostStackVersionModels = (hostStackVersion: any) => {
    const hostStackVersionModel = new HostStackVersion({} as IHostStackVersion);
    (
      Object.keys(hostMapper.hostStackVersionConfig) as Array<
        keyof typeof hostMapper.hostStackVersionConfig
      >
    ).map((key) => {
      set(
        hostStackVersionModel,
        key,
        get(hostStackVersion, hostMapper.hostStackVersionConfig[key])
      );
    });
    return hostStackVersionModel;
  };

  const populateHostModels = () => {
    const allHostModelsCopy = [] as Host[];
    forEach(get(hostsData, "items", []), (host: any, index: number) => {
      const hostModel = new Host({} as IHost);
      (
        Object.keys(hostMapper.hostConfig) as Array<
          keyof typeof hostMapper.hostConfig
        >
      ).map((key) => {
        set(hostModel, key, get(host, hostMapper.hostConfig[key]));
      });
      set(hostModel, "index", index);
      const hostComponents = get(host, "host_components", []);
      forEach(hostComponents, (hostComponent: any) => {
        const hostComponentModel = populateHostComponentModels(hostComponent);
        hostModel.hostComponents.push(hostComponentModel);
      });
      const stackVersions = get(host, "stack_versions", []);
      forEach(stackVersions, (hostStackVersion: any) => {
        const hostStackVersionModel =
          populateHostStackVersionModels(hostStackVersion);
        hostModel.stackVersions.push(hostStackVersionModel);
      });
      allHostModelsCopy.push(hostModel);
    });
    setAllHostModels(allHostModelsCopy);

    if (setPaginationLoading) {
      setPaginationLoading(false);
    }
  };
};
