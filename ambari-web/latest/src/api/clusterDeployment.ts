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
const ClusterDeploymentApi = {
    createCluster:async function createCluster(clusterName:string,data:any){{
        const url=`/clusters/${clusterName}`;
        const response=await ambariApi.request({
            url,
            method:"POST",
            data
        })
        return response.data
    }},
    createSelectedServices:async function createCluster(clusterName:string,data:any){{
        const url=`/clusters/${clusterName}/services`;
        return  ambariApi.request({
            url,
            method:"POST",
            data
        })
    }},
    addRequestToCreateComponent:async function addRequestToCreateComponent(clusterName:string,serviceName:string,data:any){{
        const url=`/clusters/${clusterName}/services?ServiceInfo/service_name=${serviceName}`
        const response=await ambariApi.request({
            url,
            method:"POST",
            data
        })
        return response.data
    }},
    registerHostToCluster:async function registerHostToCluster(clusterName:string,data:any){{
        const url=`/clusters/${clusterName}/hosts`
        const response=await ambariApi.request({
            url,
            method:"POST",
            data
        })
        return response.data
    }},
    applyClusterConfigs: async function(clusterName:string, applyConfigsPaylpoad: any) {
        const url = `/clusters/${clusterName}`;
        return ambariApi.request({
          url: url,
          method: "PUT",
          data: applyConfigsPaylpoad
        });

      }
};

export default ClusterDeploymentApi;