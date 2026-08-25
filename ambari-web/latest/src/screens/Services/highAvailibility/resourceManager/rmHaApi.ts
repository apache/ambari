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

import {
  ambariApi,
  supressErrorAmbariApi,
} from "../../../../api/config/axiosConfig";

const pathPart = (value: string) => encodeURIComponent(value);
const errorStatus = (error: unknown) => {
  const requestError = error as {
    response?: { status?: number };
    status?: number;
  };
  return requestError.response?.status ?? requestError.status;
};

function responseData(
  response: { data?: unknown; status?: number },
  requireRequestId = false,
) {
  const data =
    response.data && typeof response.data === "object"
      ? (response.data as Record<string, unknown>)
      : {};
  const request = data.Requests as { id?: unknown } | undefined;
  if (
    requireRequestId &&
    (request?.id === undefined || request.id === null || request.id === "")
  ) {
    throw new Error("Ambari did not return a request ID for the operation.");
  }
  return { ...data, status: response?.status };
}

const rmHaApi = {
  getHosts: async (clusterName: string) => {
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/hosts?fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/maintenance_state&minimal_response=true`,
      method: "GET",
    });
    return response.data;
  },

  getClusterComponents: async (clusterName: string) => {
    const fields = [
      "ServiceComponentInfo/service_name",
      "ServiceComponentInfo/component_name",
      "host_components/HostRoles/component_name",
      "host_components/HostRoles/service_name",
      "host_components/HostRoles/host_name",
      "host_components/HostRoles/state",
      "host_components/HostRoles/maintenance_state",
    ].join(",");
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/components?fields=${fields}&minimal_response=true`,
      method: "GET",
    });
    return response.data;
  },

  getHostRecommendations: async (
    stack: string,
    version: string,
    payload: unknown,
  ) => {
    const response = await ambariApi.request({
      url: `/stacks/${pathPart(stack)}/versions/${pathPart(version)}/recommendations`,
      method: "POST",
      data: payload,
    });
    return response.data;
  },

  getDesiredConfigs: async (clusterName: string) => {
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}?fields=Clusters/desired_configs`,
      method: "GET",
    });
    return response.data;
  },

  getConfigs: async (clusterName: string, query: string) => {
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/configurations?${query}`,
      method: "GET",
    });
    return response.data;
  },

  getConfigRecommendations: async (
    stack: string,
    version: string,
    payload: unknown,
  ) => {
    const response = await ambariApi.request({
      url: `/stacks/${pathPart(stack)}/versions/${pathPart(version)}/recommendations`,
      method: "POST",
      data: payload,
    });
    return response.data;
  },

  saveDesiredConfig: async (clusterName: string, payload: unknown) => {
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}`,
      method: "PUT",
      data: payload,
      headers: { "Content-Type": "text/plain" },
    });
    return responseData(response);
  },

  stopRequiredServices: async (
    clusterName: string,
    serviceNames: string[],
  ) => {
    if (!serviceNames.length) return { status: 200 };
    const query = `ServiceInfo/service_name.in(${serviceNames.join(",")})`;
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/services?${query}`,
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Stop required services",
          operation_level: { level: "CLUSTER", cluster_name: clusterName },
        },
        Body: { ServiceInfo: { state: "INSTALLED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
    return responseData(response, true);
  },

  startAllServices: async (clusterName: string, runSmokeTest: boolean) => {
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/services?params/run_smoke_test=${runSmokeTest}`,
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Start all services",
          operation_level: { level: "CLUSTER", cluster_name: clusterName },
        },
        Body: { ServiceInfo: { state: "STARTED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
    return responseData(response, true);
  },

  installAdditionalResourceManager: async (
    clusterName: string,
    hostName: string,
  ) => {
    if (!hostName) throw new Error("The additional ResourceManager host is missing.");
    const componentQuery = [
      "HostRoles/component_name=RESOURCEMANAGER",
      `HostRoles/host_name.in(${hostName})`,
      "fields=HostRoles/host_name",
      "minimal_response=true",
    ].join("&");
    const installedResponse = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/host_components?${componentQuery}`,
      method: "GET",
    });
    if (!Array.isArray(installedResponse.data?.items)) {
      throw new Error("Ambari returned an invalid ResourceManager host response.");
    }
    const alreadyRegistered = installedResponse.data.items.some(
      (item: { HostRoles?: { host_name?: string } }) =>
        item?.HostRoles?.host_name === hostName,
    );

    if (!alreadyRegistered) {
      try {
        await supressErrorAmbariApi.request({
          url: `/clusters/${pathPart(clusterName)}/services/YARN/components/RESOURCEMANAGER`,
          method: "GET",
        });
      } catch (error) {
        if (errorStatus(error) !== 404) throw error;
        await ambariApi.request({
          url: `/clusters/${pathPart(clusterName)}/services?ServiceInfo/service_name=YARN`,
          method: "POST",
          data: {
            components: [
              {
                ServiceComponentInfo: { component_name: "RESOURCEMANAGER" },
              },
            ],
          },
        });
      }

      await ambariApi.request({
        url: `/clusters/${pathPart(clusterName)}/hosts`,
        method: "POST",
        data: {
          RequestInfo: { query: `Hosts/host_name=${hostName}` },
          Body: {
            host_components: [
              { HostRoles: { component_name: "RESOURCEMANAGER" } },
            ],
          },
        },
      });
    }

    const installQuery = [
      "HostRoles/component_name=RESOURCEMANAGER",
      `HostRoles/host_name.in(${hostName})`,
      "HostRoles/maintenance_state=OFF",
    ].join("&");
    const response = await ambariApi.request({
      url: `/clusters/${pathPart(clusterName)}/host_components?${installQuery}`,
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Install ResourceManager",
          operation_level: {
            level: "CLUSTER",
            cluster_name: clusterName,
          },
          query: installQuery,
        },
        Body: { HostRoles: { state: "INSTALLED" } },
      },
      headers: { "Content-Type": "text/plain" },
    });
    return responseData(response, true);
  },
};

export type RmHaApi = typeof rmHaApi;
export default rmHaApi;
