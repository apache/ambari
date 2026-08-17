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

import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ComponentProps } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RequestApi } from "../api/requestApi";
import { ProgressStatus } from "../constants";
import { AppContext } from "../store/context";
import OperationsProgress from "./OperationsProgress";

const renderProgress = (
  operations: ComponentProps<typeof OperationsProgress>["operations"],
  setCompletionStatus = vi.fn(),
  errorCallback?: (message: string) => void,
) => {
  const result = render(
    <AppContext.Provider
      value={
        { clusterName: "c1" } as unknown as ComponentProps<
          typeof AppContext.Provider
        >["value"]
      }
    >
      <OperationsProgress
        title="Operations"
        description="Operations"
        operations={operations}
        setCompletionStatus={setCompletionStatus}
        errorCallback={errorCallback}
      />
    </AppContext.Provider>,
  );

  return { ...result, setCompletionStatus };
};

describe("OperationsProgress", () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("completes without replaying fully recovered operations", async () => {
    const callback = vi.fn();
    const { setCompletionStatus } = renderProgress([
      {
        id: 1,
        label: "Recovered operation",
        callback,
        skippable: false,
        status: ProgressStatus.COMPLETED,
      },
    ]);

    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
    expect(callback).not.toHaveBeenCalled();
  });

  it("resumes polling a recovered Ambari request", async () => {
    const callback = vi.fn();
    vi.spyOn(RequestApi, "getRequestStatus").mockResolvedValue({
      Requests: {
        id: 42,
        request_status: ProgressStatus.COMPLETED,
        progress_percent: 100,
      },
    } as any);
    const { setCompletionStatus } = renderProgress([
      {
        id: 1,
        label: "Recovered request",
        callback,
        skippable: false,
        requestId: 42,
        status: ProgressStatus.IN_PROGRESS,
      },
    ]);

    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
    expect(RequestApi.getRequestStatus).toHaveBeenCalledWith("c1", "42");
    expect(callback).not.toHaveBeenCalled();
  });

  it("makes a polling failure visible and retries the operation", async () => {
    const callback = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(RequestApi, "getRequestStatus").mockRejectedValue(
      new Error("Request status unavailable"),
    );
    const { setCompletionStatus } = renderProgress([
      {
        id: 1,
        label: "Retry request",
        callback,
        skippable: false,
        requestId: 42,
        status: ProgressStatus.IN_PROGRESS,
      },
    ]);

    expect(await screen.findByText("Request status unavailable")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: /Retry Operation/i }));

    await waitFor(() => expect(callback).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
  });

  it("cancels scheduled polling when unmounted", async () => {
    vi.useFakeTimers();
    vi.spyOn(RequestApi, "getRequestStatus").mockResolvedValue({
      Requests: {
        id: 42,
        request_status: ProgressStatus.IN_PROGRESS,
        progress_percent: 10,
      },
    } as any);
    const { unmount } = renderProgress([
      {
        id: 1,
        label: "Running request",
        callback: vi.fn(),
        skippable: false,
        requestId: 42,
        status: ProgressStatus.IN_PROGRESS,
      },
    ]);

    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });
    expect(RequestApi.getRequestStatus).toHaveBeenCalledTimes(1);

    unmount();
    await act(async () => {
      vi.advanceTimersByTime(2500);
      await Promise.resolve();
    });
    expect(RequestApi.getRequestStatus).toHaveBeenCalledTimes(1);
  });

  it("runs an explicit skip branch before advancing", async () => {
    const skipCallback = vi.fn().mockResolvedValue(undefined);
    const nextCallback = vi.fn().mockResolvedValue(undefined);
    const { setCompletionStatus } = renderProgress([
      {
        id: 1,
        label: "Failed operation",
        callback: vi.fn(),
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

    fireEvent.click(
      await screen.findByRole("button", { name: "Skip Operation" }),
    );

    await waitFor(() => expect(skipCallback).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(nextCallback).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
  });

  it("restarts the sequence from the configured operation after a late failure", async () => {
    const install = vi.fn().mockResolvedValue(undefined);
    const check = vi.fn().mockResolvedValue(undefined);
    const heartbeat = vi.fn()
      .mockRejectedValueOnce(new Error("Heartbeat lost"))
      .mockResolvedValueOnce(undefined);
    const { setCompletionStatus } = renderProgress([
      {
        id: 1,
        label: "Install client",
        callback: install,
        skippable: false,
      },
      {
        id: 2,
        label: "Service check",
        callback: check,
        skippable: false,
      },
      {
        id: 3,
        label: "Heartbeat check",
        callback: heartbeat,
        skippable: false,
        retryFromOperationId: 1,
      },
    ]);

    expect(await screen.findByText(/Heartbeat lost/)).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: /Retry Operation/i }));

    await waitFor(() => expect(install).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(check).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(heartbeat).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(setCompletionStatus).toHaveBeenCalledWith(true));
  });

  it("reports a terminal error after render and only once per failure", async () => {
    const errorCallback = vi.fn();
    renderProgress([
      {
        id: 1,
        label: "Failed request",
        callback: vi.fn(),
        skippable: false,
        status: ProgressStatus.FAILED,
        error: "Request failed",
      },
    ], vi.fn(), errorCallback);

    await waitFor(() => expect(errorCallback).toHaveBeenCalledWith("Request failed"));
    expect(errorCallback).toHaveBeenCalledTimes(1);
  });
});
