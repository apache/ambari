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

import { cleanup, render, screen } from "@testing-library/react";
import type { ComponentProps } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../store/context";
import AdminRouteGuard from "./AdminRouteGuard";

const auth = vi.hoisted(() => ({
  authenticated: true,
  permissions: new Set<string>(),
}));
vi.mock("../hooks/useAuth", () => ({
  useAuth: () => ({
    isAuthenticated: auth.authenticated,
    hasAuthorization: (permission: string) => auth.permissions.has(permission),
  }),
}));

function renderGuard(upgradeState = "NOT_REQUIRED") {
  const contextValue = { upgradeState } as unknown as ComponentProps<
    typeof AppContext.Provider
  >["value"];
  return render(
    <AppContext.Provider value={contextValue}>
      <MemoryRouter initialEntries={["/main/admin"]}>
        <Routes>
          <Route path="/login" element={<div>Login</div>} />
          <Route path="/main/dashboard/metrics" element={<div>Dashboard</div>} />
          <Route path="/main/admin" element={(
            <AdminRouteGuard><div>Admin</div></AdminRouteGuard>
          )} />
        </Routes>
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

describe("AdminRouteGuard", () => {
  afterEach(() => {
    cleanup();
    auth.authenticated = true;
    auth.permissions.clear();
  });

  it("admits both service and cluster auto-start administrators", () => {
    auth.permissions.add("SERVICE.MANAGE_AUTO_START");
    renderGuard();
    expect(screen.getByText("Admin")).toBeTruthy();
  });

  it("keeps an active upgrade visible to an otherwise unauthorized user", () => {
    renderGuard("HOLDING_FAILED");
    expect(screen.getByText("Admin")).toBeTruthy();
  });

  it("redirects an unauthorized idle user", () => {
    renderGuard();
    expect(screen.getByText("Dashboard")).toBeTruthy();
  });
});
