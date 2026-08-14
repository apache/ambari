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

import { act, cleanup, render } from "@testing-library/react";
import { createContext } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ContextWrapper } from ".";

const mocks = vi.hoisted(() => ({
  isHostsRegistered: vi.fn(),
  loadAmbariProperties: vi.fn(),
  startHostCheck: vi.fn(),
}));

vi.mock("../../api/clusterApi", () => ({
  default: { loadAmbariProperties: mocks.loadAmbariProperties },
}));
vi.mock("../../api/wizardApi", () => ({
  default: { isHostsRegistered: mocks.isHostsRegistered },
}));
vi.mock("../../hooks/useHostChecks", () => ({
  useHostChecks: () => ({
    hostCheckResult: [],
    isHostCheckRunning: false,
    startHostCheck: mocks.startHostCheck,
  }),
}));
vi.mock("../Hosts/HostChecks", () => ({
  default: () => null,
  getHostWithIssues: () => [],
}));
vi.mock("../../components/Table", () => ({ default: () => null }));
vi.mock("../../components/Paginator", () => ({ default: () => null }));
vi.mock("../../components/Spinner", () => ({ default: () => null }));
vi.mock("../../components/StepWizard/WizardFooter", () => ({
  default: () => null,
}));

import Step3 from "./Step3";

describe("Confirm Hosts registration polling", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    mocks.loadAmbariProperties.mockResolvedValue({});
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("schedules the next registration poll only after the current request settles", async () => {
    let completeFirstPoll: (response: unknown) => void = () => undefined;
    mocks.isHostsRegistered
      .mockReturnValueOnce(new Promise((resolve) => {
        completeFirstPoll = resolve;
      }))
      .mockResolvedValue({ items: [] });
    const contextValue = {
      dispatch: vi.fn(),
      flushStateToDb: vi.fn(),
      state: {
        addHostSteps: {
          HOSTS: {
            data: {
              installedHosts: [],
              isSshRegistration: false,
              targetHosts: ["host1"],
            },
          },
        },
      },
      stepWizardUtilities: {
        currentStep: { name: "HOST_STATUS" },
        handleBackImperitive: vi.fn(),
        handleNextImperitive: vi.fn(),
      },
    };
    const WizardContext = createContext(contextValue);

    render(
      <ContextWrapper.Provider value={{ Context: WizardContext }}>
        <WizardContext.Provider value={contextValue}>
          <Step3 wizardName="addHost" />
        </WizardContext.Provider>
      </ContextWrapper.Provider>,
    );
    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(mocks.isHostsRegistered).toHaveBeenCalledTimes(1);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(mocks.isHostsRegistered).toHaveBeenCalledTimes(1);

    await act(async () => {
      completeFirstPoll({ items: [] });
      await Promise.resolve();
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(mocks.isHostsRegistered).toHaveBeenCalledTimes(2);
  });
});
