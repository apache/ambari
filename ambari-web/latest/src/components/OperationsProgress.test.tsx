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
import { afterEach, describe, expect, it, vi } from "vitest";
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

const renderProgress = (
  callback: OperationsProgressProps["operations"][number]["callback"],
  dispatch: NonNullable<OperationsProgressProps["dispatch"]>,
) =>
  render(
    <AppContext.Provider value={{ clusterName: "c1" } as ContextType<typeof AppContext>}>
      <OperationsProgress
        title=""
        description=""
        setCompletionStatus={vi.fn()}
        operations={[
          {
            id: 1,
            label: "Run task",
            callback,
            skippable: false,
          },
        ]}
        dispatch={dispatch}
      />
    </AppContext.Provider>,
  );

describe("OperationsProgress persistence checkpoints", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("does not execute a queued task before its checkpoint is persisted", async () => {
    let resolveCheckpoint: (() => void) | undefined;
    const checkpoint = new Promise<void>((resolve) => {
      resolveCheckpoint = resolve;
    });
    const dispatch = vi.fn().mockReturnValueOnce(checkpoint).mockResolvedValue(undefined);
    const callback = vi.fn().mockResolvedValue({ status: 200 });

    renderProgress(callback, dispatch);

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

    renderProgress(callback, dispatch);

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
      Requests: { request_status: "COMPLETED", progress_percent: 100 },
    });

    renderProgress(callback, dispatch);

    await screen.findByText("request checkpoint unavailable");
    expect(callback).toHaveBeenCalledOnce();
    expect(mocks.getRequestStatus).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Retry checkpoint" }));

    await waitFor(() =>
      expect(mocks.getRequestStatus).toHaveBeenCalledWith("c1", "42"),
    );
    expect(callback).toHaveBeenCalledOnce();
  });

  it("shows the original error for a terminal failed operation", async () => {
    const dispatch = vi.fn().mockResolvedValue(undefined);
    const callback = vi.fn().mockRejectedValue(new Error("server rejected task"));

    renderProgress(callback, dispatch);

    expect(await screen.findByText("server rejected task")).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Retry Operation" }),
    ).toBeTruthy();
  });
});
