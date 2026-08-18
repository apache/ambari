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
  postPersistData: vi.fn(),
}));

vi.mock("../../../../../api/clusterApi", () => ({
  default: {
    getPersistData: mocks.getPersistData,
    postPersistData: mocks.postPersistData,
  },
}));
vi.mock("../../../../../hooks/useAuth", () => ({
  default: () => ({ user: { user_name: "jn-owner" } }),
}));

import {
  ManageJournalNodesContext,
  ManageJournalNodesProvider,
} from "./context";

const jumpToStep = vi.fn();
const setStepsHidden = vi.fn();
const wizardUtilities = {
  activeStep: 0,
  jumpToStep,
  setStepsHidden,
  wizardSteps: {
    0: { name: "ASSIGN_JOURNALNODES" },
    2: { name: "SAVE_NAMESPACE" },
    3: { name: "ADD_REMOVE_JOURNALNODES" },
  },
};

function StateProbe() {
  const { state, flushStateToDb } = useContext(ManageJournalNodesContext);
  return (
    <>
      <div data-testid="state">{JSON.stringify(state)}</div>
      <button onClick={() => void flushStateToDb()}>Persist</button>
    </>
  );
}

function renderProvider() {
  return render(
    <ManageJournalNodesProvider stepWizardUtilities={wizardUtilities}>
      <StateProbe />
    </ManageJournalNodesProvider>,
  );
}

describe("Manage JournalNodes workflow hydration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.postPersistData.mockResolvedValue({});
  });

  afterEach(() => cleanup());

  it("treats a missing persisted value as a fresh workflow", async () => {
    mocks.getPersistData.mockRejectedValue({ status: 404 });
    renderProvider();

    expect(await screen.findByTestId("state")).toBeTruthy();
    expect(setStepsHidden).toHaveBeenCalledWith([2, 4], false);
    expect(jumpToStep).toHaveBeenCalledWith(0, true);
    expect(mocks.postPersistData).not.toHaveBeenCalled();
  });

  it("restores delete-only hidden steps from a string-valued checkpoint", async () => {
    mocks.getPersistData.mockResolvedValue(
      JSON.stringify({
        activeStep: "SAVE_NAMESPACE",
        manageJournalNodesSteps: {
          REVIEW: { step: "REVIEW", data: { isDeleteOnly: true } },
        },
      }),
    );
    renderProvider();

    await waitFor(() => expect(jumpToStep).toHaveBeenCalledWith(3, true));
    expect(setStepsHidden).toHaveBeenCalledWith([2, 4], true);
    expect(screen.getByTestId("state").textContent).toContain("isDeleteOnly");
    expect(mocks.postPersistData).not.toHaveBeenCalled();
  });

  it("persists the workflow owner with the recoverable checkpoint", async () => {
    mocks.getPersistData.mockRejectedValue({ status: 404 });
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Persist" }));

    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalledOnce());
    expect(mocks.postPersistData.mock.calls[0][0]["wizard-data"]).toBe(
      JSON.stringify({ userName: "jn-owner" }),
    );
  });
});
