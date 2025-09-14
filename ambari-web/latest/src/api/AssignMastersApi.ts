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

const AssignMastersApi = {
  getCpuInfo: async function (HOSTS: any) {
    const hostsParams = HOSTS.join(",");
    const url = `/hosts?Hosts/host_name.in(${hostsParams})&fields=Hosts/cpu_count,Hosts/disk_info,Hosts/total_mem,Hosts/ip,Hosts/os_type,Hosts/os_arch,Hosts/public_host_name&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response;
  },
  postRecommendations: async function (
    payload: any,
    STACK: string,
    VERSION: string
  ) {
    const url = `/stacks/${STACK}/versions/${VERSION}/recommendations`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: payload,
    });
    return response.data;
  },
  postValidations: async function (
    payload: any,
    STACK: string,
    VERSION: string
  ) {
    const url = `/stacks/${STACK}/versions/${VERSION}/validations`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: payload,
    });
    return response.data;
  },
};
export default AssignMastersApi;
