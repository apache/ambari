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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({ ambariApi: { request: mocks.request } }));

import HostLogsApi from "./hostLogsApi";

describe("host logs API", () => {
  beforeEach(() => mocks.request.mockResolvedValue({ data: {} }));

  it("loads host logging metadata with encoded resource identifiers", async () => {
    await HostLogsApi.fetchHostLogs("cluster/name", "host name");
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/hosts/host%20name",
      method: "GET",
      params: {
        fields: "host_components/logging,host_components/HostRoles/service_name,host_components/HostRoles/component_name,host_components/HostRoles/display_name",
        minimal_response: true,
      },
    });
  });

  it("loads a bounded log tail without interpolating query values", async () => {
    await HostLogsApi.fetchLogTail("c1", "hdfs_namenode", "host&1", 100, 25);
    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/logging/searchEngine",
      method: "GET",
      params: {
        component_name: "hdfs_namenode",
        host_name: "host&1",
        pageSize: 100,
        startIndex: 25,
      },
    });
  });
});
