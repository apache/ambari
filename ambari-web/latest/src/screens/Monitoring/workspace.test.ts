/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import { localDateTime, readRefreshPreference, refreshPreferenceKey, saveRefreshPreference } from "./workspace";

describe("monitoring workspace preferences", () => {
  afterEach(() => { vi.restoreAllMocks(); localStorage.clear(); });
  it("retains second precision so a new sample is not excluded by minute rounding", () => {
    const date = new Date(2026, 8, 29, 11, 57, 49);
    expect(new Date(localDateTime(date)).getTime()).toBe(date.getTime());
  });
  it("defaults to live refresh and retains an explicit pause for the current principal and cluster", () => {
    const alice = refreshPreferenceKey("c1", "alice");
    expect(readRefreshPreference(alice)).toBe(30);
    saveRefreshPreference(alice, 0);
    expect(readRefreshPreference(alice)).toBe(0);
    expect(readRefreshPreference(refreshPreferenceKey("c2", "alice"))).toBe(30);
    expect(readRefreshPreference(refreshPreferenceKey("c1", "bob"))).toBe(30);
    localStorage.setItem(alice, "broken");
    expect(readRefreshPreference(alice)).toBe(30);
  });
  it("keeps the page usable when browser storage is unavailable", () => {
    vi.spyOn(localStorage, "getItem").mockImplementation(() => { throw new Error("blocked"); });
    vi.spyOn(localStorage, "setItem").mockImplementation(() => { throw new Error("blocked"); });
    expect(readRefreshPreference("key")).toBe(30);
    expect(() => saveRefreshPreference("key", 60)).not.toThrow();
  });
});
