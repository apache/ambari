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

import { set } from "lodash";
import { ambariApi } from "./config/axiosConfig";
import { buildHostSuggestionPredicate } from "../Utils/hosts";

export const HostsApi = {
  getAllHosts: async function (clusterName: string) {
    const url = `clusters/${clusterName}/hosts`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getHostsList: async function (
    clusterName: string,
    fields: string,
    data: any
  ) {
    const url = `clusters/${clusterName}/hosts?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: data,
      headers: {
        "X-Http-Method-Override": "GET",
      },
    });
    return response.data;
  },
  registerHostToComponent: async function (
    clusterName: string,
    requestData: any
  ) {
    const url = `/clusters/${encodeURIComponent(clusterName)}/hosts`;
    const response = await ambariApi.request({
      url,
      method: "POST",
      data: requestData,
    });
    return response;
  },
  updateHost: async function (clusterName: string, data: any) {
    const url = `clusters/${clusterName}/hosts`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data,
    });
    return response.data;
  },
  getHostData: async function (
    clusterName: string,
    hostName: string,
    fields: string
  ) {
    const url = `/clusters/${encodeURIComponent(clusterName)}/hosts/${encodeURIComponent(hostName)}?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getMasterSlaveClusterComponentsByComponentName: async function (
    clusterName: string,
    componentNames: string[],
    fields: string
  ) {
    const componentQuery = componentNames
      .map((name) => `ServiceComponentInfo/component_name=${name}`)
      .join("|");
    const url = `/clusters/${clusterName}/components/?${componentQuery}|ServiceComponentInfo/category.in(MASTER,CLIENT)&fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getClusterComponents: async function (clusterName: string, fields: string) {
    const url = `clusters/${clusterName}/components/?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  makeRequest: async function (data: any) {
    const url = `requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: data,
    });
    return response.data;
  },
  getRequestStatus: async function (requestID: number, fields: string) {
    const url = `requests/${requestID}?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  updateHostComponentForHost: async function (
    clusterName: string,
    hostName: string,
    componentName: string,
    data: any
  ) {
    const url = `clusters/${clusterName}/hosts/${hostName}/host_components/${componentName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data,
    });
    return response.data;
  },
  updateHostComponentsForHost: async function (
    clusterName: string,
    hostName: string,
    urlParams: string,
    data: any
  ) {
    const url = `clusters/${clusterName}/hosts/${hostName}/host_components?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data,
    });
    return response.data;
  },
  getHostStatus: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/hosts?fields=Hosts/host_state,host_components/HostRoles/state`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getNnCheckPointTime: async function (clusterName: string, hostName: string) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/NAMENODE?fields=metrics/dfs/FSNamesystem/HAState,metrics/dfs/FSNamesystem/LastCheckpointTime`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  updateHostComponentPassiveState: async function (
    clusterName: string,
    hostName: string,
    componentName: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/${componentName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: {
        RequestInfo: {
          context: data.context,
        },
        Body: {
          HostRoles: {
            maintenance_state: data.passive_state,
          },
        },
      },
    });
    set(response, "data.status", response.status);
    return response.data;
  },
  updateHostComponents: async function (
    clusterName: string,
    urlParams: string,
    data: any
  ) {
    const url = `/clusters/${encodeURIComponent(clusterName)}/host_components?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      headers: {
        "Content-Type": "text/plain",
      },
      data: JSON.stringify({
        RequestInfo: {
          context: data.context,
          operation_level: {
            level: data.level || "CLUSTER",
            cluster_name: clusterName,
          },
          query: data.query,
        },
        Body: {
          HostRoles: data.HostRoles,
        },
      }),
    });
    set(response, "data.status", response?.status)
    return response.data;
  },
  deleteHostComponent: async function (
    clusterName: string,
    hostName: string,
    componentName: string
  ) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/${componentName}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    });
    set(response, "data.status", response.status);
    return response.data;
  },
  deleteHostComponents: async function (data: any, clusterName: string) {
    const url = `/clusters/${clusterName}/host_components`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
      data: data,
    });
    return response;
  },
  clusterRequests: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}/requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: data,
    });
     set(response, "data.status", response?.status)
    return response.data;
  },
  getHostsData: async function (fields: string) {
    const url = `/hosts?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getHostComponentsDetails: async function (
    clusterName: string,
    fields: string
  ) {
    const url = `/clusters/${clusterName}/hosts?${fields}&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getHostsBulkOperations: async function (clusterName: string, data: any) {
    const url =
      `/clusters/${clusterName}/hosts?fields=Hosts/host_name,Hosts/host_state,Hosts/maintenance_state,` +
      "host_components/HostRoles/state,host_components/HostRoles/maintenance_state," +
      "Hosts/total_mem,stack_versions/HostStackVersions,stack_versions/repository_versions/RepositoryVersions/repository_version," +
      "stack_versions/repository_versions/RepositoryVersions/id," +
      "host_components/HostRoles/stale_configs," +
      "host_components/HostRoles/service_name&minimal_response=true";
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: {
        RequestInfo: { query: data.parameters },
      },
      headers: {
        "X-Http-Method-Override": "GET",
      },
    });
    return response.data;
  },
  getHostComponents: async function (
    clusterName: string,
    fields: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/hosts?${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: {
        RequestInfo: { query: data.parameters },
      },
      headers: {
        "X-Http-Method-Override": "GET",
      },
    });
    return response.data;
  },
  getInstalledHostsForHostComponents: async function (
    clusterName: string,
    componentName: string,
    hostNames: string
  ) {
    const url = `/clusters/${clusterName}/host_components?HostRoles/component_name=${componentName}&HostRoles/host_name.in(${hostNames})&fields=HostRoles/host_name&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getRegionServerPassiveState: async function (
    clusterName: string,
    hostNames: string
  ) {
    const url = `/clusters/${clusterName}/host_components?HostRoles/component_name=HBASE_REGIONSERVER&HostRoles/maintenance_state=OFF&HostRoles/desired_admin_state=INSERVICE&HostRoles/host_name.in(${hostNames})`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getResgionServerInService: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/host_components?HostRoles/component_name=HBASE_REGIONSERVER&HostRoles/desired_admin_state=INSERVICE&fields=HostRoles/host_name&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  deleteHosts: async function (
    clusterName: string,
    urlParams: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/hosts?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
      data: {
        RequestInfo: {
          query: data.query,
        },
      },
    });
    return response;
  },
  batchRequest: async function (clusterName: string, data: any) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/request_schedules`,
      method: "POST",
      data: JSON.stringify([
        {
          RequestSchedule: {
            batch: [
              {
                requests: data.batches,
              },
              {
                batch_settings: {
                  batch_separation_in_seconds: data.intervalTimeSeconds,
                  task_failure_tolerance: data.tolerateSize,
                },
              },
            ],
          },
        },
      ]),
    });
    return response;
  },
  hostComponentAddNewComponent: async function (
    clusterName: string,
    hostName: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/hosts?Hosts/host_name=${hostName}`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: data,
    });
    return response.data;
  },
  commonHostComponentUpdate: async function (
    clusterName: string,
    hostName: string,
    componentName: string,
    urlParams: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/${componentName}?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data,
    });
    return response.data;
  },
  updateComponentsState: async function (
    clusterName: string,
    urlParams: string
  ) {
    const url = `/clusters/${clusterName}/${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  updateServiceMetric: async function (clusterName: string, urlParams: string) {
    const url = `/clusters/${clusterName}/${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  commonCreateComponent: async function (
    clusterName: string,
    serviceName: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/services?ServiceInfo/service_name=${serviceName}`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: data,
    });
    return response.data;
  },
  reAssignLoadConfigs: async function (clusterName: string, urlParams: string) {
    const url = `/clusters/${clusterName}/configurations?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  configTags: async function (clusterName: string) {
    const url = `/clusters/${clusterName}?fields=Clusters/desired_configs`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  adminGetAllConfigurations: async function (
    clusterName: string,
    urlParams: string
  ) {
    const url = `/clusters/${clusterName}/configurations?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  componentDelete: async function (url: string) {
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },
  commonServiceConfigurations: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: JSON.stringify({
        Clusters: {
          desired_config: data.desired_config,
        },
      }),
    });
    return response.data;
  },
  commonServiceConfigurationsMove: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data,
    });
    set(response, "data.status", response?.status)
    return response.data;
  },
  updateServicePassiveState: async function (
    clusterName: string,
    serviceName: string,
    data: any
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/services/${serviceName}`,
      method: "PUT",
      data: JSON.stringify({
        RequestInfo: {
          context: data.requestInfo,
        },
        Body: {
          ServiceInfo: {
            maintenance_state: data.passive_state,
          },
        },
      }),
    });
    return response;
  },
  getDecommissionStatus: async function (
    clusterName: string,
    serviceName: string,
    componentName: string
  ) {
    const url = `/clusters/${clusterName}/services/${serviceName}/components/${componentName}/?fields=ServiceComponentInfo,host_components/HostRoles/state`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getDecommissionStatusForDataNode: async function (
    clusterName: string,
    hostNames: string
  ) {
    const url = `/clusters/${clusterName}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in(${hostNames})&fields=metrics/dfs/namenode`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getDecommissionStatusForRegionServer: async function (
    clusterName: string,
    hostName: string
  ) {
    const url = `/clusters/${clusterName}/host_components?HostRoles/component_name=HBASE_MASTER&HostRoles/host_name=${hostName}&fields=metrics/hbase/master/liveRegionServersHosts,metrics/hbase/master/deadRegionServersHosts&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getSlaveDesiredAdminState: async function (
    clusterName: string,
    hostName: string,
    componentName: string
  ) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/${componentName}/?fields=HostRoles/desired_admin_state`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  decommissionSlave: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}/requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: JSON.stringify({
        RequestInfo: {
          context: data.context,
          command: data.command,
          parameters: {
            slave_type: data.slaveType,
            excluded_hosts: data.hostName,
          },
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: data.clusterName,
            host_name: data.hostName,
            service_name: data.serviceName,
          },
        },
        "Requests/resource_filters": [
          {
            service_name: data.serviceName,
            component_name: data.componentName,
          },
        ],
      }),
    });
    return response.data;
  },
  regenerateKeytabsForHost: async function (
    clusterName: string,
    hostName: string
  ) {
    const url = `/clusters/${clusterName}?regenerate_keytabs=all&regenerate_hosts=${hostName}&config_update_policy=none`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: JSON.stringify({
        Clusters: {
          security_type: "KERBEROS",
        },
      }),
      headers: {
        "Content-Type": "text/plain",
      },
    });
    return response.data;
  },
  commonHostHostComponentUpdate: async function (
    clusterName: string,
    hostName: string,
    componentName: string,
    urlParams: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/${componentName}?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: data,
    });
    return response.data;
  },
  deleteHost: async function (clusterName: string, hostName: string) {
    const url = `/clusters/${clusterName}/hosts/${hostName}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },
  getHostListFilterSuggestions: async function (
    clusterName: string,
    data: any
  ) {
    const url = `/clusters/${encodeURIComponent(clusterName)}/hosts`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      params: {
        fields: `Hosts/${data.filter}`,
        minimal_response: true,
        page_size: data.pageSize,
      },
      headers: {
        "X-Http-Method-Override": "GET",
      },
      data: JSON.stringify({
        RequestInfo: {
          query: buildHostSuggestionPredicate(data.filter, data.searchTerm),
        },
      }),
    });
    return response.data;
  },
  executeCustomCommand: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}/requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: JSON.stringify({
        RequestInfo: {
          context: data.context,
          command: data.command,
        },
        "Requests/resource_filters": [
          {
            service_name: data.serviceName,
            component_name: data.componentName,
            hosts: data.hosts,
          },
        ],
      }),
    });
    return response.data;
  },
  transitionToObserver: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}/requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: JSON.stringify({
        RequestInfo: {
          context: data.context,
          command: "MAKEOBSERVER",
          operation_level: {
            level: "HOST_COMPONENT",
            cluster_name: clusterName,
            host_name: data.hostName,
            service_name: "HDFS",
          },
        },
        "Requests/resource_filters": [
          {
            service_name: "HDFS",
            component_name: "NAMENODE",
            hosts: data.hostName,
          },
        ],
      }),
    });
    return response.data;
  },
};
