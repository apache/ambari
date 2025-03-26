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
 
const ViewsInformationApi = {
  viewsListAPI: async function () {
    const url = `/views?fields=versions/ViewVersionInfo/version,versions/instances/ViewInstanceInfo,versions/*&versions/ViewVersionInfo/system=false`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getViewDetails: async function (view_name:string, version: string) {
    const url = `/views/${view_name}/versions/${version}`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getInstanceLabel: async function (view_name:string, version: string, instance_name: string) {
    const url = `/views/${view_name}/versions/${version}/instances/${instance_name}?fields=privileges%2FPrivilegeInfo,ViewInstanceInfo,resources`;
    const response = await adminApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getPrivileges: async function (view_name:string, version: string, instance_name: string) {
    const url = `/views/${view_name}/versions/${version}/instances/${instance_name}?fields=privileges%2FPrivilegeInfo`;
    const response = await adminApi.request({
      url: url,
      method: "GET"
    })
    return response.data;
  },
  deleteInstance: async function (view_name:string, version: string, instance_name: string) {
    const url = `/views/${view_name}/versions/${version}/instances/${instance_name}`;
    const response = await adminApi.request({
      url: url,
      method: "DELETE"
    })
    return response.data;
  },
  updatePrivileges: async function (view_name:string, version:string, instance_name: string, payload: any) {
    const url = `/views/${view_name}/versions/${version}/instances/${instance_name}/privileges`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      data: payload,
    });
    return response.data;
  },

  updateDetails: async function (view_name:string, version:string, instance_name: string, payload: any) {
    const url = `/views/${view_name}/versions/${version}/instances/${instance_name}`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      data: payload,
    });
    return response.data;
  },
  updateSettings: async function (view_name:string, version:string, instance_name: string, payload: any) {
    const url = `/views/${view_name}/versions/${version}/instances/${instance_name}`;
    const response = await adminApi.request({
      url: url,
      method: "PUT",
      data: payload,
    });
    return response.data;
  },
  createShortUrl: async function (urlname:string, payload: any ) {
    const url = `view/urls/${urlname}`;
    const response = await adminApi.request({
      url: url,
      method: "POST",
      data: payload,
    });
    return response.data;
  },
  deleteShortUrl: async function (urlname:string) {
    const url = `view/urls/${urlname}`;
    const response = await adminApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },

};

export default ViewsInformationApi;
