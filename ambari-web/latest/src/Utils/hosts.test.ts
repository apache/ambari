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

import { describe, expect, it, vi } from "vitest";
import {
  applyCompletedDecommissionRequest,
  applyHostComponentEvent,
  applyHostEvent,
  applyHostStackVersionVisibility,
  buildHostSuggestionPredicate,
  deleteHostComponentsInOrder,
  hasCrossStackHostVersions,
  hostComponentConfigRules,
  shouldLoadCompatibleRepositoryVersions,
  shouldExcludeAddableHostComponent,
} from "./hosts";

type TestHost = {
  alertsSummary?: unknown;
  hostComponents: Array<{
    adminState: string;
    componentName: string;
    hostName: string;
    passiveState: string;
    staleConfigs: boolean;
    workStatus: string;
  }>;
  hostName: string;
  lastHeartBeatTime?: number;
  passiveState: string;
};

const hosts: TestHost[] = [
  {
    hostName: "host1",
    passiveState: "ON",
    hostComponents: [
      {
        hostName: "host1",
        componentName: "DATANODE",
        workStatus: "STARTED",
        staleConfigs: true,
        passiveState: "IMPLIED_FROM_HOST",
        adminState: "INSERVICE",
      },
    ],
  },
  {
    hostName: "host2",
    passiveState: "OFF",
    hostComponents: [
      {
        hostName: "host2",
        componentName: "DATANODE",
        workStatus: "STARTED",
        staleConfigs: true,
        passiveState: "OFF",
        adminState: "INSERVICE",
      },
    ],
  },
];

describe("Hosts realtime reconciliation", () => {
  it("matches component events by host and retains false and OFF values", () => {
    const result = applyHostComponentEvent(hosts, {
      hostComponents: [{
        hostName: "host2",
        componentName: "DATANODE",
        currentState: "INSTALLED",
        staleConfigs: false,
        maintenanceState: "OFF",
      }],
    });

    expect(result[0].hostComponents[0].workStatus).toBe("STARTED");
    expect(result[1].hostComponents[0]).toMatchObject({
      workStatus: "INSTALLED",
      staleConfigs: false,
      passiveState: "OFF",
    });
  });

  it("retains zero values and removes implied host maintenance", () => {
    const result = applyHostEvent(hosts, {
      host_name: "host1",
      last_heartbeat_time: 0,
      maintenance_state: "OFF",
      alerts_summary: {},
    });

    expect(result[0].lastHeartBeatTime).toBe(0);
    expect(result[0].alertsSummary).toEqual({});
    expect(result[0].hostComponents[0].passiveState).toBe("OFF");
  });

  it("updates every task host in a completed decommission request", () => {
    const result = applyCompletedDecommissionRequest(hosts, {
      requestStatus: "COMPLETED",
      requestContext: "Decommission DataNode",
      Tasks: [
        { hostName: "host1", role: "DATANODE" },
        { hostName: "host2", role: "DATANODE" },
      ],
    });

    expect(result.map((host) => host.hostComponents[0].adminState)).toEqual([
      "DECOMMISSIONED",
      "DECOMMISSIONED",
    ]);
  });
});

describe("host suggestion predicates", () => {
  it("escapes regex metacharacters without double encoding POST data", () => {
    expect(buildHostSuggestionPredicate("host_name", "node[1].a+b"))
      .toBe("Hosts/host_name.matches(.*node\\[1\\]\\.a\\+b.*)");
  });

  it("rejects fields that could inject a predicate", () => {
    expect(() => buildHostSuggestionPredicate("host_name)|Hosts/ip", "x"))
      .toThrow("Unsupported host suggestion field");
  });
});

describe("host deletion sequencing", () => {
  it("stops deleting components at the first failure", async () => {
    const deleteComponent = vi.fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error("component delete failed"));

    await expect(deleteHostComponentsInOrder(
      ["DATANODE", "NODEMANAGER", "HBASE_REGIONSERVER"],
      deleteComponent,
    )).rejects.toThrow("component delete failed");

    expect(deleteComponent.mock.calls).toEqual([
      ["DATANODE"],
      ["NODEMANAGER"],
    ]);
  });
});

