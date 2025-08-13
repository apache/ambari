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

export const ChooseServicesApi = {
  serviceDetails: async function (serviceName: string, clusterName: string) {
    const url = `/clusters/${clusterName}/services/${serviceName}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  servicesList: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/services`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getServices: async (stack: string, version: string, services?: string[]) => {
    const url = `stacks/${stack}/versions/${version}/services?fields=StackServices/*,components/*,components/dependencies/Dependencies/scope,components/dependencies/Dependencies/service_name,artifacts/Artifacts/artifact_name${
      services ? `&StackServices/service_name.in(${services.join(",")})` : ""
    }`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
};
