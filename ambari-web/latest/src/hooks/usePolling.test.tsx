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

import { act, cleanup, render } from "@testing-library/react";
import { useEffect } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import usePolling from "./usePolling";

function PollingHarness({ callback }: { callback: () => Promise<void> }) {
  usePolling(callback, 1000);
  return null;
}

type PollingControls = ReturnType<typeof usePolling>;

function ControlledPollingHarness({
  callback,
  onControls,
}: {
  callback: () => Promise<void>;
  onControls: (controls: PollingControls) => void;
}) {
  const controls = usePolling(callback, 1000);
  useEffect(() => onControls(controls), [controls, onControls]);
  return null;
}

describe("usePolling", () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("does not schedule another poll when an in-flight request finishes after unmount", async () => {
    vi.useFakeTimers();
    let finishRequest: () => void = () => undefined;
    const callback = vi.fn(() => new Promise<void>((resolve) => {
      finishRequest = resolve;
    }));
    const view = render(<PollingHarness callback={callback} />);
    expect(callback).toHaveBeenCalledTimes(1);

    view.unmount();
    await act(async () => finishRequest());
    await act(async () => vi.advanceTimersByTimeAsync(5000));

    expect(callback).toHaveBeenCalledTimes(1);
  });

  it("restarts polling after an explicit stop", async () => {
    vi.useFakeTimers();
    const callback = vi.fn(async () => undefined);
    let controls: PollingControls | undefined;
    render(
      <ControlledPollingHarness
        callback={callback}
        onControls={(value) => { controls = value; }}
      />,
    );
    await act(async () => Promise.resolve());
    expect(callback).toHaveBeenCalledTimes(1);

    act(() => controls?.stopPolling());
    await act(async () => vi.advanceTimersByTimeAsync(5000));
    expect(callback).toHaveBeenCalledTimes(1);

    act(() => controls?.resumePolling());
    await act(async () => Promise.resolve());
    expect(callback).toHaveBeenCalledTimes(2);
  });
});
