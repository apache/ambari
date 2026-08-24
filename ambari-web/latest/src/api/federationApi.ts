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
import {
  buildDesiredConfigQuery,
  buildFederationRestartPayload,
  ConfigSnapshot,
} from "../screens/Services/highAvailibility/Federation/workflowUtils";

export interface ComponentCommandInput {
  command: string;
  context: string;
  serviceName: string;
  componentName: string;
  hosts: string | string[];
}

export function buildComponentCommandPayload(input: ComponentCommandInput) {
  return {
    RequestInfo: {
      command: input.command,
      context: input.context,
    },
    "Requests/resource_filters": [
      {
        service_name: input.serviceName,
        component_name: input.componentName,
        hosts: Array.isArray(input.hosts)
          ? input.hosts.join(",")
          : input.hosts,
      },
    ],
  };
}

export function desiredConfigsFromSnapshot(
  snapshot: ConfigSnapshot,
  types: string[],
  note: string,
) {
  return types.map((type) => {
    const item = snapshot.items.find((config) => config.type === type);
    if (!item) throw new Error(`The reviewed ${type} configuration is missing.`);
    return {
      type,
      properties: item.properties,
      service_config_version_note: note,
      ...(item.properties_attributes
        ? { properties_attributes: item.properties_attributes }
        : {}),
    };
  });
}

const federationApi = {
  async loadCurrentConfigurations(
    clusterName: string,
    requiredTypes: string[],
    optionalTypes: string[] = [],
  ): Promise<ConfigSnapshot> {
    const tagsResponse = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}?fields=Clusters/desired_configs`,
      method: "GET",
    });
    const desiredConfigs = tagsResponse.data?.Clusters?.desired_configs || {};
    const query = buildDesiredConfigQuery(
      desiredConfigs,
      requiredTypes,
      optionalTypes,
    );
    const configsResponse = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/configurations?${query}`,
      method: "GET",
    });
    return configsResponse.data;
  },

  async saveConfigurationTypes(
    clusterName: string,
    snapshot: ConfigSnapshot,
    types: string[],
    note: string,
    useMultiConfigurationBody = true,
  ) {
    const desiredConfigs = desiredConfigsFromSnapshot(snapshot, types, note);
    const data = useMultiConfigurationBody
      ? desiredConfigs.map((desired_config) => ({
          Clusters: { desired_config },
        }))
      : { Clusters: { desired_config: desiredConfigs[0] } };
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}`,
      method: "PUT",
      data,
    });
    return { ...response.data, status: response.status };
  },

  async executeComponentCommand(
    clusterName: string,
    input: ComponentCommandInput,
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/requests`,
      method: "POST",
      data: buildComponentCommandPayload(input),
    });
    return { ...response.data, status: response.status };
  },

  async restartNonFederationComponents(clusterName: string) {
    const response = await ambariApi.request({
      url: `/clusters/${encodeURIComponent(clusterName)}/requests`,
      method: "POST",
      data: buildFederationRestartPayload(clusterName),
    });
    return { ...response.data, status: response.status };
  },

  async getStackService(
    stackName: string,
    stackVersion: string,
    serviceName: string,
  ) {
    const response = await ambariApi.request({
      url:
        `/stacks/${encodeURIComponent(stackName)}/versions/` +
        `${encodeURIComponent(stackVersion)}/services/${encodeURIComponent(serviceName)}` +
        "?fields=StackServices/config_types,StackServices/service_type," +
        "components/StackServiceComponents/*",
      method: "GET",
    });
    return response.data;
  },
};

export default federationApi;
