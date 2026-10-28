/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
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

import { supressErrorAmbariApi } from "./config/axiosConfig";

export type MpackBinding = {
  stack_name: string; stack_version: string;
  extension_name: string; extension_version: string;
};
export type MpackMutation = {
  schema_version: 1; action: "IMPORT" | "ENABLE" | "INSTALL" | "UPDATE" | "BIND" | "UNBIND" | "UNINSTALL";
  archive_digests: string[]; release_ids: string[]; bindings: MpackBinding[];
  activate: boolean; maintenance: boolean;
};
export type MpackRelease = {
  schema_version: 1; id: string; archive_digest: string; manifest_json: string;
  contributions: { scope: string; target: string }[];
  dependencies: Record<string, { release: string; digest: string }>; installed: boolean;
};
export type MpackUploadMember = {
  archive_digest: string; name: string; version: string;
  extensions: { name: string; version: string; minimum_stacks: MpackTarget[] }[];
  stacks: MpackTarget[];
};
export type MpackUpload = MpackUploadMember & {
  schema_version: 1; bundle?: boolean; members?: MpackUploadMember[];
};
export type MpackTarget = { stack_name: string; stack_version: string };
export type MpackCapabilities = {
  schema_version: 1; target_stack_versions: MpackTarget[];
  operations: string[]; binding_scope: "STACK_VERSION"; per_cluster_definition_versions: false;
};
export type MpackPlan = {
  schema_version: 1; id: string; digest: string; mutation: MpackMutation;
  candidate_snapshot: string; affected_clusters: string[];
  maintenance_required: boolean; restart_required: boolean; expires_at: number;
  deployment?: MpackDeployment | null;
};
export type MpackDeployment = {
  stack_name: string; stack_version: string; cluster_id: number | null; service_names: string[]; service_ids: string[];
};
export type MpackServiceEntry = {
  id: string; release_id: string; archive_digest: string; service_name: string; display_name: string;
  description: string | null; service_version: string; stack_name: string; stack_version: string;
  required_services: string[]; enabled: boolean; client_only: boolean;
};
export type MpackServiceCatalog = {
  schema_version: 1; items: MpackServiceEntry[];
  unavailable: { release_id: string; code: string; message: string }[];
  destinations: { cluster_id: number; cluster_name: string; stack_name: string; stack_version: string }[];
};
export type MpackDeploymentHandoff = {
  schema_version: 1; operation_id: string; plan_id: string; effective_snapshot: string;
  deployment: MpackDeployment; cluster_name: string | null;
};
export type MpackHookReceipt = {
  schema_version: 1; operation_id: string; plan_digest: string; archive_digest: string;
  phase: string; attempt: number; state: "RUNNING" | "APPLIED" | "FAILED" | "UNKNOWN";
  effect_state: "APPLIED" | "NOT_APPLIED" | "UNKNOWN"; observations: Record<string, unknown>;
};
export type MpackOperation = {
  schema_version: 1; id: string; plan_id: string; plan_digest: string; generation: number;
  phase: string; effective_snapshot: string | null; updated_at: number; owner_id: number;
  hooks: Record<string, MpackHookReceipt>; hook_history: MpackHookReceipt[];
  error_code: string | null; error_details: Record<string, unknown>;
  action?: MpackMutation["action"]; service_names?: string[]; release_ids?: string[];
};
export type MpackMemberResult = {
  schema_version: 1; id: string; parent_id: string; release_id: string;
  archive_digest: string; phase: string;
};
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const digest = /^[0-9a-f]{64}$/;
const phases = new Set(["ACCEPTED", "PREPARING", "WAITING_MAINTENANCE", "WAITING_RESTART",
  "PUBLISHING", "SUCCEEDED", "FAILED", "CANCELLING", "CANCELLED", "RECOVERY_REQUIRED"]);
export const isMpackWaiting = (operation: MpackOperation) =>
  !["SUCCEEDED", "FAILED", "CANCELLED", "RECOVERY_REQUIRED", "WAITING_RESTART"].includes(operation.phase);
