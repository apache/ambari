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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ServiceContext } from "../../../../store/ServiceContext";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { RangerAdminHaOperation } from "./rangerAdminHaWorkflow";
import { State } from "./store/types";

const mocks = vi.hoisted(() => ({
  stopAllServices: vi.fn(),
  createInstallComponentTask: vi.fn(),
  reconfigureRangerAdminServices: vi.fn(),
  startAllServices: vi.fn(),
  getKDCSessionState: vi.fn(),
  progressProps: vi.fn(),
  footerProps: vi.fn(),
  flushStateToDb: vi.fn(),
}));

vi.mock("../../../../Utils/taskUtils", () => ({
  stopAllServices: mocks.stopAllServices,
  createInstallComponentTask: mocks.createInstallComponentTask,
  startAllServices: mocks.startAllServices,
}));
vi.mock("./rangerAdminHaApi", () => ({
  reconfigureRangerAdminServices: mocks.reconfigureRangerAdminServices,
}));
vi.mock("../../../../hooks/useKDCSessionState", () => ({
  default: () => ({ getKDCSessionState: mocks.getKDCSessionState }),
}));
vi.mock("../../../../components/OperationsProgress", () => ({
  default: (props: { operations: RangerAdminHaOperation[] }) => {
    mocks.progressProps(props);
    return <div>Operations ready</div>;
  },
}));
vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: (props: { isNextEnabled: boolean; onNext: () => void }) => {
    mocks.footerProps(props);
    return (
      <button disabled={!props.isNextEnabled} onClick={() => props.onNext()}>
        Complete
      </button>
    );
  },
}));

import Step4 from "./Step4";

const validState: State = {
  enableHighAvailibilityRangerAdminSteps: {
    GET_STARTED: {
      data: { loadBalancerUrl: "https://lb.example.com/ranger" },
    },
    SELECT_HOSTS: {
      data: {
        masterComponentHosts: [
          {
            component: "RANGER_ADMIN",
            hostName: "ra1.example.com",
            isInstalled: true,
          },
          {
            component: "RANGER_ADMIN",
            hostName: "ra2.example.com",
            isInstalled: false,
          },
          {
            component: "RANGER_ADMIN",
            hostName: "ra3.example.com",
            isInstalled: false,
          },
        ],
      },
    },
  },
};

function renderStep(state: State = validState) {
  const rangerModel = { serviceName: "ranger" };
  render(
    <AppContext.Provider
      value={
        {
          clusterName: "c1",
          services: [
            { ServiceInfo: { service_name: "RANGER" } },
            { ServiceInfo: { service_name: "HDFS" } },
          ],
          ambariProperties: { "skip.service.checks": "true" },
        } as never
      }
    >
      <ServiceContext.Provider
        value={{ allServiceModels: { ranger: rangerModel } } as never}
      >
        <EnableHighAvailibilityRangerAdminContext.Provider
          value={
            {
              state,
              dispatch: vi.fn(),
              flushStateToDb: mocks.flushStateToDb,
              stepWizardUtilities: {
                currentStep: { name: "INSTALL_START_TEST" },
              },
            } as never
          }
        >
          <Step4 />
        </EnableHighAvailibilityRangerAdminContext.Provider>
      </ServiceContext.Provider>
    </AppContext.Provider>,
  );
  return rangerModel;
}

describe("Ranger Admin HA execution step", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.stopAllServices.mockResolvedValue({ Requests: { id: 1 } });
    mocks.createInstallComponentTask.mockResolvedValue({ Requests: { id: 2 } });
    mocks.reconfigureRangerAdminServices.mockResolvedValue({ status: 202 });
    mocks.startAllServices.mockResolvedValue({ Requests: { id: 3 } });
    mocks.flushStateToDb.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("wires the exact stop, install, configure, and smoke-test sequence", async () => {
    const rangerModel = renderStep();
    expect(await screen.findByText("Operations ready")).toBeTruthy();
    const operations = mocks.progressProps.mock.calls.at(-1)?.[0].operations;

    expect(
      operations.map(({ id, label }: { id: string; label: string }) => ({
        id,
        label,
      })),
    ).toEqual([
      { id: "stopAllServices", label: "Stop All Services" },
      { id: "installRangerAdmins", label: "Install Additional Ranger Admin" },
      { id: "reconfigureServices", label: "Reconfigure Services" },
      { id: "startAllServices", label: "Start All Services" },
    ]);

    await operations[0].callback();
    await operations[1].callback();
    await operations[2].callback();
    await operations[3].callback();

    expect(mocks.stopAllServices).toHaveBeenCalledWith("c1");
    expect(mocks.createInstallComponentTask).toHaveBeenCalledWith(
      "RANGER_ADMIN",
      ["ra2.example.com", "ra3.example.com"],
      "RANGER",
      "c1",
      ["RANGER", "HDFS"],
      rangerModel,
      mocks.getKDCSessionState,
      { reconcileHosts: true },
    );
    expect(mocks.reconfigureRangerAdminServices).toHaveBeenCalledWith(
      "c1",
      "https://lb.example.com/ranger",
    );
    expect(mocks.startAllServices).toHaveBeenCalledWith("c1", {
      runSmokeTest: true,
      skipServiceChecks: true,
    });

    mocks.reconfigureRangerAdminServices.mockRejectedValueOnce(
      new Error("configuration save failed"),
    );
    await expect(operations[2].callback()).rejects.toThrow(
      "configuration save failed",
    );
  });

  it("blocks all side effects when the saved host snapshot is incomplete", async () => {
    renderStep({
      enableHighAvailibilityRangerAdminSteps: {
        GET_STARTED: validState.enableHighAvailibilityRangerAdminSteps.GET_STARTED,
        SELECT_HOSTS: {
          data: {
            masterComponentHosts: [
              {
                component: "RANGER_ADMIN",
                hostName: "ra1.example.com",
                isInstalled: true,
              },
            ],
          },
        },
      },
    });

    expect(
      screen.getByText("At least one additional Ranger Admin is required."),
    ).toBeTruthy();
    await waitFor(() => expect(mocks.progressProps).not.toHaveBeenCalled());
    expect(mocks.stopAllServices).not.toHaveBeenCalled();
    expect(mocks.createInstallComponentTask).not.toHaveBeenCalled();
  });

  it("keeps completion retryable when workflow cleanup fails", async () => {
    mocks.flushStateToDb.mockImplementation((action?: string) =>
      action === "complete"
        ? Promise.reject(new Error("cleanup unavailable"))
        : Promise.resolve(),
    );
    renderStep();
    await screen.findByText("Operations ready");
    await act(async () => {
      mocks.progressProps.mock.lastCall?.[0].setCompletionStatus(true);
    });

    fireEvent.click(screen.getByRole("button", { name: "Complete" }));

    expect(await screen.findByText("cleanup unavailable")).toBeTruthy();
    expect(
      (screen.getByRole("button", { name: "Complete" }) as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    expect(mocks.flushStateToDb).toHaveBeenCalledWith("complete");
  });
});
