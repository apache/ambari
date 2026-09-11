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

import { describe, expect, it, vi } from "vitest";
import type { ManagedDependencyPreview } from "../../api/serviceDependenciesApi";
import {
  buildManagedDependencyAdvisorPlan,
  createManagedDependencyAdvisorRunner,
  ManagedDependencyAdvisorReviewRequiredError,
  managedDependencyAdvisorInputKey,
} from "./managedDependencyAdvisor";

const preview = (dependencyType: "HDFS" | "ZOOKEEPER"): ManagedDependencyPreview => ({
  binding_id: dependencyType === "HDFS"
    ? "11111111-1111-4111-8111-111111111111"
    : "22222222-2222-4222-8222-222222222222",
  compatible: true,
  consumer: {
    lifecycle: "DRAFT",
    planned_hbase_user: "hbase_consumer",
    scope: "DRAFT",
    service_name: "HBASE",
  },
  consumer_descriptor_fingerprint: `consumer-${dependencyType}`,
  dependency_type: dependencyType,
  errors: [],
  preview_schema_version: 2,
  provider: {
    cluster_id: dependencyType === "HDFS" ? 31 : 32,
    cluster_name: `provider-${dependencyType.toLowerCase()}`,
    service_name: dependencyType,
  },
  provider_fingerprint: `provider-${dependencyType}`,
  snapshot_fingerprint: `snapshot-${dependencyType}`,
});

describe("managed dependency Stack Advisor plan", () => {
  it("projects only exact reviewed schema-2 facts into the DRAFT request", () => {
    const hdfs = preview("HDFS");
    const zookeeper = preview("ZOOKEEPER");
    expect(buildManagedDependencyAdvisorPlan(
      {
        scope: "DRAFT",
        draft_id: "33333333-3333-4333-8333-333333333333",
        expected_revision: 9,
      },
      {
        HDFS: { mode: "managed", preview: hdfs, provider: { ...hdfs.provider, compatible: true, errors: [] } },
        ZOOKEEPER: {
          mode: "managed",
          preview: zookeeper,
          provider: { ...zookeeper.provider, compatible: true, errors: [] },
        },
      },
    )).toEqual({
      consumer: {
        scope: "DRAFT",
        draft_id: "33333333-3333-4333-8333-333333333333",
        expected_revision: 9,
      },
      selections: [
        {
          binding_id: hdfs.binding_id,
          dependency_type: "HDFS",
          expected_consumer_descriptor_fingerprint: "consumer-HDFS",
          expected_provider_fingerprint: "provider-HDFS",
          expected_snapshot_fingerprint: "snapshot-HDFS",
          preview_schema_version: 2,
          provider: { cluster_id: 31, service_name: "HDFS" },
        },
        {
          binding_id: zookeeper.binding_id,
          dependency_type: "ZOOKEEPER",
          expected_consumer_descriptor_fingerprint: "consumer-ZOOKEEPER",
          expected_provider_fingerprint: "provider-ZOOKEEPER",
          expected_snapshot_fingerprint: "snapshot-ZOOKEEPER",
          preview_schema_version: 2,
          provider: { cluster_id: 32, service_name: "ZOOKEEPER" },
        },
      ],
    });
  });

  it("requires provider review for schema-1, missing snapshot, or provider drift", () => {
    const hdfs = preview("HDFS");
    const consumer = { scope: "SERVICE" as const, cluster_id: 7 };
    [
      { ...hdfs, preview_schema_version: 1 },
      { ...hdfs, snapshot_fingerprint: undefined },
    ].forEach((invalidPreview) => {
      expect(() => buildManagedDependencyAdvisorPlan(consumer, {
        HDFS: { mode: "managed", preview: invalidPreview },
      })).toThrow(ManagedDependencyAdvisorReviewRequiredError);
    });
    expect(() => buildManagedDependencyAdvisorPlan(consumer, {
      HDFS: {
        mode: "managed",
        preview: hdfs,
        provider: { ...hdfs.provider, cluster_id: 99, compatible: true, errors: [] },
      },
    })).toThrow(ManagedDependencyAdvisorReviewRequiredError);
  });

  it("leaves ordinary local HDFS and ZooKeeper advice unchanged", () => {
    expect(buildManagedDependencyAdvisorPlan(
      { scope: "SERVICE", cluster_id: 7 },
      { HDFS: { mode: "local" }, ZOOKEEPER: { mode: "local" } },
    )).toBeUndefined();
  });

  it("keeps Add Service advice on SERVICE_PLAN after an owned INIT row is observed", async () => {
    const hdfs = preview("HDFS");
    const withStateCheckpoint = vi.fn(async (
      request: (revision: number) => Promise<any>,
    ) => request(17));
    const scopeKeyRef = { current: "cluster-a:add-service" };
    const runner = createManagedDependencyAdvisorRunner({
      clusterId: 27,
      managedDependencies: { HDFS: { mode: "managed", preview: hdfs } },
      scopeKey: scopeKeyRef.current,
      scopeKeyRef,
      withStateCheckpoint,
      workflowMaterializedServices: ["HBASE"],
    });

    const prepared = await runner(async (request) => request);

    expect(prepared.properties.managed_dependency_plan?.consumer).toEqual({
      scope: "SERVICE_PLAN",
      cluster_id: 27,
      expected_revision: 17,
    });
  });

  it("keys the advice inputs deterministically and changes on semantic replacement", () => {
    const hdfs = preview("HDFS");
    const input = {
      hosts: ["host-b", "host-a"],
      selections: { HDFS: { mode: "managed" as const, preview: hdfs } },
      services: ["HBASE", "HIVE"],
      stack: "BIGTOP",
      version: "3.2.0",
    };
    const key = managedDependencyAdvisorInputKey(input);
    expect(managedDependencyAdvisorInputKey({
      ...input,
      hosts: [...input.hosts].reverse(),
      services: [...input.services].reverse(),
    })).toBe(key);
    expect(managedDependencyAdvisorInputKey({
      ...input,
      selections: { HDFS: {
        mode: "managed",
        preview: { ...hdfs, snapshot_fingerprint: "replacement-snapshot" },
      } },
    })).not.toBe(key);
    expect(managedDependencyAdvisorInputKey({
      ...input,
      hosts: ["host-a", "host-c"],
    })).not.toBe(key);
  });
});
