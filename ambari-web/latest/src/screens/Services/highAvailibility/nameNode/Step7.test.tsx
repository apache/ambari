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
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { EnableHighAvailibilityContext } from "./store/context";

const mocks = vi.hoisted(() => ({
  getClusterComponents: vi.fn(),
}));

vi.mock("../../../../api/hostsApi", () => ({
  HostsApi: { getClusterComponents: mocks.getClusterComponents },
}));
vi.mock("../../../../Utils/taskUtils", () => ({
  startServices: vi.fn(),
  updateComponent: vi.fn(),
}));
vi.mock("../../../../components/OperationsProgress", () => ({
  default: () => <div>Operations ready</div>,
}));
vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: () => null,
}));

import Step7 from "./Step7";

const wizardState = {
  enableHighAvailibilitySteps: {
    SELECT_HOSTS: {
      data: {
        masterComponentHosts: [
          { component: "NAMENODE", hostName: "nn1", isInstalled: true },
        ],
      },
    },
  },
};

function renderStep() {
  return render(
    <AppContext.Provider
      value={{ clusterName: "c1", services: [] } as never}
    >
      <EnableHighAvailibilityContext.Provider
        value={
          {
            state: wizardState,
            dispatch: vi.fn(),
            flushStateToDb: vi.fn(),
            stepWizardUtilities: {
              currentStep: { name: "START_COMPONENTS" },
              handleNextImperitive: vi.fn(),
              jumpToStep: vi.fn(),
            },
          } as never
        }
      >
        <Step7 />
      </EnableHighAvailibilityContext.Provider>
    </AppContext.Provider>,
  );
}

describe("NameNode HA start-components topology", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("shows a retryable error when the authoritative topology load fails", async () => {
    mocks.getClusterComponents
      .mockRejectedValueOnce(new Error("topology unavailable"))
      .mockResolvedValue({
        items: [
          {
            ServiceComponentInfo: {
              component_name: "ZOOKEEPER_SERVER",
              service_name: "ZOOKEEPER",
              installed_count: 3,
            },
            host_components: [
              { HostRoles: { host_name: "zk1" } },
              { HostRoles: { host_name: "zk2" } },
              { HostRoles: { host_name: "zk3" } },
            ],
          },
        ],
      });

    renderStep();

    expect(await screen.findByText("topology unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(await screen.findByText("Operations ready")).toBeTruthy();
    expect(mocks.getClusterComponents).toHaveBeenCalledTimes(2);
  });
});
