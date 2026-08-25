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

import { useContext } from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext, AppProvider } from "./context";

const mocks = vi.hoisted(() => ({
  activateClient: vi.fn(),
  deactivateClient: vi.fn(),
  getClusterData: vi.fn(),
  getHosts: vi.fn(),
  getPersistData: vi.fn(),
  getRequests: vi.fn(),
  getServices: vi.fn(),
  getUpgradeState: vi.fn(),
  loadAmbariProperties: vi.fn(),
  servicesList: vi.fn(),
}));

vi.mock("@stomp/stompjs", () => ({
  Client: class {
    activate = mocks.activateClient;
    deactivate = mocks.deactivateClient;
  },
}));

vi.mock("../hooks/useAuth", () => ({
  default: () => ({
    authorizations: [{ authorization_id: "VIEW.USE" }],
    user: { user_name: "view-user" },
  }),
}));

vi.mock("../api/clusterApi", () => ({
  default: {
    getClusterData: mocks.getClusterData,
    getHosts: mocks.getHosts,
    getPersistData: mocks.getPersistData,
    getRequests: mocks.getRequests,
    getUpgradeState: mocks.getUpgradeState,
    loadAmbariProperties: mocks.loadAmbariProperties,
    postPersistData: vi.fn(),
  },
}));

vi.mock("../api/chooseServicesApi", () => ({
  ChooseServicesApi: { servicesList: mocks.servicesList },
}));

vi.mock("../api/servicesApi", () => ({
  ServicesApi: { getServices: mocks.getServices },
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
}

function ContextProbe() {
  const {
    clusterName,
    initializationError,
    isAppLoaded,
    retryInitialization,
    serverClock,
  } = useContext(AppContext);

  return (
    <div>
      <span data-testid="app-loaded">{String(isAppLoaded)}</span>
      <span data-testid="cluster-name">{clusterName}</span>
      <span data-testid="initialization-error">{initializationError}</span>
      <span data-testid="server-clock">{String(serverClock)}</span>
      <button type="button" onClick={retryInitialization}>Retry initialization</button>
    </div>
  );
}

function installedClusterResponse() {
  return {
    items: [{
      Clusters: {
        cluster_id: 1,
        cluster_name: "view-cluster",
        provisioning_state: "INSTALLED",
        security_type: "NONE",
        version: "HDP-3.1",
      },
    }],
  };
}

describe("View-only application initialization", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getPersistData.mockResolvedValue({});
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("loads cluster identity before ready without starting operational models", async () => {
    const ambariProperties = deferred<Record<string, unknown>>();
    const clusterData = deferred<ReturnType<typeof installedClusterResponse>>();
    mocks.loadAmbariProperties.mockReturnValue(ambariProperties.promise);
    mocks.getClusterData.mockReturnValue(clusterData.promise);

    render(<AppProvider><ContextProbe /></AppProvider>);

    expect(screen.getByTestId("app-loaded").textContent).toBe("false");
    await waitFor(() => expect(mocks.loadAmbariProperties).toHaveBeenCalledOnce());
    expect(mocks.getClusterData).not.toHaveBeenCalled();

    await act(async () => ambariProperties.resolve({
      RootServiceComponents: {
        component_version: "3.0.0",
        properties: {},
        server_clock: 1_710_000_000_123,
      },
    }));
    await waitFor(() => expect(mocks.getClusterData).toHaveBeenCalledOnce());
    expect(screen.getByTestId("app-loaded").textContent).toBe("false");

    await act(async () => clusterData.resolve(installedClusterResponse()));
    await waitFor(() => expect(screen.getByTestId("app-loaded").textContent).toBe("true"));
    expect(screen.getByTestId("cluster-name").textContent).toBe("view-cluster");
    expect(screen.getByTestId("server-clock").textContent)
      .toBe("1710000000123");
    expect(mocks.servicesList).not.toHaveBeenCalled();
    expect(mocks.getHosts).not.toHaveBeenCalled();
    expect(mocks.getUpgradeState).not.toHaveBeenCalled();
    expect(mocks.getRequests).not.toHaveBeenCalled();
    expect(mocks.getServices).not.toHaveBeenCalled();
    expect(mocks.activateClient).not.toHaveBeenCalled();
    expect(mocks.getPersistData).not.toHaveBeenCalledWith("wizard-data");
    expect(mocks.getPersistData.mock.calls.some(([key]) => key === undefined)).toBe(false);
    expect(mocks.getPersistData).toHaveBeenCalledTimes(1);
  });

  it("surfaces cluster identity failure and retries the complete sequence", async () => {
    mocks.loadAmbariProperties.mockResolvedValue({
      RootServiceComponents: { component_version: "3.0.0", properties: {} },
    });
    mocks.getClusterData
      .mockRejectedValueOnce({ response: { data: { message: "Cluster identity unavailable" } } })
      .mockResolvedValueOnce(installedClusterResponse());

    render(<AppProvider><ContextProbe /></AppProvider>);

    await waitFor(() => expect(
      screen.getByTestId("initialization-error").textContent,
    ).toBe("Cluster identity unavailable"));
    expect(screen.getByTestId("app-loaded").textContent).toBe("false");

    fireEvent.click(screen.getByRole("button", { name: "Retry initialization" }));
    await waitFor(() => expect(mocks.getClusterData).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.getByTestId("app-loaded").textContent).toBe("true"));
    expect(screen.getByTestId("cluster-name").textContent).toBe("view-cluster");
  });
});
