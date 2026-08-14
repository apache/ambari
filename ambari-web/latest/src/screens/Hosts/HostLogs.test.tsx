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
import { AppContext } from "../../store/context";
import { ServiceContext } from "../../store/ServiceContext";

const mocks = vi.hoisted(() => ({ fetchHostLogs: vi.fn() }));
vi.mock("../../api/hostLogsApi", () => ({
  default: {
    fetchHostLogs: mocks.fetchHostLogs,
    fetchLogTail: vi.fn(),
  },
}));
vi.mock("../../components/Paginator", () => ({ default: () => <div>Pagination</div> }));
vi.mock("../../components/Spinner", () => ({ default: () => <div>Loading logs</div> }));
vi.mock("../../hooks/useLazyQuicklinks", () => ({
  useLazyQuicklinks: () => ({
    loadQuicklinks: vi.fn(),
    quicklinks: [{
      links: [{ label: "Log Search UI", url: "https://logsearch.example:61888" }],
    }],
  }),
}));
vi.mock("./HostLogTailModal", () => ({
  default: ({ filePath, logSearchUrl }: { filePath: string; logSearchUrl: string }) => (
    <div data-testid="log-tail">Tail {filePath} {logSearchUrl}</div>
  ),
}));

import HostLogs from "./HostLogs";

const response = {
  host_components: [{
    HostRoles: {
      component_name: "NAMENODE",
      display_name: "NameNode",
      service_name: "HDFS",
    },
    logging: {
      name: "hdfs_namenode",
      logs: [{ name: "/var/log/hdfs/namenode.log" }],
    },
  }],
};

function renderLogs() {
  return render(
    <AppContext.Provider value={{ clusterName: "c1" } as any}>
      <ServiceContext.Provider value={{
        allServiceModels: [{ serviceName: "HDFS", displayName: "HDFS" }],
      } as any}>
        <HostLogs hostName="host1" />
      </ServiceContext.Provider>
    </AppContext.Provider>,
  );
}

describe("Host Logs", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.fetchHostLogs.mockResolvedValue(response);
  });
  afterEach(() => cleanup());

  it("lists a host log and opens its tail", async () => {
    renderLogs();
    const logSearchLink = await screen.findByRole("link", { name: "Open in Log Search" });
    expect(logSearchLink.getAttribute("href")).toContain("hosts=host1");
    expect(logSearchLink.getAttribute("href")).toContain("components=hdfs_namenode");
    fireEvent.click(await screen.findByRole("button", { name: "namenode.log" }));
    expect(screen.getByTestId("log-tail").textContent).toContain(
      "Tail /var/log/hdfs/namenode.log",
    );
  });

  it("exposes a recoverable metadata failure", async () => {
    mocks.fetchHostLogs
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce(response);
    renderLogs();
    expect(await screen.findByText("Ambari could not load host logs.")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.fetchHostLogs).toHaveBeenCalledTimes(2));
    expect(await screen.findByRole("button", { name: "namenode.log" })).toBeTruthy();
  });
});
