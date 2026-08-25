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
  buildDesiredConfigPayload,
  buildHostRecommendationPayload,
  buildRmHaReviewConfig,
  canCompleteRmHa,
  chooseAdditionalRmHost,
  createRmHaAssignment,
  flattenClusterTopology,
  getRmHaEnablementErrors,
  parseRmHaHosts,
  recommendedHostsForComponent,
  RM_HA_ENABLEMENT_MESSAGES,
  RM_HA_OPERATION_IDS,
  visibleHostOptions,
} from "./rmHaUtils";
import { PersistedRmHaOperation, RmHaTopologyEntry } from "./rmHaTypes";

const topology: RmHaTopologyEntry[] = [
  {
    component: "RESOURCEMANAGER",
    serviceName: "YARN",
    hostName: "rm1.example.com",
    state: "STARTED",
    maintenanceState: "OFF",
    isInstalled: true,
  },
  {
    component: "RESOURCEMANAGER",
    serviceName: "YARN",
    hostName: "rm2.example.com",
    maintenanceState: "OFF",
    isInstalled: false,
  },
  ...["zk1.example.com", "zk2.example.com", "zk3.example.com"].map(
    (hostName) => ({
      component: "ZOOKEEPER_SERVER",
      serviceName: "ZOOKEEPER",
      hostName,
      state: "STARTED",
      maintenanceState: "OFF",
      isInstalled: true,
    }),
  ),
];

const configData = {
  items: [
    { type: "zoo.cfg", properties: { clientPort: "2222" } },
    {
      type: "yarn-site",
      properties: {
        "yarn.resourcemanager.resource-tracker.address": "old-rm:18025",
        "yarn.resourcemanager.webapp.address": "old-rm:18088",
        "yarn.resourcemanager.webapp.https.address": "old-rm:18090",
      },
    },
    { type: "yarn-env", properties: { yarn_user: "yarn" } },
  ],
};

const recommendations = {
  resources: [
    {
      recommendations: {
        blueprint: {
          configurations: {
            "core-site": {
              properties: {
                "hadoop.proxyuser.yarn.hosts":
                  "rm1.example.com,rm2.example.com",
              },
            },
          },
        },
      },
    },
  ],
};

function operation(
  id: string,
  status: string,
): PersistedRmHaOperation {
  return {
    id,
    label: id,
    skippable: false,
    status,
  };
}

