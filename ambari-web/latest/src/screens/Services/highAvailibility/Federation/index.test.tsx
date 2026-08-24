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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { ComponentProps } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";

const mocks = vi.hoisted(() => ({
  capabilityState: {
    capabilities: { nameNodeFederation: true, routerFederation: true },
    error: "",
    isLoading: false,
    retry: vi.fn(),
  },
}));

vi.mock("../useHdfsWorkflowCapabilities", () => ({
  default: () => mocks.capabilityState,
}));
vi.mock("./validateEnablement", () => ({
  default: () => <div>NameNode Federation validation ready</div>,
}));
vi.mock("../../../../components/Spinner", () => ({
  default: () => <div>Loading HDFS stack capability</div>,
}));

import EnableNamenodeFederation from "./index";

function providers(children: React.ReactNode) {
  return (
    <AppContext.Provider
      value={
        {
          allHostNames: ["h1", "h2", "h3", "h4"],
          cluster: { stack: "BIGTOP", versionNum: "3.2.0" },
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      <ServiceContext.Provider
        value={
          {
            allServiceModels: { hdfs: { isNameNodeHaEnabled: true } },
          } as unknown as ComponentProps<typeof ServiceContext.Provider>["value"]
        }
      >
        {children}
      </ServiceContext.Provider>
    </AppContext.Provider>
  );
}

function renderMenuEntry() {
  return render(providers(<MemoryRouter><EnableNamenodeFederation /></MemoryRouter>));
}

function renderDirectEntry() {
  return render(
    providers(
      <MemoryRouter initialEntries={["/main/services/NameNode/federation/step1"]}>
        <Routes>
          <Route
            path="/main/services/:componentName/federation/:stepNumber"
            element={<EnableNamenodeFederation isMappingOnly />}
          />
        </Routes>
      </MemoryRouter>,
    ),
  );
}

describe("NameNode Federation stack gate", () => {
  beforeEach(() => {
    mocks.capabilityState.capabilities = {
      nameNodeFederation: true,
      routerFederation: true,
    };
    mocks.capabilityState.error = "";
    mocks.capabilityState.isLoading = false;
    mocks.capabilityState.retry.mockReset();
  });
  afterEach(cleanup);

  it("hides the menu action and rejects direct navigation when unsupported", () => {
    mocks.capabilityState.capabilities.nameNodeFederation = false;
    renderMenuEntry();
    expect(screen.queryByText("Add New HDFS Namespace")).toBeNull();

    cleanup();
    renderDirectEntry();
    expect(screen.getByText(/not supported by the active HDFS stack/)).toBeTruthy();
  });

  it("exposes loading and retryable error states at the direct-route boundary", () => {
    mocks.capabilityState.isLoading = true;
    renderDirectEntry();
    expect(screen.getByText("Loading HDFS stack capability")).toBeTruthy();

    cleanup();
    mocks.capabilityState.isLoading = false;
    mocks.capabilityState.error = "HDFS capability failed";
    renderDirectEntry();
    fireEvent.click(screen.getByRole("button", { name: /Retry/ }));
    expect(mocks.capabilityState.retry).toHaveBeenCalledOnce();
  });

  it("enters validation only after the stack capability is confirmed", async () => {
    renderDirectEntry();
    expect(
      await screen.findByText("NameNode Federation validation ready"),
    ).toBeTruthy();
  });
});
