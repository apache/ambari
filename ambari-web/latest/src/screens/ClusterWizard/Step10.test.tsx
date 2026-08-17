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
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  flushStateToDb: vi.fn(),
  updateCluster: vi.fn(),
}));

vi.mock("../../api/clusterApi", () => ({
  default: { updateCluster: mocks.updateCluster },
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({
    isNextEnabled,
    onNext,
    step,
  }: {
    isNextEnabled: boolean;
    onNext: () => void;
    step: { nextLabel: string };
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>
      {step.nextLabel}
    </button>
  ),
}));

import Step10 from "./Step10";

function wizardState(prefix: "clusterCreation" | "addService") {
  return {
    [`${prefix}Steps`]: {
      NAME: { data: { clusterName: "cluster1" } },
      HOSTS: { data: { installedHosts: [] } },
      MASTERS: { data: { mastersData: [] } },
      SLAVES_AND_CLIENTS: { data: { serviceComponents: [] } },
      INSTALL_START_TEST: {
        data: {
          clusterStatus: { status: "STARTED" },
          hostInfo: [],
        },
      },
    },
  };
}

function renderStep(wizardName: "clusterCreation" | "addService") {
  const contextValue = {
    state: wizardState(wizardName),
    flushStateToDb: mocks.flushStateToDb,
    stepWizardUtilities: { currentStep: { name: "SUMMARY" } },
  };
  const WizardContext = createContext(contextValue);
  return render(
    <ContextWrapper.Provider value={{ Context: WizardContext }}>
      <WizardContext.Provider value={contextValue}>
        <Step10 wizardName={wizardName} />
      </WizardContext.Provider>
    </ContextWrapper.Provider>,
  );
}

describe("installation Summary completion", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.flushStateToDb.mockResolvedValue(undefined);
    mocks.updateCluster.mockResolvedValue({});
  });

  afterEach(() => cleanup());

  it("clears only Add Service state without changing cluster provisioning", async () => {
    renderStep("addService");
    fireEvent.click(screen.getByRole("button", { name: "COMPLETE" }));

    await waitFor(() => expect(mocks.flushStateToDb).toHaveBeenCalledWith("complete"));
    expect(mocks.updateCluster).not.toHaveBeenCalled();
  });

  it("keeps new-cluster state retryable when provisioning completion fails", async () => {
    mocks.updateCluster.mockRejectedValueOnce(new Error("Provisioning update failed"));
    renderStep("clusterCreation");
    fireEvent.click(screen.getByRole("button", { name: "COMPLETE" }));

    expect(await screen.findByText("Provisioning update failed")).toBeTruthy();
    expect(mocks.flushStateToDb).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "COMPLETE" }).hasAttribute("disabled")).toBe(false);
  });
});
