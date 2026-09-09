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
import { useContext } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { MemoryRouter, useLocation } from "react-router-dom";

const mocks = vi.hoisted(() => ({
  getPersistData: vi.fn(),
  reload: vi.fn(),
  release: vi.fn(),
  savePersistData: vi.fn(),
}));

vi.mock("../../../../hooks/useClusterWorkflowPersistence", () => ({
  default: () => ({
    getPersistData: mocks.getPersistData,
    reload: mocks.reload,
    release: mocks.release,
    savePersistData: mocks.savePersistData,
  }),
}));

import { AddHostContext, AddHostProvider } from "./context";
import { ActionTypes } from "./types";

const wizardUtilities = {
  currentStep: { name: "HOSTS" },
  jumpToStep: vi.fn(),
  wizardSteps: {
    1: { name: "HOSTS" },
    2: { name: "HOST_STATUS" },
    4: { name: "CONFIGURATIONS" },
  },
};

function PersistenceProbe() {
  const { dispatch, flushStateToDb } = useContext(AddHostContext);
  return (
    <>
      <button onClick={() => void flushStateToDb("default")}>Persist</button>
      <button onClick={() => void flushStateToDb("cancel").catch(() => undefined)}>
        Cancel
      </button>
      <button onClick={() => {
        dispatch({
          type: ActionTypes.STORE_INFORMATION,
          payload: { step: "REVIEW", data: { completed: true } },
        });
        void flushStateToDb("next");
      }}>
        Persist next
      </button>
    </>
  );
}

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
}

function renderProvider() {
  return render(
    <MemoryRouter initialEntries={["/main/host/add/step1"]}>
      <AppContext.Provider value={{
        clusterName: "",
        serviceComponentInfo: [],
        services: [],
      } as any}>
        <AddHostProvider stepWizardUtilities={wizardUtilities}>
          <PersistenceProbe />
          <LocationProbe />
        </AddHostProvider>
      </AppContext.Provider>
    </MemoryRouter>,
  );
}

describe("Add Host persistence", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getPersistData.mockResolvedValue({ ADD_HOST: {}, CLUSTER_STATE: {} });
    mocks.reload.mockResolvedValue({ ADD_HOST: {}, CLUSTER_STATE: {} });
    mocks.release.mockResolvedValue(undefined);
    mocks.savePersistData.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("does not overwrite persisted state when hydration fails", async () => {
    mocks.getPersistData
      .mockRejectedValueOnce({ response: { data: { message: "Restore failed" } } });
    renderProvider();

    expect(await screen.findByText("Restore failed")).toBeTruthy();
    expect(mocks.savePersistData).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByRole("button", { name: "Persist" })).toBeTruthy();
    expect(mocks.getPersistData).toHaveBeenCalledOnce();
    expect(mocks.reload).toHaveBeenCalledOnce();
  });

  it("routes a redacted SSH checkpoint to Install Options without auto-saving it", async () => {
    mocks.getPersistData.mockResolvedValue({
      ADD_HOST: {
        activeStep: "INSTALL_START_TEST",
        addHostSteps: {
          HOSTS: { data: { installOptions: { requires_reentry: true } } },
        },
      },
      CLUSTER_STATE: { stepName: "INSTALL_START_TEST" },
    });

    renderProvider();

    expect(await screen.findByText(/credential values were removed/)).toBeTruthy();
    expect(wizardUtilities.jumpToStep).toHaveBeenCalledWith(1, true);
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });

  it("writes one scoped workflow snapshot without raw persistence", async () => {
    renderProvider();

    const persist = await screen.findByRole("button", { name: "Persist" });
    fireEvent.click(persist);
    await waitFor(() => expect(mocks.savePersistData).toHaveBeenCalledOnce());
    expect(mocks.savePersistData).toHaveBeenCalledWith({
      ADD_HOST: { activeStep: "" , addHostSteps: {} },
      CLUSTER_STATE: {},
    }, "ADD_HOST");
  });

  it("persists same-event checkpoints with the destination step before resolving", async () => {
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Persist next" }));
    await waitFor(() => expect(mocks.savePersistData).toHaveBeenCalled());
    const snapshots = mocks.savePersistData.mock.calls.map(([payload]) => payload.ADD_HOST);

    expect(snapshots).toContainEqual(expect.objectContaining({
      activeStep: "HOST_STATUS",
      addHostSteps: expect.objectContaining({
        REVIEW: {
          step: "REVIEW",
          data: { completed: true },
        },
      }),
    }));
  });

  it("clears persisted state before returning to the Hosts route", async () => {
    let completeRelease: () => void = () => undefined;
    mocks.release.mockReturnValueOnce(new Promise<void>((resolve) => {
      completeRelease = resolve;
    }));
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(mocks.release).toHaveBeenCalledOnce());
    expect(screen.getByTestId("location").textContent).toBe("/main/host/add/step1");

    completeRelease();
    await waitFor(() => {
      expect(screen.getByTestId("location").textContent).toBe("/main/hosts");
    });
  });

  it("stays in the wizard when persisted state cannot be cleared", async () => {
    mocks.release.mockRejectedValueOnce(new Error("Clear failed"));
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(mocks.release).toHaveBeenCalledOnce());
    expect(screen.getByTestId("location").textContent).toBe("/main/host/add/step1");
  });
});
