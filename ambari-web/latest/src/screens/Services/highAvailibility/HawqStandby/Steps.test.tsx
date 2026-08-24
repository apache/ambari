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
import { PersistedWorkflowContext } from "../Federation/PersistedWorkflowContext";
import { HawqStandbyContext, hawqStandbySteps } from "./context";

const mocks = vi.hoisted(() => ({
  getHostComponentsDetails: vi.fn(),
  handleNext: vi.fn(),
  persist: vi.fn(),
  postRecommendations: vi.fn(),
  postValidations: vi.fn(),
  storeStep: vi.fn(),
}));

vi.mock("../../../../api/assignMastersApi", () => ({
  default: {
    postRecommendations: mocks.postRecommendations,
    postValidations: mocks.postValidations,
  },
}));

vi.mock("../../../../api/hostsApi", () => ({
  HostsApi: { getHostComponentsDetails: mocks.getHostComponentsDetails },
}));

vi.mock("../Federation/HostAssignment", () => ({
  default: ({ onChange }: {
    onChange: (
      assignments: unknown[],
      unavailableHosts: string[],
      availableHosts: string[],
    ) => void;
  }) => (
    <button
      type="button"
      onClick={() => onChange(
        [{
          component: "HAWQSTANDBY",
          hostName: "standby.example.com",
          isInstalled: false,
        }],
        [],
        ["master.example.com", "standby.example.com"],
      )}
    >
      Assign Standby
    </button>
  ),
}));

vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext }: {
    isNextEnabled: boolean;
    onNext: () => void;
  }) => (
    <button type="button" disabled={!isNextEnabled} onClick={onNext}>
      NEXT
    </button>
  ),
}));

vi.mock("../../../../components/Modal", () => ({
  default: ({ isOpen, modalBody, onClose, successCallback, options }: {
    isOpen: boolean;
    modalBody: ReactNode;
    onClose: () => void;
    successCallback: () => void;
    options: { okButtonText?: string; cancelButtonText?: string };
  }) => isOpen ? (
    <div>
      {modalBody}
      <button type="button" onClick={onClose}>
        {options.cancelButtonText || "CANCEL"}
      </button>
      <button type="button" onClick={successCallback}>
        {options.okButtonText || "OK"}
      </button>
    </div>
  ) : null,
}));

import { HawqSelectHostStep } from "./Steps";

const currentTopology = {
  items: [
    {
      Hosts: { host_name: "master.example.com" },
      host_components: [
        {
          HostRoles: {
            component_name: "HAWQMASTER",
            host_name: "master.example.com",
          },
        },
        {
          HostRoles: {
            component_name: "DATANODE",
            host_name: "master.example.com",
          },
        },
      ],
    },
    {
      Hosts: { host_name: "standby.example.com" },
      host_components: [
        {
          HostRoles: {
            component_name: "NODEMANAGER",
            host_name: "standby.example.com",
          },
        },
      ],
    },
  ],
};

const recommendations = {
  blueprint: {
    host_groups: [
      {
        name: "host-group-1",
        components: [{ name: "HAWQMASTER" }, { name: "DATANODE" }],
      },
      {
        name: "host-group-2",
        components: [{ name: "NODEMANAGER" }, { name: "HAWQSTANDBY" }],
      },
    ],
  },
  blueprint_cluster_binding: {
    host_groups: [
      {
        name: "host-group-1",
        hosts: [{ fqdn: "master.example.com" }],
      },
      {
        name: "host-group-2",
        hosts: [{ fqdn: "standby.example.com" }],
      },
    ],
  },
};

