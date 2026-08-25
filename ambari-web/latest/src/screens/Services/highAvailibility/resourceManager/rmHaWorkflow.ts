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

import rmHaApi, { RmHaApi } from "./rmHaApi";
import {
  buildConfigRecommendationPayload,
  buildDesiredConfigPayload,
  buildRmHaReviewConfig,
  requiredDesiredTag,
  requireConfigItems,
  RM_HA_OPERATION_IDS,
} from "./rmHaUtils";
import {
  PersistedRmHaOperation,
  RmHaOperation,
  RmHaReviewConfig,
  RmHaTopologyEntry,
} from "./rmHaTypes";

export type KdcSessionChecker = (
  onSuccess: () => void | Promise<void>,
  onError?: (error: unknown) => void,
) => void | Promise<void>;

export function runWithKdcSession<T>(
  getKdcSessionState: KdcSessionChecker,
  operation: () => Promise<T>,
): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    let settled = false;
    const fail = (error: unknown) => {
      if (settled) return;
      settled = true;
      reject(error instanceof Error ? error : new Error(String(error)));
    };
    const succeed = async () => {
      if (settled) return;
      try {
        const result = await operation();
        settled = true;
        resolve(result);
      } catch (error) {
        fail(error);
      }
    };
    try {
      Promise.resolve(getKdcSessionState(succeed, fail)).catch(fail);
    } catch (error) {
      fail(error);
    }
  });
}

export async function loadRmHaReview(
  {
    clusterName,
    stack,
    version,
    hostNames,
    services,
    topology,
  }: {
    clusterName: string;
    stack: string;
    version: string;
    hostNames: string[];
    services: string[];
    topology: RmHaTopologyEntry[];
  },
  api: RmHaApi = rmHaApi,
): Promise<RmHaReviewConfig> {
  const tags = await api.getDesiredConfigs(clusterName);
  const query = ["zoo.cfg", "yarn-site", "yarn-env"]
    .map((type) => `(type=${type}&tag=${requiredDesiredTag(tags, type)})`)
    .join("|");
  const configData = await api.getConfigs(clusterName, query);
  const items = requireConfigItems(configData, [
    "zoo.cfg",
    "yarn-site",
    "yarn-env",
  ]);
  const configurations = Object.fromEntries(
    items.map((item) => [item.type, { properties: item.properties }]),
  );
  const payload = buildConfigRecommendationPayload({
    hostNames,
    services,
    topology,
    configurations,
  });
  const recommendationData = await api.getConfigRecommendations(
    stack,
    version,
    payload,
  );
  return buildRmHaReviewConfig({
    configData,
    recommendationData,
    selectedServices: services,
    topology,
  });
}

async function reconfigureSite(
  clusterName: string,
  type: string,
  reviewConfig: RmHaReviewConfig,
  api: RmHaApi,
) {
  const tags = await api.getDesiredConfigs(clusterName);
  const tag = requiredDesiredTag(tags, type);
  const currentConfig = await api.getConfigs(
    clusterName,
    `(type=${type}&tag=${tag})`,
  );
  const payload = buildDesiredConfigPayload(
    currentConfig,
    type,
    reviewConfig,
    "Enable ResourceManager HA",
  );
  return api.saveDesiredConfig(clusterName, payload);
}

export function createRmHaOperations(
  {
    clusterName,
    services,
    additionalRM,
    reviewConfig,
    runSmokeTest,
    getKdcSessionState,
  }: {
    clusterName: string;
    services: string[];
    additionalRM: string;
    reviewConfig: RmHaReviewConfig;
    runSmokeTest: boolean;
    getKdcSessionState: KdcSessionChecker;
  },
  api: RmHaApi = rmHaApi,
): RmHaOperation[] {
  const operations: RmHaOperation[] = [
    {
      id: RM_HA_OPERATION_IDS.STOP_REQUIRED_SERVICES,
      label: "Stop Required Services",
      skippable: false,
      callback: () =>
        api.stopRequiredServices(
          clusterName,
          services.filter((service) => service !== "HDFS"),
        ),
    },
    {
      id: RM_HA_OPERATION_IDS.INSTALL_RESOURCE_MANAGER,
      label: "Install Additional ResourceManager",
      skippable: false,
      callback: () =>
        runWithKdcSession(getKdcSessionState, () =>
          api.installAdditionalResourceManager(clusterName, additionalRM),
        ),
    },
    {
      id: RM_HA_OPERATION_IDS.RECONFIGURE_YARN,
      label: "Reconfigure YARN",
      skippable: false,
      callback: () =>
        reconfigureSite(clusterName, "yarn-site", reviewConfig, api),
    },
  ];
  if (services.includes("HAWQ")) {
    operations.push({
      id: RM_HA_OPERATION_IDS.RECONFIGURE_HAWQ,
      label: "Reconfigure HAWQ",
      skippable: false,
      callback: () =>
        reconfigureSite(clusterName, "yarn-client", reviewConfig, api),
    });
  }
  operations.push(
    {
      id: RM_HA_OPERATION_IDS.RECONFIGURE_HDFS,
      label: "Reconfigure HDFS",
      skippable: false,
      callback: () =>
        reconfigureSite(clusterName, "core-site", reviewConfig, api),
    },
    {
      id: RM_HA_OPERATION_IDS.START_ALL_SERVICES,
      label: "Start All Services",
      skippable: false,
      callback: () => api.startAllServices(clusterName, runSmokeTest),
    },
  );
  return operations;
}

export function mergePersistedRmHaOperations(
  operations: RmHaOperation[],
  savedOperations: PersistedRmHaOperation[] | undefined,
): RmHaOperation[] {
  if (!Array.isArray(savedOperations)) return operations;
  return operations.map((operation) => {
    const saved = savedOperations.find(({ id }) => id === operation.id);
    return saved ? { ...operation, ...saved, callback: operation.callback } : operation;
  });
}
