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

import { createContext } from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ContextWrapper } from ".";
import "../../i18n";

const mocks = vi.hoisted(() => ({
  dispatch: vi.fn(),
  flushStateToDb: vi.fn(),
  getManagedDependency: vi.fn(),
  getRequests: vi.fn(),
  getRequestStatus: vi.fn(),
  updateHostComponents: vi.fn(),
  updateService: vi.fn(),
}));

vi.mock("../../api/requestApi", () => ({
  RequestApi: {
    getRequests: mocks.getRequests,
    getRequestStatus: mocks.getRequestStatus,
  },
}));
vi.mock("../../api/hostsApi", () => ({
  HostsApi: {
    updateHostComponents: mocks.updateHostComponents,
  },
}));
vi.mock("../../api/serviceApi", () => ({
  ServiceApi: { ambariService: vi.fn(), updateService: mocks.updateService },
}));
vi.mock("../../api/serviceDependenciesApi", () => ({
  default: { get: mocks.getManagedDependency },
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: () => null,
}));
vi.mock("../BackgroundOperations", () => ({
  default: () => null,
}));
vi.mock("react-router-dom", () => ({
  useBlocker: () => ({ state: "unblocked" }),
}));

import Step9 from "./Step9";

const binding = (providerPrepared: boolean) => ({
  binding_id: "11111111-1111-4111-8111-111111111111",
  consumer: { cluster_id: 27, service_name: "HBASE" },
  dependency_type: "HDFS" as const,
  desired_snapshot_version: 1,
  operation_epoch: 3,
  ownership: "managed" as const,
  readiness: {
    topology_current: true,
    required_daemon_host_ids: ["101"],
    prepared_daemon_host_ids: providerPrepared ? ["101"] : [],
    verified_daemon_host_ids: providerPrepared ? ["101"] : [],
    all_current_daemons_prepared: providerPrepared,
    all_current_daemons_verified: providerPrepared,
    active_command: false,
    preparation_requests: [{
      binding_id: "11111111-1111-4111-8111-111111111111",
      operation_id: "33333333-3333-4333-8333-333333333333",
      epoch: 3,
      snapshot_version: 1,
      host_id: 101,
      component_name: "HBASE_MASTER",
      request_id: 70,
      task_id: 701,
      state: "SUCCEEDED",
    }],
  },
  capabilities: {
    provider_prepared: providerPrepared,
    install_or_configure_allowed: providerPrepared,
    credential_status: "NOT_REQUIRED" as const,
    credentials_required: false,
    start_or_restart_allowed: false,
    retry_allowed: false,
    detach_allowed: false,
    allowed_actions: providerPrepared ? ["INSTALL_OR_CONFIGURE"] : [],
    next_action: providerPrepared
      ? "INSTALL_OR_CONFIGURE"
      : "WAIT_FOR_PROVIDER_PREPARATION",
  },
});

const installIntent = {
  clusterId: 27,
  clusterName: "cluster1",
  wizardName: "clusterCreation" as const,
  serviceNames: ["HBASE"],
  targets: [{
    serviceName: "HBASE",
    componentName: "HBASE_MASTER",
    hostName: "host-a",
  }],
  intentId: "22222222-2222-4222-8222-222222222222",
  state: "READY" as const,
};

function renderStep(
  intent = installIntent,
) {
  const state = {
    clusterCreationSteps: {
      NAME: { data: { clusterName: "cluster1" } },
      SERVICES: { data: { services: { HBASE: { selected: true, installed: false } } } },
      HOST_STATUS: { data: { hosts: [{ name: "host-a", bootStatus: "REGISTERED" }] } },
      REVIEW: { data: { clusterStatus: { status: "PENDING" } } },
      INSTALL_START_TEST: {
        data: {
          clusterStatus: { status: "PENDING" },
          phase: "WAIT_FOR_PROVIDER_PREPARATION",
          managedDependencyInstallIntent: intent,
          managedDependencyHandoff: {
            phase: "WAIT_FOR_PROVIDER_PREPARATION",
            clusterId: 27,
            clusterName: "cluster1",
            consumerServiceName: "HBASE",
            installIntent: intent,
            items: [{
              bindingId: "11111111-1111-4111-8111-111111111111",
              dependencyType: "HDFS",
              operationId: "33333333-3333-4333-8333-333333333333",
            }],
          },
          hostInfo: [],
        },
      },
    },
  };
  const contextValue = {
    state,
    dispatch: mocks.dispatch,
    flushStateToDb: mocks.flushStateToDb,
    stepWizardUtilities: {
      currentStep: { name: "INSTALL_START_TEST" },
      handleNextImperitive: vi.fn(),
    },
  };
  const WizardContext = createContext(contextValue);
  return render(
    <AppContext.Provider
      value={{
        clusterName: "cluster1",
        isKerberosEnabled: false,
        supports: { skipComponentStartAfterInstall: false },
      } as never}
    >
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={contextValue}>
          <Step9 wizardName="clusterCreation" />
        </WizardContext.Provider>
      </ContextWrapper.Provider>
    </AppContext.Provider>,
  );
}

