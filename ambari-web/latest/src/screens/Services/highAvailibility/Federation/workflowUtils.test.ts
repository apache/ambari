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
  applyReviewedProperty,
  buildDesiredConfigQuery,
  buildFederationRestartPayload,
  buildHostValidationPayload,
  buildNameNodeFederationConfiguration,
  buildRouterFederationConfiguration,
  evaluateHawqCapabilities,
  federationTaskKeys,
  hawqTaskKeys,
  isMissingComponentError,
  mutateHawqConfiguration,
  validateComponentAssignments,
  validateJournalNodeDirectory,
  validateNameserviceId,
  type ConfigSnapshot,
} from "./workflowUtils";

const snapshot = (): ConfigSnapshot => ({
  items: [
    {
      type: "hdfs-site",
      properties: {
        "dfs.nameservices": "ns1",
        "dfs.namenode.rpc-address": "host:8020",
        "dfs.namenode.http-address": "host:50070",
        "dfs.namenode.https-address": "host:50470",
        "dfs.namenode.rpc-address.ns1.nn1": "nn1:8020",
        "dfs.namenode.rpc-address.ns1.nn2": "nn2:8020",
        "dfs.namenode.servicerpc-address.ns1.nn1": "nn1:8021",
        "dfs.namenode.servicerpc-address.ns1.nn2": "nn2:8021",
        "dfs.namenode.shared.edits.dir": "qjournal://jn1:8485/ns1",
        "dfs.journalnode.edits.dir": "/journal",
      },
      properties_attributes: { final: { protected: "true" } },
    },
    {
      type: "core-site",
      properties: {
        "fs.defaultFS": "hdfs://ns1",
        "ha.zookeeper.quorum": "zk1:2181,zk2:2181,zk3:2181",
      },
    },
    {
      type: "ranger-tagsync-site",
      properties: {},
    },
    {
      type: "ranger-hdfs-security",
      properties: { "ranger.plugin.hdfs.service.name": "{{repo_name}}" },
    },
    {
      type: "accumulo-site",
      properties: {},
    },
  ],
});

const assignments = [
  { component: "NAMENODE", hostName: "nn1", isInstalled: true },
  { component: "NAMENODE", hostName: "nn2", isInstalled: true },
  { component: "NAMENODE", hostName: "nn3", isInstalled: false },
  { component: "NAMENODE", hostName: "nn4", isInstalled: false },
];

