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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { HostsApi } from "../../api/hostsApi";
import ConfigsApi from "../../api/configsApi";
import componentApi from "../../api/componentApi";
import { AppContext } from "../../store/context";
import ServiceAutoStart from ".";

vi.mock("../../api/hostsApi", () => ({
  HostsApi: { getClusterComponents: vi.fn() },
}));
vi.mock("../../api/configsApi", () => ({
  default: {
    updateConfigTags: vi.fn(),
    getConfigsByTags: vi.fn(),
  },
}));
vi.mock("../../api/componentApi", () => ({
  default: { editComponent: vi.fn() },
}));
vi.mock("../../Utils/clusterConfigUtils", () => ({
  safeUpdateClusterEnvConfig: vi.fn(),
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({
    hasAuthorization: (permission: string) => permission === "CLUSTER.MANAGE_AUTO_START",
  }),
}));

const contextValue = {
  clusterName: "c1",
  isNonWizardUser: false,
  upgradeIsRunning: false,
  upgradeSuspended: false,
  supports: { opsDuringRollingUpgrade: false },
} as unknown as ComponentProps<typeof AppContext.Provider>["value"];

const nativeRequest = globalThis.Request;

class RouterTestRequest {
  url: string;
  method: string;
  signal: AbortSignal | null;
  headers: Headers;

  constructor(input: string | URL, init: RequestInit = {}) {
    this.url = String(input);
    this.method = init.method || "GET";
    this.signal = init.signal || null;
    this.headers = new Headers(init.headers);
  }
}

describe("ServiceAutoStart navigation recovery", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(globalThis, "Request", {
      configurable: true,
      value: RouterTestRequest,
    });
    let serverRecoveryEnabled = "false";
    vi.mocked(HostsApi.getClusterComponents).mockImplementation(async () => ({
      items: [{
        ServiceComponentInfo: {
          category: "SLAVE",
          component_name: "DATANODE",
          recovery_enabled: serverRecoveryEnabled,
          service_name: "HDFS",
          total_count: 1,
        },
      }],
    }) as never);
    vi.mocked(ConfigsApi.updateConfigTags).mockResolvedValue([
      { siteName: "cluster-env", tagName: "version1" },
    ] as never);
    vi.mocked(ConfigsApi.getConfigsByTags).mockResolvedValue({
      items: [{ properties: { recovery_enabled: "false" } }],
    } as never);
    vi.mocked(componentApi.editComponent)
      .mockRejectedValueOnce(new Error("component update rejected"))
      .mockImplementationOnce(async () => {
        serverRecoveryEnabled = "true";
        return {};
      });
  });

  afterEach(() => {
    cleanup();
    Object.defineProperty(globalThis, "Request", {
      configurable: true,
      value: nativeRequest,
    });
  });

  it("blocks navigation, preserves a partial-save error, and retries remaining changes", async () => {
    const router = createMemoryRouter([
      {
        path: "/auto",
        element: (
          <AppContext.Provider value={contextValue}>
            <ServiceAutoStart />
          </AppContext.Provider>
        ),
      },
      { path: "/other", element: <div>Other page</div> },
    ], { initialEntries: ["/auto"] });
    render(<RouterProvider router={router} />);

    const componentToggle = await waitFor(() => {
      const element = document.getElementById("autostart-HDFS-DATANODE");
      expect(element).toBeTruthy();
      return element as HTMLInputElement;
    });
    fireEvent.click(componentToggle);

    await router.navigate("/other");
    expect(await screen.findByText("You have unsaved changes.")).toBeTruthy();
    expect(router.state.location.pathname).toBe("/auto");

    fireEvent.click(screen.getByTestId("confirm-ok-btn"));
    expect(await screen.findByText(/1 auto-start update request\(s\) failed/)).toBeTruthy();
    expect(router.state.location.pathname).toBe("/auto");

    fireEvent.click(screen.getByTestId("confirm-ok-btn"));
    expect(await screen.findByText("Other page")).toBeTruthy();
    expect(componentApi.editComponent).toHaveBeenCalledTimes(2);
  });
});
