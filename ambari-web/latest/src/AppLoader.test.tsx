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

import { render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ClusterApi from "./api/clusterApi";
import { LegacyMainRedirect, RouteTracker } from "./AppLoader";
import { AppContext } from "./store/context";

vi.mock("./api/clusterApi", () => ({
  default: {
    noopPolling: vi.fn(),
    postPersistData: vi.fn(),
  },
}));
vi.mock("./hooks/useAuth", () => ({
  useAuth: () => ({
    canAccessCluster: () => true,
    hasAuthorization: () => true,
    hasGlobalAuthorization: () => true,
  }),
}));

function renderTracker() {
  const value = {
    cluster: { cluster_name: "c1" },
    isClusterInstalled: true,
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  return render(
    <AppContext.Provider value={value}>
      <MemoryRouter initialEntries={["/main/services/highAvailability/NameNode/enable/step2"]}>
        <RouteTracker />
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}{location.search}</div>;
}

function renderLegacyRedirect(clusters: string[]) {
  const value = {
    availableClusters: clusters.map((cluster_name, index) => ({
      Clusters: { cluster_id: index + 1, cluster_name },
    })),
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  return render(
    <AppContext.Provider value={value}>
      <MemoryRouter initialEntries={["/main/hosts?page=2"]}>
        <Routes>
          <Route path="/main/*" element={<LegacyMainRedirect />} />
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

describe("RouteTracker preferred path", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("does not write a cluster workflow route to global server persistence", async () => {
    renderTracker();
    await waitFor(() => expect(ClusterApi.postPersistData).not.toHaveBeenCalled());
  });
});

describe("legacy main route selection", () => {
  it("preserves the suffix and query for exactly one authorized cluster", async () => {
    renderLegacyRedirect(["east / prod"]);
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters/east%20%2F%20prod/main/hosts?page=2");
  });

  it("requires a chooser when more than one cluster is available", async () => {
    renderLegacyRedirect(["alpha", "beta"]);
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters?continue=%2Fmain%2Fhosts%3Fpage%3D2");
  });
});
