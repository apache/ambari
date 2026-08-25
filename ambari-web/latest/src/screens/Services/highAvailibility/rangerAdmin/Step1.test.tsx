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
import { afterEach, describe, expect, it, vi } from "vitest";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";

vi.mock("../../../../components/StepWizard/WizardFooter", () => ({
  default: ({
    isNextEnabled,
    onNext,
  }: {
    isNextEnabled: boolean;
    onNext: () => void;
  }) => (
    <button disabled={!isNextEnabled} onClick={onNext}>
      Next
    </button>
  ),
}));

import Step1 from "./Step1";

describe("Ranger Admin HA load balancer step", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("restores the saved URL and persists the current value before continuing", async () => {
    const dispatch = vi.fn();
    const flushStateToDb = vi.fn().mockResolvedValue(undefined);
    const handleNextImperitive = vi.fn().mockResolvedValue(undefined);
    render(
      <EnableHighAvailibilityRangerAdminContext.Provider
        value={
          {
            state: {
              enableHighAvailibilityRangerAdminSteps: {
                GET_STARTED: {
                  data: { loadBalancerUrl: "https://saved.example.com/ranger" },
                },
              },
            },
            dispatch,
            flushStateToDb,
            stepWizardUtilities: {
              currentStep: { name: "GET_STARTED" },
              handleNextImperitive,
            },
          } as never
        }
      >
        <Step1 />
      </EnableHighAvailibilityRangerAdminContext.Provider>,
    );

    const input = screen.getByLabelText("URL to load balancer") as HTMLInputElement;
    const next = screen.getByRole("button", { name: "Next" });
    expect(input.value).toBe("https://saved.example.com/ranger");
    expect((next as HTMLButtonElement).disabled).toBe(false);

    fireEvent.change(input, { target: { value: "http://localhost:6080" } });
    expect((next as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText("Must be a valid URL.")).toBeTruthy();

    fireEvent.change(input, {
      target: { value: "ftp://user:pass@lb.example.com/ranger" },
    });
    fireEvent.click(next);

    await waitFor(() => expect(handleNextImperitive).toHaveBeenCalledOnce());
    expect(dispatch).toHaveBeenCalledWith({
      type: "STORE INFORMATION",
      payload: {
        step: "GET_STARTED",
        data: { loadBalancerUrl: "ftp://user:pass@lb.example.com/ranger" },
      },
    });
    expect(flushStateToDb).toHaveBeenCalledWith("next");
    expect(flushStateToDb.mock.invocationCallOrder[0]).toBeLessThan(
      handleNextImperitive.mock.invocationCallOrder[0],
    );
  });
});
