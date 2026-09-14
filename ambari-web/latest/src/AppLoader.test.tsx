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
import type { ComponentProps } from "react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { recalledClusterId, rememberCluster, savePreferredPath } from "./Utils/authNavigation";
import ClusterApi from "./api/clusterApi";
import { LandingRoute, LegacyMainRedirect, RouteTracker } from "./AppLoader";
import { AppContext } from "./store/context";

afterEach(cleanup);

vi.mock("./api/clusterApi", () => ({
  default: {
    noopPolling: vi.fn(),
    postPersistData: vi.fn(),
  },
}));
vi.mock("./hooks/useAuth", () => ({
  useAuth: () => ({
    user: { user_name: "navigation-user" },
    authorizations: [{ authorization_id: "AMBARI.RENAME_CLUSTER" }],
    canAccessCluster: (name: string) => name !== "forbidden",
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

function renderLegacyRedirect(clusters: string[], entry = "/main/hosts?page=2") {
  const value = {
    availableClusters: clusters.map((cluster_name, index) => ({
      Clusters: { cluster_id: index + 1, cluster_name },
    })),
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  return render(
    <AppContext.Provider value={value}>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/" element={<LandingRoute />} />
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
  beforeEach(() => { localStorage.clear(); sessionStorage.clear(); });
  it("preserves the suffix and query for exactly one authorized cluster", async () => {
    renderLegacyRedirect(["east / prod"]);
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters/east%20%2F%20prod/main/hosts?page=2");
  });

  it("requires a chooser when more than one cluster is available", async () => {
    renderLegacyRedirect(["alpha", "beta"]);
    expect(await screen.findByRole("dialog")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "beta" }));
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters/beta/main/hosts?page=2");
    expect(recalledClusterId("navigation-user")).toBe(2);
  });
  it("restores the last authorized cluster dashboard on direct login", async () => {
    rememberCluster("navigation-user", 2);
    renderLegacyRedirect(["alpha", "beta"], "/");
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters/beta/main/dashboard/metrics");
  });

  it("returns to the exact interrupted cluster route before using a preference", async () => {
    rememberCluster("navigation-user", 1);
    savePreferredPath("/clusters/beta/main/hosts?page=3");
    renderLegacyRedirect(["alpha", "beta"], "/");
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters/beta/main/hosts?page=3");
  });

  it("resolves a remembered numeric identity after a cluster rename", async () => {
    rememberCluster("navigation-user", 2);
    renderLegacyRedirect(["alpha", "renamed / cluster"]);
    expect((await screen.findByTestId("location")).textContent)
      .toBe("/clusters/renamed%20%2F%20cluster/main/hosts?page=2");
  });

  it("does not restore another user's preference or a revoked cluster", async () => {
    rememberCluster("another-user", 1);
    rememberCluster("navigation-user", 3);
    savePreferredPath("/clusters/forbidden/main/hosts");
    renderLegacyRedirect(["alpha", "beta", "forbidden"], "/");
    expect(await screen.findByRole("dialog")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "forbidden" })).toBeNull();
  });

});
