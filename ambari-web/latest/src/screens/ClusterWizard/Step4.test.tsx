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

import { createContext, StrictMode } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextWrapper } from ".";
import "../../i18n";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  getServices: vi.fn(),
  getDraftCandidates: vi.fn(),
  getServiceCandidates: vi.fn(),
  getServicePlanCandidates: vi.fn(),
  previewDraft: vi.fn(),
  previewService: vi.fn(),
  previewServicePlan: vi.fn(),
  onCancel: undefined as undefined | (() => unknown),
  onNext: undefined as undefined | (() => unknown),
  isNextEnabled: false,
}));

vi.mock("../../api/chooseServicesApi", () => ({
  ChooseServicesApi: { getServices: mocks.getServices },
}));
vi.mock("../../api/serviceDependenciesApi", () => ({
  default: {
    getDraftCandidates: mocks.getDraftCandidates,
    getServiceCandidates: mocks.getServiceCandidates,
    getServicePlanCandidates: mocks.getServicePlanCandidates,
    previewDraft: mocks.previewDraft,
    previewService: mocks.previewService,
    previewServicePlan: mocks.previewServicePlan,
  },
}));
vi.mock("../../components/Table", () => ({
  default: ({ columns, data }: { columns: any[]; data: Array<{ serviceName: string }> }) => (
    <div>{data.map((service) => (
      <div key={service.serviceName}>
        {columns[0].cell?.({ row: { original: service } })}
        {columns[1].cell?.({ row: { original: service } })}
      </div>
    ))}</div>
  ),
}));
vi.mock("../../components/Spinner", () => ({
  default: () => <div>Loading services</div>,
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onCancel, onNext }: {
    isNextEnabled: boolean;
    onCancel: () => unknown;
    onNext: () => unknown;
  }) => {
    mocks.onCancel = onCancel;
    mocks.onNext = onNext;
    mocks.isNextEnabled = isNextEnabled;
    return null;
  },
}));

import Step4 from "./Step4";

const stackService = (
  serviceName: string,
  displayName: string,
  isInstallable?: boolean,
  requiredServices: string[] = [],
) => ({
  StackServices: {
    comments: `${displayName} description`,
    display_name: displayName,
    is_installable: isInstallable,
    required_services: requiredServices,
    service_name: serviceName,
    service_type: "SERVICE",
    service_version: "1.0",
  },
  components: [],
});

