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

import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AppContext } from "../../store/context";

const mocks = vi.hoisted(() => ({
  getTask: vi.fn(),
}));

vi.mock("../../api/clusterApi", () => ({
  default: { getClusterRequestTaskLogs: mocks.getTask },
}));

import TaskLogs from "./TaskLogs";

describe("background task logs", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getTask.mockResolvedValue({
      Tasks: { id: 3, status: "IN_PROGRESS", stderr: "", stdout: "initial" },
    });
  });

  afterEach(() => cleanup());

  it("isolates malformed messages and unsubscribes after a terminal update", async () => {
    let onMessage: (message: { body: string }) => void = () => undefined;
    const unsubscribe = vi.fn();
    const subscribe = vi.fn((_destination, handler) => {
      onMessage = handler;
      return { unsubscribe };
    });
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);

    render(
      <AppContext.Provider value={{
        client: { subscribe },
        clusterName: "c1",
        isSocketConnected: true,
      } as unknown as React.ContextType<typeof AppContext>}>
        <TaskLogs requestId={7} task={{ id: 3, status: "IN_PROGRESS" }} />
      </AppContext.Provider>,
    );

    expect(await screen.findByText("initial")).toBeTruthy();
    expect(subscribe).toHaveBeenCalledWith("/events/tasks/3", expect.any(Function));

    act(() => onMessage({ body: "not-json" }));
    expect(consoleError).toHaveBeenCalledWith(
      "Ambari ignored a malformed task update for task 3.",
    );

    act(() => onMessage({
      body: JSON.stringify({
        id: 3,
        status: "SKIPPED_FAILED",
        errorLog: "/var/log/task.err",
        outLog: "/var/log/task.out",
        stderr: "failed",
        stdout: "finished",
      }),
    }));

    expect(await screen.findByText("failed")).toBeTruthy();
    await waitFor(() => expect(unsubscribe).toHaveBeenCalled());
    expect(subscribe).toHaveBeenCalledTimes(1);
    consoleError.mockRestore();
  });
});
