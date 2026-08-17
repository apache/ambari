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
import {
  buildConfigGroupUpdatePlan,
  moveHostsToConfigGroup,
  removeConfigGroupAndReturnHosts,
} from "./configGroupSavePlan";

const group = (id: number, hosts: string[]) => ({
  ConfigGroup: {
    id,
    hosts: hosts.map((host_name) => ({ host_name })),
  },
});

describe("config group save planning", () => {
  it("clears every changed group before writing swapped memberships", () => {
    const previous = [group(1, ["host1"]), group(2, ["host2"])];
    const updated = [group(1, ["host2"]), group(2, ["host1"])];

    const plan = buildConfigGroupUpdatePlan(previous, updated);

    expect(plan.toClear).toEqual(updated);
    expect(plan.toSet).toEqual(updated);
  });

  it("does not rewrite a group whose final membership is empty", () => {
    const previous = [group(1, ["host1"])];
    const updated = [group(1, [])];

    const plan = buildConfigGroupUpdatePlan(previous, updated);

    expect(plan.toClear).toEqual(updated);
    expect(plan.toSet).toEqual([]);
  });

  it("writes metadata-only updates without a membership clear", () => {
    const previous = [group(1, ["host1"])];
    const updated = [group(1, ["host1"])];

    const plan = buildConfigGroupUpdatePlan(previous, updated);

    expect(plan.toClear).toEqual([]);
    expect(plan.toSet).toEqual(updated);
  });
});

describe("config group membership editing", () => {
  it("moves hosts between non-default groups in one state transition", () => {
    const groups = [
      {
        ...group(1, ["host1"]),
        ConfigGroup: {
          ...group(1, ["host1"]).ConfigGroup,
          group_name: "A",
        },
      },
      {
        ...group(2, ["host2"]),
        ConfigGroup: {
          ...group(2, ["host2"]).ConfigGroup,
          group_name: "B",
        },
      },
    ];

    expect(moveHostsToConfigGroup(groups, "B", ["host1"])).toEqual([
      { ConfigGroup: { id: 1, group_name: "A", hosts: [] } },
      {
        ConfigGroup: {
          id: 2,
          group_name: "B",
          hosts: [{ host_name: "host2" }, { host_name: "host1" }],
        },
      },
    ]);
  });

  it("returns deleted group hosts to Default", () => {
    const groups = [
      {
        ...group(0, ["host2"]),
        ConfigGroup: {
          ...group(0, ["host2"]).ConfigGroup,
          group_name: "Default",
        },
      },
      {
        ...group(1, ["host1"]),
        ConfigGroup: {
          ...group(1, ["host1"]).ConfigGroup,
          group_name: "A",
        },
      },
    ];

    expect(removeConfigGroupAndReturnHosts(groups, "A")).toEqual([
      {
        ConfigGroup: {
          id: 0,
          group_name: "Default",
          hosts: [{ host_name: "host2" }, { host_name: "host1" }],
        },
      },
    ]);
  });
});
