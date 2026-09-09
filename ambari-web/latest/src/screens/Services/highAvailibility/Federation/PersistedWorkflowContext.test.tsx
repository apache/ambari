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
import { PersistedWorkflowProvider } from "./PersistedWorkflowContext";

const mocks = vi.hoisted(() => ({
  getPersistData: vi.fn(),
  reload: vi.fn(),
  release: vi.fn(),
  savePersistData: vi.fn(),
}));

vi.mock("../../../../hooks/useClusterWorkflowPersistence", () => ({
  default: () => mocks,
}));
vi.mock("../../../../hooks/useClusterNavigate", () => ({
  default: () => vi.fn(),
}));
vi.mock("../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: () => true }),
}));

describe("generic HA workflow recovery", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.reload.mockResolvedValue({});
    mocks.release.mockResolvedValue(undefined);
    mocks.savePersistData.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("routes redacted data to the first editable step without saving it", async () => {
    mocks.getPersistData.mockResolvedValue({
      ROUTER_FEDERATION: {
        activeStep: "CONFIGURE",
        steps: {
          SELECT_HOSTS: { credential: { requires_reentry: true } },
        },
      },
    });
    const jumpToStep = vi.fn();

    render(
      <PersistedWorkflowProvider
        storageKey="ROUTER_FEDERATION"
        controllerName="routerFederationWizardController"
        progressStatus="ENABLING_ROUTER_FEDERATION"
        progressStepIndex={3}
        summaryPath="/main/services/HDFS/summary"
        stepWizardUtilities={{
          activeStep: 0,
          jumpToStep,
          wizardSteps: {
            0: { name: "GET_STARTED" },
            1: { name: "SELECT_HOSTS" },
            3: { name: "CONFIGURE" },
          },
        }}
      >
        <div>Workflow content</div>
      </PersistedWorkflowProvider>,
    );

    expect(await screen.findByText(/credential values were removed/)).toBeTruthy();
    await waitFor(() => expect(jumpToStep).toHaveBeenCalledWith(1, true));
    expect(mocks.savePersistData).not.toHaveBeenCalled();
  });
});
