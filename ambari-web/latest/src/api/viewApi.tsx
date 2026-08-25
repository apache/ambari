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
import { supressErrorAmbariApi } from "./config/axiosConfig";

const ViewApi = {
  getDefinitions: async function() {
    const response = await supressErrorAmbariApi.request({
      url: "/views",
      method: "GET",
    });
    return response.data;
  },

  getInstances: async function() {
    const definitions = await ViewApi.getDefinitions();
    if (!definitions?.items?.length) {
      return { items: [] };
    }

    const response = await supressErrorAmbariApi.request({
      url: "/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label&versions/ViewVersionInfo/system=false",
      method: "GET",
    });
    return response.data;
  },
};

export default ViewApi;
