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

import { act, renderHook, waitFor } from "@testing-library/react";
import type { ComponentProps, PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import VersionsApi from "../api/versionsApi";
import ClusterApi from "../api/clusterApi";
import { AppContext } from "../store/context";
import { useUpgrade } from "./useUpgrade";

vi.mock("../api/versionsApi", () => ({
  default: {
    getUpgradeOperations: vi.fn(),
    getUpgradeItem: vi.fn(),
    getFailedServiceChecks: vi.fn(),
    setUpgradeItemState: vi.fn(),
  },
}));
vi.mock("../api/clusterApi", () => ({
  default: { postPersistData: vi.fn() },
}));

const contextSpies = {
  setUpgradeState: vi.fn(),
  setCurrentStackVersion: vi.fn(),
  setUpgradeIsFinalizeItem: vi.fn(),
};
const contextValue = {
  clusterName: "c1",
  ...contextSpies,
} as unknown as ComponentProps<typeof AppContext.Provider>["value"];

function wrapper({ children }: PropsWithChildren) {
  return <AppContext.Provider value={contextValue}>{children}</AppContext.Provider>;
}

const completedUpgrade = {
  Upgrade: {
    associated_version: "3.1.5",
    direction: "UPGRADE",
    downgrade_allowed: false,
    progress_percent: 100,
    request_status: "COMPLETED",
    skip_failures: false,
    skip_service_check_failures: false,
    suspended: false,
    upgrade_type: "ROLLING",
  },
  upgrade_groups: [],
};

const failedUpgrade = {
  Upgrade: {
    ...completedUpgrade.Upgrade,
    progress_percent: 50,
    request_status: "FAILED",
  },
  upgrade_groups: [{
    UpgradeGroup: {
      completed_task_count: 0,
      display_status: "FAILED",
      group_id: 2,
      in_progress_task_count: 0,
      name: "Core",
      progress_percent: 50,
      request_id: 17,
      status: "FAILED",
      title: "Core",
      total_task_count: 1,
    },
    upgrade_items: [{
      UpgradeItem: {
        context: "Install packages",
        display_status: "FAILED",
        group_id: 2,
        progress_percent: 50,
        request_id: 17,
        skippable: true,
        stage_id: 3,
        status: "FAILED",
        text: "Install packages",
      },
    }],
  }],
};

describe("useUpgrade read-only loading", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(ClusterApi.postPersistData).mockResolvedValue({});
  });

  it("shows an initial load failure and retries without mutating active-upgrade state", async () => {
    vi.mocked(VersionsApi.getUpgradeOperations)
      .mockRejectedValueOnce(new Error("upgrade unavailable"))
      .mockResolvedValueOnce(completedUpgrade as never);
    const { result } = renderHook(() => useUpgrade(17, true), { wrapper });

    await waitFor(() => expect(result.current.loadError).toBe("upgrade unavailable"));
    expect(result.current.loading).toBe(false);

    await act(async () => {
      await result.current.retryFetch();
    });
    await waitFor(() => expect(result.current.data).toEqual(completedUpgrade));
    expect(result.current.groups).toEqual([]);
    expect(contextSpies.setUpgradeState).not.toHaveBeenCalled();
    expect(contextSpies.setUpgradeIsFinalizeItem).not.toHaveBeenCalled();
    expect(ClusterApi.postPersistData).not.toHaveBeenCalled();
  });

  it("restarts polling after retrying an item from a terminal request", async () => {
    vi.mocked(VersionsApi.getUpgradeOperations).mockResolvedValue(failedUpgrade as never);
    vi.mocked(VersionsApi.setUpgradeItemState).mockResolvedValue({} as never);
    const { result } = renderHook(() => useUpgrade(17, false), { wrapper });

    await waitFor(() => expect(result.current.currUpgradeItem?.UpgradeItem.stage_id).toBe(3));
    const callsBeforeRetry = vi.mocked(VersionsApi.getUpgradeOperations).mock.calls.length;

    await act(async () => {
      await result.current.setUpgradeItemStatus(result.current.currUpgradeItem!, "PENDING");
    });

    expect(VersionsApi.setUpgradeItemState).toHaveBeenCalledWith("c1", {
      upgradeId: 17,
      itemId: 3,
      groupId: 2,
      status: "PENDING",
    });
    await waitFor(() => {
      expect(VersionsApi.getUpgradeOperations).toHaveBeenCalledTimes(callsBeforeRetry + 1);
    });
  });
});
