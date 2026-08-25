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
import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  fetchBackgroundOperationsSnapshot: vi.fn(),
  getRequestById: vi.fn(),
  getRequests: vi.fn(),
  hasAuthorization: vi.fn(),
  isClusterUser: vi.fn(),
  toastError: vi.fn(),
  updateRequest: vi.fn(),
}));

vi.mock("../../api/clusterApi", () => ({
  default: {
    getRequestById: mocks.getRequestById,
    getRequests: mocks.getRequests,
    updateRequest: mocks.updateRequest,
  },
}));
vi.mock("../../hooks/useAuth", () => ({
  useAuth: () => ({
    hasAuthorization: mocks.hasAuthorization,
    isClusterUser: mocks.isClusterUser,
  }),
}));
vi.mock("react-hot-toast", () => ({
  default: { error: mocks.toastError },
}));
type TestRow = Record<string, unknown>;
type TestColumn = {
  id?: string;
  cell?: (info: { row: { original: TestRow } }) => React.ReactNode;
};
vi.mock("../../components/Table", () => ({
  default: ({ columns, data }: { columns: TestColumn[]; data: TestRow[] }) => (
    <div>
      {data.map((item, rowIndex) => (
        <div key={rowIndex}>
          {columns.map((column, columnIndex) => (
            <span key={`${column.id || columnIndex}-${rowIndex}`}>
              {typeof column.cell === "function"
                ? column.cell({ row: { original: item } })
                : null}
            </span>
          ))}
        </div>
      ))}
    </div>
  ),
}));
vi.mock("../../components/Tooltip", () => ({
  default: ({ message, children }: { message: string; children: React.ReactNode }) => (
    <span title={message}>{children}</span>
  ),
}));
vi.mock("../../components/ConfirmationModal", () => ({
  default: ({ isOpen, modalTitle, successCallback, isOkDisabled }: {
    isOpen: boolean;
    modalTitle: string;
    successCallback: () => void;
    isOkDisabled?: boolean;
  }) => isOpen ? (
    <div>
      <span>{modalTitle}</span>
      <button disabled={isOkDisabled} onClick={successCallback}>Confirm abort</button>
    </div>
  ) : null,
}));
vi.mock("./Filters", () => ({ default: () => <div>Filters</div> }));
vi.mock("./HostProgress", () => ({ default: () => <div>Hosts</div> }));
vi.mock("./TasksList", () => ({ default: () => <div>Tasks</div> }));
vi.mock("./TaskLogs", () => ({ default: () => <div>Logs</div> }));

import BackgroundOperations from "./index";

const runningRequest = {
  Requests: {
    id: 7,
    progress_percent: 30,
    request_context: "Restart HDFS",
    request_status: "IN_PROGRESS",
    start_time: Date.now(),
    end_time: 0,
    user_name: "admin",
  },
};

function renderOperations(
  onClose = vi.fn(),
  contextOverrides: Record<string, unknown> = {},
) {
  function Harness() {
    const [pageSize, setPageSize] = useState(20);
    const context = {
      backgroundOperations: [runningRequest],
      backgroundOperationsPageSize: pageSize,
      clusterName: "c1",
      fetchBackgroundOperationsSnapshot: mocks.fetchBackgroundOperationsSnapshot,
      isClusterInstalled: true,
      parsedSocketMessages: [],
      runningOperationsCount: 1,
      setUserBgPreferences: vi.fn(),
      setBackgroundOperationsPageSize: setPageSize,
      updateBackgroundOperations: vi.fn(),
      userBgPreferences: true,
      ...contextOverrides,
    } as unknown as React.ContextType<typeof AppContext>;
    return (
      <AppContext.Provider value={context}>
        <BackgroundOperations isOpen isExplicitClick onClose={onClose} />
      </AppContext.Provider>
    );
  }
  return render(
    <Harness />,
  );
}

function renderRequestProgress(requestId: number) {
  const context = {
    backgroundOperations: [runningRequest],
    backgroundOperationsPageSize: 20,
    clusterName: "c1",
    fetchBackgroundOperationsSnapshot: mocks.fetchBackgroundOperationsSnapshot,
    isClusterInstalled: true,
    parsedSocketMessages: [],
    runningOperationsCount: 1,
    setUserBgPreferences: vi.fn(),
    setBackgroundOperationsPageSize: vi.fn(),
    updateBackgroundOperations: vi.fn(),
    userBgPreferences: true,
  } as unknown as React.ContextType<typeof AppContext>;
  render(
    <AppContext.Provider value={context}>
      <BackgroundOperations
        isOpen
        isExplicitClick
        requestId={requestId}
        onClose={vi.fn()}
      />
    </AppContext.Provider>,
  );
}

