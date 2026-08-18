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

const KerberosApi = {
  getKerberosDescriptorProperties: async function (
    evaluate: string,
    clusterName: string
  ) {
    const url = `/clusters/${clusterName}/kerberos_descriptors/COMPOSITE?evaluate_when=${evaluate}&_=${Date.now()}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getKerberosDescriptorArtifact: async function (clusterName: string) {
    const url = `/clusters/${clusterName}/artifacts/kerberos_descriptor?fields=artifact_data`;
    const response = await supressErrorAmbariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },

  getSecurityType: async function (clusterName: string) {
    const url = `clusters/${clusterName}?fields=Clusters/security_type`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  testKdcConnection: async function (kdcHosts: string) {
    const url = `kdc_check/${kdcHosts}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  downloadKerberosIdentitiesCsv: async function (clusterName: string) {
    const url = `clusters/${clusterName}/kerberos_identities?fields=*&format=csv`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  saveKerberosData: async function(clusterName: string, payload: any) {
    const url = `clusters/${clusterName}/artifacts/kerberos_descriptor`
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "POST",
      data: payload
    })
    return response.data;
  },
  saveAndEditKerberosData: async function(clusterName: string, payload: any) {
    const url = `clusters/${clusterName}/artifacts/kerberos_descriptor`
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "PUT",
      data: payload
    })
    return response.data;
  },
  postKDCAdminCredentials: async function (clusterName: string, payload: any) {
    const url = `clusters/${clusterName}/credentials/kdc.admin.credential`
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "POST",
      data: payload
    })
    return response.data;
  },
  postKDCAdminCredentialsSupress: async function (clusterName: string, payload: any) {
    const url = `clusters/${clusterName}/credentials/kdc.admin.credential`
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "POST",
      data: payload
    })
    return response.data;
  },
  submitKDCAdminCredentials: async function (clusterName: string, payload: any, method: string) {
    const url = `clusters/${clusterName}/credentials/kdc.admin.credential`
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: method,
      data: payload
    })
    return response.data;
  },
  deleteKDCAdminCredentials: async function (clusterName: string) {
    const url = `clusters/${clusterName}/credentials/kdc.admin.credential`
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    })
    return response.data;
  },
  getKDCAdminCredentials: async function (clusterName: string) {
    const url = `clusters/${clusterName}/credentials?fields=Credential/*`
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "GET",
    })
    return response.data;
  },

  createKerberosConfigurations: async function (
    clusterName: string,
    payloadData: any
  ) {
    const url = `clusters/${clusterName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: payloadData,
    });
    return response.data;
  },

  deleteKerberosService: async function (
    clusterName: string,
    serviceName: string
  ) {
    const url = `clusters/${clusterName}/services/${serviceName}`;
    const response = await supressErrorAmbariApi.request({
      url: url,
      method: "DELETE",
    });
    return response.data;
  },

  createAdminSession: async function (clusterName: string, payloadData: any) {
    const url = `clusters/${clusterName}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: payloadData,
    });
    return response.data;
  },

  getCredentialStoreInfo: async function (clusterName: string) {
    const url = `clusters/${clusterName}?fields=Clusters/credential_store_properties`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
};

export default KerberosApi;
