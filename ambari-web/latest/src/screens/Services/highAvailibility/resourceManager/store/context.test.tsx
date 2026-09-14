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

const mocks = vi.hoisted(() => ({
  getPersistData: vi.fn(),
  reload: vi.fn(),
  release: vi.fn(),
  savePersistData: vi.fn(),
  modalHide: vi.fn(),
}));

vi.mock("../../../../../hooks/useClusterWorkflowPersistence", () => ({
  default: () => mocks,
}));
vi.mock("../../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: () => true }),
}));
vi.mock("../../../../../store/ModalManager", () => ({
  default: { hide: mocks.modalHide },
}));

import {
  EnableHighAvailibilityContext,
  EnableHighAvailibilityProvider,
} from "./context";
import { ClusterProgressStatus } from "../../../../../constants";

function StateProbe() {
  const { state, flushStateToDb } = useContext(
    EnableHighAvailibilityContext,
  );
  return (
    <>
      <div data-testid="state">{JSON.stringify(state)}</div>
      <button onClick={() => void flushStateToDb()}>Persist</button>
      <button onClick={() => void flushStateToDb("complete")}>Complete</button>
    </>
  );
}

function renderProvider(activeStep = 1) {
  const jumpToStep = vi.fn();
  const wizardUtilities = {
    activeStep,
    jumpToStep,
    prevStepNumber: activeStep - 1,
    nextStepNumber: activeStep + 1,
    wizardSteps: {
      1: { name: "GET_STARTED" },
      2: { name: "SELECT_HOSTS" },
      3: { name: "REVIEW" },
      4: { name: "CONFIGURE_COMPONENTS" },
    },
  };
  const result = render(
    <EnableHighAvailibilityProvider stepWizardUtilities={wizardUtilities}>
      <StateProbe />
    </EnableHighAvailibilityProvider>,
  );
  return { ...result, jumpToStep };
}

describe("ResourceManager HA workflow persistence", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getPersistData.mockResolvedValue({});
    mocks.reload.mockResolvedValue({});
    mocks.release.mockResolvedValue(undefined);
    mocks.savePersistData.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("treats a missing checkpoint as a fresh Step 1 workflow", async () => {
    const { jumpToStep } = renderProvider();

    expect(await screen.findByTestId("state")).toBeTruthy();
    expect(jumpToStep).toHaveBeenCalledWith(1, true);
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });

  it("hydrates Step 4 request IDs before rendering children", async () => {
    mocks.getPersistData.mockResolvedValue(
      { HIGH_AVAILIBILITY_RM_HA: {
        activeStep: "CONFIGURE_COMPONENTS",
        enableHighAvailibilitySteps: {
          CONFIGURE_COMPONENTS: {
            step: "CONFIGURE_COMPONENTS",
            data: {
              operationsState: [
                {
                  id: "stop-required-services",
                  label: "Stop Required Services",
                  skippable: false,
                  status: "IN_PROGRESS",
                  requestId: 71,
                },
              ],
            },
          },
        },
      } },
    );
    const { jumpToStep } = renderProvider();

    await waitFor(() => expect(jumpToStep).toHaveBeenCalledWith(4, true));
    expect(screen.getByTestId("state").textContent).toContain(
      '"requestId":71',
    );
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });

  it("persists each recoverable checkpoint in the scoped workflow", async () => {
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Persist" }));

    await waitFor(() => expect(mocks.savePersistData).toHaveBeenCalledOnce());
    expect(mocks.savePersistData).toHaveBeenCalledWith({
      HIGH_AVAILIBILITY_RM_HA: expect.objectContaining({ activeStep: "" }),
      CLUSTER_STATE: {},
    }, ClusterProgressStatus.ENABLING_RM_HA);
  });

  it("releases the scoped workflow only after completion", async () => {
    renderProvider(4);

    fireEvent.click(await screen.findByRole("button", { name: "Complete" }));

    await waitFor(() => expect(mocks.release).toHaveBeenCalledOnce());
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });
});
