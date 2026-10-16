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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import type { ViewInstance } from "../../Utils/viewUtils";
import "../../i18n";

const authorizedView = {
  instanceName: "tez-jobs",
  label: "Tez Jobs",
} as unknown as ViewInstance;

vi.mock("../Views/ViewInstancesContext", () => ({
  useViewInstances: () => ({ instances: [authorizedView] }),
}));
const auth = vi.hoisted(() => ({ admin: true }));
vi.mock("../../hooks/useAuth", () => ({ useAuth: () => ({ isAdmin: () => auth.admin, user: { user_name: "admin" } }) }));
vi.mock("../../components/Navbar", () => ({
  default: ({ homePath, viewsList }: { homePath: string; viewsList: ViewInstance[] }) => (
    <div data-testid="directory-navbar">
      {homePath}:{viewsList.map((view) => view.label).join(",")}
    </div>
  ),
}));

import GlobalDirectoryLayout from "./GlobalDirectoryLayout";
import { rememberWorkspace } from "../../Utils/workspaceNavigation";

describe("global directory layout", () => {
  it("keeps the prior cluster page available across global directories", async () => {
    rememberWorkspace("admin", "/clusters/reference/main/services/DORIS/configs?version=6");
    render(<MemoryRouter initialEntries={["/clusters"]}><Routes><Route element={<GlobalDirectoryLayout />}>
      <Route path="clusters" element={<div>Cluster directory</div>} />
      <Route path="mpacks" element={<div>Pack directory</div>} />
    </Route></Routes></MemoryRouter>);
    expect(screen.getByRole("link", { name: /Return to workspace/ }).getAttribute("href")).toBe("/clusters/reference/main/services/DORIS/configs?version=6");
    fireEvent.click(screen.getByRole("link", { name: "Management Packs" }));
    await waitFor(() => expect(screen.getByText("Pack directory")).toBeTruthy());
    expect(screen.getByRole("link", { name: /Return to workspace/ }).getAttribute("href")).toContain("DORIS/configs?version=6");
  });
  it("exposes management packs to administrators before a cluster exists", () => {
    auth.admin = true;
    const { unmount } = render(<MemoryRouter><GlobalDirectoryLayout /></MemoryRouter>);
    expect(screen.getByRole("link", { name: "Management Packs" }).getAttribute("href")).toBe("/mpacks");
    unmount();
    auth.admin = false;
    const rendered = render(<MemoryRouter><GlobalDirectoryLayout /></MemoryRouter>);
    expect(screen.queryByRole("link", { name: "Management Packs" })).toBeNull();
    rendered.unmount();
    auth.admin = true;
  });
  it("passes authorized Views to the global navbar and keeps the directory home", () => {
    render(
      <MemoryRouter initialEntries={["/clusters"]}>
        <Routes>
          <Route element={<GlobalDirectoryLayout />}>
            <Route path="clusters" element={<div>Clusters</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByTestId("directory-navbar").textContent).toBe("/clusters:Tez Jobs");
  });
});
