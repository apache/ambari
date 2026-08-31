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
import { ambariApplicationRoot, createCapacityApi, parseViewContext } from "./api";

const response = (body: unknown, status = 200) => new Response(
  typeof body === "string" ? body : JSON.stringify(body),
  { status, statusText: status >= 400 ? "Failed" : "OK" },
);

describe("capacity scheduler API", () => {
  it("parses versioned and unversioned View URLs", () => {
    expect(parseViewContext("/views/CAPACITY-SCHEDULER/1.0.0/AUTO_CS_INSTANCE")).toEqual({ view: "CAPACITY-SCHEDULER", version: "1.0.0", instance: "AUTO_CS_INSTANCE" });
    expect(parseViewContext("/views/CAPACITY-SCHEDULER/AUTO_CS_INSTANCE")).toEqual({ view: "CAPACITY-SCHEDULER", version: "", instance: "AUTO_CS_INSTANCE" });
  });

  it("preserves the Ambari reverse-proxy prefix for API requests", async () => {
    const path = "/gateway/default/ambari/views/CAPACITY-SCHEDULER/1.0.0/AUTO/";
    expect(ambariApplicationRoot(path)).toBe("/gateway/default/ambari/");
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(response({ items: [] }));
    const api = createCapacityApi(
      { view: "CAPACITY-SCHEDULER", version: "1.0.0", instance: "AUTO" },
      fetcher,
      path,
    );
    await api.latest();
    expect(fetcher.mock.calls[0][0]).toBe(
      "/gateway/default/ambari/api/v1/views/CAPACITY-SCHEDULER/versions/1.0.0/instances/AUTO/resources/scheduler/configuration",
    );
  });

  it("normalizes string-encoded node labels and ResourceManager queues", async () => {
    const fetcher = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(response(JSON.stringify({ nodeLabelInfo: [{ name: "gpu" }, { name: "batch" }] })))
      .mockResolvedValueOnce(response(JSON.stringify({ scheduler: { schedulerInfo: { queueName: "root", state: "RUNNING", queues: { queue: [{ queueName: "default", state: "STOPPED" }] } } } })));
    const api = createCapacityApi({ view: "CAPACITY-SCHEDULER", version: "1.0.0", instance: "AUTO" }, fetcher);
    expect(await api.nodeLabels()).toEqual(["gpu", "batch"]);
    expect(await api.rmQueues()).toEqual([{ path: "root", state: "RUNNING" }, { path: "root.default", state: "STOPPED" }]);
  });

  it("sends the legacy desired-config contract with a request header", async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(response({ resources: [] }));
    const api = createCapacityApi({ view: "CAPACITY-SCHEDULER", version: "", instance: "AUTO" }, fetcher);
    await api.save({ "yarn.scheduler.capacity.root.capacity": "100" }, "test note", "version123");
    const [url, init] = fetcher.mock.calls[0];
    expect(url).toContain("/instances/AUTO/resources/scheduler/configuration");
    expect(new Headers(init?.headers).get("X-Requested-By")).toBe("view-capacity-scheduler");
    expect(JSON.parse(String(init?.body))).toEqual({ Clusters: { desired_config: [{ type: "capacity-scheduler", tag: "version123", service_config_version_note: "test note", properties: { "yarn.scheduler.capacity.root.capacity": "100" } }] } });
  });

  it("falls back to read-only access when the privilege request fails", async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(response({ message: "denied" }, 403));
    const api = createCapacityApi({ view: "CAPACITY-SCHEDULER", version: "", instance: "AUTO" }, fetcher);
    expect(await api.privilege()).toBe(false);
  });
});
