/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../../../store/context";
import { ReassignProvider } from "./context";

const mocks = vi.hoisted(() => ({
  getPersistData: vi.fn(),
  reload: vi.fn(),
  release: vi.fn(),
  savePersistData: vi.fn(),
}));

vi.mock("../../../../hooks/useClusterWorkflowPersistence", () => ({
  default: () => mocks,
}));
vi.mock("../../../../hooks/useDebounce", () => ({
  useDebounce: () => vi.fn(),
}));

describe("component reassignment recovery", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.reload.mockResolvedValue({});
    mocks.release.mockResolvedValue(undefined);
    mocks.savePersistData.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("routes redacted configuration to Review without background persistence", async () => {
    mocks.getPersistData.mockResolvedValue({
      REASSIGN_COMPONENT: {
        activeStep: "INSTALL_COMPONENT",
        reassignSteps: {
          REVIEW: {
            data: { serviceConfigProperties: [{ requires_reentry: true }] },
          },
        },
      },
    });
    const jumpToStep = vi.fn();

    render(
      <AppContext.Provider value={{ navigateCluster: vi.fn() } as any}>
        <ReassignProvider
          hasManualCommands={false}
          stepWizardUtilities={{
            activeStep: 5,
            currentStep: { name: "INSTALL_COMPONENT" },
            jumpToStep,
            wizardSteps: {
              1: { name: "GET_STARTED" },
              3: { name: "REVIEW" },
            },
          }}
        >
          <div>Workflow content</div>
        </ReassignProvider>
      </AppContext.Provider>,
    );

    expect(await screen.findByText(/credential values were removed/)).toBeTruthy();
    await waitFor(() => expect(jumpToStep).toHaveBeenCalledWith(3, true));
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });
});
