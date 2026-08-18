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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ManageJournalNodesContext } from "./store/context";

const mocks = vi.hoisted(() => ({ hide: vi.fn() }));

vi.mock("../../../../store/ModalManager", () => ({
  default: { show: vi.fn(), hide: mocks.hide },
}));
vi.mock("../../../../Utils/taskUtils", () => ({
  startAllServices: vi.fn(),
}));
vi.mock("../../../../components/OperationsProgress", () => ({
  default: ({ setCompletionStatus }: { setCompletionStatus: (value: boolean) => void }) => (
    <button onClick={() => setCompletionStatus(true)}>Finish operations</button>
  ),
}));
vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: ({ onNext, isNextEnabled }: { onNext: () => void; isNextEnabled: boolean }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>Complete wizard</button>
  ),
}));

import Step7 from "./Step7";

describe("Manage JournalNodes completion", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("reports checkpoint clearing failures without navigating", async () => {
    const flushStateToDb = vi.fn().mockRejectedValue(
      new Error("persist unavailable"),
    );
    render(
      <AppContext.Provider value={{ clusterName: "c1" } as never}>
        <ManageJournalNodesContext.Provider
          value={
            {
              state: { manageJournalNodesSteps: {} },
              dispatch: vi.fn(),
              flushStateToDb,
              stepWizardUtilities: {
                currentStep: { name: "START_ALL_SERVICES" },
                handleBackImperitive: vi.fn(),
              },
            } as never
          }
        >
          <Step7 />
        </ManageJournalNodesContext.Provider>
      </AppContext.Provider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Finish operations" }));
    fireEvent.click(screen.getByRole("button", { name: "Complete wizard" }));

    expect(await screen.findByText("persist unavailable")).toBeTruthy();
    expect(mocks.hide).not.toHaveBeenCalled();
  });
});
