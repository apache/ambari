/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { normalizeLegacyMainPath } from "./clusterRoute";

export type WorkflowReturnName = "ADD_SERVICE" | "ENABLING_KERBEROS";

export type WorkflowReturnScope = {
  clusterId: number | string | null | undefined;
  principal: string | null | undefined;
  workflow: WorkflowReturnName;
};

type WorkflowReturnStorage = Pick<Storage, "getItem" | "removeItem" | "setItem">;

const returnPathKey = ({ clusterId, principal, workflow }: WorkflowReturnScope) => {
  const numericClusterId = Number(clusterId);
  const normalizedPrincipal = String(principal || "").trim();
  if (!normalizedPrincipal || !Number.isInteger(numericClusterId) || numericClusterId <= 0) {
    return null;
  }
  return `ambari:workflow-return:${JSON.stringify([
    "principal-cluster-workflow",
    normalizedPrincipal,
    numericClusterId,
    workflow,
  ])}`;
};

export function saveWorkflowReturnPath(
  scope: WorkflowReturnScope,
  path: string,
  storage: WorkflowReturnStorage = sessionStorage,
) {
  const key = returnPathKey(scope);
  const normalizedPath = normalizeLegacyMainPath(path);
  if (!key || !normalizedPath) return false;
  try {
    storage.setItem(key, normalizedPath);
    return true;
  } catch {
    return false;
  }
}

export function consumeWorkflowReturnPath(
  scope: WorkflowReturnScope,
  fallback: string,
  storage: WorkflowReturnStorage = sessionStorage,
) {
  const safeFallback = normalizeLegacyMainPath(fallback) || "/main/dashboard/metrics";
  const key = returnPathKey(scope);
  if (!key) return safeFallback;
  try {
    const storedPath = storage.getItem(key);
    if (storedPath !== null) storage.removeItem(key);
    return normalizeLegacyMainPath(storedPath || "") || safeFallback;
  } catch {
    return safeFallback;
  }
}
