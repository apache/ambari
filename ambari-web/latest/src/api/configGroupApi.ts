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

import { ambariApi } from "./config/axiosConfig";

const ConfigGroupApi = {
  getHostsInfoUsingClusterName: async (clusterName: string, fields: string) => {
    const url = `clusters/${clusterName}/hosts?fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getHostsInfoUsingHostNames: async (hostNames: string, fields: string) => {
    const url = `hosts?Hosts/host_name.in(${hostNames})&fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getConfigGroupInfo: async (
    clusterName: string,
    serviceName: string,
    fields: string
  ) => {
    const url = `clusters/${clusterName}/config_groups?ConfigGroup/tag=${serviceName}&fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getDesiredConfigsInfo: async (clusterName: string, configString: string) => {
    const url = `clusters/${clusterName}/configurations?${configString}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  addConfigGroup: async (clusterName: string, data: any) => {
    const url = `clusters/${clusterName}/config_groups`;
    const response = await ambariApi.request({
      url,
      method: "POST",
      data,
    });
    return response.data;
  },
  removeConfigGroup: async (clusterName: string, configGroupId: string) => {
    const url = `clusters/${clusterName}/config_groups/${configGroupId}`;
    const response = await ambariApi.request({
      url,
      method: "DELETE",
    });
    return response.data;
  },
  updateConfigGroup: async (clusterName: string, configGroupId: string, data: any) => {
    const url = `clusters/${clusterName}/config_groups/${configGroupId}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data,
    });
    return response.data;
  },
  getConfigGroups: async (
    clusterName: string,
    fields: string
  ) => {
    const url = `clusters/${clusterName}/config_groups?fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
};

export default ConfigGroupApi;