describe("host component configuration rules", () => {
  const rules = hostComponentConfigRules({
    items: [{
      configurations: [
        {
          type: "hive-env",
          properties: { hive_database: "Existing MySQL Database" },
        },
        {
          type: "hive-interactive-env",
          properties: { enable_hive_interactive: "true" },
        },
        {
          type: "oozie-env",
          properties: { oozie_database: "New Derby Database" },
        },
      ],
    }],
  }, ["HDFS", "HIVE", "OOZIE"]);

  it("derives Oozie, Hive Interactive, and Hive database decisions from current configs", () => {
    expect(rules).toEqual({
      enableHiveInteractive: true,
      hiveDatabaseType: "Existing MySQL Database",
      isOozieServerAddable: false,
    });
  });

  it("matches classic exclusions for optional components", () => {
    expect(shouldExcludeAddableHostComponent(
      "OOZIE_SERVER", ["HDFS", "HIVE", "OOZIE"], rules,
    )).toBe(true);
    expect(shouldExcludeAddableHostComponent(
      "HIVE_SERVER_INTERACTIVE", ["HDFS", "HIVE", "OOZIE"], rules,
    )).toBe(false);
    expect(shouldExcludeAddableHostComponent(
      "OZONE_DATANODE", ["HDFS", "HIVE", "OOZIE"], rules,
    )).toBe(true);
  });
});

describe("host stack-version visibility", () => {
  const version = (
    stack: string,
    repositoryVersion: string,
    state: string,
  ): {
    HostStackVersions: { stack: string; state: string };
    is_visible?: boolean;
    repository_versions: Array<{
      RepositoryVersions: { repository_version: string };
    }>;
  } => ({
    HostStackVersions: { stack, state },
    repository_versions: [{
      RepositoryVersions: { repository_version: repositoryVersion },
    }],
  });

  it("matches classic older-version and cross-stack compatibility rules", () => {
    const host = {
      stack_versions: [
        version("HDP-3.1", "3.1.4.0", "INSTALLED"),
        version("HDP-3.1", "3.1.5.0", "CURRENT"),
        version("HDP-3.1", "3.1.6.0", "OUT_OF_SYNC"),
        version("HDP-4.0", "4.0.0.0", "OUT_OF_SYNC"),
        version("HDP-5.0", "5.0.0.0", "OUT_OF_SYNC"),
      ],
    };

    expect(hasCrossStackHostVersions([host])).toBe(true);
    applyHostStackVersionVisibility([host], ["4.0.0.0"], false);

    expect(host.stack_versions.map((item) => item.is_visible)).toEqual([
      false,
      true,
      true,
      true,
      false,
    ]);
  });

  it("shows same-stack older versions when the support flag is enabled", () => {
    const host = {
      stack_versions: [
        version("HDP-3.1", "3.1.4.0", "INSTALLED"),
        version("HDP-3.1", "3.1.5.0", "CURRENT"),
      ],
    };

    applyHostStackVersionVisibility([host], [], true);
    expect(host.stack_versions.every((item) => item.is_visible)).toBe(true);
  });

  it("waits for stack context before loading cross-stack compatibility", () => {
    const host = {
      stack_versions: [
        version("HDP-3.1", "3.1.5.0", "CURRENT"),
        version("HDP-4.0", "4.0.0.0", "OUT_OF_SYNC"),
      ],
    };

    expect(shouldLoadCompatibleRepositoryVersions([host], "", "3.1")).toBe(false);
    expect(shouldLoadCompatibleRepositoryVersions([host], "HDP", "")).toBe(false);
    expect(shouldLoadCompatibleRepositoryVersions([host], "HDP", "3.1")).toBe(true);
  });
});
