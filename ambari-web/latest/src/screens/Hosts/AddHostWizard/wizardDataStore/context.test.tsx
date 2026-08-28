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
  postPersistData: vi.fn(),
  releaseWizard: vi.fn(),
}));

vi.mock("../../../../api/clusterApi", () => ({
  default: {
    getPersistData: mocks.getPersistData,
    postPersistData: mocks.postPersistData,
  },
}));

vi.mock("../../../../Utils/wizardOwnership", () => ({
  claimWizard: vi.fn(),
  releaseWizard: mocks.releaseWizard,
}));

import { AddHostContext, AddHostProvider } from "./context";
import { ActionTypes } from "./types";

const wizardUtilities = {
  currentStep: { name: "INSTALL_OPTIONS" },
  jumpToStep: vi.fn(),
  wizardSteps: {
    1: { name: "INSTALL_OPTIONS" },
    2: { name: "HOST_STATUS" },
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
    mocks.postPersistData.mockResolvedValue({});
    mocks.releaseWizard.mockResolvedValue({});
  });

  afterEach(() => cleanup());

  it("does not overwrite persisted state when hydration fails", async () => {
    mocks.getPersistData
      .mockRejectedValueOnce({ response: { data: { message: "Restore failed" } } })
      .mockResolvedValueOnce({});
    renderProvider();

    expect(await screen.findByText("Restore failed")).toBeTruthy();
    expect(mocks.postPersistData).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByRole("button", { name: "Persist" })).toBeTruthy();
    expect(mocks.getPersistData).toHaveBeenCalledTimes(3);
  });

  it("serializes persistence requests", async () => {
    let completeFirstWrite: () => void = () => undefined;
    mocks.getPersistData.mockResolvedValue({});
    mocks.postPersistData
      .mockReturnValueOnce(new Promise<void>((resolve) => {
        completeFirstWrite = resolve;
      }))
      .mockResolvedValueOnce({});
    renderProvider();

    const persist = await screen.findByRole("button", { name: "Persist" });
    fireEvent.click(persist);
    fireEvent.click(persist);
    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalledTimes(1));

    completeFirstWrite();
    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalledTimes(2));
  });

  it("persists same-event checkpoints with the destination step before resolving", async () => {
    mocks.getPersistData.mockResolvedValue({});
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Persist next" }));
    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalled());
    const snapshots = mocks.postPersistData.mock.calls.map(([payload]) => {
      const outer = JSON.parse(payload);
      return JSON.parse(outer.ADD_HOST);
    });

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
    mocks.getPersistData.mockResolvedValue({});
    mocks.releaseWizard.mockReturnValueOnce(new Promise<void>((resolve) => {
      completeRelease = resolve;
    }));
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalledTimes(1));
    const persistedData = JSON.parse(mocks.postPersistData.mock.calls[0][0]);
    expect(JSON.parse(persistedData.ADD_HOST)).toEqual({ addHostSteps: {} });
    expect(JSON.parse(persistedData.CLUSTER_STATE)).toEqual({});
    expect(mocks.releaseWizard).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("location").textContent).toBe("/main/host/add/step1");

    completeRelease();
    await waitFor(() => {
      expect(screen.getByTestId("location").textContent).toBe("/main/hosts");
    });
  });

  it("stays in the wizard when persisted state cannot be cleared", async () => {
    mocks.getPersistData.mockResolvedValue({});
    mocks.postPersistData.mockRejectedValueOnce(new Error("Clear failed"));
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalledTimes(1));
    expect(mocks.releaseWizard).not.toHaveBeenCalled();
    expect(screen.getByTestId("location").textContent).toBe("/main/host/add/step1");
  });
});
