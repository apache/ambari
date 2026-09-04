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
import { resolveServiceNavigation } from "./serviceNavigation";

const options = {
  availableTabs: {
    HDFS: ["summary", "configs"],
    YARN: ["summary", "configs"],
  },
  canViewConfigs: true,
  canViewMetrics: true,
  installedServices: ["HDFS", "YARN"],
};

describe("service navigation", () => {
  it("returns an invalid service route to the first installed service", () => {
    expect(
      resolveServiceNavigation({
        ...options,
        requestedService: "REMOVED",
        requestedTab: "configs",
      })
    ).toEqual({
      redirectPath: "/main/services/HDFS/summary",
      selectedService: "HDFS",
      selectedTab: "summary",
    });
  });

  it("keeps a valid service and tab selection", () => {
    expect(
      resolveServiceNavigation({
        ...options,
        requestedService: "YARN",
        requestedTab: "configs",
      })
    ).toEqual({ selectedService: "YARN", selectedTab: "configs" });
  });

  it("converges invalid and unauthorized tabs to Summary", () => {
    expect(
      resolveServiceNavigation({
        ...options,
        requestedService: "HDFS",
        requestedTab: "unknown",
      }).redirectPath
    ).toBe("/main/services/HDFS/summary");
    expect(
      resolveServiceNavigation({
        ...options,
        canViewConfigs: false,
        requestedService: "HDFS",
        requestedTab: "configs",
      }).redirectPath
    ).toBe("/main/services/HDFS/summary");
    expect(
      resolveServiceNavigation({
        ...options,
        availableTabs: { HDFS: ["summary", "configs", "metrics"] },
        canViewMetrics: false,
        requestedService: "HDFS",
        requestedTab: "metrics",
      }).redirectPath
    ).toBe("/main/services/HDFS/summary");
  });
});
