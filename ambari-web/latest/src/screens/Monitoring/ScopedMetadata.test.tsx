/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import MetricsApi from "../../api/metricsApi";
import { AppContext } from "../../store/context";
import Explorer from "./Explorer";
import Targets from "./Targets";

vi.mock("../../api/metricsApi", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/metricsApi")>();
  return {
    ...actual,
    default: {
      ...actual.default,
      labels: vi.fn(),
      listDatasources: vi.fn(),
      queryRange: vi.fn(),
      targets: vi.fn(),
    },
  };
});

const unsupported = {
  response: { data: { code: "METRICS_SCOPE_UNSUPPORTED" } },
};

const datasource = {
  id: 7,
  name: "Managed metrics",
  status: "enabled",
  plugin_type: "prometheus",
  category: "prometheus",
  is_default: true,
};

const secondDatasource = {
  ...datasource,
  id: 8,
  name: "Secondary metrics",
  is_default: false,
};

const deferred = <T,>() => {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
};

describe("scoped metrics metadata", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    vi.mocked(MetricsApi.listDatasources).mockResolvedValue([datasource] as any);
    vi.mocked(MetricsApi.labels).mockRejectedValue(unsupported);
    vi.mocked(MetricsApi.targets).mockRejectedValue(unsupported);
    vi.mocked(MetricsApi.queryRange).mockResolvedValue({
      status: "success",
      data: { resultType: "matrix", result: [] },
    });
  });

  afterEach(() => cleanup());

  it("keeps Explorer queries available when label discovery is unsupported", async () => {
    render(
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <Explorer />
      </AppContext.Provider>,
    );

    expect(await screen.findByText(/label discovery is unavailable/)).toBeTruthy();
    fireEvent.submit(screen.getByLabelText("PromQL").closest("form") as HTMLFormElement);
    await waitFor(() => expect(MetricsApi.queryRange).toHaveBeenCalledOnce());
  });

  it("shows a terminal local Targets message without retrying the denied endpoint", async () => {
    render(
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <Targets />
      </AppContext.Provider>,
    );

    expect(await screen.findByText(/Scrape target metadata is unavailable/)).toBeTruthy();
    await waitFor(() => expect(MetricsApi.targets).toHaveBeenCalledOnce());
    expect(screen.getByTitle("Refresh targets").hasAttribute("disabled")).toBe(true);
    expect(screen.queryByText(/No active targets/)).toBeNull();
  });

  it("clears rows and restores the selected datasource's cached unsupported state", async () => {
    vi.mocked(MetricsApi.listDatasources).mockResolvedValue([datasource, secondDatasource] as any);
    vi.mocked(MetricsApi.targets).mockImplementation(async (id) => {
      if (id === datasource.id) throw unsupported;
      return {
        data: { activeTargets: [{ scrapeUrl: "http://secondary/metrics", health: "up" }] },
      } as any;
    });
    render(
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <Targets />
      </AppContext.Provider>,
    );

    expect(await screen.findByText(/Scrape target metadata is unavailable/)).toBeTruthy();
    fireEvent.change(screen.getByLabelText("Datasource"), { target: { value: "8" } });
    expect(await screen.findByText("http://secondary/metrics")).toBeTruthy();
    fireEvent.change(screen.getByLabelText("Datasource"), { target: { value: "7" } });
    expect(await screen.findByText(/Scrape target metadata is unavailable/)).toBeTruthy();
    expect(screen.queryByText("http://secondary/metrics")).toBeNull();
    expect(screen.queryByText(/No active targets/)).toBeNull();
    expect(vi.mocked(MetricsApi.targets).mock.calls.filter(([id]) => id === 7)).toHaveLength(1);
  });

  it("discards a late Targets response from the previous datasource", async () => {
    const first = deferred<any>();
    const second = deferred<any>();
    vi.mocked(MetricsApi.listDatasources).mockResolvedValue([datasource, secondDatasource] as any);
    vi.mocked(MetricsApi.targets).mockImplementation((id) => (
      id === datasource.id ? first.promise : second.promise
    ));
    render(
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <Targets />
      </AppContext.Provider>,
    );

    await waitFor(() => expect(MetricsApi.targets).toHaveBeenCalledWith(7));
    fireEvent.change(screen.getByLabelText("Datasource"), { target: { value: "8" } });
    await waitFor(() => expect(MetricsApi.targets).toHaveBeenCalledWith(8));
    second.resolve({ data: { activeTargets: [{ scrapeUrl: "http://secondary/metrics" }] } });
    expect(await screen.findByText("http://secondary/metrics")).toBeTruthy();
    first.resolve({ data: { activeTargets: [{ scrapeUrl: "http://stale/metrics" }] } });
    await waitFor(() => expect(screen.queryByText("http://stale/metrics")).toBeNull());
    expect(screen.getByText("http://secondary/metrics")).toBeTruthy();
  });

  it("isolates Explorer metadata, query results, and history by datasource and cluster", async () => {
    const firstLabels = deferred<any>();
    const secondLabels = deferred<any>();
    const firstQuery = deferred<any>();
    const secondQuery = deferred<any>();
    vi.mocked(MetricsApi.listDatasources).mockResolvedValue([datasource, secondDatasource] as any);
    vi.mocked(MetricsApi.labels).mockImplementation((id) => (
      id === datasource.id ? firstLabels.promise : secondLabels.promise
    ));
    vi.mocked(MetricsApi.queryRange).mockImplementation((id) => (
      id === datasource.id ? firstQuery.promise : secondQuery.promise
    ));
    render(
      <AppContext.Provider value={{
        cluster: { cluster_id: 31 },
        clusterName: "c1",
        loginName: "alice",
      } as any}>
        <Explorer />
      </AppContext.Provider>,
    );

    await waitFor(() => expect(MetricsApi.labels).toHaveBeenCalledWith(7));
    fireEvent.submit(screen.getByLabelText("PromQL").closest("form") as HTMLFormElement);
    await waitFor(() => expect(MetricsApi.queryRange).toHaveBeenCalledWith(
      7, "up", expect.any(Number), expect.any(Number), expect.any(Number),
    ));
    fireEvent.change(screen.getByLabelText("Datasource"), { target: { value: "8" } });
    await waitFor(() => expect(MetricsApi.labels).toHaveBeenCalledWith(8));
    secondLabels.resolve({ data: ["secondary_label"] });
    firstLabels.reject(unsupported);
    await waitFor(() => expect(screen.queryByText(/label discovery is unavailable/)).toBeNull());

    fireEvent.submit(screen.getByLabelText("PromQL").closest("form") as HTMLFormElement);
    await waitFor(() => expect(MetricsApi.queryRange).toHaveBeenCalledWith(
      8, "up", expect.any(Number), expect.any(Number), expect.any(Number),
    ));
    secondQuery.resolve({
      status: "success",
      data: { result: [{ metric: { job: "secondary" }, values: [[1, "1"]] }] },
    });
    expect(await screen.findByText('job="secondary"')).toBeTruthy();
    firstQuery.resolve({
      status: "success",
      data: { result: [{ metric: { job: "stale" }, values: [[1, "1"]] }] },
    });
    await waitFor(() => expect(screen.queryByText('job="stale"')).toBeNull());
    expect(localStorage.getItem(
      'ambari-promql-history:["principal-cluster","alice","id:31"]',
    )).toBe(JSON.stringify(["up"]));
  });

  it("does not persist a late query after the scoped Explorer unmounts", async () => {
    const queryResult = deferred<any>();
    vi.mocked(MetricsApi.labels).mockResolvedValue({ data: [] } as any);
    vi.mocked(MetricsApi.queryRange).mockReturnValue(queryResult.promise);
    const rendered = render(
      <AppContext.Provider value={{
        cluster: { cluster_id: 31 },
        clusterName: "c1",
        loginName: "alice",
      } as any}>
        <Explorer />
      </AppContext.Provider>,
    );

    await screen.findByRole("button", { name: "Run query" });
    fireEvent.submit(screen.getByLabelText("PromQL").closest("form") as HTMLFormElement);
    await waitFor(() => expect(MetricsApi.queryRange).toHaveBeenCalledOnce());
    rendered.unmount();
    queryResult.resolve({ status: "success", data: { result: [] } });
    await Promise.resolve();
    expect(localStorage.getItem(
      'ambari-promql-history:["principal-cluster","alice","id:31"]',
    )).toBeNull();
  });

  it("does not persist query history without a verified principal and numeric cluster", async () => {
    vi.mocked(MetricsApi.labels).mockResolvedValue({ data: [] } as any);
    render(
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <Explorer />
      </AppContext.Provider>,
    );

    await screen.findByRole("button", { name: "Run query" });
    fireEvent.submit(screen.getByLabelText("PromQL").closest("form") as HTMLFormElement);
    await waitFor(() => expect(MetricsApi.queryRange).toHaveBeenCalledOnce());
    expect(localStorage.length).toBe(0);
  });
});
