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

import { set } from "lodash";
import { ambariApi, supressErrorAmbariApi } from "./config/axiosConfig";
export const RequestApi = {
    getRequestStatus: async function (clusterName: string,requestId:string) {
        const url = `/clusters/${clusterName}/requests/${requestId}?fields=*,tasks/Tasks/request_id,tasks/Tasks/command,tasks/Tasks/command_detail,tasks/Tasks/ops_display_name,tasks/Tasks/host_name,tasks/Tasks/id,tasks/Tasks/role,tasks/Tasks/status&minimal_response=true`;
        const response = await supressErrorAmbariApi.request({
            url: url,
            method: "GET",
        });
        return response.data;
    },
     getRunningRequests: async function (clusterName: string) {
        const url = `/clusters/${clusterName}/requests/?page_size=20&fields=Requests/request_status&Requests/request_status.in(IN_PROGRESS)`;
        const response = await ambariApi.request({
            url: url,
            method: "GET",
        });
        return response.data;
    },
    stopServices: async function (clusterName:string, stopServicesdata: object) {
        const url = `clusters/${clusterName}/services`;
        const response = await ambariApi.request({
          url: url,
          method: "PUT",
          data: stopServicesdata,
          headers:{
            "Content-Type":"text/plain"
          }
        });
        set(response, "data.status", response.status);
        return response.data;
    },
    startServices: async function (clusterName: string, payload: any, params: string, method="PUT"){
      const url = `/clusters/${clusterName}/services?params/${params}`;
      const response = await ambariApi.request({
        url: url, 
        method: method, 
        data: payload,
        headers:{
          "Content-Type":"text/plain"
        }
      })
      return response.data;
    },
    performRequests: async function (clusterName: string, payload: any, method="PUT") {
      const url = `/clusters/${clusterName}/services?ServiceInfo/state=INSTALLED&ServiceInfo/service_name=KERBEROS`
      const response = await ambariApi.request({
        url: url, 
        method: method,
        data: payload,
        headers:{
          "Content-Type":"text/plain"
        }
      })
      set(response, "data.status", response.status);
      return response.data;
    },
    postRequest: async function (clusterName: string, payload: any, method="POST") {
      const url = `/clusters/${clusterName}/requests`
      const response = await ambariApi.request({
        url: url,
        method: method,
        data: payload,
      })
      set(response, "data.status", response.status);
      return response.data;
    },
    getServices: async function (clusterName: string, payload: any, params: string, method="PUT"){
      const url =   `/clusters/${clusterName}/services?${params}`
      const response = await ambariApi.request({
        url: url,
        method: method,
        data: payload,
        headers: {
          "Content-Type": "text/plain"
        }
      })
      return response.data;
    },
    getServicesWithStatus: async function (clusterName: string, payload: any, params: string, method="PUT"){
      const url =   `/clusters/${clusterName}/services?${params}`
      const response = await ambariApi.request({
        url: url,
        method: method,
        data: payload,
        headers: {
          "Content-Type": "text/plain"
        }
      })
      if (!response.data) 
        response.data = {};
      
      set(response.data, "status", response.status);
      return response.data;
    },
    preparingOperations: async function (clusterName: string, payload: any, params="") {
      let url = `/clusters/${clusterName}`;
      if(params !== "") 
        url = `${url}?${params}`
      const response = await ambariApi.request({
        url: url,
        method: "PUT",
        data: payload,
        headers:{
          "Content-Type":"text/plain"
        }
      })
      set(response, "data.status", response.status);
      return response.data;
    },
    regenerateKeytabs: async function (clusterName: string, payload: any, params: string) {
      const url = `/clusters/${clusterName}?${params}`;
      const response = await ambariApi.request({
        url: url,
        method: "PUT",
        data: payload,
        headers: {
          "Content-Type": "text/plain"
        }
      })
      set(response, "data.status", response.status);
      return response.data;
    },
    kerberosDescriptor: async function (clusterName: string, payload: any) {
      const url = `/clusters/${clusterName}/artifacts/kerberos_descriptor`;
      const response = await ambariApi.request({
        url: url,
        method: "POST",
        data: payload,
        headers: {
          "Content-Type": "text/plain"
        }
      })
      return response.data;
    },
    getTask: async function (
      clusterName: string,
      requestId: string,
      taskId: string
    ) {
      const url = `/clusters/${clusterName}/requests/${requestId}/tasks/${taskId}`;
      const response = await ambariApi.request({
        url: url,
        method: "GET",
      });
      return response.data;
    },
    installPackages: async function (clusterName: string, payload: any) {
      const url = `/clusters/${clusterName}/stack_versions`
      const response = await ambariApi.request({
        url: url,
        method: "POST",
        data: payload,
        headers: {
          "Content-Type": "text/plain"
        }
      })
      return response.data
    }, 
    getTaskId : async function (requestId: string) {
      const url = `/requests/${requestId}/tasks/?`;
      const response = await ambariApi.request({
        url: url,
        method: "GET",
      });
      return response.data;
    },
    getTaskStatus: async function (requestID:string,taskId: string) {
      const url = `requests/${requestID}/tasks/${taskId}?`;
      const response = await ambariApi.request({
        url: url,
        method: "GET",
      });
      return response.data;
    }
};