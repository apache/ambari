/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import {
  authorizedDirectoryClusters,
  DirectoryRequestCancelledError,
  DirectoryRequestLimiter,
  safeDirectoryReturnPath,
  serviceRowKey,
  serviceRowsForCluster,
} from "./directoryUtils";

describe("global directory data projection", () => {
  it("keeps authorized clusters with authoritative numeric identities", () => {
    const clusters = authorizedDirectoryClusters([
      { Clusters: { cluster_id: 17, cluster_name: "alpha", provisioning_state: "INSTALLED" } },
      { Clusters: { cluster_name: "missing-id", provisioning_state: "INSTALLED" } },
      { Clusters: { cluster_id: 19, cluster_name: "denied", provisioning_state: "INSTALLED" } },
    ], (clusterName) => clusterName !== "denied");

    expect(clusters).toEqual([{
      clusterId: 17,
      clusterName: "alpha",
      provisioningState: "INSTALLED",
      version: "",
    }]);
  });

  it("keeps equal service types separate by cluster ID", () => {
    const response = {
      items: [{ ServiceInfo: { service_name: "HBASE", state: "STARTED" } }],
    };
    const alpha = serviceRowsForCluster({
      clusterId: 17,
      clusterName: "alpha",
      provisioningState: "INSTALLED",
      version: "3.0",
    }, response)[0];
    const beta = serviceRowsForCluster({
      clusterId: 29,
      clusterName: "beta",
      provisioningState: "INSTALLED",
      version: "3.0",
    }, response)[0];

    expect(serviceRowKey(alpha)).toBe("17:HBASE");
    expect(serviceRowKey(beta)).toBe("29:HBASE");
  });

  it("bounds requests and cancels queued work before dispatch", async () => {
    const limiter = new DirectoryRequestLimiter(4);
    let active = 0;
    let maximum = 0;
    const releases: Array<() => void> = [];
    const started: number[] = [];
    const requests = [1, 2, 3, 4, 5, 6].map((item) => limiter.run(async () => {
      started.push(item);
      active += 1;
      maximum = Math.max(maximum, active);
      await new Promise<void>((resolve) => releases.push(resolve));
      active -= 1;
    }));

    expect(started).toEqual([1, 2, 3, 4]);
    limiter.cancelPending();
    releases.forEach((release) => release());
    const results = await Promise.allSettled(requests);

    expect(maximum).toBe(4);
    expect(started).toEqual([1, 2, 3, 4]);
    expect(results.slice(4).every((result) => result.status === "rejected"
      && result.reason instanceof DirectoryRequestCancelledError)).toBe(true);
  });

  it("accepts only directory return locations", () => {
    expect(safeDirectoryReturnPath("/services?q=hdfs&page=2"))
      .toBe("/services?q=hdfs&page=2");
    expect(safeDirectoryReturnPath("/clusters?sort=state")).toBe("/clusters?sort=state");
    expect(safeDirectoryReturnPath("/services/%2e%2e/adminView")).toBeNull();
    expect(safeDirectoryReturnPath("//external.example/services")).toBeNull();
    expect(safeDirectoryReturnPath("/services#outside")).toBeNull();
    expect(safeDirectoryReturnPath("/main/dashboard/metrics")).toBeNull();
  });
});