describe("Choose Services stack metadata", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    mocks.onCancel = undefined;
    mocks.onNext = undefined;
    mocks.isNextEnabled = false;
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("KERBEROS", "Kerberos", true),
        stackService("CUSTOM", "Unavailable Service", false),
      ],
    });
  });

  function renderStep(
    wizardName: "clusterCreation" | "addService",
    flushStateToDb = vi.fn(),
    overrides: Record<string, unknown> = {},
    strict = false,
    appContextOverrides: Record<string, unknown> = {},
  ) {
    const value = {
      dispatch: vi.fn(),
      flushStateToDb,
      handleBackImperitive: vi.fn(),
      installedServices: [],
      state: {
        [`${wizardName}Steps`]: {
          VERSION: {
            data: {
              selectedStack: { stack_name: "HDP" },
              selectedVersion: { stack_version: "3.1" },
            },
          },
        },
      },
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "SERVICES" },
        handleNextImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
      ...overrides,
    };
    const WizardContext = createContext(value);

    const tree = (
      <AppContext.Provider value={{
        cluster: { cluster_id: 27, cluster_name: "consumer" },
        clusterName: "consumer",
        loginName: "alice",
        services: [],
        ...appContextOverrides,
      } as any}>
        <ContextWrapper.Provider value={{ Context: WizardContext }}>
          <WizardContext.Provider value={value}>
            <Step4 wizardName={wizardName} />
          </WizardContext.Provider>
        </ContextWrapper.Provider>
      </AppContext.Provider>
    );
    render(strict ? <StrictMode>{tree}</StrictMode> : tree);
    return value;
  }

  it.each(["clusterCreation", "addService"] as const)(
    "does not offer non-installable services in %s",
    async (wizardName) => {
      renderStep(wizardName);

      await waitFor(() => expect(screen.getByText("HDFS")).toBeTruthy());
      expect(screen.queryByText("Kerberos")).toBeNull();
      expect(screen.queryByText("Unavailable Service")).toBeNull();
      expect(mocks.getServices).toHaveBeenCalledWith("HDP", "3.1");
    },
  );

  it("keeps a catalog load failure distinct and retries the full catalog", async () => {
    mocks.getServices
      .mockRejectedValueOnce(new Error("catalog unavailable"))
      .mockResolvedValueOnce({ items: [stackService("HDFS", "HDFS")] });
    renderStep("addService");

    expect(await screen.findByText("catalog unavailable")).toBeTruthy();
    expect(screen.queryByText("No installable services are available.")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("HDFS")).toBeTruthy();
    expect(mocks.getServices).toHaveBeenCalledTimes(2);
  });

  it("returns Add Service cancellation persistence to the confirmation dialog", async () => {
    const cancellation = Promise.resolve();
    const flushStateToDb = vi.fn().mockReturnValue(cancellation);
    renderStep("addService", flushStateToDb);

    await waitFor(() => expect(mocks.onCancel).toBeTypeOf("function"));
    expect(mocks.onCancel?.()).toBe(cancellation);
    expect(flushStateToDb).toHaveBeenCalledWith("cancel");
  });

  it("persists an HBase provider choice before previewing the exact draft revision", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
      ],
    });
    mocks.getDraftCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [{
        cluster_id: 41,
        cluster_name: "storage-east",
        compatible: true,
        errors: [],
        healthy: true,
        installed: true,
        service_name: "HDFS",
      }] : [],
    ));
    mocks.previewDraft.mockResolvedValue({
      binding_id: "00000000-0000-4000-8000-000000000001",
      compatible: true,
      consumer: { lifecycle: "DRAFT", planned_hbase_user: "hbase", scope: "DRAFT", service_name: "HBASE" },
      dependency_type: "HDFS",
      errors: [],
      preview_schema_version: 1,
      provider: { cluster_id: 41, cluster_name: "storage-east", service_name: "HDFS" },
    });
    const storeStepDataAndFlush = vi.fn()
      .mockResolvedValueOnce(8)
      .mockResolvedValueOnce(9)
      .mockResolvedValueOnce(10);
    renderStep("clusterCreation", vi.fn(), {
      draftId: "00000000-0000-4000-8000-000000000010",
      getDraftRevision: () => 7,
      storeStepDataAndFlush,
    });

    fireEvent.click(await screen.findByText("HBase"));
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    await waitFor(() => expect(mocks.previewDraft).toHaveBeenCalledWith(expect.objectContaining({
      draftId: "00000000-0000-4000-8000-000000000010",
      expectedRevision: 9,
      provider: { cluster_id: 41, service_name: "HDFS" },
    })));
    const savedChoice = (storeStepDataAndFlush.mock.calls[1][1] as any).managedDependencies.HDFS;
    expect(savedChoice.mode).toBe("managed");
    expect(savedChoice.preview).toBeUndefined();
    await waitFor(() => expect((storeStepDataAndFlush.mock.calls[2][1] as any)
      .managedDependencies.HDFS.preview.compatible).toBe(true));
  });

  it("retains an independently selected local HDFS service when HBase uses a managed provider", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
        stackService("HIVE", "Hive", undefined, ["HDFS"]),
      ],
    });
    mocks.getDraftCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [{
        cluster_id: 41,
        cluster_name: "storage-east",
        compatible: true,
        errors: [],
        healthy: true,
        installed: true,
        service_name: "HDFS",
      }] : [],
    ));
    mocks.previewDraft.mockResolvedValue({
      compatible: true,
      dependency_type: "HDFS",
      errors: [],
      provider: { cluster_id: 41, cluster_name: "storage-east", service_name: "HDFS" },
    });
    const storeStepDataAndFlush = vi.fn().mockResolvedValue(4);
    renderStep("clusterCreation", vi.fn(), {
      draftId: "00000000-0000-4000-8000-000000000010",
      getDraftRevision: () => 3,
      storeStepDataAndFlush,
    });

    fireEvent.click(await screen.findByText("HDFS"));
    fireEvent.click(screen.getByText("HBase"));
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    await waitFor(() => expect(storeStepDataAndFlush).toHaveBeenCalled());
    expect((storeStepDataAndFlush.mock.calls[0][1] as any).services.HDFS.selected).toBe(true);
  });

  it("uses the scoped Add Service revision for provider planning before HBase creation", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
      ],
    });
    mocks.getServicePlanCandidates.mockImplementation(({ dependencyType }) => Promise.resolve(
      dependencyType === "HDFS" ? [{
        cluster_id: 41,
        cluster_name: "storage-east",
        compatible: true,
        errors: [],
        healthy: true,
        installed: true,
        service_name: "HDFS",
      }] : [],
    ));
    mocks.previewServicePlan.mockResolvedValue({
      compatible: true,
      consumer: { cluster_id: 27, lifecycle: "ADD_SERVICE_PLAN", scope: "SERVICE_PLAN", service_name: "HBASE" },
      dependency_type: "HDFS",
      errors: [],
      provider: { cluster_id: 41, cluster_name: "storage-east", service_name: "HDFS" },
    });
    const storeStepDataAndFlush = vi.fn()
      .mockResolvedValueOnce(12)
      .mockResolvedValueOnce(13)
      .mockResolvedValueOnce(14);
    renderStep("addService", vi.fn(), {
      getWorkflowRevision: () => 11,
      storeStepDataAndFlush,
    });

    fireEvent.click(await screen.findByText("HBase"));
    fireEvent.click(await screen.findByRole("radio", { name: "storage-east / HDFS" }));

    await waitFor(() => expect(mocks.previewServicePlan).toHaveBeenCalledWith(expect.objectContaining({
      clusterId: 27,
      expectedRevision: 13,
      provider: { cluster_id: 41, service_name: "HDFS" },
    })));
    expect(mocks.getServicePlanCandidates).toHaveBeenCalledWith(expect.objectContaining({
      clusterId: 27,
      expectedWorkflowRevision: 12,
    }));
  });

  it("shares the source checkpoint across StrictMode candidate effects", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
      ],
    });
    mocks.getDraftCandidates.mockResolvedValue([]);
    let resolveCheckpoint!: (revision: number) => void;
    const storeStepDataAndFlush = vi.fn(() => new Promise<number>((resolve) => {
      resolveCheckpoint = resolve;
    }));
    renderStep("clusterCreation", vi.fn(), {
      draftId: "00000000-0000-4000-8000-000000000010",
      storeStepDataAndFlush,
    }, true);

    fireEvent.click(await screen.findByText("HBase"));
    await waitFor(() => expect(storeStepDataAndFlush).toHaveBeenCalledTimes(1));
    resolveCheckpoint(4);

    await waitFor(() => expect(mocks.getDraftCandidates).toHaveBeenCalledTimes(2));
    expect(mocks.getDraftCandidates).toHaveBeenCalledWith(expect.objectContaining({
      expectedDraftRevision: 4,
    }));
  });

  it("does not plan or rewrite dependencies when adding another service beside installed HBase", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
        stackService("KAFKA", "Kafka"),
      ],
    });
    const flushStateToDb = vi.fn().mockResolvedValue(undefined);
    const value = renderStep("addService", flushStateToDb, {
      installedServices: ["HBASE"],
      state: {
        addServiceSteps: {
          VERSION: {
            data: {
              selectedStack: { stack_name: "HDP" },
              selectedVersion: { stack_version: "3.1" },
            },
          },
          SERVICES: {
            data: {
              services: {
                HBASE: {
                  displayName: "HBase",
                  selected: true,
                  serviceName: "HBASE",
                },
              },
            },
          },
        },
      },
    });

    expect(await screen.findByText("HBase")).toBeTruthy();
    expect(screen.getByText("Kafka")).toBeTruthy();
    expect(mocks.getServices).toHaveBeenCalledWith("HDP", "3.1");
    expect(screen.queryByText("Choose HBase providers")).toBeNull();
    expect(mocks.getServicePlanCandidates).not.toHaveBeenCalled();
    fireEvent.click(screen.getByText("Kafka"));
    await waitFor(() => expect(mocks.isNextEnabled).toBe(true));
    mocks.onNext?.();

    await waitFor(() => expect(flushStateToDb).toHaveBeenCalledWith("jump", expect.any(Number)));
    const saveAction = value.dispatch.mock.calls.find(([action]: any[]) =>
      action?.payload?.step === "SERVICES");
    expect(saveAction?.[0].payload.data.managedDependencies).toEqual({});
    expect(saveAction?.[0].payload.data.services.HBASE.installed).toBe(true);
  });

  it("uses live provider review for a workflow-owned INIT HBase after refresh", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
        stackService("KAFKA", "Kafka"),
      ],
    });
    mocks.getServiceCandidates.mockResolvedValue([]);
    const preview = {
      binding_id: "00000000-0000-4000-8000-000000000001",
      compatible: true,
      consumer: {
        cluster_id: 27,
        lifecycle: "ADD_SERVICE_PLAN",
        planned_hbase_user: "hbase",
        scope: "SERVICE_PLAN",
        service_name: "HBASE",
      },
      dependency_type: "HDFS",
      errors: [],
      preview_schema_version: 1,
      provider: { cluster_id: 41, cluster_name: "storage-east", service_name: "HDFS" },
    };
    renderStep("addService", vi.fn(), {
      installedServices: ["KAFKA"],
      workflowMaterializedServices: ["HBASE"],
      storeStepDataAndFlush: vi.fn().mockResolvedValue(12),
      state: {
        addServiceSteps: {
          VERSION: {
            data: {
              selectedStack: { stack_name: "HDP" },
              selectedVersion: { stack_version: "3.1" },
            },
          },
          SERVICES: {
            data: {
              autoSelectedLocalServices: ["ZOOKEEPER"],
              managedDependencies: {
                HDFS: {
                  mode: "managed",
                  preview,
                  provider: preview.provider,
                },
                ZOOKEEPER: { mode: "local" },
              },
              services: {
                HBASE: { installed: false, selected: true, serviceName: "HBASE" },
                ZOOKEEPER: { selected: true, serviceName: "ZOOKEEPER" },
              },
            },
          },
        },
      },
    });

    expect(await screen.findByText("Kafka")).toBeTruthy();
    expect(await screen.findByText(/Provider in storage-east has been reviewed/)).toBeTruthy();
    expect(mocks.getServices).toHaveBeenCalledWith("HDP", "3.1");
    expect(mocks.getServiceCandidates).toHaveBeenCalledWith(
      "consumer",
      expect.any(String),
      expect.any(AbortSignal),
    );
    expect(mocks.getServicePlanCandidates).not.toHaveBeenCalled();
  });

  it("keeps an auto-selected dependency when another selected service still requires it", async () => {
    mocks.getServices.mockResolvedValue({
      items: [
        stackService("HDFS", "HDFS"),
        stackService("ZOOKEEPER", "ZooKeeper"),
        stackService("HBASE", "HBase", undefined, ["HDFS", "ZOOKEEPER"]),
        stackService("HIVE", "Hive", undefined, ["HDFS"]),
      ],
    });
    mocks.getDraftCandidates.mockResolvedValue([]);
    renderStep("clusterCreation", vi.fn(), {
      draftId: "00000000-0000-4000-8000-000000000010",
      storeStepDataAndFlush: vi.fn().mockResolvedValue(4),
    });

    fireEvent.click(await screen.findByText("HBase"));
    fireEvent.click(screen.getByText("Hive"));
    fireEvent.click(screen.getByText("HBase"));

    expect((document.getElementById("filesystem-step4-checkbox-HDFS") as HTMLInputElement).checked)
      .toBe(true);
    expect((document.getElementById("service-step4-checkbox-HIVE") as HTMLInputElement).checked)
      .toBe(true);
  });
});
