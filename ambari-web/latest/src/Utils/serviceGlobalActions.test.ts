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

const mocks = vi.hoisted(() => ({ clusterRequests: vi.fn() }));
vi.mock("../api/hostsApi", () => ({
  HostsApi: { clusterRequests: mocks.clusterRequests },
}));

import { restartAllRequired } from "./taskUtils";

describe("global service actions", () => {
  beforeEach(() => {
    mocks.clusterRequests.mockReset();
    mocks.clusterRequests.mockResolvedValue({});
  });

  it("restarts only stale host components with the classic predicate", async () => {
    await restartAllRequired("c1");

    expect(mocks.clusterRequests).toHaveBeenCalledWith("c1", {
      RequestInfo: {
        command: "RESTART",
        context: "Restart all required services",
        operation_level: "host_component",
      },
      "Requests/resource_filters": [
        {
          hosts_predicate:
            "HostRoles/stale_configs=true&HostRoles/cluster_name=c1",
        },
      ],
    });
  });
});
