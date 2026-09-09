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

import { createContext, type ContextType, type ReactNode, useState } from "react";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  addRequestToCreateComponent: vi.fn(),
  applyClusterConfigs: vi.fn(),
  createCluster: vi.fn(),
  createSelectedServices: vi.fn(),
  deleteCluster: vi.fn(),
  deleteRepositoryVersion: vi.fn(),
  dispatch: vi.fn(),
  flushStateToDb: vi.fn(),
  getKerberosDescriptorArtifact: vi.fn(),
  getKerberosDescriptorProperties: vi.fn(),
  getAllClusters: vi.fn(),
  getClusterData: vi.fn(),
  getCreationDraftCluster: vi.fn(),
  getAllServices: vi.fn(),
  getHostComponentsDetails: vi.fn(),
  getManagedDependency: vi.fn(),
  getVersionDefinitions: vi.fn(),
  getServices: vi.fn(),
  handleBackImperitive: vi.fn(),
  handleNextImperitive: vi.fn(),
  kerberosMode: {
    error: "",
    isLoaded: true,
    isManualKerberos: false,
    kdcType: "mit-kdc",
    reload: vi.fn(),
  },
  postVersionDefinitionFile: vi.fn(),
  previewManagedDependencyPlan: vi.fn(),
  print: vi.fn(),
  registerHostToCluster: vi.fn(),
  saveAndEditKerberosData: vi.fn(),
  saveAs: vi.fn(),
  saveKerberosData: vi.fn(),
  downloadKerberosIdentitiesCsv: vi.fn(),
  updateRepoOSInfo: vi.fn(),
  updateService: vi.fn(),
  createManagedDependencyPlan: vi.fn(),
}));

vi.mock("../../api/chooseServicesApi", () => ({
  ChooseServicesApi: { getServices: mocks.getServices },
}));
vi.mock("../../api/versionsApi", () => ({
  default: {
    deleteRepositoryVersion: mocks.deleteRepositoryVersion,
    getVersionDefinitions: mocks.getVersionDefinitions,
    postVersionDefinitionFile: mocks.postVersionDefinitionFile,
    updateRepoOSInfo: mocks.updateRepoOSInfo,
  },
}));
vi.mock("../../api/clusterApi", () => ({
  default: {
    deleteCluster: mocks.deleteCluster,
    getAllClusters: mocks.getAllClusters,
    getClusterData: mocks.getClusterData,
  },
}));
vi.mock("../../api/clusterDeployment", () => ({
  default: {
    addRequestToCreateComponent: mocks.addRequestToCreateComponent,
    applyClusterConfigs: mocks.applyClusterConfigs,
    createCluster: mocks.createCluster,
    createSelectedServices: mocks.createSelectedServices,
    registerHostToCluster: mocks.registerHostToCluster,
  },
}));
vi.mock("../../api/workflowStateApi", () => ({
  default: {
    getCreationDraftCluster: mocks.getCreationDraftCluster,
  },
}));
vi.mock("../../api/serviceApi", () => ({
  ServiceApi: {
    getAllServices: mocks.getAllServices,
    updateService: mocks.updateService,
  },
}));
vi.mock("../../api/hostsApi", () => ({
  HostsApi: {
    getHostComponentsDetails: mocks.getHostComponentsDetails,
  },
}));
vi.mock("../../api/serviceDependenciesApi", () => ({
  default: {
    create: vi.fn(),
    createMany: mocks.createManagedDependencyPlan,
    get: mocks.getManagedDependency,
    previewPlan: mocks.previewManagedDependencyPlan,
    previewService: vi.fn(),
  },
}));
vi.mock("../../api/configGroupApi", () => ({
  default: { addConfigGroup: vi.fn(), updateConfigGroup: vi.fn() },
}));
vi.mock("../../api/kerberosApi", () => ({
  default: {
    downloadKerberosIdentitiesCsv: mocks.downloadKerberosIdentitiesCsv,
    getKerberosDescriptorArtifact: mocks.getKerberosDescriptorArtifact,
    getKerberosDescriptorProperties: mocks.getKerberosDescriptorProperties,
    saveAndEditKerberosData: mocks.saveAndEditKerberosData,
    saveKerberosData: mocks.saveKerberosData,
  },
}));
vi.mock("../../hooks/useKerberosMode", () => ({
  default: () => mocks.kerberosMode,
}));
vi.mock("../../hooks/useKDCSessionState", () => ({
  default: () => ({
    getKDCSessionState: (callback: () => void | Promise<void>) => callback(),
  }),
}));
vi.mock("file-saver", () => ({ saveAs: mocks.saveAs }));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({
    isNextEnabled,
    onNext,
    sideItems,
    step,
  }: {
    isNextEnabled: boolean;
    onNext: () => void;
    sideItems: ReactNode;
    step: { nextLabel: string };
  }) => (
    <>
      {sideItems}
      <button disabled={!isNextEnabled} onClick={onNext}>
        {step.nextLabel}
      </button>
    </>
  ),
}));

import Step8 from "./Step8";
import { deploymentInputSignature } from "./deploymentInputRecovery";

const CLUSTER_DRAFT_ID = "2e97ec6a-c03d-4c52-b910-1a58ae50f390";
const BINDING_ID = "11111111-1111-4111-8111-111111111111";
const OPERATION_ID = "22222222-2222-4222-8222-222222222222";
const OLD_OPERATION_ID = "33333333-3333-4333-8333-333333333333";

const deferred = <T,>() => {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, reject, resolve };
};

const managedHdfsPreview = {
  binding_id: BINDING_ID,
  client_config: {
    "core-site": { "fs.defaultFS": "hdfs://provider:8020" },
  },
  compatible: true,
  consumer: {
    cluster_id: 72,
    cluster_name: "cluster1",
    lifecycle: "INIT",
    planned_hbase_user: "hbase_a",
    scope: "SERVICE",
    service_name: "HBASE",
  },
  consumer_descriptor_fingerprint: "consumer-fingerprint",
  dependency_type: "HDFS",
  errors: [],
  namespace: {
    root_uri: "hdfs://provider:8020/apps/hbase/a",
    wal_uri: "hdfs://provider:8020/apps/hbase/a-wal",
  },
  preview_schema_version: 2,
  provider: {
    cluster_id: 9,
    cluster_name: "provider-a",
    service_name: "HDFS",
  },
  provider_fingerprint: "provider-fingerprint",
  snapshot_fingerprint: "snapshot-fingerprint",
};

