/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file to You under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({ ambariApi: { request: mocks.request } }));

import { CentralizedServiceStateApi } from "./centralizedServiceStateApi";

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => { resolve = done; });
  return { promise, resolve };
}

describe("CentralizedServiceStateApi runtime ownership", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("drops a cleared principal's late response before cache or subscriber updates", async () => {
    const api = new CentralizedServiceStateApi();
    const aliceResponse = deferred<any>();
    mocks.request
      .mockReturnValueOnce(aliceResponse.promise)
      .mockResolvedValueOnce({
        data: { items: [{ ServiceInfo: { service_name: "HDFS", state: "STARTED" } }] },
      });
    const bobSubscriber = vi.fn();

    const aliceRequest = api.fetchAllServiceStatesAndAlerts("shared", "alice:shared");
    api.clearCache("alice:shared");
    api.subscribe("bob:shared", bobSubscriber);
    await api.fetchAllServiceStatesAndAlerts("shared", "bob:shared");
    aliceResponse.resolve({
      data: { items: [{ ServiceInfo: { service_name: "YARN", state: "STARTED" } }] },
    });
    await aliceRequest;

    expect(bobSubscriber).toHaveBeenCalledOnce();
    expect(api.getServiceStateData("bob:shared", "HDFS")?.state).toBe("STARTED");
    expect(api.getServiceStateData("alice:shared", "YARN")).toBeNull();
  });

  it("does not notify a recreated entry when an old request used the same key", async () => {
    const api = new CentralizedServiceStateApi();
    const staleResponse = deferred<any>();
    mocks.request
      .mockReturnValueOnce(staleResponse.promise)
      .mockResolvedValueOnce({
        data: { items: [{ ServiceInfo: { service_name: "HDFS", state: "STARTED" } }] },
      });
    const currentSubscriber = vi.fn();

    const staleRequest = api.fetchAllServiceStatesAndAlerts("shared", "same-key");
    api.clearCache("same-key");
    api.subscribe("same-key", currentSubscriber);
    await api.fetchAllServiceStatesAndAlerts("shared", "same-key");
    staleResponse.resolve({
      data: { items: [{ ServiceInfo: { service_name: "YARN", state: "STARTED" } }] },
    });
    await staleRequest;

    expect(currentSubscriber).toHaveBeenCalledOnce();
    expect(api.getServiceStateData("same-key", "HDFS")?.state).toBe("STARTED");
    expect(api.getServiceStateData("same-key", "YARN")).toBeNull();
  });
});
