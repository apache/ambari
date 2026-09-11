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

type SelectionStorage = Pick<Storage, "getItem" | "removeItem" | "setItem">;

type AddServiceSelectionScope = {
  clusterId: number | string | null | undefined;
  principal: string | null | undefined;
};

const serviceNamePattern = /^[A-Z][A-Z0-9_]*$/;

const selectionKey = ({ clusterId, principal }: AddServiceSelectionScope) => {
  const numericClusterId = Number(clusterId);
  const normalizedPrincipal = String(principal || "").trim();
  if (!normalizedPrincipal || !Number.isInteger(numericClusterId) || numericClusterId <= 0) {
    return null;
  }
  return `ambari:workflow-selection:${JSON.stringify([
    "principal-cluster-workflow",
    normalizedPrincipal,
    numericClusterId,
    "ADD_SERVICE",
  ])}`;
};

export function saveAddServiceSelectionIntent(
  scope: AddServiceSelectionScope,
  serviceName: string,
  storage: SelectionStorage = sessionStorage,
) {
  const key = selectionKey(scope);
  if (!key || !serviceNamePattern.test(serviceName)) return false;
  try {
    storage.setItem(key, serviceName);
    return true;
  } catch {
    return false;
  }
}

export function consumeAddServiceSelectionIntent(
  scope: AddServiceSelectionScope,
  storage: SelectionStorage = sessionStorage,
) {
  const key = selectionKey(scope);
  if (!key) return null;
  try {
    const serviceName = storage.getItem(key);
    if (serviceName !== null) storage.removeItem(key);
    return serviceName && serviceNamePattern.test(serviceName) ? serviceName : null;
  } catch {
    return null;
  }
}
