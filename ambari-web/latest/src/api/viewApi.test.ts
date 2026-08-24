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
vi.mock("./config/axiosConfig", () => ({
  supressErrorAmbariApi: { request: mocks.request },
}));

import ViewApi from "./viewApi";

describe("View API", () => {
  beforeEach(() => mocks.request.mockReset());

  it("loads definitions before requesting non-system instances", async () => {
    mocks.request
      .mockResolvedValueOnce({ data: { items: [{ ViewInfo: { view_name: "TEZ" } }] } })
      .mockResolvedValueOnce({ data: { items: [{ versions: [] }] } });

    await expect(ViewApi.getInstances()).resolves.toEqual({ items: [{ versions: [] }] });
    expect(mocks.request.mock.calls).toEqual([
      [{ url: "/views", method: "GET" }],
      [{
        url: "/views?fields=versions/instances/ViewInstanceInfo,versions/ViewVersionInfo/label&versions/ViewVersionInfo/system=false",
        method: "GET",
      }],
    ]);
  });

  it("does not send the instance query when no View definition exists", async () => {
    mocks.request.mockResolvedValueOnce({ data: { items: [] } });
    await expect(ViewApi.getInstances()).resolves.toEqual({ items: [] });
    expect(mocks.request).toHaveBeenCalledTimes(1);
  });

  it("does not hide definition request failures", async () => {
    mocks.request.mockRejectedValueOnce(new Error("unavailable"));
    await expect(ViewApi.getInstances()).rejects.toThrow("unavailable");
  });
});
