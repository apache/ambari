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
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextWrapper } from ".";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  assignMastersProps: null as any,
  assignMastersAddableProps: null as any,
  wizardFooterProps: null as any,
  flushStateToDb: vi.fn(),
  handleNextImperitive: vi.fn(),
}));

vi.mock("../../components/AssignMasters", () => ({
  default: (props: {
    setHasValidationIssues: (hasIssues: boolean) => void;
  }) => {
    mocks.assignMastersProps = props;
    return (
      <button onClick={() => props.setHasValidationIssues(true)}>REPORT ISSUE</button>
    );
  },
}));
vi.mock("../../components/AssignMastersAddable", () => ({
  default: (props: any) => {
    mocks.assignMastersAddableProps = props;
    return null;
  },
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: (props: any) => {
    mocks.wizardFooterProps = props;
    return <button onClick={props.onNext}>NEXT</button>;
  },
}));

import Step5 from "./Step5";

describe("Assign Masters validation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.assignMastersAddableProps = null;
    mocks.wizardFooterProps = null;
    mocks.flushStateToDb.mockResolvedValue(undefined);
  });

  it("requires Continue Anyway before advancing with matching issues", async () => {
    const value = {
      state: {
        clusterCreationSteps: {
          SERVICES: { data: { services: { HDFS: { selected: true } } } },
          VERSION: {
            data: {
              selectedVersion: { stack_name: "HDP", stack_version: "3.1" },
            },
          },
          HOSTS: {
            data: {
              hosts: [{ name: "host1.example.com", bootStatus: "REGISTERED" }],
            },
          },
        },
      },
      dispatch: vi.fn(),
      flushStateToDb: mocks.flushStateToDb,
      installedHosts: [],
      installedServices: [],
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "MASTERS" },
        handleNextImperitive: mocks.handleNextImperitive,
        handleBackImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext(value);

    render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={value}>
          <Step5 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "REPORT ISSUE" }));
    fireEvent.click(screen.getByRole("button", { name: "NEXT" }));

    expect(mocks.flushStateToDb).not.toHaveBeenCalled();
    expect(mocks.handleNextImperitive).not.toHaveBeenCalled();
    fireEvent.click(
      screen.getByRole("button", { name: "Continue Anyway" }),
    );

    await waitFor(() => {
      expect(mocks.flushStateToDb).toHaveBeenCalledWith("next");
      expect(mocks.handleNextImperitive).toHaveBeenCalledOnce();
    });
  });

  it("checkpoints the exact draft revision before preparing reviewed providers", async () => {
    const withStateCheckpoint = vi.fn(async (
      request: (revision: number) => Promise<any>,
    ) => request(12));
    const managedPreview = {
      binding_id: "11111111-1111-4111-8111-111111111111",
      compatible: true,
      consumer: {
        lifecycle: "DRAFT",
        planned_hbase_user: "hbase_a",
        scope: "DRAFT",
        service_name: "HBASE",
      },
      consumer_descriptor_fingerprint: "consumer-fingerprint",
      dependency_type: "HDFS",
      errors: [],
      preview_schema_version: 2,
      provider: {
        cluster_id: 31,
        cluster_name: "provider-a",
        service_name: "HDFS",
      },
      provider_fingerprint: "provider-fingerprint",
      snapshot_fingerprint: "snapshot-fingerprint",
    };
    const servicesData = {
      managedDependencies: {
        HDFS: {
          mode: "managed",
          preview: managedPreview,
          provider: { ...managedPreview.provider, compatible: true, errors: [] },
        },
      },
      services: {
        HBASE: { installed: false, selected: true },
      },
    };
    const value = {
      state: {
        clusterCreationSteps: {
          SERVICES: { data: servicesData },
          VERSION: { data: { selectedVersion: {} } },
          HOSTS: { data: { hosts: [] } },
        },
      },
      dispatch: vi.fn(),
      draftId: "22222222-2222-4222-8222-222222222222",
      flushStateToDb: mocks.flushStateToDb,
      installedHosts: [],
      installedServices: [],
      withStateCheckpoint,
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "MASTERS" },
        handleNextImperitive: mocks.handleNextImperitive,
        handleBackImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext(value);
    render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={value}>
          <Step5 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );

    const prepared = await mocks.assignMastersProps.runWithAdvisorRequest(
      async (request: any) => request,
    );

    expect(withStateCheckpoint).toHaveBeenCalledOnce();
    expect(prepared.properties).toEqual({
      managed_dependency_plan: {
        consumer: {
          scope: "DRAFT",
          draft_id: value.draftId,
          expected_revision: 12,
        },
        selections: [{
          binding_id: managedPreview.binding_id,
          dependency_type: "HDFS",
          expected_consumer_descriptor_fingerprint: "consumer-fingerprint",
          expected_provider_fingerprint: "provider-fingerprint",
          expected_snapshot_fingerprint: "snapshot-fingerprint",
          preview_schema_version: 2,
          provider: { cluster_id: 31, service_name: "HDFS" },
        }],
      },
    });
    expect(prepared.isCurrent()).toBe(true);
  });

  it("uses the owned Add Service revision for SERVICE_PLAN advice", async () => {
    const withStateCheckpoint = vi.fn(async (
      request: (revision: number) => Promise<any>,
    ) => request(17));
    const managedPreview = {
      binding_id: "11111111-1111-4111-8111-111111111111",
      compatible: true,
      consumer: {
        lifecycle: "ADD_SERVICE_PLAN",
        planned_hbase_user: "hbase_a",
        scope: "SERVICE_PLAN",
        service_name: "HBASE",
      },
      consumer_descriptor_fingerprint: "consumer-fingerprint",
      dependency_type: "HDFS",
      errors: [],
      preview_schema_version: 2,
      provider: {
        cluster_id: 31,
        cluster_name: "provider-a",
        service_name: "HDFS",
      },
      provider_fingerprint: "provider-fingerprint",
      snapshot_fingerprint: "snapshot-fingerprint",
    };
    const value = {
      state: {
        addServiceSteps: {
          SERVICES: {
            data: {
              managedDependencies: {
                HDFS: { mode: "managed", preview: managedPreview },
              },
              services: {
                HBASE: { installed: false, selected: true },
              },
            },
          },
          VERSION: { data: { selectedVersion: {} } },
        },
      },
      dispatch: vi.fn(),
      flushStateToDb: mocks.flushStateToDb,
      installedHosts: ["worker-a"],
      installedServices: ["KAFKA", "HDFS"],
      workflowMaterializedServices: ["HBASE"],
      withStateCheckpoint,
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "MASTERS" },
        handleNextImperitive: mocks.handleNextImperitive,
        handleBackImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext(value);
    const appContext = {
      cluster: { cluster_id: 27, cluster_name: "cluster-a" },
      clusterName: "cluster-a",
      runtimeKey: JSON.stringify(["alice", "cluster", 27]),
    };

    render(
      <AppContext.Provider value={appContext as any}>
        <ContextWrapper.Provider value={{ Context: WizardContext }}>
          <WizardContext.Provider value={value}>
            <Step5 wizardName="addService" />
          </WizardContext.Provider>
        </ContextWrapper.Provider>
      </AppContext.Provider>,
    );

    const prepared = await mocks.assignMastersAddableProps.runWithAdvisorRequest(
      async (request: any) => request,
    );

    expect(withStateCheckpoint).toHaveBeenCalledOnce();
    expect(prepared.properties.managed_dependency_plan.consumer).toEqual({
      scope: "SERVICE_PLAN",
      cluster_id: 27,
      expected_revision: 17,
    });
    expect(prepared.properties.managed_dependency_plan.selections).toEqual([{
      binding_id: managedPreview.binding_id,
      dependency_type: "HDFS",
      expected_consumer_descriptor_fingerprint: "consumer-fingerprint",
      expected_provider_fingerprint: "provider-fingerprint",
      expected_snapshot_fingerprint: "snapshot-fingerprint",
      preview_schema_version: 2,
      provider: { cluster_id: 31, service_name: "HDFS" },
    }]);
  });

  it("keeps Add Service Next disabled until child callbacks report ready and valid", async () => {
    const value = {
      state: {
        addServiceSteps: {
          SERVICES: {
            data: {
              services: { RANGER: { selected: true, installed: false } },
            },
          },
          MASTERS: { data: { mastersData: [] } },
        },
      },
      dispatch: vi.fn(),
      flushStateToDb: mocks.flushStateToDb,
      installedHosts: ["host-a"],
      installedServices: [],
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "MASTERS" },
        handleNextImperitive: mocks.handleNextImperitive,
        handleBackImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext(value);

    render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={value}>
          <Step5 wizardName="addService" />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );

    await waitFor(() => {
      expect(mocks.assignMastersAddableProps).toBeTruthy();
      expect(mocks.wizardFooterProps.isNextEnabled).toBe(false);
    });

    act(() => {
      mocks.assignMastersAddableProps.onLoadStateChange({ status: "ready" });
      mocks.assignMastersAddableProps.onAssignmentValidationChange(true, []);
    });
    expect(mocks.wizardFooterProps.isNextEnabled).toBe(true);

    act(() => {
      mocks.assignMastersAddableProps.onLoadStateChange({
        status: "error",
        error: "Advisor unavailable",
      });
    });
    expect(mocks.wizardFooterProps.isNextEnabled).toBe(false);
  });

  it("rejects a late checkpoint after the route switches to another draft", async () => {
    let resolveCheckpoint!: (revision: number) => void;
    const checkpoint = new Promise<number>((resolve) => {
      resolveCheckpoint = resolve;
    });
    const managedPreview = {
      binding_id: "11111111-1111-4111-8111-111111111111",
      compatible: true,
      consumer: {
        lifecycle: "DRAFT",
        planned_hbase_user: "hbase_a",
        scope: "DRAFT",
        service_name: "HBASE",
      },
      consumer_descriptor_fingerprint: "consumer-fingerprint",
      dependency_type: "HDFS",
      errors: [],
      preview_schema_version: 2,
      provider: { cluster_id: 31, cluster_name: "provider-a", service_name: "HDFS" },
      provider_fingerprint: "provider-fingerprint",
      snapshot_fingerprint: "snapshot-fingerprint",
    };
    const state = {
      clusterCreationSteps: {
        SERVICES: { data: {
          managedDependencies: { HDFS: { mode: "managed", preview: managedPreview } },
          services: { HBASE: { installed: false, selected: true } },
        } },
        VERSION: { data: { selectedVersion: {} } },
        HOSTS: { data: { hosts: [] } },
      },
    };
    const baseValue = {
      state,
      dispatch: vi.fn(),
      flushStateToDb: mocks.flushStateToDb,
      installedHosts: [],
      installedServices: [],
      withStateCheckpoint: vi.fn((
        request: (revision: number) => Promise<any>,
      ) => checkpoint.then(request)),
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "MASTERS" },
        handleNextImperitive: mocks.handleNextImperitive,
        handleBackImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext({ ...baseValue, draftId: "" });
    const rendered = render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={{
          ...baseValue,
          draftId: "22222222-2222-4222-8222-222222222222",
        }}>
          <Step5 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );
    const stalePreparation = mocks.assignMastersProps.runWithAdvisorRequest(
      async (request: any) => request,
    );

    rendered.rerender(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={{
          ...baseValue,
          draftId: "33333333-3333-4333-8333-333333333333",
        }}>
          <Step5 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );
    resolveCheckpoint(7);

    await expect(stalePreparation).rejects.toThrow(/reviewed again/i);
  });

  it("rejects late advice when provider facts change inside the same draft", async () => {
    let resolveCheckpoint!: (revision: number) => void;
    const checkpoint = new Promise<number>((resolve) => {
      resolveCheckpoint = resolve;
    });
    const preview = {
      binding_id: "11111111-1111-4111-8111-111111111111",
      compatible: true,
      consumer: {
        lifecycle: "DRAFT",
        planned_hbase_user: "hbase_a",
        scope: "DRAFT",
        service_name: "HBASE",
      },
      consumer_descriptor_fingerprint: "consumer-fingerprint",
      dependency_type: "HDFS",
      errors: [],
      preview_schema_version: 2,
      provider: { cluster_id: 31, cluster_name: "provider-a", service_name: "HDFS" },
      provider_fingerprint: "provider-fingerprint",
      snapshot_fingerprint: "snapshot-a",
    };
    const stateWithPreview = (snapshotFingerprint: string) => ({
      clusterCreationSteps: {
        SERVICES: { data: {
          managedDependencies: { HDFS: {
            mode: "managed",
            preview: { ...preview, snapshot_fingerprint: snapshotFingerprint },
          } },
          services: { HBASE: { installed: false, selected: true } },
        } },
        VERSION: { data: { selectedVersion: {
          stack_name: "BIGTOP",
          stack_version: "3.2.0",
        } } },
        HOSTS: { data: { hosts: [] } },
      },
    });
    const baseValue = {
      dispatch: vi.fn(),
      draftId: "22222222-2222-4222-8222-222222222222",
      flushStateToDb: mocks.flushStateToDb,
      installedHosts: [],
      installedServices: [],
      withStateCheckpoint: vi.fn((
        request: (revision: number) => Promise<any>,
      ) => checkpoint.then(request)),
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "MASTERS" },
        handleNextImperitive: mocks.handleNextImperitive,
        handleBackImperitive: vi.fn(),
        jumpToStep: vi.fn(),
      },
    };
    const WizardContext = createContext({
      ...baseValue,
      state: stateWithPreview("snapshot-a"),
    });
    const rendered = render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={{
          ...baseValue,
          state: stateWithPreview("snapshot-a"),
        }}>
          <Step5 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );
    const stalePreparation = mocks.assignMastersProps.runWithAdvisorRequest(
      async (request: any) => request,
    );

    rendered.rerender(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={{
          ...baseValue,
          state: stateWithPreview("snapshot-b"),
        }}>
          <Step5 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );
    resolveCheckpoint(9);

    await expect(stalePreparation).rejects.toThrow(/reviewed again/i);
  });
});
