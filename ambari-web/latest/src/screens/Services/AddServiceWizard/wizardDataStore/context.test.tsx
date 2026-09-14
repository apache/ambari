/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import "../../../../i18n";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useContext, useRef } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from "vitest";
import WorkflowStateApi, {
  type ScopedWorkflowState,
  type ScopedWorkflowUpdate,
  type WorkflowScope,
} from "../../../../api/workflowStateApi";
import { ServiceApi } from "../../../../api/serviceApi";
import VersionsApi from "../../../../api/versionsApi";
import modalManager from "../../../../store/ModalManager";
import { AppContext } from "../../../../store/context";
import { CANCEL_ADD_SERVICE_WIZARD_EVENT } from "../../../../Utils/addServicePersistence";
import { clearResolvedReentryMarkers } from "../../../../Utils/scopedWorkflow";
import {
  consumeWorkflowReturnPath,
  saveWorkflowReturnPath,
} from "../../../../Utils/workflowReturnPath";
import {
  AddServiceContext,
  AddServiceProvider,
  classifyAddServiceServices,
} from "./context";
import { ActionTypes } from "./types";

const wizardSteps = {
  1: { name: "SERVICES" },
  2: { name: "MASTERS" },
  3: { name: "SLAVES_AND_CLIENTS" },
  4: { name: "CONFIGURATION" },
  5: { name: "REVIEW" },
  6: { name: "INSTALL_START_TEST" },
  7: { name: "SUMMARY" },
};

const createWizardUtilities = () => ({
  currentStep: { name: "REVIEW" },
  jumpToStep: vi.fn(),
  wizardSteps,
});

const configuration = (clusterName: string, configProperties: any[] = []) => ({
  activeStep: "REVIEW",
  addServiceSteps: {
    NAME: { step: "NAME", data: { clusterName } },
    VERSION: { step: "VERSION", data: { selectedVersion: { id: "HDP-3.0" } } },
    SERVICES: { step: "SERVICES", data: { services: {} } },
    HOST_STATUS: { step: "HOST_STATUS", data: { hosts: [] } },
    MASTERS: { step: "MASTERS", data: { mastersData: [] } },
    CONFIGURATION: { step: "CONFIGURATION", data: { configProperties } },
  },
});

const workflowState = (
  clusterName: string,
  revision = 7,
  configProperties: any[] = [],
): ScopedWorkflowState => ({
  revision,
  owner: "alice",
  workflow: "ADD_SERVICE",
  phase: "REVIEW",
  values: {
    ADD_SERVICE: configuration(clusterName, configProperties),
    CLUSTER_STATE: { stepName: "REVIEW" },
  },
});

const emptyWorkflowState = (revision = 7): ScopedWorkflowState => ({
  revision,
  owner: "alice",
  workflow: "ADD_SERVICE",
  phase: "START",
  values: {},
});

const savedState = (
  update: ScopedWorkflowUpdate,
): ScopedWorkflowState => ({
  revision: update.expected_revision + 1,
  owner: "alice",
  workflow: update.workflow,
  phase: update.phase,
  values: update.values,
});

const deferred = <T,>() => {
  let resolve: (value: T) => void = () => undefined;
  let reject: (reason?: any) => void = () => undefined;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
};

