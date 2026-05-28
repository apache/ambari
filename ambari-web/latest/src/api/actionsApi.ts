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

export const ActionsApi = {
  serviceAction: async function (
    clusterName: string,
    serviceName: string,
    payloadData: object
  ) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/services/${serviceName}`,
      method: "PUT",
      data: payloadData,
    });
    return response;
  },
  turnOnOffMaintenance: async function (
    clusterName: string,
    serviceName: string,
    payloadData: { requestInfo: string; passive_state: string }
  ) {
    const url = `/clusters/${clusterName}/services/${serviceName}`;
    const payload = {
      RequestInfo: {
        context: payloadData.requestInfo,
      },
      Body: {
        ServiceInfo: {
          maintenance_state: payloadData.passive_state,
        },
      },
    };
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: payload,
    });
    return response;
  },
  actionRequest: async function (clusterName: string, payloadData: any) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/request_schedules`,
      method: "POST",
      data: payloadData
    });
    return response;
  },
  actionRequestRebalanceHDFS: async function (clusterName: string, payloadData: object) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/requests`,
      method: "POST",
      data: payloadData
    });
    return response;
  },
  submitActionRequest: async function (clusterName: string, payloadData: object) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/requests`,
      method: "POST",
      data: payloadData
    });
    return response;
  },
  regenerateKeytabsForService: async function (clusterName: string, serviceName: string, payloadData: any) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}?regenerate_keytabs=all&regenerate_components=${serviceName}&config_update_policy=none`,
      method: "PUT",
      data: payloadData,
    });
    return response;
  }
};
