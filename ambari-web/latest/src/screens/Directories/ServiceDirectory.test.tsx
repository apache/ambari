/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
import "../../i18n";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  canAccessCluster: vi.fn(),
  canViewClusterTasks: vi.fn(),
  getAllServices: vi.fn(),
  hasClusterAuthorization: vi.fn(),
  listDependencies: vi.fn(),
}));

vi.mock("../../api/serviceApi", () => ({
  ServiceApi: { getAllServices: mocks.getAllServices },
}));
vi.mock("../../api/serviceDependenciesApi", () => ({
  default: { list: mocks.listDependencies },
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({
    canAccessCluster: mocks.canAccessCluster,
    canViewClusterTasks: mocks.canViewClusterTasks,
    hasClusterAuthorization: mocks.hasClusterAuthorization,
  }),
}));

import ServiceDirectory from "./ServiceDirectory";

const clusters = [
  { Clusters: { cluster_id: 17, cluster_name: "alpha", provisioning_state: "INSTALLED", version: "3.0" } },
  { Clusters: { cluster_id: 29, cluster_name: "west / prod", provisioning_state: "INSTALLED", version: "3.0" } },
];
const services = (serviceName: string, state = "STARTED") => ({
  items: [{ ServiceInfo: { maintenance_state: "OFF", service_name: serviceName, state } }],
});

