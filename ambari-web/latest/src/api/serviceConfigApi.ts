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
export const ServiceConfigApi = {
    getServiceConfig: async function (clusterName: string, serviceName: string) {
        const url = `/clusters/${clusterName}/configurations/service_config_versions?service_name=${serviceName}&fields=service_config_version,user,hosts,group_id,group_name,is_current,createtime,service_name,service_config_version_note,stack_id,is_cluster_compatible&sortBy=service_config_version.desc&minimal_response=true`;
        const response = await ambariApi.request({
            url: url,
            method: "GET",
        });
        return response;
    },
    setIsCurrent: async function (clusterName: string, selectedServices: string[]) {
        const servicesQuery = selectedServices.join(',');
        const url = `/clusters/${clusterName}/configurations/service_config_versions?service_name.in(${servicesQuery})&is_current=true&fields=*&_=${Date.now()}\`;`
        const response = await ambariApi.request({
            url: url,
            method: "GET",
        });
        return response;
    },
};