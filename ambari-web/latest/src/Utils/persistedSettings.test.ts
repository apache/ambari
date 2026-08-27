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
import { parsePersistedValue, persistedPayload } from "./persistedSettings";

describe("persisted settings", () => {
  it("round trips booleans, strings, and objects as JSON values", () => {
    const payload = persistedPayload({ enabled: false, timezone: "UTC", wizard: {} });
    expect(payload).toEqual({
      enabled: "false",
      timezone: '"UTC"',
      wizard: "{}",
    });
    expect(parsePersistedValue(payload.enabled, true)).toBe(false);
    expect(parsePersistedValue(payload.timezone, "Browser")).toBe("UTC");
    expect(parsePersistedValue(payload.wizard, { stale: true })).toEqual({});
  });

  it("uses the fallback for empty or invalid persisted data", () => {
    expect(parsePersistedValue(undefined, true)).toBe(true);
    expect(parsePersistedValue(null, true)).toBe(true);
    expect(parsePersistedValue("", "Browser")).toBe("Browser");
    expect(parsePersistedValue("null", { userName: "" })).toEqual({ userName: "" });
    expect(parsePersistedValue("not-json", "Browser")).toBe("Browser");
  });
});
