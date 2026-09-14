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

import type {
  CreateManagedDependencyRequest,
  ManagedDependencyBinding,
  ManagedDependencyPlanSelection,
  ManagedDependencyPreview,
  ManagedDependencyType,
} from "../../api/serviceDependenciesApi";

export type ManagedDependencyMaterializationRecord = {
  attempts?: ManagedDependencyMaterializationAttempt[];
  bindingId: string;
  clusterId: number;
  clusterName: string;
  dependencyType: ManagedDependencyType;
  operationId: string;
  request?: CreateManagedDependencyRequest;
  response?: ManagedDependencyBinding;
};

export type ManagedDependencyMaterializationAttempt = {
  operationId: string;
  request: CreateManagedDependencyRequest;
  response?: ManagedDependencyBinding;
};

export type ManagedDependencyMaterializations = Partial<
  Record<ManagedDependencyType, ManagedDependencyMaterializationRecord>
>;

export function buildManagedDependencyPlanSelections(
  choices: Array<{
    dependencyType: ManagedDependencyType;
    preview: ManagedDependencyPreview;
  }>,
): ManagedDependencyPlanSelection[] {
  return choices.map(({ dependencyType, preview }) => ({
    binding_id: preview.binding_id,
    dependency_type: dependencyType,
    provider: {
      cluster_id: preview.provider.cluster_id,
      service_name: preview.provider.service_name,
    },
  }));
}

export const managedDependencyReviewSignature = (
  preview: ManagedDependencyPreview,
) => JSON.stringify({
  bindingId: preview.binding_id,
  clientConfig: preview.client_config || {},
  compatible: preview.compatible,
  consumerDescriptorFingerprint: preview.consumer_descriptor_fingerprint,
  dependencyType: preview.dependency_type,
  errors: preview.errors || [],
  namespace: preview.namespace || {},
  plannedHBaseUser: preview.consumer.planned_hbase_user,
  provider: {
    clusterId: preview.provider.cluster_id,
    clusterName: preview.provider.cluster_name,
    securityMode: preview.provider.security_mode,
    serviceName: preview.provider.service_name,
    version: preview.provider.version,
  },
  providerFingerprint: preview.provider_fingerprint,
  schemaVersion: preview.preview_schema_version,
});

export function buildManagedDependencyCreateRequest({
  draft,
  operationId,
  preview,
}: {
  draft?: { id: string; revision: number };
  operationId: string;
  preview: ManagedDependencyPreview;
}): CreateManagedDependencyRequest {
  if (!preview.compatible
    || preview.preview_schema_version !== 2
    || !preview.binding_id
    || !preview.provider_fingerprint
    || !preview.consumer_descriptor_fingerprint
    || !preview.snapshot_fingerprint) {
    throw new Error("A compatible managed dependency preview is required before creation.");
  }
  return {
    binding_id: preview.binding_id,
    dependency_type: preview.dependency_type,
    ...(draft ? { draft } : {}),
    expected_consumer_descriptor_fingerprint:
      preview.consumer_descriptor_fingerprint,
    expected_provider_fingerprint: preview.provider_fingerprint,
    expected_snapshot_fingerprint: preview.snapshot_fingerprint,
    operation_id: operationId,
    preview_schema_version: preview.preview_schema_version,
    provider: {
      cluster_id: preview.provider.cluster_id,
      service_name: preview.provider.service_name,
    },
  };
}