describe("Background Operations permissions and abort recovery", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getRequests.mockResolvedValue({ items: [runningRequest], itemTotal: 1 });
    mocks.fetchBackgroundOperationsSnapshot.mockImplementation(async () => ({
      items: [runningRequest],
      itemTotal: 30,
    }));
    mocks.isClusterUser.mockReturnValue(false);
  });

  afterEach(() => cleanup());

  it("does not expose Abort without SERVICE.START_STOP", async () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderOperations();

    await waitFor(() => expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledWith(20));
    expect(screen.queryByRole("button", { name: "Abort Operation" })).toBeNull();
  });

  it("does not expose Abort while another user owns a wizard", async () => {
    mocks.hasAuthorization.mockReturnValue(true);
    renderOperations(vi.fn(), { isNonWizardUser: true });

    await waitFor(() => expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledWith(20));
    expect(screen.queryByRole("button", { name: "Abort Operation" })).toBeNull();
  });

  it("uses the rolling-upgrade support flag for Abort capability", async () => {
    mocks.hasAuthorization.mockReturnValue(true);
    const { unmount } = renderOperations(vi.fn(), {
      supports: { opsDuringRollingUpgrade: false },
      upgradeIsRunning: true,
      upgradeSuspended: false,
    });

    await waitFor(() => expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledWith(20));
    expect(screen.queryByRole("button", { name: "Abort Operation" })).toBeNull();

    unmount();
    mocks.fetchBackgroundOperationsSnapshot.mockClear();
    renderOperations(vi.fn(), {
      supports: { opsDuringRollingUpgrade: true },
      upgradeIsRunning: true,
      upgradeSuspended: false,
    });

    expect(await screen.findByRole("button", { name: "Abort Operation" })).toBeTruthy();
  });

  it("opens a supplied request at its host progress level", async () => {
    mocks.hasAuthorization.mockReturnValue(false);
    mocks.getRequestById.mockResolvedValue({ Requests: runningRequest.Requests });
    renderRequestProgress(7);

    expect(await screen.findByText("Hosts")).toBeTruthy();
    await waitFor(() => expect(mocks.getRequestById).toHaveBeenCalledWith("c1", 7));
  });

  it("keeps Show More page size in the shared background snapshot", async () => {
    mocks.hasAuthorization.mockReturnValue(false);
    renderOperations();

    await waitFor(() => expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledWith(20));
    fireEvent.click(screen.getByText("Show More..."));
    await waitFor(() => expect(mocks.fetchBackgroundOperationsSnapshot).toHaveBeenCalledWith(30));
  });

  it("locks Abort while submitting and recovers after a server failure", async () => {
    let rejectAbort: (error: unknown) => void = () => undefined;
    mocks.hasAuthorization.mockReturnValue(true);
    mocks.updateRequest.mockReturnValue(new Promise((_, reject) => {
      rejectAbort = reject;
    }));
    renderOperations();

    fireEvent.click(await screen.findByRole("button", { name: "Abort Operation" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirm abort" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Confirm abort" }).hasAttribute("disabled")).toBe(true));
    fireEvent.click(screen.getByRole("button", { name: "Confirm abort" }));
    expect(mocks.updateRequest).toHaveBeenCalledTimes(1);

    rejectAbort({ response: { data: { message: "Abort rejected" } } });
    await waitFor(() => expect(mocks.toastError).toHaveBeenCalledWith("Abort rejected"));
    expect(screen.getByRole("button", { name: "Confirm abort" }).hasAttribute("disabled")).toBe(false);
  });

  it("closes without loading requests for a cluster user", async () => {
    const onClose = vi.fn();
    mocks.isClusterUser.mockReturnValue(true);
    renderOperations(onClose);

    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(mocks.getRequests).not.toHaveBeenCalled();
    expect(mocks.fetchBackgroundOperationsSnapshot).not.toHaveBeenCalled();
    expect(screen.queryByText("Background Operation")).toBeNull();
  });
});
