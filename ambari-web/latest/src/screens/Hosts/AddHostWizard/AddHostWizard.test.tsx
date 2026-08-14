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

import React, { createContext } from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../store/context";
import { ContextWrapper } from "../../ClusterWizard";

const mocks = vi.hoisted(() => ({
  flushStateToDb: vi.fn(),
  getKDCSessionState: vi.fn((callback: Function) => callback()),
  getRequestStatus: vi.fn(),
  handleBackImperitive: vi.fn(),
  handleNextImperitive: vi.fn(),
  registerHostToComponent: vi.fn(),
  regenerateKeytabs: vi.fn(),
  updateConfigGroup: vi.fn(),
  updateHostComponents: vi.fn(),
}));

vi.mock("../../../api/hostsApi", () => ({
  HostsApi: {
    registerHostToComponent: mocks.registerHostToComponent,
    updateHostComponents: mocks.updateHostComponents,
  },
}));
vi.mock("../../../api/configGroupApi", () => ({
  default: { updateConfigGroup: mocks.updateConfigGroup },
}));
vi.mock("../../../api/requestApi", () => ({
  RequestApi: {
    getRequestStatus: mocks.getRequestStatus,
    regenerateKeytabs: mocks.regenerateKeytabs,
  },
}));
vi.mock("../../../hooks/useKDCSessionState", () => ({
  default: () => ({ isLoaded: true, getKDCSessionState: mocks.getKDCSessionState }),
}));
vi.mock("../../BackgroundOperations", () => ({
  default: ({ requestId, host }: any) => <div>Logs {requestId} {host}</div>,
}));
vi.mock("../../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext, sideItems, step }: any) => (
    <div>
      {sideItems}
      <button disabled={!isNextEnabled} onClick={onNext}>
        {step.nextLabel || "NEXT"}
      </button>
    </div>
  ),
}));

import AddHostInstall from "./AddHostInstall";
import AddHostReview from "./AddHostReview";
import AddHostSummary from "./AddHostSummary";

const assignments = [{
  hostname: "host1",
  checkboxes: [{ checked: true, label: "DATANODE" }],
}];
const componentMetadata = [{
  component_category: "SLAVE",
  component_name: "DATANODE",
  is_client: false,
  service_name: "HDFS",
}];

function baseState(reviewData: Record<string, any> = {}): any {
  return {
    addHostSteps: {
      NAME: { data: { clusterName: "cluster1" } },
      HOST_STATUS: {
        data: { hosts: [{ name: "host1", bootStatus: "REGISTERED" }] },
      },
      SLAVES_AND_CLIENTS: {
        data: {
          serviceComponents: assignments,
          allServiceComponentsList: componentMetadata,
        },
      },
      CONFIGURATIONS: {
        data: {
          configurations: [{
            serviceName: "HDFS",
            configGroups: [{
              id: 7,
              group_name: "workers",
              tag: "HDFS",
              service_name: "HDFS",
              hosts: [],
              desired_configs: [],
              isSelected: true,
            }],
          }],
        },
      },
      REVIEW: { data: reviewData },
    },
  };
}

function renderWithWizard(
  component: React.ReactNode,
  state: any,
  appContext: Record<string, any> = {},
  strict = false,
) {
  const WizardContext = createContext<any>({});
  const content = (
    <AppContext.Provider value={{
      clusterName: "cluster1",
      isKerberosEnabled: false,
      ...appContext,
    } as any}>
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={{
          state,
          dispatch: vi.fn(),
          flushStateToDb: mocks.flushStateToDb,
          stepWizardUtilities: {
            currentStep: { name: "REVIEW", canGoBack: true },
            handleBackImperitive: mocks.handleBackImperitive,
            handleNextImperitive: mocks.handleNextImperitive,
          },
        }}>
          {component}
        </WizardContext.Provider>
      </ContextWrapper.Provider>
    </AppContext.Provider>
  );
  return render(strict ? <React.StrictMode>{content}</React.StrictMode> : content);
}

