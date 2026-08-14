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
  addHostRegistrationTimeoutSecs,
  buildAddHostComponentAssignments,
  buildAddHostConfigGroupUpdates,
  buildAddHostConfigGroups,
  buildBootstrapPayload,
  prepareHostInput,
  selectedAddHostServices,
} from "./hostWizard";

describe("Add Host install options", () => {
  it("normalizes, expands, de-duplicates, and filters installed hosts", () => {
    expect(prepareHostInput(
      "Node[01-03].EXAMPLE.COM node02.example.com OLD.EXAMPLE.COM",
      ["old.example.com"],
    )).toEqual({
      alreadyInstalled: ["old.example.com"],
      hadPattern: true,
      hosts: [
        "node01.example.com",
        "node02.example.com",
        "node03.example.com",
      ],
    });
  });

  it("forces root unless the customize-agent-user flag is enabled", () => {
    expect(buildBootstrapPayload({
      customizeAgentUserAccount: false,
      hosts: ["host1"],
      agentUserAccount: "ambari",
    }).userRunAs).toBe("root");
    expect(buildBootstrapPayload({
      customizeAgentUserAccount: true,
      hosts: ["host1"],
      agentUserAccount: "ambari",
    }).userRunAs).toBe("ambari");
  });

  it("allows automatic bootstrap longer to register than manual Agent mode", () => {
    expect(addHostRegistrationTimeoutSecs(true)).toBe(120);
    expect(addHostRegistrationTimeoutSecs(false)).toBe(15);
  });

  it("loads config groups only for services with selected components", () => {
    expect(selectedAddHostServices(
      [{ checkboxes: [
        { checked: true, label: "DATANODE" },
        { checked: false, label: "NODEMANAGER" },
        { checked: true, label: "CLIENT" },
      ] }],
      [
        { component_name: "DATANODE", service_name: "HDFS" },
        { component_name: "NODEMANAGER", service_name: "YARN" },
      ],
    )).toEqual(["HDFS"]);
  });

  it("preserves a restored non-default config group", () => {
    const result = buildAddHostConfigGroups(
      ["HDFS"],
      { items: [{ ConfigGroup: { tag: "HDFS", group_name: "workers" } }] },
      "c1",
      [{
        serviceName: "HDFS",
        configGroups: [{ group_name: "workers", isSelected: true }],
      }],
    );
    expect(result[0].configGroups.find((group: any) => group.isSelected).group_name)
      .toBe("workers");
  });

  it("expands the generic CLIENT selection to concrete client components", () => {
    const metadata = [
      { component_name: "HDFS_CLIENT", service_name: "HDFS", is_client: true },
      { component_name: "YARN_CLIENT", service_name: "YARN", component_category: "CLIENT" },
      { component_name: "DATANODE", service_name: "HDFS", is_client: false },
    ];
    const assignments = [{
      hostname: "host1",
      checkboxes: [{ checked: true, label: "CLIENT" }],
    }];

    expect(selectedAddHostServices(assignments, metadata)).toEqual(["HDFS", "YARN"]);
    expect(buildAddHostComponentAssignments(assignments, metadata)).toEqual({
      HDFS_CLIENT: ["host1"],
      YARN_CLIENT: ["host1"],
    });
  });

  it("builds a complete classic-compatible config-group membership update", () => {
    const assignments = [
      { hostname: "host1", checkboxes: [{ checked: true, label: "DATANODE" }] },
      { hostname: "host2", checkboxes: [{ checked: true, label: "DATANODE" }] },
    ];
    const metadata = [
      { component_name: "DATANODE", service_name: "HDFS", is_client: false },
    ];
    const configurations = [{
      serviceName: "HDFS",
      configGroups: [{
        id: 7,
        group_name: "workers",
        tag: "HDFS",
        service_name: "HDFS",
        description: "Worker hosts",
        desired_configs: [{ type: "hdfs-site", tag: "version1" }],
        hosts: [{ host_name: "existing" }],
        isSelected: true,
      }],
    }];

    expect(buildAddHostConfigGroupUpdates(
      configurations,
      assignments,
      metadata,
    )).toEqual([{
      groupId: "7",
      serviceName: "HDFS",
      payload: [{
        ConfigGroup: {
          id: 7,
          group_name: "workers",
          tag: "HDFS",
          service_name: "HDFS",
          description: "Worker hosts",
          desired_configs: [{ type: "hdfs-site", tag: "version1" }],
          hosts: [
            { host_name: "existing" },
            { host_name: "host1" },
            { host_name: "host2" },
          ],
        },
      }],
    }]);
  });
});
