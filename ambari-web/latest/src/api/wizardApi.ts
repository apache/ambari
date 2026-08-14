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

const WizardApi = {
  launchBootstrap: async (data: Record<string, unknown>) => {
    const response = await ambariApi.request({
      url: "/bootstrap",
      method: "POST",
      data,
    });
    return response.data;
  },
  getBootstrapStatus: async (requestId: string) => {
    const response = await ambariApi.request({
      url: `/bootstrap/${encodeURIComponent(requestId)}`,
      method: "GET",
    });
    return response.data;
  },
  isHostsRegistered: async () => {
    const url = `/hosts?fields=Hosts/host_status`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getStackConfigurations: async (
    stackName: string,
    stackVersion: string,
    services: string,
    fields: string
  ) => {
    const url = `/stacks/${stackName}/versions/${stackVersion}/services?StackServices/service_name.in(${services})&fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getStackThemes: async (
    stackName: string,
    stackVersion: string,
    services: string,
    fields: string
  ) => {
    const url = `/stacks/${stackName}/versions/${stackVersion}/services?StackServices/service_name.in(${services})&themes/ThemeInfo/default=true&fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getStackLevelConfigurations: async (
    stackName: string,
    stackVersion: string,
    fields: string
  ) => {
    const url = `/stacks/${stackName}/versions/${stackVersion}?fields=${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  }
};

export default WizardApi;
