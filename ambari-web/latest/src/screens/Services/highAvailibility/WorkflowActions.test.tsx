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
import { Dropdown } from "react-bootstrap";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ServiceContext } from "../../../store/ServiceContext";

const mocks = vi.hoisted(() => ({
  capabilities: {
    supported: true,
    canAdd: false,
    canRemove: false,
    canActivate: false,
  },
  hdfsCapabilities: {
    nameNodeFederation: true,
    routerFederation: true,
  },
  hawqError: "",
  hawqIsLoading: false,
  retryHawqCapabilities: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock("react-router-dom", async (importOriginal) => ({
  ...(await importOriginal<typeof import("react-router-dom")>()),
  useNavigate: () => mocks.navigate,
}));

vi.mock("./HawqStandby/useHawqStandbyCapabilities", () => ({
  default: () => ({
    capabilities: mocks.capabilities,
    error: mocks.hawqError,
    isLoading: mocks.hawqIsLoading,
    retry: mocks.retryHawqCapabilities,
  }),
}));
vi.mock("./useHdfsWorkflowCapabilities", () => ({
  default: () => ({ capabilities: mocks.hdfsCapabilities }),
}));

import WorkflowActions from "./WorkflowActions";

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  mocks.capabilities = {
    supported: true,
    canAdd: false,
    canRemove: false,
    canActivate: false,
  };
  mocks.hdfsCapabilities = {
    nameNodeFederation: true,
    routerFederation: true,
  };
  mocks.hawqError = "";
  mocks.hawqIsLoading = false;
  mocks.retryHawqCapabilities.mockReset();
});

function renderActions(
  serviceName: string,
  hdfs: Record<string, unknown> = {},
  permissions = {
    canEnableHighAvailability: true,
    canRunHawqCustomCommands: true,
    canPersistWorkflow: true,
  },
) {
  return render(
    <MemoryRouter>
      <ServiceContext.Provider
        value={
          { allServiceModels: { hdfs } } as unknown as ComponentProps<
            typeof ServiceContext.Provider
          >["value"]
        }
      >
        <Dropdown show>
          <Dropdown.Menu>
            <WorkflowActions serviceName={serviceName} {...permissions} />
          </Dropdown.Menu>
        </Dropdown>
      </ServiceContext.Provider>
    </MemoryRouter>,
  );
}

describe("Federation and HAWQ workflow actions", () => {
  it("exposes Router Federation only after multiple namespaces are loaded", () => {
    renderActions("HDFS", {
      federationNamespaces: [{ name: "ns1", hosts: ["nn1", "nn2"] }],
    });
    expect(
      screen.getByText("Add DFSRouter").closest("button,a")?.classList
        .contains("disabled"),
    ).toBe(true);

    cleanup();
    renderActions("HDFS", {
      federationNamespaces: [
        { name: "ns1", hosts: ["nn1", "nn2"] },
        { name: "ns2", hosts: ["nn3", "nn4"] },
      ],
    });
    fireEvent.click(screen.getByText("Add DFSRouter"));
    expect(mocks.navigate).toHaveBeenCalledWith(
      "/main/services/NameNode/federation/routerBasedFederation/step1",
    );
  });

  it("hides Router Federation when the active HDFS stack lacks the capability", () => {
    mocks.hdfsCapabilities.routerFederation = false;
    renderActions("HDFS", {
      federationNamespaces: [
        { name: "ns1", hosts: ["nn1", "nn2"] },
        { name: "ns2", hosts: ["nn3", "nn4"] },
      ],
    });
    expect(screen.queryByText("Add DFSRouter")).toBeNull();
  });

  it("renders only HAWQ operations advertised by stack metadata and topology", () => {
    mocks.capabilities = {
      supported: true,
      canAdd: false,
      canRemove: true,
      canActivate: true,
    };
    renderActions("HAWQ");

    expect(screen.queryByText("Add HAWQ Standby")).toBeNull();
    fireEvent.click(screen.getByText("Remove HAWQ Standby"));
    fireEvent.click(screen.getByText("Activate HAWQ Standby"));
    expect(mocks.navigate).toHaveBeenNthCalledWith(
      1,
      "/main/services/highAvailability/Hawq/remove/step1",
    );
    expect(mocks.navigate).toHaveBeenNthCalledWith(
      2,
      "/main/services/highAvailability/Hawq/activate/step1",
    );
  });

  it("hides all workflow actions without both authorization gates", () => {
    mocks.capabilities = {
      supported: true,
      canAdd: true,
      canRemove: true,
      canActivate: true,
    };
    renderActions("HAWQ", {}, {
      canEnableHighAvailability: true,
      canRunHawqCustomCommands: true,
      canPersistWorkflow: false,
    });
    expect(screen.queryByText(/HAWQ Standby/)).toBeNull();
  });

  it("requires ENABLE_HA only for adding a HAWQ Standby", () => {
    mocks.capabilities = {
      supported: true,
      canAdd: true,
      canRemove: true,
      canActivate: true,
    };
    renderActions("HAWQ", {}, {
      canEnableHighAvailability: true,
      canRunHawqCustomCommands: false,
      canPersistWorkflow: true,
    });

    expect(screen.getByText("Add HAWQ Standby")).toBeDefined();
    expect(screen.queryByText("Remove HAWQ Standby")).toBeNull();
    expect(screen.queryByText("Activate HAWQ Standby")).toBeNull();
  });

  it("allows HAWQ custom-command workflows without ENABLE_HA", () => {
    mocks.capabilities = {
      supported: true,
      canAdd: true,
      canRemove: true,
      canActivate: true,
    };
    renderActions("HAWQ", {}, {
      canEnableHighAvailability: false,
      canRunHawqCustomCommands: true,
      canPersistWorkflow: true,
    });

    expect(screen.queryByText("Add HAWQ Standby")).toBeNull();
    expect(screen.getByText("Remove HAWQ Standby")).toBeDefined();
    expect(screen.getByText("Activate HAWQ Standby")).toBeDefined();
  });

  it("shows capability loading and retry states instead of silently hiding HAWQ actions", () => {
    mocks.hawqIsLoading = true;
    const view = renderActions("HAWQ");
    expect(screen.getByText("Loading HAWQ workflow capabilities...")).toBeTruthy();

    mocks.hawqIsLoading = false;
    mocks.hawqError = "HAWQ stack metadata is unavailable";
    view.rerender(
      <MemoryRouter>
        <ServiceContext.Provider
          value={
            { allServiceModels: { hdfs: {} } } as unknown as ComponentProps<
              typeof ServiceContext.Provider
            >["value"]
          }
        >
          <Dropdown show>
            <Dropdown.Menu>
              <WorkflowActions
                serviceName="HAWQ"
                canEnableHighAvailability
                canRunHawqCustomCommands
                canPersistWorkflow
              />
            </Dropdown.Menu>
          </Dropdown>
        </ServiceContext.Provider>
      </MemoryRouter>,
    );
    expect(screen.getByText("HAWQ stack metadata is unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(mocks.retryHawqCapabilities).toHaveBeenCalledOnce();
  });
});
