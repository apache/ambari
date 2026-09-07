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
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";
import type { Dashboard } from "../types";
import { DASHBOARD_SCHEMA_VERSION } from "../types";

const mocks = vi.hoisted(() => ({
  cloneDashboard: vi.fn(),
  getDashboard: vi.fn(),
  listDatasources: vi.fn(),
  updateDashboard: vi.fn(),
  updateDashboardConfigs: vi.fn(),
}));

vi.mock("../../../api/metricsApi", () => ({
  default: mocks,
}));

vi.mock("../../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: () => true }),
}));

vi.mock("./DashboardPanel", () => ({
  default: ({ panel }: { panel: { name: string } }) => <div>{panel.name}</div>,
}));

import DashboardPage from "./DashboardPage";

const payload = {
  version: DASHBOARD_SCHEMA_VERSION,
  var: [],
  panels: [{
    id: "cpu",
    name: "CPU usage",
    titleKey: "monitoring.dashboard.sections.cpu",
    type: "timeseries",
    datasourceCate: "prometheus",
    datasourceValue: 7,
    targets: [{ refId: "A", expr: "up" }],
    layout: { h: 5, w: 12, x: 0, y: 0, i: "cpu", isResizable: true },
  }],
};

const dashboard = (values: Partial<Dashboard>): Dashboard => ({
  id: 42,
  group_id: 0,
  name: "Linux Fleet Overview",
  ident: "LINUX_FLEET_OVERVIEW",
  tags: "linux",
  public: 1,
  built_in: 1,
  hide: 0,
  create_at: 1,
  create_by: "ambari",
  update_at: 1,
  update_by: "ambari",
  public_cate: 0,
  display_locations: "",
  configs: JSON.stringify(payload),
  ...values,
});

describe("DashboardPage built-in customization", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const copied = dashboard({
      id: 43,
      name: "Linux Fleet Overview Copy",
      ident: "LINUX_FLEET_OVERVIEW-copy-12345678",
      built_in: 0,
    });
    mocks.getDashboard.mockImplementation((_clusterName, id) => Promise.resolve(
      String(id).includes("copy") ? copied : dashboard({}),
    ));
    mocks.listDatasources.mockResolvedValue([]);
    mocks.cloneDashboard.mockResolvedValue(copied);
    mocks.updateDashboard.mockResolvedValue(copied);
    mocks.updateDashboardConfigs.mockResolvedValue(copied);
  });

  afterEach(cleanup);

  it("does not create a copy until the edited panel is saved", async () => {
    const context = { clusterName: "west" } as unknown as ComponentProps<
      typeof AppContext.Provider
    >["value"];
    render(
      <AppContext.Provider value={context}>
        <MemoryRouter initialEntries={["/main/monitoring/dashboards/LINUX_FLEET_OVERVIEW"]}>
          <Routes>
            <Route path="/main/monitoring/dashboards/:dashboardId" element={<DashboardPage />} />
          </Routes>
        </MemoryRouter>
      </AppContext.Provider>,
    );

    fireEvent.click(await screen.findByRole("button", { name: "Customize charts" }));
    expect(mocks.cloneDashboard).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTitle("Edit panel"));
    fireEvent.change(await screen.findByLabelText("Panel name"), { target: { value: "Edited CPU usage" } });
    fireEvent.click(screen.getByRole("button", { name: "Apply panel" }));
    fireEvent.click(screen.getByRole("button", { name: "Save as copy" }));

    await waitFor(() => expect(mocks.cloneDashboard).toHaveBeenCalledWith("west", 42));
    expect(mocks.updateDashboardConfigs).toHaveBeenCalledWith(
      "west",
      43,
      expect.stringContaining('"name":"Edited CPU usage"'),
    );
    expect(mocks.updateDashboardConfigs.mock.calls[0][2]).not.toContain("titleKey");
  });
});
