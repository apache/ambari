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

const HISTORY_FIELDS = [
  "service_config_version",
  "user",
  "group_id",
  "group_name",
  "is_current",
  "createtime",
  "service_name",
  "hosts",
  "service_config_version_note",
  "is_cluster_compatible",
  "stack_id",
].join(",");

const ConfigHistoryApi = {
  fetchTotal: async (clusterName: string) => {
    const url = `/clusters/${clusterName}/configurations/service_config_versions?page_size=1&minimal_response=true`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  fetchConfigHistory: async (clusterName: string, parameters: string) => {
    const prefix = parameters ? `${parameters}&` : "";
    const url = `/clusters/${clusterName}/configurations/service_config_versions?${prefix}fields=${HISTORY_FIELDS}&minimal_response=true`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  fetchSuggestions: async (clusterName: string, field: string) => {
    const allowedFields = new Set([
      "group_name",
      "service_config_version_note",
      "service_name",
      "user",
    ]);
    if (!allowedFields.has(field)) {
      throw new Error(`Unsupported config history suggestion field: ${field}`);
    }
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/configurations/service_config_versions`,
      method: "GET",
      params: { fields: field, minimal_response: true },
    });
    return Array.from(new Set(
      (response.data.items || []).map((item: Record<string, unknown>) => item[field]).filter(Boolean),
    )) as string[];
  },
};

export default ConfigHistoryApi;
