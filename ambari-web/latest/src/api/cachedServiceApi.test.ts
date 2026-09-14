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

const mocks = vi.hoisted(() => ({ getAllServiceComponents: vi.fn() }));
vi.mock("./serviceApi", () => ({
  ServiceApi: { getAllServiceComponents: mocks.getAllServiceComponents },
}));

import { CachedServiceApiManager } from "./cachedServiceApi";

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => { resolve = done; });
  return { promise, resolve };
}

describe("CachedServiceApiManager runtime ownership", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("does not let an old principal completion repopulate or notify a replacement scope", async () => {
    const manager = new CachedServiceApiManager();
    const aliceResponse = deferred<any>();
    const bobData = { items: [{ ServiceComponentInfo: { service_name: "HDFS" } }] };
    mocks.getAllServiceComponents
      .mockReturnValueOnce(aliceResponse.promise)
      .mockResolvedValueOnce({ data: bobData });
    const aliceSubscriber = vi.fn();
    const bobSubscriber = vi.fn();

    manager.subscribe("alice:shared", aliceSubscriber);
    const aliceRequest = manager.fetchAllServiceComponents("shared", "alice:shared");
    manager.clear("alice:shared");
    manager.subscribe("bob:shared", bobSubscriber);
    await manager.fetchAllServiceComponents("shared", "bob:shared");

    aliceResponse.resolve({ data: { items: [{ ServiceComponentInfo: { service_name: "YARN" } }] } });
    await aliceRequest;

    expect(aliceSubscriber).not.toHaveBeenCalled();
    expect(bobSubscriber).toHaveBeenCalledOnce();
    expect(manager.getAllComponentData("bob:shared")).toEqual(bobData);
    expect(manager.getAllComponentData("alice:shared")).toBeNull();
  });

  it("deduplicates only within the same runtime scope", async () => {
    const manager = new CachedServiceApiManager();
    const responseA = deferred<any>();
    const responseB = deferred<any>();
    mocks.getAllServiceComponents
      .mockReturnValueOnce(responseA.promise)
      .mockReturnValueOnce(responseB.promise);

    const firstA = manager.fetchAllServiceComponents("A", "user:A");
    const secondA = manager.fetchAllServiceComponents("A", "user:A");
    const firstB = manager.fetchAllServiceComponents("B", "user:B");
    expect(mocks.getAllServiceComponents).toHaveBeenCalledTimes(2);

    responseA.resolve({ data: { items: [] } });
    responseB.resolve({ data: { items: [] } });
    const [firstAData, secondAData, firstBData] = await Promise.all([firstA, secondA, firstB]);
    expect(firstAData).toEqual({ items: [] });
    expect(secondAData).toBe(firstAData);
    expect(firstBData).toEqual({ items: [] });
  });

  it("does not notify a recreated entry when an old request used the same key", async () => {
    const manager = new CachedServiceApiManager();
    const staleResponse = deferred<any>();
    const currentData = { items: [{ ServiceComponentInfo: { service_name: "HDFS" } }] };
    mocks.getAllServiceComponents
      .mockReturnValueOnce(staleResponse.promise)
      .mockResolvedValueOnce({ data: currentData });
    const currentSubscriber = vi.fn();

    const staleRequest = manager.fetchAllServiceComponents("shared", "same-key");
    manager.clear("same-key");
    manager.subscribe("same-key", currentSubscriber);
    await manager.fetchAllServiceComponents("shared", "same-key");
    staleResponse.resolve({
      data: { items: [{ ServiceComponentInfo: { service_name: "STALE" } }] },
    });
    await staleRequest;

    expect(currentSubscriber).toHaveBeenCalledOnce();
    expect(manager.getAllComponentData("same-key")).toEqual(currentData);
  });
});