const managedHdfsBinding = {
  binding_id: BINDING_ID,
  dependency_type: "HDFS",
  operation: {
    epoch: 1,
    kind: "CREATE",
    operation_id: OPERATION_ID,
    state: "QUEUED",
    target_snapshot_version: 1,
  },
  creation_attempt: {
    epoch: 1,
    kind: "CREATE",
    operation_id: OPERATION_ID,
    state: "QUEUED",
    target_snapshot_version: 1,
    target_snapshot: {
      consumer_fingerprint: "consumer-fingerprint",
      fingerprint: "snapshot-fingerprint",
      provider_fingerprint: "provider-fingerprint",
      schema_version: 2,
      version: 1,
    },
  },
  ownership: "managed",
  phase: "PROVIDER_PREPARING",
  provider: managedHdfsPreview.provider,
  snapshot: {
    consumer_fingerprint: "consumer-fingerprint",
    fingerprint: "snapshot-fingerprint",
    provider_fingerprint: "provider-fingerprint",
    schema_version: 2,
    version: 1,
  },
  state: "PROVISIONING",
};

const clusterCreationSteps = {
    NAME: { data: { clusterName: "cluster1" } },
    VERSION: {
      data: {
        operatingSystems: { "HDP-3.0": [] },
        selectedStack: { id: "HDP-3.0", stack_name: "HDP", stack_version: "3.0" },
        selectedVersion: {
          id: "HDP-3.0",
          repository_version: "3.0",
          stack_version: "3.0",
        },
        versionDefinitionSource: {
          type: "url",
          payload: { VersionDefinition: { version_url: "https://repo.invalid/vdf.xml" } },
        },
      },
    },
    HOSTS: { data: { installedHosts: [] } },
    HOST_STATUS: {
      data: { hosts: [{ bootStatus: "REGISTERED", name: "host1" }] },
    },
    SERVICES: {
      data: {
        services: {
          HDFS: {
            installed: false,
            selected: true,
            serviceName: "HDFS",
          },
        },
      },
    },
    MASTERS: { data: { mastersData: [] } },
    SLAVES_AND_CLIENTS: { data: { serviceComponents: [] } },
    CONFIGURATION: {
      data: {
        configProperties: {
          HDFS: {
            "core-site": {
              properties: {
                authentication: {
                  fileName: "core-site.xml",
                  isSecureConfig: true,
                  propertyAttributes: { type: "string" },
                  propertyName: "hadoop.security.authentication",
                  type: "core-site",
                  value: "custom-kerberos",
                },
              },
            },
          },
          MISC: { "Users and Groups": { properties: {} } },
        },
      },
    },
    REVIEW: { data: {} },
};

const managedHbaseSteps = () => {
  const steps = structuredClone(clusterCreationSteps) as any;
  steps.SERVICES.data = {
    managedDependencies: {
      HDFS: {
        mode: "managed",
        preview: managedHdfsPreview,
        provider: managedHdfsPreview.provider,
      },
      ZOOKEEPER: { mode: "local" },
    },
    services: {
      HBASE: { installed: false, selected: true, serviceName: "HBASE" },
    },
  };
  steps.CONFIGURATION.data.configProperties = {
    HBASE: {
      General: {
        properties: {
          root: {
            fileName: "hbase-site.xml",
            propertyName: "hbase.rootdir",
            serviceName: "HBASE",
            type: "hbase-site",
            value: "hdfs://local/apps/hbase",
          },
        },
      },
    },
    MISC: { "Users and Groups": { properties: {} } },
  };
  return steps;
};

const managedCreateRequest = (
  operationId: string,
  revision: number,
  providerFingerprint = "provider-fingerprint",
) => ({
  binding_id: BINDING_ID,
  dependency_type: "HDFS",
  draft: { id: CLUSTER_DRAFT_ID, revision },
  expected_consumer_descriptor_fingerprint: "consumer-fingerprint",
  expected_provider_fingerprint: providerFingerprint,
  expected_snapshot_fingerprint: "snapshot-fingerprint",
  operation_id: operationId,
  preview_schema_version: 2,
  provider: { cluster_id: 9, service_name: "HDFS" },
});

const managedAttemptSteps = (
  providerFingerprint = "provider-fingerprint",
) => {
  const steps = managedHbaseSteps();
  const request = managedCreateRequest(OLD_OPERATION_ID, 1, providerFingerprint);
  steps.REVIEW.data = {
    clusterCreationAttempted: true,
    clusterId: 72,
    managedDependencyMaterializations: {
      HDFS: {
        attempts: [{ operationId: OLD_OPERATION_ID, request }],
        bindingId: BINDING_ID,
        clusterId: 72,
        clusterName: "cluster1",
        dependencyType: "HDFS",
        operationId: OLD_OPERATION_ID,
        request,
      },
    },
    repositoryVersionId: "101",
  };
  return steps;
};

function wizardState(
  wizardName: "clusterCreation" | "addService",
  steps = clusterCreationSteps,
) {
  return {
    [`${wizardName}Steps`]: structuredClone(steps),
  };
}

function renderStep({
  isKerberosEnabled = false,
  wizardName = "clusterCreation",
  steps = clusterCreationSteps,
}: {
  isKerberosEnabled?: boolean;
  wizardName?: "clusterCreation" | "addService";
  steps?: typeof clusterCreationSteps;
} = {}) {
  let revision = 4;
  const flushStateToDb = async (...args: unknown[]) => {
    const result = await mocks.flushStateToDb(...args);
    revision += 1;
    return result;
  };
  const WizardContext = createContext({} as any);
  function Harness() {
    const [state, setState] = useState(() => wizardState(wizardName, steps));
    const dispatch = (action: any) => {
      mocks.dispatch(action);
      if (action.type === "STORE INFORMATION" && action.payload?.step) {
        setState((current: any) => ({
          ...current,
          [`${wizardName}Steps`]: {
            ...current[`${wizardName}Steps`],
            [action.payload.step]: {
              data: action.payload.data,
            },
          },
        }));
      }
    };
    const storeStepDataAndFlush = async (
      step: string,
      data: Record<string, unknown>,
    ) => {
      dispatch({ type: "STORE_INFORMATION", payload: { step, data } });
      await flushStateToDb("checkpoint", -1, "DEPLOY_PREP");
      return revision;
    };
    const contextValue = {
      draftId: CLUSTER_DRAFT_ID,
      dispatch,
      flushStateToDb,
      getDraftRevision: () => revision,
      getWorkflowRevision: () => revision,
      state,
      storeStepDataAndFlush,
      installedServices: [],
      stepWizardUtilities: {
        currentStep: { name: "REVIEW" },
        handleBackImperitive: mocks.handleBackImperitive,
        handleNextImperitive: mocks.handleNextImperitive,
        jumpToStep: vi.fn(),
      },
    };
    return (
      <WizardContext.Provider value={contextValue}>
        <Step8 wizardName={wizardName} />
      </WizardContext.Provider>
    );
  }
  return render(
    <AppContext.Provider value={{
      clusterName: "cluster1",
      cluster: { cluster_id: 72, stack: "HDP", versionNum: "3.0" },
      isKerberosEnabled,
    } as ContextType<typeof AppContext>}>
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <Harness />
      </ContextWrapper.Provider>
    </AppContext.Provider>,
  );
}

