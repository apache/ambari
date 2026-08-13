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
import { hasVersionConflict, selectLandingPath } from "./authPolicy";

describe("application landing policy", () => {
  it.each([
    [{ clusterInstalled: true, clusterName: "c1", viewOnly: false }, "/main/dashboard/metrics"],
    [{
      clusterInstalled: true,
      clusterName: "c1",
      preferredPath: "/main/hosts",
      viewOnly: false,
    }, "/main/hosts"],
    [{ clusterInstalled: false, clusterName: "c1", viewOnly: false }, "/installer/step0"],
    [{ clusterInstalled: false, clusterName: "", viewOnly: false }, "/adminView"],
    [{ clusterInstalled: true, clusterName: "c1", viewOnly: true }, "/main/view"],
  ])("selects the expected landing path", (input, expected) => {
    expect(selectLandingPath(input)).toBe(expected);
  });

  it.each([
    ["", "2.0.0", false],
    ["2.0.0", "", false],
    ["2.0.0", "2.0.0", false],
    ["2.0.0", "2.0.1", true],
  ])("compares packaged and server versions", (client, server, expected) => {
    expect(hasVersionConflict(client, server)).toBe(expected);
  });
});
