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

const mocks = vi.hoisted(() => ({ fetchHostLogs: vi.fn() }));
vi.mock("../../api/hostLogsApi", () => ({
  default: { fetchHostLogs: mocks.fetchHostLogs },
}));
vi.mock("../../components/Spinner", () => ({
  default: () => <div>Loading log counts</div>,
}));

import HostLogMetrics from "./HostLogMetrics";

function renderMetrics() {
  return render(
    <MemoryRouter>
      <AppContext.Provider value={{ clusterName: "c1" } as never}>
        <ServiceContext.Provider value={{
          allServiceModels: [{ serviceName: "HDFS", displayName: "HDFS" }],
        } as never}>
          <HostLogMetrics hostName="host1" />
        </ServiceContext.Provider>
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

describe("Host log metrics", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.fetchHostLogs.mockResolvedValue({
      host_components: [{
        HostRoles: { service_name: "HDFS" },
        logging: {
          log_level_counts: [
            { name: "ERROR", value: "5" },
            { name: "INFO", value: "12" },
          ],
        },
      }],
    });
  });

  afterEach(cleanup);

  it("renders server-provided counts and links to the service logs", async () => {
    renderMetrics();
    expect(await screen.findByText("ERROR: 5")).toBeTruthy();
    expect(screen.getByText("INFO: 12")).toBeTruthy();
    expect(screen.getByRole("link", { name: "HDFS" }).getAttribute("href"))
      .toContain("service_name=HDFS");
  });

  it("shows unavailable data and retries request failures", async () => {
    mocks.fetchHostLogs
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce({ host_components: [] });
    renderMetrics();
    expect(await screen.findByText("Ambari could not load host log counts."))
      .toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.fetchHostLogs).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("Log level count data is unavailable."))
      .toBeTruthy();
  });
});
