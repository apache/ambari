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
vi.mock("../../../../hooks/useStepWizard", () => ({
  default: () => ({ currentStep: { name: "GET_STARTED" } }),
}));
vi.mock("../../../../components/StepWizard", () => ({
  default: () => <div>Router wizard ready</div>,
}));
vi.mock("../../../../components/Spinner", () => ({
  default: () => <div>Loading Router capability</div>,
}));
vi.mock("../Federation/PersistedWorkflowContext", () => ({
  PersistedWorkflowContext: {},
  PersistedWorkflowProvider: ({ children }: { children: React.ReactNode }) =>
    children,
}));

import RouterFederationWizard from "./index";

const startedComponent = (name: string) => ({
  ServiceComponentInfo: {
    component_name: name,
    total_count: 3,
    started_count: 3,
  },
  host_components: [],
});

function renderWizard(
  serviceContext: Record<string, unknown> = {},
) {
  return render(
    <AppContext.Provider
      value={
        { cluster: { stack: "BIGTOP", versionNum: "3.2.0" } } as unknown as ComponentProps<
          typeof AppContext.Provider
        >["value"]
      }
    >
      <ServiceContext.Provider
        value={
          {
            allModelsLoaded: true,
            allServiceModels: {
              hdfs: {
                isNamespaceLoaded: true,
                federationNamespaces: [
                  { name: "ns1", hosts: ["nn1", "nn2"] },
                  { name: "ns2", hosts: ["nn3", "nn4"] },
                ],
              },
            },
            serviceModels: { hdfs: { serviceComponents: [] } },
            masterSlaveClientsData: {
              zk: startedComponent("ZOOKEEPER_SERVER"),
              jn: startedComponent("JOURNALNODE"),
            },
            ...serviceContext,
          } as unknown as ComponentProps<typeof ServiceContext.Provider>["value"]
        }
      >
        <RouterFederationWizard />
      </ServiceContext.Provider>
    </AppContext.Provider>,
  );
}

describe("Router Federation stack gate", () => {
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

  it("allows the first Router install when stack metadata supports ROUTER", () => {
    renderWizard();
    expect(screen.getByText("Router wizard ready")).toBeTruthy();
  });

  it("shows a retryable stack metadata failure", () => {
    mocks.capabilityState.error = "HDFS stack read failed";
    renderWizard();
    expect(screen.getByText("HDFS stack read failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(mocks.capabilityState.retry).toHaveBeenCalledOnce();
  });

  it("fails closed when the active stack lacks Router support", () => {
    mocks.capabilityState.capabilities.routerFederation = false;
    renderWizard();
    expect(screen.getByText(/requires a stack with the ROUTER component/)).toBeTruthy();
    expect(screen.queryByText("Router wizard ready")).toBeNull();
  });

  it("settles an empty loaded topology with a retryable error", () => {
    renderWizard({ allModelsLoaded: true, masterSlaveClientsData: {} });

    expect(screen.getByText(/loaded no host-component topology/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(mocks.capabilityState.retry).toHaveBeenCalledOnce();
    expect(screen.queryByText("Loading Router capability")).toBeNull();
  });
});
