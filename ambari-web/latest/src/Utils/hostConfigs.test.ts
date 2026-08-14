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
  buildConfigGroupMembershipUpdates,
  buildHostConfigGroupState,
} from "./hostConfigs";

function group(
  id: number,
  serviceName: string,
  groupName: string,
  hosts: string[],
) {
  return {
    ConfigGroup: {
      id,
      group_name: groupName,
      description: `${groupName} description`,
      tag: serviceName,
      service_name: serviceName,
      hosts: hosts.map((host_name) => ({ host_name })),
      desired_configs: [{ type: `${serviceName.toLowerCase()}-site`, tag: "version1" }],
    },
  };
}

describe("host config groups", () => {
  const hdfsBlue = group(1, "HDFS", "Blue", ["host1", "host2"]);
  const hdfsGreen = group(2, "HDFS", "Green", ["host3"]);
  const yarnBlue = group(3, "YARN", "Blue", ["host2"]);

  it("tracks assignments independently for every service", () => {
    expect(buildHostConfigGroupState(
      [hdfsBlue, hdfsGreen, yarnBlue],
      ["HDFS", "YARN", "HIVE"],
      "host1",
    )).toEqual({
      assignedGroupByService: {
        HDFS: "Blue",
        YARN: "Default",
        HIVE: "Default",
      },
      groupsByService: {
        HDFS: [hdfsBlue, hdfsGreen],
        YARN: [yarnBlue],
        HIVE: [],
      },
    });
  });

  it("adds a host when moving from Default to a non-default group", () => {
    const updates = buildConfigGroupMembershipUpdates(
      [hdfsBlue], "HDFS", "Default", "Blue", "host3",
    );

    expect(updates).toHaveLength(1);
    expect(updates[0].groupId).toBe("1");
    expect(updates[0].payload).toEqual([{
      ConfigGroup: {
        id: 1,
        group_name: "Blue",
        description: "Blue description",
        tag: "HDFS",
        service_name: "HDFS",
        hosts: [
          { host_name: "host1" },
          { host_name: "host2" },
          { host_name: "host3" },
        ],
        desired_configs: [{ type: "hdfs-site", tag: "version1" }],
      },
    }]);
  });

  it("removes a host when moving from a non-default group to Default", () => {
    const updates = buildConfigGroupMembershipUpdates(
      [hdfsBlue], "HDFS", "Blue", "Default", "host1",
    );

    expect(updates).toHaveLength(1);
    expect(updates[0].payload[0].ConfigGroup.hosts).toEqual([
      { host_name: "host2" },
    ]);
  });

  it("updates both memberships when moving between non-default groups", () => {
    const updates = buildConfigGroupMembershipUpdates(
      [hdfsBlue, hdfsGreen], "HDFS", "Blue", "Green", "host1",
    );

    expect(updates.map((update) => update.groupId)).toEqual(["1", "2"]);
    expect(updates[0].payload[0].ConfigGroup.hosts).toEqual([
      { host_name: "host2" },
    ]);
    expect(updates[1].payload[0].ConfigGroup.hosts).toEqual([
      { host_name: "host3" },
      { host_name: "host1" },
    ]);
  });
});
