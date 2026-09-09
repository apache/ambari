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

import { ambariApi } from "./config/axiosConfig";

export type ManagedDependencyType = "HDFS" | "ZOOKEEPER";
export type ManagedDependencyOwnership = "managed" | "local" | "unmanaged" | "unknown";
export type ManagedDependencyBindingPhase =
  | "PREVIEWED"
  | "PROVIDER_PREPARING"
  | "PROVIDER_PREPARED"
  | "ZOOKEEPER_HANDOFF_RECONCILING"
  | "CONSUMER_VERIFYING"
  | "READY"
  | "STALE"
  | "FAILED"
  | "DETACHING"
  | "FENCING_UNCERTAIN"
  | "RETIRED"
  | "DETACHED"
  | "TOMBSTONED";

export type ManagedDependencyAction =
  | "INSTALL_OR_CONFIGURE"
  | "ISSUE_CREDENTIALS"
  | "START_OR_RESTART"
  | "RETRY"
  | "DETACH";

export type ManagedDependencyIssue = {
  code: string;
  message: string;
};

export type ManagedDependencyVersion = {
  active: boolean;
  client_features: string[];
  resolved_versions: Record<string, string>;
  service_version: string;
  stack_name: string;
  stack_version: string;
};

export type ManagedDependencyProvider = {
  cluster_id: number;
  cluster_name: string | null;
  healthy?: boolean;
  installed?: boolean;
  security_mode?: string;
  service_name: string;
  version?: ManagedDependencyVersion;
};

export type ManagedDependencyCandidate = ManagedDependencyProvider & {
  compatible: boolean;
  errors: ManagedDependencyIssue[];
};

export type ManagedDependencyNamespace = {
  container_znode?: string;
  root_uri?: string;
  wal_uri?: string;
  znode?: string;
};

export type ManagedDependencyConsumer = {
  cluster_id?: number;
  cluster_name?: string;
  lifecycle: string;
  planned_hbase_user: string;
  scope: string;
  service_name: "HBASE";
};

export type ManagedDependencyPreview = {
  binding_id: string;
  client_config?: Record<string, Record<string, string>>;
  compatible: boolean;
  consumer: ManagedDependencyConsumer;
  consumer_descriptor_fingerprint?: string;
  dependency_type: ManagedDependencyType;
  errors: ManagedDependencyIssue[];
  namespace?: ManagedDependencyNamespace;
  preview_schema_version: number;
  provider: ManagedDependencyProvider;
  provider_fingerprint?: string;
  snapshot_fingerprint?: string;
};

export type ManagedDependencyBindingSummary = {
  applied_snapshot_version?: number | null;
  binding_id?: string;
  dependency_type: ManagedDependencyType;
  desired_snapshot_version?: number | null;
  operation_epoch?: number | null;
  failure_code?: string | null;
  failure_message?: string | null;
  failure_retryable?: boolean | null;
  namespace?: ManagedDependencyNamespace;
  ownership: ManagedDependencyOwnership;
  phase?: ManagedDependencyBindingPhase;
  planned_hbase_user?: string;
  provider?: ManagedDependencyProvider;
  row_version?: number;
  state?: string;
  readiness?: ManagedDependencyReadiness;
  capabilities?: ManagedDependencyCapabilities;
  allowed_actions?: ManagedDependencyAction[];
  next_action?: string;
  consumer?: {
    cluster_id: number;
    service_name: string;
  };
};

export type ManagedDependencyReadiness = {
  topology_current: boolean;
  required_daemon_host_ids: string[];
  prepared_daemon_host_ids: string[];
  verified_daemon_host_ids: string[];
  all_current_daemons_prepared: boolean;
  all_current_daemons_verified: boolean;
  active_command: boolean;
  preparation_requests?: ManagedDependencyPreparationRequest[];
};

export type ManagedDependencyPreparationRequest = {
  binding_id: string;
  operation_id: string;
  epoch: number;
  snapshot_version: number;
  host_id: number;
  component_name: string;
  request_id?: number | null;
  task_id?: number | null;
  state: string;
};

export type ManagedDependencyCapabilities = {
  provider_prepared: boolean;
  install_or_configure_allowed: boolean;
  credential_status: "NOT_REQUIRED" | "REQUIRED" | "ISSUED" | "UNKNOWN";
  credentials_required: boolean;
  start_or_restart_allowed: boolean;
  retry_allowed: boolean;
  detach_allowed: boolean;
  allowed_actions: ManagedDependencyAction[];
  next_action: string;
};

export type ManagedDependencySnapshotSummary = {
  consumer_fingerprint: string;
  fingerprint: string;
  provider_fingerprint: string;
  schema_version: number;
  version: number;
};

