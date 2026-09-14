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

import { describe, expect, it } from "vitest";
import {
  appendManagedDependencyAttempt,
  assertManagedDependencyRecord,
  buildManagedDependencyCreateRequest,
  managedDependencyAttemptMatchesPreview,
  managedDependencyReviewSignature,
  type ManagedDependencyMaterializationRecord,
} from "./managedDependencyMaterialization";

const preview = {
  binding_id: "11111111-1111-4111-8111-111111111111",
  client_config: { "core-site": { "fs.defaultFS": "hdfs://provider" } },
  compatible: true,
  consumer: {
    lifecycle: "INIT",
    planned_hbase_user: "hbase_a",
    scope: "SERVICE",
    service_name: "HBASE" as const,
  },
  consumer_descriptor_fingerprint: "consumer-fingerprint",
  dependency_type: "HDFS" as const,
  errors: [],
  namespace: { root_uri: "hdfs://provider/apps/hbase/a" },
  preview_schema_version: 2,
  provider: {
    cluster_id: 9,
    cluster_name: "provider-a",
    service_name: "HDFS",
  },
  provider_fingerprint: "provider-fingerprint",
  snapshot_fingerprint: "snapshot-fingerprint",
};

describe("managed dependency materialization", () => {
  it("builds the exact immutable CREATE body from the final same-UUID preview", () => {
    expect(buildManagedDependencyCreateRequest({
      draft: { id: "22222222-2222-4222-8222-222222222222", revision: 8 },
      operationId: "33333333-3333-4333-8333-333333333333",
      preview,
    })).toEqual({
      binding_id: preview.binding_id,
      dependency_type: "HDFS",
      draft: { id: "22222222-2222-4222-8222-222222222222", revision: 8 },
      expected_consumer_descriptor_fingerprint: "consumer-fingerprint",
      expected_provider_fingerprint: "provider-fingerprint",
      expected_snapshot_fingerprint: "snapshot-fingerprint",
      operation_id: "33333333-3333-4333-8333-333333333333",
      preview_schema_version: 2,
      provider: { cluster_id: 9, service_name: "HDFS" },
    });
  });

  it("requires a fresh schema-2 preview instead of upgrading old approval locally", () => {
    expect(() => buildManagedDependencyCreateRequest({
      operationId: "33333333-3333-4333-8333-333333333333",
      preview: { ...preview, preview_schema_version: 1 },
    })).toThrow(/compatible managed dependency preview/i);
  });

  it("requires re-review when displayed paths or identity change", () => {
    expect(managedDependencyReviewSignature({
      ...preview,
      namespace: { root_uri: "hdfs://provider/apps/hbase/b" },
    })).not.toBe(managedDependencyReviewSignature(preview));
    expect(managedDependencyReviewSignature({
      ...preview,
      consumer: { ...preview.consumer, planned_hbase_user: "hbase_b" },
    })).not.toBe(managedDependencyReviewSignature(preview));
    expect(managedDependencyReviewSignature({
      ...preview,
      provider_fingerprint: "provider-policy-changed",
    })).not.toBe(managedDependencyReviewSignature(preview));
    expect(managedDependencyReviewSignature({
      ...preview,
      provider: {
        ...preview.provider,
        security_mode: "KERBEROS",
        version: {
          active: true,
          client_features: ["hdfs-client"],
          resolved_versions: { HDFS: "3.3.6" },
          service_version: "3.3.6",
          stack_name: "BIGTOP",
          stack_version: "3.3.0",
        },
      },
    })).not.toBe(managedDependencyReviewSignature(preview));
  });

  it("treats plan and live provenance as equal when semantic fingerprints match", () => {
    const planned = {
      ...preview,
      consumer: {
        ...preview.consumer,
        cluster_id: 4,
        cluster_name: "consumer",
        lifecycle: "ADD_SERVICE_PLAN",
        scope: "SERVICE_PLAN",
      },
      snapshot_fingerprint: "plan-envelope",
    };
    const live = {
      ...preview,
      consumer: {
        ...preview.consumer,
        cluster_id: 4,
        cluster_name: "consumer",
        lifecycle: "INIT",
        scope: "SERVICE",
      },
      snapshot_fingerprint: "live-envelope",
    };

    expect(managedDependencyReviewSignature(planned))
      .toBe(managedDependencyReviewSignature(live));
    expect(managedDependencyReviewSignature({
      ...live,
      provider_fingerprint: "provider-policy-changed",
    })).not.toBe(managedDependencyReviewSignature(planned));
  });

  it("rejects a same-binding response owned by another operation", () => {
    const request = buildManagedDependencyCreateRequest({
      operationId: "33333333-3333-4333-8333-333333333333",
      preview,
    });
    expect(() => assertManagedDependencyRecord({
      binding_id: preview.binding_id,
      dependency_type: "HDFS",
      operation: {
        epoch: 1,
        kind: "CREATE",
        operation_id: "44444444-4444-4444-8444-444444444444",
        state: "QUEUED",
        target_snapshot_version: 1,
      },
      creation_attempt: {
        epoch: 1,
        kind: "CREATE",
        operation_id: "44444444-4444-4444-8444-444444444444",
        state: "QUEUED",
        target_snapshot_version: 1,
        target_snapshot: {
          consumer_fingerprint: "consumer-fingerprint",
          fingerprint: "snapshot-fingerprint",
          provider_fingerprint: "provider-fingerprint",
          schema_version: 2,
          version: 1,
        },
      },
      ownership: "managed",
      provider: preview.provider,
      snapshot: {
        consumer_fingerprint: "consumer-fingerprint",
        fingerprint: "snapshot-fingerprint",
        provider_fingerprint: "provider-fingerprint",
        schema_version: 2,
        version: 1,
      },
    }, {
      bindingId: preview.binding_id,
      clusterId: 4,
      clusterName: "consumer",
      dependencyType: "HDFS",
      operationId: request.operation_id,
      request,
    })).toThrow(/does not match this wizard operation/);
  });

  it("matches an older immutable attempt only by operation and snapshot fingerprints", () => {
    const firstRequest = buildManagedDependencyCreateRequest({
      operationId: "33333333-3333-4333-8333-333333333333",
      preview,
    });
    const changedPreview = {
      ...preview,
      provider_fingerprint: "new-provider-fingerprint",
    };
    const secondRequest = buildManagedDependencyCreateRequest({
      operationId: "44444444-4444-4444-8444-444444444444",
      preview: changedPreview,
    });
    const baseRecord = {
      bindingId: preview.binding_id,
      clusterId: 4,
      clusterName: "consumer",
      dependencyType: "HDFS" as const,
      operationId: firstRequest.operation_id,
      request: firstRequest,
    };
    const record = appendManagedDependencyAttempt(baseRecord, {
      operationId: secondRequest.operation_id,
      request: secondRequest,
    });
    const binding = {
      binding_id: preview.binding_id,
      dependency_type: "HDFS" as const,
      operation: {
        epoch: 1,
        kind: "CREATE",
        operation_id: firstRequest.operation_id,
        state: "QUEUED",
        target_snapshot_version: 1,
      },
      creation_attempt: {
        epoch: 1,
        kind: "CREATE",
        operation_id: firstRequest.operation_id,
        state: "QUEUED",
        target_snapshot_version: 1,
        target_snapshot: {
          consumer_fingerprint: "consumer-fingerprint",
          fingerprint: "snapshot-fingerprint",
          provider_fingerprint: "provider-fingerprint",
          schema_version: 2,
          version: 1,
        },
      },
      ownership: "managed" as const,
      provider: preview.provider,
      snapshot: {
        consumer_fingerprint: "consumer-fingerprint",
        fingerprint: "snapshot-fingerprint",
        provider_fingerprint: "provider-fingerprint",
        schema_version: 2,
        version: 1,
      },
    };

    const recovered = assertManagedDependencyRecord(binding, record);
    expect(recovered.attempt.operationId).toBe(firstRequest.operation_id);
    expect(managedDependencyAttemptMatchesPreview(recovered.attempt, preview)).toBe(true);
    expect(managedDependencyAttemptMatchesPreview(recovered.attempt, changedPreview)).toBe(false);
    const currentBinding = {
      ...binding,
      snapshot: {
        ...binding.snapshot,
        provider_fingerprint: "current-provider-fingerprint",
      },
      operation: {
        ...binding.operation,
        kind: "UPDATE",
        operation_id: "55555555-5555-4555-8555-555555555555",
        target_snapshot_version: 2,
      },
    };
    expect(assertManagedDependencyRecord(currentBinding, record).attempt.operationId)
      .toBe(firstRequest.operation_id);
    expect(() => assertManagedDependencyRecord({
      ...binding,
      creation_attempt: {
        ...binding.creation_attempt,
        target_snapshot: {
          ...binding.creation_attempt.target_snapshot,
          provider_fingerprint: "unrecorded-provider-fingerprint",
        },
      },
    }, record)).toThrow(/does not match this wizard operation/);
    expect(() => assertManagedDependencyRecord({
      ...binding,
      creation_attempt: {
        ...binding.creation_attempt,
        kind: "UPDATE",
      },
    }, record)).toThrow(/does not match this wizard operation/);
    expect(() => assertManagedDependencyRecord({
      ...binding,
      creation_attempt: {
        ...binding.creation_attempt,
        target_snapshot_version: 2,
      },
    }, record)).toThrow(/does not match this wizard operation/);
  });

  it("recovers immutable CREATE provenance after a current UPDATE", () => {
    const createRequest = buildManagedDependencyCreateRequest({
      operationId: "33333333-3333-4333-8333-333333333333",
      preview,
    });
    const record: ManagedDependencyMaterializationRecord = {
      bindingId: preview.binding_id,
      clusterId: 4,
      clusterName: "consumer",
      dependencyType: "HDFS",
      operationId: createRequest.operation_id,
      request: createRequest,
    };
    const recovered = assertManagedDependencyRecord({
      binding_id: preview.binding_id,
      dependency_type: "HDFS",
      operation: {
        epoch: 2,
        kind: "UPDATE",
        operation_id: "44444444-4444-4444-8444-444444444444",
        state: "QUEUED",
        target_snapshot_version: 2,
      },
      creation_attempt: {
        epoch: 1,
        kind: "CREATE",
        operation_id: createRequest.operation_id,
        state: "SUCCEEDED",
        target_snapshot_version: 1,
        target_snapshot: {
          consumer_fingerprint: "consumer-fingerprint",
          fingerprint: "snapshot-fingerprint",
          provider_fingerprint: "provider-fingerprint",
          schema_version: 2,
          version: 1,
        },
      },
      ownership: "managed",
      provider: preview.provider,
      snapshot: {
        consumer_fingerprint: "consumer-fingerprint",
        fingerprint: "updated-snapshot",
        provider_fingerprint: "provider-fingerprint",
        schema_version: 2,
        version: 2,
      },
    }, record);
    expect(recovered.attempt.operationId).toBe(createRequest.operation_id);
  });

  it("bounds unresolved request history without discarding an unknown outcome", () => {
    const firstRequest = buildManagedDependencyCreateRequest({
      operationId: "00000000-0000-4000-8000-000000000000",
      preview,
    });
    let record: ManagedDependencyMaterializationRecord = {
      bindingId: preview.binding_id,
      clusterId: 4,
      clusterName: "consumer",
      dependencyType: "HDFS" as const,
      operationId: firstRequest.operation_id,
      request: firstRequest,
    };
    for (let index = 1; index < 8; index += 1) {
      const operationId = `00000000-0000-4000-8000-${String(index).padStart(12, "0")}`;
      const request = buildManagedDependencyCreateRequest({ operationId, preview });
      record = appendManagedDependencyAttempt(record, { operationId, request });
    }
    const finalOperationId = "00000000-0000-4000-8000-000000000008";
    expect(() => appendManagedDependencyAttempt(record, {
      operationId: finalOperationId,
      request: buildManagedDependencyCreateRequest({
        operationId: finalOperationId,
        preview,
      }),
    })).toThrow(/unresolved managed dependency attempts/);
    expect(record.attempts).toHaveLength(8);
  });
});
