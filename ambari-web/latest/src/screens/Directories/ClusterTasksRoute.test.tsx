/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

afterEach(cleanup);

const mocks = vi.hoisted(() => ({
  canViewClusterTasks: vi.fn(),
  receivedRequestId: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ canViewClusterTasks: mocks.canViewClusterTasks }),
}));
vi.mock("../BackgroundOperations", () => ({
  default: ({ onClose, requestId }: { onClose: () => void; requestId?: number }) => {
    mocks.receivedRequestId(requestId);
    return <button onClick={onClose} type="button">Close tasks</button>;
  },
}));

import ClusterTasksRoute from "./ClusterTasksRoute";

function renderRoute(initialEntry: string | { pathname: string; state?: unknown }) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/clusters/:clusterName/main/requests" element={<ClusterTasksRoute />} />
        <Route path="/clusters/:clusterName/main/dashboard/metrics" element={<div>Overview</div>} />
        <Route path="/services" element={<div>Service directory</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("cluster task route", () => {
  beforeEach(() => {
    mocks.canViewClusterTasks.mockReturnValue(true);
  });

  it("returns to an explicit safe directory location", () => {
    renderRoute({
      pathname: "/clusters/alpha/main/requests",
      state: { returnTo: "/services?q=hdfs&page=2" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Close tasks" }));
    expect(screen.getByText("Service directory")).toBeTruthy();
  });

  it("returns a direct deep link to the scoped overview", () => {
    renderRoute("/clusters/alpha/main/requests");

    fireEvent.click(screen.getByRole("button", { name: "Close tasks" }));
    expect(screen.getByText("Overview")).toBeTruthy();
  });

  it("redirects a role without scoped task access", () => {
    mocks.canViewClusterTasks.mockReturnValue(false);
    renderRoute("/clusters/alpha/main/requests");

    expect(screen.getByText("Overview")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "Close tasks" })).toBeNull();
  });
  it("opens the exact request linked from Admin operation history", () => {
    renderRoute("/clusters/alpha/main/requests?requestId=42");
    expect(mocks.receivedRequestId).toHaveBeenLastCalledWith(42);
  });

});
