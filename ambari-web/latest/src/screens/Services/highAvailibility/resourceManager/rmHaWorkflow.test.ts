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

import { beforeEach, describe, expect, it, vi } from "vitest";
import { RmHaApi } from "./rmHaApi";
import {
  createRmHaOperations,
  loadRmHaReview,
  mergePersistedRmHaOperations,
  runWithKdcSession,
} from "./rmHaWorkflow";
import { RM_HA_OPERATION_IDS } from "./rmHaUtils";
import { RmHaReviewConfig, RmHaTopologyEntry } from "./rmHaTypes";

const reviewConfig: RmHaReviewConfig = {
  serviceName: "MISC",
  displayName: "MISC",
  configCategories: [
    { name: "YARN", displayName: "YARN" },
    { name: "HAWQ", displayName: "HAWQ" },
    { name: "HDFS", displayName: "HDFS" },
  ],
  configs: [
    {
      name: "yarn.resourcemanager.ha.enabled",
      category: "YARN",
      filename: "yarn-site",
      value: true,
      changedValue: true,
      isEditable: false,
      isOverridable: false,
    },
    {
      name: "yarn.resourcemanager.ha",
      category: "HAWQ",
      filename: "yarn-client",
      value: "rm1:8032,rm2:8032",
      changedValue: "rm1:8032,rm2:8032",
      isEditable: false,
      isOverridable: false,
    },
    {
      name: "hadoop.proxyuser.yarn.hosts",
      category: "HDFS",
      filename: "core-site",
      value: "rm1,rm2",
      changedValue: "rm1,rm2",
      isEditable: false,
      isOverridable: false,
    },
  ],
};

function mockApi() {
  return {
    getHosts: vi.fn(),
    getClusterComponents: vi.fn(),
    getHostRecommendations: vi.fn(),
    getDesiredConfigs: vi.fn(),
    getConfigs: vi.fn(),
    getConfigRecommendations: vi.fn(),
    saveDesiredConfig: vi.fn(),
    stopRequiredServices: vi.fn(),
    startAllServices: vi.fn(),
    installAdditionalResourceManager: vi.fn(),
  } as unknown as RmHaApi;
}

