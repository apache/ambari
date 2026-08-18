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

import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ComponentProps, ContextType } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ProgressStatus } from "../constants";
import { AppContext } from "../store/context";

const mocks = vi.hoisted(() => ({
  getRequestStatus: vi.fn(),
}));

vi.mock("../api/requestApi", () => ({
  RequestApi: { getRequestStatus: mocks.getRequestStatus },
}));
vi.mock("../store/ModalManager", () => ({
  default: { show: vi.fn(), hide: vi.fn() },
}));
vi.mock("../screens/BackgroundOperations", () => ({
  default: () => null,
}));
vi.mock("../Utils/statusIcons", () => ({
  getStatusIcon: () => null,
}));

import OperationsProgress from "./OperationsProgress";

type OperationsProgressProps = ComponentProps<typeof OperationsProgress>;
type Operation = OperationsProgressProps["operations"][number];

const renderProgress = (
  operations: Operation[],
  setCompletionStatus = vi.fn(),
  errorCallback?: (message: string) => void,
  dispatch?: NonNullable<OperationsProgressProps["dispatch"]>,
) => {
  const result = render(
    <AppContext.Provider
      value={{ clusterName: "c1" } as ContextType<typeof AppContext>}
    >
      <OperationsProgress
        title="Operations"
        description="Operations"
        operations={operations}
        setCompletionStatus={setCompletionStatus}
        errorCallback={errorCallback}
        dispatch={dispatch}
      />
    </AppContext.Provider>,
  );
  return { ...result, setCompletionStatus };
};

const operation = (callback: Operation["callback"]): Operation => ({
  id: 1,
  label: "Run task",
  callback,
  skippable: false,
});

