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

interface Cluster {
    cluster_id?: number;
    name?: string;
    url?: string;
    username?: string;
    password?: string;
}

const RemoteClusterApi = {
    getRemoteClusters: async function () {
        const url = '/remoteclusters';
        const response = await adminApi.request({
          url: url,
          method: "GET"
        });
        // Fetching details for each cluster
        const clustersData = await Promise.all(
          response.data.items.map(async (cluster: any) => {
            const details = await this.getRemoteClusterByName(cluster.ClusterInfo.name);
            return details;
          })
        );
        return clustersData;
    },
    getRemoteClusterByName: async function(clusterName: string) {
        const url = `/remoteclusters/${clusterName}`;
        const response = await adminApi.request({
          url: url,
          method: "GET"
        });
        return response.data;
    },
    addRemoteCluster: async function (cluster: Cluster) {
        const url = `/remoteclusters/${cluster.name}`;
        const response = await adminApi.request({
          url: url,
          method: "POST",
          headers: {
            'Content-Type': 'application/json'
          },
          data: {ClusterInfo: cluster}
        });
        return response.data;
    },
    updateRemoteCluster: async function (clusterName: string, cluster: Cluster) {
        const url = `/remoteclusters/${clusterName}`;
        const response = await adminApi.request({
          url: url,
          method: "PUT",
          headers: {
            'Content-Type': 'application/json'
          },
          data: {ClusterInfo: cluster}
        });
        return response.data;
    },
    deregisterRemoteCluster: async function (clusterName: string) {
        const url = `/remoteclusters/${clusterName}`;
        const response = await adminApi.request({
          url: url,
          method: "DELETE"
        });
        return response.data;
    }
};

export default RemoteClusterApi;