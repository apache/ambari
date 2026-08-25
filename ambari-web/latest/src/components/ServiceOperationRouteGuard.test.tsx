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
import { ComponentProps } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it } from "vitest";
import { AppContext } from "../store/context";
import ServiceOperationRouteGuard from "./ServiceOperationRouteGuard";

const renderGuard = ({
  isNonWizardUser = false,
  operationsAllowed = false,
  upgradeIsRunning = false,
  upgradeSuspended = false,
} = {}) =>
  render(
    <AppContext.Provider
      value={
        {
          isNonWizardUser,
          supports: { opsDuringRollingUpgrade: operationsAllowed },
          upgradeIsRunning,
          upgradeSuspended,
        } as unknown as ComponentProps<
          typeof AppContext.Provider
        >["value"]
      }
    >
      <MemoryRouter initialEntries={["/operation"]}>
        <Routes>
          <Route
            path="/operation"
            element={
              <ServiceOperationRouteGuard>
                <div>Operation Wizard</div>
              </ServiceOperationRouteGuard>
            }
          />
          <Route
            path="/main/dashboard/metrics"
            element={<div>Cluster Dashboard</div>}
          />
        </Routes>
      </MemoryRouter>
    </AppContext.Provider>
  );

describe("service operation route guard", () => {
  afterEach(cleanup);

  it("allows the route when no upgrade or other user's wizard is active", () => {
    renderGuard();
    expect(screen.getByText("Operation Wizard")).toBeTruthy();
  });

  it("redirects a conflicting wizard route", () => {
    renderGuard({ isNonWizardUser: true });
    expect(screen.getByText("Cluster Dashboard")).toBeTruthy();
  });

  it("blocks an active upgrade unless the support flag permits operations", () => {
    const blocked = renderGuard({ upgradeIsRunning: true });
    expect(screen.getByText("Cluster Dashboard")).toBeTruthy();
    blocked.unmount();

    renderGuard({ operationsAllowed: true, upgradeIsRunning: true });
    expect(screen.getByText("Operation Wizard")).toBeTruthy();
  });

  it("allows operations while an upgrade is suspended", () => {
    renderGuard({ upgradeIsRunning: true, upgradeSuspended: true });
    expect(screen.getByText("Operation Wizard")).toBeTruthy();
  });
});
