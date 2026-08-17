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

import { HostsApi } from "./hostsApi";

describe("Hosts API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue({ data: {} });
  });

  it("sends suggestion paging as Axios params and the predicate in the override body", async () => {
    await HostsApi.getHostListFilterSuggestions("cluster/name", {
      filter: "host_name",
      pageSize: 25,
      searchTerm: "node[1]",
    });

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/hosts",
      method: "POST",
      params: {
        fields: "Hosts/host_name",
        minimal_response: true,
        page_size: 25,
      },
      headers: {
        "X-Http-Method-Override": "GET",
      },
      data: JSON.stringify({
        RequestInfo: {
          query: "Hosts/host_name.matches(.*node\\[1\\].*)",
        },
      }),
    });
  });
});
