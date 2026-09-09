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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AppContext } from "../../store/context";
import type { ManagedDependencyBindingPhase } from "../../api/serviceDependenciesApi";

const mocks = vi.hoisted(() => ({ list: vi.fn() }));
vi.mock("../../api/serviceDependenciesApi", () => ({ default: mocks }));

import ServiceDependencies, { dependencyPhaseTranslationKey } from "./ServiceDependencies";

const renderDependencies = (
  clusterName: string,
  runtimeKey = clusterName,
  returnTo?: string,
) => render(
  <MemoryRouter initialEntries={[{
    pathname: `/clusters/${clusterName}/main/services/HBASE/dependencies`,
    state: returnTo ? { returnTo } : undefined,
  }]}>
    <AppContext.Provider value={{ clusterName, runtimeKey } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
      <ServiceDependencies />
    </AppContext.Provider>
  </MemoryRouter>,
);

describe("HBase dependencies", () => {
  beforeEach(() => vi.resetAllMocks());

  it("shows managed identity, friendly progress, local ownership, and drift", async () => {
    mocks.list.mockResolvedValue([
      {
        applied_snapshot_version: 2,
        dependency_type: "HDFS",
        desired_snapshot_version: 3,
        ownership: "managed",
        phase: "PROVIDER_PREPARING",
        provider: {
          cluster_id: 41,
          cluster_name: "storage-east",
          security_mode: "KERBEROS",
          service_name: "HDFS",
          version: { service_version: "3.3.6" },
        },
        state: "PREPARING",
      },
      { dependency_type: "ZOOKEEPER", ownership: "local" },
    ]);

    renderDependencies("analytics");

    expect(await screen.findByText("storage-east / HDFS")).toBeTruthy();
    expect(screen.getAllByText("Prepare provider").length).toBeGreaterThan(0);
    expect(screen.getByText("Local cluster service")).toBeTruthy();
    expect(screen.getByText(/Applied version 2, desired version 3/)).toBeTruthy();
    expect(screen.queryByText("cluster_id")).toBeNull();
    expect(mocks.list).toHaveBeenCalledWith("analytics", expect.any(AbortSignal));
  });

  it("maps terminal, uncertain, and unknown server phases without implying a fresh review", () => {
    const expected: Record<ManagedDependencyBindingPhase, string> = {
      PREVIEWED: "serviceDependencies.phase.review",
      PROVIDER_PREPARING: "serviceDependencies.phase.prepareProvider",
      PROVIDER_PREPARED: "serviceDependencies.phase.installClients",
      ZOOKEEPER_HANDOFF_RECONCILING: "serviceDependencies.phase.confirmZookeeper",
      CONSUMER_VERIFYING: "serviceDependencies.phase.checkConnections",
      READY: "serviceDependencies.phase.ready",
      STALE: "serviceDependencies.phase.stale",
      FAILED: "serviceDependencies.phase.failed",
      DETACHING: "serviceDependencies.phase.detaching",
      FENCING_UNCERTAIN: "serviceDependencies.phase.checkExistingOperation",
      RETIRED: "serviceDependencies.phase.retired",
      DETACHED: "serviceDependencies.phase.detached",
      TOMBSTONED: "serviceDependencies.phase.removed",
    };
    Object.entries(expected).forEach(([phase, translationKey]) => {
      expect(dependencyPhaseTranslationKey(phase)).toBe(translationKey);
    });
    expect(dependencyPhaseTranslationKey("future-phase"))
      .toBe("serviceDependencies.phase.unavailable");
  });

  it("clears old rows and ignores a late response after the cluster runtime changes", async () => {
    let resolveOld!: (items: any[]) => void;
    mocks.list.mockImplementation((clusterName) => clusterName === "alpha"
      ? new Promise((resolve) => { resolveOld = resolve; })
      : Promise.resolve([{ dependency_type: "HDFS", ownership: "local" }]));
    const view = renderDependencies("alpha", "alice-alpha");
    await waitFor(() => expect(mocks.list).toHaveBeenCalledWith("alpha", expect.any(AbortSignal)));

    view.rerender(
      <MemoryRouter initialEntries={["/clusters/beta/main/services/HBASE/dependencies"]}>
        <AppContext.Provider value={{ clusterName: "beta", runtimeKey: "alice-beta" } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
          <ServiceDependencies />
        </AppContext.Provider>
      </MemoryRouter>,
    );
    expect(await screen.findByText("Local cluster service")).toBeTruthy();
    resolveOld([{ dependency_type: "HDFS", ownership: "managed", provider: {
      cluster_id: 9, cluster_name: "stale-provider", service_name: "HDFS",
    } }]);

    await waitFor(() => expect(screen.queryByText("stale-provider / HDFS")).toBeNull());
  });

  it("keeps a failed read scoped and reloads the selected cluster on Retry", async () => {
    mocks.list
      .mockRejectedValueOnce(new Error("dependency read failed"))
      .mockResolvedValueOnce([{ dependency_type: "ZOOKEEPER", ownership: "local" }]);

    renderDependencies("analytics", "alice-analytics");

    expect(await screen.findByText("dependency read failed")).toBeTruthy();
    expect(screen.queryByText("Local cluster service")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("Local cluster service")).toBeTruthy();
    expect(mocks.list).toHaveBeenNthCalledWith(2, "analytics", expect.any(AbortSignal));
  });

  it("offers the validated global directory return path", async () => {
    mocks.list.mockResolvedValue([{ dependency_type: "HDFS", ownership: "local" }]);

    renderDependencies("analytics", "alice-analytics", "/services?q=hbase&sort=cluster");

    const back = await screen.findByRole("link", { name: "Back to services" });
    expect(back.getAttribute("href")).toBe("/services?q=hbase&sort=cluster");
  });
});
