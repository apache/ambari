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
import { afterEach, describe, expect, it } from "vitest";
import { AppContext } from "../store/context";
import UpgradeGuard from "./UpgradeGuard";

function renderGuard(options: {
  running: boolean;
  suspended?: boolean;
  operationsAllowed?: boolean;
}) {
  const contextValue = {
    upgradeIsRunning: options.running,
    upgradeSuspended: options.suspended || false,
    supports: { opsDuringRollingUpgrade: options.operationsAllowed || false },
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  return render(
    <AppContext.Provider value={contextValue}>
      <MemoryRouter initialEntries={["/settings"]}>
        <Routes>
          <Route path="/main/dashboard" element={<div>Dashboard</div>} />
          <Route path="/settings" element={(
            <UpgradeGuard><div>Settings</div></UpgradeGuard>
          )} />
        </Routes>
      </MemoryRouter>
    </AppContext.Provider>,
  );
}

describe("UpgradeGuard", () => {
  afterEach(cleanup);

  it("blocks ordinary operations during a running upgrade", () => {
    renderGuard({ running: true });
    expect(screen.getByText("Dashboard")).toBeTruthy();
  });

  it("allows suspended upgrades and the rolling-operations feature flag", () => {
    const first = renderGuard({ running: true, suspended: true });
    expect(screen.getByText("Settings")).toBeTruthy();
    first.unmount();

    renderGuard({ running: true, operationsAllowed: true });
    expect(screen.getByText("Settings")).toBeTruthy();
  });
});
