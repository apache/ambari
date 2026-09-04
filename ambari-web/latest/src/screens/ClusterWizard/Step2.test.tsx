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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ComponentProps, createContext } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";
import { ContextWrapper } from ".";
import { ActionTypes } from "./clusterStore/types";

const mocks = vi.hoisted(() => ({
  dispatch: vi.fn(),
  flushStateToDb: vi.fn(),
  handleNextImperitive: vi.fn(),
}));

vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: ({ isNextEnabled, onNext }: {
    isNextEnabled: boolean;
    onNext: () => void;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>NEXT</button>
  ),
}));

import Step2 from "./Step2";

describe("Install Options host registration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.flushStateToDb.mockResolvedValue(undefined);
  });

  afterEach(cleanup);

  it("persists the Linux SSH registration fields before advancing", async () => {
    const contextValue = {
      state: { clusterCreationSteps: { HOSTS: { data: {} } } },
      dispatch: mocks.dispatch,
      flushStateToDb: mocks.flushStateToDb,
      stepWizardUtilities: {
        currentStep: { canGoBack: true, name: "HOSTS" },
        handleBackImperitive: vi.fn(),
        handleNextImperitive: mocks.handleNextImperitive,
      },
    };
    const WizardContext = createContext(contextValue);

    render(
      <AppContext.Provider value={{
        supports: { customizeAgentUserAccount: false },
      } as unknown as ComponentProps<typeof AppContext.Provider>["value"]}>
        <ContextWrapper.Provider value={{ Context: WizardContext }}>
          <WizardContext.Provider value={contextValue}>
            <Step2 />
          </WizardContext.Provider>
        </ContextWrapper.Provider>
      </AppContext.Provider>,
    );

    fireEvent.change(screen.getByPlaceholderText("host names"), {
      target: { value: "worker.example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("ssh private key"), {
      target: { value: "test-private-key" },
    });
    await waitFor(() => expect(
      (screen.getByRole("button", { name: "NEXT" }) as HTMLButtonElement).disabled,
    ).toBe(false));
    fireEvent.click(screen.getByRole("button", { name: "NEXT" }));

    await waitFor(() => expect(mocks.dispatch).toHaveBeenCalledWith({
      type: ActionTypes.STORE_INFORMATION,
      payload: {
        step: "HOSTS",
        data: expect.objectContaining({
          agentUserAccount: "root",
          isSshRegistration: true,
          sshKey: "test-private-key",
          sshPortNumber: 22,
          sshUserAccount: "root",
          targetHosts: ["worker.example.com"],
          useSsh: true,
        }),
      },
    }));
    expect(mocks.flushStateToDb).toHaveBeenCalledWith("next");
    expect(mocks.handleNextImperitive).toHaveBeenCalledOnce();
  });
});
