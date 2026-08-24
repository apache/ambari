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

import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import DigitalClock from "./DigitalClock";

describe("DigitalClock", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(Date.UTC(2024, 0, 1, 0, 0, 0));
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
  });

  it("tracks Ambari Server time instead of the client clock", () => {
    render(
      <DigitalClock
        serverClock={Date.UTC(2024, 0, 1, 1, 0, 0)}
        timeZone="UTC"
      />,
    );

    expect(screen.getByTestId("server-clock").textContent).toBe("01:00:00");
    act(() => vi.advanceTimersByTime(2000));
    expect(screen.getByTestId("server-clock").textContent).toBe("01:00:02");
  });
});
