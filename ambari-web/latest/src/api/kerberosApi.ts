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

const pathSegment = (value: string) => encodeURIComponent(value);
const responseStatus = (error: any) => error?.response?.status ?? error?.status;

const KerberosApi = {
  getKerberosDescriptorProperties: async function (
    evaluate: string,
    clusterName: string
  ) {
    const url = `/clusters/${pathSegment(clusterName)}/kerberos_descriptors/COMPOSITE?evaluate_when=${encodeURIComponent(evaluate)}&_=${Date.now()}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getSecurityType: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}?fields=Clusters/security_type`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  runPreKerberizeChecks: async function () {
    // Classic registers this optional feature against the Ambari API root.
    const response = await ambariApi.request({
      url: "",
      method: "GET",
    });
    return response.data;
  },

  testKdcConnection: async function (kdcHosts: string) {
    const url = `kdc_check/${pathSegment(kdcHosts)}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  downloadKerberosIdentitiesCsv: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/kerberos_identities?fields=*&format=csv`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  saveKerberosData: async function(clusterName: string, payload: any) {
    const url = `clusters/${pathSegment(clusterName)}/artifacts/kerberos_descriptor`
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: payload
    })
    return response.data;
  },
  saveAndEditKerberosData: async function(clusterName: string, payload: any) {
    const url = `clusters/${pathSegment(clusterName)}/artifacts/kerberos_descriptor`
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: payload
    })
    return response.data;
  },
  createKerberosDescriptor: async function(clusterName: string, payload: any) {
    try {
      return await this.saveKerberosData(clusterName, payload);
    } catch (error) {
      if (responseStatus(error) !== 409) {
        throw error;
      }
      return await this.saveAndEditKerberosData(clusterName, payload);
    }
  },
  updateKerberosDescriptor: async function(clusterName: string, payload: any) {
    try {
      return await this.saveAndEditKerberosData(clusterName, payload);
    } catch (error) {
      if (responseStatus(error) !== 404) {
        throw error;
      }
      return await this.saveKerberosData(clusterName, payload);
    }
  },
  postKDCAdminCredentials: async function (clusterName: string, payload: any) {
    const url = `clusters/${pathSegment(clusterName)}/credentials/kdc.admin.credential`
    const response = await ambariApi.request({
      url: url,
      method: "POST",
      data: payload
    })
    return response.data;
  },
  submitKDCAdminCredentials: async function (
    clusterName: string,
    payload: any,
    method: "POST" | "PUT",
  ) {
    const url = `clusters/${pathSegment(clusterName)}/credentials/kdc.admin.credential`
    const response = await ambariApi.request({
      url: url,
      method: method,
      data: payload
    })
    return response.data;
  },
  deleteKDCAdminCredentials: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/credentials/kdc.admin.credential`
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    })
    return response.data;
  },
  getKDCAdminCredentials: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/credentials?fields=Credential/*`
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
    const url = `clusters/${pathSegment(clusterName)}`;
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
    const url = `clusters/${pathSegment(clusterName)}/services/${pathSegment(serviceName)}`;
    const response = await ambariApi.request({
      url: url,
      method: "DELETE",
    });
    return { ...(response.data ?? {}), status: response.status };
  },

  createAdminSession: async function (clusterName: string, payloadData: any) {
    const url = `clusters/${pathSegment(clusterName)}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: payloadData,
    });
    return response.data;
  },

  getCredentialStoreInfo: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}?fields=Clusters/credential_store_properties`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getHeartbeatLostHosts: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/hosts?Hosts/host_state=HEARTBEAT_LOST`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  getKerberosClientState: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/services/KERBEROS/components/KERBEROS_CLIENT?fields=ServiceComponentInfo/state`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  installKerberosService: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/services?ServiceInfo/state=INSTALLED&ServiceInfo/service_name=KERBEROS`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Install Kerberos Service",
          operation_level: {
            level: "CLUSTER",
            cluster_name: clusterName,
          },
        },
        Body: {
          ServiceInfo: {
            state: "INSTALLED",
          },
        },
      },
      headers: {
        "Content-Type": "text/plain",
      },
    });
    return { ...response.data, status: response.status };
  },
  installKerberosClients: async function (
    clusterName: string,
    hostNames: string[],
  ) {
    const query = [
      "HostRoles/component_name=KERBEROS_CLIENT",
      `HostRoles/host_name.in(${hostNames.join(",")})`,
      "HostRoles/maintenance_state=OFF",
    ].join("&");
    const url = `clusters/${pathSegment(clusterName)}/host_components`;
    const response = await ambariApi.request({
      url,
      method: "PUT",
      data: {
        RequestInfo: {
          context: "Install Kerberos Client",
          operation_level: {
            level: "CLUSTER",
            cluster_name: clusterName,
          },
          query,
        },
        Body: {
          HostRoles: {
            state: "INSTALLED",
          },
        },
      },
      headers: {
        "Content-Type": "text/plain",
      },
    });
    return { ...response.data, status: response.status };
  },
  getAppTimelineServerHosts: async function (clusterName: string) {
    const url = `clusters/${pathSegment(clusterName)}/host_components?HostRoles/component_name=APP_TIMELINE_SERVER&fields=HostRoles/host_name&minimal_response=true`;
    const response = await ambariApi.request({
      url,
      method: "GET",
    });
    return response.data;
  },
  deleteAppTimelineServer: async function (
    clusterName: string,
    hostName: string,
  ) {
    const url = `clusters/${pathSegment(clusterName)}/hosts/${pathSegment(hostName)}/host_components/APP_TIMELINE_SERVER`;
    const response = await ambariApi.request({
      url,
      method: "DELETE",
    });
    return { ...(response.data ?? {}), status: response.status };
  },
};

export default KerberosApi;
