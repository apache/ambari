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

import { AxiosResponse } from "axios";
import { ambariApi, supressErrorAmbariApi } from "./config/axiosConfig";
import { set } from "lodash";

const ConfigsApi = {
  getServiceConfigurations: async function (
    stack: string,
    verison: string,
    services: string
  ) {
    const url = `stacks/${stack}/versions/${verison}/services?StackServices/service_name.in(${services})&fields=configurations/*,configurations/dependencies/*,StackServices/config_types/*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getConfigProperties: async function (
    stack: string,
    verison: string,
    services: string
  ) {
    const url = `stacks/${stack}/versions/${verison}/services?StackServices/service_name.in(${services})&fields=configurations/*,configurations/dependencies/*,StackServices/display_name,StackServices/config_types/*&_=1728974996201`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  validateConfigProperties: async function (
    stack: string,
    verison: string,
    payload: any
  ) {
    const url = `stacks/${stack}/versions/${verison}/validations`;
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "POST",
      data: payload,
    });
    return response.data;
  },

  getConfigValues: async function (clusterName: string, services: string) {
    const url = `clusters/${clusterName}/configurations/service_config_versions?service_name.in(${services})&is_current=true&fields=*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getVersionConfigValues: async function (
    clusterName: string,
    services: string,
    version: string
  ) {
    const url = `clusters/${clusterName}/configurations/service_config_versions?(service_name=${services}&service_config_version.in(${version}))`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getMultipleVersionConfigValues: async function (
    clusterName: string,
    serviceName: string,
    version1: string,
    version2: string
  ) {
    const url = `clusters/${clusterName}/configurations/service_config_versions?(service_name=${serviceName}&service_config_version.in(${version1},${version2}))`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getTheme: async (
    stackName: string,
    stackVersion: string,
    services: string
  ) => {
    const url = `/stacks/${stackName}/versions/${stackVersion}/services?StackServices/service_name.in(${services})&themes/ThemeInfo/default=true&fields=themes/*`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  loadConfigTags: async (clusterName: string) => {
    const url = `/clusters/${clusterName}?fields=Clusters/desired_configs`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  reassignLoadConfigs: async (clusterName: string, urlParams: string) => {
    const url = `/clusters/${clusterName}/configurations?${urlParams}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  updateConfigTags: async function (clusterName: string) {
    const url = `/clusters/${clusterName}?fields=Clusters/desired_configs`;
    const { data } = await ambariApi.request({
      url,
      method: "GET",
    });
    const tags = [];
    for (let site in data.Clusters.desired_configs) {
      tags.push({
        siteName: site,
        tagName: data.Clusters.desired_configs[site].tag,
      });
    }
    return tags;
  },
  getConfigsByTags: async function (clusterName: string, params: string) {
    const url = `/clusters/${clusterName}/configurations?${params}`;
    const { data } = await ambariApi.request({
      url,
      method: "GET",
    });
    return data;
  },
   getConfigsByTagsForService: async function (clusterName: string, params: string) {
    const url = `/clusters/${clusterName}/configurations?${params}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response
  },
  updateServiceConfigurations: async function (clusterName: string, data: any) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: {
        Clusters: {
          desired_config: data.desired_config,
        },
      },
    });
    return response.data;
  },
  updateServiceMultiConfigurations: async function (
    clusterName: string,
    data: any
  ) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: data.configs,
    });
    set(response, "data.status", response.status);
    return response.data;
  },
  loadConfigsFromStack: async function (
    stack: string,
    version: string,
    serviceNames: string[]
  ) {
    const url = serviceNames.length
      ? `/stacks/${stack}/versions/${version}/services?StackServices/service_name.in(${serviceNames})&fields=configurations/*,configurations/dependencies/*,StackServices/config_types/*`
      : `/stacks/${stack}/versions/${version}/services?fields=configurations/*,StackServices/config_types/*`;
    const { data } = await ambariApi.request({
      url,
      method: "GET",
    });
    return data;
  },
  getConfigGroups: async function (clusterName: string, serviceName: string) {
    const url = `clusters/${clusterName}/config_groups?ConfigGroup/tag.in(${serviceName})&fields=*`;
    const { data } = await ambariApi.request({
      url,
      method: "GET",
    });
    return data;
  },
  createNewConfigGroup: async function (clusterName: string, payload: any) {
    const url = `clusters/${clusterName}/config_groups`;
    const { data } = await ambariApi.request({
      url,
      method: "POST",
      data: payload,
    });
    return data;
  },
  saveConfigs: async function (clusterName: string, payload: any) {
    const url = `clusters/${clusterName}`;
    const { data } = await ambariApi.request({
      url,
      method: "PUT",
      data: payload,
    });
    return data;
  },
  updateConfigGroupProperties: async function (
    clusterName: string,
    groupId: string,
    payload: any
  ) {
    const url = `clusters/${clusterName}/config_groups/${groupId}`;
    const { data } = await ambariApi.request({
      url,
      method: "PUT",
      data: payload,
    });
    return data;
  },
  getDesiredConfigsInfo: async (
    clusterName: string
  ): Promise<AxiosResponse> => {
    const url = `/clusters/${clusterName}?fields=Clusters/desired_configs&_=${Date.now()}\``;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response;
  },
  getEnabledConfigsForRangerPlugins: async (
    hdfsTagVersion: string,
    hbaseTagVersion: string,
    hiveTagVersion: string,
    yarnTagVersion: string,
    clusterName: string
  ): Promise<AxiosResponse> => {
    const url =
      `/clusters/${clusterName}/configurations?(type=ranger-hdfs-plugin-properties&tag=${hdfsTagVersion})|` +
      `(type=ranger-yarn-plugin-properties&tag=${yarnTagVersion})|` +
      `(type=hive-env&tag=${hiveTagVersion})|` +
      `(type=ranger-hbase-plugin-properties&tag=${hbaseTagVersion})&_=${Date.now()}`;

    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response;
  },
  getRecommendations: async function (
    stack: string,
    version: string,
    payload: any
  ) {
    const url = `stacks/${stack}/versions/${version}/recommendations`;
    const { data } = await ambariApi.request({
      url,
      method: "POST",
      data: payload,
    });
    return data;
  },
  serviceMultiConfigurations: async function (
    clusterName: string,
    payload: any
  ) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: payload,
    });
    return response.data;
  },
  getServiceConfigVersions: async function (
    clusterName: string,
    serviceName: string
  ): Promise<AxiosResponse> {
    const serviceConfigFields =
      "service_config_version,user,hosts,group_id,group_name,is_current,createtime,service_name,service_config_version_note,stack_id,is_cluster_compatible";
    const url = `clusters/${clusterName}/configurations/service_config_versions?service_name=${serviceName}&fields=${serviceConfigFields}&sortBy=service_config_version.desc&minimal_response=true&_=${Date.now()}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data.items;
  },
};

export default ConfigsApi;