function Probe({
  mutation,
  advisor,
}: {
  mutation: () => void;
  advisor?: (
    revision: number,
    isCurrent: () => boolean,
  ) => Promise<void>;
}) {
  const {
    dispatch,
    flushStateToDb,
    installedServices,
    state,
    withStateCheckpoint,
    workflowMaterializedServices,
  } = useContext(AddServiceContext);
  const advisorScopeRef = useRef("provider-a");
  const reenter = () => dispatch({
    type: ActionTypes.STORE_INFORMATION,
    payload: clearResolvedReentryMarkers({
      step: "CONFIGURATION",
      data: {
        configProperties: [{
          name: "hive_database_password",
          propertyAttributes: { type: "password" },
          requires_reentry: true,
          value: "fresh-secret",
        }],
      },
    }),
  });
  const checkpointThenMutate = () => {
    void Promise.resolve(flushStateToDb(
      "checkpoint",
      -1,
      "ADD_SERVICES_DEPLOY_PREP_2",
    )).then(mutation).catch(() => undefined);
  };
  const startAdvisor = () => {
    const capturedScope = advisorScopeRef.current;
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "MASTERS",
        data: {
          mastersData: [{
            host_name: "worker-a",
            masterServices: [{ component: "HBASE_MASTER" }],
          }],
        },
      },
    });
    void withStateCheckpoint!(async (revision) => {
      await advisor?.(revision, () => advisorScopeRef.current === capturedScope);
      if (advisorScopeRef.current !== capturedScope) {
        throw new Error("Managed dependency provider changed during advice.");
      }
      mutation();
    }).catch(() => undefined);
  };
  const editMastersAgain = () => dispatch({
    type: ActionTypes.STORE_INFORMATION,
    payload: {
      step: "MASTERS",
      data: {
        mastersData: [{
          host_name: "worker-b",
          masterServices: [{ component: "HBASE_MASTER" }],
        }],
      },
    },
  });
  const changeProvider = () => {
    advisorScopeRef.current = "provider-b";
    dispatch({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "SERVICES",
        data: {
          managedDependencies: {
            HDFS: {
              mode: "managed",
              provider: { cluster_id: 42, service_name: "HDFS" },
            },
          },
        },
      },
    });
  };

  return (
    <>
      <div data-testid="state">{JSON.stringify(state)}</div>
      <div data-testid="services">{installedServices.join(",")}</div>
      <div data-testid="materialized-services">{workflowMaterializedServices.join(",")}</div>
      <button onClick={reenter}>Re-enter configuration</button>
      <button onClick={checkpointThenMutate}>Checkpoint and mutate</button>
      <button onClick={startAdvisor}>Save edited masters and advise</button>
      <button onClick={editMastersAgain}>Edit masters again</button>
      <button onClick={changeProvider}>Change provider</button>
      <button onClick={() => void flushStateToDb("cancel").catch(() => undefined)}>
        Cancel wizard
      </button>
    </>
  );
}

const appContext = (
  clusterName: string,
  clusterId: number,
  navigateCluster = vi.fn(),
) => ({
  availableClusters: [{ Clusters: { cluster_id: clusterId, cluster_name: clusterName } }],
  cluster: { cluster_id: clusterId, cluster_name: clusterName },
  clusterName,
  loginName: "alice",
  navigateCluster,
  runtimeKey: JSON.stringify(["alice", "cluster", clusterId]),
  serviceComponentInfo: [],
  services: [],
} as any);

const providerTree = (
  contextValue: any,
  utilities: ReturnType<typeof createWizardUtilities>,
  mutation = vi.fn(),
  advisor?: (
    revision: number,
    isCurrent: () => boolean,
  ) => Promise<void>,
) => (
  <MemoryRouter initialEntries={[`/clusters/${contextValue.clusterName}/main/service/add/step5`]}>
    <AppContext.Provider value={contextValue}>
      <AddServiceProvider stepWizardUtilities={utilities}>
        <Probe mutation={mutation} advisor={advisor} />
      </AddServiceProvider>
    </AppContext.Provider>
  </MemoryRouter>
);