describe("ResourceManager HA utilities", () => {
  it("parses host and topology resources without fabricating missing entries", () => {
    expect(
      parseRmHaHosts({
        items: [
          {
            Hosts: {
              host_name: "rm1.example.com",
              cpu_count: 8,
              total_mem: 16_000,
              maintenance_state: "OFF",
              disk_info: [{ mountpoint: "/" }],
            },
          },
          { Hosts: {} },
        ],
      }),
    ).toEqual([
      {
        hostName: "rm1.example.com",
        cpuCount: 8,
        totalMemory: 16_000,
        maintenanceState: "OFF",
        diskInfo: [{ mountpoint: "/" }],
      },
    ]);

    expect(
      flattenClusterTopology({
        items: [
          {
            ServiceComponentInfo: {
              component_name: "RESOURCEMANAGER",
              service_name: "YARN",
            },
            host_components: [
              {
                HostRoles: {
                  host_name: "rm1.example.com",
                  state: "STARTED",
                  maintenance_state: "OFF",
                },
              },
            ],
          },
        ],
      }),
    ).toEqual([
      expect.objectContaining({
        component: "RESOURCEMANAGER",
        serviceName: "YARN",
        hostName: "rm1.example.com",
        state: "STARTED",
      }),
    ]);
  });

  it("handles a missing ResourceManager and aggregates independent entry failures", () => {
    const missingRm = getRmHaEnablementErrors({
      topology: [],
      hostNames: ["h1", "h2", "h3"],
      yarnInstalled: true,
      alreadyEnabled: false,
      canEnableHa: true,
      canPersist: true,
    });
    expect(missingRm).toContain(
      RM_HA_ENABLEMENT_MESSAGES.resourceManagerMissing,
    );

    const errors = getRmHaEnablementErrors({
      topology: [
        { ...topology[0], state: "INSTALLED" },
        ...topology.slice(2, 4),
      ],
      hostNames: ["h1", "h2"],
      yarnInstalled: true,
      alreadyEnabled: false,
      canEnableHa: true,
      canPersist: true,
    });
    expect(errors).toEqual([
      RM_HA_ENABLEMENT_MESSAGES.resourceManagerStopped,
      RM_HA_ENABLEMENT_MESSAGES.zooKeeperCount,
      RM_HA_ENABLEMENT_MESSAGES.hostCount,
    ]);
  });

  it("builds the exact host-groups recommendation shape", () => {
    const payload = buildHostRecommendationPayload({
      hostNames: ["rm1.example.com", "rm2.example.com"],
      services: ["YARN", "ZOOKEEPER"],
      topology: topology.slice(0, 2),
    });
    expect(payload).toMatchObject({
      recommend: "host_groups",
      hosts: ["rm1.example.com", "rm2.example.com"],
      services: ["YARN", "ZOOKEEPER"],
      recommendations: {
        blueprint: {
          host_groups: [
            {
              name: "host-group-1",
              components: [{ name: "RESOURCEMANAGER" }],
            },
            {
              name: "host-group-2",
              components: [{ name: "RESOURCEMANAGER" }],
            },
          ],
        },
        blueprint_cluster_binding: {
          host_groups: [
            {
              name: "host-group-1",
              hosts: [{ fqdn: "rm1.example.com" }],
            },
            {
              name: "host-group-2",
              hosts: [{ fqdn: "rm2.example.com" }],
            },
          ],
        },
      },
    });
  });

  it("rejects malformed Advisor results and otherwise uses recommendation then capacity fallback", () => {
    expect(() =>
      recommendedHostsForComponent({}, "RESOURCEMANAGER"),
    ).toThrow("invalid host recommendation");

    const hosts = [
      { hostName: "rm1", totalMemory: 100, cpuCount: 4 },
      { hostName: "rm2", totalMemory: 200, cpuCount: 2 },
      { hostName: "rm3", totalMemory: 300, cpuCount: 8 },
    ];
    expect(chooseAdditionalRmHost(["rm2"], hosts, "rm1")).toBe("rm2");
    expect(chooseAdditionalRmHost([], hosts, "rm1")).toBe("rm3");
    expect(
      chooseAdditionalRmHost(
        ["rm3"],
        hosts.map((host) =>
          host.hostName === "rm3"
            ? { ...host, maintenanceState: "ON" }
            : host,
        ),
        "rm1",
      ),
    ).toBe("rm2");
  });

  it("limits large-cluster typeahead results to ten matching hosts", () => {
    const options = Array.from({ length: 30 }, (_, index) => ({
      label: `worker-${String(index + 1).padStart(2, "0")}.example.com`,
      value: `worker-${index + 1}`,
    }));

    expect(visibleHostOptions(options, "", true)).toHaveLength(10);
    expect(visibleHostOptions(options, "worker-2", true)).toEqual(
      options.slice(19, 29),
    );
    expect(visibleHostOptions(options, "WORKER-30", true)).toEqual([
      options[29],
    ]);
    expect(visibleHostOptions(options, "", false)).toBe(options);
  });

  it("requires distinct available hosts outside maintenance mode", () => {
    const hosts = [
      { hostName: "rm1", maintenanceState: "OFF" },
      { hostName: "rm2", maintenanceState: "ON" },
    ];
    expect(() => createRmHaAssignment("rm1", "rm1", hosts, [])).toThrow(
      "different host",
    );
    expect(() => createRmHaAssignment("rm1", "missing", hosts, [])).toThrow(
      "unavailable",
    );
    expect(() => createRmHaAssignment("rm1", "rm2", hosts, [])).toThrow(
      "maintenance mode",
    );
  });

  it("generates dynamic YARN, HAWQ, ZooKeeper, and proxyuser properties", () => {
    const review = buildRmHaReviewConfig({
      configData,
      recommendationData: recommendations,
      selectedServices: ["YARN", "HAWQ", "HDFS"],
      topology,
    });
    const values = Object.fromEntries(
      review.configs.map(({ name, value }) => [name, value]),
    );
    expect(values).toMatchObject({
      "yarn.resourcemanager.hostname.rm1": "rm1.example.com",
      "yarn.resourcemanager.hostname.rm2": "rm2.example.com",
      "yarn.resourcemanager.resource-tracker.address.rm1":
        "rm1.example.com:18025",
      "yarn.resourcemanager.resource-tracker.address.rm2":
        "rm2.example.com:18025",
      "yarn.resourcemanager.webapp.address.rm1": "rm1.example.com:18088",
      "yarn.resourcemanager.webapp.https.address.rm2":
        "rm2.example.com:18090",
      "yarn.resourcemanager.zk-address":
        "zk1.example.com:2222,zk2.example.com:2222,zk3.example.com:2222",
      "yarn.resourcemanager.ha":
        "rm1.example.com:8032,rm2.example.com:8032",
      "yarn.resourcemanager.scheduler.ha":
        "rm1.example.com:8030,rm2.example.com:8030",
      "hadoop.proxyuser.yarn.hosts": "rm1.example.com,rm2.example.com",
    });
    expect(review.configs.every(({ isEditable }) => !isEditable)).toBe(true);
    expect(review.configs.every(({ isOverridable }) => !isOverridable)).toBe(
      true,
    );
  });

  it("uses default ports and omits HAWQ and absent proxyuser changes", () => {
    const review = buildRmHaReviewConfig({
      configData: {
        items: [
          { type: "zoo.cfg", properties: {} },
          { type: "yarn-site", properties: {} },
          { type: "yarn-env", properties: { yarn_user: "yarn" } },
        ],
      },
      recommendationData: {
        resources: [
          {
            recommendations: {
              blueprint: { configurations: { "core-site": { properties: {} } } },
            },
          },
        ],
      },
      selectedServices: ["YARN", "HDFS"],
      topology,
    });
    const values = Object.fromEntries(
      review.configs.map(({ name, value }) => [name, value]),
    );
    expect(values["yarn.resourcemanager.resource-tracker.address.rm1"]).toBe(
      "rm1.example.com:8025",
    );
    expect(values["yarn.resourcemanager.webapp.address.rm2"]).toBe(
      "rm2.example.com:8088",
    );
    expect(values["yarn.resourcemanager.webapp.https.address.rm1"]).toBe(
      "rm1.example.com:8090",
    );
    expect(values["yarn.resourcemanager.zk-address"]).toContain(":2181");
    expect(values).not.toHaveProperty("yarn.resourcemanager.ha");
    expect(values).not.toHaveProperty("hadoop.proxyuser.yarn.hosts");
  });

  it("rejects missing config items and preserves complete properties and attributes", () => {
    expect(() =>
      buildRmHaReviewConfig({
        configData: { items: configData.items.slice(0, 2) },
        recommendationData: recommendations,
        selectedServices: ["YARN", "HDFS"],
        topology,
      }),
    ).toThrow("yarn-env");

    const review = buildRmHaReviewConfig({
      configData,
      recommendationData: recommendations,
      selectedServices: ["YARN", "HDFS"],
      topology,
    });
    const payload = buildDesiredConfigPayload(
      {
        items: [
          {
            type: "core-site",
            properties: { existing: "value" },
            properties_attributes: { final: { existing: "true" } },
          },
        ],
      },
      "core-site",
      review,
      "Enable ResourceManager HA",
    );
    expect(payload).toEqual({
      Clusters: {
        desired_config: [
          {
            type: "core-site",
            properties: {
              existing: "value",
              "hadoop.proxyuser.yarn.hosts":
                "rm1.example.com,rm2.example.com",
            },
            properties_attributes: { final: { existing: "true" } },
            service_config_version_note: "Enable ResourceManager HA",
          },
        ],
      },
    });
  });

  it("allows completion only when preceding tasks complete and final start terminates", () => {
    const completedPrefix = [
      RM_HA_OPERATION_IDS.STOP_REQUIRED_SERVICES,
      RM_HA_OPERATION_IDS.INSTALL_RESOURCE_MANAGER,
      RM_HA_OPERATION_IDS.RECONFIGURE_YARN,
      RM_HA_OPERATION_IDS.RECONFIGURE_HDFS,
    ].map((id) => operation(id, "COMPLETED"));
    expect(
      canCompleteRmHa([
        ...completedPrefix,
        operation(RM_HA_OPERATION_IDS.START_ALL_SERVICES, "FAILED"),
      ]),
    ).toBe(true);
    expect(
      canCompleteRmHa([
        ...completedPrefix.slice(0, 1),
        operation(RM_HA_OPERATION_IDS.INSTALL_RESOURCE_MANAGER, "FAILED"),
        ...completedPrefix.slice(2),
        operation(RM_HA_OPERATION_IDS.START_ALL_SERVICES, "COMPLETED"),
      ]),
    ).toBe(false);
    expect(
      canCompleteRmHa([
        ...completedPrefix,
        operation(RM_HA_OPERATION_IDS.START_ALL_SERVICES, "IN_PROGRESS"),
      ]),
    ).toBe(false);
  });
});
