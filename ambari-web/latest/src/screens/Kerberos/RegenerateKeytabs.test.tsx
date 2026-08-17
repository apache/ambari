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
import { ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RequestApi } from "../../api/requestApi";
import { AppContext } from "../../store/context";
import RegenerateKeytabs from "./RegenerateKeytabs";

const pollingState = vi.hoisted(() => ({
  callback: null as null | (() => Promise<void>),
  pause: vi.fn(),
  resume: vi.fn(),
}));

vi.mock("../../hooks/usePolling", () => ({
  default: (callback: () => Promise<void>) => {
    pollingState.callback = callback;
    return {
      pausePolling: pollingState.pause,
      resumePolling: pollingState.resume,
    };
  },
}));

vi.mock("../../hooks/useKDCSessionState", () => ({
  default: () => ({
    getKDCSessionState: async (callback: () => Promise<void>) => callback(),
  }),
}));

vi.mock("../BackgroundOperations", () => ({
  default: ({
    requestId,
    onClose,
  }: {
    requestId: string | number;
    onClose: () => void;
  }) => (
    <div>
      Background request {requestId}
      <button onClick={onClose}>Close operations</button>
    </div>
  ),
}));

describe("RegenerateKeytabs", () => {
  beforeEach(() => {
    pollingState.callback = null;
    pollingState.pause.mockReset();
    pollingState.resume.mockReset();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("stops at terminal state and starts automatic restart only once", async () => {
    vi.spyOn(RequestApi, "regenerateKeytabs").mockResolvedValue({
      Requests: { id: 10 },
    } as any);
    vi.spyOn(RequestApi, "getRequestStatus").mockResolvedValue({
      Requests: { request_status: "COMPLETED" },
    } as any);
    const restart = vi.spyOn(RequestApi, "postRequest").mockResolvedValue({
      Requests: { id: 20 },
    } as any);
    const onFinished = vi.fn();

    render(
      <AppContext.Provider
        value={
          { clusterName: "c1" } as unknown as ComponentProps<
            typeof AppContext.Provider
          >["value"]
        }
      >
        <RegenerateKeytabs
          missingHostCheck={false}
          restartComponentsCheck
          onFinished={onFinished}
        />
      </AppContext.Provider>,
    );

    expect(await screen.findByText("Background request 10")).toBeTruthy();
    await act(async () => {
      await pollingState.callback?.();
    });

    await waitFor(() => expect(restart).toHaveBeenCalledTimes(1));
    expect(await screen.findByText("Background request 20")).toBeTruthy();
    expect(pollingState.pause).toHaveBeenCalled();

    await act(async () => {
      await pollingState.callback?.();
    });
    expect(restart).toHaveBeenCalledTimes(1);

    screen.getByRole("button", { name: "Close operations" }).click();
    expect(onFinished).toHaveBeenCalledTimes(1);
  });

  it("releases the parent trigger after submission failure", async () => {
    vi.spyOn(RequestApi, "regenerateKeytabs").mockRejectedValue(
      new Error("submission failed"),
    );
    const onFinished = vi.fn();

    render(
      <AppContext.Provider
        value={
          { clusterName: "c1" } as unknown as ComponentProps<
            typeof AppContext.Provider
          >["value"]
        }
      >
        <RegenerateKeytabs
          missingHostCheck={false}
          restartComponentsCheck={false}
          onFinished={onFinished}
        />
      </AppContext.Provider>,
    );

    await waitFor(() => expect(onFinished).toHaveBeenCalledTimes(1));
  });
});
