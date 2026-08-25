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

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import {
  MemoryRouter,
  Route,
  Routes,
  useLocation,
} from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";

const mocks = vi.hoisted(() => ({
  getUpgradeHistory: vi.fn(),
  stackVersion: { items: [] } as unknown,
  stackVersionList: [] as unknown[],
}));

vi.mock("../../../api/versionsApi", () => ({
  default: { getUpgradeHistory: mocks.getUpgradeHistory },
}));
vi.mock("../../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: () => true }),
}));
vi.mock("../../../hooks/useStackVersion", () => ({
  default: () => ({
    stackVersion: mocks.stackVersion,
    stackVersionList: mocks.stackVersionList,
  }),
}));
vi.mock("./ListStack", () => ({ default: () => <div>Stack content</div> }));
vi.mock("./ListVersion", () => ({ default: () => <div>Version content</div> }));
vi.mock("./UpgradeHistory", () => ({ default: () => null }));
vi.mock("./Upgrade", () => ({ default: () => null }));

import StackAndVersions from "./StackAndVersions";

function CurrentPath() {
  return <div data-testid="current-path">{useLocation().pathname}</div>;
}

function renderPage(path: string) {
  return render(
    <AppContext.Provider value={{ clusterName: "c1", upgradeId: 0 } as never}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            path="/main/admin/stack/:tabName"
            element={<><StackAndVersions /><CurrentPath /></>}
          />
        </Routes>
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

describe("Admin stack version availability", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getUpgradeHistory.mockResolvedValue({ items: [] });
    mocks.stackVersion = { items: [] };
    mocks.stackVersionList = [];
  });

  afterEach(cleanup);

  it("hides Versions and redirects its deep link when no stack versions exist", async () => {
    renderPage("/main/admin/stack/versions");

    await waitFor(() => {
      expect(screen.getByTestId("current-path").textContent)
        .toBe("/main/admin/stack/services");
    });
    expect(screen.queryByRole("tab", { name: "Versions" })).toBeNull();
  });

  it("keeps Versions available when the stack version model is non-empty", () => {
    mocks.stackVersion = { items: [{}] };
    mocks.stackVersionList = [{}];

    renderPage("/main/admin/stack/services");

    expect(screen.getByRole("tab", { name: "Versions" })).toBeTruthy();
  });

  it("does not hide Versions before the initial stack-version load resolves", () => {
    mocks.stackVersion = undefined;

    renderPage("/main/admin/stack/versions");

    expect(screen.getByTestId("current-path").textContent)
      .toBe("/main/admin/stack/versions");
    expect(screen.getByRole("tab", { name: "Versions" })).toBeTruthy();
  });
});
