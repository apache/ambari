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

const AdminApi = {

  getNnCheckPointStatus: async function (clusterName:string, hostName:string) {
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/NAMENODE`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getNnCheckPointStatuses: async function (clusterName:string, hostNames:string) {
    const url = `/clusters/${clusterName}/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in(${hostNames})&fields=HostRoles/desired_state,metrics/dfs/namenode&minimal_response=true`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getJnCheckPointStatus:async function(clusterName:string, hostName:string){
    const url = `/clusters/${clusterName}/hosts/${hostName}/host_components/JOURNALNODE?fields=metrics`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getSecurityStatus:async function(clusterName:string){
    const url=`/clusters/${clusterName}?fields=Clusters/security_type`
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getSecurityType:async function(clusterName:string){
    const url=`/clusters/${clusterName}/configurations/service_config_versions?service_name=KERBEROS&is_current=true`
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },
  getKerberosSessionState:async function(clusterName:string){
    const url=`/clusters/${clusterName}/services/KERBEROS?fields=Services/attributes/kdc_validation_result,Services/attributes/kdc_validation_failure_details`
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  }
};

export default AdminApi