describe("ResourceManager HA workflow", () => {
  beforeEach(() => vi.clearAllMocks());

  it("runs a protected operation only after KDC success", async () => {
    const operation = vi.fn().mockResolvedValue("installed");
    const checker = vi.fn(async (onSuccess: () => void | Promise<void>) => {
      await onSuccess();
    });

    await expect(runWithKdcSession(checker, operation)).resolves.toBe(
      "installed",
    );
    expect(operation).toHaveBeenCalledOnce();
  });

  it("rejects KDC cancellation, checker failure, and protected-operation failure", async () => {
    const operation = vi.fn().mockResolvedValue("installed");
    await expect(
      runWithKdcSession((_onSuccess, onError) => {
        onError?.(new Error("credential entry cancelled"));
      }, operation),
    ).rejects.toThrow("credential entry cancelled");
    expect(operation).not.toHaveBeenCalled();

    await expect(
      runWithKdcSession(() => {
        throw new Error("KDC unavailable");
      }, operation),
    ).rejects.toThrow("KDC unavailable");

    await expect(
      runWithKdcSession(
        (onSuccess) => onSuccess(),
        vi.fn().mockRejectedValue(new Error("install failed")),
      ),
    ).rejects.toThrow("install failed");
  });

  it("builds stable task order with and without HAWQ", () => {
    const api = mockApi();
    const checker = vi.fn();
    const baseInput = {
      clusterName: "c1",
      additionalRM: "rm2",
      reviewConfig,
      runSmokeTest: true,
      getKdcSessionState: checker,
    };

    const withoutHawq = createRmHaOperations(
      { ...baseInput, services: ["HDFS", "YARN", "ZOOKEEPER"] },
      api,
    );
    expect(withoutHawq.map(({ id }) => id)).toEqual([
      RM_HA_OPERATION_IDS.STOP_REQUIRED_SERVICES,
      RM_HA_OPERATION_IDS.INSTALL_RESOURCE_MANAGER,
      RM_HA_OPERATION_IDS.RECONFIGURE_YARN,
      RM_HA_OPERATION_IDS.RECONFIGURE_HDFS,
      RM_HA_OPERATION_IDS.START_ALL_SERVICES,
    ]);
    expect(withoutHawq.every(({ skippable }) => !skippable)).toBe(true);

    const withHawq = createRmHaOperations(
      { ...baseInput, services: ["HDFS", "YARN", "HAWQ"] },
      api,
    );
    expect(withHawq.map(({ id }) => id)).toEqual([
      RM_HA_OPERATION_IDS.STOP_REQUIRED_SERVICES,
      RM_HA_OPERATION_IDS.INSTALL_RESOURCE_MANAGER,
      RM_HA_OPERATION_IDS.RECONFIGURE_YARN,
      RM_HA_OPERATION_IDS.RECONFIGURE_HAWQ,
      RM_HA_OPERATION_IDS.RECONFIGURE_HDFS,
      RM_HA_OPERATION_IDS.START_ALL_SERVICES,
    ]);
  });

  it("returns request-backed stop/install/start results and direct config results", async () => {
    const api = mockApi();
    api.stopRequiredServices = vi
      .fn()
      .mockResolvedValue({ Requests: { id: 31 }, status: 202 });
    api.installAdditionalResourceManager = vi
      .fn()
      .mockResolvedValue({ Requests: { id: 32 }, status: 202 });
    api.startAllServices = vi
      .fn()
      .mockResolvedValue({ Requests: { id: 33 }, status: 202 });
    api.getDesiredConfigs = vi.fn().mockResolvedValue({
      Clusters: {
        desired_configs: {
          "yarn-site": { tag: "version1" },
          "core-site": { tag: "version2" },
        },
      },
    });
    api.getConfigs = vi.fn().mockImplementation((_cluster, query) => ({
      items: [
        {
          type: query.includes("yarn-site") ? "yarn-site" : "core-site",
          properties: { existing: "value" },
        },
      ],
    }));
    api.saveDesiredConfig = vi.fn().mockResolvedValue({ status: 200 });
    const checker = vi.fn(async (onSuccess: () => void | Promise<void>) => {
      await onSuccess();
    });
    const operations = createRmHaOperations(
      {
        clusterName: "c1",
        services: ["HDFS", "YARN", "ZOOKEEPER"],
        additionalRM: "rm2",
        reviewConfig,
        runSmokeTest: true,
        getKdcSessionState: checker,
      },
      api,
    );

    await expect(operations[0].callback()).resolves.toMatchObject({
      Requests: { id: 31 },
    });
    await expect(operations[1].callback()).resolves.toMatchObject({
      Requests: { id: 32 },
    });
    await expect(operations[2].callback()).resolves.toEqual({ status: 200 });
    await expect(operations[3].callback()).resolves.toEqual({ status: 200 });
    await expect(operations[4].callback()).resolves.toMatchObject({
      Requests: { id: 33 },
    });
    expect(api.stopRequiredServices).toHaveBeenCalledWith("c1", [
      "YARN",
      "ZOOKEEPER",
    ]);
    expect(api.startAllServices).toHaveBeenCalledWith("c1", true);
    expect(api.saveDesiredConfig).toHaveBeenCalledTimes(2);
  });

  it("loads required review configs before requesting Advisor configurations", async () => {
    const api = mockApi();
    api.getDesiredConfigs = vi.fn().mockResolvedValue({
      Clusters: {
        desired_configs: {
          "zoo.cfg": { tag: "zoo1" },
          "yarn-site": { tag: "yarn1" },
          "yarn-env": { tag: "env1" },
        },
      },
    });
    api.getConfigs = vi.fn().mockResolvedValue({
      items: [
        { type: "zoo.cfg", properties: { clientPort: "2181" } },
        { type: "yarn-site", properties: {} },
        { type: "yarn-env", properties: { yarn_user: "yarn" } },
      ],
    });
    api.getConfigRecommendations = vi.fn().mockResolvedValue({
      resources: [
        {
          recommendations: {
            blueprint: {
              configurations: { "core-site": { properties: {} } },
            },
          },
        },
      ],
    });
    const topology: RmHaTopologyEntry[] = [
      {
        component: "RESOURCEMANAGER",
        hostName: "rm1",
        isInstalled: true,
      },
      {
        component: "RESOURCEMANAGER",
        hostName: "rm2",
        isInstalled: false,
      },
      {
        component: "ZOOKEEPER_SERVER",
        hostName: "zk1",
        isInstalled: true,
      },
    ];

    const result = await loadRmHaReview(
      {
        clusterName: "c1",
        stack: "HDP",
        version: "3.1",
        hostNames: ["rm1", "rm2", "zk1"],
        services: ["YARN", "HDFS"],
        topology,
      },
      api,
    );

    expect(api.getConfigs).toHaveBeenCalledWith(
      "c1",
      "(type=zoo.cfg&tag=zoo1)|(type=yarn-site&tag=yarn1)|(type=yarn-env&tag=env1)",
    );
    expect(api.getConfigRecommendations).toHaveBeenCalledWith(
      "HDP",
      "3.1",
      expect.objectContaining({
        recommend: "configurations",
        hosts: ["rm1", "rm2", "zk1"],
        services: ["YARN", "HDFS"],
      }),
    );
    expect(result.configs).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          name: "yarn.resourcemanager.hostname.rm2",
          value: "rm2",
        }),
      ]),
    );
  });

  it("merges saved operation state by stable ID and keeps live callbacks", () => {
    const api = mockApi();
    const operations = createRmHaOperations(
      {
        clusterName: "c1",
        services: ["HDFS", "YARN"],
        additionalRM: "rm2",
        reviewConfig,
        runSmokeTest: true,
        getKdcSessionState: vi.fn(),
      },
      api,
    );
    const originalCallback = operations[0].callback;
    const merged = mergePersistedRmHaOperations(operations, [
      {
        id: RM_HA_OPERATION_IDS.STOP_REQUIRED_SERVICES,
        label: "Stop Required Services",
        skippable: false,
        status: "IN_PROGRESS",
        requestId: 44,
      },
      {
        id: "unknown-operation",
        label: "Unknown",
        skippable: false,
        status: "COMPLETED",
      },
    ]);

    expect(merged[0]).toMatchObject({
      id: RM_HA_OPERATION_IDS.STOP_REQUIRED_SERVICES,
      status: "IN_PROGRESS",
      requestId: 44,
    });
    expect(merged[0].callback).toBe(originalCallback);
    expect(merged).toHaveLength(operations.length);
  });
});
