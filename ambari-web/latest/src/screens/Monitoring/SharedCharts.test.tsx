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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import type { Datasource } from "./types";
import { DASHBOARD_SCHEMA_VERSION } from "./types";

const mocks = vi.hoisted(() => ({
  getChartShares: vi.fn(),
  listDatasources: vi.fn(),
  queryRangeBatch: vi.fn(),
}));

vi.mock("../../api/metricsApi", () => ({
  default: mocks,
}));

vi.mock("./PrometheusChart", () => ({
  default: () => <div>Prometheus chart</div>,
}));

import SharedCharts from "./SharedCharts";

const datasource = {
  id: 7,
  name: "Prometheus",
  category: "prometheus",
  plugin_type: "prometheus",
  status: "enabled",
  is_default: true,
} as Datasource;

describe("SharedCharts cluster isolation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getChartShares.mockResolvedValue([{
      id: 5,
      cluster: "source-cluster",
      datasource_id: 7,
      configs: JSON.stringify({
        version: DASHBOARD_SCHEMA_VERSION,
        panel: {
          id: "shared-requests",
          name: "Shared requests",
          type: "timeseries",
          datasourceCate: "prometheus",
          layout: { h: 4, w: 12, x: 0, y: 0, i: "shared-requests", isResizable: true },
          targets: [{
            refId: "A",
            expr: 'rate(requests_total{cluster="${cluster}"}[$__rate_interval])',
          }],
        },
      }),
      create_at: 0,
      create_by: "operator",
    }]);
    mocks.listDatasources.mockResolvedValue([datasource]);
    mocks.queryRangeBatch.mockResolvedValue({ data: [{ status: "success", result: [] }] });
  });

  afterEach(cleanup);

  it("evaluates shared expressions against the cluster in AppContext", async () => {
    const context = { clusterName: "view-cluster" } as unknown as ComponentProps<
      typeof AppContext.Provider
    >["value"];
    render(
      <AppContext.Provider value={context}>
        <MemoryRouter initialEntries={["/main/monitoring/shared-charts/5"]}>
          <Routes>
            <Route path="/main/monitoring/shared-charts/:shareIds" element={<SharedCharts />} />
          </Routes>
        </MemoryRouter>
      </AppContext.Provider>,
    );

    expect(await screen.findByText("Shared requests")).toBeTruthy();
    await waitFor(() => expect(mocks.queryRangeBatch).toHaveBeenCalled());
    expect(mocks.queryRangeBatch.mock.calls[0][1][0].query).toBe(
      'rate(requests_total{cluster="view-cluster"}[120s])',
    );
    expect(screen.queryByTitle("Share chart")).toBeNull();
  });
});
