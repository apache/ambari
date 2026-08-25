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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AppContext } from "../../../../store/context";

const mocks = vi.hoisted(() => ({
  hasAuthorization: vi.fn(),
  loadRangerAdminComponent: vi.fn(),
}));

vi.mock("../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));
vi.mock("./rangerAdminHaApi", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./rangerAdminHaApi")>()),
  rangerAdminEnablementApi: {
    loadRangerAdminComponent: mocks.loadRangerAdminComponent,
  },
}));
vi.mock("./ValidateEnablement", () => ({
  default: () => <div>Validation flow</div>,
}));

import EnableHighAvailibilityRangerAdmin from "./index";

function componentResponse(states: string[]) {
  return {
    host_components: states.map((state, index) => ({
      HostRoles: {
        host_name: `ra-${index + 1}.example.com`,
        state,
      },
    })),
  };
}

function renderAction({
  allHostNames = ["host-1", "host-2", "host-3"],
  clusterName = "c1",
}: {
  allHostNames?: string[];
  clusterName?: string;
} = {}) {
  return render(
    <MemoryRouter initialEntries={["/main/services/RANGER/summary"]}>
      <AppContext.Provider value={{ clusterName, allHostNames } as never}>
        <EnableHighAvailibilityRangerAdmin />
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

function getAction() {
  return screen.getByText("Enable Ranger Admin HA");
}

function expectDisabled(action: HTMLElement) {
  expect(action.classList.contains("disabled")).toBe(true);
}

function expectEnabled(action: HTMLElement) {
  expect(action.classList.contains("disabled")).toBe(false);
}

describe("Ranger Admin HA service action", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAuthorization.mockReturnValue(true);
  });
  afterEach(() => cleanup());

  it.each(["SERVICE.ENABLE_HA", "CLUSTER.MANAGE_USER_PERSISTED_DATA"])(
    "hides the action without %s permission",
    (missingPermission) => {
      mocks.hasAuthorization.mockImplementation(
        (permission: string) => permission !== missingPermission,
      );

      renderAction();

      expect(screen.queryByText("Enable Ranger Admin HA")).toBeNull();
      expect(mocks.loadRangerAdminComponent).not.toHaveBeenCalled();
    },
  );

  it("keeps the action disabled while component state is loading", () => {
    mocks.loadRangerAdminComponent.mockReturnValue(new Promise(() => {}));

    renderAction();

    expectDisabled(getAction());
  });

  it("keeps the action disabled when component loading fails", async () => {
    mocks.loadRangerAdminComponent.mockRejectedValue(
      new Error("component lookup failed"),
    );

    renderAction();

    await waitFor(() =>
      expect(mocks.loadRangerAdminComponent).toHaveBeenCalledOnce(),
    );
    expectDisabled(getAction());
  });

  it("hides enablement when multiple Ranger Admins already exist", async () => {
    mocks.loadRangerAdminComponent.mockResolvedValue(
      componentResponse(["STARTED", "STARTED"]),
    );
    renderAction();

    await waitFor(() =>
      expect(screen.queryByText("Enable Ranger Admin HA")).toBeNull(),
    );
  });

  it.each([
    {
      name: "the cluster has one host",
      allHostNames: ["host-1"],
      response: componentResponse(["STARTED"]),
    },
    {
      name: "Ranger Admin is missing",
      allHostNames: ["host-1", "host-2"],
      response: componentResponse([]),
    },
    {
      name: "Ranger Admin is in INIT state",
      allHostNames: ["host-1", "host-2"],
      response: componentResponse(["INIT"]),
    },
    {
      name: "Ranger Admin installation failed",
      allHostNames: ["host-1", "host-2"],
      response: componentResponse(["INSTALL_FAILED"]),
    },
  ])("disables the action when $name", async ({ allHostNames, response }) => {
    mocks.loadRangerAdminComponent.mockResolvedValue(response);
    renderAction({ allHostNames });

    await waitFor(() =>
      expect(mocks.loadRangerAdminComponent).toHaveBeenCalledWith("c1"),
    );
    expectDisabled(getAction());
  });

  it("enables the action for other single-instance states", async () => {
    mocks.loadRangerAdminComponent.mockResolvedValue(
      componentResponse(["STOPPED"]),
    );
    renderAction();

    await waitFor(() => expectEnabled(getAction()));
  });
});
