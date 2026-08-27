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
import type { ComponentProps, ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  getAlertDefinition: vi.fn(),
  getAlerts: vi.fn(),
  getGroupFormattedAlertsNotifications: vi.fn(),
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
  default: ({
    columns,
    data,
  }: {
    columns: Array<{
      accessorKey?: string;
      cell?: (value: { row: { original: Record<string, unknown> } }) => ReactNode;
    }>;
    data: Array<{ label: string } & Record<string, unknown>>;
  }) => (
    <div data-testid="alerts-table">
      {data.map((item) => <span key={item.label}>{item.label}</span>)}
      {data.length
        ? columns.find((column) => column.accessorKey === "enabled")?.cell?.({
            row: { original: data[0] },
          })
        : null}
    </div>
  ),
}));

vi.mock("../../components/Paginator", () => ({ default: () => null }));
vi.mock("../../components/LastStatusChanged", () => ({ default: () => null }));
vi.mock("../../components/Modal", () => ({
  default: ({
    isOpen,
    successCallback,
  }: {
    isOpen: boolean;
    successCallback: () => void;
  }) => isOpen ? <button onClick={successCallback}>Confirm alert state</button> : null,
}));
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
      processed: {
        alert_definition_id: 1,
        enabled: true,
        label,
        statuses: [],
      },
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
    mocks.getGroupFormattedAlertsNotifications.mockResolvedValue({
      alerts_summary_grouped: [],
    });
  });

  afterEach(cleanup);

  it("does not let an older action refresh overwrite a newer refresh", async () => {
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
      .mockResolvedValueOnce(alertsResponse("initial alert"))
      .mockReturnValueOnce(oldAlerts)
      .mockResolvedValueOnce(alertsResponse("new alert"));
    mocks.getAlertDefinition
      .mockResolvedValueOnce(definitionsResponse("initial definition"))
      .mockReturnValueOnce(oldDefinitions)
      .mockResolvedValueOnce(definitionsResponse("new definition"));
    mocks.updateAlertDefinitionState.mockResolvedValue({});
    renderAlerts();

    expect(await screen.findByText("initial alert")).toBeTruthy();
    fireEvent.click(screen.getByText("Enabled"));
    fireEvent.click(screen.getByRole("button", { name: "Confirm alert state" }));
    fireEvent.click(await screen.findByText("Disabled"));
    fireEvent.click(screen.getByRole("button", { name: "Confirm alert state" }));
    expect(await screen.findByText("new alert")).toBeTruthy();
    expect(await screen.findByText("new definition")).toBeTruthy();

    await act(async () => {
      resolveOldAlerts(alertsResponse("old alert"));
      resolveOldDefinitions(definitionsResponse("old definition"));
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
