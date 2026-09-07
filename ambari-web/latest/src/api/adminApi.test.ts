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

import adminApi from "./adminApi";

describe("Admin API JMX control-plane fields", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.request.mockResolvedValue({ data: {} });
  });

  it("requests only the NameNode fields required by checkpoint validation", async () => {
    await adminApi.getNnCheckPointStatus("c1", "nn1");
    await adminApi.getNnCheckPointStatuses("c1", "nn1,nn2");

    expect(mocks.request).toHaveBeenNthCalledWith(1, {
      url: "/clusters/c1/hosts/nn1/host_components/NAMENODE?fields=HostRoles/desired_state,metrics/dfs/namenode/Safemode,metrics/dfs/namenode/JournalTransactionInfo",
      method: "GET",
    });
    expect(mocks.request).toHaveBeenNthCalledWith(2, {
      url: "/clusters/c1/host_components?HostRoles/component_name=NAMENODE&HostRoles/host_name.in(nn1,nn2)&fields=HostRoles/desired_state,metrics/dfs/namenode/Safemode,metrics/dfs/namenode/JournalTransactionInfo&minimal_response=true",
      method: "GET",
    });
  });

  it("requests only JournalNode formatted status", async () => {
    await adminApi.getJnCheckPointStatus("c1", "jn1");

    expect(mocks.request).toHaveBeenCalledWith({
      url: "/clusters/c1/hosts/jn1/host_components/JOURNALNODE?fields=metrics/dfs/journalnode/journalsStatus",
      method: "GET",
    });
  });
});
