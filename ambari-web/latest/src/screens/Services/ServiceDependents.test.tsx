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

import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import "../../i18n";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({ getDependents: vi.fn() }));
vi.mock("../../api/serviceDependenciesApi", () => ({ default: mocks }));

import ServiceDependents from "./ServiceDependents";

const renderDependents = (
  clusterName: string,
  serviceName = "HDFS",
  runtimeKey = clusterName,
) => render(
  <MemoryRouter>
    <AppContext.Provider value={{ clusterName, runtimeKey } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
      <ServiceDependents serviceName={serviceName} />
    </AppContext.Provider>
  </MemoryRouter>,
);

describe("provider dependents", () => {
  beforeEach(() => vi.resetAllMocks());

  it("renders only server-authorized named consumers and keeps hidden consumers anonymous", async () => {
    mocks.getDependents.mockResolvedValue({
      hidden_dependent_count: 2,
      impact_revision: "revision-a",
      items: [
        {
          binding_id: "binding-a",
          consumer_cluster_id: 41,
          consumer_cluster_name: "analytics",
          consumer_service_name: "HBASE",
          dependency_type: "HDFS",
          state: "READY",
        },
        {
          binding_id: "binding-future",
          consumer_cluster_id: 42,
          consumer_cluster_name: "research",
          consumer_service_name: "HBASE",
          dependency_type: "HDFS",
          state: "FUTURE_STATE",
        },
      ],
    });

    renderDependents("storage-east");

    const consumer = await screen.findByRole("link", { name: "analytics / HBASE" });
    expect(consumer.getAttribute("href"))
      .toBe("/clusters/analytics/main/services/HBASE/dependencies");
    expect(screen.getByText(/2 additional dependents are hidden/)).toBeTruthy();
    expect(screen.getByText("Ready to start")).toBeTruthy();
    expect(screen.getByText("Status unavailable")).toBeTruthy();
    expect(screen.queryByText(/consumer_cluster_id/)).toBeNull();
    expect(mocks.getDependents).toHaveBeenCalledWith(
      "storage-east",
      "HDFS",
      expect.any(AbortSignal),
    );
  });

  it("clears old consumers and ignores a late provider response after scope changes", async () => {
    let resolveOld!: (value: any) => void;
    mocks.getDependents.mockImplementation((clusterName: string) => clusterName === "provider-a"
      ? new Promise((resolve) => { resolveOld = resolve; })
      : Promise.resolve({
          hidden_dependent_count: 0,
          impact_revision: "revision-b",
          items: [{
            binding_id: "binding-b",
            consumer_cluster_id: 52,
            consumer_cluster_name: "current-consumer",
            consumer_service_name: "HBASE",
            dependency_type: "ZOOKEEPER",
            state: "READY",
          }],
        }));
    const view = renderDependents("provider-a", "HDFS", "alice-provider-a");
    await waitFor(() => expect(mocks.getDependents).toHaveBeenCalledWith(
      "provider-a", "HDFS", expect.any(AbortSignal),
    ));

    view.rerender(
      <MemoryRouter>
        <AppContext.Provider value={{ clusterName: "provider-b", runtimeKey: "alice-provider-b" } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
          <ServiceDependents serviceName="ZOOKEEPER" />
        </AppContext.Provider>
      </MemoryRouter>,
    );
    expect(await screen.findByRole("link", { name: "current-consumer / HBASE" })).toBeTruthy();
    await act(async () => {
      resolveOld({
        hidden_dependent_count: 0,
        impact_revision: "revision-old",
        items: [{
          binding_id: "binding-old",
          consumer_cluster_id: 61,
          consumer_cluster_name: "stale-consumer",
          consumer_service_name: "HBASE",
          dependency_type: "HDFS",
          state: "READY",
        }],
      });
      await Promise.resolve();
    });

    await waitFor(() => expect(screen.queryByText(/stale-consumer/)).toBeNull());
  });

  it("discards a provider response after the authenticated runtime logs out", async () => {
    let resolveOld!: (value: any) => void;
    mocks.getDependents.mockImplementation(() => new Promise((resolve) => {
      resolveOld = resolve;
    }));
    const view = renderDependents("storage-east", "HDFS", "alice-storage-east");
    await waitFor(() => expect(mocks.getDependents).toHaveBeenCalledTimes(1));

    view.rerender(
      <MemoryRouter>
        <AppContext.Provider value={{ clusterName: "", runtimeKey: "logged-out" } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
          <ServiceDependents serviceName="HDFS" />
        </AppContext.Provider>
      </MemoryRouter>,
    );
    expect(await screen.findByText(/Choose the provider cluster/)).toBeTruthy();
    await act(async () => {
      resolveOld({
        hidden_dependent_count: 0,
        impact_revision: "revision-old",
        items: [{
          binding_id: "binding-old",
          consumer_cluster_id: 61,
          consumer_cluster_name: "stale-consumer",
          consumer_service_name: "HBASE",
          dependency_type: "HDFS",
          state: "READY",
        }],
      });
      await Promise.resolve();
    });

    expect(screen.queryByText(/stale-consumer/)).toBeNull();
  });

  it("retries the selected provider without changing its explicit target", async () => {
    mocks.getDependents
      .mockRejectedValueOnce(new Error("dependent read failed"))
      .mockResolvedValueOnce({
        hidden_dependent_count: 0,
        impact_revision: "revision-a",
        items: [],
      });
    renderDependents("storage-east", "HDFS", "alice-storage-east");

    expect(await screen.findByText("dependent read failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("No managed HBase services use this provider.")).toBeTruthy();
    expect(mocks.getDependents).toHaveBeenNthCalledWith(
      2,
      "storage-east",
      "HDFS",
      expect.any(AbortSignal),
    );
  });

  it("does not claim there are no dependents when every name is hidden", async () => {
    mocks.getDependents.mockResolvedValue({
      hidden_dependent_count: 3,
      impact_revision: "revision-hidden",
      items: [],
    });
    renderDependents("storage-east");

    expect(await screen.findByText(/3 additional dependents are hidden/)).toBeTruthy();
    expect(screen.queryByText("No managed HBase services use this provider.")).toBeNull();
  });
});
