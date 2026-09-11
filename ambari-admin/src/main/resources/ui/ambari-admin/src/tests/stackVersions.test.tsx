/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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

import { act, cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AppContent from "../context/AppContext";
import StackVersionsList from "../screens/ClusterManagement/StackVersions/List";

const transport = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("../api/configs/axiosConfig", () => ({ adminApi: transport }));
vi.mock("../context/ManagementContext", () => ({ useManagement: () => ({ can: () => true }) }));
afterEach(cleanup);
beforeEach(() => vi.resetAllMocks());
const catalog = { items: [{ versions: [{ repository_versions: [{ RepositoryVersions: {
  id: 1, display_name: "Test repository", type: "STANDARD", stack_name: "BIGTOP",
  stack_version: "3.3.0", repository_version: "3.3.0", hidden: false,
} }] }] }] };
const status = (count: number) => ({ items: [{ ClusterStackVersions: {
  id: 10, state: "CURRENT", host_states: { CURRENT: Array.from({ length: count }, (_, i) => `host${i}`) },
} }] });
const setSelectedOption = vi.fn();
function page(name: string) {
  return <MemoryRouter><AppContent.Provider value={{ cluster: { cluster_name: name },
    availableClusters: [{ cluster_id: 1, cluster_name: "alpha" }, { cluster_id: 2, cluster_name: "east / prod" }], setSelectedOption }}>
    <StackVersionsList />
  </AppContent.Provider></MemoryRouter>;
}

describe("Admin repository cluster ownership", () => {
  it("renders status and encoded navigation for the selected cluster", async () => {
    transport.request.mockImplementation(async ({ url }) => {
      if (url.startsWith("/stacks?")) return { data: structuredClone(catalog) };
      if (url === "/clusters/east%20%2F%20prod/stack_versions?fields=*&ClusterStackVersions/repository_version=1") return { data: status(2) };
      throw new Error(`Unexpected request: ${url}`);
    });
    render(page("east / prod"));
    const link = await screen.findByRole("link", { name: "east / prod" });
    expect(link.getAttribute("href")).toBe("/latest/#/clusters/east%20%2F%20prod/main/admin/stack/versions");
    expect(screen.getByText("CURRENT: 2/2")).toBeTruthy();
    expect(screen.queryByRole("link", { name: "alpha" })).toBeNull();
  });

  it("loads the global repository catalog without inventing a current cluster", async () => {
    transport.request.mockImplementation(async ({ url }) => {
      if (url.startsWith("/stacks?")) return { data: structuredClone(catalog) };
      throw new Error(`Unexpected cluster request: ${url}`);
    });
    render(page(""));
    expect(await screen.findByRole("link", { name: "Test repository" })).toBeTruthy();
    expect(screen.getByText("Select a cluster to view installation status")).toBeTruthy();
    expect(screen.queryByText("INSTALL ON")).toBeNull();
  });

  it("ignores a delayed status response after switching clusters", async () => {
    let resolveAlpha!: (value: unknown) => void;
    const alphaPending = new Promise((resolve) => { resolveAlpha = resolve; });
    transport.request.mockImplementation(async ({ url }) => {
      if (url.startsWith("/stacks?")) return { data: structuredClone(catalog) };
      if (url.startsWith("/clusters/alpha/")) return alphaPending;
      return { data: status(2) };
    });
    const view = render(page("alpha"));
    await act(async () => {});
    view.rerender(page("east / prod"));
    await screen.findByRole("link", { name: "east / prod" });
    await act(async () => { resolveAlpha({ data: status(5) }); });
    expect(screen.queryByRole("link", { name: "alpha" })).toBeNull();
    expect(screen.getByText("CURRENT: 2/2")).toBeTruthy();
  });

  it("reports a failed cluster status load instead of showing a usable empty status", async () => {
    transport.request.mockImplementation(async ({ url }) => {
      if (url.startsWith("/stacks?")) return { data: structuredClone(catalog) };
      throw new Error("Status unavailable");
    });
    render(page("alpha"));
    expect(await screen.findByRole("alert")).toBeTruthy();
    expect(screen.queryByText("INSTALL ON")).toBeNull();
    expect(screen.getByRole("button", { name: "Refresh" })).toBeTruthy();
  });
});
