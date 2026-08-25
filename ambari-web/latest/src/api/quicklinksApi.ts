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

// The quicklinks config is immutable stack metadata (link templates, port/host
// property names, protocol checks); the actual port/host VALUES come from a
// separate service_config_versions call. Caching it per (stack, version,
// service) for the session avoids refetching on every Quicklinks open / service
// switch, which was making service switches feel slow.
const quicklinksConfigCache = new Map<string, any>();
const quicklinksConfigInflight = new Map<string, Promise<any>>();

export const QuicklinksApi = {
    getQuicklinks: async (stackVersion: string,stackName:string, serviceName: string) => {
        const cacheKey = `${stackName}::${stackVersion}::${serviceName}`;
        const cached = quicklinksConfigCache.get(cacheKey);
        if (cached) {
            return cached;
        }
        const pending = quicklinksConfigInflight.get(cacheKey);
        if (pending) {
            return pending;
        }
        const url = `/stacks/${stackName}/versions/${stackVersion}/services/${serviceName}/quicklinks?QuickLinkInfo/default=true&fields=*`;
        const promise = ambariApi
            .request({ url, method: "GET" })
            .then((response) => {
                quicklinksConfigCache.set(cacheKey, response);
                return response;
            })
            .finally(() => {
                quicklinksConfigInflight.delete(cacheKey);
            });
        quicklinksConfigInflight.set(cacheKey, promise);
        return promise;
    },
    getPublicHostNames: async (clusterName: string, hostNames: string[]) => {
        const hosts = hostNames.map(encodeURIComponent).join(",");
        const url = `/clusters/${clusterName}/hosts?Hosts/host_name.in(${hosts})&fields=Hosts/public_host_name&minimal_response=true`;
        const response = await ambariApi.request({
            url,
            method: "GET",
        });
        return response.data;
    },
};
