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
import { ambariApi, supressErrorAmbariApi } from "./config/axiosConfig";

const ClusterApi = {
  loadAmbariProperties: async (fields = "") => {
    const url = `/services/AMBARI/components/AMBARI_SERVER${fields}`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  fetchClusterDetails: async function (updateClusterPayloadData: object, clusterName: string) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: updateClusterPayloadData
    });
    return response;
  },
  getDesiredClusterConfigs: async function (clusterName: string,fields=`Clusters/desired_configs`) {
    const url = `/clusters/${clusterName}?fields=${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  updateCluster:async function(clusterName:string,data:any){
    const url=`/clusters/${clusterName}`
    const response=await ambariApi.request({
      url:url,
      method:"PUT",
      data
    })
    return response.data
  },
  getCluster: async function (clusterName:string) {
    const url = `/clusters/${clusterName}?fields=Clusters/desired_configs/cluster-env`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getAllClusters: async function () {
    const url = `/clusters`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getRequests: async function (clusterName: string,pageSize:number) {
    const url = `/clusters/${clusterName}/requests?to=end&page_size=${pageSize}&fields=Requests/end_time,Requests/id,Requests/progress_percent,Requests/request_context,Requests/request_status,Requests/start_time,Requests/cluster_name,Requests/user_name&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  deleteCluster: async function (clusterName:string) {
    const url = `/clusters/${clusterName}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    })
    return response.data
  },
  getRequestById: async function (clusterName: string,requestId:number|string) {
    const url = `/clusters/${clusterName}/requests/${requestId}?fields=*,tasks/Tasks/request_id,tasks/Tasks/command,tasks/Tasks/command_detail,tasks/Tasks/ops_display_name,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  updateRequest:async function (clusterName:string,requestId:number|string,payload:any){
    const url=`/clusters/${clusterName}/requests/${requestId}`
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data:payload
    });
    return response.data;
  },
  getClusterRequestTaskLogs:async function(clusterName:string,requestId:number|string,taskId:number|string){
    const url=`/clusters/${clusterName}/requests/${requestId}/tasks/${taskId}`
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getClusterName : async function () {
      const url = `/clusters?fields=Clusters`;
      const response = await ambariApi.request({
        url: url,
        method: "GET"
    });
    const clusterName = response?.data?.items[0]?.Clusters?.cluster_name;
    console.log("CLUSTER NAME", response.data.items[0].Clusters.cluster_name)
    return clusterName;
  },
  getClusterData: async function () {
    const url= `/clusters?fields=Clusters/provisioning_state,Clusters/security_type,Clusters/version,Clusters/cluster_id`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getPersistData: async function (key:any) {
    const url = `/persist/${key}`;
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  postPersistData: async function (data:any) {
    const url = `/persist`;
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "POST",
      data
    });
    return response.data;
  },
  getHosts: async function (clusterName:string) {
    const url = `/clusters/${clusterName}/hosts?minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getUpgradeState: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/upgrades?fields=Upgrade`;
    const response = await ambariApi.request({
      url: url,
      method: "GET"
    })
    return response.data;
  },
  noopPolling: async function () {
    // const timestamp = new Date().getTime();
    const url = `/clusters`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response;
  },
  getUserTimeout: async () => {
    return ambariApi.request({
      url: '/services/AMBARI/components/AMBARI_SERVER',
      method: 'GET',
      params: {
        fields: 'RootServiceComponents/properties/user.inactivity.timeout.default',
        _: Date.now() // Cache buster
      }
    });
  },
  
  createClusterCustomAction: async function (clusterName: string, payload: any) {
    const url = `/clusters/${clusterName}/requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: payload
    });
    return response.data;
  },
  createCustomAction: async function ( payload: any) {
    const url = `/requests`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: payload,
    });
    return response.data;
  }
  }

export default ClusterApi;