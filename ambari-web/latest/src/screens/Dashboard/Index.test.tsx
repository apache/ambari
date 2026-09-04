/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file distributed with
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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Dashboard from "./Index";

const mocks = vi.hoisted(() => ({
  hasAuthorization: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));

vi.mock("../Monitoring/EmbeddedDashboards", () => ({
  default: () => <div>Embedded dashboards</div>,
}));

vi.mock("./ConfigHistory", () => ({
  default: () => <div>Config history</div>,
}));

const renderDashboard = (path: string) => render(
  <MemoryRouter initialEntries={[path]}>
    <Routes>
      <Route path="/main/dashboard/:tabName" element={<Dashboard />} />
    </Routes>
  </MemoryRouter>,
);

describe("Dashboard metrics authorization", () => {
  afterEach(cleanup);

  beforeEach(() => {
    mocks.hasAuthorization.mockReset();
  });

  it("shows the Prometheus metrics tab with CLUSTER.VIEW_METRICS", () => {
    mocks.hasAuthorization.mockImplementation(
      (authorization) => authorization === "CLUSTER.VIEW_METRICS",
    );

    renderDashboard("/main/dashboard/metrics");

    expect(screen.getByRole("tab", { name: "METRICS" })).toBeTruthy();
    expect(screen.getByText("Embedded dashboards")).toBeTruthy();
  });

  it("hides metrics and converges direct access to config history", async () => {
    mocks.hasAuthorization.mockReturnValue(false);

    renderDashboard("/main/dashboard/metrics");

    await waitFor(() => {
      expect(screen.queryByRole("tab", { name: "METRICS" })).toBeNull();
      expect(screen.getByText("Config history")).toBeTruthy();
    });
  });
});
