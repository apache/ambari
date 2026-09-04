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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";
import type { Datasource, DashboardPanel as DashboardPanelConfig } from "../types";

const mocks = vi.hoisted(() => ({
  createChartShares: vi.fn(),
  queryInstantBatch: vi.fn(),
  queryRangeBatch: vi.fn(),
}));

vi.mock("../../../api/metricsApi", () => ({
  default: mocks,
}));

vi.mock("../PrometheusChart", () => ({
  default: () => <div>Prometheus chart</div>,
}));

import DashboardPanel from "./DashboardPanel";

const datasource = {
  id: 7,
  name: "Prometheus",
  category: "prometheus",
  plugin_type: "prometheus",
  status: "enabled",
  is_default: true,
} as Datasource;

const panel: DashboardPanelConfig = {
  id: "requests",
  name: "Requests",
  type: "timeseries",
  datasourceCate: "prometheus",
  datasourceValue: 7,
  layout: { h: 4, w: 12, x: 0, y: 0, i: "requests", isResizable: true },
  targets: [{
    refId: "A",
    expr: 'rate(requests_total{cluster="${cluster}"}[$__rate_interval])',
  }],
};

const contextValue = (clusterName: string) => ({ clusterName }) as unknown as ComponentProps<
  typeof AppContext.Provider
>["value"];

describe("DashboardPanel cluster isolation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.queryRangeBatch.mockResolvedValue({ data: [{ status: "success", result: [] }] });
    mocks.createChartShares.mockResolvedValue([19]);
    vi.spyOn(window, "open").mockImplementation(() => null);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("replaces protected variables for queries and persisted chart shares", async () => {
    const variables = { cluster: "payload-cluster", __rate_interval: "1s" };
    const view = render(
      <AppContext.Provider value={contextValue("cluster-a")}>
        <DashboardPanel
          panel={panel}
          start={0}
          end={7200}
          refreshKey={0}
          variables={variables}
          datasources={[datasource]}
        />
      </AppContext.Provider>,
    );

    await waitFor(() => expect(mocks.queryRangeBatch).toHaveBeenCalledWith(
      7,
      [{
        refId: "A",
        query: 'rate(requests_total{cluster="cluster-a"}[120s])',
        start: 0,
        end: 7200,
        step: 30,
      }],
      expect.any(AbortSignal),
    ));

    view.rerender(
      <AppContext.Provider value={contextValue("cluster-b")}>
        <DashboardPanel
          panel={panel}
          start={0}
          end={7200}
          refreshKey={0}
          variables={variables}
          datasources={[datasource]}
        />
      </AppContext.Provider>,
    );

    await waitFor(() => expect(mocks.queryRangeBatch).toHaveBeenLastCalledWith(
      7,
      [{
        refId: "A",
        query: 'rate(requests_total{cluster="cluster-b"}[120s])',
        start: 0,
        end: 7200,
        step: 30,
      }],
      expect.any(AbortSignal),
    ));

    fireEvent.click(screen.getByTitle("Share chart"));
    await waitFor(() => expect(mocks.createChartShares).toHaveBeenCalled());
    const [shareCluster, shares] = mocks.createChartShares.mock.calls.at(-1) || [];
    const saved = JSON.parse(shares[0].configs);

    expect(shareCluster).toBe("cluster-b");
    expect(saved.version).toBe("3.0.0");
    expect(saved.panel.targets[0].expr).toBe(
      'rate(requests_total{cluster="cluster-b"}[120s])',
    );
  });

  it("keeps target legends for a multi-target stat panel", async () => {
    mocks.queryInstantBatch.mockResolvedValueOnce({
      data: [
        { status: "success", result: [{ metric: {}, value: [7200, "3"] }] },
        { status: "success", result: [{ metric: { host: "worker-1" }, value: [7200, "2"] }] },
      ],
    });

    render(
      <AppContext.Provider value={contextValue("cluster-a")}>
        <DashboardPanel
          panel={{
            ...panel,
            type: "stat",
            options: { standardOptions: { util: "cps" } },
            targets: [
              { refId: "A", expr: "active", legend: "Active ${cluster}" },
              { refId: "B", expr: "lost", legend: "Lost on {{ host }}" },
            ],
          }}
          start={0}
          end={7200}
          refreshKey={0}
          variables={{}}
          datasources={[datasource]}
        />
      </AppContext.Provider>,
    );

    await waitFor(() => {
      expect(screen.getByText("Active cluster-a")).toBeTruthy();
      expect(screen.getByText("Lost on worker-1")).toBeTruthy();
    });
    expect(screen.getByText("3 cps")).toBeTruthy();
    expect(screen.getByText("2 cps")).toBeTruthy();
    expect(mocks.queryInstantBatch).toHaveBeenCalledWith(
      7,
      [{ refId: "A", query: "active", time: 7200 }, { refId: "B", query: "lost", time: 7200 }],
      expect.any(AbortSignal),
    );
  });
});