describe("Federation workflow utilities", () => {
  it("validates nameservice IDs only after topology is ready", () => {
    expect(validateNameserviceId("ns2", ["ns1"], false)).toContain("loading");
    expect(validateNameserviceId("ns1", ["ns1"])).toContain("already");
    expect(validateNameserviceId("-ns", ["ns1"])).toContain("1 to 63");
    expect(validateNameserviceId("ns2", ["ns1"])).toBe("");
  });

  it("matches the Classic directory rules including Windows URLs", () => {
    expect(validateJournalNodeDirectory("/journal/ns2")).toBe("");
    expect(validateJournalNodeDirectory("C:\\journal\\ns2")).toBe("");
    expect(validateJournalNodeDirectory("file:///C:/journal/ns2")).toBe("");
    expect(validateJournalNodeDirectory("/journal/a,/journal/b")).toBe("");
    expect(validateJournalNodeDirectory("/home/hdfs")).toContain("not allowed");
    expect(validateJournalNodeDirectory("/journal/a, /journal/b")).toContain(
      "whitespace",
    );
    expect(validateJournalNodeDirectory("/journal/a ")).toContain("trailing");
  });

  it("rejects missing, duplicate, and maintenance-mode assignments", () => {
    expect(
      validateComponentAssignments(assignments, "NAMENODE", 2),
    ).toBe("");
    expect(
      validateComponentAssignments(
        [...assignments.slice(0, 3), { ...assignments[3], hostName: "nn3" }],
        "NAMENODE",
        2,
      ),
    ).toContain("cannot share");
    expect(
      validateComponentAssignments(assignments, "NAMENODE", 2, ["nn4"]),
    ).toContain("maintenance");
    expect(
      validateComponentAssignments(
        assignments.map((assignment) =>
          assignment.hostName === "nn4"
            ? { ...assignment, isAvailable: false }
            : assignment,
        ),
        "NAMENODE",
        2,
      ),
    ).toContain("nn4 is no longer available");
  });

  it("generates the complete first-expansion HDFS, Ranger, and Accumulo snapshot", () => {
    const generated = buildNameNodeFederationConfiguration({
      clusterName: "c1",
      newNameserviceId: "ns2",
      namespaces: [{ name: "ns1", hosts: ["nn1", "nn2"] }],
      assignments,
      journalNodeHosts: ["jn3", "jn1", "jn2"],
      installedServices: ["HDFS", "RANGER", "ACCUMULO"],
      snapshot: snapshot(),
    });
    const hdfs = generated.snapshot.items.find(
      (item) => item.type === "hdfs-site",
    )!;
    expect(hdfs.properties).toMatchObject({
      "dfs.nameservices": "ns1,ns2",
      "dfs.internal.nameservices": "ns1,ns2",
      "dfs.ha.namenodes.ns2": "nn3,nn4",
      "dfs.namenode.rpc-address.ns2.nn3": "nn3:8020",
      "dfs.namenode.rpc-address.ns2.nn4": "nn4:8020",
      "dfs.namenode.shared.edits.dir.ns1":
        "qjournal://jn1:8485;jn2:8485;jn3:8485/ns1",
      "dfs.namenode.shared.edits.dir.ns2":
        "qjournal://jn1:8485;jn2:8485;jn3:8485/ns2",
      "dfs.journalnode.edits.dir.ns1": "/journal",
      "dfs.journalnode.edits.dir.ns2": "",
    });
    expect(hdfs.properties).not.toHaveProperty("dfs.namenode.shared.edits.dir");
    expect(hdfs.properties).not.toHaveProperty("dfs.journalnode.edits.dir");
    expect(hdfs.properties_attributes).toEqual({
      final: { protected: "true" },
    });

    const ranger = generated.snapshot.items.find(
      (item) => item.type === "ranger-tagsync-site",
    )!;
    expect(Object.keys(ranger.properties)).toHaveLength(3);
    expect(
      ranger.properties[
        "ranger.tagsync.atlas.hdfs.instance.c1.ranger.service"
      ],
    ).toBe("c1_hadoop_ns1");
    const accumulo = generated.snapshot.items.find(
      (item) => item.type === "accumulo-site",
    )!;
    expect(accumulo.properties["instance.volumes"]).toBe(
      "hdfs://ns1/apps/accumulo/data,hdfs://ns2/apps/accumulo/data",
    );
    expect(accumulo.properties["instance.volumes.replacements"]).toContain(
      "hdfs://nn1:8020/apps/accumulo/data hdfs://ns1/apps/accumulo/data",
    );

    const updated = applyReviewedProperty(
      generated,
      "dfs.journalnode.edits.dir.ns2",
      "/journal/ns2",
    );
    expect(
      updated.snapshot.items.find((item) => item.type === "hdfs-site")
        ?.properties["dfs.journalnode.edits.dir.ns2"],
    ).toBe("/journal/ns2");
  });

  it("omits first-run properties on later namespace expansion", () => {
    const current = snapshot();
    const hdfs = current.items[0].properties;
    hdfs["dfs.nameservices"] = "ns1,ns2";
    hdfs["dfs.namenode.rpc-address.ns2.nn3"] = "nn3:8020";
    hdfs["dfs.namenode.rpc-address.ns2.nn4"] = "nn4:8020";
    const generated = buildNameNodeFederationConfiguration({
      clusterName: "c1",
      newNameserviceId: "ns3",
      namespaces: [
        { name: "ns1", hosts: ["nn1", "nn2"] },
        { name: "ns2", hosts: ["nn3", "nn4"] },
      ],
      assignments: [
        ...assignments,
        { component: "NAMENODE", hostName: "nn5", isInstalled: false },
        { component: "NAMENODE", hostName: "nn6", isInstalled: false },
      ].map((item, index) => ({
        ...item,
        isInstalled: index < 4,
      })),
      journalNodeHosts: ["jn1", "jn2", "jn3"],
      installedServices: ["HDFS"],
      snapshot: current,
    });
    expect(
      generated.reviewedProperties.map((item) => item.name),
    ).not.toContain("dfs.journalnode.edits.dir.ns1");
    expect(
      generated.snapshot.items[0].properties["dfs.ha.namenodes.ns3"],
    ).toBe("nn5,nn6");
  });

  it("initializes a missing hdfs-rbf-site and derives continuous Router NN IDs", () => {
    const current = snapshot();
    current.items[0].properties["dfs.nameservices"] = "ns1,ns2";
    const generated = buildRouterFederationConfiguration(current, [
      { name: "ns1", hosts: ["nn1", "nn2"] },
      { name: "ns2", hosts: ["nn3", "nn4"] },
    ]);
    const rbf = generated.snapshot.items.find(
      (item) => item.type === "hdfs-rbf-site",
    )!;
    expect(rbf.properties).toEqual({
      "dfs.federation.router.monitor.namenode":
        "ns1.nn1,ns1.nn2,ns2.nn3,ns2.nn4",
      "dfs.federation.router.default.nameserviceId": "ns1",
      "zk-dt-secret-manager.zkAuthType": "none",
      "zk-dt-secret-manager.zkConnectionString":
        "zk1:2181,zk2:2181,zk3:2181",
    });
  });

  it("rejects a stale namespace model before generating Federation configs", () => {
    expect(() =>
      buildRouterFederationConfiguration(snapshot(), [
        { name: "ns1", hosts: ["nn1", "nn2"] },
        { name: "ns2", hosts: ["nn3", "nn4"] },
      ]),
    ).toThrow("does not match dfs.nameservices");
  });

  it("builds required and optional desired-config queries", () => {
    expect(
      buildDesiredConfigQuery(
        { "hdfs-site": { tag: "v1" }, "core-site": { tag: "v2" } },
        ["hdfs-site", "core-site"],
        ["hdfs-rbf-site"],
      ),
    ).toBe("(type=hdfs-site&tag=v1)|(type=core-site&tag=v2)");
    expect(() => buildDesiredConfigQuery({}, ["hdfs-site"])).toThrow(
      "hdfs-site",
    );
  });

  it("uses stable task keys in the exact Classic NNF order", () => {
    expect(federationTaskKeys(["HDFS", "RANGER", "AMBARI_INFRA_SOLR"])).toEqual([
      "stopRequiredServices",
      "reconfigureServices",
      "installNameNode",
      "installZKFC",
      "startJournalNodes",
      "startInfraSolr",
      "startRangerAdmin",
      "startRangerUsersync",
      "startNameNodes",
      "startZKFCs",
      "formatNameNode",
      "formatZKFC",
      "startZKFC",
      "startNameNode",
      "bootstrapNameNode",
      "startZKFC2",
      "startNameNode2",
      "restartAllServices",
    ]);
    expect(federationTaskKeys(["HDFS"])).not.toContain("startRangerAdmin");
  });

  it("restarts non-Federation components without a stale-config gate", () => {
    const predicate = buildFederationRestartPayload("c1")[
      "Requests/resource_filters"
    ][0].hosts_predicate;
    expect(predicate).toContain("HostRoles/cluster_name=c1");
    expect(predicate).toContain("HostRoles/component_name!=NAMENODE");
    expect(predicate).not.toContain("stale_configs");
  });

  it("builds the host-group validation payload used by HAWQ Add", () => {
    const payload = buildHostValidationPayload(
      ["h1", "h2"],
      ["HDFS", "HAWQ"],
      [
        { component: "HAWQMASTER", hostName: "h1", isInstalled: true },
        { component: "HAWQSTANDBY", hostName: "h2", isInstalled: false },
      ],
    );
    expect(payload.validate).toBe("host_groups");
    expect(payload.recommendations.blueprint.host_groups[1]).toEqual({
      name: "host-group-2",
      components: [{ name: "HAWQSTANDBY" }],
    });
  });
});

