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
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("./highAvailibility/Federation", () => ({
  default: () => <div data-testid="namenode-federation" />,
}));
vi.mock("./highAvailibility/RouterFederation", () => ({
  default: () => <div data-testid="router-federation" />,
}));
vi.mock("./highAvailibility/HawqStandby", () => ({
  default: ({ mode }: { mode: string }) => (
    <div data-testid="hawq-standby">{mode}</div>
  ),
}));
vi.mock("./highAvailibility/nameNode", () => ({ default: () => null }));
vi.mock("./highAvailibility/journalNode", () => ({ default: () => null }));
vi.mock("./highAvailibility/rangerAdmin", () => ({ default: () => null }));
vi.mock("./highAvailibility/resourceManager", () => ({ default: () => null }));
vi.mock("./reassign", () => ({ default: () => null }));

import ServiceActionsUrlMapping from "./ServiceActionsUrlMapping";

afterEach(cleanup);

function renderMapping(path: string, routePath: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path={routePath}
          element={<ServiceActionsUrlMapping serviceName="HDFS" />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("service workflow URL mapping", () => {
  it("maps the more specific Router Federation deep link", () => {
    renderMapping(
      "/main/services/NameNode/federation/routerBasedFederation/step1",
      "/main/services/:componentName/federation/routerBasedFederation/:stepNumber",
    );
    expect(screen.getByTestId("router-federation")).toBeTruthy();
    expect(screen.queryByTestId("namenode-federation")).toBeNull();
  });

  it("retains the NameNode Federation deep link", () => {
    renderMapping(
      "/main/services/NameNode/federation/step2",
      "/main/services/:componentName/federation/:stepNumber",
    );
    expect(screen.getByTestId("namenode-federation")).toBeTruthy();
  });

  it.each(["add", "remove", "activate"])(
    "maps the HAWQ %s deep link to its exact mode",
    (mode) => {
      renderMapping(
        `/main/services/highAvailability/Hawq/${mode}/step1`,
        "/main/services/highAvailability/:componentName/:mode/:stepNumber",
      );
      expect(screen.getByTestId("hawq-standby").textContent).toBe(mode);
    },
  );
});
