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
import { adminApi } from "./configs/axiosConfig";

const ClusterApi = {
  // Cluster APIs

  clusterInfo: async function (fields: string) {
    const url = `/clusters?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  hostClustersInfo: async function () {
    const url = `/clusters?fields=Clusters/provisioning_state`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  adminAboutInfo: async function (fields: string) {
    const url = `services/AMBARI/components/AMBARI_SERVER?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  blueprintInfo: async function (
    clusterName: string,
    format: string = "blueprint"
  ) {
    const url = `/clusters/${clusterName}?format=${format}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  updateClusterName: async function (clusterName: string,updatedClusterName:string) {
    const url = `/clusters/${clusterName}`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      data: { Clusters: { cluster_name: updatedClusterName } },
    });
    return response.data;
  },
  remoteClusterInfo: async function (fields: string) {
    const url = `/remoteclusters?fields=${fields}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  noopPolling: async function () {
    const timestamp = new Date().getTime();
    const url = `/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/properties/user.inactivity.timeout.default&_=${timestamp}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response;
  },
  getUserTimeout: async function () {
    const timestamp = new Date().getTime();
    const url = `services/AMBARI/components/AMBARI_SERVER?_=${timestamp}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response;
  }
};

export default ClusterApi;