describe("OperationsProgress", () => {
  beforeEach(() => {
    mocks.getRequestStatus.mockReset();
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("does not execute a queued task before its checkpoint is persisted", async () => {
    let resolveCheckpoint: (() => void) | undefined;
    const checkpoint = new Promise<void>((resolve) => {
      resolveCheckpoint = resolve;
    });
    const dispatch = vi.fn().mockReturnValueOnce(checkpoint).mockResolvedValue(undefined);
    const callback = vi.fn().mockResolvedValue({ status: 200 });

    renderProgress([operation(callback)], vi.fn(), undefined, dispatch);

    await waitFor(() => expect(dispatch).toHaveBeenCalledOnce());
    expect(dispatch.mock.calls[0][0][0].status).toBe("QUEUED");
    expect(callback).not.toHaveBeenCalled();

    await act(async () => resolveCheckpoint?.());
    await waitFor(() => expect(callback).toHaveBeenCalledOnce());
  });

  it("retries checkpoint persistence before continuing the task", async () => {
    const dispatch = vi
      .fn()
      .mockRejectedValueOnce(new Error("checkpoint unavailable"))
      .mockResolvedValue(undefined);
    const callback = vi.fn().mockResolvedValue({ status: 200 });

    renderProgress([operation(callback)], vi.fn(), undefined, dispatch);

    await screen.findByText("checkpoint unavailable");
    expect(callback).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Retry checkpoint" }));

    await waitFor(() => expect(callback).toHaveBeenCalledOnce());
    expect(dispatch).toHaveBeenCalledTimes(3);
  });

  it("does not resubmit a request when persisting its request ID fails", async () => {
    const dispatch = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error("request checkpoint unavailable"))
      .mockResolvedValue(undefined);
    const callback = vi.fn().mockResolvedValue({
      Requests: { id: 42, status: "Accepted" },
      href: "/requests/42",
    });
    mocks.getRequestStatus.mockResolvedValue({
      Requests: { request_status: ProgressStatus.COMPLETED, progress_percent: 100 },
    });

    renderProgress([operation(callback)], vi.fn(), undefined, dispatch);

    await screen.findByText("request checkpoint unavailable");
    expect(callback).toHaveBeenCalledOnce();
    expect(mocks.getRequestStatus).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Retry checkpoint" }));

    await waitFor(() =>
      expect(mocks.getRequestStatus).toHaveBeenCalledWith("c1", "42"),
    );
    expect(callback).toHaveBeenCalledOnce();
  });

  it("completes without replaying fully recovered operations", async () => {
    const callback = vi.fn();
    const { setCompletionStatus } = renderProgress([
      {
        ...operation(callback),
        label: "Recovered operation",
        status: ProgressStatus.COMPLETED,
      },
    ]);

    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
    expect(callback).not.toHaveBeenCalled();
  });

  it("resumes polling a recovered Ambari request", async () => {
    mocks.getRequestStatus.mockResolvedValue({
      Requests: {
        id: 42,
        request_status: ProgressStatus.COMPLETED,
        progress_percent: 100,
      },
    });
    const callback = vi.fn();
    const { setCompletionStatus } = renderProgress([
      {
        ...operation(callback),
        label: "Recovered request",
        requestId: 42,
        status: ProgressStatus.IN_PROGRESS,
      },
    ]);

    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
    expect(mocks.getRequestStatus).toHaveBeenCalledWith("c1", "42");
    expect(callback).not.toHaveBeenCalled();
  });

  it("makes a polling failure visible and retries the operation", async () => {
    mocks.getRequestStatus.mockRejectedValue(new Error("Request status unavailable"));
    const callback = vi.fn().mockResolvedValue(undefined);
    const { setCompletionStatus } = renderProgress([
      {
        ...operation(callback),
        label: "Retry request",
        requestId: 42,
        status: ProgressStatus.IN_PROGRESS,
      },
    ]);

    expect(await screen.findByText("Request status unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry Operation" }));

    await waitFor(() => expect(callback).toHaveBeenCalledOnce());
    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
  });

  it("cancels scheduled polling when unmounted", async () => {
    vi.useFakeTimers();
    mocks.getRequestStatus.mockResolvedValue({
      Requests: {
        id: 42,
        request_status: ProgressStatus.IN_PROGRESS,
        progress_percent: 10,
      },
    });
    const { unmount } = renderProgress([
      {
        ...operation(vi.fn()),
        label: "Running request",
        requestId: 42,
        status: ProgressStatus.IN_PROGRESS,
      },
    ]);

    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(mocks.getRequestStatus).toHaveBeenCalledOnce();

    unmount();
    await act(async () => {
      vi.advanceTimersByTime(2500);
      await Promise.resolve();
    });
    expect(mocks.getRequestStatus).toHaveBeenCalledOnce();
  });

  it("runs an explicit skip branch before advancing", async () => {
    const skipCallback = vi.fn().mockResolvedValue(undefined);
    const nextCallback = vi.fn().mockResolvedValue(undefined);
    const { setCompletionStatus } = renderProgress([
      {
        ...operation(vi.fn()),
        label: "Failed operation",
        skipCallback,
        skippable: true,
        status: ProgressStatus.FAILED,
      },
      {
        id: 2,
        label: "Next operation",
        callback: nextCallback,
        skippable: false,
      },
    ]);

    fireEvent.click(await screen.findByRole("button", { name: "Skip Operation" }));

    await waitFor(() => expect(skipCallback).toHaveBeenCalledOnce());
    await waitFor(() => expect(nextCallback).toHaveBeenCalledOnce());
    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
  });

  it("restarts the sequence from the configured operation after a late failure", async () => {
    const install = vi.fn().mockResolvedValue(undefined);
    const check = vi.fn().mockResolvedValue(undefined);
    const heartbeat = vi
      .fn()
      .mockRejectedValueOnce(new Error("Heartbeat lost"))
      .mockResolvedValueOnce(undefined);
    const { setCompletionStatus } = renderProgress([
      { ...operation(install), label: "Install client" },
      { ...operation(check), id: 2, label: "Service check" },
      {
        ...operation(heartbeat),
        id: 3,
        label: "Heartbeat check",
        retryFromOperationId: 1,
      },
    ]);

    expect(await screen.findByText("Heartbeat lost")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Retry Operation" }));

    await waitFor(() => expect(install).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(check).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(heartbeat).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
  });

  it("reports a terminal error after render and only once per failure", async () => {
    const errorCallback = vi.fn();
    renderProgress(
      [{
        ...operation(vi.fn()),
        label: "Failed request",
        status: ProgressStatus.FAILED,
        error: "Request failed",
      }],
      vi.fn(),
      errorCallback,
    );

    await waitFor(() => expect(errorCallback).toHaveBeenCalledWith("Request failed"));
    expect(errorCallback).toHaveBeenCalledOnce();
  });

  it("accepts any successful 2xx response", async () => {
    const callback = vi.fn().mockResolvedValue({ status: 204 });
    const { setCompletionStatus } = renderProgress([operation(callback)]);

    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
    expect(screen.queryByRole("button", { name: "Retry Operation" })).toBeNull();
  });
});
