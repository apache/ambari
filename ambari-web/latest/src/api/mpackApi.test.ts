/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
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

import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }));
vi.mock("./config/axiosConfig", () => ({ supressErrorAmbariApi: mocks }));

import MpackApi, { type MpackMutation, validateMpackOperation, isMpackWaiting } from "./mpackApi";

const id = "11111111-1111-4111-8111-111111111111";
const planId = "22222222-2222-4222-8222-222222222222";
const digest = "a".repeat(64);
const archive = "b".repeat(64);
const operation = () => ({
  schema_version: 1, id, plan_id: planId, plan_digest: digest, generation: 1,
  phase: "ACCEPTED", effective_snapshot: null, updated_at: 1, owner_id: 1,
  hooks: {}, hook_history: [], error_code: null, error_details: {},
});
const receipt = () => ({
  schema_version: 1, operation_id: id, plan_digest: digest, archive_digest: archive,
  phase: "after-install", attempt: 1, state: "APPLIED", effect_state: "APPLIED",
  observations: { resource_digest: archive },
});

describe("management pack HTTP contract", () => {
  beforeEach(() => vi.clearAllMocks());

  it("keeps durable cancellation pending until the server records its result", () => {
    const pending = validateMpackOperation({ ...operation(), phase: "CANCELLING" });
    expect(isMpackWaiting(pending)).toBe(true);
    expect(isMpackWaiting(validateMpackOperation({ ...pending, phase: "CANCELLED" }))).toBe(false);
  });

  it("replays acceptance with the same plan and idempotency key", async () => {
    mocks.post.mockResolvedValue({ data: operation() });
    await expect(MpackApi.submit(planId, "retained-key", digest)).resolves.toEqual(operation());
    expect(mocks.post).toHaveBeenCalledWith("/mpack_operations",
      { schema_version: 1, plan_id: planId }, { headers: { "Idempotency-Key": "retained-key" } });
  });

  it("rejects an acceptance result belonging to another plan", async () => {
    mocks.post.mockResolvedValue({ data: { ...operation(), plan_digest: archive } });
    await expect(MpackApi.submit(planId, "retained-key", digest)).rejects.toThrow();
  });

  it("rejects foreign operations, schemas and missing required observations", () => {
    expect(() => validateMpackOperation(operation(), planId)).toThrow();
    expect(() => validateMpackOperation({ ...operation(), schema_version: 2 })).toThrow();
    expect(() => validateMpackOperation({ ...operation(), hook_history: undefined })).toThrow();
  });

  it("requires matching hook identity even when diagnostics claim success", () => {
    const hook = { ...receipt(), operation_id: planId, observations: { diagnostic: "completed" } };
    expect(() => validateMpackOperation({ ...operation(), hooks: { [archive + "/after-install"]: hook } }))
      .toThrow();
  });

  it("does not turn missing or uncertain hook observations into success", () => {
    const value = { ...operation(), phase: "SUCCEEDED", effective_snapshot: digest };
    expect(() => validateMpackOperation({ ...value, hooks: {
      [archive + "/after-install"]: { ...receipt(), observations: {} },
    } })).toThrow();
    expect(() => validateMpackOperation({ ...value, hooks: {
      [archive + "/after-install"]: { ...receipt(), effect_state: "UNKNOWN" },
    } })).toThrow();
    expect(validateMpackOperation({ ...value, hooks: { [archive + "/after-install"]: receipt() } }).phase)
      .toBe("SUCCEEDED");
  });

  it("rejects duplicated or newer historical attempts", () => {
    const value = { ...operation(), hooks: {
      [archive + "/after-install"]: { ...receipt(), attempt: 2 },
    } };
    expect(() => validateMpackOperation({ ...value, hook_history: [receipt(), receipt()] })).toThrow();
    expect(() => validateMpackOperation({ ...value, hook_history: [{ ...receipt(), attempt: 2 }] })).toThrow();
  });

  it("compares the complete preview mutation independent of object key order", async () => {
    const mutation: MpackMutation = {
      schema_version: 1, action: "INSTALL", archive_digests: [archive], release_ids: [], bindings: [],
      activate: true, maintenance: false,
    };
    const result = {
      schema_version: 1, id: planId, digest, candidate_snapshot: archive, affected_clusters: [],
      maintenance_required: false, restart_required: false, expires_at: 1,
      mutation: Object.fromEntries(Object.entries(mutation).reverse()),
    };
    mocks.post.mockResolvedValue({ data: result });
    await expect(MpackApi.plan(mutation)).resolves.toEqual(result);
    mocks.post.mockResolvedValue({ data: { ...result, mutation: { ...mutation, maintenance: true } } });
    await expect(MpackApi.plan(mutation)).rejects.toThrow();
  });

  it("rejects a bundle member result from another parent operation", async () => {
    mocks.get.mockResolvedValue({ data: { schema_version: 1, items: [{
      schema_version: 1, id: digest, parent_id: planId, release_id: "nginx/1.0",
      archive_digest: archive, phase: "SUCCEEDED",
    }] } });
    await expect(MpackApi.members(id)).rejects.toThrow();
  });

  it("requires service-plan selections to match the submitted catalog identities", async () => {
    const deployment = { stack_name: "BASE", stack_version: "1.0", cluster_id: 5,
      service_names: ["EXAMPLE"], service_ids: [archive] };
    const plan = { schema_version: 1, id: planId, digest, candidate_snapshot: archive,
      affected_clusters: [], maintenance_required: false, restart_required: false, expires_at: 1,
      mutation: { action: "ENABLE" }, deployment };
    mocks.post.mockResolvedValue({ data: plan });
    await expect(MpackApi.planServices([archive], 5, false)).resolves.toEqual(plan);
    mocks.post.mockResolvedValue({ data: { ...plan, deployment: { ...deployment, service_ids: [digest] } } });
    await expect(MpackApi.planServices([archive], 5, false)).rejects.toThrow();
  });

  it("rejects a deployment handoff belonging to another accepted operation", async () => {
    mocks.get.mockResolvedValue({ data: { schema_version: 1, operation_id: planId, plan_id: planId,
      effective_snapshot: digest, deployment: { stack_name: "BASE", stack_version: "1.0", cluster_id: null,
        service_names: ["EXAMPLE"], service_ids: [archive] }, cluster_name: null } });
    await expect(MpackApi.deployment(id)).rejects.toThrow();
  });
});