export type ManagedDependencyOperationSummary = {
  epoch: number;
  failure_code?: string | null;
  failure_message?: string | null;
  kind: string;
  operation_id: string;
  request_id?: number | null;
  state: string;
  target_snapshot_version?: number;
};

export type ManagedDependencyAttemptSummary = ManagedDependencyOperationSummary & {
  target_snapshot?: ManagedDependencySnapshotSummary;
};

export type ManagedDependencyBinding = ManagedDependencyBindingSummary & {
  attempted_operation?: ManagedDependencyAttemptSummary;
  creation_attempt?: ManagedDependencyAttemptSummary;
  operation?: ManagedDependencyOperationSummary;
  snapshot?: ManagedDependencySnapshotSummary;
};

export type ManagedDependencyProviderReference = {
  cluster_id: number;
  service_name: string;
};

export type ManagedDependencyAdvisorConsumer =
  | { scope: "DRAFT"; draft_id: string; expected_revision: number }
  | { scope: "SERVICE_PLAN"; cluster_id: number; expected_revision: number }
  | { scope: "SERVICE"; cluster_id: number };

export type ManagedDependencyAdvisorSelection = {
  binding_id: string;
  dependency_type: ManagedDependencyType;
  expected_consumer_descriptor_fingerprint: string;
  expected_provider_fingerprint: string;
  expected_snapshot_fingerprint: string;
  preview_schema_version: number;
  provider: ManagedDependencyProviderReference;
};

export type ManagedDependencyAdvisorPlan = {
  consumer: ManagedDependencyAdvisorConsumer;
  selections: ManagedDependencyAdvisorSelection[];
};

export type ManagedDependencyPlanSelection = {
  binding_id: string;
  dependency_type: ManagedDependencyType;
  provider: ManagedDependencyProviderReference;
};

export type ManagedDependencyPlanPreviewRequest = {
  consumer: ManagedDependencyAdvisorConsumer;
  selections: ManagedDependencyPlanSelection[];
};

export type ManagedDependencyPlanPreviewResponse = {
  items: ManagedDependencyPreview[];
};

export type ManagedDependent = {
  binding_id: string;
  consumer_cluster_id: number;
  consumer_cluster_name: string;
  consumer_service_name: string;
  dependency_type: ManagedDependencyType;
  state: string;
};

export type ManagedDependentsResponse = {
  hidden_dependent_count: number;
  impact_revision: string;
  items: ManagedDependent[];
};

export type ManagedDependencyImpact = {
  action: "STOP";
  dependent_count: number;
  impact_revision: string;
  requires_confirmation: boolean;
};

export type CreateManagedDependencyRequest = {
  binding_id: string;
  dependency_type: ManagedDependencyType;
  draft?: { id: string; revision: number };
  expected_consumer_descriptor_fingerprint: string;
  expected_provider_fingerprint: string;
  expected_snapshot_fingerprint: string;
  operation_id: string;
  preview_schema_version: number;
  provider: ManagedDependencyProviderReference;
};

export type CreateManagedDependencyPlanRequest = {
  items: CreateManagedDependencyRequest[];
};

export type CreateManagedDependencyPlanResponse = {
  items: ManagedDependencyBinding[];
};

const serviceDependencyPath = (clusterName: string, serviceName = "HBASE") =>
  `/clusters/${encodeURIComponent(clusterName)}/services/${encodeURIComponent(serviceName)}/dependencies`;

const providerServicePath = (clusterName: string, serviceName: string) =>
  `/clusters/${encodeURIComponent(clusterName)}/services/${encodeURIComponent(serviceName)}`;

