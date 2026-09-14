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

import {
  createSecureUuid,
  type SecureRandomSource,
} from "../../Utils/uuid";
import {
  containsReentryMarker,
  sanitizeWorkflowValues,
} from "../../Utils/scopedWorkflow";

export type DeploymentInputSignatures = Record<string, string>;

const canonicalValue = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(canonicalValue);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .filter(([, child]) => child !== undefined)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, child]) => [key, canonicalValue(child)]),
    );
  }
  return value;
};

type DeploymentCryptoSource = SecureRandomSource & {
  subtle?: Pick<SubtleCrypto, "digest">;
};

export type DeploymentSignatureScope = {
  sensitiveEpochs: Map<string, string>;
};

export const createDeploymentSignatureScope = (): DeploymentSignatureScope => ({
  sensitiveEpochs: new Map(),
});

export const clearDeploymentSignatureScope = (scope: DeploymentSignatureScope) => {
  scope.sensitiveEpochs.clear();
};

const MAX_SENSITIVE_INPUT_EPOCHS = 32;

const localDigest = (bytes: Uint8Array) => {
  let left = 0x811c9dc5;
  let right = 0x9e3779b9;
  bytes.forEach((byte) => {
    left = Math.imul(left ^ byte, 0x01000193);
    right = Math.imul(right ^ byte, 0x85ebca6b);
  });
  return [left, right]
    .map((part) => (part >>> 0).toString(16).padStart(8, "0"))
    .join("");
};

const sensitiveRuntimeEpoch = (
  source: DeploymentCryptoSource,
  inputIdentity: string,
  scope: DeploymentSignatureScope,
) => {
  let epoch = scope.sensitiveEpochs.get(inputIdentity);
  if (!epoch) {
    if (scope.sensitiveEpochs.size >= MAX_SENSITIVE_INPUT_EPOCHS) {
      const oldest = scope.sensitiveEpochs.keys().next().value;
      if (oldest !== undefined) scope.sensitiveEpochs.delete(oldest);
    }
    epoch = createSecureUuid(source);
    scope.sensitiveEpochs.set(inputIdentity, epoch);
  }
  return epoch;
};

export const deploymentInputSignature = async (
  value: unknown,
  source: DeploymentCryptoSource = globalThis.crypto,
  scope: DeploymentSignatureScope = createDeploymentSignatureScope(),
) => {
  const sanitized = sanitizeWorkflowValues({ input: value });
  const bytes = new TextEncoder().encode(JSON.stringify(canonicalValue(sanitized)));
  const runtimeEpoch = containsReentryMarker(sanitized)
    ? `:${sensitiveRuntimeEpoch(
      source,
      JSON.stringify(canonicalValue(value)),
      scope,
    )}`
    : "";
  if (!source.subtle) {
    return `local:${localDigest(bytes)}${runtimeEpoch}`;
  }
  const digest = await source.subtle.digest("SHA-256", bytes);
  return `sha256:${[...new Uint8Array(digest)]
    .map((part) => part.toString(16).padStart(2, "0"))
    .join("")}${runtimeEpoch}`;
};

const affectedOperationPrefixes: Record<string, string[]> = {
  configuration: [
    "apply-configurations",
    "update-kerberos-descriptor",
  ],
  configGroups: ["create-configuration-groups"],
  hosts: [
    "register-hosts",
    "register-masters",
    "register-slaves-clients",
    "register-required-components",
  ],
  managedDependencies: [
    "complete-managed-dependency-previews",
    "apply-configurations",
    "create-managed-dependency",
  ],
  masters: ["register-masters", "register-required-components"],
  serviceComponents: [
    "create-components",
    "register-masters",
    "register-slaves-clients",
    "register-required-components",
  ],
  services: [
    "create-services",
    "complete-managed-dependency-previews",
    "apply-configurations",
    "create-components",
    "create-configuration-groups",
    "register-masters",
    "register-slaves-clients",
    "register-required-components",
    "update-kerberos-descriptor",
    "create-managed-dependency",
  ],
  slavesAndClients: [
    "register-slaves-clients",
    "register-required-components",
  ],
};

const matchesPrefix = (operationId: string, prefix: string) =>
  operationId === prefix || operationId.startsWith(`${prefix}-`);

const TOPOLOGY_INPUTS = new Set(["masters", "slavesAndClients"]);

const hasCompletedAffectedOperation = (
  input: string,
  completedOperationIds: string[],
) => (affectedOperationPrefixes[input] || []).some((prefix) =>
  completedOperationIds.some((operationId) => matchesPrefix(operationId, prefix)));

export class DeploymentTopologyRecoveryRequiredError extends Error {
  readonly changedInputs: string[];

  constructor(changedInputs: string[]) {
    super("Saved deployment topology changed after preparation began.");
    this.name = "DeploymentTopologyRecoveryRequiredError";
    this.changedInputs = changedInputs;
  }
}

export function reconcileDeploymentInputSignatures({
  attemptedTopologyInputs = [],
  completedOperationIds,
  current,
  previous,
}: {
  attemptedTopologyInputs?: Iterable<string>;
  completedOperationIds: Iterable<string>;
  current: DeploymentInputSignatures;
  previous?: DeploymentInputSignatures;
}) {
  const completed = [...completedOperationIds];
  const attemptedTopology = new Set(attemptedTopologyInputs);
  const changedInputs = Object.keys(current).filter(
    (key) => previous?.[key] !== current[key],
  );
  const topologyChanges = previous
    ? changedInputs.filter((key) =>
      TOPOLOGY_INPUTS.has(key)
      && (attemptedTopology.has(key) || hasCompletedAffectedOperation(key, completed)))
    : [];
  const invalidatedPrefixes = new Set(
    changedInputs.flatMap((key) => affectedOperationPrefixes[key] || []),
  );
  const nextCompletedOperationIds = completed.filter(
    (operationId) => ![...invalidatedPrefixes].some((prefix) =>
      matchesPrefix(operationId, prefix)),
  );
  return {
    changedInputs,
    topologyChanges,
    completedOperationIds: nextCompletedOperationIds,
    changed: changedInputs.length > 0,
  };
}
