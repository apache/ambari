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
import { ambariApi } from "./config/axiosConfig";

interface ServiceInfo {
  desired_repository_version_id: number;
  maintenance_state: string;
  service_name: string;
  state: string;
}

interface ServiceComponent {
  ServiceComponentInfo: {
    component_name: string;
  };
}

interface ServiceResponse {
  ServiceInfo: ServiceInfo;
  components: ServiceComponent[];
}

interface ServicesListResponse {
  items: ServiceResponse[];
}

export const ServiceApi = {
  getServiceState: async function (clusterName: string, serviceName: string) {
    // console.log(
    //   "clusterName === ",
    //   clusterName,
    //   "serviceName === ",
    //   serviceName
    // );
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/services/${serviceName}`,
      method: "GET",
    });
    return response;
  },

  /**
   * Fetches all service information for a given cluster.
   * fields like state, maintenance_state, desired_repository_version_id, and component names to name a few.
   * @param clusterName
   */
  getAllServices: async function (clusterName: string): Promise<ServicesListResponse> {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state,ServiceInfo/desired_repository_version_id,components/ServiceComponentInfo/component_name&minimal_response=true`,
      method: "GET",
    });
    return response.data;
  },

  serviceComponents: async function (clusterName: string, serviceName: string) {
    const response = await ambariApi.request({
      url: `/clusters/${clusterName}/services/${serviceName}/components`,
      method: "GET",
    });
    return response;
  },

  createComponent:async function(clusterName:string,serviceName:string,componentName:string){
    const url=`/clusters/${clusterName}/services?ServiceInfo/service_name=${serviceName}`;
    const response=await ambariApi.request({
      url,
      data:{
        components:[{
          ServiceComponentInfo:{
            component_name:componentName
          }
        }]
      }
    })
    return response.data
  },

  updateService: async function (
    clusterName: string,
    data: any,
    urlParams: string
  ) {
    const url = `/clusters/${clusterName}/services?${urlParams}`;
    const response = await ambariApi.request({
      url: url,
      method: "PUT",
      data: {
        RequestInfo: {
          context: data.context,
          operation_level: {
            level: "CLUSTER",
            cluster_name: clusterName,
          },
        },
        Body: {
          ServiceInfo: data.ServiceInfo,
        },
      },
    });
    set(response, "data.status", response?.status)
    return response.data;
  },

  ambariService: async function (fields: string) {
    const url = `/services/AMBARI/components/AMBARI_SERVER${fields}`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response.data;
  },

  getAllServiceComponentsListAndInitialMetrics: async (clusterName: string, fields: string) => {
    const url = `clusters/${clusterName}/components/?fields=${fields}&_=${Date.now()}\``
    const response = await ambariApi.request({url: url,
      method: "GET",
    });
    return response;
  },

  adminAboutInfo: async (fields: string) => {
    const response = await ambariApi.request({
      url: `/services/AMBARI/components/AMBARI_SERVER?fields=${fields}`,
      method: 'GET'
    });
    return response.data;
  },

  getAmbariServerVersion: async function() {
    const response = await ambariApi.request({
      url: '/services/AMBARI?fields=components/RootServiceComponents/component_version&components/RootServiceComponents/component_name=AMBARI_SERVER&minimal_response=true',
      method: 'GET'
    });
    console.log("response", response.data)
    return response.data;
  },
  // @ts-ignore
  isServiceCheckSupported: async function (clusterName: string, serviceName: string, stackName: string, stackVersion) {
    const url = `/stacks/${stackName}/versions/${stackVersion}/services/${serviceName}?fields=StackServices/service_check_supported`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response;
  },
  removeService: async function (clusterName: string, serviceName: string, serviceToDeleteNow: string, servicesToDeleteNext: any) {
    const url = `/clusters/${clusterName}/services/${serviceName}`
    const response =  await ambariApi.request({
      url: url,
      method: "DELETE",
       headers: {
        'Content-Type': 'application/json',
        'X-Requested-By': 'ambari-web'
      },
      data: {
        serviceName : serviceToDeleteNow,
        servicesToDeleteNext: servicesToDeleteNext
      }
    });
    return response;
  },

  /**
   * Fetches service component details including rolling restart support information
   * @param stackName The name of the stack (e.g., "VDP")
   * @param stackVersion The version of the stack (e.g., "3.4")
   * @param serviceName The name of the service (e.g., "HDFS")
   * @returns Response containing service and component details
   */
  getServiceComponentDetails: async function (stackName: string, stackVersion: string, serviceName: string) {
    const url = `/stacks/${stackName}/versions/${stackVersion}/services/${serviceName}?fields=StackServices/*,components/*,components/dependencies/Dependencies/scope,components/dependencies/Dependencies/service_name,artifacts/Artifacts/artifact_name`;
    const response = await ambariApi.request({
      url: url,
      method: "GET",
    });
    return response;
  }
};
