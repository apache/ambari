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

import type {
  ManagedDependencyAdvisorConsumer,
  ManagedDependencyAdvisorPlan,
} from "../../api/serviceDependenciesApi";
import {
  dependencyTypes,
  type ManagedDependencySelections,
} from "./managedDependencySelection";

export const MANAGED_DEPENDENCY_ADVISOR_SCHEMA_VERSION = 2;

export type PreparedStackAdvisorRequest = {
  isCurrent: () => boolean;
  properties: {
    clusterId?: string;
    managed_dependency_plan?: ManagedDependencyAdvisorPlan;
  };
};

export type RunWithStackAdvisorRequest = <T>(
  request: (prepared: PreparedStackAdvisorRequest) => Promise<T>,
) => Promise<T>;

type StateCheckpoint = <T>(
  request: (revision: number) => Promise<T>,
) => Promise<T>;

type ManagedDependencyAdvisorRunnerOptions = {
  clusterId?: number;
  draftId?: string;
  managedDependencies: ManagedDependencySelections;
  scopeKey: string;
  scopeKeyRef: { current: string };
  scopeGeneration?: number;
  scopeGenerationRef?: { current: number };
  withStateCheckpoint?: StateCheckpoint;
  /**
   * Materialized INIT rows are workflow state, not proof of an active binding.
   * Keep this compatibility input until the live SERVICE contract is released;
   * it must not select SERVICE on its own.
   */
  workflowMaterializedServices?: string[];
};

export function createManagedDependencyAdvisorRunner({
  clusterId,
  draftId,
  managedDependencies,
  scopeKey,
  scopeKeyRef,
  scopeGeneration,
  scopeGenerationRef,
  withStateCheckpoint,
}: ManagedDependencyAdvisorRunnerOptions): RunWithStackAdvisorRequest {
  return async <T>(request: (prepared: PreparedStackAdvisorRequest) => Promise<T>) => {
    const capturedScope = scopeKey;
    const capturedGeneration = scopeGeneration;
    const isCurrentScope = () =>
      scopeKeyRef.current === capturedScope
      && (scopeGenerationRef === undefined
        || scopeGenerationRef.current === capturedGeneration);
    if (!withStateCheckpoint) {
      throw new ManagedDependencyAdvisorReviewRequiredError();
    }
    if (draftId === undefined
      && (!Number.isInteger(clusterId) || (clusterId as number) <= 0)) {
      throw new ManagedDependencyAdvisorReviewRequiredError();
    }
    return withStateCheckpoint(async (revision) => {
      if (!isCurrentScope()) {
        throw new ManagedDependencyAdvisorReviewRequiredError();
      }
      const consumer = draftId !== undefined
        ? { scope: "DRAFT" as const, draft_id: draftId, expected_revision: revision }
        : {
            scope: "SERVICE_PLAN" as const,
            cluster_id: clusterId as number,
            expected_revision: revision,
          };
      const plan = buildManagedDependencyAdvisorPlan(
        consumer,
        managedDependencies,
      );
      if (!plan) throw new ManagedDependencyAdvisorReviewRequiredError();
      return request({
        isCurrent: isCurrentScope,
        properties: { managed_dependency_plan: plan },
      });
    });
  };
}

export function managedDependencyAdvisorInputKey({
  hosts,
  selections,
  services,
  stack,
  version,
}: {
  hosts: string[];
  selections: ManagedDependencySelections;
  services: string[];
  stack?: string;
  version?: string;
}) {
  return JSON.stringify({
    hosts: [...hosts].sort(),
    providers: dependencyTypes.map((dependencyType) => {
      const choice = selections[dependencyType];
      return {
        bindingId: choice?.preview?.binding_id || null,
        compatible: choice?.preview?.compatible || false,
        consumerFingerprint:
          choice?.preview?.consumer_descriptor_fingerprint || null,
        dependencyType,
        mode: choice?.mode || null,
        planningIssue: choice?.planningIssue?.code || null,
        providerClusterId: choice?.provider?.cluster_id
          ?? choice?.preview?.provider.cluster_id
          ?? null,
        providerFingerprint: choice?.preview?.provider_fingerprint || null,
        providerServiceName: choice?.provider?.service_name
          || choice?.preview?.provider.service_name
          || null,
        schemaVersion: choice?.preview?.preview_schema_version || null,
        snapshotFingerprint: choice?.preview?.snapshot_fingerprint || null,
      };
    }),
    services: [...services].sort(),
    stack: stack || null,
    version: version || null,
  });
}

