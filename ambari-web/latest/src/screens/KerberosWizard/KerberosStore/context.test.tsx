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
import { ComponentProps, isValidElement, useContext } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import ClusterApi from "../../../api/clusterApi";
import KerberosApi from "../../../api/kerberosApi";
import { RequestApi } from "../../../api/requestApi";
import modalManager from "../../../store/ModalManager";
import { AppContext } from "../../../store/context";
import {
  discardChanges,
  EnableKerberosContext,
  KerberosWizardProvider,
} from "./context";

describe("Kerberos wizard discard", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("attempts service deletion after unkerberize fails", async () => {
    vi.spyOn(RequestApi, "preparingOperations").mockRejectedValue(
      new Error("unkerberize failed"),
    );
    const remove = vi.spyOn(KerberosApi, "deleteKerberosService")
      .mockResolvedValue({} as any);

    await expect(discardChanges("c1")).resolves.toBeUndefined();

    expect(remove).toHaveBeenCalledWith("c1", "KERBEROS");
  });

  it("settles only after both idempotent cleanup attempts", async () => {
    const calls: string[] = [];
    vi.spyOn(RequestApi, "preparingOperations").mockImplementation(async () => {
      calls.push("unkerberize");
      return {};
    });
    vi.spyOn(KerberosApi, "deleteKerberosService").mockImplementation(async () => {
      calls.push("delete");
      throw new Error("already absent");
    });

    await expect(discardChanges("c1")).resolves.toBeUndefined();
    expect(calls).toEqual(["unkerberize", "delete"]);
  });

  it("hands completed footer cancellation to the outer navigation guard", async () => {
    vi.spyOn(ClusterApi, "getPersistData").mockResolvedValue({} as any);
    vi.spyOn(ClusterApi, "postPersistData").mockResolvedValue({} as any);
    vi.spyOn(RequestApi, "preparingOperations").mockResolvedValue({} as any);
    vi.spyOn(KerberosApi, "deleteKerberosService").mockResolvedValue({} as any);
    const show = vi.spyOn(modalManager, "show");
    const onWizardExitReady = vi.fn();
    const stepWizardUtilities = {
      wizardSteps: { 1: { name: "GET_STARTED" } },
      currentStep: { name: "GET_STARTED" },
      jumpToStep: vi.fn(),
    };

    function CancelButton() {
      const { onExitPopUp } = useContext(EnableKerberosContext);
      return (
        <button onClick={() => onExitPopUp(false, false)}>Cancel wizard</button>
      );
    }

    render(
      <MemoryRouter>
        <AppContext.Provider
          value={
            { clusterName: "c1" } as unknown as ComponentProps<
              typeof AppContext.Provider
            >["value"]
          }
        >
          <KerberosWizardProvider
            stepWizardUtilities={stepWizardUtilities}
            onWizardExitReady={onWizardExitReady}
          >
            <CancelButton />
          </KerberosWizardProvider>
        </AppContext.Provider>
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole("button", { name: "Cancel wizard" }));
    const confirmation = show.mock.calls[0][0];
    if (!isValidElement<{ successCallback: () => Promise<void> }>(confirmation)) {
      throw new Error("Expected a confirmation modal");
    }
    await confirmation.props.successCallback();

    await waitFor(() => expect(onWizardExitReady).toHaveBeenCalledTimes(1));
    expect(ClusterApi.postPersistData).toHaveBeenCalledTimes(1);
    const resetPayload = JSON.parse(
      vi.mocked(ClusterApi.postPersistData).mock.calls[0][0],
    );
    expect(JSON.parse(resetPayload["wizard-data"])).toEqual({});
  });

  it("shows recovery load failure and retries before rendering the wizard", async () => {
    vi.spyOn(ClusterApi, "getPersistData")
      .mockRejectedValueOnce(new Error("load failed"))
      .mockResolvedValueOnce(
        {} as Awaited<ReturnType<typeof ClusterApi.getPersistData>>,
      );
    const stepWizardUtilities = {
      wizardSteps: { 1: { name: "GET_STARTED" } },
      currentStep: { name: "GET_STARTED" },
      jumpToStep: vi.fn(),
    };

    render(
      <MemoryRouter>
        <AppContext.Provider
          value={
            { clusterName: "c1" } as unknown as ComponentProps<
              typeof AppContext.Provider
            >["value"]
          }
        >
          <KerberosWizardProvider stepWizardUtilities={stepWizardUtilities}>
            <div>Wizard content</div>
          </KerberosWizardProvider>
        </AppContext.Provider>
      </MemoryRouter>,
    );

    expect(await screen.findByText(
      "Ambari could not load the Enable Kerberos recovery state.",
    )).toBeTruthy();
    expect(screen.queryByText("Wizard content")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("Wizard content")).toBeTruthy();
    expect(ClusterApi.getPersistData).toHaveBeenCalledTimes(2);
  });

  it("makes a checkpoint save failure visible and retryable", async () => {
    vi.spyOn(ClusterApi, "getPersistData").mockResolvedValue(
      {} as Awaited<ReturnType<typeof ClusterApi.getPersistData>>,
    );
    vi.spyOn(ClusterApi, "postPersistData")
      .mockRejectedValueOnce(new Error("save failed"))
      .mockResolvedValueOnce(
        {} as Awaited<ReturnType<typeof ClusterApi.postPersistData>>,
      );
    const stepWizardUtilities = {
      wizardSteps: { 1: { name: "GET_STARTED" } },
      currentStep: { name: "GET_STARTED" },
      jumpToStep: vi.fn(),
    };

    function SaveButton() {
      const { flushStateToDb } = useContext(EnableKerberosContext);
      return (
        <button onClick={() => void flushStateToDb().catch(() => undefined)}>
          Save checkpoint
        </button>
      );
    }

    render(
      <MemoryRouter>
        <AppContext.Provider
          value={
            { clusterName: "c1" } as unknown as ComponentProps<
              typeof AppContext.Provider
            >["value"]
          }
        >
          <KerberosWizardProvider stepWizardUtilities={stepWizardUtilities}>
            <SaveButton />
          </KerberosWizardProvider>
        </AppContext.Provider>
      </MemoryRouter>,
    );

    fireEvent.click(await screen.findByRole("button", { name: "Save checkpoint" }));
    expect(await screen.findByText(
      "Ambari could not save the Enable Kerberos recovery state.",
    )).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    await waitFor(() => expect(ClusterApi.postPersistData).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByText(
      "Ambari could not save the Enable Kerberos recovery state.",
    )).toBeNull());
  });
});
