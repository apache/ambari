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

import { render, waitFor } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ClusterApi from "./api/clusterApi";
import { RouteTracker } from "./AppLoader";
import { AppContext } from "./store/context";

const policy = vi.hoisted(() => ({ canPersistRoute: false }));

vi.mock("./api/clusterApi", () => ({
  default: {
    noopPolling: vi.fn(),
    postPersistData: vi.fn(),
  },
}));
vi.mock("./hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: () => true }),
}));
vi.mock("./hooks/useAuthorizationPolicy", () => ({
  default: () => ({ isAuthorized: () => policy.canPersistRoute }),
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

describe("RouteTracker persistence authorization", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    policy.canPersistRoute = false;
  });

  it("does not persist a protected workflow route without mutation access", async () => {
    renderTracker();
    await waitFor(() => expect(ClusterApi.postPersistData).not.toHaveBeenCalled());
  });

  it("persists a protected workflow route for its authorized owner", async () => {
    policy.canPersistRoute = true;
    renderTracker();
    await waitFor(() => expect(ClusterApi.postPersistData).toHaveBeenCalledWith({
      USER_REDIRECTION_URL: "/main/services/highAvailability/NameNode/enable/step2",
    }));
  });
});
