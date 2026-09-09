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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  canAccessCluster: vi.fn(),
  canViewClusterTasks: vi.fn(),
  hasClusterAuthorization: vi.fn(),
  hasGlobalAuthorization: vi.fn(),
  getCreationDrafts: vi.fn(),
}));

vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => mocks,
}));
vi.mock("../../api/workflowStateApi", () => ({
  default: { getCreationDrafts: mocks.getCreationDrafts },
}));

import ClusterDirectory from "./ClusterDirectory";

const renderDirectory = () => render(
  <AppContext.Provider value={{
    availableClusters: [
      { Clusters: { cluster_id: 17, cluster_name: "alpha", provisioning_state: "INSTALLED", version: "3.0" } },
      { Clusters: { cluster_id: 29, cluster_name: "beta", provisioning_state: "INIT", version: "3.0" } },
    ],
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
    <MemoryRouter initialEntries={["/clusters?continue=%2Fmain%2Fhosts%3Fpage%3D2"]}>
      <ClusterDirectory />
    </MemoryRouter>
  </AppContext.Provider>,
);

describe("global cluster directory", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.canAccessCluster.mockReturnValue(true);
    mocks.canViewClusterTasks.mockReturnValue(true);
    mocks.hasClusterAuthorization.mockReturnValue(true);
    mocks.hasGlobalAuthorization.mockReturnValue(true);
    mocks.getCreationDrafts.mockResolvedValue([]);
  });
  afterEach(cleanup);

  it("preserves a safe legacy continuation and provides direct cluster actions", () => {
    renderDirectory();

    expect(screen.getByRole("link", { name: "alpha" }).getAttribute("href"))
      .toBe("/clusters/alpha/main/hosts?page=2");
    expect(screen.getByRole("link", { name: "Overview" }).getAttribute("href"))
      .toBe("/clusters/alpha/main/dashboard/metrics");
    expect(screen.getByRole("link", { name: "Tasks" }).getAttribute("href"))
      .toBe("/clusters/alpha/main/requests");
    expect(screen.queryByText("ID 17")).toBeNull();
    expect(screen.getByRole("combobox", { name: "Sort by" })).toBeTruthy();
  });

  it("starts an explicit UUID draft and does not guess recovery for incomplete clusters", async () => {
    renderDirectory();

    expect(screen.getByRole("link", { name: "Create Cluster" }).getAttribute("href"))
      .toMatch(/^\/installer\/step0\?draft=[0-9a-f-]{36}$/i);
    expect(screen.getByText("Installation is incomplete")).toBeTruthy();
    await waitFor(() => expect(mocks.getCreationDrafts).toHaveBeenCalledOnce());
    expect(screen.queryByRole("link", { name: "Resume" })).toBeNull();
  });

  it("shows only explicit owner-scoped creation drafts as resumable", async () => {
    mocks.getCreationDrafts.mockResolvedValue([{
      draft_id: "2e97ec6a-c03d-4c52-b910-1a58ae50f390",
      revision: 7,
      workflow: "CLUSTER_CREATE",
      phase: "CLUSTER_DEPLOY_PREP_2",
      cluster_id: 84,
      cluster_name: "recoverable",
    }]);
    renderDirectory();

    expect(await screen.findByRole("heading", { name: "Resume installations" })).toBeTruthy();
    expect(screen.getByText("recoverable")).toBeTruthy();
    expect(screen.getByRole("link", { name: "Resume" }).getAttribute("href"))
      .toBe("/installer/step0?draft=2e97ec6a-c03d-4c52-b910-1a58ae50f390");
    expect(screen.queryByText(/2e97ec6a/)).toBeNull();
  });

  it("hides global creation and task actions without their permissions", () => {
    mocks.hasGlobalAuthorization.mockReturnValue(false);
    mocks.canViewClusterTasks.mockReturnValue(false);
    renderDirectory();

    expect(screen.queryByRole("link", { name: "Create Cluster" })).toBeNull();
    expect(screen.queryByRole("link", { name: "Tasks" })).toBeNull();
    expect(mocks.getCreationDrafts).not.toHaveBeenCalled();
  });
});
