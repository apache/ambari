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

import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";

const mocks = vi.hoisted(() => ({
  getAlertInstancesByDefinition: vi.fn(),
  getAlertHistory: vi.fn(),
}));

vi.mock("../../api/alertsApi", () => ({ AlertsApi: mocks }));
vi.mock("../../components/Spinner", () => ({ default: () => <div>Loading instances</div> }));
vi.mock("../../components/Paginator", () => ({ default: () => <div>Pagination</div> }));
vi.mock("../../components/Table", () => ({
  default: ({ data }: { data: Array<{ host_name: string }> }) => (
    <div>{data.map((item) => <div key={item.host_name}>{item.host_name}</div>)}</div>
  ),
}));

import AlertInstancesTable from "./AlertInstancesTable";

const instancesResponse = {
  items: [{
    Alert: {
      service_name: "HDFS",
      host_name: "host1.example.test",
      state: "OK",
      maintenance_state: "OFF",
      last_updated_time: 1,
      text: "Healthy",
    },
  }],
};

function renderTable() {
  return render(
    <MemoryRouter>
      <AlertInstancesTable
        clusterName="c1"
        alert_id={11}
        definitionName="namenode_process"
      />
    </MemoryRouter>,
  );
}

describe("AlertInstancesTable loading and polling", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    mocks.getAlertInstancesByDefinition.mockResolvedValue(instancesResponse);
    mocks.getAlertHistory.mockResolvedValue({ items: [] });
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("keeps available instances when the independent history request fails", async () => {
    mocks.getAlertHistory.mockRejectedValueOnce(new Error("history unavailable"));
    renderTable();

    expect(await screen.findByText("host1.example.test")).toBeTruthy();
    expect(screen.getByText("24-hour alert history is temporarily unavailable.")).toBeTruthy();
    expect(screen.queryByText("Failed to load alert instances.")).toBeNull();
  });

  it("recovers an initial instance failure after a successful poll", async () => {
    vi.useFakeTimers();
    mocks.getAlertInstancesByDefinition
      .mockRejectedValueOnce(new Error("instances unavailable"))
      .mockResolvedValueOnce(instancesResponse);
    renderTable();

    await act(async () => undefined);
    expect(screen.getByText("Failed to load alert instances.")).toBeTruthy();

    await act(async () => vi.advanceTimersByTimeAsync(30000));
    expect(screen.getByText("host1.example.test")).toBeTruthy();
    expect(screen.queryByText("Failed to load alert instances.")).toBeNull();
  });
});
