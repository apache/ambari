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
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";

const api = vi.hoisted(() => ({
  fetchConfigHistory: vi.fn(),
  fetchSuggestions: vi.fn(),
  fetchTotal: vi.fn(),
}));

vi.mock("../../api/configHistoryApi", () => ({ default: api }));
vi.mock("../../components/Spinner", () => ({ default: () => <div>Loading history</div> }));
vi.mock("../../components/Paginator", () => ({ default: () => <div>Pagination</div> }));
vi.mock("./ConfigHistoryFilterBar", () => ({ default: () => <div>Filters</div> }));
type HistoryRow = {
  serviceConfigVersion: number | string;
  serviceName: string;
  [key: string]: unknown;
};
type HistoryColumn = {
  id?: string;
  cell?: (info: { row: { original: HistoryRow } }) => React.ReactNode;
};
vi.mock("../../components/Table", () => ({
  default: ({ columns, data }: { columns: HistoryColumn[]; data: HistoryRow[] }) => (
    <div>
      {data.map((item, rowIndex) => (
        <div key={`${item.serviceName}-${item.serviceConfigVersion}`}>
          {columns.map((column, columnIndex) => (
            <span key={`${column.id || columnIndex}-${rowIndex}`}>
              {typeof column.cell === "function"
                ? column.cell({ row: { original: item } })
                : null}
            </span>
          ))}
        </div>
      ))}
    </div>
  ),
}));

import DashboardConfigHistory from "./ConfigHistory";

const historyItem = {
  service_config_version: 12,
  user: "admin",
  group_id: 4,
  group_name: "workers",
  is_current: false,
  createtime: Date.UTC(2025, 0, 1, 0, 0),
  service_name: "HDFS",
  hosts: ["host1", "host2"],
  service_config_version_note: "A configuration note",
  is_cluster_compatible: false,
  stack_id: "HDP-3.1",
};

function Target() {
  const location = useLocation();
  return <div>Target {JSON.stringify(location.state)}</div>;
}

function renderHistory(parsedSocketMessages: Record<string, unknown>[] = []) {
  const context = {
    clusterName: "c1",
    parsedSocketMessages,
    services: [{ ServiceInfo: { service_name: "HDFS" } }],
    userTimezone: "Asia/Shanghai",
  } as unknown as React.ContextType<typeof AppContext>;
  return render(
    <AppContext.Provider value={context}>
      <MemoryRouter initialEntries={["/history"]}>
        <Routes>
          <Route path="/history" element={<DashboardConfigHistory />} />
          <Route path="/main/services/:service/configs" element={<Target />} />
        </Routes>
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

describe("Config History", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.fetchTotal.mockResolvedValue({ itemTotal: 20 });
    api.fetchSuggestions.mockResolvedValue([]);
    api.fetchConfigHistory.mockResolvedValue({ items: [historyItem], itemTotal: 1 });
  });

  afterEach(() => cleanup());

  it("shows a recoverable error and retries the snapshot", async () => {
    api.fetchConfigHistory
      .mockRejectedValueOnce(new Error("unavailable"))
      .mockResolvedValueOnce({ items: [historyItem], itemTotal: 1 });
    renderHistory();

    expect(await screen.findByText("Ambari could not load configuration history.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByRole("button", { name: /HDFS/ })).toBeTruthy();
    expect(api.fetchConfigHistory).toHaveBeenCalledTimes(2);
  });

  it("renders association fields in the user timezone and carries selection state", async () => {
    renderHistory();

    expect(await screen.findByText(/08:00/)).toBeTruthy();
    expect(screen.getByText("host1, host2")).toBeTruthy();
    expect(screen.getByText("No")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: /HDFS/ }));

    const target = await screen.findByText(/Target/);
    expect(target.textContent).toContain('"serviceConfigVersion":"12"');
    expect(target.textContent).toContain('"configGroup":"workers"');
    expect(target.textContent).toContain('"configGroupId":4');
  });

  it("renders config versions whose note is null", async () => {
    api.fetchConfigHistory.mockResolvedValue({
      items: [{ ...historyItem, service_config_version_note: null }],
      itemTotal: 1,
    });

    renderHistory();

    expect(await screen.findByRole("button", { name: /HDFS/ })).toBeTruthy();
    expect(screen.queryByRole("button", { name: ">> More" })).toBeNull();
  });

  it("refreshes the page and total when a config event arrives", async () => {
    const rendered = renderHistory();
    await waitFor(() => expect(api.fetchConfigHistory).toHaveBeenCalledTimes(1));

    rendered.rerender(
      <AppContext.Provider value={{
        clusterName: "c1",
        parsedSocketMessages: [{ destination: "/events/configs", type: "UPDATE" }],
        services: [{ ServiceInfo: { service_name: "HDFS" } }],
        userTimezone: "Asia/Shanghai",
      } as unknown as React.ContextType<typeof AppContext>}>
        <MemoryRouter initialEntries={["/history"]}>
          <Routes>
            <Route path="/history" element={<DashboardConfigHistory />} />
          </Routes>
        </MemoryRouter>
      </AppContext.Provider>,
    );

    await waitFor(() => expect(api.fetchConfigHistory).toHaveBeenCalledTimes(2));
    expect(api.fetchTotal).toHaveBeenCalledTimes(2);
  });
});
