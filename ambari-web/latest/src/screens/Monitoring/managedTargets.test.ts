/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import { managedTargetObservations } from "./managedTargets";
import type { PrometheusBatchResponse } from "../../api/metricsApi";

const labels = { cluster: "c1", ambari_cluster_id: "52", host: "worker1",
  ambari_target: "component", service: "HDFS", component: "DATANODE" };
const discovery = [{ labels: { ...labels, __metrics_path__: "/metrics/components/dn" }, targets: ["worker1:9101"] }];
const response = (): PrometheusBatchResponse => ({
  status: "success",
  data: [
    { refId: "up", status: "success", result: [{ metric: labels, value: [1000, "1"] }] },
    { refId: "timestamp", status: "success", result: [{ metric: labels, value: [1000, "980"] }] },
    { refId: "duration", status: "success", result: [{ metric: labels, value: [1000, "0.024"] }] },
  ],
});

describe("managed target observations", () => {
  it("joins discovery with fresh samples from the same cluster and component", () => {
    const [target] = managedTargetObservations(discovery, response(), "c1", 1000);
    expect(target).toMatchObject({ health: "up", scrapeUrl: "http://worker1:9101/metrics/components/dn",
      lastScrapeDuration: 0.024, lastScrape: new Date(980000).toISOString() });
  });
  it("does not turn missing, stale, future, or conflicting observations into success", () => {
    for (const stamp of [null, "500", "1100"]) {
      const data = response();
      data.data![1].result = stamp === null ? [] : [{ metric: labels, value: [1000, stamp] }];
      expect(managedTargetObservations(discovery, data, "c1", 1000)[0].health).toBe("unknown");
    }
    const conflict = response();
    conflict.data![0].result!.push({ metric: { ...labels, instance: "other-collector" }, value: [1000, "0"] });
    expect(managedTargetObservations(discovery, conflict, "c1", 1000)[0].health).toBe("unknown");
  });
  it("isolates foreign cluster, component, and persistent cluster identities", () => {
    for (const metric of [{ ...labels, cluster: "foreign" }, { ...labels, component: "NAMENODE" }, { ...labels, ambari_cluster_id: "99" }]) {
      const data = response();
      data.data![0].result![0].metric = metric;
      expect(managedTargetObservations(discovery, data, "c1", 1000)[0].health).toBe("unknown");
    }
    expect(() => managedTargetObservations([{ ...discovery[0], labels: { ...labels, cluster: "foreign" } }], response(), "c1", 1000)).toThrow();
  });
  it("preserves explicit scrape failure and rejects malformed or partial envelopes", () => {
    const failed = response();
    failed.data![0].result![0].value![1] = "0";
    expect(managedTargetObservations(discovery, failed, "c1", 1000)[0].health).toBe("down");
    expect(() => managedTargetObservations({}, response(), "c1", 1000)).toThrow();
    expect(() => managedTargetObservations(discovery, { data: [] }, "c1", 1000)).toThrow();
    const invalid = response();
    invalid.data![1] = { refId: "timestamp", status: "error", error: "diagnostic noise: success" };
    expect(() => managedTargetObservations(discovery, invalid, "c1", 1000)).toThrow();
  });
  it("never interprets diagnostics or invalid numbers as healthy observations", () => {
    const data = response();
    data.data![0].result = [{ metric: labels, value: [1000, "NaN"] }, null as never];
    expect(managedTargetObservations(discovery, data, "c1", 1000)[0].health).toBe("unknown");
  });
});
