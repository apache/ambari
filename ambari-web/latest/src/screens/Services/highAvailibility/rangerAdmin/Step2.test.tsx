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
import { AppContext } from "../../../../store/context";
import { EnableHighAvailibilityRangerAdminContext } from "./store/context";
import { RangerAdminAssignment } from "./rangerAdminHaUtils";

const mocks = vi.hoisted(() => ({ assignmentProps: vi.fn() }));

type MockAssignmentProps = {
  dispatch: (payload: { masterComponentHosts: RangerAdminAssignment[] }) => void;
  onAssignmentValidationChange: (valid: boolean, errors: string[]) => void;
  onLoadStateChange: (state: {
    status: "loading" | "ready" | "error";
    error?: string;
  }) => void;
};

vi.mock("../../../../components/AssignMastersAddable", () => ({
  default: (props: MockAssignmentProps) => {
    mocks.assignmentProps(props);
    const submit = (masterComponentHosts: RangerAdminAssignment[]) => {
      props.dispatch({ masterComponentHosts });
      props.onAssignmentValidationChange(true, []);
      props.onLoadStateChange({ status: "ready" });
    };
    return (
      <>
        <button
          onClick={() =>
            props.onLoadStateChange({
              status: "error",
              error: "recommendations failed",
            })
          }
        >
          Load error
        </button>
        <button
          onClick={() =>
            submit([
              {
                component: "RANGER_ADMIN",
                hostName: "ra1.example.com",
                isInstalled: true,
              },
              {
                component: "RANGER_ADMIN",
                hostName: "ra2.example.com",
                isInstalled: true,
              },
            ])
          }
        >
          Invalid assignments
        </button>
        <button
          onClick={() =>
            submit([
              {
                component: "RANGER_ADMIN",
                hostName: "ra1.example.com",
                isInstalled: true,
              },
              {
                component: "RANGER_ADMIN",
                hostName: "ra2.example.com",
                isInstalled: false,
              },
              {
                component: "RANGER_ADMIN",
                hostName: "ra3.example.com",
                isInstalled: false,
              },
            ])
          }
        >
          Valid assignments
        </button>
      </>
    );
  },
}));
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

import Step2 from "./Step2";

describe("Ranger Admin HA host assignment step", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("requires shared validation plus one current and at least one additional admin", async () => {
    const dispatch = vi.fn();
    const flushStateToDb = vi.fn().mockResolvedValue(undefined);
    const handleNextImperitive = vi.fn().mockResolvedValue(undefined);
    render(
      <AppContext.Provider
        value={
          {
            services: [{ ServiceInfo: { service_name: "RANGER" } }],
          } as never
        }
      >
        <EnableHighAvailibilityRangerAdminContext.Provider
          value={
            {
              state: { enableHighAvailibilityRangerAdminSteps: {} },
              dispatch,
              flushStateToDb,
              stepWizardUtilities: {
                currentStep: { name: "SELECT_HOSTS" },
                handleNextImperitive,
                handleBackImperitive: vi.fn(),
              },
            } as never
          }
        >
          <Step2 />
        </EnableHighAvailibilityRangerAdminContext.Provider>
      </AppContext.Provider>,
    );

    const next = screen.getByRole("button", { name: "Next" });
    expect((next as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText("Ranger Admin host assignments are still loading.")).toBeTruthy();
    expect(mocks.assignmentProps.mock.calls.at(-1)?.[0]).toMatchObject({
      mastersToShow: ["RANGER_ADMIN"],
      mastersToAdd: ["RANGER_ADMIN"],
      minimumAdditionalMasterCount: { RANGER_ADMIN: 1 },
      validateAssignments: true,
      onLoadStateChange: expect.any(Function),
    });

    fireEvent.click(screen.getByRole("button", { name: "Invalid assignments" }));
    expect(screen.getByText("Exactly one current Ranger Admin is required.")).toBeTruthy();
    expect(screen.getByText("At least one additional Ranger Admin is required.")).toBeTruthy();
    expect((next as HTMLButtonElement).disabled).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: "Valid assignments" }));
    await waitFor(() =>
      expect((next as HTMLButtonElement).disabled).toBe(false),
    );

    fireEvent.click(screen.getByRole("button", { name: "Load error" }));
    await waitFor(() =>
      expect((next as HTMLButtonElement).disabled).toBe(true),
    );
    fireEvent.click(screen.getByRole("button", { name: "Valid assignments" }));
    await waitFor(() =>
      expect((next as HTMLButtonElement).disabled).toBe(false),
    );
    fireEvent.click(next);

    await waitFor(() => expect(handleNextImperitive).toHaveBeenCalledOnce());
    expect(flushStateToDb).toHaveBeenCalledWith("next");
    expect(flushStateToDb.mock.invocationCallOrder[0]).toBeLessThan(
      handleNextImperitive.mock.invocationCallOrder[0],
    );
    expect(dispatch).toHaveBeenLastCalledWith(
      expect.objectContaining({
        payload: expect.objectContaining({ step: "SELECT_HOSTS" }),
      }),
    );
  });
});