describe("Add Host Review and deployment", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.flushStateToDb.mockResolvedValue(undefined);
    mocks.registerHostToComponent.mockResolvedValue({ data: {} });
    mocks.updateConfigGroup.mockResolvedValue({});
    mocks.updateHostComponents.mockResolvedValue({ Requests: { id: 10 } });
  });

  afterEach(() => cleanup());

  it("blocks installation on config-group failure and resumes completed stages", async () => {
    mocks.updateConfigGroup
      .mockRejectedValueOnce({ response: { data: { message: "Config group update failed" } } })
      .mockResolvedValueOnce({});
    renderWithWizard(<AddHostReview />, baseState());

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    expect(await screen.findByText("Config group update failed")).toBeTruthy();
    expect(mocks.updateHostComponents).not.toHaveBeenCalled();
    expect(mocks.registerHostToComponent).toHaveBeenCalledTimes(2);

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await waitFor(() => expect(mocks.updateHostComponents).toHaveBeenCalledTimes(1));
    expect(mocks.registerHostToComponent).toHaveBeenCalledTimes(2);
    expect(mocks.updateConfigGroup).toHaveBeenLastCalledWith(
      "cluster1",
      "7",
      [expect.objectContaining({
        ConfigGroup: expect.objectContaining({
          hosts: [{ host_name: "host1" }],
        }),
      })],
    );
    expect(mocks.handleNextImperitive).toHaveBeenCalledTimes(1);
  });

  it("polls install and start requests once under Strict Mode", async () => {
    mocks.getRequestStatus.mockImplementation(async (_cluster: string, requestId: string) => ({
      Requests: { request_status: "COMPLETED" },
      tasks: [{
        Tasks: {
          id: Number(requestId),
          host_name: "host1",
          role: "DATANODE",
          status: "COMPLETED",
          command: requestId === "10" ? "INSTALL" : "START",
        },
      }],
    }));
    mocks.updateHostComponents.mockResolvedValue({ Requests: { id: 11 } });
    renderWithWizard(
      <AddHostInstall />,
      baseState({
        clusterStatus: {
          status: "PENDING",
          phase: "INSTALL",
          requestId: 10,
          oldRequestsId: [10],
        },
      }),
      {},
      true,
    );

    await waitFor(() => expect(screen.getByText("Install and start completed")).toBeTruthy());
    expect(mocks.updateHostComponents).toHaveBeenCalledTimes(1);
    expect(mocks.getRequestStatus).toHaveBeenCalledWith("cluster1", "10");
    expect(mocks.getRequestStatus).toHaveBeenCalledWith("cluster1", "11");
    expect(screen.getByRole("button", { name: "View task logs" })).toBeTruthy();
  });

  it("does not launch the next phase after unmount", async () => {
    let resolveRequest: (value: any) => void = () => undefined;
    mocks.getRequestStatus.mockReturnValue(new Promise((resolve) => {
      resolveRequest = resolve;
    }));
    const rendered = renderWithWizard(
      <AddHostInstall />,
      baseState({
        clusterStatus: {
          status: "PENDING",
          phase: "INSTALL",
          requestId: 10,
          oldRequestsId: [10],
        },
      }),
    );
    await waitFor(() => expect(mocks.getRequestStatus).toHaveBeenCalledTimes(1));
    rendered.unmount();
    resolveRequest({ Requests: { request_status: "COMPLETED" }, tasks: [] });
    await Promise.resolve();

    expect(mocks.updateHostComponents).not.toHaveBeenCalled();
  });

  it("finishes by clearing Add Host state without a provisioning update", async () => {
    const state = baseState();
    state.addHostSteps.INSTALL_START_TEST = {
      data: {
        clusterStatus: { status: "STARTED" },
        hostInfo: [{ name: "host1", status: "success", logTasks: [] }],
      },
    };
    renderWithWizard(<AddHostSummary />, state);

    fireEvent.click(screen.getByRole("button", { name: "COMPLETE" }));
    await waitFor(() => expect(mocks.flushStateToDb).toHaveBeenCalledWith("cancel"));
  });
});
