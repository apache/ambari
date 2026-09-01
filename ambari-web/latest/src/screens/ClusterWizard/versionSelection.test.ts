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
import { compareVersions, isJdkCompatible } from "./versionSelection";

describe("cluster version selection", () => {
  it("compares numeric version segments", () => {
    expect(compareVersions("1.8.0_352", "1.8")).toBe(1);
    expect(compareVersions("17.0.2", "17.0.10")).toBe(-1);
    expect(compareVersions("17", "17.0")).toBe(0);
  });

  it("compares JDK feature versions and skips unconfigured JDKs", () => {
    expect(isJdkCompatible("17.0.8", "11", "17")).toBe(true);
    expect(isJdkCompatible("17", "11", "17")).toBe(true);
    expect(isJdkCompatible("8", "1.8", "1.8")).toBe(true);
    expect(isJdkCompatible("21", "11", "17")).toBe(false);
    expect(isJdkCompatible(undefined, "11", "17")).toBe(true);
    expect(isJdkCompatible("17", undefined, undefined)).toBe(true);
  });
});
