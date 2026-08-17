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

import { AlertsApi } from "./alertsApi";

describe("alerts API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue({ data: { items: [] } });
  });

  it("loads alert instances for an encoded cluster and an exact host", async () => {
    await AlertsApi.getHostAlertInstances("cluster/name", "host & one", 1234);

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/cluster%2Fname/alerts",
      method: "GET",
      params: {
        fields: "*",
        "Alert/host_name": "host & one",
        _: 1234,
      },
    });
  });
});