describe("managed dependency installation handoff", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.flushStateToDb.mockResolvedValue(undefined);
    mocks.getRequestStatus.mockResolvedValue({ Requests: { request_status: "IN_PROGRESS" }, tasks: [] });
    mocks.getManagedDependency.mockResolvedValue(binding(false));
    mocks.getRequests.mockResolvedValue({ items: [] });
    mocks.updateHostComponents.mockResolvedValue({ Requests: { id: 91 } });
  });

  afterEach(cleanup);

  it("refreshes provider readiness and submits the exact workflow host-component target", async () => {
    renderStep();
    await waitFor(() => expect(mocks.getManagedDependency).toHaveBeenCalledWith(
      "cluster1",
      "11111111-1111-4111-8111-111111111111",
    ));
    expect(mocks.updateService).not.toHaveBeenCalled();

    mocks.getManagedDependency.mockResolvedValue(binding(true));
    fireEvent.click(screen.getByRole("button", { name: "Reload dependency status" }));

    await waitFor(() => expect(mocks.updateHostComponents).toHaveBeenCalledWith(
      "cluster1",
      expect.stringContaining("HostRoles/component_name=HBASE_MASTER"),
      expect.objectContaining({
        HostRoles: { state: "INSTALLED" },
        level: "HOST_COMPONENT",
      }),
    ));
    expect(mocks.updateHostComponents.mock.calls[0][1]).not.toContain("host-b");
    expect(mocks.flushStateToDb.mock.invocationCallOrder[0])
      .toBeLessThan(mocks.updateHostComponents.mock.invocationCallOrder[0]);
    expect(mocks.updateService).not.toHaveBeenCalled();
  });

  it("keeps a lost install submission durable and never issues a second PUT", async () => {
    mocks.getManagedDependency.mockResolvedValue(binding(true));
    mocks.updateHostComponents.mockResolvedValue({});
    renderStep();

    expect(await screen.findByText(/submission outcome is unknown/)).toBeTruthy();
    expect(mocks.updateHostComponents).toHaveBeenCalledOnce();
    expect(mocks.dispatch.mock.calls.some(([action]) =>
      action.payload?.data?.managedDependencyInstallIntent?.state === "SUBMITTING",
    )).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "Reload dependency status" }));
    await waitFor(() => expect(mocks.getManagedDependency).toHaveBeenCalledTimes(2));
    expect(mocks.updateHostComponents).toHaveBeenCalledOnce();
  });

  it("recovers a lost install response from current preparation lineage and exact request targets", async () => {
    const prepResponse = {
      Requests: { id: 70, request_status: "COMPLETED" },
      tasks: [{ Tasks: {
        id: 701,
        request_id: 70,
        role: "HBASE_MASTER",
        host_name: "host-a",
        status: "COMPLETED",
      } }],
    };
    const installResponse = {
      Requests: { id: 91, request_status: "IN_PROGRESS" },
      tasks: [{ Tasks: {
        id: 901,
        request_id: 91,
        role: "HBASE_MASTER",
        host_name: "host-a",
        status: "IN_PROGRESS",
      } }],
    };
    mocks.getManagedDependency.mockResolvedValue(binding(true));
    mocks.getRequestStatus.mockImplementation(async (_cluster: string, requestId: string) =>
      requestId === "70" ? prepResponse : installResponse);
    mocks.getRequests.mockResolvedValue({
      items: [{
        Requests: {
          id: 91,
          request_context: "Install Services",
          request_status: "IN_PROGRESS",
          start_time: new Date().toISOString(),
        },
        tasks: installResponse.tasks,
      }],
    });
    renderStep({
      ...installIntent,
      state: "SUBMITTING",
      submissionStartedAt: Date.now() - 1000,
    });

    await waitFor(() => expect(mocks.getRequests).toHaveBeenCalledWith("cluster1"));
    expect(mocks.updateHostComponents).not.toHaveBeenCalled();
    expect(mocks.dispatch.mock.calls.some(([action]) =>
      action.payload?.data?.managedDependencyInstallIntent?.state === "SUBMITTED"
      && action.payload?.data?.managedDependencyInstallIntent?.requestId === 91,
    )).toBe(true);
  });

  it("rejects a lost submission when the current binding lineage is stale", async () => {
    mocks.getManagedDependency.mockResolvedValue({ ...binding(true), operation_epoch: 4 });
    renderStep({ ...installIntent, state: "SUBMITTING" });

    expect(await screen.findByText(/different cluster or service/)).toBeTruthy();
    expect(mocks.getRequests).not.toHaveBeenCalled();
    expect(mocks.updateHostComponents).not.toHaveBeenCalled();
  });

  it("rejects a recovered request containing a foreign target", async () => {
    const prepResponse = {
      Requests: { id: 70 },
      tasks: [{ Tasks: { id: 701, request_id: 70, role: "HBASE_MASTER", host_name: "host-a", status: "COMPLETED" } }],
    };
    mocks.getManagedDependency.mockResolvedValue(binding(true));
    mocks.getRequestStatus.mockResolvedValue(prepResponse);
    mocks.getRequests.mockResolvedValue({
      items: [{
        Requests: { id: 91, request_context: "Install Services", request_status: "IN_PROGRESS" },
        tasks: [{ Tasks: { id: 902, request_id: 91, role: "HBASE_MASTER", host_name: "host-b", status: "IN_PROGRESS" } }],
      }],
    });
    renderStep({ ...installIntent, state: "SUBMITTING" });

    expect(await screen.findByText(/submission outcome is unknown/)).toBeTruthy();
    expect(mocks.updateHostComponents).not.toHaveBeenCalled();
  });
});
