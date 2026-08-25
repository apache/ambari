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

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { PersistedWorkflowContext } from "../Federation/PersistedWorkflowContext";

interface CapturedOperation {
  id: string;
  callback: () => Promise<unknown>;
}

const mocks = vi.hoisted(() => ({
  loadCurrentConfigurations: vi.fn(),
  saveConfigurationTypes: vi.fn(),
  createInstallComponentTask: vi.fn(),
  updateComponent: vi.fn(),
  operations: [] as CapturedOperation[],
  getKDCSessionState: vi.fn(),
}));

vi.mock("../../../../Utils/taskUtils", () => ({
  createInstallComponentTask: mocks.createInstallComponentTask,
  updateComponent: mocks.updateComponent,
}));
vi.mock("../../../../hooks/useKDCSessionState", () => ({
  default: () => ({ getKDCSessionState: mocks.getKDCSessionState }),
}));
vi.mock("../../../../components/OperationsProgress", () => ({
  default: ({ operations }: { operations: CapturedOperation[] }) => {
    mocks.operations = operations;
    return <div>Router operations ready</div>;
  },
}));

vi.mock("../../../../api/federationApi", () => ({
  default: {
    loadCurrentConfigurations: mocks.loadCurrentConfigurations,
    saveConfigurationTypes: mocks.saveConfigurationTypes,
  },
}));

vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext }: {
    isNextEnabled: boolean;
    onNext: () => void;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>Next</button>
  ),
}));

import { RouterStep3, RouterStep4 } from "./Steps";

const snapshot = {
  items: [
    {
      type: "hdfs-site",
      properties: { "dfs.nameservices": "ns1,ns2" },
    },
    {
      type: "core-site",
      properties: { "ha.zookeeper.quorum": "zk1:2181,zk2:2181,zk3:2181" },
    },
  ],
};

describe("Router Federation Review", () => {
  const storeStep = vi.fn();
  const persist = vi.fn();
  const handleNextImperitive = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mocks.loadCurrentConfigurations.mockResolvedValue(snapshot);
    persist.mockResolvedValue(undefined);
    handleNextImperitive.mockResolvedValue(undefined);
  });

  afterEach(cleanup);

  function renderStep() {
    return render(
      <AppContext.Provider
        value={
          { clusterName: "c1" } as unknown as ComponentProps<
            typeof AppContext.Provider
          >["value"]
        }
      >
        <ServiceContext.Provider
          value={
            {
              allServiceModels: {
                hdfs: {
                  federationNamespaces: [
                    { name: "ns1", hosts: ["nn1", "nn2"] },
                    { name: "ns2", hosts: ["nn3", "nn4"] },
                  ],
                },
              },
            } as unknown as ComponentProps<
              typeof ServiceContext.Provider
            >["value"]
          }
        >
          <PersistedWorkflowContext.Provider
            value={{
              state: { activeStep: "REVIEW", steps: {} },
              storeStep,
              persist,
              stepWizardUtilities: {
                currentStep: { name: "REVIEW", canGoBack: true },
                handleNextImperitive,
                handleBackImperitive: vi.fn(),
              },
            }}
          >
            <RouterStep3 />
          </PersistedWorkflowContext.Provider>
        </ServiceContext.Provider>
      </AppContext.Provider>,
    );
  }

  it("initializes a missing hdfs-rbf-site and does not advance after a failed save", async () => {
    mocks.saveConfigurationTypes.mockRejectedValueOnce(new Error("save failed"));
    renderStep();

    expect(await screen.findByText("Review")).toBeTruthy();
    expect(screen.getByText("ns1.nn1,ns1.nn2,ns2.nn3,ns2.nn4")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("save failed")).toBeTruthy();
    expect(storeStep).not.toHaveBeenCalled();
    expect(persist).not.toHaveBeenCalled();
    expect(handleNextImperitive).not.toHaveBeenCalled();

    mocks.saveConfigurationTypes.mockResolvedValueOnce({ status: 200 });
    fireEvent.click(screen.getByRole("button", { name: "Next" }));

    await waitFor(() => expect(handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.saveConfigurationTypes).toHaveBeenLastCalledWith(
      "c1",
      expect.objectContaining({
        items: expect.arrayContaining([
          expect.objectContaining({ type: "hdfs-rbf-site" }),
        ]),
      }),
      ["hdfs-rbf-site"],
      expect.any(String),
      false,
    );
    expect(storeStep).toHaveBeenCalledWith(
      "REVIEW",
      expect.objectContaining({ configSnapshot: expect.any(Object) }),
    );
    expect(persist).toHaveBeenCalledWith("next");
  });
});

describe("Router Federation configure order", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.operations = [];
    mocks.createInstallComponentTask.mockResolvedValue({ status: 202 });
    mocks.updateComponent.mockResolvedValue({ status: 202 });
  });

  afterEach(cleanup);

  it("reconciles every selected Router to INSTALLED before STARTED", async () => {
    const routerHosts = [
      "router-existing",
      "router-new",
      "router-maintenance",
    ];
    render(
      <AppContext.Provider
        value={
          { clusterName: "c1" } as unknown as ComponentProps<
            typeof AppContext.Provider
          >["value"]
        }
      >
        <ServiceContext.Provider
          value={
            { serviceModels: { hdfs: {} } } as unknown as ComponentProps<
              typeof ServiceContext.Provider
            >["value"]
          }
        >
          <PersistedWorkflowContext.Provider
            value={{
              state: {
                activeStep: "CONFIGURE_ROUTER",
                steps: {
                  SELECT_HOSTS: {
                    assignments: routerHosts.map((hostName, index) => ({
                      component: "ROUTER",
                      hostName,
                      isInstalled: index !== 1,
                    })),
                  },
                },
              },
              storeStep: vi.fn(),
              persist: vi.fn(),
              stepWizardUtilities: {
                currentStep: { name: "CONFIGURE_ROUTER", canGoBack: false },
              },
            }}
          >
            <RouterStep4 />
          </PersistedWorkflowContext.Provider>
        </ServiceContext.Provider>
      </AppContext.Provider>,
    );

    expect(mocks.operations.map((operation) => operation.id)).toEqual([
      "installRouter",
      "startRouters",
    ]);
    await act(async () => {
      await mocks.operations[0].callback();
      await mocks.operations[1].callback();
    });
    expect(mocks.createInstallComponentTask).toHaveBeenCalledWith(
      "ROUTER",
      routerHosts,
      "HDFS",
      "c1",
      ["HDFS"],
      {},
      mocks.getKDCSessionState,
      { reconcileHosts: true },
    );
    expect(mocks.updateComponent).toHaveBeenCalledWith(
      "c1",
      "ROUTER",
      routerHosts,
      "HDFS",
      "Start",
      1,
    );
    expect(mocks.createInstallComponentTask.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.updateComponent.mock.invocationCallOrder[0],
    );
  });
});
