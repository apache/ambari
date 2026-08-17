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
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  flushStateToDb: vi.fn(),
  handleNextImperitive: vi.fn(),
}));

vi.mock("../../components/AssignMasters", () => ({
  default: ({
    setHasValidationIssues,
  }: {
    setHasValidationIssues: (hasIssues: boolean) => void;
  }) => (
    <button onClick={() => setHasValidationIssues(true)}>REPORT ISSUE</button>
  ),
}));
vi.mock("../../components/AssignMastersAddable", () => ({
  default: () => null,
}));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({ onNext }: { onNext: () => void }) => (
    <button onClick={onNext}>NEXT</button>
  ),
}));

import Step5 from "./Step5";

describe("Assign Masters validation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
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
});

