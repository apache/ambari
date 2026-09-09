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

import { createContext, type ContextType } from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  validateMapping: vi.fn(),
  setServiceComponents: vi.fn(),
  flushStateToDb: vi.fn(),
  handleNextImperitive: vi.fn(),
  jumpToStep: vi.fn(),
}));

vi.mock("../../api/validations", () => ({
  default: { validateMapping: mocks.validateMapping },
}));

vi.mock("./hooks/useServiceComponents", () => ({
  default: () => ({
    ComponentCategory: { CLIENT: "CLIENT", MASTER: "MASTER", SLAVE: "SLAVE" },
    allServiceComponentsList: [
      {
        component_category: "SLAVE",
        component_name: "DATANODE",
        service_name: "HDFS",
      },
    ],
    getClientComponents: () => [],
    hosts: { "host-a": {} },
    serviceComponents: [
      {
        checkboxes: [
          { checked: true, isDisabled: false, label: "DATANODE" },
        ],
        hostname: "host-a",
      },
    ],
    setServiceComponents: mocks.setServiceComponents,
    STACK: "BIGTOP",
    VERSION: "3.3.0",
    services: ["HDFS"],
  }),
}));

vi.mock("../../hooks/usePagination", () => ({
  default: (items: unknown[]) => ({
    changePage: vi.fn(),
    currentItems: items,
    currentPage: 1,
    itemsPerPage: items.length,
    maxPage: 1,
    setItemsPerPage: vi.fn(),
  }),
}));

vi.mock("./utils", () => ({
  blueprintUtils: {
    getBlueprint: vi.fn(() => ({
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] },
    })),
    mergeBlueprints: vi.fn(() => ({
      blueprint: { host_groups: [] },
      blueprint_cluster_binding: { host_groups: [] },
    })),
  },
  isShownOnInstallerSlaveClientPage: vi.fn(() => true),
  minToInstall: vi.fn(() => 0),
}));

vi.mock("../../components/Paginator", () => ({ default: () => null }));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext }: { isNextEnabled: boolean; onNext: () => void }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>
      Next
    </button>
  ),
}));

import Step6 from "./Step6";

const renderStep = () => {
  const value = {
    dispatch: vi.fn(),
    flushStateToDb: mocks.flushStateToDb,
    installedHosts: [],
    installedServices: [],
    state: {
      clusterCreationSteps: {
        MASTERS: { data: { mastersData: [] } },
        SERVICES: {
          data: {
            services: { HDFS: { installed: false, selected: true } },
          },
        },
        SLAVES_AND_CLIENTS: { data: { serviceComponents: [] } },
      },
    },
    stepWizardUtilities: {
      currentStep: { name: "SLAVES_AND_CLIENTS" },
      handleBackImperitive: vi.fn(),
      handleNextImperitive: mocks.handleNextImperitive,
      jumpToStep: mocks.jumpToStep,
    },
    withStateCheckpoint: async (request: (revision: number) => Promise<unknown>) =>
      request(19),
  };
  const WizardContext = createContext(value);

  return render(
    <AppContext.Provider
      value={
        {
          cluster: { cluster_id: 27 },
          clusterName: "cluster1",
          runtimeKey: "runtime-1",
        } as unknown as ContextType<typeof AppContext>
      }
    >
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={value}>
          <Step6 />
        </WizardContext.Provider>
      </ContextWrapper.Provider>
    </AppContext.Provider>
  );
};

describe("Step 6 placement validation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.validateMapping.mockRejectedValueOnce(new Error("placement unavailable"));
  });

  it("keeps Next disabled after failure and retries the current placement", async () => {
    renderStep();

    expect(await screen.findByText("placement unavailable")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Next" })).toBeDisabled();

    mocks.validateMapping.mockResolvedValueOnce({ resources: [{ items: [] }] });
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(mocks.validateMapping).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Next" })).not.toBeDisabled(),
    );
    expect(mocks.validateMapping.mock.calls[1][2]).toMatchObject({
      hosts: ["host-a"],
      services: ["HDFS"],
      validate: "host_groups",
    });
  });
});
