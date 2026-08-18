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
  buildDesiredConfigQuery,
  buildJournalNodeSharedEditsConfigs,
  evaluateCheckpointSet,
  evaluateJournalNodeFormatSet,
  evaluateNameNodeCheckpoint,
  getHdfsNamespaces,
  getHdfsUser,
  getJournalNodeDirectories,
  getJournalNodeChangeSet,
  getRangerReconfigureSiteGroups,
  isValidNameNodeHaAssignment,
  mergeReviewedConfigs,
  mergeSavedOperations,
  updateReviewedConfigValue,
} from "./haWorkflowUtils";

const checkpoint = (host: string, delta = 1) => ({
  HostRoles: { host_name: host, desired_state: "STARTED" },
  metrics: {
    dfs: {
      namenode: {
        Safemode: "Safe mode is ON",
        JournalTransactionInfo: JSON.stringify({
          LastAppliedOrWrittenTxId: 10,
          MostRecentCheckpointTxId: 10 - delta,
        }),
      },
    },
  },
});

describe("NameNode HA workflow utilities", () => {
  type TestOperation = {
    id: string | number;
    label: string;
    callback: () => Promise<unknown>;
    status?: string;
  };

  it("reads the configured HDFS user and normalizes namespace model shapes", () => {
    expect(
      getHdfsUser({
        items: [
          {
            configurations: [
              { type: "hadoop-env", properties: { hdfs_user: "hdfs-svc" } },
            ],
          },
        ],
      }),
    ).toBe("hdfs-svc");
    expect(
      getHdfsNamespaces({
        namespaces: [
          { nameSpace: "ns1", hostNames: ["nn1", "nn2", "nn1"] },
        ],
      }),
    ).toEqual([{ name: "ns1", hosts: ["nn1", "nn2"] }]);
    expect(
      getHdfsNamespaces({
        federationNamespaces: [{ name: "ns2", hosts: ["nn3", "nn4"] }],
      }),
    ).toEqual([{ name: "ns2", hosts: ["nn3", "nn4"] }]);
  });

  it("requires every JournalNode directory property and deduplicates paths", () => {
    expect(
      getJournalNodeDirectories(
        {
          federationNamespaces: [
            { name: "ns1", hosts: ["nn1", "nn2"] },
            { name: "ns2", hosts: ["nn3", "nn4"] },
          ],
        },
        {
          "dfs.journalnode.edits.dir.ns1": "/hadoop/hdfs/journal",
          "dfs.journalnode.edits.dir.ns2": "/hadoop/hdfs/journal",
        },
      ),
    ).toEqual({
      directories: ["/hadoop/hdfs/journal"],
      missingProperties: [],
    });
    expect(
      getJournalNodeDirectories(
        {
          federationNamespaces: [
            { name: "ns1", hosts: ["nn1", "nn2"] },
            { name: "ns2", hosts: ["nn3", "nn4"] },
          ],
        },
        { "dfs.journalnode.edits.dir.ns1": "/jn/ns1" },
      ).missingProperties,
    ).toEqual(["dfs.journalnode.edits.dir.ns2"]);
  });

  it("rejects malformed checkpoint data without throwing", () => {
    const result = evaluateNameNodeCheckpoint({
      HostRoles: { desired_state: "STARTED" },
      metrics: {
        dfs: {
          namenode: {
            Safemode: "Safe mode is ON",
            JournalTransactionInfo: "not-json",
          },
        },
      },
    });

    expect(result.ready).toBe(false);
    expect(result.error).toContain("malformed");
  });

  it("requires the complete JournalNode host set", () => {
    const formatted = (value: boolean) => ({
      metrics: {
        dfs: {
          journalnode: {
            journalsStatus: JSON.stringify({
              nameservice1: { Formatted: String(value) },
            }),
          },
        },
      },
    });

    expect(
      evaluateJournalNodeFormatSet(
        ["jn1", "jn2", "jn3", "jn4"],
        { jn1: formatted(true), jn2: formatted(true), jn3: formatted(true) },
        "nameservice1",
      ),
    ).toMatchObject({ ready: false, missingHosts: ["jn4"] });

    expect(
      evaluateJournalNodeFormatSet(
        ["jn1", "jn2", "jn3", "jn4"],
        {
          jn1: formatted(true),
          jn2: formatted(true),
          jn3: formatted(true),
          jn4: formatted(true),
        },
        "nameservice1",
      ).ready,
    ).toBe(true);
  });

  it("rejects missing and duplicate Federation checkpoint responses", () => {
    expect(
      evaluateCheckpointSet(["nn1", "nn2"], [checkpoint("nn1")]).ready,
    ).toBe(false);
    expect(
      evaluateCheckpointSet(
        ["nn1", "nn2"],
        [checkpoint("nn1"), checkpoint("nn1")],
      ).ready,
    ).toBe(false);
    expect(
      evaluateCheckpointSet(
        ["nn1", "nn2"],
        [checkpoint("nn2"), checkpoint("nn1")],
      ).ready,
    ).toBe(true);
  });

  it("builds one Federation shared-edits property per nameservice", () => {
    const configs = buildJournalNodeSharedEditsConfigs(
      ["ns1", "ns2"],
      ["jn3", "jn1", "jn2"],
      true,
    );

    expect(configs.map((item) => item.name)).toEqual([
      "dfs.namenode.shared.edits.dir.ns1",
      "dfs.namenode.shared.edits.dir.ns2",
    ]);
    expect(configs[0].value).toBe(
      "qjournal://jn1:8485;jn2:8485;jn3:8485/ns1",
    );
  });

  it("computes add, delete, delete-only, and no-op JournalNode modes", () => {
    expect(
      getJournalNodeChangeSet(
        [
          { component: "JOURNALNODE", hostName: "jn1" },
          { component: "JOURNALNODE", hostName: "jn3" },
        ],
        ["jn1", "jn2"],
      ),
    ).toMatchObject({
      addedHosts: ["jn3"],
      deletedHosts: ["jn2"],
      isDeleteOnly: false,
      isNoOp: false,
    });
    expect(
      getJournalNodeChangeSet(
        [{ component: "JOURNALNODE", hostName: "jn1" }],
        ["jn1", "jn2"],
      ).isDeleteOnly,
    ).toBe(true);
  });

  it("requires one additional NameNode and at least three unique JournalNodes", () => {
    expect(
      isValidNameNodeHaAssignment([
        { component: "NAMENODE", hostName: "nn1", isInstalled: true },
        { component: "NAMENODE", hostName: "nn2", isInstalled: false },
        { component: "JOURNALNODE", hostName: "jn1" },
        { component: "JOURNALNODE", hostName: "jn2" },
        { component: "JOURNALNODE", hostName: "jn3" },
      ]),
    ).toBe(true);
    expect(
      isValidNameNodeHaAssignment([
        { component: "NAMENODE", hostName: "nn1", isInstalled: true },
        { component: "NAMENODE", hostName: "nn1", isInstalled: false },
        { component: "JOURNALNODE", hostName: "jn1" },
        { component: "JOURNALNODE", hostName: "jn1" },
        { component: "JOURNALNODE", hostName: "jn3" },
      ]),
    ).toBe(false);
  });

  it("restores operation status without replacing live callbacks", async () => {
    const callback = vi.fn().mockResolvedValue({ status: 200 });
    const operations = mergeSavedOperations<TestOperation>(
      [{ id: 1, label: "Install", callback }],
      [{ id: 1, status: "FAILED", callback: vi.fn() }],
    );

    await operations[0].callback();
    expect(callback).toHaveBeenCalledOnce();
    expect(operations[0].status).toBe("FAILED");
  });

  it("migrates saved numeric task IDs to stable semantic IDs by label", () => {
    const callback = vi.fn().mockResolvedValue(undefined);
    const operations = mergeSavedOperations<TestOperation>(
      [{ id: "startRanger", label: "Start Ranger", callback }],
      [{ id: 3, label: "Start Ranger", status: "COMPLETED" }],
    );

    expect(operations[0]).toMatchObject({
      id: "startRanger",
      label: "Start Ranger",
      status: "COMPLETED",
    });
    expect(operations[0].callback).toBe(callback);
  });

  it("builds the current desired-config query and rejects missing tags", () => {
    expect(
      buildDesiredConfigQuery(
        {
          "hdfs-site": { tag: "version1" },
          "core-site": { tag: "version2" },
        },
        ["hdfs-site", "core-site", "hdfs-site"],
      ),
    ).toBe(
      "(type=hdfs-site&tag=version1)|(type=core-site&tag=version2)",
    );
    expect(() =>
      buildDesiredConfigQuery({}, ["hdfs-site"]),
    ).toThrow("current hdfs-site configuration tag is missing");
  });

  it("overlays reviewed HA properties on current secure configs", () => {
    expect(
      mergeReviewedConfigs(
        {
          items: [
            {
              type: "hdfs-site",
              properties: {
                currentSecurityProperty: "from-agent-install",
                "dfs.namenode.rpc-address": "old-host:8020",
              },
              properties_attributes: { final: { currentSecurityProperty: "true" } },
            },
          ],
        },
        {
          items: [
            {
              type: "hdfs-site",
              properties: { "dfs.nameservices": "nameservice1" },
            },
          ],
        },
        { "hdfs-site": ["dfs.namenode.rpc-address"] },
      ).items[0],
    ).toEqual({
      type: "hdfs-site",
      properties: {
        currentSecurityProperty: "from-agent-install",
        "dfs.nameservices": "nameservice1",
      },
      properties_attributes: { final: { currentSecurityProperty: "true" } },
    });
  });

  it("writes an edited Review value into the submitted site snapshot", () => {
    const original = {
      items: [
        {
          type: "hdfs-site",
          properties: { "dfs.journalnode.edits.dir": "/old/journal" },
        },
      ],
    };

    const updated = updateReviewedConfigValue(
      original,
      "hdfs-site",
      "dfs.journalnode.edits.dir",
      "/new/journal",
    );

    expect(updated.items[0].properties?.["dfs.journalnode.edits.dir"]).toBe(
      "/new/journal",
    );
    expect(original.items[0].properties["dfs.journalnode.edits.dir"]).toBe(
      "/old/journal",
    );
  });

  it("groups all installed Ranger audit and plugin sites", () => {
    const config = (type: string) => ({
      type,
      properties: { "xasecure.audit.destination.hdfs.dir": "hdfs://ns/audit" },
    });
    expect(
      getRangerReconfigureSiteGroups(
        ["YARN", "STORM", "KAFKA", "KNOX", "ATLAS", "HIVE", "RANGER_KMS"],
        [
          config("ranger-yarn-audit"),
          config("ranger-storm-plugin-properties"),
          config("ranger-storm-audit"),
          config("ranger-kafka-audit"),
          config("ranger-knox-plugin-properties"),
          config("ranger-knox-audit"),
          config("ranger-atlas-audit"),
          config("ranger-hive-plugin-properties"),
          config("ranger-hive-audit"),
          config("ranger-kms-audit"),
        ],
      ),
    ).toEqual([
      ["ranger-env"],
      ["ranger-yarn-audit"],
      ["ranger-storm-plugin-properties", "ranger-storm-audit"],
      ["ranger-kafka-audit"],
      ["ranger-knox-plugin-properties", "ranger-knox-audit"],
      ["ranger-atlas-audit"],
      ["ranger-hive-plugin-properties", "ranger-hive-audit"],
      ["ranger-kms-audit"],
    ]);
  });
});