export class ManagedDependencyAdvisorReviewRequiredError extends Error {
  constructor() {
    super("Managed dependency provider settings must be reviewed again.");
    this.name = "ManagedDependencyAdvisorReviewRequiredError";
  }
}

const requiredString = (value: unknown) =>
  typeof value === "string" && value.length > 0 ? value : null;

export function buildManagedDependencyAdvisorPlan(
  consumer: ManagedDependencyAdvisorConsumer,
  selections: ManagedDependencySelections,
): ManagedDependencyAdvisorPlan | undefined {
  const managedChoices = dependencyTypes.flatMap((dependencyType) => {
    const choice = selections[dependencyType];
    return choice?.mode === "managed" ? [{ choice, dependencyType }] : [];
  });
  if (!managedChoices.length) return undefined;

  const advisorSelections = managedChoices.map(({ choice, dependencyType }) => {
    const preview = choice.preview;
    const bindingId = requiredString(preview?.binding_id);
    const providerFingerprint = requiredString(preview?.provider_fingerprint);
    const consumerFingerprint = requiredString(
      preview?.consumer_descriptor_fingerprint,
    );
    const snapshotFingerprint = requiredString(preview?.snapshot_fingerprint);
    if (!preview?.compatible
      || preview.preview_schema_version !== MANAGED_DEPENDENCY_ADVISOR_SCHEMA_VERSION
      || preview.dependency_type !== dependencyType
      || !bindingId
      || !providerFingerprint
      || !consumerFingerprint
      || !snapshotFingerprint
      || !Number.isInteger(preview.provider.cluster_id)
      || preview.provider.cluster_id <= 0
      || !requiredString(preview.provider.service_name)
      || (choice.provider && (
        choice.provider.cluster_id !== preview.provider.cluster_id
        || choice.provider.service_name !== preview.provider.service_name
      ))) {
      throw new ManagedDependencyAdvisorReviewRequiredError();
    }
    return {
      binding_id: bindingId,
      dependency_type: dependencyType,
      expected_consumer_descriptor_fingerprint: consumerFingerprint,
      expected_provider_fingerprint: providerFingerprint,
      expected_snapshot_fingerprint: snapshotFingerprint,
      preview_schema_version: preview.preview_schema_version,
      provider: {
        cluster_id: preview.provider.cluster_id,
        service_name: preview.provider.service_name,
      },
    };
  });

  return { consumer, selections: advisorSelections };
}

export const hasManagedAdvisorDependency = (
  prepared: PreparedStackAdvisorRequest | null | undefined,
  dependencyType: "HDFS" | "ZOOKEEPER",
) => prepared?.properties.managed_dependency_plan?.selections.some(
  (selection) => selection.dependency_type === dependencyType,
) === true;

export const stackAdvisorErrorCode = (error: any) =>
  String(error?.response?.data?.code || "");

export const stackAdvisorNeedsProviderReview = (error: any) => {
  const code = stackAdvisorErrorCode(error);
  return code === "WORKFLOW_VERSION_CONFLICT"
    || code === "DEPENDENCY_PREVIEW_STALE"
    || code === "DEPENDENCY_PROVIDER_CHANGED"
    || code === "DEPENDENCY_SECURITY_PLAN_INCOMPLETE"
    || code === "DEPENDENCY_DRAFT_VERSION_UNMATERIALIZED";
};