const renderDirectory = (
  initialEntry = "/services",
  availableClusters = clusters,
) => render(
  <AppContext.Provider value={{ availableClusters } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
    <MemoryRouter initialEntries={[initialEntry]}>
      <ServiceDirectory />
    </MemoryRouter>
  </AppContext.Provider>,
);

describe("global service directory", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.canAccessCluster.mockReturnValue(true);
    mocks.canViewClusterTasks.mockReturnValue(true);
    mocks.hasClusterAuthorization.mockReturnValue(true);
    mocks.getAllServices.mockImplementation(async (clusterName: string) =>
      services(clusterName === "alpha" ? "HBASE" : "HBASE"));
    mocks.listDependencies.mockResolvedValue([
      { dependency_type: "HDFS", ownership: "local" },
      { dependency_type: "ZOOKEEPER", ownership: "local" },
    ]);
  });
  afterEach(cleanup);

  it("renders identical service types as separate cluster-owned deployments", async () => {
    renderDirectory();

    const deployments = await screen.findAllByRole("link", { name: "HBASE" });
    expect(deployments.map((link) => link.getAttribute("href"))).toEqual([
      "/clusters/alpha/main/services/HBASE/summary",
      "/clusters/west%20%2F%20prod/main/services/HBASE/summary",
    ]);
    expect(screen.getAllByRole("link", { name: "Config" })[0].getAttribute("href"))
      .toBe("/clusters/alpha/main/services/HBASE/configs");
    expect(screen.getAllByRole("link", { name: "Tasks" })[1].getAttribute("href"))
      .toBe("/clusters/west%20%2F%20prod/main/requests");
    expect(screen.queryByText("ID 17")).toBeNull();
    expect(screen.getByRole("combobox", { name: "Sort by" })).toBeTruthy();
  });

  it("shows different managed providers on each HBase deployment and links back through filters", async () => {
    mocks.listDependencies.mockImplementation(async (clusterName: string) => ([
      {
        dependency_type: "HDFS",
        ownership: "managed",
        phase: clusterName === "alpha" ? "READY" : "PROVIDER_PREPARING",
        provider: {
          cluster_id: clusterName === "alpha" ? 81 : 82,
          cluster_name: clusterName === "alpha" ? "storage-east" : "storage-west",
          service_name: "HDFS",
        },
      },
      { dependency_type: "ZOOKEEPER", ownership: "local" },
    ]));
    renderDirectory("/services?q=base&sort=cluster");

    expect(await screen.findByText(/Storage: storage-east/)).toBeTruthy();
    expect(await screen.findByText(/Storage: storage-west/)).toBeTruthy();
    expect(screen.getByText(/Ready to start/)).toBeTruthy();
    expect(screen.getByText(/Prepare provider/)).toBeTruthy();
    expect(screen.getAllByRole("link", { name: "Dependencies" }).map((link) =>
      link.getAttribute("href"))).toEqual([
      "/clusters/alpha/main/services/HBASE/dependencies",
      "/clusters/west%20%2F%20prod/main/services/HBASE/dependencies",
    ]);
  });

  it("renders a deployment before its provider summary finishes", async () => {
    let resolveSummary!: (value: any[]) => void;
    mocks.listDependencies.mockImplementation(() => new Promise((resolve) => {
      resolveSummary = resolve;
    }));
    renderDirectory("/services", [clusters[0]]);

    expect(await screen.findByRole("link", { name: "HBASE" })).toBeTruthy();
    expect(await screen.findByText("Loading provider details...")).toBeTruthy();
    expect(screen.queryByText("Loading services...")).toBeNull();

    await act(async () => {
      resolveSummary([
        { dependency_type: "HDFS", ownership: "local" },
        { dependency_type: "ZOOKEEPER", ownership: "local" },
      ]);
      await Promise.resolve();
    });
    await waitFor(() => expect(screen.queryByText("Loading provider details...")).toBeNull());
  });

  it("does not fetch HBase provider summaries while an HDFS-only filter is active", async () => {
    mocks.getAllServices.mockResolvedValue({
      items: [
        { ServiceInfo: { maintenance_state: "OFF", service_name: "HBASE", state: "STARTED" } },
        { ServiceInfo: { maintenance_state: "OFF", service_name: "HDFS", state: "STARTED" } },
      ],
    });
    renderDirectory("/services?type=HDFS");

    expect(await screen.findAllByRole("link", { name: "HDFS" })).toHaveLength(2);
    await waitFor(() => expect(mocks.getAllServices).toHaveBeenCalledTimes(2));
    expect(mocks.listDependencies).not.toHaveBeenCalled();
  });

  it("loads only newly visible HBase summaries and reuses them after returning to a filter", async () => {
    mocks.listDependencies.mockImplementation(async (clusterName: string) => ([{
      dependency_type: "HDFS",
      ownership: "managed",
      phase: "READY",
      provider: {
        cluster_id: clusterName === "alpha" ? 81 : 82,
        cluster_name: `provider-${clusterName}`,
        service_name: "HDFS",
      },
    }]));
    renderDirectory("/services?cluster=alpha");

    expect(await screen.findByText("Storage: provider-alpha")).toBeTruthy();
    expect(mocks.listDependencies).toHaveBeenCalledTimes(1);
    fireEvent.change(screen.getByLabelText("Cluster"), { target: { value: "west / prod" } });

    expect(await screen.findByText("Storage: provider-west / prod")).toBeTruthy();
    expect(mocks.listDependencies).toHaveBeenCalledTimes(2);
    fireEvent.change(screen.getByLabelText("Cluster"), { target: { value: "alpha" } });

    expect(await screen.findByText("Storage: provider-alpha")).toBeTruthy();
    expect(mocks.listDependencies.mock.calls.filter(([name]) => name === "alpha"))
      .toHaveLength(1);
  });

  it("drops a queued provider summary when its row leaves the current filter", async () => {
    const fiveClusters = Array.from({ length: 5 }, (_, index) => ({
      Clusters: {
        cluster_id: index + 1,
        cluster_name: `cluster-${index + 1}`,
        provisioning_state: "INSTALLED",
        version: "3.0",
      },
    }));
    const pendingResolvers: Array<(value: any) => void> = [];
    mocks.getAllServices.mockImplementation((clusterName: string) => clusterName === "cluster-1"
      ? Promise.resolve(services("HBASE"))
      : new Promise((resolve) => pendingResolvers.push(resolve)));
    renderDirectory("/services?cluster=cluster-1", fiveClusters);

    expect(await screen.findByRole("link", { name: "HBASE" })).toBeTruthy();
    await waitFor(() => expect(mocks.getAllServices).toHaveBeenCalledTimes(5));
    expect(mocks.listDependencies).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("Search services"), {
      target: { value: "hidden-row" },
    });
    await act(async () => {
      pendingResolvers.forEach((resolve) => resolve(services("HDFS")));
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(mocks.listDependencies).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("Search services"), { target: { value: "" } });
    await waitFor(() => expect(mocks.listDependencies)
      .toHaveBeenCalledWith("cluster-1", expect.any(AbortSignal)));
  });

  it("keeps a deployment visible and retries only its failed provider summary", async () => {
    mocks.listDependencies.mockImplementation(async (clusterName: string) => {
      const calls = mocks.listDependencies.mock.calls.filter(([name]) => name === clusterName).length;
      if (clusterName === "alpha" && calls === 1) throw new Error("summary unavailable");
      return [{
        dependency_type: "HDFS",
        ownership: "managed",
        phase: "READY",
        provider: { cluster_id: 81, cluster_name: "storage-east", service_name: "HDFS" },
      }];
    });
    renderDirectory();

    expect(await screen.findAllByRole("link", { name: "HBASE" })).toHaveLength(2);
    expect(await screen.findByText(/Provider details are unavailable/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry provider details" }));

    expect(await screen.findByText(/Storage: storage-east/)).toBeTruthy();
    expect(mocks.listDependencies.mock.calls.filter(([name]) => name === "alpha"))
      .toHaveLength(2);
    expect(mocks.getAllServices.mock.calls.filter(([name]) => name === "alpha"))
      .toHaveLength(1);
  });

  it("ignores a late provider summary after its authorized numeric cluster is removed", async () => {
    let resolveOld!: (value: any[]) => void;
    mocks.listDependencies.mockImplementation((clusterName: string) => clusterName === "alpha"
      ? new Promise((resolve) => { resolveOld = resolve; })
      : Promise.resolve([{
          dependency_type: "HDFS",
          ownership: "managed",
          phase: "READY",
          provider: { cluster_id: 82, cluster_name: "current-provider", service_name: "HDFS" },
        }]));
    const view = renderDirectory("/services", [clusters[0]]);
    await screen.findByRole("link", { name: "HBASE" });
    await waitFor(() => expect(mocks.listDependencies)
      .toHaveBeenCalledWith("alpha", expect.any(AbortSignal)));

    view.rerender(
      <AppContext.Provider value={{ availableClusters: [clusters[1]] } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
        <MemoryRouter initialEntries={["/services"]}>
          <ServiceDirectory />
        </MemoryRouter>
      </AppContext.Provider>,
    );
    expect(await screen.findByText(/Storage: current-provider/)).toBeTruthy();
    resolveOld([{
      dependency_type: "HDFS",
      ownership: "managed",
      phase: "READY",
      provider: { cluster_id: 90, cluster_name: "revoked-provider", service_name: "HDFS" },
    }]);

    await waitFor(() => expect(screen.queryByText(/revoked-provider/)).toBeNull());
  });

  it("does not request services for a cluster outside the user's scope", async () => {
    mocks.canAccessCluster.mockImplementation((clusterName) => clusterName === "alpha");
    renderDirectory();

    await screen.findByRole("link", { name: "HBASE" });
    expect(mocks.getAllServices).toHaveBeenCalledTimes(1);
    expect(mocks.getAllServices).toHaveBeenCalledWith("alpha", expect.any(AbortSignal));
  });

  it("evaluates task visibility against each row's cluster role", async () => {
    mocks.canViewClusterTasks.mockImplementation((clusterName) => clusterName !== "alpha");
    renderDirectory();

    await screen.findAllByRole("link", { name: "HBASE" });
    const taskLinks = screen.getAllByRole("link", { name: "Tasks" });
    expect(taskLinks).toHaveLength(1);
    expect(taskLinks[0].getAttribute("href"))
      .toBe("/clusters/west%20%2F%20prod/main/requests");
  });

  it("keeps successful rows visible and retries only the failed cluster", async () => {
    mocks.getAllServices.mockImplementation(async (clusterName: string) => {
      if (clusterName === "alpha" && mocks.getAllServices.mock.calls
        .filter(([name]) => name === "alpha").length === 1) {
        throw new Error("unavailable");
      }
      return services(clusterName === "alpha" ? "HDFS" : "HBASE");
    });
    renderDirectory();

    expect(await screen.findByRole("link", { name: "HBASE" })).toBeTruthy();
    expect(await screen.findByText(/alpha.*Services could not be loaded/s)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry services for alpha" }));

    expect(await screen.findByRole("link", { name: "HDFS" })).toBeTruthy();
    expect(mocks.getAllServices.mock.calls.filter(([name]) => name === "alpha")).toHaveLength(2);
    expect(mocks.getAllServices.mock.calls.filter(([name]) => name === "west / prod"))
      .toHaveLength(1);
  });

  it("applies shareable URL filters without changing the owning cluster links", async () => {
    mocks.getAllServices.mockImplementation(async (clusterName: string) => ({
      items: [
        { ServiceInfo: { maintenance_state: "OFF", service_name: "HBASE", state: "STARTED" } },
        { ServiceInfo: { maintenance_state: "OFF", service_name: "HDFS", state: "STARTED" } },
      ],
      clusterName,
    }));
    renderDirectory("/services?cluster=west+%2F+prod&type=HBASE&q=base&sort=cluster&direction=desc");

    const deployment = await screen.findByRole("link", { name: "HBASE" });
    expect(deployment.getAttribute("href"))
      .toBe("/clusters/west%20%2F%20prod/main/services/HBASE/summary");
    await waitFor(() => expect(screen.queryByRole("link", { name: "HDFS" })).toBeNull());
  });

  it("ignores a late service response after the authorized cluster set changes", async () => {
    let resolveAlpha!: (value: any) => void;
    mocks.getAllServices.mockImplementation((clusterName: string) => {
      if (clusterName === "alpha") {
        return new Promise((resolve) => { resolveAlpha = resolve; });
      }
      return Promise.resolve(services("BETA_ONLY"));
    });
    const view = renderDirectory("/services", [clusters[0]]);
    await waitFor(() => expect(mocks.getAllServices)
      .toHaveBeenCalledWith("alpha", expect.any(AbortSignal)));

    view.rerender(
      <AppContext.Provider value={{ availableClusters: [clusters[1]] } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
        <MemoryRouter initialEntries={["/services"]}>
          <ServiceDirectory />
        </MemoryRouter>
      </AppContext.Provider>,
    );
    expect(await screen.findByRole("link", { name: "BETA_ONLY" })).toBeTruthy();
    resolveAlpha(services("ALPHA_ONLY"));

    await waitFor(() => expect(screen.queryByRole("link", { name: "ALPHA_ONLY" })).toBeNull());
  });

  it("cancels queued requests when the authorized cluster generation changes", async () => {
    const oldClusters = Array.from({ length: 6 }, (_, index) => ({
      Clusters: {
        cluster_id: index + 1,
        cluster_name: `old-${index + 1}`,
        provisioning_state: "INSTALLED",
        version: "3.0",
      },
    }));
    const resolveOldRequests: Array<(value: any) => void> = [];
    mocks.getAllServices.mockImplementation((clusterName: string) => {
      if (clusterName === "current") return Promise.resolve(services("CURRENT"));
      return new Promise((resolve) => resolveOldRequests.push(resolve));
    });
    const view = renderDirectory("/services", oldClusters);
    await waitFor(() => expect(mocks.getAllServices).toHaveBeenCalledTimes(4));

    view.rerender(
      <AppContext.Provider value={{
        availableClusters: [{ Clusters: {
          cluster_id: 50,
          cluster_name: "current",
          provisioning_state: "INSTALLED",
          version: "3.0",
        } }],
      } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
        <MemoryRouter initialEntries={["/services"]}>
          <ServiceDirectory />
        </MemoryRouter>
      </AppContext.Provider>,
    );

    expect(await screen.findByRole("link", { name: "CURRENT" })).toBeTruthy();
    await act(async () => {
      resolveOldRequests.forEach((resolve) => resolve(services("REVOKED")));
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(mocks.getAllServices.mock.calls.some(([name]) => name === "old-5")).toBe(false);
    expect(mocks.getAllServices.mock.calls.some(([name]) => name === "old-6")).toBe(false);
    expect(screen.queryByRole("link", { name: "REVOKED" })).toBeNull();
  });

  it("does not render stale rows after a same-name cluster is recreated", async () => {
    let resolveReplacement!: (value: any) => void;
    mocks.getAllServices.mockResolvedValueOnce(services("OLD_DEPLOYMENT"));
    const view = renderDirectory("/services", [clusters[0]]);
    expect(await screen.findByRole("link", { name: "OLD_DEPLOYMENT" })).toBeTruthy();

    mocks.getAllServices.mockImplementationOnce(() => new Promise((resolve) => {
      resolveReplacement = resolve;
    }));
    view.rerender(
      <AppContext.Provider value={{
        availableClusters: [{ Clusters: {
          ...clusters[0].Clusters,
          cluster_id: 99,
        } }],
      } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
        <MemoryRouter initialEntries={["/services"]}>
          <ServiceDirectory />
        </MemoryRouter>
      </AppContext.Provider>,
    );

    expect(screen.queryByRole("link", { name: "OLD_DEPLOYMENT" })).toBeNull();
    resolveReplacement(services("NEW_DEPLOYMENT"));
    expect(await screen.findByRole("link", { name: "NEW_DEPLOYMENT" })).toBeTruthy();
  });

  it("queues a retry behind the same four-request limit", async () => {
    const fiveClusters = Array.from({ length: 5 }, (_, index) => ({
      Clusters: {
        cluster_id: index + 1,
        cluster_name: `cluster-${index + 1}`,
        provisioning_state: "INSTALLED",
        version: "3.0",
      },
    }));
    let resolveSecond!: (value: any) => void;
    const pending = new Map<string, Promise<any>>();
    pending.set("cluster-2", new Promise((resolve) => { resolveSecond = resolve; }));
    ["cluster-3", "cluster-4", "cluster-5"].forEach((name) => {
      pending.set(name, new Promise(() => undefined));
    });
    mocks.getAllServices.mockImplementation((clusterName: string) => {
      const clusterOneCalls = mocks.getAllServices.mock.calls
        .filter(([name]) => name === "cluster-1").length;
      if (clusterName === "cluster-1") {
        return clusterOneCalls === 1
          ? Promise.reject(new Error("unavailable"))
          : Promise.resolve(services("RETRIED"));
      }
      return pending.get(clusterName);
    });
    renderDirectory("/services", fiveClusters);
    await waitFor(() => expect(mocks.getAllServices).toHaveBeenCalledTimes(5));

    fireEvent.click(screen.getByRole("button", { name: "Retry services for cluster-1" }));
    expect(mocks.getAllServices.mock.calls.filter(([name]) => name === "cluster-1"))
      .toHaveLength(1);
    resolveSecond(services("SECOND"));

    expect(await screen.findByRole("link", { name: "RETRIED" })).toBeTruthy();
    expect(mocks.getAllServices.mock.calls.filter(([name]) => name === "cluster-1"))
      .toHaveLength(2);
  });
});
