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

import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import MonitoringLayout from "./MonitoringLayout";

const mocks = vi.hoisted(() => ({
  hasAuthorization: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));

const renderLayout = () => render(
  <MemoryRouter initialEntries={["/main/monitoring/dashboards"]}>
    <Routes>
      <Route path="/main/monitoring" element={<MonitoringLayout />}>
        <Route path="*" element={<div>Monitoring page</div>} />
      </Route>
    </Routes>
  </MemoryRouter>,
);

describe("Monitoring navigation permissions", () => {
  afterEach(cleanup);

  beforeEach(() => {
    mocks.hasAuthorization.mockReset();
  });

  it("shows cluster metric pages but not Targets with cluster-only access", () => {
    mocks.hasAuthorization.mockImplementation(
      (authorization) => authorization === "CLUSTER.VIEW_METRICS",
    );

    renderLayout();

    expect(screen.getByRole("link", { name: "Dashboards" })).toBeTruthy();
    expect(screen.getByRole("link", { name: "Explore" })).toBeTruthy();
    expect(screen.getByRole("link", { name: "Datasources" })).toBeTruthy();
    expect(screen.queryByRole("link", { name: "Targets" })).toBeNull();
  });

  it("shows only Targets with host-only metrics access", () => {
    mocks.hasAuthorization.mockImplementation(
      (authorization) => authorization === "HOST.VIEW_METRICS",
    );

    renderLayout();

    expect(screen.getByRole("link", { name: "Targets" })).toBeTruthy();
    expect(screen.queryByRole("link", { name: "Dashboards" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Explore" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Datasources" })).toBeNull();
  });
});
