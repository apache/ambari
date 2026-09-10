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
  dispatch: vi.fn(), flush: vi.fn(), next: vi.fn(), get: vi.fn(), launch: vi.fn(), retry: vi.fn(),
  scan: vi.fn(), update: vi.fn(), start: vi.fn(), status: vi.fn(),
}));
vi.mock("../../api/serviceDependenciesApi", () => ({ default: {
  getDeployment: mocks.get, launchDeployment: mocks.launch, retryDeployment: mocks.retry,
} }));
vi.mock("../../api/requestApi", () => ({ RequestApi: { getRequests: mocks.scan, getRequestStatus: mocks.status } }));
vi.mock("../../api/hostsApi", () => ({ HostsApi: { updateHostComponents: mocks.update } }));
vi.mock("../../api/serviceApi", () => ({ ServiceApi: { updateService: mocks.start } }));
vi.mock("../BackgroundOperations", () => ({ default: () => null }));
vi.mock("react-router-dom", () => ({ useBlocker: () => ({ state: "unblocked" }) }));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext, sideItems }: any) => <>{sideItems}<button disabled={!isNextEnabled} onClick={onNext}>Continue</button></>,
}));
import Step9 from "./Step9";
import { reducer } from "./clusterStore/reducer";
import { projectClusterCreationValues } from "../../Utils/scopedWorkflow";

const intent = { intentId: "22222222-2222-4222-8222-222222222222", clusterId: 27,
  clusterName: "cluster1", wizardName: "clusterCreation", serviceNames: ["HBASE"], state: "READY",
  targets: [{ serviceName: "HBASE", componentName: "HBASE_MASTER", hostName: "host-a" }] };
const progress = (state = "WAIT_DEPENDENCIES", extra = {}) => ({ deployment_id: intent.intentId,
  cluster_id: 27, state, phase: "INSTALL", request_id: 91, attempt_id: intent.intentId,
  history: [{ attemptId: intent.intentId, phase: "INSTALL", requestId: 91 }], targets: intent.targets,
  bindings: [], completed: state === "COMPLETE", install_only: state === "INSTALL_ONLY", retry_allowed: false, ...extra });
function show(saved: any = {}, legacy = false) {
  const value = { state: { clusterCreationSteps: {
    NAME: { data: { clusterName: "cluster1" } },
    REVIEW: { data: { clusterStatus: { status: "PENDING" } } },
    INSTALL_START_TEST: { data: legacy ? saved : { managedDependencyInstallIntent: intent,
      managedDependencyHandoff: { clusterId: 27, installIntent: intent, items: [{}] }, ...saved } },
  } }, dispatch: mocks.dispatch, flushStateToDb: mocks.flush,
    stepWizardUtilities: { currentStep: { name: "INSTALL_START_TEST" }, handleNextImperitive: mocks.next } };
  const Context = createContext(value);
  return render(<AppContext.Provider value={{ clusterName: "cluster1", supports: {}, isKerberosEnabled: false } as any}>
    <ContextWrapper.Provider value={{ Context }}><Context.Provider value={value}><Step9 /></Context.Provider></ContextWrapper.Provider>
  </AppContext.Provider>);
}
beforeEach(() => { vi.resetAllMocks(); mocks.flush.mockResolvedValue(undefined); mocks.get.mockResolvedValue(progress()); });
afterEach(cleanup);

