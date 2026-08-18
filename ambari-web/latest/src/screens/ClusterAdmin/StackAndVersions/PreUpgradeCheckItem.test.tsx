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

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import VersionsApi from "../../../api/versionsApi";
import useKDCSessionState from "../../../hooks/useKDCSessionState";
import { AppContext } from "../../../store/context";
import PreUpgradeCheckItem from "./PreUpgradeCheckItem";

vi.mock("../../../api/versionsApi", () => ({
  default: {
    reinstallFailedComponent: vi.fn(),
  },
}));
vi.mock("../../../hooks/useKDCSessionState");
vi.mock("../../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: () => true }),
}));

const contextValue = {
  clusterName: "c1",
  upgradeId: 0,
  upgradeSuspended: false,
  isNonWizardUser: false,
  setUpgradeState: vi.fn(),
} as unknown as ComponentProps<typeof AppContext.Provider>["value"];

function renderComponent(onRecheck = vi.fn().mockResolvedValue(undefined)) {
  render(
    <AppContext.Provider value={contextValue}>
      <MemoryRouter>
        <PreUpgradeCheckItem
          check={{
            id: "COMPONENTS_INSTALLATION",
            failed_detail: [{
              host_name: "host1",
              service_name: "HDFS",
              component_name: "DATANODE",
            }],
          }}
          repositoryVersionId={7}
          upgradeType="ROLLING"
          onRecheck={onRecheck}
        />
      </MemoryRouter>
    </AppContext.Provider>,
  );
  return onRecheck;
}

describe("PreUpgradeCheckItem", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(VersionsApi.reinstallFailedComponent).mockResolvedValue({});
  });

  afterEach(cleanup);

  it("reruns checks only after the KDC-protected repair executes", async () => {
    let protectedRepair: (() => void | Promise<void>) | undefined;
    vi.mocked(useKDCSessionState).mockReturnValue({
      isLoaded: true,
      getKDCSessionState: vi.fn(async (callback) => {
        protectedRepair = callback;
      }),
    });
    const onRecheck = renderComponent();

    fireEvent.click(screen.getByRole("button", { name: "REINSTALL" }));
    await waitFor(() => expect(protectedRepair).toBeDefined());
    expect(VersionsApi.reinstallFailedComponent).not.toHaveBeenCalled();
    expect(onRecheck).not.toHaveBeenCalled();

    await act(async () => {
      await protectedRepair?.();
    });
    expect(VersionsApi.reinstallFailedComponent).toHaveBeenCalledWith(
      "c1",
      "host1",
      "HDFS",
      "DATANODE",
    );
    expect(onRecheck).toHaveBeenCalledWith({ id: 7, type: "ROLLING" });
  });

  it("keeps a rejected protected repair visible and does not recheck", async () => {
    const requestError = new Error("component reinstall rejected");
    vi.mocked(VersionsApi.reinstallFailedComponent).mockRejectedValue(requestError);
    vi.mocked(useKDCSessionState).mockReturnValue({
      isLoaded: true,
      getKDCSessionState: vi.fn(async (callback, errorCallback) => {
        try {
          await callback();
        } catch (error) {
          errorCallback?.(error);
        }
      }),
    });
    const onRecheck = renderComponent();

    fireEvent.click(screen.getByRole("button", { name: "REINSTALL" }));
    expect(await screen.findByText("component reinstall rejected")).toBeTruthy();
    expect(onRecheck).not.toHaveBeenCalled();
  });
});