describe("HAWQ standby workflow utilities", () => {
  const capabilityInput = {
    serviceInstalled: true,
    hostCount: 3,
    configTypes: ["hawq-site"],
    stackComponents: [
      { name: "HAWQMASTER", customCommands: ["REMOVE_HAWQ_STANDBY"] },
      { name: "HAWQSTANDBY", customCommands: ["ACTIVATE_HAWQ_STANDBY"] },
    ],
    installedComponents: [
      { name: "HAWQMASTER", hostName: "master", state: "STARTED" },
      { name: "HAWQSTANDBY", hostName: "standby" },
    ],
  };

  it("requires actual stack, config, command, and installed topology capabilities", () => {
    expect(evaluateHawqCapabilities(capabilityInput)).toMatchObject({
      supported: true,
      canAdd: false,
      canRemove: true,
      canActivate: true,
      masterHost: "master",
      standbyHost: "standby",
    });
    expect(
      evaluateHawqCapabilities({ ...capabilityInput, configTypes: [] }),
    ).toMatchObject({ supported: false, canAdd: false });
    expect(
      evaluateHawqCapabilities({
        ...capabilityInput,
        installedComponents: [
          { name: "HAWQMASTER", hostName: "master", state: "INSTALLED" },
          { name: "HAWQSTANDBY", hostName: "standby" },
        ],
      }),
    ).toMatchObject({ supported: true, canRemove: false, canActivate: true });
    expect(
      evaluateHawqCapabilities({
        ...capabilityInput,
        hostCount: 1,
        installedComponents: [
          { name: "HAWQMASTER", hostName: "master", state: "STARTED" },
        ],
      }),
    ).toMatchObject({ supported: true, canAdd: false });
  });

  it("merges HAWQ changes into the latest snapshot for every mode", () => {
    const current = {
      items: [
        {
          type: "hawq-site",
          properties: {
            concurrent: "preserved",
            hawq_standby_address_host: "old",
          },
        },
      ],
    };
    expect(
      mutateHawqConfiguration(current, "add", {
        masterHost: "master",
        standbyHost: "new",
      }).items[0].properties,
    ).toMatchObject({ concurrent: "preserved", hawq_standby_address_host: "new" });
    expect(
      mutateHawqConfiguration(current, "remove", {
        masterHost: "master",
        standbyHost: "old",
      }).items[0].properties,
    ).toEqual({ concurrent: "preserved" });
    expect(
      mutateHawqConfiguration(current, "activate", {
        masterHost: "master",
        standbyHost: "old",
      }).items[0].properties,
    ).toEqual({ concurrent: "preserved", hawq_master_address_host: "old" });
  });

  it("keeps exact task order for add, remove, and activate", () => {
    expect(hawqTaskKeys("add")).toEqual([
      "stopRequiredServices",
      "installHawqStandbyMaster",
      "reconfigureHAWQ",
      "startRequiredServices",
    ]);
    expect(hawqTaskKeys("remove")).toEqual([
      "removeStandby",
      "stopRequiredServices",
      "reconfigureHAWQ",
      "deleteHawqStandbyComponent",
      "startRequiredServices",
    ]);
    expect(hawqTaskKeys("activate")).toEqual([
      "activateStandby",
      "stopRequiredServices",
      "reconfigureHAWQ",
      "installHawqMaster",
      "deleteOldHawqMaster",
      "deleteHawqStandby",
      "startRequiredServices",
    ]);
  });

  it("treats missing host components as an idempotent delete", () => {
    expect(isMissingComponentError({ response: { status: 404 } })).toBe(true);
    expect(isMissingComponentError({ status: 404 })).toBe(true);
    expect(
      isMissingComponentError({
        response: { data: { message: "NoSuchResourceException" } },
      }),
    ).toBe(true);
    expect(isMissingComponentError({ response: { status: 500 } })).toBe(false);
  });
});