describe("durable managed deployment handoff", () => {
  it("renders exact persisted deployment and waits for verification without scheduling or scanning", async () => {
    show();
    await screen.findByText("Checking dependency connections");
    expect(mocks.get).toHaveBeenCalledWith("cluster1", intent.intentId, expect.any(AbortSignal));
    expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(true);
    expect(mocks.scan).not.toHaveBeenCalled(); expect(mocks.update).not.toHaveBeenCalled(); expect(mocks.start).not.toHaveBeenCalled();
  });
  it("flushes the immutable launch ID before submission and recovers a lost response by GET", async () => {
    mocks.get.mockRejectedValueOnce({ response: { status: 404 } });
    mocks.launch.mockRejectedValueOnce(new Error("Connection lost"));
    show();
    await screen.findByText("Connection lost");
    expect(mocks.flush.mock.invocationCallOrder[0]).toBeLessThan(mocks.launch.mock.invocationCallOrder[0]);
    expect(mocks.launch).toHaveBeenCalledWith("cluster1", intent.intentId, intent.targets, false, expect.any(AbortSignal));
    fireEvent.click(screen.getByText("Reload status"));
    await screen.findByText("Checking dependency connections");
    expect(mocks.launch).toHaveBeenCalledTimes(1);
  });
  it("fails closed for legacy submitted intent without backend lineage", async () => {
    mocks.get.mockRejectedValue({ response: { status: 404 } });
    show({ managedDependencyInstallIntent: { ...intent, state: "SUBMITTING" } });
    await screen.findByText(/predates durable deployment tracking/);
    expect(mocks.launch).not.toHaveBeenCalled(); expect(mocks.scan).not.toHaveBeenCalled();
  });
  it("rejects another cluster's response", async () => {
    mocks.get.mockResolvedValue(progress("COMPLETE", { cluster_id: 99 })); show();
    await screen.findByText("Ambari returned a different deployment identity.");
    expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(true);
  });
  it("keeps summary disabled until service checks finish", async () => {
    mocks.get.mockResolvedValue(progress("CHECKING")); show();
    await screen.findByText("Running service checks"); expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(true);
    mocks.get.mockResolvedValue(progress("COMPLETE")); fireEvent.click(screen.getByText("Reload status"));
    await waitFor(() => expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(false));
    fireEvent.click(screen.getByText("Continue")); await waitFor(() => expect(mocks.next).toHaveBeenCalled());
    expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({ payload: expect.objectContaining({ data:
      expect.objectContaining({ hostInfo: [expect.objectContaining({ name: "host-a", status: "success" })] }) }) }));
  });
  it("resumes the same retry intent after a lost response", async () => {
    mocks.get.mockResolvedValue(progress("FAILED", { retry_allowed: true }));
    mocks.retry.mockRejectedValue(new Error("Retry response lost")); show();
    fireEvent.click(await screen.findByText("Retry failed deployment")); await screen.findByText("Retry response lost");
    const id = mocks.retry.mock.calls[0][2];
    fireEvent.click(screen.getByText("Retry failed deployment")); await waitFor(() => expect(mocks.retry).toHaveBeenCalledTimes(2));
    expect(mocks.retry.mock.calls[1][2]).toBe(id);
  });
  it("restores install-only completion without a Start request", async () => {
    mocks.get.mockResolvedValue(progress("INSTALL_ONLY", { request_id: null })); show({ managedDeploymentVersion: 1 });
    await waitFor(() => expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(false)); expect(mocks.start).not.toHaveBeenCalled();
  });
  it("never recreates an acknowledged deployment that disappears after completion", async () => {
    mocks.get.mockResolvedValueOnce(progress("COMPLETE"));
    show();
    await waitFor(() => expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(false));
    expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({ payload: expect.objectContaining({
      data: expect.objectContaining({ managedDeploymentAcknowledged: true }),
    }) }));
    mocks.get.mockRejectedValue({ response: { status: 404 } });
    fireEvent.click(screen.getByText("Reload status"));
    await screen.findByText(/previously confirmed deployment is missing/);
    expect(mocks.launch).not.toHaveBeenCalled();
    expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(true);
  });
  it("preserves acknowledged deployment ownership after a browser refresh", async () => {
    mocks.get.mockRejectedValue({ response: { status: 404 } });
    show({ managedDeploymentVersion: 1, managedDeploymentAcknowledged: true });
    await screen.findByText(/previously confirmed deployment is missing/);
    expect(mocks.launch).not.toHaveBeenCalled();
  });
  it("invalidates cached completion when revalidation returns another cluster", async () => {
    mocks.get.mockResolvedValueOnce(progress("COMPLETE"));
    show();
    await waitFor(() => expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(false));
    mocks.get.mockResolvedValue(progress("COMPLETE", { cluster_id: 99 }));
    fireEvent.click(screen.getByText("Reload status"));
    await screen.findByText("Ambari returned a different deployment identity.");
    expect((screen.getByText("Continue") as HTMLButtonElement).disabled).toBe(true);
    expect(mocks.next).not.toHaveBeenCalled();
  });
  it("retains the entire handoff through the real reducer, persistence projection, retry and refresh", async () => {
    let state: any = { clusterCreationSteps: {} };
    let saved: any;
    mocks.dispatch.mockImplementation(action => { state = reducer(state, action); });
    mocks.flush.mockImplementation(async () => {
      saved = projectClusterCreationValues({ state }).state.clusterCreationSteps.INSTALL_START_TEST.data;
    });
    mocks.get.mockResolvedValue(progress("FAILED", { retry_allowed: true }));
    mocks.retry.mockRejectedValue(new Error("Retry response lost"));
    show();
    fireEvent.click(await screen.findByText("Retry failed deployment"));
    await screen.findByText("Retry response lost");
    expect(saved.managedDependencyInstallIntent).toEqual(intent);
    expect(saved.managedDependencyHandoff.installIntent).toEqual(intent);
    expect(saved.managedDeploymentAcknowledged).toBe(true);
    expect(saved.managedDeploymentRetryId).toBe(mocks.retry.mock.calls[0][2]);
    cleanup();
    mocks.get.mockRejectedValue({ response: { status: 404 } });
    show(saved);
    await screen.findByText(/previously confirmed deployment is missing/);
    expect(mocks.launch).not.toHaveBeenCalled();
  });
});
