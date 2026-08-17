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
  default: () => ({ user: { user_name: "ha-owner" } }),
}));

import {
  EnableHighAvailibilityContext,
  EnableHighAvailibilityProvider,
} from "./context";

const jumpToStep = vi.fn();
const wizardUtilities = {
  activeStep: 0,
  jumpToStep,
  wizardSteps: {
    0: { name: "GET_STARTED" },
    4: { name: "CONFIGURE_COMPONENTS" },
  },
};

function StateProbe() {
  const { state, flushStateToDb } = useContext(EnableHighAvailibilityContext);
  return (
    <>
      <div data-testid="state">{JSON.stringify(state)}</div>
      <button onClick={() => void flushStateToDb()}>Persist</button>
    </>
  );
}

function renderProvider() {
  return render(
    <EnableHighAvailibilityProvider stepWizardUtilities={wizardUtilities}>
      <StateProbe />
    </EnableHighAvailibilityProvider>,
  );
}

describe("NameNode HA workflow hydration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.postPersistData.mockResolvedValue({});
  });

  afterEach(() => cleanup());

  it("treats a missing persisted value as a fresh workflow", async () => {
    mocks.getPersistData.mockRejectedValue({ response: { status: 404 } });
    renderProvider();

    expect(await screen.findByTestId("state")).toBeTruthy();
    expect(jumpToStep).toHaveBeenCalledWith(0, true);
    expect(mocks.postPersistData).not.toHaveBeenCalled();
  });

  it("restores a string-valued checkpoint before rendering children", async () => {
    mocks.getPersistData.mockResolvedValue(
      JSON.stringify({
        activeStep: "CONFIGURE_COMPONENTS",
        enableHighAvailibilitySteps: {
          REVIEW: { step: "REVIEW", data: { nameserviceId: "nameservice1" } },
        },
      }),
    );
    renderProvider();

    await waitFor(() =>
      expect(jumpToStep).toHaveBeenCalledWith(4, true),
    );
    expect(screen.getByTestId("state").textContent).toContain("nameservice1");
    expect(mocks.postPersistData).not.toHaveBeenCalled();
  });

  it("persists the workflow owner with the recoverable checkpoint", async () => {
    mocks.getPersistData.mockRejectedValue({ response: { status: 404 } });
    renderProvider();

    fireEvent.click(await screen.findByRole("button", { name: "Persist" }));

    await waitFor(() => expect(mocks.postPersistData).toHaveBeenCalledOnce());
    expect(mocks.postPersistData.mock.calls[0][0]["wizard-data"]).toBe(
      JSON.stringify({ userName: "ha-owner" }),
    );
  });
});
