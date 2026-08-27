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

const mocks = vi.hoisted(() => ({
  request: vi.fn(),
  suppressedRequest: vi.fn(),
}));
vi.mock("./config/axiosConfig", () => ({
  ambariApi: { request: mocks.request },
  supressErrorAmbariApi: { request: mocks.suppressedRequest },
}));

import ClusterApi from "./clusterApi";

describe("cluster persisted data API", () => {
  beforeEach(() => {
    mocks.request.mockReset();
    mocks.suppressedRequest.mockReset();
  });

  it("loads the aggregate persisted state and returns an optional key", async () => {
    mocks.suppressedRequest.mockResolvedValue({
      data: { CLUSTER_STATE: { clusterName: "c1" }, timezone: "UTC" },
    });

    await expect(ClusterApi.getPersistData("CLUSTER_STATE")).resolves.toEqual({
      clusterName: "c1",
    });
    await expect(ClusterApi.getPersistData()).resolves.toEqual({
      CLUSTER_STATE: { clusterName: "c1" },
      timezone: "UTC",
    });
    expect(mocks.suppressedRequest).toHaveBeenNthCalledWith(1, {
      url: "/persist",
      method: "GET",
    });
    expect(mocks.suppressedRequest).toHaveBeenNthCalledWith(2, {
      url: "/persist",
      method: "GET",
    });
  });

  it("deduplicates concurrent aggregate requests and treats missing keys as optional", async () => {
    let resolveRequest: (value: unknown) => void = () => undefined;
    mocks.suppressedRequest.mockReturnValue(new Promise((resolve) => {
      resolveRequest = resolve;
    }));

    const first = ClusterApi.getPersistData("first");
    const missing = ClusterApi.getPersistData("missing");
    resolveRequest({ data: { first: 1 } });

    await expect(first).resolves.toBe(1);
    await expect(missing).resolves.toBeUndefined();
    expect(mocks.suppressedRequest).toHaveBeenCalledTimes(1);
  });

  it("clears a rejected in-flight request so a later read can retry", async () => {
    mocks.suppressedRequest
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce({ data: { recovered: true } });

    await expect(ClusterApi.getPersistData("recovered")).rejects.toThrow("offline");
    await expect(ClusterApi.getPersistData("recovered")).resolves.toBe(true);
    expect(mocks.suppressedRequest).toHaveBeenCalledTimes(2);
  });
});
