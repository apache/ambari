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

const CredentialsApi = {
  createCredentials: async function (
    clusterName: string,
    alias: string,
    data: { resource: string }
  ) {
    const url = `/clusters/${clusterName}/credentials/${alias}`;
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: {
        Credential: data.resource,
      },
    });
    return response.data;
  },
  getCredentials: async function (
    clusterName: string,
    alias: string,
  ) {
    const url = `/clusters/${clusterName}/credentials/${alias}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  deleteCredentials: async function (
    clusterName: string,
    alias: string,
  ) {
    const url = `/clusters/${clusterName}/credentials/${alias}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },
  updateCredentials: async function (
    clusterName: string,
    alias: string,
    data:{resource:string}
  ) {
    const url = `/clusters/${clusterName}/credentials/${alias}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data:{
        Credential:data.resource
      }
    });
    return response.data;
  },
  listCredentials: async function (
    clusterName: string,
  ) {
    const url = `/clusters/${clusterName}/credentials?fields=Credential/*`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  credentialsStoreInfo: async function (
    clusterName: string,
  ) {
    const url = `/clusters/${clusterName}?fields=Clusters/credential_store_properties`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  
};

export default CredentialsApi
