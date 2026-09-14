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
import { ClusterProgressStatus } from "../../../../../constants";

const mocks = vi.hoisted(() => ({
  getPersistData: vi.fn(),
  reload: vi.fn(),
  release: vi.fn(),
  savePersistData: vi.fn(),
}));

vi.mock("../../../../../hooks/useClusterWorkflowPersistence", () => ({
  default: () => mocks,
}));
vi.mock("../../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: () => true }),
}));

import {
  EnableHighAvailibilityContext,
  EnableHighAvailibilityProvider,
} from "./context";
import { AppContext } from "../../../../../store/context";

const jumpToStep = vi.fn();
const wizardUtilities = {
  activeStep: 0,
  jumpToStep,
  wizardSteps: {
    0: { name: "GET_STARTED" },
    2: { name: "CONFIGURE_NAMESERVICE" },
    4: { name: "CONFIGURE_COMPONENTS" },
  },
};

function StateProbe() {
  const { state, flushStateToDb, showCancel } = useContext(
    EnableHighAvailibilityContext,
  );
  return (
    <>
      <div data-testid="state">{JSON.stringify(state)}</div>
      <div data-testid="show-cancel">{String(showCancel)}</div>
      <button onClick={() => void flushStateToDb()}>Persist</button>
    </>
  );
}

function renderProvider(autoRollbackHA = false) {
  return render(
    <AppContext.Provider value={{ supports: { autoRollbackHA } } as never}>
      <EnableHighAvailibilityProvider stepWizardUtilities={wizardUtilities}>
        <StateProbe />
      </EnableHighAvailibilityProvider>
    </AppContext.Provider>,
  );
}

describe("NameNode HA workflow hydration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getPersistData.mockResolvedValue({});
    mocks.reload.mockResolvedValue({});
    mocks.release.mockResolvedValue(undefined);
    mocks.savePersistData.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("treats a missing persisted value as a fresh workflow", async () => {
    renderProvider();

    expect(await screen.findByTestId("state")).toBeTruthy();
    expect(jumpToStep).toHaveBeenCalledWith(0, true);
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });

  it("restores a string-valued checkpoint before rendering children", async () => {
    mocks.getPersistData.mockResolvedValue(
      { HIGH_AVAILIBILITY_NAMENODE: {
        activeStep: "CONFIGURE_COMPONENTS",
        enableHighAvailibilitySteps: {
          REVIEW: { step: "REVIEW", data: { nameserviceId: "nameservice1" } },
        },
      } },
    );
    renderProvider();

    await waitFor(() =>
      expect(jumpToStep).toHaveBeenCalledWith(4, true),
    );
    expect(screen.getByTestId("state").textContent).toContain("nameservice1");
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });

  it("routes a redacted HA checkpoint to its editable step without auto-saving", async () => {
    mocks.getPersistData.mockResolvedValue({
      HIGH_AVAILIBILITY_NAMENODE: {
        activeStep: "CONFIGURE_COMPONENTS",
        enableHighAvailibilitySteps: {
          CONFIGURE_NAMESERVICE: {
            data: { password: { requires_reentry: true } },
          },
        },
      },
    });

    renderProvider();

    expect(await screen.findByText(/credential values were removed/)).toBeTruthy();
    expect(jumpToStep).toHaveBeenCalledWith(2, true);
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });

  it("persists the recoverable checkpoint in the scoped workflow", async () => {
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Persist" }));

    await waitFor(() => expect(mocks.savePersistData).toHaveBeenCalledOnce());
    expect(mocks.savePersistData).toHaveBeenCalledWith({
      HIGH_AVAILIBILITY_NAMENODE: expect.objectContaining({ activeStep: "" }),
      CLUSTER_STATE: {},
    }, ClusterProgressStatus.ENABLING_NAMENODE_HA);
  });

  it("locks cancellation on Classic's critical automatic-rollback phases", async () => {
    wizardUtilities.activeStep = 4;
    renderProvider(true);

    expect((await screen.findByTestId("show-cancel")).textContent).toBe("false");
    wizardUtilities.activeStep = 0;
  });
});