export const ServiceDependenciesApi = {
  getDraftCandidates: async ({
    dependencyType,
    draftId,
    expectedDraftRevision,
    signal,
  }: {
    dependencyType: ManagedDependencyType;
    draftId: string;
    expectedDraftRevision: number;
    signal?: AbortSignal;
  }) => (await ambariApi.get<{ items: ManagedDependencyCandidate[] }>(
    "/service-dependencies/candidates",
    {
      params: {
        consumer_scope: "draft",
        dependency_type: dependencyType,
        draft_id: draftId,
        expected_draft_revision: expectedDraftRevision,
      },
      signal,
    },
  )).data.items,

  previewDraft: async ({
    bindingId,
    dependencyType,
    draftId,
    expectedRevision,
    provider,
    signal,
  }: {
    bindingId?: string;
    dependencyType: ManagedDependencyType;
    draftId: string;
    expectedRevision: number;
    provider: ManagedDependencyProviderReference;
    signal?: AbortSignal;
  }) => (await ambariApi.post<ManagedDependencyPreview>(
    "/service-dependencies/preview",
    {
      ...(bindingId ? { binding_id: bindingId } : {}),
      consumer: { scope: "DRAFT", draft_id: draftId, expected_revision: expectedRevision },
      dependency_type: dependencyType,
      provider,
    },
    { signal },
  )).data,

  previewPlan: async ({
    consumer,
    selections,
    signal,
  }: ManagedDependencyPlanPreviewRequest & { signal?: AbortSignal }) => (await ambariApi.post<
    ManagedDependencyPlanPreviewResponse
  >(
    "/service-dependencies/preview",
    { consumer, selections },
    { signal },
  )).data,

  getServicePlanCandidates: async ({
    clusterId,
    dependencyType,
    expectedWorkflowRevision,
    signal,
  }: {
    clusterId: number;
    dependencyType: ManagedDependencyType;
    expectedWorkflowRevision: number;
    signal?: AbortSignal;
  }) => (await ambariApi.get<{ items: ManagedDependencyCandidate[] }>(
    "/service-dependencies/candidates",
    {
      params: {
        cluster_id: clusterId,
        consumer_scope: "service_plan",
        dependency_type: dependencyType,
        expected_workflow_revision: expectedWorkflowRevision,
      },
      signal,
    },
  )).data.items,

  previewServicePlan: async ({
    bindingId,
    clusterId,
    dependencyType,
    expectedRevision,
    provider,
    signal,
  }: {
    bindingId?: string;
    clusterId: number;
    dependencyType: ManagedDependencyType;
    expectedRevision: number;
    provider: ManagedDependencyProviderReference;
    signal?: AbortSignal;
  }) => (await ambariApi.post<ManagedDependencyPreview>(
    "/service-dependencies/preview",
    {
      ...(bindingId ? { binding_id: bindingId } : {}),
      consumer: { scope: "SERVICE_PLAN", cluster_id: clusterId, expected_revision: expectedRevision },
      dependency_type: dependencyType,
      provider,
    },
    { signal },
  )).data,

  getServiceCandidates: async (
    clusterName: string,
    dependencyType: ManagedDependencyType,
    signal?: AbortSignal,
  ) => (await ambariApi.get<{ items: ManagedDependencyCandidate[] }>(
    `${serviceDependencyPath(clusterName)}/candidates`,
    { params: { type: dependencyType }, signal },
  )).data.items,

  previewService: async (
    clusterName: string,
    dependencyType: ManagedDependencyType,
    provider: ManagedDependencyProviderReference,
    signal?: AbortSignal,
    bindingId?: string,
  ) => (await ambariApi.post<ManagedDependencyPreview>(
    `${serviceDependencyPath(clusterName)}/preview`,
    {
      ...(bindingId ? { binding_id: bindingId } : {}),
      dependency_type: dependencyType,
      provider,
    },
    { signal },
  )).data,

  create: async (
    clusterName: string,
    request: CreateManagedDependencyRequest,
    signal?: AbortSignal,
  ) => (await ambariApi.post<ManagedDependencyBinding>(
    serviceDependencyPath(clusterName),
    request,
    { signal },
  )).data,

  createMany: async (
    clusterName: string,
    requests: CreateManagedDependencyRequest[],
    signal?: AbortSignal,
  ) => (await ambariApi.post<CreateManagedDependencyPlanResponse>(
    serviceDependencyPath(clusterName),
    { items: requests },
    { signal },
  )).data.items,

  list: async (clusterName: string, signal?: AbortSignal) =>
    (await ambariApi.get<{ items: ManagedDependencyBindingSummary[] }>(
      serviceDependencyPath(clusterName),
      { signal },
    )).data.items,

  get: async (clusterName: string, bindingId: string, signal?: AbortSignal) =>
    (await ambariApi.get<ManagedDependencyBinding>(
      `${serviceDependencyPath(clusterName)}/${encodeURIComponent(bindingId)}`,
      { signal },
    )).data,

  getDependents: async (
    clusterName: string,
    serviceName: string,
    signal?: AbortSignal,
  ) => (await ambariApi.get<ManagedDependentsResponse>(
    `${providerServicePath(clusterName, serviceName)}/dependents`,
    { signal },
  )).data,

  getImpact: async (
    clusterName: string,
    serviceName: string,
    signal?: AbortSignal,
  ) => (await ambariApi.get<ManagedDependencyImpact>(
    `${providerServicePath(clusterName, serviceName)}/dependency-impact`,
    { params: { action: "STOP" }, signal },
  )).data,
};

export default ServiceDependenciesApi;
