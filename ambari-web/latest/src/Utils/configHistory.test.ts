/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
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
import {
  buildConfigHistoryParameters,
  canOpenConfigHistoryItem,
  configHistoryNavigationState,
  configHistorySort,
  formatConfigHistoryDate,
  resolveConfigHistorySelection,
} from "./configHistory";

describe("config history query contract", () => {
  it("uses the classic secondary version sort for service and group", () => {
    expect(configHistorySort("service_name", "asc"))
      .toBe("service_name.asc,service_config_version.desc");
    expect(configHistorySort("createtime", "desc")).toBe("createtime.desc");
  });

  it("builds paging, match, exact, and relative-time predicates", () => {
    const result = buildConfigHistoryParameters({
      currentPage: 3,
      pageSize: 10,
      sortColumn: "createtime",
      sortOrder: "desc",
      filters: [
        { field: { label: "Author", value: "user" }, value: { label: "a.b&c", value: "a.b&c" } },
        { field: { label: "Service", value: "service_name" }, value: { label: "HDFS", value: "HDFS" } },
        { field: { label: "Created", value: "createtime" }, value: { label: "1 hour", value: "1h" } },
      ],
    }, 10_000_000);

    expect(result).toContain("page_size=10&from=20&sortBy=createtime.desc");
    expect(result).toContain("user.matches(.*a%5C.b%26c.*)");
    expect(result).toContain("service_name=HDFS");
    expect(result).toContain("createtime>6400000");
  });

  it("carries the selected version and group to Service Configs", () => {
    expect(configHistoryNavigationState({
      serviceName: "HDFS",
      serviceConfigVersion: 12,
      groupId: 4,
      groupName: "workers",
    })).toEqual({
      serviceName: "HDFS",
      serviceConfigVersion: "12",
      configGroupId: 4,
      configGroup: "workers",
    });
  });

  it("loads the current default version with a selected non-default group version", () => {
    expect(resolveConfigHistorySelection("18", {
      serviceName: "HDFS",
      serviceConfigVersion: "12",
      configGroupId: 4,
      configGroup: "workers",
    })).toEqual({
      configGroup: "workers",
      selectedVersion: "12",
      versionsToLoad: "18,12",
    });
    expect(resolveConfigHistorySelection("18", null)).toEqual({
      configGroup: "Default",
      selectedVersion: "18",
      versionsToLoad: null,
    });
  });

  it("disables navigation for deleted groups and uninstalled services", () => {
    expect(canOpenConfigHistoryItem(
      { serviceName: "HDFS", groupName: "Default" },
      ["HDFS"],
    )).toBe(true);
    expect(canOpenConfigHistoryItem(
      { serviceName: "HDFS", groupName: "Deleted" },
      ["HDFS"],
    )).toBe(false);
    expect(canOpenConfigHistoryItem(
      { serviceName: "KERBEROS", groupName: "Default" },
      ["HDFS"],
    )).toBe(false);
  });

  it("formats create time in the persisted user timezone", () => {
    expect(formatConfigHistoryDate(Date.UTC(2025, 0, 1, 0, 0), "Asia/Shanghai"))
      .toContain("08:00");
  });
});
