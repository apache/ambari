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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";

const mocks = vi.hoisted(() => ({ getHostAlertInstances: vi.fn() }));
vi.mock("../../api/alertsApi", () => ({
  AlertsApi: { getHostAlertInstances: mocks.getHostAlertInstances },
}));
vi.mock("../../components/Paginator", () => ({ default: () => <div>Pagination</div> }));
vi.mock("../../components/Spinner", () => ({ default: () => <div>Loading alerts</div> }));
vi.mock("../../components/Table", () => ({
  default: ({ columns, data }: { columns: any[]; data: any[] }) => (
    <div>
      {data.map((item) => (
        <div key={item.alert_definition_id}>
          {columns.map((column, index) => (
            <span key={`${column.accessorKey || index}-${item.alert_definition_id}`}>
              {column.cell
                ? column.cell({ row: { original: item } })
                : item[column.accessorKey]}
            </span>
          ))}
        </div>
      ))}
    </div>
  ),
}));

import HostAlerts from "./HostAlerts";

const alertResponse = {
  items: [
    {
      Alert: {
        definition_id: 11,
        label: "DataNode process",
        latest_timestamp: 123,
        maintenance_state: "ON",
        service_name: "HDFS",
        state: "CRITICAL",
        text: "A complete response that must not be truncated by the host alerts table.",
      },
    },
    {
      Alert: {
        definition_id: 12,
        label: "Agent heartbeat",
        latest_timestamp: 124,
        maintenance_state: "OFF",
        service_name: "AMBARI",
        state: "OK",
        text: "Agent is healthy",
      },
    },
  ],
};

function renderAlerts() {
  return render(
    <MemoryRouter>
      <AppContext.Provider value={{ clusterName: "c1" } as any}>
        <ServiceContext.Provider value={{
          allServiceModels: {
            hdfs: { displayName: "HDFS Display", serviceName: "hdfs" },
          },
        } as any}>
          <HostAlerts hostname="host1" />
        </ServiceContext.Provider>
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

describe("Host Alerts", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getHostAlertInstances.mockResolvedValue(alertResponse);
  });

  afterEach(() => cleanup());

  it("loads once, preserves maintenance alerts, and links by backend identifiers", async () => {
    renderAlerts();

    const serviceLink = await screen.findByRole("link", { name: "HDFS Display" });
    expect(serviceLink.getAttribute("href")).toBe("/main/services/HDFS/summary");
    expect(screen.getByRole("link", { name: "DataNode process" }).getAttribute("href"))
      .toBe("/main/alerts/11");
    expect(screen.getByTitle("Maintenance mode")).toBeTruthy();
    expect(screen.getByText(
      "A complete response that must not be truncated by the host alerts table.",
    )).toBeTruthy();
    expect(screen.queryByRole("link", { name: "AMBARI" })).toBeNull();
    expect(mocks.getHostAlertInstances).toHaveBeenCalledTimes(1);
    expect(mocks.getHostAlertInstances).toHaveBeenCalledWith("c1", "host1");
  });

  it("applies definition filtering without being reset by sorting", async () => {
    renderAlerts();
    await screen.findByRole("link", { name: "DataNode process" });

    fireEvent.change(screen.getByLabelText("Alert Definition Name"), {
      target: { value: "heartbeat" },
    });
    await waitFor(() => expect(screen.queryByText("DataNode process")).toBeNull());
    expect(screen.getByText("Agent heartbeat")).toBeTruthy();
  });

  it("shows a recoverable server failure", async () => {
    mocks.getHostAlertInstances
      .mockRejectedValueOnce({ response: { data: { message: "Alert service unavailable" } } })
      .mockResolvedValueOnce(alertResponse);
    renderAlerts();

    expect(await screen.findByText("Alert service unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.getHostAlertInstances).toHaveBeenCalledTimes(2));
    expect(await screen.findByRole("link", { name: "DataNode process" })).toBeTruthy();
  });
});
