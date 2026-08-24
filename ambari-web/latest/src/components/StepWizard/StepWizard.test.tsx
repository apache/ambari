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
import { createContext } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import StepWizard from ".";

const wizardSteps = {
  1: {
    label: "Step One",
    completed: true,
    Component: <div>First step</div>,
    canGoBack: false,
    isNextEnabled: true,
  },
  2: {
    label: "Step Two",
    completed: false,
    Component: <div>Second step</div>,
    canGoBack: true,
    isNextEnabled: false,
  },
};

const renderWizard = (flushStateToDb: ReturnType<typeof vi.fn>) => {
  const WizardContext = createContext({ flushStateToDb });
  const jumpToStep = vi.fn();
  render(
    <WizardContext.Provider value={{ flushStateToDb }}>
      <StepWizard
        Context={WizardContext}
        wizardUtilities={{
          activeStep: 2,
          wizardSteps,
          jumpToStep,
          canJumpFromCurrentStep: () => true,
        }}
      />
    </WizardContext.Provider>,
  );
  return jumpToStep;
};

describe("StepWizard navigation persistence", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("waits for the checkpoint before changing steps", async () => {
    let resolveCheckpoint: (() => void) | undefined;
    const checkpoint = new Promise<void>((resolve) => {
      resolveCheckpoint = resolve;
    });
    const flushStateToDb = vi.fn().mockReturnValue(checkpoint);
    const jumpToStep = renderWizard(flushStateToDb);

    fireEvent.click(screen.getByText("Step One"));
    fireEvent.click(screen.getByTestId("confirm-ok-btn"));

    expect(flushStateToDb).toHaveBeenCalledWith("jump", 1);
    expect(jumpToStep).not.toHaveBeenCalled();
    expect(screen.queryByTestId("confirm-cancel-btn")).toBeNull();

    resolveCheckpoint?.();
    await waitFor(() => expect(jumpToStep).toHaveBeenCalledWith(1));
  });

  it("keeps the current step and exposes a retry after persistence fails", async () => {
    const flushStateToDb = vi
      .fn()
      .mockRejectedValueOnce(new Error("checkpoint unavailable"))
      .mockResolvedValueOnce(undefined);
    const jumpToStep = renderWizard(flushStateToDb);

    fireEvent.click(screen.getByText("Step One"));
    fireEvent.click(screen.getByTestId("confirm-ok-btn"));

    expect((await screen.findByRole("alert")).textContent).toContain(
      "checkpoint unavailable",
    );
    expect(jumpToStep).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId("confirm-ok-btn"));
    await waitFor(() => expect(jumpToStep).toHaveBeenCalledWith(1));
    expect(flushStateToDb).toHaveBeenCalledTimes(2);
  });
});