describe("cluster deployment Review", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(window, "print", {
      configurable: true,
      value: mocks.print,
    });
    mocks.flushStateToDb.mockResolvedValue(undefined);
    mocks.getAllClusters.mockResolvedValue({ items: [] });
    mocks.getAllServices.mockResolvedValue({ items: [] });
    mocks.getHostComponentsDetails.mockResolvedValue({ items: [] });
    mocks.getCreationDraftCluster.mockRejectedValue({ response: { status: 404 } });
    mocks.getClusterData.mockResolvedValue({
      items: [{ Clusters: { cluster_id: 72, cluster_name: "cluster1" } }],
    });
    mocks.getVersionDefinitions.mockResolvedValue({ items: [] });
    mocks.getServices.mockResolvedValue({
      items: [{ StackServices: { service_name: "HDFS" }, components: [] }],
    });
    mocks.postVersionDefinitionFile.mockResolvedValue({
      resources: [{
        VersionDefinition: {
          id: "101",
          stack_name: "HDP",
          stack_version: "3.0",
        },
      }],
    });
    mocks.updateRepoOSInfo.mockResolvedValue({});
    mocks.createSelectedServices.mockResolvedValue({});
    mocks.getManagedDependency.mockRejectedValue({ response: { status: 404 } });
    mocks.previewManagedDependencyPlan.mockResolvedValue({ items: [managedHdfsPreview] });
    mocks.createManagedDependencyPlan.mockImplementation(async (
      _clusterName: string,
      requests: Array<{ operation_id: string }>,
    ) => requests.map((request) => ({
      ...managedHdfsBinding,
      operation: { ...managedHdfsBinding.operation, operation_id: request.operation_id },
      creation_attempt: {
        ...managedHdfsBinding.creation_attempt,
        operation_id: request.operation_id,
      },
    })));
    mocks.downloadKerberosIdentitiesCsv.mockResolvedValue(
      "principal,keytab\nservice/host@EXAMPLE.COM,/etc/security/keytabs/service.keytab",
    );
    mocks.getKerberosDescriptorArtifact.mockResolvedValue({ artifact_data: {} });
    mocks.getKerberosDescriptorProperties.mockResolvedValue({
      KerberosDescriptor: {
        kerberos_descriptor: {
          identities: [],
          services: [{
            name: "HDFS",
            configurations: [{
              "core-site": { "hadoop.security.authentication": "kerberos" },
            }],
          }],
        },
      },
    });
    mocks.kerberosMode.error = "";
    mocks.kerberosMode.isLoaded = true;
    mocks.kerberosMode.isManualKerberos = false;
    mocks.kerberosMode.kdcType = "mit-kdc";
    mocks.applyClusterConfigs.mockResolvedValue({});
    mocks.registerHostToCluster.mockResolvedValue({});
    mocks.updateService.mockResolvedValue({ Requests: { id: 41 } });
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("retains a custom VDF source, blocks install on failure, and resumes completed stages", async () => {
    mocks.createCluster
      .mockRejectedValueOnce(new Error("Cluster creation failed"))
      .mockResolvedValueOnce({ Clusters: { cluster_id: 72 } });
    renderStep();
    await screen.findByRole("button", { name: "DEPLOY" });

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    expect(await screen.findByText("Cluster creation failed")).toBeTruthy();
    expect(mocks.updateService).not.toHaveBeenCalled();
    expect(mocks.postVersionDefinitionFile).toHaveBeenCalledWith(
      {
        VersionDefinition: { version_url: "https://repo.invalid/vdf.xml" },
        operating_systems: [],
      },
      {},
    );

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.postVersionDefinitionFile).toHaveBeenCalledOnce();
    expect(mocks.createCluster).toHaveBeenCalledTimes(2);
    expect(mocks.deleteCluster).not.toHaveBeenCalled();
    expect(mocks.deleteRepositoryVersion).not.toHaveBeenCalled();
    expect(mocks.updateService).toHaveBeenCalledOnce();
    expect(mocks.flushStateToDb).toHaveBeenLastCalledWith(
      "next",
      -1,
      "CLUSTER_INSTALLING_3",
    );
  });

  it("applies reviewed values, persists the exact create request, and pauses at provider preparation", async () => {
    mocks.createCluster.mockResolvedValue({ Clusters: { cluster_id: 72 } });
    mocks.previewManagedDependencyPlan.mockResolvedValue({ items: [managedHdfsPreview] });
    vi.spyOn(crypto, "randomUUID").mockReturnValue(OPERATION_ID);
    renderStep({ steps: managedHbaseSteps() });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/Provider preparation has started/)).toBeTruthy();
    expect(mocks.applyClusterConfigs).toHaveBeenCalledWith(
      "cluster1",
      expect.arrayContaining([
        expect.objectContaining({
          Clusters: expect.objectContaining({
            desired_config: expect.arrayContaining([
              expect.objectContaining({
                type: "hbase-site",
                properties: expect.objectContaining({
                  "hbase.rootdir": "hdfs://provider:8020/apps/hbase/a",
                  "hbase.wal.dir": "hdfs://provider:8020/apps/hbase/a-wal",
                }),
              }),
            ]),
          }),
        }),
      ]),
    );
    expect(mocks.applyClusterConfigs.mock.invocationCallOrder[0])
      .toBeLessThan(mocks.previewManagedDependencyPlan.mock.invocationCallOrder[0]);
    expect(mocks.previewManagedDependencyPlan).toHaveBeenCalledWith(
      expect.objectContaining({
        consumer: expect.objectContaining({ scope: "DRAFT" }),
        selections: [{
          binding_id: BINDING_ID,
          dependency_type: "HDFS",
          provider: { cluster_id: 9, service_name: "HDFS" },
        }],
      }),
    );
    expect(mocks.createManagedDependencyPlan).toHaveBeenCalledWith(
      "cluster1",
      [expect.objectContaining({
        binding_id: BINDING_ID,
        dependency_type: "HDFS",
        draft: { id: CLUSTER_DRAFT_ID, revision: expect.any(Number) },
        expected_consumer_descriptor_fingerprint: "consumer-fingerprint",
        expected_provider_fingerprint: "provider-fingerprint",
        expected_snapshot_fingerprint: "snapshot-fingerprint",
        operation_id: OPERATION_ID,
        preview_schema_version: 2,
        provider: { cluster_id: 9, service_name: "HDFS" },
      })],
    );
    const persistedRequestCall = mocks.dispatch.mock.calls.findIndex(
      ([action]) => action.payload?.data?.managedDependencyMaterializations?.HDFS?.request,
    );
    expect(persistedRequestCall).toBeGreaterThanOrEqual(0);
    expect(mocks.dispatch.mock.invocationCallOrder[persistedRequestCall])
      .toBeLessThan(mocks.createManagedDependencyPlan.mock.invocationCallOrder[0]);
    expect(mocks.updateService).not.toHaveBeenCalled();
  });

  it("reconciles a lost binding response by exact binding and operation identity", async () => {
    mocks.createCluster.mockResolvedValue({ Clusters: { cluster_id: 72 } });
    mocks.createManagedDependencyPlan.mockRejectedValue(new Error("response lost"));
    mocks.getManagedDependency.mockResolvedValue(managedHdfsBinding);
    vi.spyOn(crypto, "randomUUID").mockReturnValue(OPERATION_ID);
    renderStep({ steps: managedHbaseSteps() });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/Provider preparation has started/)).toBeTruthy();
    expect(mocks.createManagedDependencyPlan).toHaveBeenCalledOnce();
    expect(mocks.getManagedDependency).toHaveBeenCalledWith("cluster1", BINDING_ID);
    expect(mocks.previewManagedDependencyPlan).toHaveBeenCalledOnce();
    expect(mocks.updateService).not.toHaveBeenCalled();
  });

  it("reapproves a pre-POST attempt at the current draft revision without changing its binding", async () => {
    mocks.getCreationDraftCluster.mockResolvedValue({
      cluster_id: 72,
      cluster_name: "cluster1",
    });
    vi.spyOn(crypto, "randomUUID").mockReturnValue(OPERATION_ID);
    renderStep({ steps: managedAttemptSteps() });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/Provider preparation has started/)).toBeTruthy();
    expect(mocks.createManagedDependencyPlan).toHaveBeenCalledOnce();
    expect(mocks.createManagedDependencyPlan.mock.calls[0][1][0]).toMatchObject({
      binding_id: BINDING_ID,
      draft: { id: CLUSTER_DRAFT_ID, revision: expect.any(Number) },
      operation_id: OPERATION_ID,
    });
    expect(mocks.createManagedDependencyPlan.mock.calls[0][1][0].draft.revision)
      .toBeGreaterThan(1);
    const attemptHistory = mocks.dispatch.mock.calls
      .map(([action]) => action.payload?.data?.managedDependencyMaterializations?.HDFS?.attempts)
      .filter(Boolean)
      .find((attempts) => attempts.length === 2);
    expect(attemptHistory?.map(({ operationId }: any) => operationId)).toEqual([
      OLD_OPERATION_ID,
      OPERATION_ID,
    ]);
  });

  it("reconciles the new attempt when CREATE wins after the preflight GET", async () => {
    const missing = { response: { status: 404 } };
    const newBinding = {
      ...managedHdfsBinding,
      operation: { ...managedHdfsBinding.operation, operation_id: OPERATION_ID },
    };
    mocks.getCreationDraftCluster.mockResolvedValue({
      cluster_id: 72,
      cluster_name: "cluster1",
    });
    mocks.getManagedDependency
      .mockRejectedValueOnce(missing)
      .mockRejectedValueOnce(missing)
      .mockResolvedValueOnce(newBinding);
    mocks.createManagedDependencyPlan.mockRejectedValueOnce({
      response: { data: { code: "BINDING_ID_CONFLICT" }, status: 409 },
    });
    vi.spyOn(crypto, "randomUUID").mockReturnValue(OPERATION_ID);
    renderStep({ steps: managedAttemptSteps() });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/Provider preparation has started/)).toBeTruthy();
    expect(mocks.createManagedDependencyPlan).toHaveBeenCalledWith(
      "cluster1",
      [expect.objectContaining({ operation_id: OPERATION_ID })],
    );
    expect(mocks.getManagedDependency).toHaveBeenCalledTimes(3);
  });

  it("shows an older winning attempt whose semantic fingerprints differ", async () => {
    const missing = { response: { status: 404 } };
    const oldBinding = {
      ...managedHdfsBinding,
      operation: { ...managedHdfsBinding.operation, operation_id: OLD_OPERATION_ID },
      creation_attempt: {
        ...managedHdfsBinding.creation_attempt,
        operation_id: OLD_OPERATION_ID,
        target_snapshot: {
          ...managedHdfsBinding.creation_attempt.target_snapshot,
          provider_fingerprint: "older-provider-fingerprint",
        },
      },
      snapshot: {
        ...managedHdfsBinding.snapshot,
        provider_fingerprint: "older-provider-fingerprint",
      },
    };
    mocks.getCreationDraftCluster.mockResolvedValue({
      cluster_id: 72,
      cluster_name: "cluster1",
    });
    mocks.getManagedDependency
      .mockRejectedValueOnce(missing)
      .mockResolvedValueOnce(oldBinding);
    mocks.previewManagedDependencyPlan.mockRejectedValueOnce({
      response: { data: { code: "BINDING_ID_UNAVAILABLE" }, status: 409 },
    });
    renderStep({ steps: managedAttemptSteps("older-provider-fingerprint") });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/earlier provider request completed with different approved settings/))
      .toBeTruthy();
    expect(mocks.applyClusterConfigs).not.toHaveBeenCalled();
    expect(mocks.createManagedDependencyPlan).not.toHaveBeenCalled();
    expect(mocks.updateService).not.toHaveBeenCalled();
  });

  it("retains same-workflow INIT HBase intent without creating or adopting another service", async () => {
    const steps = managedHbaseSteps();
    const request = [{ ServiceInfo: { service_name: "HBASE" } }];
    steps.REVIEW.data = {
      serviceCreationIntent: {
        clusterId: 72,
        clusterName: "cluster1",
        request,
        serviceNames: ["HBASE"],
      },
    };
    mocks.getAllServices.mockResolvedValue({
      items: [{ ServiceInfo: { service_name: "HBASE", state: "INIT" } }],
    });
    vi.spyOn(crypto, "randomUUID").mockReturnValue(OPERATION_ID);
    renderStep({ wizardName: "addService", steps });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/Provider preparation has started/)).toBeTruthy();
    expect(mocks.createSelectedServices).not.toHaveBeenCalled();
    expect(mocks.previewManagedDependencyPlan).toHaveBeenCalledWith(
      expect.objectContaining({
        consumer: expect.objectContaining({ scope: "SERVICE_PLAN", cluster_id: 72 }),
      }),
    );
    expect(mocks.createManagedDependencyPlan.mock.calls[0][1][0]).not.toHaveProperty("draft");
  });

  it("restores the original master placement after an assignment response lost its checkpoint", async () => {
    const restoredSteps = structuredClone(clusterCreationSteps) as any;
    const savedMasters = [{
      masterServices: [{
        component: "NAMENODE",
        hostName: "old-host",
        isInstalled: false,
      }],
    }];
    restoredSteps.MASTERS.data.mastersData = [{
      masterServices: [{
        component: "NAMENODE",
        hostName: "host1",
        isInstalled: false,
      }],
    }];
    const serviceCatalog = [{
      StackServices: { service_name: "HDFS" },
      components: [{
        StackServiceComponents: {
          cardinality: "1",
          component_name: "NAMENODE",
          is_master: true,
        },
      }],
    }];
    mocks.getServices.mockResolvedValue({ items: serviceCatalog });
    mocks.getHostComponentsDetails.mockResolvedValue({
      items: [{
        Hosts: { host_name: "old-host" },
        host_components: [{ HostRoles: {
          component_name: "NAMENODE",
          host_name: "old-host",
          state: "INIT",
        } }],
      }],
    });
    mocks.getCreationDraftCluster.mockResolvedValue({
      cluster_id: 72,
      cluster_name: "cluster1",
    });
    restoredSteps.REVIEW.data = {
      clusterCreationAttempted: true,
      clusterId: 72,
      completedOperationIds: [
        "validate-cluster-target",
        "resolve-repository-version",
        "create-cluster",
        "create-services",
        "complete-managed-dependency-previews-local",
        "apply-configurations-local",
        "create-components",
        "create-configuration-groups",
        "register-hosts",
        "register-slaves-clients",
      ],
      deploymentInputSignatures: {
        configuration: await deploymentInputSignature({ old: "configuration" }),
        configGroups: await deploymentInputSignature([]),
        hosts: await deploymentInputSignature({
          installed: [],
          registered: [{ bootStatus: "REGISTERED", name: "host1" }],
        }),
        managedDependencies: await deploymentInputSignature(JSON.stringify([])),
        masters: await deploymentInputSignature(savedMasters),
        serviceComponents: await deploymentInputSignature(serviceCatalog),
        services: await deploymentInputSignature([{ installed: false, serviceName: "HDFS" }]),
        slavesAndClients: await deploymentInputSignature([]),
      },
      deploymentTopologyIntent: {
        masters: savedMasters,
        slavesAndClients: [],
      },
      hostComponentAssignmentAttempts: {
        "masters:NAMENODE": [{
          component: "NAMENODE",
          hostNames: ["old-host"],
          request: {
            RequestInfo: { query: "Hosts/host_name=old-host" },
            Body: { host_components: [{ HostRoles: { component_name: "NAMENODE" } }] },
          },
          targetClusterName: "cluster1",
          topologyInput: "masters",
        }],
      },
      repositoryVersionId: "101",
    };

    renderStep({ steps: restoredSteps });
    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/already use the saved host assignments/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", {
      name: "Restore and review saved assignments",
    }));
    await waitFor(() => expect(mocks.dispatch).toHaveBeenCalledWith(expect.objectContaining({
      payload: expect.objectContaining({
        data: expect.objectContaining({ mastersData: savedMasters }),
        step: "MASTERS",
      }),
    })));
    expect(mocks.handleNextImperitive).not.toHaveBeenCalled();
    expect(mocks.applyClusterConfigs).not.toHaveBeenCalled();
    expect(mocks.registerHostToCluster).not.toHaveBeenCalled();
    expect(mocks.createSelectedServices).not.toHaveBeenCalled();
    expect(mocks.addRequestToCreateComponent).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.registerHostToCluster).not.toHaveBeenCalled();
  });

  it("skips exact components and host assignments after a pre-checkpoint response loss", async () => {
    const steps = structuredClone(clusterCreationSteps) as any;
    steps.MASTERS.data.mastersData = [{
      masterServices: [{
        component: "NAMENODE",
        hostName: "host1",
        isInstalled: false,
      }],
    }];
    steps.REVIEW.data = {
      serviceCreationIntent: {
        clusterId: 72,
        clusterName: "cluster1",
        request: [{ ServiceInfo: { service_name: "HDFS" } }],
        serviceNames: ["HDFS"],
      },
    };
    mocks.getServices.mockResolvedValue({ items: [{
      StackServices: { service_name: "HDFS" },
      components: [{ StackServiceComponents: {
        cardinality: "1",
        component_name: "NAMENODE",
        is_master: true,
      } }],
    }] });
    mocks.getAllServices.mockResolvedValue({ items: [{
      ServiceInfo: { service_name: "HDFS", state: "INIT" },
      components: [{ ServiceComponentInfo: { component_name: "NAMENODE" } }],
    }] });
    mocks.getHostComponentsDetails.mockResolvedValue({ items: [{
      Hosts: { host_name: "host1" },
      host_components: [{ HostRoles: {
        component_name: "NAMENODE",
        host_name: "host1",
        state: "INIT",
      } }],
    }] });

    renderStep({ wizardName: "addService", steps });
    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.createSelectedServices).not.toHaveBeenCalled();
    expect(mocks.addRequestToCreateComponent).not.toHaveBeenCalled();
    expect(mocks.registerHostToCluster).not.toHaveBeenCalled();
  });

  it("does not create components after the deployment scope unmounts during reconciliation", async () => {
    const componentRead = deferred<any>();
    mocks.getAllServices
      .mockResolvedValueOnce({ items: [] })
      .mockImplementationOnce(() => componentRead.promise);
    mocks.getServices.mockResolvedValue({ items: [{
      StackServices: { service_name: "HDFS" },
      components: [{ StackServiceComponents: {
        cardinality: "1+",
        component_name: "DATANODE",
        is_slave: true,
        service_name: "HDFS",
      } }],
    }] });
    const view = renderStep({ wizardName: "addService" });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.getAllServices).toHaveBeenCalledTimes(2));
    view.unmount();
    await act(async () => {
      componentRead.resolve({ items: [] });
      await componentRead.promise;
    });

    expect(mocks.addRequestToCreateComponent).not.toHaveBeenCalled();
  });

  it("adds a new HBase RegionServer to an existing host without touching unrelated clients", async () => {
    const steps = structuredClone(clusterCreationSteps) as any;
    steps.HOSTS.data.installedHosts = ["host1"];
    steps.SERVICES.data.services = {
      HBASE: { installed: false, selected: true, serviceName: "HBASE" },
    };
    steps.SLAVES_AND_CLIENTS.data.serviceComponents = [{
      hostname: "host1",
      checkboxes: [
        {
          checked: true,
          isDisabled: false,
          isInstalled: false,
          label: "HBASE_REGIONSERVER",
        },
        {
          checked: true,
          isDisabled: true,
          isInstalled: true,
          label: "HDFS_CLIENT",
        },
      ],
    }];
    const request = [{ ServiceInfo: { service_name: "HBASE" } }];
    steps.REVIEW.data = {
      serviceCreationIntent: {
        clusterId: 72,
        clusterName: "cluster1",
        request,
        serviceNames: ["HBASE"],
      },
    };
    mocks.getServices.mockResolvedValue({ items: [{
      StackServices: { service_name: "HBASE" },
      components: [{ StackServiceComponents: {
        cardinality: "1+",
        component_name: "HBASE_REGIONSERVER",
        is_slave: true,
        service_name: "HBASE",
      } }],
    }] });
    mocks.getAllServices.mockResolvedValue({ items: [{
      ServiceInfo: { service_name: "HBASE", state: "INIT" },
      components: [{ ServiceComponentInfo: { component_name: "HBASE_REGIONSERVER" } }],
    }] });
    mocks.getHostComponentsDetails.mockResolvedValue({ items: [{
      Hosts: { host_name: "host1" },
      host_components: [{ HostRoles: {
        component_name: "HDFS_CLIENT",
        host_name: "host1",
        state: "INSTALLED",
      } }],
    }] });

    renderStep({ wizardName: "addService", steps });
    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.registerHostToCluster).toHaveBeenCalledOnce();
    expect(mocks.registerHostToCluster).toHaveBeenCalledWith(
      "cluster1",
      expect.objectContaining({
        Body: { host_components: [{ HostRoles: {
          component_name: "HBASE_REGIONSERVER",
        } }] },
        RequestInfo: { query: "Hosts/host_name=host1" },
      }),
    );
    const assignmentCheckpoint = mocks.dispatch.mock.calls.findIndex(([action]) =>
      action?.payload?.data?.hostComponentAssignmentAttempts?.[
        "slavesAndClients:HBASE_REGIONSERVER"
      ]?.[0]?.request?.RequestInfo?.query === "Hosts/host_name=host1");
    expect(assignmentCheckpoint).toBeGreaterThanOrEqual(0);
    expect(mocks.dispatch.mock.invocationCallOrder[assignmentCheckpoint])
      .toBeLessThan(mocks.registerHostToCluster.mock.invocationCallOrder[0]);
    expect(mocks.flushStateToDb.mock.invocationCallOrder.some((order) =>
      order > mocks.dispatch.mock.invocationCallOrder[assignmentCheckpoint]
      && order < mocks.registerHostToCluster.mock.invocationCallOrder[0])).toBe(true);
    expect(JSON.stringify(mocks.registerHostToCluster.mock.calls)).not.toContain("HDFS_CLIENT");
  });

  it("checkpoints and retries only the missing hosts after a partial assignment write", async () => {
    const steps = structuredClone(clusterCreationSteps) as any;
    steps.HOSTS.data.installedHosts = ["host1", "host2"];
    steps.SERVICES.data.services = {
      HBASE: { installed: false, selected: true, serviceName: "HBASE" },
    };
    steps.SLAVES_AND_CLIENTS.data.serviceComponents = ["host1", "host2"].map(
      (hostname) => ({
        hostname,
        checkboxes: [{
          checked: true,
          isDisabled: false,
          isInstalled: false,
          label: "HBASE_REGIONSERVER",
        }],
      }),
    );
    const originalRequest = {
      RequestInfo: { query: "Hosts/host_name=host1|Hosts/host_name=host2" },
      Body: { host_components: [{ HostRoles: {
        component_name: "HBASE_REGIONSERVER",
      } }] },
    };
    steps.REVIEW.data = {
      hostComponentAssignmentAttempts: {
        "slavesAndClients:HBASE_REGIONSERVER": [{
          component: "HBASE_REGIONSERVER",
          hostNames: ["host1", "host2"],
          request: originalRequest,
          targetClusterName: "cluster1",
          topologyInput: "slavesAndClients",
        }],
      },
      serviceCreationIntent: {
        clusterId: 72,
        clusterName: "cluster1",
        request: [{ ServiceInfo: { service_name: "HBASE" } }],
        serviceNames: ["HBASE"],
      },
    };
    mocks.getServices.mockResolvedValue({ items: [{
      StackServices: { service_name: "HBASE" },
      components: [{ StackServiceComponents: {
        cardinality: "1+",
        component_name: "HBASE_REGIONSERVER",
        is_slave: true,
        service_name: "HBASE",
      } }],
    }] });
    mocks.getAllServices.mockResolvedValue({ items: [{
      ServiceInfo: { service_name: "HBASE", state: "INIT" },
      components: [{ ServiceComponentInfo: { component_name: "HBASE_REGIONSERVER" } }],
    }] });
    mocks.getHostComponentsDetails.mockResolvedValue({ items: [{
      Hosts: { host_name: "host1" },
      host_components: [{ HostRoles: {
        component_name: "HBASE_REGIONSERVER",
        host_name: "host1",
        state: "INIT",
      } }],
    }] });

    renderStep({ wizardName: "addService", steps });
    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
    expect(mocks.registerHostToCluster).toHaveBeenCalledOnce();
    expect(mocks.registerHostToCluster).toHaveBeenCalledWith("cluster1", {
      RequestInfo: { query: "Hosts/host_name=host2" },
      Body: { host_components: [{ HostRoles: {
        component_name: "HBASE_REGIONSERVER",
      } }] },
    });
    const savedAttempts = mocks.dispatch.mock.calls
      .map(([action]) => action?.payload?.data?.hostComponentAssignmentAttempts?.[
        "slavesAndClients:HBASE_REGIONSERVER"
      ])
      .find((attempts) => attempts?.length === 2);
    expect(savedAttempts?.[0]?.request).toEqual(originalRequest);
    expect(savedAttempts?.[1]?.request?.RequestInfo?.query).toBe("Hosts/host_name=host2");
  });

  it("does not register a host component when its scoped attempt checkpoint fails", async () => {
    const steps = structuredClone(clusterCreationSteps) as any;
    steps.SERVICES.data.services = {
      HBASE: { installed: false, selected: true, serviceName: "HBASE" },
    };
    steps.SLAVES_AND_CLIENTS.data.serviceComponents = [{
      hostname: "host1",
      checkboxes: [{
        checked: true,
        isDisabled: false,
        isInstalled: false,
        label: "HBASE_REGIONSERVER",
      }],
    }];
    steps.REVIEW.data = {
      serviceCreationIntent: {
        clusterId: 72,
        clusterName: "cluster1",
        request: [{ ServiceInfo: { service_name: "HBASE" } }],
        serviceNames: ["HBASE"],
      },
    };
    mocks.getServices.mockResolvedValue({ items: [{
      StackServices: { service_name: "HBASE" },
      components: [{ StackServiceComponents: {
        cardinality: "1+",
        component_name: "HBASE_REGIONSERVER",
        is_slave: true,
        service_name: "HBASE",
      } }],
    }] });
    mocks.getAllServices.mockResolvedValue({ items: [{
      ServiceInfo: { service_name: "HBASE", state: "INIT" },
      components: [{ ServiceComponentInfo: { component_name: "HBASE_REGIONSERVER" } }],
    }] });
    mocks.getHostComponentsDetails.mockResolvedValue({ items: [] });
    mocks.flushStateToDb.mockImplementation(() => {
      const latest = mocks.dispatch.mock.calls.at(-1)?.[0];
      return latest?.payload?.data?.hostComponentAssignmentAttempts
        ? Promise.reject(new Error("Attempt checkpoint failed"))
        : Promise.resolve();
    });

    renderStep({ wizardName: "addService", steps });
    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText("Attempt checkpoint failed")).toBeTruthy();
    expect(mocks.registerHostToCluster).not.toHaveBeenCalled();
    expect(mocks.handleNextImperitive).not.toHaveBeenCalled();
  });

  it("creates a new target without deleting an existing cluster", async () => {
    mocks.getAllClusters.mockResolvedValue({
      items: [{ Clusters: { cluster_id: 11, cluster_name: "clusterA" } }],
    });
    mocks.createCluster.mockResolvedValue({ Clusters: { cluster_id: 72 } });
    renderStep();

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());

    expect(mocks.createCluster).toHaveBeenCalledWith("cluster1", {
      Clusters: {
        creation_draft_id: CLUSTER_DRAFT_ID,
        version: "HDP-3.0",
      },
    });
    expect(mocks.deleteCluster).not.toHaveBeenCalled();
    expect(mocks.deleteRepositoryVersion).not.toHaveBeenCalled();
  });

  it("blocks an exact cluster-name collision without mutating either resource", async () => {
    mocks.getAllClusters.mockResolvedValue({
      items: [{ Clusters: { cluster_id: 72, cluster_name: "cluster1" } }],
    });
    renderStep();

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/A cluster named "cluster1" already exists/)).toBeTruthy();
    expect(mocks.getVersionDefinitions).not.toHaveBeenCalled();
    expect(mocks.postVersionDefinitionFile).not.toHaveBeenCalled();
    expect(mocks.createCluster).not.toHaveBeenCalled();
    expect(mocks.deleteCluster).not.toHaveBeenCalled();
    expect(mocks.deleteRepositoryVersion).not.toHaveBeenCalled();
  });

  it("requires stripped credentials to be re-entered before deployment", async () => {
    const restoredSteps = structuredClone(clusterCreationSteps);
    (restoredSteps.HOSTS.data as any).requires_reentry = true;
    renderStep({ steps: restoredSteps });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/Re-enter the missing sensitive values in Install Options/)).toBeTruthy();
    expect(mocks.getAllClusters).not.toHaveBeenCalled();
    expect(mocks.createCluster).not.toHaveBeenCalled();
  });

  it("reuses an identical repository definition without updating it", async () => {
    mocks.getVersionDefinitions.mockResolvedValue({
      items: [{
        VersionDefinition: {
          id: "101",
          repository_version: "3.0",
          stack_name: "HDP",
          stack_version: "3.0",
        },
        operating_systems: [],
      }],
    });
    mocks.createCluster.mockResolvedValue({ Clusters: { cluster_id: 72 } });
    renderStep();

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());

    expect(mocks.postVersionDefinitionFile).not.toHaveBeenCalled();
    expect(mocks.updateRepoOSInfo).not.toHaveBeenCalled();
    expect(mocks.createSelectedServices).toHaveBeenCalledWith("cluster1", [{
      ServiceInfo: {
        desired_repository_version_id: "101",
        service_name: "HDFS",
      },
    }]);
  });

  it("recovers the exact draft-owned cluster after the create response is lost", async () => {
    mocks.getCreationDraftCluster
      .mockRejectedValueOnce({ response: { status: 404 } })
      .mockResolvedValueOnce({ cluster_id: 84, cluster_name: "cluster1" });
    mocks.createCluster.mockRejectedValue(new Error("The response was lost"));
    renderStep();

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());

    expect(mocks.createCluster).toHaveBeenCalledOnce();
    expect(mocks.getCreationDraftCluster).toHaveBeenCalledTimes(2);
    expect(mocks.flushStateToDb).toHaveBeenCalledWith(
      "checkpoint",
      -1,
      "CLUSTER_DEPLOY_PREP_2",
    );
    expect(mocks.deleteCluster).not.toHaveBeenCalled();
  });

  it("does not adopt an unrelated same-name cluster when draft recovery is absent", async () => {
    mocks.createCluster.mockRejectedValue(new Error("The response was lost"));
    renderStep();

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText("The response was lost")).toBeTruthy();
    expect(mocks.getClusterData).not.toHaveBeenCalled();
    expect(mocks.createSelectedServices).not.toHaveBeenCalled();
  });

  it("resumes a committed create through the draft association before checking its name", async () => {
    const restoredSteps = structuredClone(clusterCreationSteps);
    (restoredSteps.REVIEW.data as any) = {
      clusterCreationAttempted: true,
      completedOperationIds: [
        "resolve-repository-version",
        "create-cluster",
      ],
      repositoryVersionId: "101",
    };
    mocks.getCreationDraftCluster.mockResolvedValue({
      cluster_id: 84,
      cluster_name: "cluster1",
    });
    mocks.getAllClusters.mockResolvedValue({
      items: [{ Clusters: { cluster_id: 84, cluster_name: "cluster1" } }],
    });
    renderStep({ steps: restoredSteps });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());

    expect(mocks.getCreationDraftCluster).toHaveBeenCalledBefore(mocks.getAllClusters);
    expect(mocks.createCluster).not.toHaveBeenCalled();
    expect(mocks.createSelectedServices).toHaveBeenCalledWith("cluster1", [{
      ServiceInfo: {
        desired_repository_version_id: "101",
        service_name: "HDFS",
      },
    }]);
  });

  it("reconciles a completed create before continuing with service mutations", async () => {
    const restoredSteps = structuredClone(clusterCreationSteps);
    (restoredSteps.REVIEW.data as any) = {
      clusterCreationAttempted: true,
      completedOperationIds: [
        "validate-cluster-target",
        "resolve-repository-version",
        "create-cluster",
      ],
      repositoryVersionId: "101",
    };
    mocks.getCreationDraftCluster.mockResolvedValue({
      cluster_id: 84,
      cluster_name: "cluster1",
    });
    renderStep({ steps: restoredSteps });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());

    expect(mocks.getAllClusters).not.toHaveBeenCalled();
    expect(mocks.createCluster).not.toHaveBeenCalled();
    expect(mocks.getCreationDraftCluster)
      .toHaveBeenCalledBefore(mocks.createSelectedServices);
  });

  it("rejects a replacement cluster when a completed artifact lost its draft association", async () => {
    const restoredSteps = structuredClone(clusterCreationSteps);
    (restoredSteps.REVIEW.data as any) = {
      clusterId: 72,
      completedOperationIds: ["create-cluster"],
    };
    mocks.getAllClusters.mockResolvedValue({
      items: [{ Clusters: { cluster_id: 73, cluster_name: "cluster1" } }],
    });
    renderStep({ steps: restoredSteps });

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/owned by this installation draft is no longer available/))
      .toBeTruthy();
    expect(mocks.getAllClusters).not.toHaveBeenCalled();
    expect(mocks.createCluster).not.toHaveBeenCalled();
  });

  it("reports a repository conflict without changing the shared definition", async () => {
    mocks.getVersionDefinitions.mockResolvedValue({
      items: [{
        VersionDefinition: {
          id: "101",
          repository_version: "3.0",
          stack_name: "HDP",
          stack_version: "3.0",
        },
        operating_systems: [{
          OperatingSystems: { os_type: "redhat8" },
          repositories: [{ Repositories: {
            base_url: "https://other.example/hdp",
            repo_id: "HDP",
            repo_name: "HDP",
          } }],
        }],
      }],
    });
    renderStep();

    fireEvent.click(await screen.findByRole("button", { name: "DEPLOY" }));

    expect(await screen.findByText(/already exists with different repository settings/)).toBeTruthy();
    expect(mocks.postVersionDefinitionFile).not.toHaveBeenCalled();
    expect(mocks.updateRepoOSInfo).not.toHaveBeenCalled();
    expect(mocks.createCluster).not.toHaveBeenCalled();
  });

  it("prints Review and downloads the Blueprint archive", async () => {
    renderStep();
    await screen.findByRole("button", { name: "DEPLOY" });

    fireEvent.click(screen.getByRole("button", { name: "Print Review" }));
    expect(mocks.print).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole("button", { name: "Generate Blueprint" }));
    await waitFor(() => expect(mocks.saveAs).toHaveBeenCalledWith(
      expect.any(Blob),
      "cluster1-blueprint.zip",
    ));
  });

  it("prefetches identities and updates the existing descriptor for managed Kerberos", async () => {
    renderStep({ isKerberosEnabled: true, wizardName: "addService" });

    expect(await screen.findByText("Kerberos KDC type: mit-kdc")).toBeTruthy();
    expect(mocks.downloadKerberosIdentitiesCsv).toHaveBeenCalledWith("cluster1");

    fireEvent.click(screen.getByRole("button", { name: "DEPLOY" }));
    await waitFor(() => expect(mocks.saveAndEditKerberosData).toHaveBeenCalledOnce());
    expect(mocks.saveAndEditKerberosData).toHaveBeenCalledWith(
      "cluster1",
      expect.objectContaining({
        artifact_data: expect.objectContaining({
          services: [expect.objectContaining({
            configurations: [{
              "core-site": {
                "hadoop.security.authentication": "custom-kerberos",
              },
            }],
          })],
        }),
      }),
    );
    await waitFor(() => expect(mocks.handleNextImperitive).toHaveBeenCalledOnce());
  });

  it("creates the descriptor early and explains principal ownership for manual Kerberos", async () => {
    mocks.kerberosMode.isManualKerberos = true;
    mocks.kerberosMode.kdcType = "none";
    mocks.getKerberosDescriptorArtifact.mockRejectedValueOnce({
      response: { status: 404 },
    });

    renderStep({ isKerberosEnabled: true, wizardName: "addService" });

    expect(await screen.findByText(/you must create and distribute principals and keytabs/)).toBeTruthy();
    expect(mocks.saveKerberosData).toHaveBeenCalledOnce();
    expect(mocks.saveAndEditKerberosData).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Download Kerberos CSV" }));
    await waitFor(() => expect(mocks.saveAs).toHaveBeenCalledOnce());
  });

  it("blocks deployment when descriptor validation fails and allows preparation retry", async () => {
    mocks.getKerberosDescriptorProperties
      .mockResolvedValueOnce({ KerberosDescriptor: { kerberos_descriptor: {} } })
      .mockResolvedValue({
        KerberosDescriptor: {
          kerberos_descriptor: { identities: [], services: [] },
        },
      });
    renderStep({ isKerberosEnabled: true, wizardName: "addService" });

    expect(await screen.findByText("Ambari returned an invalid Kerberos descriptor.")).toBeTruthy();
    expect((screen.getByRole("button", { name: "DEPLOY" }) as HTMLButtonElement).disabled).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "Retry Kerberos Preparation" }));
    await waitFor(() => {
      expect((screen.getByRole("button", { name: "DEPLOY" }) as HTMLButtonElement).disabled).toBe(false);
    });
  });
});
