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
import { ComponentProps, ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";

const mocks = vi.hoisted(() => ({
  getStackService: vi.fn(),
}));

vi.mock("../../../../api/federationApi", () => ({
  default: { getStackService: mocks.getStackService },
}));

vi.mock("../../../../hooks/useStepWizard", () => ({
  default: () => ({ wizardSteps: {}, activeStep: 0 }),
}));

vi.mock("../../../../components/StepWizard", () => ({
  default: () => <div>HAWQ wizard ready</div>,
}));

vi.mock("../Federation/PersistedWorkflowContext", () => ({
  PersistedWorkflowContext: {},
  PersistedWorkflowProvider: ({ children }: { children: ReactNode }) =>
    children,
}));

import HawqStandbyWizard from "./index";
import { HawqStandbyMode } from "./context";

const stackService = {
  StackServices: { config_types: { "hawq-site": {} } },
  components: [
    {
      StackServiceComponents: {
        component_name: "HAWQMASTER",
        custom_commands: ["REMOVE_HAWQ_STANDBY"],
      },
    },
    {
      StackServiceComponents: {
        component_name: "HAWQSTANDBY",
        custom_commands: ["ACTIVATE_HAWQ_STANDBY"],
      },
    },
  ],
};

function component(
  name: string,
  hostName: string,
  state = "STARTED",
) {
  return {
    ServiceComponentInfo: { component_name: name },
    host_components: [{ HostRoles: { host_name: hostName, state } }],
  };
}

interface WizardFixtureProps {
  components: unknown[];
  hostNames: string[];
  mode: HawqStandbyMode;
  services?: string[];
  stack?: string;
  version?: string;
}

function WizardFixture({
  components,
  hostNames,
  mode,
  services = ["HAWQ"],
  stack = "HDP",
  version = "2.6",
}: WizardFixtureProps) {
  return (
    <AppContext.Provider
      value={
        {
          allHostNames: hostNames,
          cluster: { stack, versionNum: version },
          services: services.map((serviceName) => ({
            ServiceInfo: { service_name: serviceName },
          })),
        } as unknown as ComponentProps<typeof AppContext.Provider>["value"]
      }
    >
      <ServiceContext.Provider
        value={
          { masterSlaveClientsData: components } as unknown as ComponentProps<
            typeof ServiceContext.Provider
          >["value"]
        }
      >
        <HawqStandbyWizard mode={mode} />
      </ServiceContext.Provider>
    </AppContext.Provider>
  );
}

function renderWizard(
  mode: HawqStandbyMode,
  hostNames: string[],
  components: unknown[],
) {
  return render(
    <WizardFixture
      mode={mode}
      hostNames={hostNames}
      components={components}
    />,
  );
}

describe("HAWQ Standby capability boundary", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getStackService.mockResolvedValue(stackService);
  });

  afterEach(cleanup);

  it("blocks Add on a single host", async () => {
    renderWizard("add", ["master"], [component("HAWQMASTER", "master")]);

    expect(
      await screen.findByText(/requires more than one cluster host/i),
    ).toBeTruthy();
    expect(screen.queryByText("HAWQ wizard ready")).toBeNull();
  });

  it("requires a started Master for Remove", async () => {
    renderWizard("remove", ["master", "standby"], [
      component("HAWQMASTER", "master", "INSTALLED"),
      component("HAWQSTANDBY", "standby"),
    ]);

    expect(
      await screen.findByText(/does not expose the remove standby capability/i),
    ).toBeTruthy();
    expect(screen.queryByText("HAWQ wizard ready")).toBeNull();
  });

  it("renders Activate only when stack commands and topology are present", async () => {
    renderWizard("activate", ["master", "standby"], [
      component("HAWQMASTER", "master"),
      component("HAWQSTANDBY", "standby"),
    ]);

    expect(await screen.findByText("HAWQ wizard ready")).toBeTruthy();
  });

  it("ends empty-topology loading with a visible retryable error", async () => {
    renderWizard("add", [], []);

    expect(
      await screen.findByText(/has not loaded the cluster host-component topology/i),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: "Retry" })).toBeTruthy();
    expect(mocks.getStackService).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(
      await screen.findByText(/has not loaded the cluster host-component topology/i),
    ).toBeTruthy();
  });

  it("fails closed and retries a failed stack capability request", async () => {
    mocks.getStackService
      .mockRejectedValueOnce(new Error("stack metadata unavailable"))
      .mockResolvedValueOnce(stackService);
    renderWizard("add", ["master", "standby"], [
      component("HAWQMASTER", "master"),
    ]);

    expect(await screen.findByText("stack metadata unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("HAWQ wizard ready")).toBeTruthy();
    expect(mocks.getStackService).toHaveBeenCalledTimes(2);
  });

  it("refreshes capabilities when the stack version changes", async () => {
    const unsupportedStack = {
      ...stackService,
      StackServices: { config_types: {} },
    };
    mocks.getStackService
      .mockResolvedValueOnce(unsupportedStack)
      .mockResolvedValueOnce(stackService);
    const view = render(
      <WizardFixture
        mode="add"
        hostNames={["master", "standby"]}
        components={[component("HAWQMASTER", "master")]}
        version="2.5"
      />,
    );

    expect(await screen.findByText(/does not expose the HAWQ master/i)).toBeTruthy();
    view.rerender(
      <WizardFixture
        mode="add"
        hostNames={["master", "standby"]}
        components={[component("HAWQMASTER", "master")]}
        version="2.6"
      />,
    );

    expect(await screen.findByText("HAWQ wizard ready")).toBeTruthy();
    expect(mocks.getStackService).toHaveBeenNthCalledWith(1, "HDP", "2.5", "HAWQ");
    expect(mocks.getStackService).toHaveBeenNthCalledWith(2, "HDP", "2.6", "HAWQ");
  });

  it("refreshes capabilities when a component state changes", async () => {
    const view = render(
      <WizardFixture
        mode="remove"
        hostNames={["master", "standby"]}
        components={[
          component("HAWQMASTER", "master", "INSTALLED"),
          component("HAWQSTANDBY", "standby"),
        ]}
      />,
    );

    expect(
      await screen.findByText(/does not expose the remove standby capability/i),
    ).toBeTruthy();
    view.rerender(
      <WizardFixture
        mode="remove"
        hostNames={["master", "standby"]}
        components={[
          component("HAWQMASTER", "master", "STARTED"),
          component("HAWQSTANDBY", "standby"),
        ]}
      />,
    );

    expect(await screen.findByText("HAWQ wizard ready")).toBeTruthy();
    await waitFor(() => expect(mocks.getStackService).toHaveBeenCalledTimes(2));
  });
});