function renderStep() {
  const appValue = {
    clusterName: "c1",
    cluster: { stack: "HDP", versionNum: "2.6" },
    services: [
      { ServiceInfo: { service_name: "HDFS" } },
      { ServiceInfo: { service_name: "HAWQ" } },
      { ServiceInfo: { service_name: "YARN" } },
    ],
  } as unknown as ComponentProps<typeof AppContext.Provider>["value"];
  const workflowValue = {
    state: { steps: {} },
    storeStep: mocks.storeStep,
    persist: mocks.persist,
    stepWizardUtilities: {
      currentStep: { canGoBack: true },
      handleNextImperitive: mocks.handleNext,
      handleBackImperitive: vi.fn(),
    },
  } as unknown as ComponentProps<typeof PersistedWorkflowContext.Provider>["value"];
  return render(
    <AppContext.Provider value={appValue}>
      <HawqStandbyContext.Provider
        value={{
          mode: "add",
          capabilities: {
            supported: true,
            canAdd: true,
            canRemove: false,
            canActivate: false,
            masterHost: "master.example.com",
          },
        }}
      >
        <PersistedWorkflowContext.Provider value={workflowValue}>
          <HawqSelectHostStep />
        </PersistedWorkflowContext.Provider>
      </HawqStandbyContext.Provider>
    </AppContext.Provider>,
  );
}

async function assignAndValidate() {
  fireEvent.click(screen.getByRole("button", { name: "Assign Standby" }));
  fireEvent.click(screen.getByRole("button", { name: "NEXT" }));
}

