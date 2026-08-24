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

import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ClusterProgressStatus } from "../../../../constants";
import { AppContext } from "../../../../store/context";

const mocks = vi.hoisted(() => ({
  activeStep: 1,
  authorized: true,
  loadRangerAdminComponent: vi.fn(),
  clearPersistedState: vi.fn(),
}));

type MockModalProps = {
  isOpen: boolean;
  onClose: () => void;
  modalBody: React.ReactNode;
  options: { shouldShowFooter?: boolean };
  successCallback: () => void;
};
type MockConfirmationProps = {
  isOpen: boolean;
  modalTitle: string;
  modalBody: React.ReactNode;
};

vi.mock("../../../../hooks/useStepWizard", () => ({
  default: () => ({
    activeStep: mocks.activeStep,
    wizardSteps: {},
    jumpToStep: vi.fn(),
  }),
}));
vi.mock("../../../../hooks/useAuth", () => ({
  default: () => ({ hasAuthorization: () => mocks.authorized }),
}));
vi.mock("./rangerAdminHaApi", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./rangerAdminHaApi")>()),
  rangerAdminEnablementApi: {
    loadRangerAdminComponent: mocks.loadRangerAdminComponent,
  },
}));
vi.mock("./store/context", () => ({
  clearRangerAdminHaPersistedState: mocks.clearPersistedState,
  EnableHighAvailibilityProvider: ({
    children,
  }: {
    children: React.ReactNode;
  }) => children,
  EnableHighAvailibilityRangerAdminContext: {},
}));
vi.mock("./wizardSteps", () => ({ default: {} }));
vi.mock("../../../../components/StepWizard", () => ({
  default: () => <div>Wizard ready</div>,
}));
vi.mock("../../../../components/Spinner", () => ({
  default: () => <div>Loading</div>,
}));
vi.mock("../../../../components/Modal", () => ({
  default: ({
    isOpen,
    onClose,
    modalBody,
    options,
    successCallback,
  }: MockModalProps) =>
    isOpen ? (
      <div>
        {modalBody}
        <button onClick={onClose}>Close</button>
        {options.shouldShowFooter ? (
          <button onClick={successCallback}>Retry</button>
        ) : null}
      </div>
    ) : null,
}));
vi.mock("../../../../components/ConfirmationModal", () => ({
  default: ({ isOpen, modalTitle, modalBody }: MockConfirmationProps) =>
    isOpen ? (
      <div>
        <div>{modalTitle}</div>
        <div>{modalBody}</div>
      </div>
    ) : null,
}));

import ValidateEnablement from "./ValidateEnablement";

function componentResponse(states: string[]) {
  return {
    host_components: states.map((state, index) => ({
      HostRoles: {
        host_name: `ra-${index + 1}.example.com`,
        state,
      },
    })),
  };
}

function renderValidation({
  clusterState = {},
  allHostNames = ["host-1", "host-2", "host-3"],
}: {
  clusterState?: unknown;
  allHostNames?: string[];
} = {}) {
  return render(
    <AppContext.Provider
      value={{ clusterName: "c1", clusterState, allHostNames } as never}
    >
      <ValidateEnablement />
    </AppContext.Provider>,
  );
}

describe("Ranger Admin HA entry validation", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.activeStep = 1;
    mocks.authorized = true;
    mocks.clearPersistedState.mockResolvedValue(undefined);
  });

  afterEach(() => cleanup());

  it("blocks a fresh workflow when multiple Ranger Admins already exist", async () => {
    mocks.loadRangerAdminComponent.mockResolvedValue(
      componentResponse(["STARTED", "STARTED"]),
    );
    renderValidation();

    expect(
      await screen.findByText(
        "Ranger Admin high availability is already enabled.",
      ),
    ).toBeTruthy();
    expect(screen.queryByText("Wizard ready")).toBeNull();
  });

  it.each([
    {
      name: "the cluster has one host",
      allHostNames: ["host-1"],
      response: componentResponse(["STARTED"]),
      message:
        "Ranger Admin high availability requires at least two cluster hosts.",
    },
    {
      name: "Ranger Admin is missing",
      allHostNames: ["host-1", "host-2"],
      response: componentResponse([]),
      message:
        "An installed Ranger Admin is required before Ranger Admin high availability can be enabled.",
    },
    {
      name: "Ranger Admin is in INIT state",
      allHostNames: ["host-1", "host-2"],
      response: componentResponse(["INIT"]),
      message:
        "Ranger Admin must be installed before high availability can be enabled.",
    },
    {
      name: "Ranger Admin installation failed",
      allHostNames: ["host-1", "host-2"],
      response: componentResponse(["INSTALL_FAILED"]),
      message:
        "Ranger Admin must be installed before high availability can be enabled.",
    },
  ])(
    "blocks direct fresh entry when $name",
    async ({ allHostNames, response, message }) => {
      mocks.loadRangerAdminComponent.mockResolvedValue(response);
      renderValidation({ allHostNames });

      expect(await screen.findByText(message)).toBeTruthy();
      expect(screen.queryByText("Wizard ready")).toBeNull();
    },
  );

  it("permits recovery after the install step has created another admin", async () => {
    renderValidation({
      clusterState: {
        progressStatus: ClusterProgressStatus.ENABLING_RANGER_ADMIN_HA,
      },
    });

    expect(await screen.findByText("Wizard ready")).toBeTruthy();
    expect(mocks.loadRangerAdminComponent).not.toHaveBeenCalled();
  });

  it("shows an API error and retries the prerequisite check", async () => {
    mocks.loadRangerAdminComponent
      .mockRejectedValueOnce(new Error("component lookup failed"))
      .mockResolvedValueOnce(componentResponse(["STARTED"]));
    renderValidation();

    expect(await screen.findByText("component lookup failed")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));

    expect(await screen.findByText("Wizard ready")).toBeTruthy();
    expect(mocks.loadRangerAdminComponent).toHaveBeenCalledTimes(2);
  });

  it("requires confirmation and preserves the checkpoint when Step 4 closes", async () => {
    mocks.activeStep = 4;
    mocks.loadRangerAdminComponent.mockResolvedValue(
      componentResponse(["STARTED"]),
    );
    renderValidation();

    await screen.findByText("Wizard ready");
    fireEvent.click(screen.getByRole("button", { name: "Close" }));

    await waitFor(() =>
      expect(screen.getByText("Ranger Admin HA is still running")).toBeTruthy(),
    );
    expect(mocks.clearPersistedState).not.toHaveBeenCalled();
  });
});