function fail(): never { throw new Error("Invalid management pack response contract"); }
function object(value: unknown): Record<string, any> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return fail();
  return value as Record<string, any>;
}
function envelope(value: unknown): Record<string, any> {
  const item = object(value);
  if (item.schema_version !== 1) return fail();
  return item;
}
function identity(value: unknown, pattern: RegExp): string {
  if (typeof value !== "string" || !pattern.test(value)) return fail();
  return value;
}
function canonical(value: any): string {
  const ordered = (node: any): any => Array.isArray(node) ? node.map(ordered)
    : node && typeof node === "object" ? Object.fromEntries(Object.keys(node).sort()
      .map((key) => [key, ordered(node[key])])) : node;
  return JSON.stringify(ordered(value));
}
function binding(value: unknown): MpackBinding {
  const item = object(value);
  for (const key of ["stack_name", "stack_version", "extension_name", "extension_version"]) {
    if (typeof item[key] !== "string" || !item[key]) return fail();
  }
  return item as MpackBinding;
}
export function validateMpackOperation(value: unknown, expectedId?: string): MpackOperation {
  const item = envelope(value);
  identity(item.id, uuid); identity(item.plan_id, uuid); identity(item.plan_digest, digest);
  if (expectedId && item.id !== expectedId) return fail();
  if (!Number.isSafeInteger(item.generation) || item.generation < 0 || !phases.has(item.phase)) return fail();
  if (!Number.isSafeInteger(item.updated_at) || !Number.isSafeInteger(item.owner_id)) return fail();
  if (item.action != null && !["IMPORT", "ENABLE", "INSTALL", "UPDATE", "BIND", "UNBIND", "UNINSTALL"].includes(item.action)) return fail();
  for (const key of ["service_names", "release_ids"]) {
    if (item[key] != null && (!Array.isArray(item[key]) || item[key].some((value: unknown) => typeof value !== "string"))) return fail();
  }
  if (!Array.isArray(item.hook_history)) return fail();
  const hooks = object(item.hooks);
  object(item.error_details);
  for (const receipt of [...Object.values(hooks), ...item.hook_history]) {
    const result = envelope(receipt);
    if (result.operation_id !== item.id || result.plan_digest !== item.plan_digest) return fail();
    identity(result.archive_digest, digest);
    if (!Number.isSafeInteger(result.attempt) || result.attempt < 1) return fail();
    if (!["RUNNING", "APPLIED", "FAILED", "UNKNOWN"].includes(result.state) ||
      !["APPLIED", "NOT_APPLIED", "UNKNOWN"].includes(result.effect_state)) return fail();
    object(result.observations);
    if (!["before-install", "after-install", "before-upgrade", "after-upgrade", "before-uninstall",
      "after-uninstall"].includes(result.phase)) return fail();
  }
  Object.entries(hooks).forEach(([key, receipt]: [string, any]) => {
    if (key !== receipt.archive_digest + "/" + receipt.phase) return fail();
  });
  const attempts = new Set<string>();
  item.hook_history.forEach((receipt: MpackHookReceipt) => {
    const key = receipt.archive_digest + "/" + receipt.phase;
    const identity = key + "/" + receipt.attempt;
    if (attempts.has(identity) || hooks[key] && hooks[key].attempt <= receipt.attempt) return fail();
    attempts.add(identity);
  });
  if (item.phase === "SUCCEEDED") {
    identity(item.effective_snapshot, digest);
    if (item.error_code !== null || Object.values(hooks).some((receipt: any) =>
      receipt.state !== "APPLIED" || receipt.effect_state === "UNKNOWN" ||
      !Object.keys(receipt.observations).length)) return fail();
  }
  return item as MpackOperation;
}
function list(value: unknown): unknown[] {
  const result = envelope(value);
  if (!Array.isArray(result.items)) return fail();
  return result.items;
}
function uploadMember(value: unknown): MpackUploadMember {
  const item = object(value);
  identity(item.archive_digest, digest);
  if (typeof item.name !== "string" || typeof item.version !== "string" ||
    !Array.isArray(item.extensions) || !Array.isArray(item.stacks)) return fail();
  item.extensions.forEach((extension: any) => {
    if (!extension || typeof extension.name !== "string" || typeof extension.version !== "string" ||
      !Array.isArray(extension.minimum_stacks)) return fail();
  });
  return item as MpackUploadMember;
}
const MpackApi = {
  getPlan: async (id: string): Promise<MpackPlan> => {
    identity(id, uuid);
    const item = envelope((await supressErrorAmbariApi.get("/mpack_plans/" + id)).data);
    if (item.id !== id || !Array.isArray(item.affected_clusters)) return fail();
    identity(item.digest, digest); identity(item.candidate_snapshot, digest);
    if (item.deployment != null) validateDeployment(item.deployment);
    return item as MpackPlan;
  },
  services: async (): Promise<MpackServiceCatalog> => {
    const item = envelope((await supressErrorAmbariApi.get("/mpack_services")).data);
    if (!Array.isArray(item.items) || !Array.isArray(item.unavailable) || !Array.isArray(item.destinations)) return fail();
    item.items.forEach((entry: any) => {
      identity(entry.id, digest); identity(entry.archive_digest, digest);
      for (const key of ["release_id", "service_name", "display_name", "stack_name", "stack_version", "service_version"]) {
        if (typeof entry[key] !== "string" || !entry[key]) return fail();
      }
      if (typeof entry.enabled !== "boolean" || typeof entry.client_only !== "boolean" ||
        !Array.isArray(entry.required_services) || entry.required_services.some((name: unknown) => typeof name !== "string")) return fail();
    });
    item.destinations.forEach((entry: any) => {
      if (!Number.isSafeInteger(entry.cluster_id) || entry.cluster_id < 1 ||
        [entry.cluster_name, entry.stack_name, entry.stack_version].some(value => typeof value !== "string" || !value)) return fail();
    });
    return item as MpackServiceCatalog;
  },
  planServices: async (serviceIds: string[], clusterId: number | null, maintenance: boolean): Promise<MpackPlan> => {
    const item = envelope((await supressErrorAmbariApi.post("/mpack_service_plans", {
      schema_version: 1, service_ids: serviceIds, cluster_id: clusterId, maintenance,
    })).data);
    identity(item.id, uuid); identity(item.digest, digest); identity(item.candidate_snapshot, digest);
    if (!Array.isArray(item.affected_clusters) || typeof item.maintenance_required !== "boolean" ||
      typeof item.restart_required !== "boolean" || !Number.isSafeInteger(item.expires_at) ||
      item.mutation?.action !== "ENABLE" || !item.deployment || item.deployment.cluster_id !== clusterId) return fail();
    validateDeployment(item.deployment);
    if (canonical([...item.deployment.service_ids].sort()) !== canonical([...serviceIds].sort())) return fail();
    return item as MpackPlan;
  },
  deployment: async (id: string): Promise<MpackDeploymentHandoff> => {
    identity(id, uuid);
    const item = envelope((await supressErrorAmbariApi.get("/mpack_operations/" + id + "/deployment")).data);
    if (item.operation_id !== id) return fail();
    identity(item.plan_id, uuid); identity(item.effective_snapshot, digest);
    validateDeployment(item.deployment);
    if (item.deployment.cluster_id !== null && (typeof item.cluster_name !== "string" || !item.cluster_name)) return fail();
    return item as MpackDeploymentHandoff;
  },
  capabilities: async (): Promise<MpackCapabilities> => {
    const item = envelope((await supressErrorAmbariApi.get("/mpack_capabilities")).data);
    if (item.binding_scope !== "STACK_VERSION" || item.per_cluster_definition_versions !== false ||
      !Array.isArray(item.target_stack_versions) || !Array.isArray(item.operations)) return fail();
    return item as MpackCapabilities;
  },
  releases: async (): Promise<MpackRelease[]> => list(
    (await supressErrorAmbariApi.get("/mpacks")).data).map((value) => {
      const item = envelope(value);
      identity(item.archive_digest, digest);
      if (typeof item.id !== "string" || typeof item.manifest_json !== "string" ||
        typeof item.installed !== "boolean" || !Array.isArray(item.contributions)) return fail();
      object(item.dependencies);
      return item as MpackRelease;
    }),
  bindings: async (): Promise<MpackBinding[]> =>
    list((await supressErrorAmbariApi.get("/mpack_bindings")).data).map(binding),
  operations: async (): Promise<MpackOperation[]> =>
    list((await supressErrorAmbariApi.get("/mpack_operations")).data).map((item) => validateMpackOperation(item)),
  upload: async (file: File): Promise<MpackUpload> => {
    const item = envelope((await supressErrorAmbariApi.post("/mpack_uploads", file,
      { headers: { "Content-Type": "application/octet-stream" } })).data);
    identity(item.archive_digest, digest);
    if (item.bundle === true) {
      if (!Array.isArray(item.members) || !item.members.length) return fail();
      item.members.forEach(uploadMember);
    } else { uploadMember(item); }
    return item as MpackUpload;
  },
  plan: async (mutation: MpackMutation): Promise<MpackPlan> => {
    const item = envelope((await supressErrorAmbariApi.post("/mpack_plans", mutation)).data);
    identity(item.id, uuid); identity(item.digest, digest); identity(item.candidate_snapshot, digest);
    if (!Array.isArray(item.affected_clusters) || typeof item.maintenance_required !== "boolean" ||
      typeof item.restart_required !== "boolean" || !Number.isSafeInteger(item.expires_at)) return fail();
    if (canonical(item.mutation) !== canonical(mutation)) return fail();
    return item as MpackPlan;
  },
  submit: async (planId: string, key: string, expectedDigest: string): Promise<MpackOperation> => {
    identity(planId, uuid); identity(expectedDigest, digest);
    const result = validateMpackOperation((await supressErrorAmbariApi.post("/mpack_operations",
      { schema_version: 1, plan_id: planId }, { headers: { "Idempotency-Key": key } })).data);
    if (result.plan_id !== planId || result.plan_digest !== expectedDigest) return fail();
    return result;
  },
  operation: async (id: string): Promise<MpackOperation> => {
    identity(id, uuid);
    return validateMpackOperation((await supressErrorAmbariApi.get("/mpack_operations/" + id)).data, id);
  },
  members: async (id: string): Promise<MpackMemberResult[]> => {
    identity(id, uuid);
    return list((await supressErrorAmbariApi.get("/mpack_operations/" + id + "/members")).data).map((value) => {
      const item = envelope(value);
      identity(item.id, digest); identity(item.archive_digest, digest);
      if (item.parent_id !== id || typeof item.release_id !== "string" ||
        ![...phases, "BLOCKED"].includes(item.phase)) return fail();
      return item as MpackMemberResult;
    });
  },
  recover: async (id: string, action: "recover" | "retry" | "cancel"): Promise<MpackOperation> => {
    identity(id, uuid);
    return validateMpackOperation((await supressErrorAmbariApi.post(
      "/mpack_operations/" + id + "/" + action)).data, id);
  },
};
function validateDeployment(value: unknown): MpackDeployment {
  const item = object(value);
  if (typeof item.stack_name !== "string" || !item.stack_name || typeof item.stack_version !== "string" || !item.stack_version ||
    !Array.isArray(item.service_names) || !item.service_names.length || item.service_names.some((name: unknown) => typeof name !== "string" || !name) ||
    (item.cluster_id !== null && (!Number.isSafeInteger(item.cluster_id) || item.cluster_id < 1))) return fail();
  if (!Array.isArray(item.service_ids) || item.service_ids.length !== item.service_names.length) return fail();
  item.service_ids.forEach((id: unknown) => identity(id, digest));
  return item as MpackDeployment;
}
export function isMpackDefinitiveRejection(error: unknown): boolean {
  const response = (error as any)?.response;
  return response?.data?.schema_version === 1 && [400, 401, 403, 404, 409, 413, 422].includes(response.status)
    && ["INVALID_MANIFEST", "UNSUPPORTED_SCHEMA", "NOT_FOUND", "STALE_PLAN", "OPERATION_CONFLICT",
      "AUTHORIZATION_FAILED", "UPLOAD_LIMIT", "INVALID_TARGET"].includes(response.data.error?.code);
}
export function mpackErrorMessage(error: unknown): string {
  const response = (error as any)?.response?.data;
  if (response?.schema_version === 1 && typeof response.error?.code === "string" &&
    typeof response.error?.message === "string") {
    const details = response.error.details;
    const blockers = Array.isArray(details?.blockers) ? details.blockers.map((item: any) =>
      [item.cluster_name ?? item.cluster_id, item.service_name ?? item.service, item.release_id ?? item.provider,
        item.task_id != null ? "Task " + item.task_id : null].filter(value => value != null && value !== "").join(" / ")).filter(Boolean) : [];
    if (Array.isArray(details?.affected_clusters)) blockers.push(...details.affected_clusters.filter((value: unknown) => typeof value === "string"));
    return response.error.message + (blockers.length ? ": " + blockers.join(", ") : "");
  }
  return error instanceof Error ? error.message : "The management pack request could not be resolved";
}
export default MpackApi;
