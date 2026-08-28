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
  delete: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  request: vi.fn(),
  suppressedGet: vi.fn(),
  suppressedPost: vi.fn(),
}));

vi.mock("./config/axiosConfig", () => ({
  ambariApi: {
    delete: mocks.delete,
    get: mocks.get,
    post: mocks.post,
    put: mocks.put,
    request: mocks.request,
  },
  supressErrorAmbariApi: {
    get: mocks.suppressedGet,
    post: mocks.suppressedPost,
  },
}));

import MetricsApi from "./metricsApi";

const envelope = <T>(value: T) => ({ data: { data: value, error: "" } });

describe("monitoring API", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("preserves datasource cluster scoping and unwraps responses", async () => {
    const datasource = { id: 7, name: "default" };
    mocks.post
      .mockResolvedValueOnce(envelope([datasource]))
      .mockResolvedValueOnce(envelope(datasource))
      .mockResolvedValueOnce(envelope({ status: "success" }));
    mocks.delete.mockResolvedValueOnce(envelope(true));

    await expect(MetricsApi.listDatasources("cluster-a")).resolves.toEqual([datasource]);
    await MetricsApi.updateDatasourceStatus("cluster-a", 7, "disabled");
    await MetricsApi.testDatasource("cluster-a", 7);
    await expect(MetricsApi.deleteDatasource("cluster-a", 7)).resolves.toBe(true);

    expect(mocks.post).toHaveBeenNthCalledWith(1, "/metrics/datasource/list", {
      cluster_name: "cluster-a",
    });
    expect(mocks.post).toHaveBeenNthCalledWith(2, "/metrics/datasource/status/update", {
      id: 7,
      status: "disabled",
      cluster_name: "cluster-a",
    });
    expect(mocks.post).toHaveBeenNthCalledWith(
      3,
      "/metrics/datasource/7/test",
      undefined,
      { params: { cluster_name: "cluster-a" } },
    );
    expect(mocks.delete).toHaveBeenCalledWith("/metrics/datasource/7", {
      params: { cluster_name: "cluster-a" },
    });
  });

  it("forwards Prometheus queries through the selected datasource", async () => {
    mocks.suppressedGet.mockResolvedValue({
      data: { status: "success", data: { resultType: "vector", result: [] } },
    });

    await MetricsApi.query(4, "up", 123);
    await MetricsApi.queryRange(4, "rate(x[5m])", 100, 200, 15);
    await MetricsApi.labelValues(4, "service/name");

    expect(mocks.suppressedGet).toHaveBeenNthCalledWith(
      1,
      "/metrics/4/api/v1/query",
      { params: { query: "up", time: 123 } },
    );
    expect(mocks.suppressedGet).toHaveBeenNthCalledWith(
      2,
      "/metrics/4/api/v1/query_range",
      { params: { query: "rate(x[5m])", start: 100, end: 200, step: 15 } },
    );
    expect(mocks.suppressedGet).toHaveBeenNthCalledWith(
      3,
      "/metrics/4/api/v1/label/service%2Fname/values",
    );
  });

  it("uses cluster-scoped dashboard routes without rewriting payloads", async () => {
    const dashboard = { id: 9, name: "HDFS" };
    mocks.get.mockResolvedValueOnce(envelope([dashboard]));
    mocks.put.mockResolvedValueOnce(envelope(dashboard));

    await expect(MetricsApi.listDashboards("cluster-a", "hdfs")).resolves.toEqual([
      dashboard,
    ]);
    await MetricsApi.updateDashboardConfigs("cluster-a", 9, "{\"panels\":[]}");

    expect(mocks.get).toHaveBeenCalledWith("/metrics/boards", {
      params: { cluster_name: "cluster-a", query: "hdfs" },
    });
    expect(mocks.put).toHaveBeenCalledWith(
      "/metrics/board/9/configs",
      { configs: "{\"panels\":[]}" },
      { params: { cluster_name: "cluster-a" } },
    );
  });

  it("keeps chart share IDs and datasource references intact", async () => {
    mocks.post.mockResolvedValueOnce(envelope([21, 22]));
    mocks.get.mockResolvedValueOnce(envelope([{ id: 21 }, { id: 22 }]));
    const shares = [
      { datasource_id: 3, configs: "{\"dataProps\":{}}" },
      { datasource_id: 4, configs: "{\"dataProps\":{}}" },
    ];

    await expect(MetricsApi.createChartShares("cluster-a", shares)).resolves.toEqual([
      21, 22,
    ]);
    await MetricsApi.getChartShares("cluster-a", "21,22");

    expect(mocks.post).toHaveBeenCalledWith("/metrics/share-charts", shares, {
      params: { cluster_name: "cluster-a" },
    });
    expect(mocks.get).toHaveBeenCalledWith("/metrics/share-charts", {
      params: { cluster_name: "cluster-a", ids: "21,22" },
    });
  });
});
