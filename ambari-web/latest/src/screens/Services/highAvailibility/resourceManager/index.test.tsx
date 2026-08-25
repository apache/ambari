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

import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AppContext } from "../../../../store/context";

const mocks = vi.hoisted(() => ({
  getClusterComponents: vi.fn(),
  hasAuthorization: vi.fn(),
}));

vi.mock("../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));
vi.mock("./rmHaApi", () => ({
  default: { getClusterComponents: mocks.getClusterComponents },
}));
vi.mock("./ValidateEnablement", () => ({
  default: () => <div>ResourceManager validation flow</div>,
}));

import EnableHighAvailibilityResourceManger from "./index";

type Deferred<T> = {
  promise: Promise<T>;
  resolve: (value: T) => void;
  reject: (reason: unknown) => void;
};

function deferred<T>(): Deferred<T> {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve;
    reject = promiseReject;
  });
  return { promise, resolve, reject };
}

function componentResponse(states: string[]) {
  return {
    items: [
      {
        ServiceComponentInfo: {
          component_name: "RESOURCEMANAGER",
          service_name: "YARN",
        },
        host_components: states.map((state, index) => ({
          HostRoles: {
            component_name: "RESOURCEMANAGER",
            service_name: "YARN",
            host_name: `rm-${index + 1}.example.com`,
            state,
          },
        })),
      },
    ],
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
    <MemoryRouter initialEntries={["/main/services/YARN/summary"]}>
      <AppContext.Provider value={{ clusterName, allHostNames } as never}>
        <EnableHighAvailibilityResourceManger />
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

function getAction() {
  return screen.getByText("Enable ResourceManager HA");
}

function expectDisabled(action: HTMLElement) {
  expect(action.classList.contains("disabled")).toBe(true);
}

function expectEnabled(action: HTMLElement) {
  expect(action.classList.contains("disabled")).toBe(false);
}

describe("ResourceManager HA service action", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAuthorization.mockReturnValue(true);
  });
  afterEach(() => cleanup());

  it.each([
    "SERVICE.ENABLE_HA",
    "CLUSTER.MANAGE_USER_PERSISTED_DATA",
  ])("hides the action without %s permission", (missingPermission) => {
    mocks.hasAuthorization.mockImplementation(
      (permission: string) => permission !== missingPermission,
    );

    renderAction();

    expect(screen.queryByText("Enable ResourceManager HA")).toBeNull();
    expect(mocks.getClusterComponents).not.toHaveBeenCalled();
  });

  it("keeps the action disabled while component state is loading", async () => {
    const request = deferred<ReturnType<typeof componentResponse>>();
    mocks.getClusterComponents.mockReturnValue(request.promise);
    renderAction();

    expectDisabled(getAction());

    await act(async () => request.resolve(componentResponse(["STOPPED"])));
    await waitFor(() => expectEnabled(getAction()));
  });

  it("keeps the action disabled when component loading fails", async () => {
    const request = deferred<ReturnType<typeof componentResponse>>();
    mocks.getClusterComponents.mockReturnValue(request.promise);
    renderAction();

    await act(async () => request.reject(new Error("request failed")));

    expectDisabled(getAction());
  });

  it("hides the action when ResourceManager HA is already enabled", async () => {
    mocks.getClusterComponents.mockResolvedValue(
      componentResponse(["STARTED", "STARTED"]),
    );
    renderAction();

    await waitFor(() =>
      expect(screen.queryByText("Enable ResourceManager HA")).toBeNull(),
    );
  });

  it.each([
    {
      name: "the cluster has one host",
      allHostNames: ["host-1"],
      response: componentResponse(["STARTED"]),
    },
    {
      name: "ResourceManager is missing",
      allHostNames: ["host-1", "host-2", "host-3"],
      response: { items: [] },
    },
    {
      name: "ResourceManager is in INIT state",
      allHostNames: ["host-1", "host-2", "host-3"],
      response: componentResponse(["INIT"]),
    },
    {
      name: "ResourceManager installation failed",
      allHostNames: ["host-1", "host-2", "host-3"],
      response: componentResponse(["INSTALL_FAILED"]),
    },
  ])("disables the action when $name", async ({ allHostNames, response }) => {
    mocks.getClusterComponents.mockResolvedValue(response);
    renderAction({ allHostNames });

    await waitFor(() =>
      expect(mocks.getClusterComponents).toHaveBeenCalledWith("c1"),
    );
    expectDisabled(getAction());
  });

  it("does not disable the action for a stopped ResourceManager", async () => {
    mocks.getClusterComponents.mockResolvedValue(
      componentResponse(["STOPPED"]),
    );
    renderAction();

    await waitFor(() => expectEnabled(getAction()));
  });

  it("navigates to the first wizard step when the enabled action is clicked", async () => {
    mocks.getClusterComponents.mockResolvedValue(
      componentResponse(["STARTED"]),
    );
    render(
      <MemoryRouter initialEntries={["/main/services/YARN/summary"]}>
        <AppContext.Provider
          value={
            {
              clusterName: "c1",
              allHostNames: ["host-1", "host-2", "host-3"],
            } as never
          }
        >
          <Routes>
            <Route
              path="/main/services/YARN/summary"
              element={<EnableHighAvailibilityResourceManger />}
            />
            <Route
              path="/main/services/highAvailability/ResourceManager/enable/step1"
              element={<div>ResourceManager wizard route</div>}
            />
          </Routes>
        </AppContext.Provider>
      </MemoryRouter>,
    );
    await waitFor(() => expectEnabled(getAction()));

    fireEvent.click(getAction());

    expect(await screen.findByText("ResourceManager wizard route")).toBeTruthy();
  });

  it("renders the validation flow for the mapped ResourceManager route", () => {
    render(
      <MemoryRouter
        initialEntries={[
          "/main/services/highAvailability/ResourceManager/enable/step1",
        ]}
      >
        <AppContext.Provider
          value={{ clusterName: "c1", allHostNames: [] } as never}
        >
          <Routes>
            <Route
              path="/main/services/highAvailability/:componentName/enable/step1"
              element={<EnableHighAvailibilityResourceManger isMappingOnly />}
            />
          </Routes>
        </AppContext.Provider>
      </MemoryRouter>,
    );

    expect(screen.getByText("ResourceManager validation flow")).toBeTruthy();
    expect(screen.queryByText("Enable ResourceManager HA")).toBeNull();
    expect(mocks.getClusterComponents).not.toHaveBeenCalled();
  });
});