export function assertManagedDependencyRecord(
  binding: ManagedDependencyBinding,
  record: ManagedDependencyMaterializationRecord,
) {
  const responseAttempt = [
    binding.attempted_operation,
    binding.creation_attempt,
  ].find((candidate) => candidate?.kind === "CREATE"
    && candidate.target_snapshot
    && (candidate.target_snapshot_version === undefined
      || candidate.target_snapshot_version === candidate.target_snapshot.version)
    && managedDependencyAttempts(record).some(
      (saved) => saved.operationId === candidate.operation_id,
    ));
  const attempt = managedDependencyAttempts(record).find((candidate) =>
    candidate.operationId === responseAttempt?.operation_id);
  const request = attempt?.request;
  const targetSnapshot = responseAttempt?.target_snapshot;
  if (!attempt
    || !request
    || !targetSnapshot
    || binding.binding_id !== record.bindingId
    || binding.dependency_type !== record.dependencyType
    || targetSnapshot.consumer_fingerprint
      !== request.expected_consumer_descriptor_fingerprint
    || targetSnapshot.provider_fingerprint
      !== request.expected_provider_fingerprint
    || targetSnapshot.fingerprint !== request.expected_snapshot_fingerprint
    || targetSnapshot.schema_version !== request.preview_schema_version) {
    throw new Error("The recovered managed dependency does not match this wizard operation.");
  }
  return { attempt, binding };
}

export const managedDependencyAttempts = (
  record: ManagedDependencyMaterializationRecord,
): ManagedDependencyMaterializationAttempt[] => {
  if (record.attempts?.length) return record.attempts;
  return record.request ? [{
    operationId: record.operationId,
    request: record.request,
    response: record.response,
  }] : [];
};

export const managedDependencyAttemptMatchesPreview = (
  attempt: ManagedDependencyMaterializationAttempt,
  preview: ManagedDependencyPreview,
) => attempt.request.binding_id === preview.binding_id
  && attempt.request.dependency_type === preview.dependency_type
  && attempt.request.expected_consumer_descriptor_fingerprint
    === preview.consumer_descriptor_fingerprint
  && attempt.request.expected_provider_fingerprint === preview.provider_fingerprint
  && attempt.request.expected_snapshot_fingerprint === preview.snapshot_fingerprint
  && attempt.request.preview_schema_version === preview.preview_schema_version
  && attempt.request.provider.cluster_id === preview.provider.cluster_id
  && attempt.request.provider.service_name === preview.provider.service_name;

const MAX_UNRESOLVED_ATTEMPTS = 8;

export class ManagedDependencyAttemptLimitError extends Error {
  constructor() {
    super("Too many unresolved managed dependency attempts require reconciliation.");
    this.name = "ManagedDependencyAttemptLimitError";
  }
}

export function appendManagedDependencyAttempt(
  record: ManagedDependencyMaterializationRecord,
  attempt: ManagedDependencyMaterializationAttempt,
) {
  const attempts = managedDependencyAttempts(record);
  if (attempts.some(({ operationId }) => operationId === attempt.operationId)) {
    return record;
  }
  const unresolved = attempts.filter(({ response }) => !response);
  if (unresolved.length >= MAX_UNRESOLVED_ATTEMPTS) {
    throw new ManagedDependencyAttemptLimitError();
  }
  return {
    ...record,
    attempts: [...attempts.filter(({ response }) => !response), attempt],
    operationId: attempt.operationId,
    request: attempt.request,
  };
}

export function recordManagedDependencyResponse(
  record: ManagedDependencyMaterializationRecord,
  attempt: ManagedDependencyMaterializationAttempt,
  response: ManagedDependencyBinding,
) {
  const resolvedAttempt = { ...attempt, response };
  return {
    ...record,
    attempts: managedDependencyAttempts(record).map((candidate) =>
      candidate.operationId === attempt.operationId ? resolvedAttempt : candidate),
    operationId: attempt.operationId,
    request: attempt.request,
    response,
  };
}

export const isMissingManagedDependency = (error: any) =>
  error?.response?.status === 404;

export const isBindingIdUnavailable = (error: any) =>
  error?.response?.status === 409
  && error?.response?.data?.code === "BINDING_ID_UNAVAILABLE";

export class ManagedDependencyReviewRequiredError extends Error {
  constructor() {
    super("Managed dependency settings require review.");
    this.name = "ManagedDependencyReviewRequiredError";
  }
}
