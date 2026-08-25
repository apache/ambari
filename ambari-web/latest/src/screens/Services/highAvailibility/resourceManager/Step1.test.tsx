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
import type { ContextType } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import Step1 from "./Step1";
import { EnableHighAvailibilityContext } from "./store/context";

describe("ResourceManager HA Step 1", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("keeps cancellation open and reports checkpoint cleanup failure", async () => {
    const flushStateToDb = vi
      .fn()
      .mockRejectedValue(new Error("RM checkpoint cleanup failed"));
    const contextValue = {
      state: { enableHighAvailibilitySteps: {} },
      dispatch: vi.fn(),
      flushStateToDb,
      stepWizardUtilities: {
        activeStep: 1,
        wizardSteps: {},
        currentStep: {
          name: "GET_STARTED",
          label: "Get Started",
          completed: false,
          Component: null,
          canGoBack: false,
          isNextEnabled: true,
        },
        jumpToStep: vi.fn(),
        handleNextImperitive: vi.fn(),
        handleBackImperitive: vi.fn(),
      },
    } as unknown as ContextType<typeof EnableHighAvailibilityContext>;

    render(
      <EnableHighAvailibilityContext.Provider value={contextValue}>
        <Step1 />
      </EnableHighAvailibilityContext.Provider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "CANCEL" }));
    fireEvent.click(screen.getByTestId("confirm-ok-btn"));

    expect(
      await screen.findAllByText("RM checkpoint cleanup failed"),
    ).toHaveLength(2);
    expect(flushStateToDb).toHaveBeenCalledWith("cancel");
  });
});
