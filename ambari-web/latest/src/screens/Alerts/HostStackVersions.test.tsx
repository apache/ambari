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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import IHost from "../../models/host";
import HostStackVersion from "../../models/hostStackVersion";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  hasAuthorization: vi.fn(),
  installHostStackVersion: vi.fn(),
}));

vi.mock("../../api/versionsApi", () => ({
  default: { installHostStackVersion: mocks.installHostStackVersion },
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({ hasAuthorization: mocks.hasAuthorization }),
}));
vi.mock("../../Utils/Utility", () => ({
  translateWithVariables: (_key: string, replacements: Record<string, string>) => (
    `Install ${replacements["0"]} on this host?`
  ),
}));
vi.mock("../../components/Paginator", () => ({ default: () => <div>Pagination</div> }));
vi.mock("../../components/ConfirmationModal", () => ({
  default: ({ isOpen, modalBody, successCallback, okButtonText, isOkDisabled }: any) => isOpen ? (
    <div>
      {modalBody}
      <button disabled={isOkDisabled} onClick={successCallback}>
        Confirm {okButtonText}
      </button>
    </div>
  ) : null,
}));
vi.mock("../BackgroundOperations", () => ({
  default: ({ requestId }: { requestId: string | number }) => <div>Request {requestId}</div>,
}));
vi.mock("../../components/Table", () => ({
  default: ({ columns, data }: { columns: any[]; data: any[] }) => (
    <div>
      {data.map((item) => (
        <div key={item.key}>
          {columns.map((column, index) => (
            <span key={`${column.id || column.accessorKey || index}-${item.key}`}>
              {column.cell
                ? column.cell({ row: { original: item } })
                : column.accessorFn
                  ? column.accessorFn(item)
                  : item[column.accessorKey]}
            </span>
          ))}
        </div>
      ))}
    </div>
  ),
}));

import HostStackVersions from "./HostStackVersions";

function stackVersion(
  status: string,
  displayName: string,
  isVisible = true,
) {
  return new HostStackVersion({
    displayName,
    hostName: "host1",
    isVisible,
    repoVersion: `${displayName}-repo`,
    stack: "HDP-3.1",
    status,
    version: "3.1.5",
  } as any);
}

function renderVersions(isNonWizardUser = false) {
  const host = {
    hostName: "host1",
    stackVersions: [
      stackVersion("CURRENT", "Current version"),
      stackVersion("OUT_OF_SYNC", "Candidate version"),
      stackVersion("INSTALL_FAILED", "Hidden version", false),
    ],
  } as IHost;
  return render(
    <AppContext.Provider value={{
      backgroundOperations: [],
      clusterName: "c1",
      isNonWizardUser,
    } as any}>
      <HostStackVersions host={host} />
    </AppContext.Provider>,
  );
}

describe("Host Stack Versions", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.hasAuthorization.mockReturnValue(true);
    mocks.installHostStackVersion.mockResolvedValue({ Requests: { id: 17 } });
  });

  afterEach(() => cleanup());

  it("shows only visible host versions and hides installation without permission", () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderVersions();

    expect(screen.getAllByText("Current version").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Candidate version").length).toBeGreaterThan(0);
    expect(screen.queryByText("Hidden version")).toBeNull();
    expect(screen.queryByRole("button", { name: "Install" })).toBeNull();
  });

  it("submits an eligible version and opens its request progress", async () => {
    renderVersions();
    const installButton = screen.getAllByRole("button", { name: "Install" })
      .find((button) => !button.hasAttribute("disabled"));
    fireEvent.click(installButton!);
    fireEvent.click(screen.getByRole("button", { name: "Confirm Install" }));

    await waitFor(() => expect(mocks.installHostStackVersion).toHaveBeenCalledWith(
      "c1",
      "host1",
      expect.objectContaining({
        displayName: "Candidate version",
        repoVersion: "Candidate version-repo",
      }),
    ));
    expect(await screen.findByText("Request 17")).toBeTruthy();
    expect(screen.getByRole("button", { name: "Installing" })).toBeTruthy();
  });

  it("keeps confirmation open and retries a failed installation", async () => {
    mocks.installHostStackVersion
      .mockRejectedValueOnce({ response: { data: { message: "Repository unavailable" } } })
      .mockResolvedValueOnce({ Requests: { id: 18 } });
    renderVersions();
    const installButton = screen.getAllByRole("button", { name: "Install" })
      .find((button) => !button.hasAttribute("disabled"));
    fireEvent.click(installButton!);
    fireEvent.click(screen.getByRole("button", { name: "Confirm Install" }));

    expect(await screen.findByText("Repository unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Confirm Retry" }));
    await waitFor(() => expect(mocks.installHostStackVersion).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("Request 18")).toBeTruthy();
  });

  it("disables installation while another user owns the wizard", () => {
    renderVersions(true);
    const eligibleButton = screen.getAllByRole("button", { name: "Install" })[1];
    expect(eligibleButton.hasAttribute("disabled")).toBe(true);
  });
});