describe("Add Service scoped recovery", () => {
  let getState: MockInstance<typeof WorkflowStateApi.get>;
  let putState: MockInstance<typeof WorkflowStateApi.put>;
  let getServices: MockInstance<typeof ServiceApi.getAllServices>;

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    sessionStorage.clear();
    getState = vi.spyOn(WorkflowStateApi, "get")
      .mockResolvedValue(workflowState("cluster-a"));
    putState = vi.spyOn(WorkflowStateApi, "put")
      .mockImplementation(async (_scope, update) => savedState(update));
    getServices = vi.spyOn(ServiceApi, "getAllServices")
      .mockResolvedValue({ items: [] });
    vi.spyOn(VersionsApi, "getServices").mockResolvedValue({
      items: [{
        ClusterStackVersions: {
          repository_version: 101,
          stack: "HDP",
          state: "CURRENT",
          version: "3.0",
        },
        repository_versions: [{ RepositoryVersions: {
          display_name: "HDP-3.0",
          id: 101,
          repository_version: "3.0",
        } }],
      }],
    } as any);
    vi.spyOn(VersionsApi, "getRepoDetails").mockResolvedValue({
      items: [{
        repository_versions: [{
          operating_systems: [{
            OperatingSystems: { os_type: "redhat8" },
            repositories: [],
          }],
        }],
      }],
    } as any);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("keeps a workflow-owned INIT HBase service fresh after an interrupted create", async () => {
    const resumed = workflowState("cluster-a");
    (resumed.values.ADD_SERVICE as any).addServiceSteps.SERVICES.data.services = {
      HBASE: { installed: false, selected: true, serviceName: "HBASE" },
    };
    (resumed.values.ADD_SERVICE as any).addServiceSteps.REVIEW = { data: {
      serviceCreationIntent: {
        clusterId: 11,
        clusterName: "cluster-a",
        serviceNames: ["HBASE"],
      },
    } };
    getState.mockResolvedValue(resumed);
    getServices.mockResolvedValue({ items: [
      { ServiceInfo: { service_name: "HBASE", state: "INIT", desired_repository_version_id: 101, maintenance_state: "OFF" }, components: [] },
      { ServiceInfo: { service_name: "KAFKA", state: "INSTALLED", desired_repository_version_id: 101, maintenance_state: "OFF" }, components: [] },
    ] });

    render(providerTree(appContext("cluster-a", 11), createWizardUtilities()));

    expect(await screen.findByText("KAFKA")).toBeTruthy();
    expect(screen.getByTestId("services").textContent).toBe("KAFKA");
    expect(screen.getByTestId("materialized-services").textContent).toBe("HBASE");
    expect(screen.getByTestId("state").textContent).toContain('"HBASE"');
    expect(screen.getByTestId("state").textContent).toContain('"installed":false');
  });

  it("does not adopt a selected INIT service without an exact workflow intent", () => {
    const restored = configuration("cluster-a");
    restored.addServiceSteps.SERVICES.data.services = {
      HBASE: { installed: false, selected: true, serviceName: "HBASE" },
    };
    expect(classifyAddServiceServices([
      { ServiceInfo: { service_name: "HBASE", state: "INIT" } },
    ], restored, { clusterId: 11, clusterName: "cluster-a" })).toEqual({
      installedServices: ["HBASE"],
      workflowMaterializedServices: [],
    });
  });

  it("does not adopt an INIT service from another cluster's saved intent", () => {
    const restored: { activeStep: string; addServiceSteps: Record<string, { data: unknown }> } = configuration("cluster-a");
    restored.addServiceSteps.REVIEW = { data: {
      serviceCreationIntent: {
        clusterId: 12,
        clusterName: "cluster-b",
        serviceNames: ["HBASE"],
      },
    } };

    expect(classifyAddServiceServices([
      { ServiceInfo: { service_name: "HBASE", state: "INIT" } },
    ], restored, { clusterId: 11, clusterName: "cluster-a" })).toEqual({
      installedServices: ["HBASE"],
      workflowMaterializedServices: [],
    });
  });

  it("restores redacted configuration at the editable step and keeps fresh secrets volatile", async () => {
    getState.mockResolvedValue(workflowState("cluster-a", 7, [{
      name: "hive_database_password",
      propertyAttributes: { type: "password" },
      requires_reentry: true,
    }]));
    const storageWrite = vi.spyOn(Storage.prototype, "setItem");
    const utilities = createWizardUtilities();
    render(providerTree(appContext("cluster-a", 11), utilities));

    expect(await screen.findByText(/credential values were removed/)).toBeTruthy();
    expect(utilities.jumpToStep).toHaveBeenCalledWith(4, true);
    expect((await screen.findByTestId("state")).textContent).toContain("requires_reentry");
    expect(screen.getByRole("button", { name: "Re-enter configuration" })).toBeTruthy();
    expect(putState).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Re-enter configuration" }));
    expect(screen.getByTestId("state").textContent).toContain("fresh-secret");
    fireEvent.click(screen.getByRole("button", { name: "Checkpoint and mutate" }));
    await waitFor(() => expect(putState).toHaveBeenCalled());

    const [scope, update] = putState.mock.calls[0] as [WorkflowScope, ScopedWorkflowUpdate];
    const savedProperty = update.values.ADD_SERVICE.addServiceSteps
      .CONFIGURATION.data.configProperties[0];
    expect(update.expected_revision).toBe(7);
    expect(scope).toEqual({ type: "clusters", id: "11" });
    expect(savedProperty).not.toHaveProperty("value");
    expect(savedProperty.requires_reentry).toBe(true);
    expect(JSON.stringify(update)).not.toContain("fresh-secret");
    expect(storageWrite).not.toHaveBeenCalled();
    expect(screen.getByTestId("state").textContent).toContain("fresh-secret");
  });

  it("does not begin a deployment mutation until the scoped CAS checkpoint resolves", async () => {
    const checkpoint = deferred<ScopedWorkflowState>();
    putState.mockImplementationOnce(() => checkpoint.promise);
    const mutation = vi.fn();
    render(providerTree(appContext("cluster-a", 11), createWizardUtilities(), mutation));

    fireEvent.click(await screen.findByRole("button", { name: "Checkpoint and mutate" }));
    await waitFor(() => expect(putState).toHaveBeenCalledOnce());
    expect(mutation).not.toHaveBeenCalled();

    const [, update] = putState.mock.calls[0] as [WorkflowScope, ScopedWorkflowUpdate];
    checkpoint.resolve(savedState(update));
    await waitFor(() => expect(mutation).toHaveBeenCalledOnce());
    expect(putState.mock.invocationCallOrder[0]).toBeLessThan(
      mutation.mock.invocationCallOrder[0],
    );
  });

  it("holds delayed advice behind edited masters and invalidates it after a provider change", async () => {
    const advice = deferred<void>();
    const mutation = vi.fn();
    const advisor = vi.fn(async (
      revision: number,
      isCurrent: () => boolean,
    ) => {
      expect(revision).toBe(7);
      expect(isCurrent()).toBe(true);
      await advice.promise;
      expect(isCurrent()).toBe(false);
    });
    render(providerTree(
      appContext("cluster-a", 11),
      createWizardUtilities(),
      mutation,
      advisor,
    ));

    fireEvent.click(await screen.findByRole("button", { name: "Save edited masters and advise" }));
    await waitFor(() => expect(putState).toHaveBeenCalledOnce());
    expect((putState.mock.calls[0][1] as ScopedWorkflowUpdate).values.ADD_SERVICE
      .addServiceSteps.MASTERS.data.mastersData[0].host_name).toBe("worker-a");

    fireEvent.click(screen.getByRole("button", { name: "Edit masters again" }));
    fireEvent.click(screen.getByRole("button", { name: "Change provider" }));
    await Promise.resolve();
    expect(putState).toHaveBeenCalledOnce();

    advice.resolve();
    await waitFor(() => expect(putState.mock.calls.at(-1)?.[1].values.ADD_SERVICE
      .addServiceSteps).toMatchObject({
        MASTERS: { data: { mastersData: [{ host_name: "worker-b" }] } },
        SERVICES: { data: { managedDependencies: { HDFS: { provider: { cluster_id: 42 } } } } },
      }));
    expect(advisor).toHaveBeenCalledOnce();
    expect(mutation).not.toHaveBeenCalled();
  });

  it("does not run the advisor after a foreign-tab checkpoint conflict", async () => {
    const advice = vi.fn(async () => undefined);
    putState.mockRejectedValueOnce({
      response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } },
    });
    render(providerTree(
      appContext("cluster-a", 11),
      createWizardUtilities(),
      vi.fn(),
      advice,
    ));

    fireEvent.click(await screen.findByRole("button", { name: "Save edited masters and advise" }));
    expect(await screen.findByText(/changed in another tab/)).toBeTruthy();
    expect(advice).not.toHaveBeenCalled();
  });

  it("reloads after a checkpoint conflict and never replays the rejected mutation", async () => {
    getState
      .mockResolvedValueOnce(workflowState("cluster-a", 7))
      .mockResolvedValueOnce(workflowState("cluster-a", 8));
    putState
      .mockRejectedValueOnce({
        response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } },
      })
      .mockImplementationOnce(async (_scope, update) => savedState(update));
    const mutation = vi.fn();
    render(providerTree(appContext("cluster-a", 11), createWizardUtilities(), mutation));

    fireEvent.click(await screen.findByRole("button", { name: "Checkpoint and mutate" }));
    expect(await screen.findByText(/changed in another tab/)).toBeTruthy();
    expect(mutation).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    fireEvent.click(await screen.findByRole("button", { name: "Checkpoint and mutate" }));
    await waitFor(() => expect(mutation).toHaveBeenCalledOnce());
    expect(putState.mock.calls.length).toBeGreaterThanOrEqual(2);
    expect(putState.mock.calls[1][1].expected_revision).toBe(8);
    expect(putState.mock.calls.slice(1).every(([, update]) => update.expected_revision >= 8)).toBe(true);
  });

  it("keeps failed release ownership retryable and navigates only after IDLE is saved", async () => {
    putState
      .mockRejectedValueOnce(new Error("release failed"))
      .mockImplementationOnce(async (_scope, update) => savedState(update));
    const navigateCluster = vi.fn();
    const hide = vi.spyOn(modalManager, "hide");
    saveWorkflowReturnPath({
      clusterId: 11,
      principal: "alice",
      workflow: "ADD_SERVICE",
    }, "/main/admin/stack/services");
    saveWorkflowReturnPath({
      clusterId: 11,
      principal: "alice",
      workflow: "ENABLING_KERBEROS",
    }, "/main/admin/kerberos?source=stack");
    render(providerTree(
      appContext("cluster-a", 11, navigateCluster),
      createWizardUtilities(),
    ));

    await screen.findByRole("button", { name: "Cancel wizard" });
    fireEvent(window, new Event(CANCEL_ADD_SERVICE_WIZARD_EVENT));
    expect(await screen.findByText("release failed")).toBeTruthy();
    expect(navigateCluster).not.toHaveBeenCalled();
    expect(hide).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    await screen.findByRole("button", { name: "Cancel wizard" });
    fireEvent(window, new Event(CANCEL_ADD_SERVICE_WIZARD_EVENT));
    await waitFor(() => expect(navigateCluster).toHaveBeenCalledWith(
      "/main/admin/stack/services",
    ));
    expect((putState.mock.calls[1][1] as ScopedWorkflowUpdate).workflow).toBe("IDLE");
    expect(hide).toHaveBeenCalledOnce();
    expect(consumeWorkflowReturnPath({
      clusterId: 11,
      principal: "alice",
      workflow: "ENABLING_KERBEROS",
    }, "/main/admin/kerberos")).toBe("/main/admin/kerberos?source=stack");
  });

  it("ignores an old cluster service response after the provider switches scope", async () => {
    getState.mockImplementation(async (scope) =>
      workflowState(scope.id === "11" ? "cluster-a" : "cluster-b"));
    const clusterA = deferred<any>();
    const clusterB = deferred<any>();
    getServices.mockImplementation((clusterName) =>
      clusterName === "cluster-a" ? clusterA.promise : clusterB.promise);
    const utilitiesA = createWizardUtilities();
    const view = render(providerTree(appContext("cluster-a", 11), utilitiesA));
    await waitFor(() => expect(getServices).toHaveBeenCalledWith("cluster-a"));

    const utilitiesB = createWizardUtilities();
    view.rerender(providerTree(appContext("cluster-b", 12), utilitiesB));
    await waitFor(() => expect(getServices).toHaveBeenCalledWith("cluster-b"));
    clusterB.resolve({ items: [{ ServiceInfo: { service_name: "B_SERVICE" } }] });
    expect(await screen.findByText("B_SERVICE")).toBeTruthy();

    clusterA.resolve({ items: [{ ServiceInfo: { service_name: "A_SERVICE" } }] });
    await clusterA.promise;
    await waitFor(() => {
      expect(screen.queryByText("A_SERVICE")).toBeNull();
      expect(screen.getByTestId("services").textContent).toBe("B_SERVICE");
    });
    expect(screen.getByTestId("state").textContent).toContain("cluster-b");
  });

  it("hides the prior cluster editor while the new scoped service read is pending", async () => {
    getState.mockImplementation(async (scope) =>
      workflowState(scope.id === "11" ? "cluster-a" : "cluster-b"));
    const clusterB = deferred<any>();
    getServices.mockImplementation((clusterName) => clusterName === "cluster-a"
      ? Promise.resolve({ items: [{ ServiceInfo: { service_name: "A_SERVICE" } }] })
      : clusterB.promise);
    const view = render(providerTree(
      appContext("cluster-a", 11),
      createWizardUtilities(),
    ));
    expect(await screen.findByText("A_SERVICE")).toBeTruthy();

    view.rerender(providerTree(
      appContext("cluster-b", 12),
      createWizardUtilities(),
    ));
    await waitFor(() => {
      expect(getServices).toHaveBeenCalledWith("cluster-b");
      expect(screen.queryByTestId("state")).toBeNull();
      expect(screen.queryByTestId("services")).toBeNull();
    });

    clusterB.resolve({ items: [{ ServiceInfo: { service_name: "B_SERVICE" } }] });
    expect(await screen.findByText("B_SERVICE")).toBeTruthy();
    expect(screen.getByTestId("state").textContent).toContain("cluster-b");
  });

  it("does not carry a populated cluster snapshot into a fresh cluster scope", async () => {
    getState.mockImplementation(async (scope) => scope.id === "11"
      ? workflowState("cluster-a", 7, [{ name: "A_ONLY", value: "a-value" }])
      : emptyWorkflowState(3));
    const view = render(providerTree(
      appContext("cluster-a", 11),
      createWizardUtilities(),
    ));
    expect((await screen.findByTestId("state")).textContent).toContain("A_ONLY");
    putState.mockClear();

    view.rerender(providerTree(
      appContext("cluster-b", 12),
      createWizardUtilities(),
    ));
    await waitFor(() => {
      expect(screen.getByTestId("state").textContent).toContain("cluster-b");
      expect(screen.getByTestId("state").textContent).not.toContain("cluster-a");
      expect(screen.getByTestId("state").textContent).not.toContain("A_ONLY");
    });
    await waitFor(() => expect(putState).toHaveBeenCalled());
    putState.mock.calls.forEach(([scopeValue, update]) => {
      expect(scopeValue).toEqual({ type: "clusters", id: "12" });
      expect(JSON.stringify(update)).not.toContain("A_ONLY");
    });
  });

  it("retries a conflict from an empty server snapshot without replaying old state", async () => {
    getState
      .mockResolvedValueOnce(workflowState(
        "cluster-a",
        7,
        [{ name: "A_ONLY", value: "a-value" }],
      ))
      .mockResolvedValueOnce(emptyWorkflowState(8));
    putState.mockRejectedValueOnce({
      response: { data: { code: "WORKFLOW_VERSION_CONFLICT" } },
    });
    const mutation = vi.fn();
    render(providerTree(
      appContext("cluster-a", 11),
      createWizardUtilities(),
      mutation,
    ));

    fireEvent.click(await screen.findByRole("button", { name: "Checkpoint and mutate" }));
    expect(await screen.findByText(/changed in another tab/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => {
      expect(screen.getByTestId("state").textContent).toContain("cluster-a");
      expect(screen.getByTestId("state").textContent).not.toContain("A_ONLY");
    });
    await waitFor(() => expect(putState.mock.calls.length).toBeGreaterThan(1));
    putState.mock.calls.slice(1).forEach(([, update]) => {
      expect(JSON.stringify(update)).not.toContain("A_ONLY");
    });
    expect(mutation).not.toHaveBeenCalled();
  });
});
