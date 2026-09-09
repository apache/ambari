/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useContext, useEffect, useMemo } from "react";
import { AppContext } from "../store/context";
import { ClusterWorkflowPersistence } from "../Utils/scopedWorkflow";
import ClusterApi from "../api/clusterApi";
import { parsePersistedValue } from "../Utils/persistedSettings";
import { translate } from "../Utils/Utility";

type LegacyWorkflowOptions = {
  controllerNames?: string[];
  keys: string[];
};

const meaningfulLegacyValue = (value: unknown) => {
  const parsed = parsePersistedValue<any>(value, null);
  if (parsed == null) return false;
  if (typeof parsed === "boolean") return parsed;
  if (typeof parsed === "number") return parsed !== 0;
  if (Array.isArray(parsed)) return parsed.length > 0;
  if (typeof parsed === "object") return Object.keys(parsed).length > 0;
  return parsed !== "";
};

const optionalLegacyValue = async (key: string) => {
  try {
    return await ClusterApi.getPersistData(key);
  } catch (error: any) {
    if (error?.response?.status === 404 || error?.status === 404) return null;
    throw error;
  }
};

export default function useClusterWorkflowPersistence(
  workflow: string,
  legacy?: LegacyWorkflowOptions,
) {
  const { availableClusters, cluster, loginName, runtimeKey } = useContext(AppContext);
  const clusterId = Number(cluster?.cluster_id);
  const legacyKeys = legacy?.keys.join("\u0000") || "";
  const legacyControllerNames = legacy?.controllerNames?.join("\u0000") || "";
  const persistence = useMemo(() => (
    Number.isInteger(clusterId) && clusterId > 0
      ? new ClusterWorkflowPersistence(clusterId, workflow).withLegacyLoader(async () => {
          const keys = legacyKeys ? legacyKeys.split("\u0000") : [];
          const controllerNames = legacyControllerNames
            ? legacyControllerNames.split("\u0000")
            : [];
          if (!keys.length) return null;
          const authorizedClusterIds = (availableClusters || []).flatMap((item: any) => {
            const id = Number(item?.Clusters?.cluster_id);
            return Number.isInteger(id) && id > 0 ? [id] : [];
          });
          if (authorizedClusterIds.length !== 1 || authorizedClusterIds[0] !== clusterId) {
            return null;
          }
          const [ownerValue, ...legacyValues] = await Promise.all([
            optionalLegacyValue("wizard-data"),
            ...keys.map(optionalLegacyValue),
          ]);
          if (!legacyValues.some(meaningfulLegacyValue)) return null;
          const owner = parsePersistedValue<Record<string, any>>(ownerValue, {});
          const allowedController = !controllerNames.length
            || controllerNames.includes(String(owner.controllerName || ""));
          if (!loginName || owner.userName !== loginName || !allowedController) {
            throw new Error(
              String(translate("workflow.persistence.legacyOwnerUnknown")),
            );
          }
          const values = Object.fromEntries(
            keys.map((key, index) => [
              key,
              parsePersistedValue(legacyValues[index], {}),
            ]),
          );
          const clusterState = values.CLUSTER_STATE || {};
          return {
            phase: String(clusterState.clusterState || clusterState.stepName || workflow),
            values,
          };
        })
      : null
  ), [
    availableClusters,
    clusterId,
    legacyControllerNames,
    legacyKeys,
    loginName,
    runtimeKey,
    workflow,
  ]);

  useEffect(() => {
    persistence?.activate();
    return () => persistence?.deactivate();
  }, [persistence]);

  return persistence;
}