describe("HAWQ Add host Advisor validation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getHostComponentsDetails.mockResolvedValue(currentTopology);
    mocks.persist.mockResolvedValue(undefined);
    mocks.postRecommendations.mockResolvedValue({
      resources: [{ recommendations }],
    });
    mocks.postValidations.mockResolvedValue({ resources: [{ items: [] }] });
  });

  afterEach(cleanup);

  it("recommends the complete current mapping before validating the returned blueprint", async () => {
    renderStep();
    await assignAndValidate();

    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledOnce());
    expect(mocks.getHostComponentsDetails).toHaveBeenCalledWith(
      "c1",
      expect.stringContaining("host_components/HostRoles/component_name"),
    );
    expect(mocks.postRecommendations).toHaveBeenCalledWith(
      expect.objectContaining({
        recommend: "host_groups",
        hosts: ["master.example.com", "standby.example.com"],
        services: ["HDFS", "HAWQ", "YARN"],
        recommendations: expect.objectContaining({
          blueprint: {
            host_groups: [
              {
                name: "host-group-1",
                components: [{ name: "HAWQMASTER" }, { name: "DATANODE" }],
              },
              {
                name: "host-group-2",
                components: [{ name: "NODEMANAGER" }, { name: "HAWQSTANDBY" }],
              },
            ],
          },
        }),
      }),
      "HDP",
      "2.6",
    );
    expect(mocks.postRecommendations.mock.calls[0][0]).not.toHaveProperty("validate");
    expect(mocks.postValidations).toHaveBeenCalledWith(
      {
        hosts: ["master.example.com", "standby.example.com"],
        services: ["HDFS", "HAWQ", "YARN"],
        validate: "host_groups",
        recommendations,
      },
      "HDP",
      "2.6",
    );
    expect(mocks.getHostComponentsDetails.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.postRecommendations.mock.invocationCallOrder[0],
    );
    expect(mocks.postRecommendations.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.postValidations.mock.invocationCallOrder[0],
    );
    await waitFor(() => expect(mocks.persist).toHaveBeenCalledWith("next"));
    expect(mocks.handleNext).toHaveBeenCalledOnce();
  });

  it("shows only new-component issues and supports Continue Anyway", async () => {
    mocks.postValidations.mockResolvedValue({
      resources: [{
        items: [
          {
            type: "host-component",
            level: "WARN",
            message: "Installed DataNode warning",
            host: "master.example.com",
            "component-name": "DATANODE",
          },
          {
            type: "host-component",
            level: "ERROR",
            message: "HAWQ Standby conflicts with PostgreSQL",
            host: "standby.example.com",
            "component-name": "HAWQSTANDBY",
          },
        ],
      }],
    });
    renderStep();
    await assignAndValidate();

    expect(await screen.findByText("HAWQ Standby conflicts with PostgreSQL")).toBeTruthy();
    expect(screen.queryByText("Installed DataNode warning")).toBeNull();
    expect(mocks.persist).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Continue Anyway" }));
    await waitFor(() => expect(mocks.persist).toHaveBeenCalledWith("next"));
    expect(mocks.handleNext).toHaveBeenCalledOnce();
  });

  it("fails closed on malformed recommendations and can retry the same assignment", async () => {
    mocks.postRecommendations.mockResolvedValueOnce({
      resources: [{ recommendations: { blueprint: {} } }],
    });
    renderStep();
    await assignAndValidate();

    expect(await screen.findByText(/incomplete host-group recommendation/i)).toBeTruthy();
    expect(mocks.postValidations).not.toHaveBeenCalled();
    expect(mocks.persist).not.toHaveBeenCalled();

    mocks.postRecommendations.mockResolvedValueOnce({
      resources: [{ recommendations }],
    });
    fireEvent.click(screen.getByRole("button", { name: "NEXT" }));
    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledOnce());
    await waitFor(() => expect(mocks.persist).toHaveBeenCalledWith("next"));
  });

  it("rejects a structurally valid recommendation that drops current topology", async () => {
    mocks.postRecommendations.mockResolvedValue({
      resources: [{
        recommendations: {
          ...recommendations,
          blueprint: {
            host_groups: [
              {
                name: "host-group-1",
                components: [{ name: "HAWQMASTER" }],
              },
              recommendations.blueprint.host_groups[1],
            ],
          },
        },
      }],
    });
    renderStep();
    await assignAndValidate();

    expect(
      await screen.findByText(/did not preserve the complete current component mapping/i),
    ).toBeTruthy();
    expect(mocks.postValidations).not.toHaveBeenCalled();
    expect(mocks.persist).not.toHaveBeenCalled();
  });

  it("rejects a restored selection whose host disappeared", async () => {
    mocks.getHostComponentsDetails.mockResolvedValue({
      items: [currentTopology.items[0]],
    });
    renderStep();
    await assignAndValidate();

    expect(
      await screen.findByText(/standby\.example\.com is no longer available/i),
    ).toBeTruthy();
    expect(mocks.postRecommendations).not.toHaveBeenCalled();
    expect(mocks.postValidations).not.toHaveBeenCalled();
  });

  it("does not start concurrent validation requests", async () => {
    let resolveTopology: (value: unknown) => void = () => undefined;
    mocks.getHostComponentsDetails.mockReturnValue(
      new Promise((resolve) => {
        resolveTopology = resolve;
      }),
    );
    renderStep();
    fireEvent.click(screen.getByRole("button", { name: "Assign Standby" }));
    const next = screen.getByRole("button", { name: "NEXT" });
    fireEvent.click(next);
    fireEvent.click(next);

    expect(mocks.getHostComponentsDetails).toHaveBeenCalledOnce();
    resolveTopology(currentTopology);
    await waitFor(() => expect(mocks.postValidations).toHaveBeenCalledOnce());
    await waitFor(() => expect(mocks.persist).toHaveBeenCalledWith("next"));
  });

  it("does not advance when the validation response is malformed", async () => {
    mocks.postValidations.mockResolvedValue({ resources: [{}] });
    renderStep();
    await assignAndValidate();

    expect(await screen.findByText(/invalid host validation response/i)).toBeTruthy();
    expect(mocks.persist).not.toHaveBeenCalled();
    expect(mocks.handleNext).not.toHaveBeenCalled();
  });
});

describe("HAWQ Advisor response guards", () => {
  it("records the selected host assignment in the persisted step before validation", async () => {
    renderStep();
    fireEvent.click(screen.getByRole("button", { name: "Assign Standby" }));

    expect(mocks.storeStep).toHaveBeenCalledWith(
      hawqStandbySteps.SELECT_HOST,
      expect.objectContaining({
        assignments: [expect.objectContaining({
          component: "HAWQSTANDBY",
          hostName: "standby.example.com",
        })],
      }),
    );
  });
});
