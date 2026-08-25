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

import { describe, expect, it } from "vitest";
import { formatServerClock, normalizeServerClock } from "./serverClock";

describe("Ambari Server clock", () => {
  it("normalizes both second and millisecond server timestamps", () => {
    expect(normalizeServerClock("1710000000")).toBe(1_710_000_000_000);
    expect(normalizeServerClock(1_710_000_000_123)).toBe(1_710_000_000_123);
    expect(normalizeServerClock("invalid")).toBeNull();
  });

  it("formats the clock in the selected user timezone", () => {
    expect(formatServerClock(Date.UTC(2024, 0, 1, 0, 0, 5), "UTC"))
      .toBe("00:00:05");
    expect(formatServerClock(Date.UTC(2024, 0, 1, 0, 0, 5), "Asia/Shanghai"))
      .toBe("08:00:05");
  });
});
