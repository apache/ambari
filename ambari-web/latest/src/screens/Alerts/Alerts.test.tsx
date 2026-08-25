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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  fetchData: undefined as undefined | (() => Promise<void>),
  getAlertDefinition: vi.fn(),
  getAlerts: vi.fn(),
  getGroupFormattedAlertsNotifications: vi.fn(),
  pausePolling: vi.fn(),
  resumePolling: vi.fn(),
  updateAlertDefinitionState: vi.fn(),
}));

vi.mock("../../api/alertsApi", () => ({
  AlertsApi: {
    getAlertDefinition: mocks.getAlertDefinition,
    getAlerts: mocks.getAlerts,
    getGroupFormattedAlertsNotifications:
      mocks.getGroupFormattedAlertsNotifications,
    updateAlertDefinitionState: mocks.updateAlertDefinitionState,
  },
}));

vi.mock("../../hooks/useAuthorizationPolicy", () => ({
  default: () => ({ isAuthorized: () => true }),
}));

vi.mock("../../hooks/usePolling", () => ({
  default: (callback: () => Promise<void>) => {
    mocks.fetchData = callback;
    return {
      isPaused: false,
      pausePolling: mocks.pausePolling,
      resumePolling: mocks.resumePolling,
      stopPolling: vi.fn(),
    };
  },
}));

vi.mock("../../hooks/usePagination", () => ({
  default: (items: unknown[]) => ({
    changePage: vi.fn(),
    currentItems: items,
    currentPage: 1,
    itemsPerPage: 10,
    maxPage: 1,
    setItemsPerPage: vi.fn(),
  }),
}));

vi.mock("../../components/Table", () => ({
  default: ({ data }: { data: Array<{ label: string }> }) => (
    <div data-testid="alerts-table">
      {data.map((item) => <span key={item.label}>{item.label}</span>)}
    </div>
  ),
}));

vi.mock("../../components/Paginator", () => ({ default: () => null }));
vi.mock("../../components/LastStatusChanged", () => ({ default: () => null }));
vi.mock("../../components/Modal", () => ({ default: () => null }));
vi.mock("./MenuBar", () => ({
  default: ({ alertDefinitions }: { alertDefinitions: Array<{ label: string }> }) => (
    <div data-testid="alert-definitions">
      {alertDefinitions.map((definition) => (
        <span key={definition.label}>{definition.label}</span>
      ))}
    </div>
  ),
}));

vi.mock("../../Utils/Utility", () => ({ getCurrTimeInSec: () => 1 }));
vi.mock("./alertUtils", () => ({
  filterAlerts: (alerts: unknown[]) => alerts,
  processData: (response: { items: Array<{ processed: unknown }> }) =>
    response.items.map((item) => item.processed),
  sortAlerts: (alerts: unknown[]) => alerts,
}));

import Alerts from "./Alerts";

const alertsResponse = (label: string) => ({
  items: [
    {
      AlertGroup: { default: false, definitions: [], name: label },
      processed: { label, statuses: [] },
    },
  ],
});

const definitionsResponse = (label: string) => ({
  items: [
    {
      AlertDefinition: {
        component_name: "NAMENODE",
        enabled: true,
        id: 1,
        label,
        name: label,
        service_name: "HDFS",
      },
    },
  ],
});

const renderAlerts = () => render(
  <AppContext.Provider
    value={{ clusterName: "c1" } as unknown as ComponentProps<
      typeof AppContext.Provider
    >["value"]}
  >
    <Alerts />
  </AppContext.Provider>,
);

describe("Alerts request ordering", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.fetchData = undefined;
    mocks.getGroupFormattedAlertsNotifications.mockResolvedValue({
      alerts_summary_grouped: [],
    });
  });

  afterEach(cleanup);

  it("does not let an older poll overwrite a successful manual refresh", async () => {
    let resolveOldAlerts: (value: ReturnType<typeof alertsResponse>) => void =
      () => undefined;
    let resolveOldDefinitions: (
      value: ReturnType<typeof definitionsResponse>,
    ) => void = () => undefined;
    const oldAlerts = new Promise<ReturnType<typeof alertsResponse>>(
      (resolve) => {
        resolveOldAlerts = resolve;
      },
    );
    const oldDefinitions = new Promise<ReturnType<typeof definitionsResponse>>(
      (resolve) => {
        resolveOldDefinitions = resolve;
      },
    );

    mocks.getAlerts
      .mockRejectedValueOnce(new Error("initial failure"))
      .mockReturnValueOnce(oldAlerts)
      .mockResolvedValueOnce(alertsResponse("new alert"));
    mocks.getAlertDefinition
      .mockReturnValueOnce(oldDefinitions)
      .mockResolvedValueOnce(definitionsResponse("new definition"));
    renderAlerts();

    await act(async () => {
      await mocks.fetchData?.();
    });
    expect(await screen.findByRole("button", { name: "Retry" })).toBeTruthy();

    const oldPoll = mocks.fetchData?.();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByText("new alert")).toBeTruthy();
    expect(await screen.findByText("new definition")).toBeTruthy();

    await act(async () => {
      resolveOldAlerts(alertsResponse("old alert"));
      resolveOldDefinitions(definitionsResponse("old definition"));
      await oldPoll;
      await oldDefinitions;
    });

    await waitFor(() => {
      expect(screen.getByText("new alert")).toBeTruthy();
      expect(screen.getByText("new definition")).toBeTruthy();
    });
    expect(screen.queryByText("old alert")).toBeNull();
    expect(screen.queryByText("old definition")).toBeNull();
  });
});
